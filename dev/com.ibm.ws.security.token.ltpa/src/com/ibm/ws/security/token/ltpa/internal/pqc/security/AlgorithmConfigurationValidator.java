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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.token.ltpa.internal.TraceConstants;

/**
 * Validates configured cryptographic algorithms for PQC-LTPA.
 *
 * <p>This validator performs configuration-time checks for:
 * <ul>
 *   <li>Approved hash algorithms</li>
 *   <li>Approved signature algorithms</li>
 *   <li>Hybrid classical + PQC combinations</li>
 *   <li>FIPS mode restrictions and enforcement</li>
 *   <li>Deprecation and migration warnings</li>
 * </ul>
 *
 * <p>It complements {@link FIPSAlgorithmValidator} by applying higher-level
 * configuration rules rather than validating individual runtime cryptographic
 * operations.
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.
 */
public class AlgorithmConfigurationValidator {

    private static final TraceComponent tc = Tr.register(AlgorithmConfigurationValidator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    /** Default classical signature algorithm. */
    public static final String DEFAULT_CLASSICAL_SIGNATURE_ALGORITHM = "SHA256withRSA";

    /** Default PQC signature algorithm. */
    public static final String DEFAULT_PQC_SIGNATURE_ALGORITHM = "ML-DSA-65";

    /** Default hash algorithm. */
    public static final String DEFAULT_HASH_ALGORITHM = "SHA-256";

    private static final List<String> APPROVED_HASH_ALGORITHMS = Collections.unmodifiableList(Arrays.asList(
                    "SHA-256",
                    "SHA-384",
                    "SHA-512",
                    "SHA3-256",
                    "SHA3-384",
                    "SHA3-512"));

    private static final List<String> APPROVED_CLASSICAL_SIGNATURE_ALGORITHMS = Collections.unmodifiableList(Arrays.asList(
                    "SHA256withRSA",
                    "SHA384withRSA",
                    "SHA512withRSA",
                    "SHA256withECDSA",
                    "SHA384withECDSA",
                    "SHA512withECDSA"));

    private static final List<String> APPROVED_PQC_SIGNATURE_ALGORITHMS = Collections.unmodifiableList(Arrays.asList(
                    "ML-DSA-44",
                    "ML-DSA-65",
                    "ML-DSA-87"));

    private static final List<String> DEPRECATED_ALGORITHMS = Collections.unmodifiableList(Arrays.asList(
                    "SHA-1",
                    "SHA1",
                    "MD5",
                    "MD5withRSA",
                    "SHA1withRSA",
                    "SHA1withECDSA",
                    "DES",
                    "3DES",
                    "DESede"));

    private final boolean pqcEnabled;
    private final String validationMode;
    private final String hashAlgorithm;
    private final String classicalSignatureAlgorithm;
    private final String pqcSignatureAlgorithm;
    private final boolean strictValidation;

    /**
     * Creates an algorithm validator using supplied configuration.
     *
     * @param configuration algorithm configuration
     */
    public AlgorithmConfigurationValidator(AlgorithmConfiguration configuration) {
        this(configuration, true);
    }

    /**
     * Creates an algorithm validator using supplied configuration.
     *
     * @param configuration algorithm configuration
     * @param strictValidation whether stronger recommendations should escalate to errors
     */
    public AlgorithmConfigurationValidator(AlgorithmConfiguration configuration, boolean strictValidation) {
        if (configuration == null) {
            throw new IllegalArgumentException("AlgorithmConfiguration cannot be null");
        }

        this.pqcEnabled = configuration.isPqcEnabled();
        this.validationMode = normalize(configuration.getValidationMode(), "hybrid");
        this.hashAlgorithm = normalize(configuration.getHashAlgorithm(), DEFAULT_HASH_ALGORITHM);
        this.classicalSignatureAlgorithm = normalize(configuration.getClassicalSignatureAlgorithm(), DEFAULT_CLASSICAL_SIGNATURE_ALGORITHM);
        this.pqcSignatureAlgorithm = normalize(configuration.getPqcSignatureAlgorithm(), DEFAULT_PQC_SIGNATURE_ALGORITHM);
        this.strictValidation = strictValidation;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "AlgorithmConfigurationValidator initialized: pqcEnabled={0}, validationMode={1}, hash={2}, classicalSig={3}, pqcSig={4}",
                            Boolean.valueOf(this.pqcEnabled), this.validationMode, this.hashAlgorithm,
                            this.classicalSignatureAlgorithm, this.pqcSignatureAlgorithm);
        }
    }

    /**
     * Validates the full algorithm configuration.
     *
     * @throws SecurityException if validation fails
     */
    public void validate() throws SecurityException {
        ValidationResult result = validateConfiguration();
        if (!result.isValid()) {
            throw new SecurityException(result.getPrimaryFailureMessage());
        }
    }

    /**
     * Validates the full algorithm configuration and returns a detailed result.
     *
     * @return validation result
     */
    public ValidationResult validateConfiguration() {
        ValidationResult result = new ValidationResult();

        validateHashAlgorithm(result);
        validateSignatureAlgorithms(result);
        validateHybridCombination(result);
        validateFipsRestrictions(result);
        validateDeprecationWarnings(result);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Algorithm configuration validation complete: valid={0}, errors={1}, warnings={2}",
                            Boolean.valueOf(result.isValid()),
                            Integer.valueOf(result.getErrorCount()),
                            Integer.valueOf(result.getWarningCount()));
        }

        return result;
    }

    /**
     * Validates only hash algorithm configuration.
     *
     * @throws SecurityException if invalid
     */
    public void validateHashConfiguration() throws SecurityException {
        ValidationResult result = new ValidationResult();
        validateHashAlgorithm(result);
        validateFipsRestrictions(result);

        if (!result.isValid()) {
            throw new SecurityException(result.getPrimaryFailureMessage());
        }
    }

    /**
     * Validates only signature algorithm configuration.
     *
     * @throws SecurityException if invalid
     */
    public void validateSignatureConfiguration() throws SecurityException {
        ValidationResult result = new ValidationResult();
        validateSignatureAlgorithms(result);
        validateHybridCombination(result);
        validateFipsRestrictions(result);

        if (!result.isValid()) {
            throw new SecurityException(result.getPrimaryFailureMessage());
        }
    }

    /**
     * Returns true if an algorithm is deprecated.
     *
     * @param algorithm algorithm name
     * @return true if deprecated
     */
    @Trivial
    public boolean isDeprecated(String algorithm) {
        String normalized = normalize(algorithm, null);
        return normalized != null && DEPRECATED_ALGORITHMS.contains(normalized);
    }

    /**
     * Returns recommendations for configured algorithms.
     *
     * @return recommendations list
     */
    public List<String> getRecommendations() {
        List<String> recommendations = new ArrayList<String>();

        if ("SHA256withRSA".equals(classicalSignatureAlgorithm)) {
            recommendations.add("Consider SHA384withRSA or SHA512withRSA for longer-term classical strength requirements.");
        }

        if ("ML-DSA-44".equals(pqcSignatureAlgorithm)) {
            recommendations.add("Consider ML-DSA-65 as the balanced default for PQC deployments.");
        }

        if ("SHA-256".equals(hashAlgorithm) && FIPSModeDetector.isFIPSEnabled()) {
            recommendations.add("SHA-256 is valid in FIPS mode; consider SHA-384 for stronger security margin.");
        }

        if (!pqcEnabled && "hybrid".equals(validationMode)) {
            recommendations.add("Enable PQC to realize the intended hybrid validation policy.");
        }

        return Collections.unmodifiableList(recommendations);
    }

    private void validateHashAlgorithm(ValidationResult result) {
        if (hashAlgorithm == null) {
            result.addError("Hash algorithm must not be null.");
            return;
        }

        if (isDeprecated(hashAlgorithm)) {
            result.addError("Deprecated hash algorithm '" + hashAlgorithm + "' must not be configured.");
            return;
        }

        if (!APPROVED_HASH_ALGORITHMS.contains(hashAlgorithm)) {
            result.addError("Unsupported hash algorithm '" + hashAlgorithm + "'. Supported values are: " + APPROVED_HASH_ALGORITHMS + ".");
            return;
        }

        if (FIPSModeDetector.isFIPSEnabled()) {
            try {
                FIPSAlgorithmValidator.validateAlgorithmForFIPS(hashAlgorithm);
            } catch (SecurityException e) {
                result.addError("Hash algorithm is not permitted in FIPS mode: " + e.getMessage());
            }
        }
    }

    private void validateSignatureAlgorithms(ValidationResult result) {
        validateClassicalSignatureAlgorithm(result);

        if (pqcEnabled) {
            validatePqcSignatureAlgorithm(result);
        } else if (pqcSignatureAlgorithm != null && !DEFAULT_PQC_SIGNATURE_ALGORITHM.equals(pqcSignatureAlgorithm)) {
            result.addWarning("PQC signature algorithm is configured while PQC is disabled; the value will not be used.");
        }
    }

    private void validateClassicalSignatureAlgorithm(ValidationResult result) {
        if (classicalSignatureAlgorithm == null) {
            result.addError("Classical signature algorithm must not be null.");
            return;
        }

        if (isDeprecated(classicalSignatureAlgorithm)) {
            result.addError("Deprecated signature algorithm '" + classicalSignatureAlgorithm + "' must not be configured.");
            return;
        }

        if (!APPROVED_CLASSICAL_SIGNATURE_ALGORITHMS.contains(classicalSignatureAlgorithm)) {
            result.addError("Unsupported classical signature algorithm '" + classicalSignatureAlgorithm
                            + "'. Supported values are: " + APPROVED_CLASSICAL_SIGNATURE_ALGORITHMS + ".");
            return;
        }

        if (FIPSModeDetector.isFIPSEnabled()) {
            try {
                FIPSAlgorithmValidator.validateAlgorithmForFIPS(classicalSignatureAlgorithm);
            } catch (SecurityException e) {
                result.addError("Classical signature algorithm is not permitted in FIPS mode: " + e.getMessage());
            }
        }
    }

    private void validatePqcSignatureAlgorithm(ValidationResult result) {
        if (pqcSignatureAlgorithm == null) {
            result.addError("PQC signature algorithm must not be null when PQC is enabled.");
            return;
        }

        if (!APPROVED_PQC_SIGNATURE_ALGORITHMS.contains(pqcSignatureAlgorithm)) {
            result.addError("Unsupported PQC signature algorithm '" + pqcSignatureAlgorithm
                            + "'. Supported values are: " + APPROVED_PQC_SIGNATURE_ALGORITHMS + ".");
            return;
        }

        if (FIPSModeDetector.isFIPSEnabled()) {
            try {
                FIPSAlgorithmValidator.validateAlgorithmForFIPS(pqcSignatureAlgorithm);
            } catch (SecurityException e) {
                result.addError("PQC signature algorithm is not permitted in FIPS mode: " + e.getMessage());
            }
        }
    }

    private void validateHybridCombination(ValidationResult result) {
        if (!pqcEnabled) {
            if ("pqc-required".equals(validationMode)) {
                result.addError("Validation mode 'pqc-required' cannot be used when PQC is disabled.");
            }
            return;
        }

        if ("classical-only".equals(validationMode)) {
            result.addWarning("PQC is enabled but validation mode is 'classical-only'; hybrid tokens may be produced while hybrid validation remains disabled.");
        }

        if ("pqc-required".equals(validationMode) && pqcSignatureAlgorithm == null) {
            result.addError("PQC validation mode requires a PQC signature algorithm.");
        }

        if (classicalSignatureAlgorithm != null && classicalSignatureAlgorithm.contains("ECDSA")
                        && pqcSignatureAlgorithm != null && "ML-DSA-87".equals(pqcSignatureAlgorithm)
                        && strictValidation) {
            result.addWarning("ECDSA with ML-DSA-87 is valid but may impose higher signing and token size overhead.");
        }

        if (classicalSignatureAlgorithm != null && pqcSignatureAlgorithm != null) {
            String classicalHash = extractHashFamily(classicalSignatureAlgorithm);
            if (classicalHash != null && !hashAlgorithm.startsWith(classicalHash)) {
                result.addWarning("Configured hash algorithm '" + hashAlgorithm
                                + "' does not align with the hash family embedded in classical signature algorithm '" + classicalSignatureAlgorithm + "'.");
            }
        }
    }

    private void validateFipsRestrictions(ValidationResult result) {
        if (!FIPSModeDetector.isFIPSEnabled()) {
            return;
        }

        if (!FIPSModeDetector.validateFIPSConfiguration()) {
            result.addError("FIPS mode is enabled but the underlying FIPS provider/configuration is not valid.");
            return;
        }

        if ("SHA256withRSA".equals(classicalSignatureAlgorithm) && "SHA-512".equals(hashAlgorithm) && strictValidation) {
            result.addWarning("FIPS mode is enabled and the configured standalone hash algorithm is stronger than the classical signature hash; ensure this mismatch is intentional.");
        }

        if ("ML-DSA-44".equals(pqcSignatureAlgorithm)) {
            result.addWarning("ML-DSA-44 is permitted, but review whether its NIST level satisfies FIPS deployment policy.");
        }
    }

    private void validateDeprecationWarnings(ValidationResult result) {
        if (isDeprecated(hashAlgorithm)) {
            result.addError("Configured hash algorithm '" + hashAlgorithm + "' is deprecated.");
        }

        if (isDeprecated(classicalSignatureAlgorithm)) {
            result.addError("Configured classical signature algorithm '" + classicalSignatureAlgorithm + "' is deprecated.");
        }

        if (pqcSignatureAlgorithm != null && isDeprecated(pqcSignatureAlgorithm)) {
            result.addError("Configured PQC signature algorithm '" + pqcSignatureAlgorithm + "' is deprecated.");
        }

        if ("SHA-256".equals(hashAlgorithm) && "SHA512withRSA".equals(classicalSignatureAlgorithm)) {
            result.addWarning("Classical signature algorithm uses SHA-512 while the configured general hash is SHA-256; ensure this split is intentional.");
        }
    }

    @Trivial
    private String extractHashFamily(String signatureAlgorithm) {
        if (signatureAlgorithm == null) {
            return null;
        } else if (signatureAlgorithm.startsWith("SHA256")) {
            return "SHA-256";
        } else if (signatureAlgorithm.startsWith("SHA384")) {
            return "SHA-384";
        } else if (signatureAlgorithm.startsWith("SHA512")) {
            return "SHA-512";
        }
        return null;
    }

    @Trivial
    private String normalize(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? defaultValue : trimmed;
    }

    /**
     * Algorithm configuration abstraction used by the validator.
     */
    public interface AlgorithmConfiguration {

        /**
         * @return true if PQC is enabled
         */
        boolean isPqcEnabled();

        /**
         * @return validation mode
         */
        String getValidationMode();

        /**
         * @return configured hash algorithm
         */
        String getHashAlgorithm();

        /**
         * @return configured classical signature algorithm
         */
        String getClassicalSignatureAlgorithm();

        /**
         * @return configured PQC signature algorithm
         */
        String getPqcSignatureAlgorithm();
    }

    /**
     * Result of algorithm configuration validation.
     */
    public static class ValidationResult {

        private final List<String> errors = new ArrayList<String>();
        private final List<String> warnings = new ArrayList<String>();

        void addError(String message) {
            errors.add(message);
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "Algorithm configuration validation error: {0}", message);
            }
        }

        void addWarning(String message) {
            warnings.add(message);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Algorithm configuration validation warning: {0}", message);
            }
        }

        /**
         * @return true if valid
         */
        @Trivial
        public boolean isValid() {
            return errors.isEmpty();
        }

        /**
         * @return immutable errors
         */
        @Trivial
        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        /**
         * @return immutable warnings
         */
        @Trivial
        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        /**
         * @return first failure message or null
         */
        @Trivial
        public String getPrimaryFailureMessage() {
            return errors.isEmpty() ? null : errors.get(0);
        }

        /**
         * @return error count
         */
        @Trivial
        public int getErrorCount() {
            return errors.size();
        }

        /**
         * @return warning count
         */
        @Trivial
        public int getWarningCount() {
            return warnings.size();
        }

        @Override
        public String toString() {
            return "ValidationResult[valid=" + isValid() + ", errors=" + errors + ", warnings=" + warnings + "]";
        }
    }
}

// Made with Bob
