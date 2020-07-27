/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import static com.ibm.ejs.container.ContainerConfigConstants.allowCachedTimerDataFor;
import static com.ibm.ejs.container.ContainerConfigConstants.allowCustomFinderSQLForUpdate;
import static com.ibm.ejs.container.ContainerConfigConstants.allowEarlyInsert;
import static com.ibm.ejs.container.ContainerConfigConstants.allowPrimaryKeyMutation;
import static com.ibm.ejs.container.ContainerConfigConstants.blockWorkUntilAppStarted;
import static com.ibm.ejs.container.ContainerConfigConstants.blockWorkUntilAppStartedWaitTime;
import static com.ibm.ejs.container.ContainerConfigConstants.checkAppConfigProp;
import static com.ibm.ejs.container.ContainerConfigConstants.createInstanceAtStartup;
import static com.ibm.ejs.container.ContainerConfigConstants.defaultSessionAccessTimeout;
import static com.ibm.ejs.container.ContainerConfigConstants.defaultStatefulSessionTimeout;
import static com.ibm.ejs.container.ContainerConfigConstants.disableAsyncMethods;
import static com.ibm.ejs.container.ContainerConfigConstants.disableAutomaticLightweightMethods;
import static com.ibm.ejs.container.ContainerConfigConstants.disableMDBs;
import static com.ibm.ejs.container.ContainerConfigConstants.disablePersistentTimers;
import static com.ibm.ejs.container.ContainerConfigConstants.disableRemote;
import static com.ibm.ejs.container.ContainerConfigConstants.disableShortDefaultBindings;
import static com.ibm.ejs.container.ContainerConfigConstants.disableTimers;
import static com.ibm.ejs.container.ContainerConfigConstants.ee5Compatibility;
import static com.ibm.ejs.container.ContainerConfigConstants.ee6Compatibility;
import static com.ibm.ejs.container.ContainerConfigConstants.emptyAnnotationIgnoresExplicitInterfaces;
import static com.ibm.ejs.container.ContainerConfigConstants.excludeNestedExceptions;
import static com.ibm.ejs.container.ContainerConfigConstants.expandCMPCFJNDIName;
import static com.ibm.ejs.container.ContainerConfigConstants.extendSetRollbackOnlyBehaviorBeyondInstanceFor;
import static com.ibm.ejs.container.ContainerConfigConstants.fbpkReadOnlyProp;
import static com.ibm.ejs.container.ContainerConfigConstants.indirectLocalProxies;
import static com.ibm.ejs.container.ContainerConfigConstants.initializeEJBsAtStartup;
import static com.ibm.ejs.container.ContainerConfigConstants.limitSetRollbackOnlyBehaviorToInstanceFor;
import static com.ibm.ejs.container.ContainerConfigConstants.maxAsyncResultWaitTime;
import static com.ibm.ejs.container.ContainerConfigConstants.maxUnclaimedAsyncResults;
import static com.ibm.ejs.container.ContainerConfigConstants.noEJBPool;
import static com.ibm.ejs.container.ContainerConfigConstants.noPrimaryKeyMutation;
import static com.ibm.ejs.container.ContainerConfigConstants.passivationPolicy;
import static com.ibm.ejs.container.ContainerConfigConstants.persistentTimerSingletonDeadlockTimeout;
import static com.ibm.ejs.container.ContainerConfigConstants.poolSizeSpecProp;
import static com.ibm.ejs.container.ContainerConfigConstants.portableFinderProp;
import static com.ibm.ejs.container.ContainerConfigConstants.portableProp;
import static com.ibm.ejs.container.ContainerConfigConstants.strictMaxCacheSize;
import static com.ibm.ejs.container.ContainerConfigConstants.timerCancelTimeout;
import static com.ibm.ejs.container.ContainerConfigConstants.timerQOSAtLeastOnceForRequired;
import static com.ibm.ejs.container.ContainerConfigConstants.useFairSingletonLockingPolicy;
import static com.ibm.ejs.container.ContainerConfigConstants.validateMergedXML;
import static com.ibm.ejs.container.ContainerConfigConstants.wlmAllowOptionAReadOnlyProp;

import java.util.ArrayList;

import com.ibm.ejs.container.util.DeploymentUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.diagnostics.IntrospectionWriter;
import com.ibm.wsspi.ejbcontainer.JITDeploy;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

