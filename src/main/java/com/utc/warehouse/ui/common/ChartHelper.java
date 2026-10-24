package com.utc.warehouse.ui.common;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Helper vẽ chart cho Dashboard / Báo cáo.
 * Trả về {@link JPanel} chứa {@link ChartPanel} (đã style hợp với Theme của app)
 * — chỉ cần add vào JPanel là hiện.
 *
 * <p>Phụ thuộc: JFreeChart 1.5.x (khai báo trong pom.xml, jar có sẵn ở lib/).
 */
public class ChartHelper {

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 11);

    /** Bảng màu chung cho các series (mỗi chart xoay vòng). */
    private static final Color[] PALETTE = {
            Theme.BRAND,
            Theme.ACCENT,
            Theme.SUCCESS,
            Theme.WARNING,
            Theme.DANGER,
            new Color(0x8B5CF6),
            new Color(0x06B6D4)
    };

    /* ================== LINE CHART (doanh thu theo ngày) ================== */

    /**
     * Line chart thời gian: trục X là ngày, Y là giá trị tiền.
     *
     * @param title Tiêu đề chart
     * @param data  Map ngày -> giá trị (null = không có dữ liệu)
     */
    public static JPanel lineChartTimeSeries(String title, Map<LocalDate, BigDecimal> data) {
        TimeSeries series = new TimeSeries(title);
        TreeMap<LocalDate, BigDecimal> sorted = new TreeMap<>(data);
        for (Map.Entry<LocalDate, BigDecimal> e : sorted.entrySet()) {
            series.addOrUpdate(new Day(java.sql.Date.valueOf(e.getKey())),
                    e.getValue() == null ? 0.0 : e.getValue().doubleValue());
        }
        TimeSeriesCollection ds = new TimeSeriesCollection(series);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title, "Ngày", "Giá trị (đ)", ds, false, true, false);
        styleChart(chart);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Theme.BORDER);
        plot.setRangeGridlinePaint(Theme.BORDER);

        // Format trục Y hiển thị "1.000.000" thay vì "1.0E7"
        if (plot.getRangeAxis() instanceof NumberAxis yAxis) {
            yAxis.setNumberFormatOverride(new DecimalFormat("#,##0"));
            yAxis.setLabelFont(FONT_LABEL);
            yAxis.setTickLabelFont(FONT_LABEL);
        }
        if (plot.getDomainAxis() != null) {
            plot.getDomainAxis().setLabelFont(FONT_LABEL);
            plot.getDomainAxis().setTickLabelFont(FONT_LABEL);
        }

        if (plot.getRenderer() instanceof XYLineAndShapeRenderer r) {
            r.setSeriesPaint(0, Theme.BRAND);
            r.setSeriesStroke(0, new BasicStroke(2.2f));
            r.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));
        }

        return wrapChartPanel(chart);
    }

    /* ================== BAR CHART ================== */

    /**
     * Bar chart đơn giản: mỗi category là 1 cột.
     *
     * @param data Map (label -> giá trị)
     */
    public static JPanel barChart(String title, Map<String, Number> data) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (Map.Entry<String, Number> e : data.entrySet()) {
            ds.addValue(e.getValue() == null ? 0 : e.getValue(), title, e.getKey());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                title, "", "Giá trị (đ)", ds, PlotOrientation.VERTICAL, false, true, false);
        styleChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Theme.BORDER);
        plot.setDomainGridlinePaint(Theme.BORDER);

        CategoryAxis x = plot.getDomainAxis();
        x.setLabelFont(FONT_LABEL);
        x.setTickLabelFont(FONT_LABEL);
        if (plot.getRangeAxis() instanceof NumberAxis y) {
            y.setLabelFont(FONT_LABEL);
            y.setTickLabelFont(FONT_LABEL);
            y.setNumberFormatOverride(new DecimalFormat("#,##0"));
        }

        if (plot.getRenderer() instanceof BarRenderer r) {
            r.setSeriesPaint(0, Theme.BRAND);
            r.setBarPainter(new StandardBarPainter());
            r.setItemMargin(0.05);
        }

        return wrapChartPanel(chart);
    }

    /**
     * Bar chart GROUPED (vd: doanh thu vs lợi nhuận theo tháng).
     *
     * @param series Map (seriesName -> Map(categoryName -> value))
     */
    public static JPanel barChartGrouped(String title, Map<String, Map<String, Number>> series) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        Set<String> categories = new LinkedHashSet<>();
        for (Map<String, Number> cat : series.values()) categories.addAll(cat.keySet());
        for (Map.Entry<String, Map<String, Number>> s : series.entrySet()) {
            for (String cat : categories) {
                Number v = s.getValue().get(cat);
                ds.addValue(v == null ? 0 : v, s.getKey(), cat);
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                title, "Tháng", "Giá trị (đ)", ds, PlotOrientation.VERTICAL, true, true, false);
        styleChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Theme.BORDER);

        if (plot.getRangeAxis() instanceof NumberAxis y) {
            y.setNumberFormatOverride(new DecimalFormat("#,##0"));
            y.setLabelFont(FONT_LABEL);
            y.setTickLabelFont(FONT_LABEL);
        }
        plot.getDomainAxis().setLabelFont(FONT_LABEL);
        plot.getDomainAxis().setTickLabelFont(FONT_LABEL);

        if (plot.getRenderer() instanceof BarRenderer r) {
            r.setBarPainter(new StandardBarPainter());
            r.setItemMargin(0.05);
            for (int i = 0; i < PALETTE.length; i++) r.setSeriesPaint(i, PALETTE[i]);
        }

        return wrapChartPanel(chart);
    }

    /* ================== PIE CHART ================== */

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

        int i = 0;
        for (Object key : ds.getKeys()) {
            plot.setSectionPaint((Comparable<?>) key, PALETTE[i % PALETTE.length]);
            i++;
        }

        return wrapChartPanel(chart);
    }

    /* ================== STACKED BAR ================== */

    /** Stacked bar chart — Map (seriesName -> Map(categoryName -> value)). */
    public static JPanel stackedBarChart(String title, Map<String, Map<String, Number>> series) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        Set<String> categories = new LinkedHashSet<>();
        for (Map<String, Number> cat : series.values()) categories.addAll(cat.keySet());
        for (Map.Entry<String, Map<String, Number>> s : series.entrySet()) {
            for (String cat : categories) {
                Number v = s.getValue().get(cat);
                ds.addValue(v == null ? 0 : v, s.getKey(), cat);
            }
        }

        JFreeChart chart = ChartFactory.createStackedBarChart(
                title, "", "", ds, PlotOrientation.VERTICAL, true, true, false);
        styleChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Theme.BORDER);

        if (plot.getRangeAxis() instanceof NumberAxis y) {
            y.setNumberFormatOverride(new DecimalFormat("#,##0"));
        }

        if (plot.getRenderer() instanceof BarRenderer r) {
            r.setBarPainter(new StandardBarPainter());
            for (int i = 0; i < PALETTE.length; i++) r.setSeriesPaint(i, PALETTE[i]);
        }

        return wrapChartPanel(chart);
    }

    /* ================== Helpers ================== */

    private static JPanel wrapChartPanel(JFreeChart chart) {
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(400, 260));
        cp.setMouseWheelEnabled(true);
        cp.setOpaque(false);
        cp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        return cp;
    }

    /** Style chung cho mọi chart: font + nền. */
    private static void styleChart(JFreeChart chart) {
        chart.getTitle().setFont(FONT_TITLE);
        chart.getTitle().setPaint(Theme.TEXT);
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(FONT_LABEL);
        }
    }
}