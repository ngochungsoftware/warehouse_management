package com.utc.warehouse.ui;

import com.utc.warehouse.config.HibernateShutdown;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.ui.common.ModernComponents;
import com.utc.warehouse.ui.common.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class LoginFrame extends JFrame {

    private final JTextField     txtUsername = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JLabel         lblError    = new JLabel(" ");

    public LoginFrame() {
        setTitle("Đăng nhập - Quản lý Kho");
        setUndecorated(true);
        setSize(900, 560);
        setShape(new RoundRectangle2D.Double(0, 0, 900, 560, 20, 20));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new GradientPanel();
        root.setLayout(new GridLayout(1, 2));
        add(root);

        root.add(buildLeftHero());
        root.add(buildRightForm());

        // ESC để thoát
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "exit");
        getRootPane().getActionMap().put("exit", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                HibernateShutdown.shutdownAndExit(0);
            }
        });
    }

    /* ============== LEFT: Hero / branding ============== */
    private JPanel buildLeftHero() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(60, 50, 60, 50));

        // Logo tròn chữ W
        JPanel logo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Theme.BRAND_DARK);
                g2.setFont(Theme.FONT_BIG);
                FontMetrics fm = g2.getFontMetrics();
                String s = "W";
                int x = (getWidth()  - fm.stringWidth(s)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(s, x, y);
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        logo.setPreferredSize(new Dimension(80, 80));
        logo.setMaximumSize(new Dimension(80, 80));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("<html><div style='color:white'>" +
                "<span style='font-size:32px;font-weight:700'>Quản lý Kho</span><br>" +
                "<span style='font-size:14px;color:#E0E0E0'>Warehouse Management System</span></div></html>");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(24, 0, 16, 0));

        JLabel desc = new JLabel("<html><div style='color:#B2DFDB; font-size:13px; line-height:1.6'>" +
                "Quản lý sản phẩm, nhà cung cấp, khách hàng,<br>" +
                "phiếu nhập - xuất và báo cáo tồn kho<br>" +
                "một cách nhanh chóng và chính xác.</div></html>");
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        desc.setBorder(BorderFactory.createEmptyBorder(0, 0, 24, 0));

        p.add(logo);
        p.add(title);
        p.add(desc);
        p.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("© 2026 UTC Warehouse");
        footer.setFont(Theme.FONT_SMALL);
        footer.setForeground(new Color(0xB2DFDB));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(footer);

        return p;
    }

    /* ============== RIGHT: Form đăng nhập (card trắng) ============== */
    private JPanel buildRightForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);

        JPanel card = ModernComponents.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(360, 460));
        card.setMaximumSize(new Dimension(360, 460));
        card.setBorder(BorderFactory.createEmptyBorder(40, 36, 36, 36));

        JLabel hello = new JLabel("Xin chào!");
        hello.setFont(Theme.FONT_TITLE);
        hello.setForeground(Theme.TEXT);
        hello.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Đăng nhập để tiếp tục vào hệ thống");
        sub.setFont(Theme.FONT_NORMAL);
        sub.setForeground(Theme.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setBorder(BorderFactory.createEmptyBorder(4, 0, 28, 0));

        // Username
        JLabel lblUser = fieldLabel("Tên đăng nhập");
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        Theme.applyPlaceholder(txtUsername, "Nhập tên đăng nhập");
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Password
        JLabel lblPwd = fieldLabel("Mật khẩu");
        lblPwd.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblPwd.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        txtPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        Theme.applyPlaceholder(txtPassword, "Nhập mật khẩu");
        txtPassword.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Error
        lblError.setForeground(Theme.DANGER);
        lblError.setFont(Theme.FONT_SMALL);
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblError.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Buttons
        JButton btnLogin = ModernComponents.primaryButton("Đăng nhập");
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnLogin.setMargin(new Insets(10, 0, 10, 0));
        btnLogin.addActionListener(e -> doLogin());

        JButton btnExit = ModernComponents.ghostButton("Thoát");
        btnExit.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnExit.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnExit.setHorizontalAlignment(SwingConstants.CENTER);
        btnExit.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        btnExit.addActionListener(e -> HibernateShutdown.shutdownAndExit(0));

        // Không gợi ý tài khoản/mật khẩu mặc định trên màn hình — hiển thị
        // mật khẩu ở đây khiến bất kỳ ai mở app cũng đăng nhập được vào
        // tài khoản admin. Xem README mục "Tài khoản mặc định" để biết
        // thông tin ban đầu và nên đổi mật khẩu ngay sau lần đăng nhập đầu.
        JLabel hint = new JLabel("<html><div style='text-align:center; color:#94A3B8; font-size:11px; margin-top:12px'>"
                + "Liên hệ quản trị viên nếu quên mật khẩu</div></html>");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        hint.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(hello);
        card.add(sub);
        card.add(lblUser);
        card.add(Box.createVerticalStrut(6));
        card.add(txtUsername);
        card.add(lblPwd);
        card.add(Box.createVerticalStrut(6));
        card.add(txtPassword);
        card.add(lblError);
        card.add(Box.createVerticalStrut(20));
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(8));
        card.add(btnExit);
        card.add(Box.createVerticalGlue());
        card.add(hint);

        p.add(card);
        getRootPane().setDefaultButton(btnLogin);
        return p;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    private void doLogin() {
        String u = txtUsername.getText().trim();
        String p = new String(txtPassword.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            lblError.setText("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }
        try {
            var opt = AuthService.get().login(u, p);
            if (opt.isPresent()) {
                dispose();
                SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
            } else {
                lblError.setText("Sai tên đăng nhập hoặc mật khẩu, hoặc tài khoản đã bị khóa.");
                txtPassword.setText("");
            }
        } catch (Exception ex) {
            lblError.setText("Lỗi: " + ex.getMessage());
        }
    }

    /* ============== Gradient background panel ============== */
    private static class GradientPanel extends JPanel {
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            // Gradient xanh lá đậm -> xanh dương
            GradientPaint gp = new GradientPaint(
                    0, 0, Theme.BRAND_DARK,
                    w, h, new Color(0x0F3D2E));
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
            // Vài đường tròn trang trí
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillOval(-80, -80, 220, 220);
            g2.fillOval(w - 200, h - 200, 280, 280);
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillOval(w / 2 - 100, h - 100, 180, 180);
            g2.dispose();
        }
    }
}
