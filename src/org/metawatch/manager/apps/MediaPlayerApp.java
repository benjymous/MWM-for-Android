package org.metawatch.manager.apps;

import java.util.ArrayList;
import java.util.List;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.MediaControl;
import org.metawatch.manager.MetaWatch;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchType;
import org.metawatch.manager.Protocol;
import org.metawatch.manager.Utils;
import org.metawatch.manager.actions.Action;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

public class MediaPlayerApp extends ApplicationBase {

	public final static String APP_ID = "org.metawatch.manager.apps.MediaPlayerApp";

	static AppData appData = new AppData() {{
		id = APP_ID;
		name = "Media Player";
	
		supportsDigital = true;
		supportsAnalog = true;
	}};
	
	public final static byte VOLUME_UP = 10;
	public final static byte VOLUME_DOWN = 11;
	public final static byte NEXT = 15;
	public final static byte PREVIOUS = 16;
	public final static byte TOGGLE = 20;
	public final static byte MENU = 21;
	
	public AppData getInfo() {
		return appData;
	}

	public void activate(final Context context, int watchType) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Entering media mode");
		
		if (watchType == WatchType.DIGITAL) {
			Protocol.enableButton(1, 0, TOGGLE, MetaWatchService.WatchBuffers.APPLICATION); // right middle - immediate
			
			Protocol.enableButton(2, 1, MENU, MetaWatchService.WatchBuffers.APPLICATION); // right bottom - press
	
			if (Preferences.inverseMediaPlayerButtons) {
				Protocol.enableButton(5, 1, PREVIOUS, MetaWatchService.WatchBuffers.APPLICATION); // left middle - press
				Protocol.enableButton(5, 2, VOLUME_DOWN, MetaWatchService.WatchBuffers.APPLICATION); // left middle - hold
				Protocol.enableButton(5, 3, VOLUME_DOWN, MetaWatchService.WatchBuffers.APPLICATION); // left middle - long hold

				Protocol.enableButton(6, 1, NEXT, MetaWatchService.WatchBuffers.APPLICATION); // left top - press
				Protocol.enableButton(6, 2, VOLUME_UP, MetaWatchService.WatchBuffers.APPLICATION); // left top - hold
				Protocol.enableButton(6, 3, VOLUME_UP, MetaWatchService.WatchBuffers.APPLICATION); // left top - long hold
				
			} else {
				
				Protocol.enableButton(5, 1, VOLUME_DOWN, MetaWatchService.WatchBuffers.APPLICATION); // left middle - press
				Protocol.enableButton(5, 2, PREVIOUS, MetaWatchService.WatchBuffers.APPLICATION); // left middle - hold
				Protocol.enableButton(5, 3, PREVIOUS, MetaWatchService.WatchBuffers.APPLICATION); // left middle - long hold
				
				Protocol.enableButton(6, 1, VOLUME_UP, MetaWatchService.WatchBuffers.APPLICATION); // left top - press
				Protocol.enableButton(6, 2, NEXT, MetaWatchService.WatchBuffers.APPLICATION); // left top - hold
				Protocol.enableButton(6, 3, NEXT, MetaWatchService.WatchBuffers.APPLICATION); // left top - long hold
			}
			
		}
		else if (watchType == WatchType.ANALOG) {
			Protocol.enableButton(0, 1, TOGGLE, MetaWatchService.WatchBuffers.APPLICATION); // top - press
			Protocol.enableButton(0, 2, MENU, MetaWatchService.WatchBuffers.APPLICATION); // top hold
			Protocol.enableButton(2, 1, NEXT, MetaWatchService.WatchBuffers.APPLICATION); // bottom - press			
		}
	}
	
	public void deactivate(final Context context, int watchType) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Leaving media mode");
		
		if (watchType == WatchType.DIGITAL) {
			Protocol.disableButton(1, 0, MetaWatchService.WatchBuffers.APPLICATION);
			
			Protocol.disableButton(2, 1, MetaWatchService.WatchBuffers.APPLICATION);
			
			Protocol.disableButton(5, 0, MetaWatchService.WatchBuffers.APPLICATION);
			//Protocol.disableButton(5, 1, MetaWatchService.WatchBuffers.APPLICATION);
			Protocol.disableButton(5, 2, MetaWatchService.WatchBuffers.APPLICATION);
			Protocol.disableButton(5, 3, MetaWatchService.WatchBuffers.APPLICATION);
	
			Protocol.disableButton(6, 0, MetaWatchService.WatchBuffers.APPLICATION);
			//Protocol.disableButton(6, 1, MetaWatchService.WatchBuffers.APPLICATION);
			Protocol.disableButton(6, 2, MetaWatchService.WatchBuffers.APPLICATION);
			Protocol.disableButton(6, 3, MetaWatchService.WatchBuffers.APPLICATION);
		}
		else if (watchType == WatchType.ANALOG) {
			Protocol.disableButton(0, 1, MetaWatchService.WatchBuffers.APPLICATION); 
			Protocol.disableButton(0, 2, MetaWatchService.WatchBuffers.APPLICATION);
			Protocol.disableButton(2, 1, MetaWatchService.WatchBuffers.APPLICATION); 				
		}
	}
	
	public Bitmap update(Context context, boolean preview, int watchType) {
		
		TextPaint paintSmall = new TextPaint();
		paintSmall.setColor(Color.BLACK);
		paintSmall.setTextSize(FontCache.instance(context).Small.size);
		paintSmall.setTypeface(FontCache.instance(context).Small.face);
		
		TextPaint paintSmallOutline = new TextPaint();
		paintSmallOutline.setColor(Color.WHITE);
		paintSmallOutline.setTextSize(FontCache.instance(context).Small.size);
		paintSmallOutline.setTypeface(FontCache.instance(context).Small.face);
		
		TextPaint paintLarge = new TextPaint();
		paintLarge.setColor(Color.BLACK);
		paintLarge.setTextSize(FontCache.instance(context).Large.size);
		paintLarge.setTypeface(FontCache.instance(context).Large.face);
		
		TextPaint paintLargeOutline = new TextPaint();
		paintLargeOutline.setColor(Color.WHITE);
		paintLargeOutline.setTextSize(FontCache.instance(context).Large.size);
		paintLargeOutline.setTypeface(FontCache.instance(context).Large.face);
		
		MediaControl.TrackInfo lastTrack = MediaControl.getLastTrack();
		
		if (watchType == WatchType.DIGITAL) {
			
			Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(Color.WHITE);	
			
			if(lastTrack.isEmpty()) {
				if (Preferences.inverseMediaPlayerButtons)
					canvas.drawBitmap(Utils.getBitmap(context, "media_player_idle_button_inversed.png"), 0, 0, null);				
				else
					canvas.drawBitmap(Utils.getBitmap(context, "media_player_idle.png"), 0, 0, null);				
			}
			else {	
				if (Preferences.inverseMediaPlayerButtons)
					canvas.drawBitmap(Utils.getBitmap(context, "media_player_button_inversed.png"), 0, 0, null);				
				else
					canvas.drawBitmap(Utils.getBitmap(context, "media_player.png"), 0, 0, null);				
				
				StringBuilder lowerText = new StringBuilder();
				if(!lastTrack.artist.equals("")) {
					lowerText.append(lastTrack.artist);
				}
				if(!lastTrack.album.equals("")) {
					if(lowerText.length()>0)
						lowerText.append("\n\n");
					lowerText.append(lastTrack.album);
				}

				Utils.autoText(context, canvas, lastTrack.track, 0, 8, 96, 35, Layout.Alignment.ALIGN_CENTER, Color.BLACK);
				Utils.autoText(context, canvas, lowerText.toString(), 0, 54, 96, 35, Layout.Alignment.ALIGN_CENTER, Color.BLACK);
			}
			drawDigitalAppSwitchIcon(context, canvas, preview);
			
			return bitmap;
		}
		else if (watchType == WatchType.ANALOG) {
			Bitmap bitmap = Bitmap.createBitmap(80, 32, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(Color.WHITE);	
	
			if(lastTrack.isEmpty()) {
				canvas.drawBitmap(Utils.getBitmap(context, "media_player_idle_oled.png"), 0, 0, null);				
			}
			else {	
				canvas.drawBitmap(Utils.getBitmap(context, "media_player_oled.png"), 0, 0, null);
								
				TextPaint tp = null;
				if( paintLarge.measureText(lastTrack.track) < 75) {
					tp = paintLarge;
				}
				else {
					tp = paintSmall;
				}
				
				canvas.save();			
				StaticLayout layout = new StaticLayout(lastTrack.track, tp, 75, Layout.Alignment.ALIGN_CENTER, 1.2f, 0, false);
				int height = layout.getHeight();
				int textY = 8 - (height/2);
				if(textY<0) {
					textY=0;
				}
				canvas.translate(0, textY); //position the text
				canvas.clipRect(0,0,75,16);
				layout.draw(canvas);
				canvas.restore();	
				
				canvas.save();			
				StringBuilder lowerText = new StringBuilder();
				if(!lastTrack.artist.equals("")) {
					lowerText.append(lastTrack.artist);
				}
				if(!lastTrack.album.equals("")) {
					if(lowerText.length()>0)
						lowerText.append("\n\n");
					lowerText.append(lastTrack.album);
				}
				
				if( paintLarge.measureText(lowerText.toString()) < 75) {
					tp = paintLarge;
				}
				else {
					tp = paintSmall;
				}
				
				layout = new StaticLayout(lowerText.toString(), tp, 75, Layout.Alignment.ALIGN_CENTER, 1.0f, 0, false);
				height = layout.getHeight();
				textY = 24 - (height/2);
				if(textY<16) {
					textY=16;
				}
				canvas.translate(0, textY); //position the text
				canvas.clipRect(0,0,75,16);
				layout.draw(canvas);
				canvas.restore();	
				
			}
			
			return bitmap;
		}
		
		return null;
	}
	
	protected List<Action> getMenuActions() {
		List<Action> actions = new ArrayList<Action>();
		
		actions.add(new Action() {

			@Override
			public String getName() {
				return "Launch default music player";
			}

			@Override
			public String bulletIcon() {
				return "bullet_square.bmp";
			}

			@Override
			public int performAction(Context context) {
				try {
					Intent intent = new Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(intent);
				}
				catch (ActivityNotFoundException e) {
					return BUTTON_NOT_USED;
				}
				return BUTTON_USED;
			}
			
		});
		
		return actions;
	}

	public int buttonPressed(Context context, int id) {
		switch (id) {

		case MediaPlayerApp.VOLUME_UP:
			MediaControl.volumeUp(context);
			return BUTTON_USED;
		case MediaPlayerApp.VOLUME_DOWN:
			MediaControl.volumeDown(context);
			return BUTTON_USED;
		case MediaPlayerApp.NEXT:
			MediaControl.next(context);
			return BUTTON_USED;
		case MediaPlayerApp.PREVIOUS:
			MediaControl.previous(context);
			return BUTTON_USED;
		case MediaPlayerApp.TOGGLE:
			MediaControl.togglePause(context);
			return BUTTON_USED;
		case MediaPlayerApp.MENU:
			showMenu(context);
			return BUTTON_USED;
		}
		
		return BUTTON_NOT_USED;
	}

}
