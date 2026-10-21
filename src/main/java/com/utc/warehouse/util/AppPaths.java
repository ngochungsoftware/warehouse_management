package com.utc.warehouse.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppPaths {

    private static final String ENV_HOME = "WAREHOUSE_HOME";
    private static final String PROP_HOME = "warehouse.home";

    private static final String EXPORT_DIR = "exports";
    private static final String IMAGE_DIR = "product_images";
    private static final String BACKUP_DIR = "backups";

    private static Path appHome;

    private AppPaths() { }

    public static Path getAppHome() {
        Path cached = appHome;
        if (cached != null) return cached;

        String homeStr = firstNonNull(System.getenv(ENV_HOME), System.getProperty(PROP_HOME));
        Path resolved = (homeStr != null) ? Paths.get(homeStr) : null;
        if (resolved == null) resolved = detectAppDir();
        if (resolved == null) resolved = Paths.get(System.getProperty("user.home"), ".warehouse_management");

        Path p = resolved.toAbsolutePath().normalize();
        createDirs(p);
        appHome = p;
        return p;
    }


    private static Path detectAppDir() {
        String wd = System.getProperty("user.dir");
        if (wd != null && !wd.isBlank()) {
            Path cur = Paths.get(wd).toAbsolutePath().normalize();
            for (int i = 0; i < 5 && cur != null; i++, cur = cur.getParent()) {
                if (Files.exists(cur.resolve("pom.xml"))
                        || Files.exists(cur.resolve("build.gradle"))
                        || Files.exists(cur.resolve("build.gradle.kts"))
                        || Files.exists(cur.resolve(".git"))) {
                    return cur;
                }
            }
        }

        try {
            Path jar = Paths.get(AppPaths.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(jar)) {
                Path parent = jar.getParent();
                if (parent != null) return parent.normalize();
            } else if (Files.isDirectory(jar)) {
                return jar.normalize();
            }
        } catch (Exception ignored) {
        }
        return null;
    }


    public static Path getExportDir() {
        return resolveSubDir(EXPORT_DIR, "warehouse.exports");
    }

    public static Path getProductImageDir() {
        return resolveSubDir(IMAGE_DIR, "warehouse.images");
    }

    public static Path getBackupDir() {
        return resolveSubDir(BACKUP_DIR, "warehouse.backups");
    }

    private static Path resolveSubDir(String dirName, String propKey) {
        String override = firstNonNull(System.getenv(toEnvKey(propKey)), System.getProperty(propKey));
        Path p = (override != null)
                ? Paths.get(override)
                : getAppHome().resolve(dirName);
        p = p.toAbsolutePath().normalize();
        createDirs(p);
        return p;
    }

    private static String toEnvKey(String propKey) {
        return propKey.toUpperCase(java.util.Locale.ROOT).replace('.', '_');
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static void createDirs(Path p) {
        try {
            Files.createDirectories(p);
        } catch (Exception ignored) {
        }
    }
}