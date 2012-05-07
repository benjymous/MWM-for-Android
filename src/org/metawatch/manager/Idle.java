                                                                     
                                                                     
                                                                     
                                             
 /*****************************************************************************
  *  Copyright (c) 2011 Meta Watch Ltd.                                       *
  *  www.MetaWatch.org                                                        *
  *                                                                           *
  =============================================================================
  *                                                                           *
  *  Licensed under the Apache License, Version 2.0 (the "License");          *
  *  you may not use this file except in compliance with the License.         *
  *  You may obtain a copy of the License at                                  *
  *                                                                           *
  *    http://www.apache.org/licenses/LICENSE-2.0                             *
  *                                                                           *
  *  Unless required by applicable law or agreed to in writing, software      *
  *  distributed under the License is distributed on an "AS IS" BASIS,        *
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
  *  See the License for the specific language governing permissions and      *
  *  limitations under the License.                                           *
  *                                                                           *
  *****************************************************************************/

 /*****************************************************************************
  * Idle.java                                                                 *
  * Idle                                                                      *
  * Idle watch mode                                                           *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchType;
import org.metawatch.manager.Notification.VibratePattern;
import org.metawatch.manager.apps.InternalApp;
import org.metawatch.manager.apps.MediaPlayerApp;
import org.metawatch.manager.widgets.InternalWidget.WidgetData;
import org.metawatch.manager.widgets.WidgetManager;
import org.metawatch.manager.widgets.WidgetRow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

public class Idle {
	
	final static byte IDLE_NEXT_PAGE = 60;
	final static byte IDLE_OLED_DISPLAY = 61;
	
	// instance for new-ing
	private static Idle i = new Idle();
	
	private interface IdlePage {
		public void activate(int watchType);
		public void deactivate(int watchType);
		Bitmap draw(Context context, boolean preview, Bitmap bitmap, int watchType);
		public int screenMode();
	}
	
	private class WidgetPage implements IdlePage {

		private ArrayList<WidgetRow> rows;
		private int pageIndex;
		
		public WidgetPage( ArrayList<WidgetRow> r, int p ) {
			rows = r;
			pageIndex = p;
		}
		
		public void activate(int watchType) {
		}

		public void deactivate(int watchType) {	
		}
		
		public Bitmap draw(Context context, boolean preview, Bitmap bitmap, int watchType) {
			
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(Color.WHITE);	
			
			boolean showClock = (pageIndex==0 || Preferences.clockOnEveryPage);
			
			if(watchType == WatchType.DIGITAL && preview && showClock) {
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "dummy_clock.png"), 0, 0, null);
			} 
			
			int totalHeight = 0;
			for(WidgetRow row : rows) {
				totalHeight += row.getHeight();
			}
						
			int space = (watchType == WatchType.DIGITAL) ? (((showClock ? 64:96) - totalHeight) / (rows.size()+1)) : 0;
			int yPos = (watchType == WatchType.DIGITAL) ? (showClock ? 32:0) + space : 0;
			
			for(WidgetRow row : rows) {
				row.draw(widgetData, canvas, yPos);
				yPos += row.getHeight() + space;
			}

			if (watchType == WatchType.DIGITAL && Preferences.displayWidgetRowSeparator) {
				yPos = space/2; // Center the separators between rows.
				if (showClock) {
					yPos += 32;
					drawLine(canvas, yPos);
				}
				int i = 0;
				for(WidgetRow row : rows) {
					if (++i == rows.size())
						continue;
					yPos += row.getHeight() + space;
					drawLine(canvas, yPos);
				}
			}
			
			return bitmap;
		}
		
		public int screenMode() {
			return MetaWatchService.WatchBuffers.IDLE;
		}
	}
	
	private class AppPage implements IdlePage {

		private InternalApp app;
		
		public AppPage(InternalApp arg) {
			app = arg;
		}
		
		public void activate(int watchType) {
			app.activate(watchType);
		}

		public void deactivate(int watchType) {
			app.deactivate(watchType);
		}
		
		public Bitmap draw(Context context, boolean preview, Bitmap bitmap, int watchType) {
			return app.update(context, watchType);
		}	
		
		public int screenMode() {
			return MetaWatchService.WatchBuffers.APPLICATION;
		}
	}

	static int currentPage = 0;
	static boolean widgetsInitialised = false;
	
	static Bitmap oledIdle = null;
	
	public static void nextPage() {
		toPage(currentPage+1);
	}
	
	public static void toPage(int page) {
			
		if(idlePages != null && idlePages.size()>currentPage) {
			idlePages.get(currentPage).deactivate(MetaWatchService.watchType);
		}
				
		currentPage = (page) % numPages();
		
		if(idlePages != null && idlePages.size()>currentPage) {
			idlePages.get(currentPage).activate(MetaWatchService.watchType);
		}
	}
	
	public static int numPages() {	
		return (idlePages==null || idlePages.size()==0) ? 1 : idlePages.size();
	}
	
	private static ArrayList<IdlePage> idlePages = null;
	private static Map<String,WidgetData> widgetData = null;
	
	public static synchronized void updateIdlePages(Context context, boolean refresh)
	{
		if(!widgetsInitialised) {
			WidgetManager.initWidgets(context, null);
			widgetsInitialised = true;
		}
		
		List<WidgetRow> rows = WidgetManager.getDesiredWidgetsFromPrefs(context);
		
		ArrayList<CharSequence> widgetsDesired = new ArrayList<CharSequence>();
		for(WidgetRow row : rows) {
			widgetsDesired.addAll(row.getIds());
		}			
		
		if (refresh)
			widgetData = WidgetManager.refreshWidgets(context, widgetsDesired);
		else
			widgetData = WidgetManager.getCachedWidgets(context, widgetsDesired);
		
		for(WidgetRow row : rows) { 
			row.doLayout(widgetData);
		}
		
		int maxScreenSize = 0;
		
		if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL)
			maxScreenSize = 96;
		else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG)
			maxScreenSize = 32;
		
		// Bucket rows into pages
		ArrayList<IdlePage> screens = new ArrayList<IdlePage>();
	
		int screenSize = 0;
		if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL)
			screenSize = 32; // Initial screen has top part used by the fw clock
		
		ArrayList<WidgetRow> screenRow = new ArrayList<WidgetRow>();
		for(WidgetRow row : rows) { 
			if(screenSize+row.getHeight() > maxScreenSize) {
				screens.add(i.new WidgetPage(screenRow, screens.size()));
				screenRow = new ArrayList<WidgetRow>();
				screenSize = (Preferences.clockOnEveryPage ? 32 : 0);
			}
			screenRow.add(row);
			screenSize += row.getHeight();
		}
		screens.add(i.new WidgetPage(screenRow, screens.size()));
		
		if(Preferences.idleMusicControls) {
			screens.add(i.new AppPage(new MediaPlayerApp()));
		}
		
		idlePages = screens;
	}

	static synchronized Bitmap createIdle(Context context) {
		return createIdle(context, false, currentPage);
	}

	static synchronized Bitmap createIdle(Context context, boolean preview, int page) {
		final int width = (MetaWatchService.watchType==WatchType.DIGITAL) ? 96 : 80;
		final int height = (MetaWatchService.watchType==WatchType.DIGITAL) ? 96 : 32;
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		
		if(idlePages != null && idlePages.size()>page) {
			return idlePages.get(page).draw(context, preview, bitmap, MetaWatchService.watchType);
		}
		
		return bitmap;
	}
	
	public static Canvas drawLine(Canvas canvas, int y) {
	  Paint paint = new Paint();
	  paint.setColor(Color.BLACK);

	  int left = 3;

	  for (int i = 0+left; i < 96-left; i += 3)
	    canvas.drawLine(i, y, i+2, y, paint);
	
	  return canvas;
	}
	
	private static int getScreenMode() {
		int mode = MetaWatchService.WatchBuffers.IDLE;
		if(idlePages != null && idlePages.size()>currentPage) {
			mode = idlePages.get(currentPage).screenMode();
		}
		return mode;
	}
	
	private static synchronized void sendLcdIdle(Context context, boolean refresh) {
		if(MetaWatchService.watchState != MetaWatchService.WatchStates.IDLE) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Ignoring sendLcdIdle as not in idle");
			return;
		}
		
		final int mode = getScreenMode();
		boolean showClock = false;
		
		if(mode == MetaWatchService.WatchBuffers.IDLE) {
			updateIdlePages(context, refresh);
			showClock = (currentPage==0 || Preferences.clockOnEveryPage);
		}
		
		Protocol.sendLcdBitmap(createIdle(context), mode);
		Protocol.configureIdleBufferSize(showClock);
		Protocol.updateLcdDisplay(mode);
	}
	
	public static void enableIdleKeys() {
		if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL) {
			Protocol.enableButton(0, 0, IDLE_NEXT_PAGE, 0); // Right top immediate
			Protocol.enableButton(0, 0, IDLE_NEXT_PAGE, 1); // Right top immediate	
		}
		else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
			Protocol.enableButton(1, 0, IDLE_OLED_DISPLAY, 0); // Middle immediate
			Protocol.enableButton(1, 0, IDLE_OLED_DISPLAY, 1); // Middle immediate
		}
	}
	
	public static boolean toIdle(Context context) {
		
		MetaWatchService.WatchModes.IDLE = true;
		MetaWatchService.watchState = MetaWatchService.WatchStates.IDLE;
		
		if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL) {
			sendLcdIdle(context, true);
				
			if (numPages()>1) {
				Protocol.enableButton(0, 0, IDLE_NEXT_PAGE, 0); // Right top immediate
				Protocol.enableButton(0, 0, IDLE_NEXT_PAGE, 1); // Right top immediate
			}
		
		}
		else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
			Protocol.enableButton(1, 0, IDLE_OLED_DISPLAY, 0); // Middle immediate
			Protocol.enableButton(1, 0, IDLE_OLED_DISPLAY, 1); // Middle immediate
		}

		return true;
	}
	
	public static void updateIdle(Context context, boolean refresh) {
		if (MetaWatchService.watchState == MetaWatchService.WatchStates.IDLE )
			if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL)
				sendLcdIdle(context, refresh);
			else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG)
				updateOledIdle(context, refresh);
	}
	
	private static void updateOledIdle(Context context, boolean refresh) {	
		final int mode = getScreenMode();
		
		if(mode ==  MetaWatchService.WatchBuffers.IDLE)
			updateIdlePages(context, refresh);
				
		// get the 32px full screen
		oledIdle = createIdle(context);
	}
	
	// Send oled widgets view on demand
	public static void sendOledIdle(Context context) {
		if(oledIdle == null) {
	 		updateOledIdle(context, true);
		}
	 		
		final int mode = getScreenMode();
		
		// Split into top/bottom, and send
		for(int i=0; i<2; ++i) {
			Bitmap bitmap = Bitmap.createBitmap(80, 16, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawBitmap(oledIdle, 0, -(i*16), null);
			Protocol.sendOledBitmap(bitmap, mode, i);
		}
		Protocol.oledChangeMode(mode);					
	}
	
	public static void oledTest(Context context, String msg) {
		VibratePattern vibratePattern = new VibratePattern(false, 0, 0, 1);
		Notification.addOledNotification(context, Protocol.createOled1line(context, null, "Testing"), Protocol.createOled1line(context, null, msg), null, 0, vibratePattern);
	}
	
}
