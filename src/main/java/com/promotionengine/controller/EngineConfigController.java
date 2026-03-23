package com.promotionengine.controller;

import com.promotionengine.entity.EngineConfig;
import com.promotionengine.service.EngineConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class EngineConfigController {

  private final EngineConfigService
    engineConfigService;

  // Get all config entries
          @GetMapping
  public ResponseEntity<List<EngineConfig>> getAll() {
    return ResponseEntity.ok(
                      engineConfigService.getAll());
  }

  // Get specific config by key
          @GetMapping("/{key}")
  public ResponseEntity<EngineConfig> getByKey(
      @PathVariable String key) {
    return ResponseEntity.ok(
                      engineConfigService.getByKey(key));
  }

  // Update config value
          // e.g. change MAX_PROMOS_PER_CART from 2 to 3
          @PutMapping("/{key}")
  public ResponseEntity<EngineConfig> update(
      @PathVariable String key,
      @RequestParam String value,
      @RequestParam(defaultValue = "ADMIN")
        String updatedBy) {
    return ResponseEntity.ok(
                      engineConfigService.update(
                                key, value, updatedBy));
  }
}