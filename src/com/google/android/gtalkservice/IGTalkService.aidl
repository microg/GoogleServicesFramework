package com.google.android.gtalkservice;

import com.google.android.gtalkservice.IGTalkConnectionListener;
import com.google.android.gtalkservice.IGTalkConnection;
import com.google.android.gtalkservice.IImSession;

interface IGTalkService {
	void createGTalkConnection(String s, IGTalkConnectionListener igtalkconnectionlistener);
	List getActiveConnections();
	IGTalkConnection getConnectionForUser(String s);
	IGTalkConnection getDefaultConnection();
	IImSession getImSessionForAccountId(long l);
	void dismissAllNotifications();
	void dismissNotificationsForAccount(long l);
	void dismissNotificationFor(String s, long l);
	boolean getDeviceStorageLow();
	String printDiagnostics();
	void setTalkForegroundState();
}