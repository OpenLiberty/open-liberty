/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.AttributeChangeNotification;
import javax.management.DynamicMBean;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.StandardEmitterMBean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.application.ApplicationMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.app.manager.AppMessageHelper;
import com.ibm.ws.app.manager.ApplicationManager;
import com.ibm.ws.app.manager.ApplicationStateCoordinator;
import com.ibm.ws.app.manager.ApplicationStateCoordinator.AppStatus;
import com.ibm.ws.app.manager.internal.lifecycle.ServiceReg;
import com.ibm.ws.app.manager.internal.monitor.AppMonitorConfigurator;
import com.ibm.ws.app.manager.internal.statemachine.ApplicationStateMachine;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.application.Application;
import com.ibm.wsspi.application.ApplicationState;
import com.ibm.wsspi.application.handler.ApplicationHandler;
import com.ibm.wsspi.application.handler.ApplicationTypeSupported;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleContext;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;
import com.ibm.wsspi.logging.Introspector;

/**
 *
 */
@Component(service = { ManagedServiceFactory.class, Introspector.class, RuntimeUpdateListener.class, ApplicationRecycleCoordinator.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = {
                        Constants.SERVICE_VENDOR + "=" + "IBM",
                        Constants.SERVICE_PID + "=" + AppManagerConstants.APPLICATIONS_PID
           })
public class ApplicationConfigurator implements ManagedServiceFactory, Introspector, RuntimeUpdateListener, ApplicationRecycleCoordinator {
    private static final TraceComponent _tc = Tr.register(ApplicationConfigurator.class);

    /**
     * An instance of this class exists for each running application
     */
    private static class NamedApplication implements ApplicationStateMachine.ASMHelper {
        private final String appName;
        private final String configPid;
        private final BundleContext bundleContext;
        private final NotificationBroadcasterSupport mbeanNotifier;
        private final AtomicLong sequence = new AtomicLong();
        private ApplicationConfig appConfig;
        private ApplicationStateMachine asm;
        private ApplicationTypeSupport appTypeSupport;
        private ApplicationRecycleContext appRecycleContext;
        private final AtomicReference<ApplicationState> appStateRef = new AtomicReference<ApplicationState>();
        private final ServiceReg<Application> appReg = new ServiceReg<Application>();
        private final ServiceReg<DynamicMBean> mbeanReg = new ServiceReg<DynamicMBean>();
        private final AtomicReference<DynamicMBean> dynamicMBean = new AtomicReference<DynamicMBean>();

        NamedApplication(String appName, ApplicationConfig appConfig, BundleContext bundleContext, ExecutorService executor) {
            this.appName = appName;
            this.configPid = appConfig.getConfigPid();
            this.appConfig = appConfig;
            this.bundleContext = bundleContext;
            // TODO resolve the description using NLS.
            MBeanNotificationInfo info = new MBeanNotificationInfo(new String[] { AttributeChangeNotification.class.getName() }, AttributeChangeNotification.class.getName(), "");

            MBeanNotificationInfo infoNotification = new MBeanNotificationInfo(new String[] { Notification.class.getName() }, Notification.class.getName(), "");

            mbeanNotifier = new NotificationBroadcasterSupport(executor, info, infoNotification);

            appConfig.setMBeanNotifier(mbeanNotifier);
        }

        public String getAppName() {
            return appName;
        }

        public String getConfigPid() {
            return configPid;
        }

        public ApplicationConfig getConfig() {
            return appConfig;
        }

        public ApplicationTypeSupport getTypeSupport() {
            return appTypeSupport;
        }

        public ApplicationRecycleContext getRecycleContext() {
            return appRecycleContext;
        }

        public ApplicationStateMachine getStateMachine() {
            return asm;
        }

        public boolean isConfigured() {
            return appStateRef.get() != null;
        }

        public void setConfig(ApplicationConfig appConfig) {
            this.appConfig = appConfig;
        }

        public void setTypeSupport(ApplicationTypeSupport appTypeSupport) {
            this.appTypeSupport = appTypeSupport;
        }

        public void setRecycleContext(ApplicationRecycleContext appRecycleContext) {
            this.appRecycleContext = appRecycleContext;
        }

        public void setStateMachine(ApplicationStateMachine asm) {
            this.asm = asm;
        }

        public void unregisterServices() {
            if (dynamicMBean.getAndSet(null) != null) {
                appReg.unregister();
                mbeanReg.unregister();
            }
        }

        public void describe(StringBuilder sb) {
            appConfig.describe(sb);
            ApplicationStateMachine asm = getStateMachine();
            if (asm != null) {
                asm.describe(sb);
            }
        }

        private void register(ApplicationConfig appConfig) {
            final String pid = appConfig.getConfigPid();

            final Application appService = new Application() {
                @Override
                public Future<Boolean> start() {
                    return asm.start();
                }

                @Override
                public Future<Boolean> stop() {
                    return asm.stop();
                }

                @Override
                public void restart() {
                    asm.restart();
                }

                @Override
                public ApplicationState getState() {
                    return appStateRef.get();
                }
            };

            Map<String, ?> serviceProperties = appConfig.getServiceProperties();
            appReg.setProperties(serviceProperties);
            appReg.setProperty("application.state", appStateRef.get());
            appReg.register(bundleContext, Application.class, appService);

            ApplicationMBean appMBean = new ApplicationMBean() {
                @Override
                public String getState() {
                    return appService.getState().toString();
                }

                @Override
                public String getPid() {
                    return pid;
                }

                @Override
                public void start() {
                    appService.start();
                }

                @Override
                public void stop() {
                    appService.stop();
                }

                @Override
                public void restart() {
                    appService.restart();
                }
            };

            DynamicMBean mbean = new StandardEmitterMBean(appMBean, ApplicationMBean.class, mbeanNotifier);

            // Need to clone the old props as per the OSGi spec.
            mbeanReg.setProperties(serviceProperties);
            mbeanReg.setProperty("application.state", appStateRef.get());

            if (mbeanReg.setProperty("jmx.objectname", appConfig.getMBeanName())) {
                mbeanReg.unregister();
            }
            mbeanReg.register(bundleContext, DynamicMBean.class, mbean);
            dynamicMBean.set(mbean);
        }

        @Override
        public void switchApplicationState(ApplicationConfig appConfig, ApplicationState newAppState) {

            if (appStateRef.compareAndSet(null, ApplicationState.INSTALLED)) {
                if (appConfig != null) {
                    // appConfig == null here can only mean that we are removing an application that never got beyond INITIAL state
                    register(appConfig);
                }
            }

            ApplicationState oldAppState = appStateRef.getAndSet(newAppState);
            if (oldAppState != newAppState) {
                appReg.setProperty("application.state", newAppState);
                mbeanReg.setProperty("application.state", newAppState);

                String mbeanName = appConfig.getMBeanName();
                // Fire the correct event (State is capital S)
                mbeanNotifier.sendNotification(new AttributeChangeNotification(mbeanName, sequence.incrementAndGet(), System.currentTimeMillis(), "", "State", "java.lang.String", oldAppState.toString(), newAppState.toString()));

                // Fire the old behavior event (state is lower-case s). This is so we don't break existing applications using our API.
                mbeanNotifier.sendNotification(new AttributeChangeNotification(mbeanName, sequence.incrementAndGet(), System.currentTimeMillis(), "", "state", "java.lang.String", oldAppState.toString(), newAppState.toString()));
            }
        }

        @Override
        public boolean appTypeSupported() {
            return appTypeSupport != null && appTypeSupport.isSupported();
        }

        @Override
        public void notifyAppStarted(String pid) {
            ApplicationStateCoordinator.updateStartingAppStatus(pid, ApplicationStateCoordinator.AppStatus.STARTED);
        }

        @Override
        public void notifyAppFailed(String pid) {
            ApplicationStateCoordinator.updateStartingAppStatus(pid, ApplicationStateCoordinator.AppStatus.FAILED);
            ApplicationStateCoordinator.updateStoppingAppStatus(pid, ApplicationStateCoordinator.AppStatus.FAILED);
        }
    }

    private final Map<String, NamedApplication> _appFromName = new HashMap<String, NamedApplication>();
    private final Map<String, NamedApplication> _appFromPid = new HashMap<String, NamedApplication>();
    private volatile Set<NamedApplication> _appsToShutdown;

    private final Map<String, ApplicationConfig> _blockedConfigFromPid = new HashMap<String, ApplicationConfig>();
    private final Map<String, List<String>> _blockedPidsFromName = new HashMap<String, List<String>>();
    private final Map<String, List<ApplicationDependency>> _startAfterDependencies = new HashMap<String, List<ApplicationDependency>>();

    private final Set<String> reportedCycles = new HashSet<String>();

    /**
     * An instance of this class exists with each type of application we have
     * encountered. We keep track of the presence of support for that type
     * of application and if available the handler for that app type. When
     * we have applications available for one of these types which is still
     * waiting for it's app handler to be registered those apps are kept in
     * the waitingApps set.
     */
    private static class ApplicationTypeSupport {
        private volatile boolean supported;
        private volatile ApplicationHandler<?> appHandler;
        private Set<NamedApplication> waitingApps;

        ApplicationTypeSupport(boolean supported) {
            this.supported = supported;
        }

        public boolean isSupported() {
            return supported;
        }

        public void setSupported(boolean supported) {
            this.supported = supported;
            notifyWaitingApps();
        }

        public ApplicationHandler<?> getHandler() {
            return appHandler;
        }

        public void setHandler(ApplicationHandler<?> appHandler) {
            this.appHandler = appHandler;
            notifyWaitingApps();
        }

        private void notifyWaitingApps() {
            if (supported && appHandler != null && waitingApps != null) {
                Set<NamedApplication> apps = waitingApps;
                waitingApps = null;
                for (NamedApplication app : apps) {
                    if (app.getTypeSupport() != this) {
                        // TODO the config of the app in the list changed the type but the app was
                        // still listed as waiting for the handler for the old type
                        continue;
                    }
                    ApplicationStateMachine asm = app.getStateMachine();
                    if (asm != null) {
                        asm.setAppHandler(appHandler);
                    }
                }
            }
        }

        public void addWaitingApp(NamedApplication app) {
            if (waitingApps == null) {
                waitingApps = new HashSet<NamedApplication>();
            }
            waitingApps.add(app);
        }
    }

    private final Map<String, ApplicationTypeSupport> _appTypeSupport = new HashMap<String, ApplicationTypeSupport>();

    private volatile BundleContext _ctx;

    private volatile ApplicationDependency _appManagerRARSupportDependency;
    private volatile ApplicationDependency _appManagerReadyDependency;

    // access to this is synchronized
    private UpdateEpisodeState _currentEpisode;

    // If a context is unregistered, the corresponding components should all be
    // unregistered.  However, there is no guarantee we will receive the unset
    // events in a specific order, so we keep an entry in this map until the
    // context and all its components have been unset.

    @Trivial
    private static class ApplicationRecycleContextState {
        ApplicationRecycleContext context;
        final Set<ApplicationRecycleComponent> components = new LinkedHashSet<ApplicationRecycleComponent>();

        public boolean isEmpty() {
            return context == null && components.isEmpty();
        }
    }

    private final Map<ApplicationRecycleContext, ApplicationRecycleContextState> _appRecycleMap = new HashMap<ApplicationRecycleContext, ApplicationRecycleContextState>();

    //
    // DS dependencies
    //
    private volatile ConfigurationAdmin _configAdmin;
    private volatile WsLocationAdmin _locAdmin;
    private volatile FutureMonitor _futureMonitor;
    private volatile RuntimeUpdateManager _runtimeUpdateManager;
    private volatile ArtifactContainerFactory _artifactFactory;
    private volatile AdaptableModuleFactory _moduleFactory;
    private volatile ExecutorService _executor;
    private volatile ScheduledExecutorService _scheduledExecutor;
    private volatile AppMonitorConfigurator _appMonitorConfigurator;
    private volatile ApplicationManager _applicationManager;

    private static final Collection<String> SIMPLE_INITIAL_UPDATE_NOTIFICATIONS = Arrays.asList(new String[] { RuntimeUpdateNotification.FEATURE_UPDATES_COMPLETED,
                                                                                                               RuntimeUpdateNotification.CONFIG_UPDATES_DELIVERED,
                                                                                                               RuntimeUpdateNotification.ORB_STARTED });

    @Activate
    protected void activate(ComponentContext ctx) {
        _appManagerRARSupportDependency = createDependency("resolves when either support for type=rar applications is registered or we are ready for apps to start");
        if (_appTypeSupport.get("rar") != null) {
            _appManagerRARSupportDependency.setResult(true);
        }
        _appManagerReadyDependency = createDependency("resolves when we are ready for apps to start");
        synchronized (this) {
            if (FrameworkState.isStopping()) {
                // we are stopping so bail out
                return;
            }
            //Each call to joinEpisode is counted and the future completion reduces the count.  This initial call establishes the episde
            // and it is used by the appManagerReadyDependency, at the end of this method. This order prevents the episode from completing
            // before being completely set up.
            final UpdateEpisodeState episode = joinEpisode();
            if (episode == null) {
                return;
            }
            for (String initialNotificationName : SIMPLE_INITIAL_UPDATE_NOTIFICATIONS) {
                RuntimeUpdateNotification notification = _runtimeUpdateManager.getNotification(initialNotificationName);
                if (notification != null) {
                    joinEpisode().createSimpleDependency(notification.getFuture());
                }
            }
            _appManagerReadyDependency.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    episode.dropReference();
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    episode.dropReference();
                }
            });
        }
        ApplicationStateCoordinator.setApplicationConfigurator(this);

        _ctx = ctx.getBundleContext();
    }

    @Modified
    protected void modified(ComponentContext ctx) {
        _ctx = ctx.getBundleContext();
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        _ctx = null;
        _appManagerReadyDependency = null;
        _appManagerRARSupportDependency = null;
        final Set<NamedApplication> appsToStop;

        synchronized (this) {
            appsToStop = new HashSet<NamedApplication>(_appFromName.values());
            _appFromName.clear();
            _appFromPid.clear();
            _blockedConfigFromPid.clear();
            _blockedPidsFromName.clear();
            _appTypeSupport.clear();
        }
        for (NamedApplication app : appsToStop) {
            uninstallApp(app);
        }

        synchronized (this) {
            UpdateEpisodeState episode = _currentEpisode;
            if (episode != null) {
                episode.deactivate();
                leaveEpisode();
            }
        }
        ApplicationStateCoordinator.setApplicationConfigurator(null);
    }

    @Reference
    protected void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        _configAdmin = configurationAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
        _configAdmin = null;
    }

    @Reference(name = "locationService", service = WsLocationAdmin.class)
    protected void setLocationService(WsLocationAdmin locationService) {
        _locAdmin = locationService;
    }

    protected void unsetLocationService(WsLocationAdmin locationService) {
        _locAdmin = null;
    }

    @Reference(service = FutureMonitor.class)
    protected void setFutureMonitor(FutureMonitor futureMonitor) {
        _futureMonitor = futureMonitor;
    }

    protected void unsetFutureMonitor(FutureMonitor futureMonitor) {
        _futureMonitor = null;
    }

    @Reference(service = RuntimeUpdateManager.class)
    protected void setRuntimeUpdateManager(RuntimeUpdateManager runtimeUpdateManager) {
        _runtimeUpdateManager = runtimeUpdateManager;
    }

    protected void unsetRuntimeUpdateManager(RuntimeUpdateManager runtimeUpdateManager) {
        _runtimeUpdateManager = null;
    }

    @Reference(name = "containerFactory", service = ArtifactContainerFactory.class)
    protected void setContainerFactory(ArtifactContainerFactory containerFactory) {
        _artifactFactory = containerFactory;
    }

    protected void unsetContainerFactory(ArtifactContainerFactory containerFactory) {
        _artifactFactory = null;
    }

    @Reference(service = AdaptableModuleFactory.class)
    protected void setAdaptableModuleFactory(AdaptableModuleFactory adaptableModuleFactory) {
        _moduleFactory = adaptableModuleFactory;
    }

    protected void unsetAdaptableModuleFactory(AdaptableModuleFactory adaptableModuleFactory) {
        _moduleFactory = null;
    }

    @Reference(name = "executorService", service = ExecutorService.class)
    protected void setExecutorService(ExecutorService executorService) {
        _executor = executorService;
    }

    protected void unsetExecutorService(ExecutorService executorService) {
        _executor = null;
    }

    @Reference(name = "scheduledExecutorService", service = ScheduledExecutorService.class)
    protected void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        _scheduledExecutor = scheduledExecutorService;
    }

    protected void unsetScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        _scheduledExecutor = null;
    }

    @Reference(service = AppMonitorConfigurator.class)
    protected void setApplicationMonitorConfigurator(AppMonitorConfigurator appMonitorConfigurator) {
        _appMonitorConfigurator = appMonitorConfigurator;
    }

    protected void unsetApplicationMonitorConfigurator(AppMonitorConfigurator appMonitorConfigurator) {
        _appMonitorConfigurator = null;
    }

    @Reference(name = "appTypeSupported", service = ApplicationTypeSupported.class,
               cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setAppTypeSupported(ApplicationTypeSupported appTypeSupported, Map<String, ?> serviceProps) {
        // ignore services that don't have the property we require.
        Object o = serviceProps.get("type");
        if (o != null) {
            if (o instanceof String) {
                String appType = (String) o;
                registerAppType(appType);
            } else if (o instanceof String[]) {
                for (String appType : (String[]) o) {
                    registerAppType(appType);
                }
            }
        }
    }

    protected void unsetAppTypeSupported(ApplicationTypeSupported appTypeSupported, Map<String, ?> serviceProps) {
        // ignore services that don't have the property we require.
        Object o = serviceProps.get("type");
        if (o != null) {
            if (o instanceof String) {
                String appType = (String) o;
                unregisterAppType(appType);
            } else if (o instanceof String[]) {
                for (String appType : (String[]) o) {
                    unregisterAppType(appType);
                }
            }
        }
    }

    @Reference(name = "appHandler", service = ApplicationHandler.class,
               cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setAppHandler(ApplicationHandler<?> appHandler, Map<String, ?> serviceProps) {
        // ignore handlers that don't have the property we require.
        Object o = serviceProps.get("type");
        if (o != null) {
            if (o instanceof String) {
                String appType = (String) o;
                registerAppHandler(appType, appHandler);
            } else if (o instanceof String[]) {
                for (String appType : (String[]) o) {
                    registerAppHandler(appType, appHandler);
                }
            }
        }
    }

    protected void unsetAppHandler(ApplicationHandler<?> appHandler, Map<String, ?> serviceProps) {
        if (FrameworkState.isStopping()) {
            // we are stopping so bail out
            return;
        }

        // ignore handlers that don't have the property we require.
        Object o = serviceProps.get("type");
        if (o != null) {
            if (o instanceof String) {
                String appType = (String) o;
                unregisterAppHandler(appType, appHandler);
            } else if (o instanceof String[]) {
                for (String appType : (String[]) o) {
                    unregisterAppHandler(appType, appHandler);
                }
            }
        }
    }

    @Reference(name = "appRecycleContext", service = ApplicationRecycleContext.class,
               cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setAppRecycleContext(ApplicationRecycleContext appRecycleContext) {
        try {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, "setAppRecycleContext: context " + appRecycleContext);
            }

            final String appName = appRecycleContext.getAppName();
            synchronized (this) {
                final NamedApplication app = appName != null ? _appFromName.get(appName) : null;
                if (app != null) {
                    app.setRecycleContext(appRecycleContext);
                }

                ApplicationRecycleContextState contextState = _appRecycleMap.get(appRecycleContext);
                if (contextState == null) {
                    contextState = new ApplicationRecycleContextState();
                    _appRecycleMap.put(appRecycleContext, contextState);
                }

                contextState.context = appRecycleContext;
            }
        } finally {
            dumpApplications();
        }
    }

    protected void unsetAppRecycleContext(ApplicationRecycleContext appRecycleContext) {
        try {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, "unsetAppRecycleContext: context " + appRecycleContext);
            }

            if (FrameworkState.isStopping()) {
                // we are stopping so bail out
                return;
            }

            synchronized (this) {
                final UpdateEpisodeState episode = joinEpisode();
                if (episode == null) {
                    return;
                }
                episode.recycleAppContext(appRecycleContext);

                // When a context is removed, its components will also be
                // removed.  If this occurs outside the scope of an existing
                // runtime update episode (e.g., because a RAR is restarted due
                // to file monitor), we want to avoid repeatedly restarting an
                // application for the context and for each component.  We
                // accomplish this by deferring the call to dropContextReference
                // until all components have been unset for this context.  If
                // there are already no remaining components for this context,
                // then call dropContextReference now.  Note that repeated
                // application restarts will occur if components are unset
                // before the context is unset, but this should not occur
                // outside the scope of a server.xml episode in practice.
                ApplicationRecycleContextState state = _appRecycleMap.get(appRecycleContext);
                if (state != null) {
                    state.context = null;
                    if (state.isEmpty()) {
                        _appRecycleMap.remove(appRecycleContext);
                        episode.dropContextReference(appRecycleContext);
                    }
                } else {
                    if (_tc.isEventEnabled()) {
                        Tr.event(_tc, "unsetAppRecycleContext: context was not previously set");
                    }
                }
            }
        } finally {
            dumpApplications();
        }
    }

    @Reference(name = "appRecycleComponent", service = ApplicationRecycleComponent.class,
               cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setAppRecycleComponent(ApplicationRecycleComponent appRecycleComponent) {
        try {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, "setAppRecycleComponent: component " + appRecycleComponent + ", context " + appRecycleComponent.getContext());
            }
            ApplicationRecycleContext context = appRecycleComponent.getContext();
            if (context != null) {
                synchronized (this) {
                    ApplicationRecycleContextState contextState = _appRecycleMap.get(context);
                    if (contextState == null) {
                        contextState = new ApplicationRecycleContextState();
                        _appRecycleMap.put(context, contextState);
                    }

                    contextState.components.add(appRecycleComponent);
                }
            }
        } finally {
            dumpApplications();
        }
    }

    protected void unsetAppRecycleComponent(ApplicationRecycleComponent appRecycleComponent) {
        try {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, "unsetAppRecycleComponent: component " + appRecycleComponent + ", context " + appRecycleComponent.getContext());
            }

            if (FrameworkState.isStopping()) {
                // we are stopping so bail out
                return;
            }

            ApplicationRecycleContext context = appRecycleComponent.getContext();
            Set<String> dependentApplications = appRecycleComponent.getDependentApplications();

            synchronized (this) {
                final UpdateEpisodeState episode = joinEpisode();
                if (episode == null) {
                    return;
                }
                if (context != null) {
                    // If there are no remaining components for this context,
                    // then attempt to drop the context reference from the
                    // episode.  This will have no effect if the context has not
                    // already been unset within the episode.
                    ApplicationRecycleContextState state = _appRecycleMap.get(context);
                    if (state != null) {
                        state.components.remove(appRecycleComponent);
                        if (state.isEmpty()) {
                            _appRecycleMap.remove(context);
                            episode.dropContextReference(context);
                        }
                    } else {
                        if (_tc.isEventEnabled()) {
                            Tr.event(_tc, "unsetAppRecycleComponent: component context was not previously set");
                        }
                    }
                } else {
                    // It's possible to have orphan contexts that block episodes from ever completing. If the context is null, we need to
                    // remove all instances of this component from contexts in the appRecycleMap
                    if (_tc.isDebugEnabled()) {
                        Tr.debug(_tc, "unsetAppRecycleComponent: no context for component.");
                    }

                    Iterator<Entry<ApplicationRecycleContext, ApplicationRecycleContextState>> iter = _appRecycleMap.entrySet().iterator();
                    while (iter.hasNext()) {
                        Entry<ApplicationRecycleContext, ApplicationRecycleContextState> entry = iter.next();
                        ApplicationRecycleContextState state = entry.getValue();
                        if (state.components.remove(appRecycleComponent)) {
                            if (_tc.isDebugEnabled()) {
                                Tr.debug(_tc, "Removing component " + appRecycleComponent + " from appState map for context " + entry.getKey());
                            }
                            if (state.isEmpty()) {
                                if (_tc.isDebugEnabled()) {
                                    Tr.debug(_tc, "No components left for context " + entry.getKey() + ", removing from app recycle map.");
                                }
                                iter.remove();
                                episode.dropContextReference(entry.getKey());
                            }

                        }
                    }

                }

                Collection<NamedApplication> apps = getNamedApps(dependentApplications);
                if (apps != null && !apps.isEmpty()) {
                    episode.recycleApps(apps);
                }
                episode.dropReference();
            }
        } finally {
            dumpApplications();
        }
    }

    @Reference
    protected void setApplicationManager(ApplicationManager mgr) {
        _applicationManager = mgr;
    }

    protected void unsetApplicationManager(ApplicationManager mgr) {
        _applicationManager = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.cm.ManagedServiceFactory#getName()
     */
    @Override
    public String getName() {
        return "ApplicationConfigurator";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        if (_ctx == null || _appsToShutdown != null) {
            return;
        }
        if (properties == null) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Error while updating application configuration with pid: " + pid + ", no properties were provided");
            }
            return;
        }
        try {
            synchronized (this) {
                ApplicationConfig appConfig = new ApplicationConfig(pid, properties, _applicationManager);
                if (appConfig.getLocation() == null) {
                    if (appConfig.getName() == null) {
                        Tr.audit(_tc, "APPLICATION_NO_LOCATION_NO_NAME");
                    } else {
                        Tr.audit(_tc, "APPLICATION_NO_LOCATION", appConfig.getName());
                    }
                } else if (appConfig.getType() == null) {
                    Tr.audit(_tc, "APPLICATION_NO_TYPE", appConfig.getName(), appConfig.getLocation());
                }
                processUpdate(pid, appConfig);
            }
        } catch (Exception e) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Error while updating application configuration with pid: " + pid + ", Exception: " + e);
            }
        } finally {
            dumpApplications();
        }
    }

    @Override
    public void deleted(String pid) {
        if (_ctx == null) {
            return;
        }
        try {
            synchronized (this) {
                processDeletion(pid);
            }
        } catch (Exception e) {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Error while deleting application configuration with pid: " + pid + ", Exception: " + e);
            }
        } finally {
            dumpApplications();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorName()
     */
    @Override
    public String getIntrospectorName() {
        return "ApplicationConfigurator";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.Introspector#getIntrospectorDescription()
     */
    @Override
    public String getIntrospectorDescription() {
        return "ApplicationConfigurator";
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.logging.Introspector#introspect(java.io.PrintWriter)
     */
    @Override
    public void introspect(PrintWriter out) throws Exception {
        writeHeader("Applications", out);
        StringBuilder sb = new StringBuilder();
        for (NamedApplication app : getNamedApps()) {
            sb.append("  ");
            app.describe(sb);
            sb.append("\n\n");
        }
        out.print(sb.toString());
    }

    private void writeHeader(String header, PrintWriter writer) {
        writer.print("\n");
        writer.println(header);
        for (int i = header.length(); i > 0; i--) {
            writer.print("-");
        }
        writer.print("\n");
    }

    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (_ctx == null || _appsToShutdown != null) {
            return;
        }

        if (RuntimeUpdateNotification.APP_FORCE_RESTART.equals(notification.getName())) {
            synchronized (this) {
                final UpdateEpisodeState episode = joinEpisode();
                if (episode != null) {
                    episode.createAppForceRestartDependency(notification.getFuture(), updateManager.getNotification(RuntimeUpdateNotification.FEATURE_UPDATES_COMPLETED));
                }
            }
        }

        if (SIMPLE_INITIAL_UPDATE_NOTIFICATIONS.contains(notification.getName())) {
            synchronized (this) {
                final UpdateEpisodeState episode = joinEpisode();
                if (episode != null) {
                    episode.createSimpleDependency(notification.getFuture());
                }
            }
        }

    }

    @Override
    public void recycleApplications(Set<String> dependentApplications) {
        try {
            if (FrameworkState.isStopping()) {
                // we are stopping so bail out
                return;
            }

            synchronized (this) {
                // passing in a null here means to recycle all of the applications
                Collection<NamedApplication> apps;
                if (dependentApplications == null) {
                    apps = new HashSet<NamedApplication>(_appFromName.values());
                } else {
                    apps = getNamedApps(dependentApplications);
                }

                if (apps != null && !apps.isEmpty()) {
                    final UpdateEpisodeState episode = joinEpisode();
                    if (episode != null) {
                        episode.recycleApps(apps);
                        episode.dropReference();
                    }
                }
            }
        } finally {
            dumpApplications();
        }
    }

    private synchronized void registerAppType(String appType) {
        ApplicationTypeSupport typeSupport = _appTypeSupport.get(appType);
        if (typeSupport == null) {
            typeSupport = new ApplicationTypeSupport(true);
            _appTypeSupport.put(appType, typeSupport);
        } else {
            typeSupport.setSupported(true);
        }
        if ("rar".equals(appType)) {
            ApplicationDependency current = _appManagerRARSupportDependency;
            if (current != null) {
                current.setResult(true);
            }
        }
    }

    private synchronized void unregisterAppType(String appType) {
        if (FrameworkState.isStopping()) {
            // we are stopping so bail out
            return;
        }

        ApplicationTypeSupport typeSupport = _appTypeSupport.get(appType);
        if (typeSupport != null) {
            typeSupport.setSupported(false);
        }
    }

    private synchronized void registerAppHandler(final String appType, final ApplicationHandler<?> appHandler) {
        ApplicationTypeSupport typeSupport = _appTypeSupport.get(appType);
        if (typeSupport == null) {
            typeSupport = new ApplicationTypeSupport(false);
            _appTypeSupport.put(appType, typeSupport);
        }
        typeSupport.setHandler(appHandler);
    }

    private synchronized void unregisterAppHandler(final String appType, final ApplicationHandler<?> appHandler) {
        if (FrameworkState.isStopping()) {
            // we are stopping so bail out
            return;
        }

        ApplicationTypeSupport typeSupport = _appTypeSupport.get(appType);
        if (typeSupport == null) {
            // interesting, an app handler unregistered that we don't
            // have a registered app handler for
            throw new RuntimeException("unregisterAppHandler: appType=" + appType + ": appTypeSupport == null");
        }

        typeSupport.setHandler(null);
        Collection<NamedApplication> appsUsingHandler = new HashSet<NamedApplication>();
        for (NamedApplication app : _appFromName.values()) {
            if (app.getTypeSupport() == typeSupport) {
                appsUsingHandler.add(app);
                typeSupport.addWaitingApp(app);
            }
        }
        if (!appsUsingHandler.isEmpty()) {
            final UpdateEpisodeState episode = joinEpisode();
            if (episode != null) {
                if (_tc.isDebugEnabled()) {
                    Tr.debug(_tc, "app type: ", appType);
                }
                if (appType.equals("rar")) {
                    episode.removeRarsStartedDependency();
                }
                episode.unsetAppHandler(appsUsingHandler);
                episode.dropReference();
            }
        }
    }

    @Trivial
    private ApplicationStateMachine createStateMachine(NamedApplication app) {
        ApplicationStateMachine asm = ApplicationStateMachine.newInstance(_ctx, _locAdmin, _futureMonitor,
                                                                          _artifactFactory, _moduleFactory,
                                                                          _executor, _scheduledExecutor,
                                                                          app, _appMonitorConfigurator.getMonitor());
        app.setStateMachine(asm);
        return asm;
    }

    // called only from synchronized methods
    private void processUpdate(String pid, ApplicationConfig newAppConfig) {
        dumpApplications();
        ApplicationStateCoordinator.updateConfiguredAppStatus(pid);
        NamedApplication appFromPid = _appFromPid.get(pid);
        String newAppName = newAppConfig.getName();
        NamedApplication appFromName = _appFromName.get(newAppName);
        final NamedApplication app;
        final ApplicationStateMachine asm;
        if (appFromPid != null) {
            if (appFromName == null) {
                // the last update we received for this pid had a different name,
                // and there is no app associated with the new app name
                processUpdateWithNameChange(pid, newAppConfig, appFromPid);
                return;
            } else if (appFromName != appFromPid) {
                // the last update we received for this pid had a different name,
                // and there is already an app associated with the new name
                processUpdateWithNameConflict(pid, newAppConfig, appFromPid, appFromName);
                return;
            }
            // the last update we received for this pid had the same name, so proceed with updating

            app = appFromPid;
            app.setConfig(newAppConfig);
            if (app.getStateMachine() != null) {
                asm = app.getStateMachine();
                recycleDependentApps(app.getRecycleContext());
            } else {
                asm = createStateMachine(app);
            }
        } else {
            if (appFromName != null) {
                // there isn't an app associated with this pid, but there is
                // already an app associated with the app name
                ApplicationTypeSupport typeSupport = _appTypeSupport.get(newAppConfig.getType());
                AppMessageHelper.get(typeSupport != null ? typeSupport.getHandler() : null).error("DUPLICATE_APPLICATION_NAME", newAppName);
                blockApplication(pid, newAppConfig, newAppName);
                ApplicationStateCoordinator.updateStartingAppStatus(pid, ApplicationStateCoordinator.AppStatus.DUP_APP_NAME);
                return;
            }
            // this is the first update we received for this pid and the name is available
            app = new NamedApplication(newAppName, newAppConfig, _ctx, _executor);
            asm = createStateMachine(app);
            _appFromPid.put(pid, app);
            _appFromName.put(newAppName, app);
        }

        String appType = newAppConfig.getType();
        ApplicationTypeSupport typeSupport = _appTypeSupport.get(appType);
        if (typeSupport == null) {
            typeSupport = new ApplicationTypeSupport(false);
            _appTypeSupport.put(appType, typeSupport);
        }
        app.setTypeSupport(typeSupport);
        ApplicationHandler<?> appHandler = typeSupport.getHandler();
        asm.setAppHandler(appHandler);
        if (appHandler == null) {
            typeSupport.addWaitingApp(app);
        }

        final UpdateEpisodeState episode = joinEpisode();
        if (episode != null) {
            episode.configureApp(app);
            episode.dropReference();
        }
    }

    private boolean cleanCache(File cacheDir, Set<String> excludedPids) {
        if (cacheDir == null || !cacheDir.isDirectory())
            return false;

        boolean result = true;
        for (File pidDirectory : cacheDir.listFiles()) {
            if (!excludedPids.contains(pidDirectory.getName())) {
                result &= cleanCacheDirectory(pidDirectory);
            }
        }
        return result;
    }

    private boolean cleanCacheDirectory(File f) {
        if (f.isDirectory()) {
            boolean result = true;
            for (File child : f.listFiles()) {
                result &= cleanCacheDirectory(child);
            }
            return result &= f.delete();
        } else {
            return f.delete();
        }
    }

    // called only from synchronized methods
    private void processDeletion(String pid) {
        // find the running app for this pid
        final NamedApplication appFromPid = _appFromPid.get(pid);
        if (appFromPid == null) {
            // perhaps the config for this pid is blocked
            ApplicationConfig blockedConfig = _blockedConfigFromPid.remove(pid);
            if (blockedConfig != null) {
                String blockedName = blockedConfig.getName();
                List<String> blockedPids = _blockedPidsFromName.get(blockedName);
                if (blockedPids != null && !blockedPids.isEmpty()) {
                    blockedPids.remove(pid);
                }
                return;
            } else {
                // ignore deletion of something we didn't add, probably was
                // a bad updated call we also ignored
                return;
            }
        }

        // check that we match the app with this app name
        NamedApplication appFromName = _appFromName.get(appFromPid.getAppName());
        if (appFromPid != appFromName) {
            throw new IllegalStateException("processDeletion: appFromPid=" + appFromPid + ", appFromName=" + appFromName);
        }

        // uninstall the app currently running with this pid
        uninstallApp(appFromPid, true);
    }

    private File getCacheDir() {
        return _locAdmin.getBundleFile(this, "cache");
    }

    private File getCacheAdaptDir() {
        return _locAdmin.getBundleFile(this, "cacheAdapt");
    }

    private File getCacheOverlayDir() {
        return _locAdmin.getBundleFile(this, "cacheOverlay");
    }

    private void processUpdateWithNameChange(final String pid, final ApplicationConfig newAppConfig, final NamedApplication appFromPid) {
        final String oldAppName = appFromPid.getAppName();
        ApplicationConfig oldAppConfig = appFromPid.getConfig();
        if (oldAppConfig == null) {
            // hmmm, our pid was previously associated with a different name,
            // but we cannot find an app config with that name
            throw new RuntimeException("processUpdateWithNameChange: pid=" + pid + ", oldAppName=" + oldAppName + ": unable to find old app config");
        }
        String oldPid = oldAppConfig.getConfigPid();
        if (!oldPid.equals(pid)) {
            // the last update we received for this name had a different pid
            throw new RuntimeException("processUpdateWithNameChange: name=" + oldAppName + ", oldPid=" + oldPid + ", newPid=" + pid);
        }
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, "processUpdateWithNameChange: pid=" + pid + ", oldAppName=" + oldAppName + ", newAppName=" + newAppConfig.getName());
        }
        final UpdateEpisodeState episode = joinEpisode();
        if (episode != null) {
            ApplicationDependency appRemoved = uninstallApp(appFromPid);
            appRemoved.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    synchronized (ApplicationConfigurator.this) {
                        processUpdate(pid, newAppConfig);
                        episode.dropReference();
                    }
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    episode.dropReference();
                }
            });
        }
    }

    private void processUpdateWithNameConflict(String pid, ApplicationConfig newAppConfig,
                                               final NamedApplication appFromPid, NamedApplication appFromName) {
        String newAppName = newAppConfig.getName();
        String oldAppName = appFromPid.getAppName();
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, "processUpdateWithNameConflict: pid=" + pid + ", oldAppName=" + oldAppName + ", newAppName=" + newAppName);
        }
        final UpdateEpisodeState episode = joinEpisode();
        if (episode != null) {
            ApplicationDependency appRemoved = uninstallApp(appFromPid);
            ApplicationTypeSupport typeSupport = _appTypeSupport.get(newAppConfig.getType());
            AppMessageHelper.get(typeSupport != null ? typeSupport.getHandler() : null).audit("DUPLICATE_APPLICATION_NAME", newAppName);
            blockApplication(pid, newAppConfig, newAppName);
            ApplicationStateCoordinator.updateStartingAppStatus(pid, ApplicationStateCoordinator.AppStatus.DUP_APP_NAME);
            appRemoved.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    synchronized (ApplicationConfigurator.this) {
                        episode.dropReference();
                    }
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    episode.dropReference();
                }
            });
        }
    }

    private void recycleDependentApps(ApplicationRecycleContext recycleContext) {
        if (recycleContext != null) {
            ApplicationRecycleContextState recycleContextState = _appRecycleMap.get(recycleContext);
            if (recycleContextState != null) {
                Set<String> currentDependentApps = new HashSet<String>();
                for (ApplicationRecycleComponent recycleComponent : recycleContextState.components) {
                    Set<String> dependentApps = recycleComponent.getDependentApplications();
                    if (dependentApps != null && !dependentApps.isEmpty()) {
                        currentDependentApps.addAll(dependentApps);
                    }
                }
                Collection<NamedApplication> apps = getNamedApps(currentDependentApps);
                if (apps != null && !apps.isEmpty()) {
                    UpdateEpisodeState episode = _currentEpisode.addReference();
                    episode.recycleApps(apps);
                    episode.dropReference();
                }
            } else {
                if (_tc.isEventEnabled()) {
                    Tr.event(_tc, "recycleDependentApps: config context was not previously set");
                }
            }
        }
    }

    private Collection<NamedApplication> getNamedApps(Set<String> appNames) {
        Collection<NamedApplication> apps = null;
        if (appNames != null) {
            for (String appName : appNames) {
                NamedApplication app = _appFromName.get(appName);
                if (app != null) {
                    if (apps == null) {
                        apps = new HashSet<NamedApplication>();
                    }
                    apps.add(app);
                }
            }
        }
        return apps;
    }

    private void addStartDependency(String pid, ApplicationDependency dependency) {

        List<ApplicationDependency> deps = _startAfterDependencies.get(pid);
        if (deps == null) {
            deps = new LinkedList<ApplicationDependency>();
            _startAfterDependencies.put(pid, deps);
        }
        deps.add(dependency);
    }

    private void blockApplication(String pid, ApplicationConfig newAppConfig, String newAppName) {
        _blockedConfigFromPid.put(pid, newAppConfig);
        List<String> blockedPids = _blockedPidsFromName.get(newAppName);
        if (blockedPids == null) {
            blockedPids = new LinkedList<String>();
            _blockedPidsFromName.put(newAppName, blockedPids);
        }
        blockedPids.add(pid);
    }

    @Trivial
    ApplicationDependency createDependency(String desc) {
        ApplicationDependency appDep = new ApplicationDependency(_futureMonitor, desc);
        if (_tc.isEventEnabled()) {
            Tr.event(_tc, "createDependency: created " + appDep);
        }
        return appDep;
    }

    @Trivial
    private synchronized NamedApplication[] getNamedApps() {
        Collection<NamedApplication> apps = _appFromName.values();
        return apps.toArray(new NamedApplication[apps.size()]);
    }

    @Trivial
    void dumpApplications() {
        if (TraceComponent.isAnyTracingEnabled() && _tc.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            String prefix = " ";
            for (NamedApplication app : getNamedApps()) {
                sb.append(prefix);
                app.describe(sb);
                prefix = ", ";
            }
            String desc = sb.toString();
            if (desc.length() > 0) {
                Tr.debug(_tc, "applications:" + desc);
            }
        }
    }

    @FFDCIgnore(IllegalStateException.class)
    private UpdateEpisodeState joinEpisode() {
        if (_currentEpisode == null) {
            try {
                _currentEpisode = new UpdateEpisodeState();
            } catch (IllegalStateException ex) {
                return null;
            }
        }
        return _currentEpisode.addReference();
    }

    private void leaveEpisode() {
        UpdateEpisodeState episode = _currentEpisode;
        if (episode != null) {
            _currentEpisode = null;
        }
    }

    private final class UpdateEpisodeState implements CompletionListener<Boolean> {
        private final RuntimeUpdateNotification appsStoppedNotification;
        private final RuntimeUpdateNotification appsStartingNotification;
        private final RuntimeUpdateNotification appsInstallCalledNotification;

        private final ApplicationDependency appsStopping;
        private final ApplicationDependency appsStopped;
        private final ApplicationDependency appsStarting;
        private final ApplicationDependency appsInstallCalled;

        private final ApplicationDependency rarsHaveStarted;
        private final List<ApplicationDependency> appsStoppedFutures = new ArrayList<ApplicationDependency>();
        private final Map<ApplicationDependency, ApplicationStateMachine> appsInstallCalledFutures = new HashMap<ApplicationDependency, ApplicationStateMachine>();

        private final List<ApplicationDependency> rarAppsStartedFutures = new ArrayList<ApplicationDependency>();
        private final Map<String, ApplicationDependency> appStoppedFutureMap = new HashMap<String, ApplicationDependency>();
        private Set<ApplicationRecycleContext> unregisteredContexts;
        private final AtomicInteger refCount = new AtomicInteger();
        private final AtomicReference<CancelableCompletionListenerWrapper<Boolean>> completionListener = new AtomicReference<CancelableCompletionListenerWrapper<Boolean>>();

        UpdateEpisodeState() throws IllegalStateException {
            appsStoppedNotification = _runtimeUpdateManager.createNotification(RuntimeUpdateNotification.APPLICATIONS_STOPPED);
            appsStartingNotification = _runtimeUpdateManager.createNotification(RuntimeUpdateNotification.APPLICATIONS_STARTING, true);
            appsInstallCalledNotification = _runtimeUpdateManager.createNotification(RuntimeUpdateNotification.APPLICATIONS_INSTALL_CALLED, true);

            if (appsStoppedNotification == null || appsStartingNotification == null ||
                appsInstallCalledNotification == null) {
                throw new IllegalStateException();
            }
            this.appsStopping = createDependency("resolves when applications are stopping");
            this.appsStopped = new ApplicationDependency(_futureMonitor, appsStoppedNotification.getFuture(), "resolves when applications have stopped");
            this.appsStarting = new ApplicationDependency(_futureMonitor, appsStartingNotification.getFuture(), "resolves when applications can start");
            this.appsInstallCalled = new ApplicationDependency(_futureMonitor, appsInstallCalledNotification.getFuture(), "resolves when install has been called for all applications");

            this.rarsHaveStarted = createDependency("resolves when all resource adapters have started");
            appsStopping.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    List<ApplicationDependency> stoppedApps;
                    // need to synch on ApplicationConfigurator.this to avoid concurrent modification
                    synchronized (ApplicationConfigurator.this) {
                        stoppedApps = new ArrayList<ApplicationDependency>(appStoppedFutureMap.values());
                    }
                    FutureCollectionCompletionListener.newFutureCollectionCompletionListener(stoppedApps, appsStopped);
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                }
            });
            appsStarting.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    List<ApplicationDependency> rarAppsStarted;
                    if (result) {
                        // need to synch on ApplicationConfigurator.this to avoid concurrent modification
                        synchronized (ApplicationConfigurator.this) {
                            rarAppsStarted = new ArrayList<ApplicationDependency>(rarAppsStartedFutures);
                            rarAppsStartedFutures.clear();
                        }
                    } else {
                        rarAppsStarted = Collections.emptyList();
                    }
                    if (result && !rarAppsStarted.isEmpty()) {
                        FutureCollectionCompletionListener.newFutureCollectionCompletionListener(rarAppsStarted, rarsHaveStarted);
                    } else {
                        rarsHaveStarted.setResult(result);
                    }

                    Map<ApplicationDependency, ApplicationStateMachine> appsInstallCalledCopy;
                    if (result) {
                        // need to synch on ApplicationConfigurator.this to avoid concurrent modification
                        synchronized (ApplicationConfigurator.this) {
                            // get a snapshot of existing installCalled dependencies
                            appsInstallCalledCopy = new HashMap<ApplicationDependency, ApplicationStateMachine>(appsInstallCalledFutures);
                            appsInstallCalledFutures.clear();
                        }
                    } else {
                        appsInstallCalledCopy = Collections.emptyMap();
                    }
                    if (result && !appsInstallCalledCopy.isEmpty()) {
                        // Go through the snapshot and remove ones that are for ASMs that are still blocked
                        for (Iterator<Map.Entry<ApplicationDependency, ApplicationStateMachine>> iAppsInstallCalled = appsInstallCalledCopy.entrySet().iterator(); iAppsInstallCalled.hasNext();) {
                            if (iAppsInstallCalled.next().getValue().isBlocked()) {
                                iAppsInstallCalled.remove();
                            }
                        }
                        if (appsInstallCalledCopy.isEmpty()) {
                            // All applications are still blocked just complete the installCalled future
                            appsInstallCalled.setResult(result);
                        } else {
                            // listen for when the remaining ones complete
                            FutureCollectionCompletionListener.newFutureCollectionCompletionListener(appsInstallCalledCopy.keySet(), appsInstallCalled);
                        }
                    } else {
                        appsInstallCalled.setResult(result);
                    }
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    rarsHaveStarted.setResult(t);
                    appsInstallCalled.setResult(t);
                }
            });
        }

        /**
         * If we unset the RAR handler, remove the dependency on waiting for RARs to start
         */
        public void removeRarsStartedDependency() {
            if (_tc.isDebugEnabled()) {
                Tr.debug(_tc, "Removing rar start deps");
            }
            rarsHaveStarted.setResult(true);
        }

        public void deactivate() {
            if (!appsStarting.isDone()) {
                appsStarting.setResult(true);
            }
        }

        public void createAppForceRestartDependency(Future<Boolean> appForceRestart, RuntimeUpdateNotification runtimeUpdateNotification) {
            _futureMonitor.onCompletion(appForceRestart, new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    if (result) {
                        synchronized (ApplicationConfigurator.this) {
                            restartApps(_appFromName.values(), runtimeUpdateNotification);
                        }
                    } else {
                        dropReference();
                    }
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    dropReference();
                }
            });
        }

        public void createSimpleDependency(Future<Boolean> future) {
            _futureMonitor.onCompletion(future, new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    dropReference();
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    dropReference();
                }
            });
        }

        @Override
        public String toString() {
            return super.toString() + "[refCount=" + refCount + ']';
        }

        void recycleAppContext(ApplicationRecycleContext context) {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, "CCE: recycleAppContext: context " + context);
            }
            if (unregisteredContexts == null) {
                unregisteredContexts = new HashSet<ApplicationRecycleContext>();
            }
            unregisteredContexts.add(context);
            final String contextApp = context.getAppName();
            final Future<Boolean> appsStoppedFuture = context.getAppsStoppedFuture();
            if (appsStoppedFuture != null) {
                appsStoppedFutures.add(new ApplicationDependency(_futureMonitor, appsStoppedFuture, "resolves when the apps using resource adapter "
                                                                                                    + contextApp + " have stopped"));
            }
        }

        void dropContextReference(ApplicationRecycleContext context) {
            if (unregisteredContexts != null && unregisteredContexts.remove(context)) {
                dropReference();
            }
        }

        UpdateEpisodeState addReference() {
            final boolean addingFirstRef = refCount.getAndIncrement() == 0;
            if (addingFirstRef) {
                CancelableCompletionListenerWrapper<Boolean> listener = completionListener.getAndSet(null);
                if (listener != null) {
                    listener.cancel();
                }
            }
            return this;
        }

        UpdateEpisodeState dropReference() {
            final boolean droppingLastRef = refCount.decrementAndGet() == 0;
            if (droppingLastRef) {
                checkForCycles();
                CancelableCompletionListenerWrapper<Boolean> listener = completionListener.getAndSet(null);
                if (listener != null) {
                    listener.cancel();
                }
                startApplications();
            }
            return this;
        }

        void configureApp(NamedApplication app) {
            ApplicationConfig appConfig = app.getConfig();
            final boolean isRARApp = "rar".equals(appConfig.getType());

            final Collection<ApplicationDependency> appStartingFutures = new LinkedList<ApplicationDependency>();
            if (isRARApp) {
                appStartingFutures.add(_appManagerRARSupportDependency);
                appStartingFutures.add(appsStarting);
            } else {
                appStartingFutures.add(_appManagerReadyDependency);
                appStartingFutures.add(rarsHaveStarted);
            }

            final Collection<ApplicationDependency> startAfterFutures = new LinkedList<ApplicationDependency>();
            for (String dependency : appConfig.getStartAfter()) {
                NamedApplication depApp = _appFromPid.get(dependency);
                if (depApp == null || depApp.appStateRef.get() != ApplicationState.STARTED) {
                    ApplicationDependency startAfter = createDependency("resolves when the app " + dependency + " has started");
                    startAfterFutures.add(startAfter);
                    addStartDependency(dependency, startAfter);
                }

            }

            final String appPid = appConfig.getConfigPid();
            ApplicationDependency stoppedFuture = appStoppedFutureMap.get(appPid);
            if (stoppedFuture == null) {
                stoppedFuture = createDependency("resolves when the " + appConfig.getLabel() + " has stopped");
                appStoppedFutureMap.put(appPid, stoppedFuture);
            }
            final ApplicationDependency startingFuture = createDependency("resolves when the " + appConfig.getLabel() + " begins starting");

            // always create a future for install called
            final ApplicationDependency installCalledFuture = createDependency("resolves when the " + appConfig.getLabel() + " has called install");
            appsInstallCalledFutures.put(installCalledFuture, app.getStateMachine());

            final ApplicationDependency startedFuture = createDependency("resolves when the " + appConfig.getLabel() + " has started");
            if (isRARApp) {
                rarAppsStartedFutures.add(startedFuture);
            }
            startingFuture.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    if (TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled())
                        Tr.event(_tc, "successfulCompletion: startingFuture, awaiting " + startedFuture);
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    // This indicates a temporary failure to begin starting
                    // the application, so listeners of startedFuture might
                    // be notified successfully in the future.  We rely on
                    // FutureMonitor.setResult taking the first result only.
                    startedFuture.setResult(t);
                    installCalledFuture.setResult(true);
                }
            });
            installCalledFuture.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    if (TraceComponent.isAnyTracingEnabled() && _tc.isEventEnabled())
                        Tr.event(_tc, "successfulCompletion: installCalledFuture, awaiting " + startedFuture);
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    // This indicates a temporary failure to begin starting
                    // the application, so listeners of startedFuture might
                    // be notified successfully in the future.  We rely on
                    // FutureMonitor.setResult taking the first result only.
                    startedFuture.setResult(t);
                }
            });
            app.getStateMachine().configure(appConfig, appStartingFutures, startAfterFutures, stoppedFuture, startingFuture, installCalledFuture, startedFuture);

        }

        void unsetAppHandler(Collection<NamedApplication> appsUsingHandler) {
            for (NamedApplication app : appsUsingHandler) {
                ApplicationStateMachine asm = app.getStateMachine();
                if (asm != null) {
                    ApplicationConfig appConfig = app.getConfig();
                    final boolean isRARApp = "rar".equals(appConfig.getType());
                    final Collection<ApplicationDependency> appStartingFutures;
                    if (isRARApp) {
                        appStartingFutures = Arrays.asList(_appManagerRARSupportDependency, appsStarting);
                    } else {
                        appStartingFutures = Arrays.asList(_appManagerReadyDependency, rarsHaveStarted);
                    }
                    final String appPid = appConfig.getConfigPid();
                    ApplicationDependency stoppedFuture = appStoppedFutureMap.get(appPid);
                    if (stoppedFuture == null) {
                        stoppedFuture = createDependency("resolves when the " + appConfig.getLabel() + " has stopped");
                        appStoppedFutureMap.put(appPid, stoppedFuture);
                    }

                    // always create a future for install called
                    ApplicationDependency installCalledFuture = createDependency("resolves when the " + appConfig.getLabel() + " has called install");
                    appsInstallCalledFutures.put(installCalledFuture, app.getStateMachine());

                    ApplicationDependency startedFuture = null;
                    if (isRARApp) {
                        startedFuture = createDependency("resolves when the RAR " + appConfig.getLabel() + " has started");
                        rarAppsStartedFutures.add(startedFuture);
                    }
                    asm.setAppHandler(null);
                    asm.recycle(appStartingFutures, stoppedFuture, installCalledFuture, startedFuture);
                }
            }
        }

        /**
         * restartApps will restart all applications in response to an AppForceRestart directive on a feature
         *
         * @param featureUpdatesComplete
         */
        void restartApps(Collection<NamedApplication> apps, RuntimeUpdateNotification featureUpdatesComplete) {
            for (NamedApplication app : apps) {
                if (!app.isConfigured()) {
                    // skip apps which haven't been configured yet, they can't be using
                    // feature bundles being reprovisioned.
                    continue;
                }
                ApplicationStateMachine asm = app.getStateMachine();
                if (asm != null) {
                    ApplicationConfig appConfig = app.getConfig();
                    final boolean isRARApp = "rar".equals(appConfig.getType());
                    final Collection<ApplicationDependency> appStartingFutures = new LinkedList<ApplicationDependency>();
                    ApplicationDependency featuresComplete = new ApplicationDependency(_futureMonitor, featureUpdatesComplete.getFuture(), "Resolves when feature updates are complete after AppForceRestart");
                    appStartingFutures.add(featuresComplete);
                    if (isRARApp) {
                        appStartingFutures.add(_appManagerRARSupportDependency);
                        appStartingFutures.add(appsStarting);
                    } else {
                        appStartingFutures.add(_appManagerReadyDependency);
                        appStartingFutures.add(rarsHaveStarted);
                    }
                    final String appPid = appConfig.getConfigPid();
                    ApplicationDependency stoppedFuture = appStoppedFutureMap.get(appPid);
                    if (stoppedFuture == null) {
                        stoppedFuture = createDependency("resolves when the " + appConfig.getLabel() + " has stopped");
                        appStoppedFutureMap.put(appPid, stoppedFuture);
                    }

                    // always create a future for install called
                    ApplicationDependency installCalledFuture = createDependency("resolves when the " + appConfig.getLabel() + " has called install");
                    appsInstallCalledFutures.put(installCalledFuture, app.getStateMachine());

                    ApplicationDependency startedFuture = null;
                    if (isRARApp) {
                        startedFuture = createDependency("resolves when the RAR App" + appConfig.getLabel() + " has started");
                        rarAppsStartedFutures.add(startedFuture);
                    }

                    for (String dependency : appConfig.getStartAfter()) {
                        ApplicationDependency startAfter = createDependency("resolves when the app " + dependency + " has started");
                        appStartingFutures.add(startAfter);
                        addStartDependency(dependency, startAfter);
                    }

                    asm.recycle(appStartingFutures, stoppedFuture, installCalledFuture, startedFuture);
                }
            }
            appsStopped.onCompletion(new CompletionListener<Boolean>() {
                @Override
                public void successfulCompletion(Future<Boolean> future, Boolean result) {
                    dropReference();
                }

                @Override
                public void failedCompletion(Future<Boolean> future, Throwable t) {
                    dropReference();
                }
            });
            appsStopping.setResult(true);
        }

        void recycleApps(Collection<NamedApplication> apps) {
            for (NamedApplication app : apps) {
                ApplicationConfig appConfig = app.getConfig();
                final String appPid = appConfig.getConfigPid();
                ApplicationDependency stoppedFuture = appStoppedFutureMap.get(appPid);
                if (stoppedFuture == null) {
                    final ApplicationStateMachine asm = app.getStateMachine();
                    if (asm != null) {
                        final boolean isRARApp = "rar".equals(appConfig.getType());
                        final Collection<ApplicationDependency> appStartingFutures = new LinkedList<ApplicationDependency>();
                        if (isRARApp) {
                            appStartingFutures.add(_appManagerRARSupportDependency);
                            appStartingFutures.add(appsStarting);
                        } else {
                            appStartingFutures.add(_appManagerReadyDependency);
                            appStartingFutures.add(rarsHaveStarted);
                        }
                        stoppedFuture = createDependency("resolves when the " + appConfig.getLabel() + " has stopped");
                        appStoppedFutureMap.put(appPid, stoppedFuture);

                        // always create a future for install called
                        ApplicationDependency installCalledFuture = createDependency("resolves when the " + appConfig.getLabel() + " has called install");
                        appsInstallCalledFutures.put(installCalledFuture, app.getStateMachine());

                        ApplicationDependency startedFuture = null;
                        if (isRARApp) {
                            startedFuture = createDependency("resolves when the RAR " + appConfig.getLabel() + " has started");
                            rarAppsStartedFutures.add(startedFuture);
                        }

                        for (String dependency : appConfig.getStartAfter()) {
                            ApplicationDependency startAfter = createDependency("resolves when the app " + dependency + " has started");
                            appStartingFutures.add(startAfter);
                            addStartDependency(dependency, startAfter);
                        }

                        asm.recycle(appStartingFutures, stoppedFuture, installCalledFuture, startedFuture);
                    }
                }
            }
        }

        void startApplications() {
            CancelableCompletionListenerWrapper<Boolean> newListener = new CancelableCompletionListenerWrapper<Boolean>(this);
            CancelableCompletionListenerWrapper<Boolean> oldListener = completionListener.getAndSet(newListener);
            if (oldListener != null) {
                oldListener.cancel();
            }
            appsStopped.onCompletion(newListener);
            appsStopping.setResult(true);
        }

        @Override
        public void successfulCompletion(Future<Boolean> future, Boolean result) {
            if (_ctx == null) {
                return;
            }
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, "CCE: successfulCompletion: appStopped, result " + result);
            }
            synchronized (ApplicationConfigurator.this) {
                if (refCount.get() > 0) {
                    return;
                }
                CancelableCompletionListenerWrapper<Boolean> listener = completionListener.getAndSet(null);
                if (listener != null) {
                    listener.cancel();
                }
                for (ApplicationDependency appsStoppedFuture : appsStoppedFutures) {
                    appsStoppedFuture.setResult(true);
                }
                appsStoppedFutures.clear();
                appStoppedFutureMap.clear();
                RuntimeUpdateNotification featureUpdatesCompleted = _runtimeUpdateManager.getNotification(RuntimeUpdateNotification.FEATURE_UPDATES_COMPLETED);
                if (featureUpdatesCompleted != null) {
                    featureUpdatesCompleted.waitForCompletion();
                }
                leaveEpisode();
                appsStarting.setResult(true);
            }
        }

        @Override
        public void failedCompletion(Future<Boolean> future, Throwable t) {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, "CCE: failedCompletion: appStopped, future " + future + ", throwable " + t);
            }
            if (t != null && t instanceof ExecutionException) {
                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.app.manager.internal.ApplicationConfigurator.UpdateEpisodeState.failedCompletion", "1385");
            }

        }
    }

    private static class CancelableCompletionListenerWrapper<T> implements CompletionListener<T> {
        private volatile CompletionListener<T> listener;

        public CancelableCompletionListenerWrapper(CompletionListener<T> listener) {
            this.listener = listener;
        }

        public void cancel() {
            listener = null;
        }

        @Override
        public void successfulCompletion(Future<T> future, T result) {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, "CCLW: successfulCompletion: future " + future + ", result " + result);
            }
            CompletionListener<T> l = listener;
            if (l != null) {
                l.successfulCompletion(future, result);
            }
        }

        @Override
        public void failedCompletion(Future<T> future, Throwable t) {
            if (_tc.isEventEnabled()) {
                Tr.event(_tc, "CCLW: failedCompletion: future " + future + ", throwable " + t);
            }
            CompletionListener<T> l = listener;
            if (l != null) {
                l.failedCompletion(future, t);
            }
        }
    }

    // only called from ApplicationStateCoordinator
    public ConfigurationAdmin getConfigAdminService() {
        return _configAdmin;
    }

    public void readyForAppsToStart() {
        _appManagerRARSupportDependency.setResult(true);
        _appManagerReadyDependency.setResult(true);
    }

    public synchronized void readyForAppsToStop() {
        Set<String> appPids = new HashSet<String>();
        _appsToShutdown = new HashSet<NamedApplication>();
        for (NamedApplication app : _appFromName.values()) {
            ApplicationStateMachine asm = app.getStateMachine();
            if (asm != null) {
                ApplicationConfig appConfig = app.getConfig();
                appPids.add(appConfig.getConfigPid());
                _appsToShutdown.add(app);
            }
        }
        _appFromName.clear();
        _appFromPid.clear();
        _blockedConfigFromPid.clear();
        _blockedPidsFromName.clear();

        cleanCache(getCacheAdaptDir(), appPids);
        cleanCache(getCacheOverlayDir(), appPids);
        cleanCache(getCacheDir(), appPids);

        ApplicationStateCoordinator.setStoppingAppPids(appPids);
        for (NamedApplication app : _appsToShutdown) {
            uninstallApp(app);
        }
        _appsToShutdown.clear();
    }

    private ApplicationDependency uninstallApp(final NamedApplication appFromPid) {
        return uninstallApp(appFromPid, false);
    }

    private ApplicationDependency uninstallApp(final NamedApplication appFromPid, boolean cleanCache) {
        final String oldAppName = appFromPid.getAppName();
        ApplicationDependency appRemoved = createDependency("resolves when app " + oldAppName + " is removed");
        // uninstall the currently running app with this pid
        ApplicationStateMachine asm = appFromPid.getStateMachine();
        appFromPid.setStateMachine(null);
        if (asm != null) {
            asm.uninstall(appRemoved);
        } else {
            appRemoved.setResult(true);
        }
        appRemoved.onCompletion(new CompletionListener<Boolean>() {
            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                synchronized (ApplicationConfigurator.this) {
                    final String removedAppPid = appFromPid.getConfigPid();
                    appFromPid.unregisterServices();
                    if (_appFromPid.containsKey(removedAppPid) && _appFromPid.get(removedAppPid).equals(appFromPid)) {
                        _appFromPid.remove(removedAppPid);
                    }
                    if (_appFromName.containsKey(oldAppName) && _appFromName.get(oldAppName).equals(appFromPid)) {
                        _appFromName.remove(oldAppName);
                    }

                    ApplicationStateCoordinator.updateStartingAppStatus(removedAppPid, ApplicationStateCoordinator.AppStatus.REMOVED);
                    ApplicationStateCoordinator.updateStoppingAppStatus(removedAppPid, ApplicationStateCoordinator.AppStatus.REMOVED);
                    List<String> blockedPids = _blockedPidsFromName.get(oldAppName);
                    if (blockedPids != null && !blockedPids.isEmpty()) {
                        String blockedPid = blockedPids.remove(0);
                        ApplicationConfig blockedConfig = _blockedConfigFromPid.remove(blockedPid);
                        processUpdate(blockedPid, blockedConfig);
                    }
                    if (cleanCache) {
                        File f = new File(getCacheDir(), removedAppPid);
                        cleanCacheDirectory(f);
                        f = new File(getCacheAdaptDir(), removedAppPid);
                        cleanCacheDirectory(f);
                        f = new File(getCacheOverlayDir(), removedAppPid);
                        cleanCacheDirectory(f);
                    }

                }
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
            }
        });
        return appRemoved;
    }

    /**
     * @param appPid
     */
    public void unblockAppStartDependencies(String appPid) {
        List<ApplicationDependency> deps = _startAfterDependencies.get(appPid);
        if (deps == null)
            return;
        for (ApplicationDependency dep : deps) {
            dep.setResult(true);
        }

    }

    private class CycleException extends Exception {

        /**  */
        private static final long serialVersionUID = 3260293053577638179L;
    }

    @FFDCIgnore(CycleException.class)
    private boolean containsCycles(NamedApplication app) {
        LinkedList<NamedApplication> existing = new LinkedList<NamedApplication>();
        existing.add(app);
        try {
            checkForCycles(app, existing);
            return false;
        } catch (CycleException ex) {
            return true;
        }
    }

    private void checkForCycles() {
        for (Map.Entry<String, NamedApplication> entry : _appFromPid.entrySet()) {
            if (containsCycles(entry.getValue())) {
                ApplicationStateCoordinator.updateStartingAppStatus(entry.getKey(), AppStatus.CYCLE);
            }

        }
    }

    private void checkForCycles(NamedApplication app, LinkedList<NamedApplication> existing) throws CycleException {
        for (String pid : app.getConfig().getStartAfter()) {
            NamedApplication dependency = _appFromPid.get(pid);
            if (dependency != null) {
                // Don't worry about dependency == null, either it was reported by config as invalid
                // or it hasn't arrived yet. The cycle will be found from some other application.

                if (existing.contains(dependency)) {
                    if (reportedCycles.add(app.getAppName())) {
                        String names = "";
                        for (int i = 0; i < existing.size(); i++) {
                            names = names + existing.get(i).getAppName() + " ";
                            reportedCycles.add(existing.get(i).getAppName());
                        }
                        Tr.error(_tc, "error.startAfter.cycle", names);
                    }
                    throw new CycleException();
                }

                existing.add(dependency);

                checkForCycles(dependency, existing);

                existing.removeLast();

            }
        }

    }

}
