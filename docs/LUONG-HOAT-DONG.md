# Luồng hoạt động chính của ứng dụng

Sơ đồ các luồng nghiệp vụ chính của phần mềm Quản lý Kho. Mọi luồng trong tài liệu này được đối chiếu trực tiếp với mã nguồn hiện tại.

**Cách đọc:** nút bo góc là hành động của người dùng · hộp chữ nhạt là quyết định hệ thống · hộp đỏ là lỗi hoặc chặn.

---

## 1. Đăng nhập và phân quyền

```mermaid
flowchart TD
    A([Mở ứng dụng]) --> B["LoginFrame<br/>Nhập tên + mật khẩu"]
    B --> C{"Tên/mật khẩu<br/>để trống?"}
    C -->|Có| E["Báo lỗi<br/>quay lại ô nhập"]
    C -->|Không| D{"Tìm thấy<br/>username?"}
    D -->|Không| E
    D -->|Có| F{"active = TRUE?"}
    F -->|Không — tài khoản bị khoá| E
    F -->|Có| G{"BCrypt<br/>khớp mật khẩu?"}
    G -->|Không| E
    G -->|Có| H["Lưu currentUser<br/>mở MainFrame"]

    H --> I{"Vai trò?"}
    I -->|ADMIN| J["Hiện 17 menu<br/>kèm 3 menu quản trị"]
    I -->|STAFF| K["Ẩn các menu quản trị"]

    K --> L["Nhân viên không thấy:<br/>Quản lý nhân viên<br/>Quản lý người dùng<br/>Lịch sử thao tác"]

    J --> M["Đăng xuất (Ctrl+L)"]
    M --> N["xóa currentUser<br/>quay lại LoginFrame"]
    K --> M
```

**Cơ chế kiểm tra:**

| Bước | Nơi kiểm tra | Hành vi khi sai |
|------|---------------|-----------------|
| Ô trống | `AuthService.login()` | `Optional.empty()` |
| Không tồn tại user | `userDAO.findByUsername()` | `Optional.empty()` |
| Tài khoản bị khoá | `u.getActive()` | Từ chối đăng nhập |
| Sai mật khẩu | BCrypt `checkpw()` | `Optional.empty()` |

> Phân quyền không phải kiểm tra từng thao tác mà thực hiện **một lần lúc dựng menu** (`MainFrame.initMenu()`), dựa trên `AuthService.isAdmin()`. Menu không được tạo thì không thao tác được.

---

## 2. Nhập kho — làm tăng tồn

```mermaid
flowchart TD
    A(["Phiếu nhập"]) --> B["Chọn nhà cung cấp"]
    B --> C["Chọn sản phẩm + số lượng + đơn giá"]
    C --> D{"Số lượng<br/>nguyên dương?"}
    D -->|Không| E["Báo lỗi<br/>không thêm dòng"]
    D -->|Có| F["+ Thêm dòng (Ctrl+Enter)<br/>thêm vào bảng chi tiết"]
    F --> G{"Thêm dòng<br/>nữa?"}
    G -->|Có| C
    G -->|Không| H["Ctrl+S — Lưu phiếu"]

    H --> I{"Phiếu có<br/>ít nhất 1 sản phẩm?"}
    I -->|Không| J["Lỗi: Phiếu nhập phải có<br/>ít nhất 1 sản phẩm"]
    I -->|Có| K["Mở 1 JPA transaction"]
    K --> L["Lưu stock_transactions<br/>type = IMPORT"]
    L --> M["Lưu stock_transaction_details<br/>SL + đơn giá từng dòng"]
    M --> N["COMMIT"]

    N --> O["Tồn kho TĂNG<br/>tự động theo tổng số lượng nhập"]
    O --> P["Ghi vào Lịch sử nhập / xuất"]
```

> **Nhập kho không cần kiểm tra tồn** — hàng vào kho luôn được chấp nhận.

---

## 3. Xuất kho — làm giảm tồn (luồng có kiểm soát nghiêm nhất)

Luồng này có **hai lớp chặn**: chặn ngay khi thêm dòng (trên UI) và chặn lúc lưu (trong service).

