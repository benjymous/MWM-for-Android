package org.metawatch.manager.apps;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.FontCache.FontSize;
import org.metawatch.manager.Idle;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.WatchType;
import org.metawatch.manager.Protocol;
import org.metawatch.manager.Utils;
import org.metawatch.manager.actions.Action;
import org.metawatch.manager.actions.ActionManager;
import org.metawatch.manager.actions.ContainerAction;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelXorXfermode;
import android.graphics.Region;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;

@SuppressWarnings("deprecation")
public class ActionsApp extends ApplicationBase {

    public final static String APP_ID = "org.metawatch.manager.apps.ActionsApp";

    static AppData appData = new AppData() {
	{
	    id = APP_ID;
	    name = "Actions";

	    supportsDigital = true;
	    supportsAnalog = true;
	}
    };

    public final static byte ACTION_NEXT = 30;
    public final static byte ACTION_PERFORM = 31;
    public final static byte ACTION_SECONDARY = 32;
    public final static byte ACTION_TOP = 33;

    public AppData getInfo() {
	return appData;
    }

    public boolean isToggleable() {
	// Always provide a way to reach the Actions app (since it's quite
	// central).
	// if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL
	// &&
	// Preferences.quickButton == QuickButton.OPEN_ACTIONS) {
	// // Only set toggleable if Quick Button can open it again.
	// return true;
	// }

	return false;
    }

    Stack<ContainerAction> containerStack = new Stack<ContainerAction>(); // Contains
									  // the
									  // stack
									  // of
									  // ContainerActions
									  // opened
									  // (empty
									  // if
									  // at
									  // root).
    List<Action> currentActions = null; // Contains the list as it's shown on
					// the screen (including a "back" item
					// for sub level lists).
    Action backAction = null;

    Stack<Integer> selectionStack = new Stack<Integer>(); // Only contains
							  // selection for
							  // upper level
							  // lists.
    int currentSelection = 0;

    private void init(final Context context) {
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

	if (currentActions == null) {
	    currentActions = new ArrayList<Action>();
	}
    }

    public void activate(final Context context, int watchType) {
	init(context);

	if (watchType == WatchType.DIGITAL) {
	    Protocol.getInstance(context).enableButton(1, 1, ACTION_NEXT, MetaWatchService.WatchBuffers.APPLICATION); // right
	    // middle
	    // -
	    // press
	    Protocol.getInstance(context).enableButton(1, 2, ACTION_TOP, MetaWatchService.WatchBuffers.APPLICATION); // right
	    // middle
	    // -
	    // hold
	    Protocol.getInstance(context).enableButton(1, 3, ACTION_TOP, MetaWatchService.WatchBuffers.APPLICATION); // right
	    // middle
	    // -
	    // long
	    // hold

	    Protocol.getInstance(context).enableButton(2, 1, ACTION_PERFORM, MetaWatchService.WatchBuffers.APPLICATION); // right
	    // bottom
	    // -
	    // press
	    Protocol.getInstance(context).enableButton(2, 2, ACTION_SECONDARY, MetaWatchService.WatchBuffers.APPLICATION); // right
	    // bottom
	    // -
	    // hold
	    Protocol.getInstance(context).enableButton(2, 3, ACTION_SECONDARY, MetaWatchService.WatchBuffers.APPLICATION); // right
	    // bottom
	    // -
	    // long
	    // hold
	} else if (watchType == WatchType.ANALOG) {
	    Protocol.getInstance(context).enableButton(0, 1, ACTION_NEXT, MetaWatchService.WatchBuffers.APPLICATION); // top
	    // -
	    // press
	    Protocol.getInstance(context).enableButton(0, 2, ACTION_TOP, MetaWatchService.WatchBuffers.APPLICATION); // top
	    // -
	    // hold
	    Protocol.getInstance(context).enableButton(0, 3, ACTION_TOP, MetaWatchService.WatchBuffers.APPLICATION); // top
	    // -
	    // long
	    // hold

	    Protocol.getInstance(context).enableButton(2, 1, ACTION_PERFORM, MetaWatchService.WatchBuffers.APPLICATION); // bottom
	    // -
	    // press
	    Protocol.getInstance(context).enableButton(2, 2, ACTION_SECONDARY, MetaWatchService.WatchBuffers.APPLICATION); // bottom
	    // -
	    // hold
	    Protocol.getInstance(context).enableButton(2, 3, ACTION_SECONDARY, MetaWatchService.WatchBuffers.APPLICATION); // bottom
	    // -
	    // long
	    // hold
	}
    }

