package com.example.quanlychitieu.ui.common;

import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import com.example.quanlychitieu.data.model.Member;
import java.util.List;

/**
 * Tiện ích cho các hộp thoại nhập liệu: kiểm tra dữ liệu nhập và đổ dữ liệu vào Spinner.
 * Khi dữ liệu sai, các hàm kiểm tra báo lỗi ngay trên ô nhập rồi ném IllegalArgumentException
 * để hộp thoại không đóng.
 */
public final class Forms {
    private Forms() {
    }

    /** Lấy chữ đã nhập (bỏ khoảng trắng hai đầu); báo lỗi nếu để trống. */
    public static String required(EditText input) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            input.setError("Vui lòng nhập thông tin");
            input.requestFocus();
            throw new IllegalArgumentException("Vui lòng điền đầy đủ thông tin");
        }
        return value;
    }

    /** Lấy số nguyên từ 0 (allowZero) hoặc từ 1, tối đa 1.000 tỷ. */
    public static long number(EditText input, boolean allowZero) {
        try {
            long n = Long.parseLong(required(input));
            if (n < 0 || (!allowZero && n == 0) || n > 1000000000000L) throw new Exception();
            return n;
        } catch (Exception e) {
            input.setError("Nhập số nguyên " + (allowZero ? "từ 0" : "lớn hơn 0"));
            throw new IllegalArgumentException("Số tiền hoặc số lượng chưa hợp lệ");
        }
    }

    /** Đổ danh sách chữ vào Spinner (dữ liệu chỉ biết lúc chạy, ví dụ tên thành viên). */
    public static void fill(Spinner spinner, String[] items) {
        spinner.setAdapter(new ArrayAdapter<>(spinner.getContext(), android.R.layout.simple_spinner_dropdown_item, items));
    }

    public static String[] memberNames(List<Member> members) {
        String[] names = new String[members.size()];
        for (int i = 0; i < names.length; i++) names[i] = members.get(i).name;
        return names;
    }
}
