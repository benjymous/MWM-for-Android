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
 * Protocol.java                                                             *
 * Protocol                                                                  *
 * Basic low level protocol commands                                         *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchBuffers;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.format.DateFormat;

public class Protocol {
    
    private byte[][][] LCDDiffBuffer = new byte[3][48][30];

    private Context mContext;

    private static Protocol mInstance;

    public static Protocol getInstance(Context context) {
	if (mInstance == null)
	    mInstance = new Protocol(context);
	return mInstance;
    }

    public void destroy() {
	mInstance = null;
    }

    private Protocol(Context context) {
	mContext = context;
    }

    public void resetLCDDiffBuffer() {
	LCDDiffBuffer = new byte[3][48][30];
    }

    public boolean sendLcdBitmap(Bitmap bitmap, int bufferType) {
	if (bitmap == null || bitmap.getWidth() != 96 || bitmap.getHeight() != 96) {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Protocol.sendLcdBitmap - null or non 96px bitmap!");
	    return false;
	}

	if (Preferences.dumpWatchScreenshots)
	    Utils.dumpBitmapToSdCard(bitmap, Environment.getExternalStorageDirectory().getPath() + "MWM_" + System.currentTimeMillis() + ".png");

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.sendLcdBitmap()");
	int pixelArray[] = new int[96 * 96];
	bitmap.getPixels(pixelArray, 0, 96, 0, 0, 96, 96);

	return sendLcdArray(pixelArray, bufferType);
    }

    public boolean sendLcdArray(int[] pixelArray, int bufferType) {
	byte send[] = new byte[1152];

	for (int i = 0; i < 1152; i++) {
	    int p[] = new int[8];

	    for (int j = 0; j < 8; j++) {
		if (pixelArray[i * 8 + j] == Color.WHITE)
		    /*
		     * if (Preferences.invertLCD) p[j] = 1; else
		     */
		    p[j] = 0;
		else
		    /*
		     * if (Preferences.invertLCD) p[j] = 0; else
		     */
		    p[j] = 1;
	    }
	    send[i] = (byte) (p[7] * 128 + p[6] * 64 + p[5] * 32 + p[4] * 16 + p[3] * 8 + p[2] * 4 + p[1] * 2 + p[0] * 1);
	}
	return sendLcdBuffer(send, bufferType);
    }

    public boolean sendLcdBuffer(byte[] buffer, int bufferType) {
	if (MetaWatchService.connectionState != MetaWatchService.ConnectionState.CONNECTED)
	    return false;

	int i = 0;
	// if (bufferType == MetaWatchService.WatchBuffers.IDLE &&
	// idleShowClock)
	// i = 30;

	int sentLines = 0;
	for (; i < 96; i += 2) {
	    byte[] bytes = new byte[30];

	    bytes[0] = 0x01;
	    bytes[1] = (byte) (bytes.length + 2); // packet length
	    bytes[2] = eMessageType.WriteBuffer.msg;
	    bytes[3] = (byte) (bufferType & 3);

	    bytes[4] = (byte) i; // row A
	    for (int j = 0; j < 12; j++)
		bytes[j + 5] = buffer[i * 12 + j];

	    bytes[4 + 13] = (byte) (i + 1); // row B
	    for (int j = 0; j < 12; j++)
		bytes[j + 5 + 13] = buffer[i * 12 + j + 12];

	    // Only send the row packet if the data's changed since the
	    // last time we sent it
	    if (!Arrays.equals(LCDDiffBuffer[bufferType][i / 2], bytes)) {
		enqueue(bytes);
		LCDDiffBuffer[bufferType][i / 2] = bytes;
		sentLines += 2;
	    }
	}
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Sent " + sentLines + "/96");

	return (sentLines > 0);
    }

    public void enqueue(final byte[] bytes) {
	if (MetaWatchService.fakeWatch)
	    return;
	send(bytes);
    }

