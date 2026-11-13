package com.example.quanlychitieu.ui.common;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Định dạng số tiền, ngày tháng và chữ cái đại diện để hiển thị. */
public final class Format {
    public static final Locale VIETNAM = new Locale("vi", "VN");
    private static final NumberFormat MONEY = NumberFormat.getIntegerInstance(VIETNAM);

    private Format() {
    }

    /** 300000 → "300.000 đ" */
    public static String money(long amount) {
        return MONEY.format(amount) + " đ";
    }

    /** Chữ cái đầu viết hoa, dùng làm avatar: "bình" → "B" */
    public static String initial(String name) {
        return name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(VIETNAM);
    }

    /** Ngày/tháng, ví dụ "13/11" */
    public static String date(long millis) {
        return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date(millis));
    }

    /** Tháng/năm, ví dụ "11/2026", dùng cho bộ lọc theo tháng */
    public static String month(long millis) {
        return new SimpleDateFormat("MM/yyyy", Locale.US).format(new Date(millis));
    }
}
