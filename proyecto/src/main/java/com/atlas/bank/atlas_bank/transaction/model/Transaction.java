package com.atlas.bank.atlas_bank.transaction.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type; // DEPOSIT, WITHDRAWAL, TRANSFER
    private Long sourceAccountId;
    private Long targetAccountId;
    private BigDecimal amount;
    private BigDecimal fee;
    private String status; // PENDING, EXECUTED, REJECTED
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null)
            this.status = "EXECUTED";
    }
}