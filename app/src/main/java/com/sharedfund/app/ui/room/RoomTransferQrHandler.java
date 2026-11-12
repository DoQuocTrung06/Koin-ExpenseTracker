package com.sharedfund.app.ui.room;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.sharedfund.app.model.ScannedQrPayment;

import java.io.InputStream;

public class RoomTransferQrHandler {

    public interface QrParseCallback {
        void onSuccess(ScannedQrPayment payment);
        void onError(String message, String detail);
    }

    public static String formatBankLabel(String bankBin) {
        if (bankBin == null) return "VietQR";
        switch (bankBin.trim()) {
            case "970422": return "MBBank (970422)";
            case "970436": return "Vietcombank (970436)";
            case "970415": return "VietinBank (970415)";
            case "970418": return "BIDV (970418)";
            case "970407": return "Techcombank (970407)";
            case "970432": return "VPBank (970432)";
            case "970423": return "TPBank (970423)";
            case "970416": return "ACB (970416)";
            case "970405": return "Agribank (970405)";
            case "970403": return "Sacombank (970403)";
            case "970448": return "OCB (970448)";
            case "970441": return "VIB (970441)";
            case "970443": return "SHB (970443)";
            case "970426": return "MSB (970426)";
            case "970440": return "SeABank (970440)";
            case "970437": return "HDBank (970437)";
            case "970428": return "Nam A Bank (970428)";
            case "970449": return "LPBank (970449)";
            case "970431": return "Eximbank (970431)";
            case "970454": return "BVBank (970454)";
            case "970433": return "VietBank (970433)";
            case "970419": return "NCB (970419)";
            case "970429": return "SCB (970429)";
            case "970438": return "BaoViet Bank (970438)";
            case "970425": return "ABBank (970425)";
            case "970452": return "Kienlongbank (970452)";
            case "970427": return "VietABank (970427)";
            case "970424": return "Shinhan Bank (970424)";
            case "970400": return "Saigonbank (970400)";
            case "970446": return "COOPBANK (970446)";
            case "970414": return "OceanBank (970414)";
            case "970408": return "GPBank (970408)";
            case "970430": return "PG Bank (970430)";
            default: return "Ngân hàng (BIN " + bankBin.trim() + ")";
        }
    }

    public static void parseQrFromUri(Context context, Uri imageUri, QrParseCallback callback) {
        if (context == null || imageUri == null) {
            if (callback != null) callback.onError("Không thể mở ảnh mã QR", "URI không hợp lệ");
            return;
        }
        try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, opts);
            int maxDim = Math.max(opts.outWidth, opts.outHeight);
            int inSampleSize = 1;
            while (maxDim / inSampleSize > 1200) {
                inSampleSize *= 2;
            }
            opts.inSampleSize = inSampleSize;
            opts.inJustDecodeBounds = false;

            Bitmap bitmap;
            try (InputStream stream2 = context.getContentResolver().openInputStream(imageUri)) {
                bitmap = BitmapFactory.decodeStream(stream2, null, opts);
            }
            if (bitmap == null) {
                if (callback != null) callback.onError("Không thể mở ảnh mã QR", "Ảnh bị lỗi hoặc không thể đọc");
                return;
            }

            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            RGBLuminanceSource source = new RGBLuminanceSource(w, h, pixels);
            Result result = null;
            try {
                result = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(source)));
            } catch (Exception ignored) {
                try {
                    result = new MultiFormatReader().decode(new BinaryBitmap(new GlobalHistogramBinarizer(source)));
                } catch (Exception ignored2) {
                }
            }

            if (result != null && result.getText() != null) {
                ScannedQrPayment parsed = ScannedQrPayment.parse(result.getText());
                if (parsed != null && parsed.accountNumber != null) {
                    if (callback != null) callback.onSuccess(parsed);
                } else {
                    if (callback != null) callback.onError("Không tìm thấy thông tin chuyển khoản VietQR trong ảnh", "Vui lòng chọn ảnh chứa rõ mã VietQR");
                }
            } else {
                if (callback != null) callback.onError("Không quét được mã QR từ ảnh này", "Vui lòng chọn ảnh chứa rõ mã VietQR hợp lệ");
            }
        } catch (Exception e) {
            if (callback != null) callback.onError("Không quét được mã QR từ ảnh này", "Vui lòng chọn ảnh chứa rõ mã VietQR hợp lệ");
        }
    }
}
