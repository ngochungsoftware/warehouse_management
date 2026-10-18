package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.Product;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class ProductDAO extends GenericDAO<Product, Integer> {

    @Override
    protected Class<Product> entityClass() { return Product.class; }

    public Optional<Product> findBySku(String sku) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("FROM Product p WHERE p.sku = :s", Product.class)
                    .setParameter("s", sku)
                    .getResultStream().findFirst();
        } finally {
            em.close();
        }
    }

    public List<Product> search(String keyword) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            String kw = "%" + (keyword == null ? "" : keyword.toLowerCase()) + "%";
            return em.createQuery(
                    "FROM Product p WHERE LOWER(p.sku) LIKE :kw OR LOWER(p.name) LIKE :kw ORDER BY p.name",
                    Product.class)
                    .setParameter("kw", kw)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
