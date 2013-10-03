package org.metawatch.manager.weather;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.metawatch.manager.Application;
import org.metawatch.manager.Idle;
import org.metawatch.manager.Log;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchStatus;
import org.metawatch.manager.Monitors;
import org.metawatch.manager.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.Context;

public class YahooWeatherEngine extends AbstractWeatherEngine {

    public String getIcon(int code) {
	// http://developer.yahoo.com/weather/

	/*
	 * 0 tornado 1 tropical storm 2 hurricane 3 severe thunderstorms 4 thunderstorms 5 mixed rain and snow 6 mixed rain and sleet 7 mixed snow and sleet 8 freezing drizzle 9 drizzle 10 freezing rain 11 showers 12 showers 13 snow flurries 14 light snow showers 15 blowing snow 16 snow 17 hail 18 sleet 19 dust 20 foggy 21 haze 22 smoky 23 blustery 24 windy 25 cold 26 cloudy 27 mostly cloudy (night) 28 mostly cloudy (day) 29 partly cloudy (night) 30 partly cloudy (day) 31 clear (night) 32 sunny 33 fair (night) 34 fair (day) 35 mixed rain and hail 36 hot 37 isolated thunderstorms 38 scattered thunderstorms 39 scattered thunderstorms 40 scattered showers 41 heavy snow 42 scattered snow showers 43 heavy snow 44 partly cloudy 45 thundershowers 46 snow showers 47 isolated thundershowers 3200 not available
	 */

	switch (code) {
	case 0:
	case 1:
	case 2:
	case 3:
	case 4:
	    return "weather_thunderstorm.bmp";

	case 5:
	case 6:
	case 7:
	case 8:
	    return "weather_snow.bmp";

	case 9:
	case 10:
	case 11:
	case 12:
	    return "weather_rain.bmp";

	case 13:
	case 14:
	case 15:
	case 16:
	case 17:
	case 18:
	    return "weather_snow.bmp";

	case 19:
	case 20:
	case 21:
	case 22:
	    return "weather_fog.bmp";

	case 23:
	case 24:
	case 25:
	case 26:
	    return "weather_cloudy.bmp";
	case 27:
	    return "weather_nt_partlycloudy.bmp";
	case 28:
	    return "weather_cloudy.bmp";
	case 29:
	    return "weather_nt_partlycloudy.bmp";
	case 30:
	    return "weather_partlycloudy.bmp";
	case 31:
	    return "weather_nt_clear.bmp";
	case 32:
	    return "weather_sunny.bmp";
	case 33:
	    return "weather_nt_clear.bmp";
	case 34:
	    return "weather_sunny.bmp";
	case 35:
	    return "weather_rain.bmp";
	case 36:
	    return "weather_sunny.bmp";
	case 37:
	case 38:
	case 39:
	    return "weather_thunderstorm.bmp";
	case 40:
	    return "weather_rain.bmp";
	case 41:
	case 42:
	case 43:
	    return "weather_snow.bmp";
	case 44:
	    return "weather_partlycloudy.bmp";
	case 45:
	    return "weather_thunderstorm.bmp";
	case 46:
	    return "weather_snow.bmp";
	case 47:
	    return "weather_thunderstorm.bmp";
	default:
	    return "weather_cloudy.bmp";// Default in other impls so far
	}
    }

