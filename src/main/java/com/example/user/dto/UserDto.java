package com.example.user.dto;

import lombok.Data;

@Data
public class UserDto {
    private String email;
    private String password;
    private String nickname;
    private String birth;
    private String role;
    private int membership;
}
