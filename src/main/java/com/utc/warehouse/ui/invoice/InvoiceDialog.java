package com.utc.warehouse.ui.invoice;

import com.utc.warehouse.entity.Customer;
import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.service.InvoiceService;
import com.utc.warehouse.service.PartnerService;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.service.ReportService;
import com.utc.warehouse.ui.common.Theme;
import com.utc.warehouse.util.MoneyFormat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog tạo hóa đơn bán hàng.
 *  - Chọn khách hàng
 *  - Thêm sản phẩm (tự lấy giá bán + giá vốn từ Product)
 *  - Tính VAT, chiết khấu từng dòng
 *  - Tạo phiếu xuất kho + hóa đơn cùng lúc
 */
public class InvoiceDialog extends JDialog {

    private final InvoiceService invoiceService = new InvoiceService();
    private final ProductService productService = new ProductService();
    private final PartnerService partnerService = new PartnerService();
    private final ReportService reportService = new ReportService();

    private final JComboBox<Customer> cbCustomer = new JComboBox<>();
    private final JTextField txtNote = new JTextField();
    private final JComboBox<Product> cbProduct = new JComboBox<>();
    private final JTextField txtQty = new JTextField("1");
    private final JTextField txtUnitPrice = new JTextField("0");
    private final JTextField txtVat = new JTextField("0");
    private final JTextField txtDiscount = new JTextField("0");
    private final JTable detailTable;
    private final DefaultTableModel detailModel;
    private final List<InvoiceService.InvoiceItemDraft> drafts = new ArrayList<>();
    private final JLabel lblTotal = new JLabel("0 đ");
    private final JLabel lblCogs  = new JLabel("0 đ");
    private final JLabel lblProfit = new JLabel("0 đ");

    private boolean saved = false;

