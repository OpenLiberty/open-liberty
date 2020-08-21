/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.osgi;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.servlet.ServletContainerInitializer;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.event.Event;
import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.Container;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ModuleRuntimeContainer;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.version.ServletVersion;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threading.FutureMonitor;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.ws.webcontainer.SessionRegistry;
import com.ibm.ws.webcontainer.async.AsyncContextFactory;
import com.ibm.ws.webcontainer.collaborator.CollaboratorService;
import com.ibm.ws.webcontainer.exception.WebAppHostNotFoundException;
import com.ibm.ws.webcontainer.osgi.container.DeployedModule;
import com.ibm.ws.webcontainer.osgi.metadata.WebCollaboratorComponentMetaDataImpl;
import com.ibm.ws.webcontainer.osgi.metadata.WebComponentMetaDataImpl;
import com.ibm.ws.webcontainer.osgi.metadata.WebModuleMetaDataImpl;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer.osgi.request.IRequestFactory;
import com.ibm.ws.webcontainer.osgi.response.IResponseFactory;
import com.ibm.ws.webcontainer.osgi.session.SessionHelper;
import com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContext;
import com.ibm.ws.webcontainer.osgi.srt.SRTConnectionContextPool;
import com.ibm.ws.webcontainer.osgi.webapp.WebApp;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppFactory;
import com.ibm.ws.webcontainer.servlet.CacheServletWrapperFactory;
import com.ibm.ws.webcontainer.util.OneTimeUseArrayList;
import com.ibm.ws.webcontainer.util.VirtualHostContextRootMapper;
import com.ibm.wsspi.adaptable.module.adapters.AdapterFactoryService;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.http.EncodingUtils;
import com.ibm.wsspi.http.VirtualHost;
import com.ibm.wsspi.http.VirtualHostListener;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.cache.CacheManager;
import com.ibm.wsspi.webcontainer.extension.ExtensionFactory;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.ITransferContextService;
import com.ibm.wsspi.webcontainer.util.FFDCWrapper;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;
import com.ibm.wsspi.webcontainer.util.URIMatcherFactory;

/**
 * @author asisin
 */
@Component(name="com.ibm.ws.webcontainer", configurationPid="com.ibm.ws.webcontainer", configurationPolicy=ConfigurationPolicy.REQUIRE, property={"service.vendor=IBM","type:String=web"})
public class WebContainer extends com.ibm.ws.webcontainer.WebContainer implements ModuleRuntimeContainer, VirtualHostListener {
    // Note: registration of this class as a  VirtualHostListener is used by the HttpDispatcher to help migrate config
    
