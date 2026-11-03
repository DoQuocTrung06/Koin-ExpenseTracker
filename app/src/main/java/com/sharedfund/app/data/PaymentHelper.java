package com.sharedfund.app.data;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class PaymentHelper {

    public static final String BANK_ID = "MB";
    public static final String BANK_CODE_SEPAY = "MBBank";
    public static final String ACCOUNT_NO = "0392934291";
    public static final String ACCOUNT_NAME = "DO QUOC TRUNG";
    public static final String BANK_NAME = "MB Bank";
    public static final String CONTENT_PREFIX = "SF ";
    public static final String WALLET_CONTENT_PREFIX = "SF NAP ";

    public static String generateVietQrUrl(String joinCode, double amount) {
        return generateVietQrUrl(joinCode, null, amount);
    }

    public static String generateVietQrUrl(String joinCode, String userId, double amount) {
        long roundedAmount = Math.round(amount);
        String transferContent = getTransferContent(joinCode, userId);

        String encodedContent;
        try {
            encodedContent = URLEncoder.encode(transferContent, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedContent = transferContent.replace(" ", "%20");
        }

        return String.format(Locale.US,
                "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s",
                ACCOUNT_NO, BANK_CODE_SEPAY, roundedAmount, encodedContent);
    }

    public static String getTransferContent(String joinCode) {
        return getTransferContent(joinCode, null);
    }

    public static String getTransferContent(String joinCode, String userId) {
        String safeCode = joinCode != null ? joinCode.trim().toUpperCase(Locale.ROOT) : "";
        return CONTENT_PREFIX + safeCode;
    }

    public static String getWalletTransferContent(String userId) {
        String safeUserId = userId != null ? userId.trim().toUpperCase(Locale.ROOT) : "";
        return WALLET_CONTENT_PREFIX + safeUserId;
    }

    public static String generateWalletQrUrl(String userId, double amount) {
        long roundedAmount = Math.round(amount);
        String transferContent = getWalletTransferContent(userId);
        String encodedContent;
        try {
            encodedContent = URLEncoder.encode(transferContent, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedContent = transferContent.replace(" ", "%20");
        }
        return String.format(Locale.US,
                "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s",
                ACCOUNT_NO, BANK_CODE_SEPAY, roundedAmount, encodedContent);
    }
}
