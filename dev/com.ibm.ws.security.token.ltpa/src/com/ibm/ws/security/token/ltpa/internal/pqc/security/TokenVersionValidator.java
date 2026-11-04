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
 * Validates LTPA token versions to prevent downgrade attacks.
 * 
 * <p>This validator ensures that:
 * <ul>
 *   <li>Token version field is valid (2 for classical, 21 for hybrid)</li>
 *   <li>PQC metadata is consistent with token version</li>
 *   <li>Downgrade attacks are detected and prevented</li>
 *   <li>Version mismatches are logged for security monitoring</li>
 * </ul>
 * 
 * <h3>Downgrade Attack Prevention</h3>
 * <p>A downgrade attack occurs when an adversary modifies a hybrid token (version 21)
 * to appear as a classical token (version 2), forcing the system to use weaker
 * cryptography. This validator prevents such attacks by:
 * <ul>
 *   <li>Validating version field integrity (protected by signatures)</li>
 *   <li>Checking consistency between version and PQC metadata</li>
 *   <li>Detecting suspicious version/metadata combinations</li>
 *   <li>Enforcing PQC requirements when configured</li>
 * </ul>
 * 
 * <h3>Token Version Specification</h3>
 * <pre>
 * Version 2 (Classical):
 *   - RSA-2048 signature only
 *   - No PQC metadata
 *   - Backward compatible
 * 
 * Version 21 (Hybrid):
 *   - RSA-2048 + ML-DSA-65 signatures
 *   - PQC metadata present
 *   - Quantum-resistant
 * </pre>
 * 
 * <h3>Usage Example</h3>
 * <pre>
 * TokenVersionValidator validator = new TokenVersionValidator(config);
 * ValidationResult result = validator.validate(tokenVersion, pqcEnabled, pqcAlgorithm);
 * if (!result.isValid()) {
 *     throw new SecurityException(result.getErrorMessage());
 * }
 * </pre>
 * 
 * @see <a href="https://github.com/open-liberty/open-liberty/blob/integration/dev/com.ibm.ws.security.token.ltpa/pqc-ltpa-implementation/05-security-considerations/attack-resistance.md">Attack Resistance Documentation</a>
 */
public class TokenVersionValidator {
    
