package com.atlas.bank.atlas_bank.transaction.exception;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(Long accountId, String status) {
        super("La cuenta " + accountId + " no esta activa. Estado actual: " + status);
    }
}
