/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.feature.AppForceRestart;
import com.ibm.ws.kernel.feature.FeatureDefinition;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.ServerReadyStatus;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.ws.kernel.feature.ServerStartedPhase2;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureRepository;
import com.ibm.ws.kernel.feature.internal.subsystem.KernelFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Chain;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.launch.service.FrameworkReady;
import com.ibm.ws.kernel.launch.service.ProductExtensionServiceFingerprint;
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.ws.kernel.provisioning.LibertyBootRuntime;
import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.ws.kernel.provisioning.ProductExtensionInfo;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;
import com.ibm.wsspi.kernel.service.utils.PathUtils;
import com.ibm.wsspi.kernel.service.utils.TimestampUtils;

/**
 * The feature manager finishes the initialization of the runtime by analyzing a list
 * of enabled features and installing/starting the bundles those features require.
 * <p>
 * Enabled features are configured in server.xml:
 *
 * <pre>
 * &lt;featureManager&gt;
 * &lt;feature&gt;http&lt;/feature&gt;
 * &lt;feature&gt;httpservice&lt;/feature&gt;
 * &lt;/featureManager&gt;
 * </pre>
 *
 * <p>
 * If exceptions occur installing/starting enabled features, the <code>onError</code> attribute
 * can be set to tell the platform not to shut down when provisioning errors occur.
 * <code>onError</code> can inherit its value from the bootstrap.properties file or
 * from an explicit <code>Variable</code> definition in a server configuration file (such as server.xml)
 * </p>
 */
