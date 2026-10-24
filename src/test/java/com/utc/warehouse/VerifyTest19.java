package com.utc.warehouse;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.service.StockService;
import com.utc.warehouse.service.UserService;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * VerifyTest19 — Rủi ro vận hành thực tế: XOÁ sản phẩm đã phát sinh lịch sử kho.
 *
 * Kịch bản nguy hiểm trong thực tế:
 *   - Sản phẩm đã nhập/xuất, đã có trong phiếu và hóa đơn.
 *   - Người dùng bấm "Xoá" -> mất lịch sử? lỗi FK? tồn kho sai?
 *   - Với sản phẩm đang có tồn > 0 mà xóa đi -> số liệu tồn báo cáo sai.
 */
public class VerifyTest19 {

    private static int pass = 0, fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== RUI RO: XOA SAN PHAM DA CO LICH SU KHO ===\n");

        ProductService productService = new ProductService();
        StockService stockService = new StockService();
        UserService userService = new UserService();
        var admin = userService.all().get(0);

        // Tạo SP + nhập kho để có lịch sử
        Product p = new Product();
        p.setSku("DEL" + (System.currentTimeMillis() % 100000));
        p.setName("SP de xoa");
        p.setUnit("cai");
        p.setSalePrice(new BigDecimal("10000"));
        p.setCostPrice(BigDecimal.ZERO);
        p.setMinStock(0);
        p.setVatRate(BigDecimal.ZERO);
        p.setActive(true);
        int pid = productService.save(p).getId();

        var d = new com.utc.warehouse.entity.StockTransactionDetail();
        Product ref = new Product(); ref.setId(pid);
        d.setProduct(ref);
        d.setQuantity(50);
        d.setUnitPrice(new BigDecimal("8000"));
        var list = new ArrayList<com.utc.warehouse.entity.StockTransactionDetail>();
        list.add(d);
        stockService.createImport(admin, null, "nhap de test xoa", list);

        System.out.println("SP #" + pid + " da co lich su nhap 50 cai, ton = 50\n");

        /* 1. Thử xoá sản phẩm đã có lịch sử */
        System.out.println("--- 1. Thu XOA san pham da co phieu ---");
        String result;
        boolean crashed = false;
        try {
            productService.delete(pid);
            result = "xoa thanh cong";
        } catch (RuntimeException ex) {
            crashed = true;
            result = ex.getClass().getSimpleName() + ": " + shorten(ex.getMessage());
        }
        System.out.println("    Ket qua: " + result);

        boolean deleted = !exists(pid);
        if (deleted) {
            System.out.println("    [!] San pham DA BI XOA khoi DB");
        }

        /* 2. Tồn kho còn đúng không? */
        System.out.println("\n--- 2. Ton kho sau khi xoa ---");
        int onHand = onHand(pid);
        System.out.println("    onHand(ID=" + pid + ") = " + onHand);

        /* 3. Lich su phieu con lai? */
        System.out.println("\n--- 3. Lich su phieu con lai? ---");
        long detailCount = countDetails(pid);
        System.out.println("    So dong stock_transaction_details con tro toi SP = " + detailCount);

        /* Assert: DB phai BAO VEE du lieu — khong duoc xoa san pham dang co lich su kho. */
        check("Khong xoa san pham dang co lich su kho", !deleted,
                deleted ? "san pham bi xoa — mat lich su" : "FK chan xoa, du lieu duoc giu");
        check("Ton kho duoc giu nguyen", onHand == 50, "onHand = " + onHand + " (mong doi 50)");
        check("Lich su phieu duoc giu nguyen", detailCount > 0, "so dong con lai = " + detailCount);

        System.out.println("\n=== KET LUAN ===");
        if (crashed) {
            System.out.println("    XOA BI LOI (nhin chung la an toan - du lieu duoc giu):");
            System.out.println("    → UI se nem exception. Can bat loi va thong bao ro cho nguoi dung.");
        } else if (deleted) {
            System.out.println("    XOA THANH CONG — nguy hiem voi du lieu kho:");
            System.out.println("    → Lich su phiếu/HĐ có thể bi mat hoac ton sai.");
            System.out.println("    → KHUYEN NGHI: nen chan xoa san pham, chi dung 'Ngung kinh doanh'.");
        }

        System.out.println("\n=== KET QUA: " + pass + " passed, " + fail + " failed ===");
    }

    private static boolean exists(int pid) {
        for (Product p : new ProductService().search("")) if (p.getId() == pid) return true;
        return false;
    }

    private static int onHand(int pid) {
        return new com.utc.warehouse.dao.ReportDAO().getOnHand(pid);
    }

    private static long countDetails(int pid) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(d) FROM StockTransactionDetail d WHERE d.product.id = :pid", Long.class)
                    .setParameter("pid", pid).getSingleResult();
        } finally { em.close(); }
    }

    private static String shorten(String s) {
        if (s == null) return "null";
        return s.length() > 160 ? s.substring(0, 160) + "..." : s;
    }

    private static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "    ✓ " : "    ✗ ") + name + " — " + detail);
        if (ok) pass++; else fail++;
    }
}