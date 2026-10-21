package com.utc.warehouse.service;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.dao.ProductDAO;
import com.utc.warehouse.dao.ReportDAO;
import com.utc.warehouse.dao.StockTransactionDAO;
import com.utc.warehouse.entity.Product;
import com.utc.warehouse.entity.StockMovement;
import com.utc.warehouse.entity.StockTransaction;
import com.utc.warehouse.entity.StockTransactionDetail;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.MovementType;
import com.utc.warehouse.enums.PartnerType;
import com.utc.warehouse.enums.TransactionType;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final StockTransactionDAO txDAO = new StockTransactionDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final ProductDAO productDAO = new ProductDAO();

    public List<StockTransaction> all() { return txDAO.findAllOrdered(); }

    public List<StockTransaction> findByType(TransactionType type) { return txDAO.findByType(type); }

    public List<StockTransaction> findInRange(LocalDateTime from, LocalDateTime to, TransactionType type) {
        return txDAO.findInRange(from, to, type);
    }

    /**
     * Tạo phiếu xuất trong 1 transaction đang mở (dùng nội bộ cho InvoiceService).
     * KHÔNG đóng em, KHÔNG commit. Caller quản lý transaction.
     */
    public StockTransaction createExportInTx(EntityManager em, User user, Integer customerId, String note,
                                             List<StockTransactionDetail> details) {
        if (details == null || details.isEmpty())
            throw new IllegalArgumentException("Phiếu xuất phải có ít nhất 1 sản phẩm");
        for (StockTransactionDetail d : details) {
            int onHand = reportDAO.getOnHand(d.getProduct().getId());
            if (d.getQuantity() == null || d.getQuantity() <= 0)
                throw new IllegalArgumentException("Số lượng xuất phải > 0");
            if (d.getQuantity() > onHand) {
                throw new IllegalStateException(String.format(
                        "Không đủ tồn cho sản phẩm ID=%d%s (tồn=%d, yêu cầu xuất=%d)",
                        d.getProduct().getId(),
                        productLabel(d), onHand, d.getQuantity()));
            }
        }
        return saveTransactionInTx(em, user, TransactionType.EXPORT, PartnerType.CUSTOMER,
                customerId, note, details);
    }

    StockTransaction saveTransactionInTx(EntityManager em, User user, TransactionType type,
                                                  PartnerType partnerType, Integer partnerId, String note,
                                                  List<StockTransactionDetail> details) {
        StockTransaction t = new StockTransaction();
        t.setType(type);
        t.setPartnerType(partnerType);
        t.setPartnerId(partnerId);
        t.setNote(note);
        t.setUser(em.getReference(User.class, user.getId()));
        t.setCreatedAt(LocalDateTime.now());

        java.util.Map<Integer, BigDecimal> oldCost = new java.util.HashMap<>();
        java.util.Map<Integer, Integer> oldQty  = new java.util.HashMap<>();

        for (StockTransactionDetail d : details) {
            if (d.getQuantity() == null || d.getQuantity() <= 0)
                throw new IllegalArgumentException("Số lượng phải > 0");
            if (d.getUnitPrice() == null) d.setUnitPrice(BigDecimal.ZERO);
            Integer pid = d.getProduct().getId();
            Product p = em.find(Product.class, pid);
            if (p == null) throw new IllegalArgumentException("Sản phẩm ID=" + pid + " không tồn tại");
            oldCost.putIfAbsent(pid, p.getCostPrice() == null ? BigDecimal.ZERO : p.getCostPrice());
            oldQty.putIfAbsent(pid, p.getTotalImportedQty() == null ? 0 : p.getTotalImportedQty());
            d.setProduct(p);
            t.addDetail(d);
        }
        em.persist(t);
        em.flush();

        for (StockTransactionDetail d : t.getDetails()) {
            Product p = d.getProduct();
            Integer pid = p.getId();
            if (type == TransactionType.IMPORT) {
                BigDecimal oldC = oldCost.get(pid);
                int oldQ = oldQty.get(pid);
                int newQ = d.getQuantity();
                BigDecimal newC = d.getUnitPrice();
                BigDecimal newCost = (oldQ + newQ == 0) ? newC
                        : oldC.multiply(BigDecimal.valueOf(oldQ))
                              .add(newC.multiply(BigDecimal.valueOf(newQ)))
                              .divide(BigDecimal.valueOf(oldQ + newQ), 2, RoundingMode.HALF_UP);
                p.setCostPrice(newCost);
                p.setTotalImportedQty(oldQ + newQ);
                p.setTotalImportedCost(
                        (p.getTotalImportedCost() == null ? BigDecimal.ZERO : p.getTotalImportedCost())
                                .add(d.getUnitPrice().multiply(BigDecimal.valueOf(newQ)))
                );
                em.merge(p);
                recordMovement(em, p, MovementType.IMPORT, d.getQuantity(), pid, t.getId());
            } else {
                recordMovement(em, p, MovementType.EXPORT, -d.getQuantity(), pid, t.getId());
            }
        }
        AuditLogger.logInTx(em, user,
                type == TransactionType.IMPORT
                        ? com.utc.warehouse.enums.AuditAction.IMPORT
                        : com.utc.warehouse.enums.AuditAction.EXPORT,
                "StockTransaction", t.getId(), "#" + t.getId(),
                (type == TransactionType.IMPORT ? "Nhập kho" : "Xuất kho")
                        + " #" + t.getId() + " (" + t.getDetails().size() + " sản phẩm)");
        log.info("{} transaction #{} saved with {} details", type, t.getId(), t.getDetails().size());
        return t;
    }

    /**
     * Tạo phiếu nhập. Cập nhật giá vốn bình quân động (weighted average) cho từng sản phẩm.
     * cost_mới = (cost_cũ × qty_cũ + unitPrice × qty_nhập) / (qty_cũ + qty_nhập)
     */
    public StockTransaction createImport(User user, Integer supplierId, String note, List<StockTransactionDetail> details) {
        if (details == null || details.isEmpty())
            throw new IllegalArgumentException("Phiếu nhập phải có ít nhất 1 sản phẩm");
        return saveTransaction(user, TransactionType.IMPORT, PartnerType.SUPPLIER, supplierId, note, details);
    }

    /**
     * Tạo phiếu xuất. Kiểm tra tồn kho trước khi lưu.
     * Nếu bất kỳ dòng nào vượt tồn -> IllegalStateException, không lưu gì cả.
     */
    public StockTransaction createExport(User user, Integer customerId, String note, List<StockTransactionDetail> details) {
        if (details == null || details.isEmpty())
            throw new IllegalArgumentException("Phiếu xuất phải có ít nhất 1 sản phẩm");
        for (StockTransactionDetail d : details) {
            int onHand = reportDAO.getOnHand(d.getProduct().getId());
            if (d.getQuantity() == null || d.getQuantity() <= 0)
                throw new IllegalArgumentException("Số lượng xuất phải > 0");
            if (d.getQuantity() > onHand) {
                throw new IllegalStateException(String.format(
                        "Không đủ tồn cho sản phẩm ID=%d%s (tồn=%d, yêu cầu xuất=%d)",
                        d.getProduct().getId(),
                        productLabel(d), onHand, d.getQuantity()));
            }
        }
        return saveTransaction(user, TransactionType.EXPORT, PartnerType.CUSTOMER, customerId, note, details);
    }

    /**
     * Lấy tên/SKU sản phẩm để hiển thị trong thông báo lỗi.
     *
     * <p>Lưu ý: entity {@code Product} do UI dựng thường chỉ có {@code id}
     * (dùng {@code em.getReference}), nên các field khác có thể null hoặc chưa
     * được nạp — gọi {@code getName()} trực tiếp sẽ in ra "null" hoặc ném
     * LazyInitializationException. Vì vậy phải tự tra lại từ DB trong
     * EntityManager riêng, và vẫn fallback về id nếu không lấy được.
     */
    private String productLabel(StockTransactionDetail d) {
        Integer pid = d.getProduct().getId();
        if (pid == null) return "";
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            Product p = em.find(Product.class, pid);
            if (p == null) return "";
            String name = p.getName();
            String sku = p.getSku();
            if (name == null && sku == null) return "";
            return " '" + (name != null ? name : "?") + "'" + (sku != null ? " (" + sku + ")" : "");
        } catch (RuntimeException ex) {
            log.debug("Không lấy được thông tin sản phẩm {} để hiển thị lỗi", pid, ex);
            return "";
        } finally {
            em.close();
        }
    }

    private StockTransaction saveTransaction(User user, TransactionType type, PartnerType partnerType,
                                              Integer partnerId, String note,
                                              List<StockTransactionDetail> details) {
        EntityManager em = HibernateUtil.getEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            StockTransaction result = saveTransactionInTx(em, user, type, partnerType, partnerId, note, details);
            tx.commit();
            return result;
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            log.error("Transaction rolled back", ex);
            throw ex;
        } finally {
            em.close();
        }
    }

    private void recordMovement(EntityManager em, Product p, MovementType type, int qtyDelta,
                                Integer productId, Integer txId) {
        StockMovement mv = new StockMovement();
        mv.setProduct(p);
        mv.setType(type);
        mv.setQuantityChange(qtyDelta);
        int balance = reportDAO.getOnHand(productId);
        mv.setBalanceAfter(balance);
        mv.setRefTransactionId(txId);
        mv.setCreatedAt(LocalDateTime.now());
        em.persist(mv);
    }
}
