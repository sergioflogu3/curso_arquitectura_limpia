package com.atlas.bank.atlas_bank.service;

import com.atlas.bank.atlas_bank.model.Transaction;

import java.math.BigDecimal;

public interface ITransferService {
    Transaction execute(Long fromId, Long toId, BigDecimal amount);
}
