package com.sharedfund.app.ui.room;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sharedfund.app.R;

public class RoomUtilitiesBottomSheet extends BottomSheetDialogFragment {

    public interface OnUtilityActionListener {
        void onSplitBillSelected();
        void onRemindSelected();
    }

    private OnUtilityActionListener listener;

    public static RoomUtilitiesBottomSheet newInstance() {
        return new RoomUtilitiesBottomSheet();
    }

    public void setListener(OnUtilityActionListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_App_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_room_utilities, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton btnClose = view.findViewById(R.id.btnUtilitiesClose);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        View cardSplitBill = view.findViewById(R.id.cardOptionSplitBill);
        View cardRemind = view.findViewById(R.id.cardOptionRemind);

        if (cardSplitBill != null) {
            cardSplitBill.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onSplitBillSelected();
            });
        }

        if (cardRemind != null) {
            cardRemind.setOnClickListener(v -> {
                dismiss();
                if (listener != null) listener.onRemindSelected();
            });
        }
    }
}
