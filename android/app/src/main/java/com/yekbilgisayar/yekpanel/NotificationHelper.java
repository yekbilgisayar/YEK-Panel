package com.yekbilgisayar.yekpanel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

final class NotificationHelper {

    static final String CHANNEL_ALERTS = "yek_alerts";
    static final String CHANNEL_REMINDERS = "yek_reminders";

    private NotificationHelper() {
    }

    static void ensureChannels(Context context) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationChannel alerts = new NotificationChannel(
                CHANNEL_ALERTS, context.getString(R.string.channel_alerts),
                NotificationManager.IMPORTANCE_HIGH);
        alerts.setDescription("Pil, sıcaklık ve sistem eşik uyarıları");
        alerts.enableVibration(true);
        manager.createNotificationChannel(alerts);

        NotificationChannel reminders = new NotificationChannel(
                CHANNEL_REMINDERS, context.getString(R.string.channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT);
        reminders.setDescription("Günlük hatırlatıcı bildirimleri");
        manager.createNotificationChannel(reminders);
    }

    static boolean canNotify(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    static void notify(Context context, int id, String channel, String title, String text) {
        if (!canNotify(context)) {
            return;
        }
        try {
            ensureChannels(context);
            Intent open = new Intent(context, MainActivity.class);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent content = PendingIntent.getActivity(context, id, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder builder = new Notification.Builder(context, channel)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(new Notification.BigTextStyle().bigText(text))
                    .setContentIntent(content)
                    .setAutoCancel(true)
                    .setShowWhen(true);
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(id, builder.build());
            }
        } catch (SecurityException ignored) {
        } catch (Exception ignored) {
        }
    }
}
