package org.metawatch.manager.widgets;

import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

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
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextPaint;
import android.util.Log;

public class WithingsWidget implements InternalWidget {

	public final static String id_0 = "withings_24_32";
	final static String desc_0 = "Withings Body Metric (24x32)";
	
	private Context context = null;
	private TextPaint paintSmall;
	private TextPaint paintSmallNumerals;

	private static OAuthConsumer consumer = null;
	private static OAuthProvider provider = null;
	private static String accessToken = null;
	private static String accessTokenSecret = null;
	private static String userID = null;
	private static Object wakeupUpdateThread = new Object();
	private static boolean forceUpdate = false;

	private double weightValue = 0;
	private double fatRatioValue = 0;
	private long nextUpdate = 0;

	private Thread updateThread = new Thread(new Runnable() {
		public void run() {
			while (true) {
				nextUpdate = System.currentTimeMillis()+60*60*1000;
				if ((accessToken!=null)&&(accessTokenSecret!=null)&&(userID!=null)) {
					try {
						OAuthConsumer consumer = new CommonsHttpOAuthConsumer(MetaWatchService.Preferences.withingsAPIKey, MetaWatchService.Preferences.withingsAPISecret);
						consumer.setTokenWithSecret(accessToken, accessTokenSecret);
						String getURL = "http://wbsapi.withings.net/measure?action=getmeas&limit=1&userid=" + userID;
						HttpGet request = new HttpGet(consumer.sign(getURL));
						HttpClient httpClient = new DefaultHttpClient();
						BasicResponseHandler responseHandler = new BasicResponseHandler();
						HttpResponse response = httpClient.execute(request);
						JSONObject result = new JSONObject(responseHandler.handleResponse(response));
						if (result.getInt("status")!=0) {
							Log.d(MetaWatch.TAG,
									"WithingsWidget.updateThread.run(): Unknown status in WBS response from Withings: "
											+ result.get("status"));	
							return;
						}
						JSONArray measures = result.getJSONObject("body").getJSONArray("measuregrps").getJSONObject(0).getJSONArray("measures");
						for (int i=0;i<measures.length();i++) {
							JSONObject measure=measures.getJSONObject(i);
							if (measure.getInt("type")==1) {
								weightValue=measure.getInt("value")*Math.pow(10, measure.getInt("unit"));
							}
							if (measure.getInt("type")==6) {
								fatRatioValue=measure.getInt("value")*Math.pow(10, measure.getInt("unit"));
							}
						}
						Idle.updateIdle(context, true);
						SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();
						sharedPreferencesEditor.putFloat("WithingsWeightValue", (float)weightValue);
						sharedPreferencesEditor.putFloat("WithingsFatRatioValue", (float)fatRatioValue);
						sharedPreferencesEditor.commit();
					} catch (Exception e) {
						if (Preferences.logging)
							Log.d(MetaWatch.TAG,
									"WithingsWidget.updateThread.run(): Exception while retrieving WBS response from Withings: "
											+ e.toString());
					}
				}
				while (System.currentTimeMillis()<nextUpdate) {
					synchronized (WithingsWidget.wakeupUpdateThread) {
						if (!WithingsWidget.forceUpdate) {
							try {
								WithingsWidget.wakeupUpdateThread.wait(60*60*1000);
							} catch (InterruptedException e) {};
						}
						if (WithingsWidget.forceUpdate) {
							nextUpdate=System.currentTimeMillis();								
						}
						WithingsWidget.forceUpdate=false;
					}
				}
			}
		}
	});

	public void init(Context context, ArrayList<CharSequence> widgetIds) {
		
		if (this.context!=null)
			return;
		this.context = context;

		paintSmall = new TextPaint();
		paintSmall.setColor(Color.BLACK);
		paintSmall.setTextSize(FontCache.instance(context).Small.size);
		paintSmall.setTypeface(FontCache.instance(context).Small.face);
		paintSmall.setTextAlign(Align.CENTER);

		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		accessToken = sharedPreferences.getString("WithingsAccessToken", null);
		accessTokenSecret = sharedPreferences.getString(
				"WithingsAccessTokenSecret", null);
		userID = sharedPreferences.getString("WithingsUserID", null);
		weightValue=sharedPreferences.getFloat("WithingsWeightValue", 0);
		fatRatioValue=sharedPreferences.getFloat("WithingsFatRatioValue", 0);

		// Keep updating values
		updateThread.start();
	}