```mermaid
flowchart TD
    A(["Phiếu xuất"]) --> B["Chọn khách hàng"]
    B --> C["Chọn sản phẩm + SL xuất + đơn giá"]
    C --> D["+ Thêm dòng (Ctrl+Enter)"]

    D --> E{"Lớp 1 — KIỂM TRA TẠI UI<br/>SL + đã thêm trong phiếu<br/>> tồn kho?"}
    E -->|Có| F["Chặn ngay<br/>'Không đủ tồn'<br/>Dòng KHÔNG được thêm"]
    E -->|Không| G["Thêm vào bảng chi tiết"]

    G --> H{"Thêm dòng<br/>nữa?"}
    H -->|Có| C
    H -->|Không| I["Ctrl+S — Lưu phiếu"]

    I --> J{"Phiếu có<br/>ít nhất 1 sản phẩm?"}
    J -->|Không| K["Lỗi dữ liệu"]
    J -->|Có| L["Mở 1 JPA transaction"]

    L --> M{"Lớp 2 — KIỂM TRA TRONG SERVICE<br/>SL xuất > tồn?"}
    M -->|Có| N["ROLLBACK<br/>toàn bộ phiếu bị hoàn tác"]
    M -->|Không| O["Lưu phiếu EXPORT + chi tiết"]
    O --> P["COMMIT"]
    P --> Q["Tồn kho GIẢM"]
```

**Vì sao phải có hai lớp:**

| Lớp | Vị trí | Bắt trường hợp |
|-----|--------|---------------|
| 1 | `ExportPanel.addLine()` | Chặn sớm, báo rõ số tồn còn lại và số đã thêm trong phiếu |
| 2 | `StockService.createExport()` | Chặn từ xa — dữ liệu có thể đã thay đổi giữa lúc thêm dòng và lúc bấm Lưu |

> Lớp 2 là lớp **bảo vệ dữ liệu**: hai người dùng có thể cùng xuất một món hàng, và tồn thay đổi giữa lúc mở form và lúc bấm Lưu.

---

## 4. Công thức tính tồn kho

Tồn **không được lưu** trong database — luôn tính động từ lịch sử giao dịch:

```mermaid
flowchart LR
    A["stock_transaction_details"] --> B["Nhóm theo product_id"]
    B --> C["CASE WHEN type = IMPORT<br/>→ +quantity<br/>ELSE<br/>→ −quantity"]
    C --> D["SUM(...)"]
    D --> E["Tồn kho hiện tại"]

    F["stock_transactions.type"] --> C
```

```sql
Tồn hiện tại = SUM(CASE WHEN type = 'IMPORT'
                        THEN quantity
                        ELSE -quantity END)
```

Điểm cần hiểu: đổi một phiếu cũ sẽ **lập tức làm thay đổi** tồn kho của tất cả báo cáo, vì không có con số nào được "đóng băng".

---

## 5. Đơn đặt hàng (PO) — từ đặt hàng đến nhận kho

```mermaid
stateDiagram-v2
    [*] --> Nháp: + Tạo đơn (Ctrl+N)

    Nháp --> Đã_đặt: Duyệt →
    Đã_đặt --> Nhận_1_phần: Nhận hàng (Ctrl+R)
    Nhận_1_phần --> Nhận_1_phần: Nhận tiếp
    Nhận_1_phần --> Đã_nhận_đủ: Đủ số lượng

    Nháp --> Đã_hủy: Hủy đơn
    Đã_đặt --> Đã_hủy: Hủy đơn
    Nhận_1_phần --> Đã_hủy: Hủy đơn

    Đã_nhận_đủ --> [*]
    Đã_hủy --> [*]
```

**Luồng "Nhận hàng" kết nối PO với kho:**

```mermaid
flowchart TD
    A(["Nhấn Nhận hàng"]) --> B{"Trạng thái là<br/>Đã đặt / Nhận 1 phần?"}
    B -->|Không| C["Từ chối: Chỉ nhận hàng cho<br/>đơn Đã đặt / Nhận 1 phần"]
    B -->|Có| D["Nhập số lượng thực nhận"]
    D --> E["Tạo phiếu NHẬP kho<br/>tự động"]
    E --> F["Tồn kho tăng"]
    F --> G{"Đã nhận đủ<br/>số lượng đặt?"}
    G -->|Có| H["Trạng thái: Đã nhận đủ"]
    G -->|Không| I["Trạng thái: Nhận 1 phần<br/>và có thể nhận tiếp"]
```

