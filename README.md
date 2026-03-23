# ticketing-rule
Drools + Springboot + JPA


curl -X POST "http://localhost:8080/api/promotions?createdBy=ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "redemptionMethod"   : "PROMO_CODE",
    "promotionType"      : "PERCENTAGE",
    "promoCode"          : "SAVE10",
    "displayMessage"     : "10% off full cart",
    "usageLimit"         : 100,
    "maxUsagePerCustomer": 3,
    "discountType"       : "PERCENTAGE",
    "amount"             : 10.0,
    "stackable"          : true,
    "priority"           : 1,
    "applyFullCart"      : true,
    "startDate"          : "2026-01-01T00:00:00",
    "endDate"            : "2026-12-31T23:59:59",
    "channelWeb"         : true,
    "channelPos"         : true,
    "userType"           : "ALL_USERS",
    "status"             : "PUBLISHED"
  }'
 
  curl -X POST "http://localhost:8080/api/system-discounts?createdBy=ADMIN" \
  -H "Content-Type: application/json" \
  -d '{
    "title"                  : "LOYALTY_REWARD",
    "displayName"            : "Loyalty Reward",
    "description"            : "5% off for members",
    "discountType"           : "PERCENTAGE",
    "amount"                 : 5.0,
    "icon"                   : "loyalty-icon",
    "startDate"              : "2026-01-01T00:00:00",
    "endDate"                : "2026-12-31T23:59:59",
    "managerApprovalRequired": false,
    "stackable"              : true,
    "userType"               : "MEMBERS",
    "priority"               : 80,
    "status"                 : "PUBLISHED"
  }'

  curl http://localhost:8080/api/engine/rules
# Should return PROMO_SAVE10 and SYSDISCOUNT_LOYALTY_REWARD

curl -X POST http://localhost:8080/api/engine/apply \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-001",
    "promoCodes": ["SAVE10"],
    "userType"  : "MEMBER",
    "channel"   : "WEB",
    "items"     : [
      {
        "productId"  : "T001",
        "productName": "Adult Ticket",
        "category"   : "Museum Admission",
        "ticketType" : "General Admission",
        "ticketTitle": "Adult",
        "quantity"   : 2,
        "unitPrice"  : 100.00
      }
    ]
  }'

  INSERT INTO engine_config
    (config_key, config_value, description,
     created_at, updated_at)
VALUES
    ('MAX_PROMOS_PER_CART', '2',
     'Maximum number of promo codes customer can select per cart',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
