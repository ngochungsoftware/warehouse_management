package com.utc.warehouse.ui.common;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Dialog hiển thị danh sách phím tắt.
 * Dùng cho F1.
 */
public final class HelpDialog {

    private HelpDialog() { }

    /** Hiển thị dialog. sections: mỗi section là 1 nhóm phím tắt. */
    public static void show(JFrame owner, String title, List<ShortcutManager.Entry>... sections) {
        JDialog dlg = new JDialog(owner, title, true);
        dlg.setSize(560, 480);
        dlg.setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel header = new JLabel("⌨  Phím tắt");
        header.setFont(Theme.FONT_TITLE);
        header.setForeground(Theme.BRAND_DARK);
        root.add(header, BorderLayout.NORTH);

        // Bảng tổng hợp
        Object[] cols = {"Phím", "Hành động", "Mô tả"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (List<ShortcutManager.Entry> sec : sections) {
            for (ShortcutManager.Entry e : sec) {
                if (e.name() == null) continue;
                m.addRow(new Object[]{ e.key(), e.name(), e.description() });
            }
        }

        JTable table = new JTable(m);
        table.setRowHeight(30);
        table.setFont(Theme.FONT_NORMAL);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setShowGrid(false);
        table.getTableHeader().setFont(Theme.FONT_HEADER);
        table.getTableHeader().setBackground(Theme.BRAND);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(Theme.SELECT_BG);
        table.setSelectionForeground(Theme.TEXT);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(260);

        // In đậm các dòng phân cách (key trống)
        javax.swing.table.DefaultTableCellRenderer bold = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                JLabel l = (JLabel) c;
                String s = value == null ? "" : value.toString();
                if (s.startsWith("──")) {
                    l.setFont(Theme.FONT_HEADER);
                    l.setForeground(Theme.BRAND_DARK);
                    l.setBackground(Theme.BRAND_LIGHT);
                } else {
                    l.setFont(Theme.FONT_NORMAL);
                    l.setForeground(Theme.TEXT);
                    l.setBackground(row % 2 == 0 ? Color.WHITE : Theme.ZEBRA);
                }
                l.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return l;
            }
        };
        for (int i = 0; i < 3; i++) table.getColumnModel().getColumn(i).setCellRenderer(bold);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        root.add(sp, BorderLayout.CENTER);

        JButton btnClose = ModernComponents.primaryButton("Đóng (Esc)");
        btnClose.addActionListener(e -> dlg.dispose());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnClose);
        root.add(btnRow, BorderLayout.SOUTH);

        dlg.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        dlg.getRootPane().getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { dlg.dispose(); }
        });
        dlg.getRootPane().setDefaultButton(btnClose);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }
}
