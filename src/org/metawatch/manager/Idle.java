                                                                     
                                                                     
                                                                     
                                             
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
import org.metawatch.manager.actions.Action;
import org.metawatch.manager.actions.ActionManager;
import org.metawatch.manager.actions.AppManagerAction;
import org.metawatch.manager.actions.ContainerAction;
import org.metawatch.manager.apps.AppManager;
import org.metawatch.manager.apps.ApplicationBase;
import org.metawatch.manager.apps.ApplicationBase.AppData;
import org.metawatch.manager.widgets.InternalWidget.WidgetData;
import org.metawatch.manager.widgets.WidgetManager;
import org.metawatch.manager.widgets.WidgetRow;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.preference.PreferenceManager;
import android.util.Log;

public class Idle {
	
	//final static byte IDLE_DUMMY = 65;
	
	final static byte IDLE_NEXT_PAGE = 60;
	final static byte IDLE_OLED_DISPLAY = 61;
	final static byte RIGHT_QUICK_BUTTON = 62;
	final static byte TOGGLE_SILENT = 63;
	final static byte LEFT_QUICK_BUTTON = 64;
	
	private static boolean busy = false;
	private static Object busyObj = new Object();
	
	public static boolean isBusy() {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.isBusy()");
		synchronized (busyObj) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.busy="+busy);
			return busy;
		}
	}
	
	private static void setBusy(boolean isBusy) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.setBusy()");
		synchronized (busyObj) {
			busy = isBusy;
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.setBusy("+isBusy+")");
		}
	}
	
	private interface IdlePage {
		public void activate(Context context, int watchType);
		public void deactivate(Context context, int watchType);
		Bitmap draw(Context context, boolean preview, Bitmap bitmap, int watchType);
		public int screenMode(int watchType);
		public int buttonPressed(Context context, int id);
	}
	
	private static class WidgetPage implements IdlePage {

		private List<WidgetRow> rows;
		private int pageIndex;
		
		public WidgetPage(List<WidgetRow> r, int p) {
			rows = r;
			pageIndex = p;
		}
		
		public void activate(final Context context, int watchType) {
			//if (Preferences.quickButton != QuickButton.DISABLED) {
				if (watchType == MetaWatchService.WatchType.DIGITAL) {
					Protocol.disableButton(1, 0, MetaWatchService.WatchBuffers.IDLE); // Disable built in action for Right middle immediate
					Protocol.enableButton(1, 1, Idle.RIGHT_QUICK_BUTTON, screenMode(watchType)); // Right middle - press
				}
			//}
		}

		public void deactivate(final Context context, int watchType) {
			//if (Preferences.quickButton != QuickButton.DISABLED) {
				if (watchType == MetaWatchService.WatchType.DIGITAL) {
					Protocol.disableButton(1, 1, screenMode(watchType)); // Right middle - press
				}
			//}
		}
		
		public Bitmap draw(final Context context, boolean preview, Bitmap bitmap, int watchType) {
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(Color.WHITE);	
			
			boolean showClock = (pageIndex==0 || Preferences.clockOnEveryPage);
			
			if(watchType == WatchType.DIGITAL && preview && showClock) {
				canvas.drawBitmap(Utils.getBitmap(context, "dummy_clock.png"), 0, 0, null);
			} 
			
			if (MetaWatchService.SilentMode()) {
				if (MetaWatchService.watchType==WatchType.DIGITAL) {
					
					Paint paint = new Paint();
					paint.setColor(Color.BLACK);		
					paint.setTextSize(FontCache.instance(context).Large.size);
					paint.setTypeface(FontCache.instance(context).Large.face);
					paint.setTextAlign(Align.CENTER);
					
					canvas.drawText("Silent Mode", 48, 64, paint);
					
				}
			} else {
			
				int totalHeight = 0;
				for(WidgetRow row : rows) {
					totalHeight += row.getHeight();
				}
							
				float padding = 0;
				float yPos = 0;
				if (watchType == WatchType.DIGITAL && Preferences.alignWidgetRowToBottom) {
					padding = 0;
					yPos = (96 - totalHeight);
				} else {
					padding = (watchType == WatchType.DIGITAL) ? (float)(((showClock ? 64:96) - totalHeight) / (float)(2*rows.size())) : 0;
					yPos = (watchType == WatchType.DIGITAL) ? (showClock ? 30:0) + padding : 0;
				}
				final float space = padding;

				float widgetRowYPos = yPos;
				for(WidgetRow row : rows) {
					row.draw(widgetData, canvas, (int)widgetRowYPos);
					widgetRowYPos += row.getHeight() + (space*2);
				}
	
				float separatorYPos = yPos;
				if (watchType == WatchType.DIGITAL && Preferences.displayWidgetRowSeparator) {
					separatorYPos -= space/2; // Center the separators between rows.
					if (showClock) {
						drawLine(canvas, (int)separatorYPos);
					}
					int i = 0;
					for(WidgetRow row : rows) {
						if (++i == rows.size())
							continue;
						separatorYPos += row.getHeight() + (space*2);
						drawLine(canvas, (int)separatorYPos);
					}
				}
			}
			
			return bitmap;
		}
		
		public int screenMode(int watchType) {
			// Always use app buffer for clockless pages on gen2 watches
			// this works around a bug in the fw that stops clockless idle
			// screens displaying properly
			if (Preferences.appBufferForClocklessPages || MetaWatchService.watchGen == MetaWatchService.WatchGen.GEN2) {
				boolean showsClock = (pageIndex==0 || Preferences.clockOnEveryPage);
				if (watchType == MetaWatchService.WatchType.DIGITAL && !showsClock)
					return MetaWatchService.WatchBuffers.APPLICATION;
			}
			
			return MetaWatchService.WatchBuffers.IDLE;
		}

		public int buttonPressed(Context context, int id) {
			return ApplicationBase.BUTTON_NOT_USED;
		}
	}
	
	private static class AppPage implements IdlePage {

		private ApplicationBase app;
		
		public AppPage(ApplicationBase arg) {
			app = arg;
		}
		
		public void activate(final Context context, int watchType) {
			app.appState = ApplicationBase.ACTIVE_IDLE;
			app.activate(context, watchType);
			if (app.isToggleable())
				Application.enableToggleButton(watchType);
		}

		public void deactivate(final Context context, int watchType) {
			app.setInactive();
			app.deactivate(context, watchType);
			Application.disableToggleButton(watchType);
		}
		
		public Bitmap draw(final Context context, boolean preview, Bitmap bitmap, int watchType) {
			return app != null ? app.update(context, preview, watchType) : null;
		}	
		
		public int screenMode(int watchType) {
			return MetaWatchService.WatchBuffers.APPLICATION;
		}

		public int buttonPressed(Context context, int id) {
			return app.buttonPressed(context, id);
		}
	}

	static int currentPage = 0;
	static boolean initialised = false;
	
	static Bitmap oledIdle = null;
	
	public static void nextPage(final Context context) {
		toPage(context, currentPage+1);
	}
	
	public static void toPage(final Context context, int page) {
			
		if(idlePages != null && idlePages.size()>currentPage) {
			idlePages.get(currentPage).deactivate(context, MetaWatchService.watchType);
		}
				
		currentPage = (page) % numPages();
		
		if(idlePages != null && idlePages.size()>currentPage) {
			idlePages.get(currentPage).activate(context, MetaWatchService.watchType);
		}
	}
	
	public static int numPages() {	
		return (idlePages==null || idlePages.size()==0) ? 1 : idlePages.size();
	}
	
	public static int getAppPage(String appId) {
		if (idlePages != null) {
			for (int page = 0; page < idlePages.size(); page++) {
				if (idlePages.get(page) instanceof AppPage &&
						((AppPage)idlePages.get(page)).app.getId().equals(appId)) {
					return page;
				}
			}
		}
		
		//Not found.
		return -1;
	}
	
	public static ApplicationBase getCurrentApp() {
		if (idlePages != null && idlePages.get(currentPage) instanceof AppPage) {
			return ((AppPage)idlePages.get(currentPage)).app;
		}
		
		// Not an app page.
		return null;
	}
	
	public static synchronized int addAppPage(final Context context, ApplicationBase app) {
		int page = getAppPage(app.getId());
		
		if (page == -1) {
			AppPage aPage = new AppPage(app);
			
			if (idlePages==null)
				idlePages = new ArrayList<IdlePage>();
			idlePages.add(aPage);
			page = idlePages.indexOf(aPage);
			
			app.setPageSetting(context, true);
		}
		
		return page;
	}
	
	public static synchronized void removeAppPage(final Context context, ApplicationBase app) {
		int page = getAppPage(app.getId());

		if (page != -1) {
			if (page == currentPage) {
				toPage(context,0);
			}
			
			idlePages.remove(page);
			
			app.setPageSetting(context, false);
		}
	}
	
	private static ArrayList<IdlePage> idlePages = null;
	private static Map<String,WidgetData> widgetData = null;
	
	public static void reset(Context context) {
		toPage(context, 0);
		if (idlePages != null)
			idlePages.clear();
		idlePages = null;
	}
	
	public static void updateIdlePages(Context context, boolean refresh) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.updateIdlePages start");
		try {
			setBusy(true);
			
			if(!initialised) {
				AppManager.initApps(context);
				WidgetManager.initWidgets(context, null);
				ActionManager.initActions(context);
				initialised = true;
			}
			
			ArrayList<IdlePage> prevList = idlePages;
			
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
			if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL) {
				screenSize = 32; // Initial screen has top part used by the fw clock
			}
			
			ArrayList<WidgetRow> screenRow = new ArrayList<WidgetRow>();
			for(WidgetRow row : rows) { 
				if(screenSize+row.getHeight() > maxScreenSize) {
					screens.add(new WidgetPage(screenRow, screens.size()));
					screenRow = new ArrayList<WidgetRow>();
					if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL &&
							Preferences.clockOnEveryPage) {
						screenSize = 32;
					} else { 
						screenSize = 0;
					}
				}
				if (row.getHeight()!=0)
					screenRow.add(row);
				screenSize += row.getHeight();
			}
			screens.add(new WidgetPage(screenRow, screens.size()));
			
			if (prevList == null) {			
				SharedPreferences sharedPreferences = PreferenceManager
						.getDefaultSharedPreferences(context);
				
				AppData[] data = AppManager.getAppInfos();	
				for (AppData appEntry : data) {
					if(sharedPreferences.getBoolean(appEntry.getPageSettingName(), false)) {
						screens.add(new AppPage(AppManager.getApp(appEntry.id)));
					}
				}
								
			} else {
				// Copy app pages from previous list.
				for (IdlePage page : prevList) {
					if (page instanceof AppPage) {
						screens.add(page);
					}
				}
			}
			
			idlePages = screens;
			
			if (prevList == null) {
				//First run of this function, activate buttons for initial screen.
				toPage(context, currentPage);
			}
			
		}
		finally {
			setBusy(false);
		}
		
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.updateIdlePages end");
	}

	static Bitmap createIdle(Context context) {
		return createIdle(context, false, currentPage);
	}
	
	/* Only this (central) method need to be synchronized, the one above calls
	 * this and will be blocked anyway. */
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

	  int left = 2;

	  for (int i = 0+left; i < 96-left; i += 4)
	    canvas.drawLine(i, y, i+2, y, paint);
	
	  return canvas;
	}
	
	public static int getScreenMode(int watchType) {
		int mode = MetaWatchService.WatchBuffers.IDLE;
		if(idlePages != null && idlePages.size()>currentPage) {
			mode = idlePages.get(currentPage).screenMode(watchType);
		}
		return mode;
	}
	
	private static void sendLcdIdle(Context context, boolean refresh) {
		if(MetaWatchService.watchState != MetaWatchService.WatchStates.IDLE) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Ignoring sendLcdIdle as not in idle");
			return;
		}
		
		if (isBusy()) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Ignoring sendLcdIdle as Idle is busy");
			return;
		}
		
		if (Preferences.logging) Log.d(MetaWatch.TAG, "sendLcdIdle start");
		
		final int mode = getScreenMode(MetaWatchService.WatchType.DIGITAL);
		boolean showClock = false;
		
		if (mode == MetaWatchService.WatchBuffers.IDLE ||
				idlePages.get(currentPage) instanceof WidgetPage) {
			if (MetaWatchService.SilentMode()) {
				showClock = true;
			}
			else {
				// Update widgets.
				// Don't do it while on an AppPage in order to not overwrite any running app.
				updateIdlePages(context, refresh);
				showClock = (currentPage==0 || Preferences.clockOnEveryPage);
			}
		}
		
		Protocol.sendLcdBitmap(createIdle(context), mode);
		if (mode == MetaWatchService.WatchBuffers.IDLE)
			Protocol.configureIdleBufferSize(showClock, true);
		
		if (Preferences.logging) Log.d(MetaWatch.TAG, "sendLcdIdle: Drawing idle screen on buffer "+mode);
		Protocol.updateLcdDisplay(mode);
		
		if (Preferences.logging) Log.d(MetaWatch.TAG, "sendLcdIdle end");
	}
	
	public static void toIdle(Context context) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.toIdle()");
		
		if (Notification.isActive())
			return;
		
		MetaWatchService.WatchModes.IDLE = true;
		MetaWatchService.watchState = MetaWatchService.WatchStates.IDLE;
		
		if (idlePages != null) {
			if (currentPage>=idlePages.size()) {
				currentPage=0;
			}
			idlePages.get(currentPage).activate(context, MetaWatchService.watchType);
		}
		
		if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL) {
			sendLcdIdle(context, true);
							
			if (numPages()>1) {
				Protocol.enableButton(0, 1, IDLE_NEXT_PAGE, MetaWatchService.WatchBuffers.IDLE); // Right top press
				Protocol.enableButton(0, 1, IDLE_NEXT_PAGE, MetaWatchService.WatchBuffers.APPLICATION); // Right top press
				Protocol.enableButton(5, 0, LEFT_QUICK_BUTTON, MetaWatchService.WatchBuffers.IDLE); // left middle - press
			
			}
			
			Protocol.enableButton(0, 2, TOGGLE_SILENT, MetaWatchService.WatchBuffers.IDLE);
			Protocol.enableButton(0, 3, TOGGLE_SILENT, MetaWatchService.WatchBuffers.IDLE);
		
		}
		else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
			Protocol.disableButton(1, 0, MetaWatchService.WatchBuffers.IDLE); // Disable built in action for Middle immediate
			Protocol.enableButton(1, 1, IDLE_OLED_DISPLAY, MetaWatchService.WatchBuffers.IDLE); // Middle press
			Protocol.enableButton(1, 1, IDLE_OLED_DISPLAY, MetaWatchService.WatchBuffers.APPLICATION); // Middle press
		}
		
		Protocol.enableButton(1, 2, TOGGLE_SILENT, MetaWatchService.WatchBuffers.IDLE);
		Protocol.enableButton(1, 3, TOGGLE_SILENT, MetaWatchService.WatchBuffers.IDLE);
	}
	
	public static void updateIdle(final Context context, final boolean refresh) {
		
		if (MetaWatchService.watchType == MetaWatchService.WatchType.UNKNOWN) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.updateIdle() skipped - yet unknown watch type");
			return;
		}
		
		if (MetaWatchService.watchState == MetaWatchService.WatchStates.IDLE ) {
			Thread thread = new Thread("UpdateIdle") {
	
				@Override
				public void run() {
					if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.updateIdle()");
					long timestamp = System.currentTimeMillis();
					
					if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL)
						sendLcdIdle(context, refresh);
					else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG)
						updateOledIdle(context, refresh);
					
					if (Preferences.logging) Log.d(MetaWatch.TAG, "updateIdle took " + (System.currentTimeMillis()-timestamp) + " ms" );
				}
			};
			thread.start();
		}
		
	}
		
	
	private static void updateOledIdle(Context context, boolean refresh) {	
		if (isBusy())
			return;
		
		final int mode = getScreenMode(MetaWatchService.WatchType.ANALOG);
		
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
	 		
		final int mode = getScreenMode(MetaWatchService.WatchType.ANALOG);
			
		// Split into top/bottom, and send
		for(int i=0; i<2; ++i) {
			Bitmap bitmap = Bitmap.createBitmap(80, 16, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawBitmap(oledIdle, 0, -(i*16), null);
			Protocol.sendOledBitmap(bitmap, mode, i);
		}
		Protocol.oledChangeMode(mode);					
	}
	
	public static int appButtonPressed(Context context, int id) {
		if(idlePages != null && idlePages.size()>currentPage) {
			return idlePages.get(currentPage).buttonPressed(context, id);
		}
		return ApplicationBase.BUTTON_NOT_USED;
	}
	
	public static void quickButtonAction(Context context, String actionId) {
		
		if(actionId.startsWith(AppManagerAction.appManagerPrefix)) {
			String appId = actionId.replace(AppManagerAction.appManagerPrefix, "");
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.quickButtonAction() app: "+appId);
			AppManager.getApp(appId).open(context, true);
		}
		else {
			Action action = ActionManager.getAction(actionId);
			if(action!=null) {
				if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.quickButtonAction() "+action.getName());
				
				if(action instanceof ContainerAction) {
					ActionManager.displayAction(context, (ContainerAction)action);
				}
				else {
					action.performAction(context);
				}
			}
			else {
				if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.quickButtonAction() couldn't find action "+actionId);
			}
		}
	}

	public static void activateButtons(final Context context) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.activateButtons()");
		if(idlePages != null && idlePages.size()>currentPage) {
			idlePages.get(currentPage).activate(context, MetaWatchService.watchType);
		}
	}

	public static void deactivateButtons(final Context context) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Idle.deactivateButtons()");
		if(idlePages != null && idlePages.size()>currentPage) {
			idlePages.get(currentPage).deactivate(context, MetaWatchService.watchType);
		}
	}
	
}
