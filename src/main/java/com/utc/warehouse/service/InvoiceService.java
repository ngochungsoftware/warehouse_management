package com.utc.warehouse.service;

import com.utc.warehouse.config.HibernateUtil;
import com.utc.warehouse.dao.InvoiceDAO;
import com.utc.warehouse.dao.PaymentDAO;
import com.utc.warehouse.dao.StockTransactionDAO;
import com.utc.warehouse.entity.Customer;
import com.utc.warehouse.entity.Invoice;
import com.utc.warehouse.entity.InvoiceItem;
import com.utc.warehouse.entity.Payment;
import com.utc.warehouse.entity.Product;
import com.utc.warehouse.entity.StockTransaction;
import com.utc.warehouse.entity.StockTransactionDetail;
import com.utc.warehouse.entity.User;
import com.utc.warehouse.enums.InvoiceStatus;
import com.utc.warehouse.enums.PaymentMethod;
import com.utc.warehouse.enums.PaymentType;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Tạo và quản lý hóa đơn bán hàng, phiếu thu/chi.
 *
 * Quy trình:
 *  1) Tạo phiếu xuất kho (stock_transactions) - ghi tồn
 *  2) Tạo hóa đơn (invoices) - ghi tài chính
 *     - subtotal = sum(qty * unitPrice) chưa VAT
 *     - vatAmount = sum(qty * unitPrice * vatRate / 100)
 *     - discount = sum(discount)
 *     - total = subtotal + vatAmount - discount
 *  3) Tạo phiếu thu (payments) - cập nhật paid/debt trên invoice
 */
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    private static final DateTimeFormatter CODE_FMT = DateTimeFormatter.ofPattern("yyyy");

    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final StockTransactionDAO stockDAO = new StockTransactionDAO();
    private final StockService stockService = new StockService();

    /**
     * Tạo đồng thời phiếu xuất kho + hóa đơn.
     * Phiếu xuất giúp tồn kho giảm; hóa đơn ghi tài chính.
     */
    public Invoice createFromExport(User user, Integer customerId, String note,
                                    List<InvoiceItemDraft> drafts) {
        if (drafts == null || drafts.isEmpty())
            throw new IllegalArgumentException("Hóa đơn phải có ít nhất 1 sản phẩm");
        // Validation
        for (InvoiceItemDraft d : drafts) {
            if (d.productId == null) throw new IllegalArgumentException("Sản phẩm trống");
            if (d.quantity == null || d.quantity <= 0) throw new IllegalArgumentException("Số lượng phải > 0");
            if (d.unitPrice == null) throw new IllegalArgumentException("Đơn giá trống");
        }

        EntityManager em = HibernateUtil.getEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();

            // 1) Tạo phiếu xuất
            List<StockTransactionDetail> details = new java.util.ArrayList<>();
            for (InvoiceItemDraft d : drafts) {
                StockTransactionDetail sdet = new StockTransactionDetail();
                Product p = em.getReference(Product.class, d.productId);
                sdet.setProduct(p);
                sdet.setQuantity(d.quantity);
                sdet.setUnitPrice(d.unitPrice);
                details.add(sdet);
            }
            StockTransaction st = stockService.createExportInTx(em, user, customerId, note, details);
            em.flush();

            // 2) Tạo hóa đơn
            Invoice inv = new Invoice();
            inv.setCode(generateCode(em, "HD"));
            inv.setTransactionId(st.getId());
            if (customerId != null) inv.setCustomer(em.getReference(Customer.class, customerId));
            inv.setUser(em.getReference(User.class, user.getId()));
            inv.setIssueDate(LocalDate.now());
            inv.setStatus(InvoiceStatus.ISSUED);
            inv.setCreatedAt(LocalDateTime.now());
            inv.setNote(note);

            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal vatAmount = BigDecimal.ZERO;
            BigDecimal discount = BigDecimal.ZERO;

            for (InvoiceItemDraft d : drafts) {
                Product p = em.find(Product.class, d.productId);
                InvoiceItem it = new InvoiceItem();
                it.setProduct(p);
                it.setQuantity(d.quantity);
                it.setUnitPrice(d.unitPrice);
                it.setCostPriceSnapshot(p.getCostPrice() == null ? BigDecimal.ZERO : p.getCostPrice());
                it.setVatRate(d.vatRate == null ? p.getVatRate() : d.vatRate);
                it.setDiscount(d.discount == null ? BigDecimal.ZERO : d.discount);
                BigDecimal lineBase = d.unitPrice.multiply(BigDecimal.valueOf(d.quantity));
                BigDecimal lineVat = lineBase.multiply(it.getVatRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal lineTotal = lineBase.add(lineVat).subtract(it.getDiscount());
                it.setLineTotal(lineTotal);
                inv.addItem(it);

                subtotal = subtotal.add(lineBase);
                vatAmount = vatAmount.add(lineVat);
                discount = discount.add(it.getDiscount());
            }
            BigDecimal total = subtotal.add(vatAmount).subtract(discount);
            inv.setSubtotal(subtotal);
            inv.setVatAmount(vatAmount);
            inv.setDiscount(discount);
            inv.setTotal(total);
            inv.setPaid(BigDecimal.ZERO);
            inv.setDebt(total);
            em.persist(inv);
            em.flush();
            AuditLogger.logInTx(em, user, com.utc.warehouse.enums.AuditAction.INVOICE_CREATE,
                    "Invoice", inv.getId(), inv.getCode(),
                    "Tạo HĐ " + inv.getCode() + " từ phiếu xuất #" + st.getId()
                            + " - " + drafts.size() + " SP, tổng " + inv.getTotal());

            tx.commit();
            log.info("Created invoice {} from export #{}, total={}",
                    inv.getCode(), st.getId(), total);
            return inv;
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    /**
     * Tạo phiếu thu, cập nhật paid/debt của hóa đơn.
     */
    public Payment createReceipt(User user, Integer invoiceId, BigDecimal amount,
                                 PaymentMethod method, String note) {
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Số tiền thu phải > 0");
        return recordPayment(user, PaymentType.RECEIPT, invoiceId, amount, method, note);
    }

    /** Phiếu chi (trả NCC, chi phí) - không gắn vào invoice. */
    public Payment createPayment(User user, String partnerType, Integer partnerId,
                                 BigDecimal amount, PaymentMethod method, String note) {
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("Số tiền chi phải > 0");
        return recordPayment(user, PaymentType.PAYMENT, null, amount, method, note,
                partnerType, partnerId);
    }

    private Payment recordPayment(User user, PaymentType type, Integer invoiceId,
                                  BigDecimal amount, PaymentMethod method, String note) {
        return recordPayment(user, type, invoiceId, amount, method, note, null, null);
    }

    private Payment recordPayment(User user, PaymentType type, Integer invoiceId,
                                  BigDecimal amount, PaymentMethod method, String note,
                                  String partnerType, Integer partnerId) {
        EntityManager em = HibernateUtil.getEntityManager();
        var tx = em.getTransaction();
        try {
            tx.begin();

            Invoice inv = invoiceId == null ? null
                    : em.find(Invoice.class, invoiceId);
            if (type == PaymentType.RECEIPT && inv == null)
                throw new IllegalArgumentException("Không tìm thấy hóa đơn ID=" + invoiceId);
            if (type == PaymentType.RECEIPT && amount.compareTo(inv.getDebt()) > 0)
                throw new IllegalArgumentException("Số tiền thu vượt công nợ (" + inv.getDebt() + ")");

            Payment p = new Payment();
            p.setCode(generateCode(em, type == PaymentType.RECEIPT ? "PT" : "PC"));
            p.setType(type);
            p.setAmount(amount);
            p.setMethod(method);
            p.setInvoice(inv);
            p.setUser(em.getReference(User.class, user.getId()));
            p.setNote(note);
            p.setCreatedAt(LocalDateTime.now());
            if (inv != null) {
                p.setPartnerId(inv.getCustomer() == null ? null : inv.getCustomer().getId());
                p.setPartnerType("CUSTOMER");
            } else {
                p.setPartnerId(partnerId);
                p.setPartnerType(partnerType);
            }
            em.persist(p);

            if (inv != null) {
                BigDecimal newPaid = inv.getPaid().add(amount);
                inv.setPaid(newPaid);
                inv.setDebt(inv.getTotal().subtract(newPaid));
                if (inv.getDebt().signum() < 0) inv.setDebt(BigDecimal.ZERO);
                inv.recomputeStatus();
                em.merge(inv);
            }
            em.flush();
            AuditLogger.logInTx(em, user,
                    type == PaymentType.RECEIPT
                            ? com.utc.warehouse.enums.AuditAction.INVOICE_PAY
                            : com.utc.warehouse.enums.AuditAction.PAYMENT_CREATE,
                    "Payment", p.getId(), p.getCode(),
                    (type == PaymentType.RECEIPT ? "Thu tiền" : "Chi")
                            + " " + p.getAmount() + " đ - " + method
                            + (inv == null ? "" : " cho HĐ " + inv.getCode()));

            tx.commit();
            log.info("Created {} {} amount={}", type, p.getCode(), amount);
            return p;
        } catch (RuntimeException ex) {
            if (tx.isActive()) tx.rollback();
            throw ex;
        } finally {
            em.close();
        }
    }

    public List<Invoice> allInvoices() { return invoiceDAO.findAll(); }

    public Optional<Invoice> findInvoice(Integer id) { return invoiceDAO.findById(id); }

    public List<Payment> allPayments() { return paymentDAO.findAll(); }

    /* ============== Helpers ============== */
    private String generateCode(EntityManager em, String prefix) {
        String year = java.time.LocalDate.now().format(CODE_FMT);
        Long seq = em.createQuery(
                "SELECT COUNT(p) FROM " + (prefix.equals("HD") ? "Invoice" : "Payment") + " p " +
                        "WHERE p.code LIKE :pf", Long.class)
                .setParameter("pf", prefix + year + "-%")
                .getSingleResult();
        return String.format("%s%s-%05d", prefix, year, seq + 1);
    }

    /** DTO đầu vào cho 1 dòng hóa đơn. */
    public static class InvoiceItemDraft {
        public Integer productId;
        public Integer quantity;
        public BigDecimal unitPrice;
        public BigDecimal vatRate;
        public BigDecimal discount;
    }
}
