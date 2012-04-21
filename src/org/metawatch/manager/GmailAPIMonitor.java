                                                                     
                                                                     
                                                                     
                                             
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
  * GmailAPIMonitor.java                                                      *
  * GmailAPIMonitor                                                           *
  * Watching for latest Gmail e-mails, working with Gmail version newer than  *
  * version 2.3.6 or 4.0.5 (inclusive)                                        *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.Preferences;

import com.google.android.gm.contentprovider.GmailContract;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class GmailAPIMonitor implements GmailMonitor {
	public static boolean isSupported(Context context) {
		return GmailContract.canReadLabels(context);
	}

	Context context;
	
	MyContentObserver contentObserver = new MyContentObserver();
	
	public static Uri labelUri = null;
	public static int lastUnreadCount = 0;
	String account = null;
	
	public GmailAPIMonitor(Context ctx) {
		super();		
		context = ctx;
		account = Utils.getGoogleAccountName(ctx);
		
		if (account == null) {
			throw new IllegalArgumentException("No account found.");
		}

		// find labels for the account.
		Cursor c = context.getContentResolver().query(GmailContract.Labels.getLabelsUri(account), null, null, null, null);
		// loop through the cursor and find the Inbox.
		if (c != null) {
			// Technically, you can choose any label here, including priority inbox and all mail.
			// Make a setting for it later?
		    final String inboxCanonicalName = GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX;
		    final int canonicalNameIndex = c.getColumnIndexOrThrow(GmailContract.Labels.CANONICAL_NAME);
		    while (c.moveToNext()) {
		        if (inboxCanonicalName.equals(c.getString(canonicalNameIndex))) {
		            labelUri = Uri.parse(c.getString(c.getColumnIndexOrThrow(GmailContract.Labels.URI)));
		        }
		    }
		}
		
		if (labelUri == null) {
			throw new IllegalArgumentException("Label not found.");
		}
		
		lastUnreadCount = getUnreadCount();
	}

	public void startMonitor() {
		try {
			context.getContentResolver().registerContentObserver(labelUri, true, contentObserver);
		} catch (Exception x) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, x.toString());
		}
	}


	private class MyContentObserver extends ContentObserver {
		public MyContentObserver() {
			super(null);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			if (Preferences.logging) Log.d("ow", "onChange observer - unread");

			int currentUnreadCount = getUnreadCount();

			//if (Preferences.logging) Log.d("ow", "current gmail unread count: " + Integer.toString(currentGmailUnreadCount));

			if (Preferences.notifyGmail && currentUnreadCount > lastUnreadCount)
			{
				if (Preferences.logging) Log.d("ow", Integer.toString(currentUnreadCount) + " > " + Integer.toString(lastUnreadCount));

				NotificationBuilder.createGmailBlank(context, account, currentUnreadCount);
			}
			
			if (currentUnreadCount != lastUnreadCount)
			{
				Idle.updateIdle(context, true);
			}
			
			lastUnreadCount = currentUnreadCount;
		}
	}
	
	public int getUnreadCount() {
		try {
			Cursor c = context.getContentResolver().query(labelUri, null, null, null, null);
			c.moveToFirst();
			
			int unreadIndex = c.getColumnIndexOrThrow(GmailContract.Labels.NUM_UNREAD_CONVERSATIONS);
			 return c.getInt(unreadIndex);
		} catch (Exception x) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "GmailAPIMonitor.getUnreadCount(): caught exception: " + x.toString());
		}

		if (Preferences.logging) Log.d(MetaWatch.TAG, "GmailAPIMonitor.getUnreadCount(): couldn't find count, returning 0.");
		return 0;
	}
}
