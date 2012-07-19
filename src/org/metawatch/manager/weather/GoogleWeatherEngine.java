package org.metawatch.manager.weather;

import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.anddev.android.weatherforecast.weather.GoogleWeatherHandler;
import org.anddev.android.weatherforecast.weather.WeatherCurrentCondition;
import org.anddev.android.weatherforecast.weather.WeatherForecastCondition;
import org.anddev.android.weatherforecast.weather.WeatherSet;
import org.anddev.android.weatherforecast.weather.WeatherUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.metawatch.manager.Idle;
import org.metawatch.manager.MetaWatch;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Monitors.LocationData;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.util.Log;

public class GoogleWeatherEngine extends AbstractWeatherEngine {

	public String getIcon(String cond) {
		if (cond.equals("clear") || cond.equals("sunny"))
			return "weather_sunny.bmp";
		else if (cond.equals("cloudy") || cond.equals("overcast"))
			return "weather_cloudy.bmp";
		else if (cond.equals("mostly cloudy") || cond.equals("partly cloudy")
				|| cond.equals("mostly sunny") || cond.equals("partly sunny"))
			return "weather_partlycloudy.bmp";
		else if (cond.equals("light rain") || cond.equals("rain")
				|| cond.equals("rain showers") || cond.equals("showers")
				|| cond.equals("chance of showers")
				|| cond.equals("scattered showers")
				|| cond.equals("freezing rain")
				|| cond.equals("freezing drizzle")
				|| cond.equals("rain and snow"))
			return "weather_rain.bmp";
		else if (cond.equals("thunderstorm") || cond.equals("chance of storm")
				|| cond.equals("isolated thunderstorms"))
			return "weather_thunderstorm.bmp";
		else if (cond.equals("chance of snow") || cond.equals("snow showers")
				|| cond.equals("ice/snow") || cond.equals("flurries"))
			return "weather_snow.bmp";
		else
			return "weather_cloudy.bmp";
	}

	public synchronized WeatherData update(Context context, WeatherData weatherData) {
		try {
			if (isUpdateRequired(weatherData)) {
				if (Preferences.logging)
					Log.d(MetaWatch.TAG,
							"Monitors.updateWeatherDataGoogle(): start");
				
				String queryString;
				if (isGeolocationDataUsed()) {
					GoogleGeoCoderLocationData locationData = reverseLookupGeoLocation(context, LocationData.latitude, LocationData.longitude);
					weatherData.locationName = locationData.getLocationName();
					long lat = (long) (LocationData.latitude * 1000000);
					long lon = (long) (LocationData.longitude * 1000000);
					queryString = "http://www.google.com/ig/api?weather=,,,"
							+ lat + "," + lon;
				} else {
					queryString = "http://www.google.com/ig/api?weather="
							+ Preferences.weatherCity;
					weatherData.locationName = Preferences.weatherCity;
				}
				
				// Fixed #46 (Google Weather failing when using manually input location)
				// The URL must not contain spaces. We need to replace them.
				queryString = queryString.replace(" ", "%20");       

				HttpClient hc = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(queryString);
				HttpResponse rp = hc.execute(httpGet);

				if (rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					String s = EntityUtils.toString(rp.getEntity());
					if (Preferences.logging)
						Log.d(MetaWatch.TAG, "Got weather data " + s);

					SAXParserFactory spf = SAXParserFactory.newInstance();
					SAXParser sp = spf.newSAXParser();
					XMLReader xr = sp.getXMLReader();

					GoogleWeatherHandler gwh = new GoogleWeatherHandler();
					xr.setContentHandler(gwh);
					xr.parse(new InputSource(new StringReader(s)));
					WeatherSet ws = gwh.getWeatherSet();
					if (ws == null || ws.getWeatherCurrentCondition() == null) {
						if (Preferences.logging)
							Log.e(MetaWatch.TAG,
									"Google Weather API did not respond with valid data: "
											+ s);
					} else {
						WeatherCurrentCondition wcc = ws
								.getWeatherCurrentCondition();

						ArrayList<WeatherForecastCondition> conditions = ws
								.getWeatherForecastConditions();

						int days = conditions.size();
						weatherData.forecast = new Forecast[days];

						for (int i = 0; i < days; ++i) {
							WeatherForecastCondition wfc = conditions.get(i);

							weatherData.forecast[i] = new Forecast();
							weatherData.forecast[i].setDay(wfc.getDayofWeek());
							weatherData.forecast[i].setIcon(getIcon(wfc
									.getCondition().toLowerCase()));

							if (Preferences.weatherCelsius) {
								weatherData.forecast[i].setTempHigh(wfc
										.getTempMaxCelsius());
								weatherData.forecast[i].setTempLow(wfc
										.getTempMinCelsius());
							} else {
								weatherData.forecast[i]
										.setTempHigh(WeatherUtils
												.celsiusToFahrenheit(wfc
														.getTempMaxCelsius()));
								weatherData.forecast[i].setTempLow(WeatherUtils
										.celsiusToFahrenheit(wfc
												.getTempMinCelsius()));
							}

							if (Preferences.logging)
								Log.d(MetaWatch.TAG, "Forecast #" + i + ": "
										+ weatherData.forecast[i]);
						}

						weatherData.celsius = Preferences.weatherCelsius;

						String cond = wcc.getCondition();
						weatherData.condition = cond;

						if (Preferences.weatherCelsius) {
							weatherData.temp = Integer.toString(wcc
									.getTempCelcius());
						} else {
							weatherData.temp = Integer.toString(wcc
									.getTempFahrenheit());
						}

						cond = cond.toLowerCase();

						weatherData.icon = getIcon(cond);
						weatherData.received = true;
						weatherData.timeStamp = System.currentTimeMillis();

						Idle.updateIdle(context, true);
						MetaWatchService.notifyClients();
					}
				}
			}

		} catch (Exception e) {
			if (Preferences.logging)
				Log.e(MetaWatch.TAG, "Exception while retreiving weather", e);
		} finally {
			if (Preferences.logging)
				Log.d(MetaWatch.TAG, "Monitors.updateWeatherData(): finish");
		}

		return weatherData;
	}

	
}
