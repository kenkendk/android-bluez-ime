package com.hexad.bluezime;

import java.util.Hashtable;

import android.content.Context;
import android.view.KeyEvent;

public class HIDGamepad extends HIDReaderBase {

	private static final boolean D = true;
	private static final boolean SHOW_RAW = true;
	
	public static final String DRIVER_NAME = "icade";
	public static final String DRIVER_DISPLAYNAME = "iCade (HID)";
	public static final String LOG_NAME = "iCade";

	private final static int[] BUTTON_MAP = new int[] {
		KeyEvent.KEYCODE_DPAD_UP,
		KeyEvent.KEYCODE_DPAD_RIGHT,
		KeyEvent.KEYCODE_DPAD_LEFT,
		KeyEvent.KEYCODE_DPAD_DOWN,

		KeyEvent.KEYCODE_DPAD_UP,
		KeyEvent.KEYCODE_DPAD_RIGHT,
		KeyEvent.KEYCODE_DPAD_LEFT,
		KeyEvent.KEYCODE_DPAD_DOWN,

		KeyEvent.KEYCODE_DPAD_UP,
		KeyEvent.KEYCODE_DPAD_RIGHT,
		KeyEvent.KEYCODE_DPAD_LEFT,
		KeyEvent.KEYCODE_DPAD_DOWN,

		KeyEvent.KEYCODE_DPAD_UP,
		KeyEvent.KEYCODE_DPAD_RIGHT,
		KeyEvent.KEYCODE_DPAD_LEFT,
		KeyEvent.KEYCODE_DPAD_DOWN,
	};
	
	public HIDGamepad(String address, String sessionId, Context context, boolean startnotification) throws Exception {
		super(address, sessionId, context, startnotification);
		
		super.doConnect();
	}

	private int m_lastButtonState = 0;
	
	@Override
	protected void handleHIDMessage(byte hidType, byte reportId, byte[] data)
			throws Exception {

		int buttonState = ((((int)data[4]) & 0xff) << 8) | (((int)data[3]) & 0xff);
		
		for(int i = 0; i < 16; i++) {
			int mask = 1 << i;
			if ((buttonState & mask) != (m_lastButtonState & mask)) {
				keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, (mask & buttonState) == 0 ? KeyEvent.ACTION_UP : KeyEvent.ACTION_DOWN);
				keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, BUTTON_MAP[i]);
				keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, 0);
				keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
				m_context.sendBroadcast(keypressBroadcast);
			}
		}
		
		m_lastButtonState = buttonState;
	}

	//We keep a copy to prevent repeated allocations
	private Hashtable<Byte, Integer> m_reportCodes = null;
	
	@Override
	protected Hashtable<Byte, Integer> getSupportedReportCodes() {
		//TODO: This should be handled by SDP inquiry
		
		if (m_reportCodes == null) {
			Hashtable<Byte, Integer> results = new Hashtable<Byte, Integer>();
			
			results.put((byte)0x0, 5); //Keypress info 
			
			m_reportCodes = results;
		}
		
		return m_reportCodes;
	}

	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}

}
