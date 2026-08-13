package com.example.expensemanager.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull LocalDate date,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String vendorName,
        String description
) {}
