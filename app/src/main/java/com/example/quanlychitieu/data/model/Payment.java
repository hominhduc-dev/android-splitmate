package com.example.quanlychitieu.data.model;

/** Một lần trả nợ giữa hai thành viên (bảng settlements). */
public class Payment {
    public final long id;
    public final long groupId;
    public final long fromMemberId;
    public final long toMemberId;
    public final String fromName;
    public final String toName;
    public final long amount;
    public final long createdAt;

    public Payment(long id, long groupId, long fromMemberId, long toMemberId,
                   String fromName, String toName, long amount, long createdAt) {
        this.id = id;
        this.groupId = groupId;
        this.fromMemberId = fromMemberId;
        this.toMemberId = toMemberId;
        this.fromName = fromName;
        this.toName = toName;
        this.amount = amount;
        this.createdAt = createdAt;
    }
}