> PO là **ý định mua hàng** (hàng chưa về), phiếu nhập là **hàng đã thực sự về kho**. Bấm "Nhận hàng" là cầu nối giữa hai khái niệm này.

---

## 6. Hoá đơn bán hàng và thu tiền

Tạo hoá đơn ghi **cả phiếu xuất lẫn hoá đơn trong một transaction** — nên không thể xuất hàng mà không phát sinh tài chính.

```mermaid
flowchart TD
    A(["+ Tạo hoá đơn (Ctrl+N)"]) --> B["Chọn khách hàng<br/>(để trống = Khách lẻ)"]
    B --> C["Chọn sản phẩm<br/>tự điền đơn giá + VAT"]
    C --> D["Nhập SL, VAT %, chiết khấu (đ)"]
    D --> E{"Hoá đơn có<br/>ít nhất 1 dòng?"}
    E -->|Không| F["Lỗi"]
    E -->|Có| G["Mở 1 JPA transaction"]

    G --> H["BƯỚC 1 — Tạo phiếu XUẤT kho<br/>→ tồn kho giảm"]
    H --> I{"Tồn kho đủ?"}
    I -->|Không| J["ROLLBACK<br/>không tạo được hoá đơn"]
    I -->|Có| K["BƯỚC 2 — Tạo hoá đơn"]

    K --> L["Chốt giá vốn tại thời điểm bán<br/>(cost price snapshot)"]
    L --> M["Tính từng dòng:<br/>Thành tiền = SL × ĐG + VAT − CK"]
    M --> N["Tổng = Tổng tiền hàng + VAT − Chiết khấu"]
    N --> O["Trạng thái: ISSUED<br/>(Đã phát hành)"]
    O --> P["COMMIT"]
```

**Vì sao phải chốt giá vốn riêng?** Vì giá vốn của sản phẩm có thể thay đổi sau này, nhưng lợi nhuận của hoá đơn đã phát hành phải giữ nguyên đúng như ngày bán.

**Vòng đời thanh toán:**

```mermaid
stateDiagram-v2
    [*] --> Đã_phát_hành: Tạo hoá đơn
    Đã_phát_hành --> Trả_một_phần: Ghi nhận thu (Ctrl+P)
    Trả_một_phần --> Đã_thanh_toán: Thu đủ
    Đã_phát_hành --> Đã_thanh_toán: Thu đủ ngay

    Đã_phát_hành --> Đã_huỷ: Huỷ
    Trả_một_phần --> Đã_huỷ: Huỷ
    Đã_thanh_toán --> [*]
    Đã_huỷ --> [*]
```

**Kiểm tra khi ghi nhận thu tiền:**

```mermaid
flowchart TD
    A(["Ghi nhận thu (Ctrl+P)"]) --> B["Nhập số tiền thu"]
    B --> C{"Số tiền > 0?"}
    C -->|Không| D["Lỗi: Số tiền thu phải > 0"]
    C -->|Có| E{"Vượt quá<br/>công nợ của hoá đơn?"}
    E -->|Có| F["Chặn: Số tiền thu vượt công nợ"]
    E -->|Không| G["Lưu khoản thu vào payments"]
    G --> H{"Thu đủ chưa?"}
    H -->|Chưa| I["Trạng thái: Trả một phần"]
    H -->|Đủ| J["Trạng thái: Đã thanh toán"]
```

---

## 7. Báo cáo và công nợ

```mermaid
flowchart TD
    A["Dữ liệu giao dịch<br/>(phiếu nhập, phiếu xuất, hoá đơn, thanh toán)"]

    A --> B["Báo cáo tồn kho"]
    A --> C["Báo cáo doanh thu"]
    A --> D["Top sản phẩm bán chạy"]
    A --> E["Công nợ khách hàng"]

    B --> B1["Tồn hiện tại từng sản phẩm"]
    B --> B2["So sánh với tồn tối thiểu"]
    B2 --> B3["Cảnh báo đỏ:<br/>dưới mức tối thiểu"]

    C --> C1["Doanh thu / Giảm giá / Doanh thu thuần"]
    C --> C2["Giá vốn COGS"]
    C1 --> C3["Lợi nhuận + Margin %"]

    E --> E1["Tổng phải thu"]
    E --> E2["Đã thu"]
    E1 --> E3["Còn nợ = Tổng phải thu − Đã thu"]
    E2 --> E3
```

