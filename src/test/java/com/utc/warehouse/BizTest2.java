package com.utc.warehouse;

import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.service.ImageService;
import com.utc.warehouse.util.AppPaths;
import com.utc.warehouse.util.CsvExporter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.metamodel.Attribute;

import java.lang.annotation.Annotation;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * BizTest: kiểm tra nghiệp vụ ImageService + CsvExporter + Product có cột image_path.
 * Chạy độc lập, không mở UI.
 */
public class BizTest2 {

    public static void main(String[] args) throws Exception {
        // 0) App paths
        System.out.println("[0] AppHome = " + AppPaths.getAppHome());

        // 1) Product có field imagePath (qua reflection)
        Field f = Product.class.getDeclaredField("imagePath");
        // Đơn giản hoá: dùng reflection đọc @Column (bất kể jakarta/javax)
        Annotation col = f.getAnnotation(loadAnnotation("jakarta.persistence.Column"));
        if (col == null) col = f.getAnnotation(loadAnnotation("javax.persistence.Column"));
        String colName = (String) col.getClass().getMethod("name").invoke(col);
        int colLen = (int) col.getClass().getMethod("length").invoke(col);
        System.out.println("[1] Product.imagePath @Column(name='" + colName + "', length=" + colLen + ")");

        // 2) Hibernate auto-create: kiểm tra entity mapping
        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("warehousePU")) {
            EntityManager em = emf.createEntityManager();
            Set<Attribute<? super Product, ?>> attrs = em.getMetamodel().entity(Product.class).getAttributes();
            boolean hasImage = attrs.stream().anyMatch(a -> a.getName().equals("imagePath"));
            System.out.println("[2] Hibernate sees imagePath = " + hasImage);
            em.close();
        }

        // 3) ImageService round-trip: tạo 1 ảnh test, lưu, đọc, xoá
        ImageService img = new ImageService();
        BufferedImage src = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 200; y++)
            for (int x = 0; x < 300; x++)
                src.setRGB(x, y, (x % 255) << 16 | (y % 255) << 8 | 128);
        File tmp = new File(AppPaths.getAppHome().toFile(), "tmp_test.png");
        javax.imageio.ImageIO.write(src, "png", tmp);
        String fn = img.save(tmp);
        System.out.println("[3a] saved filename = " + fn);
        BufferedImage back = img.load(fn);
        System.out.println("[3b] reloaded = " + (back != null ? back.getWidth() + "x" + back.getHeight() : "NULL"));
        Path resolved = img.resolve(fn);
        System.out.println("[3c] resolve()   = " + resolved);
        img.delete(fn);
        System.out.println("[3d] deleted     = " + (Files.exists(resolved) ? "STILL EXISTS" : "OK"));
        tmp.delete();

        // 4) CsvExporter ghi tạm ra file
        javax.swing.table.DefaultTableModel m = new javax.swing.table.DefaultTableModel(
                new Object[]{"Tên", "Giá"}, 0);
        m.addRow(new Object[]{"Sản phẩm A, có dấu phẩy", "1000"});
        m.addRow(new Object[]{"Hàng B", "2.500"});
        File csv = new File(AppPaths.getExportDir().toFile(), "test.csv");
        CsvExporter.writeTable(m, csv);
        System.out.println("[4] CSV file size = " + csv.length() + " bytes");
        System.out.println("[4] CSV head:");
        Files.lines(csv.toPath()).limit(3).forEach(line -> System.out.println("    " + line));

        // 5) ProductService search (cần sp có sẵn)
        ProductService ps = new ProductService();
        System.out.println("[5] products count = " + ps.search("").size());

        System.out.println("=== BizTest2 ALL OK ===");
    }

    private static Class<? extends Annotation> loadAnnotation(String fqcn) {
        try { return Class.forName(fqcn).asSubclass(Annotation.class); }
        catch (ClassNotFoundException ex) { return null; }
    }
}
