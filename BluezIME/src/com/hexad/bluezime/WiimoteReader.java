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

import java.io.IOException;
import java.util.Hashtable;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;

public class WiimoteReader extends HIDReaderBase {

	private static final boolean D = false; //General debug info
	private static final boolean D2 = false; //Detailed (packages) debug info
	private static final boolean D3 = false; //Classic Controller debug info
	
	public static final String DRIVER_NAME = "wiimote";
	public static final String DISPLAY_NAME = "Wiimote (some firmwares)";

	//These are from API level 9
	public static final int KEYCODE_BUTTON_A = 0x60;
	public static final int KEYCODE_BUTTON_B = 0x61;
	public static final int KEYCODE_BUTTON_X = 0x63;
	public static final int KEYCODE_BUTTON_Y = 0x64;
	public static final int KEYCODE_BUTTON_L1 = 0x66;
	public static final int KEYCODE_BUTTON_R1 = 0x67;
	public static final int KEYCODE_BUTTON_L2 = 0x68;
	public static final int KEYCODE_BUTTON_R2 = 0x69;
	public static final int KEYCODE_BUTTON_START = 0x6c; 
	public static final int KEYCODE_BUTTON_SELECT = 0x6d; 
	
	//Value for keys we do not care about
	public static final int KEYCODE_UNUSED = 0x0;

	//Accelerometer axis (x,y,z) is reported as accelerometer (0,1,2)
	private static final int CORE_ACCELEROMETER_AXIS_OFFSET = 0;

	//Nunchuck accelerometer axis (x,y,z) is reported as accelerometer (3,4,5)
	private static final int NUNCHUCK_ACCELEROMETER_AXIS_OFFSET = 3;

	//The max value an analog control can have can report
	private static int ANALOG_MAX_VALUE = 127;
	//How large the analog value must be for it to issue an emulated keypress
	private static int ANALOG_THRESHOLD = ANALOG_MAX_VALUE / 2;
	
	//The number of messages to read before probing for extensions
	private static int EXTENSION_PROBETHRESHOLD = 15;

	//Mappings from bit-index to keycode for core buttons
	private static final int[] CORE_KEYS = new int[] {
		KeyEvent.KEYCODE_2, 			//Byte 1, bit 0
		KeyEvent.KEYCODE_1,				//Byte 1, bit 1
		KeyEvent.KEYCODE_B,				//Byte 1, bit 2
		KeyEvent.KEYCODE_A,				//Byte 1, bit 3
		KeyEvent.KEYCODE_MINUS,			//Byte 1, bit 4
		KEYCODE_UNUSED, 				//Byte 1, bit 5
		KEYCODE_UNUSED, 				//Byte 1, bit 6
		KeyEvent.KEYCODE_H,				//Byte 1, bit 7
		KeyEvent.KEYCODE_DPAD_LEFT, 	//Byte 0, bit 0
		KeyEvent.KEYCODE_DPAD_RIGHT, 	//Byte 0, bit 1
		KeyEvent.KEYCODE_DPAD_DOWN, 	//Byte 0, bit 2
		KeyEvent.KEYCODE_DPAD_UP, 		//Byte 0, bit 3
		KeyEvent.KEYCODE_PLUS, 			//Byte 0, bit 4
		KEYCODE_UNUSED, 				//Byte 0, bit 5
		KEYCODE_UNUSED, 				//Byte 0, bit 6
		KEYCODE_UNUSED, 				//Byte 0, bit 7
	};

	//Mappings from bit-index to keycode for classic keys
	private static final int[] CLASSIC_KEYS = new int[] {
		KEYCODE_UNUSED, 				//Byte 4, bit 0
		KEYCODE_BUTTON_R2,				//Byte 4, bit 1
		KeyEvent.KEYCODE_PLUS,			//Byte 4, bit 2
		KeyEvent.KEYCODE_H,				//Byte 4, bit 3
		KeyEvent.KEYCODE_MINUS,			//Byte 4, bit 4
		KEYCODE_BUTTON_L2, 				//Byte 4, bit 5
		KeyEvent.KEYCODE_DPAD_DOWN, 	//Byte 4, bit 6
		KeyEvent.KEYCODE_DPAD_RIGHT,	//Byte 4, bit 7
		KeyEvent.KEYCODE_DPAD_UP, 		//Byte 5, bit 0
		KeyEvent.KEYCODE_DPAD_LEFT, 	//Byte 5, bit 1
		KEYCODE_BUTTON_R1, 				//Byte 5, bit 2
		KEYCODE_BUTTON_X, 				//Byte 5, bit 3
		KEYCODE_BUTTON_A, 				//Byte 5, bit 4
		KEYCODE_BUTTON_Y, 				//Byte 5, bit 5
		KEYCODE_BUTTON_B, 				//Byte 5, bit 6
		KEYCODE_BUTTON_L1, 				//Byte 5, bit 7
	};
	
	//Mappings for Nunchuck buttons
	private static final int[] NUNCHUCK_KEYS = new int[] {
		KeyEvent.KEYCODE_C,		//C Button
		KeyEvent.KEYCODE_Z,		//Z Button
	};

	//Emulated keypress values for core accelerometer
	private static final int[] CORE_ACCELEROMETER_KEYS = new int[] {
		KeyEvent.KEYCODE_N, 			//Accelerometer X up
		KeyEvent.KEYCODE_M, 			//Accelerometer X down
		KeyEvent.KEYCODE_J,  			//Accelerometer Y up
		KeyEvent.KEYCODE_K,  			//Accelerometer Y down
		KeyEvent.KEYCODE_I,  			//Accelerometer Z up
		KeyEvent.KEYCODE_O,  			//Accelerometer Z down
	};

