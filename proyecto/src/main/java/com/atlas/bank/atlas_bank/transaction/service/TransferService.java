package com.atlas.bank.atlas_bank.transaction.service;

import com.atlas.bank.atlas_bank.account.exception.AccountNotFoundException;
import com.atlas.bank.atlas_bank.account.model.Account;
import com.atlas.bank.atlas_bank.transaction.exception.AccountNotActiveException;
import com.atlas.bank.atlas_bank.transaction.exception.InsufficientFoundsException;
import com.atlas.bank.atlas_bank.transaction.model.Transaction;
import com.atlas.bank.atlas_bank.account.repository.AccountRepository;
import com.atlas.bank.atlas_bank.transaction.repository.TransactionRepository;
import com.atlas.bank.atlas_bank.transaction.service.fee.FeeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransferService extends TransactionProcessor<TransferContext> implements ITransferService{
    private final AccountRepository accountRepository;
    private final List<FeeCalculator> feeCalculators;

    public TransferService(TransactionRepository transactionRepository,
                           AccountRepository accountRepository,
                           List<FeeCalculator> feeCalculators) {
        super(transactionRepository);
        this.accountRepository = accountRepository;
        this.feeCalculators = feeCalculators;
    }

    @Override
    @Transactional
    public Transaction execute(Long fromId, Long toId, BigDecimal amount) {
        // Buscar cuentas
        Account from = accountRepository.findById(fromId)
                .orElseThrow(() -> new AccountNotFoundException(fromId));
        Account to = accountRepository.findById(toId)
                .orElseThrow(() -> new AccountNotFoundException(toId));
        return process(new TransferContext(from, to, amount));
    }

    public List<Transaction> getTransactions(Long accountId) {
        return transactionRepository
                .findBySourceAccountIdOrTargetAccountId(accountId, accountId);
    }

    @Override
    protected void validate(TransferContext context) {
        // Validar que la cuenta esté activa
        if (!"ACTIVE".equals(context.from().getStatus())) {
            throw new AccountNotActiveException(context.from().getId(), context.from().getStatus());
        }
        if (!"ACTIVE".equals(context.to().getStatus())) {
            throw new AccountNotActiveException(context.to().getId(), context.to().getStatus());
        }

        // Validar fondos
        if (context.from().getBalance().compareTo(context.amount()) < 0) {
            throw new InsufficientFoundsException(context.from().getId(), context.from().getBalance(), context.amount());
        }
    }

    @Override
    protected BigDecimal calculateFee(TransferContext context) {
        // Calcular comisión — hardcodeada
        return feeCalculators.stream()
                .filter(fc -> fc.supports(context.from().getType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hay calculador para el tipo: " + context.from().getType()))
                .calculate(context.amount());
    }

    @Override
    protected void execute(TransferContext context, BigDecimal fee) {
        // Actualizar saldos
        context.from().setBalance(context.from().getBalance().subtract(context.amount()).subtract(fee));
        context.to().setBalance(context.to().getBalance().add(context.amount()));
        accountRepository.save(context.from());
        accountRepository.save(context.to());
    }

    @Override
    protected Transaction save(TransferContext context, BigDecimal fee) {
        // Crear transacción
        Transaction transaction = new Transaction();
        transaction.setType("TRANSFER");
        transaction.setSourceAccountId(context.from().getId());
        transaction.setTargetAccountId(context.to().getId());
        transaction.setAmount(context.amount());
        transaction.setFee(fee);
        transaction.setStatus("EXECUTED");
        return transactionRepository.save(transaction);
    }
}
