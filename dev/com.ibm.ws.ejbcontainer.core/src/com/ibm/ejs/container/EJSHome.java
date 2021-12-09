/*******************************************************************************
 * Copyright (c) 1998, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import static com.ibm.ejs.container.ContainerProperties.AllowEarlyInsert;
import static com.ibm.ejs.container.ContainerProperties.AllowPrimaryKeyMutation;
import static com.ibm.ejs.container.ContainerProperties.NoPrimaryKeyMutation;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.CreateException;
import javax.ejb.DuplicateKeyException;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBMetaData;
import javax.ejb.EJBObject;
import javax.ejb.EnterpriseBean;
import javax.ejb.FinderException;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.IllegalLoopbackException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.rmi.PortableRemoteObject;

import com.ibm.ejs.container.activator.ActivationStrategy;
import com.ibm.ejs.container.lock.LockManager;
import com.ibm.ejs.container.lock.LockStrategy;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ejs.util.Util;
import com.ibm.websphere.cpi.Finder;
import com.ibm.websphere.cpi.Persister;
import com.ibm.websphere.cpi.PersisterHome;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CSITransactionRolledbackException;
import com.ibm.websphere.csi.HomeWrapperSet;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.MethodInterface;
import com.ibm.websphere.csi.TransactionAttribute;
import com.ibm.websphere.ejbcontainer.EJBStoppedException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejb.portable.EJBMetaDataImpl;
import com.ibm.ws.ejb.portable.HandleImpl;
import com.ibm.ws.ejb.portable.HomeHandleImpl;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.util.Pool;
import com.ibm.ws.ejbcontainer.util.PoolDiscardStrategy;
import com.ibm.ws.exception.RuntimeWarning;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.runtime.metadata.MethodMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * Provides the base implementation of javax.ejb.EJBHome
 */
@SuppressWarnings({ "serial" })
public abstract class EJSHome implements PoolDiscardStrategy, HomeInternal, SessionBean, PersisterHome {
    private final static TraceComponent tc = Tr.register(EJSHome.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    private static final String CLASS_NAME = "com.ibm.ejs.container.EJSHome";

    /**
     * Accessor to setup ComponentMetaData on thread.
     **/
    // d627931
    private final static ComponentMetaDataAccessorImpl ivCMDAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();

    /**
     * The beanO factory creates beanOs appropriate for wrapping the
     * the type of beans managed by this home (stateless session,
     * container managed entity, etc...).
     */
    protected BeanOFactory beanOFactory;

    /**
     * Container this home is installed in. <p>
     */
    protected EJSContainer container;

    private EntityHelper ivEntityHelper;

    /**
     * The value of the NoLocalCopies java property. <p>
     */
    boolean noLocalCopies; //PQ62081

    /**
     * The value of the allowPrimaryKeyMutation property. <p>
     */
    boolean allowPrimaryKeyMutation; //d138865

    /**
     * The value of the noPrimaryKeyMutation property. <p>
     */
    private boolean noPrimaryKeyMutation; //d138865.1

    /**
     * The JNDI name of this home. <p>
     */
    protected String jndiName;

    /**
     * The Java EE name of this bean. <p>
     */
    protected J2EEName j2eeName;

    /**
     * At most, one of the following must be true for this home. <p>
     */
    protected boolean statelessSessionHome = false;
    protected boolean statefulSessionHome = false;
    protected boolean messageDrivenHome = false;
    protected boolean ivSingletonSessionHome = false; //d565527

    /**
     * Precomputed hash value for this home
     */
    protected int hashValue;

    /**
     * BeanId of this home bean. <p>
     */
    protected BeanId ivHomeId;

    /**
     * Cached wrappers for Stateless Session and WebService Endpoint homes.
     * Stateless Session and WebService Endpoint homes are singleton Stateless
     * Session beans, so their wrappers can be cached to improve performance.
     **/
    // d196581.1
    protected EJSWrapperCommon ivHomeWrappers = null;

    /**
     * Cached beanId for bean types that have no primary key. All instances of
     * Singleton session, Stateless session, and Message Driven beans have the
     * same beanId, so it can be cached to improve performance. A better
     * name for this field would be something like ivNoPrimaryKeyId, but
     * changing the name might make L2/L3 work more difficult. Thus, we
     * will continue to use the historical name for this field.
     **/
    // 140003.26
    protected BeanId ivStatelessId = null;

    /**
     * Cached wrappers for Stateless Session and Message Driven bean instances.
     * All instances of Stateless Session and Message Driven beans have the
     * same wrappers, so it can be cached to improve performance.
     **/
    // d196581.1
    protected EJSWrapperCommon ivStatelessWrappers = null;

    /**
     * Cached wrapper for Stateful Session bean homes; used for creating
     * instances of the business interfaces.
     **/
    // d369262.5
    protected EJSLocalWrapper ivStatefulBusinessHomeWrapper = null;

    protected Class<?> enterpriseBeanClass;
    protected Class<?> remoteEJBObjectClass;
    protected Class<?> localEJBObjectClass; // f111627

    /**
     * Metadata describing the beans managed by this home <p>
     */
    protected BeanMetaData beanMetaData;

    /**
     * Metadata instance to satisfy getEJBMetaData() method
     * on this home.
     */
    private EJBMetaData ejbMetaData; //p125891

    /**
     * Deployed environment properties for this home.
     */
    private Properties environment = null;

    /**
     * Activation strategy for this home.
     */
    protected ActivationStrategy activationStrategy;

    /**
     * Performance data object for this home.
     */
    protected EJBPMICollaborator pmiBean = null;

    /**
     * Persister used for container managed beans associated with this home.
     */
    protected Persister persister;

    /**
     * Lock strategy instance used when lock bean instances associated
     * with this home. <p>
     */
    protected LockStrategy lockStrategy;

    /**
     * Wrapper manager this home uses for finding/registering wrappers.
     */
    protected WrapperManager wrapperManager;

    /**
     * True iff this home is accepting requests.
     */
    protected boolean enabled;

    /**
     * The bean pool to use for allocating/freeing beans in this home.
     */
    protected Pool beanPool;

    /**
     * The number of bean instances created that are either currently in use
     * or in the free pool (beanPool). Only used when there is a limit on the
     * number of beans (Stateless).
     **/
    // PK20648
    protected int ivNumberBeansCreated = 0;

    /**
     * Amount of time in ms that a thread will wait for an available bean
     * in the bean pool. Only used when there is a limit on the number of
     * beans (Stateless).
     **/
    // PK20648
    private static int BEAN_POOL_WAIT_INTERVAL = 10000;

    /**
     * Indicats whether or not EJBAccessIntent service should be used.
     * Determined by the version of the EJB in question.
     */
    protected boolean ivAIServiceEnabled = false;

    // d173022.9 Begins
    /**
     * This is a dummy methodInfo used to obtain the bean level default AccessIntent.
     */
    protected EJBMethodInfoImpl defaultAIMethodInfo = null;

    /**
     * Cached value for the hasMethodLevelAccessIntentSet() to avoid repetitive call
     * to AppProfile to retrieve this value since this value will not change.
     * Value = 0 - Not initialized.
     * = 1 - has method level AccessIntent defined
     * = -1 - has no method level AccessIntent defined
     */
    protected static final int CachedAI_NotInitialize = 0;
    protected static final int CachedAI_HasMethodLevelAI = 1;
    protected static final int CachedAI_HasNoMethodLevelAI = -1;
    protected int cachedHasMethodLevelAI = CachedAI_NotInitialize;
    protected Object ivCachedBeanLevelAccessIntent = null; // d255080.4
    // d173022.9 Ends

    // Property access & default now handled by ContainerProperties.       391302
    private static final boolean cvUsePortableClass = ContainerProperties.Portable;

    boolean ivAllowEarlyInsert = false; //PQ89520

    protected HomeRecord homeRecord = null; //LIDB849-4

    /**
     * SingletonBeanO instance when this home is for a Singleton
     * session bean and the singleton instance has been created.
     * The first time createBeanO method is called for a singleton
     * sees this field as being null and creates the SingletonBeanO.
     */
    private volatile SingletonBeanO ivSingletonBeanO = null;

    /**
     * True while the SingletonBeanO is being created. This is to avoid
     * PostConstruct of the singleton (or one of its dependencies) from
     * attempting to use the bean before it has finished initializing.
     */
    private boolean ivCreatingSingletonBeanO; // F743-4950

    /**
     * True if an attempt was made to initialize the singleton but failed.
     */
    // F7434950.CodRev
    private boolean ivSingletonBeanOCreateFailed;

    /**
     * Flag that will default to false and be set to true only after
     * all application startup processing has completed. The
     * EJSContainer.checkIfEJBWorkAllowed() method will be called to
     * ensure that either all application startup processing has completed
     * or that the work being attempted is being done on the thread that is
     * starting the application, and therefore part of application startup
     * (for example: Singleton Startup bean work).
     * The checkIfEJBWorkAllowed() method will throw an
     * ApplicationNotStartedException if the application is still in the process
     * of started and the threads do not match. It will finish, but return "false",
     * if the the threads match, but application startup is still in progress.
     * Finally, it will return "true" if application startup is complete. That is
     * when this flag will be set to "true" so that this checking is no longer
     * performed for any future usage of this bean type.
     */
    private boolean ivApplicationStarted = false; //F743-1753CodRev

    public EJSHome() throws RemoteException {
        // Intentionally blank.
    }

    /**
     * Throw a runtime exception if this home is not open for business.
     */
    protected final void homeEnabled() {
        if (!enabled) {
            // Provide some meaningful message text.                        d350987
            String msgTxt = "The referenced version of the " + j2eeName.getComponent() +
                            " bean in the " + j2eeName.getApplication() +
                            " application has been stopped and may no longer be used. " +
                            "If the " + j2eeName.getApplication() +
                            " application has been started again, a new reference for " +
                            "the new image of the " + j2eeName.getComponent() +
                            " bean must be obtained. Local references to a bean or home " +
                            "are no longer valid once the application has been stopped.";

            if (beanMetaData.ivModuleVersion < BeanMetaData.J2EE_EJB_VERSION_3_0) {
                throw new HomeDisabledException(msgTxt);
            }

            // For EJB 3.0+, throw a more meaningful exception that is in
            // the com.ibm.websphere (public) package. Some applications may wish
            // to watch for this, and reset local references.               d350987
            throw new EJBStoppedException(msgTxt);
        } else if (!ivApplicationStarted) {
            // Ensure that the application has completed startup processing or that
            // the EJB work is being attempted on the same thread that is being used to
            // start the application.   This method will throw an ApplicationNotStartedException
            // if any of the criteria are not met.  It will also return "false" until the
            // application has completed startup processing.   In this way we will continue
            // to do this checking only until the application has started.s
            ivApplicationStarted = beanMetaData._moduleMetaData.getEJBApplicationMetaData().checkIfEJBWorkAllowed(beanMetaData._moduleMetaData); // F743-4950 d607800.CodRv
        }

    } // homeEnabled

    private void enable(EJSContainer ejsContainer,
                        BeanId id,
                        BeanMetaData bmd) throws RemoteException {
        this.container = ejsContainer;
        this.ivEntityHelper = ejsContainer.ivEntityHelper;
        this.ivHomeId = id;

        this.beanMetaData = bmd;
        this.persister = this.beanMetaData.persister;
        this.wrapperManager = ejsContainer.getWrapperManager();

        jndiName = bmd.getJndiName(); //d513978
        j2eeName = bmd.j2eeName;
        hashValue = j2eeName.hashCode();

        if (bmd.type != InternalConstants.TYPE_MANAGED_BEAN) // F743-34301
        {
            if (ejsContainer.pmiFactory != null) {
                pmiBean = ejsContainer.pmiFactory.createPmiBean(bmd, ejsContainer.ivName);
            }

            // d173022.9 Begins
            defaultAIMethodInfo = bmd.createEJBMethodInfoImpl(container.getEJBRuntime().getMetaDataSlotSize(MethodMetaData.class));
            defaultAIMethodInfo.initializeInstanceData("__defaultAIMethodInfo__",
                                                       "__defaultAIMethodInfo__NameOnly",
                                                       bmd,
                                                       MethodInterface.REMOTE,
                                                       TransactionAttribute.TX_SUPPORTS,
                                                       false);
            // d173022.9 Ends
        }

        enabled = true;

        this.homeRecord = bmd.homeRecord; //199884

    }

    public void initialize(EJSContainer ejsContainer,
                           BeanId id,
                           BeanMetaData bmd) throws RemoteException {
        // Cache the noLocalCopies system property, to determine when
        // container needs to make copies of the primary key objects,
        // since the ORB is not copying them.                              PQ62081
        // Property access & default now handled by EJSContainer.           391302
        // Property access now handled by ObjectUtil                       d587232
        this.noLocalCopies = ejsContainer.ivObjectCopier.isNoLocalCopies(); // RTC102299

        // Cache the allowPrimaryKeyMutation system property, to determine when
        // container needs to make copies of the primary key objects,
        // only copy if allow mutation is true.  This property is only
        // used for Remote Interfaces and the default value is "false".    d138865
        // Property access & default now handled by ContainerProperties.    391302
        this.allowPrimaryKeyMutation = AllowPrimaryKeyMutation;

        // Cache the noPrimaryKeyMutation system property, to determine when
        // copies of the primary key objects are not needed.  This property is only used
        // for Local Interfaces and will default to "false".   It can be set to true by
        // customers who wish to improve performance and can guarentee that
        // their code will not mutate existing PrimaryKey objects         d138865.1
        // Property access & default now handled by ContainerProperties.    391302
        this.noPrimaryKeyMutation = NoPrimaryKeyMutation;

        /* PQ89520 -start */
        if (bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X) {
            // Property access & default now handled by ContainerProperties. 391302
            this.ivAllowEarlyInsert = AllowEarlyInsert;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()
                && this.ivAllowEarlyInsert) {
                Tr.debug(tc, "Allow early insert property is true for CMP1.1 bean.");
            }
        }
        /* PQ89520 -end */

        enable(ejsContainer, id, bmd);
    }

