package com.utc.warehouse;

import com.utc.warehouse.dao.ReportDAO;
import com.utc.warehouse.entity.Product;
import com.utc.warehouse.entity.PurchaseOrder;
import com.utc.warehouse.entity.PurchaseOrderItem;
import com.utc.warehouse.enums.POStatus;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.service.PurchaseOrderService;
import com.utc.warehouse.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * VerifyTest18 — Kiểm chứng luồng ĐẶT HÀNG (PO) như thực tế vận hành kho.
 *
 *   1. Tạo đơn DRAFT
 *   2. Duyệt -> ORDERED
 *   3. Nhận hàng -> tạo phiếu nhập, cập nhật tồn + giá vốn
 *   4. Chặn duyệt lần 2 (đã ORDERED)
 *   5. Chặn huỷ đơn đã nhận (RECEIVED)
 *   6. Chặn nhận hàng khi chưa duyệt (DRAFT)
 *   7. Huỷ đơn DRAFT được phép
 */
public class VerifyTest18 {

    private static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== LUONG DAT HANG (PO) ===\n");

        ProductService productService = new ProductService();
        PurchaseOrderService poService = new PurchaseOrderService();
        UserService userService = new UserService();
        ReportDAO reportDAO = new ReportDAO();

        var admin = userService.all().get(0);

        // Chuẩn bị sản phẩm
        Product p = new Product();
        p.setSku("PO" + (System.currentTimeMillis() % 100000));
        p.setName("SP test PO");
        p.setUnit("cai");
        p.setSalePrice(new BigDecimal("50000"));
        p.setCostPrice(BigDecimal.ZERO);
        p.setMinStock(0);
        p.setVatRate(BigDecimal.ZERO);
        p.setActive(true);
        int pid = productService.save(p).getId();
        System.out.println("SP #" + pid + " | ton ban dau = " + onHand(pid) + "\n");

        var item = new PurchaseOrderItem();
        item.setProduct(productService.search("").stream()
                .filter(x -> x.getId() == pid).findFirst().orElseThrow());
        item.setOrderedQty(80);
        item.setReceivedQty(0);
        item.setUnitPrice(new BigDecimal("30000"));

        var items = new ArrayList<PurchaseOrderItem>();
        items.add(item);

        /* 1. Tạo đơn */
        System.out.println("--- 1. Tao don DRAFT ---");
        PurchaseOrder po = poService.create(admin, null, "Don test", LocalDate.now().plusDays(7), items);
        check("Tao duoc don DRAFT", po.getStatus() == POStatus.DRAFT, "status = " + po.getStatus());
        check("Ton chua doi = 0", onHand(pid) == 0, "onHand = " + onHand(pid));

        /* 2. Duyệt */
        System.out.println("\n--- 2. Duyet don ---");
        po = poService.approve(admin, po.getId());
        check("Chuyen sang ORDERED", po.getStatus() == POStatus.ORDERED, "status = " + po.getStatus());

        /* 3. Nhận hàng */
        System.out.println("\n--- 3. Nhan hang ---");
        po = poService.receive(admin, po.getId(), true);
        check("Chuyen sang RECEIVED", po.getStatus() == POStatus.RECEIVED, "status = " + po.getStatus());
        check("Ton = 80", onHand(pid) == 80, "onHand = " + onHand(pid));

        Product after = null;
        for (Product x : productService.search("")) if (x.getId() == pid) after = x;
        check("Gia von = 30.000", after != null
                        && after.getCostPrice().compareTo(new BigDecimal("30000")) == 0,
                "cost = " + (after == null ? "?" : after.getCostPrice()));

        /* 4. Chặn duyệt lần 2 */
        System.out.println("\n--- 4. Thu duyet lai (da RECEIVED) ---");
        boolean blocked = false; String msg = "";
        try { poService.approve(admin, po.getId()); }
        catch (IllegalStateException ex) { blocked = true; msg = ex.getMessage(); }
        check("Chan duyet don da ORDERED/RECEIVED", blocked,
                blocked ? msg : "KHONG CHAN — loi!");

        /* 5. Chặn huỷ đơn đã nhận */
        System.out.println("\n--- 5. Thu huy don da RECEIVED ---");
        blocked = false; msg = "";
        try { poService.cancel(admin, po.getId(), "test"); }
        catch (IllegalStateException ex) { blocked = true; msg = ex.getMessage(); }
        check("Chan huy don da nhan", blocked, blocked ? msg : "KHONG CHAN — loi!");
        check("Ton khong doi = 80", onHand(pid) == 80, "onHand = " + onHand(pid));

        /* 6. Chặn nhận khi chưa duyệt */
        System.out.println("\n--- 6. Nhan don DRAFT (chua duyet) ---");
        var item2 = new PurchaseOrderItem();
        item2.setProduct(after);
        item2.setOrderedQty(10);
        item2.setReceivedQty(0);
        item2.setUnitPrice(new BigDecimal("25000"));
        var items2 = new ArrayList<PurchaseOrderItem>();
        items2.add(item2);
        PurchaseOrder po2 = poService.create(admin, null, "Don chua duyet", LocalDate.now(), items2);

        blocked = false; msg = "";
        try { poService.receive(admin, po2.getId(), true); }
        catch (IllegalStateException ex) { blocked = true; msg = ex.getMessage(); }
        check("Chan nhan hang don DRAFT", blocked, blocked ? msg : "KHONG CHAN — loi!");

        /* 7. Huỷ đơn DRAFT — phải được phép */
        System.out.println("\n--- 7. Huy don DRAFT (hop le) ---");
        boolean ok = true; msg = "";
        try { poService.cancel(admin, po2.getId(), "huy vi sai"); }
        catch (RuntimeException ex) { ok = false; msg = ex.getMessage(); }
        check("Huy don DRAFT duoc phep", ok, ok ? "OK" : msg);

        System.out.println("\n=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }

    private static int onHand(int pid) { return new ReportDAO().getOnHand(pid); }

    private static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "    ✓ " : "    ✗ ") + name + " — " + detail);
        if (ok) pass++; else fail++;
    }
}