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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.token.ltpa.internal.pqc.bc.BouncyCastlePQCProvider;
import com.ibm.ws.security.token.ltpa.internal.pqc.native_.NativePQCProvider;

/**
 * Performance benchmark tests for PQC provider implementations.
 * 
 * <p>This test class measures and compares the performance characteristics of different
 * PQC provider implementations (BouncyCastle and Native Java) for key operations:
 * <ul>
 * <li>Key pair generation</li>
 * <li>Signature generation</li>
 * <li>Signature verification</li>
 * </ul>
 * 
 * <p><b>Performance Targets (from Phase 1 plan):</b>
 * <ul>
 * <li>Key Generation: <500ms</li>
 * <li>Sign (1KB data): <50ms</li>
 * <li>Verify (1KB data): <50ms</li>
 * </ul>
 * 
 * <p><b>Note:</b> These are benchmark tests, not strict pass/fail tests. They measure
 * performance characteristics and report results for analysis. Tests will skip if
 * providers are not available.
 * 
 * @see PQCProvider
 * @see BouncyCastlePQCProvider
 * @see NativePQCProvider
 */
public class PQCPerformanceBenchmark {

    private static final int WARMUP_ITERATIONS = 5;
    private static final int BENCHMARK_ITERATIONS = 10;
    private static final byte[] TEST_DATA_1KB = new byte[1024];
    private static final byte[] TEST_DATA_10KB = new byte[10240];

    private static boolean bouncyCastleAvailable = false;
    private static boolean nativeProviderAvailable = false;

    /**
     * Initialize test data and check provider availability.
     */
    @BeforeClass
    public static void setUpClass() {
        // Initialize test data with some pattern
        for (int i = 0; i < TEST_DATA_1KB.length; i++) {
            TEST_DATA_1KB[i] = (byte) (i % 256);
        }
        for (int i = 0; i < TEST_DATA_10KB.length; i++) {
            TEST_DATA_10KB[i] = (byte) (i % 256);
        }

        // Check BouncyCastle availability
        try {
            BouncyCastlePQCProvider bcProvider = new BouncyCastlePQCProvider();
            bouncyCastleAvailable = bcProvider.isAvailable();
        } catch (Exception e) {
            bouncyCastleAvailable = false;
        }

        // Check Native provider availability
        try {
            NativePQCProvider nativeProvider = new NativePQCProvider();
            nativeProviderAvailable = nativeProvider.isAvailable();
        } catch (Exception e) {
            nativeProviderAvailable = false;
        }

        System.out.println("=== PQC Performance Benchmark ===");
        System.out.println("BouncyCastle Provider Available: " + bouncyCastleAvailable);
        System.out.println("Native Provider Available: " + nativeProviderAvailable);
        System.out.println("Warmup Iterations: " + WARMUP_ITERATIONS);
        System.out.println("Benchmark Iterations: " + BENCHMARK_ITERATIONS);
        System.out.println("================================");
    }

    /**
     * Benchmark BouncyCastle provider key generation for ML-DSA-65.
     * 
     * <p>Target: <500ms per key pair generation
     */
    @Test
    public void testBouncyCastleKeyGenerationPerformance_MLDSA65() throws Exception {
        assumeTrue("BouncyCastle provider not available", bouncyCastleAvailable);

        BouncyCastlePQCProvider provider = new BouncyCastlePQCProvider();
        PQCAlgorithm algorithm = PQCAlgorithm.ML_DSA_65;

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            provider.generateKeyPair(algorithm);
        }

        // Benchmark
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            PQCKeyPair keyPair = provider.generateKeyPair(algorithm);
            long end = System.nanoTime();
            
