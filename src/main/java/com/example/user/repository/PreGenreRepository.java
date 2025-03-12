package com.example.user.repository;

import com.example.user.entity.PreGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PreGenreRepository extends JpaRepository<PreGenre, Long> {
    void deleteByEmail(String email);
}
