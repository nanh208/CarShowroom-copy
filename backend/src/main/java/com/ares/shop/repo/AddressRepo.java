package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface AddressRepo extends JpaRepository<Address, Long> { List<Address> findByUserIdOrderByIdDesc(Long uid); }
