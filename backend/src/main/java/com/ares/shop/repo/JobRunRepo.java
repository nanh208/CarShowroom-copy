package com.ares.shop.repo;
import com.ares.shop.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface JobRunRepo extends JpaRepository<JobRun, Long> { List<JobRun> findTop100ByOrderByIdDesc(); }
