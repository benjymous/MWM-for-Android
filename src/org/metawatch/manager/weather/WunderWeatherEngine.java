package org.metawatch.manager.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.metawatch.manager.Application;
import org.metawatch.manager.Idle;
import org.metawatch.manager.Log;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.GeolocationMode;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchStatus;
import org.metawatch.manager.Monitors;
import org.metawatch.manager.Utils;

import android.content.Context;

public class WunderWeatherEngine extends AbstractWeatherEngine {

    public String getIcon(String cond, boolean isDay) {
	if (cond.equals("clear") || cond.equals("sunny"))
	    if (isDay)
		return "weather_sunny.bmp";
	    else
		return "weather_nt_clear.bmp";
	else if (cond.equals("cloudy"))
	    return "weather_cloudy.bmp";
	else if (cond.equals("partlycloudy") || cond.equals("mostlycloudy") || cond.equals("partlysunny") || cond.equals("mostlysunny"))
	    if (isDay)
		return "weather_partlycloudy.bmp";
	    else
		return "weather_nt_partlycloudy.bmp";
	else if (cond.equals("rain") || cond.equals("chancerain"))
	    return "weather_rain.bmp";
	else if (cond.equals("fog") || cond.equals("hazy"))
	    return "weather_fog.bmp";
	else if (cond.equals("tstorms") || cond.equals("chancetstorms"))
	    return "weather_thunderstorm.bmp";
	else if (cond.equals("snow") || cond.equals("chancesnow") || cond.equals("sleet") || cond.equals("chancesleet") || cond.equals("flurries") || cond.equals("chanceflurries"))
	    return "weather_snow.bmp";
	else
	    return "weather_cloudy.bmp";
    }

    public synchronized WeatherData update(Context context, WeatherData weatherData) {
	try {
	    if (isUpdateRequired(weatherData)) {

		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Monitors.updateWeatherDataWunderground(): start");

		if (Preferences.wundergroundKey.equals("")) {
		    Log.e(MetaWatchStatus.TAG, "Weather Wunderground requires a personal key to be configured!");
		    return weatherData;
		}

		String forecastQuery = "";
		boolean hasForecast = false;

		long diff = System.currentTimeMillis() - weatherData.forecastTimeStamp;
		if (weatherData.forecast == null || (diff > 3 * 60 * 60 * 1000)) {
		    // Only update forecast every three hours
		    forecastQuery = "forecast10day/astronomy/";
		    hasForecast = true;
		}

		String requestUrl = null;

		switch (Preferences.weatherGeolocationMode) {

		case GeolocationMode.MANUAL: {
		    weatherData.locationName = Preferences.weatherCity;
		    String weatherLocation = Preferences.weatherCity.replace(",", " ").replace("  ", " ").replace(" ", "%20");

		    requestUrl = "http://api.wunderground.com/api/" + Preferences.wundergroundKey + "/conditions/" + forecastQuery + "q/" + weatherLocation + ".json";
		}
		    break;

		case GeolocationMode.ALWAYSGOOGLE: {
		    GoogleGeoCoderLocationData locationData = reverseLookupGeoLocation(context, Monitors.getInstance().mLocationData.latitude, Monitors.getInstance().mLocationData.longitude);
		    weatherData.locationName = locationData.getLocationName();
		    String weatherLocation = Double.toString(Monitors.getInstance().mLocationData.latitude) + "," + Double.toString(Monitors.getInstance().mLocationData.longitude);
		    requestUrl = "http://api.wunderground.com/api/" + Preferences.wundergroundKey + "/conditions/" + forecastQuery + "q/" + weatherLocation + ".json";
		}
		    break;

		case GeolocationMode.USEPROVIDER: {
		    String weatherLocation = Double.toString(Monitors.getInstance().mLocationData.latitude) + "," + Double.toString(Monitors.getInstance().mLocationData.longitude);
		    requestUrl = "http://api.wunderground.com/api/" + Preferences.wundergroundKey + "/geolookup/conditions/" + forecastQuery + "q/" + weatherLocation + ".json";
		}
		    break;

		default:
		    Log.e(MetaWatchStatus.TAG, "Unknown geolocation mode");
		    return weatherData;
		}

		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Request: " + requestUrl);

		JSONObject json = getJSONfromURL(requestUrl);

		JSONObject current = json.getJSONObject("current_observation");

		if (hasForecast) {
		    JSONObject moon = json.getJSONObject("moon_phase");
		    JSONObject sunrise = moon.getJSONObject("sunrise");
		    weatherData.sunriseH = sunrise.getInt("hour");
		    weatherData.sunriseM = sunrise.getInt("minute");
		    JSONObject sunset = moon.getJSONObject("sunset");
		    weatherData.sunsetH = sunset.getInt("hour");
		    weatherData.sunsetM = sunset.getInt("minute");

		    weatherData.moonPercentIlluminated = moon.getInt("percentIlluminated");
		    weatherData.ageOfMoon = moon.getInt("ageOfMoon");
		}

		boolean isDay = true;

		Calendar cal = Calendar.getInstance();
		int hours = cal.get(Calendar.HOUR_OF_DAY);
		int minutes = cal.get(Calendar.MINUTE);

		if ((hours < weatherData.sunriseH) || (hours == weatherData.sunriseH && minutes < weatherData.sunriseM) || (hours > weatherData.sunsetH) || (hours == weatherData.sunsetH && minutes > weatherData.sunsetM)) {
		    isDay = false;
		}

		if (Preferences.weatherGeolocationMode == GeolocationMode.USEPROVIDER) {
		    JSONObject location = json.getJSONObject("location");
		    weatherData.locationName = location.getString("city");
		}

		weatherData.condition = current.getString("weather");
		weatherData.icon = getIcon(current.getString("icon"), isDay);

		if (Preferences.weatherCelsius) {
		    weatherData.temp = current.getString("temp_c");
		} else {
		    weatherData.temp = current.getString("temp_f");
		}

		if (hasForecast) {
		    JSONObject forecast = json.getJSONObject("forecast");
		    JSONArray forecastday = forecast.getJSONObject("simpleforecast").getJSONArray("forecastday");

		    int days = forecastday.length();
		    weatherData.forecast = new Forecast[days];

		    for (int i = 0; i < days; ++i) {
			weatherData.forecast[i] = new Forecast();
			JSONObject day = forecastday.getJSONObject(i);
			JSONObject date = day.getJSONObject("date");

			weatherData.forecast[i].setIcon(getIcon(day.getString("icon"), true));
			weatherData.forecast[i].setDay(date.getString("weekday_short"));
			if (Preferences.weatherCelsius) {
			    weatherData.forecast[i].setTempLow(day.getJSONObject("low").getString("celsius"));
			    weatherData.forecast[i].setTempHigh(day.getJSONObject("high").getString("celsius"));
			} else {
			    weatherData.forecast[i].setTempLow(day.getJSONObject("low").getString("fahrenheit"));
			    weatherData.forecast[i].setTempHigh(day.getJSONObject("high").getString("fahrenheit"));
			}
		    }

		    weatherData.forecastTimeStamp = System.currentTimeMillis();
		}

		weatherData.celsius = Preferences.weatherCelsius;

		weatherData.received = true;
		weatherData.timeStamp = System.currentTimeMillis();

		weatherData.error = false;
		weatherData.errorString = "";
		switch(MetaWatchService.getWatchMode()) {
		case APPLICATION:
		    Application.refreshCurrentApp(context);
		    break;
		case CALL:
		    break;
		case IDLE:
		    Idle.getInstance().updateIdle(context, true);
		    break;
		case NOTIFICATION:
		    break;
		case OFF:
		    break;
		default:
		    break;
		
		}
	    }

	} catch (Exception e) {
	    weatherData.errorString = e.getMessage();
	    if (weatherData.errorString != null)
		weatherData.error = true;

	    if (Preferences.logging)
		Log.e(MetaWatchStatus.TAG, "Exception while retreiving weather", e);
	} finally {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Monitors.updateWeatherData(): finish");
	}

	return weatherData;
    }

