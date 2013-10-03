package org.metawatch.manager.weather;

import java.util.Arrays;

public class WeatherData {
    public boolean received = false;
    public String icon;
    public String temp;
    public String condition;
    public String locationName;
    public boolean celsius = false;

    public int sunriseH = 7;
    public int sunriseM = 0;

    public int sunsetH = 19;
    public int sunsetM = 0;

    public int moonPercentIlluminated = -1;
    public int ageOfMoon = -1;

    public Forecast[] forecast = null;

    public long timeStamp = 0;
    public long forecastTimeStamp = 0;

    @Override
    public String toString() {
	return "WeatherData [received=" + received + ", icon=" + icon + ", temp=" + temp + ", condition=" + condition + ", locationName=" + locationName + ", celsius=" + celsius + ", sunriseH=" + sunriseH + ", sunriseM=" + sunriseM + ", sunsetH=" + sunsetH + ", sunsetM=" + sunsetM + ", moonPercentIlluminated=" + moonPercentIlluminated + ", ageOfMoon=" + ageOfMoon + ", forecast=" + Arrays.toString(forecast) + ", timeStamp=" + timeStamp + ", forecastTimeStamp=" + forecastTimeStamp + "]";
    }

    public boolean error = false;
    public String errorString = "";

}