package com.google.android.gtalkservice;

interface IRosterListener {
	void rosterChanged();
	void presenceChanged(String s);
	void selfPresenceChanged();
}