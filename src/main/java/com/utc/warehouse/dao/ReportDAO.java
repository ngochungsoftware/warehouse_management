package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import jakarta.persistence.EntityManager;

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
}
