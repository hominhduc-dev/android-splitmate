package com.example.quanlychitieu.data.model;

/** Tổng chi của một danh mục, dùng cho biểu đồ ở tab Công nợ. */
public class CategoryTotal {
    public final String category;
    public final long total;

    public CategoryTotal(String category, long total) {
        this.category = category;
        this.total = total;
    }
}
