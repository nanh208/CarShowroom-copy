package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface UserRepo extends JpaRepository<User, Long> { Optional<User> findByEmailIgnoreCase(String e); boolean existsByEmailIgnoreCase(String e); Optional<User> findFirstByPhone(String phone); }
