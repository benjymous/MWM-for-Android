                                                                     
                                                                     
                                                                     
                                             
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
  * MediaControl.java                                                         *
  * MediaControl                                                              *
  * Volume control and vanilla Android player control via intents             *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchType;
import org.metawatch.manager.Notification.VibratePattern;
import org.metawatch.manager.apps.AppManager;
import org.metawatch.manager.apps.InternalApp;
import org.metawatch.manager.apps.MediaPlayerApp;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

public class MediaControl {
	
	final static int MUSICSERVICECOMMAND = 0;
	final static int EMULATE_HEADSET = 1;
	
	public static String lastArtist = "";
	public static String lastAlbum = "";
	public static String lastTrack = "";
	
	public static void next(Context context) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "MediaControl.next()");
		if (Preferences.idleMusicControlMethod == MediaControl.MUSICSERVICECOMMAND){
			context.sendBroadcast(new Intent("com.android.music.musicservicecommand.next"));
		}
		else {
			sendMediaButtonEvent(context, KeyEvent.KEYCODE_MEDIA_NEXT);
		}
	}
	
	public static void previous(Context context) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "MediaControl.previous()");
		if (Preferences.idleMusicControlMethod == MediaControl.MUSICSERVICECOMMAND){
			context.sendBroadcast(new Intent("com.android.music.musicservicecommand.previous"));
		}
		else {
			sendMediaButtonEvent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
		}
	}
	
	public static void togglePause(Context context) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "MediaControl.togglePause()");
		if (Preferences.idleMusicControlMethod == MediaControl.MUSICSERVICECOMMAND){
			context.sendBroadcast(new Intent("com.android.music.musicservicecommand.togglepause"));
		}
		else {
			sendMediaButtonEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
		}
		
		if (MetaWatchService.watchType == WatchType.ANALOG) {
			Idle.sendOledIdle(context);
		}
	}

	public static void AnswerCall(Context context) {
		sendMediaButtonEvent(context, KeyEvent.KEYCODE_HEADSETHOOK, "android.permission.CALL_PRIVILEGED");
	}

	public static void DismissCall(Context context) {
		sendMediaButtonEvent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
	}

	public static void ToggleSpeakerphone(Context context) {
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.setMode(AudioManager.MODE_IN_CALL);
		audioManager.setSpeakerphoneOn(!audioManager.isSpeakerphoneOn());
	}

	public static void volumeDown(Context context) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "MediaControl.volumeDown()");
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
	}
	
	public static void volumeUp(Context context) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "MediaControl.volumeUp()");
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
	}
	

	private static void sendMediaButtonEvent(Context context, int keyCode)
	{
		sendMediaButtonEvent(context, keyCode, null);
	}
	
	private static void sendMediaButtonEvent(Context context, int keyCode, String permission)
	{
		long time = SystemClock.uptimeMillis();
		Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
		KeyEvent downEvent = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0);
		downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
		context.sendOrderedBroadcast(downIntent, permission);
		Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
		KeyEvent upEvent = new KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0);
		upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
		context.sendOrderedBroadcast(upIntent, permission);
	}
	
	public static void updateNowPlaying(Context context, String artist, String album, String track, String sender) {

		/* Ignore if track info hasn't changed. */
		if (artist.equals(MediaControl.lastArtist) && track.equals(MediaControl.lastTrack) && album.equals(MediaControl.lastAlbum)) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "updateNowPlaying(): Track info hasn't changed, ignoring");
			return;
		}
		MediaControl.lastArtist = artist;
		MediaControl.lastTrack = track;
		MediaControl.lastAlbum = album;
		
		int mediaPlayerState = AppManager.getAppState(MediaPlayerApp.APP_ID);
		if (mediaPlayerState == InternalApp.ACTIVE_IDLE)
			Idle.updateIdle(context, true);
		else if (mediaPlayerState == InternalApp.ACTIVE_STANDALONE)
			Application.updateAppMode(context);
		
		if (!MetaWatchService.Preferences.notifyMusic)
			return;
		
		if(mediaPlayerState != InternalApp.INACTIVE) {
			VibratePattern vibratePattern = NotificationBuilder.createVibratePatternFromPreference(context, "settingsMusicNumberBuzzes");				
			
			if (vibratePattern.vibrate)
				Protocol.vibrate(vibratePattern.on,
						vibratePattern.off,
						vibratePattern.cycles);
			
			if (MetaWatchService.watchType == WatchType.DIGITAL) {
				if (Preferences.notifyLight)
					Protocol.ledChange(true);
			}
			else if (MetaWatchService.watchType == WatchType.ANALOG) {
				if(mediaPlayerState == InternalApp.ACTIVE_IDLE)
					Idle.sendOledIdle(context);
				//else if (mediaPlayerState == InternalApp.ACTIVE_STANDALONE)
				//	FIXME ...
			}
			
		}
		else {
			if (sender.equals("com.nullsoft.winamp.metachanged")) {
				NotificationBuilder.createWinamp(context, artist, track, album);				
			} else {
				NotificationBuilder.createMusic(context, artist, track, album);
			}
		}

	}
}
