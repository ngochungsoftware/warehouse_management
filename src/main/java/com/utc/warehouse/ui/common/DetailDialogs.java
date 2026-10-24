package com.utc.warehouse.ui.common;

import com.utc.warehouse.entity.Customer;
import com.utc.warehouse.entity.Product;
import com.utc.warehouse.entity.PurchaseOrder;
import com.utc.warehouse.entity.PurchaseOrderItem;
import com.utc.warehouse.entity.StockTransaction;
import com.utc.warehouse.entity.StockTransactionDetail;
import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.enums.POStatus;
import com.utc.warehouse.enums.PartnerType;
import com.utc.warehouse.enums.TransactionType;
import com.utc.warehouse.service.PartnerService;
import com.utc.warehouse.util.DateFormatUtil;
import com.utc.warehouse.util.MoneyFormat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.math.BigDecimal;
import java.util.List;

/**
 * Các dialog xem chi tiết dùng chung — giao diện rõ ràng, có bố cục:
 *
 * <pre>
 *  ┌──────────────────────────────────────────────────────────┐
 *  │▌ PHIẾU NHẬP KHO                          [ NHẬP ]      │  header + badge
 *  │  Mã phiếu #12  ·  21/10/2026 09:15                      │
 *  ├──────────────────────────────────────────────────────────┤
 *  │  THÔNG TIN PHIẾU                                        │
 *  │  Loại phiếu    Nhập kho      Đối tác       NCC ABC      │
 *  │  Người tạo     admin        Ngày tạo      21/10/2026   │
 *  │  Ghi chú: nhập hàng đầu tiên trong tháng                │
 *  ├──────────────────────────────────────────────────────────┤
 *  │  CHI TIẾT SẢN PHẨM (3)                                  │
 *  │  ┌────────────────────────────────────────────────────┐  │
 *  │  │ # │ SKU  │ Tên sản phẩm   │ SL │ Đơn giá │ Thành tiền│ │
 *  │  └────────────────────────────────────────────────────┘  │
 *  ├──────────────────────────────────────────────────────────┤
 *  │                              TỔNG CỘNG     3.000.000 đ  │
 *  │                              [Xuất Excel]  [Đóng]        │
 *  └──────────────────────────────────────────────────────────┘
 * </pre>
 */
public final class DetailDialogs {

    private DetailDialogs() { }

    /* ================= PHIẾU NHẬP / XUẤT ================= */

