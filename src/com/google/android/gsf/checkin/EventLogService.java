package com.google.android.gsf.checkin;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class EventLogService extends Service {

	private static final String TAG = "EventLogService";

	public static class Receiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Log.w(TAG, "Not yet implemented: BroadcastReceiver.onReceive");

		}

	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: EventLogService.onBind");
		return null;
	}

}
