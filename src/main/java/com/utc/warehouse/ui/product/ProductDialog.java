package com.utc.warehouse.ui.product;

import com.utc.warehouse.entity.Category;
import com.utc.warehouse.entity.Product;
import com.utc.warehouse.service.ImageService;
import com.utc.warehouse.service.ProductService;
import com.utc.warehouse.ui.common.Theme;
import com.utc.warehouse.util.MoneyFormat;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class ProductDialog extends JDialog {

    private final ProductService service = new ProductService();
    private final ImageService imageService = new ImageService();
    private final Product original;
    private boolean saved = false;

    /* ============== Form fields ============== */
    private final JTextField    txtSku      = new JTextField(15);
    private final JTextField    txtName     = new JTextField(20);
    private final JComboBox<Category> cbCategory = new JComboBox<>();
    private final JTextField    txtUnit     = new JTextField("cái", 10);
    private final JTextField    txtSalePrice = new JTextField("0", 12);
    private final JTextField    txtCostPrice = new JTextField("0", 12);
    private final JTextField    txtVatRate   = new JTextField("0", 6);
    private final JTextField    txtMinStock  = new JTextField("0", 5);
    private final JCheckBox     chkActive    = new JCheckBox("Đang kinh doanh", true);
    private final JLabel        lblProfit    = new JLabel(" ");

    /* ============== Image ============== */
    private final JLabel   lblImagePreview = new JLabel();
    private final JLabel   lblImageStatus  = new JLabel("(Chưa có ảnh)");
    private final JButton  btnChooseImage  = new JButton("Chọn ảnh…");
    private final JButton  btnRemoveImage  = new JButton("Xoá ảnh");
    /** Đường dẫn tương đối sẽ lưu vào DB sau khi Save. */
    private String pendingImagePath;     // filename mới upload
    private String originalImagePath;    // filename đã có khi sửa
    private String removeMark;           // nếu set = "" nghĩa là user muốn xoá ảnh cũ

    public ProductDialog(Window owner, Product product) {
        super(owner, product == null ? "Thêm sản phẩm" : "Sửa sản phẩm", ModalityType.APPLICATION_MODAL);
        this.original = product;
        if (original != null) originalImagePath = original.getImagePath();
        setSize(720, 580);
        setMinimumSize(new Dimension(640, 520));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        // Category combo
        List<Category> cats = service.allCategories();
        cbCategory.addItem(null);
        for (Category c : cats) cbCategory.addItem(c);
        cbCategory.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                    boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setText(value == null ? "(Không có)" : ((Category) value).getName());
                return l;
            }
        });

        add(buildFormPanel(), BorderLayout.CENTER);
        add(buildButtonPanel(), BorderLayout.SOUTH);

        // Live update lợi nhuận
        java.awt.event.KeyAdapter recalc = new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { recalcProfit(); }
        };
        txtSalePrice.addKeyListener(recalc);
        txtCostPrice.addKeyListener(recalc);

        if (original != null) fillFromOriginal();
        recalcProfit();
        refreshImagePreview();
    }

    /* ============== Layout: form trái + ảnh phải ============== */
    private JComponent buildFormPanel() {
        // ----- Trái: form -----
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        c.gridx = 0; c.gridy = row; form.add(label("SKU (*)"), c);
        c.gridx = 1; c.weightx = 1; form.add(txtSku, c); row++;
        c.weightx = 0;

        c.gridx = 0; c.gridy = row; form.add(label("Tên sản phẩm (*)"), c);
        c.gridx = 1; form.add(txtName, c); row++;

        c.gridx = 0; c.gridy = row; form.add(label("Danh mục"), c);
        c.gridx = 1; form.add(cbCategory, c); row++;

        c.gridx = 0; c.gridy = row; form.add(label("Đơn vị tính"), c);
        c.gridx = 1; form.add(txtUnit, c); row++;

        // Giá bán / Giá vốn / VAT
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        JPanel priceRow = new JPanel(new GridLayout(1, 3, 8, 0));
        priceRow.setOpaque(false);
        priceRow.add(buildField("Giá bán (VNĐ)", txtSalePrice));
        priceRow.add(buildField("Giá vốn (VNĐ)", txtCostPrice));
        priceRow.add(buildField("VAT %", txtVatRate));
        form.add(priceRow, c);
        row++;
        c.gridwidth = 1;

        lblProfit.setFont(Theme.FONT_NORMAL);
        lblProfit.setForeground(Theme.BRAND_DARK);
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(lblProfit, c); row++;
        c.gridwidth = 1;

        c.gridx = 0; c.gridy = row; form.add(label("Tồn tối thiểu"), c);
        c.gridx = 1; form.add(txtMinStock, c); row++;

        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        form.add(chkActive, c);
        row++;
        c.gridwidth = 1;

        // ----- Phải: ảnh -----
        JPanel imagePanel = new JPanel(new BorderLayout(8, 8));
        imagePanel.setBackground(Color.WHITE);
        imagePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Ảnh sản phẩm"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        imagePanel.setPreferredSize(new Dimension(280, 0));

        lblImagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagePreview.setVerticalAlignment(SwingConstants.CENTER);
        lblImagePreview.setPreferredSize(new Dimension(240, 240));
        lblImagePreview.setBorder(new LineBorder(Theme.BORDER, 1, true));
        lblImagePreview.setBackground(new Color(0xF8FAFC));
        lblImagePreview.setOpaque(true);
        imagePanel.add(lblImagePreview, BorderLayout.CENTER);

        JPanel imgBtns = new JPanel(new GridLayout(1, 2, 6, 0));
        imgBtns.setOpaque(false);
        styleImgBtn(btnChooseImage, Theme.BRAND);
        styleImgBtn(btnRemoveImage, Theme.DANGER);
        btnChooseImage.addActionListener(e -> onChooseImage());
        btnRemoveImage.addActionListener(e -> onRemoveImage());
        imgBtns.add(btnChooseImage);
        imgBtns.add(btnRemoveImage);
        imagePanel.add(imgBtns, BorderLayout.NORTH);

        lblImageStatus.setFont(Theme.FONT_SMALL);
        lblImageStatus.setForeground(Theme.TEXT_MUTED);
        lblImageStatus.setHorizontalAlignment(SwingConstants.CENTER);
        imagePanel.add(lblImageStatus, BorderLayout.SOUTH);

        // Split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, form, imagePanel);
        split.setResizeWeight(0.65);
        split.setBorder(null);
        split.setOpaque(false);
        return split;
    }

    private JComponent buildButtonPanel() {
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton btnSave = new JButton("Lưu");
        btnSave.setBackground(Theme.BRAND); btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.setFont(Theme.FONT_HEADER);
        btnSave.setPreferredSize(new Dimension(100, 34));
        JButton btnCancel = new JButton("Hủy");
        btnCancel.setPreferredSize(new Dimension(80, 34));
        btnSave.addActionListener(e -> onSave());
        btnCancel.addActionListener(e -> dispose());
        btns.add(btnCancel);
        btns.add(btnSave);
        return btns;
    }

    private void styleImgBtn(JButton b, Color bg) {
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_NORMAL);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void fillFromOriginal() {
        txtSku.setText(original.getSku());
        txtName.setText(original.getName());
        cbCategory.setSelectedItem(original.getCategory());
        txtUnit.setText(original.getUnit());
        txtSalePrice.setText(original.getSalePrice() == null ? "0" : original.getSalePrice().toPlainString());
        txtCostPrice.setText(original.getCostPrice() == null ? "0" : original.getCostPrice().toPlainString());
        txtVatRate.setText(original.getVatRate() == null ? "0" : original.getVatRate().toPlainString());
        txtMinStock.setText(String.valueOf(original.getMinStock()));
        chkActive.setSelected(original.getActive() == null || original.getActive());
    }

    /* ============== Image handlers ============== */
    private void onChooseImage() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Chọn ảnh sản phẩm");
        fc.setFileFilter(new FileNameExtensionFilter(
                "Ảnh (jpg, png, gif, webp, bmp)",
                "jpg", "jpeg", "png", "gif", "webp", "bmp"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File src = fc.getSelectedFile();
        try {
            // Nếu đã có ảnh pending (chưa lưu) thì xoá file tạm để tránh rác
            if (pendingImagePath != null) {
                imageService.delete(pendingImagePath);
                pendingImagePath = null;
            }
            String saved = imageService.save(src);
            pendingImagePath = saved;
            removeMark = null;   // reset lại cờ xoá
            refreshImagePreview();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi tải ảnh: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRemoveImage() {
        if (pendingImagePath != null) {
            // Vừa upload ở session này → xoá file tạm luôn
            imageService.delete(pendingImagePath);
            pendingImagePath = null;
        } else if (originalImagePath != null) {
            // Ảnh đã lưu → đánh dấu sẽ xoá khi save
            removeMark = "";
        }
        refreshImagePreview();
    }

    /** Vẽ preview hiện tại (pending → original → default). */
    private void refreshImagePreview() {
        String current = pendingImagePath;
        if (current == null && removeMark == null) current = originalImagePath;

        if (current == null) {
            lblImagePreview.setIcon(null);
            lblImagePreview.setText(" ");
            lblImagePreview.setBackground(new Color(0xF8FAFC));
            lblImageStatus.setText("(Chưa có ảnh)");
            return;
        }
        BufferedImage img = imageService.load(current);
        if (img == null) {
            lblImagePreview.setIcon(null);
            lblImageStatus.setText("⚠ Không đọc được file ảnh");
            return;
        }
        Image scaled = scaleTo(img, 240, 240);
        lblImagePreview.setIcon(new ImageIcon(scaled));
        lblImagePreview.setText("");
        lblImagePreview.setBackground(Color.WHITE);
        java.nio.file.Path p = imageService.resolve(current);
        long size = p == null ? 0 : p.toFile().length();
        lblImageStatus.setText("📷 " + current + "  (" + formatSize(size) + ")");
    }

    private static Image scaleTo(BufferedImage src, int maxW, int maxH) {
        double scale = Math.min((double) maxW / src.getWidth(), (double) maxH / src.getHeight());
        if (scale >= 1.0) return src;
        int nw = (int) Math.round(src.getWidth() * scale);
        int nh = (int) Math.round(src.getHeight() * scale);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private static String formatSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return (b / 1024) + " KB";
        return String.format("%.1f MB", b / 1024.0 / 1024.0);
    }

    /* ============== Lợi nhuận ============== */
    private JPanel buildField(String lbl, JTextField tf) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(lbl);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(tf, BorderLayout.CENTER);
        return p;
    }

    private void recalcProfit() {
        try {
            BigDecimal sale = new BigDecimal(txtSalePrice.getText().trim().replace(",", ""));
            BigDecimal cost = new BigDecimal(txtCostPrice.getText().trim().replace(",", ""));
            BigDecimal profit = sale.subtract(cost);
            BigDecimal margin = sale.signum() == 0 ? BigDecimal.ZERO
                    : profit.multiply(BigDecimal.valueOf(100)).divide(sale, 2, RoundingMode.HALF_UP);
            lblProfit.setText(String.format("💰 Lợi nhuận đơn vị: %s đ   |   Biên LN: %s %%",
                    MoneyFormat.format(profit),
                    margin.toPlainString()));
            lblProfit.setForeground(profit.signum() >= 0 ? Theme.BRAND : Theme.DANGER);
        } catch (Exception ex) {
            lblProfit.setText(" ");
        }
    }

    private JLabel label(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.FONT_NORMAL);
        return l;
    }

    /* ============== Save ============== */
    private void onSave() {
        String sku = txtSku.getText().trim();
        String name = txtName.getText().trim();
        if (sku.isEmpty()) { JOptionPane.showMessageDialog(this, "SKU không được trống."); return; }
        if (name.isEmpty()) { JOptionPane.showMessageDialog(this, "Tên sản phẩm không được trống."); return; }

        BigDecimal salePrice, costPrice, vatRate;
        int minStock;
        try { salePrice = new BigDecimal(txtSalePrice.getText().trim().replace(",", "")); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Giá bán không hợp lệ."); return; }
        try { costPrice = new BigDecimal(txtCostPrice.getText().trim().replace(",", "")); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Giá vốn không hợp lệ."); return; }
        try { vatRate = new BigDecimal(txtVatRate.getText().trim().replace(",", "")); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "VAT không hợp lệ."); return; }
        try { minStock = Integer.parseInt(txtMinStock.getText().trim()); }
        catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Tồn tối thiểu không hợp lệ."); return; }

        if (salePrice.signum() < 0 || costPrice.signum() < 0) {
            JOptionPane.showMessageDialog(this, "Giá không được âm."); return;
        }
        if (vatRate.signum() < 0 || vatRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            JOptionPane.showMessageDialog(this, "VAT phải trong khoảng 0-100%."); return;
        }

        try {
            Product p = (original == null) ? new Product() : original;
            p.setSku(sku);
            p.setName(name);
            p.setCategory((Category) cbCategory.getSelectedItem());
            p.setUnit(txtUnit.getText().trim().isEmpty() ? "cái" : txtUnit.getText().trim());
            p.setSalePrice(salePrice);
            if (original == null) p.setCostPrice(costPrice);
            p.setVatRate(vatRate);
            p.setMinStock(minStock);
            p.setActive(chkActive.isSelected());

            // Quyết định imagePath cuối cùng
            String newImagePath;
            if (pendingImagePath != null) {
                newImagePath = pendingImagePath;
            } else if (removeMark != null) {
                newImagePath = null;
            } else {
                newImagePath = original == null ? null : original.getImagePath();
            }
            p.setImagePath(newImagePath);

            if (original == null) service.save(p);
            else service.update(p);

            // Sau khi DB lưu xong: dọn ảnh cũ nếu bị thay thế
            if (originalImagePath != null
                    && !originalImagePath.equals(newImagePath)) {
                imageService.delete(originalImagePath);
            }
            JOptionPane.showMessageDialog(this, "Lưu thành công!");
            saved = true;
            dispose();
        } catch (Exception ex) {
            // Rollback file pending nếu DB lỗi
            if (pendingImagePath != null) {
                imageService.delete(pendingImagePath);
            }
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() { return saved; }
}
