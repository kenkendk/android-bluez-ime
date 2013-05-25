package com.hexad.bluezime;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

public class GameStopReader extends RfcommReader {
	
	private static final boolean D = false;
	
	public static final String DRIVER_NAME = "gamestop";
	public static final String DISPLAY_NAME = "GameStop Red Samurai Controller";
	
	private final byte MAGIC_NUMBER = (byte)0xA1;
	private final byte MAGIC_NUMBER_MESSAGE = (byte)0x01;
	private final byte MAGIC_NUMBER_HEADER = (byte)0xfe;
	private final byte MAGIC_NUMBER_BATTERY= (byte)0xff;
	
	private final int MESSAGE_LENGTH = 8;
	private final int HEADER_LENGTH = 18;
	private final int BATTERY_LENGTH = 7;

	//The max value a nub can report
	private static int ANALOG_NUB_MAX_VALUE = 127;
	//How far the nub must be pressed for it to issue an emulated keypress
	private static int ANALOG_NUB_THRESHOLD = ANALOG_NUB_MAX_VALUE / 2;

	//This is the number of directions supported, hardcoded
	private static final int SUPPORTED_DIRECTIONS = 8;
	
	//This is the number of buttons supported, hardcoded
	private static final int SUPPORTED_BUTTONS = 14;
		
	//These three keep track of the last known state of buttons, directions and emulated direction-buttons
	private boolean[] m_buttons = new boolean[SUPPORTED_BUTTONS];
	private int[] m_directions = new int[SUPPORTED_DIRECTIONS / 2];
	private boolean[] m_lastDirectionsKeys = new boolean[SUPPORTED_DIRECTIONS];
	
	//These are buffers that are used to read/parse input data,
	// they are reused to prevent re-allocation and garbage collections.
	//If they have the wrong size, they will be re-allocated once
	private int[] _directionValues = new int[SUPPORTED_DIRECTIONS / 2];
	
	//This is the reason we only support 8 directions (and my device only has 4)
	private static final int[] ANALOG_KEYCODES = new int[] { 
		KeyEvent.KEYCODE_W, //Left knob right 
		KeyEvent.KEYCODE_A, //Left knob left
		KeyEvent.KEYCODE_S, //Left knob down
		KeyEvent.KEYCODE_D,  //Left knob up

		KeyEvent.KEYCODE_6, //Right knob right
		KeyEvent.KEYCODE_4, //Right knob left
		KeyEvent.KEYCODE_5, //Right knob down
		KeyEvent.KEYCODE_8  //Right knob up
	};
	
	//Mapping of reported scan-codes to Android keypress values
	private static final int[] KEYCODE_MAPPINGS = {		
		FutureKeyCodes.KEYCODE_BUTTON_1,  //Button 1
		FutureKeyCodes.KEYCODE_BUTTON_2,  //Button 2
		FutureKeyCodes.KEYCODE_BUTTON_3,  //Button 3
		FutureKeyCodes.KEYCODE_BUTTON_4,  //Button 4

		FutureKeyCodes.KEYCODE_DPAD_UP,    //DPAD Up
		FutureKeyCodes.KEYCODE_DPAD_LEFT,  //DPAD Left
		FutureKeyCodes.KEYCODE_DPAD_DOWN,  //DPAD Down
		FutureKeyCodes.KEYCODE_DPAD_RIGHT, //DPAD Right

		FutureKeyCodes.KEYCODE_BUTTON_SELECT,   //Select
		FutureKeyCodes.KEYCODE_BUTTON_START,    //Start
		FutureKeyCodes.KEYCODE_BUTTON_5, 		//L3
		FutureKeyCodes.KEYCODE_BUTTON_6, 		//R3

		FutureKeyCodes.KEYCODE_BUTTON_L1,   	//L1
		FutureKeyCodes.KEYCODE_BUTTON_L2,   	//R1
		FutureKeyCodes.KEYCODE_BUTTON_R1, 		//L2
		FutureKeyCodes.KEYCODE_BUTTON_R2 		//R2
	};
	
