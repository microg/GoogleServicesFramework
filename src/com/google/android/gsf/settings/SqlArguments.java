package com.google.android.gsf.settings;

import android.content.ContentUris;
import android.net.Uri;

class SqlArguments {

	public String[] args;
	public String table;
	public String where;

	SqlArguments(final Uri uri) {
		if (uri.getPathSegments().size() != 1) {
			throw new IllegalArgumentException("Invalid URI: " + uri);
		}
		table = uri.getPathSegments().get(0);
		where = null;
		args = null;
	}

	SqlArguments(final Uri uri, final String selection, final String[] selectionArgs) {
		table = uri.getPathSegments().get(0);
		if (uri.getPathSegments().size() == 1) {
			where = selection;
			args = selectionArgs;
		} else {
			if (uri.getPathSegments().size() != 2) {
				throw new IllegalArgumentException("Invalid URI: " + uri);
			}
			if (selection != null && !selection.isEmpty()) {
				throw new UnsupportedOperationException("WHERE clause not supported: " + uri);
			}
			if (table.equals("partner")) {
				where = "name=?";
				args = new String[]{uri.getPathSegments().get(1)};
			} else {
				where = "_id=" + ContentUris.parseId(uri);
				args = null;
			}
		}
	}

}