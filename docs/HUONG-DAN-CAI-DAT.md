# Hướng dẫn cài đặt và chạy phần mềm Quản lý Kho

Tài liệu dành cho **người cài đặt** — người thiết lập phần mềm trên máy tính lần đầu.

Hướng dẫn sử dụng hằng ngày nằm ở [`HUONG-DAN-SU-DUNG.md`](HUONG-DAN-SU-DUNG.md).

---

## Mục lục

1. [Yêu cầu hệ thống](#1-yêu-cầu-hệ-thống)
2. [Cài đặt](#2-cài-đặt)
3. [Chạy phần mềm](#3-chạy-phần-mềm)
4. [Cấu hình kết nối cơ sở dữ liệu](#4-cấu-hình-kết-nối-cơ-sở-dữ-liệu)
5. [Sao lưu và phục hồi](#5-sao-lưu-và-phục-hồi)
6. [Khắc phục sự cố cài đặt](#6-khắc-phục-sự-cố-cài-đặt)

---

## 1. Yêu cầu hệ thống

| Thành phần | Yêu cầu tối thiểu | Ghi chú |
|------------|-------------------|---------|
| **Java** | JDK 17 trở lên | Kiểm tra bằng `java -version` |
| **MySQL** | 8.0 trở lên | Đang chạy, không phải cài nhưng chưa khởi động |
| **Maven** | 3.6+ | Chỉ cần khi **build** từ mã nguồn |

Hệ điều hành: chạy được trên **Windows, macOS, Linux** — đường dẫn lưu trữ được dò tự động, không hardcode.

**Dung lượng cần:** khoảng 500 MB (chủ yếu là Java runtime).

---

## 2. Cài đặt

### Bước 1: Kiểm tra Java

Mở terminal (hoặc Command Prompt trên Windows) và chạy:

```bash
java -version
```

Phải hiện `17` trở lên. Nếu báo không tìm thấy lệnh, cài **Eclipse Temurin JDK 17** hoặc cao hơn.

### Bước 2: Khởi động MySQL

Đảm bảo dịch vụ MySQL đang chạy.

**macOS (Homebrew):**
```bash
brew services start mysql
```

**Windows:**
Kiểm tra trong Services, hoặc chạy `net start mysql`.

### Bước 3: Tạo database

Mở MySQL CLI hoặc MySQL Workbench, chạy file `sql/schema.sql`:

```bash
mysql -u root -p < sql/schema.sql
```

Script này sẽ:
- Tạo database `warehouse_db`
- Tạo toàn bộ bảng và ràng buộc
- **Tạo sẵn 2 tài khoản** với mật khẩu đã hash BCrypt

### Bước 4: Kiểm tra kết nối

```bash
mysql -u root -p -e "USE warehouse_db; SHOW TABLES;"
```

Phải hiện danh sách bảng (`users`, `products`, `stock_transactions`…).

Nếu chưa thấy, sang [mục 4](#4-cấu-hình-kết-nối-cơ-sở-dữ-liệu).

---

## 3. Chạy phần mềm

### Cách A: Chạy từ mã nguồn (khuyến nghị cho người phát triển)

**Build:**

```bash
mvn clean package
```

Lệnh này tạo file `target/warehouse_management.jar` — đây là **fat-jar** đã gộp sẵn mọi thư viện, chạy độc lập.

**Chạy:**

```bash
java -jar target/warehouse_management.jar
```

> **Sau khi build, `pom.xml` chứa đủ mọi dependency** — gồm cả `log4j-api` và `log4j-to-slf4j` cần cho chức năng xuất Excel. Nếu bạn build bằng `javac` trực tiếp với classpath `lib/*` thì cần giữ đủ 2 file này trong `lib/`.

### Cách B: Chạy bằng IntelliJ IDEA / Eclipse

1. Import dự án như **Maven Project**.
2. Đợi Maven tải dependency xong.
3. Mở `src/main/java/com/utc/warehouse/App.java`.
4. Bấm nút **Run** cạnh phương thức `main`.

### Đăng nhập lần đầu

| Tên đăng nhập | Mật khẩu | Vai trò |
|---------------|----------|---------|
| `admin` | `admin123` | ADMIN |
| `staff` | `staff123` | STAFF |

> ⚠️ **Đổi mật khẩu `admin` ngay sau lần đăng nhập đầu tiên.** Thông tin này nằm công khai trong mã nguồn.
>
> Đổi bằng cách: **Quản lý người dùng** (`Ctrl+9`) → chọn `admin` → `Ctrl+R` → đặt mật khẩu mới.

---

## 4. Cấu hình kết nối cơ sở dữ liệu

Thông tin kết nối nằm trong file `src/main/resources/META-INF/persistence.xml`:

```xml
jakarta.persistence.jdbc.url      = jdbc:mysql://localhost:3306/warehouse_db?...
jakarta.persistence.jdbc.user     = root
jakarta.persistence.jdbc.password = 123456
```

| Mục đích | Thay đổi gì |
|----------|-------------|
| MySQL ở host khác | Đổi `localhost` trong URL |
| MySQL ở port khác | Đổi `3306` trong URL |
| Tài khoản MySQL khác | Đổi `jakarta.persistence.jdbc.user` |
| Mật khẩu khác | Đổi `jakarta.persistence.jdbc.password` |

**Sau khi sửa `persistence.xml`, phải build lại** (`mvn clean package`) vì file được đóng gói vào trong `.jar`.

### Đổi vị trí lưu file

Ảnh sản phẩm, file Excel và bản sao lưu được lưu trong các thư mục con của thư mục gốc ứng dụng:

```
warehouse_management/
├── exports/          # file .xlsx xuất từ báo cáo
├── product_images/   # ảnh sản phẩm (DB chỉ lưu tên file)
└── backups/          # bản sao lưu database
```

Thư mục gốc được **dò tự động** (từ `pom.xml`, `.git`, hoặc thư mục cha của file `.jar`) nên cùng mã nguồn chạy được trên cả 3 hệ điều hành. Muốn chỉ định vị trí khác:

| Mục đích | Biến môi trường | System property |
|----------|-----------------|-----------------|
| Thư mục gốc | `WAREHOUSE_HOME` | `-Dwarehouse.home` |
| Thư mục export | `WAREHOUSE_EXPORTS` | `-Dwarehouse.exports` |
| Thư mục ảnh | `WAREHOUSE_IMAGES` | `-Dwarehouse.images` |
| Thư mục sao lưu | `WAREHOUSE_BACKUPS` | `-Dwarehouse.backups` |

```bash
# Ví dụ: lưu file export sang ổ D: trên Windows
java -Dwarehouse.exports="D:/warehouse/exports" -jar warehouse.jar
```

> Nếu không dò được thư mục gốc (chạy từ classpath lạ), phần mềm lùi về `~/.warehouse_management/`. Mọi thư mục con đều được **tạo tự động** nếu chưa tồn tại.

---

## 5. Sao lưu và phục hồi

### Sao lưu cơ sở dữ liệu

```bash
mysqldump -u root -p --single-transaction warehouse_db > backups/warehouse_$(date +%Y%m%d).sql
```

Cần `--single-transaction` để không khóa bảng khi phần mềm đang chạy.

### Phục hồi

```bash
mysql -u root -p warehouse_db < backups/warehouse_20261021.sql
```

### Sao lưu toàn bộ dữ liệu

Ngoài database, cần copy thêm thư mục **`product_images/`** — chứa ảnh sản phẩm. Nếu chỉ sao lưu database mà không copy ảnh, các sản phẩm vẫn còn dữ liệu nhưng **mất ảnh**.

---

## 6. Khắc phục sự cố cài đặt

### `Communications link failure`

MySQL chưa chạy, hoặc đang ở host/port khác.

```bash
# macOS
brew services list | grep mysql
# Windows
net start mysql
```

Sau đó kiểm tra lại `jakarta.persistence.jdbc.url` trong `persistence.xml`.

### `Access denied for user 'root'@'localhost'`

Sai tên đăng nhập hoặc mật khẩu MySQL. Thử đăng nhập trực tiếp:

```bash
mysql -u root -p
```

Nếu đăng nhập được, cập nhật `jakarta.persistence.jdbc.user` và `jakarta.persistence.jdbc.password` cho khớp.

### `Unknown database 'warehouse_db'`

Chưa chạy `sql/schema.sql`. Xem [Bước 3 ở mục 2](#bước-3-tạo-database).

### `UnsupportedClassVersionError`

Bản Java quá cũ. Cần **JDK 17 trở lên**.

```bash
java -version
```

### Không xuất được file Excel

Triệu chứng: bấm **Xuất Excel** bị lỗi `NoClassDefFoundError: org/apache/logging/log4j/LogManager`.

Nguyên nhân: thiếu thư viện log4j mà Apache POI bắt buộc cần.

Cách khắc phục:
- **Nếu build bằng Maven** → chạy `mvn clean package`, `pom.xml` đã có sẵn dependency.
- **Nếu build bằng `javac` trực tiếp** → bảo đảm thư mục `lib/` có đủ:
  - `log4j-api-2.21.1.jar` — thư viện log4j mà POI gọi trực tiếp
  - `log4j-to-slf4j-2.21.1.jar` — bridge chuyển log của POI về Logback, tránh in cảnh báo đỏ

### Cảnh báo đỏ `Log4j2 could not find a logging implementation`

Không phải lỗi — chỉ là thư báo thiếu log4j-core. Cài `log4j-to-slf4j` như trên sẽ giải quyết, log của POI sẽ hòa vào hệ thống Logback sẵn có của ứng dụng.

### Màn hình trắng khi mở phần mềm

Mở log để xem stack trace. Đây có thể do:
- Sai cấu hình kết nối database
- Database chưa có bảng (chạy `sql/schema.sql`)

Cấu hình log nằm trong `src/main/resources/logback.xml`. Mặc định ghi ra console; sửa file này để ghi ra file thay vì console.

### Không thấy menu Quản trị

Đảm bảo đang đăng nhập bằng tài khoản **ADMIN**. Các menu *Quản lý nhân viên*, *Quản lý người dùng*, *Lịch sử thao tác* chỉ hiện với ADMIN.

---

## Xem thêm

| Tài liệu | Dành cho |
|----------|----------|
| [`HUONG-DAN-SU-DUNG.md`](HUONG-DAN-SU-DUNG.md) | Người dùng cuối — hướng dẫn thao tác hằng ngày |
| [`../README.md`](../README.md) | Người đọc kỹ thuật — kiến trúc, schema, quy tắc nghiệp vụ |