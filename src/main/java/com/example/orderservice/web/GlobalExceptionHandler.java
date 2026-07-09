package com.example.orderservice.web;

import com.example.orderservice.exception.InvalidOrderStateException;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.web.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ErrorResponse(LocalDateTime.now(), 404, "NOT_FOUND", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidOrderStateException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            new ErrorResponse(LocalDateTime.now(), 409, "INVALID_STATE", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(LocalDateTime.now(), 400, "VALIDATION_ERROR", message, request.getRequestURI())
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(LocalDateTime.now(), 400, "MALFORMED_REQUEST", "Request body is missing or not valid JSON", request.getRequestURI())
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ErrorResponse(LocalDateTime.now(), 400, "INVALID_PARAMETER",
                "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue(), request.getRequestURI())
        );
    }

    // ponytail: generic fallback — never leak stack traces to the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ErrorResponse(LocalDateTime.now(), 500, "INTERNAL_ERROR", "An unexpected error occurred", request.getRequestURI())
        );
    }
}

