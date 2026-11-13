package com.example.quanlychitieu.data.repository;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import com.example.quanlychitieu.data.local.BackupStore;
import com.example.quanlychitieu.data.local.DatabaseHelper;
import com.example.quanlychitieu.data.model.Group;
import com.example.quanlychitieu.data.model.Member;
import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu nhóm và thành viên, nhóm đang chọn, sao lưu / khôi phục.
 * Các hàm ở đây chạy đồng bộ, nên ViewModel phải gọi chúng trên luồng nền.
 */
public class GroupRepository {
    private static final String PREFS = "workspace";
    private static final String KEY_SELECTED_GROUP = "group";

    private final DatabaseHelper db;
    private final SharedPreferences prefs;

    public GroupRepository(Context context) {
        db = DatabaseHelper.getInstance(context);
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ===== Nhóm =====

    /** Tất cả nhóm, mới nhất lên đầu, kèm số thành viên. */
    public List<Group> getGroups() {
        List<Group> groups = new ArrayList<>();
        for (ContentValues v : db.rows("SELECT g.*,COUNT(m.id) AS member_count FROM groups g "
                + "LEFT JOIN members m ON m.group_id=g.id GROUP BY g.id ORDER BY g.created_at DESC")) {
            groups.add(new Group(v.getAsLong("id"), v.getAsString("name"),
                    v.getAsLong("created_at"), v.getAsInteger("member_count")));
        }
        return groups;
    }

    /** Thêm nhóm mới (id = 0) hoặc đổi tên nhóm. Trả về id của nhóm. */
    public long saveGroup(long id, String name) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        if (id == 0) v.put("created_at", System.currentTimeMillis());
        return db.save("groups", id, v);
    }

    /** Xóa nhóm cùng toàn bộ thành viên, khoản chi, món đi chợ và thanh toán. */
    public void deleteGroup(long id) {
        db.deleteGroup(id);
    }

    /** Tạo nhóm demo (hoặc trả về nhóm demo đã có). */
    public long createDemo() {
        return db.createDemo();
    }

    /** Nhóm người dùng chọn lần trước, lưu trong SharedPreferences. */
    public long getSelectedGroupId() {
        return prefs.getLong(KEY_SELECTED_GROUP, 0);
    }

    public void setSelectedGroupId(long id) {
        prefs.edit().putLong(KEY_SELECTED_GROUP, id).apply();
    }

    // ===== Thành viên =====

    /** Thành viên của nhóm, kèm số tiền đã trả, phải chịu và số dư (tính bằng SQL). */
    public List<Member> getMembers(long groupId) {
        List<Member> members = new ArrayList<>();
        for (ContentValues v : db.rows("SELECT m.*,COALESCE(p.n,0) AS paid,COALESCE(o.n,0) AS owed,"
                + "COALESCE(p.n,0)-COALESCE(o.n,0)+COALESCE(a.n,0)-COALESCE(b.n,0) AS balance FROM members m "
                + "LEFT JOIN (SELECT payer_id,SUM(amount) n FROM expenses GROUP BY payer_id) p ON p.payer_id=m.id "
                + "LEFT JOIN (SELECT member_id,SUM(share_amount) n FROM expense_shares GROUP BY member_id) o ON o.member_id=m.id "
                + "LEFT JOIN (SELECT from_member_id,SUM(amount) n FROM settlements GROUP BY from_member_id) a ON a.from_member_id=m.id "
                + "LEFT JOIN (SELECT to_member_id,SUM(amount) n FROM settlements GROUP BY to_member_id) b ON b.to_member_id=m.id "
                + "WHERE m.group_id=? ORDER BY m.id", "" + groupId)) {
            members.add(new Member(v.getAsLong("id"), v.getAsLong("group_id"), v.getAsString("name"),
                    v.getAsLong("paid"), v.getAsLong("owed"), v.getAsLong("balance")));
        }
        return members;
    }

    /** Thêm thành viên (id = 0) hoặc đổi tên. */
    public void saveMember(long id, long groupId, String name) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("group_id", groupId);
        db.save("members", id, v);
    }

    /** Xóa thành viên. Database báo lỗi nếu thành viên đã có giao dịch. */
    public void deleteMember(long id) {
        db.delete("members", id);
    }

    // ===== Sao lưu / khôi phục =====

    public void exportBackup(ContentResolver resolver, Uri uri) throws Exception {
        BackupStore.exportTo(db, resolver, uri);
    }

    public BackupStore.Archive readBackup(ContentResolver resolver, Uri uri) throws Exception {
        return BackupStore.read(resolver, uri);
    }

    /** Khôi phục thành các nhóm mới có hậu tố "(khôi phục)". */
    public void restoreBackup(BackupStore.Archive archive) {
        BackupStore.restore(db, archive);
    }
}