    public void deactivate(final Context context, int watchType) {
	// Return to root.
	toRoot();

	if (watchType == WatchType.DIGITAL) {
	    Protocol.getInstance(context).disableButton(1, 1, MetaWatchService.WatchBuffers.APPLICATION);
	    Protocol.getInstance(context).disableButton(2, 1, MetaWatchService.WatchBuffers.APPLICATION);
	    Protocol.getInstance(context).disableButton(2, 2, MetaWatchService.WatchBuffers.APPLICATION);
	    Protocol.getInstance(context).disableButton(2, 3, MetaWatchService.WatchBuffers.APPLICATION);
	} else if (watchType == WatchType.ANALOG) {
	    Protocol.getInstance(context).disableButton(0, 1, MetaWatchService.WatchBuffers.APPLICATION);
	    Protocol.getInstance(context).disableButton(2, 1, MetaWatchService.WatchBuffers.APPLICATION);
	    Protocol.getInstance(context).disableButton(2, 2, MetaWatchService.WatchBuffers.APPLICATION);
	    Protocol.getInstance(context).disableButton(2, 3, MetaWatchService.WatchBuffers.APPLICATION);
	}

    }

    public Bitmap update(final Context context, boolean preview, int watchType) {
	init(context);

	currentActions.clear();
	if (containerStack.isEmpty() || containerStack.peek() == null) {
	    // At the root.
	    currentActions = ActionManager.getInstance(context).getRootActions(context);
	} else {
	    ContainerAction container = containerStack.peek();
	    container.refreshSubActions(context);

	    Action back = container.getBackAction() == null ? backAction : container.getBackAction();

	    // In a ContainerAction.
	    if (watchType == WatchType.DIGITAL)
		currentActions.add(back);

	    currentActions.addAll(container.getSubActions());

	    // Put the back action at the end on Analog, so the first displayed
	    // element actually has content.
	    if (watchType == WatchType.ANALOG)
		currentActions.add(back);
	}

	// Clean away empty actions.
	ListIterator<Action> it = currentActions.listIterator();
	while (it.hasNext()) {
	    Action a = it.next();
	    if (a.isHidden()) {
		it.remove();
	    }
	}

	if (currentSelection >= currentActions.size()) {
	    currentSelection = 0;
	}

	if (watchType == WatchType.DIGITAL) {
	    return drawDigital(context, preview);
	} else if (watchType == WatchType.ANALOG) {
	    return drawAnalog(context, preview);
	}

	return null;
    }

