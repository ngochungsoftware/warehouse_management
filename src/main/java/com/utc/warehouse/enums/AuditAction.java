package com.utc.warehouse.enums;

/**
 * Hành động được ghi log.
 * Dùng {@code @Enumerated(EnumType.STRING)} trong entity.
 */
public enum AuditAction {
    LOGIN, LOGOUT,
    CREATE, UPDATE, DELETE,
    IMPORT, EXPORT,            // phiếu nhập / xuất
    INVOICE_CREATE, INVOICE_PAY, INVOICE_CANCEL,
    PO_CREATE, PO_APPROVE, PO_RECEIVE, PO_CANCEL,
    PAYMENT_CREATE,
    STOCK_ADJUST
}
