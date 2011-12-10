package org.anddev.android.weatherforecast.weather;

/**
 * Holds the information between the <forecast_conditions>-tag of what the
 * Google Weather API returned.
 */
public class WeatherForecastCondition {

	// ===========================================================
	// Fields
	// ===========================================================

	private String dayofWeek = null;
	private Integer tempMinCelsius = null;
	private Integer tempMaxCelsius = null;
	private Integer tempMinFahrenheit = null;
	private Integer tempMaxFahrenheit = null;
	private String iconURL = null;
	private String condition = null;

	// ===========================================================
	// Constructors
	// ===========================================================

	public WeatherForecastCondition() {

	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public String getDayofWeek() {
		return dayofWeek;
	}

	public void setDayofWeek(String dayofWeek) {
		this.dayofWeek = dayofWeek;
	}

	public Integer getTempMinCelsius() {
		return tempMinCelsius;
	}

	public void setTempMinCelsius(Integer tempMin) {
		this.tempMinCelsius = tempMin;
		this.tempMinFahrenheit = WeatherUtils.celsiusToFahrenheit(tempMin);

	}

	public Integer getTempMaxCelsius() {
		return tempMaxCelsius;
	}

	public void setTempMaxCelsius(Integer tempMax) {
		this.tempMaxCelsius = tempMax;
		this.tempMaxFahrenheit = WeatherUtils.celsiusToFahrenheit(tempMax);
	}

	public Integer getTempMinFahrenheit() {
		return tempMinFahrenheit;
	}

	public void setTempMinFahrenheit(Integer tempMin) {
		this.tempMinFahrenheit = tempMin;
		this.tempMinCelsius = WeatherUtils.fahrenheitToCelsius(tempMin);

	}

	public Integer getTempMaxFahrenheit() {
		return tempMaxFahrenheit;
	}

	public void setTempMaxFahrenheit(Integer tempMax) {
		this.tempMaxFahrenheit = tempMax;
		this.tempMaxCelsius = WeatherUtils.fahrenheitToCelsius(tempMax);
	}

	public String getIconURL() {
		return iconURL;
	}

	public void setIconURL(String iconURL) {
		this.iconURL = iconURL;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
}