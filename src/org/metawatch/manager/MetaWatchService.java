
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
 * MetaWatchService.java                                                     *
 * MetaWatchService                                                          *
 * Always connected BT watch service                                         *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.metawatch.communityedition.R;
import org.metawatch.manager.Notification.VibratePattern;
import org.metawatch.manager.actions.ActionManager;
import org.metawatch.manager.apps.ApplicationBase;
import org.metawatch.manager.widgets.WidgetManager;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class MetaWatchService extends Service {

	/* TODO: Making this static will break if we want multiple MWMs running. */
	static volatile Context context;
	
	public static BluetoothAdapter bluetoothAdapter;
	BluetoothSocket bluetoothSocket;
	static InputStream inputStream;
	static OutputStream outputStream;
	static ServiceThread serviceThread;
	static Service instance = null;

	//TelephonyManager telephonyManager;
	NotificationManager notificationManager;
	android.app.Notification notification;

	public static PowerManager powerManager;
	public static PowerManager.WakeLock wakeLock;

	public static volatile int connectionState;
	public static int watchType;
	public static int watchState;
	public static boolean fakeWatch = false; 	// Setting this to true disables all the bt comms, and just pretends its connected to a watch.  Enable by setting the MAC address to ANALOG or DIGITAL

	public static TestSmsLoop testSmsLoop;
	private boolean lastConnectionState = false;
	
	private static boolean silentMode = false;
	public static boolean SilentMode() { return silentMode; }
	public static void setSilentMode( boolean enabled ) {
		silentMode = enabled;
		Idle.updateIdle(context, true);
		
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		editor.putBoolean("SilentMode", silentMode);
		editor.commit();
		
	}

	final static class ConnectionState {
		static final int DISCONNECTED = 0;
		static final int CONNECTING = 1;
		static final int CONNECTED = 2;
		static final int DISCONNECTING = 3;
	}

	public final static class WatchBuffers {
		public static final int IDLE = 0;
		public static final int APPLICATION = 1;
		public static final int NOTIFICATION = 2;
	}

	final static class WatchStates {
		static final int OFF = 0;
		static final int IDLE = 1;
		static final int APPLICATION = 2;
		static final int NOTIFICATION = 3;
		static final int CALL = 3;
	}
	
	final static class Msg {
		static final int REGISTER_CLIENT = 0;
		static final int UNREGISTER_CLIENT = 1;
		static final int UPDATE_STATUS = 2;
		static final int SEND_TOAST = 3;
		static final int DISCONNECT = 4;
	}
	
	public final static class WeatherProvider {
		public static final int DISABLED = 0;
		public static final int GOOGLE_DEPRECATED = 1;
		public static final int WUNDERGROUND = 2;
		public static final int YAHOO = 3;
	}
	
	public final static class GeolocationMode {
		public static final int MANUAL = 0;
		public static final int USEPROVIDER = 1;
		public static final int ALWAYSGOOGLE = 2;
	}

	final static class WatchModes {
		public static volatile boolean IDLE = false;
		public static volatile boolean APPLICATION = false;
		public static volatile boolean NOTIFICATION = false;
		public static volatile boolean CALL = false;
	}
	
	public final static class QuickButton {
		public final static int DISABLED = 0;
		public final static int NOTIFICATION_REPLAY = 1;
		public final static int OPEN_ACTIONS = 2;
	}
	
	public final static class AppLaunchMode {
		public static final int POPUP = 0;
		public static final int APP_PAGE = 1;
	}

	public static class Preferences {
		public static boolean loaded = false;
		public static boolean logging = true;
		
		public static boolean notifyCall = true;
		public static boolean notifySMS = true;
		public static boolean notifyGmail = true;
		public static boolean notifyK9 = true;
		public static boolean notifyTD = true;
		public static boolean notifyAlarm = true;
		public static boolean notifyBatterylow = true;
		public static boolean notifyMusic = true;
		public static boolean notifyCalendar = true;
		public static boolean notifyNMA = true;
		public static boolean notifyNewVoicemail = true;
		public static String watchMacAddress = "";
		public static int packetWait = 10;
		public static boolean skipSDP = false;
		public static boolean insecureBtSocket = false;
		public static boolean invertLCD = false;
		public static boolean notificationCenter = false;
		public static boolean notifyLight = false;
		public static boolean stickyNotifications = true;
		public static int weatherProvider = WeatherProvider.YAHOO;
		public static String weatherCity = "Dallas TX";
		public static boolean weatherCelsius = false;
		public static int weatherGeolocationMode = GeolocationMode.USEPROVIDER;
		public static String wundergroundKey = "";
		public static int fontSize = 2;
		public static int smsLoopInterval = 15;
		public static int idleMusicControlMethod = MediaControl.MUSICSERVICECOMMAND;
		public static int quickButton = QuickButton.DISABLED;
		public static boolean notificationLarger = false;
		public static boolean autoConnect = false;
		public static boolean autoRestart = false;
		public static boolean autoClockFormat = false;
		public static boolean hapticFeedback = false;
		public static boolean readCalendarDuringMeeting = true;
		public static int readCalendarMinDurationToMeetingEnd = 15;
		public static boolean displayLocationInSmallCalendarWidget = false;
		public static boolean eventDateInCalendarWidget = false;
		public static boolean displayWidgetRowSeparator = false;
		public static boolean overlayWeatherText = false;
		public static boolean clockOnEveryPage = false;
		public static boolean appBufferForClocklessPages = true;
		public static boolean showNotificationQueue = false;
		public static boolean dumpWatchScreenshots = false;
		public static int appLaunchMode = AppLaunchMode.POPUP;
		public static boolean autoSpeakerphone = false;
		public static boolean showActionsInCall = true;
		public static String themeName = "";
		public static boolean hideEmptyWidgets = false;
		public static boolean hiddenWidgetsReserveSpace = false;
	}

	public final class WatchType {
		public static final int ANALOG = 1;
		public static final int DIGITAL = 2;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	private static OnSharedPreferenceChangeListener prefChangeListener = 
			new OnSharedPreferenceChangeListener() {
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			
			if (Preferences.logging) Log.d(MetaWatch.TAG, 
					"onSharedPreferenceChanged "+key);
			
			MetaWatchService.loadPreferences(context);
			
			if (key.contains("Weather")) {
				Monitors.restart(context);
			}
			
			if (key.contains("Idle") || key.contains(".app_enabled")) {
				Idle.reset(context);
			}	
			
			if (key.contains("Widgets") || (key.equals("SilentMode"))) {
				Idle.updateIdle(context, true);
			}
			
			if (key.equals("InvertLCD")) {
				Protocol.setNvalLcdInvert(Preferences.invertLCD);
				if(watchState == WatchStates.IDLE) {
					Protocol.updateLcdDisplay(MetaWatchService.WatchBuffers.NOTIFICATION);
					Protocol.updateLcdDisplay(MetaWatchService.WatchBuffers.IDLE);
				}
			}
		}
	};
	
	public static void loadPreferences(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		
		Preferences.logging = sharedPreferences.getBoolean("logging",
				Preferences.logging);
		Preferences.notifyCall = sharedPreferences.getBoolean("NotifyCall",
				Preferences.notifyCall);
		Preferences.notifySMS = sharedPreferences.getBoolean("NotifySMS",
				Preferences.notifySMS);
		Preferences.notifyGmail = sharedPreferences.getBoolean("NotifyGmail",
				Preferences.notifyGmail);
		Preferences.notifyK9 = sharedPreferences.getBoolean("NotifyK9",
				Preferences.notifyK9);
		Preferences.notifyTD = sharedPreferences.getBoolean("NotifyTD",
				Preferences.notifyTD);
		Preferences.notifyAlarm = sharedPreferences.getBoolean("NotifyAlarm",
				Preferences.notifyAlarm);
		Preferences.notifyMusic = sharedPreferences.getBoolean("NotifyMusic",
				Preferences.notifyMusic);
		Preferences.notifyCalendar = sharedPreferences.getBoolean(
				"NotifyCalendar", Preferences.notifyCalendar);
		Preferences.notifyNMA = sharedPreferences.getBoolean("notifyNMA",
				Preferences.notifyNMA);
		Preferences.notifyNewVoicemail = sharedPreferences.getBoolean(
				"NotifyNewVoicemail", Preferences.notifyNewVoicemail);
		Preferences.watchMacAddress = sharedPreferences.getString("MAC",
				Preferences.watchMacAddress).trim();
		Preferences.skipSDP = sharedPreferences.getBoolean("SkipSDP",
				Preferences.skipSDP);
		Preferences.insecureBtSocket = sharedPreferences.getBoolean("InsecureBtSocket", 
				Preferences.insecureBtSocket);
		Preferences.invertLCD = sharedPreferences.getBoolean("InvertLCD",
				Preferences.invertLCD);
		Preferences.notificationCenter = sharedPreferences.getBoolean(
				"notificationCenter", Preferences.notificationCenter);
		Preferences.notifyLight = sharedPreferences.getBoolean("notifyLight",
				Preferences.notifyLight);
		Preferences.stickyNotifications = sharedPreferences.getBoolean(
				"stickyNotifications", Preferences.stickyNotifications);
		Preferences.weatherCity = sharedPreferences.getString("WeatherCity",
				Preferences.weatherCity);	
		Preferences.weatherProvider = Integer.parseInt(
				sharedPreferences.getString("WeatherProvider", 
				Integer.toString(Preferences.weatherProvider)));
		Preferences.weatherCelsius = sharedPreferences.getBoolean(
				"WeatherCelsius", Preferences.weatherCelsius);
		Preferences.weatherGeolocationMode = Integer.parseInt(
				sharedPreferences.getString("WeatherGeolocationMode", 
				Integer.toString(Preferences.weatherGeolocationMode)));
		Preferences.wundergroundKey = sharedPreferences.getString(
				"WundergroundKey", Preferences.wundergroundKey).trim();
		Preferences.idleMusicControlMethod = Integer.parseInt(
				sharedPreferences.getString("IdleMusicControlMethod", 
				Integer.toString(Preferences.idleMusicControlMethod)));
		Preferences.quickButton = Integer.parseInt(
				sharedPreferences.getString("QuickButton", 
				Integer.toString(Preferences.quickButton)));
		Preferences.autoConnect = sharedPreferences.getBoolean(
				"AutoConnect", Preferences.autoConnect);	
		Preferences.autoRestart = sharedPreferences.getBoolean("AutoRestart", 
				Preferences.autoRestart);
		Preferences.autoClockFormat = sharedPreferences.getBoolean("AutoClockFormat",
				Preferences.autoClockFormat);
		Preferences.hapticFeedback = sharedPreferences.getBoolean("HapticFeedback",
				Preferences.hapticFeedback);
		Preferences.readCalendarDuringMeeting = sharedPreferences.getBoolean("ReadCalendarDuringMeeting",
				Preferences.readCalendarDuringMeeting);
		Preferences.readCalendarMinDurationToMeetingEnd = Integer.parseInt(
				sharedPreferences.getString("ReadCalendarMinDurationToMeetingEnd", 
				Integer.toString(Preferences.readCalendarMinDurationToMeetingEnd)));
		Preferences.displayLocationInSmallCalendarWidget = sharedPreferences.getBoolean("DisplayLocationInSmallCalendarWidget",
				Preferences.displayLocationInSmallCalendarWidget);
		Preferences.eventDateInCalendarWidget = sharedPreferences.getBoolean("EventDateInCalendarWidget",
				Preferences.eventDateInCalendarWidget);
		Preferences.displayWidgetRowSeparator = sharedPreferences.getBoolean("DisplayWidgetRowSeparator",
				Preferences.displayWidgetRowSeparator);
		Preferences.overlayWeatherText = sharedPreferences.getBoolean("OverlayWeatherText",
				Preferences.overlayWeatherText);
		Preferences.clockOnEveryPage = sharedPreferences.getBoolean("ClockOnEveryPage",
				Preferences.clockOnEveryPage);
		Preferences.appBufferForClocklessPages = sharedPreferences.getBoolean("AppBufferForClocklessPages",
				Preferences.appBufferForClocklessPages);
		Preferences.showNotificationQueue = sharedPreferences.getBoolean("ShowNotificationQueue",
				Preferences.showNotificationQueue);
		Preferences.dumpWatchScreenshots = sharedPreferences.getBoolean("DumpWatchScreenshots",
				Preferences.dumpWatchScreenshots);
		Preferences.autoSpeakerphone = sharedPreferences.getBoolean("autoSpeakerphone",
				Preferences.autoSpeakerphone);
		Preferences.showActionsInCall = sharedPreferences.getBoolean("showActionsInCall",
				Preferences.showActionsInCall);
		Preferences.themeName = sharedPreferences.getString("ThemeName",
				Preferences.themeName);
		Preferences.hideEmptyWidgets = sharedPreferences.getBoolean("HideEmptyWidgets",
				Preferences.hideEmptyWidgets);
		Preferences.hiddenWidgetsReserveSpace = sharedPreferences.getBoolean("HiddenWidgetsReserveSpace",
				Preferences.hiddenWidgetsReserveSpace);
		
		boolean silent = sharedPreferences.getBoolean("SilentMode", silentMode );
		if (silent!=silentMode)
			silentMode = silent;
				
		try {
			Preferences.fontSize = Integer.valueOf(sharedPreferences.getString(
					"FontSize", Integer.toString(Preferences.fontSize)));
			Preferences.packetWait = Integer.valueOf(sharedPreferences
					.getString("PacketWait",
							Integer.toString(Preferences.packetWait)));
			Preferences.smsLoopInterval = Integer.valueOf(sharedPreferences
					.getString("SmsLoopInterval",
							Integer.toString(Preferences.smsLoopInterval)));
			Preferences.appLaunchMode = Integer.valueOf(sharedPreferences.getString(
					"AppLaunchMode", Integer.toString(Preferences.appLaunchMode)));
			
		} catch (NumberFormatException e) {
		}

	}
	
	public static boolean getPreviousConnectionState(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		return sharedPreferences.getBoolean("PreviousConnectionState", false);
	}
	
	public static void setPreviousConnectionState(Context context, boolean connected) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();

		editor.putBoolean("PreviousConnectionState", connected);
		editor.commit();
	}

	public static void saveMac(Context context, String mac) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();

		editor.putString("MAC", mac);
		editor.commit();
	}
	
	public static void saveTheme(Context context, String theme) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();

		editor.putString("ThemeName", theme);
		editor.commit();
	}
	
	public static String getWidgets(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		
		if(watchType == WatchType.DIGITAL) {
			return sharedPreferences.getString("widgets", WidgetManager.defaultWidgetsDigital);
		}
		else if(watchType == WatchType.ANALOG) {
			return sharedPreferences.getString("widgetsAnalog", WidgetManager.defaultWidgetsAnalog);
		}
		
		return "";
	}
	
	public static void saveWidgets(Context context, String widgets) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();

		if(watchType == WatchType.ANALOG) {
			editor.putString("widgetsAnalog", widgets);
		}
		else {	
			editor.putString("widgets", widgets);
		}
		editor.commit();
	}
	
	public static void autoStartService(Context context) {
		if(connectionState != ConnectionState.DISCONNECTED)
			return;
		
		if (!Preferences.loaded)
			MetaWatchService.loadPreferences(context);
		
		if(Preferences.autoConnect && getPreviousConnectionState(context)==true) {
			context.startService(new Intent(context, MetaWatchService.class));
			if (Preferences.logging) Log.v(MetaWatch.TAG, "Service auto started");
		} 
	}
	
	public static boolean isRunning() {
		return instance != null;
	}
	
	public void createNotification() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		boolean hideNotificationIcon = sharedPreferences.getBoolean(
				"HideNotificationIcon", false);
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"MetaWatchService.createNotification(): hideNotificationIcon="
						+ hideNotificationIcon);
		int notificationIcon = (hideNotificationIcon ? R.drawable.transparent_square
				: R.drawable.disconnected);
		notification = new android.app.Notification(notificationIcon, getResources().getString(R.string.app_name),
				System.currentTimeMillis());
		notification.flags |= android.app.Notification.FLAG_ONGOING_EVENT;

		notification.setLatestEventInfo(this, "MetaWatch Manager", "Idle", createNotificationPendingIntent());

		startForeground(1, notification);
	}

	private PendingIntent createNotificationPendingIntent() {
		return PendingIntent.getActivity(this, 0, new Intent(this,
				MetaWatch.class), 0);
	}

	public void updateNotification() {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		boolean hideNotificationIcon = sharedPreferences.getBoolean(
				"HideNotificationIcon", false);
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"MetaWatchService.updateNotification(): hideNotificationIcon="
						+ hideNotificationIcon);
		switch (connectionState) {
		case ConnectionState.CONNECTING:
			notification.icon = (hideNotificationIcon ? R.drawable.transparent_square
					: R.drawable.disconnected);
			notification.setLatestEventInfo(this, "MetaWatch Manager", "Connecting", createNotificationPendingIntent());
			broadcastConnection(false);
			break;
		case ConnectionState.CONNECTED:
			notification.icon = (hideNotificationIcon ? R.drawable.transparent_square
					: R.drawable.connected);
			notification.setLatestEventInfo(this, getResources().getString(R.string.app_name), getResources().getString(R.string.connection_connected), createNotificationPendingIntent());
			broadcastConnection(true);
			break;
		default:
			notification.icon = (hideNotificationIcon ? R.drawable.transparent_square
					: R.drawable.disconnected);
			notification.setLatestEventInfo(this, getResources().getString(R.string.app_name), getResources().getString(R.string.connection_disconnected), createNotificationPendingIntent());
			broadcastConnection(false);
			break;
		}
		
		startForeground(1, notification);
		notifyClients();
	}

	public void removeNotification() {
		stopForeground(true);
	}

	
	@Override
	public void onCreate() {
		super.onCreate();
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"MetaWatchService.onCreate()");
		
		instance = this;
		context = this;
		
		initialize();
	}
	
	private void initialize() {
		if (!Preferences.loaded)
			loadPreferences(this);
		
		PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(prefChangeListener);
		
		createNotification();

		connectionState = ConnectionState.CONNECTING;
		watchState = WatchStates.OFF;
		watchType = WatchType.DIGITAL;

		if (bluetoothAdapter == null)
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"MetaWatch");

		Monitors.start(this/*, telephonyManager*/);
		
		// Initialise theme
		BitmapCache.getBitmap(context, "");
		
		start();

	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    //handleCommand(intent);
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
		
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"MetaWatchService.onStartCommand()");
		
		if (connectionState == ConnectionState.DISCONNECTED)
			initialize();
		
	    return START_STICKY;
	}

	@Override
	public void onDestroy() {
		disconnectExit();			
		super.onDestroy();
		serviceThread.quit();
		instance=null;
		
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"MetaWatchService.onDestroy()");
		PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(prefChangeListener);
		
		Monitors.stop(this);
		removeNotification();
		notifyClients();
		mClients.clear();
	}

	@TargetApi(10)
	void connect(Context context) {

		try {
			
			if (!Preferences.loaded)
				loadPreferences(context);
			
			MetaWatchService.fakeWatch = false;
			if (Preferences.watchMacAddress.equals("DIGITAL")) {
				MetaWatchService.fakeWatch = true;
				MetaWatchService.watchType = MetaWatchService.WatchType.DIGITAL;
			}
			if (Preferences.watchMacAddress.equals("ANALOG")) {
				MetaWatchService.fakeWatch = true;
				MetaWatchService.watchType = MetaWatchService.WatchType.ANALOG;
			}

			if (Preferences.logging) Log.d(MetaWatch.TAG, "Remote device address: '"
					+ Preferences.watchMacAddress + "'");
			
			if (!MetaWatchService.fakeWatch) {
	
				if (bluetoothAdapter == null) {
					sendToast(getResources().getString(R.string.error_bluetooth_not_supported));
					return;
				} else if (!bluetoothAdapter.isEnabled()) {
					sendToast(getResources().getString(R.string.error_bluetooth_not_enabled));
					return;
				}
				
				wakeLock.acquire();
				
				BluetoothDevice bluetoothDevice = bluetoothAdapter
						.getRemoteDevice(Preferences.watchMacAddress);
			
				if (Preferences.skipSDP) {
					Method method = bluetoothDevice.getClass().getMethod(
							"createRfcommSocket", new Class[] { int.class });
					bluetoothSocket = (BluetoothSocket) method.invoke(
							bluetoothDevice, 1);
				} else {
					UUID uuid = UUID
							.fromString("00001101-0000-1000-8000-00805F9B34FB");
					
					int currentapiVersion = android.os.Build.VERSION.SDK_INT;
					
					if (Preferences.insecureBtSocket && currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
						bluetoothSocket = bluetoothDevice
								.createInsecureRfcommSocketToServiceRecord(uuid);
					} else {
						bluetoothSocket = bluetoothDevice
								.createRfcommSocketToServiceRecord(uuid);
					}
					
				}

				bluetoothAdapter.cancelDiscovery();
				bluetoothSocket.connect();
		
				inputStream = bluetoothSocket.getInputStream();
				outputStream = bluetoothSocket.getOutputStream();
			}
			
			connectionState = ConnectionState.CONNECTED;
			setPreviousConnectionState(context, true);
			updateNotification();

			Protocol.startProtocolSender();
			
			// RM: This is disabled for now, as it seems to confuse the watch fw (3.1.0S tested)
			// and get it into a state where it won't accept any date/time format updates :-S
			
			//if( Preferences.autoClockFormat )
			//	Protocol.setTimeDateFormat(this);
			
			Protocol.getRealTimeClock();
			Protocol.getDeviceType();

			Notification.startNotificationSender(this);
			
			Idle.updateIdle(this, true);
			
		} catch (IOException ioexception) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, ioexception.toString());
		} catch (SecurityException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, e.toString());
		} catch (NoSuchMethodException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, e.toString());
		} catch (IllegalArgumentException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, e.toString());
		} catch (IllegalAccessException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, e.toString());
		} catch (InvocationTargetException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, e.toString());
		} catch (NullPointerException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, e.toString());
		} finally {
			if(wakeLock != null && wakeLock.isHeld()) {
				wakeLock.release();
			}
		}
				
		return;
	}
	
	public void sendToast(String text) {
		Message m = new Message();
		m.what = Msg.SEND_TOAST;
		m.obj = text;
		messageHandler.sendMessage(m);
	}

    /** Keeps track of all current registered clients. */
    static ArrayList<Messenger> mClients = new ArrayList<Messenger>();
  
	private static Handler messageHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "handleMessage "+msg);
			switch (msg.what) {
            case Msg.REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case Msg.UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            case Msg.SEND_TOAST:
            	Toast.makeText(context, 
            			(CharSequence) msg.obj,
            			Toast.LENGTH_SHORT).show();
                break;
            default:
                super.handleMessage(msg);
			}
		}

	};

	/**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final static Messenger mMessenger = new Messenger(messageHandler);

    public static void notifyClients() {
    	for (int i=mClients.size()-1; i>=0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null,
                        Msg.UPDATE_STATUS, 0, 0));
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            }
        }
    }
	
	void disconnect() {
		Protocol.stopProtocolSender();
		Notification.stopNotificationSender();
		try {
			if (outputStream != null)
				outputStream.close();
		} catch (IOException e) {
		}
		try {
			if (inputStream != null)
				inputStream.close();
		} catch (IOException e) {
		}
		try {
			if (bluetoothSocket != null)
				bluetoothSocket.close();
		} catch (IOException e) {
		}
		broadcastConnection(false);
	}

	void disconnectExit() {
		connectionState = ConnectionState.DISCONNECTING;
		setPreviousConnectionState(context, false);
		updateNotification();
		disconnect();
	}

	private class ServiceThread extends Thread
	{			
		private Handler handler;
		private Looper looper;
		
		public ServiceThread(String name) {
			super(name);
		}

		@Override
		public void run() {
			
			try {
								
				Looper.prepare();
				looper = Looper.myLooper();
				handler = new Handler();
				
				Runnable ProcessState = new Runnable() {
						public void run() { 
							int delay = processState();
							if(delay >= 0) {
								handler.postDelayed(this, delay);
							} else {
								connectionState = ConnectionState.DISCONNECTED;
								updateNotification();
								handler.removeCallbacks(this);
								looper.quit();
							}
						}
				};
					
				handler.post(ProcessState);
				Looper.loop();			
			} 
			catch(Throwable T)
			{
				if (Preferences.logging) Log.d(MetaWatch.TAG, "serviceThread: " + T.getMessage());	
			}
			finally
			{				
				connectionState = ConnectionState.DISCONNECTED;
				updateNotification();		
				if (instance != null) {
					instance.stopSelf();
				}
			}
		}
		
		public void quit() {
			if (looper != null)
				looper.quit();
		}		
	}	
	
	
	int processState()
	{
		int result = 0;
		switch (connectionState) {
		case ConnectionState.DISCONNECTED:
			if (Preferences.logging) Log.d(MetaWatch.TAG, "state: disconnected");
			break;
		case ConnectionState.CONNECTING:
			if (Preferences.logging) Log.d(MetaWatch.TAG, "state: connecting");
			// create initial connection or reconnect
			updateNotification();					
			connect(MetaWatchService.this);
			if(powerManager.isScreenOn()) {
				result = 10000; //try to reconnect in 10s 
			} else {
				result = 30000; //try to reconnect in 30s				
			}
			break;
		case ConnectionState.CONNECTED:
			if (Preferences.logging) Log.d(MetaWatch.TAG, "state: connected");
			// read from input stream
			readFromDevice();
			break;
		case ConnectionState.DISCONNECTING:
			if (Preferences.logging) Log.d(MetaWatch.TAG, "state: disconnecting");
			// exit			
			result = -1;
			break;
		}	
		
		return result;
	}
		
	void start() {
		
		serviceThread = new ServiceThread("MetaWatch Service Thread"); 		
		serviceThread.start();

		/* DEBUG */
		String voltageFrequencyString = PreferenceManager
				.getDefaultSharedPreferences(this).getString(
						"collectWatchVoltage", "0");
		try {
			
			final int voltageFrequency = Integer
					.parseInt(voltageFrequencyString);
			if (voltageFrequency > 0) {
				
				AlarmManager alarmManager = (AlarmManager) this
						.getSystemService(Context.ALARM_SERVICE);
				Intent intent = new Intent(this, AlarmReceiver.class);
				intent.putExtra("action_poll_voltage", "poll_voltage");
				PendingIntent sender = PendingIntent.getBroadcast(this, 1,
						intent, PendingIntent.FLAG_UPDATE_CURRENT);
				long sleep = voltageFrequency * 60 * 1000;
				alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 0, sleep,
						sender);
				if (Preferences.logging) Log.d(MetaWatch.TAG,
						"MetaWatchService.start(): Set voltage reading every "
								+ sleep + "ms");
			}
			
		} catch (NumberFormatException nfe) {
			if (Preferences.logging) Log.e(MetaWatch.TAG,
					"MetaWatchService.start(): bad voltage frequency string '"
							+ voltageFrequencyString + "'");
		}

	}

	void readFromDevice() {

		if (MetaWatchService.fakeWatch) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}
			return;
		}
		
		try {
			byte[] bytes = new byte[256];
			if (Preferences.logging) Log.d(MetaWatch.TAG, "before blocking read");
			// Do a proper read loop 
			int haveread = 0;
			int lengthtoread = 4;
			while((lengthtoread-haveread) != 0)
			{
			    haveread += inputStream.read(bytes, haveread, lengthtoread-haveread);
			    if(haveread > 1)
			    {
			        lengthtoread = bytes[1];
			    }
			}
			
			wakeLock.acquire();

			// print received
			String str = "received: ";
			int len = (bytes[1] & 0xFF);
			if (Preferences.logging) Log.d(MetaWatch.TAG, "packet length: " + len);

			for (int i = 0; i < len; i++) {
				// str+= Byte.toString(bytes[i]) + ", ";
				str += "0x"
						+ Integer.toString((bytes[i] & 0xff) + 0x100, 16)
								.substring(1) + ", ";
			}
			if (Preferences.logging) Log.d(MetaWatch.TAG, str);
			/*
			 * switch (bytes[2]) { case eMessageType.GetDeviceTypeResponse.msg:
			 * if (Preferences.logging) Log.d(MetaWatch.TAG, "received: device type response"); break;
			 * case eMessageType.NvalOperationResponseMsg.msg:
			 * if (Preferences.logging) Log.d(MetaWatch.TAG, "received: nval response"); break; case
			 * eMessageType.StatusChangeEvent.msg: if (Preferences.logging) Log.d(MetaWatch.TAG,
			 * "received: status change event"); break; }
			 */
			/*
			 * if (bytes[2] == 0x31) { // nval response if (bytes[3] == 0x00) //
			 * success if (bytes[4] == 0x00) // set to 12 hour format
			 * Protocol.setNvalTime(true); }
			 */
			if (bytes[2] == eMessageType.NvalOperationResponseMsg.msg) {
			    if (Preferences.logging) Log.d(MetaWatch.TAG,
                        "MetaWatchService.readFromDevice(): NvalOperationResponseMsg");
			    // Do something here?
			} else if (bytes[2] == eMessageType.StatusChangeEvent.msg) { // status
																	// change
																	// event
			    if (Preferences.logging) Log.d(MetaWatch.TAG,
                        "MetaWatchService.readFromDevice(): status change");
			    if (bytes[4] == 0x01) {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"MetaWatchService.readFromDevice(): mode changed");
					synchronized (Notification.modeChanged) {
						Notification.modeChanged.notify();
					}
			    } else if (bytes[4] == 0x11) {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"MetaWatchService.readFromDevice(): scroll request notification");

					synchronized (Notification.scrollRequest) {
						Notification.scrollRequest.notify();
					}
				} else if (bytes[4] == 0x10) {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"MetaWatchService.readFromDevice(): scroll complete.");
				}
			}

			else if (bytes[2] == eMessageType.ButtonEventMsg.msg) { // button
																	// press
				if (Preferences.logging) Log.d(MetaWatch.TAG,
						"MetaWatchService.readFromDevice(): button event");
				pressedButton(bytes[3]&0xFF); // 
			}

			else if (bytes[2] == eMessageType.GetDeviceTypeResponse.msg) { // device
																			// type
				if (bytes[4] == 1 || bytes[4] == 4) {
					watchType = WatchType.ANALOG;
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"MetaWatchService.readFromDevice(): device type response; analog watch");

					if (watchState == WatchStates.OFF || watchState == WatchStates.IDLE) {
						Idle.toIdle(this);
						Idle.updateIdle(this, true);
					}
					
					SharedPreferences sharedPreferences = PreferenceManager
							.getDefaultSharedPreferences(this);
					boolean displaySplash = sharedPreferences.getBoolean("DisplaySplashScreen", true);
					if (displaySplash) {
						Protocol.sendOledBitmap(Utils.getBitmap(this, "splash_16_0.bmp"), MetaWatchService.WatchBuffers.NOTIFICATION, 0);
						Protocol.sendOledBitmap(Utils.getBitmap(this, "splash_16_1.bmp"), MetaWatchService.WatchBuffers.NOTIFICATION, 1);
					}
					
				} else {
					watchType = WatchType.DIGITAL;
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"MetaWatchService.readFromDevice(): device type response; digital watch");

					Protocol.configureMode();
					Protocol.setNvalLcdInvert(Preferences.invertLCD);

					if (watchState == WatchStates.OFF || watchState == WatchStates.IDLE) {
						Idle.toIdle(this);
						Idle.updateIdle(this, true);
					}
					
					SharedPreferences sharedPreferences = PreferenceManager
							.getDefaultSharedPreferences(this);
					boolean displaySplash = sharedPreferences.getBoolean("DisplaySplashScreen", true);
					if (displaySplash) {
						Notification.addBitmapNotification(this, Utils.getBitmap(this, "splash.png"), new VibratePattern(false, 0, 0, 0), 10000, "Splash");
					}
					
					Protocol.queryNvalTime();
				}
				
				SharedPreferences sharedPreferences = PreferenceManager
						.getDefaultSharedPreferences(context);
				
				/* Notify watch on connection if requested. */
				boolean notifyOnConnect = sharedPreferences.getBoolean("NotifyWatchOnConnect", false);
				if (Preferences.logging) Log.d(MetaWatch.TAG, "MetaWatchService.connect(): notifyOnConnect=" + notifyOnConnect);
				if (notifyOnConnect) {
					NotificationBuilder.createOtherNotification(context, null, "MetaWatch", getResources().getString(R.string.connection_connected), 1);
				}
				
				Idle.activateButtons(this);
								
			} else if (bytes[2] == eMessageType.ReadBatteryVoltageResponse.msg) {
				boolean powerGood = bytes[4] > 0;
				boolean batteryCharging = bytes[5] > 0;
				float batterySense = (((int) bytes[7] << 8) + (int) bytes[6]) / 1000.0f;
				float batteryAverage = (((int) bytes[9] << 8) + (int) bytes[8]) / 1000.0f;
				if (Preferences.logging) Log.d(MetaWatch.TAG,
						"MetaWatchService.readFromDevice(): received battery voltage response."
								+ " power_good=" + powerGood
								+ " battery_charging=" + batteryCharging
								+ " battery_sense=" + batterySense
								+ " battery_average=" + batteryAverage);
				String voltageFrequencyString = PreferenceManager.getDefaultSharedPreferences(this).getString(
						"collectWatchVoltage", "0");
				final int voltageFrequency = Integer.parseInt(voltageFrequencyString);
				if (voltageFrequency > 0) {
					File sdcard = Environment.getExternalStorageDirectory();
					File csv = new File(sdcard,"metawatch_voltage.csv");
					boolean fileExists = csv.exists();
					FileWriter fw = new FileWriter(csv, true);
					if (fileExists == false) {
						fw.write("Date,Sense,Average\n");
					}
					Date date = new Date();
					fw.write("\"" + date.toString()+ "\"," + batterySense + "," + batteryAverage + "\n");
					fw.flush();
					fw.close();
				}		
			} else if (bytes[2] == eMessageType.ReadLightSensorResponse.msg) {
				float lightSense = (((int) bytes[1] << 8) + (int) bytes[0]) / 1000.0f;
				float lightAverage = (((int) bytes[3] << 8) + (int) bytes[2]) / 1000.0f;
				if (Preferences.logging) Log.d(MetaWatch.TAG,
						"MetaWatchService.readFromDevice(): received light sensor response."
								+ " light_sense=" + lightSense
								+ " light_average=" + lightAverage);
			} else if (bytes[2] == eMessageType.GetRealTimeClockResponse.msg) {
				long timeNow = System.currentTimeMillis();
				long roundTrip = timeNow - Monitors.getRTCTimestamp;
				
				if (Preferences.logging) Log.d(MetaWatch.TAG, 
						"MetaWatchService.readFromDevice(): received rtc response."
								+ " round trip= "+roundTrip );
				
				Monitors.rtcOffset = (int)(roundTrip/2000);
				
				Protocol.sendRtcNow(context);
				
			} else {
				if (Preferences.logging) Log.d(MetaWatch.TAG,
						"MetaWatchService.readFromDevice(): Unknown message : 0x"+Integer.toString((bytes[2] & 0xff) + 0x100, 16).substring(1) + ", ");
			}

		} catch (IOException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, e.toString());
			resetConnection();
		} catch(ArrayIndexOutOfBoundsException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, e.toString());
			resetConnection();
		} finally {
			if(wakeLock != null && wakeLock.isHeld()) {
				wakeLock.release();
			}
				
		}
		
	}
	
	private void resetConnection() {
		Log.d(MetaWatch.TAG, "MetaWatchService.resetConnection()");
		
		wakeLock.acquire();
		
		try {
		if (connectionState != ConnectionState.DISCONNECTING) {
			connectionState = ConnectionState.CONNECTING;			
			disconnect();
		}	
		} finally {
			if(wakeLock != null && wakeLock.isHeld()) {
				wakeLock.release();
			}				
		}
	}

	void broadcastConnection(boolean connected) {
		if (connected != lastConnectionState) {
			lastConnectionState = connected;
			Intent intent = new Intent(
					"org.metawatch.manager.CONNECTION_CHANGE");
			intent.putExtra("state", connected);
			sendBroadcast(intent);
			notifyClients();
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"MetaWatchService.broadcastConnection(): Broadcast connection change: state='"
							+ connected + "'");
			Protocol.resetLCDDiffBuffer();
		}
	}

	static long lastOledCrownPress = 0;
	void pressedButton(int button) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "button code: " + Integer.toString(button));
		
		wakeLock.acquire();
		
		try {
			
			if(button>0 && Preferences.hapticFeedback)
				Protocol.vibrate(5, 5, 2);
	
			if (Preferences.logging) Log.d(MetaWatch.TAG, "MetaWatchService.pressedButton(): watchState="
					+ watchState);
			switch (watchState) {
			case WatchStates.IDLE: {
				
				int idleAppButton = Idle.appButtonPressed(this, button);
				if( idleAppButton == ApplicationBase.BUTTON_NOT_USED )
				{
					
					switch (button) {
					
					case Idle.QUICK_BUTTON:
						Idle.quickButtonAction(this);
						break;
						
					case Idle.IDLE_NEXT_PAGE:							
						if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL) {
							Idle.nextPage(this);
							Idle.updateIdle(this, true);	
						}
						break;
						
					case Idle.TOGGLE_SILENT:
						MetaWatchService.setSilentMode(!silentMode);
						Protocol.vibrate(500, 500, 2);
						break;
						
					case Idle.IDLE_OLED_DISPLAY:
						long time = System.currentTimeMillis();
						
						if(time-lastOledCrownPress < 1000*5)
						{
							Idle.nextPage(this);
							Idle.updateIdle(this, true);
						}
						
						lastOledCrownPress = time;
						Idle.sendOledIdle(this);
						break;
													
					case Application.TOGGLE_APP:
						Application.toggleApp(context, Idle.getCurrentApp());
						break;
					}
				}
				else if (idleAppButton != ApplicationBase.BUTTON_USED_DONT_UPDATE)
				{
					Idle.updateIdle(this, false);
					if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG)
						Idle.sendOledIdle(this);
				}
				break;
			}
				
			case WatchStates.APPLICATION:
				Application.buttonPressed(this, button);
				break;
				
			case WatchStates.NOTIFICATION:
				
				switch (button) {
				case Call.CALL_ANSWER:
					MediaControl.answerCall(this);
					break;			
				case Call.CALL_DISMISS:
					MediaControl.ignoreCall(this);
					break;
				case Call.CALL_MENU:
					ActionManager.displayCallActions(this);
					break;
				default:
					Notification.buttonPressed(button);
					break;
				}
				break;
			}
		
		} 
		finally
		{
			if(wakeLock != null && wakeLock.isHeld()) {
				wakeLock.release();
			}
		}
		


	}
	
	@Override
	public void onLowMemory() {		
		MemoryInfo mi = new MemoryInfo();
		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		activityManager.getMemoryInfo(mi);
		long availableMegs = mi.availMem / 1048576L;
		
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"MetaWatchService.onLowMemory(): " + availableMegs + "Mb free");	
		
		super.onLowMemory();
	}
	

}
