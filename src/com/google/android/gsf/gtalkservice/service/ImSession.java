package com.google.android.gsf.gtalkservice.service;

import android.graphics.Bitmap;
import android.os.RemoteException;
import android.util.Log;
import com.google.android.gtalkservice.*;

import java.util.ArrayList;
import java.util.List;

public class ImSession extends IImSession.Stub {

	private final static String TAG = "GoogleImSession";
	private final List<IChatListener> chatListeners = new ArrayList<IChatListener>();
	private final GTalkConnection connection;
	private final List<IConnectionStateListener> connectionStateListeners = new ArrayList<IConnectionStateListener>();
	private final List<IGroupChatInvitationListener> groupChatInvitationListeners =
			new ArrayList<IGroupChatInvitationListener>();
	private final List<IRosterListener> rosterListeners = new ArrayList<IRosterListener>();

	public ImSession(final GTalkConnection connection) {
		Log.w(TAG, "Not yet implemented: IImSession.ctor");
		this.connection = connection;
	}

	@Override
	public void addConnectionStateListener(final IConnectionStateListener listener) throws RemoteException {
		Log.d(TAG, "IImSession.addConnectionStateListener(" + listener + ")");
		connectionStateListeners.add(listener);
	}

	@Override
	public void addContact(final String s, final String s1, final String[] as) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.addContact");

	}

	@Override
	public void addGroupChatInvitationListener(final IGroupChatInvitationListener listener) throws RemoteException {
		Log.d(TAG, "IImSession.addGroupChatInvitationListener(" + listener + ")");
		groupChatInvitationListeners.add(listener);
	}

	@Override
	public void addRemoteChatListener(final IChatListener listener) throws RemoteException {
		Log.d(TAG, "IImSession.addRemoteChatListener(" + listener + ")");
		chatListeners.add(listener);
	}

	@Override
	public void addRemoteJingleInfoStanzaListener(final IJingleInfoStanzaListener ijingleinfostanzalistener)
			throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.addRemoteJingleInfoStanzaListener");

	}

	@Override
	public void addRemoteRosterListener(final IRosterListener listener) throws RemoteException {
		Log.d(TAG, "IImSession.addRemoteRosterListener(" + listener + ")");
		rosterListeners.add(listener);
	}

	@Override
	public void addRemoteSessionStanzaListener(final ISessionStanzaListener isessionstanzalistener)
			throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.addRemoteSessionStanzaListener");

	}

	@Override
	public void approveSubscriptionRequest(final String s, final String s1, final String[] as) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.approveSubscriptionRequest");

	}

	@Override
	public void blockContact(final String s) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.blockContact");

	}

	@Override
	public void clearContactFlags(final String s) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.clearContactFlags");

	}

	@Override
	public void closeAllChatSessions() throws RemoteException {
		connection.closeAllChats();
	}

	@Override
	public IChatSession createChatSession(final String user) throws RemoteException {
		return connection.getChatSession(user, true);
	}

	@Override
	public void createGroupChatSession(final String s, final String[] as) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.createGroupChatSession");

	}

	@Override
	public void declineGroupChatInvitation(final String s, final String s1) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.declineGroupChatInvitation");

	}

	@Override
	public void declineSubscriptionRequest(final String s) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.declineSubscriptionRequest");

	}

	@Override
	public void editContact(final String s, final String s1, final String[] as) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.editContact");

	}

	@Override
	public long getAccountId() throws RemoteException {
		return connection.getAccountId();
	}

	@Override
	public IChatSession getChatSession(final String user) throws RemoteException {
		return connection.getChatSession(user, false);
	}

	@Override
	public ConnectionState getConnectionState() throws RemoteException {
		return connection.getConnectionState();
	}

	@Override
	public String getJid() throws RemoteException {
		return connection.getJid();
	}

	@Override
	public Presence getPresence() throws RemoteException {
		return connection.getPresence();
	}

	@Override
	public String getUsername() throws RemoteException {
		return connection.getUsername();
	}

	@Override
	public void goOffRecordInRoom(final String s, final boolean flag) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.goOffRecordInRoom");

	}

	@Override
	public void goOffRecordWithContacts(final List list, final boolean flag) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.goOffRecordWithContacts");

	}

	@Override
	public void hideContact(final String s) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.hideContact");

	}

	@Override
	public void inviteContactsToGroupchat(final String groupChat, final String[] contacts) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.inviteContactsToGroupchat");

	}

	@Override
	public boolean isOffRecordWithContact(final String contact) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.isOffRecordWithContact");
		return false;
	}

	@Override
	public void joinGroupChatSession(final String s, final String s1, final String s2) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.joinGroupChatSession");

	}

	@Override
	public void login(final String username, final boolean flag) throws RemoteException {
		// TODO Auto-generated method stub
		Log.d(TAG, "IImSession.login(" + username + ", " + flag + ")");
		connection.getBackend().login();
	}

	@Override
	public void logout() throws RemoteException {
		connection.getBackend().logout();
	}

	public void onChatSessionAvailable(final String user, final IChatSession chatSession) {
		Log.d(TAG, "IImSession.onChatSessionAvailable(" + user + ", " + chatSession + ")");
		synchronized (chatListeners) {
			final List<IChatListener> toRemove = new ArrayList<IChatListener>();
			for (int i = 0; i < chatListeners.size(); i++) {
				final IChatListener listener = chatListeners.get(i);
				try {
					listener.chatRead(user);
				} catch (final RemoteException e) {
					Log.w(TAG, e);
					toRemove.add(listener);
				}
			}
			for (final IChatListener listener : toRemove) {
				chatListeners.remove(listener);
			}
		}
	}

	public void onConnectionStateChanged() {
		Log.d(TAG, "IImSession.onConnectionStateChanged");
		synchronized (connectionStateListeners) {
			final List<IConnectionStateListener> toRemove = new ArrayList<IConnectionStateListener>();
			for (int i = 0; i < connectionStateListeners.size(); i++) {
				final IConnectionStateListener listener = connectionStateListeners.get(i);
				try {
					listener.connectionStateChanged(connection.getConnectionState(), connection.getConnectionError(),
													connection.getAccountId(), connection.getUsername());
				} catch (final RemoteException e) {
					Log.w(TAG, e);
					toRemove.add(listener);
				}
			}
			for (final IConnectionStateListener listener : toRemove) {
				connectionStateListeners.remove(listener);
			}
		}
	}

	public boolean onMessageReceived(final String user, final String message) {
		boolean notify = true;
		synchronized (chatListeners) {
			final List<IChatListener> toRemove = new ArrayList<IChatListener>();
			for (int i = 0; i < chatListeners.size(); i++) {
				final IChatListener listener = chatListeners.get(i);
				try {
					listener.newMessageReceived(user, message, true);
					if (listener.useLightweightNotify()) {
						notify = false;
					}
				} catch (final RemoteException e) {
					Log.w(TAG, e);
					toRemove.add(listener);
				}
			}
			for (final IChatListener listener : toRemove) {
				chatListeners.remove(listener);
			}
		}
		return notify;
	}

	public void onPresenceChanged(final String user) {
		Log.d(TAG, "IImSession.onPresenceChanged(" + user + ")");
		synchronized (rosterListeners) {
			final List<IRosterListener> toRemove = new ArrayList<IRosterListener>();
			for (int i = 0; i < rosterListeners.size(); i++) {
				final IRosterListener listener = rosterListeners.get(i);
				try {
					listener.presenceChanged(user);
				} catch (final RemoteException e) {
					Log.w(TAG, e);
					toRemove.add(listener);
				}
			}
			for (final IRosterListener listener : toRemove) {
				rosterListeners.remove(listener);
			}
		}
	}

	public void onRosterChanged() {
		Log.d(TAG, "IImSession.onRosterChanged");
		synchronized (rosterListeners) {
			final List<IRosterListener> toRemove = new ArrayList<IRosterListener>();
			for (int i = 0; i < rosterListeners.size(); i++) {
				final IRosterListener listener = rosterListeners.get(i);
				try {
					listener.rosterChanged();
				} catch (final RemoteException e) {
					Log.w(TAG, e);
					toRemove.add(listener);
				}
			}
			for (final IRosterListener listener : toRemove) {
				rosterListeners.remove(listener);
			}
		}
	}

	public void onSelfPresenceChanged() {
		Log.d(TAG, "IImSession.onSelfPresenceChanged");
		synchronized (rosterListeners) {
			final List<IRosterListener> toRemove = new ArrayList<IRosterListener>();
			for (int i = 0; i < rosterListeners.size(); i++) {
				final IRosterListener listener = rosterListeners.get(i);
				try {
					listener.selfPresenceChanged();
				} catch (final RemoteException e) {
					Log.w(TAG, e);
					toRemove.add(listener);
				}
			}
			for (final IRosterListener listener : toRemove) {
				rosterListeners.remove(listener);
			}
		}
	}

	@Override
	public void pinContact(final String s) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.pinContact");

	}

	@Override
	public void pruneOldChatSessions(final long l, final long l1, final long l2, final boolean flag)
			throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG,
			  "Not yet implemented: IImSession.pruneOldChatSessions(" + l + ", " + l1 + ", " + l2 + ", " + flag + ")");

	}

	@Override
	public void queryJingleInfo() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.queryJingleInfo");

	}

	@Override
	public void removeConnectionStateListener(final IConnectionStateListener listener) throws RemoteException {
		connectionStateListeners.remove(listener);
	}

	@Override
	public void removeContact(final String s) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.removeContact");

	}

	@Override
	public void removeGroupChatInvitationListener(final IGroupChatInvitationListener listener) throws RemoteException {
		groupChatInvitationListeners.remove(listener);
	}

	@Override
	public void removeRemoteChatListener(final IChatListener listener) throws RemoteException {
		chatListeners.remove(listener);
	}

	@Override
	public void removeRemoteJingleInfoStanzaListener(final IJingleInfoStanzaListener ijingleinfostanzalistener)
			throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.removeRemoteJingleInfoStanzaListener");

	}

	@Override
	public void removeRemoteRosterListener(final IRosterListener listener) throws RemoteException {
		rosterListeners.remove(listener);
	}

	@Override
	public void removeRemoteSessionStanzaListener(final ISessionStanzaListener isessionstanzalistener)
			throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.removeRemoteSessionStanzaListener");

	}

	@Override
	public void requestBatchedBuddyPresence() throws RemoteException {
		Log.d(TAG, "IImSession.requestBatchedBuddyPresence");
		connection.getBackend().requestRoster();
	}

	@Override
	public void sendCallPerfStatsStanza(final String s) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.sendCallPerfStatsStanza");

	}

	@Override
	public void sendSessionStanza(final String s) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.sendSessionStanza");

	}

	@Override
	public void setPresence(final Presence presence) throws RemoteException {
		connection.setPresence(presence);
	}

	@Override
	public void uploadAvatar(final Bitmap bitmap) throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.uploadAvatar");

	}

	@Override
	public void uploadAvatarFromDb() throws RemoteException {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: IImSession.uploadAvatarFromDb");

	}
}
