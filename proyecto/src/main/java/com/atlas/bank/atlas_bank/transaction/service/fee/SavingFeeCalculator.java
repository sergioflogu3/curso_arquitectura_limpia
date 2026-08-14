package com.atlas.bank.atlas_bank.transaction.service.fee;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
public class SavingFeeCalculator implements FeeCalculator{
    @Override
    public boolean supports(String accountType) {
        return "SAVINGS".equals(accountType);
    }

    @Override
    public BigDecimal calculate(BigDecimal amount){
        return amount.multiply(new BigDecimal("0.01"));
    }
}
