package com.utc.warehouse.ui.category;

import com.utc.warehouse.entity.Category;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.UIConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CategoryListPanel extends BasePanel {

    private final ProductService service = new ProductService();
    private final Object[] COLS = {"ID", "Tên danh mục", "Mô tả"};
    private JTable table;
    private JTextField searchField;

    public CategoryListPanel() { super("Quản lý Danh mục"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JTextField search = new JTextField();
        search.setPreferredSize(new Dimension(220, 30));
        search.putClientProperty("JTextField.placeholderText", "Tìm theo tên...");
        JButton btnSearch = primaryButton("Tìm");
        JButton btnAdd    = primaryButton("+ Thêm");
        JButton btnEdit   = secondaryButton("Sửa");
        JButton btnDelete = dangerButton("Xóa");
        JButton btnRefresh = secondaryButton("Làm mới");
        JButton btnExport = secondaryButton("Xuất CSV");

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnRefresh);
        right.add(btnExport);
        right.add(btnAdd);
        right.add(btnEdit);
        right.add(btnDelete);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new JLabel("Tìm:"));
        left.add(search);
        left.add(btnSearch);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        btnAdd.addActionListener(e -> onAdd());
        btnEdit.addActionListener(e -> onEdit());
        btnDelete.addActionListener(e -> onDelete());
        btnRefresh.addActionListener(e -> reload());
        btnExport.addActionListener(e ->
                com.utc.warehouse.ui.common.TableExportHelper.exportCurrentTable(table, "danh_muc"));
        btnSearch.addActionListener(e -> reloadFiltered(search.getText()));
        search.addActionListener(e -> reloadFiltered(search.getText()));

        btnAdd.setName("btnAdd");
        btnEdit.setName("btnEdit");
        btnDelete.setName("btnDelete");
        btnRefresh.setName("btnRefresh");
        btnExport.setName("btnExport");
        this.searchField = search;

        return bar;
    }

    @Override
    protected void installDefaultShortcuts() {
        com.utc.warehouse.ui.common.ShortcutManager sc = new com.utc.warehouse.ui.common.ShortcutManager(this);
        sc.bind("ctrl N",  "add",     this::onAdd,     "Thêm danh mục");
        sc.bind("ctrl E",  "edit",    this::onEdit,    "Sửa danh mục");
        sc.bind("DELETE",  "delete",  this::onDelete,  "Xoá danh mục");
        sc.bind("F5",      "refresh", this::reload,    "Làm mới");
        sc.bind("ctrl F",  "search",  this::onShortcutFocusSearch, "Tìm kiếm");
        sc.bind("ctrl X",  "export",  () -> com.utc.warehouse.ui.common.TableExportHelper.exportCurrentTable(table, "danh_muc"), "Xuất CSV");
    }

    @Override protected void onShortcutAdd()    { onAdd(); }
    @Override protected void onShortcutEdit()   { onEdit(); }
    @Override protected void onShortcutDelete() { onDelete(); }
    @Override protected void onShortcutRefresh(){ reload(); }
    @Override protected void onShortcutFocusSearch() {
        if (searchField != null) { searchField.requestFocusInWindow(); searchField.selectAll(); }
    }

    @Override
    protected JComponent buildContent() {
        table = TableUtils.createTable(COLS, new Object[0][]);
        TableUtils.centerColumn(table, 0);

        // Double-click 1 dòng → xem chi tiết danh mục
        com.utc.warehouse.ui.common.TableDoubleClick.installFor(
                table, this::categoryAtRow, this::showCategoryDetail);
        return TableUtils.scrollFor(table);
    }

    /** Tra Category từ model row index (dùng cho double-click). */
    private Category categoryAtRow(int modelRow) {
        Object idObj = TableUtils.modelOf(table).getValueAt(modelRow, 0);
        if (idObj == null) return null;
        final Integer id;
        try { id = (idObj instanceof Integer) ? (Integer) idObj : Integer.valueOf(idObj.toString()); }
        catch (NumberFormatException ex) { return null; }
        return service.allCategories().stream()
                .filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    /** Dialog xem nhanh thông tin danh mục (read-only). */
    private void showCategoryDetail(Category c) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mã danh mục: ").append(c.getId()).append('\n');
        sb.append("Tên: ").append(c.getName()).append('\n');
        sb.append("Mô tả: ").append(c.getDescription() == null || c.getDescription().isBlank()
                ? "(trống)" : c.getDescription()).append('\n');
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setFont(UIConstants.FONT_NORMAL);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                "Chi tiết " + c.getName(), JOptionPane.PLAIN_MESSAGE);
    }

    public void reload() { reloadFiltered(""); }

    public void reloadFiltered(String keyword) {
        try {
            List<Category> list = (keyword == null || keyword.isBlank())
                    ? service.allCategories()
                    : service.allCategories().stream()
                        .filter(c -> c.getName().toLowerCase().contains(keyword.toLowerCase())
                                || (c.getDescription() != null
                                    && c.getDescription().toLowerCase().contains(keyword.toLowerCase())))
                        .toList();
            DefaultTableModel m = TableUtils.modelOf(table);
            m.setRowCount(0);
            for (Category c : list) m.addRow(new Object[]{ c.getId(), c.getName(), c.getDescription() });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Category selected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        Object idObj = TableUtils.modelOf(table).getValueAt(modelRow, 0);
        Integer id = (idObj instanceof Integer) ? (Integer) idObj : Integer.valueOf(idObj.toString());
        return service.allCategories().stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    private void onAdd() {
        CategoryDialog dlg = new CategoryDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        if (dlg.isSaved()) reload();
    }

    private void onEdit() {
        Category c = selected();
        if (c == null) { JOptionPane.showMessageDialog(this, "Chọn 1 danh mục để sửa."); return; }
        CategoryDialog dlg = new CategoryDialog(SwingUtilities.getWindowAncestor(this), c);
        dlg.setVisible(true);
        if (dlg.isSaved()) reload();
    }

    private void onDelete() {
        Category c = selected();
        if (c == null) { JOptionPane.showMessageDialog(this, "Chọn 1 danh mục để xóa."); return; }
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa danh mục '" + c.getName() + "' ?\nLưu ý: sản phẩm thuộc danh mục sẽ mất liên kết.",
                "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            try {
                service.deleteCategory(c.getId());
                reload();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi xóa: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- style helpers ---
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