    private static final TraceComponent tc = Tr.register(WebContainer.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.osgi.WebContainer";

    // uncomment if the getConnectionContext() method uses it again
    // private static ThreadLocal<SRTConnectionContext> _threadLocal =
    // new ThreadLocal<SRTConnectionContext>();

    private boolean initialized;

    /** required dynamic reference */
    private volatile EventEngine eventService = null;

    /** required dynamic reference */
    private volatile FutureMonitor futureMonitor;
    
    /** Reference for delayed activation of ClassLoadingService */
    private final AtomicServiceReference<ClassLoadingService> classLoadingSRRef = new AtomicServiceReference<ClassLoadingService>("classLoadingService");

    /** Reference for delayed activation of the dispatcher */
    private final AtomicServiceReference<SessionHelper> sessionHelperSRRef = new AtomicServiceReference<SessionHelper>("sessionHelper");
    
    private final AtomicServiceReference<CacheManager> cacheManagerSRRef = new AtomicServiceReference<CacheManager>("cacheManager");

    private final AtomicServiceReference<InjectionEngine> injectionEngineSRRef = new AtomicServiceReference<InjectionEngine>("injectionEngine");
    
    private final AtomicServiceReference<ManagedObjectService> managedObjectServiceSRRef = new AtomicServiceReference<ManagedObjectService>("managedObjectService");

    @SuppressWarnings("serial")
    private final OneTimeUseArrayList backgroundWebAppStartFutures = new OneTimeUseArrayList();
    
    // Web MBean service access ...
    
    private static final String REFERENCE_WEB_MBEAN_RUNTIME = "webMBeanRuntime";
    private final AtomicServiceReference<WebMBeanRuntime> webMBeanRuntimeServiceRef = new AtomicServiceReference<WebMBeanRuntime>(REFERENCE_WEB_MBEAN_RUNTIME);
    
    @Reference(name = REFERENCE_WEB_MBEAN_RUNTIME,
                    service = WebMBeanRuntime.class,
                    cardinality = ReferenceCardinality.OPTIONAL,
                    policy = ReferencePolicy.DYNAMIC)
    protected void setWebMBeanRuntime(ServiceReference<WebMBeanRuntime> ref) {
        this.webMBeanRuntimeServiceRef.setReference(ref);
    }

    protected void unsetWebMBeanRuntime(ServiceReference<WebMBeanRuntime> ref) {
        this.webMBeanRuntimeServiceRef.unsetReference(ref);
    }
    
    /** Use the AtomicServiceReference class to manage set/unset/locate/cache of the ServletContainerInitializerExtensions */
    private final ConcurrentServiceReferenceSet<ServletContainerInitializer> servletContainerInitializers = new ConcurrentServiceReferenceSet<ServletContainerInitializer>("servletContainerInitializers");

    /** ExecutorService reference for accessing thread pools */
    private volatile ExecutorService es = null;

    /** Injected WsLocationAdmin service */
    private final AtomicReference<WsLocationAdmin> locationServiceRef = new AtomicReference<WsLocationAdmin>();

    /** Transfer for service for moving contextual data from one thread to a new thread inside the webcontainer async servlet code. */
//  private volatile ITransferContextService transferContextService = null;

    /** Use the AtomicServiceReference class to manage set/unset/locate/cache of the TransferContextService */
    private final ConcurrentServiceReferenceSet<ITransferContextService> transferContextServiceRef =
                               new ConcurrentServiceReferenceSet<ITransferContextService>("transferService");
    /* provides encoding mapping - locale -> encoding */
    private final AtomicReference<EncodingUtils> encodingServiceRef = new AtomicReference<EncodingUtils>();

    /**
     * Active WebContainer instance. May be null between deactivate and activate
     * calls.
     */
    private static final AtomicReference<WebContainer> instance = new AtomicReference<WebContainer>();

    /**
     * Bridge managing instances of virtual hosts the webcontainer needs
     * and the dynamic hosts provided by the transport. Required static reference--
     * will be set before activate is called
     */
    private DynamicVirtualHostManager vhostManager;

    private MetaDataService metaDataService = null;
    
    private J2EENameFactory j2eeNameFactory = null;

    public ComponentContext context = null;

    /**
     * Context roots that have been added to a VirtualHost.
     */
    private final Set<String> contextRoots = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Map of context roots to list of startModule results that are waiting
     * for a context root to be added.
     */
    private final Map<String, Set<DeployedModule>> pendingContextRoots = new HashMap<String, Set<DeployedModule>>();

    private Map<WebModuleInfo, DeployedModule> deployedModuleMap = new ConcurrentHashMap<WebModuleInfo, DeployedModule>();

    // LIBERTY
    private SRTConnectionContextPool connContextPool;

    private final static String DEFAULT_PORT = "*";
    private final static String DEFAULT_VHOST_NAME = "default_host";
    
    private static String cachedServerInfo  = null;

    private Object lock = new Object() {};

    private WebAppFactory webAppFactory;

    private IRequestFactory requestFactory;
    private IResponseFactory responseFactory;
    
    private AsyncContextFactory asyncContextFactory;
    
    private static final int DEFAULT_MAX_VERSION = 30;
    private ServiceReference<ServletVersion> versionRef;
    
    private static boolean serverStopping = false;

    private volatile int modulesStarting=0;
    
    // Servlet 4.0
    private URIMatcherFactory uriMatcherFactory;
    
    
    /**
     * Constructor.
     * 
     * @param name
     * @param parent
     */
    private WebContainer(String name, Container parent) {
        super(name, parent);
    }

    /**
     * DS constructor.
     */
    public WebContainer() {
        this("Was.webcontainer", null);
        requestMapper = new VirtualHostContextRootMapper();
        setVHostCompatFlag(false);
    }

    /**
     * Activate the web container as a DS component.
     * 
     * Activate all child services.  Set the web container singleton.
     * Post a started event.
     * 
     * @param componentContext The component context of the activation.
     * @param properties Properties for the activation. 
     */
    public void activate(ComponentContext compcontext, Map<String, Object> properties) {
        String methodName = "activate";
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activating the WebContainer bundle");
        }

        this.context = compcontext;
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "Default Port [ " + DEFAULT_PORT + " ]");
            Tr.debug(tc, methodName, "Default Virtual Host [ " + DEFAULT_VHOST_NAME + " ]");
        }        
        WebContainerConfiguration webconfig = new WebContainerConfiguration(DEFAULT_PORT);
        webconfig.setDefaultVirtualHostName(DEFAULT_VHOST_NAME);
        this.initialize(webconfig, properties);

        this.classLoadingSRRef.activate(context);
        this.sessionHelperSRRef.activate(context);
        this.cacheManagerSRRef.activate(context);
        this.injectionEngineSRRef.activate(context);
        this.managedObjectServiceSRRef.activate(context);
        this.servletContainerInitializers.activate(context);
        this.transferContextServiceRef.activate(context);
        
        this.webMBeanRuntimeServiceRef.activate(context);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Object mbeanService = context.locateService(REFERENCE_WEB_MBEAN_RUNTIME);
            Tr.debug(tc, methodName, "Web MBean Runtime [ " + mbeanService + " ]");
            
            Tr.debug(tc, methodName, "Web MBean Runtime Reference [ " + webMBeanRuntimeServiceRef.getReference() + " ]");
            Tr.debug(tc, methodName, "Web MBean Runtime Service [ " + webMBeanRuntimeServiceRef.getService() + " ]");            
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "Posting STARTED_EVENT");
        }
        Event event = this.eventService.createEvent(WebContainerConstants.STARTED_EVENT);
        this.eventService.postEvent(event);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "Posted STARTED_EVENT");
        }

        self.set(this);
        WebContainer.instance.set(this);
        selfInit.countDown();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "Activating the WebContainer bundle: Complete");
        }
    }
    
    
    public static void setServerStopping(boolean serverStop) {
        serverStopping = serverStop;
    }
    
    public static boolean isServerStopping() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "serverStopping = " + serverStopping );
        }    
       return serverStopping;
    }
    
    public void waitForApplicationInitialization(){
        String methodName = "waitForApplicationInitialization";
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }
        Iterator<Future<?>> backgroundWebAppStartFuturesIterator = backgroundWebAppStartFutures.iterator();
        while (backgroundWebAppStartFuturesIterator.hasNext()) {
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "waitForApplicationInitialization: Start wait for app init." );
                }    
                backgroundWebAppStartFuturesIterator.next().get(30, TimeUnit.SECONDS);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "waitForApplicationInitialization: End wait for app init." );
                }    
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName, "Exception caught while waiting for background web apps to initialize");
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "waitForApplicationInitialization: Number of modules starting = " + modulesStarting );
        }    
        // if any modules are still starting wait for up to 20 seconds for them to complete.
        for (int  i=0 ; i<40 && modulesStarting >0 ; i++) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // don't bother sleeping again.
                i=40;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    /**
     * Deactivate the web container as a DS component.
     *
     * Post a stopped event.  Deactivate all child services.  Clear the
     * web container singleton.
     * 
     * @param componentContext The component context of the deactivation. 
     */
    @FFDCIgnore(Exception.class)
    public void deactivate(ComponentContext componentContext) {
        String methodName = "deactivate";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Deactivating the WebContainer bundle");
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "Posting STOPPED_EVENT");
        }
        Event event = this.eventService.createEvent(WebContainerConstants.STOPPED_EVENT);
        this.eventService.postEvent(event);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "Posted STOPPED_EVENT");
        }

        this.classLoadingSRRef.deactivate(componentContext);
        this.sessionHelperSRRef.deactivate(componentContext);
        this.cacheManagerSRRef.deactivate(componentContext);
        this.injectionEngineSRRef.deactivate(componentContext);
        this.managedObjectServiceSRRef.deactivate(context);
        this.servletContainerInitializers.deactivate(componentContext);
        this.transferContextServiceRef.deactivate(componentContext);
        this.webMBeanRuntimeServiceRef.deactivate(componentContext);

        // will now purge each host as it becomes redundant rather than all here.
        //this.vhostManager.purge(); // Clear/purge all maps.

        WebContainer.instance.compareAndSet(this, null);
        self.compareAndSet(this, null);
        selfInit = new CountDownLatch(1);
        extensionFactories.clear();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "Deactivating the WebContainer bundle: Complete");
        }                
    }

    /**
     * Called by DS when configuration is updated (post activation).
     * 
     * @param cfg
     */
    @Modified
    protected void modified(Map<String, Object> cfg) {
        WebContainerConfiguration webconfig = new WebContainerConfiguration(DEFAULT_PORT);
        webconfig.setDefaultVirtualHostName(DEFAULT_VHOST_NAME);
        initialize(webconfig, cfg);
    }

    public void initialize(WebContainerConfiguration config, Map<String, Object> properties) {
        super.initialize(config);
        config.setConfiguration(properties);
        boolean isDefaultTempDir = true;
        com.ibm.ws.webcontainer.WebContainer.setIsDefaultTempDir(isDefaultTempDir); //set to False if we determine we're using zOS and there are multiple instances (if so, need to figure out a unique server id for each server)
        initialized = true;
    }

    /**
     * DS method for setting the class loading service reference.
     * 
     * @param service
     */
    @Reference(service=ClassLoadingService.class, name="classLoadingService")
    protected void setClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingSRRef.setReference(ref);
    }

    /**
     * DS method for unsetting the class loading service reference.
     * 
     * @param service
     */
    protected void unsetClassLoadingService(ServiceReference<ClassLoadingService> ref) {
        classLoadingSRRef.unsetReference(ref);
    }

    @Reference
    protected void setFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = futureMonitor;
    }

    protected void unsetFutureMonitor(FutureMonitor futureMonitor) {
        this.futureMonitor = null;
    }

    /**
     * DS method for setting dynamic/required the event service reference.
     * If the event service is restarted, this will be called to replace the
     * old reference with a new one.
     * 
     * @param service
     */
    @Reference(policy=ReferencePolicy.DYNAMIC)
    protected void setEventService(EventEngine service) {
        this.eventService = service;
    }

    /**
     * Access the event engine service.
     * 
     * @return EventEngine - null if not found
     */
    public static EventEngine getEventService() {
        WebContainer thisInstance = instance.get();
        if (thisInstance != null)
            return thisInstance.eventService;

        return null;
    }

    /**
     * DS method for removing the event service reference.
     * No-op. Deactivate will be called to clean up the webcontainer
     * before this method is called.
     * 
     * @param ref
     */
    protected void unsetEventService(EventEngine ref) {}

    @Reference(service=ITransferContextService.class, cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, name="transferService")
    protected void setTransferService(ServiceReference<ITransferContextService> ref) {
        transferContextServiceRef.addReference(ref);
    }

    protected void unsetTransferService(ServiceReference<ITransferContextService> ref) {
        transferContextServiceRef.removeReference(ref);
    }

    /**
     * DS method for removing the encoding reference.
     * 
     * @param ref
     */
    protected void unsetEncodingService(EncodingUtils encUtils) {
        if (encUtils == encodingServiceRef.get()) {
            encodingServiceRef.set(null);
        }
    }

    public static Iterator<ITransferContextService> getITransferContextServices() {
        WebContainer thisService = instance.get();
        if (thisService != null) {
            Iterator<ITransferContextService> services = thisService.transferContextServiceRef.getServices();
            return services;
        }
        return null;
    }

    /**
     * DS method for setting the Executor (thread pool) service.
     * 
     * @param service
     */
    @Reference(policy=ReferencePolicy.DYNAMIC)
    protected void setExecutorService(ExecutorService service) {
        this.es = service;
    }

    /**
     * DS method for setting the Encoding service.
     * 
     * @param service
     */
    @Reference(policy=ReferencePolicy.DYNAMIC)
    protected void setEncodingService(EncodingUtils encUtils) {
        encodingServiceRef.set(encUtils);
    }

    /**
     * Access the Executor (thread pool) service.
     * 
     * @return Executor Service
     * @exception Exception if service is not found.
     */
    public static ExecutorService getExecutorService() throws Exception {
        WebContainer thisInstance = instance.get();
        if (thisInstance != null) {
            return thisInstance.es;
        }

        // if we're here then something is really wrong with the system.
        // could throw our own sub class exception, seems like a waste for something that shouldn't logically happen though
        Exception e = new Exception("Executor Service not available");
        throw e;
    }

    /**
     * DS method for removing the Executor service reference.
     * 
     * @param service reference to service
     */
    protected void unsetExecutorService(ExecutorService service) {}

    /**
     * DS method for setting the collaboration engine reference.
     * This is a no-op method: it is used with the activated-service argument
     * to force activation of the CollaborationService instance.
     * 
     * @param collaborationEngine Activated collaborationService instance
     */
    @Reference
    protected void setCollaboratorService(CollaboratorService collaborationService) {}

    /**
     * DS method for removing the collaboration engine reference.
     * 
     * @param ref
     */
    protected void unsetCollaboratorService(CollaboratorService ref) {}

    /**
     * Set the reference to the session helper service.
     * 
     * @param ref
     */
    @Reference(service=SessionHelper.class, name="sessionHelper")
    protected void setSessionHelper(ServiceReference<SessionHelper> ref) {
        sessionHelperSRRef.setReference(ref);
    }

    /**
     * Remove the reference to the session helper service.
     * 
     * @param ref
     */
    protected void unsetSessionHelper(ServiceReference<SessionHelper> ref) {
        sessionHelperSRRef.unsetReference(ref);
    }

    @Override
    protected SessionRegistry getSessionRegistry() {
        // Wait until a call for the SessionRegistry before resolving the service.
        // This delays starting of the session bundle until a web application is
        // being initialized.  SessionHelper will be null if either the ComponentContext
        // or the SessionHelper service reference are null
        SessionHelper sessionHelper = sessionHelperSRRef.getService();
        if (sessionHelper != null) {
            return sessionHelper.getRegistry();
        }
        return null;
    }

    @Reference(policy=ReferencePolicy.DYNAMIC, service=InjectionEngine.class, name="injectionEngine")
    protected void setInjectionEngine(ServiceReference<InjectionEngine> ref) {
        this.injectionEngineSRRef.setReference(ref);
    }

    protected void unsetInjectionEngine(ServiceReference<InjectionEngine> ref) {
        this.injectionEngineSRRef.unsetReference(ref);
    }
    
    @Reference(name = "managedObjectService",
                    service = ManagedObjectService.class,
                    policy = ReferencePolicy.DYNAMIC,
                    policyOption = ReferencePolicyOption.GREEDY)
    protected void setManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        this.managedObjectServiceSRRef.setReference(ref);
    }

    protected void unsetManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        this.managedObjectServiceSRRef.unsetReference(ref);
    }

    /** Required static reference: called before activate */
    @Reference
    protected void setVirtualHostMgr(DynamicVirtualHostManager vhostMgr) {
        vhostManager = vhostMgr;
        
        synchronized (lock) {
            for (ExtensionFactory ef: extensionFactories) {
                registerExtensionFactoryWithVirtualHosts(ef);
            }
        }
   }

    /** Required static reference: will be called after deactivate. Avoid NPE */
    protected void unsetVirtualHostMgr(DynamicVirtualHostManager vhostMgr) {}

    /**
     * Return a dynamic virtual host managed by the VirtualHostManager.
     * The virtual host will be created on first request (whether or not there
     * is a backing VirtualHost from the transport.
     * <p>
     * The DynamicVirtualHost & DynamicVirtualHostManager support/encapsulate
     * delayed definition and activation of virtual hosts.
     */
    @Override
    public DynamicVirtualHost getVirtualHost(String targetHost) throws WebAppHostNotFoundException {
        // Should never happen: but NPEs are ugly.
        if (vhostManager == null)
            return null;

        return vhostManager.getVirtualHost(targetHost, this);
    }
    
    public Future<Boolean> addContextRootRequirement(DeployedModule deployedModule) {
        String methodName = "addContextRootRequirement";
        String contextRoot = deployedModule.getMappingContextRoot();
        synchronized (contextRoots) {

            Future<Boolean> future = futureMonitor.createFuture(Boolean.class);
            // This listener is added to track conetxtRoot future and the web application init
            deployedModule.addStartupListener(future, new CompletionListener<Boolean>() {
                @Override
                public void failedCompletion(Future<Boolean> arg0, Throwable arg1) {
                }

                @Override
                public void successfulCompletion(Future<Boolean> arg0, Boolean arg1) {
                    futureMonitor.setResult(arg0, arg1);
                }
            });

            if (contextRoots.contains(contextRoot)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName, "contextRoot-> [" + contextRoot + "] already added , set future done for deployedModule -->" + deployedModule);
                }
                //complete the notification here for app manager
                deployedModule.initTaskComplete();

            } else {

                Set<DeployedModule> pending = pendingContextRoots.get(contextRoot);
                if (pending == null) {
                    pending = new LinkedHashSet<DeployedModule>();
                    pendingContextRoots.put(contextRoot, pending);
                }

                pending.add(deployedModule);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName, "contextRoot-> [" + contextRoot + "] not added , in pending future " + future + " for deployedModule -->" + deployedModule);
                }
            }
            return future;

        }
    }

    public void removeContextRootRequirement(DeployedModule deployedModule) {
        String contextRoot = deployedModule.getMappingContextRoot();
        synchronized (contextRoots) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "removeContextRootRequirement", "contextRoot-> ["+ contextRoot+"]");
            }
            Set<DeployedModule> pending = pendingContextRoots.get(contextRoot);
            if (pending != null) {
                pending.remove(deployedModule);
                if (pending.isEmpty()) {
                    pendingContextRoots.remove(contextRoot);
                }
            }
        }
    }

    public void contextRootAdded(final String contextRoot, VirtualHost vhost) {
        synchronized (contextRoots) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "contextRootAdded", "contextRoot-> ["+ contextRoot+"]");
            }
            contextRoots.add(contextRoot);
            Set<DeployedModule> pending = pendingContextRoots.remove(contextRoot);
            if (pending != null) {
                for (DeployedModule deployedModule : pending) {
                    //complete the notification here for app manager
                    deployedModule.initTaskComplete();
                }
            }
        }
    }

    public void contextRootRemoved(String contextRoot, VirtualHost vhost) {
        synchronized (contextRoots) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "contextRootRemoved", "contextRoot-> ["+ contextRoot+"]");
            }
            contextRoots.remove(contextRoot);
        }
    }

    /**
     * Access the temp directory location unique to this running bundle's
     * webcontainer. This will be of the form
     * "server\workarea\&lt;framework-specific-bundle-path&gt;\temp\" (with
     * platform-specific separator characters) or will be null if the bundle is
     * not running.
     * 
     * @return String
     */
    public static String getTempDirectory() {
        WebContainer thisInstance = instance.get();

        if (thisInstance == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "WebContainer not running, returning null temp dir");
            }
            return null;
        }

        String rc = null;
        try {
            File f = thisInstance.context.getBundleContext().getDataFile("temp");
            if (null != f) {
                rc = f.getAbsolutePath() + File.separatorChar;
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME, "getTempDirectory", thisInstance);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error getting temp dir; " + t);
            }
            rc = null;
        }
        return rc;
    }

    /** required static reference */
    @Reference
    protected void setMetaDataService(MetaDataService ref) {
        this.metaDataService = ref;
    }

    /** required static reference: no-op to remove, will be called post-deactivate */
    protected void unsetMetaDataService(MetaDataService ref) {}
    
    /** required static reference */
    @Reference
    protected void setJ2eeNameFactory(J2EENameFactory ref) {
        this.j2eeNameFactory = ref;
    }

    /** required static reference: no-op to remove, will be called post-deactivate */
    protected void unsetJ2eeNameFactory(J2EENameFactory ref) {}

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.ws.webcontainer.WebContainer#getConnectionContext(java.lang.String)
     */
    @Override
    protected com.ibm.ws.webcontainer.srt.SRTConnectionContext getConnectionContext() {
        // SRTConnectionContext srt = _threadLocal.get();
        // if (srt == null) {
        // srt = new SRTConnectionContext();
        // _threadLocal.set(srt);
        // }
        // return srt;

        /*
         * TODO: Investigate performance impact of removing thread locals and
         * instead creating a new connection context each time. The threadlocal
         * approach causes problems for requests that go async as a subsequent
         * request may use the same thread and therefore get the same
         * SRTServletRequest instance from the SRTConnectionContext.
         */

        // return new SRTConnectionContext();
        return connContextPool.get();
    }

