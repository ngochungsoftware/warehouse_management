package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * CRUD generic dùng JPA EntityManager.
 * <p>DAO cụ thể extend class này và truyền kiểu entity.
 * Mỗi thao tác ghi (save/update/delete) chạy trong transaction riêng.
 */
public abstract class GenericDAO<T, ID> {

    private static final Logger log = LoggerFactory.getLogger(GenericDAO.class);

    protected abstract Class<T> entityClass();

    // --- Read --------------------------------------------------------------

    public Optional<T> findById(ID id) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(entityClass(), id));
        } finally {
            em.close();
        }
    }

    public List<T> findAll() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("FROM " + entityClass().getSimpleName(), entityClass())
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public long count() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(e) FROM " + entityClass().getSimpleName() + " e", Long.class)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    // --- Write -------------------------------------------------------------

    public T save(T entity) {
        return inTransaction(em -> {
            em.persist(entity);
            return entity;
        });
    }

    public T update(T entity) {
        return inTransaction(em -> em.merge(entity));
    }

    public void deleteById(ID id) {
        inTransaction(em -> {
            T e = em.find(entityClass(), id);
            if (e != null) em.remove(e);
            return null;
        });
    }

    public void delete(T entity) {
        inTransaction(em -> {
            em.remove(em.contains(entity) ? entity : em.merge(entity));
            return null;
        });
    }

    // --- Helpers -----------------------------------------------------------

    /**
     * Chạy hàm trong 1 transaction, commit nếu OK, rollback nếu exception.
     */
    protected <R> R inTransaction(Function<EntityManager, R> work) {
        EntityManager em = HibernateUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            R result = work.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            log.error("Transaction rolled back", ex);
            throw ex;
        } finally {
            em.close();
        }
    }

    /**
     * Variant cho thao tác không cần giá trị trả về.
     */
    protected void inTransactionVoid(Consumer<EntityManager> work) {
        inTransaction(em -> { work.accept(em); return null; });
    }
}
