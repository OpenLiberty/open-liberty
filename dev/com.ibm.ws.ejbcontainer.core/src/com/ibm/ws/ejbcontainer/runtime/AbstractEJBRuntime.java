/*******************************************************************************
 * Copyright (c) 1997, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.runtime;

import static com.ibm.ws.metadata.ejb.WCCMMetaDataUtil.validateMergedXML;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Resource;
import javax.ejb.CreateException;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.EntityContext;
import javax.ejb.MessageDrivenContext;
import javax.ejb.ScheduleExpression;
import javax.ejb.SessionContext;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.transaction.UserTransaction;

import com.ibm.ejs.container.BMStatefulBeanOFactory;
import com.ibm.ejs.container.BMStatelessBeanOFactory;
import com.ibm.ejs.container.BeanId;
import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.BeanO;
import com.ibm.ejs.container.BeanOFactory;
import com.ibm.ejs.container.BeanOFactory.BeanOFactoryType;
import com.ibm.ejs.container.CMStatefulBeanOFactory;
import com.ibm.ejs.container.CMStatelessBeanOFactory;
import com.ibm.ejs.container.ContainerConfig;
import com.ibm.ejs.container.ContainerEJBException;
import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.ContainerProperties;
import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.HomeOfHomes;
import com.ibm.ejs.container.HomeRecord;
import com.ibm.ejs.container.ManagedBeanOFactory;
import com.ibm.ejs.container.SingletonBeanOFactory;
import com.ibm.ejs.container.TimerNpImpl;
import com.ibm.ejs.container.WrapperManager;
import com.ibm.ejs.container.activator.ActivationStrategy;
import com.ibm.ejs.container.activator.Activator;
import com.ibm.ejs.container.activator.StatefulActivateOnceActivationStrategy;
import com.ibm.ejs.container.activator.StatefulActivateTranActivationStrategy;
import com.ibm.ejs.container.activator.UncachedActivationStrategy;
import com.ibm.ejs.container.passivator.StatefulPassivator;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.ejs.csi.UOWControl;
import com.ibm.ejs.csi.UOWHandle;
import com.ibm.ejs.util.cache.BackgroundLruEvictionStrategy;
import com.ibm.ejs.util.cache.Cache;
import com.ibm.ejs.util.cache.SweepLruEvictionStrategy;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.ContainerExtensionFactory;
import com.ibm.websphere.csi.EJBContainerException;
import com.ibm.websphere.csi.EJBModuleConfigData;
import com.ibm.websphere.csi.HomeWrapperSet;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.csi.PassivationPolicy;
import com.ibm.websphere.csi.StatefulSessionKeyFactory;
import com.ibm.websphere.ejbcontainer.EJBContextExtension;
import com.ibm.websphere.ejbcontainer.MessageDrivenContextExtension;
import com.ibm.websphere.ejbcontainer.SessionContextExtension;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBPMICollaboratorFactory;
import com.ibm.ws.ejbcontainer.EJBRequestCollaborator;
import com.ibm.ws.ejbcontainer.EJBSecurityCollaborator;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ejbcontainer.injection.factory.EJBContextObjectFactory;
import com.ibm.ws.ejbcontainer.injection.factory.MBLinkReferenceFactoryImpl;
import com.ibm.ws.ejbcontainer.injection.factory.TimerServiceObjectFactory;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;
import com.ibm.ws.ejbcontainer.util.PoolManager;
import com.ibm.ws.ejbcontainer.util.ScheduleExpressionParser;
import com.ibm.ws.ejbcontainer.util.ScheduleExpressionParserException;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.metadata.ejb.AutomaticTimerBean;
import com.ibm.ws.metadata.ejb.BeanInitData;
import com.ibm.ws.metadata.ejb.EJBMDOrchestrator;
import com.ibm.ws.metadata.ejb.ModuleInitData;
import com.ibm.ws.metadata.ejb.TimerMethodData;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.ejbcontainer.WSEJBHandlerResolver;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionEngineAccessor;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionMetaData;
import com.ibm.wsspi.injectionengine.InjectionMetaDataListener;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public abstract class AbstractEJBRuntime implements EJBRuntime, InjectionMetaDataListener {
    private static final String CLASS_NAME = AbstractEJBRuntime.class.getName();
    private static TraceComponent tc = Tr.register(AbstractEJBRuntime.class, "EJBContainer", "com.ibm.ejs.container.container");

    private static final ThreadContextAccessor svThreadContextAccessor = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction()); // F743-12528.1

    protected EJSContainer ivContainer;
    protected boolean ivInitAtStartup;
    private boolean ivInitAtStartupSet;
    private String ivDefaultDataSourceJNDIName;
    private UOWControl ivUOWControl;
    protected SfFailoverCache ivSfFailoverCache;
    protected EJBMDOrchestrator ivEJBMDOrchestrator;

    /**
     * Instance of the currently registered WebService Handler Resolver.
     **/
    // d495644
    private volatile WSEJBHandlerResolver ivWebServicesHandlerResolver = null;

    //
    // Lazily initialized core BeanOFactory instances.                     F88119
    //
    private BeanOFactory ivCMStatelessBeanOFactory;
    private BeanOFactory ivBMStatelessBeanOFactory;
    private BeanOFactory ivCMStatefulBeanOFactory;
    private BeanOFactory ivBMStatefulBeanOFactory;
    private BeanOFactory ivSingletonBeanOFactory;
    private BeanOFactory ivManagedBeanOFactory;

    /**
     * Returns true if the runtime environment is stopping.
     *
     * @return true if the runtime is stopping
     */
    public abstract boolean isStopping();

    /**
     * Creates a new EJBModuleConfigData. This is only needed in runtimes that
     * support entity beans.
     */
    protected abstract EJBModuleConfigData createEJBModuleConfigData(ModuleInitData mid);

    /**
     * Create a namespace binder for the module.
     */
    // F69147.2
    protected abstract NameSpaceBinder<?> createNameSpaceBinder(EJBModuleMetaDataImpl mmd);

    /**
     * Initialize the timer service. This method is called when it is detected
     * that a module might have previously created timers in a persistent
     * database. This method must be synchronized since modules from separate
     * applications might be started in parallel. If the runtime environment
     * does not support timers, this method should do nothing.
     *
     * @param checkDatabase only initialize the timer service if is known that
     *                          persistent timers exist in the database
     */
    protected abstract void initializeTimerService(boolean checkDatabase) throws EJBContainerException, ContainerException;

    /**
     * Register both the Module and Component Mbeans. This method is called at
     * application start time regardless of whether the EJB initialization is
     * deferred or not. If the runtime environment does not support MBeans,
     * this method should do nothing.
     *
     * the module
     */
    //198685
    protected abstract void registerMBeans(ModuleInitData mid, EJBModuleMetaDataImpl mmd);

    /**
     * Deregister MBeans that were registered by {@link registerMBeans}. If the
     * runtime environment does not support MBeans, this method should do
     * nothing.
     *
     * @param mmd the module
     */
    protected abstract void deregisterMBeans(EJBModuleMetaDataImpl mmd); //d458588

    /**
     * Start the MDBs of a module.
     */
    protected abstract void startMDBs(ModuleInitData mid, EJBModuleMetaDataImpl mmd) throws RuntimeWarning;

    /**
     * Returns true if reference processing needs to occur for the specified
     * bean during module start rather than being deferred until first touch.
     * In support of java:global, this method should conservatively return true
     * unless the runtime can determine that reference processing does not need
     * to be done early.
     *
     * @param bmd the bean
     * @return true if reference processing should not be deferred
     * @throws RuntimeWarning
     */
    protected abstract boolean isReferenceProcessingNeededAtStart(BeanMetaData bmd) // F743-29417
                    throws RuntimeWarning;

    protected abstract void fireMetaDataCreated(EJBModuleMetaDataImpl mmd) throws RuntimeWarning;

    protected abstract void fireMetaDataCreated(BeanMetaData bmd) throws RuntimeWarning;

    protected abstract void fireMetaDataDestroyed(BeanMetaData bmd) throws RuntimeWarning;

    /**
     * Returns the runtime environment specific UserTransaction implementation.
     */
    // F84120
    protected abstract UserTransaction getUserTransaction();

    protected AbstractEJBRuntime() {
        // no common constructor initialization
    }

    /**
     * Starts the container runtime.
     *
     * @param config the configuration
     * @throws CSIException if the container fails to start
     */
    public void start(EJBRuntimeConfig config) throws CSIException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "start");

        ivContainer = config.getContainer();
        ivEJBMDOrchestrator = config.getEJBMDOrchestrator();

        // Check system property to determine if the deferred EJB initialization option has been disabled
        // The initializeEJBsAtStartup system property has 3 possible values:
        // (True) - all EJBs will be initialized during application start.
        // (False) - all EJBs (except MDBs and Startupbeans) will have their initialization deferred.
        // (Not Set) - The property is not set and therefore deferred initialization will be determined
        //             on a bean-by-bean basis.
        // Moved from processBean via defect 294477
        String ias = ContainerProperties.InitializeEJBsAtStartup; // 391302
        if (ias != null) {
            ivInitAtStartupSet = true;
            ivInitAtStartup = ias.equalsIgnoreCase("true");
        }

        //PK15508: make uowCrtl a global variable.  Change from: UOWControl ivUOWControl = cef.getUOWControl(tx); to:
        ContainerExtensionFactory cef = config.getContainerExtensionFactory();
        ivUOWControl = cef.getUOWControl(getUserTransaction()); // LIDB4171-35.03 F84120
        EJBSecurityCollaborator<?> securityCollaborator = config.getSecurityCollaborator();

        StatefulPassivator statefulPassivator = config.getStatefulPassivator();
        EJBPMICollaboratorFactory pmiFactory = config.getPmiBeanFactory();
        PassivationPolicy passivationpolicy = config.getPassivationPolicy();
        ivSfFailoverCache = config.getSfFailoverCache();

        ivDefaultDataSourceJNDIName = config.getDefaultDataSourceJNDIName();

        long cacheSize = config.getCacheSize();
        long cacheSweepInterval = config.getCacheSweepInterval();

        ScheduledExecutorService ivScheduledExecutorService = this.getScheduledExecutorService(); // F73234
        ScheduledExecutorService deferrableScheduledExecutorService = this.getDeferrableScheduledExecutorService();

        PoolManager poolMgr = PoolManager.newInstance();
        poolMgr.setDrainInterval(config.getInactivePoolCleanupInterval());
        poolMgr.setScheduledExecutorService(ivScheduledExecutorService);

        Cache ejbCache = new Cache("EJB Cache", cacheSize, false);
        BackgroundLruEvictionStrategy evictor = new BackgroundLruEvictionStrategy(ejbCache, (int) cacheSize, cacheSweepInterval, ivScheduledExecutorService, deferrableScheduledExecutorService); // F73234
        ejbCache.setEvictionStrategy(evictor);
        evictor.start();

        Cache wrapperCache = new Cache("Wrapper Cache", 2 * cacheSize, true);
        SweepLruEvictionStrategy wrapperEvictor = new SweepLruEvictionStrategy(wrapperCache, (int) (2
                                                                                                    * cacheSize), 3
                                                                                                                  * cacheSweepInterval, ivScheduledExecutorService, deferrableScheduledExecutorService); // d118138, F73234
        wrapperCache.setEvictionStrategy(wrapperEvictor);
        wrapperEvictor.start();

        WrapperManager wrapperManager = config.getWrapperManager();
        wrapperManager.initialize(wrapperCache);

        EJBRequestCollaborator<?>[] afterActivationCollaborators = config.getAfterActivationCollaborators();
        EJBRequestCollaborator<?>[] beforeActivationCollaborators = config.getBeforeActivationCollaborators();
        EJBRequestCollaborator<?> beforeActivationAfterCompletionCollaborators[] = config.getBeforeActivationAfterCompletionCollaborators();

        J2EENameFactory j2eeNameFactory = config.getJ2EENameFactory();
        StatefulSessionKeyFactory sessionKeyFactory = config.getStatefulSessionKeyFactory();
        ivWebServicesHandlerResolver = config.getWSEJBHandlerResolver();

        // -------------------------------------------------------------------------
        // Initialize EJSContainer with configuration values...
        // -------------------------------------------------------------------------
        ContainerConfig containerConfig = new ContainerConfig(this, null, config.getName(), ejbCache, wrapperManager, passivationpolicy, // LIDB2775-23.4
                        config.getPersisterFactory(), config.getEntityHelper(), pmiFactory, securityCollaborator, statefulPassivator, sessionKeyFactory, // LIDB2775-23.7
                        config.getStatefulSessionHandleFactory(), // F743-13024
                        poolMgr, j2eeNameFactory, config.getObjectCopier(), // RTC102299
                        config.getOrbUtils(), // F743-13024
                        ivUOWControl, afterActivationCollaborators, beforeActivationCollaborators, beforeActivationAfterCompletionCollaborators, cef, // 125942
                        config.getStatefulBeanEnqDeq(), // d646413.2
                        config.getDispatchEventListenerManager(), // d646413.2
                        ivSfFailoverCache, // LIDB2018-1
                        config.isSFSBFailoverEnabled()); // LIDB2018-1

        ivContainer.initialize(containerConfig);

        InjectionEngine injectionEngine = getInjectionEngine();

        try {
            // Register ObjectFactory for EJBContext and subinterfaces.      F48603
            injectionEngine.registerObjectFactory(Resource.class, EntityContext.class, EJBContextObjectFactory.class, false, null, false);
            injectionEngine.registerObjectFactory(Resource.class, EJBContext.class, EJBContextObjectFactory.class, false, null, false);
            injectionEngine.registerObjectFactory(Resource.class, EJBContextExtension.class, EJBContextObjectFactory.class, false, null, false);
            injectionEngine.registerObjectFactory(Resource.class, SessionContext.class, EJBContextObjectFactory.class, false, null, false);
            injectionEngine.registerObjectFactory(Resource.class, SessionContextExtension.class, EJBContextObjectFactory.class, false, null, false);
            injectionEngine.registerObjectFactory(Resource.class, MessageDrivenContext.class, EJBContextObjectFactory.class, false, null, false);
            injectionEngine.registerObjectFactory(Resource.class, MessageDrivenContextExtension.class, EJBContextObjectFactory.class, false, null, false);

            // Register ObjectFactory to handle TimerService refs.           F48603
            injectionEngine.registerObjectFactory(Resource.class, TimerService.class, TimerServiceObjectFactory.class, false, null, false);
        } catch (InjectionException ex) {
            throw new CSIException("Failed to register injection engine object factories", ex);
        }

        // Register to bind java:comp/EJBContext, etc.                      F48603
        injectionEngine.registerInjectionMetaDataListener(this);

        // Override default managed bean auto link reference factory.    d698540.1
        injectionEngine.registerManagedBeanReferenceFactory(new MBLinkReferenceFactoryImpl());

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "start");
    }

    /**
     * Stops the container runtime.
     */
    public void stop() // F743-15582
    {
        if (ivContainer != null) {
            ivContainer.terminate();
        }
    }

    /**
     * Updates the start EJBs at application start (initAtStartup) settings. <p>
     *
     * @param startEjbsAtAppStart true indicates beans should start at application start;
     *                                false indicates beans should start at first use;
     *                                null indicates the setting from ibm-ejb-jar-ext.xml should be used.
     */
    protected void updateStartEjbsAtAppStart(Boolean startEjbsAtAppStart) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "updateStartEjbsAtAppStart : "
                         + (ContainerProperties.InitializeEJBsAtStartup == null ? "Update : " : "Ignore : ")
                         + startEjbsAtAppStart);

        if (ContainerProperties.InitializeEJBsAtStartup == null) {
            if (startEjbsAtAppStart != null) {
                ivInitAtStartupSet = true;
                ivInitAtStartup = startEjbsAtAppStart;
            } else {
                ivInitAtStartupSet = false;
                ivInitAtStartup = false;
            }
        }
    }

    /**
     * Start a new EJB Module
     *
     * Warning: This method will be called by multiple threads that are
     * starting separate applications. WebSphere runtime will still prevent
     * multiple modules within the same EAR from starting simultaneously. <p>
     */
    public void startModule(EJBModuleMetaDataImpl mmd) throws RuntimeError {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "startModule", mmd.getJ2EEName());

        String jarName = null; //294477
        String appJarName = null;
        RuntimeError error = null;
        Object origCL = ThreadContextAccessor.UNCHANGED;
        NameSpaceBinder<?> binder = null;
        boolean postInvokeNeeded = false;
        try {
            // Fire the ModuleMetaData event to the listeners.
            // TODO - why is this here rather than createMetaData?
            if (isTraceOn && tc.isDebugEnabled()) //203449
                Tr.debug(tc, "Fire Module Metadata created event to listeners for module: " +
                             mmd.getJ2EEName());
            mmd.ivMetaDataDestroyRequired = true; //d505055
            fireMetaDataCreated(mmd);

            ModuleInitData mid = mmd.ivInitData; // F743-36113
            mmd.ivInitData = null;

            jarName = mmd.getName(); //294477

            EJBModuleConfigData moduleConfig = createEJBModuleConfigData(mid);
            preInvokeStartModule(mmd, moduleConfig);

            // 621157 - Past this point, ensure we notify the container and
            // collaborators that the module has started, even if an exception
            // occurs.  Otherwise, collaborators get confused when we notify
            // them that a module is stopping without first notifying them that
            // the module has started.
            postInvokeNeeded = true;

            // For EJB 2.1 and earlier modules, the Scheduler is only
            // 'started' if EJBDeploy flagged the module as containing
            // Timer objects. For EJB 3.0 and later modules, there is no
            // EJBDeploy, so the Scheduler is started if the customer has
            // configured to use a Timer database other than the default,
            // or if the default database exists (i.e. it might have timers).
            // Otherwise, for EJB 3.0 and later, the Scheduler will be
            // created and started on first use by a Timer bean.      d438133
            if (mid.ivHasTimers == null) {
                initializeTimerService(true);
            } else if (mid.ivHasTimers) {
                initializeTimerService(false);
            }

            // Create the module namespace binder.                         F69147.2
            binder = createNameSpaceBinder(mmd);
            binder.beginBind();

            boolean hasEJB = false;
            for (BeanInitData bid : mid.ivBeans) {
                // Create the BeanMetaData if it hasn't already been created.
                BeanMetaData bmd = mmd.ivBeanMetaDatas.get(bid.ivName);
                if (bmd == null) {
                    bmd = createBeanMetaData(bid, mmd);
                }

                hasEJB |= bmd.type != InternalConstants.TYPE_MANAGED_BEAN;

                // F743-4950 - If this EJB is a Singleton Session bean, then
                // add it to the application metadata to finish its processing
                // when the application finishes its startup processing.
                if (bmd.isSingletonSessionBean()) {
                    mmd.getEJBApplicationMetaData().addSingleton(bmd, bid.ivStartup, bid.ivDependsOn);
                }
            }

            if (!hasEJB && !mmd.ivManagedBeansOnly) // F743-36113
            {
                // Error - EJB modules must have at least one bean configured.  Stop application from starting.
                Tr.error(tc, "NO_BEANS_IN_MODULE_CNTR9269W", jarName);
                throw new EJBConfigurationException("The " + jarName +
                                                    " Enterprise JavaBeans (EJB) module does not have any enterprise beans configured.");
            }

            validateMergedXML(mid); // d680497.1

            ivEJBMDOrchestrator.processEJBJarBindings(mid, mmd); // F743-36290.1

            Collection<BeanMetaData> bmds = mmd.ivBeanMetaDatas.values();

            // d664917.2 - Process all BeanMetaData.  Note that metadata
            // processing must be done using the runtime class loader.
            for (BeanMetaData bmd : bmds) {
                if (isTraceOn && tc.isDebugEnabled()) {
                    bmd.dump();
                }
                validateMergedXML(bmd); // d680497

                // Notify PM that the bean has been installed.          // RTC112791
                beanInstall(bmd);

                if (!bmd.fullyInitialized) // F91481
                {
                    initializeBMD(bmd);
                }
            }

            // Switch over from the runtime classloader to the application classloader so that
            // the classes loaded during install will be accessible during the execution of the application.
            origCL = svThreadContextAccessor.pushContextClassLoader(mid.getContextClassLoader()); // F85059

            // Start all EJBs unless they're deferred.
            for (BeanMetaData bmd : bmds) {
                if (!bmd.ivDeferEJBInitialization) {
                    fireMetaDataCreatedAndStartBean(bmd); // d648522, d739043
                }
            }

            // All EJBs have either been started, or their metadata has been
            // sufficiently processed to allow deferred initialization, so make
            // the EJB visible.
            for (BeanMetaData bmd : bmds) {
                // Add the EJB to HomeOfHomes (remote, serialized refs, etc.).
                addHome(bmd);

                // Bind non-MDB into JNDI.
                if (bmd.type != InternalConstants.TYPE_MESSAGE_DRIVEN) {
                    try {
                        bindInterfaces(binder, bmd);
                    } catch (Exception e) {
                        FFDCFilter.processException(e, CLASS_NAME + ".install", "950", this);
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "startModule: exception", e);
                        error = new RuntimeError(e); // 119723 set warning to last exception
                    }
                }
            }

            // d608829.1 - If a bean failed to start, then there's no need to
            // finish starting the module.
            if (error == null) {
                // If any Remote bindings were made (component or business) then an
                // EJBFactory also needs to be bound to support ejb-link / auto-link
                // from the client.                                             d440604
                binder.bindEJBFactory(); // F69147.2

                startMDBs(mid, mmd);

                // d604213 - Modules can be started after the application is running,
                // so we must create automatic timers at module start and not at
                // application start.
                if (mmd.ivAutomaticTimerBeans != null) {
                    // The EJB TimerService does not run in the z/OS CRA.
                    if (!EJSPlatformHelper.isZOSCRA()) {
                        int numPersistentCreated = 0;
                        int numNonPersistentCreated = 0;
                        if (mmd.ivHasNonPersistentAutomaticTimers)
                            numNonPersistentCreated = createNonPersistentAutomaticTimers(mmd.ivJ2EEName.getApplication(), mmd.getName(), mmd.ivAutomaticTimerBeans);
                        if (mmd.ivHasPersistentAutomaticTimers)
                            numPersistentCreated = createPersistentAutomaticTimers(mmd.ivJ2EEName.getApplication(), mmd.getName(), mmd.ivAutomaticTimerBeans);

                        Tr.info(tc, "AUTOMATIC_TIMER_CREATION_CNTR0219I",
                                new Object[] { numPersistentCreated, numNonPersistentCreated, mmd.getName() });
                    }

                    mmd.ivAutomaticTimerBeans = null;
                }

                if (!mmd.ivManagedBeansOnly) {
                    registerMBeans(mid, mmd); //198685
                }
            }

            ivEJBMDOrchestrator.processGeneralizations(moduleConfig, mmd); // F743-21131

            postInvokeNeeded = false; // d621157
            postInvokeStartModule(mmd, appJarName); // d621157

            mid.unload();
        }
        //d607801: removed catch block
        catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + ".install", "982", this);
            error = new RuntimeError(t); // 119723
        } finally {
            // The following code was reordered so that the context classloader       @MD20022A
            // is reset after the call to executeBatchedOperation.  On 390,           @MD20022A
            // executeBatchedOperation drives a remote operation to the control       @MD20022A
            // region.  It returns IORs for the bound homes.  For each IOR,           @MD20022A
            // the base JDK ORB calls lookupLocalObject (which 390 overrides)         @MD20022A
            // to determine if the object is local and if so to obtain a stub         @MD20022A
            // for it.  SOV defect 68226 validates that the stub returned by          @MD20022A
            // lookupLocalObject is compatible with the active classloaders.          @MD20022A
            // If the context classloader has been reset, this validation fails       @MD20022A
            // and the JDK ORB tries to load a new compatible stub.  This won't       @MD20022A
            // be able to find the stub class from the EJB application either         @MD20022A
            // and it will end up creating and caching an instance of                 @MD20022A
            // org.omg.stub.javax.ejb._EJBHome_Stub.  In order to avoid this,         @MD20022A
            // the context classloader is reset after executeBatchedOperation.        @MD20022A

            if (postInvokeNeeded) { // d621157
                try {
                    postInvokeStartModule(mmd, appJarName);
                } catch (Throwable t) {
                    FFDCFilter.processException(t, CLASS_NAME + ".startModule", "761", this);
                }
            }

            if (binder != null) {
                try {
                    binder.end(); // F69147.2
                } catch (Throwable t) {
                    if (error == null) {
                        error = new RuntimeError(t);
                    }
                }
            }

            svThreadContextAccessor.popContextClassLoader(origCL); // F85059

            if (error != null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "startModule: " + error);

                try {
                    mmd.getEJBApplicationMetaData().stoppingModule(mmd); // F743-26072
                    uninstall(mmd, true); // d127220 //d130898
                } catch (Throwable t) {
                    FFDCFilter.processException(t, CLASS_NAME + ".startModule", "980", this);
                }

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "startModule: " + error);
                throw error; // 118362
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "startModule");
        }
    }

    protected void preInvokeStartModule(EJBModuleMetaDataImpl mmd, EJBModuleConfigData moduleConfig) {
        // This method is overridden in WASEJBRuntimeImpl to allow for Persistence
        // Manager logic to be moved out of shared logic and into specific
        // traditional WAS logic.
    }

    protected void postInvokeStartModule(EJBModuleMetaDataImpl mmd, String appJarName) // d621157
    {
        // This method is overridden in WASEJBRuntimeImpl to allow for Persistence
        // Manager logic to be moved out of shared logic and into specific
        // traditional WAS logic.
    }

    protected void beanInstall(BeanMetaData bmd) // RTC112791
                    throws RemoteException {
        // This method is overridden in WASEJBRuntimeImpl to allow for Persistence
        // Manager logic to be moved out of shared logic and into specific
        // traditional WAS logic.
    }

    /**
     * Stops an EJB module.
     *
     * @param mmd the module metadata
     */
    public void stopModule(EJBModuleMetaDataImpl mmd) {
        try { //210058
            uninstall(mmd, false);
        } catch (Throwable t) { //210058
            FFDCFilter.processException(t, CLASS_NAME + ".stop", "3059", this);
            throw new ContainerEJBException("Failed to stop - caught Throwable", t);
        }
    }

    /**
     * Uninstall an EJB module. {@link EJBApplicationMetaData#stoppingModule} must be called prior to calling this method.
     *
     * @param dobj      DeployedModule representing the module to be uninstalled.
     *
     * @param doe       DeployedObjectEvent for the module to be uninstalled.
     *
     * @param unbindNow boolean which is "true" if this uninstall is being called
     *                      as a result of an error that occurred during installation.
     *
     * @exception CSIException is thrown if an unexpected exception
     *                             is caught.
     */
    private void uninstall(EJBModuleMetaDataImpl mmd, boolean unbindNow) throws RuntimeWarning //d210058
    {//d130898
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "uninstall", mmd.getJ2EEName());

        RuntimeWarning warning = null;
        EJBApplicationMetaData ejbAMD = mmd.getEJBApplicationMetaData();
        String appName = mmd.ivAppName; //497716.5.1

        NameSpaceBinder<?> binder = createNameSpaceBinder(mmd); // F69147.2

        try {
            binder.beginUnbind(unbindNow); // F69147.2

            try {
                deregisterMBeans(mmd);
            } catch (Throwable t) { // 210058
                FFDCFilter.processException(t, CLASS_NAME + ".uninstall", "2623", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception caught during uninstall (deregisterMBeans).  Uninstall continues:  ", t);
                if (warning == null) {
                    warning = new RuntimeWarning(t); // 210058 set warning to last exception
                }
            }

            for (BeanMetaData bmd : mmd.ivBeanMetaDatas.values()) {
                J2EEName j2eeName = bmd.j2eeName;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "stopping bean " + j2eeName);

                try {
                    stopBean(bmd); // F743-26072
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Bean stopped; no exceptions thrown");
                    }
                } catch (Throwable e) {
                    FFDCFilter.processException(e, CLASS_NAME + ".uninstall", "1039", this);
                    Throwable nestedExc = getNestedException(e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Exception:", e);
                    if (warning == null) {
                        warning = new RuntimeWarning(nestedExc); // 210058 set warning to last exception
                    }
                }

                HomeRecord hr = bmd.homeRecord;
                if (hr.ivJavaGlobalBindings != null) {
                    try {
                        binder.unbindJavaGlobal(hr.ivJavaGlobalBindings); // F743-26137, F69147.2
                    } catch (NamingException ex) {
                        FFDCFilter.processException(ex, CLASS_NAME + ".uninstall", "1006", this);
                        if (warning == null) {
                            warning = new RuntimeWarning(ex);
                        }
                    }
                }

                // Unbind from java:global.  If the app is stopping anyway,
                // then don't bother since the namespace will be destroyed.
                if (hr.ivJavaAppBindings != null && !ejbAMD.isStopping()) {
                    try {
                        binder.unbindJavaApp(hr.ivJavaAppBindings); // F743-26137, F69147.2
                    } catch (NamingException ex) {
                        FFDCFilter.processException(ex, CLASS_NAME + ".uninstall", "1066", this);
                        if (warning == null) {
                            warning = new RuntimeWarning(ex);
                        }
                    }
                }

                // No need to unbind from java:module since that namespace
                // will be destroyed.

                try {
                    binder.unbindBindings(hr); // F69147.2
                } catch (NamingException ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".uninstall", "2613", this);
                    if (warning == null) {
                        warning = new RuntimeWarning(ex);
                    }
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Bean uninstall sequence ending for: ", j2eeName);
                }
            }

            // d465081 start // d510405
            // Remove the module map from the application map in the full set of MessageDestinationLinks
            Map<String, Map<String, String>> appMap = InjectionEngineAccessor.getMessageDestinationLinkInstance().getMessageDestinationLinks().get(appName); //d49167
            if (appMap != null) { //d475701
                String moduleName = mmd.getJ2EEName().getModule();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Remove module from appMap of MessageDestinationLinks : " + mmd.getJ2EEName());
                appMap.remove(moduleName); // d462512

                if (appMap.size() == 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Remove appMap from MessageDestinationLinks : " + appName);
                    InjectionEngineAccessor.getMessageDestinationLinkInstance().getMessageDestinationLinks().remove(appName); //d493167
                } //d465081 end
            }

            // Unbind any EJBFactories that may have been bound during start.
            // There may be one per app, and one per module.                d440604
            try {
                binder.unbindEJBFactory(); // F69147.2
            } catch (NamingException ex) {
                if (warning == null) {
                    warning = new RuntimeWarning(ex);
                }
            }

            try {
                postInvokeStopModule(mmd);
            } catch (Throwable t) { // 210058
                FFDCFilter.processException(t, CLASS_NAME + ".uninstall", "2730", this);
                Throwable t2 = getNestedException(t);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Exception caught during uninstall (moduleUninstalPostInvoke).  Uninstall continues:  ", t2);
                if (warning == null) {
                    warning = new RuntimeWarning(t2); // 210058 set warning to last exception
                }
            }

        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + ".uninstall", "1069", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Exception:", t);
            warning = new RuntimeWarning(t); // 210058 set warning to last exception

        } finally {
            try {
                binder.end(); // F69147.2
            } catch (NamingException ex) {
                if (warning == null) {
                    warning = new RuntimeWarning(ex);
                }
            }
        }

        if (warning != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "uninstall: " + warning);
            throw warning;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "uninstall");
    }

    protected void postInvokeStopModule(EJBModuleMetaDataImpl mmd) {
        // This method is overridden in WASEJBRuntimeImpl to allow for Persistence
        // Manager logic to be moved out of shared logic and into specific
        // traditional WAS logic.
    }

    /**
     * Bind all local and remote interfaces for a bean to all binding locations.
     *
     * @param binder  the namespace binder
     * @param bmd     the bean
     * @param homeSet the remote and local home wrappers, or <tt>null</tt> if
     *                    deferred initialization bindings should be used
     */
    protected void bindInterfaces(NameSpaceBinder<?> binder, BeanMetaData bmd) throws Exception {
        HomeWrapperSet homeSet = null;
        EJSHome home = bmd.homeRecord.getHome();
        if (home != null) {
            homeSet = home.getWrapperSet();
        }

        int numRemoteInterfaces = countInterfaces(bmd, false);
        int numLocalInterfaces = countInterfaces(bmd, true);
        boolean singleGlobalInterface = (numRemoteInterfaces + numLocalInterfaces) == 1;

        bindInterfaces(binder, bmd, homeSet, false, numRemoteInterfaces, singleGlobalInterface);
        bindInterfaces(binder, bmd, homeSet, true, numLocalInterfaces, singleGlobalInterface);
    }

    /**
     * Determine the number of remote or local interfaces exposed by a bean.
     *
     * @param bmd   the bean
     * @param local <tt>true</tt> if local interfaces should be counted, or
     *                  <tt>false</tt> if remote interfaces should be counted
     * @return the number of remote or local interfaces
     */
    private int countInterfaces(BeanMetaData bmd, boolean local) // F743-23167
    {
        // Note that these variables must be kept in sync with bindInterfaces.
        String homeInterfaceClassName = local ? bmd.localHomeInterfaceClassName : bmd.homeInterfaceClassName;
        boolean hasLocalBean = local && bmd.ivLocalBean;
        String[] businessInterfaceNames = local ? bmd.ivBusinessLocalInterfaceClassNames : bmd.ivBusinessRemoteInterfaceClassNames;

        int result = (homeInterfaceClassName == null ? 0 : 1) +
                     (hasLocalBean ? 1 : 0) +
                     (businessInterfaceNames == null ? 0 : businessInterfaceNames.length);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "countInterfaces: " + bmd.j2eeName + ", local=" + local + ", result=" + result);
        return result;
    }

    /**
     * Bind all interfaces for a bean to all binding locations.
     *
     * @param binder                the namespace binder
     * @param bmd                   the bean
     * @param homeSet               the remote and local home wrappers, or <tt>null</tt> if
     *                                  deferred initialization bindings should be used
     * @param local                 <tt>true</tt> if local interfaces should be bound, or
     *                                  <tt>false</tt> if remote interfaces should be bound
     * @param numInterfaces         the number of local or remote interfaces; the result
     *                                  of passing bmd and local to {@link #countInterfaces}
     * @param singleGlobalInterface <tt>true</tt> if this bean has only one
     *                                  total interface (counting local and remote together)
     */
    private void bindInterfaces(NameSpaceBinder<?> binder,
                                BeanMetaData bmd,
                                HomeWrapperSet homeSet,
                                boolean local,
                                int numInterfaces,
                                boolean singleGlobalInterface) // F743-23167
                    throws NamingException, RemoteException, CreateException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "bindInterfaces: " + bmd.j2eeName +
                         ", deferred=" + (homeSet == null) +
                         ", local=" + local);

        // Note that these variables must be kept in sync with countInterfaces.
        String homeInterfaceClassName = local ? bmd.localHomeInterfaceClassName : bmd.homeInterfaceClassName;
        boolean hasLocalBean = local && bmd.ivLocalBean;
        String[] businessInterfaceNames = local ? bmd.ivBusinessLocalInterfaceClassNames : bmd.ivBusinessRemoteInterfaceClassNames;

        // If we have multiple interfaces (e.g., a home and one business
        // interface, or multiple business interfaces), and simple binding name
        // was specified, then issue a warning message that simple binding name
        // is misused.
        if (numInterfaces > 1 && bmd.simpleJndiBindingName != null) {
            Tr.warning(tc, "SIMPLE_BINDING_NAME_MISSUSED_CNTR0168W",
                       new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName, bmd._moduleMetaData.ivAppName });
        }

        HomeRecord hr = bmd.homeRecord;

        // Bind the home interface, if any.
        if (homeInterfaceClassName != null) {
            bindInterface(binder, hr, homeSet, numInterfaces, singleGlobalInterface,
                          homeInterfaceClassName, -1, local);
        }

        // Bind the no-interface view, if any.
        int interfaceIndex = 0;
        if (hasLocalBean) {
            bindInterface(binder, hr, homeSet, numInterfaces, singleGlobalInterface,
                          bmd.enterpriseBeanClassName, interfaceIndex, local);
            interfaceIndex++;
        }

        // Bind business interfaces, if any.
        if (businessInterfaceNames != null) {
            for (String businessInterfaceName : businessInterfaceNames) {
                bindInterface(binder, hr, homeSet, numInterfaces, singleGlobalInterface,
                              businessInterfaceName, interfaceIndex, local);
                interfaceIndex++;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "bindInterfaces");
    }

    /**
     * Bind a single interface to all binding locations.
     *
     * @param binder  the namespace binder
     * @param hr      the bean home record
     * @param homeSet the remote and local home wrappers, or <tt>null</tt> if
     *                    deferred initialization bindings should be used
     * @pram numInterfaces the number of remote or local interfaces
     * @param singleGlobalInterface <tt>true</tt> if this bean has only one
     *                                  total interface (counting local and remote together)
     * @param interfaceName         the interface name to bind
     * @param interfaceIndex        the interface index, or -1 for a home interface
     * @param local                 <tt>true</tt> if the interface to bind is a local interface
     * @param isHome                <tt>true</tt> if the interface is a home interface
     */
    private <T> void bindInterface(NameSpaceBinder<T> binder,
                                   HomeRecord hr,
                                   HomeWrapperSet homeSet,
                                   int numInterfaces,
                                   boolean singleGlobalInterface,
                                   String interfaceName,
                                   int interfaceIndex,
                                   boolean local) throws NamingException, RemoteException, CreateException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "bindInterface: " + hr.getJ2EEName() +
                         ", " + interfaceIndex +
                         ", " + interfaceName +
                         ", local=" + local);

        // Create the object to be bound.
        T bindingObject = binder.createBindingObject(hr, homeSet, interfaceName, interfaceIndex, local);

        // Bind the object to configured locations.
        boolean deferred = homeSet == null;
        if (hr.bindToContextRoot()) // F743-34301
        {
            binder.bindBindings(bindingObject, // F69147.2
                                hr, numInterfaces, singleGlobalInterface,
                                interfaceIndex, interfaceName, local, deferred);
        }

        // Bind session and managed bean objects to java:global/app/module.
        if (hr.bindToJavaNameSpaces()) // F743-34301, d660700
        {
            T javaBindingObject = binder.createJavaBindingObject(hr, homeSet, interfaceName, interfaceIndex, local, bindingObject); // F69147.2
            bindObjectToJavaNameSpaces(binder, javaBindingObject, hr, singleGlobalInterface, interfaceName, interfaceIndex, local);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "bindInterface");
    }

    /**
     * Bind the remote interfaces for all beans known to the container.
     *
     * Intended to be used to restore EJB bindings when the ORB is restarted.
     */
    protected void bindAllRemoteInterfacesToContextRoot() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "bindAllRemoteInterfacesToContextRoot");

        Map<EJBModuleMetaDataImpl, NameSpaceBinder<?>> binders = new HashMap<EJBModuleMetaDataImpl, NameSpaceBinder<?>>();

        // Start with the list of all HomeRecords. Not all of these beans may have
        // started, but this is the complete set of everything that would have been
        // bound into some naming context. The container may not be available yet
        // if the EJB feature is just starting, but then there is nothing to do.
        HomeOfHomes homeOfHomes = (ivContainer != null) ? ivContainer.getHomeOfHomes() : null;
        if (homeOfHomes != null) {
            List<HomeRecord> hrs = ivContainer.getHomeOfHomes().getAllHomeRecords();

            for (HomeRecord hr : hrs) {
                if (hr.bindToContextRoot()) {
                    BeanMetaData bmd = hr.getBeanMetaData();

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "processing bindings for " + bmd.j2eeName);

                    if (bmd.homeInterfaceClassName != null) {
                        bindRemoteInterfaceToContextRoot(binders, hr, bmd.homeInterfaceClassName, -1);
                    }

                    if (bmd.ivBusinessRemoteInterfaceClassNames != null) {
                        int interfaceIndex = 0;
                        for (String remoteInterfaceName : bmd.ivBusinessRemoteInterfaceClassNames) {
                            bindRemoteInterfaceToContextRoot(binders, hr, remoteInterfaceName, interfaceIndex++);
                        }
                    }
                }
            }
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "bindAllRemoteInterfacesToContextRoot");
    }

    @SuppressWarnings("unchecked")
    private <T> void bindRemoteInterfaceToContextRoot(Map<EJBModuleMetaDataImpl, NameSpaceBinder<?>> binders,
                                                      HomeRecord hr,
                                                      String interfaceName,
                                                      int interfaceIndex) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "bindRemoteInterfaceToContextRoot : " + interfaceName);

        BeanMetaData bmd = hr.getBeanMetaData();
        EJBModuleMetaDataImpl mmd = hr.getEJBModuleMetaData();

        NameSpaceBinder<T> binder = (NameSpaceBinder<T>) binders.get(mmd);
        if (binder == null) {
            binder = (NameSpaceBinder<T>) createNameSpaceBinder(mmd);
            binders.put(mmd, binder);
        }

        EJSHome home = hr.getHome();
        try {
            HomeWrapperSet homeSet = (home != null) ? home.getWrapperSet() : null;

            int numRemoteInterfaces = countInterfaces(bmd, false);
            int numLocalInterfaces = countInterfaces(bmd, true);
            boolean singleGlobalInterface = (numRemoteInterfaces + numLocalInterfaces) == 1;

            // Create the object to be bound.
            T bindingObject = binder.createBindingObject(hr, homeSet, interfaceName, interfaceIndex, false);

            // Bind the object to configured locations.
            binder.bindBindings(bindingObject,
                                hr, numRemoteInterfaces, singleGlobalInterface,
                                interfaceIndex, interfaceName, false, (homeSet == null));
        } catch (Exception ex) {
            // Don't fail; log FFDC and continue binding the rest of the remote interfaces
            FFDCFilter.processException(ex, CLASS_NAME + ".bindRemoteInterfaceToContextRoot", "1187", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Ignoring bind failure : " + ex, ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "bindRemoteInterfaceToContextRoot");
    }

    /**
     * Binds the binding object for an interface into java:global.
     *
     * @param binder                the namespace binder
     * @param bindingObject         the binding object
     * @param hr                    the bean home record
     * @param interfaceName         the interface name being bound
     * @param singleGlobalInterface <tt>true</tt> if this bean has only one
     *                                  total interface (counting local and remote together)
     */
    private <T> void bindObjectToJavaNameSpaces(NameSpaceBinder<T> binder,
                                                T bindingObject,
                                                HomeRecord hr,
                                                boolean singleGlobalInterface,
                                                String interfaceName,
                                                int interfaceIndex,
                                                boolean local) throws NamingException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "bindObjectToJavaNameSpaces");

        BeanMetaData bmd = hr.getBeanMetaData();
        J2EEName j2eeName = bmd.j2eeName;
        String beanName = j2eeName.getComponent();

        List<String> jndiNames = new ArrayList<String>();
        if (hr.bindInterfaceNames()) {
            jndiNames.add(beanName + '!' + interfaceName);
        }
        if (singleGlobalInterface) {
            jndiNames.add(beanName);
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "names=" + jndiNames + ", global=" + hr.bindToJavaGlobalNameSpace());

        // d660700 - Bind to java:global if necessary.
        if (hr.bindToJavaGlobalNameSpace()) {
            if (bindingObject != null) {
                sendBindingMessage(bmd, beanName, interfaceName, interfaceIndex, local);
            }

            for (String jndiName : jndiNames) {
                if (bindingObject != null) {
                    binder.bindJavaGlobal(jndiName, bindingObject); // F69147.2
                }
                hr.ivJavaGlobalBindings.add(jndiName);
            }
        }

        // d660700 - Bind to java:app.
        for (String jndiName : jndiNames) {
            if (bindingObject != null) {
                binder.bindJavaApp(jndiName, bindingObject); // F69147.2
            }
            hr.ivJavaAppBindings.add(jndiName);
        }

        // d660700 - Bind to java:module.
        if (bindingObject != null) {
            for (String jndiName : jndiNames) {
                binder.bindJavaModule(jndiName, bindingObject); // F69147.2
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "bindObjectToJavaNameSpaces");
    }

    /**
     * Send the EJB binding message to Tr.info().
     */
    protected void sendBindingMessage(BeanMetaData bmd,
                                      String beanName,
                                      String interfaceName,
                                      int interfaceIndex,
                                      boolean local) {
        EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;
        String logicalAppName = mmd.getEJBApplicationMetaData().getLogicalName();
        String logicalModuleName = mmd.ivLogicalName;
        String jndiName = logicalAppName == null ? "java:global/" + logicalModuleName + '/' + beanName + '!'
                                                   + interfaceName : "java:global/" + logicalAppName + '/' + logicalModuleName + '/' + beanName + '!' + interfaceName;

        Tr.info(tc, "JNDI_BINDING_LOCATION_INFO_CNTR0167I",
                new Object[] { interfaceName,
                               bmd.j2eeName.getComponent(),
                               bmd.j2eeName.getModule(),
                               bmd.j2eeName.getApplication(),
                               jndiName });
    }

    /**
     * Send the MDB to ActivationSpec binding message to Tr.info().
     *
     * @param bmd the bean metadata for a message-driven bean
     */
    // d744887
    public void sendMDBBindingMessage(BeanMetaData bmd) {
        // Currently, an information message is only logged for message endpoints
        // that use a JCA resource adapter, not a message listener port.
        if (bmd.ivActivationSpecJndiName != null) {
            Tr.info(tc, "MDB_ACTIVATION_SPEC_INFO_CNTR0180I",
                    new Object[] { bmd.j2eeName.getComponent(),
                                   bmd.j2eeName.getModule(),
                                   bmd.j2eeName.getApplication(),
                                   bmd.ivActivationSpecJndiName });
        }
    }

    protected static Throwable getNestedException(Throwable t) //F743-16344 - made protected
    {
        Throwable root = t;
        Throwable cause;

        while ((cause = root.getCause()) != null) {
            root = cause;
        }

        return root;
    }

    /**
     * Create bean metadata with the specified module and component data. The
     * resulting BeanMetaData object is partially initialized; sufficiently that
     * remote homes can be bound lazily into the namespace. Before the metadata
     * is usable, {@link #finishBMDInit} must be called.
     */
    public BeanMetaData createBeanMetaData(BeanInitData bid, EJBModuleMetaDataImpl mmd) // d640935
                    throws EJBConfigurationException, ContainerException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createBeanMetaData: " + bid.ivJ2EEName);

        BeanMetaData bmd = ivEJBMDOrchestrator.createBeanMetaData(bid, mmd,
                                                                  ivContainer,
                                                                  ivInitAtStartup, ivInitAtStartupSet);

        // At this point we have constructed BeanMetaData from the merged WCCM and
        // Annotations metadata.  This seems like a fine spot to validate the merged
        // metadata before we go on to start binding artifacts related to the beans into
        // the name space.  For <= EJB2.1 beans we will put error messages into the
        // log if the metadata is bad.   For >= EJB 3.0 beans we will put out the same
        // error messages, but throw an exception in order to cause application start
        // to fail.  This strategy was chosen to not break existing apps that might be
        // slightly less than perfect, but still working ok as far as the customer was
        // concerned.
        ivEJBMDOrchestrator.validateMergedMetaData(bmd);

        // stopModule assumes homeRecord is set before BMD is inserted into
        // EJBModuleMetaDataImpl.ivBeanMetaDatas.                        RTC100347
        bmd.homeRecord = createHomeRecord(bmd, ivContainer.getHomeOfHomes()); // F91481

        // Add the BeanMetaData to the module.  This is needed to avoid creating
        // duplicate BMDs during startModule for EJB-in-WAR.
        mmd.ivBeanMetaDatas.put(bmd.enterpriseBeanName, bmd);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createBeanMetaData");

        return bmd;
    }

    protected HomeRecord createHomeRecord(BeanMetaData bmd, HomeOfHomes homeOfHomes) {
        return new HomeRecord(bmd, homeOfHomes);
    }

    /**
     * Adds a home to make it externally accessible.
     *
     * @param bmd the bean
     */
    protected void addHome(BeanMetaData bmd) throws ContainerException {
        try {
            EJSContainer.homeOfHomes.addHome(bmd); //d200714 d429866.2 F743-26072
        } catch (Throwable ex) {
            ContainerException ex2 = new ContainerException(ex);
            Tr.error(tc, "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                     new Object[] { ex, ex2.toString() });
            throw ex2;
        }
    }

    @Override
    public ActivationStrategy createActivationStrategy(Activator activator, int type, PassivationPolicy passivationPolicy) {
        switch (type) {
            case Activator.UNCACHED_ACTIVATION_STRATEGY:
                return new UncachedActivationStrategy(activator);

            case Activator.STATEFUL_ACTIVATE_ONCE_ACTIVATION_STRATEGY:
                return new StatefulActivateOnceActivationStrategy(activator, passivationPolicy);

            case Activator.STATEFUL_ACTIVATE_TRAN_ACTIVATION_STRATEGY:
                return new StatefulActivateTranActivationStrategy(activator, passivationPolicy, ivContainer.getSfFailoverCache());
        }

        return null;
    }

    @Override
    public final EJSHome initializeDeferredEJB(final HomeRecord hr) // d648522
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "initializeDeferredEJB: " + hr.getJ2EEName());

        // Non-deferred initialization during application start runs on a runtime
        // thread, so run deferred initialization with the same privileges.
        EJSHome result = AccessController.doPrivileged(new PrivilegedAction<EJSHome>() {
            @Override
            public EJSHome run() {
                // @251260 Begin
                Object credentialToken = pushServerIdentity();

                // @251260 End

                UOWHandle handle = null; //PK15508

                try {

                    //PK15508: In some cases, the code path leading up to the invocation of
                    //"deferredEJBInitialization" (DEI) will have caused the current
                    //UOW (Transaction or ActivitySession) to be suspended.  In other code
                    //paths the current UOW will not be suspended.  Problems have arose
                    //whereby certain things (e.g. isolation level) have been associated
                    //with the current UOW during DEI that should not be associated with the
                    //UOW.  Therefore, we should make sure any outstanding UOW (if any) are
                    //suspended for the execution of this method, and then resumed at the
                    //end of this method.
                    handle = ivUOWControl.suspend();

                    return runAsSystem(new PrivilegedExceptionAction<EJSHome>() // d658192, RTC71814
                    {
                        @Override
                        public EJSHome run() throws CSIException, ContainerException, EJBConfigurationException {
                            return initializeDeferredEJBImpl(hr);
                        }
                    });
                } catch (Throwable ex) { //PK15508: catch Throwable rather than Exception
                    if (ex instanceof PrivilegedActionException) // d658192
                    {
                        ex = ((PrivilegedActionException) ex).getCause();
                    }
                    FFDCFilter.processException(ex, CLASS_NAME + ".initializeDeferredEJB", "3233", this); //197547

                    // Fire the a destroyed event only if we fired a created
                    // event; we might have failed prior to the create event.
                    BeanMetaData bmd = hr.getBeanMetaData();
                    if (bmd.ivMetaDataDestroyRequired) {
                        // Reset the flag in case we fail on a subsequent
                        // initialization before we successfully create.
                        bmd.ivMetaDataDestroyRequired = false;

                        try {
                            fireMetaDataDestroyed(bmd);
                        } catch (RuntimeWarning rtw) {
                            // The fireMetaDataDestroyed method already did FFDC and logging of error message,
                            // do no need to do it again.  However, we want to eat the exception thrown by it
                            // since we want to throw the ContainerEJBException that is created
                            // below to report deferred EJB initialization failure.
                        }
                    }

                    //197547
                    ContainerEJBException cex = new ContainerEJBException //208298
                    ("Unable to initialize deferred EJB.", ex);

                    throw cex;
                } finally { //d200714
                    // @251260 Begin
                    if (credentialToken != null) {
                        popServerIdentity(credentialToken); // d646413.2
                    }
                    // @251260 End

                    //PK15508: Resume the transaction that was suspended earier.
                    try {
                        ivUOWControl.resume(handle);
                    } catch (Throwable e) {
                        FFDCFilter.processException(e, CLASS_NAME + ".initializeDeferredEJB", "4420", this);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            Tr.exit(tc, "initializeDeferredEJB", "Unexpected exception in initializeDeferredEJB when trying to resume the previously suspended UOW: " + e);
                        ContainerEJBException cex = new ContainerEJBException("Unexpected exception in initializeDeferredEJB when trying to resume the previously suspended UOW.");
                        throw cex;
                    }

                } // finally
            }
        });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "initializeDeferredEJB");

        return result;
    }

    /**
     * Run the specified action with a system identity.
     *
     * @param action the action
     * @return the result of the action
     * @throws PrivilegedActionException if a checked exception is thrown
     */
    // RTC71814
    protected abstract <T> T runAsSystem(PrivilegedExceptionAction<T> action) throws PrivilegedActionException;

    /**
     * Actually initializes the deferred EJB. Assumes that a "runtime thread
     * context" has already been established.
     *
     * @param hr
     * @return
     */
    protected EJSHome initializeDeferredEJBImpl(HomeRecord hr) throws ContainerException, EJBConfigurationException {
        BeanMetaData bmd = hr.getBeanMetaData();
        Object originalLoader = ThreadContextAccessor.UNCHANGED;

        try {
            if (!bmd.fullyInitialized) // d664917.1
            {
                // The server class loader must be used during metadata processing
                // for XML parsing classes.                         d200714, PM57099
                originalLoader = svThreadContextAccessor.pushContextClassLoader(getServerClassLoader()); // d278756, d334557, PK83186, d640395.1

                //------------------------------------------------------------------------
                // WARNING: We MUST reload WCCM object pointers here in case the module was
                //          unloaded.  The WAS runtime code has an alarm thread which unloads
                //          the module after a fixed time.  In the case of deferred EJB init
                //          processing it is up to us to "touch" the DeployedModule which will
                //          cause WCCM to completely reload a new set of WCCM objects for us.
                //------------------------------------------------------------------------
                bmd.wccm.reload(); // F743-18775

                finishBMDInit(bmd); //497153
            }

            //d200714 start
            // We must ensure that the application classloader is in force prior to starting
            // the home.
            originalLoader = svThreadContextAccessor.repushContextClassLoader(originalLoader, hr.getClassLoader());
            //d200714 end

            return fireMetaDataCreatedAndStartBean(bmd); // d648522, d739043
        } finally {
            svThreadContextAccessor.popContextClassLoader(originalLoader);

            // d659020 - If an error occurs while initializing, then the wccm
            // field will not be cleared, so we need to unload its references.
            // The next attempt to initialize the bean will reload them again.
            if (bmd.wccm != null) {
                bmd.wccm.unload();
            }
        }
    }

    /**
     * Creates the reference context for this EJB.
     */
    protected ReferenceContext createReferenceContext(BeanMetaData bmd) // F743-29417
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createReferenceContext: " + bmd.j2eeName);

        if (bmd.ivReferenceContext == null) {
            bmd.ivReferenceContext = getInjectionEngine().createReferenceContext();
            bmd.ivReferenceContext.add(new ComponentNameSpaceConfigurationProviderImpl(bmd, this)); // F85115
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createReferenceContext", bmd.ivReferenceContext);
        return bmd.ivReferenceContext;
    }

    /**
     * Initialize (or defer initialization) of a bean.
     * <p>
     * This method can be overridden by subclasses.
     */
    protected void initializeBMD(BeanMetaData bmd) throws Exception {
        if (bmd.ivDeferEJBInitialization) {
            // Before WCCM data structures are cleared, determine if
            // early reference processing is needed for this EJB.  For
            // example, if the bean has java:global modules, those need
            // to be bound at module startup.                   F743-29417
            if (isReferenceProcessingNeededAtStart(bmd)) {
                ReferenceContext rc = createReferenceContext(bmd);
                rc.process();
            }

            ivEJBMDOrchestrator.processDeferredBMD(bmd);
        } else {
            finishBMDInit(bmd);
        }
    }

    /**
     * Fully initialize the BeanMetaData. When this method completes
     * successfully, bmd.fullyInitialized will be true; this method must not be
     * called if this field is already true. The context class loader must be
     * the runtime class loader when calling this method.
     */
    private void finishBMDInit(BeanMetaData bmd) throws ContainerException, EJBConfigurationException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "finishBMDInit: " + bmd.j2eeName); // d640935.1

        // First, create the reference context for the bean if we haven't already
        // done so.
        createReferenceContext(bmd); // F743-29417

        ivEJBMDOrchestrator.finishBMDInitWithReferenceContext(bmd);

        // Free resources in EJBModuleMetaData if all beans have been initialized.
        bmd._moduleMetaData.freeResourcesAfterAllBeansInitialized(bmd); //d462512

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "finishBMDInit");
    }

    /**
     * Finish filling out enough of the bean metadata to be able to create the
     * component namespace configuration for the bean. The context class loader
     * must be the runtime class loader when calling this method.
     *
     * @return the component namespace configuration
     */
    public ComponentNameSpaceConfiguration finishBMDInitForReferenceContext(BeanMetaData bmd) throws EJBConfigurationException, ContainerException {
        return ivEJBMDOrchestrator.finishBMDInitForReferenceContext(bmd,
                                                                    ivDefaultDataSourceJNDIName,
                                                                    ivWebServicesHandlerResolver);
    }

    /**
     * Starts the bean by creating a home instance via the container. When this
     * method completes successfully, bmd.homeRecord.homeInternal will be set to
     * indicate that this home is started; this method must not be called if
     * this field is already set. The context class loader must be the bean
     * class loader when calling this method.
     *
     * @param bmd the bean metadata
     * @return the home
     */
    private EJSHome fireMetaDataCreatedAndStartBean(BeanMetaData bmd) // d648522
                    throws ContainerException {
        if (!bmd.isManagedBean()) // F743-34301.1
        {
            try {
                // Fire the ComponentMetaData event to the listeners
                // (ie. we have loaded a new bean folks... )
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "startBean: Fire Component Metadata created event to listeners for: " + bmd.j2eeName);

                bmd.ivMetaDataDestroyRequired = true; //d505055
                fireMetaDataCreated(bmd);
            } catch (Throwable t) //197547
            {
                FFDCFilter.processException(t, CLASS_NAME + "startBean", "445", this);
                throw new ContainerException("Failed to start " + bmd.j2eeName, t);
            }
        }

        return startBean(bmd); // d739043
    }

    protected EJSHome startBean(BeanMetaData bmd) throws ContainerException {
        // Register the bean metadata with the container.
        return ivContainer.startBean(bmd);
    }

    protected void stopBean(BeanMetaData bmd) throws CSIException {
        ivContainer.stopBean(bmd);
    }

    /**
     * Called to make a WebService Handler Resolver instance known to the
     * EJB Container Service. <p>
     *
     * The WebService Handler Resolver may be assigned any time after the
     * EJB Container Service has initialized. And, it may be called
     * multiple times, with the new setting replacing any prior setting.
     * This will allow WebSphere provisioning to start and stop the
     * EJB Container and Web Services components independently. <p>
     *
     * The EJB Container will use the assigned WebService Handler Resolver
     * to obtain the set of application Handler classes associated with
     * an EJB. The Handler classes are then processed for Injection
     * annotations and for generating the Endpoint Wrapper. <p>
     *
     * If a WebService Handler Resolver is not assigned, then the
     * EJB Container will assume there are no application handlers. <p>
     *
     * @param resolver WebService Handler Resolver to be used by EJB Container.
     **/
    // d495644
    public final void setWebServiceHandlerResolver(WSEJBHandlerResolver resolver) {
        ivWebServicesHandlerResolver = resolver;
    }

    @Override
    public void injectionMetaDataCreated(InjectionMetaData injectionMetaData) // F48603
                    throws InjectionException {
        ComponentNameSpaceConfiguration compNSConfig = injectionMetaData.getComponentNameSpaceConfiguration();
        ComponentNameSpaceConfiguration.ReferenceFlowKind flow = compNSConfig.getOwningFlow();

        if (flow == ComponentNameSpaceConfiguration.ReferenceFlowKind.EJB ||
            flow == ComponentNameSpaceConfiguration.ReferenceFlowKind.HYBRID) {
            injectionMetaData.bindJavaComp(
                                           "EJBContext", new Reference(EJBContext.class.getName(), EJBContextObjectFactory.class.getName(), null));
            injectionMetaData.bindJavaComp(
                                           "TimerService", new Reference(EJBContext.class.getName(), TimerServiceObjectFactory.class.getName(), null));
        }
    }

    @Override
    public ContainerTx createContainerTx(EJSContainer ejsContainer,
                                         boolean isLocal,
                                         SynchronizationRegistryUOWScope currTxKey,
                                         UOWControl uowCtrl) {
        return new ContainerTx(ejsContainer, isLocal, currTxKey, uowCtrl);
    }

    /**
     * Provides the BeanOFactory instances supported by the core EJBContainer. <p>
     *
     * This method should be overridden if additional bean types will be supported.
     */
    // F88119
    @Override
    public BeanOFactory getBeanOFactory(BeanOFactoryType type, BeanMetaData bmd) {
        BeanOFactory factory = null;
        switch (type) {
            case CM_STATELESS_BEANO_FACTORY:
                factory = ivCMStatelessBeanOFactory;
                if (factory == null) {
                    factory = new CMStatelessBeanOFactory();
                    ivCMStatelessBeanOFactory = factory;
                }
                break;

            case BM_STATELESS_BEANO_FACTORY:
                factory = ivBMStatelessBeanOFactory;
                if (factory == null) {
                    factory = new BMStatelessBeanOFactory();
                    ivBMStatelessBeanOFactory = factory;
                }
                break;

            case CM_STATEFUL_BEANO_FACTORY:
                factory = ivCMStatefulBeanOFactory;
                if (factory == null) {
                    factory = new CMStatefulBeanOFactory();
                    ivCMStatefulBeanOFactory = factory;
                }
                break;

            case BM_STATEFUL_BEANO_FACTORY:
                factory = ivBMStatefulBeanOFactory;
                if (factory == null) {
                    factory = new BMStatefulBeanOFactory();
                    ivBMStatefulBeanOFactory = factory;
                }
                break;

            case SINGLETON_BEANO_FACTORY:
                factory = ivSingletonBeanOFactory;
                if (factory == null) {
                    factory = new SingletonBeanOFactory();
                    ivSingletonBeanOFactory = factory;
                }
                break;

            case MANAGED_BEANO_FACTORY:
                factory = ivManagedBeanOFactory;
                if (factory == null) {
                    factory = new ManagedBeanOFactory();
                    ivManagedBeanOFactory = factory;
                }
                break;

            default:
                // Not expected this method would ever be called for an unsupported
                // factory but throw something better than an NPE.
                throw new UnsupportedOperationException("Bean type not supported in current environment: " + type);
        }
        return factory;
    }

    @Override
    // F88119
    public Class<?> getMessageEndpointFactoryImplClass(BeanMetaData bmd) throws ClassNotFoundException {
        // Not expected this method would ever be called when MessageEndpoint
        // MDBs are not supported, but throw something better than an NPE.
        throw new UnsupportedOperationException("Message-Driven beans are not supported in the current environment : " +
                                                bmd.getName());
    }

    @Override
    // F88119
    public Class<?> getMessageEndpointImplClass(BeanMetaData bmd) throws ClassNotFoundException {
        // Not expected this method would ever be called when MessageEndpoint
        // MDBs are not supported, but throw something better than an NPE.
        throw new UnsupportedOperationException("Message-Driven beans are not supported in the current environment : " +
                                                bmd.getName());
    }

    @Override
    public void resolveMessageDestinationJndiName(BeanMetaData bmd) {
        // Not expected this method would ever be called when MessageEndpoint
        // MDBs are not supported, but throw something better than an NPE.
        throw new UnsupportedOperationException("Message-Driven beans are not supported in the current environment : " +
                                                bmd.getName());
    }

    /**
     * Only Called if non-persistent automatic timers exist
     *
     * Creates the non-persistent automatic timers for the specified module.
     *
     * @param appName    the application name
     * @param moduleName the module name
     * @param timerBeans the beans with automatic timers
     * @return the number of timers created
     */
    // F743-506 RTC109678
    protected int createNonPersistentAutomaticTimers(String appName, String moduleName, List<AutomaticTimerBean> timerBeans) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createNonPersistentAutomaticTimers: " + moduleName);

        int numCreated = 0;

        for (AutomaticTimerBean timerBean : timerBeans) {
            if (timerBean.getNumNonPersistentTimers() != 0) {
                for (TimerMethodData timerMethod : timerBean.getMethods()) {
                    for (TimerMethodData.AutomaticTimer timer : timerMethod.getAutomaticTimers()) {
                        if (!timer.isPersistent()) {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "creating non-persistent automatic timer " + timer);

                            createNonPersistentAutomaticTimer(timerBean, timer, timerMethod);
                            numCreated++;
                        }
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createNonPersistentAutomaticTimers: " + numCreated);
        return numCreated;
    }

    /**
     * Creates a specific non-persistent automatic timer and schedules the timer to run.
     */
    // RTC109678
    protected void createNonPersistentAutomaticTimer(AutomaticTimerBean timerBean,
                                                     TimerMethodData.AutomaticTimer timer,
                                                     TimerMethodData timerMethod) {
        BeanMetaData bmd = timerBean.getBeanMetaData();
        ParsedScheduleExpression parsedSchedule = timerBean.parseScheduleExpression(timer); // F743-14447
        TimerNpImpl ejbTimer = new TimerNpImpl(bmd.container, bmd, // d589357
                        timerBean.getBeanId(), timerMethod.getMethodId(), parsedSchedule, timer.getInfo());
        ejbTimer.start(); // d589357
    }

    /**
     * Only called if persistent timers exist <p>
     *
     * Creates the persistent automatic timers for the specified module. <p>
     *
     * The default behavior is that persistent timers are not supported and
     * warnings will be logged if they are present. Subclasses should override
     * this method if different behavior is required. <p>
     *
     * @param appName    the application name
     * @param moduleName the module name
     * @param timerBeans the beans with automatic timers
     * @return the number of timers created
     */
    protected int createPersistentAutomaticTimers(String appName, String moduleName, List<AutomaticTimerBean> timerBeans) throws RuntimeWarning {
        for (AutomaticTimerBean timerBean : timerBeans) {
            if (timerBean.getNumPersistentTimers() != 0) {
                Tr.warning(tc, "AUTOMATIC_PERSISTENT_TIMERS_NOT_SUPPORTED_CNTR0330W",
                           new Object[] { timerBean.getBeanMetaData().getName(), moduleName, appName });
            }
        }
        return 0;
    }

    @Override
    public Timer createTimer(BeanO beanO, Date expiration, long interval, ScheduleExpression schedule, Serializable info, boolean persistent) {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.entry(tc, "createTimer : " + beanO);

        Timer timer;

        if (schedule == null) {
            // Now create the Timer, if persistent this will also create the
            // Task in the the persistent store.
            if (persistent) {
                timer = createPersistentExpirationTimer(beanO.getId(), expiration, interval, info);
            } else {
                timer = createNonPersistentExpirationTimer(beanO, expiration, interval, info);
            }
        } else {
            ParsedScheduleExpression parsedExpr;
            try {
                parsedExpr = ScheduleExpressionParser.parse(ivContainer.ivObjectCopier.copy(schedule));
            } catch (ScheduleExpressionParserException ex) {
                // Rethrow ScheduleExpressionParserException as IllegalArgumentException
                // to hide internal details and to avoid serialization issues for remote clients.
                IllegalArgumentException ise = new IllegalArgumentException("TimerService: schedule is not valid: " + ex.getMessage());

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "createTimer: " + ise);
                throw ise;
            }

            // Now create the Timer, if persistent this will also create the
            // Task in the the persistent store.
            if (persistent) {
                timer = createPersistentCalendarTimer(beanO.getId(), parsedExpr, info);
            } else {
                timer = createNonPersistentCalendarTimer(beanO, parsedExpr, info);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createTimer :" + timer);
        return timer;
    }

    /**
     * Creates a persistent expiration based (single action or interval) EJB timer.
     *
     * @param beanId     the bean Id for which the timer is being created
     * @param expiration the initial expiration for a interval-based timer
     * @param interval   the interval for an interval-based timer, or -1 for a single-action timer
     * @param info       application information to be delivered to the timeout method, or null
     */
    protected abstract Timer createPersistentExpirationTimer(BeanId beanId, Date expiration, long interval, Serializable info);

    /**
     * Creates a persistent calendar based EJB timer.
     *
     * @param beanId     the bean Id for which the timer is being created
     * @param parsedExpr the parsed values of the schedule for a calendar-based timer
     * @param info       application information to be delivered to the timeout method, or null
     */
    protected abstract Timer createPersistentCalendarTimer(BeanId beanId, ParsedScheduleExpression parsedExpr, Serializable info);

    /**
     * Creates a non-persistent expiration based (single action or interval) EJB timer.
     *
     * @param beanO      the bean for which the timer is being created
     * @param expiration the initial expiration for a interval-based timer
     * @param interval   the interval for an interval-based timer, or -1 for a single-action timer
     * @param info       application information to be delivered to the timeout method, or null
     */
    protected Timer createNonPersistentExpirationTimer(BeanO beanO, Date expiration, long interval, Serializable info) {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.entry(tc, "createNonPersistentExpirationTimer : " + beanO);

        if (beanO.getHome().getBeanMetaData().isEntityBean()) {
            IllegalStateException ise = new IllegalStateException("Timer Service: Entity beans cannot use non-persistent timers : " + beanO.getId().getJ2EEName());

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createNonPersistentExpirationTimer : " + ise);
            throw ise;
        }

        // create the non-persistent Timer
        TimerNpImpl timer = new TimerNpImpl(beanO.getId(), expiration, interval, info);

        // queue timer to start (or start immediately if not in a global tran)
        queueOrStartNpTimer(beanO, timer);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createNonPersistentExpirationTimer : " + timer);
        return timer;
    }

    /**
     * Creates a non-persistent calendar based EJB timer.
     *
     * @param beanId     the bean Id for which the timer is being created
     * @param parsedExpr the parsed values of the schedule for a calendar-based timer
     * @param info       application information to be delivered to the timeout method, or null
     */
    protected Timer createNonPersistentCalendarTimer(BeanO beanO, ParsedScheduleExpression parsedExpr, Serializable info) {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.entry(tc, "createNonPersistentCalendarTimer : " + beanO);

        // create the non-persistent Timer
        TimerNpImpl timer = new TimerNpImpl(beanO.getId(), parsedExpr, info);

        // queue timer to start (or start immediately if not in a global tran)
        queueOrStartNpTimer(beanO, timer);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createNonPersistentCalendarTimer : " + timer);
        return timer;
    }

    /**
     * Queues a non-persistent timer with the current transaction or starts
     * the non-persistent timer if no transaction is present. <p>
     *
     * Non-persistent timers should not start running or become visible to
     * other threads until the current transaction commits. <p>
     *
     *
     * @param beanO the bean for which the timer is being created
     * @param timer the non-persistent timer to queue or start
     *
     * @throws EJBException if the application is being stopped or timer fails to start
     */
    // RTC107334
    private void queueOrStartNpTimer(BeanO beanO, TimerNpImpl timer) throws EJBException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        try {
            // d589357 - Prevent NP timers from being created after the
            // application has been stopped.  This call will race if it occurs on
            // a thread other than the one stopping the application, but
            // EJBApplicationMeaData.queueOrStartNonPersistentTimerAlarm won't
            // actually start the alarm.
            EJBModuleMetaDataImpl mmd = beanO.getHome().getBeanMetaData()._moduleMetaData;
            mmd.getEJBApplicationMetaData().checkIfCreateNonPersistentTimerAllowed(mmd);

            ContainerTx tx = beanO.getContainerTx();
            if (tx == null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "queueOrStartNpTimer found ivContainerTx null.  Calling getCurrentTx(false)");

                tx = beanO.getContainer().getCurrentContainerTx();
            }

            if (tx != null) {
                // Will queue timer for global / start immediately for local transactions
                tx.queueTimerToStart(timer);
            } else {
                // Here for backward compatibility. Could only occur for EJB 1.1
                // module with BMT bean, which the customer has re-coded to
                // implement TimedObject. Start the timer immediately, as this
                // would be a local transaction scenario in later module levels.
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Container Tx not found; starting timer immediately.");
                timer.start();
            }
        } catch (Exception e) {
            throw ExceptionUtil.EJBException(e);
        }
    }

    @Override
    public void checkLateTimerThreshold(Date scheduledRunTime, String timerId, J2EEName j2eeName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "checkLateTimerThreshold : " + scheduledRunTime + ", " + timerId + ", " + j2eeName);

        // EJB Timers never skip, however a warning may be logged if a timer
        // is running noticeably later than scheduled; like ASYN0091_LATE_ALARM
        long lateTimerThreshold = getLateTimerThreshold();

        if (lateTimerThreshold > 0) {
            long currentTime = System.currentTimeMillis();
            long lateTime = currentTime - scheduledRunTime.getTime();

            if (lateTime > lateTimerThreshold) {
                long secondsLate = lateTime / 1000;
                Tr.warning(tc, "TIMER_FIRING_LATE_CNTR0333W",
                           new Object[] { timerId,
                                          j2eeName.getComponent(),
                                          j2eeName.getModule(),
                                          j2eeName.getApplication(),
                                          scheduledRunTime, new Date(currentTime),
                                          secondsLate });
            }
        }
    }

    /**
     * Returns the late timer warning threshold that has been configured for
     * EJB timers; default is 5 minutes and 0 disables the warning message.
     */
    protected abstract long getLateTimerThreshold();

}
