package com.ares.shop.repo;
import com.ares.shop.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface UserPrefRepo extends JpaRepository<UserPref, Long> { List<UserPref> findByUserId(Long u); Optional<UserPref> findByUserIdAndK(Long u, String k); }