    // http://p-xr.com/android-tutorial-how-to-parse-read-json-data-into-a-android-listview/
    public static JSONObject getJSONfromURL(String url) throws IOException {

	// initialize
	InputStream is = null;
	String result = "";
	JSONObject jArray = null;

	// http post
	try {
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(url);
	    HttpResponse response = httpclient.execute(httppost);
	    HttpEntity entity = response.getEntity();
	    is = entity.getContent();

	} catch (Exception e) {
	    if (Preferences.logging)
		Log.e(MetaWatchStatus.TAG, "Error in http connection " + e.toString());
	    throw Utils.createCompatibleIOException(e);
	}

	// convert response to string
	if (is != null) {
	    try {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
		StringBuilder sb = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
		    sb.append(line + "\n");
		}
		is.close();
		result = sb.toString();
	    } catch (Exception e) {
		if (Preferences.logging)
		    Log.e(MetaWatchStatus.TAG, "Error converting result " + e.toString());
	    }

	    // // dump to sdcard for debugging
	    // File sdCard = Environment.getExternalStorageDirectory();
	    // File file = new File(sdCard, "weather.json");
	    //
	    // try {
	    // FileWriter writer = new FileWriter(file);
	    // writer.append(result);
	    // writer.flush();
	    // writer.close();
	    // } catch (FileNotFoundException e1) {
	    // // TODO Auto-generated catch block
	    // e1.printStackTrace();
	    // } catch (IOException e) {
	    // // TODO Auto-generated catch block
	    // e.printStackTrace();
	    // }

	    // try parse the string to a JSON object
	    try {
		jArray = new JSONObject(result);
	    } catch (JSONException e) {
		if (Preferences.logging)
		    Log.e(MetaWatchStatus.TAG, "Error parsing data " + e.toString());
		throw Utils.createCompatibleIOException(e);
	    }
	}
	return jArray;
    }

}