> Mọi báo cáo đều **tính lại từ dữ liệu gốc** mỗi khi mở, nên luôn khớp với lịch sử giao dịch — không có trường hợp báo cáo lệch với thực tế do dữ liệu bị "đóng băng" sai.

---

## 8. Xoá và khoá dữ liệu — vì sao phần mềm hay "từ chối xoá"

```mermaid
flowchart TD
    A(["Bấm Xoá"]) --> B{"Mục đó đã có<br/>dữ liệu liên quan?"}

    B -->|Chưa có| C["XOÁ HẲN<br/>không thể hoàn tác"]
    B -->|Đã có| D["Báo số bản ghi liên quan<br/>đề nghị KHOÁ tài khoản"]

    C --> E["Giữ nguyên đường dẫn lịch sử"]
    D --> F["active = FALSE<br/>không đăng nhập được nữa"]

    D --> G["Sản phẩm: dùng ô<br/>'Đang kinh doanh' thay vì xoá"]
    D --> H["Các trường hợp khác:<br/>giữ nguyên, không dùng nữa"]
```

**Nguyên tắc:** dữ liệu lịch sử không được xoá, vì mọi báo cáo đều dựa vào nó. Bảng `stock_transactions` có `FOREIGN KEY (user_id)` không kèm `ON DELETE`, nên MySQL sẽ **từ chối** xoá một tài khoản đã từng phát sinh giao dịch — đây là ràng buộc bảo vệ dữ liệu ở tầng database, không phải giới hạn của giao diện.

**Hai chốt an toàn không thể vô hiệu hoá:**

- Không xoá/khoá được **tài khoản đang đăng nhập**.
- Phải còn **ít nhất 1 ADMIN đang hoạt động** — tránh tự khoá quyền quản trị khỏi hệ thống.

---

## 9. Bản đồ tổng thể các luồng

```mermaid
flowchart TD
    A([Đăng nhập]) --> B{Loại nghiệp vụ}

    B -->|Mua hàng| C["PO: Nháp → Đã đặt"]
    C --> C1["Nhận hàng"]
    C1 --> D["Phiếu nhập"]
    D --> E["Tồn tăng"]

    B -->|Bán hàng| F["Phiếu xuất"]
    F --> G["Hoá đơn"]
    G --> H["Thu tiền"]
    H --> I["Tồn giảm<br/>Công nợ giảm"]

    E --> J["Báo cáo tồn kho"]
    I --> J
    I --> K["Báo cáo doanh thu"]
    I --> L["Công nợ khách hàng"]
    E --> M["Top sản phẩm"]

    J --> N["Xuất Excel"]
    K --> N
    L --> N
    M --> N
```

---

## Xem thêm

| Tài liệu | Nội dung |
|----------|----------|
| [`HUONG-DAN-SU-DUNG.md`](HUONG-DAN-SU-DUNG.md) | Hướng dẫn thao tác từng màn hình cho người dùng cuối |
| [`HUONG-DAN-CAI-DAT.md`](HUONG-DAN-CAI-DAT.md) | Cài đặt, cấu hình database, khắc phục sự cố |
| [`../README.md`](../README.md) | Kiến trúc, schema, quy tắc nghiệp vụ |

---

## Về thuật ngữ trong sơ đồ

| Từ trong sơ đồ | Nghĩa là gì |
|----------------|-------------|
| `IMPORT` / `EXPORT` | Loại giao dịch: nhập kho / xuất kho |
| `COMMIT` / `ROLLBACK` | Ghi nhận / hoàn tác thay đổi trong database |
| `ISSUED` / `PARTIAL` / `PAID` / `CANCELLED` | Trạng thái hoá đơn: đã phát hành / trả một phần / đã thanh toán / đã huỷ |
| `JPA transaction` | Đơn vị xử lý giao dịch database — mọi thao tác ghi đều nằm trong một transaction, lỗi thì tự động rollback |
| `COGS` | Giá vốn hàng bán (Cost of Goods Sold) |
| `Margin %` | Biên lợi nhuận = Lợi nhuận ÷ Doanh thu thuần × 100 |