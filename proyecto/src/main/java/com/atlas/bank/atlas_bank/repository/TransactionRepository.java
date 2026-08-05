package com.atlas.bank.atlas_bank.repository;

import com.atlas.bank.atlas_bank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySourceAccountIdOrTargetAccountId(Long sourceId, Long targetId);
}