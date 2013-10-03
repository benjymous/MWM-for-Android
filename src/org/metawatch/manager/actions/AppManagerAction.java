package org.metawatch.manager.actions;

import org.metawatch.manager.Idle;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.apps.AppManager;
import org.metawatch.manager.apps.ApplicationBase;
import org.metawatch.manager.apps.ApplicationBase.AppData;

import android.content.Context;

public class AppManagerAction extends ContainerAction {

    public static String appManagerPrefix = ":launch:";

    public String id = "apps";

    public String getId() {
	return id;
    }

    public String getName() {
	return "Apps";
    }

    public void refreshSubActions(Context context) {
	subActions.clear();

	for (final AppData a : AppManager.getInstance(context).getAppInfos()) {

	    int watchType = MetaWatchService.watchType;
	    if ((watchType == MetaWatchService.WatchType.ANALOG && !a.supportsAnalog) || (watchType == MetaWatchService.WatchType.DIGITAL && !a.supportsDigital))
		continue; // Skip unsupported apps.

	    subActions.add(new Action() {

		public String getId() {
		    return appManagerPrefix + a.id;
		}

		public String getName() {
		    return a.name;
		}

		private boolean isRunning() {
		    return Idle.getInstance().getAppPage(a.id) != -1;
		}

		public String bulletIcon() {
		    return isRunning() ? "bullet_square_open.bmp" : "bullet_square.bmp";
		}

		public int performAction(Context context) {
		    AppManager.getInstance(context).getApp(a.id).open(context, false);
		    return ApplicationBase.BUTTON_USED_DONT_UPDATE;
		}

		public int getSecondaryType() {
		    return isRunning() ? Action.SECONDARY_EXIT : Action.SECONDARY_NONE;
		}

		public int performSecondary(Context context) {
		    if (isRunning()) {
			Idle.getInstance().removeAppPage(context, AppManager.getInstance(context).getApp(a.id));
			return ApplicationBase.BUTTON_USED;
		    }

		    return ApplicationBase.BUTTON_NOT_USED;
		}
	    });
	}
    }

}
