package com.utc.warehouse;

import com.utc.warehouse.ui.common.ChartHelper;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * VerifyTest30 — Kiểm chứng các loại chart JFreeChart vẽ ra được (không cần DB).
 *
 * <p>Render từng loại chart ra PNG để xác nhận:
 *   1. Không ném exception khi vẽ
 *   2. Chart có kích thước hợp lệ (không rỗng / 0 pixel)
 *   3. Màu bám theo Theme của app
 *
 * Dữ liệu là số giả nên KHÔNG đụng tới database.
 */
public class VerifyTest30 {

    private static int pass = 0, fail = 0;
    private static final String OUT_DIR = "build/charts/";

    public static void main(String[] args) throws Exception {
        new File(OUT_DIR).mkdirs();
        System.out.println("=== KIỂM CHỨNG CHART (JFreeChart) ===\n");

        // ---------- 1. Line chart doanh thu 7 ngày ----------
        LocalDate from = LocalDate.now().minusDays(6);
        Map<LocalDate, BigDecimal> timeData = new TreeMap<>();
        for (int i = 0; i <= 6; i++) {
            timeData.put(from.plusDays(i), BigDecimal.valueOf(1_000_000L * (i + 1)));
        }
        check("Line chart (doanh thu 7 ngày)",
                ChartHelper.lineChartTimeSeries("Doanh thu 7 ngày gần nhất", timeData),
                "01_line_revenue.png");

        // ---------- 2. Pie chart tồn kho theo danh mục ----------
        Map<String, Number> pie = new LinkedHashMap<>();
        pie.put("Đồ gia dụng", 120);
        pie.put("Thực phẩm", 300);
        pie.put("Nguyên liệu", 75);
        pie.put("Văn phòng", 40);
        check("Pie chart (tồn kho theo danh mục)",
                ChartHelper.pieChart("Tồn kho theo danh mục", pie),
                "02_pie_inventory.png");

        // ---------- 3. Bar chart ----------
        Map<String, Number> bar = new LinkedHashMap<>();
        bar.put("SP A", 50);
        bar.put("SP B", 130);
        bar.put("SP C", 90);
        check("Bar chart (top sản phẩm)",
                ChartHelper.barChart("Top sản phẩm", bar),
                "03_bar_top_products.png");

        // ---------- 4. Grouped bar chart ----------
        Map<String, Map<String, Number>> grouped = new LinkedHashMap<>();
        Map<String, Number> rev = new LinkedHashMap<>();
        rev.put("T1", 100.0); rev.put("T2", 120.0); rev.put("T3", 90.0);
        Map<String, Number> pro = new LinkedHashMap<>();
        pro.put("T1", 30.0); pro.put("T2", 45.0); pro.put("T3", 25.0);
        grouped.put("Doanh thu", rev);
        grouped.put("Lợi nhuận", pro);
        check("Grouped bar chart (doanh thu vs lợi nhuận)",
                ChartHelper.barChartGrouped("Doanh thu vs Lợi nhuận", grouped),
                "04_grouped_revenue_profit.png");

        // ---------- 5. Stacked bar chart ----------
        Map<String, Map<String, Number>> stacked = new LinkedHashMap<>();
        Map<String, Number> imp = new LinkedHashMap<>();
        imp.put("T1", 10.0); imp.put("T2", 14.0); imp.put("T3", 8.0);
        Map<String, Number> exp = new LinkedHashMap<>();
        exp.put("T1", 6.0); exp.put("T2", 9.0); exp.put("T3", 11.0);
        stacked.put("Nhập", imp);
        stacked.put("Xuất", exp);
        check("Stacked bar chart (nhập vs xuất)",
                ChartHelper.stackedBarChart("Nhập vs Xuất", stacked),
                "05_stacked_import_export.png");

        // ---------- 6. Edge case: dữ liệu rỗng / bằng 0 ----------
        System.out.println("\n--- Edge cases ---");
        try {
            ChartHelper.pieChart("Rỗng", new LinkedHashMap<>());
            checkTrue("Pie chart với dữ liệu rỗng không ném lỗi", true);
        } catch (Exception e) {
            checkTrue("Pie chart với dữ liệu rỗng không ném lỗi", false);
        }
        try {
            Map<String, Number> zeros = new LinkedHashMap<>();
            zeros.put("A", 0.0);
            ChartHelper.barChart("Toàn 0", zeros);
            checkTrue("Bar chart toàn giá trị 0 không ném lỗi", true);
        } catch (Exception e) {
            checkTrue("Bar chart toàn giá trị 0 không ném lỗi", false);
        }
        try {
            Map<LocalDate, BigDecimal> nulls = new TreeMap<>();
            nulls.put(LocalDate.now(), null);
            ChartHelper.lineChartTimeSeries("Null value", nulls);
            checkTrue("Line chart với giá trị null không ném lỗi", true);
        } catch (Exception e) {
            checkTrue("Line chart với giá trị null không ném lỗi", false);
        }

        System.out.println("\n=== KẾT QUẢ: " + pass + " pass, " + fail + " fail ===");
        if (fail > 0) System.exit(1);
    }

    /** Vẽ chart ra PNG, kiểm tra không lỗi + ảnh không rỗng. */
    private static void check(String name, JPanel panel, String fileName) {
        try {
            panel.setSize(640, 360);
            panel.doLayout();
            JFreeChart chart = findChart(panel);
            if (chart == null) {
                checkTrue(name, false);
                return;
            }
            File out = new File(OUT_DIR + fileName);
            ChartUtils.saveChartAsPNG(out, chart, 640, 360);
            long size = out.length();
            checkTrue(name + " (PNG " + size + " bytes)", size > 1000);
        } catch (Exception e) {
            System.out.println("  [FAIL] " + name + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            checkTrue(name, false);
        }
    }

    /** Tìm JFreeChart trong cây component (bao gồm chính node gốc). */
    private static JFreeChart findChart(Container root) {
        // ChartHelper trả về chính ChartPanel (nó extends JPanel) nên phải
        // kiểm tra node gốc trước, không chỉ các con.
        if (root instanceof ChartPanel cp) return cp.getChart();
        for (Component c : root.getComponents()) {
            if (c instanceof ChartPanel cp) return cp.getChart();
            if (c instanceof Container child) {
                JFreeChart r = findChart(child);
                if (r != null) return r;
            }
        }
        return null;
    }

    private static void checkTrue(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (ok) pass++; else fail++;
    }
}