package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @JsonIgnore @ManyToOne(optional = false) public User user;
    public String label;        // Nhà riêng, Công ty...
    public String receiverName;
    public String phone;
    public String line;         // số nhà, đường
    public String ward;
    public String district;
    public String city;
    public boolean isDefault;
}
