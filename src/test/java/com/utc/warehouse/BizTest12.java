package com.utc.warehouse;

import com.utc.warehouse.ui.common.TableDoubleClick;
import com.utc.warehouse.ui.common.TableUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * BizTest12: Test TableDoubleClick — xác nhận double-click hoạt động
 * đúng kể cả khi bảng đã sort (view row != model row).
 */
public class BizTest12 {

    private static int fired = 0;
    private static int lastModelRow = -99;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // Bảng 5 dòng: cột 0 = ID, cột 1 = Tên
            Object[][] data = {
                    {5, "E"},
                    {3, "C"},
                    {1, "A"},
                    {4, "D"},
                    {2, "B"}
            };
            JTable table = TableUtils.createTable(new Object[]{"ID", "Tên"}, data);
            JFrame f = new JFrame("test");
            f.setSize(400, 300);
            f.add(new JScrollPane(table));
            f.pack();
            f.setVisible(true);

// Nếu đang ở trong EDT (test gọi từ invokeAndWait) thì chạy trực tiếp,
        // nếu không thì invokeAndWait để bảo đảm listener đã gắn xong.
        Runnable install = () -> TableDoubleClick.installFor(
                table,
                    (com.utc.warehouse.ui.common.TableDoubleClick.RowConverter<Integer>)
                            modelRow -> modelRow,
                    row -> {
                        fired++;
                        lastModelRow = row;
                        System.out.println("  → double-click nhận modelRow=" + row);
                    });
        dispatch(table, install);

            // === Case 1: double-click khi CHƯA sort (viewRow == modelRow) ===
            System.out.println("[1] Double-click dòng view #1 (không sort)");
            fireDoubleClick(table, 1);
            assertFired(1, 1);

            // === Case 2: sort bảng descending theo cột ID ===
            System.out.println("[2] Sort descending cột 'ID'");
            table.getRowSorter().toggleSortOrder(0);
            table.getRowSorter().toggleSortOrder(0); // sort descending
            System.out.println("    Thứ tự sau sort: " + orderIds(table));

            fired = 0;
            // Sau sort descending: view #0 → ID=5 (modelRow 0)
            //                     view #1 → ID=4 (modelRow 3, vì data gốc = [5,3,1,4,2])
            System.out.println("[3] Double-click dòng view #1 sau sort (kỳ vọng ID=4)");
            fireDoubleClick(table, 1);
            int expectedModelRow = 3; // ID=4 nằm ở model row index 3
            System.out.println("    Kỳ vọng modelRow=" + expectedModelRow
                    + " (dòng ID=4 trong model gốc [5,3,1,4,2])");
            if (lastModelRow == expectedModelRow) {
                System.out.println("    ✓ Đúng! Không bị lệch chỉ số khi sort.");
            } else {
                System.out.println("    ✗ SAI! modelRow=" + lastModelRow + " (mong đợi " + expectedModelRow + ")");
            }

            // === Case 3: verify selection đồng bộ sau double-click ===
            System.out.println("[4] Selection sau double-click view #1: row=" + table.getSelectedRow());
            System.out.println("    selected modelRow=" + table.convertRowIndexToModel(table.getSelectedRow()));

            f.dispose();
        });

        System.out.println("\n=== Tổng số lần callback được gọi: " + fired + " ===");
        System.out.println("=== BizTest12 hoàn tất ===");
    }

    private static void assertFired(int expectedCount, int step) {
        if (fired == expectedCount) {
            System.out.println("    ✓ Callback được gọi " + fired + " lần");
        } else {
            System.out.println("    ✗ Callback gọi " + fired + " lần (mong đợi " + expectedCount + ")");
        }
    }

    /** Giả lập double-click chuột trái tại dòng view rowRow. */
    private static void fireDoubleClick(JTable table, int row) {
        Rectangle r = table.getCellRect(row, 0, true);
        int x = r.x + r.width / 2;
        int y = r.y + r.height / 2;

        // Click 1
        send(table, MouseEvent.MOUSE_PRESSED,  x, y, 1);
        send(table, MouseEvent.MOUSE_RELEASED, x, y, 1);
        send(table, MouseEvent.MOUSE_CLICKED,  x, y, 1);
        // Click 2 — clickCount = 2 là tín hiệu double-click chuẩn của Swing
        send(table, MouseEvent.MOUSE_PRESSED,  x, y, 2);
        send(table, MouseEvent.MOUSE_RELEASED, x, y, 2);
        send(table, MouseEvent.MOUSE_CLICKED,  x, y, 2);
    }

    /** Gửi 1 mouse event tới mọi listener của table (trên EDT). */
    private static void send(JTable table, int id, int x, int y, int clickCount) {
        int mask = (id == MouseEvent.MOUSE_PRESSED) ? InputEvent.BUTTON1_DOWN_MASK : 0;
        MouseEvent e = new MouseEvent(table, id, System.currentTimeMillis(), mask,
                x, y, clickCount, false, MouseEvent.BUTTON1);
        Runnable task = () -> {
            for (MouseListener l : table.getMouseListeners()) {
                try {
                    switch (id) {
                        case MouseEvent.MOUSE_PRESSED  -> l.mousePressed(e);
                        case MouseEvent.MOUSE_RELEASED -> l.mouseReleased(e);
                        case MouseEvent.MOUSE_CLICKED  -> l.mouseClicked(e);
                        default -> { }
                    }
                } catch (Exception ignore) { }
            }
        };
        dispatch(table, task);
    }

    /**
     * Chạy task trên EDT mà không deadlock.
     * invokeAndWait sẽ treo vô hạn nếu được gọi từ chính EDT, nên phải
     * phát hiện trước: nếu đã ở trong EDT thì chạy trực tiếp.
     */
    private static void dispatch(JTable table, Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
            return;
        }
        SwingUtilities.invokeLater(task);
        try { SwingUtilities.invokeAndWait(() -> { }); } catch (Exception ignore) { }
    }

    private static String orderIds(JTable table) {
        StringBuilder sb = new StringBuilder("[");
        DefaultTableModel m = TableUtils.modelOf(table);
        for (int v = 0; v < table.getRowCount(); v++) {
            if (v > 0) sb.append(", ");
            sb.append(m.getValueAt(table.convertRowIndexToModel(v), 0));
        }
        return sb.append("]").toString();
    }
}