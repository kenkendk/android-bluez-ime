package com.hexad.bluezime;

import java.util.Hashtable;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

public class HIDKeyboard extends HIDReaderBase {

	private static final boolean D = true;
	private static final boolean SHOW_RAW = true;
	
	public static final String DRIVER_NAME = "hidkeyboard";
	public static final String DRIVER_DISPLAYNAME = "Keyboard (HID)";
	public static final String LOG_NAME = "HIDKeyboard";
	
	public static final int HIDP_LEFTCTRL = 0x01;
	public static final int HIDP_LEFTSHIFT = 0x02;
	public static final int HIDP_LEFTALT = 0x04;
	public static final int HIDP_LEFTGUI = 0x08;
	public static final int HIDP_RIGHTCTRL = 0x10;
	public static final int HIDP_RIGHTSHIFT = 0x20;
	public static final int HIDP_RIGHTALT = 0x40;
	public static final int HIDP_RIGHTGUI = 0x80;
	
	public HIDKeyboard(String address, String sessionId, Context context, boolean startnotification) throws Exception {
		super(address, sessionId, context, startnotification);
		
		super.doConnect();
	}

	//Buffer for calculating pressed key states
	private int[] m_pressed = new int[6];
	
	//Buffer for pressed keys in last scan, used to figure out what changed
	private int[] m_lastPressed = new int[6];
	private int m_lastPressedCount = 0;
	private int m_lastModifiers = 0;
	private int m_lastExtendedKeys = 0;
	
	//List of modifier keys we can send key up/down events for
	private static final int[] META_KEY_MASKS = new int[] { 
		FutureKeyCodes.META_ALT_LEFT_ON,
		FutureKeyCodes.META_ALT_RIGHT_ON,
		FutureKeyCodes.META_CTRL_LEFT_ON,
		FutureKeyCodes.META_CTRL_RIGHT_ON,
		FutureKeyCodes.META_SHIFT_LEFT_ON,
		FutureKeyCodes.META_SHIFT_RIGHT_ON,
		FutureKeyCodes.META_META_LEFT_ON,
		FutureKeyCodes.META_META_RIGHT_ON
	};
	
	//List of key codes that correspond to the mask above
	private static final int[] META_KEY_KEYS = new int[] {
		FutureKeyCodes.KEYCODE_ALT_LEFT,
		FutureKeyCodes.KEYCODE_ALT_RIGHT,
		FutureKeyCodes.KEYCODE_CTRL_LEFT,
		FutureKeyCodes.KEYCODE_CTRL_RIGHT,
		FutureKeyCodes.KEYCODE_SHIFT_LEFT,
		FutureKeyCodes.KEYCODE_SHIFT_RIGHT,
		FutureKeyCodes.KEYCODE_META_LEFT,
		FutureKeyCodes.KEYCODE_META_RIGHT
	};

	//Keys that are reported through reportId 2, these are bitmaps
	private static int[] EXT_REPORT_KEYS = new int[] {
		0,	//0x000001
		0,	//0x000002
		0,	//0x000004
		0,	//0x000008
		
		0,	//0x000010
		0,	//0x000020
		0,	//0x000040
		0,	//0x000080
		
		FutureKeyCodes.KEYCODE_ENVELOPE,			//0x000100
		FutureKeyCodes.KEYCODE_HOME,				//0x000200
		0,	//0x000400
		0,	//0x000800
		
		0,	//0x001000
		0,	//0x002000
		0,	//0x004000
		0,	//0x008000

		FutureKeyCodes.KEYCODE_VOLUME_UP, 			//0x010000
		FutureKeyCodes.KEYCODE_VOLUME_DOWN, 		//0x020000
		FutureKeyCodes.KEYCODE_MUTE,				//0x040000
		FutureKeyCodes.KEYCODE_MEDIA_NEXT,			//0x080000
		
		FutureKeyCodes.KEYCODE_MEDIA_PLAY_PAUSE, 	//0x100000
		FutureKeyCodes.KEYCODE_MEDIA_PREVIOUS, 		//0x200000
		FutureKeyCodes.KEYCODE_MEDIA_STOP,			//0x400000
		FutureKeyCodes.KEYCODE_LANGUAGE_SWITCH,		//0x800000
	};

	//Map of HID keycodes to Android key event codes
	private static final int[] HID2KEYCODE = new int[256];

