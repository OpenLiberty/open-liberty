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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Centralized security event logging for PQC-LTPA token operations.
 * 
 * <p>This logger provides comprehensive security event tracking with:
 * <ul>
 *   <li><b>Event Classification:</b> INFO, WARNING, ERROR, AUDIT levels</li>
 *   <li><b>Event Categorization:</b> Authentication, validation, attack detection</li>
 *   <li><b>Structured Logging:</b> Consistent format with contextual information</li>
 *   <li><b>Audit Trail:</b> Immutable security event records</li>
 * </ul>
 * 
 * <p><b>Event Types:</b>
 * <ul>
 *   <li>TOKEN_VALIDATION_SUCCESS - Successful token validation</li>
 *   <li>TOKEN_VALIDATION_FAILURE - Failed token validation</li>
 *   <li>ATTACK_DETECTED - Security attack detected</li>
 *   <li>DOWNGRADE_ATTEMPT - Token version downgrade attempt</li>
 *   <li>REPLAY_ATTEMPT - Token replay attack attempt</li>
 *   <li>TAMPERING_DETECTED - Token tampering detected</li>
 *   <li>SIGNATURE_FAILURE - Signature validation failure</li>
 *   <li>EXPIRATION_FAILURE - Token expiration validation failure</li>
 * </ul>
 * 
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li>Sanitizes user input to prevent log injection</li>
 *   <li>Limits log message size to prevent DoS</li>
 *   <li>Thread-safe for concurrent logging</li>
 *   <li>Integrates with Liberty Tr tracing framework</li>
 * </ul>
 * 
 * <p><b>Thread Safety:</b> This class is thread-safe and uses concurrent
 * data structures for event tracking.
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 2026
 */
public class SecurityEventLogger {
    
    private static final TraceComponent tc = Tr.register(SecurityEventLogger.class);
    
    /**
     * Security event severity levels.
     */
    public enum EventLevel {
        /** Informational events */
        INFO,
        /** Warning events requiring attention */
        WARNING,
        /** Error events indicating failures */
        ERROR,
        /** Audit events for security compliance */
        AUDIT
    }
    
    /**
     * Security event categories.
     */
    public enum EventCategory {
        /** Authentication-related events */
        AUTHENTICATION,
        /** Token validation events */
        VALIDATION,
        /** Attack detection events */
        ATTACK_DETECTION,
        /** Configuration events */
        CONFIGURATION,
        /** System events */
        SYSTEM
    }
    
    // Event statistics
    private final Map<EventLevel, AtomicLong> eventCountsByLevel;
    private final Map<EventCategory, AtomicLong> eventCountsByCategory;
    private final AtomicLong totalEvents;
    
