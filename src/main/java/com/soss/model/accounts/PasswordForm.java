package com.soss.model.accounts;

import jakarta.validation.constraints.*;

// Validates the new password, confirmation, and BCrypt byte-length limit.
public class PasswordForm {
     private String currentPassword;
    @NotBlank @Size(min=8, max=72, message="Use 8 to 72 characters.") private String password;
    @NotBlank @Size(max=72) private String confirmPassword;
    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String value) { currentPassword = value; }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String value) { confirmPassword = value; }
    @AssertTrue(message="Passwords must match.")
    // Checks that the new password and its confirmation match.
    public boolean isPasswordsMatch() { return password != null && password.equals(confirmPassword); }
    @AssertTrue(message="Password must fit within 72 UTF-8 bytes.")
    // Checks the password length in UTF-8 bytes before BCrypt hashing.
    public boolean isPasswordWithinLimit() { return password == null || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72; }
}
