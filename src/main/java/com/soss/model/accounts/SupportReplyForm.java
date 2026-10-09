package com.soss.model.accounts;

import jakarta.validation.constraints.*;

// Validates the reply and status submitted by staff or administrators.
public class SupportReplyForm {
    @NotBlank @Size(max=2000) private String reply;
    @NotNull private SupportStatus status;
    public String getReply() { return reply; }
    public void setReply(String value) { reply = value == null ? null : value.trim(); }
    public SupportStatus getStatus() { return status; }
    public void setStatus(SupportStatus value) { status = value; }
}
