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
import java.util.List;

/**
 * Top sản phẩm bán chạy theo số lượng / doanh thu / lợi nhuận trong khoảng thời gian.
 */
public class TopProductsPanel extends BasePanel {

    private final FinanceReportService service = new FinanceReportService();
    private JTable table;
    private DefaultTableModel model;
    private final Object[] COLS = {"#", "SKU", "Tên sản phẩm", "SL bán", "Doanh thu", "Lợi nhuận"};
    private JTextField txtFrom, txtTo;
    private JSpinner spLimit;

    public TopProductsPanel() { super("Top sản phẩm bán chạy"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        LocalDate today = LocalDate.now();
        bar.add(new JLabel("Từ:"));
        txtFrom = new JTextField(today.minusDays(29).toString(), 10);
        bar.add(txtFrom);
        bar.add(new JLabel("Đến:"));
        txtTo = new JTextField(today.toString(), 10);
        bar.add(txtTo);
        bar.add(new JLabel("Top:"));
        spLimit = new JSpinner(new SpinnerNumberModel(20, 5, 200, 5));
        spLimit.setPreferredSize(new Dimension(70, 28));
        bar.add(spLimit);
        JButton btnApply = primaryButton("Xem");
        btnApply.setName("btnApply");
        btnApply.addActionListener(e -> reload());
        bar.add(btnApply);
        return bar;
    }

    @Override
    protected JComponent buildContent() {
        model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = TableUtils.createTable(COLS, new Object[0][]);
        table.setModel(model);
        TableUtils.centerColumn(table, 0);
        TableUtils.centerColumn(table, 3);
        for (int i = 4; i <= 5; i++) TableUtils.rightColumn(table, i);
        return TableUtils.scrollFor(table);
    }

    public void reload() {
        try {
            LocalDate from = LocalDate.parse(txtFrom.getText().trim());
            LocalDate to   = LocalDate.parse(txtTo.getText().trim());
            int limit = (Integer) spLimit.getValue();
            List<FinanceReportDAO.TopProductRow> rows = service.topProducts(from, to, limit);
            model.setRowCount(0);
            int idx = 1;
            for (FinanceReportDAO.TopProductRow r : rows) {
                model.addRow(new Object[]{
                        idx++, r.getSku(), r.getName(),
                        r.getQuantitySold(),
                        MoneyFormat.format(r.getRevenue()),
                        MoneyFormat.format(r.getProfit())
                });
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
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
}
