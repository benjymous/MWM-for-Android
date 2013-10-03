package org.metawatch.manager.apps;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.FontCache.FontSize;
import org.metawatch.manager.Idle;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchType;
import org.metawatch.manager.MetaWatchStatus;
import org.metawatch.manager.Monitors;
import org.metawatch.manager.Protocol;
import org.metawatch.manager.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import org.metawatch.manager.Log;

public class CalendarApp extends ApplicationBase {

    public final static String APP_ID = "org.metawatch.manager.apps.CalendarApp";

    static AppData appData = new AppData() {
	{
	    id = APP_ID;
	    name = "Calendar";

	    supportsDigital = true;
	    supportsAnalog = false;
	}
    };

    public final static byte CALENDAR_FLIPVIEW = 40;
    public final static byte CALENDAR_TODAY = 41;
    public final static byte CALENDAR_PREV = 43;
    public final static byte CALENDAR_NEXT = 44;

    @Override
    public AppData getInfo() {
	return appData;
    }

    @Override
    public void activate(Context context, int watchType) {
	refresh(context);

	if (watchType == WatchType.DIGITAL) {
	    Protocol.getInstance(context).enableButton(1, 1, CALENDAR_FLIPVIEW, MetaWatchService.WatchBuffers.APPLICATION); // right
	    // middle
	    // -
	    // press
	    Protocol.getInstance(context).enableButton(2, 1, CALENDAR_TODAY, MetaWatchService.WatchBuffers.APPLICATION); // right
	    // bottom
	    // -
	    // press

	    final int leftLower = getLeftLowerButtonCode();
	    final int leftUpper = getLeftUpperButtonCode();

	    Protocol.getInstance(context).enableButton(leftLower, 1, CALENDAR_NEXT, MetaWatchService.WatchBuffers.APPLICATION); // left
	    // bottom
	    // -
	    // press
	    Protocol.getInstance(context).enableButton(leftUpper, 1, CALENDAR_PREV, MetaWatchService.WatchBuffers.APPLICATION); // left
	    // middle
	    // -
	    // press
	}
    }

    @Override
    public void deactivate(Context context, int watchType) {
	if (watchType == WatchType.DIGITAL) {
	    Protocol.getInstance(context).disableButton(1, 1, MetaWatchService.WatchBuffers.APPLICATION);
	    Protocol.getInstance(context).disableButton(2, 1, MetaWatchService.WatchBuffers.APPLICATION);

	    final int leftLower = getLeftLowerButtonCode();
	    final int leftUpper = getLeftUpperButtonCode();

	    Protocol.getInstance(context).disableButton(leftLower, 1, MetaWatchService.WatchBuffers.APPLICATION);
	    Protocol.getInstance(context).disableButton(leftUpper, 1, MetaWatchService.WatchBuffers.APPLICATION);
	}
    }

    public int getColumn(int dow) {
	switch (dow) {
	case Calendar.MONDAY:
	    return 5;

	case Calendar.TUESDAY:
	    return 18;

	case Calendar.WEDNESDAY:
	    return 31;

	case Calendar.THURSDAY:
	    return 44;

	case Calendar.FRIDAY:
	    return 57;

	case Calendar.SATURDAY:
	    return 70;

	case Calendar.SUNDAY:
	    return 83;
	}

	return 0;
    }

    long lastRefresh = 0;
    private Calendar displayDate = Calendar.getInstance();
    private List<Utils.CalendarEntry> calendarEntries = null;

    final static class ViewMode {
	static final int MonthOverview = 0;
	static final int Agenda = 1;

	static final int max = 2;
    }

    private int currentView = ViewMode.MonthOverview;

