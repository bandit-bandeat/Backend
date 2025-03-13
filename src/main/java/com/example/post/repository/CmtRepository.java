package com.example.post.repository;

import com.example.post.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CmtRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostId(long postId);
}
