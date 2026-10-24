package com.utc.warehouse.ui.user;

import com.utc.warehouse.entity.User;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.service.UserService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.ShortcutManager;
import com.utc.warehouse.ui.common.TableDoubleClick;
import com.utc.warehouse.ui.common.TableExportHelper;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.Theme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Quản lý nhân viên — chỉ ADMIN (chỉ hiện menu cho admin).
 *
 * <p>Khác {@link UserManagementPanel}: đây là màn chuyên biệt để **cấp tài
 * khoản cho nhân viên**. Chỉ hiển thị user role STAFF và không cho tạo quyền
 * ADMIN — việc phân quyền quản trị thuộc về màn "Quản lý người dùng".
 *
 * <p>Hỗ trợ đầy đủ: tìm kiếm, lọc theo trạng thái, cấp/sửa/reset mật khẩu,
 * khoá/mở, xoá, xuất Excel, xem chi tiết bằng double-click.
 */
public class EmployeeManagementPanel extends BasePanel {

    private final UserService service = new UserService();
    private final Object[] COLS = {"ID", "Mã nhân viên", "Họ tên", "Trạng thái", "Ngày cấp"};
    private JTable table;
    private JTextField searchField;
    private JComboBox<String> statusFilter;

    public EmployeeManagementPanel() {
        super("Quản lý Nhân viên",
              "Cấp tài khoản và phân công quyền truy cập cho nhân viên kho");
    }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JTextField search = new JTextField();
        search.setPreferredSize(new Dimension(200, 30));
        search.putClientProperty("JTextField.placeholderText", "Tìm mã NV / họ tên...");
        JButton btnSearch = primaryButton("Tìm");

        JComboBox<String> status = new JComboBox<>(
                new String[]{"Tất cả", "Đang hoạt động", "Đã khoá"});
        status.setPreferredSize(new Dimension(150, 30));
        status.setFont(Theme.FONT_NORMAL);