/**
 * ContainerProperties provides access to all System properties used to
 * control EJB Container behavior. <p>
 *
 * This 'singleton' class provides a location to cache all of the
 * EJB Container specific system properties, as well as a mechanism
 * for invoking 'getProperty' without using a privileged action. <p>
 *
 * The EJB Container service will access this class during server start,
 * to insure the getProperty calls are made with system authority,
 * and hold a reference to it, to insure it is not garbage collected. <p>
 *
 * See {@link ContainerConfigConstants} for details about each constant,
 * including default values. <p>
 **/
public final class ContainerProperties {
    private static final TraceComponent tc = Tr.register(ContainerProperties.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * Property that indicates the customer is willing to accept potentially stale
     * Timer data.
     **/
    public static final String AllowCachedTimerDataFor; //F001419

    /**
     * Property that allows the user to have the EJB Container dynamically
     * extend the SQL and semantics for customer finders to support
     * FOR UPDATE requirements.
     **/
    public static final String AllowCustomFinderSQLForUpdate;

    /**
     * Property that allows the user to force the container to insert record
     * during ejbCreate.
     **/
    public static final boolean AllowEarlyInsert;

    /**
     * Property that indicates the customer is allowed to mutate the
     * primary key when using 'noLocalCopies'.
     **/
    public static final boolean AllowPrimaryKeyMutation;

    /**
     * Constant that allows a specific spec violation when a down stream EJB method
     * causes a Tx to be rolled back which was begun by an up stream EJB method.
     * The EJB spec states that on a Tx rollback, a RemoteException can only be
     * returned if the transaction was rolled back by the method which started
     * the Tx (thus allowing us to reap the benefits of {@link #IncludeNestedExceptions}).
     * Customers have never been too happy about that restriction. This property
     * will allow a user to break the spec restriction and allow a RemoteException
     * to be returned no matter which bean method caused the Tx to be
     * rolled back. As explained in the javadoc for {@link ContainerConfigConstants#includeNestedExceptions}, nested exceptions
     * can only be returned to a client when they can be nested in a RemoteException
     * as opposed to a TransactionRolledbackException.
     *
     * @see ContainerConfigConstants#includeNestedExceptions
     **/
    private static final int ALLOW_SPEC_VIOLATION_ON_ROLLBACK = 4; //PK87857

    /**
     * Property used to store the 'bitwise and' of the value set by the user
     * in {@link ContainerConfigConstants#includeNestedExceptions} and the {@link #ALLOW_SPEC_VIOLATION_ON_ROLLBACK} constant
     **/
    public static final boolean AllowSpecViolationOnRollback; //PK87857

    /**
     * The number of milliseconds to "back off" the actual wait time of a retry
     * for a RemoteAsyncResultExtended.waitForResult.
     */
    public static final long AsyncNoResponseBackoff = ClientAsyncResult.AsyncResultNoResponseBackoff; // F16043

    /**
     * Property that indicates whether or not EJBs should be bound to the server
     * root namespace.
     */
    public static boolean BindToServerRoot;

    /**
     * Property that indicates whether or not EJBs should be bound to contexts in
     * the "java:" namespace.
     */
    public static boolean BindToJavaGlobal;

    /**
     * Property that allows the user to specify whether external requests
     * should be blocked until an application is fully started.
     */
    public static final boolean BlockWorkUntilAppStarted; // F743-15941

    /**
     * Property that allows the user to specify how long external requests
     * should be blocked while waiting for an application to fully start.
     */
    public static final long BlockWorkUntilAppStartedWaitTime; // F743-15941

    /**
     * Property that determines whether the container should validate
     * the meta data in the EJB module and subsequently report any
     * problems. By default, this setting is false. This setting
     * will be overridden when EJBContainer=all tracing is enabled.
     */
    public static final boolean CheckAppConfig; //F743-13921

    /**
     * Property that allows the user to prevent a bean class instance from being
     * created at EJB start.
     */
    public static final boolean CreateInstanceAtStart; // PI23717

    /**
     * Property that allows the user to indicate whether or not declared
     * RemoteExceptions are application exceptions or system exceptions.
     */
    public static final boolean DeclaredRemoteAreApplicationExceptions; // d660332

    /**
     * Property that allows the user to indicate whether or not declared
     * unchecked exceptions are system exceptions or application exceptions.
     */
    public static final boolean DeclaredUncheckedAreSystemExceptions; // d660332

    /**
     * Property that allows the user to specify (server wide) the default session concurrency
     * access timeout value. The value is specified in milliseconds.
     */
    public static final long DefaultSessionAccessTimeout;

    /**
     * Property that allows the user to specify (server wide) the stateful session bean timeout value,
     * The value is specified in minutes by the customer, but saved internally as milliseconds.
     */
    public static final long DefaultStatefulSessionTimeout; // F743-14726  d627044

    /**
     * Property that allows the user to indicate whether or not asynchronous
     * methods should be disabled.
     */
    public static final boolean DisableAsyncMethods; // F743-13022

    /**
     * Property that allows the user to indicate whether or not remote
     * asynchronous Future.get calls should use retries to prevent timeouts.
     */
    public static final boolean DisableAsyncResultRetries = ClientAsyncResult.DisableAsyncResultRetries; // F16043

    /**
     * Property that allows the user to indicate that the container should not
     * automatically enable lightweight mode for trivial method implementations.
     */
    public static final boolean DisableAutomaticLightweightMethods; // F61004.1

    /**
     * Property that allows the user to indicate whether or not MDBs should be
     * disabled.
     */
    public static final boolean DisableMDBs; // F743-13023

    /**
     * Property that allows the user to indicate whether or not remote
     * interfaces should be disabled.
     */
    public static final boolean DisableRemote; // F743-13024

    /**
     * Property that allows the user to selectively or non-selectively disable
     * simple interface bindings. <p>
     *
     * null means no property specified.
     * !null and length = 0 means all applications are to have simple interface
     * bindings disabled
     * !null and length > 0 means only appNames in this array list are to have
     * simple interface bindings disabled
     **/
    public static ArrayList<String> DisableShortDefaultBindings; // d444470
    public static ArrayList<String> DisableShortDefaultBindingsFromJVM;

    /**
     * Property that allows the user to indicate whether or not EJB timers
     * should be disabled.
     */
    public static final boolean DisableTimers; // F743-13022

    /**
     * Property that allows the user to indicate whether or not EJB persistent timers
     * should be disabled.
     */
    public static final boolean DisablePersistentTimers; // PI50798

    /**
     * Property that indicates whether or not container behavior should be
     * compatible with EE5 (EJB 3) rather than later releases (EJB 3.1+). This
     * property should only be checked for those cases where the EE
     * specifications have mandated a change in behavior between releases.
     */
    public static final boolean EE5Compatibility; // F743-14982CdRv

    /**
     * Property that indicates whether or not container behavior should be
     * compatible with EE6 (EJB 3.1) rather than later releases (EJB 3.2+). This
     * property should only be checked for those cases where the EE
     * specifications have mandated a change in behavior between releases.
     */
    private static final boolean EE6Compatibility;

    /**
     * Property that indicates whether or not explicitly configured local/remote
     * business interfaces and no-interface view should be ignored when
     * determining whether or not an empty Local/Remote annotation should cause a
     * single interface on the implements clause to be considered a business
     * interface.
     */
    public static final boolean EmptyAnnotationIgnoresExplicitInterfaces;

    /**
     * Property that indicates that the 'root' exception should be excluded
     * when a transaction is rolled back during commit processing.
     */
    public static final boolean ExcludeNestedExceptions;

    /**
     * Property that allows for expansion of a CMP Connection Factory JNDI name.
     **/
    public static final boolean ExpandCMPCFJNDIName; //d425164

    /**
     * Property that allows the user to specify application names
     * that they would like to have the 2.x EJBs have
     * the same SetRollbackOnly behavior as 3.x EJBs.
     * Beginning with EJB 3.0 (CTS) it is required that the
     * bean method return normally regardless of how the tx was
     * marked for rollback, so don't throw rollback exception, instead
     * return the application exception or return normally if no application
     * exception was caught.
     **/
    //d461917.1
    public static final ArrayList<String> ExtendSetRollbackOnlyBehaviorBeyondInstanceFor;

    /**
     * Property used to allow the customer to define the default behavior
     * for all FindByPrimaryKey methods to be READ-ONLY.
     **/
    public static final boolean FbpkAlwaysReadOnly;

    /**
     * Property used to allow customers to ignore duplicate elements in their
     * EJB bindings files.
     */
    public static boolean IgnoreDuplicateEJBBindings; // PM51230

    /**
     * Constant that indicates RemoteException should be used when possible
     * on rollback, so that nested exceptions may be included.
     **/
    private static final int INCLUDE_NESTED_EXCEPTIONS = 1; //PK87857

    /**
     * Property used to store the 'bitwise and' of the value set by the user
     * in {@link ContainerConfigConstants#includeNestedExceptions} and the {@link #INCLUDE_NESTED_EXCEPTIONS} constant
     **/
    public static final boolean IncludeNestedExceptions;

    /**
     * Constant that indicates that an exception should be nested when
     * a Tx is rolled back. This is very similar to the property {@link #IncludeNestedExceptions}. However, the 'IncludeNestedExceptions'
     * missed some scenarios. For 'backwards compatibility' we have
     * to leave the default functionality of 'IncludeNestedExceptions' as
     * is, as such, this property will indicate whether or not we can "extend"
     * the existing 'IncludeNestedExceptions'.
     *
     * @see ContainerConfigConstants#includeNestedExceptions
     **/
    private static final int INCLUDE_NESTED_EXCEPTIONS_EXTENDED = 2; //PK87857

    /**
     * Property used to store the 'bitwise and' of the value set by the user
     * in {@link ContainerConfigConstants#includeNestedExceptions} and the {@link #INCLUDE_NESTED_EXCEPTIONS_EXTENDED} constant
     **/
    public static final boolean IncludeNestedExceptionsExtended; //PK87857

    /**
     * Property that allows the user to specify that wrappers for local client
     * views should be proxied.
     */
    public static final boolean IndirectLocalProxies; // F58064

    /**
     * Property that allows the user to not defer the initialization of any EJBs.
     **/
    public static final String InitializeEJBsAtStartup;

    /**
     * Property that allows the user to specify application names
     * that they would like to have the 3.x EJBs have
     * the same SetRollbackOnly behavior as 2.x EJBs.
     * Beginning with EJB 3.0 (CTS) it is required that the
     * bean method return normally regardless of how the tx was
     * marked for rollback, so don't throw rollback exception, instead
     * return the application exception or return normally if no application
     * exception was caught. This property allows the behavior for 3.x EJBs to
     * have the same behavior as 2.x EJBs.
     **/
    //d461917.1
    public static final ArrayList<String> LimitSetRollbackOnlyBehaviorToInstanceFor;

    /**
     * Line separator characters to be used throughout EJB Container.
     **/
    public static final String LineSeparator;

    /**
     * The maximum number of milliseconds the server will wait for an
     * asynchronous result to become available before asking the client to
     * retry.
     */
    public static final long MaxAsyncResultWaitTime; // F16043

    /**
     * The maximum number of outstanding async results, or a number less than 1
     * to indicate that no limit should be enforced.
     */
    public static final int MaxUnclaimedAsyncResults; // d690014.1

    /**
     * Property that allows the customer to disable all EJB pooling.
     **/
    public static final boolean NoEJBPool;

    /**
     * Property that allows the user to specify that their code will
     * not do any mutation of the primary key object when using local
     * interfaces.
     **/
    public static final boolean NoPrimaryKeyMutation;

    /**
     * Property that allows the user to specify which Stateful Session
     * passivation policy should be used on zOS.
     **/
    public static final String PassivationPolicy;

    /**
     * Property that allows the user to specify the deadlock timeout for persistent
     * timers associated with singleton beans.
     */
    public static final long PersistentTimerSingletonDeadlockTimeout;

    /**
     * Property used to specify the min. and max. pool sizes for the
     * EJB Container. <p>
     **/
    public static final String PoolSize;

    /**
     * Property that allows the user to indicate whether to use the classes
     * found in ejbportable.jar or to use the legacy classes (the ones that
     * existed prior to release 5).
     **/
    public static final boolean Portable;

    /**
     * Property that allows the user to indicate whether to use the classes
     * found in ejbportable.jar or to use the legacy classes (the ones that
     * existed prior to release 5) for finder methods (collections).
     **/
    public static final String PortableFinder;

    /**
     * Property that allows the user to specify the level of rmic compatibility
     * for JIT-generated stubs and ties.
     */
    public static final int RMICCompatible = JITDeploy.RMICCompatible; // PM46698

    /**
     * Property that allows the user to specify that the EJB max cache size
     * should be strictly enforced.
     */
    public static final boolean StrictMaxCacheSize;

    /**
     * The maximum time, in milliseconds, for the Timer.cancel method to retry
     * the cancel operation when the time is concurrently running.
     */
    public static final long TimerCancelTimeout;

    /**
     * True if timer callback methods with a Required transaction attribute
     * should use the scheduler QOS_ATLEASTONCE option.
     */
    public static final boolean TimerQOSAtLeastOnceForRequired; // RTC116312

    /**
     * Property that allows the user to revert the way EJB stubs are generated
     * for EJB 3.x API beans to exhibit earlier behavior where a RemoteException
     * may be thrown even though RemoteException is not on the throws clause.
     */
    public static final boolean ThrowRemoteFromEjb3Stub = JITDeploy.ThrowRemoteFromEjb3Stub;

    /**
     * Property that allows the user to specify (server wide) the singleton
     * beans to use fair locking policy.
     * The value is specified as true or false (default false).
     */
    public static final boolean UseFairSingletonLockingPolicy; // F743-9002

    /**
     * User install root (profile) location for logging.
     **/
    public static final String UserInstallRoot;

    /**
     * Validation of merged XML (from AMM) has been enabled and any mismatches
     * should be logged, unless ValidateMergedXMLFail is also true.
     */
    public static final boolean ValidateMergedXML;

    /**
     * Validation of merged XML (from AMM) has been enabled and has been
     * configured to result in a failure to start any bean in error.
     */
    public static final boolean ValidateMergedXMLFail;

    /**
     * Property that allows the user to allows the container to use Option A
     * caching in a workload managed environment.
     **/
    public static final boolean WLMAllowOptionAReadOnly;

    /**
     * Property that determines the action to take in response to binding configuration errors.
     * WARN = Issue a warning for incorrect configuration.
     * FAIL = Fail application start when incorrect configuration is encountered.
     * IGNORE = Ignore incorrect configuration.
     */
    public static OnError customBindingsOnErr;

    /**
     * Temporary property to gate custom bindings behind until feature is out of beta
     */
    public static boolean customBindingsEnabledBeta;

    /**
     * Static constructor that will initialize all of the 'constants' based
     * on the corresponding system property. <p>
     *
     * See {@link ContainerConfigConstants} for details about each constant,
     * including default values. <p>
     **/
    static {
        AllowCachedTimerDataFor = System.getProperty(allowCachedTimerDataFor); //F001419

        AllowCustomFinderSQLForUpdate = System.getProperty(allowCustomFinderSQLForUpdate);

        AllowEarlyInsert = System.getProperty(allowEarlyInsert, "false").equalsIgnoreCase("true");

        AllowPrimaryKeyMutation = System.getProperty(allowPrimaryKeyMutation, "false").equalsIgnoreCase("true");

        CheckAppConfig = System.getProperty(checkAppConfigProp, "false").equalsIgnoreCase("true"); //F743-13921

        CreateInstanceAtStart = System.getProperty(createInstanceAtStartup, "true").equalsIgnoreCase("true");

        DeclaredRemoteAreApplicationExceptions = DeploymentUtil.DeclaredRemoteAreApplicationExceptions; // RTC116527

        DeclaredUncheckedAreSystemExceptions = DeploymentUtil.DeclaredUncheckedAreSystemExceptions; // d660332

        // The default session concurrency access timeout is -1 (forever), but that
        // may be overridden with any value greater than -1 (milliseconds).  d704504
        long accessTimeout = Long.getLong(defaultSessionAccessTimeout, -1);
        DefaultSessionAccessTimeout = (accessTimeout >= -1) ? accessTimeout : -1;

        // F743-14726
        // Stateful session bean timeout value of 0 disables use of the system property.
        // If specified a positive integer use the system property, otherwise use the default value of 10 minutes.
        // Stateful session bean timeout property is specified in minutes, but stored and used internally as milliseconds
        long tmpStatefulSessionTimeout = 10;
        boolean caughtNFE = false;
        try {
            tmpStatefulSessionTimeout = Long.getLong(defaultStatefulSessionTimeout, 10); // d627044
            if (tmpStatefulSessionTimeout == 0) {
                tmpStatefulSessionTimeout = 10;
            } else if (tmpStatefulSessionTimeout < 0) {
                caughtNFE = true;
            }
        } catch (NumberFormatException nfe) {
            caughtNFE = true;
        }
        if (caughtNFE) {
            Object[] parms = new Object[] { defaultStatefulSessionTimeout, tmpStatefulSessionTimeout, "10" }; // d627044
            Tr.warning(tc, "INVALID_STATEFUL_TIMEOUT_TIMEOUT_CNTR0313W", parms);
            tmpStatefulSessionTimeout = 10;
        }
        tmpStatefulSessionTimeout = tmpStatefulSessionTimeout * 60 * 1000;
        DefaultStatefulSessionTimeout = tmpStatefulSessionTimeout; // d627044

        DisableAsyncMethods = System.getProperty(disableAsyncMethods, "false").equalsIgnoreCase("true"); // F743-13022

        DisableAutomaticLightweightMethods = System.getProperty(disableAutomaticLightweightMethods, "false").equalsIgnoreCase("true"); // F61004.1

        DisableMDBs = System.getProperty(disableMDBs, "false").equalsIgnoreCase("true"); // F743-13023

        DisableRemote = System.getProperty(disableRemote, "false").equalsIgnoreCase("true"); // F743-13024

        DisableTimers = System.getProperty(disableTimers, "false").equalsIgnoreCase("true"); // F743-13022

        DisablePersistentTimers = System.getProperty(disablePersistentTimers, "false").equalsIgnoreCase("true"); // PI50798

        EE5Compatibility = Boolean.getBoolean(ee5Compatibility); // F743-14982CdRv

        EE6Compatibility = Boolean.getBoolean(ee6Compatibility); // RTC113949

        String emptyAnnotationIgnoresExplicitInterfacesString = System.getProperty(emptyAnnotationIgnoresExplicitInterfaces);
        EmptyAnnotationIgnoresExplicitInterfaces = emptyAnnotationIgnoresExplicitInterfacesString != null ? Boolean.parseBoolean(emptyAnnotationIgnoresExplicitInterfacesString) : EE6Compatibility; // RTC113949

        ExcludeNestedExceptions = Boolean.getBoolean(excludeNestedExceptions); // d672063

        //d425164: For WAS 7.0+ we should expand the CMP Connection Factory JNDI Name by default.  This is the opposite
        //of previous releases where we did NOT expand this name by default.
        ExpandCMPCFJNDIName = System.getProperty(expandCMPCFJNDIName, "true").equalsIgnoreCase("true"); //d425164

        FbpkAlwaysReadOnly = System.getProperty(fbpkReadOnlyProp, "false").equalsIgnoreCase("true");

        //PK87857 start
        String tmp = System.getProperty(ContainerConfigConstants.includeNestedExceptions);
        int op = 0;
        if (tmp != null) {
            if ("true".equalsIgnoreCase(tmp)) {//pre-PK87857 value
                op = INCLUDE_NESTED_EXCEPTIONS;
            } else {
                try {
                    op = Integer.parseInt(tmp);
                } catch (NumberFormatException e) {
                    op = 0;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught NumberFormatException: " + e);
                    }
                }
            }
        }

        IncludeNestedExceptionsExtended = (op & INCLUDE_NESTED_EXCEPTIONS_EXTENDED) > 0;
        AllowSpecViolationOnRollback = (op & ALLOW_SPEC_VIOLATION_ON_ROLLBACK) > 0;
        //Enable IncludeNestedExceptions if 'IncludeNestedExceptionsExtended' is enabled
        IncludeNestedExceptions = ((op & INCLUDE_NESTED_EXCEPTIONS) > 0) || IncludeNestedExceptionsExtended;
        //PK87857 end

        IndirectLocalProxies = Boolean.getBoolean(indirectLocalProxies); // F58064

        InitializeEJBsAtStartup = System.getProperty(initializeEJBsAtStartup);

        LineSeparator = System.getProperty("line.separator", "\n");

        // The default client-side ORB timeout is 3 minutes, so wait slightly
        // less than that with the hope that we can write a "retry" response
        // before the client times out.
        MaxAsyncResultWaitTime = Integer.getInteger(maxAsyncResultWaitTime, 3 * 60 - 30) * 1000; // F16043

        MaxUnclaimedAsyncResults = Integer.getInteger(maxUnclaimedAsyncResults, 1000); // d690014.1

        NoEJBPool = System.getProperty(noEJBPool, "false").equalsIgnoreCase("true");

        NoPrimaryKeyMutation = System.getProperty(noPrimaryKeyMutation, "false").equalsIgnoreCase("true");

        PassivationPolicy = System.getProperty(passivationPolicy);

        PersistentTimerSingletonDeadlockTimeout = Integer.getInteger(persistentTimerSingletonDeadlockTimeout, 10 * 1000);

        PoolSize = System.getProperty(poolSizeSpecProp);

        Portable = System.getProperty(portableProp, "true").equalsIgnoreCase("true");

        PortableFinder = System.getProperty(portableFinderProp);

        StrictMaxCacheSize = Boolean.getBoolean(strictMaxCacheSize);

        TimerCancelTimeout = Integer.getInteger(timerCancelTimeout, 60) * 1000; // d703086

        TimerQOSAtLeastOnceForRequired = Boolean.getBoolean(timerQOSAtLeastOnceForRequired); // RTC116312

        // F743-9002
        // Added UseFairSingletonLockingPolicy.
        UseFairSingletonLockingPolicy = Boolean.getBoolean(useFairSingletonLockingPolicy);

        BlockWorkUntilAppStarted = Boolean.getBoolean(blockWorkUntilAppStarted); // F743-15941
        BlockWorkUntilAppStartedWaitTime = Integer.getInteger(blockWorkUntilAppStartedWaitTime, 2 * 60) * 1000; // F743-15941

        // Added UserInstallRoot                                           d457128
        String installRoot = System.getProperty("user.install.root");
        if (installRoot == null)
            installRoot = System.getProperty("was.install.root", "");
        UserInstallRoot = installRoot;

        // Added ValidateMergedXML support                                 d680497
        String validateMergedXMLValue = System.getProperty(validateMergedXML);
        ValidateMergedXMLFail = "fail".equals(validateMergedXMLValue);
        ValidateMergedXML = ValidateMergedXMLFail | "log".equals(validateMergedXMLValue);

        WLMAllowOptionAReadOnly = System.getProperty(wlmAllowOptionAReadOnlyProp, "false").equalsIgnoreCase("true");

        String simpleIntBindingsProperty = System.getProperty(disableShortDefaultBindings);
        if (simpleIntBindingsProperty != null) {
            DisableShortDefaultBindingsFromJVM = new ArrayList<String>();
            if (!simpleIntBindingsProperty.equalsIgnoreCase("*")) {
                String[] apps = simpleIntBindingsProperty.split(":");
                for (int i = 0; i < apps.length; i++) {
                    DisableShortDefaultBindingsFromJVM.add(apps[i]);
                }
            }
        } else {
            DisableShortDefaultBindingsFromJVM = null;
        }

        String extendSetRollbackOnlyProperty = System.getProperty(extendSetRollbackOnlyBehaviorBeyondInstanceFor); //d461917.1
        if (extendSetRollbackOnlyProperty != null) {
            ExtendSetRollbackOnlyBehaviorBeyondInstanceFor = new ArrayList<String>();
            String[] apps = extendSetRollbackOnlyProperty.split(":");
            for (int i = 0; i < apps.length; i++) {
                ExtendSetRollbackOnlyBehaviorBeyondInstanceFor.add(apps[i]);
            }
        } else {
            ExtendSetRollbackOnlyBehaviorBeyondInstanceFor = null;
        }

        String limitSetRollbackOnlyProperty = System.getProperty(limitSetRollbackOnlyBehaviorToInstanceFor); //d461917.1
        if (limitSetRollbackOnlyProperty != null) {
            LimitSetRollbackOnlyBehaviorToInstanceFor = new ArrayList<String>();
            String[] apps = limitSetRollbackOnlyProperty.split(":");
            for (int i = 0; i < apps.length; i++) {
                LimitSetRollbackOnlyBehaviorToInstanceFor.add(apps[i]);
            }
        } else {
            LimitSetRollbackOnlyBehaviorToInstanceFor = null;
        }
    }

