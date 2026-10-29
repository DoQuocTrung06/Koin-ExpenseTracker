package com.sharedfund.app.ui.room;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sharedfund.app.R;
import com.sharedfund.app.adapter.MemberAdapter;
import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.model.Room;
import com.sharedfund.app.model.RoomMember;
import com.sharedfund.app.model.Transaction;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * MembersDialogFragment - BottomSheet hiển thị danh sách thành viên trong quỹ
 * Mỗi thành viên bao gồm: Avatar initials, tên, badge Trưởng quỹ,
 * số tiền đóng góp thực tế và % đóng góp so với tổng quỹ.
 */
public class MembersDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ROOM_ID = "room_id";

    private String roomId;

    public static MembersDialogFragment newInstance(String roomId) {
        MembersDialogFragment fragment = new MembersDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvCountHeader = view.findViewById(R.id.tvMemberCountHeader);
        TextView tvTotalContribution = view.findViewById(R.id.tvTotalContribution);
        RecyclerView rvMembers = view.findViewById(R.id.rvMembers);

        MemberAdapter adapter = new MemberAdapter();
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMembers.setAdapter(adapter);

        UserSessionManager session = UserSessionManager.getInstance(requireContext());
        RoomRepository repo = RoomRepository.getInstance();
        repo.configureAccessToken(session.getAccessToken());
        repo.loadRoomMembers(roomId, new RoomRepository.DataCallback<List<RoomMember>>() {
            @Override public void onSuccess(List<RoomMember> memberList) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    double totalContribution = 0;
                    for (RoomMember member : memberList) totalContribution += member.getTotalContributed();
                    tvCountHeader.setText(memberList.size() + " người");
                    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                    symbols.setGroupingSeparator('.');
                    DecimalFormat df = new DecimalFormat("#,##0", symbols);
                    tvTotalContribution.setText(df.format(totalContribution) + " ₫");
                    adapter.setMembers(memberList);
                });
            }
            @Override public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(),
                        "Không tải được thành viên từ Supabase: " + message, Toast.LENGTH_LONG).show());
            }
        });
    }
}
