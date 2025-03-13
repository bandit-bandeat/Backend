package com.example.user.repository;

import com.example.user.entity.AiChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AiChangeRepository extends JpaRepository<AiChange, Long> {
    @Modifying
    @Transactional
    @Query("UPDATE AiChange c SET c.cnt = 0")
    void resetCnt();
}
