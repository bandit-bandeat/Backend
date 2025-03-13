package com.example.post.repository;

import com.example.post.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CmtLikeRepository extends JpaRepository<CommentLike, Long> {
    CommentLike findByCmtIdAndEmail(long commentId, String email);
}
