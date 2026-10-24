package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Các truy vấn báo cáo tồn kho.
 * Tồn được tính động: SUM(IMPORT) - SUM(EXPORT).
 */
public class ReportDAO {

    /** Dòng tồn kho cho từng sản phẩm. */
    public static final class InventoryRow {
        private final Integer productId;
        private final String sku;
        private final String name;
        private final String unit;
        private final Integer minStock;
        private final Long onHand;
        private final String categoryName;

        public InventoryRow(Integer productId, String sku, String name, String unit,
                            Integer minStock, Long onHand, String categoryName) {
            this.productId = productId;
            this.sku = sku;
            this.name = name;
            this.unit = unit;
            this.minStock = minStock;
            this.onHand = onHand == null ? 0L : onHand;
            this.categoryName = categoryName;
        }
        public Integer getProductId() { return productId; }
        public String getSku()        { return sku; }
        public String getName()       { return name; }
        public String getUnit()       { return unit; }
        public Integer getMinStock()  { return minStock; }
        public Long    getOnHand()    { return onHand; }
        public String  getCategoryName() { return categoryName; }
    }

    public List<InventoryRow> getInventory() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createQuery(
                    "SELECT p.id, p.sku, p.name, p.unit, p.minStock, " +
                            " COALESCE(SUM(CASE WHEN t.type = com.utc.warehouse.enums.TransactionType.IMPORT " +
                            "                  THEN d.quantity ELSE -d.quantity END), 0), " +
                            " c.name " +
                            "FROM Product p " +
                            "LEFT JOIN p.category c " +
                            "LEFT JOIN StockTransactionDetail d ON d.product = p " +
                            "LEFT JOIN d.transaction t " +
                            "GROUP BY p.id, p.sku, p.name, p.unit, p.minStock, c.name " +
                            "ORDER BY p.name")
                    .getResultList();

            return rows.stream()
                    .map(r -> new InventoryRow(
                            (Integer) r[0], (String) r[1], (String) r[2], (String) r[3],
                            (Integer) r[4], ((Number) r[5]).longValue(), (String) r[6]))
                    .toList();
        } finally {
            em.close();
        }
    }

    /**
     * Tính tồn hiện tại của 1 sản phẩm. Dùng cho check trước khi xuất kho.
     */
    public int getOnHand(Integer productId) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            Long sum = em.createQuery(
                    "SELECT COALESCE(SUM(CASE WHEN t.type = com.utc.warehouse.enums.TransactionType.IMPORT " +
                            "                  THEN d.quantity ELSE -d.quantity END), 0) " +
                            "FROM StockTransactionDetail d LEFT JOIN d.transaction t " +
                            "WHERE d.product.id = :pid",
                    Long.class)
                    .setParameter("pid", productId)
                    .getSingleResult();
            return sum == null ? 0 : sum.intValue();
        } finally {
            em.close();
        }
    }

    /* ============== Nhập/Xuất theo ngày (cho stacked bar) ============== */

    /** Lượng nhập hoặc xuất trong 1 ngày. */
    public static final class StockFlowRow {
        private final LocalDate date;
        private final String type;   // IMPORT / EXPORT
        private final long quantity;

        public StockFlowRow(LocalDate date, String type, long quantity) {
            this.date = date; this.type = type; this.quantity = quantity;
        }
        public LocalDate getDate() { return date; }
        public String getType() { return type; }
        public long getQuantity() { return quantity; }
    }

    /**
     * Lượng nhập/xuất theo ngày trong khoảng [from, to] — dùng cho stacked bar.
     *
     * <p>Ngày lấy từ {@code createdAt} (LocalDateTime) của StockTransaction,
     * cắt phần giờ bằng {@code DATE()} để gom theo ngày.
     *
     * <p>Lưu ý: {@code DATE(...)} là hàm SQL nên Hibernate trả về
     * {@link java.sql.Date}, KHÔNG phải {@link LocalDate}. Phải chuyển đổi
     * ở Java — cast thẳng sẽ ném ClassCastException.
     */
    public List<StockFlowRow> stockFlowByDate(LocalDate from, LocalDate to) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createQuery(
                    "SELECT DATE(t.createdAt), t.type, COALESCE(SUM(d.quantity), 0) " +
                            "FROM StockTransaction t LEFT JOIN t.details d " +
                            "WHERE DATE(t.createdAt) >= :from AND DATE(t.createdAt) <= :to " +
                            "GROUP BY DATE(t.createdAt), t.type " +
                            "ORDER BY DATE(t.createdAt)")
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .getResultList();

            List<StockFlowRow> out = new ArrayList<>();
            for (Object[] r : rows) {
                LocalDate d = toLocalDate(r[0]);
                String type = r[1] == null ? "" : r[1].toString();
                long qty = ((Number) r[2]).longValue();
                out.add(new StockFlowRow(d, type, qty));
            }
            return out;
        } finally {
            em.close();
        }
    }

    /**
     * Chuyển giá trị ngày trả về từ query về {@link LocalDate}.
     * Chịu được cả {@link java.sql.Date}, {@link LocalDateTime}, {@link LocalDate}
     * và {@link java.util.Date} để không phụ thuộc cách driver trả về.
     */
    private static LocalDate toLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate ld) return ld;
        if (o instanceof java.sql.Date sd) return sd.toLocalDate();
        if (o instanceof LocalDateTime ldt) return ldt.toLocalDate();
        if (o instanceof java.util.Date ud)
            return new java.sql.Date(ud.getTime()).toLocalDate();
        return LocalDate.parse(o.toString());
    }
}
