package com.sairaj.jobinfra.server.controller;

import com.sairaj.jobinfra.server.controller.dto.ApiKeyResponse;
import com.sairaj.jobinfra.server.domain.ApiKeyEntity;
import com.sairaj.jobinfra.server.domain.ProjectEntity;
import com.sairaj.jobinfra.server.domain.UserEntity;
import com.sairaj.jobinfra.server.repository.ApiKeyRepository;
import com.sairaj.jobinfra.server.repository.ProjectRepository;
import com.sairaj.jobinfra.server.repository.UserRepository;
import com.sairaj.jobinfra.server.service.AuditLogger;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/keys")
@Tag(name = "API Keys", description = "Manage API keys for projects")
public class ApiKeyController {

    private final ApiKeyRepository apiKeyRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    public ApiKeyController(ApiKeyRepository apiKeyRepository, ProjectRepository projectRepository,
                            UserRepository userRepository, PasswordEncoder passwordEncoder, AuditLogger auditLogger) {
        this.apiKeyRepository = apiKeyRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
    }

    private UserEntity getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<ApiKeyResponse>> createApiKey(@PathVariable Long projectId, @RequestBody ApiKeyResponse request) {
        UserEntity user = getCurrentUser();
        ProjectEntity project = projectRepository.findById(projectId).orElseThrow();
        if (!project.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(com.sairaj.jobinfra.server.controller.dto.ApiResponse.error("FORBIDDEN", "Not allowed"));
        }

        String rawKey = "ji_live_" + UUID.randomUUID().toString().replace("-", "");
        String keyHash = hashKey(rawKey);

        ApiKeyEntity apiKey = new ApiKeyEntity(keyHash, request.getDescription(), project);
        apiKeyRepository.save(apiKey);
        
        auditLogger.logApiKey("GENERATED", String.valueOf(project.getId()), String.valueOf(apiKey.getId()), user.getUsername());

        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(new ApiKeyResponse(apiKey.getId(), rawKey, apiKey.getDescription(), apiKey.getCreatedAt())));
    }

    private String hashKey(String rawKey) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    @GetMapping
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<List<ApiKeyResponse>>> getApiKeys(@PathVariable Long projectId) {
        UserEntity user = getCurrentUser();
        ProjectEntity project = projectRepository.findById(projectId).orElseThrow();
        if (!project.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(com.sairaj.jobinfra.server.controller.dto.ApiResponse.error("FORBIDDEN", "Not allowed"));
        }

        List<ApiKeyResponse> keys = apiKeyRepository.findByProjectId(projectId).stream()
                .map(k -> new ApiKeyResponse(k.getId(), null, k.getDescription(), k.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(keys));
    }

    @DeleteMapping("/{keyId}")
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<Void>> revokeApiKey(@PathVariable Long projectId, @PathVariable Long keyId) {
        UserEntity user = getCurrentUser();
        ProjectEntity project = projectRepository.findById(projectId).orElseThrow();
        if (!project.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(com.sairaj.jobinfra.server.controller.dto.ApiResponse.error("FORBIDDEN", "Not allowed"));
        }

        ApiKeyEntity key = apiKeyRepository.findById(keyId).orElseThrow();
        if (!key.getProject().getId().equals(projectId)) {
            return ResponseEntity.badRequest().body(com.sairaj.jobinfra.server.controller.dto.ApiResponse.error("BAD_REQUEST", "Key does not belong to project"));
        }

        apiKeyRepository.delete(key);
        
        auditLogger.logApiKey("REVOKED", String.valueOf(project.getId()), String.valueOf(key.getId()), user.getUsername());
        
        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(null));
    }
}
