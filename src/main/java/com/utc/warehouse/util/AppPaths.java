package com.utc.warehouse.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Đường dẫn lưu trữ file ngoài DB (ảnh sản phẩm, backup DB, log, ...).
 *  - Mặc định: $HOME/.warehouse_management/
 *  - Có thể override bằng biến môi trường WAREHOUSE_HOME.
 *
 * Tạo thư mục nếu chưa tồn tại.
 */
public final class AppPaths {

    private AppPaths() { }

    public static Path getAppHome() {
        String custom = System.getenv("WAREHOUSE_HOME");
        Path p = (custom == null || custom.isBlank())
                ? Paths.get(System.getProperty("user.home"), ".warehouse_management")
                : Paths.get(custom);
        try { java.nio.file.Files.createDirectories(p); }
        catch (Exception ex) { /* ignored */ }
        return p;
    }

    public static Path getProductImageDir() {
        Path p = getAppHome().resolve("product_images");
        try { java.nio.file.Files.createDirectories(p); }
        catch (Exception ex) { /* ignored */ }
        return p;
    }

    public static Path getBackupDir() {
        Path p = getAppHome().resolve("backups");
        try { java.nio.file.Files.createDirectories(p); }
        catch (Exception ex) { /* ignored */ }
        return p;
    }

    public static Path getExportDir() {
        Path p = getAppHome().resolve("exports");
        try { java.nio.file.Files.createDirectories(p); }
        catch (Exception ex) { /* ignored */ }
        return p;
    }
}
