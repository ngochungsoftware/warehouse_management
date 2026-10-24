package com.utc.warehouse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;

/**
 * VerifyTest38 — Đối chiếu số liệu thật trong DB bằng native SQL.
 *
 * <p>Xác nhận giả thuyết: các bảng có dữ liệu thật, nhưng countReferences()
 * trả 0 vì dùng sai tên trong JPQL.
 */
public class VerifyTest38 {

    public static void main(String[] args) {
        System.out.println("=== SO LIEU THAT TRONG DB (native SQL) ===\n");

        EntityManager em = com.utc.warehouse.config.HibernateUtil.getEntityManager();
        try {
            // Đếm theo từng user, dùng native SQL nên không bị vướng JPQL.
            List<Object[]> rows = em.createNativeQuery(
                    "SELECT u.id, u.username, "
                  + " (SELECT COUNT(*) FROM audit_logs a WHERE a.user_id = u.id) AS n_audit, "
                  + " (SELECT COUNT(*) FROM stock_transactions t WHERE t.user_id = u.id) AS n_tx, "
                  + " (SELECT COUNT(*) FROM invoices i WHERE i.user_id = u.id) AS n_inv "
                  + "FROM users u ORDER BY u.id").getResultList();

            System.out.printf("  %-4s %-14s %-8s %-6s %-6s%n", "id", "username", "audit", "tx", "inv");
            for (Object[] r : rows) {
                System.out.printf("  %-4s %-14s %-8s %-6s %-6s%n",
                        r[0], r[1], r[2], r[3], r[4]);
            }
            System.out.println("\n  -> So sanh voi VerifyTest37: countReferences(admin) = 0");
            System.out.println("  -> Bang native SQL co so lieu => countReferences() SAI.");
        } finally {
            em.close();
        }
    }
}