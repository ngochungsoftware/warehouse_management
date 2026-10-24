package com.utc.warehouse;

import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.util.AppPaths;
import com.utc.warehouse.util.ExcelExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** VerifyTest23 — AppPaths.getExportDir() + xuất Excel (.xlsx) hợp lệ. */
public class VerifyTest23 {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        Path expected = Path.of(System.getProperty("user.dir"), "exports");
        Path actual = AppPaths.getExportDir();

        System.out.println("Expected: " + expected);
        System.out.println("Actual:   " + actual);

        int pass = 0, fail = 0;
        boolean okEq = expected.toAbsolutePath().normalize()
                .equals(actual.toAbsolutePath().normalize());
        if (okEq) pass++; else { fail++; System.out.println("FAIL: path khong khop"); }

        boolean okEx = java.nio.file.Files.exists(actual) && java.nio.file.Files.isDirectory(actual);
        if (okEx) pass++; else { fail++; System.out.println("FAIL: thu muc khong ton tai"); }

        // --- Tao bang du lieu mau co chu, so, dau phay ---
        DefaultTableModel m = new DefaultTableModel(
                new String[]{"Ten", "Gia", "So luong", "Ghi chu"}, 0);
        m.addRow(new Object[]{"Sản phẩm A, có dấu phẩy", 1000, 5, "ghi chú <&> \"test\""});
        m.addRow(new Object[]{"Hàng B", new BigDecimal("2500.75"), 12, "hàng 2"});
        m.addRow(new Object[]{"Hàng C", 0, 1, null});

        File xlsx = new File(actual.toFile(), "verify_test23.xlsx");
        boolean okWrite = ExcelExporter.writeTable(m, xlsx);
        if (okWrite) pass++; else { fail++; System.out.println("FAIL: khong ghi duoc file"); }

        if (okWrite && xlsx.exists() && xlsx.length() > 0) {
            pass++;
            System.out.println("File ghi vao: " + xlsx.getAbsolutePath() + " (" + xlsx.length() + " bytes)");

            // File .xlsx phai la ZIP hop le co dung cac part bat buoc
            try (ZipFile zip = new ZipFile(xlsx)) {
                String[] required = {
                        "[Content_Types].xml",
                        "_rels/.rels",
                        "xl/workbook.xml",
                        "xl/_rels/workbook.xml.rels",
                        "xl/styles.xml",
                        "xl/worksheets/sheet1.xml"};
                for (String part : required) {
                    ZipEntry e = zip.getEntry(part);
                    if (e != null) { pass++; }
                    else { fail++; System.out.println("FAIL: thieu part " + part); }
                }
                System.out.println("So part trong zip: " + zip.size());

                // POI dung sharedStrings cho string va <v> cho number
                ZipEntry sheet = zip.getEntry("xl/worksheets/sheet1.xml");
                String xml = new String(zip.getInputStream(sheet).readAllBytes(),
                        java.nio.charset.StandardCharsets.UTF_8);
                String shared = "";
                ZipEntry sse = zip.getEntry("xl/sharedStrings.xml");
                if (sse != null) {
                    shared = new String(zip.getInputStream(sse).readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8);
                }
                System.out.println("sheet1.xml (" + xml.length() + " bytes) sharedStrings=" + shared.length() + " bytes");

                // Chuoi co dau phay, escape XML dung: POI luu trong sharedStrings
                if (shared.contains("Sản phẩm A, có dấu phẩy")) { pass++; }
                else { fail++; System.out.println("FAIL: thieu chuoi co dau phay"); }

                // POI tu escape XML va khong ghi xml:space trong <t> mac dinh
                if (shared.contains("<t>") || shared.contains("<t xml:space=")) { pass++; }
                else { fail++; System.out.println("FAIL: shared strings khong co <t>"); }

                // Header + 3 dong du lieu = 4 <row>
                int rows = xml.split("<row ", -1).length - 1;
                if (rows == 4) { pass++; }
                else { fail++; System.out.println("FAIL: so row = " + rows + " (can 4)"); }

                // So phai ghi dang number de Excel tinh toan duoc (POI ghi 1000.0)
                if (xml.matches("(?s).*<v>1000(?:\\.0)?</v>.*")) { pass++; }
                else { fail++; System.out.println("FAIL: so nguyen khong ghi dang number"); }

                if (xml.matches("(?s).*<v>2500(?:\\.75)?</v>.*")) { pass++; }
                else { fail++; System.out.println("FAIL: so thap phan khong ghi dang number"); }

                // So 0 phai ghi duoc (khong bi bo qua nhu null)
                if (xml.matches("(?s).*<v>0(?:\\.0)?</v>.*")) { pass++; }
                else { fail++; System.out.println("FAIL: so 0 khong ghi"); }
            }
        } else {
            fail++; System.out.println("FAIL: file khong ton tai hoac rong");
        }

        if (xlsx.exists()) xlsx.delete();
        System.out.println("=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }
}