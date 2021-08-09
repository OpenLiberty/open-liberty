/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import static com.ibm.ejs.container.ContainerConfigConstants.bindToJavaGlobal;
import static com.ibm.ejs.container.ContainerConfigConstants.bindToServerRoot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.RemoveException;
import javax.ejb.Timer;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
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

import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.BeanOFactory;
import com.ibm.ejs.container.BeanOFactory.BeanOFactoryType;
import com.ibm.ejs.container.ContainerEJBException;
import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.ContainerProperties;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.EJBNotFoundException;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.EJSRemoteWrapper;
import com.ibm.ejs.container.EJSWrapperBase;
import com.ibm.ejs.container.HomeOfHomes;
import com.ibm.ejs.container.HomeRecord;
import com.ibm.ejs.container.MDBInternalHome;
import com.ibm.ejs.container.MessageEndpointCollaborator;
import com.ibm.ejs.container.PersistentTimer;
import com.ibm.ejs.container.PersistentTimerTaskHandler;
import com.ibm.ejs.container.TimerNpImpl;
import com.ibm.ejs.container.TimerNpRunnable;
import com.ibm.ejs.container.WrapperManager;
import com.ibm.ejs.container.passivator.StatefulPassivator;
import com.ibm.ejs.csi.ContainerExtensionFactoryBaseImpl;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.ejs.csi.FileBeanStore;
import com.ibm.ejs.csi.SessionKeyFactoryImpl;
import com.ibm.ejs.util.ByteArray;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.EJBModuleConfigData;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.csi.PassivationPolicy;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MetaDataService;
import com.ibm.ws.container.service.metadata.MetaDataSlotService;
import com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory;
import com.ibm.ws.container.service.naming.EJBLocalNamingHelper;
import com.ibm.ws.container.service.naming.LocalColonEJBNamingHelper;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBPMICollaboratorFactory;
import com.ibm.ws.ejbcontainer.EJBSecurityCollaborator;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.JCDIHelper;
import com.ibm.ws.ejbcontainer.diagnostics.IntrospectionWriter;
import com.ibm.ws.ejbcontainer.diagnostics.TrDumpWriter;
import com.ibm.ws.ejbcontainer.failover.SfFailoverKey;
import com.ibm.ws.ejbcontainer.jitdeploy.ClassDefiner;
import com.ibm.ws.ejbcontainer.osgi.EJBAsyncRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBHomeRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBMBeanRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBPersistentTimerRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBRemoteRuntime;
import com.ibm.ws.ejbcontainer.osgi.EJBRuntimeVersion;
import com.ibm.ws.ejbcontainer.osgi.EJBTimerRuntime;
import com.ibm.ws.ejbcontainer.osgi.JCDIHelperFactory;
import com.ibm.ws.ejbcontainer.osgi.MDBRuntime;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.OSGiBeanMetaData;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.OSGiEJBApplicationMetaData;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.OSGiEJBModuleMetaDataImpl;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.WCCMMetaDataImpl;
import com.ibm.ws.ejbcontainer.osgi.internal.naming.EJBBinding;
import com.ibm.ws.ejbcontainer.osgi.internal.naming.EJBJavaColonNamingHelper;
import com.ibm.ws.ejbcontainer.osgi.internal.passivator.StatefulPassivatorImpl;
import com.ibm.ws.ejbcontainer.runtime.AbstractEJBRuntime;
import com.ibm.ws.ejbcontainer.runtime.EJBApplicationEventListener;
import com.ibm.ws.ejbcontainer.runtime.EJBJPAContainer;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntimeConfig;
import com.ibm.ws.ejbcontainer.runtime.NameSpaceBinder;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.exception.WsRuntimeFwException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.DeploymentDescriptor;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.metadata.ejb.AutomaticTimerBean;
import com.ibm.ws.metadata.ejb.EJBMDOrchestrator;
import com.ibm.ws.metadata.ejb.ModuleInitData;
import com.ibm.ws.metadata.ejb.WCCMMetaData;
import com.ibm.ws.resource.ResourceRefConfigFactory;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.adapters.AdapterFactoryService;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.ejbcontainer.WSEJBEndpointManager;
import com.ibm.wsspi.ejbcontainer.WSEJBHandlerResolver;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.kernel.feature.LibertyFeature;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

@Component(service = { ApplicationStateListener.class, DeferredMetaDataFactory.class, EJBRuntimeImpl.class, ServerQuiesceListener.class },
           configurationPid = "com.ibm.ws.ejbcontainer.runtime",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = { "deferredMetaData=EJB" })
public class EJBRuntimeImpl extends AbstractEJBRuntime implements ApplicationStateListener, DeferredMetaDataFactory, ServerQuiesceListener {
    private static final String CLASS_NAME = EJBRuntimeImpl.class.getName();
    private static final TraceComponent tc = Tr.register(EJBRuntimeImpl.class);

    private static RuntimeException rethrow(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        throw new RuntimeException(t);
    }

    private static final String REFERENCE_INJECTION_ENGINE = "injectionEngine";
    private static final String REFERENCE_SERIALIZATION_SERVICE = "serializationService";
    private static final String REFERENCE_MDB_RUNTIME = "mdbRuntime";
    private static final String REFERENCE_JPA_CONTAINER = "jpaContainer";
    private static final String REFERENCE_JCDI_HELPER_FACTORY = "jndiHelperFactory";
    private static final String REFERENCE_EJB_ASYNC_RUNTIME = "ejbAsyncRuntime";
    private static final String REFERENCE_EJB_TIMER_RUNTIME = "ejbTimerRuntime";
    private static final String REFERENCE_EJB_PERSISTENT_TIMER_RUNTIME = "ejbPersistentTimerRuntime";
    private static final String REFERENCE_EJB_HOME_RUNTIME = "ejbHomeRuntime";
    private static final String REFERENCE_EJB_REMOTE_RUNTIME = "ejbRemoteRuntime";
    private static final String REFERENCE_EJB_MBEAN_RUNTIME = "ejbMBeanRuntime";
    private static final String REFERENCE_MANAGED_OBJECT_SERVICE = "managedObjectService";
    private static final String REFERENCE_CLASSLOADING_SERVICE = "classLoadingService";
    private static final Version DEFAULT_VERSION = EJBRuntimeVersion.VERSION_3_1;
    private static final String REFERENCE_RUNTIME_VERSION = "ejbRuntimeVersion";

    private Version runtimeVersion = DEFAULT_VERSION;
    private EJBMDOrchestrator ejbMDOrchestrator;
    private EJSContainer container;
    private final ClassDefiner classDefiner = new ClassDefiner();
    private final AtomicServiceReference<InjectionEngine> injectionEngineSRRef = new AtomicServiceReference<InjectionEngine>(REFERENCE_INJECTION_ENGINE);
    private EJBSecurityCollaborator<?> securityCollaborator;
    private EJBJavaColonNamingHelper javaColonHelper;
    private EJBLocalNamingHelper<EJBBinding> ejbLocalNamingHelper;
    private LocalColonEJBNamingHelper<EJBBinding> localColonNamingHelper;
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledExecutorService deferrableScheduledExecutorService;
    private J2EENameFactory j2eeNameFactory;
    private MetaDataService metaDataService;
    private ClassLoadingService classLoadingService;
    private MetaDataSlot appSlot;
    private UserTransaction userTransaction;
    private ResourceRefConfigFactory resourceRefConfigFactory;
    private final AtomicServiceReference<EJBRuntimeVersion> runtimeVersionRef = new AtomicServiceReference<EJBRuntimeVersion>(REFERENCE_RUNTIME_VERSION);
    private final AtomicServiceReference<SerializationService> serializationServiceRef = new AtomicServiceReference<SerializationService>(REFERENCE_SERIALIZATION_SERVICE);
    private final AtomicServiceReference<MDBRuntime> mdbRuntimeServiceRef = new AtomicServiceReference<MDBRuntime>(REFERENCE_MDB_RUNTIME);
    private final AtomicServiceReference<EJBJPAContainer> jpaContainerServiceRef = new AtomicServiceReference<EJBJPAContainer>(REFERENCE_JPA_CONTAINER);
    private final AtomicServiceReference<JCDIHelperFactory> jcdiHelperFactoryServiceRef = new AtomicServiceReference<JCDIHelperFactory>(REFERENCE_JCDI_HELPER_FACTORY);
    private final AtomicServiceReference<EJBAsyncRuntime> ejbAsyncRuntimeServiceRef = new AtomicServiceReference<EJBAsyncRuntime>(REFERENCE_EJB_ASYNC_RUNTIME);
    private final AtomicServiceReference<EJBTimerRuntime> ejbTimerRuntimeServiceRef = new AtomicServiceReference<EJBTimerRuntime>(REFERENCE_EJB_TIMER_RUNTIME);
    private final AtomicServiceReference<EJBPersistentTimerRuntime> ejbPersistentTimerRuntimeServiceRef = new AtomicServiceReference<EJBPersistentTimerRuntime>(REFERENCE_EJB_PERSISTENT_TIMER_RUNTIME);
    private final AtomicServiceReference<EJBHomeRuntime> ejbHomeRuntimeServiceRef = new AtomicServiceReference<EJBHomeRuntime>(REFERENCE_EJB_HOME_RUNTIME);
    private final AtomicServiceReference<EJBRemoteRuntime> ejbRemoteRuntimeServiceRef = new AtomicServiceReference<EJBRemoteRuntime>(REFERENCE_EJB_REMOTE_RUNTIME);
    private final AtomicServiceReference<EJBMBeanRuntime> ejbMBeanRuntimeServiceRef = new AtomicServiceReference<EJBMBeanRuntime>(REFERENCE_EJB_MBEAN_RUNTIME);
    private final AtomicServiceReference<ManagedObjectService> managedObjectServiceRef = new AtomicServiceReference<ManagedObjectService>(REFERENCE_MANAGED_OBJECT_SERVICE);

