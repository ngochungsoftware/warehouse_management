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

    /**
     * Lấy danh sách nhân viên (role = STAFF), sắp xếp: hoạt động trước, rồi theo
     * username.
     *
     * <p>Dùng cho màn "Quản lý nhân viên" — chỉ quan tâm tài khoản nhân viên,
     * không lẫn tài khoản quản trị.
     */
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

    /**
     * Đếm tổng số bản ghi ở các bảng con đang trỏ tới user này.
     *
     * <p>Trước khi hard delete, các bảng này sẽ chặn xoá do foreign key
     * ({@code fk_tx_user} không có {@code ON DELETE}). Đếm trước giúp UI hiển thị
     * lý do cụ thể thay vì lỗi SQL kèm mã lỗi khó hiểu.
     *
     * <p>Dùng <b>native SQL</b> chứ không dùng JPQL: JPQL phải gọi theo <b>tên
     * entity</b> ({@code AuditLog}, {@code StockTransaction}...), không phải
     * tên bảng ({@code audit_logs}, {@code stock_transactions}...). Dùng tên bảng
     * sẽ ném {@code QuerySyntaxException}, và nếu nuốt lỗi thì hàm luôn trả 0 —
     * tức UI không bao giờ cảnh báo "có dữ liệu liên quan" và sẽ cố xoá hẳn rồi
     * mới gặp lỗi SQL thô từ MySQL.
     *
     * <p>Chỉ đếm những bảng thực sự tồn tại trong DB (tra {@code information_schema}),
     * nên DB cũ chưa migrate không làm hỏng hàm.
     */
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

    /** Đếm trong 1 bảng; trả 0 nếu bảng không tồn tại. */
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
            // Bảng thiếu / không có cột user_id — bỏ qua, không chặn thao tác.
            return 0;
        }
    }
}
