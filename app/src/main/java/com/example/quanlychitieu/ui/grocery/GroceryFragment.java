package com.example.quanlychitieu.ui.grocery;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.example.quanlychitieu.data.model.Expense;
import com.example.quanlychitieu.data.model.GroceryItem;
import com.example.quanlychitieu.databinding.DialogGroceryBinding;
import com.example.quanlychitieu.ui.common.BaseScreenFragment;
import com.example.quanlychitieu.ui.common.Format;
import com.example.quanlychitieu.ui.common.Forms;
import com.example.quanlychitieu.ui.common.RowAdapter;
import com.example.quanlychitieu.ui.common.RowList;
import com.example.quanlychitieu.ui.group.ExpenseDialog;
import com.example.quanlychitieu.ui.group.GroupViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Màn hình Đi chợ. Một món đi qua 3 trạng thái:
 * cần mua → đã mua (tích ô) → đã ghi chi (bấm "Ghi chi" để nhập tiền thực trả và chia tiền).
 * Tab: Cần mua, Đã mua, Tất cả.
 */
public class GroceryFragment extends BaseScreenFragment {
    private static final int TAB_NEED = 0, TAB_BOUGHT = 1;
    private static final int NEED = 0, BOUGHT = 1, RECORDED = 2;

    private GroceryViewModel groceryModel;
    private int shoppingTab;

    @Override
    public void onViewCreated(@NonNull View view, Bundle state) {
        groceryModel = new ViewModelProvider(requireActivity()).get(GroceryViewModel.class);
        if (state != null) shoppingTab = state.getInt("shoppingTab");
        super.onViewCreated(view, state);
        groceryModel.getState().observe(getViewLifecycleOwner(), s -> render());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("shoppingTab", shoppingTab);
    }

    @Override
    protected void onGroupState(GroupViewModel.State state) {
        groceryModel.setGroup(state.groupId);
        render();
    }

    @Override
    protected String[] tabTitles() {
        return new String[]{"Cần mua", "Đã mua", "Tất cả"};
    }

    @Override
    protected int currentTab() {
        return shoppingTab;
    }

    @Override
    protected void onTabSelected(int index) {
        shoppingTab = index;
    }

    private static int status(GroceryItem item) {
        return !item.bought ? NEED : item.isLinked() ? RECORDED : BOUGHT;
    }

    // ===== Vẽ màn hình =====

    @Override
    protected void render() {
        if (b == null || groupState == null) return;
        GroupViewModel.State s = groupState;
        GroceryViewModel.State g = groceryModel.getState().getValue();

        showHeader("SplitMate", "Danh sách đi chợ", "Lên danh sách, mua đủ, chi hợp lý.");
        showGroupPicker(s.groupId != 0);
        b.tabs.setVisibility(s.groupId != 0 ? View.VISIBLE : View.GONE);
        b.filter.setVisibility(View.GONE);
        b.add.setText(s.groupId == 0 ? "Tạo nhóm" : "Thêm món");
        selectTab(shoppingTab);
        if (g == null || g.groupId != s.groupId) { // danh sách của nhóm này chưa tải xong
            b.convert.setVisibility(View.GONE);
            return;
        }

        int convertible = 0;
        for (GroceryItem item : g.items) if (status(item) == BOUGHT) convertible++;
        b.convert.setVisibility(convertible > 0 ? View.VISIBLE : View.GONE);
        b.convert.setText("Ghi chi " + convertible + " món đã mua");

        RowList rows = new RowList();
        if (s.groupId == 0) {
            rows.empty("Chia sẻ từ những điều nhỏ", "Tạo nhóm đầu tiên để ghi chi tiêu, chia tiền và cùng đi chợ.");
        } else {
            addSummaryRows(rows, s, g);
            addItemRows(rows, g);
        }
        adapter.submitList(rows);
    }

    private void addSummaryRows(RowList rows, GroupViewModel.State s, GroceryViewModel.State g) {
        int need = 0, waiting = 0;
        Set<Long> receipts = new HashSet<>(); // các khoản chi đã ghi từ đi chợ
        for (GroceryItem item : g.items) {
            if (status(item) == NEED) need++;
            else if (status(item) == BOUGHT) waiting++;
            else receipts.add(item.expenseId);
        }
        long actual = 0; // mỗi hóa đơn chỉ tính một lần dù có nhiều món
        for (Expense e : s.expenses) if (receipts.contains(e.id)) actual += e.amount;

        rows.row("grocerySummary", "hero", "Dự kiến còn cần mua", need + " món cần mua · " + waiting + " món chờ ghi chi",
                Format.money(g.pending), "", null);
        rows.row("groceryHelp", "summary", "Đã ghi chi: " + Format.money(actual),
                waiting > 0 ? "Dùng Ghi chi để nhập tiền thực trả và chọn người chia."
                        : "Giá dự kiến chưa tính vào chi tiêu. Các hóa đơn liên kết được tính một lần.", "", "", null);
    }

