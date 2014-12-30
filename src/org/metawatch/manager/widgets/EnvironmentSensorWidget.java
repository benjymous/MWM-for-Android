package org.metawatch.manager.widgets;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.metawatch.manager.FontCache;
import org.metawatch.manager.Idle;
import org.metawatch.manager.MetaWatch;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.Monitors;
import org.metawatch.manager.Settings;
import org.metawatch.manager.Utils;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.apps.ApplicationBase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Paint.Align;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.util.Log;
import android.widget.RemoteViews;

public class EnvironmentSensorWidget implements InternalWidget, SensorEventListener {

	public final static String id_0 = "environment_sensor_24_32";
	final static String desc_0 = "Environment Sensor (24x32)";

	private Context context = null;
	private TextPaint paintSmallCenter;
	private TextPaint paintSmallRight;

	private Float ambientTemperature = null;
	private Float relativeHumidity = null;
	private Float prevPressure = null;
	private Float pressure = null;
	private final float minPressureDiff = 1;
	private final int minPressureUnchangedCount = 3; 
	private int pressureDirection=0;
	private int pressureUnchangedCount=0;
	private SensorManager sensorManager;
	private Sensor ambientTemperatureSensor;
	private Sensor pressureSensor;
	private Sensor relativeHumiditySensor;
	private long nextUpdate=0;
	private long nextPressureDirectionUpdate=0;
	

	public void init(Context context, ArrayList<CharSequence> widgetIds) {
		
		if (this.context!=null)
			return;
		this.context = context;

		paintSmallCenter = new TextPaint();
		paintSmallCenter.setColor(Color.BLACK);
		paintSmallCenter.setTextSize(FontCache.instance(context).Small.size);
		paintSmallCenter.setTypeface(FontCache.instance(context).Small.face);
		paintSmallCenter.setTextAlign(Align.CENTER);

		paintSmallRight = new TextPaint();
		paintSmallRight.setColor(Color.BLACK);
		paintSmallRight.setTextSize(FontCache.instance(context).Small.size);
		paintSmallRight.setTypeface(FontCache.instance(context).Small.face);
		paintSmallRight.setTextAlign(Align.RIGHT);

	    sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		try {
			Field f = Sensor.class.getDeclaredField("TYPE_AMBIENT_TEMPERATURE");
			int sensorValue = (int) f.getInt(null);
		    ambientTemperatureSensor = sensorManager.getDefaultSensor(sensorValue);
		    sensorManager.registerListener(this, ambientTemperatureSensor, SensorManager.SENSOR_DELAY_UI);
		} catch (Exception e) {
		}
	    pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
	    sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI);
		try {
			Field f = Sensor.class.getDeclaredField("TYPE_RELATIVE_HUMIDITY");
			int sensorValue = (int) f.getInt(null);
			relativeHumiditySensor = sensorManager.getDefaultSensor(sensorValue);
		    sensorManager.registerListener(this, relativeHumiditySensor, SensorManager.SENSOR_DELAY_UI);
		} catch (Exception e) {
		}
		
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		prevPressure = sharedPreferences.getFloat("EnvironmentSensorPrevPressure", 0);
		if (prevPressure==0) prevPressure=null;

	}

	public void shutdown() {
		paintSmallCenter = null;
		paintSmallRight = null;
	    sensorManager.unregisterListener(this);
	}

	public void refresh(ArrayList<CharSequence> widgetIds) {
	}

	public void get(ArrayList<CharSequence> widgetIds,
			Map<String, WidgetData> result) {

		if (widgetIds == null || widgetIds.contains(id_0)) {
			result.put(id_0, GenWidget(id_0));
		}
	}

	private InternalWidget.WidgetData GenWidget(String widget_id) {
		InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

		String iconFile = "";
		String arrowUpFile = "";
		String arrowDownFile = "";
		if (widget_id == id_0) {
			widget.id = id_0;
			widget.description = desc_0;
			widget.width = 24;
			widget.height = 32;
			iconFile = "idle_environment.bmp";
			arrowDownFile = "idle_environment_arrow_down.bmp";
			arrowUpFile = "idle_environment_arrow_up.bmp";
		}

		// Bitmap icon = Utils.getBitmap(context, iconFile);

		widget.priority = 1;
		widget.bitmap = Bitmap.createBitmap(widget.width, widget.height,
				Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(widget.bitmap);
		canvas.drawColor(Color.WHITE);

		int height = 32;
		TextPaint textPaint = paintSmallRight;
		Point textOffset = Utils.getTextOffset(height);
		Bitmap icon = Utils.getBitmap(context, iconFile);
		canvas.drawBitmap(icon, 1, 3, null);
		String value="NA";
		if (ambientTemperature!=null)
			value=String.format(Locale.US,"%2.0f°", ambientTemperature);
		canvas.drawText(value, widget.width-1, 9, textPaint);
		value="NA";
		if (relativeHumidity!=null)
			value=String.format(Locale.US,"%2.0f%%", relativeHumidity);
		canvas.drawText(value, widget.width-1, 19, textPaint);
		//pressure=Float.valueOf("1999");
		if (pressureDirection!=0) {
			if (pressureDirection>0) {
				icon = Utils.getBitmap(context, arrowUpFile);
			} else {
				icon = Utils.getBitmap(context, arrowDownFile);
			}
			textOffset.x=textOffset.x-icon.getWidth()+1;
			canvas.drawBitmap(icon, widget.width-icon.getWidth()-1, widget.height-icon.getHeight()-2, null);
		}
		value="NA";
		if (pressure!=null)
			value=String.format(Locale.US,"%4.0f", pressure);
		canvas.drawText(value, textOffset.x, textOffset.y, paintSmallCenter);
		return widget;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		boolean forceUpdate=false;
		if (event.sensor==ambientTemperatureSensor) {
			if (ambientTemperature==null)
				forceUpdate=true;
			ambientTemperature=event.values[0];	
		}
		if (event.sensor==pressureSensor) {
			if (pressure==null)
				forceUpdate=true;
			pressure=event.values[0];		
			if (prevPressure==null)
				prevPressure=pressure;
			else {
				if (System.currentTimeMillis()>nextPressureDirectionUpdate) {
					if (Math.abs(prevPressure-pressure)>minPressureDiff) {
						if (pressure<prevPressure) {
							pressureDirection=-1;
						} else {
							pressureDirection=+1;						
						}
						pressureUnchangedCount=0;
					} else {
						pressureUnchangedCount++;
						if (pressureUnchangedCount>=minPressureUnchangedCount) {
							pressureDirection=0;
						}
					}
					prevPressure=pressure;
					SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
					sharedPreferencesEditor.putFloat("EnvironmentSensorPrevPressure", prevPressure);
					sharedPreferencesEditor.commit();
					nextPressureDirectionUpdate=System.currentTimeMillis()+60*60*1000;
				}
			}
		}
		if (event.sensor==relativeHumiditySensor) {
			if (relativeHumidity==null)
				forceUpdate=true;
			relativeHumidity=event.values[0];		
		}
		if ((System.currentTimeMillis()>nextUpdate)||(forceUpdate)) {
			Idle.updateIdle(context, true);
			nextUpdate=System.currentTimeMillis()+5*60*1000;
		}
	}

}
