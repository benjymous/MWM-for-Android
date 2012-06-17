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
	private Float tempMin = null;
	private Float tempMax = null;
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

	public Float getTempMinCelsius() {
		return tempMin;
	}

	public void setTempMinCelsius(Float tempMin) {
		this.tempMin = tempMin;
	}

	public Float getTempMaxCelsius() {
		return tempMax;
	}

	public void setTempMaxCelsius(Float tempMax) {
		this.tempMax = tempMax;
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