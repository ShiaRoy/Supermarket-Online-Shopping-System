package com.soss.model.accounts;

import jakarta.validation.constraints.*;

// Validates the label and delivery address entered in the saved-address form.
public class AddressForm {
    @NotBlank @Size(max=40) private String label;
    @NotBlank @Size(max=255) private String address;
    public String getLabel() { return label; }
    public void setLabel(String value) { label = value == null ? null : value.trim(); }
    public String getAddress() { return address; }
    public void setAddress(String value) { address = value == null ? null : value.trim(); }
}
