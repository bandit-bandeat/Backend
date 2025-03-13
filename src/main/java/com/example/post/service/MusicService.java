package com.example.post.service;

import com.example.post.dto.MusicDto;
import com.example.post.entity.Genre;
import com.example.post.entity.Music;
import com.example.post.repository.GenreRepository;
import com.example.post.repository.MusicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MusicService {
    private final MusicRepository musicRepository;
    private final GenreRepository genreRepository;


    public ResponseEntity<?> getAll() {
        List<Music> musics = musicRepository.findAll();
        List<MusicDto> musicDtos = new ArrayList<>();

        for (Music music : musics) {
            MusicDto musicDto = new MusicDto();
            musicDto.setMusicId(music.getMusicId());
            musicDto.setTitle(music.getTitle());
            musicDto.setImage(music.getImage());
            musicDto.setSinger(music.getSinger());
            musicDto.setYoutube(music.getYoutube());

            List<String> genres = genreRepository.findByMusicId(music.getMusicId());
            musicDto.setGenres(genres);

            musicDtos.add(musicDto);
        }

        return ResponseEntity.ok(musicDtos);
    }
}
