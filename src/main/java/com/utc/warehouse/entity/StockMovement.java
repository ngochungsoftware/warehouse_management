package com.utc.warehouse.entity;

import com.utc.warehouse.enums.MovementType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Sổ cái tồn kho - ghi lại mọi biến động tăng/giảm tồn.
 * Giúp audit trail + tính tồn nhanh hơn (lấy dòng cuối).
 */
@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_mv_product", columnList = "product_id"),
        @Index(name = "idx_mv_ref_tx",   columnList = "ref_tx_id"),
        @Index(name = "idx_mv_created",  columnList = "created_at")
})
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MovementType type;

    /** Lượng thay đổi tồn: dương cho nhập, âm cho xuất. */
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    /** Tồn kho sau khi ghi nhận biến động. */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    /** Tham chiếu phiếu gốc (nếu có). */
    @Column(name = "ref_tx_id")
    private Integer refTransactionId;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public StockMovement() { }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }

    public Integer getQuantityChange() { return quantityChange; }
    public void setQuantityChange(Integer quantityChange) { this.quantityChange = quantityChange; }

    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }

    public Integer getRefTransactionId() { return refTransactionId; }
    public void setRefTransactionId(Integer refTransactionId) { this.refTransactionId = refTransactionId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
