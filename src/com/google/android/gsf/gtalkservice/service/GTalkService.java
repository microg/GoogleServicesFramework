package com.google.android.gsf.gtalkservice.service;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gsf.talk.TalkProvider;
import com.google.android.gtalkservice.IGTalkConnection;
import com.google.android.gtalkservice.IGTalkConnectionListener;
import com.google.android.gtalkservice.IGTalkService;
import com.google.android.gtalkservice.IImSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GTalkService extends Service {

	private final static String TAG = "GoogleTalkService";

	private final Map<Long, Account> accounts = new HashMap<Long, Account>();
	private final IGTalkService.Stub binder = new GTalkServiceBinder(this);
	private final Map<String, IGTalkConnection> gTalkConnections = new HashMap<String, IGTalkConnection>();
	private final Thread talkForeground = new Thread(new Runnable() {

		@Override
		public void run() {
			boolean localTalkIsForeground = talkIsForeground;
			while (true) {
				synchronized (talkForegroundLock) {
					try {
						talkForegroundLock.wait(30000);
					} catch (final InterruptedException e) {
						return;
					}
					if (talkIsForeground) {
						talkIsForeground = false;
						localTalkIsForeground = true;
					}
				}
				for (final IGTalkConnection connection : gTalkConnections.values()) {
					final GTalkConnection c = (GTalkConnection) connection;
					c.setTalkForegroundState(localTalkIsForeground);
				}
				localTalkIsForeground = false;
			}
		}
	});
	private final Object talkForegroundLock = new Object();
	private boolean talkIsForeground = false;
	private Looper workerLooper;

	private Account createAccount(String username) {
		if (!username.contains(":")) {
			username = "com.google:" + username;
		}
		final Account account = Account.getAccount(username, this);
		accounts.put(account.getId(), account);
		return account;
	}

	private IGTalkConnection createGTalkConnection(final String username, final boolean autoLogin) {
		final IGTalkConnection connection = new GTalkConnection(this, createAccount(username), workerLooper, autoLogin);
		gTalkConnections.put(username, connection);
		return connection;
	}

	public void createGTalkConnection(final String username, final IGTalkConnectionListener listener) {
		try {
			final IGTalkConnection connection = createGTalkConnection(username, false);
			listener.onConnectionCreated(connection);
		} catch (final Throwable e) {
			Log.w(GTalkService.TAG, e);
			try {
				listener.onConnectionCreationFailed(username);
			} catch (final Exception e2) {
			}
		}
	}

	public void dismissNotificationFor(final String s, final long l) {
		((GTalkConnection) getConnectionForAccountId(l)).getNotifier().dismissChatMessages(s);
	}

	public List<IGTalkConnection> getActiveConnections() {
		final List<IGTalkConnection> list = new ArrayList<IGTalkConnection>();
		for (final IGTalkConnection con : gTalkConnections.values()) {
			list.add(con);
		}
		return list;
	}

	public IGTalkConnection getConnectionForAccountId(final long l) {
		final String user = getUserForAccountId(l);
		return getConnectionForUser(user);
	}

	public IGTalkConnection getConnectionForUser(final String username) {
		IGTalkConnection connection = null;
		synchronized (gTalkConnections) {
			connection = gTalkConnections.get(username);
		}
		if (connection == null) {
			connection = createGTalkConnection(username, true);
		}
		return connection;
	}

	public IImSession getImSessionForAccountId(final long l) throws RemoteException {
		final IGTalkConnection connection = getConnectionForAccountId(l);
		return connection.getImSessionForAccountId(l);
	}

	public String getUserForAccountId(final long id) {
		final Account account = accounts.get(id);
		if (account == null) {
			final Cursor c = getContentResolver()
					.query(TalkProvider.ACCOUNTS_URI, new String[]{"username"}, "_id=?", new String[]{id + ""}, null);
			if (c.moveToFirst()) {
				final String username = c.getString(0);
				c.close();
				return username;
			} else {
				c.close();
				return null;
			}
		}
		return account.getUsername();
	}

	@Override
	public IBinder onBind(final Intent intent) {
		if (IGTalkService.class.getName().equals(intent.getAction())) {
			return binder;
		}
		return null;
	}

	@Override
	public void onCreate() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				Looper.prepare();
				workerLooper = Looper.myLooper();
				Process.setThreadPriority(10);
				Looper.loop();
			}
		}).start();
		talkForeground.start();
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		workerLooper.quit();
		talkForeground.interrupt();
		Log.w(TAG, "Not yet implemented: GTalkService.onDestroy");
		super.onDestroy();

	}

	public void setTalkForegroundState() {
		synchronized (talkForegroundLock) {
			talkIsForeground = true;
			talkForegroundLock.notify();
		}
	}
}
