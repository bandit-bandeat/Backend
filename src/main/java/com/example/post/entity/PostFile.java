package com.example.post.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="post_file")
public class PostFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long postId;
    private String fileName;
}
