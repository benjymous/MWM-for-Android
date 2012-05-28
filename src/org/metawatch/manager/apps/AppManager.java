package org.metawatch.manager.apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppManager {

	static Map<String, InternalApp> apps = new HashMap<String, InternalApp>();
	
	public static void initApps() {
		if(apps.size()==0) {
			addApp(new MediaPlayerApp());
			addApp(new ActionsApp());
		}
	}
	
	private static void addApp(InternalApp app) {
		apps.put(app.getId(), app);
	}
	
	public static InternalApp.AppData[] getAppInfos() {
		List<InternalApp.AppData> list = new ArrayList<InternalApp.AppData>();
		for (InternalApp a : apps.values()) {
			list.add(a.getInfo());
		}
		return list.toArray(new InternalApp.AppData[0]);
	}
	
	public static InternalApp getApp(String appId) {
		if(!apps.containsKey(appId)) {
			return null;
		}
		
		return apps.get(appId);
	}
	
	public static int getAppState(String appId) {
		if(!apps.containsKey(appId)) {
			return InternalApp.INACTIVE;
		}
		
		return apps.get(appId).appState;
	}
}
