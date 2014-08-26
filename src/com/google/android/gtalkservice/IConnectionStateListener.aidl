package com.google.android.gtalkservice;

import com.google.android.gtalkservice.ConnectionState;
import com.google.android.gtalkservice.ConnectionError;

interface IConnectionStateListener {
	void connectionStateChanged(in ConnectionState connectionstate, in ConnectionError connectionerror, long l, String s);
}