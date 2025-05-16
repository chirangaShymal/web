package com.imagify.controller;     //Standard controller package

import com.imagify.dto.CommentDTO;
import com.imagify.entity.Comment;
import com.imagify.security.JwtService;
import com.imagify.service.CommentService;
import com.imagify.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController   //Marks this as a controller that returns JSON (not views)
@RequestMapping("/api/comments")   //Base path for all endpoints in this controller
public class CommentController {

    //Dependencies
    @Autowired
    private CommentService commentService;  //Handles business logic for comments

    @Autowired
    private JwtService jwtService;  //Handles JWT token operations

    @Autowired
    private UserRepository userRepository;  //Database access for user entities

    @PostMapping    //Create Comment
    public ResponseEntity<?> createComment(
        @RequestHeader("Authorization") String authHeader,  //JWT token for authentication
        @RequestBody CommentRequest request) {    //CommentRequest with postId and content
    try {
        String userId = getUserIdFromAuthHeader(authHeader);
        Comment comment = commentService.createComment(request.getPostId(), userId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.convertToDTO(comment));
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .body(Map.of("error", e.getMessage()));
    }
}

    @GetMapping("/post/{postId}")   //Get Comments by Post ID
    public ResponseEntity<List<CommentDTO>> getCommentsByPostId(@PathVariable String postId) {
        List<Comment> comments = commentService.getCommentsByPostId(postId);
        List<CommentDTO> commentDTOs = comments.stream()
                .map(commentService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(commentDTOs);
    }

    @PutMapping("/{id}")   //Update Comment
    public ResponseEntity<CommentDTO> updateComment(
            @RequestHeader("Authorization") String authHeader,  //JWT token
            @PathVariable String id,  //Comment ID in path
            @RequestBody CommentRequest request) {   //Updated content in request body
        
        String userId = getUserIdFromAuthHeader(authHeader);
        Comment comment = commentService.updateComment(id, userId, request.getContent());
        return ResponseEntity.ok(commentService.convertToDTO(comment));
    }

    @DeleteMapping("/{id}")   //Delete Comment 
    public ResponseEntity<Void> deleteComment(
            @RequestHeader("Authorization") String authHeader,  //JWT token
            @PathVariable String id) {    //Comment ID in path
        
        String userId = getUserIdFromAuthHeader(authHeader);
        commentService.deleteComment(id, userId);
        return ResponseEntity.noContent().build();
    }

    //Helper Method
    private String getUserIdFromAuthHeader(String authHeader) {  //Extracts user ID from JWT token in Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid authorization header");
        }
        String token = authHeader.substring(7);  //Extracts the JWT token from the header
        String email = jwtService.extractUsername(token);   //Extracts email from token
        return userRepository.findByEmail(email) //Finds the user in the database by using this and returns the user Id
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }

    //Inner Class
    static class CommentRequest { //Simple DTO for receiving comment creation/update data
        private String postId;
        private String content;

        // Getters and setters
        public String getPostId() {
            return postId;
        }

        public void setPostId(String postId) {
            this.postId = postId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}