package com.google.android.gtalkservice;

interface ISessionStanzaListener {
	void onStanzaReceived(String s, String s1);
	void onStanzaResponse(String s, String s1, String s2);
	long getAccountId();
}