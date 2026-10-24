package com.utc.warehouse.ui.stock;

import com.utc.warehouse.dao.StockTransactionDAO;
import com.utc.warehouse.entity.StockTransaction;
import com.utc.warehouse.entity.StockTransactionDetail;
import com.utc.warehouse.enums.TransactionType;
import com.utc.warehouse.service.StockService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.ShortcutManager;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.UIConstants;
import com.utc.warehouse.util.DateFormatUtil;
import com.utc.warehouse.util.MoneyFormat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class TransactionHistoryPanel extends BasePanel {

    private final StockService service = new StockService();
    private final StockTransactionDAO txDAO = new StockTransactionDAO();
    private final Object[] COLS = {"#", "Loại", "Mã phiếu", "Đối tác", "Người tạo", "Ngày tạo", "Số dòng", "Tổng tiền", "Ghi chú"};
    private JTable table;
    private final JComboBox<String> cbType = new JComboBox<>(new String[]{"Tất cả", "Nhập", "Xuất"});

    public TransactionHistoryPanel() { super("Lịch sử Nhập/Xuất"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JButton btnRefresh = primaryButton("Làm mới");
        JButton btnDetail  = secondaryButton("Xem chi tiết");
        bar.add(new JLabel("Loại:"));
        bar.add(cbType);
        bar.add(btnRefresh);
        bar.add(btnDetail);

        btnRefresh.addActionListener(e -> reload());
        btnDetail.addActionListener(e -> viewDetail());
        cbType.addActionListener(e -> reload());
        btnRefresh.setName("btnRefresh");
        btnDetail.setName("btnDetail");
        return bar;
    }

    @Override protected void onShortcutRefresh() { reload(); }
    @Override protected void installDefaultShortcuts() {
        ShortcutManager sc = new ShortcutManager(this);
        sc.bind("F5",        "refresh", this::reload,      "Làm mới");
        sc.bind("ctrl D",    "detail",  this::viewDetail, "Xem chi tiết phiếu đang chọn");
        sc.bind("ENTER",     "open",    this::viewDetail, "Mở chi tiết");
    }

    @Override
    protected JComponent buildContent() {
        table = TableUtils.createTable(COLS, new Object[0][]);
        TableUtils.centerColumn(table, 0);
        TableUtils.centerColumn(table, 1);
        TableUtils.centerColumn(table, 2);
        TableUtils.centerColumn(table, 6);

        // Double-click 1 dòng → xem chi tiết phiếu nhập/xuất
        com.utc.warehouse.ui.common.TableDoubleClick.installOn(table, r -> viewDetail());
        return TableUtils.scrollFor(table);
    }

    public void reload() {
        try {
            List<StockTransaction> list;
            String sel = (String) cbType.getSelectedItem();
            if ("Nhập".equals(sel))      list = service.findByType(TransactionType.IMPORT);
            else if ("Xuất".equals(sel)) list = service.findByType(TransactionType.EXPORT);
            else                          list = service.all();

            DefaultTableModel m = TableUtils.modelOf(table);
            m.setRowCount(0);
            int idx = 1;
            for (StockTransaction t : list) {
                // details + product đã được JOIN FETCH ở DAO → an toàn truy cập ngoài session
                List<StockTransactionDetail> details = t.getDetails();
                int detailsSize = details == null ? 0 : details.size();
                BigDecimal total = details == null ? BigDecimal.ZERO
                        : details.stream()
                                .map(StockTransactionDetail::getLineTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                m.addRow(new Object[]{
                        idx++,
                        t.getType() == TransactionType.IMPORT ? "Nhập" : "Xuất",
                        "#" + t.getId(),
                        partnerName(t),
                        t.getUser() == null ? "" : t.getUser().getUsername(),
                        DateFormatUtil.format(t.getCreatedAt()),
                        detailsSize,
                        MoneyFormat.format(total),
                        t.getNote() == null ? "" : t.getNote()
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String partnerName(StockTransaction t) {
        // Không join; hiển thị ID + loại (đủ cho lịch sử, nếu cần tên có thể JOIN thêm)
        if (t.getPartnerId() == null) return "-";
        return (t.getPartnerType() == null ? "" : t.getPartnerType()) + " #" + t.getPartnerId();
    }

    private void viewDetail() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn 1 phiếu."); return; }
        int modelRow = table.convertRowIndexToModel(row);

        // Lấy ID thật (chỉ số nguyên) — cột 2 hiển thị "#5", parse an toàn
        Integer id = extractId(modelRow);
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Không lấy được mã phiếu.");
            return;
        }

        // Dùng DAO riêng để fetch lại 1 phiếu kèm đầy đủ details + product
        StockTransaction t = txDAO.findByIdWithDetails(id);
        if (t == null) {
            JOptionPane.showMessageDialog(this, "Phiếu #" + id + " không còn tồn tại.");
            return;
        }

        com.utc.warehouse.ui.common.DetailDialogs.showTransactionDetail(
                SwingUtilities.getWindowAncestor(this), t);
    }

    /** Parse an toàn ID từ cột "Mã phiếu" (định dạng "#5") hoặc từ cột "#" (idx). */
    private Integer extractId(int modelRow) {
        DefaultTableModel m = TableUtils.modelOf(table);
        // Ưu tiên cột 2 (Mã phiếu kiểu "#5")
        try {
            Object v = m.getValueAt(modelRow, 2);
            if (v != null) {
                String s = v.toString().replace("#", "").trim();
                if (!s.isEmpty()) return Integer.valueOf(s);
            }
        } catch (NumberFormatException ignored) { }
        // Fallback: dùng 1 cột khác? Không có, trả null.
        return null;
    }

    private JButton primaryButton(String t)   { return styleButton(t, UIConstants.PRIMARY, Color.WHITE); }
    private JButton secondaryButton(String t) { return styleButton(t, UIConstants.ACCENT, Color.WHITE); }
    private JButton styleButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg); b.setForeground(fg);
        b.setFocusPainted(false);
        b.setFont(UIConstants.FONT_NORMAL);
        b.setPreferredSize(new Dimension(120, 30));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
