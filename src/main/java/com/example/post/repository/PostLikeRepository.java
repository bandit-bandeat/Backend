package com.example.post.repository;

import com.example.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    PostLike findByEmailAndPostId(String email, long postId);

    int countByEmailAndPostId(String email, long postId);
}
