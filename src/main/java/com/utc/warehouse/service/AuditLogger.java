package com.utc.warehouse.service;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.dao.AuditLogDAO;
import com.utc.warehouse.entity.AuditLog;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.AuditAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * Ghi log thao tác của user.
 *
 * <p>Dùng {@link #log} (ghi ngoài transaction) cho sự kiện đơn giản,
 * hoặc {@link #logInTx} (ghi trong transaction đang mở) cho sự kiện cần rollback cùng entity.
 */
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    private static final AuditLogDAO dao = new AuditLogDAO();

    public static void log(User user, AuditAction action, String entityType,
                           Integer entityId, String entityCode, String details) {
        try {
            AuditLog a = AuditLog.of(user, action, entityType, entityId, entityCode, details);
            dao.save(a);
        } catch (Exception ex) {
            log.warn("Audit log failed (action={} type={} id={}): {}",
                    action, entityType, entityId, ex.getMessage());
        }
    }

    public static void logInTx(jakarta.persistence.EntityManager em, User user,
                                AuditAction action, String entityType,
                                Integer entityId, String entityCode, String details) {
        try {
            AuditLog a = AuditLog.of(user, action, entityType, entityId, entityCode, details);
            if (user != null && user.getId() != null) a.setUser(em.getReference(User.class, user.getId()));
            a.setCreatedAt(java.time.LocalDateTime.now());
            em.persist(a);
        } catch (Exception ex) {
            log.warn("Audit log (in-tx) failed: {}", ex.getMessage());
        }
    }

    public List<AuditLog> search(Integer userId, AuditAction action, String entityType,
                                 String keyword, LocalDate from, LocalDate to, int limit) {
        return dao.search(userId, action, entityType, keyword, from, to, limit);
    }
}
