package com.sharedfund.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sharedfund.app.ui.home.HomeFragment;
import com.sharedfund.app.ui.login.LoginFragment;
import com.sharedfund.app.ui.profile.ProfileFragment;
import com.sharedfund.app.ui.room.RoomDetailFragment;
import com.sharedfund.app.ui.statistics.StatisticsFragment;
import com.sharedfund.app.ui.transaction.AddTransactionFragment;
import com.sharedfund.app.ui.transaction.TransactionFragment;

/**
 * MainActivity - Điều phối và quản lý điều hướng các màn hình Fragment
 * Thống nhất theo mục 2, 3 & 4 tài liệu Giai đoạn 3
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            // Màn hình khởi đầu: Đăng nhập (LoginFragment) theo đúng sơ đồ luồng
            loadFragment(new LoginFragment(), false);
        }
    }

    /**
     * Chuyển đến màn hình Trang chủ (sau đăng nhập)
     */
    public void openHome() {
        loadFragment(new HomeFragment(), false);
    }

    /**
     * Điều hướng và thay thế Fragment vào container
     */
    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );
        ft.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            ft.addToBackStack(null);
        }
        ft.commit();
    }

    /**
     * Chuyển đến màn hình Chi tiết quỹ
     */
    public void openRoomDetail(String roomId) {
        loadFragment(RoomDetailFragment.newInstance(roomId), true);
    }

    /**
     * Chuyển đến màn hình Lịch sử giao dịch của quỹ
     */
    public void openTransactions(String roomId) {
        loadFragment(TransactionFragment.newInstance(roomId), true);
    }

    /**
     * Chuyển đến màn hình Thêm giao dịch mới
     */
    public void openAddTransaction(String roomId) {
        loadFragment(AddTransactionFragment.newInstance(roomId), true);
    }

    /**
     * Chuyển đến màn hình Thống kê quỹ
     */
    public void openStatistics(String roomId) {
        loadFragment(StatisticsFragment.newInstance(roomId), true);
    }

    /**
     * Chuyển đến màn hình Thông tin cá nhân
     */
    public void openProfile() {
        loadFragment(new ProfileFragment(), true);
    }

    /**
     * Đăng xuất - quay về màn hình đăng nhập và xóa toàn bộ back stack
     */
    public void openLogin() {
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new LoginFragment(), false);
    }
}
