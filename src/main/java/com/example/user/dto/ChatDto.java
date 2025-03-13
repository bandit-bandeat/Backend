package com.example.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ChatDto {
    private long id;
    @JsonProperty("sEmail")
    private String sEmail;
    @JsonProperty("rEmail")
    private String rEmail;
    private LocalDateTime created;
    private String content;
}
