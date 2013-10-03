package org.metawatch.manager.actions;

import org.metawatch.manager.Application;
import org.metawatch.manager.Call;
import org.metawatch.manager.Idle;
import org.metawatch.manager.MediaControl;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchStatus;
import org.metawatch.manager.Monitors;
import org.metawatch.manager.Utils;
import org.metawatch.manager.apps.ApplicationBase;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;

public class InternalActions {

    public abstract static class ToggleAction extends Action {
	protected abstract boolean isEnabled();

	public String bulletIcon() {
	    return isEnabled() ? "bullet_circle_open.bmp" : "bullet_circle.bmp";
	}
    }

    public static class PingAction extends ToggleAction {
	Ringtone r = null;
	int volume = -1;
	int ringerMode = 0;

	public static String id = "ping";

	public String getId() {
	    return id;
	}

	public String getName() {
	    return isSilent() ? "Ping phone" : "Stop alarm";
	}

	public int performAction(Context context) {
	    if (isSilent()) {
		Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		AudioManager as = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		volume = as.getStreamVolume(AudioManager.STREAM_RING);
		ringerMode = as.getRingerMode();

		as.setStreamVolume(AudioManager.STREAM_RING, as.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
		as.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
		if (r != null)
		    r.play();
	    } else {
		if (r != null)
		    r.stop();
		r = null;

		AudioManager as = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		as.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
		as.setRingerMode(ringerMode);
	    }
	    return ApplicationBase.BUTTON_USED;
	}

	protected boolean isEnabled() {
	    return !isSilent();
	}

	private boolean isSilent() {
	    return (r == null || r.isPlaying() == false);
	}
    }

    public static class SpeakerphoneAction extends ToggleAction {
	public SpeakerphoneAction(Context context) {
	    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}

	public static String id = "speakerphone";

	public String getId() {
	    return id;
	}

	private AudioManager audioManager = null;

	public String getName() {
	    return isEnabled() ? "Disable speakerphone" : "Enable speakerphone";
	}

	protected boolean isEnabled() {
	    return audioManager != null && audioManager.isSpeakerphoneOn();
	}

	public int performAction(Context context) {
	    MediaControl.getInstance().ToggleSpeakerphone(context);
	    return ApplicationBase.BUTTON_USED;
	}
    }

    public static class ClickerAction extends Action {
	int count = 0;
	long timestamp = 0;

	public static String id = "clicker";

	public String getId() {
	    return id;
	}

	public String getName() {
	    return "Clicker: " + count;
	}

	public String bulletIcon() {
	    return "bullet_circle.bmp";
	}

	public int performAction(Context context) {
	    count++;
	    timestamp = System.currentTimeMillis();

	    return ApplicationBase.BUTTON_USED;
	}

	public int getSecondaryType() {
	    return Action.SECONDARY_RESET;
	}

	public int performSecondary(Context context) {
	    count = 0;
	    timestamp = 0;
	    return ApplicationBase.BUTTON_USED;
	}

	public long getTimestamp() {
	    return timestamp;
	}
    }

    public static class WeatherRefreshAction extends Action {

	public static String id = "weatherRefresh";

	public String getId() {
	    return id;
	}

	public String getName() {
	    return Monitors.getInstance().weatherData.received ? "Refresh Weather" : "Refreshing...";
	}

	public String bulletIcon() {
	    return "bullet_circle.bmp";
	}

	public int performAction(Context context) {
	    Monitors.getInstance().updateWeatherDataForced(context);

	    return ApplicationBase.BUTTON_USED;
	}

	public long getTimestamp() {
	    return Monitors.getInstance().weatherData.timeStamp;
	}
    }

    public static class ToggleWifiAction extends ToggleAction {

	WifiManager wifiMgr = null;

	public ToggleWifiAction(Context context) {
	    wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	}

	public static String id = "toggleWifi";

	public String getId() {
	    return id;
	}

	public String getName() {
	    return isEnabled() ? "Disable Wifi" : "Enable Wifi";
	}

	protected boolean isEnabled() {
	    return wifiMgr != null && wifiMgr.isWifiEnabled();
	}

	public int performAction(final Context context) {
	    if (wifiMgr != null) {
		context.registerReceiver(new WifiReceiver(wifiMgr.isWifiEnabled() ? WifiManager.WIFI_STATE_DISABLED : WifiManager.WIFI_STATE_ENABLED), new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		wifiMgr.setWifiEnabled(!wifiMgr.isWifiEnabled());
	    }
	    return ApplicationBase.BUTTON_USED_DONT_UPDATE;
	}

	private class WifiReceiver extends BroadcastReceiver {
	    private int mLookingFor = WifiManager.WIFI_STATE_ENABLED;
	    public WifiReceiver(int lookingFor) {
		mLookingFor = lookingFor;
	    }
	    @Override
	    public void onReceive(final Context context, Intent intent) {
		int extraWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
		if (extraWifiState == mLookingFor) {
		    switch(MetaWatchService.getWatchMode()) {
		    case APPLICATION:
			Application.refreshCurrentApp(context);
			break;
		    case CALL:
			break;
		    case IDLE:
			Idle.getInstance().updateIdle(context, true);
			break;
		    case NOTIFICATION:
			break;
		    case OFF:
			break;
		    default:
			break;
		    }
		    try {
			context.unregisterReceiver(this);
		    } catch(IllegalArgumentException e){}
		}
	    }
	}
    }

