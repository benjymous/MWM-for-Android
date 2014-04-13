package org.metawatch.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.metawatch.communityedition.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class OtherAppsList extends Activity {
	static final int MODE_WHITELIST   = 0x01;
	static final int MODE_PREFER_FULL = 0x02;
	static final int MODE_STICKY      = 0x04;

	private List<AppInfo> appInfos;

	private class AppLoader extends AsyncTask<Void, Void, List<AppInfo>> {
		private ProgressDialog pdWait;

		protected void onPreExecute() {
			pdWait = ProgressDialog.show(OtherAppsList.this, "", "Loading apps, please wait...");
		}

		@Override
		protected List<AppInfo> doInBackground(Void... params) {
			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(OtherAppsList.this);
			
			PackageManager pm = getPackageManager();
			List<PackageInfo> packages = pm.getInstalledPackages(0);
			List<AppInfo> appInfos = new ArrayList<AppInfo>();
			for (PackageInfo pi : packages) {
				/* Ignore system (non-versioned) packages */
				if (pi.versionName == null) {
					continue;
				}
				/* Ignore Android System */
				if (pi.packageName.equals("android")) {
					continue;
				}
				AppInfo appInfo = new AppInfo();
				appInfo.name = pi.applicationInfo.loadLabel(pm).toString();
				if (appInfo.name == null || appInfo.name.length() == 0) {
					appInfo.name = pi.packageName;
				}
				appInfo.icon = pi.applicationInfo.loadIcon(pm);
				appInfo.packageName = pi.packageName;
				
				int mode = sharedPreferences.getInt("appNotification_" + pi.packageName, 0);
				appInfo.isWhitelisted = ((mode & MODE_WHITELIST) != 0);
				appInfo.preferFullText = ((mode & MODE_PREFER_FULL) != 0);
				appInfo.sticky = ((mode & MODE_STICKY) != 0);
				appInfo.buzzes =
						sharedPreferences.getInt("appVibrate_" + pi.packageName, -1);
				if (appInfo.buzzes != -1) {
					/* For backwards compatibility with the old blacklist, at least whitelist apps with
					 * custom vibration pattern. */
					appInfo.isWhitelisted = true;
				}
				appInfos.add(appInfo);
			}
			Collections.sort(appInfos);

			return appInfos;
		}

		@Override
		protected void onPostExecute(List<AppInfo> appInfos) {
			ListView listView = (ListView) findViewById(android.R.id.list);
			listView.setAdapter(new WhitelistAdapter(appInfos));
			OtherAppsList.this.appInfos = appInfos;
			pdWait.dismiss();

		}

	}

	public class AppInfo implements Comparable<AppInfo> {
		String name;
		Drawable icon;
		String packageName;
		boolean isWhitelisted;
		boolean preferFullText;
		boolean sticky;
		int buzzes;

		@SuppressLint("DefaultLocale")
		public int compareTo(AppInfo another) {
			return this.name.toLowerCase().compareTo(another.name.toLowerCase());
		}
	}

	class WhitelistAdapter extends ArrayAdapter<AppInfo> {
		private final List<AppInfo> apps;
		private final String[] buzzSettingNames;
		private final String[] buzzSettingValues;

		public WhitelistAdapter(List<AppInfo> apps) {
			super(OtherAppsList.this, R.layout.other_apps_list_item, apps);
			this.apps = apps;

			this.buzzSettingNames = getResources().getStringArray(R.array.settings_number_buzzes_names);
			this.buzzSettingValues = getResources().getStringArray(R.array.settings_number_buzzes_values);
		}
		
		private String getBuzzesText(int buzzes) {
			if (buzzes == -1) {
				return getResources().getString(R.string.other_apps_vibration_default_abbr);
			} else if (buzzes == 0) {
				return getResources().getString(R.string.other_apps_vibration_none_abbr);
			} else {
				return String.valueOf(buzzes);
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			LayoutInflater inflater = OtherAppsList.this.getLayoutInflater();
			if(view == null) {
			    view = inflater.inflate(R.layout.other_apps_list_item, null);
			}
			ImageView icon = (ImageView) view
					.findViewById(R.id.other_apps_list_item_icon);
			TextView appName = (TextView) view
					.findViewById(R.id.other_apps_list_item_name);
			CheckBox checkbox = (CheckBox) view
					.findViewById(R.id.other_apps_list_item_check);
			final View settings[] = new View[] {
				view.findViewById(R.id.other_apps_list_item_buzzes_row),
				view.findViewById(R.id.other_apps_list_item_sticky_row),
				view.findViewById(R.id.other_apps_list_item_fulltext_row)
			};
			final Button buzzes = (Button) view
					.findViewById(R.id.other_apps_list_item_buzzes);
			CheckBox fullText = (CheckBox) view
					.findViewById(R.id.other_apps_list_item_fulltext);
			CheckBox sticky = (CheckBox) view
					.findViewById(R.id.other_apps_list_item_sticky);
			final AppInfo appInfo = apps.get(position);
			icon.setImageDrawable(appInfo.icon);
			appName.setText(appInfo.name);
			for (View row : settings) {
				row.setVisibility(appInfo.isWhitelisted ? View.VISIBLE : View.GONE);
			}
			
			// Remove any previous listeners to not confuse the system...
			checkbox.setOnCheckedChangeListener(null);
			fullText.setOnCheckedChangeListener(null);
			sticky.setOnCheckedChangeListener(null);
			// ...otherwise these rows triggers for the old app when the View is reused.
			checkbox.setChecked(appInfo.isWhitelisted);
			fullText.setChecked(appInfo.preferFullText);
			sticky.setChecked(appInfo.sticky);
			// Set listeners again
			checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					appInfo.isWhitelisted = isChecked;
					for (View row : settings) {
						row.setVisibility(isChecked ? View.VISIBLE : View.GONE);
					}
				}
			});
			fullText.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					appInfo.preferFullText = isChecked;
				}
			});
			sticky.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					appInfo.sticky = isChecked;
				}
			});
			
			buzzes.setText(getBuzzesText(appInfo.buzzes));
			buzzes.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					int index = -1;
					for (int i = 0; i < buzzSettingValues.length && index == -1; i++) {
						if (Integer.parseInt(buzzSettingValues[i]) == appInfo.buzzes) {
							index =  i;
						}
					}
					
					AlertDialog.Builder builder = new AlertDialog.Builder(OtherAppsList.this);
					builder.setTitle("Number of Buzzes");
					builder.setSingleChoiceItems(buzzSettingNames, index, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							appInfo.buzzes = Integer.parseInt(buzzSettingValues[item]);
							buzzes.setText(getBuzzesText(appInfo.buzzes));
							dialog.dismiss();
						}
					});
					builder.setNeutralButton(getResources().getString(R.string.other_apps_use_default), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							appInfo.buzzes = -1;
							buzzes.setText(getBuzzesText(appInfo.buzzes));
						}
					});
					builder.setNegativeButton(android.R.string.cancel, null);
					builder.setCancelable(true);
					builder.create().show();
				}
			});
			
			return view;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.other_apps_list);

		AppLoader appLoader = new AppLoader();
		appLoader.execute((Void[]) null);
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = sharedPreferences.edit();
			
			for (AppInfo appInfo : appInfos) {
				if (appInfo.isWhitelisted) {
					int mode = MODE_WHITELIST;
					if (appInfo.preferFullText) {
						mode |= MODE_PREFER_FULL;
					}
					if (appInfo.sticky) {
						mode |= MODE_STICKY;
					}
					editor.putInt("appNotification_" + appInfo.packageName, mode);
				} else {
					editor.remove("appNotification_" + appInfo.packageName);
				}
				if (appInfo.isWhitelisted && appInfo.buzzes != -1) {
					editor.putInt("appVibrate_" + appInfo.packageName, appInfo.buzzes);
				} else {
					editor.remove("appVibrate_" + appInfo.packageName);
				}
			}
			editor.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
