package org.metawatch.manager.widgets;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.metawatch.manager.Idle;
import org.metawatch.manager.MetaWatchStatus;
import org.metawatch.manager.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.FileObserver;
import org.metawatch.manager.Log;

public class PictureWidget implements InternalWidget {

    private static class Observer extends FileObserver {

	PictureWidget parent;
	Context context;

	public Observer(String path, Context context, PictureWidget widget) {
	    super(path, FileObserver.MODIFY);
	    parent = widget;
	    this.context = context;
	}

	public void onEvent(int event, String path) {
	    synchronized (this) {
		Log.d(MetaWatchStatus.TAG, "Pictures updated!");
		parent.loadPictures();
		Idle.getInstance().updateIdle(context, true);
	    }
	}
    }

    File searchDir;
    FileObserver observer;

    Map<String, Bitmap> pictures = null;
    Map<String, String> descriptions = null;

    private void loadPictures() {
	pictures = new HashMap<String, Bitmap>();
	descriptions = new HashMap<String, String>();

	if (searchDir == null)
	    return;

	File[] imageFiles = searchDir.listFiles();
	if (imageFiles == null)
	    return;

	for (File file : imageFiles) {
	    Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
	    if (bmp != null) {
		String id = "pictureWidget_" + file.getName();
		String desc = Utils.removeExtension(file.getName()) + " (" + bmp.getWidth() + "x" + bmp.getHeight() + ")";
		pictures.put(id, bmp);
		descriptions.put(id, desc);
	    }
	}
    }

    public void init(Context context, ArrayList<CharSequence> widgetIds) {
	// Ensure the images folder exists on the SD card
	searchDir = Utils.getExternalFilesDir(context, "PictureWidget");

	if (searchDir != null) {
	    loadPictures();

	    observer = new Observer(searchDir.getAbsolutePath(), context, this);
	    observer.startWatching();
	}
    }

    public void shutdown() {
	pictures = null;
	descriptions = null;

	if (observer != null) {
	    observer.stopWatching();
	    observer = null;
	}
    }

    public void refresh(ArrayList<CharSequence> widgetIds) {
	if (widgetIds == null) {
	    loadPictures();
	}
    }

    public void get(ArrayList<CharSequence> widgetIds, Map<String, WidgetData> result) {

	if (pictures == null)
	    return;

	Iterator<Entry<String, Bitmap>> it = pictures.entrySet().iterator();
	while (it.hasNext()) {
	    Map.Entry<String, Bitmap> pairs = it.next();

	    if (widgetIds == null || widgetIds.contains(pairs.getKey())) {
		InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

		widget.id = (String) pairs.getKey();
		widget.bitmap = (Bitmap) pairs.getValue();
		widget.description = descriptions.get(pairs.getKey());
		widget.width = widget.bitmap.getWidth();
		widget.height = widget.bitmap.getHeight();
		widget.priority = 1;

		result.put(widget.id, widget);
	    }
	}

    }

}
