package com.example.user.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "genre")
public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long genreId;
    private String genre;
}