	//Emulated keypress values for classic thumbsticks
	private static final int[] CLASSIC_ANALOG_KEYS = new int[] {
		KeyEvent.KEYCODE_Q, 		//Classic, Left stick left
		KeyEvent.KEYCODE_D, 		//Classic, Left stick right
		KeyEvent.KEYCODE_W, 		//Classic, Left stick up
		KeyEvent.KEYCODE_S, 		//Classic, Left stick down
		KeyEvent.KEYCODE_4, 		//Classic, Right stick left
		KeyEvent.KEYCODE_6, 		//Classic, Right stick right
		KeyEvent.KEYCODE_8, 		//Classic, Right stick up
		KeyEvent.KEYCODE_5, 		//Classic, Right stick down
		KEYCODE_UNUSED, 			//Classic, L2 up
		KEYCODE_UNUSED, 			//Classic, L2 down
		KEYCODE_UNUSED, 			//Classic, R2 up
		KEYCODE_UNUSED, 			//Classic, R2 down
	};

	//Emulated keypress values for nunchuck accelerometer
	private static final int[] NUNCHUCK_ACCELEROMETER_KEYS = new int[] {
		KeyEvent.KEYCODE_V, 		//Nunchuck, Accelerometer X up
		KeyEvent.KEYCODE_B, 		//Nunchuck, Accelerometer X down
		KeyEvent.KEYCODE_G, 		//Nunchuck, Accelerometer Y up
		KeyEvent.KEYCODE_H, 		//Nunchuck, Accelerometer Y down
		KeyEvent.KEYCODE_Y, 		//Nunchuck, Accelerometer Z up
		KeyEvent.KEYCODE_U, 		//Nunchuck, Accelerometer Z down
	};
	
	//Emulated keypress values for nunchuck thumbsticks
	private static final int[] NUNCHUCK_ANALOG_KEYS = new int[] {	
		KeyEvent.KEYCODE_D, 		//Nunchuck, Thumbstick right
		KeyEvent.KEYCODE_Q, 		//Nunchuck, Thumbstick left
		KeyEvent.KEYCODE_S, 		//Nunchuck, Thumbstick down
		KeyEvent.KEYCODE_W, 		//Nunchuck, Thumbstick up
	};

	//TODO: Figure out if it is a parsing bug that makes the first byte differ
	// for the extension ID or it is safe to ignore the first byte?
	
	//The ID for a Wii Classic Controller
	private static final byte[] CLASSIC_DEVICE_ID = new byte[] {
		0x00, 0x00, (byte)0xA4, 0x20, 0x01, 0x01
	};

	//The ID for a Wii Classic Controller (not documented, but reported)
	private static final byte[] CLASSIC_DEVICE_ID_ALT = new byte[] {
		0x01, 0x00, (byte)0xA4, 0x20, 0x01, 0x01
	};

	//The ID for a Wii Nunchuck Controller
	private static final byte[] NUNCHUCK_DEVICE_ID = new byte[] {
		0x00, 0x00, (byte)0xA4, 0x20, 0x00, 0x00
	};

	//The ID for a Wii Nunchuck Controller (not documented, but reported)
	private static final byte[] NUNCHUCK_DEVICE_ID_ALT = new byte[] {
		(byte)0xFF, 0x00, (byte)0xA4, 0x20, 0x00, 0x00
	};

	//Enumeration for states required for extension initialization
	private static final int EXTENSION_INIT_STATE_NONE = 0;
	private static final int EXTENSION_INIT_STATE_SENT_FIRST = 1;
	private static final int EXTENSION_INIT_STATE_SENT_SECOND = 2;
	private static final int EXTENSION_INIT_STATE_SENT_READ = 3;
	
	//Offsets into the control messages
	private static int EXTENSION_ADR_OFFSET = 5;
	private static int EXTENSION_VAL_OFFSET = 7;
	private static int REPORTMODE_OFFSET = 3;
	private static int SET_LED_OFFSET = 2;
	
	//Command for writing an extension command, kept here to avoid allocations
	private byte[] m_extensionWriteData = new byte[] {
		0x52, 0x16, 0x04, (byte)0xA4, 0x00, 0x00, 0x01, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00
	};

	//Command for reading extension data, kept here to avoid allocations
	private byte[] m_extensionReadData = new byte[] {
		0x52, 0x17, 0x04, (byte)0xA4, 0x00, 0x00, 0x00, 0x00
	};

	//The command used to request status, kept here to avoid allocations
	private final byte[] m_requestStatus = new byte[] {
		0x52, 0x15, 0x00	
	};

	//The command used to request status, kept here to avoid allocations
	private final byte[] m_setLEDStatus = new byte[] {
		0x52, 0x11, 0x00	
	};

	//The command used to set report mode, kept here to avoid allocations
	private byte[] m_setReportMode = new byte[] {
		0x52, 0x12, 0x00, 0x00	
	};

	//A buffer for reading memory data, kept here to avoid allocations
	private byte[] m_memoryDataBuffer = new byte[16];
	
	//A flag that tells if the Wii Classic Controller is connected
	private boolean m_isClassicConnected = false;

	//A flag that tells if the Wii Nunchuck Controller is connected
	private boolean m_isNunchuckConnected = false;

	//A state counter, used to keep track of states in the extension initialization sequence
	private int m_extensionInitState = EXTENSION_INIT_STATE_NONE;
	
