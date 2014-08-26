package com.google.android.gsf.gtalkservice.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import com.google.android.gsf.R;
import com.google.android.gtalkservice.IChatSession;

import java.util.ArrayList;
import java.util.List;

public class Notifier {

	private class NotifyChatMessage {
		private final String from;
		private final String message;

		public NotifyChatMessage(final String from, final String message) {
			this.from = from;
			this.message = message;
		}

		public String getFrom() {
			return from;
		}

		public String getMessage() {
			return message;
		}
	}

	private static final int NOTIFY_CHAT_MESSAGE = 12345;
	private static final String TAG = "GoogleTalkNotifier";

	public static Bitmap drawableToBitmap(final Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		final Bitmap bitmap =
				Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
		final Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	private final GTalkConnection connection;
	private final List<NotifyChatMessage> currentChatMessages = new ArrayList<NotifyChatMessage>();

	private final NotificationManager nm;

	public Notifier(final GTalkConnection connection) {
		this.connection = connection;
		nm = (NotificationManager) connection.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public void dismissChatMessages() {
		currentChatMessages.clear();
		notifyChatMessages(false);
	}

	public void dismissChatMessages(String user) {
		if (user.contains("/")) {
			user = user.split("/")[0];
		}
		for (final NotifyChatMessage entry : currentChatMessages) {
			if (entry.getFrom().equalsIgnoreCase(user)) {
				currentChatMessages.remove(entry);
			}
		}
		notifyChatMessages(false);
	}

	public void notifyChatMessage(final IChatSession chatSession, final long contactId, final String user,
								  final String message) {
		currentChatMessages.add(new NotifyChatMessage(user, message));
		notifyChatMessages(true);
	}

	private void notifyChatMessages(final boolean ticker) {
		if (currentChatMessages.size() == 0) {
			nm.cancel(NOTIFY_CHAT_MESSAGE);
			return;
		}
		final Notification.Builder builder = new Notification.Builder(connection.getContext());
		builder.setAutoCancel(false);
		builder.setSmallIcon(R.drawable.stat_notify_msg);
		builder.setNumber(currentChatMessages.size());
		String user = null;
		String message = null;
		for (final NotifyChatMessage entry : currentChatMessages) {
			user = entry.getFrom();
			message = entry.getMessage();
		}
		if (ticker) {
			builder.setOnlyAlertOnce(false);
			builder.setTicker(user + ": " + message);
		}
		builder.setContentTitle(user);
		builder.setContentText(message);
		builder.setLargeIcon(
				drawableToBitmap(connection.getContext().getResources().getDrawable(R.drawable.ic_contact_picture)));
		nm.notify(NOTIFY_CHAT_MESSAGE, versionSpecific(builder));
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private Notification versionSpecific(Notification.Builder builder) {
		if (Build.VERSION.SDK_INT >= 16) {
			builder.setPriority(Notification.PRIORITY_HIGH);
			return builder.build();
		} else {
			return builder.getNotification();
		}
	}

}
