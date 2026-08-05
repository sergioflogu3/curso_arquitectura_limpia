package com.atlas.bank.atlas_bank.transaction.service;

import com.atlas.bank.atlas_bank.transaction.model.Transaction;

import java.math.BigDecimal;

public interface ITransferService {
    Transaction execute(Long fromId, Long toId, BigDecimal amount);
}
