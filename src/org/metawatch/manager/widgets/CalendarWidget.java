package org.metawatch.manager.widgets;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.MetaWatch;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Monitors;
import org.metawatch.manager.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Paint.Align;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

public class CalendarWidget implements InternalWidget {

	public final static String id_0 = "Calendar_24_32";
	final static String desc_0 = "Next Calendar Appointment (24x32)";

	public final static String id_1 = "Calendar_96_32";
	final static String desc_1 = "Next Calendar Appointment (96x32)";

	public final static String id_2 = "Calendar_16_16";
	final static String desc_2 = "Next Calendar Appointment (16x16)";

	public final static String id_3 = "Calendar_80_16";
	final static String desc_3 = "Next Calendar Appointment (80x16)";
	
	public final static String id_4 = "Calendar_48_32";
	final static String desc_4 = "Next Calendar Appointment (48x32)";
	
	public final static String id_5 = "Calendar_40_16";
	final static String desc_5 = "Next Calendar Appointment (40x16)";
	
	public final static String id_6 = "Calendar_46_46";
	final static String desc_6 = "Next Calendar Appointment (46x46)";
	
	private Context context;
	private TextPaint paintLarge;
	private TextPaint paintSmall;
	private TextPaint paintSmallNumerals;
	private TextPaint paintNumerals;

	private String meetingTime = "None";
	private String meetingTitle = "";
	private String meetingLocation = "";
	private long meetingStartTimestamp = 0;
	private long meetingEndTimestamp = 0;

	public void init(Context context, ArrayList<CharSequence> widgetIds) {
		this.context = context;

		paintLarge = new TextPaint();
		paintLarge.setColor(Color.BLACK);
		paintLarge.setTextSize(FontCache.instance(context).Large.size);
		paintLarge.setTypeface(FontCache.instance(context).Large.face);
		paintLarge.setTextAlign(Align.CENTER);
		
		paintSmall = new TextPaint();
		paintSmall.setColor(Color.BLACK);
		paintSmall.setTextSize(FontCache.instance(context).Small.size);
		paintSmall.setTypeface(FontCache.instance(context).Small.face);
		paintSmall.setTextAlign(Align.CENTER);
		
		paintSmallNumerals = new TextPaint();
		paintSmallNumerals.setColor(Color.BLACK);
		paintSmallNumerals.setTextSize(FontCache.instance(context).SmallNumerals.size);
		paintSmallNumerals.setTypeface(FontCache.instance(context).SmallNumerals.face);
		paintSmallNumerals.setTextAlign(Align.CENTER);

		paintNumerals = new TextPaint();
		paintNumerals.setColor(Color.BLACK);
		paintNumerals.setTextSize(FontCache.instance(context).Numerals.size);
		paintNumerals.setTypeface(FontCache.instance(context).Numerals.face);
		paintNumerals.setTextAlign(Align.CENTER);
	}

	public void shutdown() {
		paintSmall = null;
	}

	long lastRefresh = 0;

	public void refresh(ArrayList<CharSequence> widgetIds) {

		// Run the refresh in its own thread, so as not to stall the main MWM process
		
		Thread thread = new Thread("CalendarWidget.refresh") {
			@Override
			public void run() {

				boolean readCalendar = false;
				long time = System.currentTimeMillis();
				if ((time - lastRefresh > 5*60*1000) || (Monitors.calendarChanged)) {
					readCalendar = true;
					lastRefresh = System.currentTimeMillis();
				}
				if (!Preferences.readCalendarDuringMeeting) {
					// Only update the current meeting if it is not ongoing
					if ((time>=meetingStartTimestamp) && (time<meetingEndTimestamp-Preferences.readCalendarMinDurationToMeetingEnd*60*1000)) {
						readCalendar = false;
					}
				}
				if (readCalendar) {
					if (Preferences.logging) Log.d(MetaWatch.TAG, "CalendarWidget.refresh() start");
					meetingTime = Utils.readCalendar(context, 0);
					meetingStartTimestamp = Utils.Meeting_StartTimestamp;
					meetingEndTimestamp = Utils.Meeting_EndTimestamp;
					meetingLocation = Utils.Meeting_Location != null ? Utils.Meeting_Location : "";
					meetingTitle = Utils.Meeting_Title != null ? Utils.Meeting_Title : "";
					if (Preferences.logging) Log.d(MetaWatch.TAG, "CalendarWidget.refresh() stop");   
				}
				
			}
		};
		thread.start();
	}

