package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.Category;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class CategoryDAO extends GenericDAO<Category, Integer> {

    @Override
    protected Class<Category> entityClass() { return Category.class; }

    public Optional<Category> findByName(String name) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("FROM Category c WHERE c.name = :n", Category.class)
                    .setParameter("n", name)
                    .getResultStream().findFirst();
        } finally {
            em.close();
        }
    }

    public List<Category> findAllOrdered() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("FROM Category c ORDER BY c.name", Category.class).getResultList();
        } finally {
            em.close();
        }
    }
}
