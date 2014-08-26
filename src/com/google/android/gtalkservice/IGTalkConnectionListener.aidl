package com.google.android.gtalkservice;

import com.google.android.gtalkservice.IGTalkConnection;

interface IGTalkConnectionListener {
	void onConnectionCreated(IGTalkConnection igtalkconnection);
	void onConnectionCreationFailed(String s);
}