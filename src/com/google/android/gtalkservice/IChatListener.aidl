package com.google.android.gtalkservice;

interface IChatListener {
	void newMessageReceived(String from, String body, boolean notify);
	void newMessageSent(String body);
	void missedCall();
	void callEnded();
	void chatRead(String from);
	void chatClosed(String from);
	void willConvertToGroupChat(String oldJid, String groupChatRoom, long groupId);
	void convertedToGroupChat(String oldJid, String groupChatRoom, long groupId);
	void participantJoined(String room, String nickname);
	void participantLeft(String room, String nickname);
	boolean useLightweightNotify();
}