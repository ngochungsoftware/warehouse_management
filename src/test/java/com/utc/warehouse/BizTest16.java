package com.utc.warehouse;

import com.utc.warehouse.ui.common.TableDoubleClick;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * BizTest16: Tái hiện & chống triệu chứng "lúc được lúc không" khi double-click bảng.
 *
 * Ba ca từng gây lỗi:
 *  [A] Swing báo clickCount=1 cho cả 2 click (hay gặp trên macOS + trackpad).
 *  [B] Click vào khe giữa 2 dòng → rowAtPoint() trả -1 → im lặng.
 *  [C] mousePressed và mouseClicked của cùng 1 cặp click đều thoả điều kiện
 *      → callback bị gọi 2 lần (mở 2 dialog chồng nhau).
 */
public class BizTest16 {

    private static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        caseA_clickCountAlwaysOne();
        caseB_clickOnRowGap();
        caseC_noDuplicateFire();
        caseD_sortStillCorrect();
        caseE_repeatedDoubleClicks();

        System.out.println("\n=== Tổng: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) System.exit(1);
    }

    /* ---------- Ca E: double-click nhiều lần liên tiếp ---------- */
    private static void caseE_repeatedDoubleClicks() throws Exception {
        System.out.println("\n[E] Double-click 3 lần liên tiếp trên 2 dòng khác nhau");
        List<Integer> calls = new ArrayList<>();
        JTable table = newTable();

        SwingUtilities.invokeAndWait(() -> TableDoubleClick.installOn(table, calls::add));

        twoClicksAt(table, 40, table.getCellRect(0, 0, true).y + 5, true);
        Thread.sleep(50);
        twoClicksAt(table, 40, table.getCellRect(1, 0, true).y + 5, true);
        Thread.sleep(50);
        twoClicksAt(table, 40, table.getCellRect(2, 0, true).y + 5, true);

        check("Mỗi cặp double-click chỉ fire đúng 1 lần", calls.size() == 3,
                "số lần gọi = " + calls.size() + " (mong đợi 3), các dòng = " + calls);
    }

    /* ---------- Ca A: clickCount luôn = 1, chỉ có 2 sự kiện liên tiếp ---------- */
    private static void caseA_clickCountAlwaysOne() throws Exception {
        System.out.println("[A] 2 click liên tiếp nhưng clickCount luôn = 1");
        List<Integer> calls = new ArrayList<>();
        JTable table = newTable();

        SwingUtilities.invokeAndWait(() -> TableDoubleClick.installOn(table, calls::add));

        pressReleaseClick(table, 1, false);   // clickCount = 1
        Thread.sleep(80);                    // trong ngưỡng double-click
        pressReleaseClick(table, 1, false);   // clickCount = 1 lần nữa

        check("Callback được gọi dù clickCount luôn = 1", calls.size() == 1,
                "số lần gọi = " + calls.size() + " (mong đợi 1)");
    }

    /* ---------- Ca B: click vào khe giữa 2 dòng ---------- */
    private static void caseB_clickOnRowGap() throws Exception {
        System.out.println("\n[B] Click vào khe giữa 2 dòng (rowAtPoint = -1)");
        List<Integer> calls = new ArrayList<>();
        JTable table = newTable();

        SwingUtilities.invokeAndWait(() -> {
            TableDoubleClick.installOn(table, calls::add);
            table.setSize(300, 120);
            table.setRowSelectionInterval(0, 0);   // đang chọn dòng 0
        });

        int gapY = gapPoint(table);
        twoClicksAt(table, 40, gapY, false);

        check("Vẫn mở được dòng đang chọn khi click khe", calls.size() >= 1,
                "số lần gọi = " + calls.size() + " (mong đợi >= 1)");
    }

    /* ---------- Ca C: không gọi trùng cho 1 cặp click ---------- */
    private static void caseC_noDuplicateFire() throws Exception {
        System.out.println("\n[C] mousePressed + mouseClicked của cùng 1 cặp click");
        List<Integer> calls = new ArrayList<>();
        JTable table = newTable();

        SwingUtilities.invokeAndWait(() -> TableDoubleClick.installOn(table, calls::add));

        pressReleaseClick(table, 1, false);
        Thread.sleep(60);
        pressReleaseClick(table, 2, false);   // clickCount = 2 như thật

        check("Chỉ gọi callback 1 lần cho 1 cặp click", calls.size() == 1,
                "số lần gọi = " + calls.size() + " (mong đợi 1)");
    }

