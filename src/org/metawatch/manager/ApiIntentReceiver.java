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
 * ApiIntentReceiver.java                                                    *
 * ApiIntentReceiver                                                         *
 * Intent receiver for 3rd party software                                     *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.ConnectionState;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Notification.VibratePattern;
import org.metawatch.manager.apps.AppManager;
import org.metawatch.manager.apps.ApplicationBase;
import org.metawatch.manager.apps.ExternalApp;
import org.metawatch.manager.widgets.WidgetManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import org.metawatch.manager.Log;

public class ApiIntentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

	final String action = intent.getAction();
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "ApiIntentReceiver.onReceive(): received intent, action='" + action + "'");

	// add digital watch check

	if (action.equals("org.metawatch.manager.APPLICATION_UPDATE")) {
	    String id = intent.hasExtra("id") ? intent.getStringExtra("id") : "anonymous";
	    if (intent.hasExtra("array")) {
		Bitmap bmp = Bitmap.createBitmap(intent.getIntArrayExtra("array"), 96, 96, Bitmap.Config.RGB_565);
		if (Preferences.invertLCD && intent.getBooleanExtra("autoinvert", false)) {
		    bmp = Utils.invertBitmap(bmp);
		}
		ApplicationBase app = AppManager.getInstance(context).getApp(id);
		if (app != null && app instanceof ExternalApp) {
		    ((ExternalApp) app).setBuffer(bmp);
		    if (app.appState == ApplicationBase.ACTIVE_IDLE)
			Idle.getInstance().updateIdle(context, true);
		    else if (app.appState == ApplicationBase.ACTIVE_POPUP)
			Application.refreshCurrentApp(context);
		}
	    }
	    return;
	}

	else if (action.equals("org.metawatch.manager.WIDGET_UPDATE")) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "WIDGET_UPDATE received");
	    WidgetManager.getInstance(context).getFromIntent(context, intent);
	    return;
	}

	else if (action.equals("org.metawatch.manager.APPLICATION_START") || action.equals("org.metawatch.manager.APPLICATION_ANNOUNCE")) {
	    String id = intent.hasExtra("id") ? intent.getStringExtra("id") : "anonymous";
	    String name = intent.hasExtra("name") ? intent.getStringExtra("name") : "External App";

	    ApplicationBase app = AppManager.getInstance(context).getApp(id);
	    if (app == null) {
		app = new ExternalApp(id, name);
		AppManager.getInstance(context).addApp(app);
	    }

	    if (MetaWatchService.connectionState == ConnectionState.CONNECTED) {
		if (action.equals("org.metawatch.manager.APPLICATION_START")) {
		    int page = Idle.getInstance().addAppPage(context, app);
		    Idle.getInstance().toPage(context, page);
		} else {
		    // Auto open as a page, if it's enabled in preferences
		    final String pageSetting = app.getInfo().getPageSettingName();
		    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		    if (prefs.getBoolean(pageSetting, false)) {
			Idle.getInstance().addAppPage(context, app);
		    }
		}
	    }

	    return;
	}

	else if (action.equals("org.metawatch.manager.APPLICATION_STOP")) {
	    String id = intent.hasExtra("id") ? intent.getStringExtra("id") : "anonymous";

	    ApplicationBase app = AppManager.getInstance(context).getApp(id);
	    if (app != null) {
		Idle.getInstance().removeAppPage(context, app);
		Idle.getInstance().updateIdle(context, true);
	    }

	    return;
	}

	else if (action.equals("org.metawatch.manager.NOTIFICATION")) {

	    /* Set up vibrate pattern. */
	    VibratePattern vibrate = getVibratePatternFromIntent(intent);

	    if (intent.hasExtra("oled1") || intent.hasExtra("oled1a") || intent.hasExtra("oled1b") || intent.hasExtra("oled2") || intent.hasExtra("oled2a") || intent.hasExtra("oled2b")) {

		byte[] line1 = Protocol.getInstance(context).createOled1line(context, null, "");
		byte[] line2 = Protocol.getInstance(context).createOled1line(context, null, "");
		byte[] scroll = null;
		int scrollLen = 0;
		if (intent.hasExtra("oled1")) {
		    line1 = Protocol.getInstance(context).createOled1line(context, null, intent.getStringExtra("oled1"));
		} else {
		    if (intent.hasExtra("oled1a") || intent.hasExtra("oled1b")) {
			String oled1a = "";
			String oled1b = "";
			if (intent.hasExtra("oled1a")) {
			    oled1a = intent.getStringExtra("oled1a");
			}
			if (intent.hasExtra("oled1b")) {
			    oled1b = intent.getStringExtra("oled1b");
			}
			line1 = Protocol.getInstance(context).createOled2lines(context, oled1a, oled1b);
		    }
		}
		if (intent.hasExtra("oled2")) {
		    line2 = Protocol.getInstance(context).createOled1line(context, null, intent.getStringExtra("oled2"));
		} else {
		    if (intent.hasExtra("oled2a") || intent.hasExtra("oled2b")) {
			String oled2a = "";
			String oled2b = "";
			if (intent.hasExtra("oled2a")) {
			    oled2a = intent.getStringExtra("oled2a");
			}
			if (intent.hasExtra("oled2b")) {
			    oled2b = intent.getStringExtra("oled2b");
			}
			scroll = new byte[800];
			scrollLen = Protocol.getInstance(context).createOled2linesLong(context, oled2b, scroll);
			line2 = Protocol.getInstance(context).createOled2lines(context, oled2a, oled2b);
		    }
		}
		Notification.getInstance().addOledNotification(context, line1, line2, scroll, scrollLen, vibrate, "API oled notification");

	    } else if (intent.hasExtra("text")) {
		String title = "Notification";
		if (intent.hasExtra("title")) {
		    title = intent.getStringExtra("title");
		}
		String text = intent.getStringExtra("text");

		Bitmap icon = null;
		if (intent.hasExtra("icon")) {
		    icon = Bitmap.createBitmap(intent.getIntArrayExtra("icon"), intent.getIntExtra("iconWidth", 16), intent.getIntExtra("iconHeight", 16), Bitmap.Config.RGB_565);
		}

		NotificationBuilder.createSmart(context, title, text, icon, false, vibrate);

		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "ApiIntentReceiver.onReceive(): sending text notification; text='" + text + "'");
	    } else if (intent.hasExtra("array")) {
		int[] array = intent.getIntArrayExtra("array");
		Notification.getInstance().addArrayNotification(context, array, vibrate, "API Array notification");
	    } else if (intent.hasExtra("buffer")) {
		byte[] buffer = intent.getByteArrayExtra("buffer");
		Notification.getInstance().addBufferNotification(context, buffer, vibrate, "API Buffer notification");
	    }
	    return;
	}

	if (action.equals("org.metawatch.manager.VIBRATE")) {
	    /* Set up vibrate pattern. */
	    VibratePattern vibrate = getVibratePatternFromIntent(intent);

	    if (vibrate.vibrate)
		Protocol.getInstance(context).vibrate(vibrate.on, vibrate.off, vibrate.cycles);

	    return;
	}

	else if (action.equals("org.metawatch.manager.SILENTMODE")) {
	    if (intent.hasExtra("enabled") && !MetaWatchStatus.mShutdownRequested && MetaWatchService.mIsRunning) {
		Intent setSilentMode = new Intent(context, MetaWatchService.class);
		setSilentMode.putExtra(MetaWatchService.COMMAND_KEY, intent.getBooleanExtra("enabled", false) ? MetaWatchService.SILENT_MODE_ENABLE : MetaWatchService.SILENT_MODE_DISABLE);
		context.startService(setSilentMode);
	    }
	}

    }

    private VibratePattern getVibratePatternFromIntent(Intent intent) {
	/* Set up vibrate pattern. */
	VibratePattern vibrate = Notification.VibratePattern.NO_VIBRATE;
	if (intent.hasExtra("vibrate_on") && intent.hasExtra("vibrate_off") && intent.hasExtra("vibrate_cycles")) {
	    int vibrateOn = intent.getIntExtra("vibrate_on", 500);
	    int vibrateOff = intent.getIntExtra("vibrate_off", 500);
	    int vibrateCycles = intent.getIntExtra("vibrate_cycles", 3);
	    vibrate = new VibratePattern(true, vibrateOn, vibrateOff, vibrateCycles);
	}
	return vibrate;
    }

}
