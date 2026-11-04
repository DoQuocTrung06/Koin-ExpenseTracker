package com.sharedfund.app.ui.room;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.sharedfund.app.R;
import com.sharedfund.app.util.CopyToast;

import java.util.EnumMap;
import java.util.Map;

public class InviteMemberBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_NAME = "arg_room_name";
    private static final String ARG_JOIN_CODE = "arg_join_code";

    private String roomName;
    private String joinCode;

    public static InviteMemberBottomSheet newInstance(String roomName, String joinCode) {
        InviteMemberBottomSheet sheet = new InviteMemberBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_NAME, roomName);
        args.putString(ARG_JOIN_CODE, joinCode);
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_App_BottomSheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomName = getArguments().getString(ARG_ROOM_NAME, "Quỹ nhóm");
            joinCode = getArguments().getString(ARG_JOIN_CODE, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_invite_member, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvInviteRoomTitle = view.findViewById(R.id.tvInviteRoomTitle);
        TextView tvInviteJoinCode = view.findViewById(R.id.tvInviteJoinCode);
        View layoutCopyJoinCode = view.findViewById(R.id.layoutCopyJoinCode);
        ImageView btnCopyInviteIcon = view.findViewById(R.id.btnCopyInviteIcon);
        ImageView ivInviteQrCode = view.findViewById(R.id.ivInviteQrCode);
        MaterialButton btnCloseInvite = view.findViewById(R.id.btnCloseInvite);
        MaterialButton btnShareInvite = view.findViewById(R.id.btnShareInvite);

        tvInviteRoomTitle.setText("Mời tham gia: " + roomName);
        tvInviteJoinCode.setText(joinCode);

        generateQrCode(ivInviteQrCode, joinCode);

        View.OnClickListener copyListener = v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Join Code", joinCode);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                CopyToast.show(requireContext(), requireView());
            }
        };

        if (layoutCopyJoinCode != null) {
            layoutCopyJoinCode.setOnClickListener(copyListener);
        }
        if (btnCopyInviteIcon != null) {
            btnCopyInviteIcon.setOnClickListener(copyListener);
        }

        btnShareInvite.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Mời tham gia " + roomName);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Tham gia quỹ \"" + roomName + "\" trên Koin cùng mình nhé! Nhập mã tham gia: " + joinCode);
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ mã tham gia"));
        });

        btnCloseInvite.setOnClickListener(v -> dismiss());
    }

    private void generateQrCode(ImageView imageView, String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints);
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap bitmap = encoder.createBitmap(bitMatrix);
            imageView.setImageBitmap(bitmap);
        } catch (Exception ignored) {
        }
    }
}
