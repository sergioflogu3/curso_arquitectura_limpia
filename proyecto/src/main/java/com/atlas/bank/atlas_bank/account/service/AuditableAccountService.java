package com.atlas.bank.atlas_bank.account.service;

import com.atlas.bank.atlas_bank.account.model.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Primary
public class AuditableAccountService implements IAccountService{

    private final IAccountService delegate;

    public AuditableAccountService(@Qualifier("accountService") IAccountService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Account create(Account account) {
        log.info("Creando una cuenta - numero: {}, titular {}",
                account.getAccountNumber(), account.getOwnerName());
        Account created = delegate.create(account);
        log.info("Cuenta creada exitosamente - ID: {}",  created.getId());
        return null;
    }

    @Override
    public List<Account> findAll() {
        return delegate.findAll();
    }

    @Override
    public Account findById(Long id) {
        return delegate.findById(id);
    }
}
