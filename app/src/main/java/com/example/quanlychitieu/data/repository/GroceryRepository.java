package com.example.quanlychitieu.data.repository;

import android.content.ContentValues;
import android.content.Context;
import com.example.quanlychitieu.data.local.DatabaseHelper;
import com.example.quanlychitieu.data.model.GroceryItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách đi chợ: thêm/sửa/xóa món, đánh dấu đã mua và ghi các món đã mua thành khoản chi.
 * Các hàm ở đây chạy đồng bộ, nên ViewModel phải gọi chúng trên luồng nền.
 */
public class GroceryRepository {
    private final DatabaseHelper db;

    public GroceryRepository(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    /** Các món của nhóm, mới thêm lên đầu. */
    public List<GroceryItem> getItems(long groupId) {
        List<GroceryItem> items = new ArrayList<>();
        for (ContentValues v : db.rows("SELECT * FROM grocery_items WHERE group_id=? ORDER BY id DESC", "" + groupId)) {
            items.add(new GroceryItem(v.getAsLong("id"), v.getAsLong("group_id"), v.getAsString("name"),
                    v.getAsLong("quantity"), v.getAsString("unit"), v.getAsLong("estimated_price"),
                    v.getAsInteger("is_bought") == 1, v.getAsLong("expense_id")));
        }
        return items;
    }

    /** Thêm (id = 0) hoặc sửa một món. */
    public void saveItem(long id, long groupId, String name, String unit, long quantity, long estimatedPrice) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("unit", unit);
        v.put("quantity", quantity);
        v.put("estimated_price", estimatedPrice);
        v.put("group_id", groupId);
        db.save("grocery_items", id, v);
    }

    public void deleteItem(long id) {
        db.delete("grocery_items", id);
    }

    /** Tích / bỏ tích "đã mua". Không cho bỏ tích món đã ghi chi. */
    public void markBought(long id, boolean bought) {
        db.markBought(id, bought);
    }

    /**
     * Tạo một khoản chi cho các món đã mua và liên kết các món với khoản chi đó (một transaction).
     * Báo lỗi nếu có món đã được ghi chi trước đó, nên không thể ghi trùng.
     */
    public void convertToExpense(long groupId, List<Long> itemIds, String title, long amount,
                                 long payerId, String category, List<Long> participantIds) {
        db.convertGroceries(groupId, itemIds, title, amount, payerId, category, participantIds);
    }
}