    private Bitmap drawDigital(final Context context, boolean preview) throws IllegalArgumentException {
	TextPaint paint = new TextPaint();
	paint.setColor(Color.BLACK);
	paint.setTextSize(FontCache.instance(context).Get().size);
	paint.setTypeface(FontCache.instance(context).Get().face);
	int textHeight = FontCache.instance(context).Get().realSize;

	Paint paintXor = new Paint();
	paintXor.setXfermode(new PixelXorXfermode(Color.WHITE));

	Paint paintWhite = new Paint();
	paintWhite.setColor(Color.WHITE);

	String headerText = getHeaderText(context);
	StaticLayout headerLayout = null;
	int headerHeight = 0;
	if (headerText != null) {
	    headerLayout = Utils.buildText(context, headerText, 96, Layout.Alignment.ALIGN_NORMAL, Color.BLACK, FontSize.SMALL);
	    headerHeight = headerLayout.getHeight();
	}

	// Double the height to make room for multi line items that trigger
	// scrolling.
	Bitmap bitmap = Bitmap.createBitmap(96, 192, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	final int maxY = 96 - textHeight - headerHeight;
	int y = textHeight + headerHeight + 5; // Make room for a title.

	boolean scrolled = false;
	for (int i = Math.max(0, currentSelection - 96 / textHeight + 4); (i < currentActions.size() && (i <= currentSelection || y <= 96)); i++) {
	    Action a = currentActions.get(i);

	    if (a.bulletIcon() != null) {
		canvas.drawBitmap(Utils.getBitmap(context, a.bulletIcon()), 1, y, null);
	    }

	    if (i == currentSelection) {
		// Draw full multi-line text.
		StringBuilder name = new StringBuilder(a.getName());
		if (a instanceof ContainerAction) {
		    name.append(" (");
		    name.append(((ContainerAction) a).size());
		    name.append(")");
		}

		final StaticLayout layout = new StaticLayout(name, paint, 79, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
		final int height = layout.getHeight();

		final int top = y;

		canvas.save();
		canvas.translate(7, y);
		layout.draw(canvas);
		canvas.restore();

		y += height;

		// Draw timestamp, if any.
		final long timestamp = a.getTimestamp();
		if (timestamp != -1) {
		    final String timetext = (timestamp > 0 ? Utils.ticksToText(context, timestamp, true) : "---");
		    canvas.drawLine(7, y, 86, y, paint);
		    y += 2;
		    canvas.drawText((String) TextUtils.ellipsize(timetext, paint, 79, TruncateAt.START), 7, y + textHeight, paint);
		    y += textHeight + 1;
		}

		// Invert item to mark as selected.
		canvas.drawRect(0, top - 1, 96, y, paintXor);

		// Scroll screen if necessary.
		if (y >= maxY) {
		    final int scroll = y - maxY;
		    bitmap = Bitmap.createBitmap(bitmap, 0, scroll, 96, 96);
		    canvas.setBitmap(bitmap);
		    y -= scroll;

		    if (i == currentActions.size() - 1) {
			// Mark the end of the list.
			Idle.getInstance().drawLine(canvas, 96 - textHeight / 2 - 1);
		    }
		    scrolled = true;
		}

	    } else {
		// Draw elipsized text.
		canvas.drawText((String) TextUtils.ellipsize(a.getName().replace("\n", " "), paint, 79, TruncateAt.END), 7, y + textHeight, paint);
		y += textHeight + 1;
	    }
	}

	// Draw title and header
	if (scrolled) {
	    // Paint white over any scrolled items.
	    canvas.drawRect(0, 0, 95, textHeight + 4 + headerHeight, paintWhite);
	}
	String title = getUiTitle();
	canvas.drawText((String) TextUtils.ellipsize(title, paint, 84, TruncateAt.END), 2, textHeight + 1, paint);

	if (headerLayout != null) {
	    canvas.save();
	    canvas.translate(0, textHeight + 2);
	    headerLayout.draw(canvas);
	    canvas.restore();

	    canvas.drawLine(1, textHeight + headerHeight + 2, 94, textHeight + headerHeight + 2, paint);
	} else {
	    canvas.drawLine(1, textHeight + headerHeight + 2, (isToggleable() ? 79 : 87), textHeight + headerHeight + 2, paint);
	}

	// Draw icons.
	drawDigitalAppSwitchIcon(context, canvas, preview);
	canvas.drawBitmap(Utils.getBitmap(context, "action_down.bmp"), 87, 43, null);

	final int currentType = currentActions == null || currentActions.size() == 0 ? 0 : currentActions.get(currentSelection).getSecondaryType();
	// TODO split the secodary icons to separate files and draw them in
	// addition to the right icon.
	if (currentType == Action.SECONDARY_RESET) {
	    canvas.drawBitmap(Utils.getBitmap(context, "action_reset_right.bmp"), 79, 87, null);
	} else if (currentType == Action.SECONDARY_EXIT) {
	    canvas.drawBitmap(Utils.getBitmap(context, "action_exit_right.bmp"), 79, 87, null);
	} else {
	    canvas.drawBitmap(Utils.getBitmap(context, "action_right.bmp"), 87, 87, null);
	}

	// If the screen hasn't scrolled, the bitmap is too large, shrink it.
	if (!scrolled) {
	    bitmap = Bitmap.createBitmap(bitmap, 0, 0, 96, 96);
	}
	return bitmap;
    }

    private String getUiTitle() {
	return (containerStack == null || containerStack.isEmpty() || containerStack.peek() == null) ? "Actions" : containerStack.peek().getTitle();
    }

    private String getHeaderText(Context context) {
	return (containerStack == null || containerStack.isEmpty() || containerStack.peek() == null) ? null : containerStack.peek().getHeaderText(context);
    }

    private Bitmap drawAnalog(final Context context, boolean preview) {
	TextPaint paint = new TextPaint();
	paint.setColor(Color.BLACK);
	paint.setTextSize(FontCache.instance(context).Small.size);
	paint.setTypeface(FontCache.instance(context).Small.face);

	Paint paintXor = new Paint();
	paintXor.setXfermode(new PixelXorXfermode(Color.WHITE));

	Paint paintWhite = new Paint();
	paintWhite.setColor(Color.WHITE);

	Bitmap bitmap = Bitmap.createBitmap(80, 32, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawColor(Color.WHITE);

	// Top screen
	canvas.clipRect(0, 0, 80, 16, Region.Op.REPLACE);

	String title = getUiTitle();
	StringBuilder position = new StringBuilder();
	if (currentActions.size() > 0) {
	    position.append("(");
	    position.append(currentSelection + 1);
	    position.append("/");
	    position.append(currentActions.size());
	    position.append(")");
	}

	canvas.drawText((String) TextUtils.ellipsize(title, paint, 74, TruncateAt.END), 0, 6, paint);
	canvas.drawText((String) TextUtils.ellipsize(position.toString(), paint, 74, TruncateAt.END), 0, 13, paint);

	canvas.drawBitmap(Utils.getBitmap(context, "action_down_5.bmp"), 75, 11, null);

	// Bottom screen
	canvas.clipRect(0, 16, 80, 32, Region.Op.REPLACE);

	Action a = currentActions.get(currentSelection);
	String itemLine1 = a.getName().replace("\n", " ");
	final long timestamp = a.getTimestamp();
	if (timestamp != -1) {
	    // If item has a timestamp, then display truncated item text plus
	    // timestamp
	    String itemLine2 = (timestamp > 0 ? Utils.ticksToText(context, timestamp, true) : "---");

	    canvas.drawText((String) TextUtils.ellipsize(itemLine1, paint, 74, TruncateAt.END), 0, 21, paint);
	    canvas.drawText((String) TextUtils.ellipsize(itemLine2, paint, 74, TruncateAt.END), 0, 32, paint);
	} else {
	    // Wrap item over multiple lines
	    final StaticLayout layout = new StaticLayout(itemLine1, paint, 74, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);

	    canvas.save();
	    canvas.translate(0, 16);
	    layout.draw(canvas);
	    canvas.restore();
	}

	final int type = a.getSecondaryType();
	// TODO split the secodary icons to separate files and draw them in
	// addition to the right icon.
	if (type == Action.SECONDARY_RESET) {
	    canvas.drawBitmap(Utils.getBitmap(context, "action_reset_right_5.bmp"), 75, 16, null);
	} else if (type == Action.SECONDARY_EXIT) {
	    canvas.drawBitmap(Utils.getBitmap(context, "action_exit_right_5.bmp"), 75, 16, null);
	} else {
	    canvas.drawBitmap(Utils.getBitmap(context, "action_right_5.bmp"), 75, 16, null);
	}

	return bitmap;
    }

    public int buttonPressed(Context context, int id) {
	if (currentActions == null || currentActions.size() == 0) {
	    return BUTTON_NOT_USED;
	}

	if (currentSelection >= currentActions.size()) {
	    currentSelection = 0;
	}

	Action currentAction = currentActions.get(currentSelection);
	switch (id) {
	case ACTION_NEXT:
	    currentSelection = (currentSelection + 1) % currentActions.size();
	    return BUTTON_USED;

	case ACTION_TOP:
	    currentSelection = 0;
	    return BUTTON_USED;

	case ACTION_PERFORM:
	    if (currentAction instanceof ContainerAction) {
		displayContainer((ContainerAction) currentAction);
	    }
	    return currentAction.performAction(context);

	case ACTION_SECONDARY:
	    return currentAction.performSecondary(context);
	}

	return BUTTON_NOT_USED;
    }

    public void displayContainer(ContainerAction container) {
	containerStack.push(container);
	selectionStack.push(currentSelection);
	currentSelection = 0;
    }

    public void toRoot() {
	containerStack.clear();
	selectionStack.clear();
	currentSelection = 0;
    }

}
