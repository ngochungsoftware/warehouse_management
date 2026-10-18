package com.utc.warehouse.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "ordered_qty", nullable = false)
    private Integer orderedQty;

    /** Số lượng thực nhận (mặc định = ordered qty, có thể nhỏ hơn nếu partial). */
    @Column(name = "received_qty", nullable = false)
    private Integer receivedQty = 0;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public PurchaseOrderItem() { }

    public PurchaseOrderItem(Product product, Integer orderedQty, BigDecimal unitPrice) {
        this.product = product;
        this.orderedQty = orderedQty;
        this.receivedQty = 0;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(orderedQty));
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getOrderedQty() { return orderedQty; }
    public void setOrderedQty(Integer orderedQty) {
        this.orderedQty = orderedQty;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(orderedQty));
    }

    public Integer getReceivedQty() { return receivedQty; }
    public void setReceivedQty(Integer receivedQty) { this.receivedQty = receivedQty; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(orderedQty == null ? 0 : orderedQty));
    }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    @Transient
    public boolean isFullyReceived() { return receivedQty != null && orderedQty != null && receivedQty >= orderedQty; }
}
