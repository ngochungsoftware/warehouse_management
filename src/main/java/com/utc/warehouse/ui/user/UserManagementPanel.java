package com.utc.warehouse.ui.user;

import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.Role;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.service.UserService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.ShortcutManager;
import com.utc.warehouse.ui.common.TableDoubleClick;
import com.utc.warehouse.ui.common.TableExportHelper;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.UIConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Quản lý người dùng (chỉ ADMIN).
 *
 * <p>Đầy đủ CRUD + các thao tác riêng cho tài khoản: reset mật khẩu, khoá/mở.
 *
 * <p>Về "xoá": vì {@code stock_transactions.user_id} có {@code FOREIGN KEY} không
 * {@code ON DELETE}, user đã từng phát sinh giao dịch sẽ không xoá hẳn được.
 * Vì vậy panel này ưu tiên dùng <b>khoá tài khoản</b> (soft delete) cho các user
 * đó, và chỉ xoá hẳn khi thực sự không có tham chiếu nào.
 */
public class UserManagementPanel extends BasePanel {

    private final UserService service = new UserService();
    private final Object[] COLS = {"ID", "Username", "Họ tên", "Role", "Active", "Ngày tạo"};
    private JTable table;
    private JTextField searchField;

    public UserManagementPanel() { super("Quản lý Người dùng (chỉ ADMIN)"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JTextField search = new JTextField();
        search.setPreferredSize(new Dimension(220, 30));
        search.putClientProperty("JTextField.placeholderText", "Tìm username / họ tên...");
        JButton btnSearch = primaryButton("Tìm");
        JButton btnAdd    = primaryButton("+ Thêm");
        JButton btnEdit   = secondaryButton("Sửa");
        JButton btnDelete = dangerButton("Xoá");
        JButton btnReset  = secondaryButton("Reset mật khẩu");
        JButton btnToggle = secondaryButton("Khoá/Mở");
        JButton btnRefresh = secondaryButton("Làm mới");
        JButton btnExport = secondaryButton("Xuất Excel");

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnRefresh);
        right.add(btnExport);
        right.add(btnAdd);
        right.add(btnEdit);
        right.add(btnDelete);
        right.add(btnReset);
        right.add(btnToggle);

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
        btnReset.addActionListener(e -> onReset());
        btnToggle.addActionListener(e -> onToggle());
        btnRefresh.addActionListener(e -> reload());
        btnExport.addActionListener(e -> TableExportHelper.exportCurrentTable(table, "nguoi_dung"));
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

    @Override protected void onShortcutAdd()    { onAdd(); }
    @Override protected void onShortcutEdit()   { onEdit(); }
    @Override protected void onShortcutDelete() { onDelete(); }
    @Override protected void onShortcutRefresh(){ reload(); }
    @Override protected void onShortcutFocusSearch() {
        if (searchField != null) { searchField.requestFocusInWindow(); searchField.selectAll(); }
    }

    @Override
    protected void installDefaultShortcuts() {
        ShortcutManager sc = new ShortcutManager(this);
        sc.bind("ctrl N",  "add",     this::onAdd,     "Thêm người dùng");
        sc.bind("ctrl E",  "edit",    this::onEdit,    "Sửa người dùng đang chọn");
        sc.bind("DELETE",  "delete",  this::onDelete,  "Xoá / khoá người dùng đang chọn");
        sc.bind("ctrl R",  "reset",   this::onReset,   "Reset mật khẩu user đang chọn");
        sc.bind("ctrl T",  "toggle",  this::onToggle,  "Khoá / Mở user đang chọn");
        sc.bind("F5",      "refresh", this::reload,    "Làm mới danh sách");
        sc.bind("ctrl F",  "search",  this::onShortcutFocusSearch, "Tìm kiếm");
        sc.bind("ctrl X",  "export",  () -> TableExportHelper.exportCurrentTable(table, "nguoi_dung"), "Xuất Excel");
    }

    @Override
    protected JComponent buildContent() {
        table = TableUtils.createTable(COLS, new Object[0][]);
        TableUtils.centerColumn(table, 0);
        TableUtils.centerColumn(table, 3);
        TableUtils.centerColumn(table, 4);
        TableUtils.centerColumn(table, 5);

        TableDoubleClick.installFor(table, this::userAtRow, this::showUserDetail);
        return TableUtils.scrollFor(table);
    }

    /* ============== Data ============== */

    public void reload() { reloadFiltered(""); }

    public void reloadFiltered(String keyword) {
        try {
            String kw = keyword == null ? "" : keyword.trim().toLowerCase();
            List<User> list = service.all().stream()
                    .filter(u -> kw.isEmpty()
                            || (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw))
                            || (u.getFullName()  != null && u.getFullName().toLowerCase().contains(kw)))
                    .toList();

            DefaultTableModel m = TableUtils.modelOf(table);
            m.setRowCount(0);
            for (User u : list) m.addRow(new Object[]{
                    u.getId(),
                    u.getUsername(),
                    u.getFullName(),
                    roleLabel(u.getRole()),
                    Boolean.TRUE.equals(u.getActive()) ? "✔" : "✘",
                    u.getCreatedAt() == null ? "" : u.getCreatedAt().toLocalDate().toString()
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + msgOf(ex),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String roleLabel(Role r) { return r == Role.ADMIN ? "ADMIN" : "STAFF"; }

    /** Tra User từ model row index (dùng cho double-click). */
    private User userAtRow(int modelRow) {
        Object idObj = TableUtils.modelOf(table).getValueAt(modelRow, 0);
        if (idObj == null) return null;
        final Integer id;
        try { id = (idObj instanceof Integer) ? (Integer) idObj : Integer.valueOf(idObj.toString()); }
        catch (NumberFormatException ex) { return null; }
        return service.all().stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
    }

    private User selected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return userAtRow(table.convertRowIndexToModel(row));
    }

    private void showUserDetail(User u) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID:            ").append(u.getId()).append('\n');
        sb.append("Username:      ").append(u.getUsername()).append('\n');
        sb.append("Họ tên:        ").append(blank(u.getFullName())).append('\n');
        sb.append("Role:          ").append(roleLabel(u.getRole())).append('\n');
        sb.append("Trạng thái:    ").append(Boolean.TRUE.equals(u.getActive()) ? "Đang hoạt động" : "Đã khoá").append('\n');
        sb.append("Ngày tạo:      ").append(u.getCreatedAt() == null ? "-" : u.getCreatedAt().toLocalDate());
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setFont(UIConstants.FONT_NORMAL);
        JOptionPane.showMessageDialog(this, new JScrollPane(ta),
                "Chi tiết " + u.getUsername(), JOptionPane.PLAIN_MESSAGE);
    }

    private static String blank(String s) { return (s == null || s.isBlank()) ? "(trống)" : s; }

    /* ============== CRUD ============== */

    private void onAdd() {
        UserDialog dlg = new UserDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setVisible(true);
        if (dlg.isSaved()) reloadFiltered(searchField == null ? "" : searchField.getText());
    }

    private void onEdit() {
        User u = selected();
        if (u == null) { warn("Chọn 1 người dùng để sửa."); return; }
        UserDialog dlg = new UserDialog(SwingUtilities.getWindowAncestor(this), u);
        dlg.setVisible(true);
        if (dlg.isSaved()) reloadFiltered(searchField == null ? "" : searchField.getText());
    }

    /**
     * Xoá người dùng.
     *
     * <p>Nếu user còn dữ liệu tham chiếu, MySQL sẽ chặn xoá hẳn. Khi đó hỏi
     * người dùng có muốn khoá tài khoản (soft delete) thay không — đây là cách
     * đúng vì giữ nguyên lịch sử giao dịch.
     */
    private void onDelete() {
        User u = selected();
        if (u == null) { warn("Chọn 1 người dùng để xoá."); return; }

        User me = AuthService.get().getCurrentUser();
        if (me != null && u.getId().equals(me.getId())) {
            warn("Không thể xoá tài khoản đang đăng nhập.");
            return;
        }

        long refs = service.countReferences(u.getId());
        String msg = (refs > 0)
                ? "Người dùng '" + u.getUsername() + "' đang có " + refs + " bản ghi liên quan "
                    + "(giao dịch / hoá đơn / thanh toán).\n"
                    + "Không thể xoá hẳn — nên KHOÁ tài khoản này để giữ nguyên lịch sử.\n\n"
                    + "Bạn có muốn khoá không?"
                : "Xoá vĩnh viễn người dùng '" + u.getUsername() + "' ?\nHành động này không thể hoàn tác.";

        int ok = JOptionPane.showConfirmDialog(this, msg,
                (refs > 0) ? "Có dữ liệu liên quan" : "Xác nhận xoá",
                JOptionPane.YES_NO_OPTION,
                (refs > 0) ? JOptionPane.WARNING_MESSAGE : JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;

        try {
            if (refs > 0) {
                service.deactivate(u.getId());
                JOptionPane.showMessageDialog(this, "Đã khoá tài khoản '" + u.getUsername() + "'.");
            } else {
                service.delete(u.getId());
                JOptionPane.showMessageDialog(this, "Đã xoá người dùng '" + u.getUsername() + "'.");
            }
            reloadFiltered(searchField == null ? "" : searchField.getText());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, msgOf(ex), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onReset() {
        User u = selected();
        if (u == null) { warn("Chọn 1 người dùng."); return; }
        JPasswordField pwd = new JPasswordField(18);
        int ok = JOptionPane.showConfirmDialog(this, pwd, "Mật khẩu mới cho '" + u.getUsername() + "'",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            try {
                service.resetPassword(u, new String(pwd.getPassword()));
                JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, msgOf(ex), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onToggle() {
        User u = selected();
        if (u == null) { warn("Chọn 1 người dùng."); return; }
        try {
            if (Boolean.TRUE.equals(u.getActive())) service.deactivate(u.getId());
            else service.activate(u.getId());
            reloadFiltered(searchField == null ? "" : searchField.getText());
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

    private JButton primaryButton(String t)   { return styleButton(t, UIConstants.PRIMARY, Color.WHITE); }
    private JButton secondaryButton(String t) { return styleButton(t, UIConstants.ACCENT,  Color.WHITE); }
    private JButton dangerButton(String t)    { return styleButton(t, UIConstants.DANGER,  Color.WHITE); }

    private JButton styleButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(UIConstants.FONT_NORMAL);
        b.setPreferredSize(new Dimension(130, 30));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}