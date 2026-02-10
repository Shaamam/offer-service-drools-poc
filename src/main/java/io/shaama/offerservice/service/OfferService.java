package io.shaama.offerservice.service;

import io.shaama.offerservice.config.KieContainerManager;
import io.shaama.rulesengine.model.Offer;
import io.shaama.offerservice.model.OfferRequest;
import io.shaama.offerservice.model.OfferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;

/**
 * Service for evaluating offers using Drools rules engine
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferService {
    
    private final KieContainerManager kieContainerManager;
    
    /**
     * Evaluate offer based on customer and order details
     * 
     * @param request Offer evaluation request
     * @return Offer evaluation response with discount details
     */
    public OfferResponse evaluateOffer(OfferRequest request) {
        log.info("Evaluating offer: {} for customer: {}", request.getOfferId(), request.getCustomerId());
        
        // Map request to domain model
        Offer offer = mapToOffer(request);

        log.info("Offer {}", offer);
        
        // Execute rules
        try {
            StatelessKieSession session = kieContainerManager.getOfferSession();
            
            // Set global variables if needed (for logging in rules)
            // session.setGlobal("logger", log);
            
            // Execute all rules against the offer
            // StatelessKieSession requires objects in a collection to modify them
            session.execute(Collections.singletonList(offer));
            
            log.info("Rules executed for offer: {}. Applicable: {}, Discount: {}%", 
                offer.getOfferId(), 
                offer.isOfferApplicable(), 
                offer.getDiscountPercentage());
            
        } catch (Exception e) {
            log.error("Error executing rules for offer: {}", request.getOfferId(), e);
            throw new RuntimeException("Failed to evaluate offer", e);
        }
        
        // Map result to response
        return mapToResponse(offer);
    }
    
    /**
     * Map request DTO to domain model
     */
    private Offer mapToOffer(OfferRequest request) {
        return Offer.builder()
            .offerId(request.getOfferId())
            .customerId(request.getCustomerId())
            .customerSegment(request.getCustomerSegment())
            .orderAmount(request.getOrderAmount())
            .productCategory(request.getProductCategory())
            .isFirstTimeCustomer(request.getIsFirstTimeCustomer() != null ? request.getIsFirstTimeCustomer() : false)
            .offerValidUntil(request.getOfferValidUntil())
            // Initialize output fields
            .offerApplicable(false)
            .discountPercentage(BigDecimal.ZERO)
            .discountAmount(BigDecimal.ZERO)
            .build();
    }
    
    /**
     * Map domain model to response DTO
     */
    private OfferResponse mapToResponse(Offer offer) {
        return OfferResponse.builder()
            .offerId(offer.getOfferId())
            .offerApplicable(offer.isOfferApplicable())
            .discountPercentage(offer.getDiscountPercentage())
            .discountAmount(offer.getDiscountAmount() != null 
                ? offer.getDiscountAmount().setScale(2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO)
            .appliedOfferType(offer.getAppliedOfferType())
            .finalAmount(calculateFinalAmount(offer).setScale(2, RoundingMode.HALF_UP))
            .rejectionReason(offer.getRejectionReason())
            .build();
    }
    
    /**
     * Calculate final amount after discount
     */
    private BigDecimal calculateFinalAmount(Offer offer) {
        if (offer.isOfferApplicable() && offer.getDiscountAmount() != null) {
            return offer.getOrderAmount().subtract(offer.getDiscountAmount());
        }
        return offer.getOrderAmount();
    }
}
