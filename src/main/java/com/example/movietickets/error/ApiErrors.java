package com.example.movietickets.error;

import jakarta.validation.ConstraintViolationException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException ex) { return body(ex.status, ex.getMessage()); }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
                       HttpMessageNotReadableException.class, IllegalArgumentException.class,
                       DateTimeException.class})
    ResponseEntity<Map<String, Object>> bad(Exception ex) { return body(HttpStatus.BAD_REQUEST, "Invalid request: " + ex.getMessage()); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException ex) {
        return body(HttpStatus.CONFLICT, "A record with these values already exists or is in use");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message,
                "timestamp", Instant.now().toString()));
    }
}
