# Warehouse Management

App desktop quản lý kho viết bằng **Java Swing + MySQL**, truy cập dữ liệu qua **Hibernate 6 / JPA**, build bằng **Maven**, mật khẩu hash bằng **BCrypt**.

## Tính năng

- Đăng nhập với phân quyền `ADMIN` / `STAFF`
- Quản lý danh mục, sản phẩm (có cảnh báo tồn tối thiểu)
- Quản lý nhà cung cấp, khách hàng
- Tạo phiếu nhập kho / phiếu xuất kho với nhiều sản phẩm
- Lịch sử nhập/xuất + xem chi tiết từng phiếu
- Báo cáo tồn kho tổng hợp, cảnh báo sản phẩm dưới mức tối thiểu
- Quản lý người dùng (chỉ ADMIN)

## Yêu cầu môi trường

- Java 17+ (JDK)
- MySQL 8.x đang chạy
- Maven 3.6+

## Tài liệu

| Tài liệu | Dành cho |
|----------|----------|
| [`docs/HUONG-DAN-SU-DUNG.md`](docs/HUONG-DAN-SU-DUNG.md) | **Người dùng cuối** — hướng dẫn thao tác từng màn hình, phím tắt, câu hỏi thường gặp |
| [`docs/HUONG-DAN-CAI-DAT.md`](docs/HUONG-DAN-CAI-DAT.md) | **Người cài đặt** — cài đặt, tạo DB, build & chạy, khắc phục sự cố |
| [`docs/LUONG-HOAT-DONG.md`](docs/LUONG-HOAT-DONG.md) | **Sơ đồ luồng** — 12 sơ đồ nghiệp vụ: đăng nhập, nhập/xuất kho, PO, hoá đơn, thu tiền, báo cáo |
| [`docs/README.md`](docs/README.md) | Mục lục tài liệu |

## Cài đặt & chạy

### Bước 1: Tạo database

Mở MySQL Workbench / CLI và chạy file `sql/schema.sql`:

```bash
mysql -u root -p123456 < sql/schema.sql
```

Script sẽ:
- Tạo database `warehouse_db` (với `createDatabaseIfNotExist=true` trong URL cũng tự tạo)
- Tạo toàn bộ bảng
- **Seed sẵn 2 tài khoản** (mật khẩu đã hash BCrypt):

| Username | Password   | Role  |
|----------|------------|-------|
| admin    | admin123   | ADMIN |
| staff    | staff123   | STAFF |

### Bước 2: Kiểm tra kết nối DB

Thông tin kết nối đã được **gán cứng** trong `src/main/resources/META-INF/persistence.xml`:

```xml
jakarta.persistence.jdbc.url      = jdbc:mysql://localhost:3306/warehouse_db?...
jakarta.persistence.jdbc.user     = root
jakarta.persistence.jdbc.password = 123456
```

Nếu MySQL của bạn chạy ở host/port khác hoặc dùng tài khoản khác, **sửa trực tiếp 3 dòng đó** trong file `persistence.xml`. Không cần env var.

### Bước 3: Build & chạy

```bash
mvn clean package
java -jar target/warehouse_management.jar
```

Hoặc dùng shade plugin (đã cấu hình sẵn) để ra fat-jar chạy độc lập.

### Tài khoản mặc định

Lần chạy đầu tiên, nếu bảng `users` còn trống, app tự tạo sẵn 2 tài khoản:

| User  | Password   | Role  |
|-------|------------|-------|
| admin | admin123   | ADMIN |
| staff | staff123   | STAFF |

> **Đổi mật khẩu ngay sau lần đăng nhập đầu tiên.** Hai mật khẩu này nằm trong
> mã nguồn (`AuthService.bootstrapDefaultUsers`) và là thông tin công khai của
> bài tập. Màn đăng nhập **không** hiển thị chúng nữa — hãy đổi qua màn
> **Quản lý người dùng** (`Ctrl+R` → Reset mật khẩu).

Sau khi đổi mật khẩu `admin`, hệ thống sẽ không tạo lại tài khoản cũ nữa
(`bootstrapDefaultUsers` chỉ chạy khi bảng `users` rỗng hoàn toàn).

## Cấu trúc dự án

