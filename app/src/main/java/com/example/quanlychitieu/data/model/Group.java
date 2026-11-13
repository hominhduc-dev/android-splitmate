package com.example.quanlychitieu.data.model;

/** Một nhóm chi tiêu chung: phòng trọ, chuyến đi… (bảng groups). */
public class Group {
    public final long id;
    public final String name;
    public final long createdAt;
    /** Số thành viên, đếm lúc truy vấn. */
    public final int memberCount;

    public Group(long id, String name, long createdAt, int memberCount) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.memberCount = memberCount;
    }
}
