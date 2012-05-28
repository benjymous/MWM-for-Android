package org.metawatch.manager.apps;

import org.metawatch.manager.Application;
import org.metawatch.manager.Idle;
import org.metawatch.manager.MetaWatch;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.Utils;
import org.metawatch.manager.MetaWatchService.Preferences;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

public abstract class InternalApp {
	
	public final static int BUTTON_NOT_USED = 0;
	public final static int BUTTON_USED = 1;
	public final static int BUTTON_USED_DONT_UPDATE = 2;
	
	public final static int INACTIVE = 0;
	public final static int ACTIVE_IDLE = 1; // Active as an idle screen
	public final static int ACTIVE_STANDALONE = 2; // Active as a standalone app
	
	public int appState = INACTIVE;
	
	public static class AppData {
		public String id;
		public String name;
		
		boolean supportsDigital = false;
		boolean supportsAnalog = false;
		
		boolean toggleable = true; //can toggle itself as an idle page
	}
	
	public abstract AppData getInfo();
	public String getId() {
		return getInfo().id;
	}
	public boolean isToggleable() {
		return getInfo().toggleable;
	}
	
	// An app should do any required construction on the first call of activate or update
	public abstract void activate(int watchType);
	public abstract void deactivate(int watchType);
	
	protected Bitmap getAppSwitchIcon(Context context, boolean preview) {
		String icon = null;
		if (appState == ACTIVE_IDLE || preview) // if preview is true, it's for idle mode
			icon = "switch_app.png";
		else if (appState == ACTIVE_STANDALONE)
			icon = "exit_app.png";
		else
			throw new IllegalStateException("Unknown app state.");
		
		return Utils.loadBitmapFromAssets(context, icon);
	}
	
	public abstract Bitmap update(Context context, boolean preview, int watchType);
	
	// Returns one of BUTTON_NOT_USED, BUTTON_USED or BUTTON_USED_DONT_UPDATE
	public abstract int buttonPressed(Context context, int id);
	
	public void open(Context context) {
		if(appState != INACTIVE) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Ignoring standaloneStart as app is not inactive.");
			return;
		}
		
		int page = Idle.getAppPage(getInfo().id);
		
		// Open the existing Idle app page.
		if (page != -1) {
			Idle.toPage(page);
			Idle.toIdle(context);
		
		// Open stand-alone.
		} else {
			appState = ACTIVE_STANDALONE;
			int watchType = MetaWatchService.watchType;
			if (watchType == MetaWatchService.WatchType.DIGITAL) {
				Application.startAppMode(context, this);
				Application.updateAppMode(context, update(context, false, watchType));
				Application.toApp();
			} else if (watchType == MetaWatchService.WatchType.ANALOG) {
				//FIXME
			}
		}
	}
	
	public void setInactive() {
		appState = INACTIVE;
	}
}
