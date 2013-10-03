/*****************************************************************************
 *  Copyright (c) 2011 Meta Watch Ltd.                                       *
 *  www.MetaWatch.org                                                        *
 *                                                                           *
 =============================================================================
 *                                                                           *
 *  Licensed under the Apache License, Version 2.0 (the "License");          *
 *  you may not use this file except in compliance with the License.         *
 *  You may obtain a copy of the License at                                  *
 *                                                                           *
 *    http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                           *
 *  Unless required by applicable law or agreed to in writing, software      *
 *  distributed under the License is distributed on an "AS IS" BASIS,        *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 *  See the License for the specific language governing permissions and      *
 *  limitations under the License.                                           *
 *                                                                           *
 *****************************************************************************/

/*****************************************************************************
 * Monitors.java                                                             *
 * Monitors                                                                  *
 * Starting notifications and updates                                        *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import java.util.Hashtable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.metawatch.manager.MetaWatchService.GeolocationMode;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.weather.WeatherData;
import org.metawatch.manager.weather.WeatherEngineFactory;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.location.Location;
import android.net.Uri;
import android.os.BatteryManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class Monitors {

    private Executor mExecutor = Executors.newSingleThreadExecutor();
    
    GmailMonitor gmailMonitor;

    private ContentObserverMessages contentObserverMessages;
    ContentResolver contentResolverMessages;

    private ContentObserverCalls contentObserverCalls;
    ContentResolver contentResolverCalls;

    private ContentObserverAppointments contentObserverAppointments;
    ContentResolver contentResolverAppointments;

    Hashtable<String, Integer> gmailUnreadCounts = new Hashtable<String, Integer>();

    private LocationFinder locationFinder;
    private BroadcastReceiver locationReceiver;

    private BroadcastReceiver batteryLevelReceiver;

    public long calendarChangedTimestamp = 0;
    public long getRTCTimestamp = 0;
    public int rtcOffset = 0; // Offset in seconds to add to the RTC to
				     // allow for latency

    public WeatherData weatherData = new WeatherData();

    private static Monitors mInstance;
    
    private Monitors(){}
    
    public static Monitors getInstance() {
	if (mInstance == null)
	    mInstance = new Monitors();
	return mInstance;
    }
    
    public void destroy(Context context) {
	stop(context);
	mInstance = null;
    }
    
    public LocationData mLocationData = new LocationData();
    
    public class LocationData {
	public boolean received = false;
	public double latitude;
	public double longitude;

	public long timeStamp = 0;
    }
    
    public BatteryData mBatteryData = new BatteryData();

    public static class BatteryData {
	public int level = -1;
    }
    
    public TouchDownData mTouchDownData = new TouchDownData();

    public class TouchDownData {
	public int unreadMailCount = -1;
    }

    public void updateGmailUnreadCount(String account, int count) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Monitors.updateGmailUnreadCount(): account='" + account + "' count='" + count + "'");
	gmailUnreadCounts.put(account, count);
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Monitors.updateGmailUnreadCount(): new unread count is: " + gmailUnreadCounts.get(account));
    }

    public int getGmailUnreadCount() {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Monitors.getGmailUnreadCount()");
	int totalCount = 0;
	for (String key : gmailUnreadCounts.keySet()) {
	    Integer accountCount = gmailUnreadCounts.get(key);
	    totalCount += accountCount.intValue();
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Monitors.getGmailUnreadCount(): account='" + key + "' accountCount='" + accountCount + "' totalCount='" + totalCount + "'");
	}
	return totalCount;
    }

    public int getGmailUnreadCount(String account) {
	int count = gmailUnreadCounts.get(account);
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Monitors.getGmailUnreadCount('" + account + "') returning " + count);
	return count;
    }

    public void start(Context context) {

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Monitors.start()");

	createBatteryLevelReciever(context);

	if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Initialising Geolocation");

	    try {
		locationFinder = new LocationFinder(context);
		createLocationReceiver(context);
		RefreshLocation();
	    } catch (IllegalArgumentException e) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Failed to initialise Geolocation " + e.getMessage());
	    }
	} else {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Geolocation disabled");
	}

	CallStateListener phoneListener = new CallStateListener(context);

	TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	int phoneEvents = PhoneStateListener.LISTEN_CALL_STATE | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR;
	telephonyManager.listen(phoneListener, phoneEvents);

	gmailMonitor = Utils.getGmailMonitor(context);
	if (gmailMonitor != null) {
	    gmailMonitor.startMonitor();
	}

	try {
	    contentObserverMessages = new ContentObserverMessages(context);
	    Uri uri = Uri.parse("content://mms-sms/conversations/");
	    contentResolverMessages = context.getContentResolver();
	    contentResolverMessages.registerContentObserver(uri, true, contentObserverMessages);
	} catch (Exception x) {
	}

	try {
	    contentObserverCalls = new ContentObserverCalls(context);
	    // Uri uri = Uri.parse("content://mms-sms/conversations/");
	    contentResolverCalls = context.getContentResolver();
	    contentResolverCalls.registerContentObserver(android.provider.CallLog.Calls.CONTENT_URI, true, contentObserverCalls);
	} catch (Exception x) {
	}

	try {
	    contentObserverAppointments = new ContentObserverAppointments(context);
	    Uri uri = Uri.parse("content://com.android.calendar/calendars/");
	    contentResolverAppointments = context.getContentResolver();
	    contentResolverAppointments.registerContentObserver(uri, true, contentObserverAppointments);
	} catch (Exception x) {
	}

	// temporary one time update
	updateWeatherData(context);
    }

    public void RefreshLocation() {
	if (locationFinder == null)
	    return;

	Location location = null;
	try {
	    location = locationFinder.getLastBestKnownLocation();
	} catch (NullPointerException e) {
	    e.printStackTrace();
	}
	if (location != null) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Updated location");

	    mLocationData.latitude = location.getLatitude();
	    mLocationData.longitude = location.getLongitude();

	    mLocationData.timeStamp = location.getTime();

	    mLocationData.received = true;
	}
    }

    public void stop(Context context) {

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Monitors.stop()");

	if (contentResolverMessages != null && contentObserverMessages != null)
	    contentResolverMessages.unregisterContentObserver(contentObserverMessages);

	if (batteryLevelReceiver != null) {
	    context.unregisterReceiver(batteryLevelReceiver);
	    batteryLevelReceiver = null;
	}

	if (locationReceiver != null) {
	    context.unregisterReceiver(locationReceiver);
	    locationReceiver = null;
	}
    }

    public void restart(final Context context) {
	stop(context);
	start(context);
    }

    public void updateWeatherData(final Context context) {
	// Ask the location manager for the most recent location
	// as often it seems to know, without actually notifying us!
	RefreshLocation();

	mExecutor.execute(new Runnable() {
	    @Override
	    public void run() {
		weatherData = WeatherEngineFactory.getEngine().update(context, weatherData);
	    }
	});
    }

    // Force the update, by clearing the timestamps
    public void updateWeatherDataForced(final Context context) {
	weatherData.received = false;
	weatherData.timeStamp = 0;
	weatherData.forecastTimeStamp = 0;
	updateWeatherData(context);
    }

    private class ContentObserverMessages extends ContentObserver {

	Context context;

	public ContentObserverMessages(Context context) {
	    super(null);
	    this.context = context;
	}

	@Override
	public void onChange(boolean selfChange) {
	    super.onChange(selfChange);
	    // change in SMS/MMS database
	    Idle.getInstance().updateIdle(context, true);
	}
    }

    private static class ContentObserverCalls extends ContentObserver {

	Context context;

	public ContentObserverCalls(Context context) {
	    super(null);
	    this.context = context;
	}

	@Override
	public void onChange(boolean selfChange) {
	    super.onChange(selfChange);
	    // change in call history database
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "call history change");
	    Idle.getInstance().updateIdle(context, true);
	}
    }

    private class ContentObserverAppointments extends ContentObserver {

	Context context;

	public ContentObserverAppointments(Context context) {
	    super(null);
	    this.context = context;
	}

	@Override
	public void onChange(boolean selfChange) {
	    super.onChange(selfChange);
	    // change in calendar database
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "calendar change");
	    calendarChangedTimestamp = System.currentTimeMillis();
	    Idle.getInstance().updateIdle(context, true);
	}
    }

    private void createLocationReceiver(Context context) {
	if (locationFinder == null)
	    return;

	locationReceiver = new BroadcastReceiver() {

	    @Override
	    public void onReceive(Context context, Intent intent) {
		if (!intent.hasExtra(LocationFinder.KEY_LOCATION_CHANGED))
		    return;

		try {
		    Location location = (Location) intent.getExtras().get(LocationFinder.KEY_LOCATION_CHANGED);

		    if (location != null) {
			mLocationData.latitude = location.getLatitude();
			mLocationData.longitude = location.getLongitude();

			mLocationData.timeStamp = location.getTime();

			if (Preferences.logging)
			    Log.d(MetaWatchStatus.TAG, "location changed " + location.toString());

			mLocationData.received = true;

			if (!weatherData.received /* && !WeatherData.updating */) {
			    if (Preferences.logging)
				Log.d(MetaWatchStatus.TAG, "First location - getting weather");

			    updateWeatherData(context);
			}
		    }
		} catch (java.lang.NullPointerException e) {
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "onLocationChanged: NullPointerException");
		}
	    }
	};

	IntentFilter filter = new IntentFilter("org.metawatch.manager.LOCATION_CHANGE");
	context.registerReceiver(locationReceiver, filter);
    }

    private void createBatteryLevelReciever(Context context) {
	if (batteryLevelReceiver != null)
	    return;

	batteryLevelReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
		int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		int level = -1;
		if (rawlevel >= 0 && scale > 0) {
		    level = (rawlevel * 100) / scale;
		}
		if (mBatteryData.level != level) {
		    mBatteryData.level = level;
		    Idle.getInstance().updateIdle(context, true);
		}
	    }
	};
	context.registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

}
