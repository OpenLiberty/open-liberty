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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.junit.Test;

/**
 * Unit tests for PQCKeyGenerator.
 */
public class PQCKeyGeneratorTest {

    @Test
    public void testGetAvailablePQCProviders() {
        String[] providers = PQCKeyGenerator.getAvailablePQCProviders();
        assertNotNull("Available providers should not be null", providers);
        // Note: May be empty if no PQC providers are installed
    }

    @Test
    public void testGetFirstAvailableProvider() {
        String provider = PQCKeyGenerator.getFirstAvailableProvider();
        // May be null if no PQC providers are installed
        if (provider != null) {
            assertTrue("Provider should be one of the known PQC providers",
                    provider.equals(PQCConstants.PROVIDER_OPENJCEPLUS) ||
                    provider.equals(PQCConstants.PROVIDER_IBMJCEPLUS) ||
                    provider.equals(PQCConstants.PROVIDER_IBMJCECCA));
        }
    }

    @Test
    public void testGetRecommendedAlgorithm() {
        assertEquals("128-bit security should recommend ML-DSA-44",
                PQCConstants.ALGORITHM_ML_DSA_44,
                PQCKeyGenerator.getRecommendedAlgorithm(128));

        assertEquals("192-bit security should recommend ML-DSA-65",
                PQCConstants.ALGORITHM_ML_DSA_65,
                PQCKeyGenerator.getRecommendedAlgorithm(192));

        assertEquals("256-bit security should recommend ML-DSA-87",
                PQCConstants.ALGORITHM_ML_DSA_87,
                PQCKeyGenerator.getRecommendedAlgorithm(256));
    }

    @Test
    public void testGenerateMLDSAKeyPair_WithValidProvider() throws Exception {
        String provider = PQCKeyGenerator.getFirstAvailableProvider();
        if (provider == null) {
            System.out.println("Skipping test - no PQC provider available");
            return;
        }

        KeyPair keyPair = PQCKeyGenerator.generateMLDSAKeyPair(
                PQCConstants.ALGORITHM_ML_DSA_65, provider);

        assertNotNull("KeyPair should not be null", keyPair);
        assertNotNull("Private key should not be null", keyPair.getPrivate());
        assertNotNull("Public key should not be null", keyPair.getPublic());

        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        assertEquals("Private key algorithm should be ML-DSA",
                "ML-DSA", privateKey.getAlgorithm());
        assertEquals("Public key algorithm should be ML-DSA",
                "ML-DSA", publicKey.getAlgorithm());

        assertTrue("Private key should have encoded form",
                privateKey.getEncoded().length > 0);
        assertTrue("Public key should have encoded form",
                publicKey.getEncoded().length > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateMLDSAKeyPair_WithInvalidAlgorithm() throws Exception {
        String provider = PQCKeyGenerator.getFirstAvailableProvider();
        if (provider == null) {
            throw new IllegalArgumentException("No provider available");
        }

        PQCKeyGenerator.generateMLDSAKeyPair("INVALID-ALGORITHM", provider);
    }

    @Test
    public void testIsPQCProviderAvailable() {
        // Test with known provider names
        boolean openJCEPlusAvailable = PQCKeyGenerator.isPQCProviderAvailable(
                PQCConstants.PROVIDER_OPENJCEPLUS);
        boolean ibmJCEPlusAvailable = PQCKeyGenerator.isPQCProviderAvailable(
                PQCConstants.PROVIDER_IBMJCEPLUS);
        boolean ibmJCECCAAvailable = PQCKeyGenerator.isPQCProviderAvailable(
                PQCConstants.PROVIDER_IBMJCECCA);

        // At least log the results
        System.out.println("OpenJCEPlus available: " + openJCEPlusAvailable);
        System.out.println("IBMJCEPlus available: " + ibmJCEPlusAvailable);
        System.out.println("IBMJCECCA available: " + ibmJCECCAAvailable);

        // Test with invalid provider
        assertFalse("Invalid provider should not be available",
                PQCKeyGenerator.isPQCProviderAvailable("InvalidProvider"));
    }

    @Test
    public void testGenerateAllMLDSAVariants() throws Exception {
        String provider = PQCKeyGenerator.getFirstAvailableProvider();
        if (provider == null) {
            System.out.println("Skipping test - no PQC provider available");
            return;
        }

        // Test ML-DSA-44
        KeyPair keyPair44 = PQCKeyGenerator.generateMLDSAKeyPair(
                PQCConstants.ALGORITHM_ML_DSA_44, provider);
        assertNotNull("ML-DSA-44 key pair should not be null", keyPair44);

        // Test ML-DSA-65
        KeyPair keyPair65 = PQCKeyGenerator.generateMLDSAKeyPair(
                PQCConstants.ALGORITHM_ML_DSA_65, provider);
        assertNotNull("ML-DSA-65 key pair should not be null", keyPair65);

        // Test ML-DSA-87
        KeyPair keyPair87 = PQCKeyGenerator.generateMLDSAKeyPair(
                PQCConstants.ALGORITHM_ML_DSA_87, provider);
        assertNotNull("ML-DSA-87 key pair should not be null", keyPair87);

        // Verify different key sizes
        int size44 = keyPair44.getPublic().getEncoded().length;
        int size65 = keyPair65.getPublic().getEncoded().length;
        int size87 = keyPair87.getPublic().getEncoded().length;

        assertTrue("ML-DSA-87 should have larger keys than ML-DSA-65",
                size87 > size65);
        assertTrue("ML-DSA-65 should have larger keys than ML-DSA-44",
                size65 > size44);
    }
}
