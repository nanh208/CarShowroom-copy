package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface PushSubRepo extends JpaRepository<PushSub, Long> { Optional<PushSub> findByEndpoint(String e); List<PushSub> findByUserId(Long u); }
