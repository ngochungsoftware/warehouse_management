package com.utc.warehouse.ui.invoice;

import com.utc.warehouse.entity.Invoice;
import com.utc.warehouse.entity.InvoiceItem;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.service.InvoiceService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.Theme;
import com.utc.warehouse.util.MoneyFormat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Danh sách hóa đơn bán hàng, kèm thao tác:
 *  - Tạo mới (mở InvoiceDialog)
 *  - Xem chi tiết
 *  - Ghi nhận thanh toán (phiếu thu)
 */
public class InvoicePanel extends BasePanel {

    private final InvoiceService service = new InvoiceService();
    private final Object[] COLS = {"Mã HĐ", "Ngày", "Khách hàng", "Tổng tiền", "Đã thu", "Còn nợ", "Trạng thái"};
    private JTable table;
    private JComboBox<String> cbStatus;
    private DefaultTableModel model;

    public InvoicePanel() { super("Hóa đơn bán hàng"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        bar.add(new JLabel("Trạng thái:"));
        cbStatus = new JComboBox<>(new String[]{"Tất cả", "Đã xuất", "Thanh toán 1 phần", "Đã thanh toán", "Đã hủy"});
        bar.add(cbStatus);

        JButton btnNew = primaryButton("+ Tạo hóa đơn");
        JButton btnView = secondaryButton("Xem chi tiết");
        JButton btnPay = secondaryButton("Ghi nhận thu tiền");
        JButton btnRefresh = secondaryButton("Làm mới");
        JButton btnCancel = dangerButton("Hủy hóa đơn");
        JButton btnExport = secondaryButton("Xuất Excel");
        btnNew.setName("btnNew");
        btnView.setName("btnView");
        btnPay.setName("btnPay");
        btnRefresh.setName("btnRefresh");
        btnCancel.setName("btnCancel");
        btnExport.setName("btnExport");
        bar.add(btnNew);
        bar.add(btnView);
        bar.add(btnPay);
        bar.add(btnCancel);
        bar.add(btnExport);
        bar.add(btnRefresh);

        cbStatus.addActionListener(e -> reload());
        btnNew.addActionListener(e -> onNew());
        btnView.addActionListener(e -> onView());
        btnPay.addActionListener(e -> onPay());
        btnCancel.addActionListener(e -> onCancel());
        btnRefresh.addActionListener(e -> reload());
        btnExport.addActionListener(e -> com.utc.warehouse.ui.common.TableExportHelper.exportCurrentTable(table, "hoa_don"));
        return bar;
    }

    @Override
    protected void installDefaultShortcuts() {
        com.utc.warehouse.ui.common.ShortcutManager sc = new com.utc.warehouse.ui.common.ShortcutManager(this);
        sc.bind("ctrl N", "new",     this::onNew,      "Tạo hoá đơn mới");
        sc.bind("ctrl D", "detail",  this::onView,     "Xem chi tiết");
        sc.bind("ctrl P", "pay",     this::onPay,      "Ghi nhận thu tiền");
        sc.bind("F5",     "refresh", this::reload,     "Làm mới");
        sc.bind("ctrl X", "export",  () -> com.utc.warehouse.ui.common.TableExportHelper.exportCurrentTable(table, "hoa_don"), "Xuất Excel");
    }

    @Override
    protected JComponent buildContent() {
        model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = TableUtils.createTable(COLS, new Object[0][]);
        table.setModel(model);
        TableUtils.centerColumn(table, 1);
        TableUtils.centerColumn(table, 6);
        for (int i = 3; i <= 5; i++) TableUtils.rightColumn(table, i);

        // Double-click 1 dòng → mở chi tiết hóa đơn
        com.utc.warehouse.ui.common.TableDoubleClick.installFor(
                table,
                modelRow -> {
                    try {
                        return invoiceAtRow(modelRow);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(
                                SwingUtilities.getWindowAncestor(InvoicePanel.this),
                                "Lỗi tải chi tiết hóa đơn:\n" + ex.getMessage(),
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                },
                inv -> showDetail(inv));

        // Auto-load dữ liệu ngay khi panel hiển thị lên — gọi reload() qua hook reloadDataOnShow().
        return TableUtils.scrollFor(table);
    }

    /** Tra Invoice từ model row index (dùng cho double-click). */
    private Invoice invoiceAtRow(int modelRow) {
        Object codeObj = model.getValueAt(modelRow, 0);
        if (codeObj == null) return null;
        String code = codeObj.toString();

        // Hàng đầu tiên là Code. Tra Invoice qua code bằng query JOIN FETCH để items
        // đã nạp sẵn — nếu dùng allInvoices() thì entity detached và
        // inv.getItems() sẽ ném LazyInitializationException.
        try {
            Invoice inv = service.findByCodeWithDetails(code);
            if (inv != null) return inv;
            // Fallback: bảng đang hiển thị dữ liệu cũ, code không còn trong DB.
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(InvoicePanel.this),
                    "Không tìm thấy hóa đơn có mã '" + code + "' trong dữ liệu.\n" +
                            "Hãy bấm \"Làm mới\" để tải lại danh sách.",
                    "Không tìm thấy", JOptionPane.WARNING_MESSAGE);
            return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(InvoicePanel.this),
                    "Không thể tải chi tiết hóa đơn '" + code + "':\n"
                            + ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public void reload() {
        try {
            List<Invoice> all = service.allInvoicesWithDetails();
            model.setRowCount(0);
            for (Invoice inv : all) {
                if (!matchStatus(inv)) continue;
                model.addRow(new Object[]{
                        inv.getCode(),
                        inv.getIssueDate().toString(),
                        inv.getCustomer() == null ? "(Khách lẻ)" : inv.getCustomer().getName(),
                        MoneyFormat.format(inv.getTotal()),
                        MoneyFormat.format(inv.getPaid()),
                        MoneyFormat.format(inv.getDebt()),
                        statusVi(inv.getStatus().name())
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(InvoicePanel.this),
                    "Lỗi tải danh sách hóa đơn:\n" + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean matchStatus(Invoice inv) {
        int sel = cbStatus.getSelectedIndex();
        return switch (sel) {
            case 1 -> inv.getStatus().name().equals("ISSUED");
            case 2 -> inv.getStatus().name().equals("PARTIAL");
            case 3 -> inv.getStatus().name().equals("PAID");
            case 4 -> inv.getStatus().name().equals("CANCELLED");
            default -> true;
        };
    }

    private String statusVi(String s) {
        return switch (s) {
            case "DRAFT"     -> "Nháp";
            case "ISSUED"    -> "Đã xuất";
            case "PARTIAL"   -> "Thanh toán 1 phần";
            case "PAID"      -> "Đã thanh toán";
            case "CANCELLED" -> "Đã hủy";
            default -> s;
        };
    }

    private Invoice selected() {
        int r = table.getSelectedRow();
        if (r < 0) return null;
        Object codeObj = model.getValueAt(table.convertRowIndexToModel(r), 0);
        if (codeObj == null) return null;
        // Dùng bản JOIN FETCH để items đã nạp sẵn — showDetail đọc inv.getItems().
        return service.findByCodeWithDetails(codeObj.toString());
    }

    private void onNew() {
        InvoiceDialog dlg = new InvoiceDialog(SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
        if (dlg.isSaved()) reload();
    }

    private void onView() {
        Invoice inv = selected();
        if (inv == null) { JOptionPane.showMessageDialog(this, "Chọn 1 hóa đơn."); return; }
        showDetail(inv);
    }

    private void onPay() {
        Invoice inv = selected();
        if (inv == null) { JOptionPane.showMessageDialog(this, "Chọn 1 hóa đơn."); return; }
        if (inv.getStatus().name().equals("PAID") || inv.getStatus().name().equals("CANCELLED")) {
            JOptionPane.showMessageDialog(this, "Hóa đơn này đã " + statusVi(inv.getStatus().name()) + ".");
            return;
        }
        JTextField txtAmount = new JTextField(inv.getDebt().toPlainString());
        JComboBox<com.utc.warehouse.enums.PaymentMethod> cbMethod = new JComboBox<>(
                com.utc.warehouse.enums.PaymentMethod.values());
        JTextField txtNote = new JTextField();
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Số tiền thu:"));
        p.add(txtAmount);
        p.add(new JLabel("Hình thức:"));
        p.add(cbMethod);
        p.add(new JLabel("Ghi chú:"));
        p.add(txtNote);
        int ok = JOptionPane.showConfirmDialog(this, p, "Ghi nhận thu tiền - " + inv.getCode(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            try {
                java.math.BigDecimal amount = new java.math.BigDecimal(txtAmount.getText().trim().replace(",", ""));
                service.createReceipt(AuthService.get().getCurrentUser(), inv.getId(), amount,
                        (com.utc.warehouse.enums.PaymentMethod) cbMethod.getSelectedItem(), txtNote.getText().trim());
                JOptionPane.showMessageDialog(this, "Ghi nhận thu tiền thành công!");
                reload();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onCancel() {
        Invoice inv = selected();
        if (inv == null) { JOptionPane.showMessageDialog(this, "Chọn 1 hóa đơn."); return; }
        if (inv.getStatus().name().equals("CANCELLED")) return;
        if (inv.getPaid().signum() > 0) {
            JOptionPane.showMessageDialog(this, "Hóa đơn đã có thanh toán, không thể hủy."); return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Hủy hóa đơn " + inv.getCode() + " ?",
                "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            try {
                inv.setStatus(com.utc.warehouse.enums.InvoiceStatus.CANCELLED);
                inv.setDebt(java.math.BigDecimal.ZERO);
                // Cập nhật trực tiếp
                com.utc.warehouse.config.HibernateUtil.getEntityManager().close();
                // Cần 1 service riêng để cancel; tạm thời dùng save qua EntityManager
                try (jakarta.persistence.EntityManager em = com.utc.warehouse.config.HibernateUtil.getEntityManager()) {
                    em.getTransaction().begin();
                    em.merge(inv);
                    em.getTransaction().commit();
                }
                JOptionPane.showMessageDialog(this, "Đã hủy hóa đơn.");
                reload();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showDetail(Invoice inv) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Mã HĐ: ").append(inv.getCode()).append('\n');
            sb.append("Ngày: ").append(inv.getIssueDate()).append('\n');
            sb.append("Khách: ").append(inv.getCustomer() == null ? "(Khách lẻ)" : inv.getCustomer().getName()).append('\n');
            sb.append("Trạng thái: ").append(statusVi(inv.getStatus().name())).append("\n\n");

            List<InvoiceItem> items = safeItems(inv);
            if (items == null) {
                sb.append("(Không tải được danh sách sản phẩm của hóa đơn)\n\n");
            } else if (items.isEmpty()) {
                sb.append("(Hóa đơn chưa có sản phẩm)\n\n");
            } else {
                sb.append(String.format("%-6s %-15s %-10s%n", "SL", "Đơn giá", "Thành tiền"));
                for (var it : items) {
                    sb.append(String.format("%-6d %-15s %-10s%n",
                            it.getQuantity(),
                            MoneyFormat.format(it.getUnitPrice()),
                            MoneyFormat.format(it.getLineTotal())));
                }
                sb.append('\n');
            }

            sb.append("Tổng: ").append(MoneyFormat.format(inv.getTotal())).append(" đ");
            sb.append("\nĐã thu: ").append(MoneyFormat.format(inv.getPaid())).append(" đ");
            sb.append("\nCòn nợ: ").append(MoneyFormat.format(inv.getDebt())).append(" đ");
            if (inv.getNote() != null && !inv.getNote().isBlank())
                sb.append("\nGhi chú: ").append(inv.getNote());

            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            ta.setFont(Theme.FONT_NORMAL);
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(InvoicePanel.this),
                    new JScrollPane(ta),
                    "Chi tiết " + inv.getCode(), JOptionPane.PLAIN_MESSAGE);
        } catch (Throwable t) {
            // Nuốt lỗi trong mouse listener sẽ khiến Swing im lặng — phải bắt và báo.
            t.printStackTrace();
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(InvoicePanel.this),
                    "Không xem được chi tiết hóa đơn:\n"
                            + t.getClass().getSimpleName() + "\n" + t.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Lấy items của hóa đơn, an toàn với Hibernate lazy loading.
     * Trả về null nếu không tải được (session đã đóng, detached entity...).
     */
    private List<InvoiceItem> safeItems(Invoice inv) {
        try {
            return inv.getItems();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    private JButton primaryButton(String t) {
        JButton b = new JButton(t);
        b.setBackground(Theme.BRAND); b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_HEADER);
        b.setPreferredSize(new Dimension(140, 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JButton secondaryButton(String t) {
        JButton b = new JButton(t);
        b.setBackground(Theme.ACCENT); b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_NORMAL);
        b.setPreferredSize(new Dimension(120, 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JButton dangerButton(String t) {
        JButton b = new JButton(t);
        b.setBackground(Theme.DANGER); b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_NORMAL);
        b.setPreferredSize(new Dimension(110, 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
