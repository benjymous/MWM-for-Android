package org.anddev.android.weatherforecast.weather;

/** Useful Utility in working with temperatures. (conversions). */
public class WeatherUtils {

	public static Float fahrenheitToCelsius(float tFahrenheit) {
		return ((5.0f / 9.0f) * (tFahrenheit - 32));
	}

	public static Float celsiusToFahrenheit(float tCelsius) {
		return ((9.0f / 5.0f) * tCelsius + 32);
	}
}