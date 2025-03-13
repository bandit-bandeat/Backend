package com.example.post.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="music_genre")
public class MusicGenre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long genreId;
    private long musicId;
}
