package com.utc.warehouse;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.dao.FinanceReportDAO;
import com.utc.warehouse.entity.*;
import com.utc.warehouse.enums.InvoiceStatus;
import com.utc.warehouse.enums.TransactionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * BizTest11: Seed dữ liệu mẫu vào DB để test chart Dashboard.
 *  - Tạo 5 danh mục + 15 sản phẩm
 *  - Tạo phiếu nhập (IMPORT) cho mỗi SP trong 7 ngày qua
 *  - Tạo hóa đơn bán (INVOICE) cho 1 số SP trong 7 ngày qua
 *
 *  Chạy 1 lần rồi xóa thủ công nếu cần (TRUNCATE table).
 */
public class BizTest11 {

    public static void main(String[] args) throws Exception {
        EntityManager em = HibernateUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        Random rnd = new Random(42);

        try {
            tx.begin();

            // Lấy user admin để gán vào transaction
            @SuppressWarnings("unchecked")
            List<User> users = em.createQuery("FROM User").getResultList();
            if (users.isEmpty()) {
                System.out.println("[seed] Không có user nào trong DB. Bỏ qua seed.");
                tx.rollback();
                return;
            }
            User admin = users.get(0);

            // === Danh mục ===
            String[] catNames = {"Lương thực", "Nước uống", "Chăm sóc", "Bánh kẹo", "Gia vị"};
            @SuppressWarnings("unchecked")
            List<Category> existing = em.createQuery("FROM Category").getResultList();
            Category[] cats = new Category[5];
            for (int i = 0; i < 5; i++) {
                final String name = catNames[i];
                Category c = existing.stream().filter(x -> name.equals(x.getName())).findFirst().orElse(null);
                if (c == null) {
                    c = new Category();
                    c.setName(name);
                    c.setDescription("Danh mục " + name);
                    em.persist(c);
                }
                cats[i] = c;
            }

            // === Sản phẩm ===
            String[][] prods = {
                    {"Gạo ST25 5kg", "Lương thực"}, {"Mì Hảo Hảo", "Lương thực"},
                    {"Bánh mì sandwich", "Lương thực"},
                    {"Nước suối 1.5L", "Nước uống"}, {"Coca Cola 330ml", "Nước uống"},
                    {"Trà xanh 0 độ", "Nước uống"},
                    {"Dầu gội Clear", "Chăm sóc"}, {"Sữa tắm Lifebuoy", "Chăm sóc"},
                    {"Bàn chải đánh răng", "Chăm sóc"},
                    {"Bánh Oreo", "Bánh kẹo"}, {"Snack Oishi", "Bánh kẹo"},
                    {"Kẹo Alpenliebe", "Bánh kẹo"},
                    {"Nước mắm Chinsu", "Gia vị"}, {"Hạt nêm Knorr", "Gia vị"},
                    {"Đường cát trắng", "Gia vị"}
            };
            @SuppressWarnings("unchecked")
            List<Product> existingP = em.createQuery("FROM Product").getResultList();
            Product[] products = new Product[prods.length];
            for (int i = 0; i < prods.length; i++) {
                final String pname = prods[i][0];
                Product p = existingP.stream().filter(x -> pname.equals(x.getName())).findFirst().orElse(null);
                if (p == null) {
                    p = new Product();
                    p.setSku("SKU" + String.format("%03d", i + 1));
                    p.setName(pname);
                    p.setCategory(cats[findIdx(catNames, prods[i][1])]);
                    p.setUnit("cái");
                    p.setCostPrice(BigDecimal.valueOf(10000 + rnd.nextInt(50000)));
                    p.setSalePrice(BigDecimal.valueOf(15000 + rnd.nextInt(80000)));
                    p.setVatRate(BigDecimal.valueOf(8));
                    p.setMinStock(20);
                    p.setActive(true);
                    em.persist(p);
                }
                products[i] = p;
            }

            // === Phiếu nhập kho: 1 phiếu/SP trong 7 ngày qua ===
            @SuppressWarnings("unchecked")
            List<StockTransaction> existingT = em.createQuery("FROM StockTransaction").getResultList();
            if (existingT.isEmpty()) {
                for (int i = 0; i < products.length; i++) {
                    Product p = products[i];
                    StockTransaction t = new StockTransaction();
                    t.setType(TransactionType.IMPORT);
                    t.setNote("Nhập kho ban đầu cho " + p.getName());
                    t.setUser(admin);
                    t.setCreatedAt(java.time.LocalDateTime.now()
                            .minusDays(rnd.nextInt(7)));
                    em.persist(t);

                    StockTransactionDetail d = new StockTransactionDetail();
                    d.setProduct(p);
                    d.setQuantity(50 + rnd.nextInt(200));
                    d.setUnitPrice(p.getCostPrice());
                    t.addDetail(d); // helper set transaction + add to list
                }
            }

            // === Hóa đơn bán: 3-7 ngày qua ===
            @SuppressWarnings("unchecked")
            List<Invoice> existingI = em.createQuery("FROM Invoice").getResultList();
            if (existingI.isEmpty()) {
                for (int day = 0; day <= 6; day++) {
                    LocalDate d = LocalDate.now().minusDays(day);
                    int nInvoices = 1 + rnd.nextInt(3);
                    for (int k = 0; k < nInvoices; k++) {
                        Invoice inv = new Invoice();
                        inv.setCode("HD" + System.currentTimeMillis() + "_" + k);
                        inv.setIssueDate(d);
                        inv.setStatus(InvoiceStatus.ISSUED);
                        inv.setUser(admin);
                        inv.setSubtotal(BigDecimal.ZERO);
                        inv.setDiscount(BigDecimal.ZERO);
                        inv.setVatAmount(BigDecimal.ZERO);
                        inv.setTotal(BigDecimal.ZERO);
                        inv.setPaid(BigDecimal.ZERO);
                        inv.setDebt(BigDecimal.ZERO);
                        em.persist(inv);

                        int nItems = 1 + rnd.nextInt(3);
                        BigDecimal sub = BigDecimal.ZERO;
                        for (int m = 0; m < nItems; m++) {
                            Product p = products[rnd.nextInt(products.length)];
                            int qty = 1 + rnd.nextInt(5);
                            InvoiceItem it = new InvoiceItem();
                            it.setInvoice(inv);
                            it.setProduct(p);
                            it.setQuantity(qty);
                            it.setUnitPrice(p.getSalePrice());
                            it.setCostPriceSnapshot(p.getCostPrice());
                            it.setDiscount(BigDecimal.ZERO);
                            inv.addItem(it); // helper
                            sub = sub.add(p.getSalePrice().multiply(BigDecimal.valueOf(qty)));
                        }
                        BigDecimal vat = sub.multiply(BigDecimal.valueOf(8)).divide(BigDecimal.valueOf(100));
                        BigDecimal total = sub.add(vat);
                        inv.setSubtotal(sub);
                        inv.setVatAmount(vat);
                        inv.setTotal(total);
                        // Một số hóa đơn nợ một phần
                        if (rnd.nextBoolean()) {
                            inv.setPaid(total);
                            inv.setDebt(BigDecimal.ZERO);
                        } else {
                            BigDecimal paid = total.multiply(BigDecimal.valueOf(50 + rnd.nextInt(50)))
                                    .divide(BigDecimal.valueOf(100));
                            inv.setPaid(paid);
                            inv.setDebt(total.subtract(paid));
                        }
                    }
                }
            }

            tx.commit();
            System.out.println("[seed] OK — tạo " + cats.length + " DM, "
                    + products.length + " SP, nhiều phiếu nhập + hóa đơn");
        } catch (Exception ex) {
            if (tx.isActive()) tx.rollback();
            ex.printStackTrace();
            throw ex;
        } finally {
            em.close();
        }

        // Bây giờ test chart
        System.out.println("\n=== Test chart với data vừa seed ===");
        com.utc.warehouse.service.FinanceReportService svc = new com.utc.warehouse.service.FinanceReportService();

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);
        java.util.List<FinanceReportDAO.RevenueRow> rows = svc.revenueByDate(from, to);
        System.out.println("Revenue " + from + " → " + to + ": " + rows.size() + " ngày");
        for (FinanceReportDAO.RevenueRow r : rows) {
            System.out.println("  " + r.getDate() + ": " + r.getNetRevenue() + " đ ("
                    + r.getInvoiceCount() + " hóa đơn)");
        }

        java.util.List<FinanceReportDAO.CategoryStockRow> catRows = svc.inventoryByCategory();
        System.out.println("\nTồn kho theo danh mục: " + catRows.size() + " loại");
        for (FinanceReportDAO.CategoryStockRow r : catRows) {
            System.out.println("  " + r.getCategoryName() + ": " + r.getQuantity() + " đơn vị");
        }

        FinanceReportDAO.DashboardSummary s = svc.dashboardSummary();
        System.out.println("\nDashboard summary:");
        System.out.println("  Hôm nay: doanh thu = " + s.todayRevenue + ", lợi nhuận = " + s.todayProfit);
        System.out.println("  Tháng:   doanh thu = " + s.monthRevenue + ", lợi nhuận = " + s.monthProfit
                + ", " + s.monthInvoiceCount + " hóa đơn");
        System.out.println("  Kho:     tồn giá vốn = " + s.inventoryValue + ", "
                + s.productCount + " SP, " + s.lowStockCount + " SP sắp hết");
        System.out.println("  Công nợ: " + s.totalReceivable);
    }

    private static int findIdx(String[] arr, String v) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return 0;
    }
}
