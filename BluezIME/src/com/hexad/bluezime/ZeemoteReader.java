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

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

public class ZeemoteReader extends RfcommReader {
	
	private static final boolean D = false;
	
	public static final String DRIVER_NAME = "zeemote";
	public static final String DISPLAY_NAME = "Zeemote JS1";
	
	private final byte BUTTON_UPDATE = 0x07;
	private final byte BUTTON_UPDATE_STEELSERIES = 0x1c;
	private final byte DIRECTION_UPDATE = 0x08;
	private final byte MAGIC_NUMBER = (byte)0xA1;

	//The max value a nub can report
	private static int ANALOG_NUB_MAX_VALUE = 127;
	//How far the nub must be pressed for it to issue an emulated keypress
	private static int ANALOG_NUB_THRESHOLD = ANALOG_NUB_MAX_VALUE / 2;

	//This is the number of directions supported, hardcoded
	private static final int SUPPORTED_DIRECTIONS = 8;
	
	//This is the number of buttons supported, hardcoded
	private static final int SUPPORTED_BUTTONS = 16;
		
	//These three keep track of the last known state of buttons, directions and emulated direction-buttons
	private boolean[] m_originalButtons = new boolean[SUPPORTED_BUTTONS];
	private boolean[] m_steelseriesButtons = new boolean[SUPPORTED_BUTTONS];
	private int[] m_directions = new int[SUPPORTED_DIRECTIONS / 2];
	private boolean[] m_lastDirectionsKeys = new boolean[SUPPORTED_DIRECTIONS];
	
	//These are buffers that are used to read/parse input data,
	// they are reused to prevent re-allocation and garbage collections.
	//If they have the wrong size, they will be re-allocated once
	private boolean[] _buttonStates = new boolean[SUPPORTED_BUTTONS];
	private int[] _directionValues = new int[SUPPORTED_DIRECTIONS / 2];
	private boolean[] _directionStates = new boolean[SUPPORTED_DIRECTIONS];
	
	//This is the reason we only support 8 directions (and my device only has 4)
	private static final int[] ANALOG_KEYCODES = new int[] { 
		KeyEvent.KEYCODE_DPAD_RIGHT, //Left knob right 
		KeyEvent.KEYCODE_DPAD_LEFT, //Left knob left
		KeyEvent.KEYCODE_DPAD_DOWN, //Left knob down
		KeyEvent.KEYCODE_DPAD_UP,  //Left knob up

		KeyEvent.KEYCODE_6, //Right knob right
		KeyEvent.KEYCODE_4, //Right knob left
		KeyEvent.KEYCODE_5, //Right knob down
		KeyEvent.KEYCODE_8  //Right knob up
	};
	
	//Mapping of reported scan-codes to Android keypress values
	private static final int[] KEYCODE_MAPPINGS = {
		// Keycodes from original Zeemote
		FutureKeyCodes.KEYCODE_BUTTON_A, //0x00 (A) renamed to (1)
		FutureKeyCodes.KEYCODE_BUTTON_B, //0x01 (B) renamed to (2)
		FutureKeyCodes.KEYCODE_BUTTON_C, //0x02 (C) renamed to (3)
		FutureKeyCodes.KEYCODE_BUTTON_X, //0x03 (D) renamed to (4)
		
		//Keycodes from SteelSeries Free
		KeyEvent.KEYCODE_W,	 //0x04 DPAD Up
		KeyEvent.KEYCODE_A,  //0x05 DPAD Down
		KeyEvent.KEYCODE_S,  //0x06 DPAD Left
		KeyEvent.KEYCODE_D, //0x07 DPAD Right

		FutureKeyCodes.KEYCODE_BUTTON_L1,    //0x08 L Trigger
		FutureKeyCodes.KEYCODE_BUTTON_R1,    //0x09 R Trigger
		FutureKeyCodes.KEYCODE_BUTTON_START, //0x0a A Button
		FutureKeyCodes.KEYCODE_BUTTON_SELECT //0x0b B Button
	};
	
