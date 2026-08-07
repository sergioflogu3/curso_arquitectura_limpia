package com.atlas.bank.atlas_bank.transaction.dto;

import com.atlas.bank.atlas_bank.transaction.validation.DifferentAccounts;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@DifferentAccounts
public class TransferRequest {
    @NotNull(message = "El id del origen es obligatorio")
    private Long fromAccountId;
    @NotNull(message = "El id del destino es obligatorio")
    private Long toAccountId;
    @NotNull(message = "La cantidad es obligatorio")
    @Positive(message = "El monto debe ser mayor a cero")
    private BigDecimal amount;
}
