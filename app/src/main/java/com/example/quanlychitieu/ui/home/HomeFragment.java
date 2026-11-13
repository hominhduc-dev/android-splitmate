package com.example.quanlychitieu.ui.home;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import com.example.quanlychitieu.data.model.Expense;
import com.example.quanlychitieu.ui.common.BaseScreenFragment;
import com.example.quanlychitieu.ui.common.Format;
import com.example.quanlychitieu.ui.common.RowAdapter;
import com.example.quanlychitieu.ui.common.RowList;
import com.example.quanlychitieu.ui.group.GroupViewModel;
import com.example.quanlychitieu.ui.grocery.GroceryViewModel;
import java.util.Arrays;

/** Trang chủ: tổng quan nhóm đang chọn và 5 khoản chi gần nhất. */
public class HomeFragment extends BaseScreenFragment {
    private static final int RECENT_EXPENSES = 5;
    /** Lấy tổng dự kiến đi chợ để hiện ở phần tổng quan. */
    private GroceryViewModel groceryModel;

    @Override
    public void onViewCreated(@NonNull View view, Bundle state) {
        groceryModel = new ViewModelProvider(requireActivity()).get(GroceryViewModel.class);
        super.onViewCreated(view, state);
        groceryModel.getState().observe(getViewLifecycleOwner(), s -> render());
    }

    @Override
    protected void onGroupState(GroupViewModel.State state) {
        groceryModel.setGroup(state.groupId);
        render();
    }

    @Override
    protected void render() {
        if (b == null || groupState == null) return;
        GroupViewModel.State s = groupState;

        showHeader("SplitMate", "Chi tiêu nhẹ nhàng", "Chia sẻ chi phí. Thêm vui bên nhau.");
        showGroupPicker(s.groupId != 0);
        b.tabs.setVisibility(View.GONE);
        b.filter.setVisibility(View.GONE);
        b.convert.setVisibility(View.GONE);
        b.add.setText(s.groupId == 0 ? "Tạo nhóm" : "Thêm khoản chi");

        RowList rows = new RowList();
        if (s.groupId == 0) {
            rows.empty("Chia sẻ từ những điều nhỏ", "Tạo nhóm đầu tiên để ghi chi tiêu, chia tiền và cùng đi chợ.");
        } else {
            GroceryViewModel.State grocery = groceryModel.getState().getValue();
            long pending = grocery != null && grocery.groupId == s.groupId ? grocery.pending : 0;
            rows.row("total", "hero", "Tổng chi tiêu nhóm", s.groupName() + " · " + s.members.size() + " thành viên",
                    Format.money(s.total), "", null);
            rows.row("overview", "summary", "Cần thanh toán: " + Format.money(s.debt),
                    "Đi chợ dự kiến: " + Format.money(pending), "", "", null);
            rows.section("recent", "Khoản chi gần đây");
            for (int i = 0; i < s.expenses.size() && i < RECENT_EXPENSES; i++) addExpenseRow(rows, s.expenses.get(i));
            if (s.expenses.isEmpty()) addNoExpenseRow(rows, "Chưa có khoản chi");
        }
        adapter.submitList(rows);
    }

    @Override
    protected void onAddClick() {
        if (groupState.groupId == 0) groupEditor(null);
        else openExpenseEditor(null);
    }

    @Override
    public void open(RowAdapter.Row row, View anchor) {
        if (row.item instanceof Expense) openExpenseEditor((Expense) row.item);
    }

    @Override
    public void menu(RowAdapter.Row row, View anchor) {
        if (!(row.item instanceof Expense)) return;
        Expense expense = (Expense) row.item;
        showPopup(anchor, Arrays.asList("Chỉnh sửa", "Xóa"), choice -> {
            if (choice.equals("Chỉnh sửa")) openExpenseEditor(expense);
            else confirmDelete("Xóa " + row.title + "?", () -> groupModel.deleteExpense(expense.id));
        });
    }
}
