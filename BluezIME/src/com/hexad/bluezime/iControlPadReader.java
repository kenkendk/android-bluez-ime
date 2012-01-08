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

import java.io.OutputStream;
import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

public class iControlPadReader extends RfcommReader {

	private static final boolean D = false;
	
	public static final String DRIVER_NAME = "icp";
	public static final String DISPLAY_NAME = "iControlPad";

	//These are from API level 9
	public static final int KEYCODE_BUTTON_A = 0x60;
	public static final int KEYCODE_BUTTON_B = 0x61;
	public static final int KEYCODE_BUTTON_X = 0x63;
	public static final int KEYCODE_BUTTON_Y = 0x64;
	public static final int KEYCODE_BUTTON_L1 = 0x66;
	public static final int KEYCODE_BUTTON_R1 = 0x67;
	public static final int KEYCODE_BUTTON_START = 0x6c; 
	public static final int KEYCODE_BUTTON_SELECT = 0x6d; 

	//Value for keys we do not care about
	public static final int KEYCODE_UNUSED = 0x0;

	//Unused test/sample commands
	//private static final byte[] FORCE_LED_MODE_ON = new byte[] {(byte)0x6D, 0x01};
	//private static final byte[] FORCE_LED_MODE_OFF = new byte[] {(byte)0x6D, 0x00};
	//private static final byte[] SET_LED_MODE_LOW_BATT = new byte[] {(byte)0xE4, 0x4};
	//private static final byte[] SET_LED_MODE_PULSE = new byte[] {(byte)0xE4, 0x5};
	//private static final byte[] POLL_FOR_DATA = new byte[] {(byte)0xA5};
	//private static final byte[] GET_BATTERY_LEVEL = new byte[] {(byte)0x55};

	//Commands that we can send to the controller
	private static final byte[] SET_AUTO_MODE_ON = new byte[] {(byte)0xAD, 0x01};
	private static final byte[] GET_DEVICE_ID = new byte[] {(byte)0x39};
	
	//The max value a nub can report
	private static int ANALOG_NUB_MAX_VALUE = 127;
	//How far the nub must be pressed for it to issue an emulated keypress
	private static int ANALOG_NUB_THRESHOLD = ANALOG_NUB_MAX_VALUE / 2;
	
	//Mappings from bit-index to keycode
	private static final int[] KEYS = new int[] {
		KEYCODE_BUTTON_SELECT, 			//Byte B, bit 0
		KEYCODE_BUTTON_START,			//Byte B, bit 1
		KEYCODE_BUTTON_Y, 				//Byte B, bit 2
		KEYCODE_BUTTON_A, 				//Byte B, bit 3
		KEYCODE_BUTTON_X, 				//Byte B, bit 4
		KEYCODE_BUTTON_B, 				//Byte B, bit 5
		KEYCODE_BUTTON_R1, 				//Byte B, bit 6
		KEYCODE_UNUSED, 				//Byte B, bit 7
		KeyEvent.KEYCODE_DPAD_UP, 		//Byte A, bit 0
		KeyEvent.KEYCODE_DPAD_RIGHT, 	//Byte A, bit 1
		KeyEvent.KEYCODE_DPAD_LEFT, 	//Byte A, bit 2
		KeyEvent.KEYCODE_DPAD_DOWN, 	//Byte A, bit 3
		KEYCODE_BUTTON_L1, 				//Byte A, bit 4
		KEYCODE_UNUSED, 				//Byte A, bit 5
		KEYCODE_UNUSED, 				//Byte A, bit 6
		KEYCODE_UNUSED, 				//Byte A, bit 7
	};
	
	//Nubs are emulated with keys (up,left,down,right) = (w,a,s,d) and (8,4,5,6) respectively
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
	
	//Reference to the output stream, not used but can be used to send extra control instructions
	protected OutputStream m_outStream;
	
	protected Thread m_pollThread;
	