```
warehouse_management/
├── pom.xml                                     # Maven config
├── sql/schema.sql                              # Script tạo DB
├── src/main/java/com/utc/warehouse/
│   ├── App.java                                # Main class
│   ├── config/                                 # HibernateUtil, shutdown helper
│   ├── entity/                                 # 7 JPA entities
│   ├── enums/                                  # Role, TransactionType, PartnerType
│   ├── dao/                                    # GenericDAO + DAO cụ thể + ReportDAO
│   ├── service/                                # Business logic + AuthService (BCrypt)
│   ├── ui/                                     # Swing UI (Login, Main + các panel)
│   └── util/                                   # MoneyFormat, DateFormatUtil
└── src/main/resources/
    ├── META-INF/persistence.xml                # Hibernate config
    └── logback.xml                             # Logging
```

## Đường dẫn lưu trữ file (AppPaths)

Ảnh sản phẩm, file Excel export và backup database được lưu **ngoài DB**, trong các thư mục con
của thư mục gốc ứng dụng (dò tự động từ `pom.xml` / `.git`, hoặc thư mục cha file `.jar`):

```
warehouse_management/
├── exports/          # file .xlsx xuất từ báo cáo
├── product_images/   # ảnh sản phẩm (DB chỉ lưu tên file)
└── backups/          # backup database
```

Nhờ dò tự động nên **không có đường dẫn tuyệt đối hardcode** — cùng code chạy được trên
macOS, Windows và Linux. Muốn đổi vị trí lưu trữ, ghi đè qua biến môi trường hoặc system property:

| Mục đích          | Biến môi trường       | System property         |
|-------------------|-----------------------|-------------------------|
| Thư mục gốc       | `WAREHOUSE_HOME`      | `-Dwarehouse.home`      |
| Thư mục export    | `WAREHOUSE_EXPORTS`   | `-Dwarehouse.exports`   |
| Thư mục ảnh       | `WAREHOUSE_IMAGES`    | `-Dwarehouse.images`    |
| Thư mục backup    | `WAREHOUSE_BACKUPS`   | `-Dwarehouse.backups`   |

```bash
# Ví dụ: lưu export sang ổ D: trên Windows
java -Dwarehouse.exports="D:/warehouse/exports" -jar warehouse.jar
```

Nếu không dò được thư mục gốc (chạy từ classpath lạ), app lùi về `~/.warehouse_management/`.
Mọi thư mục con đều được tạo tự động nếu chưa tồn tại.

## Xuất Excel (.xlsx)

Các màn danh sách (sản phẩm, khách hàng, nhà cung cấp, danh mục, hóa đơn, đơn đặt hàng) và báo
cáo doanh thu đều có nút **"Xuất Excel"** (phím tắt `Ctrl+X`).

## Schema MySQL (tổng quan)

- `users` — người dùng (id, username, password_hash, role, active)
- `categories` — danh mục sản phẩm
- `products` — sản phẩm (sku, name, category_id, unit, price, min_stock)
- `suppliers` — nhà cung cấp
- `customers` — khách hàng
- `stock_transactions` — phiếu nhập/xuất (type, partner_id, partner_type, user_id, note)
- `stock_transaction_details` — chi tiết phiếu (transaction_id, product_id, quantity, unit_price)

**Tồn kho được tính động** bằng tổng `IMPORT - EXPORT` trên bảng chi tiết — không cần cột `on_hand` riêng.

## Công nghệ sử dụng

| Thành phần       | Phiên bản       |
|------------------|-----------------|
| Java             | 17              |
| MySQL Connector  | 8.3.0           |
| Hibernate ORM    | 6.4.4.Final     |
| Jakarta JPA API  | 3.1.0           |
| jBCrypt          | 0.4             |
| Apache POI       | 5.2.5           |
| SLF4J + Logback  | 2.0.12 / 1.5.6  |
| Maven Shade      | 3.5.3           |

## Quy tắc nghiệp vụ

- **Nhập kho**: cộng trực tiếp vào tồn (tồn tính động).
- **Xuất kho**: phải có tồn >= số lượng xuất. Service sẽ kiểm tra trước khi lưu, rollback nếu thiếu.
- **Mỗi thao tác ghi** chạy trong 1 JPA transaction. Rollback tự động nếu exception.
- **Phân quyền**: menu "Quản lý người dùng" chỉ hiện với role ADMIN.