@Component(service = { FeatureProvisioner.class, FrameworkReady.class, ManagedService.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {
                        Constants.SERVICE_VENDOR + "=" + "IBM",
                        Constants.SERVICE_PID + "=" + "com.ibm.ws.kernel.feature"
           })
public class FeatureManager implements FeatureProvisioner, FrameworkReady, ManagedService {

    private static final String ME = FeatureManager.class.getName();
    private static final TraceComponent tc = Tr.register(FeatureManager.class);

    private final static String CFG_KEY_ACTIVE_FEATURES = "feature";

    public static final String EE_COMPATIBLE_NAME = "eeCompatible";
    final static String INSTALLED_BUNDLE_CACHE = "platform/feature.bundles.cache";
    final static String FEATURE_DEF_CACHE_FILE = "platform/feature.cache";
    final static String FEATURE_FIX_CACHE_FILE = "feature.fix.cache";
    final static int FEATURE_FIX_CACHE_VERSION = 1;
    final static String FEATURE_TEST_FIXES = "IBM-Test-Fixes";
    final static String FEATURE_INTERIM_FIXES = "IBM-Interim-Fixes";
    final static String FEATURE_PRODUCT_EXTENSIONS_INSTALL = "com.ibm.websphere.productInstall";
    final static String FEATURE_PRODUCT_EXTENSIONS_FILE_EXTENSION = ".properties";
    final static String PRODUCT_INFO_STRING_OPEN_LIBERTY = "Open Liberty";
    final static FeatureResolver featureResolver = new FeatureResolverImpl();

    final static Collection<String> ALLOWED_ON_ALL_FEATURES = Arrays.asList("com.ibm.websphere.appserver.timedexit-1.0", "com.ibm.websphere.appserver.osgiConsole-1.0");
    final static Collection<String> ALL_ALLOWED_ON_CLIENT_FEATURES;
    static {
        Collection<String> temp = new ArrayList<String>();
        temp.addAll(FeatureDefinitionUtils.ALLOWED_ON_CLIENT_ONLY_FEATURES);
        temp.addAll(ALLOWED_ON_ALL_FEATURES);
        ALL_ALLOWED_ON_CLIENT_FEATURES = Collections.unmodifiableCollection(temp);
    }

    enum ProvisioningMode {
        CONTENT_REQUEST,
        FEATURES_REQUEST,
        INITIAL_PROVISIONING,
        UPDATE,
        REFRESH
    }

    static class FeatureChange {
        final RuntimeUpdateManager runtimeUpdateManager;
        final ProvisioningMode provisioningMode;
        String[] features;
        RuntimeUpdateNotification appForceRestart = null;
        RuntimeUpdateNotification featureBundlesResolved = null;
        RuntimeUpdateNotification featureUpdatesCompleted = null;

        FeatureChange(RuntimeUpdateManager runtimeUpdateManager, ProvisioningMode provisioningMode, String[] features) {
            this.runtimeUpdateManager = runtimeUpdateManager;
            this.provisioningMode = provisioningMode;
            this.features = features;
            if (provisioningMode == ProvisioningMode.UPDATE) {
                featureUpdatesCompleted = runtimeUpdateManager.createNotification(RuntimeUpdateNotification.FEATURE_UPDATES_COMPLETED);
            }
        }

        void createNotifications() {
            appForceRestart = runtimeUpdateManager.createNotification(RuntimeUpdateNotification.APP_FORCE_RESTART, true);
            featureBundlesResolved = runtimeUpdateManager.createNotification(RuntimeUpdateNotification.FEATURE_BUNDLES_RESOLVED);
        }

        Set<String> getFeaturesWithLowerCaseName(FeatureRepository featureRepo) {
            Set<String> lcnFeatures = new HashSet<String>();
            for (String feature : features) {
                ProvisioningFeatureDefinition f = featureRepo.getFeature(feature);
                if (f == null || f.getVisibility() == Visibility.PUBLIC) {
                    lcnFeatures.add(FeatureRepository.lowerFeature(feature));
                } else {
                    lcnFeatures.add(feature);
                }
            }

            return lcnFeatures;
        }
    }

    static final String featureGroup = "feature",
                    featureGroupUsr = "feature:usr",
                    bundleGroup = "bundle";

    /** Queue for making queries for and changes to installed features */
    protected final ConcurrentLinkedQueue<FeatureChange> featureChanges;

    /** Reference to active bundle context */
    BundleContext bundleContext;

    /** Injected WsLocationAdmin service */
    protected volatile WsLocationAdmin locationService = null;

    /** Injected java.util.concurrent.ExecutorService */
    protected ExecutorService executorService = null;

    /** Variable registry service. */
    protected VariableRegistry variableRegistry = null;

    protected RuntimeUpdateManager runtimeUpdateManager = null;

    /** Injected EventAdmin service */
    protected EventAdmin eventAdminService = null;

    /** Injected RegionDigraph service */
    private RegionDigraph digraph = null;

    /** Indicator of active thread/event/whatever performing feature updates */
    protected final ReentrantLock iAmUpdater;

    /** Cast reference to system bundle as framework start level service */
    protected FrameworkStartLevel fwStartLevel;

    /** Coordinator for initial provisioning / ShutdownHookListener */
    protected final InitialProvisioningListener initialProvisioningLatch = new InitialProvisioningListener();

    private static final AtomicLong featureUpdateNumber = new AtomicLong(0);

    /**
     * Used to specify whether to stop the server for problems encountered
     * during install/start of feature bundles
     */
    protected OnError onError;

    private final String CONFIG_PACKAGE_SERVER_CONFLICT = "package.server.conflict";
    private Boolean packageServerConflict = Boolean.FALSE;

    /** Cache for currently installed features (and for all information we know about features) */
    protected FeatureRepository featureRepository;

    /** Cache for currently installed feature bundles */
    protected BundleList bundleCache;

    /** ProvisioningMode to use for next updated() call */
    protected volatile ProvisioningMode provisioningMode;

    protected KernelFeaturesHolder kernelFeaturesHolder;

    /** Package inspector: tracks API/SPI packages for various resolver hooks */
    protected final PackageInspectorImpl packageInspector;

    /** Shutdown hook to ensure waiting operations are unblocked */
    protected final ShutdownHookManager shutdownHook;

    private BundleListener bundleOriginsListener = null;

    private EnumSet<ProcessType> supportedProcessTypes = null;
    private String processTypeString = null;

    /**
     * flag to prevent attempts to update after deactivation. Updates happen asynchronously and may be scheduled before deactivation
     * but executed after.
     */
    private volatile boolean deactivated;

    private volatile LibertyBootRuntime libertyBoot;

    private FrameworkWiring frameworkWiring;

    @Reference
    private volatile List<ServerReadyStatus> serverReadyChecks;

    private static final class KernelFeaturesHolder {

        private volatile Collection<ProvisioningFeatureDefinition> kernelFeatures;

        private final FeatureManager featureManager;

        private final ProvisioningMode initialMode;

        KernelFeaturesHolder(FeatureManager featureManager, ProvisioningMode initialMode) {
            this.featureManager = featureManager;
            this.initialMode = initialMode;
        }

        Collection<ProvisioningFeatureDefinition> getKernelFeatures() {
            if (kernelFeatures == null) {
                if (initialMode == ProvisioningMode.INITIAL_PROVISIONING) {
                    kernelFeatures = KernelFeatureDefinitionImpl.getKernelFeatures(featureManager.bundleContext, featureManager.locationService);
                } else {
                    kernelFeatures = KernelFeatureDefinitionImpl.getAllKernelFeatures(featureManager.bundleContext, featureManager.locationService);
                }
            }
            return kernelFeatures;
        }
    }

    /**
     * FeatureManager is instantiated by declarative services.
     */
    public FeatureManager() {
        this.featureChanges = new ConcurrentLinkedQueue<FeatureChange>();
        this.iAmUpdater = new ReentrantLock();
        packageInspector = new PackageInspectorImpl();
        shutdownHook = new ShutdownHookManager();
        shutdownHook.addShutdownHook();
        shutdownHook.addListener(initialProvisioningLatch);
    }

    /**
     * Activate the FeatureManager implementation. This method will be called by
     * OSGi Declarative Services implementation when the component is initially
     * activated and when changes to our configuration have occurred.
     *
     * @param componentContext
     *                             the OSGi DS context
     */
    @Activate()
    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        setSupportedProcessTypes(componentContext);
        bundleContext = componentContext.getBundleContext();
        Bundle systemBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
        fwStartLevel = systemBundle.adapt(FrameworkStartLevel.class);
        frameworkWiring = systemBundle.adapt(FrameworkWiring.class);
        packageInspector.activate(bundleContext);

        variableRegistry.addVariable(featureGroupUsr, WsLocationConstants.SYMBOL_USER_EXTENSION_DIR + "lib/features/");

        // Make sure the default and usr repositories are established
        // (kernel should have done this already).
        // true: use a cache, true: use msgs
        BundleRepositoryRegistry.initializeDefaults(locationService.getServerName(), true);

        WsResource bundleCacheFile = locationService.getServerWorkareaResource(INSTALLED_BUNDLE_CACHE);
        WsResource featureCacheFile = locationService.getServerWorkareaResource(FEATURE_DEF_CACHE_FILE);

        processProductExtensionsPropertiesFiles();

        featureRepository = new FeatureRepository(featureCacheFile, bundleContext);
        bundleCache = new BundleList(bundleCacheFile, this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "Feature Manager activated");

        if (ServerContentHelper.isServerContentRequest(bundleContext)) {
            provisioningMode = ProvisioningMode.CONTENT_REQUEST;
        } else if (ServerFeaturesHelper.isServerFeaturesRequest(bundleContext)) {
            provisioningMode = ProvisioningMode.FEATURES_REQUEST;
        } else {
            provisioningMode = ProvisioningMode.INITIAL_PROVISIONING;
        }

        kernelFeaturesHolder = new KernelFeaturesHolder(this, provisioningMode);

        //register the BundleOriginMonitor for tracking bundles installed by non-feature manager bundles
        bundleContext.addBundleListener((bundleOriginsListener = new BundleInstallOriginBundleListener(bundleContext)));
    }

    /**
     * @param componentContext
     * @return
     */
    private void setSupportedProcessTypes(ComponentContext componentContext) {
        String processTypeProp = componentContext.getBundleContext().getProperty("wlp.process.type");
        this.supportedProcessTypes = ProcessType.fromString(processTypeProp);
        this.processTypeString = processTypeProp;
    }

    private void processProductExtensionsPropertiesFiles() {
        Iterator<ProductExtensionInfo> productExtensions = ProductExtension.getProductExtensions().iterator();

        while (productExtensions.hasNext()) {
            ProductExtensionInfo prodExt = productExtensions.next();
            String featureType = prodExt.getName();
            String fileName = featureType + FEATURE_PRODUCT_EXTENSIONS_FILE_EXTENSION;
            // skip a file called just .properties
            if (0 != featureType.length()) {
                String featurePath = prodExt.getLocation();
                if (featurePath != null) {
                    // add / so it can be specified with or without a / in the .properties file
                    featurePath = featurePath + "/";
                    String normalPath = PathUtils.normalize(featurePath);
                    if (PathUtils.containsSymbol(normalPath)) {
                        Tr.error(tc, "PRODUCT_FEATURE_INSTALL_PATH_SYMBOL_ERROR", new Object[] { fileName, PathUtils.getSymbol(normalPath) });
                    } else {
                        WsResource location;
                        WsLocationAdmin wsLocationAdmin = locationService;
                        // verify path from properties file plus lib/features/ exists
                        if (PathUtils.pathIsAbsolute(normalPath)) {
                            location = wsLocationAdmin.resolveResource(featurePath + ProvisionerConstants.LIB_FEATURE_PATH);
                        } else {
                            location = wsLocationAdmin.resolveResource(WsLocationConstants.SYMBOL_INSTALL_PARENT_DIR + featurePath + ProvisionerConstants.LIB_FEATURE_PATH);
                            featurePath = WsLocationConstants.SYMBOL_INSTALL_PARENT_DIR + featurePath;
                        }
                        if (location != null && location.exists()) {
                            String installDir = wsLocationAdmin.resolveString(featurePath);
                            if (installDir.equalsIgnoreCase(wsLocationAdmin.resolveString(WsLocationConstants.SYMBOL_INSTALL_DIR))) {
                                Tr.error(tc, "PRODUCT_FEATURE_INSTALL_PATH_WLP_ERROR", new Object[] { fileName });
                            } else {
                                variableRegistry.addVariable("feature:" + featureType, featurePath + ProvisionerConstants.LIB_FEATURE_PATH);
                                // add product extension location so it be looked with say ${productextension.extension.dir}
                                wsLocationAdmin.addLocation(installDir, featureType + ".extension.dir");
                                ProductExtensionServiceFingerprint.putProductExtension(prodExt.getName(), installDir);
                                BundleRepositoryRegistry.addBundleRepository(installDir, featureType);
                            }
                        } else {
                            String installDir = wsLocationAdmin.resolveString(featurePath);
                            Tr.error(tc, "PRODUCT_FEATURE_INSTALL_PATH_ERROR", new Object[] { installDir, fileName });
                        }
                    }
                } else {
                    Tr.error(tc, "PRODUCT_FEATURE_PROPERTIES_FILE_ERROR", fileName);
                }
            }
        }
    }

    /**
     * Deactivate the FeatureManager service. This method will be called by the
     * OSGi Declarative Services implementation when the component is
     * deactivated. Deactivation will occur when the service configuration
     * needs to be refreshed, when the bundle is stopped, or when the DS
     * implementation is stopped.
     *
     * @param componentContext
     *                             the OSGi DS context
     */
    @Deactivate()
    @FFDCIgnore(InterruptedException.class)
    protected void deactivate(int reason) {
        // Wait at most 30s  to get the update lock before we try to
        // allow the feature manager to shutdown. This is a best-effort attempt
        // at avoiding IllegalStateExceptions and other errors due to provisioning
        // operations in flight while the server is stopping. There are other tests
        // in place in the Provisioner to "close up early" if the framework is stopping:
        // we should never have to wait the full 30s here.
        boolean lockObtained = false;
        try {
            lockObtained = iAmUpdater.tryLock(30, TimeUnit.SECONDS);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Feature Manager deactivated, update lock obtained=" + lockObtained, this);
        } catch (InterruptedException e) {
            // we timed out or were interrupted. This is a best effort, nothing horrible happens
            // other than some less-than-pleasant FFDCs if we proceed with stopping. More horrible would
            // be doing something that would tie up or deadlock the server. So. we will allow deactivate
            // to proceed!
        } finally {
            deactivated = true;
            if (lockObtained) // if we obtained the lock rather than timing out...
                iAmUpdater.unlock();
        }

        //remove the origins listener
        bundleContext.removeBundleListener(bundleOriginsListener);
        notifyFrameworkReady();
        packageInspector.deactivate();
    }

    /**
     * Inject a <code>WsLocationAdmin</code> service instance: dynamic
     * reference required by activator, inject directly.
     *
     * @param locationService
     *                            a location service
     */
    @Reference(name = "locationService", service = WsLocationAdmin.class)
    protected void setLocationService(WsLocationAdmin locationService) {
        this.locationService = locationService;
    }

    /**
     * Required <code>WsLocationAdmin</code> service instance.
     * Called to unset intermediate dynamic references or after
     * deactivate. Do nothing.
     *
     * @param locationService
     *                            a location service
     */
    protected void unsetLocationService(WsLocationAdmin locationService) {
    }

    public WsLocationAdmin getLocationService() {
        return locationService;
    }

    /**
     *
     */
    @Reference(name = "runtimeUpdateManager", service = RuntimeUpdateManager.class)
    protected void setRuntimeUpdateManager(RuntimeUpdateManager runtimeUpdateManager) {
        this.runtimeUpdateManager = runtimeUpdateManager;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setLibertyBoot(LibertyBootRuntime libertyBoot) {
        this.libertyBoot = libertyBoot;
    }

    public LibertyBootRuntime getLibertyBoot() {
        return libertyBoot;
    }

    protected void unsetLibertyBoot(LibertyBootRuntime libertyBoot) {
        if (this.libertyBoot == libertyBoot) {
            this.libertyBoot = null;
        }
    }

    /**
     *
     */
    protected void unsetRuntimeUpdateManager(RuntimeUpdateManager runtimeUpdateManager) {
    }

    /**
     * Inject a <code>EventAdmin</code> service instance.
     */
    @Reference(name = "eventAdminService", service = EventAdmin.class)
    protected void setEventAdminService(EventAdmin eventAdminService) {
        this.eventAdminService = eventAdminService;
    }

    /**
     * Required <code>EventAdmin</code> service instance.
     * Called to unset intermediate dynamic references or after
     * deactivate. Do nothing.
     */
    protected void unsetEventAdminService(EventAdmin eventAdminService) {
    }

    /**
     * Inject a <code>RegionDigraph</code> service instance.
     */
    @Reference(name = "digraph", service = RegionDigraph.class)
    protected void setDigraph(RegionDigraph digraph) {
        this.digraph = digraph;
    }

    /**
     * Returns the digraph used to manage installed bundles
     */
    RegionDigraph getDigraph() {
        return this.digraph;
    }

    /**
     * Required <code>RegionDigraph</code> service instance.
     * Called to unset intermediate dynamic references or after
     * deactivate. Do nothing.
     */
    protected void unsetDigraph(RegionDigraph digraph) {
    }

    /**
     * Inject an <code>ExecutorService</code> service instance.
     *
     * @param executorService
     *                            an executor service
     */
    @Reference(name = "executorService", service = ExecutorService.class)
    protected void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * Remove the <code>ExecutorService</code> service instance.
     *
     * @param executorService
     *                            an executor service
     */
    protected void unsetExecutorService(ExecutorService executorService) {
    }

    /**
     * Declarative Services method for setting the variable registry service implementation reference.
     *
     * @param ref reference to the service
     */
    @Reference(name = "variableRegistry", service = VariableRegistry.class)
    protected void setVariableRegistry(VariableRegistry variableRegistry) {
        this.variableRegistry = variableRegistry;
    }

    /**
     * Required <code>VariableRegistry</code> service instance.
     * Called to unset intermediate dynamic references or after
     * deactivate. Do nothing.
     */
    protected void unsetVariableRegistry(VariableRegistry variableRegistry) {
    }

    @Override
    public void updated(Dictionary<String, ?> configuration) throws ConfigurationException {
        final ProvisioningMode mode = provisioningMode;
        provisioningMode = ProvisioningMode.UPDATE;

        if (configuration == null) {
            if (mode != ProvisioningMode.UPDATE) {
                notifyFrameworkReady();
            }
            return;
        }

        onError = (OnError) configuration.get(OnErrorUtil.CFG_KEY_ON_ERROR);
        packageServerConflict = (Boolean) configuration.get(CONFIG_PACKAGE_SERVER_CONFLICT);
        if (packageServerConflict == null) {
            packageServerConflict = Boolean.FALSE;
        }

        String[] features = (String[]) configuration.get(CFG_KEY_ACTIVE_FEATURES);
        if (features == null) {
            features = new String[0];
        }

        queueFeatureChange(mode, features);
    }

    private void queueFeatureChange(final ProvisioningMode mode, String[] features) {
        featureChanges.add(new FeatureChange(runtimeUpdateManager, mode, features));
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    processFeatureChanges();
                } finally {
                    if (mode != ProvisioningMode.UPDATE) {
                        notifyFrameworkReady();
                    }
                }
            }
        });
    }

    private void notifyFrameworkReady() {
        initialProvisioningLatch.countDown();
        shutdownHook.removeListener(initialProvisioningLatch);
    }

    /** {@inheritDoc} */
    @Override
    public void waitForFrameworkReady() throws InterruptedException {
        initialProvisioningLatch.await();
    }

    protected void update(FeatureChange featureChange) throws IllegalStateException {
        featureChange.createNotifications();

        Set<String> preInstalledFeatures = Collections.emptySet();
        Set<String> deletedAutoFeatures = new HashSet<String>();
        Set<String> deletedPublicAutoFeatures = new HashSet<String>();

        // If we're refreshing the features, we want to identify which existing features are auto features, and also
        // which ones were public. This is because the feature manifests may have been removed, and when we re-initialise
        // the featureRepo, we'll lose the ability to work this information out.
        // We store the existing auto features, and public auto features in the variables, and later on in the method
        // we use these to identify which of these features, if any, have been deleted, .

        HashSet<String> preInstalledAutoFeatures = new HashSet<String>();
        HashSet<String> preInstalledPublicAutoFeatures = new HashSet<String>();

        try {
            switch (featureChange.provisioningMode) {
                case INITIAL_PROVISIONING:
                    // Get through kernel/core startup
                    if (getStartLevel() < ProvisionerConstants.LEVEL_FEATURE_PREPARE) {
                        BundleLifecycleStatus startStatus = setStartLevel(ProvisionerConstants.LEVEL_FEATURE_PREPARE);
                        checkBundleStatus(startStatus);
                    }
                    break;
                case REFRESH:
                    // Get all the installed features.
                    for (String featureName : featureRepository.getInstalledFeatures()) {
                        ProvisioningFeatureDefinition feature = featureRepository.getFeature(featureName);
                        // If the feature is not null and is an AutoFeature store it away.
                        if (feature != null && feature.isAutoFeature()) {
                            preInstalledAutoFeatures.add(feature.getFeatureName());
                            // If the autofeature is a public one, then add it to the public autofeature set.
                            if (feature.getVisibility() == Visibility.PUBLIC)
                                preInstalledPublicAutoFeatures.add(feature.getFeatureName());
                        }
                    }
                    break;
                case CONTENT_REQUEST:
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Liberty server being held in paused state for minify operation");
                    }
                    break;
                default:
                    break;
            }
            // Create and validate feature cache (will remove cached features that no longer match filesystem)
            featureRepository.init();

            // Read bundle cache
            bundleCache.init();

            if (tc.isInfoEnabled()) {
                Tr.info(tc, "STARTING_AUDIT");
            }

            preInstalledFeatures = new HashSet<>(featureRepository.getInstalledFeatures());

            String pkgs = bundleContext.getProperty("com.ibm.ws.kernel.classloading.apiPackagesToHide");
            Set<String> apiPkgsToIgnore = pkgs == null ? null : new HashSet<String>(Arrays.asList(pkgs.split(",")));
            Provisioner provisioner = new Provisioner(this, apiPkgsToIgnore);

            // If we are refreshing the features, then find which features have now been deleted.
            if (featureChange.provisioningMode == ProvisioningMode.REFRESH) {
                //Go through the list of installed autoFeatures.
                for (String featureName : preInstalledAutoFeatures) {
                    ProvisioningFeatureDefinition feature = featureRepository.getFeature(featureName);
                    // If the lookup of the feature returns null, the feature manifest must have been deleted.
                    // Add this to the list of deletedAutoFeatures, and if relevant, to the list of deleted public autofeatures.
                    if (feature == null) {
                        deletedAutoFeatures.add(featureName);
                        if (preInstalledPublicAutoFeatures.contains(featureName))
                            deletedPublicAutoFeatures.add(featureName);
                    }
                }
                // Install and provision the required features. We pass in all of the public non-autofeatures that have already
                // been installed into the runtime, so that we can recalculate any new autofeatures to install.
                featureChange.features = getPublicFeatures(preInstalledFeatures, false).toArray(new String[] {});
            }
            updateFeatures(locationService, provisioner, preInstalledFeatures, featureChange, featureUpdateNumber.incrementAndGet());
            // All done with the updates we could find...
            switch (featureChange.provisioningMode) {
                case CONTENT_REQUEST:
                    // perform the operation to identify all server content based on loaded features
                    new ServerContentHelper(bundleContext, this, locationService).processServerContentRequest();
                    break;

                case INITIAL_PROVISIONING:
                    // Increment the start level to ensure application bundles can start,
                    // even if no features are loaded
                    BundleLifecycleStatus startStatus = setStartLevel(ProvisionerConstants.LEVEL_ACTIVE);
                    checkBundleStatus(startStatus); // FFDC, etc.

                    checkServerReady();

                    //register a service that can be looked up for server start.
                    // Need a two phase approach, since ports will be opened for listening on the first phase
                    bundleContext.registerService(ServerStarted.class, new ServerStarted() {
                    }, null);

                    // components which needed to wait till ports were opened for listening need to wait till Phase2
                    bundleContext.registerService(ServerStartedPhase2.class, new ServerStartedPhase2() {
                    }, null);

                    break;
                default:
                    break;
            }
        } finally {
            // Now that the provisioning operation has finished, we need to clean up
            // all the caches and release resources so GC can reclaim them.
            // While these do write to cache files, none of them should allow an exception
            // to escape (i.e. they should be mindful that they're being called in a
            // finally block). We'll wrap these in a try/finally to ensure the lock
            // is always unlocked, even if one of these blows up.

            // clean up bundle repositories
            BundleRepositoryRegistry.disposeAll();
            // clean up kernel features
            KernelFeatureDefinitionImpl.dispose();
            // Clean up anything we can from bundle/feature caches
            bundleCache.dispose();
            featureRepository.dispose();

            // Update/progress messages -- AFTER we've written cache files
            writeUpdateMessages(featureChange.provisioningMode, preInstalledFeatures, deletedAutoFeatures, deletedPublicAutoFeatures);
        }
    }

    private void checkServerReady() {
        serverReadyChecks.forEach(ServerReadyStatus::check);
    }

    public void queryServer(FeatureChange featureChange) throws IllegalStateException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Liberty server being held in paused state for query operation");
        }

        try {
            // Create and validate feature cache (will remove cached features that no longer match filesystem)
            featureRepository.init();
            // Read bundle cache
            bundleCache.init();

            if (tc.isInfoEnabled()) {
                Tr.info(tc, "STARTING_AUDIT");
            }

            // perform the operation to identify all server content based on loaded features
            new ServerFeaturesHelper(bundleContext).processServerFeaturesRequest(this.resolveFeatures(featureChange));

            // Complete any pending notifications
            if (featureChange.featureUpdatesCompleted != null) {
                featureChange.featureUpdatesCompleted.setResult(true);
            }
            if (featureChange.appForceRestart != null) {
                featureChange.appForceRestart.setResult(true);
            }
            if (featureChange.featureBundlesResolved != null) {
                featureChange.featureBundlesResolved.setResult(true);
            }
        } finally {
            writeServiceMessages();
        }
    }

    protected void processFeatureChanges() {
        // If this thread is the the updater (changed the value from no to yes), then
        // a) increment the start level to FEATURE_PREPARE (our constant)
        // b) poll the queue of updates: apply changes to set of enabled features as units
        boolean retry;
        do {
            retry = false;
            if (iAmUpdater.tryLock()) { // try to obtain the lock

                if (deactivated) {
                    featureChanges.clear();
                    return;
                }
                long startTime = System.nanoTime();

                FeatureChange featureChange = featureChanges.poll();
                if (featureChange == null) {
                    return;
                }
                try {

                    switch (featureChange.provisioningMode) {
                        case FEATURES_REQUEST:
                            queryServer(featureChange);
                            break;
                        default:
                            update(featureChange);
                            break;
                    }

                } catch (IllegalStateException ies) {
                    // Bundle/framework shutdown.. nothing can be done
                    // Empty out whatever that remains in the queue.
                    featureChanges.clear();
                } catch (Exception e) {
                    // Catch for FFDC: rethrowing would just go off into framework oblivion.
                } finally {
                    try {
                        writeFeatureChangeMessages(startTime, featureChange.provisioningMode);
                    } finally {
                        // Mark that we are not the updater any more.
                        iAmUpdater.unlock();
                    }
                }

                // If another thread tried but failed to become updater after we exited
                // the while(update != null) loop it's possible that there's still work
                // sitting on the queue. If we return now it may wait there indefinitely,
                // so try again.
                if (!featureChanges.isEmpty()) {
                    retry = true;
                }
            }
        } while (retry);
    }

    /**
     *
     * @param provisioningMode
     * @param preInstalledFeatures
     * @param deletedAutoFeatures       - The list of deleted AutoFeatures.This is used to trace which auto features have been deleted.
     * @param deletedPublicAutoFeatures - The list of deleted Public AutoFeatures.This is used to issue to the console which public
     *                                      auto features have been deleted.
     */
    private void writeUpdateMessages(ProvisioningMode provisioningMode, Set<String> preInstalledFeatures, Set<String> deletedAutoFeatures,
                                     Set<String> deletedPublicAutoFeatures) {
        writeServiceMessages();

        Set<String> postInstalledFeatures = new HashSet<>(featureRepository.getInstalledFeatures());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "all installed features " + postInstalledFeatures);
        }

        //remove the pre-installed features from all installed features to show just the added features
        postInstalledFeatures.removeAll(preInstalledFeatures);
        Set<String> installedPublicFeatures = Collections.emptySet();

        if (!!!postInstalledFeatures.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "added features", postInstalledFeatures);
            }
            installedPublicFeatures = getPublicFeatures(postInstalledFeatures, true);
        } else if (provisioningMode == ProvisioningMode.INITIAL_PROVISIONING) {
            // this is a case of warm start, just audit the installed features to be useful
            installedPublicFeatures = getPublicFeatures(preInstalledFeatures, true);
        }

        if (!!!installedPublicFeatures.isEmpty()) {
            if (supportedProcessTypes.contains(ProcessType.CLIENT)) {
                Tr.audit(tc, "FEATURES_ADDED_CLIENT", installedPublicFeatures);
            } else {
                Tr.audit(tc, "FEATURES_ADDED", installedPublicFeatures);
            }
        }

        featureRepository.copyInstalledFeaturesTo(postInstalledFeatures);
        preInstalledFeatures.removeAll(postInstalledFeatures);

        // Add in any deleted autofeatures to the trace.
        preInstalledFeatures.addAll(deletedAutoFeatures);
        if (!!!preInstalledFeatures.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "removed features", preInstalledFeatures);
            }

            Set<String> publicFeatures = getPublicFeatures(preInstalledFeatures, true);
            // Add in any deleted public autofeatures to the trace.
            publicFeatures.addAll(deletedPublicAutoFeatures);
            if (!!!publicFeatures.isEmpty()) {
                Tr.audit(tc, "FEATURES_REMOVED", publicFeatures);
            }
        }
    }

    /**
     *
     * @param startTime
     * @param provisioningMode
     */
    private void writeFeatureChangeMessages(long startTime, ProvisioningMode provisioningMode) {
        String time = TimestampUtils.getElapsedTimeNanos(startTime);

        if (provisioningMode == ProvisioningMode.UPDATE) {
            Tr.audit(tc, "COMPLETE_AUDIT", time);
        } else {
            if (tc.isInfoEnabled()) {
                Tr.info(tc, "COMPLETE_AUDIT", time);
            }
            if (provisioningMode == ProvisioningMode.CONTENT_REQUEST) {
                Tr.audit(tc, "SERVER_MINIFY", locationService.getServerName());
            } else if (provisioningMode == ProvisioningMode.FEATURES_REQUEST) {
                Tr.audit(tc, "SERVER_GATHER_FEATURES", locationService.getServerName());
            } else {
                if (supportedProcessTypes.contains(ProcessType.CLIENT)) {
                    Tr.audit(tc, "CLIENT_STARTED", locationService.getServerName());
                } else {
                    Tr.audit(tc, "SERVER_STARTED", locationService.getServerName(), TimestampUtils.getElapsedTime());
                }
            }
        }
    }

    @FFDCIgnore(IOException.class)
    public PrintStream getFixWriter(PrintStream out) {
        if (out == null) {
            WsLocationAdmin locAdmin = locationService;
            if (locAdmin != null) {
                WsResource fixData = locAdmin.getServerWorkareaResource("platform/fix.data");
                try {
                    out = new PrintStream(fixData.putStream());
                } catch (IOException e) {
                    // ignore this because we write this for service.
                }
            }
        }

        return out;
    }

    /**
     *
     */
    @FFDCIgnore(IllegalStateException.class)
    private void writeServiceMessages() {
        // This print stream is used to write a fix.data file containing detected fixes for service.
        PrintStream out = null;
        Map<Bundle, Map<String, String>> cachedFixes = new HashMap<>();
        boolean dirtyFixCache = readCachedFixes(cachedFixes);

        Bundle[] bundles = bundleContext.getBundles();

        Set<String> iFixSet = new HashSet<String>();
        Set<String> tFixSet = new HashSet<String>();

        for (Bundle b : bundles) {
            String tFixes;
            boolean hasTFixes = false;
            String iFixes;
            boolean hasIFixes = false;
            Map<String, String> cachedHeaders = cachedFixes.get(b);
            if (cachedHeaders == null) {
                Dictionary<String, String> headers;
                try {
                    headers = b.getHeaders("");
                } catch (IllegalStateException ise) {
                    // This can happen if a bundle was uninstalled between the call to bundleContext.getBundles() and here.
                    // Testing shows this typically happens to dynamically generated bundles, like
                    // "WSClassLoadingService@Thread Context:WebModule:basicauth-basicauth-/basicauth"
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "writeServiceMessages - caught exception getting manifest headers for bundle " + b, ise);
                    }
                    continue;
                }

                cachedHeaders = new HashMap<>(2);
                tFixes = headers.get(FEATURE_TEST_FIXES);
                hasTFixes = tFixes != null;
                if (hasTFixes) {
                    cachedHeaders.put(FEATURE_TEST_FIXES, tFixes);
                }
                iFixes = headers.get(FEATURE_INTERIM_FIXES);
                hasIFixes = iFixes != null;
                if (hasIFixes) {
                    cachedHeaders.put(FEATURE_INTERIM_FIXES, iFixes);
                }

                cachedFixes.put(b, cachedHeaders);
                dirtyFixCache = true;
            } else {
                tFixes = cachedHeaders.get(FEATURE_TEST_FIXES);
                hasTFixes = tFixes != null;
                iFixes = cachedHeaders.get(FEATURE_INTERIM_FIXES);
                hasIFixes = iFixes != null;
            }

            if (hasTFixes) {
                out = getFixWriter(out);
                out.print("tFix: ");
                out.print(b.getLocation());
                out.print(": ");
                out.println(tFixes);
                tFixSet.addAll(Arrays.asList(tFixes.split("[,\\s]")));
            }
            if (hasIFixes) {
                out = getFixWriter(out);
                out.print("iFix: ");
                out.print(b.getLocation());
                out.print(": ");
                out.println(iFixes);
                iFixSet.addAll(Arrays.asList(iFixes.split("[,\\s]")));
            }
        }

        if (!!!iFixSet.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String fix : iFixSet) {
                if (!"".equals(fix)) {
                    builder.append(',');
                    builder.append(fix);
                }
            }
            builder.deleteCharAt(0);
            String fixes = builder.toString();
            Tr.audit(tc, "INTERIM_FIX_DETECTED", fixes);
        }
        if (!!!tFixSet.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String fix : tFixSet) {
                if (!"".equals(fix)) {
                    builder.append(',');
                    builder.append(fix);
                }
            }
            builder.deleteCharAt(0);
            String fixes = builder.toString();
            Tr.warning(tc, "TEST_FIX_DETECTED", fixes);
        }

        if (out != null) {
            out.flush();
            out.close();
        }
        if (dirtyFixCache) {
            writeCachedFixes(cachedFixes);
        }
    }

    /**
     * @param cachedFixes
     */
    private void writeCachedFixes(Map<Bundle, Map<String, String>> cachedFixes) {
        File cache = bundleContext.getDataFile(FEATURE_FIX_CACHE_FILE);
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cache)))) {
            out.writeInt(FEATURE_FIX_CACHE_VERSION);
            out.writeInt(cachedFixes.size());
            for (Entry<Bundle, Map<String, String>> bFixes : cachedFixes.entrySet()) {
                out.writeLong(bFixes.getKey().getBundleId());
                out.writeLong(bFixes.getKey().getLastModified());

                writeFixHeader(FEATURE_TEST_FIXES, bFixes.getValue(), out);
                writeFixHeader(FEATURE_INTERIM_FIXES, bFixes.getValue(), out);
            }
        } catch (IOException e) {
            // auto FFDC is fine here
        }
    }

    private void writeFixHeader(String fixHeader, Map<String, String> headers, DataOutputStream out) throws IOException {
        String fixes = headers.get(fixHeader);
        boolean hasFixes = fixes != null;
        out.writeBoolean(hasFixes);
        if (hasFixes) {
            out.writeUTF(fixes);
        }
    }

    private boolean readCachedFixes(Map<Bundle, Map<String, String>> result) {
        BundleContext systemContext = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
        boolean dirty = false;
        File cache = bundleContext.getDataFile(FEATURE_FIX_CACHE_FILE);
        if (cache.isFile()) {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(cache)))) {
                if (in.readInt() != FEATURE_FIX_CACHE_VERSION) {
                    // don't understand the cache version; start with empty dirty cache
                    return true;
                }
                int numBundles = in.readInt();
                for (int i = 0; i < numBundles; i++) {
                    long id = in.readLong();
                    long lastModified = in.readLong();
                    String tFixes = readFixHeader(in);
                    String iFixes = readFixHeader(in);

                    boolean hasTFixes = tFixes != null;
                    boolean hasIFixes = iFixes != null;
                    Bundle b = systemContext.getBundle(id);
                    if (b != null && b.getLastModified() == lastModified) {
                        if (!hasTFixes && !hasIFixes) {
                            // common case; no fix headers in bundle
                            result.put(b, Collections.<String, String> emptyMap());
                        } else {
                            Map<String, String> bFixes = new HashMap<>(2);
                            if (hasTFixes) {
                                bFixes.put(FEATURE_TEST_FIXES, tFixes);
                            }
                            if (hasIFixes) {
                                bFixes.put(FEATURE_INTERIM_FIXES, iFixes);
                            }
                            result.put(b, bFixes);
                        }
                    } else {
                        // missing or updated bundle; make dirty to ensure the cache is rewritten
                        dirty = true;
                    }
                }
            } catch (IOException e) {
                // auto FFDC is fine here
            }
        } else {
            dirty = true;
        }
        return dirty;
    }

    private String readFixHeader(DataInputStream in) throws IOException {
        return in.readBoolean() ? in.readUTF() : null;
    }

    /**
     * @param postInstalledFeatures
     */
    private Set<String> getPublicFeatures(Set<String> postInstalledFeatures, boolean includeAutoFeatures) {
        Set<String> publicFeatures = new TreeSet<String>();
        Iterator<String> it = postInstalledFeatures.iterator();
        while (it.hasNext()) {
            String feature = it.next();
            FeatureDefinition fd = getFeatureDefinition(feature);

            if (fd != null && fd.getVisibility() == Visibility.PUBLIC) {
                // get name from feature definition.
                // input ones come from the cache which is lower case.
                // If we don't want to include auto features, then check each feature before adding it.
                if (!includeAutoFeatures) {
                    if (fd instanceof ProvisioningFeatureDefinition) {
                        if (!((ProvisioningFeatureDefinition) fd).isAutoFeature())
                            publicFeatures.add(fd.getFeatureName());
                    } else {
                        // If we're not an instance of ProvisioningFeatureDefinition then add the feature to the list.
                        publicFeatures.add(fd.getFeatureName());
                    }
                } else {
                    publicFeatures.add(fd.getFeatureName());
                }
            }
        }
        return publicFeatures;
    }

    private Result resolveFeatures(FeatureChange featureChange) {
        // In 850 we were not case sensitive so we need to stay that way.
        // Use a set to eliminate duplicates.
        Set<String> newConfiguredFeatures = featureChange.getFeaturesWithLowerCaseName(featureRepository);

        return resolveFeatures(newConfiguredFeatures, new ArrayList<String>(), featureChange.provisioningMode);
    }

    private Result resolveFeatures(Set<String> rootFeatures, Collection<String> restrictedAccessAttempts, ProvisioningMode mode) {

        if (rootFeatures.isEmpty() && featureRepository.emptyFeatures()) {
            Tr.warning(tc, "EMPTY_FEATURES_WARNING");
        }
        Repository restrictedRespository;
        Collection<String> restrictedRepoAccessAttempts = new ArrayList<String>();
        boolean allowMultipleVersions = false;
        boolean featureListIsComplete = false;
        boolean currentPackageServerConflict = false;
        if (ProvisioningMode.CONTENT_REQUEST == mode || ProvisioningMode.FEATURES_REQUEST == mode) {
            // allow multiple versions if in minify (TODO strange since we are minifying!)
            // For feature request using the minified approach but that could cause additional singletons to be provisioned.
            allowMultipleVersions = Boolean.getBoolean("internal.minify.ignore.singleton");
            // For packaging purposes; have an option that just blindly takes
            // the feature list and uses it without using the resolver.
            // This feature list may include public, protected, private and auto-features.
            // No additional feature resolution will be done to pull in additional required features.
            // The feature list is expected to be complete.
            featureListIsComplete = Boolean.getBoolean("internal.minify.feature.list.complete");
            // do not restrict any features
            restrictedRespository = featureRepository;
            currentPackageServerConflict = packageServerConflict;
        } else {
            if (supportedProcessTypes.contains(ProcessType.CLIENT)) {
                // do not restrict any features while resolving, but ....
                restrictedRespository = featureRepository;
                // do filter out any root features that should not be allowed in the client
                for (Iterator<String> iRootFeatures = rootFeatures.iterator(); iRootFeatures.hasNext();) {
                    ProvisioningFeatureDefinition rootDef = featureRepository.getFeature(iRootFeatures.next());
                    if (rootDef != null && !!!ALL_ALLOWED_ON_CLIENT_FEATURES.contains(rootDef.getSymbolicName())) {
                        // update the restrictedAccessAttempts list to report the error
                        restrictedRepoAccessAttempts.add(rootDef.getSymbolicName());
                        // remove the restricted root feature so it is not loaded
                        iRootFeatures.remove();
                    }
                }
            } else {
                // in server process; must restrict the client features
                RestrictedFeatureRespository temp = new RestrictedFeatureRespository(featureRepository, FeatureDefinitionUtils.ALLOWED_ON_CLIENT_ONLY_FEATURES);
                restrictedRepoAccessAttempts = temp.getRestrictedFeatureAttempts();
                restrictedRespository = temp;
            }
        }
        Result result;
        if (featureListIsComplete) {
            result = createResultFromCompleteList(restrictedRespository, rootFeatures);
        } else {
            result = callFeatureResolver(restrictedRespository, kernelFeaturesHolder.getKernelFeatures(), rootFeatures, allowMultipleVersions, currentPackageServerConflict);
        }
        restrictedAccessAttempts.addAll(restrictedRepoAccessAttempts);
        return result;
    }

    private Result callFeatureResolver(Repository restrictedRespository, Collection<ProvisioningFeatureDefinition> kernelFeatures, Set<String> rootFeatures,
                                       boolean allowMultipleVersions, boolean currentPackageServerConflict) {

        // short circuit if package server is expecting conflicts
        if (currentPackageServerConflict) {
            return featureResolver.resolveFeatures(restrictedRespository, kernelFeatures, rootFeatures, Collections.<String> emptySet(), true);
        }
        // resolve the features
        // TODO Note that we are just supporting all types at runtime right now.  In the future this may be restricted by the actual running process type
        Result result = featureResolver.resolveFeatures(restrictedRespository, kernelFeatures, rootFeatures, Collections.<String> emptySet(),
                                                        false);
        if (allowMultipleVersions) {
            if (!result.getConflicts().isEmpty()) {
                result = featureResolver.resolveFeatures(restrictedRespository, kernelFeatures, rootFeatures, Collections.<String> emptySet(), true);
            }
        }

        return result;
    }

    private Result createResultFromCompleteList(Repository repository, Set<String> rootFeatures) {
        final Set<String> missing = new HashSet<>();
        final Set<String> resolved = new LinkedHashSet<>();

        for (String featureName : rootFeatures) {
            ProvisioningFeatureDefinition feature = repository.getFeature(featureName);
            if (feature == null) {
                missing.add(featureName);
            } else {
                resolved.add(feature.getFeatureName());
            }
        }

        return new Result() {

            @Override
            public boolean hasErrors() {
                return !getMissing().isEmpty();
            }

            @Override
            public Map<String, Chain> getWrongProcessTypes() {
                return Collections.emptyMap();
            }

            @Override
            public Set<String> getResolvedFeatures() {
                return resolved;
            }

            @Override
            public Set<String> getNonPublicRoots() {
                return Collections.emptySet();
            }

            @Override
            public Set<String> getMissing() {
                return missing;
            }

            @Override
            public Map<String, Collection<Chain>> getConflicts() {
                return Collections.emptyMap();
            }
        };
    }

    /**
     * Update installed features and bundles
     *
     * @param locService           Location service used to resolve resources (feature definitions or bundles)
     * @param provisioner          Provisioner for installing/starting bundles
     * @param preInstalledFeatures
     * @param newFeatureSet        New/revised list of active features
     * @return true if no errors occurred during the update, false otherwise
     */
    @FFDCIgnore(Throwable.class)
    protected boolean updateFeatures(WsLocationAdmin locService,
                                     Provisioner provisioner,
                                     Set<String> preInstalledFeatures,
                                     FeatureChange featureChange,
                                     long sequenceNumber) {
        // NOTE RE: FFDCIgnore above-- The catch block for Throwable below, stores
        // the exception in an InstallStatus object and calls FFDC at a more appropriate time.
        BundleList newBundleList = null;

        // In 850 we were not case sensitive so we need to stay that way.
        // Use a set to eliminate duplicates.
        Set<String> newConfiguredFeatures = featureChange.getFeaturesWithLowerCaseName(featureRepository);

        if (newConfiguredFeatures.isEmpty() && featureRepository.emptyFeatures()) {

            //We instantiate a new BundleList because we want to make sure we
            //go into the code section below that clean any extra bundles. See
            //defect 43287 for more information.
            newBundleList = new BundleList(this);
        }

        BundleInstallStatus installStatus = new BundleInstallStatus();
        BundleLifecycleStatus startStatus = null;

        List<Bundle> installedBundles = new ArrayList<Bundle>();

        boolean continueOnError = onError != OnError.FAIL;

        Set<String> goodFeatures = null;

        boolean featuresHaveChanges = true;
        boolean appForceRestartSet = false;
        final boolean sameJavaSpecVersion = sameJavaSpecVersion();
        try {
            if (areConfiguredFeaturesGood(newConfiguredFeatures) && sameJavaSpecVersion) {
                featuresHaveChanges = false;
                goodFeatures = preInstalledFeatures;
            } else {
                // This will be populated by resolveFeatures if there are any restricted access attempts during resolution
                Collection<String> restrictedAccessAttempts = new ArrayList<String>();

                Result result = resolveFeatures(newConfiguredFeatures, restrictedAccessAttempts, featureChange.provisioningMode);
                boolean reportedConfigurationErrors = reportErrors(result, restrictedAccessAttempts, newConfiguredFeatures, installStatus);
                goodFeatures = result.getResolvedFeatures();

                // If the final list of good features matches the currently installed features, we don't need to do anything else.
                // NOTE: we need to recompute the bundleCache if the java spec version has changed since last launch
                if (!sameJavaSpecVersion || !featureRepository.featureSetEquals(goodFeatures)) {

                    if (installStatus.canContinue(continueOnError)) {

                        if (newBundleList == null) {
                            newBundleList = new BundleList(this);
                        }
                        // now load the bundles for the resolved features
                        for (String featureName : goodFeatures) {
                            ProvisioningFeatureDefinition fdefinition = featureRepository.getFeature(featureName);
                            if (fdefinition != null) {
                                newBundleList.addAll(fdefinition, this);
                            }
                        }
                        // Add any missing bundles. We may not have any new bundles to add, as we may have just removed some
                        // features.
                        bundleCache.addAllNoReplace(newBundleList);

                        // Update installedFeatures with the features that were successfully added
                        featureRepository.setInstalledFeatures(goodFeatures, newConfiguredFeatures, reportedConfigurationErrors);
                    }
                }
            }
            if (featureChange.appForceRestart != null) {
                final Set<String> featureSet = featureRepository.getInstalledFeatures();
                if (featureChangesRequireRestart(preInstalledFeatures, featureSet)) {
                    featureChange.appForceRestart.setResult(true);
                    appForceRestartSet = true;
                    RuntimeUpdateNotification applicationsStopped = runtimeUpdateManager.getNotification(RuntimeUpdateNotification.APPLICATIONS_STOPPED);
                    if (applicationsStopped != null) {
                        applicationsStopped.waitForCompletion();
                    }
                } else {
                    featureChange.appForceRestart.setResult(false);
                    appForceRestartSet = true;
                }
            }

            if (installStatus.canContinue(continueOnError)) {
                Set<String> regionsToRemove = Collections.emptySet();
                // do not install bundles for minify operation
                if (featureChange.provisioningMode != ProvisioningMode.CONTENT_REQUEST) {
                    //populate the SPI resolver hooks with the new feature info
                    if (featuresHaveChanges) {
                        // only need this if features have changed
                        packageInspector.populateSPIInfo(bundleContext, this);
                        regionsToRemove = provisioner.createAndUpdateProductRegions();
                    }

                    // always do the install bundle operation because it associates bundles with refeature resources
                    // TODO would be good if we could avoid this when features have not changed.
                    provisioner.installBundles(bundleContext,
                                               bundleCache,
                                               installStatus,
                                               ProvisionerConstants.LEVEL_FEATURE_SERVICES - ProvisionerConstants.PHASE_INCREMENT,
                                               ProvisionerConstants.LEVEL_FEATURE_CONTAINERS,
                                               fwStartLevel.getInitialBundleStartLevel(),
                                               locService);
                    // add all installed bundles to list of bundlesToStart.
                    // TODO would be good if we could avoid this when features have not changed, but in
                    // some scenarios, the framework may reinstall a features bundle even on a warm restart,
                    // which would leave the bundle in INSTALLED state (see issue #2081).
                    if (installStatus.contextIsValid() && installStatus.bundlesToStart()) {
                        installedBundles.addAll(installStatus.getBundlesToStart());
                    }
                }

                featureRepository.updateServices();

                if (featuresHaveChanges && featureChange.provisioningMode != ProvisioningMode.CONTENT_REQUEST) {
                    // Uninstall extra bundles.
                    // Important to test for null here, and not "!isEmpty()":
                    // if all features were removed, the "newBundles" list would be empty, and all
                    // previously installed bundles would be "extra"
                    if (installStatus.contextIsValid() && newBundleList != null) {
                        BundleList remove = bundleCache.findExtraBundles(newBundleList, this);
                        if (remove != null && !remove.isEmpty()) {
                            provisioner.uninstallBundles(bundleContext, remove, installStatus, shutdownHook);
                        }
                    }

                    // Refresh any feature bundles that need it
                    // This happens when API end up getting rewired from the system bundle
                    // to a newly enable feature that provides the API instead
                    provisioner.refreshFeatureBundles(packageInspector, bundleContext, shutdownHook);

                    // Time to clean up regions that should be removed
                    provisioner.removeStaleProductRegions(regionsToRemove);

                    // refresh any gateway bundles that may need it.
                    provisioner.refreshGatewayBundles(shutdownHook);

                }
            }
        } catch (Throwable t) {
            // we failed for some other reason..
            installStatus.addOtherException(t);
            if (!appForceRestartSet && featureChange.appForceRestart != null) {
                featureChange.appForceRestart.setResult(t);
            }
        }

        boolean status = checkInstallStatus(installStatus);

        // Make sure bundles are ready to start
        provisioner.resolveBundles(bundleContext, installedBundles);

        if (featureChange.featureBundlesResolved != null) {
            Map<String, Object> props = new HashMap<String, Object>(1);
            props.put(RuntimeUpdateNotification.INSTALLED_BUNDLES_IN_UPDATE, installStatus.getBundlesAddedDelta());
            props.put(RuntimeUpdateNotification.REMOVED_BUNDLES_IN_UPDATE, installStatus.getBundlesRemovedDelta());
            featureChange.featureBundlesResolved.setProperties(props);
            featureChange.featureBundlesResolved.setResult(true);
            RuntimeUpdateNotification featureBundlesProcessed = runtimeUpdateManager.getNotification(RuntimeUpdateNotification.FEATURE_BUNDLES_PROCESSED);
            if (featureBundlesProcessed != null) {
                featureBundlesProcessed.waitForCompletion();
            }
        }

        // Analyze unresolved bundles for missing java dependencies
        analyzeUnresolvedBundles(installedBundles, goodFeatures);

        startStatus = provisioner.preStartBundles(installedBundles);
        status &= checkBundleStatus(startStatus);

        if (featureChange.featureUpdatesCompleted != null) {
            featureChange.featureUpdatesCompleted.setResult(true);
        }

        //post the updated feature list to EventAdmin
        if (eventAdminService != null) {
            Map<String, Object> eventProps = new HashMap<String, Object>(2);
            final Set<String> featureSet = featureRepository.getInstalledFeatures();
            eventProps.put("features", featureSet.toArray(new String[featureSet.size()]));
            eventProps.put("sequenceNumber", Long.valueOf(sequenceNumber));
            Event e = new Event("com/ibm/ws/kernel/feature/internal/FeatureManager/FEATURE_CHANGE", eventProps);
            eventAdminService.postEvent(e);
        }

        return status;
    }

    private boolean sameJavaSpecVersion() {
        return Objects.equals(JavaInfo.majorVersion(), bundleCache.getJavaSpecVersion());
    }

    /**
     * @param newConfiguredFeatures
     * @return
     */
    private boolean areConfiguredFeaturesGood(Set<String> newConfiguredFeatures) {
        if (!!!featureRepository.isDirty() && !!!featureRepository.hasConfigurationError() && featureRepository.getConfiguredFeatures().equals(newConfiguredFeatures)) {
            // check that all installed features are still installed
            for (String installedFeature : featureRepository.getInstalledFeatures()) {
                if (featureRepository.getFeature(installedFeature) == null) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Analyze all unresolved bundles for a missing java dependency.
     *
     * A bundle can specify a require java execution environment via the
     * <code>Bundle-RequiredExecutionEnvironment</code> header or via a
     * <code>Requires-Capability</code> header.
     *
     * Display an error message listed which features did not resolve,
     * and the unsatisfied java version dependency.
     *
     * @param installedBundles A list of the currently installed bundles
     * @param features         A list of the currently installed features
     */
    private void analyzeUnresolvedBundles(List<Bundle> installedBundles, Set<String> features) {

        final String m = "analyzeUnresolvedBundles";

        Set<Bundle> unresolvedBundles = getUnresolvedBundles(installedBundles);

        if (unresolvedBundles.isEmpty()) {
            return; // nothing to analyze
        }

        Map<String, Set<String>> javaVersiontoFeatureMap = new HashMap<String, Set<String>>();

        for (Bundle bundle : unresolvedBundles) {
            BundleRevision revision = bundle.adapt(BundleRevision.class);
            // may be null if the bundle got uninstalled
            if (revision != null) {
                List<BundleRequirement> eeReqs = revision.getDeclaredRequirements(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
                for (BundleRequirement eeReq : eeReqs) {
                    Collection<BundleCapability> eeCaps = frameworkWiring.findProviders(eeReq);
                    if (eeCaps.isEmpty()) {
                        // Bundle is missing its required EE.
                        String javaSEVersion = getJavaSEValue(eeReq);
                        Bundle b = eeReq.getResource().getBundle();
                        if (javaSEVersion != null) {
                            // This entry(bundle) has an unsatisfied java version dependency, find the features that include it
                            Set<String> foundInFeatures = findIncludingFeatures(features, b);
                            if (javaVersiontoFeatureMap.containsKey(javaSEVersion)) {
                                javaVersiontoFeatureMap.get(javaSEVersion).addAll(foundInFeatures);
                            } else {
                                javaVersiontoFeatureMap.put(javaSEVersion, foundInFeatures);
                            }
                        }
                    }
                }
            }
        }

        // Report the list of features that failed the java version dependency check
        for (Entry<String, Set<String>> javaSEEntry : javaVersiontoFeatureMap.entrySet()) {
            for (String feature : javaSEEntry.getValue()) {
                Tr.error(tc, "FEATURE_JAVA_LEVEL_NOT_MET_ERROR", feature, javaSEEntry.getKey());
                featureRepository.removeInstalledFeature(feature);
            }
        }

        // Stop the framework (if requested) if we have any features that failed the java version dependency check
        if (!javaVersiontoFeatureMap.isEmpty() && (onError.equals(OnError.FAIL))) {
            Throwable t = new IllegalArgumentException("Unresolved feature java dependencies: " + javaVersiontoFeatureMap);
            FFDCFilter.processException(t, ME, m);
            shutdownFramework();
        }

    }

    /**
     * Returns all of the unresolved bundles.
     *
     * @param installedBundles A list of the currently installed bundles
     * @return A list of all of the unresolved bundles, from the installedBundles list
     */
    private Set<Bundle> getUnresolvedBundles(List<Bundle> installedBundles) {

        Set<Bundle> unresolvedBundles = new HashSet<Bundle>();

        if (installedBundles == null || installedBundles.isEmpty()) {
            return Collections.emptySet();
        }

        for (Bundle b : installedBundles) {
            if (b.getState() == org.osgi.framework.Bundle.INSTALLED) {
                unresolvedBundles.add(b);
            }
        }

        return unresolvedBundles;

    }

    /**
     * Read the value JavaSE version (if present) from a Requirement with an osgi.ee entry
     *
     * @param req The Requirement to read the Java value from
     * @return The JavaSE value from an osgi.ee entry
     */
    private String getJavaSEValue(Requirement req) {

        String javaSEValue = null;
        String filterString = req.getDirectives().get(org.osgi.framework.Constants.FILTER_DIRECTIVE);

        Filter filter = null;
        try {
            filter = FrameworkUtil.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            return null;
        }
        Map<String, Object> matchAttrs = new HashMap<String, Object>();
        EEValue eeValue = EEValue.getInstance();
        VersionValue versionValue = VersionValue.getInstance();
        matchAttrs.put(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE, eeValue);
        matchAttrs.put(ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE, versionValue);

        if (filter.matches(matchAttrs)) {

            List<String> eeValueStr = EEValue.getValues();
            List<String> versionValueStr = VersionValue.getValues();

            if (eeValueStr.isEmpty() || versionValueStr.isEmpty()) {
                // There must be a typo/malformed osgi.ee value in the manifest
                // We will just return the filterString verbatim, since we need something
                return filterString;
            }

            String java = eeValueStr.iterator().next();
            String version = versionValueStr.iterator().next();
            javaSEValue = java + " " + version;

        }

        return javaSEValue;

    }

    /**
     * Find which features include the given bundle
     *
     * @param features The feature list to scan for this bundle
     * @param b1       The bundle to look for in features
     * @return List of features this bundle is included in
     */
    public Set<String> findIncludingFeatures(Set<String> features, Bundle b1) {

        Set<String> foundInFeatures = new HashSet<String>();
        for (String feature : features) {
            ProvisioningFeatureDefinition fdefinition = featureRepository.getFeature(feature);
            for (FeatureResource fr : fdefinition.getConstituents(SubsystemContentType.BUNDLE_TYPE)) {
                try {
                    Bundle rfr = bundleCache.getBundle(bundleContext, fr);
                    if (b1.equals(rfr)) {
                        foundInFeatures.add(feature);
                    }
                } catch (MalformedURLException e) {
                }
            }
        }
        return foundInFeatures;
    }

    static final class ConflictRecord {
        String conflict;
        String configured;
        String chain;
        String compatibleConflict;
    }

    /**
     * Reports the errors that happened during feature resolution.
     *
     * @param result
     * @param restrictedAccessAttempts
     */
    boolean reportErrors(Result result, Collection<String> restrictedAccessAttempts, Collection<String> rootFeatures, BundleInstallStatus installStatus) {
        boolean reportedErrors = false;
        for (String nonPublicRoot : result.getNonPublicRoots()) {
            reportedErrors = true;
            if (supportedProcessTypes.contains(ProcessType.CLIENT)) {
                Tr.error(tc, "UPDATE_NOT_PUBLIC_FEATURE_CLIENT_ERROR", nonPublicRoot);
            } else {
                Tr.error(tc, "UPDATE_NOT_PUBLIC_FEATURE_ERROR", nonPublicRoot);
            }
        }
        for (String missing : result.getMissing()) {
            reportedErrors = true;
            boolean isRootFeature = rootFeatures.contains(missing);
            boolean isExtension = missing.indexOf(":") > -1;
            String altName = featureRepository.matchesAlternate(missing);
            //Check if using Open Liberty before suggesting install util for missing features
            if (!getProductInfoDisplayName().startsWith(PRODUCT_INFO_STRING_OPEN_LIBERTY)) {
                if (isRootFeature && !isExtension) {
                    // Only report this message for core features included as root features in the server.xml
                    Tr.error(tc, "UPDATE_MISSING_CORE_FEATURE_ERROR", missing, locationService.getServerName());
                } else {
                    Tr.error(tc, "UPDATE_MISSING_FEATURE_ERROR", missing);
                }
            } else {
                // Not on Open Liberty
                Tr.error(tc, "UPDATE_MISSING_FEATURE_ERROR", missing);
            }
            if (altName != null && isRootFeature && !isExtension) {
                Tr.error(tc, "MISSING_FEATURE_HAS_ALT_NAME", missing, altName);
            }
            installStatus.addMissingFeature(missing);
        }
        for (Entry<String, Chain> wrongProcessType : result.getWrongProcessTypes().entrySet()) {
            reportedErrors = true;
            List<String> chain = wrongProcessType.getValue().getChain();
            if (chain.isEmpty()) {
                if (supportedProcessTypes.contains(ProcessType.CLIENT)) {
                    Tr.error(tc, "UPDATE_WRONG_PROCESS_TYPE_CONFIGURED_CLIENT_ERROR", getFeatureName(wrongProcessType.getKey()), processTypeString + ".xml");
                } else {
                    Tr.error(tc, "UPDATE_WRONG_PROCESS_TYPE_CONFIGURED_ERROR", getFeatureName(wrongProcessType.getKey()), processTypeString + ".xml");
                }
            } else {
                if (supportedProcessTypes.contains(ProcessType.CLIENT)) {
                    Tr.error(tc, "UPDATE_WRONG_PROCESS_TYPE_DEPENDENCY_CLIENT_ERROR", getFeatureName(wrongProcessType.getKey()),
                             getFeatureName(chain.get(0)), processTypeString + ".xml");
                } else {
                    Tr.error(tc, "UPDATE_WRONG_PROCESS_TYPE_DEPENDENCY_ERROR", getFeatureName(wrongProcessType.getKey()),
                             getFeatureName(chain.get(0)), processTypeString + ".xml");
                }
            }
            String wrongProcessTypeMsg = "Unable to load feature \"" + wrongProcessType.getKey() +
                                         "\" because it does not support the correct container type.  The feature dependency chain that led to the feature is: " +
                                         buildChainString(chain, wrongProcessType.getKey());
            IllegalArgumentException ffdcError = new IllegalArgumentException(wrongProcessTypeMsg);
            FFDCFilter.processException(ffdcError, ME, "reportErrors", new Object[] { wrongProcessType.getKey(), wrongProcessType.getValue().toString() });
        }
        for (String restricted : restrictedAccessAttempts) {
            if (supportedProcessTypes.contains(ProcessType.CLIENT)) {
                Tr.error(tc, "UPDATE_WRONG_PROCESS_TYPE_CONFIGURED_CLIENT_ERROR", getFeatureName(restricted), processTypeString + ".xml");
            } else {
                Tr.error(tc, "UPDATE_WRONG_PROCESS_TYPE_CONFIGURED_ERROR", getFeatureName(restricted), processTypeString + ".xml");
            }
        }

        List<Entry<String, Collection<Chain>>> sortedConflicts = new ArrayList<Entry<String, Collection<Chain>>>(result.getConflicts().entrySet());
        sortedConflicts.sort(new ConflictComparator()); // order by importance
        List<Entry<String, String>> reportedConfigured = new ArrayList<Entry<String, String>>(); // pairs of configured features

        boolean disableAllOnConflict = disableAllOnConflict(result);
        for (Entry<String, Collection<Chain>> conflict : sortedConflicts) {
            final String compatibleFeatureBase = conflict.getKey();
            final Collection<Chain> inConflictChains = conflict.getValue();
            reportedErrors = true;
            // Attempt to gather two distinct features that are in conflict, here we assume we
            // can find candidate features that are different
            ConflictRecord conflictRecord1 = null;
            ConflictRecord conflictRecord2 = null;
            for (Chain chain : inConflictChains) {
                if (conflictRecord1 == null) {
                    conflictRecord1 = getConflictRecord(chain, inConflictChains, compatibleFeatureBase);
                } else if (!!!conflictRecord1.conflict.equals(chain.getCandidates().get(0))) {
                    conflictRecord2 = getConflictRecord(chain, inConflictChains, compatibleFeatureBase);
                    break;
                }
            }

            // Report only the most important conflict caused by two configured features
            if (!!!configuredAlreadyReported(conflictRecord1.configured, conflictRecord2.configured, reportedConfigured)) {
                if (conflictRecord1.compatibleConflict != null) {
                    final boolean ignoreVersion = true;
                    if (getEeCompatiblePlatform(conflictRecord1.conflict, ignoreVersion).equals(getEeCompatiblePlatform(conflictRecord2.conflict, ignoreVersion))) {
                        // Both conflicting features support the same named programming model (e.g Java EE or Jakarta EE)
                        if (conflictRecord1.configured.equals(conflictRecord1.compatibleConflict) && conflictRecord2.configured.equals(conflictRecord2.compatibleConflict)) {
                            Tr.error(tc, "UPDATE_CONFLICT_INCOMPATIBLE_EE_FEATURES_SAME_PLATFORM_ERROR",
                                     getPreferredEePlatform(conflictRecord1.compatibleConflict, compatibleFeatureBase),
                                     getPreferredEePlatform(conflictRecord2.compatibleConflict, compatibleFeatureBase),
                                     getFeatureName(conflictRecord1.configured),
                                     getFeatureName(conflictRecord2.configured),
                                     getEeCompatiblePlatform(conflictRecord1.conflict, ignoreVersion));
                        } else {
                            // The conflict is indirect
                            Tr.error(tc, "UPDATE_INDIRECT_CONFLICT_INCOMPATIBLE_FEATURES_SAME_PLATFORM_ERROR",
                                     getPreferredEePlatform(conflictRecord1.compatibleConflict, compatibleFeatureBase),
                                     getPreferredEePlatform(conflictRecord2.compatibleConflict, compatibleFeatureBase),
                                     getFeatureName(conflictRecord1.compatibleConflict),
                                     getFeatureName(conflictRecord2.compatibleConflict),
                                     getFeatureName(conflictRecord1.configured),
                                     getFeatureName(conflictRecord2.configured),
                                     getEeCompatiblePlatform(conflictRecord1.conflict, ignoreVersion));
                        }
                    } else {
                        // One conflicting feature supports "Jakarta EE X", the other "Java EE X"
                        Tr.error(tc, "UPDATE_CONFLICT_INCOMPATIBLE_EE_FEATURES_DIFFERENT_PLATFORM_ERROR",
                                 getPreferredEePlatform(conflictRecord1.compatibleConflict, compatibleFeatureBase),
                                 getPreferredEePlatform(conflictRecord2.compatibleConflict, compatibleFeatureBase),
                                 getFeatureName(conflictRecord1.compatibleConflict),
                                 getFeatureName(conflictRecord2.compatibleConflict),
                                 getFeatureName(conflictRecord1.configured),
                                 getFeatureName(conflictRecord2.configured),
                                 getEeCompatiblePlatform(conflictRecord1.conflict, ignoreVersion),
                                 getEeCompatiblePlatform(conflictRecord2.conflict, ignoreVersion));
                        // Remove the conflicting features (not necessarily the configured features)
                        result.getResolvedFeatures().remove(getFeatureName(conflictRecord1.compatibleConflict));
                        result.getResolvedFeatures().remove(getFeatureName(conflictRecord2.compatibleConflict));
                    }
                } else {
                    Tr.error(tc, "UPDATE_CONFLICT_FEATURE_ERROR", getFeatureName(conflictRecord1.conflict), getFeatureName(conflictRecord2.conflict),
                             getFeatureName(conflictRecord1.configured), getFeatureName(conflictRecord2.configured));
                }
                reportedConfigured.add(new SimpleImmutableEntry<String, String>(conflictRecord1.configured, conflictRecord2.configured));
            }
            String conflictMsg = "Unable to load conflicting versions of features \"" + conflictRecord1.conflict + "\" and \"" + conflictRecord2.conflict +
                                 "\".  The feature dependency chains that led to the conflict are: " + conflictRecord1.chain + " and " + conflictRecord2.chain;
            IllegalArgumentException ffdcError = new IllegalArgumentException(conflictMsg);
            FFDCFilter.processException(ffdcError, ME, "reportErrors", new Object[] { conflict.getKey(), inConflictChains.toString() });
            // TODO not really sure if detailed chain information is needed in the status; doesn't appear to need it
            for (Chain chain : inConflictChains) {
                installStatus.addConflictFeature(chain.getFeatureRequirement());
            }
        }

        if (disableAllOnConflict) {
            // Remove all features on conflicts
            Set<String> resolved = result.getResolvedFeatures();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Conflicts found in feature set, no features will be enabled:" + String.valueOf(sortedConflicts));
            }
            resolved.clear();
            Tr.warning(tc, "UPDATE_DISABLED_FEATURES_ON_CONFLICT");
        }
        return reportedErrors;

    }

    private ConflictRecord getConflictRecord(Chain chain, Collection<Chain> inConflict, String compatibleFeatureBase) {
        List<String> candidates = chain.getCandidates();
        ConflictRecord result = new ConflictRecord();
        result.conflict = candidates.get(0);
        boolean isEeCompatibleConflict = isEeCompatible(result.conflict);
        if (chain.getChain().isEmpty()) {
            // this is a configured root
            result.configured = result.conflict;
            result.chain = result.conflict;
            if (isEeCompatibleConflict) {
                // Note that this case should never happen because the compatible features are private
                // they should never be allowed to be a configured root feature.
                result.compatibleConflict = result.conflict;
            }
        } else {
            result.configured = chain.getChain().get(0);
            if (isEeCompatibleConflict) {
                // Depending on how the feature resolver processes the included features
                // it may not report the direct dependency on the compatible feature as the first
                // conflict.  This may result in a reported chain that has more than one link to the
                // compatible conflict.

                // Check each level of the chain to see if it has a direct dependency on the compatible feature
                // and is in conflict with the other chains in conflict.  If so then then use the feature
                // in this level as the compatible conflict feature.
                chainCheck: for (String feature : chain.getChain()) {
                    ProvisioningFeatureDefinition featureDef = featureRepository.getFeature(feature);
                    for (FeatureResource fr : featureDef.getConstituents(SubsystemContentType.FEATURE_TYPE)) {
                        if (isCompatibleInConflictWithChains(fr, chain, inConflict, compatibleFeatureBase)) {
                            // this level is in conflict with the other chains; use it as the compatible conflict.
                            result.compatibleConflict = feature;
                            break chainCheck;
                        }
                    }
                }
                if (result.compatibleConflict == null) {
                    // fall back to the second to last in chain that has a direct dependency on the compatible feature
                    result.compatibleConflict = chain.getChain().get(chain.getChain().size() - 1);
                }
            }
            result.chain = buildChainString(chain.getChain(), result.conflict);
        }
        return result;
    }

    private boolean isCompatibleInConflictWithChains(FeatureResource fr, Chain chain, Collection<Chain> inConflict, String compatibleFeatureBase) {
        if (fr.getSymbolicName().startsWith(compatibleFeatureBase) == false) {
            // this included feature is not a compatible feature; move on to next
            return false;
        }

        List<String> tolerates = fr.getTolerates();
        for (Chain chainInConflict : inConflict) {
            // only check against the other chains
            if (chainInConflict != chain) {
                List<String> conflictCandidates = chainInConflict.getCandidates();
                if (conflictCandidates.contains(fr.getSymbolicName())) {
                    return false;
                }
                if (tolerates != null) {
                    String[] nameAndVersion = FeatureResolverImpl.parseNameAndVersion(fr.getSymbolicName());

                    for (String tolerate : tolerates) {
                        if (conflictCandidates.contains(nameAndVersion[0] + '-' + tolerate)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean disableAllOnConflict(Result result) {
        Map<String, Collection<Chain>> conflicts = result.getConflicts();
        if (conflicts.isEmpty()) {
            return false;
        }
        // First check if any features in the resolved feature set want to disable on conflict.
        // This includes features not involved in the conflict as well as the features in the
        // chain leading up to the conflict
        for (String featureName : result.getResolvedFeatures()) {
            if (shouldDisableOnConflict(featureName)) {
                return true;
            }
        }

        // Once we get here we know that all the features in the resolution set
        // do not have disable on conflict set to true.
        // But the feature conflicting feature may want to disable all features on conflict.
        // In this case we only will disable all if all candidates want to disable for a specific chain
        // NOTE - This is a bit of a degenerate case.  If a requiring feature only tolerates versions
        // of a feature that disable on conflict then that feature likely should be disable on conflict also.
        // In that case the above loop over the resolved features would have returned true already
        for (Entry<String, Collection<Chain>> conflict : conflicts.entrySet()) {
            for (Chain chain : conflict.getValue()) {
                // NOTE - reverse logic here because there is no isEmpty on Optional in Java 8!
                if (!!!chain.getCandidates().stream().filter((f) -> !!!shouldDisableOnConflict(f)).findFirst().isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldDisableOnConflict(String featureName) {
        return featureRepository.disableAllFeaturesOnConflict(featureName);
    }

    private String getFeatureName(String symbolicName) {
        ProvisioningFeatureDefinition fd = featureRepository.getFeature(symbolicName);
        if (fd == null) {
            return symbolicName;
        }
        return fd.getFeatureName();
    }

    class ConflictComparator implements Comparator<Entry<String, Collection<Chain>>> {
        @Override
        /**
         * Order conflict elements by ascending type rank and chain length.
         */
        public int compare(Entry<String, Collection<Chain>> e1, Entry<String, Collection<Chain>> e2) {
            Iterator<Chain> e1ChainItr = e1.getValue().iterator();
            Iterator<Chain> e2ChainItr = e2.getValue().iterator();
            Chain e1Chain1 = e1ChainItr.next();
            Chain e2Chain1 = e2ChainItr.next();
            String e1Conflict = e1Chain1.getCandidates().get(0);
            String e2Conflict = e2Chain1.getCandidates().get(0);

            // Group ascending by feature type rank
            int e1Rank = rank(e1Conflict);
            int e2Rank = rank(e2Conflict);
            if (e1Rank != e2Rank)
                return e1Rank - e2Rank;

            Chain e1Chain2 = e1ChainItr.next();
            Chain e2Chain2 = e2ChainItr.next();

            // Subgroup ascending by min chain size within rank
            int e1MinChainSize = Math.min(e1Chain1.getChain().size(), e1Chain2.getChain().size());
            int e2MinChainSize = Math.min(e2Chain2.getChain().size(), e2Chain2.getChain().size());
            return e1MinChainSize - e2MinChainSize;
        }

        private int rank(String symbolicName) {
            if (isEeCompatible(symbolicName))
                return 1;
            switch (featureRepository.getFeature(symbolicName).getVisibility()) {
                case PUBLIC:
                    return 2;
                case PROTECTED:
                    return 3;
                case PRIVATE:
                    return 4;
                case INSTALL:
                default:
                    return 5;
            }
        }
    }

    boolean configuredAlreadyReported(String c1, String c2, List<Entry<String, String>> reported) {
        for (Entry<String, String> featurePair : reported)
            if (c1.equals(featurePair.getKey()) && c2.equals(featurePair.getValue()))
                return true;
        return false;
    }

    private boolean isEeCompatible(String symbolicName) {
        return symbolicName != null && symbolicName.lastIndexOf(EE_COMPATIBLE_NAME) >= 0;
    }

    private static char getEeCompatibleVersion(String symbolicName) {
        return symbolicName.charAt(symbolicName.lastIndexOf("-") + 1);
    }

    private String getEeCompatiblePlatform(String symbolicName, boolean ignoreVersion) {
        char charVersion = getEeCompatibleVersion(symbolicName);
        switch (charVersion) {
            case '9':
                return "Jakarta EE" + ((ignoreVersion) ? "" : " " + charVersion);
            case '8':
            case '7':
            case '6':
                return "Java EE" + ((ignoreVersion) ? "" : " " + charVersion);
            default:
                // TODO this is really just a fall back and for testing
                // this should come from additional meta-data of the feature
                // instead of hard-coding in the above cases
                ProvisioningFeatureDefinition fd = (ProvisioningFeatureDefinition) getFeatureDefinition(symbolicName);
                if (fd != null) {
                    String subsystemName = fd.getHeader("Subsystem-Name");
                    if (subsystemName != null) {
                        return subsystemName + ((ignoreVersion) ? "" : " " + charVersion);
                    }
                }
                return "Unknown";
        }
    }

    private String getPreferredEePlatform(String symbolicName, String compatibleFeatureBase) {
        ProvisioningFeatureDefinition fdefinition = featureRepository.getFeature(symbolicName);
        for (FeatureResource fr : fdefinition.getConstituents(SubsystemContentType.FEATURE_TYPE)) {
            if (fr.getSymbolicName().startsWith(compatibleFeatureBase)) {
                return getEeCompatiblePlatform(fr.getSymbolicName(), false); // include ee version
            }
        }
        return "";
    }

    private String buildChainString(List<String> chain, String theConflictFeature) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<String> iChain = chain.iterator(); iChain.hasNext();) {
            String chainFeature = iChain.next();
            if (!!!chainFeature.equals(theConflictFeature)) {
                builder.append(chainFeature);
            }
            if (iChain.hasNext()) {
                builder.append(" -> ");
            }
        }
        if (builder.length() != 0) {
            builder.append(" -> ");
        }
        builder.append(theConflictFeature);
        return builder.toString();
    }

    private boolean featureChangesRequireRestart(Set<String> oldFeatureSet, Set<String> newFeatureSet) {
        final boolean restartApps;

        if (oldFeatureSet == null) {
            return false;
        }

        List<String> existingFeatures = new ArrayList<String>(oldFeatureSet);
        List<String> currentFeatures = Arrays.asList(newFeatureSet.toArray(new String[] {}));

        List<String> newFeatures = new ArrayList<String>(currentFeatures);
        newFeatures.removeAll(existingFeatures);

        List<String> deletedFeatures = existingFeatures;
        deletedFeatures.removeAll(currentFeatures);

        if (shouldRestart(AppForceRestart.INSTALL, newFeatures)) {
            restartApps = true;
        } else {
            restartApps = shouldRestart(AppForceRestart.UNINSTALL, deletedFeatures);
        }
        return restartApps;
    }

    private boolean shouldRestart(AppForceRestart expectedValue, Collection<String> features) {
        for (String feature : features) {
            FeatureDefinition fd = featureRepository.getFeature(feature);
            if (fd != null) {
                AppForceRestart restart = fd.getAppForceRestart();
                if (restart.matches(expectedValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get bundle repository holder for the input type.
     *
     * @param bundleRepositoryType type of bundle holder repository to return
     * @return BundleRepositoryHolder for the input type.
     */
    public BundleRepositoryHolder getBundleRepositoryHolder(String bundleRepositoryType) {
        return BundleRepositoryRegistry.getRepositoryHolder(bundleRepositoryType);
    }

    /**
     * Check the passed in install status for exceptions resolving the feature and
     * installing its bundles.
     * Issues appropriate diagnostics & messages for this environment.
     *
     * @param listName
     *                          the name of the feature that was installed
     * @param installStatus
     *                          Status object holding any warnings or exceptions that occurred
     *                          during bundle installation
     * @return true if no exceptions occurred during bundle installation, false otherwise.
     */
    protected boolean checkInstallStatus(BundleInstallStatus installStatus) throws IllegalStateException {
        final String m = "checkInstallStatus";
        boolean shutdownFramework = false;
        boolean noExceptions = true;

        if (installStatus == null) {
            return true;
        }

        boolean continueOnError = true;
        if (onError.equals(OnError.FAIL))
            continueOnError = false;

        if (installStatus.bundlesMissing()) {
            if (!continueOnError) {
                shutdownFramework = true;
            }
            noExceptions = false;

            Throwable t = new IllegalArgumentException("Missing bundles: " + installStatus.getMissingBundles());
            FFDCFilter.processException(t, ME, m, this, new Object[] { installStatus, bundleCache });
        }

        if (installStatus.featuresMissing()) {
            if (!continueOnError) {
                shutdownFramework = true;
            }
            noExceptions = false;
        }

        if (installStatus.featuresConflict()) {
            if (!continueOnError) {
                shutdownFramework = true;
            }
            noExceptions = false;
        }

        if (installStatus.otherExceptions()) {
            if (!continueOnError) {
                shutdownFramework = true;
            }
            noExceptions = false;

            List<Throwable> otherExceptions = installStatus.getOtherExceptions();

            for (Throwable t : otherExceptions) {
                Tr.error(tc, "UPDATE_OTHER_EXCEPTION_ERROR", new Object[] { t });
                FFDCFilter.processException(t, ME, m, this, new Object[] { installStatus, featureRepository, bundleCache });
            }
        }

        if (!installStatus.contextIsValid()) {
            // rethrow the illegal state exception so it is caught by the update
            installStatus.rethrowInvalidContextException();
        }

        if (installStatus.installExceptions()) {
            if (!continueOnError) {
                shutdownFramework = true;
            }
            noExceptions = false;

            // This may seem like overkill, but each bundle had a different reason for
            // not installing: want to make it easy to figure out which bundles are being
            // bad and why
            Map<String, Throwable> installExceptions = installStatus.getInstallExceptions();
            for (Map.Entry<String, Throwable> entry : installExceptions.entrySet()) {
                Tr.error(tc, "UPDATE_INSTALL_EXCEPTIONS_ERROR", new Object[] { entry.getKey(), entry.getValue() });
                FFDCFilter.processException(entry.getValue(), ME, m, this,
                                            new Object[] { entry.getKey() });
            }
        }

        if (shutdownFramework) {
            shutdownFramework();
        }
        return noExceptions;
    }

    /**
     * Return a display name for the currently running server.
     */
    protected String getProductInfoDisplayName() {
        String result = null;
        try {
            Map<String, ProductInfo> products = ProductInfo.getAllProductInfo();
            StringBuilder builder = new StringBuilder();
            for (ProductInfo productInfo : products.values()) {
                if (productInfo.getReplacedBy() == null) {
                    if (builder.length() != 0) {
                        builder.append(", ");
                    }
                    builder.append(productInfo.getDisplayName());
                }
            }
            result = builder.toString();
        } catch (ProductInfoParseException e) {
            // ignore exceptions-- best effort to get a pretty string
        } catch (DuplicateProductInfoException e) {
            // ignore exceptions-- best effort to get a pretty string
        } catch (ProductInfoReplaceException e) {
            // ignore exceptions-- best effort to get a pretty string
        }
        return result;
    }

    /**
     * Check the passed in start status for exceptions starting bundles,
     * and issue appropriate diagnostics & messages for this environment.
     *
     * @param bundleStatus
     *                         Status object holding any exceptions that occurred
     *                         during bundle start or stop/uninstall
     * @return true if no exceptions occurred while stating bundles, false otherwise.
     */
    protected boolean checkBundleStatus(BundleLifecycleStatus bundleStatus) {
        boolean shutdownFramework = false;
        boolean noExceptions = true;

        boolean continueOnError = true;
        if (onError.equals(OnError.FAIL))
            continueOnError = false;

        if (bundleStatus.startExceptions()) {
            if (!continueOnError) {
                shutdownFramework = true;
            }
            noExceptions = false;

            // This may seem like overkill, but each bundle had a different exception:
            // make it easy to figure out who failed and why.
            Map<Bundle, Throwable> bundleExceptions = bundleStatus.getStartExceptions();
            for (Map.Entry<Bundle, Throwable> entry : bundleExceptions.entrySet()) {
                if (entry.getValue() instanceof BundleException) {
                    StringBuilder exceptionMessages = new StringBuilder();
                    Throwable e = entry.getValue();
                    while (e != null) {
                        exceptionMessages.append(e.getMessage() + "\n");
                        e = e.getCause();
                    }
                    Tr.error(tc, "BUNDLE_EXCEPTION_ERROR", exceptionMessages.toString());
                } else {
                    Tr.error(tc, "UPDATE_LIFECYCLE_EXCEPTIONS_ERROR", entry.getKey(), entry.getValue());
                    FFDCFilter.processException(entry.getValue(), ME, "checkBundleStatus", this, new Object[] { entry.getKey() });
                }
            }
        }

        if (!bundleStatus.contextIsValid()) {
            throw new IllegalStateException("Framework/VM shutting down");
        }

        if (shutdownFramework) {
            shutdownFramework();
        }
        return noExceptions;
    }

    /**
     * Set the start level of the framework, and listen for
     * framework events to ensure we wait until the start level operation
     * is complete before continuing (due to timing, this translates into
     * waiting until the next start level event is fired.. we don't
     * necessarily know that it's ours..).
     *
     * @param level
     *                  StartLevel to change to
     * @return BundleStartStatus containing any exceptions encountered
     *         during the StartLevel change operation.
     */
    @FFDCIgnore({ IllegalStateException.class })
    public BundleLifecycleStatus setStartLevel(int level) {
        StartLevelFrameworkListener slfw = new StartLevelFrameworkListener(shutdownHook);

        if (ServerContentHelper.isServerContentRequest(bundleContext)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Liberty server being held in paused state for minify operation");
            }
        } else {
            try {
                synchronized (this) {
                    fwStartLevel.setStartLevel(level, slfw);
                    slfw.waitForLevel();
                }
            } catch (IllegalStateException e) {
                // bundle context may become invalid if framework stopped
                // while we were waiting.
            }
        }

        return slfw.getStatus();
    }

    /**
     * Returns the current start level of the framework
     *
     * @return int specifying current start level
     */
    public int getStartLevel() {
        return fwStartLevel.getStartLevel();
    }

    /**
     * When an error occurs during startup and the config variable
     * fail.on.error.enabled is true,
     * then this method is used to stop the root bundle thus bringing down the
     * OSGi framework.
     */
    private final void shutdownFramework() {
        try {
            Bundle bundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);

            if (bundle != null)
                bundle.stop();
        } catch (Exception e) {
        }

        throw new IllegalStateException("Shutting down framework due to startup problems");
    }

    /*
     * FeatureProvisioner methods.
     */

    @Override
    public Set<String> getInstalledFeatures() {
        return featureRepository.getInstalledFeatures();
    }

    /**
     * @return List of installed features and implicitly-installed kernel features
     * @throws IOException
     */
    public Collection<ProvisioningFeatureDefinition> getInstalledFeatureDefinitions() {
        List<ProvisioningFeatureDefinition> result = new ArrayList<ProvisioningFeatureDefinition>();
        for (String s : getInstalledFeatures()) {
            result.add(featureRepository.getFeature(s));
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public FeatureDefinition getFeatureDefinition(String featureName) {
        return featureRepository.getFeature(featureName);
    }

    /**
     * TODO: FIXME -- this is for performance
     *
     * @return
     */
    @Override
    public String getKernelApiServices() {
        return KernelFeatureDefinitionImpl.getKernelApiServices();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.kernel.feature.FeatureController#refreshFeatures()
     */
    @Override
    public void refreshFeatures() {
        queueFeatureChange(ProvisioningMode.REFRESH, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.kernel.feature.FeatureProvisioner#refreshFeatures(org.osgi.framework.Filter)
     */
    @Override
    public void refreshFeatures(Filter filter) {
        refreshFeatures();
    }

    boolean missingRequiredJava(FeatureResource fr) {
        Integer requiredJava = fr.getRequireJava();
        return requiredJava == null ? false : JavaInfo.majorVersion() < requiredJava;
    }

}