package com.yekbilgisayar.yekpanel;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Günlük hatırlatıcılar; AlarmManager ile cihazda saklanır. */
public final class ReminderStore {

    public static final class Reminder {
        public int id;
        public int hour = 9;
        public int minute = 0;
        public String label = "Hatırlatıcı";
        public boolean enabled = true;
    }

    private static final String PREFS = "yek_reminders";
    private static final String KEY = "reminders";

    private final SharedPreferences prefs;

    public ReminderStore(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized List<Reminder> all() {
        List<Reminder> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                Reminder reminder = new Reminder();
                reminder.id = object.optInt("id");
                reminder.hour = object.optInt("hour", 9);
                reminder.minute = object.optInt("minute", 0);
                reminder.label = object.optString("label", "Hatırlatıcı");
                reminder.enabled = object.optBoolean("enabled", true);
                list.add(reminder);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private void save(List<Reminder> list) {
        try {
            JSONArray array = new JSONArray();
            for (Reminder reminder : list) {
                JSONObject object = new JSONObject();
                object.put("id", reminder.id);
                object.put("hour", reminder.hour);
                object.put("minute", reminder.minute);
                object.put("label", reminder.label);
                object.put("enabled", reminder.enabled);
                array.put(object);
            }
            prefs.edit().putString(KEY, array.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public synchronized Reminder add(String label, int hour, int minute) {
        List<Reminder> list = all();
        Reminder reminder = new Reminder();
        int next = 1;
        for (Reminder item : list) {
            next = Math.max(next, item.id + 1);
        }
        reminder.id = next;
        reminder.label = label == null || label.trim().isEmpty() ? "Hatırlatıcı" : label.trim();
        reminder.hour = hour;
        reminder.minute = minute;
        reminder.enabled = true;
        list.add(reminder);
        save(list);
        return reminder;
    }

    public synchronized boolean setEnabled(int id, boolean enabled) {
        List<Reminder> list = all();
        for (Reminder reminder : list) {
            if (reminder.id == id) {
                reminder.enabled = enabled;
                save(list);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean delete(int id) {
        List<Reminder> list = all();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                list.remove(i);
                save(list);
                return true;
            }
        }
        return false;
    }

    public synchronized Reminder find(int id) {
        for (Reminder reminder : all()) {
            if (reminder.id == id) {
                return reminder;
            }
        }
        return null;
    }

    public void clear() {
        prefs.edit().remove(KEY).apply();
    }

    /** Yedekten geri yükleme; mevcut hatırlatıcıların yerine geçer. */
    public synchronized int replaceAll(List<Reminder> incoming) {
        List<Reminder> clean = new ArrayList<>();
        int nextId = 1;
        if (incoming != null) {
            for (Reminder reminder : incoming) {
                if (reminder == null) {
                    continue;
                }
                Reminder item = new Reminder();
                item.id = reminder.id > 0 ? reminder.id : nextId;
                nextId = Math.max(nextId, item.id + 1);
                item.hour = Math.max(0, Math.min(23, reminder.hour));
                item.minute = Math.max(0, Math.min(59, reminder.minute));
                item.label = reminder.label == null || reminder.label.trim().isEmpty()
                        ? "Hatırlatıcı" : reminder.label.trim();
                item.enabled = reminder.enabled;
                clean.add(item);
            }
        }
        save(clean);
        return clean.size();
    }

    // ---- Zamanlama ----

    public static void schedule(Context context, Reminder reminder) {
        if (!reminder.enabled) {
            cancel(context, reminder.id);
            return;
        }
        AlarmManager manager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, reminder.hour);
        calendar.set(Calendar.MINUTE, reminder.minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                pending(context, reminder));
    }

    public static void cancel(Context context, int id) {
        AlarmManager manager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        manager.cancel(pending(context, id));
    }

    public static void rescheduleAll(Context context) {
        ReminderStore store = new ReminderStore(context);
        for (Reminder reminder : store.all()) {
            if (reminder.enabled) {
                schedule(context, reminder);
            }
        }
    }

    private static PendingIntent pending(Context context, Reminder reminder) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("id", reminder.id);
        intent.putExtra("label", reminder.label);
        return PendingIntent.getBroadcast(context, reminder.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent pending(Context context, int id) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        return PendingIntent.getBroadcast(context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
