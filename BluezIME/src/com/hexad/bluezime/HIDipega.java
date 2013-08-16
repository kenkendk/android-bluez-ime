package com.hexad.bluezime;

import java.util.Hashtable;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

public class HIDipega extends HIDReaderBase {

	private static final boolean D = false;
	private static final boolean SHOW_RAW = false;
	
	public static final String DRIVER_NAME = "ipega";
	public static final String DRIVER_DISPLAYNAME = "ipega Bluetooth Controller (HID)";
	public static final String LOG_NAME = "ipega";
	
    public static final int KEYCODE_UNUSED = 0x0;

    private static final int[] DPAD = new int[] {
        KeyEvent.KEYCODE_DPAD_UP, 		//Byte A, bit 0
        KeyEvent.KEYCODE_DPAD_RIGHT, 	//Byte A, bit 1
        KeyEvent.KEYCODE_DPAD_DOWN, 	//Byte A, bit 2
        KeyEvent.KEYCODE_DPAD_LEFT		//Byte A, bit 3

    };
    private static final byte[] DPAD_MAP = new byte[] {
        1,	 	//up, 			bit 0000001
        3,	 	//up-right, 	bit 0000011
        2,		//right, 		bit 0000010
        6,		//right-down, 	bit 0000110
        4,		//down			bit 0000100
        12,		//down-left		bit 0001100
        8,		//left			bit 0001000
        9		//left-up		bit 0001001
    };
	
	private static final int[] KEYS = new int[] {
        FutureKeyCodes.KEYCODE_BUTTON_SELECT, 			//Byte B, bit 0
        FutureKeyCodes.KEYCODE_BUTTON_START,			//Byte B, bit 1
		KEYCODE_UNUSED, 				                //Byte B, bit 2
		KEYCODE_UNUSED, 				                //Byte B, bit 3
		KEYCODE_UNUSED, 			                	//Byte B, bit 4
		KEYCODE_UNUSED, 		                		//Byte B, bit 5
		KEYCODE_UNUSED, 		                   		//Byte B, bit 6
		KEYCODE_UNUSED, 		                		//Byte B, bit 7
        FutureKeyCodes.KEYCODE_BUTTON_X, 				//Byte A, bit 0
        FutureKeyCodes.KEYCODE_BUTTON_A, 				//Byte A, bit 1
        FutureKeyCodes.KEYCODE_BUTTON_B, 				//Byte A, bit 2
        FutureKeyCodes.KEYCODE_BUTTON_Y, 				//Byte A, bit 3
        FutureKeyCodes.KEYCODE_BUTTON_L1, 				//Byte A, bit 4
        FutureKeyCodes.KEYCODE_BUTTON_R1, 				//Byte A, bit 5
		KEYCODE_UNUSED, 				                //Byte A, bit 6
		KEYCODE_UNUSED, 				                //Byte A, bit 7
	};
	private static final int[] ANALOG_KEYS = new int[] {
		KeyEvent.KEYCODE_D, //Nub1 right
		KeyEvent.KEYCODE_A, //Nub1 left
		KeyEvent.KEYCODE_S, //Nub1 down
		KeyEvent.KEYCODE_W, //Nub1 up
		KeyEvent.KEYCODE_6, //Nub2 right
		KeyEvent.KEYCODE_4, //Nub2 left
		KeyEvent.KEYCODE_5, //Nub2 down
		KeyEvent.KEYCODE_8  //Nub2 up
	};
	
	//The max value a nub can report
	private static int ANALOG_NUB_MAX_VALUE = 127;
	//How far the nub must be pressed for it to issue an emulated keypress
	private static int ANALOG_NUB_THRESHOLD = ANALOG_NUB_MAX_VALUE / 2;
	private static int ANALOG_NUB_OFFSET = 128;
	
	
	private int[] m_axes = new int[4];
	private int[] m_dpad = new int[4];
	private boolean[] m_emulatedButtons = new boolean[8];
	
	private int[] m_buttons = new int[16];

	public HIDipega(String address, String sessionId, Context context, boolean startnotification) throws Exception {
		super(address, sessionId, context, startnotification);
		
		super.doConnect();
	}
	
