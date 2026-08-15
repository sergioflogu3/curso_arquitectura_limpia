package com.atlas.bank.atlas_bank.transaction.service.transfer;

import com.atlas.bank.atlas_bank.account.model.Account;

import java.math.BigDecimal;

public record TransferContext(Account from, Account to, BigDecimal amount) {

}
