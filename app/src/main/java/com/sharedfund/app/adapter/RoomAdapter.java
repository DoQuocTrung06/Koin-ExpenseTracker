package com.sharedfund.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sharedfund.app.R;
import com.sharedfund.app.model.Room;

import java.util.ArrayList;
import java.util.List;

public class RoomAdapter extends RecyclerView.Adapter<RoomAdapter.RoomViewHolder> {

    public interface OnRoomClickListener {
        void onRoomClick(Room room);
        void onCopyCodeClick(Room room);
    }

    private final List<Room> rooms = new ArrayList<>();
    private final OnRoomClickListener listener;

    public RoomAdapter(OnRoomClickListener listener) {
        this.listener = listener;
    }

    public void setRooms(List<Room> newRooms) {
        this.rooms.clear();
        if (newRooms != null) {
            this.rooms.addAll(newRooms);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        Room room = rooms.get(position);
        holder.bind(room, listener);
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRoomName;
        private final TextView tvRoomDesc;
        private final TextView tvRoomBalance;
        private final TextView tvMemberCount;
        private final TextView tvJoinCode;
        private final View btnCopyCode;

        public RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomName = itemView.findViewById(R.id.tvRoomName);
            tvRoomDesc = itemView.findViewById(R.id.tvRoomDesc);
            tvRoomBalance = itemView.findViewById(R.id.tvRoomBalance);
            tvMemberCount = itemView.findViewById(R.id.tvMemberCount);
            tvJoinCode = itemView.findViewById(R.id.tvJoinCode);
            btnCopyCode = itemView.findViewById(R.id.btnCopyCode);
        }

        public void bind(final Room room, final OnRoomClickListener listener) {
            tvRoomName.setText(room.getName());
            tvRoomDesc.setText(room.getDescription() != null && !room.getDescription().isEmpty()
                    ? room.getDescription() : "Chưa có mô tả");
            tvRoomBalance.setText(room.getFormattedBalance());
            tvMemberCount.setText(room.getMemberCount() + " thành viên");
            tvJoinCode.setText(room.getJoinCode());

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onRoomClick(room);
                    }
                }
            });

            btnCopyCode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onCopyCodeClick(room);
                    }
                }
            });
        }
    }
}