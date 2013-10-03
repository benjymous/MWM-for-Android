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
 * ImageViewer.java                                                          *
 * ImageViewer                                                               *
 * System wide "Send to watch" that shows pictures in app mode               *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import java.io.FileNotFoundException;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Notification.VibratePattern;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;

public class ImageViewer extends Activity {

    @Override
    public void onCreate(Bundle bundle) {
	super.onCreate(bundle);
	Intent intent = getIntent();
	String action = intent.getAction();
	Uri u = null;
	if (action.equals(Intent.ACTION_VIEW)) {
	    u = intent.getData();
	    if (Preferences.logging)
		Log.e(MetaWatchStatus.TAG, "Intent received: Action View");
	} else if (action.equals(Intent.ACTION_SEND)) {
	    u = intent.getParcelableExtra(Intent.EXTRA_STREAM);
	    if (Preferences.logging)
		Log.e(MetaWatchStatus.TAG, "Intent received: Action Send");
	} else {
	    if (Preferences.logging)
		Log.e(MetaWatchStatus.TAG, "ImageViewer.onCreate() Unknown intent: " + action);
	    finish();
	}
	if (Preferences.logging) {
	    Log.d(MetaWatchStatus.TAG, "action: " + intent.getAction());
	    Log.d(MetaWatchStatus.TAG, "data: " + u.getPath());
	}
	try {
	    BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inSampleSize = 2;
	    Bitmap scaled = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(getContentResolver().openInputStream(u), null, options), 96, 96, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
	    Bitmap dithered = Utils.ditherTo1bit(scaled, Preferences.invertLCD);
	    scaled.recycle();
	    
	    Notification.getInstance().addBitmapNotification(this, dithered, NotificationBuilder.createVibratePatternFromBuzzes(1), Notification.getInstance().getDefaultNotificationTimeout(this), "Image viewer");
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	}
	finish();
    }
}