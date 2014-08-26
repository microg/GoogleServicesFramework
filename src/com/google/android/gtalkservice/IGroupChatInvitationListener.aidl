package com.google.android.gtalkservice;

import com.google.android.gtalkservice.GroupChatInvitation;

interface IGroupChatInvitationListener {
	boolean onInvitationReceived(in GroupChatInvitation groupchatinvitation);
}