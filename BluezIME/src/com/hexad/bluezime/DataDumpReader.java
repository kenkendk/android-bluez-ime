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

import java.lang.reflect.Method;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

public class DataDumpReader extends BluetoothReader {

	private static final String LOG_NAME = "DataDumpReader";
	public static final String DRIVER_NAME = "dump";
	public static final String DISPLAY_NAME = "Data Dump Reader";
	
	public DataDumpReader(String address, Context context) throws Exception {
		super(address, context);
	}

	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}

	@Override
	protected int setupConnection(BluetoothDevice device, byte[] readBuffer) throws Exception {
    	//Reflection method, works on HTC desire
		
		String connectionType = "?";
		
		for(int port = 0; port < 31; port++) {
			Log.d(LOG_NAME, "Connecting with port: " + port);
		
			try {
				connectionType = "Secure";
				Log.d(LOG_NAME, "Attempting createRfcommSocket");
		    	Method secure = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
		
		    	BluetoothSocket s = (BluetoothSocket)secure.invoke(device, Integer.valueOf(1));
		        s.connect();
		        
		        m_socket = s;
			} catch (Exception ex) {
				Log.e(LOG_NAME, ex.toString());
				m_socket = null;
				try {
					connectionType = "Insecure";
					Log.d(LOG_NAME, "Attempting createInsecureRfcommSocket");
			    	Method secure = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
			    	
			    	BluetoothSocket s = (BluetoothSocket)secure.invoke(device, Integer.valueOf(1));
			        s.connect();
			        
			        m_socket = s;
				} catch (Exception ex2) {
					Log.e(LOG_NAME, ex2.toString());
					m_socket = null;
				}
			}
			
			if (m_socket != null) {
				Log.d(LOG_NAME, "Connection succeeded with " + connectionType + " connection on port " + port);
				break;
			}
		}
		
		if (m_socket == null) {
	    	Method secure = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
			
	    	m_socket = (BluetoothSocket)secure.invoke(device, Integer.valueOf(1));
	        m_socket.connect();
		}
			
        Log.d(LOG_NAME, "Connected to " + m_address);
    	
    	m_input = m_socket.getInputStream();
    	return m_input.read(readBuffer);		
	}

	@Override
	protected int parseInputData(byte[] data, int read) {
		Log.i(LOG_NAME, "Read data: " + getHexString(data, 0, read));
		return 0;
	}

	@Override
	protected void validateWelcomeMessage(byte[] data, int read) {
		Log.i(LOG_NAME, "Welcome message is: " + getHexString(data, 0, read));
	}

}