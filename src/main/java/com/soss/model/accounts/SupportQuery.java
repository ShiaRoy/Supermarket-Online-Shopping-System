package com.soss.model.accounts;

import com.soss.model.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="support_queries")
// Stores a customer support message, its latest reply, and its status.
public class SupportQuery {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User owner;
    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    @Column(nullable=false,length=120) private String subject;
    public String getSubject() { return subject; }
    public void setSubject(String value) { subject = value; }
    @Column(nullable=false,length=2000) private String message;
    public String getMessage() { return message; }
    public void setMessage(String value) { message = value; }
    @Column(length=2000) private String reply;
    public String getReply() { return reply; }
    public void setReply(String value) { reply = value; }
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SupportStatus status = SupportStatus.OPEN;
    public SupportStatus getStatus() { return status; }
    public void setStatus(SupportStatus value) { status = value; }
    @Column(nullable=false) private Instant createdAt = Instant.now();
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    @Column(nullable=false) private Instant updatedAt = Instant.now();
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
