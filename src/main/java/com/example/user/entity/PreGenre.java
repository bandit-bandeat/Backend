package com.example.user.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "preGenre")
public class PreGenre {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private String email;
    private long genreId;
}
