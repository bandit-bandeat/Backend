package com.example.post.controller;

import com.example.post.entity.Post;
import com.example.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {
    private final PostService postService;

    @PostMapping("/write")
    public ResponseEntity<?> write(String title, String content, String kind,
                                   @RequestHeader("Authorization") String token,
                                   List<MultipartFile> files) {
        return postService.write(title,content,kind,token,files);
    }

    @GetMapping("/get/all")
    public ResponseEntity<?> getAll(int size, int page) {
        return postService.getAll(size,page);
    }

    @GetMapping("/get/kind")
    public ResponseEntity<?> getKind(int size, int page, String kind) {
        return postService.getKind(size,page,kind);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getDetail(@PathVariable long postId, String email) {
        return postService.getDetail(postId, email);
    }

    @PostMapping("/update")
    public ResponseEntity<?> update( long postId, String content,
                                    @RequestHeader("Authorization") String token,
                                    List<MultipartFile> files) {
        return postService.update(postId,content,token,files);
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(String category, String keyword, int page, int size) {
        return postService.search(category,keyword,page,size);
    }

    @PostMapping("/delete/{postId}")
    public ResponseEntity<?> delete(@RequestHeader("Authorization") String token,@PathVariable long postId) {
        return postService.delete(token,postId);
    }

    @PostMapping("/like")
    public ResponseEntity<?> like(@RequestHeader("Authorization") String token,long postId) {
        return postService.like(token, postId);
    }
}
