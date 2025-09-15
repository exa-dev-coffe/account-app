package com.time_tracker.be.exception;

import com.time_tracker.be.common.ResponseModel;
import com.time_tracker.be.resolver.ConstraintMessageResolver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(NotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", ex.getStatusCode());
        body.put("message", ex.getMessage());
        body.put("timestamp", ex.getTimestamp().toString());

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequestException(BadRequestException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", ex.getStatusCode());
        body.put("message", ex.getMessage());
        body.put("timestamp", ex.getTimestamp());

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbiddenException(ForbiddenException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", ex.getStatusCode());
        body.put("message", ex.getMessage());
        body.put("timestamp", ex.getTimestamp().toString());

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(NotAuthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleNotAuthorizedException(NotAuthorizedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", ex.getStatusCode());
        body.put("message", ex.getMessage());
        body.put("timestamp", ex.getTimestamp().toString());

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFoundException(NotFoundException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", 404);
        body.put("message", "Resource not found");
        body.put("timestamp", ex.getTimestamp().toString());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        ResponseModel<Map<String, String>> response = new ResponseModel<>();
        response.setSuccess(false);
        response.setMessage("Validation failed");
        response.setData(errors);
        response.setTimestamp(java.time.LocalDateTime.now());

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String dbMessage = e.getMostSpecificCause().getMessage();
        String userMessage = ConstraintMessageResolver.resolveMessage(dbMessage);

        ResponseModel<Map<String, String>> response = new ResponseModel<>();
        response.setSuccess(false);
        response.setMessage(userMessage);
        response.setData(null);
        response.setTimestamp(java.time.LocalDateTime.now());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        ResponseModel<Map<String, String>> body = new ResponseModel<>();
        body.setSuccess(false);
        body.setMessage("An unexpected error occurred");
        body.setData(null);
        body.setTimestamp(java.time.LocalDateTime.now());
        ex.printStackTrace(); // Log the exception for debugging
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
