package com.soss.controller;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes=CartApiController.class)
public class CartApiErrors {
    public record ErrorBody(String code, String message) {}

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorBody> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorBody("INVALID_REQUEST", e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorBody> validation(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorBody("INVALID_REQUEST", "Check the required fields and their formats."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorBody> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ErrorBody("REQUEST_REJECTED", e.getReason()));
    }
}
