package com.soss.model.accounts;

import jakarta.validation.constraints.*;

// Validates the subject and message of a customer support query.
public class SupportForm {
    @NotBlank @Size(max=120) private String subject;
    @NotBlank @Size(max=2000) private String message;
    public String getSubject() { return subject; }
    public void setSubject(String value) { subject = value == null ? null : value.trim(); }
    public String getMessage() { return message; }
    public void setMessage(String value) { message = value == null ? null : value.trim(); }
}
