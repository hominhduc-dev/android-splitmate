package com.example.quanlychitieu.ui.common;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quanlychitieu.data.model.GroceryItem;
import com.example.quanlychitieu.databinding.ItemWorkspaceBinding;

/**
 * Adapter dùng chung cho danh sách của cả 3 màn hình. Layout một dòng: item_workspace.xml.
 * Trường "kind" của mỗi dòng quyết định cách hiển thị:
 * hero (thẻ xanh tổng tiền), summary, section (tiêu đề nhóm dòng), empty, group, member,
 * expense, grocery, suggestion (gợi ý trả nợ), credit/debit/balance (số dư), category, settlement.
 * DiffUtil so sánh danh sách cũ và mới, chỉ vẽ lại những dòng thay đổi.
 */
public final class RowAdapter extends ListAdapter<RowAdapter.Row, RowAdapter.Holder> {

    /** Nội dung một dòng. item là đối tượng gốc (Expense, Member, GroceryItem…) để xử lý khi bấm. */
    public static final class Row {
        public final String key, kind, title, detail, amount, avatar;
        public final Object item;
        /** Ô tích "đã mua", chỉ dùng cho món đi chợ. */
        public final boolean checked, checkEnabled;

        public Row(String key, String kind, String title, String detail, String amount, String avatar, Object item) {
            this.key = key;
            this.kind = kind;
            this.title = title;
            this.detail = detail;
            this.amount = amount;
            this.avatar = avatar;
            this.item = item;
            if (item instanceof GroceryItem) {
                GroceryItem grocery = (GroceryItem) item;
                checked = grocery.bought;
                checkEnabled = !grocery.isLinked(); // món đã ghi chi thì không bỏ tích được
            } else {
                checked = false;
                checkEnabled = true;
            }
        }

        /** Hai dòng có cùng signature thì không cần vẽ lại. */
        String signature() {
            return kind + title + detail + amount + avatar + checked + checkEnabled;
        }
    }

    /** Màn hình xử lý khi người dùng bấm vào một dòng, nút ⋮ hoặc ô tích. */
    public interface Actions {
        void open(Row row, View anchor);

        void menu(Row row, View anchor);

        void check(Row row, boolean checked);
    }

    public static final class Holder extends RecyclerView.ViewHolder {
        final ItemWorkspaceBinding b;

        Holder(ItemWorkspaceBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    private final Actions actions;

    public RowAdapter(Actions actions) {
        super(new DiffUtil.ItemCallback<Row>() {
            @Override
            public boolean areItemsTheSame(@NonNull Row a, @NonNull Row b) {
                return a.key.equals(b.key);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Row a, @NonNull Row b) {
                return a.signature().equals(b.signature());
            }
        });
        this.actions = actions;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(ItemWorkspaceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Row r = getItem(position);
        ItemWorkspaceBinding b = h.b;
        boolean section = r.kind.equals("section");
        boolean hero = r.kind.equals("hero");
        boolean grocery = r.kind.equals("grocery");
        boolean hasItem = r.item != null;

        b.label.setText(r.title);
        b.detail.setText(r.detail);
        b.amount.setText(r.amount);
        b.avatar.setText(r.avatar);

        // Màu và cỡ chữ theo loại dòng
        b.label.setTextColor(hero ? Color.WHITE : 0xFF202D28);
        b.detail.setTextColor(hero ? 0xFFD5E8DE : 0xFF69766F);
        b.amount.setTextColor(hero ? Color.WHITE : r.kind.equals("debit") ? 0xFFA2502D : 0xFF236C53);
        b.label.setTextSize(section ? 18 : hero ? 14 : 16);
        b.amount.setTextSize(hero ? 32 : 18);
        b.surface.setBackgroundTintList(ColorStateList.valueOf(hero ? 0xFF236C53 : section ? 0xFFF7F8FA : Color.WHITE));

        // Ẩn những phần không có nội dung
        b.detail.setVisibility(r.detail.isEmpty() ? View.GONE : View.VISIBLE);
        b.amount.setVisibility(r.amount.isEmpty() ? View.GONE : View.VISIBLE);
        b.avatar.setVisibility(r.avatar.isEmpty() || r.kind.equals("category") ? View.GONE : View.VISIBLE);

        // Nút ⋮ (sửa / xóa)
        boolean hasMenu = hasItem && !r.kind.equals("suggestion") && !r.kind.equals("credit")
                && !r.kind.equals("debit") && !r.kind.equals("balance");
        b.more.setVisibility(hasMenu ? View.VISIBLE : View.GONE);
        b.more.setContentDescription("Tùy chọn: " + r.title);

        // Ô tích "đã mua" của món đi chợ
        b.check.setOnCheckedChangeListener(null);
        b.check.setVisibility(grocery ? View.VISIBLE : View.GONE);
        b.check.setContentDescription("Đã mua " + r.title);
        b.check.setChecked(r.checked);
        b.check.setEnabled(!grocery || r.checkEnabled);
        b.check.setOnCheckedChangeListener((v, on) -> {
            Row now = current(h);
            if (now == null) return;
            b.check.setEnabled(false); // chờ lưu xong mới cho bấm tiếp
            actions.check(now, on);
        });
        b.label.setPaintFlags(r.checked ? b.label.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
                : b.label.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        b.label.setAlpha(r.checked ? .5f : 1f);

        // Thanh biểu đồ danh mục: avatar chứa tỷ lệ phần nghìn
        b.chart.setVisibility(r.kind.equals("category") ? View.VISIBLE : View.GONE);
        if (r.kind.equals("category")) b.chart.setProgress(Integer.parseInt(r.avatar));

        // Nút "Ghi nhận đã trả" của gợi ý trả nợ
        b.action.setVisibility(r.kind.equals("suggestion") ? View.VISIBLE : View.GONE);
        b.action.setOnClickListener(v -> open(h, b.action));

        b.surface.setOnClickListener(hasItem ? v -> open(h, b.more) : null);
        b.more.setOnClickListener(v -> {
            Row now = current(h);
            if (now != null) actions.menu(now, b.more);
        });
    }

    /** Dòng hiện tại ở vị trí của holder (luôn là dữ liệu mới nhất). */
    private Row current(Holder h) {
        int position = h.getBindingAdapterPosition();
        return position == RecyclerView.NO_POSITION ? null : getItem(position);
    }

    private void open(Holder h, View anchor) {
        Row now = current(h);
        if (now != null) actions.open(now, anchor);
    }
}