    public static class ToggleSilentAction extends ToggleAction {
	public ToggleSilentAction(Context context) {
	    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
	}

	private AudioManager audioManager = null;

	public static String id = "toggleSilent";

	public String getId() {
	    return id;
	}

	public String getName() {
	    return isEnabled() ? "Disable phone silent mode" : "Enable phone silent mode";
	}

	protected boolean isEnabled() {
	    return audioManager != null && (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT);
	}

	public int performAction(Context context) {
	    audioManager.setRingerMode(audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT ? AudioManager.RINGER_MODE_NORMAL : AudioManager.RINGER_MODE_SILENT);
	    return ApplicationBase.BUTTON_USED;
	}
    }

    public static class ToggleWatchSilentAction extends ToggleAction {
	public static String id = "toggleWatchSilent";

	public String getId() {
	    return id;
	}

	public String getName() {
	    return isEnabled() ? "Disable silent mode" : "Enable silent mode";
	}

	protected boolean isEnabled() {
	    return MetaWatchService.silentMode();
	}

	public int performAction(Context context) {
	    if (!MetaWatchStatus.mShutdownRequested && MetaWatchService.mIsRunning) {
		Intent setSilentMode = new Intent(context, MetaWatchService.class);
		setSilentMode.putExtra(MetaWatchService.COMMAND_KEY, MetaWatchService.INVERT_SILENT_MODE);
		context.startService(setSilentMode);
	    }
	    return ApplicationBase.BUTTON_USED;
	}
    }

    public static class WoodchuckAction extends Action {
	private static final String QUESTION = "How much wood would a woodchuck chuck if a woodchuck could chuck wood?";
	private static final String ANSWER = "A woodchuck could chuck no amount of wood, since a woodchuck can't chuck wood.";
	String name = QUESTION;

	public static String id = "testWoodchuck";

	public String getId() {
	    return id;
	}

	public String getName() {
	    return name;
	}

	public String bulletIcon() {
	    return "bullet_square.bmp";
	}

	public int performAction(Context context) {
	    name = ANSWER;
	    return ApplicationBase.BUTTON_USED;
	}

	public int getSecondaryType() {
	    return Action.SECONDARY_RESET;
	}

	public int performSecondary(Context context) {
	    name = QUESTION;
	    return ApplicationBase.BUTTON_USED;
	}
    }

    public static abstract class AndroidAppAction extends Action {

	protected abstract String getPackage();

	public String bulletIcon() {
	    return "bullet_square.bmp";
	}

	public int performAction(Context context) {
	    try {
		Intent intent = context.getPackageManager().getLaunchIntentForPackage(getPackage());
		if (intent != null) {
		    context.startActivity(intent);
		    return ApplicationBase.BUTTON_USED;
		}
	    } catch (ActivityNotFoundException e) {
	    }

	    return ApplicationBase.BUTTON_NOT_USED;
	}

    }

    public static class MapsAction extends AndroidAppAction {

	protected String getPackage() {
	    return "com.google.android.apps.maps";
	}

	public static String id = "testMaps";

	public String getId() {
	    return id;
	}

	public String getName() {
	    return "Launch Google Maps on phone";
	}

    }

    public static class VoiceSearchAction extends AndroidAppAction {
	Context mContext;
	public VoiceSearchAction(Context context) {
	    mContext = context;
	}
	
	protected String getPackage() {
	    return "com.google.android.voicesearch";
	}

	public static String id = "voiceSearch";

	public String getId() {
	    return id;
	}

	public String getName() {
	    try {
		PackageManager pm = mContext.getPackageManager();
		pm.getApplicationInfo("com.facebook.android", 0 );
		return "Voice Search";
	    } catch( PackageManager.NameNotFoundException e ){
		return "Voice Search not Installed";
	    }
	}
    }

    public static class PhoneSettingsAction extends ContainerAction {

	public static String id = "phoneSettings";

	public String getId() {
	    return id;
	}

	public String getName() {
	    return "Phone Settings";
	}
    }

    public static class PhoneCallAction extends ContainerAction {

	public static String id = "inCall";

	public String getId() {
	    return id;
	}

	public String getName() {
	    return "Phonecall";
	}

	public boolean isHidden() {
	    return !Call.inCall;
	}

	public String getHeaderText(Context context) {
	    StringBuilder sb = new StringBuilder();
	    if (Call.phoneNumber != null) {
		String name = Utils.getContactNameFromNumber(context, Call.phoneNumber);
		if (!name.equals(Call.phoneNumber)) {
		    sb.append(name);
		    sb.append(" ");
		}
		sb.append(Call.phoneNumber);
	    }
	    return sb.toString();
	}
    }
}
