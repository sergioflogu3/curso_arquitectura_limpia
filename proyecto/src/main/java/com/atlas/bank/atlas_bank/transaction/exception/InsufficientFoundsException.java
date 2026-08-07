package com.atlas.bank.atlas_bank.transaction.exception;

import java.math.BigDecimal;

public class InsufficientFoundsException extends RuntimeException {
    public InsufficientFoundsException(Long accountId, BigDecimal balance, BigDecimal amount) {
        super("La cuenta con ID: " + accountId + " tiene saldo: " + balance + "y se intento transferir " + amount);
    }
}
