package io.shaama.offerservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain model used by Drools rules
 * This object is inserted into the KieSession for rule evaluation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Offer {
    // Input fields
    private String offerId;
    private String customerId;
    private String customerSegment;
    private BigDecimal orderAmount;
    private String productCategory;
    private Boolean isFirstTimeCustomer;
    private LocalDate offerValidUntil;
    
    // Output fields (populated by rules)
    private boolean offerApplicable;
    private BigDecimal discountPercentage;
    private BigDecimal discountAmount;
    private String appliedOfferType;
    private String rejectionReason;
    
    /**
     * Calculated final amount after discount
     */
    public BigDecimal getFinalAmount() {
        if (offerApplicable && discountAmount != null) {
            return orderAmount.subtract(discountAmount);
        }
        return orderAmount;
    }
    
    /**
     * Check if offer is expired
     */
    public boolean isExpired() {
        if (offerValidUntil == null) {
            return false;
        }
        return offerValidUntil.isBefore(LocalDate.now());
    }
}
