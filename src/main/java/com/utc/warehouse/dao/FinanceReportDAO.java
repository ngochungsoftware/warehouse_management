package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.enums.InvoiceStatus;
import com.utc.warehouse.enums.PaymentType;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FinanceReportDAO {

    /* ============== Doanh thu / Lợi nhuận theo ngày ============== */
    public static final class RevenueRow {
        public final LocalDate date;
        public final long invoiceCount;
        public final BigDecimal revenue;     // tổng tiền bán (subtotal + VAT, chưa trừ CK)
        public final BigDecimal discount;
        public final BigDecimal netRevenue; // revenue - discount
        public final BigDecimal cogs;        // giá vốn hàng bán
        public final BigDecimal vat;        // VAT thu
        public final BigDecimal profit;      // lợi nhuận = netRevenue - cogs
        public final BigDecimal profitMargin; // % margin

        public RevenueRow(LocalDate date, long invoiceCount, BigDecimal revenue, BigDecimal discount,
                          BigDecimal netRevenue, BigDecimal cogs, BigDecimal vat, BigDecimal profit) {
            this.date = date;
            this.invoiceCount = invoiceCount;
            this.revenue = revenue;
            this.discount = discount;
            this.netRevenue = netRevenue;
            this.cogs = cogs;
            this.vat = vat;
            this.profit = profit;
            this.profitMargin = netRevenue.signum() == 0 ? BigDecimal.ZERO
                    : profit.multiply(BigDecimal.valueOf(100)).divide(netRevenue, 2, java.math.RoundingMode.HALF_UP);
        }
        public LocalDate getDate() { return date; }
        public long getInvoiceCount() { return invoiceCount; }
        public BigDecimal getRevenue() { return revenue; }
        public BigDecimal getDiscount() { return discount; }
        public BigDecimal getNetRevenue() { return netRevenue; }
        public BigDecimal getCogs() { return cogs; }
        public BigDecimal getVat() { return vat; }
        public BigDecimal getProfit() { return profit; }
        public BigDecimal getProfitMargin() { return profitMargin; }
    }

    public List<RevenueRow> revenueByDate(LocalDate from, LocalDate to) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> cogsRows = em.createQuery(
                    "SELECT i.issueDate, " +
                            " COALESCE(SUM(it.quantity * it.costPriceSnapshot), 0) " +
                    "FROM InvoiceItem it JOIN it.invoice i " +
                    "WHERE i.status <> com.utc.warehouse.enums.InvoiceStatus.CANCELLED " +
                            "  AND i.issueDate >= :from AND i.issueDate <= :to " +
                    "GROUP BY i.issueDate")
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .getResultList();

            Map<LocalDate, BigDecimal> cogsMap = new HashMap<>();
            for (Object[] r : cogsRows) {
                cogsMap.put((LocalDate) r[0], toBig(r[1]));
            }

            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createQuery(
                    "SELECT i.issueDate, " +
                            " COUNT(i), " +
                            " COALESCE(SUM(i.subtotal), 0), " +
                            " COALESCE(SUM(i.discount), 0), " +
                            " COALESCE(SUM(i.vatAmount), 0) " +
                    "FROM Invoice i " +
                    "WHERE i.status <> com.utc.warehouse.enums.InvoiceStatus.CANCELLED " +
                            "  AND i.issueDate >= :from AND i.issueDate <= :to " +
                    "GROUP BY i.issueDate ORDER BY i.issueDate")
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .getResultList();
            List<RevenueRow> out = new ArrayList<>();
            for (Object[] r : rows) {
                LocalDate date = (LocalDate) r[0];
                long cnt = ((Number) r[1]).longValue();
                BigDecimal sub = toBig(r[2]);
                BigDecimal disc = toBig(r[3]);
                BigDecimal vat = toBig(r[4]);
                BigDecimal cogs = cogsMap.getOrDefault(date, BigDecimal.ZERO);
                BigDecimal net = sub.subtract(disc);
                BigDecimal profit = net.subtract(cogs);
                out.add(new RevenueRow(date, cnt, sub.add(vat), disc, net, cogs, vat, profit));
            }
            return out;
        } finally {
            em.close();
        }
    }

    public static final class TopProductRow {
        public final Integer productId;
        public final String sku;
        public final String name;
        public final long quantitySold;
        public final BigDecimal revenue;
        public final BigDecimal profit;

        public TopProductRow(Integer productId, String sku, String name,
                             long quantitySold, BigDecimal revenue, BigDecimal profit) {
            this.productId = productId; this.sku = sku; this.name = name;
            this.quantitySold = quantitySold; this.revenue = revenue; this.profit = profit;
        }
        public Integer getProductId() { return productId; }
        public String getSku() { return sku; }
        public String getName() { return name; }
        public long getQuantitySold() { return quantitySold; }
        public BigDecimal getRevenue() { return revenue; }
        public BigDecimal getProfit() { return profit; }
    }

    public List<TopProductRow> topProducts(LocalDate from, LocalDate to, int limit) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createQuery(
                    "SELECT p.id, p.sku, p.name, " +
                            " COALESCE(SUM(it.quantity), 0), " +
                            " COALESCE(SUM(it.quantity * it.unitPrice - it.discount), 0), " +
                            " COALESCE(SUM(it.quantity * (it.unitPrice - it.costPriceSnapshot) - it.discount), 0) " +
                            "FROM InvoiceItem it " +
                            " JOIN it.invoice i " +
                            " JOIN it.product p " +
                            "WHERE i.status <> com.utc.warehouse.enums.InvoiceStatus.CANCELLED " +
                            "  AND i.issueDate >= :from AND i.issueDate <= :to " +
                            "GROUP BY p.id, p.sku, p.name " +
                            "ORDER BY SUM(it.quantity * it.unitPrice) DESC")
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .setMaxResults(limit)
                    .getResultList();
            List<TopProductRow> out = new ArrayList<>();
            for (Object[] r : rows) {
                out.add(new TopProductRow(
                        (Integer) r[0], (String) r[1], (String) r[2],
                        ((Number) r[3]).longValue(), toBig(r[4]), toBig(r[5])));
            }
            return out;
        } finally {
            em.close();
        }
    }

    public static final class CustomerDebtRow {
        public final Integer customerId;
        public final String customerName;
        public final long invoiceCount;
        public final BigDecimal total;
        public final BigDecimal paid;
        public final BigDecimal debt;

        public CustomerDebtRow(Integer customerId, String customerName, long invoiceCount,
                               BigDecimal total, BigDecimal paid, BigDecimal debt) {
            this.customerId = customerId; this.customerName = customerName;
            this.invoiceCount = invoiceCount; this.total = total; this.paid = paid; this.debt = debt;
        }
        public Integer getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public long getInvoiceCount() { return invoiceCount; }
        public BigDecimal getTotal() { return total; }
        public BigDecimal getPaid() { return paid; }
        public BigDecimal getDebt() { return debt; }
    }

    public List<CustomerDebtRow> customerDebts() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createQuery(
                    "SELECT c.id, c.name, COUNT(i), " +
                            " COALESCE(SUM(i.total), 0), COALESCE(SUM(i.paid), 0), COALESCE(SUM(i.debt), 0) " +
                            "FROM Invoice i LEFT JOIN i.customer c " +
                            "WHERE i.status <> com.utc.warehouse.enums.InvoiceStatus.CANCELLED " +
                            "  AND (i.status = com.utc.warehouse.enums.InvoiceStatus.ISSUED " +
                            "    OR i.status = com.utc.warehouse.enums.InvoiceStatus.PARTIAL) " +
                            "GROUP BY c.id, c.name " +
                            "HAVING SUM(i.debt) > 0 " +
                            "ORDER BY SUM(i.debt) DESC")
                    .getResultList();
            List<CustomerDebtRow> out = new ArrayList<>();
            for (Object[] r : rows) {
                out.add(new CustomerDebtRow(
                        (Integer) r[0], (String) r[1], ((Number) r[2]).longValue(),
                        toBig(r[3]), toBig(r[4]), toBig(r[5])));
            }
            return out;
        } finally {
            em.close();
        }
    }

    public static final class DashboardSummary {
        public final BigDecimal todayRevenue;
        public final BigDecimal todayProfit;
        public final BigDecimal monthRevenue;
        public final BigDecimal monthProfit;
        public final long monthInvoiceCount;
        public final BigDecimal totalReceivable; // tổng phải thu (công nợ KH)
        public final BigDecimal inventoryValue;  // giá trị tồn kho theo cost
        public final long productCount;
        public final long lowStockCount;

        public DashboardSummary(BigDecimal todayRevenue, BigDecimal todayProfit, BigDecimal monthRevenue,
                                BigDecimal monthProfit, long monthInvoiceCount, BigDecimal totalReceivable,
                                BigDecimal inventoryValue, long productCount, long lowStockCount) {
            this.todayRevenue = todayRevenue; this.todayProfit = todayProfit;
            this.monthRevenue = monthRevenue; this.monthProfit = monthProfit;
            this.monthInvoiceCount = monthInvoiceCount;
            this.totalReceivable = totalReceivable;
            this.inventoryValue = inventoryValue;
            this.productCount = productCount; this.lowStockCount = lowStockCount;
        }
    }

    public DashboardSummary dashboardSummary() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);

            // Hôm nay: Doanh thu lấy trực tiếp từ Invoice (không JOIN items, không trừ lặp discount)
            BigDecimal todayRevenue = toBig(em.createQuery(
                    "SELECT COALESCE(SUM(i.total), 0) FROM Invoice i " +
                            "WHERE i.issueDate = :d " +
                            "  AND i.status <> com.utc.warehouse.enums.InvoiceStatus.CANCELLED")
                    .setParameter("d", today)
                    .getSingleResult());

            // Hôm nay: Lợi nhuận lấy từ InvoiceItem JOIN invoice (không nhân bản do xuất phát từ item)
            BigDecimal todayProfit = toBig(em.createQuery(
                    "SELECT COALESCE(SUM(it.quantity * (it.unitPrice - it.costPriceSnapshot) - it.discount), 0) " +
                    "FROM InvoiceItem it JOIN it.invoice i " +
                    "WHERE i.issueDate = :d " +
                    "  AND i.status <> com.utc.warehouse.enums.InvoiceStatus.CANCELLED")
                    .setParameter("d", today)
                    .getSingleResult());

            // Tháng này: Doanh thu và Số lượng hóa đơn lấy từ Invoice
            Object[] monthInv = (Object[]) em.createQuery(
                    "SELECT COALESCE(SUM(i.total), 0), COUNT(i) " +
                    "FROM Invoice i " +
                    "WHERE i.issueDate >= :from " +
                    "  AND i.status <> com.utc.warehouse.enums.InvoiceStatus.CANCELLED")
                    .setParameter("from", monthStart)
                    .getSingleResult();
            BigDecimal monthRevenue = toBig(monthInv[0]);
            long monthCount = ((Number) monthInv[1]).longValue();

            // Tháng này: Lợi nhuận lấy từ InvoiceItem JOIN invoice
            BigDecimal monthProfit = toBig(em.createQuery(
                    "SELECT COALESCE(SUM(it.quantity * (it.unitPrice - it.costPriceSnapshot) - it.discount), 0) " +
                    "FROM InvoiceItem it JOIN it.invoice i " +
                    "WHERE i.issueDate >= :from " +
                    "  AND i.status <> com.utc.warehouse.enums.InvoiceStatus.CANCELLED")
                    .setParameter("from", monthStart)
                    .getSingleResult());

            BigDecimal receivable = toBig(em.createQuery(
                    "SELECT COALESCE(SUM(i.debt), 0) FROM Invoice i " +
                            "WHERE (i.status = com.utc.warehouse.enums.InvoiceStatus.ISSUED " +
                            "    OR i.status = com.utc.warehouse.enums.InvoiceStatus.PARTIAL)")
                    .getSingleResult());

            Object[] invRow = (Object[]) em.createQuery(
                    "SELECT COALESCE(SUM(p.costPrice * COALESCE(onh.qty, 0)), 0), " +
                            " COUNT(p), " +
                            " COALESCE(SUM(CASE WHEN COALESCE(onh.qty, 0) < p.minStock THEN 1 ELSE 0 END), 0) " +
                            "FROM Product p " +
                            "LEFT JOIN (SELECT d.product.id AS pid, " +
                            "                SUM(CASE WHEN t.type = com.utc.warehouse.enums.TransactionType.IMPORT " +
                            "                         THEN d.quantity ELSE -d.quantity END) AS qty " +
                            "           FROM StockTransactionDetail d LEFT JOIN d.transaction t " +
                            "           GROUP BY d.product.id) onh ON onh.pid = p.id " +
                            "WHERE p.active = TRUE")
                    .getSingleResult();

            return new DashboardSummary(
                    todayRevenue, todayProfit,
                    monthRevenue, monthProfit, monthCount,
                    receivable,
                    toBig(invRow[0]),
                    ((Number) invRow[1]).longValue(),
                    ((Number) invRow[2]).longValue()
            );
        } finally {
            em.close();
        }
    }

    public List<CategoryStockRow> inventoryByCategory() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createQuery(
                    "SELECT c.name, " +
                            " COALESCE(SUM(CASE WHEN t.type = com.utc.warehouse.enums.TransactionType.IMPORT " +
                            "                  THEN d.quantity ELSE -d.quantity END), 0) " +
                            "FROM Product p " +
                            "LEFT JOIN p.category c " +
                            "LEFT JOIN StockTransactionDetail d ON d.product = p " +
                            "LEFT JOIN d.transaction t " +
                            "WHERE p.active = TRUE " +
                            "GROUP BY c.name")
                    .getResultList();

            List<CategoryStockRow> out = new ArrayList<>();
            for (Object[] r : rows) {
                String name = r[0] == null ? "(Chưa phân loại)" : (String) r[0];
                long qty = ((Number) r[1]).longValue();
                if (qty > 0) out.add(new CategoryStockRow(name, qty));
            }
            out.sort((a, b) -> Long.compare(b.quantity, a.quantity));
            return out;
        } finally {
            em.close();
        }
    }

    public static final class CategoryStockRow {
        public final String categoryName;
        public final long quantity;
        public CategoryStockRow(String n, long q) { this.categoryName = n; this.quantity = q; }
        public String getCategoryName() { return categoryName; }
        public long getQuantity() { return quantity; }
    }

    private static BigDecimal toBig(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}
