package com.google.android.gtalkservice;

import android.os.Parcel;
import android.os.Parcelable;

public class ConnectionError implements Parcelable {

	public static final int AUTH_EXPIRED = 5;
	public static final int AUTH_FAILED = 4;
	public static final int CONNECTION_FAILED = 2;
	public static final Parcelable.Creator<ConnectionError> CREATOR = new Creator<ConnectionError>() {

		@Override
		public ConnectionError createFromParcel(final Parcel parcel) {
			return new ConnectionError(parcel);
		}

		@Override
		public ConnectionError[] newArray(final int num) {
			return new ConnectionError[num];
		}
	};
	public static final int HEARTBEAT_TIMEOUT = 6;
	public static final int NO_ERROR = 0;
	public static final int NO_NETWORK = 1;
	public static final int SERVER_FAILED = 7;
	public static final int SERVER_REJECTED = 8;
	public static final int UNKNOWN = 9;

	public static final int UNKNOWN_HOST = 3;

	public static boolean isAuthenticationError(final int errorCode) {
		return (errorCode == AUTH_FAILED);
	}

	public static String toString(final int error) {
		switch (error) {
			case NO_ERROR:
				return "NO ERROR";
			case NO_NETWORK:
				return "NO NETWORK";
			case CONNECTION_FAILED:
				return "CONNECTION FAILED";
			case UNKNOWN_HOST:
				return "UNKNOWN HOST";
			case AUTH_FAILED:
				return "AUTH FAILED";
			case AUTH_EXPIRED:
				return "AUTH EXPIRED";
			case HEARTBEAT_TIMEOUT:
				return "HEARTBEAT TIMEOUT";
			case SERVER_FAILED:
				return "SERVER FAILED";
			case SERVER_REJECTED:
				return "SERVER REJECT - RATE LIMIT";
			case UNKNOWN:
			default:
				return "UNKNOWN";
		}
	}

	private int errorCode;

	public ConnectionError(final int error) {
		setError(error);
	}

	public ConnectionError(final Parcel parcel) {
		errorCode = parcel.readInt();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public int getError() {
		return errorCode;
	}

	public void setError(final int errorCode) {
		this.errorCode = errorCode;
	}

	@Override
	public String toString() {
		return toString(errorCode);
	}

	@Override
	public void writeToParcel(final Parcel parcel, final int flags) {
		parcel.writeInt(errorCode);
	}

}
