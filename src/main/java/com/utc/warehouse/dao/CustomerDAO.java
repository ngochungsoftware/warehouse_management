package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.Customer;
import jakarta.persistence.EntityManager;

import java.util.List;

public class CustomerDAO extends GenericDAO<Customer, Integer> {

    @Override
    protected Class<Customer> entityClass() { return Customer.class; }

    public List<Customer> findAllOrdered() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("FROM Customer c ORDER BY c.name", Customer.class).getResultList();
        } finally {
            em.close();
        }
    }
}
