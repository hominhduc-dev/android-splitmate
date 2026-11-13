package com.example.quanlychitieu.data.model;

/** Một thành viên trong nhóm (bảng members), kèm số tiền đã tính sẵn. */
public class Member {
    public final long id;
    public final long groupId;
    public final String name;
    /** Tổng tiền người này đã trả cho các khoản chi. */
    public final long paid;
    /** Tổng phần tiền người này phải chịu. */
    public final long owed;
    /**
     * Số dư = đã trả - phải chịu + đã chuyển cho người khác - đã nhận từ người khác.
     * Lớn hơn 0: được nhận lại tiền. Nhỏ hơn 0: còn phải trả.
     */
    public final long balance;

    public Member(long id, long groupId, String name, long paid, long owed, long balance) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.paid = paid;
        this.owed = owed;
        this.balance = balance;
    }
}
