package com.promotionengine.controller;

import com.promotionengine.entity.Promotion;
import com.promotionengine.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

  private final PromotionService promotionService;

  // Create — status in body decides DRAFT or PUBLISH
          @PostMapping
  public ResponseEntity<Promotion> create(
      @RequestBody Promotion promotion,
      @RequestParam(defaultValue = "ADMIN")
        String createdBy) {
    return ResponseEntity.ok(
                      promotionService.create(
                                promotion, createdBy));
  }

  // Update — status in body decides rule action
          @PutMapping("/{id}")
  public ResponseEntity<Promotion> update(
      @PathVariable Long id,
      @RequestBody Promotion promotion,
      @RequestParam(defaultValue = "ADMIN")
        String updatedBy) {
    return ResponseEntity.ok(
                      promotionService.update(
                                id, promotion, updatedBy));
  }

  // Get all promotions
          @GetMapping
  public ResponseEntity<List<Promotion>> getAll() {
    return ResponseEntity.ok(
                      promotionService.getAll());
  }

  // Get by status
          @GetMapping("/status/{status}")
  public ResponseEntity<List<Promotion>> getByStatus(
      @PathVariable
        Promotion.PromotionStatus status) {
    return ResponseEntity.ok(
                      promotionService.getByStatus(status));
  }

  // Get by id
          @GetMapping("/{id}")
  public ResponseEntity<Promotion> getById(
      @PathVariable Long id) {
    return ResponseEntity.ok(
                      promotionService.getById(id));
  }
}