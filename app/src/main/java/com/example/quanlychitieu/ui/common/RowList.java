package com.example.quanlychitieu.ui.common;

import java.util.ArrayList;

/** Danh sách các dòng sẽ hiển thị, có vài hàm tiện để thêm dòng cho gọn. */
public class RowList extends ArrayList<RowAdapter.Row> {

    /** Thêm một dòng. key phải khác nhau giữa các dòng để DiffUtil nhận ra. */
    public void row(String key, String kind, String title, String detail, String amount, String avatar, Object item) {
        add(new RowAdapter.Row(key, kind, title, detail, amount, avatar, item));
    }

    /** Dòng tiêu đề của một nhóm dòng, ví dụ "Lịch sử chi tiêu". */
    public void section(String key, String title) {
        row(key, "section", title, "", "", "", null);
    }

    /** Dòng thông báo khi danh sách trống. */
    public void empty(String title, String message) {
        row("empty", "empty", title, message, "", "", null);
    }
}
