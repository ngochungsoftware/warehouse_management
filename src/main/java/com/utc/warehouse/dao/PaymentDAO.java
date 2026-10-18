package com.utc.warehouse.dao;

import com.utc.warehouse.entity.Payment;

public class PaymentDAO extends GenericDAO<Payment, Integer> {
    @Override
    protected Class<Payment> entityClass() { return Payment.class; }
}
