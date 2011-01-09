package com.hexad.bluezime;

import android.content.Context;
import android.util.Log;

public class DataDumpReader extends BluetoothReader {

	private static final String LOG_NAME = "DataDumpReader";
	public static final String DRIVER_NAME = "dump";
	
	public DataDumpReader(String address, Context context) throws Exception {
		super(address, context);
	}

	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}

	@Override
	protected int parseInputData(byte[] data, int read) {
		Log.i(LOG_NAME, "Read data: " + getHexString(data, 0, read));
		return 0;
	}

	@Override
	protected void validateWelcomeMessage(byte[] data, int read) {
		Log.i(LOG_NAME, "Welcome message is: " + getHexString(data, 0, read));
	}

}
