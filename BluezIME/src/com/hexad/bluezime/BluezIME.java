package com.hexad.bluezime;

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
	
	private boolean[] m_lastDirectionsKeys = new boolean[4];
	private int[] m_directionKeyCodes = new int[] { KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP }; 
	
	@Override
	public void onCreate() {
		super.onCreate();
		m_prefs = new Preferences(this);
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
		
		if (connectReceiver == null) {
			connectReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (D) Log.d(LOG_NAME, "Connect received");
					Toast.makeText(context, String.format(context.getString(R.string.connected_to_device_message), intent.getStringExtra(BluezService.EVENT_CONNECTED_ADDRESS)), Toast.LENGTH_SHORT).show();
					m_connected = true;
				}
			};
			
			disconnectReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (D) Log.d(LOG_NAME, "Disconnect received");
					Toast.makeText(context, String.format(context.getString(R.string.disconnected_from_device_message), intent.getStringExtra(BluezService.EVENT_DISCONNECTED_ADDRESS)), Toast.LENGTH_SHORT).show();
					m_connected = false;
				}
			};
			
			errorReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (D) Log.d(LOG_NAME, "Error received");
					Toast.makeText(context, String.format(context.getString(R.string.error_message_generic), intent.getStringExtra(BluezService.EVENT_ERROR_SHORT)), Toast.LENGTH_SHORT).show();
				}
			};
			
			preferenceChangedHandler = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (m_connected)
						connect();
				}
			};
			
			if (activityHandler == null)
				activityHandler = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						if (D) Log.d(LOG_NAME, "Update event received");
						
						try {
							InputConnection ic = getCurrentInputConnection();
							long eventTime = SystemClock.uptimeMillis();
	
							if (intent.getAction().equals(BluezService.EVENT_KEYPRESS)) {
								int action = intent.getIntExtra(BluezService.EVENT_KEYPRESS_ACTION, KeyEvent.ACTION_DOWN);
								int key = intent.getIntExtra(BluezService.EVENT_KEYPRESS_KEY, 0);
								ic.sendKeyEvent(new KeyEvent(eventTime, eventTime, action, key, 0, 0, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE));
							} else if (intent.getAction().equals(BluezService.EVENT_DIRECTIONALCHANGE)) {
								int value = intent.getIntExtra(BluezService.EVENT_DIRECTIONALCHANGE_VALUE, 0);
								int direction = intent.getIntExtra(BluezService.EVENT_DIRECTIONALCHANGE_DIRECTION, 100);
								
								
								boolean[] newKeyStates = m_lastDirectionsKeys.clone();
								
								//We only support X/Y axis
								if (direction == 0 || direction == 1) {
									newKeyStates[(direction * 2)] = value > (128 / 2);
									newKeyStates[(direction * 2) + 1] = value < -(128 / 2);
								}
	
								for(int i = 0; i < 4; i++)
									if (newKeyStates[i] != m_lastDirectionsKeys[i])
										ic.sendKeyEvent(new KeyEvent(eventTime, eventTime, newKeyStates[i] ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP, m_directionKeyCodes[i], 0, 0, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD|KeyEvent.FLAG_KEEP_TOUCH_MODE));
				
								m_lastDirectionsKeys = newKeyStates;
							}
						} catch (Exception ex) {
							Log.e(LOG_NAME, "Failed to send key events: " + ex.toString());
						}
					}
				};	
			
	        registerReceiver(connectReceiver, new IntentFilter(BluezService.EVENT_CONNECTED));
	        registerReceiver(disconnectReceiver, new IntentFilter(BluezService.EVENT_DISCONNECTED));
	        registerReceiver(errorReceiver, new IntentFilter(BluezService.EVENT_ERROR));
	        registerReceiver(preferenceChangedHandler, new IntentFilter(Preferences.PREFERENCES_UPDATED));
	        registerReceiver(activityHandler, new IntentFilter(BluezService.EVENT_KEYPRESS));
	        registerReceiver(activityHandler, new IntentFilter(BluezService.EVENT_DIRECTIONALCHANGE));
		}
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
		
		if (m_connected) {
			if (D) Log.d(LOG_NAME, "Disconnecting");
			Intent i = new Intent(this, BluezService.class);
			i.setAction(BluezService.REQUEST_DISCONNECT);
			startService(i);
		}
		
        if (connectReceiver != null) {
	        unregisterReceiver(connectReceiver);
	        unregisterReceiver(disconnectReceiver);
	        unregisterReceiver(errorReceiver);
	        unregisterReceiver(preferenceChangedHandler);
	        unregisterReceiver(activityHandler);
	        
	        connectReceiver = null;
	        disconnectReceiver = null;
	        activityHandler = null;
	        errorReceiver = null;
	        preferenceChangedHandler = null;
        }
		
	}
	
	private BroadcastReceiver connectReceiver = null;
	private BroadcastReceiver disconnectReceiver = null;
	private BroadcastReceiver errorReceiver = null;
	private BroadcastReceiver activityHandler = null;
	private BroadcastReceiver preferenceChangedHandler = null;
	
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
