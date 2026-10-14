package com.ares.shop.model;

import jakarta.persistence.*;

@Entity
public class Brand {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String name;
    public String country;
    public String logoUrl;
    @Column(length = 1000) public String description;
}
