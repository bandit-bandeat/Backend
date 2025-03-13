package com.example.post.repository;

import com.example.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByKind(String kind, Pageable pageable);

    long countByKind(String kind);

    Page<Post> findByEmailContaining(Pageable pageable, String keyword);
    
    long countByEmailContaining(String keyword);

    Page<Post> findByTitleContaining(Pageable pageable, String keyword);

    long countByTitleContaining(String keyword);

    Page<Post> findByContentContaining(Pageable pageable, String keyword);

    long countByContentContaining(String keyword);

    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    Page<Post> findByTitleOrContentContaining(Pageable pageable, String keyword);

    @Query("SELECT count(*) FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    long countByTitleOrContentContaining(String keyword);
}
