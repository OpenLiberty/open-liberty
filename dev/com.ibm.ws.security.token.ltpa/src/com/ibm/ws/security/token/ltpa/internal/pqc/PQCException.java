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
package com.ibm.ws.security.token.ltpa.internal.pqc;

/**
 * Base exception for Post-Quantum Cryptography (PQC) operations in LTPA token processing.
 * 
 * <p>This exception is thrown when PQC cryptographic operations fail, including:</p>
 * <ul>
 *   <li>Key pair generation failures</li>
 *   <li>Signature generation failures</li>
 *   <li>Signature verification failures</li>
 *   <li>Key encoding/decoding failures</li>
 *   <li>Provider initialization failures</li>
 *   <li>Algorithm not supported errors</li>
 *   <li>FIPS compliance violations</li>
 * </ul>
 * 
 * <h3>Exception Hierarchy:</h3>
 * <pre>
 * PQCException (base)
 *   ├─ Wraps underlying cryptographic exceptions (NoSuchAlgorithmException, etc.)
 *   ├─ Provides context-specific error messages
 *   └─ Supports exception chaining for root cause analysis
 * </pre>
 * 
 * <h3>Usage Example:</h3>
 * <pre>
 * try {
 *     PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
 * } catch (PQCException e) {
 *     // Log error with full context
 *     logger.error("Failed to generate PQC key pair: " + e.getMessage(), e);
 *     // Handle gracefully (e.g., fall back to classical LTPA)
 * }
 * </pre>
 * 
 * <h3>Error Handling Guidelines:</h3>
 * <ul>
 *   <li><b>Logging:</b> Always log PQCException with full stack trace for debugging</li>
 *   <li><b>User Messages:</b> Provide user-friendly error messages without exposing sensitive details</li>
 *   <li><b>Recovery:</b> Consider fallback to classical LTPA if PQC is unavailable</li>
 *   <li><b>FFDC:</b> PQCException triggers First Failure Data Capture for problem determination</li>
 * </ul>
 * 
 * @see PQCProvider
 * @see PQCAlgorithm
 * @since 1.0
 */
public class PQCException extends Exception {
    
    /**
     * Serial version UID for serialization compatibility.
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new PQCException with no detail message.
     * 
     * <p>This constructor should be used sparingly. Prefer constructors that provide
     * a descriptive error message for better diagnostics.</p>
     */
    public PQCException() {
        super();
    }
    
    /**
     * Constructs a new PQCException with the specified detail message.
     * 
     * <p>The detail message should clearly describe the error condition and provide
     * sufficient context for problem determination. Include relevant details such as:</p>
     * <ul>
     *   <li>The operation that failed (e.g., "key generation", "signature verification")</li>
     *   <li>The algorithm involved (e.g., "ML-DSA-65")</li>
     *   <li>The provider being used (e.g., "BouncyCastle", "JEP 497")</li>
     *   <li>Any relevant configuration (e.g., "FIPS mode enabled")</li>
     * </ul>
     * 
     * <h4>Example Messages:</h4>
     * <pre>
     * "Failed to generate ML-DSA-65 key pair using BouncyCastle provider"
     * "Signature verification failed: invalid signature format"
     * "PQC provider not available: BouncyCastle PQC library not found"
     * "Algorithm ML-DSA-87 not supported in FIPS mode"
     * </pre>
     * 
     * @param message the detail message describing the error condition
     */
    public PQCException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new PQCException with the specified cause.
     * 
     * <p>This constructor is used to wrap underlying cryptographic exceptions while
     * preserving the original exception for root cause analysis. The cause is typically
     * a JCA/JCE exception such as:</p>
     * <ul>
     *   <li>{@link java.security.NoSuchAlgorithmException} - Algorithm not available</li>
     *   <li>{@link java.security.NoSuchProviderException} - Provider not available</li>
     *   <li>{@link java.security.InvalidKeyException} - Invalid key format</li>
     *   <li>{@link java.security.SignatureException} - Signature operation failed</li>
     *   <li>{@link java.security.spec.InvalidKeySpecException} - Key spec invalid</li>
     * </ul>
     * 
     * <p>The detail message is automatically set to the cause's message, or to the
     * cause's class name if the cause has no message.</p>
     * 
     * @param cause the underlying exception that caused this PQCException
     */
    public PQCException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Constructs a new PQCException with the specified detail message and cause.
     * 
     * <p>This is the most commonly used constructor, as it provides both a descriptive
     * error message and preserves the original exception for debugging. The message
     * should describe the high-level operation that failed, while the cause provides
     * the low-level technical details.</p>
     * 
     * <h4>Usage Pattern:</h4>
     * <pre>
     * try {
     *     KeyPairGenerator kpg = KeyPairGenerator.getInstance("ML-DSA-65");
     *     return kpg.generateKeyPair();
     * } catch (NoSuchAlgorithmException e) {
     *     throw new PQCException("Failed to generate ML-DSA-65 key pair: algorithm not available", e);
     * }
     * </pre>
     * 
     * @param message the detail message describing the error condition
     * @param cause the underlying exception that caused this PQCException
     */
    public PQCException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructs a new PQCException with the specified detail message, cause,
     * suppression enabled or disabled, and writable stack trace enabled or disabled.
     * 
     * <p>This constructor is provided for advanced use cases where fine-grained control
     * over exception behavior is required. Most code should use the simpler constructors.</p>
     * 
     * <h4>Parameters:</h4>
     * <ul>
     *   <li><b>enableSuppression:</b> Whether suppression is enabled or disabled.
     *       When enabled, suppressed exceptions (from try-with-resources) are recorded.</li>
     *   <li><b>writableStackTrace:</b> Whether the stack trace should be writable.
     *       Setting to false can improve performance for exceptions used for control flow.</li>
     * </ul>
     * 
     * <p><b>Note:</b> Disabling the writable stack trace should only be done for exceptions
     * that are used for control flow rather than error reporting, as it makes debugging
     * significantly more difficult.</p>
     * 
     * @param message the detail message describing the error condition
     * @param cause the underlying exception that caused this PQCException
     * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    protected PQCException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

// Made with Bob
