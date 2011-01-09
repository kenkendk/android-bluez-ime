package com.hexad.bluezime.testapp;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SeekBar;

public class TestActivity extends Activity {
    
	//These constants are copied from the BluezService
	public static final String EVENT_KEYPRESS = "com.hexad.bluezime.keypress";
	public static final String EVENT_KEYPRESS_KEY = "key";
	public static final String EVENT_KEYPRESS_ACTION = "action";

	public static final String EVENT_DIRECTIONALCHANGE = "com.hexad.bluezime.directionalchange";
	public static final String EVENT_DIRECTIONALCHANGE_DIRECTION = "direction";
	public static final String EVENT_DIRECTIONALCHANGE_VALUE = "value";

	public static final String EVENT_CONNECTED = "com.hexad.bluezime.connected";
	public static final String EVENT_CONNECTED_ADDRESS = "address";

	public static final String EVENT_DISCONNECTED = "com.hexad.bluezime.disconnected";
	public static final String EVENT_DISCONNECTED_ADDRESS = "address";

	public static final String EVENT_ERROR = "com.hexad.bluezime.error";
	public static final String EVENT_ERROR_SHORT = "message";
	public static final String EVENT_ERROR_FULL = "stacktrace";
	
	public static final String REQUEST_STATE = "com.hexad.bluezime.getstate";

	public static final String REQUEST_CONNECT = "com.hexad.bluezime.connect";
	public static final String REQUEST_CONNECT_ADDRESS = "address";
	public static final String REQUEST_CONNECT_DRIVER = "driver";
	
	public static final String REQUEST_DISCONNECT = "com.hexad.bluezime.disconnect";
	
	public static final String EVENT_REPORTSTATE = "com.hexad.bluezime.currentstate";
	public static final String EVENT_REPORTSTATE_CONNECTED = "connected";
	public static final String EVENT_REPORTSTATE_DEVICENAME = "devicename";
	public static final String EVENT_REPORTSTATE_DISPLAYNAME = "displayname";
	public static final String EVENT_REPORTSTATE_DRIVERNAME = "drivername";
	
	private static final String BLUEZ_IME_PACKAGE = "com.hexad.bluezime";
	private static final String BLUEZ_IME_SERVICE = "com.hexad.bluezime.BluezService";
	
	//These are from API level 9
	public static final int KEYCODE_BUTTON_A = 0x60;
	public static final int KEYCODE_BUTTON_B = 0x61;
	public static final int KEYCODE_BUTTON_C = 0x62;
	public static final int KEYCODE_BUTTON_X = 0x63;
	public static final int KEYCODE_BUTTON_Y = 0x64;
	public static final int KEYCODE_BUTTON_Z = 0x65;

	
	private Button m_button;
	
	private CheckBox m_checkA;
	private CheckBox m_checkB;
	private CheckBox m_checkC;
	private CheckBox m_checkX;
	private CheckBox m_checkY;
	private CheckBox m_checkZ;
	
	private SeekBar m_axisX;
	private SeekBar m_axisY;
	
	private ListView m_logList;
	
	private HashMap<Integer, CheckBox> m_buttonMap = new HashMap<Integer, CheckBox>(); 
	private ArrayList<String> m_logText = new ArrayList<String>();
	
	private boolean m_connected = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        m_button = (Button)findViewById(R.id.ConnectButton);
        m_checkA = (CheckBox)findViewById(R.id.ButtonA);
        m_checkB = (CheckBox)findViewById(R.id.ButtonB);
        m_checkC = (CheckBox)findViewById(R.id.ButtonC);
        m_checkX = (CheckBox)findViewById(R.id.ButtonX);
        m_checkY = (CheckBox)findViewById(R.id.ButtonY);
        m_checkZ = (CheckBox)findViewById(R.id.ButtonZ);
        
        m_axisX = (SeekBar)findViewById(R.id.AxisX);
        m_axisY = (SeekBar)findViewById(R.id.AxisY);
        
        m_logList = (ListView)findViewById(R.id.LogView);
        
        registerReceiver(stateCallback, new IntentFilter(EVENT_REPORTSTATE));
        registerReceiver(stateCallback, new IntentFilter(EVENT_CONNECTED));
        registerReceiver(stateCallback, new IntentFilter(EVENT_DISCONNECTED));
        
