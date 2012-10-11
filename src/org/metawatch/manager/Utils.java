                                                                     
                                                                     
                                                                     
                                             
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
  * Utils.java                                                                *
  * Utils                                                                     *
  * Different utils                                                           *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.metawatch.manager.FontCache.FontInfo;
import org.metawatch.manager.FontCache.FontSize;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings.SettingNotFoundException;
import android.text.Layout;
import android.text.Spannable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class Utils {

	static public String Meeting_Title = "---";
	static public String Meeting_Location = "---";
	static public long Meeting_EndTimestamp;
	static public long Meeting_StartTimestamp;
	
	public static class CursorHandler {
		private List<Cursor> cursors = new ArrayList<Cursor>();
		
		public Cursor add(Cursor c) {
			if (c!=null)
				cursors.add(c);
			return c;
		}
		
		public void closeAll() {
			for(Cursor c : cursors) {
				if(!c.isClosed())
					c.close();
			}
		}
	}
	
	public static String getContactNameFromNumber(Context context, String number) {
		
		CursorHandler ch = new CursorHandler();
		
		try {
			if (number.equals(""))
				return "Private number";
	
			String[] projection = new String[] { PhoneLookup.DISPLAY_NAME, PhoneLookup.NUMBER };
			
			Uri contactUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
			Cursor c = ch.add( context.getContentResolver().query(contactUri, projection, null, null, null) );
			
			if (c==null)
				return number;
			
			if (c.moveToFirst()) {
				String name = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
				if (name.length() > 0)
					return name;
			}
			
			return number;
		}
		catch(java.lang.IllegalStateException e) {
			return number;
		}
		finally {
			ch.closeAll();
		}
	}
	
	public static Bitmap getContactPhotoFromNumber(Context context, String number) {

		CursorHandler ch = new CursorHandler();
		
		try {
			if (number.equals(""))
				return null;

			Uri contactUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
			String[] projection = new String[] {
					ContactsContract.Contacts._ID,
					PhoneLookup.PHOTO_ID, 
					PhoneLookup.NUMBER };
			
		    Uri photoUri = null;
		    Cursor c = ch.add( context.getContentResolver().query(contactUri,
		            projection, null, null, null) );

		    if (c==null)
		    	return null;
		    
		    if (c.moveToFirst()) {
		    	// Try openContactPhotoInputStream first.
		        long contactId = c.getLong(c.getColumnIndex(ContactsContract.Contacts._ID));
		        photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
		        if (photoUri != null) {
		        	InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(
		        			context.getContentResolver(), photoUri);
		        	if (input != null) {
		        		return BitmapFactory.decodeStream(input);
		        	}
		        }
		        
		        // The above failed, fallback to PHOTO_ID.
				int photoID = c.getInt(c.getColumnIndex(PhoneLookup.PHOTO_ID));

				photoUri = ContactsContract.Data.CONTENT_URI;
				c = ch.add( context.getContentResolver().query(photoUri, new String[]{ContactsContract.CommonDataKinds.Photo.PHOTO, ContactsContract.Data.PHOTO_ID}, Data.PHOTO_ID + " = " + photoID, null, null) );
				
				if (c!=null && c.moveToFirst()) {
		            try {
		            	ByteArrayInputStream rawPhotoStream = new ByteArrayInputStream(c.getBlob(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Photo.PHOTO)));
		            	Bitmap contactPhoto = BitmapFactory.decodeStream(rawPhotoStream);
		            	return contactPhoto;
		            }
		            catch (NullPointerException ex) {
		            	return null;
		            }
		        }
			}
			
			return null;
		}
		catch(java.lang.IllegalStateException e) {
			return null;
		}
		finally {
			ch.closeAll();
		}

	}
	
	public static int getUnreadSmsCount(Context context) {
		int count = 0;
		
		//SMS
		try {
			Cursor cursor = context.getContentResolver().query(
					Uri.withAppendedPath(Uri.parse("content://sms"), "inbox"), 
					new String[] { "_id" }, 
					"read=0", 
					null, 
					null
				);
			
			if (cursor != null) {
				try {
					count = cursor.getCount();
				} finally {
					cursor.close();
				}
			}
		}
		catch (java.lang.IllegalStateException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Failed to query SMS content provider");
		}
		
		//MMS
		try {
			Cursor cursor = context.getContentResolver().query(
					Uri.withAppendedPath(Uri.parse("content://mms"), "inbox"), 
					new String[] { "_id" }, 
					"read=0", 
					null, 
					null
				);
			
			if (cursor != null) {
				try {
					count += cursor.getCount();
				} finally {
					cursor.close();
				}
			}
		}
		catch (java.lang.IllegalStateException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Failed to query MMS content provider");
		}
		
		return count;
	}
	
	public static int getMissedCallsCount(Context context) {
		int missed = 0;
		CursorHandler ch = new CursorHandler();
		try {
			Cursor cursor = ch.add( context.getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI, null, null, null, null) );
			
			cursor.moveToFirst();

			while (true) {
				if (cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE)) == 3)
					missed += cursor.getInt(cursor.getColumnIndex(CallLog.Calls.NEW));

				if (cursor.isLast())
					break;

				cursor.moveToNext();
			}

		} catch (Exception x) {
		}
		finally {
			ch.closeAll();
		}
		return missed;
	}
	
	public static String readCalendar(Context context, int Return) {
		long now = new Date().getTime();
		final long CurrentTime = System.currentTimeMillis();

		CursorHandler ch = new CursorHandler();
		
		try {

			String titletemp="";
			String locationtemp="";
			String MeetingTime;
			long currentremaintime;
			long begintemp=0;
			long elapsedtimetemp=0;
			long mintime=(long)(1000*60*1.2);
			if (!Preferences.readCalendarDuringMeeting) {
				mintime=-1000*60; // to have some safety margin in case the meeting is just starting
			}

			currentremaintime=0;
			//location="nowhere";

			ContentResolver cr = context.getContentResolver();
			Cursor cursor = ch.add( cr.query(Uri.parse("content://com.android.calendar/calendars"), new String[]{ "_id","name"}, null, null, null) );
			cursor.moveToFirst();
			String[] CalNames = new String[cursor.getCount()];
			int[] CalIds = new int[cursor.getCount()];
			for (int i = 0; i < CalNames.length; i++) {
				CalIds[i] = cursor.getInt(0);
				CalNames[i] = cursor.getString(1);
				cursor.moveToNext();
			}
			Uri.Builder builder = Uri.parse("content://com.android.calendar/instances/when").buildUpon();

			ContentUris.appendId(builder, now );
			ContentUris.appendId(builder, now + DateUtils.DAY_IN_MILLIS);	        
			Cursor eventCursor = ch.add(cr.query(builder.build(),
					new String[] { "event_id", "begin", "end", "allDay"}, null, null, "startDay ASC, startMinute ASC"));
			// For a full list of available columns see http://tinyurl.com/yfbg76w
			MeetingTime="None";
			while (eventCursor.moveToNext()) {
				if ((eventCursor.getLong(1) > (CurrentTime+mintime)) &&(eventCursor.getString(3).equals("0"))){
					String uid2 = eventCursor.getString(0);	
					Uri CALENDAR_URI = Uri.parse("content://com.android.calendar/events/" + uid2);
					//if (Preferences.logging) Log.d(MetaWatch.TAG,"CalendarService.GetData(): Calendar URI: "+ CALENDAR_URI);
					Cursor c = ch.add(cr.query(CALENDAR_URI,new String[] { "title", "eventLocation", "description",}, null, null, null)); 
					//if (Preferences.logging) Log.d(MetaWatch.TAG,"CalendarService.GetData(): Calendar cursor: "+ c.getCount());
					if (c.moveToFirst())
					{	
						//if (Preferences.logging) Log.d(Constants.LOG_TAG,"CalendarService.GetData(): Calendar title: "+ c.getString(c.getColumnIndex("title")));
						//if (Preferences.logging) Log.d(Constants.LOG_TAG,"CalendarService.GetData(): Calendar location: "+ c.getString(c.getColumnIndex("eventLocation")));
						titletemp = c.getString(c.getColumnIndex("title"));
						locationtemp = c.getString(c.getColumnIndex("eventLocation"));    
					}

					begintemp = eventCursor.getLong(1);
					Meeting_StartTimestamp = begintemp;
					MeetingTime = new SimpleDateFormat("HH:mm").format(begintemp);
					Meeting_EndTimestamp = eventCursor.getLong(2);

					if (Preferences.readCalendarDuringMeeting) {
					  elapsedtimetemp = (begintemp-CurrentTime);
					} else {
					  elapsedtimetemp = (Meeting_EndTimestamp-(Preferences.readCalendarMinDurationToMeetingEnd+1)*60*1000-CurrentTime);
					}

					//if (Preferences.logging) Log.d(MetaWatch.TAG,"CalendarService.GetData(): Next Meeting time : "+ MeetingTime);
					//if (Preferences.logging) Log.d(MetaWatch.TAG,"CalendarService.GetData(): Next Meeting Title : " + titletemp);
					//if (Preferences.logging) Log.d(MetaWatch.TAG,"CalendarService.GetData(): Next Meeting Location : " + locationtemp);


					if (currentremaintime != 0) {
						if (currentremaintime > elapsedtimetemp){
							currentremaintime = elapsedtimetemp;
							Meeting_Title = titletemp;
							Meeting_Location = locationtemp;
						}
					}
					else
					{
						currentremaintime = elapsedtimetemp;
						Meeting_Title = titletemp;
						Meeting_Location = locationtemp;

					}

					break;
				}
			}   

			/*** Schedule ending notification ***/
			if (!MeetingTime.equals("None")){

				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.MILLISECOND, (int)currentremaintime);
				Intent intent = new Intent("org.metawatch.manager.UPDATE_CALENDAR");

				intent.putExtra("Calendar", titletemp);
				// In reality, you would want to have a static variable for the request code instead of 192837
				PendingIntent sender = PendingIntent.getBroadcast(context, 192837, intent, PendingIntent.FLAG_UPDATE_CURRENT);

				// Get the AlarmManager service
				AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
				//if (Preferences.logging) Log.d(MetaWatch.TAG,"CalendarService: Next Meeting alarm time : "+ cal);
			}
			else {
				Meeting_Title = "---";
				Meeting_Location = "---";
			}

			

			if (Return==1){
				return String.valueOf(currentremaintime);
			}
			else{
				return MeetingTime;
			}
		}
		catch(Exception x) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Utils.readCalendar(): caught exception: " + x.toString());
			return "None";
		}
		finally {
			ch.closeAll();
		}

	}
	
	static final Uri k9AccountsUri = Uri.parse("content://com.fsck.k9.messageprovider/accounts/");
	static final String k9UnreadUri = "content://com.fsck.k9.messageprovider/account_unread/";
	
	private static int k9UnreadCount = 0;	
	private static long k9LastRefresh = 0;
	
	public static int getUnreadK9Count(Context context) {
		long time = System.currentTimeMillis();
		if(time - k9LastRefresh > 1*60*1000)
			refreshUnreadK9Count(context);
		
		return k9UnreadCount;
	}
	
	private static int getUnreadK9Count(Context context, int accountNumber) {
		CursorHandler ch = new CursorHandler();
		try {
			Cursor cur = ch.add(context.getContentResolver().query(Uri.parse(k9UnreadUri+"/"+accountNumber+"/"), null, null, null, null));
		    if (cur!=null) {
		    	if (Preferences.logging) Log.d(MetaWatch.TAG, "k9: "+cur.getCount()+ " unread rows returned");
		    			    	
		    	if (cur.getCount()>0) {
			    	cur.moveToFirst();
			    	int unread = 0;
			    	int nameIndex = cur.getColumnIndex("accountName");
			    	int unreadIndex = cur.getColumnIndex("unread");
			    	do {
			    		String acct = cur.getString(nameIndex);
			    		int unreadForAcct = cur.getInt(unreadIndex);
			    		if (Preferences.logging) Log.d(MetaWatch.TAG, "k9: "+acct+" - "+unreadForAcct+" unread");
			    		unread += unreadForAcct;
			    	} while (cur.moveToNext());
				    cur.close();
				    return unread;
		    	}
		    }
		    else {
		    	if (Preferences.logging) Log.d(MetaWatch.TAG, "Failed to query k9 unread contentprovider.");
		    }
		}
		catch (IllegalStateException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "k-9 unread uri unknown.");
		}
		return 0;
	}
	
	public static void refreshUnreadK9Count(Context context) {		
		int accounts = getK9AccountCount(context);
		if (accounts>0) {
			int count = 0;
			for (int acct=0; acct<accounts; ++acct) {
				count += getUnreadK9Count(context, acct);
			}
			k9UnreadCount = count;
			k9LastRefresh = System.currentTimeMillis();
		}	
	}
	
	public static int getK9AccountCount(Context context) {
		CursorHandler ch = new CursorHandler();
		try {
			Cursor cur = ch.add( context.getContentResolver().query(k9AccountsUri, null, null, null, null) );
		    if (cur!=null) {
		    	if (Preferences.logging) Log.d(MetaWatch.TAG, "k9: "+cur.getCount()+ " account rows returned");

		    	int count = cur.getCount();
		    	
		    	return count;
		    }
		    else {
		    	if (Preferences.logging) Log.d(MetaWatch.TAG, "Failed to query k9 unread contentprovider.");
		    }
		}
		catch (IllegalStateException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "k-9 accounts uri unknown.");
		}
		catch (java.lang.SecurityException e) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Permissions failure accessing k-9 databases");
		}
		finally {
			ch.closeAll();
		}
		return 0;

	}
	
	public static Bitmap getBitmap(Context context, String path) {
		return BitmapCache.getBitmap(context, path);
	}
	
	public static Bitmap ditherTo1bit(Bitmap input, boolean inverted) {
	
		Bitmap output = input.copy(Config.RGB_565, true);
		
		double[][] pixels = new double[input.getWidth()][input.getHeight()];
		
		final int w=input.getWidth();
		final int h=input.getHeight();
		
		for(int y=0; y<h; ++y) {
		   for(int x=0; x<w; ++x) {
			   int col = input.getPixel(x, y);
			   
			   double R = ((col >> 16) & 0xff)/256.0; 
		       double G = ((col >> 8) & 0xff)/256.0;
		       double B = (col & 0xff)/256.0;
		        
		       pixels[x][y] = ( (0.3*R) + (0.59*G) + (0.11*B) );		        
		   }
		}
		
		for(int y=0; y<h; ++y) {
			   for(int x=0; x<w; ++x) {
				   double oldpixel = pixels[x][y];
				   double newpixel = oldpixel<0.5 ? 0 : 1;
				   	   
				   pixels[x][y] = newpixel;
				   double quant_error = oldpixel - newpixel;
				   if(x<w-1)
					   pixels[x+1][y] += 7.0/16.0 * quant_error;
				   if(x>0 && y<h-1)
					   pixels[x-1][y+1] += 3.0/16.0 * quant_error;
				   if(y<h-1)
					   pixels[x][y+1] += 5.0/16.0 * quant_error;
				   if(x<w-1 && y<h-1)
					   pixels[x+1][y+1] += 1.0/16.0 * quant_error;
			
				   int col = 0;
				   if (inverted)
					   col = newpixel > 0.5 ? 0xff000000 : 0xffffffff;   
				   else
					   col = newpixel > 0.5 ? 0xffffffff : 0xff000000;
				   output.setPixel(x, y, col);
			   
			   }
		}

		return output;
	}
	
	public static Bitmap resize(Bitmap bm, int newHeight, int newWidth) {

		int width = bm.getWidth();
		int height = bm.getHeight();

		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);

		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
		return resizedBitmap;
	}
	
	public static void dumpBitmapToSdCard(Bitmap bitmap, String filename) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(filename);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public static String getVersion(Context context) {		
		try {
			PackageManager packageManager = context.getPackageManager();
			PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionName;
		} catch (NameNotFoundException e) {
		}
		return "unknown";
	}
	
	public static boolean isGmailAccessSupported(Context context) {
		return (
			GmailAPIMonitor.isSupported(context) ||
			GmailLSMonitor.isSupported(context)
		);
	}
	
	private static GmailMonitor gmailMonitor = null;
	public static GmailMonitor getGmailMonitor(Context context) {
		if (gmailMonitor == null) {
			try {
				if (GmailAPIMonitor.isSupported(context)) {
					gmailMonitor = new GmailAPIMonitor(context);
				} else if (GmailLSMonitor.isSupported(context)) {
					gmailMonitor = new GmailLSMonitor(context);
				}
			} catch (Exception e) {
				gmailMonitor = null;
			}
		}
		
		return gmailMonitor;
	}


	public static int getUnreadGmailCount(Context context) {
		GmailMonitor monitor = getGmailMonitor(context);
		if (monitor != null) {
			return monitor.getUnreadCount();
		}
		
		// Fallback to our own counter (based on notifications).
		return Monitors.getGmailUnreadCount();
	}
	
	public static String getGoogleAccountName(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccounts();

		for (Account account : accounts) {
			if (account.type.equals("com.google")) {
				return account.name;
			}
		}
		return null;
	}
	
	/**
	 * Return a lists of configured Gmail account, rather than the first one
	 * 
	 * @param context
	 * @return List<String> the list of the account names
	 */
	public static List<String> getGoogleAccountsNames(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccounts();
		
		final List<String> accNames = new ArrayList<String>();
		
		for (Account account : accounts) {
			if (account.type.equals("com.google")) {
				accNames.add(account.name);
			}
		}
		
		return accNames;
	}
	
	public static String ReadInputStream(InputStream in) throws IOException {
		StringBuffer stream = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			stream.append(new String(b, 0, n));
		}
		return stream.toString();
	}
	
	public static void drawWrappedText(String text, Canvas canvas, int x, int y, int width, TextPaint paint, android.text.Layout.Alignment align) {
		canvas.save();
		StaticLayout layout = new StaticLayout(text, paint, width, align, 1.0f, 0, false);
		canvas.translate(x, y); //position the text
		layout.draw(canvas);
		canvas.restore();	
	}
	
	public static void drawOutlinedText(String text, Canvas canvas, int x, int y, TextPaint col, TextPaint outline) {
		canvas.drawText(text, x+1, y, outline);
		canvas.drawText(text, x-1, y, outline);
		canvas.drawText(text, x, y+1, outline);
		canvas.drawText(text, x, y-1, outline);
	
		canvas.drawText(text, x, y, col);
	}
	
	public static void drawWrappedOutlinedText(String text, Canvas canvas, int x, int y, int width, TextPaint col, TextPaint outline, android.text.Layout.Alignment align) {
		drawWrappedText(text, canvas, x-1, y, width, outline, align);
		drawWrappedText(text, canvas, x+1, y, width, outline, align);
		drawWrappedText(text, canvas, x, y-1, width, outline, align);
		drawWrappedText(text, canvas, x, y+1, width, outline, align);
		
		drawWrappedText(text, canvas, x, y, width, col, align);
	}
	
	public static Bitmap DrawIconCountWidget(Context context, int width, int height, Bitmap icon, int count, TextPaint textPaint) {
		String text;
		// Stop the text being too wide for the widget
		if (height==16 && count>1999)
			text="999+";
		else {
			if (count<0)
				text = "-";
			else
				text = Integer.toString(count);
		}
		return DrawIconStringWidget(context,width,height,icon,text,textPaint);
	}
	
	public static Point getIconOffset(int rowHeight) {
		if (rowHeight == 16) {
			if (Preferences.displayWidgetIconOnTop) return new Point(2,0);
			else return new Point(2,6);
		} else if (rowHeight == 32) {
			if (Preferences.displayWidgetIconOnTop) return new Point(0,3);
			else return new Point(0,14);
		} else if (rowHeight == 46) {
			if (Preferences.displayWidgetIconOnTop) return new Point(11,6);
			else return new Point(11,24);
		} else {
			return new Point(0,0);
		}
	}
	
	public static Point getTextOffset(int rowHeight) {
		if (rowHeight == 16) {
			if (Preferences.displayWidgetIconOnTop) return new Point(8,15);
			else return new Point(8,0);
		} else if (rowHeight == 32) {
			if (Preferences.displayWidgetIconOnTop) return new Point(12,30);
			else return new Point(12,12);
		} else if (rowHeight == 46) {
			if (Preferences.displayWidgetIconOnTop) return new Point(24,40);
			else return new Point(24,18);
		} else {
			return new Point(0,rowHeight); //text is drawn bottom-up
		}
	}

	public static Bitmap DrawIconStringWidget(Context context, int width, int height, Bitmap icon, String text, TextPaint textPaint) {
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);
		
		Point iconOffset = getIconOffset(height);
		Point textOffset = getTextOffset(height);
		
		canvas.drawBitmap(icon, iconOffset.x, iconOffset.y, null);
		canvas.drawText(text, textOffset.x, textOffset.y, textPaint);
		
