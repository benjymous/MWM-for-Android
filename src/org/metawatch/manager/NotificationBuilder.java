                                                                     
                                                                     
                                                                     
                                             
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
		return new VibratePattern((numberOfBuzzes > 0),500,500,numberOfBuzzes);
	}
	public static VibratePattern createVibratePatternFromPreference(Context context, String preferenceName) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String buzzPref = sharedPreferences.getString(preferenceName, DEFAULT_NUMBER_OF_BUZZES); 
		return createVibratePatternFromBuzzes(Integer.parseInt(buzzPref));
	}

	public static void createSMS(Context context, String number, String text) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		String name = Utils.getContactNameFromNumber(context, number);
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsSMSNumberBuzzes");
		Bitmap icon = Utils.getBitmap(context, "message.bmp");
		String description = "SMS: "+name;
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			if (Preferences.stickyNotifications & !number.equals("Google Chat")) {
				Bitmap[] bitmaps = smartNotify(context, icon, name, text);
				Notification.addBitmapNotification(context, bitmaps, vibratePattern, -1, description);				
			}
			else {
				Bitmap bitmap = smartLines(context, icon, "SMS from", new String[] {name});		
				Notification.addBitmapNotification(context, bitmap, vibratePattern, 4000, description);
				Notification.addTextNotification(context, text, Notification.VibratePattern.NO_VIBRATE, Notification.getDefaultNotificationTimeout(context));				
			}
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, text, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, "SMS from"), Protocol.createOled2lines(context, name, text), scroll, len, vibratePattern, description);
		}
	}

	public static void createMMS(Context context, String number) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		String name = Utils.getContactNameFromNumber(context, number);
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsSMSNumberBuzzes");
		Bitmap icon = Utils.getBitmap(context, "message.bmp");
		String description = "MMS: "+name;
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "MMS from", new String[] {name});		
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), description);
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, name, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, "MMS from"), Protocol.createOled2lines(context, name, ""), scroll, len, vibratePattern, description);
		}
	}
	
	public static void createSmart(Context context, String title, String text) {
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsOtherNotificationNumberBuzzes");
		createSmart(context, title, text, null, true, vibratePattern);
	}
	
	public static void createSmart(Context context, String title, String text, Bitmap icon, boolean sticky, VibratePattern vibratePattern) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		if (icon == null) {
			icon = Utils.getBitmap(context, "notify.bmp");
		}
		String description = "Smart: "+text;
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap[] bitmaps;
			if (sticky) {
				bitmaps = smartNotify(context, icon, title, text);
			} else {
				bitmaps = new Bitmap[] { smartLines(context, icon, title, new String[] { text }) };
			}
			Notification.addBitmapNotification(context, bitmaps, vibratePattern, -1, description);
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, text, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, title), Protocol.createOled2lines(context, title, text), scroll, len, vibratePattern, description);
		}
	}
	
	public static void createK9(Context context, String sender, String subject, String folder) {	
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsK9NumberBuzzes");	
		Bitmap icon = Utils.getBitmap(context, "email.bmp");
		String description = "K9: "+sender;
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "K9 mail", new String[] {sender, subject, folder});
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), description);
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, subject, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, "K9 mail"), Protocol.createOled2lines(context, sender, subject), scroll, len, vibratePattern, description);
		}
	}
	
	public static void createGmail(Context context, String sender, String email, String subject, String snippet) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsGmailNumberBuzzes");	
		Bitmap icon = Utils.getBitmap(context, "gmail.bmp");
		String description = "Gmail: "+sender;
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Gmail", new String[] { sender, email, subject});
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), description);	
			Notification.addTextNotification(context, snippet, Notification.VibratePattern.NO_VIBRATE, Notification.getDefaultNotificationTimeout(context));
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, snippet, scroll);
			Notification.addOledNotification(context, Protocol.createOled2lines(context, "Gmail from " + sender, email), Protocol.createOled2lines(context, subject, snippet), scroll, len, vibratePattern, description);			
		}
	}
	
	public static void createGmailBlank(Context context, String recipient, int count) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsGmailNumberBuzzes");
		Bitmap icon = Utils.getBitmap(context, "gmail.bmp");
		String description = "Gmail: unread "+count;
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Gmail", new String[] {recipient});	
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), description);
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, recipient, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, " Gmail"), Protocol.createOled1line(context, null, recipient), scroll, len, vibratePattern, description);			
		}
	}
	
	public static void createTouchdownMail(Context context, String title, String ticker) {	
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsTDNumberBuzzes");	
		Bitmap icon = Utils.getBitmap(context, "email.bmp");
		String description = "TouchDown: "+title;
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "TouchDown", new String[] {title, ticker});
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), description);
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, ticker, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, "TouchDown"), Protocol.createOled2lines(context, title, ticker), scroll, len, vibratePattern, description);
		}
	}
	
	public static void createCalendar(Context context, String text) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsCalendarNumberBuzzes");
		Bitmap icon = Utils.getBitmap(context, "calendar.bmp");
		String description = "Cal: "+text;
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Calendar", new String[] {text});	
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), description);	
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, text, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, "  Calendar"), Protocol.createOled2lines(context, "Event Reminder:", text), scroll, len, vibratePattern, description);
		}
	}
	
	public static void createAlarm(Context context) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsAlarmNumberBuzzes");	
	    final Calendar t = Calendar.getInstance();
	    final String currentTime = DateFormat.getTimeFormat(context).format(t.getTime());
	    Bitmap icon = Utils.getBitmap(context, "timer.bmp");
	    String description = "Alarm";
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Alarm", new String[] {currentTime}, FontCache.FontSize.LARGE);		
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), description);
		} else {
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, "Alarm"), Protocol.createOled1line(context, null, currentTime), null, 0, vibratePattern, description);
		}
	}
	
	public static void createMusic(Context context, String artist, String track, String album) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsMusicNumberBuzzes");	
		Bitmap icon = Utils.getBitmap(context, "play.bmp");
		String description = "Music: "+track;
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Music", new String[] { track, album, artist});
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), description);
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, track, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, artist), Protocol.createOled2lines(context, album, track), scroll, len, vibratePattern, description);
		}
	}
	
	public static void createTimezonechange(Context context) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsTimezoneNumberBuzzes");	
		TimeZone tz = TimeZone.getDefault();
		Bitmap icon = Utils.getBitmap(context, "timezone.bmp");
		String description = "Timezone Changed";
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Timezone", new String[] {"Timezone Changed", tz.getDisplayName()});		
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), description);
		} else {
			Notification.addOledNotification(context, Protocol.createOled1line(
					context, icon, "Timezone"), Protocol
					.createOled1line(context, null, tz.getDisplayName()), null, 0,
					vibratePattern, description);
		}
	}
	
	public static void createOtherNotification(Context context, Bitmap icon, String appName, String notificationText, int buzzes) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern;
		if (buzzes != -1) {
			vibratePattern = createVibratePatternFromBuzzes(buzzes);
		} else {
			vibratePattern = createVibratePatternFromPreference(context, "settingsOtherNotificationNumberBuzzes");
		}
		if (icon==null) {
			icon = Utils.getBitmap(context, "notify.bmp"); 
		}
		String description = appName;
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, appName, new String[] {notificationText});		
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), description);
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, notificationText, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, appName), Protocol.createOled2lines(context, "Notification", notificationText), scroll, len, vibratePattern, description);
		}
	}
	
	public static void createWinamp(Context context, String artist, String track, String album) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsMusicNumberBuzzes");	
		Bitmap icon = Utils.getBitmap(context, "winamp.bmp");
		String description = "Winamp: "+track;
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Winamp", new String[] { track, album, artist});
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), description);
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, track, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, artist), Protocol.createOled2lines(context, album, track), scroll, len, vibratePattern, description);
		}
	}

	public static void createBatterylow(Context context) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(
				context, "settingsBatteryNumberBuzzes");
		StringBuilder builder = new StringBuilder();
		builder.append(Monitors.BatteryData.level);
		builder.append("%");
		String description = "Battery low";
		Bitmap icon = Utils.getBitmap(context, "batterylow.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon,
					"Battery", new String[] { "Phone battery at", builder.toString() });
			Notification.addBitmapNotification(context, bitmap, vibratePattern,
					Notification.getDefaultNotificationTimeout(context), description);
		} else {
			Notification.addOledNotification(context, Protocol.createOled1line(
					context, icon, "Warning!"), Protocol
					.createOled2lines(context, "Phone battery at", builder.toString()), null, 0,
					vibratePattern, description);
		}
	}	
	
	public static void createNMA(Context context, String appName, String event, String desc, int prio, String url) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsNMANumberBuzzes");		
		Bitmap icon = Utils.getBitmap(context, "notifymyandroid.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, appName, new String[] {event, desc});		
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context), appName+": "+event);
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, desc, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, appName), Protocol.createOled2lines(context, event, desc), scroll, len, vibratePattern, appName+": "+event);
		}
	}

	public static void createNewVoicemail(Context context) {
		if (MetaWatchService.watchType==WatchType.UNKNOWN)
			return;
		VibratePattern vibratePattern = createVibratePatternFromPreference(
				context, "settingsVoicemailNumberBuzzes");
		String description = "New Voicemail";
		Bitmap icon = Utils.getBitmap(context, "voicemail.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon,
					"Voicemail", new String[] { "You have", "new voicemail" });
			Notification.addBitmapNotification(context, bitmap, vibratePattern,
					Notification.getDefaultNotificationTimeout(context), description);
		} else {
			Notification.addOledNotification(context, Protocol.createOled1line(
					context, icon, "Voicemail"), Protocol
					.createOled2lines(context, "You have", "new voicemail"), null, 0,
					vibratePattern, description);
		}
	}	
	
	static Bitmap smartLines(Context context, Bitmap icon, String header, String[] lines) {
		return smartLines(context, icon, header, lines, FontCache.FontSize.AUTO);
	}
	
	static Bitmap smartLines(Context context, Bitmap icon, String header, String[] lines, FontCache.FontSize size) {
		
		Properties props = BitmapCache.getProperties(context, "notification.xml");
		
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
		if (icon!=null) {
			iconHeight = Math.max(16, icon.getHeight()); // make sure the text fits
			
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
		StaticLayout staticLayout = new StaticLayout(body, textPaint, textWidth,
				android.text.Layout.Alignment.ALIGN_CENTER, 1.3f, 0, false);
		
		int layoutHeight = staticLayout.getHeight();
		int textY = textTop + (textHeight/2) - (layoutHeight/2);
		if (textY < textTop)
			textY = textTop;
		
		canvas.translate(textLeft, textY); // position the text
		staticLayout.draw(canvas);

		return bitmap;
	}
		
	static Bitmap[] smartNotify(Context context, Bitmap icon, String header, String body) {	
		
		Properties props = BitmapCache.getProperties(context, "notification_sticky.xml");
		
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
		StaticLayout staticLayout = new StaticLayout(body, textPaint, textWidth,
				android.text.Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
		

		int iconHeight = 16;
		int iconOffset = 0;
		if (icon != null) {
			iconHeight = Math.max(16, icon.getHeight());
			iconOffset = iconHeight - icon.getHeight(); // align icon to bottom of text
		}
			
		int h = staticLayout.getHeight();
		int y = 0;
		int displayHeight = textHeight+3;
		
		int scroll = textHeight-font.size;
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
			canvas.clipRect(0, y, textWidth, textHeight+y);
			staticLayout.draw(canvas);
			canvas.restore();
			
			// Draw header
			if (icon != null)
				canvas.drawBitmap(icon, iconLeft, iconOffset+iconTop, paint);
			canvas.drawText(header, headerLeft, headerBaseline, paintHead);
			
			if (y>0)
				canvas.drawBitmap(Utils.getBitmap(context, "arrow_up.bmp"), arrowUpLeft, arrowUpTop, null);
			
			if((h-y)>(displayHeight) && pages<10) {
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
