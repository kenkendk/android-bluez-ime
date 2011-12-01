/* Copyright (C) 2011, Kenneth Skovhede
 * http://www.hexad.dk, opensource@hexad.dk
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package com.hexad.bluezime;

import java.util.ArrayList;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class DeviceScanActivity extends Activity {

	private static final int DO_SCAN_AFTER_ENABLE = 1;
	public static final String EXTRA_DEVICE = "device";
	public static final String EXTRA_CONTROLLER = "controller";
	
	private TextView m_knownDeviceLabel;
	private ListView m_knownDeviceList;
	private ListView m_foundDeviceList;
	private LinearLayout m_scanWaitMarker;
	private TextView m_scanWaitText;
	private Button m_scanButton;
	private BluetoothAdapter m_bluetoothAdapter;
	
	private ArrayList<BluetoothDevice> m_foundDevices;
	
	private int m_controller = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.devicelist);
		
		setResult(Activity.RESULT_CANCELED);
		
		m_controller = this.getIntent().getIntExtra(EXTRA_CONTROLLER, 0);
		
		m_knownDeviceLabel = (TextView)findViewById(R.id.PairedDeviceLabel);
		m_knownDeviceList = (ListView)findViewById(R.id.PairedDeviceList);
		m_foundDeviceList = (ListView)findViewById(R.id.FoundDeviceList);
		m_scanWaitMarker = (LinearLayout)findViewById(R.id.WaitLayoutGroup);
		m_scanWaitText = (TextView)findViewById(R.id.WaitLabelText);
		m_scanButton = (Button)findViewById(R.id.ScanButton);

		registerReceiver(discoveryStartedMonitor, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
		registerReceiver(discoveryFinishedMonitor, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
		registerReceiver(deviceFoundMonitor, new IntentFilter(BluetoothDevice.ACTION_FOUND));
		registerReceiver(deviceBondedMonitor, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
		
		m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (m_bluetoothAdapter == null)
		{
			AlertDialog dlg =  new AlertDialog.Builder(this).create();
			dlg.setMessage("No bluetooth device found");
			dlg.show();
			return;
		}
		
		ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>(m_bluetoothAdapter.getBondedDevices());
		m_knownDeviceList.setAdapter(new DeviceItemAdapter(this, R.layout.devicelist_item, devices));
		if (devices.size() == 0)
		{
			m_knownDeviceLabel.setVisibility(View.GONE);
			m_knownDeviceList.setVisibility(View.GONE);
		}
		
		m_foundDevices = new ArrayList<BluetoothDevice>();
		m_foundDeviceList.setAdapter(new DeviceItemAdapter(this, R.layout.devicelist_item, m_foundDevices));
		
		m_knownDeviceList.setOnItemClickListener(onDeviceClick);
		m_foundDeviceList.setOnItemClickListener(onDeviceClick);
		
		m_scanButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				discoverDevices();
			}
		});
		
		discoverDevices();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == DO_SCAN_AFTER_ENABLE && resultCode == Activity.RESULT_OK) {
			discoverDevices();
		}
	}
	
	private void discoverDevices() {
		
		if (!m_bluetoothAdapter.isEnabled()) {
			startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), DO_SCAN_AFTER_ENABLE);
			return;
		}
		
        if (m_bluetoothAdapter.isDiscovering()) {
        	m_bluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        m_bluetoothAdapter.startDiscovery();

	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (isFinishing() && m_bluetoothAdapter != null && m_bluetoothAdapter.isDiscovering())
			m_bluetoothAdapter.cancelDiscovery();
		
		unregisterReceiver(discoveryStartedMonitor);
		unregisterReceiver(discoveryFinishedMonitor);
		unregisterReceiver(deviceFoundMonitor);
		unregisterReceiver(deviceBondedMonitor);
	}
	
	private void deviceSelected(BluetoothDevice device) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE, device);
        intent.putExtra(EXTRA_CONTROLLER, m_controller);

        setResult(Activity.RESULT_OK, intent);
        finish();
	}
	
    private OnItemClickListener onDeviceClick = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
			
			BluetoothDevice device = (BluetoothDevice)v.getTag(); 
			
			if (device != null) {
				m_bluetoothAdapter.cancelDiscovery();
				deviceSelected(device);
			}
        }
    };

	
	private class DeviceItemAdapter extends ArrayAdapter<BluetoothDevice> {
		private ArrayList<BluetoothDevice> m_items;

		public DeviceItemAdapter(Context context, int textViewResourceId,
				ArrayList<BluetoothDevice> items) {
			super(context, textViewResourceId, items);
			m_items = items;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) super.getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.devicelist_item, null);
			}

			TextView name = (TextView)v.findViewById(R.id.DeviceName);
			TextView address = (TextView)v.findViewById(R.id.DeviceAddress);
			
			BluetoothDevice item = m_items.get(position);
			v.setTag(item);
			
			if (item == null)
			{
				name.setVisibility(View.GONE);
				address.setVisibility(View.GONE);
			}
			else
			{
				name.setVisibility(View.VISIBLE);
				address.setVisibility(View.VISIBLE);
				name.setText(item.getName());
				address.setText(item.getAddress());
			}

			return v;
		}
	}	
	
	private BroadcastReceiver discoveryStartedMonitor = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			m_foundDevices.clear();
			m_scanWaitText.setText(R.string.devicelist_scanningfordevices);
			m_scanWaitMarker.setVisibility(View.VISIBLE);
			m_scanButton.setVisibility(View.GONE);
		}
	};
	
	private BroadcastReceiver discoveryFinishedMonitor = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			m_scanWaitMarker.setVisibility(View.GONE);
			m_scanButton.setVisibility(View.VISIBLE);
		}
	};
	
	private BroadcastReceiver deviceBondedMonitor = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
			if (state == BluetoothDevice.BOND_BONDED) {
				deviceSelected((BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
			}
		}
	};
	
	private BroadcastReceiver deviceFoundMonitor = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
				BluetoothDevice found = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				
				//Filter duplicates
				for(BluetoothDevice d : m_foundDevices)
					if (d.getAddress().equals(found.getAddress()))
						return;
				
				m_foundDevices.add(found);
			}
		}
	};	
}
