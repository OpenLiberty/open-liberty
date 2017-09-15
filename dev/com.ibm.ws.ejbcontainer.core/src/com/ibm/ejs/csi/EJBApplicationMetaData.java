/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.csi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.ContainerProperties;
import com.ibm.ejs.container.EJBNotFoundException;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.HomeRecord;
import com.ibm.ejs.container.TimerNpImpl;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ejbcontainer.AmbiguousEJBReferenceException;
import com.ibm.websphere.ejbcontainer.ApplicationNotStartedException;
import com.ibm.websphere.ejbcontainer.EJBStoppedException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.runtime.EJBApplicationEventListener;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;

/**
 * <code>EJBApplicationMetaDataImpl<code> is the EJB Container's internal metadata
 * specific to an application. This object will be stored
 * in the slot reserved for the EJBContainer within the
 * ApplicationMetaData.
 */
public class EJBApplicationMetaData {
    private static final String CLASS_NAME = EJBApplicationMetaData.class.getName(); // d462512
    private static final TraceComponent tc = Tr.register(EJBApplicationMetaData.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * The container for this application.
     */
    // F743-506
    private final EJSContainer ivContainer;

    /**
     * The name of this application.
     */
    // F743-12528
    private final String ivName;

    /**
     * The logical name of this application specified by the application-name element in
     * application.xml, the name of the EAR without ".ear", <tt>null</tt> if the application
     * was a standalone module, or the logical name to be shared by several standalone
     * modules.
     */
    private final String ivLogicalName; // F743-26137

    /**
     * True if this application is a module without an EAR.
     */
    private final boolean ivStandaloneModule; // d660700

    /**
     * The runtime application meta data. This field will be null if the application is not
     * running in a WAS server environment.
     */
    // F743-12528
    private final ApplicationMetaData ivApplicationMetaData;

    /**
     * The EJB modules associated with this application.
     */
    private final Set<EJBModuleMetaDataImpl> ivModules = new LinkedHashSet<EJBModuleMetaDataImpl>(); // F743-26072

    /**
     * True if this application is currently blocking incoming work until it has fully
     * started. This flag is first initialized to the value of
     * {@link #ivCurrentlyBlockingWork} and then later set to false once the application has
     * fully started. If a late-add module is being started, the flag is reset to
     * ivBlockWorkUntilStarted.
     * 
     * <p>
     * Modifications to this variable are protected by synchronizing this object.
     */
    private volatile boolean ivCurrentlyBlockingWork; // F743-15941

    /**
     * True if this application was initially marked as blocking work.
     * 
     * <p>
     * Access to this variable is protected by synchronizing this object.
     */
    private boolean ivBlockWorkUntilStarted; // F743-15941

    /**
     * The thread that is starting the application or starting a late-add module within the
     * application, or null if the application is fully started and no late-add modules are
     * being started. If a late-add module is being started, then ivModuleBeingAddedLate
     * will also be non-null. Note that this relies on the runtime not starting modules from
     * the same application on multiple threads.
     * 
     * <p>
     * Access to this variable is protected by synchronizing this object.
     */
    private Thread ivStartupThread;

    /**
     * True if the EJBs in this application should be bound to their configured binding
     * locations.
     */
    private boolean ivBindToServerRoot = ContainerProperties.BindToServerRoot; // F743-33812,
                                                                               // RTC112554

    /**
     * True if the EJBs in this application should be bound to the java:global names
     * mandated by the EJB specification.
     */
    private boolean ivBindToJavaGlobal = ContainerProperties.BindToJavaGlobal; // F743-33812,
                                                                               // RTC112554

    /**
     * Application Custom Property :
     * 
     * True if checkEJBApplicationConfiguration has been enabled for this application.
     * Default is false.
     */
    // F743-33178
    private boolean ivCheckConfig;

    /**
     * Application Custom Property :
     * 
     * True if the local client views for EJBs in this application should use proxy
     * wrappers.
     */
    private boolean ivIndirectLocalProxies; // F58064

    /**
     * This ArrayList contains a list of all Singleton Session Beans that have been marked
     * as "Startup" via annotations or XML.
     * 
     * <p>
     * This list is null until the first startup singleton is found. This list is always
     * null after the application has started.
     */
    private List<J2EEName> ivStartupSingletonList;

    /**
     * List of singletons that have dependencies on other singletons. A LinkedHashMap is
     * used for fast iteration.
     * 
     * <p>
     * This list is null until the first application with dependencies is found. This list
     * is always null after the application has started.
     */
    private LinkedHashMap<BeanMetaData, Set<String>> ivSingletonDependencies;

    /**
     * The exception that occurred while resolving a singleton's dependencies "early" (prior
     * to {@link resolveDependencies} being called).
     */
    private RuntimeWarning ivSingletonDependencyResolutionException; // F743-20281

    /**
     * List of singletons in the order that they were initialized. A Set is used to ensure
     * that a singleton is only added once even if errors occur and initialization is
     * attempted multiple times. A LinkedHashSet is used to ensure stable ordering.
     * 
     * <p>
     * This list is null until initialization is attempted for a singleton.
     */
    private LinkedHashSet<EJSHome> ivSingletonInitializations;

    /**
     * List of timers that were started prior to the application being fully started. This
     * could occur because of automatic timers or because the timer is created from a
     * startup bean.
     * 
     * <p>
     * This list is null until the first non-persistent timer is started prior to the
     * application being fully started. This list is always null after the application has
     * started.
     */
    // F743-506
    private List<TimerNpImpl> ivQueuedNonPersistentTimers;

    /**
     * True if this application is stopping or has stopped.
     */
    private boolean ivStopping;

    /**
     * d607800.CodRv
     * 
     * Indicates that we are in a late-add flow (ie, module is being added after the
     * application started event was sent).
     * 
     * Also indicates exactly which module we are adding late, which allows to us continue
     * allowing access to beans in other modules (which were already started), while
     * preventing access to beans in the module we are currently starting.
     * 
     * After the late-add module has been processed (either we successfully started it, or
     * blew up trying), this is set back to null to indicate that we are no longer in the
     * process of late-adding a new module.
     * 
     * <p>
     * Access to this variable is protected by synchronizing this object.
     */
    private EJBModuleMetaDataImpl ivModuleBeingAddedLate = null;

    /**
     * The EJBApplicationMetaData constructor will set the starting thread and set the
     * boolean indicating that the application is in the process if starting and
     * initializing. It also set the ArrayList of Startup Singletons to null since there may
     * not be any in this particular application.
     * 
     * @param container
     *            the container for this application
     * @param name
     *            the application name
     * @param logicalName
     *            the logical name of the application
     * @param standaloneModule
     *            <tt>true</tt> if this application is a module without an EAR
     * @param amd
     *            the WAS server runtime application meta data, or null if the application
     *            is not running in a WAS server environment or the embeddable EJB container
     * @param started
     *            <tt>true</tt> if the application is already started
     * @param blockWorkUntilStarted
     *            <tt>true</tt> if this application should block incoming work until the
     *            application is fully started
     */
    public EJBApplicationMetaData(EJSContainer container, String name, String logicalName, boolean standaloneModule,
            ApplicationMetaData amd, boolean started, boolean blockWorkUntilStarted) {
        ivContainer = container; // F743-506
        ivName = name; // F743-12528
        ivLogicalName = logicalName; // F743-26137
        ivStandaloneModule = standaloneModule; // d660700
        ivApplicationMetaData = amd;

        ivBlockWorkUntilStarted = blockWorkUntilStarted; // F743-15941
        if (!started) { // F743-26072
            ivCurrentlyBlockingWork = ivBlockWorkUntilStarted; // F743-15941
            ivStartupThread = Thread.currentThread();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "EJBApplicationMetaData object created for thread: " + ivStartupThread + ", blockWorkUntilStarted="
                    + blockWorkUntilStarted);
    }

    /**
     * Returns the name of this application.
     * 
     * @return the application name
     */
    // F743-12528
    public String getName() {
        return ivName;
    }

    /**
     * Returns the logical name of this application. The logical name of an application is
     * specified by the application-name element in application.xml, the name of the EAR
     * without ".ear", <tt>null</tt> if the application was a standalone module, or the
     * logical name to be shared by several standalone modules.
     */
    public String getLogicalName() // F743-26137
    {
        return ivLogicalName;
    }

    /**
     * Returns true if this application is a module without an EAR.
     */
    public boolean isStandaloneModule() // d660700
    {
        return ivStandaloneModule;
    }

    /**
     * Returns the runtime application metadata. The result will be null if the application
     * is not running in a WAS server environment or the embeddable EJB container.
     * 
     * @return the application metadata, or null if unavailable
     */
    // F743-12528
    public ApplicationMetaData getApplicationMetaData() {
        return ivApplicationMetaData;
    }

    /**
     * Sets whether or not to bind EJBs in this application to the configured binding
     * locations.
     * <p>
     * 
     * This method sets an application custom property.
     */
    // F743-33812
    public void setBindToServerRoot(boolean bind) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setBindToServerRoot: " + bind);
        ivBindToServerRoot = bind;
    }

