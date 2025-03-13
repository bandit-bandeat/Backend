package com.example.user.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name="chat")
@Data
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String sEmail;
    private String rEmail;
    private LocalDateTime created;
    private String content;

    @PrePersist
    protected void onCreate() {
        this.created = LocalDateTime.now(); // 저장할 때 자동으로 현재 시간 설정
    }
}
