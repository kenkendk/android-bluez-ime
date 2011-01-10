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

	        //Official method, does not work gives "Discovery error"
        	//m_socket = device.createRfcommSocketToServiceRecord(HID_UUID);
        	//m_socket.connect(); 

        	//Reflection method, works on HTC desire
        	Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
	        m_socket = (BluetoothSocket)m.invoke(device, Integer.valueOf(1));
	        m_socket.connect();
	        
	        if (D) Log.d(LOG_NAME, "Connected to " + address);
        	
        	byte[] header = new byte[1024];
        	m_input = m_socket.getInputStream();
        	int read = m_input.read(header);
        	        	
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
        	if (D) Log.d(LOG_NAME + getDriverName(), "Failed to connect to " + address + ", message: " + ex.toString());
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

	@Override
	public void run() {
        byte[] buffer = new byte[80];
        int read = 0;
        int errors = 0;
        
        while (m_isRunning) {
        	try {
        		read = m_input.read(buffer);
        		errors = 0;
        		
        		int unparsed = parseInputData(buffer, read);
        		if (unparsed > 0)
    				if (D) Log.w(LOG_NAME + getDriverName(), "Unable to interpret the message: " + getHexString(buffer, read - unparsed, read));
        	} catch (Exception ex) {
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
		Log.e(LOG_NAME, ex.toString());

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
