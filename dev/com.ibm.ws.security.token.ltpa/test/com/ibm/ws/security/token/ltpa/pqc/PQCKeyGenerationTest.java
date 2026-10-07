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

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

/**
 * Simple test to verify PQC ML-DSA key generation functionality.
 */
public class PQCKeyGenerationTest {

    public static void main(String[] args) {
        System.out.println("=== PQC LTPA Key Generation Test ===\n");
        
        boolean allTestsPassed = true;
        
        // Test 1: Check available PQC providers
        System.out.println("Test 1: Checking available PQC providers...");
        allTestsPassed &= testAvailableProviders();
        System.out.println();
        
        // Test 2: Generate ML-DSA-65 keys (192-bit security - recommended)
        System.out.println("Test 2: Generating ML-DSA-65 keys (192-bit security)...");
        allTestsPassed &= testKeyGeneration(PQCConstants.ALGORITHM_ML_DSA_65);
        System.out.println();
        
        // Test 3: Test signature generation and verification
        System.out.println("Test 3: Testing signature generation and verification...");
        allTestsPassed &= testSignatureOperations();
        System.out.println();
        
        // Summary
        System.out.println("=== Test Summary ===");
        if (allTestsPassed) {
            System.out.println("✓ ALL TESTS PASSED");
            System.out.println("\nPQC LTPA key generation is working correctly!");
            System.exit(0);
        } else {
            System.out.println("✗ SOME TESTS FAILED");
            System.out.println("\nPlease check the error messages above.");
            System.exit(1);
        }
    }
    
    private static boolean testAvailableProviders() {
        try {
            String[] providers = PQCKeyGenerator.getAvailablePQCProviders();
            
            if (providers.length == 0) {
                System.out.println("✗ FAILED: No PQC providers available");
                System.out.println("  Required: OpenJCEPlus, IBMJCEPlus, or IBMJCECCA");
                return false;
            }
            
            System.out.println("✓ PASSED: Found " + providers.length + " PQC provider(s)");
            for (String provider : providers) {
                System.out.println("  - " + provider);
            }
            
            return true;
        } catch (Exception e) {
            System.out.println("✗ FAILED: Exception checking providers: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testKeyGeneration(String algorithm) {
        try {
            String provider = PQCKeyGenerator.getFirstAvailableProvider();
            if (provider == null) {
                System.out.println("✗ FAILED: No PQC provider available");
                return false;
            }
            
            System.out.println("  Using provider: " + provider);
            
            long startTime = System.currentTimeMillis();
            KeyPair keyPair = PQCKeyGenerator.generateMLDSAKeyPair(algorithm, provider);
            long duration = System.currentTimeMillis() - startTime;
            
            if (keyPair == null) {
                System.out.println("✗ FAILED: Key pair generation returned null");
                return false;
            }
            
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();
            
            if (privateKey == null || publicKey == null) {
                System.out.println("✗ FAILED: Generated keys are null");
                return false;
            }
            
            System.out.println("  ✓ Key pair generated successfully in " + duration + "ms");
            System.out.println("  - Private key size: " + privateKey.getEncoded().length + " bytes");
            System.out.println("  - Public key size: " + publicKey.getEncoded().length + " bytes");
            
            int expectedSigSize = PQCSignatureHelper.getMLDSASignatureSize(algorithm);
            System.out.println("  - Expected signature size: " + expectedSigSize + " bytes");
            
            return true;
        } catch (Exception e) {
            System.out.println("✗ FAILED: Exception during key generation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testSignatureOperations() {
        try {
            String algorithm = PQCConstants.ALGORITHM_ML_DSA_65;
            String provider = PQCKeyGenerator.getFirstAvailableProvider();
            
            if (provider == null) {
                System.out.println("✗ FAILED: No PQC provider available");
                return false;
            }
            
            System.out.println("  Using algorithm: " + algorithm);
            System.out.println("  Using provider: " + provider);
            
            KeyPair keyPair = PQCKeyGenerator.generateMLDSAKeyPair(algorithm, provider);
            
            String testMessage = "This is a test message for PQC LTPA signature verification";
            byte[] data = testMessage.getBytes("UTF-8");
            
            System.out.println("  Signing test data...");
            long signStartTime = System.currentTimeMillis();
            byte[] signature = PQCSignatureHelper.signMLDSA(data, keyPair.getPrivate(), provider);
            long signDuration = System.currentTimeMillis() - signStartTime;
            
            if (signature == null || signature.length == 0) {
                System.out.println("✗ FAILED: Signature generation failed");
                return false;
            }
            
            System.out.println("  ✓ Signature generated in " + signDuration + "ms (" + signature.length + " bytes)");
            
            System.out.println("  Verifying signature...");
            long verifyStartTime = System.currentTimeMillis();
            boolean isValid = PQCSignatureHelper.verifyMLDSA(data, signature, keyPair.getPublic(), provider);
            long verifyDuration = System.currentTimeMillis() - verifyStartTime;
            
            if (!isValid) {
                System.out.println("✗ FAILED: Signature verification failed");
                return false;
            }
            
            System.out.println("  ✓ Signature verified successfully in " + verifyDuration + "ms");
            
            System.out.println("  Testing with tampered data...");
            byte[] tamperedData = Arrays.copyOf(data, data.length);
            tamperedData[0] ^= 0xFF;
            
            boolean shouldBeFalse = PQCSignatureHelper.verifyMLDSA(tamperedData, signature, keyPair.getPublic(), provider);
            
            if (shouldBeFalse) {
                System.out.println("✗ FAILED: Tampered data verification should have failed");
                return false;
            }
            
            System.out.println("  ✓ Tampered data correctly rejected");
            
            return true;
        } catch (Exception e) {
            System.out.println("✗ FAILED: Exception during signature operations: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
