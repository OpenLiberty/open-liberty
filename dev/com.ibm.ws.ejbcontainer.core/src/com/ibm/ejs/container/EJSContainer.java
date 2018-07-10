/*******************************************************************************
 * Copyright (c) 1997, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.ejb.CreateException;
import javax.ejb.DuplicateKeyException;
import javax.ejb.EJBAccessException;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBObject;
import javax.ejb.EJBTransactionRolledbackException;
import javax.ejb.EnterpriseBean;
import javax.ejb.NoSuchEJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.RemoveException;
import javax.ejb.Timer;
import javax.rmi.PortableRemoteObject;
import javax.transaction.TransactionRolledbackException;
import javax.transaction.UserTransaction;

import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.portable.UnknownException;

import com.ibm.ejs.container.activator.ActivationStrategy;
import com.ibm.ejs.container.activator.Activator;
import com.ibm.ejs.container.interceptors.InvocationContextImpl;
import com.ibm.ejs.container.lock.LockManager;
import com.ibm.ejs.container.MessageEndpointCollaborator;
import com.ibm.ejs.container.passivator.StatefulPassivator;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ejs.container.util.MethodAttribUtils;
import com.ibm.ejs.csi.UOWControl;
import com.ibm.ejs.util.FastHashtable;
import com.ibm.ejs.util.Util;
import com.ibm.websphere.cpi.PersisterFactory;
import com.ibm.websphere.csi.CSIAccessException;
import com.ibm.websphere.csi.CSIActivitySessionResetException;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.CSITransactionRolledbackException;
import com.ibm.websphere.csi.ContainerExtensionFactory;
import com.ibm.websphere.csi.ExceptionType;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.csi.MethodInterface;
import com.ibm.websphere.csi.ORBDispatchInterceptor;
import com.ibm.websphere.csi.OrbUtils;
import com.ibm.websphere.csi.StatefulSessionHandleFactory;
import com.ibm.websphere.csi.StatefulSessionKeyFactory;
import com.ibm.websphere.csi.TransactionAttribute;
import com.ibm.websphere.ejbcontainer.EJBStoppedException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.ws.csi.DispatchEventListener;
import com.ibm.ws.csi.DispatchEventListenerCookie;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ejbcontainer.EJBPMICollaboratorFactory;
import com.ibm.ws.ejbcontainer.EJBRequestCollaborator;
import com.ibm.ws.ejbcontainer.EJBRequestData;
import com.ibm.ws.ejbcontainer.EJBSecurityCollaborator;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.diagnostics.IncidentStreamWriter;
import com.ibm.ws.ejbcontainer.diagnostics.IntrospectionWriter;
import com.ibm.ws.ejbcontainer.diagnostics.TrDumpWriter;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ejbcontainer.jitdeploy.JITDeploy;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;
import com.ibm.ws.ejbcontainer.util.ObjectCopier;
import com.ibm.ws.ejbcontainer.util.Pool;
import com.ibm.ws.ejbcontainer.util.PoolManager;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.traceinfo.ejbcontainer.TEEJBInvocationInfo;
import com.ibm.ws.traceinfo.ejbcontainer.TETxLifeCycleInfo;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;
import com.ibm.ws.uow.embeddable.UOWManager;
import com.ibm.ws.uow.embeddable.UOWManagerFactory;
import com.ibm.ws.util.StatefulBeanEnqDeq;
import com.ibm.ws.util.WSThreadLocal;
import com.ibm.wsspi.ejbcontainer.WSEJBEndpointManager;

/**
 * Container for both entity and session EJBs. <p>
 */

public class EJSContainer implements ORBDispatchInterceptor, FFDCSelfIntrospectable {
    private static final String CLASS_NAME = EJSContainer.class.getName();
    private static final TraceComponent tc = Tr.register(EJSContainer.class, "EJBContainer", "com.ibm.ejs.container.container");

    private static final TraceComponent tcClntInfo = Tr.register("WAS.clientinfopluslogging",
                                                                 EJSContainer.class,
                                                                 "WAS.clientinfopluslogging",
                                                                 "com.ibm.ejs.container.container");

    /**
     * Special Method id for internal method which requires pre/post invocation
     * semantics.
     */
    public static final int MID_getLink = -1; // d114406

    /**
     * The following method indices is to support internal PM home finders methods
     * for CMR features. The code generator uses these values for the internal
     * method calls in the generated home wrappers
     * Don't change these values unless the ejbdeploy is modified accordingly.
     */
    public static final int MID_LOCAL_HOME_INDEX = -2; // d116009
    public static final int MID_REMOTE_HOME_INDEX = -3; // d116009

    /**
     * The following method indices are for providing a Mandatory Tx Attribute
     * semantics behavior in the preinvoke processing.
     */
    public static final int MID_MANDATORY_LOCAL_HOME_INDEX = -4; // 199625
    public static final int MID_MANDATORY_REMOTE_HOME_INDEX = -5; // 199625

    /**
     * Special method id that provides a NotSupported attribue for an internal
     * remove method for business interfaces. Allows Stateful beans to be
     * removed without actually calling a real business remove method.
     */
    // F743-29185
    public static final int MID_BUSINESS_REMOVE = -6;

    public static final String NOARG_CTOR_NOT_FOUND = "A public no-arg constructor has not been provided for the enterprise bean implementation class.";
    public static final String NOARG_CTOR_FAILURE = "An operation in the enterprise bean constructor failed. It is recommended that component initialization logic be placed in a PostConstruct method instead of the bean class no-arg constructor.";

    protected StatefulBeanEnqDeq ivStatefulBeanEnqDeq; // d646413.2
    // LIDB2775-23.1
    protected DispatchEventListenerManager ivDispatchEventListenerManager; // @MD16426A, d646413.2
    protected final static boolean isZOS = EJSPlatformHelper.isZOS();
    protected final static boolean isZOSCRA = EJSPlatformHelper.isZOSCRA();

    private boolean initialized = false;

    /**
     * Default initialized container.
     */
    protected static EJSContainer defaultContainer; // f111627.1

    /**
     * The home of homes keeps track of all the home interfaces which are
     * installed in this container.
     */
    public static HomeOfHomes homeOfHomes;

    /**
     * The wrapper manager is responsible for interacting with the ORB
     * to construct outgoing IORs for objects in this container and
     * for mapping incoming IORs to objects in this container.
     */
    protected WrapperManager wrapperManager;

    protected EntityHelper ivEntityHelper;

    /**
     * The activator is responsible for activaton, passivation and caching
     * for all EJB instances.
     */
    protected Activator activator;

    /**
     * The lock manager is responsible for all bean locks used by
     * this container.
     */
    protected LockManager lockManager = null;//new LockManager();  //d173022.11

    /**
     * Passivator used by stateful session beans in this container.
     */
    protected StatefulPassivator passivator;

    /**
     * Key factory provides uuids for stateful session beans.
     */
    protected static StatefulSessionKeyFactory sessionKeyFactory;

    //86203
    /**
     * Handle factory converts stateful session beans to Handles.
     */
    protected StatefulSessionHandleFactory sessionHandleFactory;

    /**
     * Runtime neutral was of constructing persisters.
     */
    public PersisterFactory persisterFactory;//90194

    public ObjectCopier ivObjectCopier; // RTC102299

    //89188
    /**
     * Stores internal BeanMetaData instances keyed by the unique
     * bean name. This is the JNDI home name for now, but will soon
     * be changed to the App-Module-Bean name ombo.
     */
    protected Hashtable<J2EEName, BeanMetaData> internalBeanMetaDataStore = new Hashtable<J2EEName, BeanMetaData>();

    /**
     * Class loader for all classes loaded by this container.
     * Making it static makes the assumption that there is only one
     * container per server or that multiple containers share the same
     * class loader. If future trends are towards multiple containers
     * with multiple different class loaders, then we will have to
     * expand the scope/behavior of this feature
     */
    protected static ClassLoader classLoader;

    private static ThreadLocal<EJBThreadData> svThreadData = new WSThreadLocal<EJBThreadData>() {
        @Override
        protected EJBThreadData initialValue() {
            return new EJBThreadData();
        }
    };

    /**
     * Maps activity sessions to their associated container activity session
     */
    protected final static FastHashtable<Object, ContainerAS> containerASMap = new FastHashtable<Object, ContainerAS>(251); // LIDB441.5

    public static final String containerTxResourceKey = CLASS_NAME;

    /**
     * Transaction control provides interface to transaction service
     * for one-stop-shopping for all container transaction needs.
     */
    protected UOWControl uowCtrl;
    protected UserTransaction userTransactionImpl;

    /**
     * UOWManager provides access to the 'user initiated' UOW SPIs.
     * Used to set the JPA task name and register runUnderUOW() callback.
     **/
    // d458031  LI4548-11
    protected UOWManager ivUOWManager;

    /**
     * Security collaborator authorizes method invocations, or null if security
     * has been disabled.
     */
    protected volatile EJBSecurityCollaborator<?> ivSecurityCollaborator;

    /**
     * The Collaborators to be executed after the the bean is activated
     */
    protected EJBRequestCollaborator<?>[] ivAfterActivationCollaborators;

    /**
     * The Collaborators to be executed before the the bean is activated
     */
    protected EJBRequestCollaborator<?>[] ivBeforeActivationCollaborators;

    protected EJBRequestCollaborator<?>[] ivBeforeActivationAfterCompletionCollaborators;//92702

    /**
     * Maximum times to retry a bean activation.
     *
     * FIX ME: Make this a configurable parameter.
     */
    protected int maxRetries = 10;

    /**
     * PmiFactory for the container.
     */
    //86523.3
    protected EJBPMICollaboratorFactory pmiFactory = null;

    /**
     * J2EENameFactory for the container.
     */
    //89554
    protected static J2EENameFactory j2eeNameFactory = null;

    /**
     * ORBUtils object for the container.
     */
    //87918.11
    protected OrbUtils orbUtils = null;

    /**
     * True iff this container instance has dumped its internal state.
     */
    protected boolean dumped = false;

    /**
     * Unique name of this container.
     */
    protected String ivName;

    //87918.7
    /**
     * The PoolManager used to create the bean pools.
     */
    public PoolManager poolManager;

    /**
     * Factory for implementation classes that change across server types.
     */
    protected ContainerExtensionFactory containerExtFactory;

    /**
     * Pool of TimedObjectWrapper objects.
     */
    protected Pool ivTimedObjectPool = null;

    /**
     * When enabled, provides EJB 3.2 and later behavior for allowing
     * access to the Timer API methods outside of a bean.
     */
    protected boolean allowTimerAccessOutsideBean = true;

    /**
     * When true, provides EJB 3.2 and later behavior for allowing stateful
     * lifecycle methods to be transactional.
     */
    protected boolean transactionalStatefulLifecycleMethods = true;

    /**
     * When true, provides EJB 3.2 and later behavior for enabling
     * no-method interfaces for MDBs.
     */
    protected boolean noMethodInterfaceMDBEnabled = true;

    /**
     * Failover cache for SFSB.
     */
    private SfFailoverCache ivSfFailoverCache; //LIDB2018-1

    /**
     * Set to true if SFSB failover is enabled at container level.
     */
    private boolean ivSFSBFailoverEnabled; //LIDB2018-1

    //d568119 - flag to determine whether this container is running in a Java SE app
    public final boolean ivEmbedded;

    private EJBRuntime ivEJBRuntime = null; // F743-12528

    /**
     * Create a new EJS container instance. The container is not ready
     * for use until <code>initialize</code>() has completed
     * successfully. <p>
     */
    public EJSContainer() {
        this(false);
    }

    /**
     * Create a new EJS container instance. The container is not ready
     * for use until <code>initialize</code>() has completed
     * successfully. <p>
     */
    public EJSContainer(boolean embedded) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init> " + String.valueOf(embedded));

