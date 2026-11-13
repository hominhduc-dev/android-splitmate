package com.example.quanlychitieu.ui.group;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.model.CategoryTotal;
import com.example.quanlychitieu.data.model.Expense;
import com.example.quanlychitieu.data.model.Group;
import com.example.quanlychitieu.data.model.Member;
import com.example.quanlychitieu.data.model.Payment;
import com.example.quanlychitieu.databinding.DialogFilterBinding;
import com.example.quanlychitieu.databinding.DialogMemberBinding;
import com.example.quanlychitieu.databinding.DialogSettlementBinding;
import com.example.quanlychitieu.domain.DebtPlanner;
import com.example.quanlychitieu.ui.common.BaseScreenFragment;
import com.example.quanlychitieu.ui.common.Format;
import com.example.quanlychitieu.ui.common.Forms;
import com.example.quanlychitieu.ui.common.RowAdapter;
import com.example.quanlychitieu.ui.common.RowList;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình Nhóm, có hai chế độ:
 * - Danh sách nhóm (detail = false): chạm một nhóm để mở.
 * - Chi tiết nhóm (detail = true) với 3 tab: Chi tiêu, Công nợ, Thành viên.
 */
public class GroupFragment extends BaseScreenFragment {
    private static final int TAB_EXPENSES = 0, TAB_DEBTS = 1, TAB_MEMBERS = 2;

    private boolean detail;
    private int tab;
    // Bộ lọc lịch sử chi tiêu (tab Chi tiêu)
    private String search = "", categoryFilter = "", monthFilter = "";
    private long payerFilter;

    /** @param detail true để mở thẳng chi tiết nhóm đang chọn */
    public static GroupFragment create(boolean detail) {
        GroupFragment fragment = new GroupFragment();
        Bundle args = new Bundle();
        args.putBoolean("detail", detail);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle state) {
        detail = requireArguments().getBoolean("detail", false);
        if (state != null) { // khôi phục sau khi xoay màn hình
            detail = state.getBoolean("detail");
            tab = state.getInt("tab");
            search = state.getString("search", "");
            categoryFilter = state.getString("category", "");
            monthFilter = state.getString("month", "");
            payerFilter = state.getLong("payer");
        }
        super.onViewCreated(view, state);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("detail", detail);
        out.putInt("tab", tab);
        out.putString("search", search);
        out.putString("category", categoryFilter);
        out.putString("month", monthFilter);
        out.putLong("payer", payerFilter);
    }

    /** Nút Back: đang xem chi tiết nhóm thì quay về danh sách nhóm. */
    public boolean closeGroup() {
        if (!detail) return false;
        detail = false;
        render();
        return true;
    }

    private boolean inGroup() {
        return detail && groupState != null && groupState.groupId != 0;
    }

    @Override
    protected String[] tabTitles() {
        return new String[]{"Chi tiêu", "Công nợ", "Thành viên"};
    }

    @Override
    protected int currentTab() {
        return tab;
    }

    @Override
    protected void onTabSelected(int index) {
        tab = index;
    }

    @Override
    protected boolean showsGroupActions() {
        return inGroup();
    }

    // ===== Vẽ màn hình =====

    @Override
    protected void render() {
        if (b == null || groupState == null) return;
        GroupViewModel.State s = groupState;
        boolean inGroup = inGroup();

        if (inGroup) showHeader("NHÓM CỦA BẠN", s.groupName(), s.members.size() + " thành viên · Dữ liệu lưu trên thiết bị");
        else showHeader("SplitMate", "Nhóm của bạn", "Một nơi cho mọi kế hoạch chung.");
        showGroupPicker(false);
        b.tabs.setVisibility(inGroup ? View.VISIBLE : View.GONE);
        b.filter.setVisibility(inGroup && tab == TAB_EXPENSES ? View.VISIBLE : View.GONE);
        b.filter.setText(hasFilter() ? "Bộ lọc • Đang áp dụng" : "Bộ lọc");
        b.convert.setVisibility(View.GONE);
        selectTab(tab);
        b.add.setText(!inGroup ? "Tạo nhóm" : tab == TAB_MEMBERS ? "Thêm thành viên"
                : tab == TAB_DEBTS ? "Ghi nhận trả nợ" : "Thêm khoản chi");

        RowList rows = new RowList();
        if (!inGroup) addGroupRows(rows);
        else if (tab == TAB_MEMBERS) addMemberRows(rows);
        else if (tab == TAB_DEBTS) addDebtRows(rows);
        else addExpenseRows(rows);
        adapter.submitList(rows);
    }

