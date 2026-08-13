package com.example.expensemanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String vendorName;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private boolean anomaly;

    public Expense() {}

    public Expense(Long id, LocalDate date, BigDecimal amount, String vendorName,
                   String description, String category, boolean anomaly) {
        this.id = id;
        this.date = date;
        this.amount = amount;
        this.vendorName = vendorName;
        this.description = description;
        this.category = category;
        this.anomaly = anomaly;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public BigDecimal getAmount() { return amount; }
    public String getVendorName() { return vendorName; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public boolean isAnomaly() { return anomaly; }

    public void setId(Long id) { this.id = id; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setVendorName(String vendorName) { this.vendorName = vendorName; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setAnomaly(boolean anomaly) { this.anomaly = anomaly; }
}
