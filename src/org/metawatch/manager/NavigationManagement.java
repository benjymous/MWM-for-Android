package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.WatchModes;

import android.content.Context;

public class NavigationManagement {
    public static void processWatchConnection(Context context) {
	switch(MetaWatchService.getWatchMode()) {
	case APPLICATION:
	    Application.stopAppMode(context);
	    Idle.getInstance().updateIdle(context, true);
	    break;
	case CALL:
	case NOTIFICATION:
	case IDLE:
	default:
	    Idle.getInstance().toIdle(context);
	    Idle.getInstance().updateIdle(context, true);
	    break;
	}
    }
    
    public static void processEndMode(Context context) {
	MetaWatchService.previousWatchMode();
	WatchModes mode = MetaWatchService.getWatchMode();
	switch(mode) {
	case APPLICATION:
	    if (!Application.toCurrentApp(context)) {
		Application.stopAppMode(context);
	    } else {
		Application.refreshCurrentApp(context);
	    }
	    break;
	case CALL:
	    Call.startRinging(context, Call.phoneNumber);
	    break;
	case NOTIFICATION:
	    processEndMode(context);
	    break;
	case IDLE:
	default:
	    Idle.getInstance().toIdle(context);
	    Idle.getInstance().updateIdle(context, false);
	    break;
	}
    }
}