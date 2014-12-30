                                                                     
                                                                     
                                                                     
                                             
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

import java.util.ArrayList;
import java.util.List;

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

	/**
	 * Describe a Gmail account. Used to store how many unread messages are in which account
	 * 
	 * @author tschork
	 * 
	 */
	public class GmailAccount {
		public int		unreadCount	= 0;
		public String	accountName	= null;
		public String 	inboxName 	= null;
		public Uri		uri			= null;

		public GmailAccount(String pAccountName, String pInboxName, int pUnreadCount, Uri pUri) {
			accountName = pAccountName;
			inboxName = pInboxName;
			unreadCount = pUnreadCount;
			uri = pUri;
		}
	}
	
	Context context;
	
	MyContentObserver contentObserver = new MyContentObserver();
	
	public static List<GmailAccount> ListAccounts=new ArrayList<GmailAccount>();
	public static int lastUnreadCount = 0;
	String account = null;
	
	public GmailAPIMonitor(Context ctx) {
		super();		
		context = ctx;
		Utils.CursorHandler ch = new Utils.CursorHandler();
		try {
			final List<String> accounts = Utils.getGoogleAccountsNames(ctx);
			if (accounts.size() == 0) {
				throw new IllegalArgumentException("No account found.");
			}
			
			for (String account : accounts) {
				// find labels for the account.
				Cursor c = ch.add(context.getContentResolver().query(GmailContract.Labels.getLabelsUri(account), null, null, null, null));
				// loop through the cursor and find the Inbox.
				if (c != null) {
					// Technically, you can choose any label here, including priority inbox and all mail.
					// Make a setting for it later?
					ArrayList<String> inboxCanonicalNames = new ArrayList<String>();
					inboxCanonicalNames.add(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX);
					inboxCanonicalNames.add(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX_CATEGORY_FORUMS);
					inboxCanonicalNames.add(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX_CATEGORY_PRIMARY);
					inboxCanonicalNames.add(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX_CATEGORY_PROMOTIONS);
					inboxCanonicalNames.add(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX_CATEGORY_SOCIAL);
					inboxCanonicalNames.add(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX_CATEGORY_UPDATES);
					inboxCanonicalNames.add(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_PRIORITY_INBOX);
					final int canonicalNameIndex = c.getColumnIndexOrThrow(GmailContract.Labels.CANONICAL_NAME);
					final int nameIndex = c.getColumnIndexOrThrow(GmailContract.Labels.NAME);
					while (c.moveToNext()) {
						for (String inboxCanonicalName : inboxCanonicalNames) {
							if (inboxCanonicalName.equals(c.getString(canonicalNameIndex))) {
								Uri uri = Uri.parse(c.getString(c.getColumnIndexOrThrow(GmailContract.Labels.URI)));
								ListAccounts.add(new GmailAccount(account, c.getString(nameIndex), 0, uri));
							}
						}
					}
				}
				if (ListAccounts.size() == 0) {
					throw new IllegalArgumentException("Label not found.");
				}
				
				lastUnreadCount = getUnreadCount();
			}
		} catch (Exception e) {
			// handle exception
			Log.e(MetaWatch.TAG, e.getMessage());
		}
		finally {
			ch.closeAll();
		}
	}

	public void startMonitor() {
		for (GmailAccount objAccnt : ListAccounts) {
			try {
				context.getContentResolver().registerContentObserver(objAccnt.uri, true, contentObserver);
			} catch (Exception x) {
				if (Preferences.logging)
					Log.e(MetaWatch.TAG, x.toString());
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

			if (Preferences.logging) Log.d("ow", "onChange observer - unread");

			int currentUnreadCount = getUnreadCount();

			//if (Preferences.logging) Log.d("ow", "current gmail unread count: " + Integer.toString(currentGmailUnreadCount));

			if (Preferences.notifyGmail && currentUnreadCount > lastUnreadCount)
			{
				if (Preferences.logging) Log.d("ow", Integer.toString(currentUnreadCount) + " > " + Integer.toString(lastUnreadCount));

				// TODO: when the length of the recipients is too big, split the notification on 2 screens?
				String recipient = "";
				final List<String> accounts = Utils.getGoogleAccountsNames(context);
				for (String account : accounts) {
					StringBuilder sb = new StringBuilder();
					if (!recipient.equals("")) {
						sb.append(recipient).append("\n");
					}
					//sb.append(account).append(" = ");
					boolean firstEntry = true;
					for (GmailAccount objGmailUnread : ListAccounts) {
						if (objGmailUnread.accountName.equals(account)&&(objGmailUnread.unreadCount > 0)) {
							if (!firstEntry) {
								sb.append(", ");
							}
							sb.append(objGmailUnread.inboxName).append(": ").append(objGmailUnread.unreadCount);
							firstEntry = false;
						}
					}
					recipient = sb.toString();
				}
				NotificationBuilder.createGmailBlank(context, recipient, currentUnreadCount);
			}
			
			if (currentUnreadCount != lastUnreadCount)
			{
				Idle.updateIdle(context, true);
			}
			
			lastUnreadCount = currentUnreadCount;
		}
	}
	
	/***
	 * Returns how many unread Gmail messages are in a given label
	 * 
	 * @param uri
	 *            The label uri
	 * @return integer The number of unread messages
	 */
	private int getUnreadCount(Uri uri) {
		int unreadCnt = 0;
		Utils.CursorHandler ch = new Utils.CursorHandler();
		try {
			Cursor c = ch.add(context.getContentResolver().query(uri, null, null, null, null));
			c.moveToFirst();
			unreadCnt += c.getInt(c.getColumnIndexOrThrow(GmailContract.Labels.NUM_UNREAD_CONVERSATIONS));
		} catch (Exception x) {
			if (Preferences.logging)
				Log.d(MetaWatch.TAG, "GmailAPIMonitor.getUnreadCount(): caught exception: " + x.toString());
		} finally {
			ch.closeAll();
		}
		return unreadCnt;
	}
		
	public int getUnreadCount() {
		int unreadCnt = 0;
		int accUnread = 0;
		try {
			for (GmailAccount objGmailUnread : ListAccounts) {
				accUnread = getUnreadCount(objGmailUnread.uri);
				objGmailUnread.unreadCount = accUnread;
				unreadCnt += accUnread;
			}
		} catch (Exception x) {
			if (Preferences.logging)
				Log.d(MetaWatch.TAG, "GmailAPIMonitor.getUnreadCount(): caught exception: " + x.toString());
		}
		
		if (Preferences.logging)
			Log.d(MetaWatch.TAG, "GmailAPIMonitor.getUnreadCount(): found " + unreadCnt + " unread messages");
		return unreadCnt;
	}
}
