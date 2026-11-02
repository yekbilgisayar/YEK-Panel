package com.yekbilgisayar.yekpanel;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Notlar yalnızca cihazda, uygulamanın özel alanında saklanır. */
public final class NoteStore {

    public static final class Note {
        public long id;
        public String title = "";
        public String body = "";
        public long updatedAt = 0;
    }

    private static final String PREFS = "yek_notes";
    private static final String KEY = "notes";
    private static final int MAX_NOTES = 100;

    private final SharedPreferences prefs;

    public NoteStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized List<Note> all() {
        List<Note> notes = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                Note note = new Note();
                note.id = object.optLong("id");
                note.title = object.optString("title");
                note.body = object.optString("body");
                note.updatedAt = object.optLong("updatedAt");
                notes.add(note);
            }
        } catch (Exception ignored) {
        }
        Collections.sort(notes, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
        return notes;
    }

    private void save(List<Note> notes) {
        try {
            JSONArray array = new JSONArray();
            for (Note note : notes) {
                JSONObject object = new JSONObject();
                object.put("id", note.id);
                object.put("title", note.title);
                object.put("body", note.body);
                object.put("updatedAt", note.updatedAt);
                array.put(object);
            }
            prefs.edit().putString(KEY, array.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public synchronized Note add(String title, String body) {
        List<Note> notes = all();
        Note note = new Note();
        note.id = System.currentTimeMillis();
        note.title = cleanTitle(title, body);
        note.body = body == null ? "" : body.trim();
        note.updatedAt = System.currentTimeMillis();
        notes.add(0, note);
        while (notes.size() > MAX_NOTES) {
            notes.remove(notes.size() - 1);
        }
        save(notes);
        return note;
    }

    public synchronized boolean update(long id, String title, String body) {
        List<Note> notes = all();
        for (Note note : notes) {
            if (note.id == id) {
                note.title = cleanTitle(title, body);
                note.body = body == null ? "" : body.trim();
                note.updatedAt = System.currentTimeMillis();
                save(notes);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean delete(long id) {
        List<Note> notes = all();
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).id == id) {
                notes.remove(i);
                save(notes);
                return true;
            }
        }
        return false;
    }

    public synchronized void clear() {
        prefs.edit().remove(KEY).apply();
    }

    public synchronized String exportText() {
        StringBuilder builder = new StringBuilder("YEK PANEL · NOTLAR\n\n");
        for (Note note : all()) {
            builder.append(note.title).append("\n");
            builder.append("----------------------------------------\n");
            builder.append(note.body).append("\n\n");
        }
        return builder.toString();
    }

    public synchronized String exportMarkdown() {
        StringBuilder builder = new StringBuilder("# YEK Panel Notları\n\n");
        for (Note note : all()) {
            builder.append("## ").append(note.title).append("\n\n");
            builder.append(note.body).append("\n\n---\n\n");
        }
        return builder.toString();
    }

    private static String cleanTitle(String title, String body) {
        String result = title == null ? "" : title.trim();
        if (!result.isEmpty()) {
            return result;
        }
        if (body != null) {
            for (String line : body.split("\n")) {
                if (!line.trim().isEmpty()) {
                    result = line.trim();
                    break;
                }
            }
        }
        if (result.isEmpty()) {
            result = "Başlıksız not";
        }
        return result.length() > 60 ? result.substring(0, 60) + "…" : result;
    }
}
