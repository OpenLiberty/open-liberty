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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Unit tests for PQCAlgorithm enum.
 * 
 * <p>This test suite provides comprehensive coverage of the PQCAlgorithm enum,
 * including algorithm metadata, name mapping, and validation.</p>
 * 
 * <h3>Test Coverage Areas:</h3>
 * <ul>
 *   <li>Enum values and constants</li>
 *   <li>Algorithm metadata (key sizes, signature sizes, security levels)</li>
 *   <li>NIST name mapping</li>
 *   <li>BouncyCastle name mapping</li>
 *   <li>String parsing (fromString method)</li>
 *   <li>Invalid algorithm name handling</li>
 *   <li>toString() method</li>
 * </ul>
 */
public class PQCAlgorithmTest {
    
    // ========================================================================
    // Enum Values Tests
    // ========================================================================
    
    /**
     * Test that ML_DSA_65 enum constant exists and has correct NIST name.
     */
    @Test
    public void test_ML_DSA_65_EnumConstant() {
        PQCAlgorithm algorithm = PQCAlgorithm.ML_DSA_65;
        assertNotNull("ML_DSA_65 enum constant should not be null", algorithm);
        assertEquals("ML_DSA_65 NIST name should be 'ML-DSA-65'", 
                     "ML-DSA-65", algorithm.getNistName());
    }
    
    /**
     * Test that ML_DSA_87 enum constant exists and has correct NIST name.
     */
    @Test
    public void test_ML_DSA_87_EnumConstant() {
        PQCAlgorithm algorithm = PQCAlgorithm.ML_DSA_87;
        assertNotNull("ML_DSA_87 enum constant should not be null", algorithm);
        assertEquals("ML_DSA_87 NIST name should be 'ML-DSA-87'", 
                     "ML-DSA-87", algorithm.getNistName());
    }
    
    /**
     * Test that values() returns all enum constants.
     */
    @Test
    public void test_values_ReturnsAllConstants() {
        PQCAlgorithm[] algorithms = PQCAlgorithm.values();
        assertNotNull("values() should not return null", algorithms);
        assertEquals("Should have exactly 2 algorithm constants", 2, algorithms.length);
        assertEquals("First algorithm should be ML_DSA_65", PQCAlgorithm.ML_DSA_65, algorithms[0]);
        assertEquals("Second algorithm should be ML_DSA_87", PQCAlgorithm.ML_DSA_87, algorithms[1]);
    }
    
    // ========================================================================
    // Algorithm Metadata Tests - ML-DSA-65
    // ========================================================================
    
    /**
     * Test ML-DSA-65 NIST name.
     */
    @Test
    public void test_ML_DSA_65_getNistName() {
        assertEquals("ML-DSA-65 NIST name should be 'ML-DSA-65'",
                     "ML-DSA-65", PQCAlgorithm.ML_DSA_65.getNistName());
    }
    
    /**
     * Test ML-DSA-65 BouncyCastle name.
     */
    @Test
    public void test_ML_DSA_65_getBouncyCastleName() {
        assertEquals("ML-DSA-65 BouncyCastle name should be 'Dilithium3'",
                     "Dilithium3", PQCAlgorithm.ML_DSA_65.getBouncyCastleName());
    }
    
    /**
     * Test ML-DSA-65 security level.
     */
    @Test
    public void test_ML_DSA_65_getSecurityLevel() {
        assertEquals("ML-DSA-65 security level should be 3",
                     3, PQCAlgorithm.ML_DSA_65.getSecurityLevel());
    }
    
    /**
     * Test ML-DSA-65 public key size.
     */
    @Test
    public void test_ML_DSA_65_getPublicKeySize() {
        assertEquals("ML-DSA-65 public key size should be 1952 bytes",
                     1952, PQCAlgorithm.ML_DSA_65.getPublicKeySize());
    }
    
