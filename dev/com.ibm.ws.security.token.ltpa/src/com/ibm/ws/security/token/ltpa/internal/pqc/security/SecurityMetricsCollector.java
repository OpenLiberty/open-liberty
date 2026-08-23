/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal.pqc.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.token.ltpa.internal.TraceConstants;

/**
 * Collects security metrics for LTPA token validation and attack detection.
 * 
 * <p>This collector tracks:
 * <ul>
 *   <li>Token validation attempts (success/failure)</li>
 *   <li>Attack detection events (downgrade, replay, tampering)</li>
 *   <li>Token type distribution (classical vs hybrid)</li>
 *   <li>Validation performance metrics</li>
 *   <li>Security anomalies and trends</li>
 * </ul>
 * 
 * <h3>Security Monitoring</h3>
 * <p>Metrics enable security teams to:
 * <ul>
 *   <li><b>Detect Attacks</b>: Identify unusual patterns or spikes</li>
 *   <li><b>Monitor Health</b>: Track validation success rates</li>
 *   <li><b>Analyze Trends</b>: Understand token usage patterns</li>
 *   <li><b>Audit Compliance</b>: Verify security policy enforcement</li>
 * </ul>
 * 
 * <h3>Metric Categories</h3>
 * <pre>
 * Validation Metrics:
 *   - Total validations
 *   - Successful validations
 *   - Failed validations
 *   - Average validation time
 * 
 * Token Type Metrics:
 *   - Classical tokens validated
 *   - Hybrid tokens validated
 *   - Token type distribution
 * 
 * Attack Detection Metrics:
 *   - Downgrade attacks detected
 *   - Replay attacks detected
 *   - Tampering attempts detected
 *   - Expired token usage
 * 
 * Performance Metrics:
 *   - Min/max/avg validation time
 *   - Validation throughput
 *   - Cache hit rates
 * </pre>
 * 
 * <h3>Thread Safety</h3>
 * <p>This class is thread-safe and uses lock-free atomic operations for
 * high-performance metric collection in concurrent environments.
 * 
 * <h3>Usage Example</h3>
 * <pre>
 * SecurityMetricsCollector metrics = SecurityMetricsCollector.getInstance();
 * 
 * // Record validation attempt
 * long startTime = System.nanoTime();
 * boolean success = validateToken(token);
 * long duration = System.nanoTime() - startTime;
 * 
 * if (success) {
 *     metrics.recordValidationSuccess(tokenType, duration);
 * } else {
 *     metrics.recordValidationFailure(tokenType, failureReason);
 * }
 * 
 * // Get metrics summary
 * MetricsSummary summary = metrics.getSummary();
 * System.out.println("Success rate: " + summary.getSuccessRate() + "%");
 * </pre>
 * 
 * @see <a href="https://github.com/open-liberty/open-liberty/blob/integration/dev/com.ibm.ws.security.token.ltpa/pqc-ltpa-implementation/05-security-considerations/attack-resistance.md">Attack Resistance Documentation</a>
 */
public class SecurityMetricsCollector {
    
