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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.InputMethodService;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

public class BluezIME extends InputMethodService {

	private static final String SESSION_ID = "com.hexad.bluezime.ime.controller";
	
	private static final boolean D = false;
	private static final String LOG_NAME = "BluezInput";
	private final String[] m_connectedIds = new String[Preferences.MAX_NO_OF_CONTROLLERS];
	private Preferences m_prefs;
	
	private NotificationManager m_notificationManager;
	private Notification m_notification;
	private int[] m_keyMappingCache = null;
	
	private PowerManager.WakeLock m_wakelock = null;
	private int m_wakelocktype = 0;
	
	@Override
	public void onCreate() {
		super.onCreate();
		m_prefs = new Preferences(this);
		
		m_notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		m_notification = new Notification(R.drawable.icon, getString(R.string.app_name), System.currentTimeMillis());
		m_keyMappingCache = new int[Math.max(0x100, KeyEvent.getMaxKeyCode())];
		for(int i = 0; i < m_keyMappingCache.length; i++)
			m_keyMappingCache[i] = -1;
				
		setNotificationText(getString(R.string.ime_starting));
		acquireWakeLock();

        registerReceiver(connectReceiver, new IntentFilter(BluezService.EVENT_CONNECTED));
        registerReceiver(connectingReceiver, new IntentFilter(BluezService.EVENT_CONNECTING));
        registerReceiver(disconnectReceiver, new IntentFilter(BluezService.EVENT_DISCONNECTED));
        registerReceiver(errorReceiver, new IntentFilter(BluezService.EVENT_ERROR));
        registerReceiver(preferenceChangedHandler, new IntentFilter(Preferences.PREFERENCES_UPDATED));
        registerReceiver(activityHandler, new IntentFilter(BluezService.EVENT_KEYPRESS));
        registerReceiver(activityHandler, new IntentFilter(BluezService.EVENT_DIRECTIONALCHANGE));
    	registerReceiver(bluetoothStateMonitor, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
	}
	
	private void setNotificationText(CharSequence message) {
		Intent i = new Intent(this, BluezIMESettings.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		m_notification.setLatestEventInfo(this, getString(R.string.app_name), message, pi);
		m_notificationManager.notify(1, m_notification);
	}
	
	private void acquireWakeLock() {
		if (m_wakelock == null) {
			int wakelocktype = m_prefs.getWakeLock();
			if (wakelocktype != Preferences.NO_WAKE_LOCK) {
				try 
				{ 
					m_wakelock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(wakelocktype, "com.hexad.bluezime.WakeLock"); 
					m_wakelock.acquire();
					m_wakelocktype = wakelocktype;
				}
				catch (Throwable e) {
					if (D) Log.e(LOG_NAME, e.getMessage());
				}
				
			}
		}
	}
	
	private void releaseWakeLock() {
		if (m_wakelock != null) {
			m_wakelock.release();
			m_wakelock = null;
			m_wakelocktype = 0;
		}
	}
	
	private int getConnectedCount() {
		int count = 0;
		for(int i = 0; i < m_connectedIds.length; i++)
			if (m_connectedIds[i] != null)
				count++;
		
		return count;
	}

	@Override
	public View onCreateInputView() {
		super.onCreateInputView();
		return null;
	}

	@Override
	public View onCreateCandidatesView() {
		super.onCreateCandidatesView();
		return null;
	}
	
	@Override
	public void onStartInputView(EditorInfo info, boolean restarting) {
		super.onStartInputView(info, restarting);
		
		if (D) Log.d(LOG_NAME, "Start input view");

        if (getConnectedCount() != m_prefs.getControllerCount())
        	connect();
	}

	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		super.onStartInput(attribute, restarting);
		
		//Reconnect if we lost connection
		if (getConnectedCount() != m_prefs.getControllerCount())
			connect();
	}
	
	private void connect() {
    	if (D) Log.d(LOG_NAME, "Connecting");
    	
    	if (m_prefs.getManageBluetooth()) {
	    	try {
	    		if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
	    	    	//Unfortunately this does not work because there is no view
	    	    	// associated with this IME, and thus we cannot show popup dialogs
	    			//ImprovedBluetoothDevice.ActivateBluetooth(this, m_inputView);
	    			
	    			//NOTE: This violates the guidelines that state that the user should 
	    			// be asked explicitly before turning BT on 
	    			BluetoothAdapter.getDefaultAdapter().enable();
	
	    			Toast.makeText(this, this.getString(R.string.enabling_bluetooth), Toast.LENGTH_SHORT).show();
	    			setNotificationText(this.getString(R.string.enabling_bluetooth));
	
	    			//We will connect when BT is on
	    			return;
	    		}
	    	} catch (Exception ex) {
	    		Log.e(LOG_NAME, "Failed to activate bluetooth: " + ex.getMessage());
	    	}
    	}
    	
    	for(int i = 0; i < m_prefs.getControllerCount(); i++) {
	    	String address = m_prefs.getSelectedDeviceAddress(i);
	    	String driver = m_prefs.getSelectedDriverName(i);
	
	    	Intent intent = new Intent(this, BluezService.class);
	    	intent.setAction(BluezService.REQUEST_CONNECT);
	    	intent.putExtra(BluezService.REQUEST_CONNECT_ADDRESS, address);
	    	intent.putExtra(BluezService.REQUEST_CONNECT_DRIVER, driver);
	    	intent.putExtra(BluezService.SESSION_ID, SESSION_ID + i);
	    	intent.putExtra(BluezService.REQUEST_CONNECT_CREATE_NOTIFICATION, false);
			startService(intent);
    	}
	}
	
