package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.User;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class UserDAO extends GenericDAO<User, Integer> {

    @Override
    protected Class<User> entityClass() { return User.class; }

    public Optional<User> findByUsername(String username) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("FROM User u WHERE u.username = :u", User.class)
                    .setParameter("u", username)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    public List<User> findAllActive() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("FROM User u WHERE u.active = true ORDER BY u.username", User.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<User> findAllStaff() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                            "FROM User u WHERE u.role = com.utc.warehouse.enums.Role.STAFF "
                            + "ORDER BY u.active DESC, u.username", User.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long countReferences(Integer userId) {
        EntityManager em = HibernateUtil.getEntityManager();
        String[] tables = {
                "stock_transactions", "invoices", "payments",
                "purchase_orders", "audit_logs"
        };
        long total = 0;
        try {
            for (String t : tables) {
                total += countTable(em, t, userId);
            }
            return total;
        } finally {
            em.close();
        }
    }

    private long countTable(EntityManager em, String table, Integer userId) {
        try {
            Object exists = em.createNativeQuery(
                            "SELECT COUNT(*) FROM information_schema.tables "
                          + "WHERE table_schema = DATABASE() AND table_name = :t")
                    .setParameter("t", table)
                    .getSingleResult();
            if (((Number) exists).longValue() == 0) return 0;

            return ((Number) em.createNativeQuery(
                                "SELECT COUNT(*) FROM " + table + " WHERE user_id = :uid")
                        .setParameter("uid", userId)
                        .getSingleResult()).longValue();
        } catch (Exception ex) {
            return 0;
        }
    }
}