    private void addGroupRows(RowList rows) {
        for (Group g : groupState.groups) {
            rows.row("group" + g.id, "group", g.name, g.memberCount + " thành viên · Mở nhóm", "", Format.initial(g.name), g);
        }
        if (rows.isEmpty()) rows.empty("Bắt đầu với một nhóm", "Tạo nhóm phòng trọ, bạn bè hoặc chuyến đi, rồi thêm thành viên.");
    }

    private void addMemberRows(RowList rows) {
        if (groupState.members.isEmpty()) rows.empty("Thêm người cùng chia sẻ", "Thêm thành viên trước khi tạo khoản chi đầu tiên.");
        for (Member m : groupState.members) {
            rows.row("member" + m.id, "member", m.name, "Thành viên nhóm", "", Format.initial(m.name), m);
        }
    }

    private void addExpenseRows(RowList rows) {
        GroupViewModel.State s = groupState;
        rows.row("total", "hero", "Tổng chi tiêu nhóm", s.groupName() + " · " + s.members.size() + " thành viên",
                Format.money(s.total), "", null);
        rows.section("expenseHeading", "Lịch sử chi tiêu");
        int count = 0;
        for (Expense e : s.expenses) {
            if (!matchesFilter(e)) continue;
            addExpenseRow(rows, e);
            count++;
        }
        if (count == 0) addNoExpenseRow(rows, hasFilter() ? "Không có kết quả phù hợp" : "Chưa có khoản chi");
    }

    /** Tab Công nợ: tổng nợ, gợi ý ai trả ai, số dư từng người, biểu đồ danh mục, lịch sử trả nợ. */
    private void addDebtRows(RowList rows) {
        GroupViewModel.State s = groupState;
        rows.row("debt", "hero", "Công nợ còn lại", "Sau các khoản thanh toán đã ghi nhận", Format.money(s.debt), "", null);

        rows.section("suggestions", "Gợi ý ai trả ai");
        Map<Long, Long> balances = new LinkedHashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (Member m : s.members) {
            balances.put(m.id, m.balance);
            names.put(m.id, m.name);
        }
        for (DebtPlanner.Transfer t : DebtPlanner.plan(balances)) {
            rows.row("suggest" + t.from + ":" + t.to, "suggestion", names.get(t.from) + " → " + names.get(t.to),
                    "Chạm để ghi nhận sau khi đã chuyển tiền", Format.money(t.amount), "", t);
        }
        if (s.debt == 0) rows.row("settled", "summary", "Cả nhóm đã cân bằng", "Không có khoản nào cần thanh toán.", "", "", null);

        rows.section("balances", "Số dư thành viên");
        for (Member m : s.members) {
            long n = m.balance;
            rows.row("balance" + m.id, n < 0 ? "debit" : n > 0 ? "credit" : "balance", m.name,
                    "Đã trả " + Format.money(m.paid) + " · Phần chịu " + Format.money(m.owed),
                    n > 0 ? "Nhận lại " + Format.money(n) : n < 0 ? "Cần trả " + Format.money(-n) : "Đã cân bằng",
                    Format.initial(m.name), m);
        }
        if (s.members.isEmpty()) rows.empty("Chưa có công nợ", "Thêm thành viên và khoản chi để xem số dư.");

        rows.section("categories", "Chi tiêu theo danh mục · Toàn bộ");
        for (CategoryTotal c : s.categories) {
            int perMille = s.total == 0 ? 0 : (int) ((double) c.total / s.total * 1000); // phần nghìn, cho thanh biểu đồ
            rows.row("cat" + c.category, "category", c.category,
                    String.format(Format.VIETNAM, "%.1f%% tổng chi", perMille / 10.0), Format.money(c.total), "" + perMille, null);
        }

        rows.section("history", "Thanh toán đã ghi nhận");
        for (Payment p : s.payments) {
            rows.row("settlement" + p.id, "settlement", p.fromName + " → " + p.toName, Format.date(p.createdAt),
                    Format.money(p.amount), "", p);
        }
        if (s.payments.isEmpty()) rows.row("noSettlement", "empty", "Chưa có thanh toán", "Ghi nhận khi thành viên đã chuyển tiền cho nhau.", "", "", null);
    }

