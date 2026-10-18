package com.utc.warehouse.entity;

import com.utc.warehouse.enums.PartnerType;
import com.utc.warehouse.enums.TransactionType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_transactions")
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(name = "partner_id")
    private Integer partnerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner_type", length = 10)
    private PartnerType partnerType;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StockTransactionDetail> details = new ArrayList<>();

    public StockTransaction() { }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public Integer getPartnerId() { return partnerId; }
    public void setPartnerId(Integer partnerId) { this.partnerId = partnerId; }

    public PartnerType getPartnerType() { return partnerType; }
    public void setPartnerType(PartnerType partnerType) { this.partnerType = partnerType; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<StockTransactionDetail> getDetails() { return details; }
    public void setDetails(List<StockTransactionDetail> details) { this.details = details; }

    public void addDetail(StockTransactionDetail d) {
        d.setTransaction(this);
        this.details.add(d);
    }
}
