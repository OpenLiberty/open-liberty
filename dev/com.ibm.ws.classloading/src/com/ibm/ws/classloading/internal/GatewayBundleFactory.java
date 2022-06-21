/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.dynamic.bundle.BundleFactory;
import com.ibm.ws.dynamic.bundle.DynamicBundleException;
import com.ibm.ws.kernel.feature.ApiRegion;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoadingServiceException;
import com.ibm.wsspi.classloading.GatewayConfiguration;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * This class uses OSGi semantics to define a bundle on the fly that has clearly defined access to
 * specific other bundles within the OSGi framework.
 * 
 */
class GatewayBundleFactory {
    private static final TraceComponent tc = Tr.register(GatewayBundleFactory.class);
    private static final String BUNDLE_LOCATION_PREFIX = "WSClassLoadingService@";
    private static final String REGION_PREFIX = "liberty.gateway";
    private static final String REGION_POSTFIX = ".hub";
    private static final String REGION_PRODUCT_HUB = "liberty.product.api.spi.hub";
    //package visibility for the hook factory..
    static final String MANIFEST_GATEWAY_ALLOWEDTYPES_PROPERTY_KEY = "IBM-ApiTypeVisibility";
    static final String GATEWAY_BUNDLE_MARKER = "IBM-GatewayBundle";
    protected final BundleContext bundleContext;
    private final FrameworkWiring frameworkWiring;
    private final RegionDigraph digraph;
    final Map<Bundle, Set<GatewayClassLoader>> classloaders;

    GatewayBundleFactory(BundleContext bundleContext, RegionDigraph digraph, Map<Bundle, Set<GatewayClassLoader>> classloaders) {
        this.bundleContext = bundleContext;
        this.frameworkWiring = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
        this.digraph = digraph;
        this.classloaders = classloaders;
    }

