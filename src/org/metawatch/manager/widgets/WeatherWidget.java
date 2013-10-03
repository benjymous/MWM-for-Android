package org.metawatch.manager.widgets;

import java.util.ArrayList;
import java.util.Map;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.GeolocationMode;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Monitors;
import org.metawatch.manager.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;

public class WeatherWidget implements InternalWidget {
    public final static String id_0 = "weather_24_32";
    final static String desc_0 = "Current Weather (24x32)";

    public final static String id_1 = "weather_96_32";
    final static String desc_1 = "Current Weather (96x32)";

    public final static String id_2 = "weather_fc_96_32";
    final static String desc_2 = "Weather Forecast (96x32)";

    public final static String id_3 = "moon_24_32";
    final static String desc_3 = "Moon Phase (24x32)";

    public final static String id_4 = "weather_80_16";
    final static String desc_4 = "Current Weather (80x16)";

    public final static String id_5 = "moon_16_16";
    final static String desc_5 = "Moon Phase (16x16)";

    public final static String id_6 = "weather_fc_80_16";
    final static String desc_6 = "Weather Forecast (80x16)";

    public final static String id_7 = "weather_48_32";
    final static String desc_7 = "Current Weather (48x32)";

    public final static String id_8 = "weather_ic_12_12";
    final static String desc_8 = "Weather Icon (12x12)";

    public final static String id_9 = "weather_ic_24_24";
    final static String desc_9 = "Weather Icon (24x24)";

    public final static String id_10 = "weather_24_16";
    final static String desc_10 = "Current Weather (24x16)";

    public final static String id_11 = "weather_46_46";
    final static String desc_11 = "Current Weather (46x46)";

    private Context context = null;
    private TextPaint paintSmall;
    private TextPaint paintSmallOutline;
    private TextPaint paintLarge;
    private TextPaint paintLargeOutline;
    private TextPaint paintSmallNumerals;
    private TextPaint paintSmallNumeralsOutline;

    public void init(Context context, ArrayList<CharSequence> widgetIds) {
	this.context = context;

	paintSmall = new TextPaint();
	paintSmall.setColor(Color.BLACK);
	paintSmall.setTextSize(FontCache.instance(context).Small.size);
	paintSmall.setTypeface(FontCache.instance(context).Small.face);

	paintSmallOutline = new TextPaint();
	paintSmallOutline.setColor(Color.WHITE);
	paintSmallOutline.setTextSize(FontCache.instance(context).Small.size);
	paintSmallOutline.setTypeface(FontCache.instance(context).Small.face);

	paintLarge = new TextPaint();
	paintLarge.setColor(Color.BLACK);
	paintLarge.setTextSize(FontCache.instance(context).Large.size);
	paintLarge.setTypeface(FontCache.instance(context).Large.face);

	paintLargeOutline = new TextPaint();
	paintLargeOutline.setColor(Color.WHITE);
	paintLargeOutline.setTextSize(FontCache.instance(context).Large.size);
	paintLargeOutline.setTypeface(FontCache.instance(context).Large.face);

	paintSmallNumerals = new TextPaint();
	paintSmallNumerals.setColor(Color.BLACK);
	paintSmallNumerals.setTextSize(FontCache.instance(context).SmallNumerals.size);
	paintSmallNumerals.setTypeface(FontCache.instance(context).SmallNumerals.face);

	paintSmallNumeralsOutline = new TextPaint();
	paintSmallNumeralsOutline.setColor(Color.WHITE);
	paintSmallNumeralsOutline.setTextSize(FontCache.instance(context).SmallNumerals.size);
	paintSmallNumeralsOutline.setTypeface(FontCache.instance(context).SmallNumerals.face);
    }

    public void shutdown() {
	paintSmall = null;
    }

    public void refresh(ArrayList<CharSequence> widgetIds) {
    }