	@Override
	public void onFinishInput() {
        super.onFinishInput();

        if (D) Log.d(LOG_NAME, "Finish input view");
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (D) Log.d(LOG_NAME, "Destroy IME");
		
		m_notificationManager.cancel(1);
		
		if (getConnectedCount() > 0) {
			if (D) Log.d(LOG_NAME, "Disconnecting");

			for(int i = 0; i < m_connectedIds.length; i++) {
				if (m_connectedIds[i] != null) {
					Intent intent = new Intent(this, BluezService.class);
					intent.setAction(BluezService.REQUEST_DISCONNECT);
					intent.putExtra(BluezService.SESSION_ID, m_connectedIds[i]);
					startService(intent);
				}
			}
		}
		
		releaseWakeLock();
		
        unregisterReceiver(connectReceiver);
        unregisterReceiver(connectingReceiver);
        unregisterReceiver(disconnectReceiver);
        unregisterReceiver(errorReceiver);
        unregisterReceiver(preferenceChangedHandler);
        unregisterReceiver(activityHandler);
        unregisterReceiver(bluetoothStateMonitor);
        
        connectReceiver = null;
        connectingReceiver = null;
        disconnectReceiver = null;
        activityHandler = null;
        errorReceiver = null;
        preferenceChangedHandler = null;
        bluetoothStateMonitor = null;
        
        if (m_prefs.getManageBluetooth()) {
	        try {
	        	if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
	        		Toast.makeText(this, this.getString(R.string.disabling_bluetooth), Toast.LENGTH_SHORT).show();
	        		BluetoothAdapter.getDefaultAdapter().disable();
	        	}
	        } catch (Exception ex) {
	        	Log.e(LOG_NAME, "Failed to turn BT off: " + ex.getMessage());
	        }
        }
	}
	
	private BroadcastReceiver connectingReceiver =  new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String sid = intent.getStringExtra(BluezService.SESSION_ID);
			if (sid == null || !sid.startsWith(SESSION_ID))
				return;
			
			String address = intent.getStringExtra(BluezService.EVENT_CONNECTING_ADDRESS);
			Toast.makeText(BluezIME.this, String.format(getString(R.string.ime_connecting), address), Toast.LENGTH_SHORT).show();
			setNotificationText(String.format(getString(R.string.ime_connecting), address));
		}
	};

	private BroadcastReceiver connectReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String sid = intent.getStringExtra(BluezService.SESSION_ID);
			if (sid == null || !sid.startsWith(SESSION_ID))
				return;

			int controllerNo = -1;
			try { controllerNo = Integer.parseInt(sid.substring(SESSION_ID.length())); }
			catch (Throwable t) { if (D) Log.w(LOG_NAME, "Failed to parse connectId: " + sid); }

			if (controllerNo < 0 || controllerNo >= m_connectedIds.length)
				return;
			
			if (D) Log.d(LOG_NAME, "Connect received");
			Toast.makeText(context, String.format(context.getString(R.string.connected_to_device_message), intent.getStringExtra(BluezService.EVENT_CONNECTED_ADDRESS)), Toast.LENGTH_SHORT).show();
			m_connectedIds[controllerNo] = sid;
			setNotificationText(String.format(getString(R.string.ime_connected), intent.getStringExtra(BluezService.EVENT_CONNECTED_ADDRESS)));
		}
	};
	
	private BroadcastReceiver disconnectReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String sid = intent.getStringExtra(BluezService.SESSION_ID);
			if (sid == null || !sid.startsWith(SESSION_ID))
				return;

			if (D) Log.d(LOG_NAME, "Disconnect received");
			Toast.makeText(context, String.format(context.getString(R.string.disconnected_from_device_message), intent.getStringExtra(BluezService.EVENT_DISCONNECTED_ADDRESS)), Toast.LENGTH_SHORT).show();
			for(int i = 0; i < m_connectedIds.length; i++) {
				if (sid.equals(m_connectedIds[i]))
					m_connectedIds[i] = null;
			}
			
			setNotificationText(getString(R.string.ime_disconnected));
		}
	};
	
	private BroadcastReceiver errorReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String sid = intent.getStringExtra(BluezService.SESSION_ID);
			if (sid == null || !sid.startsWith(SESSION_ID))
				return;

			if (D) Log.d(LOG_NAME, "Error received");
			Toast.makeText(context, String.format(context.getString(R.string.error_message_generic), intent.getStringExtra(BluezService.EVENT_ERROR_SHORT)), Toast.LENGTH_SHORT).show();
			setNotificationText(String.format(getString(R.string.ime_error), intent.getStringExtra(BluezService.EVENT_ERROR_SHORT)));
		}
	};

	private BroadcastReceiver preferenceChangedHandler = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			//Clear the key mapping cache
			for(int i = 0; i < m_keyMappingCache.length; i++)
				m_keyMappingCache[i] = -1;
			
			if (getConnectedCount() > 0)
				connect();
			
			if (m_wakelocktype != m_prefs.getWakeLock()) {
				releaseWakeLock();
				acquireWakeLock();
			}
		}
	};

	private BroadcastReceiver activityHandler = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String sid = intent.getStringExtra(BluezService.SESSION_ID);
			if (sid == null || !sid.startsWith(SESSION_ID))
				return;
			

			if (D) Log.d(LOG_NAME, "Update event received");
			
			try {
				int controllerNo = Integer.parseInt(sid.substring(SESSION_ID.length()));

				InputConnection ic = getCurrentInputConnection();
				long eventTime = SystemClock.uptimeMillis();

				if (intent.getAction().equals(BluezService.EVENT_KEYPRESS)) {
					int action = intent.getIntExtra(BluezService.EVENT_KEYPRESS_ACTION, KeyEvent.ACTION_DOWN);
					int key = intent.getIntExtra(BluezService.EVENT_KEYPRESS_KEY, 0);

					//This construct ensures that we can perform lock free
					// access to m_keyMappingCache and never risk sending -1 
					// as the keyCode
					if (key >= m_keyMappingCache.length) {
						Log.e(LOG_NAME, "Key reported by driver: " + key + ", size of keymapping array: " + m_keyMappingCache.length);
					} else {
						int translatedKey = m_keyMappingCache[key];
						if (translatedKey == -1) {
							translatedKey = m_prefs.getKeyMapping(key, controllerNo);
							m_keyMappingCache[key] = translatedKey;
						} 
						if (D) Log.d(LOG_NAME, "Sending key event: " + (action == KeyEvent.ACTION_DOWN ? "Down" : "Up") + " - " + key);
						ic.sendKeyEvent(new KeyEvent(eventTime, eventTime, action, translatedKey, 0, 0, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE));
					}
				}
			} catch (Exception ex) {
				Log.e(LOG_NAME, "Failed to send key events: " + ex.toString());
			}
		}
	};
	
	private BroadcastReceiver bluetoothStateMonitor = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
			if (state == BluetoothAdapter.STATE_ON) {
				if (getConnectedCount() == 0)
					connect();
			}
		}
	};
}
