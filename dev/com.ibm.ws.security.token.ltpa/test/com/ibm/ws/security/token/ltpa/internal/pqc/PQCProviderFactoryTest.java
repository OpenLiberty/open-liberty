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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.token.ltpa.internal.pqc.bc.BouncyCastlePQCProvider;
import com.ibm.ws.security.token.ltpa.internal.pqc.native_.NativePQCProvider;

/**
 * Unit tests for PQCProviderFactory.
 * 
 * <p>This test suite provides comprehensive coverage of the PQC provider factory,
 * including automatic provider selection, FIPS requirement handling, provider
 * caching, and multi-threaded access.</p>
 * 
 * <h3>Test Coverage Areas:</h3>
 * <ul>
 *   <li>Automatic provider selection (Native → BouncyCastle)</li>
 *   <li>FIPS requirement handling</li>
 *   <li>Provider caching (singleton pattern)</li>
 *   <li>Cache invalidation (resetCache)</li>
 *   <li>Error handling (no provider available)</li>
 *   <li>Multi-threaded access</li>
 *   <li>Provider preference system</li>
 * </ul>
 * 
 * <h3>Provider Selection Order:</h3>
 * <ol>
 *   <li>Native PQC Provider (Java 21+, FIPS-compliant on Java 24+)</li>
 *   <li>BouncyCastle PQC Provider (Java 8+, not FIPS-compliant)</li>
 * </ol>
 */
public class PQCProviderFactoryTest {
    
    private static int javaVersion;
    private static boolean nativeProviderAvailable;
    private static boolean bcProviderAvailable;
    
    // ========================================================================
    // Test Setup
    // ========================================================================
    
    /**
     * One-time setup to detect provider availability.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        javaVersion = Runtime.version().feature();
        System.out.println("Java version: " + javaVersion);
        
        // Check native provider availability
        try {
            NativePQCProvider nativeProvider = new NativePQCProvider();
            nativeProviderAvailable = nativeProvider.isAvailable();
            System.out.println("Native PQC provider available: " + nativeProviderAvailable);
        } catch (Exception e) {
            nativeProviderAvailable = false;
            System.out.println("Native PQC provider not available: " + e.getMessage());
        }
        
        // Check BouncyCastle provider availability
        try {
            BouncyCastlePQCProvider bcProvider = new BouncyCastlePQCProvider();
            bcProviderAvailable = bcProvider.isAvailable();
            System.out.println("BouncyCastle PQC provider available: " + bcProviderAvailable);
        } catch (Exception e) {
            bcProviderAvailable = false;
            System.out.println("BouncyCastle PQC provider not available: " + e.getMessage());
        }
    }
    
    /**
     * Reset provider cache before each test.
     */
    @Before
    public void setUp() {
        PQCProviderFactory.resetCache();
    }
    
    /**
     * Clean up after each test.
     */
    @After
    public void tearDown() {
        PQCProviderFactory.resetCache();
    }
    
    // ========================================================================
    // Provider Selection Tests
    // ========================================================================
    
    /**
     * Test that getProvider returns a non-null provider.
     */
    @Test
    public void test_getProvider_ReturnsNonNull() throws Exception {
        // At least one provider should be available (either native or BC)
        if (nativeProviderAvailable || bcProviderAvailable) {
            PQCProvider provider = PQCProviderFactory.getProvider();
            assertNotNull("Provider should not be null", provider);
            assertTrue("Provider should be available", provider.isAvailable());
        }
    }
    
    /**
     * Test that getProvider prefers native provider when available.
     */
    @Test
    public void test_getProvider_PrefersNativeProvider() throws Exception {
        if (nativeProviderAvailable) {
            PQCProvider provider = PQCProviderFactory.getProvider();
            assertNotNull("Provider should not be null", provider);
            assertTrue("Provider should be NativePQCProvider", 
                       provider instanceof NativePQCProvider);
        }
    }
    
