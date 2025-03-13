package com.example.post.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name="post")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long postId;
    private String email;
    private String title;
    private String content;
    private String created;
    private String kind;
    private long cnt;
    private long heart;
    int isFile;

    @PrePersist
    public void prePersist() {
        // 현재 날짜를 "yyyy-MM-dd" 형식으로 설정
        this.created = LocalDate.now().toString();
    }
}
