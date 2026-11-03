package com.sharedfund.app.util;

public class ErrorMessageHelper {

    public static String getFriendlyMessage(String rawError) {
        if (rawError == null) {
            return "Đã xảy ra lỗi, vui lòng thử lại.";
        }
        String lower = rawError.toLowerCase();
        if (lower.contains("insufficient") && lower.contains("room")) {
            return "Số dư quỹ không đủ để thực hiện giao dịch.";
        }
        if (lower.contains("insufficient") && lower.contains("balance")) {
            return "Số dư không đủ để thực hiện giao dịch.";
        }
        if (lower.contains("42p17") || lower.contains("pgrst201") || lower.contains("pgrst")) {
            return "Hệ thống đang xử lý, vui lòng thử lại sau ít phút.";
        }
        if (lower.contains("network") || lower.contains("timeout") || lower.contains("connect")) {
            return "Kết nối không ổn định, vui lòng kiểm tra mạng và thử lại.";
        }
        if (lower.contains("unauthorized") || lower.contains("403") || lower.contains("401")) {
            return "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.";
        }
        if (lower.contains("500") || lower.contains("server error")) {
            return "Lỗi máy chủ, vui lòng thử lại sau.";
        }
        if (lower.contains("not found") || lower.contains("404")) {
            return "Không tìm thấy thông tin yêu cầu.";
        }
        return "Đã xảy ra lỗi, vui lòng thử lại.";
    }
}
