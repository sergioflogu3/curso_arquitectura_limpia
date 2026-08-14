package com.atlas.bank.atlas_bank.transaction.service;

import com.atlas.bank.atlas_bank.transaction.model.Transaction;
import com.atlas.bank.atlas_bank.transaction.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public abstract class TransactionProcessor<C> {
    protected final TransactionRepository transactionRepository;

    @Transactional
    public Transaction process(C context) {
        validate(context);
        BigDecimal fee = calculateFee(context);
        execute(context, fee);
        return save(context, fee);
    }

    protected abstract void validate(C context);
    protected abstract BigDecimal calculateFee(C context);
    protected abstract void execute(C context, BigDecimal fee);
    protected abstract Transaction save(C context, BigDecimal fee);
}