    /**
     * Test ML-DSA-65 private key size.
     */
    @Test
    public void test_ML_DSA_65_getPrivateKeySize() {
        assertEquals("ML-DSA-65 private key size should be 4000 bytes",
                     4000, PQCAlgorithm.ML_DSA_65.getPrivateKeySize());
    }
    
    /**
     * Test ML-DSA-65 signature size.
     */
    @Test
    public void test_ML_DSA_65_getSignatureSize() {
        assertEquals("ML-DSA-65 signature size should be 3293 bytes",
                     3293, PQCAlgorithm.ML_DSA_65.getSignatureSize());
    }
    
    // ========================================================================
    // Algorithm Metadata Tests - ML-DSA-87
    // ========================================================================
    
    /**
     * Test ML-DSA-87 NIST name.
     */
    @Test
    public void test_ML_DSA_87_getNistName() {
        assertEquals("ML-DSA-87 NIST name should be 'ML-DSA-87'",
                     "ML-DSA-87", PQCAlgorithm.ML_DSA_87.getNistName());
    }
    
    /**
     * Test ML-DSA-87 BouncyCastle name.
     */
    @Test
    public void test_ML_DSA_87_getBouncyCastleName() {
        assertEquals("ML-DSA-87 BouncyCastle name should be 'Dilithium5'",
                     "Dilithium5", PQCAlgorithm.ML_DSA_87.getBouncyCastleName());
    }
    
    /**
     * Test ML-DSA-87 security level.
     */
    @Test
    public void test_ML_DSA_87_getSecurityLevel() {
        assertEquals("ML-DSA-87 security level should be 5",
                     5, PQCAlgorithm.ML_DSA_87.getSecurityLevel());
    }
    
    /**
     * Test ML-DSA-87 public key size.
     */
    @Test
    public void test_ML_DSA_87_getPublicKeySize() {
        assertEquals("ML-DSA-87 public key size should be 2592 bytes",
                     2592, PQCAlgorithm.ML_DSA_87.getPublicKeySize());
    }
    
    /**
     * Test ML-DSA-87 private key size.
     */
    @Test
    public void test_ML_DSA_87_getPrivateKeySize() {
        assertEquals("ML-DSA-87 private key size should be 4864 bytes",
                     4864, PQCAlgorithm.ML_DSA_87.getPrivateKeySize());
    }
    
    /**
     * Test ML-DSA-87 signature size.
     */
    @Test
    public void test_ML_DSA_87_getSignatureSize() {
        assertEquals("ML-DSA-87 signature size should be 4595 bytes",
                     4595, PQCAlgorithm.ML_DSA_87.getSignatureSize());
    }
    
    // ========================================================================
    // Name Mapping Tests - NIST Names
    // ========================================================================
    
    /**
     * Test fromString with ML-DSA-65 NIST name.
     */
    @Test
    public void test_fromString_NIST_ML_DSA_65() {
        PQCAlgorithm algorithm = PQCAlgorithm.fromString("ML-DSA-65");
        assertEquals("fromString('ML-DSA-65') should return ML_DSA_65",
                     PQCAlgorithm.ML_DSA_65, algorithm);
    }
    
    /**
     * Test fromString with ML-DSA-87 NIST name.
     */
    @Test
    public void test_fromString_NIST_ML_DSA_87() {
        PQCAlgorithm algorithm = PQCAlgorithm.fromString("ML-DSA-87");
        assertEquals("fromString('ML-DSA-87') should return ML_DSA_87",
                     PQCAlgorithm.ML_DSA_87, algorithm);
    }
    
    // ========================================================================
    // Name Mapping Tests - BouncyCastle Names
    // ========================================================================
    
    /**
     * Test fromString with Dilithium3 BouncyCastle name.
     */
    @Test
    public void test_fromString_BouncyCastle_Dilithium3() {
        PQCAlgorithm algorithm = PQCAlgorithm.fromString("Dilithium3");
        assertEquals("fromString('Dilithium3') should return ML_DSA_65",
                     PQCAlgorithm.ML_DSA_65, algorithm);
    }
    
