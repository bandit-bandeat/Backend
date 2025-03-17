package com.example.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class JoinDto {
    private String email;
    private String password;
    private String nickname;
    private String birth;
    private List<String> preGenres;
}
