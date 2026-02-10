package io.shaama.offerservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for offer evaluation API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferRequest {
    private String offerId;
    private String customerId;
    private String customerSegment; // PREMIUM, REGULAR, VIP
    private BigDecimal orderAmount;
    private String productCategory; // ELECTRONICS, FASHION, GROCERIES, etc.
    private Boolean isFirstTimeCustomer;
    private LocalDate offerValidUntil;
}
