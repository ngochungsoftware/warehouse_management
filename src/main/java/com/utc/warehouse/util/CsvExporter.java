package com.utc.warehouse.util;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Xuất JTable ra file CSV. Mở file sau khi ghi xong nếu có Desktop support.
 */
public final class CsvExporter {

    private CsvExporter() { }

    /** Xuất bảng ra CSV. Trả về file đã ghi. */
    public static File exportTable(JTable table, String defaultName) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(AppPaths.getExportDir().toFile(),
                defaultName + "_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv"));
        if (fc.showSaveDialog(table) != JFileChooser.APPROVE_OPTION) return null;
        File out = fc.getSelectedFile();
        return writeTable(table.getModel(), out) ? out : null;
    }

    /** Ghi TableModel ra file CSV. Trả về true nếu thành công. */
    public static boolean writeTable(TableModel m, File out) {
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8))) {
            pw.println('\ufeff'); // BOM cho Excel nhận UTF-8
            // Header
            for (int c = 0; c < m.getColumnCount(); c++) {
                pw.print(quote(m.getColumnName(c)));
                pw.print(c == m.getColumnCount() - 1 ? '\n' : ',');
            }
            // Rows
            for (int r = 0; r < m.getRowCount(); r++) {
                for (int c = 0; c < m.getColumnCount(); c++) {
                    Object v = m.getValueAt(r, c);
                    String s = v == null ? "" : v.toString();
                    pw.print(quote(s));
                    pw.print(c == m.getColumnCount() - 1 ? '\n' : ',');
                }
            }
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Lỗi xuất CSV: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static String quote(String s) {
        if (s == null) return "\"\"";
        // Escape " bằng cách nhân đôi
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
