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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Preferences {
	
	public static final String[] PROFILE_NAMES = new String[] {"<default>", "Profile 2", "Profile 3", "Profile 4", "Profile 5" };
	public static final String[] PROFILE_KEYS = new String[] {"", "Profile2", "Profile3", "Profile4", "Profile5" };
	
	public static final String PREFERENCES_UPDATED = "com.hexad.bluezime.preferenceschanged";

	private static final String PREF_DONATION_AMOUNT = "donation amount";
	private static final String PREF_DEVICE_NAME = "device name";
	private static final String PREF_DEVICE_ADDRESS = "device address";
	private static final String PREF_DRIVER_NAME = "driver name";
	private static final String PREF_KEY_MAPPING = "key mapping";
	private static final String PREF_KEY_MAPPING_PROFILE = "key mapping profile";
	private static final String PREF_PROFILE_NAME = "profile name";
	private static final String PREF_MANAGE_BLUETOOTH = "manage bluetooth";
	
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
	
	public int getDonatedAmount() {
		return m_prefs.getInt(PREF_DONATION_AMOUNT, 0);
	}

	public void setDonatedAmount(int amount) {
		Editor e = m_prefs.edit();
		e.putInt(PREF_DONATION_AMOUNT, amount);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public String getProfileDisplayName(String profilename) {
		String profKey = profilename;
		if (profKey.endsWith(":"))
			profKey = profKey.substring(0, profKey.length() - 1);
		
		String defaultName = profKey;
		for(int i = 0; i < PROFILE_KEYS.length; i++)
			if (PROFILE_KEYS[i].equals(profKey)) {
				defaultName = PROFILE_NAMES[i];
				break;
			}
		
		String res = m_prefs.getString(profKey + ":" + PREF_PROFILE_NAME, defaultName);
		if (res == null || res.equals(""))
			res = defaultName;
		
		return res;
	}

	public String getProfileDisplayName() {
		return getProfileDisplayName(getCurrentProfile());
	}
	
	public void setProfileDisplayName(String value) {
		Editor e = m_prefs.edit();
		e.putString(getCurrentProfile() + PREF_PROFILE_NAME, value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public boolean getManageBluetooth() {
		return m_prefs.getBoolean(PREF_MANAGE_BLUETOOTH, true);
	}
	
	public void setManageBluetooth(boolean value) {
		Editor e = m_prefs.edit();
		e.putBoolean(PREF_MANAGE_BLUETOOTH, value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

}
