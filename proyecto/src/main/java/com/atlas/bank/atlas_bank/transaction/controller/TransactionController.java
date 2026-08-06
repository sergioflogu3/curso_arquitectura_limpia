package com.atlas.bank.atlas_bank.transaction.controller;

import com.atlas.bank.atlas_bank.transaction.dto.TransactionResponse;
import com.atlas.bank.atlas_bank.transaction.dto.TransferRequest;
import com.atlas.bank.atlas_bank.transaction.model.Transaction;
import com.atlas.bank.atlas_bank.transaction.service.ITransactionQueryService;
import com.atlas.bank.atlas_bank.transaction.service.ITransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final ITransferService transferService;
    private final ITransactionQueryService transactionQueryService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@RequestBody TransferRequest request){
        Transaction transaction = transferService.execute(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount()
        );
        return ResponseEntity.ok(toResponse(transaction));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@PathVariable Long id){
        List<TransactionResponse> responses = transactionQueryService.getByAccountId(id)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    private TransactionResponse toResponse(Transaction transaction){
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setType(transaction.getType());
        response.setSourceAccountId(transaction.getSourceAccountId());
        response.setTargetAccountId(transaction.getTargetAccountId());
        response.setAmount(transaction.getAmount());
        response.setFee(transaction.getFee());
        response.setStatus(transaction.getStatus());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }
}
