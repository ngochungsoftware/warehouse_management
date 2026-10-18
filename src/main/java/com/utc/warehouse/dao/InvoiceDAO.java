package com.utc.warehouse.dao;

import com.utc.warehouse.entity.Invoice;

public class InvoiceDAO extends GenericDAO<Invoice, Integer> {
    @Override
    protected Class<Invoice> entityClass() { return Invoice.class; }
}
