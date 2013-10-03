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
import org.metawatch.manager.MetaWatchService.WatchModes;
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

public class Idle {

    // final static byte IDLE_DUMMY = 65;
    
    final static byte IDLE_NEXT_PAGE = 60;
    final static byte IDLE_OLED_DISPLAY = 61;
    final static byte RIGHT_QUICK_BUTTON = 62;
    final static byte TOGGLE_SILENT = 63;
    final static byte LEFT_QUICK_BUTTON = 64;

    private static int currentPage = 0;

    private Bitmap oledIdle = null;
    
    private ArrayList<IdlePage> idlePages = null;
    private Map<String, WidgetData> widgetData = null;
    
    private static Idle mInstance = null;
    
    public static Idle getInstance() {
	if (mInstance == null)
	    mInstance = new Idle();
	return mInstance;
    }
    
    public void destroy() {
	mInstance = null;
    }
    
    private Idle(){}

    private interface IdlePage {
	public void activate(Context context, int watchType);

	public void deactivate(Context context, int watchType);

	Bitmap draw(Context context, boolean preview, Bitmap bitmap, int watchType);

	public int screenMode(int watchType);

	public int buttonPressed(Context context, int id);
    }

    private class WidgetPage implements IdlePage {

	private List<WidgetRow> rows;
	private int pageIndex;

	public WidgetPage(List<WidgetRow> r, int p) {
	    rows = r;
	    pageIndex = p;
	}

	public void activate(final Context context, int watchType) {
	    // if (Preferences.quickButton != QuickButton.DISABLED) {
	    if (watchType == MetaWatchService.WatchType.DIGITAL) {
		Protocol.getInstance(context).disableButton(1, 0, MetaWatchService.WatchBuffers.IDLE); // Disable
		// built
		// in
		// action
		// for
		// Right
		// middle
		// immediate
		Protocol.getInstance(context).enableButton(1, 1, Idle.RIGHT_QUICK_BUTTON, screenMode(watchType)); // Right
		// middle
		// -
		// press
	    }
	    // }
	}

	public void deactivate(final Context context, int watchType) {
	    // if (Preferences.quickButton != QuickButton.DISABLED) {
	    if (watchType == MetaWatchService.WatchType.DIGITAL) {
		Protocol.getInstance(context).disableButton(1, 1, screenMode(watchType)); // Right
		// middle
		// -
		// press
	    }
	    // }
	}

