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
package com.ibm.ws.security.token.ltpa.pqc;

/**
 * Constants for Post-Quantum Cryptography (PQC) support in LTPA tokens.
 * 
 * This class defines constants used throughout the PQC LTPA implementation,
 * including cryptographic modes, algorithm identifiers, and key properties.
 * 
 * @see <a href="https://github.ibm.com/websphere/WS-CD-Open/issues/35556">Issue #35556</a>
 */
public class PQCConstants {
    
    // ========== Cryptographic Modes ==========
    
    /** Classical mode: Uses only RSA signatures (LTPA v2.0 compatible) */
    public static final String CRYPTO_MODE_CLASSICAL = "classical";
    
    /** PQC mode: Uses only ML-DSA signatures (quantum-resistant) */
    public static final String CRYPTO_MODE_PQC = "pqc";
    
    /** Hybrid mode: Uses both RSA and ML-DSA signatures (defense-in-depth) */
    public static final String CRYPTO_MODE_HYBRID = "hybrid";
    
    // ========== PQC Algorithms (FIPS 204) ==========
    
    /** ML-DSA-44: NIST FIPS 204 algorithm with 128-bit security level */
    public static final String ALGORITHM_ML_DSA_44 = "ML-DSA-44";
    
    /** ML-DSA-65: NIST FIPS 204 algorithm with 192-bit security level (recommended) */
    public static final String ALGORITHM_ML_DSA_65 = "ML-DSA-65";
    
    /** ML-DSA-87: NIST FIPS 204 algorithm with 256-bit security level */
    public static final String ALGORITHM_ML_DSA_87 = "ML-DSA-87";
    
    // ========== Key File Properties ==========
    
    /** Property key for ML-DSA private key in LTPA key file */
    public static final String KEYIMPORT_MLDSA_PRIVATEKEY = "com.ibm.websphere.ltpa.MLDSAPrivateKey";
    
    /** Property key for ML-DSA public key in LTPA key file */
    public static final String KEYIMPORT_MLDSA_PUBLICKEY = "com.ibm.websphere.ltpa.MLDSAPublicKey";
    
    /** Property key for PQC algorithm identifier in LTPA key file */
    public static final String KEYIMPORT_PQC_ALGORITHM = "com.ibm.websphere.ltpa.PQCAlgorithm";
    
    /** Property key for crypto mode in LTPA key file */
    public static final String KEYIMPORT_CRYPTO_MODE = "com.ibm.websphere.ltpa.CryptoMode";
    
    // ========== LTPA Version ==========
    
    /** LTPA version 2.0: Classical RSA-based tokens */
    public static final String LTPA_VERSION_2_0 = "2.0";
    
    /** LTPA version 3.0: PQC-enabled tokens (ML-DSA support) */
    public static final String LTPA_VERSION_3_0 = "3.0";
    
    // ========== Crypto Providers ==========
    
    /** OpenJCEPlus provider (preferred for PQC) */
    public static final String PROVIDER_OPENJCEPLUS = "OpenJCEPlus";
    
    /** IBMJCEPlus provider (alternative for PQC) */
    public static final String PROVIDER_IBMJCEPLUS = "IBMJCEPlus";
    
    /** IBMJCECCA provider (alternative for PQC) */
    public static final String PROVIDER_IBMJCECCA = "IBMJCECCA";
    
    // ========== Token Format ==========
    
    /** Token version byte for classical LTPA tokens */
    public static final byte TOKEN_VERSION_CLASSICAL = 0x00;
    
    /** Token version byte for PQC LTPA tokens */
    public static final byte TOKEN_VERSION_PQC = 0x01;
    
    /** Token version byte for hybrid LTPA tokens */
    public static final byte TOKEN_VERSION_HYBRID = 0x02;
    
    // ========== Configuration Defaults ==========
    
    /** Default crypto mode (classical for backward compatibility) */
    public static final String DEFAULT_CRYPTO_MODE = CRYPTO_MODE_CLASSICAL;
    
    /** Default PQC algorithm (ML-DSA-65 for 192-bit security level - NIST Category 3) */
    public static final String DEFAULT_PQC_ALGORITHM = ALGORITHM_ML_DSA_65;
    
    /** Default PQC enablement (disabled for backward compatibility) */
    public static final boolean DEFAULT_ENABLE_PQC = false;
    
    // Private constructor to prevent instantiation
    private PQCConstants() {
        throw new AssertionError("PQCConstants should not be instantiated");
    }
}
