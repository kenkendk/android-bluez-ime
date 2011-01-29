package com.hexad.bluezime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Preferences {
	
	public static final String PREFERENCES_UPDATED = "com.hexad.bluezime.preferenceschanged";
	
	private static final String PREF_DEVICE_NAME = "device name";
	private static final String PREF_DEVICE_ADDRESS = "device address";
	private static final String PREF_DRIVER_NAME = "driver name";
	
	private SharedPreferences m_prefs;
	private Context m_context;
	
	public Preferences(Context context) {
		m_prefs = PreferenceManager.getDefaultSharedPreferences(context);
		m_context = context;
	}
	
	public String getSelectedDriverName() {
		return m_prefs.getString(PREF_DRIVER_NAME, BluezService.DEFAULT_DRIVER_NAME);
	}
	
	public void setSelectedDriverName(String value) {
		Editor e = m_prefs.edit();
		e.putString(PREF_DRIVER_NAME, value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public String getSelectedDeviceName() {
		return m_prefs.getString(PREF_DEVICE_NAME, null);
	}
	
	public void setSelectedDeviceName(String value) {
		Editor e = m_prefs.edit();
		e.putString(PREF_DEVICE_NAME, value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public String getSelectedDeviceAddress() {
		return m_prefs.getString(PREF_DEVICE_ADDRESS, null);
	}
	
	public void setSelectedDeviceAddress(String value) {
		Editor e = m_prefs.edit();
		e.putString(PREF_DEVICE_ADDRESS, value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public void setSelectedDevice(String name, String address) {
		Editor e = m_prefs.edit();
		e.putString(PREF_DEVICE_NAME, name);
		e.putString(PREF_DEVICE_ADDRESS, address);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}
}
