                                                                     
                                                                     
                                                                     
                                             
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
	
	public static VibratePattern createVibratePatternFromPreference(Context context, String preferenceName) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String buzzPref = sharedPreferences.getString(preferenceName, DEFAULT_NUMBER_OF_BUZZES); 
		int numberOfBuzzes = Integer.parseInt(buzzPref);
		return new VibratePattern((numberOfBuzzes > 0),500,500,numberOfBuzzes);
	}

	public static void createSMS(Context context, String number, String text) {
		String name = Utils.getContactNameFromNumber(context, number);
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsSMSNumberBuzzes");
		Bitmap icon = Utils.loadBitmapFromAssets(context, "message.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			if (Preferences.stickyNotifications & !number.equals("Google Chat")) {
				Bitmap[] bitmaps = smartNotify(context, icon, name, text);
				Notification.addBitmapNotification(context, bitmaps, vibratePattern, -1);				
			}
			else {
				Bitmap bitmap = smartLines(context, icon, "SMS from", new String[] {name});		
				Notification.addBitmapNotification(context, bitmap, vibratePattern, 4000);
				Notification.addTextNotification(context, text, Notification.VibratePattern.NO_VIBRATE, Notification.getDefaultNotificationTimeout(context));				
			}
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, text, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, "SMS from"), Protocol.createOled2lines(context, name, text), scroll, len, vibratePattern);
		}
	}

	public static void createMMS(Context context, String number) {
		String name = Utils.getContactNameFromNumber(context, number);
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsSMSNumberBuzzes");
		Bitmap icon = Utils.loadBitmapFromAssets(context, "message.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "MMS from", new String[] {name});		
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context));
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, name, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, "MMS from"), Protocol.createOled2lines(context, name, ""), scroll, len, vibratePattern);
		}
	}
	
	public static void createSmart(Context context, String title, String text) {
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsOtherNotificationNumberBuzzes");
		createSmart(context, title, text, vibratePattern);
	}
	
	public static void createSmart(Context context, String title, String text, VibratePattern vibratePattern) {
		Bitmap icon = Utils.loadBitmapFromAssets(context, "notify.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {		
			Bitmap[] bitmaps = smartNotify(context, icon, title, text);
			Notification.addBitmapNotification(context, bitmaps, vibratePattern, -1);
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, text, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, title), Protocol.createOled2lines(context, title, text), scroll, len, vibratePattern);
		}
	}
	
	public static void createK9(Context context, String sender, String subject, String folder) {	
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsK9NumberBuzzes");	
		Bitmap icon = Utils.loadBitmapFromAssets(context, "email.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "K9 mail", new String[] {sender, subject, folder});
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context));
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, subject, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, "K9 mail"), Protocol.createOled2lines(context, sender, subject), scroll, len, vibratePattern);
		}
	}
	
	public static void createGmail(Context context, String sender, String email, String subject, String snippet) {
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsGmailNumberBuzzes");	
		Bitmap icon = Utils.loadBitmapFromAssets(context, "gmail.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Gmail", new String[] { sender, email, subject});
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context));	
			Notification.addTextNotification(context, snippet, Notification.VibratePattern.NO_VIBRATE, Notification.getDefaultNotificationTimeout(context));
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, snippet, scroll);
			Notification.addOledNotification(context, Protocol.createOled2lines(context, "Gmail from " + sender, email), Protocol.createOled2lines(context, subject, snippet), scroll, len, vibratePattern);			
		}
	}
	
	public static void createGmailBlank(Context context, String recipient, int count) {
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsGmailNumberBuzzes");
		String messages = count + " new " + (count == 1 ? "message" : "messages");
		Bitmap icon = Utils.loadBitmapFromAssets(context, "gmail.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Gmail", new String[] {messages, recipient});	
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context));
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, recipient, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, " Gmail"), Protocol.createOled2lines(context, messages, recipient), scroll, len, vibratePattern);			
		}
	}
	
	public static void createCalendar(Context context, String text) {
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsCalendarNumberBuzzes");
		Bitmap icon = Utils.loadBitmapFromAssets(context, "calendar.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Calendar", new String[] {text});	
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context));	
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, text, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, "  Calendar"), Protocol.createOled2lines(context, "Event Reminder:", text), scroll, len, vibratePattern);
		}
	}
	
	public static void createAlarm(Context context) {
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsAlarmNumberBuzzes");	
	    final Calendar t = Calendar.getInstance();
	    final String currentTime = DateFormat.getTimeFormat(context).format(t.getTime());
	    Bitmap icon = Utils.loadBitmapFromAssets(context, "timer.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Alarm", new String[] {currentTime}, FontCache.FontSize.LARGE);		
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context));
		} else {
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, "Alarm"), Protocol.createOled1line(context, null, currentTime), null, 0, vibratePattern);
		}
	}
	
	public static void createMusic(Context context, String artist, String track, String album) {
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsMusicNumberBuzzes");	
		Bitmap icon = Utils.loadBitmapFromAssets(context, "play.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Music", new String[] { track, album, artist});
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context));
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, track, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, artist), Protocol.createOled2lines(context, album, track), scroll, len, vibratePattern);
		}
	}
	
	public static void createTimezonechange(Context context) {
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsTimezoneNumberBuzzes");	
		TimeZone tz = TimeZone.getDefault();
		Bitmap icon = Utils.loadBitmapFromAssets(context, "timezone.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Timezone", new String[] {"Timezone Changed", tz.getDisplayName()});		
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context));
		} else {
			Notification.addOledNotification(context, Protocol.createOled1line(
					context, icon, "Timezone"), Protocol
					.createOled1line(context, null, tz.getDisplayName()), null, 0,
					vibratePattern);
		}
	}
	
	public static void createOtherNotification(Context context, Bitmap icon, String appName, String notificationText) {
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsOtherNotificationNumberBuzzes");	
		if (icon==null) {
			icon = Utils.loadBitmapFromAssets(context, "notify.bmp"); 
		}
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, appName, new String[] {notificationText});		
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context));
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, notificationText, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, appName), Protocol.createOled2lines(context, "Notification", notificationText), scroll, len, vibratePattern);
		}
	}
	
	public static void createWinamp(Context context, String artist, String track, String album) {
		VibratePattern vibratePattern = createVibratePatternFromPreference(context, "settingsMusicNumberBuzzes");	
		Bitmap icon = Utils.loadBitmapFromAssets(context, "winamp.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon, "Winamp", new String[] { track, album, artist});
			Notification.addBitmapNotification(context, bitmap, vibratePattern, Notification.getDefaultNotificationTimeout(context));
		} else {
			byte[] scroll = new byte[800];
			int len = Protocol.createOled2linesLong(context, track, scroll);
			Notification.addOledNotification(context, Protocol.createOled1line(context, icon, artist), Protocol.createOled2lines(context, album, track), scroll, len, vibratePattern);
		}
	}

	public static void createBatterylow(Context context) {
		VibratePattern vibratePattern = createVibratePatternFromPreference(
				context, "settingsBatteryNumberBuzzes");
		StringBuilder builder = new StringBuilder();
		builder.append(Monitors.BatteryData.level);
		builder.append("%");
		Bitmap icon = Utils.loadBitmapFromAssets(context, "batterylow.bmp");
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
			Bitmap bitmap = smartLines(context, icon,
					"Battery", new String[] { "Phone battery at", builder.toString() });
			Notification.addBitmapNotification(context, bitmap, vibratePattern,
					Notification.getDefaultNotificationTimeout(context));
		} else {
			Notification.addOledNotification(context, Protocol.createOled1line(
					context, icon, "Warning!"), Protocol
					.createOled2lines(context, "Phone battery at", builder.toString()), null, 0,
					vibratePattern);
		}
	}	
	
	static Bitmap smartLines(Context context, Bitmap icon, String header, String[] lines) {
		return smartLines(context, icon, header, lines, FontCache.FontSize.AUTO);
	}
	
	static Bitmap smartLines(Context context, Bitmap icon, String header, String[] lines, FontCache.FontSize size) {
		FontInfo font = FontCache.instance(context).Get(size);	
		
		Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);		
		
		Paint paintHead = new Paint();
		paintHead.setColor(Color.BLACK);		
		paintHead.setTextSize(FontCache.instance(context).Large.size);
		paintHead.setTypeface(FontCache.instance(context).Large.face);
		
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);		
		paint.setTextSize(font.size);	
		paint.setTypeface(font.face);
				
		canvas.drawColor(Color.WHITE);
		
		int iconWidth = 0;
		int iconHeight = 16;
		if (icon!=null) {
			iconWidth = icon.getWidth();
			iconHeight = Math.max(16, icon.getHeight()); // make sure the text fits
			
			// align icon to bottom of text
			canvas.drawBitmap(icon, 0, iconHeight - icon.getHeight(), paint);
		}			
		
		canvas.drawText(header, iconWidth+1, iconHeight-2, paintHead);
		
		canvas.drawLine(1, iconHeight, 95, iconHeight, paint);
		
		String body = "";		
		for (String line : lines) {
			if (body.length() > 0)
				body += "\n\n";
			body += line;
		}
		
		TextPaint textPaint = new TextPaint(paint);
		StaticLayout staticLayout = new StaticLayout(body, textPaint, 94,
				android.text.Layout.Alignment.ALIGN_CENTER, 1.3f, 0, false);
		
		int textHeight = staticLayout.getHeight();
		int headerHeight = iconHeight+2;
		int textY = (56) - (textHeight/2);
		if (textY < headerHeight)
			textY = headerHeight;
		
		canvas.translate(1, textY); // position the text
		staticLayout.draw(canvas);

		return bitmap;
	}
		
	static Bitmap[] smartNotify(Context context, Bitmap icon, String header, String body) {	
		FontInfo font = FontCache.instance(context).Get();		
		
		List<Bitmap> bitmaps = new ArrayList<Bitmap>();	
		
		Paint paintHead = new Paint();
		paintHead.setColor(Color.BLACK);		
		paintHead.setTextSize(FontCache.instance(context).Large.size);
		paintHead.setTypeface(FontCache.instance(context).Large.face);
		
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);		
		paint.setTextSize(font.size);	
		paint.setTypeface(font.face);
		
		Paint whitePaint = new Paint();
		whitePaint.setColor(Color.WHITE);	
		
		TextPaint textPaint = new TextPaint(paint);
		StaticLayout staticLayout = new StaticLayout(body, textPaint, 86,
				android.text.Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
		

		int iconWidth = 0;
		int iconHeight = 16;
		int iconOffset = 0;
		if (icon != null) {
			iconWidth = icon.getWidth();
			iconHeight = Math.max(16, icon.getHeight());
			iconOffset = iconHeight - icon.getHeight(); // align icon to bottom of text
		}

		int h = staticLayout.getHeight();
		int y = 0;
		int displayHeight = 96 - iconHeight+2;
		
		int scroll = 72;
		boolean more = true;
		
		while (more) {	
			more = false;
			Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);	
			
			canvas.drawColor(Color.WHITE);
			
			canvas.save();
			canvas.translate(1, iconHeight+2 - y); // position the text
			staticLayout.draw(canvas);
			canvas.restore();
			
			// Draw header
			canvas.drawRect(new android.graphics.Rect(0,0,96,iconHeight), whitePaint);
			if (icon != null)
				canvas.drawBitmap(icon, 0, iconOffset, paint);
			canvas.drawText(header, iconWidth+1, iconHeight-2, paintHead);
			
			//canvas.drawText(""+(h-y)+" "+displayHeight, icon.getWidth()+1, icon.getHeight()-2, paintHead);
			
			canvas.drawLine(1, iconHeight, 88, iconHeight, paint);
			canvas.drawLine(88, iconHeight, 88, 95, paint);
			
			if (y>0)
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "arrow_up.bmp"), 90, 17, null);
			
			if((h-y)>(displayHeight)) {
				more = true;
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "arrow_down.bmp"), 90, 56, null);
			}
						
			canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "close.bmp"), 90, 89, null);
			
			y += scroll;
			bitmaps.add(bitmap);
		} 
		
		Bitmap[] bitmapArray = new Bitmap[bitmaps.size()];
		bitmaps.toArray(bitmapArray);
		return bitmapArray;
	}
		
}