    public void get(ArrayList<CharSequence> widgetIds, Map<String, WidgetData> result) {

	if (context == null)
	    return;

	if (widgetIds == null || widgetIds.contains(id_0)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_0;
	    widget.description = desc_0;
	    widget.width = 24;
	    widget.height = 32;

	    widget.bitmap = draw0();
	    widget.priority = calcPriority();

	    result.put(widget.id, widget);
	}

	if (widgetIds == null || widgetIds.contains(id_1)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_1;
	    widget.description = desc_1;
	    widget.width = 96;
	    widget.height = 32;

	    widget.bitmap = draw1();
	    widget.priority = calcPriority();

	    result.put(widget.id, widget);
	}

	if (widgetIds == null || widgetIds.contains(id_2)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_2;
	    widget.description = desc_2;
	    widget.width = 96;
	    widget.height = 32;

	    widget.bitmap = draw2();
	    widget.priority = calcPriority();

	    result.put(widget.id, widget);
	}

	if (widgetIds == null || widgetIds.contains(id_3)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_3;
	    widget.description = desc_3;
	    widget.width = 24;
	    widget.height = 32;

	    widget.bitmap = draw3();
	    widget.priority = Monitors.getInstance().weatherData.moonPercentIlluminated != -1 ? calcPriority() : -1;

	    result.put(widget.id, widget);
	}

	if (widgetIds == null || widgetIds.contains(id_4)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_4;
	    widget.description = desc_4;
	    widget.width = 80;
	    widget.height = 16;

	    widget.bitmap = draw4();
	    widget.priority = calcPriority();

	    result.put(widget.id, widget);
	}

	if (widgetIds == null || widgetIds.contains(id_5)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_5;
	    widget.description = desc_5;
	    widget.width = 16;
	    widget.height = 16;

	    widget.bitmap = draw5();
	    widget.priority = Monitors.getInstance().weatherData.moonPercentIlluminated != -1 ? calcPriority() : -1;

	    result.put(widget.id, widget);
	}

	if (widgetIds == null || widgetIds.contains(id_6)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_6;
	    widget.description = desc_6;
	    widget.width = 80;
	    widget.height = 16;

	    widget.bitmap = draw6();
	    widget.priority = calcPriority();

	    result.put(widget.id, widget);
	}

	if (widgetIds == null || widgetIds.contains(id_7)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_7;
	    widget.description = desc_7;
	    widget.width = 48;
	    widget.height = 32;

	    widget.bitmap = draw7();
	    widget.priority = calcPriority();

	    result.put(widget.id, widget);
	}

	if (widgetIds == null || widgetIds.contains(id_8)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_8;
	    widget.description = desc_8;
	    widget.width = 12;
	    widget.height = 12;

	    widget.bitmap = draw8();
	    widget.priority = calcPriority();

	    result.put(widget.id, widget);
	}

	if (widgetIds == null || widgetIds.contains(id_9)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_9;
	    widget.description = desc_9;
	    widget.width = 24;
	    widget.height = 24;

	    widget.bitmap = draw9();
	    widget.priority = calcPriority();

	    result.put(widget.id, widget);
	}

	if (widgetIds == null || widgetIds.contains(id_10)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_10;
	    widget.description = desc_10;
	    widget.width = 24;
	    widget.height = 16;

	    widget.bitmap = draw10();
	    widget.priority = calcPriority();

	    result.put(widget.id, widget);
	}

	if (widgetIds == null || widgetIds.contains(id_11)) {
	    InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	    widget.id = id_11;
	    widget.description = desc_11;
	    widget.width = 46;
	    widget.height = 46;

	    widget.bitmap = draw11();
	    widget.priority = calcPriority();

	    result.put(widget.id, widget);
	}
    }

    private int calcPriority() {
	if (Preferences.weatherProvider == MetaWatchService.WeatherProvider.DISABLED)
	    return -1;

	return Monitors.getInstance().weatherData.received ? 1 : 0;
    }

