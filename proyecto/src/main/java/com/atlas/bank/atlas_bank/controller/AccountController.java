package com.atlas.bank.atlas_bank.controller;

import com.atlas.bank.atlas_bank.model.Account;
import com.atlas.bank.atlas_bank.model.Transaction;
import com.atlas.bank.atlas_bank.repository.TransactionQueryService;
import com.atlas.bank.atlas_bank.service.AccountService;
import com.atlas.bank.atlas_bank.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final TransferService transferService;
    private final TransactionQueryService transactionQueryService;

    @PostMapping
    public ResponseEntity<Account> create(@RequestBody Account account){
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.create(account));
    }

    @GetMapping
    public ResponseEntity<List<Account>> findAll(){
        return ResponseEntity.ok(accountService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> findById(@PathVariable Long id){
        return ResponseEntity.ok(accountService.findById(id));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(@RequestParam Long fromId,
                                                @RequestParam Long toId,
                                                @RequestParam BigDecimal amount){
        return ResponseEntity.ok(transferService.execute(fromId, toId, amount));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(@PathVariable Long id){
        return ResponseEntity.ok(transactionQueryService.getByAccountId(id));
    }

}
