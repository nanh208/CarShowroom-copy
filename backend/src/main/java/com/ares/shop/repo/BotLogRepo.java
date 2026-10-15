package com.ares.shop.repo;
import com.ares.shop.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface BotLogRepo extends JpaRepository<BotLog, Long> {
    List<BotLog> findTop100BySessionOrderByIdAsc(String s);
    List<BotLog> findTop1000ByOrderByIdDesc();
}