	//The state of all core buttons, 1 means down, 0 means up
	private int[] m_coreButtons = new int[16];
	
	//The core accelerometer values for x,y,z
	private int[] m_coreAccelerometerValues = new int[3];

	//State of emulated direction keys for core accelerometer
	private boolean[] m_coreEmulatedAccelerometerButtons = new boolean[m_coreAccelerometerValues.length * 2];

	//The state of all classic controller buttons, 1 means down, 0 means up
	private int[] m_classicButtons = new int[16];

	//The analog values from the classic controller thumbsticks
	private int[] m_classicAnalogValues = new int[6];

	//State of emulated direction keys for classic controller thumbsticks
	private boolean[] m_classicEmulatedButtons = new boolean[m_classicAnalogValues.length * 2];

	//The accelerometer values for x,y,z
	private int[] m_nunchuckAccelerometerValues = new int[3];
	
	//The analog values for the nunchuck thumbstick
	private int[] m_nunchuckAnalogValues = new int[2];
	
	//State of emulated direction keys for nunchuck accelerometer
	private boolean[] m_nunchuckEmulatedAccelerometerButtons = new boolean[m_nunchuckAccelerometerValues.length * 2];

	//State of emulated direction keys for nunchuck thumbstick
	private boolean[] m_nunchuckEmulatedButtons = new boolean[m_nunchuckAnalogValues.length * 2]; 
	
	//The state of the nunchuck buttons
	private boolean[] m_nunchuckButtons = new boolean[2];
	
	//A temporary holder for analog values
	private int[] m_tmpAnalogValues = new int[16];
	
	//A flag that keeps the rumble state
	private boolean m_rumbleOn = false;
	
	//The state of the LEDs
	private byte m_LEDstate = 0x10;
	
	//A flag indicating if the accelerometer is active
	private boolean m_useAccelerometer = false;
	
	//A counter used to probe for extensions
	private int m_probeExtension = 0;
	
	//A flag that indicates an update needs to be performed,
	//This flag reduces the number of times a synchronized method is called
	private volatile boolean m_dirtyUpdateFlag;

	//The variables keep an intent to change, and are processed in synchronized methods
	private boolean m_updateRequestRumble = m_rumbleOn;
	private byte m_updateRequestLEDState = m_LEDstate;
	private boolean m_updateRequestAccelerometer = m_useAccelerometer;
	
	private static class ClassicCalibrationDataAxis {
		public int min;
		public int max;
		public int center;
		
		public void ResetAsLeft() {
			//6 bits = 0 - 63
			min = 0;
			center = 31;
			max = 63;
			
			//min = 5;
			//center = 30;
			//max = 55;
		}

		public void ResetAsRight() {
			//5 bits = 0 - 31
			min = 0;
			center = 15;
			max = 31;
			
			//min = 0;
			//center = 15;
			//max = 30;
		}
		
		public int NormalizedValue(int raw) {
			if (raw == center) {
				return 0;
			} else if (raw < center) {
				return ((byte)(((raw - min) / (float)(center - min)) * 127)) - 127;
			} else {
				return (byte)(((raw - center) / (float)(max - center)) * 127);
			}
		}
	}
	
	private static class ClassicCalibrationDataStick {
		public final ClassicCalibrationDataAxis x = new ClassicCalibrationDataAxis();
		public final ClassicCalibrationDataAxis y = new ClassicCalibrationDataAxis();
		private final boolean m_isLeft;
	
		public ClassicCalibrationDataStick(boolean isLeft) {
			m_isLeft = isLeft;
			Reset();
		}
		
		public void Reset() {
			if (m_isLeft)
				ResetAsLeft();
			else
				ResetAsRight();
		}
		
		public void ResetAsLeft() {
			x.ResetAsLeft();
			y.ResetAsLeft();
		}

		public void ResetAsRight() {
			x.ResetAsRight();
			y.ResetAsRight();
		}
	}
	
	private final ClassicCalibrationDataStick m_classic_calibration_left = new ClassicCalibrationDataStick(true);
	private final ClassicCalibrationDataStick m_classic_calibration_right = new ClassicCalibrationDataStick(false);
	
	//We keep a copy to prevent repeated allocations
	private Hashtable<Byte, Integer> m_reportCodes = null;
	
	@Override
	protected Hashtable<Byte, Integer> getSupportedReportCodes() {
		//TODO: This should be handled by SDP inquiry
		
		if (m_reportCodes == null) {
			Hashtable<Byte, Integer> results = new Hashtable<Byte, Integer>();
			
			results.put((byte)0x20, 6); //Status information 
			results.put((byte)0x21, 21); //Read register data
			results.put((byte)0x22, 4); //Acknowledge report
			
			//Data reports
			results.put((byte)0x30, 2);
			results.put((byte)0x31, 5);
			results.put((byte)0x32, 10);
			results.put((byte)0x33, 17);
			results.put((byte)0x34, 21);
			results.put((byte)0x35, 21);
			results.put((byte)0x36, 21);
			results.put((byte)0x37, 21);
			results.put((byte)0x3d, 21);
			
			m_reportCodes = results;
		}
		
		return m_reportCodes;
	}
	