	public void get(ArrayList<CharSequence> widgetIds, Map<String,WidgetData> result) {

		if(widgetIds == null || widgetIds.contains(id_0)) {		
			result.put(id_0, GenWidget(id_0));
		}

		if(widgetIds == null || widgetIds.contains(id_1)) {		
			result.put(id_1, GenWidget(id_1));
		}
		
		if(widgetIds == null || widgetIds.contains(id_2)) {		
			result.put(id_2, GenWidget(id_2));
		}
		
		if(widgetIds == null || widgetIds.contains(id_3)) {		
			result.put(id_3, GenWidget(id_3));
		}
		
		if(widgetIds == null || widgetIds.contains(id_4)) {		
			result.put(id_4, GenWidget(id_4));
		}
		
		if(widgetIds == null || widgetIds.contains(id_5)) {		
			result.put(id_5, GenWidget(id_5));
		}

		if(widgetIds == null || widgetIds.contains(id_6)) {		
			result.put(id_6, GenWidget(id_6));
		}
	
	}

	private InternalWidget.WidgetData GenWidget(String widget_id) {
		InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

		widget.priority = meetingTime.equals("None") ? 0 : 1;	

		String iconFile = "idle_calendar.bmp";
		if (widget_id.equals(id_0)) {
			widget.id = id_0;
			widget.description = desc_0;
			widget.width = 24;
			widget.height = 32;
		}
		else if (widget_id.equals(id_1)) {
			widget.id = id_1;
			widget.description = desc_1;
			widget.width = 96;
			widget.height = 32;
		}
		else if (widget_id.equals(id_2)) {
			widget.id = id_2;
			widget.description = desc_2;
			widget.width = 16;
			widget.height = 16;
			iconFile = "idle_calendar_10.bmp";
		}
		else if (widget_id.equals(id_3)) {
			widget.id = id_3;
			widget.description = desc_3;
			widget.width = 80;
			widget.height = 16;
			iconFile = "idle_calendar_10.bmp";
		}
		else if (widget_id.equals(id_4)) {
			widget.id = id_4;
			widget.description = desc_4;
			widget.width = 48;
			widget.height = 32;
			iconFile = null;
		}
		else if (widget_id.equals(id_5)) {
			widget.id = id_5;
			widget.description = desc_5;
			widget.width = 40;
			widget.height = 16;
			iconFile = null;
		}
		else if (widget_id.equals(id_6)) {
			widget.id = id_6;
			widget.description = desc_6;
			widget.width = 46;
			widget.height = 46;
		}

		Bitmap icon = iconFile == null ? null : Utils.getBitmap(context, iconFile);

		widget.bitmap = Bitmap.createBitmap(widget.width, widget.height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(widget.bitmap);
		canvas.drawColor(Color.WHITE);

		Point iconOffset = Utils.getIconOffset(widget.height);
		Point textOffset = Utils.getTextOffset(widget.height);
		
		if (widget.height == 16 && icon != null) {
			canvas.drawBitmap(icon, widget.width == 16 ? 2 : 0, iconOffset.y, null);

			if(meetingTime.equals("None"))
				canvas.drawText("-", widget.width == 16 ? 8 : 6, textOffset.y, paintSmallNumerals);
			else {
				// Strip out colon to make it fit;
				String time = meetingTime.replace(":", "");

				if (widget.width==16) {
					canvas.drawText(time, 8, textOffset.y, paintSmallNumerals);
				}
				else {
					paintSmallNumerals.setTextAlign(Align.LEFT);
					canvas.drawText(time, 0, textOffset.y, paintSmallNumerals);
					paintSmallNumerals.setTextAlign(Align.CENTER);
				}
			}
		}
		else if (widget.height == 46 && icon != null){
			canvas.drawBitmap(icon, 11, iconOffset.y, null);
		
			if ((Preferences.displayLocationInSmallCalendarWidget)&&
					(!meetingTime.equals("None"))&&(meetingLocation!=null)&&
					(!meetingLocation.equals("---"))&&(widget_id.equals(id_0))&&
					(meetingLocation.length()>0)&&(meetingLocation.length()<=3)) {
				canvas.drawText(meetingLocation, 23, (iconOffset.y+13), paintSmall);        
			}
			else 
			{
				Calendar c = Calendar.getInstance(); 
				if ((Preferences.eventDateInCalendarWidget)&&
						(!meetingTime.equals("None"))) {
					c.setTimeInMillis(meetingStartTimestamp);
				}
				int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
				if(dayOfMonth<10) {
					canvas.drawText(""+dayOfMonth, 23, (iconOffset.y+13), paintNumerals);
				}
				else
				{
					canvas.drawText(""+dayOfMonth/10, 20, (iconOffset.y+13), paintNumerals);
					canvas.drawText(""+dayOfMonth%10, 26, (iconOffset.y+13), paintNumerals);
				}
			}
			canvas.drawText(meetingTime, 24, textOffset.y, paintLarge);
		}
		else if (icon!=null){
			canvas.drawBitmap(icon, 0, iconOffset.y, null);

			if ((Preferences.displayLocationInSmallCalendarWidget)&&
					(!meetingTime.equals("None"))&&(meetingLocation!=null)&&
					(!meetingLocation.equals("---"))&&(widget_id.equals(id_0))&&
					(meetingLocation.length()>0)&&(meetingLocation.length()<=3)) {
				canvas.drawText(meetingLocation, 12, (iconOffset.y+13), paintSmall);        
			}
			else 
			{
				Calendar c = Calendar.getInstance(); 
				if ((Preferences.eventDateInCalendarWidget)&&
						(!meetingTime.equals("None"))) {
					c.setTimeInMillis(meetingStartTimestamp);
				}
				int dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
				if(dayOfMonth<10) {
					canvas.drawText(""+dayOfMonth, 12, (iconOffset.y+13), paintNumerals);
				}
				else
				{
					canvas.drawText(""+dayOfMonth/10, 9, (iconOffset.y+13), paintNumerals);
					canvas.drawText(""+dayOfMonth%10, 15, (iconOffset.y+13), paintNumerals);
				}
			}
			canvas.drawText(meetingTime, 12, textOffset.y, paintSmall);
		}

		String text = "";
		if (iconFile==null)
			text = meetingTime;
		if ((meetingTitle!=null)) {
			if (text.length()>0)
				text += " : ";
			text += meetingTitle;
		}
		if ((meetingLocation !=null) && (meetingLocation.length()>0))
			text += " - " + meetingLocation;
		
		if (widget_id.equals(id_1) || widget_id.equals(id_4) ) {
			
			paintSmall.setTextAlign(Align.LEFT);
			
			int iconSpace = iconFile == null ? 0 : 25;
			
			canvas.save();			
			StaticLayout layout = new StaticLayout(text, paintSmall, widget.width-iconSpace, Layout.Alignment.ALIGN_CENTER, 1.2f, 0, false);
			int height = layout.getHeight();
			int textY = 16 - (height/2);
			if(textY<0) {
				textY=0;
			}
			canvas.translate(iconSpace, textY); //position the text
			layout.draw(canvas);
			canvas.restore();	

			paintSmall.setTextAlign(Align.CENTER);
		}
		else if (widget_id.equals(id_3) || widget_id.equals(id_5) ) {
			
			paintSmall.setTextAlign(Align.LEFT);

			int iconSpace = iconFile == null ? 0 : 11;

			canvas.save();			
			StaticLayout layout = new StaticLayout(text, paintSmall, widget.width-iconSpace, Layout.Alignment.ALIGN_CENTER, 1.0f, 0, false);
			int height = layout.getHeight();
			int textY = 8 - (height/2);
			if(textY<0) {
				textY=0;
			}
			canvas.translate(iconSpace, textY); //position the text
			layout.draw(canvas);
			canvas.restore();	

			paintSmall.setTextAlign(Align.CENTER);
		}


		return widget;
	}



}