	public Bitmap draw(final Context context, boolean preview, Bitmap bitmap, int watchType) {
	    Canvas canvas = new Canvas(bitmap);
	    canvas.drawColor(Color.WHITE);

	    boolean showClock = (pageIndex == 0 || Preferences.clockOnEveryPage);

	    if (watchType == WatchType.DIGITAL && preview && showClock) {
		canvas.drawBitmap(Utils.getBitmap(context, "dummy_clock.png"), 0, 0, null);
	    }

	    if (MetaWatchService.silentMode()) {
		if (MetaWatchService.watchType == WatchType.DIGITAL) {

		    Paint paint = new Paint();
		    paint.setColor(Color.BLACK);
		    paint.setTextSize(FontCache.instance(context).Large.size);
		    paint.setTypeface(FontCache.instance(context).Large.face);
		    paint.setTextAlign(Align.CENTER);

		    canvas.drawText("Silent Mode", 48, 64, paint);

		}
	    } else {

		int totalHeight = 0;
		for (WidgetRow row : rows) {
		    totalHeight += row.getHeight();
		}

		float padding = 0;
		float yPos = 0;
		if (watchType == WatchType.DIGITAL && Preferences.alignWidgetRowToBottom) {
		    padding = 0;
		    yPos = (96 - totalHeight);
		} else {
		    padding = (watchType == WatchType.DIGITAL) ? (float) (((showClock ? 64 : 96) - totalHeight) / (float) (2 * rows.size())) : 0;
		    yPos = (watchType == WatchType.DIGITAL) ? (showClock ? 30 : 0) + padding : 0;
		}
		final float space = padding;

		float widgetRowYPos = yPos;
		for (WidgetRow row : rows) {
		    row.draw(widgetData, canvas, (int) widgetRowYPos);
		    widgetRowYPos += row.getHeight() + (space * 2);
		}

		float separatorYPos = yPos;
		if (watchType == WatchType.DIGITAL && Preferences.displayWidgetRowSeparator) {
		    separatorYPos -= space / 2; // Center the separators between
						// rows.
		    if (showClock) {
			drawLine(canvas, (int) separatorYPos);
		    }
		    int i = 0;
		    for (WidgetRow row : rows) {
			if (++i == rows.size())
			    continue;
			separatorYPos += row.getHeight() + (space * 2);
			drawLine(canvas, (int) separatorYPos);
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
		boolean showsClock = (pageIndex == 0 || Preferences.clockOnEveryPage);
		if (watchType == MetaWatchService.WatchType.DIGITAL && !showsClock)
		    return MetaWatchService.WatchBuffers.APPLICATION;
	    }

	    return MetaWatchService.WatchBuffers.IDLE;
	}

	public int buttonPressed(Context context, int id) {
	    return ApplicationBase.BUTTON_NOT_USED;
	}
    }

    private class AppPage implements IdlePage {

	private ApplicationBase app;

	public AppPage(ApplicationBase arg) {
	    app = arg;
	}

	public void activate(final Context context, int watchType) {
	    app.appState = ApplicationBase.ACTIVE_IDLE;
	    app.activate(context, watchType);
	}

	public void deactivate(final Context context, int watchType) {
	    app.setInactive();
	    app.deactivate(context, watchType);
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

    public void nextPage(final Context context) {
	toPage(context, currentPage + 1);
    }

    public void toPage(final Context context, int page) {

	if (idlePages != null && idlePages.size() > currentPage) {
	    idlePages.get(currentPage).deactivate(context, MetaWatchService.watchType);
	}

	currentPage = page % numPages();

	if (idlePages != null && idlePages.size() > currentPage) {
	    idlePages.get(currentPage).activate(context, MetaWatchService.watchType);
	}
    }

    public int numPages() {
	return (idlePages == null || idlePages.size() == 0) ? 1 : idlePages.size();
    }

    public int getAppPage(String appId) {
	if (idlePages != null) {
	    for (int page = 0; page < idlePages.size(); page++) {
		if (idlePages.get(page) instanceof AppPage && ((AppPage) idlePages.get(page)).app.getId().equals(appId)) {
		    return page;
		}
	    }
	}

	// Not found.
	return -1;
    }

    public ApplicationBase getCurrentApp() {
	if (idlePages != null && idlePages.get(currentPage) instanceof AppPage) {
	    return ((AppPage) idlePages.get(currentPage)).app;
	}

	// Not an app page.
	return null;
    }

    public int addAppPage(final Context context, ApplicationBase app) {
	int page = getAppPage(app.getId());

	if (page == -1) {
	    AppPage aPage = new AppPage(app);

	    if (idlePages == null)
		idlePages = new ArrayList<IdlePage>();
	    idlePages.add(aPage);
	    page = idlePages.indexOf(aPage);

	    app.setPageSetting(context, true);
	}

	return page;
    }

    public void removeAppPage(final Context context, ApplicationBase app) {
	int page = getAppPage(app.getId());

	if (page != -1) {
	    if (page == currentPage) {
		toPage(context, 0);
	    }

	    idlePages.remove(page);

	    app.setPageSetting(context, false);
	}
    }

    public void reset(Context context) {
	toPage(context, 0);
	if (idlePages != null)
	    idlePages.clear();
	idlePages = null;
    }

    void updateIdlePages(Context context) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Idle.updateIdlePages start");

	try {

	    List<WidgetRow> rows = WidgetManager.getInstance(context).getDesiredWidgetsFromPrefs(context);

	    ArrayList<CharSequence> widgetsDesired = new ArrayList<CharSequence>();
	    for (WidgetRow row : rows) {
		widgetsDesired.addAll(row.getIds());
	    }

	    widgetData = WidgetManager.getInstance(context).refreshWidgets(context, widgetsDesired);

	    for (WidgetRow row : rows) {
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
		screenSize = 32; // Initial screen has top part used by the fw
				 // clock
	    }

	    ArrayList<WidgetRow> screenRow = new ArrayList<WidgetRow>();
	    for (WidgetRow row : rows) {
		if (screenSize + row.getHeight() > maxScreenSize) {
		    screens.add(new WidgetPage(screenRow, screens.size()));
		    screenRow = new ArrayList<WidgetRow>();
		    if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL && Preferences.clockOnEveryPage) {
			screenSize = 32;
		    } else {
			screenSize = 0;
		    }
		}
		screenRow.add(row);
		screenSize += row.getHeight();
	    }
	    screens.add(new WidgetPage(screenRow, screens.size()));

	    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	    
	    AppData[] data = AppManager.getInstance(context).getAppInfos();
	    for (AppData appEntry : data) {
		if (sharedPreferences.getBoolean(appEntry.getPageSettingName(), false)) {
		    screens.add(new AppPage(AppManager.getInstance(context).getApp(appEntry.id)));
		}
	    }

	    idlePages = screens;

	} finally {
	}

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Idle.updateIdlePages end");
    }

    Bitmap createIdle(Context context) {
	return createIdle(context, false, currentPage);
    }

    /*
     * Only this (central) method need to be synchronized, the one above calls this and will be blocked anyway.
     */
    synchronized Bitmap createIdle(Context context, boolean preview, int page) {
	final int width = (MetaWatchService.watchType == WatchType.DIGITAL) ? 96 : 80;
	final int height = (MetaWatchService.watchType == WatchType.DIGITAL) ? 96 : 32;
	Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

	if (idlePages != null && idlePages.size() > page) {
	    return idlePages.get(page).draw(context, preview, bitmap, MetaWatchService.watchType);
	}

	return bitmap;
    }

    public Canvas drawLine(Canvas canvas, int y) {
	Paint paint = new Paint();
	paint.setColor(Color.BLACK);

	int left = 2;

	for (int i = 0 + left; i < 96 - left; i += 4)
	    canvas.drawLine(i, y, i + 2, y, paint);

	return canvas;
    }

    private int getScreenMode(int watchType) {
	int mode = MetaWatchService.WatchBuffers.IDLE;
	if (idlePages != null && idlePages.size() > currentPage) {
	    mode = idlePages.get(currentPage).screenMode(watchType);
	}
	return mode;
    }

    private void sendLcdIdle(final Context context, final boolean refresh) {
	if (MetaWatchService.getWatchMode() != MetaWatchService.WatchModes.IDLE) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Ignoring sendLcdIdle as not in idle");
	    return;
	}

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "sendLcdIdle start");

