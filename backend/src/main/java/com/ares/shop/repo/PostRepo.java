package com.ares.shop.repo;
import com.ares.shop.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PostRepo extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug); boolean existsBySlug(String slug);
    List<Post> findByStatusOrderByPublishedAtDesc(String status);
    List<Post> findAllByOrderByIdDesc();
}
