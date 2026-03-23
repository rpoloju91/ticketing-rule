package com.promotionengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {

  private String productId;
  private String productName;

  // e.g. Museum Admission
          private String category;

  // e.g. General Admission
          private String ticketType;

  // e.g. Adult / Youth / Child
          private String ticketTitle;

  private int quantity;

  // e.g. 29.99
          private Double unitPrice;

  public Double getItemTotal() {
    return unitPrice * quantity;
  }
}