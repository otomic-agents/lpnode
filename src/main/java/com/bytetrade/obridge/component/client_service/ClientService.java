package com.bytetrade.obridge.component.client_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class ClientService {
    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);
    private static final String ENV_PREFIX = "CHAIN_SERVICE_";

    private final Map<String, String> chainUrlMap = new HashMap<>();

    @PostConstruct
    public void init() {
        loadChainServices();
        validateConfiguration();
    }

    /**
     * Validates that at least one valid chain service is configured
     * 
     * @throws RuntimeException if no valid chain services are found
     */
    private void validateConfiguration() {
        if (chainUrlMap.isEmpty()) {
            String errorMsg = "No valid chain services found. Please check the following environment variables";
            logger.error("❌ {}", errorMsg);
            throw new RuntimeException(errorMsg);
        }
    }

    private void loadChainServices() {
        try {
            logger.info("🚀 Starting to load chain services from environment variables");
            Map<String, String> envVars = System.getenv();
            int totalVars = 0;
            int validVars = 0;

            for (Map.Entry<String, String> entry : envVars.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (key.startsWith(ENV_PREFIX)) {
                    totalVars++;
                    String chainId = key.substring(ENV_PREFIX.length());

                    if (value != null && !value.trim().isEmpty()) {
                        chainUrlMap.put(chainId, value.trim());
                        logger.info("✅ Loaded chain service - Chain ID: {}, URL: {}", chainId, value);
                        validVars++;
                    } else {
                        logger.debug("⚠️ Skipped empty chain service configuration for Chain ID: {}", chainId);
                    }
                }
            }

            logger.info("📊 Chain service loading summary:");
            logger.info("   Total configurations found: {}", totalVars);
            logger.info("   Valid configurations loaded: {}", validVars);
            logger.info("   Skipped configurations: {}", (totalVars - validVars));

        } catch (Exception e) {
            logger.error("❌ Failed to load chain services", e);
            throw new RuntimeException("Failed to initialize chain services", e);
        }
    }

    public List<String> getAllUrls() {
        List<String> urls = new ArrayList<>(chainUrlMap.values());
        if (urls.isEmpty()) {
            String errorMsg = "No chain service URLs available";
            logger.error("❌ {}", errorMsg);
            throw new RuntimeException(errorMsg);
        }
        logger.info("📋 Returning URLs (total: {}): {}", urls.size(), urls);
        return urls;
    }

    /**
     * Get URL for specific chain ID
     * 
     * @param chainId the chain identifier
     * @return URL for the specified chain, or null if not found
     */
    public String getUrlByChainId(String chainId) {
        String url = chainUrlMap.get(chainId);
        if (url != null) {
            logger.info("🔍 Found URL for chain ID {}: {}", chainId, url);
        } else {
            logger.warn("⚠️ No URL found for chain ID: {}", chainId);
        }
        return url;
    }

    /**
     * Check if service exists for given chain ID
     * 
     * @param chainId the chain identifier
     * @return true if service exists, false otherwise
     */
    public boolean hasChainService(String chainId) {
        boolean exists = chainUrlMap.containsKey(chainId);
        logger.debug("🔍 Checking chain service existence for {}: {}", chainId, exists);
        return exists;
    }

    /**
     * Get all configured chain IDs
     * 
     * @return Set of all configured chain IDs
     */
    public Set<String> getAllChainIds() {
        Set<String> chainIds = new HashSet<>(chainUrlMap.keySet());
        logger.info("📋 Returning all chain IDs (total: {}): {}", chainIds.size(), chainIds);
        return chainIds;
    }

    /**
     * Get configuration summary
     * 
     * @return String containing configuration summary
     */
    public String getConfigurationSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("\n=== Chain Service Configuration Summary ===\n");
        summary.append(String.format("Total configured services: %d\n", chainUrlMap.size()));
        chainUrlMap.forEach((chainId, url) -> summary.append(String.format("Chain ID: %s -> URL: %s\n", chainId, url)));
        summary.append("=====================================");
        return summary.toString();
    }
}
