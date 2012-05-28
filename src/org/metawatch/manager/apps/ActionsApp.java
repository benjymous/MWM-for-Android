package org.metawatch.manager.apps;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.Idle;
import org.metawatch.manager.MediaControl;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.Notification;
import org.metawatch.manager.Protocol;
import org.metawatch.manager.Notification.NotificationType;
import org.metawatch.manager.Utils;
import org.metawatch.manager.MetaWatchService.WatchType;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelXorXfermode;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;

public class ActionsApp extends InternalApp {
	
	public final static String APP_ID = "org.metawatch.manager.apps.ActionsApp";

	public interface Action {
		public String getName();
		public String bulletIcon();
		public int performAction(Context context);
	}
	
	public interface ResettableAction extends Action {
		public int performReset(Context context);
	}
	
	public interface TimestampAction extends Action {
		public long getTimestamp();
	}
	
	// Only needed for the anonymous classes below.
	public interface ResettableTimestampAction extends ResettableAction, TimestampAction {}
	
	public abstract class ContainerAction implements Action {
		List<Action> subActions = new ArrayList<Action>();
		
		public String bulletIcon() {
			return "bullet_plus.bmp"; //plus
		}
		
		public int performAction(Context context) {
			actionStack.push(subActions);
			selectionStack.push(currentSelection);
			
			currentSelection = 0;
			
			return BUTTON_USED;
		}
	}
	
	// ------------------------------------------------------------------------
	
	static AppData appData = new AppData() {{
		id = APP_ID;
		name = "Actions";
	
		supportsDigital = true;
		//supportsAnalog = true;
		
		toggleable = false;
	}};
	
	
	public final static byte ACTION_NEXT = 30;
	public final static byte ACTION_PERFORM = 31;
	public final static byte ACTION_RESET = 32;
	public final static byte ACTION_TOP = 33;
	
	public AppData getInfo() {
		return appData;
	}
	
	List<Action> internalActions = null;
	List<Action> rootActions = null;
	Stack<List<Action>> actionStack = new Stack<List<Action>>(); //Contains the upper levels and current level lists (excluding "back" items).
	List<Action> currentActions = null; //Contains the list as it's shown on the screen (including a "back" item for sub level lists).
	Action backAction = null;
	ContainerAction notificationAction = null;
	
	Stack<Integer> selectionStack = new Stack<Integer>(); //Only contains selection for upper level lists.
	int currentSelection = 0;
	
	private List<Action> getAppActions() {
		List<Action> list = new ArrayList<Action>();
		
		for (final AppData a : AppManager.getAppInfos()) {
			if (a.id.equals(APP_ID))
				continue; // Skip self.
			
			if (Idle.getAppPage(a.id) != -1) {
				continue; // Skip apps running as idle pages.
			}
			
			int watchType = MetaWatchService.watchType;
			if ((watchType == MetaWatchService.WatchType.ANALOG && !a.supportsAnalog) ||
					(watchType == MetaWatchService.WatchType.DIGITAL && !a.supportsDigital))
				continue; // Skip unsupported apps.
				
			
			list.add(new Action() {
				public String getName() {
					return a.name;
				}

				public String bulletIcon() {
					return "bullet_square.bmp";
				}

				public int performAction(Context context) {
					AppManager.getApp(a.id).open(context);
					return BUTTON_USED;
				}
			});
		}
		
		return list;
	}

	private List<Action> getNotificationActions() {
		List<Action> list = new ArrayList<Action>();
		for(final NotificationType n : Notification.history()) {
			list.add(new TimestampAction() {
				public String getName() {
					return n.description;
				}
				
				public long getTimestamp() {
					return n.timestamp;
				} 
				
				public String bulletIcon() {
					return "bullet_triangle.bmp";
				}
	
				public int performAction(Context context) {
					Notification.replay(context, n);
					// DONT_UPDATE since the idle screen overwrites the notification otherwise.
					return BUTTON_USED_DONT_UPDATE;
				}
			});
		}
		
		return list;
	}
	
	private void init() {
		if (backAction == null) {
			backAction = new Action() {
				public String getName() {
					return "-- Back --";
				}
				
				public String bulletIcon() {
					return null;
				}
				
				public int performAction(Context context) {
					if (actionStack.size() > 1) {
						currentActions = actionStack.pop();
						currentSelection = selectionStack.pop();
					}
					return BUTTON_USED;
				}
			};
		}
		if (notificationAction == null) {
			notificationAction = new ContainerAction() {
				
				public String getName() {
					return "Recent Notifications";
				}
			};
		}
		
		if (internalActions == null) {
			internalActions = new ArrayList<Action>();
			
			internalActions.add(new ResettableTimestampAction() {

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
					return BUTTON_USED;
				}

				public int performReset(Context context) {
					count = 0;
					timestamp = 0;
					return BUTTON_USED;
				}
			});
			
			internalActions.add(new Action() {
				
				public String getName() {
					return "Toggle Speakerphone";
				}
				
				public String bulletIcon() {
					return "bullet_circle.bmp";
				}

				public int performAction(Context context) {
					MediaControl.ToggleSpeakerphone(context);
					return BUTTON_USED;
				}
			});
			
			internalActions.add(new Action() {
				
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
					return BUTTON_USED;
				}
			});	
			
