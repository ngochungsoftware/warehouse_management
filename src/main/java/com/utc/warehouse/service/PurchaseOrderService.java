package com.utc.warehouse.service;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.dao.PurchaseOrderDAO;
import com.utc.warehouse.entity.Product;
import com.utc.warehouse.entity.PurchaseOrder;
import com.utc.warehouse.entity.PurchaseOrderItem;
import com.utc.warehouse.entity.StockTransaction;
import com.utc.warehouse.entity.StockTransactionDetail;
import com.utc.warehouse.entity.Supplier;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.AuditAction;
import com.utc.warehouse.enums.POStatus;
import com.utc.warehouse.enums.PartnerType;
import com.utc.warehouse.enums.TransactionType;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Quản lý đơn đặt hàng mua (Purchase Order).
 *
 * Workflow:
 *  1) create()         - tạo đơn (status=DRAFT)
 *  2) approve()        - chuyển sang ORDERED (gửi NCC)
 *  3) receive()        - nhận hàng, tự tạo phiếu nhập kho, cập nhật cost price
 *  4) cancel()         - huỷ đơn (chỉ khi DRAFT/ORDERED)
 */
public class PurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderService.class);
    private static final DateTimeFormatter CODE_FMT = DateTimeFormatter.ofPattern("yyyy");

    private final PurchaseOrderDAO dao = new PurchaseOrderDAO();
    private final StockService stockService = new StockService();

    public List<PurchaseOrder> all() { return dao.findAllOrdered(); }
    public List<PurchaseOrder> findByStatus(POStatus st) { return dao.findByStatus(st); }
    public Optional<PurchaseOrder> findById(Integer id) { return dao.findById(id); }

    /** Tạo đơn DRAFT. */
    public PurchaseOrder create(User user, Integer supplierId, String note,
                                 LocalDate expectedDate, List<PurchaseOrderItem> items) {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Đơn hàng phải có ít nhất 1 sản phẩm");
        EntityManager em = HibernateUtil.getEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            PurchaseOrder po = new PurchaseOrder();
            po.setCode(generateCode(em, "PO"));
            po.setStatus(POStatus.DRAFT);
            po.setUser(em.getReference(User.class, user.getId()));
            if (supplierId != null) po.setSupplier(em.getReference(Supplier.class, supplierId));
            po.setNote(note);
            po.setExpectedDate(expectedDate);
            po.setCreatedAt(LocalDateTime.now());
            po.setUpdatedAt(LocalDateTime.now());

            for (PurchaseOrderItem it : items) {
                if (it.getOrderedQty() == null || it.getOrderedQty() <= 0)
                    throw new IllegalArgumentException("Số lượng đặt phải > 0");
                if (it.getUnitPrice() == null) it.setUnitPrice(BigDecimal.ZERO);
                Product p = em.find(Product.class, it.getProduct().getId());
                if (p == null) throw new IllegalArgumentException("Sản phẩm ID=" + it.getProduct().getId() + " không tồn tại");
                it.setProduct(p);
                po.addItem(it);
            }
            po.recomputeTotal();
            em.persist(po);
            AuditLogger.logInTx(em, user, AuditAction.PO_CREATE, "PurchaseOrder",
                    null, po.getCode(),
                    "Tạo đơn đặt hàng " + po.getCode() + " với " + items.size() + " sản phẩm, tổng " + po.getTotal());
            tx.commit();
            return po;
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    /** Chuyển DRAFT -> ORDERED. */
    public PurchaseOrder approve(User user, Integer poId) {
        EntityManager em = HibernateUtil.getEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            PurchaseOrder po = em.find(PurchaseOrder.class, poId);
            if (po == null) throw new IllegalArgumentException("Không tìm thấy PO #" + poId);
            if (po.getStatus() != POStatus.DRAFT)
                throw new IllegalStateException("Chỉ duyệt được đơn ở trạng thái DRAFT (hiện tại: " + po.getStatus() + ")");
            po.setStatus(POStatus.ORDERED);
            po.setUpdatedAt(LocalDateTime.now());
            em.merge(po);
            AuditLogger.logInTx(em, user, AuditAction.PO_APPROVE, "PurchaseOrder",
                    po.getId(), po.getCode(), "Duyệt đơn " + po.getCode());
            tx.commit();
            return po;
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    /**
     * Nhận hàng: tạo phiếu nhập kho từ PO, cập nhật cost price từng SP.
     * Nếu nhận đủ -> status=RECEIVED. Nếu nhận một phần -> status=PARTIAL.
     * Nếu chưa nhận gì -> giữ ORDERED.
     */
    public PurchaseOrder receive(User user, Integer poId, boolean receiveAll) {
        EntityManager em = HibernateUtil.getEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            PurchaseOrder po = em.find(PurchaseOrder.class, poId);
            if (po == null) throw new IllegalArgumentException("Không tìm thấy PO #" + poId);
            if (po.getStatus() != POStatus.ORDERED && po.getStatus() != POStatus.PARTIAL)
                throw new IllegalStateException("Chỉ nhận hàng cho đơn ORDERED/PARTIAL (hiện tại: " + po.getStatus() + ")");

            // Tính số lượng còn phải nhận
            List<StockTransactionDetail> details = new ArrayList<>();
            boolean fullyReceived = true;
            for (PurchaseOrderItem it : po.getItems()) {
                int remain = it.getOrderedQty() - it.getReceivedQty();
                if (remain <= 0) continue;
                if (receiveAll) {
                    it.setReceivedQty(it.getOrderedQty());
                    StockTransactionDetail d = new StockTransactionDetail();
                    d.setProduct(it.getProduct());
                    d.setQuantity(remain);
                    d.setUnitPrice(it.getUnitPrice());
                    details.add(d);
                } else {
                    // Partial: hỏi từng dòng (giao diện sẽ xử lý). Ở service mặc định nhận hết.
                    it.setReceivedQty(it.getOrderedQty());
                    StockTransactionDetail d = new StockTransactionDetail();
                    d.setProduct(it.getProduct());
                    d.setQuantity(remain);
                    d.setUnitPrice(it.getUnitPrice());
                    details.add(d);
                }
            }
            if (details.isEmpty())
                throw new IllegalStateException("Đơn hàng đã được nhận đủ.");

            // Tạo phiếu nhập (gọi vào transaction hiện tại)
            StockTransaction st = stockService.saveTransactionInTx(
                    em, user, TransactionType.IMPORT, PartnerType.SUPPLIER,
                    po.getSupplier() == null ? null : po.getSupplier().getId(),
                    "Nhận hàng từ PO " + po.getCode(),
                    details);

            // Cập nhật trạng thái PO
            for (PurchaseOrderItem it : po.getItems()) {
                if (it.getReceivedQty() < it.getOrderedQty()) {
                    fullyReceived = false;
                    break;
                }
            }
            po.setStatus(fullyReceived ? POStatus.RECEIVED : POStatus.PARTIAL);
            po.setReceivedTransactionId(st.getId());
            po.setUpdatedAt(LocalDateTime.now());
            em.merge(po);

            AuditLogger.logInTx(em, user, AuditAction.PO_RECEIVE, "PurchaseOrder",
                    po.getId(), po.getCode(),
                    "Nhận hàng PO " + po.getCode() + " → phiếu nhập #" + st.getId()
                            + (fullyReceived ? " (đủ)" : " (một phần)"));

            tx.commit();
            return po;
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    /** Huỷ đơn. */
    public PurchaseOrder cancel(User user, Integer poId, String reason) {
        EntityManager em = HibernateUtil.getEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();
            PurchaseOrder po = em.find(PurchaseOrder.class, poId);
            if (po == null) throw new IllegalArgumentException("Không tìm thấy PO #" + poId);
            if (po.getStatus() == POStatus.RECEIVED || po.getStatus() == POStatus.CANCELLED)
                throw new IllegalStateException("Không thể huỷ đơn ở trạng thái " + po.getStatus());
            po.setStatus(POStatus.CANCELLED);
            po.setUpdatedAt(LocalDateTime.now());
            em.merge(po);
            AuditLogger.logInTx(em, user, AuditAction.PO_CANCEL, "PurchaseOrder",
                    po.getId(), po.getCode(),
                    "Huỷ đơn " + po.getCode() + (reason == null || reason.isBlank() ? "" : " - " + reason));
            tx.commit();
            return po;
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    private String generateCode(EntityManager em, String prefix) {
        String year = LocalDate.now().format(CODE_FMT);
        Long seq = em.createQuery(
                "SELECT COUNT(p) FROM PurchaseOrder p WHERE p.code LIKE :pf", Long.class)
                .setParameter("pf", prefix + year + "-%")
                .getSingleResult();
        return String.format("%s%s-%05d", prefix, year, seq + 1);
    }
}