    private static final TraceComponent tc = Tr.register(SecurityMetricsCollector.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    
    /** Singleton instance */
    private static volatile SecurityMetricsCollector instance;
    
    /** Lock for singleton initialization */
    private static final Object LOCK = new Object();
    
    // Validation metrics
    private final AtomicLong totalValidations = new AtomicLong(0);
    private final AtomicLong successfulValidations = new AtomicLong(0);
    private final AtomicLong failedValidations = new AtomicLong(0);
    
    // Token type metrics
    private final AtomicLong classicalTokens = new AtomicLong(0);
    private final AtomicLong hybridTokens = new AtomicLong(0);
    
    // Attack detection metrics
    private final AtomicLong downgradeAttacks = new AtomicLong(0);
    private final AtomicLong replayAttacks = new AtomicLong(0);
    private final AtomicLong tamperingAttempts = new AtomicLong(0);
    private final AtomicLong expiredTokens = new AtomicLong(0);
    
    // Performance metrics
    private final AtomicLong totalValidationTimeNs = new AtomicLong(0);
    private final AtomicLong minValidationTimeNs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxValidationTimeNs = new AtomicLong(0);
    
    // Failure reason tracking
    private final ConcurrentHashMap<String, AtomicLong> failureReasons = new ConcurrentHashMap<>();
    
    // Metrics collection start time
    private final long startTimeMs;
    
    /**
     * Private constructor for singleton pattern.
     */
    private SecurityMetricsCollector() {
        this.startTimeMs = System.currentTimeMillis();
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "SecurityMetricsCollector initialized");
        }
    }
    
    /**
     * Returns the singleton instance of SecurityMetricsCollector.
     * 
     * @return the singleton instance
     */
    public static SecurityMetricsCollector getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new SecurityMetricsCollector();
                }
            }
        }
        return instance;
    }
    
    /**
     * Records a successful token validation.
     * 
     * @param tokenType the type of token validated ("classical" or "hybrid")
     * @param validationTimeNs validation duration in nanoseconds
     */
    public void recordValidationSuccess(String tokenType, long validationTimeNs) {
        totalValidations.incrementAndGet();
        successfulValidations.incrementAndGet();
        
        // Track token type
        if ("classical".equalsIgnoreCase(tokenType)) {
            classicalTokens.incrementAndGet();
        } else if ("hybrid".equalsIgnoreCase(tokenType)) {
            hybridTokens.incrementAndGet();
        }
        
        // Update performance metrics
        updatePerformanceMetrics(validationTimeNs);
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Validation success recorded: type=" + tokenType + 
                        ", duration=" + (validationTimeNs / 1_000_000.0) + "ms");
        }
    }
    
    /**
     * Records a failed token validation.
     * 
     * @param tokenType the type of token validated ("classical" or "hybrid")
     * @param failureReason the reason for validation failure
     */
    public void recordValidationFailure(String tokenType, String failureReason) {
        totalValidations.incrementAndGet();
        failedValidations.incrementAndGet();
        
        // Track failure reason
        failureReasons.computeIfAbsent(failureReason, k -> new AtomicLong(0)).incrementAndGet();
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Validation failure recorded: type=" + tokenType + 
                        ", reason=" + failureReason);
        }
        
        // Log warning for high failure rate
        long total = totalValidations.get();
        long failed = failedValidations.get();
        if (total > 100 && (failed * 100 / total) > 10) { // >10% failure rate
            Tr.warning(tc, "CWWKS4250W: High token validation failure rate: " + 
                          (failed * 100 / total) + "% (" + failed + "/" + total + ")");
        }
    }
    
    /**
     * Records a downgrade attack detection.
     * 
     * @param details additional details about the attack
     */
    public void recordDowngradeAttack(String details) {
        downgradeAttacks.incrementAndGet();
        Tr.audit(tc, "SECURITY: Downgrade attack detected - " + details);
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Downgrade attack recorded: " + details);
        }
    }
    
    /**
     * Records a replay attack detection.
     * 
     * @param details additional details about the attack
     */
    public void recordReplayAttack(String details) {
        replayAttacks.incrementAndGet();
        Tr.audit(tc, "SECURITY: Replay attack detected - " + details);
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Replay attack recorded: " + details);
        }
    }
    
    /**
     * Records a tampering attempt detection.
     * 
     * @param details additional details about the attempt
     */
    public void recordTamperingAttempt(String details) {
        tamperingAttempts.incrementAndGet();
        Tr.audit(tc, "SECURITY: Token tampering detected - " + details);
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Tampering attempt recorded: " + details);
        }
    
    /**
     * Records a generic attack detection event.
     * This method routes to specific attack type methods based on the attack type.
     * 
     * @param attackType The type of attack detected (e.g., "DOWNGRADE_ATTACK", "REPLAY_ATTACK", "TAMPERING_*")
     * @param userId The user ID associated with the attack (may be null)
     */
    public void recordAttackDetection(String attackType, String userId) {
        if (attackType == null) {
            return;
        }
        
        // Route to specific attack type methods
        if (attackType.equals("DOWNGRADE_ATTACK")) {
            recordDowngradeAttack("User: " + (userId != null ? userId : "unknown"));
        } else if (attackType.equals("REPLAY_ATTACK")) {
            recordReplayAttack("User: " + (userId != null ? userId : "unknown"));
        } else if (attackType.startsWith("TAMPERING_")) {
            recordTamperingAttempt(attackType + " - User: " + (userId != null ? userId : "unknown"));
        } else {
            // Generic attack detection
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Generic attack detected: " + attackType + " for user: " + userId);
            }
        }
    }
    }
    
    /**
     * Records an expired token usage.
     * 
     * @param expirationTimeMs the token's expiration time
     */
    public void recordExpiredToken(long expirationTimeMs) {
        expiredTokens.incrementAndGet();
        
        long currentTimeMs = System.currentTimeMillis();
        long expiredByMs = currentTimeMs - expirationTimeMs;
        
        Tr.audit(tc, "SECURITY: Expired token usage - expired " + expiredByMs + "ms ago");
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Expired token recorded: expired by " + expiredByMs + "ms");
        }
    }
    
    /**
     * Updates performance metrics with a new validation time.
     */
    private void updatePerformanceMetrics(long validationTimeNs) {
        totalValidationTimeNs.addAndGet(validationTimeNs);
        
        // Update min time
        long currentMin;
        do {
            currentMin = minValidationTimeNs.get();
            if (validationTimeNs >= currentMin) {
                break;
            }
        } while (!minValidationTimeNs.compareAndSet(currentMin, validationTimeNs));
        
        // Update max time
        long currentMax;
        do {
            currentMax = maxValidationTimeNs.get();
            if (validationTimeNs <= currentMax) {
                break;
            }
        } while (!maxValidationTimeNs.compareAndSet(currentMax, validationTimeNs));
    }
    
    /**
     * Returns a summary of collected metrics.
     * 
     * @return metrics summary
     */
    public MetricsSummary getSummary() {
        return new MetricsSummary(
            totalValidations.get(),
            successfulValidations.get(),
            failedValidations.get(),
            classicalTokens.get(),
            hybridTokens.get(),
            downgradeAttacks.get(),
            replayAttacks.get(),
            tamperingAttempts.get(),
            expiredTokens.get(),
            totalValidationTimeNs.get(),
            minValidationTimeNs.get() == Long.MAX_VALUE ? 0 : minValidationTimeNs.get(),
            maxValidationTimeNs.get(),
            new ConcurrentHashMap<>(failureReasons),
            startTimeMs
        );
    }
    
    /**
     * Resets all metrics to zero.
     * 
     * <p>This method should be used with caution as it clears all collected data.
     */
    public void reset() {
        totalValidations.set(0);
        successfulValidations.set(0);
        failedValidations.set(0);
        classicalTokens.set(0);
        hybridTokens.set(0);
        downgradeAttacks.set(0);
        replayAttacks.set(0);
        tamperingAttempts.set(0);
        expiredTokens.set(0);
        totalValidationTimeNs.set(0);
        minValidationTimeNs.set(Long.MAX_VALUE);
        maxValidationTimeNs.set(0);
        failureReasons.clear();
        
        Tr.info(tc, "CWWKS4251I: Security metrics reset");
    }
    
    /**
     * Logs a summary of current metrics.
     */
    public void logSummary() {
        MetricsSummary summary = getSummary();
        
        Tr.info(tc, "=== Security Metrics Summary ===");
        Tr.info(tc, "Total Validations: " + summary.getTotalValidations());
        Tr.info(tc, "Success Rate: " + String.format("%.2f%%", summary.getSuccessRate()));
        Tr.info(tc, "Classical Tokens: " + summary.getClassicalTokens());
        Tr.info(tc, "Hybrid Tokens: " + summary.getHybridTokens());
        Tr.info(tc, "Downgrade Attacks: " + summary.getDowngradeAttacks());
        Tr.info(tc, "Replay Attacks: " + summary.getReplayAttacks());
        Tr.info(tc, "Tampering Attempts: " + summary.getTamperingAttempts());
        Tr.info(tc, "Expired Tokens: " + summary.getExpiredTokens());
        Tr.info(tc, "Avg Validation Time: " + String.format("%.2fms", summary.getAverageValidationTimeMs()));
        Tr.info(tc, "================================");
    }
    
    /**
     * Immutable summary of security metrics.
     */
    public static class MetricsSummary {
        private final long totalValidations;
        private final long successfulValidations;
        private final long failedValidations;
        private final long classicalTokens;
        private final long hybridTokens;
        private final long downgradeAttacks;
        private final long replayAttacks;
        private final long tamperingAttempts;
        private final long expiredTokens;
        private final long totalValidationTimeNs;
        private final long minValidationTimeNs;
        private final long maxValidationTimeNs;
        private final Map<String, AtomicLong> failureReasons;
        private final long startTimeMs;
        
        MetricsSummary(long totalValidations, long successfulValidations, long failedValidations,
                      long classicalTokens, long hybridTokens, long downgradeAttacks,
                      long replayAttacks, long tamperingAttempts, long expiredTokens,
                      long totalValidationTimeNs, long minValidationTimeNs, long maxValidationTimeNs,
                      Map<String, AtomicLong> failureReasons, long startTimeMs) {
            this.totalValidations = totalValidations;
            this.successfulValidations = successfulValidations;
            this.failedValidations = failedValidations;
            this.classicalTokens = classicalTokens;
            this.hybridTokens = hybridTokens;
            this.downgradeAttacks = downgradeAttacks;
            this.replayAttacks = replayAttacks;
            this.tamperingAttempts = tamperingAttempts;
            this.expiredTokens = expiredTokens;
            this.totalValidationTimeNs = totalValidationTimeNs;
            this.minValidationTimeNs = minValidationTimeNs;
            this.maxValidationTimeNs = maxValidationTimeNs;
            this.failureReasons = failureReasons;
            this.startTimeMs = startTimeMs;
        }
        
        public long getTotalValidations() { return totalValidations; }
        public long getSuccessfulValidations() { return successfulValidations; }
        public long getFailedValidations() { return failedValidations; }
        public long getClassicalTokens() { return classicalTokens; }
        public long getHybridTokens() { return hybridTokens; }
        public long getDowngradeAttacks() { return downgradeAttacks; }
        public long getReplayAttacks() { return replayAttacks; }
        public long getTamperingAttempts() { return tamperingAttempts; }
        public long getExpiredTokens() { return expiredTokens; }
        
        public double getSuccessRate() {
            return totalValidations == 0 ? 0.0 : (successfulValidations * 100.0 / totalValidations);
        }
        
        public double getAverageValidationTimeMs() {
            return totalValidations == 0 ? 0.0 : (totalValidationTimeNs / 1_000_000.0 / totalValidations);
        }
        
        public double getMinValidationTimeMs() {
            return minValidationTimeNs / 1_000_000.0;
        }
        
        public double getMaxValidationTimeMs() {
            return maxValidationTimeNs / 1_000_000.0;
        }
        
        public Map<String, Long> getFailureReasonCounts() {
            Map<String, Long> counts = new ConcurrentHashMap<>();
            failureReasons.forEach((reason, count) -> counts.put(reason, count.get()));
            return counts;
        }
        
        public long getUptimeMs() {
            return System.currentTimeMillis() - startTimeMs;
        }
        
        @Override
        public String toString() {
            return String.format(
                "MetricsSummary[total=%d, success=%d (%.2f%%), failed=%d, " +
                "classical=%d, hybrid=%d, attacks=%d, avgTime=%.2fms]",
                totalValidations, successfulValidations, getSuccessRate(), failedValidations,
                classicalTokens, hybridTokens, 
                (downgradeAttacks + replayAttacks + tamperingAttempts),
                getAverageValidationTimeMs()
            );
        }
    }
}

// Made with Bob
