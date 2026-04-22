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
 * Validates PQC-LTPA security-related server configuration.
 *
 * <p>This validator centralizes security validation for server.xml and runtime
 * LTPA settings that influence PQC, hybrid token processing, and time-based
 * token protections. It is designed to be used during activation, modified
 * processing, and explicit compliance checks.
 *
 * <p><b>Validation Coverage:</b>
 * <ul>
 *   <li>PQC enablement and prerequisite validation</li>
 *   <li>Hybrid/classical validation mode consistency</li>
 *   <li>Token version policy validation</li>
 *   <li>Token expiration and replay-protection bounds</li>
 *   <li>Clock skew tolerance validation</li>
 *   <li>FIPS-sensitive configuration restrictions</li>
 * </ul>
 *
 * <p><b>Validation Behavior:</b>
 * <ul>
 *   <li>Errors are reported as {@link SecurityException} when using enforcing APIs</li>
 *   <li>Warnings are accumulated in validation reports for non-fatal conditions</li>
 *   <li>FIPS mode uses {@link FIPSModeDetector} and {@link FIPSAlgorithmValidator}</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.
 */
public class SecurityConfigurationValidator {

    private static final TraceComponent tc = Tr.register(SecurityConfigurationValidator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    /** Default PQC algorithm. */
    public static final String DEFAULT_PQC_ALGORITHM = "ML-DSA-65";

    /** Default validation mode. */
    public static final String DEFAULT_VALIDATION_MODE = "hybrid";

    /** Default primary token version for classical mode. */
    public static final int DEFAULT_CLASSICAL_TOKEN_VERSION = TokenVersionValidator.VERSION_CLASSICAL;

    /** Default token version for hybrid mode. */
    public static final int DEFAULT_HYBRID_TOKEN_VERSION = TokenVersionValidator.VERSION_HYBRID;

    /** Maximum accepted clock skew tolerance: 10 minutes. */
    public static final long MAX_CLOCK_SKEW_TOLERANCE_MS = 10 * 60 * 1000L;

    /** Minimum accepted clock skew tolerance: zero. */
    public static final long MIN_CLOCK_SKEW_TOLERANCE_MS = 0L;

    /** Minimum supported expiration difference allowed. */
    public static final long MIN_EXPIRATION_DIFFERENCE_ALLOWED_MS = 0L;

    /** Maximum supported expiration difference allowed: 24 hours. */
    public static final long MAX_EXPIRATION_DIFFERENCE_ALLOWED_MS = 24 * 60 * 60 * 1000L;

    private static final List<String> VALID_PQC_ALGORITHMS = Collections.unmodifiableList(Arrays.asList(
                    "ML-DSA-44",
                    "ML-DSA-65",
                    "ML-DSA-87"));

    private static final List<String> VALID_VALIDATION_MODES = Collections.unmodifiableList(Arrays.asList(
                    "hybrid",
                    "pqc-required",
                    "classical-only"));

    private final boolean pqcEnabled;
    private final String pqcAlgorithm;
    private final String pqcValidationMode;
    private final String primaryKeysFileName;
    private final String pqcKeysFileName;
    private final long tokenExpirationMinutes;
    private final long expirationDifferenceAllowedMs;
    private final long clockSkewToleranceMs;
    private final int configuredTokenVersion;
    private final boolean strictValidation;
    private final boolean fallbackToClassicalAllowed;

    /**
     * Creates a validator with supplied configuration.
     *
     * @param configuration the configuration to validate
     */
    public SecurityConfigurationValidator(SecurityConfiguration configuration) {
        this(configuration, true, true);
    }

    /**
     * Creates a validator with supplied configuration and behavior flags.
     *
     * @param configuration the configuration to validate
     * @param strictValidation whether warnings should be treated more aggressively
     * @param fallbackToClassicalAllowed whether PQC-unavailable conditions may degrade to classical mode
     */
    public SecurityConfigurationValidator(SecurityConfiguration configuration, boolean strictValidation, boolean fallbackToClassicalAllowed) {
        if (configuration == null) {
            throw new IllegalArgumentException("SecurityConfiguration cannot be null");
        }

        this.pqcEnabled = configuration.isPqcEnabled();
        this.pqcAlgorithm = normalizeString(configuration.getPqcAlgorithm(), DEFAULT_PQC_ALGORITHM);
        this.pqcValidationMode = normalizeString(configuration.getPqcValidationMode(), DEFAULT_VALIDATION_MODE);
        this.primaryKeysFileName = trimToNull(configuration.getPrimaryKeysFileName());
        this.pqcKeysFileName = trimToNull(configuration.getPqcKeysFileName());
        this.tokenExpirationMinutes = configuration.getTokenExpirationMinutes();
        this.expirationDifferenceAllowedMs = configuration.getExpirationDifferenceAllowedMs();
        this.clockSkewToleranceMs = configuration.getClockSkewToleranceMs();
        this.configuredTokenVersion = configuration.getTokenVersion();
        this.strictValidation = strictValidation;
        this.fallbackToClassicalAllowed = fallbackToClassicalAllowed;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "SecurityConfigurationValidator initialized: pqcEnabled={0}, algorithm={1}, mode={2}, tokenVersion={3}",
                            Boolean.valueOf(this.pqcEnabled), this.pqcAlgorithm, this.pqcValidationMode, Integer.valueOf(this.configuredTokenVersion));
        }
    }

    /**
     * Validates the full configuration and throws {@link SecurityException}
     * on any fatal violation.
     *
     * @throws SecurityException if the configuration is invalid
     */
    public void validate() throws SecurityException {
        ValidationReport report = validateConfiguration();
        if (!report.isValid()) {
            throw new SecurityException(report.getPrimaryFailureMessage());
        }
    }

    /**
     * Validates the full configuration and returns a detailed report.
     *
     * @return the validation report
     */
    public ValidationReport validateConfiguration() {
        ValidationReport report = new ValidationReport();

        validatePqcEnablement(report);
        validateValidationMode(report);
        validateTokenVersion(report);
        validateExpirationSettings(report);
        validateClockSkew(report);
        validateCrossAttributeConstraints(report);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Security configuration validation complete: valid={0}, errors={1}, warnings={2}",
                            Boolean.valueOf(report.isValid()),
                            Integer.valueOf(report.getErrorCount()),
                            Integer.valueOf(report.getWarningCount()));
        }

        return report;
    }

    /**
     * Validates only PQC enablement and prerequisite rules.
     *
     * @throws SecurityException if PQC-related configuration is invalid
     */
    public void validatePqcConfiguration() throws SecurityException {
        ValidationReport report = new ValidationReport();
        validatePqcEnablement(report);
        validateValidationMode(report);
        validateCrossAttributeConstraints(report);

        if (!report.isValid()) {
            throw new SecurityException(report.getPrimaryFailureMessage());
        }
    }

    /**
     * Validates only time-based replay protection settings.
     *
     * @throws SecurityException if expiration or clock skew settings are invalid
     */
    public void validateTimeBasedConfiguration() throws SecurityException {
        ValidationReport report = new ValidationReport();
        validateExpirationSettings(report);
        validateClockSkew(report);
        validateCrossAttributeConstraints(report);

        if (!report.isValid()) {
            throw new SecurityException(report.getPrimaryFailureMessage());
        }
    }

    /**
     * Performs PQC enablement validation.
     */
    private void validatePqcEnablement(ValidationReport report) {
        if (!pqcEnabled) {
            if ("pqc-required".equals(pqcValidationMode)) {
                report.addError("PQC validation mode 'pqc-required' cannot be used when pqcEnabled is false.");
            }

            if (configuredTokenVersion == TokenVersionValidator.VERSION_HYBRID) {
                report.addError("Hybrid token version requires pqcEnabled=true.");
            }

            if (pqcKeysFileName != null) {
                report.addWarning("pqcKeysFileName is configured but PQC is disabled; the setting will be ignored.");
            }
            return;
        }

        if (!VALID_PQC_ALGORITHMS.contains(pqcAlgorithm)) {
            report.addError("Unsupported pqcAlgorithm '" + pqcAlgorithm + "'. Supported values are: " + VALID_PQC_ALGORITHMS + ".");
        } else {
            try {
                FIPSAlgorithmValidator.validateAlgorithmForFIPS(pqcAlgorithm);
            } catch (SecurityException e) {
                report.addError("Configured PQC algorithm is not permitted in the current FIPS mode: " + e.getMessage());
            }
        }

        if (pqcKeysFileName == null) {
            if (primaryKeysFileName == null) {
                report.addError("PQC is enabled but no pqcKeysFileName is configured and no primary keys file is available for deriving a default.");
            } else {
                report.addWarning("PQC is enabled without explicit pqcKeysFileName; runtime should derive a default companion PQC keys file.");
            }
        }

        if (!isPqcProviderAvailable()) {
            if (fallbackToClassicalAllowed) {
                report.addWarning("PQC is enabled but no PQC provider is currently available; runtime may fall back to classical mode.");
            } else {
                report.addError("PQC is enabled but no PQC provider is currently available.");
            }
        }

        if (FIPSModeDetector.isFIPSEnabled() && !FIPSModeDetector.validateFIPSConfiguration()) {
            report.addError("FIPS mode is enabled but the active FIPS configuration is not valid for PQC-LTPA.");
        }
    }

    /**
     * Validates hybrid/classical token validation mode.
     */
    private void validateValidationMode(ValidationReport report) {
        if (!VALID_VALIDATION_MODES.contains(pqcValidationMode)) {
            report.addError("Unsupported pqcValidationMode '" + pqcValidationMode + "'. Supported values are: " + VALID_VALIDATION_MODES + ".");
            return;
        }

        if ("classical-only".equals(pqcValidationMode) && pqcEnabled) {
            report.addWarning("PQC is enabled while pqcValidationMode is 'classical-only'; the server will create PQC-capable state but reject hybrid validation behavior.");
        }

        if ("pqc-required".equals(pqcValidationMode) && !pqcEnabled) {
            report.addError("pqcValidationMode 'pqc-required' requires pqcEnabled=true.");
        }

        if ("hybrid".equals(pqcValidationMode) && !pqcEnabled) {
            report.addWarning("pqcValidationMode is 'hybrid' while PQC is disabled; the runtime will effectively operate in classical mode until PQC is enabled.");
        }

        if (FIPSModeDetector.isFIPSEnabled() && "classical-only".equals(pqcValidationMode) && pqcEnabled) {
            report.addWarning("FIPS mode is enabled and PQC is configured, but pqcValidationMode is 'classical-only'; review whether this meets migration and compliance intent.");
        }
    }

    /**
     * Validates configured token version settings.
     */
    private void validateTokenVersion(ValidationReport report) {
        if (configuredTokenVersion != TokenVersionValidator.VERSION_CLASSICAL
                        && configuredTokenVersion != TokenVersionValidator.VERSION_HYBRID) {
            report.addError("Unsupported token version '" + configuredTokenVersion + "'. Supported values are "
                            + TokenVersionValidator.VERSION_CLASSICAL + " and " + TokenVersionValidator.VERSION_HYBRID + ".");
            return;
        }

        if (pqcEnabled && configuredTokenVersion == TokenVersionValidator.VERSION_CLASSICAL) {
            report.addWarning("PQC is enabled but configured token version is classical; hybrid issuance may be inconsistent with server intent.");
        }

        if (!pqcEnabled && configuredTokenVersion == TokenVersionValidator.VERSION_HYBRID) {
            report.addError("Hybrid token version cannot be configured when PQC is disabled.");
        }

        if ("pqc-required".equals(pqcValidationMode) && configuredTokenVersion != TokenVersionValidator.VERSION_HYBRID) {
            report.addError("pqcValidationMode 'pqc-required' requires hybrid token version " + TokenVersionValidator.VERSION_HYBRID + ".");
        }

        if ("classical-only".equals(pqcValidationMode) && configuredTokenVersion == TokenVersionValidator.VERSION_HYBRID) {
            report.addWarning("Hybrid token version is configured while pqcValidationMode is 'classical-only'; received tokens may be rejected unexpectedly.");
        }
    }

    /**
     * Validates token expiration configuration.
     */
    private void validateExpirationSettings(ValidationReport report) {
        if (tokenExpirationMinutes <= 0) {
            report.addError("Token expiration must be greater than zero minutes.");
            return;
        }

        long tokenExpirationMs = tokenExpirationMinutes * 60 * 1000L;

        if (tokenExpirationMs < TokenExpirationValidator.MIN_LIFETIME_MS) {
            report.addError("Token expiration is shorter than the minimum supported token lifetime of "
                            + TokenExpirationValidator.MIN_LIFETIME_MS + " ms.");
        }

        if (tokenExpirationMs > TokenExpirationValidator.MAX_LIFETIME_MS) {
            if (strictValidation) {
                report.addError("Token expiration exceeds the maximum supported token lifetime of "
                                + TokenExpirationValidator.MAX_LIFETIME_MS + " ms.");
            } else {
                report.addWarning("Token expiration exceeds the recommended maximum token lifetime of "
                                + TokenExpirationValidator.MAX_LIFETIME_MS + " ms.");
            }
        }

        if (expirationDifferenceAllowedMs < MIN_EXPIRATION_DIFFERENCE_ALLOWED_MS
                        || expirationDifferenceAllowedMs > MAX_EXPIRATION_DIFFERENCE_ALLOWED_MS) {
            report.addError("expirationDifferenceAllowed must be between "
                            + MIN_EXPIRATION_DIFFERENCE_ALLOWED_MS + " and "
                            + MAX_EXPIRATION_DIFFERENCE_ALLOWED_MS + " ms.");
        }

        if (expirationDifferenceAllowedMs > tokenExpirationMs) {
            report.addWarning("expirationDifferenceAllowed is greater than token expiration; replay tolerance may be too permissive.");
        }

        if (FIPSModeDetector.isFIPSEnabled() && tokenExpirationMs > TokenExpirationValidator.DEFAULT_MAX_LIFETIME_MS) {
            report.addWarning("FIPS mode is enabled and token expiration exceeds the default recommended lifetime of "
                            + TokenExpirationValidator.DEFAULT_MAX_LIFETIME_MS + " ms.");
        }
    }

    /**
     * Validates clock skew tolerance configuration.
     */
    private void validateClockSkew(ValidationReport report) {
        if (clockSkewToleranceMs < MIN_CLOCK_SKEW_TOLERANCE_MS) {
            report.addError("Clock skew tolerance cannot be negative.");
            return;
        }

        if (clockSkewToleranceMs > MAX_CLOCK_SKEW_TOLERANCE_MS) {
            if (strictValidation) {
                report.addError("Clock skew tolerance exceeds the maximum supported value of "
                                + MAX_CLOCK_SKEW_TOLERANCE_MS + " ms.");
            } else {
                report.addWarning("Clock skew tolerance exceeds the recommended maximum of "
                                + MAX_CLOCK_SKEW_TOLERANCE_MS + " ms.");
            }
        }

        if (clockSkewToleranceMs > TokenExpirationValidator.DEFAULT_CLOCK_SKEW_MS) {
            report.addWarning("Clock skew tolerance exceeds the default recommended value of "
                            + TokenExpirationValidator.DEFAULT_CLOCK_SKEW_MS + " ms.");
        }

        long tokenExpirationMs = tokenExpirationMinutes <= 0 ? 0 : tokenExpirationMinutes * 60 * 1000L;
        if (tokenExpirationMs > 0 && clockSkewToleranceMs >= tokenExpirationMs) {
            report.addError("Clock skew tolerance must be smaller than the token expiration interval.");
        }
    }

    /**
     * Validates cross-attribute security constraints.
     */
    private void validateCrossAttributeConstraints(ValidationReport report) {
        if (pqcEnabled && primaryKeysFileName != null && pqcKeysFileName != null
                        && primaryKeysFileName.equals(pqcKeysFileName)) {
            report.addError("Primary LTPA keys file and PQC keys file must not refer to the same path.");
        }

        if (pqcEnabled && "ML-DSA-44".equals(pqcAlgorithm) && FIPSModeDetector.isFIPSEnabled()) {
            report.addWarning("ML-DSA-44 is configured in FIPS mode. Review whether its security level satisfies deployment policy.");
        }

        if (strictValidation && !pqcEnabled && configuredTokenVersion == TokenVersionValidator.VERSION_CLASSICAL
                        && "hybrid".equals(pqcValidationMode)) {
            report.addWarning("Strict validation is enabled but configuration is classical-only in practice while retaining hybrid validation mode.");
        }
    }

    /**
     * Checks PQC provider availability without hard dependency on provider internals.
     *
     * @return true if a PQC provider appears to be available
     */
    private boolean isPqcProviderAvailable() {
        try {
            Class<?> factoryClass = Class.forName("com.ibm.ws.security.token.ltpa.internal.pqc.PQCProviderFactory");
            Object provider = factoryClass.getMethod("getProvider").invoke(null);
            return provider != null;
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "PQC provider availability check failed", e);
            }
            return false;
        }
    }

    @Trivial
    private static String normalizeString(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    @Trivial
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Immutable configuration abstraction used by this validator.
     */
    public interface SecurityConfiguration {

        /**
         * @return true if PQC is enabled
         */
        boolean isPqcEnabled();

        /**
         * @return configured PQC algorithm, or null
         */
        String getPqcAlgorithm();

        /**
         * @return configured validation mode, or null
         */
        String getPqcValidationMode();

        /**
         * @return primary LTPA keys file path, or null
         */
        String getPrimaryKeysFileName();

        /**
         * @return PQC keys file path, or null
         */
        String getPqcKeysFileName();

        /**
         * @return token expiration in minutes
         */
        long getTokenExpirationMinutes();

        /**
         * @return expirationDifferenceAllowed in milliseconds
         */
        long getExpirationDifferenceAllowedMs();

        /**
         * @return clock skew tolerance in milliseconds
         */
        long getClockSkewToleranceMs();

        /**
         * @return configured token version
         */
        int getTokenVersion();
    }

    /**
     * Immutable validation report.
     */
    public static class ValidationReport {

        private final List<String> errors = new ArrayList<String>();
        private final List<String> warnings = new ArrayList<String>();

        void addError(String message) {
            errors.add(message);
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "Security configuration validation error: {0}", message);
            }
        }

        void addWarning(String message) {
            warnings.add(message);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Security configuration validation warning: {0}", message);
            }
        }

        /**
         * @return true if no validation errors occurred
         */
        @Trivial
        public boolean isValid() {
            return errors.isEmpty();
        }

        /**
         * @return immutable list of validation errors
         */
        @Trivial
        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        /**
         * @return immutable list of validation warnings
         */
        @Trivial
        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        /**
         * @return number of validation errors
         */
        @Trivial
        public int getErrorCount() {
            return errors.size();
        }

        /**
         * @return number of validation warnings
         */
        @Trivial
        public int getWarningCount() {
            return warnings.size();
        }

        /**
         * @return first validation error, or null if valid
         */
        @Trivial
        public String getPrimaryFailureMessage() {
            return errors.isEmpty() ? null : errors.get(0);
        }

        @Override
        public String toString() {
            return "ValidationReport[valid=" + isValid()
                            + ", errors=" + errors
                            + ", warnings=" + warnings + "]";
        }
    }
}

// Made with Bob