    private void setStartLevel(Bundle b) {
        FrameworkStartLevel fsl = frameworkWiring.getBundle().adapt(FrameworkStartLevel.class);
        BundleStartLevel bsl = b.adapt(BundleStartLevel.class);
        int currentStartLevel = fsl.getStartLevel();
        int neededStartLevel = bsl.getStartLevel();
        if (neededStartLevel > currentStartLevel) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "Changing the start level of bundle {0} from {1} to the current level of {2}", b, neededStartLevel, currentStartLevel);
            bsl.setStartLevel(currentStartLevel);
        }
    }

    GatewayClassLoader createGatewayBundleClassLoader(GatewayConfiguration gwConfig, ClassLoaderConfiguration clConfig, CompositeResourceProvider resourceProviders) {
        // Needs syncToOSThread protection.  Problems were encountered
        // while creating the shared-library for a lazy-activated JDBC bundle.
        Object token = ThreadIdentityManager.runAsServer();
        try {
            Bundle b = createGatewayBundle(gwConfig, clConfig);
            setStartLevel(b);
            try {
                start(b);
            } catch (BundleException e) {
                // Failed to resolve gateway bundle;
                throw new ClassLoadingServiceException(
                                Tr.formatMessage(tc, "cls.gateway.not.resolvable", gwConfig.getApplicationName(), gwConfig.getApplicationVersion()),
                                e);

            }
            BundleWiring bw = b.adapt(BundleWiring.class);
            ClassLoader bundleLoader = null;
            if (bw == null) {
                // The BundleWiring can be null if the bundle is no longer resolved

                // The most likely cause of this is that the server is shutting down. If that's the case, 
                // create a GatewayClassLoader using a null bundleLoader. This allows us to avoid logging FFDCs
                // and dereferencing a null classloader upstream. 
                //
                // If it's something else weird, throw an exception
                if (!FrameworkState.isStopping())
                    throw new ClassLoadingServiceException(
                                    Tr.formatMessage(tc, "cls.gateway.not.resolvable", gwConfig.getApplicationName(), gwConfig.getApplicationVersion()));
            } else {
                bundleLoader = bw.getClassLoader();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "The state of started bundle {0} is {1}", b, b.getState());
            }
            return GatewayClassLoader.createGatewayClassLoader(classloaders, gwConfig, bundleLoader, resourceProviders);
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    private void start(Bundle b) throws BundleException {
        BundleException resolverException = null;
        for (int i = 0; i < 2; i++) {
            resolverException = null;
            try {
                b.start(Bundle.START_ACTIVATION_POLICY);
                return;
            } catch (BundleException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "An exception occurred while starting bundle {0}: {1}", b, e);
                }
                if (e.getType() == BundleException.RESOLVE_ERROR) {
                    // Failed to resolve; 
                    // typically the bundle exception message will have some useful resolver error information
                    resolverException = e;
                }
            }
        }
        if (resolverException != null) {
            throw resolverException;
        }
    }

    private Bundle createGatewayBundle(GatewayConfiguration gwConfig, ClassLoaderConfiguration clConfig) {
        return new BundleFactory()
                        .setBundleName(getAppName(gwConfig, clConfig))
                        .setBundleVersion(getAppVersion(gwConfig))
                        .setBundleSymbolicName(getSymbolicName(clConfig))
                        .importPackages(gwConfig.getImportPackage())
                        .requireBundles(gwConfig.getRequireBundle())
                        .dynamicallyImportPackages(gwConfig.getDynamicImportPackage())
                        .addAttributeValues(GATEWAY_BUNDLE_MARKER, "true")
                        .addManifestAttribute(MANIFEST_GATEWAY_ALLOWEDTYPES_PROPERTY_KEY, gwConfig.getApiTypeVisibility())
                        .setBundleLocationPrefix(BUNDLE_LOCATION_PREFIX)
                        .setBundleLocation(clConfig.getId().toString())
                        .setBundleContext(bundleContext)
                        .setRegion(getRegion(gwConfig.getApiTypeVisibility()))
                        .setLazyActivation(true)
                        .createBundle();
    }

    private Region getRegion(EnumSet<ApiType> apiTypeVisibility) {
        if (digraph == null) {
            // this is for testing purposes
            return null;
        }
        if (apiTypeVisibility == null) {
            return digraph.getRegion(ApiRegion.THREAD_CONTEXT.getRegionName());
        }
        if (apiTypeVisibility.isEmpty()) {
            return digraph.getRegion(ApiRegion.INTERNAL.getRegionName());
        }
        StringBuilder regionName = new StringBuilder(REGION_PREFIX);
        for (ApiType apiType : apiTypeVisibility) {
            regionName.append('.').append(apiType.toString());
        }
        regionName.append(REGION_POSTFIX);
        Region region = digraph.getRegion(regionName.toString());
        if (region == null) {
            return createRegion(apiTypeVisibility, regionName.toString());
        }
        return region;
    }

    private Region createRegion(final EnumSet<ApiType> apiTypeVisibility, final String regionName) {
        try {
            ApiRegion.update(digraph, new Callable<RegionDigraph>() {
                @Override
                public RegionDigraph call() throws BundleException {
                    RegionDigraph copy;
                    try {
                        copy = digraph.copy();
                        Region region = copy.getRegion(regionName.toString());
                        if (region != null) {
                            // Another thread won before we got the copy
                            return null;
                        }
                        region = copy.createRegion(regionName);
                        connectToApiRegions(region, apiTypeVisibility, copy);
                        connectProductHubToGatewayRegion(region, copy);
                        return copy;
                    } catch (BundleException e) {
                        // Have to throw this here.
                        // Something unexpected and non recoverable happened.
                        throw new DynamicBundleException("Failed to modify the region graph for region: " + regionName, e);
                    }
                }
            });
        } catch (BundleException e) {
            throw new DynamicBundleException(e);
        }
        return digraph.getRegion(regionName);
    }

    private void connectToApiRegions(Region region, EnumSet<ApiType> apiTypeVisibility, RegionDigraph copy) throws BundleException {
        RegionFilterBuilder allBuilder = copy.createRegionFilterBuilder();
        // We want to import ALL from the api regions
        allBuilder.allowAll(RegionFilter.VISIBLE_ALL_NAMESPACE);

        boolean connectToInternal = false;
        for (ApiType apiType : apiTypeVisibility) {
            ApiRegion apiRegionType = ApiRegion.valueFromApiType(apiType.toString());
            connectToInternal |= apiRegionType.delegateInternal();
            Region apiRegion = copy.getRegion(apiRegionType.getRegionName());
            region.connectRegion(apiRegion, allBuilder.build());
        }
        if (connectToInternal) {
            // connect to the internal region if one of the base api types says to
            region.connectRegion(copy.getRegion(ApiRegion.INTERNAL.getRegionName()), allBuilder.build());
        }
    }

    private void connectProductHubToGatewayRegion(Region region, RegionDigraph copy) throws BundleException {
        RegionFilterBuilder builder = copy.createRegionFilterBuilder();
        // allow all services into the product hub so that all services registered by 
        //  gateway bundles are visible for all to use (JNDI needs this).
        builder.allowAll(RegionFilter.VISIBLE_SERVICE_NAMESPACE);
        Region productHub = copy.getRegion(REGION_PRODUCT_HUB);
        productHub.connectRegion(region, builder.build());
    }

    private static Version getAppVersion(GatewayConfiguration config) {
        Version appVersion = config.getApplicationVersion();
        if (appVersion == null)
            appVersion = Version.emptyVersion;
        return appVersion;
    }

    private static String getSymbolicName(ClassLoaderConfiguration clConfig) {
        String symbolicName = String.format("gateway.bundle.%s.%s", clConfig.getId().getDomain(), clConfig.getId().getId());
        // Sanitize the symbolic name so it's syntactically correct. For
        // example, '/' characters must be removed from the context root of a
        // WAR. Do not use '.' as the replacement character because it's a
        // literal in the OSGi grammar, not a token. Symbolic names may only
        // begin and end with a token.
        return symbolicName.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String getAppName(GatewayConfiguration config, ClassLoaderConfiguration clConfig) {
        // work out a bundle name to use
        String appName = config.getApplicationName();
        if (appName == null)
            appName = "" + clConfig.getId();
        appName = "Gateway bundle for " + appName;
        return appName;
    }
}