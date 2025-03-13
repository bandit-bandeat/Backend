package com.example.user.service;

import com.example.user.dto.UserDto;
import com.example.user.entity.AiTable;
import com.example.user.entity.Genre;
import com.example.user.entity.PreGenre;
import com.example.user.entity.User;
import com.example.user.jwt.JwtUtil;
import com.example.user.repository.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshService refreshService;
    private final GenreRepository genreRepository;
    private final PreGenreRepository preGenreRepository;
    private final AiTableRepository aiTableRepository;


    public ResponseEntity<?> login(String email, String password, HttpServletResponse res) {
        User user = userRepository.findById(email).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body("등록되지 않은 ID");
        if(!bCryptPasswordEncoder.matches(password, user.getPassword()))
            return ResponseEntity.badRequest().body("비밀번호 불일치");

        String accessToken = jwtUtil.createToken(email, user.getRole(), "access");
        String refreshToken = jwtUtil.createToken(email, user.getRole(), "refresh");

        refreshService.removeRefreshToken(email);
        refreshService.setRefreshToken(email, refreshToken);

        res.addCookie(creatCookie("refresh", refreshToken)); //리프레시는 쿠키로 전송
        res.addHeader("AccessToken", accessToken);

        UserDto userDto = new UserDto();
        userDto.setEmail(email);
        userDto.setBirth(user.getBirth());
        userDto.setRole(user.getRole());
        userDto.setNickname(user.getNickname());
        userDto.setMembership(user.getMembership());

        List<String> preGenre = genreRepository.findByEmail(email);

        return ResponseEntity.ok(Map.of(
                "userDto" , userDto,
                "preGenre", preGenre
        ));
    }
    private Cookie creatCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setHttpOnly(true);
        return cookie;
    }

    public ResponseEntity<?> join(String email, String password, String nickname, String birth, List<String> preGenres) {
        User user = userRepository.findById(email).orElse(null);
        if(user != null)
            return ResponseEntity.badRequest().body("이미 존재하는 ID");

        user = new User();
        user.setEmail(email);
        user.setPassword(bCryptPasswordEncoder.encode(password));
        user.setNickname(nickname);
        user.setBirth(birth);
        user.setRole("ROLE_USER");

        userRepository.save(user);

        AiTable change = new AiTable();
        change.setEmail(user.getEmail());

        aiTableRepository.save(change);

        for(String preGenre : preGenres){
            Genre genre = genreRepository.findByGenre(preGenre);
            PreGenre preGenreEntity = new PreGenre();
            preGenreEntity.setEmail(email);
            preGenreEntity.setGenreId(genre.getGenreId());
            preGenreRepository.save(preGenreEntity);
        }
        return ResponseEntity.ok().body("회원가입 성공");
    }

    public ResponseEntity<?> logout(HttpServletRequest req, HttpServletResponse res) {
        String refresh = "";
        Cookie[] cookies = req.getCookies();
        for(Cookie cookie : cookies){
            if(cookie.getName().equals("refresh"))
                refresh = cookie.getValue();
        }
        if(refresh.isEmpty()) return ResponseEntity.badRequest().body("옳바르지 않은 리프레시 토큰");
        String email = "";
        try{
            email = jwtUtil.getEmail(refresh);
        }catch(Exception e){
            return ResponseEntity.badRequest().body("옳바르지 않은 리프레시 토큰");
        }

        refreshService.removeRefreshToken(email);
        res.addCookie(creatCookie("refresh", refresh));

        Cookie cookie = new Cookie("refresh", refresh);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        res.addCookie(cookie);
        return ResponseEntity.ok("로그아웃 성공");
    }

    public ResponseEntity<?> reissue(String email, HttpServletResponse res) {
        String refresh = refreshService.getRefreshToken(email);
        if(refresh == null) return ResponseEntity.badRequest().body("옳바르지 않은 리프레시 토큰");

        try{
            jwtUtil.isExpired(refresh);
        }catch(Exception e){
            return ResponseEntity.badRequest().body("옳바르지 않은 리프레시 토큰");
        }
        String role = jwtUtil.getRole(refresh);
        String accessToken = jwtUtil.createToken(email, role, "access");

        res.addHeader("AccessToken", accessToken);


        return ResponseEntity.ok("재발급 성공");
    }

    @Transactional
    public ResponseEntity<?> update(String token,String nickname, String birth, List<String> preGenres) {
        String email = "";
        try{
            jwtUtil.isExpired(token);
            email = jwtUtil.getEmail(token);
        }catch(Exception e){
            return ResponseEntity.badRequest().body("옳바르지 않은 액세스 토큰");
        }

        User user = userRepository.findById(email).orElse(null);
        if(user == null) return ResponseEntity.badRequest().body("옳바르지 않은 ID");

        if(nickname != null) user.setNickname(nickname);
        if(birth != null) user.setBirth(birth);
        userRepository.save(user);

        preGenreRepository.deleteByEmail(email);

        for(String preGenre : preGenres){
            Genre genre = genreRepository.findByGenre(preGenre);
            PreGenre preGenreEntity = new PreGenre();
            preGenreEntity.setEmail(email);
            preGenreEntity.setGenreId(genre.getGenreId());
            preGenreRepository.save(preGenreEntity);
        }
        return ResponseEntity.ok("업데이트 성공");
    }

    @Transactional
    public ResponseEntity<?> delete(String token, String email) {
        String temail = "";
        String role = "";
        try{
            jwtUtil.isExpired(token);
            temail = jwtUtil.getEmail(token);
            role = jwtUtil.getRole(token);
        }catch(Exception e){
            return ResponseEntity.badRequest().body("옳바르지 않은 액세스 토큰");
        }

        if(!temail.equals(email) && !role.equals("ROLE_ADMIN"))
            return ResponseEntity.badRequest().body("탈퇴는 본인 혹은 관리자만 가능");
        userRepository.deleteById(email);
        return ResponseEntity.ok("탈퇴 성공");
    }

    public ResponseEntity<?> membership(String token) {
        String email = "";
        try{
            jwtUtil.isExpired(token);
            email = jwtUtil.getEmail(token);
        }catch(Exception e){
            return ResponseEntity.badRequest().body("유효하지 않은 액세스 토큰");
        }

        User user = userRepository.findById(email).orElse(null);
        if(user == null) return ResponseEntity.badRequest().body("존재하지 않는 유저");

        user.setMembership(1);
        userRepository.save(user);
        return ResponseEntity.ok("멤버쉽 가입 성공");
    }
}
