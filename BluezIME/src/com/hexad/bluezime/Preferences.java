package com.hexad.bluezime;

import java.util.ArrayList;
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
	private static final String PREF_KEY_MAPPING = "key mapping";
	private static final String PREF_KEY_MAPPING_PROFILE = "key mapping profile";
	
	private SharedPreferences m_prefs;
	private Context m_context;
	
	public Preferences(Context context) {
		m_prefs = PreferenceManager.getDefaultSharedPreferences(context);
		m_context = context;
	}
	
	public String getSelectedDriverName() {
		return m_prefs.getString(getCurrentProfile() + PREF_DRIVER_NAME, BluezService.DEFAULT_DRIVER_NAME);
	}
	
	public void setSelectedDriverName(String value) {
		Editor e = m_prefs.edit();
		e.putString(getCurrentProfile() + PREF_DRIVER_NAME, value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public String getSelectedDeviceName() {
		return m_prefs.getString(getCurrentProfile() + PREF_DEVICE_NAME, null);
	}
	
	public void setSelectedDeviceName(String value) {
		Editor e = m_prefs.edit();
		e.putString(getCurrentProfile() + PREF_DEVICE_NAME, value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public String getSelectedDeviceAddress() {
		return m_prefs.getString(getCurrentProfile() + PREF_DEVICE_ADDRESS, null);
	}
	
	public void setSelectedDeviceAddress(String value) {
		Editor e = m_prefs.edit();
		e.putString(getCurrentProfile() + PREF_DEVICE_ADDRESS, value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public void setSelectedDevice(String name, String address) {
		Editor e = m_prefs.edit();
		e.putString(getCurrentProfile() + PREF_DEVICE_NAME, name);
		e.putString(getCurrentProfile() + PREF_DEVICE_ADDRESS, address);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}
	
	public int getKeyMapping(int key) {
		String mapping = getCurrentProfile() + PREF_KEY_MAPPING + getSelectedDriverName() + "-" + Integer.toHexString(key); 
		return m_prefs.getInt(mapping, key);
	}
	
	public void setKeyMapping(int fromKey, int toKey) {
		String mapping = getCurrentProfile() + PREF_KEY_MAPPING + getSelectedDriverName() + "-" + Integer.toHexString(fromKey); 
		Editor e = m_prefs.edit();
		e.putInt(mapping, toKey);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}
	
	public void setCurrentProfile(String value) {
		Editor e = m_prefs.edit();
		e.putString(PREF_KEY_MAPPING_PROFILE, value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}
	
	public String getCurrentProfile() {
		String prof = m_prefs.getString(PREF_KEY_MAPPING_PROFILE, "");
		if (prof != null && prof.length() > 0)
			return prof + ":";
		else
			return "";
	}
	
	public void deleteProfile(String profilename) {
		if (profilename == null || profilename.length() == 0)
			return;
		
		clearByPrefix(profilename + ":");
	}

	public void clearKeyMappings() {
		clearByPrefix(getCurrentProfile() + PREF_KEY_MAPPING + getSelectedDriverName() + "-");
	}
	
	private void clearByPrefix(String prefix) {
		ArrayList<String> toRemove = new ArrayList<String>();
		for(String s : m_prefs.getAll().keySet())
			if (s.startsWith(prefix))
				toRemove.add(s);
		
		Editor e = m_prefs.edit();
		
		for(String s : toRemove)
			e.remove(s);
		
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}
	
}
