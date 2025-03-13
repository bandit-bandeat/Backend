package com.example.post.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Table(name="users")
@Entity
public class User {
    @Id
    private String email;
    private String password;
    private String nickname;
    private String birth;
    private String role;
}
