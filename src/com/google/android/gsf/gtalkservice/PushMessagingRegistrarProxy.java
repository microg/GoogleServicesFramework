package com.google.android.gsf.gtalkservice;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class PushMessagingRegistrarProxy extends IntentService {

	private static final String TAG = "PushMessagingRegistrarProxy";

	public PushMessagingRegistrarProxy() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent: " + intent);
		intent.setClass(this, PushMessagingRegistrar.class);
		startService(intent);
	}

}
