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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import com.hexad.bluezime.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.inputmethod.InputMethodManager;

public class BluezIMESettings extends PreferenceActivity {

	private static final String SCAN_MARKER = "<scan>";
	private static final int DISCOVER_DEVICE_COMPLETE = 1;
	
	private CheckBoxPreference m_bluetoothActivity;
	private ListPreference[] m_pairedDevices;
	private ListPreference[] m_drivers;
	private Preference[] m_configButtons;
	private Preference m_selectIME;
	private Preference m_helpButton;
	private ListPreference m_donateButton;
	private PreferenceCategory m_devicesCategory;	
	private CheckBoxPreference m_manageBluetooth;
	private ListPreference m_wakelockType;
	private ListPreference m_controllerCount;
	
	private HashMap<String, String> m_pairedDeviceLookup;
	
	private Preferences m_prefs;
	
	@SuppressWarnings("unused")
	private Object m_donationObserver; 
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.bluezimesettings);

        m_prefs = new Preferences(this);
        
        m_bluetoothActivity = (CheckBoxPreference)findPreference("blue_activated");
        m_devicesCategory = (PreferenceCategory)findPreference("devices_category");
        m_controllerCount = (ListPreference)findPreference("multidevice_select");
        
        m_pairedDevices = new ListPreference[Preferences.MAX_NO_OF_CONTROLLERS];
    	m_drivers = new ListPreference[Preferences.MAX_NO_OF_CONTROLLERS];
    	m_configButtons = new Preference[Preferences.MAX_NO_OF_CONTROLLERS];
    	
    	m_pairedDevices[0] = (ListPreference)findPreference("blue_devices_1");
    	m_drivers[0] = (ListPreference)findPreference("blue_drivers_1");
    	m_configButtons[0] = (Preference)findPreference("blue_buttons_1");
    	
    	for(int i = 1; i < Preferences.MAX_NO_OF_CONTROLLERS; i++) {
    		m_pairedDevices[i] = new ListPreference(this);
    		m_drivers[i] = new ListPreference(this);
    		m_configButtons[i] = new Preference(this);
    	}
    	
        for(int i = 0; i < m_configButtons.length; i++) {
    		Intent intent = new Intent(this, ButtonConfiguration.class);
    		intent.putExtra(ButtonConfiguration.EXTRA_CONTROLLER, i);
            m_configButtons[i].setIntent(intent);
            m_configButtons[i].setSummary(R.string.preferencelist_configure_keys_long);
        	
        }
        
        m_selectIME = (Preference)findPreference("blue_selectime");
        m_helpButton = (Preference)findPreference("blue_help");
        m_donateButton = (ListPreference)findPreference("donate_button");
        m_manageBluetooth = (CheckBoxPreference)findPreference("blue_autoactivate");
        m_wakelockType = (ListPreference)findPreference("wakelock_type");
        
        //Populate the list, otherwise the app will crash
        m_donateButton.setEntries(new CharSequence[] { getString(R.string.preference_use_paypal) });
        m_donateButton.setEntryValues(new CharSequence[] {"PAYPAL"} );

        m_wakelockType.setEntries(new CharSequence[] { getString(R.string.preference_wakelock_none), getString(R.string.preference_wakelock_full), getString(R.string.preference_wakelock_dim) });
        m_wakelockType.setEntryValues(new CharSequence[] { Preferences.NO_WAKE_LOCK+"", PowerManager.FULL_WAKE_LOCK+"", PowerManager.SCREEN_DIM_WAKE_LOCK+"" } );

        CharSequence[] cnts = new CharSequence[Preferences.MAX_NO_OF_CONTROLLERS];
        for(int i = 0; i < cnts.length; i++) {
        	cnts[i] = (i + 1) + "";
        }
        
        m_controllerCount.setEntries(cnts);
        m_controllerCount.setEntryValues(cnts);
        
        try {
        	//This code enables the in-app donation system, but does not require it for compilation
        	//This is done to avoid polluting the project source with all the boilerplate code
        	dalvik.system.PathClassLoader loader = new dalvik.system.PathClassLoader(this.getPackageCodePath(), java.lang.ClassLoader.getSystemClassLoader());

        	Class<?> c = loader.loadClass("com.hexad.bluezime.donation.DonationObserver");
        	Constructor<?> cc = c.getDeclaredConstructor(Activity.class);
        	
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
        
        m_manageBluetooth.setChecked(m_prefs.getManageBluetooth());
        m_manageBluetooth.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				m_prefs.setManageBluetooth((Boolean)newValue);
				return true;
			}
		});

        m_wakelockType.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (newValue instanceof String) {
					int v = -1;
					
					try { v = Integer.parseInt((String)newValue); }
					catch (Throwable t) { }
					
					if (v >= 0) {
						m_prefs.setWakeLock(v);
						return true;
					}
				}
				
				return false;
			}
		});
        
        m_controllerCount.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if (newValue instanceof String) {
					int v = -1;
					
					try { v = Integer.parseInt((String)newValue); }
					catch (Throwable t) { }
					
					if (v > 0) {
						m_prefs.setControllerCount(v);
						return true;
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
        	for(ListPreference p : m_pairedDevices) {
        		p.setEntries(entries);
        		p.setEntryValues(entries);
        		p.setEnabled(false);
        	}
        	
        	for (ListPreference p : m_drivers)
        		p.setEnabled(false);
        	
        	for(Preference p : m_configButtons)
        		p.setEnabled(false);
        	
        	AlertDialog dlg = new AlertDialog.Builder(this).create();
        	dlg.setMessage(this.getString(R.string.bluetooth_unsupported));
        	dlg.show();
        }
        else
        {        	
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
	        			ImprovedBluetoothDevice.ActivateBluetooth(BluezIMESettings.this, null);
        			}
        			return false;
        		}
        	});
        	
        	registerReceiver(bluetoothStateMonitor, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        	
        	for(ListPreference p : m_pairedDevices) {
	        	p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						int pos = 1;
						for(int i = 0; i < m_pairedDevices.length; i++)
							if (m_pairedDevices[i] == preference) {
								pos = i;
								break;
							}
						
						if (newValue != null && newValue.equals(SCAN_MARKER)) {
							Intent i = new Intent(BluezIMESettings.this, DeviceScanActivity.class);
							i.putExtra(DeviceScanActivity.EXTRA_CONTROLLER, pos);
							startActivityForResult(i, DISCOVER_DEVICE_COMPLETE);
							return false;
						} else {
							String address = (String)newValue;
							
							
							m_prefs.setSelectedDevice(m_pairedDeviceLookup.get(address), address, pos);
							return true;
						}
						
					}
				});
        	}
        	
        	for(ListPreference p : m_drivers) {
	        	p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						String driver = (String)newValue;
						
						int pos = 1;
						for(int i = 0; i < m_drivers.length; i++)
							if (m_drivers[i] == preference) {
								pos = i;
								break;
							}
						
						m_prefs.setSelectedDriverName(driver, pos);
						return true;
					}
				});
        	}
        	
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
    	
    	for(ListPreference p : m_drivers) {
    		p.setEntries(entries);
    		p.setEntryValues(entryValues);
    	}

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

    		int controller = data.getIntExtra(DeviceScanActivity.EXTRA_CONTROLLER, -1);
    		
    		if (controller >= 0 && controller < m_prefs.getControllerCount())
    			m_prefs.setSelectedDevice(device.getName(), device.getAddress(), controller);
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
    	for(BluetoothDevice pd : pairedDevices)
    		m_pairedDeviceLookup.put(pd.getAddress(), pd.getName());

    	for(int i = 0; i < m_prefs.getControllerCount(); i++) {
    		String addr = m_prefs.getSelectedDeviceAddress(i);
    		if (addr != null && !m_pairedDeviceLookup.containsKey(addr)) {
    			m_pairedDeviceLookup.put(addr, m_prefs.getSelectedDeviceName(i));
    		}
    	}
    	
    	CharSequence[] entries = new CharSequence[m_pairedDeviceLookup.size() + 1];
    	CharSequence[] entryValues = new CharSequence[entries.length];

    	int index = 0;
    	for(String s : m_pairedDeviceLookup.keySet()) {
    		entries[index] = m_pairedDeviceLookup.get(s);
    		entryValues[index] = s;
    		index++;
    	}
    	    	
    	entries[entries.length - 1] = this.getString(R.string.bluetooth_scan_menu);
    	entryValues[entries.length - 1] = SCAN_MARKER;
    	
    	for(ListPreference p : m_pairedDevices) {
    		p.setEntries(entries);
    		p.setEntryValues(entryValues);
    	}
    }

	private void updateDisplay() {
		enumerateBondedDevices();
		
		int controllers = m_prefs.getControllerCount();
		
    	m_devicesCategory.removeAll();
    	for(int i = 0; i < Preferences.MAX_NO_OF_CONTROLLERS; i++) {
    		if (i < controllers) {
	    		m_devicesCategory.addPreference(m_pairedDevices[i]);
	    		m_devicesCategory.addPreference(m_drivers[i]);
	    		m_devicesCategory.addPreference(m_configButtons[i]);
    		}
    	}
    	
    	if (controllers == 1) {
    		m_pairedDevices[0].setTitle(R.string.preferencelist_selectdevice);
    		m_drivers[0].setTitle(R.string.preferencelist_selectdriver);
    		m_configButtons[0].setTitle(R.string.preferencelist_configure_keys_short);
    	} else {
	    	int i = 1;
	    	for(ListPreference p : m_pairedDevices) {
	    		p.setTitle(String.format(this.getString(R.string.preferencelist_selectdevice_n), i++));
	    	}
	    	
	    	i = 1;
	    	for(ListPreference p : m_drivers) {
	    		p.setTitle(String.format(this.getString(R.string.preferencelist_selectdriver_n), i++));
	    	}

	    	i = 1;
	    	for(Preference p : m_configButtons) {
	    		p.setTitle(String.format(this.getString(R.string.preferencelist_configbuttons_n), i++));
	    	}
    	}
		
		
		for(int i = 0; i < m_pairedDevices.length; i++) {
			ListPreference pd = m_pairedDevices[i];
			ListPreference drv = m_drivers[i];
			Preference btn = m_configButtons[i];
			
			String address = m_prefs.getSelectedDeviceAddress(i);
			String driver = m_prefs.getSelectedDriverName(i);

			if (address == null) {
				pd.setSummary(R.string.bluetooth_no_device);
			} else {
				
				pd.setSummary(m_prefs.getSelectedDeviceName(i) + " - " + address);
	
				CharSequence[] items = pd.getEntryValues();
				for(int j = 0; i < items.length; j++)
					if (items[j].equals(address)) {
						pd.setValueIndex(j);
						break;
					}
			}
			
			int index = -1;
			for(int j = 0; j < BluezService.DRIVER_NAMES.length; j++)
				if (BluezService.DRIVER_NAMES[j].equals(driver)) {
					index = j;
					break;
				}
	
			if (index < 0 || index >= BluezService.DRIVER_DISPLAYNAMES.length)
				drv.setSummary(R.string.preference_device_unknown);
			else
				drv.setSummary(BluezService.DRIVER_DISPLAYNAMES[index]);
			
			btn.setEnabled(m_prefs.getSelectedDriverName(i) != null && m_prefs.getSelectedDriverName(i).length() > 0);
		}
		
		m_manageBluetooth.setChecked(m_prefs.getManageBluetooth());
		
		String wakeType = m_prefs.getWakeLock() + "";
		CharSequence[] wakeValues = m_wakelockType.getEntryValues();
		m_wakelockType.setSummary("");
		
		for(int i = 0; i < wakeValues.length; i++) {
			if (wakeValues[i].toString().equals(wakeType)) {
				m_wakelockType.setSummary(m_wakelockType.getEntries()[i]);
				break;
			}
		}
		
		m_controllerCount.setSummary(String.format(this.getString(R.string.preferencelist_configure_controllers_long), m_prefs.getControllerCount()));
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
