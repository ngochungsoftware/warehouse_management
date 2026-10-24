# Hướng dẫn sử dụng phần mềm Quản lý Kho

Tài liệu dành cho **người dùng cuối** — người dùng phần mềm hằng ngày để nhập hàng, xuất hàng, xuất hoá đơn và xem báo cáo.

Tài liệu kỹ thuật (cài đặt, database, schema) nằm ở [`README.md`](../README.md) và [`HUONG-DAN-CAI-DAT.md`](HUONG-DAN-CAI-DAT.md).
Sơ đồ các luồng nghiệp vụ nằm ở [`LUONG-HOAT-DONG.md`](LUONG-HOAT-DONG.md) — hữu ích khi bạn muốn hiểu **nguyên tắc xử lý** thay vì chỉ biết bấm nút.

---

## Mục lục

1. [Khởi động và đăng nhập](#1-khởi-động-và-đăng-nhập)
2. [Làm quen với giao diện](#2-làm-quen-với-giao-diện)
3. [Quản lý danh mục và sản phẩm](#3-quản-lý-danh-mục-và-sản-phẩm)
4. [Quản lý nhà cung cấp và khách hàng](#4-quản-lý-nhà-cung-cấp-và-khách-hàng)
5. [Nhập kho](#5-nhập-kho)
6. [Xuất kho](#6-xuất-kho)
7. [Đơn đặt hàng (PO)](#7-đơn-đặt-hàng-po)
8. [Hoá đơn bán hàng và thu tiền](#8-hoá-đơn-bán-hàng-và-thu-tiền)
9. [Báo cáo và thống kê](#9-báo-cáo-và-thống-kê)
10. [Quản lý tài khoản (chỉ Quản trị viên)](#10-quản-lý-tài-khoản-chỉ-quản-trị-viên)
11. [Xuất dữ liệu ra Excel](#11-xuất-dữ-liệu-ra-excel)
12. [Phím tắt](#12-phím-tắt)
13. [Câu hỏi thường gặp](#13-câu-hỏi-thường-gặp)

---

## 1. Khởi động và đăng nhập

### Mở phần mềm

Chạy file `warehouse.jar` bằng lệnh `java -jar warehouse.jar`.

Màn hình đăng nhập hiện ra với hai ô: **Tên đăng nhập** và **Mật khẩu**.

> **Lưu ý:** Màn hình đăng nhập **không** hiển thị tài khoản mặc định, vì nếu hiển thị thì bất kỳ ai mở phần mềm cũng đăng nhập được vào tài khoản quản trị. Nếu quên mật khẩu, liên hệ quản trị viên để đặt lại.

### Đăng nhập

| Bước | Thao tác |
|------|----------|
| 1 | Nhập **Tên đăng nhập** |
| 2 | Nhập **Mật khẩu** |
| 3 | Bấm **Đăng nhập** (hoặc nhấn `Enter`) |

Tài khoản có sẵn khi cài mới:

| Tên đăng nhập | Mật khẩu | Vai trò |
|---------------|----------|---------|
| `admin` | `admin123` | Quản trị viên (ADMIN) |
| `staff` | `staff123` | Nhân viên (STAFF) |

> ⚠️ **Đổi mật khẩu ngay sau lần đăng nhập đầu tiên.** Đây là thông tin công khai trong mã nguồn, bất kỳ ai cũng đoán được. Xem [mục 10](#10-quản-lý-tài-khoản-chỉ-quản-trị-viên) để đổi.

### Đăng xuất

- Bấm nút **Đăng xuất** ở góc dưới bên trái, hoặc
- Nhấn `Ctrl+L`

Sau khi đăng xuất bạn trở lại màn hình đăng nhập.

### Thoát phần mềm

- Bấm nút **X** trên cửa sổ, hoặc
- Nhấn `Ctrl+Q`

> Luôn **thoát bằng nút X hoặc `Ctrl+Q`** thay vì đóng cửa sổ bằng tổ hợp khác. Cách này giúp phần mềm lưu và đóng kết nối cơ sở dữ liệu an toàn, tránh rò rỉ dữ liệu.

---

## 2. Làm quen với giao diện

Sau khi đăng nhập, cửa sổ chính gồm 3 phần:

```
┌──────────┬──────────────────────────────────────────────┐
│          │  Thanh tìm kiếm                  Đồng hồ     │
│  MENU    ├──────────────────────────────────────────────┤
│  (trái)  │                                              │
│          │           NỘI DUNG TRANG                    │
│  - Tổng  │                                              │
│    quan  │                                              │
│  - Sản   │                                              │
│    phẩm  │                                              │
│  ...     │                                              │
│          │                                              │
│──────────┤                                              │
│ Ngô Hành │                                              │
│ [ADMIN]  │                                              │
│ Đăng xuất│                                              │
└──────────┴──────────────────────────────────────────────┘
```

- **Menu bên trái** — bấm để chuyển giữa các màn hình. Dòng đang chọn được tô đậm.
- **Thanh tìm kiếm phía trên** — dùng chung cho một số màn, nhấn `Ctrl+F` để nhảy tới ô tìm.
- **Đồng hồ góc trên phải** — hiển thị ngày giờ hiện tại.
- **Góc dưới trái** — tên người đang đăng nhập, vai trò, nút **Đăng xuất**.

### Cách điều hướng nhanh

| Cách | Mô tả |
|------|-------|
| Bấm menu trái | Chuyển trang bình thường |
| `Ctrl+1` … `Ctrl+9` | Chuyển nhanh tới các trang đầu tiên |
| Menu **Xem** trên thanh trên cùng | Liệt kê các trang kèm số thứ tự |
| `F1` | Xem danh sách phím tắt của trang đang mở |

> Các trang **Báo cáo** và **Hoá đơn** tự tải lại dữ liệu mỗi khi bạn chuyển sang, nên không cần bấm làm mới.

### Cấu trúc chung của các màn danh sách

Hầu hết các màn danh sách đều có:

- **Thanh công cụ phía trên** — nút Thêm / Sửa / Xoá / Làm mới / Xuất Excel và ô tìm kiếm
- **Bảng dữ liệu ở giữa**
- **Thanh báo số lượng bản ghi ở dưới**

Thao tác chuẩn trên mọi màn:

| Thao tác | Cách làm |
|----------|----------|
| Thêm | Chọn 1 dòng rồi `Ctrl+N`, hoặc bấm **+ Thêm** |
| Sửa | Chọn 1 dòng rồi `Ctrl+E`, hoặc **double-click** vào dòng đó |
| Xoá | Chọn 1 dòng rồi phím `Delete` |
| Làm mới | `F5` |
| Tìm kiếm | `Ctrl+F` |

---

## 3. Quản lý danh mục và sản phẩm

### Danh mục

Menu **Danh mục** — nhóm sản phẩm, ví dụ: Nước giải khát, Đồ ăn nhanh, Bánh kẹo.

Một danh mục có thể bị **xoá** chỉ khi chưa có sản phẩm nào thuộc danh mục đó. Nếu đã có sản phẩm, phần mềm sẽ báo chặn — vì xoá danh mục sẽ làm mất liên kết của các sản phẩm đó.

### Sản phẩm

Menu **Sản phẩm** — thông tin chi tiết của từng món hàng.

**Thêm sản phẩm** (`Ctrl+N`):

| Trường | Bắt buộc | Ý nghĩa |
|--------|----------|---------|
| Mã SKU | ✅ | Mã định danh duy nhất, ví dụ `NK001`. Dùng để tra cứu, **không sửa được sau khi tạo** |
| Tên sản phẩm | ✅ | Ví dụ `Nước suối Aqua 500ml` |
| Danh mục | ✅ | Chọn từ danh sách danh mục |
| Đơn vị tính | ✅ | `chai`, `hộp`, `thùng`… |
| Giá bán | ✅ | Đơn giá bán ra (đ) |
| Giá vốn | ✅ | Giá mua vào (đ), dùng để tính lợi nhuận |
| Tồn tối thiểu | | Ngưỡng cảnh báo. Tồn xuống dưới ngưỡng này sẽ hiện cảnh báo đỏ ở báo cáo tồn kho |
| Thuế suất | | Phần trăm VAT, ví dụ `10` = 10% |
| Ảnh | | Ảnh sản phẩm (JPG/PNG) |

**Về ảnh sản phẩm:**
- Ảnh được **tự động thu nhỏ** về kích thước vừa phù hợp khi bạn chọn.
- Cơ sở dữ liệu chỉ lưu **tên file**, ảnh thật nằm trong thư mục `product_images/`.
- Nếu sửa sản phẩm và **không** chọn ảnh mới, ảnh cũ được giữ nguyên.

**Xoá sản phẩm:**

Xoá sản phẩm là thao tác **không thể hoàn tác**. Phần mềm sẽ cảnh báo và **từ chối xoá** nếu sản phẩm đã từng phát sinh dữ liệu:

> *Không thể xóa sản phẩm 'Nước suối Aqua' (NK001) vì sản phẩm đã có phát sinh dữ liệu trong phiếu nhập, phiếu xuất hoặc hóa đơn. Hãy đặt trạng thái 'Ngừng kinh doanh' thay vì xóa.*

Đây là hành vi **đúng**. Dữ liệu lịch sử cần giữ nguyên để báo cáo chính xác. Thay vào đó, hãy **bỏ tích chọn "Đang kinh doanh"** — sản phẩm sẽ không hiện trong danh sách chọn khi tạo phiếu nhập/xuất, nhưng toàn bộ lịch sử vẫn giữ nguyên.

---

## 4. Quản lý nhà cung cấp và khách hàng

### Nhà cung cấp (NCC)

Menu **Nhà cung cấp** — các đối tác cung ứng hàng.

Các trường: **Tên NCC** (bắt buộc), Số điện thoại, Email, Địa chỉ.

Tên nhà cung cấp xuất hiện trong danh sách chọn ở màn **Phiếu nhập** và **Đơn đặt hàng**.

> NCC đã có đơn mua thì **không xoá được** — cũng lý do an toàn dữ liệu như sản phẩm.

### Khách hàng

Menu **Khách hàng** — các đối tác mua hàng.

Các trường: **Tên khách hàng** (bắt buộc), Số điện thoại, Email, Địa chỉ.

Tên khách hàng xuất hiện khi tạo **Hoá đơn bán hàng**.

---

## 5. Nhập kho

Menu **Phiếu nhập** — ghi nhận hàng nhập về từ nhà cung cấp.

### Các bước tạo phiếu nhập

| Bước | Thao tác |
|------|----------|
| 1 | Chọn **Nhà cung cấp** |
| 2 | Nhập **Ghi chú** (không bắt buộc) |
| 3 | Chọn **Sản phẩm** |
| 4 | Nhập **Số lượng** (mặc định 1) |
| 5 | Nhập **Đơn giá** (đ) |
| 6 | Bấm **+ Thêm dòng** hoặc `Ctrl+Enter` |
| 7 | Lặp lại bước 3–6 cho từng sản phẩm còn lại |
| 8 | Bấm **Lưu phiếu** hoặc `Ctrl+S` |

Bảng chi tiết hiển thị phía dưới: `#`, `SKU`, `Tên sản phẩm`, `SL`, `Đơn giá`, `Thành tiền`. Xoá một dòng sai bằng cách chọn dòng rồi nhấn `Delete`.

### Kết quả

- **Tồn kho của sản phẩm tăng thêm** số lượng vừa nhập.
- Phiếu được lưu vào **Lịch sử nhập / xuất** kèm người lập, thời điểm và ghi chú.
- Nếu chưa có nhà cung cấp nào trong danh sách chọn, danh sách sẽ hiện dòng mặc định *(Chọn NCC)* — hãy thêm NCC ở menu **Nhà cung cấp** trước.

> Số lượng phải là **số nguyên dương**, đơn giá phải là **số không âm**. Sai định dạng sẽ bị báo lỗi và không lưu được.

---

## 6. Xuất kho

Menu **Phiếu xuất** — ghi nhận hàng bán ra hoặc xuất nội bộ.

### Cách tạo phiếu xuất

Cách làm **giống hệt** màn Phiếu nhập: chọn **Khách hàng**, thêm từng dòng sản phẩm với số lượng và đơn giá, rồi `Ctrl+S`. Bảng chi tiết của phiếu xuất có thêm cột **Tồn** để bạn so sánh.

### Quy tắc quan trọng: không được xuất quá tồn

Ngay khi bạn bấm **+ Thêm dòng** với số lượng vượt quá tồn kho, phần mềm **báo cảnh báo và không cho thêm dòng**:

> *Không đủ tồn cho 'Nước suối Aqua'. Tồn hiện tại: 50, đã thêm vào phiếu: 20, yêu cầu thêm: 40*

Phần mềm tính cả số lượng **đã thêm vào phiếu đang soạn**, nên bạn không thể vô tình vượt tồn bằng cách thêm nhiều dòng cho cùng một sản phẩm.

**Số tồn được tính tự động** = tổng số lượng nhập − tổng số lượng xuất. Bạn không cần (và không thể) sửa tồn kho bằng tay.

### Xem tồn kho tại chỗ

Mỗi sản phẩm trong danh sách chọn hiển thị số tồn hiện tại, ví dụ `NK001 - Nước suối Aqua (tồn=50)`. Nhấn `F5` để cập nhật nếu số liệu có thay đổi từ máy khác.

---

## 7. Đơn đặt hàng (PO)

Menu **Đơn đặt hàng (PO)** — theo dõi đơn đặt hàng **mua** từ nhà cung cấp.

Đơn đặt hàng khác phiếu nhập ở chỗ: PO là **ý định đặt hàng** (đơn chưa đến), còn phiếu nhập là **hàng đã thực sự về kho**.

### Vòng đời của một đơn

```
Nháp ──[Duyệt →]──> Đã đặt ──[Nhận hàng]──> Nhận 1 phần ──> Đã nhận đủ
                                                    │
                                              [Hủy đơn] ↓
                                                 Đã hủy
```

### Các nút thao tác

| Nút | Phím tắt | Ý nghĩa |
|-----|----------|---------|
| **+ Tạo đơn** | `Ctrl+N` | Tạo đơn mới, ban đầu ở trạng thái *Nháp* |
| **Vem chi tiết** | | Xem các dòng hàng trong đơn (cũng bằng cách double-click dòng) |
| **Duyệt →** | | Chuyển từ *Nháp* sang *Đã đặt* |
| **Nhận hàng** | `Ctrl+R` | Ghi nhận hàng đã về — tự sinh phiếu nhập kho và cộng tồn |
| **Hủy đơn** | | Huỷ đơn |
| **Làm mới** | `F5` | Tải lại danh sách |
| **Xuất Excel** | `Ctrl+X` | Xuất danh sách đơn ra file `.xlsx` |

### Mẹo

Dùng bộ lọc **Trạng thái** ở thanh công cụ để chỉ xem nhóm đơn bạn cần — ví dụ chỉ xem đơn đang *Nháp* cần duyệt.

---

## 8. Hoá đơn bán hàng và thu tiền

Menu **Hoá đơn bán hàng** — lập hoá đơn cho khách hàng và ghi nhận thu tiền.

### Tạo hoá đơn

`Ctrl+N` hoặc bấm **+ Tạo hoá đơn**.

| Trường | Bắt buộc | Ý nghĩa |
|--------|----------|---------|
| Khách hàng | | Có thể để trống nếu bán lẻ — hiển thị là *(Khách lẻ)* |
| Ngày hoá đơn | ✅ | Mặc định là hôm nay, có thể sửa |

Thêm từng sản phẩm bán (lặp lại cho nhiều dòng):

| Trường | Ý nghĩa |
|--------|---------|
| Sản phẩm | Chọn từ danh sách, đơn giá và VAT tự điền theo sản phẩm |
| Số lượng | Số lượng bán |
| Đơn giá (đ) | Giá bán cho khách |
| VAT (%) | Thuế suất áp dụng cho dòng này |
| Chiết khấu (đ) | Khoản giảm tiền **tuyệt đối** của dòng này, không phải phần trăm |

Bảng chi tiết hiển thị: `#`, `SKU`, `Tên SP`, `SL`, `Đơn giá`, `VAT%`, `CK`, `Thành tiền`.

Phần mềm tự tính theo từng dòng: **Thành tiền = Số lượng × Đơn giá + VAT − Chiết khấu**, rồi cộng lại thành **Tổng phải thu**.

### Trạng thái hoá đơn

| Trạng thái | Nghĩa |
|------------|-------|
| **Đã phát hành** | Vừa tạo, chưa thu tiền hoặc thu một phần |
| **Trả một phần** | Đã thu tiền nhưng chưa đủ |
| **Đã thanh toán** | Đã thu đủ |
| **Đã huỷ** | Đã huỷ, không tính vào doanh thu |

### Xem chi tiết và thu tiền

- **Xem chi tiết** (`Ctrl+D`) hoặc **double-click** vào dòng hoá đơn.
- **Ghi nhận thu tiền** (`Ctrl+P`) — nhập số tiền thu. Phần mềm tự tính **còn nợ** và chuyển trạng thái cho khớp.
- **Huỷ hoá đơn** — dùng khi cần bỏ một hoá đơn. Hoá đơn đã huỷ không tính vào doanh thu.

> Sửa/xoá hoá đơn sau khi phát hành được hạn chế để bảo toàn số liệu tài chính. Nếu cần bỏ, hãy **huỷ** rồi lập hoá đơn mới.

---

## 9. Báo cáo và thống kê

### Báo cáo tồn kho

Menu **Báo cáo tồn kho** — tổng hợp tồn hiện tại của từng sản phẩm.

Cột quan trọng nhất là **mức cảnh báo**:
- 🟢 Bình thường — tồn ≥ tồn tối thiểu
- 🟡 **Dưới mức tối thiểu** — nên đặt đơn nhập bổ sung
- 🔴 Hết hàng — tồn = 0

Cột **Tồn tối thiểu** cho biết ngưỡng cảnh báo bạn đã đặt ở màn Sản phẩm.

Phím tắt: `Ctrl+F` tìm sản phẩm, `Ctrl+X` xuất Excel, `F5` làm mới.

### Báo cáo doanh thu

Menu **Báo cáo doanh thu** — doanh thu trong một khoảng thời gian.

**Cách 1 — chọn khoảng có sẵn:** ở ô *Khoảng*, chọn `7 ngày qua`, `30 ngày qua`, `Tháng này`, `Tháng trước` hoặc `Tự chọn`. Báo cáo tự tải ngay.

**Cách 2 — tự chọn:** chọn `Tự chọn`, rồi nhập **Từ ngày** và **Đến ngày** (định dạng `dd/MM/yyyy`), nhấn `Enter` để áp dụng.

Bảng xuất hiện doanh thu theo từng khoảng thời gian; phía dưới là dòng tổng kết: **Tổng doanh thu, Số hoá đơn, Giá trị trung bình/hoá đơn**.

`F5` làm mới, `Ctrl+E` xuất Excel.

### Top sản phẩm bán chạy

Menu **Top sản phẩm bán chạy** — xếp hạng theo số lượng bán trong khoảng thời gian. Hữu ích để biết nên tăng đơn hàng cho món nào.

### Công nợ khách hàng

Menu **Công nợ khách hàng** — theo dõi khách nào còn nợ bao nhiêu.

Cột **Còn nợ** = Tổng phải thu − Đã thu. Xem [mục 8](#8-hoá-đơn-bán-hàng-và-thu-tiền) để ghi nhận thu tiền.

---

## 10. Quản lý tài khoản (chỉ Quản trị viên)

Chỉ tài khoản **ADMIN** mới thấy 3 màn này trong menu. Nhân viên không thấy.

### 10.1. Quản lý nhân viên

Menu **Quản lý nhân viên** — cấp tài khoản cho nhân viên.

**Cấp tài khoản** (`Ctrl+N`):

| Trường | Bắt buộc | Ý nghĩa |
|--------|----------|---------|
| Tên đăng nhập | ✅ | Tên dùng để đăng nhập |
| Mật khẩu (*) | ✅ | Bạn có thể tự gõ, hoặc bấm **Sinh ngẫu nhiên** để phần mềm tạo giúp |
| Họ tên | | Họ và tên đầy đủ, hiện ở góc dưới trái khi đăng nhập |
| Quyền | | **Cố định là Nhân viên (STAFF)** — không chọn được, vì màn này chỉ dành cho nhân viên |

Mật khẩu sinh ngẫu nhiên dài **8 ký tự**, loại bỏ các ký tự dễ nhầm `0/O`, `1/l/I`. Sau khi bấm **Lưu**, phần mềm hiển thị **một lần duy nhất** thông tin đăng nhập để bạn chuyển cho nhân viên.

> ⚠️ Thông tin đăng nhập này **chỉ hiện đúng một lần**. Hãy ghi lại vào nơi an toàn trước khi đóng hộp thoại. Nếu nhân viên quên mật khẩu, quản trị viên đặt lại bằng `Ctrl+R`.

**Màn này khác màn "Quản lý người dùng" ở 2 điểm:**
- Chỉ hiển thị tài khoản vai trò `STAFF`, **không cấp được quyền ADMIN** — cần cấp quyền quản trị thì sang màn "Quản lý người dùng".
- Có nút **Sinh ngẫu nhiên** để tạo mật khẩu.

**Bộ lọc:** Tất cả / Đang hoạt động / Đã khoá. **Double-click** vào một dòng để xem chi tiết nhân viên.

### 10.2. Quản lý người dùng

Menu **Quản lý người dùng** — quản lý toàn bộ tài khoản, kể cả tài khoản quản trị.

| Phím tắt | Chức năng |
|----------|-----------|
| `Ctrl+N` | Thêm người dùng |
| `Ctrl+E` | Sửa họ tên / vai trò (không sửa tên đăng nhập) |
| `Delete` | Xoá hoặc khoá (xem bên dưới) |
| `Ctrl+R` | **Reset mật khẩu** — dùng để đổi mật khẩu `admin` lần đầu |
| `Ctrl+T` | Khoá / Mở tài khoản |
| `Ctrl+F` | Tìm theo tên đăng nhập / họ tên |
| `Ctrl+X` | Xuất Excel |

**Về thao tác xoá — phần mềm xử lý hai trường hợp:**

- **Tài khoản chưa có dữ liệu liên quan** → xoá hẳn, **không thể hoàn tác**.
- **Tài khoản đã có dữ liệu liên quan** (từng phát sinh giao dịch, hoá đơn, thanh toán) → phần mềm báo số bản ghi bị liên quan và đề nghị **khoá tài khoản** thay vì xoá.

Nếu cần xoá thật một tài khoản đã có dữ liệu, hãy **gán lại vai trò** hoặc **khoá tài khoản** — đây là cách đúng vì giữ nguyên lịch sử giao dịch.

**Hai chốt an toàn không thể vô hiệu hoá từ giao diện:**

- ❌ **Không xoá được tài khoản đang đăng nhập.**
- ❌ **Phải còn ít nhất 1 tài khoản ADMIN đang hoạt động** — tránh tình trạng tự khoá quyền quản trị khỏi hệ thống.

> **Tên đăng nhập không sửa được sau khi tạo**, vì dữ liệu lịch sử đã ghi theo tên cũ. Nếu cần đổi, hãy tạo tài khoản mới.

### 10.3. Lịch sử thao tác

Menu **Lịch sử thao tác** — nhật ký ai làm gì, lúc nào. Dùng để truy vết khi cần kiểm tra dữ liệu bị thay đổi bởi ai.

---

## 11. Xuất dữ liệu ra Excel

Hầu hết màn danh sách và báo cáo đều có nút **Xuất Excel** hoặc phím `Ctrl+X`.

File `.xlsx` được lưu vào thư mục `exports/` cạnh phần mềm, tên file dạng:

```
san_pham_2026-10-21_143052.xlsx
hoa_don_2026-10-21_143052.xlsx
don_dat_hang_2026-10-21_143052.xlsx
```

Sau khi xuất, phần mềm hiện thông báo kèm **đường dẫn đầy đủ** để bạn mở ngay.

> **Mở file Excel đang mở:** nếu bạn đang mở file cùng tên, hãy đóng file đó trước khi xuất lại, tránh lỗi không ghi được.

**Thư mục lưu file:**

| Loại file | Thư mục |
|-----------|---------|
| File Excel xuất ra | `exports/` |
| Ảnh sản phẩm | `product_images/` |
| Sao lưu database | `backups/` |

---

## 12. Phím tắt

Nhấn **`F1`** bất kỳ lúc nào trong phần mềm để xem danh sách phím tắt **của trang đang mở** cùng các phím tắt toàn cục.

### Toàn cục (áp dụng mọi màn)

| Phím | Chức năng |
|------|-----------|
| `F1` | Xem danh sách phím tắt |
| `Ctrl+1` … `Ctrl+8` | Mở nhanh: Tổng quan, Sản phẩm, Danh mục, NCC, Khách hàng, Phiếu nhập, Phiếu xuất, Lịch sử |
| `Ctrl+9` | Mở Quản lý người dùng (chỉ ADMIN) |
| `Ctrl+L` | Đăng xuất |
| `Ctrl+Q` | Thoát ứng dụng |

### Theo màn hình

Mọi màn danh sách đều có bộ phím tắt chung:

| Phím | Chức năng |
|------|-----------|
| `Ctrl+N` | Thêm mới |
| `Ctrl+E` | Sửa mục đang chọn |
| `Delete` | Xoá mục đang chọn |
| `F5` | Làm mới danh sách |
| `Ctrl+F` | Đưa con trỏ về ô tìm kiếm |
| `Ctrl+X` | Xuất Excel |

**Riêng một số màn:**

| Màn | Phím tắt riêng |
|-----|---------------|
| Phiếu nhập / Phiếu xuất | `Ctrl+Enter` thêm dòng · `Ctrl+S` lưu phiếu |
| Hoá đơn bán hàng | `Ctrl+D` xem chi tiết · `Ctrl+P` ghi nhận thu tiền |
| Đơn đặt hàng | `Ctrl+R` nhận hàng |
| Quản lý người dùng / nhân viên | `Ctrl+R` reset mật khẩu · `Ctrl+T` khoá / mở tài khoản |
| Lịch sử nhập / xuất | `Ctrl+D` hoặc `Enter` xem chi tiết phiếu |
| Báo cáo doanh thu | `Enter` áp dụng bộ lọc · `Ctrl+E` xuất Excel |

---

## 13. Câu hỏi thường gặp

### Tôi quên mật khẩu

Liên hệ quản trị viên (tài khoản ADMIN). Quản trị viên mở **Quản lý người dùng** → chọn tài khoản của bạn → `Ctrl+R` để đặt lại mật khẩu.

### Tôi không xoá được sản phẩm / nhà cung cấp

Đây là hành vi **đúng và có chủ đích**: mục đó đã có dữ liệu liên quan (phiếu nhập, phiếu xuất, hoá đơn). Xoá đi sẽ làm mất lịch sử và làm sai số liệu báo cáo.

**Cách xử lý đúng:**
- Với **sản phẩm**: bỏ tích chọn *"Đang kinh doanh"* để ngừng bán nhưng giữ lịch sử.
- Với **nhà cung cấp**: để nguyên, không sử dụng nữa.
- Với **tài khoản**: dùng chức năng **Khoá** (`Ctrl+T`).

### Tôi không xuất được hàng quá số tồn

Đây là quy tắc an toàn tồn kho. Bạn cần nhập bổ sung hàng trước (màn **Phiếu nhập**) rồi mới xuất được. Nếu bạn cho rằng số tồn hiển thị sai, nhấn `F5` để tải lại.

### Tồn kho của tôi có bị sai không?

Tồn kho được **tính tự động** từ dữ liệu phiếu nhập và phiếu xuất:

```
Tồn hiện tại = Tổng số lượng đã nhập − Tổng số lượng đã xuất
```

Mỗi lần lưu phiếu, hệ thống ghi nhận đầy đủ vào cơ sở dữ liệu, nên số tồn luôn khớp với lịch sử giao dịch. Mở **Lịch sử nhập / xuất** để đối chiếu.

### Ảnh sản phẩm lưu ở đâu?

Ảnh lưu trong thư mục `product_images/` cạnh phần mềm; cơ sở dữ liệu chỉ lưu tên file. Khi sao lưu hoặc chuyển máy, nhớ copy cả thư mục này.

### Tôi có thể đổi vị trí lưu file không?

Được. Liên hệ người quản trị kỹ thuật để đặt biến môi trường hoặc tham khảo mục *Đường dẫn lưu trữ file* trong [`README.md`](../README.md).

### Phần mềm báo "Không thể tải dữ liệu"

Có thể kết nối cơ sở dữ liệu bị gián đoạn. Thử bấm `F5`. Nếu vẫn lỗi, hãy thông báo cho quản trị viên.

---

## Xem thêm

| Tài liệu | Dành cho |
|----------|----------|
| [`LUONG-HOAT-DONG.md`](LUONG-HOAT-DONG.md) | Sơ đồ các luồng nghiệp vụ chính của ứng dụng |
| [`README.md`](../README.md) | Người đọc kỹ thuật — kiến trúc, database, schema |
| [`HUONG-DAN-CAI-DAT.md`](HUONG-DAN-CAI-DAT.md) | Người cài đặt và chạy phần mềm lần đầu |