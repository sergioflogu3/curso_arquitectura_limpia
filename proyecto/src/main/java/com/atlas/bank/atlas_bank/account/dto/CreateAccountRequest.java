package com.atlas.bank.atlas_bank.account.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {
    private String accountNumber;
    private String ownerName;
    private String email;
    private String type;  // SAVING, CHECKING,
    private BigDecimal balance;
}
