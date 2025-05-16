package com.imagify.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.imagify.dto.LearningPlanDTO;
import com.imagify.entity.LearningPlan;
import com.imagify.entity.User;
import com.imagify.repo.UserRepository;
import com.imagify.security.JwtService;
import com.imagify.service.LearningPlanService;

import java.util.List;

@RestController
@RequestMapping("/api/learningplans")
public class LearningPlanController {
    private final LearningPlanService learningPlanService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Autowired
    public LearningPlanController(LearningPlanService learningPlanService, JwtService jwtService, UserRepository userRepository) {
        this.learningPlanService = learningPlanService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<LearningPlan> createLearningPlan(@RequestHeader("Authorization") String authHeader, @RequestBody LearningPlanDTO learningPlanDTO) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        LearningPlan learningPlan = new LearningPlan();
        learningPlan.setName(learningPlanDTO.getName());
        learningPlan.setDescription(learningPlanDTO.getDescription());
        learningPlan.setTopics(learningPlanDTO.getTopics());
        learningPlan.setResources(learningPlanDTO.getResources());
        learningPlan.setUserId(user.getId());

        LearningPlan created = learningPlanService.createLearningPlan(learningPlan);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LearningPlan> getLearningPlanById(@PathVariable String id) {
        LearningPlan learningPlan = learningPlanService.getLearningPlanById(id);
        if (learningPlan != null) {
            return ResponseEntity.ok(learningPlan);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<LearningPlan> getLearningPlanByName(@PathVariable String name) {
        LearningPlan learningPlan = learningPlanService.getLearningPlanByName(name);
        if (learningPlan != null) {
            return ResponseEntity.ok(learningPlan);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<LearningPlan>> getAllLearningPlans() {
        return ResponseEntity.ok(learningPlanService.getAllLearningPlans());
    }

    @GetMapping("/my")
    public ResponseEntity<List<LearningPlan>> getMyLearningPlans(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractUsername(token);
        // Find user by email
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        List<LearningPlan> plans = learningPlanService.getLearningPlansByUserId(user.getId());
        return ResponseEntity.ok(plans);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LearningPlan> updateLearningPlan(@PathVariable String id, @RequestBody LearningPlanDTO learningPlanDTO) {
        LearningPlan updated = new LearningPlan();
        updated.setName(learningPlanDTO.getName());
        updated.setDescription(learningPlanDTO.getDescription());
        updated.setTopics(learningPlanDTO.getTopics());
        updated.setResources(learningPlanDTO.getResources());
        LearningPlan result = learningPlanService.updateLearningPlan(id, updated);
        if (result != null) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLearningPlan(@PathVariable String id) {
        if (learningPlanService.deleteLearningPlan(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