	public void shutdown() {
		paintSmall = null;
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
		if (widget_id == id_0) {
			widget.id = id_0;
			widget.description = desc_0;
			widget.width = 24;
			widget.height = 32;
			iconFile = "idle_weight.bmp";
		}

		// Bitmap icon = Utils.getBitmap(context, iconFile);
		String count = String.format(Locale.US,"%2.1f",weightValue);

		if (weightValue!=0)
			widget.priority = 1;
		else
			widget.priority = 0;
		widget.bitmap = Bitmap.createBitmap(widget.width, widget.height,
				Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(widget.bitmap);
		canvas.drawColor(Color.WHITE);

		int height = 32;
		TextPaint textPaint = paintSmall;
		Point iconOffset = Utils.getIconOffset(height);
		Point textOffset = Utils.getTextOffset(height);
		Bitmap icon = Utils.getBitmap(context, iconFile);
		canvas.drawBitmap(icon, iconOffset.x, iconOffset.y, null);
		canvas.drawText(count, textOffset.x, textOffset.y, textPaint);

		return widget;
	}

	public static void retrieveRequestToken(final Activity initiator) {
		SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager
				.getDefaultSharedPreferences(initiator).edit();
		sharedPreferencesEditor.putBoolean("WithingsAccountAuthenticated",
				false);
		sharedPreferencesEditor.commit();
		consumer = new CommonsHttpOAuthConsumer(
				MetaWatchService.Preferences.withingsAPIKey,
				MetaWatchService.Preferences.withingsAPISecret);
		provider = new CommonsHttpOAuthProvider(
				"https://oauth.withings.com/account/request_token",
				"https://oauth.withings.com/account/access_token",
				"https://oauth.withings.com/account/authorize");
		provider.setOAuth10a(true);
		final String mCallbackUrl = "metawatch://withings.com";
		new Thread(new Runnable() {
			public void run() {
				try {
					String authUrl = provider.retrieveRequestToken(consumer,
							mCallbackUrl);
					initiator.startActivity(new Intent(
							"android.intent.action.VIEW", Uri.parse(authUrl)));
				} catch (Exception e) {
					if (Preferences.logging)
						Log.d(MetaWatch.TAG,
								"WithingsWidget.retrieveRequestToken(): Exception while initiating authentification process with Withings: "
										+ e.toString());
					return;
				}
			}
		}).start();
	}

	public static void checkURI(final Activity initiator, Uri uri) {
		String host = uri.getHost();
		if (host.contains("withings.com")) {
			final String token = uri.getQueryParameter("oauth_token");
			final String verifier = uri.getQueryParameter("oauth_verifier");
			final String userID = uri.getQueryParameter("userid");
			if ((token == null) || (verifier == null))
				return;
			new Thread(new Runnable() {
				public void run() {
					try {
						provider.retrieveAccessToken(consumer, verifier);
						accessToken = consumer.getToken();
						accessTokenSecret = consumer.getTokenSecret();
						SharedPreferences.Editor sharedPreferencesEditor = PreferenceManager
								.getDefaultSharedPreferences(initiator).edit();
						sharedPreferencesEditor.putString(
								"WithingsAccessToken", accessToken);
						sharedPreferencesEditor.putString(
								"WithingsAccessTokenSecret", accessTokenSecret);
						sharedPreferencesEditor.putString("WithingsUserID",
								userID);
						sharedPreferencesEditor.putBoolean(
								"WithingsAccountAuthenticated", true);
						sharedPreferencesEditor.commit();
						synchronized (WithingsWidget.wakeupUpdateThread) {
							WithingsWidget.forceUpdate=true;
							WithingsWidget.wakeupUpdateThread.notify();							
						}
					} catch (Exception e) {
						if (Preferences.logging)
							Log.d(MetaWatch.TAG,
									"WithingsWidget.retrieveRequestToken(): Exception while retrieving access token from Withings: "
											+ e.toString());
						return;
					}
				}
			}).start();
		}
	}
}
