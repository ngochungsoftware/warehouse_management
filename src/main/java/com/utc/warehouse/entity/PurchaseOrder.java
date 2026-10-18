package com.utc.warehouse.entity;

import com.utc.warehouse.enums.POStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Đơn đặt hàng từ nhà cung cấp.
// * Workflow: DRAFT (tạo) → ORDERED (gửi NCC) → PARTIAL/RECEIVED (nhận hàng) → CANCELLED.
 * Khi "nhận hàng" sẽ tự tạo phiếu nhập kho (stock_transaction IMPORT) từ các item.
 */
@Entity
@Table(name = "purchase_orders", indexes = {
        @Index(name = "idx_po_code",      columnList = "code", unique = true),
        @Index(name = "idx_po_supplier",  columnList = "supplier_id"),
        @Index(name = "idx_po_status",    columnList = "status"),
        @Index(name = "idx_po_created",   columnList = "created_at")
})
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Mã đơn, vd PO2026-0001. */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private POStatus status = POStatus.DRAFT;

    /** Ngày dự kiến NCC giao. */
    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "total", nullable = false, precision = 15, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 500)
    private String note;

    /** Tham chiếu phiếu nhập kho khi nhận hàng (nhiều-nếu partial). */
    @Column(name = "received_tx_id")
    private Integer receivedTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    public PurchaseOrder() { }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public POStatus getStatus() { return status; }
    public void setStatus(POStatus status) { this.status = status; }

    public LocalDate getExpectedDate() { return expectedDate; }
    public void setExpectedDate(LocalDate expectedDate) { this.expectedDate = expectedDate; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Integer getReceivedTransactionId() { return receivedTransactionId; }
    public void setReceivedTransactionId(Integer receivedTransactionId) { this.receivedTransactionId = receivedTransactionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<PurchaseOrderItem> getItems() { return items; }
    public void setItems(List<PurchaseOrderItem> items) { this.items = items; }

    public void addItem(PurchaseOrderItem it) {
        it.setPurchaseOrder(this);
        this.items.add(it);
    }

    public void recomputeTotal() {
        BigDecimal t = BigDecimal.ZERO;
        for (PurchaseOrderItem it : items) {
            t = t.add(it.getLineTotal());
        }
        this.total = t;
    }
}
