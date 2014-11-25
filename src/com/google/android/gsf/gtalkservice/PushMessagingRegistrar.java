package com.google.android.gsf.gtalkservice;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.location.GeofenceHardware;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import com.google.android.AndroidContext;
import com.google.c2dm.C2DMClient;
import com.google.android.gsf.PrivacyExtension;
import com.google.tools.SignatureTools;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PushMessagingRegistrar extends IntentService {

	private static final String TAG = "PushMessagingRegistrar";

	public PushMessagingRegistrar() {
		super(TAG);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent: " + intent);
		try {
			if (intent != null && intent.getAction() != null) {
				if (intent.getAction().equalsIgnoreCase("com.google.android.c2dm.intent.REGISTER")) {
					register(intent);
				} else if (intent.getAction().equalsIgnoreCase("com.google.android.c2dm.intent.UNREGISTER")) {
					unregister(intent);
				}
			}
		} catch (Exception e) {
			Log.w(TAG, e);
		}
	}

	private void unregister(Intent intent) {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: PushMessagingRegistrar.unregister: " + intent);

	}

	private static Map<String, String> mapFromBundle(Bundle bundle) {
		HashMap<String, String> result = new HashMap<String, String>();
		for (String key : bundle.keySet()) {
			Log.d(TAG, "extra:: " + key + "=" + bundle.get(key));
			if (!key.equalsIgnoreCase("sender") && !key.equalsIgnoreCase("app")) {
				result.put(key, bundle.get(key).toString());
			}
		}
		return result;
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private String packageFromPendingIntent(PendingIntent pi) {
		if (Build.VERSION.SDK_INT < 17) {
			return pi.getTargetPackage();
		} else {
			return pi.getCreatorPackage();
		}
	}

	private void register(Intent intent) throws IOException {
		Log.d(TAG, "register: " + intent);
        Intent outIntent = new Intent("com.google.android.c2dm.intent.REGISTRATION");
		PendingIntent pendingIntent = intent.getParcelableExtra("app");
		String sender = intent.getStringExtra("sender");
		String app = packageFromPendingIntent(pendingIntent);
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType("com.google");
        String token = null;
        if (accounts.length>=1) {
            try {
                token = am.blockingGetAuthToken(accounts[0], "ac2dm", true);
            } catch (Exception e) {
            }
        }
        if (token != null) {
            AndroidContext info = PrivacyExtension.getAndroidContext(this);
            info.set(AndroidContext.KEY_AUTHORIZATION_TOKEN, token);
            Log.d(TAG, app + ", " + getSignatureFingerprint(app) + ", " + sender);
            String regId =
                    new C2DMClient(info).registerC2DM(app, getSignatureFingerprint(app), sender, mapFromBundle(intent.getExtras()));
            outIntent.setPackage(app);
            if (regId != null) {
                Log.d(TAG, "Success: regId for " + app + " is " + regId);
                outIntent.putExtra("registration_id", regId);
            } else {
                Log.d(TAG, "Error: no regId for " + app + "!");
                outIntent.putExtra("error", "SERVICE_NOT_AVAILABLE");
            }
        } else {
            outIntent.putExtra("error", "CANT_LOGIN");
        }
		sendOrderedBroadcast(outIntent, null);
	}

	private String getSignatureFingerprint(String s) {
		PackageManager pm = getPackageManager();
		try {
			if (pm.getApplicationInfo(s, 0) == null) {
				return null;
			}
			PackageInfo packageinfo = pm.getPackageInfo(s, PackageManager.GET_SIGNATURES);
			if (packageinfo == null || packageinfo.signatures == null || packageinfo.signatures.length == 0 ||
				packageinfo.signatures[0] == null) {
				return null;
			}
			return SignatureTools.signatureDigest(packageinfo.signatures[0].toByteArray());
		} catch (Exception e) {
			return null;
		}
	}
}
