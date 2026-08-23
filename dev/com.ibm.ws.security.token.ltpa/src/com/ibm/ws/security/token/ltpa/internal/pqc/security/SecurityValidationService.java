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

import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.token.ltpa.internal.TraceConstants;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCAlgorithm;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCException;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCProvider;
import com.ibm.ws.security.token.ltpa.internal.pqc.PQCProviderFactory;

/**
 * Central orchestration service for PQC-LTPA security validation.
 *
 * <p>This service integrates and coordinates the complete PQC-LTPA security
 * validation framework, providing a unified API across validation, detection,
 * FIPS compliance, configuration hardening, and metrics collection.
 *
 * <p><b>Integrated Components:</b>
 * <ul>
 *   <li>Token version validation and downgrade protection</li>
 *   <li>Hybrid signature chain verification</li>
 *   <li>Expiration and replay attack detection</li>
 *   <li>Tampering detection and constant-time validation support</li>
 *   <li>FIPS mode and algorithm compliance validation</li>
 *   <li>Configuration and key strength validation</li>
 *   <li>Centralized event logging and metric aggregation</li>
 * </ul>
 *
 * <p><b>Validation Modes:</b>
 * <ul>
 *   <li><b>STRICT</b> - Any validation error causes overall failure</li>
 *   <li><b>LENIENT</b> - Errors are downgraded where possible and surfaced as warnings</li>
 *   <li><b>AUDIT</b> - Validation is performed for visibility only; no enforcement</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> The service is designed as a thread-safe singleton.
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * SecurityValidationService service = SecurityValidationService.getInstance();
 *
 * SecurityValidationService.TokenValidationRequest request =
 *     new SecurityValidationService.TokenValidationRequest.Builder()
 *         .userId("alice")
 *         .tokenData(tokenBytes)
 *         .tokenVersion(21)
 *         .pqcEnabled(true)
 *         .pqcAlgorithm(PQCAlgorithm.ML_DSA_65)
 *         .creationTimeMs(created)
 *         .expirationTimeMs(expires)
 *         .rsaSignature(rsaSig)
 *         .pqcSignature(pqcSig)
 *         .rsaPublicKey(rsaPublic)
 *         .pqcPublicKey(pqcPublic)
 *         .build();
 *
 * SecurityValidationService.SecurityValidationReport report =
 *     service.validateTokenValidation(request, SecurityValidationService.ValidationMode.STRICT);
 *
 * if (!report.isSuccessful()) {
 *     throw new SecurityException(report.getPrimaryFailureMessage());
 * }
 * </pre>
 */
public class SecurityValidationService {

