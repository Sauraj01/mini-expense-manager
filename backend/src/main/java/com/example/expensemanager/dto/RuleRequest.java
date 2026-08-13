package com.example.expensemanager.dto;

import jakarta.validation.constraints.NotBlank;

public record RuleRequest(
        @NotBlank String vendorKeyword,
        @NotBlank String category
) {}
