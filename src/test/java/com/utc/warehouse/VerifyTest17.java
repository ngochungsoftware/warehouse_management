package com.utc.warehouse;

import com.utc.warehouse.dao.ReportDAO;
import com.utc.warehouse.entity.Invoice;
import com.utc.warehouse.entity.Product;
import com.utc.warehouse.entity.StockTransactionDetail;
import com.utc.warehouse.enums.InvoiceStatus;
import com.utc.warehouse.enums.PaymentMethod;
import com.utc.warehouse.service.InvoiceService;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.service.StockService;
import com.utc.warehouse.service.UserService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * VerifyTest17 — Kiểm chứng LUỒNG VẬN HÀNH KHO THỰC TẾ end-to-end với DB thật.
 *
 * Mô phỏng 1 ngày vận hành kho:
 *   1. Nhập kho lần 1
 *   2. Nhập lần 2 giá khác  -> giá vốn bình quân phải cập nhật
 *   3. Bán hàng -> HĐ + phiếu xuất (tồn giảm, VAT, công nợ)
 *   4. Thu tiền một phần  -> PARTIAL
 *   5. Thu nốt            -> PAID
 *   6. Xuất kho trực tiếp
 *   7. Chặn xuất vượt tồn
 *   8. Báo cáo tồn khớp số liệu
 */
public class VerifyTest17 {

    private static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== LUỒNG VẬN HÀNH KHO THỰC TẾ ===\n");

        ProductService productService = new ProductService();
        StockService stockService = new StockService();
        InvoiceService invoiceService = new InvoiceService();
        UserService userService = new UserService();
        ReportDAO reportDAO = new ReportDAO();

        var users = userService.all();
        if (users.isEmpty()) {
            System.out.println("Chưa có user. Chạy sql/schema.sql trước.");
            return;
        }
        var admin = users.get(0);
        System.out.println("User: " + admin.getUsername() + " (" + admin.getRole() + ")\n");

        // ---- Chuẩn bị ----
        String sku = "ZZTEST" + (System.currentTimeMillis() % 100000);
        Product p = new Product();
        p.setSku(sku);
        p.setName("SP kiem thu van hanh kho");
        p.setUnit("cai");
        p.setSalePrice(new BigDecimal("100000"));
        p.setCostPrice(BigDecimal.ZERO);
        p.setMinStock(10);
        p.setVatRate(new BigDecimal("10"));
        p.setActive(true);
        Product created = productService.save(p);
        int pid = created.getId();
        System.out.println("Tao san pham #" + pid + " SKU=" + sku + "\n");

        /* 1. Nhập kho lần 1 */
        System.out.println("--- 1. NHAP KHO 100 cai @ 50.000 ---");
        stockService.createImport(admin, null, "Nhap dot 1", list(detail(pid, 100, "50000")));

        check("Gia von = 50.000", eq(reload(pid).getCostPrice(), "50000"),
                "cost = " + reload(pid).getCostPrice());
        check("Ton = 100", onHand(pid) == 100, "onHand = " + onHand(pid));

        /* 2. Nhập kho lần 2, giá khác */
        System.out.println("\n--- 2. NHAP KHO them 100 cai @ 70.000 ---");
        stockService.createImport(admin, null, "Nhap dot 2", list(detail(pid, 100, "70000")));

        // (50000*100 + 70000*100) / 200 = 60000
        check("Gia von binh quan = 60.000", eq(reload(pid).getCostPrice(), "60000"),
                "cost = " + reload(pid).getCostPrice());
        check("Ton = 200", onHand(pid) == 200, "onHand = " + onHand(pid));

        /* 3. Bán hàng */
        System.out.println("\n--- 3. BAN 30 cai @ 100.000, VAT 10%, giam 20.000 ---");
        var draft = new InvoiceService.InvoiceItemDraft();
        draft.productId = pid;
        draft.quantity = 30;
        draft.unitPrice = new BigDecimal("100000");
        draft.vatRate = new BigDecimal("10");
        draft.discount = new BigDecimal("20000");

        Invoice inv = invoiceService.createFromExport(admin, null, "Ban le", List.of(draft));
        check("Ton giam con 170", onHand(pid) == 170, "onHand = " + onHand(pid));

        Invoice full = invoiceService.findByCodeWithDetails(inv.getCode());
        check("HD co 1 dong hang", full.getItems().size() == 1,
                "so dong = " + full.getItems().size());

