package com.hexad.bluezime;

import android.content.Context;
import android.view.KeyEvent;

public class ZeemoteReader extends BluetoothReader {
	
	//These are from API level 9
	public static final int KEYCODE_BUTTON_A = 0x60;
	public static final int KEYCODE_BUTTON_B = 0x61;
	public static final int KEYCODE_BUTTON_C = 0x62;
	public static final int KEYCODE_BUTTON_X = 0x63;
	
	public static final String DRIVER_NAME = "zeemote";
	public static final String DISPLAY_NAME = "Zeemote JS1";
	
	private final byte BUTTON_UPDATE = 0x07;
	private final byte DIRECTION_UPDATE = 0x08;
	private final byte MAGIC_NUMBER = (byte)0xA1;

	//This is the number of directions supported, hardcoded into this app
	private static final int SUPPORTED_DIRECTIONS = 4;
	
	//These are for buffers and the values correspond to what my device sends
	//If another devices sends something else, the arrays are re-allocated once
	private static final int DEFAULT_BUTTONS_REPORTED = 6;
	private static final int DEFAULT_DIRECTIONS_REPORTED = 2;
	
	//These three keep track of the last known state of buttons, directions and emulated direction-buttons
	private boolean[] m_buttons = new boolean[DEFAULT_BUTTONS_REPORTED];
	private int[] m_directions = new int[SUPPORTED_DIRECTIONS];
	private boolean[] m_lastDirectionsKeys = new boolean[SUPPORTED_DIRECTIONS];
	
	//These are buffers that are used to read/parse input data,
	// they are reused to prevent re-allocation and garbage collections.
	//If they have the wrong size, they will be re-allocated once
	private boolean[] _buttonStates = new boolean[DEFAULT_BUTTONS_REPORTED];
	private int[] _directionValues = new int[DEFAULT_DIRECTIONS_REPORTED];
	private boolean[] _directionStates = new boolean[SUPPORTED_DIRECTIONS];
	
	//This is the reason we only support 4 directions (and my device only has 4)
	private static final int[] m_directionKeyCodes = new int[] { KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP }; 
	
	public ZeemoteReader(String address, Context context) throws Exception {
		super(address, context);
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
		while (remaining > 3 && remaining >= data[offset + 0] + 1 && data[offset + 1] == MAGIC_NUMBER) {
			int consumed = data[offset + 0] + 1;
			remaining -= consumed;
			
			if (data[offset + 2] == BUTTON_UPDATE) {

				//Prevent allocations -> GC
				if (consumed - 3 != _buttonStates.length)
					_buttonStates = new boolean[consumed - 3];
				else
				{
					//If we re-use the array, we must clear it
					for(int i = 0; i < _buttonStates.length; i++)
						_buttonStates[i] = false;
				}
				
				boolean[] buttons = _buttonStates;
				for(int i = 3; i < consumed; i++)
					if (data[offset + i] < buttons.length && data[offset + i] >= 0)
						buttons[data[offset + i]] = true;

				//If we need to reallocate, make sure we maintain the values
				if (m_buttons.length != buttons.length) {
					boolean[] tmp = new boolean[buttons.length];
					for(int i = 0; i < Math.min(m_buttons.length, tmp.length); i++)
						tmp[i] = m_buttons[i];
					
					m_buttons = tmp;
				}
				
				for(int i = 0; i < Math.min(buttons.length, m_buttons.length); i++)
					if (m_buttons[i] != buttons[i])
					{
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, buttons[i] ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, (i < 6 ? KEYCODE_BUTTON_A : KeyEvent.KEYCODE_A) + i);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
						m_context.sendBroadcast(keypressBroadcast);
						m_buttons[i] = buttons[i];
					}
				
			} else if (data[offset + 2] == DIRECTION_UPDATE) {
				
				//Prevent allocations -> GC
				if (consumed - 4 != _directionValues.length)
					_directionValues = new int[consumed - 4];
				
				int[] directions = _directionValues;
				for(int i = 4; i < consumed; i++)
					directions[i - 4] = data[offset + i];

				boolean[] newKeyStates = _directionStates;
				
				//If we need to reallocate, make sure we maintain the values
				if (m_directions.length != directions.length) {
					int[] tmp = new int[newKeyStates.length];
					for(int i = 0; i < Math.min(m_directions.length, tmp.length); i++)
						tmp[i] = m_directions[i];
				
					m_directions = tmp;
				}

				for(int i = 0; i < Math.min(directions.length, m_directions.length); i++)
					if (m_directions[i] != directions[i]) {
						directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_DIRECTION, i);
						directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_VALUE, directions[i]);
						m_context.sendBroadcast(directionBroadcast);
						m_directions[i] = directions[i];
						
						//We only support X/Y axis
						if (i == 0 || i == 1) {
							newKeyStates[(i * 2)] = directions[i] > (128 / 2);
							newKeyStates[(i * 2) + 1] = directions[i] < -(128 / 2);
						}
					}
				
				//Send simulated key presses as well
				for(int i = 0; i < SUPPORTED_DIRECTIONS; i++)
					if (newKeyStates[i] != m_lastDirectionsKeys[i])
					{
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, newKeyStates[i] ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, m_directionKeyCodes[i]);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, true);
						m_context.sendBroadcast(keypressBroadcast);
						m_lastDirectionsKeys[i] = newKeyStates[i];
					}
			}
		}
		
		return remaining;
	}
	
	@Override
	protected void validateWelcomeMessage(byte[] data, int read) {
		//TODO: Find some documentation that explains how to parse the message
	}

	public static int[] getButtonCodes() {
		return new int[] { KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KEYCODE_BUTTON_A, KEYCODE_BUTTON_B, KEYCODE_BUTTON_C, KEYCODE_BUTTON_X };
	}

	public static int[] getButtonNames() {
		return new int[] { R.string.zeemote_axis_left, R.string.zeemote_axis_right, R.string.zeemote_axis_up, R.string.zeemote_axis_down, R.string.zeemote_button_a, R.string.zeemote_button_b, R.string.zeemote_button_c, R.string.zeemote_button_d };
	}

}
