package com.promotionengine.controller;

import com.promotionengine.entity.SystemDiscount;
import com.promotionengine.enums.DiscountStatus;
import com.promotionengine.service.SystemDiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system-discounts")
@RequiredArgsConstructor
public class SystemDiscountController {

  private final SystemDiscountService
    systemDiscountService;

  // Create — status in body decides DRAFT or PUBLISH
          @PostMapping
  public ResponseEntity<SystemDiscount> create(
      @RequestBody SystemDiscount discount,
      @RequestParam(defaultValue = "ADMIN")
        String createdBy) {
    return ResponseEntity.ok(
                      systemDiscountService.create(
                                discount, createdBy));
  }

  // Update — status in body decides rule action
          @PutMapping("/{id}")
  public ResponseEntity<SystemDiscount> update(
      @PathVariable Long id,
      @RequestBody SystemDiscount discount,
      @RequestParam(defaultValue = "ADMIN")
        String updatedBy) {
    return ResponseEntity.ok(
                      systemDiscountService.update(
                                id, discount, updatedBy));
  }

  // Get all
          @GetMapping
  public ResponseEntity<List<SystemDiscount>> getAll() {
    return ResponseEntity.ok(
                      systemDiscountService.getAll());
  }

  // Get by status
          @GetMapping("/status/{status}")
  public ResponseEntity<List<SystemDiscount>>
  getByStatus(
      @PathVariable
      DiscountStatus status) {
    return ResponseEntity.ok(
                      systemDiscountService.getByStatus(status));
  }

  // Get by id
          @GetMapping("/{id}")
  public ResponseEntity<SystemDiscount> getById(
      @PathVariable Long id) {
    return ResponseEntity.ok(
                      systemDiscountService.getById(id));
  }
}