        registerReceiver(statusMonitor, new IntentFilter(EVENT_DIRECTIONALCHANGE));
        registerReceiver(statusMonitor, new IntentFilter(EVENT_KEYPRESS));
        
        m_buttonMap.put(KEYCODE_BUTTON_A, m_checkA);
        m_buttonMap.put(KEYCODE_BUTTON_B, m_checkB);
        m_buttonMap.put(KEYCODE_BUTTON_C, m_checkC);
        m_buttonMap.put(KEYCODE_BUTTON_X, m_checkX);
        m_buttonMap.put(KEYCODE_BUTTON_Y, m_checkY);
        m_buttonMap.put(KEYCODE_BUTTON_Z, m_checkZ);
        
        Intent serviceIntent = new Intent(REQUEST_STATE);
        serviceIntent.setClassName(BLUEZ_IME_PACKAGE, BLUEZ_IME_SERVICE);
        
        m_logList.setAdapter(new ArrayAdapter<String>(this, R.layout.log_item, m_logText));
        
        m_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        if (m_connected) {
			        Intent serviceIntent = new Intent(REQUEST_DISCONNECT);
			        serviceIntent.setClassName(BLUEZ_IME_PACKAGE, BLUEZ_IME_SERVICE);
			        startService(serviceIntent);
				} else {
			        Intent serviceIntent = new Intent(REQUEST_CONNECT);
			        serviceIntent.setClassName(BLUEZ_IME_PACKAGE, BLUEZ_IME_SERVICE);
			        startService(serviceIntent);
				}
			}
		});
        
        try { startService(serviceIntent); }
        catch (Exception ex) {
        	AlertDialog dlg = new AlertDialog.Builder(this).create();
        	dlg.setMessage(String.format(this.getString(R.string.error_no_bluezime), ex.getMessage()));
        	dlg.show();
        	finish();
        }
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	unregisterReceiver(stateCallback);
    	unregisterReceiver(statusMonitor);
    }
    
    private BroadcastReceiver stateCallback = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() == null)
				return;
			
			if (intent.getAction().equals(EVENT_REPORTSTATE)) {
				m_connected = intent.getBooleanExtra(EVENT_REPORTSTATE_CONNECTED, false);
				m_button.setText(m_connected ? R.string.bluezime_connected : R.string.bluezime_disconnected);
				
			} else if (intent.getAction().equals(EVENT_CONNECTED)) {
				m_button.setText(R.string.bluezime_connected);
				m_connected = true;
			} else if (intent.getAction().equals(EVENT_DISCONNECTED)) {
				m_button.setText(R.string.bluezime_disconnected);
				m_connected = false;
			}
		}
	};
	
	private BroadcastReceiver statusMonitor = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() == null)
				return;
			
			if (intent.getAction().equals(EVENT_DIRECTIONALCHANGE)) {
				int value = intent.getIntExtra(EVENT_DIRECTIONALCHANGE_VALUE, 0);
				int direction = intent.getIntExtra(EVENT_DIRECTIONALCHANGE_DIRECTION, 100);

				if (direction == 0 || direction == 1) {
					SeekBar sbar = direction == 0 ? m_axisX : m_axisY;
					sbar.setProgress(Math.min(Math.max(0, 128 + value), sbar.getMax()));
				}
				else {
					reportUnmatched(String.format(getString(R.string.unmatched_axis_event), direction + "", value + ""));
				}
				
				
			} else if (intent.getAction().equals(EVENT_KEYPRESS)) {
				int key = intent.getIntExtra(EVENT_KEYPRESS_KEY, 0);
				int action = intent.getIntExtra(EVENT_KEYPRESS_ACTION, 100);
				
				if (m_buttonMap.containsKey(key)) 
					m_buttonMap.get(key).setChecked(action == KeyEvent.ACTION_DOWN);
				else {
					reportUnmatched(String.format(getString(action == KeyEvent.ACTION_DOWN ? R.string.unmatched_key_event_down : R.string.unmatched_key_event_up), key + ""));
				}
			}
		}
	};
	
	private void reportUnmatched(String entry) {
		m_logText.add(entry);
		while (m_logText.size() > 50)
			m_logText.remove(0);
		
		
	}

}