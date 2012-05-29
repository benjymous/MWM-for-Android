package org.metawatch.manager.apps;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.Idle;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.Notification;
import org.metawatch.manager.Protocol;
import org.metawatch.manager.Notification.NotificationType;
import org.metawatch.manager.actions.Action;
import org.metawatch.manager.actions.ContainerAction;
import org.metawatch.manager.actions.HidableAction;
import org.metawatch.manager.actions.InternalActions;
import org.metawatch.manager.actions.ResettableAction;
import org.metawatch.manager.actions.TimestampAction;
import org.metawatch.manager.Utils;
import org.metawatch.manager.MetaWatchService.WatchType;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelXorXfermode;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;

public class ActionsApp extends InternalApp {
	
	public final static String APP_ID = "org.metawatch.manager.apps.ActionsApp";
	
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
	
	public static class NotificationsAction extends ContainerAction implements TimestampAction {
		public String getName() {
			return "Recent Notifications";
		}
		
		public String getTitle() {
			return "Notifications";
		}

		public long getTimestamp() {
			if (subActions.size() > 0) {
				return ((TimestampAction)subActions.get(0)).getTimestamp();
			} else {
				return 0;
			}
		}
		
	}
	
	public AppData getInfo() {
		return appData;
	}
	
	List<Action> internalActions = null;
	Stack<ContainerAction> containerStack = new Stack<ContainerAction>(); //Contains the stack of ContainerActions opened (empty if at root).
	List<Action> currentActions = null; //Contains the list as it's shown on the screen (including a "back" item for sub level lists).
	Action backAction = null;
	NotificationsAction notificationsAction = null;
	
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
					if (!containerStack.isEmpty()) {
						containerStack.pop();
						currentSelection = selectionStack.pop();
					}
					return BUTTON_USED;
				}
			};
		}
		if (notificationsAction == null) {
			notificationsAction = new NotificationsAction();
		}
		
		if (internalActions == null) {
			internalActions = new ArrayList<Action>();

			internalActions.add(new InternalActions.PingAction());
			internalActions.add(new InternalActions.SpeakerphoneAction());
			internalActions.add(new InternalActions.ClickerAction());
			//internalActions.add(new InternalActions.MapsAction());
			//internalActions.add(new InternalActions.WoodchuckAction());
			
			/*
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
		
		if (currentActions == null) {
			currentActions = new ArrayList<Action>();
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
		if (!containerStack.isEmpty()) {
			//Return to root.
			containerStack.clear();
			while(selectionStack.size() > 1)
				selectionStack.pop();
			currentSelection = selectionStack.pop();
		}
		
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
		
		Paint paintWhite = new Paint();
		paintWhite.setColor(Color.WHITE);
		
		// This is not the nicest solution, but it keeps the display updated.
		if (containerStack.isEmpty() ||
				containerStack.peek() == notificationsAction.getSubActions()) {
			List<Action> notifications = notificationsAction.getSubActions();
			notifications.clear();
			notifications.addAll(getNotificationActions());
		}

		currentActions.clear();
		if (containerStack.isEmpty()) {
			// At the root.
			//TODO Add external actions using intents, similar to widgets.
			currentActions.add(notificationsAction);
			currentActions.addAll(getAppActions());
			currentActions.addAll(internalActions);
		} else {
			// In a ContainerAction.
			currentActions.add(backAction);
			currentActions.addAll(containerStack.peek().getSubActions());
		}
		
		// Clean away empty actions.
		ListIterator<Action> it = currentActions.listIterator();
		while (it.hasNext()) {
			Action a = it.next();
			if (a instanceof HidableAction && ((HidableAction)a).isHidden()) {
				it.remove();
			}
		}
		
		if (currentSelection >= currentActions.size()) {
			currentSelection = 0;
		}
		
		if (watchType == WatchType.DIGITAL) {
			// Double the height to make room for multi line items that trigger scrolling.
			Bitmap bitmap = Bitmap.createBitmap(96, 192, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(Color.WHITE);
			
			final int maxY = 96 - textHeight;
			int y = 1;
			if (!containerStack.isEmpty()) {
				y += textHeight + 4; //Make room for a title.
			}

			boolean scrolled = false;
			for (int i = Math.max(0, currentSelection - 96/textHeight + (containerStack.isEmpty() ? 3 : 4));
					(i < currentActions.size() && (i <= currentSelection || y <= 96));
					i++) {
				Action a = currentActions.get(i);
				
				if (a.bulletIcon() != null) {
					canvas.drawBitmap(Utils.loadBitmapFromAssets(context, a.bulletIcon()), 1, y, null);
				}
				
				if (i == currentSelection) {
					// Draw full multi-line text.
					StringBuilder name = new StringBuilder(a.getName());
					if (a instanceof ContainerAction) {
						name.append(" (");
						name.append(((ContainerAction)a).size());
						name.append(")");
					}
					
					final StaticLayout layout = new StaticLayout(name, paint, 79, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
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
					if (y >= maxY) {
						final int scroll = y - maxY;
						bitmap = Bitmap.createBitmap(bitmap, 0, scroll, 96, 96);
						canvas.setBitmap(bitmap);
						y -= scroll;
						
						if (i == currentActions.size() - 1) {
							// Mark the end of the list.
							Idle.drawLine(canvas, 96 - textHeight/2 - 1);
						}
						scrolled = true;
					}
								
				} else {
					//Draw elipsized text.
					canvas.drawText((String) TextUtils.ellipsize(a.getName(), paint, 79, TruncateAt.END), 7, y+textHeight, paint);
					y+= textHeight+1;
				}
			}
			
			if (!containerStack.isEmpty()) {
				// Draw title.
				if (scrolled) {
					// Paint white over any scrolled items.
					canvas.drawRect(0, 0, 95, textHeight+4, paintWhite);
				}
				canvas.drawText((String) TextUtils.ellipsize(containerStack.peek().getTitle(), paint, 84, TruncateAt.END), 2, textHeight+1, paint);
				canvas.drawLine(1, textHeight+2, 86, textHeight+2, paint);
			}
			
			// Draw icons.
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
		
		Action currentAction = currentActions.get(currentSelection);
		switch (id) {
		case ACTION_NEXT:
			currentSelection = (currentSelection+1)%currentActions.size();
			return BUTTON_USED;
			
		case ACTION_TOP:
			currentSelection = 0;
			return BUTTON_USED;
			
		case ACTION_PERFORM:
			if (currentAction instanceof ContainerAction) {
				containerStack.push((ContainerAction)currentAction);
				selectionStack.push(currentSelection);
				currentSelection = 0;
				
				return BUTTON_USED;
				
			} else {
				return currentAction.performAction(context);
			}
			
		case ACTION_RESET:
			if (currentAction instanceof ResettableAction)
				return ((ResettableAction)currentAction).performReset(context);
			else
				return BUTTON_NOT_USED;
		}
		
		
		return BUTTON_NOT_USED;
	}

}