        JButton btnGrant = primaryButton("+ Cấp tài khoản");
        JButton btnEdit   = secondaryButton("Sửa");
        JButton btnReset  = secondaryButton("Đổi mật khẩu");
        JButton btnToggle = secondaryButton("Khoá/Mở");
        JButton btnDelete = dangerButton("Xoá");
        JButton btnRefresh = secondaryButton("Làm mới");
        JButton btnExport = secondaryButton("Xuất Excel");

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnRefresh);
        right.add(btnExport);
        right.add(btnGrant);
        right.add(btnEdit);
        right.add(btnReset);
        right.add(btnToggle);
        right.add(btnDelete);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new JLabel("Tìm:"));
        left.add(search);
        left.add(btnSearch);
        left.add(status);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        btnGrant.addActionListener(e -> onGrant());
        btnEdit.addActionListener(e -> onEdit());
        btnReset.addActionListener(e -> onResetPassword());
        btnToggle.addActionListener(e -> onToggle());
        btnDelete.addActionListener(e -> onDelete());
        btnRefresh.addActionListener(e -> reload());
        btnExport.addActionListener(e -> TableExportHelper.exportCurrentTable(table, "nhan_vien"));
        btnSearch.addActionListener(e -> reload());
        search.addActionListener(e -> reload());
        status.addActionListener(e -> reload());

        btnGrant.setName("btnAdd");
        btnEdit.setName("btnEdit");
        btnDelete.setName("btnDelete");
        btnRefresh.setName("btnRefresh");
        btnExport.setName("btnExport");

        this.searchField = search;
        this.statusFilter = status;
        return bar;
    }

    @Override protected void onShortcutAdd()    { onGrant(); }
    @Override protected void onShortcutEdit()   { onEdit(); }
    @Override protected void onShortcutDelete() { onDelete(); }
    @Override protected void onShortcutRefresh(){ reload(); }
    @Override protected void onShortcutFocusSearch() {
        if (searchField != null) { searchField.requestFocusInWindow(); searchField.selectAll(); }
    }

    @Override
    protected void installDefaultShortcuts() {
        ShortcutManager sc = new ShortcutManager(this);
        sc.bind("ctrl N",  "add",     this::onGrant,    "Cấp tài khoản nhân viên");
        sc.bind("ctrl E",  "edit",    this::onEdit,     "Sửa thông tin nhân viên");
        sc.bind("DELETE",  "delete",  this::onDelete,   "Xoá / khoá nhân viên");
        sc.bind("ctrl R",  "reset",   this::onResetPassword, "Đổi mật khẩu nhân viên");
        sc.bind("ctrl T",  "toggle",  this::onToggle,   "Khoá / Mở tài khoản");
        sc.bind("F5",      "refresh", this::reload,     "Làm mới danh sách");
        sc.bind("ctrl F",  "search",  this::onShortcutFocusSearch, "Tìm kiếm");
        sc.bind("ctrl X",  "export",  () -> TableExportHelper.exportCurrentTable(table, "nhan_vien"), "Xuất Excel");
    }

    @Override
    protected JComponent buildContent() {
        table = TableUtils.createTable(COLS, new Object[0][]);
        TableUtils.centerColumn(table, 0);
        TableUtils.centerColumn(table, 3);
        TableUtils.centerColumn(table, 4);

        TableDoubleClick.installFor(table, this::employeeAtRow, this::showDetail);
        return TableUtils.scrollFor(table);
    }

    /* ============== Data ============== */

    public void reload() {
        try {
            String kw = (searchField == null) ? "" : searchField.getText().trim().toLowerCase();
            String st = (statusFilter == null || statusFilter.getSelectedItem() == null)
                    ? "Tất cả" : statusFilter.getSelectedItem().toString();

            List<User> list = service.allStaff().stream()
                    .filter(u -> kw.isEmpty()
                            || (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw))
                            || (u.getFullName()  != null && u.getFullName().toLowerCase().contains(kw)))
                    .filter(u -> switch (st) {
                        case "Đang hoạt động" -> Boolean.TRUE.equals(u.getActive());
                        case "Đã khoá"       -> !Boolean.TRUE.equals(u.getActive());
                        default              -> true;
                    })
                    .toList();

            DefaultTableModel m = TableUtils.modelOf(table);
            m.setRowCount(0);
            for (User u : list) m.addRow(new Object[]{
                    u.getId(),
                    u.getUsername(),
                    u.getFullName(),
                    Boolean.TRUE.equals(u.getActive()) ? "Hoạt động" : "Đã khoá",
                    u.getCreatedAt() == null ? "" : u.getCreatedAt().toLocalDate().toString()
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + msgOf(ex),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private User employeeAtRow(int modelRow) {
        Object idObj = TableUtils.modelOf(table).getValueAt(modelRow, 0);
        if (idObj == null) return null;
        final Integer id;
        try { id = (idObj instanceof Integer) ? (Integer) idObj : Integer.valueOf(idObj.toString()); }
        catch (NumberFormatException ex) { return null; }
        return service.allStaff().stream()
                .filter(u -> u.getId().equals(id)).findFirst().orElse(null);
    }

    private User selected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return employeeAtRow(table.convertRowIndexToModel(row));
    }

    private void showDetail(User u) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mã nhân viên:  ").append(u.getUsername()).append('\n');
        sb.append("Họ tên:        ").append(blank(u.getFullName())).append('\n');
        sb.append("Quyền:         Nhân viên (STAFF)\n");
        sb.append("Trạng thái:    ").append(Boolean.TRUE.equals(u.getActive()) ? "Đang hoạt động" : "Đã khoá").append('\n');
        sb.append("Ngày cấp:      ").append(u.getCreatedAt() == null ? "-" : u.getCreatedAt().toLocalDate());
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setFont(Theme.FONT_NORMAL);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                "Chi tiết " + u.getUsername(), JOptionPane.PLAIN_MESSAGE);
    }

    private static String blank(String s) { return (s == null || s.isBlank()) ? "(trống)" : s; }

    /* ============== Actions ============== */

    private void onGrant() {
        EmployeeDialog dlg = new EmployeeDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        if (dlg.isSaved()) reload();
    }

    private void onEdit() {
        User u = selected();
        if (u == null) { warn("Chọn 1 nhân viên để sửa."); return; }
        EmployeeDialog dlg = new EmployeeDialog(SwingUtilities.getWindowAncestor(this), u);
        dlg.setVisible(true);
        if (dlg.isSaved()) reload();
    }

    private void onResetPassword() {
        User u = selected();
        if (u == null) { warn("Chọn 1 nhân viên."); return; }
        JPasswordField pwd = new JPasswordField(18);
        int ok = JOptionPane.showConfirmDialog(this, pwd,
                "Mật khẩu mới cho '" + u.getUsername() + "'",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        try {
            service.resetPassword(u, new String(pwd.getPassword()));
            JOptionPane.showMessageDialog(this, "Đã đổi mật khẩu cho " + u.getUsername() + ".");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, msgOf(ex), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onToggle() {
        User u = selected();
        if (u == null) { warn("Chọn 1 nhân viên."); return; }
        try {
            if (Boolean.TRUE.equals(u.getActive())) service.deactivate(u.getId());
            else service.activate(u.getId());
            reload();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, msgOf(ex), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Xoá nhân viên. Nhân viên đã phát sinh giao dịch sẽ bị foreign key chặn —
     * khi đó đề nghị khoá tài khoản để giữ lịch sử.
     */
    private void onDelete() {
        User u = selected();
        if (u == null) { warn("Chọn 1 nhân viên để xoá."); return; }

        User me = AuthService.get().getCurrentUser();
        if (me != null && u.getId().equals(me.getId())) {
            warn("Không thể xoá tài khoản đang đăng nhập.");
            return;
        }

        long refs = service.countReferences(u.getId());
        String msg = (refs > 0)
                ? "Nhân viên '" + u.getUsername() + "' đã có " + refs + " bản ghi liên quan "
                  + "(phiếu nhập/xuất, hoá đơn).\n"
                  + "Không thể xoá hẳn — nên KHOÁ tài khoản để giữ nguyên lịch sử.\n\n"
                  + "Bạn có muốn khoá không?"
                : "Xoá vĩnh viễn tài khoản '" + u.getUsername() + "' ?\n"
                  + "Nhân viên sẽ không thể đăng nhập nữa. Hành động không thể hoàn tác.";

        int ok = JOptionPane.showConfirmDialog(this, msg,
                (refs > 0) ? "Có dữ liệu liên quan" : "Xác nhận xoá",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            if (refs > 0) {
                service.deactivate(u.getId());
                JOptionPane.showMessageDialog(this, "Đã khoá tài khoản '" + u.getUsername() + "'.");
            } else {
                service.delete(u.getId());
                JOptionPane.showMessageDialog(this, "Đã xoá tài khoản '" + u.getUsername() + "'.");
            }
            reload();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, msgOf(ex), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /* ============== helpers ============== */

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thiếu thông tin", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String msgOf(Exception ex) {
        String m = ex.getMessage();
        if (m == null) return ex.getClass().getSimpleName();
        int nl = m.indexOf('\n');
        return nl > 0 ? m.substring(0, nl) : m;
    }

    private JButton primaryButton(String t)   { return styleButton(t, Theme.BRAND, Color.WHITE); }
    private JButton secondaryButton(String t) { return styleButton(t, Theme.ACCENT,  Color.WHITE); }
    private JButton dangerButton(String t)    { return styleButton(t, Theme.DANGER,  Color.WHITE); }

    private JButton styleButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_NORMAL);
        b.setPreferredSize(new Dimension(130, 30));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}