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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.InputMethodService;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

public class BluezIME extends InputMethodService {

	private static final boolean D = false;
	private static final String LOG_NAME = "BluezInput";
	private boolean m_connected = false;
	private Preferences m_prefs;
	
	private NotificationManager m_notificationManager;
	private Notification m_notification;
	private int[] m_keyMappingCache = null;
	
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

        registerReceiver(connectReceiver, new IntentFilter(BluezService.EVENT_CONNECTED));
        registerReceiver(connectingReceiver, new IntentFilter(BluezService.EVENT_CONNECTING));
        registerReceiver(disconnectReceiver, new IntentFilter(BluezService.EVENT_DISCONNECTED));
        registerReceiver(errorReceiver, new IntentFilter(BluezService.EVENT_ERROR));
        registerReceiver(preferenceChangedHandler, new IntentFilter(Preferences.PREFERENCES_UPDATED));
        registerReceiver(activityHandler, new IntentFilter(BluezService.EVENT_KEYPRESS));
        registerReceiver(activityHandler, new IntentFilter(BluezService.EVENT_DIRECTIONALCHANGE));
	}
	
	private void setNotificationText(CharSequence message) {
		Intent i = new Intent(this, BluezIMESettings.class);
		PendingIntent pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		m_notification.setLatestEventInfo(this, getString(R.string.app_name), message, pi);
		m_notificationManager.notify(1, m_notification);
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

        if (!m_connected)
        	connect();
	}

	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		super.onStartInput(attribute, restarting);
		
		//Reconnect if we lost connection
		if (!m_connected)
			connect();
	}
	
	private void connect() {
    	if (D) Log.d(LOG_NAME, "Connecting");
    	String address = m_prefs.getSelectedDeviceAddress();
    	String driver = m_prefs.getSelectedDriverName();

		Intent i = new Intent(this, BluezService.class);
		i.setAction(BluezService.REQUEST_CONNECT);
		i.putExtra(BluezService.REQUEST_CONNECT_ADDRESS, address);
		i.putExtra(BluezService.REQUEST_CONNECT_DRIVER, driver);
		startService(i);
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
		
		if (m_connected) {
			if (D) Log.d(LOG_NAME, "Disconnecting");
			Intent i = new Intent(this, BluezService.class);
			i.setAction(BluezService.REQUEST_DISCONNECT);
			startService(i);
		}
		
        unregisterReceiver(connectReceiver);
        unregisterReceiver(connectingReceiver);
        unregisterReceiver(disconnectReceiver);
        unregisterReceiver(errorReceiver);
        unregisterReceiver(preferenceChangedHandler);
        unregisterReceiver(activityHandler);
        
        connectReceiver = null;
        connectingReceiver = null;
        disconnectReceiver = null;
        activityHandler = null;
        errorReceiver = null;
        preferenceChangedHandler = null;
	}
	
	private BroadcastReceiver connectingReceiver =  new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String address = intent.getStringExtra(BluezService.EVENT_CONNECTING_ADDRESS);
			setNotificationText(String.format(getString(R.string.ime_connecting), address));
		}
	};

	private BroadcastReceiver connectReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (D) Log.d(LOG_NAME, "Connect received");
			Toast.makeText(context, String.format(context.getString(R.string.connected_to_device_message), intent.getStringExtra(BluezService.EVENT_CONNECTED_ADDRESS)), Toast.LENGTH_SHORT).show();
			m_connected = true;
			setNotificationText(String.format(getString(R.string.ime_connected), intent.getStringExtra(BluezService.EVENT_CONNECTED_ADDRESS)));
		}
	};
	
	private BroadcastReceiver disconnectReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (D) Log.d(LOG_NAME, "Disconnect received");
			Toast.makeText(context, String.format(context.getString(R.string.disconnected_from_device_message), intent.getStringExtra(BluezService.EVENT_DISCONNECTED_ADDRESS)), Toast.LENGTH_SHORT).show();
			m_connected = false;
			setNotificationText(getString(R.string.ime_disconnected));
		}
	};
	
	private BroadcastReceiver errorReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
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
			
			if (m_connected)
				connect();
		}
	};

	private BroadcastReceiver activityHandler = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (D) Log.d(LOG_NAME, "Update event received");
			
			try {
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
							translatedKey = m_prefs.getKeyMapping(key);
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

	
	//Deprecated, could be used to simulate hardware keypress
	/*final IWindowManager windowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
	
    private void doInjectKeyEvent(KeyEvent kEvent) {
        try {
                // Inject the KeyEvent to the Window-Manager.
                windowManager.injectKeyEvent(kEvent.isDown(), kEvent.getKeyCode(),
                                kEvent.getRepeatCount(), kEvent.getDownTime(), kEvent
                                                .getEventTime(), true);
        } catch (DeadObjectException e) {
                e.printStackTrace();
        }
    }*/	

}
