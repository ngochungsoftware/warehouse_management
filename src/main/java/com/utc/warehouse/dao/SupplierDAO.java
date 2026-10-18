package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.Supplier;
import jakarta.persistence.EntityManager;

import java.util.List;

public class SupplierDAO extends GenericDAO<Supplier, Integer> {

    @Override
    protected Class<Supplier> entityClass() { return Supplier.class; }

    public List<Supplier> findAllOrdered() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("FROM Supplier s ORDER BY s.name", Supplier.class).getResultList();
        } finally {
            em.close();
        }
    }
}
