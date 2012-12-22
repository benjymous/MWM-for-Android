                                                                     
                                                                     
                                                                     
                                             
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

import org.metawatch.manager.MetaWatchService.GeolocationMode;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.weather.WeatherData;
import org.metawatch.manager.weather.WeatherEngineFactory;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.location.Location;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


public class Monitors {
	
	public static AlarmManager alarmManager;
	public static Intent intent;
	public static PendingIntent sender;
	
	static GmailMonitor gmailMonitor;
	
	private static ContentObserverMessages contentObserverMessages;
	static ContentResolver contentResolverMessages;
	
	private static ContentObserverCalls contentObserverCalls;
	static ContentResolver contentResolverCalls;
	
	private static ContentObserverAppointments contentObserverAppointments;
	static ContentResolver contentResolverAppointments;

	static Hashtable<String, Integer> gmailUnreadCounts = new Hashtable<String, Integer>();
	
	private static LocationFinder locationFinder;
	private static BroadcastReceiver locationReceiver;
	
	private static BroadcastReceiver batteryLevelReceiver;
	
	public static long calendarChangedTimestamp = 0;
	public static long getRTCTimestamp = 0;
	public static int rtcOffset = 0; // Offset in seconds to add to the RTC to allow for latency

	public static WeatherData weatherData = new WeatherData();
	
	public static class LocationData {
		public static boolean received = false;
	    public static double latitude;
	    public static double longitude;
	    
	    public static long timeStamp = 0;
	}
	
	public static class BatteryData {
		public static int level = -1;
	}
	
