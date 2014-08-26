package com.google.android.gsf.gtalkservice.service;

import android.os.RemoteException;
import android.util.Log;
import com.google.android.gtalkservice.IGTalkConnection;
import com.google.android.gtalkservice.IGTalkConnectionListener;
import com.google.android.gtalkservice.IGTalkService;
import com.google.android.gtalkservice.IImSession;

import java.util.List;

public class GTalkServiceBinder extends IGTalkService.Stub {

	private static final String TAG = "GoogleTalkServiceBinder";

	private final GTalkService service;

	public GTalkServiceBinder(final GTalkService service) {
		this.service = service;
	}

	@Override
	public void createGTalkConnection(final String username, final IGTalkConnectionListener listener)
			throws RemoteException {
		service.createGTalkConnection(username, listener);
	}

	@Override
	public void dismissAllNotifications() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IGTalkService.dismissAllNotifications");
	}

	@Override
	public void dismissNotificationFor(final String s, final long l) throws RemoteException {
		service.dismissNotificationFor(s, l);
	}

	@Override
	public void dismissNotificationsForAccount(final long l) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IGTalkService.dismissNotificationsForAccount(" + l + ")");
	}

	@Override
	public List<IGTalkConnection> getActiveConnections() throws RemoteException {
		return service.getActiveConnections();
	}

	@Override
	public IGTalkConnection getConnectionForUser(final String s) throws RemoteException {
		return service.getConnectionForUser(s);
	}

	@Override
	public IGTalkConnection getDefaultConnection() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IGTalkService.getDefaultConnection");
		return null;
	}

	@Override
	public boolean getDeviceStorageLow() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IGTalkService.getDeviceStorageLow");
		return false;
	}

	@Override
	public IImSession getImSessionForAccountId(final long l) throws RemoteException {
		try {
			return service.getImSessionForAccountId(l);
		} catch (final RuntimeException e) {
			Log.w(TAG, e);
			throw e;
		}
	}

	@Override
	public String printDiagnostics() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IGTalkService.printDiagnostics");
		return null;
	}

	@Override
	public void setTalkForegroundState() throws RemoteException {
		service.setTalkForegroundState();
	}
}