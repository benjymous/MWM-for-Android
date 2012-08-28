                                                                     
                                                                     
                                                                     
                                             
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
  * MetaWatch.java                                                            *
  * MetaWatch                                                                 *
  * Main activity with tab container                                          *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/


package org.metawatch.manager;

import java.io.IOException;
import java.io.InputStream;

import org.metawatch.communityedition.R;
import org.metawatch.manager.MetaWatchService.GeolocationMode;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WeatherProvider;
import org.metawatch.manager.Monitors.LocationData;
import org.metawatch.manager.apps.AppManager;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.bugsense.trace.BugSenseHandler;

public class MetaWatch extends TabActivity {
   
	public static final String TAG = "MetaWatch";
	
	public static TextView textView = null;	
	public static ToggleButton toggleButton = null;
	
    private static Messenger mService = null;
	    
    private static long startupTime = 0;
    
    private static Context context = null;
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        
        // If you want to use BugSense for your fork, register with them
        // and place your API key in /assets/bugsense.txt
        // (This prevents me receiving reports of crashes from forked versions
        // which is somewhat confusing!)      
        try {
			InputStream inputStream = getAssets().open("bugsense.txt");
			String key = Utils.ReadInputStream(inputStream);
			key=key.trim();
			if (Preferences.logging) Log.d(MetaWatch.TAG, "BugSense enabled");
			BugSenseHandler.setup(this, key);
		} catch (IOException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "No BugSense keyfile found");
		}
        
		MetaWatchService.loadPreferences(this);
		AppManager.initApps(this);
        
        startupTime = System.currentTimeMillis();
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        final Resources res = getResources();
        final TabHost tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator(res.getString(R.string.ui_tab_status),res.getDrawable(R.drawable.ic_tab_status))
                .setContent(new Intent(this, MetaWatchStatus.class)));

        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator(res.getString(R.string.ui_tab_preferences),res.getDrawable(R.drawable.ic_tab_settings))
                .setContent(new Intent(this, Settings.class)));
        
        tabHost.addTab(tabHost.newTabSpec("tab3")
        		.setIndicator("destroy")
                .setIndicator(res.getString(R.string.ui_tab_widgets),res.getDrawable(R.drawable.ic_tab_widgets))
                .setContent(new Intent(this, WidgetSetup.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        
        tabHost.addTab(tabHost.newTabSpec("tab4")
                .setIndicator(res.getString(R.string.ui_tab_tests),res.getDrawable(R.drawable.ic_tab_test))
                .setContent(new Intent(this, Test.class)));
        
        synchronized (MetaWatchStatus.textView) {
        	if (MetaWatchStatus.textView==null) {
		        try {
					MetaWatchStatus.textView.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					finish();
				}
        	}
	        textView = MetaWatchStatus.textView;
	        toggleButton = MetaWatchStatus.toggleButton;
	        toggleButton.setChecked(isServiceRunning());
	        
        }
		
		if (Preferences.watchMacAddress == "") {
			// Show the watch discovery screen on first start
			startActivity(new Intent(getApplicationContext(), DeviceSelection.class));
		}
		
	
		toggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if(toggleButton.isChecked())
            		startService();
            	else
            		stopService();
            }
        });
		
		displayStatus();
		
		Protocol.configureMode();
		
		if (!isServiceRunning() && Preferences.autoConnect) {
			startService();
		}
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	@Override
	protected void onStart() {
		super.onStart();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.about:
	    	showAbout();
	        return true;
	    case R.id.exit:	        
	    	exit();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
    
	void startService() {

		if(!isServiceRunning()) {
			Context context = getApplicationContext();
			context.bindService(new Intent(MetaWatch.this, 
					MetaWatchService.class), mConnection, Context.BIND_AUTO_CREATE);
		}
		
        if(isServiceRunning()) {
        	toggleButton.setChecked(true);
        }
	}
	
    void stopService() {

		Context context = getApplicationContext();
        try {
        	context.stopService(new Intent(this, MetaWatchService.class));
            context.unbindService(mConnection);            	
        }
        catch(Throwable e) {
        	// The service wasn't running
        	if (Preferences.logging) Log.d(MetaWatch.TAG, e.getMessage());          	
        }

    	if(!isServiceRunning()) {
    		toggleButton.setChecked(false);
    	}
    }
    
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("org.metawatch.manager.MetaWatchService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    void exit() {
    	System.exit(0);
    }
    
    void showAbout() {
    	
    	WebView webView = new WebView(this);
		String html = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /><title>About</title></head><body><center>" + 
						"<img src=\"banner.jpg\" width=\"100%\">" +
						"<p>Version " + Utils.getVersion(this) + ".</p>" +
						"<b>MetaWatch Community Team</b><br>" +
						"Joakim Andersson<br>Chris Boyle<br>Garth Bushell<br>Prash D<br>Matthias Gruenewald<br>"+
						"Richard Munn<br>Craig Oliver<br>Didi Pfeifle<br>Thierry Schork<br>Kyle Schroeder<br>"+
						"Chris Sewell<br>Dobie Wollert<p>"+
						"<b>Translation Team</b><br>"+
						"Miguel Branco<br>Didi Pfeifle<br>Geurt Pieter Maassen van den Brink<br>Thierry Schork<p>"+
						"<p>&copy; Copyright 2011-2012 Meta Watch Ltd.</p>" +
						"</center></body></html>";
        webView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
        
        new AlertDialog.Builder(this).setView(webView).setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener() {			
			//@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).show();        
    }
    
    /**
     * Handler of incoming messages from service.
     */
    static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MetaWatchService.Msg.UPDATE_STATUS:
                    displayStatus();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
    private static void displayStatus() {
    	Resources res = context.getResources();
    	textView.setText(res.getString(R.string.app_name_long));
    	textView.append("\n\n");
    	
    	switch (MetaWatchService.connectionState) {
	    	case MetaWatchService.ConnectionState.DISCONNECTED:
	    		Utils.appendColoredText(textView, res.getString(R.string.connection_disconnected).toUpperCase(), Color.RED);
	    		break;
	    	case MetaWatchService.ConnectionState.CONNECTING:
	    		Utils.appendColoredText(textView, res.getString(R.string.connection_connecting).toUpperCase(), Color.YELLOW);
	    		break;
	    	case MetaWatchService.ConnectionState.CONNECTED:
	    		Utils.appendColoredText(textView, res.getString(R.string.connection_connected).toUpperCase(), Color.GREEN);
	    		break;
	    	case MetaWatchService.ConnectionState.DISCONNECTING:
	    		Utils.appendColoredText(textView, res.getString(R.string.connection_disconnecting).toUpperCase(), Color.YELLOW);
	    		break;
    	}
    	textView.append("\n");
    	
    	if (Preferences.weatherProvider != WeatherProvider.DISABLED) {
    		textView.append("\n");
    		if (Monitors.weatherData.received) {
    			textView.append(res.getString(R.string.status_weather_last_updated));
    			textView.append("\n  ");
    			textView.append(res.getString(R.string.status_weather_forecast));
    			textView.append("\n    ");
    			printDate(Monitors.weatherData.forecastTimeStamp);
    			textView.append("  ");
    			textView.append(res.getString(R.string.status_weather_observation));
    			textView.append("\n    ");
    			printDate(Monitors.weatherData.timeStamp);
    		}
    		else {
    			textView.append(res.getString(R.string.status_weather_waiting));
    		}
    	}
    	
    	if (Preferences.weatherGeolocationMode != GeolocationMode.MANUAL) {
    		textView.append("\n");
    		if (LocationData.received) {
    			textView.append(res.getString(R.string.status_location_updated));
    			textView.append("\n  ");
    			printDate(LocationData.timeStamp);
    		}
    		else {
    			textView.append(res.getString(R.string.status_location_waiting));
    			textView.append("\n");
    		}
    	}
    	
    	textView.append("\n");
    	if (Utils.isAccessibilityEnabled(context)) {    		
	    	if (MetaWatchAccessibilityService.accessibilityReceived) {
	    		Utils.appendColoredText(textView, res.getString(R.string.status_accessibility_working), Color.GREEN);
	    	}
	    	else {
	    		if(startupTime==0 || System.currentTimeMillis()-startupTime<60*1000) {
	    			textView.append(res.getString(R.string.status_accessibility_waiting));
	    		}
	    		else {
	    			Utils.appendColoredText(textView, res.getString(R.string.status_accessibility_failed), Color.RED);
	    		}
	    	}
	    }
    	else {
    		textView.append(res.getString(R.string.status_accessibility_disabled));
    	}
    	textView.append("\n");
    
    	textView.append("\n"+res.getString(R.string.status_message_queue)+" " + Protocol.getQueueLength());
    	textView.append("\n"+res.getString(R.string.status_notification_queue)+" " + Notification.getQueueLength() + "\n");
    	
    	if(Preferences.showNotificationQueue) {
    		textView.append(Notification.dumpQueue());
    	}
    }
    
    private static void printDate(long ticks) {
    	if(ticks==0) {
    		textView.append(context.getResources().getString(R.string.status_loading));
    	}
    	else {
	    	textView.append(Utils.ticksToText(context, ticks));
    	}
    	textView.append("\n");
    }
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    
    final static IncomingHandler mIncomingHandler = new IncomingHandler();
    
    final static Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private static ServiceConnection mConnection = new ServiceConnection() {
    	   	
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);
            textView.append("Attached to service\n");

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        MetaWatchService.Msg.REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {

            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            textView.append("Disconnected from service\n");
        }
    };

    /*
    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
    	mIsBound = bindService(new Intent(MetaWatch.this, 
                MetaWatchService.class), mConnection, Context.BIND_AUTO_CREATE);
        textView.append("Binding.\n");
    }*/

    /*
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            MetaWatchService.Msg.UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            textView.append("Binding.\n");
        }
        
    }*/
    
}
