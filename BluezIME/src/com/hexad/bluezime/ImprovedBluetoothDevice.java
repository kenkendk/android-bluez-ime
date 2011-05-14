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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Parcel;
import android.os.ParcelUuid;

//Class that mimics a regular android.bluetooth.BluetoothDevice,
// but exposes some of the internal methods as regular methods

public class ImprovedBluetoothDevice {
	public final BluetoothDevice mDevice;
	
	@SuppressWarnings("unchecked")
	private static Method getMethod(Class cls, String name, Class[] args) {
		try {
			return cls.getMethod(name, args);
		} catch (Exception ex) {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	private static Constructor getConstructor(Class cls, Class[] args) {
		try {
			Constructor c = cls.getDeclaredConstructor(args);
			if (!c.isAccessible())
				c.setAccessible(true);
			return c;
		} catch (Exception ex) {
			return null;
		}
	}
	
	private static final int TYPE_RFCOMM = 1;
	private static final int TYPE_SCO = 2;
	private static final int TYPE_L2CAP = 3;
	
	private static final Method _createRfcommSocket = getMethod(BluetoothDevice.class, "createRfcommSocket", new Class[] { int.class });
	private static final Method _createInsecureRfcommSocket = getMethod(BluetoothDevice.class, "createInsecureRfcommSocket", new Class[] { int.class });
	private static final Method _setPin = getMethod(BluetoothDevice.class, "setPin", new Class[] { byte[].class });
	private static final Method _setPasskey = getMethod(BluetoothDevice.class, "setPasskey", new Class[] { int.class });
	private static final Constructor _socketConstructor = getConstructor(BluetoothSocket.class, new Class[] {int.class, int.class, boolean.class, boolean.class, BluetoothDevice.class, int.class, ParcelUuid.class});
	
	public ImprovedBluetoothDevice(BluetoothDevice base) {
		if (base == null)
			throw new NullPointerException();
		
		mDevice = base;
	}
	
	public BluetoothSocket createRfcommSocketToServiceRecord(UUID uuid) throws IOException {
		return mDevice.createRfcommSocketToServiceRecord(uuid);
	}
	
	public int describeContents() {
		return mDevice.describeContents();
	}
	
	public String getAddress() {
		return mDevice.getAddress();
	}
	
	public BluetoothClass getBluetoothClass() {
		return mDevice.getBluetoothClass();
	}
	
	public int getBondState() {
		return mDevice.getBondState();
	}
	
	public String getName() {
		return mDevice.getName();
	}
	
	public String toString() {
		return mDevice.toString();
	}
	
	public void writeToParcel(Parcel out, int flags) {
		mDevice.writeToParcel(out, flags);
	}
	
	public BluetoothSocket createRfcommSocket(int channel) throws Exception {
		if (_createRfcommSocket == null) 
			throw new NoSuchMethodException("createRfcommSocket");
		try {
			return (BluetoothSocket)_createRfcommSocket.invoke(mDevice, channel);
		} catch (InvocationTargetException tex) {
			if (tex.getCause() instanceof Exception)
				throw (Exception)tex.getCause();
			else
				throw tex;
		}
	}

	public BluetoothSocket createInsecureRfcommSocket(int channel) throws Exception {
		if (_createInsecureRfcommSocket == null) 
			throw new NoSuchMethodException("createInsecureRfcommSocket");
		
		try {
			return (BluetoothSocket)_createInsecureRfcommSocket.invoke(mDevice, channel);
		} catch (InvocationTargetException tex) {
			if (tex.getCause() instanceof Exception)
				throw (Exception)tex.getCause();
			else
				throw tex;
		}
	}
	
	public BluetoothSocket createLCAPSocket(int channel) throws Exception {
		if (_socketConstructor == null)
			throw new NoSuchMethodException("new BluetoothSocket");
		
		try {
			return (BluetoothSocket)_socketConstructor.newInstance(TYPE_L2CAP, -1, true, true, mDevice, channel, null);
		} catch (InvocationTargetException tex) {
			if (tex.getCause() instanceof Exception)
				throw (Exception)tex.getCause();
			else
				throw tex;
		}
	}

	public BluetoothSocket createInsecureLCAPSocket(int channel) throws Exception {
		if (_socketConstructor == null)
			throw new NoSuchMethodException("new BluetoothSocket");
		
		try {
			return (BluetoothSocket)_socketConstructor.newInstance(TYPE_L2CAP, -1, false, false, mDevice, channel, null);
		} catch (InvocationTargetException tex) {
			if (tex.getCause() instanceof Exception)
				throw (Exception)tex.getCause();
			else
				throw tex;
		}
	}
	
	public boolean setPin(byte[] pin) throws Exception {
		if (_setPin == null)
			throw new NoSuchMethodException("setPin");

		try {
			return (Boolean)_setPin.invoke(mDevice, pin);
		} catch (InvocationTargetException tex) {
			if (tex.getCause() instanceof Exception)
				throw (Exception)tex.getCause();
			else
				throw tex;
		}
	}
	
	public boolean setPasskey(int passkey) throws Exception {
		if (_setPasskey == null)
			throw new NoSuchMethodException("setPasskey");

		try {
			return (Boolean)_setPasskey.invoke(mDevice, passkey);
		} catch (InvocationTargetException tex) {
			if (tex.getCause() instanceof Exception)
				throw (Exception)tex.getCause();
			else
				throw tex;
		}
	}
}
