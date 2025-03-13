package com.example.post.service;

import com.example.post.dto.CmtDto;
import com.example.post.dto.PostDto;
import com.example.post.entity.*;
import com.example.post.jwt.JwtUtil;
import com.example.post.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostFileRepository postFileRepository;
    private final JwtUtil jwtUtil;
    private final AwsS3Service awsS3Service;
    private final UserRepository userRepository;
    private final CmtRepository cmtRepository;

    public ResponseEntity<?> write(String title, String content, String kind, String token, List<MultipartFile> files) {
        String email = "";
        try{
            jwtUtil.isExpired(token);
            email = jwtUtil.getEmail(token);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("잘못된 액세스 토큰");
        }

        Post post = new Post();
        post.setTitle(title);
        post.setContent(content);
        post.setKind(kind);
        post.setEmail(email);

        if(files == null || files.isEmpty()){
            post.setIsFile(0);
            postRepository.save(post);
        }
        else{
            post.setIsFile(1);
            postRepository.save(post);

            for(MultipartFile file : files){
                try {
                    String url = awsS3Service.upload(file);

                    PostFile postFile = new PostFile();
                    postFile.setFileName(url);
                    postFile.setPostId(post.getPostId());
                    postFileRepository.save(postFile);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
        return ResponseEntity.ok().body("작성 완료");
    }

    public ResponseEntity<?> getAll(int size, int page) {
        Sort sortByIdDesc = Sort.by(Sort.Direction.DESC, "postId");
        Pageable pageable = PageRequest.of(page, size, sortByIdDesc);
        Page<Post> posts = postRepository.findAll(pageable);

        long total = postRepository.count();

        List<PostDto> postDtoList = new ArrayList<>();
        List<String> nicknameList = new ArrayList<>();
        for(Post post : posts){
            PostDto postDto = new PostDto();
            postDto.setPostId(post.getPostId());
            postDto.setTitle(post.getTitle());
            postDto.setContent(post.getContent());
            postDto.setKind(post.getKind());
            postDto.setEmail(post.getEmail());
            postDto.setCnt(post.getCnt());
            postDto.setCreated(post.getCreated());
            postDto.setHeart(post.getHeart());

            postDtoList.add(postDto);

            User user = userRepository.findById(post.getEmail()).orElse(null);
            nicknameList.add(user.getNickname());
        }
        return ResponseEntity.ok(Map.of(
                "post", postDtoList,
                "nickname", nicknameList,
                "total", total
        ));

    }

    public ResponseEntity<?> getKind(int size, int page, String kind) {
        Sort sortByIdDesc = Sort.by(Sort.Direction.DESC, "postId");
        Pageable pageable = PageRequest.of(page, size, sortByIdDesc);
        Page<Post> posts = postRepository.findByKind(kind,pageable);

        long total = postRepository.countByKind(kind);

        List<PostDto> postDtoList = new ArrayList<>();
        List<String> nicknameList = new ArrayList<>();
        for(Post post : posts){
            PostDto postDto = new PostDto();
            postDto.setPostId(post.getPostId());
            postDto.setTitle(post.getTitle());
            postDto.setContent(post.getContent());
            postDto.setKind(post.getKind());
            postDto.setEmail(post.getEmail());
            postDto.setCnt(post.getCnt());
            postDto.setCreated(post.getCreated());
            postDto.setHeart(post.getHeart());

            postDtoList.add(postDto);

            User user = userRepository.findById(post.getEmail()).orElse(null);
            nicknameList.add(user.getNickname());
        }
        return ResponseEntity.ok(Map.of(
                "post", postDtoList,
                "nickname", nicknameList,
                "total", total
        ));
    }

    public ResponseEntity<?> getDetail(long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if(post == null) return ResponseEntity.badRequest().body("게시글이 존재하지 않음");

        post.setCnt(post.getCnt() + 1);
        postRepository.save(post);

        PostDto postDto = new PostDto();
        postDto.setPostId(post.getPostId());
        postDto.setTitle(post.getTitle());
        postDto.setContent(post.getContent());
        postDto.setKind(post.getKind());
        postDto.setEmail(post.getEmail());
        postDto.setCnt(post.getCnt());
        postDto.setCreated(post.getCreated());
        postDto.setHeart(post.getHeart());

        String writer = userRepository.findById(post.getEmail()).orElse(null).getNickname();

        List<String> postFileList = new ArrayList<>();
        if(post.getIsFile() == 1){
            List<PostFile> postFiles = postFileRepository.findByPostId(post.getPostId());
            for(PostFile postFile : postFiles)
                postFileList.add(postFile.getFileName());

        }

        List<Comment> commentList = cmtRepository.findByPostId(postId);
        List<CmtDto> cmtDtoList = new ArrayList<>();

        for(Comment comment : commentList){
            CmtDto cmtDto = new CmtDto();
            cmtDto.setPostId(comment.getPostId());
            cmtDto.setHeart(comment.getHeart());
            cmtDto.setEmail(comment.getEmail());
            cmtDto.setContent(comment.getContent());
            cmtDto.setCmtId(comment.getCmtId());

            cmtDto.setNickname(userRepository.findById(comment.getEmail()).orElse(null).getNickname());

            cmtDtoList.add(cmtDto);
        }

        //댓글과 댓글 닉네임도 추가
        return ResponseEntity.ok(Map.of(
                "post", postDto,
                "postFile", postFileList,
                "writer", writer,
                "commentList", cmtDtoList
        ));
    }

    @Transactional
    public ResponseEntity<?> update(long postId, String content, String token, List<MultipartFile> files) {
        Post post = postRepository.findById(postId).orElse(null);
        if(post == null) return ResponseEntity.badRequest().body("존재하지 않는 게시글");

        String email = "";
        try{
            jwtUtil.isExpired(token);
            email = jwtUtil.getEmail(token);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(!email.equals(post.getEmail())) return ResponseEntity.badRequest().body("작성자만 수정 가능");

        post.setContent(content);
        postFileRepository.deleteByPostId(postId);
        if(files == null || files.isEmpty())
            post.setIsFile(0);
        else{
            post.setIsFile(1);
            for(MultipartFile file : files){
                try {
                    String url = awsS3Service.upload(file);

                    PostFile postFile = new PostFile();
                    postFile.setPostId(postId);
                    postFile.setFileName(url);
                    postFileRepository.save(postFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        postRepository.save(post);
        return ResponseEntity.ok("수정 완료");
    }

    public ResponseEntity<?> search(String category, String keyword, int page, int size) {
        List<PostDto> postDtoList = new ArrayList<>();
        List<String> nicknameList = new ArrayList<>();
        long total = 0;

        Sort sortByIdDesc = Sort.by(Sort.Direction.DESC, "postId");
        Pageable pageable = PageRequest.of(page, size, sortByIdDesc);

        Page<Post> postList = null;

        if(category.equals("작성자")){
            postList = postRepository.findByEmailContaining(pageable,keyword);
            total = postRepository.countByEmailContaining(keyword);
        }
        else if(category.equals("제목")){
            postList = postRepository.findByTitleContaining(pageable,keyword);
            total = postRepository.countByTitleContaining(keyword);
        }
        else if(category.equals("내용")){
            postList = postRepository.findByContentContaining(pageable,keyword);
            total = postRepository.countByContentContaining(keyword);
        }
        else if(category.equals("제목+내용")){
            postList = postRepository.findByTitleOrContentContaining(pageable,keyword);
            total = postRepository.countByTitleOrContentContaining(keyword);
        }

        for(Post post : postList){
            PostDto postDto = new PostDto();
            postDto.setPostId(post.getPostId());
            postDto.setTitle(post.getTitle());
            postDto.setContent(post.getContent());
            postDto.setKind(post.getKind());
            postDto.setEmail(post.getEmail());
            postDto.setCnt(post.getCnt());
            postDto.setCreated(post.getCreated());
            postDto.setHeart(post.getHeart());

            postDtoList.add(postDto);

            User user = userRepository.findById(post.getEmail()).orElse(null);
            nicknameList.add(user.getNickname());
        }

        return ResponseEntity.ok(Map.of(
                "post", postDtoList,
                "nickname", nicknameList,
                "total", total
        ));
    }

    @Transactional
    public ResponseEntity<?> delete(String token, long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if(post == null) return ResponseEntity.badRequest().body("존재하지 않는 게시글");

        String email = "";
        String role = "";
        try{
            jwtUtil.isExpired(token);
            email = jwtUtil.getEmail(token);
            role = jwtUtil.getRole(token);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        if(!email.equals(post.getEmail()) && !role.equals("ROLE_ADMIN"))
            return ResponseEntity.badRequest().body("삭제는 관리자와 작성자만 가능");
        postRepository.deleteById(postId);
        return ResponseEntity.ok("삭제 완료");
    }

    @Transactional
    public ResponseEntity<?> like(String token, long postId) {
        Post post = postRepository.findById(postId).orElse(null);
        if(post == null) return ResponseEntity.badRequest().body("존재하지 않는 게시글");

        String email = "";
        try{
            jwtUtil.isExpired(token);
            email = jwtUtil.getEmail(token);
        }catch (Exception e){
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        PostLike postLike = postLikeRepository.findByEmailAndPostId(email, postId);
        if(postLike == null){
            postLike = new PostLike();
            postLike.setPostId(postId);
            postLike.setEmail(email);
            postLikeRepository.save(postLike);

            post.setHeart(post.getHeart() + 1);
            return ResponseEntity.ok("좋아요 성공");
        }else{
            postLikeRepository.delete(postLike);
            post.setHeart(post.getHeart() - 1);
            return ResponseEntity.ok("좋아요 삭제");
        }
    }
}
