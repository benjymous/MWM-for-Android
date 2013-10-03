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
 * NotificationBuilder.java                                                  *
 * NotificationBuilder                                                       *
 * Templates for different kinds of notification screens                     *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.metawatch.manager.FontCache.FontInfo;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchType;
import org.metawatch.manager.Notification.VibratePattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.format.DateFormat;

public class NotificationBuilder {

    public static final String DEFAULT_NUMBER_OF_BUZZES = "3";

    public static VibratePattern createVibratePatternFromBuzzes(int numberOfBuzzes) {
	return new VibratePattern((numberOfBuzzes > 0), 500, 500, numberOfBuzzes);
    }

    public static VibratePattern createVibratePatternFromPreference(Context context, String preferenceName) {
	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	String buzzPref = sharedPreferences.getString(preferenceName, DEFAULT_NUMBER_OF_BUZZES);
	return createVibratePatternFromBuzzes(Integer.parseInt(buzzPref));
    }

    public static void createSMS(Context context, String number, String text) {
	String name = Utils.getContactNameFromNumber(context, number);
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsSMSNumberBuzzes");
	Bitmap icon = Utils.getBitmap(context, "message.bmp");
	String description = "SMS: " + name;
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    if (Preferences.stickyNotifications && !number.equals("Google Chat")) {
		Bitmap[] bitmaps = smartNotify(context, icon, name, text);
		Notification.getInstance().addBitmapNotification(context, bitmaps, vibratePattern, -1, description);
	    } else {
		Bitmap bitmap = smartLines(context, icon, "SMS from", new String[] { name });
		if (Preferences.smsWithAlert) {
		    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, 4000, description);
		    vibratePattern = Notification.VibratePattern.NO_VIBRATE;
		}
		Notification.getInstance().addTextNotification(context, name + ": " + text, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context));
	    }
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, text, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, "SMS from"), Protocol.getInstance(context).createOled2lines(context, name, text), scroll, len, vibratePattern, description);
	}
    }

    public static void createMMS(Context context, String number) {
	String name = Utils.getContactNameFromNumber(context, number);
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsSMSNumberBuzzes");
	Bitmap icon = Utils.getBitmap(context, "message.bmp");
	String description = "MMS: " + name;
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "MMS from", new String[] { name });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, name, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, "MMS from"), Protocol.getInstance(context).createOled2lines(context, name, ""), scroll, len, vibratePattern, description);
	}
    }

    public static void createSmart(Context context, String title, String text) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsOtherNotificationNumberBuzzes");
	createSmart(context, title, text, null, true, vibratePattern);
    }

    public static void createSmart(Context context, String title, String text, Bitmap icon, boolean sticky, VibratePattern vibratePattern) {
	if (icon == null) {
	    icon = Utils.getBitmap(context, "notify.bmp");
	}
	String description = "Smart: " + text;
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap[] bitmaps;
	    if (sticky) {
		bitmaps = smartNotify(context, icon, title, text);
	    } else {
		bitmaps = new Bitmap[] { smartLines(context, icon, title, new String[] { text }) };
	    }
	    Notification.getInstance().addBitmapNotification(context, bitmaps, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, text, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, title), Protocol.getInstance(context).createOled2lines(context, title, text), scroll, len, vibratePattern, description);
	}
    }

    public static void createK9(Context context, String sender, String subject, String folder) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsK9NumberBuzzes");
	Bitmap icon = Utils.getBitmap(context, "email.bmp");
	String description = "K9: " + sender;
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "K9 mail", new String[] { sender, subject, folder });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, subject, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, "K9 mail"), Protocol.getInstance(context).createOled2lines(context, sender, subject), scroll, len, vibratePattern, description);
	}
    }

    public static void createGmail(Context context, String sender, String email, String subject, String snippet) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsGmailNumberBuzzes");
	Bitmap icon = Utils.getBitmap(context, "gmail.bmp");
	String description = "Gmail: " + sender;
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "Gmail", new String[] { sender, email, subject });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	    Notification.getInstance().addTextNotification(context, snippet, Notification.VibratePattern.NO_VIBRATE, Notification.getInstance().getDefaultNotificationTimeout(context));
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, snippet, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled2lines(context, "Gmail from " + sender, email), Protocol.getInstance(context).createOled2lines(context, subject, snippet), scroll, len, vibratePattern, description);
	}
    }

    public static void createGmailBlank(Context context, String recipient, int count) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsGmailNumberBuzzes");
	String messages = count + " new " + (count == 1 ? "message" : "messages");
	Bitmap icon = Utils.getBitmap(context, "gmail.bmp");
	String description = "Gmail: unread " + count;
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "Gmail", new String[] { messages, recipient });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, recipient, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, " Gmail"), Protocol.getInstance(context).createOled2lines(context, messages, recipient), scroll, len, vibratePattern, description);
	}
    }

    public static void createTouchdownMail(Context context, String title, String ticker) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsTDNumberBuzzes");
	Bitmap icon = Utils.getBitmap(context, "email.bmp");
	String description = "TouchDown: " + title;
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "TouchDown", new String[] { title, ticker });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, ticker, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, "TouchDown"), Protocol.getInstance(context).createOled2lines(context, title, ticker), scroll, len, vibratePattern, description);
	}
    }

    public static void createCalendar(Context context, String text) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsCalendarNumberBuzzes");
	Bitmap icon = Utils.getBitmap(context, "calendar.bmp");
	String description = "Cal: " + text;
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "Calendar", new String[] { text });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, text, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, "  Calendar"), Protocol.getInstance(context).createOled2lines(context, "Event Reminder:", text), scroll, len, vibratePattern, description);
	}
    }

    public static void createAlarm(Context context) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsAlarmNumberBuzzes");
	final Calendar t = Calendar.getInstance();
	final String currentTime = DateFormat.getTimeFormat(context).format(t.getTime());
	Bitmap icon = Utils.getBitmap(context, "timer.bmp");
	String description = "Alarm";
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "Alarm", new String[] { currentTime }, FontCache.FontSize.LARGE);
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, "Alarm"), Protocol.getInstance(context).createOled1line(context, null, currentTime), null, 0, vibratePattern, description);
	}
    }

    public static void createMusic(Context context, String artist, String track, String album) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsMusicNumberBuzzes");
	Bitmap icon = Utils.getBitmap(context, "play.bmp");
	String description = "Music: " + track;
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "Music", new String[] { track, album, artist });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, track, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, artist), Protocol.getInstance(context).createOled2lines(context, album, track), scroll, len, vibratePattern, description);
	}
    }

    public static void createTimezonechange(Context context) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsTimezoneNumberBuzzes");
	TimeZone tz = TimeZone.getDefault();
	Bitmap icon = Utils.getBitmap(context, "timezone.bmp");
	String description = "Timezone Changed";
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "Timezone", new String[] { "Timezone Changed", tz.getDisplayName() });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, "Timezone"), Protocol.getInstance(context).createOled1line(context, null, tz.getDisplayName()), null, 0, vibratePattern, description);
	}
    }

    public static void createOtherNotification(Context context, Bitmap icon, String appName, String notificationText, int buzzes) {
	VibratePattern vibratePattern;
	if (buzzes != -1) {
	    vibratePattern = createVibratePatternFromBuzzes(buzzes);
	} else {
	    vibratePattern = createVibratePatternFromPreference(context, "settingsOtherNotificationNumberBuzzes");
	}
	if (icon == null) {
	    icon = Utils.getBitmap(context, "notify.bmp");
	}
	String description = appName;
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, appName, new String[] { notificationText });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, notificationText, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, appName), Protocol.getInstance(context).createOled2lines(context, "Notification", notificationText), scroll, len, vibratePattern, description);
	}
    }

    public static void createWinamp(Context context, String artist, String track, String album) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsMusicNumberBuzzes");
	Bitmap icon = Utils.getBitmap(context, "winamp.bmp");
	String description = "Winamp: " + track;
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "Winamp", new String[] { track, album, artist });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, track, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, artist), Protocol.getInstance(context).createOled2lines(context, album, track), scroll, len, vibratePattern, description);
	}
    }

    public static void createBatterylow(Context context) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsBatteryNumberBuzzes");
	StringBuilder builder = new StringBuilder();
	builder.append(Monitors.getInstance().mBatteryData.level);
	builder.append("%");
	String description = "Battery low";
	Bitmap icon = Utils.getBitmap(context, "batterylow.bmp");
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "Battery", new String[] { "Phone battery at", builder.toString() });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, "Warning!"), Protocol.getInstance(context).createOled2lines(context, "Phone battery at", builder.toString()), null, 0, vibratePattern, description);
	}
    }

    public static void createNMA(Context context, String appName, String event, String desc, int prio, String url) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsNMANumberBuzzes");
	Bitmap icon = Utils.getBitmap(context, "notifymyandroid.bmp");
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, appName, new String[] { event, desc });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), appName + ": " + event);
	} else {
	    byte[] scroll = new byte[800];
	    int len = Protocol.getInstance(context).createOled2linesLong(context, desc, scroll);
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, appName), Protocol.getInstance(context).createOled2lines(context, event, desc), scroll, len, vibratePattern, appName + ": " + event);
	}
    }

    public static void createNewVoicemail(Context context) {
	VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsVoicemailNumberBuzzes");
	String description = "New Voicemail";
	Bitmap icon = Utils.getBitmap(context, "voicemail.bmp");
	if (MetaWatchService.watchType == WatchType.DIGITAL) {
	    Bitmap bitmap = smartLines(context, icon, "Voicemail", new String[] { "You have", "new voicemail" });
	    Notification.getInstance().addBitmapNotification(context, bitmap, vibratePattern, Notification.getInstance().getDefaultNotificationTimeout(context), description);
	} else {
	    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled1line(context, icon, "Voicemail"), Protocol.getInstance(context).createOled2lines(context, "You have", "new voicemail"), null, 0, vibratePattern, description);
	}
    }

    static Bitmap smartLines(Context context, Bitmap icon, String header, String[] lines) {
	return smartLines(context, icon, header, lines, FontCache.FontSize.AUTO);
    }

    static Bitmap smartLines(Context context, Bitmap icon, String header, String[] lines, FontCache.FontSize size) {

	Properties props = BitmapCache.getInstance().getProperties(context, "notification.xml");

	final int textTop = Integer.parseInt(props.getProperty("textTop", "24"));
	final int textLeft = Integer.parseInt(props.getProperty("textLeft", "3"));
	final int textWidth = Integer.parseInt(props.getProperty("textWidth", "90"));
	final int textHeight = Integer.parseInt(props.getProperty("textHeight", "69"));

	final int iconTop = Integer.parseInt(props.getProperty("iconTop", "0"));
	final int iconLeft = Integer.parseInt(props.getProperty("iconLeft", "0"));

	final int headerLeft = Integer.parseInt(props.getProperty("headerLeft", "20"));
	final int headerBaseline = Integer.parseInt(props.getProperty("headerBaseline", "15"));

	final int headerColor = props.getProperty("headerColor", "white").equalsIgnoreCase("black") ? Color.BLACK : Color.WHITE;
	final int textColor = props.getProperty("textColor", "black").equalsIgnoreCase("black") ? Color.BLACK : Color.WHITE;

	FontInfo font = FontCache.instance(context).Get(size);

	Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);

	Paint paintHead = new Paint();
	paintHead.setColor(headerColor);
	paintHead.setTextSize(FontCache.instance(context).Large.size);
	paintHead.setTypeface(FontCache.instance(context).Large.face);

	Paint paint = new Paint();
	paint.setColor(textColor);
	paint.setTextSize(font.size);
	paint.setTypeface(font.face);

	canvas.drawColor(Color.WHITE);

	canvas.drawBitmap(Utils.getBitmap(context, "notify_background.png"), 0, 0, null);

	int iconHeight = 16;
	if (icon != null) {
	    iconHeight = Math.max(16, icon.getHeight()); // make sure the text
							 // fits

	    // align icon to bottom of text
	    canvas.drawBitmap(icon, iconLeft, iconTop + iconHeight - icon.getHeight(), paint);
	}

	canvas.drawText(header, headerLeft, headerBaseline, paintHead);

	String body = "";
	for (String line : lines) {
	    if (body.length() > 0)
		body += "\n\n";
	    body += line;
	}

	TextPaint textPaint = new TextPaint(paint);
	StaticLayout staticLayout = new StaticLayout(body, textPaint, textWidth, android.text.Layout.Alignment.ALIGN_CENTER, 1.3f, 0, false);

	int layoutHeight = staticLayout.getHeight();
	int textY = textTop + (textHeight / 2) - (layoutHeight / 2);
	if (textY < textTop)
	    textY = textTop;

	canvas.translate(textLeft, textY); // position the text
	staticLayout.draw(canvas);

	return bitmap;
    }

    static Bitmap[] smartNotify(Context context, Bitmap icon, String header, String body) {

	Properties props = BitmapCache.getInstance().getProperties(context, "notification_sticky.xml");

	final int textTop = Integer.parseInt(props.getProperty("textTop", "24"));
	final int textLeft = Integer.parseInt(props.getProperty("textLeft", "3"));
	final int textWidth = Integer.parseInt(props.getProperty("textWidth", "85"));
	final int textHeight = Integer.parseInt(props.getProperty("textHeight", "69"));

	final int iconTop = Integer.parseInt(props.getProperty("iconTop", "0"));
	final int iconLeft = Integer.parseInt(props.getProperty("iconLeft", "0"));

	final int headerLeft = Integer.parseInt(props.getProperty("headerLeft", "20"));
	final int headerBaseline = Integer.parseInt(props.getProperty("headerBaseline", "15"));

	final int headerColor = props.getProperty("headerColor", "white").equalsIgnoreCase("black") ? Color.BLACK : Color.WHITE;
	final int textColor = props.getProperty("textColor", "black").equalsIgnoreCase("black") ? Color.BLACK : Color.WHITE;

	final int arrowUpLeft = Integer.parseInt(props.getProperty("arrowUpLeft", "91"));
	final int arrowUpTop = Integer.parseInt(props.getProperty("arrowUpTop", "23"));

	final int arrowDownLeft = Integer.parseInt(props.getProperty("arrowDownLeft", "91"));
	final int arrowDownTop = Integer.parseInt(props.getProperty("arrowDownTop", "56"));

	final int closeLeft = Integer.parseInt(props.getProperty("closeLeft", "91"));
	final int closeTop = Integer.parseInt(props.getProperty("closeTop", "89"));

	FontInfo font = FontCache.instance(context).Get();

	List<Bitmap> bitmaps = new ArrayList<Bitmap>();

	Paint paintHead = new Paint();
	paintHead.setColor(headerColor);
	paintHead.setTextSize(FontCache.instance(context).Large.size);
	paintHead.setTypeface(FontCache.instance(context).Large.face);

	Paint paint = new Paint();
	paint.setColor(textColor);
	paint.setTextSize(font.size);
	paint.setTypeface(font.face);

	Paint whitePaint = new Paint();
	whitePaint.setColor(Color.WHITE);

	TextPaint textPaint = new TextPaint(paint);
	StaticLayout staticLayout = new StaticLayout(body, textPaint, textWidth, android.text.Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);

	int iconHeight = 16;
	int iconOffset = 0;
	if (icon != null) {
	    iconHeight = Math.max(16, icon.getHeight());
	    iconOffset = iconHeight - icon.getHeight(); // align icon to bottom
							// of text
	}

	int h = staticLayout.getHeight();
	int y = 0;
	int displayHeight = textHeight + 3;

	int scroll = textHeight - font.size;
	boolean more = true;

	int pages = 0;

	while (more) {
	    more = false;
	    Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
	    Canvas canvas = new Canvas(bitmap);

	    canvas.drawColor(Color.WHITE);

	    canvas.drawBitmap(Utils.getBitmap(context, "notify_background_sticky.png"), 0, 0, null);

	    canvas.save();
	    canvas.translate(textLeft, textTop - y); // position the text
	    canvas.clipRect(0, y, textWidth, textHeight + y);
	    staticLayout.draw(canvas);
	    canvas.restore();

	    // Draw header
	    if (icon != null)
		canvas.drawBitmap(icon, iconLeft, iconOffset + iconTop, paint);
	    canvas.drawText(header, headerLeft, headerBaseline, paintHead);

	    if (y > 0)
		canvas.drawBitmap(Utils.getBitmap(context, "arrow_up.bmp"), arrowUpLeft, arrowUpTop, null);

	    if ((h - y) > (displayHeight) && pages < 10) {
		more = true;
		pages++;
		canvas.drawBitmap(Utils.getBitmap(context, "arrow_down.bmp"), arrowDownLeft, arrowDownTop, null);
	    }

	    canvas.drawBitmap(Utils.getBitmap(context, "close.bmp"), closeLeft, closeTop, null);

	    y += scroll;
	    bitmaps.add(bitmap);
	}

	Bitmap[] bitmapArray = new Bitmap[bitmaps.size()];
	bitmaps.toArray(bitmapArray);
	return bitmapArray;
    }

}
