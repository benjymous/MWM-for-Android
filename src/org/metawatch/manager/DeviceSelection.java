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
 * DeviceSelection.java                                                      *
 * DeviceSelection                                                           *
 * Bluetooth device picker activity                                          *
 *                                                                           *
 *                                                                           *
 *****************************************************************************/

package org.metawatch.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.metawatch.manager.MetaWatchService.Preferences;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class DeviceSelection extends SherlockFragmentActivity {

    private ActionBar mActionBar;
    private BluetoothAdapter bluetoothAdapter;

    class Receiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {

	    if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "discovery finished");

		ProgressBar progress = (ProgressBar) findViewById(R.id.progressScanning);
		progress.setVisibility(ProgressBar.INVISIBLE);

		Button searchButton = (Button) findViewById(R.id.buttonSearch);
		searchButton.setEnabled(true);
		searchButton.setText("Search for devices");

		if (list.size() == 0) {
		    sendToast("No watch found");
		    finish();
		}
	    }

	    if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "device found");

		BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

		if (device.getBondState() == BluetoothDevice.BOND_BONDED)
		    return;

		String deviceName = device.getName();
		String deviceMac = device.getAddress();

		addToList(deviceMac, deviceName);
	    }
	}

    }

    private void processActionBar() {
	mActionBar = getSupportActionBar();
	mActionBar.setDisplayHomeAsUpEnabled(true);
	mActionBar.setDisplayShowTitleEnabled(true);
	this.invalidateOptionsMenu();
    }

    static Context context;
    ListView listView;
    List<Map<String, String>> list = new ArrayList<Map<String, String>>();
    Set<String> foundMacs = new TreeSet<String>();
    private Receiver receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	context = this;

	SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
	boolean showFakeWatches = sharedPreferences.getBoolean("ShowFakeWatches", false);

	setContentView(R.layout.device_selection);
	listView = (ListView) findViewById(android.R.id.list);

	listView.setOnItemClickListener(new OnItemClickListener() {

	    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "device selected: " + arg2);

		cleanup();
		
		Map<String, String> map = list.get(arg2);
		String mac = map.get("mac");

		if (Preferences.logging)
		    Log.d(MetaWatchStatus.TAG, "mac selected: " + mac);

		MetaWatchService.Preferences.watchMacAddress = mac;
		MetaWatchService.saveMac(context, mac);

		sendToast("Selected watch set");
		Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra(MetaWatchStatus.DEVICE_SELECTED_AUTO_CONNECT, true);
		startActivity(intent);
		finish();
	    }


	});

	Button searchButton = (Button) findViewById(R.id.buttonSearch);

	searchButton.setOnClickListener(new OnClickListener() {

	    public void onClick(View arg0) {
		startDiscovery();
	    }

	});

	if (bluetoothAdapter == null)
	    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	try {
	    BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
	    defaultAdapter.enable();
	} catch(Exception e){e.printStackTrace();}
	
	Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
	if (pairedDevices.size() > 0) {
	    for (BluetoothDevice device : pairedDevices) {
		addToList(device.getAddress(), device.getName());
	    }
	}

	if (showFakeWatches) {
	    addToList("DIGITAL", "Fake Digital Watch (Use for debugging digital functionality within MWM)");
	    addToList("ANALOG", "Fake Analog Watch (Use for debugging analog functionality within MWM)");
	}
	
	ProgressBar progress = (ProgressBar) findViewById(R.id.progressScanning);
	progress.setVisibility(ProgressBar.GONE);

	if ( bluetoothAdapter.isEnabled())
	    startDiscovery();

	processActionBar();

    }
    
    private void sendToast(String string) {
	Toast.makeText(context, string, Toast.LENGTH_SHORT).show();
    }


    void startDiscovery() {
	receiver = new Receiver();
	IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	intentFilter.addAction(BluetoothDevice.ACTION_FOUND);

	registerReceiver(receiver, intentFilter);

	bluetoothAdapter.startDiscovery();

	ProgressBar progress = (ProgressBar) findViewById(R.id.progressScanning);
	progress.setVisibility(ProgressBar.VISIBLE);

	Button searchButton = (Button) findViewById(R.id.buttonSearch);
	searchButton.setEnabled(false);
	searchButton.setText("Searching...");
    }

    void addToList(String mac, String name) {

	if (!foundMacs.contains(mac)) {
	    Map<String, String> map = new HashMap<String, String>();
	    map.put("mac", mac);
	    map.put("name", name);

	    list.add(map);
	    foundMacs.add(mac);
	    displayList();
	}
    }

    void displayList() {

	listView.setAdapter(new SimpleAdapter(this, list, R.layout.list_item, new String[] { "name", "mac" }, new int[] { R.id.text1, R.id.text2 }));
    }
    
    private void cleanup() {
	if (receiver != null)
	    try{unregisterReceiver(receiver);} catch (Exception e){}
	if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering())
	    bluetoothAdapter.cancelDiscovery();
	ProgressBar progress = (ProgressBar) findViewById(R.id.progressScanning);
	if (progress != null)
	    progress.setVisibility(ProgressBar.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
	super.onDestroy();
	cleanup();
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