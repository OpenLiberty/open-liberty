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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.token.ltpa.internal.TraceConstants;

/**
 * Validates LTPA token expiration to prevent replay attacks.
 * 
 * <p>This validator ensures that:
 * <ul>
 *   <li>Tokens have valid creation and expiration timestamps</li>
 *   <li>Tokens are not expired</li>
 *   <li>Tokens are not used before their creation time (clock skew tolerance)</li>
 *   <li>Token lifetime is within acceptable bounds</li>
 * </ul>
 * 
 * <h3>Replay Attack Prevention</h3>
 * <p>A replay attack occurs when an adversary captures a valid token and reuses it
 * later. Token expiration provides time-based protection:
 * <ul>
 *   <li><b>Limited Lifetime</b>: Tokens expire after a configured duration</li>
 *   <li><b>Timestamp Validation</b>: Creation and expiration times are verified</li>
 *   <li><b>Clock Skew Tolerance</b>: Accounts for time differences between servers</li>
 *   <li><b>Audit Logging</b>: Expired token usage is logged for monitoring</li>
 * </ul>
 * 
 * <h3>Validation Rules</h3>
 * <pre>
 * Valid Token:
 *   creationTime <= currentTime <= expirationTime
 *   
 * With Clock Skew Tolerance:
 *   (creationTime - clockSkew) <= currentTime <= (expirationTime + clockSkew)
 *   
 * Lifetime Validation:
 *   (expirationTime - creationTime) <= maxLifetime
 * </pre>
 * 
 * <h3>Configuration Parameters</h3>
 * <ul>
 *   <li><b>clockSkewTolerance</b>: Maximum allowed time difference (default: 5 minutes)</li>
 *   <li><b>maxTokenLifetime</b>: Maximum token lifetime (default: 2 hours)</li>
 *   <li><b>strictMode</b>: Reject tokens with suspicious timestamps (default: true)</li>
 * </ul>
 * 
 * <h3>Usage Example</h3>
 * <pre>
 * TokenExpirationValidator validator = new TokenExpirationValidator();
 * 
 * long creationTime = token.getCreationTime();
 * long expirationTime = token.getExpirationTime();
 * 
 * ValidationResult result = validator.validate(creationTime, expirationTime);
 * if (!result.isValid()) {
 *     throw new TokenExpiredException(result.getErrorMessage());
 * }
 * </pre>
 * 
 * @see <a href="https://github.com/open-liberty/open-liberty/blob/integration/dev/com.ibm.ws.security.token.ltpa/pqc-ltpa-implementation/05-security-considerations/attack-resistance.md">Attack Resistance Documentation</a>
 */
public class TokenExpirationValidator {
    
