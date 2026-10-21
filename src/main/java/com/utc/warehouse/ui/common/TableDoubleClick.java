package com.utc.warehouse.ui.common;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Tiện ích cho hành vi double-click trên JTable.
 *
 * <p>Vấn đề 1 — chỉ số dòng: khi bảng có {@code setAutoCreateRowSorter(true)}
 * (xem {@link TableUtils#createTable}), chỉ số dòng trên UI khác với chỉ số dòng
 * trong model. Nếu đọc thẳng model bằng chỉ số view thì sẽ lấy sai dòng khi
 * người dùng đã sort. Helper này gói sẵn việc chuyển đổi chỉ số.
 *
 * <p>Vấn đề 2 — {@code MouseEvent#getClickCount()} không đáng tin trên mọi nền tảng
 * (đặc biệt macOS + trackpad): có lúc 2 click liên tiếp vẫn báo {@code clickCount == 1},
 * khiến hành vi "lúc được lúc không". Vì vậy helper tự phát hiện double-click bằng
 * <b>khoảng cách thời gian + cùng dòng</b>, đồng thời vẫn chấp nhận {@code clickCount == 2}
 * từ Swing để tương thích với cách gọi bằng tay trong test.
 *
 * <p>Vấn đề 3 — {@code rowAtPoint()} trả {@code -1} khi người dùng click vào khe
 * giữa hai dòng hoặc vùng trống dưới bảng. Helper sẽ fallback sang dòng đang được
 * chọn thay vì im lặng bỏ qua.
 *
 * <p>Cách dùng:
 * <pre>{@code
 * TableDoubleClick.installFor(table, this::productAtRow, p -> openDetail(p));
 * }</pre>
 */
public final class TableDoubleClick {

    /** Khoảng cách tối đa giữa 2 click để coi là double-click (ms). */
    private static final long DOUBLE_CLICK_MS = 400;

    private TableDoubleClick() { }

    /**
     * Gắn double-click chuột trái cho {@code table}.
     * Callback nhận <b>model row index</b> (đã quy đổi từ view row).
     */
    public static void installOn(JTable table, java.util.function.IntConsumer modelRowConsumer) {
        table.addMouseListener(new MouseAdapter() {
            private long lastClickAt;
            private int  lastRow = -1;
            /** Đặt lại mỗi lần có MOUSE_PRESSED — tức là bắt đầu 1 click mới. */
            private boolean alreadyFired;

            @Override
            public void mousePressed(MouseEvent e) {
                // Mỗi lần nhấn chuột xuống = 1 click mới bắt đầu. Phải mở lại
                // "còng" chống bắn trùng, nếu không thì sau double-click đầu
                // tiên mọi double-click về sau (kể cả trên dòng khác) đều bị bỏ qua.
                // Chỉ reset khi clickCount == 1: click thứ 2 trong cùng cặp cũng
                // có mousePressed nhưng mang clickCount = 2, phải giữ còng lại
                // để không bắn hai lần cho một cặp chuột.
                if (e.getClickCount() <= 1) alreadyFired = false;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    lastRow = -1;
                    alreadyFired = false;
                    return;
                }

                int row = resolveRow(e);
                if (row < 0) {
                    lastRow = -1;
                    alreadyFired = false;
                    return;
                }

                long now = System.currentTimeMillis();
                boolean byEvent = e.getClickCount() >= 2;
                boolean byTiming = (row == lastRow) && (now - lastClickAt <= DOUBLE_CLICK_MS);

                if ((byEvent || byTiming) && !alreadyFired) {
                    alreadyFired = true;
                    fire(row);
                }

                lastClickAt = now;
                lastRow = row;
            }

            /** Dòng view cần xử lý: ưu tiên vị trí chuột, fallback dòng đang chọn. */
            private int resolveRow(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) return row;
                return table.getSelectedRow();
            }

            private void fire(int viewRow) {
                try {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    modelRowConsumer.accept(table.convertRowIndexToModel(viewRow));
                } catch (Throwable t) {
                    // Listener chạy trong EDT: nếu để exception nổi ra, Swing sẽ nuốt
                    // và người dùng thấy "bấm gì cũng không có gì xảy ra".
                    t.printStackTrace();
                    JOptionPane.showMessageDialog(
                            SwingUtilities.getWindowAncestor(table),
                            "Không mở được chi tiết:\n"
                                    + t.getClass().getSimpleName() + "\n" + t.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    /**
     * Gắn double-click và tự tra entity: {@code rowConverter} nhận model row index
     * và trả về domain object (có thể null nếu dòng không hợp lệ).
     */
    public static <T> void installFor(JTable table, RowConverter<T> converter, RowAction<T> action) {
        installOn(table, modelRow -> {
            T row = converter.convert(modelRow);
            if (row != null) action.open(row);
        });
    }

    /** Chuyển model row index -> domain object. */
    @FunctionalInterface
    public interface RowConverter<T> {
        T convert(int modelRow);
    }

    /** Hành động khi double-click, nhận domain object đã tra. */
    @FunctionalInterface
    public interface RowAction<T> {
        void open(T row);
    }
}