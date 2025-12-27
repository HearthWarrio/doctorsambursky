package com.doctor.booking.web;

import com.doctor.booking.exception.*;
import jakarta.validation.ConstraintViolationException;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Data
    public static class ApiError {
        private OffsetDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, Object> details;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.put(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", request, details);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed JSON request", request, null);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(ex.getParameterName(), "missing");
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", ex.getMessage(), request, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> constraintViolation(ConstraintViolationException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> typeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidTimeFormatException.class)
    public ResponseEntity<Object> invalidTime(InvalidTimeFormatException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_TIME_FORMAT", ex.getMessage(), request, null);
    }

    @ExceptionHandler(SlotUnavailableException.class)
    public ResponseEntity<Object> slotUnavailable(SlotUnavailableException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, "SLOT_UNAVAILABLE", ex.getMessage(), request, null);
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<Object> notFound(AppointmentNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidAppointmentStateException.class)
    public ResponseEntity<Object> badState(InvalidAppointmentStateException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, "INVALID_STATE", ex.getMessage(), request, null);
    }

    @ExceptionHandler(BotUnavailableException.class)
    public ResponseEntity<Object> botDown(BotUnavailableException ex, WebRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "BOT_UNAVAILABLE", ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> illegalArgument(IllegalArgumentException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> illegalState(IllegalStateException ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR", ex.getMessage(), request, null);
    }

    private ResponseEntity<Object> build(HttpStatus status, String error, String message, WebRequest request, Map<String, Object> details) {
        ApiError body = new ApiError();
        body.setTimestamp(OffsetDateTime.now());
        body.setStatus(status.value());
        body.setError(error);
        body.setMessage(message);
        body.setDetails(details);
        String desc = request.getDescription(false);
        body.setPath(desc != null && desc.startsWith("uri=") ? desc.substring(4) : desc);
        return ResponseEntity.status(status).body(body);
    }
}