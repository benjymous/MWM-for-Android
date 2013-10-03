package org.metawatch.manager.apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchStatus;

import android.content.Context;
import android.content.Intent;
import org.metawatch.manager.Log;

public class AppManager {

    Map<String, ApplicationBase> apps = new HashMap<String, ApplicationBase>();

    private static AppManager mInstance;
    
    private AppManager(Context context) {
	initApps(context);
    }
    
    public static AppManager getInstance(Context context) {
	if (mInstance == null)
	    mInstance = new AppManager(context);
	return mInstance;
    }
    
    public void destroy() {
	mInstance = null;
    }
    
    private void initApps(Context context) {
	sendDiscoveryBroadcast(context);
	if (getApp(MediaPlayerApp.APP_ID) == null)
	    addApp(new MediaPlayerApp());
	if (getApp(ActionsApp.APP_ID) == null)
	    addApp(new ActionsApp());
	if (getApp(CalendarApp.APP_ID) == null)
	    addApp(new CalendarApp());
    }

    public void addApp(ApplicationBase app) {
	apps.put(app.getId(), app);
    }

    public void removeApp(ApplicationBase app) {
	if (apps.containsKey(app.getId())) {
	    apps.remove(app.getId());
	}
    }

    public ApplicationBase.AppData[] getAppInfos() {
	List<ApplicationBase.AppData> list = new ArrayList<ApplicationBase.AppData>();
	for (ApplicationBase a : apps.values()) {
	    list.add(a.getInfo());
	}
	return list.toArray(new ApplicationBase.AppData[0]);
    }

    public ApplicationBase getApp(String appId) {
	if (!apps.containsKey(appId)) {
	    return null;
	}

	return apps.get(appId);
    }

    public int getAppState(String appId) {
	if (!apps.containsKey(appId)) {
	    return ApplicationBase.INACTIVE;
	}

	return apps.get(appId).appState;
    }

    public void sendDiscoveryBroadcast(Context context) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Broadcasting APPLICATION_DISCOVERY");
	Intent intent = new Intent("org.metawatch.manager.APPLICATION_DISCOVERY");
	context.sendBroadcast(intent);
    }
}
