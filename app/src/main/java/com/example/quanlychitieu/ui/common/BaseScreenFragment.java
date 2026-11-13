package com.example.quanlychitieu.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.quanlychitieu.MainActivity;
import com.example.quanlychitieu.data.local.BackupStore;
import com.example.quanlychitieu.data.model.Expense;
import com.example.quanlychitieu.data.model.Group;
import com.example.quanlychitieu.databinding.DialogGroupBinding;
import com.example.quanlychitieu.databinding.FragmentWorkspaceBinding;
import com.example.quanlychitieu.ui.group.ExpenseDialog;
import com.example.quanlychitieu.ui.group.GroupViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Phần dùng chung của 3 màn hình Trang chủ, Nhóm và Đi chợ:
 * - Khung giao diện fragment_workspace.xml: tiêu đề, nút chọn nhóm, tab, danh sách, nút "+".
 * - Menu ⋮ ở góc trên: sao lưu, khôi phục, tạo dữ liệu demo, đổi tên / xóa nhóm.
 * - Hàm mở hộp thoại nhập liệu, hộp thoại xác nhận xóa, hộp thoại tạo nhóm và khoản chi.
 * Mỗi màn hình con chỉ cần viết render() (vẽ danh sách) và xử lý khi bấm vào từng dòng.
 */
public abstract class BaseScreenFragment extends Fragment implements RowAdapter.Actions {

    /** Việc cần làm khi bấm Lưu. Ném IllegalArgumentException nếu dữ liệu nhập chưa hợp lệ. */
    public interface FormAction {
        void save(Runnable close);
    }

    protected FragmentWorkspaceBinding b;
    protected GroupViewModel groupModel;
    /** Dữ liệu mới nhất của nhóm đang chọn; null khi chưa tải xong. */
    protected GroupViewModel.State groupState;
    protected RowAdapter adapter;
    /** Hộp thoại nhập liệu đang mở (để khóa nút Lưu khi đang lưu). */
    private AlertDialog editor;
    /** Đang chọn tab bằng code, không phải người dùng bấm. */
    private boolean syncingTabs;