### Quản lý người dùng (ADMIN)

Đầy đủ CRUD + các thao tác riêng cho tài khoản:

| Phím tắt | Chức năng |
|----------|-----------|
| `Ctrl+N` | Thêm người dùng |
| `Ctrl+E` | Sửa họ tên / role (không sửa username) |
| `Delete` | Xoá hoặc khoá (xem bên dưới) |
| `Ctrl+R` | Reset mật khẩu |
| `Ctrl+T` | Khoá / Mở tài khoản |
| `Ctrl+F` | Tìm theo username / họ tên |
| `Ctrl+X` | Xuất Excel |

**Về thao tác xoá** — bảng `stock_transactions` có
`FOREIGN KEY (user_id) REFERENCES users(id)` **không kèm `ON DELETE`**, nên MySQL
sẽ từ chối xoá hẳn một user đã từng phát sinh giao dịch / hoá đơn / thanh toán.

Vì vậy panel xử lý hai trường hợp:

- **Chưa có dữ liệu liên quan** → xoá hẳn, không thể hoàn tác.
- **Đã có dữ liệu liên quan** → báo số bản ghi bị liên quan và đề nghị **khoá tài
  khoản** (soft delete) thay vì xoá. Đây là cách đúng vì giữ nguyên lịch sử.

**Chốt an toàn** (không thể vô hiệu hoá từ UI):

- Không xoá được tài khoản **đang đăng nhập**.
- Phải còn ít nhất **1 tài khoản ADMIN đang hoạt động** — tránh tự khoá quyền
  quản trị khỏi hệ thống.

Username không sửa được sau khi tạo vì dữ liệu lịch sử đã ghi theo tên cũ.

### Quản lý nhân viên (ADMIN)

Màn chuyên biệt để **cấp tài khoản cho nhân viên** — chỉ hiện menu cho ADMIN.

Khác màn "Quản lý người dùng" ở 2 điểm:

- Chỉ hiển thị user role `STAFF`, **không cấp được quyền ADMIN** — cấp quyền quản
  trị thì sang màn "Quản lý người dùng".
- Có nút **Sinh mật khẩu ngẫu nhiên** (8 ký tự, loại ký tự dễ nhầm `0/O`, `1/l/I`).
  Sau khi cấp, app hiển thị thông tin đăng nhập một lần để admin chuyển cho nhân
  viên.

| Phím tắt | Chức năng |
|----------|-----------|
| `Ctrl+N` | Cấp tài khoản nhân viên |
| `Ctrl+E` | Sửa họ tên |
| `Delete` | Xoá hoặc khoá (xem bên dưới) |
| `Ctrl+R` | Đổi mật khẩu |
| `Ctrl+T` | Khoá / Mở tài khoản |
| `Ctrl+F` | Tìm mã NV / họ tên |
| `Ctrl+X` | Xuất Excel |

Ngoài ra có bộ lọc **Tất cả / Đang hoạt động / Đã khoá**, và double-click để
xem chi tiết nhân viên.

Nhân viên không thấy menu này (và cũng không thấy menu "Quản lý người dùng",
"Lịch sử thao tác") — quyền quản trị chỉ dành cho ADMIN.

Quy tắc xoá giống màn người dùng: chưa có giao dịch thì xoá hẳn, đã có giao dịch
thì đề nghị khoá tài khoản để giữ lịch sử.

## Khắc phục sự cố

**Lỗi `Communications link failure`**: MySQL chưa chạy, hoặc sai port. Kiểm tra `DB_URL`.

**Lỗi `Access denied for user`**: Sai username/password trong `DB_USER` / `DB_PASSWORD`.

**Lỗi `Unknown database 'warehouse_db'`**: Chưa chạy `sql/schema.sql`.

**Màn hình trắng khi mở MainFrame**: Mở log, kiểm tra stack trace. Thường do query JPQL sai trong `ReportDAO`.


## Phát triển

- Có thể mở rộng thêm: in hóa đơn, xuất PDF, thống kê doanh thu theo khoảng thời gian, quản lý nhiều kho.
- Để chuyển sang H2 Database (test trong bộ nhớ), chỉ cần đổi driver và dialect trong `persistence.xml`.

## Tác giả

UTC — đồ án môn học / bài tập lớn.
