
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
 * Settings.java                                                             *
 * Settings                                                                  *
 * Preference activity                                                       *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import java.util.List;
import java.util.Map;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

import org.metawatch.communityedition.R;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.actions.Action;
import org.metawatch.manager.actions.ActionManager;
import org.metawatch.manager.apps.AppManager;
import org.metawatch.manager.apps.ApplicationBase.AppData;
import org.metawatch.manager.widgets.WidgetManager;
import org.metawatch.manager.widgets.WithingsWidget;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.webkit.WebView;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	static CharSequence[] entriesArray;
	static CharSequence[] entryValuesArray;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.layout.settings);

		EditTextPreference editTextMac = (EditTextPreference) findPreference("MAC");
		editTextMac.setText(MetaWatchService.Preferences.watchMacAddress);

		Preference discovery = findPreference("Discovery");
		discovery.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {

				if (Preferences.logging)
					Log.d(MetaWatch.TAG, "discovery click");

				startActivity(new Intent(Settings.this, DeviceSelection.class));

				return false;
			}
		});

		Preference theme = findPreference("Theme");
		theme.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {

				if (Preferences.logging)
					Log.d(MetaWatch.TAG, "theme click");

				startActivity(new Intent(Settings.this, ThemeContainer.class));

				return false;
			}
		});

		Preference otherAppsList = findPreference("otherAppsList");
		otherAppsList
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference arg0) {
						startActivity(new Intent(Settings.this,
								OtherAppsList.class));
						return false;
					}
				});

		Preference resetWidgets = findPreference("ResetWidgets");
		resetWidgets
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					public boolean onPreferenceClick(Preference arg0) {
						WidgetManager.resetWidgetsToDefaults(Settings.this);
						return false;
					}
				});

		Preference backup = findPreference("Backup");
		backup.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				Utils.backupUserPrefs(Settings.this);
				return false;
			}
		});

		Preference restore = findPreference("Restore");
		restore.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				if (Utils.restoreUserPrefs(Settings.this)) {
					// Restart
					AlarmManager alm = (AlarmManager) Settings.this
							.getSystemService(Context.ALARM_SERVICE);
					alm.set(AlarmManager.RTC,
							System.currentTimeMillis() + 1000, PendingIntent
									.getActivity(Settings.this, 0, new Intent(
											Settings.this, MetaWatch.class), 0));
					android.os.Process.sendSignal(android.os.Process.myPid(),
							android.os.Process.SIGNAL_KILL);
				}
				return false;
			}
		});

		Preference about = findPreference("About");
		about.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				showAbout();
				return false;
			}
		});

		Preference exit = findPreference("Exit");
		exit.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				exit();
				return false;
			}
		});

		// InsecureBtSocket requires API10 or higher
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion < android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			findPreference("InsecureBtSocket").setEnabled(false);
		}

		Preference withings = findPreference("WithingsAccountAuthenticated");
		withings.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				WithingsWidget.retrieveRequestToken(Settings.this);
				return false;
			}
		});

		super.onStart();
	}

	@Override
	protected void onResume() {

	    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	    
		// Dynamically add the "Enabled app" controls
		PreferenceCategory appGroup = (PreferenceCategory) findPreference("ActiveApps");

		appGroup.removeAll();

		AppData[] data = AppManager.getAppInfos();
		for (AppData appEntry : data) {

			if (Preferences.logging)
				Log.d(MetaWatch.TAG, "Adding setting for " + appEntry.id);

			CheckBoxPreference test = new CheckBoxPreference(this);
			test.setKey(appEntry.getPageSettingName());
			test.setTitle(appEntry.name);

			appGroup.addPreference(test);
		}

		setQuickButtonPreferences();

		ListPreferenceMultiSelect calendars = (ListPreferenceMultiSelect) findPreference("DisplayCalendars");
		if (calendars != null) {
			final Map<String, Integer> calData = Utils.getCalendars(this);

			Resources res = getResources();

			entriesArray = new CharSequence[calData.size() + 1];
			entryValuesArray = new CharSequence[calData.size() + 1];

			entriesArray[0] = res.getString(R.string.settings_calendar_all);
			entryValuesArray[0] = "#ALL#";

			int i = 1;
			for (String name : calData.keySet()) {
				entriesArray[i] = name == null ? "" : name;
				entryValuesArray[i] = calData.get(name).toString();
				i++;
			}

			calendars.setEntries(entriesArray);
			calendars.setEntryValues(entryValuesArray);
		}
		
		super.onResume();
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
	
	private void setQuickButtonPreferences() {
		List<Action> actions = ActionManager.getBindableActions(this);

		final int items = actions.size();

		entriesArray = new CharSequence[items];
		entryValuesArray = new CharSequence[items];

		int i = 0;
		for (Action action : actions) {
			entriesArray[i] = action.getName();
			entryValuesArray[i] = action.getId();
			i++;
		}

		ListPreference leftQuickButton = (ListPreference) findPreference("QuickButtonL");
		if (leftQuickButton != null) {
			leftQuickButton.setEntries(entriesArray);
			leftQuickButton.setEntryValues(entryValuesArray);

		}

		ListPreference rightQuickButton = (ListPreference) findPreference("QuickButtonR");
		if (rightQuickButton != null) {
			rightQuickButton.setEntries(entriesArray);
			rightQuickButton.setEntryValues(entryValuesArray);

		}
	}

	void showAbout() {

		WebView webView = new WebView(this);
		String html = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /><title>About</title></head><body><center>"
				+ "<img src=\"banner.jpg\" width=\"100%\">"
				+ "<p>Version "
				+ Utils.getVersion(this)
				+ ".</p>"
				+ "<b>MetaWatch Community Team</b><br>"
				+ "Joakim Andersson<br>Chris Boyle<br>Garth Bushell<br>Prash D<br>Matthias Gruenewald<br>"
				+ "Richard Munn<br>Diogo Neves<br>Craig Oliver<br>Didi Pfeifle<br>Thierry Schork<br>"
				+ "Kyle Schroeder<br>Chris Sewell<br>Geoff Willingham<br>Dobie Wollert<p>"
				+ "<b>Translation Team</b><br>"
				+ "Miguel Branco<br>Didi Pfeifle<br>Geurt Pieter Maassen van den Brink<br>Thierry Schork<br>"
				+ "Kamosan Software<br>Erisi Roberto<p>"
				+ "<p>&copy; Copyright 2011-2012 Meta Watch Ltd.</p>"
				+ "</center></body></html>";
		webView.loadDataWithBaseURL("file:///android_asset/", html,
				"text/html", "utf-8", null);

		new AlertDialog.Builder(this).setView(webView).setCancelable(true)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					// @Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();
	}

	void exit() {
		stopService(new Intent(this, MetaWatchService.class));
		MetaWatchService.setPreviousConnectionState(this, false);
		System.exit(0);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if (key.equals("WithingsAccountAuthenticated")) {
    		CheckBoxPreference withings = (CheckBoxPreference)findPreference("WithingsAccountAuthenticated");
    		withings.setChecked(sharedPreferences.getBoolean("WithingsAccountAuthenticated", false));
    	}
		if (key.contains("Idle") || key.contains(".app_enabled")) {
			Idle.reset(this);
			Idle.updateIdle(this, true);
		}
    }
}