    // ===== Bấm vào dòng =====

    @Override
    protected void onAddClick() {
        if (!inGroup()) groupEditor(null);
        else if (tab == TAB_MEMBERS) memberEditor(null);
        else if (tab == TAB_DEBTS) settlementEditor(null);
        else openExpenseEditor(null);
    }

    @Override
    public void open(RowAdapter.Row row, View anchor) {
        if (row.item instanceof Group) {
            detail = true;
            tab = TAB_EXPENSES;
            groupModel.selectGroup(((Group) row.item).id);
        } else if (row.item instanceof Expense) {
            openExpenseEditor((Expense) row.item);
        } else if (row.item instanceof DebtPlanner.Transfer) {
            settlementEditor((DebtPlanner.Transfer) row.item);
        } else if (row.kind.equals("member")) {
            memberEditor((Member) row.item);
        }
    }

    @Override
    public void menu(RowAdapter.Row row, View anchor) {
        List<String> items = new ArrayList<>();
        if (!(row.item instanceof Payment)) items.add("Chỉnh sửa");
        items.add("Xóa");
        showPopup(anchor, items, choice -> {
            if (choice.equals("Chỉnh sửa")) {
                if (row.item instanceof Group) groupEditor((Group) row.item);
                else open(row, anchor);
            } else {
                confirmDelete("Xóa " + row.title + "?", () -> delete(row.item));
            }
        });
    }

    private void delete(Object item) {
        if (item instanceof Group) groupModel.deleteGroup(((Group) item).id);
        else if (item instanceof Expense) groupModel.deleteExpense(((Expense) item).id);
        else if (item instanceof Member) groupModel.deleteMember(((Member) item).id);
        else if (item instanceof Payment) groupModel.deletePayment(((Payment) item).id);
    }

    // ===== Hộp thoại =====

    /** Thêm hoặc đổi tên thành viên. Layout: dialog_member.xml */
    private void memberEditor(Member old) {
        final long group = groupState.groupId;
        DialogMemberBinding form = DialogMemberBinding.inflate(getLayoutInflater());
        if (old != null) form.name.setText(old.name);
        showForm(old == null ? "Thêm thành viên" : "Đổi tên thành viên", form.getRoot(), false, close -> {
            String name = Forms.required(form.name);
            groupModel.saveMember(old == null ? 0 : old.id, group, name, close);
        });
    }

