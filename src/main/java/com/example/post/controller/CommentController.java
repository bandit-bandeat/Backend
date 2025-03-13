package com.example.post.controller;

import com.example.post.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comment")
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/write")
    public ResponseEntity<?> write(@RequestHeader("Authorization") String token, long postId, String content) {
        return commentService.write(token,postId,content);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestHeader("Authorization") String token, long commentId) {
        return commentService.delete(token,commentId);
    }

    @PostMapping("/like")
    public ResponseEntity<?> like(@RequestHeader("Authorization") String token, long commentId) {
        return commentService.like(token,commentId);
    }

}
