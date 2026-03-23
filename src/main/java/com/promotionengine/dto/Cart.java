package com.promotionengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    private String customerId;
    private List<String> promoCodes;
    private List<CartItem> items;
    private String userType;
    private String channel;
    private Integer loyaltyPoints;
    private Boolean firstOrder;
    private String membershipType;

    // ✅ FIX: Always set to now on apply
    // Set by DroolsEngineService before fireAllRules()
    // NOT from request body
    private LocalDateTime currentDateTime;

    public Double getCartTotal() {
        if (items == null || items.isEmpty())
            return 0.0;
        return items.stream()
                .mapToDouble(CartItem::getItemTotal)
                .sum();
    }

    public int getTotalItemCount() {
        if (items == null) return 0;
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public boolean hasPromoCode(String promoCode) {
        if (promoCodes == null
                || promoCodes.isEmpty())
            return false;
        return promoCodes.contains(promoCode);
    }

    public Double getCategoryTotal(String category) {
        if (items == null || category == null)
            return 0.0;
        return items.stream()
                .filter(i -> category.equalsIgnoreCase(
                        i.getCategory()))
                .mapToDouble(CartItem::getItemTotal)
                .sum();
    }

    public Double getTicketTypeTotal(
            String ticketType) {
        if (items == null || ticketType == null)
            return 0.0;
        return items.stream()
                .filter(i -> ticketType.equalsIgnoreCase(
                        i.getTicketType()))
                .mapToDouble(CartItem::getItemTotal)
                .sum();
    }

    public Double getTicketTitleTotal(
            String ticketTitle) {
        if (items == null || ticketTitle == null)
            return 0.0;
        return items.stream()
                .filter(i -> ticketTitle.equalsIgnoreCase(
                        i.getTicketTitle()))
                .mapToDouble(CartItem::getItemTotal)
                .sum();
    }

    public Double getTicketTitleTotalForQty(
            String ticketTitle, int maxQty) {
        if (items == null || ticketTitle == null)
            return 0.0;
        return items.stream()
                .filter(i -> ticketTitle.equalsIgnoreCase(
                        i.getTicketTitle()))
                .mapToDouble(i -> {
                    int qty = Math.min(
                            i.getQuantity(), maxQty);
                    return qty * i.getUnitPrice();
                }).sum();
    }
}