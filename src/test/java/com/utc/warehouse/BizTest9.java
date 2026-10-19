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
 * BizTest9: Render thử 4 loại chart JFreeChart ra PNG để xác nhận hoạt động.
 */
public class BizTest9 {

    public static void main(String[] args) throws Exception {
        String outDir = "build/charts";
        new File(outDir).mkdirs();

        // 1) Line chart: doanh thu 7 ngày gần nhất
        Map<LocalDate, BigDecimal> dailyRevenue = new TreeMap<>();
        dailyRevenue.put(LocalDate.now().minusDays(6), new BigDecimal("2500000"));
        dailyRevenue.put(LocalDate.now().minusDays(5), new BigDecimal("3100000"));
        dailyRevenue.put(LocalDate.now().minusDays(4), new BigDecimal("1800000"));
        dailyRevenue.put(LocalDate.now().minusDays(3), new BigDecimal("4200000"));
        dailyRevenue.put(LocalDate.now().minusDays(2), new BigDecimal("5500000"));
        dailyRevenue.put(LocalDate.now().minusDays(1), new BigDecimal("3800000"));
        dailyRevenue.put(LocalDate.now(),                new BigDecimal("6100000"));
        JPanel linePanel = ChartHelper.lineChartTimeSeries("Doanh thu 7 ngày gần nhất", dailyRevenue);
        savePng(linePanel, outDir + "/01_line_revenue.png");
        System.out.println("[1] Line chart OK: " + dailyRevenue.size() + " điểm");

        // 2) Bar chart: top 5 SP bán chạy
        Map<String, Number> topProducts = new LinkedHashMap<>();
        topProducts.put("Gạo ST25 5kg", 156);
        topProducts.put("Mì Hảo Hảo", 132);
        topProducts.put("Nước suối 1.5L", 98);
        topProducts.put("Dầu gội Clear", 76);
        topProducts.put("Bánh Oreo", 64);
        JPanel barPanel = ChartHelper.barChart("Top 5 SP bán chạy", topProducts);
        savePng(barPanel, outDir + "/02_bar_top_products.png");
        System.out.println("[2] Bar chart OK: " + topProducts.size() + " cột");

        // 3) Pie chart: tỉ lệ tồn kho theo danh mục
        Map<String, Number> invByCategory = new LinkedHashMap<>();
        invByCategory.put("Lương thực", 850);
        invByCategory.put("Nước uống", 620);
        invByCategory.put("Chăm sóc", 380);
        invByCategory.put("Bánh kẹo", 290);
        invByCategory.put("Khác", 180);
        JPanel piePanel = ChartHelper.pieChart("Tồn kho theo danh mục", invByCategory);
        savePng(piePanel, outDir + "/03_pie_inventory.png");
        System.out.println("[3] Pie chart OK: " + invByCategory.size() + " miếng");

        // 4) Grouped bar: doanh thu vs lợi nhuận 6 tháng
        Map<String, Map<String, Number>> grouped = new LinkedHashMap<>();
        Map<String, Number> revenue = new LinkedHashMap<>();
        revenue.put("T5", 120_000_000d);
        revenue.put("T6", 145_000_000d);
        revenue.put("T7", 138_000_000d);
        revenue.put("T8", 165_000_000d);
        revenue.put("T9", 180_000_000d);
        revenue.put("T10", 210_000_000d);
        Map<String, Number> profit = new LinkedHashMap<>();
        profit.put("T5", 28_000_000d);
        profit.put("T6", 35_000_000d);
        profit.put("T7", 32_000_000d);
        profit.put("T8", 42_000_000d);
        profit.put("T9", 50_000_000d);
        profit.put("T10", 65_000_000d);
        grouped.put("Doanh thu", revenue);
        grouped.put("Lợi nhuận", profit);
        JPanel groupedPanel = ChartHelper.barChartGrouped("Doanh thu vs Lợi nhuận 6 tháng", grouped);
        savePng(groupedPanel, outDir + "/04_grouped_revenue_profit.png");
        System.out.println("[4] Grouped bar chart OK: 2 series x " + revenue.size() + " tháng");

        // 5) Stacked bar: nhập vs xuất theo tháng
        Map<String, Map<String, Number>> stacked = new LinkedHashMap<>();
        Map<String, Number> importMap = new LinkedHashMap<>();
        Map<String, Number> exportMap = new LinkedHashMap<>();
        for (int m = 5; m <= 10; m++) {
            String key = "T" + m;
            importMap.put(key, 50_000_000d + m * 3_000_000d);
            exportMap.put(key, 30_000_000d + m * 2_000_000d);
        }
        stacked.put("Nhập kho", importMap);
        stacked.put("Xuất kho", exportMap);
        JPanel stackedPanel = ChartHelper.stackedBarChart("Nhập/Xuất theo tháng", stacked);
        savePng(stackedPanel, outDir + "/05_stacked_import_export.png");
        System.out.println("[5] Stacked bar chart OK");

        // Verify files
        File dir = new File(outDir);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".png"));
        long totalSize = 0;
        for (File f : files) totalSize += f.length();
        System.out.println("=== Tạo " + files.length + " file PNG, tổng " + (totalSize/1024) + " KB ===");
        for (File f : files) System.out.println("  " + f.getName() + " (" + (f.length()/1024) + " KB)");
    }

    private static void savePng(JPanel p, String path) throws Exception {
        p.setSize(640, 360);
        // ChartHelper trả thẳng ChartPanel
        JFreeChart chart;
        if (p instanceof ChartPanel cp) {
            chart = cp.getChart();
        } else {
            ChartPanel cp = findChartPanel(p);
            if (cp == null) {
                System.out.println("    [warn] Không tìm thấy ChartPanel");
                return;
            }
            chart = cp.getChart();
        }
        ChartUtils.saveChartAsPNG(new File(path), chart, 640, 360);
    }

    private static ChartPanel findChartPanel(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof ChartPanel cp) return cp;
            if (c instanceof Container ch) {
                ChartPanel f = findChartPanel(ch);
                if (f != null) return f;
            }
        }
        return null;
    }
}
