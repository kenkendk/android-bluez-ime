package com.hexad.bluezime;

import java.util.HashMap;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

public class BGP100Reader extends BluetoothReader {

	private static final boolean D = false;
	
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
	
	public BGP100Reader(String address, Context context) throws Exception {
		super(address, context);
		
		//TODO: It is possible to map all buttons by looking at
		// the least significant 4 bits, and then use a 
		// 16 element integer array, instead of
		// the HashMap and thus improve performance and
		// reduce memory usage
		
		_lookup = new HashMap<Integer, KeyEvent>();

		//Bugfix: Phonejoy sends incorrect first byte, so the lower 4 bits are cleared
		
		//A
		//_lookup.put(0xb649, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_A));
		//_lookup.put(0xf609, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_A));
		_lookup.put(0xb049, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_A));
		_lookup.put(0xf009, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_A));

		//B
		//_lookup.put(0xb54a, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_B));
		//_lookup.put(0xf50a, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_B));
		_lookup.put(0xb04a, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_B));
		_lookup.put(0xf00a, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_B));

		//C
		//_lookup.put(0xb748, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_C));
		//_lookup.put(0xf708, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_C));
		_lookup.put(0xb048, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_C));
		_lookup.put(0xf008, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_C));

		//D
		//_lookup.put(0xbe41, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_X));
		//_lookup.put(0xfe01, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_X));
		_lookup.put(0xb041, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_X));
		_lookup.put(0xf001, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_X));

		//Left
		//_lookup.put(0xbb44, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
		//_lookup.put(0xfb04, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_LEFT));
		_lookup.put(0xb044, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
		_lookup.put(0xf004, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_LEFT));

		//Right
		//_lookup.put(0xbc43, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
		//_lookup.put(0xfc03, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_RIGHT));
		_lookup.put(0xb043, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
		_lookup.put(0xf003, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_RIGHT));

		//Up
		//_lookup.put(0xba45, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
		//_lookup.put(0xfa05, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_UP));
		_lookup.put(0xb045, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
		_lookup.put(0xf005, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_UP));

		//Down
		//_lookup.put(0xbd42, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
		//_lookup.put(0xfd02, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_DOWN));
		_lookup.put(0xb042, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
		_lookup.put(0xf002, new KeyEvent(KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_DPAD_DOWN));

		//R
		//_lookup.put(0xb946, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_R1));
		//_lookup.put(0xf906, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_R1));
		_lookup.put(0xb046, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_R1));
		_lookup.put(0xf006, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_R1));

		//L
		//_lookup.put(0xb847, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_L1));
		//_lookup.put(0xf807, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_L1));
		_lookup.put(0xb047, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_L1));
		_lookup.put(0xf007, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_L1));

		//Start
		//_lookup.put(0xb44b, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_START));
		//_lookup.put(0xf40b, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_START));
		_lookup.put(0xb04b, new KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_BUTTON_START));
		_lookup.put(0xf00b, new KeyEvent(KeyEvent.ACTION_UP,   KEYCODE_BUTTON_START));

	}

	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}

	@Override
	protected int parseInputData(byte[] data, int read) {
		
		if (read < 2)
			return read;
		
		int offset = 0;
		int remaining = read;
		
		while(remaining >= 2) {

			//If the high bit is set in byte 0 and not in byte 1, we accept it
			if (((data[offset] & 0x80) != 0) && ((data[offset + 1] & 0x80) == 0)) {
				int value = (data[offset] & 0xff) << 8 | (data[offset + 1] & 0xff);
				
				//Bugfix: Phonejoy sends incorrect first byte, so the lower 4 bits are cleared
				value = value & 0xf0ff;
				
				if (_lookup.containsKey(value)) {
					
					KeyEvent e = _lookup.get(value);
					
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, e.getAction());
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, e.getKeyCode());
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
					m_context.sendBroadcast(keypressBroadcast);
					
				} else {
					if (D) Log.w(getDriverName(), "Umatched button press: " + getHexString(data, offset, 2));
				}
				
				offset += 2;
				remaining -= 2;
			} else {
				offset++;
				remaining--;
			}
			
			//Otherwise just skip 1
		}
		
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