	public WiimoteReader(String address, String sessionId, Context context, boolean startnotification) throws Exception {
		super(address, sessionId, context, startnotification);

		super.doConnect();
	}

	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}

	@Override
	protected void verifyHIDDevice() throws Exception {
		//When this is called, we are connected, 
		// so we set up the current state
		
		//Set the LEDs to indicate we are now connected
		setLEDs(true, false, false, false);
		
		//Set the report mode
		updateReportMode();
		
		//Request a status report so we detect extensions immediately
		requestStatus();
	}
	
	@Override
	protected void handleHIDMessage(byte hidType, byte reportId, byte[] data) throws Exception {
		if (D2) Log.d(DRIVER_NAME, "Got Wii message " + reportId + ": " + getHexString(data, 0, data.length));
		
		//Core buttons are always present in the status reports
		handleCoreButtons(data[0], data[1]);

		//If the accelerometer is active, we handle it here
		if (m_useAccelerometer) {
			switch(reportId) {
				case (byte)0x31: //Core buttons + accelerometer
				case (byte)0x33: //Core buttons + accelerometer + 12 IR bytes
				case (byte)0x35: //Core buttons + accelerometer + 16 extension bytes
				case (byte)0x37: //Core buttons + accelerometer + 10 IR bytes + 6 extension bytes
					handleAccelerometerData(data[2], data[3], data[4]);
					break;
			}
		}
		
		switch(reportId) {
			case (byte)0x20: //Status report
				handleStatusReport(data[2], data[5]);
				break;
			case (byte)0x21: //Read memory data
				if ((data[2] & 0xf) != 0) {
					if (D) Log.e(DRIVER_NAME, "Failed to read extension type");
					m_extensionInitState = EXTENSION_INIT_STATE_NONE;
				} else {
					byte size = (byte)((byte)((data[2] & 0xff) >>> 4) + 1);
					int dataoffset = (data[3] << 8) | (((int)data[4]) &0xff);
					System.arraycopy(data, 5, m_memoryDataBuffer, 0, size);
					handleExtensionDataRead(dataoffset, size, m_memoryDataBuffer);
				}
				break;
			case (byte)0x22: //Acknowledge report
				handleAcknowledgeReport(data[2], data[3]);
				break;
			case (byte)0x32: //Core buttons + 8 extension bytes
				handleExtensionData(data, 2);
				break;
			case (byte)0x34: //Core buttons + 19 extension bytes
				handleExtensionData(data, 4);
				break;
			case (byte)0x35: //Core buttons + accelerometer + 16 extension bytes
				handleExtensionData(data, 5);
				break;
			case (byte)0x36: //Core buttons + 10 IR bytes + 9 extension bytes
				handleExtensionData(data, 12);
				break;
			case (byte)0x37: //Core buttons + accelerometer + 10 IR bytes + 6 extension bytes
				handleExtensionData(data, 15);
				break;

			case (byte)0x30: //Core buttons
			case (byte)0x31: //Core buttons + accelerometer
			case (byte)0x33: //Core buttons + accelerometer + 12 IR bytes
				//Do not report any unexpected message for these
				break;

			default:
				if (D) Log.w(DRIVER_NAME, "Got unexpected wii message: " + data[1]);
				break;
		}

		//If we have not yet seen an extension, lets force activation
		if (m_probeExtension == EXTENSION_PROBETHRESHOLD && !(m_isClassicConnected || m_isNunchuckConnected)) {
			if (D) Log.i(DRIVER_NAME, "Probing for extensions");
			//These will activate the extension without encryption
			writeExtensionRegister((byte)0xf0, (byte)0x55);
			m_extensionInitState = EXTENSION_INIT_STATE_SENT_FIRST;
		}

		//Count until we hit the threshold
		if (m_probeExtension <= EXTENSION_PROBETHRESHOLD)
			m_probeExtension++;

		//We only call the synchronized method when the flag is set
		//which reduces the number of times we need to obtain the lock
		if (m_dirtyUpdateFlag)
			processUpdateRequest();
	}

	private void handleExtensionDataRead(int offset, byte size, byte[] data) throws IOException {
		
		//Special report, read calibration data from classic controller
		if (offset == 0x0020) {
			//TODO: The Nunchuck can also deliver calibration data
			if ((data[0] & 0xff) != 0xff && data[0] != 0x00 && size >= 12 && m_isClassicConnected) {
				m_classic_calibration_left.x.max = data[0] / 4;
				m_classic_calibration_left.x.min = data[1] / 4;
				m_classic_calibration_left.x.center = data[2] / 4;
				m_classic_calibration_left.y.max = data[3] / 4;
				m_classic_calibration_left.y.min = data[4] / 4;
				m_classic_calibration_left.y.center = data[5] / 4;

				m_classic_calibration_left.x.max = data[6] / 8;
				m_classic_calibration_left.x.min = data[7] / 8;
				m_classic_calibration_left.x.center = data[8] / 8;
				m_classic_calibration_left.y.max = data[9] / 8;
				m_classic_calibration_left.y.min = data[10] / 8;
				m_classic_calibration_left.y.center = data[11] / 8;
			}
		} else	if (offset != 0x00fa || size != CLASSIC_DEVICE_ID.length) {
			Log.e(DRIVER_NAME, "Unexpected data read: " + getHexString(data, 0, size));
		} else if (m_extensionInitState != EXTENSION_INIT_STATE_SENT_READ) {
			Log.e(DRIVER_NAME, "Got extension data but state was: " + m_extensionInitState);			
		} else {
			
			if (D) Log.d(DRIVER_NAME, "Extension device id: " + getHexString(data, 0, size));
			
			m_extensionInitState = EXTENSION_INIT_STATE_NONE;
			boolean classic = true;
			for(int i = 0; i < size; i++)
				classic &= data[i] == CLASSIC_DEVICE_ID[i];

			boolean classic_alt = true;
			for(int i = 0; i < size; i++)
				classic_alt &= data[i] == CLASSIC_DEVICE_ID_ALT[i];

			boolean nunchuck = true;
			for(int i = 0; i < size; i++)
				nunchuck &= data[i] == NUNCHUCK_DEVICE_ID[i];

			boolean nunchuck_alt = true;
			for(int i = 0; i < size; i++)
				nunchuck_alt &= data[i] == NUNCHUCK_DEVICE_ID_ALT[i];
			
			if (classic || classic_alt) {
				if (D) Log.d(DRIVER_NAME, "Wii Classic Controller Extension connected");
				
				//Clear any previous states
				for(int i = 0; i < m_classicButtons.length; i++)
					m_classicButtons[i] = 0;
				for(int i = 0; i < m_classicAnalogValues.length; i++)
					m_classicAnalogValues[i] = 0;
				for(int i = 0; i < m_classicEmulatedButtons.length; i++)
					m_classicEmulatedButtons[i] = false;
				
				m_isClassicConnected = true;
				m_classic_calibration_left.Reset();
				m_classic_calibration_right.Reset();
				
				//Ask for calibration data, if we do not get it, we just use the default data
				readExtensionRegisters((byte)0x20, (byte)16);
				
				updateReportMode();
			} else if (nunchuck | nunchuck_alt) {
				if (D) Log.d(DRIVER_NAME, "Wii Nunchuck Controller Extension connected");
				
				//Clear any previous states
				for(int i = 0; i < m_nunchuckButtons.length; i++)
					m_nunchuckButtons[i] = false;
				for(int i = 0; i < m_nunchuckAccelerometerValues.length; i++)
					m_nunchuckAccelerometerValues[i] = 0;
				for(int i = 0; i < m_nunchuckAnalogValues.length; i++)
					m_nunchuckAnalogValues[i] = 0;
				for(int i = 0; i < m_nunchuckEmulatedAccelerometerButtons.length; i++)
					m_nunchuckEmulatedAccelerometerButtons[i] = false;
				for(int i = 0; i < m_nunchuckEmulatedButtons.length; i++)
					m_nunchuckEmulatedButtons[i] = false;

				m_isNunchuckConnected = true;
				updateReportMode();
			} else {
				Log.d(DRIVER_NAME, "Unknown extension device id: " + getHexString(data, 0, size));
			}
		}
	}
	
	//Method that provides a thread-safe way of updating the LED state
	public synchronized void request_SetLEDState(boolean l1, boolean l2, boolean l3, boolean l4) {
		//TODO: The request is not detected until the next 
		//report is sent from the wiimote, which may cause a large delay
		
		m_dirtyUpdateFlag = true;
		m_updateRequestLEDState = (byte)
			((l1 ? 0x10 : 0x00) |
			(l2 ? 0x20 : 0x00) |
			(l3 ? 0x40 : 0x00) |
			(l4 ? 0x80 : 0x00));
	}

	//Method that provides a thread-safe way of setting the rumble state
	public synchronized void request_SetRumble(boolean active) {
		//TODO: The request is not detected until the next 
		//report is sent from the wiimote, which may cause a large delay

		m_dirtyUpdateFlag = true;
		m_updateRequestRumble = active;
	}
	
	//Method that provides a thread-safe way of setting the accelerometer state
	public synchronized void request_UseAccelerometer(boolean active) {
		//TODO: The request is not detected until the next 
		//report is sent from the wiimote, which may cause a large delay

		m_dirtyUpdateFlag = true;
		m_updateRequestAccelerometer = active;
	}

	//This method is called when the volatile update flag is set, and processes all pending updates
	private synchronized void processUpdateRequest() throws Exception {
		m_dirtyUpdateFlag = false;
		
		if (m_LEDstate != m_updateRequestLEDState || m_rumbleOn != m_updateRequestRumble) {
			m_LEDstate = m_updateRequestLEDState;
			m_rumbleOn = m_updateRequestRumble;
			
			updateLEDStates();
		}
		
		if (m_useAccelerometer != m_updateRequestAccelerometer) {
			m_useAccelerometer = m_updateRequestAccelerometer;
			
			setAccelerometerActive(m_useAccelerometer);
		}
	}
	
	private void setLEDs(boolean l1, boolean l2, boolean l3, boolean l4) throws Exception {
		m_LEDstate = (byte)
			((l1 ? 0x10 : 0x00) |
			(l2 ? 0x20 : 0x00) |
			(l3 ? 0x40 : 0x00) |
			(l4 ? 0x80 : 0x00));
		
		updateLEDStates();
	}
	
	private void setAccelerometerActive(boolean active) throws IOException {
		if (active != m_useAccelerometer) {
			m_useAccelerometer = active;
			
			if (m_useAccelerometer) {
				
				//Reset data
				for(int i = 0; i < m_coreAccelerometerValues.length; i++)
					m_coreAccelerometerValues[i] = 0;
				for(int i = 0; i < m_coreEmulatedAccelerometerButtons.length; i++)
					m_coreEmulatedAccelerometerButtons[i] = false;
				
				if (m_isNunchuckConnected) {
					for(int i = 0; i < m_nunchuckEmulatedAccelerometerButtons.length; i++)
						m_nunchuckEmulatedAccelerometerButtons[i] = false;
					for(int i = 0; i < m_nunchuckAccelerometerValues.length; i++)
						m_nunchuckAccelerometerValues[i] = 0;
				}
			}
			
			updateReportMode();
		}
	}
	
	@SuppressWarnings("unused")
	private void setRumble(boolean rumbleOn) throws Exception {
		m_rumbleOn = rumbleOn;
		
		updateLEDStates();
	}

	private void updateLEDStates() throws IOException {
		m_setLEDStatus[SET_LED_OFFSET] = (byte)((m_LEDstate & 0xf0) | (m_rumbleOn ? 0x01 : 0x00));
		m_control.write(m_setLEDStatus);
		m_control.flush();
	}

	
	private void requestStatus() throws IOException {
		m_control.write(m_requestStatus);
		m_control.flush();
	}
	
	private void updateReportMode() throws IOException {
		byte mode;
		
		if (m_isClassicConnected || m_isNunchuckConnected)   
			mode = (byte)(m_useAccelerometer ? 0x35 : 0x32);
		else
			mode = (byte)(m_useAccelerometer ? 0x31 : 0x30);

		
		m_setReportMode[REPORTMODE_OFFSET] = mode;
		m_control.write(m_setReportMode);
		m_control.flush();
	}

	private void writeExtensionRegister(byte address, byte value) throws IOException {
		m_extensionWriteData[EXTENSION_ADR_OFFSET] = address;
		m_extensionWriteData[EXTENSION_VAL_OFFSET] = value;
		
		m_control.write(m_extensionWriteData);
		m_control.flush();
	}
	
	private void readExtensionRegisters(byte address, byte size) throws IOException {
		m_extensionReadData[EXTENSION_ADR_OFFSET] = address;
		m_extensionReadData[EXTENSION_VAL_OFFSET] = size;
		
		m_control.write(m_extensionReadData);
		m_control.flush();
	}
	
	private void handleAcknowledgeReport(byte reportId, byte result) throws IOException {
		
		if (reportId == 0x16) {
			
			if (result != 0x0) {
				Log.w(DRIVER_NAME, "Got NACK for write register report");
			}
			
			switch (m_extensionInitState) {
				case EXTENSION_INIT_STATE_SENT_FIRST:
					//Confirm extension initialization
					writeExtensionRegister((byte)0xfb, (byte)0x00);
					m_extensionInitState = EXTENSION_INIT_STATE_SENT_SECOND;
					break;
				case EXTENSION_INIT_STATE_SENT_SECOND:
					//This will read the extension device ID
					readExtensionRegisters((byte)0xfa, (byte)0x6);
					m_extensionInitState = EXTENSION_INIT_STATE_SENT_READ;
					break;
			}
		}
	}

	private void handleStatusReport(byte state, byte voltage) throws IOException {
		//boolean batteryNearlyDone  = (state & 0x1) != 0;
		boolean extensionConnected = (state & 0x2) != 0;
		//boolean speakerEnabled     = (state & 0x4) != 0;
		//boolean irEnabled          = (state & 0x8) != 0;

		//If we get this message, there is no need to probe
		if (extensionConnected)
			m_probeExtension = EXTENSION_PROBETHRESHOLD + 1;
		
		//If we have an extension connection change, examine it
		if (extensionConnected != (m_isClassicConnected || m_isNunchuckConnected)) {

			//If a new extension is connected, examine it to see what type it is
			if (extensionConnected) {
				if (D && m_extensionInitState != EXTENSION_INIT_STATE_NONE)
					Log.e(DRIVER_NAME, "Started extension init seqence but state was: " + m_extensionInitState);
				
				//These will activate the extension without encryption
				writeExtensionRegister((byte)0xf0, (byte)0x55);
				m_extensionInitState = EXTENSION_INIT_STATE_SENT_FIRST;
			} else {
				if (D) Log.d(DRIVER_NAME, "Extension disconnected");
				
				m_isClassicConnected = false;
				m_isNunchuckConnected = false;
				
				//Disable extension data
				updateReportMode();
			}
		}
	}

	private void handleDigitalButtons(int bitmask, int[] states, int[] keys) {
		for(int i = 0; i < 16; i++) {
			if ((bitmask & 1) != states[i]) {
				states[i] = (bitmask & 1);
				
				if (keys[i] != KEYCODE_UNUSED) {
					if (D || (D3 && keys == CLASSIC_KEYS)) Log.d(getDriverName(), "Button " + i + " changed to: " + (states[i] == 1 ? "down" : "up"));

					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, states[i] == 1 ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, keys[i]);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
					m_context.sendBroadcast(keypressBroadcast);
				}
			}
			bitmask = bitmask >>> 1;
		}	
	}
	
	private void handleAnalogValues(int[] newValues, int[] prev, boolean[] buttonstates, int[] keys, int report_axis_offset, boolean isAccelerometer) {
		
		for(int i = 0; i < prev.length; i++) {
			//Make sure the values are always in range
			newValues[i] = Math.max(-127, Math.min(127, newValues[i]));
			
			if (prev[i] != newValues[i]) {
				
				if (D && Math.abs(prev[i] - newValues[i]) > 10) Log.d(getDriverName(), "Axis " + (i + report_axis_offset) + " changed from " + prev[i] + " to: " + newValues[i]);
				
				boolean up = newValues[i] >= ANALOG_THRESHOLD;
				boolean down = newValues[i] <= -ANALOG_THRESHOLD;
				
				prev[i] = newValues[i];
				
				if (isAccelerometer) {
					accelerometerBroadcast.putExtra(BluezService.EVENT_ACCELEROMETERCHANGE_AXIS, i + report_axis_offset);
					accelerometerBroadcast.putExtra(BluezService.EVENT_ACCELEROMETERCHANGE_VALUE, prev[i]);
					m_context.sendBroadcast(accelerometerBroadcast);
				} else {
					directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_DIRECTION, i + report_axis_offset);
					directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_VALUE, prev[i]);
					m_context.sendBroadcast(directionBroadcast);
				}
				
				if (up != buttonstates[i*2]) {
					buttonstates[i*2] = up;
					if (keys[i*2] != KEYCODE_UNUSED) {
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, up ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, keys[i*2]);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, true);
						m_context.sendBroadcast(keypressBroadcast);
					}
				}
				
				if (down != buttonstates[(i*2) + 1]) {
					buttonstates[i*2 + 1] = down;
					if (keys[i*2 + 1] != KEYCODE_UNUSED) {
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, down ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, keys[(i*2) + 1]);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, true);
						m_context.sendBroadcast(keypressBroadcast);
					}
				}
			}
		}				
	}
	
	private void handleCoreButtons(int a, int b) {
		a = a & 0xff;
		b = b & 0xff;
		
		handleDigitalButtons((a << 8) | b, m_coreButtons, CORE_KEYS);
	}

	private void handleAccelerometerData(byte x, byte y, byte z) {

		//Fix the lame Java signed byte representation
		int raw_x = ((int)x) & 0xff;
		int raw_y = ((int)y) & 0xff;
		int raw_z = ((int)z) & 0xff;
		
		//Assign the values and normalize them from 
		// [0 -> 0x80 -> 0xff] to [-127, 0, +127] 
		m_tmpAnalogValues[0] = raw_x - 0x80; 
		m_tmpAnalogValues[1] = raw_y - 0x80;
		m_tmpAnalogValues[2] = raw_z - 0x80;
		
		handleAnalogValues(m_tmpAnalogValues, m_coreAccelerometerValues, m_coreEmulatedAccelerometerButtons, CORE_ACCELEROMETER_KEYS, CORE_ACCELEROMETER_AXIS_OFFSET, true);
	}

	private void handleExtensionData(byte[] data, int offset) {

		if (m_isClassicConnected) {

			if (D3) Log.d(DRIVER_NAME, "Got Classic data: " + getHexString(data, offset, offset + 6));
			
			//These report inverse of the core buttons, 0=pressed, 1=unpressed
			int byteA = (~data[offset + 4]) & 0xff;
			int byteB = (~data[offset + 5]) & 0xff;
			
			handleDigitalButtons((byteB << 8) | byteA, m_classicButtons, CLASSIC_KEYS);
			
			m_tmpAnalogValues[0] = data[offset] & 0x3f; //Left X
			m_tmpAnalogValues[1] = data[offset + 1] & 0x3f; //Left Y
	
			m_tmpAnalogValues[2] = (((data[offset + 2] >>> 7) & 0x1) | ((data[offset + 1] >>> 5) & 0x6) | ((data[offset] >>> 3) & 0x18)) & 0x1f; //Right X
			m_tmpAnalogValues[3] = data[offset + 2] & 0x1f; //Right Y
	
			m_tmpAnalogValues[4] = (((data[offset + 3] >>> 5) & 0x7) | ((data[offset + 2] >>> 2) & 0x18)) & 0x1f; //Left trigger
			m_tmpAnalogValues[5] = data[offset + 3] & 0x1f; //Right trigger
			
			if (D3) Log.d(DRIVER_NAME, "Raw classic analog values: " 
					+ m_tmpAnalogValues[0] + ", " 
					+ m_tmpAnalogValues[1] + ", "
					+ m_tmpAnalogValues[2] + ", "
					+ m_tmpAnalogValues[3] + ", "
					+ m_tmpAnalogValues[4] + ", "
					+ m_tmpAnalogValues[5] + ", ");

			//We scale up the values so they are all in the -127/+127 range
			// if we have some calibration data, we also apply that
			m_tmpAnalogValues[0] = m_classic_calibration_left.x.NormalizedValue(m_tmpAnalogValues[0]);
			m_tmpAnalogValues[1] = m_classic_calibration_left.y.NormalizedValue(m_tmpAnalogValues[1]);
			m_tmpAnalogValues[2] = m_classic_calibration_right.x.NormalizedValue(m_tmpAnalogValues[2]);
			m_tmpAnalogValues[3] = m_classic_calibration_right.y.NormalizedValue(m_tmpAnalogValues[3]);
						
			//The L/R triggers have 5 bits, but only reports in 0-127,
			// as you can only push it in one direction
			m_tmpAnalogValues[4] = ((byte)(m_tmpAnalogValues[4] << 2));
			m_tmpAnalogValues[5] = ((byte)(m_tmpAnalogValues[5] << 2));
	
			//Invert the Y axis
			m_tmpAnalogValues[1] *= -1;
			m_tmpAnalogValues[3] *= -1;
			
			if (D3) Log.d(DRIVER_NAME, "Normalized classic values: " 
					+ m_tmpAnalogValues[0] + ", " 
					+ m_tmpAnalogValues[1] + ", "
					+ m_tmpAnalogValues[2] + ", "
					+ m_tmpAnalogValues[3] + ", "
					+ m_tmpAnalogValues[4] + ", "
					+ m_tmpAnalogValues[5] + ", ");
						
			handleAnalogValues(m_tmpAnalogValues, m_classicAnalogValues, m_classicEmulatedButtons, CLASSIC_ANALOG_KEYS, 0, false);
		
		} else if (m_isNunchuckConnected) {
			
			//The buttons report 0 for pressed
			boolean isCPressed = (data[offset + 5] & 0x2) == 0;
			boolean isZPressed = (data[offset + 5] & 0x1) == 0;
			if (m_nunchuckButtons[0] != isCPressed) {
				m_nunchuckButtons[0] = isCPressed;
				if (NUNCHUCK_KEYS[0] != KEYCODE_UNUSED) {
					if (D) Log.d(getDriverName(), "Button C changed to: " + (isCPressed ? "down" : "up"));

					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, isCPressed ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, NUNCHUCK_KEYS[0]);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
					m_context.sendBroadcast(keypressBroadcast);
				}
			}

			if (m_nunchuckButtons[1] != isZPressed) {
				m_nunchuckButtons[1] = isZPressed;
				if (NUNCHUCK_KEYS[1] != KEYCODE_UNUSED) {
					if (D) Log.d(getDriverName(), "Button Z changed to: " + (isZPressed ? "down" : "up"));

					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, isZPressed ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, NUNCHUCK_KEYS[1]);
					keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ANALOG_EMULATED, false);
					m_context.sendBroadcast(keypressBroadcast);
				}
			}

			if (m_useAccelerometer) {
				m_tmpAnalogValues[0] = ((((int)data[offset + 2] & 0xff) << 2) | ((((int)data[offset + 5] & 0xff) >>> 6) & 0x3)) & 0x3ff; //Accelerometer X 
				m_tmpAnalogValues[1] = ((((int)data[offset + 3] & 0xff) << 2) | ((((int)data[offset + 5] & 0xff) >>> 4) & 0x3)) & 0x3ff; //Accelerometer Y 
				m_tmpAnalogValues[2] = ((((int)data[offset + 4] & 0xff) << 2) | ((((int)data[offset + 5] & 0xff) >>> 2) & 0x3)) & 0x3ff; //Accelerometer Z
	
				//We scale the values so they are all in the -127/+127 range
				m_tmpAnalogValues[0] = (m_tmpAnalogValues[0] >>> 2) - 0x80; 
				m_tmpAnalogValues[1] = (m_tmpAnalogValues[1] >>> 2) - 0x80; 
				m_tmpAnalogValues[2] = (m_tmpAnalogValues[2] >>> 2) - 0x80;

				handleAnalogValues(m_tmpAnalogValues, m_nunchuckAccelerometerValues, m_nunchuckEmulatedAccelerometerButtons, NUNCHUCK_ACCELEROMETER_KEYS, NUNCHUCK_ACCELEROMETER_AXIS_OFFSET, true);
			}
			
			m_tmpAnalogValues[0] = (((int)data[offset]) & 0xff); //Thumbstick up/down
			m_tmpAnalogValues[1] = (((int)data[offset + 1]) & 0xff); //Thumbstick left/right
			
			//We scale the values so they are all in the -127/+127 range
			m_tmpAnalogValues[0] = (int)((m_tmpAnalogValues[0] - 0x78) * 1.27);
			m_tmpAnalogValues[1] = (int)((m_tmpAnalogValues[1] - 0x81) * 1.3);
			
			//Bugfix, invert the Y-axis values:
			m_tmpAnalogValues[1] *= -1;
			
			handleAnalogValues(m_tmpAnalogValues, m_nunchuckAnalogValues, m_nunchuckEmulatedButtons, NUNCHUCK_ANALOG_KEYS, 0, false);
		}
	}
	
	public static int[] getButtonCodes() {
		return new int[] { 
				//Core/regular buttons
				KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, 
				KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, 
				KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_H,
				
				//Classic buttons
				KEYCODE_BUTTON_A, KEYCODE_BUTTON_B, KEYCODE_BUTTON_X, KEYCODE_BUTTON_Y,
				KEYCODE_BUTTON_L1, KEYCODE_BUTTON_R1, KEYCODE_BUTTON_L2, KEYCODE_BUTTON_R2,
				
				//Nunchuck/classic thumbstick keys
				KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D,
				KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6,
				
				//Nunchuck buttons
				KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_Z,
				
				//Core accelerometer keys
				//KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_M,
				//KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K,
				//KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_O,
				
				//Nunchuck accelerometer keys
				//KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_B,
				//KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H,
				//KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_U,
		};
	}
	
	public static int[] getButtonNames() {
		return new int[] {
				//Core/regular buttons
				R.string.wiimote_dpad_left, R.string.wiimote_dpad_right, R.string.wiimote_dpad_up, R.string.wiimote_dpad_down,  
				R.string.wiimote_core_a, R.string.wiimote_core_b, R.string.wiimote_core_1, R.string.wiimote_core_2,
				R.string.wiimote_plus, R.string.wiimote_minus, R.string.wiimote_home,  
				
				//Classic buttons
				R.string.wiimote_classic_a, R.string.wiimote_classic_b, R.string.wiimote_classic_x, R.string.wiimote_classic_y,
				R.string.wiimote_classic_l1, R.string.wiimote_classic_r1, R.string.wiimote_classic_l2, R.string.wiimote_classic_r2,
				
				//Nunchuck/classic thumbstick keys
				R.string.wiimote_thumb1_up, R.string.wiimote_thumb1_left, R.string.wiimote_thumb1_down, R.string.wiimote_thumb1_right,
				R.string.wiimote_thumb2_up, R.string.wiimote_thumb2_left, R.string.wiimote_thumb2_down, R.string.wiimote_thumb2_right,
				
				//Nunchuck buttons
				R.string.wiimote_nunchuck_c, R.string.wiimote_nunchuck_z,

		};
	}
	
}