        // base 3.000.000 | vat 300.000 | total 3.280.000
        check("Subtotal = 3.000.000", eq(full.getSubtotal(), "3000000"), "= " + full.getSubtotal());
        check("VAT = 300.000", eq(full.getVatAmount(), "300000"), "= " + full.getVatAmount());
        check("Discount = 20.000", eq(full.getDiscount(), "20000"), "= " + full.getDiscount());
        check("Total = 3.280.000", eq(full.getTotal(), "3280000"), "= " + full.getTotal());
        check("Chua thu: paid = 0", full.getPaid().signum() == 0, "= " + full.getPaid());
        check("Cong no = 3.280.000", eq(full.getDebt(), "3280000"), "= " + full.getDebt());
        check("Cost snapshot = 60.000",
                eq(full.getItems().get(0).getCostPriceSnapshot(), "60000"),
                "= " + full.getItems().get(0).getCostPriceSnapshot());

        /* 4. Thu một phần */
        System.out.println("\n--- 4. Thu 1.000.000 ---");
        invoiceService.createReceipt(admin, full.getId(), new BigDecimal("1000000"),
                PaymentMethod.CASH, "Thu 1 phan");
        Invoice a1 = invoiceService.findByCodeWithDetails(inv.getCode());
        check("paid = 1.000.000", eq(a1.getPaid(), "1000000"), "= " + a1.getPaid());
        check("debt = 2.280.000", eq(a1.getDebt(), "2280000"), "= " + a1.getDebt());
        check("Trang thai = PARTIAL", a1.getStatus() == InvoiceStatus.PARTIAL, "= " + a1.getStatus());

        /* 5. Thu nốt */
        System.out.println("\n--- 5. Thu not 2.280.000 ---");
        invoiceService.createReceipt(admin, a1.getId(), new BigDecimal("2280000"),
                PaymentMethod.BANK, "Thu not");
        Invoice a2 = invoiceService.findByCodeWithDetails(inv.getCode());
        check("debt = 0", a2.getDebt().signum() == 0, "= " + a2.getDebt());
        check("Trang thai = PAID", a2.getStatus() == InvoiceStatus.PAID, "= " + a2.getStatus());

        /* 6. Xuất kho trực tiếp */
        System.out.println("\n--- 6. XUAT KHO truc tiep 50 cai ---");
        stockService.createExport(admin, null, "Xuat du an", list(detail(pid, 50, "0")));
        check("Ton = 120", onHand(pid) == 120, "onHand = " + onHand(pid));

        /* 7. Chặn xuất vượt tồn */
        System.out.println("\n--- 7. Thu xuat 9999 cai (vuot ton) ---");
        boolean blocked = false;
        String err = "";
        try {
            stockService.createExport(admin, null, "Xuat qua ton",
                    list(detail(pid, 9999, "0")));
        } catch (IllegalStateException ex) {
            blocked = true;
            err = ex.getMessage();
        }
        check("Chan xuat vuot ton", blocked, blocked ? "da chan: " + err : "KHONG CHAN — loi nghiem trong!");
        check("Ton khong doi = 120", onHand(pid) == 120, "onHand = " + onHand(pid));

        /* 8. Báo cáo tồn khớp */
        System.out.println("\n--- 8. Bao cao ton kho ---");
        var row = reportDAO.getInventory().stream()
                .filter(r -> r.getProductId() == pid).findFirst().orElse(null);
        check("Bao cao ton = 120", row != null && row.getOnHand() == 120,
                row == null ? "khong tim thay SP" : "onHand = " + row.getOnHand());

        System.out.println("\n=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }

    /* ================= helpers ================= */

    private static List<StockTransactionDetail> list(StockTransactionDetail d) {
        var l = new ArrayList<StockTransactionDetail>();
        l.add(d);
        return l;
    }

    private static StockTransactionDetail detail(int pid, int qty, String price) {
        var d = new StockTransactionDetail();
        Product p = new Product();
        p.setId(pid);
        d.setProduct(p);
        d.setQuantity(qty);
        d.setUnitPrice(new BigDecimal(price));
        return d;
    }

    private static int onHand(int pid) { return new ReportDAO().getOnHand(pid); }

    private static Product reload(int pid) {
        for (Product p : new ProductService().search("")) {
            if (p.getId() == pid) return p;
        }
        return null;
    }

    private static boolean eq(BigDecimal actual, String expected) {
        return actual != null && actual.compareTo(new BigDecimal(expected)) == 0;
    }

    private static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "    ✓ " : "    ✗ ") + name + " — " + detail);
        if (ok) pass++; else fail++;
    }
}