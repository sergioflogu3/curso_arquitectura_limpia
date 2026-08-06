package com.atlas.bank.atlas_bank.transaction.dto;

import com.atlas.bank.atlas_bank.transaction.model.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    TransactionResponse toResponse(Transaction transaction);
}
