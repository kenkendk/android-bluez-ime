package com.hexad.bluezime;

import java.io.InputStream;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public abstract class BluetoothReader implements BluezDriverInterface {

	private static final boolean D = false;
	private static final String LOG_NAME = "BluetoothReader - ";
	
	protected volatile boolean m_isRunning = true;

	protected BluetoothSocket m_socket = null;
	protected InputStream m_input = null;
	protected Context m_context = null;
	protected String m_address = null;
	protected String m_name = null;

	protected Intent errorBroadcast = new Intent(BluezService.EVENT_ERROR);
	protected Intent connectedBroadcast = new Intent(BluezService.EVENT_CONNECTED);
	protected Intent disconnectedBroadcast = new Intent(BluezService.EVENT_DISCONNECTED);
	protected Intent keypressBroadcast = new Intent(BluezService.EVENT_KEYPRESS);
	protected Intent directionBroadcast = new Intent(BluezService.EVENT_DIRECTIONALCHANGE);
	
	//private static final UUID HID_UUID = UUID.fromString("00001124-0000-1000-8000-00805f9b34fb");
	//private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	
	public BluetoothReader(String address, Context context) throws Exception {
		
		try
		{
			m_context = context;
			m_address = address;
			BluetoothAdapter blue = BluetoothAdapter.getDefaultAdapter();
			if (blue == null)
				throw new Exception(m_context.getString(R.string.bluetooth_unsupported));
			if (!blue.isEnabled())
				throw new Exception(m_context.getString(R.string.error_bluetooth_off));
			
			blue.cancelDiscovery();
			
	        BluetoothDevice device = blue.getRemoteDevice(address);
	        m_name = device.getName();
	        
	        if (D) Log.d(LOG_NAME, "Connecting to " + address);

	        //Official method, does not work gives "Discovery error" or "Service discovery failed"
        	//m_socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
        	//m_socket.connect(); 

	        byte[] header = new byte[1024];
	        int read = -1;
	        
			//We need to do this a few times as that fixes some connection issues
			int retryCount = 5;
			do
			{
				try {
		        	//Reflection method, works on HTC desire
		        	Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
			        m_socket = (BluetoothSocket)m.invoke(device, Integer.valueOf(1));
			        m_socket.connect();

			        if (D) Log.d(LOG_NAME, "Connected to " + address);
		        	
		        	m_input = m_socket.getInputStream();
		        	read = m_input.read(header);

			        retryCount = 0;
				} catch (Exception ex) {
					if (retryCount == 0)
						throw ex;
					
					try { if (m_socket != null) m_socket.close(); }
					catch (Exception e) { }
					m_socket = null;
				}
			} while(retryCount-- > 0);
	        
        	if (D) Log.d(LOG_NAME, "Welcome message from controller was " + getHexString(header, 0, read));

        	validateWelcomeMessage(header, read);
        	
			connectedBroadcast.putExtra(BluezService.EVENT_CONNECTED_ADDRESS, address);
			m_context.sendBroadcast(connectedBroadcast);
		}
		catch (Exception ex)
		{
			try { if (m_socket != null) m_socket.close(); }
			catch (Exception e) { }
			
			m_socket = null;
        	Log.d(LOG_NAME + getDriverName(), "Failed to connect to " + address + ", message: " + ex.toString());
        	notifyError(ex);
        	
        	throw ex;
		}
			
	}
	
	protected abstract void validateWelcomeMessage(byte[] data, int read);

	@Override
	public String getDeviceAddress() {
		return m_address;
	}

	@Override
	public String getDeviceName() {
		return m_name;
	}

	@Override
	public abstract String getDriverName();

	@Override
	public boolean isRunning() {
		return m_isRunning;
	}

	@Override
	public void stop() {
		if (m_socket != null) {
			disconnectedBroadcast.putExtra(BluezService.EVENT_DISCONNECTED_ADDRESS, getDeviceAddress());
			m_context.sendBroadcast(disconnectedBroadcast);

			try { m_socket.close(); }
			catch (Exception ex) { notifyError(ex); }
		}

		m_isRunning = false;
		m_socket = null;
		m_input = null;
	}

	/*private byte[][] test = {
			{(byte)0xb6},
			{(byte)0x49, (byte)0xf6, (byte)0x09},
			{(byte)0x00},
			{(byte)0xf6, (byte)0x09},
			{(byte)0xb3, (byte)0x07},
	};
	
	private int test_index = 0;
	
	private int testRead(byte[] buffer, int offset, int max_len) {
		for(int i = 0; i < test[test_index].length; i++)
			buffer[i + offset] = test[test_index][i];
		return test[test_index++].length;
	}*/
		
	@Override
	public void run() {
        byte[] buffer = new byte[80];
        int read = 0;
        int errors = 0;
        
        int unparsed = 0;
        
        while (m_isRunning) {
        	try {
        		
        		//read = testRead(buffer, unparsed, buffer.length - unparsed);
        		read = m_input.read(buffer, unparsed, buffer.length - unparsed);
        		errors = 0;
        		
        		unparsed = parseInputData(buffer, read + unparsed);
        		if (unparsed >= buffer.length - 10) {
        			if (D) Log.e(LOG_NAME + getDriverName(), "Dumping unparsed data: " + getHexString(buffer, 0, unparsed));
        			
        			unparsed = 0;
        		}
        	} catch (Exception ex) {
        		if (D) Log.e(LOG_NAME + getDriverName(), "Got error: " + ex.toString());
        		
        		errors++;
        		if (errors > 10) {
    				//Give up
        			notifyError(ex);
        			m_isRunning = false;
        		} else if (errors > 1) {
        			//Retry after a little while
        			try { Thread.sleep(100 * errors); }
        			catch (Exception e) {}
        		}
        	}
        }
		
	}

	protected abstract int parseInputData(byte[] data, int read);
	
	private void notifyError(Exception ex) {
		Log.e(LOG_NAME + getDriverName(), ex.toString());

		errorBroadcast.putExtra(BluezService.EVENT_ERROR_SHORT, ex.getMessage());
		errorBroadcast.putExtra(BluezService.EVENT_ERROR_FULL, ex.toString());
		m_context.sendBroadcast(errorBroadcast);
		
		stop();
	}

	public static String getHexString(byte[] buffer, int offset, int count) {
        StringBuilder buf = new StringBuilder();
        for (int i = offset; i < count; i++) {
            if (buffer[i] < 0x10) 
                buf.append("0");
            buf.append(Integer.toHexString(buffer[i])).append(" ");
        }
        
        return buf.toString();
	}
	
}
