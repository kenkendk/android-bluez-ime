package com.hexad.bluezime;

public interface BluezDriverInterface extends Runnable {
	void stop();
	String getDeviceAddress();
	String getDeviceName();
	String getDriverName();
}
