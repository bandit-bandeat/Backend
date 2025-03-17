package com.example.user.controller;

import com.example.user.dto.JoinDto;
import com.example.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login (String email, String password, HttpServletResponse res) {
        return authService.login(email, password, res);
    }

    @PostMapping("/join")
    public ResponseEntity<?> join (@RequestBody JoinDto joinDto) {
        return authService.join(joinDto.getEmail(),joinDto.getPassword(), joinDto.getNickname(),
                joinDto.getBirth(), joinDto.getPreGenres());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout (HttpServletRequest req, HttpServletResponse res) {
        return authService.logout(req,res);
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue (String email, HttpServletResponse res) {
        return authService.reissue(email,res);
    }

    @PostMapping("/update")
    public ResponseEntity<?> update (@RequestHeader("Authorization") String token, String nickname, String birth,
                                     @RequestParam List<String> preGenres) {
        return authService.update(token,nickname,birth,preGenres);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete (@RequestHeader("Authorization") String token,String email) {
        return authService.delete(token, email);
    }

    @PostMapping("/membership")
    public ResponseEntity<?> membership (@RequestHeader("Authorization") String token) {
        return authService.membership(token);
    }

    // 테스트용 지우셈
    @GetMapping("/all")
    public ResponseEntity<?> all () {
        return authService.all();
    }

}
