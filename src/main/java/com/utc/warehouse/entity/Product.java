package com.utc.warehouse.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 20)
    private String unit = "cái";

    /** Giá bán lẻ hiện tại. Map vào column `price` cũ (backward-compat). */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice = BigDecimal.ZERO;

    /** Giá vốn bình quân hiện tại (cập nhật tự động khi nhập kho). */
    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    /** Tổng số lượng đã nhập (dùng tính giá vốn bình quân). */
    @Column(name = "total_imported_qty", nullable = false)
    private Integer totalImportedQty = 0;

    /** Tổng tiền vốn đã nhập. */
    @Column(name = "total_imported_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalImportedCost = BigDecimal.ZERO;

    @Column(name = "min_stock", nullable = false)
    private Integer minStock = 0;

    /** % VAT áp dụng cho sản phẩm (0 = không chịu thuế). */
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = BigDecimal.ZERO;

    /** Còn kinh doanh hay ngừng. */
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    /** Đường dẫn tương đối tới file ảnh (lưu trong thư mục app). Null = dùng ảnh mặc định. */
    @Column(name = "image_path", length = 255)
    private String imagePath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Product() { }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public Integer getTotalImportedQty() { return totalImportedQty; }
    public void setTotalImportedQty(Integer totalImportedQty) { this.totalImportedQty = totalImportedQty; }

    public BigDecimal getTotalImportedCost() { return totalImportedCost; }
    public void setTotalImportedCost(BigDecimal totalImportedCost) { this.totalImportedCost = totalImportedCost; }

    public Integer getMinStock() { return minStock; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }

    public BigDecimal getVatRate() { return vatRate; }
    public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Lợi nhuận đơn vị = giá bán - giá vốn. */
    @Transient
    public BigDecimal getProfitPerUnit() {
        if (salePrice == null || costPrice == null) return BigDecimal.ZERO;
        return salePrice.subtract(costPrice);
    }

    /** % lợi nhuận gộp. */
    @Transient
    public BigDecimal getProfitMarginPercent() {
        if (salePrice == null || salePrice.signum() == 0) return BigDecimal.ZERO;
        return getProfitPerUnit()
                .multiply(BigDecimal.valueOf(100))
                .divide(salePrice, 2, java.math.RoundingMode.HALF_UP);
    }
}
