package com.utc.warehouse.dao;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.entity.AuditLog;
import com.utc.warehouse.enums.AuditAction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuditLogDAO extends GenericDAO<AuditLog, Integer> {

    @Override
    protected Class<AuditLog> entityClass() { return AuditLog.class; }

    /**
     * Lọc log theo nhiều tiêu chí. Tất cả tham số đều nullable.
     * - userId: lọc theo user
     * - action: lọc theo hành động
     * - entityType: lọc theo loại đối tượng
     * - keyword: tìm trong entityCode hoặc details
     * - fromDate, toDate: khoảng thời gian (so sánh với created_at)
     */
    public List<AuditLog> search(Integer userId, com.utc.warehouse.enums.AuditAction action,
                                 String entityType, String keyword,
                                 LocalDate fromDate, LocalDate toDate,
                                 int limit) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<AuditLog> cq = cb.createQuery(AuditLog.class);
            Root<AuditLog> r = cq.from(AuditLog.class);
            List<Predicate> ps = new ArrayList<>();
            if (userId != null)        ps.add(cb.equal(r.get("user").get("id"), userId));
            if (action != null)        ps.add(cb.equal(r.get("action"), action));
            if (entityType != null && !entityType.isBlank())
                                       ps.add(cb.equal(r.get("entityType"), entityType));
            if (keyword != null && !keyword.isBlank()) {
                String k = "%" + keyword.toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(r.get("entityCode")), k),
                        cb.like(cb.lower(r.get("details")), k),
                        cb.like(cb.lower(r.get("username")), k)
                ));
            }
            if (fromDate != null)       ps.add(cb.greaterThanOrEqualTo(r.get("createdAt"),
                                                fromDate.atStartOfDay()));
            if (toDate != null)         ps.add(cb.lessThanOrEqualTo(r.get("createdAt"),
                                                toDate.atTime(23, 59, 59)));
            cq.where(ps.toArray(new Predicate[0]));
            cq.orderBy(cb.desc(r.get("createdAt")));
            return em.createQuery(cq).setMaxResults(limit).getResultList();
        } finally {
            em.close();
        }
    }

    public Optional<AuditLog> findById(Integer id) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(AuditLog.class, id));
        } finally {
            em.close();
        }
    }
}
