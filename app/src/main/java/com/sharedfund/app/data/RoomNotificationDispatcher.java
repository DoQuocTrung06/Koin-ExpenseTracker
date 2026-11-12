package com.sharedfund.app.data;

import android.content.Context;

import com.sharedfund.app.SharedFundApp;
import com.sharedfund.app.model.AppNotification;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.RoomMember;
import com.sharedfund.app.model.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class RoomNotificationDispatcher {

    private static volatile RoomNotificationDispatcher instance;

    private RoomNotificationDispatcher() {}

    public static RoomNotificationDispatcher getInstance() {
        if (instance == null) {
            synchronized (RoomNotificationDispatcher.class) {
                if (instance == null) {
                    instance = new RoomNotificationDispatcher();
                }
            }
        }
        return instance;
    }

    public void dispatchExpenseApprovalNotifications(Context context, String roomId,
                                                     String requesterId, String requesterName,
                                                     double amount, String note) {
        Context appCtx = context != null ? context.getApplicationContext() : SharedFundApp.getInstance();
        if (appCtx == null || roomId == null || roomId.trim().isEmpty()) return;
        RoomRepository roomRepo = RoomRepository.getInstance();
        if (roomRepo.isUserRoomOwner(roomId, requesterId)) return;

        boolean isRequesterTreasurer = roomRepo.isUserRoomTreasurer(roomId, requesterId);
        Room r = roomRepo.getRoomSync(roomId);
        String ownerId = r != null ? r.getCreatedBy() : null;
        String fundName = (r != null && r.getName() != null && !r.getName().trim().isEmpty()) ? r.getName() : "Quỹ nhóm";

        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.GERMANY);
        sym.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", sym);
        String senderDisplay = (requesterName != null && !requesterName.trim().isEmpty()) ? requesterName.trim() : "Thành viên";
        String safeNote = (note != null && !note.trim().isEmpty()) ? note.trim() : "Yêu cầu chi tiêu";
        String title = (isRequesterTreasurer ? "Phó quỹ " : "") + senderDisplay + " yêu cầu duyệt chi " + df.format(Math.abs(amount)) + " đ";
        String desc = "Nội dung: " + safeNote + (isRequesterTreasurer ? ". Cần Chủ quỹ phê duyệt." : ". Vui lòng kiểm tra và phê duyệt.");

        roomRepo.loadRoomMembers(roomId, new RoomRepository.DataCallback<List<RoomMember>>() {
            @Override
            public void onSuccess(List<RoomMember> members) {
                Set<String> targetIds = new HashSet<>();
                if (ownerId != null && !ownerId.trim().isEmpty()) {
                    targetIds.add(ownerId.trim());
                }
                if (!isRequesterTreasurer && r != null && r.getTreasurerIds() != null) {
                    for (String tid : r.getTreasurerIds()) {
                        if (tid != null && !tid.trim().isEmpty()) {
                            targetIds.add(tid.trim());
                        }
                    }
                }
                if (members != null) {
                    for (RoomMember m : members) {
                        if (m == null || m.getUserId() == null || m.getUserId().trim().isEmpty()) continue;
                        String uId = m.getUserId().trim();
                        boolean memIsOwner = "OWNER".equalsIgnoreCase(m.getRole())
                                || (ownerId != null && ownerId.equalsIgnoreCase(uId));
                        boolean memIsTreasurer = !memIsOwner && ("TREASURER".equalsIgnoreCase(m.getRole())
                                || (r != null && containsIgnoreCase(r.getTreasurerIds(), uId)));
                        if (memIsOwner) {
                            targetIds.add(uId);
                        } else if (!isRequesterTreasurer && memIsTreasurer) {
                            targetIds.add(uId);
                        }
                    }
                }
                for (String targetId : targetIds) {
                    if (requesterId != null && requesterId.equalsIgnoreCase(targetId)) continue;
                    AppNotification notif = new AppNotification(
                            UUID.randomUUID().toString(),
                            AppNotification.TYPE_EXPENSE_APPROVAL,
                            fundName,
                            title,
                            desc,
                            "Vừa xong",
                            false,
                            roomId,
                            fundName
                    );
                    NotificationManager.getInstance(appCtx).sendNotificationToUser(notif, targetId);
                }
            }

            @Override
            public void onError(String error) {
                Set<String> fallbackIds = new HashSet<>();
                if (ownerId != null && !ownerId.trim().isEmpty()) {
                    fallbackIds.add(ownerId.trim());
                }
                if (!isRequesterTreasurer && r != null && r.getTreasurerIds() != null) {
                    for (String tid : r.getTreasurerIds()) {
                        if (tid != null && !tid.trim().isEmpty()) {
                            fallbackIds.add(tid.trim());
                        }
                    }
                }
                for (String targetId : fallbackIds) {
                    if (requesterId != null && requesterId.equalsIgnoreCase(targetId)) continue;
                    AppNotification notif = new AppNotification(
                            UUID.randomUUID().toString(),
                            AppNotification.TYPE_EXPENSE_APPROVAL,
                            fundName,
                            title,
                            desc,
                            "Vừa xong",
                            false,
                            roomId,
                            fundName
                    );
                    NotificationManager.getInstance(appCtx).sendNotificationToUser(notif, targetId);
                }
            }
        });
    }

    public void dispatchExpenseResultNotification(Context context, String roomId,
                                                  Transaction tx, boolean approved, String reason,
                                                  String approverId, String approverRoleLabel) {
        Context appCtx = context != null ? context.getApplicationContext() : SharedFundApp.getInstance();
        if (appCtx == null || tx == null) return;
        String requesterId = tx.getUserId();
        if (requesterId == null || requesterId.trim().isEmpty()) return;
        if (approverId != null && approverId.equalsIgnoreCase(requesterId.trim())) return;

        RoomRepository roomRepo = RoomRepository.getInstance();
        Room r = roomId != null ? roomRepo.getRoomSync(roomId) : null;
        String fundName = (r != null && r.getName() != null && !r.getName().trim().isEmpty()) ? r.getName() : "Quỹ nhóm";

        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.GERMANY);
        sym.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", sym);
        String amountStr = df.format(Math.abs(tx.getAmount())) + " đ";
        String safeNote = (tx.getNote() != null && !tx.getNote().trim().isEmpty()) ? tx.getNote().trim() : "Khoản chi quỹ";
        String roleDisplay = (approverRoleLabel != null && !approverRoleLabel.trim().isEmpty()) ? approverRoleLabel.trim() : "Quản trị viên";

        String title;
        String desc;
        if (approved) {
            title = "Yêu cầu chi đã được duyệt";
            desc = roleDisplay + " đã duyệt khoản chi \"" + safeNote + "\" (" + amountStr + ") của bạn tại " + fundName + ".";
        } else {
            title = "Yêu cầu chi bị từ chối";
            String reasonPart = (reason != null && !reason.trim().isEmpty()) ? (" Lý do: " + reason.trim()) : "";
            desc = roleDisplay + " đã từ chối khoản chi \"" + safeNote + "\" (" + amountStr + ") tại " + fundName + "." + reasonPart;
        }

        AppNotification notif = new AppNotification(
                UUID.randomUUID().toString(),
                AppNotification.TYPE_FUND_ACTIVITY,
                fundName,
                title,
                desc,
                "Vừa xong",
                false,
                roomId,
                fundName
        );
        NotificationManager.getInstance(appCtx).sendNotificationToUser(notif, requesterId.trim());
    }

    public void dispatchJoinRequestNotification(Context context, String roomId, String roomName, Room.JoinRequestMeta req) {
        Context appCtx = context != null ? context.getApplicationContext() : SharedFundApp.getInstance();
        if (appCtx == null || roomId == null || req == null) return;
        RoomRepository roomRepo = RoomRepository.getInstance();
        Room r = roomRepo.getRoomSync(roomId);
        String ownerId = r != null ? r.getCreatedBy() : null;
        String fundName = (roomName != null && !roomName.trim().isEmpty()) ? roomName : (r != null && r.getName() != null ? r.getName() : "Quỹ nhóm");

        String requesterDisplay = (req.applicantName != null && !req.applicantName.trim().isEmpty())
                ? req.applicantName.trim()
                : ((req.email != null && !req.email.trim().isEmpty()) ? req.email.trim() : "Thành viên mới");

        String title = "Yêu cầu tham gia quỹ " + fundName;
        String desc = requesterDisplay + " đã gửi yêu cầu tham gia quỹ " + fundName + ". Vui lòng kiểm tra và phê duyệt.";

        Set<String> targetIds = new HashSet<>();
        if (ownerId != null && !ownerId.trim().isEmpty()) {
            targetIds.add(ownerId.trim());
        }
        if (r != null && r.getTreasurerIds() != null) {
            for (String tid : r.getTreasurerIds()) {
                if (tid != null && !tid.trim().isEmpty()) {
                    targetIds.add(tid.trim());
                }
            }
        }

        for (String targetId : targetIds) {
            if (targetId == null || targetId.trim().isEmpty()) continue;
            if (req.userId != null && targetId.equalsIgnoreCase(req.userId.trim())) continue;
            AppNotification notif = new AppNotification(
                    UUID.randomUUID().toString(),
                    AppNotification.TYPE_MEMBER_JOIN,
                    fundName,
                    title,
                    desc,
                    "Vừa xong",
                    false,
                    roomId,
                    fundName
            );
            NotificationManager.getInstance(appCtx).sendNotificationToUser(notif, targetId.trim());
        }
    }

    public void dispatchJoinResultNotification(Context context, String roomId, String targetUserId, String targetUserName, boolean approved, String approverName) {
        Context appCtx = context != null ? context.getApplicationContext() : SharedFundApp.getInstance();
        if (appCtx == null || targetUserId == null || targetUserId.trim().isEmpty() || targetUserId.startsWith("mock-")) return;
        RoomRepository roomRepo = RoomRepository.getInstance();
        Room r = roomId != null ? roomRepo.getRoomSync(roomId) : null;
        String fundName = (r != null && r.getName() != null && !r.getName().trim().isEmpty()) ? r.getName() : "Quỹ nhóm";

        String title;
        String desc;
        String approverDisplay = (approverName != null && !approverName.trim().isEmpty()) ? approverName.trim() : "Quản trị viên";
        if (approved) {
            title = "Yêu cầu tham gia quỹ đã được phê duyệt";
            desc = "Chúc mừng! " + approverDisplay + " đã phê duyệt yêu cầu tham gia quỹ " + fundName + " của bạn.";
        } else {
            title = "Yêu cầu tham gia quỹ bị từ chối";
            desc = approverDisplay + " đã từ chối yêu cầu tham gia quỹ " + fundName + " của bạn.";
        }

        AppNotification notif = new AppNotification(
                UUID.randomUUID().toString(),
                AppNotification.TYPE_MEMBER_JOIN,
                fundName,
                title,
                desc,
                "Vừa xong",
                false,
                roomId,
                fundName
        );
        NotificationManager.getInstance(appCtx).sendNotificationToUser(notif, targetUserId.trim());
    }

    private static boolean containsIgnoreCase(Set<String> set, String target) {
        if (set == null || target == null || target.trim().isEmpty()) return false;
        for (String item : set) {
            if (item != null && target.equalsIgnoreCase(item.trim())) return true;
        }
        return false;
    }
}