	public GameStopReader(String address, String sessionId, Context context, boolean startnotification) throws Exception {
		super(address, sessionId, context, startnotification);
	}
	
	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}
	
	@Override
	protected int parseInputData(byte[] data, int read) {
	
		int offset = 0;
		int remaining = read;
		
		//This should always be true
		while (remaining > 3 && data[offset + 0] == MAGIC_NUMBER) {
			if (data[offset + 1] == MAGIC_NUMBER_BATTERY) {
				remaining -= BATTERY_LENGTH;
				offset += BATTERY_LENGTH;
			} else if (data[offset + 1] == MAGIC_NUMBER_HEADER) {
				remaining -= HEADER_LENGTH;
				offset += HEADER_LENGTH;
			} else if (data[offset + 1] == MAGIC_NUMBER_MESSAGE) {
				int buttons = ((data[offset + 6] & 0xff) << 8) | (data[offset + 7] & 0xff);
				//For some strange reason, the UP bit is flipped
				int up = (buttons & (1 << 8)) == 0 ? 1 : 0;
				buttons = (buttons & ~(1 << 8)) | (up << 8);
				
				_directionValues[0] = (data[offset + 2] & 0xff) - 0x80;
				_directionValues[1] = (data[offset + 3] & 0xff) - 0x80;
				_directionValues[2] = (data[offset + 4] & 0xff) - 0x80;
				_directionValues[3] = (data[offset + 5] & 0xff) - 0x80;
				
				for(int i = 0; i < m_buttons.length; i++) {
					boolean state =  (buttons & (1 << i)) != 0;
					if (state != m_buttons[i]) {
						m_buttons[i] = state;
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, state ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, KEYCODE_MAPPINGS[i]);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, 0);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
						m_context.sendBroadcast(keypressBroadcast);
					}
				}
				
				for(int i = 0; i < m_directions.length; i++) {
					boolean large = _directionValues[i] > ANALOG_NUB_THRESHOLD;
					boolean small = _directionValues[i] < -ANALOG_NUB_THRESHOLD;
					
					if (large != m_lastDirectionsKeys[i * 2]) {
						m_lastDirectionsKeys[i * 2] = large;
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, large ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, ANALOG_KEYCODES[i]);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, true);
						m_context.sendBroadcast(keypressBroadcast);
					}
					
					if (small != m_lastDirectionsKeys[(i * 2) + 1]) {
						m_lastDirectionsKeys[(i * 2) + 1] = small;
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, small ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, ANALOG_KEYCODES[i]);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, true);
						m_context.sendBroadcast(keypressBroadcast);
					}
					
					if (_directionValues[i] != m_directions[i]) {
						directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_DIRECTION, i);
						directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_VALUE, _directionValues[i]);
						m_context.sendBroadcast(directionBroadcast);
						m_directions[i] = _directionValues[i];
					}
						
				}
				
				remaining -= MESSAGE_LENGTH;
				offset += MESSAGE_LENGTH;
				
			} else {
				//Ditch the rest
				remaining = 0;
			}
		}
		
		return remaining;
	}
	
	@Override
	protected int setupConnection(ImprovedBluetoothDevice device, byte[] readBuffer) throws Exception {
		try {
			//Most devices supports using the reflection method
			if (D) Log.d(getDriverName(), "Attempting reflection connect");
			return super.setupConnection(device, readBuffer);
		} catch (Exception ex) {

			if (D) Log.d(getDriverName(), "Reflection connect failed, error: " + ex.getMessage());

			if (D && ex instanceof InvocationTargetException) {
				InvocationTargetException tex = (InvocationTargetException)ex;
				Log.e(getDriverName(), "TargetInvocation cause: " + (tex.getCause() == null ? "<null>" : tex.getCause().toString()));
				Log.e(getDriverName(), "TargetInvocation target: " + (tex.getTargetException() == null ? "<null>" : tex.getTargetException().toString()));
			}

			try {
				if (D) Log.d(getDriverName(), "Attempting createRfcommSocketToServiceRecord connect");

				//In case the reflection method was not present, we try the correct method
		        m_socket = device.createRfcommSocketToServiceRecord(UUID.fromString("8e1f0cf7-508f-4875-b62c-fbb67fd34812"));
		        m_socket.connect();

		        if (D) Log.d(getDriverName(), "Connected with createRfcommSocketToServiceRecord() to " + m_address);
		    	
		    	m_input = m_socket.getInputStream();
		    	return m_input.read(readBuffer);
		    	
			} catch (Exception ex2) {
				if (D) Log.e(getDriverName(), "Failed on createRfcommSocketToServiceRecord: " + ex2.getMessage());
				
				//Report the original error, not the secondary
				throw ex;
			}
			
		}
	}
	
	@Override
	protected void validateWelcomeMessage(byte[] data, int read) {
		//TODO: Find some documentation that explains how to parse the message
	}

	public static int[] getButtonCodes() {
		return new int[] { 
				FutureKeyCodes.KEYCODE_DPAD_UP,    //DPAD Up
				FutureKeyCodes.KEYCODE_DPAD_LEFT,  //DPAD Left
				FutureKeyCodes.KEYCODE_DPAD_DOWN,  //DPAD Down
				FutureKeyCodes.KEYCODE_DPAD_RIGHT, //DPAD Right

				FutureKeyCodes.KEYCODE_BUTTON_1,  //Button 1
				FutureKeyCodes.KEYCODE_BUTTON_2,  //Button 2
				FutureKeyCodes.KEYCODE_BUTTON_3,  //Button 3
				FutureKeyCodes.KEYCODE_BUTTON_4,  //Button 4

				FutureKeyCodes.KEYCODE_BUTTON_SELECT,   //Select
				FutureKeyCodes.KEYCODE_BUTTON_START,    //Start
				FutureKeyCodes.KEYCODE_BUTTON_5, 		//L3
				FutureKeyCodes.KEYCODE_BUTTON_6, 		//R3

				FutureKeyCodes.KEYCODE_BUTTON_L1,   	//L1
				FutureKeyCodes.KEYCODE_BUTTON_L2,   	//R1
				FutureKeyCodes.KEYCODE_BUTTON_R1, 		//L2
				FutureKeyCodes.KEYCODE_BUTTON_R2, 		//R2

				KeyEvent.KEYCODE_W, //Left knob right 
				KeyEvent.KEYCODE_A, //Left knob left
				KeyEvent.KEYCODE_S, //Left knob down
				KeyEvent.KEYCODE_D,  //Left knob up

				KeyEvent.KEYCODE_6, //Right knob right
				KeyEvent.KEYCODE_4, //Right knob left
				KeyEvent.KEYCODE_5, //Right knob down
				KeyEvent.KEYCODE_8  //Right knob up
		};
	}

	public static int[] getButtonNames() {
		return new int[] { 
				R.string.gamestop_dpad_up, 
				R.string.gamestop_dpad_left, 
				R.string.gamestop_dpad_down, 
				R.string.gamestop_dpad_right, 
				
				R.string.gamestop_button_1, 
				R.string.gamestop_button_2, 
				R.string.gamestop_button_3, 
				R.string.gamestop_button_4, 

				R.string.gamestop_button_start, 
				R.string.gamestop_button_select, 
				R.string.gamestop_button_l3, 
				R.string.gamestop_button_r3, 

				R.string.gamestop_button_l1, 
				R.string.gamestop_button_l2, 
				R.string.gamestop_button_r1, 
				R.string.gamestop_button_r2, 

				R.string.gamestop_leftknob_left, 
				R.string.gamestop_leftknob_right, 
				R.string.gamestop_leftknob_up, 
				R.string.gamestop_leftknob_down, 

				R.string.gamestop_rightknob_left, 
				R.string.gamestop_rightknob_right, 
				R.string.gamestop_rightknob_up, 
				R.string.gamestop_rightknob_down, 
		};
	}
}
