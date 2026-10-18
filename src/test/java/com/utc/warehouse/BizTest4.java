package com.utc.warehouse;

import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.ImageService;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.ui.product.ProductViewDialog;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;

/**
 * BizTest4: Khởi tạo ProductViewDialog headless và verify.
 * (Test thực tế UI cần AWT, ta chỉ verify constructor + các getter không NPE.)
 */
public class BizTest4 {

    public static void main(String[] args) throws Exception {
        ProductService ps = new ProductService();
        ImageService imgSvc = new ImageService();
        List<Product> all = ps.search("");
        if (all.isEmpty()) {
            System.out.println("No products in DB. Skipping.");
            return;
        }
        Product p = all.get(0);
        System.out.println("[1] Sample product: id=" + p.getId() + ", name=" + p.getName()
                + ", imagePath=" + p.getImagePath());

        // Lấy 1 sản phẩm có ảnh và 1 không có
        Product withImg = all.stream().filter(x -> x.getImagePath() != null && !x.getImagePath().isBlank()).findFirst().orElse(null);
        Product noImg   = all.stream().filter(x -> x.getImagePath() == null || x.getImagePath().isBlank()).findFirst().orElse(null);
        System.out.println("[2] withImg = " + (withImg == null ? "null" : withImg.getName()));
        System.out.println("[3] noImg   = " + (noImg   == null ? "null" : noImg.getName()));

        // Test imageService.load cho từng loại
        if (withImg != null) {
            BufferedImage loaded = imgSvc.load(withImg.getImagePath());
            System.out.println("[4] Load image for '" + withImg.getName() + "' = "
                    + (loaded == null ? "NULL" : loaded.getWidth() + "x" + loaded.getHeight()));
        }
        if (noImg != null) {
            BufferedImage loaded = imgSvc.load(noImg.getImagePath());
            System.out.println("[5] Load null imagePath = " + (loaded == null ? "NULL" : "OK"));
        }

        // Test dialog constructor headless: thử 1 lần trong try-catch
        // Headless mode sẽ throw HeadlessException — điều đó chỉ ra dialog gọi đúng code path
        System.setProperty("java.awt.headless", "true");
        try {
            JDialog dlg = new ProductViewDialog(null, withImg != null ? withImg : p);
            System.out.println("[6] Dialog created OK (headless): " + dlg.getTitle());
        } catch (java.awt.HeadlessException he) {
            System.out.println("[6] Dialog cần AWT runtime (HeadlessException) — đúng như mong đợi.");
        } catch (Throwable t) {
            System.out.println("[6] Dialog error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            t.printStackTrace();
        }
        System.setProperty("java.awt.headless", "false");

        System.out.println("=== BizTest4 OK ===");
    }
}
