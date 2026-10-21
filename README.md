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

### Đăng nhập

Sau khi chạy `sql/schema.sql`, đăng nhập với 1 trong 2 tài khoản đã seed sẵn:

| User  | Password   | Role  |
|-------|------------|-------|
| admin | admin123   | ADMIN |
| staff | staff123   | STAFF |

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

Ảnh sản phẩm, file CSV export và backup database được lưu **ngoài DB**, trong các thư mục con
của thư mục gốc ứng dụng (dò tự động từ `pom.xml` / `.git`, hoặc thư mục cha file `.jar`):

```
warehouse_management/
├── exports/          # file CSV xuất từ báo cáo
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
| SLF4J + Logback  | 2.0.12 / 1.5.6  |
| Maven Shade      | 3.5.3           |

## Quy tắc nghiệp vụ

- **Nhập kho**: cộng trực tiếp vào tồn (tồn tính động).
- **Xuất kho**: phải có tồn >= số lượng xuất. Service sẽ kiểm tra trước khi lưu, rollback nếu thiếu.
- **Mỗi thao tác ghi** chạy trong 1 JPA transaction. Rollback tự động nếu exception.
- **Phân quyền**: menu "Quản lý người dùng" chỉ hiện với role ADMIN.

## Khắc phục sự cố

**Lỗi `Communications link failure`**: MySQL chưa chạy, hoặc sai port. Kiểm tra `DB_URL`.

**Lỗi `Access denied for user`**: Sai username/password trong `DB_USER` / `DB_PASSWORD`.

**Lỗi `Unknown database 'warehouse_db'`**: Chưa chạy `sql/schema.sql`.

**Màn hình trắng khi mở MainFrame**: Mở log, kiểm tra stack trace. Thường do query JPQL sai trong `ReportDAO`.

## Phát triển

- Có thể mở rộng thêm: in hóa đơn, xuất CSV/Excel, thống kê doanh thu theo khoảng thời gian, quản lý nhiều kho.
- Để chuyển sang H2 Database (test trong bộ nhớ), chỉ cần đổi driver và dialect trong `persistence.xml`.

## Tác giả

UTC — đồ án môn học / bài tập lớn.
