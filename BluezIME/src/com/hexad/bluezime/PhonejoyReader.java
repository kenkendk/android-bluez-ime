package com.hexad.bluezime;

import android.content.Context;
import android.view.KeyEvent;

public class PhonejoyReader extends BGP100Reader {

	public static final int KEYCODE_BUTTON_L2 = 0x68;
	public static final int KEYCODE_BUTTON_R2 = 0x69;
	public static final int KEYCODE_BUTTON_SELECT = 0x6d; 

	public static final String DRIVER_NAME = "phonejoy";
	public static final String DISPLAY_NAME = "Phonejoy / Vinyson";
	
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
	}

	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}

	public static int[] getButtonCodes() {
		return new int[] { KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KEYCODE_BUTTON_A, KEYCODE_BUTTON_B, KEYCODE_BUTTON_C, KEYCODE_BUTTON_X, KEYCODE_BUTTON_L1, KEYCODE_BUTTON_R1, KEYCODE_BUTTON_L2, KEYCODE_BUTTON_R2, KEYCODE_BUTTON_START, KEYCODE_BUTTON_SELECT };
	}

	public static int[] getButtonNames() {
		return new int[] { R.string.bgp100_dpad_left, R.string.bgp100_dpad_right, R.string.bgp100_dpad_up, R.string.bgp100_dpad_down, R.string.bgp100_button_a, R.string.bgp100_button_b, R.string.bgp100_button_c, R.string.bgp100_button_d, R.string.joyphone_button_l1, R.string.joyphone_button_r1, R.string.joyphone_button_l2, R.string.joyphone_button_r2, R.string.bgp100_button_start, R.string.joyphone_button_select };
	}
	
}
