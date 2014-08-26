package com.google.android.gsf.subscribedfeeds;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class SubscribedFeedsProvider extends ContentProvider {

	private static final String TAG = "SubscribedFeedsProvider";

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: delete: " + uri);
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: getType: " + uri);
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: insert: " + uri);
		return null;
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: onCreate");
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: query: " + uri);
		return new MatrixCursor(projection);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		Log.w(TAG, "Not yet implemented: update: " + uri);
		return 0;
	}

}
