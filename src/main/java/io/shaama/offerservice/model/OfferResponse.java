package io.shaama.offerservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO for offer evaluation API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferResponse {
    private String offerId;
    private Boolean offerApplicable;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private String appliedOfferType;
    private BigDecimal finalAmount;
    private String rejectionReason;
}