	public static class TouchDownData {
		public static int unreadMailCount = -1;
	}
	
	
	public static void updateGmailUnreadCount(String account, int count) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Monitors.updateGmailUnreadCount(): account='"
				+ account + "' count='" + count + "'");
		gmailUnreadCounts.put(account, count);
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"Monitors.updateGmailUnreadCount(): new unread count is: "
						+ gmailUnreadCounts.get(account));
	}
	
	public static int getGmailUnreadCount() {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Monitors.getGmailUnreadCount()");
		int totalCount = 0;
		for (String key : gmailUnreadCounts.keySet()) {
			Integer accountCount = gmailUnreadCounts.get(key);
			totalCount += accountCount.intValue();
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Monitors.getGmailUnreadCount(): account='"
					+ key + "' accountCount='" + accountCount
					+ "' totalCount='" + totalCount + "'");
		}
		return totalCount;
	}
	
	public static int getGmailUnreadCount(String account) {
		int count = gmailUnreadCounts.get(account);
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Monitors.getGmailUnreadCount('"+account+"') returning " + count);
		return count;
	}
	
	public static void start(Context context/*, TelephonyManager telephonyManager*/) {
		// start weather updater
		
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"Monitors.start()");
		
		createBatteryLevelReciever(context);
				
		if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"Initialising Geolocation");
			
			try {
				locationFinder = new LocationFinder(context);
				createLocationReceiver(context);
				RefreshLocation();
			} catch (IllegalArgumentException e) {
				if (Preferences.logging)
					Log.d(MetaWatch.TAG,"Failed to initialise Geolocation "+e.getMessage());
			}
		}
		else {
			if (Preferences.logging) Log.d(MetaWatch.TAG,"Geolocation disabled");
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
			//Uri uri = Uri.parse("content://mms-sms/conversations/");
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
		
		startAlarmTicker(context);
	}
	
	public static void RefreshLocation() {
		if (locationFinder==null)
			return;
		
		Location location = locationFinder.getLastBestKnownLocation();
		if (location!=null) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Updated location");
			
			LocationData.latitude = location.getLatitude();
			LocationData.longitude = location.getLongitude();
			
			LocationData.timeStamp = location.getTime();
			
			LocationData.received = true;
		}
	}
	
	public static void stop(Context context) {
		
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"Monitors.stop()");
		
		contentResolverMessages.unregisterContentObserver(contentObserverMessages);
		stopAlarmTicker();
		
		if (batteryLevelReceiver!=null) {
			context.unregisterReceiver(batteryLevelReceiver);
			batteryLevelReceiver=null;
		}
		
		if (locationReceiver!=null) {
			context.unregisterReceiver(locationReceiver);
			locationReceiver=null;
		}
	}
	
	public static void restart(final Context context) {
		stop(context);
		start(context);
	}
	

	public static void updateWeatherData(final Context context) {
		// Ask the location manager for the most recent location
		// as often it seems to know, without actually notifying us!
		RefreshLocation();
		
		Thread thread = new Thread("WeatherUpdater") {

			@Override
			public void run() {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Weather");
			 	wl.acquire();
			 	
			 	weatherData = WeatherEngineFactory.getEngine().update(context, weatherData);
			 	
			 	wl.release();
			}
		};
		thread.start();
	}
	
	// Force the update, by clearing the timestamps
	public static void updateWeatherDataForced(final Context context) {
		weatherData.received = false;
		weatherData.timeStamp = 0;
		weatherData.forecastTimeStamp = 0;
		updateWeatherData(context);
	}
		
	
	static void startAlarmTicker(Context context) {		
		if (Preferences.logging) Log.d(MetaWatch.TAG, "startAlarmTicker()");
		alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		intent = new Intent(context, AlarmReceiver.class);
		intent.putExtra("action_update", "update");
		sender = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 0, AlarmManager.INTERVAL_HALF_HOUR, sender);  
	}
	
	static void stopAlarmTicker() {
		alarmManager.cancel(sender);
	}
	
	private static class ContentObserverMessages extends ContentObserver {

		Context context;
		
		public ContentObserverMessages(Context context) {
			super(null);
			this.context = context;			
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);			
			// change in SMS/MMS database			
			Idle.updateIdle(context, true);
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
			if (Preferences.logging) Log.d(MetaWatch.TAG, "call history change");
			Idle.updateIdle(context, true);
		}
	}
	
	private static class ContentObserverAppointments extends ContentObserver {

		Context context;

		public ContentObserverAppointments(Context context) {
			super(null);
			this.context = context;     
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);     
			// change in calendar database
			if (Preferences.logging) Log.d(MetaWatch.TAG, "calendar change");
				calendarChangedTimestamp = System.currentTimeMillis();
				Idle.updateIdle(context, true);
			}
		}

	private static void createLocationReceiver(Context context) {
		if (locationFinder == null) return;
	
		locationReceiver = new BroadcastReceiver() {
	
			@Override
			public void onReceive(Context context, Intent intent) {
				if (!intent.hasExtra(LocationFinder.KEY_LOCATION_CHANGED)) return;
				
				try {
					Location location = (Location) intent.getExtras().get(LocationFinder.KEY_LOCATION_CHANGED);
					
					if (location != null) {
						LocationData.latitude = location.getLatitude();
						LocationData.longitude = location.getLongitude();
						
						LocationData.timeStamp = location.getTime();
						
						if (Preferences.logging) Log.d(MetaWatch.TAG, "location changed "+location.toString() );
						
						LocationData.received = true;
						MetaWatchService.notifyClients();
						
						if (!weatherData.received /*&& !WeatherData.updating*/) {
							if (Preferences.logging) Log.d(MetaWatch.TAG, "First location - getting weather");
							
							Monitors.updateWeatherData(context);
						}
					}
				} catch (java.lang.NullPointerException e) {
					if (Preferences.logging) Log.d(MetaWatch.TAG, "onLocationChanged: NullPointerException");
				}
			}
		};
		
		IntentFilter filter = new IntentFilter("org.metawatch.manager.LOCATION_CHANGE");
		context.registerReceiver(locationReceiver, filter);
	}
	

	private static void createBatteryLevelReciever(Context context) {
		if(batteryLevelReceiver!=null)
			return;
		
		batteryLevelReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				int level = -1;
				if (rawlevel >= 0 && scale > 0) {
					level = (rawlevel * 100) / scale;
				}
				if(BatteryData.level != level) {
					BatteryData.level = level;
					Idle.updateIdle(context, true);
				}
			}
		};
		context.registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}

	
}
