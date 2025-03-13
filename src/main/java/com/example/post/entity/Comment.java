package com.example.post.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="cmt")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long cmtId;
    private long postId;
    private String email;
    private String content;
    private long heart;
}
