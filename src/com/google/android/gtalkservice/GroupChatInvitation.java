package com.google.android.gtalkservice;

import android.os.Parcel;
import android.os.Parcelable;

public class GroupChatInvitation implements Parcelable {

	public static final Parcelable.Creator<GroupChatInvitation> CREATOR = new Creator<GroupChatInvitation>() {

		@Override
		public GroupChatInvitation createFromParcel(final Parcel parcel) {
			return new GroupChatInvitation(parcel);
		}

		@Override
		public GroupChatInvitation[] newArray(final int num) {
			return new GroupChatInvitation[num];
		}
	};
	private final long groupContactId;
	private final String inviter;
	private final String password;
	private final String reason;

	private final String roomAddress;

	public GroupChatInvitation(final Parcel parcel) {
		roomAddress = parcel.readString();
		inviter = parcel.readString();
		reason = parcel.readString();
		password = parcel.readString();
		groupContactId = parcel.readLong();
	}

	public GroupChatInvitation(final String roomAddress, final String inviter, final String reason,
							   final String password, final long groupContactId) {
		this.roomAddress = roomAddress;
		this.inviter = inviter;
		this.reason = reason;
		this.password = password;
		this.groupContactId = groupContactId;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public long getGroupContactId() {
		return groupContactId;
	}

	public String getInviter() {
		return inviter;
	}

	public String getPassword() {
		return password;
	}

	public String getReason() {
		return reason;
	}

	public String getRoomAddress() {
		return roomAddress;
	}

	@Override
	public void writeToParcel(final Parcel parcel, final int flags) {
		parcel.writeString(roomAddress);
		parcel.writeString(inviter);
		parcel.writeString(reason);
		parcel.writeString(password);
		parcel.writeLong(groupContactId);
	}

}
