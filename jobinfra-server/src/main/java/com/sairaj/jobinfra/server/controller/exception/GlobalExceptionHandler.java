package com.sairaj.jobinfra.server.controller.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        StringBuilder errorMessage = new StringBuilder();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errorMessage.append(fieldName).append(" ").append(message).append("; ");
        });
        
        logException(ex, request, "Validation Error: " + errorMessage.toString());

        return ResponseEntity.badRequest().body(com.sairaj.jobinfra.server.controller.dto.ApiResponse.error(
                "VALIDATION_ERROR", errorMessage.toString()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<Object>> handleAllExceptions(
            Exception ex, HttpServletRequest request) {
        
        logException(ex, request, ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(com.sairaj.jobinfra.server.controller.dto.ApiResponse.error(
                        "INTERNAL_ERROR", "An unexpected error occurred."));
    }

    private void logException(Exception ex, HttpServletRequest request, String customMessage) {
        String endpoint = request != null ? request.getRequestURI() : "Unknown";
        String ip = request != null ? request.getHeader("X-Forwarded-For") : null;
        if (ip == null || ip.isEmpty()) {
            ip = request != null ? request.getRemoteAddr() : "Unknown";
        }
        
        MDC.put("exceptionClass", ex.getClass().getName());
        MDC.put("endpoint", endpoint);
        MDC.put("clientIp", ip);
        
        log.error("Exception handled: {}", customMessage, ex);
        
        MDC.remove("exceptionClass");
        MDC.remove("endpoint");
        MDC.remove("clientIp");
    }
}