	protected void parseDPad(byte[] data, int offset) {
		int v;
		if((data[offset] & 0xff) != 0x88){
			v = DPAD_MAP[data[offset] & 0xff];
		}else{
			v = 0;
		}
		for(int i = 0; i < 4; i++) {
			if ((v & 1) != m_dpad[i]) {
				m_dpad[i] = (v & 1);
				
				if (D) Log.d(getDriverName(), "dpad " + i + " changed to: " + (m_dpad[i] == 1 ? "down" : "up"));
				

				keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, m_dpad[i] == 1 ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
				keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, DPAD[i]);
				keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, 0);
				keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
				m_context.sendBroadcast(keypressBroadcast);
			}
			v = v >>> 1;
		}
	}
	
	protected void parseDigital(byte A, byte B) {
		int v = (A << 8) | B;
		for(int i = 0; i < 16; i++) {
			if ((v & 1) != m_buttons[i]) {
				m_buttons[i] = (v & 1);
				
				if (D) Log.d(getDriverName(), "Button " + i + " changed to: " + (m_buttons[i] == 1 ? "down" : "up"));
				
				if (KEYS[i] != KEYCODE_UNUSED) {
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, m_buttons[i] == 1 ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, KEYS[i]);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, 0);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
					m_context.sendBroadcast(keypressBroadcast);
				}
			}
			v = v >>> 1;
		}
	}
	
    protected void parseAnalog(byte[] data, int offset) {
		
		for(int i = 0; i < 4; i++) {
			int newvalue = (data[offset + i] & 0xff)-ANALOG_NUB_OFFSET;
			if (m_axes[i] != newvalue) {
				
				boolean up = newvalue >= ANALOG_NUB_THRESHOLD;
				boolean down = newvalue <= -ANALOG_NUB_THRESHOLD;
				
				if (D) Log.d(getDriverName(), "Axis " + i + " changed to: " + newvalue);
				
				m_axes[i] = newvalue;
				directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_DIRECTION, i);
				directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_VALUE, m_axes[i]);
				m_context.sendBroadcast(directionBroadcast);
				
				if (up != m_emulatedButtons[i*2]) {
					m_emulatedButtons[i*2] = up;
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, up ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, ANALOG_KEYS[i*2]);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, 0);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, true);
					m_context.sendBroadcast(keypressBroadcast);
				}
				
				if (down != m_emulatedButtons[(i*2) + 1]) {
					m_emulatedButtons[i*2 + 1] = down;
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, down ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, ANALOG_KEYS[(i*2) + 1]);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, 0);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, true);
					m_context.sendBroadcast(keypressBroadcast);
				}
			}
		}		
	}

	@Override
	protected void handleHIDMessage(byte hidType, byte reportId, byte[] data) throws Exception {
		if (reportId == 0x07) {
			if (data.length < 5) {
				Log.w(LOG_NAME, "Got keypress message with too few bytes: " + getHexString(data, 0, data.length));
			} else {
				//if (D) Log.w(LOG_NAME, "Got keypress message, bytes: " + getHexString(data, 0, data.length));

				if (SHOW_RAW) {
					HIDKeyboard.DumpReport1Data(LOG_NAME, data);
				}
				parseDPad(data, 4);
				parseAnalog(data, 0);
				parseDigital(data[5],data[6]);
				
				
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
			
			results.put((byte)0x7, 8); 
			
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
				FutureKeyCodes.KEYCODE_BUTTON_A, FutureKeyCodes.KEYCODE_BUTTON_B,
				FutureKeyCodes.KEYCODE_BUTTON_X, FutureKeyCodes.KEYCODE_BUTTON_Y,
                FutureKeyCodes.KEYCODE_BUTTON_START, FutureKeyCodes.KEYCODE_BUTTON_SELECT,
                FutureKeyCodes.KEYCODE_BUTTON_L1, FutureKeyCodes.KEYCODE_BUTTON_R1,
                KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_S,
                KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_5
        };
	}

	public static int[] getButtonNames() {
		return new int[] { R.string.ipega_dpad_left, R.string.ipega_dpad_right, R.string.ipega_dpad_up, R.string.ipega_dpad_down,
				R.string.ipega_button_a, R.string.ipega_button_b,
                R.string.ipega_button_x, R.string.ipega_button_y,
				R.string.ipega_button_start, R.string.ipega_button_select,
                R.string.ipega_button_l1, R.string.ipega_button_r1,
                R.string.ipega_left_stick_left, R.string.ipega_left_stick_right, R.string.ipega_left_stick_up, R.string.ipega_left_stick_down,
                R.string.ipega_right_stick_left, R.string.ipega_right_stick_right, R.string.ipega_right_stick_up, R.string.ipega_right_stick_down,
		};
	}
	

}
