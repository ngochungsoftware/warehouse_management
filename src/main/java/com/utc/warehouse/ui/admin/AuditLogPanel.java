package com.utc.warehouse.ui.admin;

import com.utc.warehouse.entity.AuditLog;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.AuditAction;
import com.utc.warehouse.service.AuditLogger;
import com.utc.warehouse.service.UserService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.Theme;
import com.utc.warehouse.util.DateFormatUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Lịch sử thao tác (Audit Log). Chỉ ADMIN thấy trong menu.
 * Lọc theo user, hành động, loại đối tượng, khoảng thời gian.
 */
public class AuditLogPanel extends BasePanel {

    private final AuditLogger service = new AuditLogger();
    private final UserService userService = new UserService();
    private JTable table;
    private DefaultTableModel model;
    private final Object[] COLS = {"#", "Thời gian", "User", "Hành động", "Loại", "Mã", "Chi tiết"};
    private JTextField txtFrom, txtTo, txtKeyword;
    private JComboBox<String> cbUser, cbAction, cbEntity;

    public AuditLogPanel() { super("Lịch sử thao tác (Audit Log)"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        bar.setOpaque(false);

        bar.add(new JLabel("Từ:"));
        txtFrom = new JTextField(LocalDate.now().minusDays(7).toString(), 10);
        bar.add(txtFrom);
        bar.add(new JLabel("Đến:"));
        txtTo = new JTextField(LocalDate.now().toString(), 10);
        bar.add(txtTo);

        bar.add(new JLabel("User:"));
        cbUser = new JComboBox<>();
        cbUser.addItem("Tất cả");
        for (User u : userService.all()) cbUser.addItem(u.getUsername());
        bar.add(cbUser);

        bar.add(new JLabel("Hành động:"));
        cbAction = new JComboBox<>();
        cbAction.addItem("Tất cả");
        for (AuditAction a : AuditAction.values()) cbAction.addItem(a.name());
        bar.add(cbAction);

        bar.add(new JLabel("Loại:"));
        cbEntity = new JComboBox<>(new String[]{"Tất cả", "User", "Product", "Category",
                "Supplier", "Customer", "StockTransaction", "Invoice", "Payment", "PurchaseOrder"});
        bar.add(cbEntity);

        bar.add(new JLabel("Từ khóa:"));
        txtKeyword = new JTextField(12);
        bar.add(txtKeyword);

        JButton btnApply = primaryButton("Lọc");
        JButton btnRefresh = secondaryButton("Làm mới");
        btnApply.setName("btnApply");
        btnRefresh.setName("btnRefresh");
        btnApply.addActionListener(e -> reload());
        btnRefresh.addActionListener(e -> reload());
        bar.add(btnApply);
        bar.add(btnRefresh);
        return bar;
    }

    @Override
    protected JComponent buildContent() {
        model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = TableUtils.createTable(COLS, new Object[0][]);
        table.setModel(model);
        TableUtils.centerColumn(table, 0);
        TableUtils.centerColumn(table, 2);
        TableUtils.centerColumn(table, 3);
        TableUtils.centerColumn(table, 4);
        return TableUtils.scrollFor(table);
    }

    public void reload() {
        try {
            LocalDate from = txtFrom.getText().trim().isBlank() ? null : LocalDate.parse(txtFrom.getText().trim());
            LocalDate to   = txtTo.getText().trim().isBlank()   ? null : LocalDate.parse(txtTo.getText().trim());
            String userSel = (String) cbUser.getSelectedItem();
            Integer userId = null;
            if (!"Tất cả".equals(userSel)) {
                User u = userService.all().stream().filter(x -> x.getUsername().equals(userSel)).findFirst().orElse(null);
                if (u != null) userId = u.getId();
            }
            String actSel = (String) cbAction.getSelectedItem();
            AuditAction action = "Tất cả".equals(actSel) ? null : AuditAction.valueOf(actSel);
            String entSel = (String) cbEntity.getSelectedItem();
            String entityType = "Tất cả".equals(entSel) ? null : entSel;
            String kw = txtKeyword.getText().trim();

            List<AuditLog> logs = service.search(userId, action, entityType, kw, from, to, 500);
            model.setRowCount(0);
            int idx = 1;
            for (AuditLog a : logs) {
                model.addRow(new Object[]{
                        idx++,
                        DateFormatUtil.format(a.getCreatedAt()),
                        a.getUsername(),
                        actionVi(a.getAction()),
                        a.getEntityType(),
                        a.getEntityCode(),
                        a.getDetails()
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String actionVi(AuditAction a) {
        return switch (a) {
            case LOGIN           -> "Đăng nhập";
            case LOGOUT          -> "Đăng xuất";
            case CREATE          -> "Tạo";
            case UPDATE          -> "Sửa";
            case DELETE          -> "Xóa";
            case IMPORT          -> "Nhập kho";
            case EXPORT          -> "Xuất kho";
            case INVOICE_CREATE  -> "Tạo HĐ";
            case INVOICE_PAY     -> "Thu tiền";
            case INVOICE_CANCEL  -> "Huỷ HĐ";
            case PO_CREATE       -> "Tạo PO";
            case PO_APPROVE      -> "Duyệt PO";
            case PO_RECEIVE      -> "Nhận PO";
            case PO_CANCEL       -> "Huỷ PO";
            case PAYMENT_CREATE  -> "Phiếu chi";
            case STOCK_ADJUST    -> "Điều chỉnh tồn";
        };
    }

    private JButton primaryButton(String t) {
        JButton b = new JButton(t);
        b.setBackground(Theme.BRAND); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setFont(Theme.FONT_HEADER);
        b.setPreferredSize(new Dimension(80, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JButton secondaryButton(String t) {
        JButton b = new JButton(t);
        b.setBackground(Theme.ACCENT); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setFont(Theme.FONT_NORMAL);
        b.setPreferredSize(new Dimension(100, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