    private volatile CountDownLatch remoteFeatureLatch = null;
    private volatile boolean ejbRuntimeActive = false;
    private volatile boolean serverStopping = false;

    private WSEJBHandlerResolver webServicesHandlerResolver;
    private EJBPMICollaboratorFactory ejbPMICollaboratorFactory;

    private boolean persistentTimerMsgLogged = false;

    private static final String CACHE_SIZE = "cacheSize";
    private static final String CACHE_CLEANUP_INTERVAL = "cacheCleanupInterval";
    private static final String POOL_CLEANUP_INTERVAL = "poolCleanupInterval";
    private static final String START_EJBS_AT_APP_START = "startEJBsAtAppStart";

    private static final String BIND_TO_SERVER_ROOT = "bindToServerRoot";
    private static final String BIND_TO_JAVA_GLOBAL = "bindToJavaGlobal";
    private static final String DISABLE_SHORT_DEFAULT_BINDINGS = "disableShortDefaultBindings";
    private static final String CUSTOM_BINDINGS_ON_ERROR = "customBindingsOnError";

    @Override
    public void serverStopping() {
        serverStopping = true;
        TimerNpRunnable.serverStopping();

        EJBPersistentTimerRuntime ejbPersistentTimerRuntime = ejbPersistentTimerRuntimeServiceRef.getService();
        if (ejbPersistentTimerRuntime != null) {
            ejbPersistentTimerRuntime.serverStopping();
        }

        CountDownLatch remoteLatch = remoteFeatureLatch;
        if (remoteLatch != null) {
            remoteFeatureLatch = null;
            remoteLatch.countDown();
        }
    }

    public void introspect(IntrospectionWriter writer) {
        EJBSecurityCollaborator<?> securityCollaborator;
        synchronized (this) {
            securityCollaborator = this.securityCollaborator;
        }

        writer.begin("EJBRuntime Fields");
        writer.println("deferrableScheduledExecutorService = " + deferrableScheduledExecutorService);
        writer.println("ejbAsyncRuntime          = " + ejbAsyncRuntimeServiceRef);
        writer.println("ejbTimerRuntime          = " + ejbTimerRuntimeServiceRef);
        writer.println("ejbPersistentTimerRuntime= " + ejbPersistentTimerRuntimeServiceRef);
        writer.println("ejbHomeRuntime           = " + ejbHomeRuntimeServiceRef);
        writer.println("ejbRemoteRuntime         = " + ejbRemoteRuntimeServiceRef);
        writer.println("ejbMBeanRuntime          = " + ejbMBeanRuntimeServiceRef);
        writer.println("injectionEngine          = " + injectionEngineSRRef);
        writer.println("j2eeNameFactory          = " + j2eeNameFactory);
        writer.println("javaColonHelper          = " + javaColonHelper);
        writer.println("jpaContainer             = " + jpaContainerServiceRef);
        writer.println("mdbRuntime               = " + mdbRuntimeServiceRef);
        writer.println("jcdiHelperFactory        = " + jcdiHelperFactoryServiceRef);
        writer.println("metaDataService          = " + metaDataService);
        writer.println("appSlot                  = " + appSlot);
        writer.println("resourceRefConfigFactory = " + resourceRefConfigFactory);
        writer.println("scheduledExecutorService = " + scheduledExecutorService);
        writer.println("securityCollaborator     = " + securityCollaborator);
        writer.println("serializationService     = " + serializationServiceRef);
        writer.println("userTransaction          = " + userTransaction);
        writer.println("runtimeVersion           = " + runtimeVersionRef);
        writer.end();

        if (container != null) {
            container.introspect(writer, true);
        }
    }

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        BundleContext bc = cc.getBundleContext();

        injectionEngineSRRef.activate(cc);
        serializationServiceRef.activate(cc);
        mdbRuntimeServiceRef.activate(cc);
        jcdiHelperFactoryServiceRef.activate(cc);
        jpaContainerServiceRef.activate(cc);
        ejbAsyncRuntimeServiceRef.activate(cc);
        ejbTimerRuntimeServiceRef.activate(cc);
        ejbPersistentTimerRuntimeServiceRef.activate(cc);
        ejbHomeRuntimeServiceRef.activate(cc);
        ejbRemoteRuntimeServiceRef.activate(cc);
        ejbMBeanRuntimeServiceRef.activate(cc);
        managedObjectServiceRef.activate(cc);
        runtimeVersionRef.activate(cc);

        this.ejbMDOrchestrator = new EJBMDOrchestratorImpl(managedObjectServiceRef);

        int cacheSize = (Integer) properties.get(CACHE_SIZE);
        long cacheSweepIntervalSeconds = (Long) properties.get(CACHE_CLEANUP_INTERVAL);
        long inactivePoolCleanupIntervalSeconds = (Long) properties.get(POOL_CLEANUP_INTERVAL);
        Boolean startEjbsAtAppStart = (Boolean) properties.get(START_EJBS_AT_APP_START);

        processCustomBindingsConfig(properties);

        EJSContainer container = new EJSContainer();

        EJBRuntimeConfig config = new EJBRuntimeConfig();
        config.setJ2EENameFactory(j2eeNameFactory);
        config.setEJBMDOrchestrator(ejbMDOrchestrator);
        config.setContainer(container);
        config.setWrapperManager(new WrapperManager(container));
        config.setOrbUtils(new OrbUtilsImpl());
        config.setContainerExtensionFactory(new ContainerExtensionFactoryBaseImpl());
        config.setObjectCopier(new ObjectCopierImpl(serializationServiceRef));
        config.setPassivationPolicy(PassivationPolicy.ON_CACHE_FULL);
        config.setStatefulSessionKeyFactory(new SessionKeyFactoryImpl());
        config.setStatefulSessionHandleFactory(new SessionHandleFactoryImpl());
        config.setCacheSize(cacheSize);
        config.setCacheSweepInterval(TimeUnit.MILLISECONDS.convert(cacheSweepIntervalSeconds,
                                                                   TimeUnit.SECONDS));
        config.setInactivePoolCleanupInterval(TimeUnit.MILLISECONDS.convert(inactivePoolCleanupIntervalSeconds,
                                                                            TimeUnit.SECONDS));
        config.setWSEJBHandlerResolver(this.webServicesHandlerResolver);

        File passivationDir = bc.getDataFile("passivation");
        createPassivationDirectory(passivationDir);
        FileBeanStore fileBeanStore = new FileBeanStore(passivationDir.getAbsolutePath());
        StatefulPassivator statefulPassivator = new StatefulPassivatorImpl(fileBeanStore, container, serializationServiceRef);
        config.setStatefulPassivator(statefulPassivator);

        try {
            start(config);
        } catch (CSIException e) {
            throw new IllegalStateException(e);
        }

        updateStartEjbsAtAppStart(startEjbsAtAppStart);