    private Bitmap draw0() {
	Bitmap bitmap = Bitmap.createBitmap(24, 32, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	if (Monitors.getInstance().weatherData.received && Monitors.getInstance().weatherData.forecast != null && Monitors.getInstance().weatherData.forecast.length > 0) {

	    // icon
	    Bitmap image = Utils.getBitmap(context, Monitors.getInstance().weatherData.icon);
	    canvas.drawBitmap(image, 0, 4, null);

	    // temperatures
	    if (Monitors.getInstance().weatherData.celsius) {
		Utils.drawOutlinedText(Monitors.getInstance().weatherData.temp + "°C", canvas, 0, 7, paintSmall, paintSmallOutline);
	    } else {
		Utils.drawOutlinedText(Monitors.getInstance().weatherData.temp + "°F", canvas, 0, 7, paintSmall, paintSmallOutline);
	    }
	    paintLarge.setTextAlign(Paint.Align.LEFT);

	    Utils.drawOutlinedText("H " + Monitors.getInstance().weatherData.forecast[0].getTempHigh(), canvas, 0, 25, paintSmall, paintSmallOutline);
	    Utils.drawOutlinedText("L " + Monitors.getInstance().weatherData.forecast[0].getTempLow(), canvas, 0, 31, paintSmall, paintSmallOutline);

	} else {
	    paintSmall.setTextAlign(Paint.Align.CENTER);

	    canvas.drawText("Wait", 12, 16, paintSmall);

	    paintSmall.setTextAlign(Paint.Align.LEFT);
	}

	return bitmap;
    }

    private Bitmap draw1() {
	Bitmap bitmap = Bitmap.createBitmap(96, 32, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	if (Monitors.getInstance().weatherData.received) {

	    // icon
	    Bitmap image = Utils.getBitmap(context, Monitors.getInstance().weatherData.icon);
	    if (Preferences.overlayWeatherText)
		canvas.drawBitmap(image, 36, 5, null);
	    else
		canvas.drawBitmap(image, 34, 1, null);

	    // condition
	    if (Preferences.overlayWeatherText)
		Utils.drawWrappedOutlinedText(Monitors.getInstance().weatherData.condition, canvas, 1, 2, 60, paintSmall, paintSmallOutline, Layout.Alignment.ALIGN_NORMAL);
	    else
		Utils.drawWrappedOutlinedText(Monitors.getInstance().weatherData.condition, canvas, 1, 2, 34, paintSmall, paintSmallOutline, Layout.Alignment.ALIGN_NORMAL);

	    // temperatures
	    paintLarge.setTextAlign(Paint.Align.RIGHT);
	    paintLargeOutline.setTextAlign(Paint.Align.RIGHT);
	    Utils.drawOutlinedText(Monitors.getInstance().weatherData.temp, canvas, 82, 13, paintLarge, paintLargeOutline);
	    if (Monitors.getInstance().weatherData.celsius) {
		// RM: since the degree symbol draws wrong...
		canvas.drawText("O", 82, 7, paintSmall);
		canvas.drawText("C", 95, 13, paintLarge);
	    } else {
		// RM: since the degree symbol draws wrong...
		canvas.drawText("O", 83, 7, paintSmall);
		canvas.drawText("F", 95, 13, paintLarge);
	    }
	    paintLarge.setTextAlign(Paint.Align.LEFT);

	    if (Monitors.getInstance().weatherData.forecast != null && Monitors.getInstance().weatherData.forecast.length > 0) {
		final String high = Monitors.getInstance().weatherData.forecast[0].getTempHigh();
		final String low = Monitors.getInstance().weatherData.forecast[0].getTempLow();
		final boolean shortLabel = (high.length() > 2 || low.length() > 2);
		canvas.drawText(shortLabel ? "Hi" : "High", 64, 23, paintSmall);
		canvas.drawText(shortLabel ? "Lo" : "Low", 64, 31, paintSmall);

		paintSmall.setTextAlign(Paint.Align.RIGHT);
		canvas.drawText(high, 95, 23, paintSmall);
		canvas.drawText(low, 95, 31, paintSmall);
		paintSmall.setTextAlign(Paint.Align.LEFT);
	    }

	    Utils.drawOutlinedText((String) TextUtils.ellipsize(Monitors.getInstance().weatherData.locationName, paintSmall, 63, TruncateAt.END), canvas, 1, 31, paintSmall, paintSmallOutline);

	} else {
	    paintSmall.setTextAlign(Paint.Align.CENTER);
	    if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
		if (!Monitors.getInstance().mLocationData.received) {
		    canvas.drawText("Awaiting location", 48, 18, paintSmall);
		} else {
		    canvas.drawText("Awaiting weather", 48, 18, paintSmall);
		}
	    } else {
		canvas.drawText("No data", 48, 18, paintSmall);
	    }
	    paintSmall.setTextAlign(Paint.Align.LEFT);
	}

	return bitmap;
    }

    private Bitmap draw2() {
	Bitmap bitmap = Bitmap.createBitmap(96, 32, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	paintSmall.setTextAlign(Align.LEFT);
	paintSmallOutline.setTextAlign(Align.LEFT);

	if (Monitors.getInstance().weatherData.received && Monitors.getInstance().weatherData.forecast != null && Monitors.getInstance().weatherData.forecast.length > 1) {
	    int weatherIndex = 0;
	    if (Monitors.getInstance().weatherData.forecast.length > 4)
		weatherIndex = 1; // Start with tomorrow's weather if we've got
				  // enough entries

	    int max = Math.min(4, Monitors.getInstance().weatherData.forecast.length);

	    for (int i = 0; i < max; ++i) {
		int x = i * 24;

		if (max == 2)
		    x += (i + 1) * 16;
		else if (max == 3)
		    x += (i + 1) * 12;

		Bitmap image = Utils.getBitmap(context, Monitors.getInstance().weatherData.forecast[weatherIndex].getIcon());
		canvas.drawBitmap(image, x, 4, null);
		Utils.drawOutlinedText(Monitors.getInstance().weatherData.forecast[weatherIndex].getDay(), canvas, x, 6, paintSmall, paintSmallOutline);

		Utils.drawOutlinedText("H " + Monitors.getInstance().weatherData.forecast[weatherIndex].getTempHigh(), canvas, x, 25, paintSmall, paintSmallOutline);
		Utils.drawOutlinedText("L " + Monitors.getInstance().weatherData.forecast[weatherIndex].getTempLow(), canvas, x, 31, paintSmall, paintSmallOutline);

		weatherIndex++;
	    }
	} else {
	    paintSmall.setTextAlign(Paint.Align.CENTER);
	    if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
		if (!Monitors.getInstance().mLocationData.received) {
		    canvas.drawText("Awaiting location", 48, 18, paintSmall);
		} else {
		    canvas.drawText("Awaiting weather", 48, 18, paintSmall);
		}
	    } else {
		canvas.drawText("No data", 48, 18, paintSmall);
	    }
	    paintSmall.setTextAlign(Paint.Align.LEFT);
	}

	return bitmap;
    }

    final static int[] phaseImage = { 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 5, 5, 5, 5, 5, 6, 6, 7, 7, 7, 7, 0, 0, 0 };

    private Bitmap draw3() {
	Bitmap bitmap = Bitmap.createBitmap(24, 32, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	paintSmall.setTextAlign(Paint.Align.CENTER);

	final boolean shouldInvert = Preferences.invertLCD || (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG);

	if (Monitors.getInstance().weatherData.received && Monitors.getInstance().weatherData.ageOfMoon >= 0 && Monitors.getInstance().weatherData.ageOfMoon < phaseImage.length) {
	    int moonPhase = Monitors.getInstance().weatherData.ageOfMoon;
	    int moonImage = phaseImage[moonPhase];
	    int x = 0 - (moonImage * 24);
	    int y = (Preferences.displayWidgetIconOnTop) ? 0 : 8;
	    Bitmap image = shouldInvert ? Utils.getBitmap(context, "moon-inv.bmp") : Utils.getBitmap(context, "moon.bmp");
	    canvas.drawBitmap(image, x, y, null);

	    canvas.drawText(Integer.toString(Monitors.getInstance().weatherData.moonPercentIlluminated) + "%", 12, (Preferences.displayWidgetIconOnTop) ? 30 : 6, paintSmall);
	} else {
	    canvas.drawText("Wait", 12, 16, paintSmall);
	}

	paintSmall.setTextAlign(Paint.Align.LEFT);

	return bitmap;
    }

    private Bitmap draw4() {
	Bitmap bitmap = Bitmap.createBitmap(80, 16, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	if (Monitors.getInstance().weatherData.received) {

	    // icon
	    String smallIcon = Monitors.getInstance().weatherData.icon.replace(".bmp", "_12.bmp");
	    Bitmap image = Utils.getBitmap(context, smallIcon);
	    canvas.drawBitmap(image, 46, 2, null);

	    // condition
	    Utils.drawWrappedOutlinedText(Monitors.getInstance().weatherData.condition, canvas, 0, 0, 60, paintSmall, paintSmallOutline, Layout.Alignment.ALIGN_NORMAL);

	    // temperatures

	    paintSmall.setTextAlign(Paint.Align.RIGHT);
	    paintSmallOutline.setTextAlign(Paint.Align.RIGHT);

	    StringBuilder string = new StringBuilder();
	    string.append(Monitors.getInstance().weatherData.temp);

	    if (Monitors.getInstance().weatherData.celsius) {
		string.append("°C");
	    } else {
		string.append("°F");
	    }
	    Utils.drawOutlinedText(string.toString(), canvas, 80, 5, paintSmall, paintSmallOutline);

	    if (Monitors.getInstance().weatherData.forecast != null && Monitors.getInstance().weatherData.forecast.length > 0) {
		string = new StringBuilder();
		string.append(Monitors.getInstance().weatherData.forecast[0].getTempHigh());
		string.append("/");
		string.append(Monitors.getInstance().weatherData.forecast[0].getTempLow());

		Utils.drawOutlinedText(string.toString(), canvas, 80, 16, paintSmall, paintSmallOutline);
	    }
	    paintSmall.setTextAlign(Paint.Align.LEFT);
	    paintSmallOutline.setTextAlign(Paint.Align.LEFT);

	    Utils.drawOutlinedText((String) TextUtils.ellipsize(Monitors.getInstance().weatherData.locationName, paintSmall, 47, TruncateAt.END), canvas, 0, 16, paintSmall, paintSmallOutline);

	} else {
	    paintSmall.setTextAlign(Paint.Align.CENTER);
	    if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
		if (!Monitors.getInstance().mLocationData.received) {
		    canvas.drawText("Awaiting location", 40, 9, paintSmall);
		} else {
		    canvas.drawText("Awaiting weather", 40, 9, paintSmall);
		}
	    } else {
		canvas.drawText("No data", 40, 9, paintSmall);
	    }
	    paintSmall.setTextAlign(Paint.Align.LEFT);
	}

	return bitmap;
    }

    private Bitmap draw5() {
	Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	final boolean shouldInvert = Preferences.invertLCD || (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG);

	paintSmall.setTextAlign(Paint.Align.CENTER);
	if (Monitors.getInstance().weatherData.received && Monitors.getInstance().weatherData.ageOfMoon >= 0 && Monitors.getInstance().weatherData.ageOfMoon < phaseImage.length) {
	    int moonPhase = Monitors.getInstance().weatherData.ageOfMoon;
	    int moonImage = phaseImage[moonPhase];
	    int x = 0 - (moonImage * 16);
	    Bitmap image = shouldInvert ? Utils.getBitmap(context, "moon-inv_10.bmp") : Utils.getBitmap(context, "moon_10.bmp");
	    canvas.drawBitmap(image, x, 0, null);
	} else {
	    canvas.drawText("--", 8, 9, paintSmall);
	}
	paintSmall.setTextAlign(Paint.Align.LEFT);

	return bitmap;
    }

    private Bitmap draw6() {
	Bitmap bitmap = Bitmap.createBitmap(80, 16, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	paintSmall.setTextAlign(Align.LEFT);
	paintSmallOutline.setTextAlign(Align.LEFT);

	if (Monitors.getInstance().weatherData.received && Monitors.getInstance().weatherData.forecast != null && Monitors.getInstance().weatherData.forecast.length > 1) {
	    int weatherIndex = 0;
	    if (Monitors.getInstance().weatherData.forecast.length > 3)
		weatherIndex = 1; // Start with tomorrow's weather if we've got
				  // enough entries

	    final int max = Math.min(3, Monitors.getInstance().weatherData.forecast.length);

	    for (int i = 0; i < max; ++i) {
		int x = i * 26;
		if (max == 2)
		    x += (i + 1) * 8;
		final String smallIcon = Monitors.getInstance().weatherData.forecast[weatherIndex].getIcon().replace(".bmp", "_12.bmp");
		Bitmap image = Utils.getBitmap(context, smallIcon);
		canvas.drawBitmap(image, x + 12, 0, null);
		Utils.drawOutlinedText(Monitors.getInstance().weatherData.forecast[weatherIndex].getDay().substring(0, 2), canvas, x + 1, 6, paintSmall, paintSmallOutline);

		StringBuilder hilow = new StringBuilder();
		hilow.append(Monitors.getInstance().weatherData.forecast[weatherIndex].getTempHigh());
		hilow.append("/");
		hilow.append(Monitors.getInstance().weatherData.forecast[weatherIndex].getTempLow());

		Utils.drawOutlinedText(hilow.toString(), canvas, x + 1, 16, paintSmallNumerals, paintSmallNumeralsOutline);

		weatherIndex++;
	    }
	} else {
	    paintSmall.setTextAlign(Paint.Align.CENTER);
	    if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
		if (!Monitors.getInstance().mLocationData.received) {
		    canvas.drawText("Awaiting location", 40, 8, paintSmall);
		} else {
		    canvas.drawText("Awaiting weather", 40, 8, paintSmall);
		}
	    } else {
		canvas.drawText("No data", 40, 8, paintSmall);
	    }
	    paintSmall.setTextAlign(Paint.Align.LEFT);
	}

	return bitmap;
    }

    private Bitmap draw7() {
	Bitmap bitmap = Bitmap.createBitmap(48, 32, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	if (Monitors.getInstance().weatherData.received) {

	    // icon
	    Bitmap image = Utils.getBitmap(context, Monitors.getInstance().weatherData.icon);

	    canvas.drawBitmap(image, 0, 0, null);

	    // temperatures
	    paintLarge.setTextAlign(Paint.Align.RIGHT);
	    paintLargeOutline.setTextAlign(Paint.Align.RIGHT);
	    Utils.drawOutlinedText(Monitors.getInstance().weatherData.temp, canvas, 43, 13, paintLarge, paintLargeOutline);
	    if (Monitors.getInstance().weatherData.celsius) {
		canvas.drawText("C", 43, 7, paintSmall);
	    } else {
		canvas.drawText("F", 43, 7, paintSmall);
	    }
	    paintLarge.setTextAlign(Paint.Align.LEFT);

	    if (Monitors.getInstance().weatherData.forecast != null && Monitors.getInstance().weatherData.forecast.length > 0) {

		StringBuilder builder = new StringBuilder();
		builder.append(Monitors.getInstance().weatherData.forecast[0].getTempHigh());
		builder.append("/");
		builder.append(Monitors.getInstance().weatherData.forecast[0].getTempLow());

		paintSmall.setTextAlign(Paint.Align.RIGHT);
		canvas.drawText(builder.toString(), 47, 21, paintSmall);
		paintSmall.setTextAlign(Paint.Align.LEFT);
	    }

	    Utils.drawOutlinedText((String) TextUtils.ellipsize(Monitors.getInstance().weatherData.locationName, paintSmall, 48, TruncateAt.END), canvas, 0, 30, paintSmall, paintSmallOutline);

	} else {
	    paintSmall.setTextAlign(Paint.Align.CENTER);
	    if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
		canvas.drawText("Awaiting", 24, 15, paintSmall);
		if (!Monitors.getInstance().mLocationData.received) {
		    canvas.drawText("location", 24, 21, paintSmall);
		} else {
		    canvas.drawText("weather", 24, 21, paintSmall);
		}
	    } else {
		canvas.drawText("No data", 24, 18, paintSmall);
	    }
	    paintSmall.setTextAlign(Paint.Align.LEFT);
	}

	return bitmap;
    }

    private Bitmap draw8() {
	Bitmap bitmap = Bitmap.createBitmap(12, 12, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	if (Monitors.getInstance().weatherData.received && Monitors.getInstance().weatherData.forecast != null && Monitors.getInstance().weatherData.forecast.length > 0) {
	    // icon
	    final String icon = Monitors.getInstance().weatherData.icon.replace(".bmp", "_12.bmp");
	    Bitmap image = Utils.getBitmap(context, icon);
	    canvas.drawBitmap(image, 0, 0, null);
	} else {
	    paintSmall.setTextAlign(Paint.Align.CENTER);

	    canvas.drawText("--", 6, 8, paintSmall);

	    paintSmall.setTextAlign(Paint.Align.LEFT);
	}

	return bitmap;
    }

    private Bitmap draw9() {
	Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	if (Monitors.getInstance().weatherData.received && Monitors.getInstance().weatherData.forecast != null && Monitors.getInstance().weatherData.forecast.length > 0) {
	    // icon
	    Bitmap image = Utils.getBitmap(context, Monitors.getInstance().weatherData.icon);
	    canvas.drawBitmap(image, 0, 0, null);
	} else {
	    paintSmall.setTextAlign(Paint.Align.CENTER);

	    canvas.drawText("Wait", 6, 8, paintSmall);

	    paintSmall.setTextAlign(Paint.Align.LEFT);
	}

	return bitmap;
    }

    private Bitmap draw10() {
	Bitmap bitmap = Bitmap.createBitmap(24, 16, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	paintSmall.setTextAlign(Align.LEFT);
	paintSmallOutline.setTextAlign(Align.LEFT);

	if (Monitors.getInstance().weatherData.received && Monitors.getInstance().weatherData.forecast != null && Monitors.getInstance().weatherData.forecast.length > 0) {

	    final String smallIcon = Monitors.getInstance().weatherData.icon.replace(".bmp", "_12.bmp");
	    Bitmap image = Utils.getBitmap(context, smallIcon);
	    canvas.drawBitmap(image, 12, 0, null);

	    Utils.drawOutlinedText(Monitors.getInstance().weatherData.temp, canvas, 1, 7, paintSmallNumerals, paintSmallNumeralsOutline);

	    StringBuilder hilow = new StringBuilder();
	    hilow.append(Monitors.getInstance().weatherData.forecast[0].getTempHigh());
	    hilow.append("/");
	    hilow.append(Monitors.getInstance().weatherData.forecast[0].getTempLow());

	    Utils.drawOutlinedText(hilow.toString(), canvas, 1, 16, paintSmallNumerals, paintSmallNumeralsOutline);

	} else {
	    paintSmall.setTextAlign(Paint.Align.CENTER);

	    canvas.drawText("Wait", 12, 8, paintSmall);

	    paintSmall.setTextAlign(Paint.Align.LEFT);
	}

	return bitmap;
    }

    private Bitmap draw11() {
	Bitmap bitmap = Bitmap.createBitmap(46, 46, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	if (Monitors.getInstance().weatherData.received) {

	    // icon
	    Bitmap image = Utils.getBitmap(context, Monitors.getInstance().weatherData.icon);

	    canvas.drawBitmap(image, 0, 7, null);

	    // temperatures
	    paintLarge.setTextAlign(Paint.Align.RIGHT);
	    paintLargeOutline.setTextAlign(Paint.Align.RIGHT);
	    Utils.drawOutlinedText(Monitors.getInstance().weatherData.temp, canvas, 43, 11, paintLarge, paintLargeOutline);
	    if (Monitors.getInstance().weatherData.celsius) {
		canvas.drawText("C", 43, 5, paintSmall);
	    } else {
		canvas.drawText("F", 43, 5, paintSmall);
	    }
	    paintLarge.setTextAlign(Paint.Align.LEFT);

	    if (Monitors.getInstance().weatherData.forecast != null && Monitors.getInstance().weatherData.forecast.length > 0) {
		paintSmall.setTextAlign(Paint.Align.RIGHT);
		canvas.drawText(Monitors.getInstance().weatherData.forecast[0].getTempHigh(), 47, 19, paintSmall);
		canvas.drawText(Monitors.getInstance().weatherData.forecast[0].getTempLow(), 47, 27, paintSmall);
		paintSmall.setTextAlign(Paint.Align.LEFT);
	    }

	    StringBuilder sb = new StringBuilder();
	    sb.append(Monitors.getInstance().weatherData.locationName);
	    sb.append(" ");
	    sb.append(Monitors.getInstance().weatherData.condition);

	    canvas.save();
	    StaticLayout layout = new StaticLayout(sb.toString(), paintSmall, 46, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);

	    int h = layout.getHeight();
	    if (h > 17)
		h = 17;

	    canvas.translate(0, 46 - h); // position the text
	    layout.draw(canvas);
	    canvas.restore();

	} else {
	    paintSmall.setTextAlign(Paint.Align.CENTER);
	    if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
		canvas.drawText("Awaiting", 23, 22, paintSmall);
		if (!Monitors.getInstance().mLocationData.received) {
		    canvas.drawText("location", 23, 28, paintSmall);
		} else {
		    canvas.drawText("weather", 23, 28, paintSmall);
		}
	    } else {
		canvas.drawText("No data", 23, 25, paintSmall);
	    }
	    paintSmall.setTextAlign(Paint.Align.LEFT);
	}

	return bitmap;
    }

}
