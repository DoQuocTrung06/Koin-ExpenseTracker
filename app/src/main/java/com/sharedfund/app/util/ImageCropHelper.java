package com.sharedfund.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageCropHelper {

    public interface OnCropFinishedListener {
        void onSuccess(String fileUri, Bitmap bitmap);
        void onError(Exception e);
    }

    public static void cropAndSaveCoverAsync(Context context, Uri sourceUri, String targetFileName, OnCropFinishedListener listener) {
        if (context == null || sourceUri == null) {
            if (listener != null) listener.onError(new IllegalArgumentException("Context or Uri is null"));
            return;
        }

        Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                int rotationDegrees = 0;
                try (InputStream exifStream = context.getContentResolver().openInputStream(sourceUri)) {
                    if (exifStream != null) {
                        ExifInterface exif = new ExifInterface(exifStream);
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                            rotationDegrees = 90;
                        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                            rotationDegrees = 180;
                        } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                            rotationDegrees = 270;
                        }
                    }
                } catch (Exception ignored) {}

                BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
                boundsOptions.inJustDecodeBounds = true;
                try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
                    BitmapFactory.decodeStream(is, null, boundsOptions);
                }

                int rawW = boundsOptions.outWidth;
                int rawH = boundsOptions.outHeight;
                if (rotationDegrees == 90 || rotationDegrees == 270) {
                    int tmp = rawW;
                    rawW = rawH;
                    rawH = tmp;
                }

                int inSampleSize = 1;
                while ((rawW / (inSampleSize * 2)) >= 1200 && (rawH / (inSampleSize * 2)) >= 545) {
                    inSampleSize *= 2;
                }

                BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
                decodeOptions.inSampleSize = inSampleSize;
                decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565;

                Bitmap decoded;
                try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
                    decoded = BitmapFactory.decodeStream(is, null, decodeOptions);
                }

                if (decoded == null) {
                    throw new IllegalStateException("Failed to decode bitmap from stream");
                }

                if (rotationDegrees != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotationDegrees);
                    Bitmap rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), matrix, true);
                    if (rotated != decoded) {
                        decoded.recycle();
                        decoded = rotated;
                    }
                }

                float targetRatio = 2.2f;
                int curW = decoded.getWidth();
                int curH = decoded.getHeight();
                float curRatio = (float) curW / (float) curH;

                int cropW, cropH, cropX, cropY;
                if (curRatio > targetRatio) {
                    cropH = curH;
                    cropW = Math.max(1, Math.round(curH * targetRatio));
                    cropX = Math.max(0, (curW - cropW) / 2);
                    cropY = 0;
                } else {
                    cropW = curW;
                    cropH = Math.max(1, Math.round(curW / targetRatio));
                    cropX = 0;
                    cropY = Math.max(0, (curH - cropH) / 2);
                }

                if (cropX + cropW > curW) cropW = curW - cropX;
                if (cropY + cropH > curH) cropH = curH - cropY;

                Bitmap cropped = Bitmap.createBitmap(decoded, cropX, cropY, cropW, cropH);
                if (cropped != decoded) {
                    decoded.recycle();
                }

                if (cropped.getWidth() > 1200) {
                    int scaledH = Math.max(1, Math.round(1200 / targetRatio));
                    Bitmap scaled = Bitmap.createScaledBitmap(cropped, 1200, scaledH, true);
                    if (scaled != cropped) {
                        cropped.recycle();
                        cropped = scaled;
                    }
                }

                File destFile = new File(context.getFilesDir(), targetFileName);
                try (FileOutputStream fos = new FileOutputStream(destFile)) {
                    cropped.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    fos.flush();
                }

                final Bitmap finalBmp = cropped;
                final String resultUri = Uri.fromFile(destFile).toString();
                mainHandler.post(() -> {
                    if (listener != null) listener.onSuccess(resultUri, finalBmp);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onError(e);
                });
            }
        }).start();
    }
}
