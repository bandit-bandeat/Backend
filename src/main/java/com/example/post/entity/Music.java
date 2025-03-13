package com.example.post.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="music")
@Data
public class Music {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long musicId;
    private String title;
    private String singer;
    private String image;
    private String youtube;

}