	//We initialize them here because it is easier to read this way,
	// downside is that we may map the same key twice
	static {
		
		//TODO: Rewrite using the HUT section 10:
		http://www.usb.org/developers/devclass_docs/Hut1_11.pdf
		
		HID2KEYCODE[0x1e] = FutureKeyCodes.KEYCODE_1;
		HID2KEYCODE[0x1f] = FutureKeyCodes.KEYCODE_2;
		HID2KEYCODE[0x20] = FutureKeyCodes.KEYCODE_3;
		HID2KEYCODE[0x21] = FutureKeyCodes.KEYCODE_4;
		HID2KEYCODE[0x22] = FutureKeyCodes.KEYCODE_5;
		HID2KEYCODE[0x23] = FutureKeyCodes.KEYCODE_6;
		HID2KEYCODE[0x24] = FutureKeyCodes.KEYCODE_7;
		HID2KEYCODE[0x25] = FutureKeyCodes.KEYCODE_8;
		HID2KEYCODE[0x26] = FutureKeyCodes.KEYCODE_9;
		HID2KEYCODE[0x27] = FutureKeyCodes.KEYCODE_0;

		HID2KEYCODE[0x14] = FutureKeyCodes.KEYCODE_Q;
		HID2KEYCODE[0x1a] = FutureKeyCodes.KEYCODE_W;
		HID2KEYCODE[0x08] = FutureKeyCodes.KEYCODE_E;
		HID2KEYCODE[0x15] = FutureKeyCodes.KEYCODE_R;
		HID2KEYCODE[0x17] = FutureKeyCodes.KEYCODE_T;
		HID2KEYCODE[0x1c] = FutureKeyCodes.KEYCODE_Y;
		HID2KEYCODE[0x18] = FutureKeyCodes.KEYCODE_U;
		HID2KEYCODE[0x0c] = FutureKeyCodes.KEYCODE_I;
		HID2KEYCODE[0x12] = FutureKeyCodes.KEYCODE_O;
		HID2KEYCODE[0x13] = FutureKeyCodes.KEYCODE_P;

		HID2KEYCODE[0x04] = FutureKeyCodes.KEYCODE_A;
		HID2KEYCODE[0x16] = FutureKeyCodes.KEYCODE_S;
		HID2KEYCODE[0x07] = FutureKeyCodes.KEYCODE_D;
		HID2KEYCODE[0x09] = FutureKeyCodes.KEYCODE_F;
		HID2KEYCODE[0x0a] = FutureKeyCodes.KEYCODE_G;
		HID2KEYCODE[0x0b] = FutureKeyCodes.KEYCODE_H;
		HID2KEYCODE[0x0d] = FutureKeyCodes.KEYCODE_J;
		HID2KEYCODE[0x0e] = FutureKeyCodes.KEYCODE_K;
		HID2KEYCODE[0x0f] = FutureKeyCodes.KEYCODE_L;
		HID2KEYCODE[0x2a] = FutureKeyCodes.KEYCODE_DEL;

		HID2KEYCODE[0x1d] = FutureKeyCodes.KEYCODE_Z;
		HID2KEYCODE[0x1b] = FutureKeyCodes.KEYCODE_X;
		HID2KEYCODE[0x06] = FutureKeyCodes.KEYCODE_C;
		HID2KEYCODE[0x19] = FutureKeyCodes.KEYCODE_V;
		HID2KEYCODE[0x05] = FutureKeyCodes.KEYCODE_B;
		HID2KEYCODE[0x11] = FutureKeyCodes.KEYCODE_N;
		HID2KEYCODE[0x10] = FutureKeyCodes.KEYCODE_M;
		HID2KEYCODE[0x36] = FutureKeyCodes.KEYCODE_COMMA;
		HID2KEYCODE[0x37] = FutureKeyCodes.KEYCODE_PERIOD;
		HID2KEYCODE[0x28] = FutureKeyCodes.KEYCODE_ENTER;

		HID2KEYCODE[0x33] = FutureKeyCodes.KEYCODE_SEMICOLON;

		HID2KEYCODE[0x52] = FutureKeyCodes.KEYCODE_DPAD_UP;
		HID2KEYCODE[0x51] = FutureKeyCodes.KEYCODE_DPAD_DOWN;
		HID2KEYCODE[0x50] = FutureKeyCodes.KEYCODE_DPAD_LEFT;
		HID2KEYCODE[0x4f] = FutureKeyCodes.KEYCODE_DPAD_RIGHT;
		HID2KEYCODE[0x4b] = FutureKeyCodes.KEYCODE_PAGE_UP;
		HID2KEYCODE[0x4a] = FutureKeyCodes.KEYCODE_PAGE_DOWN;
		HID2KEYCODE[0x4d] = FutureKeyCodes.KEYCODE_MOVE_HOME;
		HID2KEYCODE[0x4c] = FutureKeyCodes.KEYCODE_MOVE_END;

		HID2KEYCODE[0x29] = FutureKeyCodes.KEYCODE_ESCAPE;
		HID2KEYCODE[0x2b] = FutureKeyCodes.KEYCODE_TAB;
		HID2KEYCODE[0x49] = FutureKeyCodes.KEYCODE_INSERT;

		HID2KEYCODE[0x34] = FutureKeyCodes.KEYCODE_APOSTROPHE;
		HID2KEYCODE[0x35] = FutureKeyCodes.KEYCODE_GRAVE;
		HID2KEYCODE[0x2f] = FutureKeyCodes.KEYCODE_LEFT_BRACKET;
		HID2KEYCODE[0x30] = FutureKeyCodes.KEYCODE_RIGHT_BRACKET;
		HID2KEYCODE[0x31] = FutureKeyCodes.KEYCODE_BACKSLASH;
		HID2KEYCODE[0x38] = FutureKeyCodes.KEYCODE_SLASH;
		HID2KEYCODE[0x2d] = FutureKeyCodes.KEYCODE_MINUS;
		HID2KEYCODE[0x2e] = FutureKeyCodes.KEYCODE_EQUALS;
		HID2KEYCODE[0x2c] = FutureKeyCodes.KEYCODE_SPACE;

		
		HID2KEYCODE[0x3a] = FutureKeyCodes.KEYCODE_F1;
		HID2KEYCODE[0x3b] = FutureKeyCodes.KEYCODE_F2;
		HID2KEYCODE[0x3d] = FutureKeyCodes.KEYCODE_F3;
		HID2KEYCODE[0x3d] = FutureKeyCodes.KEYCODE_F4;
		HID2KEYCODE[0x3e] = FutureKeyCodes.KEYCODE_F5;
		HID2KEYCODE[0x3f] = FutureKeyCodes.KEYCODE_F6;
		HID2KEYCODE[0x40] = FutureKeyCodes.KEYCODE_F7;
		HID2KEYCODE[0x41] = FutureKeyCodes.KEYCODE_F8;
		HID2KEYCODE[0x42] = FutureKeyCodes.KEYCODE_F9;
		HID2KEYCODE[0x43] = FutureKeyCodes.KEYCODE_F10;
		HID2KEYCODE[0x44] = FutureKeyCodes.KEYCODE_F11;
		HID2KEYCODE[0x45] = FutureKeyCodes.KEYCODE_F12;
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
	
	public static int ParseModifiers(int data) {
		int modifiers = 0;
		if ((data & HIDP_LEFTCTRL) != 0)
			modifiers |= FutureKeyCodes.META_CTRL_LEFT_ON | FutureKeyCodes.META_CTRL_ON;
		if ((data & HIDP_RIGHTCTRL) != 0)
			modifiers |= FutureKeyCodes.META_CTRL_RIGHT_ON | FutureKeyCodes.META_CTRL_ON;
		if ((data & HIDP_LEFTALT) != 0)
			modifiers |= FutureKeyCodes.META_ALT_LEFT_ON | FutureKeyCodes.META_ALT_ON;
		if ((data & HIDP_RIGHTALT) != 0)
			modifiers |= FutureKeyCodes.META_ALT_RIGHT_ON | FutureKeyCodes.META_ALT_ON;
		if ((data & HIDP_LEFTSHIFT) != 0)
			modifiers |= FutureKeyCodes.META_SHIFT_LEFT_ON | FutureKeyCodes.META_SHIFT_ON;
		if ((data & HIDP_RIGHTSHIFT) != 0)
			modifiers |= FutureKeyCodes.META_SHIFT_RIGHT_ON | FutureKeyCodes.META_SHIFT_ON;
		if ((data & HIDP_LEFTGUI) != 0)
			modifiers |= FutureKeyCodes.META_META_LEFT_ON | FutureKeyCodes.META_META_ON;
		if ((data & HIDP_RIGHTGUI) != 0)
			modifiers |= FutureKeyCodes.META_META_RIGHT_ON | FutureKeyCodes.META_META_ON;
		
		return modifiers;
	}
	
	public static void DumpReport1Data(String logname, byte[] data) {
		String tmp = "";

		if (data[0] != 0)
			tmp += "[0x" + getHexString(data, 0, 1) + "] ";
		
		boolean any = false;
		for(int i = 2; i < data.length; i++) {
			if (data[i] != 0) {
				if (!any)
					any = true;
				else
					tmp += ",";
				
				tmp += "0x" + getHexString(data, i, i + 1);
			}
		}
		
		Log.i(LOG_NAME, tmp);
	}

	@Override
	protected void handleHIDMessage(byte hidType, byte reportId, byte[] data) throws Exception {
		if (reportId == 0x01) {
			if (data.length < 5) {
				Log.w(LOG_NAME, "Got keypress message with too few bytes: " + getHexString(data, 0, data.length));
			} else {
				if (D) Log.w(LOG_NAME, "Got keypress message, bytes: " + getHexString(data, 0, data.length));

				if (SHOW_RAW) {
					DumpReport1Data(LOG_NAME, data);
				}

				int scanmodifiers = ((int)data[0]) & 0xff;
				int modifiers = ParseModifiers(scanmodifiers);
				
				//Figure out if any meta keys (CTRL, SHIFT, etc) have changed state,
				// and send an appropriate key event
				for(int i = 0; i < META_KEY_MASKS.length; i++) {
					if ((m_lastModifiers & META_KEY_MASKS[i]) != (modifiers & META_KEY_MASKS[i])) {
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, (modifiers & META_KEY_MASKS[i]) == 0 ? KeyEvent.ACTION_UP : KeyEvent.ACTION_DOWN);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, META_KEY_KEYS[i]);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, 0);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
						m_context.sendBroadcast(keypressBroadcast);
						
					}
				}
				
				//Re-allocate if we suddenly get more data than expected.
				//This is done to prevent repeated allocations
				if (m_pressed.length < data.length - 2) {
					int[] tmp = new int[data.length -2];
					System.arraycopy(m_pressed, 0, tmp, 0, m_pressed.length);
					m_pressed = tmp;
					
					tmp = new int[data.length - 2];
					System.arraycopy(m_lastPressed, 0, tmp, 0, m_lastPressed.length);
					m_lastPressed = tmp;
				}

				//First we map all key scan codes to keyevent codes
				int pressedcount = 0;
				for(int i = 2; i < data.length; i++) {
					if (data[i] != 0) {
						int keycode = HID2KEYCODE[((int)data[i]) & 0xff];
						if (keycode != 0) {
							m_pressed[pressedcount] = keycode;
							pressedcount++;
						}
					}
				}
				
				//Then we figure out which have changed
				for(int i = 0; i < pressedcount; i++) {
					int keycode = m_pressed[i];
					int pressed = -1;
					for(int j = 0; j < m_lastPressedCount; j++) {
						if (m_lastPressed[j] == keycode) {
							pressed = j;
							m_lastPressed[j] = 0;
							break;
						}
					}
					
					//The key was not pressed before, send keydown event
					if (pressed == -1) {
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, KeyEvent.ACTION_DOWN);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, keycode);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, modifiers);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
						m_context.sendBroadcast(keypressBroadcast);
					}
				}
				
				for(int i = 0; i < m_lastPressedCount; i++) {
					//If we have non-zero entries here, the key is no longer pressed
					if (m_lastPressed[i] != 0) {
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, KeyEvent.ACTION_UP);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, m_lastPressed[i]);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, modifiers);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
						m_context.sendBroadcast(keypressBroadcast);
					}
				}

				//Make the current last, and save the current 
				// as a buffer for the next event set
				int[] tmp = m_lastPressed;
				m_lastPressed = m_pressed;
				m_pressed = tmp;
				m_lastPressedCount = pressedcount;
				m_lastModifiers = modifiers;
			}
			
		} else if (reportId == 0x02) {
			if (data.length < 3) {
				Log.w(LOG_NAME, "Got ext keypress message with too few bytes: " + getHexString(data, 0, data.length));
			} else {
				int scanvalue = 
						((((int)data[0]) & 0xff) << 16) |
						((((int)data[1]) & 0xff) << 8) | 
						(((int)data[2]) & 0xff)
						;
				
				if (SHOW_RAW)
					Log.i(LOG_NAME, scanvalue + "");
				
				for(int i = 0; i < EXT_REPORT_KEYS.length; i++) {
					if (EXT_REPORT_KEYS[i] != 0) {
						int mask = 1 << i;
						if ((scanvalue & mask) != (m_lastExtendedKeys & mask)) {
							keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, (scanvalue & mask) == 0 ? KeyEvent.ACTION_UP : KeyEvent.ACTION_DOWN);
							keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, EXT_REPORT_KEYS[i]);
							keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, m_lastModifiers);
							keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
							m_context.sendBroadcast(keypressBroadcast);
						}
					}
				}
				
				m_lastExtendedKeys = scanvalue;
			}
		} else {
			Log.w(LOG_NAME, "Got report " + hidType + ":" + reportId +  " message: " + getHexString(data, 0, data.length));
	
		}
	}

}