	final int mode = getScreenMode(MetaWatchService.WatchType.DIGITAL);
	boolean showClock = false;

	if (mode == MetaWatchService.WatchBuffers.IDLE || idlePages.get(currentPage) instanceof WidgetPage) {
	    if (MetaWatchService.silentMode()) {
		showClock = true;
	    } else {
		if (refresh)
		    updateIdlePages(context);
		showClock = (currentPage == 0 || Preferences.clockOnEveryPage);
	    }
	}

	//Send the Idle screen
	Protocol.getInstance(context).sendLcdBitmap(createIdle(context), mode);

	//Update the buffers for the sent Idle screen
	Protocol.getInstance(context).configureIdleBufferSize(showClock);

	//Draw the sent Idle screen on the buffer
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "sendLcdIdle: Drawing idle screen on buffer " + mode);
	Protocol.getInstance(context).updateLcdDisplay(mode);

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "sendLcdIdle end");	
    }

    public void toIdle(Context context) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Idle.toIdle()");

	if (Notification.getInstance().isActive())
	    return;

	if (idlePages == null)
	    updateIdlePages(context);
	
	MetaWatchService.clearWatchMode();
	MetaWatchService.setWatchMode(WatchModes.IDLE);
	
	idlePages.get(currentPage).activate(context, MetaWatchService.watchType);

	if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL) {
	    if (numPages() > 1) {
		Protocol.getInstance(context).enableButton(0, 1, IDLE_NEXT_PAGE, MetaWatchService.WatchBuffers.IDLE); // Right
		// top
		// press
		Protocol.getInstance(context).enableButton(0, 1, IDLE_NEXT_PAGE, MetaWatchService.WatchBuffers.APPLICATION); // Right
		// top
		// press
		Protocol.getInstance(context).enableButton(5, 0, LEFT_QUICK_BUTTON, MetaWatchService.WatchBuffers.IDLE); // left
		// middle
		// press
	    }
	    Protocol.getInstance(context).enableButton(0, 2, TOGGLE_SILENT, MetaWatchService.WatchBuffers.IDLE);
	    Protocol.getInstance(context).enableButton(0, 3, TOGGLE_SILENT, MetaWatchService.WatchBuffers.IDLE);
	} else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
	    Protocol.getInstance(context).disableButton(1, 0, MetaWatchService.WatchBuffers.IDLE); // Disable
	    // built
	    // in
	    // action
	    // for
	    // Middle
	    // immediate
	    Protocol.getInstance(context).enableButton(1, 1, IDLE_OLED_DISPLAY, MetaWatchService.WatchBuffers.IDLE); // Middle
	    // press
	    Protocol.getInstance(context).enableButton(1, 1, IDLE_OLED_DISPLAY, MetaWatchService.WatchBuffers.APPLICATION); // Middle
	    // press
	}

	Protocol.getInstance(context).enableButton(1, 2, TOGGLE_SILENT, MetaWatchService.WatchBuffers.IDLE);
    }

    public void updateIdle(final Context context, final boolean refresh) {
	if (!MetaWatchService.mIsRunning || MetaWatchService.watchType == MetaWatchService.WatchType.UNKNOWN || MetaWatchService.getWatchMode() != MetaWatchService.WatchModes.IDLE) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Idle.updateIdle() skipped - yet unknown watch type");
	    return;
	}

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Idle.updateIdle()");
	long timestamp = System.currentTimeMillis();
	
	if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL)
	    sendLcdIdle(context, refresh);
	else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG)
	    updateOledIdle(context, refresh);
	
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "updateIdle took " + (System.currentTimeMillis() - timestamp) + " ms");
    }

    private void updateOledIdle(final Context context, final boolean refresh) {
	final int mode = getScreenMode(MetaWatchService.WatchType.ANALOG);

	if (mode == MetaWatchService.WatchBuffers.IDLE)
	    updateIdlePages(context);

	// get the 32px full screen
	oledIdle = createIdle(context);
    }

    // Send oled widgets view on demand
    public void sendOledIdle(final Context context) {
	if (oledIdle == null) {
	    updateOledIdle(context, true);
	}

	final int mode = getScreenMode(MetaWatchService.WatchType.ANALOG);

	// Split into top/bottom, and send
	for (int i = 0; i < 2; ++i) {
	    Bitmap bitmap = Bitmap.createBitmap(80, 16, Bitmap.Config.RGB_565);
	    Canvas canvas = new Canvas(bitmap);
	    canvas.drawBitmap(oledIdle, 0, -(i * 16), null);
	    Protocol.getInstance(context).sendOledBitmap(bitmap, mode, i);
	}
	Protocol.getInstance(context).oledChangeMode(mode);		
    }

    public int appButtonPressed(Context context, int id) {
	if (idlePages != null && idlePages.size() > currentPage) {
	    return idlePages.get(currentPage).buttonPressed(context, id);
	}
	return ApplicationBase.BUTTON_NOT_USED;
    }

    public void quickButtonAction(Context context, String actionId) {

	if (actionId.startsWith(AppManagerAction.appManagerPrefix)) {
	    String appId = actionId.replace(AppManagerAction.appManagerPrefix, "");
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Idle.quickButtonAction() app: " + appId);
	    AppManager.getInstance(context).getApp(appId).open(context, true);
	} else {
	    Action action = ActionManager.getInstance(context).getAction(actionId);
	    if (action != null) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Idle.quickButtonAction() " + action.getName());

		if (action instanceof ContainerAction) {
		    ActionManager.getInstance(context).displayAction(context, (ContainerAction) action);
		} else {
		    action.performAction(context);
		}
	    } else {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "Idle.quickButtonAction() couldn't find action " + actionId);
	    }
	}
    }

    public void activateButtons(final Context context) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Idle.activateButtons()");
	if (idlePages != null && idlePages.size() > currentPage) {
	    idlePages.get(currentPage).activate(context, MetaWatchService.watchType);
	}
    }

    public void deactivateButtons(final Context context) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Idle.deactivateButtons()");
	if (idlePages != null && idlePages.size() > currentPage) {
	    idlePages.get(currentPage).deactivate(context, MetaWatchService.watchType);
	}
    }

}