    private void refresh(final Context context) {
	Thread thread = new Thread("CalendarApp.refresh") {
	    @Override
	    public void run() {

		boolean readCalendar = false;
		long time = System.currentTimeMillis();
		if ((time - lastRefresh > 5 * DateUtils.MINUTE_IN_MILLIS) || (Monitors.getInstance().calendarChangedTimestamp > lastRefresh)) {
		    readCalendar = true;
		    lastRefresh = System.currentTimeMillis();
		}

		if (readCalendar) {
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "CalendarApp.refresh() start");

		    Calendar cal = (Calendar) displayDate.clone();
		    cal.set(Calendar.DAY_OF_MONTH, 1);

		    long startTime = cal.getTimeInMillis();
		    long endTime = startTime + DateUtils.DAY_IN_MILLIS * (31 + 10);

		    calendarEntries = Utils.readCalendar(context, startTime, endTime, false);

		    Idle.getInstance().updateIdle(context, false);

		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "CalendarApp.refresh() stop - " + (calendarEntries == null ? "0" : calendarEntries.size()) + " entries found");
		}

	    }
	};
	thread.start();
    }

    @Override
    public Bitmap update(Context context, boolean preview, int watchType) {

	Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	TextPaint paintSmall = new TextPaint();
	paintSmall.setColor(Color.WHITE);
	paintSmall.setTextSize(FontCache.instance(context).Small.size);
	paintSmall.setTypeface(FontCache.instance(context).Small.face);
	paintSmall.setTextAlign(Align.LEFT);

	TextPaint paintSmallNumerals = new TextPaint();
	paintSmallNumerals.setColor(Color.BLACK);
	paintSmallNumerals.setTextSize(FontCache.instance(context).SmallNumerals.size);
	paintSmallNumerals.setTypeface(FontCache.instance(context).SmallNumerals.face);
	paintSmallNumerals.setTextAlign(Align.LEFT);

	Paint paintBlack = new Paint();
	paintBlack.setColor(Color.BLACK);

	Calendar calToday = Calendar.getInstance();
	Calendar calTomorrow = Calendar.getInstance();
	calTomorrow.add(Calendar.DAY_OF_MONTH, 1);

	Calendar cal = (Calendar) displayDate.clone();

	if (currentView == ViewMode.MonthOverview) {
	    canvas.drawBitmap(Utils.getBitmap(context, "calendar_app_month.png"), 0, 0, null);

	    SimpleDateFormat df = new SimpleDateFormat("MMMM yyyy");
	    canvas.drawText(df.format(cal.getTime()), 4, 8, paintSmall);

	    final int month = cal.get(Calendar.MONTH);

	    int yPos = 18;

	    Map<Integer, Point> dateCells = new HashMap<Integer, Point>();

	    final int days = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
	    for (int day = 1; day <= days; ++day) {
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), day);

		final int dow = cal.get(Calendar.DAY_OF_WEEK);
		final int xPos = getColumn(dow);

		dateCells.put(cal.get(Calendar.DAY_OF_MONTH), new Point(xPos, yPos));

		if (Utils.isSameDate(cal, calToday)) {
		    canvas.drawBitmap(Utils.getBitmap(context, "calendar_app_today.png"), xPos - 4, yPos - 4, null);
		}

		canvas.drawText("" + day, xPos, yPos + 5, paintSmallNumerals);

		if (dow == Calendar.SUNDAY)
		    yPos += 13;
	    }

	    if (calendarEntries != null) {

		for (Utils.CalendarEntry entry : calendarEntries) {
		    cal.setTimeInMillis(entry.startTimestamp);

		    if (cal.get(Calendar.MONTH) == month) {
			final int dom = cal.get(Calendar.DAY_OF_MONTH);
			final Point cellLoc = dateCells.get(dom);

			if (entry.isAllDay) {
			    canvas.drawLine(cellLoc.x - 1, cellLoc.y + 6, cellLoc.x + 9, cellLoc.y + 6, paintBlack);
			} else {
			    // Each horizontal pixel represents 2 hours, giving
			    // a span of 7am to 11pm
			    final int hour = cal.get(Calendar.HOUR_OF_DAY);
			    int xStart = java.lang.Math.max(0, (hour - 7) / 2);
			    int xEnd = java.lang.Math.min(8, xStart + (int) ((entry.endTimestamp - entry.startTimestamp) / DateUtils.HOUR_IN_MILLIS * 2));

			    if (xEnd > xStart)
				canvas.drawRect(new Rect(cellLoc.x + xStart, cellLoc.y + 6, cellLoc.x + xEnd, cellLoc.y + 9), paintBlack);
			}
		    }
		}
	    }
	} else if (currentView == ViewMode.Agenda) {
	    canvas.drawBitmap(Utils.getBitmap(context, "calendar_app_agenda.png"), 0, 0, null);

	    canvas.drawText(DateFormat.getDateFormat(context).format(cal.getTimeInMillis()), 4, 8, paintSmall);

	    int yPos = 11;

	    Calendar lastDate = Calendar.getInstance();
	    lastDate.setTimeInMillis(0);

	    if (calendarEntries != null) {

		for (Utils.CalendarEntry entry : calendarEntries) {

		    if (entry.isOngoing(displayDate.getTimeInMillis()) || entry.isFuture(displayDate.getTimeInMillis())) {
			Calendar date = Calendar.getInstance();
			date.setTimeInMillis(entry.startTimestamp);

			if (!Utils.isSameDate(date, lastDate)) {
			    String dateHeader = "";
			    if (Utils.isSameDate(date, calToday)) {
				dateHeader = "TODAY";
			    } else if (Utils.isSameDate(date, calTomorrow)) {
				dateHeader = "TOMORROW";
			    } else {
				dateHeader = DateFormat.getDateFormat(context).format(entry.startTimestamp);
			    }

			    StaticLayout layout = Utils.buildText(context, dateHeader, 88, Layout.Alignment.ALIGN_NORMAL, Color.BLACK, FontSize.SMALL);

			    canvas.save();
			    canvas.clipRect(new Rect(4, 11, 92, 92));
			    canvas.translate(4, yPos);
			    layout.draw(canvas);
			    canvas.restore();
			    yPos += layout.getHeight() + 1;

			}

			lastDate = date;

			StringBuilder builder = new StringBuilder();

			if (!entry.isAllDay) {
			    builder.append(DateFormat.getTimeFormat(context).format(entry.startTimestamp));
			    builder.append(": ");
			}

			builder.append(entry.title);

			if (entry.isAllDay) {
			    builder.append(" (All day)");
			}

			StaticLayout layout = Utils.buildText(context, builder.toString(), 84, Layout.Alignment.ALIGN_NORMAL, Color.BLACK, FontSize.SMALL);

			canvas.save();
			canvas.clipRect(new Rect(8, 11, 92, 92));
			canvas.translate(8, yPos);
			layout.draw(canvas);
			canvas.restore();
			yPos += layout.getHeight() + 1;

			if (yPos > 92)
			    break;
		    }

		}
	    }
	}

	if (calendarEntries == null) {
	    canvas.drawBitmap(Utils.getBitmap(context, "busy.png"), 0, 80, null);
	}

	drawDigitalAppSwitchIcon(context, canvas, preview);

	return bitmap;
    }

    private void setDate(Context context, Calendar cal) {
	if (Utils.isDifferentMonth(cal, displayDate)) {
	    lastRefresh = 0;
	    calendarEntries = null;
	}
	displayDate = cal;
	if (calendarEntries == null)
	    refresh(context);
    }

    private void advance(Context context, int amount) {
	Calendar newDate = (Calendar) displayDate.clone();
	newDate.add(currentView == ViewMode.MonthOverview ? Calendar.MONTH : Calendar.DAY_OF_MONTH, amount);
	setDate(context, newDate);
    }

    @Override
    public int buttonPressed(Context context, int id) {

	switch (id) {
	case CALENDAR_FLIPVIEW:
	    currentView = (currentView + 1) % ViewMode.max;
	    return BUTTON_USED;

	case CALENDAR_TODAY:
	    lastRefresh = 0;
	    calendarEntries = null;
	    setDate(context, Calendar.getInstance());
	    return BUTTON_USED;

	case CALENDAR_PREV:
	    advance(context, -1);
	    return BUTTON_USED;

	case CALENDAR_NEXT:
	    advance(context, 1);
	    return BUTTON_USED;
	}
	return BUTTON_NOT_USED;
    }

}
