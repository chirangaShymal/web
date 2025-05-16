package com.imagify.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imagify.dto.AuthRequest;
import com.imagify.dto.AuthResponse;
import com.imagify.dto.GoogleAuthRequest;
import com.imagify.entity.User;
import com.imagify.repo.UserRepository;
import com.imagify.security.JwtService;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName()); // Set name from request
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = jwtService.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    // Google signup and login use the same logic: verify token, create user if not exists, return JWT
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleSignup(@RequestBody GoogleAuthRequest request) {
        return handleGoogleAuth(request);
    }

    @PostMapping("/google-login")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleAuthRequest request) {
        return handleGoogleAuth(request);
    }

    // Shared logic for Google signup/login
    private ResponseEntity<AuthResponse> handleGoogleAuth(GoogleAuthRequest request) {
        try {
            // 1. Verify the Google ID token
            String idToken = request.getCredential();
            if (!StringUtils.hasText(idToken)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            String tokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            RestTemplate restTemplate = new RestTemplate();
            String result = restTemplate.getForObject(tokenInfoUrl, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(result);
            String email = json.get("email").asText();
            String name = json.has("name") ? json.get("name").asText() : email;
            // 2. Check if user exists
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setName(name);
                user.setPassword(""); // No password for Google users
                userRepository.save(user);
            }
            // 3. Issue JWT
            String token = jwtService.generateToken(user.getEmail());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // --- Google OAuth2 Success Handler ---
    @GetMapping("/oauth2/success")
    public void oauth2Success(
            @AuthenticationPrincipal OidcUser oidcUser,
            HttpServletResponse response
    ) throws java.io.IOException {
        if (oidcUser == null) {
            response.sendRedirect("http://localhost:3000/login?error=oauth");
            return;
        }
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        // Check if user exists, else create
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            // No password for OAuth users
            return userRepository.save(newUser);
        });
        String token = jwtService.generateToken(user.getEmail());
        // Redirect to frontend with token as query param
        response.sendRedirect("http://localhost:3000/?token=" + token);
    }
}
