                                                                     
                                                                     
                                                                     
                                             
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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class DeviceSelection extends Activity {

	class Receiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
				if (Preferences.logging) Log.d(MetaWatch.TAG, "discovery finished");
				
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
				if (Preferences.logging) Log.d(MetaWatch.TAG, "device found");
				
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				if (device.getBondState() == BluetoothDevice.BOND_BONDED)
					return;
				
				String deviceName = device.getName();
				String deviceMac = device.getAddress();
				
				addToList(deviceMac, deviceName);
			}
		}
	}
	
	Context context;
	ListView listView;
	List<Map<String, String>> list = new ArrayList<Map<String, String>>();
	Set<String> foundMacs = new TreeSet<String>();
	private Receiver receiver;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		
		// No point in discovering if it's switched off
		BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
		if((defaultAdapter == null) || (!BluetoothAdapter.getDefaultAdapter().isEnabled())) {
			finish();
			return;
		}
	
		setContentView(R.layout.device_selection);
		listView = (ListView) findViewById(android.R.id.list);
		
		listView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				if (Preferences.logging) Log.d(MetaWatch.TAG, "device selected: " + arg2);
				
				Map<String, String> map = list.get(arg2);
				String mac = map.get("mac");
				
				if (Preferences.logging) Log.d(MetaWatch.TAG, "mac selected: " + mac);
				
				MetaWatchService.Preferences.watchMacAddress = mac;
				MetaWatchService.saveMac(context, mac);
				
				sendToast("Selected watch set");
				finish();
			} 
			
		});
		
		Button searchButton = (Button) findViewById(R.id.buttonSearch);
		
		searchButton.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				startDiscovery();
			}


		});
		
		
		
		if (MetaWatchService.bluetoothAdapter == null)
			MetaWatchService.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (MetaWatchService.bluetoothAdapter == null) {
			sendToast("Bluetooth not supported");
			return;
		}
		
		Set<BluetoothDevice> pairedDevices = MetaWatchService.bluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
		    for (BluetoothDevice device : pairedDevices) {
		        addToList(device.getAddress(), device.getName());
		    }
		}
		
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean showFakeWatches = sharedPreferences.getBoolean("ShowFakeWatches", false);
		
		if(showFakeWatches) {		
			addToList("DIGITAL", "Fake Digital Watch (Use for debugging digital functionality within MWM)");
			addToList("ANALOG", "Fake Analog Watch (Use for debugging analog functionality within MWM)");
		}
		
		startDiscovery();
	
	}
	
	void startDiscovery() {
		receiver = new Receiver();
		IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
		
		registerReceiver(receiver, intentFilter);
		
		MetaWatchService.bluetoothAdapter.startDiscovery();
		
		ProgressBar progress = (ProgressBar) findViewById(R.id.progressScanning);
		progress.setVisibility(ProgressBar.VISIBLE);
		
		Button searchButton = (Button) findViewById(R.id.buttonSearch);
		searchButton.setEnabled(false);
		searchButton.setText("Searching...");
	}
	
	void addToList(String mac, String name) {
		
		if(!foundMacs.contains(mac)) {	
			Map<String, String> map = new HashMap<String, String>();
			map.put("mac", mac);
			map.put("name", name);
					
			list.add(map);
			foundMacs.add(mac);
			displayList();
		}
	}
	
	void displayList() {
		
		listView.setAdapter(new SimpleAdapter(this, list, R.layout.list_item, new String[] { "name", "mac"}, new int[] { R.id.text1, R.id.text2} ));
	}
	
	public void sendToast(String text) {
		Message m = new Message();
		m.what = 1;
		m.obj = text;
		messageHandler.sendMessage(m);
	}

	private Handler messageHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				Toast.makeText(context, (CharSequence) msg.obj, Toast.LENGTH_SHORT).show();
				break;
			}
		}

	};
 

	@Override
	protected void onDestroy() {		
		super.onDestroy();
		if (receiver!=null)
			unregisterReceiver(receiver);
	}
}
