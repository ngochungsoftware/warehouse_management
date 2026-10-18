package com.utc.warehouse;

import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.ImageService;
import com.utc.warehouse.service.ProductService;

import java.lang.reflect.Field;
import java.util.List;

/**
 * BizTest3: Kiểm tra xoá/edit sản phẩm có ảnh và không có ảnh,
 * đảm bảo selected() từ bảng không bị NPE.
 */
public class BizTest3 {

    public static void main(String[] args) throws Exception {
        ProductService ps = new ProductService();
        ImageService img = new ImageService();

        // 0. Có field imagePath
        Field f = Product.class.getDeclaredField("imagePath");
        System.out.println("[0] imagePath field exists, type = " + f.getType().getSimpleName());

        // Cleanup các bản ghi test cũ (nếu có) — tránh trùng SKU
        for (Product old : ps.search("TEST-")) {
            try {
                if (old.getImagePath() != null) img.delete(old.getImagePath());
                ps.delete(old.getId());
            } catch (Exception ignored) { }
        }

        // 1. Tạo sản phẩm test (không ảnh)
        Product p1 = new Product();
        p1.setSku("TEST-NOIMG");
        p1.setName("Test xoá không ảnh");
        p1.setUnit("cái");
        p1.setSalePrice(new java.math.BigDecimal("10000"));
        p1.setCostPrice(new java.math.BigDecimal("5000"));
        p1.setVatRate(new java.math.BigDecimal("10"));
        p1.setMinStock(0);
        p1.setActive(true);
        p1.setImagePath(null);
        ps.save(p1);
        System.out.println("[1] Created id=" + p1.getId() + ", imagePath=" + p1.getImagePath());

        // 2. Tạo sản phẩm test (có ảnh) — tạo ảnh 100x100 rồi lưu
        java.awt.image.BufferedImage src = new java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.File tmp = new java.io.File(System.getProperty("user.home")
                + "/.warehouse_management/tmp_test2.png");
        tmp.getParentFile().mkdirs();
        javax.imageio.ImageIO.write(src, "png", tmp);
        String filename = img.save(tmp);

        Product p2 = new Product();
        p2.setSku("TEST-WITHIMG");
        p2.setName("Test xoá có ảnh");
        p2.setUnit("cái");
        p2.setSalePrice(new java.math.BigDecimal("20000"));
        p2.setCostPrice(new java.math.BigDecimal("10000"));
        p2.setVatRate(new java.math.BigDecimal("10"));
        p2.setMinStock(0);
        p2.setActive(true);
        p2.setImagePath(filename);
        ps.save(p2);
        System.out.println("[2] Created id=" + p2.getId() + ", imagePath=" + filename);
        // Capture filename trước khi session đóng (entity có thể bị detach)
        final String p2ImgOld = filename;
        java.nio.file.Path savedPath = img.resolve(filename);
        System.out.println("[2] Image file exists: " + (savedPath != null && java.nio.file.Files.exists(savedPath)));

        // 3. search() trả về cả 2 (giả lập bảng có data)
        List<Product> all = ps.search("");
        System.out.println("[3] Total products: " + all.size());

        // 4. Edit: thay đổi giá bán của p1
        p1.setSalePrice(new java.math.BigDecimal("12000"));
        ps.update(p1);
        Product reloaded = ps.search("").stream().filter(x -> x.getId().equals(p1.getId())).findFirst().orElseThrow();
        System.out.println("[4] Edit OK, new sale = " + reloaded.getSalePrice() + ", imagePath still = " + reloaded.getImagePath());

        // 5. Edit: thay đổi ảnh (giả lập user chọn ảnh mới trong dialog)
        final String oldFilename = p2ImgOld;
        String newFilename = img.save(tmp);
        p2.setImagePath(newFilename);
        ps.update(p2);
        Product reloaded2 = ps.search("").stream().filter(x -> x.getId().equals(p2.getId())).findFirst().orElseThrow();
        System.out.println("[5] Edit image OK, new imagePath = " + reloaded2.getImagePath());
        System.out.println("[5] oldFilename = " + oldFilename + ", newFilename = " + newFilename);
        // Dialog sẽ gọi delete trên ảnh cũ sau khi save DB thành công:
        img.delete(oldFilename);
        java.nio.file.Path op = java.nio.file.Paths.get(System.getProperty("user.home"),
                ".warehouse_management", "product_images", oldFilename);
        System.out.println("[5] Old file path: " + op + ", exists: " + java.nio.file.Files.exists(op));

        // 6. Xoá p1 (không ảnh) — không được NPE
        ps.delete(p1.getId());
        Product afterDel = ps.search("").stream().filter(x -> x.getId().equals(p1.getId())).findFirst().orElse(null);
        System.out.println("[6] Delete no-image OK, p1 in list: " + (afterDel != null));

        // 7. Xoá p2 (có ảnh) — file phải được xoá
        ps.delete(p2.getId());
        Product afterDel2 = ps.search("").stream().filter(x -> x.getId().equals(p2.getId())).findFirst().orElse(null);
        System.out.println("[7] Delete with-image OK, p2 in list: " + (afterDel2 != null));
        img.delete(newFilename);
        System.out.println("[7] After delete, image file exists: " + java.nio.file.Files.exists(img.resolve(newFilename)));

        tmp.delete();

        System.out.println("=== BizTest3 ALL OK ===");
    }
}
