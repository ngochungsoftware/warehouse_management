package com.utc.warehouse.ui.user;

import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.Role;
import com.utc.warehouse.service.UserService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.ShortcutManager;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.UIConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class UserManagementPanel extends BasePanel {

    private final UserService service = new UserService();
    private final Object[] COLS = {"ID", "Username", "Họ tên", "Role", "Active", "Ngày tạo"};
    private JTable table;

    public UserManagementPanel() { super("Quản lý Người dùng (chỉ ADMIN)"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JButton btnAdd    = primaryButton("+ Thêm");
        JButton btnReset  = secondaryButton("Reset mật khẩu");
        JButton btnToggle = secondaryButton("Khoá/Mở");
        JButton btnRefresh = secondaryButton("Làm mới");
        bar.add(btnRefresh); bar.add(btnAdd); bar.add(btnReset); bar.add(btnToggle);

        btnAdd.addActionListener(e -> onAdd());
        btnReset.addActionListener(e -> onReset());
        btnToggle.addActionListener(e -> onToggle());
        btnRefresh.addActionListener(e -> reload());
        btnAdd.setName("btnAdd");
        btnRefresh.setName("btnRefresh");
        return bar;
    }

    @Override protected void onShortcutAdd()    { onAdd(); }
    @Override protected void onShortcutRefresh(){ reload(); }
    // User panel không có edit/delete theo nghĩa thông thường; Reset/ Toggle có phím riêng
    @Override protected void installDefaultShortcuts() {
        ShortcutManager sc = new com.utc.warehouse.ui.common.ShortcutManager(this);
        sc.bind("ctrl N",  "add",     this::onAdd,         "Thêm người dùng");
        sc.bind("ctrl R",  "reset",   this::onReset,       "Reset mật khẩu user đang chọn");
        sc.bind("ctrl T",  "toggle",  this::onToggle,      "Khoá / Mở user đang chọn");
        sc.bind("F5",      "refresh", this::reload,        "Làm mới danh sách");
    }

    @Override
    protected JComponent buildContent() {
        table = TableUtils.createTable(COLS, new Object[0][]);
        TableUtils.centerColumn(table, 0);
        TableUtils.centerColumn(table, 3);
        TableUtils.centerColumn(table, 4);
        TableUtils.centerColumn(table, 5);
        return TableUtils.scrollFor(table);
    }

    public void reload() {
        try {
            List<User> list = service.all();
            DefaultTableModel m = TableUtils.modelOf(table);
            m.setRowCount(0);
            for (User u : list) m.addRow(new Object[]{
                    u.getId(), u.getUsername(), u.getFullName(), u.getRole(),
                    Boolean.TRUE.equals(u.getActive()) ? "✔" : "✘",
                    u.getCreatedAt() == null ? "" : u.getCreatedAt().toLocalDate().toString()
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private User selected() {
        int row = table.getSelectedRow(); if (row < 0) return null;
        int modelRow = table.convertRowIndexToModel(row);
        Object idObj = TableUtils.modelOf(table).getValueAt(modelRow, 0);
        Integer id = (idObj instanceof Integer) ? (Integer) idObj : Integer.valueOf(idObj.toString());
        return service.all().stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
    }

    private void onAdd() {
        JTextField username = new JTextField();
        JPasswordField pwd = new JPasswordField();
        JTextField fullName = new JTextField();
        JComboBox<Role> role = new JComboBox<>(Role.values());

        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Username:")); p.add(username);
        p.add(new JLabel("Mật khẩu:")); p.add(pwd);
        p.add(new JLabel("Họ tên:"));   p.add(fullName);
        p.add(new JLabel("Role:"));     p.add(role);
        int ok = JOptionPane.showConfirmDialog(this, p, "Thêm người dùng",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            try {
                service.create(username.getText().trim(), new String(pwd.getPassword()),
                        fullName.getText().trim(), (Role) role.getSelectedItem());
                reload();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onReset() {
        User u = selected();
        if (u == null) { JOptionPane.showMessageDialog(this, "Chọn 1 user."); return; }
        JPasswordField pwd = new JPasswordField();
        int ok = JOptionPane.showConfirmDialog(this, pwd, "Mật khẩu mới cho '" + u.getUsername() + "'",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            try { service.resetPassword(u, new String(pwd.getPassword())); JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công."); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void onToggle() {
        User u = selected();
        if (u == null) { JOptionPane.showMessageDialog(this, "Chọn 1 user."); return; }
        try {
            if (Boolean.TRUE.equals(u.getActive())) service.deactivate(u.getId());
            else service.activate(u.getId());
            reload();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton primaryButton(String t)   { return styleButton(t, UIConstants.PRIMARY,   Color.WHITE); }
    private JButton secondaryButton(String t) { return styleButton(t, UIConstants.ACCENT,    Color.WHITE); }
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
