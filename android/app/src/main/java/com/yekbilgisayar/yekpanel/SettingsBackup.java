package com.yekbilgisayar.yekpanel;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Ayarlar, notlar, hatırlatıcılar ve sunucu uçlarının cihaz içi JSON yedeği.
 * Yedek dosyası kullanıcıya SAF ile bırakılır; hiçbir sunucuya gönderilmez.
 */
public final class SettingsBackup {

    private static final String APP = "yek-panel-android";
    private static final int SCHEMA = 1;

    public static final class Summary {
        public int notes;
        public int reminders;
        public int endpoints;
    }

    private SettingsBackup() {
    }

    public static String export(Context context) {
        JSONObject root = new JSONObject();
        try {
            root.put("app", APP);
            root.put("schema", SCHEMA);
            root.put("version", "1.2.0");
            root.put("exportedAt", System.currentTimeMillis());

            SharedPreferences prefs =
                    context.getSharedPreferences(AppPrefs.PREFS, Context.MODE_PRIVATE);
            JSONObject settings = new JSONObject();
            settings.put("alerts", prefs.getBoolean(AppPrefs.K_ALERTS, true));
            settings.put("low", prefs.getInt(AppPrefs.K_LOW, AppPrefs.DEFAULT_LOW));
            settings.put("high", prefs.getInt(AppPrefs.K_HIGH, AppPrefs.DEFAULT_HIGH));
            settings.put("temp", prefs.getInt(AppPrefs.K_TEMP, AppPrefs.DEFAULT_TEMP));
            settings.put("quiet", prefs.getBoolean(AppPrefs.K_QUIET, false));
            settings.put("quietStart",
                    prefs.getInt(AppPrefs.K_QUIET_START, AppPrefs.DEFAULT_QUIET_START));
            settings.put("quietEnd",
                    prefs.getInt(AppPrefs.K_QUIET_END, AppPrefs.DEFAULT_QUIET_END));
            settings.put("refresh",
                    prefs.getInt(AppPrefs.K_REFRESH, AppPrefs.DEFAULT_REFRESH));
            settings.put("pcHost", prefs.getString(AppPrefs.K_PC_HOST, ""));
            settings.put("pcPort",
                    prefs.getInt(AppPrefs.K_PC_PORT, AppPrefs.DEFAULT_PC_PORT));
            settings.put("pcPin", prefs.getString(AppPrefs.K_PC_PIN, ""));
            settings.put("pcAuto", prefs.getBoolean(AppPrefs.K_PC_AUTO, true));
            settings.put("updateAuto", prefs.getBoolean(AppPrefs.K_UPDATE_AUTO, true));
            root.put("settings", settings);

            JSONArray notes = new JSONArray();
            for (NoteStore.Note note : new NoteStore(context).all()) {
                JSONObject item = new JSONObject();
                item.put("title", note.title);
                item.put("body", note.body);
                item.put("updatedAt", note.updatedAt);
                notes.put(item);
            }
            root.put("notes", notes);

            JSONArray reminders = new JSONArray();
            for (ReminderStore.Reminder reminder : new ReminderStore(context).all()) {
                JSONObject item = new JSONObject();
                item.put("hour", reminder.hour);
                item.put("minute", reminder.minute);
                item.put("label", reminder.label);
                item.put("enabled", reminder.enabled);
                reminders.put(item);
            }
            root.put("reminders", reminders);

            JSONArray endpoints = new JSONArray();
            for (EndpointStore.Endpoint endpoint : new EndpointStore(context).all()) {
                JSONObject item = new JSONObject();
                item.put("name", endpoint.name);
                item.put("url", endpoint.url);
                endpoints.put(item);
            }
            root.put("endpoints", endpoints);
        } catch (Exception ignored) {
        }
        return root.toString();
    }

    public static Summary importData(Context context, String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!APP.equals(root.optString("app"))) {
            throw new IllegalArgumentException("Bu dosya bir YEK Panel Android yedeği değil");
        }
        if (root.optInt("schema", 0) > SCHEMA) {
            throw new IllegalArgumentException("Yedek daha yeni bir sürümden geliyor");
        }

        JSONObject settings = root.optJSONObject("settings");
        List<NoteStore.Note> notes = new ArrayList<>();
        JSONArray notesArray = root.optJSONArray("notes");
        if (notesArray != null) {
            for (int i = 0; i < notesArray.length(); i++) {
                JSONObject item = notesArray.getJSONObject(i);
                NoteStore.Note note = new NoteStore.Note();
                note.title = item.optString("title");
                note.body = item.optString("body");
                note.updatedAt = item.optLong("updatedAt");
                notes.add(note);
            }
        }

