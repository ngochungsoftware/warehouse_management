package com.utc.warehouse.ui.stock;

import com.utc.warehouse.entity.Product;
import com.utc.warehouse.entity.StockTransactionDetail;
import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.service.PartnerService;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.service.ReportService;
import com.utc.warehouse.service.StockService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.UIConstants;
import com.utc.warehouse.util.MoneyFormat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ImportPanel extends BasePanel {

    private final ProductService  productService  = new ProductService();
    private final PartnerService  partnerService  = new PartnerService();
    private final StockService    stockService    = new StockService();
    private final ReportService   reportService   = new ReportService();

    private final JComboBox<Supplier> cbSupplier  = new JComboBox<>();
    private final JTextField          txtNote     = new JTextField();
    private final JComboBox<Product>  cbProduct   = new JComboBox<>();
    private final JTextField          txtQty      = new JTextField("1");
    private final JTextField          txtPrice    = new JTextField("0");
    private final JTable              detailTable;
    private final Object[] DET_COLS = {"#", "SKU", "Tên sản phẩm", "SL", "Đơn giá", "Thành tiền"};
    private final DefaultListModel<StockTransactionDetail> detailsModel = new DefaultListModel<>();
    // Ta dùng List<StockTransactionDetail> thay cho list model
    private final List<StockTransactionDetail> details = new ArrayList<>();

    public ImportPanel() {
        super("Tạo Phiếu Nhập Kho");
        detailTable = TableUtils.createTable(DET_COLS, new Object[0][]);
    }

    @Override
    protected JComponent buildToolbar() {
        // Không dùng toolbar, ta sẽ build full content
        return null;
    }

    @Override
    protected JComponent buildContent() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setOpaque(false);

        // --- Top: thông tin phiếu ---
        JPanel top = new JPanel(new GridBagLayout());
        top.setBackground(UIConstants.CARD_BG);
        top.setBorder(BorderFactory.createTitledBorder("Thông tin phiếu nhập"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        loadSuppliers();
        c.gridx = 0; c.gridy = 0; top.add(new JLabel("Nhà cung cấp:"), c);
        c.gridx = 1; c.weightx = 1; top.add(cbSupplier, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0; top.add(new JLabel("Ghi chú:"), c);
        c.gridx = 1; c.weightx = 1; top.add(txtNote, c);

        // --- Middle: thêm dòng ---
        JPanel mid = new JPanel(new GridBagLayout());
        mid.setBackground(UIConstants.CARD_BG);
        mid.setBorder(BorderFactory.createTitledBorder("Thêm sản phẩm"));
        GridBagConstraints m = new GridBagConstraints();
        m.insets = new Insets(6, 8, 6, 8);
        m.anchor = GridBagConstraints.WEST;
        m.fill = GridBagConstraints.HORIZONTAL;

        loadProducts();
        m.gridx = 0; m.gridy = 0; mid.add(new JLabel("Sản phẩm:"), m);
        m.gridx = 1; m.gridx = 1; m.weightx = 1; mid.add(cbProduct, m);

        m.gridx = 0; m.gridy = 1; m.weightx = 0; mid.add(new JLabel("Số lượng:"), m);
        m.gridx = 1; mid.add(txtQty, m);

        m.gridx = 0; m.gridy = 2; mid.add(new JLabel("Đơn giá (VNĐ):"), m);
        m.gridx = 1; mid.add(txtPrice, m);

        m.gridx = 0; m.gridy = 3; m.gridwidth = 2; m.anchor = GridBagConstraints.CENTER;
        JButton btnAddLine = primaryButton("+ Thêm dòng");
        btnAddLine.setName("btnAddLine");
        btnAddLine.addActionListener(e -> addLine());
        mid.add(btnAddLine, m);

        // --- Right: bảng chi tiết + nút lưu ---
        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setOpaque(false);
        TableUtils.centerColumn(detailTable, 0);
        TableUtils.centerColumn(detailTable, 3);
        TableUtils.rightColumn(detailTable, 4);
        TableUtils.rightColumn(detailTable, 5);
        right.add(TableUtils.scrollFor(detailTable), BorderLayout.CENTER);

        JButton btnRemove = dangerButton("Xóa dòng chọn");
        btnRemove.setName("btnRemove");
        btnRemove.addActionListener(e -> removeLine());
        JButton btnSave = primaryButton("Lưu phiếu nhập");
        btnSave.setPreferredSize(new Dimension(160, 36));
        btnSave.setName("btnSave");
        btnSave.addActionListener(e -> saveTransaction());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnRemove);
        btnRow.add(btnSave);
        right.add(btnRow, BorderLayout.SOUTH);

        // --- Combine ---
        JPanel topWrap = new JPanel();
        topWrap.setLayout(new BoxLayout(topWrap, BoxLayout.Y_AXIS));
        topWrap.setOpaque(false);
        topWrap.add(top);
        topWrap.add(mid);

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(380, 0));
        left.add(topWrap, BorderLayout.NORTH);

        root.add(left, BorderLayout.WEST);
        root.add(right, BorderLayout.CENTER);

        return root;
    }

    private void loadSuppliers() {
        cbSupplier.removeAllItems();

        // Mục placeholder, id = -1, name = "(Chọn NCC)". KHÔNG lưu được
        // vào DB vì createImport sẽ bỏ qua null.
        Supplier placeholder = new Supplier();
        placeholder.setId(-1);
        placeholder.setName("(Chọn NCC)");
        cbSupplier.addItem(placeholder);

        List<Supplier> list = partnerService.allSuppliers();
        for (Supplier s : list) cbSupplier.addItem(s);

        cbSupplier.setSelectedItem(placeholder);   // mặc định CHƯA chọn NCC
        cbSupplier.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                Supplier s = (value instanceof Supplier) ? (Supplier) value : null;
                if (s == null) {
                    l.setText("(Chọn NCC)");
                } else if (s.getId() != null && s.getId() == -1) {
                    l.setText("(Chọn NCC)");  // placeholder
                } else {
                    l.setText(s.getName());
                }
                return l;
            }
        });
    }

    private void loadProducts() {
        cbProduct.removeAllItems();
        List<Product> list = productService.search("");
        for (Product p : list) cbProduct.addItem(p);
        cbProduct.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) l.setText("(Chọn sản phẩm)");
                else {
                    Product p = (Product) value;
                    l.setText(p.getSku() + " - " + p.getName() + " (tồn=" + reportService.getOnHand(p.getId()) + ")");
                }
                return l;
            }
        });
    }

    private void addLine() {
        Product p = (Product) cbProduct.getSelectedItem();
        if (p == null) { JOptionPane.showMessageDialog(this, "Chọn sản phẩm."); return; }
        int qty;
        BigDecimal price;
        try { qty = Integer.parseInt(txtQty.getText().trim()); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Số lượng không hợp lệ."); return; }
        try { price = new BigDecimal(txtPrice.getText().trim().replace(",", "")); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Đơn giá không hợp lệ."); return; }
        if (qty <= 0) { JOptionPane.showMessageDialog(this, "Số lượng phải > 0."); return; }

        details.add(new StockTransactionDetail(p, qty, price));
        reloadTable();
    }

    private void removeLine() {
        int row = detailTable.getSelectedRow();
        if (row < 0) return;
        int modelRow = detailTable.convertRowIndexToModel(row);
        details.remove(modelRow);
        reloadTable();
    }

    private void reloadTable() {
        DefaultTableModel m = TableUtils.modelOf(detailTable);
        m.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < details.size(); i++) {
            StockTransactionDetail d = details.get(i);
            BigDecimal lineTotal = d.getLineTotal();
            total = total.add(lineTotal);
            m.addRow(new Object[]{
                    i + 1, d.getProduct().getSku(), d.getProduct().getName(),
                    d.getQuantity(), d.getUnitPrice(), lineTotal
            });
        }
        m.addRow(new Object[]{
                "", "", "TỔNG CỘNG", "", "", total
        });
        // format tiền cho cột 4, 5
        for (int r = 0; r < m.getRowCount(); r++) {
            Object v4 = m.getValueAt(r, 4);
            Object v5 = m.getValueAt(r, 5);
            if (v4 instanceof BigDecimal) m.setValueAt(MoneyFormat.format((BigDecimal) v4), r, 4);
            if (v5 instanceof BigDecimal) m.setValueAt(MoneyFormat.format((BigDecimal) v5), r, 5);
        }
    }

    private void saveTransaction() {
        if (details.isEmpty()) { JOptionPane.showMessageDialog(this, "Phiếu phải có ít nhất 1 sản phẩm."); return; }
        User u = AuthService.get().getCurrentUser();
        if (u == null) { JOptionPane.showMessageDialog(this, "Phiên đăng nhập đã hết."); return; }

        Supplier sup = (Supplier) cbSupplier.getSelectedItem();
        Integer supId = (sup == null || sup.getId() != null && sup.getId() == -1) ? null : sup.getId();
        try {
            stockService.createImport(u, supId, txtNote.getText().trim(), new ArrayList<>(details));
            JOptionPane.showMessageDialog(this, "Lưu phiếu nhập thành công!");
            details.clear(); reloadTable(); txtNote.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    protected void installDefaultShortcuts() {
        com.utc.warehouse.ui.common.ShortcutManager sc =
                new com.utc.warehouse.ui.common.ShortcutManager(this);
        sc.bind("ctrl ENTER", "addLine", this::addLine,    "Thêm dòng sản phẩm vào phiếu");
        sc.bind("DELETE",     "remove",  this::removeLine, "Xoá dòng đang chọn");
        sc.bind("ctrl S",     "save",    this::saveTransaction, "Lưu phiếu nhập");
    }

    private JButton primaryButton(String t) { return styleButton(t, UIConstants.PRIMARY, Color.WHITE); }
    private JButton dangerButton(String t)  { return styleButton(t, UIConstants.DANGER, Color.WHITE); }
    private JButton styleButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(UIConstants.FONT_NORMAL);
        b.setPreferredSize(new Dimension(140, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
