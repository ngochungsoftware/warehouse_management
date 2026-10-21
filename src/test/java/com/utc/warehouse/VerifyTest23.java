package com.utc.warehouse;

import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.util.AppPaths;
import com.utc.warehouse.util.CsvExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.nio.file.Path;

/** VerifyTest23 — AppPaths.getExportDir() phai tra ve path moi. */
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

        // Viet file that vao day, duong dan phai dung
        DefaultTableModel m = new DefaultTableModel(new String[]{"Ten", "Gia"}, 0);
        m.addRow(new Object[]{"Verify", 1000});
        File written = CsvExporter.writeTable(m, new File(actual.toFile(), "verify_test23.csv"))
                ? new File(actual.toFile(), "verify_test23.csv") : null;
        boolean okWrite = written != null && written.exists() && written.length() > 0;
        if (okWrite) pass++; else { fail++; System.out.println("FAIL: khong viet duoc file"); }

        if (okWrite) {
            System.out.println("File ghi vao: " + written.getAbsolutePath() + " (" + written.length() + " bytes)");
            System.out.println("Noi dung: " + java.nio.file.Files.readString(written.toPath()));
            written.delete();
        }
        System.out.println("=== KET QUA: " + pass + " passed, " + fail + " failed ===");
        if (fail > 0) System.exit(1);
    }
}