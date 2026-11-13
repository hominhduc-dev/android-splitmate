package com.example.quanlychitieu.ui.grocery;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.quanlychitieu.data.model.GroceryItem;
import com.example.quanlychitieu.data.repository.GroceryRepository;
import com.example.quanlychitieu.ui.common.BaseViewModel;
import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel của danh sách đi chợ. Màn hình gọi setGroup() khi nhóm đang chọn thay đổi
 * (nhóm lấy từ GroupViewModel).
 */
public class GroceryViewModel extends BaseViewModel {

    public static class State {
        public long groupId;
        public List<GroceryItem> items = new ArrayList<>();
        /** Tổng dự kiến của các món chưa mua. */
        public long pending;
    }

    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final GroceryRepository repository;
    /** -1 = chưa biết nhóm nào. */
    private long groupId = -1;
    private int generation;

    public GroceryViewModel(@NonNull Application app) {
        super(app);
        repository = new GroceryRepository(app);
    }

    public LiveData<State> getState() {
        return state;
    }

    /** Tải danh sách đi chợ của nhóm (không làm gì nếu đang xem đúng nhóm đó). */
    public void setGroup(long id) {
        if (id == groupId) return;
        groupId = id;
        refresh();
    }

    @Override
    public void refresh() {
        if (groupId < 0) return;
        final int ticket = ++generation;
        final long group = groupId;
        read(() -> {
            State s = new State();
            s.groupId = group;
            s.items = repository.getItems(group);
            for (GroceryItem item : s.items) if (!item.bought) s.pending += item.estimatedTotal();
            return s;
        }, loaded -> {
            if (ticket == generation) state.setValue(loaded);
        }, "Không thể tải dữ liệu. Vui lòng mở lại ứng dụng.");
    }

    public void saveItem(long id, long groupId, String name, String unit, long quantity, long price, Runnable onSaved) {
        write(() -> repository.saveItem(id, groupId, name, unit, quantity, price), onSaved);
    }

    public void deleteItem(long id) {
        write(() -> repository.deleteItem(id), null);
    }

    public void markBought(long id, boolean bought) {
        write(() -> repository.markBought(id, bought), null);
    }

    /** Ghi các món đã mua thành một khoản chi với số tiền thực trả. */
    public void convertToExpense(long groupId, List<Long> itemIds, String title, long amount, long payerId,
                                 String category, List<Long> participantIds, Runnable onSaved) {
        write(() -> repository.convertToExpense(groupId, itemIds, title, amount, payerId, category, participantIds), onSaved);
    }
}