			/*
			internalActions.add(new ResettableAction() {
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
					return BUTTON_USED;
				}

				public int performReset(Context context) {
					name = QUESTION;
					return BUTTON_USED;
				}
			});	
			
			internalActions.add(new Action() {
				
				public String getName() {
					return "Launch Google Maps on phone";
				}
				
				public String bulletIcon() {
					return "bullet_square.bmp";
				}

				public int performAction(Context context) {					
					Intent LaunchIntent = context.getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");
					context.startActivity( LaunchIntent );
					return BUTTON_USED;
				}
			});
			
			// For scroll testing.
			for (int i = 0; i < 12; i++) {
				final int f = i;
				internalActions.add(new Action() {
					public String getName() {
						return String.valueOf(f);
					}

					public String bulletIcon() {
						return "bullet_triangle.bmp";
					}

					public int performAction(Context context) {
						return BUTTON_USED;
					}
				});
			}
			*/
		}
		
		if (rootActions == null) {
			rootActions = new ArrayList<Action>();
		}
		
		if (actionStack.empty()) {
			actionStack.push(rootActions);
		}
	}
	
	public void activate(int watchType) {
		init();
		
		if (watchType == WatchType.DIGITAL) {
			Protocol.enableButton(1, 1, ACTION_NEXT, MetaWatchService.WatchBuffers.APPLICATION); // right middle - press
			Protocol.enableButton(1, 2, ACTION_TOP, MetaWatchService.WatchBuffers.APPLICATION); // right middle - hold
			Protocol.enableButton(1, 3, ACTION_TOP, MetaWatchService.WatchBuffers.APPLICATION); // right middle - long hold
			
			Protocol.enableButton(2, 1, ACTION_PERFORM, MetaWatchService.WatchBuffers.APPLICATION); // right bottom - press
			Protocol.enableButton(2, 2, ACTION_RESET, MetaWatchService.WatchBuffers.APPLICATION); // right bottom - hold
			Protocol.enableButton(2, 3, ACTION_RESET, MetaWatchService.WatchBuffers.APPLICATION); // right bottom - long hold
		}
		else if (watchType == WatchType.ANALOG) {
			Protocol.enableButton(0, 1, ACTION_NEXT, MetaWatchService.WatchBuffers.APPLICATION); // top - press
			Protocol.enableButton(0, 2, ACTION_TOP, MetaWatchService.WatchBuffers.APPLICATION); // top - hold
			Protocol.enableButton(0, 3, ACTION_TOP, MetaWatchService.WatchBuffers.APPLICATION); // top - long hold
			
			Protocol.enableButton(2, 1, ACTION_PERFORM, MetaWatchService.WatchBuffers.APPLICATION); // bottom - press
			Protocol.enableButton(2, 2, ACTION_RESET, MetaWatchService.WatchBuffers.APPLICATION); // bottom - hold
			Protocol.enableButton(2, 3, ACTION_RESET, MetaWatchService.WatchBuffers.APPLICATION); // bottom - long hold
		}
	}

	public void deactivate(int watchType) {
		if (watchType == WatchType.DIGITAL) {
			Protocol.disableButton(1, 1, MetaWatchService.WatchBuffers.APPLICATION);
			Protocol.disableButton(2, 1, MetaWatchService.WatchBuffers.APPLICATION);
			Protocol.disableButton(2, 2, MetaWatchService.WatchBuffers.APPLICATION);
			Protocol.disableButton(2, 3, MetaWatchService.WatchBuffers.APPLICATION);
		}
		else if (watchType == WatchType.ANALOG) {
			Protocol.disableButton(0, 1, MetaWatchService.WatchBuffers.APPLICATION);
			Protocol.disableButton(2, 1, MetaWatchService.WatchBuffers.APPLICATION);
			Protocol.disableButton(2, 2, MetaWatchService.WatchBuffers.APPLICATION);
			Protocol.disableButton(2, 3, MetaWatchService.WatchBuffers.APPLICATION);
		}
		
	}

	public Bitmap update(final Context context, boolean preview, int watchType) {
		init();
		
		TextPaint paint = new TextPaint();
		paint.setColor(Color.BLACK);
		paint.setTextSize(FontCache.instance(context).Get().size);
		paint.setTypeface(FontCache.instance(context).Get().face);
		int textHeight = FontCache.instance(context).Get().realSize;
		
		Paint paintXor = new Paint();
		paintXor.setXfermode(new PixelXorXfermode(Color.WHITE));
		
		// This is not the nicest solution, but it keeps the lists updated when shown.
		if (actionStack.peek() == rootActions) {
			rootActions.clear();

			//TODO Add external actions using intents, similar to widgets.
			rootActions.add(notificationAction);
			rootActions.addAll(getAppActions());
			rootActions.addAll(internalActions);
			
		} else if (actionStack.peek() == notificationAction.subActions) {
			notificationAction.subActions.clear();
			notificationAction.subActions.addAll(getNotificationActions());
		}
		
		currentActions = new ArrayList<Action>();
		if (actionStack.size() > 1) {
			currentActions.add(backAction);
		}
		currentActions.addAll(actionStack.peek());
		
		if (currentSelection >= currentActions.size()) {
			currentSelection = 0;
		}
		
		if (watchType == WatchType.DIGITAL) {
			// Double the height to make room for multi line items that trigger scrolling.
			Bitmap bitmap = Bitmap.createBitmap(96, 192, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(Color.WHITE);	
			
			int y=1;
			boolean scrolled = false;
			
			for (int i = Math.max(0, currentSelection - 96/textHeight + 3);
					(i < currentActions.size() && (i <= currentSelection || y <= 96));
					i++) {
				Action a = currentActions.get(i);
				
				if (a.bulletIcon() != null) {
					canvas.drawBitmap(Utils.loadBitmapFromAssets(context, a.bulletIcon()), 1, y, null);
				}
				
				if(i==currentSelection) {
					// Draw full multi-line text.
					final StaticLayout layout = new StaticLayout(a.getName(), paint, 79, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
					final int height = layout.getHeight();
					
					final int top = y;
					
					canvas.save();		
					canvas.translate(7, y);
					layout.draw(canvas);
					canvas.restore();
				
					y+= height;
					
					// Draw timestamp, if any.
					if(a instanceof TimestampAction) {
						final long timestamp = ((TimestampAction)a).getTimestamp();
						final String timetext = (timestamp > 0 ?
								Utils.ticksToText(context, timestamp, true) :
								"---");
						canvas.drawLine(7, y, 86, y, paint);
						y+= 2;
						canvas.drawText((String) TextUtils.ellipsize(timetext, paint, 79, TruncateAt.START), 7, y+textHeight, paint);
						y+= textHeight+1;
					}
	
					// Invert item to mark as selected.
					canvas.drawRect(0, top-1, 96, y, paintXor);
					
					// Scroll screen if necessary.
					final int maxY = 96 - textHeight;
					if (y > maxY) {
						final int scroll = y - maxY;
						bitmap = Bitmap.createBitmap(bitmap, 0, scroll, 96, 96);
						canvas.setBitmap(bitmap);
						y -= scroll;
						
						scrolled = true;

						if (i == currentActions.size() - 1) {
							// Mark the end of the list.
							Idle.drawLine(canvas, 96 - textHeight/2 - 1);
						}
					}
								
				}
				else {
					//Draw elipsized text.
					canvas.drawText((String) TextUtils.ellipsize(a.getName(), paint, 79, TruncateAt.END), 7, y+textHeight, paint);
					y+= textHeight+1;
				}
			}			
			
			canvas.drawBitmap(getAppSwitchIcon(context, preview), 87, 0, null);
			canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "action_down.bmp"), 87, 43, null);
			if (currentActions.get(currentSelection) instanceof ResettableAction) {
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "action_reset_right.bmp"), 79, 87, null);
			} else {
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "action_right.bmp"), 87, 87, null);
			}
			
			// If the screen hasn't scrolled, the bitmap is too large, shrink it.
			if (!scrolled) {
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, 96, 96);
			}
			
			return bitmap;
			
		} else if (watchType == WatchType.ANALOG) {
			Bitmap bitmap = Bitmap.createBitmap(80, 32, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(Color.WHITE);
			
			//FIXME ...
			
			return bitmap;
		}
		
		return null;
	}

	public int buttonPressed(Context context, int id) {
		if(currentActions==null) {
			return BUTTON_NOT_USED;
		}
		
		if (currentSelection >= currentActions.size()) {
			currentSelection = 0;
		}
		
		switch (id) {
		case ACTION_NEXT:
			currentSelection = (currentSelection+1)%currentActions.size();
			return BUTTON_USED;
			
		case ACTION_TOP:
			currentSelection = 0;
			return BUTTON_USED;
			
		case ACTION_PERFORM:
			return currentActions.get(currentSelection).performAction(context);
			
		case ACTION_RESET:
			if (currentActions.get(currentSelection) instanceof ResettableAction)
				return ((ResettableAction)currentActions.get(currentSelection)).performReset(context);
			else
				return BUTTON_NOT_USED;
		}
		
		
		return BUTTON_NOT_USED;
	}

}
