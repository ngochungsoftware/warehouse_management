package com.utc.warehouse.ui.product;

import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.ImageService;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.TableExportHelper;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.UIConstants;
import com.utc.warehouse.util.CsvExporter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductListPanel extends BasePanel {

    private final ProductService service = new ProductService();
    private final ImageService imageService = new ImageService();

    // Cột:  0=Ảnh, 1=ID, 2=SKU, 3=Tên, 4=Danh mục, 5=ĐVT, 6=Giá bán,
    //       7=Giá vốn, 8=LN/ĐV, 9=Margin%, 10=VAT%, 11=Tồn tối thiểu, 12=Kinh doanh
    private static final int COL_IMAGE  = 0;
    private static final int COL_ID     = 1;
    private static final int COL_SKU    = 2;
    private static final int COL_NAME   = 3;
    private static final int COL_CAT    = 4;
    private static final int COL_UNIT   = 5;
    private static final int COL_SALE   = 6;
    private static final int COL_COST   = 7;
    private static final int COL_PROFIT = 8;
    private static final int COL_MARGIN = 9;
    private static final int COL_VAT    = 10;
    private static final int COL_MIN    = 11;
    private static final int COL_ACTIVE = 12;

    private final Object[] COLS = {
            "Ảnh", "ID", "SKU", "Tên sản phẩm", "Danh mục", "ĐVT",
            "Giá bán", "Giá vốn", "LN/ĐV", "Margin%", "VAT%", "Tồn tối thiểu", "Kinh doanh"
    };

    private JTable table;
    private JTextField searchField;
    /** Cache id -> imagePath để renderer không phải query DB lặp lại. */
    private final Map<Integer, String> imagePathCache = new HashMap<>();
    /** Cache id -> BufferedImage (đã scale 40x40) để renderer KHÔNG phải đọc disk mỗi lần vẽ. */
    private final Map<Integer, BufferedImage> imageThumbCache = new HashMap<>();
    /** Cache id -> Product để tránh service.search() mỗi lần. */
    private final Map<Integer, Product> productCache = new HashMap<>();

    public ProductListPanel() { super("Quản lý Sản phẩm"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JTextField search = new JTextField();
        search.setPreferredSize(new Dimension(220, 30));
        JButton btnSearch   = primaryButton("Tìm");
        JButton btnRefresh  = secondaryButton("Làm mới");
        JButton btnAdd      = primaryButton("+ Thêm");
        JButton btnEdit     = secondaryButton("Sửa");
        JButton btnDelete   = dangerButton("Xóa");
        JButton btnExport   = secondaryButton("Xuất CSV");

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new JLabel("Tìm:"));
        left.add(search);
        left.add(btnSearch);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnRefresh); right.add(btnExport); right.add(btnAdd); right.add(btnEdit); right.add(btnDelete);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        btnSearch.addActionListener(e -> reloadFiltered(search.getText()));
        search.addActionListener(e -> reloadFiltered(search.getText()));
        btnRefresh.addActionListener(e -> { search.setText(""); reload(); });
        btnExport.addActionListener(e -> exportCsv());
        btnAdd.addActionListener(e    -> onAdd());
        btnEdit.addActionListener(e   -> onEdit());
        btnDelete.addActionListener(e -> onDelete());

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
        sc.bind("ctrl N",  "add",     this::onAdd,     "Thêm sản phẩm");
        sc.bind("ctrl E",  "edit",    this::onEdit,    "Sửa sản phẩm");
        sc.bind("DELETE",  "delete",  this::onDelete,  "Xoá sản phẩm");
        sc.bind("F5",      "refresh", this::reload,    "Làm mới danh sách");
        sc.bind("ctrl F",  "search",  this::onShortcutFocusSearch, "Tìm kiếm");
        sc.bind("ctrl X",  "export",  this::exportCsv, "Xuất CSV");
    }

    @Override protected void onShortcutAdd()     { onAdd(); }
    @Override protected void onShortcutEdit()    { onEdit(); }
    @Override protected void onShortcutDelete()  { onDelete(); }
    @Override protected void onShortcutRefresh() { reload(); }
    @Override protected void onShortcutFocusSearch() {
        if (searchField != null) { searchField.requestFocusInWindow(); searchField.selectAll(); }
    }

    @Override
    protected JComponent buildContent() {
        table = TableUtils.createTable(COLS, new Object[0][]);

        // Format & alignment cho từng cột
        TableUtils.centerColumn(table, COL_ID);
        TableUtils.centerColumn(table, COL_UNIT);
        TableUtils.centerColumn(table, COL_VAT);
        TableUtils.centerColumn(table, COL_MIN);
        TableUtils.centerColumn(table, COL_ACTIVE);
        TableUtils.rightColumn(table, COL_SALE);
        TableUtils.rightColumn(table, COL_COST);
        TableUtils.rightColumn(table, COL_PROFIT);
        TableUtils.centerColumn(table, COL_MARGIN);

        // Cột Ảnh
        table.getColumnModel().getColumn(COL_IMAGE).setPreferredWidth(56);
        table.getColumnModel().getColumn(COL_IMAGE).setMaxWidth(80);
        table.getColumnModel().getColumn(COL_IMAGE).setCellRenderer(new ImageCellRenderer());
        table.setRowHeight(48);

        // Double-click một dòng → mở dialog xem chi tiết sản phẩm
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() < 2) return;
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0) return;
                int modelRow = table.convertRowIndexToModel(viewRow);
                // Đảm bảo dòng được chọn (phòng khi reload vừa reset selection)
                table.setRowSelectionInterval(viewRow, viewRow);
                Product p = selected(modelRow);
                if (p == null) return;
                ProductViewDialog dlg = new ProductViewDialog(
                        SwingUtilities.getWindowAncestor(ProductListPanel.this), p);
                dlg.setVisible(true);
            }
        });

        return TableUtils.scrollFor(table);
    }

    /** Renderer cho cột ảnh: lấy BufferedImage từ cache (đã scale 40x40 sẵn). */
    private class ImageCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, "", sel, focus, row, col);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            l.setText("");

            int modelRow = t.convertRowIndexToModel(row);
            Object idObj = TableUtils.modelOf(t).getValueAt(modelRow, COL_ID);
            if (idObj == null) {
                l.setText("—"); l.setForeground(UIConstants.TEXT_MUTED);
                return l;
            }
            Integer id = (idObj instanceof Integer) ? (Integer) idObj : Integer.valueOf(idObj.toString());

            // 1) Ưu tiên cache RAM (đã load 1 lần khi reload)
            BufferedImage thumb = imageThumbCache.get(id);
            if (thumb != null) {
                l.setIcon(new ImageIcon(thumb));
                l.setText("");
                return l;
            }
            // 2) Có path nhưng cache rỗng (chưa preload xong) → hiện placeholder, KHÔNG đọc disk tại đây
            String path = imagePathCache.get(id);
            if (path == null || path.isBlank()) {
                l.setText("—"); l.setForeground(UIConstants.TEXT_MUTED);
                return l;
            }
            l.setText("…"); l.setForeground(UIConstants.TEXT_MUTED);
            return l;
        }
    }

    private static Image scaleImage(BufferedImage src, int maxW, int maxH) {
        double scale = Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight());
        int nw = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int nh = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally { g.dispose(); }
        return out;
    }

    public void reload() { reloadFiltered(""); }

    public void reloadFiltered(String keyword) {
        try {
            List<Product> list = service.search(keyword);
            DefaultTableModel m = TableUtils.modelOf(table);

            // Lưu lại dòng đang chọn để sau khi populate xong restore theo ID
            int previousSelected = table.getSelectedRow();
            Integer previousId = null;
            if (previousSelected >= 0) {
                try {
                    Object v = m.getValueAt(table.convertRowIndexToModel(previousSelected), COL_ID);
                    if (v != null) previousId = (v instanceof Integer) ? (Integer) v
                            : Integer.valueOf(v.toString());
                } catch (Exception ignored) { }
            }

            m.setRowCount(0);

            // Refresh cache để renderer không query DB / đọc disk
            productCache.clear();
            imagePathCache.clear();
            imageThumbCache.clear();

            for (Product p : list) {
                Integer id = p.getId();
                productCache.put(id, p);
                String path = p.getImagePath();
                imagePathCache.put(id, path);
                // Eager-load thumbnail trong EDT — list thường nhỏ (< vài trăm), chấp nhận
                // nếu ảnh to, sẽ được tải lần lượt. Ảnh đã load sẽ cache → click lần 2+ cực nhanh.
                if (path != null && !path.isBlank()) {
                    BufferedImage img = imageService.load(path);
                    if (img != null) {
                        imageThumbCache.put(id, (BufferedImage) scaleImage(img, 40, 40));
                    }
                }

                m.addRow(new Object[]{
                        null,                                // Ảnh — renderer vẽ
                        id,                                  // 1 ID
                        p.getSku(),                          // 2 SKU
                        p.getName(),                         // 3 Tên
                        p.getCategory() == null ? "" : p.getCategory().getName(), // 4 Danh mục
                        p.getUnit(),                         // 5 ĐVT
                        p.getSalePrice(),                    // 6 Giá bán
                        p.getCostPrice(),                    // 7 Giá vốn
                        p.getProfitPerUnit(),                // 8 LN/ĐV
                        p.getProfitMarginPercent().toPlainString() + "%",  // 9 Margin
                        p.getVatRate().toPlainString() + "%",// 10 VAT
                        p.getMinStock(),                     // 11 Tồn
                        Boolean.TRUE.equals(p.getActive()) ? "✓" : "✗"     // 12 Active
                });
            }
            // Restore selection theo ID (vì setRowCount(0) + addRow làm mất selection)
            if (previousId != null) {
                for (int r = 0; r < m.getRowCount(); r++) {
                    Object v = m.getValueAt(r, COL_ID);
                    Integer id = (v instanceof Integer) ? (Integer) v
                            : (v == null ? null : Integer.valueOf(v.toString()));
                    if (previousId.equals(id)) {
                        int viewRow = table.convertRowIndexToView(r);
                        if (viewRow >= 0) {
                            table.setRowSelectionInterval(viewRow, viewRow);
                            table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                        }
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Trả về Product đang được chọn trong bảng. Null nếu chưa chọn. */
    private Product selected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        return selected(modelRow);
    }

    /** Lấy Product theo modelRow cụ thể (dùng khi biết chính xác dòng user click). */
    private Product selected(int modelRow) {
        if (modelRow < 0 || modelRow >= TableUtils.modelOf(table).getRowCount()) return null;
        Object idObj = TableUtils.modelOf(table).getValueAt(modelRow, COL_ID);
        if (idObj == null) return null;
        Integer id;
        try { id = (idObj instanceof Integer) ? (Integer) idObj : Integer.valueOf(idObj.toString()); }
        catch (NumberFormatException ex) { return null; }
        if (id == null) return null;

        // Ưu tiên cache (vừa reload); nếu không có mới query
        Product p = productCache.get(id);
        if (p != null) return p;
        return service.search("").stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
    }

    private void onAdd() {
        ProductDialog d = new ProductDialog(SwingUtilities.getWindowAncestor(this), null);
        d.setVisible(true);
        if (d.isSaved()) reload();
    }

    private void onEdit() {
        Product p = selected();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Chọn 1 sản phẩm để sửa.");
            return;
        }
        ProductDialog d = new ProductDialog(SwingUtilities.getWindowAncestor(this), p);
        d.setVisible(true);
        if (d.isSaved()) reload();
    }

    private void onDelete() {
        Product p = selected();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Chọn 1 sản phẩm để xóa.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa sản phẩm '" + p.getName() + "' ?",
                "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            try {
                service.delete(p.getId());
                // Xoá ảnh đính kèm (nếu có) để giải phóng đĩa
                if (p.getImagePath() != null && !p.getImagePath().isBlank()) {
                    imageService.delete(p.getImagePath());
                }
                reload();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi xóa: " + ex.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Xuất danh sách hiện tại ra CSV (bỏ cột Ảnh vì chỉ là renderer). */
    public void exportCsv() {
        try {
            if (TableUtils.modelOf(table).getRowCount() == 0) reload();
            DefaultTableModel src = TableUtils.modelOf(table);

            // Bỏ cột COL_IMAGE (0)
            DefaultTableModel copy = new DefaultTableModel();
            for (int c = 0; c < src.getColumnCount(); c++) {
                if (c == COL_IMAGE) continue;
                copy.addColumn(src.getColumnName(c));
            }
            for (int r = 0; r < src.getRowCount(); r++) {
                Object[] row = new Object[copy.getColumnCount()];
                int idx = 0;
                for (int c = 0; c < src.getColumnCount(); c++) {
                    if (c == COL_IMAGE) continue;
                    row[idx++] = src.getValueAt(r, c);
                }
                copy.addRow(row);
            }

            java.io.File out = CsvExporter.exportTable(new JTable(copy), "san_pham");
            if (out != null) {
                JOptionPane.showMessageDialog(this,
                        "Đã xuất " + copy.getRowCount() + " dòng ra:\n" + out.getAbsolutePath());
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
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
