package com.hexad.bluezime;

import java.lang.reflect.Method;

import android.os.IBinder;
import android.util.Log;


//Mock implementation of ServiceManager
public class ServiceManager {

	private static boolean D = true;
	private static final String LOG_NAME = "FakeServiceManager";

	private static final Method METHOD;
	
	static {
		if (D) Log.i(LOG_NAME, "Probing for ServiceManager");
		
		Method m = null;
		try {
			Class<?> cls = Class.forName("android.os.ServiceManager");
			if (D) Log.i(LOG_NAME, "Probing for getService");
	        m = cls.getDeclaredMethod("getService", String.class);
	        if (D) Log.i(LOG_NAME, "Fixing permission issues");
	        m.setAccessible(true);
	        if (D) Log.i(LOG_NAME, "All good, we have a manager");
		} catch (Throwable t) {
			Log.e(LOG_NAME, "Error fetching real ServiceManager" + t.getMessage());
			if (D) t.printStackTrace();
		}

		METHOD = m;
	}
	
	
	public static IBinder getService(String name) {
		try {
			if (D) Log.i(LOG_NAME, "Fetching service: " + name); 
			return (IBinder)METHOD.invoke(null, name);
		} catch (Throwable e) {
			Log.e(LOG_NAME, "Failed to get service: " + e.getMessage());
			if (D) e.printStackTrace();
		}
		
		return null;
	}
}
