package com.sharedfund.app.ui.recurring;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.sharedfund.app.R;

public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    public interface OnSwipeListener {
        void onSwipedLeft(int position);
    }

    private final OnSwipeListener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public SwipeToDeleteCallback(OnSwipeListener listener) {
        super(0, ItemTouchHelper.LEFT);
        this.listener = listener;
        paint.setColor(Color.parseColor("#EF4444"));
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        if (direction == ItemTouchHelper.LEFT && listener != null) {
            listener.onSwipedLeft(viewHolder.getBindingAdapterPosition());
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
            float cornerRadius = 16 * itemView.getResources().getDisplayMetrics().density;
            RectF background = new RectF(
                    (float) itemView.getRight() + dX,
                    (float) itemView.getTop() + (6 * itemView.getResources().getDisplayMetrics().density),
                    (float) itemView.getRight() - (16 * itemView.getResources().getDisplayMetrics().density),
                    (float) itemView.getBottom() - (6 * itemView.getResources().getDisplayMetrics().density)
            );
            c.drawRoundRect(background, cornerRadius, cornerRadius, paint);

            Drawable icon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_delete);
            if (icon != null) {
                int iconMargin = (int) (18 * itemView.getResources().getDisplayMetrics().density);
                int iconSize = (int) (24 * itemView.getResources().getDisplayMetrics().density);
                int iconTop = itemView.getTop() + (itemView.getHeight() - iconSize) / 2;
                int iconRight = itemView.getRight() - (int) (16 * itemView.getResources().getDisplayMetrics().density) - iconMargin;
                int iconLeft = iconRight - iconSize;
                int iconBottom = iconTop + iconSize;

                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                icon.setTint(Color.WHITE);
                icon.draw(c);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
