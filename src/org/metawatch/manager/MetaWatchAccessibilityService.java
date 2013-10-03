package org.metawatch.manager;

import java.util.Arrays;

import org.metawatch.manager.MetaWatchService.Preferences;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;

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

	// ArrayList<PInfo> apps = getInstalledApps(true);
	// for (PInfo pinfo : apps) {
	// appsByPackage.put(pinfo.pname, pinfo);
	// }
    }

    private String currentActivity = "";
    public static boolean accessibilityReceived = false;

    private String lastNotificationPackage = "";
    private String lastNotificationText = "";
    private long lastNotificationWhen = 0;
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

	if (!accessibilityReceived) {
	    accessibilityReceived = true;
	}

	/* Acquire details of event. */
	int eventType = event.getEventType();

	String packageName = "";
	String className = "";
	try {
	    packageName = event.getPackageName().toString();
	    className = event.getClassName().toString();
	} catch (java.lang.NullPointerException e) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "MetaWatchAccessibilityService.onAccessibilityEvent(): null package or class name");
	    return;
	}

	if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "MetaWatchAccessibilityService.onAccessibilityEvent(): Received event, packageName = '" + packageName + "' className = '" + className + "'");

	    Parcelable p = event.getParcelableData();
	    if (p instanceof android.app.Notification == false) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchAccessibilityService.onAccessibilityEvent(): Not a real notification, ignoring.");
		return;
	    }

	    android.app.Notification notification = (android.app.Notification) p;
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "MetaWatchAccessibilityService.onAccessibilityEvent(): notification text = '" + notification.tickerText + "' flags = " + notification.flags + " (" + Integer.toBinaryString(notification.flags) + ")");

	    if (notification.tickerText == null || notification.tickerText.toString().trim().length() == 0) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchAccessibilityService.onAccessibilityEvent(): Empty text, ignoring.");
		return;
	    }

	    String tickerText = notification.tickerText.toString();

	    if (lastNotificationPackage.equals(packageName) && lastNotificationText.equals(tickerText) && lastNotificationWhen == notification.when) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchAccessibilityService.onAccessibilityEvent(): Duplicate notification, ignoring.");
		return;
	    }

	    lastNotificationPackage = packageName;
	    lastNotificationText = tickerText;
	    lastNotificationWhen = notification.when;

	    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

	    /* Forward calendar event */
	    if (packageName.equals("com.android.calendar")) {
		if (sharedPreferences.getBoolean("NotifyCalendar", true)) {
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "onAccessibilityEvent(): Sending calendar event: '" + tickerText + "'.");
		    NotificationBuilder.createCalendar(this, tickerText);
		    return;
		}
	    }

	    /* Forward google chat or voice event */
	    if (packageName.equals("com.google.android.gsf") || packageName.equals("com.google.android.apps.googlevoice")) {
		if (sharedPreferences.getBoolean("notifySMS", true)) {
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "onAccessibilityEvent(): Sending SMS event: '" + tickerText + "'.");
		    NotificationBuilder.createSMS(this, "Google Message", tickerText);
		    return;
		}
	    }

	    /* Deezer or Spotify track notification */
	    if (packageName.equals("deezer.android.app") || packageName.equals("com.spotify.mobile.android.ui")) {

		String text = tickerText.trim();

		int truncatePos = text.indexOf(" - ");
		if (truncatePos > -1) {
		    String artist = text.substring(0, truncatePos);
		    String track = text.substring(truncatePos + 3);

		    MediaControl.getInstance().updateNowPlaying(this, artist, "", track, packageName);

		    return;
		}

		return;
	    }

	    if ((notification.flags & android.app.Notification.FLAG_ONGOING_EVENT) > 0) {
		/* Ignore updates to ongoing events. */
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchAccessibilityService.onAccessibilityEvent(): Ongoing event, ignoring.");
		return;
	    }

	    /* Some other notification */
	    if (sharedPreferences.getBoolean("NotifyOtherNotification", true)) {

		String[] appBlacklist = sharedPreferences.getString("appBlacklist", OtherAppsList.DEFAULT_BLACKLIST).split(",");
		Arrays.sort(appBlacklist);

		/* Ignore if on blacklist */
		if (Arrays.binarySearch(appBlacklist, packageName) >= 0) {
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "onAccessibilityEvent(): App is blacklisted, ignoring.");
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
		    icon = NotificationIconShrinker.shrink(pm.getResourcesForApplication(packageInfo.applicationInfo), iconId, packageName.toString(), NotificationIconShrinker.NOTIFICATION_ICON_SIZE);
		} catch (NameNotFoundException e) {
		    /* OK, appName is null */
		}

		int buzzes = sharedPreferences.getInt("appVibrate_" + packageName, -1);

		if (appName == null) {
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "onAccessibilityEvent(): Unknown app -- sending notification: '" + tickerText + "'.");
		    NotificationBuilder.createOtherNotification(this, icon, "Notification", tickerText, buzzes);
		} else {
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "onAccessibilityEvent(): Sending notification: app='" + appName + "' notification='" + tickerText + "'.");
		    NotificationBuilder.createOtherNotification(this, icon, appName, tickerText, buzzes);
		}
	    }
	} else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
	    if (currentActivity.startsWith("com.fsck.k9")) {
		if (!className.startsWith("com.fsck.k9")) {
		    // User has switched away from k9, so refresh the read count
		    Utils.refreshUnreadK9Count(this);
		    Idle.getInstance().updateIdle(this, true);
		}
	    }

	    currentActivity = className;
	}
    }

    @Override
    public void onInterrupt() {
	/* Do nothing */
    }

}
