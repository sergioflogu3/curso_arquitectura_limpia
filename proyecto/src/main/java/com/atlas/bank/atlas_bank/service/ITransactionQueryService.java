package com.atlas.bank.atlas_bank.service;

import com.atlas.bank.atlas_bank.model.Transaction;

import java.util.List;

public interface ITransactionQueryService {
    List<Transaction> getByAccountId(Long accountId);
}
