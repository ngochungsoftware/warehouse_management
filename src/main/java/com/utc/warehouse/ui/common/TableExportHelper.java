package com.utc.warehouse.ui.common;

import com.utc.warehouse.util.CsvExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.io.File;

/**
 * Helper để các panel xuất CSV nhanh. Gọi {@link #exportCurrentTable} từ nút "Xuất CSV".
 */
public final class TableExportHelper {

    private TableExportHelper() { }

    /** Xuất nguyên bảng hiện tại. */
    public static void exportCurrentTable(JTable table, String name) {
        export(table.getModel(), table, name);
    }

    /** Xuất với bảng phụ (vd đã lọc bớt cột ảnh). */
    public static void exportModel(TableModel m, JComponent parent, String name) {
        File out = CsvExporter.exportTable(new JTable(m), name);
        notifyOk(parent, out, m.getRowCount());
    }

    private static void export(TableModel m, JComponent parent, String name) {
        File out = CsvExporter.exportTable(new JTable(m), name);
        notifyOk(parent, out, m.getRowCount());
    }

    private static void notifyOk(JComponent parent, File out, int rows) {
        if (out != null) {
            JOptionPane.showMessageDialog(parent,
                    "Đã xuất " + rows + " dòng ra:\n" + out.getAbsolutePath());
        }
    }
}