        List<ReminderStore.Reminder> reminders = new ArrayList<>();
        JSONArray remindersArray = root.optJSONArray("reminders");
        if (remindersArray != null) {
            for (int i = 0; i < remindersArray.length(); i++) {
                JSONObject item = remindersArray.getJSONObject(i);
                ReminderStore.Reminder reminder = new ReminderStore.Reminder();
                reminder.hour = item.optInt("hour", 9);
                reminder.minute = item.optInt("minute", 0);
                reminder.label = item.optString("label", "Hatırlatıcı");
                reminder.enabled = item.optBoolean("enabled", true);
                reminders.add(reminder);
            }
        }

        List<EndpointStore.Endpoint> endpoints = new ArrayList<>();
        JSONArray endpointsArray = root.optJSONArray("endpoints");
        if (endpointsArray != null) {
            for (int i = 0; i < endpointsArray.length(); i++) {
                JSONObject item = endpointsArray.getJSONObject(i);
                EndpointStore.Endpoint endpoint = new EndpointStore.Endpoint();
                endpoint.name = item.optString("name");
                endpoint.url = item.optString("url");
                endpoints.add(endpoint);
            }
        }

        if (settings != null) {
            SharedPreferences prefs =
                    context.getSharedPreferences(AppPrefs.PREFS, Context.MODE_PRIVATE);
            int low = clamp(settings.optInt("low", AppPrefs.DEFAULT_LOW), 5, 50);
            int high = clamp(settings.optInt("high", AppPrefs.DEFAULT_HIGH), 70, 100);
            int temp = clamp(settings.optInt("temp", AppPrefs.DEFAULT_TEMP), 35, 55);
            int start = clamp(settings.optInt("quietStart", AppPrefs.DEFAULT_QUIET_START),
                    0, 23 * 60 + 59);
            int end = clamp(settings.optInt("quietEnd", AppPrefs.DEFAULT_QUIET_END),
                    0, 23 * 60 + 59);
            int refresh = settings.optInt("refresh", AppPrefs.DEFAULT_REFRESH);
            if (refresh != 2 && refresh != 5 && refresh != 10) {
                refresh = AppPrefs.DEFAULT_REFRESH;
            }
            String pcHost = settings.optString("pcHost", "").trim();
            int pcPort = clamp(settings.optInt("pcPort", AppPrefs.DEFAULT_PC_PORT),
                    1, 65535);
            String pcPin = settings.optString("pcPin", "").trim();
            if (!pcPin.matches("\\d{6}")) {
                pcPin = "";
            }
            prefs.edit()
                    .putBoolean(AppPrefs.K_ALERTS, settings.optBoolean("alerts", true))
                    .putInt(AppPrefs.K_LOW, low)
                    .putInt(AppPrefs.K_HIGH, high)
                    .putInt(AppPrefs.K_TEMP, temp)
                    .putBoolean(AppPrefs.K_QUIET, settings.optBoolean("quiet", false))
                    .putInt(AppPrefs.K_QUIET_START, start)
                    .putInt(AppPrefs.K_QUIET_END, end)
                    .putInt(AppPrefs.K_REFRESH, refresh)
                    .putString(AppPrefs.K_PC_HOST, pcHost)
                    .putInt(AppPrefs.K_PC_PORT, pcPort)
                    .putString(AppPrefs.K_PC_PIN, pcPin)
                    .putBoolean(AppPrefs.K_PC_AUTO, settings.optBoolean("pcAuto", true))
                    .putBoolean(AppPrefs.K_UPDATE_AUTO,
                            settings.optBoolean("updateAuto", true))
                    .apply();
        }

        NoteStore noteStore = new NoteStore(context);
        ReminderStore reminderStore = new ReminderStore(context);
        for (ReminderStore.Reminder old : reminderStore.all()) {
            ReminderStore.cancel(context, old.id);
        }

        Summary summary = new Summary();
        summary.notes = noteStore.replaceAll(notes);
        summary.reminders = reminderStore.replaceAll(reminders);
        new EndpointStore(context).replaceAll(endpoints);
        summary.endpoints = new EndpointStore(context).all().size();
        ReminderStore.rescheduleAll(context);
        return summary;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
