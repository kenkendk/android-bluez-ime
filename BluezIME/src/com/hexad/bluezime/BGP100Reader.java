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

import java.util.HashMap;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

public class BGP100Reader extends RfcommReader {

	private static final boolean D = false;
	private static final boolean D2 = false;
	
	//These are from API level 9
	public static final int KEYCODE_BUTTON_A = 0x60;
	public static final int KEYCODE_BUTTON_B = 0x61;
	public static final int KEYCODE_BUTTON_C = 0x62;
	public static final int KEYCODE_BUTTON_X = 0x63;
	public static final int KEYCODE_BUTTON_L1 = 0x66;
	public static final int KEYCODE_BUTTON_R1 = 0x67;
	public static final int KEYCODE_BUTTON_START = 0x6c; 
	
	public static final String DRIVER_NAME = "bgp100";
	public static final String DISPLAY_NAME = "MSI Chainpus BGP100";
	
	protected HashMap<Integer, KeyEvent> _lookup;
	
	protected HashMap<Integer, Boolean> _keyStates;
	
	public BGP100Reader(String address, String sessionId, Context context, boolean startnotification) throws Exception {
		super(address, sessionId, context, startnotification);
		
		//TODO: It is possible to map all buttons by looking at
		// the least significant 4 bits, and then use a 
		// 16 element integer array, instead of
		// the HashMap and thus improve performance and
		// reduce memory usage
		
		_lookup = new HashMap<Integer, KeyEvent>();

		//A
		_lookup.put(0xb649, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_A));
		_lookup.put(0xf609, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_A));

		//B
		_lookup.put(0xb54a, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_B));
		_lookup.put(0xf50a, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_B));

		//C
		_lookup.put(0xb748, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_C));
		_lookup.put(0xf708, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_C));

		//D
		_lookup.put(0xbe41, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_X));
		_lookup.put(0xfe01, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_X));

		//Left
		_lookup.put(0xbb44, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
		_lookup.put(0xfb04, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_LEFT));

		//Right
		_lookup.put(0xbc43, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
		_lookup.put(0xfc03, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_RIGHT));

		//Up
		_lookup.put(0xba45, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
		_lookup.put(0xfa05, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_UP));

		//Down
		_lookup.put(0xbd42, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
		_lookup.put(0xfd02, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_DOWN));

		//R
		_lookup.put(0xb946, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_R1));
		_lookup.put(0xf906, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_R1));

		//L
		_lookup.put(0xb847, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_L1));
		_lookup.put(0xf807, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_L1));

		//Start
		_lookup.put(0xb44b, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_START));
		_lookup.put(0xf40b, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_START));

	}

	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}

	@Override
	protected int parseInputData(byte[] data, int read) {
		
		//For debugging, we keep track of the current key states
		if (D2 && _keyStates == null) {
			_keyStates = new HashMap<Integer, Boolean>();
			
			for(Integer i : _lookup.keySet())
				_keyStates.put(_lookup.get(i).getKeyCode(), false);
		}
		
		if (read < 2)
			return read;
		
		int offset = 0;
		int remaining = read;
		
		if (D) Log.w(getDriverName(), "Sequence read from device: " + getHexString(data, 0, read));
		
		while(remaining >= 2) {

			//If the high bit is set in byte 0 and not in byte 1, we accept it
			if (((data[offset] & 0x80) != 0) && ((data[offset + 1] & 0x80) == 0)) {
				
				//TODO: This is actually for Phonejoy, not BGP100
				if ((data[offset] & 0xff) == 0xff) {
					
					//Make sure we have the next byte as well
					if (remaining < 3)
						return remaining;
					
					int axis = (data[offset + 1] & 0xff) - 0x11;
					if (axis >= 0 && axis < 4) {
						int axis_value = data[offset + 2] & 0xff;
						int normalized = Math.max(-127, Math.min(127, (axis_value - 127)));
						
						if (D) Log.d(getDriverName(), "Axis " + axis + " changed to " + normalized + " (" + axis_value + ")");
						
						directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_DIRECTION, axis);
						directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_VALUE, normalized);
						m_context.sendBroadcast(directionBroadcast);
						
					} else {
						if (D) Log.w(getDriverName(), "Unexpected axis: " + axis + ", raw value: " + data[offset + 1]);
					}
					
					offset += 3;
					remaining -= 3;
					
				} else {
					int value = (data[offset] & 0xff) << 8 | (data[offset + 1] & 0xff);
					
					if (_lookup.containsKey(value)) {
						
						KeyEvent e = _lookup.get(value);
						
						if (D) Log.w(getDriverName(), "Sending button event, button: " + e.getKeyCode() + ", direction: " + (e.getAction() == KeyEvent.ACTION_DOWN ? "DOWN" : "UP"));
						
						if (D2) {
							if (_keyStates.containsKey(e.getKeyCode())) {
								boolean oldState = _keyStates.get(e.getKeyCode());
								boolean newState = e.getAction() == KeyEvent.ACTION_DOWN; 
								
								if (oldState == newState)
									Log.w(getDriverName(), "Probably a bug, got a new event, but the state was not changed? KeyCode: " + e.getKeyCode() + ", direction: " + (newState ? "DOWN" : "UP"));
								else
									Log.d(getDriverName(), "Changed state for KeyCode: " + e.getKeyCode() + ", direction: " + (newState ? "DOWN" : "UP"));
								
								_keyStates.remove(e.getKeyCode());
								_keyStates.put(e.getKeyCode(), newState);
								
							} else {
								Log.e(getDriverName(), "Bug in logging, the key was found, but then not? KeyCode:" + e.getKeyCode());
							}
								
						}
						
						
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, e.getAction());
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, e.getKeyCode());
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
						m_context.sendBroadcast(keypressBroadcast);

					} else {
						if (D) Log.w(getDriverName(), "Unmatched button press: " + getHexString(data, offset, 2) + ", full block: " + getHexString(data, 0, data.length));
					}
					
					offset += 2;
					remaining -= 2;
				}
			} else {
				if (D) Log.w(getDriverName(), "Umatched byte #" + offset + " in: " + getHexString(data, 0, read));
				offset++;
				remaining--;
			}
		}
		
		if (D2 && remaining > 0) Log.i(getDriverName(), "Reports remaining: " + remaining);
		
		return remaining;
	}

	@Override
	protected void validateWelcomeMessage(byte[] data, int read) {
		//TODO: Find some documentation that explains how to parse the message
	}

	public static int[] getButtonCodes() {
		return new int[] { KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KEYCODE_BUTTON_A, KEYCODE_BUTTON_B, KEYCODE_BUTTON_C, KEYCODE_BUTTON_X, KEYCODE_BUTTON_L1, KEYCODE_BUTTON_R1, KEYCODE_BUTTON_START };
	}

	public static int[] getButtonNames() {
		return new int[] { R.string.bgp100_dpad_left, R.string.bgp100_dpad_right, R.string.bgp100_dpad_up, R.string.bgp100_dpad_down, R.string.bgp100_button_a, R.string.bgp100_button_b, R.string.bgp100_button_c, R.string.bgp100_button_d, R.string.bgp100_button_l, R.string.bgp100_button_r, R.string.bgp100_button_start };
	}

}
