package org.metawatch.manager.widgets;

import java.util.ArrayList;
import java.util.Map;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.Monitors;
import org.metawatch.manager.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Paint.Align;
import android.text.TextPaint;

public class PhoneStatusWidget implements InternalWidget {

	public final static String id_0 = "phoneStatus_24_32";
	final static String desc_0 = "Phone Battery Status (24x32)";
	
	public final static String id_1 = "phoneStatus_16_16";
	final static String desc_1 = "Phone Battery Status (16x16)";
	
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
			iconFile = "idle_phone_status.bmp";
		}
		else if( widget_id == id_1 ) {
			widget.id = id_1;
			widget.description = desc_1;
			widget.width = 16;
			widget.height = 16; 
			iconFile = "idle_phone_status_10.bmp";
		}
		
		Bitmap icon = Utils.getBitmap(context, iconFile);

		int level = Monitors.BatteryData.level;
		String count = level==-1 ? "-" : level+"%";

		widget.priority = level==-1 ? 0 : 1;		
		widget.bitmap = Bitmap.createBitmap(widget.width, widget.height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(widget.bitmap);
		canvas.drawColor(Color.WHITE);
		
		int height = ((widget_id == id_0) ? 32 : 16);
		TextPaint textPaint = ((widget_id == id_0) ? paintSmall : paintSmallNumerals);
		Point iconOffset = Utils.getIconOffset(height);
		Point textOffset = Utils.getTextOffset(height);
		
		canvas.drawBitmap(icon, iconOffset.x, iconOffset.y, null);
		canvas.drawText(count, textOffset.x, textOffset.y, textPaint);

		if (widget_id == id_0 ) {
////			canvas.drawBitmap(icon, 0, 3, null);
////			canvas.drawText(count, 12, 30,  paintSmall);
//			canvas.drawText(count, 12, 12, paintSmall);
//			canvas.drawBitmap(icon, 0, 14, null);
		
			if(level>-1)
				canvas.drawRect((iconOffset.x+13), (iconOffset.y+5) + ((100-level)/10), (iconOffset.x+19), (iconOffset.y+15), textPaint);
		}
		else if (widget_id == id_1 ) {
//			canvas.drawBitmap(icon, 2, 0, null);
//			canvas.drawText(count, 8, 15,  paintSmallNumerals);
		
			if(level>-1)
				canvas.drawRect((iconOffset.x+7), (iconOffset.y+1) + ((100-level)/12), (iconOffset.x+10), (iconOffset.y+8), textPaint);	
		}
			
		
		return widget;
	}
}
	