//		if(height==16) {
//			canvas.drawBitmap(icon, 2, 0, null);
//			canvas.drawText(text, 8, 15, textPaint);
//		}
//		else if(height==32) {
////			canvas.drawBitmap(icon, 0, 3, null);
////			canvas.drawText(text, 12, 30, textPaint);
//			canvas.drawText(text, 12, 12, textPaint);
//			canvas.drawBitmap(icon, 0, 14, null);
//		}
		
		return bitmap;
	}
	
    public static boolean isAccessibilityEnabled(Context context) {
	    int accessibilityEnabled = 0;
	    final String ACCESSIBILITY_SERVICE_NAME = context.getPackageName()+"/org.metawatch.manager.MetaWatchAccessibilityService";
	
	    try {
	        accessibilityEnabled = android.provider.Settings.Secure.getInt(context.getContentResolver(),android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
	    }
	    catch (SettingNotFoundException e) {
	        return false;
	    }
	
	    android.text.TextUtils.SimpleStringSplitter mStringColonSplitter = new android.text.TextUtils.SimpleStringSplitter(':');
	
	    if (accessibilityEnabled==1){
	         String settingValue = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
	         if (settingValue != null) {
	        	 android.text.TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
	             splitter.setString(settingValue);
	             while (splitter.hasNext()) {
	                 String accessabilityService = splitter.next();
	                 if (accessabilityService.equalsIgnoreCase(ACCESSIBILITY_SERVICE_NAME)){
	                     return true;
	                 }
	             }
	         }
	    }
	    return false;
	}
    
    public static void appendColoredText(TextView tv, String text, int color) {
    	int start = tv.getText().length();
    	tv.append(text);
    	int end = tv.getText().length();
    	
    	Spannable spannableText = (Spannable) tv.getText();
    	spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
    }
    
    public static ArrayList<String> grabLogCat(String filter) {
    	try {
            Process process = Runtime.getRuntime().exec("logcat -d -s "+filter);
            BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(process.getInputStream()));

            ArrayList<String> log = new ArrayList<String>();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
              log.add(line);
            }
            return log;
          } catch (IOException e) {
        	  return null;
          }
    }
    
	public static void backupUserPrefs(Context context) {
		final File prefsFile = new File(context.getFilesDir(), "../shared_prefs/"+context.getPackageName()+"_preferences.xml");
		final File backupFile = new File(getExternalFilesDir(context, null), "preferenceBackup.xml");
		String error = "";
		Toast toast;
		try {
			FileChannel src = new FileInputStream(prefsFile).getChannel();
			FileChannel dst = new FileOutputStream(backupFile).getChannel();
			
	        dst.transferFrom(src, 0, src.size());
	        src.close();
	        dst.close();
	        
	        toast = Toast.makeText(context, "Backed up user prefs to "+backupFile.getAbsolutePath(), Toast.LENGTH_SHORT);
	        toast.show();
	        return;
		} catch (FileNotFoundException e) {
			error = e.getMessage();
			e.printStackTrace();
		} catch (IOException e) {
			error = e.getMessage();
			e.printStackTrace();
		}
		
		 toast = Toast.makeText(context, "Failed to Back up user prefs to "+backupFile.getAbsolutePath()+ " - "+error, Toast.LENGTH_SHORT);
		 toast.show();
		
	}
    
	@TargetApi(8)
	public static boolean restoreUserPrefs(Context context) {
		final File backupFile = new File(getExternalFilesDir(context, null), "preferenceBackup.xml");
		String error = "";
		
	    try {
	    	
			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(context);
			
			Editor editor = sharedPreferences.edit();
	    	
	    	InputStream inputStream = new FileInputStream(backupFile);
	
	    	DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	
	        Document doc = docBuilder.parse(inputStream);        
	        Element root = doc.getDocumentElement();
	        
	        Node child = root.getFirstChild();
	        while(child!=null) {	
	        	if(child.getNodeType() == Node.ELEMENT_NODE) {
	        	
	        		Element element = (Element)child;
	        		
		        	String type = element.getNodeName();
		        	String name = element.getAttribute("name");
		        	
		        	
		        	if(type.equals("string")) {
		        		String value = element.getTextContent();
		        		editor.putString(name, value);
		        	}
		        	else if(type.equals("boolean")) {
		        		String value = element.getAttribute("value");
		        		editor.putBoolean(name, value.equals("true"));
		        	}
	        	}
		        	
		        child = child.getNextSibling();
	        	
	        }
	        
	        editor.commit();
	        Toast toast = Toast.makeText(context, "Restored user prefs from "+backupFile.getAbsolutePath(), Toast.LENGTH_SHORT);
	        toast.show();
	        
	        return true;
	    
		} catch (FileNotFoundException e) {
			error = e.getMessage();
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			error = e.getMessage();
			e.printStackTrace();
		} catch (SAXException e) {
			error = e.getMessage();
			e.printStackTrace();
		} catch (IOException e) {
			error = e.getMessage();
			e.printStackTrace();
		}
		
		Toast toast = Toast.makeText(context, "Failed to restore user prefs from "+backupFile.getAbsolutePath()+ " - "+error, Toast.LENGTH_SHORT);
		toast.show();
		
		return false;
	}
	
	public static String removeExtension(String filePath) {
	    File f = new File(filePath);
	    // if it's a directory, don't remove the extention
	    if (f.isDirectory()) return f.getName();
	    String name = f.getName();
	    // if it is a hidden file
	    if (name.startsWith(".")) {
	        // if there is no extn, do not rmove one...
	        if (name.lastIndexOf('.') == name.indexOf('.')) return name;
	    }
	    // if there is no extention, don't do anything
	    if (!name.contains(".")) return name;
	    // Otherwise, remove the last 'extension type thing'
	    return name.substring(0, name.lastIndexOf('.'));
	}
	
	@TargetApi(8)
	public static File getExternalFilesDir(Context context, String type) {
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.FROYO) {
			return context.getExternalFilesDir(type);
		}
		
		// Fallback for Android 2.1
		File folder = new File(Environment.getExternalStorageDirectory() + "/Android/data/" + context.getPackageName() + "/Files/" + (type != null ? type : "" ) );
	
		if(!folder.exists())
		{
		    folder.mkdirs();
		} 
		
		return folder;
	}
	
	public static Bitmap invertBitmap(final Bitmap bitmap) {
		int size = bitmap.getWidth() * bitmap.getHeight();
		int pixelArray[] = new int[size];
		bitmap.getPixels(pixelArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
		for(int i=0; i<size; ++i) {
			pixelArray[i] = 0xFFFFFF - pixelArray[i];
		}
		return Bitmap.createBitmap(pixelArray, bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565 );
		//bitmap.setPixels(pixelArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
	}

	public static String ticksToText(Context context, long ticks) {
		return ticksToText(context, ticks, false);
	}
    public static String ticksToText(Context context, long ticks, boolean trimDateIfToday) {   	
    	final Calendar cal = Calendar.getInstance();
    	cal.setTimeInMillis(ticks);
    	Date date = cal.getTime();
    	Date today = new Date();
    	StringBuilder builder = new StringBuilder();
    	if(date.getYear()!=today.getYear() || date.getMonth()!=today.getMonth() || date.getDay()!=today.getDay() || (!trimDateIfToday))
    	{
    		builder.append(DateFormat.getDateFormat(context).format(date));
    		builder.append(" ");
    	}
    	builder.append(DateFormat.getTimeFormat(context).format(date));
    	return builder.toString();
    }
    
    public static StaticLayout buildText(Context context, String text, int width, Layout.Alignment alignment, int textCol,  FontSize size ) {
		TextPaint tp = new TextPaint();
		tp.setColor(textCol);
		FontInfo info = FontCache.instance(context).Get(size);
		tp.setTextSize(info.size);
		tp.setTypeface(info.face);
    	
    	return new StaticLayout(text, tp, width, alignment, 1.0f, 0, false);
    }
    
    public static void autoText(Context context, Canvas canvas, String text, int tX, int tY, int width, int height, Layout.Alignment alignment, int textCol) {
    	
    	StaticLayout layout = buildText(context, text, width, alignment, textCol, FontSize.LARGE);
    	if(layout.getHeight()>height) {
    		layout = buildText(context, text, width, alignment, textCol, FontSize.MEDIUM);
    	}
    	if(layout.getHeight()>height) {
    		layout = buildText(context, text, width, alignment, textCol, FontSize.SMALL);
    	}
    	
    	int textHeight = layout.getHeight();
		int textY = tY+(height/2) - (textHeight/2);
		if(textY<tY) {
			textY=tY;
		}
		
		canvas.save();		
		canvas.translate(tX, textY); //position the text
		//canvas.clipRect(0,0,width,height);
		layout.draw(canvas);
		canvas.restore();	
    }
    
    // String.isEmpty doesn't work on 2.1
    public static boolean stringIsEmpty(final String string) {
    	return string.trim().equals("");
    }
	
}
