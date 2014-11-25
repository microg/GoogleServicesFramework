package com.google.android.gsf.settings;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import com.google.android.gsf.GoogleSettingsContract;

public class UseLocationForServicesActivity extends Activity {
	protected void onResume() {
		super.onResume();
		setUseLocationForServices(this, !getIntent().getBooleanExtra("disable", false));
		setResult(-1);
		finish();
	}

    public static boolean setUseLocationForServices(Context context, boolean enable) {
        ContentResolver contentresolver = context.getContentResolver();
        boolean result = GoogleSettingsContract.Partner.putInt(contentresolver, "use_location_for_services", (enable ? 1 : 0));
        context.sendBroadcast(new Intent("com.google.android.gsf.settings.GoogleLocationSettings.UPDATE_LOCATION_SETTINGS"));
        return result;
    }
}
