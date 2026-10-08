package com.soss.model.accounts;

import jakarta.validation.constraints.*;

// Validates the email address supplied when requesting a password reset.
public class ForgotPasswordForm {
    @NotBlank @Email @Size(max=254) private String email;
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value == null ? null : value.trim(); }
}
