package com.atlas.bank.atlas_bank.account.model;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    private String accountNumber;
    private String ownerName;
    private String email;
    private String type;  // SAVING, CHECKING,
    private BigDecimal balance;
    private String status; // ACTIVE, CLOSED, FROZEN
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (status == null) this.status = "ACTIVE";
        if (balance == null) this.balance = BigDecimal.ZERO;
    }
}
