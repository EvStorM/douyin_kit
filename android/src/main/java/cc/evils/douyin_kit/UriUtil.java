package cc.evils.douyin_kit;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Environment;
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
                return copyFileToCache(context, sourceUri);
            case "content":
                return copyContentUriToCache(context, sourceUri);
            case "http":
            case "https":
                return downloadHttpUriToCache(context, sourceUri);
            default:
                throw new IOException("Unsupported URI scheme: " + scheme);
        }
    }

    private static File copyFileToCache(Context context, Uri fileUri) throws IOException {
        String filePath = fileUri.getPath();
        if (filePath == null) {
            throw new IOException("File path is null");
        }

        File sourceFile = new File(filePath);
        if (!sourceFile.exists()) {
            throw new IOException("File does not exist: " + filePath);
        }

        File cacheDir = getCacheDir(context);
        File destFile = new File(cacheDir, sourceFile.getName());

        // 如果目标文件已存在且大小一致，直接返回
        if (destFile.exists() && destFile.length() == sourceFile.length()) {
            return destFile;
        }

        try (InputStream inputStream = new java.io.FileInputStream(sourceFile);
             OutputStream outputStream = new FileOutputStream(destFile)) {
            copyStream(inputStream, outputStream);
        }

        return destFile;
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
            // 动态获取 FileProvider 的 authority，避免硬编码
            String authority = getFileProviderAuthority(context);
            Uri contentUri = FileProvider.getUriForFile(context, authority, file);
            // 抖音主应用和极速版都需要授权
            context.grantUriPermission("com.ss.android.ugc.aweme", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.grantUriPermission("com.ss.android.ugc.aweme.lite", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return contentUri.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get FileProvider URI", e);
            throw new IOException("Failed to get FileProvider URI", e);
        }
    }

    private static String getFileProviderAuthority(Context context) throws PackageManager.NameNotFoundException {
        try {
            // 动态从 PackageManager 获取 FileProvider 的 authority
            ProviderInfo providerInfo = context.getPackageManager()
                    .getProviderInfo(new ComponentName(context, DouyinFileProvider.class), PackageManager.MATCH_DEFAULT_ONLY);
            return providerInfo.authority;
        } catch (PackageManager.NameNotFoundException e) {
            // 降级方案：使用硬编码的 authority
            Log.w(TAG, "Failed to get authority dynamically, falling back to hardcoded value", e);
            return context.getPackageName() + ".DouyinFileProvider";
        }
    }

    private static File getCacheDir(Context context) {
        File cacheDir;
        // 优先使用 getExternalFilesDir(null)，与抖音官方文档建议保持一致
        // 该目录对应 external-files-path，在 FileProvider 配置中已支持
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir != null) {
            cacheDir = externalFilesDir;
        } else {
            // 降级使用 getExternalCacheDir
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
