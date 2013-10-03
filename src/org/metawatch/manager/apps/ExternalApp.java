package org.metawatch.manager.apps;

import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchStatus;
import org.metawatch.manager.Protocol;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import org.metawatch.manager.Log;

public class ExternalApp extends ApplicationBase {

    Bitmap buffer = null;
    AppData appData = null;
    boolean receivedData = false;

    public ExternalApp(final String appId, final String appName) {

	appData = new AppData() {
	    {
		id = appId;
		name = appName;

		supportsDigital = true;
		supportsAnalog = false;
	    }
	};
    }

    @Override
    public AppData getInfo() {
	return appData;
    }

    @Override
    public void activate(Context context, int watchType) {
	if (appData != null) {
	    Intent intent = new Intent("org.metawatch.manager.APPLICATION_ACTIVATE");
	    Bundle b = new Bundle();
	    b.putString("id", appData.id);
	    intent.putExtras(b);
	    context.sendBroadcast(intent);

	    for (int button = 1; button < 6; ++button) {
		if (button == 3)
		    continue; // don't override the LED button
		for (int type = 0; type < 4; ++type) {
		    int code = 200 + ((button - 1) * 4) + type;
		    int actualButton = button;
		    // Gen2 watches have LED on top left rather than bottom left
		    // button
		    if (MetaWatchService.watchGen == MetaWatchService.WatchGen.GEN2) {
			if (actualButton > 3)
			    actualButton++;
		    } else {
			if (actualButton > 4)
			    actualButton++;
		    }

		    Protocol.getInstance(context).enableButton(actualButton, type, code, MetaWatchService.WatchBuffers.APPLICATION);
		}
	    }

	}
    }

    @Override
    public void deactivate(Context context, int watchType) {
	if (appData != null) {
	    Intent intent = new Intent("org.metawatch.manager.APPLICATION_DEACTIVATE");
	    Bundle b = new Bundle();
	    b.putString("id", appData.id);
	    intent.putExtras(b);
	    context.sendBroadcast(intent);

	    for (int button = 1; button < 6; ++button) {
		if (button == 3)
		    continue; // don't override the LED button
		for (int mode = 0; mode < 4; ++mode) {
		    int actualButton = button;

		    if (actualButton > 3)
			actualButton++;
		    Protocol.getInstance(context).disableButton(actualButton, mode, MetaWatchService.WatchBuffers.APPLICATION);
		}
	    }
	}
    }

    @Override
    public Bitmap update(Context context, boolean preview, int watchType) {
	initBuffer(context);
	receivedData = true;
	Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	canvas.drawBitmap(buffer, 0, 0, null);
	drawDigitalAppSwitchIcon(context, canvas, preview);
	return bitmap;
    }

    @Override
    public int buttonPressed(Context context, int id) {
	if (id >= 200 && id < 220) {
	    id = id - 200;

	    int button = 1 + (id / 4);
	    int type = id % 4;

	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Sending button press: " + button + " " + type);

	    Intent intent = new Intent("org.metawatch.manager.BUTTON_PRESS");
	    intent.putExtra("id", appData.id);
	    intent.putExtra("button", button);
	    intent.putExtra("type", type);
	    context.sendBroadcast(intent);

	    return ApplicationBase.BUTTON_USED;
	}
	return ApplicationBase.BUTTON_NOT_USED;
    }

    private void initBuffer(Context context) {
	if (buffer == null) {
	    buffer = Protocol.getInstance(context).createTextBitmap(context, "Starting application mode ...");
	}
    }

    public void setBuffer(Bitmap bitmap) {
	buffer = bitmap;
    }

}
