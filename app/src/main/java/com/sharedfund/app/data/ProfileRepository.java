package com.sharedfund.app.data;

import android.content.ContentResolver;
import android.net.Uri;

import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileRepository {
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final String AVATAR_BUCKET = "avatars";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public interface Callback {
        void onSuccess(String avatarUrl);
        void onError(String message);
    }

    private final OkHttpClient client = new OkHttpClient.Builder()
            .dns(SafeDns.getInstance())
            .authenticator(new SupabaseAuthenticator(com.sharedfund.app.SharedFundApp.getInstance()))
            .build();
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public void uploadAvatar(ContentResolver resolver, Uri imageUri, String userId, String accessToken,
                             Callback callback) {
        io.execute(() -> {
            try {
                if (userId == null || userId.trim().isEmpty()) throw new IOException("Tài khoản không hợp lệ.");
                if (accessToken == null || accessToken.trim().isEmpty()) throw new IOException("Phiên đăng nhập đã hết hạn.");
                String mimeType = resolver.getType(imageUri);
                if (mimeType == null || !mimeType.toLowerCase().startsWith("image/"))
                    throw new IOException("Tệp đã chọn không phải ảnh.");
                byte[] bytes = readLimited(resolver.openInputStream(imageUri));
                String extension = extensionFor(mimeType);
                String objectPath = userId + "/" + UUID.randomUUID() + "." + extension;
                String encodedPath = encodePath(objectPath);
                Request upload = new Request.Builder()
                        .url(SupabaseConfig.SUPABASE_URL + "/storage/v1/object/" + AVATAR_BUCKET + "/" + encodedPath)
                        .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", mimeType)
                        .addHeader("x-upsert", "false")
                        .post(RequestBody.create(bytes, MediaType.get(mimeType)))
                        .build();
                try (Response response = client.newCall(upload).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) throw new IOException("Storage HTTP " + response.code() + ": " + body);
                }
                String publicUrl = SupabaseConfig.SUPABASE_URL + "/storage/v1/object/public/"
                        + AVATAR_BUCKET + "/" + encodedPath;
                saveAvatarUrl(userId, accessToken, publicUrl, callback);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Không tải ảnh lên được.");
            }
        });
    }

    public void saveAvatarUrl(String userId, String accessToken, String avatarUrl, Callback callback) {
        io.execute(() -> {
            try {
                if (userId == null || userId.trim().isEmpty()) throw new IOException("Tài khoản không hợp lệ.");
                if (accessToken == null || accessToken.trim().isEmpty()) throw new IOException("Phiên đăng nhập đã hết hạn.");
                JsonObject payload = new JsonObject();
                if (avatarUrl == null) payload.add("p_avatar_url", com.google.gson.JsonNull.INSTANCE);
                else payload.addProperty("p_avatar_url", avatarUrl);
                Request update = new Request.Builder()
                        .url(SupabaseConfig.REST_URL + "/rpc/update_my_avatar_url")
                        .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(payload.toString(), JSON))
                        .build();
                try (Response response = client.newCall(update).execute()) {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) throw new IOException("Không lưu được avatar vào profiles (HTTP "
                            + response.code() + "): " + body);
                    if (!body.isEmpty()) {
                        try {
                            JsonObject result = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                            if (result.has("success") && !result.get("success").getAsBoolean()) {
                                throw new IOException(result.has("message")
                                        ? result.get("message").getAsString() : "Supabase từ chối cập nhật avatar.");
                            }
                        } catch (IllegalStateException invalidResult) {
                            throw new IOException("RPC cập nhật avatar trả về dữ liệu không hợp lệ.");
                        }
                    }
                }
                callback.onSuccess(avatarUrl);
            } catch (Exception e) {
                callback.onError(e.getMessage() != null ? e.getMessage() : "Không lưu được ảnh đại diện.");
            }
        });
    }

    private byte[] readLimited(InputStream input) throws IOException {
        if (input == null) throw new IOException("Không mở được ảnh đã chọn.");
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = stream.read(buffer)) != -1) {
                total += read;
                if (total > MAX_IMAGE_BYTES) throw new IOException("Ảnh tối đa 5 MB.");
                output.write(buffer, 0, read);
            }
            if (total == 0) throw new IOException("Ảnh trống hoặc không đọc được.");
            return output.toByteArray();
        }
    }

    private String extensionFor(String mimeType) {
        if ("image/png".equalsIgnoreCase(mimeType)) return "png";
        if ("image/webp".equalsIgnoreCase(mimeType)) return "webp";
        if ("image/gif".equalsIgnoreCase(mimeType)) return "gif";
        return "jpg";
    }

    private String encodePath(String path) throws Exception {
        String[] segments = path.split("/");
        StringBuilder encoded = new StringBuilder();
        for (String segment : segments) {
            if (encoded.length() > 0) encoded.append('/');
            encoded.append(URLEncoder.encode(segment, StandardCharsets.UTF_8.name()).replace("+", "%20"));
        }
        return encoded.toString();
    }
}
