package com.utc.warehouse.ui.common;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Day;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Helper vẽ chart cho Dashboard / Báo cáo.
 * Trả về {@link ChartPanel} (đã được style hợp với FlatLaf) — chỉ cần add vào JPanel.
 *
 * <p>Phụ thuộc: JFreeChart 1.5.x (đã thêm vào pom.xml / lib/).
 */
public class ChartHelper {

    /** Style tổng: dùng màu brand + font đồng bộ với Theme của app. */
    private static final java.awt.Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 14);
    private static final java.awt.Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Color BRAND       = new Color(0x3B82F6);
    private static final Color SUCCESS     = new Color(0x10B981);
    private static final Color WARNING     = new Color(0xF59E0B);
    private static final Color DANGER      = new Color(0xEF4444);
    private static final Color TEXT        = new Color(0x1F2937);
    private static final Color GRID        = new Color(0xE5E7EB);

    /* ================== LINE CHART (doanh thu 30 ngày) ================== */

    /**
     * Line chart thời gian, trục X là ngày, Y là giá trị tiền.
     *
     * @param title  Tiêu đề chart
     * @param data   Map ngày (LocalDate) -> giá trị
     */
    public static JPanel lineChartTimeSeries(String title, Map<java.time.LocalDate, BigDecimal> data) {
        TimeSeries series = new TimeSeries(title);
        // Sắp xếp theo ngày tăng dần
        TreeMap<java.time.LocalDate, BigDecimal> sorted = new TreeMap<>(data);
        for (Map.Entry<java.time.LocalDate, BigDecimal> e : sorted.entrySet()) {
            series.addOrUpdate(new Day(java.sql.Date.valueOf(e.getKey())),
                    e.getValue() == null ? 0.0 : e.getValue().doubleValue());
        }
        TimeSeriesCollection ds = new TimeSeriesCollection(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title, "Ngày", "Giá trị (đ)", ds, false, true, false);

        styleChart(chart);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(GRID);
        plot.setRangeGridlinePaint(GRID);

        // Format trục Y hiển thị "1.000.000" thay vì "1.0E7"
        if (plot.getRangeAxis() instanceof NumberAxis yAxis) {
            yAxis.setNumberFormatOverride(new DecimalFormat("#,##0"));
        }

        // Màu đường
        if (plot.getRenderer() instanceof org.jfree.chart.renderer.xy.XYLineAndShapeRenderer r) {
            r.setSeriesPaint(0, BRAND);
            r.setSeriesStroke(0, new BasicStroke(2.2f));
            r.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3, -3, 6, 6));
        }

        return wrapChartPanel(chart);
    }

    /* ================== BAR CHART (top SP bán chạy / theo tháng) ================== */

    /**
     * Bar chart đơn giản, mỗi category là 1 cột.
     *
     * @param title  Tiêu đề
     * @param data   Map (label -> giá trị)
     */
    public static JPanel barChart(String title, Map<String, Number> data) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (Map.Entry<String, Number> e : data.entrySet()) {
            ds.addValue(e.getValue(), title, e.getKey());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                title, "", "Giá trị", ds, PlotOrientation.VERTICAL,
                false, true, false);
        styleChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(GRID);
        plot.setDomainGridlinePaint(GRID);

        CategoryAxis x = plot.getDomainAxis();
        x.setLabelFont(FONT_LABEL);
        x.setTickLabelFont(FONT_LABEL);
        if (plot.getRangeAxis() instanceof NumberAxis y) {
            y.setLabelFont(FONT_LABEL);
            y.setTickLabelFont(FONT_LABEL);
            y.setNumberFormatOverride(new DecimalFormat("#,##0"));
        }

        if (plot.getRenderer() instanceof BarRenderer r) {
            r.setSeriesPaint(0, BRAND);
            r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
            r.setItemMargin(0.05);
        }

        return wrapChartPanel(chart);
    }

    /**
     * Bar chart GROUPED (vd: doanh thu vs lợi nhuận theo tháng).
     *
     * @param title    Tiêu đề
     * @param series   Map (seriesName -> Map(categoryName -> value))
     */
    public static JPanel barChartGrouped(String title, Map<String, Map<String, Number>> series) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        java.util.Set<String> categories = new java.util.LinkedHashSet<>();
        for (Map<String, Number> cat : series.values()) categories.addAll(cat.keySet());
        for (Map.Entry<String, Map<String, Number>> s : series.entrySet()) {
            for (String cat : categories) {
                Number v = s.getValue().get(cat);
                ds.addValue(v == null ? 0 : v, s.getKey(), cat);
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                title, "Tháng", "Giá trị (đ)", ds, PlotOrientation.VERTICAL,
                true, true, false);
        styleChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(GRID);

        if (plot.getRangeAxis() instanceof NumberAxis y) {
            y.setNumberFormatOverride(new DecimalFormat("#,##0"));
            y.setLabelFont(FONT_LABEL);
            y.setTickLabelFont(FONT_LABEL);
        }
        plot.getDomainAxis().setLabelFont(FONT_LABEL);
        plot.getDomainAxis().setTickLabelFont(FONT_LABEL);

        if (plot.getRenderer() instanceof BarRenderer r) {
            r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
            r.setItemMargin(0.05);
            Color[] palette = {BRAND, SUCCESS, WARNING, DANGER, new Color(0x8B5CF6)};
            for (int i = 0; i < palette.length; i++) r.setSeriesPaint(i, palette[i]);
        }

        return wrapChartPanel(chart);
    }

    /* ================== PIE CHART (tỉ lệ tồn kho theo danh mục) ================== */

    /** Pie chart — Map label -> giá trị. */
    public static JPanel pieChart(String title, Map<String, Number> data) {
        DefaultPieDataset ds = new DefaultPieDataset();
        for (Map.Entry<String, Number> e : data.entrySet()) {
            ds.setValue(e.getKey(), e.getValue() == null ? 0.0 : e.getValue().doubleValue());
        }

        JFreeChart chart = ChartFactory.createPieChart(title, ds, true, true, false);
        styleChart(chart);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setLabelFont(FONT_LABEL);
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                "{0}: {1} ({2})", new DecimalFormat("#,##0"), new DecimalFormat("0.0%")));
        Color[] palette = {BRAND, SUCCESS, WARNING, DANGER, new Color(0x8B5CF6), new Color(0x06B6D4)};
        int i = 0;
        for (Object key : ds.getKeys()) {
            plot.setSectionPaint((Comparable<?>) key, palette[i % palette.length]);
            i++;
        }

        return wrapChartPanel(chart);
    }

    /* ================== STACKED BAR (nhập vs xuất theo tháng) ================== */

    public static JPanel stackedBarChart(String title, Map<String, Map<String, Number>> series) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        java.util.Set<String> categories = new java.util.LinkedHashSet<>();
        for (Map<String, Number> cat : series.values()) categories.addAll(cat.keySet());
        for (Map.Entry<String, Map<String, Number>> s : series.entrySet()) {
            for (String cat : categories) {
                Number v = s.getValue().get(cat);
                ds.addValue(v == null ? 0 : v, s.getKey(), cat);
            }
        }

        JFreeChart chart = ChartFactory.createStackedBarChart(
                title, "", "", ds, PlotOrientation.VERTICAL,
                true, true, false);
        styleChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(GRID);

        if (plot.getRangeAxis() instanceof NumberAxis y) {
            y.setNumberFormatOverride(new DecimalFormat("#,##0"));
        }

        if (plot.getRenderer() instanceof BarRenderer r) {
            r.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
            Color[] palette = {SUCCESS, BRAND};
            for (int i = 0; i < palette.length; i++) r.setSeriesPaint(i, palette[i]);
        }

        return wrapChartPanel(chart);
    }

    /* ================== Helpers ================== */

    private static JPanel wrapChartPanel(JFreeChart chart) {
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(400, 260));
        cp.setMouseWheelEnabled(true);
        cp.setOpaque(false);
        // Border nhẹ đồng bộ với KPI card
        cp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB), 1, true),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        return cp;
    }

    /** Style chung cho mọi chart: font + nền. */
    private static void styleChart(JFreeChart chart) {
        chart.getTitle().setFont(FONT_TITLE);
        chart.getTitle().setPaint(TEXT);
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(FONT_LABEL);
        }
    }
}
