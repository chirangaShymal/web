package com.imagify.controller;

import com.mongodb.client.gridfs.model.GridFSFile;
import com.imagify.dto.PostDTO;
import com.imagify.entity.Post;
import com.imagify.repo.UserRepository;
import com.imagify.security.JwtService;
import com.imagify.service.FileStorageService;
import com.imagify.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostDTO> createPost(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) List<MultipartFile> images,
            @RequestParam(required = false) MultipartFile video) throws IOException {

        String userId = getUserIdFromAuthHeader(authHeader);
        Post post = postService.createPost(userId, content, images, video);
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.convertToDTO(post));
    }

    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        List<Post> posts = postService.getAllPosts();
        List<PostDTO> postDTOs = posts.stream()
                .map(postService::convertToDTO)
                .toList();
        return ResponseEntity.ok(postDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable String id) {
        Post post = postService.getPostById(id);
        return ResponseEntity.ok(postService.convertToDTO(post));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getPostsByUser(@PathVariable String userId) {
        List<Post> posts = postService.getPostsByUserId(userId);
        List<PostDTO> postDTOs = posts.stream()
                .map(postService::convertToDTO)
                .toList();
        return ResponseEntity.ok(postDTOs);
    }

    @GetMapping("/me")
    public ResponseEntity<List<PostDTO>> getMyPosts(@RequestHeader("Authorization") String authHeader) {
        String userId = getUserIdFromAuthHeader(authHeader);
        return getPostsByUser(userId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PostDTO> updatePost(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String id,
            @RequestParam String content) {

        String userId = getUserIdFromAuthHeader(authHeader);
        Post post = postService.getPostById(id);

        if (!post.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Post updatedPost = postService.updatePost(id, content);
        return ResponseEntity.ok(postService.convertToDTO(updatedPost));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String id) {

        String userId = getUserIdFromAuthHeader(authHeader);
        Post post = postService.getPostById(id);

        if (!post.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<PostDTO> likePost(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String postId) {

        String userId = getUserIdFromAuthHeader(authHeader);
        Post post = postService.likePost(postId, userId);
        return ResponseEntity.ok(postService.convertToDTO(post));
    }

    @PostMapping("/{postId}/unlike")
    public ResponseEntity<PostDTO> unlikePost(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String postId) {

        String userId = getUserIdFromAuthHeader(authHeader);
        Post post = postService.unlikePost(postId, userId);
        return ResponseEntity.ok(postService.convertToDTO(post));
    }

    @GetMapping("/media/{fileId}")
    public ResponseEntity<byte[]> getMedia(@PathVariable String fileId) {
        try {
            GridFSFile file = fileStorageService.getGridFsFile(fileId);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }

            GridFsResource resource = fileStorageService.getGridFsResource(file);
            InputStream inputStream = resource.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[4096];

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            byte[] fileBytes = buffer.toByteArray();

            String contentType = resource.getContentType();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                    .body(fileBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getUserIdFromAuthHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid authorization header");
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}