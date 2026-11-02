package com.yekbilgisayar.yekpanel;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** İzlenen sunucu adresleri; yalnızca cihazda saklanır. */
public final class EndpointStore {

    public static final class Endpoint {
        public long id;
        public String name = "";
        public String url = "";
    }

    public static final int MAX_ENDPOINTS = 8;
    private static final String PREFS = "yek_endpoints";
    private static final String KEY = "endpoints";

    private final SharedPreferences prefs;

    public EndpointStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized List<Endpoint> all() {
        List<Endpoint> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                Endpoint endpoint = new Endpoint();
                endpoint.id = object.optLong("id");
                endpoint.name = object.optString("name");
                endpoint.url = object.optString("url");
                if (endpoint.id > 0 && !endpoint.url.isEmpty()) {
                    list.add(endpoint);
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private void save(List<Endpoint> list) {
        try {
            JSONArray array = new JSONArray();
            for (Endpoint endpoint : list) {
                JSONObject object = new JSONObject();
                object.put("id", endpoint.id);
                object.put("name", endpoint.name);
                object.put("url", endpoint.url);
                array.put(object);
            }
            prefs.edit().putString(KEY, array.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public synchronized Endpoint add(String name, String rawUrl) {
        String url = normalizeUrl(rawUrl);
        if (url == null || all().size() >= MAX_ENDPOINTS) {
            return null;
        }
        List<Endpoint> list = all();
        for (Endpoint existing : list) {
            if (existing.url.equalsIgnoreCase(url)) {
                return null;
            }
        }
        Endpoint endpoint = new Endpoint();
        endpoint.id = nextId(list);
        endpoint.url = url;
        endpoint.name = cleanName(name, url);
        list.add(endpoint);
        save(list);
        return endpoint;
    }

    public synchronized boolean delete(long id) {
        List<Endpoint> list = all();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                list.remove(i);
                save(list);
                return true;
            }
        }
        return false;
    }

    public synchronized void replaceAll(List<Endpoint> endpoints) {
        List<Endpoint> clean = new ArrayList<>();
        long next = System.currentTimeMillis();
        for (Endpoint endpoint : endpoints) {
            if (endpoint == null) {
                continue;
            }
            String url = normalizeUrl(endpoint.url);
            if (url == null) {
                continue;
            }
            Endpoint item = new Endpoint();
            item.id = endpoint.id > 0 ? endpoint.id : next++;
            item.url = url;
            item.name = cleanName(endpoint.name, url);
            clean.add(item);
            if (clean.size() >= MAX_ENDPOINTS) {
                break;
            }
        }
        save(clean);
    }

    public synchronized String toJson() {
        try {
            JSONArray array = new JSONArray();
            for (Endpoint endpoint : all()) {
                JSONObject object = new JSONObject();
                object.put("name", endpoint.name);
                object.put("url", endpoint.url);
                array.put(object);
            }
            return array.toString();
        } catch (Exception ignored) {
            return "[]";
        }
    }

    public static String normalizeUrl(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (!value.toLowerCase(Locale.ROOT).matches("^https?://.+")) {
            value = "https://" + value;
        }
        try {
            URL url = new URL(value);
            String host = url.getHost();
            if (host == null || host.isEmpty()) {
                return null;
            }
            return value;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String cleanName(String name, String url) {
        String result = name == null ? "" : name.trim();
        if (!result.isEmpty()) {
            return result.length() > 32 ? result.substring(0, 32) : result;
        }
        try {
            URL parsed = new URL(url);
            String host = parsed.getHost();
            return host == null || host.isEmpty() ? "Sunucu" : host;
        } catch (Exception ignored) {
            return "Sunucu";
        }
    }

    private static long nextId(List<Endpoint> list) {
        long id = System.currentTimeMillis();
        boolean collision = true;
        while (collision) {
            collision = false;
            for (Endpoint endpoint : list) {
                if (endpoint.id == id) {
                    collision = true;
                    id++;
                    break;
                }
            }
        }
        return id;
    }
}
