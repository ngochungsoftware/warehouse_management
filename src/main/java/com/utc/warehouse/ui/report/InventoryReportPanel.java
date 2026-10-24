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
 * tồn kho, công nợ, sản phẩm cần chú ý + 2 chart:
 * doanh thu 7 ngày gần nhất & tồn kho theo danh mục.
 */
public class InventoryReportPanel extends BasePanel {

    private final FinanceReportService service = new FinanceReportService();

    private JLabel lblTodayRevenue, lblTodayProfit;
    private JLabel lblMonthRevenue, lblMonthProfit, lblMonthInvoice;
    private JLabel lblReceivable, lblInventoryValue;
    private JLabel lblProductCount, lblLowStock;
    private JLabel lblUpdatedAt;

    /** Chart containers — reload() sẽ thay nội dung của các panel này. */
    private JPanel chartRevenueContainer;
    private JPanel chartStockContainer;

    public InventoryReportPanel() {
        super("Tổng quan & Dashboard");
        // Nội dung cao cố định (4 hàng KPI + 2 chart) nên khi thu nhỏ cửa sổ
        // phần dưới bị cắt mất. Bật cuộn chuột cho riêng màn này.
        scrollContent = true;
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

        // Hàng 1: Hôm nay / Tháng này
        JPanel row1 = new JPanel(new GridLayout(1, 2, 16, 0));
        row1.setOpaque(false);
        row1.add(buildKpiCard("💰 Doanh thu hôm nay",     "lblTodayRevenue",   Theme.BRAND));
        row1.add(buildKpiCard("📈 Lợi nhuận hôm nay",     "lblTodayProfit",   Theme.SUCCESS));
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        root.add(row1);
        root.add(Box.createVerticalStrut(12));

        JPanel row2 = new JPanel(new GridLayout(1, 3, 16, 0));
        row2.setOpaque(false);
        row2.add(buildKpiCard("📅 Doanh thu tháng này",   "lblMonthRevenue",  Theme.ACCENT));
        row2.add(buildKpiCard("💎 Lợi nhuận tháng",       "lblMonthProfit",   Theme.BRAND));
        row2.add(buildKpiCard("🧾 Số hóa đơn tháng",      "lblMonthInvoice",  Theme.TEXT_MUTED));
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        root.add(row2);
        root.add(Box.createVerticalStrut(12));

        JPanel row3 = new JPanel(new GridLayout(1, 2, 16, 0));
        row3.setOpaque(false);
        row3.add(buildKpiCard("📦 Giá trị tồn kho (giá vốn)", "lblInventoryValue", Theme.ACCENT));
        row3.add(buildKpiCard("💳 Công nợ phải thu",          "lblReceivable",     Theme.WARNING));
        row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        root.add(row3);
        root.add(Box.createVerticalStrut(12));

        JPanel row4 = new JPanel(new GridLayout(1, 2, 16, 0));
        row4.setOpaque(false);
        row4.add(buildKpiCard("🏷 Tổng sản phẩm",        "lblProductCount", Theme.BRAND));
        row4.add(buildKpiCard("⚠ Cảnh báo tồn kho",     "lblLowStock",     Theme.DANGER));
        row4.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        root.add(row4);
        root.add(Box.createVerticalStrut(16));

        // ===== Charts: doanh thu 7 ngày + tồn kho theo danh mục =====
        JPanel chartRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartRow.setOpaque(false);
        chartRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        chartRow.setMinimumSize(new Dimension(0, 320));

        chartRevenueContainer = new JPanel(new BorderLayout());
        chartRevenueContainer.setBackground(Color.WHITE);
        chartRevenueContainer.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
        chartRevenueContainer.add(buildChartPlaceholder("Đang tải..."), BorderLayout.CENTER);
        chartRow.add(chartRevenueContainer);

        chartStockContainer = new JPanel(new BorderLayout());
        chartStockContainer.setBackground(Color.WHITE);
        chartStockContainer.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1, true));
        chartStockContainer.add(buildChartPlaceholder("Đang tải..."), BorderLayout.CENTER);
        chartRow.add(chartStockContainer);

        root.add(chartRow);
        // KHÔNG dùng Box.createVerticalGlue() ở đây: glue co giãn lấp hết
        // khoảng trống nội dung → nội dung luôn vừa khung → thanh cuộn
        // (scrollContent của BasePanel) không bao giờ xuất hiện, và chart
        // cuối cùng bị cắt khi thu nhỏ cửa sổ.
        root.add(Box.createVerticalStrut(16));

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

    /* ================== Charts ================== */

    /** Placeholder hiển thị trong lúc chờ dữ liệu chart về. */
    private JLabel buildChartPlaceholder(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(Theme.FONT_NORMAL);
        l.setForeground(Theme.TEXT_MUTED);
        l.setPreferredSize(new Dimension(0, 220));
        return l;
    }

    /**
     * Load chart doanh thu 7 ngày gần nhất.
     *
     * <p>Chạy bằng SwingWorker để không block EDT: query + vẽ chart là việc nặng,
     * làm trực tiếp trên EDT sẽ làm cả app đứng hình.
     */
    private void loadRevenueChartAsync() {
        if (chartRevenueContainer == null) return;
        replaceChart(chartRevenueContainer, buildChartPlaceholder("Đang tải doanh thu..."),
                "📊 Doanh thu 7 ngày gần nhất");

        new SwingWorker<Map<LocalDate, BigDecimal>, Void>() {
            @Override
            protected Map<LocalDate, BigDecimal> doInBackground() {
                LocalDate to = LocalDate.now();
                LocalDate from = to.minusDays(6);
                List<FinanceReportDAO.RevenueRow> rows = service.revenueByDate(from, to);

                Map<LocalDate, BigDecimal> map = new TreeMap<>();
                // Fill sẵn 7 ngày (kể cả ngày không có doanh thu) để line chart
                // liền mạch, không bị "nhảy" qua các ngày trống.
                for (int i = 0; i <= 6; i++) map.put(from.plusDays(i), BigDecimal.ZERO);
                for (FinanceReportDAO.RevenueRow r : rows) {
                    map.put(r.getDate(), r.getNetRevenue());
                }
                return map;
            }

            @Override
            protected void done() {
                try {
                    Map<LocalDate, BigDecimal> data = get();
                    JPanel chart = ChartHelper.lineChartTimeSeries("Doanh thu 7 ngày gần nhất", data);
                    replaceChart(chartRevenueContainer, chart, "📊 Doanh thu 7 ngày gần nhất");
                } catch (Exception ex) {
                    replaceChart(chartRevenueContainer,
                            buildChartError("Lỗi tải chart: " + ex.getMessage()),
                            "📊 Doanh thu 7 ngày gần nhất");
                }
            }
        }.execute();
    }

    /** Load pie chart tồn kho theo danh mục. */
    private void loadStockChartAsync() {
        if (chartStockContainer == null) return;
        replaceChart(chartStockContainer, buildChartPlaceholder("Đang tải tồn kho..."),
                "📦 Tồn kho theo danh mục");

        new SwingWorker<Map<String, Number>, Void>() {
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
                        replaceChart(chartStockContainer,
                                buildChartPlaceholder("Chưa có dữ liệu tồn kho"),
                                "📦 Tồn kho theo danh mục");
                        return;
                    }
                    JPanel chart = ChartHelper.pieChart("Tồn kho theo danh mục", data);
                    replaceChart(chartStockContainer, chart, "📦 Tồn kho theo danh mục");
                } catch (Exception ex) {
                    replaceChart(chartStockContainer,
                            buildChartError("Lỗi tải chart: " + ex.getMessage()),
                            "📦 Tồn kho theo danh mục");
                }
            }
        }.execute();
    }

    private JLabel buildChartError(String msg) {
        JLabel l = new JLabel(msg, SwingConstants.CENTER);
        l.setFont(Theme.FONT_NORMAL);
        l.setForeground(Theme.DANGER);
        l.setPreferredSize(new Dimension(0, 220));
        return l;
    }

    /** Thay nội dung chart bên trong container, giữ nguyên title bar. */
    private void replaceChart(JPanel container, Component newContent, String title) {
        container.removeAll();
        container.setLayout(new BorderLayout());
        container.add(buildChartTitleBar(title), BorderLayout.NORTH);
        container.add(newContent, BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
    }

    private JComponent buildChartTitleBar(String title) {
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

    public void reload() {
        try {
            FinanceReportDAO.DashboardSummary s = service.dashboardSummary();
            lblTodayRevenue.setText(com.utc.warehouse.util.MoneyFormat.format(s.todayRevenue));
            lblTodayProfit.setText(com.utc.warehouse.util.MoneyFormat.format(s.todayProfit));
            lblMonthRevenue.setText(com.utc.warehouse.util.MoneyFormat.format(s.monthRevenue));
            lblMonthProfit.setText(com.utc.warehouse.util.MoneyFormat.format(s.monthProfit));
            lblMonthInvoice.setText(s.monthInvoiceCount + " hóa đơn");
            lblReceivable.setText(com.utc.warehouse.util.MoneyFormat.format(s.totalReceivable));
            lblInventoryValue.setText(com.utc.warehouse.util.MoneyFormat.format(s.inventoryValue));
            lblProductCount.setText(s.productCount + " SP");
            lblLowStock.setText(s.lowStockCount + " SP cần nhập");
            lblUpdatedAt.setText("Cập nhật lúc: " + LocalDate.now() + " " +
                    java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        // Charts load SAU KPI và chạy async: query + vẽ chart nặng, không
        // được block EDT (nếu không cả app đứng hình lúc mở trang).
        loadRevenueChartAsync();
        loadStockChartAsync();
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
