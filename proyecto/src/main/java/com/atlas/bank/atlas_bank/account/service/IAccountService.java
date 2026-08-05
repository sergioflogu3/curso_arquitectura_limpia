package com.atlas.bank.atlas_bank.service;

import com.atlas.bank.atlas_bank.account.model.Account;

import java.util.List;

public interface IAccountService {
    Account create(Account account);
    List<Account> findAll();
    Account findById(Long id);
}
