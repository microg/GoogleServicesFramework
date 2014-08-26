package com.google.android.gsf.gtalkservice.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gsf.talk.TalkProvider;
import com.google.android.gtalkservice.*;
import com.google.android.gtalkservice.Presence.Show;

import java.util.HashMap;
import java.util.Map;

public class GTalkConnection extends IGTalkConnection.Stub {

	private final static String RESOURCE = "androidv2";
	private static final String TAG = "GoogleTalkConnection";
	private final static int XMPP_PORT = 5222;

	private final Account account;
	private final BackendHandler backendHandler;
	private final Map<String, IChatSession> chatSessions = new HashMap<String, IChatSession>();
	private ConnectionError connectionError = new ConnectionError(ConnectionError.NO_ERROR);
	private ConnectionState connectionState = new ConnectionState(ConnectionState.IDLE);
	private final ImSession imSession;
	private final Looper looper;
	private final Notifier notifier;
	private Presence presence = new Presence();

	private final GTalkService service;

	public GTalkConnection(final GTalkService service, final Account account, final Looper looper,
						   final boolean autoLogin) {
		Log.d(TAG, "GTalkConnection.ctor(" + service + ", " + account + ", " + looper + ")");
		this.service = service;
		this.account = account;
		this.looper = looper;
		notifier = new Notifier(this);
		imSession = new ImSession(this);
		readAccount();
        backendHandler = null;
		/*backendHandler = new SmackHandler(this.looper, this);
		if (autoLogin) {
			backendHandler.connectAndlogin();
		} else {
			backendHandler.connect();
		}*/
	}

	public void addChatSession(final String user, final IChatSession chatSession) {
		String useri = user;
		if (useri.contains("/")) {
			useri = useri.split("/")[0];
		}
		synchronized (chatSessions) {
			chatSessions.put(useri, chatSession);
		}
	}