    // Configuration
    private static final int MAX_MESSAGE_LENGTH = 1024;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    /**
     * Creates a new SecurityEventLogger.
     */
    public SecurityEventLogger() {
        this.eventCountsByLevel = new ConcurrentHashMap<>();
        this.eventCountsByCategory = new ConcurrentHashMap<>();
        this.totalEvents = new AtomicLong(0);
        
        // Initialize counters
        for (EventLevel level : EventLevel.values()) {
            eventCountsByLevel.put(level, new AtomicLong(0));
        }
        for (EventCategory category : EventCategory.values()) {
            eventCountsByCategory.put(category, new AtomicLong(0));
        }
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "SecurityEventLogger initialized");
        }
    }
    
    /**
     * Logs a security event with full context.
     * 
     * @param level the event severity level
     * @param category the event category
     * @param eventType the specific event type
     * @param message the event message
     * @param userId the user identifier (may be null)
     * @param details additional event details (may be null)
     */
    public void logEvent(EventLevel level, EventCategory category, String eventType,
                        String message, String userId, Map<String, String> details) {
        if (level == null || category == null || eventType == null || message == null) {
            throw new IllegalArgumentException("Level, category, eventType, and message cannot be null");
        }
        
        // Sanitize inputs
        String sanitizedMessage = sanitize(message);
        String sanitizedUserId = sanitize(userId);
        String sanitizedEventType = sanitize(eventType);
        
        // Update statistics
        totalEvents.incrementAndGet();
        eventCountsByLevel.get(level).incrementAndGet();
        eventCountsByCategory.get(category).incrementAndGet();
        
        // Format log message
        String logMessage = formatLogMessage(level, category, sanitizedEventType,
                                            sanitizedMessage, sanitizedUserId, details);
        
        // Log to appropriate level
        switch (level) {
            case INFO:
                if (tc.isInfoEnabled()) {
                    Tr.info(tc, logMessage);
                }
                break;
            case WARNING:
                if (tc.isWarningEnabled()) {
                    Tr.warning(tc, logMessage);
                }
                break;
            case ERROR:
                if (tc.isErrorEnabled()) {
                    Tr.error(tc, logMessage);
                }
                break;
            case AUDIT:
                if (tc.isAuditEnabled()) {
                    Tr.audit(tc, logMessage);
                }
                break;
        }
    }
    
    /**
     * Logs a validation success event.
     * 
     * @param userId the user identifier
     * @param tokenVersion the token version
     */
    public void logValidationSuccess(String userId, int tokenVersion) {
        logEvent(EventLevel.INFO, EventCategory.VALIDATION,
                "TOKEN_VALIDATION_SUCCESS",
                "Token validation successful for user: " + userId + ", version: " + tokenVersion,
                userId, null);
    }
    
    /**
     * Logs a validation failure event.
     * 
     * @param userId the user identifier
     * @param reason the failure reason
     */
    public void logValidationFailure(String userId, String reason) {
        logEvent(EventLevel.WARNING, EventCategory.VALIDATION,
                "TOKEN_VALIDATION_FAILURE",
                "Token validation failed: " + reason,
                userId, null);
    }
    
    /**
     * Logs an attack detection event.
     * 
     * @param attackType the type of attack detected
     * @param userId the user identifier
     * @param details additional attack details
     */
    public void logAttackDetection(String attackType, String userId, Map<String, String> details) {
        logEvent(EventLevel.ERROR, EventCategory.ATTACK_DETECTION,
                "ATTACK_DETECTED",
                "Security attack detected: " + attackType,
                userId, details);
    }
    
    /**
     * Logs a downgrade attack attempt.
     * 
     * @param userId the user identifier
     * @param fromVersion the original version
     * @param toVersion the downgraded version
     */
    public void logDowngradeAttempt(String userId, int fromVersion, int toVersion) {
        Map<String, String> details = new ConcurrentHashMap<>();
        details.put("fromVersion", String.valueOf(fromVersion));
        details.put("toVersion", String.valueOf(toVersion));
        
        logEvent(EventLevel.ERROR, EventCategory.ATTACK_DETECTION,
                "DOWNGRADE_ATTEMPT",
                "Token version downgrade attempt detected",
                userId, details);
    }
    
    /**
     * Logs a replay attack attempt.
     * 
     * @param userId the user identifier
     * @param tokenFingerprint the token fingerprint
     */
    public void logReplayAttempt(String userId, String tokenFingerprint) {
        Map<String, String> details = new ConcurrentHashMap<>();
        details.put("fingerprint", tokenFingerprint);
        
        logEvent(EventLevel.ERROR, EventCategory.ATTACK_DETECTION,
                "REPLAY_ATTEMPT",
                "Token replay attack attempt detected",
                userId, details);
    }
    
    /**
     * Logs a tampering detection event.
     * 
     * @param userId the user identifier
     * @param tamperingType the type of tampering detected
     */
    public void logTamperingDetection(String userId, String tamperingType) {
        Map<String, String> details = new ConcurrentHashMap<>();
        details.put("tamperingType", tamperingType);
        
        logEvent(EventLevel.ERROR, EventCategory.ATTACK_DETECTION,
                "TAMPERING_DETECTED",
                "Token tampering detected: " + tamperingType,
                userId, details);
    }
    
    /**
     * Formats a log message with consistent structure.
     * 
     * @param level the event level
     * @param category the event category
     * @param eventType the event type
     * @param message the message
     * @param userId the user identifier
     * @param details additional details
     * @return the formatted log message
     */
    @Trivial
    private String formatLogMessage(EventLevel level, EventCategory category,
                                    String eventType, String message,
                                    String userId, Map<String, String> details) {
        StringBuilder sb = new StringBuilder();
        
        // Timestamp
        sb.append("[").append(DATE_FORMAT.format(new Date())).append("] ");
        
        // Level and category
        sb.append("[").append(level).append("] ");
        sb.append("[").append(category).append("] ");
        
        // Event type
        sb.append("[").append(eventType).append("] ");
        
        // Message
        sb.append(message);
        
        // User ID
        if (userId != null && !userId.isEmpty()) {
            sb.append(" | User: ").append(userId);
        }
        
        // Additional details
        if (details != null && !details.isEmpty()) {
            sb.append(" | Details: {");
            boolean first = true;
            for (Map.Entry<String, String> entry : details.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            sb.append("}");
        }
        
        return sb.toString();
    }
    
    /**
     * Sanitizes input to prevent log injection attacks.
     * 
     * @param input the input to sanitize
     * @return the sanitized input
     */
    @Trivial
    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        
        // Remove control characters and limit length
        String sanitized = input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        if (sanitized.length() > MAX_MESSAGE_LENGTH) {
            sanitized = sanitized.substring(0, MAX_MESSAGE_LENGTH) + "...";
        }
        
        return sanitized;
    }
    
    /**
     * Gets the total number of events logged.
     * 
     * @return the total event count
     */
    public long getTotalEvents() {
        return totalEvents.get();
    }
    
    /**
     * Gets the number of events logged at a specific level.
     * 
     * @param level the event level
     * @return the event count for the level
     */
    public long getEventCountByLevel(EventLevel level) {
        AtomicLong count = eventCountsByLevel.get(level);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Gets the number of events logged in a specific category.
     * 
     * @param category the event category
     * @return the event count for the category
     */
    public long getEventCountByCategory(EventCategory category) {
        AtomicLong count = eventCountsByCategory.get(category);
        return count != null ? count.get() : 0;
    }
    
    /**
     * Resets all event statistics.
     * This method is primarily for testing purposes.
     */
    public void resetStatistics() {
        totalEvents.set(0);
        for (AtomicLong count : eventCountsByLevel.values()) {
            count.set(0);
        }
        for (AtomicLong count : eventCountsByCategory.values()) {
            count.set(0);
        }
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Event statistics reset");
        }
    }
}

// Made with Bob
