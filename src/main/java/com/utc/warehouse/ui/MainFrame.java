package com.utc.warehouse.ui;

import com.utc.warehouse.config.HibernateShutdown;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.service.AuthService;
import com.utc.warehouse.ui.category.CategoryListPanel;
import com.utc.warehouse.ui.common.ModernComponents;
import com.utc.warehouse.ui.common.Theme;
import com.utc.warehouse.ui.customer.CustomerListPanel;
import com.utc.warehouse.ui.product.ProductListPanel;
import com.utc.warehouse.ui.report.InventoryReportPanel;
import com.utc.warehouse.ui.report.RevenueReportPanel;
import com.utc.warehouse.ui.report.TopProductsPanel;
import com.utc.warehouse.ui.invoice.InvoicePanel;
import com.utc.warehouse.ui.invoice.DebtReportPanel;
import com.utc.warehouse.ui.purchase.PurchaseOrderPanel;
import com.utc.warehouse.ui.admin.AuditLogPanel;
import com.utc.warehouse.ui.stock.ImportPanel;
import com.utc.warehouse.ui.stock.ExportPanel;
import com.utc.warehouse.ui.stock.TransactionHistoryPanel;
import com.utc.warehouse.ui.supplier.SupplierListPanel;
import com.utc.warehouse.ui.user.UserManagementPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

    private final JPanel        contentPanel = new JPanel(new CardLayout());
    private final JPanel        menuContainer = new JPanel();
    private final DefaultListModel<MenuItem> menuModel = new DefaultListModel<>();
    private final JList<MenuItem> menuList = new JList<>(menuModel);
    private final Map<String, String> keyToCard = new LinkedHashMap<>();
    private final JLabel        lblUser   = new JLabel();
    private final JLabel        lblRole   = new JLabel();
    private final JLabel        lblClock  = new JLabel();
    private final List<String>  menuOrder = new ArrayList<>();
    private final com.utc.warehouse.ui.common.ShortcutManager globalSc;

    public MainFrame() {
        setTitle("Quản lý Kho - Warehouse Management");
        setSize(1280, 780);
        setMinimumSize(new Dimension(1024, 640));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                HibernateShutdown.shutdownAndExit(0);
            }
        });

        // Phím tắt toàn cục (Ctrl+1..9, F1, Ctrl+L, Ctrl+Q, Esc, ...)
        globalSc = new com.utc.warehouse.ui.common.ShortcutManager(this);
        globalSc.bind("F1",      "help",    this::showHelpDialog,   "Xem danh sách phím tắt");
        globalSc.bind("ctrl L",  "logout",  this::doLogout,         "Đăng xuất");
        globalSc.bind("ctrl Q",  "quit",    () -> HibernateShutdown.shutdownAndExit(0), "Thoát ứng dụng");
        globalSc.bind("ctrl 1",  "go1", () -> switchMenu(0), "Mở: Tổng quan");
        globalSc.bind("ctrl 2",  "go2", () -> switchMenu(1), "Mở: Sản phẩm");
        globalSc.bind("ctrl 3",  "go3", () -> switchMenu(2), "Mở: Danh mục");
        globalSc.bind("ctrl 4",  "go4", () -> switchMenu(3), "Mở: Nhà cung cấp");
        globalSc.bind("ctrl 5",  "go5", () -> switchMenu(4), "Mở: Khách hàng");
        globalSc.bind("ctrl 6",  "go6", () -> switchMenu(5), "Mở: Phiếu nhập");
        globalSc.bind("ctrl 7",  "go7", () -> switchMenu(6), "Mở: Phiếu xuất");
        globalSc.bind("ctrl 8",  "go8", () -> switchMenu(7), "Mở: Lịch sử nhập/xuất");
        // Ctrl+9 = user management (chỉ ADMIN)

        setJMenuBar(buildMenuBar());

        initLayout();
        initMenu();
        startClock();

        ((CardLayout) contentPanel.getLayout()).show(contentPanel, firstKey());
    }

    private void switchMenu(int idx) {
        if (idx < 0 || idx >= menuModel.size()) return;
        String label = menuModel.get(idx).label;
        menuList.setSelectedIndex(idx);
        ((CardLayout) contentPanel.getLayout()).show(contentPanel, label);
    }

    private void doLogout() {
        AuthService.get().logout();
        dispose();
        new LoginFrame().setVisible(true);
    }

    private void showHelpDialog() {
        java.util.List<com.utc.warehouse.ui.common.ShortcutManager.Entry> global = globalSc.getBindings();
                // Lấy phím tắt của panel hiện tại
                java.util.List<com.utc.warehouse.ui.common.ShortcutManager.Entry> page = new java.util.ArrayList<>();
                com.utc.warehouse.ui.common.BasePanel found = null;
                for (Component c : contentPanel.getComponents()) {
                    if (c.isVisible() && c instanceof com.utc.warehouse.ui.common.BasePanel bp) {
                        found = bp;
                        break;
                    }
                }
                if (found != null) {
                    javax.swing.InputMap imap = found.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                    javax.swing.ActionMap amap = found.getActionMap();
                    java.util.Map<String, javax.swing.Action> seen = new java.util.LinkedHashMap<>();
                    for (KeyStroke ks : imap.allKeys()) {
                        Object key = imap.get(ks);
                        if (key == null) continue;
                        javax.swing.Action a = amap.get(key);
                        if (a == null) continue;
                        String name = (String) a.getValue("scName");
                        if (name == null || seen.containsKey(name)) continue;
                        seen.put(name, a);
                    }
                    for (var en : seen.entrySet()) {
                        javax.swing.Action a = en.getValue();
                        String kspec = (String) a.getValue("scKeySpec");
                        String desc  = (String) a.getValue("scDesc");
                        page.add(new com.utc.warehouse.ui.common.ShortcutManager.Entry(
                                (String) a.getValue("scName"),
                                kspec == null ? "" : kspec,
                                desc == null ? "" : desc));
                    }
                    String title = found.title == null ? "Trang hiện tại" : found.title;
                    page.add(0, new com.utc.warehouse.ui.common.ShortcutManager.Entry(
                            "── Phím tắt của: " + title + " ──", "", ""));
                }
                java.util.List<com.utc.warehouse.ui.common.ShortcutManager.Entry> headerList =
                        java.util.List.of(new com.utc.warehouse.ui.common.ShortcutManager.Entry(
                                "── Toàn cục ──", "", ""));
                com.utc.warehouse.ui.common.HelpDialog.show(this, "Phím tắt", headerList, global, page);
    }

    /* ===================== LAYOUT ===================== */
    private void initLayout() {
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);
        add(buildMain(),   BorderLayout.CENTER);
    }

    /** Sidebar trái: brand + menu dạng list custom (icon + label + active highlight). */
    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(Theme.SIDEBAR_BG);
        side.setPreferredSize(new Dimension(230, 0));

        // Brand
        JPanel brand = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.SIDEBAR_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Logo chữ W
                g2.setColor(Theme.BRAND);
                g2.fillRoundRect(14, 18, 36, 36, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(Theme.FONT_TITLE);
                FontMetrics fm = g2.getFontMetrics();
                int x = 14 + (36 - fm.stringWidth("W")) / 2;
                int y = 18 + (36 - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString("W", x, y);
                g2.dispose();
            }
        };
        brand.setOpaque(false);
        brand.setPreferredSize(new Dimension(230, 72));
        brand.setLayout(null);
        JLabel brandName = new JLabel("WAREHOUSE");
        brandName.setFont(Theme.FONT_BRAND);
        brandName.setForeground(Color.WHITE);
        brandName.setBounds(60, 18, 160, 22);
        JLabel brandSub = new JLabel("Quản lý kho hàng");
        brandSub.setFont(Theme.FONT_BRAND_SM);
        brandSub.setForeground(new Color(0x94A3B8));
        brandSub.setBounds(60, 40, 160, 16);
        brand.add(brandName);
        brand.add(brandSub);
        side.add(brand, BorderLayout.NORTH);

        // Menu list
        menuContainer.setOpaque(false);
        menuContainer.setLayout(new BorderLayout());
        menuContainer.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        menuList.setBackground(Theme.SIDEBAR_BG);
        menuList.setCellRenderer(new SidebarRenderer());
        menuList.setFixedCellHeight(44);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                MenuItem m = menuList.getSelectedValue();
                if (m != null) {
                    ((CardLayout) contentPanel.getLayout()).show(contentPanel, m.label);
                    // Tự reload dữ liệu mỗi khi chuyển trang (cho Dashboard / Báo cáo / Hóa đơn)
                    for (Component c : contentPanel.getComponents()) {
                        if (c.isVisible() && c instanceof com.utc.warehouse.ui.common.BasePanel bp) {
                            try { bp.reload(); } catch (Exception ignored) { }
                            break;
                        }
                    }
                }
            }
        });
        menuContainer.add(menuList, BorderLayout.CENTER);
        side.add(menuContainer, BorderLayout.CENTER);

        // User info bottom
        side.add(buildUserBox(), BorderLayout.SOUTH);

        return side;
    }

    private JPanel buildUserBox() {
        User u = AuthService.get().getCurrentUser();
        JPanel box = new JPanel(new BorderLayout(0, 6));
        box.setBackground(Theme.SIDEBAR_BG);
        box.setBorder(BorderFactory.createEmptyBorder(12, 16, 14, 16));

        // Avatar tròn + tên
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        JLabel avatar = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BRAND);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String initial = (u == null || u.getFullName() == null || u.getFullName().isBlank())
                        ? "U" : u.getFullName().substring(0, 1).toUpperCase();
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth()  - fm.stringWidth(initial)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initial, x, y);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));
        avatar.setOpaque(false);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        lblUser.setText(u == null ? "" : (u.getFullName() != null ? u.getFullName() : u.getUsername()));
        lblUser.setFont(Theme.FONT_HEADER);
        lblUser.setForeground(Color.WHITE);
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblRole.setText(u == null ? "" : (u.getRole() != null ? "[" + u.getRole() + "]" : ""));
        lblRole.setFont(Theme.FONT_SMALL);
        lblRole.setForeground(new Color(0x94A3B8));
        lblRole.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.add(lblUser);
        info.add(lblRole);

        row.add(avatar);
        row.add(info);
        box.add(row, BorderLayout.NORTH);

        JButton btnLogout = ModernComponents.dangerButton("Đăng xuất");
        btnLogout.setMargin(new Insets(8, 0, 8, 0));
        btnLogout.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnLogout.addActionListener(e -> {
            AuthService.get().logout();
            dispose();
            new LoginFrame().setVisible(true);
        });
        JPanel btnWrap = new JPanel();
        btnWrap.setOpaque(false);
        btnWrap.setLayout(new BoxLayout(btnWrap, BoxLayout.Y_AXIS));
        btnLogout.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnWrap.add(btnLogout);
        box.add(btnWrap, BorderLayout.SOUTH);

        return box;
    }

    /** Main area: topbar (search + clock) + content card. */
    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);

        // Top bar
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.CARD_BG);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));

        JTextField search = ModernComponents.searchField();
        top.add(search, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);
        lblClock.setFont(Theme.FONT_NORMAL);
        lblClock.setForeground(Theme.TEXT_MUTED);
        right.add(lblClock);
        top.add(right, BorderLayout.EAST);

        main.add(top, BorderLayout.NORTH);

        // Content area
        JPanel contentWrap = new JPanel(new BorderLayout());
        contentWrap.setBackground(Theme.BG);
        contentWrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        contentPanel.setBackground(Theme.BG);
        contentPanel.setOpaque(true);
        contentWrap.add(contentPanel, BorderLayout.CENTER);
        main.add(contentWrap, BorderLayout.CENTER);

        return main;
    }

    /* ===================== MENU ===================== */
    private void initMenu() {
        boolean admin = AuthService.get().isAdmin();
        addMenu("Tổng quan",           IconKind.DASHBOARD, new InventoryReportPanel());
        addMenu("Sản phẩm",            IconKind.PRODUCT,   new ProductListPanel());
        addMenu("Danh mục",            IconKind.CATEGORY,  new CategoryListPanel());
        addMenu("Nhà cung cấp",        IconKind.SUPPLIER,  new SupplierListPanel());
        addMenu("Khách hàng",          IconKind.CUSTOMER,  new CustomerListPanel());
        addMenu("Phiếu nhập",          IconKind.IMPORT,    new ImportPanel());
        addMenu("Phiếu xuất",          IconKind.EXPORT,    new ExportPanel());
        addMenu("Lịch sử nhập / xuất", IconKind.HISTORY,   new TransactionHistoryPanel());
        addMenu("Hóa đơn bán hàng",    IconKind.INVOICE,   new InvoicePanel());
        addMenu("Báo cáo doanh thu",   IconKind.REPORT,    new RevenueReportPanel());
        addMenu("Top sản phẩm bán chạy", IconKind.STAR,    new TopProductsPanel());
        addMenu("Công nợ khách hàng",   IconKind.DEBT,     new DebtReportPanel());
        addMenu("Đơn đặt hàng (PO)",    IconKind.ORDER,    new PurchaseOrderPanel());
        if (admin) {
            addMenu("Quản lý người dùng", IconKind.USER,    new UserManagementPanel());
            addMenu("Lịch sử thao tác",   IconKind.HISTORY,  new AuditLogPanel());
        }
        menuList.setSelectedIndex(0);
    }

    private void addMenu(String label, IconKind icon, JPanel panel) {
        menuModel.addElement(new MenuItem(label, icon));
        menuOrder.add(label);
        keyToCard.put(label, label);
        contentPanel.add(panel, label);
        if (panel instanceof com.utc.warehouse.ui.common.BasePanel bp) {
            bp.init();
            bp.reloadDataOnShow(); // Auto-load data khi panel lần đầu hiển thị
        }
        // Bind Ctrl+9 cho menu cuối (thường là User Management, nếu có)
        int idx = menuOrder.size(); // 1-based sau khi add
        if (idx <= 9) {
            final int i = idx - 1;
            globalSc.bind("ctrl " + idx, "go" + idx, () -> switchMenu(i), "Mở: " + label);
        }
    }

    private String firstKey() { return menuModel.get(0).label; }

    /* ===================== Helpers ===================== */
    private void startClock() {
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy - HH:mm");
        Timer t = new Timer(1000, e -> {
            lblClock.setText(java.time.LocalDateTime.now().format(fmt));
        });
        t.setInitialDelay(0);
        t.start();
    }

    /** Tạo JMenuBar với File / View / Help. Mỗi item có accelerator. */
    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.setBackground(Theme.CARD_BG);
        mb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        JMenu mFile = new JMenu("Hệ thống");
        mFile.setMnemonic('H');
        addMenuItem(mFile, "Đăng xuất", 'L', this::doLogout);
        mFile.addSeparator();
        addMenuItem(mFile, "Thoát", 'Q', () -> HibernateShutdown.shutdownAndExit(0));
        mb.add(mFile);

        JMenu mView = new JMenu("Xem");
        mView.setMnemonic('X');
        for (int i = 0; i < menuOrder.size() && i < 9; i++) {
            String label = menuOrder.get(i);
            final int idx = i;
            JMenuItem mi = new JMenuItem((i + 1) + ". " + label);
            mi.setAccelerator(KeyStroke.getKeyStroke("ctrl " + (i + 1)));
            mi.addActionListener(e -> switchMenu(idx));
            mView.add(mi);
        }
        mb.add(mView);

        JMenu mHelp = new JMenu("Trợ giúp");
        mHelp.setMnemonic('T');
        addMenuItem(mHelp, "Phím tắt (F1)", 'P', this::showHelpDialog);
        addMenuItem(mHelp, "Giới thiệu", 'G', this::showAbout);
        mb.add(mHelp);
        return mb;
    }

    private void addMenuItem(JMenu menu, String text, char mnemonic, Runnable r) {
        JMenuItem mi = new JMenuItem(text, mnemonic);
        mi.addActionListener(e -> r.run());
        // Gán accelerator thông minh dựa trên tên
        switch (text) {
            case "Đăng xuất" -> mi.setAccelerator(KeyStroke.getKeyStroke("ctrl L"));
            case "Thoát"     -> mi.setAccelerator(KeyStroke.getKeyStroke("ctrl Q"));
            case "Phím tắt (F1)" -> mi.setAccelerator(KeyStroke.getKeyStroke("F1"));
        }
        menu.add(mi);
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "Quản lý Kho - Warehouse Management\n" +
                "Phiên bản 1.0.0\n\n" +
                "Java Swing + Hibernate/JPA + MySQL\n" +
                "© 2026 UTC",
                "Giới thiệu", JOptionPane.INFORMATION_MESSAGE);
    }

    /** DTO cho menu item (label + icon). */
    private record MenuItem(String label, IconKind icon) { }

    private enum IconKind {
        DASHBOARD, PRODUCT, CATEGORY, SUPPLIER, CUSTOMER, IMPORT, EXPORT, HISTORY, USER,
        INVOICE, REPORT, STAR, DEBT, ORDER
    }

    /** Custom renderer vẽ icon SVG-like + highlight khi selected. */
    private class SidebarRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            MenuItem m = (MenuItem) value;
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 12));

            // Icon canvas
            JLabel iconLbl = new JLabel(new VectorIcon(m.icon, isSelected ? Color.WHITE : new Color(0xB0BEC5)));
            iconLbl.setPreferredSize(new Dimension(20, 20));
            row.add(iconLbl, BorderLayout.WEST);

            JLabel lbl = new JLabel(m.label);
            lbl.setFont(Theme.FONT_NORMAL);
            lbl.setForeground(isSelected ? Color.WHITE : new Color(0xCBD5E1));
            row.add(lbl, BorderLayout.CENTER);

            // Indicator thẳng đứng khi selected
            JPanel indicator = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    if (isSelected) {
                        g.setColor(Theme.BRAND);
                        g.fillRect(0, 0, 3, getHeight());
                    }
                }
            };
            indicator.setOpaque(false);
            indicator.setPreferredSize(new Dimension(3, 30));

            JPanel wrapper = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    if (isSelected) {
                        g.setColor(Theme.SIDEBAR_ACTIVE);
                    } else if (cellHasFocus) {
                        g.setColor(Theme.SIDEBAR_HOVER);
                    } else {
                        g.setColor(Theme.SIDEBAR_BG);
                    }
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            wrapper.setOpaque(false);
            wrapper.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            wrapper.add(indicator, BorderLayout.WEST);
            JPanel inner = new JPanel(new BorderLayout());
            inner.setOpaque(false);
            inner.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
            inner.add(row, BorderLayout.CENTER);
            wrapper.add(inner, BorderLayout.CENTER);
            return wrapper;
        }
    }

    /** Icon vector đơn giản cho menu. */
    private static class VectorIcon implements Icon {
        private final IconKind kind;
        private final Color color;
        VectorIcon(IconKind k, Color c) { this.kind = k; this.color = c; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f));
            int s = 18, pad = 1;
            int x0 = x + pad, y0 = y + pad;
            switch (kind) {
                case DASHBOARD -> {
                    g2.drawRoundRect(x0, y0, s, s, 3, 3);
                    g2.drawLine(x0, y0 + s/2, x0 + s, y0 + s/2);
                    g2.drawLine(x0 + s/2, y0, x0 + s/2, y0 + s);
                }
                case PRODUCT -> {
                    g2.drawRect(x0, y0, s, s);
                    g2.drawLine(x0, y0 + 5, x0 + s, y0 + 5);
                }
                case CATEGORY -> {
                    g2.drawOval(x0, y0, s, s);
                    g2.drawLine(x0, y0 + s/2, x0 + s, y0 + s/2);
                    g2.drawLine(x0 + s/2, y0, x0 + s/2, y0 + s);
                }
                case SUPPLIER -> {
                    g2.drawRect(x0, y0, 11, s);
                    g2.drawLine(x0 + 3, y0 + 5, x0 + 8, y0 + 5);
                    g2.drawLine(x0 + 3, y0 + 9, x0 + 8, y0 + 9);
                    g2.drawLine(x0 + 3, y0 + 13, x0 + 8, y0 + 13);
                }
                case CUSTOMER -> {
                    g2.drawOval(x0 + 5, y0, 8, 8);
                    g2.drawArc(x0, y0 + 9, s, 12, 0, -180);
                }
                case IMPORT -> {
                    g2.drawRect(x0, y0, s, s);
                    g2.drawLine(x0 + 4, y0 + 5, x0 + 14, y0 + 5);
                    g2.drawLine(x0 + 4, y0 + 9, x0 + 14, y0 + 9);
                    g2.drawLine(x0 + 4, y0 + 13, x0 + 14, y0 + 13);
                }
                case EXPORT -> {
                    g2.drawRect(x0, y0, s, s);
                    g2.drawLine(x0 + 4, y0 + 5, x0 + 14, y0 + 5);
                    g2.drawLine(x0 + 4, y0 + 9, x0 + 14, y0 + 9);
                    g2.drawLine(x0 + 4, y0 + 13, x0 + 14, y0 + 13);
                }
                case HISTORY -> {
                    g2.drawOval(x0, y0, s, s);
                    g2.drawLine(x0 + s/2, y0 + 3, x0 + s/2, y0 + s/2);
                    g2.drawLine(x0 + s/2, y0 + s/2, x0 + s - 2, y0 + s/2);
                }
                case USER -> {
                    g2.drawOval(x0 + 5, y0, 8, 8);
                    g2.drawArc(x0, y0 + 8, s, 14, 0, -180);
                }
                case INVOICE -> {
                    g2.drawRect(x0 + 1, y0, s - 2, s);
                    g2.drawLine(x0 + 3, y0 + 5, x0 + s - 3, y0 + 5);
                    g2.drawLine(x0 + 3, y0 + 9, x0 + s - 3, y0 + 9);
                    g2.drawLine(x0 + 3, y0 + 13, x0 + s - 3, y0 + 13);
                }
                case REPORT -> {
                    g2.drawRect(x0, y0 + 2, s, s - 4);
                    g2.setStroke(new BasicStroke(1.8f));
                    int baseY = y0 + s - 2;
                    int[] xs = {x0 + 2, x0 + 6, x0 + 10, x0 + 14};
                    int[] ys = {y0 + 10, y0 + 6, y0 + 8, y0 + 3};
                    g2.drawPolyline(xs, ys, 4);
                }
                case STAR -> {
                    int cx = x0 + s / 2, cy = y0 + s / 2;
                    int r = s / 2;
                    java.awt.Polygon p = new java.awt.Polygon();
                    for (int i = 0; i < 5; i++) {
                        double a = -Math.PI / 2 + i * 2 * Math.PI / 5;
                        p.addPoint(cx + (int) (r * Math.cos(a)), cy + (int) (r * Math.sin(a)));
                    }
                    g2.drawPolygon(p);
                }
                case DEBT -> {
                    g2.drawOval(x0, y0, s, s);
                    g2.drawLine(x0 + 2, y0 + s / 2, x0 + s - 2, y0 + s / 2);
                    g2.drawLine(x0 + s / 2, y0 + 2, x0 + s / 2, y0 + s - 2);
                }
                case ORDER -> {
                    g2.drawRect(x0, y0 + 2, s - 2, s - 4);
                    g2.setStroke(new BasicStroke(1.6f));
                    g2.drawLine(x0 + 2, y0 + 5, x0 + s - 4, y0 + 5);
                    g2.drawLine(x0 + 2, y0 + 9, x0 + s - 4, y0 + 9);
                    g2.drawLine(x0 + 2, y0 + 13, x0 + s / 2, y0 + 13);
                }
            }
            g2.dispose();
        }
        @Override public int getIconWidth()  { return 20; }
        @Override public int getIconHeight() { return 20; }
    }
}
