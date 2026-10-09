package com.soss.service.accounts;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
// send a password reset link using a smtp server.
public class PasswordResetMailer {
    private final ObjectProvider<JavaMailSender> senders;
    private final String from;
    private final String baseUrl;
    public PasswordResetMailer(ObjectProvider<JavaMailSender> senders,
            @Value("${accounts.reset.mail-from:}") String from,
            @Value("${accounts.reset.base-url:http://localhost:8080}") String baseUrl) {
        this.senders = senders; this.from = from; this.baseUrl = baseUrl.replaceAll("/+$", "");
    }
    // build and send password expiry link.
    public void send(String email, String token) {
        JavaMailSender sender = senders.getIfAvailable();
        if (sender == null || from.isBlank()) throw new IllegalStateException("Password reset email is not configured.");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email); message.setSubject("Bula Basket password reset");
        message.setText("Open this link to reset your password. It expires in 15 minutes and can be used once.\n\n"
            + baseUrl + "/reset-password?token=" + token
            + "\n\nIf you did not request this, you can ignore this message.");
        sender.send(message);
    }
}
