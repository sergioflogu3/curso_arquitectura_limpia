package com.atlas.bank.atlas_bank.service;

import com.atlas.bank.atlas_bank.model.Transaction;
import com.atlas.bank.atlas_bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionQueryService implements ITransactionQueryService {
    private final TransactionRepository transactionRepository;

    @Override
    public List<Transaction> getByAccountId (Long accountId){
        return transactionRepository.findBySourceAccountIdOrTargetAccountId(accountId, accountId);
    }
}
