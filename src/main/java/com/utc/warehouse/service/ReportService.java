package com.utc.warehouse.service;

import com.utc.warehouse.dao.ReportDAO;

import java.time.LocalDate;
import java.util.List;

public class ReportService {

    private final ReportDAO reportDAO = new ReportDAO();

    public List<ReportDAO.InventoryRow> getInventory() {
        return reportDAO.getInventory();
    }

    public int getOnHand(Integer productId) {
        return reportDAO.getOnHand(productId);
    }

    /** Lượng nhập/xuất theo ngày trong khoảng [from, to] — dùng cho stacked bar. */
    public List<ReportDAO.StockFlowRow> stockFlowByDate(LocalDate from, LocalDate to) {
        return reportDAO.stockFlowByDate(from, to);
    }
}
