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

public class GmailWidget implements InternalWidget {

	public final static String id_0 = "unreadGmail_24_32";
	final static String desc_0 = "Unread Gmail (24x32)";

	public final static String id_1 = "unreadGmail_16_16";
	final static String desc_1 = "Unread Gmail (16x16)";
	
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

	public void get(ArrayList<CharSequence> widgetIds, Map<String,WidgetData> result) {

		if(widgetIds == null || widgetIds.contains(id_0)) {		
			result.put(id_0, GenWidget(id_0));
		}
		
		if(widgetIds == null || widgetIds.contains(id_1)) {		
			result.put(id_1, GenWidget(id_1));
		}
	}
	
	private InternalWidget.WidgetData GenWidget(String widget_id) {
		InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

		String iconFile="";
		if( widget_id == id_0 ) {
			widget.id = id_0;
			widget.description = desc_0;
			widget.width = 24;
			widget.height = 32;
			iconFile = "idle_gmail.bmp";
		}
		else if( widget_id == id_1 ) {
			widget.id = id_1;
			widget.description = desc_1;
			widget.width = 16;
			widget.height = 16;
			iconFile = "idle_gmail_10.bmp";
		}
		
		Bitmap icon = Utils.loadBitmapFromAssets(context, iconFile);

		int count = Utils.getUnreadGmailCount(context);

		widget.priority = count;		
		widget.bitmap = Utils.DrawIconCountWidget(context, widget.width, widget.height, icon, count, widget.width == 24 ? paintSmall : paintSmallNumerals);
		
		return widget;
	}
	


}