    public InvoiceDialog(Window owner) {
        super(owner, "Tạo hóa đơn bán hàng", ModalityType.APPLICATION_MODAL);
        setSize(1000, 640);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // Khách
        cbCustomer.addItem(null);
        for (Customer c : partnerService.allCustomers()) cbCustomer.addItem(c);
        cbCustomer.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setText(value == null ? "(Khách lẻ)" : ((Customer) value).getName());
                return l;
            }
        });

        // Sản phẩm
        for (Product p : productService.search("")) {
            if (Boolean.FALSE.equals(p.getActive())) continue;
            cbProduct.addItem(p);
        }
        cbProduct.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) l.setText("(Chọn sản phẩm)");
                else {
                    Product p = (Product) value;
                    l.setText(p.getSku() + " - " + p.getName() +
                            " (tồn=" + reportService.getOnHand(p.getId()) + ", giá=" +
                            MoneyFormat.format(p.getSalePrice()) + ")");
                }
                return l;
            }
        });
        cbProduct.addActionListener(e -> {
            Product p = (Product) cbProduct.getSelectedItem();
            if (p != null) {
                txtUnitPrice.setText(p.getSalePrice().toPlainString());
                txtVat.setText(p.getVatRate() == null ? "0" : p.getVatRate().toPlainString());
            }
        });

        // --- Layout: trái = thông tin + thêm sp, phải = bảng + tổng ---
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(Color.WHITE);
        left.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        left.setPreferredSize(new Dimension(380, 0));

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin hóa đơn"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; infoPanel.add(new JLabel("Khách hàng:"), c);
        c.gridx = 1; c.weightx = 1; infoPanel.add(cbCustomer, c);
        c.gridx = 0; c.gridy = 1; c.weightx = 0; infoPanel.add(new JLabel("Ghi chú:"), c);
        c.gridx = 1; infoPanel.add(txtNote, c);

        JPanel addPanel = new JPanel(new GridBagLayout());
        addPanel.setBackground(Color.WHITE);
        addPanel.setBorder(BorderFactory.createTitledBorder("Thêm sản phẩm"));
        GridBagConstraints m = new GridBagConstraints();
        m.insets = new Insets(6, 6, 6, 6);
        m.anchor = GridBagConstraints.WEST;
        m.fill = GridBagConstraints.HORIZONTAL;
        m.gridx = 0; m.gridy = 0; addPanel.add(new JLabel("Sản phẩm:"), m);
        m.gridx = 1; m.weightx = 1; addPanel.add(cbProduct, m);
        m.gridx = 0; m.gridy = 1; m.weightx = 0; addPanel.add(new JLabel("Số lượng:"), m);
        m.gridx = 1; addPanel.add(txtQty, m);
        m.gridx = 0; m.gridy = 2; addPanel.add(new JLabel("Đơn giá (đ):"), m);
        m.gridx = 1; addPanel.add(txtUnitPrice, m);
        m.gridx = 0; m.gridy = 3; addPanel.add(new JLabel("VAT %:"), m);
        m.gridx = 1; addPanel.add(txtVat, m);
        m.gridx = 0; m.gridy = 4; addPanel.add(new JLabel("Chiết khấu (đ):"), m);
        m.gridx = 1; addPanel.add(txtDiscount, m);
        m.gridx = 0; m.gridy = 5; m.gridwidth = 2; m.anchor = GridBagConstraints.CENTER;
        JButton btnAddLine = new JButton("+ Thêm dòng");
        btnAddLine.setBackground(Theme.BRAND); btnAddLine.setForeground(Color.WHITE);
        btnAddLine.setFocusPainted(false);
        btnAddLine.setPreferredSize(new Dimension(150, 32));
        btnAddLine.addActionListener(e -> addLine());
        addPanel.add(btnAddLine, m);

        left.add(infoPanel);
        left.add(Box.createVerticalStrut(8));
        left.add(addPanel);

        // --- Bảng chi tiết ---
        Object[] cols = {"#", "SKU", "Tên SP", "SL", "Đơn giá", "VAT%", "CK", "Thành tiền"};
        detailModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        detailTable = new JTable(detailModel);
        detailTable.setRowHeight(28);
        detailTable.setFont(Theme.FONT_NORMAL);
        detailTable.getTableHeader().setFont(Theme.FONT_HEADER);
        detailTable.getTableHeader().setBackground(Theme.BRAND);
        detailTable.getTableHeader().setForeground(Color.WHITE);

        JButton btnRemove = new JButton("Xóa dòng chọn");
        btnRemove.setBackground(Theme.DANGER); btnRemove.setForeground(Color.WHITE);
        btnRemove.setFocusPainted(false);
        btnRemove.addActionListener(e -> removeLine());

        JPanel right = new JPanel(new BorderLayout(8, 8));
        right.setBackground(Color.WHITE);
        right.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        right.add(new JScrollPane(detailTable), BorderLayout.CENTER);

        JPanel southRight = new JPanel(new BorderLayout());
        southRight.setOpaque(false);
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.add(btnRemove);
        southRight.add(btnRow, BorderLayout.NORTH);

        // Tổng
        JPanel summary = new JPanel(new GridLayout(3, 2, 8, 4));
        summary.setOpaque(false);
        summary.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        summary.add(rightLabel("Tổng tiền hàng (đã VAT + CK):"));
        JLabel lblTotalWrap = new JLabel("0 đ");
        lblTotalWrap.setFont(Theme.FONT_TITLE);
        lblTotalWrap.setForeground(Theme.BRAND);
        summary.add(lblTotalWrap);
        lblTotal.setFont(Theme.FONT_TITLE);
        lblTotal.setForeground(Theme.BRAND);
        lblTotal.setText("0 đ");
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        // Replace bằng label trên (giữ để update)
        summary.removeAll();
        summary.setLayout(new GridLayout(3, 2, 8, 6));
        summary.add(rightLabel("Giá vốn (COGS):"));
        JLabel cogs = rightLabel("");
        cogs.setName("cogs");
        cogs.setForeground(Theme.WARNING);
        summary.add(cogs);
        summary.add(rightLabel("Lợi nhuận gộp:"));
        JLabel profit = rightLabel("");
        profit.setName("profit");
        profit.setForeground(Theme.SUCCESS);
        summary.add(profit);
        summary.add(rightLabel("Tổng hóa đơn:"));
        JLabel total = rightLabel("");
        total.setName("total");
        total.setFont(Theme.FONT_TITLE);
        total.setForeground(Theme.BRAND);
        summary.add(total);
        southRight.add(summary, BorderLayout.SOUTH);
        // bind by name for quick update
        Runnable refreshTotals = () -> {
            BigDecimal cogsSum = BigDecimal.ZERO, profitSum = BigDecimal.ZERO, totalSum = BigDecimal.ZERO;
            for (InvoiceService.InvoiceItemDraft d : drafts) {
                Product p = productService.search("").stream().filter(x -> x.getId().equals(d.productId))
                        .findFirst().orElse(null);
                BigDecimal cost = p == null ? BigDecimal.ZERO : (p.getCostPrice() == null ? BigDecimal.ZERO : p.getCostPrice());
                BigDecimal lineCogs = cost.multiply(BigDecimal.valueOf(d.quantity));
                BigDecimal lineBase = d.unitPrice.multiply(BigDecimal.valueOf(d.quantity));
                BigDecimal lineVat = lineBase.multiply(d.vatRate == null ? BigDecimal.ZERO : d.vatRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal lineTotal = lineBase.add(lineVat).subtract(d.discount == null ? BigDecimal.ZERO : d.discount);
                cogsSum = cogsSum.add(lineCogs);
                profitSum = profitSum.add(lineTotal.subtract(lineCogs));
                totalSum = totalSum.add(lineTotal);
            }
            cogs.setText(MoneyFormat.format(cogsSum));
            profit.setText(MoneyFormat.format(profitSum));
            total.setText(MoneyFormat.format(totalSum));
        };
        // wire update when adding/removing
        addPanel.putClientProperty("refreshTotals", refreshTotals);

        right.add(southRight, BorderLayout.SOUTH);

        // Nút lưu / hủy
        JButton btnSave = new JButton("Lưu & Xuất hóa đơn");
        btnSave.setBackground(Theme.BRAND); btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setFont(Theme.FONT_HEADER);
        btnSave.setPreferredSize(new Dimension(200, 38));
        btnSave.addActionListener(e -> onSave());
        JButton btnCancel = new JButton("Hủy");
        btnCancel.setPreferredSize(new Dimension(80, 38));
        btnCancel.addActionListener(e -> dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        footer.add(btnCancel);
        footer.add(btnSave);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        // Lưu refreshTotals để dùng sau
        detailTable.putClientProperty("refreshTotals", refreshTotals);
    }

    private static JLabel rightLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_HEADER);
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        return l;
    }

    private void addLine() {
        try {
            Product p = (Product) cbProduct.getSelectedItem();
            if (p == null) { JOptionPane.showMessageDialog(this, "Chọn sản phẩm."); return; }
            int qty = Integer.parseInt(txtQty.getText().trim());
            BigDecimal price = new BigDecimal(txtUnitPrice.getText().trim().replace(",", ""));
            BigDecimal vat = new BigDecimal(txtVat.getText().trim().replace(",", ""));
            BigDecimal disc = new BigDecimal(txtDiscount.getText().trim().replace(",", ""));
            if (qty <= 0) { JOptionPane.showMessageDialog(this, "Số lượng phải > 0."); return; }
            int onHand = reportService.getOnHand(p.getId());
            int reservedInList = drafts.stream()
                    .filter(d -> d.productId.equals(p.getId()))
                    .mapToInt(d -> d.quantity).sum();
            if (qty + reservedInList > onHand) {
                JOptionPane.showMessageDialog(this,
                        String.format("Không đủ tồn cho '%s'.\nTồn=%d, đã thêm=%d, yêu cầu=%d",
                                p.getName(), onHand, reservedInList, qty),
                        "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            InvoiceService.InvoiceItemDraft d = new InvoiceService.InvoiceItemDraft();
            d.productId = p.getId();
            d.quantity = qty;
            d.unitPrice = price;
            d.vatRate = vat;
            d.discount = disc;
            drafts.add(d);

            BigDecimal lineBase = price.multiply(BigDecimal.valueOf(qty));
            BigDecimal lineVat = lineBase.multiply(vat).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineBase.add(lineVat).subtract(disc);
            detailModel.addRow(new Object[]{
                    drafts.size(), p.getSku(), p.getName(), qty,
                    MoneyFormat.format(price), vat.toPlainString() + "%",
                    MoneyFormat.format(disc), MoneyFormat.format(lineTotal)
            });
            Runnable r = (Runnable) detailTable.getClientProperty("refreshTotals");
            if (r != null) r.run();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số không hợp lệ.");
        }
    }

    private void removeLine() {
        int r = detailTable.getSelectedRow();
        if (r < 0) return;
        int modelRow = detailTable.convertRowIndexToModel(r);
        drafts.remove(modelRow);
        detailModel.removeRow(modelRow);
        Runnable fn = (Runnable) detailTable.getClientProperty("refreshTotals");
        if (fn != null) fn.run();
    }

    private void onSave() {
        if (drafts.isEmpty()) { JOptionPane.showMessageDialog(this, "Hóa đơn phải có ít nhất 1 sản phẩm."); return; }
        try {
            Customer cust = (Customer) cbCustomer.getSelectedItem();
            var inv = invoiceService.createFromExport(
                    AuthService.get().getCurrentUser(),
                    cust == null ? null : cust.getId(),
                    txtNote.getText().trim(),
                    drafts);
            JOptionPane.showMessageDialog(this,
                    "Đã tạo hóa đơn " + inv.getCode() + "!\nTổng: " + MoneyFormat.format(inv.getTotal()));
            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() { return saved; }
}