    /**
     * Test fromString with Dilithium5 BouncyCastle name.
     */
    @Test
    public void test_fromString_BouncyCastle_Dilithium5() {
        PQCAlgorithm algorithm = PQCAlgorithm.fromString("Dilithium5");
        assertEquals("fromString('Dilithium5') should return ML_DSA_87",
                     PQCAlgorithm.ML_DSA_87, algorithm);
    }
    
    // ========================================================================
    // Invalid Name Handling Tests
    // ========================================================================
    
    /**
     * Test fromString with null algorithm name throws IllegalArgumentException.
     */
    @Test
    public void test_fromString_NullName_ThrowsException() {
        try {
            PQCAlgorithm.fromString(null);
            fail("fromString(null) should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Exception message should indicate null algorithm name",
                         "Algorithm name cannot be null", e.getMessage());
        }
    }
    
    /**
     * Test fromString with empty string throws IllegalArgumentException.
     */
    @Test
    public void test_fromString_EmptyString_ThrowsException() {
        try {
            PQCAlgorithm.fromString("");
            fail("fromString('') should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertEquals("Exception message should indicate unsupported algorithm",
                         true, e.getMessage().startsWith("Unsupported PQC algorithm:"));
        }
    }
    
    /**
     * Test fromString with invalid algorithm name throws IllegalArgumentException.
     */
    @Test
    public void test_fromString_InvalidName_ThrowsException() {
        try {
            PQCAlgorithm.fromString("InvalidAlgorithm");
            fail("fromString('InvalidAlgorithm') should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertEquals("Exception message should indicate unsupported algorithm",
                         true, e.getMessage().contains("Unsupported PQC algorithm: InvalidAlgorithm"));
            assertEquals("Exception message should list supported algorithms",
                         true, e.getMessage().contains("ML-DSA-65"));
            assertEquals("Exception message should list supported algorithms",
                         true, e.getMessage().contains("ML-DSA-87"));
        }
    }
    
    /**
     * Test fromString with case-sensitive mismatch throws IllegalArgumentException.
     */
    @Test
    public void test_fromString_CaseSensitive_ThrowsException() {
        try {
            PQCAlgorithm.fromString("ml-dsa-65"); // lowercase
            fail("fromString('ml-dsa-65') should throw IllegalArgumentException (case-sensitive)");
        } catch (IllegalArgumentException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertEquals("Exception message should indicate unsupported algorithm",
                         true, e.getMessage().startsWith("Unsupported PQC algorithm:"));
        }
    }
    
    /**
     * Test fromString with Dilithium2 (not supported) throws IllegalArgumentException.
     */
    @Test
    public void test_fromString_Dilithium2_NotSupported() {
        try {
            PQCAlgorithm.fromString("Dilithium2");
            fail("fromString('Dilithium2') should throw IllegalArgumentException (not supported)");
        } catch (IllegalArgumentException e) {
            assertNotNull("Exception message should not be null", e.getMessage());
            assertEquals("Exception message should indicate unsupported algorithm",
                         true, e.getMessage().contains("Unsupported PQC algorithm: Dilithium2"));
        }
    }
    
    // ========================================================================
    // toString() Tests
    // ========================================================================
    
    /**
     * Test toString returns NIST name for ML-DSA-65.
     */
    @Test
    public void test_toString_ML_DSA_65() {
        assertEquals("toString() should return NIST name for ML-DSA-65",
                     "ML-DSA-65", PQCAlgorithm.ML_DSA_65.toString());
    }
    
    /**
     * Test toString returns NIST name for ML-DSA-87.
     */
    @Test
    public void test_toString_ML_DSA_87() {
        assertEquals("toString() should return NIST name for ML-DSA-87",
                     "ML-DSA-87", PQCAlgorithm.ML_DSA_87.toString());
    }
}

// Made with Bob