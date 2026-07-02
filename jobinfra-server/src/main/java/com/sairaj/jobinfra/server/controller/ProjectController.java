package com.sairaj.jobinfra.server.controller;

import com.sairaj.jobinfra.server.controller.dto.ProjectDto;
import com.sairaj.jobinfra.server.domain.ProjectEntity;
import com.sairaj.jobinfra.server.domain.UserEntity;
import com.sairaj.jobinfra.server.repository.ProjectRepository;
import com.sairaj.jobinfra.server.repository.UserRepository;
import com.sairaj.jobinfra.server.service.AuditLogger;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Manage projects for background jobs")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    public ProjectController(ProjectRepository projectRepository, UserRepository userRepository, AuditLogger auditLogger) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
    }

    private UserEntity getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }

    @PostMapping
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<ProjectDto>> createProject(@jakarta.validation.Valid @RequestBody ProjectDto request) {
        UserEntity user = getCurrentUser();
        ProjectEntity project = new ProjectEntity(request.getName(), user);
        projectRepository.save(project);
        
        auditLogger.logProject("CREATED", String.valueOf(project.getId()), project.getName(), user.getUsername());
        
        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(new ProjectDto(project.getId(), project.getName())));
    }

    @GetMapping
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<List<ProjectDto>>> getProjects() {
        UserEntity user = getCurrentUser();
        List<ProjectDto> projects = projectRepository.findByUserId(user.getId()).stream()
                .map(p -> new ProjectDto(p.getId(), p.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(projects));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<Void>> deleteProject(@PathVariable Long id) {
        UserEntity user = getCurrentUser();
        ProjectEntity project = projectRepository.findById(id).orElseThrow();
        if (!project.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(com.sairaj.jobinfra.server.controller.dto.ApiResponse.error("FORBIDDEN", "Not allowed"));
        }
        projectRepository.delete(project);
        
        auditLogger.logProject("DELETED", String.valueOf(project.getId()), project.getName(), user.getUsername());
        
        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(null));
    }
}
