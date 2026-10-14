package com.ares.shop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false) public String email;
    @JsonIgnore public String password;
    public String fullName;
    public String phone;
    public String avatarUrl;
    @Enumerated(EnumType.STRING) public Enums.Role role = Enums.Role.USER;
    public String provider = "LOCAL";
    public boolean locked = false;
    public LocalDateTime createdAt = LocalDateTime.now();
}
