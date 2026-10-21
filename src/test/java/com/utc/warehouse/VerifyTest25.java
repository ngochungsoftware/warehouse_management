package com.utc.warehouse;

import com.utc.warehouse.util.AppPaths;

import java.nio.file.Path;

/** VerifyTest25 — Thu muc backups dung path moi. */
public class VerifyTest25 {
    public static void main(String[] args) throws Exception {
        javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());

        Path expected = Path.of(System.getProperty("user.dir"), "backups");
        Path actual = AppPaths.getBackupDir();
        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);

        int pass = 0, fail = 0;
        if (expected.toAbsolutePath().normalize().equals(actual.toAbsolutePath().normalize())) pass++;
        else { fail++; System.out.println("FAIL: path khong khop"); }

        if (java.nio.file.Files.exists(actual) && java.nio.file.Files.isDirectory(actual)) pass++;
        else { fail++; System.out.println("FAIL: thu muc khong ton tai"); }

        // Viet 1 file DB gia vao day de chan do: sqlite test
        java.io.File probe = new java.io.File(actual.toFile(), "warehouse.db");
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(probe)) {
            fos.write("SQLite-format-fake".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        if (probe.exists() && probe.length() > 0) { pass++; System.out.println("Ghi file OK: " + probe); }
        else { fail++; System.out.println("FAIL: khong ghi duoc"); }

        if (probe.delete()) { pass++; System.out.println("Xoa file OK"); }
        else { fail++; System.out.println("FAIL: khong xoa duoc"); }

        // Dam bao cac path cu van `trong` (moi lan thay doi se tao thu muc ~/.warehouse_management vi getAppHome)
        Path legacyImage = java.nio.file.Path.of(System.getProperty("user.home"), ".warehouse_management", "product_images");
        Path legacyBackup = java.nio.file.Path.of(System.getProperty("user.home"), ".warehouse_management", "backups");
        System.out.println("Legacy image dir exists?  " + java.nio.file.Files.exists(legacyImage));
        System.out.println("Legacy backup dir exists? " + java.nio.file.Files.exists(legacyBackup));

        System.out.println("=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }
}