package com.yekbilgisayar.yekpanel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("id", 0);
        String label = intent.getStringExtra("label");
        if (label == null || label.isEmpty()) {
            ReminderStore.Reminder reminder = new ReminderStore(context).find(id);
            if (reminder != null) {
                label = reminder.label;
            }
        }
        NotificationHelper.notify(context, 1000 + id, NotificationHelper.CHANNEL_REMINDERS,
                "YEK Panel · Hatırlatıcı", label == null ? "Hatırlatıcı zamanı" : label);

        ReminderStore store = new ReminderStore(context);
        ReminderStore.Reminder reminder = store.find(id);
        if (reminder != null && reminder.enabled) {
            ReminderStore.schedule(context, reminder);
        }
    }
}
