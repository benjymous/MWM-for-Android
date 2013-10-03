package org.metawatch.manager.widgets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metawatch.manager.Idle;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchType;
import org.metawatch.manager.MetaWatchStatus;
import org.metawatch.manager.widgets.InternalWidget.WidgetData;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import org.metawatch.manager.Log;
import android.widget.Toast;

public class WidgetManager {
    List<InternalWidget> widgets = new ArrayList<InternalWidget>();
    Map<String, WidgetData> dataCache;
    Object lock = new Object();

    long lastWidgetBroadcast = 0;

    public String defaultWidgetsDigital = "weather_96_32|missedCalls_24_32,unreadSms_24_32,unreadGmail_24_32";
    public String defaultWidgetsAnalog = "weather_80_16|missedCalls_16_16,unreadSms_16_16,unreadGmail_16_16";

    private final int TIME_ONE_MINUTE = 60 * 1000;
    
    private static WidgetManager mInstance;
    
    private WidgetManager(Context context) {
	initWidgets(context, null);
    }
    
    public static WidgetManager getInstance(Context context) {
	if (mInstance == null)
	    mInstance = new WidgetManager(context);
	return mInstance;
    }
    
    public void destroy() {
	mInstance = null;
    }

    public void initWidgets(Context context, ArrayList<CharSequence> widgetsDesired) {

	if (widgets.size() == 0) {
	    widgets.add(new MissedCallsWidget());
	    widgets.add(new SmsWidget());
	    widgets.add(new K9Widget());
	    widgets.add(new GmailWidget());
	    widgets.add(new WeatherWidget());
	    widgets.add(new CalendarWidget());
	    widgets.add(new PhoneStatusWidget());
	    widgets.add(new PictureWidget());
	    widgets.add(new TouchDownWidget());
	    widgets.add(new VoicemailWidget());
	    widgets.add(new NowPlayingWidget());
	    if (Preferences.showTestWidgets) {
		widgets.add(new TestWidget());
	    }
	}

	for (InternalWidget widget : widgets) {
	    widget.init(context, widgetsDesired);
	}

	refreshWidgets(context, null);
    }

    public Map<String, WidgetData> refreshWidgets(Context context, ArrayList<CharSequence> widgetsDesired) {
	synchronized (lock) {

	    if (dataCache == null)
		dataCache = new HashMap<String, WidgetData>();

	    for (InternalWidget widget : widgets) {
		widget.refresh(widgetsDesired);
		widget.get(widgetsDesired, dataCache);
	    }

	    if (System.currentTimeMillis() - lastWidgetBroadcast > TIME_ONE_MINUTE) {
		Intent intent = new Intent("org.metawatch.manager.REFRESH_WIDGET_REQUEST");
		Bundle b = new Bundle();
		if (widgetsDesired == null)
		    b.putBoolean("org.metawatch.manager.get_previews", true);
		else {
		    String[] temp = widgetsDesired.toArray(new String[widgetsDesired.size()]);
		    b.putStringArray("org.metawatch.manager.widgets_desired", temp);
		}

		intent.putExtras(b);

		context.sendBroadcast(intent);
		lastWidgetBroadcast = System.currentTimeMillis();
	    }

	    return dataCache;

	}
    }

    public Map<String, WidgetData> getCachedWidgets(Context context, ArrayList<CharSequence> widgetsDesired) {
	if (dataCache == null)
	    return refreshWidgets(context, widgetsDesired);

	return dataCache;
    }

    public List<WidgetRow> getDesiredWidgetsFromPrefs(Context context) {

	String[] rows = MetaWatchService.getWidgets(context).split("\\|");

	List<WidgetRow> result = new ArrayList<WidgetRow>();

	for (String line : rows) {
	    WidgetRow row = new WidgetRow();
	    String[] widgets = line.split(",");
	    for (String widget : widgets) {
		row.add(widget);
	    }
	    result.add(row);
	}

	return result;
    }

    public void getFromIntent(Context context, Intent intent) {

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "WidgetManager.getFromIntent()");

	synchronized (lock) {

	    WidgetData widget = new WidgetData();

	    Bundle b = intent.getExtras();

	    if (!b.containsKey("id") || !b.containsKey("desc") || !b.containsKey("width") || !b.containsKey("height") || !b.containsKey("priority") || !b.containsKey("array")) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Malformed WIDGET_UPDATE intent");
		return;
	    }

	    widget.id = b.getString("id");
	    widget.description = b.getString("desc");
	    widget.width = b.getInt("width");
	    widget.height = b.getInt("height");
	    widget.priority = b.getInt("priority");

	    int[] buffer = b.getIntArray("array");

	    widget.bitmap = Bitmap.createBitmap(widget.width, widget.height, Bitmap.Config.RGB_565);
	    widget.bitmap.setPixels(buffer, 0, widget.width, 0, 0, widget.width, widget.height);

	    if (dataCache == null)
		dataCache = new HashMap<String, WidgetData>();

	    dataCache.put(widget.id, widget);
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Received widget " + widget.id + " successfully");

	    Idle.getInstance().updateIdle(context, false); // false as we don't want to
					     // trigger another UPDATE
					     // broadcast

	}
    }

    public void resetWidgetsToDefaults(Context context) {

	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	Editor editor = sharedPreferences.edit();

	if (MetaWatchService.watchType == WatchType.ANALOG) {
	    editor.putString("widgetsAnalog", defaultWidgetsAnalog);
	} else {
	    editor.putString("widgets", defaultWidgetsDigital);
	}
	editor.commit();

	Toast toast = Toast.makeText(context, "Reset widget layouts", Toast.LENGTH_SHORT);
	toast.show();

	Idle.getInstance().updateIdle(context, true);
    }
}
