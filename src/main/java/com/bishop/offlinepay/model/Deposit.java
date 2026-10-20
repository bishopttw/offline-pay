package com.bishop.offlinepay.model;

import jakarta.persistence.*;
import org.springframework.cglib.core.Local;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposits")
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reference;

    private Long accountId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private DepositStatus status;

    private LocalDateTime createdAt;

    @Version
    private Long version;

    public Deposit() {}

    public Deposit(String reference, Long accountId, BigDecimal amount) {
        this.reference = reference;
        this.accountId = accountId;
        this.amount = amount;
        this.status = DepositStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public DepositStatus getStatus() {
        return status;
    }

    public void setStatus(DepositStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
