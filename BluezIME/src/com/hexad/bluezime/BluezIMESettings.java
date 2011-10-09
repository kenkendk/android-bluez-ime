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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Set;
import com.hexad.bluezime.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.inputmethod.InputMethodManager;

public class BluezIMESettings extends PreferenceActivity {

	private static final String SCAN_MARKER = "<scan>";
	private static final int DISCOVER_DEVICE_COMPLETE = 1;
	
	private CheckBoxPreference m_bluetoothActivity;
	private ListPreference m_pairedDevices;
	private ListPreference m_drivers;
	private Preference m_selectIME;
	private Preference m_helpButton;
	private Preference m_configureButton;
	private ListPreference m_donateButton;
	
	private HashMap<String, String> m_pairedDeviceLookup;
	
	private Preferences m_prefs;
	
	@SuppressWarnings("unused")
	private Object m_donationObserver; 
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bluezimesettings);

        m_prefs = new Preferences(this);
        
        m_bluetoothActivity = (CheckBoxPreference)findPreference("blue_activated");
        m_pairedDevices = (ListPreference)findPreference("blue_devices");
        m_drivers = (ListPreference)findPreference("blue_drivers");
        m_selectIME = (Preference)findPreference("blue_selectime");
        m_helpButton = (Preference)findPreference("blue_help");
        m_configureButton = (Preference)findPreference("configure_keys");
        m_donateButton = (ListPreference)findPreference("donate_button");
        
        //Populate the list, otherwise the app will crash
        m_donateButton.setEntries(new CharSequence[] { getString(R.string.preference_use_paypal) });
        m_donateButton.setEntryValues(new CharSequence[] {"PAYPAL"} );
        
        try {
        	//This code enables the in-app donation system, but does not require it for compilation
        	//This is done to avoid polluting the project source with all the boilerplate code
        	dalvik.system.PathClassLoader loader = new dalvik.system.PathClassLoader(this.getPackageCodePath(), java.lang.ClassLoader.getSystemClassLoader());

        	Class c = loader.loadClass("com.hexad.bluezime.donation.DonationObserver");
        	Constructor cc = c.getDeclaredConstructor(Activity.class);
        	
        	m_donationObserver = cc.newInstance(this);
        } catch (Exception ex) {
        	inAppDonationsEnabled(false);
        }
        
        m_helpButton.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				try {
					String url =  "http://code.google.com/p/android-bluez-ime/";
					Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse( url ) );
				    startActivity( browse );			
				} catch (Exception e) {
				}
				
				return false;
			}
		});

        m_donateButton.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (newValue instanceof String && ((String)newValue).equals("PAYPAL"))
				{
					try {
						String url = "https://www.paypal.com/cgi-bin/webscr?cmd=_xclick&business=paypal%40hexad%2edk&item_name=BluezIME%20Donation&no_shipping=2&no_note=1&tax=0&currency_code=EUR&bn=PP%2dDonationsBF&charset=UTF%2d8";
						Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse( url ) );
					    startActivity( browse );			
					} catch (Exception e) {
					}
				}
				return false;
			}
		});

        BluetoothAdapter blue = BluetoothAdapter.getDefaultAdapter();
        if (blue == null)
        {
        	m_bluetoothActivity.setEnabled(false);
        	m_bluetoothActivity.setSummary(R.string.bluetooth_unsupported);
        	bluetoothStateMonitor = null;
        	
        	CharSequence[] entries = new CharSequence[0];
        	m_pairedDevices.setEntries(entries);
        	m_pairedDevices.setEntryValues(entries);
        	
        	m_pairedDevices.setEnabled(false);
        	m_drivers.setEnabled(false);
        	m_configureButton.setEnabled(false);
        	
        	AlertDialog dlg = new AlertDialog.Builder(this).create();
        	dlg.setMessage(this.getString(R.string.bluetooth_unsupported));
        	dlg.show();
        }
        else
        {
            m_configureButton.setIntent(new Intent(this, ButtonConfiguration.class));
        	
        	m_bluetoothActivity.setChecked(blue.isEnabled());
        	m_bluetoothActivity.setEnabled(true);
        	
        	if (blue.isEnabled()) {
        		m_bluetoothActivity.setSummary(R.string.bluetooth_state_on);
        	} else {
        		m_bluetoothActivity.setSummary(R.string.bluetooth_state_off);
        	}
        	
        	m_bluetoothActivity.setOnPreferenceClickListener(new OnPreferenceClickListener() {
        		public boolean onPreferenceClick(Preference preference) {
        			
        			if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
        				m_bluetoothActivity.setChecked(true);
        				ImprovedBluetoothDevice.DeactivateBluetooth(BluezIMESettings.this);
        			} else {
	        			m_bluetoothActivity.setChecked(false);
	        			ImprovedBluetoothDevice.ActivateBluetooth(BluezIMESettings.this);
        			}
        			return false;
        		}
        	});
        	
        	registerReceiver(bluetoothStateMonitor, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        	
        	m_pairedDevices.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (newValue != null && newValue.equals(SCAN_MARKER)) {
						startActivityForResult(new Intent(BluezIMESettings.this, DeviceScanActivity.class), DISCOVER_DEVICE_COMPLETE);
						return false;
					} else {
						String address = (String)newValue;
						m_prefs.setSelectedDevice(m_pairedDeviceLookup.get(address), address);
						return true;
					}
					
				}
			});

        	m_drivers.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String driver = (String)newValue;
					m_prefs.setSelectedDriverName(driver);
					return true;
				}
			});

    		updateDisplay();
        }

    	CharSequence[] entries = new CharSequence[BluezService.DRIVER_NAMES.length];
    	CharSequence[] entryValues = new CharSequence[entries.length];
    	String[] displayNames = this.getResources().getStringArray(R.array.driver_displaynames);
    	
    	for(int i = 0; i < entries.length; i++) {
    		if (displayNames.length > i)
    			entries[i] = displayNames[i];
    		else
    			entries[i] = BluezService.DRIVER_DISPLAYNAMES[i];
    		
    		entryValues[i] = BluezService.DRIVER_NAMES[i];
    	}
    	
    	m_drivers.setEntries(entries);
    	m_drivers.setEntryValues(entryValues);

    	registerReceiver(preferenceUpdateMonitor, new IntentFilter(Preferences.PREFERENCES_UPDATED));
    	
    	m_selectIME.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				InputMethodManager m = (InputMethodManager)BluezIMESettings.this.getSystemService(INPUT_METHOD_SERVICE);
				m.showInputMethodPicker();
				return false;
			}
		});
    }

    @Override
	protected void onDestroy() {
    	super.onDestroy();
    	
    	if (bluetoothStateMonitor != null)
    		unregisterReceiver(bluetoothStateMonitor);
    	unregisterReceiver(preferenceUpdateMonitor);
    }
    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	if (requestCode == DISCOVER_DEVICE_COMPLETE && resultCode == Activity.RESULT_OK) {
    		BluetoothDevice device = (BluetoothDevice)data.getParcelableExtra(DeviceScanActivity.EXTRA_DEVICE);

    		if (!m_pairedDeviceLookup.containsKey(device.getAddress()))
    			m_pairedDeviceLookup.put(device.getAddress(), device.getName());

    		m_prefs.setSelectedDevice(device.getName(), device.getAddress());
    	}
    }
    
    public void updateDonationState(String itemid) {
    	
    	try {
    		int purchased = Integer.parseInt(itemid.substring(itemid.indexOf("_") + 1));
    		m_prefs.setDonatedAmount(m_prefs.getDonatedAmount() + purchased);
    	} catch (Exception ex) {
    	}
    	
    	//Update the display
    	inAppDonationsEnabled(true);
    }
    
    public void inAppDonationsEnabled(boolean enabled) {
    	if (m_prefs.getDonatedAmount() > 0) {
	    	m_donateButton.setTitle(R.string.preferencelist_donate_short_donated);
	    	m_donateButton.setSummary(String.format(this.getString(R.string.preferencelist_donate_long_donated), m_prefs.getDonatedAmount()));
    	} else {
	    	m_donateButton.setTitle(R.string.preferencelist_donate_short);
	    	m_donateButton.setSummary(R.string.preferencelist_donate_long);
    	}
    }
    
    public ListPreference getDonateButton() { return m_donateButton; }
    
    private void enumerateBondedDevices() {
    	Set<BluetoothDevice> pairedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
    	
    	m_pairedDeviceLookup = new HashMap<String, String>();
    	
    	boolean containsCurrent = false;
    	String curDevice = m_prefs.getSelectedDeviceAddress(); 

    	curDevice = null;
    	
    	if (curDevice == null)
	    	for (BluetoothDevice d : pairedDevices)
	    		if (d.getAddress().equals(curDevice)) {
	    			containsCurrent = true;
	    			break;
	    		}
    	
    	
    	CharSequence[] entries = new CharSequence[pairedDevices.size() + 1 + ((containsCurrent || curDevice == null) ? 0 : 1)];
    	CharSequence[] entryValues = new CharSequence[entries.length];
    	
    	if (pairedDevices.size() > 0) {
    		BluetoothDevice[] devices = new BluetoothDevice[pairedDevices.size()];
    		pairedDevices.toArray(devices);
    		
    		// Loop through paired devices
    	    for (int i = 0; i < devices.length; i++) {
    	    	entries[i] = devices[i].getName();
    	    	entryValues[i] = devices[i].getAddress();
    	    	m_pairedDeviceLookup.put(devices[i].getAddress(), devices[i].getName());
    	    }
    	}
    	
    	if (!containsCurrent && curDevice != null) {
    		entries[entries.length - 2] = m_prefs.getSelectedDeviceName();
    		entryValues[entries.length - 2] = m_prefs.getSelectedDeviceAddress();
    	}
    	
    	entries[entries.length - 1] = this.getString(R.string.bluetooth_scan_menu);
    	entryValues[entries.length - 1] = SCAN_MARKER;
    	
    	m_pairedDevices.setEntries(entries);
    	m_pairedDevices.setEntryValues(entryValues);
    }

	private void updateDisplay() {
		enumerateBondedDevices();
		
		if (m_prefs.getSelectedDeviceAddress() == null) {
			m_pairedDevices.setSummary(R.string.bluetooth_no_device);
		} else {
			String address = m_prefs.getSelectedDeviceAddress();
			m_pairedDevices.setSummary(m_prefs.getSelectedDeviceName() + " - " + address);

			CharSequence[] items = m_pairedDevices.getEntryValues();
			for(int i = 0; i < items.length; i++)
				if (items[i].equals(address)) {
					m_pairedDevices.setValueIndex(i);
					break;
				}
		}
		
		String driver = m_prefs.getSelectedDriverName();
		int index = -1;
		for(int i = 0; i < BluezService.DRIVER_NAMES.length; i++)
			if (BluezService.DRIVER_NAMES[i].equals(driver)) {
				index = i;
				break;
			}

		if (index < 0 || index >= BluezService.DRIVER_DISPLAYNAMES.length)
			m_drivers.setSummary(R.string.preference_device_unknown);
		else
			m_drivers.setSummary(BluezService.DRIVER_DISPLAYNAMES[index]);
		
		m_configureButton.setEnabled(m_prefs.getSelectedDriverName() != null && m_prefs.getSelectedDriverName().length() > 0);
	}
    
	private BroadcastReceiver bluetoothStateMonitor = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
			if (state == BluetoothAdapter.STATE_ON) {
				m_bluetoothActivity.setChecked(true);
				m_bluetoothActivity.setEnabled(true);
				m_bluetoothActivity.setSummary(R.string.bluetooth_state_on);
				
				enumerateBondedDevices();
			}
			else if (state == BluetoothAdapter.STATE_OFF) {
				m_bluetoothActivity.setChecked(false);
				m_bluetoothActivity.setEnabled(true);
				m_bluetoothActivity.setSummary(R.string.bluetooth_state_off);
			}
			else if (state == BluetoothAdapter.STATE_TURNING_OFF) {
				m_bluetoothActivity.setChecked(false);
				m_bluetoothActivity.setEnabled(false);
				m_bluetoothActivity.setSummary(R.string.bluetooth_state_turning_off);
			}
			else if (state == BluetoothAdapter.STATE_TURNING_ON) {
				m_bluetoothActivity.setChecked(false);
				m_bluetoothActivity.setEnabled(false);
				m_bluetoothActivity.setSummary(R.string.bluetooth_state_turning_on);
			}
		}
	};
	
	private BroadcastReceiver preferenceUpdateMonitor = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateDisplay();
		}
	};	
}
