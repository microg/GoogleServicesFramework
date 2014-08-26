package com.google.android.gtalkservice;

import com.google.android.gtalkservice.IHttpRequestCallback;
import com.google.android.gtalkservice.IImSession;

interface IGTalkConnection{
	String getUsername();
	String getJid();
	String getDeviceId();
	boolean isConnected();
	IImSession createImSession();
	IImSession getImSessionForAccountId(long l);
	IImSession getDefaultImSession();
	long getLastActivityFromServerTime();
	long getLastActivityToServerTime();
	int getNumberOfConnectionsMade();
	int getNumberOfConnectionsAttempted();
	int getConnectionUptime();
	void clearConnectionStatistics();
	void sendHttpRequest(in byte[] bytes, IHttpRequestCallback httprequestcallback);
	void sendHeartbeat();
}