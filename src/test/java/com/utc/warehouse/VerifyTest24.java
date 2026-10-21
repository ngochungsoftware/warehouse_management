package com.utc.warehouse;

import com.utc.warehouse.service.ImageService;
import com.utc.warehouse.util.AppPaths;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

/** VerifyTest24 — Anh san pham luu dung thu muc moi. */
public class VerifyTest24 {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        Path expected = Path.of(System.getProperty("user.dir"), "product_images");
        Path actual = AppPaths.getProductImageDir();
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);

        int pass = 0, fail = 0;
        if (expected.toAbsolutePath().normalize().equals(actual.toAbsolutePath().normalize())) pass++;
        else { fail++; System.out.println("FAIL: path khong khop"); }

        if (java.nio.file.Files.exists(actual) && java.nio.file.Files.isDirectory(actual)) pass++;
        else { fail++; System.out.println("FAIL: thu muc khong ton tai"); }

        // Tao file anh tam de gui vao ImageService
        File tmp = File.createTempFile("v24_src", ".png");
        BufferedImage img = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, 300, 200);
        g.setColor(Color.BLACK);
        g.drawString("SP", 120, 110);
        g.dispose();
        ImageIO.write(img, "png", tmp);

        var svc = new ImageService();
        String filename = svc.save(tmp);
        File saved = new File(actual.toFile(), filename);
        if (saved.exists() && saved.length() > 0) {
            pass++;
            System.out.println("Anh da luu: " + saved.getAbsolutePath()
                    + " (" + saved.length() + " bytes, ten trong DB = " + filename + ")");
        } else {
            fail++;
            System.out.println("FAIL: khong tim thay file anh " + filename);
        }

        // Doc lai de chan do
        BufferedImage reread = ImageIO.read(saved);
        if (reread != null && reread.getWidth() > 0) {
            pass++;
            System.out.println("Doc lai OK: " + reread.getWidth() + "x" + reread.getHeight());
        } else {
            fail++;
            System.out.println("FAIL: doc lai that bai");
        }

        // Don dep
        svc.delete(filename);
        tmp.delete();
        if (!saved.exists()) { pass++; System.out.println("Xoa anh OK"); }
        else { fail++; System.out.println("FAIL: khong xoa duoc"); }

        System.out.println("=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }
}