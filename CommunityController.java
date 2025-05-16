package com.imagify.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.imagify.dto.CommunityDTO;
import com.imagify.entity.Community;
import com.imagify.entity.User;
import com.imagify.repo.UserRepository;
import com.imagify.security.JwtService;
import com.imagify.service.CommunityService;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {
    private final CommunityService communityService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Autowired
    public CommunityController(CommunityService communityService, JwtService jwtService, UserRepository userRepository) {
        this.communityService = communityService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Community> createCommunity(@RequestHeader("Authorization") String authHeader, @RequestBody CommunityDTO communityDTO) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Community community = new Community();
        community.setName(communityDTO.getName());
        community.setDescription(communityDTO.getDescription());
        community.setCreatedBy(user.getId());
        community.getMembers().add(user.getId());

        Community created = communityService.createCommunity(community);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Community>> getAllCommunities() {
        return ResponseEntity.ok(communityService.getAllCommunities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Community> getCommunityById(@PathVariable String id) {
        Community community = communityService.getCommunityById(id);
        if (community != null) {
            return ResponseEntity.ok(community);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> joinCommunity(@RequestHeader("Authorization") String authHeader, @PathVariable String id) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        boolean joined = communityService.joinCommunity(id, user.getId());
        if (joined) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveCommunity(@RequestHeader("Authorization") String authHeader, @PathVariable String id) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        boolean left = communityService.leaveCommunity(id, user.getId());
        if (left) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<Community>> getMyCommunities(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        List<Community> communities = communityService.getCommunitiesByUser(user.getId());
        return ResponseEntity.ok(communities);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Community> updateCommunity(@PathVariable String id, @RequestBody CommunityDTO communityDTO) {
        Community updated = new Community();
        updated.setName(communityDTO.getName());
        updated.setDescription(communityDTO.getDescription());
        Community result = communityService.updateCommunity(id, updated);
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommunity(@PathVariable String id) {
        if (communityService.deleteCommunity(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