    /** Ghi nhận trả nợ; suggestion là gợi ý "ai trả ai" nếu người dùng chạm vào gợi ý. Layout: dialog_settlement.xml */
    private void settlementEditor(DebtPlanner.Transfer suggestion) {
        final List<Member> members = groupState.members;
        if (members.size() < 2) {
            toast("Cần ít nhất 2 thành viên");
            return;
        }
        final long group = groupState.groupId;
        DialogSettlementBinding form = DialogSettlementBinding.inflate(getLayoutInflater());
        Spinner from = form.from, to = form.to;
        Forms.fill(from, Forms.memberNames(members));
        Forms.fill(to, Forms.memberNames(members));
        // Chọn sẵn người đang nợ làm người chuyển, người được nhận làm người nhận
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).balance < 0) from.setSelection(i);
            if (members.get(i).balance > 0) to.setSelection(i);
        }
        if (suggestion != null) {
            for (int i = 0; i < members.size(); i++) {
                if (members.get(i).id == suggestion.from) from.setSelection(i);
                if (members.get(i).id == suggestion.to) to.setSelection(i);
            }
            form.amount.setText(String.valueOf(suggestion.amount));
        }
        showForm("Ghi nhận trả nợ", form.getRoot(), false, close -> {
            long fromId = members.get(from.getSelectedItemPosition()).id;
            long toId = members.get(to.getSelectedItemPosition()).id;
            long amount = Forms.number(form.amount, false);
            if (fromId == toId) throw new IllegalArgumentException("Chọn hai thành viên khác nhau");
            groupModel.addPayment(group, fromId, toId, amount, close);
        });
    }

    // ===== Bộ lọc lịch sử chi tiêu =====

    @Override
    protected void onFilterClick() {
        DialogFilterBinding form = DialogFilterBinding.inflate(getLayoutInflater());
        EditText query = form.query;
        query.setText(search);

        // Danh mục: thêm "Tất cả danh mục" lên đầu danh sách trong arrays.xml
        String[] base = getResources().getStringArray(R.array.expense_categories);
        String[] categories = new String[base.length + 1];
        categories[0] = "Tất cả danh mục";
        System.arraycopy(base, 0, categories, 1, base.length);
        Spinner category = form.category;
        Forms.fill(category, categories);
        for (int i = 1; i < categories.length; i++) if (categories[i].equals(categoryFilter)) category.setSelection(i);

        List<Member> members = groupState.members;
        String[] payers = new String[members.size() + 1];
        payers[0] = "Tất cả người trả";
        for (int i = 0; i < members.size(); i++) payers[i + 1] = members.get(i).name;
        Spinner payer = form.payer;
        Forms.fill(payer, payers);
        for (int i = 0; i < members.size(); i++) if (members.get(i).id == payerFilter) payer.setSelection(i + 1);

        List<String> months = new ArrayList<>();
        months.add("Tất cả thời gian");
        for (Expense e : groupState.expenses) {
            String m = Format.month(e.createdAt);
            if (!months.contains(m)) months.add(m);
        }
        Spinner month = form.month;
        Forms.fill(month, months.toArray(new String[0]));
        if (months.contains(monthFilter)) month.setSelection(months.indexOf(monthFilter));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Lọc lịch sử chi tiêu")
                .setView(form.getRoot())
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Xóa bộ lọc", (d, w) -> {
                    search = "";
                    categoryFilter = "";
                    monthFilter = "";
                    payerFilter = 0;
                    render();
                })
                .setPositiveButton("Áp dụng", (d, w) -> {
                    search = query.getText().toString().trim();
                    categoryFilter = category.getSelectedItemPosition() == 0 ? "" : categories[category.getSelectedItemPosition()];
                    payerFilter = payer.getSelectedItemPosition() == 0 ? 0 : members.get(payer.getSelectedItemPosition() - 1).id;
                    monthFilter = month.getSelectedItemPosition() == 0 ? "" : months.get(month.getSelectedItemPosition());
                    render();
                    b.list.scrollToPosition(0);
                })
                .show();
    }

    private boolean hasFilter() {
        return !search.isEmpty() || !categoryFilter.isEmpty() || !monthFilter.isEmpty() || payerFilter != 0;
    }

    private boolean matchesFilter(Expense e) {
        if (!search.isEmpty() && !e.title.toLowerCase(Format.VIETNAM).contains(search.toLowerCase(Format.VIETNAM))) return false;
        if (!categoryFilter.isEmpty() && !categoryFilter.equals(e.category)) return false;
        if (payerFilter != 0 && e.payerId != payerFilter) return false;
        return monthFilter.isEmpty() || monthFilter.equals(Format.month(e.createdAt));
    }
}
