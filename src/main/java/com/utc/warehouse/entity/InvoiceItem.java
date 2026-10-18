package com.utc.warehouse.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    /** Giá vốn snapshot tại thời điểm bán (để tính lợi nhuận). */
    @Column(name = "cost_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPriceSnapshot = BigDecimal.ZERO;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "line_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public InvoiceItem() { }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getCostPriceSnapshot() { return costPriceSnapshot; }
    public void setCostPriceSnapshot(BigDecimal costPriceSnapshot) { this.costPriceSnapshot = costPriceSnapshot; }

    public BigDecimal getVatRate() { return vatRate; }
    public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    /** Lợi nhuận dòng = (đơn giá - giá vốn) * SL - discount. */
    @Transient
    public BigDecimal getLineProfit() {
        if (unitPrice == null || costPriceSnapshot == null || quantity == null) return BigDecimal.ZERO;
        BigDecimal profit = unitPrice.subtract(costPriceSnapshot).multiply(BigDecimal.valueOf(quantity));
        if (discount != null) profit = profit.subtract(discount);
        return profit;
    }
}
