package com.soss.model.accounts;

import com.soss.model.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="saved_addresses")
// Stores a named delivery address and whether it is the account default.
public class SavedAddress {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User owner;
    public Long getId() { return id; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    @Column(nullable=false,length=40) private String label;
    public String getLabel() { return label; }
    public void setLabel(String value) { label = value; }
    @Column(nullable=false,length=255) private String address;
    public String getAddress() { return address; }
    public void setAddress(String value) { address = value; }
    @Column(nullable=false) private boolean defaultAddress = false;
    public boolean getDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(boolean value) { defaultAddress = value; }
}
