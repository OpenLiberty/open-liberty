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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.security.token.ltpa.internal.pqc.bc.BouncyCastlePQCProvider;
import com.ibm.ws.security.token.ltpa.internal.pqc.native_.NativePQCProvider;

/**
 * Factory for obtaining the best available {@link PQCProvider} implementation for the
 * current runtime environment.
 *
 * <p>This class centralizes provider selection for PQC-LTPA operations and applies the
 * following selection order:</p>
 * <ol>
 *   <li>Evaluate whether FIPS 140-3 mode is required</li>
 *   <li>Prefer {@link NativePQCProvider} when available (Java 21+)</li>
 *   <li>Fall back to {@link BouncyCastlePQCProvider} when native support is unavailable</li>
 *   <li>Fail with {@link PQCException} when no suitable provider is available</li>
 * </ol>
 *
 * <p>Provider instances are lazily initialized and cached using thread-safe double-checked
 * locking. Separate cache entries are maintained for general selection and FIPS-required
 * selection to avoid repeated environment probing.</p>
 *
 * <p>This is a utility class and cannot be instantiated.</p>
 */
public final class PQCProviderFactory {

    private static final TraceComponent tc = Tr.register(PQCProviderFactory.class);

    private static final String SEMERU_FIPS_PROPERTY = "semeru.fips";
    private static final String IBM_FIPS_MODE_PROPERTY = "com.ibm.fips.mode";
    private static final String IBM_FIPS_140_3_VALUE = "140-3";

    /**
     * Cached provider for normal selection.
     */
    private static volatile PQCProvider cachedProvider;

    /**
     * Cached provider for explicit FIPS-required selection.
     */
    private static volatile PQCProvider cachedFipsProvider;

    /**
     * Lock used for lazy initialization.
     */
    private static final Object LOCK = new Object();

    /**
     * Prevent instantiation.
     */
    private PQCProviderFactory() {
        throw new AssertionError("PQCProviderFactory is a utility class and cannot be instantiated");
    }

    /**
     * Returns the best available PQC provider for the current runtime.
     *
     * <p>If the runtime is detected to be operating in FIPS 140-3 mode, the returned
     * provider must be FIPS-compliant. Otherwise, the best available provider is
     * selected using the standard preference order.</p>
     *
     * @return the selected PQC provider
     * @throws PQCException if no suitable provider is available
     */
    public static PQCProvider getProvider() throws PQCException {
        return getProvider(false);
    }

    /**
     * Returns the best available PQC provider, optionally requiring FIPS compliance.
     *
     * <p>When {@code requireFips} is {@code true}, only FIPS-compliant providers are
     * considered valid. When {@code requireFips} is {@code false}, runtime FIPS detection
     * is still honored, meaning a FIPS-enabled runtime will still require a FIPS-compliant
     * provider.</p>
     *
     * @param requireFips true if a FIPS-compliant provider is required
     * @return the selected PQC provider
     * @throws PQCException if no suitable provider is available
     */
    public static PQCProvider getProvider(boolean requireFips) throws PQCException {
        boolean effectiveRequireFips = requireFips || isFipsRequired();
        PQCProvider provider = effectiveRequireFips ? cachedFipsProvider : cachedProvider;

        if (provider == null) {
            synchronized (LOCK) {
                provider = effectiveRequireFips ? cachedFipsProvider : cachedProvider;
                if (provider == null) {
                    provider = selectProvider(effectiveRequireFips);
                    if (effectiveRequireFips) {
                        cachedFipsProvider = provider;
                    } else {
                        cachedProvider = provider;
                    }
                }
            }
        }

        return provider;
    }

    /**
     * Clears all cached provider instances.
     *
     * <p>This method is intended for test scenarios that need to force re-evaluation of
     * provider availability or FIPS mode detection.</p>
     */
    public static void resetCache() {
        synchronized (LOCK) {
            cachedProvider = null;
            cachedFipsProvider = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
            Tr.info(tc, "PQCProviderFactory provider cache reset");
        }
    }