            assertNotNull("Key pair should not be null", keyPair);
            times.add((end - start) / 1_000_000); // Convert to milliseconds
        }

        printStatistics("BouncyCastle ML-DSA-65 Key Generation", times, 500);
    }

    /**
     * Benchmark BouncyCastle provider signature generation for ML-DSA-65.
     * 
     * <p>Target: <50ms for 1KB data
     */
    @Test
    public void testBouncyCastleSignaturePerformance_MLDSA65() throws Exception {
        assumeTrue("BouncyCastle provider not available", bouncyCastleAvailable);

        BouncyCastlePQCProvider provider = new BouncyCastlePQCProvider();
        PQCAlgorithm algorithm = PQCAlgorithm.ML_DSA_65;

        // Generate key pair once
        PQCKeyPair keyPair = provider.generateKeyPair(algorithm);
        PrivateKey privateKey = keyPair.getPrivateKey();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            provider.sign(TEST_DATA_1KB, privateKey, algorithm);
        }

        // Benchmark
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            byte[] signature = provider.sign(TEST_DATA_1KB, privateKey, algorithm);
            long end = System.nanoTime();
            
            assertNotNull("Signature should not be null", signature);
            times.add((end - start) / 1_000_000); // Convert to milliseconds
        }

        printStatistics("BouncyCastle ML-DSA-65 Signature (1KB)", times, 50);
    }

    /**
     * Benchmark BouncyCastle provider signature verification for ML-DSA-65.
     * 
     * <p>Target: <50ms for 1KB data
     */
    @Test
    public void testBouncyCastleVerificationPerformance_MLDSA65() throws Exception {
        assumeTrue("BouncyCastle provider not available", bouncyCastleAvailable);

        BouncyCastlePQCProvider provider = new BouncyCastlePQCProvider();
        PQCAlgorithm algorithm = PQCAlgorithm.ML_DSA_65;

        // Generate key pair and signature once
        PQCKeyPair keyPair = provider.generateKeyPair(algorithm);
        PublicKey publicKey = keyPair.getPublicKey();
        PrivateKey privateKey = keyPair.getPrivateKey();
        byte[] signature = provider.sign(TEST_DATA_1KB, privateKey, algorithm);

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            provider.verify(TEST_DATA_1KB, signature, publicKey, algorithm);
        }

        // Benchmark
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            boolean valid = provider.verify(TEST_DATA_1KB, signature, publicKey, algorithm);
            long end = System.nanoTime();
            
            assertTrue("Signature should be valid", valid);
            times.add((end - start) / 1_000_000); // Convert to milliseconds
        }

        printStatistics("BouncyCastle ML-DSA-65 Verification (1KB)", times, 50);
    }

    /**
     * Benchmark Native provider key generation for ML-DSA-65.
     * 
     * <p>Target: <500ms per key pair generation
     */
    @Test
    public void testNativeKeyGenerationPerformance_MLDSA65() throws Exception {
        assumeTrue("Native provider not available", nativeProviderAvailable);

        NativePQCProvider provider = new NativePQCProvider();
        PQCAlgorithm algorithm = PQCAlgorithm.ML_DSA_65;

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            provider.generateKeyPair(algorithm);
        }

        // Benchmark
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            PQCKeyPair keyPair = provider.generateKeyPair(algorithm);
            long end = System.nanoTime();
            
            assertNotNull("Key pair should not be null", keyPair);
            times.add((end - start) / 1_000_000); // Convert to milliseconds
        }

        printStatistics("Native ML-DSA-65 Key Generation", times, 500);
    }

    /**
     * Benchmark Native provider signature generation for ML-DSA-65.
     * 
     * <p>Target: <50ms for 1KB data
     */
    @Test
    public void testNativeSignaturePerformance_MLDSA65() throws Exception {
        assumeTrue("Native provider not available", nativeProviderAvailable);

        NativePQCProvider provider = new NativePQCProvider();
        PQCAlgorithm algorithm = PQCAlgorithm.ML_DSA_65;

        // Generate key pair once
        PQCKeyPair keyPair = provider.generateKeyPair(algorithm);
        PrivateKey privateKey = keyPair.getPrivateKey();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            provider.sign(TEST_DATA_1KB, privateKey, algorithm);
        }

        // Benchmark
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            byte[] signature = provider.sign(TEST_DATA_1KB, privateKey, algorithm);
            long end = System.nanoTime();
            
            assertNotNull("Signature should not be null", signature);
            times.add((end - start) / 1_000_000); // Convert to milliseconds
        }

        printStatistics("Native ML-DSA-65 Signature (1KB)", times, 50);
    }

    /**
     * Benchmark Native provider signature verification for ML-DSA-65.
     * 
     * <p>Target: <50ms for 1KB data
     */
    @Test
    public void testNativeVerificationPerformance_MLDSA65() throws Exception {
        assumeTrue("Native provider not available", nativeProviderAvailable);

        NativePQCProvider provider = new NativePQCProvider();
        PQCAlgorithm algorithm = PQCAlgorithm.ML_DSA_65;

        // Generate key pair and signature once
        PQCKeyPair keyPair = provider.generateKeyPair(algorithm);
        PublicKey publicKey = keyPair.getPublicKey();
        PrivateKey privateKey = keyPair.getPrivateKey();
        byte[] signature = provider.sign(TEST_DATA_1KB, privateKey, algorithm);

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            provider.verify(TEST_DATA_1KB, signature, publicKey, algorithm);
        }

        // Benchmark
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            boolean valid = provider.verify(TEST_DATA_1KB, signature, publicKey, algorithm);
            long end = System.nanoTime();
            
            assertTrue("Signature should be valid", valid);
            times.add((end - start) / 1_000_000); // Convert to milliseconds
        }

        printStatistics("Native ML-DSA-65 Verification (1KB)", times, 50);
    }

    /**
     * Benchmark BouncyCastle provider with larger data (10KB).
     */
    @Test
    public void testBouncyCastlePerformance_10KB() throws Exception {
        assumeTrue("BouncyCastle provider not available", bouncyCastleAvailable);

        BouncyCastlePQCProvider provider = new BouncyCastlePQCProvider();
        PQCAlgorithm algorithm = PQCAlgorithm.ML_DSA_65;

        // Generate key pair once
        PQCKeyPair keyPair = provider.generateKeyPair(algorithm);
        PrivateKey privateKey = keyPair.getPrivateKey();
        PublicKey publicKey = keyPair.getPublicKey();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            byte[] sig = provider.sign(TEST_DATA_10KB, privateKey, algorithm);
            provider.verify(TEST_DATA_10KB, sig, publicKey, algorithm);
        }

        // Benchmark signature
        List<Long> signTimes = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            byte[] signature = provider.sign(TEST_DATA_10KB, privateKey, algorithm);
            long end = System.nanoTime();
            
            assertNotNull("Signature should not be null", signature);
            signTimes.add((end - start) / 1_000_000);
        }

        // Benchmark verification
        byte[] signature = provider.sign(TEST_DATA_10KB, privateKey, algorithm);
        List<Long> verifyTimes = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            boolean valid = provider.verify(TEST_DATA_10KB, signature, publicKey, algorithm);
            long end = System.nanoTime();
            
            assertTrue("Signature should be valid", valid);
            verifyTimes.add((end - start) / 1_000_000);
        }

        printStatistics("BouncyCastle ML-DSA-65 Signature (10KB)", signTimes, 100);
        printStatistics("BouncyCastle ML-DSA-65 Verification (10KB)", verifyTimes, 100);
    }

    /**
     * Benchmark Native provider with larger data (10KB).
     */
    @Test
    public void testNativePerformance_10KB() throws Exception {
        assumeTrue("Native provider not available", nativeProviderAvailable);

        NativePQCProvider provider = new NativePQCProvider();
        PQCAlgorithm algorithm = PQCAlgorithm.ML_DSA_65;

        // Generate key pair once
        PQCKeyPair keyPair = provider.generateKeyPair(algorithm);
        PrivateKey privateKey = keyPair.getPrivateKey();
        PublicKey publicKey = keyPair.getPublicKey();

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            byte[] sig = provider.sign(TEST_DATA_10KB, privateKey, algorithm);
            provider.verify(TEST_DATA_10KB, sig, publicKey, algorithm);
        }

        // Benchmark signature
        List<Long> signTimes = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            byte[] signature = provider.sign(TEST_DATA_10KB, privateKey, algorithm);
            long end = System.nanoTime();
            
            assertNotNull("Signature should not be null", signature);
            signTimes.add((end - start) / 1_000_000);
        }

        // Benchmark verification
        byte[] signature = provider.sign(TEST_DATA_10KB, privateKey, algorithm);
        List<Long> verifyTimes = new ArrayList<>();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            boolean valid = provider.verify(TEST_DATA_10KB, signature, publicKey, algorithm);
            long end = System.nanoTime();
            
            assertTrue("Signature should be valid", valid);
            verifyTimes.add((end - start) / 1_000_000);
        }

        printStatistics("Native ML-DSA-65 Signature (10KB)", signTimes, 100);
        printStatistics("Native ML-DSA-65 Verification (10KB)", verifyTimes, 100);
    }

    /**
     * Compare BouncyCastle and Native provider performance side-by-side.
     */
    @Test
    public void testProviderComparison() throws Exception {
        assumeTrue("At least one provider must be available", 
                   bouncyCastleAvailable || nativeProviderAvailable);

        System.out.println("\n=== Provider Performance Comparison ===");

        if (bouncyCastleAvailable && nativeProviderAvailable) {
            // Both available - do comparison
            BouncyCastlePQCProvider bcProvider = new BouncyCastlePQCProvider();
            NativePQCProvider nativeProvider = new NativePQCProvider();
            PQCAlgorithm algorithm = PQCAlgorithm.ML_DSA_65;

            // Key generation comparison
            long bcKeyGenTime = measureKeyGeneration(bcProvider, algorithm);
            long nativeKeyGenTime = measureKeyGeneration(nativeProvider, algorithm);
            
            System.out.println("Key Generation:");
            System.out.println("  BouncyCastle: " + bcKeyGenTime + "ms");
            System.out.println("  Native:       " + nativeKeyGenTime + "ms");
            System.out.println("  Speedup:      " + String.format("%.2fx", (double) bcKeyGenTime / nativeKeyGenTime));

            // Signature comparison
            PQCKeyPair bcKeyPair = bcProvider.generateKeyPair(algorithm);
            PQCKeyPair nativeKeyPair = nativeProvider.generateKeyPair(algorithm);
            
            long bcSignTime = measureSignature(bcProvider, algorithm, bcKeyPair.getPrivateKey());
            long nativeSignTime = measureSignature(nativeProvider, algorithm, nativeKeyPair.getPrivateKey());
            
            System.out.println("\nSignature Generation (1KB):");
            System.out.println("  BouncyCastle: " + bcSignTime + "ms");
            System.out.println("  Native:       " + nativeSignTime + "ms");
            System.out.println("  Speedup:      " + String.format("%.2fx", (double) bcSignTime / nativeSignTime));

        } else if (bouncyCastleAvailable) {
            System.out.println("Only BouncyCastle provider available - no comparison possible");
        } else {
            System.out.println("Only Native provider available - no comparison possible");
        }

        System.out.println("======================================\n");
    }

    /**
     * Measure average key generation time.
     */
    private long measureKeyGeneration(PQCProvider provider, PQCAlgorithm algorithm) throws Exception {
        long total = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            provider.generateKeyPair(algorithm);
            long end = System.nanoTime();
            total += (end - start);
        }
        return total / BENCHMARK_ITERATIONS / 1_000_000; // Convert to milliseconds
    }

    /**
     * Measure average signature generation time.
     */
    private long measureSignature(PQCProvider provider, PQCAlgorithm algorithm, PrivateKey privateKey) throws Exception {
        long total = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            provider.sign(TEST_DATA_1KB, privateKey, algorithm);
            long end = System.nanoTime();
            total += (end - start);
        }
        return total / BENCHMARK_ITERATIONS / 1_000_000; // Convert to milliseconds
    }

    /**
     * Print performance statistics.
     * 
     * @param operation The operation being benchmarked
     * @param times List of measured times in milliseconds
     * @param target Target performance in milliseconds
     */
    private void printStatistics(String operation, List<Long> times, long target) {
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        long sum = 0;

        for (long time : times) {
            min = Math.min(min, time);
            max = Math.max(max, time);
            sum += time;
        }

        double avg = (double) sum / times.size();
        
        // Calculate standard deviation
        double variance = 0;
        for (long time : times) {
            variance += Math.pow(time - avg, 2);
        }
        double stdDev = Math.sqrt(variance / times.size());

        System.out.println("\n" + operation + ":");
        System.out.println("  Iterations: " + times.size());
        System.out.println("  Min:        " + min + "ms");
        System.out.println("  Max:        " + max + "ms");
        System.out.println("  Average:    " + String.format("%.2f", avg) + "ms");
        System.out.println("  Std Dev:    " + String.format("%.2f", stdDev) + "ms");
        System.out.println("  Target:     " + target + "ms");
        
        if (avg <= target) {
            System.out.println("  Status:     ✓ MEETS TARGET");
        } else {
            System.out.println("  Status:     ✗ EXCEEDS TARGET (+" + String.format("%.2f", avg - target) + "ms)");
        }
    }
}

// Made with Bob
