package com.example.post.dto;

import lombok.Data;

@Data
public class CmtDto {
    private long cmtId;
    private long postId;
    private String email;
    private String nickname;
    private String content;
    private long heart;

    private int isLike;
}
