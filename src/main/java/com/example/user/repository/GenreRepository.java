package com.example.user.repository;

import com.example.user.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    @Query("SELECT g.genre FROM Genre g JOIN PreGenre pg ON g.genreId = pg.genreId WHERE pg.email = :email")
    List<String> findByEmail(@Param("email") String email);

    Genre findByGenre(String preGenre);
}
