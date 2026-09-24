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
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.token.ltpa.internal.TraceConstants;

/**
 * Validates cryptographic key strength for PQC-LTPA security configuration and
 * runtime key material.
 *
 * <p>This validator enforces minimum strength requirements for:
 * <ul>
 *   <li>RSA keys used for classical LTPA compatibility</li>
 *   <li>ML-DSA public/private key encodings for PQC signatures</li>
 *   <li>AES secret keys used in surrounding token protection flows</li>
 *   <li>ECDSA/EC keys where elliptic-curve support is configured</li>
 * </ul>
 *
 * <p><b>FIPS Enforcement:</b> When FIPS mode is enabled, stronger
 * recommendations become hard requirements where appropriate.
 *
 * <p><b>Thread Safety:</b> This class is immutable and thread-safe.
 */
public class KeyStrengthValidator {

    private static final TraceComponent tc = Tr.register(KeyStrengthValidator.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    /** Minimum RSA key size in bits. */
    public static final int MIN_RSA_BITS = 2048;

    /** Recommended RSA key size in bits. */
    public static final int RECOMMENDED_RSA_BITS = 3072;

    /** FIPS-preferred RSA key size in bits. */
    public static final int FIPS_RECOMMENDED_RSA_BITS = 3072;

    /** Minimum AES key size in bits. */
    public static final int MIN_AES_BITS = 128;

    /** FIPS-preferred AES key size in bits. */
    public static final int FIPS_RECOMMENDED_AES_BITS = 256;

    /** Minimum EC key size in bits, equivalent to P-256. */
    public static final int MIN_EC_BITS = 256;

    /** FIPS-preferred EC key size in bits. */
    public static final int FIPS_RECOMMENDED_EC_BITS = 384;

    /** Expected ML-DSA-44 public key encoding size in bytes. */
    public static final int ML_DSA_44_PUBLIC_KEY_BYTES = 1312;

    /** Expected ML-DSA-65 public key encoding size in bytes. */
    public static final int ML_DSA_65_PUBLIC_KEY_BYTES = 1952;

    /** Expected ML-DSA-87 public key encoding size in bytes. */
    public static final int ML_DSA_87_PUBLIC_KEY_BYTES = 2592;

    /** Expected ML-DSA-44 private key lower bound in bytes. */
    public static final int ML_DSA_44_PRIVATE_KEY_MIN_BYTES = 2420;

    /** Expected ML-DSA-65 private key lower bound in bytes. */
    public static final int ML_DSA_65_PRIVATE_KEY_MIN_BYTES = 4032;

    /** Expected ML-DSA-87 private key lower bound in bytes. */
    public static final int ML_DSA_87_PRIVATE_KEY_MIN_BYTES = 4896;

    /**
     * Validates a generic key and throws {@link SecurityException} on failure.
     *
     * @param key the key to validate
     * @throws SecurityException if the key does not satisfy minimum requirements
     */
    public void validateKey(Key key) throws SecurityException {
        ValidationResult result = validateKeyStrength(key);
        if (!result.isValid()) {
            throw new SecurityException(result.getPrimaryFailureMessage());
        }
    }

    /**
     * Validates a generic key and returns a detailed result.
     *
     * @param key the key to validate
     * @return the validation result
     */
    public ValidationResult validateKeyStrength(Key key) {
        ValidationResult result = new ValidationResult();

        if (key == null) {
            result.addError("Key must not be null.");
            return result;
        }

        String algorithm = normalizeAlgorithm(key.getAlgorithm());
        if (algorithm == null) {
            result.addError("Key algorithm must not be null or empty.");
            return result;
        }

        if (algorithm.contains("RSA")) {
            validateRsaKey(key, result);
        } else if (algorithm.contains("AES")) {
            validateAesKey(key, result);
        } else if (algorithm.contains("EC") || algorithm.contains("ECDSA")) {
            validateEcKey(key, result);
        } else if (algorithm.contains("ML-DSA") || algorithm.contains("MLDSA")) {
            validatePqcKey(key, algorithm, result);
        } else {
            result.addWarning("No explicit key strength rules are defined for algorithm '" + algorithm + "'.");
        }

        return result;
    }

    /**
     * Validates RSA key strength.
     *
     * @param key the RSA key
     * @throws SecurityException if invalid
     */
    public void validateRsaKey(Key key) throws SecurityException {
        ValidationResult result = new ValidationResult();
        validateRsaKey(key, result);
        if (!result.isValid()) {
            throw new SecurityException(result.getPrimaryFailureMessage());
        }
    }

    /**
     * Validates PQC key strength against expected ML-DSA encoding sizes.
     *
     * @param key the PQC key
     * @param algorithm the configured/declared PQC algorithm
     * @throws SecurityException if invalid
     */
    public void validatePqcKey(Key key, String algorithm) throws SecurityException {
        ValidationResult result = new ValidationResult();
        validatePqcKey(key, normalizeAlgorithm(algorithm), result);
        if (!result.isValid()) {
            throw new SecurityException(result.getPrimaryFailureMessage());
        }
    }

    /**
     * Validates AES key strength.
     *
     * @param key the AES key
     * @throws SecurityException if invalid
     */
    public void validateAesKey(Key key) throws SecurityException {
        ValidationResult result = new ValidationResult();
        validateAesKey(key, result);
        if (!result.isValid()) {
            throw new SecurityException(result.getPrimaryFailureMessage());
        }
    }

    /**
     * Validates EC/ECDSA key strength.
     *
     * @param key the EC key
     * @throws SecurityException if invalid
     */
    public void validateEcKey(Key key) throws SecurityException {
        ValidationResult result = new ValidationResult();
        validateEcKey(key, result);
        if (!result.isValid()) {
            throw new SecurityException(result.getPrimaryFailureMessage());
        }
    }

    /**
     * Provides key size recommendations for an algorithm.
     *
     * @param algorithm the algorithm name
     * @return recommendations
     */
    public List<String> getRecommendations(String algorithm) {
        String normalized = normalizeAlgorithm(algorithm);
        if (normalized == null) {
            return Collections.singletonList("Specify a non-empty algorithm name.");
        }

        List<String> recommendations = new ArrayList<String>();

        if (normalized.contains("RSA")) {
            recommendations.add("Use RSA keys of at least " + MIN_RSA_BITS + " bits.");
            recommendations.add("Prefer RSA " + RECOMMENDED_RSA_BITS + " bits or stronger for long-lived deployments.");
        } else if (normalized.contains("AES")) {
            recommendations.add("Use AES keys of 128, 192, or 256 bits only.");
            recommendations.add("Prefer AES-" + FIPS_RECOMMENDED_AES_BITS + " when FIPS or higher assurance is required.");
        } else if (normalized.contains("EC") || normalized.contains("ECDSA")) {
            recommendations.add("Use EC keys equivalent to P-256 or stronger.");
            recommendations.add("Prefer P-384 for FIPS-sensitive deployments.");
        } else if (normalized.contains("ML-DSA-44") || normalized.contains("MLDSA44")) {
            recommendations.add("ML-DSA-44 public keys should encode to approximately " + ML_DSA_44_PUBLIC_KEY_BYTES + " bytes.");
            recommendations.add("ML-DSA-44 private keys should meet or exceed " + ML_DSA_44_PRIVATE_KEY_MIN_BYTES + " bytes.");
        } else if (normalized.contains("ML-DSA-65") || normalized.contains("MLDSA65")) {
            recommendations.add("ML-DSA-65 public keys should encode to approximately " + ML_DSA_65_PUBLIC_KEY_BYTES + " bytes.");
            recommendations.add("ML-DSA-65 private keys should meet or exceed " + ML_DSA_65_PRIVATE_KEY_MIN_BYTES + " bytes.");
        } else if (normalized.contains("ML-DSA-87") || normalized.contains("MLDSA87")) {
            recommendations.add("ML-DSA-87 public keys should encode to approximately " + ML_DSA_87_PUBLIC_KEY_BYTES + " bytes.");
            recommendations.add("ML-DSA-87 private keys should meet or exceed " + ML_DSA_87_PRIVATE_KEY_MIN_BYTES + " bytes.");
        } else {
            recommendations.add("No algorithm-specific key strength recommendations are available.");
        }

        return Collections.unmodifiableList(recommendations);
    }

    private void validateRsaKey(Key key, ValidationResult result) {
        Integer modulusBits = extractRsaBits(key);
        if (modulusBits == null) {
            result.addError("Unable to determine RSA key length.");
            return;
        }

        if (modulusBits.intValue() < MIN_RSA_BITS) {
            result.addError("RSA key length " + modulusBits + " bits is below the minimum " + MIN_RSA_BITS + " bits.");
            return;
        }

        if (FIPSModeDetector.isFIPSEnabled() && modulusBits.intValue() < FIPS_RECOMMENDED_RSA_BITS) {
            result.addWarning("RSA key length " + modulusBits + " bits meets minimum requirements but FIPS deployments should prefer "
                            + FIPS_RECOMMENDED_RSA_BITS + " bits or stronger.");
        } else if (modulusBits.intValue() < RECOMMENDED_RSA_BITS) {
            result.addWarning("RSA key length " + modulusBits + " bits is acceptable but " + RECOMMENDED_RSA_BITS + " bits is recommended.");
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Validated RSA key length: {0} bits", modulusBits);
        }
    }

    private void validatePqcKey(Key key, String algorithm, ValidationResult result) {
        if (algorithm == null) {
            algorithm = normalizeAlgorithm(key.getAlgorithm());
        }

        byte[] encoded = key.getEncoded();
        if (encoded == null || encoded.length == 0) {
            result.addError("PQC key encoding is unavailable; cannot validate key strength.");
            return;
        }

        int length = encoded.length;
        int expectedPublic = getExpectedPublicKeyLength(algorithm);
        int expectedPrivateMin = getExpectedPrivateKeyMinLength(algorithm);

        if (expectedPublic < 0 && expectedPrivateMin < 0) {
            result.addError("Unsupported PQC algorithm '" + algorithm + "' for key strength validation.");
            return;
        }

        boolean matchesPublic = expectedPublic > 0 && length == expectedPublic;
        boolean matchesPrivate = expectedPrivateMin > 0 && length >= expectedPrivateMin;

        if (!matchesPublic && !matchesPrivate) {
            result.addError("PQC key encoded length " + length + " bytes does not match expected strength profile for " + algorithm + ".");
            return;
        }

        if (FIPSModeDetector.isFIPSEnabled()) {
            try {
                FIPSAlgorithmValidator.validateAlgorithmForFIPS(algorithm);
            } catch (SecurityException e) {
                result.addError("PQC key algorithm is not permitted in FIPS mode: " + e.getMessage());
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Validated PQC key strength for {0}: encodedLength={1}", algorithm, Integer.valueOf(length));
        }
    }

    private void validateAesKey(Key key, ValidationResult result) {
        Integer aesBits = extractSecretKeyBits(key);
        if (aesBits == null) {
            result.addError("Unable to determine AES key length.");
            return;
        }

        if (aesBits.intValue() != 128 && aesBits.intValue() != 192 && aesBits.intValue() != 256) {
            result.addError("AES key length " + aesBits + " bits is invalid. Supported lengths are 128, 192, and 256 bits.");
            return;
        }

        if (aesBits.intValue() < MIN_AES_BITS) {
            result.addError("AES key length " + aesBits + " bits is below minimum requirements.");
            return;
        }

        if (FIPSModeDetector.isFIPSEnabled() && aesBits.intValue() < FIPS_RECOMMENDED_AES_BITS) {
            result.addWarning("AES-" + aesBits + " is allowed, but AES-" + FIPS_RECOMMENDED_AES_BITS + " is preferred for FIPS-sensitive deployments.");
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Validated AES key length: {0} bits", aesBits);
        }
    }

    private void validateEcKey(Key key, ValidationResult result) {
        Integer ecBits = extractEcBits(key);
        if (ecBits == null) {
            result.addError("Unable to determine EC key strength.");
            return;
        }

        if (ecBits.intValue() < MIN_EC_BITS) {
            result.addError("EC key strength " + ecBits + " bits is below minimum P-256 equivalent requirements.");
            return;
        }

        if (FIPSModeDetector.isFIPSEnabled() && ecBits.intValue() < FIPS_RECOMMENDED_EC_BITS) {
            result.addWarning("EC key strength " + ecBits + " bits meets minimum requirements but P-384 equivalent strength is preferred in FIPS deployments.");
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Validated EC key strength: {0} bits", ecBits);
        }
    }

    @Trivial
    private Integer extractRsaBits(Key key) {
        if (key instanceof RSAKey) {
            return Integer.valueOf(((RSAKey) key).getModulus().bitLength());
        }

        byte[] encoded = key.getEncoded();
        if (encoded != null && encoded.length > 0) {
            return Integer.valueOf(encoded.length * 8);
        }

        return null;
    }

    @Trivial
    private Integer extractSecretKeyBits(Key key) {
        if (key instanceof SecretKey) {
            byte[] encoded = key.getEncoded();
            if (encoded != null) {
                return Integer.valueOf(encoded.length * 8);
            }
        }

        byte[] encoded = key.getEncoded();
        return encoded == null ? null : Integer.valueOf(encoded.length * 8);
    }

    @Trivial
    private Integer extractEcBits(Key key) {
        if (key instanceof ECKey) {
            return Integer.valueOf(((ECKey) key).getParams().getCurve().getField().getFieldSize());
        }

        byte[] encoded = key.getEncoded();
        if (encoded != null && encoded.length > 0) {
            return Integer.valueOf(Math.max(0, encoded.length * 8 / 2));
        }

        return null;
    }

    @Trivial
    private int getExpectedPublicKeyLength(String algorithm) {
        if (algorithm == null) {
            return -1;
        } else if (algorithm.contains("ML-DSA-44") || algorithm.contains("MLDSA44")) {
            return ML_DSA_44_PUBLIC_KEY_BYTES;
        } else if (algorithm.contains("ML-DSA-65") || algorithm.contains("MLDSA65")) {
            return ML_DSA_65_PUBLIC_KEY_BYTES;
        } else if (algorithm.contains("ML-DSA-87") || algorithm.contains("MLDSA87")) {
            return ML_DSA_87_PUBLIC_KEY_BYTES;
        }
        return -1;
    }

    @Trivial
    private int getExpectedPrivateKeyMinLength(String algorithm) {
        if (algorithm == null) {
            return -1;
        } else if (algorithm.contains("ML-DSA-44") || algorithm.contains("MLDSA44")) {
            return ML_DSA_44_PRIVATE_KEY_MIN_BYTES;
        } else if (algorithm.contains("ML-DSA-65") || algorithm.contains("MLDSA65")) {
            return ML_DSA_65_PRIVATE_KEY_MIN_BYTES;
        } else if (algorithm.contains("ML-DSA-87") || algorithm.contains("MLDSA87")) {
            return ML_DSA_87_PRIVATE_KEY_MIN_BYTES;
        }
        return -1;
    }

    @Trivial
    private String normalizeAlgorithm(String algorithm) {
        if (algorithm == null) {
            return null;
        }
        String trimmed = algorithm.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    /**
     * Validation result container for key strength checks.
     */
    public static class ValidationResult {

        private final List<String> errors = new ArrayList<String>();
        private final List<String> warnings = new ArrayList<String>();

        void addError(String message) {
            errors.add(message);
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "Key strength validation error: {0}", message);
            }
        }

        void addWarning(String message) {
            warnings.add(message);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Key strength validation warning: {0}", message);
            }
        }

        /**
         * @return true if no validation errors were recorded
         */
        @Trivial
        public boolean isValid() {
            return errors.isEmpty();
        }

        /**
         * @return immutable validation errors
         */
        @Trivial
        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        /**
         * @return immutable validation warnings
         */
        @Trivial
        public List<String> getWarnings() {
            return Collections.unmodifiableList(warnings);
        }

        /**
         * @return primary failure message or null
         */
        @Trivial
        public String getPrimaryFailureMessage() {
            return errors.isEmpty() ? null : errors.get(0);
        }

        /**
         * @return number of validation errors
         */
        @Trivial
        public int getErrorCount() {
            return errors.size();
        }

        /**
         * @return number of warnings
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
