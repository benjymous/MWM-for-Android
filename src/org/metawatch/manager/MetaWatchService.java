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
import java.util.Date;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.metawatch.manager.Notification.VibratePattern;
import org.metawatch.manager.actions.ActionManager;
import org.metawatch.manager.apps.AppManager;
import org.metawatch.manager.apps.ApplicationBase;
import org.metawatch.manager.widgets.WidgetManager;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
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
import android.os.ConditionVariable;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

public class MetaWatchService extends Service {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private WatchReceiverThread watchReceiverThread;
    private Handler weatherBatteryPollHandler = new Handler(Looper.getMainLooper());
    private ScheduledExecutorService watchSenderThread = Executors.newSingleThreadScheduledExecutor();
    private Future<?> mPendingSend;

    // These static fields are not modified outside of this class
    // The only reason sendQueue is static is for the Statistic dialog
    // mPauseQueue is static to block the Notification queue
    public static volatile LinkedBlockingQueue<byte[]> sendQueue = new LinkedBlockingQueue<byte[]>();
    public static ConditionVariable mPauseQueue = new ConditionVariable(true);

    private PowerManager powerManager;

    // These are not modified outside of this class, only read
    public static volatile int connectionState;
    public static int watchType = WatchType.UNKNOWN;
    public static int watchGen = WatchGen.UNKNOWN;
    private static Stack<WatchModes> watchMode = new Stack<WatchModes>();
    public static boolean fakeWatch = false;
    private boolean lastConnectionState = false;

    public static final String COMMAND_KEY = "COMMAND_KEY";
    public static final int SILENT_MODE_DISABLE = 0;
    public static final int SILENT_MODE_ENABLE = 1;
    public static final int INVERT_SILENT_MODE = 2;
    public static final int SEND_BYTE_ARRAY = 3;
    public static final String BYTE_ARRAY = "BYTE_ARRAY";

    public static boolean mIsRunning = false;
    private static boolean silentMode = false;
    
    public static WatchModes getWatchMode() {
	try {
	    return watchMode.peek();
	} catch (Exception e) {
	    return WatchModes.OFF;
	}
    }
    
    public static void previousWatchMode() {
	if (watchMode.size() != 0)
	    watchMode.pop();
	if (watchMode.size() == 0)
	    watchMode.push(WatchModes.IDLE);
    }
    
    public static void setWatchMode(WatchModes watchMode) {
	MetaWatchService.watchMode.push(watchMode);
    }
    
    public static void clearWatchMode() {
	watchMode.clear();
    }

    @Override
    public void onCreate() {
	super.onCreate();
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "MetaWatchService.onCreate()");

	if (!Preferences.loaded)
	    loadPreferences(this);

	PreferenceManager.getDefaultSharedPreferences(MetaWatchService.this).registerOnSharedPreferenceChangeListener(prefChangeListener);

	createNotification();

	connectionState = ConnectionState.CONNECTING;
	
	watchMode.clear();
	watchMode.push(WatchModes.IDLE);

	watchType = WatchType.UNKNOWN;
	watchGen = WatchGen.UNKNOWN;
	Monitors.getInstance().getRTCTimestamp = 0;

	if (bluetoothAdapter == null)
	    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
	powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

	Notification.getInstance().startNotificationSender(this);
	Monitors.getInstance().start(this/* , telephonyManager */);

	watchReceiverThread = new WatchReceiverThread("MetaWatch Service Thread");
	watchReceiverThread.setPriority(7);
	watchReceiverThread.start();
	
	watchSenderThread.execute(protocolSender);
	weatherBatteryPollHandler.post(pollWeatherBattery);

