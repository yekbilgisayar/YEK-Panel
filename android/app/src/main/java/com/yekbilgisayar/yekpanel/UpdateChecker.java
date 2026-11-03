package com.yekbilgisayar.yekpanel;

import android.app.DownloadManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Android sürümünü GitHub Releases üzerinden denetler. Bu akış yalnızca
 * doğrudan APK olarak kurulan (sideload) sürüm içindir; Play sürümünde
 * MainActivity güncellemeleri Google Play'e yönlendirir.
 */
public final class UpdateChecker {

    public static final String API_URL =
            "https://api.github.com/repos/yekbilgisayar/YEK-Panel/releases?per_page=30";
    public static final String APK_NAME = "yek-panel-update.apk";
    public static final String MIME_APK = "application/vnd.android.package-archive";

    public static final class Release {
        public String tag = "";
        public String version = "";
        public String notes = "";
        public String apkUrl = "";
        public String htmlUrl = "";
        public long apkSize = 0;
    }

    public static final class Progress {
        public int percent = -1;
        public int status = DownloadManager.STATUS_PENDING;
        public long downloaded = 0;
        public long total = 0;
        public int reason = 0;
    }

    private UpdateChecker() {
    }

    public static String feedUrl(Context context) {
        if (BuildConfig.DEBUG) {
            try {
                String override = Settings.Global.getString(
                        context.getContentResolver(), "yek_update_feed");
                if (override != null
                        && (override.startsWith("http://") || override.startsWith("https://"))) {
                    return override;
                }
            } catch (Exception ignored) {
            }
        }
        return API_URL;
    }

    /** Daha yeni bir Android sürümü varsa döndürür; yoksa null. */
    public static Release check(Context context) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(feedUrl(context)).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent",
                    "YEKPanel-Android/" + BuildConfig.VERSION_NAME);
            int code = connection.getResponseCode();
            if (code != 200) {
                throw new IllegalStateException("GitHub yanıtı: HTTP " + code);
            }
            String body = read(connection);
            JSONArray releases = new JSONArray(body);
            Release best = null;
            for (int i = 0; i < releases.length(); i++) {
                JSONObject item = releases.optJSONObject(i);
                if (item == null || item.optBoolean("draft", false)
                        || item.optBoolean("prerelease", false)) {
                    continue;
                }
                String tag = item.optString("tag_name", "");
                if (!tag.startsWith("android-")) {
                    continue;
                }
                String version = tag.substring("android-".length()).trim();
                if (!version.matches("\\d+(\\.\\d+)*")) {
                    continue;
                }
                if (best != null && compareVersions(version, best.version) <= 0) {
                    continue;
                }
                JSONArray assets = item.optJSONArray("assets");
                if (assets == null) {
                    continue;
                }
                String apkUrl = "";
                long apkSize = 0;
                for (int a = 0; a < assets.length(); a++) {
                    JSONObject asset = assets.optJSONObject(a);
                    if (asset == null) {
                        continue;
                    }
                    String name = asset.optString("name", "");
                    if (name.toLowerCase(Locale.ROOT).endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", "");
                        apkSize = asset.optLong("size", 0);
                        break;
                    }
                }
                if (apkUrl.isEmpty()) {
                    continue;
                }
                Release release = new Release();
                release.tag = tag;
                release.version = version;
                release.notes = trimNotes(item.optString("body", ""));
                release.apkUrl = apkUrl;
                release.htmlUrl = item.optString("html_url", "");
                release.apkSize = apkSize;
                best = release;
            }
            if (best == null) {
                return null;
            }
            if (compareVersions(best.version, BuildConfig.VERSION_NAME) <= 0) {
                return null;
            }
            return best;
        } finally {
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String read(HttpURLConnection connection) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) > 0 && builder.length() < 512_000) {
                builder.append(buffer, 0, count);
            }
        }
        return builder.toString();
    }

    private static String trimNotes(String notes) {
        String clean = notes == null ? "" : notes.trim();
        if (clean.isEmpty()) {
            return "Bu sürüm için ayrıntılı not girilmemiş.";
        }
        if (clean.length() > 1800) {
            return clean.substring(0, 1800).trim() + "…";
        }
        return clean;
    }

    public static int compareVersions(String left, String right) {
        String[] a = safe(left).split("\\.");
        String[] b = safe(right).split("\\.");
        int size = Math.max(a.length, b.length);
        for (int i = 0; i < size; i++) {
            int x = i < a.length ? parseInt(a[i]) : 0;
            int y = i < b.length ? parseInt(b[i]) : 0;
            if (x != y) {
                return x < y ? -1 : 1;
            }
        }
        return 0;
    }

    private static String safe(String value) {
        if (value == null) {
            return "0";
        }
        String clean = value.trim();
        return clean.isEmpty() ? "0" : clean;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9].*$", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static File apkFile(Context context) {
        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) {
            dir = new File(context.getFilesDir(), "downloads");
        }
        return new File(dir, APK_NAME);
    }

    public static long download(Context context, Release release) {
        File target = apkFile(context);
        if (target.exists()) {
            //noinspection ResultOfMethodCallIgnored
            target.delete();
        }
        DownloadManager manager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) {
            return -1;
        }
        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(release.apkUrl));
        request.setTitle("YEK Panel " + release.version);
        request.setDescription("Android güncellemesi indiriliyor");
        request.setMimeType(MIME_APK);
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalFilesDir(context,
                Environment.DIRECTORY_DOWNLOADS, APK_NAME);
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(false);
        return manager.enqueue(request);
    }

    public static Progress progress(Context context, long downloadId) {
        Progress progress = new Progress();
        DownloadManager manager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null || downloadId < 0) {
            return progress;
        }
        try (Cursor cursor = manager.query(
                new DownloadManager.Query().setFilterById(downloadId))) {
            if (cursor == null || !cursor.moveToFirst()) {
                return progress;
            }
            progress.status = cursor.getInt(cursor.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_STATUS));
            progress.downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            progress.total = cursor.getLong(cursor.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            progress.reason = cursor.getInt(cursor.getColumnIndexOrThrow(
                    DownloadManager.COLUMN_REASON));
            if (progress.total > 0) {
                progress.percent = (int) Math.max(0, Math.min(100,
                        progress.downloaded * 100 / progress.total));
            }
        } catch (Exception ignored) {
        }
        return progress;
    }

    public static boolean isDownloaded(Context context) {
        File file = apkFile(context);
        return file.isFile() && file.length() > 0;
    }

    public static boolean canInstall(Context context) {
        return context.getPackageManager().canRequestPackageInstalls();
    }

    public static void openInstallPermission(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ignored) {
            try {
                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignoredAgain) {
            }
        }
    }

    /** Kurulum ekranını açar. false dönerse izin ekranı gösterilmiştir. */
    public static boolean install(Context context) {
        File file = apkFile(context);
        if (!file.isFile() || file.length() == 0) {
            return false;
        }
        if (!canInstall(context)) {
            openInstallPermission(context);
            return false;
        }
        try {
            Uri uri = UpdateFileProvider.uriFor(context);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, MIME_APK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClipData(ClipData.newRawUri("YEK Panel APK", uri));
            context.startActivity(intent);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
