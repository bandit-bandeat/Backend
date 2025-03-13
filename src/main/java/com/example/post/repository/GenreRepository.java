package com.example.post.repository;

import com.example.post.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    @Query("SELECT g.genre FROM Genre g WHERE g.genreId IN (SELECT mg.genreId FROM MusicGenre mg WHERE mg.musicId = :musicId)")
    List<String> findByMusicId(long musicId);
}
