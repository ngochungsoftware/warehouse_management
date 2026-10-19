package com.utc.warehouse;

import com.utc.warehouse.dao.FinanceReportDAO;
import com.utc.warehouse.service.FinanceReportService;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * BizTest10: Test chart thật từ DB vào Dashboard.
 *  - Query revenueByDate(7 ngày)
 *  - Query inventoryByCategory()
 *  - Render ra PNG để xác nhận hiển thị đúng.
 */
public class BizTest10 {

    public static void main(String[] args) throws Exception {
        FinanceReportService svc = new FinanceReportService();
        String outDir = "build/charts";
        new File(outDir).mkdirs();

        // 1) Revenue 7 ngày
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);
        List<FinanceReportDAO.RevenueRow> rows = svc.revenueByDate(from, to);
        System.out.println("[1] Query revenueByDate(" + from + " → " + to + "): " + rows.size() + " ngày có data");
        for (FinanceReportDAO.RevenueRow r : rows) {
            System.out.println("    " + r.getDate() + ": " + r.getNetRevenue() + " đ ("
                    + r.getInvoiceCount() + " hóa đơn)");
        }
        Map<LocalDate, BigDecimal> data = new TreeMap<>();
        for (int i = 0; i <= 6; i++) data.put(from.plusDays(i), BigDecimal.ZERO);
        for (FinanceReportDAO.RevenueRow r : rows) data.put(r.getDate(), r.getNetRevenue());

        JPanel linePanel = ChartHelper.lineChartTimeSeries("Doanh thu 7 ngày gần nhất", data);
        saveChartPanelAsPng(linePanel, outDir + "/dashboard_revenue_7days.png", 700, 340);
        System.out.println("    [OK] PNG: dashboard_revenue_7days.png");

        // 2) Tồn kho theo danh mục
        List<FinanceReportDAO.CategoryStockRow> catRows = svc.inventoryByCategory();
        System.out.println("[2] Query inventoryByCategory: " + catRows.size() + " danh mục");
        for (FinanceReportDAO.CategoryStockRow r : catRows) {
            System.out.println("    " + r.getCategoryName() + ": " + r.getQuantity() + " đơn vị");
        }
        if (catRows.isEmpty()) {
            System.out.println("    [skip] Không có dữ liệu tồn kho");
        } else {
            Map<String, Number> pieData = new LinkedHashMap<>();
            for (FinanceReportDAO.CategoryStockRow r : catRows) {
                pieData.put(r.getCategoryName(), r.getQuantity());
            }
            JPanel piePanel = ChartHelper.pieChart("Tồn kho theo danh mục", pieData);
            saveChartPanelAsPng(piePanel, outDir + "/dashboard_inventory_by_category.png", 700, 340);
            System.out.println("    [OK] PNG: dashboard_inventory_by_category.png");
        }

        // 3) Tổng hợp dashboard summary
        FinanceReportDAO.DashboardSummary s = svc.dashboardSummary();
        System.out.println("[3] Dashboard summary:");
        System.out.println("    Hôm nay: doanh thu = " + s.todayRevenue + ", lợi nhuận = " + s.todayProfit);
        System.out.println("    Tháng:   doanh thu = " + s.monthRevenue + ", lợi nhuận = " + s.monthProfit
                + ", " + s.monthInvoiceCount + " hóa đơn");
        System.out.println("    Kho:     tồn giá vốn = " + s.inventoryValue + ", "
                + s.productCount + " SP, " + s.lowStockCount + " SP sắp hết");
        System.out.println("    Công nợ: " + s.totalReceivable);

        // List output files
        File dir = new File(outDir);
        File[] files = dir.listFiles((d, n) -> n.startsWith("dashboard_") && n.endsWith(".png"));
        if (files != null && files.length > 0) {
            System.out.println("=== Tạo " + files.length + " file dashboard PNG ===");
            for (File f : files) System.out.println("  " + f.getName() + " (" + (f.length()/1024) + " KB)");
        } else {
            System.out.println("[warn] Không có PNG nào được tạo");
        }
    }

    private static void saveChartPanelAsPng(JPanel p, String path, int w, int h) throws Exception {
        p.setSize(w, h);
        if (p instanceof ChartPanel cp) {
            JFreeChart chart = cp.getChart();
            ChartUtils.saveChartAsPNG(new File(path), chart, w, h);
        }
    }
}
