package com.ares.shop.model;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Phân bổ tồn kho theo kho (tổng luôn khớp Product.stock; kho chính HCM nhận phần chênh lệch). */
@Entity @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"productId", "warehouse"}))
public class WarehouseStock {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long productId; public String warehouse; public Integer qty = 0;
}
