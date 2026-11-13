package com.example.quanlychitieu.data.model;

import java.util.List;

/** Một khoản chi (bảng expenses) và danh sách người cùng chia (bảng expense_shares). */
public class Expense {
    public final long id;
    public final long groupId;
    public final String title;
    /** Số tiền, đơn vị đồng. */
    public final long amount;
    public final long payerId;
    public final String payerName;
    public final String category;
    public final long createdAt;
    /** Id các thành viên cùng chia khoản chi này. */
    public final List<Long> participantIds;

    public Expense(long id, long groupId, String title, long amount, long payerId, String payerName,
                   String category, long createdAt, List<Long> participantIds) {
        this.id = id;
        this.groupId = groupId;
        this.title = title;
        this.amount = amount;
        this.payerId = payerId;
        this.payerName = payerName;
        this.category = category;
        this.createdAt = createdAt;
        this.participantIds = participantIds;
    }
}