    /**
     * Selects the most appropriate provider for the runtime.
     *
     * @param requireFips true if a FIPS-compliant provider is required
     * @return the selected provider
     * @throws PQCException if no suitable provider is available
     */
    private static PQCProvider selectProvider(boolean requireFips) throws PQCException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Selecting PQC provider. requireFips=" + requireFips);
        }

        PQCProvider nativeProvider = createNativeProvider();
        if (isUsable(nativeProvider, requireFips)) {
            logSelection(nativeProvider, requireFips);
            return nativeProvider;
        }

        PQCProvider bcProvider = createBouncyCastleProvider();
        if (isUsable(bcProvider, requireFips)) {
            logSelection(bcProvider, requireFips);
            return bcProvider;
        }

        StringBuilder message = new StringBuilder("No suitable PQC provider is available");
        if (requireFips) {
            message.append(" for FIPS 140-3 mode");
        }
        message.append(". Native provider available=")
               .append(nativeProvider != null && nativeProvider.isAvailable())
               .append(", native FIPS-compliant=")
               .append(nativeProvider != null && nativeProvider.isFIPSCompliant())
               .append(", BouncyCastle provider available=")
               .append(bcProvider != null && bcProvider.isAvailable())
               .append(", BouncyCastle FIPS-compliant=")
               .append(bcProvider != null && bcProvider.isFIPSCompliant());

        throw new PQCException(message.toString());
    }

    /**
     * Creates a native provider instance.
     *
     * @return the native provider instance, or null if construction failed unexpectedly
     */
    private static PQCProvider createNativeProvider() {
        try {
            PQCProvider provider = new NativePQCProvider();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Native PQC provider probed. available=" + provider.isAvailable()
                             + ", fips=" + provider.isFIPSCompliant()
                             + ", provider=" + provider.getProviderName()
                             + ", version=" + provider.getProviderVersion());
            }

            return provider;
        } catch (RuntimeException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Native PQC provider initialization failed unexpectedly: " + e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * Creates a BouncyCastle PQC provider instance.
     *
     * @return the BouncyCastle provider instance, or null if construction failed unexpectedly
     */
    private static PQCProvider createBouncyCastleProvider() {
        try {
            PQCProvider provider = new BouncyCastlePQCProvider();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "BouncyCastle PQC provider probed. available=" + provider.isAvailable()
                             + ", fips=" + provider.isFIPSCompliant()
                             + ", provider=" + provider.getProviderName()
                             + ", version=" + provider.getProviderVersion());
            }

            return provider;
        } catch (RuntimeException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "BouncyCastle PQC provider initialization failed unexpectedly: " + e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * Returns whether the provider is usable for the given selection requirements.
     *
     * @param provider the provider to evaluate
     * @param requireFips true if FIPS compliance is required
     * @return true if the provider can be used
     */
    private static boolean isUsable(PQCProvider provider, boolean requireFips) {
        if (provider == null) {
            return false;
        }

        if (!provider.isAvailable()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Provider rejected because it is unavailable: " + provider.getClass().getName());
            }
            return false;
        }

        if (requireFips && !provider.isFIPSCompliant()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Provider rejected because it is not FIPS-compliant: "
                             + provider.getProviderName());
            }
            return false;
        }

        return true;
    }

    /**
     * Determines whether FIPS mode is required by the current runtime.
     *
     * <p>Detection order:</p>
     * <ol>
     *   <li>System property {@code semeru.fips=true}</li>
     *   <li>System property {@code com.ibm.fips.mode=140-3}</li>
     *   <li>{@link CryptoUtils#isFips140_3Enabled()}</li>
     * </ol>
     *
     * @return true if FIPS mode is required
     */
    private static boolean isFipsRequired() {
        try {
            String semeruFips = System.getProperty(SEMERU_FIPS_PROPERTY);
            if ("true".equalsIgnoreCase(semeruFips)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "FIPS mode detected from system property: " + SEMERU_FIPS_PROPERTY + "=true");
                }
                return true;
            }

            String ibmFipsMode = System.getProperty(IBM_FIPS_MODE_PROPERTY);
            if (IBM_FIPS_140_3_VALUE.equals(ibmFipsMode)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "FIPS mode detected from system property: "
                                 + IBM_FIPS_MODE_PROPERTY + "=" + IBM_FIPS_140_3_VALUE);
                }
                return true;
            }
        } catch (SecurityException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to inspect FIPS system properties due to SecurityException: " + e.getMessage(), e);
            }
        }

        try {
            if (CryptoUtils.isFips140_3Enabled()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "FIPS mode detected from CryptoUtils.isFips140_3Enabled()");
                }
                return true;
            }
        } catch (RuntimeException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "CryptoUtils FIPS detection failed unexpectedly: " + e.getMessage(), e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "FIPS mode not detected");
        }
        return false;
    }

    /**
     * Logs the selected provider at INFO level.
     *
     * @param provider the selected provider
     * @param requireFips true if selection required FIPS compliance
     */
    private static void logSelection(PQCProvider provider, boolean requireFips) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
            Tr.info(tc, "Selected PQC provider: " + provider.getProviderName()
                        + ", version=" + provider.getProviderVersion()
                        + ", fipsCompliant=" + provider.isFIPSCompliant()
                        + ", requireFips=" + requireFips);
        }
    }
}

// Made with Bob
