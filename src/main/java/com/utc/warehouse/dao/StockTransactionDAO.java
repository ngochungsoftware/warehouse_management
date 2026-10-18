package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.StockTransaction;
import com.utc.warehouse.enums.TransactionType;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;

public class StockTransactionDAO extends GenericDAO<StockTransaction, Integer> {

    @Override
    protected Class<StockTransaction> entityClass() { return StockTransaction.class; }

    /**
     * Lấy tất cả phiếu, JOIN FETCH user + details (+ product) để tránh
     * LazyInitializationException khi UI đọc ngoài transaction.
     *
     * <p>Lưu ý: Hibernate không cho dùng setMaxResults kèm collection fetch.
     * Nếu sau này cần phân trang, phải query 2-step (query id, sau đó fetch).
     */
    public List<StockTransaction> findAllOrdered() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT t FROM StockTransaction t " +
                    "LEFT JOIN FETCH t.user " +
                    "LEFT JOIN FETCH t.details d " +
                    "LEFT JOIN FETCH d.product " +
                    "ORDER BY t.createdAt DESC",
                    StockTransaction.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<StockTransaction> findByType(TransactionType type) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT t FROM StockTransaction t " +
                    "LEFT JOIN FETCH t.user " +
                    "LEFT JOIN FETCH t.details d " +
                    "LEFT JOIN FETCH d.product " +
                    "WHERE t.type = :ty " +
                    "ORDER BY t.createdAt DESC",
                    StockTransaction.class)
                    .setParameter("ty", type)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<StockTransaction> findInRange(LocalDateTime from, LocalDateTime to, TransactionType type) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT DISTINCT t FROM StockTransaction t " +
                    "LEFT JOIN FETCH t.user " +
                    "LEFT JOIN FETCH t.details d " +
                    "LEFT JOIN FETCH d.product " +
                    "WHERE t.createdAt BETWEEN :f AND :to");
            if (type != null) jpql.append(" AND t.type = :ty");
            jpql.append(" ORDER BY t.createdAt DESC");
            var q = em.createQuery(jpql.toString(), StockTransaction.class)
                    .setParameter("f", from)
                    .setParameter("to", to);
            if (type != null) q.setParameter("ty", type);
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    /** Lấy 1 phiếu kèm đầy đủ details + product + user. Dùng cho dialog chi tiết. */
    public StockTransaction findByIdWithDetails(Integer id) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            List<StockTransaction> list = em.createQuery(
                    "SELECT DISTINCT t FROM StockTransaction t " +
                    "LEFT JOIN FETCH t.user " +
                    "LEFT JOIN FETCH t.details d " +
                    "LEFT JOIN FETCH d.product " +
                    "WHERE t.id = :id",
                    StockTransaction.class)
                    .setParameter("id", id)
                    .getResultList();
            return list.isEmpty() ? null : list.get(0);
        } finally {
            em.close();
        }
    }
}
