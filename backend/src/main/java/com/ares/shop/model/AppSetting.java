package com.ares.shop.model;
import jakarta.persistence.*;
/** Cấu hình dạng khoá–giá trị (VD: ma trận phân quyền nhân viên). */
@Entity
public class AppSetting {
    @Id public String k;
    @Column(length = 8000) public String v;
}
