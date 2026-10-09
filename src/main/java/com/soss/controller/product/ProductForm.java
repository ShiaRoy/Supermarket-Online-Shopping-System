package com.soss.controller.product;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProductForm {
    @NotBlank @Size(max = 120)
    private String name;
    @NotBlank @Size(max = 80)
    private String category;
    @NotBlank @Size(max = 2000)
    private String description;
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2)
    private BigDecimal price;
    @NotBlank @Pattern(regexp = "basket|milk|bread|chicken|juice|drink")
    private String icon = "basket";
    private boolean active = true;
    @NotNull @Positive
    private Long storeId;
    @NotNull @Min(0) @Max(1000000)
    private Integer quantity = 0;
    @NotNull @Min(0) @Max(1000000)
    private Integer lowStockThreshold = 10;
    @NotBlank @Size(max = 250)
    private String stockReason = "Stock adjustment";
    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer threshold) { this.lowStockThreshold = threshold; }
    public String getStockReason() { return stockReason; }
    public void setStockReason(String stockReason) { this.stockReason = stockReason; }
    private Long productVersion;
    private Long inventoryVersion;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Long getProductVersion() { return productVersion; }
    public void setProductVersion(Long productVersion) { this.productVersion = productVersion; }
    public Long getInventoryVersion() { return inventoryVersion; }
    public void setInventoryVersion(Long inventoryVersion) { this.inventoryVersion = inventoryVersion; }
}
