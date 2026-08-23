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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Detects downgrade attacks where an attacker attempts to force the use of
 * weaker cryptographic algorithms by manipulating token versions or configuration.
 * 
 * <p>This detector monitors for suspicious patterns such as:
 * <ul>
 *   <li>Sudden switches from hybrid (v21) to classical (v2) tokens</li>
 *   <li>Repeated attempts to use classical tokens when hybrid is configured</li>
 *   <li>Version rollback attempts within the same session</li>
 *   <li>Mismatched token versions across a token chain</li>
 * </ul>
 * 
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li>Tracks token version history per user/session to detect anomalies</li>
 *   <li>Implements rate limiting to prevent false positives from legitimate transitions</li>
 *   <li>Provides configurable thresholds for attack detection sensitivity</li>
 *   <li>Integrates with SecurityMetricsCollector for centralized monitoring</li>
 * </ul>
 * 
 * <p><b>Thread Safety:</b> This class is thread-safe and uses concurrent data structures
 * for tracking attack patterns across multiple threads.
 * 
 * <p><b>Performance:</b> Uses efficient in-memory tracking with automatic cleanup of
 * stale entries to prevent memory leaks.
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 2026
 */
public class DowngradeAttackDetector {
    
    private static final TraceComponent tc = Tr.register(DowngradeAttackDetector.class);
    
    /**
     * Default threshold for detecting downgrade attacks.
     * If more than this many classical tokens are seen after hybrid tokens
     * within the time window, a downgrade attack is suspected.
     */
    private static final int DEFAULT_DOWNGRADE_THRESHOLD = 3;
    
    /**
     * Default time window (in milliseconds) for tracking downgrade patterns.
     * Entries older than this are automatically cleaned up.
     */
    private static final long DEFAULT_TIME_WINDOW_MS = 300000; // 5 minutes
    
    /**
     * Maximum number of entries to track before triggering cleanup.
     * Prevents unbounded memory growth.
     */
    private static final int MAX_TRACKED_ENTRIES = 10000;
    
    // Configuration
    private final int downgradeThreshold;
    private final long timeWindowMs;
    private final SecurityMetricsCollector metricsCollector;
    
    // Tracking data structures
    private final ConcurrentHashMap<String, TokenVersionHistory> versionHistory;
    private final AtomicLong totalDowngradeAttempts;
    private final AtomicLong totalDowngradeDetections;
    private final AtomicLong lastCleanupTime;
    
    /**
     * Creates a new DowngradeAttackDetector with default configuration.
     * 
     * @param metricsCollector the metrics collector for recording attack events
     * @throws IllegalArgumentException if metricsCollector is null
     */
    public DowngradeAttackDetector(SecurityMetricsCollector metricsCollector) {
        this(metricsCollector, DEFAULT_DOWNGRADE_THRESHOLD, DEFAULT_TIME_WINDOW_MS);
    }
    
