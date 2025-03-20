package com.example.post.dto;

import lombok.Data;

@Data
public class PostDto {
    private long postId;
    private String email;
    private String title;
    private String content;
    private String created;
    private String kind;
    private long cnt;
    private long heart;
    private int isFile;

    private int isLike;
}
