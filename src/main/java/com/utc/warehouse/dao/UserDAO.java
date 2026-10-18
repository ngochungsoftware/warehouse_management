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
}