    public void completeInitialization() throws RemoteException {
        enterpriseBeanClass = beanMetaData.enterpriseBeanClass;
        remoteEJBObjectClass = beanMetaData.remoteImplClass;
        localEJBObjectClass = beanMetaData.localImplClass; // f111627
        environment = beanMetaData.envProps;

        if (beanMetaData.type == InternalConstants.TYPE_STATELESS_SESSION) {
            statelessSessionHome = true;
            ivStatelessId = new BeanId(this, null); // d140003.26
        } else if (beanMetaData.type == InternalConstants.TYPE_STATEFUL_SESSION) {
            statefulSessionHome = true;
        } else if (beanMetaData.type == InternalConstants.TYPE_MESSAGE_DRIVEN) {
            messageDrivenHome = true;
            ivStatelessId = new BeanId(this, null); // d140003.26
        } else if (beanMetaData.type == InternalConstants.TYPE_SINGLETON_SESSION) // F743-508
        {
            ivSingletonSessionHome = true;
            ivStatelessId = new BeanId(this, null);
        }

        // If this is a Stateful Session bean home, and it has business
        // interfaces, then create a special wrapper used for creating
        // instances for injection and lookup of the business
        // interfaces.                                                   d369262.5
        if (statefulSessionHome &&
            (beanMetaData.ivBusinessLocalInterfaceClasses != null ||
             beanMetaData.ivBusinessRemoteInterfaceClasses != null)) {
            try {
                EJSLocalWrapper wrapper = new EJSLocalWrapper();
                wrapper.beanId = ivHomeId;
                wrapper.bmd = beanMetaData;
                wrapper.methodInfos = new EJBMethodInfoImpl[1];
                wrapper.methodNames = new String[] { "create" };
                wrapper.container = container;
                wrapper.wrapperManager = wrapperManager;
                wrapper.ivPmiBean = pmiBean;
                wrapper.ivCommon = null;
                wrapper.isManagedWrapper = false;
                wrapper.ivInterface = WrapperInterface.LOCAL_HOME;

                wrapper.methodInfos[0] = beanMetaData.createEJBMethodInfoImpl(beanMetaData.container.getEJBRuntime().getMetaDataSlotSize(MethodMetaData.class));
                wrapper.methodInfos[0].initializeInstanceData("create:",
                                                              "create",
                                                              beanMetaData,
                                                              MethodInterface.LOCAL_HOME,
                                                              TransactionAttribute.TX_NOT_SUPPORTED,
                                                              false);
                wrapper.methodInfos[0].setMethodDescriptor("(Ljava.lang.Class;)Ljava.lang.Object;");
                ivStatefulBusinessHomeWrapper = wrapper;
            } catch (Throwable th) {
                FFDCFilter.processException(th, CLASS_NAME + ".completeInitialization",
                                            "642", this);
                throw ExceptionUtil.EJBException(th);
            }
        }

        // An Exclusive Lock Strategy is used for both Option A Entity
        // caching, as well as for Stateless Session beans with a
        // maximum creation limit.                                         PK20648
        if (beanMetaData.optionACommitOption ||
            beanMetaData.ivMaxCreation > 0) {
            lockStrategy = LockStrategy.EXCLUSIVE_LOCK_STRATEGY;
        } else {
            lockStrategy = LockStrategy.NULL_LOCK_STRATEGY;
        }

        //---------------------------------------------------------
        // Stateful session beans must never be pooled, so use the
        // null pool for them.
        //---------------------------------------------------------

        if (statefulSessionHome == true || ivSingletonSessionHome) //d565527
        {
            beanPool = null;
        } else {
            beanPool = container.poolManager.create(beanMetaData.minPoolSize,
                                                    beanMetaData.maxPoolSize,
                                                    pmiBean,
                                                    this);

            if (beanMetaData.ivInitialPoolSize != 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Pre-loading BeanPool with " + beanMetaData.ivInitialPoolSize);

                // d648522 - Pre-load the bean pool on a separate thread to avoid
                // wrapper locking issues during deferred initialization.
                container.getEJBRuntime().getScheduledExecutorService().schedule(new Runnable() {
                    @Override
                    public void run() {
                        // d664917.1 - We already have a reference to the
                        // EJSHome, but we call through HomeRecord to ensure
                        // the deferred init thread has finished before we
                        // begin preloading.
                        homeRecord.getHomeAndInitialize().preLoadBeanPool();
                    }
                }, 0, TimeUnit.MILLISECONDS); // F73234
            }
        }

        //139562.15.EJBC begin
        // We only want to use AccessIntent service when it is a BMP in a EJB 2.0
        // or later version of module where a BMP is supported in that version.
        if (beanMetaData.getEJBModuleVersion() >= BeanMetaData.J2EE_EJB_VERSION_2_0 && //d174083
            beanMetaData.getEJBComponentType() == InternalConstants.TYPE_BEAN_MANAGED_ENTITY) {
            ivAIServiceEnabled = true;
        }
        //139562.15.EJBC end

    }

    /**
     * Pre-load the BeanPool if the BeanPool min value has been configured as a
     * 'hard' limit. Any exceptions encountered will be ignored, and the pool
     * just not pre-loaded. Currently only supported for Stateless Session
     * beans.
     */
    private void preLoadBeanPool() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.debug(tc, "preLoadBeanPool: " + j2eeName);

