package com.atlas.bank.atlas_bank.service;

import com.atlas.bank.atlas_bank.model.Account;
import com.atlas.bank.atlas_bank.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    public Account create(Account account){
        return accountRepository.save(account);
    }

    public List<Account> findAll(){
        return accountRepository.findAll();
    }

    public Account findById(Long id){
        return accountRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Cuenta no encontrada")
        );
    }
}
