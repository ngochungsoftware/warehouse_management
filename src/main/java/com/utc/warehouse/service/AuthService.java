package com.utc.warehouse.service;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.dao.UserDAO;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.Role;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Quản lý đăng nhập, hash mật khẩu bằng BCrypt, lưu user hiện tại vào memory.
 * Có cơ chế bootstrap 2 user mặc định nếu DB rỗng.
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final AuthService INSTANCE = new AuthService();

    private final UserDAO userDAO = new UserDAO();
    private User currentUser;

    private AuthService() { }

    public static AuthService get() { return INSTANCE; }

    public User getCurrentUser() { return currentUser; }

    public boolean isLoggedIn() { return currentUser != null; }

    public boolean isAdmin() { return currentUser != null && currentUser.isAdmin(); }


    public Optional<User> login(String username, String plainPassword) {
        if (username == null || username.isBlank() || plainPassword == null || plainPassword.isEmpty()) {
            return Optional.empty();
        }
        Optional<User> opt = userDAO.findByUsername(username.trim());
        if (opt.isEmpty()) return Optional.empty();
        User u = opt.get();
        if (!Boolean.TRUE.equals(u.getActive())) return Optional.empty();

        try {
            if (BCrypt.checkpw(plainPassword, u.getPasswordHash())) {
                this.currentUser = u;
                log.info("User '{}' logged in (role={})", u.getUsername(), u.getRole());
                AuditLogger.log(u, com.utc.warehouse.enums.AuditAction.LOGIN, "User", u.getId(),
                        u.getUsername(), "Đăng nhập hệ thống");
                return Optional.of(u);
            }
        } catch (IllegalArgumentException ex) {
            log.warn("Password hash invalid for user '{}'", u.getUsername());
        }
        return Optional.empty();
    }

    public void logout() {
        if (currentUser != null) {
            log.info("User '{}' logged out", currentUser.getUsername());
            AuditLogger.log(currentUser, com.utc.warehouse.enums.AuditAction.LOGOUT, "User",
                    currentUser.getId(), currentUser.getUsername(), "Đăng xuất");
        }
        this.currentUser = null;
    }


    public User createUser(String username, String plainPassword, String fullName, Role role) {
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
        User u = new User(username, hash, fullName, role);
        return userDAO.save(u);
    }


    public void changePassword(User user, String newPlainPassword) {
        user.setPasswordHash(BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(10)));
        userDAO.update(user);
    }


    public void bootstrapDefaultUsers() {
        long count = userDAO.count();
        if (count > 0) {
            log.info("Users table not empty (count={}), skip bootstrap.", count);
            return;
        }
        log.warn("Users table empty -> bootstrapping default users (admin/admin123, staff/staff123).");
        try (var em = HibernateUtil.getEntityManager()) {
            var tx = em.getTransaction();
            tx.begin();
            try {
                User admin = new User("admin",
                        BCrypt.hashpw("admin123", BCrypt.gensalt(10)),
                        "Quản trị viên", Role.ADMIN);
                User staff = new User("staff",
                        BCrypt.hashpw("staff123", BCrypt.gensalt(10)),
                        "Nhân viên kho", Role.STAFF);
                em.persist(admin);
                em.persist(staff);
                tx.commit();
                log.info("Default users created.");
            } catch (RuntimeException ex) {
                if (tx.isActive()) tx.rollback();
                throw ex;
            }
        }
    }
}
