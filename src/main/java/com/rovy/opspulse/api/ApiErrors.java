package com.rovy.opspulse.api;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ApiErrors {
    public record ErrorBody(Instant timestamp,int status,String error,String message) {}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException ex){
        String msg=ex.getBindingResult().getFieldErrors().stream().map(e->e.getField()+": "+e.getDefaultMessage()).sorted().findFirst().orElse("Invalid input");
        return error(HttpStatus.BAD_REQUEST,msg);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorBody> unreadable(HttpMessageNotReadableException ex){return error(HttpStatus.BAD_REQUEST,"Invalid JSON or enum value");}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorBody> status(ResponseStatusException ex){
        HttpStatusCode code=ex.getStatusCode();
        return ResponseEntity.status(code).body(new ErrorBody(Instant.now(),code.value(),"Request rejected",ex.getReason()));
    }
    private ResponseEntity<ErrorBody> error(HttpStatus status,String msg){return ResponseEntity.status(status).body(new ErrorBody(Instant.now(),status.value(),status.getReasonPhrase(),msg));}
}