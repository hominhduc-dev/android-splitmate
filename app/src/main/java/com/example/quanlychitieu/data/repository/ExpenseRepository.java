package com.example.quanlychitieu.data.repository;

import android.content.ContentValues;
import android.content.Context;
import com.example.quanlychitieu.data.local.DatabaseHelper;
import com.example.quanlychitieu.data.model.CategoryTotal;
import com.example.quanlychitieu.data.model.Expense;
import com.example.quanlychitieu.data.model.Payment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Khoản chi, phần chia, thanh toán trả nợ và thống kê theo danh mục.
 * Các hàm ở đây chạy đồng bộ, nên ViewModel phải gọi chúng trên luồng nền.
 */
public class ExpenseRepository {
    private final DatabaseHelper db;

    public ExpenseRepository(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    // ===== Khoản chi =====

    /** Khoản chi của nhóm, mới nhất lên đầu, kèm tên người trả và danh sách người chia. */
    public List<Expense> getExpenses(long groupId) {
        String key = "" + groupId;
        // Gom danh sách người chia theo từng khoản chi
        Map<Long, List<Long>> participants = new HashMap<>();
        for (ContentValues share : db.rows("SELECT x.* FROM expense_shares x "
                + "JOIN expenses e ON e.id=x.expense_id WHERE e.group_id=?", key)) {
            participants.computeIfAbsent(share.getAsLong("expense_id"), k -> new ArrayList<>())
                    .add(share.getAsLong("member_id"));
        }
        List<Expense> expenses = new ArrayList<>();
        for (ContentValues v : db.rows("SELECT e.*,m.name AS payer FROM expenses e "
                + "JOIN members m ON m.id=e.payer_id WHERE e.group_id=? ORDER BY e.created_at DESC,e.id DESC", key)) {
            long id = v.getAsLong("id");
            List<Long> ids = participants.get(id);
            expenses.add(new Expense(id, v.getAsLong("group_id"), v.getAsString("title"), v.getAsLong("amount"),
                    v.getAsLong("payer_id"), v.getAsString("payer"), v.getAsString("category"),
                    v.getAsLong("created_at"), ids == null ? new ArrayList<>() : ids));
        }
        return expenses;
    }

    /** Thêm (id = 0) hoặc sửa khoản chi, chia đều cho participantIds trong một transaction. */
    public void saveExpense(long id, long groupId, String title, long amount, long payerId,
                            String category, List<Long> participantIds) {
        db.expense(id, groupId, title, amount, payerId, category, participantIds);
    }

    /** Xóa khoản chi. Món đi chợ liên kết sẽ quay về trạng thái "đã mua, chưa ghi chi". */
    public void deleteExpense(long id) {
        db.delete("expenses", id);
    }

    /** Tổng chi theo danh mục, nhiều nhất lên đầu. */
    public List<CategoryTotal> getCategoryTotals(long groupId) {
        List<CategoryTotal> totals = new ArrayList<>();
        for (ContentValues v : db.rows("SELECT category,SUM(amount) AS total FROM expenses "
                + "WHERE group_id=? GROUP BY category ORDER BY total DESC", "" + groupId)) {
            totals.add(new CategoryTotal(v.getAsString("category"), v.getAsLong("total")));
        }
        return totals;
    }

    // ===== Thanh toán trả nợ =====

    /** Các lần trả nợ đã ghi nhận, mới nhất lên đầu. */
    public List<Payment> getPayments(long groupId) {
        List<Payment> payments = new ArrayList<>();
        for (ContentValues v : db.rows("SELECT s.*,a.name AS sender,b.name AS receiver FROM settlements s "
                + "JOIN members a ON a.id=s.from_member_id JOIN members b ON b.id=s.to_member_id "
                + "WHERE s.group_id=? ORDER BY s.created_at DESC", "" + groupId)) {
            payments.add(new Payment(v.getAsLong("id"), v.getAsLong("group_id"),
                    v.getAsLong("from_member_id"), v.getAsLong("to_member_id"),
                    v.getAsString("sender"), v.getAsString("receiver"),
                    v.getAsLong("amount"), v.getAsLong("created_at")));
        }
        return payments;
    }

    /** Ghi nhận fromId đã chuyển amount cho toId. Báo lỗi nếu vượt quá công nợ. */
    public void addPayment(long groupId, long fromId, long toId, long amount) {
        db.settle(groupId, fromId, toId, amount);
    }

    public void deletePayment(long id) {
        db.delete("settlements", id);
    }
}
