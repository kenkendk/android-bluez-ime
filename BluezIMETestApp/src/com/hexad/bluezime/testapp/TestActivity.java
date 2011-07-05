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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class TestActivity extends Activity {
    
	//These constants are copied from the BluezService
	public static final String SESSION_ID = "com.hexad.bluezime.sessionid";
	
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
	
	public static final String REQUEST_FEATURECHANGE = "com.hexad.bluezime.featurechange";
	public static final String REQUEST_FEATURECHANGE_RUMBLE = "rumble"; //Boolean, true=on, false=off
	public static final String REQUEST_FEATURECHANGE_LEDID = "ledid"; //Integer, LED to use 1-4 for Wiimote
	public static final String REQUEST_FEATURECHANGE_ACCELEROMETER = "accelerometer"; //Boolean, true=on, false=off
	
	public static final String REQUEST_CONFIG = "com.hexad.bluezime.getconfig";
	
	public static final String EVENT_REPORT_CONFIG = "com.hexad.bluezime.config";
	public static final String EVENT_REPORT_CONFIG_VERSION = "version";
	public static final String EVENT_REPORT_CONFIG_DRIVER_NAMES = "drivernames";
	public static final String EVENT_REPORT_CONFIG_DRIVER_DISPLAYNAMES = "driverdisplaynames";

	
	private static final String BLUEZ_IME_PACKAGE = "com.hexad.bluezime";
	private static final String BLUEZ_IME_SERVICE = "com.hexad.bluezime.BluezService";
	
	//These are from API level 9
	public static final int KEYCODE_BUTTON_A = 0x60;
	public static final int KEYCODE_BUTTON_B = 0x61;
	public static final int KEYCODE_BUTTON_C = 0x62;
	public static final int KEYCODE_BUTTON_X = 0x63;
	public static final int KEYCODE_BUTTON_Y = 0x64;
	public static final int KEYCODE_BUTTON_Z = 0x65;

	//A string used to ensure that apps do not interfere with each other
	public static final String SESSION_NAME = "TEST-BLUEZ-IME";
	
	//Data for drivers, used for Bluez-IME < 9
	private String[] m_driver_names = {
		"zeemote", 
		"bgp100", 
		"phonejoy",
		"icontrolpad",
		"wiimote",
		"dump"
	};
	
	public String[] m_driver_displaynames = {
		"Zeemote JS1", 
		"BGP100 Chainpus", 
		"Phonejoy",
		"iControlPad",
		"Wiimote",
		"Data dump"
	};

	
	private TextView m_mac;
	private Spinner m_driver;
	private String m_selectedDriver;
	private ArrayAdapter<CharSequence> m_driverAdapter;
	private TableLayout m_connectData;
	private Button m_button;
	
	private CheckBox m_checkA;
	private CheckBox m_checkB;
	private CheckBox m_checkC;
	private CheckBox m_checkX;
	private CheckBox m_checkY;
	private CheckBox m_checkZ;
	
	private SeekBar m_axisX1;
	private SeekBar m_axisY1;
	private SeekBar m_axisX2;
	private SeekBar m_axisY2;
	
	private ListView m_logList;
	private ArrayAdapter<String> m_logAdapter;
	
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
        
        m_axisX1 = (SeekBar)findViewById(R.id.AxisX1);
        m_axisY1 = (SeekBar)findViewById(R.id.AxisY1);
        m_axisX2 = (SeekBar)findViewById(R.id.AxisX2);
        m_axisY2 = (SeekBar)findViewById(R.id.AxisY2);
        
        m_logList = (ListView)findViewById(R.id.LogView);
        
        m_connectData = (TableLayout)findViewById(R.id.ConnectionData);
        m_mac = (TextView)findViewById(R.id.MACAddress);
        m_driver = (Spinner)findViewById(R.id.DriverName);
        m_driver.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				m_selectedDriver = m_driver_names[pos];
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
        
        m_driverAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);
        m_driverAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        m_driver.setAdapter(m_driverAdapter);
        
        populateDriverBox(m_driver_names, m_driver_displaynames);
        
        registerReceiver(stateCallback, new IntentFilter(EVENT_REPORT_CONFIG));
        registerReceiver(stateCallback, new IntentFilter(EVENT_REPORTSTATE));
        registerReceiver(stateCallback, new IntentFilter(EVENT_CONNECTED));
        registerReceiver(stateCallback, new IntentFilter(EVENT_DISCONNECTED));
        registerReceiver(stateCallback, new IntentFilter(EVENT_ERROR));
        
        registerReceiver(statusMonitor, new IntentFilter(EVENT_DIRECTIONALCHANGE));
        registerReceiver(statusMonitor, new IntentFilter(EVENT_KEYPRESS));
        
        m_buttonMap.put(KEYCODE_BUTTON_A, m_checkA);
        m_buttonMap.put(KEYCODE_BUTTON_B, m_checkB);
        m_buttonMap.put(KEYCODE_BUTTON_C, m_checkC);
        m_buttonMap.put(KEYCODE_BUTTON_X, m_checkX);
        m_buttonMap.put(KEYCODE_BUTTON_Y, m_checkY);
        m_buttonMap.put(KEYCODE_BUTTON_Z, m_checkZ);
        
        m_logAdapter = new ArrayAdapter<String>(this, R.layout.log_item, m_logText);
        m_logList.setAdapter(m_logAdapter);
        
        m_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		        if (m_connected) {
			        Intent serviceIntent = new Intent(REQUEST_DISCONNECT);
			        serviceIntent.setClassName(BLUEZ_IME_PACKAGE, BLUEZ_IME_SERVICE);
			        serviceIntent.putExtra(SESSION_ID, SESSION_NAME);
			        startService(serviceIntent);
				} else {
			        Intent serviceIntent = new Intent(REQUEST_CONNECT);
			        serviceIntent.setClassName(BLUEZ_IME_PACKAGE, BLUEZ_IME_SERVICE);
			        serviceIntent.putExtra(SESSION_ID, SESSION_NAME);
			        serviceIntent.putExtra(REQUEST_CONNECT_ADDRESS, m_mac.getText().toString());
			        serviceIntent.putExtra(REQUEST_CONNECT_DRIVER, m_selectedDriver);
			        startService(serviceIntent);
				}
			}
		});
        
        //Request config, not present in version < 9
        Intent serviceIntent = new Intent(REQUEST_CONFIG);
        serviceIntent.setClassName(BLUEZ_IME_PACKAGE, BLUEZ_IME_SERVICE);
        serviceIntent.putExtra(SESSION_ID, SESSION_NAME);
        startService(serviceIntent); 
        
        //Request device connection state
        serviceIntent = new Intent(REQUEST_STATE);
        serviceIntent.setClassName(BLUEZ_IME_PACKAGE, BLUEZ_IME_SERVICE);
        serviceIntent.putExtra(SESSION_ID, SESSION_NAME);
        startService(serviceIntent);
    }
    
    private void setConnectPropertiesVisibility(boolean visible) {
    	m_connectData.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
    
    private void populateDriverBox(String[] keys, String[] displays) {
    	m_driverAdapter.clear();
    	m_driver_names = keys;
    	m_driver_displaynames = displays;
    	for(int i = 0; i < keys.length; i++)
    		m_driverAdapter.add(displays[i]);
    		
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
			
			//Filter everything that is not related to this session
			if (!SESSION_NAME.equals(intent.getStringExtra(SESSION_ID)))
				return;
			
			if (intent.getAction().equals(EVENT_REPORT_CONFIG)) {
				Toast.makeText(TestActivity.this, "Bluez-IME version " + intent.getIntExtra(EVENT_REPORT_CONFIG_VERSION, 0), Toast.LENGTH_SHORT).show();				
				populateDriverBox(intent.getStringArrayExtra(EVENT_REPORT_CONFIG_DRIVER_NAMES), intent.getStringArrayExtra(EVENT_REPORT_CONFIG_DRIVER_DISPLAYNAMES));
			} else if (intent.getAction().equals(EVENT_REPORTSTATE)) {
				m_connected = intent.getBooleanExtra(EVENT_REPORTSTATE_CONNECTED, false);
				m_button.setText(m_connected ? R.string.bluezime_connected : R.string.bluezime_disconnected);
				
				//After we connect, we rumble the device for a second if it is supported
				if (m_connected) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							Intent req = new Intent(REQUEST_FEATURECHANGE);
							req.putExtra(REQUEST_FEATURECHANGE_LEDID, 2);
							req.putExtra(REQUEST_FEATURECHANGE_RUMBLE, true);
							startService(req);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
							}
							req.putExtra(REQUEST_FEATURECHANGE_LEDID, 1);
							req.putExtra(REQUEST_FEATURECHANGE_RUMBLE, false);
							startService(req);
						}
					});
				}
				
			} else if (intent.getAction().equals(EVENT_CONNECTED)) {
				m_button.setText(R.string.bluezime_connected);
				m_connected = true;
			} else if (intent.getAction().equals(EVENT_DISCONNECTED)) {
				m_button.setText(R.string.bluezime_disconnected);
				m_connected = false;
			} else if (intent.getAction().equals(EVENT_ERROR)) {
				Toast.makeText(TestActivity.this, "Error: " + intent.getStringExtra(EVENT_ERROR_SHORT), Toast.LENGTH_SHORT).show();
				reportUnmatched("Error: " + intent.getStringExtra(EVENT_ERROR_FULL));
				m_connected = false;
			}
			
			setConnectPropertiesVisibility(!m_connected);			
		}
	};
	
	private BroadcastReceiver statusMonitor = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() == null)
				return;
			if (!SESSION_NAME.equals(intent.getStringExtra(SESSION_ID)))
				return;

			if (intent.getAction().equals(EVENT_DIRECTIONALCHANGE)) {
				int value = intent.getIntExtra(EVENT_DIRECTIONALCHANGE_VALUE, 0);
				int direction = intent.getIntExtra(EVENT_DIRECTIONALCHANGE_DIRECTION, 100);

				SeekBar sbar = null;
				switch (direction) {
					case 0:
						sbar = m_axisX1;
						break;
					case 1:
						sbar = m_axisY1;
						break;
					case 2:
						sbar = m_axisX2;
						break;
					case 3:
						sbar = m_axisY2;
						break;
				}
				
				if (sbar != null) {
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
		m_logAdapter.add(entry);
		while (m_logAdapter.getCount() > 50)
			m_logAdapter.remove(m_logAdapter.getItem(0));
	}

}