    private final ActivityResultLauncher<String> exportFile = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri == null) return;
                groupModel.exportBackup(requireContext().getContentResolver(), uri, () -> toast("Đã lưu bản sao lưu"));
            });

    private final ActivityResultLauncher<String[]> importFile = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                groupModel.readBackup(requireContext().getContentResolver(), uri, this::confirmRestore);
            });

    // ===== Phần màn hình con phải / có thể viết =====

    /** Vẽ lại tiêu đề và danh sách từ dữ liệu hiện có. */
    protected abstract void render();

    /** Khi bấm nút "+" ở góc dưới. */
    protected abstract void onAddClick();

    /** Tên các tab; mặc định không có tab. */
    protected String[] tabTitles() {
        return new String[0];
    }

    protected int currentTab() {
        return 0;
    }

    protected void onTabSelected(int index) {
    }

    /** Khi dữ liệu nhóm thay đổi. Mặc định chỉ vẽ lại. */
    protected void onGroupState(GroupViewModel.State state) {
        render();
    }

    /** Có hiện "Đổi tên nhóm" và "Xóa nhóm" trong menu ⋮ không. */
    protected boolean showsGroupActions() {
        return false;
    }

    protected void onFilterClick() {
    }

    protected void onConvertClick() {
    }

    @Override
    public void check(RowAdapter.Row row, boolean checked) {
    }

    // ===== Vòng đời =====

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        b = FragmentWorkspaceBinding.inflate(inflater, parent, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle state) {
        groupModel = new ViewModelProvider(requireActivity()).get(GroupViewModel.class);

        adapter = new RowAdapter(this);
        b.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.list.setAdapter(adapter);
        b.list.setItemAnimator(null);

        for (String title : tabTitles()) b.tabs.addTab(b.tabs.newTab().setText(title));
        if (b.tabs.getTabCount() > 0) b.tabs.selectTab(b.tabs.getTabAt(currentTab()));
        b.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (syncingTabs) return;
                BaseScreenFragment.this.onTabSelected(tab.getPosition());
                render();
                b.list.scrollToPosition(0);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        b.groupPicker.setOnClickListener(v -> pickGroup());
        b.groupMenu.setOnClickListener(this::showGroupMenu);
        b.filter.setOnClickListener(v -> onFilterClick());
        b.convert.setOnClickListener(v -> onConvertClick());
        b.add.setOnClickListener(v -> {
            if (groupState != null) onAddClick();
        });

        groupModel.getState().observe(getViewLifecycleOwner(), s -> {
            groupState = s;
            onGroupState(s);
        });
        BaseViewModel.busy().observe(getViewLifecycleOwner(), busy -> {
            b.loading.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
            b.add.setEnabled(!busy);
            if (editor != null && editor.isShowing()) editor.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!busy);
        });
        BaseViewModel.error().observe(getViewLifecycleOwner(), message -> {
            if (message == null) return;
            toast(message);
            adapter.notifyItemRangeChanged(0, adapter.getItemCount()); // mở lại ô tích vừa bị khóa
            BaseViewModel.clearError();
        });
    }

    @Override
    public void onDestroyView() {
        if (editor != null) editor.dismiss();
        b.list.setAdapter(null);
        b = null;
        super.onDestroyView();
    }

    // ===== Tiêu đề =====

    protected void showHeader(String eyebrow, String title, String subtitle) {
        b.eyebrow.setText(eyebrow);
        b.title.setText(title);
        b.subtitle.setText(subtitle);
        b.groupMenu.setVisibility(View.VISIBLE);
        b.groupMenu.setContentDescription("Tùy chọn và sao lưu");
    }

    /** Nút "Tên nhóm ▾" để đổi nhóm đang xem. */
    protected void showGroupPicker(boolean visible) {
        b.groupPicker.setVisibility(visible ? View.VISIBLE : View.GONE);
        b.groupPicker.setText(groupState.groupName() + "  ▾");
    }

    /** Chọn tab bằng code mà không gọi lại onTabSelected(). */
    protected void selectTab(int index) {
        syncingTabs = true;
        b.tabs.selectTab(b.tabs.getTabAt(index));
        syncingTabs = false;
    }

    private void pickGroup() {
        if (groupState == null) return;
        final List<Group> groups = groupState.groups;
        String[] names = new String[groups.size()];
        for (int i = 0; i < names.length; i++) names[i] = groups.get(i).name;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chọn nhóm")
                .setItems(names, (d, which) -> groupModel.selectGroup(groups.get(which).id))
                .setNegativeButton("Đóng", null)
                .show();
    }

    // ===== Menu ⋮ =====

    private void showGroupMenu(View anchor) {
        Group current = groupState == null ? null : groupState.currentGroup();
        boolean groupActions = showsGroupActions() && current != null;
        List<String> items = new ArrayList<>();
        if (groupActions) items.addAll(Arrays.asList("Đổi tên nhóm", "Xóa nhóm"));
        items.addAll(Arrays.asList("Sao lưu dữ liệu", "Khôi phục dữ liệu", "Tạo dữ liệu demo"));
        showPopup(anchor, items, choice -> {
            switch (choice) {
                case "Sao lưu dữ liệu":
                    exportFile.launch("SplitMate-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".json");
                    break;
                case "Khôi phục dữ liệu":
                    importFile.launch(new String[]{"application/json", "text/plain", "application/octet-stream"});
                    break;
                case "Tạo dữ liệu demo":
                    confirmDemo();
                    break;
                case "Đổi tên nhóm":
                    groupEditor(current);
                    break;
                case "Xóa nhóm":
                    confirmDelete("Xóa nhóm và toàn bộ dữ liệu?", () -> groupModel.deleteGroup(current.id));
                    break;
            }
        });
    }

    private void confirmDemo() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tạo nhóm trình diễn?")
                .setMessage("Thêm nhóm Demo · Phòng trọ xanh với 3 thành viên, khoản chi và danh sách đi chợ. Các nhóm của bạn được giữ nguyên.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tạo demo", (d, w) -> groupModel.createDemo(id -> {
                    groupModel.selectGroup(id);
                    openGroupScreen();
                }))
                .show();
    }

    private void confirmRestore(BackupStore.Archive archive) {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Khôi phục dữ liệu?")
                .setMessage(archive.groupCount() + " nhóm, " + archive.expenseCount() + " khoản chi, " + archive.groceryCount()
                        + " món hàng.\nDữ liệu sẽ được thêm thành nhóm mới có hậu tố (khôi phục). Các nhóm hiện có được giữ nguyên.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Khôi phục", (d, w) -> groupModel.restoreBackup(archive, () -> toast("Đã khôi phục thành nhóm mới")))
                .show();
    }

    // ===== Hộp thoại dùng chung =====

    /** Tạo nhóm (old == null) hoặc đổi tên nhóm. Layout: dialog_group.xml */
    protected void groupEditor(Group old) {
        DialogGroupBinding form = DialogGroupBinding.inflate(getLayoutInflater());
        if (old != null) form.name.setText(old.name);
        showForm(old == null ? "Tạo nhóm" : "Đổi tên nhóm", form.getRoot(), false, close -> {
            String name = Forms.required(form.name);
            groupModel.saveGroup(old == null ? 0 : old.id, name, id -> {
                close.run();
                if (old == null) { // nhóm mới: mở luôn nhóm đó
                    groupModel.selectGroup(id);
                    openGroupScreen();
                }
            });
        });
    }

    /** Thêm (old == null) hoặc sửa khoản chi của nhóm đang chọn. Layout: dialog_expense.xml */
    protected void openExpenseEditor(Expense old) {
        if (groupState == null) return;
        final long group = groupState.groupId;
        ExpenseDialog.show(this, groupState, old, Collections.emptyList(),
                (title, amount, payerId, category, participants, close) -> groupModel.saveExpense(
                        old == null ? 0 : old.id, group, title, amount, payerId, category, participants, close));
    }

    /**
     * Mở hộp thoại có nút Hủy / Lưu với nội dung là một layout dialog_*.xml đã inflate.
     * Bấm Lưu: gọi action (kiểm tra dữ liệu rồi gọi ViewModel). Lỗi nhập liệu thì hiện thông báo, không đóng.
     */
    public void showForm(String title, View content, boolean fullscreen, FormAction action) {
        editor = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(content)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null) // gắn xử lý riêng bên dưới để không tự đóng
                .create();
        final AlertDialog dialog = editor;
        dialog.setOnShowListener(d -> {
            if (fullscreen && dialog.getWindow() != null) dialog.getWindow().setLayout(-1, -1);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    action.save(dialog::dismiss);
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                } catch (IllegalArgumentException e) {
                    toast(e.getMessage());
                }
            });
        });
        dialog.show();
    }

    protected void confirmDelete(String title, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage("Thao tác này không thể hoàn tác.")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> onConfirm.run())
                .show();
    }

    /** Menu bật lên ở nút ⋮ của một dòng. */
    protected void showPopup(View anchor, List<String> items, Consumer<String> onPick) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        for (String item : items) popup.getMenu().add(item);
        popup.setOnMenuItemClickListener(item -> {
            onPick.accept(item.getTitle().toString());
            return true;
        });
        popup.show();
    }

    public void toast(String text) {
        if (isAdded()) Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
    }

    /** Chuyển sang tab Nhóm và mở chi tiết nhóm đang chọn. */
    protected void openGroupScreen() {
        if (isAdded()) ((MainActivity) requireActivity()).openGroup();
    }

    // ===== Dòng khoản chi (Trang chủ và Nhóm đều dùng) =====

    protected void addExpenseRow(RowList rows, Expense e) {
        rows.row("expense" + e.id, "expense", e.title,
                e.payerName + " đã trả · " + e.category + " · " + Format.date(e.createdAt),
                Format.money(e.amount), "", e);
    }

    protected void addNoExpenseRow(RowList rows, String title) {
        rows.empty(title, groupState.members.isEmpty()
                ? "Mở Nhóm → Thành viên để thêm người tham gia."
                : "Thêm khoản chi đầu tiên. SplitMate sẽ chia tiền giúp bạn.");
    }
}
