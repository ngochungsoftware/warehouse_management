package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.PurchaseOrder;
import com.utc.warehouse.enums.POStatus;
import jakarta.persistence.EntityManager;

import java.util.List;

public class PurchaseOrderDAO extends GenericDAO<PurchaseOrder, Integer> {

    @Override
    protected Class<PurchaseOrder> entityClass() { return PurchaseOrder.class; }

    /**
     * Lấy tất cả PO, JOIN FETCH items + product để tránh
     * LazyInitializationException khi UI đọc ngoài transaction.
     *
     * <p>supplier và user đã EAGER, không cần fetch thêm.
     */
    public List<PurchaseOrder> findAllOrdered() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT p FROM PurchaseOrder p " +
                    "LEFT JOIN FETCH p.items i " +
                    "LEFT JOIN FETCH i.product " +
                    "ORDER BY p.createdAt DESC",
                    PurchaseOrder.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<PurchaseOrder> findByStatus(POStatus status) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT p FROM PurchaseOrder p " +
                    "LEFT JOIN FETCH p.items i " +
                    "LEFT JOIN FETCH i.product " +
                    "WHERE p.status = :st " +
                    "ORDER BY p.createdAt DESC",
                    PurchaseOrder.class)
                    .setParameter("st", status)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Lấy 1 PO kèm items + product cho dialog chi tiết. */
    public PurchaseOrder findByIdWithItems(Integer id) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            List<PurchaseOrder> list = em.createQuery(
                    "SELECT DISTINCT p FROM PurchaseOrder p " +
                    "LEFT JOIN FETCH p.items i " +
                    "LEFT JOIN FETCH i.product " +
                    "WHERE p.id = :id",
                    PurchaseOrder.class)
                    .setParameter("id", id)
                    .getResultList();
            return list.isEmpty() ? null : list.get(0);
        } finally {
            em.close();
        }
    }
}
