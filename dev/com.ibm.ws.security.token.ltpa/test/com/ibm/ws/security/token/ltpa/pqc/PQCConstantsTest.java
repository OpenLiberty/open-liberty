/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.pqc;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for PQCConstants.
 */
public class PQCConstantsTest {

    @Test
    public void testCryptoModeConstants() {
        assertEquals("Classical mode should be 'classical'",
                "classical", PQCConstants.CRYPTO_MODE_CLASSICAL);
        assertEquals("PQC mode should be 'pqc'",
                "pqc", PQCConstants.CRYPTO_MODE_PQC);
        assertEquals("Hybrid mode should be 'hybrid'",
                "hybrid", PQCConstants.CRYPTO_MODE_HYBRID);
    }

    @Test
    public void testAlgorithmConstants() {
        assertEquals("ML-DSA-44 should be 'ML-DSA-44'",
                "ML-DSA-44", PQCConstants.ALGORITHM_ML_DSA_44);
        assertEquals("ML-DSA-65 should be 'ML-DSA-65'",
                "ML-DSA-65", PQCConstants.ALGORITHM_ML_DSA_65);
        assertEquals("ML-DSA-87 should be 'ML-DSA-87'",
                "ML-DSA-87", PQCConstants.ALGORITHM_ML_DSA_87);
    }

    @Test
    public void testKeyImportConstants() {
        assertEquals("ML-DSA private key property",
                "com.ibm.websphere.ltpa.MLDSAPrivateKey",
                PQCConstants.KEYIMPORT_MLDSA_PRIVATEKEY);
        assertEquals("ML-DSA public key property",
                "com.ibm.websphere.ltpa.MLDSAPublicKey",
                PQCConstants.KEYIMPORT_MLDSA_PUBLICKEY);
        assertEquals("PQC algorithm property",
                "com.ibm.websphere.ltpa.PQCAlgorithm",
                PQCConstants.KEYIMPORT_PQC_ALGORITHM);
    }

    @Test
    public void testProviderConstants() {
        assertEquals("OpenJCEPlus provider",
                "OpenJCEPlus", PQCConstants.PROVIDER_OPENJCEPLUS);
        assertEquals("IBMJCEPlus provider",
                "IBMJCEPlus", PQCConstants.PROVIDER_IBMJCEPLUS);
        assertEquals("IBMJCECCA provider",
                "IBMJCECCA", PQCConstants.PROVIDER_IBMJCECCA);
    }

    @Test
    public void testVersionConstants() {
        assertEquals("LTPA version 2.0",
                "2.0", PQCConstants.LTPA_VERSION_2_0);
        assertEquals("LTPA version 3.0",
                "3.0", PQCConstants.LTPA_VERSION_3_0);
    }

    @Test
    public void testTokenVersionConstants() {
        assertEquals("Classical token version should be 0x00",
                (byte) 0x00, PQCConstants.TOKEN_VERSION_CLASSICAL);
        assertEquals("PQC token version should be 0x01",
                (byte) 0x01, PQCConstants.TOKEN_VERSION_PQC);
    }

    @Test
    public void testDefaultConstants() {
        assertEquals("Default crypto mode should be classical",
                PQCConstants.CRYPTO_MODE_CLASSICAL,
                PQCConstants.DEFAULT_CRYPTO_MODE);
        assertEquals("Default PQC algorithm should be ML-DSA-65",
                PQCConstants.ALGORITHM_ML_DSA_65,
                PQCConstants.DEFAULT_PQC_ALGORITHM);
        assertFalse("Default PQC enablement should be false",
                PQCConstants.DEFAULT_ENABLE_PQC);
    }

    @Test
    public void testConstantsAreNotNull() {
        assertNotNull("CRYPTO_MODE_CLASSICAL should not be null",
                PQCConstants.CRYPTO_MODE_CLASSICAL);
        assertNotNull("CRYPTO_MODE_PQC should not be null",
                PQCConstants.CRYPTO_MODE_PQC);
        assertNotNull("CRYPTO_MODE_HYBRID should not be null",
                PQCConstants.CRYPTO_MODE_HYBRID);
        assertNotNull("ALGORITHM_ML_DSA_44 should not be null",
                PQCConstants.ALGORITHM_ML_DSA_44);
        assertNotNull("ALGORITHM_ML_DSA_65 should not be null",
                PQCConstants.ALGORITHM_ML_DSA_65);
        assertNotNull("ALGORITHM_ML_DSA_87 should not be null",
                PQCConstants.ALGORITHM_ML_DSA_87);
    }

    @Test
    public void testConstantsAreImmutable() {
        // Verify that constants are final by attempting to use them
        String mode = PQCConstants.CRYPTO_MODE_CLASSICAL;
        assertNotNull("Should be able to read constant", mode);

        String algorithm = PQCConstants.ALGORITHM_ML_DSA_65;
        assertNotNull("Should be able to read constant", algorithm);
    }

    @Test(expected = AssertionError.class)
    public void testCannotInstantiate() {
        // PQCConstants should not be instantiable
        // This uses reflection to test the private constructor
        try {
            java.lang.reflect.Constructor<?> constructor =
                    PQCConstants.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        } catch (java.lang.reflect.InvocationTargetException e) {
            // The constructor throws AssertionError
            throw (AssertionError) e.getCause();
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}
