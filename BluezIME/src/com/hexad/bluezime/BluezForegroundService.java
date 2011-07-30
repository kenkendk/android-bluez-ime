package com.hexad.bluezime;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class BluezForegroundService extends Service {

	private static final int NOTIFICATION_ID = 10;
	private NotificationManager notificationManager;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (notificationManager != null) return START_NOT_STICKY;
		
		notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		PendingIntent settingsIntent = PendingIntent.getActivity(this, 0, new Intent(this, BluezIMESettings.class), 0);

		Notification notification = new Notification(R.drawable.icon, null, System.currentTimeMillis());
		notification.setLatestEventInfo(getApplicationContext(), getResources().getString(R.string.app_name), getResources().getString(R.string.notification_text), settingsIntent);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;

		notificationManager.notify(NOTIFICATION_ID, notification);
		startForeground(NOTIFICATION_ID, notification);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		notificationManager.cancel(NOTIFICATION_ID);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
