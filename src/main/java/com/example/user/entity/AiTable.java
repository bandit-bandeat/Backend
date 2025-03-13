package com.example.user.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="ai_table")
public class AiTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String email;
    private long changeCnt;
    private long recommandCnt;
    private long melodyCnt;
    private long imageCnt;
}
