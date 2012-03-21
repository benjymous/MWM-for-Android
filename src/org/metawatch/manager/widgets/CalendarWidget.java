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

	public final static String id_2 = "Calendar_19_16";
	final static String desc_2 = "Next Calendar Appointment (19x16)";

	private Context context;
	private TextPaint paintSmall;
	private TextPaint paintSmallNumerals;
	private TextPaint paintNumerals;

	private String meetingTime = "None";
	private String meetingTitle;
	private String meetingLocation;
	private long meetingStartTimestamp = 0;
	private long meetingEndTimestamp = 0;

	public void init(Context context, ArrayList<CharSequence> widgetIds) {
		this.context = context;

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
			meetingLocation = Utils.Meeting_Location;
			meetingTitle = Utils.Meeting_Title;
			if (Preferences.logging) Log.d(MetaWatch.TAG, "CalendarWidget.refresh() stop");   
		}
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
			widget.width = 19;
			widget.height = 16;
			iconFile = "idle_calendar_10.bmp";
		}

		Bitmap icon = Utils.loadBitmapFromAssets(context, iconFile);

		widget.bitmap = Bitmap.createBitmap(widget.width, widget.height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(widget.bitmap);
		canvas.drawColor(Color.WHITE);

		if (widget.height == 16) {
			canvas.drawBitmap(icon, 4, 0, null);
			if(meetingTime.equals("None"))
				canvas.drawText("-", 10, 15, paintSmallNumerals);
			else
				canvas.drawText(meetingTime, 10, 15, paintSmallNumerals);
		}
		else {
			canvas.drawBitmap(icon, 0, 3, null);

			if ((Preferences.displayLocationInSmallCalendarWidget)&&
					(!meetingTime.equals("None"))&&(meetingLocation!=null)&&
					(!meetingLocation.equals("---"))&&(widget_id.equals(id_0))&&
					(meetingLocation.length()>0)&&(meetingLocation.length()<=3)) {
				canvas.drawText(meetingLocation, 12, 15, paintSmall);        
			}
			else 
			{
				Calendar c = Calendar.getInstance(); 
				int dayOfMonth = c.get(Calendar.DAY_OF_MONTH); 
				if(dayOfMonth<10) {
					canvas.drawText(""+dayOfMonth, 12, 16, paintNumerals);
				}
				else
				{
					canvas.drawText(""+dayOfMonth/10, 9, 16, paintNumerals);
					canvas.drawText(""+dayOfMonth%10, 15, 16, paintNumerals);
				}
			}
			canvas.drawText(meetingTime, 12, 30, paintSmall);
		}
		
		

		if (widget_id.equals(id_1)) {
			paintSmall.setTextAlign(Align.LEFT);

			String text = meetingTitle;
			if ((meetingLocation !=null) && (meetingLocation.length()>0))
				text += " - " + meetingLocation;

			canvas.save();			
			StaticLayout layout = new StaticLayout(text, paintSmall, 70, Layout.Alignment.ALIGN_CENTER, 1.2f, 0, false);
			int height = layout.getHeight();
			int textY = 16 - (height/2);
			if(textY<0) {
				textY=0;
			}
			canvas.translate(25, textY); //position the text
			layout.draw(canvas);
			canvas.restore();	

			paintSmall.setTextAlign(Align.CENTER);
		}

		return widget;
	}



}