	//The current analog nub values
	private int[] m_axes = new int[4];
	//The state of buttons that are emulated by nubs, true means pressed
	private boolean[] m_emulatedButtons = new boolean[8];
	//The state of buttons, 1 = pressed, 0 = unpressed
	private int[] m_buttons = new int[16];
	
	public iControlPadReader(String address, String sessionId, Context context, boolean startnotification) throws Exception {
		super(address, sessionId, context, startnotification);
	}

	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}

	protected void parseAnalog(byte[] data, int offset) {
		
		for(int i = 0; i < 4; i++) {
			int newvalue = data[offset + i];
			if (m_axes[i] != newvalue) {
				
				if (D) Log.d(getDriverName(), "Axis " + i + " changed to: " + newvalue);
				
				boolean up = newvalue >= ANALOG_NUB_THRESHOLD;
				boolean down = newvalue <= -ANALOG_NUB_THRESHOLD;
				
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
	
	@Override
	protected int parseInputData(byte[] data, int read) {

		int offset = 0;
		int remaining = read;
		
		while(remaining >= 6) {
			
			if (D) Log.d(getDriverName(), "Got seq: " + getHexString(data, offset, 6));

			parseAnalog(data, offset);
			parseDigital(data[offset + 4], data[offset + 5]);
			
			offset += 6;
			remaining -= 6;
		}
		
		return remaining;
	}

	@Override
	protected void validateWelcomeMessage(byte[] data, int read) {
		String version = new String(data, 0, read);
		
		if (D) Log.i(getDriverName(), "Connected to version: " + version);
	}
	
	@Override
	protected int setupConnection(ImprovedBluetoothDevice device, byte[] readBuffer) throws Exception {
        m_socket = device.createInsecureRfcommSocket(1);
        m_socket.connect();

        if (D) Log.d(getDriverName(), "Connected to " + m_address);
    	
        m_outStream = m_socket.getOutputStream();
    	m_input = m_socket.getInputStream();
    	
        m_outStream.write(GET_DEVICE_ID);
        m_outStream.flush();
    	int count = 0; 
    	int maxRetries = 5;
		
    	while(maxRetries-- > 0) {
	    	count += m_input.read(readBuffer, count, readBuffer.length - count);
	    	for(int i = 0; i < count; i++)
	    		if (readBuffer[i] == 0x0A) { //LF terminates the string

	    			//Start reporting 
	    			m_outStream.write(SET_AUTO_MODE_ON);
	    			m_outStream.flush();

	    			if (m_input.read() != 0x80)
	    				throw new Exception("Failed to set auto-report mode");
	    			
	    			return count;
	    		}
	    	
	    	Thread.sleep(500);
    	}
    	
    	Log.e(getDriverName(), "Header data read: " + getHexString(readBuffer, 0, count));
    	
    	throw new Exception("Failed to read device id");
	}

	public static int[] getButtonCodes() {
		return new int[] { 
				KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, 
				KEYCODE_BUTTON_A, KEYCODE_BUTTON_B, KEYCODE_BUTTON_X, KEYCODE_BUTTON_Y, 
				KEYCODE_BUTTON_L1, KEYCODE_BUTTON_R1, KEYCODE_BUTTON_START, KEYCODE_BUTTON_SELECT,
				KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D,
				KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6
		};
	}

	public static int[] getButtonNames() {
		return new int[] { 
				R.string.icp_button_left, R.string.icp_button_right, R.string.icp_button_up, R.string.icp_button_down, 
				R.string.icp_button_a, R.string.icp_button_b, R.string.icp_button_x, R.string.icp_button_y,
				R.string.icp_button_l, R.string.icp_button_r, R.string.icp_button_start, R.string.icp_button_select,
				R.string.icp_leftnub_up, R.string.icp_leftnub_left, R.string.icp_leftnub_down, R.string.icp_leftnub_right,
				R.string.icp_rightnub_up, R.string.icp_rightnub_left, R.string.icp_rightnub_down, R.string.icp_rightnub_right
		};
	}

}
