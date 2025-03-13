package com.example.user.repository;

import com.example.user.entity.AiTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AiTableRepository extends JpaRepository<AiTable, Long> {
    @Modifying
    @Transactional
    @Query("UPDATE AiTable a SET a.changeCnt = 0,a.recommandCnt =0, a.imageCnt=0, a.melodyCnt=0")
    void resetCnt();
}
