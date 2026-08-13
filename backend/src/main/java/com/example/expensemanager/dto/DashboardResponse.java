package com.example.expensemanager.dto;

import com.example.expensemanager.entity.Expense;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        String month,
        Map<String, BigDecimal> monthlyTotalsByCategory,
        List<VendorTotal> topVendors,
        long anomalyCount,
        List<Expense> anomalies
) {
    public record VendorTotal(String vendorName, BigDecimal total) {}
}