    /**
     * Returns <tt>true</tt> if EJBs in this application should be bound to the configured
     * binding locations.
     * <p>
     * 
     * This method returns an application custom property.
     */
    // F743-33812
    public boolean isBindToServerRoot() {
        return ivBindToServerRoot;
    }

    /**
     * Sets whether or not to bind EJBs in this application to the java:global names
     * required by the EJB specification.
     * <p>
     * 
     * This method sets an application custom property.
     */
    // F743-33812
    public void setBindToJavaGlobal(boolean bind) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setBindToJavaGlobal: " + bind);
        ivBindToJavaGlobal = bind;
    }

    /**
     * Returns <tt>true</tt> if EJBs in this application should be bound to the java:global
     * names required by the EJB specification.
     * <p>
     * 
     * This method returns an application custom property.
     */
    // F743-33812
    public boolean isBindToJavaGlobal() {
        return ivBindToJavaGlobal;
    }

    /**
     * Sets whether or not to perform more rigid checking for EJB application configuration
     * problems. This may result in additional warnings and/or errors.
     * <p>
     * 
     * This method sets an application custom property.
     */
    // F743-33178
    public void setCheckConfig(boolean checkConfig) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setCheckConfig: " + checkConfig);
        ivCheckConfig = checkConfig;
    }

    /**
     * Returns <tt>true</tt> if more rigid checking for EJB application configuration
     * problems should be performed and should result in additional warnings and/or errors.
     * <p>
     * .
     * 
     * This method returns an application custom property.
     */
    // F743-33178
    public boolean isCheckConfig() {
        return ivCheckConfig;
    }

    // F58064
    public void setIndirectLocalProxies(boolean indirectLocalProxies) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setIndirectLocalProxies: " + indirectLocalProxies);
        ivIndirectLocalProxies = indirectLocalProxies;
    }

    // F58064
    public boolean isIndirectLocalProxies() {
        return ivIndirectLocalProxies;
    }

    /**
     * Adds a singleton bean belonging to this application.
     * 
     * @param bmd
     *            the bean metadata
     * @param startup
     *            true if this is a startup singleton bean
     * @param dependsOnLinks
     *            list of dependency EJB links
     */
    public void addSingleton(BeanMetaData bmd, boolean startup, Set<String> dependsOnLinks) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "addSingleton: " + bmd.j2eeName + " (startup=" + startup + ", dependsOn=" + (dependsOnLinks != null) + ")");

        if (startup) {
            if (ivStartupSingletonList == null) {
                ivStartupSingletonList = new ArrayList<J2EEName>();
            }
            ivStartupSingletonList.add(bmd.j2eeName);
        }

        if (dependsOnLinks != null) {
            if (ivSingletonDependencies == null) {
                ivSingletonDependencies = new LinkedHashMap<BeanMetaData, Set<String>>();
            }
            ivSingletonDependencies.put(bmd, dependsOnLinks);
        }
    }

    /**
     * Prevent EJB work until the application has finished starting startup singletons. This
     * method blocks for a period of time to give the application a chance to start
     * (finishStarting) and then throws an exception if the application is still not
     * started. The only exception to this rule is work that is happening as part of
     * application startup.
     * 
     * @return true if the application has started, and false if the application has not
     *         started, but work is allowed from the current thread
     * @throws ApplicationNotStartedException
     *             if the application has not started and work is not allowed from the
     *             current thread
     * @throws EJBStoppedException
     *             if the application failed to start while waiting for startup singletons.
     */
    public boolean checkIfEJBWorkAllowed(EJBModuleMetaDataImpl module) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // F743-15941 - By default, this flag will be false for compatibility.
        // For performance, this flag will be reset after the application is
        // fully started, so we use double-checked locking with a volatile.
        // For late add modules, this flag will be reset back to true by the
        // starting thread before any beans from that module are exposed.
        if (!ivCurrentlyBlockingWork) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "checkIfEJBWorkAllowed: " + module.getJ2EEName() + ": not blocking work");
            return true;
        }

        EJBModuleMetaDataImpl moduleBeingAddedLate;
        synchronized (this) {
            if (!ivCurrentlyBlockingWork) {
                // The application has fully started, so allow this and all future
                // work requests.
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "checkIfEJBWorkAllowed: " + module.getJ2EEName() + ": not blocking work");
                return true;
            }

            if (ivStartupThread == Thread.currentThread()) {
                // The application is in the process of being started, and the
                // caller is the startup thread. Allow this work request only
                // until the application is fully started.
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "checkIfEJBWorkAllowed: " + module.getJ2EEName() + ": allowing startup thread");
                return false;
            }

            moduleBeingAddedLate = ivModuleBeingAddedLate;
            if (moduleBeingAddedLate != null && moduleBeingAddedLate != module) {
                // startupThread is not null and ivModuleBeingAddedLate is not
                // null, which means a module is being added to a started
                // application. Since the work request is for a module that is
                // already part of the started application, allow this and all
                // future work requests.
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "checkIfEJBWorkAllowed: " + module.getJ2EEName() + ": allowing non-late-add module");
                return true;
            }

            // At this point, one of two scenarios is occurring:
            // 1. The application is being started, but work has been requested on
            // some thread other than the startup thread.
            // 2. A module is being added to a started application, but work has
            // been requested for that module on some thread other than the
            // module startup thread.
            //
            // F743-15941 - In either case, wait for the application or module to
            // finish starting either successfully or unsuccessfully.
            long begin = System.nanoTime();
            long end = begin + TimeUnit.MILLISECONDS.toNanos(ContainerProperties.BlockWorkUntilAppStartedWaitTime);

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "checkIfEJBWorkAllowed: " + module.getJ2EEName() + ": waiting for application to start");

            // Continue waiting for either the application or module to finish
            // starting. If we were waiting for a late-add module (i.e.,
            // moduleBeingAddedLate != null), then stop waiting if a different
            // module is now being started.
            while (ivCurrentlyBlockingWork && (moduleBeingAddedLate == null || moduleBeingAddedLate == ivModuleBeingAddedLate)) {
                try {
                    long waitTime = end - System.nanoTime();
                    if (waitTime <= 0) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "checkIfEJBWorkAllowed: wait timed out");

                        // The application or module did not finish starting
                        // in the allotted time. Note that this will occur if
                        // the startup thread (e.g., servlet context listener,
                        // singleton startup, or legacy startup bean) attempted
                        // to wait for asynchronous work (e.g., fire-and-return
                        // async, MDB, timer) to be delivered to itself.

                        if (moduleBeingAddedLate != null) {
                            throw new ApplicationNotStartedException("module " + module.getName() + " in application " + ivName
                                    + " has not finished startup processing");
                        }

                        throw new ApplicationNotStartedException("application " + ivName + " has not finished startup processing");
                    }

                    // Wait until notified by unblockThreadsWaitingForStart.
                    TimeUnit.NANOSECONDS.timedWait(this, waitTime);
                } catch (InterruptedException ex) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "checkIfEJBWorkAllowed: wait interrupted");
                    FFDCFilter.processException(ex, CLASS_NAME + ".checkIfEJBWorkAllowed", "360", this);
                    Thread.currentThread().interrupt();
                }
            }
        }

        // The application or module is no longer starting, but now we need
        // to check whether the start was successful.

        // EJBModuleMetaDataImpl.ivStopping isn't volatile, but the startup
        // thread will also synchronize on this object via cleanUp.
        if (moduleBeingAddedLate != null && moduleBeingAddedLate.ivStopping) {
            // The late-add module failed to start.
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "checkIfEJBWorkAllowed: module failed to start");
            throw new EJBStoppedException("module " + module.getName() + " in application " + ivName + " failed to start");
        }

        if (ivStopping) {
            // The application failed to start.
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "checkIfEJBWorkAllowed: application failed to start");
            throw new EJBStoppedException("application " + ivName + " failed to start");
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "checkIfEJBWorkAllowed: application started");
        return true;
    }

    /**
     * Prevent non-persistent timers from being created after the application or module has
     * begun stopping.
     * 
     * @throws EJBStoppedException
     *             if the application or module is stopping
     */
    public synchronized void checkIfCreateNonPersistentTimerAllowed(EJBModuleMetaDataImpl mmd) {
        if (ivStopping) {
            throw new EJBStoppedException(ivName);
        }

        if (mmd != null && mmd.ivStopping) {
            throw new EJBStoppedException(mmd.getJ2EEName().toString());
        }
    }

    /**
     * Notification that a module within this application is starting. This method can
     * either be called as part of application start or as part of a fine-grained
     * application update, in which case a module is being added to this already started
     * application. Regardless of the outcome of this call, the caller is responsible for
     * ensuring that a subsequent call is made to either {@link #startedModule} or
     * {@link #stoppingModule}.
     * 
     * @param mmd
     *            the module being started
     * @param blockWorkUntilStarted
     *            <tt>true</tt> if this module should block incoming work until it is fully
     *            started
     */
    public synchronized void startingModule(EJBModuleMetaDataImpl mmd, boolean blockWorkUntilStarted) { // F743-26072
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "startingModule: " + mmd.getJ2EEName() + ", blockWorkUntilStarted=" + blockWorkUntilStarted + ", started="
                    + isStarted());

        ivModules.add(mmd);

        // If the application is already started, then this is a fine-grained
        // module start. Prevent work on this module until it fully starts.
        if (isStarted()) {
            ivModuleBeingAddedLate = mmd;
            ivStartupThread = Thread.currentThread();
            ivBlockWorkUntilStarted |= blockWorkUntilStarted; // F743-15941
            ivCurrentlyBlockingWork = ivBlockWorkUntilStarted; // F743-15941
        }
    }

    /**
     * Notification that the application is about to finish starting. Performs processing of
     * singletons after all modules have been started but before the application has
     * finished starting. Module references are resolved, and startup singletons are
     * started.
     * 
     * @throws RuntimeWarning
     *             if a dependency reference cannot be resolved or a startup singleton fails
     *             to initialize
     */
    private void finishStarting() throws RuntimeWarning {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "finishStarting: " + ivName);

        // NOTE - Any state that is cleared here should also be cleared in
        // stoppingModule.

        if (ivModules != null) { // F743-26072
            notifyApplicationEventListeners(ivModules, true);
        }

        // Signal that the application is "fully started".
        unblockThreadsWaitingForStart(); // F743-15941

        synchronized (this) {
            // F743-506 - Now that the application is fully started, start the
            // queued non-persistent timers.
            if (ivQueuedNonPersistentTimers != null) {
                startQueuedNonPersistentTimerAlarms();
                ivQueuedNonPersistentTimers = null;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "finishStarting");
    }

    /**
     * Notification that a module within this application has finished starting
     * successfully. The caller must ensure that a prior call to {@link #startingModule} has
     * been made. If this method throws an exception, the caller is responsible for ensuring
     * that stoppingModule is called.
     * 
     * @param mmd
     *            the module
     * @throws RuntimeWarning
     *             if an error occurs while finishing the start
     */
    public void startedModule(EJBModuleMetaDataImpl mmd) throws RuntimeWarning { // F743-26072
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "startedModule: " + mmd.getJ2EEName());

        // NOTE - Any state that is cleared here should also be cleared in
        // stoppingModule.

        // d701887 - CTS requires that we start startup singletons at the end
        // of module start rather than at the end of app start. We depend on
        // ejb-link (rather than merged view or similar) to resolve singleton
        // dependencies, so this isn't ideal for the reasons explained in
        // resolveBeanDependencies(BeanMetaData).

        if (ivSingletonDependencies != null) {
            resolveDependencies();
            ivSingletonDependencies = null;
        }

        if (ivStartupSingletonList != null) {
            createStartupBeans();
            ivStartupSingletonList = null;
        }

        // If this was a late-add module, then finishing up starting as if the
        // entire application had finished starting.
        if (ivModuleBeingAddedLate == mmd) {
            // ivModuleBeingAddedLate (and other state variables) will either
            // be cleared as part of finishStarting if the start finishes
            // successfully or as part of stoppingModule, which the caller is
            // required to call, if the start fails.
            finishStarting();
        }
    }

    /**
     * Notification that the modules within the application have finished starting. The
     * caller is responsible for calling {@link #stopping} if this method fails.
     * 
     * @throws RuntimeWarning
     *             if an error occurs while finishing the start
     */
    public void started() throws RuntimeWarning { // F743-26072
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "started");

        finishStarting();
    }

    /**
     * Signals that any threads waiting for this application to become fully started should
     * be unblocked.
     */
    private synchronized void unblockThreadsWaitingForStart() { // F743-15941
        ivStartupThread = null;
        ivModuleBeingAddedLate = null; // d607800
        ivCurrentlyBlockingWork = false;

        // Notify any threads that are waiting in checkIfEJBWorkAllowed.
        notifyAll();
    }

    /**
     * Resolves a dependency reference link to another EJB.
     * 
     * @param source
     *            the dependent bean
     * @param link
     *            the dependency bean using EJB link syntax
     * @return the resulting home record
     * @throws RuntimeWarning
     *             if the link cannot be resolved
     */
    private HomeRecord resolveEJBLink(J2EEName source, String link) throws RuntimeWarning {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolveEJBLink: " + link);

        String module = source.getModule();
        String component = source.getComponent();
        HomeRecord result;

        // F7434950.CodRev - Use resolveEJBLink
        try {
            result = EJSContainer.homeOfHomes.resolveEJBLink(source.getApplication(), module, link);
        } catch (EJBNotFoundException ex) {
            Tr.error(tc, "SINGLETON_DEPENDS_ON_NONEXISTENT_BEAN_CNTR0198E", new Object[] { component, module, link });

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "resolveEJBLink");
            throw new RuntimeWarning("CNTR0198E: The " + component + " singleton session bean in the " + module + " module depends on "
                    + link + ", which does not exist.", ex);
        } catch (AmbiguousEJBReferenceException ex) {
            Tr.error(tc, "SINGLETON_DEPENDS_ON_AMBIGUOUS_NAME_CNTR0199E", new Object[] { component, module, link });

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "resolveEJBLink");
            throw new RuntimeWarning("CNTR0199E: The " + component + " singleton session bean in the " + module + " module depends on "
                    + link + ", which does not uniquely specify an enterprise bean.", ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolveEJBLink: " + result);
        return result;
    }

    /**
     * Resolves and verifies the singleton dependency links.
     * 
     * @throws RuntimeWarning
     *             if verification fails
     */
    private void resolveDependencies() throws RuntimeWarning {
        // d684950
        if (EJSPlatformHelper.isZOSCRA()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "resolveDependencies: skipped in adjunct process");
            return;
        }

        // F743-20281 - If an exception happened during early dependency
        // resolution, then fail the application.
        if (ivSingletonDependencyResolutionException != null) {
            throw ivSingletonDependencyResolutionException;
        }

        // Beans currently on a dependency list. If A->B->C->A, then used={A}
        // then used={A,B} then used={A,B,C} then C->A is an error because A
        // is already in the set.
        Set<BeanMetaData> used = new HashSet<BeanMetaData>();

        // F7434950.CodRev - Create a copy of the key list since
        // resolveBeanDependencies will modify ivSingletonDependencies.
        for (BeanMetaData bmd : new ArrayList<BeanMetaData>(ivSingletonDependencies.keySet())) {
            used.add(bmd);
            resolveBeanDependencies(bmd, used);
            used.remove(bmd);
        }
    }

    /**
     * Verifies that the specified bean only depends on other singletons and that it does
     * not depend on itself. This method calls itself recursively to process all
     * dependencies.
     * 
     * @param bmd
     *            the bean to check
     * @param used
     *            the set of dependent beans that are already being processed
     * @param checked
     *            the set of beans that have already been processed
     * @throws RuntimeWarning
     *             if verification fails
     */
    private void resolveBeanDependencies(BeanMetaData bmd, Set<BeanMetaData> used) throws RuntimeWarning {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolveBeanDependencies: " + bmd.j2eeName);

        // F7434950.CodRev - If another bean depended on this bean, then
        // this bean's dependencies have already been resolved and verified.
        // F743-20281 - Alternatively, this bean's dependencies might have been
        // resolved early, in which case we don't need to resolve them again.
        if (bmd.ivDependsOn != null) {
            return;
        }
        bmd.ivDependsOn = new ArrayList<J2EEName>();

        Set<String> dependsOnLinks = ivSingletonDependencies.remove(bmd);
        if (dependsOnLinks == null) {
            return;
        }

        for (String dependencyLink : dependsOnLinks) {
            HomeRecord hr = resolveEJBLink(bmd.j2eeName, dependencyLink);
            BeanMetaData dependency = hr.getBeanMetaData();
            J2EEName dependencyName = dependency.j2eeName;

            if (!dependency.isSingletonSessionBean()) {
                Tr.error(
                        tc,
                        "SINGLETON_DEPENDS_ON_NON_SINGLETON_BEAN_CNTR0200E",
                        new Object[] { bmd.j2eeName.getComponent(), bmd.j2eeName.getModule(), dependencyName.getComponent(),
                                dependencyName.getModule() });
                throw new RuntimeWarning("CNTR0200E: The " + bmd.j2eeName.getComponent() + " singleton session bean in the "
                        + bmd.j2eeName.getModule() + " module depends on the " + dependencyName.getComponent() + " enterprise bean in the "
                        + dependencyName.getModule() + ", but the target is not a singleton session bean.");
            }

            if (!used.add(dependency)) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "circular dependency from " + dependencyName);

                Tr.error(tc, "SINGLETON_DEPENDS_ON_SELF_CNTR0201E",
                        new Object[] { dependencyName.getComponent(), dependencyName.getModule() });
                throw new RuntimeWarning("CNTR0201E: The " + dependencyName.getComponent() + " singleton session bean in the "
                        + dependencyName.getModule() + " module directly or indirectly depends on itself.");
            }

            bmd.ivDependsOn.add(dependencyName); // d588220
            resolveBeanDependencies(dependency, used);

            used.remove(dependency);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolveBeanDependencies: " + bmd.j2eeName);
    }

    /**
     * Resolves dependencies for a singleton bean. The {@link #checkIfEJBWorkAllowed} method
     * must have been called prior to calling this method.
     * 
     * @param bmd
     *            the bean to check
     * @return a list of dependencies, or null if the bean has no dependencies
     * @throws RuntimeWarning
     *             if dependencies are invalid
     */
    public List<J2EEName> resolveBeanDependencies(BeanMetaData bmd) throws RuntimeWarning { // F743-20281
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolveBeanDependencies: " + bmd.j2eeName);

        // This method might be called "early" prior to finishStarting.
        // Because checkIfEJBWorkAllowed must be called before this method, we
        // know that this is the startup thread in that case, which means we do
        // not need synchronization.
        //
        // As an known quirk, the ejb-link resolution process depends on
        // knowing the full set of beans in the application (not module) for
        // cross-module dependencies. Since it's possible that there are
        // remaining unstarted EJB modules, resolveEJBLink will make the wrong
        // decision in the following scenario:
        //
        // ejb1.jar: @Singleton public class A { }
        // ejb2.jar: @Singleton @DependsOn({"A"}) public class B { }
        // test.war: // ServletContextListener uses ejb2.jar#B
        // ejb3.jar: @Singleton public class A { }
        //
        // Normally, if ejb2.jar#B were accessed after the application is fully
        // started, the "A" dependency would fail as ambiguous. However, when
        // test.war is started, only the ejb1.jar#A bean is known, so the
        // dependency will be resolved.
        if (bmd.ivDependsOn == null && ivSingletonDependencies != null) {
            Set<BeanMetaData> used = new HashSet<BeanMetaData>();
            used.add(bmd);

            try {
                resolveBeanDependencies(bmd, used);
            } catch (RuntimeWarning ex) {
                // Save the exception to fail later in resolveDependencies
                // during finishStarting.
                ivSingletonDependencyResolutionException = ex;
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "resolveBeanDependencies: " + ex);
                throw ex;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolveBeanDependencies: " + bmd.ivDependsOn);
        return bmd.ivDependsOn;
    }

    /**
     * Creates all startup singleton beans.
     * 
     * @throws RuntimeWarning
     *             if a bean fails to initialize
     */
    private void createStartupBeans() throws RuntimeWarning {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // d684950
        if (EJSPlatformHelper.isZOSCRA()) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createStartupBeans: skipped in adjunct process");
            return;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createStartupBeans");

        for (int i = 0, size = ivStartupSingletonList.size(); i < size; i++) {
            J2EEName startupName = ivStartupSingletonList.get(i);

            try { // F743-1753CodRev
                EJSHome home = (EJSHome) EJSContainer.homeOfHomes.getHome(startupName);

                if (home == null) {
                    // The home won't exist if singleton beans aren't enabled
                    // in the runtime.
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Ignoring Singleton Startup bean: " + startupName);
                } else {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Creating instance for Singleton Startup bean: " + startupName.toString());

                    home.createSingletonBeanO();
                }
            } catch (Throwable t) {
                // A failure has occurred processing the Startup Singletons.
                // Post an FFDC, log the failure, and throw a RuntimWarning to
                // cause this application start to abort.
                FFDCFilter.processException(t, CLASS_NAME + ".createStartupBeans", "6921", this);

                Tr.error(tc, "STARTUP_SINGLETON_SESSION_BEAN_INITIALIZATION_FAILED_CNTR0190E", new Object[] { startupName.getComponent(),
                        startupName.getModule(), t });
                throw new RuntimeWarning("CNTR0201E: The " + startupName.getComponent() + " startup singleton session bean in the "
                        + startupName.getModule() + " module failed initialization.", t);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createStartupBeans");
    }

    /**
     * Notifies all application event listeners in the specified modules.
     * 
     * @param modules
     *            the list of modules to notify
     * @param started
     *            <tt>true</tt> if the application is started, or <tt>false</tt> if the
     *            application is stopping
     * @throws RuntimeWarning
     *             if any listener throws an exception
     */
    private void notifyApplicationEventListeners(Collection<EJBModuleMetaDataImpl> modules, boolean started) throws RuntimeWarning { // F743-26072
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "notifyApplicationEventListeners: started=" + started);

        RuntimeWarning warning = null;

        for (EJBModuleMetaDataImpl mmd : modules) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "notifying listeners in module: " + mmd.ivJ2EEName);

            List<EJBApplicationEventListener> listeners = mmd.ivApplicationEventListeners;
            if (listeners != null) {
                for (EJBApplicationEventListener listener : listeners) {
                    try {
                        if (started) {
                            listener.applicationStarted(ivName);
                        } else {
                            listener.applicationStopping(ivName);
                        }
                    } catch (Throwable t) {
                        FFDCFilter.processException(t, CLASS_NAME + ".notifyApplicationEventListeners", "781", this);
                        if (isTraceOn && tc.isEventEnabled())
                            Tr.event(tc, listener + " threw unexpected throwable: " + t, t);

                        // Save the first exception to be rethrown.
                        if (warning == null) {
                            warning = new RuntimeWarning(t);
                        }
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "notifyApplicationEventListeners: exception=" + warning);

        if (warning != null) {
            throw warning;
        }
    }

    /**
     * Starts non-persistent timers that were queued prior to the application being fully
     * started.
     */
    // F743-506
    private void startQueuedNonPersistentTimerAlarms() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (EJSPlatformHelper.isZOSCRA()) // d684950
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "startQueuedNonPersistentTimerAlarms: skipped in adjunct process");
            return;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "startQueuedNonPersistentTimers");

        for (TimerNpImpl timer : ivQueuedNonPersistentTimers) {
            timer.schedule(); // d589357
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "startQueuedNonPersistentTimers");
    }

    /**
     * Record the initialization attempt of a singleton. This method must not be called for
     * a bean before it is called for each of that bean's dependencies. This method may be
     * called multiple times for the same bean, but only the first call will have an effect.
     * 
     * @param home
     *            the bean's home
     * @throws EJBStoppedException
     *             if the application is stopping
     */
    public synchronized void addSingletonInitialization(EJSHome home) {
        if (ivStopping) {
            throw new EJBStoppedException(home.getJ2EEName().toString());
        }

        if (ivSingletonInitializations == null) {
            ivSingletonInitializations = new LinkedHashSet<EJSHome>();
        }
        ivSingletonInitializations.add(home);
    }

    /**
     * Queues or starts a non-persistent timer alarm, returning an indication of whether or
     * not the operation was successful.
     * 
     * If the application has not yet started, then the timer is queued and its alarm will
     * be started after the application is fully started.
     * 
     * If the application has previously been started, but now we are late-adding a new
     * module, the timer we are trying to start is from that new module, then the timer is
     * queued and its alarm will be started after the module is fully started.
     * 
     * If everything in the application is already running, then the timer alarm is started
     * immediately.
     * 
     * If we are in the late-add flow, and the timer is not from the module we are in the
     * process of adding (ie, the module the timer is from has already been started), then
     * the timer alarm is started immediately.
     * 
     * Otherwise, the request is ignored because the application is stopping or stopped, and
     * all non-persistent timers for the application have already been removed. False is
     * returned.
     * 
     * @return true if the timer was successfully queued or started; otherwise false.
     */
    public synchronized boolean queueOrStartNonPersistentTimerAlarm(TimerNpImpl timer, EJBModuleMetaDataImpl module) {
        if (ivStopping) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "not starting timer alarm after application stop: " + timer);
            return false;
        } else {
            if (isStarted()) {
                timer.schedule();
            } else if (ivModuleBeingAddedLate != null && ivModuleBeingAddedLate != module) // d607800.CodRv
            {
                timer.schedule();
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "queueing timer alarm until full application start: " + timer);

                if (ivQueuedNonPersistentTimers == null) {
                    ivQueuedNonPersistentTimers = new ArrayList<TimerNpImpl>();
                }
                ivQueuedNonPersistentTimers.add(timer);
            }
        }
        return true;
    }

    /**
     * Notification that the application or a module within the application will begin
     * stopping stopping. This method should never throw an exception.
     * 
     * @param application
     *            <tt>true</tt> if the application is stopping
     * @param j2eeName
     *            the J2EEName of the application or module, or <tt>null</tt> if application
     *            is true and J2EEName is unavailable
     * @param modules
     *            the list of modules being stopped
     */
    private void beginStopping(boolean application, J2EEName j2eeName, Collection<EJBModuleMetaDataImpl> modules) { // F743-26072
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "beginStopping: application=" + application + ", " + j2eeName);

        synchronized (this) {
            if (application) { // F743-26072
                ivStopping = true;
            }

            // F743-15941 - If the application or fine-grained module start
            // failed, then we never transitioned to "fully started". Unblock
            // any threads that are waiting in checkIfEJBWorkAllowed.
            //
            // This call is within the synchronized block only to reduce
            // monitor acquisition/release.
            unblockThreadsWaitingForStart();
        }

        if (j2eeName != null) {
            // d589357 - Cancel non-persistent timers. We do not allow new
            // non-persistent timers to be created after the application or
            // module has begun stopping (e.g., during PreDestroy), so we also
            // remove all existing timers before calling PreDestroy.
            ivContainer.getEJBRuntime().removeTimers(j2eeName); // F743-26072
        }

        if (ivSingletonInitializations != null) {
            List<EJSHome> reverse = new ArrayList<EJSHome>(ivSingletonInitializations);
            for (int i = reverse.size(); --i >= 0;) {
                EJSHome home = reverse.get(i);
                J2EEName homeJ2EEName = home.getJ2EEName();

                if (application || (j2eeName.getModule().equals(homeJ2EEName.getModule()))) {
                    home.destroy();
                    ivSingletonInitializations.remove(home);
                }
            }
        }

        try {
            notifyApplicationEventListeners(modules, false); // F743-26072
        } catch (RuntimeWarning rw) {
            FFDCFilter.processException(rw, CLASS_NAME + ".stopping", "977", this);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "beginStopping");
    }

    /**
     * Notification that a module within this application will begin stopping. This method
     * should never throw an exception.
     * 
     * @param mmd
     *            the module
     */
    public void stoppingModule(EJBModuleMetaDataImpl mmd) { // F743-26072
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "stoppingModule: " + mmd.getJ2EEName());

        // If the application is stopping (rather than a single module), then
        // we don't need to do anything special.
        if (!ivStopping) {
            mmd.ivStopping = true;
            ivModules.remove(mmd);

            try {
                beginStopping(false, mmd.getJ2EEName(), Collections.singletonList(mmd));
            } finally {
                // If fine-grained module start failed, be sure to clear
                // internal state.
                ivSingletonDependencies = null;
                ivStartupSingletonList = null;
                ivQueuedNonPersistentTimers = null;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "stoppingModule");
    }

    /**
     * Notification that the application will begin stopping.
     */
    public void stopping() {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "stopping");

        J2EEName j2eeName = ivApplicationMetaData == null ? null : ivApplicationMetaData.getJ2EEName();
        beginStopping(true, j2eeName, ivModules); // F743-26072
    }

    /**
     * //d607800.CodRv
     * 
     * Indicates if the application is currently completed started, or not.
     * 
     * If any part of the application is actively being started, the the ivStartupThread
     * switch is non-null to indicate this.
     * 
     * Note that when an ejb module is added late (ie, after the application has already
     * been started), the ivStartupThread switch will stay null until the module-starting
     * event is received from the runtime. This actually occurs after the process of adding
     * the module has begun.
     * 
     */
    public boolean isStarted() {
        return ivStartupThread == null;
    }

    /**
     * Returns true if the application is being stopped or has stopped.
     */
    // d660700
    public boolean isStopping() {
        return ivStopping;
    }

    /**
     * Verifies that all versioned modules in an application use the same application base
     * name, and unique module base names.
     * <p>
     * 
     * An IllegalArgumentException is thrown if both criteria are not met.
     */
    // F54184.1 F54184.2
    void validateVersionedModuleBaseName(String appBaseName, String modBaseName) {
        for (EJBModuleMetaDataImpl ejbMMD : ivModules) {
            String versionedAppBaseName = ejbMMD.ivVersionedAppBaseName;
            if (versionedAppBaseName != null && !versionedAppBaseName.equals(appBaseName)) {
                throw new IllegalArgumentException("appBaseName (" + appBaseName + ") does not equal previously set value : "
                        + versionedAppBaseName);
            }

            if (modBaseName.equals(ejbMMD.ivVersionedModuleBaseName)) {
                throw new IllegalArgumentException("duplicate baseName : " + modBaseName);
            }
        }
    }
}
