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
 * Test.java                                                                 *
 * Test                                                                      *
 * Activity for sending test commands                                        *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchModes;
import org.metawatch.manager.MetaWatchService.WatchType;
import org.metawatch.manager.Notification.VibratePattern;
import org.metawatch.manager.apps.AppManager;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

@SuppressWarnings("deprecation")
public class Test extends SherlockPreferenceActivity {

    Context context;
    PreferenceScreen preferenceScreen;
    ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	context = this;

	addPreferencesFromResource(R.layout.test);

	preferenceScreen = getPreferenceScreen();

	processActionBar();

    }

    private void processActionBar() {
	mActionBar = getSupportActionBar();
	mActionBar.setTitle(R.string.metawatch_tests);
	mActionBar.setDisplayHomeAsUpEnabled(true);
	mActionBar.setDisplayShowTitleEnabled(true);
	this.invalidateOptionsMenu();
    }

    @Override
    protected void onStart() {

	preferenceScreen.findPreference("calendar").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		NotificationBuilder.createCalendar(context, "Tea with the Hatter - Windmill");
		return true;
	    }
	});

	preferenceScreen.findPreference("notification").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		if (MetaWatchService.watchType == WatchType.DIGITAL) {
		    // NotificationBuilder.createSmart(context,
		    // "Notification", ipsum);
		    Notification.getInstance().addTextNotification(context, "Notification", new VibratePattern(true, 500, 500, 3), Notification.getInstance().getDefaultNotificationTimeout(context));
		} else {
		    Notification.getInstance().addOledNotification(context, Protocol.getInstance(context).createOled2lines(context, "Display A, line 1", "Display A, line 2"), Protocol.getInstance(context).createOled2lines(context, "Display B, line 1", "Display B, line 2"), null, 0, null, "notification");
		    if (Preferences.logging)
			Log.d(MetaWatchStatus.TAG, "Notification timeout is: " + Notification.getInstance().getDefaultNotificationTimeout(context));

		}
		return true;
	    }
	});

	preferenceScreen.findPreference("sms").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		String smsText = "";
		for (int i = 0; i < 20; ++i) {
		    smsText += "SMS Line " + i + "\n";
		}
		NotificationBuilder.createSMS(context, "555-123-4567", smsText);
		return true;
	    }
	});

	preferenceScreen.findPreference("sms2").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		String smsText = "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.1.";
		NotificationBuilder.createSMS(context, "555-123-4567", smsText);
		return true;
	    }
	});

	preferenceScreen.findPreference("sms3").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		String smsText = "";
		for (int i = 0; i < 12; ++i) {
		    smsText += "SMS Line " + i + "\n";
		}
		NotificationBuilder.createSMS(context, "555-123-4567", smsText);
		return true;
	    }
	});

	preferenceScreen.findPreference("mms").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		NotificationBuilder.createMMS(context, "555-123-4567");
		return true;
	    }
	});

	preferenceScreen.findPreference("testShortMessage").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		NotificationBuilder.createSMS(context, "555-123-4567", "Hi.");
		return true;
	    }
	});

	preferenceScreen.findPreference("k9").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		NotificationBuilder.createK9(context, "The Doctor <doctor@gallifrey.net>", "Now drop your weapons, or I'll kill him with this deadly jelly baby!", "tardis:INBOX");
		return true;
	    }
	});

	preferenceScreen.findPreference("gmail_short").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		NotificationBuilder.createGmailBlank(context, "me@gmail.com", 513);
		return true;
	    }
	});

	preferenceScreen.findPreference("gmail_full").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		NotificationBuilder.createGmail(context, "bruce@wayneenterprises.com", "me@gmail.com", "Need a ride", "Alfred, would you bring the car around to the docks?");
		return true;
	    }
	});

	preferenceScreen.findPreference("alarm").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		NotificationBuilder.createAlarm(context);
		return true;
	    }
	});

	preferenceScreen.findPreference("timezone").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		NotificationBuilder.createTimezonechange(context);
		return true;
	    }
	});

	preferenceScreen.findPreference("Batterylow").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		NotificationBuilder.createBatterylow(context);
		return true;
	    }
	});

	preferenceScreen.findPreference("music").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		NotificationBuilder.createMusic(context, "Park", "Who is Aliandra", "Building a Better");
		return true;
	    }
	});

	preferenceScreen.findPreference("winamp").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		NotificationBuilder.createWinamp(context, "Winamp", "It really whips the llama's...", "One Hump or Two");
		return true;
	    }
	});

	preferenceScreen.findPreference("call_start").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		final String incomingNumber = "555-123-4567";
		Call.inCall = true;
		Call.phoneNumber = incomingNumber;
		MetaWatchService.setWatchMode(WatchModes.CALL);
		Call.startRinging(context, incomingNumber);
		return true;
	    }
	});

	preferenceScreen.findPreference("call_start2").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		final String incomingNumber = "unknown";
		Call.inCall = true;
		Call.phoneNumber = incomingNumber;
		MetaWatchService.setWatchMode(WatchModes.CALL);
		Call.startRinging(context, incomingNumber);
		return true;
	    }
	});

	preferenceScreen.findPreference("call_stop").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		Call.inCall = false;
		Call.phoneNumber = null;
		Call.endRinging(context);
		return true;
	    }
	});

	preferenceScreen.findPreference("vibrate").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		Protocol.getInstance(context).vibrate(300, 500, 3);
		return true;
	    }
	});

	preferenceScreen.findPreference("set_rtc").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		// Query the time on the watch, which will trigger
		// timing
		// of the round trip, so we can try and correct for that
		// when setting the time
		Protocol.getInstance(context).getRealTimeClock();
		return true;
	    }
	});

	preferenceScreen.findPreference("refresh_location").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		Monitors.getInstance().RefreshLocation();
		return true;
	    }
	});

	preferenceScreen.findPreference("refresh_weather").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		Monitors.getInstance().updateWeatherDataForced(context);
		return true;
	    }
	});

	preferenceScreen.findPreference("random_location").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		Random rnd = new Random();
		Monitors.getInstance().mLocationData.latitude = (rnd.nextDouble() * 180.0) - 90.0;
		Monitors.getInstance().mLocationData.longitude = (rnd.nextDouble() * 360.0) - 180.0;
		Monitors.getInstance().mLocationData.timeStamp = System.currentTimeMillis();
		Monitors.getInstance().mLocationData.received = true;
		Monitors.getInstance().weatherData.timeStamp = 0;
		Monitors.getInstance().updateWeatherData(context);
		return true;
	    }
	});

	preferenceScreen.findPreference("led_on").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		Protocol.getInstance(context).ledChange(true);
		return true;
	    }
	});

	preferenceScreen.findPreference("led_off").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		Protocol.getInstance(context).ledChange(false);
		return true;
	    }
	});

	preferenceScreen.findPreference("time_24hr").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		Protocol.getInstance(context).setTimeDateFormat(context);
		NotificationBuilder.createOtherNotification(context, null, "Time and date", "Formats updated.", 1);
		return true;
	    }
	});

	preferenceScreen.findPreference("time_hands_1200").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {

		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		int hour = 12 - (calendar.get(Calendar.HOUR) % 12);
		int minute = 60 - (calendar.get(Calendar.MINUTE));
		int second = 60 - (calendar.get(Calendar.SECOND));

		Protocol.getInstance(context).sendAdvanceHands(hour, minute, second);

		return true;
	    }
	});

	preferenceScreen.findPreference("time_hands_set").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {

		Date date = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		int hour = calendar.get(Calendar.HOUR);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);

		Protocol.getInstance(context).sendAdvanceHands(hour, minute, second);

		return true;
	    }
	});

	preferenceScreen.findPreference("media_next").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		MediaControl.getInstance().next(context);
		return true;
	    }
	});

	preferenceScreen.findPreference("media_previous").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		MediaControl.getInstance().previous(context);
		return true;
	    }
	});

	preferenceScreen.findPreference("media_togglepause").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference arg0) {
		MediaControl.getInstance().togglePause(context);
		return true;
	    }
	});

	preferenceScreen.findPreference("discover_apps").setOnPreferenceClickListener(new OnPreferenceClickListener() {
	    public boolean onPreferenceClick(Preference preference) {
		AppManager.getInstance(context).sendDiscoveryBroadcast(context);
		return true;
	    }
	});

	super.onStart();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case android.R.id.home:
	    finish();
	    return true;
	default:
	    return false;
	}
    }
}