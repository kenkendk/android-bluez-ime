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
import android.os.PowerManager;
import android.preference.PreferenceManager;

public class Preferences {
	
	public static final String[] PROFILE_NAMES = new String[] {"<default>", "Profile 2", "Profile 3", "Profile 4", "Profile 5", "Profile 6", "Profile 7", "Profile 8", "Profile 9", "Profile 10" };
	public static final String[] PROFILE_KEYS = new String[] {"", "Profile2", "Profile3", "Profile4", "Profile5", "Profile6", "Profile7", "Profile8", "Profile9", "Profile10" };
	
	public static final String PREFERENCES_UPDATED = "com.hexad.bluezime.preferenceschanged";

	public static final int NO_WAKE_LOCK = 0;
	
	public static final int MAX_NO_OF_CONTROLLERS = 4;
	
	private static final String PREF_DONATION_AMOUNT = "donation amount";
	private static final String PREF_DEVICE_NAME = "device name";
	private static final String PREF_DEVICE_ADDRESS = "device address";
	private static final String PREF_DRIVER_NAME = "driver name";
	private static final String PREF_KEY_MAPPING = "key mapping";
	private static final String PREF_META_KEY_MAPPING = "meta key mapping";
	private static final String PREF_KEY_MAPPING_PROFILE = "key mapping profile";
	private static final String PREF_PROFILE_NAME = "profile name";
	private static final String PREF_CONTROLLER_COUNT = "controller count";
	private static final String PREF_MANAGE_BLUETOOTH = "manage bluetooth";
	private static final String PREF_WAKE_LOCK = "wake lock";
	
	private SharedPreferences m_prefs;
	private Context m_context;
	
	public Preferences(Context context) {
		m_prefs = PreferenceManager.getDefaultSharedPreferences(context);
		m_context = context;
	}
	
	private static String getSuffix(int pos) {
		return pos < 1 ? "" : (" #" + pos);
			
	}
	
	public String getSelectedDriverName(int pos) {
		return m_prefs.getString(PREF_DRIVER_NAME + getSuffix(pos), BluezService.DEFAULT_DRIVER_NAME);
	}
	
	public void setSelectedDriverName(String value, int pos) {
		Editor e = m_prefs.edit();
		e.putString(PREF_DRIVER_NAME + getSuffix(pos), value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public String getSelectedDeviceName(int pos) {
		return m_prefs.getString(PREF_DEVICE_NAME + getSuffix(pos), null);
	}
	
	public void setSelectedDeviceName(String value, int pos) {
		Editor e = m_prefs.edit();
		e.putString(PREF_DEVICE_NAME + getSuffix(pos), value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public String getSelectedDeviceAddress(int pos) {
		return m_prefs.getString(PREF_DEVICE_ADDRESS + getSuffix(pos), null);
	}
	
	public void setSelectedDeviceAddress(String value, int pos) {
		Editor e = m_prefs.edit();
		e.putString(PREF_DEVICE_ADDRESS + getSuffix(pos), value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public void setSelectedDevice(String name, String address, int pos) {
		Editor e = m_prefs.edit();
		e.putString(getCurrentProfile() + PREF_DEVICE_NAME + getSuffix(pos), name);
		e.putString(getCurrentProfile() + PREF_DEVICE_ADDRESS + getSuffix(pos), address);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}
	
	public int getKeyMapping(int key, int controllerNo) {
		String mapping = getCurrentProfile() + PREF_KEY_MAPPING + getSelectedDriverName(controllerNo) + (controllerNo == 0 ? "" : "#" + controllerNo) + "-" + Integer.toHexString(key); 
		return m_prefs.getInt(mapping, key);
	}
	
	public void setKeyMapping(int fromKey, int toKey, int controllerNo) {
		String mapping = getCurrentProfile() + PREF_KEY_MAPPING + getSelectedDriverName(controllerNo) + (controllerNo == 0 ? "" : "#" + controllerNo) + "-" + Integer.toHexString(fromKey); 
		Editor e = m_prefs.edit();
		e.putInt(mapping, toKey);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public int getMetaKeyMapping(int sourceKey, int controllerNo) {
		String mapping = getCurrentProfile() + PREF_META_KEY_MAPPING + getSelectedDriverName(controllerNo) + (controllerNo == 0 ? "" : "#" + controllerNo) + "-" + Integer.toHexString(sourceKey); 
		return m_prefs.getInt(mapping, 0);
	}
	
	public void setMetaKeyMapping(int sourceKey, int metaKey, int controllerNo) {
		String mapping = getCurrentProfile() + PREF_META_KEY_MAPPING + getSelectedDriverName(controllerNo) + (controllerNo == 0 ? "" : "#" + controllerNo) + "-" + Integer.toHexString(sourceKey); 
		Editor e = m_prefs.edit();
		e.putInt(mapping, metaKey);
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

	public void clearKeyMappings(int controllerNo) {
		clearByPrefix(getCurrentProfile() + PREF_KEY_MAPPING + getSelectedDriverName(controllerNo) + (controllerNo == 0 ? "" : "#" + controllerNo) + "-");
		clearByPrefix(getCurrentProfile() + PREF_META_KEY_MAPPING + getSelectedDriverName(controllerNo) + (controllerNo == 0 ? "" : "#" + controllerNo) + "-");
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
	
	public int getControllerCount() {
		return m_prefs.getInt(PREF_CONTROLLER_COUNT, 1);
	}

	public void setControllerCount(int count) {
		Editor e = m_prefs.edit();
		e.putInt(PREF_CONTROLLER_COUNT, count);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	public void setManageBluetooth(boolean value) {
		Editor e = m_prefs.edit();
		e.putBoolean(PREF_MANAGE_BLUETOOTH, value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}

	
	public void setWakeLock(int value) {
		switch(value)
		{
			case PowerManager.FULL_WAKE_LOCK:
			case PowerManager.PARTIAL_WAKE_LOCK:
			case PowerManager.SCREEN_BRIGHT_WAKE_LOCK:
			case PowerManager.SCREEN_DIM_WAKE_LOCK:
			case NO_WAKE_LOCK:
				break;
			default:
				return;
		}
		
		Editor e = m_prefs.edit();
		e.putInt(PREF_WAKE_LOCK, value);
		e.commit();
		m_context.sendBroadcast(new Intent(PREFERENCES_UPDATED));
	}
	
	public int getWakeLock() {
		return m_prefs.getInt(PREF_WAKE_LOCK, NO_WAKE_LOCK);
	}

}
