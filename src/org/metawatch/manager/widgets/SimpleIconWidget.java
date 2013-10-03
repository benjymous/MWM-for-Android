package org.metawatch.manager.widgets;

import java.util.ArrayList;
import java.util.Map;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.text.TextPaint;

public abstract class SimpleIconWidget implements InternalWidget {

    protected Context context;
    protected static TextPaint paintLarge;
    protected static TextPaint paintSmall;
    protected static TextPaint paintSmallNumerals;

    private ArrayList<WidgetSize> widgets;

    private class WidgetSize {
	WidgetSize(int w, int h) {
	    width = w;
	    height = h;
	}

	int width;
	int height;

	String getId() {
	    return idBase() + "_" + width + "_" + height;
	}

	String getDescription() {
	    return descBase() + " (" + width + "x" + height + ")";
	}
    }

    public void init(Context context, ArrayList<CharSequence> widgetIds) {
	this.context = context;

	widgets = new ArrayList<WidgetSize>();
	widgets.add(new WidgetSize(16, 16));
	widgets.add(new WidgetSize(24, 32));
	widgets.add(new WidgetSize(46, 46));

	if (paintLarge == null) {
	    paintLarge = new TextPaint();
	    paintLarge.setColor(Color.BLACK);
	    paintLarge.setTextSize(FontCache.instance(context).Large.size);
	    paintLarge.setTypeface(FontCache.instance(context).Large.face);
	    paintLarge.setTextAlign(Align.CENTER);
	}

	if (paintSmall == null) {
	    paintSmall = new TextPaint();
	    paintSmall.setColor(Color.BLACK);
	    paintSmall.setTextSize(FontCache.instance(context).Small.size);
	    paintSmall.setTypeface(FontCache.instance(context).Small.face);
	    paintSmall.setTextAlign(Align.CENTER);
	}

	if (paintSmallNumerals == null) {
	    paintSmallNumerals = new TextPaint();
	    paintSmallNumerals.setColor(Color.BLACK);
	    paintSmallNumerals.setTextSize(FontCache.instance(context).SmallNumerals.size);
	    paintSmallNumerals.setTypeface(FontCache.instance(context).SmallNumerals.face);
	    paintSmallNumerals.setTextAlign(Align.CENTER);
	}

    }

    public void shutdown() {
    }

    public abstract void refresh(ArrayList<CharSequence> widgetIds);

    public void get(ArrayList<CharSequence> widgetIds, Map<String, WidgetData> result) {

	for (WidgetSize ws : widgets) {
	    String id = ws.getId();
	    if (widgetIds == null || widgetIds.contains(id)) {
		result.put(id, GenWidget(ws));
	    }
	}
    }

    private WidgetData GenWidget(WidgetSize ws) {
	InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	String iconFile = "";
	TextPaint textPaint = null;
	widget.id = ws.getId();
	widget.description = ws.getDescription();
	widget.width = ws.width;
	widget.height = ws.height;
	if (ws.height == 16) {
	    iconFile = icon16x16();
	    textPaint = paintSmallNumerals;
	} else if (ws.height == 32) {
	    iconFile = icon24x32();
	    textPaint = paintSmall;
	} else if (ws.height == 46) {
	    iconFile = icon46x46();
	    textPaint = paintLarge;
	}

	Bitmap icon = Utils.getBitmap(context, iconFile);

	int count = getCount();

	widget.priority = count;
	widget.bitmap = Utils.DrawIconCountWidget(context, widget.width, widget.height, icon, count, textPaint);

	return widget;
    }

    protected abstract int getCount();

    protected abstract String idBase();

    protected abstract String descBase();

    protected abstract String icon16x16();

    protected abstract String icon24x32();

    protected abstract String icon46x46();

}
