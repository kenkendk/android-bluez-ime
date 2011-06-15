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

import android.content.Context;
import android.view.KeyEvent;

public class PhonejoyReader extends BGP100Reader {

	public static final int KEYCODE_BUTTON_L2 = 0x68;
	public static final int KEYCODE_BUTTON_R2 = 0x69;
	public static final int KEYCODE_BUTTON_SELECT = 0x6d; 

	public static final String DRIVER_NAME = "phonejoy";
	public static final String DISPLAY_NAME = "Phonejoy";
	
	public PhonejoyReader(String address, Context context) throws Exception {
		super(address, context);
		
		//R
		_lookup.put(0xb24e, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_R2));
		_lookup.put(0xf20e, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_R2));

		//L
		_lookup.put(0xb14d, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_L2));
		_lookup.put(0xf10d, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_L2));

		//Select
		_lookup.put(0xb34c, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_SELECT));
		_lookup.put(0xf30c, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_SELECT));
		
		//Left Analog stick up
		_lookup.put(0xa121, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_T));
		_lookup.put(0xe111, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_T));

		//Left Analog stick down
		_lookup.put(0xa222, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_G));
		_lookup.put(0xe212, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_G));

		//Left Analog stick left
		_lookup.put(0xa323, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F));
		_lookup.put(0xe313, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_F));

		//Left Analog stick right
		_lookup.put(0xa424, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_H));
		_lookup.put(0xe414, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_H));

		//Right Analog stick up
		_lookup.put(0xa525, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_I));
		_lookup.put(0xe515, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_I));

		//Right Analog stick down
		_lookup.put(0xa626, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_K));
		_lookup.put(0xe616, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_K));

		//Right Analog stick left
		_lookup.put(0xa727, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_J));
		_lookup.put(0xe717, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_J));

		//Right Analog stick right
		_lookup.put(0xa828, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_L));
		_lookup.put(0xe818, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_L));
		
	}

	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}

	public static int[] getButtonCodes() {
		return new int[] { KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, 
				KEYCODE_BUTTON_A, KEYCODE_BUTTON_B, KEYCODE_BUTTON_C, KEYCODE_BUTTON_X, 
				KEYCODE_BUTTON_L1, KEYCODE_BUTTON_R1, KEYCODE_BUTTON_L2, KEYCODE_BUTTON_R2, 
				KEYCODE_BUTTON_START, KEYCODE_BUTTON_SELECT,
				KeyEvent.KEYCODE_T, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_H,   
				KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_L,   
			};
	}

	public static int[] getButtonNames() {
		return new int[] { 
				R.string.bgp100_dpad_left, R.string.bgp100_dpad_right, R.string.bgp100_dpad_up, R.string.bgp100_dpad_down, 
				R.string.bgp100_button_a, R.string.bgp100_button_b, R.string.bgp100_button_c, R.string.bgp100_button_d, 
				R.string.phonejoy_button_l1, R.string.phonejoy_button_r1, R.string.phonejoy_button_l2, R.string.phonejoy_button_r2, 
				R.string.bgp100_button_start, R.string.phonejoy_button_select,
				R.string.phonejoy_analog_left_up, R.string.phonejoy_analog_left_down, R.string.phonejoy_analog_left_left, R.string.phonejoy_analog_left_right,
				R.string.phonejoy_analog_right_up, R.string.phonejoy_analog_right_down, R.string.phonejoy_analog_right_left, R.string.phonejoy_analog_right_right,
		};
	}
	
}
