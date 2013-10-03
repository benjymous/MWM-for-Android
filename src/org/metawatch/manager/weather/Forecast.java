package org.metawatch.manager.weather;

public class Forecast {

    private String day;
    private String icon;
    private String tempHigh;
    private String tempLow;

    public String getDay() {
	return day;
    }

    public void setDay(String day) {
	this.day = day;
    }

    public String getIcon() {
	return icon;
    }

    public void setIcon(String icon) {
	this.icon = icon;
    }

    public String getTempHigh() {
	return tempHigh;
    }

    public void setTempHigh(String tempHigh) {
	this.tempHigh = tempHigh;
    }

    public String getTempLow() {
	return tempLow;
    }

    public void setTempLow(String tempLow) {
	this.tempLow = tempLow;
    }

    public void setTempHigh(Float tempHigh) {
	// Not much space on the display, so we use the
	// integer part of the float only.
	setTempHigh("" + tempHigh.intValue());
    }

    public void setTempLow(Float tempLow) {
	setTempLow("" + tempLow.intValue());
    }

    @Override
    public String toString() {
	return "Forecast [day=" + getDay() + ", icon=" + getIcon() + ", tempHigh=" + getTempHigh() + ", tempLow=" + getTempLow() + "]";
    }
}