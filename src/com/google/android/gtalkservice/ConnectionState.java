package com.google.android.gtalkservice;

import android.os.Parcel;
import android.os.Parcelable;

public class ConnectionState implements Parcelable {

	public static final int AUTHENTICATED = 3;
	public static final int CONNECTING = 2;
	public static final Parcelable.Creator<ConnectionState> CREATOR = new Creator<ConnectionState>() {

		@Override
		public ConnectionState createFromParcel(final Parcel parcel) {
			return new ConnectionState(parcel);
		}

		@Override
		public ConnectionState[] newArray(final int num) {
			return new ConnectionState[num];
		}
	};
	public static final int IDLE = 0;
	public static final int ONLINE = 4;

	public static final int RECONNECTION_SCHEDULED = 1;

	public static String toString(final int state) {
		switch (state) {
			case IDLE:
				return "IDLE";
			case RECONNECTION_SCHEDULED:
				return "RECONNECTION_SCHEDULED";
			case CONNECTING:
				return "CONNECTING";
			case AUTHENTICATED:
				return "AUTHENTICATED";
			case ONLINE:
			default:
				return "ONLINE";
		}
	}

	private int stateCode;

	public ConnectionState(final int state) {
		setState(state);
	}

	public ConnectionState(final Parcel parcel) {
		stateCode = parcel.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public int getState() {
		return stateCode;
	}

	public boolean isLoggedIn() {
		return (stateCode >= AUTHENTICATED);
	}

	public boolean isOnline() {
		return (stateCode == ONLINE);
	}

	public boolean isPendingReconnect() {
		return (stateCode == RECONNECTION_SCHEDULED);
	}

	public void setState(final int stateCode) {
		this.stateCode = stateCode;
	}

	@Override
	public String toString() {
		return toString(stateCode);
	}

	@Override
	public void writeToParcel(final Parcel parcel, final int flags) {
		parcel.writeInt(stateCode);
	}
}