// LIBERTY - NEW METHOD
    /**
     * This will create the metadata for a web module in the web container
     */
    @Override
    public ModuleMetaData createModuleMetaData(ExtendedModuleInfo moduleInfo) throws MetaDataException {
        WebModuleInfo webModule = (WebModuleInfo) moduleInfo;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "createModuleMetaData: " + webModule.getName() + " " + webModule.getContextRoot());
        }
        try {
            com.ibm.wsspi.adaptable.module.Container webModuleContainer = webModule.getContainer();
            WebAppConfiguration appConfig = webModuleContainer.adapt(WebAppConfiguration.class);
            String appName = appConfig.getApplicationName();
            String j2eeModuleName = appConfig.getJ2EEModuleName();
            WebModuleMetaDataImpl wmmd = (WebModuleMetaDataImpl) appConfig.getMetaData();
            wmmd.setJ2EEName(j2eeNameFactory.create(appName, j2eeModuleName, null));
            appConfig.setWebApp(createWebApp(moduleInfo, appConfig));
            return wmmd;
        }
        catch (Throwable e) {
            Throwable cause = e.getCause();
            MetaDataException m;
            if ((cause!=null) && (cause instanceof MetaDataException)) {
                m = (MetaDataException) cause;
                //don't log a FFDC here as we already logged an exception
            } else if ( instance.get() == null ) {
                // The web container is deactivated, let the upstream call handle this
                m = new MetaDataException(e);
            } else {
                m = new MetaDataException(e);
                FFDCWrapper.processException(e, getClass().getName(), "createModuleMetaData", new Object[] { webModule, this });//this throws the exception
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "createModuleMetaData: " + webModule.getName() + "; " + e);
            }
            throw m;
        }
    }

    private WebApp createWebApp(ModuleInfo moduleInfo, WebAppConfiguration webAppConfig) {
        if (instance.get() == null ) {
            throw new IllegalStateException("The web container has been deactivated");
        }
        ReferenceContext referenceContext = injectionEngineSRRef.getServiceWithException().getCommonReferenceContext(webAppConfig.getMetaData());
        ManagedObjectService managedObjectService = managedObjectServiceSRRef.getServiceWithException();
        
        WebApp webApp = webAppFactory.createWebApp(webAppConfig, moduleInfo.getClassLoader(), referenceContext, metaDataService, j2eeNameFactory, managedObjectService);
        webApp.setName(webAppConfig.getModuleName());
        webApp.setModuleContainer(moduleInfo.getContainer());
        webApp.setOrderedLibPaths(webAppConfig.getOrderedLibPaths());
        referenceContext.add(webApp);
        return webApp;
    }

 // LIBERTY - NEW METHOD
    /**
     * This will start a web module in the web container
     */
    public Future<Boolean> startModule(final ExtendedModuleInfo moduleInfo) throws StateChangeException {
        final WebModuleInfo webModule = (WebModuleInfo) moduleInfo;
        
        // Track number of modules starting. Rarely a server can begin shutting down while a module is still starting which
        // can result in an NPE. By counting modules in start, if there are module still starting during server quiesce the
        // webcontainer can hold server start while modules finish starting.
        modulesStarting++;
        Future<Boolean> result;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "startModule: " + webModule.getName() + " " + webModule.getContextRoot() + ", modulesStarting = " + modulesStarting);
        }
        try {
            
            // if this service has been deactivated, then leave as cleanly as possible
            if ((futureMonitor == null) || (instance.get() == null)){
               CompletedFuture f = new CompletedFuture(false);
               return f;
            }
            // If server is stopping return don't start the application, just an future set to true. 
            if (isServerStopping()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "startModule: " + "server is stopping");
                }
                // Server is stopping so we don't want to continue but options for return are not great:
                // - null is not an option, it results in an NPE from the caller.
                // - a Future with no result : server will wait 30 seconds for the result to be set
                // - a Future with the result set to False : server will output a W message to say app did not start.
                // - a Future with the result set to True  : sever will output an I message to say app has started.
                // Go with the latter, because a W message would cause tests to fail,
                result = futureMonitor.createFutureWithResult(Boolean.TRUE);
            } else {
                WebModuleMetaData wmmd = (WebModuleMetaData) ((ExtendedModuleInfo)webModule).getMetaData();
                J2EEName j2eeName = wmmd.getJ2EEName();
                WebAppConfiguration appConfig = (WebAppConfiguration) wmmd.getConfiguration();
 
                WebCollaboratorComponentMetaDataImpl wccmdi = (WebCollaboratorComponentMetaDataImpl) wmmd.getCollaboratorComponentMetaData();
                wccmdi.setJ2EEName(j2eeName);
                metaDataService.fireComponentMetaDataCreated(wccmdi);

                WebComponentMetaDataImpl wcmdi = (WebComponentMetaDataImpl) appConfig.getDefaultComponentMetaData();
                wcmdi.setJ2EEName(j2eeName);
                metaDataService.fireComponentMetaDataCreated(wcmdi);

                for (Iterator<IServletConfig> servletConfigs = appConfig.getServletInfos(); servletConfigs.hasNext();) {
                    IServletConfig servletConfig = servletConfigs.next();
                    wcmdi = (WebComponentMetaDataImpl) servletConfig.getMetaData();
                    wcmdi.setJ2EEName(j2eeNameFactory.create(j2eeName.getApplication(), j2eeName.getModule(), servletConfig.getName()));
                    metaDataService.fireComponentMetaDataCreated(wcmdi);
                }

                final ClassLoader moduleLoader = webModule.getClassLoader();

                // The class loader for this application will do two things:
                // 1. Provide classes needed for the application to run
                // 2. Provide internal classes that implement third party or spec interfaces and are loaded by factories inspecting the thread context class loader
                // The class loader on the web module can handle 1 already but we enhance it with some internal classes in order to support 2.
                // Note that we only have one class loader though as it delegates to the enhanced class loader or app class loader which means that when app classes 
                // are created they will use the app class loader but when something asks the TCCL for one of the internal class they will go to the enhanced CL delegate
                final ClassLoadingService cls = classLoadingSRRef.getService();
                final ClassLoader threadClassLoader = cls.createThreadContextClassLoader(moduleLoader);
                com.ibm.wsspi.adaptable.module.Container webModuleContainer = webModule.getContainer();
                final DeployedModule dMod = new DeployedModule(webModuleContainer, appConfig, threadClassLoader);
                this.deployedModuleMap.put(webModule, dMod);
                addWebApplication(dMod);
                result =  addContextRootRequirement(dMod);
                // If the Webcontainer attribute "deferServletLoad" is set to "false" (not the default)
                // then start the web application now, inline on this thread
                // otherwise, launch is on its own thread
                if (!WCCustomProperties.DEFER_SERVLET_LOAD) {
                    if (!startWebApplication(dMod)) {
                        throw new StateChangeException("startWebApplication");
                    }
                } else {
                    backgroundWebAppStartFutures.add(es, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (startWebApplication(dMod)) {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "startWebApplication async [" + webModule.getName() + "]: success.");
                                    }
                                } else {
                                    throw new Exception("startWebApplication async [" + webModule.getName() + "]: failed.");
                                }
                            } catch (Throwable e) {
                                if (dMod != null && dMod instanceof com.ibm.ws.webcontainer.osgi.container.DeployedModule) {
                                    ((com.ibm.ws.webcontainer.osgi.container.DeployedModule) dMod).initTaskFailed();
                                }
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "startModule async [" + webModule.getContextRoot() + "]; " + e);
                                }
                                stopModule(moduleInfo);
                            }
                        }
                    });
                }
                registerMBeans((WebModuleMetaDataImpl) wmmd, webModuleContainer);
                
            }
        } catch (Throwable e) {
            FFDCWrapper.processException(e, getClass().getName(), "startModule", new Object[] { webModule, this });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "startModule: " + webModule.getName() + "; " + e);
            }
            
            //PI58875
            this.stopModule(moduleInfo);
            throw new StateChangeException(e);
        } finally {
            modulesStarting--;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "startModule: ", "modulesStarting = " + modulesStarting);
        }
        return result;
    }

    /**
     * Check for the JSR77 runtime, and, if available, use it to register module
     * and servlet mbeans.  As a side effect, assign the mbean registration to
     * the web module metadata and to the servlet metadata.  
     *
     * The web module container is required for access to the web module descriptor.
     *
     * @param webModule The web module to register.
     * @param container The container of the web module.
     */
    protected void registerMBeans(WebModuleMetaDataImpl webModule, com.ibm.wsspi.adaptable.module.Container container) {
        String methodName = "registerMBeans";

        String appName = webModule.getApplicationMetaData().getName();
        String webAppName = webModule.getName();

        String debugName;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            debugName = appName + " " + webAppName;
        } else {
            debugName = null;
        }

        WebMBeanRuntime mBeanRuntime = webMBeanRuntimeServiceRef.getService();
        if (mBeanRuntime == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "Web Module [ " + debugName + " ]: No MBean Runtime");
            }
            return;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "Web Module [ " + debugName + " ]: MBean Runtime");
            }
        }

        String ddPath = com.ibm.ws.javaee.dd.web.WebApp.DD_NAME; // This should be obtained by an adapt call.

        Iterator<IServletConfig> servletConfigs = webModule.getConfiguration().getServletInfos();

        webModule.mBeanServiceReg = mBeanRuntime.registerModuleMBean(appName, webAppName, container, ddPath, servletConfigs);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "Web Module [ " + debugName + " ]: Registration [ " + webModule.mBeanServiceReg + " ]");
        }
        
        servletConfigs = webModule.getConfiguration().getServletInfos();        
        while (servletConfigs.hasNext()) {
            IServletConfig servletConfig = servletConfigs.next();
            String servletName = servletConfig.getServletName();

            WebComponentMetaDataImpl wcmdi = (WebComponentMetaDataImpl) servletConfig.getMetaData();
            wcmdi.mBeanServiceReg = mBeanRuntime.registerServletMBean(appName, webAppName, servletName);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "Web Module [ " + debugName + " ] Servlet [ " + servletName  + " ]: Registration [ " + wcmdi.mBeanServiceReg + " ]");
            }            
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "Web Module [ " + debugName + " ]: Completed registrations");
        }        
    }

    /**
     * Deregister the JSR77 mbeans of a web module and of its servlets.
     *
     * Deregistration uses the registrations which were stored to the web module
     * metadata and to the servlet metadata.
     *
     * @param webModule The web module which is to be deregistered.
     */
    protected void deregisterMBeans(WebModuleMetaDataImpl webModule) {
        String methodName = "deregisterMBeans";

        String debugName;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            debugName = webModule.getApplicationMetaData().getName() + " " + webModule.getName();                                
        } else {
            debugName = null;
        }

        
        WebMBeanRuntime mBeanRuntime = webMBeanRuntimeServiceRef.getService();
        if (mBeanRuntime == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "Web Module [ " + debugName + " ]: No MBean Runtime");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "Web Module [ " + debugName + " ]: MBean Runtime");
            }
        }

        ServiceRegistration<?> webModuleReg = webModule.mBeanServiceReg;                    
        if (webModuleReg != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "Web Module [ " + debugName + " ]: Deregister module [ " + webModuleReg + " ]");
            }
            webModule.mBeanServiceReg = null;
            // The preference would be to call ServiceRegistrationImpl.isUnregistered(), but that 
            // is not in the ServiceRegistration API. That still might not be good enough anyways, 
            // if the deregistration happened between the call to isUnregistered and the local unregister() call.
            if (mBeanRuntime != null) {
                try {
                    webModuleReg.unregister();
                } catch (IllegalStateException ise) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                       Tr.debug(tc, methodName, "Web Module [ " + debugName + " ]: Had already deregistered module [ " + webModuleReg + " ]");
                    }
                }
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                debugName = webModule.getApplicationMetaData().getName() + webModule.getName();                
                Tr.debug(tc, methodName, "Web Module [ " + debugName + " ]: No registration");
            }
        }

        Iterator<IServletConfig> servletConfigs = webModule.getConfiguration().getServletInfos();        
        while (servletConfigs.hasNext()) {
            IServletConfig servletConfig = servletConfigs.next();
            String servletName = servletConfig.getServletName();

            WebComponentMetaDataImpl wcmdi = (WebComponentMetaDataImpl) servletConfig.getMetaData();
            ServiceRegistration<?> servletReg = wcmdi.mBeanServiceReg;                                
            if (servletReg != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName, "Servlet [ " + servletName  + " ]: Deregister [ " + servletReg + " ]");
                }
                wcmdi.mBeanServiceReg = null;
                // The preference would be to call ServiceRegistrationImpl.isUnregistered(), but that 
                // is not in the ServiceRegistration API. That still might not be good enough anyways, 
                // if the deregistration happened between the call to isUnregistered and the local unregister() call.
                if (mBeanRuntime != null) {
                    try {
                        servletReg.unregister();
                    } catch (IllegalStateException ise) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, methodName, "Servlet [ " + servletName + " ]: Had already deregistered [ " + servletReg + " ]");
                        }
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName, "Servlet [ " + servletName  + " ]: No registration");
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName, "Web Module [ " + debugName + " ]: Completed deregistrations");
        }                
    }

    private boolean startWebApplication(DeployedModule dm) {
        String virtualHost = dm.getVirtualHostName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "startWebApplication","virtualHost -> [" + virtualHost +"]");
        }
        if ((virtualHost == null) || (virtualHost.equals("")))
            virtualHost = DEFAULT_HOST;

        DynamicVirtualHost vHost;
        try {
            vHost = getVirtualHost(virtualHost);
            return vHost.startWebApplication(dm);
        } catch (WebAppHostNotFoundException e) {
            FFDCWrapper.processException(e, getClass().getName(), "startWebApplication", new Object[] { dm, this });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error starting module: " + dm + "; " + e);
            }
        }
        return false;
    }

 // LIBERTY - NEW METHOD
    /**
     * This will stop a web module in the web container
     */
    public void stopModule(ExtendedModuleInfo moduleInfo) {
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "stopModule()",((WebModuleInfo)moduleInfo).getName());
        }
       
        WebModuleInfo webModule = (WebModuleInfo) moduleInfo;
        try {
            DeployedModule dMod = this.deployedModuleMap.remove(webModule);
            if (null == dMod) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                    Tr.exit(tc, "stopModule()","DeployedModule not known");
                }
                return;
            }
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "stopModule: " + webModule.getName() + " " + webModule.getContextRoot());
            }

            removeContextRootRequirement(dMod);
            removeModule(dMod);
            
            this.vhostManager.purgeHost(dMod.getVirtualHostName());

            WebModuleMetaData wmmd = (WebModuleMetaData) ((ExtendedModuleInfo)webModule).getMetaData();
            
            deregisterMBeans((WebModuleMetaDataImpl) wmmd);
            
            WebAppConfiguration appConfig = (WebAppConfiguration) wmmd.getConfiguration();
            for (Iterator<IServletConfig> servletConfigs = appConfig.getServletInfos(); servletConfigs.hasNext();) {
                IServletConfig servletConfig = servletConfigs.next();
                metaDataService.fireComponentMetaDataDestroyed(servletConfig.getMetaData());
            }

            metaDataService.fireComponentMetaDataDestroyed(appConfig.getDefaultComponentMetaData());
            metaDataService.fireComponentMetaDataDestroyed(wmmd.getCollaboratorComponentMetaData());
        } catch (Throwable e) {
            FFDCWrapper.processException(e, getClass().getName(), "stopModule", new Object[] { webModule, this });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "stopModule: " + webModule.getName() + "; " + e);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "stopModule()");
        }

    }
    private void removeModule(DeployedModule mod) {
        /*
         * First set the class loader on the thread to be the web app class loader. This is need for libraries like JSF that use this to load resources (see defect 52736), need to
         * track the old class loader so we can set it back again
         */
        ClassLoader originalThreadClassLoader = ThreadContextHelper.getContextClassLoader();
        ThreadContextHelper.setClassLoader(mod.getClassLoader());

        try {
            removeWebApplication(mod);
        } catch (Exception e) {
            FFDCWrapper.processException(e, getClass().getName(), "removeModule", new Object[] { mod, this });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error removing module: " + mod + "; " + e);
            }
        } finally {
            ThreadContextHelper.setClassLoader(originalThreadClassLoader);
            
            // Try to destroy URLClassLoader. If we can't, it's OK.
            ClassLoadingService cls = classLoadingSRRef.getService();
            if (cls != null) {
                cls.destroyThreadContextClassLoader(mod.getClassLoader());
            }
        }
    }

    /**
     * This method will add a new JavaEEWar module to the web container and associate it with the supplied web application information. The module should not be in the list of
     * modules returned by {@link WebAppInfo#getWarModules()} prior to calling this method (it will be added to that list as part of the implementation).
     * 
     * @param webAppInfo The web app information to add the module to. The list returned from {@link WebAppInfo#getWarModules()} must be mutable
     * @param newModule The module to add
     */
    // Liberty new method

    /**
     * @return boolean
     */
    public boolean isInitialized() {
        return this.initialized;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.ws.webcontainer.WebContainer#releaseConnectionContext(com.ibm.ws
     * .webcontainer.srt.SRTConnectionContext)
     */
    @Override
    protected void releaseConnectionContext(com.ibm.ws.webcontainer.srt.SRTConnectionContext context) {
        connContextPool.put((SRTConnectionContext) context);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.webcontainer.WebContainer#shouldDecode()
     */
    public boolean shouldDecode() {
        // TODO Auto-generated method stub
        return true;
    }

    @Reference(cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC)
    protected void setExtensionFactory(ExtensionFactory ef) {
        synchronized (lock) {
            extensionFactories.add(ef);

            if (vhostManager != null) {
                registerExtensionFactoryWithVirtualHosts(ef);
            }
        }
    }

    private void registerExtensionFactoryWithVirtualHosts(ExtensionFactory ef) {
        Iterator<DynamicVirtualHost> vhostList = vhostManager.getVirtualHosts();
        while ( vhostList.hasNext()) {
            DynamicVirtualHost vhost = vhostList.next();
            Iterator<WebApp> webApps = vhost.getWebApps();
            while (webApps.hasNext())
            {
                WebApp webApp = (WebApp) webApps.next();
                webApp.addExtensionFactory(ef);
            }
        }
    }

    protected void unsetExtensionFactory(ExtensionFactory ef) {
        synchronized (lock) {
            extensionFactories.remove(ef);

            if (vhostManager != null) {
                // remove from existing apps now
                Iterator<DynamicVirtualHost> vhostList = vhostManager.getVirtualHosts();
                while (vhostList.hasNext()) {
                    DynamicVirtualHost vhost = vhostList.next();
                    Iterator<WebApp> webApps = vhost.getWebApps();
                    while (webApps.hasNext()) {
                        WebApp webApp = (WebApp) webApps.next();
                        webApp.removeExtensionFactory(ef);
                    }
                }
            }
        }
    }

    @Reference(service=ServletContainerInitializer.class, cardinality=ReferenceCardinality.MULTIPLE, policy=ReferencePolicy.DYNAMIC, name="servletContainerInitializers")
    protected void setServletContainerInitializers(ServiceReference<ServletContainerInitializer> ref) {
        servletContainerInitializers.addReference(ref);
    }

    protected void unsetServletContainerInitializers(ServiceReference<ServletContainerInitializer> ref) {
        servletContainerInitializers.removeReference(ref);
    }

    public static Iterator<ServletContainerInitializer> getServletContainerInitializerExtension() {
        WebContainer thisService = instance.get();
        if (thisService != null) {
            Iterator<ServletContainerInitializer> scis = thisService.servletContainerInitializers.getServices();
            return scis;
        }
        return null;
    }

    public static EncodingUtils getEncodingUtils() {
        WebContainer thisService = instance.get();
        if (thisService != null) {
            EncodingUtils encodingUtils = thisService.encodingServiceRef.get();
            return encodingUtils;
        }
        return null;
    }

    @Override
    public void decrementNumRequests() {
    // TODO Auto-generated method stub

    }

    public String getDefaultVirtualHostName() {
        return wcconfig.getDefaultVirtualHostName();
    }

    /** {@inheritDoc} */
    @Override
    public boolean areRequestsOutstanding() {
        return false;
    }

    /*
     * The bnd entry, and this corresponding setter, are temporary work arounds to ensure we have the
     * adapter we need when addWebApp is called.
     */
    @Reference(target="(containerToType=com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration)")
    protected void setAdapterFactoryDependency(AdapterFactoryService afs) { /* see comment above */}

    /*
     * The bnd entry, and this corresponding unsetter, are temporary work arounds to ensure we have the
     * adapter we need when addWebApp is called.
     */
    protected void unsetAdapterFactoryDependency(AdapterFactoryService afs) { /* see comment above */}

    /**
     * Declarative Services method for setting the location service reference.
     * For now, we're using a static reference policy for an optional service reference.
     * That means this component will be deactivated when bound service changes,
     * and this setter method will be called before activate()?
     * 
     * @param ref reference to service object; type of service object is verified
     */
    @Reference
    protected void setLocationService(WsLocationAdmin locAdmin) {
        locationServiceRef.set(locAdmin);
    }

    /**
     * Declarative Services method for unsetting the session store service reference
     * 
     * @param ref reference to service object; type of service object is verified
     */
    protected void unsetLocationService(WsLocationAdmin locAdmin) {
        if (locAdmin == locationServiceRef.get()) {
            locationServiceRef.set(null);
        }
    }

    public static WsLocationAdmin getLocationService() {
        WebContainer thisService = instance.get();
        WsLocationAdmin locationService = null;
        if (thisService != null) {
            locationService = thisService.locationServiceRef.get();
        }
        return locationService;
    }
    
    @Reference(service=CacheManager.class, cardinality=ReferenceCardinality.OPTIONAL, policy=ReferencePolicy.DYNAMIC, name="cacheManager")
    protected void setCacheManager(ServiceReference<CacheManager> ref) {
        this.cacheManagerSRRef.setReference(ref);
    }
    
    protected void unsetCacheManager(ServiceReference<CacheManager> ref) {
        this.cacheManagerSRRef.unsetReference(ref);
    }
    
    public static CacheManager getCacheManager() {
        WebContainer thisService = instance.get();
        CacheManager cacheManager = null;
        if (thisService != null) {
            cacheManager = thisService.getCacheManagerService();
        }
        return cacheManager;
    }
    
    private CacheManager getCacheManagerService() {
        return this.cacheManagerSRRef == null ? null : this.cacheManagerSRRef.getService();
    }
    
    public static String getServerInfoFromBundle(){
        String serverInfo = cachedServerInfo;

        if ( serverInfo == null ) {
            WebContainer thisInstance = instance.get();

            if (thisInstance == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "WebContainer not running, returning null ");
                }
                return null;
            }

            String serverName = thisInstance.context.getBundleContext().getBundle().getHeaders("").get("WLP-ServerName"); 
            String serverVersion = thisInstance.context.getBundleContext().getBundle().getHeaders("").get("WLP-ServerVersion"); // the "" prevents localization

            serverInfo = cachedServerInfo = serverName +'/' + serverVersion;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "serverInfo -->" + serverInfo);
        }
        return serverInfo;          
    }

    // Only DS should invoke this method
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    protected void setConnContextPool(SRTConnectionContextPool pool) {
        this.connContextPool = pool;
    }

    // Only DS should invoke this method
    protected void unsetConnContextPool(SRTConnectionContextPool pool) {
        // no-op intended here to avoid connContextPool being null when switching service implementations
    }
    
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    protected void setWebAppFactory(WebAppFactory factory) {
        this.webAppFactory = factory;
    }
    
    protected void unsetWebAppFactory(WebAppFactory factory) {
        // no-op intended here to avoid webAppFactory being null when switching service implementations
    }
    
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    protected void setRequestFactory(IRequestFactory factory) {
        this.requestFactory = factory;
    }
    
    protected void unsetRequestFactory(IRequestFactory factory) {
     // no-op intended here to avoid requestFactory being null when switching service implementations
    }
    
    public IRequestFactory getRequestFactory() {
        return this.requestFactory;
    }
    
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    protected void setResponseFactory(IResponseFactory factory) {
        this.responseFactory = factory;
    }
    
    protected void unsetResponseFactory(IResponseFactory factory) {
     // no-op intended here to avoid responseFactory being null when switching service implementations
    }
    
    public IResponseFactory getResponseFactory() {
        return this.responseFactory;
    }
    
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    protected void setAsyncContextFactory(AsyncContextFactory factory) {
        asyncContextFactory = factory;
    }
    
    protected void unsetAsyncContextFactory(AsyncContextFactory factory) {
     // no-op intended here to avoid responseFactory being null when switching service implementations
    }
    
    @Override
    public AsyncContextFactory getAsyncContextFactory() {
        return asyncContextFactory;
    }

    // Servlet 4.0
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    protected void setURIMatcherFactory(URIMatcherFactory factory) {
        uriMatcherFactory = factory;
    }
    
    // Servlet 4.0
    protected void unsetURIMatcherFactory(URIMatcherFactory factory){
        // no-op intended here to avoid uriMatcherFactory being null when switching service implementations
    }
    
    // Serlvet 4.0
    @Override
    public URIMatcherFactory getURIMatcherFactory() {
        return uriMatcherFactory;
    }
    
    // Servlet 4.0
    @Reference(cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    protected void setCacheServletWrapperFactory(CacheServletWrapperFactory factory) {
        cacheServletWrapperFactory = factory;
    }
    
    // Servlet 4.0
    protected void unsetCacheServletWrapperFactory(CacheServletWrapperFactory factory) {
        // no-op intended here to avoid cacheServletWrapperFactory being null when switching service implementations
    }

    
    @Reference(service=ServletVersion.class, cardinality=ReferenceCardinality.MANDATORY, policy=ReferencePolicy.DYNAMIC, policyOption=ReferencePolicyOption.GREEDY)
    protected synchronized void setVersion(ServiceReference<ServletVersion> reference) {
        versionRef = reference;
        WebContainer.loadedContainerSpecLevel = (Integer) reference.getProperty("version");
    }

    protected synchronized void unsetVersion(ServiceReference<ServletVersion> reference) {
        if (reference == this.versionRef) {
            versionRef = null;
            WebContainer.loadedContainerSpecLevel = DEFAULT_MAX_VERSION;
        }
    }
    
    public static final int SPEC_LEVEL_UNLOADED = -1;
    public static final int SPEC_LEVEL_30 = 30;
    public static final int SPEC_LEVEL_31 = 31;
    public static final int SPEC_LEVEL_40 = 40;
    public static final int SPEC_LEVEL_50 = 50;
    private static final int DEFAULT_SPEC_LEVEL = 30;
    
    private static int loadedContainerSpecLevel = SPEC_LEVEL_UNLOADED;
    
    public static int getServletContainerSpecLevel() {
        if (WebContainer.loadedContainerSpecLevel == SPEC_LEVEL_UNLOADED) {
            CountDownLatch currentLatch = selfInit;
            // wait for activation
            try {
                currentLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // auto-FFDC
                Thread.currentThread().interrupt();
            }
            currentLatch.countDown(); // don't wait again

            if (WebContainer.loadedContainerSpecLevel == SPEC_LEVEL_UNLOADED) {
                logger.logp(Level.WARNING, CLASS_NAME, "getServletContainerSpecLevel", "servlet.feature.not.loaded.correctly");
                return WebContainer.DEFAULT_SPEC_LEVEL;
            }
        }
        
        return WebContainer.loadedContainerSpecLevel;
    }
    
    
    protected static class CompletedFuture implements Future {

        boolean value = false;
        
        // simple future that is assigned a Boolean when constructed
        public CompletedFuture(boolean v) {
            value = v;
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
        
        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public Boolean get() throws InterruptedException, ExecutionException {
            return value;
        }
    
        @Override
        public Boolean get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
            return value;
        }    
    }
        
}