        synchronized (beanPool) {
            Object oldClassLoader = ThreadContextAccessor.UNCHANGED;

            try {
                // To support injection, etc. we must put the bmd and classloader on the thread.
                ivCMDAccessor.beginContext(beanMetaData);
                oldClassLoader = EJBThreadData.svThreadContextAccessor.pushContextClassLoaderForUnprivileged(beanMetaData.ivContextClassLoader);

                for (int i = ivNumberBeansCreated; i < beanMetaData.ivInitialPoolSize; i++) {
                    BeanO beanO = beanOFactory.create(container, this, false);
                    beanPool.put(beanO);
                    if (beanMetaData.ivMaxCreation > 0) {
                        ++ivNumberBeansCreated;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Pre-load BeanPool(" +
                                         ivNumberBeansCreated + "/" +
                                         beanMetaData.ivMaxCreation + ")");
                    }
                }
            } catch (Throwable ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".preLoadBeanPool", "561", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Pre-load of BeanPool failed: exception ignored: " +
                                 ex);
                if (ex instanceof InvocationTargetException) {
                    ex = ex.getCause();
                }
                Tr.warning(tc, "IGNORING_UNEXPECTED_EXCEPTION_CNTR0033E", ex);
            } finally {
                EJBThreadData.svThreadContextAccessor.popContextClassLoaderForUnprivileged(oldClassLoader);
                ivCMDAccessor.endContext();
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "preLoadBeanPool");
    }

    /**
     * Disable this home instance and free all resources associated
     * with it.
     */
    public void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "Destroying home ", new Object[] { this, j2eeName });

        if (!enabled) {
            return;
        }

        // F743-1751 - Explicitly destroy singletons since they are not pooled.
        if (ivSingletonBeanO != null) {
            try {
                ivSingletonBeanO.destroy();
            } catch (Throwable ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".destroy", "809", this);
            }
        }

        synchronized (this) {
            if (beanPool != null) {
                beanPool.destroy();
            }

            enabled = false;

            if (pmiBean != null) {
                container.pmiFactory.removePmiModule(pmiBean); // d146239.14
            }
        }
    } // destroy

    @Override
    public int hashCode() {
        return hashValue;
    }

    /**
     * Get the JNDI name of this home.
     */
    @Override
    public String getJNDIName(Object pkey) {
        return jndiName;
    } // getJNDIName

    /**
     * Get the Java EE name of this home. <p>
     */
    @Override
    public J2EEName getJ2EEName() {
        return j2eeName;
    } // getJ2EEName

    /**
     * Get the id of this home bean. <p>
     */
    @Override
    public BeanId getId() {
        return ivHomeId;
    } // getId

    /**
     * d112604.1
     * Get the EJSDeployedSupport for this thread's current method
     */

    @Override
    public Object getCurrentThreadDeployedSupport() { // d112604.1
        return EJSContainer.getMethodContext(); // d646139.1
    }

    // d112604.5
    @Override
    public void setCustomFinderAccessIntentThreadState(boolean cfwithupdateaccess,
                                                       boolean readonly,
                                                       String methodname) {
        container.setCustomFinderAccessIntentThreadState(cfwithupdateaccess,
                                                         readonly,
                                                         methodname);
    }

    @Override
    public void resetCustomFinderAccessIntentContext() {
        container.resetCustomFinderAccessIntentContext();
    }

    // d112604.5

    /**
     * Get a wrapper for this home. <p>
     */
    @Override
    public final EJSWrapperCommon getWrapper() // f111627
                    throws CSIException, RemoteException {
        homeEnabled();

        // EJS Homes are stateless session beans with singleton wrappers.
        // If the wrapper has been created, and is still in the wrapper cache,
        // then just return the singleton EJSWrapperCommon.
        // Note that 'inCache' will 'touch' the wrapper in the Wrapper Cache,
        // updating the LRU data and preventing it from unregistering with
        // the ORB.                                                      d196581.1
        if (ivHomeWrappers == null ||
            !ivHomeWrappers.inCache()) {
            ivHomeWrappers = wrapperManager.getWrapper(ivHomeId); // d156807.1
        }

        return ivHomeWrappers;
    }

    /**
     * Gets the set of wrappers for binding this home to naming.
     */
    public final HomeWrapperSet getWrapperSet() // d648522, d739542
                    throws CSIException, RemoteException {
        EJSWrapperCommon w = getWrapper();
        Remote remote = beanMetaData.homeRemoteImplClass == null ? null : w.getRemoteWrapper();
        Object local = beanMetaData.homeLocalImplClass == null ? null : w.getLocalObject();
        return new HomeBindingInfo(remote, local);
    }

    /**
     * Get a wrapper for the given BeanId <p>
     */
    @Override
    public final EJSWrapperCommon getWrapper(BeanId id) // f111627
                    throws CSIException, RemoteException {
        homeEnabled();
        return wrapperManager.getWrapper(id); // d156807.1
    }

    /**
     * Get a TimedObject wrapper for the given BeanId. <p>
     *
     * TimedObject wrappers are not generated, since there is just one defined
     * method that is the same for all bean types. Also, TimedObject wrappers
     * are never given out, so the lifecycle is well known. Therefore,
     * TimedObject wrappers are pooled, rather than cached and managed by
     * the WrapperManager. <p>
     *
     * This method must be used instead of getWrapper() to obtain a TimedObject
     * wrapper, and, putTimedObjectWrapper() should be called when use of the
     * wrapper is no longer needed. <p>
     *
     * @param beanId identity of bean to obtain a wrapper for.
     *
     * @return a TimedObject wrapper for the given bean id.
     **/
    // LI2281.07
    final TimedObjectWrapper getTimedObjectWrapper(BeanId beanId) {
        TimedObjectWrapper timedWrapper = null;

        // Get timedObject from container pool of timed objects.
        timedWrapper = (TimedObjectWrapper) container.ivTimedObjectPool.get();

        // If the pool was empty, create a new one... and set the
        // invariant fields.
        if (timedWrapper == null) {
            timedWrapper = new TimedObjectWrapper();
            timedWrapper.container = container;
            timedWrapper.wrapperManager = wrapperManager;
            timedWrapper.ivCommon = null; // Not cached        d174057.2
            timedWrapper.isManagedWrapper = false; // Not managed       d174057.2
            timedWrapper.ivInterface = WrapperInterface.TIMED_OBJECT; // d366807
        }

        // Now, fill in the TimedObjectWrapper, based on this home.
        timedWrapper.beanId = beanId;
        timedWrapper.bmd = beanMetaData;
        timedWrapper.methodInfos = beanMetaData.timedMethodInfos;
        timedWrapper.methodNames = beanMetaData.timedMethodNames;
        timedWrapper.isolationAttrs = null; // not used for EJB 2.x
        timedWrapper.ivPmiBean = pmiBean; // d174057.2

        return timedWrapper;
    }

    /**
     * Returns a TimedObject wrapper to the pool. <p>
     *
     * TimedObject wrappers are not generated, since there is just one defined
     * method that is the same for all bean types. Also, TimedObject wrappers
     * are never given out, so the lifecycle is well known. Therefore,
     * TimedObject wrappers are pooled, rather than cached and managed by
     * the WrapperManager. <p>
     *
     * This method should be called when use of the wrapper is no longer needed.
     * <p>
     *
     * @param timedWrapper TimedObject wrapper to return to the pool.
     **/
    // LI2281.07
    final void putTimedObjectWrapper(TimedObjectWrapper timedWrapper) {
        // Clear the variant fields and return to pool...
        timedWrapper.beanId = null;
        timedWrapper.bmd = null;
        timedWrapper.methodInfos = null;
        timedWrapper.isolationAttrs = null;
        timedWrapper.methodNames = null;
        timedWrapper.ivPmiBean = null; // d174057.2

        // Put timedObject back in container pool of timed objects.        d174057
        container.ivTimedObjectPool.put(timedWrapper);
    }

    /**
     * This method creates and returns a new <code>BeanO</code> instance
     * appropriate for this home. <p>
     *
     * The returned <code>BeanO</code> has a newly created enterprise
     * bean instance associated with it, and the enterprise bean instance
     * has had its set...Context() method called on it to set its context
     * to the returned <code>BeanO</code>. <p>
     *
     * This method must only be called when a new <code>BeanO</code>
     * instance is needed. It always creates a new <code>BeanO</code>
     * instance and a new instance of the associated enterprise bean. <p>
     *
     * For CMP beans it is necessary to reset CMP fields to Java defaults
     * at this time. Our solution is to create a new instance each time
     * a create is invoked on the CMP home. This instance is added to the
     * pool during passivate and only if the pool size is less than the
     * the max else it is discarded. This approach avoids messy reflection
     * API usage for resetting.<p>
     *
     * If this method returns successfully, then {@link EJBThreadData#pushCallbackBeanO} will have been called, and the
     * caller must ensure that {@link EJBThreadData#popCallbackBeanO} is
     * eventually called. <p>
     *
     * @param threadData the <code>EJBThreadData</code> associated with the
     *            currently running thread
     * @param tx the <code>ContainerTx</code> to associate with the newly
     *            created <code>BeanO</code> <p>
     * @param activate true if the created BeanO will be used for bean
     *            activation, false for bean creation.
     * @param context the context for creating the bean, or null
     *
     * @return newly created <code>BeanO</code> associated with a newly
     *         created bean instance of type of beans managed by this
     *         home <p>
     */
    private BeanO createBeanO(EJBThreadData threadData,
                              ContainerTx tx,
                              boolean activate,
                              ManagedObjectContext context) throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled()) // d367572.7
            Tr.entry(tc, "createBeanO(ContainerTx, activate) activate = " + activate);

        homeEnabled();
        BeanO result = null;

        // -----------------------------------------------------------------------
        // For Stateless Session beans, the number of 'active' beans may
        // be limited.  The call to allocateBeanO will 'assign' one to this
        // thread and either return one from the pool, or return null to
        // indicate the pool is empty, but the limit is not currently in use
        // so one should be created. allocateBeanO will block and control will
        // NOT be returned until a BeanO has been allocated... or the attempt
        // has timed out, in which case an Exception will be thrown.       PK20648
        // -----------------------------------------------------------------------
        if (beanMetaData.ivMaxCreation > 0) {
            result = allocateBeanO(tx);
        }
        // -----------------------------------------------------------------------
        // F743-509.CodRev
        // Max creation limit is zero. Determine if is a Singleton session home.
        // A singleton session home never has a max creation limit since there
        // never is more than 1 Singleton instance created.
        // -----------------------------------------------------------------------
        else if (ivSingletonSessionHome) //d565527
        {
            result = createSingletonBeanO(); //F743-1753
        }
        // -----------------------------------------------------------------------
        // F743-509.CodRev
        // Neither a Stateless with a max creation limit nor a Singleton session bean
        // home.  If there is a bean pool, get an instance from the pool. Otherwise,
        // fall through and create a new one.
        // -----------------------------------------------------------------------
        else if (beanPool != null) {
            result = (BeanO) beanPool.get();
        }

        if (result == null) { // Create a new instance.
            if (statefulSessionHome && activate) // d367572.7
            {
                try {
                    result = beanOFactory.create(container, this, true); // d367572.7
                } catch (InvocationTargetException e) {
                    // we don't create the instance when reactivating, so ITE should not be thrown
                    FFDCFilter.processException(e, CLASS_NAME + ".createBeanO", "960", this);
                    throw new IllegalStateException(e);
                }
            } else {
                long createStartTime = -1;
                try {
                    // For Stateless and MessageDriven, create count is the same
                    // as instantiation count. For these types, create time should
                    // include creating the instance and calling any lifecycle
                    // callbacks.                                           d626533.1
                    if (pmiBean != null &&
                        (statelessSessionHome || messageDrivenHome)) {
                        createStartTime = pmiBean.initialTime(EJBPMICollaborator.CREATE_RT);
                    }

                    result = beanOFactory.create(container, this, false);
                } catch (InvocationTargetException e) {
                    FFDCFilter.processException(e, CLASS_NAME + ".createBeanO", "977", this);
                    throw new RemoteException(enterpriseBeanClass.getName(), e.getCause());
                } finally {
                    // Even if the create fails, go ahead and add the time, so
                    // the number of times counted matches the create count.
                    if (createStartTime > -1) {
                        pmiBean.finalTime(EJBPMICollaborator.CREATE_RT, createStartTime);
                    }
                }
            }
        } else if (!activate
                   && beanMetaData.type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY
                   && beanMetaData.cmpResetFields != null) {
            // Bean was obtained from pool and needs CMP instance values reset
            // to Java defaults
            EnterpriseBean b = result.getEnterpriseBean();
            for (int i = 0; i < beanMetaData.cmpResetFields.length; ++i) {
                try {
                    java.lang.reflect.Field f = beanMetaData.cmpResetFields[i];
                    Class<?> clzz = f.getType();
                    if (clzz.isPrimitive()) {
                        if (clzz == Long.TYPE) {
                            f.setLong(b, 0);
                        } else if (clzz == Integer.TYPE) {
                            f.setInt(b, 0);
                        } else if (clzz == Boolean.TYPE) {
                            f.setBoolean(b, false);
                        } else if (clzz == Short.TYPE) {
                            f.setShort(b, (short) 0);
                        } else if (clzz == Byte.TYPE) {
                            f.setByte(b, (byte) 0);
                        } else if (clzz == Character.TYPE) {
                            f.setChar(b, (char) 0);
                        } else if (clzz == Double.TYPE) {
                            f.setDouble(b, 0);
                        } else if (clzz == Float.TYPE) {
                            f.setFloat(b, 0);
                        }
                    } else {
                        f.set(b, null);
                    }
                } catch (IllegalAccessException iae) {
                    FFDCFilter.processException(iae, CLASS_NAME + ".createBeanO",
                                                "598", this);
                    throw new ContainerException("Problem occurred resetting CMP fields to Java default values", iae);
                }
            }
        }

        // -----------------------------------------------------------------------
        // Set the created/found BeanO as the 'Callback' BeanO, as this is the
        // BeanO that is becoming the active beanO for the thread.
        // This will allow methods called by customer code (like Timer methods)
        // to determine the state of the BeanO that is making the call     d168509
        // -----------------------------------------------------------------------
        threadData.pushCallbackBeanO(result); // d630940

        if (isTraceOn && tc.isEntryEnabled()) // d367572.7 d402055
            Tr.exit(tc, "createBeanO(ContainerTx, activate) activate = " + activate);

        return result;
    } // createBeanO

    /**
     * This method creates and returns a new <code>BeanO</code> instance
     * appropriate for this home. <p>
     *
     * The returned <code>BeanO</code> has a newly created enterprise
     * bean instance associated with it, and the enterprise bean instance
     * has had its set...Context() method called on it to set its context
     * to the returned <code>BeanO</code>. <p>
     *
     * This method must only be called when a new <code>BeanO</code>
     * instance is needed. It always creates a new <code>BeanO</code>
     * instance and a new instance of the associated enterprise bean. <p>
     *
     * If this method returns successfully, then {@link EJBThreadData#pushCallbackBeanO} will have been called, and the
     * caller must ensure that {@link EJBThreadData#popCallbackBeanO} is
     * eventually called. <p>
     *
     * @param threadData the <code>EJBThreadData</code> associated with the
     *            currently running thread
     * @param tx the <code>ContainerTx</code> to associate with the newly
     *            created <code>BeanO</code> <p>
     * @param id the <code>BeanId</code> to associate with the newly
     *            created <code>BeanO</code> <p>
     *
     * @return newly created <code>BeanO</code> associated with a newly
     *         created bean instance of type of beans managed by this
     *         home <p>
     */
    // Added ContainerTx d168509; added EJBThreadData d630940
    @Override
    public BeanO createBeanO(EJBThreadData threadData, ContainerTx tx, BeanId id) throws RemoteException {
        homeEnabled();
        BeanO result = createBeanO(threadData, tx, true, null); // d630940
        result.setId(id);
        return result;
    } // createBeanO

    /**
     * Creates a bean instance for this home. <p>
     *
     * This method is intended to be called by the create methods of entity and
     * SFSB home classes generated by EJBDeploy and JITDeploy. The methods will
     * have been called through home wrappers, so preInvoke will have been
     * called immediately prior to this method being invoked. <p>
     *
     * Need to distinguish between invocations of this method from the
     * create path and the activate path.
     * On the create path a new instance of a CMP bean needs to be
     * created so that the fields in the CMP bean are set to their Java
     * defaults. On the activate path, we can reuse an already created
     * instance of the bean. <p>
     *
     * If this method returns successfully, the caller (the generated entity or
     * stateful home bean) must call {@link #preEjbCreate}, then ejbCreate, then {@link #postCreate}, then ejbPostCreate (if necessary), then {@link #afterPostCreate}. If the
     * entire sequence is successful, then {@link #afterPostCreateCompletion} must be called. If an exception
     * occurs at any point, then {@link #createFailure} must be called.
     */
    public BeanO createBeanO() throws RemoteException {
        EJSDeployedSupport s = EJSContainer.getMethodContext();
        return createBeanO(s.ivThreadData, s.currentTx, false, null); // d630940
    }

    /**
     * Internal method used by createBeanO to enforce a max / limit
     * to the number of active beans (Stateless).
     *
     * allocateBeanO will 'assign' a BeanO to the current thread and
     * either return one from the pool, or return null to indicate the
     * pool is empty, but the limit has not currently been reached,
     * so one should be created.
     *
     * allocateBeanO will block and control will NOT be returned until
     * a BeanO has been allocated/assigned... or the attempt has timed
     * out, in which case an Exception will be thrown.
     *
     * allocateBeanO will time out when the global transaction times
     * out or 5 minutes, whichever occurs first. Note that local
     * transactions do not time out. The default 5 minute timeout
     * may be overridden with the poolSize property.
     *
     * @param tx the <code>ContainerTx</code> to associate with the newly
     *            created <code>BeanO</code> <p>
     *
     * @return <code>BeanO</code> assigned from the BeanPool, or null
     *         if a new instance needs to be created.
     **/
    // PK20648
    private BeanO allocateBeanO(ContainerTx tx) throws RemoteException {
        BeanO result = null;
        long wait_time = beanMetaData.ivMaxCreationTimeout; // default is 5 minutes
        boolean beanAvailable = false;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "allocateBeanO: Obtaining HomeId lock for BeanPool : " + tx);

        lockStrategy.lock(container, tx, ivHomeId, LockManager.EXCLUSIVE);

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "allocateBeanO: Obtained HomeId lock for BeanPool : " + tx);

        try {
            while (!beanAvailable) {
                synchronized (beanPool) {
                    result = (BeanO) beanPool.get();

                    if (result == null) {
                        if (ivNumberBeansCreated < beanMetaData.ivMaxCreation) {
                            beanAvailable = true;
                            ++ivNumberBeansCreated;
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "allocateBeanO: Not found in BeanPool(" +
                                             ivNumberBeansCreated + "/" +
                                             beanMetaData.ivMaxCreation +
                                             ") : creating new instance");
                        } else {
                            long wait_interval = Math.min(wait_time, BEAN_POOL_WAIT_INTERVAL);
                            if (wait_interval > 0) {
                                if (isTraceOn && tc.isDebugEnabled())
                                    Tr.debug(tc, "allocateBeanO: Not found in BeanPool(" +
                                                 ivNumberBeansCreated + "/" + beanMetaData.ivMaxCreation +
                                                 ") : waiting for bean - " + wait_interval);

                                beanPool.wait(wait_interval);
                                wait_time -= wait_interval;
                            }
                        }
                    } else {
                        beanAvailable = true;
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "allocateBeanO: Found in BeanPool(" +
                                         ivNumberBeansCreated + "/" +
                                         beanMetaData.ivMaxCreation + ") : " + result);
                    }
                }

                if (!beanAvailable) {
                    homeEnabled();
                    if (tx.getGlobalRollbackOnly()) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "allocateBeanO: Tx timeout waiting for bean " +
                                         "(" + (beanMetaData.ivMaxCreationTimeout - wait_time)
                                         + "): BeanPool(" + ivNumberBeansCreated + "/" +
                                         beanMetaData.ivMaxCreation + ")");
                        throw new CSITransactionRolledbackException("Transaction timed out or marked rolled back");
                    }

                    if (wait_time <= 0) // no time left to wait
                    {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "allocateBeanO: Wait timeout waiting for bean " +
                                         "(" + (beanMetaData.ivMaxCreationTimeout) +
                                         "): BeanPool(" + ivNumberBeansCreated + "/" +
                                         beanMetaData.ivMaxCreation + ")");
                        throw new EJBException("Instance of EJB " + j2eeName +
                                               " not available : wait timeout");
                    }
                }
            }
        } catch (InterruptedException iex) {
            FFDCFilter.processException(iex, CLASS_NAME + ".allocateBeanO",
                                        "919", this);
            ContainerEJBException ex = new ContainerEJBException("Instance of EJB " + j2eeName +
                                                                 " not available : interrupted", iex);
            Tr.error(tc,
                     "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                     new Object[] { iex, ex.toString() });
            throw ex;
        } finally {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "allocateBeanO: Releasing HomeId lock for BeanPool : " +
                             tx);
            lockStrategy.unlock(container, ivHomeId, tx);
        }

        return result;
    }

    /**
     * Create wrapper instance of the type of wrappers managed by this
     * home. <p>
     *
     * This method provides a wrapper factory capability. <p>
     *
     * This method is called by Stateless Session and Message Driven
     * bean Home create methods. <p>
     *
     * Notes: EJB 2.0 Have to keep this interface because stateless session
     * bean generated code directly references this method. <p>
     *
     * The BeanId parameter is never used, but must remain a parameter for
     * compatibility with code generated on previous releases. <p>
     *
     * @param id BeanId for wrapper to be created.
     *
     * @return <code>EJSWrapper</code> instance whose most specific
     *         type is the type of wrappers managed by this home <p>
     */
    public EJSWrapper createWrapper(BeanId id) throws CreateException, RemoteException, CSIException {
        homeEnabled();
        EJSWrapper result = null;

        //------------------------------------------------------------------------
        // This is the method used by stateless session beans home create methods
        // to check and see if their singleton wrapper is already registered.
        //------------------------------------------------------------------------

        // If the wrapper has been created, and is still in the wrapper cache,
        // then just return the remote wrapper from the singleton WrapperCommon.
        // Note that 'inCache' will 'touch' the wrapper in the Wrapper Cache,
        // updating the LRU data and preventing it from unregistering with
        // the ORB.                                                      d196581.1
        if (ivStatelessWrappers != null &&
            ivStatelessWrappers.inCache()) {
            // Calling getRemoteWrapper will cause the wrapper to register with
            // the ORB, if that hasn't already been done.
            result = ivStatelessWrappers.getRemoteWrapper();
        } else {
            // The singleton wrapper has either not been created yet, or is not
            // currently in the wrapper cache.  So, getting it from the Wrapper
            // Manager will cause it to be created (if needed) and faulted into
            // the Wrapper Cache, and then getting the Remote wrapper will cause
            // it to be registered with the ORB.                          d196581.1

            // Use a cached instance beanId... for stateless and message beans all
            // instances have the same id.                               d140003.26

            if (statelessSessionHome) {
                // Since this is is one of the home types with a singleton wrapper,
                // then set the local cached value now that it has been created
                // and inserted into the Wrapper Cache.  It cannot be cached
                // locally until inserted into the Wrapper Cache, as the insert
                // updates the wrapper with a reference to the bucket.       d233983
                ivStatelessWrappers = wrapperManager.getWrapper(ivStatelessId);

                result = ivStatelessWrappers.getRemoteWrapper();
            }
        }

        return result;
    }

    // f111627 Begin
    /**
     * Create wrapper instance of the type of wrappers managed by this
     * home. <p>
     *
     * This method provides a wrapper factory capability. <p>
     *
     * This method is called by Stateless Session and Message Driven
     * bean Home create methods. <p>
     *
     * Notes: EJB 2.0 Have to keep this interface because stateless session
     * bean generated code directly referencing this method. The MDBHomeBean
     * also calls this method to create the wrapper implementing the
     * MessageListener interface. <p>
     *
     * The BeanId parameter is never used, but must remain a parameter for
     * compatibility with code generated on previous releases. <p>
     *
     * @param id BeanId for wrapper to be created.
     *
     * @return <code>EJSWrapper</code> instance whose most specific
     *         type is the type of wrappers managed by this home <p>
     */
    public EJSLocalWrapper createWrapper_Local(BeanId id) throws CreateException, RemoteException, CSIException {
        homeEnabled();
        EJSLocalWrapper result = null;

        //------------------------------------------------------------------------
        // This is the method used by Stateless and Message home create methods
        // to check and see if their singleton wrapper is already registered.
        //------------------------------------------------------------------------

        // If the wrapper has been created, and is still in the wrapper cache,
        // then just return the local wrapper from the singleton WrapperCommon.
        // Note that 'inCache' will 'touch' the wrapper in the Wrapper Cache,
        // updating the LRU data.                                        d196581.1
        if (ivStatelessWrappers != null &&
            ivStatelessWrappers.inCache()) {
            result = (EJSLocalWrapper) ivStatelessWrappers.getLocalObject();
        } else {
            // The singleton wrapper has either not been created yet, or is not
            // currently in the wrapper cache.  So, getting it from the Wrapper
            // Manager will cause it to be created (if needed) and faulted into
            // the Wrapper Cache.                                         d196581.1

            // Use a cached instance beanId... for stateless and message beans all
            // instances have the same id.                               d140003.26

            if (statelessSessionHome || messageDrivenHome) {
                // Since this is is one of the home types with a singleton wrapper,
                // then set the local cached value now that it has been created
                // and inserted into the Wrapper Cache.  It cannot be cached
                // locally until inserted into the Wrapper Cache, as the insert
                // updates the wrapper with a reference to the bucket.       d233983
                ivStatelessWrappers = wrapperManager.getWrapper(ivStatelessId);

                result = (EJSLocalWrapper) ivStatelessWrappers.getLocalObject();
            }
        }

        return result;
    }

    /**
     * Returns an Object (wrapper) representing the specified EJB 3.0
     * Business Local or Remote Interface, managed by this home. <p>
     *
     * This method provides a Business Object (wrapper) factory capability,
     * for use during business interface injection or lookup. It is
     * called directly for Stateless Session beans, or through the
     * basic wrapper for Stateful Session beans. <p>
     *
     * For Stateless Session beans, a 'singleton' (per home) wrapper instance
     * is returned, since all Stateless Session beans are interchangeable.
     * Since no customer code is invoked, no pre or postInvoke calls are
     * required. <p>
     *
     * For Stateful Session beans, a new instance of a Stateful Session bean
     * is created, and the corresponding new wrapper instance is returned.
     * Since a new instance will be constructed, this method must be wrapped
     * with pre and postInvoke calls. <p>
     *
     * @param businessInterfaceName One of the local business interfaces or remote
     *            business interfaces for this session bean.
     * @param useSupporting whether or not to try to match the passed in interface
     *            to a known sublcass (ejb-link / beanName situation)
     *
     * @return The business object (wrapper) corresponding to the given
     *         business interface.
     * @throws ClassNotFoundException if the passed in interface could not
     *             be loaded
     * @throws EJBConfigurationException if the bean is mis-configured
     */
    // d366807.4
    public Object createBusinessObject(String businessInterfaceName,
                                       boolean useSupporting) throws CreateException, RemoteException, ClassNotFoundException, EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createBusinessObject: " + businessInterfaceName); // d367572.7

        int localIndex = beanMetaData.getLocalBusinessInterfaceIndex(businessInterfaceName);
        if (localIndex != -1) {
            return createLocalBusinessObject(localIndex, null);
        }

        int remoteIndex = beanMetaData.getRemoteBusinessInterfaceIndex(businessInterfaceName);
        if (remoteIndex != -1) {
            container.getEJBRuntime().checkRemoteSupported(this, businessInterfaceName);
            return createRemoteBusinessObject(remoteIndex, null);
        }

        if (useSupporting) {
            Class<?> target = beanMetaData.classLoader.loadClass(businessInterfaceName);
            localIndex = beanMetaData.getAssignableLocalBusinessInterfaceIndex(target);
            remoteIndex = beanMetaData.getAssignableRemoteBusinessInterfaceIndex(target);

            if (localIndex != -1) {
                if (remoteIndex != -1) {
                    Tr.error(tc, "AMBIGUOUS_REFERENCE_TO_DUPLICATE_INTERFACE_CNTR0155E",
                             new Object[] { beanMetaData.enterpriseBeanName,
                                            beanMetaData._moduleMetaData.ivName,
                                            businessInterfaceName });
                    EJBConfigurationException ejbex = new EJBConfigurationException("Another component has an ambiguous reference to interface: " + businessInterfaceName +
                                                                                    " which has both local and remote implementions on bean: " + j2eeName);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "createBusinessObject : " + ejbex);

                    throw ejbex;
                }

                return createLocalBusinessObject(localIndex, null);
            }

            if (remoteIndex == -1) {
                Tr.error(tc, "ATTEMPT_TO_REFERENCE_MISSING_INTERFACE_CNTR0154E",
                         new Object[] { beanMetaData.enterpriseBeanName,
                                        beanMetaData._moduleMetaData.ivName,
                                        businessInterfaceName });
                EJBConfigurationException ejbex = new EJBConfigurationException("Another component is attempting to reference local interface: " + businessInterfaceName +
                                                                                " which is not implemented by bean: " + j2eeName);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "createBusinessObject : " + ejbex);

                throw ejbex;
            }

            container.getEJBRuntime().checkRemoteSupported(this, businessInterfaceName);
            return createRemoteBusinessObject(remoteIndex, null);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "createBusinessObject : IllegalStateException : " +
                             "Requested business interface not found : " +
                             businessInterfaceName);

            throw new IllegalStateException("Requested business interface not found : " + businessInterfaceName);
        }
    }

    /**
     * Returns an Object (wrapper) representing the specified EJB 3.0
     * Business Local Interface, managed by this home. <p>
     *
     * This method provides a Business Object (wrapper) factory capability,
     * for use during business interface injection or lookup. It is
     * called directly for Stateless Session beans, or through the
     * basic wrapper for Stateful Session beans. <p>
     *
     * For Stateless Session beans, a 'singleton' (per home) wrapper instance
     * is returned, since all Stateless Session beans are interchangeable.
     * Since no customer code is invoked, no pre or postInvoke calls are
     * required. <p>
     *
     * For Stateful Session beans, a new instance of a Stateful Session bean
     * is created, and the corresponding new wrapper instance is returned.
     * Since a new instance will be constructed, this method must be wrapped
     * with pre and postInvoke calls. <p>
     *
     * @param interfaceName One of the local business interfaces for this
     *            session bean
     * @param useSupporting whether or not to try to match the passed in interface
     *            to a known sublcass (ejb-link / beanName situation)
     * @return The business object (wrapper) corresponding to the given
     *         business interface.
     *
     * @throws RemoteException
     * @throws CreateException
     * @throws ClassNotFoundException
     * @throws EJBConfigurationException
     */
    public Object createLocalBusinessObject(String interfaceName,
                                            boolean useSupporting) throws RemoteException, CreateException, ClassNotFoundException, EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createLocalBusinessObject: " + interfaceName); // d367572.7

        int interfaceIndex;
        if (useSupporting) {
            interfaceIndex = beanMetaData.getSupportingLocalBusinessInterfaceIndex(interfaceName);
        } else {
            interfaceIndex = beanMetaData.getRequiredLocalBusinessInterfaceIndex(interfaceName);
        }

        Object result = createLocalBusinessObject(interfaceIndex, null);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc,
                    "createLocalBusinessObject returning: " + Util.identity(result)); // d367572.7

        return result;
    }

    /**
     * Method to create a local business reference object. Once the index
     * of the interface is found, it is then passed through the EJSWrapperCommon
     * for the rest of the logic.
     *
     * @param interfaceIndex the local business interface index
     * @param context the context for creating the object, or null
     */
    public Object createLocalBusinessObject(int interfaceIndex, ManagedObjectContext context) throws RemoteException, CreateException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createLocalBusinessObject: " + interfaceIndex); // d367572.7

        Object result;

        EJSWrapperCommon commonWrapper = createBusinessObjectWrappers(context);
        result = commonWrapper.getLocalBusinessObject(interfaceIndex);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createLocalBusinessObject returning: " + Util.identity(result)); // d367572.7

        return result;
    }

    /**
     * Returns an Object (wrapper) representing the specified EJB 3.0
     * Business Remote Interface, managed by this home. <p>
     *
     * This method provides a Business Object (wrapper) factory capability,
     * for use during business interface injection or lookup. It is
     * called directly for Stateless Session beans, or through the
     * basic wrapper for Stateful Session beans. <p>
     *
     * For Stateless Session beans, a 'singleton' (per home) wrapper instance
     * is returned, since all Stateless Session beans are interchangeable.
     * Since no customer code is invoked, no pre or postInvoke calls are
     * required. <p>
     *
     * For Stateful Session beans, a new instance of a Stateful Session bean
     * is created, and the corresponding new wrapper instance is returned.
     * Since a new instance will be constructed, this method must be wrapped
     * with pre and postInvoke calls. <p>
     *
     * @param interfaceName One of the remote business interfaces for this
     *            session bean
     * @param useSupporting whether or not to try to match the passed in interface
     *            to a known sublcass (ejb-link / beanName situation)
     * @return The business object (wrapper) corresponding to the given
     *         business interface.
     *
     * @throws RemoteException
     * @throws CreateException
     * @throws ClassNotFoundException
     * @throws EJBConfigurationException
     */
    public Object createRemoteBusinessObject(String interfaceName,
                                             boolean useSupporting) throws RemoteException, CreateException, ClassNotFoundException, EJBConfigurationException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createRemoteBusinessObject: " + interfaceName); // d367572.7

        int interfaceIndex;
        if (useSupporting) {
            interfaceIndex = beanMetaData.getSupportingRemoteBusinessInterfaceIndex(interfaceName);
        } else {
            interfaceIndex = beanMetaData.getRemoteBusinessInterfaceIndex(interfaceName);

            if (interfaceIndex == -1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "createRemoteBusinessObject : IllegalStateException : " +
                                 "Requested business interface not found : " +
                                 interfaceName);

                throw new IllegalStateException("Requested business interface not found : " + interfaceName);

            }
        }

        Object result = createRemoteBusinessObject(interfaceIndex, null);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc,
                    "createRemoteBusinessObject returning: " + Util.identity(result)); // d367572.7

        return result;
    }

    /**
     * Method to create a remote home reference object. Once the client
     * reference (stub) is found, it is then passed through the EJSWrapperCommon
     * to perform a narrow.
     *
     */
    public Object createRemoteHomeObject() throws RemoteException, CreateException {
        EJSWrapperCommon wc = getWrapper();
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createRemoteHomeObject: " + wc);

        Object result;

        Object stub = getContainer().getEJBRuntime().getRemoteReference(wc.getRemoteWrapper());
        result = wc.getRemoteHomeObject(stub, getBeanMetaData().homeInterfaceClass);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc,
                    "createRemoteHomeObject returning: " + Util.identity(result));
        return result;
    }

    /**
     * Method to create a remote business reference object. Once the index
     * of the interface is found, it is then passed through the EJSWrapperCommon
     * for the rest of the logic.
     *
     * @param interfaceIndex the remote business interface index
     * @param context the context for creating the object, or null
     */
    public Object createRemoteBusinessObject(int interfaceIndex, ManagedObjectContext context) throws RemoteException, CreateException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createRemoteBusinessObject: " + interfaceIndex); // d367572.7

        Object result;

        EJSWrapperCommon commonWrapper = createBusinessObjectWrappers(context);
        result = commonWrapper.getRemoteBusinessObject(interfaceIndex);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc,
                    "createRemoteBusinessObject returning: " + Util.identity(result)); // d367572.7

        return result;
    }

    /**
     * Returns an Object (wrapper) representing all of the EJB 3.0+
     * business local interfaces managed by this home. <p>
     *
     * This method provides a Business Object (wrapper) factory capability,
     * for use during business interface injection or lookup. It is
     * called directly for Stateless Session beans, or through the
     * basic wrapper for Stateful Session beans. <p>
     *
     * For Stateless Session beans, a 'singleton' (per home) wrapper instance
     * is returned, since all Stateless Session beans are interchangeable.
     * Since no customer code is invoked, no pre or postInvoke calls are
     * required. <p>
     *
     * For Stateful Session beans, a new instance of a Stateful Session bean
     * is created, and the corresponding new wrapper instance is returned.
     * Since a new instance will be constructed, this method must be wrapped
     * with pre and postInvoke calls. <p>
     *
     * @param context the context for creating the object, or null
     * @return The business object (wrapper) that is an aggregate of all
     *         business local interfaces.
     * @throws CreateException if an application-level failure occurs creating
     *             an instance of the bean.
     * @throws EJBException if a failure occurs attempting to generate the
     *             aggregate wrapper class or create an instance of it.
     */
    // F743-34304
    public Object createAggregateLocalReference(ManagedObjectContext context) throws CreateException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createAggregateLocalReference: " + beanMetaData.j2eeName);
        Object result = null;

        try {
            EJSWrapperCommon wrappers = createBusinessObjectWrappers(context);
            result = wrappers.getAggregateLocalWrapper();

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createAggregateLocalReference: " + Util.identity(result));

            return result;
        } catch (RemoteException rex) {
            FFDCFilter.processException(rex, CLASS_NAME + ".createAggregateLocalReference",
                                        "1697", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createAggregateLocalReference: " + Util.identity(result));
            throw ExceptionUtil.EJBException("Failed to create aggregate local reference: ", rex);
        }
    }

    /**
     * Common code used by methods that need to create a business object
     * and return one of the wrapper types. <p>
     *
     * @param context the context for creating the object, or null
     * @return the set of all wrappers for the created business object.
     * @throws CreateException if an application-level failure occurs creating
     *             an instance of the bean.
     * @throws RemoteException if a failure occurs accessing the wrapper cache.
     * @throws EJBException if a failure occurs attempting to generate the
     *             aggregate wrapper class or create an instance of it.
     */
    // F743-34304
    public EJSWrapperCommon createBusinessObjectWrappers(ManagedObjectContext context) throws CreateException, RemoteException {
        homeEnabled();
        EJSWrapperCommon result = null;

        //------------------------------------------------------------------------
        // This is the method used by stateless session beans home create methods
        // to check and see if their singleton wrapper is already registered.
        //------------------------------------------------------------------------

        if ((ivSingletonSessionHome) || // F743-508
            (statelessSessionHome)) {
            // If the wrapper has been created, and is still in the wrapper cache,
            // then just return the remote wrapper from the singleton WrapperCommon.
            // Note that 'inCache' will 'touch' the wrapper in the Wrapper Cache,
            // updating the LRU data and preventing it from unregistering with
            // the ORB.
            if (ivStatelessWrappers != null &&
                ivStatelessWrappers.inCache()) {
                // Calling getRemoteWrapper will cause the wrapper to register with
                // the ORB, if that hasn't already been done.
                result = ivStatelessWrappers;
            } else {
                // The stateless wrapper (which is a singleton) has either not been
                // created yet, or is not currently in
                // the wrapper cache.  So, getting it from the Wrapper
                // Manager will cause it to be created (if needed) and faulted into
                // the Wrapper Cache, and then getting the Remote wrapper will cause
                // it to be registered with the ORB.

                // Use a cached instance beanId... for stateless and message beans all
                // instances have the same id.

                // Since this is is one of the home types with a singleton wrapper,
                // then set the local cached value now that it has been created
                // and inserted into the Wrapper Cache.  It cannot be cached
                // locally until inserted into the Wrapper Cache, as the insert
                // updates the wrapper with a reference to the bucket.
                ivStatelessWrappers = wrapperManager.getWrapper(ivStatelessId);

                result = ivStatelessWrappers;
            }
        }

        //------------------------------------------------------------------------
        // This is the home create method for stateful session beans, and
        // constructs a new instance of the Stateful Session bean.
        //
        // Basically the same as a combined generated home wrapper and home bean
        // create method method, except there is no ejbCreate method to call;
        // ejbCreate is only used for the EJB 2.x component interfaces.  d369262.5
        //
        //------------------------------------------------------------------------

        else if (statefulSessionHome) {
            EJSDeployedSupport s = new EJSDeployedSupport();

            // BEGIN home wrapper
            try {
                container.preInvoke(ivStatefulBusinessHomeWrapper, 0, s);

                // BEGIN home create
                BeanO beanO = null;
                boolean exceptionOccurred = false;
                boolean preEjbCreateCalled = false;
                try {
                    // NOTE: Deployed code calls no-arg createBeanO().
                    beanO = createBeanO(s.ivThreadData, s.currentTx, false, context); // d630940
                    preEjbCreateCalled = preEjbCreate(beanO);
                    // Note: There is no ejbCreate for business interfaces.
                    result = postCreateCommon(beanO, beanO.getId(), false); // d618887
                    // This must be be the last action of this try block because if
                    // anything throws, createFailure is called and we should not pop
                    // twice.
                    EJSContainer.getThreadData().popCallbackBeanO();
                } catch (CreateException createexception) {
                    exceptionOccurred = true;
                    throw createexception;
                } catch (Throwable throwable) {
                    exceptionOccurred = true;
                    throw ExceptionUtil.EJBException("Create failed", throwable);
                } finally {
                    if (exceptionOccurred)
                        createFailure(beanO);
                    else if (preEjbCreateCalled)
                        afterPostCreateCompletion(beanO);
                }
                // END home create
            } catch (CreateException createexception) {
                s.setCheckedException(createexception);
                throw createexception;
            } catch (Throwable throwable) {
                s.setUncheckedLocalException(throwable);
            } finally {
                try {
                    container.postInvoke(ivStatefulBusinessHomeWrapper, 0, s);
                } catch (Throwable throwable) {
                    s.setUncheckedLocalException(throwable);
                }
            }
            // END home wrapper
        }

        return result;
    }

    @Override
    public EJSWrapperCommon internalCreateWrapper(BeanId id) // f111627
                    throws RemoteException {
        homeEnabled();

        EJSWrapperCommon wrappers;

        // For Stateless Session, MessageDriven, and WebService Enpoints, the
        // wrappers for bean instances are singletons, and so that singleton
        // is cached on the home, to avoid Wrapper Cache lookups.  However, the
        // wrappers are still held in the wrapper cache to insure the remote
        // wrapper is registered with the ORB properly. If the wrappers are ever
        // evicted from the Wrapper Cache, then this method will be called to
        // fault it back in, and may just return that singleton.         d196581.1
        if (ivStatelessWrappers != null) {
            wrappers = ivStatelessWrappers;
        } else {
            // This home does not have a singleton wrapper, or it is not been
            // created yet, so create a new one now.
            wrappers = new EJSWrapperCommon(remoteEJBObjectClass, // f111627
                            localEJBObjectClass, // f111627
                            id, // f111627
                            beanMetaData, // f111627
                            pmiBean, // d140003.33
                            container, // f111627
                            wrapperManager, // f111627
                            false); // f111627
        }

        return wrappers;
    } // createWrapper

    /**
     * Return true iff this home contains message driven beans. <p>
     */
    @Override
    public final boolean isMessageDrivenHome() {
        return messageDrivenHome;
    }

    /**
     * Return true iff this home contains singleton session bean. <p>
     */
    @Override
    public final boolean isSingletonSessionHome() //d565527
    {
        return ivSingletonSessionHome;
    }

    /**
     * Return true iff this home contains stateless session beans. <p>
     */
    @Override
    public final boolean isStatelessSessionHome() {
        return statelessSessionHome;
    }

    /**
     * Return true iff this home contains stateful session beans. <p>
     */
    @Override
    public final boolean isStatefulSessionHome() {
        return statefulSessionHome;
    }

    /**
     * Return true iff this home contains managed beans. <p>
     */
    // F743-34301
    public boolean isManagedBeanHome() {
        return false;
    }

    public EJSContainer getContainer() {
        return container;
    }

    @Override
    public BeanMetaData getBeanMetaData(Object homeKey) {
        homeEnabled();
        return beanMetaData;
    }

    // LIDB2775-23.0/LIDB2775-23.1 Begins
    public BeanMetaData getBeanMetaData() {
        homeEnabled();
        return beanMetaData;
    }

    // LIDB2775-23.0/LIDB2775-23.1 Ends

    @Override
    public ClassLoader getClassLoader() {
        homeEnabled();
        return beanMetaData.classLoader;
    }

    /**
     * Return the ActivationStrategy for beans associated with
     * this home. <p>
     */
    @Override
    public final ActivationStrategy getActivationStrategy() {
        return activationStrategy;
    }

    /**
     * Return the method name for the given method id.
     */
    @Override
    public final String getMethodName(Object homeKey, int id, boolean isHome) {
        homeEnabled();
        if (isHome) {
            return beanMetaData.homeMethodNames[id];
        } else {
            return beanMetaData.methodNames[id];
        }
    }

    /**
     * Return the name of the class that implements the bean's owned
     * by the given home.
     */
    @Override
    public String getEnterpriseBeanClassName(Object homeKey) {
        homeEnabled();
        return beanMetaData.enterpriseBeanClass.getName();
    } // getEnterpriseBeanClassName

    /**
     * Create a Handle for the given BeanId
     */
    @Override
    public final Handle createHandle(BeanId id) throws RemoteException {
        EJSWrapper wrapper = null;
        homeEnabled();

        wrapper = getWrapper(id).getRemoteWrapper(); // f111627; //p116577

        if (!statelessSessionHome && !statefulSessionHome) {
            if (cvUsePortableClass) //p125891
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "createHandle():Creating portable HandleImpl"); //p125891

                // p116577 - start of change
                // Need to make EntityHandle class use only
                // SUN JDK or Java EE interfaces.  So, we need to pass EJBObject
                // stub to the EntityHandle constructor.
                return new HandleImpl((EJBObject) PortableRemoteObject.toStub(wrapper)); // F743-509.CodRev
                // p116577 - end of change
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "createHandle():Creating non-portable EntityHandle"); //p125891
                Properties pFake = null;
                return new EntityHandle(id, beanMetaData, pFake); //p125891 //d145386
            }
        } else {
            Object wrapperRef = container.getEJBRuntime().getRemoteReference(wrapper);
            EJBObject object = (EJBObject) PortableRemoteObject.narrow(wrapperRef, EJBObject.class);
            return container.sessionHandleFactory.create(object);
        }
    }

    /**
     * Activates and returns the EJSWrapperCommon associated with the
     * specified BeanId. <p>
     *
     * If the bean defined by beanId is already active then returns
     * the active instance. Otherwise, the bean is activated (ejbActivate)
     * and loaded (ejbLoad). <p>
     *
     * This method is intended to be called by the FinderResultServerImpl
     * for multi-row finder collection processing. This applies to CMP 2.x
     * (with and without inheritance). <p>
     *
     * @param beanId the <code>BeanId</code> identifying the bean to
     *            activate.
     *
     * @return the EJSWrapperCollection associated with the specified
     *         beanid.
     *
     * @exception CSIException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key).
     * @exception RemoteException thrown if a system exception occurs while
     *                trying to locate the <code>EJBObject</code> instance
     *                corresponding to the primary key.
     */
    // d146034.6
    public EJSWrapperCommon activateBean(BeanId beanId,
                                         ContainerTx currentTx) throws CSIException, RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "activateBean (" + beanId + ")", currentTx);

        homeEnabled();

        EJSWrapperCommon result = null;
        try {
            // For Single-object finder methods (other than findByPrimaryKey),
            // the Persistence Manager is required to perform a flush of the
            // Entity beans prior to the query.  If this flush was not
            // performed, then throw an exception indicating the tx is in an
            // illegal state.
            if (currentTx.ivFlushRequired) {
                IllegalStateException isex = new IllegalStateException("Persistence Manager failed to perform synchronization " +
                                                                       "of Entity beans prior to find<METHOD>");

                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "activateBean", isex);

                throw isex;
            }

            // Look to see if this beanId is already cached with its serialized
            // byte array to improve performance. This also causes the beanId
            // in the wrapper to == the one in the BeanO.
            beanId = wrapperManager.beanIdCache.find(beanId);

            container.activator.activateBean(EJSContainer.getThreadData(), currentTx, beanId); // d630940
            result = wrapperManager.getWrapper(beanId); // d156807.1
        } catch (NoSuchObjectException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".activateBean",
                                        "998", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "activateBean : NoSuchObjectException", ex);

            // Something very odd has happened.  The collection does not care
            // that the object doesn't exist, nor should it have to deal with
            // it, so just create a wrapper instead of activating.....
            result = getWrapper(beanId);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "activateBean", result);

        return result;
    } // activateBean

    /**
     * Activates and returns the remote <code>EJBObject</code> associated
     * with the specified primary key for this home. <p>
     *
     * If the bean defined by primaryKey is already active then returns
     * the active instance. Otherwise, the bean is activated (ejbActivate)
     * and loaded (ejbLoad). <p>
     *
     * This method is intended to be called by the deployed/generated
     * findByPrimaryKey method on the home. This applies to BMP,
     * CMP 2.x (with and without inheritance) and CMP 1.x (without
     * inheritance). <p>
     *
     * For CMP 1.x with inheritance, refer to getBean(key, data) and
     * getBean(type, key, data). <p>
     *
     * @param primaryKey the <code>Object</code> containing the primary
     *            key of the <code>EJBObject</code> to return.
     *
     * @return the <code>EJBObject</code> associated with the specified
     *         primary key.
     *
     * @exception FinderException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key).
     * @exception RemoteException thrown if a system exception occurs while
     *                trying to locate the <code>EJBObject</code> instance
     *                corresponding to the primary key.
     */
    @Override
    public EJBObject activateBean(Object primaryKey) throws FinderException, RemoteException {
        EJSWrapperCommon wrappers = null; // d215317

        // Single-object ejbSelect methods may result in a null value,
        // and since this code path also supports ejbSelect, null must
        // be tolerated and returned.                                      d149627
        if (primaryKey == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "activateBean : key = null, returning null");
            return null;
        }

        // If noLocalCopies and allowPrimaryKeyMutation are true then we
        // must create a deep copy of the primaryKey object to avoid data
        // corruption.                                             PQ62081 d138865
        // The copy will actually be made in activateBean_Common only if the
        // primary key is used to insert into the cache, but the determination
        // must be done here, as it is different for local and remote.     d215317
        // "Primary Key" should not be copied for a StatefulSession bean.  d247634
        boolean pkeyCopyRequired = false;
        if (noLocalCopies && allowPrimaryKeyMutation && !statefulSessionHome) {
            pkeyCopyRequired = true;
        }

        wrappers = ivEntityHelper.activateBean_Common(this, primaryKey, pkeyCopyRequired);

        return wrappers.getRemoteWrapper(); // f111627
    } // activateBean

    /**
     * Activates and returns the local <code>EJBObject</code> associated
     * with the specified primary key for this home. <p>
     *
     * If the bean defined by primaryKey is already active then returns
     * the active instance. Otherwise, the bean is activated (ejbActivate)
     * and loaded (ejbLoad). <p>
     *
     * This method is intended to be called by the deployed/generated
     * findByPrimaryKey method on the home. This applies to BMP,
     * CMP 2.x (with and without inheritance) and CMP 1.x (without
     * inheritance). <p>
     *
     * For CMP 1.x with inheritance, refer to getBean(key, data) and
     * getBean(type, key, data). <p>
     *
     * @param primaryKey the <code>Object</code> containing the primary
     *            key of the <code>EJBObject</code> to return.
     *
     * @return the <code>EJBObject</code> associated with the specified
     *         primary key.
     *
     * @exception FinderException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key).
     * @exception RemoteException thrown if a system exception occurs while
     *                trying to locate the <code>EJBObject</code> instance
     *                corresponding to the primary key.
     */
    // f111627
    @Override
    public EJBLocalObject activateBean_Local(Object primaryKey) throws FinderException, RemoteException {
        EJSWrapperCommon wrappers = null; // d215317

        // Single-object ejbSelect methods may result in a null value,
        // and since this code path also supports ejbSelect, null must
        // be tolerated and returned.                                      d149627
        if (primaryKey == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "activateBean_Local : key = null, returning null");
            return null;
        }

        // Make a "deep copy" of the primarykey object if the noPriaryKeyMutation
        // property is false. This property can be set to "true" by the customer
        // to increase performance, however they must guarentee that their code
        // will no mutate existing primary key objects.                  d138865.1
        // The copy will actually be made in activateBean_Common only if the
        // primary key is used to insert into the cache, but the determination
        // must be done here, as it is different for local and remote.     d215317
        // "Primary Key" should not be copied for a StatefulSession bean.  d247634
        boolean pkeyCopyRequired = false;
        if (!noPrimaryKeyMutation && !statefulSessionHome) {
            pkeyCopyRequired = true;
        }

        wrappers = ivEntityHelper.activateBean_Common(this, primaryKey, pkeyCopyRequired);

        return wrappers.getLocalObject();
    } // f111627

    @Override
    public EJBObject getBeanWrapper(Object primaryKey) throws FinderException, RemoteException {
        BeanId id = new BeanId(this, (Serializable) primaryKey, false);
        return getWrapper(id).getRemoteWrapper(); // f111627
    }

    /**
     * Return the <code>EJBObject</code> associated with the specified
     * type, primary key and data. <p>
     *
     * If the bean defined by the (type, primaryKey) tuple is already
     * active then return the active instance. <p>
     *
     * Otherwise, activate the bean and hydrate it with the given data. <p>
     *
     * @param type a <code>String</code> defining the most-specific
     *            type of the bean being activated <p>
     *
     * @param primaryKey the <code>Object</code> containing the primary
     *            key of the <code>EJBObject</code> to return <p>
     *
     * @param data an <code>Object</code> containing the data to use
     *            to hydrate bean if necessary <p>
     *
     * @return the <code>EJBObject</code> associated with the specified
     *         primary key <p>
     *
     * @exception FinderException thrown if a finder-specific
     *                error occurs (such as no object with corresponding
     *                primary key) <p>
     *
     * @exception RemoteException thrown if a system
     *                exception occurs while trying to activate the
     *                <code>BeanO</code> instance corresponding
     */
    public EJBObject getBean(String type, Object primaryKey, Object data) throws FinderException, RemoteException {
        return ivEntityHelper.getBean(this, type, primaryKey, data);
    } // getBean

    /**
     * Returns true if inheritance has been defined for this home; otherwise
     * returns false.
     *
     * Inheritance is defined in the deployment descriptor as
     * 'generalizations', and indicates that the finder methods on the home
     * may return subclasses of the bean class for which this is a home.
     *
     * This method is intended for use by the FinderHelper to determine if
     * beans need to be activated for a greedy enumerator.
     **/
    // PQ57446
    @Override
    public final boolean hasInheritance() {
        return homeRecord.ivHasInheritance; // d154342.9 , LIDB859-4.1
    }

    @Override
    public boolean isChild() {
        return homeRecord.ivIsChild;
    } // d112604.1  begin

    /**
     * Return the <code>EJBObject</code> associated with the specified
     * primary key if it exists within the cache for this home or
     * a subclass/child home if inheritance has been defined.
     * Null will be returned if the bean cannot be found in the cache. <p>
     *
     * For caching strategies which allow muiltiple instances of the same
     * entity bean (such as Options B and C), then a transactional lookup
     * will occur. In this situation, only beans enlisted in the current
     * transaction will be found. For caching strategies which allow only
     * a single instance of a given bean (such as Option A), a transactional
     * lookup is not perormed, and an <code>EJBObject</code> is returned if
     * the bean is found, even though it may be enlisted with another
     * transaction. The bean will not be enlisted with the current
     * transaction as a result of this method call. Basically, the bean
     * may be found if it may be enlisted with the transaction without
     * being re-loaded from the database. <p>
     *
     * This method is primarily intended for use by the generated persister
     * code for EJB 1.1 Entity Beans, specifically when inheritance is
     * involved. It is also used by the generated findByPrimaryKey method
     * on a BMP Home, for EJB 2.0 and beyond. It provides a mechanism for
     * the generated BMP Home or CMP 1.1 persister
     * <code>findByPrimaryKey()</code> method to look in the cache
     * prior to performing a database query. <p>
     *
     * When CMP 1.1 and inheritance is not involved, {@link #activateBean(Object)
     * activateBean()} should be used instead. <p>
     *
     * @param primaryKey the <code>Object</code> containing the primary
     *            key of the <code>EJBObject</code> to return.
     *
     * @return the <code>EJBObject</code> associated with the specified
     *         primary key or null if the bean cannot be found in the cache.
     *
     * @exception RemoteException thrown if a system exception occurs while
     *                trying to locate the <code>EJBObject</code> instance
     *                corresponding to the primary key within the cache.
     **/
    // d116859
    @Override
    public EJBObject getBean(Object primaryKey) throws RemoteException {
        return ivEntityHelper.getBean(this, primaryKey);
    } // getBean

    /**
     * Return the <code>EJBLocalObject</code> associated with the specified
     * primary key if it exists within the cache for this home or
     * a subclass/child home if inheritance has been defined.
     * Null will be returned if the bean cannot be found in the cache. <p>
     *
     * For caching strategies which allow muiltiple instances of the same
     * entity bean (such as Options B and C), then a transactional lookup
     * will occur. In this situation, only beans enlisted in the current
     * transaction will be found. For caching strategies which allow only
     * a single instance of a given bean (such as Option A), a transactional
     * lookup is not perormed, and an <code>EJBLocalObject</code> is returned if
     * the bean is found, even though it may be enlisted with another
     * transaction. The bean will not be enlisted with the current
     * transaction as a result of this method call. Basically, the bean
     * may be found if it may be enlisted with the transaction without
     * being re-loaded from the database. <p>
     *
     * This method is primarily intended for use by the generated
     * findByPrimaryKey method on a BMP Home, for EJB 2.0 and beyond.
     * It provides a mechanism for the generated
     * <code>findByPrimaryKey()</code> method to look in the cache
     * prior to performing a database query. <p>
     *
     * @param primaryKey the <code>Object</code> containing the primary
     *            key of the <code>EJBObject</code> to return.
     *
     * @return the <code>EJBLocalObject</code> associated with the specified
     *         primary key or null if the bean cannot be found in the cache.
     *
     * @exception RemoteException thrown if a system exception occurs while
     *                trying to locate the <code>EJBObject</code> instance
     *                corresponding to the primary key within the cache.
     **/
    // d140003.14
    public EJBLocalObject getBean_Local(Object primaryKey) throws RemoteException {
        return ivEntityHelper.getBean_Local(this, primaryKey);
    } // getBean_Local

    /**
     * Complete the bean creation process and return wrapper for
     * newly created bean. The actual database store may take place in
     * <code>afterPostCreate()</code>.<p>
     *
     * @param beanO the <code>BeanO</code> being created <p>
     *
     * @param primaryKey an <code>Object</code> containing the
     *            primary key of the bean being created <p>
     *
     * @param inSupportOfEJBPostCreateChanges a <code>boolean</code> which is set to
     *            true if database inserts in ejbPostCreate will
     *            be supported. <p>
     *
     * @return <code>EJBObject</code> for newly created bean <p>
     *
     * @exception CreateException thrown if create-specific
     *                error occurs <p>
     *
     * @exception RemoteException thrown if a container
     *                error occurs <p>
     */
    // d142250
    public EJBObject postCreate(BeanO beanO,
                                Object primaryKey,
                                boolean inSupportOfEJBPostCreateChanges) throws CreateException, RemoteException {

        boolean supportEJBPostCreateChanges = inSupportOfEJBPostCreateChanges && !ivAllowEarlyInsert; //PQ89520

        // If noLocalCopies and allowPrimaryKeyMutation are true then we
        // must create a deep copy of the primaryKey object to avoid data
        // corruption.                                             PQ62081 d138865
        // "Primary Key" should not be copied for a StatefulSession bean.  d247634
        if (noLocalCopies && allowPrimaryKeyMutation && !statefulSessionHome) {
            try {
                primaryKey = container.ivObjectCopier.copy((Serializable) primaryKey); // RTC102299
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".postCreate",
                                            "1551", this);
                ContainerEJBException ex = new ContainerEJBException("postCreate failed attempting to process PrimaryKey", t);
                Tr.error(tc,
                         "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                         new Object[] { t, ex.toString() }); // d194031
                throw ex;
            }
        }

        EJSWrapperCommon common = postCreateCommon(beanO, primaryKey,
                                                   supportEJBPostCreateChanges); // d142250
        EJSWrapper w = common.getRemoteWrapper(); // f111627

        // Pop the callback bean if afterPostCreate will not be called.  This
        // must be be the last action of this method because if anything throws,
        // createFailure is called and we should not pop twice.
        if (!inSupportOfEJBPostCreateChanges || statefulSessionHome) {
            EJSContainer.getThreadData().popCallbackBeanO();
        }

        return w;
    }

    /**
     * Complete the bean creation process. <p>
     *
     * @param beanO the <code>BeanO</code> being created <p>
     *
     * @exception CreateException thrown if create-specific
     *                error occurs <p>
     *
     * @exception RemoteException thrown if a container
     *                error occurs <p>
     */
    public EJBObject postCreate(BeanO beanO) throws CreateException, RemoteException {
        homeEnabled();
        return postCreate(beanO, beanO.getId(), false); // d142250
    } // postCreate

    /**
     * Complete the bean creation process. <p>
     *
     * @param beanO the <code>BeanO</code> being created <p>
     *
     * @param supportEJBPostCreateChanges a <code>boolean</code> which is set to
     *            true if database inserts in ejbPostCreate will
     *            be supported. <p>
     *
     * @exception CreateException thrown if create-specific
     *                error occurs <p>
     *
     * @exception RemoteException thrown if a container
     *                error occurs <p>
     */
    // d142250
    public EJBObject postCreate(BeanO beanO, boolean supportEJBPostCreateChanges) throws CreateException, RemoteException {
        homeEnabled();
        return postCreate(beanO, beanO.getId(), supportEJBPostCreateChanges);
    } // postCreate

    /**
     * Complete the bean creation process. <p>
     *
     * @param beanO the <code>BeanO</code> being created <p>
     *
     * @exception CreateException thrown if create-specific
     *                error occurs <p>
     *
     * @exception RemoteException thrown if a container
     *                error occurs <p>
     */
    public EJBObject postCreate(BeanO beanO, Object primaryKey) throws CreateException, RemoteException {
        homeEnabled();
        return postCreate(beanO, primaryKey, false); //142250
    } // postCreate

    /**
     * Internal method that provides common implementation of both
     * postCreate and postCreate_Local. <p>
     **/
    // f111627 d142250
    private EJSWrapperCommon postCreateCommon(BeanO beanO,
                                              Object primaryKey,
                                              boolean supportEJBPostCreateChanges) throws CreateException, RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "postCreate", primaryKey);

        ContainerTx currentTx = null;

        homeEnabled();

        if (!statefulSessionHome) {
            beanO.setId(new BeanId(this, (Serializable) primaryKey, false));
        }

        currentTx = container.getCurrentContainerTx(); //d149448.1//d171654

        //----------------------------------------------------------------
        // For the option A case specifically, grab an instance lock on
        // the bean before proceeding. For other activation strategies this
        // is a no-op.  This must be done prior to calling postCreate
        // which will access the database, otherwise a deadlock may occur.
        //-----------------------------------------------------------------
        container.lockBean(beanO, currentTx); //142250.2

        beanO.postCreate(supportEJBPostCreateChanges); // d142250

        //-------------------------------------------------------------
        // Add the bean to the activator
        //
        // For now assume that there is always at most a single
        // thread executing on behalf of a given transaction. This
        // means that if the container thinks this bean already exists
        // in the context of this transaction that we must fail the
        // create with a DuplicateKeyException. Otherwise, this is the
        // first successful create of a bean with the given identity
        // in the context of this transaction.
        //-------------------------------------------------------------

        BeanO existingBeanO = container.addBean(beanO, currentTx); // d149448.1
        if (existingBeanO != null) {
            throw new DuplicateKeyException(primaryKey.toString());
        }

        // If the EJB was deployed prior to WebSphere Version 5.0, then
        // changes to EJB db field swhich are made within EJBPostCreate()
        // will be ignored.  To fix this problem the actual creation of the
        // EJB in the database is delayed until after the completion of
        // EJBPostCreate().  However we must still support EJBs which were
        // deployed in previous WebSphere versions so this code remains,
        // gated by the supportEJBPostCreateChanges flag.                  d142250
        if (!supportEJBPostCreateChanges) {
            // At this point, the deployed home has successfully created teh EB
            // so, we call the perfdata hooks for create
            if (pmiBean != null) {
                pmiBean.beanCreated();
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "postCreate", beanO);

        return wrapperManager.getWrapperForCreate(beanO); // f111627 d156807.1 F61004.6 d729903
    } // postCreateCommon

    /**
     * This method is called by home beans generated by jitdeploy when an
     * exception is thrown from {@link #createBeanO()}, {@link #preEjbCreate},
     * the Init method, or a postCreate method.
     */
    protected EJBException newCreateFailureException_Local(Throwable cause) {
        return ExceptionUtil.EJBException("Create failure", cause);
    }

    /**
     * This method is only called by entity home beans generated by ejbdeploy on
     * or after WebSphere Version 5.0. This method is only called if postCreate
     * is called with inSupportOfEJBPostCreateChanges=true and ejbPostCreate
     * returns successfully.
     *
     * @param beanO the <code>BeanO</code> being created <p>
     *
     * @param primaryKey an <code>Object</code> containing the
     *            primary key of the bean being created <p>
     *
     * @exception DuplicateKeyException thrown if EJB already
     *                exists within context of this transaction <p>
     **/
    // d142250
    public void afterPostCreate(BeanO beanO, Object primaryKey) throws CreateException, RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "afterPostCreate", primaryKey);

        if (!ivAllowEarlyInsert) //PQ89520
        {
            homeEnabled();

            beanO.afterPostCreate();

            // At this point, the deployed home has successfully created the EB
            // so, we call the perfdata hooks for create
            if (pmiBean != null) {
                pmiBean.beanCreated();
            }
        }

        // This must be be the last action of this method because if anything
        // throws, createFailure is called and we should not pop twice.
        EJSContainer.getThreadData().popCallbackBeanO();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "afterPostCreate", beanO);
    } // afterPostCreate

    /**
     * This method is only called by entity and stateful home beans generated
     * by ejbdeploy on or after WebSphere Version 5.0. This method is only
     * called if {@link #preEjbCreate} returns {@code true}. Additionally, for
     * entity home beans, this method is only called if {@link #afterPostCreate} returns successfully, and for stateful home beans, this method is only
     * called if ejbCreate returns successfully.
     */
    //d142250
    public void afterPostCreateCompletion(BeanO beanO) throws CreateException {
        // Inform the beanO that the create() is complete... the bean
        // is now in the READY state.                                    d140886.1
        beanO.afterPostCreateCompletion(); //d142250
    }

    /**
     * Complete the bean creation process and return wrapper for
     * newly created bean. <p>
     *
     * @param beanO the <code>BeanO</code> being created <p>
     *
     * @param primaryKey an <code>Object</code> containing the
     *            primary key of the bean being created <p>
     *
     * @param supportEJBPostCreateChanges a <code>boolean</code> which is set to
     *            true if database inserts in ejbPostCreate will
     *            be supported. <p>
     *
     * @return <code>EJBObject</code> for newly created bean <p>
     *
     * @exception CreateException thrown if create-specific
     *                error occurs <p>
     *
     * @exception RemoteException thrown if a container
     *                error occurs <p>
     */
    // f111627
    public EJBLocalObject postCreate_Local(BeanO beanO,
                                           Object primaryKey,
                                           boolean supportEJBPostCreateChanges) throws CreateException, ContainerException, RemoteException {
        // Make a "deep copy" of the primarykey object if the noPriaryKeyMutation
        // property is false. This property can be set to "true" by the customer
        // to increase performance, however they must guarentee that their code
        // will no mutate existing primary key objects.                  d138865.1
        // "Primary Key" should not be copied for a StatefulSession bean.  d247634
        if (!noPrimaryKeyMutation && !statefulSessionHome) {
            try {
                primaryKey = container.ivObjectCopier.copy((Serializable) primaryKey); // RTC102299
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".postCreate_Local",
                                            "1674", this);
                ContainerEJBException ex = new ContainerEJBException("postCreate_Local failed attempting to process PrimaryKey", t);
                Tr.error(tc,
                         "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                         new Object[] { t, ex.toString() }); // d194031
                throw ex;
            }
        }

        EJSWrapperCommon common = postCreateCommon(beanO, primaryKey,
                                                   supportEJBPostCreateChanges);

        EJBLocalObject w = common.getLocalObject();

        // Pop the callback bean if afterPostCreate will not be called.  This
        // must be be the last action of this method because if anything throws,
        // createFailure is called and we should not pop twice.
        if (!supportEJBPostCreateChanges || statefulSessionHome) {
            EJSContainer.getThreadData().popCallbackBeanO();
        }

        return w;
    } // postCreate_Local                                              // f111627

    /**
     * Complete the bean creation process. <p>
     *
     * @param beanO the <code>BeanO</code> being created <p>
     *
     * @exception CreateException thrown if create-specific
     *                error occurs <p>
     *
     * @exception RemoteException thrown if a container
     *                error occurs <p>
     */
    // f111627
    public EJBLocalObject postCreate_Local(BeanO beanO) throws CreateException, ContainerException, RemoteException {
        homeEnabled();
        return postCreate_Local(beanO, beanO.getId(), false);
    } // postCreate_Local

    /**
     * Complete the bean creation process. <p>
     *
     * @param beanO the <code>BeanO</code> being created <p>
     *
     * @param supportEJBPostCreateChanges a <code>boolean</code> which is set to
     *            true if database inserts in ejbPostCreate will
     *            be supported. <p>
     *
     * @exception CreateException thrown if create-specific
     *                error occurs <p>
     *
     * @exception RemoteException thrown if a container
     *                error occurs <p>
     */
    // f111627
    public EJBLocalObject postCreate_Local(BeanO beanO,
                                           boolean supportEJBPostCreateChanges) throws CreateException, ContainerException, RemoteException {
        homeEnabled();
        return postCreate_Local(beanO, beanO.getId(), supportEJBPostCreateChanges);
    } // postCreate_Local

    /**
     * Complete the bean creation process. <p>
     *
     * @param beanO the <code>BeanO</code> being created <p>
     *
     * @exception CreateException thrown if create-specific
     *                error occurs <p>
     *
     * @exception RemoteException thrown if a container
     *                error occurs <p>
     */
    // f111627
    public EJBLocalObject postCreate_Local(BeanO beanO, Object primaryKey) throws CreateException, ContainerException, RemoteException {
        homeEnabled();
        return postCreate_Local(beanO, primaryKey, false);
    } // postCreate_Local

    /**
     * Process a create failure for the given beanO. <p>
     *
     * @param beanO the <code>BeanO</code> instance that was
     *            being created <p>
     */
    public void createFailure(BeanO beanO) {
        //-----------------------------------------------------------
        // If beanO is null then it was never added to the container
        // so we don't need to do anything.
        //-----------------------------------------------------------

        if (beanO != null) {
            EJSContainer.getThreadData().popCallbackBeanO();

            beanO.destroy();
            try {
                container.removeBean(beanO);
            } catch (CSITransactionRolledbackException ex) {
                //---------------------------------------------------
                // If transaction rolled back beanO has already been
                // successfully removed from container.
                //---------------------------------------------------
                FFDCFilter.processException(ex, CLASS_NAME + ".createFailure",
                                            "1732", this);
            }
        }
    } // createFailure

    /**
     * Remove the EJB identified by the given <code>Handle</code>. <p>
     *
     * @param handle a <code>Handle</code> identifing the EJB to remove <p>
     *
     * @exception RemoteException
     * @exception RemoveException
     */
    public void remove(Handle handle) throws RemoteException, RemoveException {
        homeEnabled();

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "remove", handle);

        EJBObject ejb = handle.getEJBObject();
        ejb.remove();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "remove");
    } // remove

    /**
     * Remove the EJB identitifed by the given primary key. <p>
     *
     * @param primaryKey the <code>Object</code> containing the primary
     *            key of EJB to remove from this home <p>
     */
    public void remove(Object primaryKey) throws RemoteException, RemoveException, FinderException {
        // d141216 method rewritten to avoid calling preinvoke/postinvoke
        //         twice in code path (i.e. don't just call remove on the
        //         bean after it is activated).  With the re-write, the
        //         bean will be activated and removed in the same tran
        //         as defined by the tx attribute on home.remove().

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "remove(pk) : " + primaryKey);

        homeEnabled();

        // Session beans and MDBs cannot be removed using this method.
        // For session beans, the spec calls for throwing an exception
        // to indicate this (no PK). MDB section not specific, thus do
        // the same for consistency.

        if (statefulSessionHome || statelessSessionHome || messageDrivenHome) {
            throw new RemoveException();
        }

        BeanId beanId = ivEntityHelper.remove(this, primaryKey);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "remove(pk) : " + beanId);
    } // remove

    /**
     * This method returns an <code>EntityBeanO</code> instance whose
     * associated enterprise bean instance can be used to
     * invoke the ejbFindByPriamryKey method. <p>
     *
     * Note, this method is intended for use only on <code>EJSHome</code>
     * instances that manages entity beans in a EJB 2.0 module, using
     * bean managed persistence (BMP) or EJB 2.x container managed
     * persistence (CMP 2.x). <p>
     *
     * If this method returns successfully, the caller (the generated home bean)
     * must call {@link #releaseFinderEntityBeanO} if the method completes
     * successfully or throws FinderException, or the caller must call {@link #discardFinderEntityBeanO} if any other exception occurs.
     *
     * @return Entity bean context, from which the Entity bean instance
     *         may be obtained (and optionally activated).
     */
    // d140003.16
    public EntityBeanO getFindByPrimaryKeyEntityBeanO() throws RemoteException {
        return ivEntityHelper.getFindByPrimaryKeyEntityBeanO(this);
    } // getFindByPrimaryKeyEntityBeanO

    /**
     * This method returns an <code>EntityBeanO</code> instance whose
     * associated enterprise bean instance can be used to
     * invoke a finder method. <p>
     *
     * Note, this method must be invoked only on <code>EJSHome</code>
     * instances that manage entity beans using bean managed
     * persistence or EJB 2.x container managed persistence. <p>
     *
     * If this method returns successfully, the caller (the generated home bean)
     * must call {@link #releaseFinderEntityBeanO} if the method completes
     * successfully or throws FinderException, or the caller must call {@link #discardFinderEntityBeanO} if any other exception occurs.
     */
    // f110762.5
    public EntityBeanO getFinderEntityBeanO() throws RemoteException {
        return ivEntityHelper.getFinderEntityBeanO(this);
    } // getFinderEntityBeanO

    /**
     * Inform this home that the given <code>EntityBeanO</code>
     * encounted an unexpected exception while processing a
     * finder request, and should be transitioned to the "does
     * not exist" state. <p>
     *
     * Note, this method must be invoked only on <code>EJSHome</code>
     * instances that manage entity beans that use bean managed
     * persistence or EJB 2.x container managed persistence. <p>
     */
    // d140003.16
    public void discardFinderEntityBeanO(EntityBeanO beanO) throws RemoteException {
        ivEntityHelper.discardFinderEntityBeanO(this, beanO);
    } // discardFinderEntityBeanO

    /**
     * Inform this home that the given <code>EntityBeanO</code>
     * is no longer being used to satisfy a finder request. <p>
     *
     * Note, this method must be invoked only on <code>EJSHome</code>
     * instances that manage entity beans that use bean managed
     * persistence or EJB 2.x container managed persistence. <p>
     */
    // f110762.5
    public void releaseFinderEntityBeanO(EntityBeanO beanO) throws RemoteException {
        ivEntityHelper.releaseFinderEntityBeanO(this, beanO);
    } // releaseFinderEntityBeanO

    /**
     * This method returns a <code>BeanManagedBeanO</code> instance whose
     * associated enterprise bean instance can be used to
     * invoke a bean managed finder method. <p>
     *
     * Note, this method must be invoked only on <code>EJSHome</code>
     * instances that manage entity beans using bean managed
     * persistence. <p>
     *
     * The caller (the generated home bean) must call {@link #releaseFinderBeanO} before returning to the client.
     *
     * Maintained for EJB 1.1 compatibility. Use getFinderEntityBeano instead.
     */
    public BeanManagedBeanO getFinderBeanO() throws RemoteException {
        return ivEntityHelper.getFinderBeanO(this);
    } // getFinderBeanO

    /**
     * Inform this home that the given <code>BeanManagedBeanO</code>
     * is no longer being used to satisfy a bean managed finder request. <p>
     *
     * Note, this method must be invoked only on <code>EJSHome</code>
     * instances that manage entity beans that use bean managed
     * persistence. <p>
     *
     * Maintained for EJB 1.1 compatibility. Use releaseFinderEntityBeano instead.
     */
    public void releaseFinderBeanO(BeanManagedBeanO beanO) throws RemoteException {
        ivEntityHelper.releaseFinderBeanO(this, beanO);
    } // releaseFinderBeanO

    /**
     * This method returns an <code>EntityBeanO</code> instance whose
     * associated enterprise bean instance can be used to
     * invoke a home method. <p>
     *
     * Note, this method must be invoked only on <code>EJSHome</code>
     * instances that manage entity beans using Entity Beans.<p>
     *
     * If this method returns successfully, the caller (the generated home bean)
     * must call {@link #releaseHomeMethodEntityBeanO} after the home method has
     * been invoked. <p>
     */
    public EntityBeanO getHomeMethodEntityBeanO() throws RemoteException {
        return ivEntityHelper.getHomeMethodEntityBeanO(this);
    } // getHomeMethodEntityBeanO

    /**
     * Inform this home that the given <code>EntityBeanO</code>
     * is no longer being used to satisfy a bean managed home method request. <p>
     *
     * Note, this method must be invoked only on <code>EJSHome</code>
     * instances that manage entity beans. <p>
     */
    public void releaseHomeMethodEntityBeanO(EntityBeanO beanO) throws RemoteException {
        ivEntityHelper.releaseHomeMethodEntityBeanO(this, beanO);
    } // releaseHomeMethodEntityBeanO

    /**
     * Return an enumeration instance that wraps the given enumeration
     * of primary keys. <p>
     *
     * This helper method is used to construct the result enumeration
     * for finder methods on homes that contain container managed entity
     * beans. <p>
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Enumeration getEnumeration(Finder finder) throws FinderException, RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getEnumeration(EJSFinder)");

        homeEnabled();

        final ContainerTx currentTx = container.getCurrentContainerTx();//d171654
        Enumeration rtnEnum = container.persisterFactory.wrapResultsInEnumeration(currentTx,
                                                                                  this,
                                                                                  finder);
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getEnumeration");

        return rtnEnum;
    } // getEnumeration

    @Override
    @SuppressWarnings("rawtypes")
    public Collection getCollection(Finder finder) throws FinderException, RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getCollection(EJSFinder)");

        homeEnabled();

        final ContainerTx currentTx = container.getCurrentContainerTx();//d171654
        Collection rtnColl = container.persisterFactory.wrapResultsInCollection(currentTx,
                                                                                this, finder);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getCollection");

        return rtnColl;
    }

    /**
     * Return an enumeration instance that wraps the given enumeration
     * of primary keys. <p>
     *
     * This helper method is used to construct the result enumeration
     * for finder methods on homes that contain bean managed entity
     * beans. <p>
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Enumeration getEnumeration(Enumeration keys) throws FinderException, RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getEnumeration(Enumeration)");

        homeEnabled();

        final ContainerTx currentTx = container.getCurrentContainerTx();//d171654
        Enumeration rtnEnum = container.persisterFactory.wrapResultsInEnumeration(currentTx,
                                                                                  this, keys);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getEnumeration");

        return rtnEnum;
    } // getEnumeration

    /**
     * Return a collection instance that wraps the given collection
     * of primary keys. <p>
     *
     * This helper method is used to construct the result collection
     * for finder methods on homes that contain bean managed entity
     * beans. <p>
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Collection getCollection(Collection keys) throws FinderException, RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getCollection(Collection)");

        homeEnabled();

        final ContainerTx currentTx = container.getCurrentContainerTx();//d171654
        Collection rtnColl = container.persisterFactory.wrapResultsInCollection(currentTx,
                                                                                this, keys);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getCollection");

        return rtnColl;
    }

    // d112592 Begin
    /**
     * Return an remote enumeration instance that wraps the given enumeration
     * of primary keys. <p>
     *
     * This helper method is used to construct the result enumeration
     * for finder methods on homes that supports bean managed entity
     * beans.
     */
    @SuppressWarnings("rawtypes")
    public Enumeration getCMP20Enumeration(Enumeration keys) throws FinderException, RemoteException {
        return ivEntityHelper.getCMP20Enumeration(this, keys);
    }

    /**
     * Return an local enumeration instance that wraps the given enumeration
     * of primary keys. <p>
     *
     * This helper method is used to construct the result enumeration
     * for finder methods on homes that supports bean managed entity
     * beans.
     */
    @SuppressWarnings("rawtypes")
    public Enumeration getCMP20Enumeration_Local(Enumeration keys) throws FinderException, // d135330
                    RemoteException {
        return ivEntityHelper.getCMP20Enumeration_Local(this, keys);
    }

    /**
     * Return a remote collection instance that wraps the given collection
     * of primary keys. <p>
     *
     * This helper method is used to construct the result collection
     * for finder methods on homes that supports both container and bean
     * managed entity beans.
     */
    @SuppressWarnings("rawtypes")
    public Collection getCMP20Collection(Collection keys) throws FinderException, RemoteException {
        return ivEntityHelper.getCMP20Collection(this, keys);
    }

    /**
     * Return a local collection instance that wraps the given collection
     * of primary keys. <p>
     *
     * This helper method is used to construct the result collection
     * for finder methods on homes that supports both container and bean
     * managed entity beans.
     */
    @SuppressWarnings("rawtypes")
    public Collection getCMP20Collection_Local(Collection keys) throws FinderException, // d135330
                    RemoteException {
        return ivEntityHelper.getCMP20Collection_Local(this, keys);
    }

    // d112592 End

    /**
     * Get the meta data for the EJBs managed by this home. <p>
     *
     * Part of the EJBHome interface. <p>
     */
    public EJBMetaData getEJBMetaData() throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getEJBMetaData");

        homeEnabled();

        // We construct the meta data lazily here to avoid initialization-
        // ordering issues if we attempt to do it in initialize().

        if (ejbMetaData == null) {
            // p116618 - start of change
            EJSWrapper wrapper = getWrapper().getRemoteWrapper();

            if (cvUsePortableClass == false) // p125891
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "getEJBMetaData():Creating non-portable EJBMetaDataImpl");

                ejbMetaData = new com.ibm.ejs.container.EJBMetaDataImpl(beanMetaData, getWrapper().getRemoteWrapper(), j2eeName); // p125891

            } else {
                if (isTraceOn && tc.isDebugEnabled()) // p125891
                    Tr.debug(tc, "getEJBMetaData():Creating portable EJBMetaDataImpl"); //p125891

                Object wrapperRef = container.getEJBRuntime().getRemoteReference(wrapper);
                EJBHome homeStub = (EJBHome) PortableRemoteObject.narrow(wrapperRef, EJBHome.class);
                Class<?> pKeyClass = null;
                if (beanMetaData.pKeyClass != null) {
                    pKeyClass = beanMetaData.pKeyClass;
                }

                int beanType = EJBMetaDataImpl.NON_SESSION_BEAN;
                if (beanMetaData.type == InternalConstants.TYPE_STATEFUL_SESSION) {
                    beanType = EJBMetaDataImpl.STATEFUL_SESSION;
                } else if (beanMetaData.type == InternalConstants.TYPE_STATELESS_SESSION) {
                    beanType = EJBMetaDataImpl.STATELESS_SESSION;
                }

                ejbMetaData = new EJBMetaDataImpl(beanType, homeStub, beanMetaData.enterpriseBeanAbstractClass, //150685
                                beanMetaData.homeInterfaceClass, beanMetaData.remoteInterfaceClass, pKeyClass);
            }
            // p116618 - end of change
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getEJBMetaData", ejbMetaData);
        return ejbMetaData;
    } // getEJBMetaData

    public HomeHandle getHomeHandle() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getHomeHandle");

        // p116276 - add code to push/pop java namespace onto ThreadContext.
        ComponentMetaDataAccessorImpl cmdAccessor = null; // p125735
        try {
            cmdAccessor = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor(); // p125735
            cmdAccessor.beginContext(beanMetaData); // p125735
            HomeHandle ehh = null; //p116577 //p125891

            //EntityHomeHandle is really a misnomer; is serves for all types
            //of homes

            // p116577 - start of change.
            // We need to make EntityHomeHandle class use only
            // SUN JDK or Java EE interfaces.  So, we need to pass EJBHome
            // stub to the EntityHomeHandle constructor. Unlike EJBHome
            // interface, the EJBLocalHome does not have a getHomeHandle
            // method.  So, it is safe to assume we need a stub.
            try {
                if (cvUsePortableClass) //p125891
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "getHomeHandle():Creating portable HomeHandleImpl"); //p125891

                    EJSWrapper wrapper = getWrapper().getRemoteWrapper();
                    Object wrapperRef = container.getEJBRuntime().getRemoteReference(wrapper);
                    EJBHome home = (EJBHome) PortableRemoteObject.narrow(wrapperRef, EJBHome.class);
                    ehh = new HomeHandleImpl(home);
                } else {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "getHomeHandle():Creating non-portable EntityHomeHandle"); //p125891
                    Properties pFake = null;
                    ehh = new EntityHomeHandle(ivHomeId, beanMetaData.homeInterfaceClass.getName(), beanMetaData, pFake); // p125891 d145368
                }
            } catch (Exception e) {
                FFDCFilter.processException(e, CLASS_NAME + ".getHomeHandle",
                                            "2362", this);
                EJBException ex = new EJBException("get of EJBHome failed", e);
                Tr.error(tc,
                         "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                         new Object[] { e, ex.toString() }); // d194031
                throw ex;
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".getHomeHandle",
                                            "2372", this);
                ContainerEJBException ex;
                ex = new ContainerEJBException("get of EJBHome failed", t);
                Tr.error(tc,
                         "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                         new Object[] { t, ex.toString() }); // d194031
                throw ex;
            }
            // p116577 - end of change.

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getHomeHandle", ehh);
            return ehh;
        } finally {
            if (cmdAccessor != null)
                cmdAccessor.endContext(); // p125735
        }
    }

    /**
     * Return a <code>LockStrategy</code> instance to use when locking
     * bean instances associated with this home. <p>
     */
    public LockStrategy getLockStrategy() {
        return lockStrategy;
    } // getLockStrategy

    /**
     * Get environment properties associated with this home.
     */
    public Properties getEnvironment() {
        homeEnabled();
        return environment;
    } // getEnvironment

    //------------------------------------
    //
    // Methods from SessionBean interface
    //
    //------------------------------------

    /**
     * Nothing to do on create. <p>
     */
    public void ejbCreate() throws RemoteException {
        //--------------------------------------------
        // This method body intentionally left blank.
        //--------------------------------------------
    } // ejbCreate

    /**
     * Nothing to do on activation. <p>
     */
    @Override
    public void ejbActivate() throws RemoteException {
        //--------------------------------------------
        // This method body intentionally left blank.
        //--------------------------------------------
    } // ejbActivate

    /**
     * EJS homes must never be passivated. <p>
     */
    @Override
    public void ejbPassivate() throws RemoteException {
        throw new UnsupportedOperationException(); // OK
    } // ejbPassivate

    /**
     * Nothing to do when home removed. <p>
     */
    @Override
    public void ejbRemove() throws RemoteException {
        //--------------------------------------------
        // This method body intentionally left blank.
        //--------------------------------------------
    } // ejbRemove

    /**
     * The EJSHome is a true stateless session bean. It doesn't even
     * use its session context. <p>
     */
    @Override
    public void setSessionContext(SessionContext ctx) throws RemoteException {
        //--------------------------------------------
        // This method body intentionally left blank.
        //--------------------------------------------
    } // setSessionContext

    //--------------------------------------------
    //
    // Methods from PoolDiscardStrategy interface
    //
    //--------------------------------------------

    /**
     * This method is called when beans are deallocated from the pool.
     */
    @Override
    public void discard(Object o) {
        BeanO beanO = (BeanO) o;
        beanO.destroy();
    } // discard

    /**
     * This method is only called by entity and stateful home beans generated
     * by ejbdeploy on or after WebSphere Version 5.0. This method is called
     * just prior to calling ejbCreate. This method only seems to be called for
     * EJB 2.0+ deployment descriptors (and for CMP, only for CMP 2.0).
     *
     * @param beanO the bean returned by {@link #createBeanO}
     * @return {@code true} if {@link #afterPostCreateCompletion} should be
     *         called
     */
    // d117006
    public boolean preEjbCreate(BeanO beanO) throws CreateException //144144
    {
        // Beans that have been configured with a reload interval, and are
        // therefore ReadOnly, may not be created. However this is not
        // considered a rollback situation, so throw a checked exception.   LI3408
        if (beanMetaData.ivCacheReloadType != BeanMetaData.CACHE_RELOAD_NONE) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "CreateException: Read only EJB may not be created.");

            throw new CreateException("Read only EJB may not be created.");
        }

        // Let the BeanO know it is about to be used to perform a create(), so
        // ejbStore is not called on the BeanO until after create.    d140886.1
        beanO.preEjbCreate();
        return true;
    }

    /**
     * @deprecated // d142250
     *             Manage Connection Handles during ejbCreate. Called by deployed
     *             code after it returns from bean.ejbPostCreate().
     */
    // d117006
    @Deprecated
    public void postEjbPostCreate(BeanO beanO) throws CreateException //144144
    {
        // Inform the beanO that the create() is complete... the bean
        // is now in the READY state.                                    d140886.1
        beanO.afterPostCreateCompletion();
    }

    //PK26539 start :
    /**
     * This method is used to make a copy of the Primary Key object.
     *
     * For the APAR PK26539, the user is doing a EJBLocalObject.getPrimaryKey().
     * They are then modifying the resultant primary key (PK) and using the PK for further
     * operations. When the user updates the PK, they are essentially editing the "live" PK
     * that we used as part of our caching, thus messing up our internal cache. However, what the
     * user is doing is perfectly fine and acceptable. To account for this, we need to do a "deep"
     * copy to copy the PK object and return the copy of the PK object, not the "live" object. This
     * issue was only hit in the local case as we had already accounted for this in the remote case.
     *
     * PK34120: The above info is true, but with one twist. When EJBLocalObject.getPrimaryKey is called,
     * there is the potential that the ContextClassLoader (CCL) used during the copy is not the same
     * CCL used later when operations are performed on the key. As such, we must make sure that the
     * same CCL is used every time the key is copied. To do this, we must get the CCL from the bean meta
     * data and make sure that CCL is used for the duration of the copy. This copy is a special
     * case. For most methods on EJBLocalObject pre/postInvoke is called which will set the
     * correct CCL. However, the EJBLocalObject.getPrimaryKey method is a special case were pre/postInvoke
     * processing isn't called, thus resulting in the potential for an incorrect CCL being used during the copy.
     *
     * @param obj the object to be copied
     */
    protected final Object WASInternal_copyPrimaryKey(Object obj) {
        // Make a "deep copy" of the primarykey object if the noPriaryKeyMutation
        // property is false.
        if (!noPrimaryKeyMutation && !statefulSessionHome) {
            //PK34120: start
            Object copy = null;

            //Get the CCL from the bmd and use SetContextClassLoaderPrivileged to change the CCL if necessary.
            Object oldCL = EJBThreadData.svThreadContextAccessor.pushContextClassLoaderForUnprivileged(beanMetaData.classLoader); //PK83186
            try {
                copy = container.ivObjectCopier.copy((Serializable) obj); // RTC102299
            } catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".WASInternal_copyPrimaryKey",
                                            "4016", this);
                ContainerEJBException ex = new ContainerEJBException("WASInternal_copyPrimaryKey failed attempting to process PrimaryKey", t);
                Tr.error(tc,
                         "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                         new Object[] { t, ex.toString() });
                throw ex;
            } finally {
                EJBThreadData.svThreadContextAccessor.popContextClassLoaderForUnprivileged(oldCL);
            }

            return copy;
            //PK34120: end
        }
        return obj;
    }

    //PK26539 end

    // d173022.9 Begins
    /**
     * hasMethodLevelAccessIntentSet returns true if there is an access intent policy
     * set on at least one of the method for this bean.
     */
    public final boolean hasMethodLevelAccessIntentSet() {
        return ivEntityHelper.hasMethodLevelAccessIntentSet(this);
    }

    // d173022.9 Ends

    /**
     * Returns the HomeRecord associated with this EJSHome instance. <p>
     **/
    public HomeRecord getHomeRecord() {
        return homeRecord;
    }

    private transient final ReentrantLock createSingletonLock = new ReentrantLock();

    /**
     * Create the Singleton bean instance if it doesn't
     * already exist.
     * Synchronized to ensure only 1 thread is able to create,
     * initialize, and cache the Singleton instance in this home instance.
     *
     * @return BeanO - Singleton BeanO instance
     */
    //F743-1753
    public BeanO createSingletonBeanO() {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createSingletonBeanO: " + j2eeName);

        // F743-4950 - This check is done (unsynchronized) from createBeanO, but
        // it also needs to happen for dependencies.
        homeEnabled();

        if (ivSingletonBeanO != null) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createSingletonBeanO");
            return ivSingletonBeanO;
        }

        BeanO result = null;

        /*
         * This method used to just be synchronized but was converted to use a ReentrantLock instead to avoid deadlocks.
         *
         * The EJB specification states that regardless of our concurrency management type (BEAN or CONTAINER) Singleton
         * initialization sequence should block, but never forever. So we must have a timeout value. We have a default of 2
         * minutes which can be overridden by ContainerProperties.DefaultSessionAccessTimeout ONLY if it is not set to -1
         * (block forever) which then can also be overridden for CONTAINER managed concurrency through @AcessTimeout ONLY
         * if the @AcessTimeout is not -1 or 0 which mean block forever and concurrent access is not permitted respectively.
         */
        // Timeout is in milliseconds
        long timeout = 2 * 60 * 1000;

        // NOTE: We are very specifically checking >= 0 for DefaultSessionAccessTimeout and > 0 for @AccessTimeout as
        // 0 for DefaultSessionAccessTimeout is immediate timeout and 0 for @AccessTimeout means concurrent access is not permitted.
        timeout = ContainerProperties.DefaultSessionAccessTimeout >= 0 ? ContainerProperties.DefaultSessionAccessTimeout : timeout;

        if (!beanMetaData.ivSingletonUsesBeanManagedConcurrency) {
            if (getCurrentThreadDeployedSupport() != null) {
                EJSDeployedSupport ds = (EJSDeployedSupport) getCurrentThreadDeployedSupport();
                timeout = ds.getConcurrencyAccessTimeout() > 0 ? ds.getConcurrencyAccessTimeout() : timeout;
            }
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "createSingletonBeanO: timeout (in ms) = " + timeout);

        boolean finishedWaiting;
        try {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "createSingletonBeanO: attempting to acquire lock");
            finishedWaiting = createSingletonLock.tryLock(timeout, TimeUnit.MILLISECONDS);

            // returned false from hitting timeout
            if (!finishedWaiting) {
                throw new ConcurrentAccessTimeoutException("Timeout occured trying to acquire singleton session bean " + j2eeName + ".");
            }
        } catch (InterruptedException e) {
            throw new ConcurrentAccessTimeoutException("Timeout occured trying to acquire singleton session bean " + j2eeName + ".");
        }
        // Now we are synchronized through the ReentrantLock, very important to have a finally block to release lock.
        try {
            if (ivSingletonBeanO == null) {

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "createSingletonBeanO: synchronized block");

                if (ivCreatingSingletonBeanO) {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "createSingletonBeanO: IllegalLoopbackException");
                    throw new IllegalLoopbackException("Cannot call a method on a singleton session bean while " +
                                                       "constructing the bean instance : " + j2eeName);
                }

                // F7434950.CodRev - The EG has decided that the container must only
                // attempt to initialize a singleton once.
                // d632115 - and the required exception is NoSuchEJBException.
                if (ivSingletonBeanOCreateFailed) {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "createSingletonBeanO: NoSuchEJBException - prior failure");
                    throw ExceptionUtil.NoSuchEJBException("An error occurred during a previous attempt to initialize the " +
                                                           "singleton session bean " + j2eeName + ".", null);
                }

                // F743-4950 - Avoid direct (or indirect) attempts to use this
                // singleton before it has finished initializing.
                ivCreatingSingletonBeanO = true;

                try {

                    // If anything below this point fails, then any subsequent attempts
                    // to create the bean instance must also fail.               d632115
                    ivSingletonBeanOCreateFailed = true; // F7434950.CodRev

                    // F743-20281 - Resolve dependencies.
                    List<J2EEName> dependsOn;
                    try {
                        dependsOn = beanMetaData._moduleMetaData.getEJBApplicationMetaData().resolveBeanDependencies(beanMetaData);
                    } catch (RuntimeWarning rw) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "dependency resolution error", rw);
                        throw ExceptionUtil.NoSuchEJBException(rw.getMessage(), rw);
                    }

                    // F743-4950 - Initialize dependencies before this singleton.
                    if (dependsOn != null) // F743-20281
                    {
                        for (J2EEName dependency : dependsOn) // F743-20281
                        {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "initializing dependency " + dependency);

                            try {
                                EJSHome dependencyHome = (EJSHome) EJSContainer.homeOfHomes.getHome(dependency);
                                dependencyHome.createSingletonBeanO();
                            } catch (Throwable t) {
                                if (isTraceOn && tc.isDebugEnabled())
                                    Tr.exit(tc, "createSingletonBeanO: failed to initialize dependency",
                                            t);
                                // d632115 - required exception is NoSuchEJBException.
                                throw ExceptionUtil.NoSuchEJBException("Failed to initialize singleton session bean " + j2eeName +
                                                                       " because the dependency " + dependency +
                                                                       " failed to initialize.", t);
                            }
                        }
                    }

                    // Now that dependencies have been initialized, add the singleton
                    // to the initialization list.  It doesn't matter if initialization
                    // fails since addInitializedSingleton is idempotent, and we really
                    // only care that the initialization is recorded for this home at
                    // some point after it is recorded for all its dependencies.
                    beanMetaData._moduleMetaData.getEJBApplicationMetaData().addSingletonInitialization(this);

                    long createStartTime = -1;
                    Object oldClassLoader = ThreadContextAccessor.UNCHANGED; // d627931
                    try {
                        // For Singleton, create time should include creating the
                        // instance and calling any lifecycle callbacks.        d626533.1
                        if (pmiBean != null) {
                            createStartTime = pmiBean.initialTime(EJBPMICollaborator.CREATE_RT);
                        }

                        // To support injection, etc. we must put the bmd and classloader
                        // on the thread.   // d627931
                        ivCMDAccessor.beginContext(beanMetaData);
                        oldClassLoader = EJBThreadData.svThreadContextAccessor.pushContextClassLoaderForUnprivileged(beanMetaData.ivContextClassLoader); // F85059

                        ivSingletonBeanO = (SingletonBeanO) beanOFactory.create(container, this, false);
                        ivSingletonBeanOCreateFailed = false; // F7434950.CodRev
                    } catch (Throwable t) {
                        FFDCFilter.processException(t, CLASS_NAME + ".createBeanO", "1047", this);
                        if (t instanceof InvocationTargetException) {
                            t = t.getCause();
                        }
                        // F743-1751CodRev - Always wrap the exception in EJBException to
                        // satisfy the contract of preInvokeForLifecycleInterceptors.
                        // d632115 - and the required exception is NoSuchEJBException.
                        String msgTxt = "An error occurred during initialization of singleton session bean " +
                                        j2eeName + ", resulting in the discarding of the singleton instance.";
                        throw ExceptionUtil.NoSuchEJBException(msgTxt, t);
                    } finally // d627931
                    {
                        EJBThreadData.svThreadContextAccessor.popContextClassLoaderForUnprivileged(oldClassLoader);
                        ivCMDAccessor.endContext();

                        // Even if the create fails, go ahead and add the time, so
                        // the number of times counted matches the create count.
                        if (createStartTime > -1) {
                            pmiBean.finalTime(EJBPMICollaborator.CREATE_RT, createStartTime);
                        }
                    }

                } finally {

                    ivCreatingSingletonBeanO = false;
                }

            }

            // Return the cached Singleton instance.
            result = ivSingletonBeanO;

        } finally {
            // un-synchronized
            createSingletonLock.unlock();
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createSingletonBeanO");
        return result;
    }

}
