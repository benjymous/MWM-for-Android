package org.metawatch.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Field;

import org.metawatch.manager.MetaWatchService.Preferences;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;

public class MetaWatchAccessibilityService extends AccessibilityService {

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		AccessibilityServiceInfo asi = new AccessibilityServiceInfo();
		asi.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
				| AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
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

	private static HashMap<String, String> lastNotificationTexts = new HashMap<String, String>();

	private class ActionContent {
		Integer viewId = null;
		Integer type = null;
		Object value = null;
	}

	private void parseObjectFields(Object object, Class<?> currentClass,
			ActionContent actionContent) {
		Field innerFields[] = currentClass.getDeclaredFields();
		for (Field field : innerFields) {
			field.setAccessible(true);
			try {
				if (field.getName().equals("value")) {
					actionContent.value = field.get(object);
				} else if (field.getName().equals("type")) {
					actionContent.type = field.getInt(object);
				} else if (field.getName().equals("viewId")) {
					actionContent.viewId = field.getInt(object);
				}
			} catch (Exception e) {
				if (Preferences.logging)
					e.printStackTrace();
			}
		}
		currentClass = currentClass.getSuperclass();
		if (!currentClass.getName().equals("java.lang.Object")) {
			parseObjectFields(object, currentClass, actionContent);
		}
	}

