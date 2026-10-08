package com.soss.model.accounts;

import com.soss.model.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="password_reset_tokens")
// Stores the hash and expiry time of an email reset token, linked to its account.
public class PasswordResetToken {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User owner;
    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    @Column(nullable=false,unique=true,length=64) private String tokenHash;
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String value) { tokenHash = value; }
    @Column(nullable=false) private Instant createdAt = Instant.now();
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    @Column(nullable=false) private Instant expiresAt;
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { expiresAt = value; }
}
