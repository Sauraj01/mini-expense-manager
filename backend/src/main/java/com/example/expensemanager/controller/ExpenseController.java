package com.example.expensemanager.controller;

import com.example.expensemanager.dto.DashboardResponse;
import com.example.expensemanager.dto.ExpenseRequest;
import com.example.expensemanager.dto.RuleRequest;
import com.example.expensemanager.entity.Expense;
import com.example.expensemanager.entity.VendorCategoryRule;
import com.example.expensemanager.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @GetMapping("/expenses")
    public List<Expense> expenses() {
        return service.findAll();
    }

    @PostMapping("/expenses")
    public Expense create(@Valid @RequestBody ExpenseRequest request) {
        return service.create(request);
    }

    @PostMapping(value = "/expenses/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        return new UploadResponse(service.upload(file));
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestParam(defaultValue = "") String month) {
        if (month.isBlank()) month = YearMonth.now().toString();
        return service.dashboard(month);
    }

    @GetMapping("/rules")
    public List<VendorCategoryRule> rules() {
        return service.rules();
    }

    @PostMapping("/rules")
    public VendorCategoryRule addRule(@Valid @RequestBody RuleRequest request) {
        return service.addRule(new VendorCategoryRule(
                null, request.vendorKeyword(), request.category()
        ));
    }

    public record UploadResponse(int imported) {}
}
