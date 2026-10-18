package com.utc.warehouse.entity;

import com.utc.warehouse.enums.AuditAction;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Lịch sử thao tác của user. Ghi lại ai làm gì, với đối tượng nào, lúc nào.
 * Dùng cho kế toán kiểm tra + truy vết sự cố.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user",      columnList = "user_id"),
        @Index(name = "idx_audit_action",    columnList = "action"),
        @Index(name = "idx_audit_entity",    columnList = "entity_type,entity_id"),
        @Index(name = "idx_audit_created",   columnList = "created_at")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 50)
    private String username;     // snapshot để truy vết khi user bị xoá

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private AuditAction action;

    /** Loại đối tượng bị tác động: Product, Invoice, PO, ... */
    @Column(name = "entity_type", length = 30)
    private String entityType;

    /** ID đối tượng bị tác động. */
    @Column(name = "entity_id")
    private Integer entityId;

    /** Mã hiển thị (vd SP0001, HD2026-0001) để tra cứu nhanh. */
    @Column(name = "entity_code", length = 50)
    private String entityCode;

    /** Mô tả ngắn (vd "Thêm 10 sp ABC", "Sửa giá từ 100->120"). */
    @Column(length = 500)
    private String details;

    /** IP client (nếu có). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuditLog() { }

    /** Builder tiện dụng. */
    public static AuditLog of(User user, AuditAction action, String entityType,
                              Integer entityId, String entityCode, String details) {
        AuditLog a = new AuditLog();
        a.setUser(user);
        a.setUsername(user == null ? "system" : (user.getUsername() == null ? "?" : user.getUsername()));
        a.setAction(action);
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setEntityCode(entityCode);
        a.setDetails(details);
        return a;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Integer getEntityId() { return entityId; }
    public void setEntityId(Integer entityId) { this.entityId = entityId; }

    public String getEntityCode() { return entityCode; }
    public void setEntityCode(String entityCode) { this.entityCode = entityCode; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