        synchronized (this) {
            this.container = container;
            updateEJSContainerFromRuntimeVersion();
            if (ejbTimerRuntimeServiceRef.getReference() != null || ejbPersistentTimerRuntimeServiceRef.getReference() != null) {
                container.setupTimers();
            }
            if (securityCollaborator != null) {
                container.setSecurityCollaborator(securityCollaborator);
            }
            if (ejbPMICollaboratorFactory != null) {
                container.setPMICollaboratorFactory(ejbPMICollaboratorFactory);
            }
        }
        ejbRuntimeActive = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            ContainerProperties.introspect(new TrDumpWriter(tc));
        }
    }

    private void processCustomBindingsConfig(Map<String, Object> properties) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processCustomBindingsConfig");

        // Overwrite the JVM properties if config is set, otherwise we will use the JVM props or the JVM property defaults
        ContainerProperties.BindToServerRoot = properties.get(BIND_TO_SERVER_ROOT) != null ? (Boolean) properties.get(BIND_TO_SERVER_ROOT) : System.getProperty(bindToServerRoot,
                                                                                                                                                                "true").equalsIgnoreCase("true");
        ContainerProperties.BindToJavaGlobal = properties.get(BIND_TO_JAVA_GLOBAL) != null ? (Boolean) properties.get(BIND_TO_JAVA_GLOBAL) : System.getProperty(bindToJavaGlobal,
                                                                                                                                                                "true").equalsIgnoreCase("true");

        OnError customBindingsOnError = OnError.WARN;
        try {
            customBindingsOnError = (OnError) properties.get(CUSTOM_BINDINGS_ON_ERROR);
        } catch (IllegalArgumentException iae) {
            //We'll fall back to default
        }
        ContainerProperties.customBindingsOnErr = customBindingsOnError;

        String disableShortBindingsProperty = (String) properties.get(DISABLE_SHORT_DEFAULT_BINDINGS);
        if (disableShortBindingsProperty != null) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "Disable short default bindings string: " + disableShortBindingsProperty);
            }
            ArrayList<String> DisableShortDefaultBindings = new ArrayList<String>();
            if (!disableShortBindingsProperty.contains("*")) {
                String[] apps = disableShortBindingsProperty.split(":");
                for (int i = 0; i < apps.length; i++) {
                    DisableShortDefaultBindings.add(apps[i]);
                }
            }
            // If they pass in * we will have an empty initialized list and then disable for all apps

            // If JVM prop has something set, merge them
            ContainerProperties.DisableShortDefaultBindings = DisableShortDefaultBindings;

            if (ContainerProperties.DisableShortDefaultBindingsFromJVM != null) {
                ContainerProperties.DisableShortDefaultBindings.addAll(ContainerProperties.DisableShortDefaultBindingsFromJVM);
            }
        } else if (ContainerProperties.DisableShortDefaultBindingsFromJVM != null) {
            ContainerProperties.DisableShortDefaultBindings = ContainerProperties.DisableShortDefaultBindingsFromJVM;
        } else {
            ContainerProperties.DisableShortDefaultBindings = null;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processCustomBindingsConfig");
    }

    private void updateEJSContainerFromRuntimeVersion() {
        container.setAllowTimerAccessOutsideBean(runtimeVersion.compareTo(EJBRuntimeVersion.VERSION_3_2) >= 0);
        container.setTransactionalStatefulLifecycleMethods(runtimeVersion.compareTo(EJBRuntimeVersion.VERSION_3_2) >= 0);
        container.setNoMethodInterfaceMDBEnabled(runtimeVersion.compareTo(EJBRuntimeVersion.VERSION_3_2) >= 0);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> properties) {
        int cacheSize = (Integer) properties.get(CACHE_SIZE);
        long cacheSweepIntervalSeconds = (Long) properties.get(CACHE_CLEANUP_INTERVAL);
        long inactivePoolCleanupIntervalSeconds = (Long) properties.get(POOL_CLEANUP_INTERVAL);
        Boolean startEjbsAtAppStart = (Boolean) properties.get(START_EJBS_AT_APP_START);

        processCustomBindingsConfig(properties);

        container.setPreferredCacheSize(cacheSize);

        container.setInactiveCacheCleanupInterval(TimeUnit.MILLISECONDS.convert(cacheSweepIntervalSeconds,
                                                                                TimeUnit.SECONDS));

        container.setInactivePoolCleanupInterval(TimeUnit.MILLISECONDS.convert(inactivePoolCleanupIntervalSeconds,
                                                                               TimeUnit.SECONDS));

        updateStartEjbsAtAppStart(startEjbsAtAppStart);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            ContainerProperties.introspect(new TrDumpWriter(tc));
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        ejbRuntimeActive = false;
        stop();

        injectionEngineSRRef.deactivate(cc);
        serializationServiceRef.deactivate(cc);
        mdbRuntimeServiceRef.deactivate(cc);
        jcdiHelperFactoryServiceRef.deactivate(cc);
        jpaContainerServiceRef.deactivate(cc);
        ejbAsyncRuntimeServiceRef.deactivate(cc);
        ejbTimerRuntimeServiceRef.deactivate(cc);
        ejbPersistentTimerRuntimeServiceRef.deactivate(cc);
        ejbHomeRuntimeServiceRef.deactivate(cc);
        ejbRemoteRuntimeServiceRef.deactivate(cc);
        ejbMBeanRuntimeServiceRef.deactivate(cc);
        managedObjectServiceRef.deactivate(cc);

        runtimeVersionRef.deactivate(cc);
    }

    @Reference(name = REFERENCE_RUNTIME_VERSION,
               service = EJBRuntimeVersion.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected synchronized void setEJBRuntimeVersion(ServiceReference<EJBRuntimeVersion> ref) {
        this.runtimeVersionRef.setReference(ref);
        runtimeVersion = Version.parseVersion((String) ref.getProperty(EJBRuntimeVersion.VERSION));
        if (container != null) {
            updateEJSContainerFromRuntimeVersion();
        }
    }

    protected synchronized void unsetEJBRuntimeVersion(ServiceReference<EJBRuntimeVersion> ref) {
        this.runtimeVersionRef.unsetReference(ref);
        runtimeVersion = DEFAULT_VERSION;
        if (container != null) {
            updateEJSContainerFromRuntimeVersion();
        }
    }

    @Reference
    protected void setJ2EENameFactory(J2EENameFactory ref) {
        this.j2eeNameFactory = ref;
    }

    protected void unsetJ2EENameFactory(J2EENameFactory ref) {
    }

    @Reference
    protected void setMetaDataService(MetaDataService ref) {
        this.metaDataService = ref;
    }

    protected void unsetMetaDataService(MetaDataService ref) {
        this.metaDataService = null;
    }

    @Reference
    protected void setMetaDataSlotService(MetaDataSlotService ref) {
        this.appSlot = ref.reserveMetaDataSlot(ApplicationMetaData.class);
    }

    protected void unsetMetaDataSlotService(MetaDataSlotService ref) {
        // Nothing.
    }

    @Reference
    protected void setUserTransaction(UserTransaction ref) {
        this.userTransaction = ref;
    }

    protected void unsetUserTransaction(UserTransaction ref) {
        this.userTransaction = null;
    }

    @Reference
    protected void setResourceRefConfigFactory(ResourceRefConfigFactory ref) {
        this.resourceRefConfigFactory = ref;
    }

    protected void unsetResourceRefConfigFactory(ResourceRefConfigFactory ref) {
        this.resourceRefConfigFactory = null;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected synchronized void setSecurityCollaborator(EJBSecurityCollaborator<?> ref) {
        securityCollaborator = ref;
        if (container != null) {
            container.setSecurityCollaborator(ref);
        }
    }

    protected synchronized void unsetSecurityCollaborator(EJBSecurityCollaborator<?> ref) {
        securityCollaborator = null;
        container.setSecurityCollaborator(null);
    }

    @Override
    public boolean isStopping() {
        return serverStopping;
    }

    @Override
    public ClassLoader getServerClassLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public int getMetaDataSlotSize(Class<?> metaDataClass) {
        return 0;
    }

    @Override
    public WCCMMetaData setupBean(BeanMetaData bmd, boolean hasRemote) {
        return new WCCMMetaDataImpl(resourceRefConfigFactory.createResourceRefConfigList());
    }

    @Override
    public void setupAsync() {
        // The presence of async methods will be ignored unless specifically disabled.
        if (ContainerProperties.DisableAsyncMethods) {
            throw new EJBException("Asynchronous methods have been disabled.");
        }

        // No other setup required:
        // - DS takes care of providing an Executor
        // - No Remote support, so no RemoteResult reaper needed.
    }

    @Override
    public Future<?> scheduleAsync(EJSWrapperBase wrapper, EJBMethodInfoImpl methodInfo, int methodId, Object[] args) throws RemoteException {
        EJBAsyncRuntime ejbAsyncRuntime = ejbAsyncRuntimeServiceRef.getService();
        if (ejbAsyncRuntime == null) {
            J2EEName j2eeName = methodInfo.getJ2EEName();
            String methodName = methodInfo.getMethodName();
            String appName = j2eeName.getApplication();
            String moduleName = j2eeName.getModule();
            String ejbName = j2eeName.getComponent();

            String msgTxt = Tr.formatMessage(tc, "ASYNC_METHODS_NOT_SUPPORTED_CNTR4017E",
                                             methodName,
                                             ejbName,
                                             moduleName,
                                             appName);

            throw new EJBException(msgTxt);
        }

        return ejbAsyncRuntime.scheduleAsync(wrapper, methodInfo, methodId, args);
    }

    @Override
    @FFDCIgnore(IllegalStateException.class)
    public void setupTimers(BeanMetaData bmd) {

        // No additional setup is required for non-persistent timers, but if the application has
        // persistent automatic timers or a timeout method (programmatic) which could be persistent
        // then database polling needs to be enabled in the persistent timer service.
        //
        // Note: if the ejbPersistentTimer feature is not enabled, then no setup is performed.
        //       Persistent automatic timers are ignored; programmatic will fail on creation

        boolean hasPersistentAutomaticTimers = bmd._moduleMetaData.ivHasPersistentAutomaticTimers;

        if (bmd.isTimedObject || hasPersistentAutomaticTimers) {
            EJBPersistentTimerRuntime ejbPersistentTimerRuntime = ejbPersistentTimerRuntimeServiceRef.getService();
            if (ejbPersistentTimerRuntime != null) {
                try {
                    ejbPersistentTimerRuntime.enableDatabasePolling();
                } catch (IllegalStateException ex) {
                    if (hasPersistentAutomaticTimers) {
                        FFDCFilter.processException(ex, CLASS_NAME + ".setupTimers", "560", this, new Object[] { bmd });

                        // When automatic timers are present, fail application start if the ejbPersistentTimer feature
                        // is enabled but cannot access the database; unable to automatically create them
                        Tr.error(tc, "AUTOMATIC_PERSISTENT_TIMERS_NOT_AVAILABLE_CNTR4020E", bmd.j2eeName.getComponent(), bmd.j2eeName.getModule(), bmd.j2eeName.getApplication());
                        throw ex;
                    } else {
                        // When programmatic timers exist, provide an informational message if the ejbPersistentTimer
                        // feature is enabled but cannot access the database; any previously created timers will not
                        // run; new ones will fail on programmatic timer create. This is a one time message that occurs
                        // if at least one programmatic timer application is present.
                        if (!persistentTimerMsgLogged) {
                            Tr.info(tc, "PERSISTENT_TIMERS_NOT_AVAILABLE_CNTR4021I");
                            persistentTimerMsgLogged = true;
                        }
                    }
                }
            }
        }
    }

    @Override
    // RTC109678
    public int createNonPersistentAutomaticTimers(String appName, String moduleName, List<AutomaticTimerBean> timerBeans) {
        if (ejbTimerRuntimeServiceRef.getReference() == null) {
            for (AutomaticTimerBean bean : timerBeans) {
                Tr.warning(tc, "AUTOMATIC_TIMERS_NOT_SUPPORTED_CNTR4010W", bean.getBeanMetaData().getName(), moduleName, appName);
            }
            return 0;
        }
        return super.createNonPersistentAutomaticTimers(appName, moduleName, timerBeans);
    }

    private EJBPersistentTimerRuntime getPersistentTimerRuntime() {
        EJBPersistentTimerRuntime ejbPersistentTimerRuntime = ejbPersistentTimerRuntimeServiceRef.getService();
        if (ejbPersistentTimerRuntime == null) {
            String msgTxt = Tr.formatMessage(tc, "PERSISTENT_TIMERS_NOT_SUPPORTED_CNTR4019E");
            throw new EJBException(msgTxt);
        }
        return ejbPersistentTimerRuntime;
    }

    @Override
    protected int createPersistentAutomaticTimers(String appName, String moduleName, List<AutomaticTimerBean> timerBeans) throws RuntimeWarning {
        EJBPersistentTimerRuntime ejbPersistentTimerRuntime = ejbPersistentTimerRuntimeServiceRef.getService();
        if (ejbPersistentTimerRuntime == null) {
            return super.createPersistentAutomaticTimers(appName, moduleName, timerBeans);
        }
        return ejbPersistentTimerRuntime.createPersistentAutomaticTimers(appName, moduleName, timerBeans);
    }

    @Override
    protected Timer createPersistentExpirationTimer(BeanId beanId, Date expiration, long interval, @Sensitive Serializable info) {
        return getPersistentTimerRuntime().createPersistentExpirationTimer(beanId, expiration, interval, info);
    }

    @Override
    protected Timer createPersistentCalendarTimer(BeanId beanId, ParsedScheduleExpression parsedExpr, @Sensitive Serializable info) {
        return getPersistentTimerRuntime().createPersistentCalendarTimer(beanId, parsedExpr, info);
    }

    @Override
    public PersistentTimer getPersistentTimer(long taskId, J2EEName j2eeName, PersistentTimerTaskHandler taskHandler) {
        return getPersistentTimerRuntime().getPersistentTimer(taskId, j2eeName, taskHandler);
    }

    @Override
    public Timer getPersistentTimer(long taskId) {
        EJBPersistentTimerRuntime ejbPersistentTimerRuntime = ejbPersistentTimerRuntimeServiceRef.getService();
        if (ejbPersistentTimerRuntime == null) {
            // TODO : return a Timer proxy
        }
        return ejbPersistentTimerRuntime.getPersistentTimer(taskId);
    }

    @Override
    public Timer getPersistentTimerFromStore(long taskId) throws NoSuchObjectLocalException {
        return getPersistentTimerRuntime().getPersistentTimerFromStore(taskId);
    }

    private EJBTimerRuntime getTimerRuntime() {
        EJBTimerRuntime ejbTimerRuntime = ejbTimerRuntimeServiceRef.getService();
        if (ejbTimerRuntime == null) {
            String msgTxt = Tr.formatMessage(tc, "NON_PERSISTENT_TIMERS_NOT_SUPPORTED_CNTR4018E");
            throw new EJBException(msgTxt);
        }
        return ejbTimerRuntime;
    }

    @Override
    protected Timer createNonPersistentExpirationTimer(BeanO beanO, Date expiration, long interval, @Sensitive Serializable info) {
        // Make sure the EJBTimerRuntime is available
        getTimerRuntime();

        return super.createNonPersistentExpirationTimer(beanO, expiration, interval, info);
    }

    @Override
    protected Timer createNonPersistentCalendarTimer(BeanO beanO, ParsedScheduleExpression parsedExpr, @Sensitive Serializable info) {
        // Make sure the EJBTimerRuntime is available
        getTimerRuntime();

        return super.createNonPersistentCalendarTimer(beanO, parsedExpr, info);
    }

    @Override
    public TimerNpRunnable createNonPersistentTimerTaskHandler(TimerNpImpl timer) {
        return getTimerRuntime().createNonPersistentTimerTaskHandler(timer);
    }

    @Override
    public Collection<Timer> getTimers(BeanO beanO) {
        // Make sure the EJBTimerRuntime is available
        getTimerRuntime();

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        BeanId beanId = beanO.getId();

        // Find non-persistent timers for this beanID that have started
        // and add them to the returned Collection

        ContainerTx tx = beanO.getContainerTx();

        if (tx == null) {

            // singleton beans will have ivContainerTx==null, so get it from the container
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "getTimers found ivContainerTx null.  Calling getCurrentTx(false)");
            }
            tx = beanO.getContainer().getCurrentContainerTx();
        }

        Collection<Timer> timers = TimerNpImpl.findTimersByBeanId(beanId, tx);

        // Find non-persistent timers for this beanID that are queued to start
        // and add them to the returned Collection
        if ((tx != null) && tx.timersQueuedToStart != null) {
            Collection<TimerNpImpl> timersQueuedToStart = tx.timersQueuedToStart.values();
            for (TimerNpImpl timer : timersQueuedToStart) {
                if (timer.getIvBeanId().equals(beanId)) {
                    timers.add(timer);
                }
            }
        } else {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "Beano.getTimers could not find a tran context.");
            }
        }

        // Find all persistent timers for this bean
        EJBPersistentTimerRuntime ejbPersistentTimerRuntime = ejbPersistentTimerRuntimeServiceRef.getService();
        if (ejbPersistentTimerRuntime != null && ejbPersistentTimerRuntime.isConfigured()) {
            timers.addAll(ejbPersistentTimerRuntime.getTimers(beanId));
        }

        return timers;
    }

    @Override
    public Collection<Timer> getAllTimers(EJBModuleMetaDataImpl mmd) {
        // Make sure the EJBTimerRuntime is available
        getTimerRuntime();

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getAllTimers( " + mmd + ")");

        // Find non-persistent timers for this module that have started
        // and add them to the returned Collection

        Collection<Timer> timers = TimerNpImpl.findTimersByModule(mmd);

        // Find non-persistent timers for this module that are queued to start
        // and add them to the returned Collection
        ContainerTx tx = container.getCurrentContainerTx();
        if ((tx != null) && tx.timersQueuedToStart != null) {
            Collection<TimerNpImpl> timersQueuedToStart = tx.timersQueuedToStart.values();
            for (TimerNpImpl timer : timersQueuedToStart) {
                if (timer.getIvBeanId().getBeanMetaData().getModuleMetaData() == mmd) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "getAllTimers: adding queued timer: " + timer);
                    timers.add(timer);
                }
            }
        } else {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "getAllTimers did not find a transaction context.");
        }

        // Find all persistent timers for this module
        EJBPersistentTimerRuntime ejbPersistentTimerRuntime = ejbPersistentTimerRuntimeServiceRef.getService();
        if (ejbPersistentTimerRuntime != null && ejbPersistentTimerRuntime.isConfigured()) {
            timers.addAll(ejbPersistentTimerRuntime.getAllTimers(mmd.ivAppName, mmd.ivName, mmd.ivAllowsCachedTimerData));
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getAllTimers: " + timers.size());

        return timers;
    }

    @Override
    public void removeTimers(BeanO beanO) {
        // Ignore silently.
    }

    @Override
    public void removeTimers(J2EEName j2eeName) {
        TimerNpImpl.removeTimersByJ2EEName(j2eeName); // F743-26072
    }

    @Override
    protected long getLateTimerThreshold() {
        EJBTimerRuntime timerRuntime = ejbTimerRuntimeServiceRef.getService();
        if (timerRuntime != null) {
            return timerRuntime.getLateTimerThreshold();
        }
        // Late timer threshold is disabled if timer service not available
        return 0;
    }

    @Override
    public ClassDefiner getClassDefiner() {
        return classDefiner;
    }

    @Override
    public ObjectInputStream createObjectInputStream(InputStream in, ClassLoader loader) throws IOException {
        return serializationServiceRef.getServiceWithException().createObjectInputStream(in, loader);
    }

    @Override
    protected EJBModuleConfigData createEJBModuleConfigData(ModuleInitData mid) {
        return null;
    }

    @Override
    protected NameSpaceBinder<?> createNameSpaceBinder(EJBModuleMetaDataImpl mmd) {
        OSGiEJBModuleMetaDataImpl osgiMMD = getOSGiEJBModuleMetaDataImpl(mmd);
        waitForEJBRemoteRuntime();
        if (osgiMMD.isSystemModule()) {
            // Use the NameSpaceBinder if cached (i.e. module starting), otherwise create a new one
            NameSpaceBinder<?> binder = osgiMMD.systemModuleNameSpaceBinder;
            return binder != null ? binder : new SystemNameSpaceBinderImpl(ejbRemoteRuntimeServiceRef.getService());
        }
        return new NameSpaceBinderImpl(mmd, getJavaColonHelper(), getEJBLocalNamingHelper(), getLocalColonEJBNamingHelper(), ejbRemoteRuntimeServiceRef);
    }

    @Override
    protected void initializeTimerService(boolean checkDatabase) {
        // Ignore silently.
    }

    @Override
    protected void registerMBeans(ModuleInitData mid, EJBModuleMetaDataImpl mmd) {
        OSGiEJBModuleMetaDataImpl mmdImpl = getOSGiEJBModuleMetaDataImpl(mmd);
        if (!mmdImpl.isSystemModule()) {
            EJBMBeanRuntime mbeanRuntime = ejbMBeanRuntimeServiceRef.getService();
            if (mbeanRuntime != null) {
                String appName = mmd.getEJBApplicationMetaData().isStandaloneModule() ? null : mmd.ivAppName;

                Container container = null;
                String ddPath = null;
                if (mid.ivEJBJar instanceof DeploymentDescriptor) {
                    container = ((ModuleInitDataImpl) mid).container;
                    ddPath = ((DeploymentDescriptor) mid.ivEJBJar).getDeploymentDescriptorPath();
                }

                List<EJBComponentMetaData> ejbs = new ArrayList<EJBComponentMetaData>(mmd.ivBeanMetaDatas.size());
                for (BeanMetaData bmd : mmd.ivBeanMetaDatas.values()) {
                    if (bmd.type != InternalConstants.TYPE_MANAGED_BEAN) {
                        ejbs.add(bmd);
                    }
                }

                mmdImpl.mbeanServiceReg = mbeanRuntime.registerModuleMBean(appName, mmd.ivName, container, ddPath, ejbs);

                for (OSGiBeanMetaData bmd : mmdImpl.getOSGiBeanMetaDatas()) {
                    if (bmd.type != InternalConstants.TYPE_MANAGED_BEAN) {
                        bmd.mbeanServiceReg = mbeanRuntime.registerEJBMBean(appName, mmd.ivName, bmd.enterpriseBeanName, bmd.getEJBType());
                    }
                }
            }
        }
    }

    @Override
    @FFDCIgnore(IllegalStateException.class)
    protected void deregisterMBeans(EJBModuleMetaDataImpl mmd) {
        OSGiEJBModuleMetaDataImpl mmdImpl = getOSGiEJBModuleMetaDataImpl(mmd);
        if (mmdImpl.mbeanServiceReg != null) {
            try {
                mmdImpl.mbeanServiceReg.unregister();
            } catch (IllegalStateException isex) {
                // The MBean has already been unregistered (server shutdown)
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, mmd.getJ2EEName() + " MBean already unregistered: " + isex);
            }
            mmdImpl.mbeanServiceReg = null;
        }

        for (OSGiBeanMetaData bmd : mmdImpl.getOSGiBeanMetaDatas()) {
            if (bmd.mbeanServiceReg != null) {
                try {
                    bmd.mbeanServiceReg.unregister();
                } catch (IllegalStateException isex) {
                    // The MBean has already been unregistered (server shutdown)
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, bmd.getJ2EEName() + " MBean already unregistered: " + isex);
                }
                bmd.mbeanServiceReg = null;
            }
        }
    }

    @Override
    protected void startMDBs(ModuleInitData mid, EJBModuleMetaDataImpl mmd) {
        ComponentMetaData[] listMD = mmd.getComponentMetaDatas();

        if (listMD != null) {
            int metaDataLength = listMD.length; //345616

            for (int i = 0; i < metaDataLength; ++i) { //345616
                OSGiBeanMetaData bmd = getOSGiBeanMetaData((BeanMetaData) listMD[i]);
                if (bmd.isMessageDrivenBean() && bmd.beanRuntime != null) {
                    EJSHome ejsHome = bmd.getHome(); //345616

                    // Does this MDB home want to be notified of application events/
                    if (ejsHome instanceof EJBApplicationEventListener) { // d450478
                        // Yep, then add it to the list of listeners for this module.
                        EJBApplicationEventListener listener = (EJBApplicationEventListener) ejsHome;
                        mmd.addApplicationEventListener(listener);
                    }

                    sendMDBBindingMessage(bmd); // d744887

                    // Now activate this message endpoint.
                    try {
                        ((MDBInternalHome) ejsHome).activateEndpoint(); //357657, d659020.5
                    } catch (Exception ex) {
                        throw new ContainerEJBException(ex);
                    }
                }
            }
        }
    }

    @Override
    protected boolean isReferenceProcessingNeededAtStart(BeanMetaData bmd) {
        return false; // 83611 - temporarily set to false to prevent early loading of JPA entities
    }

    @Override
    protected void fireMetaDataCreated(EJBModuleMetaDataImpl mmd) {
        // Nothing (except log metadata dump).  This is done by the application handler.
        if (TraceComponent.isAnyTracingEnabled() & tc.isDebugEnabled())
            Tr.debug(tc, mmd.toDumpString());
    }

    @Override
    protected void fireMetaDataCreated(BeanMetaData bmd) throws RuntimeWarning {
        if (getOSGiBeanMetaData(bmd).beanRuntime != null &&
            !getOSGiEJBModuleMetaDataImpl(bmd._moduleMetaData).isSystemModule()) {
            boolean success = false;
            try {
                metaDataService.fireComponentMetaDataCreated(bmd);
                success = true;
            } catch (MetaDataException e) {
                throw new RuntimeWarning(e);
            } finally {
                bmd.ivMetaDataDestroyRequired = success;
            }
        }
    }

    @Override
    protected void fireMetaDataDestroyed(BeanMetaData bmd) {
        metaDataService.fireComponentMetaDataDestroyed(bmd);
    }

    public EJBModuleMetaDataImpl createModuleMetaData(ModuleInfo moduleInfo, ModuleInitData mid) {
        ApplicationInfo appInfo = moduleInfo.getApplicationInfo();
        ApplicationMetaData amd = ((ExtendedApplicationInfo) appInfo).getMetaData();

        EJBApplicationMetaData ejbAMD = (EJBApplicationMetaData) amd.getMetaData(appSlot);
        if (ejbAMD == null) {
            boolean standaloneModule = moduleInfo.getContainer() == appInfo.getContainer();
            String appName = mid.ivAppName;
            String appLogicalName = standaloneModule ? null : appInfo.getName();

            ejbAMD = new OSGiEJBApplicationMetaData(container, appName, appLogicalName, standaloneModule, amd, false, true);
            amd.setMetaData(appSlot, ejbAMD);
        }

        EJBModuleMetaDataImpl mmd = ejbMDOrchestrator.createEJBModuleMetaDataImpl(ejbAMD, mid, null, container);

        mmd.ivJCDIHelper = getJCDIHelper(moduleInfo.getContainer());

        return mmd;
    }

    public JCDIHelper getJCDIHelper(final Container moduleContainer) {
        return new JCDIHelper() {
            Container container = moduleContainer;
            JCDIHelper delegate;

            private JCDIHelper getDelegate() {
                if (delegate == null && container != null) {
                    JCDIHelperFactory jcdiHelperFactory = jcdiHelperFactoryServiceRef.getService();
                    if (jcdiHelperFactory != null) {
                        delegate = jcdiHelperFactory.getJCDIHelper(container);
                        if (delegate == null) {
                            container = null;
                        }
                    }
                }
                return delegate;
            }

            @Override
            public Class<?> getFirstEJBInterceptor(J2EEName j2eeName, Class<?> ejbImpl) {
                JCDIHelper helper = getDelegate();
                return helper != null ? helper.getFirstEJBInterceptor(j2eeName, ejbImpl) : null;
            }

            @Override
            public Class<?> getEJBInterceptor(J2EEName j2eeName, Class<?> ejbImpl) {
                JCDIHelper helper = getDelegate();
                return helper != null ? helper.getEJBInterceptor(j2eeName, ejbImpl) : null;
            }
        };
    }

    public EJBModuleMetaDataImpl createSystemModuleMetaData(ModuleInitData mid) {
        OSGiEJBApplicationMetaData ejbAMD = new OSGiEJBApplicationMetaData(container, mid.ivAppName, null, true, null, false, true);
        return ejbMDOrchestrator.createEJBModuleMetaDataImpl(ejbAMD, mid, null, container);
    }

    @FFDCIgnore(WsRuntimeFwException.class)
    public void start(EJBModuleMetaDataImpl mmd) throws EJBRuntimeException {
        String name = mmd.ivName;
        String appName = mmd.ivAppName;
        EJBApplicationMetaData ejbAMD = mmd.getEJBApplicationMetaData();
        initializeContextClassLoader(mmd);

        try {
            Tr.info(tc, "STARTING_MODULE_CNTR4000I", name, appName);
            ejbAMD.startingModule(mmd, true);
            super.startModule(mmd);
            ejbAMD.startedModule(mmd);
            Tr.info(tc, "STARTED_MODULE_CNTR4001I", name, appName);
        } catch (WsRuntimeFwException t) {
            Tr.error(tc, "ERROR_STARTING_MODULE_CNTR4002E", name, appName, t);
            destroyContextClassLoader(mmd);
            throw new EJBRuntimeException(t);
        }
    }

    @FFDCIgnore(WsRuntimeFwException.class)
    public void startSystemModule(EJBModuleMetaDataImpl mmd) throws EJBRuntimeException {
        EJBApplicationMetaData ejbAMD = mmd.getEJBApplicationMetaData();
        mmd.ivInitData.ivContextClassLoader = mmd.ivInitData.ivClassLoader;
        try {
            ejbAMD.startingModule(mmd, true);
            super.startModule(mmd);
            ejbAMD.startedModule(mmd);
            ejbAMD.started();
        } catch (WsRuntimeFwException t) {
            throw new EJBRuntimeException(t);
        }
    }

    @Trivial
    private OSGiBeanMetaData getOSGiBeanMetaData(BeanMetaData bmd) {
        return (OSGiBeanMetaData) bmd;
    }

    @Trivial
    private OSGiEJBModuleMetaDataImpl getOSGiEJBModuleMetaDataImpl(EJBModuleMetaDataImpl mmd) {
        return (OSGiEJBModuleMetaDataImpl) mmd;
    }

    @Override
    protected ReferenceContext createReferenceContext(BeanMetaData bmd) {
        if (getOSGiEJBModuleMetaDataImpl(bmd._moduleMetaData).isSystemModule()) {
            // Without proper MetaData, injection processing does not work.
            // See EJBMDOrchestratorImpl.processReferenceContext.
            return null;
        }
        return super.createReferenceContext(bmd);
    }

    @Override
    protected void initializeBMD(BeanMetaData bmd) throws Exception {
        if (getOSGiBeanMetaData(bmd).beanRuntime != null) {
            super.initializeBMD(bmd);
        }
    }

    @Override
    protected void addHome(BeanMetaData bmd) throws ContainerException {
        if (getOSGiBeanMetaData(bmd).beanRuntime != null) {
            super.addHome(bmd);
        }
    }

    @Override
    protected EJSHome startBean(BeanMetaData bmd) throws ContainerException {
        if (getOSGiBeanMetaData(bmd).beanRuntime != null) {
            return super.startBean(bmd);
        }
        return null;
    }

    @Override
    protected void stopBean(BeanMetaData bmd) throws CSIException {
        // Only need to stop the bean if it started (bean runtime exists) and
        // EJB Container is still active (internal state cleared on deactivate)
        if (getOSGiBeanMetaData(bmd).beanRuntime != null && ejbRuntimeActive) {
            super.stopBean(bmd);
        }
    }

    @Override
    protected void bindInterfaces(NameSpaceBinder<?> binder, BeanMetaData bmd) throws Exception {
        if (getOSGiBeanMetaData(bmd).beanRuntime != null) {
            super.bindInterfaces(binder, bmd);
        }
    }

    @Override
    @FFDCIgnore({ RuntimeException.class, Exception.class })
    protected <T> T runAsSystem(PrivilegedExceptionAction<T> action) throws PrivilegedActionException {
        try {
            return action.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new PrivilegedActionException(e);
        }
    }

    @Override
    protected EJSHome initializeDeferredEJBImpl(HomeRecord hr) throws ContainerException, EJBConfigurationException {
        try {
            return super.initializeDeferredEJBImpl(hr);
        } catch (Throwable t) {
            BeanMetaData bmd = hr.getBeanMetaData();
            EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;
            Tr.error(tc, "ERROR_STARTING_EJB_CNTR4006E", bmd.enterpriseBeanName, mmd.ivName, mmd.ivAppName, t);

            if (t instanceof ContainerException) {
                throw (ContainerException) t;
            }
            if (t instanceof EJBConfigurationException) {
                throw (EJBConfigurationException) t;
            }
            throw rethrow(t);
        }
    }

    public void stop(EJBModuleMetaDataImpl mmd) {

        String name = mmd.ivName;
        String appName = mmd.getEJBApplicationMetaData().getName();

        if (!ejbRuntimeActive || metaDataService == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "EJBRuntime deactivated, cannot stop module " + name + " in application " + appName);
            return;
        }

        try {
            Tr.info(tc, "STOPPING_MODULE_CNTR4003I", name, appName);
            mmd.getEJBApplicationMetaData().stoppingModule(mmd);
            super.stopModule(mmd);
            Tr.info(tc, "STOPPED_MODULE_CNTR4004I", name, appName);
        } catch (Throwable t) {
            Tr.info(tc, "ERROR_STOPPING_MODULE_CNTR4005E", name, appName, t);
        } finally {
            // Must destroy contextClassLoader obtained during createModuleMetaData (via ModuleInitDataAdapter)
            destroyContextClassLoader(mmd);
            for (BeanMetaData bmd : mmd.ivBeanMetaDatas.values()) {
                if (bmd.ivMetaDataDestroyRequired) {
                    metaDataService.fireComponentMetaDataDestroyed(bmd);
                }
            }
        }
    }

    public void stopSystemModule(EJBModuleMetaDataImpl mmd) {
        try {
            mmd.getEJBApplicationMetaData().stoppingModule(mmd);
            super.stopModule(mmd);
        } catch (Throwable t) {
            // Automatic FFDC only.
        }
    }

    @Override
    public InjectionEngine getInjectionEngine() {
        return injectionEngineSRRef.getService();
    }

    @Reference(name = REFERENCE_INJECTION_ENGINE, service = InjectionEngine.class)
    protected void setInjectionEngine(ServiceReference<InjectionEngine> ref) {
        this.injectionEngineSRRef.setReference(ref);
    }

    protected void unsetInjectionEngine(ServiceReference<InjectionEngine> ref) {
        this.injectionEngineSRRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_MANAGED_OBJECT_SERVICE,
               service = ManagedObjectService.class,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        this.managedObjectServiceRef.setReference(ref);
    }

    protected void unsetManagedObjectService(ServiceReference<ManagedObjectService> ref) {
        this.managedObjectServiceRef.unsetReference(ref);
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    @Reference
    protected void setScheduledExecutorService(ScheduledExecutorService executor) {
        scheduledExecutorService = executor;
    }

    protected void unsetScheduledExecutorService(ScheduledExecutorService executor) {
        scheduledExecutorService = null;
    }

    @Override
    public ScheduledExecutorService getDeferrableScheduledExecutorService() {
        return deferrableScheduledExecutorService;
    }

    @Reference(target = "(deferrable=true)")
    protected void setDeferrableScheduledExecutorService(ScheduledExecutorService executor) {
        deferrableScheduledExecutorService = executor;
    }

    protected void unsetDeferrableScheduledExecutorService(ScheduledExecutorService executor) {
        deferrableScheduledExecutorService = null;
    }

    @Override
    public Object pushServerIdentity() {
        return ThreadIdentityManager.runAsServer();
    }

    @Override
    public void popServerIdentity(Object oldIdentity) {
        ThreadIdentityManager.reset(oldIdentity);
    }

    @Override
    public SfFailoverKey createFailoverKey(BeanId beanId) {
        throw new UnsupportedOperationException("failover not supported");
    }

    @Override
    public HomeRecord createHomeRecord(BeanMetaData bmd, HomeOfHomes homeOfHomes) {
        return new HomeRecordImpl(bmd, homeOfHomes, getOSGiBeanMetaData(bmd).systemHomeBindingName);
    }

    public EJBJavaColonNamingHelper getJavaColonHelper() {
        return this.javaColonHelper;
    }

    @Reference
    protected void setJavaColonHelper(EJBJavaColonNamingHelper jcnh) {
        this.javaColonHelper = jcnh;
    }

    protected void unsetJavaColonHelper(EJBJavaColonNamingHelper jcnh) {
        this.javaColonHelper = jcnh;
    }

    public EJBLocalNamingHelper getEJBLocalNamingHelper() {
        return this.ejbLocalNamingHelper;
    }

    @Reference
    protected void setEJBLocalNamingHelper(EJBLocalNamingHelper ejblocnh) {
        this.ejbLocalNamingHelper = ejblocnh;
    }

    protected void unsetEJBLocalNamingHelper(EJBLocalNamingHelper ejblocnh) {
        this.ejbLocalNamingHelper = ejblocnh;
    }

    public LocalColonEJBNamingHelper getLocalColonEJBNamingHelper() {
        return this.localColonNamingHelper;
    }

    @Reference
    protected void setLocalColonNamingHelper(LocalColonEJBNamingHelper localnh) {
        this.localColonNamingHelper = localnh;
    }

    protected void unsetLocalColonNamingHelper(LocalColonEJBNamingHelper localnh) {
        this.localColonNamingHelper = localnh;
    }

    @Reference(target = "(&" +
                        "(containerToType=com.ibm.ws.javaee.dd.ejb.EJBJar)" +
                        "(containerToType=com.ibm.ws.javaee.dd.ejbext.EJBJarExt)" +
                        "(containerToType=com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd)" +
                        "(containerToType=com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd)" +
                        ")")
    protected void setAdapterFactoryDependency(AdapterFactoryService afs) {
    }

    protected void unsetAdapterFactoryDependency(AdapterFactoryService afs) {
    }

    @Override
    public boolean isRemoteUsingPortableServer() {
        return true;
    }

    @Override
    public void registerServant(ByteArray key, EJSRemoteWrapper remoteObject) {
        // Ignore silently.
    }

    @Override
    public void unregisterServant(EJSRemoteWrapper remoteObject) {
        // Ignore silently.
    }

    @Override
    public Object getRemoteReference(EJSRemoteWrapper remoteObject) {
        waitForEJBRemoteRuntime();
        return ejbRemoteRuntimeServiceRef.getServiceWithException().getReference(remoteObject);
    }

    @Override
    public Object getClusterIdentity(J2EEName j2eeName) {
        return null;
    }

    @Reference(name = REFERENCE_JPA_CONTAINER,
               service = EJBJPAContainer.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setJPAContainer(ServiceReference<EJBJPAContainer> ref) {
        this.jpaContainerServiceRef.setReference(ref);
    }

    protected void unsetJPAContainer(ServiceReference<EJBJPAContainer> ref) {
        this.jpaContainerServiceRef.unsetReference(ref);
    }

    @Override
    public EJBJPAContainer getEJBJPAContainer() {
        return this.jpaContainerServiceRef.getService();
    }

    @Reference(name = REFERENCE_SERIALIZATION_SERVICE, service = SerializationService.class)
    protected void setSerializationService(ServiceReference<SerializationService> ref) {
        this.serializationServiceRef.setReference(ref);
    }

    protected void unsetSerializationService(ServiceReference<SerializationService> ref) {
        this.serializationServiceRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_MDB_RUNTIME,
               service = MDBRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setMDBRuntime(ServiceReference<MDBRuntime> ref) {
        this.mdbRuntimeServiceRef.setReference(ref);
    }

    protected void unsetMDBRuntime(ServiceReference<MDBRuntime> ref) {
        this.mdbRuntimeServiceRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_JCDI_HELPER_FACTORY,
               service = JCDIHelperFactory.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setJCDIHelperFactory(ServiceReference<JCDIHelperFactory> ref) {
        this.jcdiHelperFactoryServiceRef.setReference(ref);
    }

    protected void unsetJCDIHelperFactory(ServiceReference<JCDIHelperFactory> ref) {
        this.jcdiHelperFactoryServiceRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_CLASSLOADING_SERVICE,
               service = ClassLoadingService.class)
    protected void setClassLoadingService(ClassLoadingService ref) {
        classLoadingService = ref;
    }

    protected void unsetClassLoadingService(ClassLoadingService ref) {
        classLoadingService = null;
    }

    @Reference(name = REFERENCE_EJB_ASYNC_RUNTIME,
               service = EJBAsyncRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setEJBAsyncRuntime(ServiceReference<EJBAsyncRuntime> ref) {
        this.ejbAsyncRuntimeServiceRef.setReference(ref);
    }

    protected void unsetEJBAsyncRuntime(ServiceReference<EJBAsyncRuntime> ref) {
        this.ejbAsyncRuntimeServiceRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_EJB_TIMER_RUNTIME,
               service = EJBTimerRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected synchronized void setEJBTimerRuntime(ServiceReference<EJBTimerRuntime> ref) {
        // Setup timers before allowing EJBTimerRuntime to be used.
        if (container != null) {
            container.setupTimers();
        }

        this.ejbTimerRuntimeServiceRef.setReference(ref);
    }

    protected void unsetEJBTimerRuntime(ServiceReference<EJBTimerRuntime> ref) {
        this.ejbTimerRuntimeServiceRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_EJB_PERSISTENT_TIMER_RUNTIME,
               service = EJBPersistentTimerRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected synchronized void setEJBPersistentTimerRuntime(ServiceReference<EJBPersistentTimerRuntime> ref) {
        // Setup timers before allowing EJBPersistentTimerRuntime to be used.
        if (container != null) {
            container.setupTimers();
        }

        this.ejbPersistentTimerRuntimeServiceRef.setReference(ref);
    }

    protected void unsetEJBPersistentTimerRuntime(ServiceReference<EJBPersistentTimerRuntime> ref) {
        this.ejbPersistentTimerRuntimeServiceRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_EJB_HOME_RUNTIME,
               service = EJBHomeRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        this.ejbHomeRuntimeServiceRef.setReference(ref);
    }

    protected void unsetEJBHomeRuntime(ServiceReference<EJBHomeRuntime> ref) {
        this.ejbHomeRuntimeServiceRef.unsetReference(ref);
    }

    @Reference(name = REFERENCE_EJB_REMOTE_RUNTIME,
               service = EJBRemoteRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setEJBRemoteRuntime(ServiceReference<EJBRemoteRuntime> ref) {
        this.ejbRemoteRuntimeServiceRef.setReference(ref);

        CountDownLatch remoteLatch = remoteFeatureLatch;
        if (remoteLatch != null) {
            remoteFeatureLatch = null;
            remoteLatch.countDown();
        }

        // bind any Remote interfaces to COS Naming for beans already started
        bindAllRemoteInterfacesToContextRoot();
    }

    protected void unsetEJBRemoteRuntime(ServiceReference<EJBRemoteRuntime> ref) {
        this.ejbRemoteRuntimeServiceRef.unsetReference(ref);
    }

    @FFDCIgnore(InterruptedException.class)
    private void waitForEJBRemoteRuntime() {
        // If the ejbRemote feature has been configured, but not yet started
        // then wait for up to 30 seconds for it to come up (primarily waiting
        // for the ORB to start.
        CountDownLatch remoteLatch = remoteFeatureLatch;
        if (remoteLatch != null) {
            try {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Waiting 30 seconds for EJBRemoteRuntime");
                if (remoteLatch.await(30, TimeUnit.SECONDS) == false) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "EJBRemoteRuntime did not come up within 30 seconds");
                }
            } catch (InterruptedException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Waiting for EJBRemoteRuntime failed: " + e);
            }
            // Once one thread has completed the wait; no point in others continuing to wait
            remoteFeatureLatch = null;
            remoteLatch.countDown();
        }
    }

    @Reference(name = REFERENCE_EJB_MBEAN_RUNTIME,
               service = EJBMBeanRuntime.class,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC)
    protected void setEJBMBeanRuntime(ServiceReference<EJBMBeanRuntime> ref) {
        this.ejbMBeanRuntimeServiceRef.setReference(ref);
    }

    protected void unsetEJBMBeanRuntime(ServiceReference<EJBMBeanRuntime> ref) {
        this.ejbMBeanRuntimeServiceRef.unsetReference(ref);
    }

    @Override
    @FFDCIgnore(NamingException.class)
    public Object javaColonLookup(String name, EJSHome home) {
        Throwable cause = null;
        if (name.startsWith("java:")) {
            try {
                return new InitialContext().lookup(name);
            } catch (NamingException e) {
                cause = e;
            }
        }

        J2EEName j2eeName = home.getJ2EEName();
        throw new IllegalArgumentException(name + " was not defined for the " + j2eeName.getComponent()
                                           + " component in the " + j2eeName.getModule()
                                           + " module in the " + j2eeName.getApplication()
                                           + " application", cause);
    }

    protected static void createPassivationDirectory(final File passivationDir) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                boolean created = passivationDir.mkdir();
                // If the directory exists already, delete any files that might have
                // failed to be deleted
                if (!created) {
                    File[] existingFiles = passivationDir.listFiles();

                    if (existingFiles == null) {
                        throw new IllegalStateException("failed to retrieve files from  " + passivationDir.getAbsolutePath());
                    }

                    for (File f : existingFiles) {
                        if (!f.delete()) {
                            throw new IllegalStateException("failed to delete " + f.getAbsolutePath());
                        }
                    }
                }
                return null;
            }
        });
    }

    @Override
    protected UserTransaction getUserTransaction() {
        UserTransaction userTran = this.userTransaction;
        if (userTran == null) {
            throw new IllegalStateException("UserTransaction not available");
        }
        return userTran;
    }

    /**
     * Returns the binding context for the currently active extended-scoped
     * persistence context for the thread of execution. Null will be returned
     * if an extended-scoped persistence context is not currently active. <p>
     *
     * @return binding context for currently active extended-scoped
     *         persistence context.
     */
    public Object getExPcBindingContext() {
        return container.getExPcBindingContext();
    }

    public WSEJBEndpointManager createWebServiceEndpointManager(J2EEName name, Class<?> provider, Method[] methods) throws EJBException, EJBConfigurationException {
        return container.createWebServiceEndpointManager(name, provider, methods);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void registerWebServiceHandlerResolver(WSEJBHandlerResolver resolver) {
        webServicesHandlerResolver = resolver;
        this.setWebServiceHandlerResolver(resolver);
    }

    protected void unregisterWebServiceHandlerResolver(WSEJBHandlerResolver resolver) {
        webServicesHandlerResolver = null;
        this.setWebServiceHandlerResolver(null);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected synchronized void setEJBPMICollaboratorFactory(EJBPMICollaboratorFactory factory) {
        ejbPMICollaboratorFactory = factory;
        if (container != null) {
            container.setPMICollaboratorFactory(factory);
        }
    }

    protected synchronized void unsetEJBPMICollaboratorFactory(EJBPMICollaboratorFactory factory) {
        ejbPMICollaboratorFactory = null;
        container.setPMICollaboratorFactory(null);
    }

    @Reference(name = "features", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setLibertyFeature(ServiceReference<LibertyFeature> feature) {
        // If the remote runtime hasn't come up yet, but remote is configured,
        // then create a latch to support a pause in starting remote EJBs.
        if (remoteFeatureLatch == null && ejbRemoteRuntimeServiceRef.getReference() == null) {
            String featureName = (String) feature.getProperty("ibm.featureName");
            if (featureName != null && (featureName.startsWith("enterpriseBeansRemote") || featureName.startsWith("ejbRemote"))) {
                remoteFeatureLatch = new CountDownLatch(1);
            }
        }
    }

    protected void unsetLibertyFeature(ServiceReference<LibertyFeature> feature) {
        // If the remote feature was configured, but never came up, and now
        // is being removed, then also remove the remote latch.
        CountDownLatch remoteLatch = remoteFeatureLatch;
        if (remoteLatch != null) {
            String featureName = (String) feature.getProperty("ibm.featureName");
            if (featureName != null && (featureName.startsWith("enterpriseBeansRemote") || featureName.startsWith("ejbRemote"))) {
                remoteFeatureLatch = null;
                remoteLatch.countDown();
            }
        }
    }

    @Override
    public BeanOFactory getBeanOFactory(BeanOFactoryType type, BeanMetaData bmd) {
        BeanOFactory factory = null;

        if (bmd != null && bmd.isMessageDrivenBean()) {
            MDBRuntime mdbRuntime = getOSGiBeanMetaData(bmd).getMDBRuntime();
            if (mdbRuntime != null) {
                factory = mdbRuntime.getBeanOFactory(type);
            }
        }

        if (factory == null) {
            // Must be one of the core types
            factory = super.getBeanOFactory(type, bmd);
        }

        return factory;
    }

    @Override
    public Class<?> getMessageEndpointFactoryImplClass(BeanMetaData bmd) throws ClassNotFoundException {
        MDBRuntime mdbRuntime = getOSGiBeanMetaData(bmd).getMDBRuntime();
        if (mdbRuntime != null) {
            return mdbRuntime.getMessageEndpointFactoryImplClass(bmd);
        }
        return super.getMessageEndpointFactoryImplClass(bmd);
    }

    @Override
    public Class<?> getMessageEndpointImplClass(BeanMetaData bmd) throws ClassNotFoundException {
        MDBRuntime mdbRuntime = getOSGiBeanMetaData(bmd).getMDBRuntime();
        if (mdbRuntime != null) {
            return mdbRuntime.getMessageEndpointImplClass(bmd);
        }
        return super.getMessageEndpointImplClass(bmd);
    }

    @Override
    public MessageEndpointCollaborator getMessageEndpointCollaborator(BeanMetaData bmd) {
        MDBRuntime mdbRuntime = getOSGiBeanMetaData(bmd).getMDBRuntime();
        if (mdbRuntime != null) {
            return mdbRuntime.getMessageEndpointCollaborator();
        }
        return null;
    }

    @Override
    public void resolveMessageDestinationJndiName(BeanMetaData bmd) {
        // On Liberty, handled in MDBRuntimeImpl.activateEndpoint; nothing to do here
    }

    public Object createAggregateLocalReference(J2EEName beanName, ManagedObjectContext context) throws CreateException, EJBNotFoundException {
        return container.createAggregateLocalReference(beanName, context);
    }

    public boolean removeStatefulBean(@Sensitive Object bean) throws RemoteException, RemoveException {
        return container.removeStatefulBean(bean);
    }

    @Override
    protected void sendBindingMessage(BeanMetaData bmd,
                                      String beanName,
                                      String interfaceName,
                                      int interfaceIndex,
                                      boolean local) {
        if (interfaceIndex == -1 && ejbHomeRuntimeServiceRef.getReference() == null) {
            return;
        }

        if (!local && ejbRemoteRuntimeServiceRef.getReference() == null) {
            return;
        }

        super.sendBindingMessage(bmd, beanName, interfaceName, interfaceIndex, local);
    }

    @Override
    public boolean isRemoteSupported() {
        if (remoteFeatureLatch != null || ejbRemoteRuntimeServiceRef.getReference() != null) {
            return true;
        }
        return false;
    }

    @Override
    public void checkRemoteSupported(EJSHome home, String interfaceName) throws EJBNotFoundException {
        waitForEJBRemoteRuntime();
        if (ejbRemoteRuntimeServiceRef.getReference() == null) {
            J2EEName j2eeName = home.getJ2EEName();
            String appName = j2eeName.getApplication();
            String moduleName = j2eeName.getModule();
            String ejbName = j2eeName.getComponent();

            String msgTxt = Tr.formatMessage(tc, "INJECTION_CANNOT_INSTANTIATE_REMOTE_CNTR4012E",
                                             interfaceName,
                                             ejbName,
                                             moduleName,
                                             appName);

            throw new EJBNotFoundException(msgTxt);
        }
    }

    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        // Nothing.
    }

    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        ApplicationMetaData amd = ((ExtendedApplicationInfo) appInfo).getMetaData();
        EJBApplicationMetaData ejbAMD = (EJBApplicationMetaData) amd.getMetaData(appSlot);
        if (ejbAMD != null) {
            try {
                ejbAMD.started();
            } catch (RuntimeWarning e) {
                throw new StateChangeException(e);
            }
        }
    }

    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        ApplicationMetaData amd = ((ExtendedApplicationInfo) appInfo).getMetaData();
        EJBApplicationMetaData ejbAMD = (EJBApplicationMetaData) amd.getMetaData(appSlot);
        if (ejbAMD != null) {
            ejbAMD.stopping();
        }
    }

    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        // Nothing.
    }

    private void initializeContextClassLoader(EJBModuleMetaDataImpl mmd) {
        // ??? The core container would prefer this be set on ModuleInitData
        // earlier so it can be propagated to BeanMetaData as they are created,
        // but there's no good way for the EJB container to ensure it gets
        // destroyed in error scenarios.  In the future, we'd prefer the
        // management of this be handled by the ModuleInfo or an equivalent
        // service, but for now, create it here and propagate as necessary.
        ClassLoader contextClassLoader = classLoadingService.createThreadContextClassLoader(mmd.ivInitData.ivClassLoader);

        // Update any BeanMetaData that have already been created.
        for (BeanMetaData bmd : mmd.ivBeanMetaDatas.values()) {
            bmd.ivContextClassLoader = contextClassLoader;
        }

        // Set it on ModuleInitData for any BeanMetaData that will be created.
        mmd.ivInitData.ivContextClassLoader = contextClassLoader;

        // Set it on OSGiEJBModuleMetaDataImpl to be destroyed during stop().
        ((OSGiEJBModuleMetaDataImpl) mmd).ivContextClassLoader = contextClassLoader;
    }

    void destroyContextClassLoader(EJBModuleMetaDataImpl mmd) {
        if (mmd instanceof OSGiEJBModuleMetaDataImpl) {
            ClassLoader loader = ((OSGiEJBModuleMetaDataImpl) mmd).ivContextClassLoader;
            if (loader != null && ejbRuntimeActive) {
                classLoadingService.destroyThreadContextClassLoader(loader);
            }
        }
    }

    /**
     * @see com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory#createComponentMetaData(java.lang.String)
     */
    @FFDCIgnore(EJBNotFoundException.class)
    @Override
    public ComponentMetaData createComponentMetaData(String identifier) {
        String[] parts = identifier.split("#");
        J2EEName beanName = j2eeNameFactory.create(parts[1], parts[2], parts[3]); // ignore parts[0] which is the prefix: EJB
        try {
            return container.getInstalledHome(beanName).getBeanMetaData();
        } catch (EJBNotFoundException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "not found", e);
            return null;
        }
    }

    /**
     * @see com.ibm.ws.container.service.metadata.extended.DeferredMetaDataFactory#initialize(com.ibm.ws.runtime.metadata.ComponentMetaData)
     */
    @Override
    public void initialize(ComponentMetaData metadata) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return "EJB#" + j2eeName.toString()
     */
    public static String getMetaDataIdentifierImpl(J2EEName j2eeName) {
        return "EJB#" + j2eeName.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMetaDataIdentifier(String appName, String moduleName, String componentName) {
        return getMetaDataIdentifierImpl(j2eeNameFactory.create(appName, moduleName, componentName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getClassLoader(ComponentMetaData metaData) {
        return (metaData instanceof BeanMetaData) ? ((BeanMetaData) metaData).ivContextClassLoader : null;
    }

    /**
     * Perform any platform specific processing that should be done immediately
     * prior to invoking the MDB method. This may include calling test
     * framework hooks or logging performance metrics (PMI)
     */
    @Override
    public void notifyMessageDelivered(Object proxy) {
        // Nothing to do by default
        // tWAS can do 'if (proxy instanceof MEH) { that logic }'
    }

    /**
     * Method to get the XAResource corresponding to an ActivationSpec from the RRSXAResourceFactory
     *
     * @param bmd The BeanMetaData object for the MDB being handled
     * @param xid Transaction branch qualifier
     * @return the XAResource
     */
    @Override
    public XAResource getRRSXAResource(BeanMetaData bmd, Xid xid) throws XAResourceNotAvailableException {
        MDBRuntime mdbRuntime = getOSGiBeanMetaData(bmd).getMDBRuntime();
        return mdbRuntime.getRRSXAResource(bmd.ivActivationSpecJndiName, xid);
    }
}