    /* ---------- Ca D: vẫn đúng khi bảng đã sort ---------- */
    private static void caseD_sortStillCorrect() throws Exception {
        System.out.println("\n[D] Double-click sau khi sort — phải trả đúng model row");
        List<Integer> calls = new ArrayList<>();
        JTable table = newTable();

        SwingUtilities.invokeAndWait(() -> TableDoubleClick.installOn(table, calls::add));
        SwingUtilities.invokeAndWait(() -> table.getRowSorter().toggleSortOrder(0));
        Thread.sleep(100);

        System.out.println("    Thứ tự sau sort: " + order(table));
        twoClicksAt(table, 40, table.getCellRect(0, 0, true).y + 5, true);

        check("Trả model row hợp lệ sau sort",
                !calls.isEmpty() && calls.get(0) >= 0 && calls.get(0) < 5,
                "modelRow = " + calls);
    }

    /* ===================== Helpers ===================== */

    /** Bảng 5 dòng: cột ID = [5,3,1,4,2] trong model gốc. Dùng TableUtils để có row sorter như app thật. */
    private static JTable newTable() {
        JTable t = new JTable(new DefaultTableModel(
                new Object[][]{
                        {5, "A"}, {3, "B"}, {1, "C"}, {4, "D"}, {2, "E"}
                }, new Object[]{"ID", "Tên"}));
        t.setAutoCreateRowSorter(true);   // giống TableUtils.createTable()
        return t;
    }

    /** Gửi press + release + click. Ghi đè clickCount nếu forceClickCount. */
    private static void pressReleaseClick(JTable t, int clickCount, boolean force) {
        Rectangle r = t.getCellRect(0, 0, true);
        int x = r.x + r.width / 2, y = r.y + r.height / 2;
        send(t, MouseEvent.MOUSE_PRESSED, x, y, clickCount);
        send(t, MouseEvent.MOUSE_RELEASED, x, y, clickCount);
        send(t, MouseEvent.MOUSE_CLICKED, x, y, force ? 2 : clickCount);
    }

    /** 2 click liên tiếp tại toạ độ bất kỳ. */
    private static void twoClicksAt(JTable t, int x, int y, boolean useClickCount) {
        send(t, MouseEvent.MOUSE_PRESSED, x, y, 1);
        send(t, MouseEvent.MOUSE_RELEASED, x, y, 1);
        send(t, MouseEvent.MOUSE_CLICKED, x, y, useClickCount ? 2 : 1);
        send(t, MouseEvent.MOUSE_PRESSED, x, y, useClickCount ? 2 : 1);
        send(t, MouseEvent.MOUSE_RELEASED, x, y, useClickCount ? 2 : 1);
        send(t, MouseEvent.MOUSE_CLICKED, x, y, useClickCount ? 2 : 1);
    }

    private static void send(JTable t, int id, int x, int y, int clickCount) {
        long t0 = System.currentTimeMillis();
        int mask = (id == MouseEvent.MOUSE_PRESSED) ? InputEvent.BUTTON1_DOWN_MASK : 0;
        MouseEvent e = new MouseEvent(t, id, t0, mask, x, y, clickCount,
                false, MouseEvent.BUTTON1);
        // mousePressed / mouseClicked phải chạy trên EDT
        Runnable task = () -> {
            for (java.awt.event.MouseListener l : t.getMouseListeners()) {
                try {
                    switch (id) {
                        case MouseEvent.MOUSE_PRESSED   -> l.mousePressed(e);
                        case MouseEvent.MOUSE_RELEASED  -> l.mouseReleased(e);
                        case MouseEvent.MOUSE_CLICKED   -> l.mouseClicked(e);
                        default -> { }
                    }
                } catch (Exception ignore) { }
            }
        };
        try { SwingUtilities.invokeAndWait(task); }
        catch (Exception ignore) { }
    }

    /** Trả về toạ độ y nằm giữa dòng 0 và dòng 1 (rowAtPoint sẽ trả -1). */
    private static int gapPoint(JTable t) {
        Rectangle r0 = t.getCellRect(0, 0, true);
        Rectangle r1 = t.getCellRect(1, 0, true);
        return (r0.y + r0.height + r1.y) / 2;
    }

    private static List<Integer> order(JTable t) {
        List<Integer> out = new ArrayList<>();
        for (int vr = 0; vr < t.getRowCount(); vr++) {
            out.add((Integer) t.getValueAt(vr, 0));
        }
        return out;
    }

    private static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "    ✓ " : "    ✗ ") + name + " — " + detail);
        if (ok) passed++; else failed++;
    }
}