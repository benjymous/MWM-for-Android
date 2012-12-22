package org.metawatch.manager;

import java.util.List;

import org.metawatch.manager.MetaWatchService.Preferences;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Creates a simple location finder. It can be used to return the last best
 * known location and if that isn't good enough, request a single update.
 * 
 * It uses the Fluent pattern so you can write it like: </br></br><code>
 * &nbsp;&nbsp;LocationFinder finder = new LocationFinder()</br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.setMaxTime(5 * 60 * 1000)</br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.setMinAccuracy(100.0f)</br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;.setCriteria(criteria);</br>
 * </code></br> or</br></br><code>
 * &nbsp;&nbsp;LocationFinder finder = new LocationFinder();</br>
 * &nbsp;&nbsp;finder.setMaxTime(1 * 60 * 1000);</br>
 * </code></br> and it'll work just fine ;)
 */
public class LocationFinder {

	public static final String TAG = "LocationUpdate";

	public static final String KEY_LOCATION_CHANGED = "LOCATION_CHANGED";
	public static final String ACTION_LOCATION_CHANGE = "org.metawatch.manager.LOCATION_CHANGE";

	private final Context context;
	private final LocationManager locationManager;

	private Criteria criteria;
	private long maxTime;
	private float minAccuracy;

	public LocationFinder(Context context) {
		this.context = context;

		// Get Android's location manager
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		criteria = new Criteria();
		criteria.setPowerRequirement(Criteria.POWER_LOW);

		maxTime = 5 * 60 * 1000;
		minAccuracy = 100.0f;
	}

	public Context getContext() {
		return context;
	}

	public long getMaxTime() {
		return maxTime;
	}

	/**
	 * Sets the maximum time of a location to be fresh. When you call
	 * getLastBestKnownLocation(), if the location isn't fresh enough, the
	 * system will request a single location update with a provider that matches
	 * the current criteria.</br></br> <strong>Default:</strong> 5 * 60 * 1000
	 * 
	 * @param time
	 *            Maximum time to consider a location as fresh.
	 * @return this
	 */
	public LocationFinder setMaxTime(long time) {
		maxTime = time;
		return this;
	}

	public float getMinAccuracy() {
		return minAccuracy;
	}

	/**
	 * Sets the minimum accuracy of a location. When you call
	 * getLastBestKnownLocation(), if the location isn't accurate enough, the
	 * system will request a single location update with a provider that matches
	 * the current criteria.</br></br> <strong>Default:</strong> 100.0f
	 * 
	 * @param accuracy
	 *            Minimum accuracy for location requests.
	 * @return this
	 */
	public LocationFinder setMinAccuracy(float accuracy) {
		minAccuracy = accuracy;
		return this;
	}

	public Criteria getCriteria() {
		return criteria;
	}

	/**
	 * Criteria used when we have to request a single location update.
	 * 
	 * @param criteria
	 *            used to request a single update
	 * @return this
	 */
	public LocationFinder setCriteria(Criteria criteria) {
		this.criteria = criteria;
		return this;
	}

	/**
	 * Returns the most accurate and fresh known location. If the returned
	 * location doesn't match the minimum requirements, it'll trigger a single
	 * location update using the current criteria.
	 * 
	 * @return best known location
	 */
	public Location getLastBestKnownLocation() {
		assert (locationManager != null);

		Location bestResult = null;
		float bestAccuracy = Float.MAX_VALUE;
		long bestFreshness = Long.MAX_VALUE;

		// Iterate through all the providers on the system, keeping
		// note of the most accurate result within the acceptable time limit.
		// If no result is found within maxTime, return the newest Location.
		List<String> matchingProviders = locationManager.getAllProviders();
		long currentTime = System.currentTimeMillis();
		for (String provider : matchingProviders) {
			Location location = locationManager.getLastKnownLocation(provider);
			if (location != null) {
				float accuracy = location.getAccuracy();
				long freshness = currentTime - location.getTime();

				// We set the best result if either we haven't found one yet or
				// this is the best so far.
				if ((freshness < bestFreshness && accuracy < bestAccuracy)
						|| bestResult == null) {

					bestResult = location;
					bestAccuracy = accuracy;
					bestFreshness = freshness;
				}
			}
		}

		if (Preferences.logging) {
			String info = String
					.format("Best Known location from %s (freshness: %d secs ago, accuracy: %.2f m)",
							bestResult.getProvider(), bestFreshness / 1000,
							bestAccuracy);
			Log.d(TAG, info);
		}

		// If the best result is beyond the allowed time limit, or the accuracy
		// of the best result is wider than the acceptable minimum accuracy,
		// request a single update.
		if (bestFreshness > getMaxTime() || bestAccuracy > getMinAccuracy()) {
			if (Preferences.logging)
				Log.d(TAG,
						"LastKnownLocation isn't good enough, requesting a single update");

			String provider = locationManager.getBestProvider(criteria, true);
			if (provider != null) {
				locationManager.requestLocationUpdates(provider, 0, 0,
						singleUpdateListener);
			}
		}

		return bestResult;
	}

	protected LocationListener singleUpdateListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			assert (locationManager != null);
			assert (getContext() != null);

			if (location == null)
				return;

			if (Preferences.logging)
				Log.d(TAG, "redirecting location update");

			Intent sendIntent = new Intent(ACTION_LOCATION_CHANGE);
			sendIntent.putExtra(KEY_LOCATION_CHANGED, location);
			getContext().sendBroadcast(sendIntent);

			// Unregister from the updates (this is a single update)
			locationManager.removeUpdates(singleUpdateListener);
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

	};
}
