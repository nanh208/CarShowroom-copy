package com.ares.shop.repo;
import com.ares.shop.model.*; import org.springframework.data.jpa.repository.*; import java.util.*;
public interface QuestionRepo extends JpaRepository<Question, Long> { List<Question> findByProductIdAndVisibleTrueOrderByCreatedAtDesc(Long pid); List<Question> findAllByOrderByCreatedAtDesc(); }
