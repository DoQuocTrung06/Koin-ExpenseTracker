package com.sharedfund.app.ui.room;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.sharedfund.app.R;
import com.sharedfund.app.adapter.MemberAdapter;
import com.sharedfund.app.data.RoomRepository;
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

        // Load dữ liệu thành viên từ Repository
        RoomRepository repo = RoomRepository.getInstance();
        Room room = repo.getRoomSync(roomId);
        if (room == null) return;

        List<Transaction> allTx = repo.getTransactionsForRoom(roomId);

        // Xây dựng danh sách RoomMember từ mock data
        List<String> memberNames = room.getMembers();
        List<RoomMember> memberList = new ArrayList<>();
        double totalIncome = 0;

        for (int i = 0; i < memberNames.size(); i++) {
            String name = memberNames.get(i);
            boolean isAdmin = name.contains("Trưởng quỹ");
            // Tính tổng đóng góp (thu) của từng thành viên
            double contributed = 0;
            for (Transaction tx : allTx) {
                if (tx.isIncome() && tx.getCreatedBy() != null) {
                    String creator = tx.getCreatedBy();
                    // So sánh tên xấp xỉ
                    if (name.contains(creator) || creator.contains(name.split(" ")[0])) {
                        contributed += tx.getAmount();
                    }
                }
            }
            totalIncome += contributed;
            memberList.add(new RoomMember(
                    "uid-" + i, name, "", contributed, isAdmin, ""));
        }

        // Cập nhật header
        tvCountHeader.setText(memberList.size() + " người");

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        tvTotalContribution.setText(df.format(totalIncome) + " ₫");

        adapter.setMembers(memberList);
    }
}
