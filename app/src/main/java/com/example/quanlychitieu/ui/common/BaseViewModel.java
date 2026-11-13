package com.example.quanlychitieu.ui.common;

import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Lớp cha của mọi ViewModel trong app.
 *
 * - Chạy thao tác database trên MỘT luồng nền dùng chung, để không làm đơ giao diện
 *   và để các thao tác chạy lần lượt theo đúng thứ tự.
 * - Sau mỗi lần ghi dữ liệu thành công, mọi ViewModel đang hoạt động tự tải lại
 *   (ví dụ ghi chi ở màn Đi chợ thì tổng chi ở màn Nhóm cũng cập nhật).
 * - busy() và error() là trạng thái chung của cả app, để hiện thanh tải và thông báo lỗi.
 */
public abstract class BaseViewModel extends AndroidViewModel {

    /** Một việc chạy ở luồng nền và trả về kết quả. */
    public interface Task<T> {
        T run() throws Exception;
    }

    /** Một việc chạy ở luồng nền, không trả về kết quả. */
    public interface Action {
        void run() throws Exception;
    }

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Set<BaseViewModel> ACTIVE = new LinkedHashSet<>();
    private static final MutableLiveData<Boolean> BUSY = new MutableLiveData<>(false);
    private static final MutableLiveData<String> ERROR = new MutableLiveData<>();
    /** Số việc đang chạy; chỉ truy cập trên luồng chính. */
    private static int running;

    protected BaseViewModel(@NonNull Application app) {
        super(app);
        ACTIVE.add(this);
    }

    /** true khi đang có thao tác database chạy. */
    public static LiveData<Boolean> busy() {
        return BUSY;
    }

    /** Thông báo lỗi mới nhất (null nếu không có). */
    public static LiveData<String> error() {
        return ERROR;
    }

    public static void clearError() {
        ERROR.setValue(null);
    }

    /** Đọc lại dữ liệu từ database và cập nhật LiveData của màn hình. */
    public abstract void refresh();

    /**
     * Đọc dữ liệu ở luồng nền rồi trả kết quả về luồng chính.
     * @param errorMessage thông báo khi lỗi; null thì dùng thông báo của chính lỗi đó
     */
    protected <T> void read(Task<T> task, Consumer<T> onResult, String errorMessage) {
        start();
        WORKER.execute(() -> {
            try {
                T value = task.run();
                MAIN.post(() -> {
                    onResult.accept(value);
                    finish();
                });
            } catch (Exception e) {
                MAIN.post(() -> {
                    finish();
                    ERROR.setValue(errorMessage != null ? errorMessage
                            : e.getMessage() != null ? e.getMessage() : "Không thể đọc tệp");
                });
            }
        });
    }

    /** Ghi dữ liệu ở luồng nền. Thành công thì gọi onSuccess (có thể null) rồi tải lại mọi màn hình. */
    protected void write(Action action, Runnable onSuccess) {
        writeAndGet(() -> {
            action.run();
            return null;
        }, onSuccess == null ? null : result -> onSuccess.run());
    }

    /** Giống write() nhưng trả về kết quả, ví dụ id của nhóm vừa tạo. */
    protected <T> void writeAndGet(Task<T> task, Consumer<T> onSuccess) {
        start();
        WORKER.execute(() -> {
            try {
                T result = task.run();
                MAIN.post(() -> {
                    if (onSuccess != null) onSuccess.accept(result);
                    refreshAll();
                    finish();
                });
            } catch (Exception e) {
                MAIN.post(() -> {
                    finish();
                    ERROR.setValue(writeError(e));
                });
            }
        });
    }

    private static String writeError(Exception e) {
        if (e instanceof SQLiteConstraintException) return "Thành viên đã có giao dịch. Bạn chỉ có thể đổi tên.";
        return e.getMessage() == null ? "Không thể lưu. Vui lòng thử lại." : e.getMessage();
    }

    private static void refreshAll() {
        for (BaseViewModel model : new ArrayList<>(ACTIVE)) model.refresh();
    }

    private static void start() {
        running++;
        BUSY.setValue(true);
    }

    private static void finish() {
        running--;
        if (running == 0) BUSY.setValue(false);
    }

    @Override
    protected void onCleared() {
        ACTIVE.remove(this);
    }
}
