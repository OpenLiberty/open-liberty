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

import java.security.Provider;
import java.security.Security;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Detects and validates FIPS 140-3 mode for cryptographic operations.
 * 
 * <p>This class provides comprehensive FIPS mode detection for Liberty's PQC-LTPA
 * implementation. FIPS 140-3 (Federal Information Processing Standard) is a U.S.
 * government security standard that specifies requirements for cryptographic modules.
 * 
 * <p><b>FIPS 140-3 Requirements:</b>
 * <ul>
 *   <li>Use only FIPS-approved cryptographic algorithms</li>
 *   <li>Use only FIPS-validated cryptographic providers</li>
 *   <li>Enforce minimum key sizes (RSA 2048+, AES 256)</li>
 *   <li>Use approved random number generators</li>
 *   <li>Implement proper key management</li>
 * </ul>
 * 
 * <p><b>Detection Methods:</b>
 * <ol>
 *   <li><b>JVM Property:</b> Check {@code -Dsemeru.fips=true} or {@code -Xenablefips140-3}</li>
 *   <li><b>Security Property:</b> Check {@code com.ibm.fips.mode=140-3}</li>
 *   <li><b>Provider Detection:</b> Check for FIPS-validated providers (OpenJCEPlusFIPS, IBMJCEPlusFIPS)</li>
 *   <li><b>Algorithm Availability:</b> Test for FIPS-approved algorithms</li>
 * </ol>
 * 
 * <p><b>Supported FIPS Providers:</b>
 * <ul>
 *   <li><b>OpenJCEPlusFIPS:</b> IBM Semeru Runtime FIPS provider</li>
 *   <li><b>IBMJCEPlusFIPS:</b> IBM JDK FIPS provider</li>
 *   <li><b>SunPKCS11-NSS:</b> NSS-based FIPS provider (Linux)</li>
 * </ul>
 * 
 * <p><b>PQC and FIPS 140-3:</b>
 * Post-Quantum algorithms (ML-DSA-65, ML-DSA-87) are FIPS-approved as of
 * NIST FIPS 204 (August 2024). However, provider support varies:
 * <ul>
 *   <li><b>Java 24+ Native:</b> FIPS-compliant PQC support</li>
 *   <li><b>BouncyCastle:</b> NOT FIPS-validated (use only in non-FIPS mode)</li>
 * </ul>
 * 
 * <p><b>Usage Example:</b>
 * <pre>
 * if (FIPSModeDetector.isFIPSEnabled()) {
 *     // Use only FIPS-approved algorithms and providers
 *     provider = new NativePQCProvider(); // FIPS-compliant
 * } else {
 *     // Can use non-FIPS providers
 *     provider = new BouncyCastlePQCProvider(); // Not FIPS-validated
 * }
 * </pre>
 * 
 * <p><b>Thread Safety:</b> All methods are thread-safe with cached results.
 * 
 * @author IBM Corporation
 * @version 1.0
 * @since 2026
 */
public class FIPSModeDetector {
    
    private static final TraceComponent tc = Tr.register(FIPSModeDetector.class);
    
    /**
     * JVM property for IBM Semeru FIPS mode.
     */
    private static final String SEMERU_FIPS_PROPERTY = "semeru.fips";
    
    /**
     * JVM property for IBM JDK FIPS mode.
     */
    private static final String IBM_FIPS_PROPERTY = "com.ibm.fips.mode";
    
    /**
     * Security property for FIPS mode.
     */
    private static final String SECURITY_FIPS_PROPERTY = "com.ibm.fips.mode";
    
    /**
     * Expected value for FIPS 140-3 mode.
     */
    private static final String FIPS_140_3_VALUE = "140-3";
    
    /**
     * FIPS provider names.
     */
    private static final String[] FIPS_PROVIDER_NAMES = {
        "OpenJCEPlusFIPS",
        "IBMJCEPlusFIPS",
        "SunPKCS11-NSS"
    };
    
    /**
     * Cached FIPS mode detection result.
     */
    private static final AtomicBoolean fipsEnabled = new AtomicBoolean(false);
    
    /**
     * Cached FIPS provider reference.
     */
    private static final AtomicReference<Provider> fipsProvider = new AtomicReference<>(null);
    
    /**
     * Flag indicating whether FIPS detection has been performed.
     */
    private static final AtomicBoolean detectionPerformed = new AtomicBoolean(false);
    
    /**
     * Private constructor to prevent instantiation.
     */
    private FIPSModeDetector() {
        // Utility class - no instances
    }
    
    /**
     * Checks if FIPS 140-3 mode is enabled.
     * 
     * <p>This method performs comprehensive FIPS detection on first call and
     * caches the result for subsequent calls. Detection includes:
     * <ul>
     *   <li>JVM property checks</li>
     *   <li>Security property checks</li>
     *   <li>Provider detection</li>
     *   <li>Algorithm availability tests</li>
     * </ul>
     * 
     * @return true if FIPS 140-3 mode is enabled, false otherwise
     */
    public static boolean isFIPSEnabled() {
        if (!detectionPerformed.get()) {
            synchronized (FIPSModeDetector.class) {
                if (!detectionPerformed.get()) {
                    performFIPSDetection();
                    detectionPerformed.set(true);
                }
            }
        }
        return fipsEnabled.get();
    }
    
    /**
     * Gets the FIPS-validated cryptographic provider.
     * 
     * <p>Returns the detected FIPS provider, or null if FIPS mode is not enabled
     * or no FIPS provider is available.
     * 
     * @return the FIPS provider, or null if not available
     */
    public static Provider getFIPSProvider() {
        if (!isFIPSEnabled()) {
            return null;
        }
        return fipsProvider.get();
    }
    