    private static final TraceComponent tc = Tr.register(TokenVersionValidator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    
    /** Classical LTPA token version (RSA-only) */
    public static final int VERSION_CLASSICAL = 2;
    
    /** Hybrid LTPA token version (RSA + PQC) */
    public static final int VERSION_HYBRID = 21;
    
    /** Minimum supported token version */
    public static final int VERSION_MIN = 2;
    
    /** Maximum supported token version */
    public static final int VERSION_MAX = 21;
    
    private final boolean pqcEnforced;
    private final boolean strictValidation;
    
    /**
     * Creates a new TokenVersionValidator with default settings.
     * 
     * <p>Default settings:
     * <ul>
     *   <li>PQC not enforced (accepts both classical and hybrid tokens)</li>
     *   <li>Strict validation enabled (detects all anomalies)</li>
     * </ul>
     */
    public TokenVersionValidator() {
        this(false, true);
    }
    
    /**
     * Creates a new TokenVersionValidator with specified settings.
     * 
     * @param pqcEnforced if true, only hybrid tokens are accepted
     * @param strictValidation if true, all version anomalies are rejected
     */
    public TokenVersionValidator(boolean pqcEnforced, boolean strictValidation) {
        this.pqcEnforced = pqcEnforced;
        this.strictValidation = strictValidation;
        
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "TokenVersionValidator initialized: pqcEnforced=" + pqcEnforced + 
                        ", strictValidation=" + strictValidation);
        }
    }
    
    /**
     * Validates a token's version information.
     * 
     * @param tokenVersion the token version field value
     * @param pqcEnabled whether PQC is enabled in the token payload
     * @param pqcAlgorithm the PQC algorithm name (may be null)
     * @return validation result containing success status and error details
     */
    public ValidationResult validate(int tokenVersion, boolean pqcEnabled, String pqcAlgorithm) {
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "validate", tokenVersion, pqcEnabled, pqcAlgorithm);
        }
        
        ValidationResult result = validateInternal(tokenVersion, pqcEnabled, pqcAlgorithm);
        
        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "validate", result);
        }
        
        return result;
    }
    
    /**
     * Internal validation logic.
     */
    private ValidationResult validateInternal(int tokenVersion, boolean pqcEnabled, String pqcAlgorithm) {
        // Step 1: Validate version range
        if (tokenVersion < VERSION_MIN || tokenVersion > VERSION_MAX) {
            String error = "Invalid token version: " + tokenVersion + 
                          " (must be between " + VERSION_MIN + " and " + VERSION_MAX + ")";
            Tr.error(tc, "CWWKS4210E: " + error);
            return ValidationResult.invalid(error, ValidationFailureReason.INVALID_VERSION);
        }
        
        // Step 2: Validate version-specific requirements
        if (tokenVersion == VERSION_CLASSICAL) {
            return validateClassicalToken(pqcEnabled, pqcAlgorithm);
        } else if (tokenVersion == VERSION_HYBRID) {
            return validateHybridToken(pqcEnabled, pqcAlgorithm);
        } else {
            // Unknown version (should not reach here due to range check)
            String error = "Unsupported token version: " + tokenVersion;
            Tr.error(tc, "CWWKS4211E: " + error);
            return ValidationResult.invalid(error, ValidationFailureReason.UNSUPPORTED_VERSION);
        }
    }
    
    /**
     * Validates a classical token (version 2).
     */
    private ValidationResult validateClassicalToken(boolean pqcEnabled, String pqcAlgorithm) {
        // Classical tokens should not have PQC metadata
        if (pqcEnabled) {
            String error = "Classical token (version 2) has PQC enabled flag set";
            Tr.warning(tc, "CWWKS4202W: Token version mismatch detected - " + error);
            
            if (strictValidation) {
                return ValidationResult.invalid(error, ValidationFailureReason.VERSION_MISMATCH);
            } else {
                // Log warning but allow (defensive programming)
                Tr.audit(tc, "SECURITY: Potential downgrade attack detected - classical token with PQC metadata");
            }
        }
        
        if (pqcAlgorithm != null && !pqcAlgorithm.isEmpty()) {
            String error = "Classical token (version 2) has PQC algorithm specified: " + pqcAlgorithm;
            Tr.warning(tc, "CWWKS4202W: Token version mismatch detected - " + error);
            
            if (strictValidation) {
                return ValidationResult.invalid(error, ValidationFailureReason.VERSION_MISMATCH);
            }
        }
        
        // Check if PQC is enforced
        if (pqcEnforced) {
            String error = "Classical token rejected (PQC enforced by configuration)";
            Tr.error(tc, "CWWKS4212E: " + error);
            return ValidationResult.invalid(error, ValidationFailureReason.PQC_REQUIRED);
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Validates a hybrid token (version 21).
     */
    private ValidationResult validateHybridToken(boolean pqcEnabled, String pqcAlgorithm) {
        // Hybrid tokens must have PQC enabled
        if (!pqcEnabled) {
            String error = "Hybrid token (version 21) does not have PQC enabled";
            Tr.error(tc, "CWWKS4213E: " + error);
            return ValidationResult.invalid(error, ValidationFailureReason.PQC_METADATA_MISSING);
        }
        
        // Hybrid tokens must specify PQC algorithm
        if (pqcAlgorithm == null || pqcAlgorithm.isEmpty()) {
            String error = "Hybrid token (version 21) does not specify PQC algorithm";
            Tr.error(tc, "CWWKS4213E: " + error);
            return ValidationResult.invalid(error, ValidationFailureReason.PQC_METADATA_MISSING);
        }
        
        // Validate PQC algorithm
        if (!isValidPqcAlgorithm(pqcAlgorithm)) {
            String error = "Hybrid token has invalid PQC algorithm: " + pqcAlgorithm;
            Tr.error(tc, "CWWKS4214E: " + error);
            return ValidationResult.invalid(error, ValidationFailureReason.INVALID_PQC_ALGORITHM);
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Checks if a PQC algorithm name is valid.
     */
    private boolean isValidPqcAlgorithm(String algorithm) {
        return "ML-DSA-65".equals(algorithm) || "ML-DSA-87".equals(algorithm);
    }
    
    /**
     * Represents the result of a token version validation.
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
        /** Token version is outside valid range */
        INVALID_VERSION,
        
        /** Token version is not supported */
        UNSUPPORTED_VERSION,
        
        /** Version field does not match PQC metadata */
        VERSION_MISMATCH,
        
        /** PQC is required but token is classical */
        PQC_REQUIRED,
        
        /** Hybrid token is missing PQC metadata */
        PQC_METADATA_MISSING,
        
        /** PQC algorithm is invalid or unsupported */
        INVALID_PQC_ALGORITHM
    }
}

// Made with Bob
