package com.sharedfund.app.data;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PaymentHelper {

    public static final String BANK_ID = "MB";
    public static final String BANK_NAME = "MBBank (Quân Đội)";
    public static final String ACCOUNT_NO = "0392934291";
    public static final String ACCOUNT_NAME = "DO QUOC TRUNG";
    public static final String TEMPLATE = "compact";

    public static String generateVietQrUrl(String joinCode, double amount) {
        String content = "SF " + (joinCode != null ? joinCode.trim().toUpperCase() : "FUND");
        String encodedContent = content;
        String encodedName = ACCOUNT_NAME;

        try {
            encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8.toString());
            encodedName = URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ignored) {
        }

        long roundedAmount = (long) amount;
        return "https://img.vietqr.io/image/" + BANK_ID + "-" + ACCOUNT_NO + "-" + TEMPLATE + ".png"
                + "?amount=" + roundedAmount
                + "&addInfo=" + encodedContent
                + "&accountName=" + encodedName;
    }

    public static String getTransferContent(String joinCode) {
        return "SF " + (joinCode != null ? joinCode.trim().toUpperCase() : "FUND");
    }
}
