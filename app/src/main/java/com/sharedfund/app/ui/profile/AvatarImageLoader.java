package com.sharedfund.app.ui.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.sharedfund.app.data.SafeDns;

public final class AvatarImageLoader {
    private static final long MAX_AVATAR_BYTES = 5L * 1024L * 1024L;
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .dns(SafeDns.getInstance())
            .build();

    private AvatarImageLoader() {}

    public static void load(ImageView imageView, TextView initials, String url) {
        if (url == null || url.trim().isEmpty()) {
            if (initials != null) initials.setVisibility(View.VISIBLE);
            imageView.setImageDrawable(null);
            return;
        }
        String requestedUrl = url.trim();
        imageView.setTag(requestedUrl);
        final Request request;
        try {
            request = new Request.Builder().url(requestedUrl).get().build();
        } catch (IllegalArgumentException invalidUrl) {
            return;
        }
        CLIENT.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;
                if (response.body().contentLength() > MAX_AVATAR_BYTES) return;
                byte[] bytes = response.body().bytes();
                if (bytes.length == 0 || bytes.length > MAX_AVATAR_BYTES) return;
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap == null) return;
                imageView.post(() -> {
                    if (!requestedUrl.equals(imageView.getTag())) return;
                    imageView.setImageBitmap(bitmap);
                    if (initials != null) initials.setVisibility(View.GONE);
                });
            }
        });
    }
}