	public ZeemoteReader(String address, String sessionId, Context context, boolean startnotification) throws Exception {
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
		while (remaining > 3 && remaining >= data[offset + 0] + 1 && data[offset + 1] == MAGIC_NUMBER) {
			int consumed = data[offset + 0] + 1;
			remaining -= consumed;
			
			if (data[offset + 2] == BUTTON_UPDATE || data[offset + 2] == BUTTON_UPDATE_STEELSERIES) {

				//Clear the values
				for(int i = 0; i < _buttonStates.length; i++)
					_buttonStates[i] = false;
				
				//Mark the pressed buttons
				for(int i = 3; i < consumed; i++)
					if (data[offset + i] < _buttonStates.length && data[offset + i] >= 0)
						_buttonStates[data[offset + i]] = true;

				boolean[] curStates = data[offset + 2] == BUTTON_UPDATE ? m_originalButtons : m_steelseriesButtons;
				
				for(int i = 0; i < curStates.length; i++)
					if (curStates[i] != _buttonStates[i] && i < KEYCODE_MAPPINGS.length && i >= 0)
					{
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, _buttonStates[i] ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, KEYCODE_MAPPINGS[i]);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_MODIFIERS, 0);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
						m_context.sendBroadcast(keypressBroadcast);
						curStates[i] = _buttonStates[i];
					}
				
			} else if (data[offset + 2] == DIRECTION_UPDATE) {
				
				//data[offset + 3] is the index of the analog stick, and we keep two values
				int indexmultiplier = data[offset + 3] * 2;
				
				if (consumed - 4 >= 2)
				{
					//Prevent allocations -> GC
					if ((2 * indexmultiplier) >  _directionValues.length)
						_directionValues = new int[(2 * indexmultiplier)];
					
					int[] directions = _directionValues;
					directions[indexmultiplier + 0] = data[offset + 4];
					directions[indexmultiplier + 1] = data[offset + 5];
	
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
							if (i < SUPPORTED_DIRECTIONS && i >= 0) {
								newKeyStates[(i * 2)] = directions[i] > ANALOG_NUB_THRESHOLD;
								newKeyStates[(i * 2) + 1] = directions[i] < -ANALOG_NUB_THRESHOLD;
							}
						}
					
					//Send simulated key presses as well
					for(int i = 0; i < ANALOG_KEYCODES.length; i++)
						if (newKeyStates[i] != m_lastDirectionsKeys[i])
						{
							keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, newKeyStates[i] ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
							keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, ANALOG_KEYCODES[i]);
							keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, true);
							m_context.sendBroadcast(keypressBroadcast);
							m_lastDirectionsKeys[i] = newKeyStates[i];
						}
				}
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
				KeyEvent.KEYCODE_DPAD_LEFT, 
				KeyEvent.KEYCODE_DPAD_RIGHT, 
				KeyEvent.KEYCODE_DPAD_UP, 
				KeyEvent.KEYCODE_DPAD_DOWN, 
				
				FutureKeyCodes.KEYCODE_BUTTON_A, 
				FutureKeyCodes.KEYCODE_BUTTON_B, 
				FutureKeyCodes.KEYCODE_BUTTON_C, 
				FutureKeyCodes.KEYCODE_BUTTON_X,
				
				FutureKeyCodes.KEYCODE_BUTTON_L1,
				FutureKeyCodes.KEYCODE_BUTTON_R1,
				FutureKeyCodes.KEYCODE_BUTTON_START,
				FutureKeyCodes.KEYCODE_BUTTON_SELECT,

				KeyEvent.KEYCODE_W, 
				KeyEvent.KEYCODE_A, 
				KeyEvent.KEYCODE_S, 
				KeyEvent.KEYCODE_D, 

				KeyEvent.KEYCODE_6, 
				KeyEvent.KEYCODE_4, 
				KeyEvent.KEYCODE_5, 
				KeyEvent.KEYCODE_8 
		};
	}

	public static int[] getButtonNames() {
		return new int[] { 
				R.string.zeemote_axis_left, 
				R.string.zeemote_axis_right, 
				R.string.zeemote_axis_up, 
				R.string.zeemote_axis_down, 
				
				R.string.zeemote_button_a, 
				R.string.zeemote_button_b, 
				R.string.zeemote_button_c, 
				R.string.zeemote_button_d, 

				R.string.zeemote_button_l, 
				R.string.zeemote_button_r, 
				R.string.zeemote_button_newa, 
				R.string.zeemote_button_newb, 

				R.string.zeemote_dpad_up, 
				R.string.zeemote_dpad_left, 
				R.string.zeemote_dpad_down, 
				R.string.zeemote_dpad_right, 

				R.string.zeemote_rightaxis_left, 
				R.string.zeemote_rightaxis_right, 
				R.string.zeemote_rightaxis_up, 
				R.string.zeemote_rightaxis_down, 
		};
	}

}
