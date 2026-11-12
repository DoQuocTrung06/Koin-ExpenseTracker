package com.sharedfund.app.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RoomMetadataCodec {

    public static final String TREASURER_TAG = "||TREASURERS:";
    public static final String PRIVATE_TAG = "||PRIVATE:";
    public static final String REQUESTS_TAG = "||REQUESTS:";
    public static final String NUDGE_TAG = "||NUDGES:";

    private static final Gson gson = new Gson();

    private RoomMetadataCodec() {}

    public static String extractTagValue(String text, String tag) {
        if (text == null || tag == null) return null;
        int start = text.indexOf(tag);
        if (start < 0) return null;
        start += tag.length();
        int end = text.indexOf("||", start);
        return end >= 0 ? text.substring(start, end).trim() : text.substring(start).trim();
    }

    public static String stripMetadata(String raw) {
        if (raw == null) return "";
        String clean = raw;
        int idx1 = clean.indexOf(TREASURER_TAG);
        if (idx1 >= 0) clean = clean.substring(0, idx1);
        int idx2 = clean.indexOf(PRIVATE_TAG);
        if (idx2 >= 0) clean = clean.substring(0, idx2);
        int idx3 = clean.indexOf(REQUESTS_TAG);
        if (idx3 >= 0) clean = clean.substring(0, idx3);
        int idx4 = clean.indexOf(NUDGE_TAG);
        if (idx4 >= 0) clean = clean.substring(0, idx4);
        if (clean.startsWith("treasurers:")) return "";
        return clean.trim();
    }

    public static Set<String> parseTreasurers(String description) {
        Set<String> set = new HashSet<>();
        if (description == null) return set;
        String rawIds = extractTagValue(description, TREASURER_TAG);
        if (rawIds == null && description.startsWith("treasurers:")) {
            int end = description.indexOf("||");
            rawIds = end >= 0 ? description.substring("treasurers:".length(), end) : description.substring("treasurers:".length());
        }
        if (rawIds != null && !rawIds.trim().isEmpty()) {
            for (String p : rawIds.split(",")) {
                if (p != null && !p.trim().isEmpty()) {
                    set.add(p.trim());
                }
            }
        }
        return set;
    }

    public static boolean parseIsPrivate(String description) {
        if (description == null) return false;
        String val = extractTagValue(description, PRIVATE_TAG);
        return "true".equalsIgnoreCase(val);
    }

    public static List<Room.JoinRequestMeta> parsePendingJoinRequests(String description) {
        List<Room.JoinRequestMeta> list = new ArrayList<>();
        if (description == null) return list;
        String rawJson = extractTagValue(description, REQUESTS_TAG);
        if (rawJson != null && !rawJson.trim().isEmpty()) {
            try {
                Type type = new TypeToken<List<Room.JoinRequestMeta>>(){}.getType();
                List<Room.JoinRequestMeta> parsed = gson.fromJson(rawJson, type);
                if (parsed != null) list.addAll(parsed);
            } catch (Exception ignored) {}
        }
        return list;
    }

    public static Map<String, Long> parseRecurringNudgeTimestamps(String description) {
        Map<String, Long> map = new HashMap<>();
        if (description == null) return map;
        String raw = extractTagValue(description, NUDGE_TAG);
        if (raw == null || raw.trim().isEmpty()) return map;
        long now = System.currentTimeMillis();
        for (String part : raw.split(",")) {
            int eq = part.indexOf('=');
            if (eq > 0 && eq < part.length() - 1) {
                String k = part.substring(0, eq).trim();
                try {
                    long ts = Long.parseLong(part.substring(eq + 1).trim());
                    if (ts > 0L && (now - ts) < 3600000L && (ts - now) < 300000L) {
                        map.put(k, ts);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    public static String buildFullDescription(String cleanDesc, Set<String> treasurers, boolean isPrivate, List<Room.JoinRequestMeta> requests, Map<String, Long> nudges) {
        StringBuilder sb = new StringBuilder(stripMetadata(cleanDesc));
        if (treasurers != null && !treasurers.isEmpty()) {
            sb.append(TREASURER_TAG);
            boolean first = true;
            for (String t : treasurers) {
                if (t != null && !t.trim().isEmpty()) {
                    if (!first) sb.append(",");
                    sb.append(t.trim());
                    first = false;
                }
            }
        }
        if (isPrivate) {
            sb.append(PRIVATE_TAG).append("true");
        }
        if (requests != null && !requests.isEmpty()) {
            sb.append(REQUESTS_TAG).append(gson.toJson(requests));
        }
        if (nudges != null && !nudges.isEmpty()) {
            long now = System.currentTimeMillis();
            StringBuilder nsb = new StringBuilder();
            for (Map.Entry<String, Long> entry : nudges.entrySet()) {
                String k = entry.getKey();
                Long v = entry.getValue();
                if (k != null && !k.trim().isEmpty() && v != null && v > 0L && (now - v) < 3600000L && (v - now) < 300000L) {
                    if (nsb.length() > 0) nsb.append(",");
                    nsb.append(k.trim()).append("=").append(v);
                }
            }
            if (nsb.length() > 0) {
                sb.append(NUDGE_TAG).append(nsb);
            }
        }
        return sb.toString();
    }
}
