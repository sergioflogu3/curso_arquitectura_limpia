package com.atlas.bank.atlas_bank.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAccountRequest {
    @NotBlank(message = "El numero de cuenta es obligatorio")
    private String accountNumber;
    @NotBlank(message = "El nombre del titular es obligatorio")
    private String ownerName;
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El eamil no tiene el formato valido")
    private String email;
    @NotBlank(message = "El tipo de cuenta es obligatorio")
    private String type;  // SAVING, CHECKING,
    @PositiveOrZero(message = "El balance no puede ser negativo")
    private BigDecimal balance;
}