	@Override
	public void clearConnectionStatistics() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GTalkConnection.clearConnectionStatistics");

	}

	public void closeAllChats() {
		synchronized (chatSessions) {
			for (final IChatSession chatSession : chatSessions.values()) {
				try {
					chatSession.leave();
				} catch (final RemoteException e) {
					// never happens as chatsessions are not remote...
				}
			}
		}
	}

	@Override
	public IImSession createImSession() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GTalkConnection.createImSession");
		return null;
	}

	public Account getAccount() {
		return account;
	}

	public long getAccountId() {
		return account.getId();
	}

	public String getAuth() {
		return account.getAuth();
	}

	public BackendHandler getBackend() {
		return backendHandler;
	}

	public IChatSession getChatSession(final String user, final boolean create) {
		String useri = user;
		if (useri.contains("/")) {
			useri = useri.split("/")[0];
		}
		synchronized (chatSessions) {
			if (chatSessions.containsKey(useri)) {
				Log.d(TAG, "GTalkConnection.getChatSession(" + user + ", " + create + ")=" + chatSessions.get(useri));
				return chatSessions.get(useri);
			} else if (create) {
				//final IChatSession session = new BackendChatSession(this, user);
                final IChatSession session = null;
				chatSessions.put(useri, session);
				Log.d(TAG, "GTalkConnection.getChatSession(" + user + ", " + create + ")=" + session);
				return session;
			} else {
				Log.d(TAG, "GTalkConnection.getChatSession(" + user + ", " + create + ")=null");
				return null;
			}
		}
	}

	public ConnectionError getConnectionError() {
		return connectionError;
	}

	public ConnectionState getConnectionState() {
		return connectionState;
	}

	@Override
	public int getConnectionUptime() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GTalkConnection.getConnectionUptime");
		return 0;
	}

	public Context getContext() {
		return service;
	}

	@Override
	public IImSession getDefaultImSession() throws RemoteException {
		return imSession;
	}

	@Override
	public String getDeviceId() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GTalkConnection.getDeviceId");
		return "";
	}

	public ImSession getImSession() {
		return imSession;
	}

	@Override
	public IImSession getImSessionForAccountId(final long l) throws RemoteException {
		if (l == account.getId()) {
			Log.d(TAG, "IGTalkConnection.getImSessionForAccountId(" + l + ")=" + imSession);
			return imSession;
		} else {
			Log.d(TAG, "IGTalkConnection.getImSessionForAccountId(" + l + ")=null");
			return null;
		}
	}

	@Override
	public String getJid() throws RemoteException {
		return account.getJid();
	}

	@Override
	public long getLastActivityFromServerTime() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GTalkConnection.getLastActivityFromServerTime");
		return 0;
	}

	@Override
	public long getLastActivityToServerTime() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GTalkConnection.getLastActivityToServerTime");
		return 0;
	}

	public Notifier getNotifier() {
		return notifier;
	}

	@Override
	public int getNumberOfConnectionsAttempted() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GTalkConnection.getNumberOfConnectionsAttempted");
		return 0;
	}

	@Override
	public int getNumberOfConnectionsMade() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GTalkConnection.getNumberOfConnectionsMade");
		return 0;
	}

	public Presence getPresence() {
		return presence;
	}

	public String getResource() {
		if (getBackend() != null && getBackend().getXmpp() != null && getBackend().getXmpp().getUser() != null &&
			getBackend().getXmpp().getUser().contains("/")) {
			return getBackend().getXmpp().getUser().split("/")[1];
		}
		return RESOURCE;
	}

	public String getServiceHost() {
		return account.getServiceHost();
	}

	public String getServiceName() {
		Log.d(TAG, "GTalkConnection.getServiceName=" + account.getServiceName());
		return account.getServiceName();
	}

	public int getServicePort() {
		return XMPP_PORT;
	}

	@Override
	public String getUsername() throws RemoteException {
		return account.getUsername();
	}

	@Override
	public boolean isConnected() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GTalkConnection.isConnected");
		return false;
	}

	public void onChatSessionAvailable(final String user, final IChatSession chatSession) {
		imSession.onChatSessionAvailable(user, chatSession);
	}

	public void onPresenceChanged(final String user) {
		imSession.onPresenceChanged(user);
	}

	public void onRosterChanged() {
		imSession.onRosterChanged();
	}

	public void onSelfPresenceChanged() {
		imSession.onSelfPresenceChanged();
		saveAccount();
		saveAccountStatus();
	}

	public void readAccount() {
		final Cursor c = getContext().getContentResolver()
				.query(TalkProvider.ACCOUNTS_URI, new String[]{"last_login_state"}, "username LIKE ?",
					   new String[]{getAccount().getUsername()}, null);
		if (c.moveToFirst()) {
			presence = new Presence(c.getInt(0));
		}
		c.close();
	}

	public void saveAccount() {
		final ContentValues values = new ContentValues();
		values.put("name", getAccount().getUsername());
		values.put("username", getAccount().getJid());
		values.put("last_login_state", getPresence().getNumeric());
		getContext().getContentResolver().insert(TalkProvider.ACCOUNTS_URI, values);
	}

	public void saveAccountStatus() {
		final ContentValues values = new ContentValues();
		values.put("account", getAccountId());
		values.put("presenceStatus", getPresence().getNumeric());
		values.put("connStatus", getConnectionState().getState());
		getContext().getContentResolver().insert(TalkProvider.ACCOUNT_STATUS_URI, values);
	}

	@Override
	public void sendHeartbeat() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GTalkConnection.sendHeartbeat");

	}

	@Override
	public void sendHttpRequest(final byte[] bytes, final IHttpRequestCallback httprequestcallback)
			throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: GTalkConnection.sendHttpRequest");

	}

	public void setConnectionError(final ConnectionError connectionError) {
		Log.d(TAG, "GTalkConnection.setConnectionError(" + connectionError + ")");
		this.connectionError = connectionError;
		imSession.onConnectionStateChanged();
	}

	public void setConnectionError(final int connectionError) {
		Log.d(TAG, "GTalkConnection.setConnectionError(" + connectionError + ")");
		setConnectionError(new ConnectionError(connectionError));
	}

	public void setConnectionState(final ConnectionState connectionState) {
		this.connectionState = connectionState;
		imSession.onConnectionStateChanged();
		saveAccountStatus();
	}

	public void setConnectionState(final int connectionState) {
		setConnectionState(new ConnectionState(connectionState));
	}

	public void setPresence(final Presence presence) {
		backendHandler.presence(presence);
		this.presence = presence;
	}

	public void setTalkForegroundState(final boolean talkIsForeground) {
		if (presence.isAvailable() && !presence.isInvisible()) {
			if (talkIsForeground && presence.getShow() == Show.AWAY) {
				presence.setShow(Show.AVAILABLE);
				setPresence(presence);
			} else if (!talkIsForeground && presence.getShow() == Show.AVAILABLE) {
				presence.setShow(Show.AWAY);
				setPresence(presence);
			}
		}
	}
}
