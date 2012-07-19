package org.metawatch.manager.weather;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.metawatch.manager.MetaWatch;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Monitors.LocationData;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

public abstract class AbstractWeatherEngine implements WeatherEngine {

	private final static int TIME_FIVE_MINUTES = 5 * 60 * 1000;

	/**
	 * Weather update is done frequently. Update rate can be configured here.
	 * 
	 * @param data
	 *            Weather data
	 * @return True if weather data shall be updated
	 */
	public boolean isUpdateRequired(WeatherData data) {

		if (data.timeStamp > 0 && data.received) {
			long currentTime = System.currentTimeMillis();
			long diff = currentTime - data.timeStamp;

			if (diff < TIME_FIVE_MINUTES) {
				if (Preferences.logging)
					Log.d(MetaWatch.TAG,
							"Skipping weather update - updated less than 5m ago");

				return false;
			}
		}

		return true;
	}

	protected boolean isGeolocationDataUsed() {
		return Preferences.weatherGeolocation && LocationData.received;
	}

	protected GoogleGeoCoderLocationData reverseLookupGeoLocation(
			Context context, double latitude, double longitude)
			throws IOException {
		GoogleGeoCoderLocationData locationData = new GoogleGeoCoderLocationData();
		Geocoder geocoder = new Geocoder(context, Locale.getDefault());
		List<Address> addresses = geocoder.getFromLocation(latitude, longitude,
				1);

		for (Address address : addresses) {
			if (Preferences.logging)
				Log.d(MetaWatch.TAG, "GeoCoder address data: " + address);
			
			
			if (address.getPostalCode() != null) {
				String s = address.getPostalCode().trim();
				if (!s.equals(""))
					locationData.postalcode = s;
			}

			if (address.getLocality() != null) {
				String s = address.getLocality().trim();
				if (!s.equals(""))
					locationData.locality = s;
			}
		}

		if (Preferences.logging)
			Log.d(MetaWatch.TAG, "GeoCoder location data: " + locationData);

		return locationData;
	}

	class GoogleGeoCoderLocationData {
		String locality;
		String postalcode;

		public String getLocationName() {
			if (locality != null)
				return locality;
			if (postalcode != null)
				return postalcode;
			return Preferences.weatherCity;
		}

		@Override
		public String toString() {
			return "GoogleGeoCoderLocationData [locality=" + locality
					+ ", postalcode=" + postalcode + "]";
		}
	}
}
