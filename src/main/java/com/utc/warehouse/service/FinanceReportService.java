package com.utc.warehouse.service;

import com.utc.warehouse.dao.FinanceReportDAO;

import java.time.LocalDate;
import java.util.List;

public class FinanceReportService {

    private final FinanceReportDAO dao = new FinanceReportDAO();

    public List<FinanceReportDAO.RevenueRow> revenueByDate(LocalDate from, LocalDate to) {
        return dao.revenueByDate(from, to);
    }

    public List<FinanceReportDAO.TopProductRow> topProducts(LocalDate from, LocalDate to, int limit) {
        return dao.topProducts(from, to, limit);
    }

    public List<FinanceReportDAO.CustomerDebtRow> customerDebts() {
        return dao.customerDebts();
    }

    public FinanceReportDAO.DashboardSummary dashboardSummary() {
        return dao.dashboardSummary();
    }

    /** Tổng số lượng tồn theo danh mục — dùng cho pie chart trên dashboard. */
    public List<FinanceReportDAO.CategoryStockRow> inventoryByCategory() {
        return dao.inventoryByCategory();
    }
}
