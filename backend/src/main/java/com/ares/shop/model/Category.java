package com.ares.shop.model;

import jakarta.persistence.*;

/** Danh mục: nhóm TOY (Xe Model, Xe RC, Xe Trưng Bày, Lắp ráp) hoặc ACCESSORY (Pin, Khiển RC, Bánh xe RC...). */
@Entity
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true) public String code;
    public String name;
    public String grp = "TOY";
    public String image;
    public int sortOrder;
}
