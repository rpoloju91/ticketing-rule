/*
package com.promotionengine.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<Map<String, Object>>
  handleRuntime(RuntimeException ex) {
    log.error("RuntimeException: {}",
                      ex.getMessage());
    Map<String, Object> error =
      new LinkedHashMap<>();
    error.put("status",
                      HttpStatus.BAD_REQUEST.value());
    error.put("error", ex.getMessage());
    error.put("timestamp",
                      LocalDateTime.now().toString());
    return ResponseEntity
      .badRequest().body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>>
  handleGeneral(Exception ex) {
    log.error("Exception: {}", ex.getMessage());
    Map<String, Object> error =
      new LinkedHashMap<>();
    error.put("status",
                      HttpStatus.INTERNAL_SERVER_ERROR.value());
    error.put("error", "Internal server error");
    error.put("timestamp",
                      LocalDateTime.now().toString());
    return ResponseEntity
      .internalServerError().body(error);
  }
}*/
