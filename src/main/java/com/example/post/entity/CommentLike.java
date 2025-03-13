package com.example.post.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="cmtLike")
public class CommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long cmtId;
    private String email;
}
