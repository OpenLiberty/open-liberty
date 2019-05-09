/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature;

import java.util.concurrent.Callable;

import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.BundleException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * An enumeration of supported API region types. Each API region
 * is associated with the following three things:
 * <ul>
 * <li>The API type</li>
 * <li>The Region name</li>
 * <li>If the region has access (delegates to) the internal region</li>
 * </ul>
 * <p>
 * API regions are used to grant applications access to API packages as defined
 * by liberty features. The {@link #THREAD_CONTEXT} type is used to grant the
 * thread context class loaders access to packages exported with the matching
 * attribute thread-context=true.
 */
public enum ApiRegion {
    /**
     * User defined API, delegates to internal
     */
    USER("api", "liberty.user.defined.api", true),
    /**
     * IBM API, delegates to internal
     */
    IBM("ibm-api", "liberty.ibm.api", true),
    /**
     * Specification API, delegates to internal
     */
    SPEC("spec", "liberty.spec.api", true),
    /**
     * Third party API, delegates to internal
     */
    THIRD_PARTY("third-party", "liberty.third.party.api", true),
    /**
     * Stable API, delegates to internal
     */
    STABLE("stable", "liberty.stable.api", true),
    /**
     * Internal API, does not delegate to internal because it IS internal
     */
    INTERNAL("internal", "liberty.internal.api", false),
    /**
     * Used by OSGi Apps to get access to all API, does not delegate to internal
     * because internal is already included in the region
     */
    ALL("all", "liberty.all.api", false),

    /**
     * Used by thread context gateway bundles
     */
    THREAD_CONTEXT("thread-context", "liberty.thread.context.api", false);

    private static final TraceComponent tc = Tr.register(ApiRegion.class);
    private static final ApiRegion[] values = ApiRegion.values();
    private final String apiType;
    private final String regionName;
    private final boolean delegateInternal;

    private ApiRegion(String apiType, String regionName, boolean delegateInternal) {
        this.apiType = apiType;
        this.regionName = regionName;
        this.delegateInternal = delegateInternal;
    }

    /**
     * Returns the api type granted by the region
     * 
     * @return the api type
     */
    public String getApiType() {
        return apiType;
    }

    /**
     * Returns the name of the region that grants access to the
     * api type.
     * 
     * @return the region name
     */
    public String getRegionName() {
        return regionName;
    }

    /**
     * Returns true if the region delegates to the internal region
     * to grant access to internal packages
     * 
     * @return true if delegation to the internal region.
     */
    public boolean delegateInternal() {
        return delegateInternal;
    }

    /**
     * Returns the APIRegion type for the specified API type
     * 
     * @param apiType the API type to get the region for
     * @return the APIRegion type for the specified API type
     * @throws IllegalArgumentException if the apiType is not valid
     */
    public static ApiRegion valueFromApiType(String apiType) {

        for (ApiRegion apiRegion : values) {
            if (apiRegion.apiType.equals(apiType)) {
                return apiRegion;
            }
        }
        throw new IllegalArgumentException(apiType);
    }

    /**
     * Updates the specified region digraph to the region
     * digraph returned by the specified callable. If a
     * conflict is found while trying to apply the updated
     * digraph then multiple calls to the specified callable
     * will be done until a successful replace has been done
     * or the limit on retries has been reached.
     * <p>
     * The specified callable must return a new region digraph
     * which was created by calling {@link RegionDigraph#copy()} on the
     * specified region digraph. A {@code null} value
     * may be returned by the callable if no update is needed.
     * 
     * @param digraph The region digraph to update. Also the
     *            region digraph used to make a copy to be returned by
     *            the specified updateTo callable.
     * @param updateTo A callable that returns an updated copy of
     *            the specified digraph. The returned digraph will be used to
     *            call {@link RegionDigraph#replace(RegionDigraph)} on the
     *            specified digraph.
     * @throws BundleException if the update could not complete or if the
     *             callable throws a BundleException it is rethrown.
     * @throws RuntimeException if the callable throws any exception other
     *             than a BundleException.
     */
    @FFDCIgnore({ BundleException.class, InterruptedException.class })
    public static void update(RegionDigraph digraph, Callable<RegionDigraph> updateTo) throws BundleException {
        // Region digraph does optimistic updates which requires us to retry on error.
        // Only try 10 times then fail.
        int numRetries = 10;
        for (int i = 0; i < numRetries; i++) {
            RegionDigraph copy;
            try {
                copy = updateTo.call();
            } catch (BundleException e) {
                // Just rethrow the original
                throw e;
            } catch (RuntimeException e) {
                // Just rethrow the original
                throw e;
            } catch (Exception e) {
                // not expected, throw runtime exception
                throw new RuntimeException(e);
            }
            try {
                if (copy != null) {
                    digraph.replace(copy);
                }
                // success!
                return;
            } catch (BundleException e) {
                if (i == numRetries - 1) {
                    throw e;
                }
                // Some other thread may be thrashing the system
                // We sleep here just to give some time before we try again
                try {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "RegionDigraph replace contention: Delay");
                    }
                    Thread.sleep(200);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "RegionDigraph replace contention: Continue");
                    }
                } catch (InterruptedException ie) {
                    Thread.interrupted();
                    throw e;
                }
            }
        }
    }
}
