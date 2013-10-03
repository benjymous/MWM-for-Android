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
 * GmailLSMonitor.java                                                       *
 * GmailLSMonitor                                                            *
 * Watching for latest Gmail e-mails, working with Gmail version older than  *
 * version 2.3.5 (excluded)                                                  *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Utils.CursorHandler;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import org.metawatch.manager.Log;

public class GmailLSMonitor implements GmailMonitor {
    public static boolean isSupported(Context context) {
	try {
	    PackageManager packageManager = context.getPackageManager();
	    PackageInfo packageInfo = packageManager.getPackageInfo("com.google.android.gm", 0);
	    // check for Gmail version earlier than v2.3.5 (169)
	    if (packageInfo.versionCode < 169)
		return true;

	} catch (NameNotFoundException e) {
	}

	return false;
    }

    Context context;

    MyContentObserver contentObserver = new MyContentObserver();

    public static List<String> accounts;
    public static int lastUnreadCount = 0;

    public GmailLSMonitor(Context ctx) {
	super();
	context = ctx;
	accounts = Utils.getGoogleAccountsNames(context);

	if (accounts.size() == 0) {
	    throw new IllegalArgumentException("No account found.");
	}

	lastUnreadCount = getUnreadCount("^u");
    }

    public void startMonitor() {
	for (String account : accounts) {
	    try {
		Uri uri = Uri.parse("content://gmail-ls/conversations/" + account);
		context.getContentResolver().registerContentObserver(uri, true, contentObserver);
	    } catch (Exception x) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, x.toString());
	    }
	}
    }

    private class MyContentObserver extends ContentObserver {
	public MyContentObserver() {
	    super(null);
	}

	@Override
	public void onChange(boolean selfChange) {
	    super.onChange(selfChange);

	    if (Preferences.logging)
		Log.d("ow", "onChange observer - unread");

	    int currentUnreadCount = getUnreadCount("^u");

	    // if (Preferences.logging) Log.d("ow",
	    // "current gmail unread count: " +
	    // Integer.toString(currentGmailUnreadCount));

	    if (Preferences.notifyGmail && currentUnreadCount > lastUnreadCount) {
		if (Preferences.logging)
		    Log.d("ow", Integer.toString(currentUnreadCount) + " > " + Integer.toString(lastUnreadCount));
		sendUnreadGmail();
	    }

	    if (currentUnreadCount != lastUnreadCount) {
		Idle.getInstance().updateIdle(context, true);
	    }

	    lastUnreadCount = currentUnreadCount;
	}
    }

    public int getUnreadCount() {
	return getUnreadCount("^i");
    }

    private int getUnreadCount(String label) {
	int intUnread = 0;

	Utils.CursorHandler ch = new Utils.CursorHandler();
	for (String account : accounts) {
	    try {

		int nameColumn = 0;

		Cursor c = ch.add(context.getContentResolver().query(Uri.parse("content://gmail-ls/labels/" + account), null, null, null, null));
		c.moveToFirst();

		nameColumn = c.getColumnIndexOrThrow("canonicalName");

		while (true) {
		    if (c.getString(nameColumn).equals(label)) {
			intUnread += c.getInt(c.getColumnIndexOrThrow("numUnreadConversations"));
		    }

		    c.moveToNext();

		    if (c.isLast()) {
			break;
		    }
		}
	    } catch (Exception x) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "GmailLSMonitor.getUnreadCount(): caught exception: " + x.toString());
	    } finally {
		ch.closeAll();
	    }
	}
	// if (Preferences.logging) Log.d(MetaWatchStatus.TAG,
	// "GmailLSMonitor.getUnreadCount(): couldn't find count, returning 0.");
	return intUnread;
    }

    private void sendUnreadGmail() {
	CursorHandler ch = new Utils.CursorHandler();
	try {
	    for (String account : accounts) {
		try {
		    int nameColumn = 0;
		    String id = "";
		    String convId = "";

		    double maxDate = 0;

		    Cursor c = ch.add(context.getContentResolver().query(Uri.parse("content://gmail-ls/labels/" + account), null, null, null, null));
		    c.moveToFirst();

		    nameColumn = c.getColumnIndexOrThrow("canonicalName");

		    while (true) {
			if (c.getString(nameColumn).equals("^u"))
			    id = c.getString(c.getColumnIndexOrThrow("_id"));

			if (c.isLast())
			    break;

			c.moveToNext();
		    }

		    Cursor c2 = ch.add(context.getContentResolver().query(Uri.parse("content://gmail-ls/conversations/" + account), null, null, null, null));
		    c2.moveToLast();

		    nameColumn = c2.getColumnIndexOrThrow("labelIds");

		    while (true) {
			if (c2.getString(nameColumn).indexOf(id) >= 0)
			    convId = c2.getString(c2.getColumnIndexOrThrow("conversation_id"));

			if (c2.isFirst())
			    break;

			c2.moveToPrevious();
		    }
		    // ///////////////

		    maxDate = 0;

		    String subject = "";
		    String sender = "";
		    String snippet = "";

		    Cursor c3 = ch.add(context.getContentResolver().query(Uri.parse("content://gmail-ls/conversations/" + account + "/" + convId + "/messages"), null, null, null, null));
		    // startManagingCursor(c3);
		    c3.moveToFirst();

		    int colConvId = c3.getColumnIndexOrThrow("conversation");
		    int colSub = c3.getColumnIndexOrThrow("subject");
		    int colFrom = c3.getColumnIndexOrThrow("fromAddress");
		    int colRcv = c3.getColumnIndexOrThrow("dateReceivedMs");

		    while (true) {
			if (c3.getString(colConvId).indexOf(convId) >= 0) {
			    double thisDate = Double.parseDouble(c3.getString(colRcv));

			    if (thisDate > maxDate) {
				subject = c3.getString(colSub);
				sender = c3.getString(colFrom);
				snippet = c3.getString(c3.getColumnIndexOrThrow("snippet"));
				maxDate = thisDate;
			    }
			}

			if (c3.isLast())
			    break;

			c3.moveToNext();
		    }

		    Pattern pattern = Pattern.compile("(\"[^\"]*\") (<.*>)");
		    Matcher matcher = pattern.matcher(sender);
		    matcher.find();

		    String senderName = matcher.group(1).replace("\"", "");
		    String senderMail = matcher.group(2).replace("<", "").replace(">", "");

		    NotificationBuilder.createGmail(context, senderName, senderMail, subject, snippet);

		} catch (Exception x) {
		}
	    }
	} finally {
	    ch.closeAll();
	}
    }

}
