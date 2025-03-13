package com.example.post.service;

import com.example.post.entity.Comment;
import com.example.post.entity.CommentLike;
import com.example.post.entity.Post;
import com.example.post.entity.PostLike;
import com.example.post.jwt.JwtUtil;
import com.example.post.repository.CmtLikeRepository;
import com.example.post.repository.CmtRepository;
import com.example.post.repository.PostRepository;
import com.example.post.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CmtRepository cmtRepository;
    private final CmtLikeRepository cmtLikeRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PostRepository postRepository;

    public ResponseEntity<?> write(String token, long postId, String content) {
        Post post = postRepository.findById(postId).orElse(null);
        if(post == null) return ResponseEntity.badRequest().body("존재하지 않는 게시글");

        String email = "";
        try{
            jwtUtil.isExpired(token);
            email = jwtUtil.getEmail(token);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setPostId(postId);
        comment.setEmail(email);

        cmtRepository.save(comment);
        return ResponseEntity.ok("댓글 작성 성공");
    }

    @Transactional
    public ResponseEntity<?> delete(String token, long commentId) {
        Comment comment = cmtRepository.findById(commentId).orElse(null);
        if(comment == null) return ResponseEntity.badRequest().body("존재하지 않는 댓글");

        String email = "";
        String role = "";
        try{
            jwtUtil.isExpired(token);
            email = jwtUtil.getEmail(token);
            role = jwtUtil.getRole(token);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(!email.equals(comment.getEmail()) && !role.equals("ROLE_ADMIN"))
            return ResponseEntity.badRequest().body("삭제는 관리자와 작성자만 가능");

        cmtRepository.delete(comment);
        return ResponseEntity.ok("삭제 완료");
    }

    @Transactional
    public ResponseEntity<?> like(String token, long commentId) {
        Comment comment = cmtRepository.findById(commentId).orElse(null);
        if(comment == null) return ResponseEntity.badRequest().body("존재하지 않는 댓글");

        String email = "";
        String role = "";
        try{
            jwtUtil.isExpired(token);
            email = jwtUtil.getEmail(token);
            role = jwtUtil.getRole(token);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        CommentLike commentLike = cmtLikeRepository.findByCmtIdAndEmail(commentId,email);
        if(commentLike == null){
            commentLike = new CommentLike();
            commentLike.setCmtId(commentId);
            commentLike.setEmail(email);
            comment.setHeart(comment.getHeart() + 1);
            cmtLikeRepository.save(commentLike);
            return ResponseEntity.ok("좋아요 성공");
        }else{
            comment.setHeart(comment.getHeart() - 1);
            cmtLikeRepository.delete(commentLike);
            return ResponseEntity.ok("좋아요 삭제");
        }
    }
}