    private void addItemRows(RowList rows, GroceryViewModel.State g) {
        int shown = 0;
        for (int status = NEED; status <= RECORDED; status++) {
            if (shoppingTab == TAB_NEED && status != NEED || shoppingTab == TAB_BOUGHT && status == NEED) continue;
            boolean heading = false;
            for (GroceryItem item : g.items) {
                if (status(item) != status) continue;
                if (!heading) {
                    rows.section("shoppingSection" + status, status == NEED ? "Cần mua" : status == BOUGHT ? "Đã mua · Chưa ghi chi" : "Đã ghi chi");
                    heading = true;
                }
                rows.row("grocery" + item.id, "grocery", item.name,
                        item.quantity + " " + item.unit + " × " + Format.money(item.estimatedPrice) + " dự kiến",
                        status == RECORDED ? "Đã ghi chi · Mở hóa đơn" : "Dự kiến " + Format.money(item.estimatedTotal()), "", item);
                shown++;
            }
        }
        if (shown == 0) {
            rows.empty(shoppingTab == TAB_BOUGHT ? "Chưa có món đã mua" : "Danh sách đang trống",
                    shoppingTab == TAB_BOUGHT ? "Tích chọn món ở tab Cần mua khi mua xong."
                            : "Thêm món và đơn vị: kg, quả, chai… Bạn có thể để giá dự kiến bằng 0.");
        }
    }

    // ===== Bấm vào dòng =====

    @Override
    protected void onAddClick() {
        if (groupState.groupId == 0) groupEditor(null);
        else groceryEditor(null);
    }

    @Override
    public void open(RowAdapter.Row row, View anchor) {
        if (!(row.item instanceof GroceryItem)) return;
        GroceryItem item = (GroceryItem) row.item;
        if (!item.isLinked()) {
            groceryEditor(item);
            return;
        }
        // Món đã ghi chi: mở khoản chi liên kết
        Expense expense = groupState.findExpense(item.expenseId);
        if (expense != null) openExpenseEditor(expense);
        else toast("Khoản chi không còn tồn tại");
    }

    @Override
    public void menu(RowAdapter.Row row, View anchor) {
        if (!(row.item instanceof GroceryItem)) return;
        GroceryItem item = (GroceryItem) row.item;
        List<String> items = new ArrayList<>();
        if (item.isLinked()) {
            items.add("Mở khoản chi");
        } else {
            items.add("Chỉnh sửa");
            if (item.bought) items.add("Ghi thành khoản chi");
            items.add("Xóa");
        }
        showPopup(anchor, items, choice -> {
            if (choice.equals("Ghi thành khoản chi")) recordExpense(Collections.singletonList(item));
            else if (choice.equals("Xóa")) confirmDelete("Xóa " + row.title + "?", () -> groceryModel.deleteItem(item.id));
            else open(row, anchor);
        });
    }

    @Override
    public void check(RowAdapter.Row row, boolean checked) {
        groceryModel.markBought(((GroceryItem) row.item).id, checked);
    }

    // ===== Hộp thoại =====

    /** Thêm hoặc sửa món. Layout: dialog_grocery.xml */
    private void groceryEditor(GroceryItem old) {
        final long group = groupState.groupId;
        DialogGroceryBinding form = DialogGroceryBinding.inflate(getLayoutInflater());
        EditText name = form.name, quantity = form.quantity, price = form.price, unit = form.unit;
        name.setText(old == null ? "" : old.name);
        quantity.setText(old == null ? "1" : String.valueOf(old.quantity));
        price.setText(old == null ? "0" : String.valueOf(old.estimatedPrice));
        unit.setText(old == null ? "phần" : old.unit);
        showForm(old == null ? "Thêm món hàng" : "Sửa món hàng", form.getRoot(), false, close -> {
            long q = Forms.number(quantity, false);
            long p = Forms.number(price, true);
            if (q > 1000000 || p > 1000000000000L / q) throw new IllegalArgumentException("Số lượng tối đa 1 triệu, tổng tối đa 1.000 tỷ");
            groceryModel.saveItem(old == null ? 0 : old.id, group, Forms.required(name), Forms.required(unit), q, p, close);
        });
    }

    /** Nút "Ghi chi … món đã mua": chọn các món cùng một hóa đơn. */
    @Override
    protected void onConvertClick() {
        GroceryViewModel.State g = groceryModel.getState().getValue();
        if (g == null) return;
        List<GroceryItem> available = new ArrayList<>();
        for (GroceryItem item : g.items) if (status(item) == BOUGHT) available.add(item);
        if (available.isEmpty()) {
            toast("Chưa có món đã mua chờ ghi chi");
            return;
        }
        String[] labels = new String[available.size()];
        boolean[] selected = new boolean[available.size()];
        Arrays.fill(selected, true);
        for (int i = 0; i < labels.length; i++) {
            labels[i] = available.get(i).name + " · " + Format.money(available.get(i).estimatedTotal());
        }
        AlertDialog chooser = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Chọn món để ghi chi")
                .setMultiChoiceItems(labels, selected, (d, which, on) -> selected[which] = on)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tiếp tục", null)
                .create();
        chooser.setOnShowListener(d -> chooser.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            List<GroceryItem> chosen = new ArrayList<>();
            for (int i = 0; i < selected.length; i++) if (selected[i]) chosen.add(available.get(i));
            if (chosen.isEmpty()) {
                toast("Chọn ít nhất một món");
                return;
            }
            chooser.dismiss();
            recordExpense(chosen);
        }));
        chooser.show();
    }

    /** Mở hộp thoại khoản chi để nhập tiền thực trả cho các món đã mua. */
    private void recordExpense(List<GroceryItem> items) {
        final long group = groupState.groupId;
        List<Long> itemIds = new ArrayList<>();
        for (GroceryItem item : items) itemIds.add(item.id);
        ExpenseDialog.show(this, groupState, null, items,
                (title, amount, payerId, category, participants, close) -> groceryModel.convertToExpense(
                        group, itemIds, title, amount, payerId, category, participants, close));
    }
}
