package io.shaama.offerservice.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Manages KieContainer lifecycle and hot-reloading using KieScanner
 * KieScanner automatically polls the Maven repository for KJAR updates
 */
@Slf4j
@Component
public class KieContainerManager {
    
    private final KieServices kieServices;
    private volatile KieContainer kieContainer;
    
    @Getter
    private ReleaseId currentReleaseId;
    
    @Value("${rules.engine.groupId}")
    private String groupId;
    
    @Value("${rules.engine.artifactId}")
    private String artifactId;
    
    @Value("${rules.engine.version}")
    private String version;
    
    @Value("${rules.engine.session-name}")
    private String sessionName;
    
    @Value("${rules.engine.auto-reload}")
    private boolean autoReload;
    
    @Value("${rules.engine.scan-interval-seconds:10}")
    private long scanIntervalSeconds;
    
    public KieContainerManager() {
        this.kieServices = KieServices.Factory.get();
        configureMavenSettings();
    }
    
    /**
     * Configure Maven settings to use GitHub Packages for KieScanner
     * This allows KieScanner to poll GitHub Packages at runtime
     */
    private void configureMavenSettings() {
        try {
            // Set Maven settings file location for KieScanner to use
            String settingsPath = System.getProperty("user.dir") + "/maven-settings.xml";
            System.setProperty("kie.maven.settings.custom", settingsPath);
            log.info("Configured Maven settings for KieScanner: {}", settingsPath);
        } catch (Exception e) {
            log.warn("Could not configure custom Maven settings, KieScanner will use default settings", e);
        }
    }
    
    /**
     * Initialize KieContainer and start KieScanner on application startup
     */
    @PostConstruct
    public void init() {
        log.info("Initializing KieContainerManager...");
        loadRules();
        
        if (autoReload) {
            startAutoReload();
        }
    }
    
    /**
     * Load rules from KJAR dependency
     * Uses LATEST to always poll for the newest version
     */
    private void loadRules() {
        try {
            // Use version from properties (typically LATEST for always getting newest)
            currentReleaseId = kieServices.newReleaseId(groupId, artifactId, version);
            log.info("Loading rules from KJAR: {}:{}:{}", groupId, artifactId, version);
            
            kieContainer = kieServices.newKieContainer(currentReleaseId);
            
            log.info("✓ Rules loaded successfully from KJAR");
            log.info("  - KBase names: {}", kieContainer.getKieBaseNames());
            log.info("  - KSession names: {}", kieContainer.getKieSessionNamesInKieBase(
                kieContainer.getKieBaseNames().iterator().next()
            ));
            
        } catch (Exception e) {
            log.error("✗ Failed to load rules from KJAR", e);
            throw new RuntimeException("Failed to initialize Drools rules engine", e);
        }
    }
    
    /**
     * Start KieScanner for automatic hot-reloading
     * KieScanner polls the Maven repository and updates the container when a new version is found
     */
    private void startAutoReload() {
        try {
            log.info("Starting KieScanner for automatic rule hot-reloading...");
            log.info("  - Scan interval: {} seconds", scanIntervalSeconds);
            log.info("  - Watching: {}:{}:{}", groupId, artifactId, version);
            
            // Start KieScanner - polls Maven repo and automatically updates KieContainer
            kieServices.newKieScanner(kieContainer).start(scanIntervalSeconds * 1000L);
            
            log.info("✓ KieScanner started successfully");
            log.info("  ⚡ Rules will hot-reload automatically when KJAR is updated in Maven repo");
            
        } catch (Exception e) {
            log.error("✗ Failed to start KieScanner", e);
            log.warn("  Auto-reload disabled. Rules will only load on application restart.");
        }
    }
    
    /**
     * Get a stateless KIE session for offer evaluation
     * StatelessKieSession is thread-safe and doesn't maintain state between executions
     * 
     * @return StatelessKieSession for offer rules
     */
    public StatelessKieSession getOfferSession() {
        try {
            return kieContainer.newStatelessKieSession(sessionName);
        } catch (Exception e) {
            log.error("Failed to create KieSession: {}", sessionName, e);
            throw new RuntimeException("Failed to create KieSession for offer evaluation", e);
        }
    }
    
    /**
     * Get a stateful KIE session (if needed for complex scenarios)
     * 
     * @return KieSession
     */
    public KieSession getStatefulSession() {
        try {
            return kieContainer.newKieSession(sessionName);
        } catch (Exception e) {
            log.error("Failed to create stateful KieSession: {}", sessionName, e);
            throw new RuntimeException("Failed to create stateful KieSession", e);
        }
    }
    
    /**
     * Get current KieContainer information
     * 
     * @return KieContainer metadata
     */
    public KieContainerInfo getContainerInfo() {
        return KieContainerInfo.builder()
            .releaseId(currentReleaseId.toString())
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .sessionName(sessionName)
            .autoReloadEnabled(autoReload)
            .scanIntervalSeconds(scanIntervalSeconds)
            .kieBaseNames(kieContainer.getKieBaseNames().toString())
            .build();
    }
    
    /**
     * Clean up resources on application shutdown
     */
    @PreDestroy
    public void destroy() {
        log.info("Shutting down KieContainerManager...");
        if (kieContainer != null) {
            kieContainer.dispose();
            log.info("✓ KieContainer disposed");
        }
    }
    
    /**
     * KieContainer metadata DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class KieContainerInfo {
        private String releaseId;
        private String groupId;
        private String artifactId;
        private String version;
        private String sessionName;
        private boolean autoReloadEnabled;
        private long scanIntervalSeconds;
        private String kieBaseNames;
    }
}
