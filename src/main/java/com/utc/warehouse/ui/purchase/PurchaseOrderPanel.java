package com.utc.warehouse.ui.purchase;

import com.utc.warehouse.entity.Product;
import com.utc.warehouse.entity.PurchaseOrder;
import com.utc.warehouse.entity.PurchaseOrderItem;
import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.POStatus;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.service.PartnerService;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.service.PurchaseOrderService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.Theme;
import com.utc.warehouse.util.MoneyFormat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Danh sách đơn đặt hàng mua (PO).
 * Workflow: Tạo DRAFT -> Duyệt (ORDERED) -> Nhận hàng (RECEIVED/PARTIAL) -> Huỷ.
 */
public class PurchaseOrderPanel extends BasePanel {

    private final PurchaseOrderService service = new PurchaseOrderService();
    private final PartnerService partnerService = new PartnerService();
    private final ProductService productService = new ProductService();
    private final Object[] COLS = {"Mã PO", "Ngày tạo", "NCC", "Ngày dự kiến", "Số SP", "Tổng tiền", "Trạng thái"};
    private JTable table;
    private JComboBox<String> cbStatus;
    private DefaultTableModel model;

    public PurchaseOrderPanel() { super("Đơn đặt hàng mua (PO)"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        bar.add(new JLabel("Trạng thái:"));
        cbStatus = new JComboBox<>(new String[]{"Tất cả", "Nháp", "Đã đặt", "Nhận 1 phần", "Đã nhận đủ", "Đã hủy"});
        bar.add(cbStatus);

        JButton btnNew = primaryButton("+ Tạo đơn");
        JButton btnView = secondaryButton("Xem chi tiết");
        JButton btnApprove = secondaryButton("Duyệt →");
        JButton btnReceive = primaryButton("Nhận hàng");
        JButton btnCancel = dangerButton("Hủy đơn");
        JButton btnRefresh = secondaryButton("Làm mới");
        JButton btnExport = secondaryButton("Xuất CSV");
        btnNew.setName("btnNew");
        btnView.setName("btnView");
        btnApprove.setName("btnApprove");
        btnReceive.setName("btnReceive");
        btnCancel.setName("btnCancel");
        btnRefresh.setName("btnRefresh");
        btnExport.setName("btnExport");
        bar.add(btnNew);
        bar.add(btnView);
        bar.add(btnApprove);
        bar.add(btnReceive);
        bar.add(btnCancel);
        bar.add(btnExport);
        bar.add(btnRefresh);

        cbStatus.addActionListener(e -> reload());
        btnNew.addActionListener(e -> onNew());
        btnView.addActionListener(e -> onView());
        btnApprove.addActionListener(e -> onApprove());
        btnReceive.addActionListener(e -> onReceive());
        btnCancel.addActionListener(e -> onCancel());
        btnRefresh.addActionListener(e -> reload());
        btnExport.addActionListener(e -> com.utc.warehouse.ui.common.TableExportHelper.exportCurrentTable(table, "don_dat_hang"));
        return bar;
    }

    @Override
    protected void installDefaultShortcuts() {
        com.utc.warehouse.ui.common.ShortcutManager sc = new com.utc.warehouse.ui.common.ShortcutManager(this);
        sc.bind("ctrl N", "new",     this::onNew,     "Tạo PO");
        sc.bind("ctrl R", "receive", this::onReceive, "Nhận hàng");
        sc.bind("F5",     "refresh", this::reload,    "Làm mới");
        sc.bind("ctrl X", "export",  () -> com.utc.warehouse.ui.common.TableExportHelper.exportCurrentTable(table, "don_dat_hang"), "Xuất CSV");
    }

    @Override
    protected JComponent buildContent() {
        model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = TableUtils.createTable(COLS, new Object[0][]);
        table.setModel(model);
        TableUtils.centerColumn(table, 1);
        TableUtils.centerColumn(table, 4);
        TableUtils.centerColumn(table, 6);
        TableUtils.rightColumn(table, 5);
        return TableUtils.scrollFor(table);
    }

    public void reload() {
        try {
            List<PurchaseOrder> list = service.all();
            model.setRowCount(0);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            int sel = cbStatus.getSelectedIndex();
            for (PurchaseOrder po : list) {
                if (sel > 0 && po.getStatus().ordinal() != sel - 1) continue;
                // items đã được JOIN FETCH ở DAO → an toàn đọc ngoài session
                int itemCount = po.getItems() == null ? 0 : po.getItems().size();
                model.addRow(new Object[]{
                        po.getCode(),
                        po.getCreatedAt() == null ? "" : po.getCreatedAt().toLocalDate().format(fmt),
                        po.getSupplier() == null ? "(không)" : po.getSupplier().getName(),
                        po.getExpectedDate() == null ? "" : po.getExpectedDate().format(fmt),
                        itemCount,
                        MoneyFormat.format(po.getTotal()),
                        statusVi(po.getStatus())
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String statusVi(POStatus s) {
        return switch (s) {
            case DRAFT     -> "Nháp";
            case ORDERED   -> "Đã đặt";
            case PARTIAL   -> "Nhận 1 phần";
            case RECEIVED  -> "Đã nhận đủ";
            case CANCELLED -> "Đã hủy";
        };
    }

    private PurchaseOrder selected() {
        int r = table.getSelectedRow();
        if (r < 0) return null;
        int modelRow = table.convertRowIndexToModel(r);
        Object codeObj = model.getValueAt(modelRow, 0);
        if (codeObj == null) return null;
        String code = codeObj.toString();
        // Lấy trực tiếp từ bảng (đã fetch items ở reload) — tránh query lại toàn bộ.
        for (PurchaseOrder p : service.all()) {
            if (code.equals(p.getCode())) return p;
        }
        return null;
    }

    private void onNew() {
        PurchaseOrderDialog dlg = new PurchaseOrderDialog(SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
        if (dlg.isSaved()) reload();
    }

    private void onView() {
        PurchaseOrder po = selected();
        if (po == null) { JOptionPane.showMessageDialog(this, "Chọn 1 đơn."); return; }
        showDetail(po);
    }

    private void onApprove() {
        PurchaseOrder po = selected();
        if (po == null) { JOptionPane.showMessageDialog(this, "Chọn 1 đơn."); return; }
        if (po.getStatus() != POStatus.DRAFT) {
            JOptionPane.showMessageDialog(this, "Chỉ duyệt được đơn ở trạng thái Nháp."); return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Duyệt đơn " + po.getCode() + " ?", "Xác nhận",
                JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            try {
                service.approve(AuthService.get().getCurrentUser(), po.getId());
                JOptionPane.showMessageDialog(this, "Đã duyệt đơn " + po.getCode());
                reload();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onReceive() {
        PurchaseOrder po = selected();
        if (po == null) { JOptionPane.showMessageDialog(this, "Chọn 1 đơn."); return; }
        if (po.getStatus() != POStatus.ORDERED && po.getStatus() != POStatus.PARTIAL) {
            JOptionPane.showMessageDialog(this, "Chỉ nhận hàng cho đơn Đã đặt / Nhận 1 phần."); return;
        }
        // Bảo vệ: nếu items null → fetch lại từ DAO
        if (po.getItems() == null) {
            try {
                com.utc.warehouse.dao.PurchaseOrderDAO dao = new com.utc.warehouse.dao.PurchaseOrderDAO();
                PurchaseOrder fresh = po.getId() == null ? null : dao.findByIdWithItems(po.getId());
                if (fresh != null) po = fresh;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không tải được chi tiết đơn: " + ex.getMessage());
                return;
            }
        }
        // Kiểm tra đã nhận gì chưa
        final PurchaseOrder finalPo = po;
        int alreadyReceived = po.getItems().stream().mapToInt(it -> {
            Integer rq = it.getReceivedQty(); return rq == null ? 0 : rq;
        }).sum();
        int totalOrdered = po.getItems().stream().mapToInt(it -> {
            Integer oq = it.getOrderedQty(); return oq == null ? 0 : oq;
        }).sum();
        if (alreadyReceived >= totalOrdered) {
            JOptionPane.showMessageDialog(this, "Đơn đã nhận đủ."); return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Nhận hàng cho " + finalPo.getCode() + " ?\nHệ thống sẽ tự tạo phiếu nhập kho và cập nhật giá vốn.",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            try {
                PurchaseOrder updated = service.receive(AuthService.get().getCurrentUser(), finalPo.getId(), true);
                JOptionPane.showMessageDialog(this, "Đã nhận hàng. Trạng thái: " + statusVi(updated.getStatus()));
                reload();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onCancel() {
        PurchaseOrder po = selected();
        if (po == null) { JOptionPane.showMessageDialog(this, "Chọn 1 đơn."); return; }
        if (po.getStatus() == POStatus.RECEIVED || po.getStatus() == POStatus.CANCELLED) {
            JOptionPane.showMessageDialog(this, "Không thể hủy đơn " + statusVi(po.getStatus()) + "."); return;
        }
        String reason = JOptionPane.showInputDialog(this, "Lý do hủy:");
        if (reason == null) return;
        try {
            service.cancel(AuthService.get().getCurrentUser(), po.getId(), reason);
            JOptionPane.showMessageDialog(this, "Đã hủy đơn " + po.getCode());
            reload();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showDetail(PurchaseOrder po) {
        // Bảo vệ: nếu items null (PO không có items hoặc chưa fetch) → fallback findByIdWithItems
        if (po.getItems() == null) {
            try {
                com.utc.warehouse.dao.PurchaseOrderDAO dao = new com.utc.warehouse.dao.PurchaseOrderDAO();
                PurchaseOrder fresh = po.getId() == null ? null : dao.findByIdWithItems(po.getId());
                if (fresh != null) po = fresh;
            } catch (Exception ignored) { }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Mã PO: ").append(po.getCode() == null ? "—" : po.getCode()).append('\n');
        sb.append("NCC: ").append(po.getSupplier() == null ? "(không)" : po.getSupplier().getName()).append('\n');
        sb.append("Trạng thái: ").append(statusVi(po.getStatus())).append('\n');
        sb.append("Ngày tạo: ").append(po.getCreatedAt() == null ? "—" : po.getCreatedAt().toLocalDate()).append('\n');
        if (po.getExpectedDate() != null) sb.append("Dự kiến giao: ").append(po.getExpectedDate()).append('\n');
        if (po.getReceivedTransactionId() != null)
            sb.append("Phiếu nhập: #").append(po.getReceivedTransactionId()).append('\n');
        if (po.getNote() != null && !po.getNote().isBlank()) sb.append("Ghi chú: ").append(po.getNote()).append('\n');
        sb.append('\n');
        sb.append(String.format("%-6s %-12s %-25s %6s %6s %12s%n",
                "ID", "SKU", "Tên SP", "Đặt", "Đã nhận", "Thành tiền"));
        if (po.getItems() != null) {
            for (PurchaseOrderItem it : po.getItems()) {
                String name = it.getProduct() == null ? "(đã xoá)" : it.getProduct().getName();
                String sku  = it.getProduct() == null ? "—" : it.getProduct().getSku();
                Integer pid = it.getProduct() == null ? null : it.getProduct().getId();
                sb.append(String.format("%-6s %-12s %-25s %6d %6d %12s%n",
                        pid == null ? "—" : pid.toString(),
                        truncate(sku, 12),
                        truncate(name, 25),
                        it.getOrderedQty() == null ? 0 : it.getOrderedQty(),
                        it.getReceivedQty() == null ? 0 : it.getReceivedQty(),
                        MoneyFormat.format(it.getLineTotal())));
            }
        }
        sb.append('\n').append("TỔNG: ").append(MoneyFormat.format(po.getTotal())).append(" đ");
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setFont(Theme.FONT_NORMAL);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                "Chi tiết " + (po.getCode() == null ? "" : po.getCode()), JOptionPane.PLAIN_MESSAGE);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /* ============== helpers ============== */
    private JButton primaryButton(String t) {
        JButton b = new JButton(t);
        b.setBackground(Theme.BRAND); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setFont(Theme.FONT_HEADER);
        b.setPreferredSize(new Dimension(120, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JButton secondaryButton(String t) {
        JButton b = new JButton(t);
        b.setBackground(Theme.ACCENT); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setFont(Theme.FONT_NORMAL);
        b.setPreferredSize(new Dimension(110, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JButton dangerButton(String t) {
        JButton b = new JButton(t);
        b.setBackground(Theme.DANGER); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setFont(Theme.FONT_NORMAL);
        b.setPreferredSize(new Dimension(90, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /* ============== Dialog tạo PO ============== */
    class PurchaseOrderDialog extends JDialog {
        private final JComboBox<Supplier> cbSupplier = new JComboBox<>();
        private final JTextField txtExpected = new JTextField(LocalDate.now().plusDays(7).toString(), 10);
        private final JTextField txtNote = new JTextField();
        private final JComboBox<Product> cbProduct = new JComboBox<>();
        private final JTextField txtQty = new JTextField("1");
        private final JTextField txtPrice = new JTextField("0");
        private final JTable lineTable;
        private final DefaultTableModel lineModel;
        private final List<PurchaseOrderItem> drafts = new ArrayList<>();
        private final JLabel lblTotal = new JLabel("0 đ");
        private boolean saved = false;

        PurchaseOrderDialog(Window owner) {
            super(owner, "Tạo đơn đặt hàng mua", ModalityType.APPLICATION_MODAL);
            setSize(900, 600);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout());

            // Header info
            cbSupplier.addItem(null);
            for (Supplier s : partnerService.allSuppliers()) cbSupplier.addItem(s);
            cbSupplier.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                        boolean isSelected, boolean cellHasFocus) {
                    JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    l.setText(value == null ? "(Chọn NCC)" : ((Supplier) value).getName());
                    return l;
                }
            });
            for (Product p : productService.search("")) {
                if (Boolean.FALSE.equals(p.getActive())) continue;
                cbProduct.addItem(p);
            }
            cbProduct.setRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                        boolean isSelected, boolean cellHasFocus) {
                    JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value == null) l.setText("(Chọn SP)");
                    else {
                        Product p = (Product) value;
                        l.setText(p.getSku() + " - " + p.getName()
                                + " (giá vốn hiện tại=" + MoneyFormat.format(p.getCostPrice()) + ")");
                    }
                    return l;
                }
            });
            cbProduct.addActionListener(e -> {
                Product p = (Product) cbProduct.getSelectedItem();
                if (p != null && p.getCostPrice() != null) {
                    txtPrice.setText(p.getCostPrice().toPlainString());
                }
            });

            JPanel info = new JPanel(new GridBagLayout());
            info.setBackground(Color.WHITE);
            info.setBorder(BorderFactory.createTitledBorder("Thông tin đơn"));
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(6, 6, 6, 6);
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0; c.gridy = 0; info.add(new JLabel("Nhà cung cấp:"), c);
            c.gridx = 1; c.weightx = 1; info.add(cbSupplier, c);
            c.gridx = 0; c.gridy = 1; c.weightx = 0; info.add(new JLabel("Ngày dự kiến:"), c);
            c.gridx = 1; info.add(txtExpected, c);
            c.gridx = 0; c.gridy = 2; info.add(new JLabel("Ghi chú:"), c);
            c.gridx = 1; info.add(txtNote, c);

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
            m.gridx = 0; m.gridy = 2; addPanel.add(new JLabel("Đơn giá dự kiến:"), m);
            m.gridx = 1; addPanel.add(txtPrice, m);
            m.gridx = 0; m.gridy = 3; m.gridwidth = 2; m.anchor = GridBagConstraints.CENTER;
            JButton btnAdd = new JButton("+ Thêm dòng");
            btnAdd.setBackground(Theme.BRAND); btnAdd.setForeground(Color.WHITE);
            btnAdd.setFocusPainted(false);
            btnAdd.setPreferredSize(new Dimension(150, 32));
            btnAdd.addActionListener(e -> addLine());
            addPanel.add(btnAdd, m);

            JPanel left = new JPanel();
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            left.setBackground(Color.WHITE);
            left.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
            left.setPreferredSize(new Dimension(360, 0));
            left.add(info);
            left.add(Box.createVerticalStrut(8));
            left.add(addPanel);

            // Bảng
            Object[] cols = {"#", "SKU", "Tên SP", "SL đặt", "Đơn giá", "Thành tiền"};
            lineModel = new DefaultTableModel(cols, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            lineTable = new JTable(lineModel);
            lineTable.setRowHeight(28);
            lineTable.setFont(Theme.FONT_NORMAL);
            lineTable.getTableHeader().setFont(Theme.FONT_HEADER);
            lineTable.getTableHeader().setBackground(Theme.BRAND);
            lineTable.getTableHeader().setForeground(Color.WHITE);

            JButton btnRemove = new JButton("Xóa dòng chọn");
            btnRemove.setBackground(Theme.DANGER); btnRemove.setForeground(Color.WHITE);
            btnRemove.setFocusPainted(false);
            btnRemove.addActionListener(e -> removeLine());

            JPanel right = new JPanel(new BorderLayout(8, 8));
            right.setBackground(Color.WHITE);
            right.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
            right.add(new JScrollPane(lineTable), BorderLayout.CENTER);

            JPanel south = new JPanel(new BorderLayout());
            south.setOpaque(false);
            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            btnRow.setOpaque(false);
            btnRow.add(btnRemove);
            south.add(btnRow, BorderLayout.NORTH);

            lblTotal.setFont(Theme.FONT_TITLE);
            lblTotal.setForeground(Theme.BRAND);
            lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
            JPanel tot = new JPanel(new BorderLayout());
            tot.setOpaque(false);
            tot.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            tot.add(lblTotal, BorderLayout.CENTER);
            south.add(tot, BorderLayout.SOUTH);
            right.add(south, BorderLayout.SOUTH);

            JButton btnSave = new JButton("Lưu đơn (DRAFT)");
            btnSave.setBackground(Theme.BRAND); btnSave.setForeground(Color.WHITE);
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
        }

        private void addLine() {
            try {
                Product p = (Product) cbProduct.getSelectedItem();
                if (p == null) { JOptionPane.showMessageDialog(this, "Chọn SP."); return; }
                int qty = Integer.parseInt(txtQty.getText().trim());
                BigDecimal price = new BigDecimal(txtPrice.getText().trim().replace(",", ""));
                if (qty <= 0) { JOptionPane.showMessageDialog(this, "SL phải > 0."); return; }
                PurchaseOrderItem item = new PurchaseOrderItem(p, qty, price);
                drafts.add(item);
                lineModel.addRow(new Object[]{
                        drafts.size(), p.getSku(), p.getName(), qty,
                        MoneyFormat.format(price), MoneyFormat.format(item.getLineTotal())
                });
                BigDecimal sum = drafts.stream().map(PurchaseOrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
                lblTotal.setText(MoneyFormat.format(sum) + " đ");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Số không hợp lệ.");
            }
        }

        private void removeLine() {
            int r = lineTable.getSelectedRow();
            if (r < 0) return;
            int mr = lineTable.convertRowIndexToModel(r);
            drafts.remove(mr);
            lineModel.removeRow(mr);
            BigDecimal sum = drafts.stream().map(PurchaseOrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            lblTotal.setText(MoneyFormat.format(sum) + " đ");
        }

        private void onSave() {
            if (drafts.isEmpty()) { JOptionPane.showMessageDialog(this, "Phải có ít nhất 1 SP."); return; }
            try {
                Supplier sup = (Supplier) cbSupplier.getSelectedItem();
                LocalDate exp = txtExpected.getText().trim().isBlank() ? null : LocalDate.parse(txtExpected.getText().trim());
                PurchaseOrder po = service.create(AuthService.get().getCurrentUser(),
                        sup == null ? null : sup.getId(), txtNote.getText().trim(), exp, drafts);
                JOptionPane.showMessageDialog(this, "Đã tạo đơn " + po.getCode() + " (DRAFT).");
                saved = true;
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }

        public boolean isSaved() { return saved; }
    }
}
