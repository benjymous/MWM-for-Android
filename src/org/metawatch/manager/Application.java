                                                                     
                                                                     
                                                                     
                                             
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
  * Application.java                                                          *
  * Application                                                               *
  * Application watch mode                                                    *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.apps.InternalApp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

public class Application {
	//FIXME This class has next to NO support for analog watches...
	
	public final static byte EXIT_APP = 100;
	public final static byte TOGGLE_APP = 101;
	
	private static InternalApp currentApp = null;

	public static void startAppMode(Context context) {
		startAppMode(context, null);
	}
	
	public static void startAppMode(Context context, InternalApp internalApp) {
		if (currentApp != null) {
			stopAppMode(context);
		}

		MetaWatchService.WatchModes.APPLICATION = true;
		currentApp = internalApp;
	}
	
	public static void stopAppMode(Context context) {
		MetaWatchService.WatchModes.APPLICATION = false;
		
		int watchType = MetaWatchService.watchType;
		disableToggleButton(watchType);
		if (watchType == MetaWatchService.WatchType.DIGITAL) {
			Protocol.disableButton(0, 1, MetaWatchService.WatchBuffers.APPLICATION); // right top - press
		} else if (watchType == MetaWatchService.WatchType.ANALOG) {
			Protocol.disableButton(1, 1, MetaWatchService.WatchBuffers.APPLICATION); // right middle - press
		}
		if (currentApp != null) {
			currentApp.deactivate(MetaWatchService.watchType);
			currentApp.setInactive();
		}
		currentApp = null;
		
		if (MetaWatchService.WatchModes.IDLE == true) {
			Idle.toIdle(context);
		}
	}
	
	public static void updateAppMode(Context context) {
		Bitmap bitmap;
		if (currentApp != null) {
			bitmap = currentApp.update(context, false, MetaWatchService.watchType);
		} else {
			bitmap = Protocol.createTextBitmap(context, "Starting application mode ...");
		}
		
		updateAppMode(context, bitmap);
	}
	
	public static void updateAppMode(Context context, Bitmap bitmap) {
		MetaWatchService.WatchModes.APPLICATION = true;
		
		if (MetaWatchService.WatchModes.APPLICATION == true) {
			
			// enable app mode if there is no parent mode currently active
			if (MetaWatchService.watchState < MetaWatchService.WatchStates.APPLICATION)
				MetaWatchService.watchState = MetaWatchService.WatchStates.APPLICATION;
			
			if (MetaWatchService.watchState == MetaWatchService.WatchStates.APPLICATION) {
				Protocol.sendLcdBitmap(bitmap, MetaWatchService.WatchBuffers.APPLICATION);
				Protocol.updateLcdDisplay(MetaWatchService.WatchBuffers.APPLICATION);
			}
		}		
	}
	
	public static void updateAppMode(Context context, int[] array) {
		MetaWatchService.WatchModes.APPLICATION = true;
		
		if (MetaWatchService.WatchModes.APPLICATION == true) {
			
			// enable app mode if there is no parent mode currently active
			if (MetaWatchService.watchState < MetaWatchService.WatchStates.APPLICATION)
				MetaWatchService.watchState = MetaWatchService.WatchStates.APPLICATION;
			
			if (MetaWatchService.watchState == MetaWatchService.WatchStates.APPLICATION) {
				Protocol.sendLcdArray(array, MetaWatchService.WatchBuffers.APPLICATION);
				Protocol.updateLcdDisplay(MetaWatchService.WatchBuffers.APPLICATION);
			}
		}		
	}
	
	public static void updateAppMode(Context context, byte[] buffer) {
		MetaWatchService.WatchModes.APPLICATION = true;
		
		if (MetaWatchService.WatchModes.APPLICATION == true) {
			
			// enable app mode if there is no parent mode currently active
			if (MetaWatchService.watchState < MetaWatchService.WatchStates.APPLICATION)
				MetaWatchService.watchState = MetaWatchService.WatchStates.APPLICATION;
			
			if (MetaWatchService.watchState == MetaWatchService.WatchStates.APPLICATION) {
				Protocol.sendLcdBuffer(buffer, MetaWatchService.WatchBuffers.APPLICATION);
				Protocol.updateLcdDisplay(MetaWatchService.WatchBuffers.APPLICATION);
			}
		}		
	}
	
