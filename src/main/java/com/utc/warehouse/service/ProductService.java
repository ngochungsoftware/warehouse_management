package com.utc.warehouse.service;

import com.utc.warehouse.dao.CategoryDAO;
import com.utc.warehouse.dao.ProductDAO;
import com.utc.warehouse.entity.Category;
import com.utc.warehouse.entity.Product;

import java.util.List;

public class ProductService {

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    public List<Product> all() { return productDAO.findAll(); }

    public List<Product> search(String keyword) { return productDAO.search(keyword); }

    public Product save(Product p) {
        if (p.getSku() == null || p.getSku().isBlank())
            throw new IllegalArgumentException("SKU không được trống");
        if (p.getName() == null || p.getName().isBlank())
            throw new IllegalArgumentException("Tên sản phẩm không được trống");
        if (p.getSalePrice() == null) p.setSalePrice(java.math.BigDecimal.ZERO);
        if (p.getCostPrice() == null) p.setCostPrice(java.math.BigDecimal.ZERO);
        if (p.getVatRate() == null) p.setVatRate(java.math.BigDecimal.ZERO);
        if (p.getActive() == null) p.setActive(Boolean.TRUE);
        Product saved = productDAO.save(p);
        AuditLogger.log(AuthService.get().getCurrentUser(),
                com.utc.warehouse.enums.AuditAction.CREATE, "Product", saved.getId(),
                saved.getSku(), "Thêm sản phẩm " + saved.getSku() + " - " + saved.getName());
        return saved;
    }

    public Product update(Product p) {
        Product updated = productDAO.update(p);
        AuditLogger.log(AuthService.get().getCurrentUser(),
                com.utc.warehouse.enums.AuditAction.UPDATE, "Product", updated.getId(),
                updated.getSku(), "Cập nhật sản phẩm " + updated.getSku());
        return updated;
    }

    public void delete(Integer id) {
        productDAO.findById(id).ifPresent(p ->
                AuditLogger.log(AuthService.get().getCurrentUser(),
                        com.utc.warehouse.enums.AuditAction.DELETE, "Product", p.getId(),
                        p.getSku(), "Xoá sản phẩm " + p.getSku()));
        productDAO.deleteById(id);
    }

    public List<Category> allCategories() { return categoryDAO.findAllOrdered(); }

    public Category saveCategory(Category c) {
        if (c.getName() == null || c.getName().isBlank())
            throw new IllegalArgumentException("Tên danh mục không được trống");
        return categoryDAO.save(c);
    }

    public Category updateCategory(Category c) { return categoryDAO.update(c); }

    public void deleteCategory(Integer id) { categoryDAO.deleteById(id); }
}
