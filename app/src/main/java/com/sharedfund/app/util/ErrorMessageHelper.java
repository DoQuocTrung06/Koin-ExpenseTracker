package com.sharedfund.app.util;

public class ErrorMessageHelper {

    public static String getFriendlyMessage(String rawError) {
        if (rawError == null) {
            return "Đã xảy ra lỗi, vui lòng thử lại.";
        }
        String lower = rawError.toLowerCase();
        if (lower.contains("invalid login") || lower.contains("invalid credentials")
                || lower.contains("invalid_grant") || lower.contains("invalid_credentials")
                || lower.contains("invalid email") || lower.contains("wrong password")
                || lower.contains("email not confirmed") || lower.contains("user not found")) {
            return "Email hoặc mật khẩu không đúng.";
        }
        if (lower.contains("email already") || lower.contains("user already")) {
            return "Email này đã được đăng ký.";
        }
        if (lower.contains("signup") && lower.contains("disabled")) {
            return "Đăng ký tạm thời bị tắt, vui lòng thử lại sau.";
        }
        if (lower.contains("insufficient") && lower.contains("room")) {
            return "Số dư quỹ không đủ để thực hiện giao dịch.";
        }
        if (lower.contains("insufficient") && lower.contains("balance")) {
            return "Số dư không đủ để thực hiện giao dịch.";
        }
        if (lower.contains("42p17") || lower.contains("pgrst201") || lower.contains("pgrst")) {
            return "Hệ thống đang xử lý, vui lòng thử lại sau ít phút.";
        }
        if (lower.contains("network") || lower.contains("timeout") || lower.contains("connect")
                || lower.contains("unable to resolve") || lower.contains("socket")) {
            return "Kết nối không ổn định, vui lòng kiểm tra mạng và thử lại.";
        }
        if (lower.contains("unauthorized") || lower.contains("403") || lower.contains("401") || lower.contains("forbidden")) {
            return "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.";
        }
        if (lower.contains("500") || lower.contains("server error") || lower.contains("internal")) {
            return "Lỗi máy chủ, vui lòng thử lại sau.";
        }
        if (lower.contains("not found") || lower.contains("404")) {
            return "Không tìm thấy thông tin yêu cầu.";
        }
        if (lower.contains("rate limit") || lower.contains("too many")) {
            return "Bạn thao tác quá nhanh, vui lòng đợi một lát.";
        }
        return "Đã xảy ra lỗi, vui lòng thử lại.";
    }
}
