package com.ares.shop.repo;
import com.ares.shop.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ChatIntentRepo extends JpaRepository<ChatIntent, Long> { List<ChatIntent> findAllByOrderBySortOrderAscIdAsc(); }