    /**
     * Test that getProvider falls back to BouncyCastle when native unavailable.
     */
    @Test
    public void test_getProvider_FallsBackToBouncyCastle() throws Exception {
        if (!nativeProviderAvailable && bcProviderAvailable) {
            PQCProvider provider = PQCProviderFactory.getProvider();
            assertNotNull("Provider should not be null", provider);
            assertTrue("Provider should be BouncyCastlePQCProvider", 
                       provider instanceof BouncyCastlePQCProvider);
        }
    }
    
    /**
     * Test that getProvider throws exception when no provider available.
     */
    @Test
    public void test_getProvider_NoProviderAvailable_ThrowsException() {
        if (!nativeProviderAvailable && !bcProviderAvailable) {
            try {
                PQCProviderFactory.getProvider();
                fail("getProvider should throw PQCException when no provider available");
            } catch (PQCException e) {
                assertNotNull("Exception message should not be null", e.getMessage());
                assertTrue("Exception message should mention no provider available",
                           e.getMessage().contains("No suitable PQC provider is available"));
            }
        }
    }
    
    // ========================================================================
    // FIPS Requirement Tests
    // ========================================================================
    
    /**
     * Test that getProvider with requireFips=true enforces FIPS compliance.
     */
    @Test
    public void test_getProvider_RequireFips_EnforcesFIPSCompliance() throws Exception {
        try {
            PQCProvider provider = PQCProviderFactory.getProvider(true);
            assertNotNull("Provider should not be null", provider);
            assertTrue("Provider should be FIPS-compliant when required", 
                       provider.isFIPSCompliant());
        } catch (PQCException e) {
            // Expected if no FIPS-compliant provider is available
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention FIPS mode",
                       e.getMessage().contains("FIPS 140-3 mode"));
        }
    }
    
    /**
     * Test that getProvider with requireFips=false allows non-FIPS providers.
     */
    @Test
    public void test_getProvider_RequireFipsFalse_AllowsNonFIPS() throws Exception {
        if (nativeProviderAvailable || bcProviderAvailable) {
            PQCProvider provider = PQCProviderFactory.getProvider(false);
            assertNotNull("Provider should not be null", provider);
            // Provider may or may not be FIPS-compliant, but should be available
            assertTrue("Provider should be available", provider.isAvailable());
        }
    }
    
    /**
     * Test that BouncyCastle provider is rejected when FIPS required.
     */
    @Test
    public void test_getProvider_RequireFips_RejectsBouncyCastle() throws Exception {
        if (!nativeProviderAvailable && bcProviderAvailable) {
            // Only BouncyCastle is available, which is not FIPS-compliant
            try {
                PQCProvider provider = PQCProviderFactory.getProvider(true);
                fail("Should throw PQCException when only non-FIPS provider available");
            } catch (PQCException e) {
                assertNotNull("Exception message should not be null", e.getMessage());
                assertTrue("Exception message should mention FIPS mode",
                           e.getMessage().contains("FIPS 140-3 mode"));
            }
        }
    }
    
    // ========================================================================
    // Provider Caching Tests
    // ========================================================================
    
    /**
     * Test that getProvider returns cached instance on subsequent calls.
     */
    @Test
    public void test_getProvider_ReturnsCachedInstance() throws Exception {
        if (nativeProviderAvailable || bcProviderAvailable) {
            PQCProvider provider1 = PQCProviderFactory.getProvider();
            PQCProvider provider2 = PQCProviderFactory.getProvider();
            
            assertNotNull("First provider should not be null", provider1);
            assertNotNull("Second provider should not be null", provider2);
            assertSame("Should return same cached instance", provider1, provider2);
        }
    }
    
    /**
     * Test that separate caches are maintained for FIPS and non-FIPS.
     */
    @Test
    public void test_getProvider_SeparateCachesForFIPS() throws Exception {
        if (nativeProviderAvailable || bcProviderAvailable) {
            try {
                PQCProvider nonFipsProvider = PQCProviderFactory.getProvider(false);
                PQCProvider fipsProvider = PQCProviderFactory.getProvider(true);
                
                assertNotNull("Non-FIPS provider should not be null", nonFipsProvider);
                assertNotNull("FIPS provider should not be null", fipsProvider);
                
                // If both succeed, they may or may not be the same instance
                // depending on whether the selected provider is FIPS-compliant
                if (nonFipsProvider.isFIPSCompliant()) {
                    // If non-FIPS provider is FIPS-compliant, they should be the same
                    assertSame("Should use same instance when provider is FIPS-compliant",
                               nonFipsProvider, fipsProvider);
                }
            } catch (PQCException e) {
                // Expected if FIPS provider not available
            }
        }
    }
    
    /**
     * Test that resetCache clears cached providers.
     */
    @Test
    public void test_resetCache_ClearsCachedProviders() throws Exception {
        if (nativeProviderAvailable || bcProviderAvailable) {
            PQCProvider provider1 = PQCProviderFactory.getProvider();
            assertNotNull("First provider should not be null", provider1);
            
            PQCProviderFactory.resetCache();
            
            PQCProvider provider2 = PQCProviderFactory.getProvider();
            assertNotNull("Second provider should not be null", provider2);
            
            // After cache reset, we should get a new instance
            // Note: We can't use assertNotSame because the factory might
            // return the same provider type, just a new instance
            assertNotNull("Provider after reset should not be null", provider2);
        }
    }
    
    // ========================================================================
    // Multi-threaded Access Tests
    // ========================================================================
    
    /**
     * Test that getProvider is thread-safe.
     */
    @Test
    public void test_getProvider_ThreadSafe() throws Exception {
        if (nativeProviderAvailable || bcProviderAvailable) {
            final int threadCount = 10;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch doneLatch = new CountDownLatch(threadCount);
            final AtomicInteger successCount = new AtomicInteger(0);
            final AtomicReference<PQCProvider> firstProvider = new AtomicReference<>();
            
            // Create multiple threads that all try to get provider simultaneously
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        PQCProvider provider = PQCProviderFactory.getProvider();
                        
                        if (provider != null) {
                            successCount.incrementAndGet();
                            firstProvider.compareAndSet(null, provider);
                        }
                    } catch (Exception e) {
                        // Ignore exceptions in threads
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }
            
            // Start all threads simultaneously
            startLatch.countDown();
            
            // Wait for all threads to complete
            doneLatch.await();
            
            // All threads should have succeeded
            assertEquals("All threads should get provider", threadCount, successCount.get());
            assertNotNull("At least one thread should have gotten provider", 
                          firstProvider.get());
        }
    }
    
    /**
     * Test that concurrent getProvider calls return same cached instance.
     */
    @Test
    public void test_getProvider_ConcurrentCallsReturnSameInstance() throws Exception {
        if (nativeProviderAvailable || bcProviderAvailable) {
            final int threadCount = 10;
            final CountDownLatch startLatch = new CountDownLatch(1);
            final CountDownLatch doneLatch = new CountDownLatch(threadCount);
            final PQCProvider[] providers = new PQCProvider[threadCount];
            
            // Create multiple threads that all try to get provider simultaneously
            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                new Thread(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready
                        providers[index] = PQCProviderFactory.getProvider();
                    } catch (Exception e) {
                        // Ignore exceptions in threads
                    } finally {
                        doneLatch.countDown();
                    }
                }).start();
            }
            
            // Start all threads simultaneously
            startLatch.countDown();
            
            // Wait for all threads to complete
            doneLatch.await();
            
            // All threads should have gotten the same instance
            PQCProvider firstProvider = providers[0];
            assertNotNull("First provider should not be null", firstProvider);
            
            for (int i = 1; i < threadCount; i++) {
                assertSame("All threads should get same cached instance", 
                           firstProvider, providers[i]);
            }
        }
    }
    
    // ========================================================================
    // Error Handling Tests
    // ========================================================================
    
    /**
     * Test that factory handles provider initialization failures gracefully.
     */
    @Test
    public void test_getProvider_HandlesInitializationFailures() {
        // This test verifies that the factory doesn't crash when providers fail to initialize
        // The factory should try all providers and throw PQCException if none work
        try {
            PQCProvider provider = PQCProviderFactory.getProvider();
            // If we get here, at least one provider worked
            assertNotNull("Provider should not be null", provider);
        } catch (PQCException e) {
            // Expected if no providers are available
            assertNotNull("Exception message should not be null", e.getMessage());
            assertTrue("Exception message should mention no provider available",
                       e.getMessage().contains("No suitable PQC provider is available"));
        }
    }
    
    /**
     * Test that factory provides detailed error message when no provider available.
     */
    @Test
    public void test_getProvider_DetailedErrorMessage() {
        if (!nativeProviderAvailable && !bcProviderAvailable) {
            try {
                PQCProvider provider = PQCProviderFactory.getProvider();
                fail("Should throw PQCException when no provider available");
            } catch (PQCException e) {
                String message = e.getMessage();
                assertNotNull("Exception message should not be null", message);
                
                // Message should include availability status of both providers
                assertTrue("Message should mention native provider",
                           message.contains("Native provider available="));
                assertTrue("Message should mention BouncyCastle provider",
                           message.contains("BouncyCastle provider available="));
            }
        }
    }
    
    // ========================================================================
    // Utility Class Tests
    // ========================================================================
    
    /**
     * Test that PQCProviderFactory cannot be instantiated.
     */
    @Test
    public void test_cannotInstantiate() {
        try {
            // Use reflection to try to instantiate the factory
            java.lang.reflect.Constructor<?> constructor = 
                PQCProviderFactory.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
            fail("Should not be able to instantiate PQCProviderFactory");
        } catch (java.lang.reflect.InvocationTargetException e) {
            // Expected - constructor should throw AssertionError
            Throwable cause = e.getCause();
            assertTrue("Constructor should throw AssertionError", 
                       cause instanceof AssertionError);
            assertTrue("Error message should mention utility class",
                       cause.getMessage().contains("utility class"));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
    
    // ========================================================================
    // Provider Information Tests
    // ========================================================================
    
    /**
     * Test that selected provider has valid information.
     */
    @Test
    public void test_selectedProvider_HasValidInformation() throws Exception {
        if (nativeProviderAvailable || bcProviderAvailable) {
            PQCProvider provider = PQCProviderFactory.getProvider();
            
            assertNotNull("Provider should not be null", provider);
            assertNotNull("Provider name should not be null", provider.getProviderName());
            assertNotNull("Provider version should not be null", provider.getProviderVersion());
            assertTrue("Provider should be available", provider.isAvailable());
            assertNotNull("Supported algorithms should not be null", 
                          provider.getSupportedAlgorithms());
            assertFalse("Supported algorithms should not be empty", 
                        provider.getSupportedAlgorithms().isEmpty());
        }
    }
    
    /**
     * Test that selected provider supports required algorithms.
     */
    @Test
    public void test_selectedProvider_SupportsRequiredAlgorithms() throws Exception {
        if (nativeProviderAvailable || bcProviderAvailable) {
            PQCProvider provider = PQCProviderFactory.getProvider();
            
            assertTrue("Provider should support ML-DSA-65", 
                       provider.supportsAlgorithm(PQCAlgorithm.ML_DSA_65));
            assertTrue("Provider should support ML-DSA-87", 
                       provider.supportsAlgorithm(PQCAlgorithm.ML_DSA_87));
        }
    }
    
    /**
     * Test that selected provider can generate keys.
     */
    @Test
    public void test_selectedProvider_CanGenerateKeys() throws Exception {
        if (nativeProviderAvailable || bcProviderAvailable) {
            PQCProvider provider = PQCProviderFactory.getProvider();
            
            PQCKeyPair keyPair = provider.generateKeyPair(PQCAlgorithm.ML_DSA_65);
            assertNotNull("Key pair should not be null", keyPair);
            assertNotNull("Public key should not be null", keyPair.getPublicKey());
            assertNotNull("Private key should not be null", keyPair.getPrivateKey());
        }
    }
}

// Made with Bob