package com.example.post.dto;

import lombok.Data;

import java.util.List;

@Data
public class MusicDto {
    private long musicId;
    private String title;
    private String singer;
    private String image;
    private String youtube;
    private List<String> genres;
}
