package org.metawatch.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.metawatch.manager.MetaWatchService.Preferences;

import android.app.AlertDialog;
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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

public class OtherAppsList extends SherlockFragmentActivity {

    public static final String DEFAULT_BLACKLIST = "com.android.mms," + "com.google.android.gm," + "com.fsck.k9," + "com.android.alarmclock," + "com.htc.android.worldclock," + "com.android.deskclock," + "com.sonyericsson.alarm," + "com.motorola.blur.alarmclock";
    private List<AppInfo> appInfos;
    private AppLoader appLoader;

    private class AppLoader extends AsyncTask<Void, Void, List<AppInfo>> {

	protected void onPreExecute() {
	    OtherAppsList.this.setProgressBarIndeterminateVisibility(Boolean.TRUE);
	}

	@Override
	protected List<AppInfo> doInBackground(Void... params) {
	    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(OtherAppsList.this);
	    String[] blacklist = sharedPreferences.getString("appBlacklist", DEFAULT_BLACKLIST).split(",");
	    Arrays.sort(blacklist);

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
		appInfo.icon = pi.applicationInfo.loadIcon(pm);
		appInfo.packageName = pi.packageName;
		appInfo.isBlacklisted = (Arrays.binarySearch(blacklist, pi.packageName) >= 0);
		appInfo.buzzes = sharedPreferences.getInt("appVibrate_" + pi.packageName, -1);
		appInfos.add(appInfo);
	    }
	    Collections.sort(appInfos);

	    return appInfos;
	}

	@Override
	protected void onPostExecute(List<AppInfo> appInfos) {
	    try {
		ListView listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(new BlacklistAdapter(appInfos));
		OtherAppsList.this.appInfos = appInfos;
		OtherAppsList.this.setProgressBarIndeterminateVisibility(Boolean.FALSE);
	    } catch(NullPointerException e) {
		e.printStackTrace();
		//Likely the user closed the activity before this thread finished
	    }
	    
	}
    }

    public class AppInfo implements Comparable<AppInfo> {
	String name;
	Drawable icon;
	String packageName;
	boolean isBlacklisted;
	int buzzes;

	public int compareTo(AppInfo another) {
	    return this.name.compareTo(another.name);
	}
    }

    class BlacklistAdapter extends ArrayAdapter<AppInfo> {
	private final List<AppInfo> apps;
	private final String[] buzzSettingNames;
	private final String[] buzzSettingValues;

	public BlacklistAdapter(List<AppInfo> apps) {
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
	    if (view == null) {
		view = inflater.inflate(R.layout.other_apps_list_item, null);
	    }
	    ImageView icon = (ImageView) view.findViewById(R.id.other_apps_list_item_icon);
	    TextView appName = (TextView) view.findViewById(R.id.other_apps_list_item_name);
	    CheckBox checkbox = (CheckBox) view.findViewById(R.id.other_apps_list_item_check);
	    final Button buzzes = (Button) view.findViewById(R.id.other_apps_list_item_buzzes);
	    final AppInfo appInfo = apps.get(position);
	    icon.setImageDrawable(appInfo.icon);
	    appName.setText(appInfo.name);

	    // Remove any previous listener to not confuse the system...
	    checkbox.setOnCheckedChangeListener(null);
	    // ...otherwise this row triggers for the old app when the View is
	    // reused.
	    checkbox.setChecked(!appInfo.isBlacklisted);
	    checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		    appInfo.isBlacklisted = !isChecked;
		    buzzes.setEnabled(isChecked);
		}
	    });

	    buzzes.setEnabled(!appInfo.isBlacklisted);
	    buzzes.setText(getBuzzesText(appInfo.buzzes));
	    buzzes.setOnClickListener(new View.OnClickListener() {
		public void onClick(View v) {
		    int index = -1;
		    for (int i = 0; i < buzzSettingValues.length && index == -1; i++) {
			if (Integer.parseInt(buzzSettingValues[i]) == appInfo.buzzes) {
			    index = i;
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
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	setContentView(R.layout.other_apps_list);

	appLoader = new AppLoader();
	appLoader.execute((Void[]) null);
    }
    
    @Override
    public void onDestroy() {
	super.onDestroy();
	if (appLoader != null)
	    appLoader.cancel(true);
    }

    @Override
    protected void onPause() {
	super.onPause();
	try {
	    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	    SharedPreferences.Editor editor = sharedPreferences.edit();

	    StringBuilder sb = new StringBuilder();
	    for (AppInfo appInfo : appInfos) {
		if (appInfo.isBlacklisted) {
		    if (sb.length() > 0) {
			sb.append(",");
		    }
		    sb.append(appInfo.packageName);
		}
		if (appInfo.buzzes == -1) {
		    editor.remove("appVibrate_" + appInfo.packageName);
		} else {
		    editor.putInt("appVibrate_" + appInfo.packageName, appInfo.buzzes);
		}
	    }
	    String blacklist = sb.toString();
	    editor.putString("appBlacklist", blacklist);
	    editor.commit();
	    if (Preferences.logging)
		Log.d(MetaWatchStatus.TAG, "OtherAppsList: " + blacklist);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}