    /** Mở dialog chi tiết cho 1 phiếu nhập/xuất kho. */
    public static void showTransactionDetail(Window owner, StockTransaction tx) {
        boolean isImport = tx.getType() == TransactionType.IMPORT;

        // ---- Bảng chi tiết sản phẩm ----
        String[] cols = {"#", "SKU", "Tên sản phẩm", "Đơn vị", "SL", "Đơn giá", "Thành tiền"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        BigDecimal total = BigDecimal.ZERO;
        int stt = 1;
        if (tx.getDetails() != null) {
            for (StockTransactionDetail d : tx.getDetails()) {
                Product p = d.getProduct();
                BigDecimal lineTotal = d.getLineTotal();
                total = total.add(lineTotal == null ? BigDecimal.ZERO : lineTotal);
                m.addRow(new Object[]{
                        stt++,
                        p == null ? "—" : p.getSku(),
                        p == null ? "(sản phẩm đã xoá)" : p.getName(),
                        p == null ? "" : p.getUnit(),
                        d.getQuantity(),
                        MoneyFormat.format(d.getUnitPrice()),
                        MoneyFormat.format(lineTotal)
                });
            }
        }

        JTable table = buildTable(cols, m);
        TableUtils.centerColumn(table, 0);
        TableUtils.centerColumn(table, 4);
        TableUtils.rightColumn(table, 5);
        TableUtils.rightColumn(table, 6);
        setColWidth(table, 0, 46);
        setColWidth(table, 1, 100);
        setColWidth(table, 2, 240);
        setColWidth(table, 3, 70);
        setColWidth(table, 4, 60);
        setColWidth(table, 5, 120);
        setColWidth(table, 6, 130);

        // ---- Thông tin phiếu ----
        String partnerName = resolvePartnerName(tx.getPartnerType(), tx.getPartnerId());
        String[][] info = {
                {"Loại phiếu",   isImport ? "Nhập kho" : "Xuất kho"},
                {"Đối tác",      partnerName},
                {"Người tạo",    tx.getUser() == null ? "(không rõ)" : tx.getUser().getUsername()},
                {"Ngày tạo",     DateFormatUtil.format(tx.getCreatedAt())},
                {"Số dòng SP",   String.valueOf(m.getRowCount())},
                {"Tổng tiền",    MoneyFormat.format(total)}
        };

        String note = (tx.getNote() == null || tx.getNote().isBlank()) ? null : tx.getNote();

        JDialog dlg = buildDialog(owner,
                isImport ? "Chi tiết phiếu nhập kho" : "Chi tiết phiếu xuất kho",
                "Mã phiếu #" + tx.getId() + "  ·  " + DateFormatUtil.format(tx.getCreatedAt()),
                isImport ? "NHẬP" : "XUẤT",
                isImport ? Theme.SUCCESS : Theme.ACCENT,
                info, note, table, total, m,
                "phieu_" + (isImport ? "nhap" : "xuat") + "_" + tx.getId());
        dlg.setVisible(true);
    }

    /* ================= ĐƠN ĐẶT HÀNG (PO) ================= */

    /** Mở dialog chi tiết cho 1 đơn đặt hàng (phiếu mua). */
    public static void showPurchaseOrderDetail(Window owner, PurchaseOrder po) {
        String[] cols = {"#", "SKU", "Tên sản phẩm", "Đơn vị", "Đặt", "Đã nhận", "Đơn giá", "Thành tiền"};
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        BigDecimal total = BigDecimal.ZERO;
        int stt = 1;
        int notReceived = 0;
        if (po.getItems() != null) {
            for (PurchaseOrderItem it : po.getItems()) {
                Product p = it.getProduct();
                BigDecimal lineTotal = it.getLineTotal();
                total = total.add(lineTotal == null ? BigDecimal.ZERO : lineTotal);
                boolean full = it.isFullyReceived();
                if (!full) notReceived++;
                m.addRow(new Object[]{
                        stt++,
                        p == null ? "—" : p.getSku(),
                        p == null ? "(sản phẩm đã xoá)" : p.getName(),
                        p == null ? "" : p.getUnit(),
                        it.getOrderedQty(),
                        it.getReceivedQty(),
                        MoneyFormat.format(it.getUnitPrice()),
                        MoneyFormat.format(lineTotal)
                });
            }
        }

        JTable table = buildTable(cols, m);
        TableUtils.centerColumn(table, 0);
        TableUtils.centerColumn(table, 4);
        TableUtils.centerColumn(table, 5);
        TableUtils.rightColumn(table, 6);
        TableUtils.rightColumn(table, 7);
        setColWidth(table, 0, 46);
        setColWidth(table, 1, 100);
        setColWidth(table, 2, 210);
        setColWidth(table, 3, 66);
        setColWidth(table, 4, 62);
        setColWidth(table, 5, 76);
        setColWidth(table, 6, 115);
        setColWidth(table, 7, 125);

        POStatus st = po.getStatus();
        String[][] info = {
                {"Mã đơn",        po.getCode() == null ? "—" : po.getCode()},
                {"Nhà cung cấp",  po.getSupplier() == null ? "(không có)" : po.getSupplier().getName()},
                {"Người tạo",     po.getUser() == null ? "(không rõ)" : po.getUser().getUsername()},
                {"Ngày tạo",      po.getCreatedAt() == null ? "—" : DateFormatUtil.formatDate(po.getCreatedAt())},
                {"Ngày dự kiến",  po.getExpectedDate() == null ? "—" : po.getExpectedDate().toString()},
                {"Phiếu nhập",    po.getReceivedTransactionId() == null ? "Chưa nhận hàng" : "#" + po.getReceivedTransactionId()}
        };

        String note = (po.getNote() == null || po.getNote().isBlank()) ? null : po.getNote();

        // Cảnh báo nếu còn dòng chưa nhận đủ
        String warn = null;
        if (notReceived > 0 && st != POStatus.CANCELLED) {
            warn = notReceived + " sản phẩm chưa nhận đủ số lượng đặt.";
        }

        JDialog dlg = buildDialog(owner,
                "Chi tiết đơn đặt hàng",
                (po.getCode() == null ? "" : po.getCode() + "  ·  ")
                        + (po.getSupplier() == null ? "" : po.getSupplier().getName()),
                statusVi(st), statusColor(st),
                info, note, table, total, m,
                "don_dat_hang_" + (po.getCode() == null ? po.getId() : po.getCode()),
                warn);
        dlg.setVisible(true);
    }

    /* ================= DỰNG DIALOG ================= */

    private static JDialog buildDialog(Window owner, String title, String subtitle,
                                       String badgeText, Color badgeColor,
                                       String[][] info, String note,
                                       JTable table, BigDecimal total,
                                       DefaultTableModel model, String exportName) {
        return buildDialog(owner, title, subtitle, badgeText, badgeColor,
                info, note, table, total, model, exportName, null);
    }

    private static JDialog buildDialog(Window owner, String title, String subtitle,
                                       String badgeText, Color badgeColor,
                                       String[][] info, String note,
                                       JTable table, BigDecimal total,
                                       DefaultTableModel model, String exportName,
                                       String warning) {
        JDialog dlg = new JDialog(owner, title, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setMinimumSize(new Dimension(820, 560));
        dlg.setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);

        root.add(buildHeader(title, subtitle, badgeText, badgeColor), BorderLayout.NORTH);

        // ---- Body: thông tin + bảng ----
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBackground(Theme.BG);
        body.setBorder(BorderFactory.createEmptyBorder(14, 18, 8, 18));

        JPanel infoCard = new JPanel(new BorderLayout(0, 8));
        infoCard.setBackground(Theme.CARD_BG);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        infoCard.add(sectionTitle("Thông tin"), BorderLayout.NORTH);

        JPanel infoWrap = new JPanel(new BorderLayout());
        infoWrap.setOpaque(false);
        infoWrap.add(infoGrid(info), BorderLayout.NORTH);
        if (note != null) infoWrap.add(noteRow(note), BorderLayout.CENTER);
        infoCard.add(infoWrap, BorderLayout.CENTER);

        JPanel tableCard = new JPanel(new BorderLayout(0, 8));
        tableCard.setBackground(Theme.CARD_BG);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        tableCard.add(sectionTitle("Chi tiết sản phẩm (" + model.getRowCount() + ")"), BorderLayout.NORTH);

        if (model.getRowCount() == 0) {
            tableCard.add(emptyState("Phiếu này chưa có sản phẩm nào."), BorderLayout.CENTER);
        } else {
            // ScrollPane mặc định có preferred height rất lớn (viewport ~400px) khiến
            // dialog pack() ra quá cao → chốt chiều cao theo số dòng thực tế.
            JScrollPane sp = TableUtils.scrollFor(table);
            int rows = Math.min(model.getRowCount(), 12);
            int height = 40 /* header */ + rows * table.getRowHeight() + 2;
            height = Math.max(height, 90);
            sp.setPreferredSize(new Dimension(tableWidth(table), height));
            tableCard.add(sp, BorderLayout.CENTER);
        }

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.add(infoCard, BorderLayout.NORTH);
        content.add(tableCard, BorderLayout.CENTER);
        body.add(content, BorderLayout.CENTER);

        if (warning != null) body.add(warningRow(warning), BorderLayout.SOUTH);

        root.add(body, BorderLayout.CENTER);
        root.add(buildFooter(total, model, exportName, dlg), BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        return dlg;
    }

    /* ================= HEADER ================= */

    private static JComponent buildHeader(String title, String subtitle, String badgeText, Color badgeColor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BRAND);
        p.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(Theme.FONT_TITLE);
        t.setForeground(Color.WHITE);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel s = new JLabel(subtitle == null ? "" : subtitle);
        s.setFont(Theme.FONT_NORMAL);
        s.setForeground(new Color(0xD6EDE0));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        left.add(t);
        left.add(s);

        p.add(left, BorderLayout.WEST);
        p.add(new Pill(badgeText, badgeColor), BorderLayout.EAST);
        return p;
    }

    /* ================= INFO GRID ================= */

    private static JComponent sectionTitle(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(Theme.FONT_HEADER);
        l.setForeground(Theme.TEXT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        return l;
    }

    private static JPanel infoGrid(String[][] pairs) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 12);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        for (int i = 0; i < pairs.length; i += 2) {
            c.gridy = row;
            c.gridx = 0; c.weightx = 0; p.add(infoLabel(pairs[i][0]), c);
            c.gridx = 1; c.weightx = 1; p.add(infoValue(pairs[i][1]), c);
            if (i + 1 < pairs.length) {
                c.gridx = 2; c.weightx = 0; p.add(infoLabel(pairs[i + 1][0]), c);
                c.gridx = 3; c.weightx = 1; p.add(infoValue(pairs[i + 1][1]), c);
            }
            row++;
        }
        return p;
    }

