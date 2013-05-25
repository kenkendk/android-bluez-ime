package com.hexad.bluezime;

//We use a fake ServiceManager
//import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyEvent;

public class KeyPressServer {

	private static final String LOG_NAME = "KeyPressServer";
	private static final boolean D = true;
	
	public static void main(String[] args) {
		if (D) Log.i(LOG_NAME, "Started");
		
		new Thread(
			new Runnable() {
				@Override
				public void run() {
					
					for(int i = 0; i < 100; i++) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (D) Log.i(LOG_NAME, "Sending");
						sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN + "");
						if (D) Log.i(LOG_NAME, "Done");
					}
				}
			}
		).start();
		if (D) Log.i(LOG_NAME, "Done");
	}
	
	private static void sendKeyEvent(String event) {
		int eventCode = Integer.parseInt(event);
		long now = SystemClock.uptimeMillis();
	        	        
		if(D) Log.i("SendKeyEvent", event);
	    try {
	        KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, eventCode, 0);
	        KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, eventCode, 0);
	        IWindowManager m = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
	            m.injectKeyEvent(down, false);
	            m.injectKeyEvent(up, false);
	    		if(D) Log.i("Key was SENT!", event);
	    } catch (Throwable e) {
	        Log.e(LOG_NAME, e.getMessage());
	        if (D) e.printStackTrace();
	    }
	}
}