    /**
     * Creates a new DowngradeAttackDetector with custom configuration.
     * 
     * @param metricsCollector the metrics collector for recording attack events
     * @param downgradeThreshold the number of downgrades to trigger detection
     * @param timeWindowMs the time window for tracking downgrades (milliseconds)
     * @throws IllegalArgumentException if metricsCollector is null or thresholds are invalid
     */
    public DowngradeAttackDetector(SecurityMetricsCollector metricsCollector,
                                   int downgradeThreshold,
                                   long timeWindowMs) {
        if (metricsCollector == null) {
            throw new IllegalArgumentException("SecurityMetricsCollector cannot be null");
        }
        if (downgradeThreshold < 1) {
            throw new IllegalArgumentException("Downgrade threshold must be at least 1");
        }
        if (timeWindowMs < 1000) {
            throw new IllegalArgumentException("Time window must be at least 1000ms");
        }
        
        this.metricsCollector = metricsCollector;
        this.downgradeThreshold = downgradeThreshold;
        this.timeWindowMs = timeWindowMs;
        this.versionHistory = new ConcurrentHashMap<>();
        this.totalDowngradeAttempts = new AtomicLong(0);
        this.totalDowngradeDetections = new AtomicLong(0);
        this.lastCleanupTime = new AtomicLong(System.currentTimeMillis());
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "DowngradeAttackDetector initialized with threshold=" + 
                     downgradeThreshold + ", timeWindow=" + timeWindowMs + "ms");
        }
    }
    
    /**
     * Checks if a token version transition represents a potential downgrade attack.
     * 
     * <p>This method tracks token version history per user/session and detects
     * suspicious patterns such as repeated downgrades from hybrid to classical tokens.
     * 
     * @param userId the user identifier (e.g., username or session ID)
     * @param currentVersion the current token version being validated
     * @param previousVersion the previous token version seen for this user (may be null)
     * @return true if a downgrade attack is detected, false otherwise
     * @throws IllegalArgumentException if userId or currentVersion is null
     */
    public boolean detectDowngrade(String userId, int currentVersion, Integer previousVersion) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Perform periodic cleanup
        performPeriodicCleanup(currentTime);
        
        // Get or create version history for this user
        TokenVersionHistory history = versionHistory.computeIfAbsent(userId, 
            k -> new TokenVersionHistory());
        
        // Check if this is a downgrade
        boolean isDowngrade = isVersionDowngrade(currentVersion, previousVersion, history);
        
        if (isDowngrade) {
            totalDowngradeAttempts.incrementAndGet();
            history.recordDowngrade(currentTime);
            
            // Check if downgrade threshold exceeded
            int recentDowngrades = history.getRecentDowngradeCount(currentTime, timeWindowMs);
            
            if (recentDowngrades >= downgradeThreshold) {
                totalDowngradeDetections.incrementAndGet();
                
                // Record attack detection
                metricsCollector.recordAttackDetection("DOWNGRADE_ATTACK", userId);
                
                if (tc.isWarningEnabled()) {
                    Tr.warning(tc, "CWWKS4200W: Downgrade attack detected for user {0}. " +
                              "Detected {1} downgrades within {2}ms window (threshold: {3})",
                              userId, recentDowngrades, timeWindowMs, downgradeThreshold);
                }
                
                return true;
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Downgrade detected for user " + userId + 
                            " but below threshold (" + recentDowngrades + "/" + 
                            downgradeThreshold + ")");
                }
            }
        }
        
        // Update version history
        history.recordVersion(currentVersion, currentTime);
        
        return false;
    }
    
    /**
     * Determines if a version transition represents a downgrade.
     * 
     * @param currentVersion the current token version
     * @param previousVersion the previous token version (may be null)
     * @param history the version history for this user
     * @return true if this is a downgrade, false otherwise
     */
    private boolean isVersionDowngrade(int currentVersion, Integer previousVersion, 
                                       TokenVersionHistory history) {
        // If no previous version, check against history
        if (previousVersion == null) {
            Integer lastVersion = history.getLastVersion();
            if (lastVersion == null) {
                return false; // First token for this user
            }
            previousVersion = lastVersion;
        }
        
        // Version 21 (hybrid) to version 2 (classical) is a downgrade
        if (previousVersion == 21 && currentVersion == 2) {
            return true;
        }
        
        // Any decrease in version number is suspicious
        if (currentVersion < previousVersion) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Performs periodic cleanup of stale entries to prevent memory leaks.
     * 
     * @param currentTime the current timestamp
     */
    private void performPeriodicCleanup(long currentTime) {
        long lastCleanup = lastCleanupTime.get();
        
        // Cleanup every minute or when max entries exceeded
        if (currentTime - lastCleanup > 60000 || versionHistory.size() > MAX_TRACKED_ENTRIES) {
            if (lastCleanupTime.compareAndSet(lastCleanup, currentTime)) {
                cleanupStaleEntries(currentTime);
            }
        }
    }
    
    /**
     * Removes stale entries from the version history map.
     * 
     * @param currentTime the current timestamp
     */
    private void cleanupStaleEntries(long currentTime) {
        int removedCount = 0;
        
        for (String userId : versionHistory.keySet()) {
            TokenVersionHistory history = versionHistory.get(userId);
            if (history != null && history.isStale(currentTime, timeWindowMs * 2)) {
                if (versionHistory.remove(userId) != null) {
                    removedCount++;
                }
            }
        }
        
        if (tc.isDebugEnabled() && removedCount > 0) {
            Tr.debug(tc, "Cleaned up " + removedCount + " stale version history entries");
        }
    }
    
    /**
     * Clears all tracked version history.
     * This method is primarily for testing purposes.
     */
    public void clearHistory() {
        versionHistory.clear();
        totalDowngradeAttempts.set(0);
        totalDowngradeDetections.set(0);
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Version history cleared");
        }
    }
    
    /**
     * Gets the total number of downgrade attempts detected.
     * 
     * @return the total downgrade attempt count
     */
    public long getTotalDowngradeAttempts() {
        return totalDowngradeAttempts.get();
    }
    
    /**
     * Gets the total number of downgrade attacks detected (threshold exceeded).
     * 
     * @return the total downgrade detection count
     */
    public long getTotalDowngradeDetections() {
        return totalDowngradeDetections.get();
    }
    
    /**
     * Gets the current number of tracked users.
     * 
     * @return the number of users being tracked
     */
    public int getTrackedUserCount() {
        return versionHistory.size();
    }
    
    /**
     * Tracks token version history for a single user/session.
     * 
     * <p>This class maintains a sliding window of version transitions
     * to detect downgrade patterns.
     */
    private static class TokenVersionHistory {
        private volatile Integer lastVersion;
        private volatile long lastAccessTime;
        private final AtomicLong downgradeCount;
        private final ConcurrentHashMap<Long, Integer> versionTimeline;
        
        public TokenVersionHistory() {
            this.lastVersion = null;
            this.lastAccessTime = System.currentTimeMillis();
            this.downgradeCount = new AtomicLong(0);
            this.versionTimeline = new ConcurrentHashMap<>();
        }
        
        public void recordVersion(int version, long timestamp) {
            this.lastVersion = version;
            this.lastAccessTime = timestamp;
            this.versionTimeline.put(timestamp, version);
            
            // Cleanup old timeline entries
            if (versionTimeline.size() > 100) {
                long cutoffTime = timestamp - 600000; // Keep last 10 minutes
                versionTimeline.entrySet().removeIf(e -> e.getKey() < cutoffTime);
            }
        }
        
        public void recordDowngrade(long timestamp) {
            downgradeCount.incrementAndGet();
            lastAccessTime = timestamp;
        }
        
        public int getRecentDowngradeCount(long currentTime, long timeWindow) {
            long cutoffTime = currentTime - timeWindow;
            int count = 0;
            
            // Count downgrades in the time window
            Integer prevVersion = null;
            for (Long timestamp : versionTimeline.keySet().stream()
                    .sorted().toArray(Long[]::new)) {
                if (timestamp < cutoffTime) {
                    prevVersion = versionTimeline.get(timestamp);
                    continue;
                }
                
                Integer currentVersion = versionTimeline.get(timestamp);
                if (prevVersion != null && currentVersion < prevVersion) {
                    count++;
                }
                prevVersion = currentVersion;
            }
            
            return count;
        }
        
        public Integer getLastVersion() {
            return lastVersion;
        }
        
        public boolean isStale(long currentTime, long maxAge) {
            return (currentTime - lastAccessTime) > maxAge;
        }
    }
}

// Made with Bob
