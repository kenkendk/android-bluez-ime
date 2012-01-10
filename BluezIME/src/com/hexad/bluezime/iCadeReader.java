package com.hexad.bluezime;

import java.util.Hashtable;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

public class iCadeReader extends HIDReaderBase {

	private static final boolean D = true;
	private static final boolean SHOW_RAW = true;
	
	public static final String DRIVER_NAME = "icade";
	public static final String DRIVER_DISPLAYNAME = "iCade (HID)";
	public static final String LOG_NAME = "iCade";
	
	private static final KeyEvent[] KEY_MAP = new KeyEvent[256];
	
	static {
		
		//W - E -> Up
		KEY_MAP[0x1a] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_DPAD_UP); 
		KEY_MAP[0x08] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_DPAD_UP); 
		//X - Z -> Down
		KEY_MAP[0x1b] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_DPAD_DOWN); 
		KEY_MAP[0x1d] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_DPAD_DOWN); 
		//A - Q -> Left
		KEY_MAP[0x04] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_DPAD_LEFT); 
		KEY_MAP[0x14] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_DPAD_LEFT); 
		//D - C -> Right
		KEY_MAP[0x07] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_DPAD_RIGHT); 
		KEY_MAP[0x06] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_DPAD_RIGHT);
		
		//Y - T
		KEY_MAP[0x1c] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_BUTTON_A); 
		KEY_MAP[0x17] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_BUTTON_A); 
		//U - F
		KEY_MAP[0x18] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_BUTTON_B); 
		KEY_MAP[0x09] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_BUTTON_B); 
		//I - M
		KEY_MAP[0x0c] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_BUTTON_C); 
		KEY_MAP[0x10] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_BUTTON_C); 
		//O - G
		KEY_MAP[0x12] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_BUTTON_START); 
		KEY_MAP[0x0a] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_BUTTON_START); 
		
		
		//H - R
		KEY_MAP[0x0b] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_BUTTON_X); 
		KEY_MAP[0x15] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_BUTTON_X); 
		//J - N
		KEY_MAP[0x0d] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_BUTTON_Y); 
		KEY_MAP[0x11] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_BUTTON_Y); 
		//K - P
		KEY_MAP[0x0e] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_BUTTON_Z); 
		KEY_MAP[0x13] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_BUTTON_Z); 
		//L - V
		KEY_MAP[0x0f] = new KeyEvent(KeyEvent.ACTION_DOWN, FutureKeyCodes.KEYCODE_BUTTON_SELECT); 
		KEY_MAP[0x19] = new KeyEvent(KeyEvent.ACTION_UP, FutureKeyCodes.KEYCODE_BUTTON_SELECT); 
	}

	public iCadeReader(String address, String sessionId, Context context, boolean startnotification) throws Exception {
		super(address, sessionId, context, startnotification);
		
		super.doConnect();
	}

	@Override
	protected void handleHIDMessage(byte hidType, byte reportId, byte[] data) throws Exception {
		if (reportId == 0x01) {
			if (data.length < 5) {
				Log.w(LOG_NAME, "Got keypress message with too few bytes: " + getHexString(data, 0, data.length));
			} else {
				if (D) Log.w(LOG_NAME, "Got keypress message, bytes: " + getHexString(data, 0, data.length));

				if (SHOW_RAW) {
					HIDKeyboard.DumpReport1Data(LOG_NAME, data);
				}
				
				//As we use scan codes, we can just look at the input data directly
				//Since the iCade sends different keys for up/down, 
				// we do not need to keep any state info
				for(int i = 2; i < data.length; i++) {
					if (data[i] != 0) {
						KeyEvent keycode = KEY_MAP[((int)data[i]) & 0xff];
						if (keycode != null) {
							if (D) Log.i(LOG_NAME, "Sending Android keyevent for key " + keycode.getKeyCode() + " " + (keycode.getAction() == KeyEvent.ACTION_DOWN ? "Down" : "Up"));
							
							keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, keycode.getAction());
							keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, keycode.getKeyCode());
							keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, 0);
							keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
							m_context.sendBroadcast(keypressBroadcast);
						}
					}
				}

				
			}
		} else {
			Log.w(LOG_NAME, "Got report " + hidType + ":" + reportId +  " message: " + getHexString(data, 0, data.length));
	
		}
	}

	//We keep a copy to prevent repeated allocations
	private Hashtable<Byte, Integer> m_reportCodes = null;
	
	@Override
	protected Hashtable<Byte, Integer> getSupportedReportCodes() {
		//TODO: This should be handled by SDP inquiry
		
		if (m_reportCodes == null) {
			Hashtable<Byte, Integer> results = new Hashtable<Byte, Integer>();
			
			results.put((byte)0x1, 8); //Keypress info 
			results.put((byte)0x2, 3); //Extended Keypress info 
			
			m_reportCodes = results;
		}
		
		return m_reportCodes;
	}

	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}
	
	public static int[] getButtonCodes() {
		return new int[] { KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, 
				FutureKeyCodes.KEYCODE_BUTTON_A, FutureKeyCodes.KEYCODE_BUTTON_B, FutureKeyCodes.KEYCODE_BUTTON_C, FutureKeyCodes.KEYCODE_BUTTON_START,
				FutureKeyCodes.KEYCODE_BUTTON_X, FutureKeyCodes.KEYCODE_BUTTON_Y, FutureKeyCodes.KEYCODE_BUTTON_Z, FutureKeyCodes.KEYCODE_BUTTON_SELECT
				};
	}

	public static int[] getButtonNames() {
		return new int[] { R.string.icade_stick_left, R.string.icade_stick_right, R.string.icade_stick_up, R.string.icade_stick_down, 
				R.string.icade_button_a, R.string.icade_button_b, R.string.icade_button_c, R.string.icade_button_d, 
				R.string.icade_button_x, R.string.icade_button_y, R.string.icade_button_z, R.string.icade_button_w, 
		};
	}
	

}
