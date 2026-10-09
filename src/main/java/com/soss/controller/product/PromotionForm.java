package com.soss.controller.product;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PromotionForm {
    @NotNull @Positive private Long productId;
    private Long version;
    @NotBlank @Size(max = 100) private String name;
    @NotNull @DecimalMin("0.01") @DecimalMax("99") @Digits(integer = 2, fraction = 2)
    private BigDecimal percentOff;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate startsOn;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate endsOn;
    private boolean enabled = true;
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPercentOff() { return percentOff; }
    public void setPercentOff(BigDecimal percentOff) { this.percentOff = percentOff; }
    public LocalDate getStartsOn() { return startsOn; }
    public void setStartsOn(LocalDate startsOn) { this.startsOn = startsOn; }
    public LocalDate getEndsOn() { return endsOn; }
    public void setEndsOn(LocalDate endsOn) { this.endsOn = endsOn; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
