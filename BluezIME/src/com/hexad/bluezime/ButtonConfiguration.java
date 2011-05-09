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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Hashtable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.KeyEvent;

public class ButtonConfiguration extends PreferenceActivity {

	private Preferences m_prefs;
	private Hashtable<Integer, String> m_name_lookup;
	private Hashtable<Preference, Integer> m_list_lookup;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.buttonconfiguration);
		
		m_prefs = new Preferences(this);
		
		//Use reflection to build a list of possible keys we can send
		m_name_lookup = new Hashtable<Integer, String>();
		for (Field f : KeyEvent.class.getDeclaredFields()) {
			String name = f.getName();
			if (name.startsWith("KEYCODE_"))
				try {
					m_name_lookup.put(f.getInt(null), name.substring("KEYCODE_".length()));
				} catch (Exception e) {	}
		}
		
		String driver = m_prefs.getSelectedDriverName();
		
		int[] buttonCodes;
		int[] buttonNames; 
		
		if (driver.equals(ZeemoteReader.DRIVER_NAME)) {
			buttonCodes = ZeemoteReader.getButtonCodes();
			buttonNames = ZeemoteReader.getButtonNames();
		} else if (driver.equals(BGP100Reader.DRIVER_NAME)) {
			buttonCodes = BGP100Reader.getButtonCodes();
			buttonNames = BGP100Reader.getButtonNames();
		} else if (driver.equals(PhonejoyReader.DRIVER_NAME)) {
			buttonCodes = PhonejoyReader.getButtonCodes();
			buttonNames = PhonejoyReader.getButtonNames();
		} else if (driver.equals(iControlPadReader.DRIVER_NAME)) {
			buttonCodes = iControlPadReader.getButtonCodes();
			buttonNames = iControlPadReader.getButtonNames();
		} else {
			buttonCodes = new int[0];
			buttonNames = new int[0];
		}
		
		PreferenceCategory buttonCategory = (PreferenceCategory)this.findPreference("cat_buttons");
		Preference resetButton = (Preference)this.findPreference("reset_button");
		resetButton.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				m_prefs.clearKeyMappings();
				return true;
			}
		});
		
		
		CharSequence[] entries = new CharSequence[KeyEvent.getMaxKeyCode()];
		CharSequence[] entryValues = new CharSequence[KeyEvent.getMaxKeyCode()];
		
		for(int i = 0; i < entries.length; i++)
		{
			if (m_name_lookup.containsKey(i))
				entries[i] = m_name_lookup.get(i);
			else
				entries[i] = "UNKNOWN - 0x" + Integer.toHexString(i);
			entryValues[i] =  Integer.toString(i);
		}
		
		m_list_lookup = new Hashtable<Preference, Integer>();
		
		for(int i = 0; i < buttonCodes.length; i++)
		{
			ListPreference lp = new ListPreference(this);
			m_list_lookup.put(lp, buttonCodes[i]);
			
			lp.setTitle(buttonNames[i]);
			lp.setEntries(entries);
			lp.setEntryValues(entryValues);

			lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					int fromKey = m_list_lookup.get(preference);
					int toKey = Integer.parseInt((String)newValue);
					m_prefs.setKeyMapping(fromKey, toKey);
					return true;
				}
			});
			
			buttonCategory.addPreference(lp);
		}
		
		updateDisplay();
		registerReceiver(preferenceUpdateMonitor, new IntentFilter(Preferences.PREFERENCES_UPDATED));
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();

    	unregisterReceiver(preferenceUpdateMonitor);
	}
	
	private void updateDisplay() {
		
		for(Preference p : Collections.list(m_list_lookup.keys()))
		{
			ListPreference lp = (ListPreference)p;
			int code = m_prefs.getKeyMapping(m_list_lookup.get(p));
			lp.setSummary(m_name_lookup.containsKey(code) ? m_name_lookup.get(code) : "UNKNOWN - 0x" + Integer.toHexString(code));	
			lp.setValue(Integer.toString(code));
		}
	}
	
	private BroadcastReceiver preferenceUpdateMonitor = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateDisplay();
		}
	};	
	
}
