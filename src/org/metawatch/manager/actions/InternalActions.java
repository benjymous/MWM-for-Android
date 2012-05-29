package org.metawatch.manager.actions;

import org.metawatch.manager.MediaControl;
import org.metawatch.manager.apps.InternalApp;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

public class InternalActions {
	
	public static class PingAction implements Action {
		Ringtone r = null;
		int volume = -1;
		
		public String getName() {
			if( r==null || r.isPlaying() == false ) {
				return "Ping phone";
			}
			else {
				return "Stop alarm";
			}
		}
		
		public String bulletIcon() {
			return "bullet_circle.bmp";
		}
	
		public int performAction(Context context) {
			if(r==null || r.isPlaying() == false ) {
				Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
				AudioManager as = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
				volume = as.getStreamVolume(AudioManager.STREAM_RING);
				as.setStreamVolume(AudioManager.STREAM_RING, as.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
				r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
				r.play();
			}
			else {
				r.stop();
				r = null;
				
				AudioManager as = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
				as.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
			}
			return InternalApp.BUTTON_USED;
		}
	}

	public static class SpeakerphoneAction implements Action {
		public String getName() {
			return "Toggle Speakerphone";
		}
		
		public String bulletIcon() {
			return "bullet_circle.bmp";
		}

		public int performAction(Context context) {
			MediaControl.ToggleSpeakerphone(context);
			return InternalApp.BUTTON_USED;
		}
	}
	
	public static class ClickerAction implements ResettableAction, TimestampAction {
		int count = 0;
		long timestamp = 0;
		
		public String getName() {
			return "Clicker: "+count;
		}
		
		public long getTimestamp() {
			return timestamp;
		} 
		
		public String bulletIcon() {
			return "bullet_circle.bmp";
		}
	
		public int performAction(Context context) {
			count++;
			timestamp = System.currentTimeMillis();
			
			return InternalApp.BUTTON_USED;
		}
	
		public int performReset(Context context) {
			count = 0;
			timestamp = 0;
			return InternalApp.BUTTON_USED;
		}
	}

	public static class WoodchuckAction implements ResettableAction {
		private static final String QUESTION = "How much wood would a woodchuck chuck if a woodchuck could chuck wood?";
		private static final String ANSWER = "A woodchuck could chuck no amount of wood, since a woodchuck can't chuck wood.";
		String name = QUESTION;
		
		public String getName() {
			return name;
		}
		
		public String bulletIcon() {
			return "bullet_square.bmp";
		}

		public int performAction(Context context) {
			name = ANSWER;
			return InternalApp.BUTTON_USED;
		}

		public int performReset(Context context) {
			name = QUESTION;
			return InternalApp.BUTTON_USED;
		}
	}
	
	public static class MapsAction implements Action {
		public String getName() {
			return "Launch Google Maps on phone";
		}
		
		public String bulletIcon() {
			return "bullet_square.bmp";
		}

		public int performAction(Context context) {					
			Intent mapsIntent = context.getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
			context.startActivity(mapsIntent);
			return InternalApp.BUTTON_USED;
		}
	}
}