        ivEmbedded = embedded;
    } // EJSContainer

    /**
     * Initialize this <code>EJSContainer</code> instance. <p>
     *
     * @param configData must be an instance of
     *            <code>ContainerConfig</code> that contains the
     *            configuration information for this container instance <p>
     *
     */
    public synchronized void initialize(ContainerConfig configData) throws CSIException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "initialize");

        if (initialized) {
            throw new CSIException("already initialized");
        }

        this.containerExtFactory = configData.getContainerExtensionFactory(); // 125942
        this.ivEJBRuntime = configData.getEJBRuntime(); // F743-12528
        classLoader = configData.getClassLoader();
        if (classLoader == null)
            classLoader = this.getClass().getClassLoader(); // 92886
        this.ivName = configData.getContainerName();
        this.persisterFactory = configData.getPersisterFactory();
        EJSContainer.sessionKeyFactory = configData.getSessionKeyFactory();
        this.sessionHandleFactory = configData.getStatefulSessionHandleFactory();//86203
        this.ivSecurityCollaborator = configData.getSecurityCollaborator();

        this.uowCtrl = configData.getUOWControl();
        this.userTransactionImpl = uowCtrl.getUserTransaction();
        this.ivUOWManager = UOWManagerFactory.getUOWManager(); // LI4548-11
        this.ivAfterActivationCollaborators = configData.getAfterActivationCollaborators();
        this.ivBeforeActivationCollaborators = configData.getBeforeActivationCollaborators();
        this.ivBeforeActivationAfterCompletionCollaborators = configData.getBeforeActivationAfterCompletionCollaborators();//92702
        j2eeNameFactory = configData.getJ2EENameFactory();//89554
        this.pmiFactory = configData.getPmiBeanFactory();//86523.3
        this.ivObjectCopier = configData.getObjectCopier(); // RTC102299
        this.orbUtils = configData.getOrbUtils();//87918.11
        this.ivEntityHelper = configData.getEntityHelper();
        this.ivSFSBFailoverEnabled = configData.isEnabledSFSBFailover(); //LIDB2018-1
        this.ivSfFailoverCache = configData.getStatefulFailoverCache(); //LIDB2018-1
        this.passivator = configData.getStatefulPassivator(); // F87439
        this.ivStatefulBeanEnqDeq = configData.getStatefulBeanEnqDeq(); // d646413.2
        this.ivDispatchEventListenerManager = configData.getDispatchEventListenerManager(); // d646413.2

        //-------------------------------------------------
        // FIX ME: make cache size parameters configurable
        //-------------------------------------------------

        poolManager = configData.getPoolManager();//87918.7
        activator = new Activator(this, configData.getEJBCache(), configData.getPassivationPolicy(), passivator, ivSfFailoverCache); //LIDB2018-1
        homeOfHomes = new HomeOfHomes(this, activator);
        int uncached = Activator.UNCACHED_ACTIVATION_STRATEGY;
        ActivationStrategy as = activator.getActivationStrategy(uncached);
        homeOfHomes.setActivationStrategy(as);

        wrapperManager = configData.getWrapperManager();

        // Register an instance of RunUnderUOWCallback with the UOWManager,
        // so the EJB Container is notified when a 'user' (like Spring)
        // initiates a transaction.                                      LI4548-11
        ivUOWManager.registerRunUnderUOWCallback(new RunUnderUOWCallback());

        initialized = true;
        defaultContainer = this; // f111627.1

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "initialize");
    }

    /**
     * Force this container instance to terminate. <p>
     *
     * This method causes the container to mark for rollback all
     * associated transactions (i.e. all those involving bean
     * instances managed by this container), to release all active
     * and pooled EJB instances, and to unregister with the
     * ORB. <p>
     *
     * This method is guaranteed to return and does not guarantee that
     * all termination actions have completed when it does so, only
     * that they have all been initiated, i.e. the caller cannot assume
     * that when this method completes that all transactions this
     * container was associated with will have been rolled back. <p>
     *
     * There may be backgrond threads running on behalf of this container
     * after this method completes. <p>
     *
     */
    public void terminate() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "terminate");

        //-----------------------------
        // Stop passivation activity
        //-----------------------------
        if (passivator != null) // May be null if initialize failed d162687.1
            passivator.terminate();

        //-----------------------------
        // Delete all passivated beans
        //-----------------------------
        if (activator != null) // May be null if initialize failed d162687.1
            activator.terminate();

        //-------------------------------------
        // Unregister from the transport layer
        //-------------------------------------
        if (wrapperManager != null) // May be null if initialize failed d162687.1
            wrapperManager.destroy();

        //d583637 - start
        //-------------------------------------
        // Unset static instance references
        //-------------------------------------
        defaultContainer = null;
        homeOfHomes = null;

        activator = null;
        passivator = null;
        wrapperManager = null;
        poolManager.cancel();
        poolManager = null;
        //d583637 - end

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "terminate");
    } // terminate

    /**
     * Return the EJB runtime.
     *
     * @return the EJB runtime
     */
    public EJBRuntime getEJBRuntime() {
        return ivEJBRuntime;
    }

    public void setSecurityCollaborator(EJBSecurityCollaborator<?> securityCollaborator) {
        ivSecurityCollaborator = securityCollaborator;
    }

    public void setPMICollaboratorFactory(EJBPMICollaboratorFactory factory) {
        pmiFactory = factory;
    }

    /**
     * Factory method for creating EJSDeployedSupport. Callers should
     * instantiate EJSDeployedSupport rather than calling this method.
     *
     * <p>NOTE: This method only exists to support ejbdeploy wrappers.
     *
     * @param wrapper the wrapper requesting the object
     */
    public EJSDeployedSupport getEJSDeployedSupport(EJSWrapperBase wrapper) {
        return new EJSDeployedSupport(); // F61004.3
    }

    /**
     * Container hook for future EJSDeployedSupport caching. This method only
     * needs to be called for entity beans supporting CMR.
     *
     * <p>NOTE: This method only exists to support ejbdeploy wrappers.
     */
    // d113344
    public void putEJSDeployedSupport(EJSDeployedSupport s) {
        //d146239.5
        if (s.methodId < 0) {
            s.ivThreadData.getEJBMethodInfoStack().done(s.methodInfo); // d742761
        }
        //d146239.5
    } // d113344

    /**
     * Returns a WSEJBEndpointManager instance which may be used to establish
     * the proper EJB environment to invoke a WebService Endpoint method,
     * associated with the specified stateless session bean. <p>
     *
     * A new Endpoint Manager instance must be obtained for every
     * WebService Endpoint request. Endpoint Manager instances are
     * NOT thread safe. <p>
     *
     * @param j2eeName is the J2EEName of the stateless session bean.
     * @param provider javax.xml.ws.Provider.class if the bean has been
     *            annotated {@code @WebServiceProvider}. Must be null if
     *            methods is not null.
     * @param methods are the methods that must be implemented by the
     *            bean, and supported on the returned endpoint manager.
     *            Must be specified when provider is null.
     *
     * @return Web Service endpoint manager for the EJB indicated by the
     *         specified J2EEName, supporting the specified methods.
     *
     * @exception EJBConfigurationException is thrown if the EJB does not
     *                implement the required methods or the methods violate
     *                the EJB Specification, or the bean is not a Stateless
     *                session EJB.
     * @exception EJBException if a unexpected Throwable occurred that
     *                prevented this method from generating the bean reference.
     *                Use the getCause method to recover unexpected Throwable
     *                that occurred.
     **/
    // LI3294-35 d496060
    public WSEJBEndpointManager createWebServiceEndpointManager(J2EEName j2eeName,
                                                                Class<?> provider, // d492780, F87951
                                                                Method[] methods) throws EJBException, EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createWebServiceEndpointManager : " + j2eeName,
                     new Object[] { provider, methods });

        EJSHome home = (EJSHome) homeOfHomes.getHome(j2eeName);

        // If a WebService request comes in after a home has been uninstalled,
        // then it won't be found.                                         d547849
        if (home == null) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createWebServiceEndpointManager could not find home for : " +
                            j2eeName);
            throw ExceptionUtil.EJBException("Unable to find EJB component: " + j2eeName, null);
        }

        BeanMetaData bmd = home.beanMetaData;
        Method[] endpointMethods = methods;

        // -----------------------------------------------------------------------
        // If this is the first time the endpoint has been accessed,
        // then resolve the methods to insure this is a valid configuration
        // and JITDeploy the WSEJBProxy class if there are around invoke
        // interceptors.                                                   d497921
        // -----------------------------------------------------------------------
        synchronized (bmd) {
            if (!bmd.ivWebServiceEndpointCreated) {
                if (provider != null) {
                    // The Web Service Endpoint Interface from ejb-jar.xml is for
                    // JAX-RPC, and cannot be used with a JAX-WS Provider.
                    if (bmd.webserviceEndpointInterfaceClass != null) {
                        // Log the error and throw meaningful exception.
                        Tr.error(tc, "WS_ENDPOINT_PROVIDER_CONFLICT_CNTR0176E",
                                 new Object[] { bmd.webserviceEndpointInterfaceClass.getName(),
                                                bmd.j2eeName.getComponent(),
                                                bmd.j2eeName.getModule(),
                                                bmd.j2eeName.getApplication() });
                        throw new EJBConfigurationException("Web Service Provider interface conflicts with the " +
                                                            "configured Web Service Endpoint interface " +
                                                            bmd.webserviceEndpointInterfaceClass.getName() +
                                                            " for the " + bmd.j2eeName.getComponent() +
                                                            " bean in the " + bmd.j2eeName.getModule() +
                                                            " module of the " + bmd.j2eeName.getApplication() +
                                                            " application.");
                    }

                    endpointMethods = provider.getMethods();

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "provider methods : ", (Object[]) endpointMethods);
                }

                // Resolve the Web Service Endpoint methods.  This will make sure that
                // the bean actually implements the methods, but will also remove any
                // excess methodInfos for the webservice endpoint interface in BMD.
                // Until now, EJBContainer is not aware of what the endpoint methods
                // are, so assumes all methods on the bean may be endpoint methods.
                boolean hasAroundInvoke = WSEJBWrapper.resolveWebServiceEndpointMethods(bmd, endpointMethods);

                // Finally, if any of the methods have around invoke interceptors,
                // then a WSEJBProxy must be created by JITDeploy.  An instance of
                // the proxy will be returned to WebServices component to invoke
                // the bean method, instead of an actual bean instance.
                if (hasAroundInvoke) {
                    try {
                        bmd.ivWebServiceEndpointProxyClass = JITDeploy.generateWSEJBProxy(bmd.classLoader,
                                                                                          bmd.ivWebServiceEndpointProxyName,
                                                                                          provider,
                                                                                          endpointMethods,
                                                                                          bmd.wsEndpointMethodInfos,
                                                                                          bmd.enterpriseBeanClassName,
                                                                                          bmd.j2eeName.toString(),
                                                                                          getEJBRuntime().getClassDefiner()); // F70650
                    } catch (ClassNotFoundException ex) {
                        // Log the error and throw meaningful exception.
                        Tr.error(tc, "WS_EJBPROXY_FAILURE_CNTR0177E",
                                 new Object[] { bmd.j2eeName.getComponent(),
                                                bmd.j2eeName.getModule(),
                                                bmd.j2eeName.getApplication(),
                                                ex });
                        throw ExceptionUtil.EJBException("Failure occurred attempting to create a Web service " +
                                                         "endpoint proxy for the " + bmd.j2eeName.getComponent() +
                                                         " bean in the " + bmd.j2eeName.getModule() +
                                                         " module of the " + bmd.j2eeName.getApplication() +
                                                         " application.", ex);
                    }
                }

                bmd.ivWebServiceEndpointCreated = true;

                // Dump the BeanMetaData now that the endpoint methodinfos are set.
                bmd.dump();
            }
        } // end synchronization                                        d521886

        // -----------------------------------------------------------------------
        // Create a new instance of the generic Web Services Endpoint EJB Wrapper,
        // and initialize it specific to the requested bean type.
        // -----------------------------------------------------------------------

        WSEJBWrapper wrapper = new WSEJBWrapper();

        wrapper.container = this;
        wrapper.wrapperManager = wrapperManager;
        wrapper.ivCommon = null; // Not cached
        wrapper.isManagedWrapper = false; // Not managed
        wrapper.ivInterface = WrapperInterface.SERVICE_ENDPOINT;

        // Now, fill in the home specific information.
        wrapper.beanId = home.ivStatelessId;
        wrapper.bmd = bmd;
        wrapper.methodInfos = bmd.wsEndpointMethodInfos;
        wrapper.methodNames = bmd.wsEndpointMethodNames;
        wrapper.isolationAttrs = null; // not used for EJB 2.x
        wrapper.ivPmiBean = home.pmiBean;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createWebServiceEndpointManager : " + wrapper);

        return wrapper;
    }

    /**
     * Gets a home by name.
     *
     * @throws EJBStoppedException if the bean has been stopped
     */
    public EJSHome getStartedHome(J2EEName beanName) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "getStartedHome : " + beanName);

        HomeRecord hr = homeOfHomes.getHomeRecord(beanName); //LIDB859-4
        EJSHome hi = hr.getHomeAndInitialize(); //d199071
        if (hi == null) {
            // This method should only be called when it is known that the bean
            // has been started.  Doing nothing would result in an NPE, so report
            // a meaningful exception instead.                             d547849
            String msgTxt = "The " + beanName.getComponent() +
                            " bean in the " + beanName.getModule() +
                            " module of the " + beanName.getApplication() +
                            " application has been stopped and may no longer be used." +
                            " Wait for the bean to be started, and then try again.";
            throw new EJBStoppedException(msgTxt);
        }

        return hi;
    }

    /**
     * Returns the home for the specified bean. <p>
     *
     * This method should be used instead of getStartedHome when the caller
     * does not know for sure that the bean has ever been started. <p>
     *
     * @throws EJBNotFoundException if the specified bean cannot be found; the
     *             application may have been stopped or has not been installed.
     */
    // F743-34304
    public EJSHome getInstalledHome(J2EEName beanName) throws EJBNotFoundException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "getInstalledHome : " + beanName);

        HomeRecord hr = homeOfHomes.getHomeRecord(beanName);
        if (hr != null) {
            return hr.getHomeAndInitialize();
        }

        // If the home was not found, then the application has either not
        // been installed or started, or possibly failed to start.
        // Log a meaningful warning, and throw a meaningful exception.
        Tr.warning(tc, "HOME_NOT_FOUND_CNTR0092W", beanName.toString());
        throw new EJBNotFoundException("Attempted to access bean " + beanName.getComponent() +
                                       " in module " + beanName.getModule() + " of application " +
                                       beanName.getApplication() +
                                       " that has not been started or does not exist.");
    }

    /**
     * Return the local home named beanName.
     */
    EJSWrapperCommon getHomeWrapperCommon(J2EEName beanName) throws CSIException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getHomeWrapperCommon : " + beanName);

        EJSHome hi = getStartedHome(beanName); // d660700
        EJSWrapperCommon result;
        try {
            result = hi.getWrapper();
        } catch (CSIException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".getHomeWrapperCommon",
                                        "740", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "CSIException occurred", ex);
            throw ex;
        } catch (RemoteException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".getHomeWrapperCommon",
                                        "745", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Remote Exception occurred", ex);
            throw new CSIException("failed to get home instance", ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getHomeWrapperCommon : " + result);
        return result;
    } // getLocalHome

    // f111627.1 End

    /**
     * Stop the bean instance specified by the beanName. ManagedContainer
     * wrapper around uninstallBean
     */
    public void stopBean(BeanMetaData bmd) throws CSIException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "stopBean : " + bmd.j2eeName);

        try {
            uninstallBean(bmd, false);
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".stopBean", "876", this);
            throw new CSIException("Stop on bean " + bmd.j2eeName + " failed", ex);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "stopBean");
    }

    /**
     * Remove the bean with the given bean home name from
     * this container. <p>
     */
    private void uninstallBean(BeanMetaData bmd, boolean terminate) throws ContainerException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        J2EEName beanHomeName = bmd.j2eeName;
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "uninstallBean : " + beanHomeName);

        //89188
        // If the BeanMetaData is not in the internal store, then we only need to
        // remove the HomeRecord.
        if (!internalBeanMetaDataStore.containsKey(bmd.j2eeName)) {
            homeOfHomes.removeHome(bmd); // F743-26072

            if (isTraceOn && tc.isEntryEnabled()) // d402055
                Tr.exit(tc, "uninstallBean"); // d402055
            return;
        }

        // A failure has been reported in the code below that could only occur
        // if the two BMDs are out of synch, so the following FFDC is designed
        // to detect that state and collect possibly useful information.   PM65053
        BeanMetaData ibmd = internalBeanMetaDataStore.get(bmd.j2eeName);
        if (bmd != ibmd) {
            Exception isex = new IllegalStateException("Mismatch in internal bean metadata : " + bmd.j2eeName);
            FFDCFilter.processException(isex, CLASS_NAME + ".uninstallBean", "1500",
                                        this, new Object[] { bmd, ibmd });
        }

        //479980: Simply moved the 'try' to here from down below.  This move is necessary to properly handle the case
        //where a ContainerException is thrown in the next couple lines below.  If this exception is going to be thrown,
        //we should still perform the code contained in the 'finally' block which corresponds to this 'try' block.
        EJSHome home = null;
        try //202018
        {
            // First, remove the home from the HomeOfHomes to prevent any further
            // lookups of this home, and prevent any of the shutdown of the home
            // that is about to be perfomed from being undone. For example, the
            // wrappers should not be put back in the wrapper cache after being
            // removed below.                                               d547849
            home = homeOfHomes.removeHome(bmd); // F743-26072

            // d608829.1 - The home might not exist if a failure occurred before
            // it was created.  Do not throw another (meaningless) exception.
            if (home != null) {
                //PK65102 - Prior to this APAR defect, the call below to 'unregisterHome' was not performed for
                //an MDB home.  This resulted in the home objects remaining in memory after the app containing the home
                //was stopped.
                try {

                    if (home.isMessageDrivenHome()) {
                        ((MDBInternalHome) home).deactivateEndpoint(); //LI2110.41
                    }
                } catch (Throwable e) { // d138839
                    FFDCFilter.processException(e, CLASS_NAME + ".uninstallBean", "988", this);
                }

                try {
                    // If the container is being terminated , cost of passivating a
                    // cache full of stateful beans could be quite expensive.
                    if (!terminate)
                        activator.uninstallBean(beanHomeName);
                } catch (Throwable e) {
                    FFDCFilter.processException(e, CLASS_NAME + ".uninstallBean", "1853", this);
                }

                home.destroy();

                // F743-1751 - Unregister the home from the wrapper manager after uninstalling the bean
                // from the activator and destroying the home since either could cause PreDestroy
                // lifecycle interceptors to be called, which could cause wrappers to be registered.
                try {
                    wrapperManager.unregisterHome(beanHomeName, home);
                } catch (Throwable e) { // d138839
                    FFDCFilter.processException(e, CLASS_NAME + ".uninstallBean", "999", this);
                }
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + ".uninstallBean",
                                        "1430", this);
            ContainerEJBException ex = new ContainerEJBException("Failed to destroy the home.", t);
            Tr.error(tc,
                     "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                     new Object[] { t, ex.toString() });
            throw ex;
        } finally {
            internalBeanMetaDataStore.remove(beanHomeName);//d146239.5

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "uninstallBean");
        } //finally                                   //202018
    } // uninstallBean

    /**
     * Registers the bean with the container, and creates, enables, and returns
     * the home instance.
     *
     * @return the home instance
     */
    public EJSHome startBean(BeanMetaData bmd) throws ContainerException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "startBean: " + bmd.j2eeName);

        J2EEName homeKey = bmd.j2eeName;
        EJSHome ejsHome = null;

        try {
            internalBeanMetaDataStore.put(homeKey, bmd);

            //end 89188
            bmd.dump();

            //----------------------
            // Initialize home bean
            //----------------------

            ejsHome = homeOfHomes.create(bmd);

            //------------------------------------------
            // Complete initialization of the home bean
            //------------------------------------------

            ejsHome.initialize(this, new BeanId(homeOfHomes, homeKey, true), bmd);
            homeOfHomes.setActivationStrategy(ejsHome, bmd);
            ejsHome.completeInitialization(); // PK20648

            if (bmd.persister != null) {
                bmd.persister.setHome(ejsHome);
            }
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".startBean", "1126", this);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Caught exception", ex);
            if (ejsHome != null) {
                try {
                    ejsHome.destroy();
                } catch (Throwable t) {
                    FFDCFilter.processException(t, CLASS_NAME + ".startBean", "1831", this);
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Caught exception destroying home", t);
                }
            }
            String msgTxt = "Failed to start " + homeKey;
            throw new ContainerException(msgTxt, ex);
        }

        // d664917.1 - Set the homeInternal field to indicate that this bean is
        // fully started.
        ejsHome.homeRecord.homeInternal = ejsHome;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "startBean");
        return ejsHome;
    }

    /**
     * Return the home of homes instance owned by this container. <p>
     */
    public HomeOfHomes getHomeOfHomes() {
        return homeOfHomes;
    }

    /**
     * Return the Java EE name factory instance owned by this container. <p>
     */
    //89554
    protected J2EENameFactory getJ2EENameFactory() {
        return j2eeNameFactory;
    } // getHomeOfHomes

    /**
     * Return lock manager used by this container.
     */
    public final LockManager getLockManager() {
        return lockManager;
    } // getTranControl

    public WrapperManager getWrapperManager() {
        return wrapperManager;
    }

    public Activator getActivator() {
        return activator;
    }

    /**
     * Utility method used by UOWControl to determine if a
     * given BeanId corresponds to a stateless session bean. <p>
     */
    public final boolean isStatelessSessionBean(BeanId id) {
        boolean result = id.getHome().isStatelessSessionHome();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isStatelessSessionBean : " + result + ", " + id);
        return result;
    } // isStatelessSessionBean

    /**
     * Utility method used by UOWControl to determine if a
     * given BeanId corresponds to a stateful session bean. <p>
     */
    public final boolean isStatefulSessionBean(BeanId id) {
        boolean result = id.getHome().isStatefulSessionHome();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isStatefulSessionBean : " + result + ", " + id);
        return result;
    } // isStatefulSessionBean

    /**
     * d112604.5
     *
     * Method called after first set of 25 CMP11 entities returned
     * during a lazy enumeration custom finder execution. Called for each set of
     * instances to be hydrated via the RemoteEnumerator code path.
     *
     */
    public void setCustomFinderAccessIntentThreadState(boolean cfwithupdateaccess,
                                                       boolean readonly,
                                                       String methodname) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        // create a thread local context for ContainerManagedBeanO to determine CF Access Information
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "setCustomFinderAccessIntentThreadState");

        //CMP11CustomFinderAIContext = new WSThreadLocal();    deleted PQ95614
        CMP11CustomFinderAccIntentState _state = new CMP11CustomFinderAccIntentState(methodname, cfwithupdateaccess, readonly);

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "current thread CMP11 Finder state" + _state);

        svThreadData.get().ivCMP11CustomFinderAccIntentState = _state; // d630940

        if (isTraceOn && tc.isEntryEnabled()) //PQ95614
            Tr.exit(tc, "setCustomFinderAccessIntentThreadState");
    }

    /**
     * d112604.5
     *
     * ContainerManagedBeanO will call this method if the method context
     * is not valid, which is the key to determinig if a lazy enumeration.
     *
     */
    public CMP11CustomFinderAccIntentState getCustomFinderAccessIntentThreadState() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        // create a thread local context for ContainerManagedBeanO to determine CF Access Information
        CMP11CustomFinderAccIntentState state = null;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getCustomFinderAccessIntentThreadState");

        state = svThreadData.get().ivCMP11CustomFinderAccIntentState; // d630940

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getCustomFinderAccessIntentThreadState");

        return state;
    }

    /**
     * d112604.5
     *
     * Method called after first set of 25 CMP11 entities returned
     * during a lazy enumeration custom finder execution. Called after each set of
     * instances are hydrated via the RemoteEnumerator code path.
     *
     * Must reset each batch is returned as there is no way to guarantee customer will
     * return to get more instances.
     */
    public void resetCustomFinderAccessIntentContext() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        // create a thread local context for ContainerManagedBeanO to determine CF Access Information
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resetCustomFinderAccessIntentContext");

        svThreadData.get().ivCMP11CustomFinderAccIntentState = null; //PQ95614, d630940

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resetCustomFinderAccessIntentContext");
    }

    // d112604.5

    /**
     * Tell the container that the given container transaction has
     * completed. <p>
     */
    public void containerTxCompleted(ContainerTx containerTx) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "containerTxCompleted (" +
                         ContainerTx.uowIdToString(containerTx.ivTxKey) + ")");

        // Release any Option A locks not released as part of normal afterCompletion
        // processing.  This serves as a way to release locks from failed bean
        // creates (DuplicateKeyExceptions) and internal Container problems. d110984
        if (lockManager != null)//added 173022.11
            lockManager.unlock(containerTx);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "containerTxCompleted");
    } // containerTxCompleted

    // LIDB441.5
    /**
     * Tell the container that the given container transaction has
     * completed. <p>
     */
    public void containerASCompleted(Object asKey) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "containerASCompleted : " + asKey);

        containerASMap.remove(asKey);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "containerASCompleted");
    } // containerAsCompleted

    /**
     * Returns container transaction associated with the current thread. <p>
     *
     * Similar to {@link #getActiveContainerTx()}, but this method will
     * return the current transaction even if it has been rolled back or
     * marked for rollback only. <p>
     *
     * Will return null if a container transaction is not associated with
     * the current thread. For example, during after_completion callbacks. <p>
     *
     * @return container transaction associated with the current thread.
     */
    public ContainerTx getCurrentContainerTx() {
        ContainerTx result = null;
        SynchronizationRegistryUOWScope currTxKey = null;
        try {
            currTxKey = uowCtrl.getCurrentTransactionalUOW(false);
        } catch (CSITransactionRolledbackException ex) {
            // (not expected, since passing false on getCurrentTransactionalUOW)
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getCurrentContainerTx: " + ex);
        }

        if (currTxKey != null) {
            result = (ContainerTx) currTxKey.getResource(containerTxResourceKey);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getCurrentContainerTx() : " + result);

        return result;
    }

    /**
     * Returns container transaction associated with the current thread. <p>
     *
     * Similar to {@link #getCurrentContainerTx()}, but this method will
     * throw an exception if the current transaction has been rolled back or
     * marked for rollback only. <p>
     *
     * Will return null if a container transaction is not associated with
     * the current thread. For example, during after_completion callbacks. <p>
     *
     * @return container transaction associated with the current thread.
     *
     * @exception CSITransactionRolledbackException if the current transaction
     *                has been marked for rolled back only, is rolling back, or
     *                has rolled back.
     */
    public ContainerTx getActiveContainerTx() throws CSITransactionRolledbackException {
        ContainerTx result = null;
        SynchronizationRegistryUOWScope currTxKey = uowCtrl.getCurrentTransactionalUOW(true);

        if (currTxKey != null) {
            result = (ContainerTx) currTxKey.getResource(containerTxResourceKey);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getActiveContainerTx() : " + result);

        return result;
    }

    /**
     * Returns container transaction associated with the current thread. <p>
     *
     * Will return null if a container transaction is not associated with
     * the current thread. For example, during after_completion callbacks. <p>
     *
     * @return container transaction associated with the current thread.
     *
     * @exception CSITransactionRolledbackException if the current transaction
     *                has been marked for rolled back only, is rolling back, or
     *                has rolled back.
     * @deprecated use {@link #getActiveContainerTx()} instead.
     */
    @Deprecated
    public ContainerTx getCurrentTx() throws CSITransactionRolledbackException {
        // Forward call to method with boolean flag.                       d166414
        return getCurrentTx(true);
    } // getCurrentTx

    /**
     * Returns container transaction associated with the current thread. <p>
     *
     * Will return null if a container transaction is not associated with
     * the current thread. For example, during after_completion callbacks. <p>
     *
     * @param checkMarkedRollback set to true if an exception should be thrown
     *            if the transaction has been marked for
     *            rollback only.
     *
     * @return container transaction associated with the current thread.
     *
     * @exception CSITransactionRolledbackException if the current transaction
     *                has been marked for rolled back only, is rolling back, or
     *                has rolled back.
     * @deprecated use {@link #getCurrentContainerTx()} instead.
     */
    @Deprecated
    public ContainerTx getCurrentTx(boolean checkMarkedRollback) throws CSITransactionRolledbackException {
        ContainerTx result = null;
        SynchronizationRegistryUOWScope currTxKey = uowCtrl.getCurrentTransactionalUOW(checkMarkedRollback);

        //---------------------------------------------------------
        // No transaction available in after_completion callbacks.
        // Okay to return null in this case.
        //---------------------------------------------------------

        if (currTxKey != null) {
            result = (ContainerTx) currTxKey.getResource(containerTxResourceKey);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getCurrentTx() : " + result);

        return result;
    } // getCurrentTx

    /**
     * Returns container transaction associated with the specified
     * transactional UOW, creating a new one if necessary. <p>
     *
     * Null is returned if the unit of work identifier is null. <p>
     *
     * @param uowId Unit of Work identifier (UOWCoordinator).
     * @param isLocal true if this is a local transaction (not global).
     *
     * @return container transaction associated with the specified UOW;
     *         null if the UOW is null.
     *
     * @exception CSITransactionRolledbackException if the container
     *                fails to enlist with the specified transaction.
     */
    // d139352-2
    public ContainerTx getCurrentTx(SynchronizationRegistryUOWScope uowId, boolean isLocal) throws CSITransactionRolledbackException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getCurrentTx (" + ContainerTx.uowIdToString(uowId) +
                         ", " + (isLocal ? "local" : "global") + ")");

        ContainerTx result = null;
        SynchronizationRegistryUOWScope currTxKey = uowId;

        // If there is no transaction, then this is probably BMT and either
        // an EJB 1.1 module or the BMT transaction has been rolled back.
        // In either case, just allow the method to continue without a
        // ContainerTx.  If there is a transaction, then look for an existing
        // ContainerTx, or create one if not found.

        if (currTxKey != null) {
            result = (ContainerTx) currTxKey.getResource(containerTxResourceKey);
            if (result == null) {
                result = ivEJBRuntime.createContainerTx(this, !isLocal, currTxKey,
                                                        uowCtrl);

                try {
                    uowCtrl.enlistWithTransaction(currTxKey, result);
                } catch (CSIException ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".getCurrentTx",
                                                "1305", this);
                    uowCtrl.setRollbackOnly();
                    throw new CSITransactionRolledbackException("Enlistment with transaction failed", ex);
                }
                currTxKey.putResource(containerTxResourceKey, result);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getCurrentTx : " + result);

        return result;
    }

    // LIDB441.5
    /**
     * Return activity session associated with the current thread. <p>
     *
     * @param checkMarkedReset set to true if an exception should be thrown
     *            if the activitySession has been marked for
     *            reset only.
     *
     * @exception CSIActivitySessionResetException rasied if an activitySession
     *                exists on the current thread, but has been marked for
     *                resetOnly. <p>
     */
    // Added checMarkedRest parameter.                                   d348420
    public ContainerAS getCurrentSessionalUOW(boolean checkMarkedReset) throws CSIException, CSITransactionRolledbackException {
        ContainerAS result = null;
        Object currASKey = uowCtrl.getCurrentSessionalUOW(checkMarkedReset);

        //---------------------------------------------------------
        // No activity session available in after_completion callbacks.
        // Okay to return null in this case.
        //---------------------------------------------------------

        if (currASKey == null) {
            return null;
        }

        result = containerASMap.get(currASKey);
        if (result == null) {
            result = new ContainerAS(this, currASKey);
            uowCtrl.enlistWithSession(result);
            containerASMap.put(currASKey, result);
        }

        return result;
    } // getCurrentSessionalUOW

    /**
     * Returns the EJB thread data if an EJB context is active on the current
     * thread and that EJB may use UserTransaction.
     *
     * @return verified EJB thread data
     * @throws EJBException if the pre-conditions aren't met
     */
    public static EJBThreadData getUserTransactionThreadData() {
        EJBThreadData threadData = getThreadData();
        BeanO beanO = threadData.getCallbackBeanO();

        if (beanO == null) {
            EJBException ex = new EJBException("EJB UserTransaction can only be used from an EJB");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getUserTransactionThreadData: " + ex);
            throw ex;
        }

        try {
            beanO.getUserTransaction();
        } catch (IllegalStateException ex) {
            EJBException ex2 = new EJBException("EJB UserTransaction cannot be used: " + ex, ex);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getUserTransactionThreadData: " + beanO + ": " + ex2);
            throw ex2;
        }

        return threadData;
    }

    /**
     * LIDB1181.23.5.1
     * For BMT beans userTransction calls may transition the bean
     * back and forth between local and global transactions. This
     * method provides a means to create the new ContainerTx and
     * and associate it with the EJSDeployedSupport object for the
     * method invocation
     *
     * @param threadData the thread data returned by {@link #getUserTransactionThreadData}
     * @param isLocal if the user transaction ended
     */
    public void processTxContextChange(EJBThreadData threadData, boolean isLocal) throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processTxContextChange (" + isLocal + ")");

        // Get the current transaction, and use it to locate or create the
        // Container Transaction object (ContainerTx).                   d139352-2
        SynchronizationRegistryUOWScope uowId = uowCtrl.getCurrentTransactionalUOW(true); // d166414

        ContainerTx tx = getCurrentTx(uowId, isLocal);

        EJSDeployedSupport s = threadData.getMethodContext();
        if (s != null && !threadData.isLifeCycleMethodTransactionActive()) {
            // Then update the EJSDeployedSupport object for the current method
            // invokation with the new ContainerTx and UOWCoordinator.       d139352-2
            s.currentTx = tx;//d140003.19
            s.uowCookie.setTransactionalUOW(uowId); // d139352-2

            // If currentTx is changed while running under UOWManager.runUnderUOW, then
            // the value may not be valid during postInvoke; indicate reset needed.
            if (s.ivRunUnderUOW > 0) {
                s.resetCurrentTx = true;
            }

            //d458031 - start: The task name will be set in TranStrategry when a local Tx is begun, so no need to set
            //it here, only set a task name when the user begins a tx.
            if (!isLocal) {
                EJBMethodInfoImpl methodInfo = s.methodInfo;
                //Get the bean class name and method name which will be used as the default JPA task name.
                String taskName = methodInfo.getBeanClassName() + "." + methodInfo.getMethodName();
                //d458031 TO-DO: JPA should provide a constanst class containing the "key" we should use rather than hard coding it.
                ivUOWManager.putResource("com.ibm.websphere.profile", taskName);
                if (isTraceOn && tc.isEventEnabled()) {
                    Tr.event(tc, "Set JPA task name: " + taskName);
                }
            }
            //d458031 - end
        }

        // d165585 Begins
        if (isTraceOn && TETxLifeCycleInfo.isTraceEnabled()) { // PQ74774
            String idStr;
            if (isLocal) {
                if (uowId != null) {
                    idStr = "" + System.identityHashCode(uowId);
                    TETxLifeCycleInfo.traceLocalTxBegin(idStr, "Local Tx Begin");
                }
            } else {
                idStr = uowId.toString();
                int idx;
                idStr = (idStr != null) ? (((idx = idStr.indexOf("(")) != -1) ? idStr.substring(idx + 1,
                                                                                                idStr.indexOf(")")) : ((idx = idStr.indexOf("tid=")) != -1) ? idStr.substring(idx
                                                                                                                                                                              + 4) : idStr) : "NoTx";
                TETxLifeCycleInfo.traceUserTxBegin(idStr, "User Tx Begin");
            }
        } // PQ74774
          // d165585 Ends

        // Starting with EJB 2.0 deployed jars, when the global tran
        // commits, a local tran will be started, so a new ContainerTx
        // will be created and must be setup.  If a ContainerTx does
        // not exist, then this must be an EJB 1.1 jar.                    d125430
        if (tx != null) {
            BeanO beanO = threadData.getCallbackBeanO();
            UserTransactionEnabledContext utxBeanO = (UserTransactionEnabledContext) beanO;

            if (utxBeanO.getModuleVersion() == BeanMetaData.J2EE_EJB_VERSION_1_1) {
                tx.setIsolationLevel(utxBeanO.getIsolationLevel());
            }

            // Enlist the bean with the ContainerTx
            if (utxBeanO.enlist(tx)) {
                activator.enlistBean(tx, beanO);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processTxContextChange");
    }

    /**
     * Transitions the current method call from a local transaction to the
     * sticky global transaction in which the stateful bean being activated
     * is enlisted. <p>
     *
     * It is possible another thread has already resumed the sticky global
     * transaction associated with the bean, in which case the current thread
     * is left with a local tran, and false is returned. <p>
     *
     * @param beanId the bean being transitioned from local to sticky global
     * @param currentTx currently active transaction on the thread
     * @param expectedTx the sticky global tran that the bean is enlisted with.
     *
     * @return true if the expected sticky transaction has been resumed;
     *         otherwise false.
     */
    // d671368 d671368.1
    boolean transitionToStickyGlobalTran(BeanId beanId,
                                         ContainerTx currentTx,
                                         ContainerTx expectedTx) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "transitionToStickyGlobalTran (" + beanId +
                         "," + currentTx + ", " + expectedTx + ")");

        ContainerTx resumedTx = null;
        EJSDeployedSupport s = getMethodContext();
        EJBMethodInfoImpl methodInfo = s.methodInfo;
        BeanMetaData bmd = beanId.getBeanMetaData();

        try {
            // -----------------------------------------------------------------------
            // First, complete the local transaction that was started early in
            // preInvoke with the normal postInvoke processing.
            // -----------------------------------------------------------------------
            currentTx.postInvoke(s);
            uowCtrl.postInvoke(beanId, s.uowCookie, s.exType, methodInfo);

            // Clear to insure postInvoke we occur again if there are failures below.
            s.uowCtrlPreInvoked = false;
            s.currentTx = null;

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "transitionToStickyGlobalTran : local tx completed");

            // -----------------------------------------------------------------------
            // Next, resume the sticky global transaction by executing the same
            // code that would normally happen early in preInvoke.
            // -----------------------------------------------------------------------

            s.uowCookie = uowCtrl.preInvoke(beanId, methodInfo);
            s.uowCtrlPreInvoked = true;

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "transitionToStickyGlobalTran : sticky global resumed");

            // Get the Container Transaction object (ContainerTx) associated
            // with the current transaction, as returned in the UOW Cookie
            // on UOW preInvoke.
            boolean isLocal = s.uowCookie.isLocalTx();
            SynchronizationRegistryUOWScope UOWid = s.uowCookie.getTransactionalUOW();
            resumedTx = getCurrentTx(UOWid, isLocal);

            s.currentTx = resumedTx;
            resumedTx.preInvoke(s);

            // -----------------------------------------------------------------------
            // Now complete the rest of the transaction processing that normally
            // occurs in preInvoke.
            // -----------------------------------------------------------------------

            int isolationLevel = methodInfo.isolationAttr;
            if (bmd.ivModuleVersion <= BeanMetaData.J2EE_EJB_VERSION_1_1 ||
                (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_2_0 &&
                 bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X)) {
                resumedTx.setIsolationLevel(isolationLevel);
            }
            s.currentIsolationLevel = isolationLevel;
        } catch (RemoteException rex) {
            FFDCFilter.processException(rex, CLASS_NAME + ".getBean", "1542", this);

            // Something very unexpected, and ugly happened. Report this as an
            // EJBException, which will cause bean activation to fail. The method
            // context should be in an appropriate state that postInvoke will be
            // able to cleanup properly.
            throw ExceptionUtil.EJBException("A failure occured resuming a bean managed transaction", rex);
        }

        // -----------------------------------------------------------------------
        // Return true only if expected 'sticky' transaction was indeed resumed.
        // It is possible that a different thread resumed the sticky transaction
        // first, so this thread will still just have a local transaction and
        // should continue to wait to lock the bean; false is returned.  d671368.1
        // -----------------------------------------------------------------------

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "transitionToStickyGlobalTran : " + (resumedTx == expectedTx));

        return resumedTx == expectedTx;
    }

    //-----------------------------------------------------------------
    // FIX ME: Nuke this or move it or something. Needs to be a better
    //         way to expose this to connection manager than static
    //         method that is poking around in guts of container.
    //         (Of course, this is nice and simple which is why we
    //         did it this way for now.)
    //
    // This method should no longer be used... it has been replaced by
    // a new version that takes the PortabilityLayer as a parameter.
    // It has been left for now for compatibility purposes for efixes.   d107762
    //-----------------------------------------------------------------
    public static int getIsolationLevel(SynchronizationRegistryUOWScope txKey) {
        ContainerTx ctx = (ContainerTx) txKey.getResource(containerTxResourceKey);
        if (ctx == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "ContainerTx is null, returning default");
            return com.ibm.websphere.csi.Defaults.DEFAULT_ISOLATION_LEVEL;
        } else {
            int isolationLevel = ctx.getIsolationLevel();
            // The new code may return NONE, so convert to old default.   d107762
            if (isolationLevel == java.sql.Connection.TRANSACTION_NONE)
                isolationLevel = com.ibm.websphere.csi.Defaults.DEFAULT_ISOLATION_LEVEL;
            return isolationLevel;
        }
    } // getIsolationLevel

    /**
     * Returns the isolation level for the specified transaction. If an
     * isolation level has not yet been specified for the transaction, or
     * the Container is not involved in the specified transaction, then
     * the isolation level preferred by the connection (as determined from
     * the PortabilityLayer) will be returned.
     *
     * If an isolation level has not been set for the transaction and the
     * Container is involved in the transaction, then the returned value
     * (as determined from the PortabilityLayer) will be set as the
     * isolation level for the transaction.
     *
     * @param txKey transaction key
     * @param preferredIsolationLevel the preferred isolation level as returned
     *            by the portability layer
     *
     * @return the isolation level for the specified transaction.
     **/
    // d107762
    public static int getIsolationLevel(SynchronizationRegistryUOWScope txKey,
                                        int preferredIsolationLevel) {
        int isolationLevel;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getIsolationLevel: " + txKey);

        ContainerTx ctx = (ContainerTx) txKey.getResource(containerTxResourceKey);

        if (ctx == null) {
            // The Container is not involved in the specified transaction.
            // The call is probably for a JSP/Servlet, so just get the preferred
            // value from the PortabilityLayer and return it.
            if (isTraceOn && tc.isEventEnabled())
                Tr.event(tc, "ContainerTx is null, determining from PortabilityLayer");
            isolationLevel = preferredIsolationLevel;
        } else {
            // The Container is involved in the specified tranaction.
            // Get the isolation level from the ContainerTx object. And, if
            // the value is NONE (either not set yet, or was not specified
            // for the bean method), then obtain the preferred value from
            // the PortabilityLayer and set that for the transaction.
            isolationLevel = ctx.getIsolationLevel();

            if (isolationLevel == java.sql.Connection.TRANSACTION_NONE) {
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "Determining from PortabilityLayer");

                isolationLevel = preferredIsolationLevel;
                try {
                    ctx.setIsolationLevel(isolationLevel);
                } catch (IsolationLevelChangeException ex) {
                    // This would be a Container internal error.  It would seem to
                    // only be possible if another thread were modifying the
                    // ContainerTx at the same time.  Log it and continue.
                    FFDCFilter.processException(ex, CLASS_NAME + ".getIsolationLevel",
                                                "1433");
                    Tr.error(tc, "IGNORING_UNEXPECTED_EXCEPTION_CNTR0033E", ex);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getIsolationLevel: " +
                        MethodAttribUtils.getIsolationLevelString(isolationLevel));

        return isolationLevel;
    } // getIsolationLevel

    /**
     * Returns the isolation level for the current transaction. If an
     * isolation level has not yet been specified for the transaction, or
     * the EJB Container is not involved in the specified transaction, then
     * the isolation level preferred by the connection (as specified with
     * the "preferred" parameter) will be returned.
     *
     * If an isolation level has not been set for the transaction and the
     * EJB Container is involved in the transaction, then the returned value
     * (as specified by the "preferred" parameter) will be set as the
     * isolation level for the transaction.
     *
     * @param preferred preferred isolation level for the connection in use
     *
     * @return the isolation level for the current transaction.
     **/
    // d128344.1
    public int getIsolationLevel(int preferred) {
        int isolationLevel;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getIsolationLevel: " + preferred);

        // First, go get the ContainerTx for the current transaction.

        ContainerTx ctx = null;
        try {
            ctx = getCurrentTx(false);//d171654
        } catch (CSITransactionRolledbackException ex) {
            // This would be a Container internal error. Log it and continue.
            FFDCFilter.processException(ex, CLASS_NAME + ".getIsolationLevel",
                                        "1614");
            Tr.error(tc, "IGNORING_UNEXPECTED_EXCEPTION_CNTR0033E", ex);
        }

        if (ctx == null) {
            // The Container is not involved in the specified transaction.
            // The call is probably for a JSP/Servlet, so just return the
            // specified defrault.
            if (isTraceOn && tc.isEventEnabled())
                Tr.event(tc, "ContainerTx is null, returning specified default");
            isolationLevel = preferred;
        } else {
            // The Container is involved in the specified tranaction.
            // Get the isolation level from the ContainerTx object. And, if
            // the value is NONE (either not set yet, or was not specified
            // for the bean method), then set the specified default for the
            // transaction.
            isolationLevel = ctx.getIsolationLevel();

            if (isolationLevel == java.sql.Connection.TRANSACTION_NONE) {
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "Using specified default");

                isolationLevel = preferred;
                try {
                    ctx.setIsolationLevel(isolationLevel);
                } catch (IsolationLevelChangeException ex) {
                    // This would be a Container internal error.  It would seem to
                    // only be possible if another thread were modifying the
                    // ContainerTx at the same time.  Log it and continue.
                    FFDCFilter.processException(ex, CLASS_NAME + ".getIsolationLevel",
                                                "1645");
                    Tr.error(tc, "IGNORING_UNEXPECTED_EXCEPTION_CNTR0033E", ex);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getIsolationLevel: " +
                        MethodAttribUtils.getIsolationLevelString(isolationLevel));

        return isolationLevel;
    } // getIsolationLevel

    /**
     * Add the bean associated with the given <code>BeanO</code> to this
     * container in the context of the given transaction. <p>
     *
     * If a bean with the same identity already exists in this container
     * within the same transaction context then the bean is not added
     * and the <code>BeanO</code> associated with the existing bean is
     * returned. In this case, the caller may retry the add. <p>
     *
     * @param beanO the <code>BeanO</code> to add to this container <p>
     *
     * @param containerTx the <code>ContainerTx</code> to add BeanO in <p>
     *
     * @return the <code>BeanO</code>, if any, that prevented bean from
     *         being added to this container; null if bean successfully
     *         added <p>
     */
    public BeanO addBean(BeanO beanO, ContainerTx containerTx) throws DuplicateKeyException, RemoteException {
        return activator.addBean(containerTx, beanO);
    } // addBean

    public boolean lockBean(BeanO beanO, ContainerTx containerTx) throws RemoteException {
        return activator.lockBean(containerTx, beanO.beanId); // d140003.29
    }

    /**
     * Attempt to retrieve from cache bean with given <code>beanId</code>
     * in current transaction context. <p>
     *
     * This method returns null if a bean with given id is not found. <p>
     *
     * @param id the <code>BeanId</code> to retrieve bean for <p>
     */
    public EJBObject getBean(ContainerTx tx, BeanId id) throws RemoteException {
        EJBObject result = null;

        BeanO beanO = activator.getBean(tx, id);
        if (beanO != null) {
            try { // d116480
                result = wrapperManager.getWrapper(id).getRemoteWrapper(); // f111627 d156807.1
            } catch (IllegalStateException ise) { // d116480
                // Result is still null if no defined interface
                FFDCFilter.processException(ise, CLASS_NAME + ".getBean", "1542", this);
            } // d116480
        }
        return result;
    } // getBean

    /**
     * Remove the bean instance defined by the given wrapper. <p>
     *
     * Provides the implementation of the remove method defined on EJBObject and
     * EJBLocalOBject. <p>
     *
     * Also provides support for removing stateful beans that is required
     * to support the JCDI session scoped context. When the JCDI context ends,
     * all stateful beans associated with it must be removed. For component
     * interfaces, the normal remove method is called, however for business
     * interfaces, an internal remove method is used. <p>
     *
     * @param w the <code>EJSWrapper</code> to remove <p>
     */
    public void removeBean(EJSWrapperBase w) throws RemoteException, RemoveException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        // Beans that have been configured with a reload interval, and are
        // therefore ReadOnly, may not be removed. However this is not
        // considered a rollback situation, so throw a checked exception.   LI3408
        if (w.bmd.ivCacheReloadType != BeanMetaData.CACHE_RELOAD_NONE) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "RemoveException: Read only EJB may not be removed.");

            throw new RemoveException("Read only EJB may not be removed.");
        }

        // If this is a business interface, then it may not have a remove method
        // and if it does, it may take parameters that aren't available. So, for
        // business interfaces, an internal method is used that doesn't actually
        // call a method on the bean, but acts like a remove method. If this is
        // a component interface, then perform the normal remove processing that
        // is required for EJBObject/EJBLocalObject remove.             F743-29185
        //
        // We cannot easily share a "fake" remove EJBMethodInfo for both component
        // business due to the differences in PMI and enlisted remove.     d742761
        int methodId = MID_BUSINESS_REMOVE;
        if (w.ivInterface == WrapperInterface.LOCAL ||
            w.ivInterface == WrapperInterface.REMOTE) {
            methodId = w.methodInfos.length - 1;
        }

        //------------------------------------------------------------------------
        // Remove is modelled after bean interface method, with
        // similar pre/postInvoke behavior, but since remove requires
        // special interaction with container it is not identical.
        //------------------------------------------------------------------------

        EJSDeployedSupport s = new EJSDeployedSupport(); // d630940
        boolean isStateless = isStatelessSessionBean(w.beanId); // d115455

        try {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.entry(tc, "removeBean(wrapperBase) : " + w.beanId);
            EjbPreInvoke(w, methodId, s, new Object[0]); // LIDB2167.11 d369262.6

            // For component interfaces, EJSWrapper/EJSLocalWrapper.remove() calls
            // this method, so we call BeanO.remove() to remove the bean.
            //
            // For business interface @Remove methods, the generated wrapper needs
            // to call the bean rather than this method, so BeanO.remove() is
            // called indirectly via BeanO.commit().  This method can still be
            // called for CDI remove, so we still rely on BeanO.commit() rather
            // than explicitly calling BeanO.remove() here for proper PMI handling.
            if (methodId == MID_BUSINESS_REMOVE) {
                // Set the bean in the current tx for removal after the transaction
                // completes.  This would normally be set during postInvoke, but the
                // temporary EJBMethodInfoImpl isn't marked setSFSBRemove (perhaps
                // because extra cleanup would be required when putting it back in
                // the temporary pool).
                s.currentTx.ivRemoveBeanO = s.beanO; // F743-29185
            } else {
                s.beanO.remove();//d140003.19
            }

            //---------------------------------------------------------------------
            // Don't unregister stateless session bean instances, since there
            // is a single wrapper instance for many bean instances.
            //
            // Don't unregister if this method invokation did not pin the Wrapper
            // in the Wrapper Cache.  This may occur if another thread is also
            // calling a method on the wrapper, or if this is a reentrant call
            // to remove.                                                   d147604
            //---------------------------------------------------------------------

            if (!isStateless &&
                s.unpinOnPostInvoke == true) {
                boolean removed = wrapperManager.unregister(w.beanId, true); //d181569
                if (removed)
                    s.unpinOnPostInvoke = false; // d140003.19
            }
            // d115455 begin
        } catch (UnknownException ex) {
            // When current invocation is executed within another Tx scope, an
            // BeanNotReentrantException will be thrown. The remote exception mapping
            // mechanism will turns the BeanNotReentrantException to an UnknownException.
            FFDCFilter.processException(ex, CLASS_NAME + ".removeBean", "1589", this);
            if ((isStateless || isStatefulSessionBean(w.beanId)) &&
                ex.originalEx instanceof BeanNotReentrantException) {

                // section 7.6.4 - It is an error for a client to invoke the remove
                //  method on the sessionobject's home or component interface object
                //  if a session bean instance is participating in a transaction.
                RemoveException rex = new RemoveException(ex.originalEx.toString());
                s.setCheckedException(rex);//d140003.19
                throw rex;
            } else {
                // treats the reentrant exceptoin like Throwable exception
                s.setUncheckedException(ex);
            }
            // d115455 end
            // d171551 Begins
        } catch (EJBException ejbex) {
            // When current invocation is executed within another Tx scope, an
            // BeanNotReentrantException will be thrown. The local exception mapping
            // mechanism will turns the BeanNotReentrantException to an EJBException.
            FFDCFilter.processException(ejbex, CLASS_NAME + ".removeBean", "2077", this);
            if ((isStateless || isStatefulSessionBean(w.beanId)) &&
                ejbex.getCausedByException() instanceof BeanNotReentrantException) {

                // section 7.6.4 - It is an error for a client to invoke the remove
                //  method on the sessionobject's home or component interface object
                //  if a session bean instance is participating in a transaction.
                RemoveException rex = new RemoveException(ejbex.getCausedByException().toString());
                s.setCheckedException(rex);
                throw rex;
            } else {
                // treats the reentrant exception like Throwable exception
                s.setUncheckedException(ejbex);
            }
            // d171551 Ends

        } catch (RemoveException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".removeBean", "1605", this);
            s.setCheckedException(ex);//d140003.19
            throw ex;
        } catch (RemoteException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".removeBean", "1609", this);
            s.setUncheckedException(ex); // c154712
            throw ex;
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".removeBean", "1612", this);
            s.setUncheckedException(ex);
        } finally {
            try {
                postInvoke(w, methodId, s);
            } finally {
                putEJSDeployedSupport(s); // d742761
            }
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "removeBean(wrapperBase)");
        }
    } // removeBean

    /**
     * Remove bean from container in current transaction context.
     * Used to clean up from create failures. It is not an error if
     * the bean does not exist in this container.
     */
    public void removeBean(BeanO beanO) throws CSITransactionRolledbackException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "removeBean with BeanO");

        //------------------------------------------------------------------------
        // If bean id is null then beanO could not have been added to this
        // container. Also, if beanO is not enlisted in the tran, then it either
        // was never added to this container, or it was added, but already
        // removed.  This may occur for example in the case of a
        // DuplicateKeyException.                                 dPQ51806 d145697
        //------------------------------------------------------------------------

        BeanId id = beanO.getId();

        if (id != null) {
            try {
                // If this particular BeanO was enlisted, then it needs to be
                // removed from the tran and the ejb cache.                  d145697
                ContainerTx currentTx = getCurrentTx(false); //d171654 // d139352-2
                if (currentTx.delist(beanO)) {
                    activator.removeBean(currentTx, beanO);
                } else {
                    // This is normal, it just means the failure occurred early in
                    // the create proccess, before the bean was ever enlisted or
                    // added to the EJB Cache.                                d145697
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "BeanO not enlisted - not removed from tx or cache");
                }
            } catch (Throwable th) {
                // The transaction may have already rolled back.             d145697
                FFDCFilter.processException(th, CLASS_NAME + ".removeBean(BeanO)",
                                            "1949", this);
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "Exception thrown in removeBean()",
                             new Object[] { beanO, th });
            }
        } else {
            // This is normal, it just means the failure occurred early in
            // the create proccess, before the bean was ever enlisted or
            // added to the EJB Cache.                                      d145697
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "BeanId is null");
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "removeBean");
    } // removeBean

    /**
     * Removes the specified stateful session bean. <p>
     *
     * True is returned if the bean is successfully removed, and false is returned
     * if the bean has already been removed. An exception will occur if the bean
     * exists but cannot be removed. <p>
     *
     * @param bean reference to a stateful session bean
     *
     * @throws RemoteException when the bean cannot be removed (for example, in use).
     * @throws RemoveException if the specified object is not a stateful bean
     *             reference or the bean cannot be removed.
     *
     * @return true if removed, false if previously removed
     */
    // F743-29185
    public boolean removeStatefulBean(Object bean) throws RemoteException, RemoveException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "removeStatefulBean : " + Util.identity(bean));

        // -----------------------------------------------------------------------
        // Determine whether the parameter is a normal wrapper (business or
        // component interface) or a No-Interface reference, and if No-Interface
        // then extract the normal wrapper from it.
        // -----------------------------------------------------------------------

        EJSWrapperBase wrapper = null;
        if (bean instanceof EJSWrapperBase) {
            wrapper = (EJSWrapperBase) bean;
        } else if (bean instanceof LocalBeanWrapper) {
            wrapper = EJSWrapperCommon.getLocalBeanWrapperBase((LocalBeanWrapper) bean);
        } else {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "removeStatefulBean : RemoveException:" + Util.identity(bean));
            throw new RemoveException("Object to remove is not an enterprise bean reference : " + Util.identity(bean));
        }

        // -----------------------------------------------------------------------
        // Confirm that the bean is a stateful bean, per method contract.
        // -----------------------------------------------------------------------

        if (!isStatefulSessionBean(wrapper.beanId)) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "removeStatefulBean : RemoveException:not stateful : " + wrapper.beanId);
            throw new RemoveException("Object to remove is not a stateful bean reference : " + wrapper.beanId);
        }

        // -----------------------------------------------------------------------
        // Actually remove the bean, reporting true for success, false for does
        // not exist, and propagating other exceptions.
        //
        // Note that this must go through a 'normal' pre/post invoke cycle, to
        // activate the bean and insure it is in a proper state for removal.
        // This will perform normal lifecycle removal and update pmi statistics.
        // -----------------------------------------------------------------------

        try {
            removeBean(wrapper);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "removeStatefulBean : true");
            return true;
        } catch (NoSuchEJBException ex) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "removeStatefulBean : false (NoSuchEJBException)");
            return false;
        } catch (NoSuchObjectLocalException ex) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "removeStatefulBean : false (NoSuchObjectLocalException)");
            return false;
        } catch (OBJECT_NOT_EXIST ex) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "removeStatefulBean : false (OBJECT_NOT_EXIST)");
            return false;
        } catch (EJBException ex) {
            // The RemoveException is considered 'checked' and should not be
            // wrapped in an EJBException.                                  d661827
            if (ex.getCause() instanceof RemoveException) {
                RemoveException rex = (RemoveException) ex.getCause();
                throw rex;
            }
            throw ex;
        }
    }

    /**
     * Resolves the wrapper for a local wrapper proxy.
     *
     * @param proxy the proxy state
     * @return the wrapper
     * @throws EJBException if the proxy cannot be reconnected
     */
    public static Object resolveWrapperProxy(BusinessLocalWrapperProxy proxy) {
        WrapperProxyState state = proxy.ivState;

        Object wrapper = state.ivWrapper;
        if (wrapper == null) {
            do {
                state = state.reconnect();
                wrapper = state.ivWrapper;
            } while (wrapper == null);

            proxy.ivState = state;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "resolveWrapperProxy: " + state);
        }

        return wrapper;
    }

    /**
     * Resolves the wrapper for a local object wrapper proxy.
     *
     * @param proxy the proxy state
     * @return the wrapper
     * @throws EJBException if the proxy cannot be reconnected
     */
    public static Object resolveWrapperProxy(EJSLocalWrapperProxy proxy) {
        WrapperProxyState state = proxy.ivState;

        Object wrapper = state.ivWrapper;
        if (wrapper == null) {
            do {
                state = state.reconnect();
                wrapper = state.ivWrapper;
            } while (wrapper == null);

            proxy.ivState = state;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "resolveWrapperProxy: " + state);
        }

        return wrapper;
    }

    // d114406 Begin
    /**
     * Map the input method id to its corresponding MethodInfoImpl object.
     * If the method id is negative, it maps to some pre-defined internal methods
     * that does not exposed through the local or remote interface. These
     * internal method id is defined as EJSContainer.MID_<method name>.
     * A new EJBMethodInfoImpl is constructed due to the dependency of bean
     * meta data in this object for each method call. This method info is stored
     * in the EJSDeployedSupport for the duration of the method call.
     */
    //d140003.20 return signature change
    private EJBMethodInfoImpl mapMethodInfo(EJSDeployedSupport s,
                                            EJSWrapperBase wrapper, int methodId,
                                            String methodSignature) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "mapMethodInfo(" + methodId + "," + methodSignature + ")");

        EJBMethodInfoImpl rtnInfo = null; // d122418-1 //d140003.20

        s.methodId = methodId; //130230 d140003.19
        s.ivWrapper = wrapper; // d366807.1

        if (methodId < 0) {
            if (methodId == MID_BUSINESS_REMOVE) {
                // Internal remove method for business interfaces.        F743-29185
                rtnInfo = s.ivThreadData.getEJBMethodInfoStack().get(
                                                                     "$remove:", null, wrapper,
                                                                     wrapper.ivInterface == WrapperInterface.BUSINESS_LOCAL ? MethodInterface.LOCAL : MethodInterface.REMOTE,
                                                                     wrapper.bmd.usesBeanManagedTx ? TransactionAttribute.TX_BEAN_MANAGED : TransactionAttribute.TX_NOT_SUPPORTED);
                rtnInfo.setClassLoader = true; // d661827
            } else {
                rtnInfo = ivEntityHelper.getPMMethodInfo(s, wrapper, methodId, methodSignature);
            }
        } else // d122418-1
        { // d122418-1
          // d206696 - Account for case where
          // methodInfos array is null (which occurs if WS invocation
          // API is used on an SLSB with no service endpoint interface defined)
            if (wrapper.methodInfos == null)
                throw new IllegalStateException("An attempt was made to invoke a method on a bean interface that is not defined.");
            rtnInfo = wrapper.methodInfos[methodId]; // d122418-1
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "mapMethodInfo");

        return rtnInfo; // d122418-1
    }

    // d114406 End

    /**
     * Perform container actions prior to invocation of bean method. <p>
     *
     * This method is called before every bean method. It is responsible
     * for informing all container collaborators that a method invocation
     * is beginning. <p>
     *
     * This method must return a <code>BeanO</code> instance that the
     * method may be invoked on. <p>
     *
     * @param wrapper the <code>EJSWrapper</code> instance that method
     *            is being invoked on <p>
     *
     * @param methodId an int indicating which method on the bean is being
     *            invoked <p>
     *
     * @param s an instance of EJSDeployedSupport that records whether
     *            a new transaction was started <p>
     *
     * @exception RemoteException thrown if any error occur trying to set
     *                container state; this exception will be "handled" by
     *                postInvoke (see below) thanks to try-catch clauses in
     *                deployed code (where preInvoke is called) <p>
     */
    public EnterpriseBean preInvoke(EJSWrapper wrapper, int methodId,
                                    EJSDeployedSupport s) throws RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "old preinvoke called by EJBDeploy, not new JACC preinvoke");

        Object[] args = null; //LIDB2617.11
        return preInvoke(wrapper, methodId, s, args); // f111627
    }

    /**
     * Introduced this preInvoke( EJSWrapperBase...) for Local Interface support.
     * The old preInvoke( EJSWrapper...) must still be supported for backward
     * compatible with old generated code base.
     */
    public EnterpriseBean preInvoke(EJSWrapperBase wrapper,
                                    int methodId,
                                    EJSDeployedSupport s) throws RemoteException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "old preinvoke called by EJBDeploy, not new JACC preinvoke");

        Object[] args = null; //LIDB2617.11
        return preInvoke(wrapper, methodId, s, args); //LIDB2617.11
    }

    /**
     * Returns boolean true iff wrapper code must pass EJB method
     * arguments to preinvoke method of this class (see preinvoke immediately
     * after this method). If boolean false is returned, wrapper
     * should pass a null reference for the EJB arguments and avoid
     * creation of the Object array for the EJB arguments so that
     * performance is not impacted when EJB arguments are not required.
     *
     */
    //LIDB2617.11 added this method.
    //d209070 rewrote method.
    final public boolean doesJaccNeedsEJBArguments(EJSWrapperBase wrapper) {
        // Default to false in case Security Collaborator is not defined
        EJBSecurityCollaborator<?> securityCollaborator = ivSecurityCollaborator;
        boolean result = securityCollaborator != null && securityCollaborator.areRequestMethodArgumentsRequired();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "doesJaccNeedsEJBArguments returning " + result);

        return result;
    }

    /**
     * Introduced this preInvoke method to allow generated code base to pass
     * the EJB arguments required for JACC 1.0 support. The old
     * preInvoke( EJSWrapper...) must still be supported for backward compatible
     * with old generated code base.
     */
    //LIDB2617.11 added this method.
    public EnterpriseBean preInvoke(EJSWrapperBase wrapper,
                                    int methodId,
                                    EJSDeployedSupport s,
                                    Object[] args) throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        // Get the EJBMethodInfoImpl object for the methodId
        // assigned to this method by ejbdeploy tool.
        EJBMethodInfoImpl methodInfo = mapMethodInfo(s, wrapper, methodId, null);

        try {
            if (isTraceOn) {
                if (TEEJBInvocationInfo.isTraceEnabled())
                    TEEJBInvocationInfo.tracePreInvokeBegins(s, wrapper);

                if (tc.isEntryEnabled())
                    Tr.entry(tc, "EJBpreInvoke(" + methodId + ":" +
                                 methodInfo.getMethodName() + ")");
                // d191679 Begins
                if (tcClntInfo.isDebugEnabled())
                    Tr.debug(tcClntInfo, "preInvoke(" + methodInfo.getMethodName() + ")");
                // d191679 Ends
            }

            // If this is a stateless SessionBean create, run only the bare minimum of
            // collaborators and other processing. This path is encapsulated in a separate
            // method for now, to avoid lots of "if" statements within this method.
            Object bean = null; // Changed EnterpriseBean to Object       d366807.1
            if (methodInfo.isStatelessSessionBean && methodInfo.isHomeCreate) {
                bean = preInvokeForStatelessSessionCreate(wrapper, methodId, s, methodInfo, args); //LIDB2617.11
            } else {
                // Perform preinvoke processing that must occur prior
                // to activation of the EJB.
                bean = preInvokeActivate(wrapper, methodId, s, methodInfo);

                // Now perform preinvoke processing that must occur after
                // the EJB is activated.
                preInvokeAfterActivate(wrapper, bean, s, args);
            }

            // Normal path exit.
            if (isTraceOn) {
                if (tc.isEntryEnabled()) {
                    ContainerTx containerTx = s.currentTx;

                    Tr.exit(tc, "EJBpreInvoke(" + methodId + ":" +
                                methodInfo.getMethodName() + ") " +
                                " Invoking method '" + methodInfo.getMethodName() +
                                "' on bean '" + wrapper.getClass().getName() +
                                "(" + wrapper.beanId + ")" +
                                "', '" + containerTx + "', isGlobalTx=" +
                                (containerTx != null ? (containerTx.isTransactionGlobal() ? "true" : "false") : "Unknown"));
                }

                if (TEEJBInvocationInfo.isTraceEnabled())
                    TEEJBInvocationInfo.tracePreInvokeEnds(s, wrapper);
            }

            // No exceptions occured, so return the Enterprise bean
            // that was activated and/or loaded.
            return (EnterpriseBean) bean;
        } catch (Throwable t) {
            //FFDCFilter.processException( t, CLASS_NAME + ".preInvoke", "2558");
            preinvokeHandleException(t, wrapper, methodId, s, methodInfo);

            // This will never happen since the setUncheckException should
            // always throw an exception.  But we need this line to make
            // the compiler happy (it does know exception always thrown).
            return null;
        }
    }

    /**
     * Introduced this preInvoke method to support the EJB 3.0 Business
     * interfaces... where the EJB may not implement EnterpriseBean. <p>
     *
     * All previous preInvoke( EJSWrapper...) methods must still be supported
     * for backward compatible with old generated code base.
     **/
    // d366807.1
    public Object EjbPreInvoke(EJSWrapperBase wrapper,
                               int methodId,
                               EJSDeployedSupport s,
                               Object[] args) throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        // Get the EJBMethodInfoImpl object for the methodId
        // assigned to this method by JITDeploy/EJBbdeploy tool.
        EJBMethodInfoImpl methodInfo = mapMethodInfo(s, wrapper, methodId, null);

        try {
            if (isTraceOn) {
                if (TEEJBInvocationInfo.isTraceEnabled())
                    TEEJBInvocationInfo.tracePreInvokeBegins(s, wrapper);

                if (tc.isEntryEnabled())
                    Tr.entry(tc, "EJBpreInvoke(" + methodId + ":" +
                                 methodInfo.getMethodName() + ")");

                if (tcClntInfo.isDebugEnabled())
                    Tr.debug(tcClntInfo, "preInvoke(" + methodInfo.getMethodName() + ")");
            }

            // Perform preinvoke processing that must occur prior
            // to activation of the EJB.
            Object bean = preInvokeActivate(wrapper, methodId, s, methodInfo);

            // Now perform preinvoke processing that must occur after
            // the EJB is activated.
            preInvokeAfterActivate(wrapper, bean, s, args);

            // Normal path exit.
            if (isTraceOn) {
                if (tc.isEntryEnabled()) {
                    ContainerTx containerTx = s.currentTx;

                    // Change trace message when method is asynchronous
                    if (methodInfo.isAsynchMethod()) {

                        Tr.exit(tc, "EJBpreInvoke(" + methodId + ":" +
                                    methodInfo.getMethodName() + ") " +
                                    " Invoking asynch method '" + methodInfo.getMethodName() +
                                    "' on bean '" + wrapper.getClass().getName() +
                                    "(" + wrapper.beanId + ")" +
                                    "', '" + containerTx + "', isGlobalTx=" +
                                    (containerTx != null ? (containerTx.isTransactionGlobal() ? "true" : "false") : "Unknown"));

                    } else {

                        Tr.exit(tc, "EJBpreInvoke(" + methodId + ":" +
                                    methodInfo.getMethodName() + ") " +
                                    " Invoking method '" + methodInfo.getMethodName() +
                                    "' on bean '" + wrapper.getClass().getName() +
                                    "(" + wrapper.beanId + ")" +
                                    "', '" + containerTx + "', isGlobalTx=" +
                                    (containerTx != null ? (containerTx.isTransactionGlobal() ? "true" : "false") : "Unknown"));
                    }
                }

                if (TEEJBInvocationInfo.isTraceEnabled())
                    TEEJBInvocationInfo.tracePreInvokeEnds(s, wrapper);
            }

            // No exceptions occured, so return the Enterprise bean
            // that was activated and/or loaded.
            return bean;
        } catch (Throwable t) {
            //FFDCFilter.processException( t, CLASS_NAME + ".preInvoke", "2558");
            preinvokeHandleException(t, wrapper, methodId, s, methodInfo);

            // This will never happen since the setUncheckException should
            // always throw an exception.  But we need this line to make
            // the compiler happy (it does not know exception always thrown).
            return null;
        }
    }

    /**
     * Introduced this preInvoke to be used by other methods of this class only
     * when the methodId is < 0 (internal PM home method calls). The alternative
     * to introducing this method would have been to keep the old preinvoke_internal
     * method and add a 5th parameter to it. This 5th parameter adds extra
     * performance overhead that would occur on every preinvoke call when it fact
     * most preinvoke calls only need 4 parameters. To minimize performance impacts
     * due to JACC 1.0 changes, it was decided to replace preinvoke_internal with
     * this method for the PM internal home calls and the other new preinvoke
     * method for the non-PM internal home calls (e.g business methods where methodId >= 0).
     */
    //LIDB2617.11 added this method.
    private EnterpriseBean preInvokePmInternal(EJSWrapperBase wrapper,
                                               int methodId,
                                               EJSDeployedSupport s,
                                               EJBMethodInfoImpl methodInfo) throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        try {
            if (isTraceOn) {
                if (TEEJBInvocationInfo.isTraceEnabled())
                    TEEJBInvocationInfo.tracePreInvokeBegins(s, wrapper);

                if (tc.isEntryEnabled())
                    Tr.entry(tc, "EJBpreInvoke(" + methodId + ":" +
                                 methodInfo.getMethodName() + ")");
            }

            // If this is a stateless SessionBean create, run only the bare minimum of
            // collaborators and other processing. This path is encapsulated in a separate
            // method for now, to avoid lots of "if" statements within this method.
            Object bean = null; // Changed EnterpriseBean to Object       d366807.1
            if (methodInfo.isStatelessSessionBean && methodInfo.isHomeCreate) {
                bean = preInvokeForStatelessSessionCreate(wrapper, methodId, s, methodInfo, null); //LIDB2617.11
            } else {
                // Perform preinvoke processing that must occur prior
                // to activation of the EJB.
                bean = preInvokeActivate(wrapper, methodId, s, methodInfo);

                // Now perform preinvoke processing that must occur after
                // the EJB is activated.
                preInvokeAfterActivate(wrapper, bean, s, null);
            }

            // Normal path exit.
            if (isTraceOn) {
                if (tc.isEntryEnabled()) {
                    ContainerTx containerTx = s.currentTx;
                    Tr.exit(tc, "EJBpreInvoke(" + methodId + ":" +
                                methodInfo.getMethodName() + ") " +
                                " Invoking method '" + methodInfo.getMethodName() +
                                "' on bean '" + wrapper.getClass().getName() +
                                "(" + wrapper.beanId + ")" +
                                "', '" + containerTx + "', isGlobalTx=" +
                                (containerTx != null ? (containerTx.isTransactionGlobal() ? "true" : "false") : "Unknown"));
                }

                if (TEEJBInvocationInfo.isTraceEnabled())
                    TEEJBInvocationInfo.tracePreInvokeEnds(s, wrapper);
            }

            // No exceptions occured, so return the Enterprise bean
            // that was activated and/or loaded.
            return (EnterpriseBean) bean;
        } catch (Throwable t) {
            //FFDCFilter.processException( t, CLASS_NAME + ".preInvoke", "2558");
            preinvokeHandleException(t, wrapper, methodId, s, methodInfo);

            // This will never happen since the setUncheckException should
            // always throw an exception.  But we need this line to make
            // the compiler happy (it does know exception always thrown).
            return null;
        }
    }

    /**
     * Perform preinvoke processing for a MDB method that must occur
     * prior to EJB arguments becoming known.
     *
     * @param wrapper is the EJSWrapperBase object used by caller
     *            to request preinvoke processing.
     * @param methodId is the integer assigned to the method similar to ejbdeploy.
     *            Must be >= 0 for message-driven beans.
     * @param s is the EJSDeployedSupport object used by the caller
     *            to request preinvoke processing.
     *
     * @return the EnterpriseBean instance that was activated.
     */
    //LIDB2617.11 added this method.  F88119 made method public
    public Object preInvokeMdbActivate(EJSWrapperBase wrapper,
                                       int methodId,
                                       EJSDeployedSupport s) throws Exception {
        EJBMethodInfoImpl methodInfo = mapMethodInfo(s, wrapper, methodId, null);

        if (TraceComponent.isAnyTracingEnabled()) {
            if (TEEJBInvocationInfo.isTraceEnabled())
                TEEJBInvocationInfo.tracePreInvokeBegins(s, wrapper);

            if (tc.isEntryEnabled())
                Tr.entry(tc, "EJBpreInvoke(" + methodId + ":" +
                             methodInfo.getMethodName() + ")");
        }

        // Perform preinvoke processing that must occur prior
        // to activation of the bean and then activate the bean.
        Object bean = preInvokeActivate(wrapper, methodId, s, methodInfo);

        // No exception occured, so return the MessageDrivenBean
        // that was activated.
        return bean; // d414873
    }

    /**
     * Complete preinvoke processing for a MDB method that must occur
     * after EJB arguments become known. <p>
     *
     * Note that this method will account for whether or not JACC support
     * requires the MDB arguments. <p>
     *
     * @param wrapper is the same EJSWrapperBase object that was passed
     *            to the preInvokeMdbActivate method of this class.
     * @param s is the same EJSDeployedSupport object that was passed
     *            to the preInvokeMdbActivate method of this class.
     * @param eb is the EnterpriseBean returned by the preInvokeMdbActivate
     *            method of this class.
     * @param args is an Object array that contains the arguments to be
     *            passed to the EJB method when it is invoked. If the EJB
     *            method takes no arguments, then an empty Object array must
     *            be passed to this method rather than a null reference.
     */
    //LIDB2617.11 added this method.  F88119 made method public
    public void preInvokeMdbAfterActivate(EJSWrapperBase wrapper,
                                          EJSDeployedSupport s,
                                          Object eb, // d414873
                                          Object[] args) throws RemoteException {
        // Now perform preinvoke processing that must occur after
        // the bean is activated.
        preInvokeAfterActivate(wrapper, eb, s, args);

        // If there is a registered message endpoint collaborator, call it for preInvoke processing.
        MessageEndpointCollaborator meCollaborator = getEJBRuntime().getMessageEndpointCollaborator(wrapper.bmd);
        if (meCollaborator != null) {
            HashMap<String, Object> contextData = new HashMap<String, Object>();
            contextData.put(MessageEndpointCollaborator.KEY_ACTIVATION_SPEC_ID, wrapper.bmd.ivActivationSpecJndiName);
            contextData.put(MessageEndpointCollaborator.KEY_J2EE_NAME, wrapper.bmd.j2eeName);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invoking MECollaborator " + meCollaborator + " for preInvoke processing with the following context data: " + contextData);
            }
            
            s.messageEndpointContext = meCollaborator.preInvoke(contextData);
        }

        // Normal path exit.
        if (TraceComponent.isAnyTracingEnabled()) {
            if (tc.isEntryEnabled()) {
                int methodId = s.methodId;
                EJBMethodInfoImpl methodInfo = s.methodInfo;
                ContainerTx containerTx = s.currentTx;
                Tr.exit(tc, "EJBpreInvoke(" + methodId + ":" +
                            methodInfo.getMethodName() + ") " +
                            " Invoking method '" + methodInfo.getMethodName() +
                            "' on bean '" + wrapper.getClass().getName() +
                            "(" + wrapper.beanId + ")" +
                            "', '" + containerTx + "', isGlobalTx=" +
                            (containerTx != null ? (containerTx.isTransactionGlobal() ? "true" : "false") : "Unknown"));
            }

            if (TEEJBInvocationInfo.isTraceEnabled())
                TEEJBInvocationInfo.tracePreInvokeEnds(s, wrapper);
        }
    }

    /**
     * Handle Throwable that occurs during preinvoke processing
     * of an EJB method invocation. This method always throws either
     * RemoteException or EJBException (for local interface and MDB case).
     *
     * @param t is the Throwable that occured during preinvoke.
     * @param wrapper is the EJSWrapperBase passed to preinvoke.
     * @param methodId is the method ID passed to preinvoke.
     * @param s is the EJSDeployedSupport object used by preinvoke.
     * @param methodInfo is the EJBMethodInfo object used by preinvoke.
     * @throws RemoteException if remote interface used to invoke EJB method.
     * @throws EJBException if local interface or message listener interface of
     *             MDB is used to invoke EJB method.
     */
    public void preinvokeHandleException(Throwable t, EJSWrapperBase wrapper,
                                         int methodId, EJSDeployedSupport s,
                                         EJBMethodInfoImpl methodInfo) throws RemoteException {
        // Note, FFDC will be logged in setUncheckedException, as needed.  d350987

        s.preInvokeException = true; // d116274.1
        if (TraceComponent.isAnyTracingEnabled()) {
            if (tc.isEntryEnabled()) {
                ContainerTx containerTx = s.currentTx;
                Tr.exit(tc, "EJBpreInvoke(" + methodId + ":" +
                            methodInfo.getMethodName() + ") " +
                            " Invoking method '" + methodInfo.getMethodName() +
                            "' on bean '" + wrapper.getClass().getName() +
                            "(" + wrapper.beanId + ")" +
                            "', '" + containerTx + "', isGlobalTx=" +
                            (containerTx != null ? (containerTx.isTransactionGlobal() ? "true" : "false") : "Unknown"));
            }

            if (TEEJBInvocationInfo.isTraceEnabled())
                TEEJBInvocationInfo.tracePreInvokeException(s, wrapper, t);
        }

        // Map Throwable to Exception to throw and throw it.
        s.setUncheckedException(t);
    }

    //d139562.14.EJBC Begin
    /**
     * This method is called LinkTargetHelper.getLink when PM wants to provide
     * AccessIntent to use for ejbLink processing.
     * When this method is called, the methodId should be in the negative range to
     * indicate this is a special method with the method signature passed in. This
     * method signature is then used to create the EJSMethodInfo in mapMethodInfo call.
     * The method signature is in the form defined in BeanMetaData.java.
     *
     * methodName ":" [ parameterType [ "," parameterType]* ]+
     *
     * E.g. "findEJBRelationshipRole_Local:java.lang.Object"
     * "noParameterMethod:"
     * ":"
     */
    public EnterpriseBean preInvoke(EJSWrapperBase wrapper,
                                    int methodId,
                                    EJSDeployedSupport s,
                                    EJBMethodInfoImpl methodInfo) throws RemoteException {

        s.methodId = methodId; //130230 d140003.19
        s.ivWrapper = wrapper; // d366807.1

        return preInvokePmInternal(wrapper, methodId, s, methodInfo); //LIDB2617.11 //181971
    }

    //d139562.14.EJBC end

    // d117288 begin
    /**
     * This method is called by the generated code to support PMgr home finder methods.
     * When this method is called, the methodId should be in the negative range to
     * indicate this is a special method with the method signature passed in. This
     * method signature is then used to create the EJSMethodInfo in mapMethodInfo call.
     * The method signature is in the form defined in BeanMetaData.java.
     *
     * methodName ":" [ parameterType [ "," parameterType]* ]+
     *
     * E.g. "findEJBRelationshipRole_Local:java.lang.Object"
     * "noParameterMethod:"
     * ":"
     */
    public EnterpriseBean preInvoke(EJSWrapperBase wrapper,
                                    int methodId,
                                    EJSDeployedSupport s,
                                    String methodSignature) throws RemoteException {
        EJBMethodInfoImpl methodInfo = mapMethodInfo(s, wrapper, methodId,
                                                     methodSignature); //130230 d140003.20 d139562.14.EJBC

        return preInvokePmInternal(wrapper, methodId, s, methodInfo); //LIDB2617.11 //181971
    }

    /**
     * This method is called for non-MDB beans to perform the
     * before activation container collaborator processing and
     * then activates the bean instance. Note, the preinvokeAfterActivate
     * method must be called to complete the preinvoke processing that
     * must occurs after bean is activated.
     *
     * <p><b>pre-condition</b><p>
     * <ul>
     * <li>The methodInfo parameter must be for the method that is
     * identified by the methodId parameter. Use the mapMethodInfo
     * method in this class prior to calling this method to ensure
     * this requirement is met.
     * <li>wrapper must container the BeanId for bean instance
     * being invoked by application and the bean can not be a MDB.
     * Use preinvokeMdbActivate method for MDB beans.
     * </ul>
     * <p><b>post-condition</b><p>
     * <ul>
     * <li>s.methodInfo is set to the EJBMethodInfo object passed to
     * this method as the methodInfo parameter.
     * <li>s.currentTx is set to the transaction context to be used
     * by the ejb method invocation as specified by the TX attribute
     * for the EJB method invoked.
     * <li>All container collaborators that must be invoked prior to
     * activation are called and result from before activation
     * collaborators saved in EJSDeployedSupport.
     * <li>s.beanO contains the BeanO object for activated bean instance.
     * </ul>
     *
     * @param wrapper is the EJSWrapperBase object used by caller
     *            to request preinvoke processing.
     *
     * @param methodId is the integer assigned to the method by ejbdeploy.
     *            Note, when < 0, it is one of the pre-defined constant values
     *            in this class for internal home method calls (that is, methods
     *            that are internal to websphere and hidden from application).
     *
     * @param s is the EJSDeployedSupport object used by the caller
     *            to request preinvoke processing. Note, method
     *            method previnvokeBeforeActivate ensures fields in s
     *            required by this method are filled in with values needed.
     *
     * @param methodInfo is the EJSMethodInfoImpl object returned by the
     *            mapMethodInfo method of this class.
     *
     * @return the EnterpriseBean instance that was activated.
     *
     * @throws RemoteException if any Throwable occurs during the activation
     *             of the bean instance.
     */
    // LIDB2617.11
    // Changed to return Object instead of EnterpriseBean.              d366807.1
    private Object preInvokeActivate(EJSWrapperBase wrapper,
                                     int methodId,
                                     EJSDeployedSupport s,
                                     EJBMethodInfoImpl methodInfo) throws Exception {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2
        EJBThreadData threadData = s.ivThreadData; // d630940
        ContainerTx containerTx = null; // LI3795-56
        s.isLightweight = methodInfo.isLightweight; // LI3795-56

        s.methodInfo = methodInfo; // d140003.19 d114406
        s.ivCallerContext = threadData.pushMethodContext(s); // d515803, d646139.1, RTC102449

        // LIDB2775-23.1
        DispatchEventListenerCookie[] dispatchEventListenerCookies = null; // @MD16633A

        // LIDB2775-23.1 ASV60
        if (isZOS) {
            // To make it very obvious what the problem is, if an attempt is made
            // to invoke an EJB method in the CRA, throw an exception.    d450334.1
            if (isZOSCRA) {
                throw new EJBException("EJB access not permitted in Adjunct process");
            }

            if (ivDispatchEventListenerManager != null &&
                ivDispatchEventListenerManager.dispatchEventListenersAreActive()) {
                // create and stack DispatchContext cookies ...
                dispatchEventListenerCookies = ivDispatchEventListenerManager.getNewDispatchEventListenerCookieArray();
                s.ivDispatchEventListenerCookies = dispatchEventListenerCookies; // d646413.2
            } //@MD16633A
        }

        // Pin and update the LRU data for any managed wrapper     d174057.2
        s.unpinOnPostInvoke = (wrapper.isManagedWrapper) ? wrapperManager.preInvoke(wrapper) : false;

        int isolationLevel = methodInfo.isolationAttr; // d114677 //d140003.20

        //86523.3
        // Pmi instrumentation for methods in beans
        // Use pmiBean cached in the wrapper.                     d140003.33
        EJBPMICollaborator pmiBean = wrapper.ivPmiBean; // d174057.2
        if (pmiBean != null) {
            s.pmiCookie = pmiBean.methodPreInvoke(wrapper.beanId, methodInfo);
            s.pmiPreInvoked = true;
        }

        BeanMetaData bmd = wrapper.bmd;

        // Push the ComponentMetaData onto the thread... for use by other
        // components invoked during the current EJB method call.
        // Note that this was done as the first BeforeActivationCollaborator
        // but for performance, the collaborator is no longer used, and
        // the CMD Accessor is used directly.                            LI3795-56
        threadData.ivComponentMetaDataContext.beginContext(bmd); // d646139.1

        // LI3795-56 - begin
        // -----------------------------------------------------------------------
        // For 'Lightweight' methods, as much of preInvoke and postInvoke is
        // skipped as possible, such as the SecurityCollaborator and setting
        // the class loader.  However, the following will be performed for
        // 'Lightweight' methods:
        //  - MethodContext (deployedsupport) will be pushed onto the current
        //    thread (see methodContext.beginContext above).
        //  - ComponentMetaData (bmd) will be pushed onto the current thread
        //    (see ivCMDAccessor.beginContext above).
        //  - Transaction collaborator may be called if a ContainerTx does
        //    not exist on the thread, or the Tx Attribute does not support
        //    a global transaction.
        //  - PMI and SMF(zOS) metrics will be collected if configured.
        // -----------------------------------------------------------------------

        // LI3795-56 - end
        // -----------------------------------------------------------------------
        // When not running in 'Lightweight' mode, perform full preInvoke and
        // postInvoke processing....
        // -----------------------------------------------------------------------
        if (!s.isLightweight) {
            // 112678.6
            // For a remote method, the preInvokeORBDispatch method interface is
            // called prior to this method, which already set thread context
            // loader, so only do this processing for a "local" method.   d115602-1
            if (methodInfo.setClassLoader) {
                s.oldClassLoader = EJBThreadData.svThreadContextAccessor.pushContextClassLoaderForUnprivileged(bmd.ivContextClassLoader); // d369927, PK83186, F85059
            }
            //end 89188

            if (ivBeforeActivationCollaborators != null) { //87918.8(2)
                for (int i = 0; i < ivBeforeActivationCollaborators.length; i++) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "preInvokeActivate : Invoking EJBComponentInitializationCollaborator.preInvoke method on: "
                                     + ivBeforeActivationCollaborators[i].getClass().getName());
                    //d140003.19
                    Object cookie = ivBeforeActivationCollaborators[i].preInvoke(s);
                    if (cookie != null) {
                        if (s.ivBeforeActivationCookies == null) {
                            s.ivBeforeActivationCookies = new Object[ivBeforeActivationCollaborators.length];
                        }
                        s.ivBeforeActivationCookies[i] = cookie;
                    }
                    s.ivBeforeActivationPreInvoked++; // d228192
                    //d140003.19
                }
            }

            //92702
            if (ivBeforeActivationAfterCompletionCollaborators != null) {
                int n = ivBeforeActivationAfterCompletionCollaborators.length;
                for (int i = 0; i < n; i++) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "preInvokeActivate : Invoking BeforeActivationAfterCompletionCollaborator.preInvoke method on: "
                                     + ivBeforeActivationAfterCompletionCollaborators[i].getClass().getName());

                    //d140003.19
                    Object cookie = ivBeforeActivationAfterCompletionCollaborators[i].preInvoke(s);
                    if (cookie != null) {
                        if (s.ivBeforeActivationAfterCompletionCookies == null) {
                            s.ivBeforeActivationAfterCompletionCookies = new Object[ivBeforeActivationAfterCompletionCollaborators.length];
                        }
                        s.ivBeforeActivationAfterCompletionCookies[i] = cookie;
                    }
                    s.ivBeforeActivationAfterCompletionPreInvoked++; // d228192
                    //d140003.19
                }
            }
            //end 92702
        } // LI3795-56

        if (methodInfo.isLightweightTxCapable) {
            if (s.isLightweight) {
                // A true lightweight bean.  Only reuse the caller's transaction if
                // it was established by another EJB and runUnderUOW has not been
                // called (i.e. user initiated tx).                        LI4548-11
                EJSDeployedSupport sCaller = s.ivCallerContext;
                if (sCaller != null &&
                    sCaller.ivRunUnderUOW == 0) {
                    ContainerTx currentTx = sCaller.currentTx;
                    if (currentTx != null && currentTx.isActiveGlobal()) {
                        containerTx = currentTx;
                        s.currentTx = containerTx;
                        containerTx.preInvoke(s);
                    }
                }
            } else {
                // An automatic lightweight transaction method.  Reuse the
                // transaction context if possible.                         F61004.1
                SynchronizationRegistryUOWScope uow = uowCtrl.getCurrentTransactionalUOW(false);
                if (uow != null) {
                    // For SFSB, the optimization simulates enlist and commit of the
                    // active transaction for proper state transitions.  If a global
                    // transaction is active, CMT SFSB might already be enlisted, so
                    // we skip the optimization to avoid wrong state transitions.
                    boolean isLocal = uow.getUOWType() != UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION;
                    if (isLocal ||
                        bmd.type != InternalConstants.TYPE_STATEFUL_SESSION) {
                        // Ensure the caller transaction has not timed out.  This is
                        // an observable side effect of calling an EJB method, and it
                        // seems probable that a customer would make a call to a
                        // trivial EJB method just to check transaction timeout.
                        uowCtrl.completeTxTimeout();

                        containerTx = getCurrentTx(uow, isLocal);
                        s.currentTx = containerTx;
                        containerTx.preInvoke(s);
                    }
                }
            }

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Lightweight: Tx = " + containerTx);
        }

        // If the ContainerTx from a calling EJB is not being used (i.e.
        // 'Lightweight' mode), then invoke the transaction collaborator
        // and locate or create the ContainerTx as necessary, per the
        // method transaction attribute.                                 LI3795-56
        if (containerTx == null) {
            // NOTE: The following code to setup the transaction is 'duplicated'
            //       in transitionToStickyGlobalTransaction(...) as this may need
            //       to be redone when there is concurrent access for a BMT
            //       stateful bean with a sticky global transaction.        d671368

            //d140003.19
            s.uowCookie = uowCtrl.preInvoke(wrapper.beanId, methodInfo);
            s.uowCtrlPreInvoked = true;
            //d140003.19

            // Get the Container Transaction object (ContainerTx) associated
            // with the current transaction, as returned in the UOW Cookie
            // on UOW preInvoke.  This may return null for BMT (either for
            // EJB 1.1 modules or if the tran has rolled back) or it may
            // result in a CSITransactionRolledBack if the tran has been
            // rolled back or the Container was unable to enlist.         d139352-2
            boolean isLocal = s.uowCookie.isLocalTx();
            SynchronizationRegistryUOWScope UOWid = s.uowCookie.getTransactionalUOW();
            containerTx = getCurrentTx(UOWid, isLocal); // d161864

            if (containerTx != null) {
                s.currentTx = containerTx;//d140003.19
                containerTx.preInvoke(s);

                //d140003.20
                if (bmd.ivModuleVersion <= BeanMetaData.J2EE_EJB_VERSION_1_1 ||
                    (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_2_0 &&
                     bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X)) {
                    containerTx.setIsolationLevel(isolationLevel);
                }
                //d140003.20

                s.currentIsolationLevel = isolationLevel;
            }
        } // LI3795-56

        // LIDB2775-23.1 ASV60
        if (isZOS && dispatchEventListenerCookies != null) {
            // notify event dispatch listeners
            this.ivDispatchEventListenerManager.callDispatchEventListeners(DispatchEventListener.BEGIN_DISPATCH, dispatchEventListenerCookies, methodInfo); // @MD16426A
        }

        //******************************************************************
        // Note, we are required by EJB spec to discard bean instance if an
        // unchecked exception occurs from bean provider written code.  We are
        // not required to discard if unchecked exception comes from WAS code
        // rather than bean provider code.  Since all of the code prior to
        // activation of bean is WAS code and not bean provider code (we know
        // this since we do not have a bean instance until bean is activated),
        // then all unchecked exceptions prior to this point should NOT discard
        // the bean.   During activation, it is possible that bean provider written
        // code is called as a result of EJB callback methods that are invoked during
        // the activation (e.g setEntityBeanContext, setSessionBeanContext, ejbactivate,
        // ejbload, etc).   The code executed in activation path is responsible for
        // determining whether unchecked came from bean provider written code or from
        // WAS code.  If the former, then ensure discard occurs.  If the latter, we
        // prefer not to discard (although the latter behavior is not required
        // by EJB spec) to ensure existing testcases continue to work as currently
        // implemented by various test teams.  This may or may not be implemented
        // correctly at this moment in time.  However, if that is the case, then the
        // other WAS code needs to be fixed, not the code in this method of this class.
        // One final note is prior to activateBean call, s.beanO must be set to null and
        // s.preinvokeException set to false.  This ensures postInvoke method of this
        // class does nothing since any unchecked exceptions that occur prior to activate
        // and/or during activation is handled as previously described.
        //***************************************************************************
        BeanO beanO = activator.preInvokeActivateBean(threadData, containerTx, wrapper.beanId); // d114677, d630940, d641259

        // Activator will have pushed CallbackBeanO, except for home beans, which
        // are identified by a null home.                                  d694142
        s.ivPopCallbackBeanORequired = beanO.home != null;

        //************************************************************************
        // Now that we have a bean instance, we want to save its reference in
        // deployed support so that the postInvoke method of this class can correctly
        // process any unchecked exception that occurs from this point on.
        // Tthe postInvoke method of this class must check both s.beanO and s.preinvoke
        // to determine what action to take. If unchecked exception occurred while still
        // in preinvoke, s.preinvoke will be set to true.  In this case, the bean instance
        // should not be discarded since we know the unchecked exception did not come from
        // bean provider code (e.g not from the business method).  If s.preinvoke is false
        // and s.beanO != null, then we know the unchecked exception came from the
        // business method itself.  In that case, we must discard the bean instance
        // as required by the EJB spec  .
        //************************************************************************

        // Note, keep these 2 lines together and do not insert
        // anything between them.  Otherwise, if you put something
        // between them that causes an exception, then postinvoke will
        // more than likely fail with InvalidBeanOStateException.
        s.beanO = beanO; //d140003.19
        Object bean = beanO.preInvoke(s, containerTx);

        // return EJB that was activated/loaded.
        return bean;
    }

    /*
     * Common preInvoke processings that must occur after activating
     * the bean instance.
     *
     * <p><b>pre-condition</b><p>
     * <ul>
     * <li>The preInvokeActivate method in this class must be
     * called prior calling this method.
     * </ul>
     * <p><b>post-condition</b><p>
     * <ul>
     * <li>All container collaborators that must be invoked after
     * activation of bean instance are called,
     * including the SecurityCollaborator.
     * <li>s.ivEJBArguments is set to the args parameter passed to this method.
     * </ul>
     *
     * @param wrapper is the same EJSWrapperBase object that was passed
     * to the preInvokeActivate method of this class.
     *
     * @param eb is the EnterpriseBean returned by the preInvokeActivate
     * method of this class.
     *
     * @param s is the same EJSDeployedSupport object that was passed
     * to the preInvokeActivatemethod of this class.
     *
     * @param args is an Object array that contains the arguments to be
     * passed to the EJB method when it is invoked. If the EJB
     * method takes no arguments, then an empty Object array must
     * be passed to this method rather than a null reference.
     *
     * @throws RemoteException if any Throwable occurs during the activation
     * of the bean instance.
     */
    //  LIDB2617.11
    private void preInvokeAfterActivate(EJSWrapperBase wrapper,
                                        Object bean, // d366807.1
                                        EJSDeployedSupport s,
                                        Object[] args) throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        // Remember EJB arguments in EJSDeployedSupport.  Needed
        // to cover case where EJB method decides to call isCallerInRole
        // method on the EJBContext object.
        s.ivEJBMethodArguments = args;

        // Security and AfterActivation collaborators are executed after
        // bean activation, unless the current method is 'Lightweight'.  LI3795-56
        if (!s.isLightweight) {
            try {
                // LIDB2167.11
                // Now that we have both EJB instance and the EJB arguments,
                // we can call security collaborator preInvoke. Note, this code
                // must remain after beanO.preinvoke so that if security exception
                // occurs, the postinvoke will work without encountering a
                // InvalidBeanOStateException.
                EJBSecurityCollaborator<?> securityCollaborator = this.ivSecurityCollaborator;
                if (securityCollaborator != null && s.methodId > -1) {
                    // For internal PM methods with negative methodId, do not invoke
                    // security collaborator.
                    s.securityCookie = notifySecurityCollaboratorPreInvoke(securityCollaborator, s); //LIDB2617.11
                    s.ivSecurityCollaborator = securityCollaborator; // d139352-2
                }

                if (ivAfterActivationCollaborators != null) {
                    for (int i = 0; i < ivAfterActivationCollaborators.length; i++) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "preInvokeAfterActivate : Invoking AfterActivationCollaborator.preInvoke method on: "
                                         + ivAfterActivationCollaborators[i].getClass().getName());

                        //PQ44001
                        //d140003.19
                        Object cookie = ivAfterActivationCollaborators[i].preInvoke(s);
                        if (cookie != null) {
                            if (s.ivAfterActivationCookies == null) {
                                s.ivAfterActivationCookies = new Object[ivAfterActivationCollaborators.length];
                            }
                            s.ivAfterActivationCookies[i] = cookie;
                        }
                        s.ivAfterActivationPreInvoked++; // d228192
                        //d140003.19
                    }
                }
            } catch (RuntimeException ex) {
                throw ex;
            } catch (CSIException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new CSIException("", ex);
            }
        } // LI3795-56
          // LI2775-107.2 Begins - MD17809
        if (isZOS) {
            DispatchEventListenerCookie[] dispatchEventListenerCookies = s.ivDispatchEventListenerCookies;
            if (dispatchEventListenerCookies != null) {
                // notify event dispatch listeners
                // Note: It is important that the 'beforeEjbMethod' notification
                //       occur as the last event during preInvoke so that this
                //       signal is delivered as close as possible to the actual
                //       invocation of the bean method.
                this.ivDispatchEventListenerManager.callDispatchEventListeners(DispatchEventListener.BEFORE_EJBMETHOD,
                                                                               dispatchEventListenerCookies, // d646413.2
                                                                               s.methodInfo);
            }
        }
        // LI2775-107.2 Ends
    }

    /**
     * Notify the security collaborator for pre-invoke. This method handles
     * exception mapping for EJBSecurityCollaborator that throw
     * EJBAccessException rather than the legacy CSIAccessException.
     */
    private Object notifySecurityCollaboratorPreInvoke(EJBSecurityCollaborator<?> collaborator, EJBRequestData request) throws CSIException {
        try {
            return collaborator.preInvoke(request);
        } catch (EJBAccessException ex) {
            // The CSI exception created here will eventually be mapped back to an
            // EJBAccessException, so avoid setting the cause so that we don't end
            // up with an EJBAccessException chained to an EJBAccessException.
            // Also, security actually throws an 'internal' subclass of
            // EJBAccessException that will not deserialize on a client.
            CSIAccessException csiEx = new CSIAccessException(ex.getMessage());
            csiEx.setStackTrace(ex.getStackTrace());
            throw csiEx;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (CSIException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CSIException("", ex);
        }
    }

    /**
     * An optimized version of EJB preInvoke specifically for the create
     * method of a Stateless Session Home. <p>
     *
     * When a Stateless Session Home 'create' method is invoked, it does
     * not result in any 'customer' code being called, so many of the
     * functions normally required during preInvoke processing may
     * be skipped.... thus improving performance. <p>
     *
     * This method is NOT called directly by generated code. Instead,
     * the normal preInvoke detects that the call is for a Stateless
     * create method, and re-directs preInvoke to this method. <p>
     **/
    // d110126
    private EnterpriseBean preInvokeForStatelessSessionCreate(EJSWrapperBase wrapper,
                                                              int methodId, // f111627
                                                              EJSDeployedSupport s,
                                                              EJBMethodInfoImpl methodInfo,
                                                              Object[] args) throws RemoteException {
        s.methodInfo = methodInfo; // d140003.19 d114406 LIDB2617.11
        s.ivEJBMethodArguments = args; // LIDB2617.11
        EnterpriseBean eb = null;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        try {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.entry(tc, "EJBpreInvokeForStatelessSessionCreate(" + methodId +
                             ":" + methodInfo.getMethodName() + ")"); // d161864

            s.ivThreadData.pushMethodContext(s); // 127257, d646139.1, RTC102449

            // Pin and update the LRU data for any managed wrapper        d174057.2
            s.unpinOnPostInvoke = (wrapper.isManagedWrapper) ? wrapperManager.preInvoke(wrapper) : false;

            //d173022.7 begins
            // Only invoke security collaborator if security is enabled.
            EJBSecurityCollaborator<?> securityCollaborator = ivSecurityCollaborator;
            if (securityCollaborator != null) {
                s.securityCookie = notifySecurityCollaboratorPreInvoke(securityCollaborator, s); //LIDB2617.11
                s.ivSecurityCollaborator = securityCollaborator;
            }

            // Since we know this method is only called when create is called on the
            // home wrapper for a Stateless SessionBean, we know that the SessionBean
            // this method will return is the home bean for the Stateless SessionBean.
            // Therefore, we can skip most of the "normal" preinvoke processing and
            // just return the singleton SessionBean bean that is kept in the StatelessBeanO
            // for the home associated with beanId in the home wrapper.
            // Actually, to avoid the cache lookup of the home in the HomeOfHomes,
            // and accessing the BeanO, the home may be accessed directly from
            // the BeanMetaData referenced by the Wrapper.                  d221309
            eb = (EnterpriseBean) wrapper.bmd.homeRecord.homeInternal;
            //173022.7 ends

            if (isTraceOn) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "EJBpreInvokeForStatelessSessionCreate(" + methodId +
                                ":" + methodInfo.getMethodName() + ") " +
                                " Invoking method '" + methodInfo.getMethodName() +
                                "' on bean '" + wrapper.getClass().getName() +
                                "(" + wrapper.beanId + ")");

                if (TEEJBInvocationInfo.isTraceEnabled()) // LIDB2617.11
                    TEEJBInvocationInfo.tracePreInvokeEnds(s, wrapper);
            }

            return eb; // LIDB2617.11
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, CLASS_NAME +
                                            ".preInvokeForStatelessSessionCreate",
                                        "2139", new Object[] { this, wrapper, Integer.valueOf(methodId), s, methodInfo });//123338

            s.preInvokeException = true; // d116274.1

            if (isTraceOn) {
                if (tc.isEntryEnabled()) // LIDB2617.11
                    Tr.exit(tc, "EJBpreInvokeForStatelessSessionCreate(" + methodId +
                                ":" + methodInfo.getMethodName() + ") " +
                                " Invoking method '" + methodInfo.getMethodName() +
                                "' on bean '" + wrapper.getClass().getName() +
                                "(" + wrapper.beanId + ")");

                if (TEEJBInvocationInfo.isTraceEnabled()) // LIDB2617.11
                    TEEJBInvocationInfo.tracePreInvokeException(s, wrapper, ex);
            }

            s.setUncheckedException(ex);

            return null; // LIDB2617.11
        }
    }

    /**
     * An optimized version of EJB preInvoke spcifically for the create
     * method of a Stateless Session Home, that IS called directly
     * by the wrappers generated by JITDeploy. <p>
     *
     * When a Stateless Session Home 'create' method is invoked, it does
     * not result in any 'customer' code being called, so many of the
     * functions normally required during preInvoke processing may
     * be skipped.... thus improving perormance. <p>
     *
     * This method IS called directly by generated code. It is very similar
     * to preInvokeForStatelessSessionCreate, but was introduced with EJB 3.0,
     * so that it could be called directly by the JITDeploy generated wrappers
     * to improve performance. The older version of this method could not be
     * called directly, as it relied on the normal flow preInvoke to obtain
     * the methodId and perform tracing. <p>
     **/
    // d413752
    public Object EjbPreInvokeForStatelessCreate(EJSWrapperBase wrapper,
                                                 int methodId,
                                                 EJSDeployedSupport s,
                                                 Object[] args) throws RemoteException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        // Get the EJBMethodInfoImpl object for the methodId
        // assigned to this method by JITDeploy/EJBbdeploy tool.
        EJBMethodInfoImpl methodInfo = mapMethodInfo(s, wrapper, methodId, null);

        try {
            if (isTraceOn) {
                if (TEEJBInvocationInfo.isTraceEnabled())
                    TEEJBInvocationInfo.tracePreInvokeBegins(s, wrapper);

                if (tc.isEntryEnabled())
                    Tr.entry(tc, "EjbPreInvokeForStatelessCreate(" + methodId + ":" +
                                 methodInfo.getMethodName() + ")");

                if (tcClntInfo.isDebugEnabled())
                    Tr.debug(tcClntInfo, "preInvoke(" + methodInfo.getMethodName() + ")");
            }

            s.methodInfo = methodInfo;
            s.ivEJBMethodArguments = args;

            s.ivThreadData.pushMethodContext(s); // d646139.1, RTC102449

            // Pin and update the LRU data for any managed wrapper        d174057.2
            s.unpinOnPostInvoke = (wrapper.isManagedWrapper) ? wrapperManager.preInvoke(wrapper) : false;

            //d173022.7 begins
            // Only invoke security collaborator if security is enabled.
            EJBSecurityCollaborator<?> securityCollaborator = ivSecurityCollaborator;
            if (securityCollaborator != null) {
                s.securityCookie = notifySecurityCollaboratorPreInvoke(securityCollaborator, s);
                s.ivSecurityCollaborator = securityCollaborator;
            }

            // Since we know this method is only called when create is called on the
            // home wrapper for a Stateless SessionBean, we know that the SessionBean
            // this method will return is the home bean for the Stateless SessionBean.
            // Therefore, we can skip most of the "normal" preinvoke processing and
            // just return the singleton SessionBean bean that is kept in the StatelessBeanO
            // for the home associated with beanId in the home wrapper.
            // Actually, to avoid the cache lookup of the home in the HomeOfHomes,
            // and accessing the BeanO, the home may be accessed directly from
            // the BeanMetaData referenced by the Wrapper.                  d221309
            Object bean = wrapper.bmd.homeRecord.homeInternal;

            if (isTraceOn) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "EjbPreInvokeForStatelessCreate(" + methodId +
                                ":" + methodInfo.getMethodName() + ") " +
                                " Invoking method '" + methodInfo.getMethodName() +
                                "' on bean '" + wrapper.getClass().getName() +
                                "(" + wrapper.beanId + ")");

                if (TEEJBInvocationInfo.isTraceEnabled())
                    TEEJBInvocationInfo.tracePreInvokeEnds(s, wrapper);
            }

            return bean; // LIDB2617.11
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, CLASS_NAME +
                                            ".EjbPreInvokeForStatelessCreate",
                                        "3894",
                                        new Object[] { this, wrapper,
                                                       Integer.valueOf(methodId), s, methodInfo });

            s.preInvokeException = true;

            if (isTraceOn) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "EjbPreInvokeForStatelessCreate(" + methodId +
                                ":" + methodInfo.getMethodName() + ") " +
                                " Invoking method '" + methodInfo.getMethodName() +
                                "' on bean '" + wrapper.getClass().getName() +
                                "(" + wrapper.beanId + ")");

                if (TEEJBInvocationInfo.isTraceEnabled())
                    TEEJBInvocationInfo.tracePreInvokeException(s, wrapper, ex);
            }

            s.setUncheckedException(ex);

            return null;
        }
    }

    /**
     * An special version of EJB preInvoke spcifically for ManagedBeans
     * with AroundInvoke interceptors. This is called directly by the
     * wrappers generated by JITDeploy. <p>
     *
     * When a ManagedBean with interceptors is invoked, very little needs
     * to be done except set up the EJSDeployedSupport object for the
     * interceptor processing code. ManagedBeans receive not other container
     * services. <p>
     *
     * There is no corresponding postInvoke. Nothing is performed in this
     * method that requires any postInvoke processing. <p>
     **/
    // F743-34301.1
    public Object EjbPreInvokeForManagedBean(EJSWrapperBase wrapper,
                                             int methodId,
                                             EJSDeployedSupport s,
                                             BeanO beanO,
                                             Object[] args) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // Get the EJBMethodInfoImpl object for the methodId
        // assigned to this method by JITDeploy/EJBbdeploy tool.
        EJBMethodInfoImpl methodInfo = mapMethodInfo(s, wrapper, methodId, null);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "EjbPreInvokeForManagedBean(" + methodId + ":" +
                         methodInfo.getMethodName() + ")");

        s.methodInfo = methodInfo;
        s.ivEJBMethodArguments = args;
        s.beanO = beanO;
        s.unpinOnPostInvoke = false;

        Object bean = beanO.getBeanInstance();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "EjbPreInvokeForManagedBean(" + methodId +
                        ":" + methodInfo.getMethodName() + ") " +
                        " Invoking method '" + methodInfo.getMethodName() +
                        "' on bean " + Util.identity(bean));
        }

        return bean;
    }

    // F743-1751
    /**
     * A subset of preInvoke specifically for the lifecycle interceptors of a
     * Singleton Session bean. This method must be paired with a call to
     * postInvokeForLifecycleInterceptors.
     *
     * <p>Exceptions that are thrown from this method,
     * setUncheckedLocalException, or postInvokeForLifecycleInterceptors must
     * also be wrapped in EJBException to avoid causing the resulting exception
     * from being interpreted as an application exception by the preInvoke of
     * the method invocation that is attempting to activate this bean.
     */
    public void preInvokeForLifecycleInterceptors(LifecycleInterceptorWrapper wrapper,
                                                  int methodId,
                                                  EJSDeployedSupport s,
                                                  BeanO beanO) throws RemoteException {
        // NOTE: The implementation of this method is structured after preInvoke.
        // For a detailed explanation of the operations here, see the comments in
        // that method.

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        EJBThreadData threadData = s.ivThreadData; // d630940
        EJBMethodInfoImpl methodInfo = mapMethodInfo(s, wrapper, methodId, null);

        if (isTraceOn) {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "preInvokeForLifecycleInterceptors(" + methodId + ":" +
                             methodInfo.getMethodName() + ")");

            if (tcClntInfo.isDebugEnabled())
                Tr.debug(tcClntInfo, "preInvoke(" + methodInfo.getMethodName() + ")");
        }

        BeanMetaData bmd = wrapper.bmd;

        s.methodInfo = methodInfo;
        s.ivCallerContext = threadData.pushMethodContext(s); // d646139.1
        threadData.ivComponentMetaDataContext.beginContext(bmd); // d578360, d646139.1, RTC102449

        // d578360 - Normally, updateCallbackBeanO would be called via
        // EJSHome.createBeanO or ActivationStrategy.atActivate (for cached
        // instances), but we skipped that step since we started with the
        // BeanO, which is unlike the other pre/postInvoke.
        threadData.pushCallbackBeanO(beanO); // d630940
        s.beanO = beanO;
        s.ivPopCallbackBeanORequired = true; // RTC112628

        // d598062 - Always set context class loader for lifecycle methods.
        s.oldClassLoader = EJBThreadData.svThreadContextAccessor.pushContextClassLoaderForUnprivileged(bmd.ivContextClassLoader); // F85059

        try {
            // Set up transaction context
            s.uowCookie = uowCtrl.preInvoke(wrapper.beanId, methodInfo);
            s.uowCtrlPreInvoked = true;
            s.currentTx = getCurrentTx(s.uowCookie.getTransactionalUOW(), false);
            s.currentTx.preInvoke(s);
            s.currentIsolationLevel = methodInfo.isolationAttr;

            // Set up security context
            EJBSecurityCollaborator<?> securityCollaborator = ivSecurityCollaborator;
            if (securityCollaborator != null) {
                s.securityCookie = notifySecurityCollaboratorPreInvoke(securityCollaborator, s);
                s.ivSecurityCollaborator = securityCollaborator;
            }

            if (isTraceOn) {
                if (tc.isEntryEnabled()) {
                    ContainerTx containerTx = s.currentTx;
                    Tr.exit(tc, "preInvokeForLifecycleInterceptors(" + methodId + ":" +
                                methodInfo.getMethodName() + ") " +
                                " Invoking method '" + methodInfo.getMethodName() +
                                "' on bean '" + wrapper.getClass().getName() +
                                "(" + wrapper.beanId + ")" +
                                "', '" + containerTx + "', isGlobalTx=" +
                                (containerTx != null ? (containerTx.isTransactionGlobal() ? "true" : "false") : "Unknown"));
                }

                if (TEEJBInvocationInfo.isTraceEnabled())
                    TEEJBInvocationInfo.tracePreInvokeEnds(s, wrapper);
            }
        } catch (Throwable t) {
            preinvokeHandleException(t, wrapper, methodId, s, methodInfo);
        }
    }

    /**
     * Perform container actions on completion of bean method. <p>
     *
     * This method is called after every bean method, regardless of whether
     * the bean method throws an exception. It's responsibility is to inform
     * the transaction control of the method termination, determine the
     * outcome of the transaction associated with this method invocation, if
     * any, and return an exception to be thrown to the client of this
     * method, if appropriate. <p>
     *
     * @param wrapper the <code>EJSWrapper</code> instance that the
     *            method was invoked on <p>
     *
     * @param methodId an <code>int</code> indicating which method on
     *            the bean is being completed <p>
     *
     */
    public void postInvoke(EJSWrapper wrapper, int methodId,
                           EJSDeployedSupport s) throws RemoteException {
        postInvoke((EJSWrapperBase) wrapper, methodId, s); // f111627
    }

    /**
     * Introduced this postInvoke( EJSWrapperBase...) for Local Interface support.
     * The old postInvoke( EJSWrapper...) must still be support for backward
     * compatible with old generated code base.
     */
    public void postInvoke(EJSWrapperBase wrapper, int methodId,
                           EJSDeployedSupport s) throws RemoteException {
        // LIDB2775-23.1
        DispatchEventListenerCookie[] dispatchEventListenerCookies = null;// @MD16426A
        EJBMethodInfoImpl methodInfo = s.methodInfo; // d114406 d154342.4 d173022
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        // If this is a stateless SessionBean create, run only the bare minimum of
        // collaborators and other processing. This path is encapsulated in a separate
        // method for now, to avoid lots of "if" statements within this method.
        if (methodInfo.isStatelessSessionBean && methodInfo.isHomeCreate) {
            EjbPostInvokeForStatelessCreate(wrapper, methodId, s);
        } else {
            if (isTraceOn) {
                if (TEEJBInvocationInfo.isTraceEnabled())
                    TEEJBInvocationInfo.tracePostInvokeBegins(s, wrapper); // d161864

                if (tcClntInfo.isDebugEnabled())
                    Tr.debug(tcClntInfo, "postInvoke(" + methodInfo.getMethodName() + ")");

                if (tc.isEntryEnabled())
                    Tr.entry(tc, "EJBpostInvoke(" + methodId + ":" +
                                 methodInfo.getMethodName() + ")");
            }

            EJBThreadData threadData = s.ivThreadData; // d646139.1
            BeanId beanId = wrapper.beanId;
            BeanO beanO = null;
            BeanMetaData bmd = null;//92702

            // UOWManager.runUnderUOW may have left cached currentTx invalid; reset
            if (s.resetCurrentTx) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "s.currentTx may be invalid; reset");
                s.currentTx = getCurrentContainerTx();
            }

            // LIDB2775-23.1
            // Do SMF after method recording - // MD16426A - begin
            if (isZOS) {
                dispatchEventListenerCookies = s.ivDispatchEventListenerCookies;
                if (dispatchEventListenerCookies != null) {
                    // notify event dispatch listeners -
                    // Note: It is important that the 'afterEjbMethod' notification
                    //       occur as the first event during postInvoke so that this
                    //       signal is delivered as close as possible to the actual return
                    //       from the bean method invocation.
                    this.ivDispatchEventListenerManager.callDispatchEventListeners(DispatchEventListener.AFTER_EJBMETHOD, dispatchEventListenerCookies, methodInfo);// @MD16426A
                } // @MD16426A
            }

            try {

                beanO = s.beanO;//d140003.19

                if (s.exType != ExceptionType.NO_EXCEPTION) { // d113344  //d140003.19
                    //87918.8(2)
                    if (s.exType == ExceptionType.CHECKED_EXCEPTION) { // d113344 //d140003.19
                        if (isTraceOn && tc.isEventEnabled())
                            Tr.event(tc, "Bean method threw exception",
                                     s.getException());//d140003.19
                    } else if (s.exType == ExceptionType.UNCHECKED_EXCEPTION) {
                        if (isTraceOn && tc.isEventEnabled())
                            Tr.event(tc, "Bean method threw unchecked exception",
                                     s.getException()); //d140003.19
                        // d113344
                        if (beanO != null && s.preInvokeException == false) {
                            // This indicates unchecked exception came from the business
                            // method itself, so we want to discard the bean instance.
                            beanO.discard();
                        }

                        if (isTraceOn && tc.isEventEnabled())
                            Tr.event(tc, "Bean Discarded as non null");

                    }
                }

                //89188
                // bmd = (BeanMetaData) internalBeanMetaDataStore.get(wrapper.beanId.getJ2EEName());//89554
                // d110126
                // Optimizations to preInvoke/postInvoke processing:
                // Created specialized case for stateless SessionBean creation
                // BeanMetaData no longer looked up via hashtable, is now stored in the EJSWrapper
                // Removed extraneous call to TxCntl.getCurrentTransaction (dead code)

                bmd = wrapper.bmd; // d110126
                //end 89188

                // Call postInvoke on all of the BeforeActivation Collaborators
                // that were successfully preInvoked. Note that the order is the
                // same as preInvoke. Note the ComponentMetaDataCollaborator is
                // no longer in this list; see below.              d228192 LI3795-56
                if (ivBeforeActivationCollaborators != null) {
                    for (int i = 0; i < s.ivBeforeActivationPreInvoked; i++) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "postInvoke : Invoking BeforeActivationCollaborator.postInvoke method on: "
                                         + ivBeforeActivationCollaborators[i].getClass().getName());

                        Object cookie = s.ivBeforeActivationCookies == null ? null : s.ivBeforeActivationCookies[i]; // F61004.3
                        notifyPostInvoke(ivBeforeActivationCollaborators[i], s, cookie);
                    }
                }

                // Call postInvoke on all of the AfterActivation Collaborators
                // that were successfully preInvoked. Note that the order is the
                // same as preInvoke.                                        d228192
                if (ivAfterActivationCollaborators != null) {
                    for (int i = 0; i < s.ivAfterActivationPreInvoked; i++) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "postInvoke : Invoking AfterActivationCollaborator.postInvoke method on: "
                                         + ivAfterActivationCollaborators[i].getClass().getName());

                        Object cookie = s.ivAfterActivationCookies == null ? null : s.ivAfterActivationCookies[i]; // F61004.3
                        notifyPostInvoke(ivAfterActivationCollaborators[i], s, cookie);
                    }
                }

                if (beanO != null) {
                    beanO.postInvoke(methodId, s);
                }

            } catch (RemoteException ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".postInvoke", "2268", new Object[] { this, wrapper, Integer.valueOf(methodId), s });//123338
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "postInvoke failed", ex);
                throw ex;
            } catch (Throwable ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".postInvoke", "2273", new Object[] { this, wrapper, Integer.valueOf(methodId), s });//123338
                if (isTraceOn && tc.isEventEnabled())
                    Tr.event(tc, "postInvoke failed", ex);

                s.setUncheckedException(ex); //d117817
            } finally {

                // LIDB2775-23.1
                if (isZOS && dispatchEventListenerCookies != null) {
                    // Before transaction context changes, do SMF end dispatch recording
                    this.ivDispatchEventListenerManager.callDispatchEventListeners(DispatchEventListener.END_DISPATCH, dispatchEventListenerCookies, methodInfo); // @MD16426A
                }

                final ContainerTx currentTx = s.currentTx;//d140003.19

                try {
                    if (beanO != null) {
                        activator.postInvoke(currentTx, beanO);
                    }

                    if (currentTx != null) {
                        currentTx.postInvoke(s);
                    }

                    if (s.uowCtrlPreInvoked) {
                        if (s.began && currentTx != null && currentTx.ivPostInvokeContext == null) {
                            // Indicate that afterCompletion should call
                            // postInvokePopCallbackContexts.                 RTC107108
                            // Don't do see if this is a nested EJB call from
                            // beforeCompletion.                              RTC115108
                            currentTx.ivPostInvokeContext = s;
                        }

                        uowCtrl.postInvoke(beanId, s.uowCookie, s.exType, methodInfo); // d113344 //d140003.19
                    }
                    // Since 'Lightweight' methods may not be executing the
                    // Transaction Collaborator, the postInvoke processing must
                    // be done here.                                        LI3795-56
                    else if (methodInfo.isLightweightTxCapable && s.currentTx != null) {
                        if (s.isLightweight) {
                            if (s.exType == ExceptionType.UNCHECKED_EXCEPTION) {
                                // In this scenario, there is always an inherited global
                                // transaction, and the correct action for all Tx
                                // Strategies is to mark the tx as rollbackonly and throw
                                // the CSITransactionRolledbackException, forcing the
                                // catch block below.
                                if (isTraceOn && tc.isDebugEnabled())
                                    Tr.debug(tc, "Lightweight:handleException - rollback");
                                uowCtrl.setRollbackOnly();
                                throw new CSITransactionRolledbackException("Unexpected Exception from Lightweight EJB method");
                            }
                        } else if (beanO != null &&
                                   bmd.type == InternalConstants.TYPE_STATEFUL_SESSION) {
                            // Simulate transaction commit for the bean.  We do not
                            // need to call ContainerTx.delist because we avoided
                            // ContainerTx.enlist during activation.            F61004.1
                            beanO.beforeCompletion();
                            simulateCommitBean(beanO, currentTx);
                        }
                    }

                    // If a remove method was invoked, and the bean instance was
                    // NOT removed during afterCompletion, then the transaction
                    // did not commit.. and thus a RemoveException should be
                    // thrown.  Stateful beans may NOT be removed while still
                    // enlisted in a transaction.  The RemoveException will be
                    // wrapped in the appropriate exception, as it is generally
                    // not on the throws clause for @Remove methods.           390657
                    if (currentTx != null &&
                        currentTx.ivRemoveBeanO != null) {
                        // Per a SUN (CTS) clarification, not removing a SF bean
                        // in a transaction will only be enforced for Bean Managed
                        // beans, since the UserTx is 'sticky' and would be
                        // orphanded if the bean were removed.                 d451675
                        if (bmd.usesBeanManagedTx ||
                            bmd.usesBeanManagedAS) {
                            currentTx.ivRemoveBeanO = null;
                            throw new RemoveException("Cannot remove stateful session bean " +
                                                      "within a transaction.");
                        } else {
                            // For the CMT case, the bean will just be removed from
                            // the transaction and removed.. without calling the
                            // synchronization methods. The following will transition
                            // the bean to the correct state, and remove from the
                            // EJB Cache... deleting the bean.                  d451675

                            // d666718 - Delist the bean from the transaction.
                            // Otherwise, we will try to perform redundant processing
                            // on it later when the transaction actually completes.
                            try {
                                currentTx.delist(beanO);
                            } catch (TransactionRolledbackException ex) {
                                FFDCFilter.processException(ex, CLASS_NAME + ".postInvoke",
                                                            "4641", this);
                                if (isTraceOn && tc.isEventEnabled())
                                    Tr.event(tc, "Exception thrown from ContainerTx.delist()",
                                             new Object[] { beanO, ex });
                            }

                            simulateCommitBean(beanO, currentTx);
                        }
                    }

                } catch (CSITransactionRolledbackException ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".postInvoke",
                                                "2326", new Object[] { this, wrapper, Integer.valueOf(methodId), s });//123338
                    postInvokeRolledbackException(s, ex);
                } catch (Throwable t) {
                    FFDCFilter.processException(t, CLASS_NAME + ".postInvoke",
                                                "2366", this);
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "postInvoke: Exception in finally clause. An unexpected case", t);

                    if (s.exType == ExceptionType.NO_EXCEPTION) { //PQ94124
                        // An unexpected exception has occured in post processing,
                        // that the application is unaware of.
                        // call setUnCheckedEx and throw an exception.
                        s.setUncheckedException(t); //d117817
                        // s.setUncheckedException throws either runtime (for local I/F) or
                        // remote exception.
                    }
                } finally {
                    // Bean-specific contexts need to be present for beforeCompletion
                    // for JPA @PreUpdate, so we cannot pop the contexts prior to
                    // transaction completion, but ContainerTx.afterCompletion unpins
                    // beans from the current transaction, which would leave "stale"
                    // contexts in place for subsequent afterCompletion callbacks,
                    // so ContainerTx.afterCompletion pops contexts.  However, we
                    // need to pop contexts ourselves if there was no transaction, or
                    // the transaction wasn't committed (because this method didn't
                    // begin it or it was a sticky BMT), or the transaction was
                    // committed but ContainerTx.afterCompletion wasn't called due to
                    // some fatal error in transactions.                    RTC107108
                    if (currentTx == null) {
                        postInvokePopCallbackContexts(s);
                    } else if (!s.began || currentTx.ivPostInvokeContext != null) {
                        postInvokePopCallbackContexts(s);

                        // Reset the indicator so that sticky BMT doesn't pop the
                        // callback context in the middle of a bean method, but only
                        // do so if we set the method context above (don't do so if
                        // this is a nested EJB call from beforeCompletion). RTC115108
                        if (currentTx.ivPostInvokeContext == s) {
                            currentTx.ivPostInvokeContext = null;
                        }
                    }

                    // Pmi instrumentation for methods in beans
                    // Use pmiBean cached in the wrapper.                  d140003.33
                    // Insure this is done even if an exception is thrown.    d623908
                    EJBPMICollaborator pmiBean = wrapper.ivPmiBean; // d174057.2
                    if (pmiBean != null && s.pmiPreInvoked) {
                        pmiBean.methodPostInvoke(wrapper.beanId, methodInfo,
                                                 s.pmiCookie);

                        if (methodInfo.isHomeCreate()) {
                            // d647928 - SFSB and entity creation always goes through
                            // home wrappers.  The other bean types have code in
                            // EJSHome for create time.
                            if (bmd.isStatefulSessionBean() || bmd.isEntityBean()) {
                                pmiBean.finalTime(EJBPMICollaborator.CREATE_RT, s.pmiCookie);
                            }
                        } else if (methodInfo.isComponentRemove()) {
                            // d647928 - SFSB and entity can both be removed via the
                            // remove method on the wrapper.  That method is a no-op
                            // for the other bean types.
                            if (bmd.isStatefulSessionBean() || bmd.isEntityBean()) {
                                pmiBean.finalTime(EJBPMICollaborator.REMOVE_RT, s.pmiCookie);
                            }
                        }
                    }

                    // PQ74774 Begins
                    if (s.ivSecurityCollaborator != null) {
                        notifyPostInvoke(s.ivSecurityCollaborator, s, s.securityCookie);
                    }
                    // PQ74774 Ends

                    //92702 - BAAC collaborators must be called last of all
                    //the only example we know of is the JNS collaborator
                    if (ivBeforeActivationAfterCompletionCollaborators != null) {
                        // Call postInvoke on all of the BeforeActivationAfterCompletion
                        // Collaborators that were successfully preInvoked. Note that
                        // the order is the same as preInvoke.                    d228192
                        for (int i = 0; i < s.ivBeforeActivationAfterCompletionPreInvoked; i++) {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "postInokve : Invoking BeforeActivationAfterCompletionCollaborator.postInvoke method on: "
                                             + ivBeforeActivationAfterCompletionCollaborators[i].getClass().getName());

                            Object cookie = s.ivBeforeActivationAfterCompletionCookies == null ? null : s.ivBeforeActivationAfterCompletionCookies[i]; // F61004.3
                            notifyPostInvoke(ivBeforeActivationAfterCompletionCollaborators[i], s, cookie);
                        }
                    }

                    // Inform EJB method callback that method has completed. This must be
                    // called after the transaction has completed.
                    if (s.ivEJBMethodCallback != null) {
                        // hmmmm, what if exception occurs?  Same problem as if beforeActivationAfterCompletion
                        // collaborators throw an exception prior to this change.
                        s.invocationCallbackPostInvoke(); // d194342.1.1
                    }

                    // drop the reference on the wrapper
                    // Not required if the wrapper has already been removed
                    if (s.unpinOnPostInvoke) //d140003.19
                        wrapperManager.postInvoke(wrapper);

                    // If preInvoke began the transaction, then the ContainerTx
                    // is no longer in use, and so may be cleared, so just call
                    // releaseResources() to clear all of the fields... making garbage
                    // collection eaiser, and allows things to be garbage collected
                    // even if some other object (like PM) holds a reference to the
                    // ContainerTx for awhile.                     d154342.10 d156606
                    // Note that 'began' for the ContainerTx is not valid at this
                    // point, since the postInvoke call on it restored the 'began'
                    // setting to the previous method state, so use the 'began' flag
                    // in EJSDeployedSupport (valid for current method).      d156688
                    if (s.currentTx != null && s.began) {
                        s.currentTx.releaseResources(); // d215317
                        s.currentTx = null;
                    }

                    // 112678.6 d115602-1
                    if (methodInfo.setClassLoader) {
                        EJBThreadData.svThreadContextAccessor.popContextClassLoaderForUnprivileged(s.oldClassLoader);
                    } else {
                        threadData.popORBWrapperClassLoader();
                    }

                    //92682
                    threadData.popMethodContext(); // d646139.1, RTC102449

                    // d161864 Begins
                    if (isTraceOn) {
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "EJBpostInvoke(" + methodId + ":" +
                                        methodInfo.getMethodName() + ")" +
                                        ((s.ivException != null) ? ("**** throws " + s.ivException) : ""));
                        if (TEEJBInvocationInfo.isTraceEnabled()) {
                            if (s.ivException == null) {
                                TEEJBInvocationInfo.tracePostInvokeEnds(s, wrapper);
                            } else {
                                TEEJBInvocationInfo.tracePostInvokeException(s, wrapper,
                                                                             s.ivException);
                            }
                        }
                    }
                    // d161864 Ends
                } //end 92702
            }
        }
    } // postInvoke

    /**
     * Pop contexts from the thread for the callback bean.
     */
    void postInvokePopCallbackContexts(EJSDeployedSupport s) {
        // Pop the callback bean if Activator.preInvokeActivateBean was called
        // successfully (excluding homes).                                 d694142
        if (s.ivPopCallbackBeanORequired) {
            s.ivThreadData.popCallbackBeanO();

            // If necessary, return the bean to the pool.  It will be null for
            // BeanNotReentrantException, which will prevent an in-use bean from
            // being returned to the pool.                                RTC107108
            BeanO beanO = s.beanO;
            if (beanO != null) {
                BeanMetaData bmd = beanO.home.beanMetaData;
                if (bmd.type == InternalConstants.TYPE_STATELESS_SESSION ||
                    bmd.type == InternalConstants.TYPE_MESSAGE_DRIVEN) {
                    try {
                        beanO.returnToPool();
                    } catch (RemoteException ex) {
                        FFDCFilter.processException(ex, CLASS_NAME + ".postInvoke", "359", this);
                    }
                }
            }
        }

        s.ivThreadData.ivComponentMetaDataContext.endContext(); // d646139.1
    }

    /**
     * Call {@link EJBRequestCollaborator#postInvoke}. Handles generics casting
     * and CSIException handling.
     */
    private static <T> void notifyPostInvoke(EJBRequestCollaborator<T> collaborator, EJBRequestData request, Object preInvokeData) throws CSIException {
        try {
            @SuppressWarnings("unchecked")
            T uncheckedCookie = (T) preInvokeData;
            collaborator.postInvoke(request, uncheckedCookie);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (CSIException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CSIException("", ex);
        }
    }

    // F743-1751 - Factored out from postInvoke unchanged.
    /**
     * Called by postInvoke methods when CSIRolledbackException is caught from
     * UOWControl.postInvoke.
     *
     * @param ex the exception
     */
    private void postInvokeRolledbackException(EJSDeployedSupport s, CSITransactionRolledbackException ex) throws RemoteException {
        //---------------------------------------------------------------
        // Check to see if the application requested that this transaction
        // be rolled back. If so, we can report no exception so suppress
        // the rollback exception here.
        // Only applies if the beginner bean called setRollbackOnly
        // during the business method.                            d186801
        // Or, if the beginner threw an Application Exception     d199884
        // Except, don't supress for ejbTimeout.                LI2281.07
        //---------------------------------------------------------------

        //d140003.19 d186801
        if ((!s.ivBeginnerSetRollbackOnly &&
             (!s.began ||
              s.exType != ExceptionType.CHECKED_EXCEPTION))
            ||
            s.methodInfo.ivInterface == MethodInterface.TIMED_OBJECT) { //90514;;
                                                                                                                                                                           //d140003.19
            final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2
            if (isTraceOn && tc.isEventEnabled())
                Tr.event(tc, "postInvoke: transaction rollback in finally",
                         ex); //93496

            // If an exception occurred during beforeCompletion, then override
            // the cause set by TranStrategy since that one will contain
            // superfluous RuntimeException wrappers.          d109641.1 d616849.2
            if (s.currentTx != null &&
                s.currentTx.ivPostProcessingException != null) {
                // If there was an exception during post processing
                // (commit), then set it as an unchecked exception.
                // This will not override any exception that occurred
                // during the bean method.        PQ90221 d219086 d219086.1
                s.getExceptionMappingStrategy().setUncheckedException(s, s.currentTx.ivPostProcessingException);
                ex.detail = s.getRootCause();
            } else if (ex.detail == null) {
                ex.detail = s.getRootCause();//d140003.19
            }
            //d171654 make sure CSITRBException root cause not overwritten
            else {
                //d174358.1---------->
                Throwable exceptionToUse = s.getRootCause();//d140003.19

                if (exceptionToUse == null) {
                    s.rootEx = s.getExceptionMappingStrategy().findRootCause(ex.detail);
                } else {
                    // we have a checked or unchecked so use that per spec
                    ex.detail = exceptionToUse;
                }
                //<----------d174358.1
            } //<--------d171654

            // Perform standard exception mapping using the exception
            // mapping utilities (just like EJSDeployedSupport). This
            // will wrap the CSI exception in a
            // TransactionRolledBackException, which is the exception
            // required by the EJB spec, then that will be mapped to
            // a CORBA TRANSACTION_ROLLEDBACK exception, as required
            // by the ORB.  The ORB should convert the
            // TRANSACTION_ROLLEDBACK to a TransactionRolledBackException
            // on the client.  Note that the root cause will be lost,
            // but will be embeded as part of the message text.  d109641.1
            Exception crex = s.mapCSITransactionRolledBackException(ex); //d140003.19             // d113344

            // The above result should always be a TRANSACTION_ROLLEDBACK,
            // but this is coded just in case mapException is changed in
            // the future.                                       d109641.1
            if (crex instanceof RemoteException)
                throw (RemoteException) crex;
            else if (crex instanceof RuntimeException)
                throw (RuntimeException) crex;
        }
    }

    /**
     * Simulate the commit of a transaction for a bean.
     *
     * @param beanO the bean
     * @param containerTx the transaction
     */
    // F61004.1
    private void simulateCommitBean(BeanO beanO, ContainerTx containerTx) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "simulateCommitBean");

        // React to exceptions the same as afterCompletion, insure
        // both commit and commitBean are called.     F743-22462.CR

        try {
            beanO.commit(containerTx);
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".simulateCommitBean",
                                        "5300", new Object[] { this, beanO });
            if (isTraceOn && tc.isEventEnabled())
                Tr.event(tc, "Exception thrown from BeanO.commit()",
                         new Object[] { beanO, ex });
        }

        try {
            activator.commitBean(containerTx, beanO);
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".simulateCommitBean",
                                        "5313", new Object[] { this, beanO });
            if (isTraceOn && tc.isEventEnabled())
                Tr.event(tc, "Exception thrown from commitBean()",
                         new Object[] { beanO, ex });
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "simulateCommitBean");
    }

    public void EjbPostInvokeForStatelessCreate(EJSWrapperBase wrapper,
                                                int methodId, // f111627
                                                EJSDeployedSupport s) throws RemoteException {
        EJBMethodInfoImpl methodInfo = s.methodInfo; // d114406 d154342.4 d173022
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn) {
            if (TEEJBInvocationInfo.isTraceEnabled())
                TEEJBInvocationInfo.tracePostInvokeBegins(s, wrapper); // d161864

            if (tcClntInfo.isDebugEnabled())
                Tr.debug(tcClntInfo, "postInvoke(" + methodInfo.getMethodName() + ")");

            if (tc.isEntryEnabled())
                Tr.entry(tc, "EjbPostInvokeForStatelessCreate(" + methodId +
                             ":" + methodInfo.getMethodName() + ")"); // d161864
        }

        // UOWManager.runUnderUOW may have left cached currentTx invalid; reset
        if (s.resetCurrentTx) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "s.currentTx may be invalid; reset");
            s.currentTx = getCurrentContainerTx();
        }

        EJBThreadData threadData = s.ivThreadData;

        // do pop first to ensure that pop occurs even if exception occurs
        // after this point.
        if (!methodInfo.setClassLoader) {
            threadData.popORBWrapperClassLoader();
        }

        // d173022.7 Begins
        // Deleted a bunch of code that is no longer needed as a result of the changes
        // made for d173022.7 in the preinvokeForStatelessSessionCreate method.

        // PQ74774 Begins
        if (s.ivSecurityCollaborator != null) {
            notifyPostInvoke(s.ivSecurityCollaborator, s, s.securityCookie);
        }
        // PQ74774 Ends
        // d173022.7 Ends

        // drop the reference on the wrapper
        // Not required if the wrapper has already been removed
        if (s.unpinOnPostInvoke) // d140003.19
            wrapperManager.postInvoke(wrapper);

        threadData.popMethodContext(); // d127257, d646139.1, RTC102449

        // d161864 Begins
        if (isTraceOn) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "EjbPostInvokeForStatelessCreate(" + methodId +
                            ":" + methodInfo.getMethodName() + ")");

            if (TEEJBInvocationInfo.isTraceEnabled()) {
                if (s.ivException == null) {
                    TEEJBInvocationInfo.tracePostInvokeEnds(s, wrapper);
                } else {
                    TEEJBInvocationInfo.tracePostInvokeException(s, wrapper,
                                                                 s.ivException);
                }
            }
        }
        // d161864 Ends
    } // EjbPostInvokeForStatelessCreate

    // F743-1751
    /**
     * A subset of postInvoke specifically for the lifecycle interceptors of a
     * Singleton Session bean. This method must be paired with
     * preInvokeForLifecycleInterceptors.
     */
    public void postInvokeForLifecycleInterceptors(LifecycleInterceptorWrapper wrapper,
                                                   int methodId,
                                                   EJSDeployedSupport s) throws RemoteException {
        // NOTE: The implementation of this method is structured after postInvoke.
        // For a detailed explanation of the operations here, see the comments in
        // that method.

        EJBMethodInfoImpl methodInfo = s.methodInfo;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn) {
            if (TEEJBInvocationInfo.isTraceEnabled())
                TEEJBInvocationInfo.tracePostInvokeBegins(s, wrapper);

            if (tcClntInfo.isDebugEnabled())
                Tr.debug(tcClntInfo, "postInvoke(" + methodInfo.getMethodName() + ")");

            if (tc.isEntryEnabled())
                Tr.entry(tc, "postInvokeForLifecycleInterceptors(" + methodId +
                             ":" + methodInfo.getMethodName() + ")");
        }

        // UOWManager.runUnderUOW may have left cached currentTx invalid; reset
        if (s.resetCurrentTx) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "s.currentTx may be invalid; reset");
            s.currentTx = getCurrentContainerTx();
        }

        s.currentTx.postInvoke(s);

        try {
            boolean isRollbackOnly = uowCtrl.getRollbackOnly(); //d636725
            try {
                if (s.began && s.currentTx.ivPostInvokeContext == null) {
                    s.currentTx.ivPostInvokeContext = s;
                }

                uowCtrl.postInvoke(wrapper.beanId, s.uowCookie, s.exType, methodInfo);
            } catch (CSITransactionRolledbackException ex) {
                // Per preInvokeForLifecycleInterceptors, all exceptions thrown from
                // lifecycle interceptors must not be application exceptions.  This
                // ensures postInvokeRolledbackException will propagate this exception
                // (unless setRollbackOnly was called).  The exception is mapped to
                // prevent application code from seeing internal CSIExceptions.
                postInvokeRolledbackException(s, ex);
            }

            //d636725 start
            // If setRollbackOnly was called in a PostConstruct method of a
            // singleton bean, then we need to throw an
            // EJBTransactionRolledBackException here which will be wrapped
            // in a NoSuchEJBException.
            // Our interpretation of the spec requires that we throw this
            // exception here (as opposed to within the setRollbackOnly
            // method - which is an allowed operation) so that the the
            // NoSuchEJBException will be thrown to the client during
            // postInvoke. This behavior is specific to Singleton beans
            // as Stateful and Stateless beans are not allowed to call
            // setRollbackOnly inside PostConstruct methods - they would
            // receive an IllegalStateException.
            if (methodId == LifecycleInterceptorWrapper.MID_POST_CONSTRUCT &&
                isRollbackOnly) {

                String msg = "setRollbackOnly called from within a singleton post construct method.";
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, msg);
                throw new EJBTransactionRolledbackException(msg);
            }
            //d636725 end
        } finally {
            if (!s.began || s.currentTx.ivPostInvokeContext != null) {
                postInvokePopCallbackContexts(s);
            }

            if (s.ivSecurityCollaborator != null) {
                notifyPostInvoke(s.ivSecurityCollaborator, s, s.securityCookie);
            }

            if (s.currentTx != null && s.began) {
                s.currentTx.releaseResources();
                s.currentTx = null;
            }

            EJBThreadData.svThreadContextAccessor.popContextClassLoaderForUnprivileged(s.oldClassLoader);

            s.ivThreadData.popMethodContext(); // d646139.1, RTC102449

            if (isTraceOn) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "postInvokeForLifecycleInterceptors(" + methodId + ":" +
                                methodInfo.getMethodName() + ")" +
                                ((s.ivException != null) ? ("**** throws " + s.ivException) : ""));
            }
        }
    }

    /**
     * Get <code>EJSWrapper</code> instance associated with
     * with the same <code>BeanId</code> as the given
     * <code>BeanO</code>. <p>
     */
    //   public EJSWrapperCommon getWrapper(BeanO b)                     // f111627
    //         throws CSIException,
    //                RemoteException
    //   {
    //      return getWrapper(b.getId());
    //   }                             // getWrapper

    /**
     * Get <code>EJSWrapper</code> instance associated with
     * with the given <code>BeanId</code><p>
     */

    public EJSWrapperCommon getWrapper(BeanId id) throws CSIException, RemoteException {
        return wrapperManager.getWrapper(id);
    } // getWrapper

    /**
     * Get an <code>EJSWrapper</code> instance that corresponds to the
     * given bean id. <p>
     *
     * This method serves as a factory for <code>EJSWrappers</code> to the
     * wrapper manager which interacts with the ORB to supply wrappers for
     * for the ORB to invoke methods on. <p>
     *
     * @param beanId the <code>BeanId</code> to return a wrapper for <p>
     *
     * @return an <code>EJSWrapper</code> corresponding to the given
     *         <code>BeanId</code> <p>
     *
     * @exception InvalidBeanIdException thrown if the given
     *                bean id does not correspond to a bean installed in this
     *                container <p>
     *
     */
    public EJSWrapperCommon createWrapper(BeanId beanId) throws RemoteException, CSIException {
        HomeInternal home = beanId.getHome();

        if (home == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Unknown beanId home name", beanId);
            throw new InvalidBeanIdException();
        }

        EJSWrapperCommon result = null; // f111627
        try {
            result = home.internalCreateWrapper(beanId);
        } catch (javax.ejb.CreateException ex) {
            // Note, this can never happen.
            FFDCFilter.processException(ex, CLASS_NAME + ".createWrapper", "2651", this);
        }

        return result;
    } // getWrapper

    // f11627.1 Begin
    /**
     * Return the default EJB container in application server.
     */
    public static EJSContainer getContainer(String containerName) {
        return getDefaultContainer();
    }

    /**
     * Return the default EJB container in application server.
     */
    public static EJSContainer getDefaultContainer() {
        return defaultContainer;
    }

    // f11627.1 End

    /**
     * Gets the EJB data associated with the currently running thread.
     */
    public static EJBThreadData getThreadData() {
        return svThreadData.get();
    }

    /**
     * Gets the bean that the container is currently processing via a business
     * method or lifecycle callback.
     */
    public static BeanO getCallbackBeanO() {
        return svThreadData.get().getCallbackBeanO();
    }

    /**
     * Return the class loader passed in as part of the config data
     * by the server
     */
    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Return the orb utils object passed in as part of the config data
     * by the server
     */
    public OrbUtils getOrbUtils() {
        return orbUtils;
    }

    /**
     * Return the ContainerExtensionFactory object passed in as part of the
     * config data by the server
     */
    public ContainerExtensionFactory getContainerExtensionFactory() {
        return containerExtFactory;
    }

    //92682
    // Return EJSDeployedSupport object passed during preinvoke
    // of a EJB business method.
    //
    // WARNING: Caller must handle possiblity of null being returned.
    // A null reference is returned if preinvoke for a business method had not occured.
    // This can happen in some scenarios.  For example, a servlet drives commit
    // on user transaction and ejbStore from bean calls isCallerInRole.
    // For that scenario, container may end up calling this method and
    // discover a null reference is returned.  Another scenario would
    // be the timer going off for a EJB timed object.  That could result
    // in this method being called and a null reference being returned.
    // Bottom line is caller must check for null being returned and handle
    // appropriatedly.
    //
    public static EJSDeployedSupport getMethodContext() {
        return getThreadData().getMethodContext(); // d646139.1
    }

    /**
     * Get the ClassLoader for the bean specified by beanName
     */
    public static ClassLoader getClassLoader(J2EEName beanName) {
        ClassLoader cl = null;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getClassLoader(" + beanName + ")");

        HomeInternal hi = homeOfHomes.getHome(beanName);
        if (hi != null) {
            cl = hi.getClassLoader();
        } else {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.exit(tc, "getClassLoader: Home not found!");
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getClassLoader: " + cl);

        return cl;
    }

    /**
     * Force the persistent state of all entity beans modified in the
     * current transaction to be written to the persistent store.
     */
    public void flush() throws RemoteException {
        ContainerTx containerTx = getCurrentTx(false); //d171654
        containerTx.flush();
    } // flush

    /**
     * Dump the internal state of this container instance to the
     * trace stream. <p>
     */
    public void dump() {
        if (!tc.isDumpEnabled() || dumped) {
            return;
        }

        try {
            introspect(new TrDumpWriter(tc), true);
        } finally {
            dumped = true;
        }
    } // dump

    /**
     * Reset dumped state of this container instance. <p>
     */
    public void resetDump() {
        dumped = false;
    } // resetDump

    /**
     * This method is driven when dispatch is called upon a server request.
     * It should return a 'cookie' Object which uniquely identifies this preinvoke.
     * This 'cookie' should be passed as a paramater when the corresponding
     * postinvoke is called.
     *
     * @param object the IDL Servant or RMI Tie
     * @param operation the operation being invoked
     * @return Object a 'cookie' which uniquely identifies this preinvoke()
     */
    @Override
    public Object preInvokeORBDispatch(Object object, String operation) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled()) {
            // toString of 'object' can be quite large and can really bloat the
            // trace.... so just trace the class name; usually the tie name or
            // class loader name. The IOR or classpath is not traced.       d133384
            String objStr = null;
            if (object != null)
                objStr = object.getClass().getName();
            Tr.debug(tc, "preInvokeORBDispatch(" + objStr + ", " + operation + ")");
        }

        EJBThreadData threadData = null;

        if (object instanceof javax.rmi.CORBA.Tie) {
            object = ((javax.rmi.CORBA.Tie) object).getTarget();
            if (object instanceof EJSWrapperBase) {
                final EJSWrapperBase wrapper = (EJSWrapperBase) object;
                BeanMetaData bmd = wrapper.bmd;
                // null if EJBFactory
                if (bmd != null) {
                    threadData = svThreadData.get();

                    // d153263
                    // push the old class loader to the ORB dispatch stack so that the wrapper classloader
                    // can be popped off the "stack" before the marshalling the return object in the stub.
                    // See any generated stub with input param and return to see the problem described in defect
                    //
                    // There are 2 paths need to be taken care of:
                    // 1. in the normal case, the wrapper classloader is popped off when from container.postinvoke().
                    // 2. after the object resolver preInvoke is performed and exception takes place before
                    //    wrapper.postInvoke() has a chance to clean up.
                    threadData.pushClassLoader(bmd);
                }
            }
        }

        // d646139.2 - For performance, return the EJBThreadData as the cookie to
        // avoid needing to look it up again in postInvokeORBDispatch.
        // d717291 - Return null to indicate a non-EJB request.
        return threadData; // d646139.2
    }

    /**
     * This method is driven just before the server side interceptors are
     * driven for a server response.
     *
     * @param object a 'cookie' object which uniquley identified the preceeding
     *            preinvoke(Object, String)
     */
    @Override
    public void postInvokeORBDispatch(Object object) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "postInvokeORBDispatch: " + (object != null));

        // d717291 - Null object indicates a non-EJB request.
        if (object != null) {
            ((EJBThreadData) object).popClassLoader();
        }
    }

    // p118811 - start of change.

    /**
     * Returns a reference to the EJBHome associated with a specified J2EEName.
     *
     * @param name is the J2EEName of the EJB's home object.
     *
     * @exception ContainerEJBException if any Throwable occurs that
     *                prevented this method from return EJBHome. Use the getCause
     *                method to recover the Throwable that occured.
     */
    public EJBHome getEJBHome(J2EEName name) throws ContainerEJBException {
        try {
            EJSWrapperCommon wrapperCommon = getHomeWrapperCommon(name);
            EJSWrapper wrapper = wrapperCommon.getRemoteWrapper();
            return (EJBHome) PortableRemoteObject.toStub(wrapper); // PM35068
        } catch (RemoteException re) {
            FFDCFilter.processException(re, CLASS_NAME + ".getEJBHome", "2955", this);
            if (re.detail == null) {
                throw new ContainerEJBException("Could not get EJBHome", re);
            } else {
                throw new ContainerEJBException("Could not get EJBHome", re.detail);
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + ".getEJBHome", "2967", this);
            throw new ContainerEJBException("Could not get EJBHome", t);
        }
    }

    /**
     * Returns a reference to the EJBLocalHome associated with a specified J2EEName.
     *
     * @param name is the J2EEName of the EJB's local home object.
     *
     * @return reference to EJBLocalHome for J2EEName specified by name parameter.
     *
     * @exception ContainerEJBException if any Throwable occurs that
     *                prevented this method from return EJBLocalHome. Use the getCause
     *                method to recover the Throwable that occured.
     */
    public EJBLocalHome getEJBLocalHome(J2EEName name) throws ContainerEJBException {
        try {
            EJSWrapperCommon wrapperCommon = getHomeWrapperCommon(name);
            return (EJBLocalHome) wrapperCommon.getLocalObject(); // d188404

        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + ".getEJBLocalHome", "2993", this);
            throw new ContainerEJBException("Could not get EJBLocalHome", t);
        }
    }

    /**
     * Returns an aggregate reference for all of the bean's business local
     * interfaces, including the no-interface view (if present). <p>
     *
     * Only session beans support an aggregate local reference. <p>
     *
     * Caution: When using the returned aggregate reference, the SessionContext
     * method getInvokedBusinessInterface() will not function properly
     * for methods that are common to multiple local interfaces. An
     * IllegalStateException will be thrown. <p>
     *
     * @param beanName is the unique J2EEName of the bean.
     * @param context the context for creating the object, or null
     * @return an aggregate reference for all of the beans business local
     *         interfaces, including the no-interface view.
     * @throws CreateException if an application-level failure occurs creating
     *             an instance of the bean.
     * @throws EJBNotFoundException if the specified bean cannot be found; the
     *             application may have been stopped or has not been installed.
     * @throws EJBException if the bean is not a session bean, has no business
     *             local interfaces or a failure occurs attempting to generate
     *             the aggregate wrapper class or create an instance of it.
     */
    // F743-34304
    public Object createAggregateLocalReference(J2EEName beanName, ManagedObjectContext context) throws EJBNotFoundException, CreateException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createAggregateLocalReference : " + beanName);

        EJSHome home = getInstalledHome(beanName);

        if (!home.beanMetaData.isSessionBean()) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createAggregateLocalReference : not a session bean!");
            throw new EJBException("The " + beanName.getComponent() + " bean in the " +
                                   beanName.getModule() + " module of the " + beanName.getApplication() +
                                   " application has no business local interfaces.");
        }

        Object reference = null;

        // create the aggregate reference
        reference = home.createAggregateLocalReference(context);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createAggregateLocalReference : " + Util.identity(reference));

        return reference;
    }

    /**
     * Returns the J2EEName given the home object for an EJB.
     *
     * @param wrapper is either a stub or a wrapper for the
     *            home object. For the case of a stub, both the stub
     *            and the wrapper must be colocated (located in same process).
     */
    public J2EEName getJ2EEName(EJSWrapperBase wrapper) {
        return wrapper.beanId.getJ2EEName();
    }

    // p118811 - end of change.

    /**
     * The interval between removing unused bean instances.
     *
     * @param interval the interval in milliseconds.
     */
    public void setInactivePoolCleanupInterval(long interval) {
        poolManager.setDrainInterval(interval);
    }

    /**
     * Set the interval between passivating unused stateful session bean instances
     * when the size is exceeded.
     *
     * @param interval the interval in milliseconds.
     */
    public void setInactiveCacheCleanupInterval(long interval) {
        activator.setCacheSweepInterval(interval);
        wrapperManager.setWrapperCacheSweepInterval(3 * interval);
    }

    /**
     * Set the cache size used by this container.
     *
     * @param size the preferred capacity of the cache
     */
    public void setPreferredCacheSize(long size) {
        int intSize = (int) size;
        activator.setCachePreferredMaxSize(intSize);
        wrapperManager.setWrapperCacheSize(2 * intSize);
    }

    /**
     * Called by the runtime environment to prepare the EJB container for
     * running EJB timers.
     */
    public void setupTimers() {
        // The size of the pool should be related to the max number of alarm
        // threads for the scheduler, but there is not currently a public
        // method to obtain that information.  At max, there would only
        // ever be one wrapper per alarm thread.  For now, a reasonable
        // min and max value will be hard coded.                           d174057
        if (ivTimedObjectPool == null) {
            ivTimedObjectPool = poolManager.createThreadSafePool(5, 50);
        }
    }

    /**
     * Called by the runtime environment to set the behavior for allowing
     * access to the Timer APIs. The default behavior is to allow access.
     * Restricting access will provide behavior that is compliant with
     * EJB 3.1 and earlier.
     */
    public void setAllowTimerAccessOutsideBean(boolean allowAccess) {
        this.allowTimerAccessOutsideBean = allowAccess;
    }

    /**
     * Called by the runtime environment to set the behavior for respecting
     * transaction attributes for stateful lifecycle callback methods. The
     * default behavior is to respect the transaction attributes. Disabling this
     * will provide the behavior that is compliant with EJB 3.1 and earlier.
     */
    public void setTransactionalStatefulLifecycleMethods(boolean transactional) {
        this.transactionalStatefulLifecycleMethods = transactional;
    }

    public boolean isTransactionStatefulLifecycleMethods() {
        return transactionalStatefulLifecycleMethods;
    }

    /**
     * Called by the runtime environment to set the behavior for allowing
     * no-method inteface MDBs. The default behavior is to enable this behavior.
     * Disabling will provide behavior that is compliant with EJB 3.1 and earlier.
     */
    public void setNoMethodInterfaceMDBEnabled(boolean enable) {
        this.noMethodInterfaceMDBEnabled = enable;
    }

    public boolean isNoMethodInterfaceMDBEnabled() {
        return noMethodInterfaceMDBEnabled;
    }

    /*
     * Method added to remove check in method containerTxCompleted
     * to improve number of instructions when optA Entity beans are not
     * in the server. <p>
     *
     * BeanMetaData will call this method if any optA beans are installed. <p>
     *
     * Also called when a 'hard' limit has been specified for Stateless
     * Session beans.
     *
     * Method is synchronized to avoid creating multiple LockManager objects,
     * since bean install/start is no longer synchronized. d334557 PK20648
     */
    public synchronized void initOptACleanUpLockManager() {
        if (lockManager == null)
            lockManager = new LockManager();
    }

    // 199625 Begins
    /**
     * Returns the primary key class associated to the input j2eeName or
     * null if j2eeName or primary key is not specified.
     *
     * @param j2eeName unique identifier for EJB
     * @return Prmiary key class.
     */
    public Class<?> getEJBPrimaryKeyClass(J2EEName j2eeName) {
        Class<?> rtnPKeyClass = null;
        BeanMetaData bmd = internalBeanMetaDataStore.get(j2eeName);
        if (bmd != null) {
            rtnPKeyClass = bmd.pKeyClass;
        }
        return rtnPKeyClass;
    }

    /**
     * Get whether SFSB failover is enabled at container level.
     *
     * @return true if enabled at container level.
     */
    final public boolean isEnableSFSBFailover() {
        return ivSFSBFailoverEnabled;
    }

    /**
     * Returns the SfFailoverCache used by this EJB container.
     * to hold replicated SFSB data when failover is enabled.
     * A null reference is returned if SFSB failover is not enabled.
     */
    public SfFailoverCache getSfFailoverCache() {
        return ivSfFailoverCache;
    }

    // 199625 Ends

    /**
     * Invoke around invoke or around timer interceptors and then invoke the
     * business or timer method. <p>
     *
     * Note, this method is used for both around invoke and around timeout on
     * EJB 3.x session beans (Stateless, Stateful, or Singleton), but is only
     * used for around timeout on message driven beans. <p>
     *
     * @param s is the same EJSDeployedSupport that was passed to the preInvoke method
     *            of this class.
     * @param timer the timer, or <tt>null</tt> if this is not a timer method
     *
     * @return Object returned by business method invocation (which may be null
     *         if business method has void for return value).
     */
    public Object invoke(EJSDeployedSupport s, Timer timer) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "EJBinvoke(" + s.methodId + ":" +
                         s.methodInfo.getMethodName() + ")");

        // Get BeanO stored in EJSDeployedSupport during preInvoke processing and
        // use it to get a InvocationContext needed for invoking the AroundInvoke
        // or AroundTimeout interceptors (works for Session and MDB).
        ManagedBeanOBase beanO = (ManagedBeanOBase) s.beanO;
        InvocationContextImpl<?> invCtx = beanO.getInvocationContext();
        invCtx.setTimer(timer);

        // Invoke the around invoke interceptors, which will invoke the business method when
        // the last interceptor calls proceed on InvocationContext.
        EJBMethodInfoImpl methodInfo = s.methodInfo;
        return invCtx.doAroundInvoke(methodInfo.ivAroundInterceptors,
                                     methodInfo.ivMethod,
                                     s.ivEJBMethodArguments,
                                     s);
    }

    /**
     * Invoke around invoke interceptors and business method of "managed bean" types. <p>
     *
     * This method is intended for use when invoking a WebService Endpoint
     * interface method with around invoke interceptors. <p>
     *
     * This method differs from the standard invoke method in that the EJB method
     * parameters are passed, instead of being set in EJSDeployedSupport. There
     * are two reasons for this:
     *
     * 1) The parameters are not known when preInvoke is called, and therefore
     * cannot be set in EJSDeployedSupport at that time.
     * 2) Setting the parameters in EJSDeployedSupport makes them available for
     * use by JACC security if isCallerInRole is called, and the method
     * parameters are NOT supposed to be available for WebService Endpoints.
     *
     * @param s is the same EJSDeployedSupport that was passed to the preInvoke
     *            method of this class.
     * @param parameters is the array of arguments to be passed to business method.
     *
     * @return Object returned by business method invocation (which may be null
     *         if business method has void for return value).
     **/
    // d507967
    public Object invoke(EJSDeployedSupport s,
                         Object[] parameters) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "WS-EJBinvoke(" + s.methodId + ":" +
                         s.methodInfo.getMethodName() + ")");

        // Get SessionBeanO stored in EJSDeployedSupport during preInvoke
        // processing and use it to get a InvocationContext needed for
        // invoking the AroundInvoke interceptors.
        SessionBeanO beanO = (SessionBeanO) s.beanO;
        InvocationContextImpl<?> invCtx = beanO.getInvocationContext();

        // Invoke the around invoke interceptors, which will invoke the
        // business method when the last interceptor calls proceed on
        // InvocationContext.
        EJBMethodInfoImpl methodInfo = s.methodInfo;
        return invCtx.doAroundInvoke(methodInfo.ivAroundInterceptors, // F743-17763.1
                                     methodInfo.ivMethod,
                                     parameters,
                                     s);
    }

    /**
     * When last around invoke interceptor calls the proceed method of the
     * javax.interceptor.InvocationContext interface, the InvocationContext
     * implementation calls this method to invoke the business method.
     * If one or more of around invoke interceptors had modified any of the
     * method arguments and a JACC provider wants to be notified if any of the
     * the parameters were changed by any of the around invoke interceptors,
     * then the SecurityCollaborator is called to notified it of the change
     * to one or more of the method parameters.
     *
     * @param s is the EJSDeployedSupport used to do preInvoke for the business method.
     * @param businessMethod is the java reflection Method object for invoking the business method.
     * @param bean is the EJB instance being invoked.
     * @param methodParameters is the array of parameters to be passed to business method.
     * @param parametersModified must be true if and only if one or more around invoke interceptors
     *            had called javax.interceptors.InvocationContext.setParameters to modify one or more
     *            of the method parameters.
     *
     * @return the return value from the business method invoked or null.
     *
     * @throws Exception is thrown if either security collaborator or business
     *             business method throws an exception.
     */
    // LIDB3294-41
    public Object invokeProceed(EJSDeployedSupport s, Method businessMethod, Object bean, Object[] methodParameters, boolean parametersModified) throws Exception {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "EJBinvokeProceed(" + s.methodId + ":" +
                         s.methodInfo.getMethodName() + ")");

        if (parametersModified) {
            s.ivEJBMethodArguments = methodParameters;

            if (doesJaccNeedsEJBArguments(null) && s.ivSecurityCollaborator != null) {
                // JACC parameter checking is enabled and SecurityCollaborator preInvoke was
                // called and atleast one of the business method parameter was modified by an
                // around invoke interceptor. Inform SecurityCollaborator of this change so
                // that it can notify JACC provider of the parameter change.
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "invoking EJBSecurityCollaborator.argumentsUpdated");
                notifySecurityCollaboratorArgumentsUpdated(s.ivSecurityCollaborator, s);
            }
        }

        // If no exeception is thrown by security, then it is okay to invoke the business method.
        Object returnValue = businessMethod.invoke(bean, methodParameters);
        if (isTraceOn && tc.isEntryEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("EJBinvokeProceed(").append(s.methodId).append(":");
            sb.append(s.methodInfo.getMethodName()).append(") returning ");
            if (returnValue == null) {
                sb.append("null");
            } else {
                sb.append(returnValue.getClass().getName());
                sb.append("@").append(returnValue.hashCode());
            }

            Tr.exit(tc, sb.toString());
        }
        return returnValue;
    }

    private static <T> void notifySecurityCollaboratorArgumentsUpdated(EJBSecurityCollaborator<T> collaborator,
                                                                       EJSDeployedSupport s) throws Exception {
        @SuppressWarnings("unchecked")
        T preInvokeData = (T) s.securityCookie;
        collaborator.argumentsUpdated(s, preInvokeData);
    }

    /**
     * Returns the binding context for the currently active extended-scoped
     * persistence context for the thread of execution. Null will be returned
     * if an extended-scoped persistence context is not currently active. <p>
     *
     * @return binding context for currently active extended-scoped
     *         persistence context.
     */
    // d515803
    public Object getExPcBindingContext() {
        EJSDeployedSupport s = getMethodContext(); // d646139.1

        if (s != null) {
            return s.getExPcBindingContext();
        }
        return null;
    }

    /**
     * F743-609
     *
     * The <code>scheduleAsynchMethod<code> code is called by the EJB wrapper class when
     * it needs to dispatch an asynchronous method. The method is wrapped into a Work
     * object and sent to a work manager for dispatch on another thread. The work object
     * also provides the wrapper function of calling ejb container preInvoke and postInvoke
     * around the actual bean method dispatch. <p>
     *
     * @param wrapper - the wrapper that originally intercepted the asynchronous method call
     * @param methodId - the internal container methodId of the asynchronous method
     * @param args - the method's arguments
     *
     * @return Future - depending on the method signature of the asynch method, results
     *         retuned may be a Future object, or null if the asynch method was
     *         a fireAndForget method (ie. return type of the interface method
     *         was void).
     *
     * @exception EJBException - if any exception occurs trying to schedule the asynch method
     *                for execution, we wrap it in an EJBException that flows
     *                through the wrapper to the client.
     * @exception RemoteException - if the bean implements rmi remote business interface, wrap
     *                the exception in a RemoteException instead of EJBException
     */
    public Future<?> scheduleAsynchMethod(EJSWrapperBase wrapper, int methodId, Object[] args) throws RemoteException {
        return ivEJBRuntime.scheduleAsync(wrapper, wrapper.methodInfos[methodId], methodId, args); // F743-13022
    }

    // begin 619922
    @Override
    public String[] introspectSelf() {
        // return nothing
        return new String[] {};
    }

    /**
     * Returns void and instead writes directly to IncidentStream to avoid large string buffers.<p>
     *
     * Used by the EJBContainerDiagnosticModule instead of toString().
     */
    public void ffdcDump(IncidentStream is) {
        introspect(new IncidentStreamWriter(is), false);
    }

    // end 619922

    /**
     * Writes the significant state data of this class, in a readable format,
     * to the specified output writer. <p>
     *
     * @param writer output resource for the introspection data
     * @param fullBMD true indicates BeanMetaData are also introspected
     */
    // F86406
    public void introspect(IntrospectionWriter writer, boolean fullBMD) {
        // -----------------------------------------------------------------------
        // Indicate the start of the dump, and include the toString()
        // of EJSContainer, so this can easily be matched to a trace.
        // -----------------------------------------------------------------------
        writer.begin("EJSContainer Dump ---> " + this);

        writer.println("ivName                = " + ivName);
        writer.println("ivEJBRuntime          = " + ivEJBRuntime);
        writer.println("ivEmbedded            = " + ivEmbedded);
        writer.println("ivEntityHelper        = " + ivEntityHelper);
        writer.println("ivSFSBFailoverEnabled = " + ivSFSBFailoverEnabled);
        writer.println("ivUOWManager          = " + ivUOWManager);
        writer.println("pmiFactory            = " + pmiFactory);
        writer.println("uowCtrl               = " + uowCtrl);
        writer.println("userTransactionImpl   = " + userTransactionImpl);

        // ----------------------------------------------------------------------
        // Collaborators
        // ----------------------------------------------------------------------

        writer.begin("Collaborators");
        writer.println("securityCollaborator                                    = " + ivSecurityCollaborator);
        introspectCollab("beforeActivationCollaborators", ivBeforeActivationCollaborators, writer);
        introspectCollab("beforeActivationAfterCompletionCollaborators", ivBeforeActivationAfterCompletionCollaborators, writer);
        introspectCollab("afterActivationCollaborators", ivAfterActivationCollaborators, writer);
        writer.end();

        // ----------------------------------------------------------------------
        // Dump metadata for all installed beans
        // ----------------------------------------------------------------------

        writer.begin("internalBeanMetaDataStore : " + internalBeanMetaDataStore.size() + " installed beans");
        synchronized (internalBeanMetaDataStore) {
            if (fullBMD) {
                for (Enumeration<BeanMetaData> en = internalBeanMetaDataStore.elements(); en.hasMoreElements();) {
                    BeanMetaData bmd = en.nextElement();
                    bmd.introspect(writer);
                }
            } else {
                List<String> keyNames = new ArrayList<String>();

                for (J2EEName name : internalBeanMetaDataStore.keySet()) {
                    keyNames.add(name.toString());
                }

                Collections.sort(keyNames);

                Set<J2EEName> keys = internalBeanMetaDataStore.keySet();
                for (String keyName : keyNames) {
                    for (J2EEName key : keys) {
                        if (keyName.equals(key.toString())) {
                            writer.println(keyName + " : " + internalBeanMetaDataStore.get(key));
                            break;
                        }
                    }
                }
            }
        } // end synchronized
        writer.end();

        activator.introspect(writer);
        wrapperManager.introspect(writer);

        writer.end();
    }

    /**
     * Used by introspect() to nicely format an array of collaborators
     *
     * @param name name of collaborator array
     * @param collabArray the array itself
     * @param is stream for introspection data
     */
    private void introspectCollab(String name, EJBRequestCollaborator<?>[] collabArray, IntrospectionWriter writer) {

        if (collabArray != null && collabArray.length > 0) { // d742434
            int i = 0;
            for (Object element : collabArray) {
                String outString = name + "[" + i++ + "]";
                String format = String.format("%-55s = %s", outString, Util.identity(element)); // 619922.1
                writer.println(format);
            }
        } else {
            String outString = name + "[]";
            String format = String.format("%-55s %s", outString, "is empty.");
            writer.println(format);
        }

    }

} // EJSContainer
