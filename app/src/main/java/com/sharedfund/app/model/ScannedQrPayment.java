package com.sharedfund.app.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScannedQrPayment {
    public String bankBin;
    public String accountNumber;
    public String recipientName;
    public String transferContent;
    public Long amount;
    public String rawPayload;

    public static ScannedQrPayment parse(String payload) {
        if (payload == null || payload.trim().isEmpty()) return null;
        Map<String, String> root = parseTlv(payload.trim());
        if (root.isEmpty()) return null;
        ScannedQrPayment result = new ScannedQrPayment();
        result.rawPayload = payload.trim();
        result.recipientName = root.get("59");
        result.transferContent = findTransferContent(root.get("62"));
        String amount = root.get("54");
        if (amount != null) {
            try { result.amount = (long) Double.parseDouble(amount); }
            catch (NumberFormatException ignored) { }
        }
        for (Map.Entry<String, String> entry : root.entrySet()) {
            if (!entry.getKey().matches("\\d{2}") || !entry.getValue().startsWith("00")) continue;
            Map<String, String> merchant = parseTlv(entry.getValue());
            if (!"A000000727".equalsIgnoreCase(merchant.get("00"))) continue;
            result.bankBin = merchant.get("01");
            result.accountNumber = merchant.get("02");
            break;
        }
        return result;
    }

    private static String findTransferContent(String additionalData) {
        if (additionalData == null) return null;
        Map<String, String> fields = parseTlv(additionalData);
        return fields.get("08");
    }

    private static Map<String, String> parseTlv(String input) {
        Map<String, String> fields = new LinkedHashMap<>();
        int offset = 0;
        while (offset + 4 <= input.length()) {
            String tag = input.substring(offset, offset + 2);
            int length;
            try { length = Integer.parseInt(input.substring(offset + 2, offset + 4)); }
            catch (NumberFormatException error) { break; }
            offset += 4;
            if (length < 0 || offset + length > input.length()) break;
            fields.put(tag, input.substring(offset, offset + length));
            offset += length;
        }
        return fields;
    }
}
