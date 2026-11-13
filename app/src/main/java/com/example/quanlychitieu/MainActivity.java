package com.example.quanlychitieu;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import com.example.quanlychitieu.databinding.ActivityMainBinding;
import com.example.quanlychitieu.ui.grocery.GroceryFragment;
import com.example.quanlychitieu.ui.group.GroupFragment;
import com.example.quanlychitieu.ui.home.HomeFragment;

/** Chỉ lo điều hướng giữa 3 màn hình bằng thanh điều hướng dưới. Dữ liệu nằm trong các ViewModel. */
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Chừa khoảng cho thanh trạng thái, thanh điều hướng hệ thống và bàn phím
        new WindowInsetsControllerCompat(getWindow(), binding.getRoot()).setAppearanceLightNavigationBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        binding.navigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_groups) show(GroupFragment.create(false));
            else if (id == R.id.nav_grocery) show(new GroceryFragment());
            else show(new HomeFragment());
            return true;
        });
        if (state == null) binding.navigation.setSelectedItemId(R.id.nav_home);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
                // Đang xem chi tiết nhóm thì quay về danh sách nhóm
                if (current instanceof GroupFragment && ((GroupFragment) current).closeGroup()) return;
                // Đang ở tab khác thì về Trang chủ
                if (binding.navigation.getSelectedItemId() != R.id.nav_home) {
                    binding.navigation.setSelectedItemId(R.id.nav_home);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        });
    }

    /** Mở tab Nhóm ở chế độ chi tiết của nhóm đang chọn (sau khi tạo nhóm mới hoặc nhóm demo). */
    public void openGroup() {
        binding.navigation.getMenu().findItem(R.id.nav_groups).setChecked(true);
        show(GroupFragment.create(true));
    }

    private void show(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }
}
