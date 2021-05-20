/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.wab.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.servlet.context.ExtendedServletContext;
import com.ibm.websphere.servlet.filter.IFilterConfig;
import com.ibm.ws.app.manager.ApplicationStateCoordinator;
import com.ibm.ws.app.manager.module.DeployedAppServices;
import com.ibm.ws.app.manager.module.DeployedModuleInfo;
import com.ibm.ws.app.manager.module.internal.ModuleClassLoaderFactory;
import com.ibm.ws.app.manager.module.internal.ModuleHandler;
import com.ibm.ws.app.manager.module.internal.ModuleInfoUtils;
import com.ibm.ws.app.manager.module.internal.SimpleDeployedAppInfoBase;
import com.ibm.ws.app.manager.wab.helper.WABClassInfoHelper;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleClassesInfo;
import com.ibm.ws.container.service.app.deploy.extended.ApplicationInfoFactory;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ModuleContainerInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.eba.wab.integrator.EbaProvider;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.kernel.feature.ServerReadyStatus;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;
import com.ibm.wsspi.wab.configure.WABConfiguration;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.extension.ExtensionProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

/**
 * This installer is an immediate DS component that will create a RecursiveBundleTracker to look for WABs, when one is found it will install it.
 *
 * Locks in this class :
 *
 * This class uses 2 maps of lists. The 'knownPaths' and the 'wabsEligibleForCollisionResolution'
 * both maps go from a contextpath, to a 'holder' which contains a list of wabs for the context path.
 *
 * Whenever the map is updated, the code should lock on the map itself to prevent concurrent updates.
 * Whenever an update is to be made to the holder, the code should lock on the holder to prevent concurrent updates.
 * If performing both operations, the map lock should be obtained first, and then the holder lock
 * Code executing under the holder lock must not cause actions that require the map lock to be acquired.
 *
 * 2nd lock hierarchy is WAB's lock every state change on an internal lock 'terminated' which also early cancels any
 * changes once set.
 *
 * --Adding bundle on wab--
 * L terminated
 * _addToWebContainer
 * __L addRemoveLock
 * ___installIntoWebContainer
 * ____L wabGroup
 * _____WabGroup.addWAB
 * _____checks wabGroup lock.
 *
 * --Removing bundle on wab--
 * L terminated
 * _invokes wab.removeWab
 * __L terminated
 * ___removeFromWebContainer
 * ____L addRemove Lock
 * _____uninstallFromWebContainer
 * ______L WABgroup
 *
 * --Add bundle on installer (collide)--
 * Move bundle to deploying
 * _L terminated
 * __Bundle added to collide set.
 *
 *
 * --Add bundle on installer (no collide)--
 * Move bundle to deploying
 * _L terminated
 * __Enable tracker on WAB
 * (new thread)
 * L terminated
 * _open subtracker
 * __(callback goes to 'Adding bundle on web')
 * __(callback either on this thread, or another).
 *
 * --'UNDEPLOYED' event.--
 * attemptRedeployOfPreviouslyCollidedContextPath
 * _wab.attemptRedeployOfPreviouslyBlockedWAB
 * __L terminated
 * ___enableTracker.. (see from 'Enable Tracker on WAB'
 * ___under Add bundle on installer [no collide])
 *
 *
 * --Remove bundle on wab installer--
 * _no actions taken on wabgroup / wab
 *
 * --Deactivate wab installer--
 * disable primary tracker
 * __iterate wabGroups
 * ___iterate wabs in each wabGroup
 * ____invokes wab.removeWab
 * _____L terminated
 * ______removeFromWebContainer
 * _______L addRemove Lock
 * ________uninstallFromWebContainer
 * _________L WABgroup
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           service = { EventHandler.class, RuntimeUpdateListener.class, ServerQuiesceListener.class, ServerReadyStatus.class },
           property = { "service.vendor=IBM", "event.topics=org/osgi/service/web/UNDEPLOYED" })
public class WABInstaller implements EventHandler, ExtensionFactory, RuntimeUpdateListener, ServerQuiesceListener, ServerReadyStatus {

    private static final TraceComponent tc = Tr.register(WABInstaller.class);
    private static final String CONFIGURABLE_FILTER = "(&"
                                                      + "(" + Constants.OBJECTCLASS + "=" + WABConfiguration.class.getName() + ")"
                                                      + "(" + WABConfiguration.CONTEXT_NAME + "=*)"
                                                      + "(" + WABConfiguration.CONTEXT_PATH + "=*))";

    private interface WABLifeCycle {
    };

    private static final TraceComponent tcWabLifeCycleDebug = Tr.register(WABLifeCycle.class);

    //don't trace the trace!
    @Trivial
    void wabLifecycleDebug(String message, Object... args) {
        if (TraceComponent.isAnyTracingEnabled() && tcWabLifeCycleDebug.isDebugEnabled()) {
            Tr.debug(tcWabLifeCycleDebug, message, args);
        }
    }

    private RegionDigraph digraph;
    private WABTracker<ConfigurableWAB> tracker;
    private ServiceTracker<WABConfiguration, AtomicReference<ConfigurableWAB>> configurableTracker;

    private final AtomicServiceReference<ExecutorService> executorService = new AtomicServiceReference<ExecutorService>("ExecutorService");
    private final AtomicServiceReference<FutureMonitor> futureMonitor = new AtomicServiceReference<FutureMonitor>("FutureMonitor");
    private final AtomicServiceReference<EventAdmin> eventAdminService = new AtomicServiceReference<EventAdmin>("EventAdmin");
    private final AtomicServiceReference<ArtifactContainerFactory> containerFactorySRRef = new AtomicServiceReference<ArtifactContainerFactory>("ContainerFactory");
    private final AtomicServiceReference<AdaptableModuleFactory> adaptableModuleFactorySRRef = new AtomicServiceReference<AdaptableModuleFactory>("AdaptableModuleFactory");
    private final AtomicServiceReference<ApplicationInfoFactory> applicationInfoFactorySRRef = new AtomicServiceReference<ApplicationInfoFactory>("ApplicationInfoFactory");
    private final AtomicServiceReference<DeployedAppServices> deployedAppServicesSRRef = new AtomicServiceReference<DeployedAppServices>("DeployedAppServices");
    private final AtomicServiceReference<ModuleHandler> webModuleHandlerSRRef = new AtomicServiceReference<ModuleHandler>("WebModuleHandler");
    private final AtomicServiceReference<VariableRegistry> variableRegistrySRRef = new AtomicServiceReference<VariableRegistry>("VariableRegistry");

    private final AtomicBoolean deactivated = new AtomicBoolean(false);
    private final ReentrantReadWriteLock deactivationLock = new ReentrantReadWriteLock();
    private final AtomicBoolean quiesceStarted = new AtomicBoolean(false);

    /**
     * A map of WABs grouped under an EBA, keyed by the EBA ID
     */
    private final ReentrantLock wabGroupsLock = new ReentrantLock();
    private final Map<String, WABGroup> wabGroups = new HashMap<String, WABGroup>();

    private BundleContext ctx = null;

    private ServiceTracker<EbaProvider, EbaProvider> ebaProviderTracker;

    private final Map<String, WABPathSpecificItemHolder> knownPaths = new HashMap<String, WABPathSpecificItemHolder>();
    private final Hashtable<String, WABPathSpecificItemHolder> wabsEligibleForCollisionResolution = new Hashtable<String, WABPathSpecificItemHolder>();

    @Reference
    private volatile List<WABClassInfoHelper> classInfoHelpers;

    private final Map<WAB, Future<?>> systemWABsDeploying = new ConcurrentHashMap<>();

    @Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
    private volatile ServerStarted serverStarted;

    /* Package protected method to allow WAB's to obtain bundle trackers too */
    <T> WABTracker<T> getTracker(BundleTrackerCustomizer<T> wabTrackerCustomizer) {
        try {
            //we are interested in WABs that are starting (could be lazy activation)
            //or already active
            int mask = Bundle.STARTING | Bundle.ACTIVE;
            return new WABTracker<T>(ctx, mask, wabTrackerCustomizer);
        } catch (Exception e) {
            // If this goes wrong FFDC and log the error as we won't be able to be used
            Tr.error(tc, "bundle.tracker.init.fail");
        }
        return null;
    }

    /**
     * Activate this service and start the {@link WABTracker}.
     *
     * @param context
     */
    //@Override
    protected void activate(ComponentContext context) {
        ctx = context.getBundleContext();
        ebaProviderTracker = new ServiceTracker<EbaProvider, EbaProvider>(ctx, EbaProvider.class, null);
        ebaProviderTracker.open();
        executorService.activate(context);
        futureMonitor.activate(context);
        eventAdminService.activate(context);
        containerFactorySRRef.activate(context);
        adaptableModuleFactorySRRef.activate(context);
        applicationInfoFactorySRRef.activate(context);
        deployedAppServicesSRRef.activate(context);
        webModuleHandlerSRRef.activate(context);
        variableRegistrySRRef.activate(context);

        WABTrackerCustomizer customizer = new WABTrackerCustomizer(digraph);
        try {
            configurableTracker = new ServiceTracker<WABConfiguration, AtomicReference<ConfigurableWAB>>(ctx, ctx.createFilter(CONFIGURABLE_FILTER), customizer);
            configurableTracker.open();
        } catch (InvalidSyntaxException e) {
            // auto FFDC; should never happen
        }

        tracker = getTracker(customizer);
        tracker.open();
        wabLifecycleDebug("Primary WAB Tracker has opened.");
    }

    /**
     * Stops the tracker
     */
    //@Override
    protected void deactivate(ComponentContext context) {
        // need to track activation state since there may be threads in flight trying to deploy a WAB
        deactivationLock.writeLock().lock();
        try {
            deactivated.set(true);
        } finally {
            deactivationLock.writeLock().unlock();
        }

        //turn off the tracker if we are shutting down
        if (tracker != null) {
            tracker.close();
            wabLifecycleDebug("Primary WAB Tracker has closed.");
        }
        if (configurableTracker != null) {
            configurableTracker.close();
        }

        //forget all wabs eligible for collision detection to prevent collision resolution from
        //reinstalling bundles during shutdown!

        // forget all pending context resolutions..
        synchronized (wabsEligibleForCollisionResolution) {
            wabsEligibleForCollisionResolution.clear();
        }

        //remove all the known WABs from the web container
        Collection<WABGroup> toRemoveGroups;
        wabGroupsLock.lock();
        try {
            toRemoveGroups = new ArrayList<>(wabGroups.values());
        } finally {
            wabGroupsLock.unlock();
        }
        for (WABGroup wabGroup : toRemoveGroups) {
            wabLifecycleDebug("Primary WAB Tracker uninstalling WABGroup during deactivate", wabGroup);
            wabGroup.uninstallGroup(this);
        }

        //forget the wabGroups
        wabGroupsLock.lock();
        try {
            wabGroups.clear();
        } finally {
            wabGroupsLock.unlock();
        }

        // forget all our wabs.
        synchronized (knownPaths) {
            knownPaths.clear();
        }

        wabLifecycleDebug("Primary WAB Tracker no longer tracking wabs");

        if (ebaProviderTracker != null)
            ebaProviderTracker.close();

        executorService.deactivate(context);
        futureMonitor.deactivate(context);
        eventAdminService.deactivate(context);
        containerFactorySRRef.deactivate(context);
        adaptableModuleFactorySRRef.deactivate(context);
        applicationInfoFactorySRRef.deactivate(context);
        deployedAppServicesSRRef.deactivate(context);
        webModuleHandlerSRRef.deactivate(context);
        variableRegistrySRRef.deactivate(context);
        ctx = null;
    }

    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (RuntimeUpdateNotification.CONFIG_UPDATES_DELIVERED.equals(notification.getName())) {
            notification.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    if (result) {
                        List<Bundle> bundles = new ArrayList<>();
                        wabGroupsLock.lock();
                        try {
                            for (WABGroup group : wabGroups.values()) {
                                for (WAB wab : group.getWABs()) {
                                    if (!wab.isResolvedVirtualHostValid()) {
                                        bundles.add(wab.getBundle());
                                    }
                                }
                            }
                        } finally {
                            wabGroupsLock.unlock();
                        }

                        for (Bundle b : bundles) {
                            restart(b);
                        }
                    }
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                }
            });
        }
    }

    protected String resolveVariable(String stringToResolve) {
        return variableRegistrySRRef.getService().resolveString(stringToResolve);
    }

    /**
     * This method will install the supplied bundle into the web container.
     *
     * This method is called asynchronously from queued work resulting from
     * WAB bundles entering the STARTING|ACTIVE states as tracked by the {@link WABTracker} and {@link WABTrackerCustomizer}.
     *
     * @param wab    - the tracked {@link WAB} object
     * @param bundle - the {@link Bundle} to install
     */
    protected boolean installIntoWebContainer(WAB wab) {
        Bundle bundle = wab.getBundle();
        String contextRoot = wab.getContextRoot();
        //hold a lock to prevent deactivation of 'this' component while work is in progress
        deactivationLock.readLock().lock();
        try {
            if (this.deactivated.get() == true) {
                //we seem to be attempting to deploy a WAB concurrently with the de-activation of
                // this WAB installer from another thread
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "installIntoWebContainer(WAB) entered by thread after component deactivation",
                             wab);
                }
                return false;
            }
            ModuleHandler webModuleHandler = webModuleHandlerSRRef.getService();
            if (webModuleHandler != null) {
                // Do a quick test to make sure we're not shutting down, bomb out if we are
                if (FrameworkState.isStopping()) {
                    return false;
                }

                // Get the wiring for the bundle
                BundleWiring wiring = bundle.adapt(BundleWiring.class);
                if (wiring == null) {
                    postFailureEvent(wab, "wab.install.fail.wiring", bundle, contextRoot);
                    Tr.error(tc, "wab.install.fail.wiring", bundle, contextRoot);
                    return false;
                }

                Container wabContainer = getContainerForBundle(wab, bundle, contextRoot);

                Entry manifestEntry = wabContainer.getEntry("/META-INF/MANIFEST.MF");
                if (manifestEntry != null) {
                    NonPersistentCache cache = manifestEntry.adapt(NonPersistentCache.class);
                    cache.addToCache(Dictionary.class, bundle.getHeaders(""));
                }

                // Get the classloader for the bundle
                ClassLoader loader = wiring.getClassLoader();

                // create a classes info that reflects the bundle wiring so that we can
                // put that into the overlay cache to supersede the JEE classes info
                WebModuleClassesInfo classesInfo = getClassesInfo(wab, wabContainer, bundle, contextRoot, wiring);
                // classesInfo is known to be non-null - getClassesInfo always constructs a new one
                NonPersistentCache npc = wabContainer.adapt(NonPersistentCache.class);
                npc.addToCache(WebModuleClassesInfo.class, classesInfo);

                // Use the name of the bundle as its module path within the EBA.
                final String modPath = bundle.getSymbolicName();

                if (!contextRoot.startsWith("/")) {
                    contextRoot = "/" + contextRoot;
                }

                // Now find the parent application, WABs are always grouped together according to their owning EBA
                EbaProvider ebaProviderService = ebaProviderTracker.getService();
                ExtendedApplicationInfo appInfo = null;
                //an EBA provider is only available if EBAs are available, system WABs do without this grouping
                if (ebaProviderService != null) {
                    appInfo = (ExtendedApplicationInfo) ebaProviderService.getApplicationInfo(bundle);
                }

                if (appInfo == null) {
                    //Standalone WAB
                    //need to create an app info for it
                    String moduleURI = ModuleInfoUtils.getModuleURIFromLocation(modPath);
                    String moduleName = ModuleInfoUtils.getModuleName(wabContainer.adapt(WebApp.class), moduleURI);
                    appInfo = applicationInfoFactorySRRef.getService().createApplicationInfo(modPath,
                                                                                             moduleName,
                                                                                             wabContainer,
                                                                                             null,
                                                                                             null);
                    wab.setCreatedApplicationInfo();
                }
                wab.setApplicationInfo(appInfo);

                WABGroup wabGroup;
                // Using old fashion locks here. Can revisit when we can use Java 8.
                boolean groupsLocked = true;
                wabGroupsLock.lock();
                try {
                    wabGroup = wabGroups.get(appInfo.getName());
                    if (wabGroup == null) {
                        wabGroup = new WABGroup(new WABDeployedAppInfo(deployedAppServicesSRRef.getService(), appInfo));
                        wabGroups.put(appInfo.getName(), wabGroup);
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        //Note: this trace string is checked in FAT tests
                        Tr.debug(tc, "installIntoWebContainer wabGroups.putIfAbsent(" + appInfo.getName() + " , " + wabGroup + ") ==> " + wabGroup);
                    }

                    //We need to make sure that the initial addWebContainerApplication calls
                    //have completed before we try to add any new modules to the application.
                    synchronized (wabGroup) {
                        WABDeployedAppInfo deployedApp = (WABDeployedAppInfo) wabGroup.getDeployedAppInfo();

                        //add the WAB to the WAB group (we add standalone WABs to a group, but they'll only ever have one WAB in the group)
                        wabGroup.addWab(wab, this);
                        groupsLocked = false;
                        wabGroupsLock.unlock();

                        ModuleContainerInfo mci = deployedApp.createModuleContainerInfo(webModuleHandler, contextRoot, wabContainer, modPath, loader);
                        wab.setModuleContainerInfo(mci);
                        if (wab.getCreatedApplicationInfo()) {
                            if (!deployedApp.installApp(wab)) {
                                postFailureEvent(wab, "wab.install.fail", bundle, contextRoot);
                                Tr.error(tc, "wab.install.fail", bundle, contextRoot);
                                return false;
                            }
                        } else {
                            if (!deployedApp.installModule(wab)) {
                                postFailureEvent(wab, "wab.install.fail", bundle, contextRoot);
                                Tr.error(tc, "wab.install.fail", bundle, contextRoot);
                                return false;
                            }
                        }

                        //register the WAB bundle with a key that can be looked up elsewhere
                        //based on the J2EEName form
                        Dictionary<String, Object> bRegProps = new Hashtable<String, Object>(1);
                        bRegProps.put("web.module.key", appInfo.getName() + "#" + deployedApp.getModuleName(mci));
                        bRegProps.put("installed.wab.contextRoot", contextRoot);
                        bRegProps.put("installed.wab.container", wabContainer);
                        ServiceRegistration<Bundle> reg = ctx.registerService(Bundle.class, bundle, bRegProps);
                        wab.setRegistration(reg);
                    }
                } finally {
                    if (groupsLocked) {
                        wabGroupsLock.unlock();
                    }
                }
            } else {
                postFailureEvent(wab, "wab.install.fail.container", bundle, contextRoot);
                Tr.error(tc, "wab.install.fail.container", bundle, contextRoot);
                return false;
            }
        } catch (Throwable e) {
            postEvent(wab.createFailedEvent(e));
            // we won't be able to install the WAB. Issue error message.
            Tr.error(tc, "wab.install.fail", bundle, contextRoot);
            return false;
        } finally {
            deactivationLock.readLock().unlock();
        }
        return true;
    }

    /**
     *
     * @param wab
     * @return true if the wab was removed, or false if the liberty web module handler
     *         was unavailable. (this implies the webcontainer has gone away)
     */
    protected boolean uninstallFromWebContainer(WAB wab) {
        // Make sure the web container is still active otherwise we won't be able to uninstall
        ModuleHandler webModuleHandler = webModuleHandlerSRRef.getService();
        if (webModuleHandler != null) {
            //check if this is a WAB from an EBA group
            ApplicationInfo info = wab.getApplicationInfo();
            String groupKey = info.getName();
            WABGroup group;
            boolean groupsLocked = true;
            wabGroupsLock.lock();
            try {
                group = wabGroups.get(groupKey);
                if (group != null) {
                    //synchronize on the WABGroup to match up with install
                    synchronized (group) {
                        WABDeployedAppInfo deployedApp = (WABDeployedAppInfo) group.getDeployedAppInfo();
                        if (group.removeWAB(wab)) {
                            // group is empty remove it
                            wabGroups.remove(groupKey);
                            groupsLocked = false;
                            wabGroupsLock.unlock();
                        }
                        if (wab.getCreatedApplicationInfo()) {
                            // system WABs get uninstalled here
                            deployedApp.uninstallApp(wab);
                        } else {
                            if (ctx.getBundle(org.osgi.framework.Constants.SYSTEM_BUNDLE_LOCATION).getState() == Bundle.STOPPING) {
                                // if shutting down then launch uninstallModule asynchronously and wait up
                                // to 30 secs. This avoids blocking server shutdown unduly.
                                final CountDownLatch latch = new CountDownLatch(1);
                                executorService.getService().submit(new Runnable() {
                                    @Override
                                    public void run() {
                                        deployedApp.uninstallModule(wab);
                                        latch.countDown();
                                    }
                                }, Boolean.TRUE);
                                try {
                                    // If a quiesce has not been initiated then a force stop was called so
                                    // continue without waiting. Otherwise give the uninstallModule thread
                                    // time to unwind before proceeding with WAB removal.
                                    if (quiesceStarted.get()) {
                                        latch.await(30, TimeUnit.SECONDS);
                                    }
                                } catch (InterruptedException e) {
                                    // continue with wab uninstall.
                                    Thread.currentThread().interrupt();
                                }
                            } else {
                                deployedApp.uninstallModule(wab);
                            }
                        }
                    }
                }
            } finally {
                if (groupsLocked) {
                    wabGroupsLock.unlock();
                }
            }

            return true;
            //if the entry was already removed then that entire WAB group was already removed
        }
        return false;
    }

    @Reference(service = ExecutorService.class)
    protected void setExecutorService(ServiceReference<ExecutorService> reference) {
        executorService.setReference(reference);
    }

    protected void unsetExecutorService(ServiceReference<ExecutorService> reference) {
        executorService.unsetReference(reference);
    }

    @Reference(service = FutureMonitor.class)
    protected void setFutureMonitor(ServiceReference<FutureMonitor> reference) {
        futureMonitor.setReference(reference);
    }

    protected void unsetFutureMonitor(ServiceReference<FutureMonitor> reference) {
        futureMonitor.unsetReference(reference);
    }

    @Reference(service = EventAdmin.class)
    protected void setEventAdmin(ServiceReference<EventAdmin> reference) {
        eventAdminService.setReference(reference);
    }

    protected void unsetEventAdmin(ServiceReference<EventAdmin> reference) {
        eventAdminService.unsetReference(reference);
    }

    @Reference(service = ArtifactContainerFactory.class, target = "(&(category=DIR)(category=JAR)(category=BUNDLE))")
    protected void setContainerFactory(ServiceReference<ArtifactContainerFactory> ref) {
        containerFactorySRRef.setReference(ref);
    }

    protected void unsetContainerFactory(ServiceReference<ArtifactContainerFactory> ref) {
        containerFactorySRRef.unsetReference(ref);
    }

    @Reference(service = AdaptableModuleFactory.class)
    protected void setAdaptableModuleFactory(ServiceReference<AdaptableModuleFactory> ref) {
        adaptableModuleFactorySRRef.setReference(ref);
    }

    protected void unsetAdaptableModuleFactory(ServiceReference<AdaptableModuleFactory> ref) {
        adaptableModuleFactorySRRef.unsetReference(ref);
    }

    @Reference(service = ApplicationInfoFactory.class)
    protected void setApplicationInfoFactory(ServiceReference<ApplicationInfoFactory> ref) {
        applicationInfoFactorySRRef.setReference(ref);
    }

    protected void unsetApplicationInfoFactory(ServiceReference<ApplicationInfoFactory> ref) {
        applicationInfoFactorySRRef.unsetReference(ref);
    }

    @Reference
    protected void setDeployedAppServices(ServiceReference<DeployedAppServices> ref) {
        deployedAppServicesSRRef.setReference(ref);
    }

    protected void unsetDeployedAppServices(ServiceReference<DeployedAppServices> ref) {
        deployedAppServicesSRRef.unsetReference(ref);
    }

    @Reference(service = ModuleHandler.class, target = "(type=web)")
    protected void setWebModuleHandler(ServiceReference<ModuleHandler> ref) {
        webModuleHandlerSRRef.setReference(ref);
    }

    protected void unsetWebModuleHandler(ServiceReference<ModuleHandler> ref) {
        webModuleHandlerSRRef.unsetReference(ref);
    }

    @Reference(service = VariableRegistry.class)
    protected void setVariableRegistry(ServiceReference<VariableRegistry> ref) {
        variableRegistrySRRef.setReference(ref);
    }

    protected void unsetVariableRegistry(ServiceReference<VariableRegistry> ref) {
        variableRegistrySRRef.unsetReference(ref);
    }

    @Reference
    protected void setWABExtensionFactory(WABExtensionFactory extFactory) {
        extFactory.setDelegate(this);
    }

    protected void unsetWABExtensionFactory(WABExtensionFactory extFactory) {
        extFactory.setDelegate(null);
    }

    @Reference
    protected void setRegionDigraph(RegionDigraph digraph) {
        this.digraph = digraph;
    }

    protected void unsetRegionDigraph(RegionDigraph digraph) {
        // do nothing
    }

    static void restart(Bundle bundle) {
        // if the wab bundle has been associated with this configuration then it must be restarted
        if (bundle.getState() != Bundle.STOPPING) {
            try {
                bundle.stop();
            } catch (BundleException e) {
                // auto FFCD
            }
            // note that we are restarting the WAB, but it is likely there is no
            // available configuration yet for it so it will not trigger another WAB creation until
            // there is appropriate configuration for it.
            try {
                bundle.start();
            } catch (BundleException e) {
                // auto FFCD
            }
        }
    }

    /*
     * A basic struct to hold the objects necessary to create a WAB. Currently the only things necessary
     * to create a WAB is the WAB bundle and a configured context path. Once both of these are
     * available then the wabRef will be updated with the actual WAB object.
     */
    final static class ConfigurableWAB {
        Bundle wabBundle = null;
        String configuredContextPath;
        final AtomicReference<WAB> wabRef;
        final String webContextPathName;
        final Region webContextPathRegion;

        ConfigurableWAB(Bundle wabBundle, Region region, String webContextPathName, String configuredContextPath, WAB wab) {
            this.wabBundle = wabBundle;
            this.webContextPathName = webContextPathName;
            this.configuredContextPath = configuredContextPath;
            this.wabRef = new AtomicReference<WAB>(wab);
            this.webContextPathRegion = region;
        }
    }

    final class WABTrackerCustomizer implements BundleTrackerCustomizer<ConfigurableWAB>, ServiceTrackerCustomizer<WABConfiguration, AtomicReference<ConfigurableWAB>> {
        private final RegionDigraph digraph;
        private final Map<String, Map<Region, ConfigurableWAB>> configurableContextPaths = new HashMap<String, Map<Region, ConfigurableWAB>>();

        public WABTrackerCustomizer(RegionDigraph digraph) {
            this.digraph = digraph;
        }

        /** {@inheritDoc} */
        @Override
        public ConfigurableWAB addingBundle(Bundle bundle, BundleEvent event) {
            //the bundle is in STARTING | ACTIVE state because of our state mask

            // The "Web-ContextPath" header defines the context root for the web application and is the flag that indicates if a bundle is a WAB
            String webContextPath = bundle.getHeaders("").get("Web-ContextPath");
            if (webContextPath == null) {
                return null;
            }

            if (webContextPath.startsWith("@")) {
                // this is the case where the WAB wants its context path to be configured
                return createConfigurableWAB(bundle, webContextPath.substring(1));
            }

            webContextPath = WASContextPathNormalize(webContextPath);
            // Return a fully configured WAB that does not have a context name to be configurable.
            // This is the normal case.
            return new ConfigurableWAB(bundle, null, null, webContextPath, createWAB(bundle, webContextPath));
        }

        /**
         * Returns a ConfigurableWAB for the specified WAB bundle and its context path name.
         * If there is already a ConfigurableWAB with the specified context name then a
         * check is done to see if its configuredContextPath is set. If so then an actual
         * WAB is created with the WAB bundle and its configured context path.
         *
         * @param wabBundle
         * @param webContextPathName
         * @return
         */
        private ConfigurableWAB createConfigurableWAB(Bundle wabBundle, String webContextPathName) {
            synchronized (configurableContextPaths) {
                Region region = digraph.getRegion(wabBundle);
                if (region == null) {
                    // this should never happen, but if it does it could indicate the bundle
                    // got uninstalled right in the middle of us tracking it!
                    // just return null to indicate we do not want to track it.
                    return null;
                }
                ConfigurableWAB configurableWAB = findConfigurableWAB(region, webContextPathName);
                if (configurableWAB == null) {
                    // no exist configurable WAB.  Here we know there is no configured path yet
                    configurableWAB = new ConfigurableWAB(wabBundle, region, webContextPathName, null, null);
                    // store the configurable WAB so it can be processed if/when a proper configuration is found
                    putConfigurableWAB(configurableWAB);
                } else {
                    // make sure the exist configurable WAB bundle is not set
                    if (configurableWAB.wabBundle != null) {
                        throw new IllegalStateException("A bundle is already attempting to use the name: " + webContextPathName + " - " + configurableWAB.wabBundle);
                    }

                    configurableWAB.wabBundle = wabBundle;
                    // check to see if a configured context path is available
                    if (configurableWAB.configuredContextPath != null) {
                        // found one, now create the actual WAB since we have all the information required
                        configurableWAB.wabRef.set(createWAB(configurableWAB.wabBundle, configurableWAB.configuredContextPath));
                    }
                }
                return configurableWAB;
            }
        }

        private void putConfigurableWAB(ConfigurableWAB configurableWAB) {
            Map<Region, ConfigurableWAB> configurableWABs = getOrCreateConfigurableWABMap(configurableWAB.webContextPathName);
            configurableWABs.put(configurableWAB.webContextPathRegion, configurableWAB);
        }

        private ConfigurableWAB findConfigurableWAB(Region region, String webContextPathName) {
            return getOrCreateConfigurableWABMap(webContextPathName).get(region);
        }

        private Map<Region, ConfigurableWAB> getOrCreateConfigurableWABMap(String webContextPathName) {
            synchronized (configurableContextPaths) {
                Map<Region, ConfigurableWAB> configurableWABs = configurableContextPaths.get(webContextPathName);
                if (configurableWABs == null) {
                    configurableWABs = new HashMap<Region, WABInstaller.ConfigurableWAB>();
                    configurableContextPaths.put(webContextPathName, configurableWABs);
                }
                return configurableWABs;
            }
        }

        /**
         * Returns a ConfigurableWAB for the specified context path name with the configured context path.
         * If there is already a ConfigurableWAB with the specified context name then a check is done to
         * see if its wabBundle is set. If so then an actual WAB is created with the WAB bundle and its configured context path.
         *
         * @param webContextPathName
         * @param configuredContextPath
         * @return
         */
        private ConfigurableWAB createConfigurableWAB(ServiceReference<WABConfiguration> ref) {
            synchronized (configurableContextPaths) {
                Region region = digraph.getRegion(ref.getBundle());
                if (region == null) {
                    // this should never happen, but if it does it could indicate the bundle
                    // got uninstalled right in the middle of us tracking it!
                    // just return null to indicate we do not want to track it.
                    return null;
                }
                String webContextPathName = (String) ref.getProperty(WABConfiguration.CONTEXT_NAME);
                String configuredContextPath = (String) ref.getProperty(WABConfiguration.CONTEXT_PATH);
                ConfigurableWAB configurableWAB = findConfigurableWAB(region, webContextPathName);
                if (configurableWAB == null) {
                    // no exist configurable WAB.  Here we know there is no WAB bundle yet
                    configurableWAB = new ConfigurableWAB(null, region, webContextPathName, configuredContextPath, null);
                    // store the configurable WAB so it can be processed if/when a proper WAB bundle is found
                    putConfigurableWAB(configurableWAB);
                } else {
                    // make sure the exist configurable context path is not set
                    if (configurableWAB.configuredContextPath != null) {
                        throw new IllegalStateException("A context path \"" + configurableWAB.configuredContextPath + "\" is already configured for the name: "
                                                        + webContextPathName);
                    }
                    configurableWAB.configuredContextPath = configuredContextPath;
                    // check to see if a configured wab bundle available
                    if (configurableWAB.wabBundle != null) {
                        // found one, now create the actual WAB since we have all the information required
                        configurableWAB.wabRef.set(createWAB(configurableWAB.wabBundle, configurableWAB.configuredContextPath));
                    }
                }
                return configurableWAB;
            }
        }

        /*
         * The web context path name has had its configuration removed/changed
         * If there is a configurableWAB associated with this context path name
         * it must be reconfigured.
         */
        private void unconfigureContextPath(ConfigurableWAB configurableWAB) {
            synchronized (configurableContextPaths) {
                // null out its configured path since it is no longer configured
                configurableWAB.configuredContextPath = null;
                Bundle wabBundle = configurableWAB.wabBundle;
                if (wabBundle != null) {
                    restart(wabBundle);
                } else {
                    getOrCreateConfigurableWABMap(configurableWAB.webContextPathName).remove(configurableWAB.webContextPathRegion);
                }
            }
        }

        private void unconfigureWABBundle(ConfigurableWAB configurableWAB) {
            if (configurableWAB.webContextPathName == null) {
                // this is the normal case where the WAB really isn't configurable
                return;
            }
            synchronized (configurableContextPaths) {
                configurableWAB.wabBundle = null;
                if (configurableWAB.configuredContextPath == null) {
                    // if there is no configured context path then there is no point in keeping the object around
                    getOrCreateConfigurableWABMap(configurableWAB.webContextPathName).remove(configurableWAB.webContextPathRegion);
                }
            }
        }

        private WAB createWAB(Bundle bundle, String webContextPath) {
            //if we got here then this bundle is a WAB, so we need to create a WAB tracking object
            WAB addedWAB = new WAB(bundle, webContextPath, WABInstaller.this);

            wabLifecycleDebug("Primary WAB Tracker processing new WAB", addedWAB);

            //change the state to deploying and fire the event
            if (addedWAB.moveToDeploying()) {
                //moveToDeploying will have obtained and released the terminate lock during its execution.

                //check for a Web-ContextPath collision
                //create a new holder for this context path, with the current WAB in its list
                WABPathSpecificItemHolder holder = new WABPathSpecificItemHolder(addedWAB);
                WABPathSpecificItemHolder collisionsHolder = new WABPathSpecificItemHolder(addedWAB);
                WABPathSpecificItemHolder knownHolder;
                WABPathSpecificItemHolder knownCollisionsHolder;

                //update the 'knownPaths' set, that holds ALL WABs even in failed state.
                //sync'd for consistency.. if the context path is already known, then
                //the wab is not added (yet), else the wab (in its holder) is added
                synchronized (knownPaths) {
                    knownHolder = knownPaths.put(webContextPath, holder);
                    if (knownHolder != null) {
                        //there was a Web-ContextPath collision
                        // put it back -- makes life easier elsewhere
                        knownPaths.put(webContextPath, knownHolder);
                    }
                }

                //if knownHolder is null, then there were no other wabs known for this context path, and we are in the new set.
                //if it's non-null, that means there were bundles known, and we are not in the set yet.
                if (knownHolder != null) {
                    synchronized (knownHolder) {
                        Collection<WAB> collisions = knownHolder.getWABs();
                        ArrayList<WAB> toRemove = new ArrayList<WAB>();
                        for (WAB w : collisions) {
                            Bundle collidingBundle = w.getBundle();
                            if (collidingBundle.getBundleId() == bundle.getBundleId()) {
                                wabLifecycleDebug("Primary WAB Tracker cleaning up old known WAB for bundleId ", w, bundle.getBundleId());
                                //the bundle ids matched.. this is odd, since we haven't added our wab
                                //to the known collisions set yet. Most likely we have an entry for this wab
                                //in a prior stage of its lifecycle.. so remove that one, as we'll add the new
                                //one in a mo.
                                w.terminateWAB();
                                toRemove.add(w);
                            }
                        }
                        //remove any stale wabs with this bundle id.
                        knownHolder.getWABs().removeAll(toRemove);
                        //finally add this new WAB to the set.
                        knownHolder.getWABs().add(addedWAB);
                    }
                }

                //next up we process the 'eligible for collision resolution' set.
                //this holds bundles that failed to install because the context path
                //was already in use. It does not hold bundles that fail for other reasons.
                synchronized (wabsEligibleForCollisionResolution) {
                    knownCollisionsHolder = wabsEligibleForCollisionResolution.put(webContextPath, collisionsHolder);
                    if (knownCollisionsHolder != null && !knownCollisionsHolder.getWABs().isEmpty()) {
                        //there was already a holder for this path, we are not in it yet.
                        wabsEligibleForCollisionResolution.put(webContextPath, knownCollisionsHolder);
                        synchronized (knownCollisionsHolder) {
                            WAB selfAlreadyInSet = null;
                            for (WAB w : knownCollisionsHolder.getWABs()) {
                                if (w.getBundle().getBundleId() == bundle.getBundleId()) {
                                    // We found a bundle with our id already in the set.. this is odd.. we'd better clean up..
                                    w.terminateWAB();
                                    selfAlreadyInSet = w;
                                    knownCollisionsHolder = null;
                                    break;
                                }
                            }
                            if (selfAlreadyInSet != null) {
                                knownCollisionsHolder.getWABs().remove(selfAlreadyInSet);
                            }
                        }
                    }
                }

                if (knownCollisionsHolder != null && !knownCollisionsHolder.getWABs().isEmpty()) {
                    //there was a Web-ContextPath collision
                    //add previous bundles to the collisions list
                    long[] collisionIds;
                    synchronized (knownCollisionsHolder) {
                        Collection<WAB> collisions = knownCollisionsHolder.getWABs();
                        ArrayList<WAB> toRemove = new ArrayList<WAB>();
                        collisionIds = new long[collisions.size()];
                        int i = 0;
                        for (WAB w : collisions) {
                            Bundle collidingBundle = w.getBundle();
                            collisionIds[i] = collidingBundle.getBundleId();
                            i++;
                            if (collidingBundle.getBundleId() == bundle.getBundleId()) {
                                wabLifecycleDebug("Primary WAB Tracker cleaning up old colliding WAB for bundleId ", w, bundle.getBundleId());
                                //the bundle ids matched. Most likely we have an entry for this wab
                                //in a prior stage of its lifecycle.. so remove that one, as we'll
                                // add the new one in a mo.
                                w.terminateWAB();
                                toRemove.add(w);
                            }
                        }

                        //remove any stale wabs with this bundle id.
                        knownCollisionsHolder.getWABs().removeAll(toRemove);

                        // At this point the knownCollisionsHolder is only items that don't much our bundle ID.
                        // If this set is not empty this is the ideal time to print out a message
                        if (!knownCollisionsHolder.getWABs().isEmpty()) {
                            ArrayList<String> collidingWabs = new ArrayList<String>(knownCollisionsHolder.getWABs().size());
                            for (WAB w : knownCollisionsHolder.getWABs()) {
                                collidingWabs.add(w.getBundle().toString());
                            }
                            Tr.error(tc, "wab.install.fail.clash", bundle, webContextPath, collidingWabs);
                        }

                        //add this wab to the wabs eligible for collision resolution./
                        knownCollisionsHolder.getWABs().add(addedWAB);
                    }

                    wabLifecycleDebug("Primary WAB Tracker adding WAB to collision set.", addedWAB);
                    //we know there is a collision so for this case so post a
                    //failed event
                    postEvent(addedWAB.createFailedEvent(webContextPath, collisionIds));
                } else {
                    //the holder will already have the wab in its list from construction
                    //no collision, just tell the wab's internal tracker to take over
                    //it will synchronously handle the deploy/undeploy
                    wabLifecycleDebug("Primary WAB Tracker enabling SubTracker for new WAB", addedWAB);
                    addedWAB.enableTracker();
                }

                //return the WAB tracked object
                return addedWAB;
            } else
                return null;
        }

        /** {@inheritDoc} */
        @Override
        public void modifiedBundle(final Bundle bundle, BundleEvent event, ConfigurableWAB configurable) {
            //No-op, we don't mind about changes between starting/active
        }

        /** {@inheritDoc} */
        @Override
        public void removedBundle(Bundle bundle, BundleEvent event, ConfigurableWAB configurable) {
            removeWAB(configurable);
        }

        private void removeWAB(ConfigurableWAB configurable) {
            try {
                //The installer only needs to forget about this wab..
                //the remaining actions are now handled by the WAB itself.
                WAB wab = configurable.wabRef.get();
                if (wab == null) {
                    return;
                }
                wabLifecycleDebug("Primary WAB Tracker processing shutdown for ", wab);

                //this WAB is being removed
                synchronized (knownPaths) {
                    WABPathSpecificItemHolder holder = knownPaths.get(wab.getContextRoot());
                    if (holder != null) {
                        synchronized (holder) {
                            holder.getWABs().remove(wab);
                        }
                    }
                }
                synchronized (wabsEligibleForCollisionResolution) {
                    WABPathSpecificItemHolder eligibleWab = wabsEligibleForCollisionResolution.get(wab.getContextRoot());
                    if (eligibleWab != null) {
                        synchronized (eligibleWab) {
                            eligibleWab.getWABs().remove(wab);
                        }
                    }
                }
            } finally {
                // always be sure to unconfigure the WAB bundle
                unconfigureWABBundle(configurable);
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
         */
        @Override
        public AtomicReference<ConfigurableWAB> addingService(ServiceReference<WABConfiguration> ref) {
            return configurableWAB(new AtomicReference<ConfigurableWAB>(), ref);
        }

        private AtomicReference<ConfigurableWAB> configurableWAB(AtomicReference<ConfigurableWAB> configurableWABref, ServiceReference<WABConfiguration> ref) {
            // getting the configurable WAB here may result in creating the WAB if all the ingredients are available.
            ConfigurableWAB configurableWAB = createConfigurableWAB(ref);
            // we use an AtomicReference here because the actual name we are tracking could change if the service props
            // are modified.  So we cannot just track the immutable ConfigurableWAB.
            configurableWABref.set(configurableWAB);
            return configurableWABref;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
         */
        @Override
        public void modifiedService(ServiceReference<WABConfiguration> ref, AtomicReference<ConfigurableWAB> tracked) {
            // modifying involves a remove and then add operation to keep things 'simple'
            unconfigureContextPath(tracked.get());
            // must check that the service is still registered since removing the service could have caused something to
            // unregister while stopping the associated WAB
            if (ref.getBundle() != null) {
                // add back to re-configure based on the latest properties
                configurableWAB(tracked, ref);
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
         */
        @Override
        public void removedService(ServiceReference<WABConfiguration> ref, AtomicReference<ConfigurableWAB> tracked) {
            unconfigureContextPath(tracked.get());
        }
    }

    Future<?> executeRunnable(Runnable runnable) {
        ExecutorService executor = executorService.getService();
        //if there is no executor we can't do the work
        if (executor != null)
            return executor.submit(runnable);
        return null;
    }

    void postFailureEvent(WAB wab, String throwableKey, Object... args) {
        postEvent(wab.createFailedEvent(new Exception(TraceNLS.getFormattedMessage(getClass(), tc.getResourceBundleName(), throwableKey, args, null))));
    }

    void postEvent(Event e) {
        if (e != null) {
            EventAdmin eventAdmin = eventAdminService.getService();
            if (eventAdmin != null) {
                eventAdmin.postEvent(e);
            }
        }
    }

    public void attemptRedeployOfPreviouslyCollidedContextPath(String contextPath) {
        wabLifecycleDebug("Primary WAB Tracker performing collision resolution for ", contextPath);

        //tidy up the known wabs for this context path
        WABPathSpecificItemHolder knownHolder;
        synchronized (knownPaths) {
            knownHolder = knownPaths.get(contextPath);
            if (knownHolder != null) {
                synchronized (knownHolder) {
                    if (knownHolder.getWABs().isEmpty()) {
                        //there are no more wabs for this context path
                        knownPaths.remove(contextPath);
                    }
                }
            }
        }

        //tidy up the eligible for resolution wabs for this context path
        WABPathSpecificItemHolder holder;
        synchronized (wabsEligibleForCollisionResolution) {
            holder = wabsEligibleForCollisionResolution.get(contextPath);
            if (holder != null) {
                synchronized (holder) {
                    if (holder.getWABs().isEmpty()) {
                        //there are no more wabs for this context path
                        wabsEligibleForCollisionResolution.remove(contextPath);
                        holder = null;
                    }
                }
            }
        }

        //if we found a holder when looking, then there are wabs we can use
        //for resolution..
        if (holder != null) {
            WAB collidingWabToDeploy = null;

            synchronized (holder) {
                Collection<WAB> wabs = holder.getWABs();
                //if there were colliding bundles, we can now try to deploy
                //the next one since one has undeployed
                //we can now try to deploy the next one
                long lowestId = Long.MAX_VALUE;
                for (WAB w : wabs) {
                    long id = w.getBundle().getBundleId();
                    if (id < lowestId) {
                        lowestId = id;
                        collidingWabToDeploy = w;
                    }
                }
                //DO NOT remove the selected wab from the set eligible for redeploy
                //otherwise we'll be unable to report collisions messages.
                //(comment left here after I already made this change twice.. doh)
            }
            if (collidingWabToDeploy != null) {
                wabLifecycleDebug("Primary WAB Tracker selected WAB to deploy from collision resolution ", collidingWabToDeploy);
                collidingWabToDeploy.attemptDeployOfPreviouslyBlockedWab();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleEvent(Event event) {
        //this EventHandler is listening for org/osgi/service/web/UNDEPLOYED events
        //when a WAB is undeployed from the webcontainer, the context root can safely
        //be removed if it was the path and bundle we were expecting
        String contextPath = (String) event.getProperty("context.path");
        attemptRedeployOfPreviouslyCollidedContextPath(contextPath);
    }

    private Container getContainerForBundle(WAB wab, Bundle bundle, String contextRoot) {

        //for a bundle, we can use the bundles own private data storage as the cache..
        File cacheDir = bundle.getDataFile("cache");
        if (!FileUtils.ensureDirExists(cacheDir)) {
            postFailureEvent(wab, "wab.install.fail.cache", bundle, contextRoot);
            Tr.error(tc, "wab.install.fail.cache", bundle, contextRoot);
            return null;
        }
        File cacheDirAdapt = bundle.getDataFile("cacheAdapt");
        if (!FileUtils.ensureDirExists(cacheDirAdapt)) {
            postFailureEvent(wab, "wab.install.fail.adapt", bundle, contextRoot);
            Tr.error(tc, "wab.install.fail.adapt", bundle, contextRoot);
            return null;
        }
        File cacheDirOverlay = bundle.getDataFile("cacheOverlay");
        if (!FileUtils.ensureDirExists(cacheDirOverlay)) {
            postFailureEvent(wab, "wab.install.fail.overlay", bundle, contextRoot);
            Tr.error(tc, "wab.install.fail.overlay", bundle, contextRoot);
            return null;
        }
        // Create an artifact API and adaptable Container implementation for the bundle
        ArtifactContainer artifactContainer = containerFactorySRRef.getService().getContainer(cacheDir, bundle);
        Container wabContainer = adaptableModuleFactorySRRef.getService().getContainer(cacheDirAdapt, cacheDirOverlay, artifactContainer);
        return wabContainer;
    }

    private static class WABContainerInfo implements ContainerInfo {
        private final String entryName;
        private final Container entryContainer;

        WABContainerInfo(String entryName, Container entryContainer) {
            this.entryName = entryName;
            this.entryContainer = entryContainer;
        }

        @Override
        public Type getType() {
            return Type.WEB_INF_LIB;
        }

        @Override
        public String getName() {
            return this.entryName;
        }

        @Override
        public Container getContainer() {
            return this.entryContainer;
        }

        @Override
        public String toString() {
            return "[WEB_INF_LIB]" + entryName + ":" + entryContainer;
        }
    }

    private static class WABClassesInfo implements WebModuleClassesInfo {
        private final List<ContainerInfo> containerInfos;

        WABClassesInfo(List<ContainerInfo> containerInfos) {
            this.containerInfos = containerInfos;
        }

        @Override
        public List<ContainerInfo> getClassesContainers() {
            return containerInfos;
        }

        @Override
        public String toString() {
            return this.containerInfos.toString();
        }
    }

    private List<ContainerInfo> getContainerInfosForBundle(Bundle b, Container rootContainer) throws UnableToAdaptException {
        Dictionary<String, String> headers = b.getHeaders("");
        String classPath = headers.get(org.osgi.framework.Constants.BUNDLE_CLASSPATH);

        if (classPath == null || classPath.trim().equals("")) {
            classPath = ".";
        }

        //TODO: use a proper classpath parser...
        //this is a bit crude.. we can improve it
        String classPathElements[] = classPath.split(",");

        List<ContainerInfo> cpContainers = new ArrayList<ContainerInfo>();
        for (String cpElt : classPathElements) {
            cpElt = cpElt.trim();
            final String entryName = cpElt;
            final Container entryContainer;

            if (".".equals(cpElt)) {
                entryContainer = rootContainer;
            } else {
                Entry e = rootContainer.getEntry(cpElt);
                if (e == null) {
                    continue;
                }
                final Container cpContainer = e.adapt(Container.class);
                if (cpContainer == null) {
                    continue;
                }
                entryContainer = cpContainer;
            }
            cpContainers.add(new WABContainerInfo(entryName, entryContainer));
        }
        return cpContainers;
    }

    void removeWabFromEligibleForCollisionResolution(WAB wab) {
        synchronized (wabsEligibleForCollisionResolution) {
            String contextPath = wab.getContextRoot();
            WABPathSpecificItemHolder holder = wabsEligibleForCollisionResolution.get(contextPath);
            if (holder != null) {
                synchronized (holder) {
                    holder.getWABs().remove(wab);
                }
            }

        }
    }

    private WebModuleClassesInfo getClassesInfo(WAB wab, Container wabContainer, Bundle bundle, String contextRoot, BundleWiring wiring) throws UnableToAdaptException {
        List<ContainerInfo> containerInfos = getContainerInfos(wab, wabContainer, bundle, contextRoot, wiring);
        for (WABClassInfoHelper helper : classInfoHelpers) {
            containerInfos.addAll(helper.getContainerInfos(wabContainer, bundle));
        }
        WebModuleClassesInfo classesInfo = new WABClassesInfo(containerInfos);
        return classesInfo;
    }

    private List<ContainerInfo> getContainerInfos(WAB wab, Container wabContainer, Bundle bundle, String contextRoot, BundleWiring wiring) throws UnableToAdaptException {

        List<ContainerInfo> cpContainers = getContainerInfosForBundle(bundle, wabContainer);

        List<BundleWire> bwl = wiring.getProvidedWires(BundleRevision.HOST_NAMESPACE);
        if (bwl != null) {
            for (BundleWire bw : bwl) {
                BundleWiring rw = bw.getRequirerWiring();
                if (rw != null) {
                    BundleRevision br = rw.getRevision();
                    if (br != null) {
                        Bundle fragment = br.getBundle();
                        if (fragment != null) {
                            Container fragContainer = getContainerForBundle(wab, fragment, contextRoot);
                            if (fragContainer != null) {
                                List<ContainerInfo> fragContainers = getContainerInfosForBundle(fragment, fragContainer);
                                cpContainers.addAll(fragContainers);
                            }
                        }
                    }
                }
            }
        }

        return cpContainers;
    }

    private String WASContextPathNormalize(String contextPath) {
        //this is how websphere manipulates the context root repeatedly internally
        //we have to emulate this, as the webapp we see will have been processed,
        //where as the webapp we have is raw from the bundle.
        if (!contextPath.startsWith("/"))
            contextPath = "/" + contextPath;
        if (contextPath.endsWith("/") && !contextPath.equals("/"))
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        return contextPath;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    @Override
    public ExtensionProcessor createExtensionProcessor(IServletContext iServletContext) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "WAB createExtensionProcessor", iServletContext.getContextPath());
        Map<String, WABPathSpecificItemHolder> knownPaths = this.knownPaths;
        if (knownPaths != null) {
            //get the known wabs for this path
            WABPathSpecificItemHolder holder;
            synchronized (knownPaths) {
                holder = knownPaths.get(iServletContext.getContextPath());
            }
            if (holder != null) {
                Collection<WAB> wabs = holder.getWABs();
                WAB wab = null;
                synchronized (holder) {
                    //we might have a long list
                    for (WAB w : wabs) {
                        switch (w.getState()) {
                            case DEPLOYING:
                            case DEPLOYED:
                                wab = w;
                                break;
                            default:
                                break;
                        }
                    }
                }
                if (wab != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Found WAB matching context path", iServletContext.getContextPath());
                    BundleContext wabBC = wab.getBundle().getBundleContext();
                    //Register the BundleContext as an attribute on the ServletContext
                    iServletContext.setAttribute("osgi-bundlecontext", wabBC);
                    // For Spring DM based applications register BundleContext under attribute that
                    // org.springframework.osgi.web.context.support.OsgiBundleXmlWebApplicationContext looks for.
                    iServletContext.setAttribute("org.springframework.osgi.web." + BundleContext.class.getName(), wabBC);
                    //add the OSGi directory protection filter
                    //Add dynamic filter to prevent access to OSGI-INF and OSGI-OPT
                    ExtendedServletContext esc = iServletContext;
                    IFilterConfig fc = esc.getFilterConfig("com.ibm.ws.app.manager.wab.internal.WABInstaller.OSGIDENYCONFIG"); //name is just a unique string.
                    fc.setDescription("Filter protecting OSGI-INF and OSGI-OPT resources");
                    fc.setName("com.ibm.ws.app.manager.wab.internal.OsgiDirectoryProtectionFilter"); // DO NOT ALLOW TO BE NULL!! (will break WAS)
                    fc.setFilterClassLoader(this.getClass().getClassLoader());
                    fc.setFilterClassName(OsgiDirectoryProtectionFilter.class.getName()); //The new filter class..
                    fc.setDisplayName("OSGI Directory protection filter");
                    fc.setDispatchMode(new int[] { IFilterConfig.FILTER_ERROR, IFilterConfig.FILTER_FORWARD, IFilterConfig.FILTER_INCLUDE, IFilterConfig.FILTER_REQUEST });
                    fc.setAsyncSupported(true); //Added so that the filter can support Async servlet functionality.  It doesn't affect behaviour for Sync Servlets.
                    esc.addMappingFilter("/*", fc);
                    //iServletContext.addFilter("/*", new OsgiDirectoryProtectionFilter());
                    //Register the ServletContext in the service registry
                    wab.registerServletContext(iServletContext);
                }
                return null;
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No WABS matching context path", iServletContext.getContextPath());
                    Tr.debug(tc, "Known WAB context paths", knownPaths);
                }
                return null;
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Not initialized with knownPaths");
            }
            return null;
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public List getPatternList() {
        return Collections.EMPTY_LIST;
    }

    final class WABPathSpecificItemHolder {
        private final Collection<WAB> wabs = new ArrayList<WAB>();

        WABPathSpecificItemHolder(WAB wab) {
            wabs.add(wab);
        }

        Collection<WAB> getWABs() {
            return wabs;
        }

    }

    static final class WABDeployedAppInfo extends SimpleDeployedAppInfoBase {
        WABDeployedAppInfo(DeployedAppServices deployedAppServices, ExtendedApplicationInfo appInfo) throws UnableToAdaptException {
            super(deployedAppServices);
            super.appInfo = appInfo;
        }

        WAB currentWAB;

        ModuleContainerInfo createModuleContainerInfo(ModuleHandler webModuleHandler, String contextRoot, Container moduleContainer,
                                                      String moduleLocation, ClassLoader loader) throws UnableToAdaptException {
            final ClassLoader moduleClassLoader = loader;
            final ModuleClassLoaderFactory moduleClassLoaderFactory = new ModuleClassLoaderFactory() {
                @Override
                public ClassLoader createModuleClassLoader(ModuleInfo moduleInfo, List<ContainerInfo> moduleClassesContainers) {
                    return moduleClassLoader;
                }
            };
            String moduleURI = ModuleInfoUtils.getModuleURIFromLocation(moduleLocation);
            WebModuleContainerInfo mci = new WebModuleContainerInfo(webModuleHandler, deployedAppServices.getModuleMetaDataExtenders("web"), deployedAppServices.getNestedModuleMetaDataFactories("web"), moduleContainer, null, moduleURI, moduleClassLoaderFactory, moduleClassesInfo, contextRoot);
            moduleContainerInfos.add(mci);
            return mci;
        }

        @Override
        public DeployedModuleInfo getDeployedModule(ExtendedModuleInfo moduleInfo) {
            DeployedModuleInfo deployedMod = super.getDeployedModule(moduleInfo);
            currentWAB.setDeployedModuleInfo(deployedMod);

            WebAppConfiguration appConfig = (WebAppConfiguration) ((WebModuleMetaData) moduleInfo.getMetaData()).getConfiguration();
            if (appConfig != null) {
                String virtualHost = currentWAB.getVirtualHost();
                if (virtualHost != null) {
                    appConfig.setVirtualHostName(virtualHost);
                }
            }
            return deployedMod;
        }

        public boolean installApp(WAB wab) {
            currentWAB = wab;
            Future<Boolean> result = futureMonitor.createFuture(Boolean.class);
            return installApp(result);
        }

        public boolean installModule(WAB wab) throws MetaDataException, InterruptedException, ExecutionException {
            WebModuleContainerInfo mci = (WebModuleContainerInfo) wab.getModuleContainerInfo();
            currentWAB = wab;
            mci.createModuleMetaData(appInfo, this);
            DeployedModuleInfo deployedMod = getDeployedModule(mci.moduleInfo);

            //deploy the module
            Future<Boolean> appFuture = mci.moduleHandler.deployModule(deployedMod, this);
            return !appFuture.isDone() || appFuture.get();
        }

        public void uninstallApp(WAB wab) {
            uninstallApp();
        }

        public void uninstallModule(WAB wab) {
            WebModuleContainerInfo mci = (WebModuleContainerInfo) wab.getModuleContainerInfo();
            DeployedModuleInfo deployedMod = wab.getDeployedModuleInfo();
            mci.moduleHandler.undeployModule(deployedMod);
        }

        public String getModuleName(ModuleContainerInfo mci) {
            return ((WebModuleContainerInfo) mci).moduleInfo.getName();
        }
    }

    @Override
    public void serverStopping() {
        quiesceStarted.set(true);
    }

    void addSystemWABDeployFuture(WAB wab, Future<?> future) {
        if (serverStarted != null) {
            return;
        }
        EbaProvider ebaProvider = ebaProviderTracker.getService();
        if (ebaProvider != null && ebaProvider.getApplicationInfo(wab.getBundle()) != null) {
            return;
        }
        // This is a system wab, not from an OSGi application EBA.
        systemWABsDeploying.put(wab, future);
    }

    @Override
    public void check() {
        AtomicBoolean timeoutOccurred = new AtomicBoolean();
        systemWABsDeploying.forEach((w, f) -> {
            try {
                if (timeoutOccurred.get()) {
                    if (!f.isDone()) {
                        logSlowWab(w);
                    }
                } else {
                    f.get(ApplicationStateCoordinator.getApplicationStartTimeout(), TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                // FFDC here and keep interruption
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // FFDC here and continue
            } catch (TimeoutException e) {
                timeoutOccurred.set(true);
                logSlowWab(w);
            }
        });
        systemWABsDeploying.clear();
    }

    private void logSlowWab(WAB w) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Stopped waiting for the system WAB to start: ", w.getBundle().getSymbolicName());
        }
    }
}
