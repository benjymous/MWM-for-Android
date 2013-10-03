package org.metawatch.manager.weather;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WeatherProvider;
import org.metawatch.manager.MetaWatchStatus;

import org.metawatch.manager.Log;

public class WeatherEngineFactory {

    private static int currentEngineId = -1;
    private static WeatherEngine currentEngine = null;

    private static WeatherEngine createEngine() {
	int engineId = Preferences.weatherProvider;
	switch (engineId) {

	case WeatherProvider.WUNDERGROUND:
	    return new WunderWeatherEngine();

	case WeatherProvider.GOOGLE_DEPRECATED:
	case WeatherProvider.YAHOO:
	    return new YahooWeatherEngine();

	default:
	    return new DummyWeatherEngine();
	}
    }

    public static synchronized WeatherEngine getEngine() {
	int engineId = Preferences.weatherProvider;
	if (currentEngine == null || engineId != currentEngineId) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Creating new weather engine with id " + engineId);

	    currentEngineId = engineId;
	    currentEngine = createEngine();
	}
	return currentEngine;
    }

}
