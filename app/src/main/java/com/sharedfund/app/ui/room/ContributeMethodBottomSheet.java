package com.sharedfund.app.ui.room;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.sharedfund.app.R;
import com.sharedfund.app.data.RoomRepository;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ContributeMethodBottomSheet extends BottomSheetDialogFragment {

    public interface OnMethodSelectedListener {
        void onSelectWallet();
        void onSelectVietQr();
    }

    private OnMethodSelectedListener listener;

    public static ContributeMethodBottomSheet newInstance() {
        return new ContributeMethodBottomSheet();
    }

    public void setListener(OnMethodSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public int getTheme() {
        return R.style.Theme_App_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_contribute_method, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout btnMethodWallet = view.findViewById(R.id.btnMethodWallet);
        LinearLayout btnMethodVietQr = view.findViewById(R.id.btnMethodVietQr);
        TextView tvMethodWalletBalance = view.findViewById(R.id.tvMethodWalletBalance);
        MaterialButton btnCancelMethod = view.findViewById(R.id.btnCancelMethod);

        double balance = RoomRepository.getInstance().getUserWalletBalance();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        tvMethodWalletBalance.setText("Số dư ví: " + formatter.format(balance) + " ₫");

        btnMethodWallet.setOnClickListener(v -> {
            if (listener != null) listener.onSelectWallet();
            dismiss();
        });

        btnMethodVietQr.setOnClickListener(v -> {
            if (listener != null) listener.onSelectVietQr();
            dismiss();
        });

        btnCancelMethod.setOnClickListener(v -> dismiss());
    }
}
