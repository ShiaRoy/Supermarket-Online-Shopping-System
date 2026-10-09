package com.soss.service.accounts;

import com.soss.model.User;
import com.soss.model.accounts.*;
import com.soss.repository.UserRepository;
import com.soss.repository.accounts.PasswordResetTokenRepository;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
//change password and manage all password reset, expiry links.
public class PasswordService {
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final PasswordResetMailer mailer;
    private final Validator validator;
    private final SecureRandom random = new SecureRandom();
    public PasswordService(UserRepository users, PasswordResetTokenRepository tokens,
                           PasswordEncoder encoder, PasswordResetMailer mailer, Validator validator) {
        this.users = users; this.tokens = tokens; this.encoder = encoder; this.mailer = mailer; this.validator = validator;
    }
    private void validate(PasswordForm form) {
        if (!validator.validate(form).isEmpty()) throw new IllegalArgumentException("Check your password and confirmation.");
    }
    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private Optional<PasswordResetToken> validToken(String raw) {
        if (raw == null || !raw.matches("[a-f0-9]{64}")) return Optional.empty();
        return tokens.findByTokenHash(hash(raw)).filter(t -> t.getExpiresAt().isAfter(Instant.now()))
            .filter(t -> tokens.findFirstByOwnerIdOrderByCreatedAtDesc(t.getOwner().getId())
                .map(latest -> latest.getId().equals(t.getId())).orElse(false));
    }
    @Transactional(readOnly=true)
    public boolean isValidToken(String raw) { return validToken(raw).isPresent(); }

    @Transactional
    public void requestReset(String email) {
        Optional<User> found = users.lockByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (found.isEmpty()) return;
        User user = found.get(); Instant now = Instant.now();
        if (tokens.findFirstByOwnerIdOrderByCreatedAtDesc(user.getId())
            .filter(t -> t.getCreatedAt().isAfter(now.minusSeconds(60))).isPresent()) return;
        if (tokens.countByOwnerIdAndCreatedAtAfter(user.getId(), now.minusSeconds(3600)) >= 5) return;
        byte[] bytes = new byte[32]; random.nextBytes(bytes); String raw = HexFormat.of().formatHex(bytes);
        PasswordResetToken token = new PasswordResetToken(); token.setOwner(user);
        token.setTokenHash(hash(raw)); token.setCreatedAt(now); token.setExpiresAt(now.plusSeconds(900));
        tokens.saveAndFlush(token);
        // A mail failure rolls back token creation; raw tokens are never stored or logged.
        mailer.send(user.getEmail(), raw);
    }
    @Transactional
    // Checks the current password before hashing and saving the replacement.
    public void changePassword(String email, PasswordForm form) {
        validate(form);
        User user = users.lockByEmail(email).orElseThrow(() -> new IllegalArgumentException("Account unavailable."));
        if (form.getCurrentPassword() == null || !encoder.matches(form.getCurrentPassword(), user.getPasswordHash()))
            throw new IllegalArgumentException("Your current password is incorrect.");
        if (encoder.matches(form.getPassword(), user.getPasswordHash()))
            throw new IllegalArgumentException("Choose a different password.");
        user.setPasswordHash(encoder.encode(form.getPassword())); tokens.deleteByOwnerId(user.getId());
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    // Validates the reset token again under the account lock, changes the password, and removes reset tokens.
    public String resetPassword(String raw, PasswordForm form) {
        validate(form);
        PasswordResetToken candidate = validToken(raw)
            .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or expired. Request a new link."));
        User user = users.lockByEmail(candidate.getOwner().getEmail())
            .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or expired."));
        // Recheck after acquiring the user lock, so simultaneous submissions cannot reuse a token.
        validToken(raw).orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or expired."));
        user.setPasswordHash(encoder.encode(form.getPassword())); tokens.deleteByOwnerId(user.getId());
        return user.getEmail();
    }
}
