package com.hexad.bluezime;

import android.content.Context;
import android.view.KeyEvent;

public class ZeemoteReader extends BluetoothReader {
	
	//These are from API level 9
	public static final int KEYCODE_BUTTON_A = 0x60;
	public static final int KEYCODE_BUTTON_B = 0x61;
	public static final int KEYCODE_BUTTON_C = 0x62;
	public static final int KEYCODE_BUTTON_X = 0x63;
	
	public static final String DRIVER_NAME = "zeemote";
	
	private final byte BUTTON_UPDATE = 0x07;
	private final byte DIRECTION_UPDATE = 0x08;
	private final byte MAGIC_NUMBER = (byte)0xA1;

	private boolean[] m_buttons = new boolean[0];
	private int[] m_directions = new int[0];

	public ZeemoteReader(String address, Context context) throws Exception {
		super(address, context);
	}
	
	@Override
	public String getDriverName() {
		return DRIVER_NAME;
	}
	
	@Override
	protected int parseInputData(byte[] data, int read) {
	
		int offset = 0;
		int remaining = read;
		
		//This should always be true
		while (remaining > 3 && remaining >= data[offset + 0] + 1 && data[offset + 1] == MAGIC_NUMBER) {
			int consumed = data[offset + 0] + 1;
			remaining -= consumed;
			
			if (data[offset + 2] == BUTTON_UPDATE) {

				boolean[] buttons = new boolean[consumed - 3];
				for(int i = 3; i < consumed; i++)
					if (data[offset + i] < buttons.length && data[offset + i] >= 0)
						buttons[data[offset + i]] = true;
				
				for(int i = 0; i < Math.min(buttons.length, m_buttons.length); i++)
					if (m_buttons[i] != buttons[i])
					{
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_ACTION, buttons[i] ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP);
						keypressBroadcast.putExtra(BluezService.EVENT_KEYPRESS_KEY, (i < 6 ? KEYCODE_BUTTON_A : KeyEvent.KEYCODE_A) + i);
						m_context.sendBroadcast(keypressBroadcast);
					}
				
				m_buttons = buttons;
			} else if (data[offset + 2] == DIRECTION_UPDATE) {
				int[] directions = new int[consumed - 4];
				for(int i = 4; i < consumed; i++)
					directions[i - 4] = data[offset + i];

				for(int i = 0; i < Math.min(directions.length, m_directions.length); i++)
					if (m_directions[i] != directions[i]) {
						directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_DIRECTION, i);
						directionBroadcast.putExtra(BluezService.EVENT_DIRECTIONALCHANGE_VALUE, directions[i]);
						m_context.sendBroadcast(directionBroadcast);
					}
						
				m_directions = directions;
			}
		}
		
		return remaining;
	}
	
	@Override
	protected void validateWelcomeMessage(byte[] data, int read) {
		//TODO: Find some documentation that explains how to parse the message
	}


}
