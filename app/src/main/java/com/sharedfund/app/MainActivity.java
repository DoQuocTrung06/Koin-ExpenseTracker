package com.sharedfund.app;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.sharedfund.app.data.RoomRepository;
import com.sharedfund.app.data.SupabaseAuthenticator;
import com.sharedfund.app.data.UserSessionManager;
import com.sharedfund.app.ui.home.HomeFragment;
import com.sharedfund.app.ui.login.LoginFragment;
import com.sharedfund.app.ui.profile.ProfileFragment;
import com.sharedfund.app.ui.room.RoomDetailFragment;
import com.sharedfund.app.ui.statistics.StatisticsFragment;
import com.sharedfund.app.ui.transaction.TransactionFragment;
import com.sharedfund.app.ui.widget.AppToaster;

public class MainActivity extends AppCompatActivity {
    private static final String SETTINGS_PREFS = "koin_ui_settings";
    private static final String KEY_DARK_THEME = "dark_theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(isDarkTheme()
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupabaseAuthenticator.setSessionExpiredListener(this::onSessionExpired);
        RoomRepository.getInstance().startRealtime();

        if (savedInstanceState == null) {
            UserSessionManager session = UserSessionManager.getInstance(this);
            if (session.isLoggedIn() && session.getUserId() != null && !session.getUserId().trim().isEmpty()) {
                if (session.isTokenExpired() && (session.getRefreshToken() == null || session.getRefreshToken().trim().isEmpty())) {
                    session.clearSession();
                    loadFragment(new LoginFragment(), false);
                } else {
                    RoomRepository.getInstance().setCurrentUser(session.getUserId(), session.getFullName());
                    RoomRepository.getInstance().configureAccessToken(session.getAccessToken());
                    loadFragment(new HomeFragment(), false);
                }
            } else {
                loadFragment(new LoginFragment(), false);
            }
        }
    }

    public boolean isDarkTheme() {
        return getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE).getBoolean(KEY_DARK_THEME, false);
    }

    public void setDarkTheme(boolean darkTheme) {
        getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(KEY_DARK_THEME, darkTheme)
                .apply();
        AppCompatDelegate.setDefaultNightMode(darkTheme
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    public void onSessionExpired() {
        runOnUiThread(() -> {
            UserSessionManager.getInstance(this).clearSession();
            AppToaster.error(this, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
            openLogin();
        });
    }

    public void openHome() {
        loadFragment(new HomeFragment(), false);
    }

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

    public void openWalletTransactions() {
        loadFragment(new com.sharedfund.app.ui.home.WalletTransactionsFragment(), true);
    }

    public void openWalletTransfer() {
        loadFragment(com.sharedfund.app.ui.home.WalletTransferFragment.newInstance(), true);
    }

    public void openFundAction(String roomId, double roomBalance) {
        loadFragment(com.sharedfund.app.ui.room.FundActionFragment.newInstance(roomId, roomBalance), true);
    }

    public void openRoomTransfer(String roomId, double roomBalance) {
        loadFragment(com.sharedfund.app.ui.room.RoomTransferFragment.newInstance(roomId, roomBalance), true);
    }

    public void openRoomDetail(String roomId) {
        loadFragment(RoomDetailFragment.newInstance(roomId), true);
    }

    public void openTransactions(String roomId) {
        loadFragment(TransactionFragment.newInstance(roomId), true);
    }

    public void openStatistics(String roomId) {
        loadFragment(StatisticsFragment.newInstance(roomId), true);
    }

    public void openProfile() {
        loadFragment(new ProfileFragment(), true);
    }

    public void openLogin() {
        getSupportFragmentManager().popBackStack(null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new LoginFragment(), false);
    }
}
