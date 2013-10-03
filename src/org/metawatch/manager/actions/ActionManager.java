package org.metawatch.manager.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.metawatch.manager.Call;
import org.metawatch.manager.MediaControl;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WeatherProvider;
import org.metawatch.manager.Notification;
import org.metawatch.manager.actions.InternalActions.PhoneCallAction;
import org.metawatch.manager.actions.InternalActions.PhoneSettingsAction;
import org.metawatch.manager.apps.ActionsApp;
import org.metawatch.manager.apps.AppManager;
import org.metawatch.manager.apps.ApplicationBase;

import android.content.Context;
import android.content.pm.PackageManager;

public class ActionManager {

    Map<String, Action> actions = new HashMap<String, Action>();

    NotificationsAction notificationsAction = null;
    PhoneSettingsAction phoneSettingsAction = null;
    PhoneCallAction phoneCallAction = null;
    AppManagerAction appManagerAction = null;
    QuickDialAction quickDialAction = null;
    Action lastNotificationAction = null;
    
    private static ActionManager mInstance;
    
    private ActionManager(Context context) {
	initActions(context);
    }
    
    public static ActionManager getInstance(Context context) {
	if (mInstance == null)
	    mInstance = new ActionManager(context);
	return mInstance;
    }
    
    public void destroy() {
	mInstance = null;
    }

    private void initActions(final Context context) {
	if (actions.size() == 0) {

	    notificationsAction = new NotificationsAction();
	    addAction(notificationsAction);

	    appManagerAction = new AppManagerAction();
	    addAction(appManagerAction);

	    phoneSettingsAction = new PhoneSettingsAction();
	    addAction(phoneSettingsAction);

	    PackageManager pm = context.getPackageManager();
	    if (pm.hasSystemFeature(PackageManager.FEATURE_WIFI))
		addAction(new InternalActions.ToggleWifiAction(context), phoneSettingsAction);
	    addAction(new InternalActions.ToggleSilentAction(context), phoneSettingsAction);
	    if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
		addAction(new InternalActions.SpeakerphoneAction(context), phoneSettingsAction);

	    phoneCallAction = new PhoneCallAction();
	    addAction(phoneCallAction);

	    quickDialAction = new QuickDialAction();
	    addAction(quickDialAction);

	    // TODO: Move these into InternalActions once the functionality is
	    // finalised
	    addAction(new Action() {

		@Override
		public String getName() {
		    return Preferences.autoSpeakerphone ? "Answer (speakerphone)" : "Answer";
		}

		@Override
		public String bulletIcon() {
		    return "bullet_circle.bmp";
		}

		@Override
		public int performAction(Context context) {
		    MediaControl.getInstance().answerCall(context);
		    return ApplicationBase.BUTTON_USED;
		}

		public boolean isHidden() {
		    return !Call.isRinging;
		}

	    }, phoneCallAction);
	    addAction(new Action() {

		@Override
		public String getName() {
		    return "Reject";
		}

		@Override
		public String bulletIcon() {
		    return "bullet_circle.bmp";
		}

		@Override
		public int performAction(Context context) {
		    MediaControl.getInstance().dismissCall(context);
		    toRoot(context);
		    return ApplicationBase.BUTTON_USED;
		}

		public boolean isHidden() {
		    return !Call.isRinging;
		}

	    }, phoneCallAction);

	    addAction(new Action() {

		@Override
		public String getName() {
		    return "Hang up";
		}

		@Override
		public String bulletIcon() {
		    return "bullet_circle.bmp";
		}

		@Override
		public int performAction(Context context) {
		    MediaControl.getInstance().dismissCall(context);
		    toRoot(context);
		    return ApplicationBase.BUTTON_USED;
		}

		public boolean isHidden() {
		    return Call.isRinging;
		}

	    }, phoneCallAction);
	    addAction(new InternalActions.SpeakerphoneAction(context), phoneCallAction);

	    addAction(new InternalActions.PingAction());
	    addAction(new InternalActions.WeatherRefreshAction());
	    // addAction(new InternalActions.ClickerAction());
	    addAction(new InternalActions.VoiceSearchAction(context));
	    addAction(new InternalActions.ToggleWatchSilentAction());

	    lastNotificationAction = new Action() {

		public String getName() {
		    return "Last Notification";
		}

		public String getId() {
		    return "lastNotification";
		}

		public String bulletIcon() {
		    return "bullet_triangle.bmp";
		}

		public int performAction(Context context) {
		    Notification.getInstance().replay(context);
		    // DONT_UPDATE since the idle screen overwrites the
		    // notification otherwise.
		    return ApplicationBase.BUTTON_USED_DONT_UPDATE;
		}
	    };

	    addAction(lastNotificationAction);

	}
    }

    public void addAction(Action action) {
	if (action.getId() != null) {
	    actions.put(action.getId(), action);
	}
    }

    public void addAction(Action action, ContainerAction parent) {
	if (action.getId() != null) {
	    actions.put(action.getId(), action);
	}

	parent.addSubAction(action);
    }

    public void addAction(Action action, String parentId) {
	if (actions.containsKey(parentId)) {
	    Action parent = actions.get(parentId);
	    addAction(action, (ContainerAction) parent);
	} else {
	    addAction(action);
	}
    }

    public Action getAction(final String id) {
	if (!actions.containsKey(id))
	    return null;
	return actions.get(id);
    }

    public List<Action> getRootActions(Context context) {
	List<Action> result = new ArrayList<Action>();

	initActions(context);

	result.add(phoneCallAction);
	result.add(quickDialAction);

	notificationsAction.refreshSubActions(context);
	appManagerAction.refreshSubActions(context);
	quickDialAction.refreshSubActions(context);
	result.add(notificationsAction);
	result.add(appManagerAction);
	result.add(phoneSettingsAction);

	result.add(getAction(InternalActions.ToggleWatchSilentAction.id));

	result.add(getAction(InternalActions.PingAction.id));

	if (Preferences.weatherProvider != WeatherProvider.DISABLED)
	    result.add(getAction(InternalActions.WeatherRefreshAction.id));

	result.add(getAction(InternalActions.VoiceSearchAction.id));

	// result.add(getAction(InternalActions.MapsAction.id);
	// result.add(getAction(InternalActions.WoodchuckAction.id);

	/*
	 * // For scroll testing. for (int i = 0; i < 12; i++) { final int f = i; result.add(new Action() { public String getName() { return String.valueOf(f); }
	 * 
	 * public String bulletIcon() { return "bullet_triangle.bmp"; }
	 * 
	 * public int performAction(Context context) { return BUTTON_USED; } }); }
	 */

	return result;
    }

    public List<Action> getBindableActions(final Context context) {
	List<Action> result = new ArrayList<Action>();

	initActions(context);

	result.add(quickDialAction);
	result.add(lastNotificationAction);
	result.add(notificationsAction);
	result.add(appManagerAction);
	result.add(phoneSettingsAction);

	appManagerAction.refreshSubActions(context);
	result.addAll(appManagerAction.getSubActions());

	return result;
    }

    public void displayAction(final Context context, ContainerAction container) {
	ActionsApp app = (ActionsApp) AppManager.getInstance(context).getApp(ActionsApp.APP_ID);
	if (app != null) {
	    app.displayContainer(container);
	    app.open(context, true);
	}
    }

    public void displayCallActions(final Context context) {
	Call.endRinging(context);
	displayAction(context, phoneCallAction);
    }

    public void toRoot(final Context context) {
	ActionsApp app = (ActionsApp) AppManager.getInstance(context).getApp(ActionsApp.APP_ID);
	app.toRoot();
    }

}
