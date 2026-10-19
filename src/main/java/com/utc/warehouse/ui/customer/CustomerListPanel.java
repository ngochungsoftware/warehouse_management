package com.utc.warehouse.ui.customer;

import com.utc.warehouse.entity.Customer;
import com.utc.warehouse.service.PartnerService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.UIConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CustomerListPanel extends BasePanel {

    private final PartnerService service = new PartnerService();
    private final Object[] COLS = {"ID", "Tên khách hàng", "SĐT", "Địa chỉ", "Email"};
    private JTable table;

    public CustomerListPanel() { super("Quản lý Khách hàng"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JButton btnAdd = primaryButton("+ Thêm");
        JButton btnEdit = secondaryButton("Sửa");
        JButton btnDelete = dangerButton("Xóa");
        JButton btnRefresh = secondaryButton("Làm mới");
        JButton btnExport = secondaryButton("Xuất CSV");
        bar.add(btnRefresh); bar.add(btnExport); bar.add(btnAdd); bar.add(btnEdit); bar.add(btnDelete);

        btnAdd.addActionListener(e -> { CustomerDialog d = new CustomerDialog(SwingUtilities.getWindowAncestor(this), null); d.setVisible(true); if (d.isSaved()) reload(); });
        btnEdit.addActionListener(e -> { Customer c = selected(); if (c == null) return; CustomerDialog d = new CustomerDialog(SwingUtilities.getWindowAncestor(this), c); d.setVisible(true); if (d.isSaved()) reload(); });
        btnDelete.addActionListener(e -> onDelete());
        btnRefresh.addActionListener(e -> reload());
        btnExport.addActionListener(e -> com.utc.warehouse.ui.common.TableExportHelper.exportCurrentTable(table, "khach_hang"));
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
        sc.bind("ctrl N", "add",    this::onShortcutAdd,    "Thêm KH");
        sc.bind("ctrl E", "edit",   this::onShortcutEdit,   "Sửa KH");
        sc.bind("DELETE", "delete", this::onShortcutDelete, "Xoá KH");
        sc.bind("F5",     "refresh",this::onShortcutRefresh,"Làm mới");
        sc.bind("ctrl X", "export", () -> com.utc.warehouse.ui.common.TableExportHelper.exportCurrentTable(table, "khach_hang"), "Xuất CSV");
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
        return TableUtils.scrollFor(table);
    }

    public void reload() {
        // STUB: bỏ trống logic server — chỉ giữ UI
    }

    private Customer selected() {
        // STUB: bỏ trống logic server — chỉ giữ UI
        return null;
    }

    private void onDelete() {
        // STUB: bỏ trống logic server — chỉ giữ UI
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
