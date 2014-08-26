package com.google.android.gsf.settings;

import android.app.Activity;
import com.google.android.gsf.UseLocationForServices;

public class UseLocationForServicesActivity extends Activity {
	protected void onResume() {
		super.onResume();
		UseLocationForServices.setUseLocationForServices(this, !getIntent().getBooleanExtra("disable", false));
		setResult(-1);
		finish();
	}
}
