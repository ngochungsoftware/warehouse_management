package com.utc.warehouse.ui.report;

import com.utc.warehouse.dao.FinanceReportDAO;
import com.utc.warehouse.service.FinanceReportService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.Theme;
import com.utc.warehouse.util.MoneyFormat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Báo cáo doanh thu / lợi nhuận theo khoảng thời gian.
 *  - Doanh thu (subtotal + VAT)
 *  - Giảm giá
 *  - Doanh thu thuần
 *  - Giá vốn hàng bán (COGS)
 *  - Lợi nhuận gộp + % margin
 */
public class RevenueReportPanel extends BasePanel {

    private final FinanceReportService service = new FinanceReportService();
    private final Object[] COLS = {"Ngày", "Số HĐ", "Doanh thu", "Giảm giá", "Doanh thu thuần",
            "Giá vốn (COGS)", "Lợi nhuận", "Margin%"};
    private JTable table;
    private JLabel lblSummary;
    private JTextField txtFrom, txtTo;
    private JComboBox<String> cbRange;
    private DefaultTableModel model;

    public RevenueReportPanel() { super("Báo cáo Doanh thu & Lợi nhuận"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        bar.add(new JLabel("Khoảng:"));
        cbRange = new JComboBox<>(new String[]{"7 ngày qua", "30 ngày qua", "Tháng này", "Tháng trước", "Tự chọn"});
        bar.add(cbRange);

        bar.add(new JLabel("Từ:"));
        txtFrom = new JTextField(10);
        bar.add(txtFrom);
        bar.add(new JLabel("Đến:"));
        txtTo = new JTextField(10);
        bar.add(txtTo);

        JButton btnApply = primaryButton("Xem");
        JButton btnRefresh = secondaryButton("Làm mới");
        JButton btnExport = secondaryButton("Xuất Excel");
        btnApply.setName("btnApply");
        btnRefresh.setName("btnRefresh");
        btnExport.setName("btnExport");
        bar.add(btnApply);
        bar.add(btnRefresh);
        bar.add(btnExport);

        btnApply.addActionListener(e -> reload());
        btnRefresh.addActionListener(e -> reload());
        btnExport.addActionListener(e -> exportExcel());
        cbRange.addActionListener(e -> applyPreset());

        return bar;
    }

    private void applyPreset() {
        LocalDate today = LocalDate.now();
        switch (cbRange.getSelectedIndex()) {
            case 0 -> { txtFrom.setText(today.minusDays(6).toString());  txtTo.setText(today.toString()); }
            case 1 -> { txtFrom.setText(today.minusDays(29).toString()); txtTo.setText(today.toString()); }
            case 2 -> { txtFrom.setText(today.withDayOfMonth(1).toString()); txtTo.setText(today.toString()); }
            case 3 -> {
                LocalDate first = today.minusMonths(1).withDayOfMonth(1);
                LocalDate last  = today.withDayOfMonth(1).minusDays(1);
                txtFrom.setText(first.toString()); txtTo.setText(last.toString());
            }
        }
    }

    @Override
    protected JComponent buildContent() {
        model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = TableUtils.createTable(COLS, new Object[0][]);
        // Replace model
        table.setModel(model);
        TableUtils.centerColumn(table, 1);
        TableUtils.centerColumn(table, 7);
        for (int i = 2; i <= 6; i++) TableUtils.rightColumn(table, i);

        applyPreset();

        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.add(TableUtils.scrollFor(table), BorderLayout.CENTER);

        lblSummary = new JLabel(" ");
        lblSummary.setFont(Theme.FONT_HEADER);
        lblSummary.setForeground(Theme.BRAND_DARK);
        lblSummary.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        wrap.add(lblSummary, BorderLayout.SOUTH);

        return wrap;
    }

    public void reload() {
        try {
            LocalDate from = LocalDate.parse(txtFrom.getText().trim());
            LocalDate to   = LocalDate.parse(txtTo.getText().trim());
            if (from.isAfter(to)) {
                JOptionPane.showMessageDialog(this, "Từ ngày phải <= đến ngày."); return;
            }
            List<FinanceReportDAO.RevenueRow> rows = service.revenueByDate(from, to);
            model.setRowCount(0);
            java.math.BigDecimal sumRev = java.math.BigDecimal.ZERO;
            java.math.BigDecimal sumDisc = java.math.BigDecimal.ZERO;
            java.math.BigDecimal sumNet = java.math.BigDecimal.ZERO;
            java.math.BigDecimal sumCogs = java.math.BigDecimal.ZERO;
            java.math.BigDecimal sumProfit = java.math.BigDecimal.ZERO;
            long totalInv = 0;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            for (FinanceReportDAO.RevenueRow r : rows) {
                model.addRow(new Object[]{
                        r.getDate().format(fmt),
                        r.getInvoiceCount(),
                        MoneyFormat.format(r.getRevenue()),
                        MoneyFormat.format(r.getDiscount()),
                        MoneyFormat.format(r.getNetRevenue()),
                        MoneyFormat.format(r.getCogs()),
                        MoneyFormat.format(r.getProfit()),
                        r.getProfitMargin().toPlainString() + "%"
                });
                sumRev = sumRev.add(r.getRevenue());
                sumDisc = sumDisc.add(r.getDiscount());
                sumNet = sumNet.add(r.getNetRevenue());
                sumCogs = sumCogs.add(r.getCogs());
                sumProfit = sumProfit.add(r.getProfit());
                totalInv += r.getInvoiceCount();
            }
            // Tổng cuối
            model.addRow(new Object[]{
                    "TỔNG", totalInv,
                    MoneyFormat.format(sumRev), MoneyFormat.format(sumDisc), MoneyFormat.format(sumNet),
                    MoneyFormat.format(sumCogs), MoneyFormat.format(sumProfit),
                    sumNet.signum() == 0 ? "0%" :
                            sumProfit.multiply(java.math.BigDecimal.valueOf(100))
                                    .divide(sumNet, 2, java.math.RoundingMode.HALF_UP).toPlainString() + "%"
            });
            lblSummary.setText(String.format(
                    "Tổng: %d hóa đơn  |  Doanh thu: %s  |  Lợi nhuận: %s  |  Margin TB: %s",
                    totalInv, MoneyFormat.format(sumNet), MoneyFormat.format(sumProfit),
                    sumNet.signum() == 0 ? "0%" :
                            sumProfit.multiply(java.math.BigDecimal.valueOf(100))
                                    .divide(sumNet, 2, java.math.RoundingMode.HALF_UP).toPlainString() + "%"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportExcel() {
        java.io.File out = com.utc.warehouse.util.ExcelExporter.exportTable(table, "doanh_thu");
        if (out != null) {
            JOptionPane.showMessageDialog(this, "Đã xuất: " + out.getAbsolutePath());
        }
    }

    @Override
    protected void installDefaultShortcuts() {
        com.utc.warehouse.ui.common.ShortcutManager sc = new com.utc.warehouse.ui.common.ShortcutManager(this);
        sc.bind("F5",     "refresh", this::reload, "Làm mới báo cáo");
        sc.bind("ENTER",  "apply",   this::reload, "Áp dụng bộ lọc");
        sc.bind("ctrl E", "export",  this::exportExcel, "Xuất Excel");
    }

    private JButton primaryButton(String t) {
        JButton b = new JButton(t);
        b.setBackground(Theme.BRAND); b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_HEADER);
        b.setPreferredSize(new Dimension(90, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private JButton secondaryButton(String t) {
        JButton b = new JButton(t);
        b.setBackground(Theme.ACCENT); b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_NORMAL);
        b.setPreferredSize(new Dimension(110, 32));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
