package com.utc.warehouse.ui.supplier;

import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.service.PartnerService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.UIConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SupplierListPanel extends BasePanel {

    private final PartnerService service = new PartnerService();
    private final Object[] COLS = {"ID", "Tên NCC", "SĐT", "Địa chỉ", "Email"};
    private JTable table;

    public SupplierListPanel() { super("Quản lý Nhà cung cấp"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JButton btnAdd    = primaryButton("+ Thêm");
        JButton btnEdit   = secondaryButton("Sửa");
        JButton btnDelete = dangerButton("Xóa");
        JButton btnRefresh = secondaryButton("Làm mới");
        JButton btnExport = secondaryButton("Xuất Excel");
        bar.add(btnRefresh); bar.add(btnExport); bar.add(btnAdd); bar.add(btnEdit); bar.add(btnDelete);

        btnAdd.addActionListener(e    -> { SupplierDialog d = new SupplierDialog(SwingUtilities.getWindowAncestor(this), null); d.setVisible(true); if (d.isSaved()) reload(); });
        btnEdit.addActionListener(e   -> { Supplier s = selected(); if (s == null) return; SupplierDialog d = new SupplierDialog(SwingUtilities.getWindowAncestor(this), s); d.setVisible(true); if (d.isSaved()) reload(); });
        btnDelete.addActionListener(e -> onDelete());
        btnRefresh.addActionListener(e -> reload());
        btnExport.addActionListener(e -> com.utc.warehouse.ui.common.TableExportHelper.exportCurrentTable(table, "nha_cung_cap"));
        btnAdd.setName("btnAdd");
        btnEdit.setName("btnEdit");
        btnDelete.setName("btnDelete");
        btnRefresh.setName("btnRefresh");
        btnExport.setName("btnExport");
        return bar;
    }

    @Override protected void onShortcutAdd()    { triggerButton("btnAdd"); }
    @Override protected void onShortcutEdit()   { triggerButton("btnEdit"); }
    @Override protected void onShortcutDelete() { triggerButton("btnDelete"); }
    @Override protected void onShortcutRefresh(){ triggerButton("btnRefresh"); }
    @Override
    protected void installDefaultShortcuts() {
        com.utc.warehouse.ui.common.ShortcutManager sc = new com.utc.warehouse.ui.common.ShortcutManager(this);
        sc.bind("ctrl N", "add",    this::onShortcutAdd,    "Thêm NCC");
        sc.bind("ctrl E", "edit",   this::onShortcutEdit,   "Sửa NCC");
        sc.bind("DELETE", "delete", this::onShortcutDelete, "Xoá NCC");
        sc.bind("F5",     "refresh",this::onShortcutRefresh,"Làm mới");
        sc.bind("ctrl X", "export", () -> com.utc.warehouse.ui.common.TableExportHelper.exportCurrentTable(table, "nha_cung_cap"), "Xuất Excel");
    }

    private JButton findButtonByName(String name) {
        return (JButton) BasePanel.findFirst(this, c -> c instanceof JButton && name.equals(c.getName()));
    }
    private void triggerButton(String name) {
        JButton b = findButtonByName(name);
        if (b != null && b.isEnabled()) b.doClick();
    }

    @Override
    protected JComponent buildContent() {
        table = TableUtils.createTable(COLS, new Object[0][]);
        TableUtils.centerColumn(table, 0);

        // Double-click 1 dòng → xem chi tiết nhà cung cấp
        com.utc.warehouse.ui.common.TableDoubleClick.installFor(
                table, this::supplierAtRow, this::showSupplierDetail);
        return TableUtils.scrollFor(table);
    }

    /** Tra Supplier từ model row index (dùng cho double-click). */
    private Supplier supplierAtRow(int modelRow) {
        Object idObj = TableUtils.modelOf(table).getValueAt(modelRow, 0);
        if (idObj == null) return null;
        final Integer id;
        try { id = (idObj instanceof Integer) ? (Integer) idObj : Integer.valueOf(idObj.toString()); }
        catch (NumberFormatException ex) { return null; }
        return service.allSuppliers().stream()
                .filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }

    /** Dialog xem nhanh thông tin nhà cung cấp (read-only). */
    private void showSupplierDetail(Supplier s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mã: ").append(s.getId()).append('\n');
        sb.append("Tên: ").append(s.getName()).append('\n');
        sb.append("SĐT: ").append(s.getPhone() == null || s.getPhone().isBlank() ? "(trống)" : s.getPhone()).append('\n');
        sb.append("Email: ").append(s.getEmail() == null || s.getEmail().isBlank() ? "(trống)" : s.getEmail()).append('\n');
        sb.append("Địa chỉ: ").append(s.getAddress() == null || s.getAddress().isBlank() ? "(trống)" : s.getAddress()).append('\n');
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setFont(UIConstants.FONT_NORMAL);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                "Chi tiết " + s.getName(), JOptionPane.PLAIN_MESSAGE);
    }

    public void reload() {
        try {
            List<Supplier> list = service.allSuppliers();
            DefaultTableModel m = TableUtils.modelOf(table);
            m.setRowCount(0);
            for (Supplier s : list) m.addRow(new Object[]{ s.getId(), s.getName(), s.getPhone(), s.getAddress(), s.getEmail() });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Supplier selected() {
        int row = table.getSelectedRow(); if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        Object idObj = TableUtils.modelOf(table).getValueAt(modelRow, 0);
        Integer id = (idObj instanceof Integer) ? (Integer) idObj : Integer.valueOf(idObj.toString());
        return service.allSuppliers().stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }

    private void onDelete() {
        Supplier s = selected();
        if (s == null) { JOptionPane.showMessageDialog(this, "Chọn 1 nhà cung cấp để xóa."); return; }
        int ok = JOptionPane.showConfirmDialog(this, "Xóa NCC '" + s.getName() + "' ?",
                "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            try { service.deleteSupplier(s.getId()); reload(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Lỗi xóa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private JButton primaryButton(String t)   { return styleButton(t, UIConstants.PRIMARY,   Color.WHITE); }
    private JButton secondaryButton(String t) { return styleButton(t, UIConstants.ACCENT,    Color.WHITE); }
    private JButton dangerButton(String t)    { return styleButton(t, UIConstants.DANGER,    Color.WHITE); }
    private JButton styleButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(UIConstants.FONT_NORMAL);
        b.setPreferredSize(new Dimension(100, 30));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
