/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.eclipse.equinox.region.RegionDigraph.FilteredRegion;
import org.eclipse.equinox.region.RegionDigraphVisitor;
import org.eclipse.equinox.region.RegionFilter;
import org.eclipse.equinox.region.RegionFilterBuilder;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Capability;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.feature.ApiRegion;
import com.ibm.ws.kernel.feature.internal.BundleList.FeatureResourceHandler;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.LibertyBootRuntime;
import com.ibm.ws.kernel.provisioning.packages.SharedPackageInspector.PackageType;
import com.ibm.ws.kernel.service.util.ResolutionReportHelper;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * There are two provisioners in the runtime: one is used by the code that
 * creates and launches the framework: it installs the base set of bundles
 * required to start/bootstrap the server.
 *
 * This provisioner is used to install and start additional/optional feature
 * bundles.
 */
public class Provisioner {
    private static final TraceComponent tc = Tr.register(Provisioner.class);
    private static final String BUNDLE_LOC_FEATURE_TAG = "feature@";
    static final String BUNDLE_LOC_REFERENCE_TAG = "reference:";
    private final String BUNDLE_LOC_FILE_REFERENCE_TAG = BUNDLE_LOC_REFERENCE_TAG + "file:";
    private static final String BUNDLE_LOC_PROD_EXT_TAG = "productExtension:";

    private static final String REGION_EXTENSION_PREFIX = "liberty.extension.";
    private static final String REGION_PRODUCT_HUB = "liberty.product.api.spi.hub";

    private static final Pattern INVALID_REGION_CHARS = Pattern.compile("[:=\\n*?\"\\\\]");

