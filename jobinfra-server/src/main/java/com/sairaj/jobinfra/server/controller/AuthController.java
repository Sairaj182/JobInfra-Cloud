package com.sairaj.jobinfra.server.controller;

import com.sairaj.jobinfra.server.controller.dto.AuthRequest;
import com.sairaj.jobinfra.server.controller.dto.AuthResponse;
import com.sairaj.jobinfra.server.domain.UserEntity;
import com.sairaj.jobinfra.server.repository.UserRepository;
import com.sairaj.jobinfra.server.security.JwtService;
import com.sairaj.jobinfra.server.service.AuditLogger;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration and login")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final AuditLogger auditLogger;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, AuthenticationManager authenticationManager,
                          UserDetailsService userDetailsService, AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/register")
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<AuthResponse>> register(@jakarta.validation.Valid @RequestBody AuthRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            auditLogger.logAuth("REGISTRATION", request.getUsername(), false, "Username already taken");
            return ResponseEntity.badRequest().body(com.sairaj.jobinfra.server.controller.dto.ApiResponse.error("USER_EXISTS", "Username already taken"));
        }
        UserEntity user = new UserEntity(request.getUsername(), passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        auditLogger.logAuth("REGISTRATION", request.getUsername(), true, "User registered successfully");

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(new AuthResponse(token)));
    }

    @PostMapping("/login")
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<AuthResponse>> login(@jakarta.validation.Valid @RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            auditLogger.logAuth("LOGIN", request.getUsername(), false, "Bad credentials");
            throw ex; // Re-throw to be handled by GlobalExceptionHandler or Spring Security
        } catch (Exception ex) {
            auditLogger.logAuth("LOGIN", request.getUsername(), false, ex.getMessage());
            throw ex;
        }

        auditLogger.logAuth("LOGIN", request.getUsername(), true, "Login successful");

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(new AuthResponse(token)));
    }
}
