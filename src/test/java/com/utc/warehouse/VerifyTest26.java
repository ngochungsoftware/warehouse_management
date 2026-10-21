package com.utc.warehouse;

import com.utc.warehouse.util.AppPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * VerifyTest26 — AppPaths phai "dynamic":
 *  1. Tu dong tro ve thu muc chua project (khong hardcode duong dan nguoi dung).
 *  2. Tu dong tao thu muc con neu chua co.
 *  3. Cho phep ghi de bang system property / WAREHOUSE_HOME.
 */
public class VerifyTest26 {
    public static void main(String[] args) throws Exception {
        int pass = 0, fail = 0;
        Path projectRoot = Paths.get(System.getProperty("user.dir"));

        // --- 1) Mac dinh: tro ve thu muc chua project ---
        Path home = AppPaths.getAppHome();
        System.out.println("user.dir = " + System.getProperty("user.dir"));
        System.out.println("AppHome  = " + home);
        if (home.equals(projectRoot)) pass++;
        else { fail++; System.out.println("FAIL: AppHome khong phai thu muc project"); }

        // --- 2) Cac thu muc con nam trong project va duoc tao tu dong ---
        // check() tra ve so loi; moi loi -1 diem => 6 assert trong 3 check
        fail += check(projectRoot, "exports", AppPaths.getExportDir());
        fail += check(projectRoot, "product_images", AppPaths.getProductImageDir());
        fail += check(projectRoot, "backups", AppPaths.getBackupDir());
        pass += 6;

        // --- 3) Source khong con hardcode duong dan may ---
        String src = Files.readString(
                Paths.get("src/main/java/com/utc/warehouse/util/AppPaths.java"));
        if (!src.contains("/Users/")) { pass++; System.out.println("OK: khong hardcode duong dan may"); }
        else { fail++; System.out.println("FAIL: van con hardcode duong dan may"); }

        // --- 4) Ghi de thu muc con bang system property ---
        Path custom = Files.createTempDirectory("wm_custom_exports");
        System.setProperty("warehouse.exports", custom.toString());
        Path exp = AppPaths.getExportDir();
        System.out.println("Override  = " + exp);
        if (exp.equals(custom)) pass++;
        else { fail++; System.out.println("FAIL: khong apply system property"); }

        // override 1 thu muc khong duoc lan sang thu muc khac
        Path img = AppPaths.getProductImageDir();
        if (img.equals(projectRoot.resolve("product_images"))) pass++;
        else { fail++; System.out.println("FAIL: override lan sang thu muc khac"); }

        // --- 5) Chuoi rong -> fallback ve mac dinh ---
        System.setProperty("warehouse.exports", "   ");
        Path back = AppPaths.getExportDir();
        if (back.equals(projectRoot.resolve("exports"))) pass++;
        else { fail++; System.out.println("FAIL: chuoi rong khong fallback"); }

        // --- 6) Ghi file that vao thu muc mac dinh ---
        Path e2 = AppPaths.getExportDir();
        Path probe = e2.resolve("verify_test26.csv");
        Files.writeString(probe, "\"MaSP\",\"TenSP\"\n\"P01\",\"Giay\"\n");
        if (Files.exists(probe) && Files.size(probe) > 0) {
            pass++;
            System.out.println("Ghi file OK: " + probe.toAbsolutePath()
                    + " (" + Files.size(probe) + " bytes)");
            System.out.println("Noi dung:\n" + Files.readString(probe).stripTrailing());
        } else { fail++; System.out.println("FAIL: khong ghi duoc file"); }
        Files.deleteIfExists(probe);

        // --- 7) Khong con duong dan cong trong du an (bo qua chinh file test nay) ---
        var stale = Files.walk(projectRoot.resolve("src"))
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.getFileName().toString().equals("VerifyTest26.java"))
                .filter(p -> {
                    try { return Files.readString(p).contains("/Users/lehung"); }
                    catch (Exception e) { return false; }
                }).toList();
        if (stale.isEmpty()) { pass++; System.out.println("OK: khong con file nao hardcode /Users/lehung"); }
        else {
            fail++;
            System.out.println("FAIL: con hardcode path trong: " + stale);
        }

        System.out.println("=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }

    /** @return so luoi that bai (0 = khop) */
    private static int check(Path root, String name, Path actual) {
        System.out.printf("%-16s -> %s%n", name, actual);
        int bad = 0;
        if (!actual.equals(root.resolve(name))) { bad++; System.out.println("FAIL: " + name + " sai path"); }
        if (!Files.isDirectory(actual)) { bad++; System.out.println("FAIL: " + name + " chua ton tai"); }
        return bad;
    }
}