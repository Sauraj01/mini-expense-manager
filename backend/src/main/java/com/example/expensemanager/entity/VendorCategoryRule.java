package com.example.expensemanager.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vendor_category_rules", uniqueConstraints = @UniqueConstraint(columnNames = "vendorKeyword"))
public class VendorCategoryRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String vendorKeyword;

    @Column(nullable = false)
    private String category;

    public VendorCategoryRule() {}

    public VendorCategoryRule(Long id, String vendorKeyword, String category) {
        this.id = id;
        this.vendorKeyword = vendorKeyword;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getVendorKeyword() { return vendorKeyword; }
    public String getCategory() { return category; }

    public void setId(Long id) { this.id = id; }
    public void setVendorKeyword(String vendorKeyword) { this.vendorKeyword = vendorKeyword; }
    public void setCategory(String category) { this.category = category; }
}
