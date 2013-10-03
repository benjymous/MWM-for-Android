package org.metawatch.manager.widgets;

import java.util.ArrayList;
import java.util.Map;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.MediaControl;
import org.metawatch.manager.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.text.Layout;
import android.text.TextPaint;

public class NowPlayingWidget implements InternalWidget {

    public final static String id_0 = "nowplaying_96_32";
    final static String desc_0 = "Now Playing (96x32)";

    public final static String id_1 = "nowplaying_80_16";
    final static String desc_1 = "Now Playing (80x16)";

    private Context context;
    private TextPaint paintSmall;
    private TextPaint paintSmallNumerals;

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
    }

    public void shutdown() {
	paintSmall = null;
    }

    public void refresh(ArrayList<CharSequence> widgetIds) {
    }

    public void get(ArrayList<CharSequence> widgetIds, Map<String, WidgetData> result) {

	if (widgetIds == null || widgetIds.contains(id_0)) {
	    result.put(id_0, GenWidget(id_0));
	}

	if (widgetIds == null || widgetIds.contains(id_1)) {
	    result.put(id_1, GenWidget(id_1));
	}
    }

    private InternalWidget.WidgetData GenWidget(String widget_id) {
	InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

	if (widget_id == id_0) {
	    widget.id = id_0;
	    widget.description = desc_0;
	    widget.width = 96;
	    widget.height = 32;
	} else if (widget_id == id_1) {
	    widget.id = id_1;
	    widget.description = desc_1;
	    widget.width = 80;
	    widget.height = 16;
	}

	MediaControl.TrackInfo info = MediaControl.getInstance().getLastTrack();
	widget.priority = info.isEmpty() ? -1 : 10;

	String trackInfoText;
	if (info.isEmpty()) {
	    trackInfoText = "Nothing playing";
	} else {
	    StringBuilder text = new StringBuilder();
	    text.append(info.track);
	    if (!info.artist.equals("")) {
		if (text.length() > 0)
		    text.append("\n");
		text.append(info.artist);
	    }
	    if (!info.album.equals("")) {
		if (text.length() > 0)
		    text.append("\n");
		text.append(info.album);
	    }
	    trackInfoText = text.toString();
	}

	widget.bitmap = Bitmap.createBitmap(widget.width, widget.height, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(widget.bitmap);
	canvas.drawColor(Color.WHITE);

	Bitmap image = Utils.getBitmap(context, "idle_music.bmp");
	canvas.drawBitmap(image, 0, 1, null);

	Utils.autoText(context, canvas, trackInfoText, 17, 1, widget.width - 17, widget.height - 2, Layout.Alignment.ALIGN_CENTER, Color.BLACK);

	return widget;
    }
}
