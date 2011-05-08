package com.hexad.bluezime;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class BluezService extends IntentService {
	
	public static final String[] DRIVER_NAMES = {
		ZeemoteReader.DRIVER_NAME, 
		BGP100Reader.DRIVER_NAME, 
		PhonejoyReader.DRIVER_NAME,
		iControlPadReader.DRIVER_NAME,
		DataDumpReader.DRIVER_NAME
	};
	
	public static final String[] DRIVER_DISPLAYNAMES = {
		ZeemoteReader.DISPLAY_NAME, 
		BGP100Reader.DISPLAY_NAME, 
		PhonejoyReader.DISPLAY_NAME,
		iControlPadReader.DISPLAY_NAME,
		DataDumpReader.DISPLAY_NAME
	};
	public static final String DEFAULT_DRIVER_NAME = DRIVER_NAMES[0];
	
	public static final String EVENT_KEYPRESS = "com.hexad.bluezime.keypress";
	public static final String EVENT_KEYPRESS_KEY = "key";
	public static final String EVENT_KEYPRESS_ACTION = "action";
	public static final String EVENT_KEYPRESS_ANALOG_EMULATED = "emulated";

	public static final String EVENT_DIRECTIONALCHANGE = "com.hexad.bluezime.directionalchange";
	public static final String EVENT_DIRECTIONALCHANGE_DIRECTION = "direction";
	public static final String EVENT_DIRECTIONALCHANGE_VALUE = "value";

	public static final String EVENT_CONNECTING = "com.hexad.bluezime.connecting";
	public static final String EVENT_CONNECTING_ADDRESS = "address";

	public static final String EVENT_CONNECTED = "com.hexad.bluezime.connected";
	public static final String EVENT_CONNECTED_ADDRESS = "address";

	public static final String EVENT_DISCONNECTED = "com.hexad.bluezime.disconnected";
	public static final String EVENT_DISCONNECTED_ADDRESS = "address";

	public static final String EVENT_ERROR = "com.hexad.bluezime.error";
	public static final String EVENT_ERROR_SHORT = "message";
	public static final String EVENT_ERROR_FULL = "stacktrace";

	public static final String REQUEST_CONNECT = "com.hexad.bluezime.connect";
	public static final String REQUEST_CONNECT_ADDRESS = "address";
	public static final String REQUEST_CONNECT_DRIVER = "driver";
	
	public static final String REQUEST_DISCONNECT = "com.hexad.bluezime.disconnect";
	
	public static final String REQUEST_STATE = "com.hexad.bluezime.getstate";
	
	public static final String EVENT_REPORTSTATE = "com.hexad.bluezime.currentstate";
	public static final String EVENT_REPORTSTATE_CONNECTED = "connected";
	public static final String EVENT_REPORTSTATE_DEVICENAME = "devicename";
	public static final String EVENT_REPORTSTATE_DISPLAYNAME = "displayname";
	public static final String EVENT_REPORTSTATE_DRIVERNAME = "drivername";
	
	private static final String LOG_NAME = "BluezService";
	private final Binder binder = new LocalBinder();
	
	private static BluezDriverInterface m_reader = null;
	
	public BluezService() {
		super(LOG_NAME);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	public class LocalBinder extends Binder {
		BluezService getService() {
			return(BluezService.this);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent == null || intent.getAction() == null)
			return;
		
		if (intent.getAction().equals(REQUEST_CONNECT)) {
			Preferences p = new Preferences(this);
			
			String address = p.getSelectedDeviceAddress();
			String driver = p.getSelectedDriverName();
			
			if (intent.hasExtra(REQUEST_CONNECT_ADDRESS))
				address = intent.getStringExtra(REQUEST_CONNECT_ADDRESS);
			if (intent.hasExtra(REQUEST_CONNECT_DRIVER))
				driver = intent.getStringExtra(REQUEST_CONNECT_DRIVER);
			
			connectToDevice(address, driver);
		} else if (intent.getAction().equals(REQUEST_DISCONNECT)) {
			disconnectFromDevice();
		} else if (intent.getAction().equals(REQUEST_STATE)) {
			Intent i = new Intent(EVENT_REPORTSTATE);
			
			synchronized (this) {
				i.putExtra(EVENT_REPORTSTATE_CONNECTED, m_reader != null);
				if (m_reader != null) {
					i.putExtra(EVENT_REPORTSTATE_DEVICENAME, m_reader.getDeviceAddress());
					i.putExtra(EVENT_REPORTSTATE_DISPLAYNAME, m_reader.getDeviceName());
					i.putExtra(EVENT_REPORTSTATE_DRIVERNAME, m_reader.getDriverName());
				}
			}
			
			sendBroadcast(i);
		} else {
			notifyError(new Exception(this.getString(R.string.bluetooth_unsupported)));
		}
	}

	private synchronized void disconnectFromDevice()
	{
		String adr = "<null>"; 
		try
		{
			if (m_reader != null) {
				adr = m_reader.getDeviceAddress();
				m_reader.stop();
			}
		}
		catch (Exception ex)
		{
        	Log.e(LOG_NAME, "Error on disconnect from " + adr + ", message: " + ex.toString());
        	notifyError(ex);
		}
		finally
		{
			m_reader = null;
		}
	}
	
	private synchronized void connectToDevice(String address, String driver) {

		try {
			if (address == null || address.trim().length() == 0)
				throw new Exception("Invalid call, no address specified");
			if (driver == null || driver.trim().length() == 0)
				throw new Exception("Invalid call, no driver specified");

			if (m_reader != null)
			{
				if (m_reader.isRunning() && address.equals(m_reader.getDeviceAddress()) && driver.toLowerCase().equals(m_reader.getDriverName()))
					return; //Already connected
				
				//Connect to other device, disconnect
				disconnectFromDevice();
			}
			
			BluetoothAdapter blue = BluetoothAdapter.getDefaultAdapter();
			if (blue == null)
				throw new Exception(this.getString(R.string.bluetooth_unsupported));

			if (!blue.isEnabled())
				throw new Exception(this.getString(R.string.error_bluetooth_off));

			Intent connectingBroadcast = new Intent(EVENT_CONNECTING);
			connectingBroadcast.putExtra(EVENT_CONNECTING_ADDRESS, address);
			sendBroadcast(connectingBroadcast);

			if (driver.toLowerCase().equals(ZeemoteReader.DRIVER_NAME.toLowerCase()))
				m_reader = new ZeemoteReader(address, getApplicationContext());
			else if (driver.toLowerCase().equals(BGP100Reader.DRIVER_NAME.toLowerCase()))
				m_reader = new BGP100Reader(address, getApplicationContext());
			else if (driver.toLowerCase().equals(PhonejoyReader.DRIVER_NAME.toLowerCase()))
				m_reader = new PhonejoyReader(address, getApplicationContext());
			else if (driver.toLowerCase().equals(DataDumpReader.DRIVER_NAME.toLowerCase()))
				m_reader = new DataDumpReader(address, getApplicationContext());
			else if (driver.toLowerCase().equals(iControlPadReader.DRIVER_NAME.toLowerCase()))
				m_reader = new iControlPadReader(address, getApplicationContext());
			else
				throw new Exception(String.format(this.getString(R.string.invalid_driver), driver));
			
			new Thread(m_reader).start();
		} catch (Exception ex) {
			notifyError(ex);
		}
	}

	private void notifyError(Exception ex) {
		Log.e(LOG_NAME, ex.toString());

		Intent errorBroadcast = new Intent(EVENT_ERROR);
		errorBroadcast.putExtra(EVENT_ERROR_SHORT, ex.getMessage());
		errorBroadcast.putExtra(EVENT_ERROR_FULL, ex.toString());
		sendBroadcast(errorBroadcast);
		
		disconnectFromDevice();
	}
}
