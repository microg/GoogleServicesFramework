package com.google.android.gtalkservice;

interface IJingleInfoStanzaListener {
	void onStanzaReceived(String s);
	long getAccountId();
}