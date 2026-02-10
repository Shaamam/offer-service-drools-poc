package io.shaama.offerservice.controller;

import io.shaama.offerservice.config.KieContainerManager;
import io.shaama.offerservice.model.OfferRequest;
import io.shaama.offerservice.model.OfferResponse;
import io.shaama.offerservice.service.OfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for offer evaluation APIs
 */
@Slf4j
@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {
    
    private final OfferService offerService;
    private final KieContainerManager kieContainerManager;
    
    /**
     * Evaluate an offer based on customer and order details
     * 
     * POST /api/offers/evaluate
     */
    @PostMapping("/evaluate")
    public ResponseEntity<OfferResponse> evaluateOffer(@RequestBody OfferRequest request) {
        log.info("Received offer evaluation request: {}", request.getOfferId());
        
        OfferResponse response = offerService.evaluateOffer(request);
        
        log.info("Offer evaluation completed: {} - Applicable: {}", 
            response.getOfferId(), 
            response.getOfferApplicable());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get information about currently loaded rules
     * 
     * GET /api/offers/rules-info
     */
    @GetMapping("/rules-info")
    public ResponseEntity<Map<String, Object>> getRulesInfo() {
        log.info("Fetching rules information");
        
        KieContainerManager.KieContainerInfo info = kieContainerManager.getContainerInfo();
        
        Map<String, Object> response = Map.of(
            "releaseId", info.getReleaseId(),
            "groupId", info.getGroupId(),
            "artifactId", info.getArtifactId(),
            "version", info.getVersion(),
            "sessionName", info.getSessionName(),
            "autoReloadEnabled", info.isAutoReloadEnabled(),
            "scanIntervalSeconds", info.getScanIntervalSeconds(),
            "kieBaseNames", info.getKieBaseNames(),
            "status", "Active"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint for rules engine
     * 
     * GET /api/offers/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        try {
            kieContainerManager.getContainerInfo();
            return ResponseEntity.ok(Map.of(
                "status", "UP",
                "rulesEngine", "Active"
            ));
        } catch (Exception e) {
            log.error("Rules engine health check failed", e);
            return ResponseEntity.status(503).body(Map.of(
                "status", "DOWN",
                "rulesEngine", "Error: " + e.getMessage()
            ));
        }
    }
}
