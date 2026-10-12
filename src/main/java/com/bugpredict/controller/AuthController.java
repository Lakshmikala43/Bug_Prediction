package com.bugpredict.controller;

import com.bugpredict.config.SecurityConfig;
import com.bugpredict.model.RoleName;
import com.bugpredict.model.User;
import com.bugpredict.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityConfig securityConfig;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, SecurityConfig securityConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityConfig = securityConfig;
    }

    public record RegisterRequest(String username, String email, String password, String fullName) {}
    public record LoginRequest(String username, String password) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is already taken"));
        }
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already registered"));
        }

        User user = new User(req.username(), req.email(), passwordEncoder.encode(req.password()), req.fullName());
        user.setRoles(Set.of(RoleName.ROLE_DEVELOPER));
        userRepository.save(user);

        String token = securityConfig.generateToken(user.getUsername(), user.getRoles());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "roles", user.getRoles()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        User user = userRepository.findByUsername(req.username()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }

        String token = securityConfig.generateToken(user.getUsername(), user.getRoles());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", user.getUsername(),
                "roles", user.getRoles()
        ));
    }
}
