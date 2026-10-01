package com.soss.model;

import jakarta.persistence.*;

@Entity
@Table(name = "store")
public class Store {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    protected Store() {}
    public Store(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
}
