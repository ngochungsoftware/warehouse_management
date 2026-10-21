package com.utc.warehouse.entity;

import com.utc.warehouse.enums.InvoiceStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hóa đơn bán hàng. Tách biệt với phiếu xuất kho:
 *  - Phiếu xuất kho: ghi nhận biến động tồn kho.
 *  - Hóa đơn: tài liệu kế toán, có VAT, công nợ, theo dõi thanh toán.
 * Một phiếu xuất có thể đi kèm 1 hóa đơn (mặc định 1-1).
 */
@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_inv_code",     columnList = "code", unique = true),
        @Index(name = "idx_inv_customer", columnList = "customer_id"),
        @Index(name = "idx_inv_status",   columnList = "status"),
        @Index(name = "idx_inv_issue",    columnList = "issue_date")
})
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Mã hóa đơn, vd HD2026-0001. */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /** Tham chiếu phiếu xuất kho (stock_transactions). */
    @Column(name = "tx_id")
    private Integer transactionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "vat_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal paid = BigDecimal.ZERO;

    @Column(name = "debt", precision = 15, scale = 2, nullable = false)
    private BigDecimal debt = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate = LocalDate.now();

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<InvoiceItem> items = new ArrayList<>();

    public Invoice() { }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getVatAmount() { return vatAmount; }
    public void setVatAmount(BigDecimal vatAmount) { this.vatAmount = vatAmount; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal getPaid() { return paid; }
    public void setPaid(BigDecimal paid) { this.paid = paid; }

    public BigDecimal getDebt() { return debt; }
    public void setDebt(BigDecimal debt) { this.debt = debt; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }

    public void addItem(InvoiceItem it) {
        it.setInvoice(this);
        this.items.add(it);
    }

    /** Cập nhật trạng thái dựa trên paid. */
    public void recomputeStatus() {
        if (status == InvoiceStatus.CANCELLED || status == InvoiceStatus.DRAFT) return;
        int cmp = paid.compareTo(total);
        if (cmp >= 0)       status = InvoiceStatus.PAID;    // đã thu đủ
        // paid > 0 nhưng < total => thu một phần (cmp == -1 vẫn là đã thu)
        else if (paid.signum() > 0) status = InvoiceStatus.PARTIAL;
        else                        status = InvoiceStatus.ISSUED;   // chưa thu gì
    }
}
