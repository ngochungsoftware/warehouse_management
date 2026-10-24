package com.utc.warehouse.ui.user;

import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.Role;
import com.utc.warehouse.service.UserService;
import com.utc.warehouse.ui.common.UIConstants;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Optional;

/**
 * Dialog thêm / sửa người dùng.
 *
 * <p>Dùng chung cho cả Create và Edit — truyền {@code null} để thêm, truyền
 * entity có sẵn để sửa. Kiểm tra trùng username ngay tại chỗ (không chỉ dựa vào
 * exception từ DB) để báo lỗi rõ ràng cho người dùng.
 */
public class UserDialog extends JDialog {

    private final UserService service = new UserService();
    private final User original;
    private boolean saved = false;

    private final JTextField txtUsername = new JTextField(20);
    private final JTextField txtFullName = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JComboBox<Role> cboRole = new JComboBox<>(Role.values());
    private final JLabel lblPassword = new JLabel("Mật khẩu (*):");
    private final JLabel lblPasswordHint = new JLabel("");

    /** Danh sách username đang có, để chặn trùng ngay khi bấm Lưu. */
    private final List<User> existing;

    public UserDialog(Window owner, User user) {
        super(owner, user == null ? "Thêm người dùng" : "Sửa người dùng",
                ModalityType.APPLICATION_MODAL);
        this.original = user;
        this.existing = service.all();

        setSize(460, 300);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 10, 18));
        form.setBackground(Color.WHITE);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        form.add(label("Username (*)"), c);
        c.gridx = 1; c.weightx = 1;
        form.add(txtUsername, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        form.add(lblPassword, c);
        c.gridx = 1; c.weightx = 1;
        form.add(txtPassword, c);

        // Khi sửa thì không bắt nhập lại mật khẩu (giữ nguyên mật khẩu cũ).
        lblPasswordHint.setFont(UIConstants.FONT_SMALL);
        lblPasswordHint.setForeground(UIConstants.TEXT_MUTED);
        c.gridx = 1; c.gridy = 2; c.weightx = 1;
        form.add(lblPasswordHint, c);

        c.gridx = 0; c.gridy = 3; c.weightx = 0;
        form.add(label("Họ tên"), c);
        c.gridx = 1; c.weightx = 1;
        form.add(txtFullName, c);

        c.gridx = 0; c.gridy = 4; c.weightx = 0;
        form.add(label("Role"), c);
        c.gridx = 1; c.weightx = 1;
        form.add(cboRole, c);

        add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnSave = new JButton("Lưu");
        btnSave.setBackground(UIConstants.PRIMARY);
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(UIConstants.FONT_NORMAL);
        JButton btnCancel = new JButton("Hủy");
        btnCancel.setFont(UIConstants.FONT_NORMAL);
        btns.add(btnSave);
        btns.add(btnCancel);
        add(btns, BorderLayout.SOUTH);

        fillFields();

        btnSave.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());

        getRootPane().setDefaultButton(btnSave);
    }

    private void fillFields() {
        if (original != null) {
            txtUsername.setText(original.getUsername());
            txtFullName.setText(original.getFullName());
            cboRole.setSelectedItem(original.getRole());
            // Sửa: khoá username vì đổi username sẽ làm lệch dữ liệu
            // lịch sử đã ghi theo tên cũ.
            txtUsername.setEditable(false);
            txtPassword.setEnabled(false);
            lblPassword.setText("Mật khẩu:");
            lblPasswordHint.setText("Để nguyên mật khẩu cũ. Dùng nút \"Reset mật khẩu\" nếu cần đổi.");
        } else {
            cboRole.setSelectedItem(Role.STAFF);
        }
    }

    private void onSave() {
        String username = txtUsername.getText().trim();
        String fullName  = txtFullName.getText().trim();
        Role   role     = (Role) cboRole.getSelectedItem();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username không được trống.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (original == null) {
                String pw = new String(txtPassword.getPassword());
                if (pw.length() < 3) {
                    JOptionPane.showMessageDialog(this, "Mật khẩu phải có ít nhất 3 ký tự.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                checkDuplicateUsername(username, null);
                service.create(username, pw, fullName, role);
                JOptionPane.showMessageDialog(this, "Thêm người dùng thành công!");
            } else {
                checkDuplicateUsername(username, original.getId());
                original.setFullName(fullName);
                original.setRole(role);
                service.update(original);
                JOptionPane.showMessageDialog(this, "Cập nhật thành công!");
            }
            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, msgOf(ex), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Chặn trùng username, bỏ qua chính bản ghi đang sửa. */
    private void checkDuplicateUsername(String username, Integer selfId) throws Exception {
        boolean dup = existing.stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username)
                        && (selfId == null || !u.getId().equals(selfId)));
        if (dup) throw new IllegalArgumentException("Username '" + username + "' đã tồn tại.");
    }

    /** Rút gọn message exception cho dễ đọc (bỏ mã lỗi SQL dài dòng). */
    private static String msgOf(Exception ex) {
        String m = ex.getMessage();
        if (m == null) return ex.getClass().getSimpleName();
        int nl = m.indexOf('\n');
        return nl > 0 ? m.substring(0, nl) : m;
    }

    private JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(UIConstants.FONT_NORMAL);
        return l;
    }

    public boolean isSaved() { return saved; }
}