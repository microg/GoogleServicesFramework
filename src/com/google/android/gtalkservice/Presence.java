package com.google.android.gtalkservice;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Presence implements Parcelable {

	public static enum Show {
		NONE, AWAY, EXTENDED_AWAY, DND, AVAILABLE;
	}

	public static final Parcelable.Creator<Presence> CREATOR = new Creator<Presence>() {

		@Override
		public Presence createFromParcel(final Parcel parcel) {
			return new Presence(parcel);
		}

		@Override
		public Presence[] newArray(final int num) {
			return new Presence[num];
		}
	};
	public static final Presence OFFLINE = new Presence();
	private boolean allowInvisibility;
	private boolean available;
	private int capabilities;
	private final List<String> defaultStatusList;
	private final List<String> dndStatusList;
	private boolean invisible;
	private Show show;
	private String status;
	private int statusListContentsMax;
	private int statusListMax;
	private int statusMax;

	public int getNumeric() {
		if (!isAvailable()) {
			return 0;
		}
		if (isInvisible()) {
			return 1;
		}
		switch (show) {
			case DND:
				return 4;
			case AWAY:
			case EXTENDED_AWAY:
				return 2;
			case AVAILABLE:
			default:
				return 5;
		}
	}

	public Presence() {
		this(false, Show.NONE, null, 8);
	}

	public Presence(int numeric) {
		this(numeric != 0, Show.NONE, null, 8);
		switch (numeric) {
			case 1:
				invisible = true;
				break;
			case 4:
				show = Show.DND;
				break;
			case 2:
			case 5:
			default:
				show = Show.AVAILABLE;
				break;
		}
	}

	public Presence(final boolean available, final Show show, final String status, final int capabilities) {
		this.available = available;
		this.show = show;
		this.status = status;
		this.invisible = false;
		this.defaultStatusList = new ArrayList<String>();
		this.dndStatusList = new ArrayList<String>();
		this.capabilities = capabilities;
	}

	public Presence(final Parcel parcel) {
		super();
		setStatusMax(parcel.readInt());
		setStatusListMax(parcel.readInt());
		setStatusListContentsMax(parcel.readInt());
		if (parcel.readInt() != 0) {
			setAllowInvisibility(true);
		} else {
			setAllowInvisibility(false);
		}
		if (parcel.readInt() != 0) {
			setAvailable(true);
		} else {
			setAvailable(false);
		}
		setShow(Show.valueOf(parcel.readString()));
		status = parcel.readString();
		if (parcel.readInt() == 0) {
			setInvisible(false);
		} else {
			setInvisible(true);
		}
		defaultStatusList = new ArrayList<String>();
		parcel.readStringList(defaultStatusList);
		dndStatusList = new ArrayList<String>();
		parcel.readStringList(dndStatusList);
		setCapabilities(parcel.readInt());
	}

	public Presence(final Presence presence) {
		statusMax = presence.statusMax;
		statusListMax = presence.statusListMax;
		statusListContentsMax = presence.statusListContentsMax;
		allowInvisibility = presence.allowInvisibility;
		available = presence.available;
		this.show = presence.show;
		status = presence.status;
		invisible = presence.invisible;
		defaultStatusList = presence.defaultStatusList;
		dndStatusList = presence.dndStatusList;
		capabilities = presence.capabilities;
	}

	private boolean addToList(final List<String> list, String string) {
		if (string == null || string.isEmpty()) {
			return false;
		}
		for (final String inList : list) {
			if (inList.trim().equals(string.trim())) {
				return false;
			}
		}

		if (string.length() > getStatusMax()) {
			string = string.substring(0, getStatusMax());
		}
		list.add(0, string);
		checkListContentsLength(list);
		return true;
	}

	public boolean allowInvisibility() {
		return allowInvisibility;
	}

	private List<String> checkListContentsLength(final List<String> list) {
		if (list.size() > getStatusListContentsMax()) {
			for (int i = list.size() - 1; i >= getStatusListContentsMax(); i--) {
				list.remove(i);
			}

		}
		return list;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public boolean equals(final Presence presence) {
		if (presence != null && available == presence.available && show == presence.show &&
			(status == null ? presence.status == null : status.equals(presence.status)) &&
			(invisible == presence.invisible && statusMax == presence.statusMax &&
			 statusListMax == presence.statusListMax && statusListContentsMax == presence.statusListContentsMax &&
			 allowInvisibility == presence.allowInvisibility &&
			 listEqual(defaultStatusList, presence.defaultStatusList) &&
			 listEqual(dndStatusList, presence.dndStatusList) && capabilities == presence.capabilities)) {
			return true;
		}
		return false;
	}

	public int getCapabilities() {
		return capabilities;
	}

	public List<String> getDefaultStatusList() {
		return new ArrayList<String>(defaultStatusList);
	}

	public List<String> getDndStatusList() {
		return new ArrayList<String>(dndStatusList);
	}

	public Show getShow() {
		return show;
	}

	public String getStatus() {
		return status;
	}

	public int getStatusListContentsMax() {
		return statusListContentsMax;
	}

	public int getStatusListMax() {
		return statusListMax;
	}

	public int getStatusMax() {
		return statusMax;
	}

	public boolean isAvailable() {
		return available;
	}

	public boolean isInvisible() {
		return invisible;
	}

	private boolean listEqual(final List<String> firstList, final List<String> secondList) {
		if (firstList.size() != secondList.size()) {
			return false;
		}
		for (int i = 0; i < firstList.size(); i++) {
			if (!firstList.get(i).equals(secondList.get(i))) {
				return false;
			}
		}
		return true;
	}

	public String printDetails() {
		final StringBuilder stringbuilder = new StringBuilder();
		stringbuilder.append("{ available=").append(available).append(", show=").append(show).append(", ");
		if (status == null) {
			stringbuilder.append("");
		} else {
			stringbuilder.append(status);
		}
		stringbuilder.append((new StringBuilder()).append(", invisible=").append(invisible).toString())
					 .append(", allowInvisible=").append(allowInvisibility).append(", caps=0x")
					 .append(Integer.toHexString(capabilities)).append(", default={");
		if (defaultStatusList != null) {
			boolean first = true;
			for (final String defaultStatus : defaultStatusList) {
				if (!first) {
					stringbuilder.append(", ");
				}
				stringbuilder.append(defaultStatus);
				first = false;
			}

		}
		stringbuilder.append("}, dnd={");
		if (dndStatusList != null) {
			boolean first = true;
			for (final String dndStatus : dndStatusList) {
				if (!first) {
					stringbuilder.append(", ");
				}
				stringbuilder.append(dndStatus);
				first = false;
			}

		}
		return stringbuilder.append("}").append("}").toString();
	}

	public void setAllowInvisibility(final boolean allowInvisibility) {
		this.allowInvisibility = allowInvisibility;
	}

	public void setAvailable(final boolean available) {
		this.available = available;
	}

	public void setCapabilities(final int capabilities) {
		this.capabilities = capabilities;
	}

	public boolean setInvisible(final boolean invisible) {
		this.invisible = invisible;
		if (invisible && !allowInvisibility()) {
			return false;
		} else {
			return true;
		}
	}

	public void setShow(final Show show) {
		this.show = show;
	}

	public void setStatus(final Show show, final String s) {
		setShow(show);
		setStatus(s, true);
	}

	public void setStatus(final String s) {
		setStatus(s, false);
	}

	private void setStatus(final String s, final boolean flag) {
		status = s;
		if (!flag) {
			switch (show) {
				case DND:
					addToList(dndStatusList, s);
					break;
				case AVAILABLE:
					addToList(defaultStatusList, s);
					break;
				default:
					break;
			}
		}
	}

	public void setStatusListContentsMax(final int i) {
		statusListContentsMax = i;
	}

	public void setStatusListMax(final int i) {
		statusListMax = i;
	}

	public void setStatusMax(final int i) {
		statusMax = i;
	}

	@Override
	public void writeToParcel(final Parcel parcel, final int flags) {
		parcel.writeInt(getStatusMax());
		parcel.writeInt(getStatusListMax());
		parcel.writeInt(getStatusListContentsMax());
		if (allowInvisibility()) {
			parcel.writeInt(1);
		} else {
			parcel.writeInt(0);
		}
		if (available) {
			parcel.writeInt(1);
		} else {
			parcel.writeInt(0);
		}
		parcel.writeString(show.toString());
		parcel.writeString(status);
		if (invisible) {
			parcel.writeInt(1);
		} else {
			parcel.writeInt(0);
		}
		parcel.writeStringList(defaultStatusList);
		parcel.writeStringList(dndStatusList);
		parcel.writeInt(getCapabilities());
	}

}
