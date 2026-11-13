package com.example.quanlychitieu.ui.group;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.quanlychitieu.data.local.BackupStore;
import com.example.quanlychitieu.data.model.CategoryTotal;
import com.example.quanlychitieu.data.model.Expense;
import com.example.quanlychitieu.data.model.Group;
import com.example.quanlychitieu.data.model.Member;
import com.example.quanlychitieu.data.model.Payment;
import com.example.quanlychitieu.data.repository.ExpenseRepository;
import com.example.quanlychitieu.data.repository.GroupRepository;
import com.example.quanlychitieu.ui.common.BaseViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ViewModel của nhóm đang chọn: danh sách nhóm, thành viên, khoản chi, công nợ.
 * Dùng chung cho Trang chủ, màn Nhóm và màn Đi chợ (gắn với Activity), nên cả 3 màn hình
 * luôn hiển thị cùng một nhóm.
 */
public class GroupViewModel extends BaseViewModel {

    /** Toàn bộ dữ liệu của nhóm đang chọn, đọc một lần rồi đưa cho giao diện vẽ. */
    public static class State {
        /** 0 nếu chưa có nhóm nào. */
        public long groupId;
        public List<Group> groups = new ArrayList<>();
        public List<Member> members = new ArrayList<>();
        public List<Expense> expenses = new ArrayList<>();
        public List<Payment> payments = new ArrayList<>();
        public List<CategoryTotal> categories = new ArrayList<>();
        /** Tổng chi của nhóm. */
        public long total;
        /** Tổng số tiền còn phải trả (cộng các số dư dương). */
        public long debt;

        public Group currentGroup() {
            for (Group g : groups) if (g.id == groupId) return g;
            return null;
        }

        public String groupName() {
            Group g = currentGroup();
            return g == null ? "Chọn nhóm" : g.name;
        }

        public Expense findExpense(long id) {
            for (Expense e : expenses) if (e.id == id) return e;
            return null;
        }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    /** Nhóm người dùng muốn xem. */
    private long requestedGroup;
    /** Tăng mỗi lần tải; chỉ nhận kết quả của lần tải mới nhất. */
    private int generation;

    public GroupViewModel(@NonNull Application app) {
        super(app);
        groupRepository = new GroupRepository(app);
        expenseRepository = new ExpenseRepository(app);
        requestedGroup = groupRepository.getSelectedGroupId();
        refresh();
    }

    public LiveData<State> getState() {
        return state;
    }

    /** Chuyển sang xem nhóm khác. */
    public void selectGroup(long groupId) {
        requestedGroup = groupId;
        refresh();
    }

    @Override
    public void refresh() {
        final int ticket = ++generation;
        final long requested = requestedGroup;
        read(() -> loadState(requested), loaded -> {
            if (ticket != generation) return; // đã có lần tải mới hơn
            requestedGroup = loaded.groupId;
            groupRepository.setSelectedGroupId(loaded.groupId);
            state.setValue(loaded);
        }, "Không thể tải dữ liệu. Vui lòng mở lại ứng dụng.");
    }

    /** Chạy ở luồng nền: đọc mọi thứ của nhóm từ các Repository. */
    private State loadState(long requested) {
        State s = new State();
        s.groups = groupRepository.getGroups();
        // Nhóm đã bị xóa thì chọn nhóm đầu tiên
        s.groupId = s.groups.isEmpty() ? 0 : s.groups.get(0).id;
        for (Group g : s.groups) if (g.id == requested) s.groupId = requested;

        s.members = groupRepository.getMembers(s.groupId);
        s.expenses = expenseRepository.getExpenses(s.groupId);
        s.payments = expenseRepository.getPayments(s.groupId);
        s.categories = expenseRepository.getCategoryTotals(s.groupId);
        for (Expense e : s.expenses) s.total += e.amount;
        for (Member m : s.members) s.debt += Math.max(0, m.balance);
        return s;
    }

    // ===== Nhóm =====

    /** Tạo nhóm (id = 0) hoặc đổi tên; onSaved nhận id của nhóm. */
    public void saveGroup(long id, String name, Consumer<Long> onSaved) {
        writeAndGet(() -> groupRepository.saveGroup(id, name), onSaved);
    }

    public void deleteGroup(long id) {
        write(() -> groupRepository.deleteGroup(id), null);
    }

    /** Tạo nhóm demo; onCreated nhận id của nhóm demo. */
    public void createDemo(Consumer<Long> onCreated) {
        writeAndGet(groupRepository::createDemo, onCreated);
    }

    // ===== Thành viên =====

    public void saveMember(long id, long groupId, String name, Runnable onSaved) {
        write(() -> groupRepository.saveMember(id, groupId, name), onSaved);
    }

    public void deleteMember(long id) {
        write(() -> groupRepository.deleteMember(id), null);
    }

    // ===== Khoản chi =====

    public void saveExpense(long id, long groupId, String title, long amount, long payerId,
                            String category, List<Long> participantIds, Runnable onSaved) {
        write(() -> expenseRepository.saveExpense(id, groupId, title, amount, payerId, category, participantIds), onSaved);
    }

    public void deleteExpense(long id) {
        write(() -> expenseRepository.deleteExpense(id), null);
    }

    // ===== Trả nợ =====

    public void addPayment(long groupId, long fromId, long toId, long amount, Runnable onSaved) {
        write(() -> expenseRepository.addPayment(groupId, fromId, toId, amount), onSaved);
    }

    public void deletePayment(long id) {
        write(() -> expenseRepository.deletePayment(id), null);
    }

    // ===== Sao lưu / khôi phục =====

    public void exportBackup(ContentResolver resolver, Uri uri, Runnable onDone) {
        write(() -> groupRepository.exportBackup(resolver, uri), onDone);
    }

    /** Đọc và kiểm tra tệp sao lưu; chưa ghi gì vào database. */
    public void readBackup(ContentResolver resolver, Uri uri, Consumer<BackupStore.Archive> onRead) {
        read(() -> groupRepository.readBackup(resolver, uri), onRead, null);
    }

    public void restoreBackup(BackupStore.Archive archive, Runnable onDone) {
        write(() -> groupRepository.restoreBackup(archive), onDone);
    }
}
