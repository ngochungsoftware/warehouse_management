package com.utc.warehouse.ui.common;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.List;

/**
 * Tiện ích tạo JTable với style đẹp:
 * - Header gradient (brand color)
 * - Zebra row
 * - Row hover highlight
 * - Selection blue
 * - Auto resize
 */
public final class TableUtils {

    private TableUtils() { }

    /** Tạo JTable không cho edit, có selection đơn, header đậm, row height đẹp. */
    public static JTable createTable(Object[] columns, Object[][] data) {
        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model) {
            private int hoverRow = -1;
            { addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                    int r = rowAtPoint(e.getPoint());
                    if (r != hoverRow) { hoverRow = r; repaint(); }
                }
            });
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    hoverRow = -1; repaint();
                }
            }); }

            @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    boolean zebra = (row % 2 == 1);
                    c.setBackground(row == hoverRow ? Theme.SELECT_BG : (zebra ? Theme.ZEBRA : Color.WHITE));
                    c.setForeground(Theme.TEXT);
                } else {
                    c.setBackground(Theme.SELECT_BG);
                    c.setForeground(Theme.TEXT);
                }
                if (c instanceof JLabel l) {
                    l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                }
                return c;
            }
        };
        table.setRowHeight(34);
        table.setFont(Theme.FONT_NORMAL);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setBackground(Color.WHITE);
        table.setForeground(Theme.TEXT);

        // Header style
        JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(0, 40));
        header.setReorderingAllowed(false);
        return table;
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            l.setBackground(Theme.BRAND);
            l.setForeground(Color.WHITE);
            l.setFont(Theme.FONT_HEADER);
            l.setHorizontalAlignment(SwingConstants.LEFT);
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(255, 255, 255, 40)),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            l.setOpaque(true);
            return l;
        }
    }

    /** Helper lấy DefaultTableModel từ JTable. */
    public static DefaultTableModel modelOf(JTable table) {
        return (DefaultTableModel) table.getModel();
    }

    /** Thay toàn bộ data của bảng. */
    public static void setData(JTable table, List<Object[]> rows) {
        DefaultTableModel m = modelOf(table);
        m.setRowCount(0);
        for (Object[] r : rows) m.addRow(r);
    }

    /** Center renderer cho 1 cột cụ thể. */
    public static void centerColumn(JTable table, int col) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(col).setCellRenderer(r);
    }

    /** Right renderer cho 1 cột cụ thể. */
    public static void rightColumn(JTable table, int col) {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(col).setCellRenderer(r);
    }

    /** Tạo scroll pane có viền, nền trắng. */
    public static JScrollPane scrollFor(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        sp.getViewport().setBackground(Color.WHITE);
        sp.setBackground(Color.WHITE);
        return sp;
    }
}