    /**
     * This rule denies all capabilities with the namespace
     * osgi.wiring.package, osgi.wiring.bundle, or osgi.wiring.host.
     * Also deny the region specific org.eclipse.equinox.allow.bundle[.lifecycle] namespaces that imports whole bundles
     * and bundle lifecycle events.
     * All other namespaces are let through
     */
    private static final String COMMON_ALL_NAMESPACE_FILTER = "(!(|" +
                                                              "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + PackageNamespace.PACKAGE_NAMESPACE + ")" +
                                                              "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + BundleNamespace.BUNDLE_NAMESPACE + ")" +
                                                              "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + HostNamespace.HOST_NAMESPACE + ")" +
                                                              "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + RegionFilter.VISIBLE_BUNDLE_NAMESPACE + ")" +
                                                              "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + RegionFilter.VISIBLE_BUNDLE_LIFECYCLE_NAMESPACE + ")))";

    /**
     * Same rule as COMMON_ALL_NAMESPACE_FILTER except for OSGi apps we also want to prevent osgi.service and
     * region specific org.eclipse.equinox.allow.service namespaces so they can be controlled by the
     * IBM-API-Service header.
     */
    private static final String COMMON_OSGI_APPS_ALL_NAMESPACE_FILTER = "(!(|" +
                                                                        "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + PackageNamespace.PACKAGE_NAMESPACE + ")" +
                                                                        "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + BundleNamespace.BUNDLE_NAMESPACE + ")" +
                                                                        "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + HostNamespace.HOST_NAMESPACE + ")" +
                                                                        "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + RegionFilter.VISIBLE_BUNDLE_NAMESPACE + ")" +
                                                                        "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + RegionFilter.VISIBLE_BUNDLE_LIFECYCLE_NAMESPACE
                                                                        + ")" +
                                                                        "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + RegionFilter.VISIBLE_SERVICE_NAMESPACE + ")" +
                                                                        "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + ServiceNamespace.SERVICE_NAMESPACE + ")))";

    /**
     * OSGi Apps appear to have these services available to them no matter what
     */
    private static final String[] COMMON_OSGI_APPS_SERVICE_REQUIREMENTS = new String[] {
                                                                                         "(objectClass=javax.transaction.TransactionSynchronizationRegistry)",
                                                                                         "(objectClass=javax.transaction.UserTransaction)",
                                                                                         "(osgi.jndi.url.scheme=*)",
                                                                                         "(osgi.jndi.service.name=*)"
    };

    /** Allows bundles to require the system bundle */
    private static final String COMMON_BUNDLE_NAMESPACE_FILTER = "(|" +
                                                                 "(" + BundleNamespace.BUNDLE_NAMESPACE + "=" + Constants.SYSTEM_BUNDLE_SYMBOLICNAME + ")" +
                                                                 "(" + BundleNamespace.BUNDLE_NAMESPACE + "=org.eclipse.osgi))";

    private static final String PACKAGE_AND_BUNDLE_FILTER = "(!(|" +
                                                            "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + PackageNamespace.PACKAGE_NAMESPACE + ")" +
                                                            "(" + RegionFilter.VISIBLE_ALL_NAMESPACE_ATTRIBUTE + "=" + RegionFilter.VISIBLE_BUNDLE_NAMESPACE + ")))";

    private static final String THREAD_CONTEXT_FILTER = "(thread-context=true)";

    private static final String REGION_KERNEL = "org.eclipse.equinox.region.kernel";

    /** Owning/Associated feature manager */
    private final FeatureManager featureManager;
    private final Region kernelRegion;

    /**
     * Passed in via system property, this set contains a list of packages that will not
     * be exposed as APIs or SPIs to user applications. This property overrides APIs/SPIs
     * defined in feature manifests.
     */
    private final Set<String> apiPackagesToIgnore;

    private final Field dynamicMissRefField;

    private final boolean libertyBoot;

    public Provisioner(FeatureManager mgr, Set<String> apiPackagesToIgnore) throws IllegalStateException {
        featureManager = mgr;
        final RegionDigraph original = mgr.getDigraph();
        Region systemRegion = original.getRegion(mgr.bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION));
        if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(systemRegion.getName()) && systemRegion.getBundleIds().size() > 1) {
            //Moving all the other bundles except the system bundle from system bundle region to kernel region.
            //This can happen when the kernel bundle is discarded because of a timestamp change on disk to the kernel bundle jar which causes it to move to system.bundle region.
            //We want only system bundle to be present inside the system.bundle region
            try {
                ApiRegion.update(featureManager.getDigraph(), new Callable<RegionDigraph>() {
                    @Override
                    public RegionDigraph call() throws Exception {
                        RegionDigraph copy;
                        try {
                            copy = original.copy();
                        } catch (BundleException e) {
                            // have to throw this here
                            throw e;
                        }
                        Region fromSystemRegion = copy.getRegion(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
                        Region toKernelRegion = copy.getRegion(REGION_KERNEL);

                        for (long bundleId : fromSystemRegion.getBundleIds()) {
                            if (bundleId > 0) {
                                fromSystemRegion.removeBundle(bundleId);
                                toKernelRegion.addBundle(bundleId);
                            }
                        }
                        return copy;
                    }
                });
            } catch (BundleException e) {
                throw new IllegalStateException(e);
            }
        }
        kernelRegion = original.getRegion(mgr.bundleContext.getBundle());

        if (apiPackagesToIgnore == null) {
            apiPackagesToIgnore = Collections.emptySet();
        }
        this.apiPackagesToIgnore = apiPackagesToIgnore;
        Field tmpField = null;
        try {
            tmpField = ModuleWiring.class.getDeclaredField("dynamicMissRef");
            tmpField.setAccessible(true);
        } catch (Exception e) {
            // really bad stuff if this happens; blow up
            throw new RuntimeException(e);
        }
        this.dynamicMissRefField = tmpField;

        libertyBoot = Boolean.parseBoolean(mgr.bundleContext.getProperty(BootstrapConstants.LIBERTY_BOOT_PROPERTY));
    }

    /**
     * Install framework bundles.
     *
     * @param bundleList
     *                              Properties describing the bundles to install
     * @param minStartLevel
     *                              Minimum start level for bundles named in the properties file
     * @param defaultStartLevel
     *                              Default start level for bundles named in the properties file
     * @param locSvc
     *                              WsLocationAdmin service to use to find bundles
     * @return BundleInstallStatus containing details about the bundles
     *         installed, exceptions that occurred, bundles that couldn't be
     *         found, etc.
     */
    // NOTE: The catch blocks for Exception/IllegalStateException below stores the exception in an
    // InstallStatus object and used in FFDC at a more appropriate time.
    public void installBundles(final BundleContext bContext,
                               final BundleList bundleList,
                               final BundleInstallStatus installStatus,
                               final int minStartLevel, final int defaultStartLevel,
                               final int defaultInitialStartLevel,
                               final WsLocationAdmin locSvc) {

        if (bundleList == null || bundleList.isEmpty())
            return;
        final FrameworkWiring fwkWiring = featureManager.bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(FrameworkWiring.class);
        final File bootFile = getBootJar();
        bundleList.foreach(new BundleList.FeatureResourceHandler() {
            @Override
            @FFDCIgnore({ IllegalStateException.class, Exception.class })
            public boolean handle(FeatureResource fr) {
                Bundle bundle = null;
                WsResource resource = null;
                String urlString = fr.getLocation();

                try {
                    String bundleRepositoryType = fr.getBundleRepositoryType();
                    BundleRepositoryHolder bundleRepositoryHolder = featureManager.getBundleRepositoryHolder(bundleRepositoryType);
                    if (bundleRepositoryHolder == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Bundle repository not found for type=" + bundleRepositoryType);
                        }
                        Tr.error(tc, "UPDATE_MISSING_BUNDLE_ERROR", fr.getMatchString());
                        installStatus.addMissingBundle(fr);
                        return true;
                    }
                    // Get the product name for which the bundles are being installed.
                    String productName = bundleRepositoryHolder.getFeatureType();

                    if (libertyBoot) {
                        bundle = installLibertyBootBundle(productName, fr, fwkWiring);
                    } else {
                        bundle = installFeatureBundle(urlString, productName, bundleRepositoryHolder, fr);
                    }
                    if (bundle == null) {
                        return true;
                    }

                    BundleStartLevel bsl = bundle.adapt(BundleStartLevel.class);
                    BundleRevision bRev = bundle.adapt(BundleRevision.class);

                    int level = 0;

                    // For non-fragment bundles set the bundle startLevel then
                    // add to the list of bundles to be started.
                    // The order is important because the bundles are sorted by
                    // start level to preserve the start level ordering during
                    // dynamic feature additions.
                    if ((bRev.getTypes() & BundleRevision.TYPE_FRAGMENT) != BundleRevision.TYPE_FRAGMENT) {

                        level = bsl.getStartLevel();

                        // Set the start level on the bundle to the selected value
                        // if it hasn't been set before
                        if (level == defaultInitialStartLevel) {
                            int sl = fr.getStartLevel();
                            int newLevel = (sl == 0) ? defaultStartLevel : (sl < minStartLevel) ? minStartLevel : sl;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(this, tc, "Changing the start level of bundle {0} from {1} to the current level of {2}", bundle, level, newLevel);
                            }
                            level = newLevel;
                            bsl.setStartLevel(level);
                        }

                        installStatus.addBundleToStart(bundle);
                    }

                    // need to get a resource for the bundle list createAssociation
                    // call, if we end up with a null resource then bad things
                    // happen, like we fail to uninstall bundles when we should
                    File bundleFile = getBundleFile(bundle);
                    resource = locSvc.asResource(bundleFile, bundleFile.isFile());

                    // Update bundle list with resolved information
                    bundleList.createAssociation(fr, bundle, resource, level);
                } catch (IllegalStateException e) {
                    // The framework is stopping: this is an expected but not ideal occurrence.
                    installStatus.markContextInvalid(e);
                    return false;
                } catch (Exception e) {
                    // We encountered an error installing a bundle, add it to
                    // the status, and continue. The caller will handle as appropriate
                    installStatus.addInstallException("INSTALL " + urlString + " (resolved from: " + fr + ")", e);
                }
                return true;
            }

            private File getBundleFile(Bundle bundle) {
                if (libertyBoot) {
                    return bootFile;
                }
                // make sure we have a File for the bundle that is already
                // installed. Get this by processing the location. We
                // need to get past the reference:file: part of the URL.
                String location = bundle.getLocation();
                int index = location.indexOf(BUNDLE_LOC_REFERENCE_TAG);
                location = location.substring(index + BUNDLE_LOC_REFERENCE_TAG.length());
                // This file path is URL form, convert it back to a valid system File path
                return new File(URI.create(location));
            }

            private Bundle installLibertyBootBundle(String productName, FeatureResource fr, FrameworkWiring fwkWiring) throws BundleException, IOException {
                //getting the LibertyBootRuntime instance and installing the boot bundle
                LibertyBootRuntime libertyBoot = featureManager.getLibertyBoot();
                if (libertyBoot == null) {
                    throw new IllegalStateException("No LibertBootRuntime service available!");
                }

                Bundle bundle = libertyBoot.installBootBundle(fr.getSymbolicName(), fr.getVersionRange(), BUNDLE_LOC_FEATURE_TAG);
                if (bundle == null) {
                    installStatus.addMissingBundle(fr);
                    return null;
                }

                Region productRegion = getProductRegion(productName);
                Region current = featureManager.getDigraph().getRegion(bundle);
                if (!productRegion.equals(current)) {
                    current.removeBundle(bundle);
                    productRegion.addBundle(bundle);
                }
                return bundle;
            }

            private Bundle installFeatureBundle(String urlString, String productName, BundleRepositoryHolder bundleRepositoryHolder,
                                                FeatureResource fr) throws BundleException, IOException {
                Bundle bundle = fetchInstalledBundle(urlString, productName);
                if (bundle == null) {
                    ContentBasedLocalBundleRepository lbr = bundleRepositoryHolder.getBundleRepository();
                    // Try to find the file, hopefully using the cached path
                    File bundleFile = lbr.selectBundle(urlString, fr.getSymbolicName(), fr.getVersionRange());

                    if (bundleFile == null) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Bundle not matched", lbr, fr);
                        }
                        Tr.error(tc, "UPDATE_MISSING_BUNDLE_ERROR", fr.getMatchString());
                        installStatus.addMissingBundle(fr);
                        return null;
                    }

                    // Get URL from filename
                    URI uri = bundleFile.toURI();
                    urlString = uri.toURL().toString();

                    urlString = PathUtils.normalize(urlString);

                    // Install this bundle as a "reference"-- this means that the
                    // framework will not copy this bundle into it's private cache,
                    // it will run from the actual jar (wherever it is).
                    urlString = BUNDLE_LOC_REFERENCE_TAG + urlString;

                    // Get the bundle location.
                    // The location format being returned must match the format in SchemaBundle and BundleList.
                    String location = getBundleLocation(urlString, productName);

                    Region productRegion = getProductRegion(productName);
                    // Bundle will just be returned if something from this location exists already.
                    bundle = productRegion.installBundleAtLocation(location, new URL(urlString).openStream());
                }
                return bundle;
            }

            private Bundle fetchInstalledBundle(String urlString, String productName) {
                // We install bundles as references so we need to ensure that we add reference: to the file url.
                String location = getBundleLocation(BUNDLE_LOC_REFERENCE_TAG + urlString, productName);
                Bundle b = featureManager.bundleContext.getBundle(location);

                if (b != null && b.getState() == Bundle.UNINSTALLED) {
                    b = null;
                }

                return b;
            }
        });
    }

    private File getBootJar() {
        if (!libertyBoot) {
            return null;
        }
        ProtectionDomain pd = getClass().getProtectionDomain();
        if (pd == null) {
            throw new IllegalStateException("No protection domain for boot jar.");
        }
        CodeSource cs = pd.getCodeSource();
        if (cs == null) {
            throw new IllegalStateException("No code source for boot jar.");
        }
        URL loc = cs.getLocation();
        String spec = loc.toExternalForm();
        if ("jar".equals(loc.getProtocol())) {
            int bangSlash;
            while ((bangSlash = spec.lastIndexOf("!/")) != -1) {
                spec = spec.substring(0, bangSlash);
            }
            spec = spec.substring(4); // jar: length
        }
        if (!spec.startsWith("file:")) {
            throw new IllegalStateException("The code source for the boot jar does not come from a file:" + loc);
        }
        try {
            return new File(new URI(spec));
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    static File getFile(URL url) {
        String path;
        try {
            // The URL for a UNC path is file:////server/path, but the
            // deprecated File.toURL() as used by java -jar/-cp incorrectly
            // returns file://server/path/, which has an invalid authority
            // component.  Rewrite any URLs with an authority ala
            // http://wiki.eclipse.org/Eclipse/UNC_Paths
            if (url.getAuthority() != null) {
                url = new URL("file://" + url.toString().substring("file:".length()));
            }

            path = new File(url.toURI()).getPath();
        } catch (MalformedURLException e) {
            path = null;
        } catch (URISyntaxException e) {
            path = null;
        }

        if (path == null) {
            // If something failed, assume the path is good enough.
            path = url.getPath();
        }

        return new File(normalizePathDrive(path));
    }

    static String normalizePathDrive(String path) {
        if (File.separatorChar == '\\' && path.length() > 1 && path.charAt(1) == ':' && path.charAt(0) >= 'a' && path.charAt(0) <= 'z') {
            path = Character.toUpperCase(path.charAt(0)) + path.substring(1);
        }
        return path;
    }

    /**
     * Start all previously installed bundles, but defer (or not)
     * the ACTIVATION of those bundles based on the Bundle-ActivationPolicy
     * value set in MANIFEST.MF.
     *
     * Perform the resolve operation before starting bundles
     * to avoid intermittent issues with BundleException.STATECHANGED
     * during Bundle start as other bundles resolve asynchronously
     *
     * @return BundleStartStatus object containing exceptions encountered while
     *         starting bundles
     * @see BundleLifecycleStatus
     */
    @FFDCIgnore(BundleException.class)
    public BundleLifecycleStatus preStartBundles(List<Bundle> installedBundles) {
        BundleLifecycleStatus startStatus = new BundleLifecycleStatus();

        if (installedBundles == null || installedBundles.size() == 0)
            return startStatus;

        for (Bundle b : installedBundles) {
            // Skip any null bundles in the list
            if (b == null)
                continue;

            int state = b.getState();

            // Only start bundles that are in certain states (not UNINSTALLED, or already STARTING)
            if (state == Bundle.UNINSTALLED || state >= org.osgi.framework.Bundle.STARTING)
                continue;

            try {
                b.start(Bundle.START_ACTIVATION_POLICY);
            } catch (BundleException e) {
                // No FFDC, these are handled later.
                startStatus.addStartException(b, e);
            }
        }

        return startStatus;
    }

    /**
     * Utility for resolving bundles
     *
     * @param bundlesToResolve
     */
    public void resolveBundles(BundleContext bContext, List<Bundle> bundlesToResolve) {
        if (bundlesToResolve == null || bundlesToResolve.size() == 0) {
            return;
        }

        // do a quick check if there is any work to do
        boolean allResolved = true;
        int resolveMask = Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE | Bundle.STOPPING;
        for (Bundle bundle : bundlesToResolve) {
            allResolved &= (bundle.getState() & resolveMask) != 0;
        }
        if (allResolved) {
            return;
        }
        FrameworkWiring wiring = adaptSystemBundle(bContext, FrameworkWiring.class);
        if (wiring != null) {
            ResolutionReportHelper rrh = null;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                rrh = new ResolutionReportHelper();
                rrh.startHelper(bContext);
            }
            try {
                wiring.resolveBundles(bundlesToResolve);
            } finally {
                if (rrh != null) {
                    rrh.stopHelper();
                    Tr.debug(this, tc, rrh.getResolutionReportString());
                }
            }
        }
    }

    /**
     * Remove any element in this cache that is not also in the bundle list
     * passed in as a parameter: Uninstall the corresponding bundle
     * for every removed element.
     *
     * @param bundleCache
     * @param newBundleList
     * @param bundleContext
     * @throws BundleException
     */
    @FFDCIgnore({ IllegalStateException.class, Exception.class })
    public BundleLifecycleStatus uninstallBundles(final BundleContext bundleContext,
                                                  final BundleList removeBundles,
                                                  final BundleInstallStatus installStatus,
                                                  ShutdownHookManager shutdownHook) {

        final List<Bundle> bundlesToUninstall = new ArrayList<Bundle>();
        removeBundles.foreach(new FeatureResourceHandler() {

            @Override
            @FFDCIgnore({ Exception.class })
            public boolean handle(FeatureResource fr) {
                try {
                    Bundle b = removeBundles.getBundle(bundleContext, fr);

                    // We found the bundle we want to uninstall
                    if (b != null && b.getBundleId() > 0) {
                        bundlesToUninstall.add(b);
                    }
                } catch (Exception e) {
                    installStatus.addInstallException("UNINSTALL " + fr.getLocation(), e);
                }
                return true;
            }
        });

        // sort the bundle in reverse start-level order so that
        // start-phase is consistently honored
        Collections.sort(bundlesToUninstall, Collections.reverseOrder(BundleInstallStatus.sortByStartLevel));
        for (Bundle bundleToUninstall : bundlesToUninstall) {
            try {
                bundleToUninstall.uninstall();
            } catch (IllegalStateException e) {
                // ok: bundle already uninstalled or the framework is stopping,
                // determine if we should continue on to the next:
                // (if the bundle was uninstalled, but the framework is in perfect health),
                // or not (if the framework is stopping and we should just get done early).
                if (!!!FrameworkState.isValid()) {
                    break;
                }
            } catch (Exception e) {
                installStatus.addInstallException("UNINSTALL " + bundleToUninstall.getLocation(), e);
            }
        }

        // Refresh bundles: provide a listener for notification of when refresh is complete
        RefreshBundlesListener listener = new RefreshBundlesListener(shutdownHook);
        FrameworkWiring wiring = adaptSystemBundle(bundleContext, FrameworkWiring.class);
        if (wiring != null) {
            wiring.refreshBundles(null, listener);

            // wait for refresh operation to complete
            listener.waitForComplete();
        }
        return listener.getStatus();
    }

    /**
     * Gets the bundle location.
     * The location format is consistent with what SchemaBundle and BundleList.
     *
     * @return The bundle location.
     */
    public static String getBundleLocation(String urlString, String productName) {
        String productNameInfo = (productName != null && !productName.isEmpty()) ? (BUNDLE_LOC_PROD_EXT_TAG + productName + ":") : "";
        return BUNDLE_LOC_FEATURE_TAG + productNameInfo + urlString;
    }

    /**
     * Gets the region name according to the product name.
     *
     * @param productName the product name. Empty string or <code>null</code> indicates
     *                        the liberty profile itself.
     * @return the region name
     */
    private String getRegionName(String productName) {
        if (productName == null || productName.isEmpty()) {
            return kernelRegion.getName();
        }
        return REGION_EXTENSION_PREFIX + INVALID_REGION_CHARS.matcher(productName).replaceAll("-");
    }

    private Region getProductRegion(String productName) throws BundleException {
        RegionDigraph digraph = featureManager.getDigraph();
        return digraph.getRegion(getRegionName(productName));
    }

    Set<String> createAndUpdateProductRegions() throws BundleException {
        final Set<String> products = new HashSet<String>();
        // Keep a map of product name -> set of apiServices
        final Map<String, Set<String>> productAPIServiceMap = new HashMap<String, Set<String>>();
        for (ProvisioningFeatureDefinition featureDef : featureManager.getInstalledFeatureDefinitions()) {
            products.add(featureDef.getBundleRepositoryType());
            // Collect each feature ApiServices and store them for the associated product
            String featureApiServices = featureDef.getApiServices();
            if (featureApiServices != null && !!!featureApiServices.isEmpty()) {
                Set<String> productApiServices = productAPIServiceMap.get(featureDef.getBundleRepositoryType());
                if (productApiServices == null) {
                    productApiServices = new LinkedHashSet<String>();
                    productAPIServiceMap.put(featureDef.getBundleRepositoryType(), productApiServices);
                }
                productApiServices.add(featureApiServices);
            }
        }

        if (products.isEmpty()) {
            // This should never be empty.
            // unit tests seem to have this empty
            return Collections.emptySet();
        }

        // always prime the products with the kernel name (empty string)
        products.add("");

        final Set<String> productRegionsToRemove = new HashSet<String>();
        ApiRegion.update(featureManager.getDigraph(), new Callable<RegionDigraph>() {

            @Override
            public RegionDigraph call() throws Exception {
                productRegionsToRemove.clear();
                RegionDigraph copy;
                try {
                    copy = featureManager.getDigraph().copy();
                } catch (BundleException e) {
                    // have to throw this here
                    throw e;
                }
                // collect all the current product region names
                for (Region region : copy) {
                    if (region.getName().startsWith(REGION_EXTENSION_PREFIX)) {
                        productRegionsToRemove.add(region.getName());
                    }
                }

                // Create the system bundle region, if needed
                createSystemBundleRegion(copy);
                // Make sure all the gateway API hubs exist
                primeAPIRegions(copy);

                for (String productName : products) {
                    String productRegionName = getRegionName(productName);
                    Region existing = copy.getRegion(productRegionName);
                    Set<Long> existingBundles = Collections.emptySet();
                    Set<FilteredRegion> existingEdges = Collections.emptySet();
                    if (existing != null) {
                        existingBundles = existing.getBundleIds();
                        existingEdges = existing.getEdges();
                        copy.removeRegion(existing);
                    }
                    // Pass in the apiServices associated with this product
                    Region newProductRegion = createProductRegion(productName, productRegionName, productAPIServiceMap.get(productName), copy, existingBundles, existingEdges);
                    productRegionsToRemove.remove(newProductRegion.getName());
                }

                return copy;
            }
        });
        return productRegionsToRemove;
    }

    private void createSystemBundleRegion(RegionDigraph digraph) throws BundleException {

        Bundle systemBundle = this.featureManager.bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);

        // Check to see if we already moved the system bundle to its own region
        if (digraph.getRegion(systemBundle).getName().equals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME)) {
            return;
        }

        Region systemBundleRegion;
        digraph.getRegion(systemBundle).removeBundle(systemBundle);
        systemBundleRegion = digraph.createRegion(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
        systemBundleRegion.addBundle(systemBundle);

    }

    private void primeAPIRegions(RegionDigraph digraph) throws BundleException {
        Region productHub = digraph.getRegion(REGION_PRODUCT_HUB);
        if (productHub == null) {
            productHub = digraph.createRegion(REGION_PRODUCT_HUB);
        }
        RegionFilterBuilder builder = digraph.createRegionFilterBuilder();
        // allow all services into the product hub so that all services registered by
        //  gateway bundles are visible for all to use (JNDI needs this).
        builder.allowAll(RegionFilter.VISIBLE_SERVICE_NAMESPACE);
        RegionFilter filter = builder.build();

        for (ApiRegion apiRegion : ApiRegion.values()) {
            Region regionApiHub = digraph.getRegion(apiRegion.getRegionName());
            if (regionApiHub == null) {
                regionApiHub = digraph.createRegion(apiRegion.getRegionName());
                productHub.connectRegion(regionApiHub, filter);
            }
        }
    }

    private Region createProductRegion(String productName, String productRegionName, Set<String> productApiServices, RegionDigraph digraph, Set<Long> bundleIds,
                                       Set<FilteredRegion> edges) throws BundleException {
        Region productRegion = digraph.createRegion(productRegionName);
        for (Long bundleId : bundleIds) {
            productRegion.addBundle(bundleId.longValue());
        }
        try {
            connectToProductHub(productName, productRegion, digraph);
            connectToGatewayHubs(productName, productRegion, productApiServices, digraph);
            for (FilteredRegion edge : edges) {
                Region head = edge.getRegion();
                if (!(REGION_PRODUCT_HUB.equals(head.getName()) || Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(head.getName()))) {
                    digraph.connect(productRegion, edge.getFilter(), edge.getRegion());
                }
            }
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        return productRegion;
    }

    /**
     * @param productName
     * @param productRegion
     * @param productApiServices
     * @param digraph
     * @throws BundleException
     * @throws InvalidSyntaxException
     */
    private void connectToGatewayHubs(String productName, Region productRegion, Set<String> productApiServices,
                                      RegionDigraph digraph) throws BundleException, InvalidSyntaxException {
        List<String> ibmApiFilters = new ArrayList<String>();
        List<String> strictSpecApiFilters = new ArrayList<String>();
        List<String> thirdPartyApiFilters = new ArrayList<String>();
        List<String> stableApiFilters = new ArrayList<String>();
        List<String> userDefinedApiFilters = new ArrayList<String>();
        List<String> internalApiFilters = new ArrayList<String>();
        List<String> osgiAppsAPIFilters = new ArrayList<String>();
        for (Iterator<String> gatewayPackages = featureManager.packageInspector.getGatewayPackages(productName); gatewayPackages.hasNext();) {
            String packageName = gatewayPackages.next();
            if (apiPackagesToIgnore.contains(packageName)) {
                // ignore this package:
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ignoring API/SPI package due to bootstrap property override: " + packageName);
                }
                continue;
            }
            String clause = "(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + packageName + ")";
            // all types of gateway packages are available to osgi apps
            osgiAppsAPIFilters.add(clause);
            PackageType type = featureManager.packageInspector.getExportedPackageType(packageName);
            if (type.isInternalApi()) {
                // TODO For internal types only add to the internal region
                // All the defined api type regions delegate to internal
                // granting them access to internal packages.
                // That seems wrong, but keeping it that way to avoid breaking
                // behavior changes.
                internalApiFilters.add(clause);
            } else {
                if (type.isIbmApi()) {
                    ibmApiFilters.add(clause);
                }
                if (type.isStrictSpecApi()) {
                    strictSpecApiFilters.add(clause);
                }
                if (type.isThirdPartyApi()) {
                    thirdPartyApiFilters.add(clause);
                }
                if (type.isStableApi()) {
                    stableApiFilters.add(clause);
                }
                if (type.isUserDefinedApi()) {
                    userDefinedApiFilters.add(clause);
                }
            }
        }

        // Only care about the productApiServices for the osgiApps region
        connectGatewayRegion(ApiRegion.ALL, osgiAppsAPIFilters, productRegion, productApiServices, digraph);
        connectGatewayRegion(ApiRegion.IBM, ibmApiFilters, productRegion, null, digraph);
        connectGatewayRegion(ApiRegion.SPEC, strictSpecApiFilters, productRegion, null, digraph);
        connectGatewayRegion(ApiRegion.THIRD_PARTY, thirdPartyApiFilters, productRegion, null, digraph);
        connectGatewayRegion(ApiRegion.STABLE, stableApiFilters, productRegion, null, digraph);
        connectGatewayRegion(ApiRegion.USER, userDefinedApiFilters, productRegion, null, digraph);
        connectGatewayRegion(ApiRegion.INTERNAL, internalApiFilters, productRegion, null, digraph);
        connectGatewayRegion(ApiRegion.THREAD_CONTEXT, Collections.singletonList(THREAD_CONTEXT_FILTER), productRegion, null, digraph);
    }

    /**
     * @param apiRegion
     * @param packageFilters
     * @param productRegion
     * @param productApiServices
     * @throws InvalidSyntaxException
     * @throws BundleException
     */
    private void connectGatewayRegion(ApiRegion apiRegion, List<String> packageFilters, Region productRegion, Set<String> productApiServices,
                                      RegionDigraph digraph) throws InvalidSyntaxException, BundleException {
        RegionFilterBuilder toProductBuilder = digraph.createRegionFilterBuilder();

        if (productApiServices != null) {
            // product api services are used to isolate osgi apps from our services
            toProductBuilder.allow(RegionFilter.VISIBLE_ALL_NAMESPACE, COMMON_OSGI_APPS_ALL_NAMESPACE_FILTER);
        } else {
            toProductBuilder.allow(RegionFilter.VISIBLE_ALL_NAMESPACE, COMMON_ALL_NAMESPACE_FILTER);
        }
        toProductBuilder.allow(BundleNamespace.BUNDLE_NAMESPACE, COMMON_BUNDLE_NAMESPACE_FILTER);

        // This creates a sharing rule for importing all capabilities in the osgi.wiring.package namespace
        // which are API packages.
        for (String packageFilter : packageFilters) {
            toProductBuilder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, packageFilter);
        }

        if (productApiServices != null) {
            for (String apiServices : productApiServices) {
                ManifestElement[] serviceElements = ManifestElement.parseHeader(FeatureDefinitionUtils.IBM_API_SERVICE, apiServices);
                if (serviceElements != null) {
                    for (ManifestElement serviceElement : serviceElements) {
                        // each API service header clause contains a service objectClass and map of attributes
                        // create a filter that uses this information to match the provided service
                        StringBuilder serviceFilterBuilder = new StringBuilder();
                        // always add the objectClass to the filter
                        serviceFilterBuilder.append("(").append(Constants.OBJECTCLASS).append("=").append(serviceElement.getValue()).append(")");
                        Enumeration<String> attrKeys = serviceElement.getKeys();
                        if (attrKeys != null && attrKeys.hasMoreElements()) {
                            // if there are attributes then create an AND'ed filter that matches each attribute
                            serviceFilterBuilder.insert(0, "(&");
                            while (attrKeys.hasMoreElements()) {
                                String attrKey = attrKeys.nextElement();
                                String attrValue = serviceElement.getAttribute(attrKey);
                                serviceFilterBuilder.append("(").append(attrKey).append("=").append(attrValue).append(")");
                            }
                            serviceFilterBuilder.append(")");
                        }
                        toProductBuilder.allow(ServiceNamespace.SERVICE_NAMESPACE, serviceFilterBuilder.toString());
                        // TODO Need to remove the following allow once it is deprecated by equinox regions
                        toProductBuilder.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, serviceFilterBuilder.toString());
                    }
                }
            }

            for (String commonServiceFilter : COMMON_OSGI_APPS_SERVICE_REQUIREMENTS) {
                toProductBuilder.allow(ServiceNamespace.SERVICE_NAMESPACE, commonServiceFilter);
                // TODO Need to remove the following allow once it is deprecated by equinox regions
                toProductBuilder.allow(RegionFilter.VISIBLE_SERVICE_NAMESPACE, commonServiceFilter);
            }
        }

        Region regionApiHub = digraph.getRegion(apiRegion.getRegionName());
        regionApiHub.connectRegion(productRegion, toProductBuilder.build());
    }

    /**
     * @param productName
     * @param productRegion
     * @param digraph
     * @throws BundleException
     * @throws InvalidSyntaxException
     */
    private void connectToProductHub(String productName, Region productRegion, RegionDigraph digraph) throws BundleException, InvalidSyntaxException {
        Region productHub = digraph.getRegion(REGION_PRODUCT_HUB);
        if (productHub == null) {
            productHub = digraph.createRegion(REGION_PRODUCT_HUB);
        }

        RegionFilterBuilder toHubBuilder = digraph.createRegionFilterBuilder();
        RegionFilterBuilder toProductBuilder = digraph.createRegionFilterBuilder();

        // We want to import ALL from the hub
        toHubBuilder.allowAll(RegionFilter.VISIBLE_ALL_NAMESPACE);
        productRegion.connectRegion(productHub, toHubBuilder.build());

        toProductBuilder.allow(RegionFilter.VISIBLE_ALL_NAMESPACE, COMMON_ALL_NAMESPACE_FILTER);
        toProductBuilder.allow(BundleNamespace.BUNDLE_NAMESPACE, COMMON_BUNDLE_NAMESPACE_FILTER);
        // for products we want to share all lifecycle operations across products
        toProductBuilder.allowAll(RegionFilter.VISIBLE_BUNDLE_LIFECYCLE_NAMESPACE);

        // This creates a sharing rule for importing all capabilities in the osgi.wiring.package namespace
        // which are API packages.
        for (Iterator<String> productPackages = featureManager.packageInspector.getExtensionPackages(productName); productPackages.hasNext();) {
            toProductBuilder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + productPackages.next() + ")");
        }

        // Block for Liberty/Kernel product region case
        if (productRegion.getName().equals(kernelRegion.getName())) {
            // For the kernel/core region be sure to share all system bundle packages
            // TODO This is not clean design, but the behavior of 8.5.5
            toProductBuilder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, "(" + PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE + "="
                                                                           + Constants.SYSTEM_BUNDLE_SYMBOLICNAME + ")");

            // Allow all from the System Bundle Region to the Kernel Region
            Region systemBundleRegion = digraph.getRegion(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
            RegionFilterBuilder toKernelfromSystemBuilder = digraph.createRegionFilterBuilder();
            toKernelfromSystemBuilder.allowAll(RegionFilter.VISIBLE_ALL_NAMESPACE);
            systemBundleRegion.connectRegion(productRegion, toKernelfromSystemBuilder.build());

            RegionFilterBuilder toSystemFromKernelBuilder = digraph.createRegionFilterBuilder();

            if (featureManager.packageInspector.listKernelBlackListApiPackages().hasNext()) {
                // Allow everything except the blacklist packages
                StringBuffer buf = new StringBuffer();
                for (Iterator<String> kernelBlackListExport = featureManager.packageInspector.listKernelBlackListApiPackages(); kernelBlackListExport.hasNext();) {
                    String pack = kernelBlackListExport.next();
                    buf.append("(" + PackageNamespace.PACKAGE_NAMESPACE + "=" + pack + ")");
                }
                String apiPackagesToFilterOut = "(!(|" + buf.toString() + "))";
                toSystemFromKernelBuilder.allow(RegionFilter.VISIBLE_PACKAGE_NAMESPACE, apiPackagesToFilterOut);
                toSystemFromKernelBuilder.allow(RegionFilter.VISIBLE_ALL_NAMESPACE, PACKAGE_AND_BUNDLE_FILTER);

            } else {
                // Allow all filter
                toSystemFromKernelBuilder.allowAll(RegionFilter.VISIBLE_ALL_NAMESPACE);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "System bundle region filter : {0}", toSystemFromKernelBuilder.build().getSharingPolicy());
            }

            productRegion.connectRegion(systemBundleRegion, toSystemFromKernelBuilder.build());
        }
        productHub.connectRegion(productRegion, toProductBuilder.build());
    }

    /**
     * Adapt the system bundle to the specified class. Try w/in a catch block
     * for IllegalStateException, which will be thrown if the bundle context
     * has become invalid (because, for example, the bundle is stopping).
     *
     * @param bundleContext
     * @param target
     * @return
     */
    @FFDCIgnore(IllegalStateException.class)
    private <T> T adaptSystemBundle(final BundleContext bundleContext,
                                    Class<T> target) {
        try {
            Bundle systemBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            if (systemBundle != null)
                return systemBundle.adapt(target);
        } catch (IllegalStateException e) {
        }
        return null;
    }

    /**
     * @param regionsToRemove
     * @throws BundleException
     */
    void removeStaleProductRegions(final Set<String> regionsToRemove) throws BundleException {
        if (regionsToRemove.isEmpty()) {
            return;
        }

        // Need to check for orphans first to ensure we uninstall them while others
        // can still see them.
        // Note that we must not uninstall the bundles while modifying the copy of the
        // digraph because that would immediately invalidate the copy preventing us from
        // using the modified copy to replace the current digraph.

        final RegionDigraph digraph = featureManager.getDigraph();
        final Set<Long> orphanIDs = new HashSet<Long>();
        // Collection orphan bundle IDs
        for (String regionName : regionsToRemove) {
            Region toRemove = digraph.getRegion(regionName);
            if (toRemove != null) {
                orphanIDs.addAll(toRemove.getBundleIds());
            }
        }
        uninstallOphans(orphanIDs);

        ApiRegion.update(featureManager.getDigraph(), new Callable<RegionDigraph>() {

            @Override
            public RegionDigraph call() throws Exception {
                RegionDigraph copy;
                try {
                    copy = digraph.copy();
                } catch (BundleException e) {
                    // have to throw this here
                    throw e;
                }
                // Remove each region and collection orphan bundle IDs.
                // Need to collect orphans here also incase some other thread installed
                // more bundles since the first time we checked.
                for (String regionName : regionsToRemove) {
                    Region toRemove = copy.getRegion(regionName);
                    if (toRemove != null) {
                        orphanIDs.addAll(toRemove.getBundleIds());
                        copy.removeRegion(toRemove);
                    }
                }
                return copy;
            }
        });

        // Now remove any new orphan bundles that came in.
        // This operation may impact extenders listening to bundles,
        // they will not see the uninstall events.  But we must clean up
        // regardless.
        uninstallOphans(orphanIDs);
    }

    @FFDCIgnore(IllegalStateException.class)
    private void uninstallOphans(Set<Long> orphanIDs) {
        // must use the system bundle context since the orphans could be hidden from us
        BundleContext systemContext = featureManager.bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
        for (Long ophanId : orphanIDs) {
            Bundle ophanBundle = systemContext.getBundle(ophanId);
            if (ophanBundle != null) {
                try {
                    ophanBundle.uninstall();
                } catch (IllegalStateException e) {
                    // ignore; move on.  We don't care if something else uninstalled it
                } catch (BundleException e) {
                    // Auto FFDC, but continue on
                }
            }
        }
    }

    void refreshGatewayBundles(ShutdownHookManager shutdownHook) {
        BundleContext systemContext = featureManager.bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
        Set<Bundle> needsRefresh = new LinkedHashSet<Bundle>();
        RegionDigraph digraph = featureManager.getDigraph();
        for (Bundle b : systemContext.getBundles()) {
            // Not sure this is a valid assumption on gateway BSN
            String bsn = b.getSymbolicName();
            if (bsn != null && bsn.startsWith("gateway.bundle.")) {
                Region gatewayRegion = digraph.getRegion(b);
                BundleWiring wiring = b.adapt(BundleWiring.class);
                if (wiring != null) {
                    List<BundleWire> packageWires = wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
                    for (BundleWire packageWire : packageWires) {
                        if (!!!isPackageAccessible(gatewayRegion, packageWire.getCapability())) {
                            // no longer share the dynamic package we are wired to, need to refresh.
                            needsRefresh.add(b);
                        }
                    }
                    if (!!!needsRefresh.contains(b)) {
                        // now clear package misses; this is equinox specific
                        clearPackageMisses(wiring);
                    }
                }
            }
        }

        refreshBundles(needsRefresh, shutdownHook);
    }

    private boolean isPackageAccessible(Region gatewayRegion, final BundleCapability packageCapability) {
        final AtomicBoolean found = new AtomicBoolean(false);
        gatewayRegion.visitSubgraph(new RegionDigraphVisitor() {
            @Override
            public boolean visit(Region region) {
                boolean capabilityInRegion = region.contains(packageCapability.getRevision().getBundle());
                if (capabilityInRegion) {
                    found.set(true);
                    // if we got to the region that contains the capability stop searching
                    return false;
                }
                // otherwise continue on searching to the connects from this region
                return true;
            }

            @Override
            public boolean preEdgeTraverse(RegionFilter filter) {
                // evaluate the filter allows the capability we are looking for
                return filter.isAllowed(packageCapability);
            }

            @Override
            public void postEdgeTraverse(RegionFilter arg0) {
                // do nothing
            }
        });
        return found.get();
    }

    /**
     * @param wiring
     */
    private void clearPackageMisses(BundleWiring wiring) {
        try {
            AtomicReference<?> dynamicMissRef = (AtomicReference<?>) dynamicMissRefField.get(wiring);
            dynamicMissRef.set(null);
        } catch (IllegalArgumentException e) {
            // auto-FFDC
        } catch (IllegalAccessException e) {
            // auto-FFDC
        }
    }

    private void refreshBundles(Collection<Bundle> needsRefresh, ShutdownHookManager shutdownHook) {
        if (!!!needsRefresh.isEmpty()) {
            RefreshBundlesListener listener = new RefreshBundlesListener(shutdownHook);
            FrameworkWiring wiring = adaptSystemBundle(featureManager.bundleContext, FrameworkWiring.class);
            if (wiring != null) {
                wiring.refreshBundles(needsRefresh, listener);
                // wait for refresh operation to complete
                listener.waitForComplete();
            }
        }
    }

    public void refreshFeatureBundles(PackageInspectorImpl packageInspector, BundleContext bundleContext, ShutdownHookManager shutdownHook) {

        Set<String> blackList = returnSet(packageInspector.listKernelBlackListApiPackages());
        if (blackList.isEmpty()) {
            return; // return if the blacklist is empty
        }

        List<Bundle> needsRefresh = new ArrayList<Bundle>();
        // Get the package wiring from system bundle
        BundleWiring systemBundleWiring = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).adapt(BundleWiring.class);
        List<BundleWire> systemPackages = systemBundleWiring.getProvidedWires(PackageNamespace.PACKAGE_NAMESPACE);

        // Find the package wires for package exports that are also currently on the blacklist
        // This means an API package was being provided by the system bundle (which includes all Java provided packages) but
        // now a feature enabled is providing the API package
        for (BundleWire bw : systemPackages) {
            Capability cap = bw.getCapability();
            String pkg = (String) cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);

            if (blackList.contains(pkg)) {
                needsRefresh.add(bw.getRequirerWiring().getBundle());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Bundles requiring refresh, wired to system bundle : {0}", needsRefresh);
        }

        refreshBundles(needsRefresh, shutdownHook);
    }

    private Set<String> returnSet(Iterator<String> iterator) {

        Set<String> hash = new HashSet<String>();
        if (iterator.hasNext()) {
            for (Iterator<String> it = iterator; it.hasNext();) {
                hash.add(it.next());
            }
            return hash;
        } else {
            return Collections.emptySet();
        }
    }

}