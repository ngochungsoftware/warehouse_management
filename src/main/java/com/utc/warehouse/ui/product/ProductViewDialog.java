package com.utc.warehouse.ui.product;

import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.ImageService;
import com.utc.warehouse.ui.common.Theme;
import com.utc.warehouse.util.MoneyFormat;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Dialog chỉ-đọc để xem chi tiết 1 sản phẩm.
 * Click vào ảnh nhỏ sẽ phóng to ảnh trong cửa sổ riêng.
 */
public class ProductViewDialog extends JDialog {

    private final Product product;
    private final ImageService imageService = new ImageService();
    private final JLabel lblImage = new JLabel();
    private final JLabel lblImgHint = new JLabel("(Click ảnh để phóng to)", SwingConstants.CENTER);

    public ProductViewDialog(Window owner, Product product) {
        super(owner, "Chi tiết sản phẩm", ModalityType.APPLICATION_MODAL);
        this.product = product;
        setSize(720, 560);
        setMinimumSize(new Dimension(640, 500));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(),   BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        loadImage();
    }

    /* ==================== Header ==================== */
    private JComponent buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BRAND);
        p.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JLabel title = new JLabel("📦  " + nullSafe(product.getName()));
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        p.add(title, BorderLayout.WEST);
        JLabel sku = new JLabel("SKU: " + nullSafe(product.getSku()));
        sku.setFont(Theme.FONT_NORMAL);
        sku.setForeground(new Color(255, 255, 255, 220));
        p.add(sku, BorderLayout.EAST);
        return p;
    }

    /* ==================== Body: ảnh trái + info phải ==================== */
    private JComponent buildBody() {
        // ----- Ảnh -----
        lblImage.setHorizontalAlignment(SwingConstants.CENTER);
        lblImage.setVerticalAlignment(SwingConstants.CENTER);
        lblImage.setBackground(new Color(0xF8FAFC));
        lblImage.setOpaque(true);
        lblImage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblImage.setBorder(new LineBorder(Theme.BORDER, 1, true));
        lblImage.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openZoom(); }
        });

        lblImgHint.setFont(Theme.FONT_SMALL);
        lblImgHint.setForeground(Theme.TEXT_MUTED);

        JPanel imgPanel = new JPanel(new BorderLayout(0, 6));
        imgPanel.setBackground(Theme.CARD_BG);
        imgPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Ảnh sản phẩm"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        imgPanel.setPreferredSize(new Dimension(300, 0));
        imgPanel.add(lblImage, BorderLayout.CENTER);
        imgPanel.add(lblImgHint, BorderLayout.SOUTH);

        // ----- Thông tin -----
        JPanel info = new JPanel(new GridBagLayout());
        info.setBackground(Theme.CARD_BG);
        info.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Thông tin"),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 6, 5, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        addInfo(info, g, row++, "Danh mục",
                product.getCategory() == null ? "(Chưa phân loại)" : product.getCategory().getName());
        addInfo(info, g, row++, "Đơn vị tính", nullSafe(product.getUnit()));
        addInfo(info, g, row++, "Trạng thái",
                Boolean.TRUE.equals(product.getActive()) ? "Đang kinh doanh" : "Ngừng kinh doanh");

        row = addSeparator(info, g, row, "💰 Giá & Lợi nhuận");

        addInfo(info, g, row++, "Giá bán", MoneyFormat.format(safeBd(product.getSalePrice())) + " đ");
        addInfo(info, g, row++, "Giá vốn", MoneyFormat.format(safeBd(product.getCostPrice())) + " đ");

        BigDecimal profit = safeBd(product.getSalePrice()).subtract(safeBd(product.getCostPrice()));
        BigDecimal margin = safeBd(product.getSalePrice()).signum() == 0 ? BigDecimal.ZERO
                : profit.multiply(BigDecimal.valueOf(100))
                .divide(safeBd(product.getSalePrice()), 2, RoundingMode.HALF_UP);
        JLabel profitLbl = new JLabel(MoneyFormat.format(profit) + " đ  (" + margin + "%)");
        profitLbl.setFont(Theme.FONT_HEADER);
        profitLbl.setForeground(profit.signum() >= 0 ? Theme.BRAND : Theme.DANGER);
        addInfo(info, g, row++, "Lợi nhuận / đơn vị", profitLbl);

        addInfo(info, g, row++, "VAT", safeBd(product.getVatRate()) + " %");

        row = addSeparator(info, g, row, "📊 Kho");

        addInfo(info, g, row++, "Tồn kho hiện tại", String.valueOf(product.getTotalImportedQty()));
        addInfo(info, g, row++, "Tồn tối thiểu", String.valueOf(product.getMinStock()));
        if (product.getTotalImportedQty() < product.getMinStock()) {
            JLabel warn = new JLabel("⚠ Dưới mức tối thiểu");
            warn.setFont(Theme.FONT_HEADER);
            warn.setForeground(Theme.DANGER);
            addInfo(info, g, row++, "Cảnh báo", warn);
        }

        // ----- Split -----
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imgPanel, wrapInCard(info));
        split.setResizeWeight(0.45);
        split.setBorder(null);
        split.setOpaque(false);
        return split;
    }

    /** Bọc info panel trong card để đồng bộ style với imgPanel. */
    private JComponent wrapInCard(JComponent c) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.CARD_BG);
        card.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        card.add(c, BorderLayout.CENTER);
        return card;
    }

    private void addInfo(JPanel p, GridBagConstraints g, int row, String label, String value) {
        addInfo(p, g, row, label, new JLabel(value));
    }

    private void addInfo(JPanel p, GridBagConstraints g, int row, String label, JLabel value) {
        value.setFont(Theme.FONT_NORMAL);
        value.setForeground(Theme.TEXT);
        JLabel l = new JLabel(label);
        l.setFont(Theme.FONT_NORMAL);
        l.setForeground(Theme.TEXT_MUTED);
        g.gridx = 0; g.gridy = row; g.weightx = 0; g.gridwidth = 1;
        p.add(l, g);
        g.gridx = 1; g.weightx = 1;
        p.add(value, g);
    }

    /** Thêm 1 dòng separator ngang với tiêu đề. Trả về row tiếp theo. */
    private int addSeparator(JPanel p, GridBagConstraints g, int row, String title) {
        JLabel l = new JLabel(title);
        l.setFont(Theme.FONT_HEADER);
        l.setForeground(Theme.BRAND_DARK);
        l.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        g.gridx = 0; g.gridy = row; g.gridwidth = 2; g.weightx = 1;
        p.add(l, g);
        g.gridwidth = 1;
        return row + 1;
    }

    /* ==================== Footer ==================== */
    private JComponent buildFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        p.setBackground(Theme.BG);
        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(Theme.FONT_HEADER);
        btnClose.setPreferredSize(new Dimension(90, 32));
        btnClose.addActionListener(e -> dispose());
        p.add(btnClose);
        return p;
    }

    /* ==================== Ảnh ==================== */
    private void loadImage() {
        String path = product.getImagePath();
        if (path == null || path.isBlank()) {
            lblImage.setIcon(null);
            lblImage.setText("📷\n(Chưa có ảnh)");
            lblImage.setFont(Theme.FONT_HEADER);
            lblImage.setForeground(Theme.TEXT_MUTED);
            lblImage.setCursor(Cursor.getDefaultCursor());
            lblImage.setEnabled(false);
            lblImgHint.setVisible(false);
            return;
        }
        BufferedImage img = imageService.load(path);
        if (img == null) {
            lblImage.setIcon(null);
            lblImage.setText("⚠ Không đọc được ảnh");
            lblImage.setForeground(Theme.WARNING);
            lblImage.setCursor(Cursor.getDefaultCursor());
            lblImage.setEnabled(false);
            lblImgHint.setVisible(false);
            return;
        }
        // Fit vào khung ~280x380
        int maxW = 280, maxH = 380;
        double scale = Math.min((double) maxW / img.getWidth(), (double) maxH / img.getHeight());
        int nw = Math.max(1, (int) Math.round(img.getWidth() * scale));
        int nh = Math.max(1, (int) Math.round(img.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(img, 0, 0, nw, nh, null);
        } finally { g.dispose(); }
        lblImage.setIcon(new ImageIcon(scaled));
        lblImage.setText("");
    }

    /** Mở cửa sổ phóng to ảnh. */
    private void openZoom() {
        if (!lblImage.isEnabled()) return;
        String path = product.getImagePath();
        BufferedImage img = path == null ? null : imageService.load(path);
        if (img == null) return;

        JDialog zoom = new JDialog(this, "Ảnh: " + product.getName(), ModalityType.MODELESS);
        zoom.setUndecorated(false);
        JLabel big = new JLabel(new ImageIcon(img));
        big.setHorizontalAlignment(SwingConstants.CENTER);
        JScrollPane sp = new JScrollPane(big);
        sp.getViewport().setBackground(Theme.CARD_BG);
        zoom.add(sp);
        // Size vừa ảnh, tối đa 900x700
        int w = Math.min(900, img.getWidth() + 20);
        int h = Math.min(700, img.getHeight() + 60);
        zoom.setSize(w, h);
        zoom.setLocationRelativeTo(this);
        zoom.setVisible(true);
    }

    private static String nullSafe(String s) { return s == null || s.isBlank() ? "—" : s; }
    private static BigDecimal safeBd(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
