# SplitMate 1.1

Ứng dụng Android Java/XML và SQLite, package `com.example.quanlychitieu`. Hoạt động offline.

## Đi chợ theo luồng mới
1. Chọn nhóm ở đầu màn hình Đi chợ.
2. Thêm **tên món**, **số lượng**, **đơn vị** và **đơn giá dự kiến**. Ví dụ 500 g gạo hoặc 10 quả trứng. Số lượng là số nguyên; dùng g/ml cho lượng lẻ. Giá chưa biết có thể để 0.
3. Tab **Cần mua** chứa món chưa mua. Tích checkbox khi mua xong; món chuyển sang tab **Đã mua**.
4. Chọn **Ghi chi … món đã mua**, chọn các món cùng hóa đơn, nhập **tổng số tiền thực trả**, người trả và người chia.
5. Các món được liên kết với một hóa đơn, chặn ghi chi hai lần. Chạm món đã ghi chi để mở hóa đơn. Sửa số tiền ở hóa đơn liên kết; không phải ở giá dự kiến.

Tổng dự kiến chỉ tính các món chưa mua. Tổng đã ghi chi tính mỗi hóa đơn đi chợ liên kết một lần, kể cả hóa đơn chứa nhiều món.
Xóa hóa đơn liên kết sẽ đưa các món về trạng thái đã mua chưa ghi chi. Muốn bỏ đánh dấu một món đã ghi chi, cần xóa hóa đơn liên kết trước.
Không tự tạo chi tiêu chỉ vì tích checkbox.

## Nhóm, chi tiêu và công nợ
- Nhóm → Tạo nhóm → tab Thành viên → thêm người → tab Chi tiêu → thêm khoản chi.
- Biểu mẫu xem trước phần tiền mỗi người, chia chính xác từng đồng.
- **Bộ lọc** trong tab Chi tiêu hỗ trợ tên, tháng, danh mục, người trả; có nút Xóa bộ lọc.
- Tab Công nợ hiển thị **ai trả ai**, số dư, biểu đồ thanh theo danh mục và lịch sử thanh toán.
- Nút **Ghi nhận đã trả** điền sẵn thông tin. Chỉ lưu sau khi đã chuyển tiền thực tế; có thể điều chỉnh số tiền để trả một phần.
- Gợi ý ghép người còn nợ với người cần nhận theo số dư. Không cam kết số giao dịch tối thiểu toàn cục.
- Tổng chi tiêu và biểu đồ là toàn bộ lịch sử của nhóm, độc lập với bộ lọc lịch sử.

## Sao lưu, khôi phục và demo
Nút **⋮** ở góc trên có:
- **Sao lưu dữ liệu**: chọn nơi lưu tệp JSON bằng trình chọn tệp Android. Tệp chứa toàn bộ nhóm, thành viên, khoản chi, phần chia, đi chợ và thanh toán.
- **Khôi phục dữ liệu**: chọn tệp, xem số lượng dữ liệu rồi xác nhận. Dữ liệu được thêm thành nhóm mới có hậu tố `(khôi phục)`; các nhóm hiện có được giữ nguyên. Nhập lại cùng tệp sẽ tạo thêm một bản.
- **Tạo dữ liệu demo**: thêm nhóm `Demo · Phòng trọ xanh`, 3 thành viên và dữ liệu mẫu. Gọi lại sẽ mở nhóm demo đã có. Có thể xóa nhóm demo như nhóm thường.

Bản sao lưu là JSON không mã hóa; người có tệp có thể đọc dữ liệu. Giới hạn nhập tệp 10 MB và tổng 100.000 dòng.

## Cấu trúc
- `MainActivity`: điều hướng Trang chủ, Nhóm, Đi chợ.
- `WorkspaceFragment`: các màn hình và biểu mẫu.
- `WorkspaceAdapter`: RecyclerView/ListAdapter, DiffUtil cập nhật từng dòng.
- `WorkspaceModel`: ViewModel/LiveData; mọi thao tác SQLite và tệp chạy trên executor nền.
- `DatabaseHelper`: 6 bảng, transaction cho hóa đơn/phần chia/chuyển món đi chợ; migration DB 1/2 → 3 giữ dữ liệu và thêm đơn vị, liên kết hóa đơn.
- `BackupStore`: kiểm tra cấu trúc, khóa liên kết, tổng phần chia trước khi khôi phục trong transaction.
- `DebtPlanner`: tính gợi ý thanh toán từ số dư.

## Chạy và kiểm thử
Mở Android Studio, Sync Gradle và Run biến thể **debug** để dùng ứng dụng.
```
./gradlew.bat assembleDebug testQaUnitTest lintDebug
./gradlew.bat connectedQaAndroidTest
```
Bản **qa** có applicationId `com.example.quanlychitieu.qa` riêng; kiểm thử không gỡ app chính hoặc dùng database của app chính.
Các kiểm thử bao gồm chia tiền/phần lẻ, gợi ý thanh toán, tạo nhóm, lọc lịch sử, trả nợ, trạng thái đi chợ sau khi tái tạo Activity, ghi chi thực tế, chống ghi trùng, migration và sao lưu/khôi phục đúng liên kết.
APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Giới hạn
Chưa có đồng bộ máy chủ, quét hóa đơn, chia theo tỷ lệ hoặc phân trang cho lịch sử rất lớn. Chưa đo FPS trên điện thoại thật. Hộp nhập liệu hiện không giữ bản nháp khi xoay màn hình.