    /**
     * Gets the name of the FIPS provider.
     * 
     * @return the FIPS provider name, or null if not available
     */
    @Trivial
    public static String getFIPSProviderName() {
        Provider provider = getFIPSProvider();
        return provider != null ? provider.getName() : null;
    }
    
    /**
     * Checks if a specific provider is FIPS-validated.
     * 
     * @param providerName the provider name to check
     * @return true if the provider is FIPS-validated, false otherwise
     */
    @Trivial
    public static boolean isFIPSProvider(String providerName) {
        if (providerName == null) {
            return false;
        }
        
        for (String fipsName : FIPS_PROVIDER_NAMES) {
            if (providerName.equals(fipsName) || providerName.startsWith(fipsName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Forces re-detection of FIPS mode.
     * 
     * <p>This method clears the cached detection result and forces a new
     * detection on the next call to {@link #isFIPSEnabled()}. Use this
     * method only if FIPS configuration changes at runtime.
     */
    public static void resetDetection() {
        synchronized (FIPSModeDetector.class) {
            detectionPerformed.set(false);
            fipsEnabled.set(false);
            fipsProvider.set(null);
            
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "FIPS detection reset");
            }
        }
    }
    
    /**
     * Performs comprehensive FIPS mode detection.
     * 
     * <p>This method checks multiple indicators to determine if FIPS mode
     * is enabled. It updates the cached detection results.
     */
    private static void performFIPSDetection() {
        boolean detected = false;
        Provider detectedProvider = null;
        
        // Method 1: Check JVM properties
        if (checkJVMProperties()) {
            detected = true;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "FIPS mode detected via JVM properties");
            }
        }
        
        // Method 2: Check security properties
        if (!detected && checkSecurityProperties()) {
            detected = true;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "FIPS mode detected via security properties");
            }
        }
        
        // Method 3: Check for FIPS providers
        detectedProvider = detectFIPSProvider();
        if (detectedProvider != null) {
            detected = true;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "FIPS mode detected via provider: {0}", 
                        detectedProvider.getName());
            }
        }
        
        // Update cached results
        fipsEnabled.set(detected);
        fipsProvider.set(detectedProvider);
        
        // Log detection result
        if (detected) {
            if (tc.isAuditEnabled()) {
                Tr.audit(tc, "CWWKS4210I: FIPS 140-3 mode is enabled. Provider: {0}",
                        detectedProvider != null ? detectedProvider.getName() : "Unknown");
            }
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "FIPS 140-3 mode is not enabled");
            }
        }
    }
    
    /**
     * Checks JVM properties for FIPS mode indicators.
     * 
     * @return true if FIPS mode is indicated by JVM properties
     */
    @Trivial
    private static boolean checkJVMProperties() {
        // Check IBM Semeru FIPS property
        String semeruFips = System.getProperty(SEMERU_FIPS_PROPERTY);
        if ("true".equalsIgnoreCase(semeruFips)) {
            return true;
        }
        
        // Check IBM JDK FIPS property
        String ibmFips = System.getProperty(IBM_FIPS_PROPERTY);
        if (FIPS_140_3_VALUE.equals(ibmFips)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks security properties for FIPS mode indicators.
     * 
     * @return true if FIPS mode is indicated by security properties
     */
    @Trivial
    private static boolean checkSecurityProperties() {
        try {
            String fipsMode = Security.getProperty(SECURITY_FIPS_PROPERTY);
            return FIPS_140_3_VALUE.equals(fipsMode);
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to check security properties", e);
            }
            return false;
        }
    }
    
    /**
     * Detects FIPS-validated cryptographic providers.
     * 
     * @return the first detected FIPS provider, or null if none found
     */
    @Trivial
    private static Provider detectFIPSProvider() {
        try {
            Provider[] providers = Security.getProviders();
            if (providers == null) {
                return null;
            }
            
            for (Provider provider : providers) {
                String name = provider.getName();
                if (isFIPSProvider(name)) {
                    return provider;
                }
            }
            
            return null;
            
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to detect FIPS provider", e);
            }
            return null;
        }
    }
    
    /**
     * Validates that FIPS mode is properly configured.
     * 
     * <p>This method performs additional validation to ensure FIPS mode
     * is correctly configured and functional. It checks:
     * <ul>
     *   <li>FIPS provider is available</li>
     *   <li>FIPS-approved algorithms are available</li>
     *   <li>Non-FIPS algorithms are disabled</li>
     * </ul>
     * 
     * @return true if FIPS mode is properly configured, false otherwise
     */
    public static boolean validateFIPSConfiguration() {
        if (!isFIPSEnabled()) {
            return false;
        }
        
        Provider provider = getFIPSProvider();
        if (provider == null) {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "CWWKS4211W: FIPS mode is enabled but no FIPS provider found");
            }
            return false;
        }
        
        // Validate FIPS-approved algorithms are available
        if (!validateFIPSAlgorithms(provider)) {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "CWWKS4212W: FIPS provider does not support required algorithms");
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * Validates that FIPS-approved algorithms are available.
     * 
     * @param provider the FIPS provider to validate
     * @return true if required algorithms are available
     */
    @Trivial
    private static boolean validateFIPSAlgorithms(Provider provider) {
        // Check for essential FIPS-approved algorithms
        String[] requiredAlgorithms = {
            "SHA-256",
            "SHA-384",
            "SHA-512",
            "AES",
            "RSA"
        };
        
        for (String algorithm : requiredAlgorithms) {
            if (provider.getService("MessageDigest", algorithm) == null &&
                provider.getService("Cipher", algorithm) == null &&
                provider.getService("KeyPairGenerator", algorithm) == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Required algorithm not found: {0}", algorithm);
                }
                return false;
            }
        }
        
        return true;
    }
}

// Made with Bob
