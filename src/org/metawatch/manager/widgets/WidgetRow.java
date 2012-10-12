package org.metawatch.manager.widgets;

import java.util.ArrayList;
import java.util.Map;

import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.widgets.InternalWidget.WidgetData;

import android.graphics.Canvas;

public class WidgetRow {
	
	ArrayList<CharSequence> widgetIDs = new ArrayList<CharSequence>();
	
	ArrayList<WidgetData> widgets = null;
	int totalWidth = 0;
	int totalHeight = 0;
	
	private int screenWidth() {
		if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL)
			return 96;
		else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG)
			return 80;
		else
			return 0;
	}
	
	public void add(String id) {
		widgetIDs.add(id);
	}
	
	public ArrayList<CharSequence> getIds() {
		return widgetIDs;
	}
	
	public void doLayout(Map<String,WidgetData> widgetData) {
		widgets = new ArrayList<WidgetData>();
		
		final int screenWidth = screenWidth();
		
		int priorityCutoff = (Preferences.hideEmptyWidgets && !Preferences.hiddenWidgetsReserveSpace) ? 0 : -1; 
		
		totalWidth = 0;
		for( CharSequence id : widgetIDs ) {
			WidgetData widget = widgetData.get(id);
			if(widget!=null && widget.bitmap!=null && widget.priority>priorityCutoff) {
				widgets.add(widget);
				totalWidth += widget.width;
			}
		}
		
		// Cull widgets to fit
		while(totalWidth>screenWidth) {
			int lowestPri = Integer.MAX_VALUE;
			int cull = -1;
			for(int i=0; i<widgets.size(); ++i) {
				int pri = widgets.get(i).priority;
				if(pri <= lowestPri) {
					cull = i;
					lowestPri = pri;
				}
			}
			if(cull>-1)
			{
				totalWidth-=widgets.get(cull).width;
				widgets.remove(cull);
			}
			else
			{
				widgets = null;
				return;
			}
		}
		
		totalHeight = 0;
		for(WidgetData widget : widgets)
			totalHeight = Math.max(totalHeight, widget.height);
	}
	
	public int getWidth() {
		if(widgets==null)
			return 0;
		return totalWidth;
	}
	
	public int getHeight() {
		if(widgets==null)
			return 0;
		return totalHeight;
	}
	
	public void draw(Map<String,WidgetData> widgetData, Canvas canvas, int y)
	{
		if (widgets==null)
			return;
		
		final float space = (float)(screenWidth()-totalWidth)/(float)(2*(widgets.size()));		
		float x=space;
		for(WidgetData widget : widgets) {
			int yAdd = 0;
			if(widget.height<totalHeight)
				yAdd = (totalHeight/2)-(widget.height/2);

			if ( !(Preferences.hideEmptyWidgets && Preferences.hiddenWidgetsReserveSpace && (widget.priority < 1))) 
				canvas.drawBitmap(widget.bitmap, (int)x, y+yAdd, null);
			
			x += ((space*2)+widget.width);
		}	
	}
}
