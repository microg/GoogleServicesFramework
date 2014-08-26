package com.google.android.gtalkservice;

import com.google.android.gtalkservice.IChatListener;
import android.net.Uri;

interface IChatSession {
	boolean isGroupChat();
	void markAsRead();
	String[] getParticipants();
	void inviteContact(String contact);
	void leave();
	void sendChatMessage(String message);
	void saveUnsentComposedMessage(String message);
	String getUnsentComposedMessage();
	void addRemoteChatListener(IChatListener listener);
	void removeRemoteChatListener(IChatListener listener);
	boolean isOffTheRecord();
	boolean getLightweightNotify();
	void reportEndCause(String s, boolean flag, int i);
	void reportMissedCall(String s, String s1, boolean flag, boolean flag1);
	void ensureNonZeroLastMessageDate();
	void clearChatHistory(in Uri uri);
}