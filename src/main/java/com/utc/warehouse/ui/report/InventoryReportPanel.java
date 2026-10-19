package com.utc.warehouse.ui.report;

import com.utc.warehouse.dao.FinanceReportDAO;
import com.utc.warehouse.service.FinanceReportService;
import com.utc.warehouse.ui.common.BasePanel;
import com.utc.warehouse.ui.common.ChartHelper;
import com.utc.warehouse.ui.common.Theme;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dashboard Tổng quan: doanh thu hôm nay / tháng, lợi nhuận,
 * tồn kho, công nợ, sản phẩm cần chú ý, + 2 chart: doanh thu 7 ngày & tồn kho theo danh mục.
 */
public class InventoryReportPanel extends BasePanel {

    private final FinanceReportService service = new FinanceReportService();

    private JLabel lblTodayRevenue, lblTodayProfit;
    private JLabel lblMonthRevenue, lblMonthProfit, lblMonthInvoice;
    private JLabel lblReceivable, lblInventoryValue;
    private JLabel lblProductCount, lblLowStock;
    private JLabel lblUpdatedAt;

    // Chart containers — reload() sẽ thay nội dung của các panel này.
    private JPanel chartRevenueContainer;
    private JPanel chartStockContainer;

    public InventoryReportPanel() {
        super("Tổng quan & Dashboard");
    }

