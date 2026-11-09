package com.sharedfund.app.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;

import com.sharedfund.app.model.Transaction;
import com.sharedfund.app.ui.transaction.TransactionAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ReportExportHelper {

    private ReportExportHelper() {}

    public static String normalizeForFileName(String input) {
        if (input == null || input.trim().isEmpty()) return "BaoCao";
        String nfd = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(nfd)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        String clean = withoutAccents.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_");
        if (clean.startsWith("_")) clean = clean.substring(1);
        if (clean.endsWith("_")) clean = clean.substring(0, clean.length() - 1);
        return clean.isEmpty() ? "BaoCao" : clean;
    }

    private static String escapeCsvField(String field) {
        if (field == null) return "";
        return field.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ").trim();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void writeCsvContent(
            OutputStreamWriter writer,
            String reportTitle,
            String targetSubtitle,
            String dateRange,
            String reporterName,
            String reporterRole,
            List<Transaction> transactions
    ) throws Exception {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        writer.write("\uFEFF");
        writer.write("\"BÁO CÁO TÀI CHÍNH THU CHI - " + escapeCsvField(reportTitle != null ? reportTitle.toUpperCase() : "KOIN") + "\"\n");
        if (targetSubtitle != null && !targetSubtitle.isEmpty()) {
            writer.write("\"" + escapeCsvField(targetSubtitle) + "\"\n");
        }
        writer.write("\"Kỳ báo cáo:\";\"" + escapeCsvField(dateRange != null ? dateRange : "Toàn bộ") + "\";\"Ngày xuất:\";\"\t" + sdf.format(new Date()) + "\"\n");
        if (reporterName != null && !reporterName.isEmpty()) {
            writer.write("\"Người lập báo cáo:\";\"" + escapeCsvField(reporterName) + "\";\"Vai trò:\";\"" + escapeCsvField(reporterRole != null ? reporterRole : "Thành viên") + "\"\n");
        }
        writer.write("\"Trạng thái: Dữ liệu đồng bộ Realtime từ Cloud Supabase\"\n\n");

        double totalIncome = 0;
        double totalExpense = 0;

        if (transactions != null) {
            for (Transaction tx : transactions) {
                if (tx == null) continue;
                if (tx.isIncome()) totalIncome += tx.getAmount();
                else totalExpense += tx.getAmount();
            }
        }

        writer.write("\"--- TỔNG QUAN CHI TIẾT TÀI CHÍNH ---\"\n");
        writer.write(String.format(Locale.ROOT, "\"TỔNG TIỀN VÀO (+):\";\"+%s VND\"\n", formatter.format(totalIncome)));
        writer.write(String.format(Locale.ROOT, "\"TỔNG TIỀN RA (-):\";\"-%s VND\"\n", formatter.format(totalExpense)));
        writer.write(String.format(Locale.ROOT, "\"CHÊNH LỆCH TỒN QUỸ:\";\"%s%s VND\"\n\n", (totalIncome - totalExpense >= 0 ? "+" : ""), formatter.format(totalIncome - totalExpense)));

        writer.write("\"STT\";\"Thời gian\";\"Loại\";\"Danh mục\";\"Nội dung\";\"Số tiền (VND)\";\"Người thực hiện\";\"Hóa đơn\"\n");

        if (transactions != null) {
            int index = 1;
            for (Transaction tx : transactions) {
                if (tx == null) continue;
                String time = TransactionAdapter.formatDateTime(tx.getCreatedAt());
                String timeText = "\t" + time;
                String type = tx.isIncome() ? "Tiền vào (+)" : "Tiền ra (-)";
                String category = escapeCsvField(tx.getCategory() != null ? tx.getCategory() : "Khác");
                String note = escapeCsvField(tx.getNote() != null ? tx.getNote() : category);
                double amount = tx.getAmount();
                String amountFormatted = (tx.isIncome() ? "+" : "-") + formatter.format(amount);

                String creator = escapeCsvField(tx.getCreatedBy() != null ? tx.getCreatedBy() : "");
                String billUrl = tx.getBillImageUrl() != null ? tx.getBillImageUrl().trim() : "";
                String hasBill = billUrl.isEmpty() ? "Không" : "Có (" + billUrl + ")";

                writer.write(String.format(Locale.ROOT, "%d;\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\";\"%s\"\n",
                        index++, timeText, type, category, note, amountFormatted, creator, escapeCsvField(hasBill)));
            }
        }
    }

    private static void writeExcelContent(
            OutputStreamWriter writer,
            String reportTitle,
            String targetSubtitle,
            String dateRange,
            String reporterName,
            String reporterRole,
            List<Transaction> transactions
    ) throws Exception {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        double totalIncome = 0;
        double totalExpense = 0;
        if (transactions != null) {
            for (Transaction tx : transactions) {
                if (tx == null) continue;
                if (tx.isIncome()) totalIncome += tx.getAmount();
                else totalExpense += tx.getAmount();
            }
        }
        double netBalance = totalIncome - totalExpense;

        String roleDisplayName = reporterRole != null && !reporterRole.isEmpty() ? reporterRole : "Thành viên";
        if (reporterRole != null) {
            if (reporterRole.contains("Chủ quỹ") || "OWNER".equalsIgnoreCase(reporterRole)) {
                roleDisplayName = "★ Chủ quỹ";
            } else if (reporterRole.contains("Phó quỹ") || "TREASURER".equalsIgnoreCase(reporterRole) || "VICE".equalsIgnoreCase(reporterRole)) {
                roleDisplayName = "★ Phó quỹ";
            } else if (reporterRole.contains("Chủ tài khoản")) {
                roleDisplayName = "Chủ tài khoản";
            }
        }

        writer.write("<!DOCTYPE html>\n<html lang=\"vi\">\n<head>\n");
        writer.write("<meta charset=\"UTF-8\">\n");
        writer.write("<style>\n");
        writer.write("body { font-family: Calibri, 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0; color: #0F172A; }\n");
        writer.write(".title { font-size: 16pt; font-weight: 800; color: #0F172A; text-align: center; padding: 12px 0; }\n");
        writer.write(".meta { font-size: 10.5pt; color: #475569; text-align: center; }\n");
        writer.write(".card-income { background: #E3FCEF; color: #00875A; font-weight: bold; border: 1.5px solid #00875A; padding: 12px; font-size: 11pt; }\n");
        writer.write(".card-expense { background: #F1F5F9; color: #0F172A; font-weight: bold; border: 1.5px solid #64748B; padding: 12px; font-size: 11pt; }\n");
        writer.write(".card-net { background: #EBF3FF; color: #0052CC; font-weight: bold; border: 1.5px solid #0052CC; padding: 12px; font-size: 11pt; }\n");
        writer.write(".excel-table { border-collapse: collapse; width: 100%; border: 1.5px solid #64748B; }\n");
        writer.write(".excel-table th { background-color: #0052CC; color: #FFFFFF; font-weight: bold; font-size: 10pt; border: 1px solid #003E99; padding: 10px 8px; text-align: center; white-space: nowrap; }\n");
        writer.write(".excel-table td { border: 1px solid #94A3B8; padding: 8px 10px; font-size: 10pt; vertical-align: middle; }\n");
        writer.write(".cell-text { mso-number-format: '\\@'; }\n");
        writer.write(".badge-in { color: #00875A; font-weight: bold; }\n");
        writer.write(".badge-out { color: #0F172A; font-weight: bold; }\n");
        writer.write(".badge-bill { color: #0052CC; font-weight: bold; text-decoration: underline; }\n");
        writer.write("</style>\n</head>\n<body>\n");

        writer.write("<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%; margin-bottom:12px;\">\n");
        writer.write("<tr><td colspan=\"8\" class=\"title\" style=\"border:none;\">BÁO CÁO TÀI CHÍNH THU CHI - <span style=\"color:#0052CC;\">" + escapeHtml(reportTitle != null ? reportTitle.toUpperCase() : "KOIN") + "</span></td></tr>\n");
        if (targetSubtitle != null && !targetSubtitle.isEmpty()) {
            writer.write("<tr><td colspan=\"8\" class=\"meta\" style=\"border:none;\"><b>" + escapeHtml(targetSubtitle) + "</b></td></tr>\n");
        }
        writer.write("<tr><td colspan=\"8\" class=\"meta\" style=\"border:none;\">Kỳ báo cáo: " + escapeHtml(dateRange != null ? dateRange : "Toàn bộ") + " &nbsp;|&nbsp; Ngày xuất: " + sdf.format(new Date()) + "</td></tr>\n");
        if (reporterName != null && !reporterName.isEmpty()) {
            writer.write("<tr><td colspan=\"8\" class=\"meta\" style=\"border:none;\">Người lập: <b>" + escapeHtml(reporterName) + "</b> &nbsp;|&nbsp; Vai trò: <b>" + escapeHtml(roleDisplayName) + "</b></td></tr>\n");
        }
        writer.write("<tr><td colspan=\"8\" class=\"meta\" style=\"border:none;\">Trạng thái: Dữ liệu đồng bộ Realtime từ Cloud Supabase</td></tr>\n");
        writer.write("</table>\n\n");

        writer.write("<table border=\"1\" cellpadding=\"8\" cellspacing=\"0\" style=\"margin-bottom:16px; width:100%; border-collapse:collapse;\">\n");
        writer.write("<tr>\n");
        writer.write("<td colspan=\"2\" class=\"card-income\" style=\"text-align:center;\">TỔNG TIỀN VÀO (+)<br><span style=\"font-size:13pt;\">+" + formatter.format(totalIncome) + " VND</span></td>\n");
        writer.write("<td colspan=\"3\" class=\"card-expense\" style=\"text-align:center;\">TỔNG TIỀN RA (-)<br><span style=\"font-size:13pt;\">-" + formatter.format(totalExpense) + " VND</span></td>\n");
        writer.write("<td colspan=\"3\" class=\"card-net\" style=\"text-align:center;\">CHÊNH LỆCH TỒN QUỸ<br><span style=\"font-size:13pt;\">" + (netBalance >= 0 ? "+" : "") + formatter.format(netBalance) + " VND</span></td>\n");
        writer.write("</tr>\n</table>\n\n");

        writer.write("<table border=\"1\" cellpadding=\"8\" cellspacing=\"0\" class=\"excel-table\">\n");
        writer.write("<colgroup>\n");
        writer.write("<col width=\"50\">\n");
        writer.write("<col width=\"160\">\n");
        writer.write("<col width=\"120\">\n");
        writer.write("<col width=\"130\">\n");
        writer.write("<col width=\"280\">\n");
        writer.write("<col width=\"140\">\n");
        writer.write("<col width=\"160\">\n");
        writer.write("<col width=\"120\">\n");
        writer.write("</colgroup>\n");
        writer.write("<thead>\n<tr>\n");
        writer.write("<th style=\"width:50px;\">STT</th>\n");
        writer.write("<th style=\"width:160px;\">Thời gian</th>\n");
        writer.write("<th style=\"width:120px;\">Loại</th>\n");
        writer.write("<th style=\"width:130px;\">Danh mục</th>\n");
        writer.write("<th style=\"width:280px;\">Nội dung</th>\n");
        writer.write("<th style=\"width:140px;\">Số tiền (VND)</th>\n");
        writer.write("<th style=\"width:160px;\">Người thực hiện</th>\n");
        writer.write("<th style=\"width:120px;\">Hóa đơn</th>\n");
        writer.write("</tr>\n</thead>\n<tbody>\n");

        if (transactions != null) {
            int index = 1;
            for (Transaction tx : transactions) {
                if (tx == null) continue;
                String time = TransactionAdapter.formatDateTime(tx.getCreatedAt());
                boolean isIncome = tx.isIncome();
                String typeBadge = isIncome ? "<span class=\"badge-in\">Tiền vào (+)</span>" : "<span class=\"badge-out\">Tiền ra (-)</span>";
                String category = escapeHtml(tx.getCategory() != null && !tx.getCategory().isEmpty() ? tx.getCategory() : "Khác");
                String note = escapeHtml(tx.getNote() != null && !tx.getNote().isEmpty() ? tx.getNote() : category);
                double amount = tx.getAmount();
                String amountFormatted = (isIncome ? "+" : "-") + formatter.format(amount);
                String creator = escapeHtml(tx.getCreatedBy() != null ? tx.getCreatedBy() : "");

                String billCell;
                if (tx.getBillImageUrl() != null && !tx.getBillImageUrl().trim().isEmpty()) {
                    billCell = "<a href=\"" + escapeHtml(tx.getBillImageUrl().trim()) + "\" target=\"_blank\" class=\"badge-bill\">Xem ảnh ↗</a>";
                } else {
                    billCell = "<span style=\"color:#94A3B8;\">-</span>";
                }

                writer.write("<tr>\n");
                writer.write("<td align=\"center\" class=\"cell-text\">" + (index++) + "</td>\n");
                writer.write("<td align=\"center\" class=\"cell-text\">" + time + "</td>\n");
                writer.write("<td align=\"center\">" + typeBadge + "</td>\n");
                writer.write("<td>" + category + "</td>\n");
                writer.write("<td>" + note + "</td>\n");
                writer.write("<td align=\"right\" class=\"" + (isIncome ? "badge-in" : "badge-out") + " cell-text\">" + amountFormatted + "</td>\n");
                writer.write("<td>" + creator + "</td>\n");
                writer.write("<td align=\"center\">" + billCell + "</td>\n");
                writer.write("</tr>\n");
            }
        }

        writer.write("</tbody>\n</table>\n");
        writer.write("<p class=\"meta\" style=\"margin-top:20px;\">Hệ thống Quản lý Quỹ và Thu chi Koin - Dữ liệu xác thực từ Cloud Supabase</p>\n");
        writer.write("</body>\n</html>\n");
    }

    private static void writeHtmlContent(
            OutputStreamWriter writer,
            String reportTitle,
            String targetSubtitle,
            String dateRange,
            String reporterName,
            String reporterRole,
            List<Transaction> transactions
    ) throws Exception {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        double totalIncome = 0;
        double totalExpense = 0;
        if (transactions != null) {
            for (Transaction tx : transactions) {
                if (tx == null) continue;
                if (tx.isIncome()) totalIncome += tx.getAmount();
                else totalExpense += tx.getAmount();
            }
        }
        double netBalance = totalIncome - totalExpense;

        String roleBadgeClass = "role-member";
        String roleDisplayName = reporterRole != null && !reporterRole.isEmpty() ? reporterRole : "Thành viên";
        if (reporterRole != null) {
            if (reporterRole.contains("Chủ quỹ") || "OWNER".equalsIgnoreCase(reporterRole)) {
                roleBadgeClass = "role-owner";
                roleDisplayName = "★ Chủ quỹ";
            } else if (reporterRole.contains("Phó quỹ") || "TREASURER".equalsIgnoreCase(reporterRole) || "VICE".equalsIgnoreCase(reporterRole)) {
                roleBadgeClass = "role-vice";
                roleDisplayName = "★ Phó quỹ";
            } else if (reporterRole.contains("Chủ tài khoản")) {
                roleBadgeClass = "role-account";
                roleDisplayName = "Chủ tài khoản";
            }
        }

        writer.write("<!DOCTYPE html>\n<html lang=\"vi\">\n<head>\n");
        writer.write("<meta charset=\"UTF-8\">\n");
        writer.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        writer.write("<title>Báo Cáo Tài Chính - " + escapeHtml(reportTitle) + "</title>\n");
        writer.write("<style>\n");
        writer.write(":root { --primary: #0052CC; --text-primary: #0F172A; --text-secondary: #64748B; --border: #E2E8F0; --income: #10B981; --income-bg: #E3FCEF; --expense: #0F172A; --expense-bg: #F1F5F9; }\n");
        writer.write("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background: #F8FAFC; color: var(--text-primary); margin: 0; padding: 24px; }\n");
        writer.write(".container { max-width: 980px; margin: 0 auto; background: #FFFFFF; border-radius: 16px; border: 1px solid var(--border); padding: 28px; box-shadow: 0 4px 20px rgba(0,0,0,0.06); }\n");
        writer.write(".header-box { border-bottom: 1px solid var(--border); padding-bottom: 16px; margin-bottom: 22px; }\n");
        writer.write(".title { font-size: 20px; font-weight: 800; color: #0F172A; text-transform: uppercase; margin: 0 0 10px 0; letter-spacing: 0.3px; }\n");
        writer.write(".meta-line { font-size: 13.5px; color: var(--text-secondary); margin-bottom: 5px; }\n");
        writer.write(".meta-line strong { color: var(--text-primary); }\n");
        writer.write(".role-owner { display: inline-block; padding: 2px 8px; font-size: 12px; font-weight: 700; border-radius: 6px; background: #FEF3C7; color: #B45309; border: 0.8px solid #FDE047; white-space: nowrap; }\n");
        writer.write(".role-vice { display: inline-block; padding: 2px 8px; font-size: 12px; font-weight: 700; border-radius: 6px; background: #F1F5F9; color: #334155; border: 0.8px solid #CBD5E1; white-space: nowrap; }\n");
        writer.write(".role-account { display: inline-block; padding: 2px 8px; font-size: 12px; font-weight: 700; border-radius: 6px; background: #EBF3FF; color: var(--primary); border: 0.8px solid #A8C4FF; white-space: nowrap; }\n");
        writer.write(".role-member { display: inline-block; padding: 2px 8px; font-size: 12px; font-weight: 700; border-radius: 6px; background: #F1F5F9; color: var(--text-secondary); border: 0.8px solid #CBD5E1; white-space: nowrap; }\n");
        writer.write(".summary-grid { display: flex; gap: 14px; margin-bottom: 26px; flex-wrap: wrap; }\n");
        writer.write(".summary-card { flex: 1; min-width: 220px; background: #FFFFFF; border: 1px solid var(--border); border-radius: 12px; padding: 14px 18px; box-sizing: border-box; }\n");
        writer.write(".summary-card.net { background: #EBF3FF; border-color: #C0D9FF; }\n");
        writer.write(".summary-label { font-size: 11.5px; font-weight: 700; color: var(--text-secondary); text-transform: uppercase; margin-bottom: 6px; }\n");
        writer.write(".summary-card.net .summary-label { color: var(--primary); }\n");
        writer.write(".summary-val { font-size: 22px; font-weight: 800; }\n");
        writer.write(".val-income { color: var(--income); }\n");
        writer.write(".val-expense { color: var(--expense); }\n");
        writer.write(".val-net { color: var(--primary); }\n");
        writer.write("table { width: 100%; border-collapse: separate; border-spacing: 0; border: 1px solid var(--border); border-radius: 12px; overflow: hidden; margin-top: 12px; }\n");
        writer.write("th { background: #F8FAFC; color: var(--text-secondary); font-size: 12px; font-weight: 700; text-align: left; padding: 12px 14px; border-bottom: 1px solid var(--border); text-transform: uppercase; letter-spacing: 0.2px; white-space: nowrap; }\n");
        writer.write("td { padding: 12px 14px; border-bottom: 1px solid #F1F5F9; font-size: 13px; color: var(--text-primary); vertical-align: middle; }\n");
        writer.write("tr:last-child td { border-bottom: none; }\n");
        writer.write("tr:hover td { background: #F8FAFC; }\n");
        writer.write(".badge-type-in { display: inline-block; white-space: nowrap; padding: 4px 10px; border-radius: 6px; background: var(--income-bg); color: #00875A; font-weight: 700; font-size: 11.5px; }\n");
        writer.write(".badge-type-out { display: inline-block; white-space: nowrap; padding: 4px 10px; border-radius: 6px; background: var(--expense-bg); color: #334155; font-weight: 700; font-size: 11.5px; }\n");
        writer.write(".amount-in { color: var(--income); font-weight: 700; white-space: nowrap; }\n");
        writer.write(".amount-out { color: var(--expense); font-weight: 700; white-space: nowrap; }\n");
        writer.write(".badge-bill { display: inline-flex; align-items: center; justify-content: center; gap: 4px; padding: 4px 12px; border-radius: 20px; background: #EBF3FF; color: #0052CC; font-size: 11.5px; font-weight: 700; text-decoration: none; border: 1px solid #C0D9FF; white-space: nowrap; box-shadow: 0 1px 2px rgba(0,82,204,0.06); transition: all 0.15s ease-in-out; }\n");
        writer.write(".badge-bill:hover { background: #0052CC; color: #FFFFFF; border-color: #0052CC; }\n");
        writer.write(".badge-bill-empty { color: #94A3B8; font-weight: 600; font-size: 13px; text-align: center; display: block; }\n");
        writer.write(".footer-note { text-align: center; margin-top: 26px; font-size: 12px; color: #94A3B8; }\n");
        writer.write("</style>\n</head>\n<body>\n");
        writer.write("<div class=\"container\">\n");
        writer.write("<div class=\"header-box\">\n");
        writer.write("<div class=\"title\">BÁO CÁO TÀI CHÍNH THU CHI - <span style=\"color:var(--primary);\">" + escapeHtml(reportTitle != null ? reportTitle.toUpperCase() : "KOIN") + "</span></div>\n");
        if (targetSubtitle != null && !targetSubtitle.isEmpty()) {
            writer.write("<div class=\"meta-line\"><strong>" + escapeHtml(targetSubtitle) + "</strong></div>\n");
        }
        writer.write("<div class=\"meta-line\"><strong>Kỳ báo cáo:</strong> " + escapeHtml(dateRange != null ? dateRange : "Toàn bộ") + " &nbsp;|&nbsp; <strong>Ngày xuất:</strong> " + sdf.format(new Date()) + "</div>\n");
        if (reporterName != null && !reporterName.isEmpty()) {
            writer.write("<div class=\"meta-line\"><strong>Người xuất:</strong> " + escapeHtml(reporterName) + " &nbsp;|&nbsp; <strong>Vai trò:</strong> <span class=\"" + roleBadgeClass + "\">" + escapeHtml(roleDisplayName) + "</span></div>\n");
        }
        writer.write("<div class=\"meta-line\"><strong>Trạng thái:</strong> Dữ liệu đồng bộ Realtime từ Supabase Cloud</div>\n");
        writer.write("</div>\n");

        writer.write("<div class=\"summary-grid\">\n");
        writer.write("<div class=\"summary-card\"><div class=\"summary-label\">Tổng Tiền Vào (+)</div><div class=\"summary-val val-income\">+" + formatter.format(totalIncome) + " &#8363;</div></div>\n");
        writer.write("<div class=\"summary-card\"><div class=\"summary-label\">Tổng Tiền Ra (-)</div><div class=\"summary-val val-expense\">-" + formatter.format(totalExpense) + " &#8363;</div></div>\n");
        writer.write("<div class=\"summary-card net\"><div class=\"summary-label\">Chênh Lệch Tồn Quỹ</div><div class=\"summary-val val-net\">" + (netBalance >= 0 ? "+" : "") + formatter.format(netBalance) + " &#8363;</div></div>\n");
        writer.write("</div>\n");

        writer.write("<table>\n<thead>\n<tr>\n");
        writer.write("<th style=\"width:45px; text-align:center; white-space:nowrap;\">STT</th>\n");
        writer.write("<th style=\"width:140px; white-space:nowrap;\">Thời Gian</th>\n");
        writer.write("<th style=\"width:115px; text-align:center; white-space:nowrap;\">Loại</th>\n");
        writer.write("<th style=\"width:110px; white-space:nowrap;\">Danh Mục</th>\n");
        writer.write("<th style=\"min-width:180px;\">Nội Dung</th>\n");
        writer.write("<th style=\"width:130px; text-align:right; white-space:nowrap;\">Số Tiền (VND)</th>\n");
        writer.write("<th style=\"width:140px; white-space:nowrap;\">Người Thực Hiện</th>\n");
        writer.write("<th style=\"width:120px; text-align:center; white-space:nowrap;\">Hóa Đơn</th>\n");
        writer.write("</tr>\n</thead>\n<tbody>\n");

        if (transactions != null) {
            int index = 1;
            for (Transaction tx : transactions) {
                if (tx == null) continue;
                String time = TransactionAdapter.formatDateTime(tx.getCreatedAt());
                boolean isIncome = tx.isIncome();
                String typeBadge = isIncome ? "<span class=\"badge-type-in\">Tiền vào (+)</span>" : "<span class=\"badge-type-out\">Tiền ra (-)</span>";
                String category = escapeHtml(tx.getCategory() != null && !tx.getCategory().isEmpty() ? tx.getCategory() : "Khác");
                String note = escapeHtml(tx.getNote() != null && !tx.getNote().isEmpty() ? tx.getNote() : category);
                double amount = tx.getAmount();
                String amountClass = isIncome ? "amount-in" : "amount-out";
                String amountStr = (isIncome ? "+" : "-") + formatter.format(amount) + " &#8363;";
                String creator = escapeHtml(tx.getCreatedBy() != null ? tx.getCreatedBy() : "");

                String billCell;
                if (tx.getBillImageUrl() != null && !tx.getBillImageUrl().trim().isEmpty()) {
                    billCell = "<a href=\"" + escapeHtml(tx.getBillImageUrl().trim()) + "\" target=\"_blank\" class=\"badge-bill\">📄 Xem ảnh &#8599;</a>";
                } else {
                    billCell = "<span class=\"badge-bill-empty\">&mdash;</span>";
                }

                writer.write("<tr>\n");
                writer.write("<td style=\"text-align:center; white-space:nowrap;\">" + (index++) + "</td>\n");
                writer.write("<td style=\"white-space:nowrap;\">" + time + "</td>\n");
                writer.write("<td style=\"text-align:center; white-space:nowrap;\">" + typeBadge + "</td>\n");
                writer.write("<td style=\"white-space:nowrap;\">" + category + "</td>\n");
                writer.write("<td>" + note + "</td>\n");
                writer.write("<td style=\"text-align:right; white-space:nowrap;\" class=\"" + amountClass + "\">" + amountStr + "</td>\n");
                writer.write("<td style=\"white-space:nowrap;\">" + creator + "</td>\n");
                writer.write("<td style=\"text-align:center; white-space:nowrap;\">" + billCell + "</td>\n");
                writer.write("</tr>\n");
            }
        }

        writer.write("</tbody>\n</table>\n");
        writer.write("<div class=\"footer-note\">Hệ thống Quản lý Quỹ và Thu chi Koin - Dữ liệu xác thực từ Cloud Supabase</div>\n");
        writer.write("</div>\n</body>\n</html>\n");
    }

    public static File exportTransactionsToHtml(Context context, String reportTitle, String targetSubtitle, String dateRange, String reporterName, String reporterRole, List<Transaction> transactions) throws Exception {
        if (context == null || transactions == null) return null;
        File exportDir = new File(context.getCacheDir(), "reports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        String cleanTitle = normalizeForFileName(reportTitle);
        String fileName = "BaoCao_" + cleanTitle + "_" + System.currentTimeMillis() + ".html";
        File file = new File(exportDir, fileName);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writeHtmlContent(writer, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
        }
        return file;
    }

    public static File exportTransactionsToExcel(Context context, String reportTitle, String targetSubtitle, String dateRange, String reporterName, String reporterRole, List<Transaction> transactions) throws Exception {
        if (context == null || transactions == null) return null;
        File exportDir = new File(context.getCacheDir(), "reports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        String cleanTitle = normalizeForFileName(reportTitle);
        String fileName = "BaoCao_" + cleanTitle + "_" + System.currentTimeMillis() + ".xls";
        File file = new File(exportDir, fileName);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writeExcelContent(writer, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
        }
        return file;
    }

    public static File exportTransactionsToCsv(Context context, String reportTitle, String targetSubtitle, String dateRange, String reporterName, String reporterRole, List<Transaction> transactions) throws Exception {
        if (context == null || transactions == null) return null;
        File exportDir = new File(context.getCacheDir(), "reports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        String cleanTitle = normalizeForFileName(reportTitle);
        String fileName = "BaoCao_" + cleanTitle + "_" + System.currentTimeMillis() + ".csv";
        File file = new File(exportDir, fileName);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writeCsvContent(writer, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
        }
        return file;
    }

    public static List<File> exportReportBundle(
            Context context,
            String reportTitle,
            String targetSubtitle,
            String dateRange,
            String reporterName,
            String reporterRole,
            List<Transaction> transactions
    ) throws Exception {
        List<File> files = new ArrayList<>();
        File htmlFile = exportTransactionsToHtml(context, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
        if (htmlFile != null && htmlFile.exists()) files.add(htmlFile);
        File xlsFile = exportTransactionsToExcel(context, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
        if (xlsFile != null && xlsFile.exists()) files.add(xlsFile);
        File csvFile = exportTransactionsToCsv(context, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
        if (csvFile != null && csvFile.exists()) files.add(csvFile);
        return files;
    }

    public static boolean saveReportToDownloads(
            Context context,
            String reportTitle,
            String targetSubtitle,
            String dateRange,
            String reporterName,
            String reporterRole,
            List<Transaction> transactions
    ) {
        if (context == null || transactions == null) return false;
        String cleanTitle = normalizeForFileName(reportTitle);
        long ts = System.currentTimeMillis();
        String htmlFileName = "BaoCao_" + cleanTitle + "_" + ts + ".html";
        String xlsFileName = "BaoCao_" + cleanTitle + "_" + ts + ".xls";
        String csvFileName = "BaoCao_" + cleanTitle + "_" + ts + ".csv";

        boolean htmlSaved = false;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = context.getContentResolver();

                ContentValues htmlValues = new ContentValues();
                htmlValues.put(MediaStore.MediaColumns.DISPLAY_NAME, htmlFileName);
                htmlValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/html");
                htmlValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri htmlUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, htmlValues);
                if (htmlUri != null) {
                    try (OutputStream os = resolver.openOutputStream(htmlUri);
                         OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                        writeHtmlContent(writer, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
                    }
                    htmlSaved = true;
                }

                ContentValues xlsValues = new ContentValues();
                xlsValues.put(MediaStore.MediaColumns.DISPLAY_NAME, xlsFileName);
                xlsValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.ms-excel");
                xlsValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri xlsUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, xlsValues);
                if (xlsUri != null) {
                    try (OutputStream os = resolver.openOutputStream(xlsUri);
                         OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                        writeExcelContent(writer, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
                    }
                }

                ContentValues csvValues = new ContentValues();
                csvValues.put(MediaStore.MediaColumns.DISPLAY_NAME, csvFileName);
                csvValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                csvValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri csvUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, csvValues);
                if (csvUri != null) {
                    try (OutputStream os = resolver.openOutputStream(csvUri);
                         OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                        writeCsvContent(writer, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
                    }
                }
            } else {
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) downloadDir.mkdirs();

                File htmlFile = new File(downloadDir, htmlFileName);
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(htmlFile), StandardCharsets.UTF_8)) {
                    writeHtmlContent(writer, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
                }
                htmlSaved = true;

                File xlsFile = new File(downloadDir, xlsFileName);
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(xlsFile), StandardCharsets.UTF_8)) {
                    writeExcelContent(writer, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
                }

                File csvFile = new File(downloadDir, csvFileName);
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(csvFile), StandardCharsets.UTF_8)) {
                    writeCsvContent(writer, reportTitle, targetSubtitle, dateRange, reporterName, reporterRole, transactions);
                }
            }
        } catch (Exception e) {
            return false;
        }
        return htmlSaved;
    }

    public static void shareReportFile(Context context, File reportFile) {
        if (context == null || reportFile == null || !reportFile.exists()) return;
        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", reportFile);
        Intent intent = new Intent(Intent.ACTION_SEND);
        String name = reportFile.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html")) {
            intent.setType("text/html");
        } else if (name.endsWith(".xls")) {
            intent.setType("application/vnd.ms-excel");
        } else {
            intent.setType("text/csv");
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, "Báo cáo sao kê Koin");
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Chia sẻ báo cáo sao kê"));
    }

    public static void shareReportBundle(Context context, List<File> reportFiles) {
        if (context == null || reportFiles == null || reportFiles.isEmpty()) return;
        ArrayList<Uri> uris = new ArrayList<>();
        for (File file : reportFiles) {
            if (file != null && file.exists()) {
                uris.add(FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file));
            }
        }
        if (uris.isEmpty()) return;
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("*/*");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        intent.putExtra(Intent.EXTRA_SUBJECT, "Trọn bộ sao kê tài chính Koin (Excel, Web, CSV)");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Chia sẻ trọn bộ sao kê Koin"));
    }
}
