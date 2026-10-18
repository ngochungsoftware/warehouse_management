package com.utc.warehouse.enums;

public enum InvoiceStatus {
    DRAFT,        // nháp, chưa xuất
    ISSUED,       // đã xuất, chưa thanh toán
    PARTIAL,      // thanh toán 1 phần
    PAID,         // đã thanh toán đủ
    CANCELLED     // huỷ
}