    @Override
    protected JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);
        JButton btnRefresh = primaryButton("Làm mới (F5)");
        btnRefresh.setName("btnRefresh");
        btnRefresh.addActionListener(e -> reload());
        bar.add(btnRefresh);
        return bar;
    }

    @Override
    protected JComponent buildContent() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(false);
        root.setBackground(Color.WHITE);

        // Hàng 1: Hôm nay
        JPanel row1 = new JPanel(new GridLayout(1, 2, 16, 0));
        row1.setOpaque(false);
        row1.add(buildKpiCard("💰 Doanh thu hôm nay",     "lblTodayRevenue",   Theme.BRAND));
        row1.add(buildKpiCard("📈 Lợi nhuận hôm nay",     "lblTodayProfit",   Theme.SUCCESS));
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        root.add(row1);
        root.add(Box.createVerticalStrut(12));

        // Hàng 2: Tháng này
        JPanel row2 = new JPanel(new GridLayout(1, 3, 16, 0));
        row2.setOpaque(false);
        row2.add(buildKpiCard("📅 Doanh thu tháng này",   "lblMonthRevenue",  Theme.ACCENT));
        row2.add(buildKpiCard("💎 Lợi nhuận tháng",       "lblMonthProfit",   Theme.BRAND));
        row2.add(buildKpiCard("🧾 Số hóa đơn tháng",      "lblMonthInvoice",  Theme.TEXT_MUTED));
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        root.add(row2);
        root.add(Box.createVerticalStrut(12));

        // Hàng 3: Kho & Công nợ
        JPanel row3 = new JPanel(new GridLayout(1, 2, 16, 0));
        row3.setOpaque(false);
        row3.add(buildKpiCard("📦 Giá trị tồn kho (giá vốn)", "lblInventoryValue", Theme.ACCENT));
        row3.add(buildKpiCard("💳 Công nợ phải thu",          "lblReceivable",     Theme.WARNING));
        row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        root.add(row3);
        root.add(Box.createVerticalStrut(12));

        // Hàng 4: Cảnh báo
        JPanel row4 = new JPanel(new GridLayout(1, 2, 16, 0));
        row4.setOpaque(false);
        row4.add(buildKpiCard("🏷 Tổng sản phẩm",        "lblProductCount", Theme.BRAND));
        row4.add(buildKpiCard("⚠ Cảnh báo tồn kho",     "lblLowStock",     Theme.DANGER));
        row4.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        root.add(row4);
        root.add(Box.createVerticalStrut(20));

        // ===== Charts: doanh thu 7 ngày + tồn kho theo danh mục =====
        JPanel chartRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartRow.setOpaque(false);
        chartRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        chartRevenueContainer = new JPanel(new BorderLayout());
        chartRevenueContainer.setBackground(Color.WHITE);
        chartRevenueContainer.setBorder(buildChartBorder("📊 Doanh thu 7 ngày gần nhất"));
        // Placeholder trước khi reload
        chartRevenueContainer.add(buildChartPlaceholder("Đang tải..."), BorderLayout.CENTER);
        chartRow.add(chartRevenueContainer);

        chartStockContainer = new JPanel(new BorderLayout());
        chartStockContainer.setBackground(Color.WHITE);
        chartStockContainer.setBorder(buildChartBorder("📦 Tồn kho theo danh mục"));
        chartStockContainer.add(buildChartPlaceholder("Đang tải..."), BorderLayout.CENTER);
        chartRow.add(chartStockContainer);

        root.add(chartRow);
        root.add(Box.createVerticalGlue());

        lblUpdatedAt = new JLabel(" ");
        lblUpdatedAt.setFont(Theme.FONT_SMALL);
        lblUpdatedAt.setForeground(Theme.TEXT_MUTED);
        lblUpdatedAt.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblUpdatedAt.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        root.add(lblUpdatedAt);

        return root;
    }

    private JPanel buildKpiCard(String title, String labelKey, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new SideAccentBorder(accent, 4),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(Theme.FONT_NORMAL);
        lblTitle.setForeground(Theme.TEXT_MUTED);

        JLabel lblValue = new JLabel("...");
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblValue.setForeground(accent);
        lblValue.setName(labelKey);
        switch (labelKey) {
            case "lblTodayRevenue"  -> lblTodayRevenue = lblValue;
            case "lblTodayProfit"   -> lblTodayProfit  = lblValue;
            case "lblMonthRevenue"  -> lblMonthRevenue = lblValue;
            case "lblMonthProfit"   -> lblMonthProfit  = lblValue;
            case "lblMonthInvoice"  -> lblMonthInvoice = lblValue;
            case "lblReceivable"    -> lblReceivable   = lblValue;
            case "lblInventoryValue"-> lblInventoryValue = lblValue;
            case "lblProductCount"  -> lblProductCount = lblValue;
            case "lblLowStock"      -> lblLowStock     = lblValue;
        }

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    /** Border cho chart card (đồng bộ với KPI card). */
    private javax.swing.border.Border buildChartBorder(String title) {
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_HEADER);
        titleLbl.setForeground(Theme.TEXT);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(10, 14, 8, 14));
        JPanel titleWrap = new JPanel(new BorderLayout());
        titleWrap.setBackground(Color.WHITE);
        titleWrap.add(titleLbl, BorderLayout.CENTER);
        titleWrap.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));

        // Trả về compound border đơn giản — sẽ tự vẽ + add title vào ChartHelper.wrapChartPanel
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB), 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }

    private JLabel buildChartPlaceholder(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.FONT_NORMAL);
        l.setForeground(Theme.TEXT_MUTED);
        l.setPreferredSize(new Dimension(0, 220));
        return l;
    }

    public void reload() {
        // === 1. KPI numbers (chạy nhanh, block EDT) ===
        try {
            FinanceReportDAO.DashboardSummary s = service.dashboardSummary();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            lblTodayRevenue.setText(com.utc.warehouse.util.MoneyFormat.format(s.todayRevenue) + " đ");
            lblTodayProfit.setText(com.utc.warehouse.util.MoneyFormat.format(s.todayProfit) + " đ");
            lblMonthRevenue.setText(com.utc.warehouse.util.MoneyFormat.format(s.monthRevenue) + " đ");
            lblMonthProfit.setText(com.utc.warehouse.util.MoneyFormat.format(s.monthProfit) + " đ");
            lblMonthInvoice.setText(s.monthInvoiceCount + " hóa đơn");
            lblReceivable.setText(com.utc.warehouse.util.MoneyFormat.format(s.totalReceivable) + " đ");
            lblInventoryValue.setText(com.utc.warehouse.util.MoneyFormat.format(s.inventoryValue) + " đ");
            lblProductCount.setText(s.productCount + " SP");
            lblLowStock.setText(s.lowStockCount + " SP cần nhập");
            lblUpdatedAt.setText("Cập nhật lúc: " + LocalDate.now() + " " +
                    java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        // === 2. Charts (load async để không block EDT khi data lớn) ===
        loadRevenueChartAsync();
        loadStockChartAsync();
    }

    /** Load chart doanh thu 7 ngày gần nhất, render async. */
    private void loadRevenueChartAsync() {
        setChartLoading(chartRevenueContainer, "Đang tải doanh thu...");
        SwingWorker<Map<LocalDate, BigDecimal>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<LocalDate, BigDecimal> doInBackground() {
                LocalDate to = LocalDate.now();
                LocalDate from = to.minusDays(6);
                List<FinanceReportDAO.RevenueRow> rows = service.revenueByDate(from, to);
                Map<LocalDate, BigDecimal> map = new TreeMap<>();
                for (int i = 0; i <= 6; i++) {
                    map.put(from.plusDays(i), BigDecimal.ZERO); // fill gap
                }
                for (FinanceReportDAO.RevenueRow r : rows) {
                    map.put(r.getDate(), r.getNetRevenue());
                }
                return map;
            }
            @Override
            protected void done() {
                try {
                    Map<LocalDate, BigDecimal> data = get();
                    renderRevenueChart(data);
                } catch (Exception ex) {
                    setChartError(chartRevenueContainer, "Lỗi tải chart: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void renderRevenueChart(Map<LocalDate, BigDecimal> data) {
        JPanel chartPanel = ChartHelper.lineChartTimeSeries("Doanh thu 7 ngày gần nhất", data);
        replaceChart(chartRevenueContainer, chartPanel, "📊 Doanh thu 7 ngày gần nhất");
    }

    /** Load pie chart tồn kho theo danh mục, render async. */
    private void loadStockChartAsync() {
        setChartLoading(chartStockContainer, "Đang tải tồn kho...");
        SwingWorker<Map<String, Number>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Number> doInBackground() {
                List<FinanceReportDAO.CategoryStockRow> rows = service.inventoryByCategory();
                Map<String, Number> map = new LinkedHashMap<>();
                for (FinanceReportDAO.CategoryStockRow r : rows) {
                    map.put(r.getCategoryName(), r.getQuantity());
                }
                return map;
            }
            @Override
            protected void done() {
                try {
                    Map<String, Number> data = get();
                    if (data.isEmpty()) {
                        setChartEmpty(chartStockContainer);
                        return;
                    }
                    renderStockChart(data);
                } catch (Exception ex) {
                    setChartError(chartStockContainer, "Lỗi tải chart: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void renderStockChart(Map<String, Number> data) {
        JPanel chartPanel = ChartHelper.pieChart("Tồn kho theo danh mục", data);
        replaceChart(chartStockContainer, chartPanel, "📦 Tồn kho theo danh mục");
    }

    /* ============== chart container helpers ============== */

    private void setChartLoading(JPanel container, String msg) {
        replaceChart(container, buildChartPlaceholder(msg), titleOf(container));
    }

    private void setChartError(JPanel container, String msg) {
        JLabel l = new JLabel(msg, SwingConstants.CENTER);
        l.setForeground(Theme.DANGER);
        l.setFont(Theme.FONT_NORMAL);
        replaceChart(container, l, titleOf(container));
    }

    private void setChartEmpty(JPanel container) {
        replaceChart(container, buildChartPlaceholder("Chưa có dữ liệu tồn kho"), titleOf(container));
    }

    /** Thay chart bên trong container (giữ nguyên border + title). */
    private void replaceChart(JPanel container, Component newContent, String title) {
        container.removeAll();
        container.setLayout(new BorderLayout());
        container.add(buildTitleBar(title), BorderLayout.NORTH);
        container.add(newContent, BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
    }

    private JComponent buildTitleBar(String title) {
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_HEADER);
        titleLbl.setForeground(Theme.TEXT);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(10, 14, 8, 14));
        JPanel titleWrap = new JPanel(new BorderLayout());
        titleWrap.setBackground(Color.WHITE);
        titleWrap.add(titleLbl, BorderLayout.CENTER);
        titleWrap.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        return titleWrap;
    }

    /** Lấy title hiện tại của container (best-effort, dùng cho replace). */
    private String titleOf(JPanel container) {
        // Container chỉ dùng làm key; title thật nằm ở buildContent.
        if (container == chartRevenueContainer) return "📊 Doanh thu 7 ngày gần nhất";
        if (container == chartStockContainer)   return "📦 Tồn kho theo danh mục";
        return "";
    }

    private static class SideAccentBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        SideAccentBorder(Color color, int thickness) { this.color = color; this.thickness = thickness; }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillRoundRect(x, y, thickness, height - 1, 4, 4);
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) {
            return new Insets(10, 18, 10, 14);
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