    public synchronized WeatherData update(Context context, WeatherData weatherData) {
	try {
	    if (isUpdateRequired(weatherData)) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Monitors.updateWeatherDataYahoo(): start");

		// http://developer.yahoo.com/geo/placefinder/guide/requests.html#gflags-parameter
		// The problem is when we do not use the "gflags=R" argument, we
		// don't always get a WOEID
		String arguments = "&count=1&gflags=R";

		String placeFinderUrl = null;
		if (isGeolocationDataUsed()) {
		    placeFinderUrl = "http://where.yahooapis.com/geocode?q=" + Monitors.getInstance().mLocationData.latitude + "," + Monitors.getInstance().mLocationData.longitude + arguments;
		} else {
		    String weatherLocation = Preferences.weatherCity.replace(" ", "%20");
		    placeFinderUrl = "http://where.yahooapis.com/geocode?q=" + weatherLocation + arguments;
		}

		weatherData = requestWeatherFromYahooPlacefinder(placeFinderUrl, weatherData);

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

    /**
     * Checks the YAHOO PLACEFINDER service to lookup WOED, an unique weather location id. This is required to access the weather service later on.
     */
    private WeatherData requestWeatherFromYahooPlacefinder(String placeFinderUrl, WeatherData weatherData) throws IOException {
	try {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Placefinder URL: " + placeFinderUrl);

	    // Ask YAHOO PLACEFINDER to search the WOEID.
	    HttpClient hc = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(placeFinderUrl);
	    HttpResponse rp = hc.execute(httpGet);
	    if (rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();

		String s = EntityUtils.toString(rp.getEntity());
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Got placefinder response " + s);

		YahooPlacefinderHandler handler = new YahooPlacefinderHandler();
		xr.setContentHandler(handler);
		xr.parse(new InputSource(new StringReader(s)));
		String woeId = handler.getWoeId();
		String city = handler.getCity();

		// if (Preferences.weatherGeolocation)
		weatherData.locationName = city;
		// else
		// weatherData.locationName = Preferences.weatherCity;

		// DEBUG - WILL BE REMOVED AFTER SOME TEST PERIOD
		// String tt = new SimpleDateFormat("hh:mm").format(new Date());
		// weatherData.locationName = tt + " " + city;
		// DEBUG

		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Got WOEID: " + woeId + " and CITY: " + city);

		// Seconds web service access, now with WOEID
		return requestWeatherFromWoeId(woeId, weatherData);

	    } else {
		throw new IOException("Placefinder failed: " + rp.getStatusLine());
	    }
	} catch (SAXException e) {
	    throw Utils.createCompatibleIOException(e);
	} catch (ParserConfigurationException e) {
	    throw Utils.createCompatibleIOException(e);
	}
    }

    /**
     * YAHOO Weather API. Checkout documentation: http://developer.yahoo.com/weather/.
     * 
     * To request the weather from "DALLAS,US" the WOEID (Where On Earth ID) is "2388929". http://weather.yahooapis.com/forecastrss?w=2388929
     * 
     * If temperature shall be returned in CELSIUS and all units in metric system, an argument "u=c" shall be sent to the API.
     * 
     * @param woeId
     *            Where On Earth ID of YAHOO web services
     * @param weatherData
     * @return
     * @throws IOException
     */
    private WeatherData requestWeatherFromWoeId(String woeId, WeatherData weatherData) throws IOException {
	try {
	    String url = "http://weather.yahooapis.com/forecastrss?w=" + woeId;
	    if (Preferences.weatherCelsius) {
		url += "&u=c";
	    }
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Weather URL: " + url);

	    // Ask Yahoo Weather API
	    HttpClient hc = new DefaultHttpClient();
	    HttpGet httpGet = new HttpGet(url);
	    HttpResponse rp = hc.execute(httpGet);
	    if (rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();

		String s = EntityUtils.toString(rp.getEntity());
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Got Weather API response " + s);

		YahooWeatherHandler handler = new YahooWeatherHandler();
		xr.setContentHandler(handler);
		xr.parse(new InputSource(new StringReader(s)));

		weatherData.ageOfMoon = 0; // TODO
		weatherData.celsius = Preferences.weatherCelsius;
		weatherData.condition = handler.getText();
		weatherData.temp = handler.getTemp();
		weatherData.forecastTimeStamp = System.currentTimeMillis();
		weatherData.timeStamp = weatherData.forecastTimeStamp;
		weatherData.icon = getIcon(handler.getCode());
		List<Forecast> forecasts = handler.getForecasts();
		weatherData.forecast = new Forecast[forecasts.size()];
		forecasts.toArray(weatherData.forecast);
		weatherData.received = true;

		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Got weather data: " + weatherData);

		return weatherData;

	    } else {
		throw new IOException("Placefinder failed: " + rp.getStatusLine());
	    }
	} catch (SAXException e) {
	    throw Utils.createCompatibleIOException(e);
	} catch (ParserConfigurationException e) {
	    throw Utils.createCompatibleIOException(e);
	}

    }

    class YahooPlacefinderHandler extends DefaultHandler {

	boolean gatheringWoeId = false;
	boolean gatheringCity = false;
	String woeId = "";
	String city = "";

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
	    if (localName.equals("woeid"))
		gatheringWoeId = true;
	    else if (localName.equals("city"))
		gatheringCity = true;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
	    if (localName.equals("woeid"))
		gatheringWoeId = false;
	    else if (localName.equals("city"))
		gatheringCity = false;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
	    if (gatheringWoeId)
		woeId = new String(ch, start, length);
	    else if (gatheringCity)
		city = new String(ch, start, length);
	}

	public String getCity() {
	    return city;
	}

	public String getWoeId() {
	    return woeId;
	}

    }

    class YahooWeatherHandler extends DefaultHandler {
	String text = "";
	int code = 3200; // Default 3200 = "not available"
	String temp = "";
	List<Forecast> forecasts = new ArrayList<Forecast>();

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
	    // if (Preferences.logging)
	    // Log.d(MetaWatchStatus.TAG, "startElement " + localName);

	    if (localName.equals("condition")) {
		// <yweather:condition text="Partly Cloudy" code="30" temp="64"
		// date="Wed, 11 Jul 2012 10:19 am CEST" />
		text = attributes.getValue("text");
		code = parseCode(attributes.getValue("code"));
		temp = attributes.getValue("temp");

		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Weather Condition " + text + " " + code + " " + temp);

	    } else if (localName.equals("forecast")) {
		// <yweather:forecast day="Wed" date="11 Jul 2012" low="55"
		// high="69" text="Few Showers" code="11" />
		Forecast fc = new Forecast();
		fc.setTempLow(attributes.getValue("low"));
		fc.setTempHigh(attributes.getValue("high"));
		fc.setDay(attributes.getValue("day"));
		fc.setIcon(getIcon(parseCode(attributes.getValue("code"))));
		forecasts.add(fc);
	    }
	}

	public int getCode() {
	    return code;
	}

	public String getTemp() {
	    return temp;
	}

	public String getText() {
	    return text;
	}

	public List<Forecast> getForecasts() {
	    return forecasts;
	}

    }

    private static int parseCode(String code) {
	try {
	    return Integer.parseInt(code.trim());
	} catch (Exception e) {
	    return 3200;
	}
    }

}