	mIsRunning = true;
    }

    @Override
    public synchronized int onStartCommand(final Intent intent, int flags, int startId) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "MetaWatchService.onStartCommand()");
	if (intent != null) {
	    switch (intent.getIntExtra(COMMAND_KEY, -1)) {
	    case SILENT_MODE_ENABLE:
		setSilentMode(true);
		break;
	    case SILENT_MODE_DISABLE:
		setSilentMode(false);
		break;
	    case INVERT_SILENT_MODE:
		setSilentMode(!silentMode);
		break;
	    case SEND_BYTE_ARRAY:
		sendQueue.add(intent.getByteArrayExtra(BYTE_ARRAY));
		break;
	    }
	}
	return Service.START_STICKY;
    }

    private Runnable protocolSender = new Runnable() {
	public void run() {
	    byte[] message = null;
	    message = sendQueue.peek();
	    if (message != null) {
		try {
		    outputStream.write(message);
		    outputStream.flush();
		    sendQueue.remove(message);
		} catch (Exception e) {
		    e.printStackTrace();
		    resetConnection();
		} finally {
		}
	    }
	    try {
		mPendingSend = watchSenderThread.schedule(this, Preferences.packetWait, TimeUnit.MILLISECONDS);
	    } catch(RejectedExecutionException e) {
		e.printStackTrace();
		//Shutting down
	    }
	    mPauseQueue.block();
	}
    };

    @Override
    public void onDestroy() {
	super.onDestroy();
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "MetaWatchService.onDestroy()");
	connectionState = ConnectionState.DISCONNECTING;
	setPreviousConnectionState(MetaWatchService.this, false);
	if (watchReceiverThread != null) {
	    watchReceiverThread.quit();
	} else {
	    connectionState = ConnectionState.DISCONNECTED;
	    setPreviousConnectionState(MetaWatchService.this, false);
	}
	if (mPendingSend != null)
	    mPendingSend.cancel(true);
	if (sendQueue != null)
	    sendQueue.clear();
	if (watchSenderThread != null)
	    watchSenderThread.shutdownNow();
	
	//This will cancel the Notification's Future because unblocking from a potentially pauses state
	Notification.getInstance().destroy();
	if (mPauseQueue != null)
	    mPauseQueue.open();

	if (weatherBatteryPollHandler != null)
	    weatherBatteryPollHandler.removeCallbacks(pollWeatherBattery);

	cleanup();
	Idle.getInstance().destroy();

	stopForeground(true);
	if (prefChangeListener != null)
	    PreferenceManager.getDefaultSharedPreferences(MetaWatchService.this).unregisterOnSharedPreferenceChangeListener(prefChangeListener);

	Monitors.getInstance().destroy(this);
	BitmapCache.getInstance().destroy();
	AppManager.getInstance(this).destroy();
	ActionManager.getInstance(this).destroy();
	WidgetManager.getInstance(this).destroy();
	
	Protocol.getInstance(this).destroy();
	MediaControl.getInstance().destroy();
	
	watchMode.clear();
	
	mIsRunning = false;
    }

    @TargetApi(10)
    private boolean connect() {

	try {

	    if (!Preferences.loaded)
		loadPreferences(this);

	    MetaWatchService.fakeWatch = false;
	    if (Preferences.watchMacAddress.equals("DIGITAL")) {
		MetaWatchService.fakeWatch = true;
		MetaWatchService.watchType = WatchType.DIGITAL;
	    }
	    if (Preferences.watchMacAddress.equals("ANALOG")) {
		MetaWatchService.fakeWatch = true;
		MetaWatchService.watchType = WatchType.ANALOG;
	    }

	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Remote device address: '" + Preferences.watchMacAddress + "'");

	    if (!MetaWatchService.fakeWatch) {

		if (bluetoothAdapter == null)
		    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(Preferences.watchMacAddress);

		int currentapiVersion = android.os.Build.VERSION.SDK_INT;

		if (Preferences.skipSDP) {
		    Method method;
		    if (Preferences.insecureBtSocket && currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			method = bluetoothDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
		    } else {
			method = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
		    }
		    bluetoothSocket = (BluetoothSocket) method.invoke(bluetoothDevice, 1);
		} else {
		    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

		    if (Preferences.insecureBtSocket && currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
		    } else {
			bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
		    }
		}

		bluetoothAdapter.cancelDiscovery();
		bluetoothSocket.connect();

		inputStream = bluetoothSocket.getInputStream();
		outputStream = bluetoothSocket.getOutputStream();
	    }

	    connectionState = ConnectionState.CONNECTED;
	    setPreviousConnectionState(this, true);
	    updateNotification();

	    Protocol.getInstance(MetaWatchService.this).getDeviceType();
	    
	    NavigationManagement.processWatchConnection(this);
	    
	    // Unblock the message protocol queue, and the notification queue.
	    mPauseQueue.open();

	    return true;

	} catch (IOException ioexception) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, ioexception.toString());
	} catch (SecurityException e) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, e.toString());
	} catch (NoSuchMethodException e) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, e.toString());
	} catch (IllegalArgumentException e) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, e.toString());
	} catch (IllegalAccessException e) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, e.toString());
	} catch (InvocationTargetException e) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, e.toString());
	} catch (NullPointerException e) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, e.toString());
	} finally {
	}
	return false;
    }

    void cleanup() {
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

    private void resetConnection() {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "MetaWatchService.resetConnection()");
	// The block is released when the connection is reestablished, it blocks the message and notification queues.
	// If the connection is not reestablished it's also unblocked in the Service onDestroy, along with the queue being cleared and shutdown, etc...
	mPauseQueue.close();
	//The receiving thread handles the connection process and by simply setting the connection state to connect, and cleaning up the streams, the connection process will occur
	connectionState = ConnectionState.CONNECTING;
	cleanup();
    }

    //A reasonable implementation.
    private class WatchReceiverThread extends Thread {
	private Handler handler;
	private Looper looper;

	public WatchReceiverThread(String name) {
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
			if (delay >= 0) {
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
	    } catch (Throwable T) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "serviceThread: " + T.getMessage());
	    } finally {
		connectionState = ConnectionState.DISCONNECTED;
		updateNotification();
		stopSelf();
	    }
	}

	public void quit() {
	    if (looper != null)
		looper.quit();
	}
    }

    int processState() {
	int result = 0;
	switch (connectionState) {
	case ConnectionState.DISCONNECTED:
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "state: disconnected");
	    break;
	case ConnectionState.CONNECTING:
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "state: connecting");
	    // create initial connection or reconnect
	    updateNotification();
	    if (!connect()) {
		if (powerManager.isScreenOn()) {
		    result = 1000;
		} else {
		    result = 5000;
		}
	    }
	    break;
	case ConnectionState.CONNECTED:
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "state: connected");
	    // read from input stream
	    readFromDevice();
	    break;
	case ConnectionState.DISCONNECTING:
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "state: disconnecting");
	    // exit
	    result = -1;
	    break;
	}

	return result;
    }

    private Runnable pollWeatherBattery = new Runnable() {
	@Override
	public void run() {
	    String voltageFrequencyString = PreferenceManager.getDefaultSharedPreferences(MetaWatchService.this).getString("collectWatchVoltage", "0");
	    try {
		final int voltageFrequency = Integer.parseInt(voltageFrequencyString);
		if (voltageFrequency > 0) {

		    Intent intent = new Intent(MetaWatchService.this, AlarmReceiver.class);
		    intent.putExtra("action_poll_voltage", "poll_voltage");
		    intent.putExtra("action_update", "update");
		    MetaWatchService.this.sendBroadcast(intent);
		}
	    } catch (NumberFormatException nfe) {
		if (Preferences.logging)
		    Log.e(MetaWatchStatus.TAG, "MetaWatchService.start(): bad voltage frequency string '" + voltageFrequencyString + "'");
	    }
	    weatherBatteryPollHandler.postDelayed(this, 360000/*Five Minutes*/);
	}
    };

    private void readFromDevice() {

	if (MetaWatchService.fakeWatch) {
	    try {
		Thread.sleep(10000);
	    } catch (InterruptedException e) {
	    }
	    return;
	}

	try {
	    byte[] bytes = new byte[256];
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "before blocking read");
	    // Do a proper read loop
	    int haveread = 0;
	    int lengthtoread = 4;
	    while ((lengthtoread - haveread) != 0) {
		haveread += inputStream.read(bytes, haveread, lengthtoread - haveread);
		if (haveread > 1) {
		    lengthtoread = bytes[1];
		}
	    }

	    // print received
	    String str = "received: ";
	    int len = (bytes[1] & 0xFF);
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "packet length: " + len);

	    for (int i = 0; i < len; i++) {
		// str+= Byte.toString(bytes[i]) + ", ";
		str += "0x" + Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1) + ", ";
	    }
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, str);
	    /*
	     * switch (bytes[2]) { case eMessageType.GetDeviceTypeResponse.msg: if (Preferences.logging) Log.d(MetaWatchStatus.TAG, "received: device type response"); break; case eMessageType.NvalOperationResponseMsg.msg: if (Preferences.logging) Log.d(MetaWatchStatus.TAG, "received: nval response"); break; case eMessageType.StatusChangeEvent.msg: if (Preferences.logging) Log.d(MetaWatchStatus.TAG, "received: status change event"); break; }
	     */
	    /*
	     * if (bytes[2] == 0x31) { // nval response if (bytes[3] == 0x00) // success if (bytes[4] == 0x00) // set to 12 hour format Protocol.setNvalTime(true); }
	     */
	    if (bytes[2] == eMessageType.NvalOperationResponseMsg.msg) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): NvalOperationResponseMsg");
		// Do something here?
	    } else if (bytes[2] == eMessageType.StatusChangeEvent.msg) { // status
	    // change
	    // event
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): status change");
		if (bytes[4] == 0x01) {
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): mode changed");
		    synchronized (Notification.getInstance().modeChanged) {
			Notification.getInstance().modeChanged.notify();
		    }
		} else if (bytes[4] == 0x11) {
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): scroll request notification");

		    synchronized (Notification.getInstance().scrollRequest) {
			Notification.getInstance().scrollRequest.notify();
		    }
		} else if (bytes[4] == 0x10) {
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): scroll complete.");
		} else if (bytes[4] == 0x02) {
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): mode timeout.");
		    // The watch switches back to idle mode (showing the initial
		    // page) after 10 minutes
		    // Activate the last used idle page in this case
		    Idle.getInstance().toPage(MetaWatchService.this, 0);
		    Idle.getInstance().toIdle(MetaWatchService.this);
		    Idle.getInstance().updateIdle(MetaWatchService.this, true);

		}
	    }

	    else if (bytes[2] == eMessageType.ButtonEventMsg.msg) { // button
		// press
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): button event");
		pressedButton(bytes[3] & 0xFF); //
	    }

	    else if (bytes[2] == eMessageType.GetDeviceTypeResponse.msg) { // device
		// type
		if (bytes[4] == 1 || bytes[4] == 4) {
		    watchType = WatchType.ANALOG;
		    watchGen = WatchGen.GEN1;
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): device type response; analog watch (gen1)");

		    NavigationManagement.processWatchConnection(this);
		    
		    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		    boolean displaySplash = sharedPreferences.getBoolean("DisplaySplashScreen", false);
		    if (displaySplash) {
			Protocol.getInstance(MetaWatchService.this).sendOledBitmap(Utils.getBitmap(this, "splash_16_0.bmp"), WatchBuffers.NOTIFICATION, 0);
			Protocol.getInstance(MetaWatchService.this).sendOledBitmap(Utils.getBitmap(this, "splash_16_1.bmp"), WatchBuffers.NOTIFICATION, 1);
		    }

		} else {
		    watchType = WatchType.DIGITAL;

		    if (bytes[4] == 5 || bytes[4] == 6) {
			watchGen = WatchGen.GEN2;
			if (Preferences.logging)
			    Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): device type response; Strata/Frame (gen2)");
		    } else {
			watchGen = WatchGen.GEN1;
			if (Preferences.logging)
			    Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): device type response; digital watch (gen1)");
		    }

		    Protocol.getInstance(MetaWatchService.this).configureMode();
		    Protocol.getInstance(MetaWatchService.this).setNvalLcdInvert(Preferences.invertLCD);

		    Protocol.getInstance(MetaWatchService.this).configureIdleBufferSize(true);

		    // Disable built in action for Right top immediate
		    Protocol.getInstance(MetaWatchService.this).disableButton(0, 0, WatchBuffers.IDLE);
		    Protocol.getInstance(MetaWatchService.this).disableButton(0, 0, WatchBuffers.APPLICATION);
		    Protocol.getInstance(MetaWatchService.this).disableButton(0, 0, WatchBuffers.NOTIFICATION);

		    NavigationManagement.processWatchConnection(this);
		    
		    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		    boolean displaySplash = sharedPreferences.getBoolean("DisplaySplashScreen", false);
		    if (displaySplash) {
			Notification.getInstance().addBitmapNotification(this, Utils.getBitmap(this, "splash.png"), new VibratePattern(false, 0, 0, 0), 10000, "Splash");
		    }

		    // In 10 seconds update the date and time format
		    // Well after the entire connection process, and Idle update on the watch
		    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
			@Override
			public void run() {
			    Protocol.getInstance(MetaWatchService.this).setTimeDateFormat(MetaWatchService.this);
			}
		    }, 10000);
		    
		}

		/* DRM Changed from getRealTimeClock(); */
		Protocol.getInstance(MetaWatchService.this).setRealTimeClock(MetaWatchService.this);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MetaWatchService.this);

		/* Notify watch on connection if requested. */
		boolean notifyOnConnect = sharedPreferences.getBoolean("NotifyWatchOnConnect", false);
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchService.connect(): notifyOnConnect=" + notifyOnConnect);
		if (notifyOnConnect) {
		    NotificationBuilder.createOtherNotification(MetaWatchService.this, null, "MetaWatch", getResources().getString(R.string.connection_connected), 1);
		}

		Idle.getInstance().activateButtons(this);

	    } else if (bytes[2] == eMessageType.ReadBatteryVoltageResponse.msg) {
		boolean powerGood = bytes[4] > 0;
		boolean batteryCharging = bytes[5] > 0;
		float batterySense = (((int) bytes[7] << 8) + (int) bytes[6]) / 1000.0f;
		float batteryAverage = (((int) bytes[9] << 8) + (int) bytes[8]) / 1000.0f;
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): received battery voltage response." + " power_good=" + powerGood + " battery_charging=" + batteryCharging + " battery_sense=" + batterySense + " battery_average=" + batteryAverage);
		String voltageFrequencyString = PreferenceManager.getDefaultSharedPreferences(this).getString("collectWatchVoltage", "0");
		final int voltageFrequency = Integer.parseInt(voltageFrequencyString);
		if (voltageFrequency > 0) {
		    File sdcard = Environment.getExternalStorageDirectory();
		    File csv = new File(sdcard, "metawatch_voltage.csv");
		    boolean fileExists = csv.exists();
		    FileWriter fw = new FileWriter(csv, true);
		    if (fileExists == false) {
			fw.write("Date,Sense,Average\n");
		    }
		    Date date = new Date();
		    fw.write("\"" + date.toString() + "\"," + batterySense + "," + batteryAverage + "\n");
		    fw.flush();
		    fw.close();
		}
	    } else if (bytes[2] == eMessageType.ReadLightSensorResponse.msg) {
		float lightSense = (((int) bytes[1] << 8) + (int) bytes[0]) / 1000.0f;
		float lightAverage = (((int) bytes[3] << 8) + (int) bytes[2]) / 1000.0f;
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): received light sensor response." + " light_sense=" + lightSense + " light_average=" + lightAverage);
	    } else if (bytes[2] == eMessageType.GetRealTimeClockResponse.msg) {
		long timeNow = System.currentTimeMillis();
		long roundTrip = timeNow - Monitors.getInstance().getRTCTimestamp;

		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): received rtc response." + " round trip= " + roundTrip);

		Monitors.getInstance().rtcOffset = (int) (roundTrip / 2000);

		Protocol.getInstance(MetaWatchService.this).setRealTimeClock(MetaWatchService.this);

	    } else {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "MetaWatchService.readFromDevice(): Unknown message : 0x" + Integer.toString((bytes[2] & 0xff) + 0x100, 16).substring(1) + ", ");
	    }

	} catch (IOException e) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, e.toString());
	    resetConnection();
	} catch (Exception e) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, e.toString());
	    resetConnection();
	}
    }

    private void broadcastConnection(boolean connected) {
	if (connected != lastConnectionState) {
	    lastConnectionState = connected;
	    Intent intent = new Intent("org.metawatch.manager.CONNECTION_CHANGE");
	    intent.putExtra("state", connected);
	    sendBroadcast(intent);
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "MetaWatchService.broadcastConnection(): Broadcast connection change: state='" + connected + "'");
	    Protocol.getInstance(MetaWatchService.this).resetLCDDiffBuffer();
	}
    }

    static long lastOledCrownPress = 0;

    private void pressedButton(int button) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "button code: " + Integer.toString(button));

	try {

	    if (button > 0 && Preferences.hapticFeedback)
		Protocol.getInstance(MetaWatchService.this).vibrate(50, 5, 3);

	    WatchModes currentMode = watchMode.peek();
	    
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "MetaWatchService.pressedButton(): watchState=" + currentMode);
	    switch (currentMode) {
	    case IDLE: {
		int idleAppButton = Idle.getInstance().appButtonPressed(this, button);
		if (idleAppButton == ApplicationBase.BUTTON_NOT_USED) {
		    switch (button) {
		    case Idle.LEFT_QUICK_BUTTON:
			Idle.getInstance().quickButtonAction(MetaWatchService.this, Preferences.quickButtonL);
			break;
		    case Idle.RIGHT_QUICK_BUTTON:
			Idle.getInstance().quickButtonAction(MetaWatchService.this, Preferences.quickButtonR);
			break;
		    case Idle.IDLE_NEXT_PAGE:
			Idle.getInstance().nextPage(this);
			Idle.getInstance().updateIdle(this, false);
			break;
		    case Idle.TOGGLE_SILENT:
			setSilentMode(!silentMode);
			Protocol.getInstance(MetaWatchService.this).vibrate(500, 500, 2);
			break;
		    case Idle.IDLE_OLED_DISPLAY:
			long time = System.currentTimeMillis();

			if (time - lastOledCrownPress < 1000 * 5) {
			    Idle.getInstance().nextPage(this);
			    Idle.getInstance().updateIdle(this, true);
			}
			lastOledCrownPress = time;
			Idle.getInstance().sendOledIdle(this);
			break;
		    }
		} else if (idleAppButton != ApplicationBase.BUTTON_USED_DONT_UPDATE) {
		    if (MetaWatchService.watchType == WatchType.ANALOG)
			Idle.getInstance().sendOledIdle(this);
		    else {
			Idle.getInstance().updateIdle(this, false);
		    }
		}
		break;
	    }
	    case APPLICATION:
		Application.buttonPressed(this, button);
		break;
	    case NOTIFICATION:
		switch (button) {
		case Call.CALL_ANSWER:
		    MediaControl.getInstance().answerCall(this);
		    break;
		case Call.CALL_DISMISS:
		    MediaControl.getInstance().dismissCall(this);
		    break;
//		case Call.CALL_MENU:
//		    ActionManager.getInstance(this).displayCallActions(this);
//		    break;
		default:
		    break;
		}
		break;
	    case CALL:
		break;
	    default:
		break;
	    }

	} finally {
	}
    }

    public static boolean silentMode() {
	return silentMode;
    }

    private void setSilentMode(boolean enabled) {
	silentMode = enabled;
	Idle.getInstance().updateIdle(MetaWatchService.this, true);
	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MetaWatchService.this);
	Editor editor = sharedPreferences.edit();
	editor.putBoolean("SilentMode", silentMode);
	editor.commit();
    }

    public static void sentBytes(Context context, byte[] bytes) {
	if (!MetaWatchStatus.mShutdownRequested && mIsRunning) {
	    Intent intent = new Intent(context, MetaWatchService.class);
	    intent.putExtra(MetaWatchService.COMMAND_KEY, MetaWatchService.SEND_BYTE_ARRAY);
	    intent.putExtra(MetaWatchService.BYTE_ARRAY, bytes);
	    context.startService(intent);
	}
    }

    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }

    private OnSharedPreferenceChangeListener prefChangeListener = new OnSharedPreferenceChangeListener() {
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "onSharedPreferenceChanged " + key);

	    loadPreferences(MetaWatchService.this);

	    if (key.contains("Weather")) {
		Monitors.getInstance().restart(MetaWatchService.this);
	    }

	    if (key.contains("Idle") || key.contains(".app_enabled")) {
		Idle.getInstance().reset(MetaWatchService.this);
	    }

	    if (key.contains("Widget") || (key.equals("SilentMode")) || key.equals("ClockOnAppBuffers")) {
		Idle.getInstance().updateIdle(MetaWatchService.this, true);
	    }

	    if (key.equals("InvertLCD")) {
		Protocol.getInstance(MetaWatchService.this).setNvalLcdInvert(Preferences.invertLCD);
//		if (watchState == WatchStates.IDLE) {
//		    Protocol.getInstance(MetaWatchService.this).configureIdleBufferSize(false);
//		    Protocol.getInstance(MetaWatchService.this).updateLcdDisplay(WatchBuffers.NOTIFICATION);
//		    Protocol.getInstance(MetaWatchService.this).configureIdleBufferSize(false);
//		    Protocol.getInstance(MetaWatchService.this).updateLcdDisplay(WatchBuffers.IDLE);
//		}
	    }

	    if (key.contains("Calendar")) {
		Monitors.getInstance().calendarChangedTimestamp = System.currentTimeMillis();
		Idle.getInstance().updateIdle(MetaWatchService.this, true);
	    }
	}
    };

    public static void loadPreferences(Context context) {
	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

	Preferences.logging = sharedPreferences.getBoolean("logging", Preferences.logging);
	Preferences.notifyCall = sharedPreferences.getBoolean("NotifyCall", Preferences.notifyCall);
	Preferences.notifySMS = sharedPreferences.getBoolean("NotifySMS", Preferences.notifySMS);
	Preferences.notifyGmail = sharedPreferences.getBoolean("NotifyGmail", Preferences.notifyGmail);
	Preferences.notifyK9 = sharedPreferences.getBoolean("NotifyK9", Preferences.notifyK9);
	Preferences.notifyTD = sharedPreferences.getBoolean("NotifyTD", Preferences.notifyTD);
	Preferences.notifyAlarm = sharedPreferences.getBoolean("NotifyAlarm", Preferences.notifyAlarm);
	Preferences.notifyMusic = sharedPreferences.getBoolean("NotifyMusic", Preferences.notifyMusic);
	Preferences.notifyCalendar = sharedPreferences.getBoolean("NotifyCalendar", Preferences.notifyCalendar);
	Preferences.notifyNMA = sharedPreferences.getBoolean("notifyNMA", Preferences.notifyNMA);
	Preferences.notifyNewVoicemail = sharedPreferences.getBoolean("NotifyNewVoicemail", Preferences.notifyNewVoicemail);
	Preferences.watchMacAddress = sharedPreferences.getString("MAC", Preferences.watchMacAddress).trim();
	Preferences.skipSDP = sharedPreferences.getBoolean("SkipSDP", Preferences.skipSDP);
	Preferences.insecureBtSocket = sharedPreferences.getBoolean("InsecureBtSocket", Preferences.insecureBtSocket);
	Preferences.invertLCD = sharedPreferences.getBoolean("InvertLCD", Preferences.invertLCD);
	Preferences.notificationCenter = sharedPreferences.getBoolean("notificationCenter", Preferences.notificationCenter);
	Preferences.notifyLight = sharedPreferences.getBoolean("notifyLight", Preferences.notifyLight);
	// Preferences.stickyNotifications = sharedPreferences.getBoolean("stickyNotifications", Preferences.stickyNotifications);
	Preferences.weatherCity = sharedPreferences.getString("WeatherCity", Preferences.weatherCity);
	Preferences.weatherProvider = Integer.parseInt(sharedPreferences.getString("WeatherProvider", Integer.toString(Preferences.weatherProvider)));
	Preferences.weatherCelsius = sharedPreferences.getBoolean("WeatherCelsius", Preferences.weatherCelsius);
	Preferences.weatherGeolocationMode = Integer.parseInt(sharedPreferences.getString("WeatherGeolocationMode", Integer.toString(Preferences.weatherGeolocationMode)));
	Preferences.wundergroundKey = sharedPreferences.getString("WundergroundKey", Preferences.wundergroundKey).trim();
	Preferences.idleMusicControlMethod = Integer.parseInt(sharedPreferences.getString("IdleMusicControlMethod", Integer.toString(Preferences.idleMusicControlMethod)));
	Preferences.quickButtonL = sharedPreferences.getString("QuickButtonL", Preferences.quickButtonL);
	Preferences.quickButtonR = sharedPreferences.getString("QuickButtonR", Preferences.quickButtonR);
	Preferences.autoConnect = sharedPreferences.getBoolean("AutoConnect", Preferences.autoConnect);
	Preferences.autoRestart = sharedPreferences.getBoolean("AutoRestart", Preferences.autoRestart);
	Preferences.autoClockFormat = sharedPreferences.getBoolean("AutoClockFormat", Preferences.autoClockFormat);
	Preferences.hapticFeedback = sharedPreferences.getBoolean("HapticFeedback", Preferences.hapticFeedback);
	Preferences.readCalendarDuringMeeting = sharedPreferences.getBoolean("ReadCalendarDuringMeeting", Preferences.readCalendarDuringMeeting);
	Preferences.readCalendarMinDurationToMeetingEnd = Integer.parseInt(sharedPreferences.getString("ReadCalendarMinDurationToMeetingEnd", Integer.toString(Preferences.readCalendarMinDurationToMeetingEnd)));
	Preferences.displayLocationInSmallCalendarWidget = sharedPreferences.getBoolean("DisplayLocationInSmallCalendarWidget", Preferences.displayLocationInSmallCalendarWidget);
	Preferences.eventDateInCalendarWidget = sharedPreferences.getBoolean("EventDateInCalendarWidget", Preferences.eventDateInCalendarWidget);
	Preferences.displayWidgetRowSeparator = sharedPreferences.getBoolean("DisplayWidgetRowSeparator", Preferences.displayWidgetRowSeparator);
	Preferences.overlayWeatherText = sharedPreferences.getBoolean("OverlayWeatherText", Preferences.overlayWeatherText);
	Preferences.clockOnEveryPage = sharedPreferences.getBoolean("ClockOnEveryPage", Preferences.clockOnEveryPage);
	Preferences.appBufferForClocklessPages = sharedPreferences.getBoolean("AppBufferForClocklessPages", Preferences.appBufferForClocklessPages);
	Preferences.showNotificationQueue = sharedPreferences.getBoolean("ShowNotificationQueue", Preferences.showNotificationQueue);
	Preferences.dumpWatchScreenshots = sharedPreferences.getBoolean("DumpWatchScreenshots", Preferences.dumpWatchScreenshots);
	Preferences.autoSpeakerphone = sharedPreferences.getBoolean("autoSpeakerphone", Preferences.autoSpeakerphone);
	Preferences.showActionsInCall = sharedPreferences.getBoolean("showActionsInCall", Preferences.showActionsInCall);
	Preferences.themeName = sharedPreferences.getString("ThemeName", Preferences.themeName);
	Preferences.hideEmptyWidgets = sharedPreferences.getBoolean("HideEmptyWidgets", Preferences.hideEmptyWidgets);
	Preferences.inverseMediaPlayerButtons = sharedPreferences.getBoolean("InverseMediaPlayerButtons", Preferences.inverseMediaPlayerButtons);
	Preferences.clockOnAppScreens = sharedPreferences.getBoolean("ClockOnAppBuffers", Preferences.clockOnAppScreens);
	Preferences.showTestWidgets = sharedPreferences.getBoolean("TestWidgets", Preferences.showTestWidgets);
	Preferences.hiddenWidgetsReserveSpace = sharedPreferences.getBoolean("HiddenWidgetsReserveSpace", Preferences.hiddenWidgetsReserveSpace);
	Preferences.alignWidgetRowToBottom = sharedPreferences.getBoolean("AlignWidgetRowToBottom", Preferences.alignWidgetRowToBottom);
	Preferences.displayWidgetIconOnTop = sharedPreferences.getBoolean("DisplayWidgetIconOnTop", Preferences.displayWidgetIconOnTop);
	Preferences.displayCalendars = sharedPreferences.getString("DisplayCalendars", Preferences.displayCalendars);
	Preferences.animations = sharedPreferences.getBoolean("animations", true);
	Preferences.smsWithAlert = sharedPreferences.getBoolean("NotifySMSAlert", true);

	boolean silent = sharedPreferences.getBoolean("SilentMode", silentMode);
	if (silent != silentMode)
	    silentMode = silent;

	try {
	    Preferences.fontSize = Integer.valueOf(sharedPreferences.getString("FontSize", Integer.toString(Preferences.fontSize)));
	    // Preferences.packetWait = Integer.valueOf(sharedPreferences.getString("PacketWait", Integer.toString(Preferences.packetWait)));
	    Preferences.smsLoopInterval = Integer.valueOf(sharedPreferences.getString("SmsLoopInterval", Integer.toString(Preferences.smsLoopInterval)));
	    Preferences.calendarLookahead = Integer.valueOf(sharedPreferences.getString("CalendarLookahead", Integer.toString(Preferences.calendarLookahead)));

	} catch (NumberFormatException e) {
	}

    }

    public static boolean getPreviousConnectionState(Context context) {
	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	return sharedPreferences.getBoolean("PreviousConnectionState", false);
    }

    public static void setPreviousConnectionState(Context context, boolean connected) {
	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	Editor editor = sharedPreferences.edit();

	editor.putBoolean("PreviousConnectionState", connected);
	editor.commit();
    }

    public static void saveMac(Context context, String mac) {
	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	Editor editor = sharedPreferences.edit();

	editor.putString("MAC", mac);
	editor.commit();
    }

    public static void saveTheme(Context context, String theme) {
	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	Editor editor = sharedPreferences.edit();

	editor.putString("ThemeName", theme);
	editor.commit();
    }

    public static String getWidgets(Context context) {
	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

	if (watchType == WatchType.DIGITAL) {
	    return sharedPreferences.getString("widgets", WidgetManager.getInstance(context).defaultWidgetsDigital);
	} else if (watchType == WatchType.ANALOG) {
	    return sharedPreferences.getString("widgetsAnalog", WidgetManager.getInstance(context).defaultWidgetsAnalog);
	}

	return "";
    }

    public static void saveWidgets(Context context, String widgets) {
	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	Editor editor = sharedPreferences.edit();

	if (watchType == WatchType.ANALOG) {
	    editor.putString("widgetsAnalog", widgets);
	} else {
	    editor.putString("widgets", widgets);
	}
	editor.commit();
    }

    public static void autoStartService(Context context) {
	if (connectionState != ConnectionState.DISCONNECTED)
	    return;

	if (!Preferences.loaded)
	    loadPreferences(context);

	if (Preferences.autoConnect) {
	    context.startService(new Intent(context, MetaWatchService.class));
	    if (Preferences.logging)
		Log.v(MetaWatchStatus.TAG, "Service auto started");
	}
    }

    public void createNotification() {
	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	boolean hideNotificationIcon = sharedPreferences.getBoolean("HideNotificationIcon", false);
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "MetaWatchService.createNotification(): hideNotificationIcon=" + hideNotificationIcon);

	NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
	builder.setTicker(getResources().getString(R.string.app_name));
	builder.setContentTitle(getResources().getString(R.string.app_name));
	builder.setContentIntent(createNotificationPendingIntent());
	builder.setOngoing(true);
	builder.setOnlyAlertOnce(true);
	switch (connectionState) {
	case ConnectionState.CONNECTING:
	    builder.setContentText(getResources().getString(R.string.connection_connecting));
	    builder.setSmallIcon((hideNotificationIcon ? R.drawable.transparent_square : R.drawable.disconnected));
	    builder.setProgress(0, 0, true);
	    broadcastConnection(false);
	    break;
	case ConnectionState.CONNECTED:
	    builder.setContentText(getResources().getString(R.string.connection_connected));
	    builder.setSmallIcon((hideNotificationIcon ? R.drawable.transparent_square : R.drawable.connected));
	    broadcastConnection(true);
	    break;
	default:
	    builder.setContentText(getResources().getString(R.string.connection_disconnected));
	    builder.setSmallIcon((hideNotificationIcon ? R.drawable.transparent_square : R.drawable.disconnected));
	    broadcastConnection(false);
	    break;
	}
	startForeground(1, builder.build());
    }

    private PendingIntent createNotificationPendingIntent() {
	return PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
    }

    public void updateNotification() {
	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	boolean hideNotificationIcon = sharedPreferences.getBoolean("HideNotificationIcon", false);
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "MetaWatchService.updateNotification(): hideNotificationIcon=" + hideNotificationIcon);
	createNotification();
    }

    @Override
    public void onLowMemory() {
	MemoryInfo mi = new MemoryInfo();
	ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	activityManager.getMemoryInfo(mi);
	long availableMegs = mi.availMem / 1048576L;

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "MetaWatchService.onLowMemory(): " + availableMegs + "Mb free");

	super.onLowMemory();
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

    public static enum WatchModes {
	OFF, IDLE, APPLICATION, NOTIFICATION, CALL
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
	public static int packetWait = 30;
	public static boolean skipSDP = false;
	public static boolean insecureBtSocket = false;
	public static boolean invertLCD = false;
	public static boolean notificationCenter = false;
	public static boolean notifyLight = false;
	public static boolean stickyNotifications = false;
	public static int weatherProvider = WeatherProvider.YAHOO;
	public static String weatherCity = "Dallas TX";
	public static boolean weatherCelsius = false;
	public static int weatherGeolocationMode = GeolocationMode.USEPROVIDER;
	public static String wundergroundKey = "";
	public static int fontSize = 2;
	public static int smsLoopInterval = 15;
	public static int idleMusicControlMethod = MediaControl.MUSICSERVICECOMMAND;
	public static String quickButtonL = ":launch:org.metawatch.manager.apps.MediaPlayerApp";
	public static String quickButtonR = "lastNotification";
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
	public static boolean autoSpeakerphone = false;
	public static boolean showActionsInCall = true;
	public static String themeName = "";
	public static boolean hideEmptyWidgets = false;
	public static boolean inverseMediaPlayerButtons = false;
	public static boolean clockOnAppScreens = false;
	public static boolean hiddenWidgetsReserveSpace = false;
	public static boolean showTestWidgets = false;
	public static boolean alignWidgetRowToBottom = false;
	public static boolean displayWidgetIconOnTop = true;
	public static String displayCalendars = "";
	public static int calendarLookahead = 24;
	public static boolean animations = true;
	public static boolean smsWithAlert = true;
    }

    public final class WatchType {
	public static final int UNKNOWN = 0;
	public static final int ANALOG = 1;
	public static final int DIGITAL = 2;
    }

    public final class WatchGen {
	public static final int UNKNOWN = 0;
	public static final int GEN1 = 1; // Original dev watches
	public static final int GEN2 = 2; // Strata / Frame
    }

}