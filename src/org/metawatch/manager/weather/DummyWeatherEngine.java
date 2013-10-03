package org.metawatch.manager.weather;

import android.content.Context;

public class DummyWeatherEngine extends AbstractWeatherEngine {

    public WeatherData update(Context context, WeatherData data) {
	return data;
    }

}