    public void send(final byte[] bytes) {
	if (bytes == null)
	    return;
	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	try {
	    byteArrayOutputStream.write(bytes);
	    byteArrayOutputStream.write(crc(bytes));
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return;
	}

	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
	if (sharedPreferences.getBoolean("logPacketDetails", false)) {
	    String str = "sending: ";
	    byte[] b = byteArrayOutputStream.toByteArray();
	    for (int i = 0; i < b.length; i++) {
		str += "0x" + Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1) + ", ";
	    }
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, str);
	}

	MetaWatchService.sentBytes(mContext, byteArrayOutputStream.toByteArray());
    }

    public void sendAdvanceHands(int hour, int minute, int second) {
	try {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Protocol.sendAdvanceHands()");

	    byte[] bytes = new byte[7];

	    bytes[0] = eMessageType.start;
	    bytes[1] = (byte) (bytes.length + 2); // length
	    bytes[2] = eMessageType.AdvanceWatchHandsMsg.msg; // advance watch
							      // hands
	    bytes[3] = 0x00;

	    bytes[4] = (byte) hour;
	    bytes[5] = (byte) minute;
	    bytes[6] = (byte) second;

	    enqueue(bytes);

	} catch (Exception x) {
	}
    }

    public void setRealTimeClock(Context context) {
	try {
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Protocol.setRealTimeClock()");

	    Date date = new Date();
	    Calendar calendar = Calendar.getInstance();
	    calendar.setTime(date);
	    int year = calendar.get(Calendar.YEAR);

	    byte[] bytes = new byte[12];

	    bytes[0] = eMessageType.start;
	    bytes[1] = (byte) (bytes.length + 2); // length
	    bytes[2] = eMessageType.SetRealTimeClock.msg; // set rtc
	    bytes[3] = 0x00; // not used

	    bytes[4] = (byte) (year / 256);
	    bytes[5] = (byte) (year % 256);
	    bytes[6] = (byte) (calendar.get(Calendar.MONTH) + 1);
	    bytes[7] = (byte) calendar.get(Calendar.DAY_OF_MONTH);
	    bytes[8] = (byte) (calendar.get(Calendar.DAY_OF_WEEK) - 1);
	    bytes[9] = (byte) calendar.get(Calendar.HOUR_OF_DAY);
	    bytes[10] = (byte) calendar.get(Calendar.MINUTE);
	    bytes[11] = (byte) (calendar.get(Calendar.SECOND) + Monitors.getInstance().rtcOffset);

	    send(bytes);

	} catch (Exception x) {
	}
    }

    public void getRealTimeClock() {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.getRealTimeClock()");
	byte[] bytes = new byte[4];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.GetRealTimeClock.msg;
	bytes[3] = 0;

	Monitors.getInstance().getRTCTimestamp = System.currentTimeMillis();
	send(bytes);
    }

    public byte[] crc(byte[] bytes) {
	byte[] result = new byte[2];
	short crc = (short) 0xFFFF;
	for (int j = 0; j < bytes.length; j++) {
	    byte c = bytes[j];
	    for (int i = 7; i >= 0; i--) {
		boolean c15 = ((crc >> 15 & 1) == 1);
		boolean bit = ((c >> (7 - i) & 1) == 1);
		crc <<= 1;
		if (c15 ^ bit)
		    crc ^= 0x1021; // 0001 0000 0010 0001 (0, 5, 12)
	    }
	}
	int crc2 = crc - 0xffff0000;
	result[0] = (byte) (crc2 % 256);
	result[1] = (byte) (crc2 / 256);
	return result;
    }

    public Bitmap createTextBitmap(Context context, String text) {

	FontCache.FontInfo font = FontCache.instance(context).Get();

	Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(bitmap);
	Paint paint = new Paint();
	paint.setColor(Color.BLACK);
	paint.setTextSize(font.size);
	if (Preferences.notificationCenter) {
	    paint.setTextAlign(Align.CENTER);
	}
	paint.setTypeface(font.face);
	canvas.drawColor(Color.WHITE);
	canvas = breakText(canvas, text, paint, 0, 0);
	/*
	 * FileOutputStream fos = new FileOutputStream("/sdcard/test.png"); image.compress(Bitmap.CompressFormat.PNG, 100, fos); fos.close(); if (Preferences.logging) Log.d("ow", "bmp ok");
	 */
	return bitmap;
    }

    public Canvas breakText(Canvas canvas, String text, Paint pen, int x, int y) {
	TextPaint textPaint = new TextPaint(pen);
	StaticLayout staticLayout = new StaticLayout(text, textPaint, 94, android.text.Layout.Alignment.ALIGN_NORMAL, 1.3f, 0, false);

	int top = 1;
	int left = 1;
	if (Preferences.notificationCenter) {
	    top = 48 - (staticLayout.getHeight() / 2);
	    if (top < 1) {
		top = 1;
	    }
	    left = 48;
	}

	canvas.translate(left, top); // position the text
	staticLayout.draw(canvas);
	return canvas;
    }

    public void loadTemplate(int mode) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.loadTemplate(): mode=" + mode);
	byte[] bytes = new byte[5];

	bytes[0] = 0x01;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.LoadTemplate.msg; // load template
	bytes[3] = (byte) mode;

	bytes[4] = (byte) 0; // write all "0"

	enqueue(bytes);
    }

    public void updateLcdDisplay(int bufferType) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.updateLcdDisplay(): bufferType=" + bufferType);
	// byte[] bytes = new byte[MetaWatchService.watchGen ==
	// MetaWatchService.WatchGen.GEN2 ? 5 : 4];
	byte[] bytes = new byte[4];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.UpdateDisplay.msg; // update display

	if (MetaWatchService.watchGen == MetaWatchService.WatchGen.GEN2) {
	    // bytes[3] = (byte) (bufferType);

	    final int mode = bufferType;
	    final boolean showGrid = false;
	    final int pageId = 0;
	    final boolean changePage = false;

	    int baseCode = 0x00;
	    if (mode == 0) {
		baseCode = 0x80;
	    }
	    int code = baseCode | ((showGrid ? 0 : 1) << 6) | ((changePage ? 1 : 0) << 5) | (pageId << 2) | mode;
	    bytes[3] = (byte) (code & 0xFF);
	    // bytes[4] = 0x00;

	} else
	    bytes[3] = (byte) (bufferType + 16); // Undocumented, but fw 3.1.0
						 // and earlier seems to need
						 // this!

	enqueue(bytes);

	if (MetaWatchService.watchGen == MetaWatchService.WatchGen.GEN2) {
	    changeMode(bufferType);
	}
    }

    public void oledChangeMode(int bufferType) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.OledChangeMode(): bufferType=" + bufferType);
	byte[] bytes = new byte[4];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.OledChangeModeMsg.msg; // update display
	bytes[3] = (byte) (bufferType);

	enqueue(bytes);
    }

    public void vibrate(int on, int off, int cycles) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.vibrate(): on=" + on + " off=" + off + " cycles=" + cycles);
	byte[] bytes = new byte[10];

	bytes[0] = eMessageType.start;
	bytes[1] = 12; // delka
	bytes[2] = eMessageType.SetVibrateMode.msg; // set vibrate
	bytes[3] = 0x00; // unused

	bytes[4] = 0x01; // enabled
	bytes[5] = (byte) (on % 256);
	bytes[6] = (byte) (on / 256);
	bytes[7] = (byte) (off % 256);
	bytes[8] = (byte) (off / 256);
	bytes[9] = (byte) cycles;

	enqueue(bytes);
    }

    public void writeBuffer() {

	// for (int j = 0; j < 96; j = j+2) {
	byte[] bytes = new byte[17];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.WriteBuffer.msg; // write lcd buffer
	// bytes[3] = 0x02; // notif, two lines
	// bytes[3] = 18;
	bytes[3] = 0;
	// bytes[3] = 16;

	bytes[4] = 31;

	bytes[5] = 15;
	bytes[6] = 15;
	bytes[7] = 15;
	bytes[8] = 15;
	bytes[9] = 15;
	bytes[10] = 15;
	bytes[11] = 15;
	bytes[12] = 15;
	bytes[13] = 15;
	bytes[14] = 15;
	bytes[15] = 15;
	bytes[16] = 15;

	enqueue(bytes);
	// processSendQueue();
	// }
    }

    public void enableButton(int button, int type, int code, int mode) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.enableButton(): button=" + button + " type=" + type + " code=" + code);
	byte[] bytes = new byte[9];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.EnableButtonMsg.msg; // enable button
	bytes[3] = 0; // not used

	bytes[4] = (byte) mode; // (idle,etc)
	bytes[5] = (byte) button;
	bytes[6] = (byte) type; // immediate
	bytes[7] = 0x34;
	bytes[8] = (byte) code;

	enqueue(bytes);
    }

    public void disableButton(int button, int type, int mode) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.disableButton(): button=" + button + " type=" + type);
	byte[] bytes = new byte[7];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.DisableButtonMsg.msg; // disable button
	bytes[3] = 0; // not used

	bytes[4] = (byte) mode; // (idle,etc)
	bytes[5] = (byte) button;
	bytes[6] = (byte) type; // immediate

	enqueue(bytes);
    }

    public void readButtonConfiguration() {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.readButtonConfiguration()");
	byte[] bytes = new byte[9];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.ReadButtonConfigMsg.msg; //
	bytes[3] = 0; // not used

	bytes[4] = 0;
	bytes[5] = 1;
	bytes[6] = 2; // press type
	bytes[7] = 0x34;
	bytes[8] = 0;

	enqueue(bytes);
    }

    public void configureMode() {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.configureMode()");
	byte[] bytes = new byte[6];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.ConfigureMode.msg; //
	bytes[3] = 0;

	bytes[4] = 10;
	bytes[5] = (byte) (MetaWatchService.Preferences.invertLCD ? 1 : 0); // invert

	enqueue(bytes);
    }

    public void configureIdleBufferSize(boolean showClock) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.configureIdleBufferSize(" + showClock + ")");
	
	byte[] bytes = new byte[5];
	
	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.ConfigureIdleBufferSize.msg;
	bytes[3] = 0;
	bytes[4] = (byte) (showClock ? 0 : 1);
	
	enqueue(bytes);
    }

    public void getDeviceType() {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.getDeviceType()");
	byte[] bytes = new byte[4];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.GetDeviceType.msg;
	bytes[3] = 0;

	enqueue(bytes);
    }

    public void readBatteryVoltage() {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.readBatteryVoltage()");
	byte[] bytes = new byte[4];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.ReadBatteryVoltageMsg.msg;
	bytes[3] = 0;

	enqueue(bytes);
    }

    public void readLightSensor() {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.readLightSensor()");
	byte[] bytes = new byte[4];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.ReadLightSensorMsg.msg;
	bytes[3] = 0;

	enqueue(bytes);
    }

    public void setTimeDateFormat(Context context) {
	// Set the watch to 12h or 24h mode, depending on watch setting
	if (DateFormat.is24HourFormat(context)) {
	    setNvalTime(true);
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Setting watch to 24h format");
	} else {
	    setNvalTime(false);
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Setting watch to 12h format");
	}

	char[] order = DateFormat.getDateFormatOrder(context);
	if (order[0] == DateFormat.DATE) {
	    setNvalDate(true);
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Setting watch to ddmm format");
	} else {
	    setNvalDate(false);
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "Setting watch to mmdd format");
	}
    }

    public void setNvalLcdInvert(boolean invert) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.setNvalLcdInvert()");
	byte[] bytes = new byte[8];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.NvalOperationMsg.msg; // nval operations
	bytes[3] = 0x02; // write

	bytes[4] = 0x03;
	bytes[5] = 0x00;
	bytes[6] = 0x01; // size
	if (invert)
	    bytes[7] = 0x01; // 24 hour mode
	else
	    bytes[7] = 0x00; // 12 hour mode

	enqueue(bytes);
    }

    public void setNvalTime(boolean militaryTime) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.setNvalTime()");
	byte[] bytes = new byte[8];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.NvalOperationMsg.msg; // nval operations
	bytes[3] = 0x02; // write

	bytes[4] = 0x09;
	bytes[5] = 0x20;
	bytes[6] = 0x01; // size
	if (militaryTime)
	    bytes[7] = 0x01; // 24 hour mode
	else
	    bytes[7] = 0x00; // 12 hour mode

	enqueue(bytes);
    }

    public void setNvalDate(boolean dayFirst) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.setNvalDate()");
	byte[] bytes = new byte[8];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.NvalOperationMsg.msg; // nval operations
	bytes[3] = 0x02; // write

	bytes[4] = 0x0a;
	bytes[5] = 0x20;
	bytes[6] = 0x01; // size
	if (dayFirst)
	    bytes[7] = 0x01; // 24 hour mode
	else
	    bytes[7] = 0x00; // 12 hour mode

	enqueue(bytes);
    }

    public void ledChange(boolean ledOn) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.ledChange()");
	byte[] bytes = new byte[4];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.LedChange.msg;
	bytes[3] = ledOn ? (byte) 0x01 : (byte) 0x00;

	enqueue(bytes);
    }

    public byte[] createOled1line(Context context, Bitmap icon, String line) {
	int offset = 0;

	if (icon != null)
	    offset += 17;

	Bitmap image = Bitmap.createBitmap(80, 16, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(image);
	Paint paint = new Paint();
	paint.setColor(Color.BLACK);
	paint.setTextSize(FontCache.instance(context).Large.size);
	paint.setTypeface(FontCache.instance(context).Large.face);
	canvas.drawColor(Color.WHITE);
	canvas.drawText(line, offset, 14, paint);

	if (icon != null) {
	    canvas.drawBitmap(icon, 0, 0, null);
	}

	int poleInt[] = new int[16 * 80];
	image.getPixels(poleInt, 0, 80, 0, 0, 80, 16);

	byte[] display = new byte[160];

	for (int i = 0; i < 160; i++) {
	    boolean[] column = new boolean[8];
	    for (int j = 0; j < 8; j++) {
		if (i < 80) {
		    if (poleInt[80 * j + i] == Color.WHITE)
			column[j] = false;
		    else
			column[j] = true;
		} else {
		    if (poleInt[80 * 8 + 80 * j + i - 80] == Color.WHITE)
			column[j] = false;
		    else
			column[j] = true;
		}
	    }
	    for (int j = 0; j < 8; j++) {
		if (column[j])
		    display[i] += Math.pow(2, j);
	    }
	}

	return display;
    }

    public byte[] createOled2lines(Context context, String line1, String line2) {
	int offset = 0;

	/* Convert newlines to spaces */
	line1 = line1.replace('\n', ' ');
	line2 = line2.replace('\n', ' ');

	Bitmap image = Bitmap.createBitmap(80, 16, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(image);
	Paint paint = new Paint();
	paint.setColor(Color.BLACK);
	paint.setTextSize(FontCache.instance(context).Small.size);
	paint.setTypeface(FontCache.instance(context).Small.face);
	canvas.drawColor(Color.WHITE);
	canvas.drawText(line1, offset, 7, paint);
	canvas.drawText(line2, offset, 15, paint);

	int poleInt[] = new int[16 * 80];
	image.getPixels(poleInt, 0, 80, 0, 0, 80, 16);

	byte[] display = new byte[160];

	for (int i = 0; i < 160; i++) {
	    boolean[] column = new boolean[8];
	    for (int j = 0; j < 8; j++) {
		if (i < 80) {
		    if (poleInt[80 * j + i] == Color.WHITE)
			column[j] = false;
		    else
			column[j] = true;
		} else {
		    if (poleInt[80 * 8 + 80 * j + i - 80] == Color.WHITE)
			column[j] = false;
		    else
			column[j] = true;
		}
	    }
	    for (int j = 0; j < 8; j++) {
		if (column[j])
		    display[i] += Math.pow(2, j);
	    }
	}

	return display;
    }

    public int createOled2linesLong(Context context, String line, byte[] display) {
	int offset = 0 - 79;

	/* Replace newlines with spaces */
	line = line.replace('\n', ' ');

	final int width = 800;

	Bitmap image = Bitmap.createBitmap(width, 8, Bitmap.Config.RGB_565);
	Canvas canvas = new Canvas(image);
	Paint paint = new Paint();
	paint.setColor(Color.BLACK);
	paint.setTextSize(FontCache.instance(context).Small.size);
	paint.setTypeface(FontCache.instance(context).Small.face);
	canvas.drawColor(Color.WHITE);
	canvas.drawText(line, offset, 7, paint);

	int poleInt[] = new int[8 * width];
	image.getPixels(poleInt, 0, width, 0, 0, width, 8);

	for (int i = 0; i < width; i++) {
	    boolean[] column = new boolean[8];
	    for (int j = 0; j < 8; j++) {
		if (poleInt[width * j + i] == Color.WHITE)
		    column[j] = false;
		else
		    column[j] = true;
	    }
	    for (int j = 0; j < 8; j++) {
		if (column[j])
		    display[i] += Math.pow(2, j);
	    }
	}

	return (int) paint.measureText(line) - 79;
    }

    public void sendOledBitmap(Bitmap bitmap, int bufferType, int page) {
	if (bitmap == null)
	    return;

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.sendOledBitmap()");

	int pixelArray[] = new int[16 * 80];
	bitmap.getPixels(pixelArray, 0, 80, 0, 0, 80, 16);

	sendOledArray(pixelArray, bufferType, page);
    }

    private void sendOledArray(int[] pixelArray, int bufferType, int page) {
	byte[] send = new byte[160];

	for (int i = 0; i < 160; i++) {
	    boolean[] column = new boolean[8];
	    for (int j = 0; j < 8; j++) {
		if (i < 80) {
		    if (pixelArray[80 * j + i] == Color.WHITE)
			column[j] = false;
		    else
			column[j] = true;
		} else {
		    if (pixelArray[80 * 8 + 80 * j + i - 80] == Color.WHITE)
			column[j] = false;
		    else
			column[j] = true;
		}
	    }
	    for (int j = 0; j < 8; j++) {
		if (column[j])
		    send[i] += Math.pow(2, j);
	    }
	}

	sendOledBuffer(send, bufferType, page, false);
    }

    public void sendOledBuffer(byte[] display, int bufferType, int page, boolean scroll) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.sendOledBuffer()");
	try {

	    byte[] bytes;

	    for (int a = 0; a < 160; a += 20) {
		bytes = new byte[27];
		bytes[0] = eMessageType.start;
		bytes[1] = (byte) (bytes.length + 2); // length
		bytes[2] = eMessageType.OledWriteBufferMsg.msg; // oled write
		if (scroll && bufferType == WatchBuffers.NOTIFICATION)
		    bytes[3] = (byte) 0x82; // notification + scroll
		else
		    bytes[3] = (byte) bufferType; // notification

		bytes[4] = (byte) page;

		bytes[5] = (byte) a; // row
		bytes[6] = 0x14; // size

		System.arraycopy(display, a, bytes, 7, 20);

		enqueue(bytes);
	    }

	    updateOledDisplay(page == 0, bufferType, scroll);

	} catch (Exception x) {
	    if (Preferences.logging)
		Log.e(MetaWatchStatus.TAG, "Protocol.sendOledBuffer(): exception occured", x);
	}
    }

    public void updateOledDisplay(boolean top, int bufferType, boolean scroll) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.updateOledNotification(): top=" + top + " scroll=" + scroll);
	byte[] bytes = new byte[7];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.OledWriteBufferMsg.msg; // oled write
	if (scroll && bufferType == WatchBuffers.NOTIFICATION)
	    bytes[3] = (byte) 0xC2; // notification, activate, scroll
	else
	    bytes[3] = (byte) (0x40 + bufferType); // activate

	if (top)
	    bytes[4] = 0x00; // top page
	else
	    bytes[4] = 0x01; // bottom page
	bytes[5] = 0x00; // row
	bytes[6] = 0x00; // size

	enqueue(bytes);
    }

    public void updateOledsNotification() {
	updateOledDisplay(true, WatchBuffers.NOTIFICATION, false);
	updateOledDisplay(false, WatchBuffers.NOTIFICATION, false);
    }

    public void sendOledBuffer(boolean startScroll) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.sendOledBuffer(): startScroll=" + startScroll);
	byte[] bytes = new byte[25];
	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.OledWriteScrollBufferMsg.msg; // write oled
							      // buffer
	if (startScroll)
	    bytes[3] = 0x02; // not last, start
	else
	    bytes[3] = 0x00; // not last

	bytes[4] = 20; // size
	for (int i = 0; i < 20; i++)
	    bytes[5 + i] = (byte) 0xAA;

	enqueue(bytes);
    }

    public void sendOledBufferPart(byte[] display, int start, int length, boolean startScroll, boolean last) {

	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.sendOledBufferPart(): sending oled buffer part, start: " + start + ", length: " + length);

	for (int j = start; j < start + length; j += 20) {
	    byte[] bytes = new byte[25];
	    bytes[0] = eMessageType.start;
	    bytes[1] = (byte) (bytes.length + 2); // length
	    bytes[2] = eMessageType.OledWriteScrollBufferMsg.msg; // write oled
								  // buffer
	    bytes[3] = 0x00; // not last

	    if (j + 20 >= start + length) { // is last packet
		if (startScroll)
		    bytes[3] = 0x02; // not last, start
		if (last)
		    bytes[3] = 0x01; // last
		if (startScroll && last)
		    bytes[3] = 0x03; // last, start
	    }

	    bytes[4] = 20; // size
	    for (int i = 0; i < 20; i++)
		bytes[5 + i] = display[j + i];

	    enqueue(bytes);
	}

    }

    public void changeMode(int mode) {
	if (Preferences.logging)
	    Log.d(MetaWatchStatus.TAG, "Protocol.changeMode()");
	byte[] bytes = new byte[4];

	bytes[0] = eMessageType.start;
	bytes[1] = (byte) (bytes.length + 2); // length
	bytes[2] = eMessageType.ChangeModeMsg.msg;
	bytes[3] = (byte) (mode + 0x10);

	enqueue(bytes);
    }
}