	private SparseArray<String> parseRemoteView(RemoteViews views) {
		@SuppressWarnings("rawtypes")
		Class secretClass = views.getClass();
		SparseArray<String> texts = new SparseArray<String>();
		try {
			Field[] outerFields = secretClass.getDeclaredFields();
			for (int i = 0; i < outerFields.length; i++) {
				if (!outerFields[i].getName().equals("mActions"))
					continue;
				outerFields[i].setAccessible(true);
				@SuppressWarnings("unchecked")
				ArrayList<Object> actions = (ArrayList<Object>) outerFields[i]
						.get(views);
				for (Object action : actions) {
					ActionContent actionContent = new ActionContent();
					parseObjectFields(action, action.getClass(), actionContent);
					if ((actionContent.type != null)
							&& (actionContent.viewId != null)
							&& (actionContent.value != null)
							&& (actionContent.type == 9 || actionContent.type == 10)) {
						texts.put(actionContent.viewId,
								actionContent.value.toString());
					}
				}
			}
		} catch (Exception e) {
			if (Preferences.logging)
				e.printStackTrace();
		}
		return texts;
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {

		MetaWatchService.autoStartService(this);

		if (!accessibilityReceived) {
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
			if (Preferences.logging)
				Log.d(MetaWatch.TAG,
						"MetaWatchAccessibilityService.onAccessibilityEvent(): null package or class name");
			return;
		}

		if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			if (Preferences.logging)
				Log.d(MetaWatch.TAG,
						"MetaWatchAccessibilityService.onAccessibilityEvent(): Received event, packageName = '"
								+ packageName
								+ "' className = '"
								+ className
								+ "'");

			Parcelable p = event.getParcelableData();
			if (p instanceof android.app.Notification == false) {
				if (Preferences.logging)
					Log.d(MetaWatch.TAG,
							"MetaWatchAccessibilityService.onAccessibilityEvent(): Not a real notification, ignoring.");
				return;
			}

			android.app.Notification notification = (android.app.Notification) p;
			if (Preferences.logging)
				Log.d(MetaWatch.TAG,
						"MetaWatchAccessibilityService.onAccessibilityEvent(): notification text = '"
								+ notification.tickerText + "' flags = "
								+ notification.flags + " ("
								+ Integer.toBinaryString(notification.flags)
								+ ")");

			/*if (packageName.contains("com.google.android.keep")) {
				Log.d(MetaWatch.TAG, "keep notification received");
			}*/

			// Analyze the content views
			SparseArray<String> notificationContentTexts = parseRemoteView(notification.contentView);
			RemoteViews bigContentView = null;
			try {
				@SuppressWarnings("rawtypes")
				Class secretClass = notification.getClass();
				Field f = secretClass.getDeclaredField("bigContentView");
				bigContentView = (RemoteViews) f.get(notification);
			} catch (Exception e) {
				// bigContentView not supported, so keep the original ticker
				// text
			}
			SparseArray<String> notificationBigContentTexts = null;
			if (bigContentView != null)
				notificationBigContentTexts = parseRemoteView(bigContentView);

			// If ticker text is empty, create it from the notification contents
			String tickerText = "";
			if ((notification.tickerText == null)
					|| (notification.tickerText.toString().trim().length() == 0)) {
				if (tickerText.equals("")) {
					for (int i = 0; i < notificationContentTexts.size(); i++) {
						tickerText = tickerText + notificationContentTexts.valueAt(i) + "\n";
					}
				}
			} else
				tickerText = notification.tickerText.toString();

			if (tickerText.toString().trim().length() == 0) {
				if (Preferences.logging)
					Log.d(MetaWatch.TAG,
							"MetaWatchAccessibilityService.onAccessibilityEvent(): Empty text, ignoring.");
				return;
			}

			// Check if this notification was already shown
			String fullNotificationText = "";
			for (int i = 0; i < notificationContentTexts.size(); i++) {
				fullNotificationText += notificationContentTexts.valueAt(i);
			}
			if (notificationBigContentTexts != null) {
				for (int i = 0; i < notificationContentTexts.size(); i++) {
					fullNotificationText += notificationBigContentTexts
							.valueAt(i);
				}

			}
			if (lastNotificationTexts.containsKey(packageName)) {
				if (fullNotificationText.equals(lastNotificationTexts
						.get(packageName))) {
					if (Preferences.logging)
						Log.d(MetaWatch.TAG,
								"MetaWatchAccessibilityService.onAccessibilityEvent(): Duplicate notification, ignoring.");
					return;
				}
			}
			lastNotificationTexts.put(packageName, fullNotificationText);

			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(this);

			/* Forward calendar event */
			if (packageName.equals("com.android.calendar")) {
				if (sharedPreferences.getBoolean("NotifyCalendar", true)) {
					if (Preferences.logging)
						Log.d(MetaWatch.TAG,
								"onAccessibilityEvent(): Sending calendar event: '"
										+ tickerText + "'.");
					NotificationBuilder.createCalendar(this, tickerText);
					return;
				}
			}

			/* Forward google chat or voice event */
			if (packageName.equals("com.google.android.gsf")
					|| packageName
							.equals("com.google.android.apps.googlevoice")) {
				if (sharedPreferences.getBoolean("notifySMS", true)) {
					if (Preferences.logging)
						Log.d(MetaWatch.TAG,
								"onAccessibilityEvent(): Sending SMS event: '"
										+ tickerText + "'.");
					NotificationBuilder.createSMS(this, "Google Message",
							tickerText);
					return;
				}
			}

			/* Deezer or Spotify track notification */
			if (packageName.equals("deezer.android.app")
					|| packageName.equals("com.spotify.mobile.android.ui")) {

				String text = tickerText.trim();

				int truncatePos = text.indexOf(" - ");
				if (truncatePos > -1) {
					String artist = text.substring(0, truncatePos);
					String track = text.substring(truncatePos + 3);

					MediaControl.updateNowPlaying(this, artist, "", track,
							packageName);

					return;
				}

				return;
			}

			/* Special handling for whatsapp */
			if ((packageName.contains("com.whatsapp"))
					&& (sharedPreferences.getBoolean("notifySMS", true))) {
				if (notificationBigContentTexts != null) {
					String from = notificationBigContentTexts.valueAt(0);
					String message = "";
					String contents = "";
					for (int i = 2; i < notificationBigContentTexts.size(); i++) {
						contents += notificationBigContentTexts.valueAt(i) + "\n";
					}
					String lines[] = contents.split("\n");
					for (String line : lines) {
						message = line + "\n" + message;
					}
					message = "WhatsApp: " + notificationBigContentTexts.valueAt(1) + "\n" + message;
					NotificationBuilder.createSMS(this, from, message);
					return;
				}
			}

			/* Special handling for hangout */
			if ((packageName.contains("com.google.android.talk"))
					&& (sharedPreferences.getBoolean("notifySMS", true))) {
				if (notificationBigContentTexts != null) {
					String from = notificationBigContentTexts.valueAt(0);
					String message = "";
					String contents = "";
					for (int i = 1; i < notificationBigContentTexts.size(); i++) {
						contents += notificationBigContentTexts.valueAt(i) + "\n";
					}
					String lines[] = contents.split("\n");
					for (String line : lines) {
						message = line + "\n" + message;
					}
					message = "Hangouts:\n" + message;
					NotificationBuilder.createSMS(this, from, message);
					return;
				}
			}

			if ((notification.flags & android.app.Notification.FLAG_ONGOING_EVENT) > 0) {
				/* Ignore updates to ongoing events. */
				if (Preferences.logging)
					Log.d(MetaWatch.TAG,
							"MetaWatchAccessibilityService.onAccessibilityEvent(): Ongoing event, ignoring.");
				return;
			}

			/* Some other notification */
			if (sharedPreferences.getBoolean("NotifyOtherNotification", true)) {

				String[] appBlacklist = sharedPreferences.getString(
						"appBlacklist", OtherAppsList.DEFAULT_BLACKLIST).split(
						",");
				Arrays.sort(appBlacklist);

				/* Ignore if on blacklist */
				if (Arrays.binarySearch(appBlacklist, packageName) >= 0) {
					if (Preferences.logging)
						Log.d(MetaWatch.TAG,
								"onAccessibilityEvent(): App is blacklisted, ignoring.");
					return;
				}

				/* Get details about package */
				Bitmap icon = null;
				PackageManager pm = getPackageManager();
				PackageInfo packageInfo = null;
				String appName = null;
				try {
					packageInfo = pm.getPackageInfo(packageName.toString(), 0);
					appName = packageInfo.applicationInfo.loadLabel(pm)
							.toString();
					int iconId = notification.icon;
					icon = NotificationIconShrinker
							.shrink(pm
									.getResourcesForApplication(packageInfo.applicationInfo),
									iconId,
									packageName.toString(),
									NotificationIconShrinker.NOTIFICATION_ICON_SIZE);
				} catch (NameNotFoundException e) {
					/* OK, appName is null */
				}

				int buzzes = sharedPreferences.getInt("appVibrate_"
						+ packageName, -1);

				if (appName == null) {
					if (Preferences.logging)
						Log.d(MetaWatch.TAG,
								"onAccessibilityEvent(): Unknown app -- sending notification: '"
										+ tickerText + "'.");
					NotificationBuilder.createOtherNotification(this, icon,
							"Notification", tickerText, buzzes);
				} else {
					if (Preferences.logging)
						Log.d(MetaWatch.TAG,
								"onAccessibilityEvent(): Sending notification: app='"
										+ appName + "' notification='"
										+ tickerText + "'.");
					NotificationBuilder.createOtherNotification(this, icon,
							appName, tickerText, buzzes);
				}
			}
		} else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
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

}
