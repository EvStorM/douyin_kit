package cc.evils.douyin_kit;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.WorkerThread;
import android.database.Cursor;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class UriUtil {

    private static final String TAG = "UriUtil";

    @WorkerThread
    public static String convertToFileProviderUri(Context context, Uri sourceUri) throws IOException {
        File cacheFile = copyToCacheDir(context, sourceUri);
        return getFileProviderUri(context, cacheFile);
    }

    @WorkerThread
    public static File copyToCacheDir(Context context, Uri sourceUri) throws IOException {
        String scheme = sourceUri.getScheme();
        if (scheme == null) {
            throw new IOException("URI scheme is null");
        }

        switch (scheme.toLowerCase(Locale.ROOT)) {
            case "file":
                return new File(sourceUri.getPath());
            case "content":
                return copyContentUriToCache(context, sourceUri);
            case "http":
            case "https":
                return downloadHttpUriToCache(context, sourceUri);
            default:
                throw new IOException("Unsupported URI scheme: " + scheme);
        }
    }

    private static File copyContentUriToCache(Context context, Uri contentUri) throws IOException {
        File cacheDir = getCacheDir(context);
        String fileName = getFileName(context, contentUri);
        if (fileName == null) {
            fileName = "share_" + System.currentTimeMillis();
        }

        File destFile = new File(cacheDir, fileName);

        try (InputStream inputStream = context.getContentResolver().openInputStream(contentUri);
             OutputStream outputStream = new FileOutputStream(destFile)) {
            if (inputStream == null) {
                throw new IOException("Cannot open input stream for: " + contentUri);
            }
            copyStream(inputStream, outputStream);
        }

        return destFile;
    }

    private static File downloadHttpUriToCache(Context context, Uri httpUri) throws IOException {
        File cacheDir = getCacheDir(context);
        String fileName = getFileNameFromHttpUri(httpUri);
        if (fileName == null) {
            fileName = "share_" + System.currentTimeMillis();
        }

        File destFile = new File(cacheDir, fileName);

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            URL url = new URL(httpUri.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to download file, response code: " + responseCode);
            }

            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(destFile);

            copyStream(inputStream, outputStream);

        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {}
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return destFile;
    }

    private static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    public static String getFileProviderUri(Context context, File file) throws IOException {
        try {
            String authority = context.getPackageName() + ".DouyinFileProvider";
            Uri contentUri = FileProvider.getUriForFile(context, authority, file);
            context.grantUriPermission("com.ss.android.ugc.aweme", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return contentUri.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get FileProvider URI", e);
            throw new IOException("Failed to get FileProvider URI", e);
        }
    }

    private static File getCacheDir(Context context) {
        File cacheDir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cacheDir = context.getExternalCacheDirs()[0];
        } else {
            cacheDir = context.getExternalCacheDir();
        }

        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }

        if (cacheDir == null) {
            cacheDir = new File(Environment.getDataDirectory().getPath()
                    + "/data/" + context.getPackageName() + "/cache/");
        }

        cacheDir.mkdirs();
        return cacheDir;
    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private static String getFileNameFromHttpUri(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf('/');
            if (cut != -1) {
                String fileName = path.substring(cut + 1);
                if (!fileName.isEmpty() && fileName.contains(".")) {
                    return fileName;
                }
            }
        }
        return null;
    }

    public static String getFileExtension(String mimeType) {
        if (mimeType == null) return ".bin";
        switch (mimeType) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/webp":
                return ".webp";
            case "video/mp4":
                return ".mp4";
            case "video/webm":
                return ".webm";
            case "video/3gpp":
                return ".3gp";
            case "video/x-matroska":
                return ".mkv";
            default:
                return ".bin";
        }
    }
}
