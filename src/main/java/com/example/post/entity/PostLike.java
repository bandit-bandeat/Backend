package com.example.post.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="post_like")
public class PostLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String email;
    private long postId;
}
