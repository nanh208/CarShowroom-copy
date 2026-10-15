package com.ares.shop.model;
import jakarta.persistence.*;
@Entity @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"reviewId","userId"}))
public class ReviewVote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public Long reviewId; public Long userId; public boolean helpful = true;
}
