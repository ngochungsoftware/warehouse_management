package com.utc.warehouse;

import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.ui.product.ProductViewDialog;

import java.lang.reflect.Constructor;

/**
 * BizTest15: Verify ProductViewDialog + product detail flow hoạt động.
 *
 * Đây là test cho màn Sản phẩm (double-click) người dùng báo vẫn không hiện gì.
 * Chạy bằng cách khởi tạo dialog thật và đọc field 'product' sau đó.
 */
public class BizTest15 {

    public static void main(String[] args) throws Exception {
        int pass = 0, fail = 0;

        System.setProperty("java.awt.headless", "false");

        ProductService service = new ProductService();
        var products = service.search("");
        System.out.println("[1] service.search(\"\") → " + products.size() + " sản phẩm");
        if (products.isEmpty()) {
            System.out.println("Không có sản phẩm nào trong DB. Seed trước.");
            return;
        }
        pass++;

        Product p = products.get(0);
        System.out.println("[2] Sản phẩm đầu: " + p.getName() + " (id=" + p.getId() + ", sku=" + p.getSku() + ")");

        // Đọc các field qua reflection để chắc chắn entity "còn sống"
        try {
            String name = p.getName();
            String sku = p.getSku();
            java.math.BigDecimal salePrice = p.getSalePrice();
            String cat = p.getCategory() == null ? "(null)" : p.getCategory().getName();
            System.out.println("[3] Đọc tất cả field OK");
            System.out.println("    name=" + name);
            System.out.println("    sku=" + sku);
            System.out.println("    salePrice=" + salePrice);
            System.out.println("    category=" + cat);
            pass++;
        } catch (Throwable t) {
            System.out.println("[3] ✗ Lỗi đọc field: " + t);
            t.printStackTrace();
            fail++;
            return;
        }

        // Khởi tạo Dialog thật
        System.out.println("[4] Khởi tạo ProductViewDialog thật:");
        javax.swing.JFrame owner = new javax.swing.JFrame("owner");
        try {
            // Dùng reflection để gọi constructor (vì là inner class)
            Constructor<?>[] ctors = ProductViewDialog.class.getDeclaredConstructors();
            for (Constructor<?> c : ctors) {
                if (c.getParameterTypes().length == 2
                        && c.getParameterTypes()[0] == java.awt.Window.class) {
                    c.setAccessible(true);
                    ProductViewDialog dlg = (ProductViewDialog) c.newInstance(owner, p);
                    System.out.println("    ✓ Dialog khởi tạo OK (size="
                            + dlg.getWidth() + "x" + dlg.getHeight() + ")");
                    pass++;
                    dlg.dispose();
                    owner.dispose();
                    break;
                }
            }
        } catch (Throwable t) {
            System.out.println("    ✗ Lỗi tạo dialog: " + t);
            t.printStackTrace();
            fail++;
        }

        // Đặc biệt: verify product.getCategory() không LazyInit
        System.out.println("[5] Verify Product.getCategory() an toàn:");
        try {
            String cat = p.getCategory() == null ? "?" : p.getCategory().getName();
            System.out.println("    category.name = " + cat);
            pass++;
        } catch (Throwable t) {
            System.out.println("    ✗ LazyInit: " + t.getMessage());
            fail++;
        }

        System.out.println("\n=== Kết quả: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }
}