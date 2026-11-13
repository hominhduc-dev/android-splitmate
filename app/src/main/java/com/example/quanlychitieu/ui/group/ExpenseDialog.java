package com.example.quanlychitieu.ui.group;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.model.Expense;
import com.example.quanlychitieu.data.model.GroceryItem;
import com.example.quanlychitieu.data.model.Member;
import com.example.quanlychitieu.databinding.DialogExpenseBinding;
import com.example.quanlychitieu.domain.SplitCalculator;
import com.example.quanlychitieu.ui.common.BaseScreenFragment;
import com.example.quanlychitieu.ui.common.Format;
import com.example.quanlychitieu.ui.common.Forms;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hộp thoại thêm / sửa khoản chi (layout dialog_expense.xml). Dùng ở cả 3 màn hình:
 * - Trang chủ và Nhóm: thêm hoặc sửa một khoản chi bình thường.
 * - Đi chợ: ghi các món đã mua thành một khoản chi (danh sách groceries không rỗng).
 * Hộp thoại chỉ lo nhập liệu và xem trước phần chia; việc lưu do Saver của màn hình gọi quyết định.
 */
public final class ExpenseDialog {

    /** Được gọi khi bấm Lưu với dữ liệu đã hợp lệ. Lưu xong thì gọi close để đóng hộp thoại. */
    public interface Saver {
        void save(String title, long amount, long payerId, String category, List<Long> participantIds, Runnable close);
    }

    private ExpenseDialog() {
    }

    /**
     * @param old       khoản chi cần sửa, hoặc null để thêm mới
     * @param groceries các món đi chợ được ghi chi (rỗng nếu là khoản chi bình thường)
     */
    public static void show(BaseScreenFragment host, GroupViewModel.State data, Expense old,
                            List<GroceryItem> groceries, Saver saver) {
        if (data.members.isEmpty()) {
            host.toast("Thêm thành viên trong Nhóm trước khi tạo khoản chi");
            return;
        }
        final List<Member> members = data.members;
        DialogExpenseBinding form = DialogExpenseBinding.inflate(host.getLayoutInflater());
        form.groupInfo.setText(data.groupName() + " · Chia đều cho những người bạn chọn");

        EditText title = form.title;
        EditText amount = form.amount;
        if (old != null) {
            title.setText(old.title);
            amount.setText(String.valueOf(old.amount));
        }

        // Ghi chi từ đi chợ: gợi ý tên và số tiền theo giá dự kiến của các món
        boolean fromGrocery = !groceries.isEmpty();
        if (fromGrocery) {
            long estimate = 0;
            List<String> names = new ArrayList<>();
            for (GroceryItem item : groceries) {
                estimate += item.estimatedTotal();
                names.add(item.name);
            }
            form.groceryInfo.setText("Món đã mua: " + TextUtils.join(", ", names)
                    + ". Nhập tổng tiền thực trả; giá dự kiến chỉ để tham khảo.");
            form.groceryInfo.setVisibility(View.VISIBLE);
            title.setText(names.size() == 1 ? names.get(0) : "Đi chợ · " + names.size() + " món");
            amount.setText(String.valueOf(estimate));
        }

        // Người trả và danh mục
        Spinner payer = form.payer;
        Spinner category = form.category;
        Forms.fill(payer, Forms.memberNames(members));
        String[] categories = host.getResources().getStringArray(R.array.expense_categories);
        Forms.fill(category, categories);
        if (fromGrocery) category.setSelection(3); // "Mua sắm"
        if (old != null) {
            for (int i = 0; i < members.size(); i++) if (members.get(i).id == old.payerId) payer.setSelection(i);
            for (int i = 0; i < categories.length; i++) if (categories[i].equals(old.category)) category.setSelection(i);
        }

        // Mỗi thành viên một CheckBox (item_share_member.xml). Sửa khoản chi thì tích sẵn những người đã chia.
        List<CheckBox> checks = new ArrayList<>();
        for (Member m : members) {
            CheckBox box = (CheckBox) host.getLayoutInflater().inflate(R.layout.item_share_member, form.members, false);
            box.setText(m.name);
            box.setChecked(old == null || old.participantIds.contains(m.id));
            form.members.addView(box);
            checks.add(box);
        }

        // Xem trước "Mỗi người chịu…" mỗi khi đổi số tiền hoặc người chia
        Runnable preview = () -> {
            try {
                long n = Long.parseLong(amount.getText().toString());
                Map<Long, Long> shares = SplitCalculator.splitEqually(n, checkedIds(members, checks));
                StringBuilder text = new StringBuilder("Mỗi người chịu\n");
                for (Member m : members) {
                    if (shares.containsKey(m.id)) text.append(m.name).append(": ").append(Format.money(shares.get(m.id))).append("\n");
                }
                form.preview.setText(text.toString().trim());
            } catch (Exception e) {
                form.preview.setText("Nhập số tiền và chọn người tham gia để xem phần chia.");
            }
        };
        amount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                preview.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        for (CheckBox box : checks) box.setOnCheckedChangeListener((v, on) -> preview.run());
        preview.run();

        host.showForm(old == null ? "Thêm khoản chi" : "Sửa khoản chi", form.getRoot(), true, close -> {
            String t = Forms.required(title);
            long n = Forms.number(amount, false);
            long payerId = members.get(payer.getSelectedItemPosition()).id;
            String c = categories[category.getSelectedItemPosition()];
            List<Long> ids = checkedIds(members, checks);
            if (ids.isEmpty()) throw new IllegalArgumentException("Chọn ít nhất một người tham gia chia tiền");
            saver.save(t, n, payerId, c, ids, close);
        });
    }

    private static List<Long> checkedIds(List<Member> members, List<CheckBox> checks) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < checks.size(); i++) if (checks.get(i).isChecked()) ids.add(members.get(i).id);
        return ids;
    }
}