    private static JLabel infoLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_SMALL);
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    private static JLabel infoValue(String text) {
        JLabel l = new JLabel(text == null || text.isBlank() ? "—" : text);
        l.setFont(Theme.FONT_NORMAL);
        l.setForeground(Theme.TEXT);
        return l;
    }

    private static JComponent noteRow(String note) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JLabel lbl = new JLabel("Ghi chú");
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setVerticalAlignment(SwingConstants.TOP);

        JTextArea ta = new JTextArea(note);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setFont(Theme.FONT_NORMAL);
        ta.setForeground(Theme.TEXT);
        ta.setBackground(new Color(0xF8FAFC));
        ta.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        ta.setRows(2);

        p.add(lbl, BorderLayout.WEST);
        p.add(ta, BorderLayout.CENTER);
        return p;
    }

    private static JComponent warningRow(String warning) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(new Color(0xFEF3C7));
        p.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel l = new JLabel("⚠  " + warning);
        l.setFont(Theme.FONT_NORMAL);
        l.setForeground(new Color(0x92400E));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    /* ================= FOOTER ================= */

    private static JComponent buildFooter(BigDecimal total, DefaultTableModel model,
                                          String exportName, JDialog dlg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.CARD_BG);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        // Tổng tiền
        JPanel totalBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        totalBox.setOpaque(false);

        JLabel lbl = new JLabel("TỔNG CỘNG");
        lbl.setFont(Theme.FONT_HEADER);
        lbl.setForeground(Theme.TEXT);

        JLabel val = new JLabel(MoneyFormat.format(total));
        val.setFont(Theme.FONT_TITLE);
        val.setForeground(Theme.BRAND);

        totalBox.add(lbl);
        totalBox.add(val);
        p.add(totalBox, BorderLayout.WEST);

        // Nút
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);

        JButton btnExport = ModernComponents.secondaryButton("Xuất Excel");
        btnExport.addActionListener(e -> TableExportHelper.exportModel(model, (JComponent) dlg.getContentPane(), exportName));

        JButton btnClose = ModernComponents.primaryButton("Đóng");
        btnClose.addActionListener(e -> dlg.dispose());

        btns.add(btnExport);
        btns.add(btnClose);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    /* ================= PHẦN TỬ NHỎ ================= */

    /** Bảng dùng trong dialog (dùng chung style với app). */
    private static JTable buildTable(String[] cols, DefaultTableModel m) {
        JTable t = TableUtils.createTable(cols, new Object[0][]);
        // Giữ model gốc (đã có dữ liệu) thay vì model rỗng mới tạo
        t.setModel(m);
        t.setRowHeight(32);
        t.setAutoCreateRowSorter(false);
        t.setFillsViewportHeight(true);
        return t;
    }

    private static void setColWidth(JTable t, int col, int w) {
        if (col < t.getColumnCount()) {
            t.getColumnModel().getColumn(col).setPreferredWidth(w);
        }
    }

    /** Tổng chiều rộng ưu tiên của các cột (dùng làm chiều rộng dialog). */
    private static int tableWidth(JTable t) {
        int w = 0;
        for (int i = 0; i < t.getColumnCount(); i++) {
            w += t.getColumnModel().getColumn(i).getPreferredWidth();
        }
        return Math.max(w, 760);
    }

    /** Placeholder khi phiếu không có dòng sản phẩm nào. */
    private static JComponent emptyState(String message) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(760, 90));

        JLabel l = new JLabel(message);
        l.setFont(Theme.FONT_NORMAL);
        l.setForeground(Theme.TEXT_MUTED);
        p.add(l);
        return p;
    }

    /** Tra tên đối tác (NCC / KH) từ partnerId. */
    private static String resolvePartnerName(PartnerType type, Integer id) {
        if (id == null) return "—";
        PartnerService ps = new PartnerService();
        try {
            if (type == PartnerType.SUPPLIER) {
                List<Supplier> list = ps.allSuppliers();
                for (Supplier s : list) if (id.equals(s.getId())) return s.getName();
                return "NCC #" + id + " (đã xoá)";
            }
            if (type == PartnerType.CUSTOMER) {
                List<Customer> list = ps.allCustomers();
                for (Customer c : list) if (id.equals(c.getId())) return c.getName();
                return "KH #" + id + " (đã xoá)";
            }
        } catch (Exception ignored) { }
        return "#" + id;
    }

    public static String statusVi(POStatus s) {
        if (s == null) return "—";
        return switch (s) {
            case DRAFT     -> "NHÁP";
            case ORDERED   -> "ĐÃ ĐẶT";
            case PARTIAL   -> "NHẬN 1 PHẦN";
            case RECEIVED  -> "ĐÃ NHẬN ĐỦ";
            case CANCELLED -> "ĐÃ HỦY";
        };
    }

    public static Color statusColor(POStatus s) {
        if (s == null) return Theme.TEXT_MUTED;
        return switch (s) {
            case DRAFT     -> new Color(0x64748B);
            case ORDERED   -> Theme.ACCENT;
            case PARTIAL   -> Theme.WARNING;
            case RECEIVED  -> Theme.SUCCESS;
            case CANCELLED -> Theme.DANGER;
        };
    }

    /** Badge pill bo tròn hiển thị trạng thái. */
    private static class Pill extends JComponent {
        private final String text;
        private final Color bg;
        private final Color fg;

        Pill(String text, Color bg) {
            this.text = text == null ? "" : text;
            Color base = bg == null ? Theme.TEXT_MUTED : bg;

            // Badge nằm trên header màu BRAND (#1B7F4F). Nếu màu badge quá gần
            // màu header (vd SUCCESS #16A34A vs BRAND, tương phản chỉ ~1.5:1) thì
            // viền + chữ sẽ chìm vào nền → đổi sang nền trắng, chữ màu base.
            if (contrast(base, Theme.BRAND) < 2.5) {
                this.bg = Color.WHITE;
                this.fg = darken(base, 0.10f);
            } else {
                this.bg = base;
                this.fg = Color.WHITE;
            }

            setPreferredSize(new Dimension(
                    Math.max(74, getFontMetrics(Theme.FONT_HEADER).stringWidth(text) + 30), 30));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
            g2.setColor(fg);
            g2.setFont(Theme.FONT_HEADER);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            g2.drawString(text, (getWidth() - tw) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }

        private static float contrast(Color a, Color b) {
            float la = luminance(a), lb = luminance(b);
            float hi = Math.max(la, lb), lo = Math.min(la, lb);
            return (hi + 0.05f) / (lo + 0.05f);
        }

        private static float luminance(Color c) {
            float r = channel(c.getRed() / 255f);
            float g = channel(c.getGreen() / 255f);
            float b = channel(c.getBlue() / 255f);
            return 0.2126f * r + 0.7152f * g + 0.0722f * b;
        }

        private static float channel(float c) {
            return c <= 0.03928f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4);
        }

        private static Color darken(Color c, float amt) {
            return new Color(
                    Math.max(0, Math.min(255, (int) (c.getRed()   * (1 - amt)))),
                    Math.max(0, Math.min(255, (int) (c.getGreen() * (1 - amt)))),
                    Math.max(0, Math.min(255, (int) (c.getBlue()  * (1 - amt)))));
        }
    }
}
