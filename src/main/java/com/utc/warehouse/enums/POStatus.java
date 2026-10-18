package com.utc.warehouse.enums;

public enum POStatus {
    DRAFT,        // nháp, chưa gửi NCC
    ORDERED,      // đã gửi, chờ NCC giao
    PARTIAL,      // nhận một phần
    RECEIVED,     // đã nhận đủ
    CANCELLED     // hủy
}
