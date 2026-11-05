package com.sharedfund.app.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScannedQrPayment {
    public String bankBin;
    public String accountNumber;
    public String recipientName;
    public String transferContent;
    public Long amount;
    public String rawPayload;

    public static ScannedQrPayment parse(String payload) {
        if (payload == null || payload.trim().isEmpty()) return null;
        String raw = payload.trim();

        if (raw.startsWith("000201")) {
            ScannedQrPayment emvco = parseEmvco(raw);
            if (emvco != null && emvco.accountNumber != null) {
                return emvco;
            }
        }

        ScannedQrPayment urlResult = parseUrlOrText(raw);
        if (urlResult != null) {
            return urlResult;
        }

        ScannedQrPayment fallback = parseEmvco(raw);
        if (fallback != null && fallback.accountNumber != null) {
            return fallback;
        }

        Pattern genericPattern = Pattern.compile("(?:^|[^0-9])(9704\\d{2})[^0-9]+([0-9]{6,24})(?:[^0-9]|$)");
        Matcher mg = genericPattern.matcher(raw);
        if (mg.find()) {
            ScannedQrPayment res = new ScannedQrPayment();
            res.rawPayload = raw;
            res.bankBin = mg.group(1);
            res.accountNumber = mg.group(2);
            return res;
        }

        return fallback;
    }

    private static ScannedQrPayment parseEmvco(String raw) {
        Map<String, String> root = parseTlv(raw);
        if (root.isEmpty()) return null;
        ScannedQrPayment result = new ScannedQrPayment();
        result.rawPayload = raw;
        result.recipientName = root.get("59");
        result.transferContent = findTransferContent(root.get("62"));
        String amountStr = root.get("54");
        if (amountStr != null) {
            try { result.amount = (long) Double.parseDouble(amountStr); }
            catch (NumberFormatException ignored) {}
        }
        for (Map.Entry<String, String> entry : root.entrySet()) {
            if (!entry.getKey().matches("\\d{2}") || !entry.getValue().startsWith("00")) continue;
            Map<String, String> merchant = parseTlv(entry.getValue());
            if (!"A000000727".equalsIgnoreCase(merchant.get("00"))) continue;

            String sub01 = merchant.get("01");
            if (sub01 != null) {
                Map<String, String> subMerchant = parseTlv(sub01);
                if (!subMerchant.isEmpty()) {
                    result.bankBin = subMerchant.get("00");
                    result.accountNumber = subMerchant.get("01");
                    if (result.accountNumber == null) {
                        result.accountNumber = subMerchant.get("02");
                    }
                }
            }
            if (result.bankBin == null) {
                result.bankBin = merchant.get("01");
            }
            if (result.accountNumber == null) {
                result.accountNumber = merchant.get("02");
            }
            break;
        }

        if (result.accountNumber == null && root.containsKey("38")) {
            String val38 = root.get("38");
            if (val38 != null) {
                Pattern binPat = Pattern.compile("(9704\\d{2})");
                Matcher mBin = binPat.matcher(val38);
                if (mBin.find()) {
                    result.bankBin = mBin.group(1);
                }
                Pattern accPat = Pattern.compile("01(\\d{2})(\\d{6,24})");
                Matcher mAcc = accPat.matcher(val38);
                if (mAcc.find()) {
                    result.accountNumber = mAcc.group(2);
                }
            }
        }

        return result;
    }

    private static ScannedQrPayment parseUrlOrText(String raw) {
        Pattern vietQrImgPattern = Pattern.compile("(?:vietqr\\.io|api\\.vietqr\\.io)/image/([0-9A-Za-z]+)-([0-9A-Za-z]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = vietQrImgPattern.matcher(raw);
        if (m.find()) {
            ScannedQrPayment res = new ScannedQrPayment();
            res.rawPayload = raw;
            res.bankBin = m.group(1);
            res.accountNumber = m.group(2);
            Pattern amtPattern = Pattern.compile("[?&]amount=([0-9]+)", Pattern.CASE_INSENSITIVE);
            Matcher mAmt = amtPattern.matcher(raw);
            if (mAmt.find()) {
                try { res.amount = Long.parseLong(mAmt.group(1)); }
                catch (NumberFormatException ignored) {}
            }
            Pattern notePattern = Pattern.compile("[?&](?:addInfo|memo|note)=([^&]+)", Pattern.CASE_INSENSITIVE);
            Matcher mNote = notePattern.matcher(raw);
            if (mNote.find()) {
                try { res.transferContent = java.net.URLDecoder.decode(mNote.group(1), "UTF-8"); }
                catch (Exception e) { res.transferContent = mNote.group(1); }
            }
            return res;
        }

        Pattern bankPipePattern = Pattern.compile("^([0-9]{4,8})[|\\-/:]([0-9]{6,24})(?:[|\\-/:](.*))?$");
        Matcher mPipe = bankPipePattern.matcher(raw);
        if (mPipe.find()) {
            ScannedQrPayment res = new ScannedQrPayment();
            res.rawPayload = raw;
            res.bankBin = mPipe.group(1);
            res.accountNumber = mPipe.group(2);
            if (mPipe.group(3) != null) {
                res.recipientName = mPipe.group(3).trim();
            }
            return res;
        }

        return null;
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
