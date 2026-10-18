package com.utc.warehouse.entity;

import com.utc.warehouse.enums.PaymentMethod;
import com.utc.warehouse.enums.PaymentType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phiếu thu/chi - theo dõi dòng tiền vào/ra.
 * RECEIPT = phiếu thu (KH trả tiền hóa đơn / thu khác)
 * PAYMENT = phiếu chi (trả NCC, chi phí)
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_pay_code",    columnList = "code", unique = true),
        @Index(name = "idx_pay_type",    columnList = "type"),
        @Index(name = "idx_pay_invoice", columnList = "invoice_id"),
        @Index(name = "idx_pay_created", columnList = "created_at")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Mã phiếu: PT00001 (thu) / PC00001 (chi). */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PaymentType type;

    @Column(name = "partner_id")
    private Integer partnerId;

    @Column(name = "partner_type", length = 10)
    private String partnerType;  // SUPPLIER / CUSTOMER

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 15)
    private PaymentMethod method = PaymentMethod.CASH;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Payment() { }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public PaymentType getType() { return type; }
    public void setType(PaymentType type) { this.type = type; }

    public Integer getPartnerId() { return partnerId; }
    public void setPartnerId(Integer partnerId) { this.partnerId = partnerId; }

    public String getPartnerType() { return partnerType; }
    public void setPartnerType(String partnerType) { this.partnerType = partnerType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
