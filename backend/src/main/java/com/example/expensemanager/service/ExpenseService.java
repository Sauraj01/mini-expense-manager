package com.example.expensemanager.service;

import com.example.expensemanager.dto.DashboardResponse;
import com.example.expensemanager.dto.ExpenseRequest;
import com.example.expensemanager.entity.Expense;
import com.example.expensemanager.entity.VendorCategoryRule;
import com.example.expensemanager.repository.ExpenseRepository;
import com.example.expensemanager.repository.VendorCategoryRuleRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final VendorCategoryRuleRepository ruleRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          VendorCategoryRuleRepository ruleRepository) {
        this.expenseRepository = expenseRepository;
        this.ruleRepository = ruleRepository;
    }

    public List<Expense> findAll() {
        return expenseRepository.findAll();
    }

    public Expense create(ExpenseRequest request) {
        Expense expense = new Expense(
                null,
                request.date(),
                request.amount(),
                request.vendorName().trim(),
                request.description(),
                categorize(request.vendorName()),
                false
        );
        expense = expenseRepository.save(expense);
        recalculateAnomalies();
        return expenseRepository.findById(expense.getId()).orElseThrow();
    }

    public int upload(MultipartFile file) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            var format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .build();

            for (CSVRecord record : format.parse(reader)) {
                LocalDate date = LocalDate.parse(record.get("date").trim());
                BigDecimal amount = new BigDecimal(record.get("amount").trim());
                String vendor = record.get("vendorName").trim();
                String description = record.isMapped("description")
                        ? record.get("description").trim() : "";

                expenseRepository.save(new Expense(
                        null, date, amount, vendor, description,
                        categorize(vendor), false
                ));
                count++;
            }
            recalculateAnomalies();
            return count;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid CSV. Expected columns: date, amount, vendorName, description", e);
        }
    }

    public DashboardResponse dashboard(String month) {
        YearMonth ym = YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Expense> monthExpenses = expenseRepository.findByDateBetween(start, end);

        Map<String, BigDecimal> totals = monthExpenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));

        List<DashboardResponse.VendorTotal> topVendors = monthExpenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getVendorName,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .map(e -> new DashboardResponse.VendorTotal(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(DashboardResponse.VendorTotal::total).reversed())
                .limit(5)
                .toList();

        List<Expense> anomalies = monthExpenses.stream()
                .filter(Expense::isAnomaly)
                .sorted(Comparator.comparing(Expense::getAmount).reversed())
                .toList();

        return new DashboardResponse(
                month, totals, topVendors, anomalies.size(), anomalies
        );
    }

    public List<VendorCategoryRule> rules() {
        return ruleRepository.findAll();
    }

    public VendorCategoryRule addRule(VendorCategoryRule rule) {
        rule.setVendorKeyword(rule.getVendorKeyword().trim().toLowerCase());
        return ruleRepository.save(rule);
    }

    private String categorize(String vendor) {
        String normalized = vendor.toLowerCase(Locale.ROOT);
        return ruleRepository.findAll().stream()
                .filter(rule -> normalized.contains(rule.getVendorKeyword().toLowerCase(Locale.ROOT)))
                .map(VendorCategoryRule::getCategory)
                .findFirst()
                .orElse("Other");
    }

    private void recalculateAnomalies() {
        List<Expense> expenses = expenseRepository.findAll();

        Map<String, BigDecimal> averages = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.collectingAndThen(
                                Collectors.mapping(Expense::getAmount, Collectors.toList()),
                                amounts -> amounts.stream()
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                                        .divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP)
                        )
                ));

        for (Expense expense : expenses) {
            BigDecimal average = averages.get(expense.getCategory());
            boolean anomaly = average != null
                    && average.compareTo(BigDecimal.ZERO) > 0
                    && expense.getAmount().compareTo(average.multiply(BigDecimal.valueOf(3))) > 0;
            expense.setAnomaly(anomaly);
        }

        expenseRepository.saveAll(expenses);
    }
}
