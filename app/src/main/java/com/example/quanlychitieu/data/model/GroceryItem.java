package com.example.quanlychitieu.data.model;

/** Một món trong danh sách đi chợ (bảng grocery_items). */
public class GroceryItem {
    public final long id;
    public final long groupId;
    public final String name;
    public final long quantity;
    /** Đơn vị: kg, g, quả, chai… */
    public final String unit;
    /** Đơn giá dự kiến, đơn vị đồng. */
    public final long estimatedPrice;
    public final boolean bought;
    /** Id khoản chi đã ghi cho món này; null nếu chưa ghi chi. */
    public final Long expenseId;

    public GroceryItem(long id, long groupId, String name, long quantity, String unit,
                       long estimatedPrice, boolean bought, Long expenseId) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.estimatedPrice = estimatedPrice;
        this.bought = bought;
        this.expenseId = expenseId;
    }

    /** Món đã được ghi thành khoản chi chưa. */
    public boolean isLinked() {
        return expenseId != null;
    }

    /** Tổng dự kiến = số lượng × đơn giá. */
    public long estimatedTotal() {
        return quantity * estimatedPrice;
    }
}