    private static final TraceComponent tc = Tr.register(TokenExpirationValidator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    
    /** Default clock skew tolerance: 5 minutes (300,000 milliseconds) */
    public static final long DEFAULT_CLOCK_SKEW_MS = 5 * 60 * 1000L;
    
    /** Default maximum token lifetime: 2 hours (7,200,000 milliseconds) */
    public static final long DEFAULT_MAX_LIFETIME_MS = 2 * 60 * 60 * 1000L;
    
    /** Minimum acceptable token lifetime: 1 minute (60,000 milliseconds) */
    public static final long MIN_LIFETIME_MS = 60 * 1000L;
    
    /** Maximum acceptable token lifetime: 24 hours (86,400,000 milliseconds) */
    public static final long MAX_LIFETIME_MS = 24 * 60 * 60 * 1000L;
    
    private final long clockSkewToleranceMs;
    private final long maxTokenLifetimeMs;
    private final boolean strictMode;
    
    /**
     * Creates a new TokenExpirationValidator with default settings.
     * 
     * <p>Default settings:
     * <ul>
     *   <li>Clock skew tolerance: 5 minutes</li>
     *   <li>Max token lifetime: 2 hours</li>
     *   <li>Strict mode: enabled</li>
     * </ul>
     */
    public TokenExpirationValidator() {
        this(DEFAULT_CLOCK_SKEW_MS, DEFAULT_MAX_LIFETIME_MS, true);
    }
    
    /**
     * Creates a new TokenExpirationValidator with specified settings.
     * 
     * @param clockSkewToleranceMs clock skew tolerance in milliseconds
     * @param maxTokenLifetimeMs maximum token lifetime in milliseconds
     * @param strictMode if true, reject tokens with suspicious timestamps
     * @throws IllegalArgumentException if parameters are invalid
     */
    public TokenExpirationValidator(long clockSkewToleranceMs, long maxTokenLifetimeMs, boolean strictMode) {
        if (clockSkewToleranceMs < 0) {
            throw new IllegalArgumentException("Clock skew tolerance cannot be negative");
        }
        
        if (maxTokenLifetimeMs < MIN_LIFETIME_MS || maxTokenLifetimeMs > MAX_LIFETIME_MS) {
            throw new IllegalArgumentException("Max token lifetime must be between " + 
                                             MIN_LIFETIME_MS + " and " + MAX_LIFETIME_MS + " ms");
        }
        
        this.clockSkewToleranceMs = clockSkewToleranceMs;
        this.maxTokenLifetimeMs = maxTokenLifetimeMs;
        this.strictMode = strictMode;
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "TokenExpirationValidator initialized: clockSkew=" + clockSkewToleranceMs + 
                        "ms, maxLifetime=" + maxTokenLifetimeMs + "ms, strictMode=" + strictMode);
        }
    }
    
    /**
     * Validates token expiration timestamps.
     * 
     * @param creationTimeMs token creation time in milliseconds since epoch
     * @param expirationTimeMs token expiration time in milliseconds since epoch
     * @return validation result containing success status and error details
     */
    public ValidationResult validate(long creationTimeMs, long expirationTimeMs) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "validate", creationTimeMs, expirationTimeMs);
        }
        
        ValidationResult result = validateInternal(creationTimeMs, expirationTimeMs);
        
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "validate", result);
        }
        
        return result;
    }
    
    /**
     * Internal validation logic.
     */
    private ValidationResult validateInternal(long creationTimeMs, long expirationTimeMs) {
        long currentTimeMs = System.currentTimeMillis();
        
        // Step 1: Validate timestamp sanity
        if (creationTimeMs <= 0) {
            String error = "Invalid creation time: " + creationTimeMs;
            Tr.error(tc, "CWWKS4240E: " + error);
            return ValidationResult.invalid(error, ValidationFailureReason.INVALID_CREATION_TIME);
        }
        
        if (expirationTimeMs <= 0) {
            String error = "Invalid expiration time: " + expirationTimeMs;
            Tr.error(tc, "CWWKS4241E: " + error);
            return ValidationResult.invalid(error, ValidationFailureReason.INVALID_EXPIRATION_TIME);
        }
        
        // Step 2: Validate creation time is before expiration time
        if (creationTimeMs >= expirationTimeMs) {
            String error = "Creation time (" + creationTimeMs + ") is not before expiration time (" + 
                          expirationTimeMs + ")";
            Tr.error(tc, "CWWKS4242E: " + error);
            return ValidationResult.invalid(error, ValidationFailureReason.INVALID_TIME_ORDER);
        }
        
        // Step 3: Validate token lifetime
        long tokenLifetimeMs = expirationTimeMs - creationTimeMs;
        
        if (tokenLifetimeMs < MIN_LIFETIME_MS) {
            String error = "Token lifetime (" + tokenLifetimeMs + "ms) is too short (min: " + 
                          MIN_LIFETIME_MS + "ms)";
            Tr.error(tc, "CWWKS4243E: " + error);
            return ValidationResult.invalid(error, ValidationFailureReason.LIFETIME_TOO_SHORT);
        }
        
        if (tokenLifetimeMs > maxTokenLifetimeMs) {
            String error = "Token lifetime (" + tokenLifetimeMs + "ms) exceeds maximum (" + 
                          maxTokenLifetimeMs + "ms)";
            Tr.error(tc, "CWWKS4244E: " + error);
            
            if (strictMode) {
                return ValidationResult.invalid(error, ValidationFailureReason.LIFETIME_TOO_LONG);
            } else {
                Tr.warning(tc, "CWWKS4244W: " + error + " (allowed in non-strict mode)");
            }
        }
        
        // Step 4: Check if token is not yet valid (with clock skew tolerance)
        long effectiveCreationTime = creationTimeMs - clockSkewToleranceMs;
        
        if (currentTimeMs < effectiveCreationTime) {
            long timeDiff = effectiveCreationTime - currentTimeMs;
            String error = "Token not yet valid (creation time is " + timeDiff + "ms in the future)";
            Tr.error(tc, "CWWKS4245E: " + error);
            Tr.audit(tc, "SECURITY: Token used before creation time - possible clock skew or replay attack");
            return ValidationResult.invalid(error, ValidationFailureReason.NOT_YET_VALID);
        }
        
        // Step 5: Check if token is expired (with clock skew tolerance)
        long effectiveExpirationTime = expirationTimeMs + clockSkewToleranceMs;
        
        if (currentTimeMs > effectiveExpirationTime) {
            long timeDiff = currentTimeMs - effectiveExpirationTime;
            String error = "Token expired " + timeDiff + "ms ago";
            Tr.error(tc, "CWWKS4246E: " + error);
            Tr.audit(tc, "SECURITY: Expired token usage detected - possible replay attack");
            return ValidationResult.invalid(error, ValidationFailureReason.EXPIRED);
        }
        
        // Step 6: Warn if token is close to expiration
        long timeUntilExpiration = expirationTimeMs - currentTimeMs;
        if (timeUntilExpiration < 60000) { // Less than 1 minute
            Tr.warning(tc, "CWWKS4247W: Token expires in " + timeUntilExpiration + "ms");
        }
        
        // Token is valid
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Token expiration validated: lifetime=" + tokenLifetimeMs + 
                        "ms, remaining=" + timeUntilExpiration + "ms");
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Checks if a token is expired (without clock skew tolerance).
     * 
     * <p>This is a strict check used for security auditing.
     * 
     * @param expirationTimeMs token expiration time in milliseconds since epoch
     * @return true if token is expired, false otherwise
     */
    public boolean isExpired(long expirationTimeMs) {
        long currentTimeMs = System.currentTimeMillis();
        return currentTimeMs > expirationTimeMs;
    }
    
    /**
     * Calculates the remaining lifetime of a token in milliseconds.
     * 
     * @param expirationTimeMs token expiration time in milliseconds since epoch
     * @return remaining lifetime in milliseconds (negative if expired)
     */
    public long getRemainingLifetimeMs(long expirationTimeMs) {
        long currentTimeMs = System.currentTimeMillis();
        return expirationTimeMs - currentTimeMs;
    }
    
    /**
     * Calculates the age of a token in milliseconds.
     * 
     * @param creationTimeMs token creation time in milliseconds since epoch
     * @return token age in milliseconds (negative if creation time is in the future)
     */
    public long getTokenAgeMs(long creationTimeMs) {
        long currentTimeMs = System.currentTimeMillis();
        return currentTimeMs - creationTimeMs;
    }
    
    /**
     * Returns the configured clock skew tolerance in milliseconds.
     */
    public long getClockSkewToleranceMs() {
        return clockSkewToleranceMs;
    }
    
    /**
     * Returns the configured maximum token lifetime in milliseconds.
     */
    public long getMaxTokenLifetimeMs() {
        return maxTokenLifetimeMs;
    }
    
    /**
     * Returns whether strict mode is enabled.
     */
    public boolean isStrictMode() {
        return strictMode;
    }
    
    /**
     * Represents the result of a token expiration validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final ValidationFailureReason failureReason;
        
        private ValidationResult(boolean valid, String errorMessage, ValidationFailureReason failureReason) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.failureReason = failureReason;
        }
        
        /**
         * Creates a successful validation result.
         */
        public static ValidationResult valid() {
            return new ValidationResult(true, null, null);
        }
        
        /**
         * Creates a failed validation result.
         */
        public static ValidationResult invalid(String errorMessage, ValidationFailureReason reason) {
            return new ValidationResult(false, errorMessage, reason);
        }
        
        /**
         * Returns whether the validation succeeded.
         */
        public boolean isValid() {
            return valid;
        }
        
        /**
         * Returns the error message (null if validation succeeded).
         */
        public String getErrorMessage() {
            return errorMessage;
        }
        
        /**
         * Returns the failure reason (null if validation succeeded).
         */
        public ValidationFailureReason getFailureReason() {
            return failureReason;
        }
        
        @Override
        public String toString() {
            if (valid) {
                return "ValidationResult[valid=true]";
            } else {
                return "ValidationResult[valid=false, reason=" + failureReason + 
                       ", message=" + errorMessage + "]";
            }
        }
    }
    
    /**
     * Enumeration of validation failure reasons.
     */
    public enum ValidationFailureReason {
        /** Creation time is invalid (zero or negative) */
        INVALID_CREATION_TIME,
        
        /** Expiration time is invalid (zero or negative) */
        INVALID_EXPIRATION_TIME,
        
        /** Creation time is not before expiration time */
        INVALID_TIME_ORDER,
        
        /** Token lifetime is too short */
        LIFETIME_TOO_SHORT,
        
        /** Token lifetime exceeds maximum allowed */
        LIFETIME_TOO_LONG,
        
        /** Token is not yet valid (creation time in future) */
        NOT_YET_VALID,
        
        /** Token has expired */
        EXPIRED
    }
}

// Made with Bob