    private static final TraceComponent tc = Tr.register(SecurityValidationService.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static volatile SecurityValidationService instance;
    private static final Object LOCK = new Object();

    private final SecurityMetricsCollector metricsCollector;
    private final SecurityEventLogger eventLogger;
    private final DowngradeAttackDetector downgradeAttackDetector;
    private final ReplayAttackDetector replayAttackDetector;
    private final TamperingDetector tamperingDetector;
    private final KeyStrengthValidator keyStrengthValidator;

    private SecurityValidationService() {
        this.metricsCollector = SecurityMetricsCollector.getInstance();
        this.eventLogger = new SecurityEventLogger();
        this.downgradeAttackDetector = new DowngradeAttackDetector(metricsCollector);
        this.replayAttackDetector = new ReplayAttackDetector(metricsCollector);
        this.tamperingDetector = new TamperingDetector(metricsCollector);
        this.keyStrengthValidator = new KeyStrengthValidator();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "SecurityValidationService initialized");
        }
    }

    /**
     * Returns the singleton service instance.
     *
     * @return singleton validation service
     */
    public static SecurityValidationService getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new SecurityValidationService();
                }
            }
        }
        return instance;
    }

    /**
     * Performs pre-generation validation.
     *
     * @param request generation request
     * @param mode validation mode
     * @return validation report
     */
    public SecurityValidationReport validateTokenGeneration(TokenGenerationRequest request, ValidationMode mode) {
        long start = System.nanoTime();
        SecurityValidationReport report = createReport("token-generation", mode);

        if (request == null) {
            report.addError("generation-request", "Token generation request must not be null.");
            return finalizeReport(report, start, "generation", request == null ? null : request.getUserId(), null);
        }

        try {
            validateGenerationInputs(request, report);
            validateGenerationConfiguration(request, report);
            validateGenerationAlgorithms(request, report);
            validateGenerationKeys(request, report);
            validateGenerationFips(request, report);
        } catch (RuntimeException e) {
            Tr.error(tc, "CWWKS4260E: Unexpected error during token generation validation: " + e.getMessage());
            report.addError("generation-runtime", "Unexpected token generation validation error: " + e.getMessage());
        } finally {
            cleanGenerationSensitiveData(request);
        }

        return finalizeReport(report, start, "generation", request.getUserId(), request.getTokenType());
    }

    /**
     * Performs pre-validation checks for a token.
     *
     * @param request token validation request
     * @param mode validation mode
     * @return validation report
     */
    public SecurityValidationReport validateTokenValidation(TokenValidationRequest request, ValidationMode mode) {
        long start = System.nanoTime();
        SecurityValidationReport report = createReport("token-validation", mode);

        if (request == null) {
            report.addError("validation-request", "Token validation request must not be null.");
            return finalizeReport(report, start, "unknown", null, null);
        }

        try {
            validateTokenStructure(request, report);
            validateTokenVersionAndMetadata(request, report);
            validateTokenTiming(request, report);
            detectRuntimeAttacks(request, report);
            validateTokenSignatures(request, report);
            validateTimingResistance(request, report);
            validateValidationKeys(request, report);
            validateRuntimeFips(request, report);
        } catch (RuntimeException e) {
            Tr.error(tc, "CWWKS4261E: Unexpected error during token validation orchestration: " + e.getMessage());
            report.addError("validation-runtime", "Unexpected token validation orchestration error: " + e.getMessage());
        } finally {
            cleanValidationSensitiveData(request);
        }

        return finalizeReport(report, start, request.getTokenType(), request.getUserId(), request.getTokenType());
    }

    /**
     * Validates a server/runtime configuration.
     *
     * @param request configuration request
     * @param mode validation mode
     * @return validation report
     */
    public SecurityValidationReport validateConfiguration(ConfigurationValidationRequest request, ValidationMode mode) {
        long start = System.nanoTime();
        SecurityValidationReport report = createReport("configuration-validation", mode);

        if (request == null) {
            report.addError("configuration-request", "Configuration validation request must not be null.");
            return finalizeReport(report, start, "configuration", null, null);
        }

        try {
            SecurityConfigurationValidator configurationValidator = new SecurityConfigurationValidator(
                            request.getSecurityConfiguration());

            SecurityConfigurationValidator.ValidationReport configurationReport = configurationValidator.validateConfiguration();
            mergeValidationReport(report, "configuration", configurationReport.getErrors(), configurationReport.getWarnings());

            AlgorithmConfigurationValidator algorithmValidator = new AlgorithmConfigurationValidator(
                            request.getAlgorithmConfiguration());

            AlgorithmConfigurationValidator.ValidationResult algorithmResult = algorithmValidator.validateConfiguration();
            mergeValidationResult(report, "algorithm-configuration", algorithmResult.getErrors(), algorithmResult.getWarnings());

            if (!algorithmValidator.getRecommendations().isEmpty()) {
                for (String recommendation : algorithmValidator.getRecommendations()) {
                    report.addRecommendation(recommendation);
                }
            }

            validateConfigurationFips(report, mode);
        } catch (RuntimeException e) {
            Tr.error(tc, "CWWKS4262E: Unexpected configuration validation error: " + e.getMessage());
            report.addError("configuration-runtime", "Unexpected configuration validation error: " + e.getMessage());
        }

        return finalizeReport(report, start, "configuration", request.getUserId(), null);
    }

    /**
     * Validates key strength and key usage suitability.
     *
     * @param request key validation request
     * @param mode validation mode
     * @return validation report
     */
    public SecurityValidationReport validateKeys(KeyValidationRequest request, ValidationMode mode) {
        long start = System.nanoTime();
        SecurityValidationReport report = createReport("key-validation", mode);

        if (request == null) {
            report.addError("key-request", "Key validation request must not be null.");
            return finalizeReport(report, start, "keys", null, null);
        }

        try {
            validateKeyInternal(request.getPrimaryKey(), request.getPrimaryAlgorithm(), "primary-key", report);
            validateKeyInternal(request.getSecondaryKey(), request.getSecondaryAlgorithm(), "secondary-key", report);
            validateKeyInternal(request.getSharedSecretKey(), request.getSharedSecretAlgorithm(), "shared-secret", report);

            if (request.getPrimaryAlgorithm() != null) {
                for (String recommendation : keyStrengthValidator.getRecommendations(request.getPrimaryAlgorithm())) {
                    report.addRecommendation(recommendation);
                }
            }
            if (request.getSecondaryAlgorithm() != null) {
                for (String recommendation : keyStrengthValidator.getRecommendations(request.getSecondaryAlgorithm())) {
                    report.addRecommendation(recommendation);
                }
            }

            if (FIPSModeDetector.isFIPSEnabled()) {
                if (request.getPrimaryKey() != null && !FIPSComplianceChecker.validateKey(request.getPrimaryKey())) {
                    report.addError("fips-primary-key", "Primary key is not FIPS-compliant.");
                }
                if (request.getSecondaryKey() != null && !FIPSComplianceChecker.validateKey(request.getSecondaryKey())) {
                    report.addError("fips-secondary-key", "Secondary key is not FIPS-compliant.");
                }
            }
        } catch (RuntimeException e) {
            Tr.error(tc, "CWWKS4263E: Unexpected key validation error: " + e.getMessage());
            report.addError("key-runtime", "Unexpected key validation error: " + e.getMessage());
        }

        return finalizeReport(report, start, "keys", request.getUserId(), null);
    }

    /**
     * Performs a comprehensive security audit.
     *
     * @param request audit request
     * @param mode validation mode
     * @return validation report
     */
    public SecurityValidationReport performSecurityAudit(SecurityAuditRequest request, ValidationMode mode) {
        long start = System.nanoTime();
        SecurityValidationReport report = createReport("security-audit", mode);

        if (request == null) {
            report.addError("audit-request", "Security audit request must not be null.");
            return finalizeReport(report, start, "audit", null, null);
        }

        try {
            if (request.getConfigurationRequest() != null) {
                report.merge(validateConfiguration(request.getConfigurationRequest(), ValidationMode.AUDIT));
            }

            if (request.getKeyValidationRequest() != null) {
                report.merge(validateKeys(request.getKeyValidationRequest(), ValidationMode.AUDIT));
            }

            if (request.getGenerationRequest() != null) {
                report.merge(validateTokenGeneration(request.getGenerationRequest(), ValidationMode.AUDIT));
            }

            if (request.getTokenValidationRequest() != null) {
                report.merge(validateTokenValidation(request.getTokenValidationRequest(), ValidationMode.AUDIT));
            }

            FIPSComplianceChecker.FIPSComplianceReport fipsReport = FIPSComplianceChecker.checkCompliance(mapFipsMode(mode));
            report.setFipsCompliant(fipsReport.isCompliant());
            report.addDetail("fipsViolationCount", Integer.valueOf(fipsReport.getViolationCount()));
            report.addDetail("fipsSeverity", fipsReport.getHighestSeverity().name());

            for (String violation : fipsReport.getViolations()) {
                report.addIssue(ValidationSeverity.ERROR, "fips-audit", violation);
            }

            appendAttackSummary(report);
            appendMetricsSummary(report);
            appendAuditRecommendations(report, fipsReport);
        } catch (RuntimeException e) {
            Tr.error(tc, "CWWKS4264E: Unexpected security audit error: " + e.getMessage());
            report.addError("audit-runtime", "Unexpected security audit error: " + e.getMessage());
        }

        return finalizeReport(report, start, "audit", request.getUserId(), null);
    }

    /**
     * Returns current aggregated security metrics.
     *
     * @return current metrics snapshot
     */
    public SecurityMetricsCollector.MetricsSummary getSecurityMetrics() {
        return metricsCollector.getSummary();
    }

    private SecurityValidationReport createReport(String operation, ValidationMode mode) {
        SecurityValidationReport report = new SecurityValidationReport(operation, mode);
        report.setFipsEnabled(FIPSModeDetector.isFIPSEnabled());
        return report;
    }

    private void validateGenerationInputs(TokenGenerationRequest request, SecurityValidationReport report) {
        if (request.getTokenPayload() == null || request.getTokenPayload().length == 0) {
            report.addError("generation-payload", "Token payload must not be null or empty.");
        }

        if (request.getTokenVersion() == TokenVersionValidator.VERSION_HYBRID && request.getPqcAlgorithm() == null) {
            report.addError("generation-pqc", "Hybrid token generation requires a PQC algorithm.");
        }

        if (request.getCreationTimeMs() > 0 && request.getExpirationTimeMs() > 0) {
            TokenExpirationValidator expirationValidator = new TokenExpirationValidator();
            TokenExpirationValidator.ValidationResult result = expirationValidator.validate(request.getCreationTimeMs(), request.getExpirationTimeMs());
            if (!result.isValid()) {
                report.addError("generation-timing", result.getErrorMessage());
            }
        }
    }

    private void validateGenerationConfiguration(TokenGenerationRequest request, SecurityValidationReport report) {
        if (request.getSecurityConfiguration() == null || request.getAlgorithmConfiguration() == null) {
            report.addWarning("Generation request does not provide full configuration objects; configuration orchestration was partially skipped.");
            return;
        }

        SecurityValidationReport configurationReport = validateConfiguration(
                        new ConfigurationValidationRequest(request.getUserId(),
                                        request.getSecurityConfiguration(),
                                        request.getAlgorithmConfiguration(),
                                        request.isFallbackToClassicalAllowed()),
                        report.getMode());

        report.merge(configurationReport);
    }

    private void validateGenerationAlgorithms(TokenGenerationRequest request, SecurityValidationReport report) {
        if (request.getClassicalSignatureAlgorithm() != null && !FIPSComplianceChecker.validateAlgorithm(request.getClassicalSignatureAlgorithm())) {
            report.addError("generation-classical-algorithm", "Configured classical signature algorithm is not valid for the active FIPS policy.");
        }

        if (request.getPqcAlgorithm() != null && !FIPSComplianceChecker.validateAlgorithm(request.getPqcAlgorithm().getNistName())) {
            report.addError("generation-pqc-algorithm", "Configured PQC algorithm is not valid for the active FIPS policy.");
        }
    }

    private void validateGenerationKeys(TokenGenerationRequest request, SecurityValidationReport report) {
        if (request.getRsaPrivateKey() != null) {
            validateKeyInternal(request.getRsaPrivateKey(), "RSA", "generation-rsa-private", report);
        }

        if (request.getPqcPrivateKey() != null && request.getPqcAlgorithm() != null) {
            validateKeyInternal(request.getPqcPrivateKey(), request.getPqcAlgorithm().getNistName(), "generation-pqc-private", report);
        }
    }

    private void validateGenerationFips(TokenGenerationRequest request, SecurityValidationReport report) {
        if (!FIPSModeDetector.isFIPSEnabled()) {
            return;
        }

        if (!FIPSModeDetector.validateFIPSConfiguration()) {
            report.addError("generation-fips", "FIPS mode is enabled but the underlying FIPS configuration is invalid.");
        }

        if (request.getPqcAlgorithm() != null && !FIPSComplianceChecker.validateAlgorithm(request.getPqcAlgorithm().getNistName())) {
            report.addError("generation-fips-pqc", "PQC algorithm is not permitted in FIPS mode.");
        }
    }

    private void validateTokenStructure(TokenValidationRequest request, SecurityValidationReport report) {
        if (request.getTokenData() == null || request.getTokenData().length == 0) {
            report.addError("token-data", "Token data must not be null or empty.");
            return;
        }

        boolean tampered = tamperingDetector.detectTampering(request.getTokenData(), request.getUserId());
        if (tampered) {
            report.addAttack("tampering", "Token structure tampering detected.");
            eventLogger.logTamperingDetection(request.getUserId(), "TOKEN_STRUCTURE");
        }
    }

    private void validateTokenVersionAndMetadata(TokenValidationRequest request, SecurityValidationReport report) {
        TokenVersionValidator validator = new TokenVersionValidator(request.isPqcRequired(), report.getMode() == ValidationMode.STRICT);
        TokenVersionValidator.ValidationResult result = validator.validate(
                        request.getTokenVersion(),
                        request.isPqcEnabled(),
                        request.getPqcAlgorithm() == null ? null : request.getPqcAlgorithm().getNistName());

        if (!result.isValid()) {
            report.addError("token-version", result.getErrorMessage());
        }

        boolean metadataTampering = tamperingDetector.detectMetadataTampering(request.getTokenVersion(), request.getCreationTimeMs(), request.getUserId());
        if (metadataTampering) {
            report.addAttack("metadata-tampering", "Token metadata tampering detected.");
            eventLogger.logTamperingDetection(request.getUserId(), "METADATA");
        }
    }

    private void validateTokenTiming(TokenValidationRequest request, SecurityValidationReport report) {
        TokenExpirationValidator validator = new TokenExpirationValidator(
                        request.getClockSkewToleranceMs(),
                        request.getMaxTokenLifetimeMs(),
                        report.getMode() == ValidationMode.STRICT);

        TokenExpirationValidator.ValidationResult result = validator.validate(request.getCreationTimeMs(), request.getExpirationTimeMs());
        if (!result.isValid()) {
            report.addError("token-expiration", result.getErrorMessage());
            if (result.getFailureReason() == TokenExpirationValidator.ValidationFailureReason.EXPIRED) {
                metricsCollector.recordExpiredToken(request.getExpirationTimeMs());
            }
        }
    }

    private void detectRuntimeAttacks(TokenValidationRequest request, SecurityValidationReport report) {
        boolean downgradeDetected = false;
        try {
            downgradeDetected = downgradeAttackDetector.detectDowngrade(
                            defaultUserId(request.getUserId()),
                            request.getTokenVersion(),
                            request.getPreviousTokenVersion());
        } catch (IllegalArgumentException e) {
            report.addWarning("Downgrade detection skipped: " + e.getMessage());
        }

        if (downgradeDetected) {
            report.addAttack("downgrade", "Potential downgrade attack detected.");
            eventLogger.logDowngradeAttempt(request.getUserId(), request.getPreviousTokenVersion() == null ? -1 : request.getPreviousTokenVersion().intValue(),
                            request.getTokenVersion());
        }

        boolean replayDetected = false;
        try {
            replayDetected = replayAttackDetector.detectReplay(request.getTokenData(), defaultUserId(request.getUserId()));
        } catch (IllegalArgumentException e) {
            report.addWarning("Replay detection skipped: " + e.getMessage());
        }

        if (replayDetected) {
            report.addAttack("replay", "Potential replay attack detected.");
            eventLogger.logReplayAttempt(request.getUserId(), fingerprintForLog(request.getTokenData()));
        }

        if (request.getExpectedRsaSignature() != null && request.getRsaSignature() != null) {
            boolean signatureTampering = tamperingDetector.detectSignatureTampering(
                            request.getExpectedRsaSignature(),
                            request.getRsaSignature(),
                            request.getUserId());

            if (signatureTampering) {
                report.addAttack("signature-tampering", "Signature tampering detected.");
                eventLogger.logTamperingDetection(request.getUserId(), "RSA_SIGNATURE");
            }
        }
    }

    private void validateTokenSignatures(TokenValidationRequest request, SecurityValidationReport report) {
        if (request.getRsaPublicKey() == null) {
            report.addError("rsa-public-key", "RSA public key is required for token validation.");
            return;
        }

        try {
            PQCProvider provider = resolveProvider(request.isRequireFipsProvider());
            SignatureChainValidator signatureValidator = new SignatureChainValidator(provider, report.getMode() == ValidationMode.STRICT);

            boolean valid;
            byte[] payload = request.getTokenPayload() != null ? request.getTokenPayload() : request.getTokenData();
            if (payload == null || payload.length == 0) {
                report.addError("signature-payload", "Token payload is required for signature validation.");
                return;
            }

            if (request.getTokenVersion() == TokenVersionValidator.VERSION_HYBRID) {
                if (request.getRsaSignature() == null || request.getPqcSignature() == null || request.getPqcPublicKey() == null || request.getPqcAlgorithm() == null) {
                    report.addError("pqc-signature", "Hybrid token validation requires RSA signature, PQC signature, PQC public key, and PQC algorithm.");
                    return;
                }
                valid = signatureValidator.validateHybridToken(
                                payload,
                                request.getRsaSignature(),
                                request.getPqcSignature(),
                                request.getRsaPublicKey(),
                                request.getPqcPublicKey(),
                                request.getPqcAlgorithm());
            } else {
                if (request.getRsaSignature() == null) {
                    report.addError("rsa-signature", "Classical token validation requires an RSA signature.");
                    return;
                }
                valid = signatureValidator.validateClassicalToken(
                                payload,
                                request.getRsaSignature(),
                                request.getRsaPublicKey());
            }

            if (!valid) {
                report.addError("signature-chain", "Token signature chain validation failed.");
            }
        } catch (PQCException e) {
            report.addError("pqc-provider", "Unable to obtain PQC provider: " + e.getMessage());
        }
    }

    private void validateTimingResistance(TokenValidationRequest request, SecurityValidationReport report) {
        if (request.getExpectedRsaSignature() != null && request.getRsaSignature() != null) {
            boolean timingSafe = TimingAttackResistantValidator.validateSignature(
                            safeBytes(request.getTokenPayload()),
                            request.getExpectedRsaSignature(),
                            request.getRsaSignature());

            if (!timingSafe) {
                report.addWarning("Timing-resistant validation rejected the RSA signature comparison.");
            }
        }
    }

    private void validateValidationKeys(TokenValidationRequest request, SecurityValidationReport report) {
        validateKeyInternal(request.getRsaPublicKey(), "RSA", "validation-rsa-public", report);

        if (request.getPqcPublicKey() != null && request.getPqcAlgorithm() != null) {
            validateKeyInternal(request.getPqcPublicKey(), request.getPqcAlgorithm().getNistName(), "validation-pqc-public", report);
        }
    }

    private void validateRuntimeFips(TokenValidationRequest request, SecurityValidationReport report) {
        if (!FIPSModeDetector.isFIPSEnabled()) {
            return;
        }

        FIPSComplianceChecker.FIPSComplianceReport fipsReport = FIPSComplianceChecker.checkCompliance(mapFipsMode(report.getMode()));
        report.setFipsCompliant(fipsReport.isCompliant());

        if (!fipsReport.isCompliant()) {
            for (String violation : fipsReport.getViolations()) {
                report.addError("fips-runtime", violation);
            }
        }

        if (request.getPqcAlgorithm() != null && !FIPSComplianceChecker.validateAlgorithm(request.getPqcAlgorithm().getNistName())) {
            report.addError("fips-pqc-algorithm", "Runtime PQC algorithm is not compliant with FIPS requirements.");
        }
    }

    private void validateConfigurationFips(SecurityValidationReport report, ValidationMode mode) {
        FIPSComplianceChecker.FIPSComplianceReport fipsReport = FIPSComplianceChecker.checkCompliance(mapFipsMode(mode));
        report.setFipsCompliant(fipsReport.isCompliant());

        if (!fipsReport.isCompliant()) {
            for (String violation : fipsReport.getViolations()) {
                report.addIssue(ValidationSeverity.ERROR, "fips-configuration", violation);
            }
        }
    }

    private void validateKeyInternal(Key key, String algorithm, String component, SecurityValidationReport report) {
        if (key == null) {
            return;
        }

        KeyStrengthValidator.ValidationResult result = keyStrengthValidator.validateKeyStrength(key);
        mergeValidationResult(report, component, result.getErrors(), result.getWarnings());

        if (algorithm != null) {
            for (String recommendation : keyStrengthValidator.getRecommendations(algorithm)) {
                report.addRecommendation(recommendation);
            }
        }
    }

    private SecurityValidationReport finalizeReport(SecurityValidationReport report, long startTimeNs, String tokenType, String userId, String metricsTokenType) {
        long duration = System.nanoTime() - startTimeNs;
        report.setDurationNs(duration);
        report.addDetail("validationDurationMs", Double.valueOf(duration / 1_000_000.0));

        boolean success = isSuccessful(report);
        report.setSuccessful(success);

        String normalizedType = metricsTokenType == null ? normalizeTokenType(tokenType) : normalizeTokenType(metricsTokenType);
        if (success) {
            metricsCollector.recordValidationSuccess(normalizedType, duration);
            eventLogger.logValidationSuccess(userId, report.getResolvedTokenVersion());
        } else {
            metricsCollector.recordValidationFailure(normalizedType, defaultFailureReason(report));
            eventLogger.logValidationFailure(userId, report.getPrimaryFailureMessage());
        }

        report.setMetricsSummary(metricsCollector.getSummary());

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Security validation finalized: operation={0}, success={1}, durationMs={2}",
                            report.getOperation(), Boolean.valueOf(success), Double.valueOf(duration / 1_000_000.0));
        }

        return report;
    }

    private boolean isSuccessful(SecurityValidationReport report) {
        if (report.getMode() == ValidationMode.AUDIT) {
            return true;
        }
        if (report.getMode() == ValidationMode.LENIENT) {
            return report.getErrorCount() == 0 || report.hasOnlySoftErrors();
        }
        return report.getErrorCount() == 0;
    }

    private void mergeValidationReport(SecurityValidationReport report, String component, List<String> errors, List<String> warnings) {
        mergeValidationResult(report, component, errors, warnings);
    }

    private void mergeValidationResult(SecurityValidationReport report, String component, List<String> errors, List<String> warnings) {
        if (errors != null) {
            for (String error : errors) {
                report.addError(component, error);
            }
        }
        if (warnings != null) {
            for (String warning : warnings) {
                report.addWarning(component, warning);
            }
        }
    }

    private void appendAttackSummary(SecurityValidationReport report) {
        report.addDetail("downgradeAttempts", Long.valueOf(downgradeAttackDetector.getTotalDowngradeAttempts()));
        report.addDetail("downgradeDetections", Long.valueOf(downgradeAttackDetector.getTotalDowngradeDetections()));
        report.addDetail("replayAttempts", Long.valueOf(replayAttackDetector.getTotalReplayAttempts()));
        report.addDetail("replayDetections", Long.valueOf(replayAttackDetector.getTotalReplayDetections()));
        report.addDetail("tamperingAttempts", Long.valueOf(tamperingDetector.getTotalTamperingAttempts()));
        report.addDetail("signatureTamperingCount", Long.valueOf(tamperingDetector.getSignatureTamperingCount()));
        report.addDetail("payloadTamperingCount", Long.valueOf(tamperingDetector.getPayloadTamperingCount()));
        report.addDetail("metadataTamperingCount", Long.valueOf(tamperingDetector.getMetadataTamperingCount()));
    }

    private void appendMetricsSummary(SecurityValidationReport report) {
        SecurityMetricsCollector.MetricsSummary summary = metricsCollector.getSummary();
        report.setMetricsSummary(summary);
        report.addDetail("totalValidations", Long.valueOf(summary.getTotalValidations()));
        report.addDetail("successRate", Double.valueOf(summary.getSuccessRate()));
        report.addDetail("averageValidationTimeMs", Double.valueOf(summary.getAverageValidationTimeMs()));
        report.addDetail("failureReasons", summary.getFailureReasonCounts());
    }

    private void appendAuditRecommendations(SecurityValidationReport report, FIPSComplianceChecker.FIPSComplianceReport fipsReport) {
        if (!fipsReport.isCompliant()) {
            report.addRecommendation("Resolve FIPS compliance violations before enabling strict enforcement.");
        }
        if (report.getAttackCount() > 0) {
            report.addRecommendation("Review recent attack detections and correlate with trace/audit logs.");
        }
        if (report.getMetricsSummary() != null && report.getMetricsSummary().getFailedValidations() > 0) {
            report.addRecommendation("Investigate validation failure causes and adjust configuration or key material.");
        }
    }

    private FIPSComplianceChecker.ValidationMode mapFipsMode(ValidationMode mode) {
        if (mode == null) {
            return FIPSComplianceChecker.ValidationMode.STRICT;
        }

        switch (mode) {
            case AUDIT:
                return FIPSComplianceChecker.ValidationMode.AUDIT;
            case LENIENT:
                return FIPSComplianceChecker.ValidationMode.LENIENT;
            case STRICT:
            default:
                return FIPSComplianceChecker.ValidationMode.STRICT;
        }
    }

    private PQCProvider resolveProvider(boolean requireFips) throws PQCException {
        return PQCProviderFactory.getProvider(requireFips);
    }

    @Trivial
    private static byte[] safeBytes(byte[] input) {
        return input == null ? new byte[0] : input;
    }

    @Trivial
    private static String defaultUserId(String userId) {
        return userId == null || userId.trim().isEmpty() ? "anonymous" : userId;
    }

    @Trivial
    private static String normalizeTokenType(String tokenType) {
        if (tokenType == null) {
            return "unknown";
        }
        if ("hybrid".equalsIgnoreCase(tokenType) || "classical".equalsIgnoreCase(tokenType)) {
            return tokenType.toLowerCase();
        }
        return "unknown";
    }

    @Trivial
    private static String defaultFailureReason(SecurityValidationReport report) {
        return report.getPrimaryFailureMessage() == null ? "validation-failed" : report.getPrimaryFailureMessage();
    }

    @Trivial
    private static String fingerprintForLog(byte[] tokenData) {
        if (tokenData == null) {
            return "null-token";
        }
        return Integer.toHexString(java.util.Arrays.hashCode(tokenData));
    }

    private void cleanGenerationSensitiveData(TokenGenerationRequest request) {
        MemoryCleaner.cleanByteArray(request.getTokenPayload());
    }

    private void cleanValidationSensitiveData(TokenValidationRequest request) {
        MemoryCleaner.cleanByteArrays(
                        request.getTokenPayload(),
                        request.getTokenData(),
                        request.getRsaSignature(),
                        request.getPqcSignature(),
                        request.getExpectedRsaSignature());
    }

    /**
     * Service validation modes.
     */
    public enum ValidationMode {
        STRICT,
        LENIENT,
        AUDIT
    }

    /**
     * Severity for validation issues.
     */
    public enum ValidationSeverity {
        INFO,
        WARNING,
        ERROR
    }

    /**
     * Request model for token generation validation.
     */
    public static class TokenGenerationRequest {
        private final String userId;
        private final byte[] tokenPayload;
        private final int tokenVersion;
        private final long creationTimeMs;
        private final long expirationTimeMs;
        private final boolean fallbackToClassicalAllowed;
        private final String classicalSignatureAlgorithm;
        private final PQCAlgorithm pqcAlgorithm;
        private final PrivateKey rsaPrivateKey;
        private final PrivateKey pqcPrivateKey;
        private final SecurityConfigurationValidator.SecurityConfiguration securityConfiguration;
        private final AlgorithmConfigurationValidator.AlgorithmConfiguration algorithmConfiguration;

        public TokenGenerationRequest(String userId, byte[] tokenPayload, int tokenVersion, long creationTimeMs, long expirationTimeMs,
                        boolean fallbackToClassicalAllowed, String classicalSignatureAlgorithm, PQCAlgorithm pqcAlgorithm,
                        PrivateKey rsaPrivateKey, PrivateKey pqcPrivateKey,
                        SecurityConfigurationValidator.SecurityConfiguration securityConfiguration,
                        AlgorithmConfigurationValidator.AlgorithmConfiguration algorithmConfiguration) {
            this.userId = userId;
            this.tokenPayload = tokenPayload;
            this.tokenVersion = tokenVersion;
            this.creationTimeMs = creationTimeMs;
            this.expirationTimeMs = expirationTimeMs;
            this.fallbackToClassicalAllowed = fallbackToClassicalAllowed;
            this.classicalSignatureAlgorithm = classicalSignatureAlgorithm;
            this.pqcAlgorithm = pqcAlgorithm;
            this.rsaPrivateKey = rsaPrivateKey;
            this.pqcPrivateKey = pqcPrivateKey;
            this.securityConfiguration = securityConfiguration;
            this.algorithmConfiguration = algorithmConfiguration;
        }

        public String getUserId() {
            return userId;
        }

        public byte[] getTokenPayload() {
            return tokenPayload;
        }

        public int getTokenVersion() {
            return tokenVersion;
        }

        public long getCreationTimeMs() {
            return creationTimeMs;
        }

        public long getExpirationTimeMs() {
            return expirationTimeMs;
        }

        public boolean isFallbackToClassicalAllowed() {
            return fallbackToClassicalAllowed;
        }

        public String getClassicalSignatureAlgorithm() {
            return classicalSignatureAlgorithm;
        }

        public PQCAlgorithm getPqcAlgorithm() {
            return pqcAlgorithm;
        }

        public PrivateKey getRsaPrivateKey() {
            return rsaPrivateKey;
        }

        public PrivateKey getPqcPrivateKey() {
            return pqcPrivateKey;
        }

        public SecurityConfigurationValidator.SecurityConfiguration getSecurityConfiguration() {
            return securityConfiguration;
        }

        public AlgorithmConfigurationValidator.AlgorithmConfiguration getAlgorithmConfiguration() {
            return algorithmConfiguration;
        }

        public boolean isPqcEnabled() {
            return tokenVersion == TokenVersionValidator.VERSION_HYBRID || pqcAlgorithm != null;
        }

        public String getTokenType() {
            return tokenVersion == TokenVersionValidator.VERSION_HYBRID ? "hybrid" : "classical";
        }
    }

    /**
     * Request model for token validation orchestration.
     */
    public static class TokenValidationRequest {
        private final String userId;
        private final byte[] tokenData;
        private final byte[] tokenPayload;
        private final int tokenVersion;
        private final Integer previousTokenVersion;
        private final boolean pqcEnabled;
        private final boolean pqcRequired;
        private final boolean requireFipsProvider;
        private final PQCAlgorithm pqcAlgorithm;
        private final long creationTimeMs;
        private final long expirationTimeMs;
        private final long clockSkewToleranceMs;
        private final long maxTokenLifetimeMs;
        private final byte[] rsaSignature;
        private final byte[] pqcSignature;
        private final byte[] expectedRsaSignature;
        private final PublicKey rsaPublicKey;
        private final PublicKey pqcPublicKey;

        private TokenValidationRequest(Builder builder) {
            this.userId = builder.userId;
            this.tokenData = builder.tokenData;
            this.tokenPayload = builder.tokenPayload;
            this.tokenVersion = builder.tokenVersion;
            this.previousTokenVersion = builder.previousTokenVersion;
            this.pqcEnabled = builder.pqcEnabled;
            this.pqcRequired = builder.pqcRequired;
            this.requireFipsProvider = builder.requireFipsProvider;
            this.pqcAlgorithm = builder.pqcAlgorithm;
            this.creationTimeMs = builder.creationTimeMs;
            this.expirationTimeMs = builder.expirationTimeMs;
            this.clockSkewToleranceMs = builder.clockSkewToleranceMs <= 0 ? TokenExpirationValidator.DEFAULT_CLOCK_SKEW_MS : builder.clockSkewToleranceMs;
            this.maxTokenLifetimeMs = builder.maxTokenLifetimeMs <= 0 ? TokenExpirationValidator.DEFAULT_MAX_LIFETIME_MS : builder.maxTokenLifetimeMs;
            this.rsaSignature = builder.rsaSignature;
            this.pqcSignature = builder.pqcSignature;
            this.expectedRsaSignature = builder.expectedRsaSignature;
            this.rsaPublicKey = builder.rsaPublicKey;
            this.pqcPublicKey = builder.pqcPublicKey;
        }

        public String getUserId() {
            return userId;
        }

        public byte[] getTokenData() {
            return tokenData;
        }

        public byte[] getTokenPayload() {
            return tokenPayload;
        }

        public int getTokenVersion() {
            return tokenVersion;
        }

        public Integer getPreviousTokenVersion() {
            return previousTokenVersion;
        }

        public boolean isPqcEnabled() {
            return pqcEnabled;
        }

        public boolean isPqcRequired() {
            return pqcRequired;
        }

        public boolean isRequireFipsProvider() {
            return requireFipsProvider;
        }

        public PQCAlgorithm getPqcAlgorithm() {
            return pqcAlgorithm;
        }

        public long getCreationTimeMs() {
            return creationTimeMs;
        }

        public long getExpirationTimeMs() {
            return expirationTimeMs;
        }

        public long getClockSkewToleranceMs() {
            return clockSkewToleranceMs;
        }

        public long getMaxTokenLifetimeMs() {
            return maxTokenLifetimeMs;
        }

        public byte[] getRsaSignature() {
            return rsaSignature;
        }

        public byte[] getPqcSignature() {
            return pqcSignature;
        }

        public byte[] getExpectedRsaSignature() {
            return expectedRsaSignature;
        }

        public PublicKey getRsaPublicKey() {
            return rsaPublicKey;
        }

        public PublicKey getPqcPublicKey() {
            return pqcPublicKey;
        }

        public String getTokenType() {
            return tokenVersion == TokenVersionValidator.VERSION_HYBRID ? "hybrid" : "classical";
        }

        public static class Builder {
            private String userId;
            private byte[] tokenData;
            private byte[] tokenPayload;
            private int tokenVersion = TokenVersionValidator.VERSION_CLASSICAL;
            private Integer previousTokenVersion;
            private boolean pqcEnabled;
            private boolean pqcRequired;
            private boolean requireFipsProvider;
            private PQCAlgorithm pqcAlgorithm;
            private long creationTimeMs;
            private long expirationTimeMs;
            private long clockSkewToleranceMs;
            private long maxTokenLifetimeMs;
            private byte[] rsaSignature;
            private byte[] pqcSignature;
            private byte[] expectedRsaSignature;
            private PublicKey rsaPublicKey;
            private PublicKey pqcPublicKey;

            public Builder userId(String userId) { this.userId = userId; return this; }
            public Builder tokenData(byte[] tokenData) { this.tokenData = tokenData; return this; }
            public Builder tokenPayload(byte[] tokenPayload) { this.tokenPayload = tokenPayload; return this; }
            public Builder tokenVersion(int tokenVersion) { this.tokenVersion = tokenVersion; return this; }
            public Builder previousTokenVersion(Integer previousTokenVersion) { this.previousTokenVersion = previousTokenVersion; return this; }
            public Builder pqcEnabled(boolean pqcEnabled) { this.pqcEnabled = pqcEnabled; return this; }
            public Builder pqcRequired(boolean pqcRequired) { this.pqcRequired = pqcRequired; return this; }
            public Builder requireFipsProvider(boolean requireFipsProvider) { this.requireFipsProvider = requireFipsProvider; return this; }
            public Builder pqcAlgorithm(PQCAlgorithm pqcAlgorithm) { this.pqcAlgorithm = pqcAlgorithm; return this; }
            public Builder creationTimeMs(long creationTimeMs) { this.creationTimeMs = creationTimeMs; return this; }
            public Builder expirationTimeMs(long expirationTimeMs) { this.expirationTimeMs = expirationTimeMs; return this; }
            public Builder clockSkewToleranceMs(long clockSkewToleranceMs) { this.clockSkewToleranceMs = clockSkewToleranceMs; return this; }
            public Builder maxTokenLifetimeMs(long maxTokenLifetimeMs) { this.maxTokenLifetimeMs = maxTokenLifetimeMs; return this; }
            public Builder rsaSignature(byte[] rsaSignature) { this.rsaSignature = rsaSignature; return this; }
            public Builder pqcSignature(byte[] pqcSignature) { this.pqcSignature = pqcSignature; return this; }
            public Builder expectedRsaSignature(byte[] expectedRsaSignature) { this.expectedRsaSignature = expectedRsaSignature; return this; }
            public Builder rsaPublicKey(PublicKey rsaPublicKey) { this.rsaPublicKey = rsaPublicKey; return this; }
            public Builder pqcPublicKey(PublicKey pqcPublicKey) { this.pqcPublicKey = pqcPublicKey; return this; }

            public TokenValidationRequest build() {
                return new TokenValidationRequest(this);
            }
        }
    }

    /**
     * Request model for configuration validation.
     */
    public static class ConfigurationValidationRequest {
        private final String userId;
        private final SecurityConfigurationValidator.SecurityConfiguration securityConfiguration;
        private final AlgorithmConfigurationValidator.AlgorithmConfiguration algorithmConfiguration;
        private final boolean fallbackToClassicalAllowed;

        public ConfigurationValidationRequest(String userId,
                        SecurityConfigurationValidator.SecurityConfiguration securityConfiguration,
                        AlgorithmConfigurationValidator.AlgorithmConfiguration algorithmConfiguration,
                        boolean fallbackToClassicalAllowed) {
            this.userId = userId;
            this.securityConfiguration = securityConfiguration;
            this.algorithmConfiguration = algorithmConfiguration;
            this.fallbackToClassicalAllowed = fallbackToClassicalAllowed;
        }

        public String getUserId() {
            return userId;
        }

        public SecurityConfigurationValidator.SecurityConfiguration getSecurityConfiguration() {
            return securityConfiguration;
        }

        public AlgorithmConfigurationValidator.AlgorithmConfiguration getAlgorithmConfiguration() {
            return algorithmConfiguration;
        }

        public boolean isFallbackToClassicalAllowed() {
            return fallbackToClassicalAllowed;
        }
    }

    /**
     * Request model for key validation.
     */
    public static class KeyValidationRequest {
        private final String userId;
        private final Key primaryKey;
        private final String primaryAlgorithm;
        private final Key secondaryKey;
        private final String secondaryAlgorithm;
        private final Key sharedSecretKey;
        private final String sharedSecretAlgorithm;

        public KeyValidationRequest(String userId, Key primaryKey, String primaryAlgorithm,
                        Key secondaryKey, String secondaryAlgorithm,
                        Key sharedSecretKey, String sharedSecretAlgorithm) {
            this.userId = userId;
            this.primaryKey = primaryKey;
            this.primaryAlgorithm = primaryAlgorithm;
            this.secondaryKey = secondaryKey;
            this.secondaryAlgorithm = secondaryAlgorithm;
            this.sharedSecretKey = sharedSecretKey;
            this.sharedSecretAlgorithm = sharedSecretAlgorithm;
        }

        public String getUserId() {
            return userId;
        }

        public Key getPrimaryKey() {
            return primaryKey;
        }

        public String getPrimaryAlgorithm() {
            return primaryAlgorithm;
        }

        public Key getSecondaryKey() {
            return secondaryKey;
        }

        public String getSecondaryAlgorithm() {
            return secondaryAlgorithm;
        }

        public Key getSharedSecretKey() {
            return sharedSecretKey;
        }

        public String getSharedSecretAlgorithm() {
            return sharedSecretAlgorithm;
        }
    }

    /**
     * Request model for a full audit run.
     */
    public static class SecurityAuditRequest {
        private final String userId;
        private final ConfigurationValidationRequest configurationRequest;
        private final KeyValidationRequest keyValidationRequest;
        private final TokenGenerationRequest generationRequest;
        private final TokenValidationRequest tokenValidationRequest;

        public SecurityAuditRequest(String userId,
                        ConfigurationValidationRequest configurationRequest,
                        KeyValidationRequest keyValidationRequest,
                        TokenGenerationRequest generationRequest,
                        TokenValidationRequest tokenValidationRequest) {
            this.userId = userId;
            this.configurationRequest = configurationRequest;
            this.keyValidationRequest = keyValidationRequest;
            this.generationRequest = generationRequest;
            this.tokenValidationRequest = tokenValidationRequest;
        }

        public String getUserId() {
            return userId;
        }

        public ConfigurationValidationRequest getConfigurationRequest() {
            return configurationRequest;
        }

        public KeyValidationRequest getKeyValidationRequest() {
            return keyValidationRequest;
        }

        public TokenGenerationRequest getGenerationRequest() {
            return generationRequest;
        }

        public TokenValidationRequest getTokenValidationRequest() {
            return tokenValidationRequest;
        }
    }

    /**
     * Comprehensive unified validation report.
     */
    public static class SecurityValidationReport {
        private final String operation;
        private final ValidationMode mode;
        private final List<ValidationIssue> issues = new ArrayList<ValidationIssue>();
        private final List<String> recommendations = new ArrayList<String>();
        private final List<String> detectedAttacks = new ArrayList<String>();
        private final Map<String, Object> details = new ConcurrentHashMap<String, Object>();

        private boolean successful;
        private boolean fipsEnabled;
        private boolean fipsCompliant = true;
        private long durationNs;
        private int resolvedTokenVersion = -1;
        private SecurityMetricsCollector.MetricsSummary metricsSummary;

        private SecurityValidationReport(String operation, ValidationMode mode) {
            this.operation = operation;
            this.mode = mode == null ? ValidationMode.STRICT : mode;
        }

        public void addError(String component, String message) {
            addIssue(ValidationSeverity.ERROR, component, message);
        }

        public void addError(String message) {
            addIssue(ValidationSeverity.ERROR, "general", message);
        }

        public void addWarning(String component, String message) {
            addIssue(ValidationSeverity.WARNING, component, message);
        }

        public void addWarning(String message) {
            addIssue(ValidationSeverity.WARNING, "general", message);
        }

        public void addAttack(String type, String message) {
            detectedAttacks.add(type + ": " + message);
            addIssue(ValidationSeverity.ERROR, "attack-" + type, message);
        }

        public void addRecommendation(String recommendation) {
            if (recommendation != null && recommendation.length() > 0 && !recommendations.contains(recommendation)) {
                recommendations.add(recommendation);
            }
        }

        public void addDetail(String name, Object value) {
            if (name != null) {
                details.put(name, value);
            }
        }

        public void merge(SecurityValidationReport other) {
            if (other == null) {
                return;
            }
            this.issues.addAll(other.issues);
            this.recommendations.addAll(other.recommendations);
            this.detectedAttacks.addAll(other.detectedAttacks);
            this.details.putAll(other.details);
            if (!other.fipsCompliant) {
                this.fipsCompliant = false;
            }
            if (other.metricsSummary != null) {
                this.metricsSummary = other.metricsSummary;
            }
        }

        public void addIssue(ValidationSeverity severity, String component, String message) {
            if (message == null) {
                return;
            }

            ValidationIssue issue = new ValidationIssue(severity, component, message);
            issues.add(issue);

            if (severity == ValidationSeverity.ERROR && mode != ValidationMode.STRICT) {
                issue.setSoft(true);
            }
        }

        @Trivial
        public boolean isSuccessful() {
            return successful;
        }

        void setSuccessful(boolean successful) {
            this.successful = successful;
        }

        @Trivial
        public String getPrimaryFailureMessage() {
            for (ValidationIssue issue : issues) {
                if (issue.getSeverity() == ValidationSeverity.ERROR) {
                    return issue.getMessage();
                }
            }
            return null;
        }

        @Trivial
        public String getOperation() {
            return operation;
        }

        @Trivial
        public ValidationMode getMode() {
            return mode;
        }

        @Trivial
        public List<ValidationIssue> getIssues() {
            return Collections.unmodifiableList(issues);
        }

        @Trivial
        public int getErrorCount() {
            int count = 0;
            for (ValidationIssue issue : issues) {
                if (issue.getSeverity() == ValidationSeverity.ERROR) {
                    count++;
                }
            }
            return count;
        }

        @Trivial
        public int getWarningCount() {
            int count = 0;
            for (ValidationIssue issue : issues) {
                if (issue.getSeverity() == ValidationSeverity.WARNING) {
                    count++;
                }
            }
            return count;
        }

        @Trivial
        public int getAttackCount() {
            return detectedAttacks.size();
        }

        @Trivial
        public List<String> getDetectedAttacks() {
            return Collections.unmodifiableList(detectedAttacks);
        }

        @Trivial
        public List<String> getRecommendations() {
            return Collections.unmodifiableList(recommendations);
        }

        @Trivial
        public Map<String, Object> getDetails() {
            return Collections.unmodifiableMap(details);
        }

        @Trivial
        public boolean isFipsEnabled() {
            return fipsEnabled;
        }

        void setFipsEnabled(boolean fipsEnabled) {
            this.fipsEnabled = fipsEnabled;
        }

        @Trivial
        public boolean isFipsCompliant() {
            return fipsCompliant;
        }

        void setFipsCompliant(boolean fipsCompliant) {
            this.fipsCompliant = fipsCompliant;
        }

        @Trivial
        public long getDurationNs() {
            return durationNs;
        }

        void setDurationNs(long durationNs) {
            this.durationNs = durationNs;
        }

        @Trivial
        public SecurityMetricsCollector.MetricsSummary getMetricsSummary() {
            return metricsSummary;
        }

        void setMetricsSummary(SecurityMetricsCollector.MetricsSummary metricsSummary) {
            this.metricsSummary = metricsSummary;
        }

        @Trivial
        public int getResolvedTokenVersion() {
            return resolvedTokenVersion;
        }

        public void setResolvedTokenVersion(int resolvedTokenVersion) {
            this.resolvedTokenVersion = resolvedTokenVersion;
        }

        @Trivial
        boolean hasOnlySoftErrors() {
            for (ValidationIssue issue : issues) {
                if (issue.getSeverity() == ValidationSeverity.ERROR && !issue.isSoft()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Single issue in the unified report.
     */
    public static class ValidationIssue {
        private final ValidationSeverity severity;
        private final String component;
        private final String message;
        private boolean soft;

        ValidationIssue(ValidationSeverity severity, String component, String message) {
            this.severity = severity;
            this.component = component;
            this.message = message;
        }

        @Trivial
        public ValidationSeverity getSeverity() {
            return severity;
        }

        @Trivial
        public String getComponent() {
            return component;
        }

        @Trivial
        public String getMessage() {
            return message;
        }

        @Trivial
        public boolean isSoft() {
            return soft;
        }

        void setSoft(boolean soft) {
            this.soft = soft;
        }
    }
}

// Made with Bob
