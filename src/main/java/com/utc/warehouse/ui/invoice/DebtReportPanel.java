package com.utc.warehouse.ui.invoice;

import com.utc.warehouse.dao.FinanceReportDAO;
import com.utc.warehouse.service.FinanceReportService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.TableUtils;
import com.utc.warehouse.ui.common.Theme;
import com.utc.warehouse.util.MoneyFormat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Báo cáo công nợ khách hàng: ai đang nợ bao nhiêu, theo hóa đơn nào.
 */
public class DebtReportPanel extends BasePanel {

    private final FinanceReportService service = new FinanceReportService();
    private JTable table;
    private DefaultTableModel model;
    private final Object[] COLS = {"ID", "Khách hàng", "Số HĐ nợ", "Tổng HĐ", "Đã thu", "Còn nợ"};
    private JLabel lblTotal;

    public DebtReportPanel() { super("Công nợ khách hàng"); }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        JButton btnRefresh = primaryButton("Làm mới");
        btnRefresh.setName("btnRefresh");
        btnRefresh.addActionListener(e -> reload());
        bar.add(btnRefresh);
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
        TableUtils.centerColumn(table, 2);
        for (int i = 3; i <= 5; i++) TableUtils.rightColumn(table, i);
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.add(TableUtils.scrollFor(table), BorderLayout.CENTER);
        lblTotal = new JLabel(" ");
        lblTotal.setFont(Theme.FONT_HEADER);
        lblTotal.setForeground(Theme.DANGER);
        lblTotal.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        wrap.add(lblTotal, BorderLayout.SOUTH);
        return wrap;
    }

    public void reload() {
        try {
            List<FinanceReportDAO.CustomerDebtRow> rows = service.customerDebts();
            model.setRowCount(0);
            BigDecimal totalDebt = BigDecimal.ZERO;
            for (FinanceReportDAO.CustomerDebtRow r : rows) {
                model.addRow(new Object[]{
                        r.getCustomerId() == null ? "-" : r.getCustomerId(),
                        r.getCustomerName() == null ? "(Khách lẻ)" : r.getCustomerName(),
                        r.getInvoiceCount(),
                        MoneyFormat.format(r.getTotal()),
                        MoneyFormat.format(r.getPaid()),
                        MoneyFormat.format(r.getDebt())
                });
                totalDebt = totalDebt.add(r.getDebt());
            }
            model.addRow(new Object[]{
                    "", "TỔNG CỘNG", "", "", "", MoneyFormat.format(totalDebt)
            });
            lblTotal.setText("Tổng công nợ: " + MoneyFormat.format(totalDebt) + "  |  " + rows.size() + " khách hàng");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton primaryButton(String t) {
        JButton b = new JButton(t);
        b.setBackground(Theme.BRAND); b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setFont(Theme.FONT_HEADER);
        b.setPreferredSize(new Dimension(120, 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
