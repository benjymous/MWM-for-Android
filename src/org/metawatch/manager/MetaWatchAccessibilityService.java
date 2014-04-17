package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.Preferences;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.TextView;

public class MetaWatchAccessibilityService extends AccessibilityService {

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		AccessibilityServiceInfo asi = new AccessibilityServiceInfo();
		asi.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
		asi.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
		asi.flags = AccessibilityServiceInfo.DEFAULT;
		asi.notificationTimeout = 100;
		setServiceInfo(asi);
	}

	private String currentActivity = "";
	public static boolean accessibilityReceived = false;

	private static String lastNotificationPackage = "";
	private static String lastNotificationText = "";
	private static long lastNotificationWhen = 0;
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {

		MetaWatchService.autoStartService(this);
		
		if(!accessibilityReceived) {
			accessibilityReceived = true;
			MetaWatchService.notifyClients();
		}
		
		/* Acquire details of event. */
		int eventType = event.getEventType();
		
		String packageName = "";
		String className = "";
		try {
			packageName = event.getPackageName().toString();
			className = event.getClassName().toString();
		} catch (java.lang.NullPointerException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): null package or class name");
			return;
		}
				
		if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): Received event, packageName = '"
							+ packageName + "' className = '" + className + "'");
	
			Parcelable p = event.getParcelableData();
			if (p instanceof android.app.Notification == false) {
				if (Preferences.logging) Log.d(MetaWatch.TAG,
						"MetaWatchAccessibilityService.onAccessibilityEvent(): Not a real notification, ignoring.");
				return;
			}
	
			android.app.Notification notification = (android.app.Notification) p;
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): notification text = '"
							+ notification.tickerText + "' flags = "
							+ notification.flags + " ("
							+ Integer.toBinaryString(notification.flags) + ")");

			String notificationText = "";
			if (notification.tickerText != null
					&& notification.tickerText.toString().trim().length() > 0) {
				notificationText = notification.tickerText.toString().trim();
			}
			
			if (lastNotificationPackage.equals(packageName) && lastNotificationText.equals(notificationText) &&
					lastNotificationWhen == notification.when) {
				if (Preferences.logging) Log.d(MetaWatch.TAG,
						"MetaWatchAccessibilityService.onAccessibilityEvent(): Duplicate notification, ignoring.");
				return;
			}
			
			lastNotificationPackage = packageName;
			lastNotificationText = notificationText;
			lastNotificationWhen = notification.when;
	
			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(this);
	
			if (notificationText.length() > 0) {
				/* Forward calendar event */
				if (packageName.equals("com.android.calendar")) {
					if (sharedPreferences.getBoolean("NotifyCalendar", true)) {
						if (Preferences.logging) Log.d(MetaWatch.TAG,
								"onAccessibilityEvent(): Sending calendar event: '"	+ notificationText + "'.");
						NotificationBuilder.createCalendar(this, notificationText);
						return;
					}
				}
				
				/* Forward google chat or voice event */
				if (packageName.equals("com.google.android.gsf") || packageName.equals("com.google.android.apps.googlevoice")) {
					if (sharedPreferences.getBoolean("notifySMS", true)) {
						if (Preferences.logging) Log.d(MetaWatch.TAG,
								"onAccessibilityEvent(): Sending SMS event: '"
										+ notificationText + "'.");
						NotificationBuilder.createSMS(this,"Google Message", notificationText);
						return;
					}
				}
				
				/* Deezer or Spotify track notification */
				if (packageName.equals("deezer.android.app") || packageName.equals("com.spotify.mobile.android.ui")) {
					int truncatePos = notificationText.indexOf(" - ");
					if (truncatePos>-1)
					{
						String artist = notificationText.substring(0, truncatePos);
						String track = notificationText.substring(truncatePos+3);
						
						MediaControl.updateNowPlaying(this, artist, "", track, packageName);
						
						return;
					}
					
					return;
				}
			}
			
			if ((notification.flags & android.app.Notification.FLAG_ONGOING_EVENT) > 0) {
				/* Ignore updates to ongoing events. */
				if (Preferences.logging) Log.d(MetaWatch.TAG,
						"MetaWatchAccessibilityService.onAccessibilityEvent(): Ongoing event, ignoring.");
				return;
			}
			
			/* Some other notification */
			if (sharedPreferences.getBoolean("NotifyOtherNotification", true)) {
				int notificationMode = sharedPreferences.getInt("appNotification_" + packageName, 0);
	
				/* Ignore if not whitelisted */
				if ((notificationMode & OtherAppsList.MODE_WHITELIST) == 0) {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"MetaWatchAccessibilityService.onAccessibilityEvent(): App not whitelisted, ignoring.");
					return;
				}
				
				/* Read content text instead of ticker text if wanted/needed */
				if ((notificationMode & OtherAppsList.MODE_PREFER_FULL) != 0 || notificationText.length() == 0) {
					if (Preferences.logging) Log.d(MetaWatch.TAG, 
							"MetaWatchAccessibilityService.onAccessibilityEvent(): Fetching full text...");
					try {
			        	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			        		notificationText += "\n" + getExtraBigData(notification, notificationText.trim());
			        	} else {
			        		notificationText += "\n" + getExtraData(notification, notificationText.trim());
			        	}
			        	notificationText = notificationText.trim();
						if (Preferences.logging) Log.d(MetaWatch.TAG,
								"MetaWatchAccessibilityService.onAccessibilityEvent(): Full text: '" + notificationText + "'");
					} catch (Exception e) {
						if (Preferences.logging) Log.e(MetaWatch.TAG,
								"MetaWatchAccessibilityService.onAccessibilityEvent(): Fetching full text failed!");
					}
				}
				
				/* Ignore empty notifiations */
				if (notificationText.length() == 0) {
					if (Preferences.logging) Log.e(MetaWatch.TAG,
							"MetaWatchAccessibilityService.onAccessibilityEvent(): Empty text, ignoring notification.");
					return;
				}
	
				Bitmap icon = null;
				PackageManager pm = getPackageManager();
				PackageInfo packageInfo = null;
				String appName = null;
				try {
					packageInfo = pm.getPackageInfo(packageName.toString(), 0);
					appName = packageInfo.applicationInfo.loadLabel(pm).toString();
					int iconId = notification.icon;
					icon = NotificationIconShrinker.shrink(
							pm.getResourcesForApplication(packageInfo.applicationInfo),
							iconId, packageName.toString(), NotificationIconShrinker.NOTIFICATION_ICON_SIZE);
				} catch (NameNotFoundException e) {
					/* OK, appName is null */
				}
				
				boolean sticky = ((notificationMode & OtherAppsList.MODE_STICKY) != 0);
				int buzzes = sharedPreferences.getInt("appVibrate_" + packageName, -1);
	
				if (appName == null) {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"onAccessibilityEvent(): Unknown app -- sending notification: '"
									+ notificationText + "'.");
					NotificationBuilder.createOtherNotification(this, icon,
							"Notification", notificationText, buzzes, sticky);
				} else {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"onAccessibilityEvent(): Sending notification: app='"
									+ appName + "' notification='"
									+ notificationText + "'.");
					NotificationBuilder.createOtherNotification(this, icon, appName,
							notificationText, buzzes, sticky);
				}
			}
		}
		else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
		{
			if (currentActivity.startsWith("com.fsck.k9")) {
				if (!className.startsWith("com.fsck.k9")) {
					// User has switched away from k9, so refresh the read count
					Utils.refreshUnreadK9Count(this);
					Idle.updateIdle(this, true);
				}
			}
			
			currentActivity = className;
		}
	}

	@Override
	public void onInterrupt() {
		/* Do nothing */
	}
	
	/*
	 * Code below is taken from Pebble Notifier.
	 */
	
	private String getExtraData(Notification notification, String existing_text) {
		RemoteViews views = notification.contentView;
		if (views == null) {
			return "";
		}

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		try {
			ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
			views.reapply(getApplicationContext(), localView);
			return dumpViewGroup(0, localView, existing_text);
		} catch (android.content.res.Resources.NotFoundException e) {
			return "";
		}
	}

	@TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN)
	private String getExtraBigData(Notification notification, String existing_text) {
		RemoteViews views = null;
		try {
			views = notification.bigContentView;
		} catch (NoSuchFieldError e) {
			return getExtraData(notification, existing_text);
		}
		if (views == null) {
			return getExtraData(notification, existing_text);
		}
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		try {
			ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
			views.reapply(getApplicationContext(), localView);
			return dumpViewGroup(0, localView, existing_text);
		} catch (android.content.res.Resources.NotFoundException e) {
			return "";
		}
	}

	private String dumpViewGroup(int depth, ViewGroup vg, String existing_text) {
		String text = "";
		for (int i = 0; i < vg.getChildCount(); ++i) {
			View v = vg.getChildAt(i);
			if (v.getId() == android.R.id.title || v instanceof android.widget.Button
					|| v.getClass().toString().contains("android.widget.DateTimeView")) {
				if (existing_text.length() > 0 || v.getId() != android.R.id.title) {
					continue;
				}
			}

			if (v instanceof TextView) {
				TextView tv = (TextView) v;
				if (tv.getText().toString() == "..." || isInteger(tv.getText().toString())
						|| tv.getText().toString().trim().equalsIgnoreCase(existing_text)) {
					continue;
				}
				text += tv.getText().toString() + "\n";
			}
			if (v instanceof ViewGroup) {
				text += dumpViewGroup(depth + 1, (ViewGroup) v, existing_text);
			}
		}
		return text;
	}

	private boolean isInteger(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