	public static void enableToggleButton(int watchType) {
		if (watchType == MetaWatchService.WatchType.DIGITAL) {
			Protocol.enableButton(0, 2, TOGGLE_APP, MetaWatchService.WatchBuffers.APPLICATION); // right top - hold
			Protocol.enableButton(0, 3, TOGGLE_APP, MetaWatchService.WatchBuffers.APPLICATION); // right top - long hold
		} else if (watchType == MetaWatchService.WatchType.ANALOG) {
			Protocol.enableButton(1, 2, TOGGLE_APP, MetaWatchService.WatchBuffers.APPLICATION); // right middle - hold
			Protocol.enableButton(1, 3, TOGGLE_APP, MetaWatchService.WatchBuffers.APPLICATION); // right middle - long hold
		}
	}
	
	public static void disableToggleButton(int watchType) {
		if (watchType == MetaWatchService.WatchType.DIGITAL) {
			Protocol.disableButton(0, 2, MetaWatchService.WatchBuffers.APPLICATION); // right top - hold
			Protocol.disableButton(0, 3, MetaWatchService.WatchBuffers.APPLICATION); // right top - long hold
		} else if (watchType == MetaWatchService.WatchType.ANALOG) {
			Protocol.disableButton(1, 2, MetaWatchService.WatchBuffers.APPLICATION); // right middle - hold
			Protocol.disableButton(1, 3, MetaWatchService.WatchBuffers.APPLICATION); // right middle - long hold
		}
	}
	
	public static void toApp() {
		MetaWatchService.watchState = MetaWatchService.WatchStates.APPLICATION;
		
		Idle.deactivateButtons();

		int watchType = MetaWatchService.watchType;
		if (currentApp != null) {
			currentApp.activate(watchType);
		}
		if (watchType == MetaWatchService.WatchType.DIGITAL) {
			Protocol.enableButton(0, 1, EXIT_APP, MetaWatchService.WatchBuffers.APPLICATION); // right top - press
		} else if (watchType == MetaWatchService.WatchType.ANALOG) {
			Protocol.enableButton(1, 1, EXIT_APP, MetaWatchService.WatchBuffers.APPLICATION); // right middle - press
		}
		if (currentApp != null && currentApp.isToggleable())
			enableToggleButton(watchType);
		
		// update screen with cached buffer
		Protocol.updateLcdDisplay(MetaWatchService.WatchBuffers.APPLICATION);
	}
	
	public static void toggleApp(Context context, InternalApp app) {
		if (app != null) {
			if (app.appState == InternalApp.ACTIVE_IDLE) {
				if (Preferences.logging) Log.d(MetaWatch.TAG, "Application.toggleApp(): switching to stand-alone.");
				
				Idle.removeAppPage(app);
				app.open(context);
				return;
			
			} else if (app.appState == InternalApp.ACTIVE_STANDALONE) {
				if (Preferences.logging) Log.d(MetaWatch.TAG, "Application.toggleApp(): switching to idle.");
				
				int page = Idle.addAppPage(app);
				currentApp = null; // Avoid having stopAppMode() deactivate the app.
				Idle.toPage(page);
				stopAppMode(context); // Goes to Idle if not in Notification.
				return;
			}
		}
		
		throw new IllegalStateException("Can't toggle app mode for an inactive app");
	}
	
	public static void buttonPressed(Context context, byte button) {
		if (button == EXIT_APP) {
			stopAppMode(context);
			
		} else if (button == TOGGLE_APP) {
			toggleApp(context, currentApp);
			
		} else if (currentApp != null) {
			currentApp.buttonPressed(context, button);
			
		} else {
			// Broadcast button to external app
			Intent intent = new Intent("org.metawatch.manager.BUTTON_PRESS");
			intent.putExtra("button", button);
			context.sendBroadcast(intent);
		}
	}
}
