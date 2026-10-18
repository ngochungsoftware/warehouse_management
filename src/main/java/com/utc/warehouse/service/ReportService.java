package com.utc.warehouse.service;

import com.utc.warehouse.dao.ReportDAO;

import java.util.List;

public class ReportService {

    private final ReportDAO reportDAO = new ReportDAO();

    public List<ReportDAO.InventoryRow> getInventory() {
        return reportDAO.getInventory();
    }

    public int getOnHand(Integer productId) {
        return reportDAO.getOnHand(productId);
    }
}
