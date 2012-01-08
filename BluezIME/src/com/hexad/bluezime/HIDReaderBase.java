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
import java.io.OutputStream;
import java.util.Hashtable;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

public abstract class HIDReaderBase extends RfcommReader {

	private static final boolean D = true;
	private static final String LOG_NAME = "HID Reader - ";
	
	private static final int HIDP_CONTROL_CHANNEL = 0x11; 
	private static final int HIDP_INTERRUPT_CHANNEL = 0x13; 
	
	protected BluetoothSocket m_controlSocket;
	protected OutputStream m_control;

	//A list of buffers, used to send HID reports
	protected Hashtable<Integer, byte[]> m_readBuffers;
	
	public HIDReaderBase(String address, String sessionId, Context context, boolean startnotification) throws Exception {
		super(address, sessionId, context, false, startnotification);
		
		m_readBuffers = new Hashtable<Integer, byte[]>();
	}

	@Override
	protected int setupConnection(ImprovedBluetoothDevice device, byte[] readBuffer) throws Exception {
		
		boolean isSecure = false;
		
		if (!m_useInsecureChannel) {
			try {
				m_controlSocket = device.createLCAPSocket(HIDP_CONTROL_CHANNEL);
				m_controlSocket.connect();
				isSecure = true;
			} catch (Exception ex) {
				m_controlSocket = null;
			}
		}
		
		if (m_controlSocket == null) {
			m_controlSocket = device.createInsecureLCAPSocket(HIDP_CONTROL_CHANNEL);
			m_controlSocket.connect();
		}
		
		m_control = m_controlSocket.getOutputStream();
		
        m_socket = isSecure ? device.createLCAPSocket(HIDP_INTERRUPT_CHANNEL) : device.createInsecureLCAPSocket(HIDP_INTERRUPT_CHANNEL);
        m_socket.connect();

        if (D) Log.d(LOG_NAME, "Connected to " + m_address);
    	
    	m_input = m_socket.getInputStream();
    	
    	verifyHIDDevice();
    	
    	return 0;		
	}
	
	protected void verifyHIDDevice() throws Exception {
	}
	
	@Override
	protected int parseInputData(byte[] data, int read) {
		
		int offset = 0;
		int remaining = read;

		Hashtable<Byte, Integer> supportedReports = getSupportedReportCodes();

		if (remaining <= 2)
			return remaining;

		//If we get a HID A or C data package, process it
		if (data[offset] == (byte)0xa1 || data[offset] == (byte)0xb1) {
			
			if (supportedReports.containsKey(data[offset + 1]))
			{
				int neededBytes = supportedReports.get(data[offset + 1]);
				
				//Safeguard, if we get too little data, wait for some more
				if (neededBytes > remaining - 2) {
					if (D) Log.w(getDriverName(), "Got " + (remaining - 2) + " bytes for report " + data[offset + 1] + ", but need " + neededBytes);
					return remaining;
				}
				
				if (!m_readBuffers.containsKey(neededBytes))
					m_readBuffers.put(neededBytes, new byte[neededBytes]);
				
				byte[] buffer = m_readBuffers.get(neededBytes);
				System.arraycopy(data, offset + 2, buffer, 0, neededBytes);
				
				try {
					handleHIDMessage(data[offset], data[offset + 1], buffer);
				} catch (Exception ex) {
					Log.e(getDriverName(), "Handling HID message " + data[offset + 1] + " failed: " + ex.toString());
				}
			}
			else
			{
				if (D) Log.w(getDriverName(), "Got an unsupported HID report: " + data[offset + 1] + ", length: " + (remaining - 2) + ", data: " + getHexString(data, offset, offset + (remaining)));
			}
		}
		
		//Since we cannot get the underlying L2CAP "field length", we
		// *assume* that a single read delivers a single package
		//As "an error is the result of an assumption" this may break :( 
		return 0;
	}

	@Override
	protected void validateWelcomeMessage(byte[] data, int read) {
	}
	
	@Override
	public void stop() {
		if (m_controlSocket != null) {
			
			if (m_control != null) {
				try {
					m_control.close();
				} catch (IOException e) {
				}
				
				m_control = null;
			}
			
			try {
				m_controlSocket.close();
			} catch (IOException e) {
			}
			m_controlSocket = null;
		}
		
		super.stop();
	}
	
	protected abstract void handleHIDMessage(byte hidType, byte reportId, byte[] data) throws Exception;

	protected abstract Hashtable<Byte, Integer> getSupportedReportCodes();
	
}