    /**
     * Writes the important state data of this class, in a readable format,
     * to the specified output writer. <p>
     *
     * @param writer output resource for the introspection data
     */
    public static void introspect(IntrospectionWriter writer) {
        writer.begin("Container Properties");
        writer.println("Property: AllowCachedTimerDataFor = " + AllowCachedTimerDataFor);//F001419
        writer.println("Property: AllowCustomFinderSQLForUpdate = " + AllowCustomFinderSQLForUpdate);
        writer.println("Property: AllowEarlyInsert        = " + AllowEarlyInsert);
        writer.println("Property: AllowPrimaryKeyMutation = " + AllowPrimaryKeyMutation);
        writer.println("Property: AllowSpecViolationOnRollback = " + AllowSpecViolationOnRollback); //PK87857
        writer.println("Property: AsyncResultNoResponseBackoff = " + AsyncNoResponseBackoff);
        writer.println("Property: BindToJavaGlobal        = " + BindToJavaGlobal);
        writer.println("Property: BindToServerRoot        = " + BindToServerRoot);
        writer.println("Property: BlockWorkUntilAppStarted = " + BlockWorkUntilAppStarted); // F743-15941
        writer.println("Property: BlockWorkUntilAppStartedWaitTime = " + BlockWorkUntilAppStartedWaitTime); // F743-15941
        writer.println("Property: CheckAppConfig          = " + CheckAppConfig); // F743-13921
        writer.println("Property: CreateInstanceAtStart   = " + CreateInstanceAtStart);
        writer.println("Property: CustomBindingsOnError   = " + customBindingsOnErr);
        writer.println("Property: DefaultSessionAccessTimeout = " + DefaultSessionAccessTimeout); // d704504
        writer.println("Property: DefaultStatefulSessionTimeout  = " + DefaultStatefulSessionTimeout); // F743-14726  d627044
        writer.println("Property: DisableAsyncMethods     = " + DisableAsyncMethods); // F743-13022
        writer.println("Property: DisableAsyncResultRetries = " + DisableAsyncResultRetries);
        writer.println("Property: DisableAutomaticLightweightMethods = " + DisableAutomaticLightweightMethods);
        writer.println("Property: DisableMDBs             = " + DisableMDBs); // F743-13023
        writer.println("Property: DisableRemote           = " + DisableRemote); // F743-13024.CdRv
        writer.println("Property: DisableShortDefaultBindings = " + DisableShortDefaultBindings);
        writer.println("Property: DisableTimers           = " + DisableTimers); // F743-13022
        writer.println("Property: DisablePersistentTimers = " + DisablePersistentTimers); // PI50798
        writer.println("Property: EE5Compatibility        = " + EE5Compatibility); // F743-14982CdRv
        writer.println("Property: EE6Compatibility        = " + EE6Compatibility); // RTC113949
        writer.println("Property: EmptyAnnotationIgnoresExplicitInterfaces = " +
                       EmptyAnnotationIgnoresExplicitInterfaces); // RTC113949
        writer.println("Property: ExcludeNestedExceptions = " + ExcludeNestedExceptions); // d672063
        writer.println("Property: ExpandCMPCFJNDIName     = " + ExpandCMPCFJNDIName); //d425164
        writer.println("Property: FbpkAlwaysReadOnly      = " + FbpkAlwaysReadOnly);
        writer.println("Property: IgnoreDuplicateEJBBindings = " + IgnoreDuplicateEJBBindings);
        writer.println("Property: IncludeNestedExceptions = " + IncludeNestedExceptions);
        writer.println("Property: IncludeNestedExceptionsExtended = " + IncludeNestedExceptionsExtended); //PK87857
        writer.println("Property: IndirectLocalProxies    = " + IndirectLocalProxies);
        writer.println("Property: InitializeEJBsAtStartup = " + InitializeEJBsAtStartup);
        writer.println("Property: MaxAsyncResultWaitTime  = " + MaxAsyncResultWaitTime);
        writer.println("Property: MaxUnclaimedAsyncResults = " + MaxUnclaimedAsyncResults); // d690014.1
        writer.println("Property: NoEJBPool               = " + NoEJBPool);
        writer.println("Property: NoPrimaryKeyMutation    = " + NoPrimaryKeyMutation);
        writer.println("Property: PassivationPolicy       = " + PassivationPolicy);
        writer.println("Property: PersistentTimerSingletonDeadlockTimeout = " + PersistentTimerSingletonDeadlockTimeout);
        writer.println("Property: PoolSize                = " + PoolSize);
        writer.println("Property: Portable                = " + Portable);
        writer.println("Property: PortableFinder          = " + PortableFinder);
        writer.println("Property: RMICCompatible          = " + RMICCompatible);
        writer.println("Property: StrictMaxCacheSize      = " + StrictMaxCacheSize);
        writer.println("Property: TimerCancelTimeout      = " + TimerCancelTimeout);
        writer.println("Property: TimerQOSAtLeastOnceForRequired = " + TimerQOSAtLeastOnceForRequired);
        writer.println("Property: ThrowRemoteFromEjb3Stub = " + ThrowRemoteFromEjb3Stub);
        writer.println("Property: UseFairSingletonLockingPolicy = " + UseFairSingletonLockingPolicy); // F743-9002
        writer.println("Property: UserInstallRoot         = " + UserInstallRoot);
        writer.println("Property: WLMAllowOptionAReadOnly = " + WLMAllowOptionAReadOnly);
        writer.println("Property: ExtendSetRollbackOnlyBehaviorBeyondInstanceFor = " +
                       ExtendSetRollbackOnlyBehaviorBeyondInstanceFor);
        writer.println("Property: LimitSetRollbackOnlyBehaviorToInstanceFor = " +
                       LimitSetRollbackOnlyBehaviorToInstanceFor);
        writer.println("Property: customBindingsEnabledBeta = " + customBindingsEnabledBeta);
        writer.end();
    }

    /**
     * This method is provided as a way to force this 'singleton' class to be
     * loaded, and returns a reference to this class, which should be held to
     * insure the class is not garbage collected / unloaded. <p>
     **/
    public static Class<ContainerProperties> init() {
        return ContainerProperties.class;
    }

    /**
     * Default (and only) constructor is declared private to insure no instances
     * are ever created; in conjunction with the final class declaration insures
     * this class is a Singleton object. <p>
     **/
    private ContainerProperties() {
        // Intentionally left blank - this constructor can never be called.
    }
}
