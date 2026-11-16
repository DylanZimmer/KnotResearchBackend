package com.knots.backend.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

// Moves throw IllegalArgumentException when asked for something the diagram doesn't have
// (e.g. removing a twist at a crossing that isn't one). Report that as a 400 with the reason
// instead of a 500.
@RestControllerAdvice
public class BadMoveHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badMove(IllegalArgumentException e) {
        String message = e.getMessage() == null ? "Invalid move" : e.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Bad Request", "message", message));
    }
}
