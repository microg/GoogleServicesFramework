package com.google.android.gsf.talk;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

public class TalkProvider extends ContentProvider {

	private class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper() {
			super(getContext(), databaseFile, null, databaseVersion);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "SQLiteOpenHelper.onCreate");
			db.beginTransaction();
			db.execSQL(
					"CREATE TABLE accounts (_id INTEGER PRIMARY KEY,name TEXT,username TEXT,locked INTEGER NOT NULL DEFAULT 0,keep_signed_in INTEGER NOT NULL DEFAULT 0,last_login_state INTEGER NOT NULL DEFAULT 0,UNIQUE (username));");
			db.execSQL(
					"CREATE TABLE IF NOT EXISTS contacts (_id INTEGER PRIMARY KEY,username TEXT,nickname TEXT,account INTEGER,contactList INTEGER,type INTEGER,subscriptionStatus INTEGER,subscriptionType INTEGER,qc INTEGER,rejected INTEGER,otr INTEGER, UNIQUE(account, username));");
			db.execSQL(
					"CREATE TABLE IF NOT EXISTS contactsEtag (_id INTEGER PRIMARY KEY,etag TEXT,otr_etag TEXT,account INTEGER UNIQUE);");
			db.execSQL("CREATE INDEX contactsIndex ON contacts (account,username);");
			db.execSQL(
					"CREATE TABLE IF NOT EXISTS messages (_id INTEGER PRIMARY KEY,thread_id INTEGER,nickname TEXT,body TEXT,date INTEGER,real_date INTEGER,type INTEGER,packet_id TEXT,err_code INTEGER NOT NULL DEFAULT 0,err_msg TEXT,is_muc INTEGER,show_ts INTEGER,consolidation_key INTEGER,message_read BOOLEAN,send_status INTEGER,UNIQUE(thread_id, real_date, type));");
			db.execSQL(
					"CREATE TABLE IF NOT EXISTS chats (_id INTEGER PRIMARY KEY AUTOINCREMENT,contact_id INTEGER UNIQUE,jid_resource TEXT,groupchat INTEGER,last_unread_message TEXT,last_message_date INTEGER,unsent_composed_message TEXT,shortcut INTEGER,local INTEGER,otherClient INTEGER,is_active BOOLEAN,account_id INTEGER);");
			db.execSQL(
					"CREATE TRIGGER IF NOT EXISTS contact_cleanup DELETE ON contacts BEGIN DELETE FROM chats WHERE contact_id = OLD._id;DELETE FROM messages WHERE thread_id = OLD._id;END");
			db.execSQL("CREATE INDEX consolidationIndex ON messages (consolidation_key);");
			db.execSQL(
					"CREATE TABLE avatars (_id INTEGER PRIMARY KEY,contact TEXT,account_id INTEGER,hash TEXT,data BLOB,UNIQUE (account_id, contact));");
			db.execSQL(
					"CREATE TABLE accountSettings (_id INTEGER PRIMARY KEY,name TEXT,value TEXT,account_id INTEGER,UNIQUE (name, account_id));");
			db.execSQL(
					"CREATE TRIGGER account_cleanup DELETE ON accounts BEGIN DELETE FROM avatars WHERE account_id= OLD._id;DELETE FROM accountSettings WHERE account_id= OLD._id;DELETE FROM contacts WHERE account= OLD._id;DELETE FROM contactsEtag WHERE account= OLD._id;END");
			db.execSQL(
					"CREATE TABLE outgoingRmqMessages (_id INTEGER PRIMARY KEY,rmq_id INTEGER,type INTEGER,ts INTEGER,data BLOB,account INTEGER,packet_id TEXT);");
			db.execSQL("CREATE TABLE lastrmqid (_id INTEGER PRIMARY KEY,rmq_id INTEGER);");
			db.execSQL("CREATE TABLE s2dRmqIds (_id INTEGER PRIMARY KEY,rmq_id INTEGER);");
			db.setTransactionSuccessful();
			db.endTransaction();
		}

		@Override
		public void onOpen(final SQLiteDatabase db) {
			if (!db.isReadOnly()) {
				db.execSQL("ATTACH DATABASE ':memory:' AS talk_transient;");
				db.execSQL(
						"CREATE TABLE IF NOT EXISTS talk_transient.inMemoryMessages (_id INTEGER PRIMARY KEY,thread_id INTEGER,nickname TEXT,body TEXT,date INTEGER,real_date INTEGER,type INTEGER,packet_id TEXT,err_code INTEGER NOT NULL DEFAULT 0,err_msg TEXT,is_muc INTEGER,show_ts INTEGER,consolidation_key INTEGER,message_read BOOLEAN,send_status INTEGER,UNIQUE(thread_id, real_date, type));");
				db.execSQL(
						"CREATE INDEX IF NOT EXISTS talk_transient.consolidationIndex ON inMemoryMessages (consolidation_key);");
				db.execSQL(
						"CREATE TABLE IF NOT EXISTS talk_transient.presence (_id INTEGER PRIMARY KEY,contact_id INTEGER UNIQUE,jid_resource TEXT,client_type INTEGER,cap INTEGER,priority INTEGER,mode INTEGER,status TEXT);");
				db.execSQL(
						"CREATE TABLE IF NOT EXISTS talk_transient.invitations (_id INTEGER PRIMARY KEY,accountId INTEGER,inviteId TEXT,sender TEXT,groupName TEXT,note TEXT,status INTEGER);");
				db.execSQL(
						"CREATE TABLE IF NOT EXISTS talk_transient.groupMembers (_id INTEGER PRIMARY KEY,groupId INTEGER,username TEXT,nickname TEXT);");
				db.execSQL(
						"CREATE TABLE IF NOT EXISTS talk_transient.accountStatus (_id INTEGER PRIMARY KEY,account INTEGER UNIQUE,presenceStatus INTEGER,connStatus INTEGER);");
			}
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
			// TODO Auto-generated method stub
			Log.w(TAG, "Not yet implemented: SQLiteOpenHelper.onUpgrade");
			throw new RuntimeException("We can't update for now!!!");
		}

	}

	public static final Uri ACCOUNT_SETTINGS_URI =
			Uri.parse("content://com.google.android.providers.talk/accountSettings");
	private static final HashMap<String, String> ACCOUNT_STATUS_PROJECTION = new HashMap<String, String>();
	public static final Uri ACCOUNT_STATUS_URI = Uri.parse("content://com.google.android.providers.talk/accountStatus");
	public static final Uri ACCOUNTS_URI = Uri.parse("content://com.google.android.providers.talk/accounts");
	public static final Uri CHATS_URI = Uri.parse("content://com.google.android.providers.talk/chats");
	public static final Uri CONTACTS_BAREBONE_URI =
			Uri.parse("content://com.google.android.providers.talk/contacts_barebone");
	private static final HashMap<String, String> CONTACTS_PROJECTION = new HashMap<String, String>();
	public static final Uri CONTACTS_URI = Uri.parse("content://com.google.android.providers.talk/contacts");
	private static final HashMap<String, String> IN_MEMEORY_MESSAGES_PROJECTION = new HashMap<String, String>();
	private static final HashMap<String, String> MESSAGES_PROJECTION = new HashMap<String, String>();
	public static final Uri MESSAGES_URI = Uri.parse("content://com.google.android.providers.talk/messages");
	public static final Uri PRESENCE_URI = Uri.parse("content://com.google.android.providers.talk/presence");
	private static final String PROVIDER_NAME = "com.google.android.providers.talk";
	private static final String TAG = "GoogleTalkProvider";
	private static final UriMatcher URI_MATCHER = new UriMatcher(-1);

	static {
		URI_MATCHER.addURI(PROVIDER_NAME, "accounts", 10);
		URI_MATCHER.addURI(PROVIDER_NAME, "accounts/#", 11);
		URI_MATCHER.addURI(PROVIDER_NAME, "accounts/status", 12);
		URI_MATCHER.addURI(PROVIDER_NAME, "contacts", 20);
		URI_MATCHER.addURI(PROVIDER_NAME, "contacts_barebone", 21);
		URI_MATCHER.addURI(PROVIDER_NAME, "contacts_chatting", 22);
		URI_MATCHER.addURI(PROVIDER_NAME, "contacts/blocked", 23);
		URI_MATCHER.addURI(PROVIDER_NAME, "contacts/#", 24);
		URI_MATCHER.addURI(PROVIDER_NAME, "contactsEtag", 37);
		URI_MATCHER.addURI(PROVIDER_NAME, "contactsEtag/#", 38);
		URI_MATCHER.addURI(PROVIDER_NAME, "presence", 40);
		URI_MATCHER.addURI(PROVIDER_NAME, "presence/#", 41);
		URI_MATCHER.addURI(PROVIDER_NAME, "presence/account/#", 42);
		URI_MATCHER.addURI(PROVIDER_NAME, "messages", 50);
		URI_MATCHER.addURI(PROVIDER_NAME, "messagesByAcctAndContact/#/*", 51);
		URI_MATCHER.addURI(PROVIDER_NAME, "messagesByThreadId/#", 52);
		URI_MATCHER.addURI(PROVIDER_NAME, "messagesByAccount/#", 53);
		URI_MATCHER.addURI(PROVIDER_NAME, "messages/#", 54);
		URI_MATCHER.addURI(PROVIDER_NAME, "otrMessages", 55);
		URI_MATCHER.addURI(PROVIDER_NAME, "otrMessagesByAcctAndContact/#/*", 56);
		URI_MATCHER.addURI(PROVIDER_NAME, "otrMessagesByThreadId/#", 57);
		URI_MATCHER.addURI(PROVIDER_NAME, "otrMessagesByAccount/#", 58);
		URI_MATCHER.addURI(PROVIDER_NAME, "otrMessages/#", 59);
		URI_MATCHER.addURI(PROVIDER_NAME, "groupMembers", 65);
		URI_MATCHER.addURI(PROVIDER_NAME, "groupMembers/#", 66);
		URI_MATCHER.addURI(PROVIDER_NAME, "avatars", 70);
		URI_MATCHER.addURI(PROVIDER_NAME, "avatars/#", 71);
		URI_MATCHER.addURI(PROVIDER_NAME, "avatarsBy/#", 72);
		URI_MATCHER.addURI(PROVIDER_NAME, "chats", 80);
		URI_MATCHER.addURI(PROVIDER_NAME, "chats/account/#", 81);
		URI_MATCHER.addURI(PROVIDER_NAME, "chats/#", 82);
		URI_MATCHER.addURI(PROVIDER_NAME, "accountSettings", 90);
		URI_MATCHER.addURI(PROVIDER_NAME, "accountSettings/#", 91);
		URI_MATCHER.addURI(PROVIDER_NAME, "accountSettings/#/*", 92);
		URI_MATCHER.addURI(PROVIDER_NAME, "invitations", 100);
		URI_MATCHER.addURI(PROVIDER_NAME, "invitations/#", 101);
		URI_MATCHER.addURI(PROVIDER_NAME, "accountStatus", 104);
		URI_MATCHER.addURI(PROVIDER_NAME, "accountStatus/#", 105);
		URI_MATCHER.addURI(PROVIDER_NAME, "accountStatus/new_messages", 106);
		URI_MATCHER.addURI(PROVIDER_NAME, "search_suggest_query", 130);
		URI_MATCHER.addURI(PROVIDER_NAME, "search_suggest_query/*", 130);
		URI_MATCHER.addURI(PROVIDER_NAME, "outgoingRmqMessages", 200);
		URI_MATCHER.addURI(PROVIDER_NAME, "outgoingRmqMessages/#", 201);
		URI_MATCHER.addURI(PROVIDER_NAME, "outgoingHighestRmqId", 202);
		URI_MATCHER.addURI(PROVIDER_NAME, "lastRmqId", 203);
		URI_MATCHER.addURI(PROVIDER_NAME, "s2dids", 204);
		ACCOUNT_STATUS_PROJECTION.put("_id", "accounts._id AS _id");
		ACCOUNT_STATUS_PROJECTION.put("username", "accounts.username AS username");
		ACCOUNT_STATUS_PROJECTION.put("account_connStatus", "accountStatus.connStatus AS account_connStatus");
		CONTACTS_PROJECTION.put("_id", "contacts._id AS _id");
		CONTACTS_PROJECTION.put("_count", "COUNT(*) AS _count");
		CONTACTS_PROJECTION.put("_id", "contacts._id as _id");
		CONTACTS_PROJECTION.put("username", "contacts.username as username");
		CONTACTS_PROJECTION.put("nickname", "contacts.nickname as nickname");
		CONTACTS_PROJECTION.put("account", "contacts.account as account");
		CONTACTS_PROJECTION.put("contactList", "contacts.contactList as contactList");
		CONTACTS_PROJECTION.put("type", "contacts.type as type");
		CONTACTS_PROJECTION.put("subscriptionStatus", "contacts.subscriptionStatus as subscriptionStatus");
		CONTACTS_PROJECTION.put("subscriptionType", "contacts.subscriptionType as subscriptionType");
		CONTACTS_PROJECTION.put("qc", "contacts.qc as qc");
		CONTACTS_PROJECTION.put("rejected", "contacts.rejected as rejected");
		CONTACTS_PROJECTION.put("contact_id", "presence.contact_id AS contact_id");
		CONTACTS_PROJECTION.put("mode", "presence.mode AS mode");
		CONTACTS_PROJECTION.put("status", "presence.status AS status");
		CONTACTS_PROJECTION.put("client_type", "presence.client_type AS client_type");
		CONTACTS_PROJECTION.put("cap", "presence.cap AS cap");
		CONTACTS_PROJECTION.put("chats_contact", "chats.contact_id AS chats_contact_id");
		CONTACTS_PROJECTION.put("jid_resource", "chats.jid_resource AS jid_resource");
		CONTACTS_PROJECTION.put("groupchat", "chats.groupchat AS groupchat");
		CONTACTS_PROJECTION.put("last_unread_message", "chats.last_unread_message AS last_unread_message");
		CONTACTS_PROJECTION.put("last_message_date", "chats.last_message_date AS last_message_date");
		CONTACTS_PROJECTION.put("unsent_composed_message", "chats.unsent_composed_message AS unsent_composed_message");
		CONTACTS_PROJECTION.put("shortcut", "chats.SHORTCUT AS shortcut");
		CONTACTS_PROJECTION.put("is_active", "chats.IS_ACTIVE AS IS_ACTIVE");
		CONTACTS_PROJECTION.put("avatars_hash", "avatars.hash AS avatars_hash");
		CONTACTS_PROJECTION.put("avatars_data", "avatars.data AS avatars_data");
		MESSAGES_PROJECTION.put("_id", "messages._id AS _id");
		MESSAGES_PROJECTION.put("_count", "COUNT(*) AS _count");
		MESSAGES_PROJECTION.put("thread_id", "messages.thread_id AS thread_id");
		MESSAGES_PROJECTION.put("packet_id", "messages.packet_id AS packet_id");
		MESSAGES_PROJECTION.put("nickname", "messages.nickname AS nickname");
		MESSAGES_PROJECTION.put("body", "messages.body AS body");
		MESSAGES_PROJECTION.put("date", "messages.date AS date");
		MESSAGES_PROJECTION.put("type", "messages.type AS type");
		MESSAGES_PROJECTION.put("msg_type", "messages.type AS msg_type");
		MESSAGES_PROJECTION.put("send_status", "messages.send_status AS send_status");
		MESSAGES_PROJECTION.put("err_code", "messages.err_code AS err_code");
		MESSAGES_PROJECTION.put("err_msg", "messages.err_msg AS err_msg");
		MESSAGES_PROJECTION.put("is_muc", "messages.is_muc AS is_muc");
		MESSAGES_PROJECTION.put("show_ts", "messages.show_ts AS show_ts");
		MESSAGES_PROJECTION.put("contact", "contacts.username AS contact");
		MESSAGES_PROJECTION.put("account", "contacts.account AS account");
		MESSAGES_PROJECTION.put("contact_type", "contacts.type AS contact_type");
		MESSAGES_PROJECTION.put("consolidation_key", "messages.consolidation_key as consolidation_key");
		IN_MEMEORY_MESSAGES_PROJECTION.put("_id", "inMemoryMessages._id AS _id");
		IN_MEMEORY_MESSAGES_PROJECTION.put("_count", "COUNT(*) AS _count");
		IN_MEMEORY_MESSAGES_PROJECTION.put("thread_id", "inMemoryMessages.thread_id AS thread_id");
		IN_MEMEORY_MESSAGES_PROJECTION.put("packet_id", "inMemoryMessages.packet_id AS packet_id");
		IN_MEMEORY_MESSAGES_PROJECTION.put("nickname", "inMemoryMessages.nickname AS nickname");
		IN_MEMEORY_MESSAGES_PROJECTION.put("body", "inMemoryMessages.body AS body");
		IN_MEMEORY_MESSAGES_PROJECTION.put("date", "inMemoryMessages.date AS date");
		IN_MEMEORY_MESSAGES_PROJECTION.put("type", "inMemoryMessages.type AS type");
		IN_MEMEORY_MESSAGES_PROJECTION.put("msg_type", "inMemoryMessages.type AS msg_type");
		IN_MEMEORY_MESSAGES_PROJECTION.put("send_status", "inMemoryMessages.send_status AS send_status");
		IN_MEMEORY_MESSAGES_PROJECTION.put("err_code", "inMemoryMessages.err_code AS err_code");
		IN_MEMEORY_MESSAGES_PROJECTION.put("err_msg", "inMemoryMessages.err_msg AS err_msg");
		IN_MEMEORY_MESSAGES_PROJECTION.put("is_muc", "inMemoryMessages.is_muc AS is_muc");
		IN_MEMEORY_MESSAGES_PROJECTION.put("show_ts", "inMemoryMessages.show_ts AS show_ts");
		IN_MEMEORY_MESSAGES_PROJECTION.put("contact", "contacts.username AS contact");
		IN_MEMEORY_MESSAGES_PROJECTION.put("account", "contacts.account AS account");
		IN_MEMEORY_MESSAGES_PROJECTION.put("contact_type", "contacts.type AS contact_type");
		IN_MEMEORY_MESSAGES_PROJECTION
				.put("consolidation_key", "inMemoryMessages.consolidation_key as consolidation_key");
	}

	private static String arrayToString(final String[] array) {
		if (array == null) {
			return "null";
		}
		final StringBuilder sb = new StringBuilder("[");
		boolean first = true;
		for (final String string : array) {
			if (!first) {
				sb.append(", ");
			}
			sb.append(string);
			first = false;
		}
		return sb.append("]").toString();
	}

	private final String databaseFile = "talk.db";
	private DatabaseHelper databaseHelper;
	private final int databaseVersion = 68;

	private void appendAccountIdFromUri(final Uri uri, final StringBuilder whereClause) {
		appendIdFromUri(uri, "account_id", whereClause);
	}

	private void appendAndIfNeeded(final StringBuilder whereClause) {
		if (whereClause.length() != 0) {
			whereClause.append(" AND ");
		}
	}

	private void appendIdFromUri(final Uri uri, final String idName, final StringBuilder whereClause) {
		appendAndIfNeeded(whereClause);
		whereClause.append(idName).append("=");
		DatabaseUtils.appendValueToSql(whereClause, uri.getPathSegments().get(1));
	}

	private void appendIdFromUri(final Uri uri, final StringBuilder whereClause) {
		appendIdFromUri(uri, "_id", whereClause);
	}

	@Override
	public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
		// TODO Auto-generated method stub
		final int match = URI_MATCHER.match(uri);
		final SQLiteDatabase db = databaseHelper.getWritableDatabase();
		switch (match) {
			case 50:
				return db.delete("messages", selection, selectionArgs);
			default:
				Log.w(TAG,
					  "Not yet implemented: TalkProvider.delete (" + uri + ", " + selection + ", " + selectionArgs +
					  ")");
		}
		return 0;
	}

	@Override
	public String getType(final Uri uri) {
		final int match = URI_MATCHER.match(uri);
		switch (match) {
			case 82:
				return "vnd.android.cursor.item/gtalk-chats";
			default:
				Log.w(TAG, "Not yet implemented: TalkProvider.getType (" + uri + ")");
				return null;
		}
	}

	/**
	 * Get uniques of a table in the database
	 *
	 * @param table the table of which uniques should be returned
	 * @return array of uniques in the given table or null if unknown
	 */
	private String[] getUniques(final String table) {
		String[] uniques = null;
		if (table.equalsIgnoreCase("contacts")) {
			uniques = new String[]{"account", "username"};
		} else if (table.equalsIgnoreCase("accounts")) {
			uniques = new String[]{"username"};
		}
		return uniques;
	}

	private long insert(final String table, final String nullColumnHack, final ContentValues values) {
		final SQLiteDatabase db = databaseHelper.getWritableDatabase();
		return db.insert(table, nullColumnHack, values);
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		final int match = URI_MATCHER.match(uri);
		Uri result = uri;
		switch (match) {
			case 10:
				result = ContentUris.withAppendedId(ACCOUNTS_URI, replace("accounts", null, values));
				break;
			case 20:
				result = ContentUris.withAppendedId(CONTACTS_URI, replace("contacts", null, values));
				break;
			case 40:
				result = ContentUris.withAppendedId(PRESENCE_URI, replace("talk_transient.presence", null, values));
				break;
			case 50:
				result = ContentUris.withAppendedId(MESSAGES_URI, insert("messages", null, values));
				break;
			case 80:
				result = ContentUris.withAppendedId(CHATS_URI, replace("chats", null, values));
				break;
			case 90:
				result = ContentUris.withAppendedId(ACCOUNT_SETTINGS_URI, replace("accountSettings", null, values));
				break;
			case 104:
				result = ContentUris
						.withAppendedId(ACCOUNT_STATUS_URI, replace("talk_transient.accountStatus", null, values));
				break;
			default:
				Log.w(TAG, "Not yet implemented: TalkProvider.insert (" + uri + ", " + values + ")");
				result = null;
				break;
		}
		Log.w(TAG, "TalkProvider.insert (" + uri + ", " + values + ") = " + result);
		return result;
	}

	@Override
	public boolean onCreate() {
		databaseHelper = new DatabaseHelper();
		return true;
	}

	@Override
	public Cursor query(final Uri uri, final String[] projection, final String selection, final String[] selectionArgs,
						final String sortOrder) {

		final int match = URI_MATCHER.match(uri);
		final SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		final StringBuilder whereClause = new StringBuilder();
		if (selection != null) {
			whereClause.append(selection);
		}
		final String limit = null;
		final SQLiteDatabase db = databaseHelper.getReadableDatabase();
		switch (match) {
			case 10:
				builder.setTables("accounts");
				break;
			case 11:
				builder.setTables("accounts");
				appendIdFromUri(uri, whereClause);
				break;
			case 12:
				builder.setTables("accounts LEFT OUTER JOIN accountStatus ON (accounts._id = accountStatus.account)");
				builder.setProjectionMap(ACCOUNT_STATUS_PROJECTION);
				break;
			case 20:
				builder.setTables(
						"contacts LEFT OUTER JOIN presence ON (contacts._id = presence.contact_id) LEFT OUTER JOIN chats ON (contacts._id = chats.contact_id) LEFT OUTER JOIN avatars ON (contacts.username = avatars.contact AND contacts.account = avatars.account_id)");
				builder.setProjectionMap(CONTACTS_PROJECTION);
				break;
			case 21:
				builder.setTables("contacts");
				break;
			case 22:
				builder.setTables(
						"chats LEFT OUTER JOIN contacts ON (contacts._id = chats.contact_id) LEFT OUTER JOIN presence ON (contacts._id = presence.contact_id) LEFT OUTER JOIN avatars ON (contacts.username = avatars.contact AND contacts.account = avatars.account_id)");
				builder.setProjectionMap(CONTACTS_PROJECTION);
			/*
			 * if (whereClause.length() != 0) { whereClause.append(" AND "); }
			 * whereClause.append("chats.last_message_date IS NOT NULL");
			 */
				break;
			case 40:
				builder.setTables("presence");
				break;
			case 51:
				builder.setTables("messages LEFT OUTER JOIN contacts ON (contacts._id = messages.thread_id)");
				appendIdFromUri(uri, "account", whereClause);
				whereClause.append(" AND contacts.username LIKE ");
				try {
					DatabaseUtils
							.appendValueToSql(whereClause, URLDecoder.decode(uri.getPathSegments().get(2), "UTF-8"));
				} catch (final UnsupportedEncodingException e1) {
					Log.w(TAG, e1);
					whereClause.append("\"%\"");
				}
				builder.setProjectionMap(MESSAGES_PROJECTION);
				break;
			case 72:
				builder.setTables("avatars");
				appendAccountIdFromUri(uri, whereClause);
				break;
			case 80:
				builder.setTables("chats");
				break;
			case 82:
				builder.setTables("messages");
				appendIdFromUri(uri, "thread_id", whereClause);
				break;
			case 90:
				builder.setTables("accountSettings");
				break;
			case 91:
				builder.setTables("accountSettings");
				appendAccountIdFromUri(uri, whereClause);
				break;
			case 92:
				builder.setTables("accountSettings");
				appendAccountIdFromUri(uri, whereClause);
				whereClause.append(" AND name LIKE ");
				DatabaseUtils.appendValueToSql(whereClause, uri.getPathSegments().get(2));
				break;
			case 106:
				return db.rawQuery(
						"select contacts.account, count(last_unread_message) as active_count from chats,contacts where chats.contact_id = contacts._id AND chats.is_active = 1 group by contacts.account order by contacts.account;",
						null);
			default:
				Log.w(TAG, "Not yet implemented: TalkProvider.query (" + uri + ", " + arrayToString(projection) + ", " +
						   selection + ", " + arrayToString(selectionArgs) + ", " + sortOrder + ")");
				return null;
		}
		try {
			final Cursor cursor =
					builder.query(db, projection, whereClause.toString(), selectionArgs, null, null, sortOrder, limit);
			if (cursor != null) {
				cursor.setNotificationUri(getContext().getContentResolver(), uri);
				if (cursor.getCount() == 0) {
					Log.w(TAG,
						  "Empty Result for: TalkProvider.query (" + uri + ", " + arrayToString(projection) + ", " +
						  selection + ", " + arrayToString(selectionArgs) + ", " + sortOrder + ")");
				}
				return cursor;
			}
		} catch (final Exception e) {
			Log.e(TAG, "error while db query for " + uri, e);
		}

		return null;
	}

	/**
	 * Convenience method for replacing a row in the database. Holds id on
	 * replace if possible (using getUniques)
	 *
	 * @param table          the table in which to replace the row
	 * @param nullColumnHack optional; may be null. SQL doesn't allow inserting a
	 *                       completely empty row without naming at least one column name.
	 *                       If your provided initialValues is empty, no column names are
	 *                       known and an empty row can't be inserted. If not set to null,
	 *                       the nullColumnHack parameter provides the name of nullable
	 *                       column name to explicitly insert a NULL into in the case where
	 *                       your initialValues is empty.
	 * @param values         this map contains the column values for the row.
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	private long replace(final String table, final String nullColumnHack, final ContentValues values) {
		final String[] uniques = getUniques(table);
		return replace(table, nullColumnHack, values, uniques);
	}

	/**
	 * Convenience method for replacing a row in the database. Holds id on
	 * replace if entry with uniques is available
	 *
	 * @param table          the table in which to replace the row
	 * @param nullColumnHack optional; may be null. SQL doesn't allow inserting a
	 *                       completely empty row without naming at least one column name.
	 *                       If your provided initialValues is empty, no column names are
	 *                       known and an empty row can't be inserted. If not set to null,
	 *                       the nullColumnHack parameter provides the name of nullable
	 *                       column name to explicitly insert a NULL into in the case where
	 *                       your initialValues is empty.
	 * @param values         this map contains the column values for the row.
	 * @param uniques        array describing the uniques of a table, if null or empty new
	 *                       id is created
	 * @return the row ID of the newly inserted row, or -1 if an error occurred
	 */
	private long replace(final String table, final String nullColumnHack, final ContentValues values,
						 final String... uniques) {
		final SQLiteDatabase db = databaseHelper.getWritableDatabase();
		if (uniques == null || uniques.length == 0) {
			return db.replace(table, nullColumnHack, values);
		}
		final StringBuilder whereClause = new StringBuilder();
		final String[] selectionArgs = new String[uniques.length];
		for (int i = 0; i < uniques.length; i++) {
			final String unique = uniques[i];
			appendAndIfNeeded(whereClause);
			whereClause.append(unique).append("=?");
			selectionArgs[i] = values.getAsString(unique);
		}
		final Cursor c = db.query(table, new String[]{"_id"}, whereClause.toString(), selectionArgs, null, null, null);
		if (c.moveToFirst()) {
			final long id = c.getLong(0);
			c.close();
			db.update(table, values, "_id=?", new String[]{id + ""});
			return id;
		} else {
			c.close();
			return db.replace(table, nullColumnHack, values);
		}
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
		final int match = URI_MATCHER.match(uri);
		final SQLiteDatabase db = databaseHelper.getWritableDatabase();
		String database = null;
		final StringBuilder whereClause = new StringBuilder(selection);
		switch (match) {
			case 20:
				database = "contacts";
				break;
			case 21:
				database = "contacts";
				appendIdFromUri(uri, whereClause);
				break;
			case 40:
				database = "talk_transient.presence";
				break;
			case 41:
				database = "talk_transient.presence";
				appendIdFromUri(uri, whereClause);
				break;
			case 50:
				database = "messages";
				break;
			case 80:
				database = "chats";
				break;
			case 81:
				database = "chats";
				appendIdFromUri(uri, whereClause);
				break;
			case 90:
				database = "accountSettings";
				break;
			case 91:
				database = "accountSettings";
				appendIdFromUri(uri, whereClause);
			case 104:
				database = "talk_transient.accountStatus";
				break;
			case 105:
				database = "talk_transient.accountStatus";
				appendIdFromUri(uri, whereClause);
				break;
			default:
				Log.w(TAG,
					  "Not yet implemented: TalkProvider.update (" + uri + ", " + values + ", " + selection + ", " +
					  arrayToString(selectionArgs) + ")");
		}
		if (database != null) {
			return db.update(database, values, whereClause.toString(), selectionArgs);
		}
		return 0;
	}

}
