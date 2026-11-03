package com.sharedfund.app.data;

import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.Transaction;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SearchHelper {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static String stripAccents(String s) {
        if (s == null) {
            return "";
        }
        String normalized = Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD);
        String withoutMarks = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return withoutMarks.replace('\u0111', 'd').replace('\u0110', 'd');
    }

    public static boolean containsQuery(String target, String cleanQuery) {
        if (target == null || cleanQuery == null || cleanQuery.isEmpty()) {
            return false;
        }
        return stripAccents(target).contains(cleanQuery);
    }

    public static List<Room> filterRooms(List<Room> source, String query) {
        if (source == null) {
            return new ArrayList<>();
        }
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(source);
        }
        String cleanQuery = stripAccents(query);
        List<Room> result = new ArrayList<>();
        for (Room r : source) {
            if (r == null) continue;
            boolean matchName = containsQuery(r.getName(), cleanQuery);
            boolean matchDesc = containsQuery(r.getDescription(), cleanQuery);
            boolean matchCode = containsQuery(r.getJoinCode(), cleanQuery);
            if (matchName || matchDesc || matchCode) {
                result.add(r);
            }
        }
        return result;
    }

    public static List<Transaction> filterTransactions(List<Transaction> source, String query, String statusFilter) {
        if (source == null) {
            return new ArrayList<>();
        }
        boolean hasQuery = query != null && !query.trim().isEmpty();
        boolean hasStatus = statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter);
        String cleanQuery = hasQuery ? stripAccents(query) : "";

        List<Transaction> result = new ArrayList<>();
        for (Transaction tx : source) {
            if (tx == null) continue;

            if (hasStatus) {
                String txStatus = tx.getStatus() != null ? tx.getStatus() : "";
                if (!txStatus.equalsIgnoreCase(statusFilter)) {
                    continue;
                }
            }

            if (hasQuery) {
                boolean matchNote = containsQuery(tx.getNote(), cleanQuery);
                boolean matchCategory = containsQuery(tx.getCategory(), cleanQuery);
                boolean matchUser = containsQuery(tx.getCreatedBy(), cleanQuery);
                boolean matchAmount = containsQuery(String.valueOf((long) tx.getAmount()), cleanQuery);
                if (!matchNote && !matchCategory && !matchUser && !matchAmount) {
                    continue;
                }
            }

            result.add(tx);
        }
        return result;
    }
}