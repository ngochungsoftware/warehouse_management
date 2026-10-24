package com.utc.warehouse.ui.user;

import com.utc.warehouse.entity.User;
import com.utc.warehouse.service.UserService;
import com.utc.warehouse.ui.common.Theme;

import javax.swing.*;
import java.awt.*;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

/**
 * Dialog cấp tài khoản cho nhân viên.
 *
 * <p>Khác {@link UserDialog}: role bị cố định {@code STAFF}, không cho chọn —
 * màn này chỉ dành cho nhân viên. Cấp quyền quản trị phải sang màn
 * "Quản lý người dùng".
 *
 * <p>Có nút sinh mật khẩu ngẫu nhiên: admin cấp tài khoản xong sẽ báo mật khẩu
 * cho nhân viên qua kênh khác (Zalo/email/giấy), không cần tự nghĩ mật khẩu an
 * toàn rồi cũng không nhớ.
 */
public class EmployeeDialog extends JDialog {

    private final UserService service = new UserService();
    private final User original;
    private boolean saved = false;

    private final JTextField txtUsername = new JTextField(20);
    private final JTextField txtFullName = new JTextField(20);
    private final JPasswordField txtPassword = new JPasswordField(20);
    private final JLabel lblPasswordHint = new JLabel("");

    private final List<User> existing;

    public EmployeeDialog(Window owner, User employee) {
        super(owner, employee == null ? "Cấp tài khoản nhân viên" : "Sửa tài khoản nhân viên",
                ModalityType.APPLICATION_MODAL);
        this.original = employee;
        this.existing = service.all();

        setSize(480, 330);
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
        form.add(label("Mật khẩu (*)"), c);

        // Ô mật khẩu + nút sinh mật khẩu nằm chung một hàng.
        JPanel pwRow = new JPanel(new BorderLayout(6, 0));
        pwRow.setOpaque(false);
        JButton btnGen = new JButton("Sinh ngẫu nhiên");
        btnGen.setFont(Theme.FONT_SMALL);
        btnGen.setFocusPainted(false);
        btnGen.setPreferredSize(new Dimension(140, 28));
        btnGen.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pwRow.add(txtPassword, BorderLayout.CENTER);
        pwRow.add(btnGen, BorderLayout.EAST);
        c.gridx = 1; c.weightx = 1;
        form.add(pwRow, c);

        lblPasswordHint.setFont(Theme.FONT_SMALL);
        lblPasswordHint.setForeground(Theme.TEXT_MUTED);
        c.gridx = 1; c.gridy = 2; c.weightx = 1;
        form.add(lblPasswordHint, c);

        c.gridx = 0; c.gridy = 3; c.weightx = 0;
        form.add(label("Họ tên"), c);
        c.gridx = 1; c.weightx = 1;
        form.add(txtFullName, c);

        c.gridx = 0; c.gridy = 4; c.weightx = 0;
        form.add(label("Quyền"), c);
        JLabel role = new JLabel("Nhân viên (STAFF)");
        role.setFont(Theme.FONT_NORMAL);
        c.gridx = 1; c.weightx = 1;
        form.add(role, c);

        add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnSave = new JButton("Lưu");
        btnSave.setBackground(Theme.BRAND);
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(Theme.FONT_NORMAL);
        JButton btnCancel = new JButton("Hủy");
        btnCancel.setFont(Theme.FONT_NORMAL);
        btns.add(btnSave);
        btns.add(btnCancel);
        add(btns, BorderLayout.SOUTH);

        fillFields();
        btnGen.addActionListener(e -> txtPassword.setText(generatePassword()));
        btnSave.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(btnSave);
    }

    private void fillFields() {
        if (original != null) {
            txtUsername.setText(original.getUsername());
            txtFullName.setText(original.getFullName());
            txtUsername.setEditable(false);
            txtPassword.setEnabled(false);
            lblPasswordHint.setText("Giữ nguyên mật khẩu. Dùng \"Reset mật khẩu\" nếu nhân viên quên.");
        } else {
            lblPasswordHint.setText("Bấm \"Sinh ngẫu nhiên\" để tạo mật khẩu, hoặc tự nhập (>= 6 ký tự).");
        }
    }

    /** Mật khẩu 8 ký tự gồm chữ + số, loại ký tự dễ nhầm (0/O, 1/l/I). */
    private static String generatePassword() {
        final String LETTERS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
        final String DIGITS   = "23456789";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 6; i++) sb.append(LETTERS.charAt(rnd.nextInt(LETTERS.length())));
        for (int i = 0; i < 2; i++) sb.append(DIGITS.charAt(rnd.nextInt(DIGITS.length())));
        // Xáo trộn để ký tự đầu không phải lúc nào cũng là chữ.
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            char t = chars[i]; chars[i] = chars[j]; chars[j] = t;
        }
        return new String(chars);
    }

    private void onSave() {
        String username = txtUsername.getText().trim();
        String fullName  = txtFullName.getText().trim();

        if (username.isEmpty()) {
            warn("Username không được trống.");
            return;
        }
        try {
            if (original == null) {
                String pw = new String(txtPassword.getPassword());
                if (pw.length() < 6) {
                    warn("Mật khẩu phải có ít nhất 6 ký tự.");
                    return;
                }
                checkDuplicate(username, null);
                User created = service.createEmployee(username, pw, fullName);
                confirmPassword(created, pw);
            } else {
                checkDuplicate(username, original.getId());
                original.setFullName(fullName);
                service.update(original);
                JOptionPane.showMessageDialog(this, "Cập nhật thành công!");
            }
            saved = true;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, msgOf(ex), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Hiển thị thông tin đăng nhập để admin chuyển cho nhân viên.
     * Không hiện mật khẩu trên màn hình nữa vì đã đóng dialog — chỉ nhắc nhở.
     */
    private void confirmPassword(User created, String pw) {
        JTextPane tp = new JTextPane();
        tp.setEditable(false);
        tp.setContentType("text/plain");
        tp.setText("Đã cấp tài khoản thành công.\n\n"
                + "Username: " + created.getUsername() + "\n"
                + "Mật khẩu:  " + pw + "\n\n"
                + "Hãy chuyển thông tin này cho nhân viên và nhắc họ đổi mật khẩu "
                + "sau lần đăng nhập đầu tiên.");
        tp.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(tp),
                "Thông tin đăng nhập", JOptionPane.INFORMATION_MESSAGE);
    }

    private void checkDuplicate(String username, Integer selfId) {
        boolean dup = existing.stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username)
                        && (selfId == null || !u.getId().equals(selfId)));
        if (dup) throw new IllegalArgumentException("Username '" + username + "' đã tồn tại.");
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
    }

    private static String msgOf(Exception ex) {
        String m = ex.getMessage();
        if (m == null) return ex.getClass().getSimpleName();
        int nl = m.indexOf('\n');
        return nl > 0 ? m.substring(0, nl) : m;
    }

    private JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.FONT_NORMAL);
        return l;
    }

    public boolean isSaved() { return saved; }
}