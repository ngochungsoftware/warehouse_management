package com.utc.warehouse.dao;

import com.utc.warehouse.entity.Invoice;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

/**
 * DAO riêng cho {@link Invoice}.
 *
 * <p>Khác biệt so với {@link GenericDAO}: các query đọc đều dùng
 * {@code JOIN FETCH} cho các collection liên quan (items, customer) và
 * giữ EntityManager mở trong suốt lúc map kết quả. Nhờ vậy entity trả về
 * đã nạp đủ dữ liệu, đọc ở UI (ngoài transaction) sẽ không bị
 * {@code LazyInitializationException}.
 */
public class InvoiceDAO extends GenericDAO<Invoice, Integer> {

    @Override
    protected Class<Invoice> entityClass() { return Invoice.class; }

    /**
     * Lấy tất cả hóa đơn kèm items + customer (JOIN FETCH).
     * Dùng cho bảng danh sách và cho màn xem chi tiết.
     */
    public List<Invoice> findAllWithDetails() {
        EntityManager em = com.utc.warehouse.config.HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT DISTINCT i FROM Invoice i " +
                            "LEFT JOIN FETCH i.customer c " +
                            "LEFT JOIN FETCH i.items it " +
                            "LEFT JOIN FETCH it.product p", Invoice.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /** Lấy 1 hóa đơn theo mã, kèm items + customer. */
    public Optional<Invoice> findByCodeWithDetails(String code) {
        EntityManager em = com.utc.warehouse.config.HibernateUtil.getEntityManager();
        try {
            List<Invoice> rows = em.createQuery(
                            "SELECT DISTINCT i FROM Invoice i " +
                            "LEFT JOIN FETCH i.customer c " +
                            "LEFT JOIN FETCH i.items it " +
                            "LEFT JOIN FETCH it.product p " +
                            "WHERE i.code = :code", Invoice.class)
                    .setParameter("code", code)
                    .getResultList();
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } finally {
            em.close();
        }
    }

    /** Lấy 1 hóa đơn theo id, kèm items + customer. */
    public Optional<Invoice> findByIdWithDetails(Integer id) {
        EntityManager em = com.utc.warehouse.config.HibernateUtil.getEntityManager();
        try {
            List<Invoice> rows = em.createQuery(
                            "SELECT DISTINCT i FROM Invoice i " +
                            "LEFT JOIN FETCH i.customer c " +
                            "LEFT JOIN FETCH i.items it " +
                            "LEFT JOIN FETCH it.product p " +
                            "WHERE i.id = :id", Invoice.class)
                    .setParameter("id", id)
                    .getResultList();
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } finally {
            em.close();
        }
    }
}