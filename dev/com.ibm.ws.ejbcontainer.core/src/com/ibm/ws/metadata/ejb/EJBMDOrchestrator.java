/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import static com.ibm.ejs.container.ContainerProperties.AllowCachedTimerDataFor;
import static com.ibm.ejs.container.ContainerProperties.AllowCustomFinderSQLForUpdate;
import static com.ibm.ejs.container.ContainerProperties.DefaultSessionAccessTimeout;
import static com.ibm.ejs.container.ContainerProperties.DefaultStatefulSessionTimeout;
import static com.ibm.ejs.container.ContainerProperties.EE5Compatibility;
import static com.ibm.ejs.container.ContainerProperties.ExtendSetRollbackOnlyBehaviorBeyondInstanceFor;
import static com.ibm.ejs.container.ContainerProperties.FbpkAlwaysReadOnly;
import static com.ibm.ejs.container.ContainerProperties.LimitSetRollbackOnlyBehaviorToInstanceFor;
import static com.ibm.ejs.container.ContainerProperties.NoEJBPool;
import static com.ibm.ejs.container.ContainerProperties.PoolSize;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapper.LOCAL_BEAN_WRAPPER_FIELD;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapper.MANAGED_BEAN_BEANO_FIELD;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapper.MESSAGE_ENDPOINT_BASE_FIELD;
import static com.ibm.ws.metadata.ejb.CheckEJBAppConfigHelper.isValidationFailable;
import static com.ibm.ws.metadata.ejb.CheckEJBAppConfigHelper.isValidationLoggable;

import java.io.Serializable;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.RunAs;
import javax.ejb.AfterBegin;
import javax.ejb.AfterCompletion;
import javax.ejb.BeforeCompletion;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJBContext;
import javax.ejb.Init;
import javax.ejb.LockType;
import javax.ejb.MessageDrivenBean;
import javax.ejb.Remove;
import javax.ejb.Schedule;
import javax.ejb.ScheduleExpression;
import javax.ejb.Schedules;
import javax.ejb.SessionBean;
import javax.ejb.SessionSynchronization;
import javax.ejb.StatefulTimeout;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.BusinessLocalWrapperProxy;
import com.ibm.ejs.container.ContainerConfigConstants;
import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.ContainerProperties;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.LifecycleInterceptorWrapper;
import com.ibm.ejs.container.ManagedBeanHome;
import com.ibm.ejs.container.SessionHome;
import com.ibm.ejs.container.UserTransactionWrapper;
import com.ibm.ejs.container.WrapperProxyState;
import com.ibm.ejs.container.interceptors.InterceptorMetaData;
import com.ibm.ejs.container.util.DeploymentUtil;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.ejs.container.util.MethodAttribUtils;
import com.ibm.ejs.container.util.NameUtil;
import com.ibm.ejs.csi.ActivitySessionMethod;
import com.ibm.ejs.csi.ApplicationExceptionImpl;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.tx.jta.embeddable.LocalTransactionSettings;
import com.ibm.websphere.cpi.Persister;
import com.ibm.websphere.csi.ActivitySessionAttribute;
import com.ibm.websphere.csi.EJBModuleConfigData;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.MethodInterface;
import com.ibm.websphere.csi.TransactionAttribute;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.CallbackKind;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.facade.EJBClassFactory;
import com.ibm.ws.ejbcontainer.facade.EJBConfiguration;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperProxy;
import com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType;
import com.ibm.ws.ejbcontainer.jitdeploy.JITDeploy;
import com.ibm.ws.ejbcontainer.runtime.EJBJPAContainer;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.javaee.dd.common.DisplayName;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.SecurityRoleRef;
import com.ibm.ws.javaee.dd.ejb.ApplicationException;
import com.ibm.ws.javaee.dd.ejb.AssemblyDescriptor;
import com.ibm.ws.javaee.dd.ejb.CMRField;
import com.ibm.ws.javaee.dd.ejb.ContainerTransaction;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.EJBRelation;
import com.ibm.ws.javaee.dd.ejb.EJBRelationshipRole;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejb.Entity;
import com.ibm.ws.javaee.dd.ejb.ExcludeList;
import com.ibm.ws.javaee.dd.ejb.InitMethod;
import com.ibm.ws.javaee.dd.ejb.Interceptor;
import com.ibm.ws.javaee.dd.ejb.InterceptorBinding;
import com.ibm.ws.javaee.dd.ejb.Interceptors;
import com.ibm.ws.javaee.dd.ejb.MessageDriven;
import com.ibm.ws.javaee.dd.ejb.MethodPermission;
import com.ibm.ws.javaee.dd.ejb.NamedMethod;
import com.ibm.ws.javaee.dd.ejb.Relationships;
import com.ibm.ws.javaee.dd.ejb.RemoveMethod;
import com.ibm.ws.javaee.dd.ejb.SecurityIdentity;
import com.ibm.ws.javaee.dd.ejb.Session;
import com.ibm.ws.javaee.dd.ejb.TimerSchedule;
import com.ibm.ws.javaee.util.DDUtil;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.runtime.metadata.MethodMetaData;
import com.ibm.ws.traceinfo.ejbcontainer.TEBeanMetaDataInfo;
import com.ibm.wsspi.ejbcontainer.WSEJBHandlerResolver;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionEngineAccessor;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefBindingHelper;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;
import com.ibm.wsspi.injectionengine.MethodMap;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 * <code>EJBMDOrchestrator</code> constructs and populates the application
 * metadata objects required for the container to operate at runtime.
 * This includes both metadata processing that occurs when applications
 * are started, and additional processing which occurs as beans (ie. EJBs)
 * are started.
 **/
public abstract class EJBMDOrchestrator {
    private static final String CLASS_NAME = EJBMDOrchestrator.class.getName();
    private static final TraceComponent tc = Tr.register(EJBMDOrchestrator.class, "EJBContainer", "com.ibm.ejs.container.container");

    private static final TraceComponent tcVerbose = Tr.register(CLASS_NAME + "-Verbose",
                                                                EJBMDOrchestrator.class,
                                                                "MetaDataVerbose",
                                                                "com.ibm.ejs.container.container"); //d481127.11

    private static final TraceComponent tcInjection = Tr.register(CLASS_NAME + "-Injection",
                                                                  EJBMDOrchestrator.class,
                                                                  InjectionConfigConstants.traceString,
                                                                  InjectionConfigConstants.messageFile);

    private static final Class<?>[] NO_PARAMS = new Class<?>[0];

    /**
     * Static initializer for common_tc.
     */
    static {
        // populate static mapping tables
        populateNameTypeFromBeanTypeMap();
        populateCompressBeanTypeMap();
    }

    /*************************************************************************************
     *
     * The following set of methods do metadata processing at application start time.
     * See header below of methods that handle bean start time.
     *
     *************************************************************************************/

    /**
     * Create the EJB Container's internal module metadata object and fill in the data from
     * the metadata framework's generic ModuleDataObject. This occurs at application start
     * time.
     *
     * @param ejbAMD is the container's metadata for the application containing this module
     * @param mid the data needed to initialize a bean module.
     * @param statefulFailoverCache is an EJB Container configuration object that indicates whether
     *            or not SFSB failover is active for this module.
     * @param container used to obtain container-level SFSB failover values.
     *
     * @return EJBModuleMetaDataImpl is the Container's internal format for module
     *         configuration data.
     */
    public EJBModuleMetaDataImpl createEJBModuleMetaDataImpl(EJBApplicationMetaData ejbAMD, // F743-4950
                                                             ModuleInitData mid,
                                                             SfFailoverCache statefulFailoverCache,
                                                             EJSContainer container) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createEJBModuleMetaDataImpl");

        EJBJar ejbJar = mid.ivEJBJar;

        EJBModuleMetaDataImpl mmd = mid.createModuleMetaData(ejbAMD);
        mmd.ivInitData = mid;
        mmd.ivMetadataComplete = mid.ivMetadataComplete;
        mmd.ivJ2EEName = mid.ivJ2EEName;
        mmd.ivName = mid.ivName;
        mmd.ivAppName = mid.ivAppName;
        mmd.ivLogicalName = mid.ivLogicalName;
        mmd.ivApplicationExceptionMap = createApplicationExceptionMap(ejbJar);
        mmd.ivModuleVersion = ejbJar == null ? BeanMetaData.J2EE_EJB_VERSION_3_0 : ejbJar.getVersionID();

        // Determine if the SetRollbackOnly behavior should be as it was in 2.x
        // modules (where SetRollbackOnly would need to be invoked within the
        // actual EJB instance), or use the 3.x behavior (where
        // SetRollbackOnly can be invoke within and ejb instance that is itself
        // invoked from within the ejb instance that began the transaction).
        // For EJB 3.x applications, the LimitSetRollbackOnlyBehaviorToInstanceFor property
        // is usd to indicate that the 2.x behavior should be used.
        // Likewise, 2.x applications can use the ExtendSetRollbackOnlyBehaviorBeyondInstanceFor
        // property to get the 3.x behavior.
        // d461917.1
        if (mmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) {
            if ((LimitSetRollbackOnlyBehaviorToInstanceFor == null) ||
                (!LimitSetRollbackOnlyBehaviorToInstanceFor.contains(mmd.ivAppName))) {
                mmd.ivUseExtendedSetRollbackOnlyBehavior = true;
            }

        } else { // not a 3.0 module
            if ((ExtendSetRollbackOnlyBehaviorBeyondInstanceFor != null) &&
                (ExtendSetRollbackOnlyBehaviorBeyondInstanceFor.contains("*") ||
                 ExtendSetRollbackOnlyBehaviorBeyondInstanceFor.contains(mmd.ivAppName))) {
                mmd.ivUseExtendedSetRollbackOnlyBehavior = true;
            }
        }

        // Get the failover instance ID and SFSB Failover value to use for this module if
        // a SfFailoverCache object was created.
        if (statefulFailoverCache != null) {
            mmd.ivSfsbFailover = getSFSBFailover(mmd, container);
            mmd.ivFailoverInstanceId = getFailoverInstanceId(mmd, statefulFailoverCache);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createEJBModuleMetaDataImpl: " + mmd);
        return mmd;

    } // createEJBModuleMetaDataImpl

    private Map<String, javax.ejb.ApplicationException> createApplicationExceptionMap(EJBJar ejbJar) {
        // Get List of ApplicationException objects and convert to a Map where
        // key is String name of exceptions class and value is the rollback
        // setting of the exception class from xml.
        // d541552, d541552.1 - Improve performance and footprint.  Returned
        //                      exceptionMap in MDO may be null (ie. the
        //                      map is only created when it is needed).
        if (ejbJar != null) {
            AssemblyDescriptor assemblyDescriptor = ejbJar.getAssemblyDescriptor();
            if (assemblyDescriptor != null) {
                List<ApplicationException> applicationExceptions = assemblyDescriptor.getApplicationExceptionList();
                if (!applicationExceptions.isEmpty()) {
                    Map<String, javax.ejb.ApplicationException> appExMap = new HashMap<String, javax.ejb.ApplicationException>(); // F743-14982
                    for (ApplicationException appEx : applicationExceptions) {
                        String className = appEx.getExceptionClassName();
                        appExMap.put(className, new ApplicationExceptionImpl(appEx.isRollback(), appEx.isInherited())); // F743-14982
                    }

                    return appExMap;
                }
            }
        }

        return null;
    }

    /**
     * Create the EJB Container's internal bean metadata object and fill in the data from
     * the metadata framework's generic ComponentDataObject. During application start the
     * constructor fills in enough data to allow a Home record to be created for the Bean.
     * Later when an application wants to use the bean (ie. bean start) the remainder of
     * BMD will be filled in.
     *
     * @param bid the data needed to initialize a bean.
     * @param mmd is the Module's configuration data in the EJB Container's
     *            internal format.
     * @param container is the EJB Container in the current process.
     * @return BeanMetaData the EJB Container's internal format for bean configuration
     *         data.
     * @throws EJBConfigurationException - for customer configuration errors
     * @throws ContainerException - for internal problems creating the meta data
     */
    public BeanMetaData createBeanMetaData(BeanInitData bid,
                                           EJBModuleMetaDataImpl mmd,
                                           EJSContainer container,
                                           boolean initAtStartup,
                                           boolean initAtStartupSet) throws EJBConfigurationException, ContainerException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "createBeanMetaData");
        }

        // Create empty BMD and helper object to store WCCM data pointers so that they may easily be dropped
        // after we have read everything we need from WCCM.
        BeanMetaData bmd = bid.createBeanMetaData();
        bmd.ivInitData = bid;

        // save pointers to EJSContainer and module level metadata
        bmd.container = container;
        bmd._moduleMetaData = mmd;

        // load basic metadata for the Bean
        bmd.enterpriseBeanName = bid.ivName;
        bmd.enterpriseBeanClassName = bid.ivClassName;
        bmd.j2eeName = bid.ivJ2EEName;
        bmd.type = bid.ivType;

        boolean hasRemote = bid.ivRemoteHomeInterfaceName != null ||
                            bid.ivRemoteBusinessInterfaceNames != null;
        bmd.wccm = container.getEJBRuntime().setupBean(bmd, hasRemote); // F743-13023, F743-13024, F743-18775
        bmd.wccm.initialize(bid); // F743-18775

        if (bmd.wccm.hasApplicationVersion()) {
            bmd.ivApplicationVersionId = bmd.wccm.getApplicationVersion(); // F68113.4
        }

        bmd.ivLocalBean = bid.ivLocalBean;

        // Load presence of WebServiceEndpoint                      F743-1756CodRv
        if (bid.ivWebServiceEndpointInterfaceName != null) {
            // F743-16272
            if (mmd.ivEJBInWAR) {
                // Section 20.4.6 of EJB 3.1 spec prohibits JAX-RPC endpoints from being
                // packaged inside a war.
                String endpointInterface = bid.ivWebServiceEndpointInterfaceName; // 658576
                Tr.error(tc, "JAX_RPC_ENDPOINT_IN_WAR_CNTR0317E", new Object[] { bmd.j2eeName, endpointInterface }); // 658576
                throw new EJBConfigurationException("The " + bmd.j2eeName + " bean is packaged inside a WAR " +
                                                    "module and has been defined as a JAX-RPC endpoint using the " +
                                                    "ejb-jar.xml deployment descriptor.  The " +
                                                    endpointInterface + // 658576
                                                    " interface is declared as the JAX-RPC endpoint. " +
                                                    "However, beans packaged in a WAR " +
                                                    "module are not supported as JAX-RPC endpoints.  Package " +
                                                    "the bean in an EJBJAR module, or remove the service endpoint interface " +
                                                    "from the deployment descriptor.");
            }

            // If a JAX-RPC service-endpoint has been specified in XML, then the
            // bean has a service endpoint if it is stateless or singleton.
            bmd.ivHasWebServiceEndpoint = bmd.type == InternalConstants.TYPE_STATELESS_SESSION ||
                                          bmd.type == InternalConstants.TYPE_SINGLETON_SESSION;
        } else {
            if (mmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) {
                if (bmd.type == InternalConstants.TYPE_STATELESS_SESSION) {
                    // d630468 - In EJB 3.0, the container did not want to have
                    // logic to determine whether or not a JAX-WS service endpoint
                    // existed, so it allowed service endpoints to be defined for
                    // all stateless beans.  Unfortunately, this allowed JAX-RPC
                    // service endpoints to be declared in webservices.xml without
                    // being declared in ejb-jar.xml.  For compatibility, we allow
                    // this even though it is clearly disallowed by the EJB spec.
                    bmd.ivHasWebServiceEndpoint = true;
                } else if (bid.ivWebServiceEndpoint) {
                    // Otherwise, if the bean is a singleton, we have already
                    // determined whether or not the bean has a JAX-WS endpoint.
                    bmd.ivHasWebServiceEndpoint = bmd.type == InternalConstants.TYPE_SINGLETON_SESSION;
                }
            }
        }

        bmd.passivationCapable = bid.ivPassivationCapable;

        if (bid.ivStartup && bmd.type != InternalConstants.TYPE_SINGLETON_SESSION) {
            // Error - only Singletons can be marked with Startup
            // via annotations or XML.
            Tr.error(tc, "STARTUP_SPECIFIED_ON_NON_SINGLETON_SESSION_BEAN_CNTR0189E",
                     new Object[] { bmd.j2eeName });
            throw new EJBConfigurationException("The " + bmd.j2eeName + " bean has been defined to " +
                                                "be a Startup bean.  Only Singleton Session " +
                                                "beans may contain the @Startup annotations or " +
                                                "be defined with the Startup XML deployment descriptor.");
        }

        // F743-4950 - Perform basic error checking for DependsOn.  The bulk of
        // the processing occurs at the end of application start time when the
        // links can be resolved.
        if (bid.ivDependsOn != null && bmd.type != InternalConstants.TYPE_SINGLETON_SESSION) {
            Tr.error(tc, "DEPENDS_ON_SPECIFIED_ON_NON_SINGLETON_BEAN_CNTR0197E",
                     new Object[] { bmd.j2eeName.getComponent() });
            throw new EJBConfigurationException("CNTR0197E: The " + bmd.j2eeName.getComponent()
                                                + " non-singleton enterprise bean has dependency metadata.");
        }

        // If configuration data for this whole module is only specified via xml, then the
        // metadata for this bean will only come from xml (except for ManagedBeans F92163).
        bmd.metadataComplete = mmd.ivMetadataComplete && bmd.type != InternalConstants.TYPE_MANAGED_BEAN;

        bmd.usesBeanManagedTx = bid.ivBeanManagedTransaction;
        bmd.cmpVersion = bid.ivCMPVersion;

        // The initialization of EJBs is deferred until they are first used.  However there are certain
        // exceptions to that rule. These are listed below in order of priority.
        //
        // 1. Currently ZOS is at JDK 1.4.1.  Certain JDK 1.4.2 APIs are required to allow an IOR to be
        //    created without its corresponding object existing and therefore deferred EJB initialization
        //    is not possible.  This will be removed when ZOS moves to 1.4.2.
        // 2. Message Driven Beans must exist at application start time.
        // 3. Startup Beans must exist at application start time.
        // 4. The user can set a property "com.ibm.websphere.ejbcontainer.initializeEJBsAtStartup" to
        //    globally disable deferred EJB initialization.
        // 5. The user can set a flag within the deployment descriptors "isStartEJBAtApplicationStart" to
        //    disable deferred EJB initialization for an individual EJB type.
        //                                                                          //d203449

        boolean deferEJBInitialization = true; // The default behavior is to defer initialization
        boolean isStartupBean = false;
        boolean isStartEJBAtApplicationStart = false; //199884

        // For performance reasons, only check other parameters if this home has not already
        // been determined to be for a MDB or ManagedBean.
        if (bmd.type != InternalConstants.TYPE_MESSAGE_DRIVEN &&
            bmd.type != InternalConstants.TYPE_MANAGED_BEAN) {
            // Check if Module or Application Startup Bean
            String appStartupbeanHomeInterface = "com.ibm.websphere.startupservice.AppStartUpHome";
            String modStartupbeanHomeInterface = "com.ibm.websphere.startupservice.ModStartUpHome"; //234978
            String homeInterfaceClassName = bid.ivRemoteHomeInterfaceName;
            if ((homeInterfaceClassName != null) &&
                ((homeInterfaceClassName.equals(appStartupbeanHomeInterface)) ||
                 (homeInterfaceClassName.equals(modStartupbeanHomeInterface)))) { //234978
                isStartupBean = true;

                // F743-21901 d641395
                // We now know this is a legacy startup bean.
                //
                // For new technologies, such as ejb-in-war, customers should no
                // longer be using the legacy startup beans.  Rather, they should
                // now be using Singleton beans flagged with the @Startup annoation
                // or xml.
                //
                // If the ModuleDataObject contains a WARFile, then that tells us
                // we are dealing with an ejb embedded inside a war module, and so
                // we want to fail the application startup.
                if (mmd.ivEJBInWAR) {
                    Tr.error(tc, "LEGACY_STARTUP_BEAN_IN_WAR_CNTR0319E", new Object[] { bmd.j2eeName });
                    throw new EJBConfigurationException("The " + bmd.j2eeName + " bean is a startup bean, and is " +
                                                        "packaged inside of a Web archive (WAR) module, which is not allowed.  Startup " +
                                                        "beans must be packaged inside of a stand-alone Enterprise JavaBean (EJB) module.  Start-up behavior " +
                                                        "for an EJB component packaged inside of a WAR module is obtained by using a " +
                                                        "singleton session bean that is marked with the @Startup annotation, or the " +
                                                        "corresponding XML element.");
                }
            }

            // For performance reasons, only check others parameters if this home has not already
            // been determined to be for a Startup Bean
            if (!isStartupBean) {

                // The global defer ejb initialization flag has priority over the flag on the individual
                // homes, so it is only important to check the individual flags when the global flag is not
                // set.
                if (!initAtStartupSet) {
                    isStartEJBAtApplicationStart = bmd.wccm.isStartEJBAtApplicationStart(); // F743-18775
                } // !initAtStartupSet
            } //!isStartupBean
        } // !isMessageDriven

        // By default EJB initialization is deferred, but, if they are
        // MessageDrivenBeans, StartupBeans, or they have been configured
        // to not be deferred then they will not be defered.  However,
        // all EJBs except MessageDrivenBeans will be deferred in the
        // adjunct process on z.                                           d259812
        if (bmd.type == InternalConstants.TYPE_MESSAGE_DRIVEN ||
            (!EJSPlatformHelper.isZOSCRA() &&
             (isStartupBean ||
              initAtStartup ||
              isStartEJBAtApplicationStart))) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, bmd.enterpriseBeanName + " will be initialized at Application start");
            deferEJBInitialization = false;
        } else {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Initialization of " + bmd.enterpriseBeanName + " will be deferred until it is first used.");
        }
        bmd.ivDeferEJBInitialization = deferEJBInitialization;

        bmd.classLoader = bid.ivModuleInitData.ivClassLoader;
        bmd.ivContextClassLoader = bid.ivModuleInitData.getContextClassLoader(); // F85059
        bmd.ivModuleVersion = mmd.ivModuleVersion;

        bmd.homeInterfaceClassName = bid.ivRemoteHomeInterfaceName;
        bmd.localHomeInterfaceClassName = bid.ivLocalHomeInterfaceName;
        bmd.ivBusinessRemoteInterfaceClassNames = bid.ivRemoteBusinessInterfaceNames;
        bmd.ivBusinessLocalInterfaceClassNames = bid.ivLocalBusinessInterfaceNames;

        if (isTraceOn && tcVerbose.isDebugEnabled()) { //d481127.11
            Tr.debug(tcVerbose, bmd.wccm.dump());
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "createBeanMetaData for " + bmd.j2eeName);
        }

        return (bmd);

    } // createBeanMetaData

    /**
     * Process binding data from ibm-ejb-jar-bnd files.
     *
     * @param mid the data needed to initialize a bean module.
     * @param mmd the module metadata
     */
    // F743-36290.1
    public abstract void processEJBJarBindings(ModuleInitData mid, EJBModuleMetaDataImpl mmd) throws EJBConfigurationException;

    /*************************************************************************************
     *
     * The following set of methods do metadata processing at bean start time. See
     * above for methods that handle application start time.
     *
     *************************************************************************************/

    /**
     * Create the persister needed to support CMP 1.1 beans.
     *
     * @param bmd is the EJB Container's internal format for bean configuration data.
     * @param defaultDataSourceJNDIName the default data source JNDI name
     * @return Persister provides persistence services for CMP 1.1 beans.
     * @throws ContainerException - for internal problems creating persister
     */
    public abstract Persister createCMP11Persister(BeanMetaData bmd,
                                                   String defaultDataSourceJNDIName) throws ContainerException;

    /**
     * Perform metadata processing for a bean that is being deferred.
     */
    public void processDeferredBMD(BeanMetaData bmd) throws EJBConfigurationException, ContainerException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processDeferredBMD: " + bmd.j2eeName);

        // F743-506 - Process automatic timers.  This always needs to be
        // done regardless of deferred EJB initialization.
        if (bmd.ivInitData.ivTimerMethods == null) {
            processAutomaticTimerMetaData(bmd);
        }

        // d659020 - Clear references that aren't needed for deferred init.
        bmd.ivInitData.unload();
        bmd.wccm.unload();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processDeferredBMD");
    }

    /**
     * Finish initializing any remaining internal metadata we need so
     * that we are prepared to activate and provide container services
     * for bean instances of this EJB type.
     *
     * @param bmd current metadata for the bean
     * @throws ContainerException - for internal problems initializing bean metadata
     * @throws EJBConfigurationException - for customer configuration errors
     */
    //  F743-17630 refactored method F743-17630CodRv
    public void finishBMDInitWithReferenceContext(BeanMetaData bmd) throws ContainerException, EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) //367834.5
            Tr.entry(tc, "finishBMDInitWithReferenceContext", bmd.j2eeName); //367834.5  d640935.1

        try {
            processReferenceContext(bmd);
        } catch (InjectionException ex) {
            // Unwrap the exception thrown by ComponentNameSpaceConfigurationProviderImpl.
            for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof EJBConfigurationException) {
                    throw (EJBConfigurationException) cause;
                }
                if (cause instanceof ContainerException) {
                    throw (ContainerException) cause;
                }
            }

            throw new EJBConfigurationException(ex);
        }

        EnterpriseBean eb = bmd.wccm.enterpriseBean;

        // -----------------------------------------------------------------------
        // Look for Session Synchronization methods for Stateful beans  F743-25855
        // -----------------------------------------------------------------------
        if (bmd.isStatefulSessionBean() &&
            !bmd.usesBeanManagedTx) {
            processSessionSynchronizationMD(bmd, (Session) eb);
        }

        // -----------------------------------------------------------------------
        // Get the managed object factory or constructor for non-entity
        // -----------------------------------------------------------------------
        // Must obtain managed object factory before generating the implementation
        // classes, as the wrapper may be different if managed object factory will
        // handle injection and interceptors.
        if (!bmd.isEntityBean()) {
            initializeManagedObjectFactoryOrConstructor(bmd);
        }

        // ------------------------------------------------------------------------------
        // Load the all the generated (ie. EJBDeploy or JITDeploy)implementation classes
        // ------------------------------------------------------------------------------
        loadGeneratedImplementationClasses(bmd,
                                           bmd.methodsExposedOnLocalInterface, // F743-17630
                                           bmd.methodsExposedOnRemoteInterface, // F743-17630
                                           bmd.methodsExposedOnLocalHomeInterface, // F743-17630
                                           bmd.methodsExposedOnRemoteHomeInterface); // F743-17630

        // -----------------------------------------------------------------------
        // Get the constructor for entity beans
        // -----------------------------------------------------------------------
        // Must obtain the entity bean constructor after loading the generated
        // implementation classes, as the concrete bean class may be generated.
        if (bmd.isEntityBean()) {
            initializeEntityConstructor(bmd);
        }

        // -----------------------------------------------------------------------
        // Build the Message Destination Link Map if this is an MDB.       d510405
        // -----------------------------------------------------------------------
        // For an annotations only configuration, there will be no Enterprise
        // Bean in WCCM.  In this case there are no resource refs in xml to retrieve.
        if (eb != null && eb.getKindValue() == EnterpriseBean.KIND_MESSAGE_DRIVEN) {
            MessageDriven mdb = (MessageDriven) eb;
            String link = mdb.getLink();

            if (link != null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "processing message-destination-link : " + link +
                                 " : " + bmd.j2eeName);

                String moduleName = null;
                String destJndiName = null;
                String appName = bmd.j2eeName.getApplication();

                Map<String, Map<String, String>> appMap = InjectionEngineAccessor.getMessageDestinationLinkInstance().getMessageDestinationLinks().get(appName); //d493167

                // create a new application map if one is not present
                if (appMap == null) {
                    appMap = new HashMap<String, Map<String, String>>();
                    InjectionEngineAccessor.getMessageDestinationLinkInstance().getMessageDestinationLinks().put(appName, appMap); //493167
                }

                // Get the JNDIname of the MessageDestination from BMD
                destJndiName = bmd.ivMessageDestinationJndiName;

                // parse the link if the link name is in the format of moduleName#linkName
                int lindex = link.indexOf('#');
                if (lindex != -1) {
                    // Module name may have path information... remove it.    d510405
                    int mindex = link.lastIndexOf('/');
                    if (mindex > -1 && mindex < lindex) {
                        moduleName = link.substring(mindex + 1, lindex);
                    } else {
                        // If no path was specified, then the referenced module
                        // is in the same location. Use first half of the link.
                        moduleName = link.substring(0, lindex);
                    }
                    link = link.substring(lindex + 1);
                } else {
                    moduleName = bmd.j2eeName.getModule();
                }

                Map<String, String> moduleMap = appMap.get(moduleName);
                if (moduleMap == null) {
                    moduleMap = new HashMap<String, String>();
                    appMap.put(moduleName, moduleMap);
                }

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "adding destination: " + link + " -> destinationJndiName:" +
                                 destJndiName + " for module: " + moduleName);

                moduleMap.put(link, destJndiName);
            }
        }

        //------------------------------------------------------------------------
        // Finish up a few last odds & ends
        //------------------------------------------------------------------------

        // Z/OS specific metadata and trace
        processZOSMetadata(bmd);

        // F743-609CodRev
        // If this bean has any asynchronous methods, make sure the container has a work manager
        // setup to execute runable objects for those methods
        if (bmd.ivHasAsynchMethod) {
            bmd.container.getEJBRuntime().setupAsync();
        }

        // Finally, run some checks to make sure everything we have initialized
        // is legal.
        bmd.validate();//d133681

        // Metadata for this bean type is now fully ready to use.  This flag must only be
        // set at the very end of BMD initialization.
        bmd.fullyInitialized = true;
        bmd.wccm = null;
        bmd.ivInitData = null;
        removeTemporaryMethodData(bmd); // F743-17630
        if (bmd._moduleMetaData.ivInterceptorBindingMap != null) {
            bmd._moduleMetaData.removeEJBInterceptorBindings(bmd.enterpriseBeanName); // d695226
        }

        // Write out bean metadata into the trace file in a format that the trace
        // explorer tool can read.
        if (isTraceOn && TEBeanMetaDataInfo.isTraceEnabled()) // d527372
            TEBeanMetaDataInfo.writeTraceBeanMetaData(bmd, bmd.ivModuleVersion); // d161864

        // TODO: MLS - this was an old comment.  Check if ActivationConfig could go
        // in the new bmd.wccm object now and be nulled out.
        // Note, do not null out ivActivationConfig at this time since
        // we must defer the destruction of reference until endpoint is
        // activated. See MessageEndpointFactoryImpl.activateEndpoint method.
        //ivActivationConfig = null;   //LI2110.41

        if (isTraceOn && tc.isEntryEnabled()) //367834.5
            Tr.exit(tc, "finishBMDInitWithReferenceContext"); //367834.5 d640935.1

    } // finishBMDinit

    /**
     * Set the activation and load policies for this bean. Useful only
     * for stateful session beans and entity beans.
     *
     * For stateful beans the following rules hold :
     * ActivationPolicy - can be set to ONCE or TRAN or AS (Activity Session)
     * default is ONCE
     *
     * For entity beans the following rules hold :
     * Activation - ONCE, Load - ONCE = Option A
     * Activation - ONCE, Load - TRAN = Option B
     * Activation - TRAN, Load - TRAN = Option C
     * Activation - TRAN, Load - ONCE = Invalid, print a warning
     * Activation - AS, Load - ONCE = Invalid, print a warning
     * Activation - AS, Load - TRAN = Activity Session Activation
     *
     * @param bmd
     * @throws EJBConfigurationException - for customer configuration errors
     *
     **/
    //d481127.11
    protected abstract void setActivationLoadPolicy(BeanMetaData bmd) throws EJBConfigurationException;

    /**
     *
     * 116394.3
     * Set the Concurrency Control policies for this bean.
     *
     * For session beans the following rules hold :
     * optimisticConcurrencyControl = false
     *
     * For entity beans the following rules hold :
     * BMP Beans - optimisticConcurrencyControl = false
     * CMP Beans with ConcurrencyControl set to Optimistic - optimisticConcurrencyControl = true
     * CMP Beans with ConcurrencyControl set to Pessimistic - optimisticConcurrencyControl = false
     *
     * @parm bmd
     */
    protected abstract void setConcurrencyControl(BeanMetaData bmd);

    // TODO:
    ///////////////////////////////////////////////////////////////////////
    // FIX ME : This method should be removed and replaced by a method
    //          which does something with the pin policy. This is a place
    //          holder method for now.
    //          Check if the pin policy is anything other than transaction
    //          Print a warning to inform the use that the only valid pin
    //          policy is that of transaction
    //////////////////////////////////////////////////////////////////////
    //

    /**
     * @param bmd
     */
    protected abstract void checkPinPolicy(BeanMetaData bmd);

    // F743-14912
    /**
     * Sets BeanMetaData.commitDanglingWork depending on the local transaction
     * settings.
     *
     * @param bmd
     */
    protected void setCommitDanglingWork(BeanMetaData bmd) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        int unresolvedAction = bmd._localTran.getUnresolvedAction();
        if (unresolvedAction == LocalTransactionSettings.UNRESOLVED_COMMIT) {
            bmd.commitDanglingWork = true;
        }

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "The commitDanglingWork flag is set to " + bmd.commitDanglingWork);
        }
    }

    // F743-1751 - Factored out of initializeBeanMethodMD.
    /**
     * Populate the transactionAttrs and activitySessionAttrs arrays from XML
     * metadata in activitySessionList and transactionList and from annotation
     * data in ejbMethods.
     */
    private void initializeBeanMethodTransactionAttributes(boolean isEntity,
                                                           boolean addRemove,
                                                           MethodInterface methodInterface,
                                                           Method[] ejbMethods,
                                                           String[] entityNoTxAttrMethods,
                                                           String[] entityNoTxAttrMethodSignatures,
                                                           List<ContainerTransaction> transactionList,
                                                           List<ActivitySessionMethod> activitySessionList,
                                                           String[] methodNames,
                                                           Class<?>[][] methodParamTypes,
                                                           String[] methodSignatures,
                                                           TransactionAttribute[] transactionAttrs,
                                                           ActivitySessionAttribute[] activitySessionAttrs,
                                                           BeanMetaData bmd) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "initializeBeanMethodTransactionAttributes: " + methodInterface);

        int numMethods = ejbMethods.length;
        int numExplicitMethods = numMethods - (addRemove ? 1 : 0);

        for (int i = 0; i < numExplicitMethods; i++) {
            // For Bean-managed TX (session beans only), initialize method Tx attrs
            // to TX_BEAN_MANAGED.  For other cases, initialize to TX_REQUIRED.
            // FIX_ME (RRS): I made this TX_REQUIRED to conform to the way it was in 4.0.1, but
            // it seems to me that this should perhaps be TX_NOT_SUPPORTED?
            // ejbTimeout must default to REQUIRES_NEW since REQUIRED is not
            // allowed per the EJB Specification.                         LI2281.11
            if (bmd.usesBeanManagedTx)
                transactionAttrs[i] = TransactionAttribute.TX_BEAN_MANAGED;

            // LIDB441.5 - For Bean-managed AS (session beans only), initialize method AS attrs
            // to AS_BEAN_MANAGED.  For other cases, initialize to AS_UNKNOWN.
            activitySessionAttrs[i] = (bmd.usesBeanManagedAS) ? ActivitySessionAttribute.AS_BEAN_MANAGED : ActivitySessionAttribute.AS_UNKNOWN;
        }

        int metaMethodElementKind = methodInterface.getValue();

        // Only get and check method-level Tx attributes if container is managing Tx
        if (!bmd.usesBeanManagedTx) {
            // Process all Transaction Attributes from WCCM. If there is no transactionList,
            // assume this is an annotations only configuration scenario.
            if (transactionList != null) {
                MethodAttribUtils.getXMLCMTransactions(transactionAttrs,
                                                       metaMethodElementKind,
                                                       methodNames,
                                                       methodParamTypes,
                                                       transactionList,
                                                       bmd); //PK93643
            }

            // Process all Transaction Attributes from Java Annotations,
            // and set defaults for all methods not explicitly configured.
            MethodAttribUtils.getAnnotationCMTransactions(transactionAttrs,
                                                          metaMethodElementKind,
                                                          ejbMethods,
                                                          bmd);

            // Per section 11.4.1 of EJB 1.1 spec:
            // - for EntityBeans: getEJBHome, getHandle, getPrimaryKey and isIdentical must be TX_NOT_SUPPORTED
            // - for SessionBeans: all EJBObject interface methods must be TX_NOT_SUPPORTED
            if (isEntity) {
                MethodAttribUtils.checkTxAttrs(transactionAttrs,
                                               methodNames,
                                               methodSignatures, //PQ63130
                                               entityNoTxAttrMethods,
                                               entityNoTxAttrMethodSignatures, //PQ63130
                                               TransactionAttribute.TX_NOT_SUPPORTED);
            } else if (addRemove) {
                // For Session beans, the only EJBObject or EJBLocalObject method
                // that could be in the list is the 'remove' method added to the
                // end.  If it is present, force it to NOT_SUPPORTED.
                // Note that there may be another 'remove' method in the list from
                // the business interfaces.... but it is allowed for that one to
                // have a different transaction attribute.                   d405948
                transactionAttrs[numExplicitMethods] = TransactionAttribute.TX_NOT_SUPPORTED;
            }

            if (methodInterface == MethodInterface.TIMED_OBJECT) {
                // Per EJB Specification, ejbTimeout must be TX_REQUIRES_NEW or
                // TX_NOT_SUPPORTED.  However, internally, TX_REQUIRES_NEW will be
                // implemented as TX_REQUIRED so that the EJB method shares the
                // global transaction begun by scheduler QOS_ONLYONCE.     LI2281.11
                // If timerQOSAtLeastOnceForRequired is specified, TX_REQUIRED is
                // implemented as TX_REQUIRES_NEW, which uses scheduler
                // QOS_ATLEASTONCE and then begins a new global transaction for the
                // EJB method as expected.                                 RTC116312
                for (int i = 0; i < numMethods; i++) {
                    TransactionAttribute txAttr = transactionAttrs[i];
                    if (txAttr == TransactionAttribute.TX_REQUIRES_NEW) {
                        transactionAttrs[i] = TransactionAttribute.TX_REQUIRED;
                    } else if (txAttr == TransactionAttribute.TX_REQUIRED &&
                               ContainerProperties.TimerQOSAtLeastOnceForRequired) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "updating " + ejbMethods[i] +
                                         " from TX_REQUIRED to TX_REQUIRES_NEW for QOS_ATLEASTONCE");
                        transactionAttrs[i] = TransactionAttribute.TX_REQUIRES_NEW;
                    }
                }
            } else if (methodInterface == MethodInterface.LIFECYCLE_INTERCEPTOR &&
                       bmd.type == InternalConstants.TYPE_SINGLETON_SESSION) {
                // F743-1751 - Like timer methods, lifecycle interceptor methods
                // translate REQUIRED into REQUIRES_NEW for singleton.
                for (int i = 0; i < numMethods; i++) {
                    if (transactionAttrs[i] == TransactionAttribute.TX_REQUIRED) {
                        transactionAttrs[i] = TransactionAttribute.TX_REQUIRES_NEW;
                    }
                }
            }

        } else {

            if (bmd.wccm.enterpriseBean != null) {
                MethodAttribUtils.chkBMTFromXML(transactionList, bmd.wccm.enterpriseBean, bmd.j2eeName);//d1414634
            }

            // no need to check annotations if all metadata came
            // from xml.
            if (!bmd.metadataComplete) {
                MethodAttribUtils.chkBMTFromAnnotations(ejbMethods, bmd.j2eeName); //d395828
            }
        }

        // Only get method-level Activity Session attributes if container is managing AS
        if (!bmd.usesBeanManagedAS) {
            MethodAttribUtils.getActivitySessions(activitySessionAttrs,
                                                  metaMethodElementKind,
                                                  methodNames,
                                                  methodParamTypes,
                                                  activitySessionList,
                                                  bmd.enterpriseBeanName,
                                                  bmd.usesBeanManagedAS); // LIDB441.5
        } // if !usesBeanManagedAS
        else {
            if (bmd.wccm.enterpriseBean != null) {
                MethodAttribUtils.chkBMASFromXML(activitySessionList, bmd.wccm.enterpriseBean, bmd.j2eeName);//d141634
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "initializeBeanMethodTransactionAttributes: " + methodInterface);
    }

    /**
     * getIsolationLevels fills the isoLevels array with
     * the isolation level of each method which is specified.
     */
    protected abstract void getIsolationLevels(int[] isoLevels,
                                               int type,
                                               String[] methodNames,
                                               Class<?>[][] methodParamTypes,
                                               List<?> isoLevelList,
                                               EnterpriseBean enterpriseBean);

    /**
     * getReadOnlyAttributes fills the readOnlyAttrs array with
     * the readOnly attribute of each method which is specified.
     */
    protected abstract void getReadOnlyAttributes(boolean[] readOnlyAttrs,
                                                  int type,
                                                  String[] methodNames,
                                                  Class<?>[][] methodParamTypes,
                                                  List<?> accessIntentList,
                                                  EnterpriseBean enterpriseBean);

    /**
     * Determine the bean's method level metadata
     *
     * @param isEntity
     * @param methodInterface
     * @param interfaceClass
     * @param businessInterfaceClasses
     * @param beanMethods
     * @param implMethods
     * @param entityNoTxAttrMethods
     * @param entityNoTxAttrMethodSignatures
     * @param sessionNoTxAttrMethods
     * @param sessionNoTxAttrMethodSignatures
     * @param accessIntentList
     * @param isoLevelList
     * @param transactionList
     * @param activitySessionList
     * @param cdo
     * @param bmd
     * @return the bean's method level meta data.
     */
    private MethodDataWrapper initializeBeanMethodMD(boolean isEntity,
                                                     MethodInterface methodInterface,
                                                     Class<?> interfaceClass,
                                                     Class<?>[] businessInterfaceClasses, // F743-24429
                                                     Method beanMethods[],
                                                     Method implMethods[], // d494984
                                                     String[] entityNoTxAttrMethods,
                                                     String[] entityNoTxAttrMethodSignatures, //PQ63130
                                                     List<?> accessIntentList,
                                                     List<?> isoLevelList,
                                                     List<ContainerTransaction> transactionList,
                                                     List<MethodPermission> securityList,
                                                     ExcludeList excludeList,
                                                     List<ActivitySessionMethod> activitySessionList,
                                                     BeanMetaData bmd,
                                                     ByteCodeMetaData byteCodeMetaData) throws EJBConfigurationException {
        //--------------------------------------------------------
        // Initialize per-method attributes (include place-holder
        // for remove()  method at end of per-method attribute
        // tables)
        //--------------------------------------------------------
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "initializeBeanMethodMetadata: " + methodInterface + ", " + beanMethods.length);

        int metaMethodElementKind = methodInterface.getValue(); // d162441

        int numMethods = beanMethods.length;

        boolean checkAppConfigCustom = bmd.isCheckConfig(); // F743-33178

        // We have to include the remove method of the EJBObject interface,
        // which is filtered out by the DeploymentUtil.getMethods() call.
        // This is not done for the non-EJBObject/EJBLocalObject interfaces
        boolean addRemove = false; // LI2281.07
        if (interfaceClass != null && // d366807
            (methodInterface == MethodInterface.REMOTE ||
             methodInterface == MethodInterface.LOCAL)) {
            addRemove = true;
            numMethods++;
        }

        String[] methodNames = new String[numMethods];
        Class<?>[][] methodParamTypes = new Class<?>[numMethods][];
        String[] methodSignatures = new String[numMethods];
        String[] jdiMethodSignatures = new String[numMethods];
        Method[] ejbMethods = new Method[numMethods]; // 366845.9
        boolean[] asynchMethodFlags = new boolean[numMethods];
        Method[] bridgeMethods = new Method[numMethods]; // d517824
        EJBMethodInfoImpl[] methodInfos = new EJBMethodInfoImpl[numMethods];
        EnterpriseBean wccmEnterpriseBean = bmd.wccm.enterpriseBean; // F743-1752.1
        LockType lockType[] = new LockType[numMethods]; // F743-1752.1
        long accessTimeout[] = new long[numMethods]; // F743-1752.1

        // NOTE: The method-level isolationAttrs and readOnlyAttrs are only applicable to CMP 1.1
        //       beans.  They are not used for CMP 2.0 beans.  (The application profile function
        //       is used instead for CMP 2.0.)

        int[] isolationAttrs = new int[numMethods];
        boolean[] readOnlyAttrs = new boolean[numMethods];
        TransactionAttribute[] transactionAttrs = new TransactionAttribute[numMethods];

        boolean[] denyAll = new boolean[numMethods]; //d366845.11.1
        boolean[] permitAll = new boolean[numMethods]; //d366845.11.1

        @SuppressWarnings("unchecked")
        ArrayList<String>[] roleArrays = new ArrayList[numMethods]; //d366845.11.1

        ActivitySessionAttribute[] activitySessionAttrs = new ActivitySessionAttribute[numMethods]; // LIDB441.5

        for (int i = 0; i < beanMethods.length; i++) {
            methodNames[i] = beanMethods[i].getName();
            methodParamTypes[i] = beanMethods[i].getParameterTypes();
            methodSignatures[i] = MethodAttribUtils.methodSignatureOnly(beanMethods[i]);
            jdiMethodSignatures[i] = MethodAttribUtils.jdiMethodSignature(beanMethods[i]);

            // For every method from the interface(s), find the corresponding
            // method on the EJB implementation class.  This will be needed
            // to obtain any method level annotations, as the interface
            // methods are NOT annotated.  Also, note that it would be an
            // invalid EJB if the bean does not implement all methods on
            // the configured interfaces.                                  366845.9
            // d571522 - For timed object methods, the interface method might be a
            // non-public method from the bean class.  In that case, we already
            // have the Method object.
            if (methodInterface == MethodInterface.TIMED_OBJECT && !Modifier.isPublic(beanMethods[i].getModifiers())) {
                ejbMethods[i] = beanMethods[i];
            } else {
                try {
                    ejbMethods[i] = bmd.enterpriseBeanClass.getMethod(methodNames[i], methodParamTypes[i]);
                } catch (NoSuchMethodException nsmex) {
                    Tr.error(tc, "BEAN_DOES_NOT_IMPLEMENT_REQUIRED_METHOD_CNTR0157E",
                             new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName, beanMethods[i] });
                    throw new EJBConfigurationException("The bean: " + bmd.enterpriseBeanName + " in module: " + bmd._moduleMetaData.ivName +
                                                        " does not implement the required method: " + beanMethods[i]);
                }
            }

            // d494984.1
            // If the ejbMethod is a bridge method, then replace with the target
            // of the bridge method.
            if (ejbMethods[i].isBridge()) {
                bridgeMethods[i] = ejbMethods[i];
                ejbMethods[i] = byteCodeMetaData.getBridgeMethodTarget(ejbMethods[i]); // F61004.1

                if (ejbMethods[i] == null) {
                    //FFDCFilter.processException(e, CLASS_NAME + ".initializeBeanMethodMD", "2452", this);

                    // Note, when looking for target of a bridge method, using the toGenericString() method
                    // on the Method object from interface class returns a method signature that contains
                    // the generic types used as method parameters and return types.
                    String genericInterfaceMethod = beanMethods[i].toGenericString();

                    // CNTR8992E: This message is an English-only Error message: {0}.
                    String errorMessage = "Failure occured while attempting to find the target of a bridge method for the "
                                          + bmd.enterpriseBeanName + " enterprise bean in the " + bmd._moduleMetaData.ivName + " module."
                                          + " The java compiler generates a synthetic bridge method in the enterprise bean class for the generic "
                                          + genericInterfaceMethod + " method of inteface " + beanMethods[i].getDeclaringClass().getName();
                    Tr.error(tc, "ENGLISH_ONLY_ERROR_MESSAGE_CNTR8992E", errorMessage);
                    throw new EJBConfigurationException(errorMessage, byteCodeMetaData.getScanException());
                }
            }

            // Initialize isolationAttrs to TX_NONE (unspecified) by default.  Portability layer
            // will do the correct thing to replace Tx_None with the proper value, depending on DB type.
            isolationAttrs[i] = java.sql.Connection.TRANSACTION_NONE;

        } // for all bean methods

        // Special case the remove method of the EJBObject interface
        if (addRemove) {
            int lastMethodIndex = numMethods - 1;
            methodNames[lastMethodIndex] = "remove";
            methodParamTypes[lastMethodIndex] = NO_PARAMS;
            methodSignatures[lastMethodIndex] = "";
            jdiMethodSignatures[lastMethodIndex] = "()V";
            ejbMethods[lastMethodIndex] = null; // internal method   366845.9
            readOnlyAttrs[lastMethodIndex] = false;
            isolationAttrs[lastMethodIndex] = java.sql.Connection.TRANSACTION_NONE;

            // remove is an 'internal' method, and the tx attribute cannot be set
            // via annotations, so set the default here; XML may still override.
            if (bmd.usesBeanManagedTx)
                transactionAttrs[lastMethodIndex] = TransactionAttribute.TX_BEAN_MANAGED;
            else
                transactionAttrs[lastMethodIndex] = (isEntity) ? TransactionAttribute.TX_REQUIRED : TransactionAttribute.TX_NOT_SUPPORTED;

            // LIDB441.5
            if (bmd.usesBeanManagedAS) // d127328 - do this independent of ActivityPolicyKind
                activitySessionAttrs[lastMethodIndex] = ActivitySessionAttribute.AS_BEAN_MANAGED;
            else
                activitySessionAttrs[lastMethodIndex] = ActivitySessionAttribute.AS_SUPPORTS;
        }

        //Process the Security MetaData       //366845.11

        // Process methods to exclude via security from WCCM. If there is no excludeList,
        // then any methods to exclude must have been specified via annotations.  //PK93643
        if (excludeList != null) {
            // Process all method-permission elements.   //d366845.11.1
            MethodAttribUtils.getXMLMethodsDenied(denyAll,
                                                  metaMethodElementKind,
                                                  methodNames,
                                                  methodParamTypes,
                                                  excludeList,
                                                  bmd); //PK93643

            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "denyAll = " + Arrays.toString(denyAll));
            }
        }

        // Process all Security Attributes from WCCM. If there is no excludeList,
        // then any security attributes must have been specified via annotations.  //PK93643
        if (securityList != null) { //PK93643
            MethodAttribUtils.getXMLPermissions(roleArrays,
                                                permitAll,
                                                denyAll,
                                                metaMethodElementKind,
                                                methodNames,
                                                methodParamTypes,
                                                securityList,
                                                bmd); //PK93643
        }

        // If not marked MetaDataComplete then look for
        // security annotation information.    //366845.11.2
        if (!bmd.metadataComplete) {
            MethodAttribUtils.getAnnotationsForSecurity(ejbMethods,
                                                        roleArrays,
                                                        denyAll,
                                                        permitAll);
        }

        // F743-1751 - Factored out transaction logic.
        initializeBeanMethodTransactionAttributes(isEntity,
                                                  addRemove,
                                                  methodInterface,
                                                  ejbMethods,
                                                  entityNoTxAttrMethods,
                                                  entityNoTxAttrMethodSignatures,
                                                  transactionList,
                                                  activitySessionList,
                                                  methodNames,
                                                  methodParamTypes,
                                                  methodSignatures,
                                                  transactionAttrs,
                                                  activitySessionAttrs,
                                                  bmd);

        if (bmd.ivModuleVersion <= BeanMetaData.J2EE_EJB_VERSION_1_1 ||
            bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X) {
            //Need Isolation level for
            //1) All beans in Java EE 1.2 app
            //2) CMP11 beans ONLY in Java EE 1.3 app
            getIsolationLevels(isolationAttrs, // F743-18775
                               metaMethodElementKind,
                               methodNames,
                               methodParamTypes,
                               isoLevelList,
                               wccmEnterpriseBean);

            if (bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X) {
                getReadOnlyAttributes(readOnlyAttrs, // F743-18775
                                      metaMethodElementKind,
                                      methodNames,
                                      methodParamTypes,
                                      accessIntentList,
                                      wccmEnterpriseBean);

                readOnlyAttrs[numMethods - 1] = false; //EJBObject.remove() is never read-only, so force it to be read-write
                                                       //in case getReadOnlyAttributes set it to true (e.g., from a method wild-card).
            }
        }

        //F743-4582:
        // Process all Asynchronous methods configured via xml. If there is no EnterpriseBean,
        // assume this is an annotations only configuration scenario.
        if (wccmEnterpriseBean != null) {

            // 621123 - If any method of the bean is asychronous, set the flag in bmd.  Use bit-wise
            //          OR so that it is not unset by mistake because either XML or annotations happened
            //          to not define an async method for this bean.
            bmd.ivHasAsynchMethod |= MethodAttribUtils.getXMLAsynchronousMethods(asynchMethodFlags,
                                                                                 methodInterface,
                                                                                 methodNames,
                                                                                 methodParamTypes,
                                                                                 wccmEnterpriseBean);
        }

        // Next, process all @Asynchronous annotations unless the bean's configuration
        // was completely done via xml.
        if (!bmd.metadataComplete) {

            // 621123 - If any method of the bean is asychronous, set the flag in bmd.  Use bit-wise
            //          OR so that it is not unset by mistake because either XML or annotations happened
            //          to not define an async method for this bean.
            bmd.ivHasAsynchMethod |= MethodAttribUtils.getAsynchronousMethods(ejbMethods,
                                                                              asynchMethodFlags,
                                                                              methodInterface); //d599046
        }

        // F743-1752.1 start
        // Get Lock and AccessTimeout metadata if EJB is a Singleton session bean and
        // it uses container managed concurrency control.
        boolean singletonWithContainerManagedConcurrency = bmd.isSingletonSessionBean()
                                                           && (bmd.ivSingletonUsesBeanManagedConcurrency == false);

        if (singletonWithContainerManagedConcurrency) {
            // The default AccessTimeout for Singletons is to wait forever (-1),
            // unless overridden by a system property.                      d704504
            long defaultTimeout = DefaultSessionAccessTimeout;

            // Initialize the settings to an invalid value so annotation processing
            // can tell when the value has been set by XML processing. F743-21028.5
            Arrays.fill(accessTimeout, -2);

            // F743-7027 start
            // Process all @Lock/@AccessAsynchronous methods configured via xml. If there is no EnterpriseBean,
            // assume this is an annotations only configuration scenario.
            if (wccmEnterpriseBean != null) {
                Session sessionBean = (Session) wccmEnterpriseBean;
                MethodAttribUtils.getXMLCMCLockType(lockType, ejbMethods, sessionBean);
                MethodAttribUtils.getXMLCMCLockAccessTimeout(accessTimeout, ejbMethods, sessionBean);
            } // F743-7027 end

            // Next, process all all @Lock/@AccessTimeout annotations. Note, we need to
            // call even if metadata complete is true since we want default values to be
            // set if nothing was in the ejb-jar.xml for lock and access timeout.
            MethodAttribUtils.getAnnotationCMCLockType(lockType, ejbMethods, bmd);
            MethodAttribUtils.getAnnotationCMCLockAccessTimeout(accessTimeout, ejbMethods,
                                                                defaultTimeout, bmd.metadataComplete);
        } // F743-1752.1 end

        // Also get AccessTimeout metadata for Stateful session beans.  F743-22462
        else if (bmd.isStatefulSessionBean()) {
            // The default AccessTimeout starting in EE6 is to wait forever (-1),
            // though this may be overrident with a system property.
            // Previously, concurrent access of Stateful beans was not supported
            // and resulted in an exception. For compatibility, that would be the
            // same as an AccessTimeout of 0 (don't wait).
            long defaultTimeout = EE5Compatibility ? 0 : DefaultSessionAccessTimeout; // d704504

            // Initialize the settings to an invalid value so annotation processing
            // can tell when the value has been set by XML processing.
            Arrays.fill(accessTimeout, -2);

            // Process all methods configured via xml. If there is no EnterpriseBean,
            // assume this is an annotations only configuration scenario.
            if (wccmEnterpriseBean != null) {
                MethodAttribUtils.getXMLCMCLockAccessTimeout(accessTimeout, ejbMethods,
                                                             (Session) wccmEnterpriseBean);
            }

            // Next, process all @AccessTimeout annotations. Note, we need to
            // call even if metadata complete is true since we want default values to be
            // set if nothing was in the ejb-jar.xml for lock and access timeout.
            MethodAttribUtils.getAnnotationCMCLockAccessTimeout(accessTimeout, ejbMethods,
                                                                defaultTimeout, bmd.metadataComplete);
        }

        // interfaceClass is null when there is no home interface (business interface only)
        int minInterfaceIndex = interfaceClass == null ? 0 : -1;
        int numBusinessInterfaces = businessInterfaceClasses == null ? 0 : businessInterfaceClasses.length;
        int slotSize = bmd.container.getEJBRuntime().getMetaDataSlotSize(MethodMetaData.class);

        // Finally, build up method info objects containing all the configuratin attributes
        // for each ejb method.

        for (int i = 0; i < numMethods; i++) {
            String[] rolesAllowed = null;
            if (roleArrays[i] != null && !roleArrays[i].isEmpty()) {
                rolesAllowed = new String[roleArrays[i].size()];
                rolesAllowed = roleArrays[i].toArray(new String[0]); //d366845.11.2
            }

            if (isTraceOn && tc.isDebugEnabled()) {
                String ms = (i < beanMethods.length) ? MethodAttribUtils.methodSignature(ejbMethods[i]) : methodNames[i] + MethodAttribUtils.METHOD_ARGLIST_SEP
                                                                                                          + methodSignatures[i]; //d494984.1
                Tr.debug(tc, "Createing MethodInfo:  methodName = " +
                             methodNames[i] + " methodSignature = " +
                             ms + " PermitAll = " + //d494984.1
                             permitAll[i] + " DenyAll = " + denyAll[i]);
                Tr.debug(tc, "RolesAllowed = " + Arrays.toString(rolesAllowed));
            }

            Class<?>[][] declaredExceptions = null;
            Class<?>[] declaredExceptionsComp = null; // d734957

            if (ejbMethods[i] != null) {
                Class<?> classReturnType = ejbMethods[i].getReturnType();

                if (asynchMethodFlags[i]) {
                    // If the current method is an asynchronous method, and the bean uses CMT transactions, but has an incorrect
                    // TX attribute configured, we can stop now and log an error.  Asynch methods can only have TX attributes of
                    // type TX_REQUIRED, TX_REQUIRES_NEW, or TX_NOT_SUPPORTED configured.
                    if (!bmd.usesBeanManagedTx &&
                        transactionAttrs[i] != TransactionAttribute.TX_REQUIRED &&
                        transactionAttrs[i] != TransactionAttribute.TX_REQUIRES_NEW &&
                        transactionAttrs[i] != TransactionAttribute.TX_NOT_SUPPORTED) {
                        Tr.error(tc, "INVALID_TX_ATTRIBUTE_FOR_ASYNCH_METHOD_CNTR0187E",
                                 new Object[] { methodNames[i], bmd.j2eeName.getComponent(), bmd.j2eeName.getModule(),
                                                bmd.j2eeName.getApplication(), transactionAttrs[i] });
                        throw new EJBConfigurationException("The " + methodNames[i] +
                                                            " method on the " + bmd.j2eeName.getComponent() +
                                                            " bean in the " + bmd.j2eeName.getModule() +
                                                            " module of the " + bmd.j2eeName.getApplication() +
                                                            " application has a " + transactionAttrs[i] +
                                                            " transaction attribute configured.  Asynchronous methods" +
                                                            " only support transaction attributes of type" +
                                                            " TX_REQUIRED, TX_REQUIRES_NEW, or TX_NOT_SUPPORTED.");
                    }

                    // F743-4633
                    // Ensure for async methods the return type is void or Future<V> otherwise throw EJBConfigurationException.
                    if (classReturnType != Void.TYPE && classReturnType != Future.class) {
                        Tr.error(tc, "ASYNC_METHOD_MUST_RETURN_VOID_OR_FUTURE_CNTR0206E",
                                 new Object[] { methodNames[i], bmd.j2eeName.getComponent(), classReturnType.getName() });
                        throw new EJBConfigurationException("CNTR0206E: The " + methodNames[i]
                                                            + " asynchronous method on the " + bmd.j2eeName.getComponent()
                                                            + " bean has a return type of " + classReturnType.getName()
                                                            + ".  A return type of void or Future<V> is required for asynchronous methods.");
                    }

                    declaredExceptions = new Class<?>[numBusinessInterfaces][];
                }

                // Ensure that all the signatures of the business methods declared
                // on each interface class match the corresponding signatures of
                // the methods on the bean class.
                // d668039 - Also check component interfaces by using a special
                // index of -1.
                for (int interfaceIndex = minInterfaceIndex; interfaceIndex < numBusinessInterfaces; interfaceIndex++) {
                    Class<?> klass = interfaceIndex >= 0 ? businessInterfaceClasses[interfaceIndex] : interfaceClass; // d668039

                    Method method;
                    try {
                        method = klass.getMethod(methodNames[i], methodParamTypes[i]);
                    } catch (NoSuchMethodException ex) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "method " + beanMethods[i] + " not found on " + klass.getName());
                        continue;
                    }

                    // F743-16193 d644502
                    // Ensure the bean return type and the interface return type match
                    Class<?> interfaceReturnType = method.getReturnType();
                    if (!classReturnType.equals(interfaceReturnType)) {
                        // d645058
                        // For asynchronous methods this will be an error condition.
                        // To be compatible with previous versions when this return type checking was not done,
                        // only a warning will be issued when the customer has setup for validation logging.
                        if (asynchMethodFlags[i]) {
                            Tr.error(tc, "INCOMPATIBLE_RETURN_TYPES_CNTR0321E",
                                     new Object[] { classReturnType.getName(), methodNames[i], bmd.j2eeName.getComponent(),
                                                    interfaceReturnType.getName(), klass.getName() });
                            throw new EJBConfigurationException("CNTR0321E: The " + classReturnType.getName()
                                                                + " return type for the " + methodNames[i]
                                                                + " method of the " + bmd.j2eeName.getComponent()
                                                                + " enterprise bean does not match the " + interfaceReturnType.getName()
                                                                + " return type for the corresponding method on the " + klass.getName()
                                                                + " interface.");
                        } else if (isValidationLoggable(checkAppConfigCustom)) {
                            Tr.warning(tc, "INCOMPATIBLE_RETURN_TYPES_CNTR0322W",
                                       new Object[] { classReturnType.getName(), methodNames[i], bmd.j2eeName.getComponent(),
                                                      interfaceReturnType.getName(), klass.getName() });
                            if (isValidationFailable(checkAppConfigCustom)) {
                                throw new EJBConfigurationException("The " + classReturnType.getName() +
                                                                    " return type for the " + methodNames[i] +
                                                                    " method of the " + bmd.j2eeName.getComponent() +
                                                                    " enterprise bean is not compatible with the " + interfaceReturnType.getName() +
                                                                    " return type for the corresponding method on the " + klass.getName() +
                                                                    " interface.");
                            }
                        }
                    }

                    if (asynchMethodFlags[i]) {
                        Class<?>[] interfaceDeclaredExceptions = DeploymentUtil.getCheckedExceptions(
                                                                                                     method, Remote.class.isAssignableFrom(klass),
                                                                                                     DeploymentUtil.DeploymentTarget.WRAPPER); // d660332
                        if (interfaceIndex >= 0) {
                            declaredExceptions[interfaceIndex] = interfaceDeclaredExceptions;
                        } else {
                            declaredExceptionsComp = interfaceDeclaredExceptions;
                        }
                        if (classReturnType == Void.TYPE && interfaceDeclaredExceptions.length != 0) {
                            // Pass all the required params to the error message.  F743-4633
                            String exceptionList = "";
                            for (Class<?> exception : interfaceDeclaredExceptions) {
                                exceptionList += exception.getName() + ", ";
                            }

                            Tr.error(tc, "INVALID_APPLICATION_EXCEPTION_FOR_VOID_ASYNCH_METHOD_CNTR0202E",
                                     new Object[] { methodNames[i], bmd.j2eeName.getComponent(), exceptionList });
                            throw new EJBConfigurationException("CNTR0202E: The " + methodNames[i]
                                                                + " asynchronous method on the " + bmd.j2eeName.getComponent()
                                                                + " bean has a return type of void and has the " + exceptionList
                                                                + " application exception(s) on its throws clause.");
                        }
                    }
                }
            }

            // build the method info object of this method
            EJBMethodInfoImpl methodInfoImpl = bmd.createEJBMethodInfoImpl(slotSize);
            String methodSig = i < beanMethods.length ? MethodAttribUtils.methodSignature(beanMethods[i]) : methodNames[i] + MethodAttribUtils.METHOD_ARGLIST_SEP
                                                                                                            + methodSignatures[i];
            methodInfoImpl.initializeInstanceData(methodSig,
                                                  methodNames[i],
                                                  bmd,
                                                  methodInterface,
                                                  transactionAttrs[i],
                                                  asynchMethodFlags[i]);
            methodInfoImpl.setMethodDescriptor(jdiMethodSignatures[i]);
            methodInfoImpl.setIsolationLevel(isolationAttrs[i]);
            methodInfoImpl.setActivitySessionAttribute(activitySessionAttrs[i]);
            methodInfoImpl.setReadOnly(readOnlyAttrs[i]);
            methodInfoImpl.setSecurityPolicy(denyAll[i], permitAll[i], rolesAllowed);

            methodInfoImpl.setMethod(ejbMethods[i]); // 366845.9 // F743-1752.1
            methodInfoImpl.setBridgeMethod(bridgeMethods[i]); // d517824 // F743-1752.1

            // If this is a singleton session bean that uses container managed concurrency
            // control, then set the LockType and AccessTimeout for this method.
            if (singletonWithContainerManagedConcurrency) {
                methodInfoImpl.setCMLockType(lockType[i]); // F743-1752.1
                methodInfoImpl.setCMLockAccessTimeout(accessTimeout[i]); // F743-1752.1
            } else if (bmd.isStatefulSessionBean()) {
                methodInfoImpl.setCMLockAccessTimeout(accessTimeout[i]); // F743-22462
            }

            methodInfoImpl.setDeclaredExceptions(declaredExceptions); // F743-761
            methodInfoImpl.setDeclaredExceptionsComp(declaredExceptionsComp); // d734957

            methodInfos[i] = methodInfoImpl; // F743-1752.1
        }

        if (addRemove) {
            methodInfos[numMethods - 1].setComponentRemove(true);

            // EJB 2.1 Stateful remove methods (EJBObject/EJBLocalObject) must
            // throw a RemoveException if the bean is in use, so set concurrency
            // timout to 0 (no waiting).                                    d704504
            if (bmd.isStatefulSessionBean()) {
                methodInfos[numMethods - 1].setCMLockAccessTimeout(0);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "initializeBeanMethodMetadata");

        return new MethodDataWrapper(methodNames, methodInfos, isolationAttrs, readOnlyAttrs);

    } //initializeBeanMethodMD

    // F743-1751
    /**
     * Initialize lifecycleInterceptorMethodInfos and
     * lifecycleInterceptorMethodNames fields in the specified BeanMetaData.
     */
    private void initializeLifecycleInterceptorMethodMD(Method[] ejbMethods,
                                                        List<ContainerTransaction> transactionList,
                                                        List<ActivitySessionMethod> activitySessionList,
                                                        BeanMetaData bmd) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "initializeLifecycleInterceptorMethodMD", (Object[]) ejbMethods);

        final int numMethods = LifecycleInterceptorWrapper.NUM_METHODS;
        String[] methodNames = new String[numMethods];
        Class<?>[][] methodParamTypes = new Class<?>[numMethods][];
        String[] methodSignatures = new String[numMethods];
        TransactionAttribute[] transactionAttrs = new TransactionAttribute[numMethods];
        ActivitySessionAttribute[] activitySessionAttrs = new ActivitySessionAttribute[numMethods];

        for (int i = 0; i < numMethods; i++) {
            if (ejbMethods[i] == null) {
                methodNames[i] = LifecycleInterceptorWrapper.METHOD_NAMES[i];
                methodParamTypes[i] = LifecycleInterceptorWrapper.METHOD_PARAM_TYPES[i];
                methodSignatures[i] = LifecycleInterceptorWrapper.METHOD_SIGNATURES[i];
                // The spec states that in order to specify a transaction attribute
                // for lifecycle interceptor methods, the method must be declared
                // on the bean class.  Afterwards, it states that if a transaction
                // attribute is not specified, then it must be REQUIRED.  This
                // implies that if the bean class specifies a class-level
                // transaction attribute of NOT_SUPPORT but does not specify a
                // lifecycle interceptor method, we still use REQUIRED for the
                // other lifecycle interceptors.
                //
                // By specifying the default transaction attribute here, we avoid
                // NPE in getAnnotationCMTransactions by using null ejbMethods[i].
                transactionAttrs[i] = bmd.type == InternalConstants.TYPE_STATEFUL_SESSION ? TransactionAttribute.TX_NOT_SUPPORTED : TransactionAttribute.TX_REQUIRED;
            } else {
                methodNames[i] = ejbMethods[i].getName();
                methodParamTypes[i] = ejbMethods[i].getParameterTypes();
                methodSignatures[i] = MethodAttribUtils.methodSignatureOnly(ejbMethods[i]);
            }
        }

        initializeBeanMethodTransactionAttributes(false,
                                                  false,
                                                  MethodInterface.LIFECYCLE_INTERCEPTOR,
                                                  ejbMethods,
                                                  null,
                                                  null,
                                                  transactionList,
                                                  activitySessionList,
                                                  methodNames,
                                                  methodParamTypes,
                                                  methodSignatures,
                                                  transactionAttrs,
                                                  activitySessionAttrs,
                                                  bmd);

        EJBMethodInfoImpl[] methodInfos = new EJBMethodInfoImpl[numMethods];

        int slotSize = bmd.container.getEJBRuntime().getMetaDataSlotSize(MethodMetaData.class);
        for (int i = 0; i < numMethods; i++) {
            String methodSig = ejbMethods[i] == null ? methodNames[i] + MethodAttribUtils.METHOD_ARGLIST_SEP
                                                       + methodSignatures[i] : MethodAttribUtils.methodSignature(ejbMethods[i]);
            String jdiMethodSig = ejbMethods[i] == null ? LifecycleInterceptorWrapper.METHOD_JDI_SIGNATURES[i] : MethodAttribUtils.jdiMethodSignature(ejbMethods[i]);

            EJBMethodInfoImpl methodInfo = bmd.createEJBMethodInfoImpl(slotSize);
            methodInfo.initializeInstanceData(methodSig, methodNames[i], bmd, MethodInterface.LIFECYCLE_INTERCEPTOR, transactionAttrs[i], false);
            methodInfo.setMethodDescriptor(jdiMethodSig);
            methodInfo.setActivitySessionAttribute(activitySessionAttrs[i]);
            methodInfos[i] = methodInfo;
        }

        bmd.lifecycleInterceptorMethodInfos = methodInfos;
        bmd.lifecycleInterceptorMethodNames = methodNames;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "initializeLifecycleInterceptorMethodMD");
    }

    /**
     * Determine the Home's method level metadata
     *
     * @param isEntity
     * @param methodInterface
     * @param homeMethods
     * @param sessionHomeNoTxAttrMethods
     * @param sessionHomeNoTxAttrMethodSignatures
     * @param entityHomeNoTxAttrMethods
     * @param entityHomeNoTxAttrMethodSignatures
     * @param accessIntentList
     * @param isoLevelList
     * @param transactionList
     * @param securityList
     * @param excludeList
     * @param activitySessionList
     * @param cdo
     * @param bmd
     * @return the Home's method level meta data.
     */

    private MethodDataWrapper initializeHomeMethodMD(boolean isEntity,
                                                     MethodInterface methodInterface,
                                                     Method homeMethods[],
                                                     String[] sessionHomeNoTxAttrMethods,
                                                     String[] sessionHomeNoTxAttrMethodSignatures, //PQ63130
                                                     String[] entityHomeNoTxAttrMethods,
                                                     String[] entityHomeNoTxAttrMethodSignatures, //PQ63130
                                                     List<?> accessIntentList,
                                                     List<?> isoLevelList,
                                                     List<ContainerTransaction> transactionList,
                                                     List<MethodPermission> securityList,
                                                     ExcludeList excludeList,
                                                     List<ActivitySessionMethod> activitySessionList,
                                                     BeanMetaData bmd) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "initializeHomeMethodMetadata");

        int metaMethodElementKind = methodInterface.getValue(); // d162441

        EJBMethodInfoImpl[] homeMethodInfos = new EJBMethodInfoImpl[homeMethods.length];
        int[] homeIsolationAttrs = new int[homeMethods.length];
        boolean[] homeReadOnlyAttrs = new boolean[homeMethods.length];//94756

        String[] homeMethodNames = new String[homeMethods.length];
        Class<?>[][] homeMethodParamTypes = new Class<?>[homeMethods.length][];
        String[] homeMethodSignatures = new String[homeMethods.length];
        String[] jdiHomeMethodSignatures = new String[homeMethods.length];

        boolean[] homeDenyAll = new boolean[homeMethods.length]; //d366845.11.1
        boolean[] homePermitAll = new boolean[homeMethods.length]; //d366845.11.1

        @SuppressWarnings("unchecked")
        ArrayList<String[]>[] homeRoleArrays = new ArrayList[homeMethods.length]; //d366845.11.1

        TransactionAttribute[] homeTransactionAttrs = new TransactionAttribute[homeMethods.length];
        ActivitySessionAttribute[] homeActivitySessionAttrs = new ActivitySessionAttribute[homeMethods.length]; // LIDB441.5

        for (int i = 0; i < homeMethods.length; i++) {
            homeMethodNames[i] = homeMethods[i].getName();
            homeMethodParamTypes[i] = homeMethods[i].getParameterTypes();
            homeMethodSignatures[i] = MethodAttribUtils.methodSignatureOnly(homeMethods[i]);
            jdiHomeMethodSignatures[i] = MethodAttribUtils.jdiMethodSignature(homeMethods[i]);

            // Initialize isolationAttrs to TX_NONE (unspecified) by default.  Portability layer
            // will do the correct thing to replace Tx_None with the proper value, depending on DB type.
            homeIsolationAttrs[i] = java.sql.Connection.TRANSACTION_NONE;

            // For Bean-managed Tx (session beans only), initialize home method Tx attrs
            // to TX_BEAN_MANAGED.  For other cases, initialize to TX_REQUIRED.
            // Home methods are considered 'internal', and the tx attribute cannot be set
            // via annotations, so set the default here; XML may still override.
            homeTransactionAttrs[i] = (bmd.usesBeanManagedTx) ? TransactionAttribute.TX_BEAN_MANAGED : TransactionAttribute.TX_REQUIRED;

            // LIDB441.5 - For Bean-managed AS (session beans only), initialize method AS attrs
            // to AS_BEAN_MANAGED.  For other cases, initialize to AS_UNKNOWN.
            homeActivitySessionAttrs[i] = (bmd.usesBeanManagedAS) ? ActivitySessionAttribute.AS_BEAN_MANAGED : ActivitySessionAttribute.AS_UNKNOWN;
        }

        //Process the Security MetaData       //366845.11.1

        // Process methods to exclude via security from WCCM. If there is no excludeList,
        // then any methods to exclude must have been specified via annotations.  //PK93643
        if (excludeList != null) {
            // Process all exclude-list elements.
            MethodAttribUtils.getXMLMethodsDenied(homeDenyAll,
                                                  metaMethodElementKind,
                                                  homeMethodNames,
                                                  homeMethodParamTypes,
                                                  excludeList,
                                                  bmd); //PK93643
        }

        // Process all Security Attributes from WCCM. If there is no excludeList,
        // then any security attributes must have been specified via annotations.  //PK93643
        if (securityList != null) {
            // Process all method-permission elements.
            MethodAttribUtils.getXMLPermissions(homeRoleArrays,
                                                homePermitAll,
                                                homeDenyAll,
                                                metaMethodElementKind,
                                                homeMethodNames,
                                                homeMethodParamTypes,
                                                securityList,
                                                bmd); //PK93643
        }

        // FYI... Homes have no annotations

        // Only get and check method-level Tx attributes if container is managing Tx
        if (!bmd.usesBeanManagedTx) {
            // Process all Transaction Attributes from WCCM. If there is no transactionList,
            // assume this is an annotations only configuration scenario.
            if (transactionList != null) {
                MethodAttribUtils.getXMLCMTransactions(homeTransactionAttrs,
                                                       metaMethodElementKind,
                                                       homeMethodNames,
                                                       homeMethodParamTypes,
                                                       transactionList,
                                                       bmd); //PK93643
            }

            // Note that home methods may not have annotations, so no need to call
            // the annotation processing code here... defaults were set above.

            // If bean uses container-managed Tx, override setting to TX_NOT_SUPPORTED for:
            // - All Home interface methods of a session bean
            // - getEJBMetaData and getHomeHandle of the Home interface of an entity bean
            if (isEntity) {
                MethodAttribUtils.checkTxAttrs(homeTransactionAttrs,
                                               homeMethodNames,
                                               homeMethodSignatures, //PQ63130
                                               entityHomeNoTxAttrMethods,
                                               entityHomeNoTxAttrMethodSignatures, //PQ63130
                                               TransactionAttribute.TX_NOT_SUPPORTED);
            } else { // SessionBean
                MethodAttribUtils.checkTxAttrs(homeTransactionAttrs,
                                               homeMethodNames,
                                               homeMethodSignatures, //PQ63130
                                               sessionHomeNoTxAttrMethods,
                                               sessionHomeNoTxAttrMethodSignatures, //PQ63130
                                               TransactionAttribute.TX_NOT_SUPPORTED);
            }
        } // if !usesBeanManagedTx

        // Only get method-level Activity Session attributes if container is managing AS
        if (!bmd.usesBeanManagedAS) {
            MethodAttribUtils.getActivitySessions(homeActivitySessionAttrs,
                                                  metaMethodElementKind,
                                                  homeMethodNames,
                                                  homeMethodParamTypes,
                                                  activitySessionList,
                                                  bmd.enterpriseBeanName,
                                                  bmd.usesBeanManagedAS); // LIDB441.5
        } // if !usesBeanManagedAS

        if (bmd.ivModuleVersion <= BeanMetaData.J2EE_EJB_VERSION_1_1 ||
            bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X) {
            //Need Isolation level for
            //1) All beans in Java EE 1.2 app
            //2) CMP11 beans ONLY in Java EE 1.3 app
            // Get user-specified isolation level settings
            getIsolationLevels(homeIsolationAttrs, // F743-18775
                               metaMethodElementKind,
                               homeMethodNames,
                               homeMethodParamTypes,
                               isoLevelList,
                               bmd.wccm.enterpriseBean);

            if (bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X) {
                // Get user-specified access intent settings
                getReadOnlyAttributes(homeReadOnlyAttrs, // F743-18775
                                      metaMethodElementKind,
                                      homeMethodNames,
                                      homeMethodParamTypes,
                                      accessIntentList,
                                      bmd.wccm.enterpriseBean);
            }
        }

        // If the user specified the FbpkAlwaysReadOnly system property, set the readonly attribute on
        // findByPrimaryKey methods to True.  This is to emulate the default WS3.5 behavior for
        // these methods, which is needed in some cases (4.0 default is false).  This behavior is
        // required when the FOR UPDATE clause on the fBPK SQL query is not wanted.

        // Currently it's an all-or-nothing proposition (all EntityBeans or none); this may be enhanced
        // in the future to allow setting on specific bean types.
        // Property access & default now handled by ContainerProperties.    391302

        boolean fbpkReadOnlyOverride = false;

        if (FbpkAlwaysReadOnly) {
            fbpkReadOnlyOverride = true;
            // If the user overrode all beans, only show the message once in the console (rather than
            // for every bean type)
            if (!BeanMetaData.fbpkReadOnlyOverrideAllBeans) {
                BeanMetaData.fbpkReadOnlyOverrideAllBeans = true;
                Tr.audit(tc, "FBPK_READONLY_OVERRIDE_ALL_CNTR0061I");
            }
        }

        // d112604.1 begin

        boolean cfHonorAccessIntent = false; // default is disable this unless customer
        String cfMethodSignatures[][] = null;
        if (bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X) {
            // custom finder support only applies to 1_x beans
            if (!BeanMetaData.allowCustomFinderSQLForUpdateALLBeans) { // static initializer, avoid this if "all" is specified
                String allowCustomFinderSQLForUpdateStr = AllowCustomFinderSQLForUpdate;
                if (isTraceOn && tc.isDebugEnabled()) {
                    if (allowCustomFinderSQLForUpdateStr != null) {
                        Tr.debug(tc, ContainerConfigConstants.allowCustomFinderSQLForUpdate + " Value : " + allowCustomFinderSQLForUpdateStr);
                    } else {
                        Tr.debug(tc, ContainerConfigConstants.allowCustomFinderSQLForUpdate + " Value : (null)");
                    }
                }

                if (allowCustomFinderSQLForUpdateStr != null) {
                    if (allowCustomFinderSQLForUpdateStr.equalsIgnoreCase("all")) {
                        BeanMetaData.allowCustomFinderSQLForUpdateALLBeans = true;
                        bmd.allowCustomFinderSQLForUpdateThisBean = true;
                    } else {
                        StringTokenizer parser = new StringTokenizer(allowCustomFinderSQLForUpdateStr, ":");
                        int numTokens = parser.countTokens();
                        for (int i = 0; i < numTokens; i++) {
                            String compString = parser.nextToken();
                            /*
                             * if (isTraceOn && tc.isDebugEnabled()) {
                             * Tr.debug(tc, " Compare ["+ compString + "," + enterpriseBeanClassName + "]");
                             * }
                             */
                            if (compString.equals(bmd.enterpriseBeanClassName)) {
                                bmd.allowCustomFinderSQLForUpdateThisBean = true;
                                if (isTraceOn && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Custom Finder SQL For Update Enabled for : " +
                                                 bmd.enterpriseBeanClassName);
                                }
                                break;
                            } // if
                        } // for
                    } // else
                } // processing string complete
            } else { // turn on for this bean if all have enabled across all beans (e.g. a bean before this has set static initilizer)
                bmd.allowCustomFinderSQLForUpdateThisBean = true;
            }

            // This support is to be compatiable with 390 applications that expect this
            // behavior to be enabled. The value is set to true to disable, negate to
            // clean up understanding during code flow.
            bmd.allowWAS390CustomFinderAtBeanLevel = !getWAS390CustomFinderBeanLevel(ContainerConfigConstants.allowWAS390CustomFinderAtBeanLevelStr, bmd).booleanValue();

            // determine if this bean has any per method overrides
            String envCustomFinderMethodsStr = getCustomFinderSignatures(ContainerConfigConstants.envCustomFinderMethods, bmd);

            if (envCustomFinderMethodsStr != null) {
                bmd.allowCustomFinderSQLForUpdateMethodLevel = true;
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Method Level Finders Defined [" + envCustomFinderMethodsStr + "]");
                }
                // initialize this vector
                cfMethodSignatures = bmd.getMethodLevelCustomFinderMethodSignatures(envCustomFinderMethodsStr);
                /*
                 * if (isTraceOn && tc.isDebugEnabled()) {
                 * for(int lcv=0;lcv<cfMethodSignatures.length;lcv++){
                 * Tr.debug(tc, "Returned CF Method Name [" + lcv + "]: " + cfMethodSignatures[lcv][CF_METHOD_NAME_OFFSET]);
                 * if (cfMethodSignatures[lcv][CF_METHOD_SIG_OFFSET]!=null) {
                 * Tr.debug(tc, "Returned CF Method Signature[" + lcv + "]: " + cfMethodSignatures[lcv][CF_METHOD_SIG_OFFSET]);
                 * }
                 * }
                 * } // if debug enabled
                 */
            }

            cfHonorAccessIntent = false; // default is disable this unless customer
            // specifies CMP 11 Custom Finders Access Intent
            // should be honored

            if (bmd.allowCustomFinderSQLForUpdateThisBean || BeanMetaData.allowCustomFinderSQLForUpdateALLBeans) {
                if (bmd.allowWAS390CustomFinderAtBeanLevel) { // 390 previous config can turn off a bean
                    cfHonorAccessIntent = true; // this will prove true unless detect a 390 bean level shut off
                    Tr.audit(tc, "CUSTOMFINDER_SQLFORUPDATE_CNTR0078I", new Object[] { bmd.enterpriseBeanClassName });
                    // honoring this support regardless of other
                    // of other settings
                } else {
                    cfHonorAccessIntent = false; // note, a method level override can still control this

                }
            }

            // Custom Finder Access Intents are not honored when optimistic
            // concurrency control is in effect. This avoids server and
            // bean level controls, also need to override method level
            // below in terms of optimisticConcurrencyControl.

            if (bmd.optimisticConcurrencyControl && cfHonorAccessIntent) {
                cfHonorAccessIntent = false;
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Optimistic Conncurrency Enabled, " + bmd.enterpriseBeanClassName + " Custom Finders will be read-only");
            }

            // Custom finder support not applicable when Option A in effect.

            if (bmd.optionACommitOption && cfHonorAccessIntent) {
                cfHonorAccessIntent = false;
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Option A Enabled, " + bmd.enterpriseBeanClassName + " Custom Finders will be read-only");
            }
        } // if a CMP 1 X bean

        // d112604.1 end

        int slotSize = bmd.container.getEJBRuntime().getMetaDataSlotSize(MethodMetaData.class);

        for (int i = 0; i < homeMethods.length; i++) {

            // d112604.1 begin
            boolean finderDetect = false;
            boolean cfForUpdateSupported = false;
            boolean fbpkDetect = false;
            boolean isFBPK = false;

            // Need custom finders, FBPK should be checked subsequently
            // Spec calls out that home finders must stat with "find", take minor perf
            // hit at this point...

            finderDetect = homeMethodNames[i].startsWith("find"); // d360576

            // Four cases override method level enablement: cmp1x bean, optimstic concurrency enabled,
            // Option A in effect and if access intent is read only.

            // a) access intent needs to be update before any custom finder access intent
            //    support is enforced

            // b) This will override method level checking

            if (!homeReadOnlyAttrs[i] && // must be a r/w method, r/o only method opts
                !bmd.optimisticConcurrencyControl && // optimistic concurrency not support case
                !bmd.optionACommitOption && // option A not supported
                bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X) {
                // must be a 1_X bean  (either module type ok)
                // either enaabled at server or bean level or method specific approval in effect
                if (bmd.allowCustomFinderSQLForUpdateMethodLevel || cfHonorAccessIntent) {
                    if (finderDetect) {
                        fbpkDetect = homeMethodNames[i].equals("findByPrimaryKey");
                        if (finderDetect && !fbpkDetect) {
                            if (cfHonorAccessIntent) { // can avoid doing method compares below
                                cfForUpdateSupported = true;
                            } else {
                                cfForUpdateSupported = false;
                            }
                            // ok, check case where method level override in effect and bean & server overrides
                            // not in place, try to avoid as this is performance intensive checking strings
                            if (bmd.allowCustomFinderSQLForUpdateMethodLevel && !cfForUpdateSupported) {
                                if (bmd.cfMethodSignatureEqual(homeMethodNames[i],
                                                               MethodAttribUtils.methodSignatureOnly(homeMethods[i]),
                                                               cfMethodSignatures)) {
                                    cfForUpdateSupported = true;
                                    if (isTraceOn && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "Custom Finder Access Intent Method Level Override in Effect: " + MethodAttribUtils.methodSignature(homeMethods[i]));
                                    }
                                }
                            }
                            /*
                             * if (cfForUpdateSupported) {
                             * if (isTraceOn && tc.isDebugEnabled()) {
                             * Tr.debug(tc, "Custom Finder Update Access Intent Honored: " + MethodAttribUtils.methodSignature(homeMethods[i]));
                             * }
                             * }
                             */
                        } // if method being processed is a custom finder
                    } // if finder
                } // if cfHonorAccessIntent || method level overide in effect
            } // if custom finder supported in the static configuration of environment

            // enforcing the honoring of access intent for custom finders complete...
            // d112604.1 end

            // optimize fbpk Read Only override. if read-only currently, skip fbpk string comp
            if (finderDetect) { // d170166
                isFBPK = homeMethodNames[i].equals("findByPrimaryKey");
                if (isFBPK && fbpkReadOnlyOverride) { // d170166
                    homeReadOnlyAttrs[i] = true;
                }
            }
            // d112604.1 end

            String[] homeRolesAllowed = null;
            if ((homeRoleArrays[i] != null) && !(homeRoleArrays[i].isEmpty())) {
                homeRolesAllowed = homeRoleArrays[i].toArray(new String[0]); //d366845.11.2
            }

            EJBMethodInfoImpl methodInfo = bmd.createEJBMethodInfoImpl(slotSize);
            methodInfo.initializeInstanceData(MethodAttribUtils.methodSignature(homeMethods[i]),
                                              homeMethodNames[i],
                                              bmd,
                                              methodInterface,
                                              homeTransactionAttrs[i],
                                              false);
            methodInfo.setMethodDescriptor(jdiHomeMethodSignatures[i]);
            methodInfo.setIsolationLevel(homeIsolationAttrs[i]);
            methodInfo.setActivitySessionAttribute(homeActivitySessionAttrs[i]);
            methodInfo.setReadOnly(homeReadOnlyAttrs[i]);
            methodInfo.setSecurityPolicy(homeDenyAll[i], homePermitAll[i], homeRolesAllowed);
            methodInfo.setCMP11CustomFinderWithForUpdateAI(cfForUpdateSupported);
            methodInfo.setCMP11FBPK(isFBPK);
            homeMethodInfos[i] = methodInfo;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "initializeHomeMethodMetadata");

        return new MethodDataWrapper(homeMethodNames, homeMethodInfos, homeIsolationAttrs, homeReadOnlyAttrs);

        // f111627 End

    } // initializeHomeMethodMD

    /**
     *
     * @param requiredAttr
     * @param bmd
     * @return String or Null
     */
    protected String getCustomFinderSignatures(String requiredAttr, BeanMetaData bmd) {
        return null;
    } // getCustomFinderSignatures

    /**
     *
     * @param requiredAttr
     * @param bmd
     * @return boolean
     *
     */
    protected Boolean getWAS390CustomFinderBeanLevel(String requiredAttr, BeanMetaData bmd) {
        return Boolean.FALSE;
    }

    /**
     * Process generalizations from the EJBJarExtension and fill in the
     * childBeans hash table for all beans. This is how the EJB container
     * provides EJB inheritance support.
     *
     * @param moduleConfig <code>EJBModuleConfigData</code> holding module information.
     * @param mmd <code>EJBModuleMetaDataImpl</code> metadata for a module.
     *
     * @throws EJBConfigurationException for customer configuration errors.
     */
    public abstract void processGeneralizations(EJBModuleConfigData moduleConfig,
                                                EJBModuleMetaDataImpl mmd) throws EJBConfigurationException;

    /**
     *
     * @param t
     * @return Throwable
     */
    //d494984 eliminate assignment to parameter t.
    @SuppressWarnings("deprecation")
    protected Throwable getNestedException(Throwable t) {
        Throwable root = t;
        for (;;) {
            if (root instanceof RemoteException) {
                RemoteException re = (RemoteException) root;
                if (re.detail == null)
                    break;
                root = re.detail;
            } else if (root instanceof NamingException) {
                NamingException ne = (NamingException) root;
                if (ne.getRootCause() == null)
                    break;
                root = ne.getRootCause();
            } else if (root instanceof com.ibm.ws.exception.WsNestedException) { //d121522
                com.ibm.ws.exception.WsNestedException ne = (com.ibm.ws.exception.WsNestedException) root;
                if (ne.getCause() == null)
                    break;
                root = ne.getCause();
            } else {
                break;
            }
        }
        return root;
    }

    /**
     * Get the failover instance ID to use for SFSB failover if enabled
     * for this EJB module.
     *
     * @param mmd the module metadata
     * @param statefulFailoverCache is the failover cache that is used to hold created failover instances.
     *            Note, this method should not be called if statefulFailoverCache is null (otherwise,
     *            a NullPointerException will occur).
     * @return String
     **/
    protected abstract String getFailoverInstanceId(EJBModuleMetaDataImpl mmd, SfFailoverCache statefulFailoverCache);

    /**
     * Get whether SFSB failover is enabled for this SFSB at either
     * the module, application, or EJB container level (in that order).
     *
     * @param mmd the module metadata
     * @param container the container
     * @return true if enabled.
     */
    protected abstract boolean getSFSBFailover(EJBModuleMetaDataImpl mmd, EJSContainer container);

    /**
     * This method processes the optional system property {@link ContainerConfigConstants#allowCachedTimerDataFor}.
     **/
    //F001419
    private void processCachedTimerDataSettings(BeanMetaData bmd) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "processCachedTimerDataSettings " + AllowCachedTimerDataFor);
        }

        //F001419: start
        if (AllowCachedTimerDataFor != null) {
            StringTokenizer st = new StringTokenizer(AllowCachedTimerDataFor, ":");

            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int assignmentPivot = token.indexOf('=');

                if (assignmentPivot > 0) { //case where we have 'j2eename=<integer>' or '*=<integer>'

                    String lh = token.substring(0, assignmentPivot).trim(); //get j2eename or '*'
                    if (lh.equals(bmd.j2eeName.toString()) || lh.equals("*")) { // d130438

                        String rh = token.substring(assignmentPivot + 1).trim();//get <integer>
                        try {
                            bmd.allowCachedTimerDataForMethods = Integer.parseInt(rh);
                            bmd._moduleMetaData.ivAllowsCachedTimerData = true;
                            break;
                        } catch (NumberFormatException e) {
                            FFDCFilter.processException(e, CLASS_NAME + ".processCachedTimerDataSettings", "3923", this);
                        }
                    }
                } else { //token did not include an equals sign....case where we have just 'j2eename' or '*'.  Apply all caching in this case.
                    if (token.equals(bmd.j2eeName.toString()) || token.equals("*")) { // d130438
                        bmd.allowCachedTimerDataForMethods = -1; // d642293
                        bmd._moduleMetaData.ivAllowsCachedTimerData = true;
                        break;
                    }
                }
            } // while loop
        } // allowCachedTimerDataForSpec not null

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "processCachedTimerDataSettings " + bmd.allowCachedTimerDataForMethods);
        }
    }

    /**
     * This method calculates the bean pool limits of this bean type. There is one
     * bean pool for each of the application's bean types.
     *
     * This method supplies default min / max pool sizes if none
     * are specified via system property values.
     *
     * The two system properties examined by this method are
     * "noEJBPool" and "poolSizeSpecProp". The first property
     * "noEJBPool" allows customers to specify that we are to do no
     * pooling. If this property is set, the second property is
     * irrelevant. The second property "poolSizeSpecProp" allows
     * customers to provide min / max pool sizes for one or more
     * bean types by J2EEName. The format of this property is
     *
     * beantype=[H]min,[H]max [:beantype=[H]min,[H]max...]
     *
     * For example:
     *
     * SMApp#PerfModule#TunerBean=54,:SMApp#SMModule#TypeBean=100,200
     *
     * Note that either the min or max value may be ommitted. If so,
     * a default value is applied.
     *
     * Also note that "H" may be prepended to either min or max value(s).
     * This "H" designates the value as a hard limit. A hard limit on
     * min value causes the container to guarantee that at least that
     * many bean instances are available at all times (ie. this is the
     * combination of bean instances currently in use plus those in the
     * pool). A hard limint on max value causes the container to enforce
     * that the total number of bean instances (ie. total in use plus those
     * in the pool) does not exceed the max value. Without this hard
     * limit set only the maximum instance count in the pool is enforced,
     * there may also be additional instances in use.
     *
     * @param bmd is the configuration data to be updated with the
     *            calculated pool limits. The bmd variables set by this
     *            method include:
     *
     *            bmd.minPoolSize
     *            bmd.maxPoolSize,
     *            bmd.ivInitialPoolSize
     *            bmd.ivMaxCreation.
     */
    private void processBeanPoolLimits(BeanMetaData bmd) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "processBeanPoolLimits");
        }

        boolean checkAppConfigCustom = bmd.isCheckConfig(); // F743-33178

        //------------------------------------------------------------------------
        // The following default values are documented in the info center
        // changing them may affect existing customer applicatioins.
        //
        // Arguments can be made about the following values. No great
        // science was used in determining these numbers.
        //------------------------------------------------------------------------
        int defaultMinPoolSize = 50;
        int defaultMaxPoolSize = 500;
        long defaultMaxCreationTimeout = 300000; // 5 minutes

        String minStr = null;
        String maxStr = null;
        String timeoutStr = null;
        String poolSizeSpec = null;
        boolean noEJBPool = false; // d262413 PK20648

        //------------------------------------------------------------------------
        // Determine if the noEJBPool or poolSize java system properties have
        // been specified. noEJBPool overrides everything, so the poolSize
        // property is not needed when noEJBPool is enabled.       d262413 PK20648
        // Property access & default now handled by ContainerProperties.    391302
        //------------------------------------------------------------------------
        if (NoEJBPool)
            noEJBPool = true;
        else
            poolSizeSpec = PoolSize; // 121700

        if (poolSizeSpec != null) {
            StringTokenizer st = new StringTokenizer(poolSizeSpec, ":");

            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                int assignmentPivot = token.indexOf('=');
                if (assignmentPivot > 0) {
                    String lh = token.substring(0, assignmentPivot).trim();
                    // Now Z/OS does not allow "#" in java properties so we also
                    // must allow "%".   If those are used we will switch them to "#" before
                    // proceeding.         //d434795
                    lh = lh.replaceAll("%", "#");
                    if (lh.equals(bmd.j2eeName.toString())) { // d130438
                        String rh = token.substring(assignmentPivot + 1).trim();
                        StringTokenizer sizeTokens = new StringTokenizer(rh, ",");
                        if (sizeTokens.hasMoreTokens()) {
                            // min parm was specified
                            minStr = sizeTokens.nextToken().trim();
                        }
                        if (sizeTokens.hasMoreTokens()) {
                            // max parm was specified
                            maxStr = sizeTokens.nextToken().trim();
                        }
                        if (sizeTokens.hasMoreTokens()) {
                            // timeout parm was specified
                            timeoutStr = sizeTokens.nextToken().trim();
                        }
                    } else if (lh.equals("*") && (minStr == null || maxStr == null || timeoutStr == null)) {
                        String rh = token.substring(assignmentPivot + 1).trim();
                        StringTokenizer sizeTokens = new StringTokenizer(rh, ",");
                        if (sizeTokens.hasMoreTokens()) {
                            // min parm was specified
                            String defaultMinStr = sizeTokens.nextToken();
                            if (minStr == null) {
                                minStr = defaultMinStr.trim();
                            }
                        }
                        if (sizeTokens.hasMoreTokens()) {
                            // max parm was specified
                            String defaultMaxStr = sizeTokens.nextToken();
                            if (maxStr == null) {
                                maxStr = defaultMaxStr.trim();
                            }
                        }
                        if (sizeTokens.hasMoreTokens()) {
                            // timeout parm was specified
                            String defaultTimeoutStr = sizeTokens.nextToken();
                            if (timeoutStr == null) {
                                timeoutStr = defaultTimeoutStr.trim();
                            }
                        }
                    }
                } else {
                    // Syntax error -- token did not include an equals sign....
                    Tr.warning(tc, "POOLSIZE_MISSING_EQUALS_SIGN_CNTR0062W", new Object[] { token });
                    if (isValidationFailable(checkAppConfigCustom)) {
                        EJBConfigurationException ecex = new EJBConfigurationException("An equals sign was not found in the pool size specification string " +
                                                                                       token + ".");
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(tc, "processBeanPoolLimits : " + ecex);
                        throw ecex;
                    }
                }
            } // while loop
        } // poolSizeSpec not null

        int tmpMinPoolSize, tmpMaxPoolSize;
        long tmpMaxCreationTimeout;
        boolean setInitialPoolSize = false; // PK20648
        boolean setMaxCreation = false; // PK20648
        if (minStr != null) {
            try {
                // An H prefix indicates a hard limit (pre-load pool).       PK20648
                if (minStr.startsWith("H")) {
                    setInitialPoolSize = true;
                    minStr = minStr.substring(1);
                }
                tmpMinPoolSize = Integer.parseInt(minStr);
                if (tmpMinPoolSize < 1) {
                    Tr.warning(tc, "INVALID_MIN_POOLSIZE_CNTR0057W", new Object[] { (bmd.j2eeName).toString(), Integer.toString(tmpMinPoolSize) });
                    tmpMinPoolSize = defaultMinPoolSize;
                    if (isValidationFailable(checkAppConfigCustom)) {
                        EJBConfigurationException ecex = new EJBConfigurationException("Minimum pool size specified for bean " + bmd.j2eeName +
                                                                                       " not a valid positive integer: " + tmpMinPoolSize + ".");
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(tc, "processBeanPoolLimits : " + ecex);
                        throw ecex;
                    }
                }
            } catch (NumberFormatException e) {
                FFDCFilter.processException(e, CLASS_NAME + ".processBeanPoolLimits", "2594", this);
                Tr.warning(tc, "INVALID_MIN_POOLSIZE_CNTR0057W", new Object[] { (bmd.j2eeName).toString(), minStr });
                tmpMinPoolSize = defaultMinPoolSize;
                if (isValidationFailable(checkAppConfigCustom)) {
                    EJBConfigurationException ecex = new EJBConfigurationException("Minimum pool size specified for bean " + bmd.j2eeName +
                                                                                   " not a valid integer: " + minStr + ".");
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "processBeanPoolLimits : " + ecex);
                    throw ecex;
                }
            }
        } else {
            tmpMinPoolSize = defaultMinPoolSize;
        }

        if (maxStr != null) {
            try {
                // An H prefix indicates a hard limit (Max Creation).        PK20648
                if (maxStr.startsWith("H")) {
                    setMaxCreation = true;
                    maxStr = maxStr.substring(1);
                }
                tmpMaxPoolSize = Integer.parseInt(maxStr);
                if (tmpMaxPoolSize < 1) {
                    Tr.warning(tc, "INVALID_MAX_POOLSIZE_CNTR0058W", new Object[] { (bmd.j2eeName).toString(), Integer.toString(tmpMaxPoolSize) });
                    tmpMaxPoolSize = defaultMaxPoolSize;
                    if (isValidationFailable(checkAppConfigCustom)) {
                        EJBConfigurationException ecex = new EJBConfigurationException("Maximum pool size specified for bean " + bmd.j2eeName +
                                                                                       " not a valid positive integer: " + tmpMaxPoolSize + ".");
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(tc, "processBeanPoolLimits : " + ecex);
                        throw ecex;
                    }
                }
            } catch (NumberFormatException e) {
                FFDCFilter.processException(e, CLASS_NAME + ".processBeanPoolLimits", "2616", this);
                Tr.warning(tc, "INVALID_MAX_POOLSIZE_CNTR0058W", new Object[] { (bmd.j2eeName).toString(), maxStr });
                tmpMaxPoolSize = defaultMaxPoolSize;
                if (isValidationFailable(checkAppConfigCustom)) {
                    EJBConfigurationException ecex = new EJBConfigurationException("Maximum pool size specified for bean " + bmd.j2eeName +
                                                                                   " not a valid integer: " + maxStr + ".");
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "processBeanPoolLimits : " + ecex);
                    throw ecex;
                }
            }
        } else {
            tmpMaxPoolSize = defaultMaxPoolSize;
        }

        if (setMaxCreation && timeoutStr != null) {
            try {
                tmpMaxCreationTimeout = Integer.parseInt(timeoutStr) * 1000;
                if (tmpMaxCreationTimeout < 0) {
                    Tr.warning(tc, "INVALID_MAX_POOLTIMEOUT_CNTR0127W", new Object[] { (bmd.j2eeName).toString(), timeoutStr, "300" });
                    tmpMaxCreationTimeout = defaultMaxCreationTimeout;
                    if (isValidationFailable(checkAppConfigCustom)) {
                        EJBConfigurationException ecex = new EJBConfigurationException("Maximum pool size timeout specified for bean " + bmd.j2eeName +
                                                                                       " not a valid positive integer: " + timeoutStr + ".");
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(tc, "processBeanPoolLimits : " + ecex);
                        throw ecex;
                    }
                }
            } catch (NumberFormatException e) {
                FFDCFilter.processException(e, CLASS_NAME + ".processBeanPoolLimits", "2573", this);
                Tr.warning(tc, "INVALID_MAX_POOLTIMEOUT_CNTR0127W", new Object[] { (bmd.j2eeName).toString(), timeoutStr, "300" });
                tmpMaxCreationTimeout = defaultMaxCreationTimeout;
                if (isValidationFailable(checkAppConfigCustom)) {
                    EJBConfigurationException ecex = new EJBConfigurationException("Maximum pool size timeout specified for bean " + bmd.j2eeName +
                                                                                   " not a valid integer: " + timeoutStr + ".");
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "processBeanPoolLimits : " + ecex);
                    throw ecex;
                }
            }
        } else {
            tmpMaxCreationTimeout = defaultMaxCreationTimeout;
        }

        // If the noEJBPool system property has been set to "true", then a
        // no-capacity ejb pool will be created. (This should only be used
        // for development / debug purposes)                    // d262413 PK20648
        if (noEJBPool) {
            bmd.minPoolSize = 0;
            bmd.maxPoolSize = 0;
        }
        // Otherwise, if an invalid min/max pair was specified, then the
        // defaults will be used, and a warning issued.
        else if (tmpMaxPoolSize < tmpMinPoolSize) {
            Tr.warning(tc, "INVALID_POOLSIZE_COMBO_CNTR0059W", new Object[] { (bmd.j2eeName).toString(), Integer.toString(tmpMinPoolSize), Integer.toString(tmpMaxPoolSize) });
            bmd.minPoolSize = defaultMinPoolSize;
            bmd.maxPoolSize = defaultMaxPoolSize;
            if (isValidationFailable(checkAppConfigCustom)) {
                EJBConfigurationException ecex = new EJBConfigurationException("Minimum pool size specified for bean " + bmd.j2eeName +
                                                                               " is greater than maximum pool size specified: (" +
                                                                               tmpMinPoolSize + "," + tmpMaxPoolSize + ")");
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "processBeanPoolLimits : " + ecex);
                throw ecex;
            }
        }
        // And finally, everything looks good, so the configured sizes
        // and pre-load / max creation limit values will be used.
        else {
            bmd.minPoolSize = tmpMinPoolSize;
            bmd.maxPoolSize = tmpMaxPoolSize;

            // Set the pre-load and max creation values to the min/max pool
            // size values if 'H' (hard) specified, and the values were
            // otherwise valid.                                             PK20648
            if (setInitialPoolSize)
                bmd.ivInitialPoolSize = bmd.minPoolSize;
            if (setMaxCreation) {
                bmd.ivMaxCreation = bmd.maxPoolSize;
                bmd.ivMaxCreationTimeout = tmpMaxCreationTimeout;
            }
        }

        if (poolSizeSpec != null || noEJBPool) {
            // Log an Info message indicating pool min/max values, including
            // whether or not they are 'hard' limits.                       PK20648
            String minPoolSizeStr = Integer.toString(bmd.minPoolSize);
            if (bmd.ivInitialPoolSize > 0)
                minPoolSizeStr = "H " + minPoolSizeStr;
            String maxPoolSizeStr = Integer.toString(bmd.maxPoolSize);
            if (bmd.ivMaxCreation > 0) {
                maxPoolSizeStr = "H " + maxPoolSizeStr;
                String maxPoolTimeoutStr = Long.toString(bmd.ivMaxCreationTimeout / 1000);
                Tr.info(tc, "POOLSIZE_VALUES_CNTR0128I",
                        new Object[] { minPoolSizeStr, maxPoolSizeStr, maxPoolTimeoutStr, bmd.enterpriseBeanClassName });
            } else {
                Tr.info(tc, "POOLSIZE_VALUES_CNTR0060I",
                        new Object[] { minPoolSizeStr, maxPoolSizeStr, bmd.enterpriseBeanClassName });
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "processBeanPoolLimits");
        }

    } // processBeanPoolLimits

    /**
     * Process method configuration data. This includes both bean methods and home methods.
     *
     * @param bmd
     * @param cdo
     * @param beanType
     * @param transactionList
     * @param securityList
     * @param activitySessionList
     * @param localBeanMethods
     * @param remoteBeanMethods
     * @param timerMethods
     * @param webserviceMethods
     * @param localHomeMethods
     * @param remoteHomeMethods
     * @param implMethods
     */
    private void processMethodMetadata(BeanMetaData bmd,
                                       ByteCodeMetaData byteCodeMetaData,
                                       int beanType,
                                       List<ContainerTransaction> transactionList,
                                       List<MethodPermission> securityList,
                                       ExcludeList excludeList,
                                       List<ActivitySessionMethod> activitySessionList,
                                       Method[] localBeanMethods,
                                       Method[] remoteBeanMethods,
                                       Method[] timerMethods,
                                       Method[] webserviceMethods,
                                       Method[] localHomeMethods,
                                       Method[] remoteHomeMethods,
                                       Method[] implMethods) // d494984
                    throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "processMethodMetadata");
        }

        //------------------------------------------------------------------------
        // Build up all the method level metadata objects.  This
        // code must come after we have finished the specific processing
        // for each bean type in finishBMDInit.
        //
        // Note that remove() method level attributes are initialized
        // as follows:
        //
        //  - for entity beans, just use default transaction attr
        //  - for session beans,
        //      if they use TX_BEAN_MANAGED then
        //        use TX_BEAN_MANAGED
        //      else
        //        use TX_NOT_SUPPORTED
        //------------------------------------------------------------------------

        if (bmd.remoteInterfaceClass != null ||
            bmd.ivBusinessRemoteInterfaceClasses != null) {
            // f111627 Begin
            //---------------------------------------------------------------------
            // Determine method level metadata (include place-holder
            // for remove() ) for Remote methods.
            //---------------------------------------------------------------------
            initializeBeanMethodMD(beanType == Entity_Type,
                                   MethodInterface.REMOTE, // d162441
                                   bmd.remoteInterfaceClass,
                                   bmd.ivBusinessRemoteInterfaceClasses, // F743-24429
                                   remoteBeanMethods, // d366807
                                   implMethods, // d494984
                                   BeanMetaData.entityRemoteNoTxAttrMethods,
                                   BeanMetaData.entityRemoteNoTxAttrMethodSignatures, //PQ63130
                                   bmd.accessIntentList,
                                   bmd.isoLevelList,
                                   transactionList,
                                   securityList,
                                   excludeList,
                                   activitySessionList,
                                   bmd,
                                   byteCodeMetaData).postProcessRemoteBeanMethodData(bmd); // d115551
        }

        if (bmd.localInterfaceClass != null ||
            bmd.ivBusinessLocalInterfaceClasses != null) {
            //--------------------------------------------------------
            // Determine method level metadata (include place-holder
            // for remove() ) for Local methods.
            //--------------------------------------------------------
            initializeBeanMethodMD(beanType == Entity_Type,
                                   (bmd.type == InternalConstants.TYPE_MESSAGE_DRIVEN) ? MethodInterface.MESSAGE_LISTENER : MethodInterface.LOCAL, // d162441
                                   bmd.localInterfaceClass,
                                   bmd.ivBusinessLocalInterfaceClasses, // F743-24429
                                   localBeanMethods, // d366807
                                   implMethods, // d494984
                                   BeanMetaData.entityLocalNoTxAttrMethods,
                                   BeanMetaData.entityLocalNoTxAttrMethodSignatures, //PQ63130
                                   bmd.accessIntentList,
                                   bmd.isoLevelList,
                                   transactionList,
                                   securityList,
                                   excludeList,
                                   activitySessionList,
                                   bmd,
                                   byteCodeMetaData).postProcessLocalBeanMethodData(bmd); // d115551
        }

        if (timerMethods != null) {
            // -----------------------------------------------------------------
            // Determine method level metadata (include place-holder
            // for remove() ) for TimedObject methods.                 LI2281.07
            // -----------------------------------------------------------------
            initializeBeanMethodMD(beanType == Entity_Type,
                                   MethodInterface.TIMED_OBJECT,
                                   BeanMetaData.svTimedObjectClass,
                                   null,
                                   timerMethods,
                                   implMethods, // d494984
                                   BeanMetaData.entityLocalNoTxAttrMethods,
                                   BeanMetaData.entityLocalNoTxAttrMethodSignatures,
                                   bmd.accessIntentList,
                                   bmd.isoLevelList,
                                   transactionList,
                                   securityList,
                                   excludeList,
                                   activitySessionList,
                                   bmd,
                                   byteCodeMetaData).postProcessTimedBeanMethodData(bmd);

            //F743-15870
            //Currently, the EJBMethodInfoImpl.ivNumberOfParms variable we set here
            //is only needed to invoke timer callback methods.  If it's ever needed
            //elsewhere, we may need to move this logic into a more common location.
            for (EJBMethodInfoImpl ejbMethodInfoImpl : bmd.timedMethodInfos) {
                ejbMethodInfoImpl.setNumberOfMethodParms(ejbMethodInfoImpl.getMethod().getParameterTypes().length);
            }
        }

        if (bmd.ivHasWebServiceEndpoint) {
            // --------------------------------------------------------------------
            // Determine method level metadata (include place-holder
            // for remove() ) for WebService Endpoint methods.            LI2281.24
            // --------------------------------------------------------------------
            initializeBeanMethodMD(beanType == Entity_Type,
                                   MethodInterface.SERVICE_ENDPOINT,
                                   bmd.webserviceEndpointInterfaceClass,
                                   null,
                                   webserviceMethods, // d366807
                                   implMethods, // d494984
                                   BeanMetaData.entityLocalNoTxAttrMethods,
                                   BeanMetaData.entityLocalNoTxAttrMethodSignatures,
                                   bmd.accessIntentList,
                                   bmd.isoLevelList,
                                   transactionList,
                                   securityList,
                                   excludeList,
                                   activitySessionList,
                                   bmd,
                                   byteCodeMetaData).postProcessWebserviceBeanMethodData(bmd);
        }

        if (bmd.type != InternalConstants.TYPE_MESSAGE_DRIVEN) {
            if (bmd.homeInterfaceClass != null) {
                //--------------------------------------------------------
                // Determine method level metadata for Remote home methods.
                //--------------------------------------------------------
                initializeHomeMethodMD(beanType == Entity_Type,
                                       MethodInterface.HOME, // d162441
                                       remoteHomeMethods,
                                       BeanMetaData.sessionRemoteHomeNoTxAttrMethods,
                                       BeanMetaData.sessionRemoteHomeNoTxAttrMethodSignatures, //PQ63130
                                       BeanMetaData.entityRemoteHomeNoTxAttrMethods,
                                       BeanMetaData.entityRemoteHomeNoTxAttrMethodSignatures, //PQ63130
                                       bmd.accessIntentList,
                                       bmd.isoLevelList,
                                       transactionList,
                                       securityList,
                                       excludeList,
                                       activitySessionList,
                                       bmd).postProcessRemoteHomeMethodData(bmd); // d115551
            }

            if (bmd.localHomeInterfaceClass != null) {
                //--------------------------------------------------------
                // Determine method level metadata for Local home methods.
                //--------------------------------------------------------
                initializeHomeMethodMD(beanType == Entity_Type,
                                       MethodInterface.LOCAL_HOME, // d162441
                                       localHomeMethods,
                                       BeanMetaData.sessionLocalHomeNoTxAttrMethods,
                                       BeanMetaData.sessionLocalHomeNoTxAttrMethodSignatures, //PQ63130
                                       BeanMetaData.entityLocalHomeNoTxAttrMethods,
                                       BeanMetaData.entityLocalHomeNoTxAttrMethodSignatures, //PQ63130
                                       bmd.accessIntentList,
                                       bmd.isoLevelList,
                                       transactionList,
                                       securityList,
                                       excludeList,
                                       activitySessionList,
                                       bmd).postProcessLocalHomeMethodData(bmd); // d115551
            }
        }
        // f111627 End

        //------------------------------------------------------------------------
        // Now that we have built up all the method level metadata objects,
        // determine if any methods are CMR set methods.  CMR set methods
        // may only be exposed on the Local Interface of CMP 2.x beans.
        // If CMR set methods are found, we update the corresponding
        // method info object(s)                                        d184523
        //------------------------------------------------------------------------
        if (bmd.localMethodInfos != null &&
            bmd.cmpVersion == InternalConstants.CMP_VERSION_2_X) {
            Relationships relationships = bmd.wccm.ejbjar.getRelationshipList();
            List<CMRField> cmrFields = new ArrayList<CMRField>();
            if (relationships != null) {
                for (EJBRelation rel : relationships.getEjbRelations()) {
                    for (EJBRelationshipRole role : rel.getRelationshipRoles()) {
                        if (role.getSource().getEntityBeanName().equals(bmd.enterpriseBeanName)) {
                            CMRField cmrField = role.getCmrField();
                            if (cmrField != null) {
                                cmrFields.add(cmrField);
                            }
                        }
                    }
                }
            }

            int cmrFieldSize = cmrFields.size();
            if (cmrFieldSize > 0) {
                // First, build a list of all CMR set method names.
                ArrayList<String> cmrSetters = new ArrayList<String>();
                for (int i = 0; i < cmrFieldSize; i++) {
                    CMRField cmrField = cmrFields.get(i);
                    String name = cmrField.getName();
                    cmrSetters.add("set" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
                }

                // Loop through all local method names, looking for a match
                for (int i = 0; i < bmd.localMethodInfos.length && !cmrSetters.isEmpty(); i++) {
                    String methodName = bmd.localMethodInfos[i].getName();
                    if (methodName.startsWith("set")) {
                        for (int j = 0; j < cmrSetters.size(); j++) {
                            if (methodName.equals(cmrSetters.get(j))) {
                                if (isTraceOn && tc.isDebugEnabled())
                                    Tr.debug(tc, methodName + " is a CMR Set Method");

                                // mark it as a cmr set method
                                bmd.localMethodInfos[i].isCMRSetMethod = true;

                                // remove from list to improve performance
                                cmrSetters.remove(j);
                                break;
                            }
                        }
                    }
                }
            }
        } // CMR method calulations

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "processMethodMetadata");
        }

    } // processMethodMetadata

    /**
     * Process the Security Identity for the EnterpriseBean using the
     * following rules:
     * 1. Determine if the security-identity deployment descriptor for this
     * enterprise bean contains a role or the use-caller-identity element.
     * 2. If no security-identity exists in XML then check for the
     * RunAs annotation defined on the enterprise bean.
     * 3. Default behavior will be determined by the Security Component.
     *
     * @param bmd BeanMetaData for the specific Enterprise Bean
     */
    private void processSecurityIdentity(BeanMetaData bmd) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "processSecurityIdentity");
        }
        EnterpriseBean ejbBean = bmd.wccm.enterpriseBean;

        boolean runAsSet = false;
        if (ejbBean != null) {
            // get runAs identity
            SecurityIdentity secIdentity = ejbBean.getSecurityIdentity();
            if (secIdentity != null) {
                runAsSet = true;
                if (secIdentity.isUseCallerIdentity()) {
                    bmd.ivUseCallerIdentity = true;
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "RunAs set to Caller Identity ");
                } else {
                    bmd.ivRunAs = secIdentity.getRunAs().getRoleName();
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "RunAs set to " + bmd.ivRunAs);
                }
            } // if (secIdentity != null)

        } // if (ejbBean != null)

        //Only check for @runAs annotation if it is not set by XML.  //446358
        if (!runAsSet) {
            //check for @RunAs annotation
            RunAs runAs = bmd.enterpriseBeanClass.getAnnotation(javax.annotation.security.RunAs.class);
            if (runAs != null) {
                bmd.ivRunAs = runAs.value();
            }
        } // if (!runAsSet)

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "processSecurityIdentity:  useCallerIdentity = " +
                        bmd.ivUseCallerIdentity + ":  Use Role = " +
                        bmd.ivRunAs);
        }
    } // processSecurityIdentity

    /**
     * Process any role links that have been established. We
     * will create a HashMap linking these role-links (if any) to
     * their corresponding roles. This information is needed during
     * isCallerInRole processing.
     *
     * @param bmd BeanMetaData for the specific Enterprise Bean
     */

    private void processSecurityRoleLink(BeanMetaData bmd) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "processSecurityRoleLink");
        }

        EnterpriseBean ejbBean = bmd.wccm.enterpriseBean;

        if (ejbBean != null) {
            // get security-role-ref if any

            // The role name in thesecurity-role-ref is not really a role name,
            // it is a seudo name that is to be linked to the "real"
            // role name that is in the role-link stanza  //429623
            for (SecurityRoleRef roleRef : ejbBean.getSecurityRoleRefs()) {
                String link = roleRef.getLink();
                if (link != null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "role-ref = " + roleRef.getName() + " role-link = " + link);
                    //create a new HashMap only when needed.
                    if (bmd.ivRoleLinkMap == null) {
                        bmd.ivRoleLinkMap = new HashMap<String, String>();
                    }
                    bmd.ivRoleLinkMap.put(roleRef.getName(), link);
                }
            }
        } // if (ejbBean != null)

        if (isTraceOn && tc.isEntryEnabled()) {
            if (bmd.ivRoleLinkMap == null) {
                Tr.exit(tc, "processSecurityRoleLink:  no role-link elements found.");
            } else {
                Tr.exit(tc, "processSecurityRoleLink:  " + bmd.ivRoleLinkMap.toString());
            }
        }

    } // processSecurityRoleLink

    /**
     *
     * Process IBM extensions to the EJB Spec; DisableFlushBeforeFind,
     * DisableEJBStoreForNonDirtyBeans, and LightWeightLocal that
     * may be configured via a marker interface or a bean environment
     * variable may now also be configured via WCCM in the IBM
     * extensions xmi file (ibm-ejb-jar-ext.xmi). Extract that information
     * from the Entity or ContainerManagedEntity as appropriate, and enable
     * that support if specified. Note that only a 'true' value is processed,
     * 'false' is ignored, and does not 'disable' the support. The support
     * is enabled if the marker interface is present or either the WCCM field
     * or the bean env variable is set to 'true'. d341280
     *
     * @param bmd
     */
    protected abstract void processEJBExtensionsMetadata(BeanMetaData bmd);

    /**
     * Processes stateful session timeout extension.
     *
     * @return true if the session timeout was found
     */
    protected abstract boolean processSessionExtensionTimeout(BeanMetaData bmd);

    /**
     * Processes session bean extensions.
     */
    protected abstract void processSessionExtension(BeanMetaData bmd) throws EJBConfigurationException;

    /**
     * Processes entity bean extensions.
     */
    protected abstract void processEntityExtension(BeanMetaData bmd) throws EJBConfigurationException;

    /**
     * Metadata processing that is unique for Z/OS platform
     *
     * @param bmd
     */
    protected abstract void processZOSMetadata(BeanMetaData bmd);

    /**
     * This methods groups together most of the metadata processing that is unique
     * to a given bean type in hopes of avoiding many "if" checks all over the rest of
     * the metadata processing code.
     *
     * @param bmd
     * @param cdo
     * @param beanType
     * @param classNameToLoad
     *
     * @throws ContainerException - for internal errors
     * @throws EJBConfigurationException - for customer configuration errors
     *
     */
    private void processMetadataByBeanType(BeanMetaData bmd,
                                           int beanType) // F743-17630
                    throws ContainerException, EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "processMetadataByBeanType");
        }

        switch (beanType) {

            case Session_Type:

                // Set session timeout.  Session timeout only applies to Stateful session beans.
                // During server startup the system properties are checked and set, if the system
                // property for stateful session bean timeout was not set, the default value of
                // 10 minutes is set in the ContainerProperties.DefaultStatefulSessionTimeout.
                // Initially use this value, then check if the timeout is set in
                // ibm-ejb-jar-ext.xmi file, then take that value.     d627044
                if (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION) {
                    // F743-14726  d627044
                    // Set initially to the value of ContainerProperties.DefaultStatefulSessionTimeout
                    bmd.sessionTimeout = DefaultStatefulSessionTimeout;
                    boolean sessionTimeoutExtensionFound = processSessionExtensionTimeout(bmd); // F743-6605, F743-18775
                    // begin F743-6605

                    // stateful-timeout DD in ejb-jar.xml and/or @StatefulTimeout annotation will
                    // override session timeout in the SessionExtension, if present.
                    // XML value takes precedence over annotation
                    // Retrieve the stateful-timeout value, if one was computed by the WCCM config reader

                    Session session = (Session) bmd.wccm.enterpriseBean;
                    com.ibm.ws.javaee.dd.ejb.StatefulTimeout statefulTimeoutXML = session == null ? null : session.getStatefulTimeout();
                    if (statefulTimeoutXML != null) {
                        long statefulTimeoutXMLTimeout = statefulTimeoutXML.getTimeout();
                        TimeUnit statefulTimeoutXMLUnit = statefulTimeoutXML.getUnitValue();

                        bmd.sessionTimeout = MethodAttribUtils.timeUnitToMillis(statefulTimeoutXMLTimeout, statefulTimeoutXMLUnit, false, bmd);

                        if (isTraceOn && tc.isDebugEnabled()) {
                            if (sessionTimeoutExtensionFound) {
                                Tr.debug(tc, "Overriding session timeout " + bmd.sessionTimeout +
                                             " found in extension with stateful-timeout with " +
                                             statefulTimeoutXMLTimeout + " " + statefulTimeoutXMLUnit +
                                             " found in ejb-jar.xml.");
                            } else {
                                Tr.debug(tc, "Using stateful timeout " + bmd.sessionTimeout + " from XML for bean " + bmd.getName());
                            }

                        }

                    } else {

                        // XML value not found, so look for @StatefulTimeout annotation

                        StatefulTimeout statefulTimeout = bmd.enterpriseBeanClass.getAnnotation(javax.ejb.StatefulTimeout.class);
                        if (statefulTimeout != null) {

                            // F743-14726  d627044
                            // Set to the value of ContainerProperties.DefaultStatefulSessionTimeout
                            long statefulTimeoutAnnTimeout = DefaultStatefulSessionTimeout; // F743-6605.1  F743-14726  d627044
                            statefulTimeoutAnnTimeout = statefulTimeout.value();
                            TimeUnit unit = statefulTimeout.unit();
                            bmd.sessionTimeout = MethodAttribUtils.timeUnitToMillis(statefulTimeoutAnnTimeout, unit, true, bmd);

                            if (isTraceOn && tc.isDebugEnabled()) {
                                if (sessionTimeoutExtensionFound) {
                                    Tr.debug(tc, "Overriding session timeout " + bmd.sessionTimeout +
                                                 " found in extension with StatefulTimeout with " +
                                                 statefulTimeoutAnnTimeout + " " + unit + " found in bean-level annotation.");
                                } else {
                                    Tr.debug(tc, "Using stateful timeout " + bmd.sessionTimeout + " from annotation for bean " + bmd.getName()); // F743-6605.1
                                }
                            }

                        }

                    }
                    // end F743-6605

                    //LIDB2018-1 begins
                    // Check if this SFSB is a startup bean by looking at whether
                    // home interface is the startup bean home interface.
                    if ((bmd.homeInterfaceClassName != null && bmd.homeInterfaceClassName.equals("com.ibm.websphere.startupservice.AppStartUpHome")) ||
                        (bmd.localHomeInterfaceClassName != null && bmd.localHomeInterfaceClassName.equals("com.ibm.websphere.startupservice.AppStartUpHome"))) {
                        // It is a startup bean, so ensure failover is disabled since we never
                        // want to failover a startup bean.
                        bmd.ivSFSBFailover = false; //d210506
                    } else {
                        // Not a startup bean, so determine if failover should be enabled.
                        // d361748 start of change.
                        EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;
                        if (mmd.ivSfsbFailover) {
                            if (!bmd.passivationCapable) {
                                Tr.info(tc, "FAILOVER_DISABLED_NOT_PASSIVATION_CAPABLE_CNTR0331I",
                                        new Object[] { bmd.enterpriseBeanName,
                                                       bmd._moduleMetaData.ivName,
                                                       bmd._moduleMetaData.ivAppName });
                            } else {
                                // SFSB failover is enabled for this module, so initialize
                                // BeanMetaData with SFSB failover flag and reference to
                                // the SfFailoverClient object to use for replication of SFSB data.
                                // The module meta data has the failover string ID that was used
                                // to create and cache the SfFailoverClient object for this module.
                                String failoverId = mmd.ivFailoverInstanceId;
                                SfFailoverCache failover = bmd.container.getSfFailoverCache();
                                bmd.ivSfFailoverClient = failover.getCachedSfFailoverClient(failoverId);
                                if (bmd.ivSfFailoverClient != null) {
                                    bmd.ivSFSBFailover = true;
                                } else {
                                    // This should never happen if the admin console GUI is used to
                                    // configure SFSB failover. However, if customer edits the deployment.xml
                                    // and/or server.xml file directly, then we could end up in this
                                    // code path.  So log warning message to inform administrator that
                                    // we are missing a default failover setting to use and that SFSB
                                    // failover is not enabled for this bean.
                                    bmd.ivSFSBFailover = false;
                                    // CNTR0097W: Memory to memory replication settings for EJB container is missing.
                                    Tr.warning(tc, "MISSING_CONTAINER_DRSSETTINGS_CNTR0097W");
                                }
                            }
                        } // d361748 end of change.

                    } //LIDB2018-1 ends
                } else if (bmd.type == InternalConstants.TYPE_SINGLETON_SESSION) {
                    // Determine singleton session bean concurrency control.
                    processSingletonConcurrencyManagementType(bmd);
                }

                // Bean reentrant flag is set below according to bean type.
                bmd.reentrant = false;

                // TODO: MLS - Is removeTxAttr on bmd really used any more?
                if (bmd.usesBeanManagedTx) { // LIDB2257.19
                    bmd.removeTxAttr = BeanMetaData.TX_ATTRIBUTE_TYPE_BEAN_MANAGED;
                } else {
                    bmd.removeTxAttr = BeanMetaData.TX_ATTRIBUTE_TYPE_NOT_SUPPORTED;
                }

                processSessionExtension(bmd); // F743-18775

                bmd.usesBeanManagedAS = (bmd.container).getContainerExtensionFactory().isActivitySessionBeanManaged(bmd.usesBeanManagedTx); // LIDB441.5, d126204.2

                bmd.pKeyClass = null;

                break;

            case Entity_Type:

                bmd.usesBeanManagedAS = false;
                bmd.sessionTimeout = 0;

                // primary key class is loader here rather than in
                // loadCustomerProvidedClasses because customers only
                // supply key implementations for entity beans
                Entity entity = (Entity) bmd.wccm.enterpriseBean;
                String primaryKeyClassName = entity.getPrimaryKeyName();

                try {
                    bmd.pKeyClass = bmd.classLoader.loadClass(primaryKeyClassName); //d494984
                } catch (ClassNotFoundException ex) {
                    FFDCFilter.processException(ex, CLASS_NAME + ".processMetadataByBeanType",
                                                "3392", this);
                    if (isTraceOn && tc.isEventEnabled())
                        Tr.event(tc, "Failed to initialize BeanMetaData instance", ex);
                    EJBConfigurationException ecex;
                    ecex = new EJBConfigurationException("Bean class " + primaryKeyClassName + " could not be found or loaded", ex);
                    Tr.error(tc, "BEANCLASS_NOT_FOUND_CNTR0075E", primaryKeyClassName); //d494984
                    throw ecex;
                }
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, " **** bean primary key class = " +
                                 primaryKeyClassName);
                }

                bmd.reentrant = entity.isReentrant();
                processEntityExtension(bmd); // F743-18775
                break;

            case Message_Type:

                // Message Driven Beans are required to be non-reentrant
                // as stated in 15.8.4 of EJB 2.1 specification.
                bmd.reentrant = false;

                bmd.usesBeanManagedAS = bmd.container.getContainerExtensionFactory().isActivitySessionBeanManaged(bmd.usesBeanManagedTx); // LIDB441.5, d126204.2

                bmd.pKeyClass = null;

                // d244832 start of change
                // Verify MDB bean implements the message listener interface required by messaging type.
                // Note:  Starting in EJB3 the MessageListenerInterface is no longer required to be on
                // the implements clause of the EJB, so this check is only for prior versions.  //450391
                if (!(bmd.localInterfaceClass.isAssignableFrom(bmd.enterpriseBeanAbstractClass)) &&
                    (bmd.getEJBModuleVersion() < BeanMetaData.J2EE_EJB_VERSION_3_0)) {
                    // Whoops, the EJB 2.x specification says the MDB class must implement,
                    // directly or indirectly, the message listener interface required by the
                    // messaging type that it supports. In the case of JMS, this is the
                    // javax.jms.MessageListener interface.  So let's cause MDB to fail to start
                    // in order to avoid the problem of JCA 1.5 RA encountering a ClassCastException
                    // when it tries to call a method on MDB instance that does not implement
                    // the messaging listener interface.

                    // Log CNTR0112E: The user-provided class "{0}" must implement the "{1}" interface.
                    Tr.error(tc, "MDB_MUST_IMPLEMENT_INTERFACE_CNTR0112E", new Object[] { bmd.enterpriseBeanClassName, bmd.localInterfaceClass.getName() });
                    throw new EJBConfigurationException("MDB " + bmd.enterpriseBeanClassName
                                                        + " must implement interface "
                                                        + bmd.localInterfaceClass.getName());

                } //d244832 end of change

                break;

            case Managed_Type:

                // There is no type specific processing for Managed Beans F743-34301

                break;

            default:

                throw new ContainerException("Unknown BeanType");
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "processMetadataByBeanType");
        }

    } //procesMetadataByBeanType

    /**
     * Process the annotation or xml for a singleton session bean concurrency
     * management type metadata. Note, this method assumes caller has ensured this
     * method is only called for Singleton session bean.
     *
     * @param bmd is the BeanMetaData for the Singleton session bean.
     *
     * @throws EJBConfigurationException
     */
    // F743-1752.1 added entire method.
    // F743-1752CodRev rewrote to do error checking and validation.

    private void processSingletonConcurrencyManagementType(BeanMetaData bmd) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "processSingletonConcurrencyManagementType");
        }

        // Initialize DD Concurrency Management type to null and change to
        // non null value if it is specified in the DD for the module.
        ConcurrencyManagementType ddType = null;

        // F743-7027 start
        Session sessionBean = (Session) bmd.wccm.enterpriseBean;
        if (sessionBean != null) {
            int type = sessionBean.getConcurrencyManagementTypeValue();
            if (type == Session.CONCURRENCY_MANAGEMENT_TYPE_CONTAINER) {
                ddType = ConcurrencyManagementType.CONTAINER;
            } else if (type == Session.CONCURRENCY_MANAGEMENT_TYPE_BEAN) {
                ddType = ConcurrencyManagementType.BEAN;
            }
        } // F743-7027 end

        // Initialize annotation Concurrency Management type to null and change to
        // non null value only if metadata complete is false and annotation is used
        // for this Singleton session bean.
        ConcurrencyManagementType annotationType = null;
        if (bmd.metadataComplete == false) {
            // Metadata complete is false, so do we have annotation value?
            // Note, EJB spec says annotation can not be inherited from superclass.
            Class<?> ejbClass = bmd.enterpriseBeanClass;
            ConcurrencyManagement annotation = ejbClass.getAnnotation(javax.ejb.ConcurrencyManagement.class);
            if (annotation != null) {
                // We have an annotation on EJB class, so validate that it is a valid value.
                annotationType = annotation.value();
                if (annotationType != ConcurrencyManagementType.CONTAINER && annotationType != ConcurrencyManagementType.BEAN) {
                    // CNTR0193E: The value, {0}, that is specified for the concurrency management type of
                    // the {1} enterprise bean class must be either BEAN or CONTAINER.
                    Tr.error(tc, "SINGLETON_INVALID_CONCURRENCY_MANAGEMENT_CNTR0193E", new Object[] { annotationType, bmd.enterpriseBeanClassName });

                    throw new EJBConfigurationException("CNTR0193E: The value, " + annotationType
                                                        + ", that is specified for the concurrency management type of the "
                                                        + bmd.enterpriseBeanClassName + " enterprise bean class must be either BEAN or CONTAINER.");
                }
            }
        }

        // Assign concurrency management type for this Singleton seesion bean.
        // Note, in "4.8.4.3 Specification of a Concurrency Management Type" of EJB 3.1 specification,
        // it says "The application assembler is not permitted to use the deployment
        // descriptor to override a bean's concurrency management type regardless of whether
        // it has been explicitly specified or defaulted by the Bean Provider."  However,
        // we have no way to know whether DD is from application assembler vs Bean Provider,
        // so we have to assume it came from Bean Provider.  The team decided that if both
        // annotation and DD is used, then the value must match. If they do not match,
        // throw an exception.

        // Was a value provided via DD?
        if (ddType == null) {
            // DD did not provide a value, so use annotation value if one was provided.
            // Otherwise, use default value required by EJB 3.1 spec.
            if (annotationType != null) {
                // No DD, but we have annotation. So use the annotation.
                bmd.ivSingletonUsesBeanManagedConcurrency = (annotationType == ConcurrencyManagementType.BEAN);
            } else {
                // Neither DD nor annotation, so default to Container Concurrency Management type.
                bmd.ivSingletonUsesBeanManagedConcurrency = false;
            }
        } else {
            // DD did provide a concurrency management type. Verify it matches annotation type
            // if both annotation and DD provided a value.
            if (annotationType != null && ddType != annotationType) {
                // CNTR0194E: The value {0} that is specified in the ejb-jar.xml file for concurrency management type is
                // not the same as the @ConcurrencyManagement annotation value {1} on the {2} enterprise bean class.
                Tr.error(tc, "SINGLETON_XML_OVERRIDE_CONCURRENCY_MANAGEMENT_CNTR0194E", new Object[] { ddType, annotationType, bmd.enterpriseBeanClassName });

                throw new EJBConfigurationException("CNTR0194E: The value " + ddType
                                                    + " that is specified in the ejb-jar.xml file for concurrency management type is not the same "
                                                    + " as the @ConcurrencyManagement annotation value " + annotationType + " on the "
                                                    + bmd.enterpriseBeanClassName + " enterprise bean class.");
            }

            // Use DD value since either annotation not used or DD matches the annotation type.
            bmd.ivSingletonUsesBeanManagedConcurrency = (ddType == ConcurrencyManagementType.BEAN);
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "processSingletonConcurrencyManagementType returning "
                        + bmd.ivSingletonUsesBeanManagedConcurrency + " for j2ee name = " + bmd.j2eeName);
        }
    }

    /**
     * Create the appropriate ManagedObjectFactory for the bean type, or obtain the constructor
     * if a ManagedObjectFactory will not be used.
     */
    private void initializeManagedObjectFactoryOrConstructor(BeanMetaData bmd) throws EJBConfigurationException {
        if (bmd.isManagedBean()) {
            bmd.ivEnterpriseBeanFactory = getManagedBeanManagedObjectFactory(bmd, bmd.enterpriseBeanClass);

            // If the managed object factroy and the managed object instances will perform all injection
            // and interceptor processing, then save that indication and reset all of the methodinfos so
            // that AroundInvoke is not handled by the EJB Container. Injection, PostConstruct, and
            // PreDestroy will also be skipped.
            if (bmd.ivEnterpriseBeanFactory != null && bmd.ivEnterpriseBeanFactory.managesInjectionAndInterceptors()) {
                bmd.managedObjectManagesInjectionAndInterceptors = true;
                if (bmd.ivInterceptorMetaData != null && bmd.localMethodInfos != null) {
                    for (EJBMethodInfoImpl mbMethod : bmd.localMethodInfos) {
                        mbMethod.setAroundInterceptorProxies(null);
                    }
                }
            }
        } else {
            bmd.ivEnterpriseBeanFactory = getEJBManagedObjectFactory(bmd, bmd.enterpriseBeanClass);
        }
        if (bmd.ivEnterpriseBeanFactory == null) {
            try {
                bmd.ivEnterpriseBeanClassConstructor = bmd.enterpriseBeanClass.getConstructor((Class<?>[]) null);
            } catch (NoSuchMethodException e) {
                Tr.error(tc, "JIT_NO_DEFAULT_CTOR_CNTR5007E",
                         new Object[] { bmd.enterpriseBeanClassName, bmd.enterpriseBeanName });
                throw new EJBConfigurationException("CNTR5007E: The " + bmd.enterpriseBeanClassName + " bean class for the " + bmd.enterpriseBeanName +
                                                    " bean does not have a public constructor that does not take parameters.");
            }
        }
        // Removed else clause that calls bmd.ivEnterpriseBeanFactory.getConstructor() because this method
        // is called before cdiMMD.setWebBeansContext(webBeansContext) in LibertySingletonServer. If we call
        // getConstructor before setting the webBeansContext, we default to a constructor that may not be correct.
        // eg. returning no-arg constructor when multi-arg constructor injection should be invoked.
    }

    /**
     * Obtain the constructor for the entity bean concrete class.
     */
    private void initializeEntityConstructor(BeanMetaData bmd) throws EJBConfigurationException {
        try {
            bmd.ivEnterpriseBeanClassConstructor = bmd.enterpriseBeanClass.getConstructor((Class<?>[]) null);
        } catch (NoSuchMethodException e) {
            Tr.error(tc, "JIT_NO_DEFAULT_CTOR_CNTR5007E",
                     new Object[] { bmd.enterpriseBeanClassName, bmd.enterpriseBeanName });
            throw new EJBConfigurationException("CNTR5007E: The " + bmd.enterpriseBeanClassName + " bean class for the " + bmd.enterpriseBeanName +
                                                " bean does not have a public constructor that does not take parameters.");
        }
    }

    /**
     * Returns the managed object service.
     */
    protected abstract ManagedObjectService getManagedObjectService() throws EJBConfigurationException;

    /**
     * Gets a managed object factory for the specified ManagedBean class, or null if
     * instances of the class do not need to be managed.
     *
     * @param bmd the bean metadata
     * @param klass the ManagedBean class
     */
    protected <T> ManagedObjectFactory<T> getManagedBeanManagedObjectFactory(BeanMetaData bmd, Class<T> klass) throws EJBConfigurationException {
        ManagedObjectFactory<T> factory = null;
        ManagedObjectService managedObjectService = getManagedObjectService();
        if (managedObjectService != null) {
            try {
                factory = managedObjectService.createManagedObjectFactory(bmd._moduleMetaData, klass, true);
                //TODO the isManaged method could be moved to the ManagedObjectService and then wouldn't need to create the factory
                if (!factory.isManaged()) {
                    factory = null;
                }
            } catch (ManagedObjectException e) {
                throw new EJBConfigurationException(e);
            }
        }

        return factory;
    }

    /**
     * Gets a managed object factory for the specified interceptor class, or null if
     * instances of the class do not need to be managed.
     *
     * @param bmd the bean metadata
     * @param klass the interceptor class
     */
    protected <T> ManagedObjectFactory<T> getInterceptorManagedObjectFactory(BeanMetaData bmd, Class<T> klass) throws EJBConfigurationException {
        ManagedObjectFactory<T> factory = null;
        ManagedObjectService managedObjectService = getManagedObjectService();
        if (managedObjectService != null) {
            try {
                factory = managedObjectService.createInterceptorManagedObjectFactory(bmd._moduleMetaData, klass);
                //TODO the isManaged method could be moved to the ManagedObjectService and then wouldn't need to create the factory
                if (!factory.isManaged()) {
                    factory = null;
                }
            } catch (ManagedObjectException e) {
                throw new EJBConfigurationException(e);
            }
        }

        return factory;
    }

    /**
     * Gets a managed object factory for the specified EJB class, or null if
     * instances of the class do not need to be managed.
     *
     * @param bmd the bean metadata
     * @param klass the bean implementation class
     */
    protected <T> ManagedObjectFactory<T> getEJBManagedObjectFactory(BeanMetaData bmd, Class<T> klass) throws EJBConfigurationException {
        ManagedObjectFactory<T> factory = null;
        ManagedObjectService managedObjectService = getManagedObjectService();
        if (managedObjectService != null) {
            try {
                factory = managedObjectService.createEJBManagedObjectFactory(bmd._moduleMetaData, klass, bmd.j2eeName.getComponent());
                //TODO the isManaged method could be moved to the ManagedObjectService and then wouldn't need to create the factory
                if (!factory.isManaged()) {
                    factory = null;
                }
            } catch (ManagedObjectException e) {
                throw new EJBConfigurationException(e);
            }
        }

        return factory;
    }

    /**
     *
     * Load all classes provided by customer application. Pre-EJB3 versions of the EJB spec
     * required customers to provide certain inteface and implementation classes. These are
     * loaded by this method.
     *
     * @param cdo
     * @param bmd
     *
     * @throws ContainerException for internal errors
     * @throws EJBConfigurationException for customer configuration errors
     *
     */
    private void loadCustomerProvidedClasses(BeanMetaData bmd) throws ContainerException, EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "loadCustomerProvidedClasses");
        }

        // For MDBs, the local interface must be read from a different spot
        // in WCCM compared to other bean types.  Also, beginning with EJB 2.1
        // MDBs are allowed to specified a MessageListener interface other
        // than javax.jms.MessageListener (ie. this is referred to as Messaging
        // Type in the xml).   //430621
        if (bmd.type == InternalConstants.TYPE_MESSAGE_DRIVEN) {
            bmd.ivMessagingTypeClassName = bmd.ivInitData.ivMessageListenerInterfaceName;
            if (bmd.ivMessagingTypeClassName == null) {
                // d659661.1 - validateMergedMetaData has already ensured that 3.0+
                // beans have a valid interface specified.
                bmd.ivMessagingTypeClassName = "javax.jms.MessageListener";
            } // else if ( bmd.ivMessagingTypeClassName == null )
        }

        // Note: We need to be on correct classloader because customer classes are
        //       loaded.  There are a couple paths where this method can be called
        //       so be sure to use application's classloader just in case it is not
        //       currently the thread's active classloader.
        ClassLoader classLoader = bmd.classLoader;

        // -----------------------------------------------------------------------
        // If this EJB is being dynamically generated via the EJB Facade support
        // then the facade EJBClassFactory needs to be called now to give the
        // ejb provider the opportunity to generate the EJB classes before
        // the code below attempts to obtain a reference to them.  F3438-12805CdRv
        // -----------------------------------------------------------------------
        EJBConfiguration facadeConfig = bmd.ivInitData.ivFacadeConfiguration;
        if (facadeConfig != null) {
            EJBClassFactory facadeClassFactory = facadeConfig.getEJBClassFactory();

            if (facadeClassFactory != null) {
                facadeClassFactory.loadEJBClasses(classLoader,
                                                  facadeConfig);
            }
        }

        // -----------------------------------------------------------------------
        // We load most of the customer provided interface and implementation
        // classes here.
        //
        // The primary key class is not loaded here because it only applies to
        // Entity beans.  Also the Web Service endpoint interface is not loaded
        // here because it applies only to stateless session beans.   Both these
        // classes are loaded later in this method
        //
        // Generated classes (ie. EJBDeploy and JITDeploy) are loaded much later
        // near the end of this method.
        // -----------------------------------------------------------------------

        String classNameToLoad = null;

        try {
            classNameToLoad = bmd.ivInitData.ivRemoteInterfaceName;
            if (classNameToLoad != null) {
                bmd.remoteInterfaceClass = classLoader.loadClass(classNameToLoad);
            }

            classNameToLoad = bmd.ivInitData.ivRemoteHomeInterfaceName;
            if (classNameToLoad != null) {
                bmd.homeInterfaceClass = classLoader.loadClass(classNameToLoad);

                // When a component remote interface is added via annotations, just
                // the component remote home interface is specified, and the remote
                // interface must be derived from the remote home.  The component
                // remote interface must be the return type of all remote home
                // create methods.                                          366845.9
                if (bmd.remoteInterfaceClass == null) {
                    // JITDeploy knows how to extract this information and
                    // perform some validation (will fail if null).           d443878
                    bmd.remoteInterfaceClass = JITDeploy.getComponentInterface(bmd.homeInterfaceClass,
                                                                               bmd.j2eeName.toString());

                    // save the name for NameUtil/JITDeploy use later.
                    bmd.ivInitData.ivRemoteInterfaceName = bmd.remoteInterfaceClass.getName();
                }
            }

            classNameToLoad = bmd.ivInitData.ivLocalInterfaceName;
            if (classNameToLoad != null) {
                bmd.localInterfaceClass = classLoader.loadClass(classNameToLoad);
            } else {
                classNameToLoad = bmd.ivMessagingTypeClassName;
                if (classNameToLoad != null) {
                    bmd.localInterfaceClass = classLoader.loadClass(classNameToLoad);
                }
            }
            classNameToLoad = bmd.ivInitData.ivLocalHomeInterfaceName;
            if (classNameToLoad != null) {
                bmd.localHomeInterfaceClass = classLoader.loadClass(classNameToLoad);

                // When a component local interface is added via annotations, just
                // the component local home interface is specified, and the local
                // interface must be derived from the local home.  The component
                // local interface must be the return type of all local home
                // create methods. 366845.9
                if (bmd.localInterfaceClass == null) {
                    // JITDeploy knows how to extract this information and
                    // perform some validation (will fail if null).           d443878
                    bmd.localInterfaceClass = JITDeploy.getComponentInterface(bmd.localHomeInterfaceClass,
                                                                              bmd.j2eeName.toString());

                    // save the name for NameUtil/JITDeploy use later.
                    bmd.ivInitData.ivLocalInterfaceName = bmd.localInterfaceClass.getName();
                }
            }

            // Load business local interface classes.                     d366807.4
            String[] businessLocalInterfaceName = bmd.ivInitData.ivLocalBusinessInterfaceNames;
            if (businessLocalInterfaceName != null) {
                int offset = 0;
                int numInterfaces = businessLocalInterfaceName.length;

                // Leave room for No-Interface view at the beginning       F743-1756
                if (bmd.ivLocalBean) {
                    offset = 1;
                }

                bmd.ivBusinessLocalInterfaceClasses = new Class[numInterfaces + offset];
                for (int i = 0; i < numInterfaces; ++i) {
                    classNameToLoad = businessLocalInterfaceName[i];
                    bmd.ivBusinessLocalInterfaceClasses[i + offset] = classLoader.loadClass(classNameToLoad);
                }
            }

            // Load business remote interface classes.                    d366807.4
            String[] businessRemoteInterfaceName = bmd.ivInitData.ivRemoteBusinessInterfaceNames;
            if (businessRemoteInterfaceName != null) {
                int numInterfaces = businessRemoteInterfaceName.length;
                bmd.ivBusinessRemoteInterfaceClasses = new Class[numInterfaces];
                for (int i = 0; i < numInterfaces; ++i) {
                    classNameToLoad = businessRemoteInterfaceName[i];
                    bmd.ivBusinessRemoteInterfaceClasses[i] = classLoader.loadClass(classNameToLoad);
                }
            }

            // load webservice endpoint interface if applicable
            classNameToLoad = bmd.ivInitData.ivWebServiceEndpointInterfaceName;
            if (classNameToLoad != null) {
                //d495288: start
                try {
                    bmd.webserviceEndpointInterfaceClass = bmd.classLoader.loadClass(classNameToLoad);
                } catch (ClassNotFoundException ex) {
                    //If the SEI class is not found, a ClassNotFoundException (CNFE) will be thrown.  Prior to EJB 3.0 FEP,
                    //the exception would be completely eaten (i.e. no trace, no ffdc, no exceptions rethrown).  In EJB 3.0 FEP the
                    //code to load the SEI class (and other classes), has been refactored (moved from ComponentNameSpaceHelper to here).
                    //In the refactor, when an SEI class is not found the CNFE will NOT be eaten, thus causing the app to not
                    //start.  To preserve compatibility, we must allow the loading of the SEI to function the same way as it did
                    //in prior releases.  However, for EJB 3.0+ modules, we can allow the CNFE to propagate since EJB 3.0 modules
                    //are new and we don't have to worry about regression.
                    if (bmd.ivModuleVersion <= BeanMetaData.J2EE_EJB_VERSION_2_1) {
                        if (isTraceOn && tc.isDebugEnabled()) {
                            Tr.debug(tc, "loadCustomerProvidedClasses : Caught the following exception when attempting to load a "
                                         + "Service Endpoint Interface (SEI): " + ex + " : " + ex.getMessage() + ".  This is an EJB 1.x or "
                                         + "2.x module and the SEI class can't be found.  One possible reason is that the user defined a "
                                         + "<service-endpoint> stanza in their ejb-jar.xml file, but didn't provide the SEI class (at least "
                                         + "it is not found on the classpath).");
                        }
                    } else {
                        //rethrow the exception and let the 'outer' catch block catch and handle the exception
                        throw ex;
                    }
                }
                //d495288: end
            }

            // Load customer provided bean implementation class.  Not that
            // for CMP 2.X beans this is really an abstract implementation
            // that will be replaced below by a generated concrete bean
            // implementation subclass.
            classNameToLoad = bmd.enterpriseBeanClassName;
            bmd.enterpriseBeanClass = bmd.classLoader.loadClass(classNameToLoad);

            // Save enterpriseBeanClass name in case it turns out to be
            // an abstract implementation.    //150685
            bmd.enterpriseBeanAbstractClass = bmd.enterpriseBeanClass;

            // If No-Interface view (LocalBean), then add the EJB impl class
            // as the first local business interface.                     F743-1756
            if (bmd.ivLocalBean) {
                if (bmd.ivBusinessLocalInterfaceClasses == null) {
                    bmd.ivBusinessLocalInterfaceClasses = new Class[1];
                }
                bmd.ivBusinessLocalInterfaceClasses[0] = bmd.enterpriseBeanClass;
            }

            // Trace all the customer provided classes we have loaded.
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, " **** component local interface class = " +
                             bmd.ivInitData.ivLocalInterfaceName);
                Tr.debug(tc, "   (OR) **** MDB local interface class = " +
                             bmd.ivMessagingTypeClassName);
                Tr.debug(tc, " **** component remote interface class = " +
                             bmd.ivInitData.ivRemoteInterfaceName);
                if (businessLocalInterfaceName != null) {
                    Tr.debug(tc, " **** business local interface class(es) = " +
                                 Arrays.toString(businessLocalInterfaceName));
                } else {
                    Tr.debug(tc, " **** business local interface class(es) = null");
                }
                if (businessRemoteInterfaceName != null) {
                    Tr.debug(tc, " **** business remote inteface class(es) = " +
                                 Arrays.toString(businessRemoteInterfaceName));
                } else {
                    Tr.debug(tc, " **** business remote inteface class(es) = null");
                }
                Tr.debug(tc, " **** bean implementation class = " +
                             bmd.enterpriseBeanClassName);
                Tr.debug(tc, " **** home remote interface class = " +
                             bmd.homeInterfaceClassName);
                Tr.debug(tc, " **** home local interface class = " +
                             bmd.localHomeInterfaceClassName);
            }

        } catch (ClassNotFoundException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".loadCustomerProvidedClasses", "3830");
            if (isTraceOn && tc.isEventEnabled())
                Tr.event(tc, "Failed to initialize BeanMetaData instance", ex);
            EJBConfigurationException ecex;
            ecex = new EJBConfigurationException("Bean class " + classNameToLoad + " could not be found or loaded", ex);
            Tr.error(tc, "BEANCLASS_NOT_FOUND_CNTR0075E", classNameToLoad);
            throw ecex;
        } catch (LinkageError ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".loadCustomerProvidedClasses", "3840");
            if (isTraceOn && tc.isEventEnabled())
                Tr.event(tc, "Failed to initialize BeanMetaData instance", ex);
            EJBConfigurationException ecex;
            ecex = new EJBConfigurationException("Bean class " + classNameToLoad + " could not be loaded", ex);
            Tr.error(tc, "BEANCLASS_NOT_FOUND_CNTR0075E", classNameToLoad);
            throw ecex;
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".loadCustomerProvidedClasses", "3850");
            ContainerException cex;
            if (classNameToLoad != null) {
                cex = new ContainerException("Bean class " + classNameToLoad + " could not be found or loaded", ex);
                Tr.error(tc, "BEANCLASS_NOT_FOUND_CNTR0075E", classNameToLoad);
            } else {
                cex = new ContainerException("Failed to initialize BeanMetaData instance - " +
                                             "caught Throwable", ex);
                Tr.error(tc, "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                         new Object[] { ex, cex.toString() });
            }
            throw cex;
        }

        // Now that all of the classes have been loaded, perform some basic
        // interface validation.  Each of the interfaces will be checked
        // against the rules defined in the EJB Specification, similar
        // to the type of checking done by EJBDeploy.                      d443878
        // Note: only required JITDeployed beans, as EJBDeploy will have
        // already performed similar validation for other beans.           PK60953
        if (!bmd._moduleMetaData.isEJBDeployed()) {
            JITDeploy.validateInterfaceBasics(bmd.homeInterfaceClass,
                                              bmd.remoteInterfaceClass,
                                              bmd.localHomeInterfaceClass,
                                              bmd.localInterfaceClass,
                                              bmd.webserviceEndpointInterfaceClass,
                                              bmd.ivBusinessRemoteInterfaceClasses,
                                              bmd.ivBusinessLocalInterfaceClasses,
                                              bmd.enterpriseBeanClass,
                                              bmd.j2eeName.toString(),
                                              bmd.type);
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "loadCustomerProvidedClasses");
        }

    } // loadCustomerProvidedClasses

    /**
     * Load the enterprise bean class provided by the customer application.
     *
     * @param cdo
     * @param bmd
     * @throws ContainerException for internal errors
     * @throws EJBConfigurationException for customer configuration errors
     */
    // F743-506
    private static Class<?> loadCustomerProvidedBeanClass(BeanMetaData bmd) throws ContainerException, EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "loadCustomerProvidedBeanClass : " + bmd.enterpriseBeanClassName);

        // enterpriseBeanClass will be null only if loadCustomerProvidedBeanClass
        // is called prior to loadCustomerProvidedClasses.  For example, from
        // processAutomaticTimerMetaData via processBean if deferred init.
        if (bmd.enterpriseBeanClass == null) {
            try {
                bmd.enterpriseBeanClass = bmd.classLoader.loadClass(bmd.enterpriseBeanClassName);
            } catch (Throwable ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".loadCustomerProvidedBeanClass", "6369");
                Tr.error(tc, "BEANCLASS_NOT_FOUND_CNTR0075E", bmd.enterpriseBeanClassName);

                String message = "Bean class " + bmd.enterpriseBeanClassName + " could not be found or loaded";
                if (ex instanceof ClassNotFoundException || ex instanceof LinkageError)
                    throw new EJBConfigurationException(message, ex);
                throw new ContainerException(message, ex);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "loadCustomerProvidedBeanClass");
        return bmd.enterpriseBeanClass;
    }

    /**
     * This method loads all the generated implementations classes (i.e classes
     * not directly provided by the customer). This includes both classes
     * generated by EJBDeploy and JITDeploy.
     *
     * @throws ContainerException for internal errors
     * @throws EJBConfigurationsException for customer configuration errors
     */
    private void loadGeneratedImplementationClasses(BeanMetaData bmd,
                                                    Method[] localMethods,
                                                    Method[] remoteMethods,
                                                    Method[] localHomeMethods,
                                                    Method[] remoteHomeMethods) throws ContainerException, EJBConfigurationException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "loadGeneratedImplementationClasses");
        }

        // Use indirect local proxies if configured and supported.          F58064
        EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;
        bmd.ivIndirectLocalProxies = (ContainerProperties.IndirectLocalProxies ||
                                      mmd.getEJBApplicationMetaData().isIndirectLocalProxies() ||
                                      bmd.ivUnversionedJ2eeName != null)
                                     &&
                                     (bmd.isEntityBean() ||
                                      bmd.isStatelessSessionBean() ||
                                      bmd.isSingletonSessionBean());

        String classNameToLoad = null;
        EJBRuntime runtime = bmd.container.getEJBRuntime();

        try {
            String componentRemoteWrapperImplName = null;
            String componentLocalWrapperImplName = null;
            String businessRemoteWrapperImplName[] = null;
            String businessLocalWrapperImplName[] = null;
            String homeRemoteWrapperImplName = null;
            String homeLocalWrapperImplName = null;
            String homeImplName = null;
            ClassLoader classLoader = bmd.classLoader;

            // --------------------------------------------------------------------
            // For ManagedBeans, usually the only 'generated' class would be the
            // home, which is really just a special subclass of EJSHome that is
            // not generated at all, and can just be set directly. There is no
            // need for a NameUtil for this, so skip all of the processing below
            // and just set the home class and return.
            //
            // However, if the ManagedBean has either PreDestroy or AroundInvoke
            // interceptors, then a special version of a No-Interface wrapper
            // is needed (which skips pre/post inovke).. so fall through to
            // the code below and generate one.                        F743-34301.1
            // --------------------------------------------------------------------
            if (bmd.type == InternalConstants.TYPE_MANAGED_BEAN) {
                // ManagedBeans have special home for bean creation
                bmd.homeBeanClass = ManagedBeanHome.class;

                if (bmd.ivInterceptorMetaData == null) {
                    return; // no interceptors = no wrapper
                }

                if (bmd.ivInterceptorMetaData.ivPreDestroyInterceptors == null) {
                    boolean hasAroundInvoke = false;
                    for (EJBMethodInfoImpl methodInfo : bmd.localMethodInfos) {
                        if (methodInfo.getAroundInterceptorProxies() != null) {
                            hasAroundInvoke = true;
                            break;
                        }
                    }

                    // No PreDestory && no AroundInvoke = no wrapper
                    if (!hasAroundInvoke) {
                        return;
                    }
                }
            }

            // -----------------------------------------------------------------------
            //
            // Create an instance of NameUtil, passing it all of the data that it
            // would have previously extracted from WCCM.  This allows NameUtil
            // to generate deployed class names independent of the source (i.e.
            // WCCM or annotations)... and by using an instance, instead of static
            // methods, provides improved performance, as the common parts of
            // the deployed class names can be calculated once.
            //
            // Note that the meta data constants used elsewhere in EJB Container
            // must be converted to the NameUtil constants to avoid circular build
            // dependencies, and to allow NameUtil to continue to be shared
            // with EJBDeploy.                                               d366807.3
            //
            // -----------------------------------------------------------------------

            // retrieve customer provided class names
            String remoteHomeInterface = bmd.ivInitData.ivRemoteHomeInterfaceName;
            String remoteInterface = bmd.ivInitData.ivRemoteInterfaceName;
            String localHomeInterface = bmd.ivInitData.ivLocalHomeInterfaceName;
            String localInterface = bmd.ivInitData.ivLocalInterfaceName;
            String[] remoteBusinessInterfaces = bmd.ivInitData.ivRemoteBusinessInterfaceNames;
            String[] localBusinessInterfaces = bmd.ivInitData.ivLocalBusinessInterfaceNames;
            String beanClass = bmd.ivInitData.ivClassName;
            String primaryKey = bmd.pKeyClass == null ? null : bmd.pKeyClass.getName();

            // No-Interface view counts as a local interface, and since it requires
            // the same processing below, will be added to the list of local business
            // interfaces (and may be the only local interface).          F743-1756
            if (bmd.ivLocalBean) {
                if (localBusinessInterfaces == null) {
                    localBusinessInterfaces = new String[1];
                } else {
                    String[] newNames = new String[localBusinessInterfaces.length + 1];
                    System.arraycopy(localBusinessInterfaces, 0, newNames, 1,
                                     localBusinessInterfaces.length);
                    localBusinessInterfaces = newNames;
                }

                localBusinessInterfaces[0] = beanClass;
            }

            // Convert bean type into NameUtil type string
            String nameUtilBeanType = EJBMDOrchestrator.nameTypeFromBeanTypeMap[bmd.type];

            // RELEASE: This switch statement must be updated with every new release level
            // Calculate NameUtil version from bean version
            int nameUtilBeanVersion = -1;
            switch (bmd.ivModuleVersion) {
                case BeanMetaData.J2EE_EJB_VERSION_1_1:
                case BeanMetaData.J2EE_EJB_VERSION_1_0:
                    nameUtilBeanVersion = NameUtil.EJB_1X;
                    break;

                case BeanMetaData.J2EE_EJB_VERSION_2_0:
                case BeanMetaData.J2EE_EJB_VERSION_2_1:
                    nameUtilBeanVersion = NameUtil.EJB_2X;
                    break;

                default:
                    nameUtilBeanVersion = NameUtil.EJB_3X;
                    break;
            }

            NameUtil nameUtil = new NameUtil(bmd.enterpriseBeanName, remoteHomeInterface, remoteInterface, localHomeInterface, localInterface, remoteBusinessInterfaces, localBusinessInterfaces, beanClass, primaryKey, nameUtilBeanType, nameUtilBeanVersion);

            // --------------------------------------------------------------------
            //JITDeploy handling for all MDB's.
            // --------------------------------------------------------------------
            if (bmd.type == InternalConstants.TYPE_MESSAGE_DRIVEN) {
                // Set localImplClassName based on whether the old or new MDB implementation
                // is being used. A null value indicates the new MDB implementation and will
                // always be returned on Liberty. The new MDB implementation runs through JITDeploy.
                // For traditional WAS, the old MDB message listener style could be returned (MessageListener.class),
                // and we need to preserve this behavior.
                bmd.localImplClass = runtime.getMessageEndpointImplClass(bmd);
                if (bmd.localImplClass != null) {
                    componentLocalWrapperImplName = bmd.localImplClass.getName();
                } else {
                    componentLocalWrapperImplName = nameUtil.getMDBProxyClassName();
                    classNameToLoad = componentLocalWrapperImplName;
                    EJBWrapperType wrapperType;
                    if (bmd.ivIsNoMethodInterfaceMDB) {
                        wrapperType = EJBWrapperType.MDB_NO_METHOD_INTERFACE_PROXY;
                    } else {
                        wrapperType = EJBWrapperType.MDB_PROXY;
                    }
                    bmd.localImplClass = JITDeploy.generateMDBProxy(classLoader,
                                                                    classNameToLoad,
                                                                    new Class<?>[] { bmd.localInterfaceClass, bmd.enterpriseBeanClass },
                                                                    wrapperType,
                                                                    bmd.enterpriseBeanClass.getName(),
                                                                    localMethods,
                                                                    bmd.localMethodInfos,
                                                                    bmd.j2eeName.toString(),
                                                                    runtime.getClassDefiner());
                }
                // Clear classNameToLoad so any exceptions here won't get
                // reported in the catch block below as a ClassNotFound,
                // but instead as a general container error.
                classNameToLoad = null;

                // For no-method interface MDB, obtain the ivMessageEndpointBase field
                // on the generated proxy class that holds a reference to the
                // MessageEndpointBase object and make it accessible so it may be set
                // when an instance is created.
                if (bmd.ivIsNoMethodInterfaceMDB) {
                    bmd.ivMessageEndpointBaseField = bmd.localImplClass.getDeclaredField(MESSAGE_ENDPOINT_BASE_FIELD);
                    bmd.ivMessageEndpointBaseField.setAccessible(true);
                }

                // L001126: If we have an activation spec then the home bean
                // class is a MessageEndpointFactory.  If not, then we must be
                // using listener ports, so set the home bean class to an MDBPool.
                bmd.homeBeanClass = runtime.getMessageEndpointFactoryImplClass(bmd);
                homeImplName = bmd.homeBeanClass.getName();

                // Finally, set the activation config properties in the bmd and
                // resolve the message destination JNDI name for the MDB
                bmd.ivActivationConfig = bmd.ivInitData.ivActivationConfigProperties;
                runtime.resolveMessageDestinationJndiName(bmd);
            }

            // --------------------------------------------------------------------
            // If this is a ManagedBean that requires a wrapper, then generate a
            // special version of a no-interface wrapper that doesn't call pre
            // or post invoke.                                         F743-34301.1
            // --------------------------------------------------------------------
            else if (bmd.type == InternalConstants.TYPE_MANAGED_BEAN) {
                bmd.ivBusinessLocalImplClasses = new Class[1];
                classNameToLoad = nameUtil.getBusinessLocalImplClassName(0);
                bmd.ivBusinessLocalImplClasses[0] = JITDeploy.generateEJBWrapper(classLoader,
                                                                                 classNameToLoad,
                                                                                 new Class<?>[] { bmd.ivBusinessLocalInterfaceClasses[0] },
                                                                                 EJBWrapperType.MANAGED_BEAN,
                                                                                 localMethods,
                                                                                 bmd.localMethodInfos,
                                                                                 bmd.enterpriseBeanClassName,
                                                                                 bmd.j2eeName.toString(),
                                                                                 runtime.getClassDefiner()); // F70650

                // For No-Interface view (LocalBean), obtain the Fields on
                // the generated wrapper class that holds a reference to the
                // container and wrapper metadata (an EJSWrapperBase subclass)
                // and make them accessible so they may be set when an
                // instance is created.

                // Clear classNameToLoad so any exceptions here won't get
                // reported in the catch block below as a ClassNotFound,
                // but instead as a general container error.
                classNameToLoad = null;
                bmd.ivLocalBeanWrapperField = bmd.ivBusinessLocalImplClasses[0].getDeclaredField(LOCAL_BEAN_WRAPPER_FIELD);
                bmd.ivLocalBeanWrapperField.setAccessible(true);
                bmd.ivManagedBeanBeanOField = bmd.ivBusinessLocalImplClasses[0].getDeclaredField(MANAGED_BEAN_BEANO_FIELD);
                bmd.ivManagedBeanBeanOField.setAccessible(true);
            }

            // --------------------------------------------------------------------
            // Beginning with EJB 3.0 module level, all 'generated' classes
            // (except CMP) are generated and loaded dynamically via ASM
            // (i.e. 'just-in-time' deploy).
            // --------------------------------------------------------------------
            else if (!mmd.isEJBDeployed()) {
                // Generate/Load the Component Remote Implementation ('Wrapper')
                if (remoteInterface != null) {
                    componentRemoteWrapperImplName = nameUtil.getRemoteImplClassName();
                    classNameToLoad = componentRemoteWrapperImplName;
                    bmd.remoteImplClass = JITDeploy.generateEJBWrapper(classLoader,
                                                                       classNameToLoad,
                                                                       new Class<?>[] { bmd.remoteInterfaceClass },
                                                                       EJBWrapperType.REMOTE, // d413752
                                                                       remoteMethods,
                                                                       bmd.methodInfos, // d367572.4
                                                                       bmd.enterpriseBeanClassName,
                                                                       bmd.j2eeName.toString(), // d443878
                                                                       runtime.getClassDefiner()); // F70650

                    // Only generate the Tie if remote is supported (CORBA classes are available)
                    if (runtime.isRemoteSupported()) {
                        bmd.remoteTieClass = JITDeploy.generate_Tie(classLoader,
                                                                    classNameToLoad,
                                                                    bmd.remoteInterfaceClass,
                                                                    bmd.j2eeName.toString(),
                                                                    runtime.getClassDefiner(),
                                                                    mmd.getRMICCompatible(),
                                                                    runtime.isRemoteUsingPortableServer());
                    }
                }

                // Generate/Load the Component Local Implementation ('Wrapper')
                if (localInterface != null) {
                    componentLocalWrapperImplName = nameUtil.getLocalImplClassName(); // d114199 d147734

                    // L001126: Moved this section into the else block, this used to sit outside the if/else block
                    // but is no longer valid if the bmd.type is MESSAGE_DRIVEN as we will set localImplClass
                    // directly at that point.
                    classNameToLoad = componentLocalWrapperImplName;
                    bmd.localImplClass = JITDeploy.generateEJBWrapper(classLoader,
                                                                      classNameToLoad,
                                                                      new Class<?>[] { bmd.localInterfaceClass },
                                                                      EJBWrapperType.LOCAL, // d413752
                                                                      localMethods,
                                                                      bmd.localMethodInfos, // d367572.4
                                                                      bmd.enterpriseBeanClassName,
                                                                      bmd.j2eeName.toString(), // d443878
                                                                      runtime.getClassDefiner()); // F70650

                    // L001126 - end change
                }

                // Generate/Load the implementation classes (wrappers) for the
                // Business Local interfaces (except for MDB).              d369262
                if (bmd.ivBusinessLocalInterfaceClasses != null) {
                    int numInterfaces = bmd.ivBusinessLocalInterfaceClasses.length;
                    bmd.ivBusinessLocalImplClasses = new Class[numInterfaces];
                    businessLocalWrapperImplName = new String[numInterfaces];
                    // If No-Interface view present (LocalBean), then the first
                    // local wrapper is of type LOCAL_BEAN                  F743-1756
                    EJBWrapperType wrapperType = (bmd.ivLocalBean) ? EJBWrapperType.LOCAL_BEAN : EJBWrapperType.BUSINESS_LOCAL;
                    for (int i = 0; i < numInterfaces; ++i) {
                        businessLocalWrapperImplName[i] = nameUtil.getBusinessLocalImplClassName(i);
                        classNameToLoad = businessLocalWrapperImplName[i];
                        bmd.ivBusinessLocalImplClasses[i] = JITDeploy.generateEJBWrapper(classLoader,
                                                                                         classNameToLoad,
                                                                                         new Class<?>[] { bmd.ivBusinessLocalInterfaceClasses[i] },
                                                                                         wrapperType, // d413752 F743-1756
                                                                                         localMethods,
                                                                                         bmd.localMethodInfos, // d367572.4
                                                                                         bmd.enterpriseBeanClassName,
                                                                                         bmd.j2eeName.toString(), // d443878
                                                                                         runtime.getClassDefiner()); // F70650
                        wrapperType = EJBWrapperType.BUSINESS_LOCAL; // F743-1756
                    }

                    // For No-Interface view (LocalBean), obtain the Fields on
                    // the generated wrapper class that holds a reference to the
                    // container and wrapper metadata (an EJSWrapperBase subclass)
                    // and make them accessible so they may be set when an
                    // instance is created.                                 F743-1756
                    if (bmd.ivLocalBean) {
                        // Clear classNameToLoad so any exceptions here won't get
                        // reported in the catch block below as a ClassNotFound,
                        // but instead as a general container error.
                        classNameToLoad = null;
                        bmd.ivLocalBeanWrapperField = bmd.ivBusinessLocalImplClasses[0].getDeclaredField(LOCAL_BEAN_WRAPPER_FIELD);
                        bmd.ivLocalBeanWrapperField.setAccessible(true);
                    }

                    if (bmd.ivIndirectLocalProxies) {
                        bmd.ivBusinessLocalImplProxyConstructors = new Constructor<?>[numInterfaces];
                        for (int i = 0; i < numInterfaces; i++) {
                            Class<?> interfaceClass = bmd.ivBusinessLocalInterfaceClasses[i];
                            Method[] methods = DeploymentUtil.getMethods(null, new Class<?>[] { interfaceClass });
                            classNameToLoad = EJBWrapperProxy.getProxyClassName(interfaceClass);
                            bmd.ivBusinessLocalImplProxyConstructors[i] = getWrapperProxyConstructor(bmd, classNameToLoad, interfaceClass, methods);
                        }
                    }
                }

                // Generate/Load the implementation classes (wrappers) for the
                // Business Remote interfaces.                               d369262
                if (bmd.ivBusinessRemoteInterfaceClasses != null) {
                    int numInterfaces = bmd.ivBusinessRemoteInterfaceClasses.length;
                    bmd.ivBusinessRemoteImplClasses = new Class[numInterfaces];
                    bmd.ivBusinessRemoteTieClasses = new Class[numInterfaces]; // d416391
                    businessRemoteWrapperImplName = new String[numInterfaces];
                    for (int i = 0; i < numInterfaces; ++i) {
                        businessRemoteWrapperImplName[i] = nameUtil.getBusinessRemoteImplClassName(i);
                        classNameToLoad = businessRemoteWrapperImplName[i];
                        bmd.ivBusinessRemoteImplClasses[i] = JITDeploy.generateEJBWrapper(classLoader,
                                                                                          classNameToLoad,
                                                                                          new Class<?>[] { bmd.ivBusinessRemoteInterfaceClasses[i] },
                                                                                          EJBWrapperType.BUSINESS_REMOTE, // d413752
                                                                                          remoteMethods,
                                                                                          bmd.methodInfos, // d367572.4
                                                                                          bmd.enterpriseBeanClassName,
                                                                                          bmd.j2eeName.toString(), // d443878
                                                                                          runtime.getClassDefiner()); // F70650

                        // Only generate the Tie if remote is supported (CORBA classes are available)
                        if (runtime.isRemoteSupported()) {
                            bmd.ivBusinessRemoteTieClasses[i] = JITDeploy.generate_Tie(classLoader,
                                                                                       classNameToLoad,
                                                                                       bmd.ivBusinessRemoteInterfaceClasses[i],
                                                                                       bmd.j2eeName.toString(),
                                                                                       runtime.getClassDefiner(),
                                                                                       mmd.getRMICCompatible(),
                                                                                       runtime.isRemoteUsingPortableServer());
                        }
                    }
                }

                // Not an MDB, so generate home bean class name and home wrapper
                // class if necessary via JITDeploy.
                homeImplName = nameUtil.getHomeBeanClassName(); // d114199 d147734

                if (homeImplName != null) {
                    classNameToLoad = homeImplName;
                    bmd.homeBeanClass = JITDeploy.generateEJBHomeImplClass(bmd.classLoader,
                                                                           classNameToLoad,
                                                                           bmd.homeInterfaceClass,
                                                                           bmd.localHomeInterfaceClass,
                                                                           bmd.enterpriseBeanClass, // d369262.5
                                                                           bmd.pKeyClass, // d369262.8
                                                                           bmd.ivInitMethodMap, // d369262.5
                                                                           bmd.j2eeName.toString(), // d443878
                                                                           bmd.type,
                                                                           runtime.getClassDefiner()); // F70650
                } else if (bmd.type == InternalConstants.TYPE_SINGLETON_SESSION || // F743-508
                           bmd.type == InternalConstants.TYPE_STATELESS_SESSION ||
                           bmd.type == InternalConstants.TYPE_STATEFUL_SESSION) {
                    // Use generic SessionHome when no home is defined.
                    bmd.homeBeanClass = SessionHome.class; // F743-34301.1
                }

                // load generated remote Home ('Wrapper') Impl class - EJSRemote[Type]HomeItf
                //    e.g. EJSRemoteCMPTest1Home
                homeRemoteWrapperImplName = nameUtil.getHomeRemoteImplClassName(); // d114199 d147734
                if (homeRemoteWrapperImplName != null) {
                    classNameToLoad = homeRemoteWrapperImplName;
                    bmd.homeRemoteImplClass = JITDeploy.generateEJBWrapper(bmd.classLoader,
                                                                           classNameToLoad,
                                                                           new Class<?>[] { bmd.homeInterfaceClass },
                                                                           EJBWrapperType.REMOTE_HOME, // d413752
                                                                           remoteHomeMethods, // d413752
                                                                           bmd.homeMethodInfos, // d367572.4
                                                                           bmd.homeBeanClass.getName(),
                                                                           bmd.j2eeName.toString(), // d443878
                                                                           runtime.getClassDefiner()); // F70650

                    // Only generate the Tie if remote is supported (CORBA classes are available)
                    if (runtime.isRemoteSupported()) {
                        bmd.homeRemoteTieClass = JITDeploy.generate_Tie(classLoader,
                                                                        classNameToLoad,
                                                                        bmd.homeInterfaceClass,
                                                                        bmd.j2eeName.toString(), // d443878
                                                                        runtime.getClassDefiner(), // F70650
                                                                        mmd.getRMICCompatible(),
                                                                        runtime.isRemoteUsingPortableServer());
                    }
                }

                // load generated local Home ('Wrapper') Impl class - EJSLocal[Type]HomeItf
                //    e.g. EJSLocalCMPTest1Home
                homeLocalWrapperImplName = nameUtil.getHomeLocalImplClassName(); // f111627 d114199 d147734
                if (homeLocalWrapperImplName != null) {
                    classNameToLoad = homeLocalWrapperImplName;
                    bmd.homeLocalImplClass = JITDeploy.generateEJBWrapper(bmd.classLoader,
                                                                          classNameToLoad,
                                                                          new Class<?>[] { bmd.localHomeInterfaceClass },
                                                                          EJBWrapperType.LOCAL_HOME, // d413752
                                                                          localHomeMethods, // d413752
                                                                          bmd.localHomeMethodInfos, // d367572.4
                                                                          bmd.homeBeanClass.getName(),
                                                                          bmd.j2eeName.toString(), // d443878
                                                                          runtime.getClassDefiner()); // F70650
                }
            }

            // ----------------------------------------------------------
            // For module versions <= EJB 2.1, the classes generated by
            // EJBDeploy are still loaded, just as in previous releases.
            // ----------------------------------------------------------
            else {
                // Load the Component Remote Implementation ('Wrapper')
                componentRemoteWrapperImplName = nameUtil.getRemoteImplClassName();
                classNameToLoad = componentRemoteWrapperImplName;
                bmd.remoteImplClass = loadGeneratedClass(classLoader,
                                                         classNameToLoad,
                                                         nameUtil);

                // Load the Component Local Implementation ('Wrapper')
                componentLocalWrapperImplName = nameUtil.getLocalImplClassName(); // d114199 d147734

                classNameToLoad = componentLocalWrapperImplName;
                bmd.localImplClass = loadGeneratedClass(classLoader,
                                                        classNameToLoad,
                                                        nameUtil);

                // For EJB 2.X Entity Beans with Container Managed Persistance
                // (CMP), the Enterprise Bean provided by the customer in the
                // deployment descriptor will be an abstract class. The Concrete
                // beam implementation name must be determined and that class
                // loaded, replacing the customer provided abstract bean
                // implementation class loaded earlier in this method.    f110762.2

                // Calculate module version flag
                // WCCM agrees to contract that version IDs are sequentially assigned.
                boolean isPost11DD = (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_2_0);

                if (isPost11DD && // 114138.3 d147734
                    bmd.type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY &&
                    bmd.cmpVersion == InternalConstants.CMP_VERSION_2_X) {
                    // This is an EJB 2.x CMP Entity Bean.  The bean class loaded
                    // above is abstract. Determine the concrete implementation
                    // name and load it, replacing the abstract one from above.
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, bmd.enterpriseBeanClassName + " is an EJB 2.x CMP Bean");

                    bmd.enterpriseBeanClassName = nameUtil.getConcreteBeanClassName();
                    classNameToLoad = bmd.enterpriseBeanClassName;
                    bmd.enterpriseBeanClass = loadGeneratedClass(classLoader, classNameToLoad, nameUtil); // d183360

                } else if (bmd.type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY) {
                    // This is an EJB 1.x CMP Entity Bean. Persistence will be
                    // handled through a generated persister class which was loaded
                    // elsewhere.                                                  f110762.2
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, bmd.enterpriseBeanClassName + " is an EJB 1.x CMP Bean");
                }

                // Load Home implementation bean if necessary.
                // d147734

                // Not an MDB, so generated name for home bean class and load the
                // generated home class if one was generated by ejbdeploy.
                homeImplName = nameUtil.getHomeBeanClassName(); // d114199 d147734

                if (homeImplName != null) {
                    classNameToLoad = homeImplName;
                    bmd.homeBeanClass = loadGeneratedClass(classLoader, classNameToLoad, nameUtil); // f111627 d147734

                } else if (bmd.type == InternalConstants.TYPE_SINGLETON_SESSION || // F743-508
                           bmd.type == InternalConstants.TYPE_STATELESS_SESSION ||
                           bmd.type == InternalConstants.TYPE_STATEFUL_SESSION) {
                    // Use generic SessionHome when no home is defined.
                    bmd.homeBeanClass = SessionHome.class; // F743-34301.1
                }

                // load generated remote Home ('Wrapper') Impl class - EJSRemote[Type]HomeItf
                //    e.g. EJSRemoteCMPTest1Home
                homeRemoteWrapperImplName = nameUtil.getHomeRemoteImplClassName(); // d114199 d147734
                classNameToLoad = homeRemoteWrapperImplName;
                bmd.homeRemoteImplClass = loadGeneratedClass(classLoader, homeRemoteWrapperImplName, nameUtil); // f111627 d147734

                // load generated local Home ('Wrapper') Impl class - EJSLocal[Type]HomeItf
                //    e.g. EJSLocalCMPTest1Home
                homeLocalWrapperImplName = nameUtil.getHomeLocalImplClassName(); // f111627 d114199 d147734
                classNameToLoad = homeLocalWrapperImplName;
                bmd.homeLocalImplClass = loadGeneratedClass(classLoader, classNameToLoad, nameUtil); // f111627 d147734

            } // End else version <= 2.1 load EJBDeploy generated classes

            if (bmd.ivIndirectLocalProxies && bmd.localHomeInterfaceClass != null) {
                classNameToLoad = EJBWrapperProxy.getProxyClassName(bmd.localHomeInterfaceClass);
                bmd.homeLocalImplProxyConstructor = getWrapperProxyConstructor(bmd, classNameToLoad, bmd.localHomeInterfaceClass, localHomeMethods);

                classNameToLoad = EJBWrapperProxy.getProxyClassName(bmd.localInterfaceClass);
                bmd.localImplProxyConstructor = getWrapperProxyConstructor(bmd, classNameToLoad, bmd.localInterfaceClass, localMethods);
            }

            // If there is a WebService Endpoint, save the name to be used when
            // generating the WebService Endpoint wrapper class.     F743-1756CodRv
            if (bmd.ivHasWebServiceEndpoint) {
                bmd.ivWebServiceEndpointProxyName = nameUtil.getWebServiceEndpointProxyClassName();
            }

            // Trace gnerated implementation classes that we have loaded
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, " **** component remote wrapper impl class = " +
                             componentRemoteWrapperImplName);
                Tr.debug(tc, " **** component local wrapper impl class = " +
                             componentLocalWrapperImplName);
                if (businessRemoteWrapperImplName != null) {
                    Tr.debug(tc, " **** business remote wrapper impl class(es) = " +
                                 Arrays.toString(businessRemoteWrapperImplName));
                } else {
                    Tr.debug(tc, " **** business remote wrapper impl class(es) = null");
                }
                if (businessLocalWrapperImplName != null) {
                    Tr.debug(tc, " **** business local wrapper impl class(es) = " +
                                 Arrays.toString(businessLocalWrapperImplName));
                } else {
                    Tr.debug(tc, " **** business local wrapper impl class(es) = null");
                }
                Tr.debug(tc, " **** home remote wrapper impl class = " +
                             homeRemoteWrapperImplName);
                Tr.debug(tc, " **** home local wrapper impl class = " +
                             homeLocalWrapperImplName);
                Tr.debug(tc, " **** home impl class = " +
                             homeImplName);
            }

        } catch (ClassNotFoundException ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".loadGeneratedImplementationClasses", "4353");
            if (isTraceOn && tc.isEventEnabled())
                Tr.event(tc, "Failed to initialize BeanMetaData instance", ex);
            EJBConfigurationException ecex;
            ecex = new EJBConfigurationException("Bean class " + classNameToLoad + " could not be found or loaded", ex);
            Tr.error(tc, "BEANCLASS_NOT_FOUND_CNTR0075E", classNameToLoad);
            throw ecex;
        } catch (LinkageError ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".loadGeneratedImplementationClasses", "4363");
            if (isTraceOn && tc.isEventEnabled())
                Tr.event(tc, "Failed to initialize BeanMetaData instance", ex);
            EJBConfigurationException ecex;
            ecex = new EJBConfigurationException("Bean class " + classNameToLoad + " could not be loaded", ex);
            Tr.error(tc, "BEANCLASS_NOT_FOUND_CNTR0075E", classNameToLoad);
            throw ecex;
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".loadGeneratedImplementationClasses", "4373");
            ContainerException cex;
            if (classNameToLoad != null) {
                cex = new ContainerException("Bean class " + classNameToLoad + " could not be found or loaded", ex);
                Tr.error(tc, "BEANCLASS_NOT_FOUND_CNTR0075E", classNameToLoad);
            } else {
                cex = new ContainerException("Failed to initialize BeanMetaData instance - " +
                                             "caught Throwable", ex);
                Tr.error(tc, "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                         new Object[] { ex, cex.toString() });
            }
            throw cex;
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "loadGeneratedImplementationClasses");
        }

    } // loadGeneratedImplementationClasses

    /**
     * Loads classes generated by EJBDeploy. Returns null if the specified
     * class name to load is null. <p>
     *
     * The algorithm used to determine the generated class names has changed
     * over time, and this method will account for all of the possible
     * names that may be present in the current EJB jar file. <p>
     *
     * The first time this method is called for a particular EJB, the
     * className parameter is expected to be the class name based on the
     * most recent version. If that name fails to load, then names
     * based on prior version will be attempted until one works, or a
     * ClassNotFoundException is returned. <p>
     *
     * Once a class name algorithm is identified that successfully loads
     * a class, the nameUtil parameter is updated to reflect that
     * algorithm, and thus subsequent calls to this method should
     * specify a class name using the new algoritm. <p>
     *
     * @param classLoader Application class loader to be used
     * @param className Name of the class to load
     * @param nameUtil Name Utility instance used to generate the
     *            name of the generated class to load
     *
     * @return the loaded class or null if the class name was null.
     *
     * @throws ClassNotFoundException if a class by the specified
     *             class name could not be loaded.
     */
    private Class<?> loadGeneratedClass(ClassLoader classLoader,
                                        String className,
                                        NameUtil nameUtil) throws ClassNotFoundException {
        if (className == null)
            return null;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        Class<?> loadedClass = null;
        String loadClassName = className;

        // Exception to throw if we can not load the class and we can not
        // determine name of generated class. Not thrown if this method is
        // able to return the loaded Class object.
        ClassNotFoundException primaryException = new ClassNotFoundException(className); // F743-1752.1

        // -----------------------------------------------------------------------
        // First, try and load the class using the class name as specified.
        //
        // This is different than previous releases, where the hash suffix
        // was first added for EJB 1.1 classes.  Instead, the hash suffix
        // is being added automatically by NameUtil for EJB 1.1 class names,
        // so it doesn't need to be done here any more.
        // -----------------------------------------------------------------------
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "loadGeneratedClass: Loading class = " + loadClassName);

        try {
            loadedClass = classLoader.loadClass(loadClassName);
        } catch (ClassNotFoundException cnfe) {
            // Do not FFDC log this, as this is a normal exception when the
            // class was generated with an older version of EJBDeploy
            // FFDCFilter.processException(ex, CLASS_NAME + ".loadGeneratedClass", "4458");
            primaryException = cnfe;
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "loadGeneratedClass: load failed: " + cnfe);
        }

        // -----------------------------------------------------------------------
        // Second, try and load the class using the modified BuzzHash suffix.
        //
        // For performance, the call to NameUtil to get the modified hash suffix
        // will change the state of the NameUtil instance so that subsequent
        // names obtained for this EJB will use the modified suffix... and the
        // subsequent classes will be loaded by the first attempt above,
        // reducing the number of ClassNotFoundExceptions.
        // -----------------------------------------------------------------------
        if (loadedClass == null) {
            loadClassName = nameUtil.updateFilenameHashCode(loadClassName);
            if (loadClassName != null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "loadGeneratedClass: Loading class = " + loadClassName);
                try {
                    loadedClass = classLoader.loadClass(loadClassName);
                } catch (ClassNotFoundException cnfe) {
                    // Do not FFDC log this, as this is a normal exception when the
                    // class was generated with an older version of EJBDeploy
                    // FFDCFilter.processException(ex, CLASS_NAME + ".loadGeneratedClass", "378");
                    primaryException = cnfe; // F743-1752.1
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "loadGeneratedClass: load failed: " + cnfe);
                }
            }
        }

        // -----------------------------------------------------------------------
        // Third, try and load the class using the EJB 1.1 original class name
        // (i.e. contained no hashcode suffix). The updated name will be null
        // for EJB 2.0 and later module levels.
        //
        // For performance, the call to NameUtil to remove the hash suffix
        // will change the state of the NameUtil instance so that subsequent
        // names obtained for this EJB will contain no suffix... and the
        // subsequent classes will be loaded by the first attempt above,
        // reducing the number of ClassNotFoundExceptions.
        // -----------------------------------------------------------------------
        if (loadedClass == null) {
            loadClassName = nameUtil.updateFilenameHashCode(loadClassName);
            if (loadClassName != null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "loadGeneratedClass: Loading class = " + loadClassName);
                try {
                    loadedClass = classLoader.loadClass(loadClassName);
                } catch (ClassNotFoundException cnfe) {
                    // Do not FFDC log this, as it will be logged by the caller
                    // FFDCFilter.processException(ex, CLASS_NAME + ".loadGeneratedClass", "4520");
                    primaryException = cnfe; // F743-1752.1
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "loadGeneratedClass: load failed: " + cnfe);
                }
            }
        }

        // -----------------------------------------------------------------------
        // Finally, insure an exception is thrown if none of the class load
        // attempts were successful.
        // -----------------------------------------------------------------------
        if (loadedClass == null) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "loadGeneratedClass: all attempts failed: " + primaryException);

            throw primaryException;
        }

        return loadedClass;

    } // loadGeneratedClass

    /**
     * Returns a class loader that can be used to define a proxy for the
     * specified interface.
     *
     * @param bmd the bean metadata
     * @param intf the interface to proxy
     * @return the loader
     * @throws EJBConfigurationException
     */
    private ClassLoader getWrapperProxyClassLoader(BeanMetaData bmd, Class<?> intf) // F58064
                    throws EJBConfigurationException {
        // Wrapper proxies are intended to support application restart scenarios.
        // In order to allow GC of the target EJB class loader when the
        // application stops, the wrapper proxy class must not be defined by the
        // target EJB class loader.  Instead, we directly define the class on the
        // class loader of the interface.

        ClassLoader loader = intf.getClassLoader();

        // Classes defined by the boot class loader (i.e., java.lang.Runnable)
        // have a null class loader.
        if (loader != null) {
            try {
                loader.loadClass(BusinessLocalWrapperProxy.class.getName());
                return loader;
            } catch (ClassNotFoundException ex) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "unable to load EJSLocalWrapperProxy for " + intf.getName() + " from " + loader);
            }
        }

        // The class loader of the interface did not have visibility to our
        // com.ibm.ejs.container classes, which is required.  Attempt to use the
        // server class loader instead.
        loader = bmd.container.getEJBRuntime().getServerClassLoader();

        try {
            if (loader.loadClass(intf.getName()) == intf) {
                return loader;
            }
        } catch (ClassNotFoundException ex) {
            // Nothing.
        }

        // The server class loader did not have visibility to the interface
        // class.  The interface was probably loaded by a non-runtime bundle that
        // didn't import com.ibm.ejs.container.  Just use the application class
        // loader even though this will prevent it from being garbage collected,
        // and it will do the wrong thing for package-private methods in
        // no-interface view.
        return bmd.classLoader; // d727494
    }

    /**
     * Defines a wrapper proxy class and obtains its constructor that accepts a
     * WrapperProxyStte.
     *
     * @param bmd the bean metadata
     * @param proxyClassName the wrapper proxy class name
     * @param interfaceClass the interface class to proxy
     * @param methods the methods to define on the proxy
     * @return the constructor with a WrapperProxyState parameter
     * @throws EJBConfigurationException if a class loader cannot be found
     * @throws ClassNotFoundException if class generation fails
     */
    private Constructor<?> getWrapperProxyConstructor(BeanMetaData bmd,
                                                      String proxyClassName,
                                                      Class<?> interfaceClass,
                                                      Method[] methods) // F58064
                    throws EJBConfigurationException, ClassNotFoundException {
        ClassLoader proxyClassLoader = getWrapperProxyClassLoader(bmd, interfaceClass);
        Class<?>[] interfaces = new Class<?>[] { interfaceClass };
        Class<?> proxyClass = JITDeploy.generateEJBWrapperProxy(proxyClassLoader, proxyClassName, interfaces, methods,
                                                                bmd.container.getEJBRuntime().getClassDefiner()); // F70650

        try {
            return proxyClass.getConstructor(WrapperProxyState.class);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Returns true if the specified method has an empty throws clause.
     *
     * @param method the method to check
     * @return true if the method throws clause is empty
     */

    private static boolean hasEmptyThrowsClause(Method method) {
        for (Class<?> klass : method.getExceptionTypes()) {
            // Per CTS, callback methods can declare unchecked exceptions on the
            // throws clause.
            if (!RuntimeException.class.isAssignableFrom(klass) &&
                !Error.class.isAssignableFrom(klass)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Process the metadata to determine if the current EJB supports timeout
     * callbacks; and determine the specific callback method. <p>
     *
     * An EJB may support timeout callbacks through three configuration
     * options:
     * <ol>
     * <li> javax.ejb.TimedObject interface ('ejbTimeout')
     * <li> <timeout-method> deployment descriptor entry
     * <li> @Timeout annotation on the timeout method
     * </ol>
     *
     * When more than one of the above are specified, they must match
     * exactly. An EJB cannot have more than one timeout callback. <p>
     **/
    // d438133.2
    void processTimeoutMetaData(BeanMetaData bmd) //d738042
                    throws EJBConfigurationException, ContainerException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processTimeoutMetaData : " + bmd.j2eeName);

        // local variable ultimately set as value of bmd.ivTimeoutMethod //d738042
        Method timeoutMethod = null; //d738042

        // -----------------------------------------------------------------------
        // First - determine if the bean implements the TimedObject interface.
        // -----------------------------------------------------------------------
        if (BeanMetaData.svTimedObjectClass.isAssignableFrom(bmd.enterpriseBeanClass)) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "processTimeoutMetaData : implements TimedObject");
            bmd.isTimedObject = true;
        } else {
            // The default is 'false', but if a bean fails to start, and a 2nd
            // attempt is made to start the bean, then insure it is reset to the
            // default value before executing timer metadata processing.    d547849
            bmd.isTimedObject = false;
        }

        // -----------------------------------------------------------------------
        // Second - For Session beans, look for <timeout-method> in XML.
        // -----------------------------------------------------------------------

        String timeoutName = null; //F743-15870
        int timeoutMethodParmCount = -1; //Compiler requires us to initialize variable.

        if (bmd.wccm.enterpriseBean != null &&
            bmd.wccm.enterpriseBean instanceof Session) {
            NamedMethod toMethod = ((Session) bmd.wccm.enterpriseBean).getTimeoutMethod();

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "processTimeoutMetaData : timeout-method = " + toMethod);

            if (toMethod != null) {
                timeoutName = toMethod.getMethodName();
                timeoutMethodParmCount = verifyXMLTimerParmList(toMethod.getMethodParamList(), //F743-15870
                                                                bmd,
                                                                timeoutName,
                                                                true);

                // If the EJB implements the timed object interface, the timed object method must be named "ejbTimeout".
                if (bmd.isTimedObject) {
                    if (!"ejbTimeout".equals(timeoutName)) {
                        Tr.error(tc, "TIMER_BEAN_MUST_IMPLEMENT_EJBTIMEOUT_CNTR0160E",
                                 new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                        throw new EJBConfigurationException("The bean: " + bmd.enterpriseBeanName + " in module: " + bmd._moduleMetaData.ivName + " inherits from TimedObject " +
                                                            "but does not implement the required method: ejbTimeout");
                    }

                    timeoutName = null;
                }

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "processTimeoutMetaData : timeout-method = " +
                                 timeoutName);
            }
        }

        // -----------------------------------------------------------------------
        // Third - If <timeout-method> was specified then loop through the
        //         methods looking for the right one.... or if XML metadata is
        //         NOT complete, thn loop through the methods looking for
        //         all @Timeout annotations.
        // -----------------------------------------------------------------------

        if (timeoutName != null ||
            !bmd.metadataComplete) {
            Method annotationMethod = null;
            Method[] xmlMethods = new Method[2];
            Class<?> clazz = bmd.enterpriseBeanClass;

            Collection<MethodMap.MethodInfo> methodInfos = MethodMap.getAllDeclaredMethods(clazz); // d659779, d666251

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "processTimeoutMetaData : looking through methods of " +
                             clazz.getName() + " : " + methodInfos.size());

            for (MethodMap.MethodInfo methodInfo : methodInfos) {
                Method method = methodInfo.getMethod();

                if (method.getName().equals(timeoutName) &&
                    hasTimeoutCallbackParameters(methodInfo)) {
                    int numParms = methodInfo.getNumParameters();
                    // If the expected number of parameters is unspecified or 0,
                    // defer the selection between no-param and 1-param.
                    if (timeoutMethodParmCount <= 0) {
                        if (xmlMethods[numParms] == null) {
                            xmlMethods[numParms] = method;
                        }
                    } else if (numParms == 1) {
                        if (timeoutMethod == null) {
                            timeoutMethod = method; //d738042
                        }
                    }
                }

                if (!bmd.metadataComplete) {
                    Object annotation = method.getAnnotation(Timeout.class);

                    if (annotation != null) {
                        if (!hasTimeoutCallbackParameters(methodInfo)) {
                            Tr.error(tc, "TIMEOUT_METHOD_MISSING_REQUIRED_PARM_CNTR0158E",
                                     new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName, method.getName() });
                            throw new EJBConfigurationException("The bean: " + bmd.enterpriseBeanName + " in module: " + bmd._moduleMetaData.ivName + " has timeout method: " +
                                                                method.getName() + " configured, but the required javax.ejb.Timer parameter is missing.");
                        }

                        if (bmd.isTimedObject &&
                            !("ejbTimeout".equals(method.getName()))) {
                            Tr.error(tc, "TIMER_BEAN_MUST_IMPLEMENT_EJBTIMEOUT_CNTR0160E",
                                     new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                            throw new EJBConfigurationException("The bean: " + bmd.enterpriseBeanName + " in module: " + bmd._moduleMetaData.ivName + " inherits from TimedObject "
                                                                +
                                                                "but does not implement the required method: ejbTimeout");
                        }

                        if (annotationMethod == null) {
                            annotationMethod = method;
                        } else {
                            Tr.error(tc, "TIMEOUT_ANNOTATION_OVERSPECIFIED_CNTR0161E",
                                     new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                            throw new EJBConfigurationException("The bean: " + bmd.enterpriseBeanName + " in module: " + bmd._moduleMetaData.ivName +
                                                                " has specified the @Timeout annotation on more than one method");
                        }
                    }
                }
            }

            // -----------------------------------------------------------------------
            // Fourth - If <timeout-method> was specified without <method-params>
            //          or with empty <method-params>, we need to pick the best
            //          matching method now.                                   d659928
            // -----------------------------------------------------------------------

            if (timeoutName != null) {
                if (timeoutMethodParmCount == 0) {
                    // Per the spec, "<method-params/>" can refer to either no-param
                    // or 1-param, with preference for no-param.
                    timeoutMethod = xmlMethods[0] != null ? xmlMethods[0] : xmlMethods[1]; // d738042
                } else if (timeoutMethodParmCount == -1) {
                    // The user did not specify <method-params/>.  Prefer to use the
                    // annotated method if possible.  For backwards compatibility,
                    // prefer to use the 1-param method rather than no-param.
                    if (annotationMethod != null &&
                        annotationMethod.getName().equals(timeoutName)) {
                        timeoutMethod = annotationMethod; // d738042
                    } else {
                        timeoutMethod = xmlMethods[1] != null ? xmlMethods[1] : xmlMethods[0]; //d738042
                    }
                }
            }

            // --------------------------------------------------------------------
            // Finally - Perform validation of results of method loop.
            //           Most validation checking is not required when the EJB
            //           implements TimedObject, as java forces the signature.
            // --------------------------------------------------------------------

            if (timeoutName != null &&
                timeoutMethod == null) {
                // The configured timeout method was not found on the bean implementation.
                Tr.error(tc, "CONFIGURED_TIMEOUT_METHOD_NOT_FOUND_CNTR0162E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName, timeoutName });
                throw new EJBConfigurationException("The bean: " + bmd.enterpriseBeanName + " in module: " + bmd._moduleMetaData.ivName +
                                                    " has configured timeout method: " + timeoutName + ", but this method was not found on the bean implementation.");
            }

            if (timeoutMethod != null && //d738042
                annotationMethod != null &&
                timeoutMethod != annotationMethod) {
                // Timeout method configuration conflict:
                // XML has one configured timeout method name, and an @Timeout annotation has a different timeout method name
                Tr.error(tc, "CONFLICTING_TIMEOUT_METHOD_NAMES_CNTR0163E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName, timeoutMethod.getName(), annotationMethod.getName() }); // d738042
                throw new EJBConfigurationException("The bean: " + bmd.enterpriseBeanName + " in module: " + bmd._moduleMetaData.ivName + " has configured timeout method: "
                                                    + timeoutMethod.getName() + " in XML, and timeout method: " + annotationMethod.getName() + " via an @Timeout annotation."); // d738042
            }

            if (timeoutMethod == null) {
                timeoutMethod = annotationMethod; //d738042
            }

            if (timeoutMethod != null) {
                if (!hasEmptyThrowsClause(timeoutMethod)) {
                    Tr.error(tc, "TIMEOUT_METHOD_THROWS_APP_EXCEPTION_CNTR0164E",
                             new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName, timeoutMethod.getName() }); // d738042
                    throw new EJBConfigurationException("The bean: " + bmd.enterpriseBeanName + " in module: " + bmd._moduleMetaData.ivName + " has timeout method "
                                                        + timeoutMethod.getName() + " which must not throw application exceptions."); //d738042
                }

                Class<?> returnType = timeoutMethod.getReturnType(); //d738042
                if (returnType != Void.TYPE) {
                    Tr.error(tc, "TIMEOUT_METHOD_MUST_RETURN_VOID_CNTR0165E",
                             new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName, timeoutMethod.getName() }); //d738042
                    throw new EJBConfigurationException("The bean: " + bmd.enterpriseBeanName + " in module: " + bmd._moduleMetaData.ivName + " has timeout method: "
                                                        + timeoutMethod.getName() + " which must return void."); //d738042
                }

                int modifiers = timeoutMethod.getModifiers(); //d738042
                if (Modifier.isStatic(modifiers) ||
                    Modifier.isFinal(modifiers)) {
                    Tr.error(tc, "TIMEOUT_METHOD_STATIC_OR_FINAL_CNTR0166E",
                             new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName, timeoutMethod.getName() }); //d738042
                    throw new EJBConfigurationException("The bean: " + bmd.enterpriseBeanName + " in module: " + bmd._moduleMetaData.ivName + " has timeout method: "
                                                        + timeoutMethod.getName() + " which must not be declared as static or final."); //d738042
                }

                bmd.isTimedObject = true;

            }
        }

        bmd.ivTimeoutMethod = timeoutMethod; //d738042

        // -----------------------------------------------------------------------
        // If this EJB has been identified as implementing TimedObject or
        // a Timeout method, then the TimerService Scheduler needs to be
        // started and available for use, if not alread.
        // Note: Timer Service does not run in z adjunct.                  d259812
        //
        // For EJBDeployed modules, the EJB runtime already started the timer
        // service during module start if needed.  For JITDEployed modules, the
        // EJB runtime tentatively scanned the database to determine whether to
        // start the timer service during module start.  If it chose not to, then
        // we need to try to start it now so that we can fail the bean
        // initialization if the timer service is unavailable.
        // -----------------------------------------------------------------------

        if (bmd.isTimedObject &&
            !bmd._moduleMetaData.isEJBDeployed()) {
            bmd.container.getEJBRuntime().setupTimers(bmd); // F743-13022
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processTimeoutMetaData : " + bmd.j2eeName +
                        " : isTimedObject = " + bmd.isTimedObject +
                        " : ivTimeoutMethod = " + bmd.ivTimeoutMethod);
    }

    /**
     * Process metadata for automatic created timers from both XML and
     * annotations. If the bean has no automatic timers, then the method has
     * no effect. Otherwise, the list of metadata for timer methods (timeout
     * and automatic) is stored in the component data object in the {@link ComponentDataObjectFields.TIMER_METHODS} field.
     *
     * @param cdo the component data
     * @param bmd the bean metadata
     */
    // F743-506
    private void processAutomaticTimerMetaData(BeanMetaData bmd) throws EJBConfigurationException, ContainerException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processAutomaticTimerMetaData: " + bmd.j2eeName);

        // There are several considerations for how to obtain this metadata:
        //
        // 1.  Schedule/Schedules annotations can exist on super classes, so we
        //     cannot exclusively rely on ASM scanning.
        // 2.  Schedule/Schedules annotations can exist on private methods, and
        //     there is no way to unambiguously specify in XML which class in the
        //     hierarchy contains the method, so we cannot exclusively rely on
        //     the merged view.
        // 3.  We must have a complete view of annotations at application start
        //     time so we can create those timers.  However, we do not want to
        //     load bean classes or super-classes unless absolutely necessary.
        //
        // In order to accurately reason about annotations, we load bean classes.
        // However, to avoid loading every bean class at application startup, we
        // only do so if the merged view tells us we should expect to find
        // automatic timers.

        if (bmd.ivInitData.ivHasScheduleTimers == null) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "processAutomaticTimerMetaData: class scan data not found");
            // If there's no merged view data, then just keep going.
        } else if (!bmd.ivInitData.ivHasScheduleTimers) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "processAutomaticTimerMetaData: no class scan scheduler timers");
            return;
        }

        // The following rules are used for timers:
        //
        // 1.  @Schedule, @Schedules, and <timer/> are all additive when applied
        //     to the same method.
        // 2.  For non-private methods, @Schedule and @Schedules are only
        //     applicable for lowest sub-class (ala transaction attributes).
        // 3.  For private methods, @Schedule and @Schedules are applicable for
        //     any method in the class hierarchy.
        // 4.  For <timer/>, the method is located on the lowest sub-class,
        //     regardless of its visibility.

        //F743-15870
        // Timers can be specified in xml with three types of parm lists:
        //       1) <method-parm>javax.ejb.Timer</method-parm> (ie, 1 parm)
        //       2) <method-parm><method-parm>                 (ie, 0 parm)
        //       3) omitting <method-params> and/or <method-parm> (ie, unspecified parm)
        //
        // Timers with 1 parm can only map to Methods with 1 parm.
        // Timers with 0 parm can only map to Methods with 0 parm.
        // Timers with unspecified parms can map to Methods with 1 or 0 parm.
        List<com.ibm.ws.javaee.dd.ejb.Timer> timers = getAutomaticTimersFromXML(bmd.wccm.enterpriseBean);
        Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timers0ParmByMethodName = new HashMap<String, List<com.ibm.ws.javaee.dd.ejb.Timer>>();
        Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timers1ParmByMethodName = new HashMap<String, List<com.ibm.ws.javaee.dd.ejb.Timer>>();
        Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timersUnspecifiedParmByMethodName = new HashMap<String, List<com.ibm.ws.javaee.dd.ejb.Timer>>();

        // Add each timer specified in xml to the correct list, based on its parms.
        for (com.ibm.ws.javaee.dd.ejb.Timer timer : timers) {
            NamedMethod namedMethod = timer.getTimeoutMethod();
            String methodName = timer.getTimeoutMethod().getMethodName();
            List<String> methodParams = namedMethod.getMethodParamList();
            int parmCount = verifyXMLTimerParmList(methodParams, //F743-15870
                                                   bmd,
                                                   methodName,
                                                   false);

            addTimerToCorrectMap(timers1ParmByMethodName, //F743-15870
                                 timers0ParmByMethodName,
                                 timersUnspecifiedParmByMethodName,
                                 parmCount,
                                 methodName,
                                 timer);
        }

        // Load the bean class.
        Class<?> beanClass = loadCustomerProvidedBeanClass(bmd);

        // Determine if the unspecified timers map to 0 or 1 parm Methods
        if (!timersUnspecifiedParmByMethodName.isEmpty()) {
            mapUnspecifiedTimers(timers0ParmByMethodName,
                                 timers1ParmByMethodName,
                                 timersUnspecifiedParmByMethodName,
                                 beanClass,
                                 bmd);
        }

        // Process timeout metadata.  If we have a timeout method, create a
        // TimerMethodData with a special depth of -1.  This will ensure it is
        // sorted as the first timer method.  This TimerMethodData will be
        // reused below if an automatic timer also exists for the method.
        processTimeoutMetaData(bmd);

        List<TimerMethodData> timerMethods = new ArrayList<TimerMethodData>();
        Method timeoutMethod = getTimeoutMethod(bmd);

        if (timeoutMethod != null) {
            boolean oneParm = timeoutMethod.getParameterTypes().length == 1; //F743-15870
            timerMethods.add(new TimerMethodData(timeoutMethod, -1, oneParm)); //F743-15780
        }

        // Iterate over all methods declared in all classes, and build a list of
        // AutomaticTimerMethod objects for this bean.
        for (MethodMap.MethodInfo methodInfo : MethodMap.getAllDeclaredMethods(beanClass)) {
            Method method = methodInfo.getMethod();
            int depth = methodInfo.getClassDepth();
            String methodName = method.getName();

            TimerMethodData timerMethod = null;
            boolean createdTimerMethod = false;

            if (method.equals(timeoutMethod)) {
                timerMethod = timerMethods.get(0);

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "using timeout method ID for " + method);
            }

            // Process XML timers.  By removing the list of timers from
            // the map, we are ensuring the timers apply to methods on
            // the lowest sub-class only.

            List<com.ibm.ws.javaee.dd.ejb.Timer> timerList;
            boolean methodHas1Parm = false;

            if (hasTimeoutCallbackParameters(methodInfo)) {
                if (methodInfo.getNumParameters() == 0) {
                    timerList = timers0ParmByMethodName.remove(methodName);
                } else {
                    timerList = timers1ParmByMethodName.remove(methodName);
                    methodHas1Parm = true;
                }
            } else {
                timerList = null;
            }

            if (timerList != null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "processing XML timer for " + method);

                if (timerMethod == null) {
                    timerMethod = new TimerMethodData(method, depth, methodHas1Parm); //F743-15870
                    timerMethods.add(timerMethod);
                    createdTimerMethod = true; // d666251
                }

                for (com.ibm.ws.javaee.dd.ejb.Timer timer : timerList) {
                    timerMethod.addAutomaticTimer(processAutomaticTimerFromXML(timer));
                }
            }
            // Check annoations if metadata is NOT complete, and we did NOT
            // have any xml defined timer (0, 1, or undefined parms) mapping
            // to this Method
            else if (!bmd.metadataComplete) {
                // Process the @Schedules annotation, if any.
                Schedules schedulesAnnotation = method.getAnnotation(Schedules.class);
                if (schedulesAnnotation != null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "processing @Schedules for " + method);

                    for (Schedule scheduleAnnotation : schedulesAnnotation.value()) {
                        if (timerMethod == null) {
                            timerMethod = new TimerMethodData(method, depth, methodHas1Parm);
                            timerMethods.add(timerMethod);
                            createdTimerMethod = true; // d666251
                        }

                        timerMethod.addAutomaticTimer(processScheduleAnnotation(timerMethod, method, timerMethods, scheduleAnnotation));
                    }
                }

                // Process the @Schedule annotation, if any.
                Schedule scheduleAnnotation = method.getAnnotation(Schedule.class);
                if (scheduleAnnotation != null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "processing @Schedule for " + method);

                    if (timerMethod == null) {
                        timerMethod = new TimerMethodData(method, depth, methodHas1Parm); //F743-15870
                        timerMethods.add(timerMethod);
                        createdTimerMethod = true; // d666251
                    }

                    timerMethod.addAutomaticTimer(processScheduleAnnotation(timerMethod, method, timerMethods, scheduleAnnotation));
                }
            }

            // d666251 - Validate method signature.
            if (createdTimerMethod) {
                validateTimeoutCallbackMethod(bmd, methodInfo);
            }
        }

        // Diagnose any unsatisfied automatic timers from XML.
        dealWithUnsatisifedXMLTimers(timers1ParmByMethodName, //F743-15870
                                     timers0ParmByMethodName,
                                     bmd);

        if (!timerMethods.isEmpty()) {
            if (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION ||
                bmd.type == InternalConstants.TYPE_MANAGED_BEAN) {
                Tr.error(tc, "AUTOMATIC_TIMER_ON_STATEFUL_SESSION_CNTR0207E",
                         new Object[] { bmd.j2eeName.getComponent(), bmd.j2eeName.getModule() });
                throw new EJBConfigurationException("CNTR0207E: The " + bmd.j2eeName.getComponent()
                                                    + " stateful session bean in the " + bmd.j2eeName.getModule()
                                                    + " module has an automatic timer.");
            }

            // Sort the timer methods, and then assign method IDs.  We must sort
            // methods so that method IDs remain stable for persistent timers even
            // if the JVM changes the order of methods returned by getMethods().
            Collections.sort(timerMethods);

            int methodId = 0;
            for (TimerMethodData timerMethod : timerMethods) {
                timerMethod.ivMethodId = methodId++;
            }

            // Add the timer methods to module metadata so they will be created
            // once all the automatic timers for the module have been processed.
            bmd._moduleMetaData.addAutomaticTimerBean(new AutomaticTimerBean(bmd, timerMethods));

            // Check now if the runtime environment supports timers
            // rather than waiting until the automatic timers are created.
            bmd.container.getEJBRuntime().setupTimers(bmd);
        }

        // Always insert so finishBMDInit does not do extra work by calling
        // processTimeoutMetaData during deferred EJB initialization.
        bmd.ivInitData.ivTimerMethods = timerMethods;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processAutomaticTimerMetaData: " + timerMethods.size());
    }

    //F743-15870
    /**
     * Takes in a set of timers defined in xml without parameter lists, and
     * determines if each of them should map to a 1 parm or a 0 parm method.
     *
     * At a future point in the process, we take each method in the bean's
     * class heirarchy that is eligible to be a timer callback method,
     * and we associate it with any timers that target it.
     *
     * The logic to do this method--to--timers association requires that the
     * timers be sorted into two groups, one for timers targeting methods that
     * take 1 parm, and the other for timers targeting methods that take 0 parms.
     *
     * The point of the mapUnspecifiedTimers method is to take xml timers that
     * haven't explicitly indicated if they are for 0 or 1 parm methods, and
     * figure out which group they belong in.
     *
     * @param timers0ParmByMethodName mapping of method names for methods that
     *            take 0 parms, and the timers that target them
     * @param timers1ParmByMethodName mapping of method names for methods that
     *            take 1 parm, and the timers that target them
     * @param timersUnspecifiedParmByMethodName mapping of method names and the
     *            timers that target them, for timers that have an unspecified parm list in xml
     * @param beanClass the base ejb class
     * @parma bmd BeanMetaData
     */
    private void mapUnspecifiedTimers(Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timers0ParmByMethodName,
                                      Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timers1ParmByMethodName,
                                      Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timersUnspecifiedParmByMethodName,
                                      Class<?> beanClass,
                                      BeanMetaData bmd) throws EJBConfigurationException {
        //F743-15870CodRv
        //There are four scenarios we must deal with here:
        //
        //scenario 1: methodName is defined once in the hierarchy
        //
        //       This is valid.  Timers should be associated with the Method.
        //
        //scenario 2: methodName is defined on both child and parent class,
        //            both methods are non-private, and both methods have the
        //            same parameter list (0 or 1).
        //
        //       This is valid.  The method on the child class overrides the
        //       method on the parent class.  Timers should be associated
        //       with the Method on the child class.
        //
        //scenario 3: methodName is defined on both child and parent class,
        //            at least one method is private, and both methods have
        //            same parameter list (0 or 1).
        //
        //       This is valid.  There is no overriding that takes place because
        //       one of the methods is private, but that doesn't matter.  The
        //       timers should still be associated with the Method on the child
        //       class.
        //
        //scenario 4: methodName is defined in multiple places (once a child and once on parent class,
        //            or twice on the same class, etc), and the two definitions take *different*
        //            parm lists (0 and 1).
        //
        //       This is NOT valid.  This is called ambiguous by the spec
        //       (regardless of whether either the methods is private or not) and
        //       is a deployment error.

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "mapUnspecifiedTimers", new Object[] { timersUnspecifiedParmByMethodName });
        }

        Map<String, Integer> unspecParamCounts = new HashMap<String, Integer>();
        String ambiguousTimer = null;
        for (MethodMap.MethodInfo methodInfo : MethodMap.getAllDeclaredMethods(beanClass)) {
            Method method = methodInfo.getMethod();
            String methodName = methodInfo.getMethod().getName();

            if (timersUnspecifiedParmByMethodName.containsKey(methodName) &&
                hasTimeoutCallbackParameters(methodInfo)) {
                //Method has a signature that makes it a valid recipient of a
                //timer callback, and its got the same name as method that was
                //targeted by undefined xml timers.

                Integer currentCount = unspecParamCounts.get(methodName);

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "method=" + method + ", currentCount=" + currentCount);

                if (currentCount == null) {
                    //scenario #1
                    //This is the first time we've encountered a callback
                    //eligible method of this name in the bean's class
                    //heirarchy. Indicate whether the undefined timers targeting
                    //this method should go into the 0 or 1 parm list.
                    unspecParamCounts.put(methodName, methodInfo.getNumParameters());
                } else {
                    //This is NOT the first time we've encountered a callback
                    //eligible method of this name in the bean's class
                    //heirarchy.  Determine if this is a deployment error or not.
                    if (currentCount != methodInfo.getNumParameters()) {
                        //scenario #4
                        ambiguousTimer = methodName;
                        Tr.error(tc, "AUTOMATIC_TIMER_METHOD_AMBIGUOUS_CNTR0314E",
                                 new Object[] { bmd.j2eeName.getComponent(), bmd.j2eeName.getModule(), methodName });
                    } else {
                        //scenario #2 or scenario #3
                        if (isTraceOn && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Encountered an additional definition of Method **" + methodName + "** in the class hierarchy, but " +
                                         "not flagging this as an ambiguous situation because the method has the same number of parms as the " +
                                         "method of the same name on the child class.");
                        }
                    }
                }
            }
        }

        //Fail if we have any ambiguous timers
        if (ambiguousTimer != null) {

            throw new EJBConfigurationException("CNTR0314: The " + bmd.j2eeName.getComponent()
                                                + " enterprise bean in the " + bmd.j2eeName.getModule()
                                                + " module has an automatic timer configured in the deployment descriptor targeting the " + ambiguousTimer
                                                + " method, but does not indicate if the targeted method accepts zero or one parameters, and both"
                                                + " zero and one parameter versions of the method exist in the bean's class hierarchy, so the"
                                                + " EJB container is unable to determine which of the methods the timer must target.");
        }

        //At this point we have determined which group (0 or 1 parm)
        //each methodName targetted by unspecified xml timers belongs to.
        //
        //Now, add the timers targeting these methodNames to the
        //correct 0 or 1 parm list.
        for (String methodTargetedByUnspecifiedParms : timersUnspecifiedParmByMethodName.keySet()) {
            Integer paramCount = unspecParamCounts.get(methodTargetedByUnspecifiedParms);
            if (paramCount == null) {
                //An unspecified timer was defined in xml, but we failed to associated
                //that with a Method in the bean's class heirarchy
                Tr.error(tc, "AUTOMATIC_TIMER_METHOD_NOT_FOUND_CNTR0210E",
                         new Object[] { bmd.j2eeName.getComponent(), bmd.j2eeName.getModule(), methodTargetedByUnspecifiedParms });

                throw new EJBConfigurationException("CNTR0210: The " + bmd.j2eeName.getComponent()
                                                    + " enterprise bean in the " + bmd.j2eeName.getModule()
                                                    + " module has an automatic timer configured in the deployment descriptor for the " + methodTargetedByUnspecifiedParms
                                                    + " method, but no timeout callback method with that name was found.");
            } else if (paramCount == 0) {
                addUnspecifiedTimersToMap(timers0ParmByMethodName,
                                          methodTargetedByUnspecifiedParms,
                                          timersUnspecifiedParmByMethodName);
            } else {
                addUnspecifiedTimersToMap(timers1ParmByMethodName,
                                          methodTargetedByUnspecifiedParms,
                                          timersUnspecifiedParmByMethodName);
            }
        }

        if (isTraceOn) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "List of xml timers explicitly specifiying 0 parm: **" + timers0ParmByMethodName + "**");
                Tr.debug(tc, "List of xml timers explicitly specifiying 1 parms: **" + timers1ParmByMethodName + "**");
            }
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, "mapUnspecifiedTimers");
            }
        }

    }

    //F743-15780
    /**
     *
     * Gets the set of timers with unspecified parms that are associated with the specified
     * method name, and adds them to the correct group of timers (timers targeting 1 parm
     * methods, or timers targeting 0 parm methods).
     *
     * @param methodToTimers mapping of methodNames to the timers that are associated
     *            with that method; represents either the set of timers targeting 1 parm methods
     *            or the set of timers targeting 0 parm methods
     * @param methodName the name of a method that is targeted by timers defined in
     *            xml with unspecified parameter lists
     * @param timersUnspecifiedParmByMethodName mapping of method names and the timers
     *            with unspecified parameter lists that are associated with those methods
     */
    private void addUnspecifiedTimersToMap(Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> methodToTimers,
                                           String methodName,
                                           Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timersUnspecifiedParmByMethodName) {
        //note: We use .get() instead of .remove() to avoid a ConcurrentAccessException,
        //      because the map is being used in a loop in the caling method
        List<com.ibm.ws.javaee.dd.ejb.Timer> targetingTimers = timersUnspecifiedParmByMethodName.get(methodName);

        List<com.ibm.ws.javaee.dd.ejb.Timer> timerList = methodToTimers.get(methodName);

        if (timerList == null) {
            timerList = new ArrayList<com.ibm.ws.javaee.dd.ejb.Timer>();
            methodToTimers.put(methodName, timerList);
        }

        timerList.addAll(targetingTimers);
    }

    //F743-15870
    /**
     * Verifies that every timer defined in xml was successfully associated
     * with a Method.
     *
     * @param timers1ParmByMethodName List of timers defined in xml with 1 parm.
     * @param timers0ParmByMethodName List of timers defined in xml with 0 parms.
     * @param timersUnspecifiedParmByMethodName List of timers defined in xml with unspecified parms.
     * @param bmd BeanMetaData
     * @throws EJBConfigurationException
     */
    private void dealWithUnsatisifedXMLTimers(Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timers1ParmByMethodName,
                                              Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timers0ParmByMethodName,
                                              BeanMetaData bmd) throws EJBConfigurationException {
        String oneMethodInError = null;

        String last1ParmError = dealWithUnsatisfiedXMLTimers(timers1ParmByMethodName, bmd);
        String last0ParmError = dealWithUnsatisfiedXMLTimers(timers0ParmByMethodName, bmd);

        //We log all the methods-in-error, but we only explicitly callout one of them in the exception, and it doesn't matter which one we pick.
        if (last1ParmError != null) {
            oneMethodInError = last1ParmError;
        } else {
            if (last0ParmError != null) {
                oneMethodInError = last0ParmError;
            }
        }

        if (oneMethodInError != null) {
            throw new EJBConfigurationException("CNTR0210: The " + bmd.j2eeName.getComponent()
                                                + " enterprise bean in the " + bmd.j2eeName.getModule()
                                                + " module has an automatic timer configured in the deployment descriptor for the " + oneMethodInError
                                                + " method, but no timeout callback method with that name was found.");
        }
    }

    //F743-15870
    /**
     * Verifies that all timers of a certain parm type (1, 0, or unspecified parm)
     * were successfully mapped to a Method.
     *
     * @param xmlTimers List of Timer instances representing timers defined in xml.
     * @param bmd BeanMetaData
     * @return
     * @throws EJBConfigurationException
     */
    private String dealWithUnsatisfiedXMLTimers(Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> xmlTimers, BeanMetaData bmd) throws EJBConfigurationException {
        String mostRecentMethodWithError = null;
        for (Map.Entry<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> entry : xmlTimers.entrySet()) {
            for (com.ibm.ws.javaee.dd.ejb.Timer timer : entry.getValue()) {
                String methodName = timer.getTimeoutMethod().getMethodName();
                Tr.error(tc, "AUTOMATIC_TIMER_METHOD_NOT_FOUND_CNTR0210E",
                         new Object[] { bmd.j2eeName.getComponent(), bmd.j2eeName.getModule(), methodName });

                mostRecentMethodWithError = methodName;
            }
        }
        return mostRecentMethodWithError;
    }

    /**
     * Returns true if the specified method has the proper parameter types for
     * a timeout callback method.
     *
     * @param methodInfo the method info
     * @return true if the method has the proper parameter
     */
    private boolean hasTimeoutCallbackParameters(MethodMap.MethodInfo methodInfo) {
        int numParams = methodInfo.getNumParameters();
        return numParams == 0 ||
               (numParams == 1 && methodInfo.getParameterType(0) == Timer.class);
    }

    //F743-15870
    /**
     * Sticks a timer defined in xml into the correct list, based on the number
     * of parameters specified in xml for the timer.
     *
     * @param timers1ParmByMethodName list of timers specified in xml with 1 parm
     * @param timers0ParmByMethodName list of timers specified in xml with 0 parms
     * @param timersUnspecifiedParmByMethodName list of timers specified in xml with undefined parms
     * @param parmCount number of parms explicitily defined for the timer in xml (1, 0, or -1 for undefined)
     * @param methodName name of the Method that should be the recipient of the timer callback
     * @param timer The Timer instance representing the timer defined in xml
     */
    private void addTimerToCorrectMap(Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timers1ParmByMethodName,
                                      Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timers0ParmByMethodName,
                                      Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> timersUnspecifiedParmByMethodName,
                                      int parmCount,
                                      String methodName,
                                      com.ibm.ws.javaee.dd.ejb.Timer timer) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        Map<String, List<com.ibm.ws.javaee.dd.ejb.Timer>> mapToUse = null;
        if (parmCount == 0) {
            mapToUse = timers0ParmByMethodName;
        } else if (parmCount == 1) {
            mapToUse = timers1ParmByMethodName;
        } else {
            mapToUse = timersUnspecifiedParmByMethodName;
        }

        List<com.ibm.ws.javaee.dd.ejb.Timer> timerList = mapToUse.get(methodName);

        if (timerList == null) {
            timerList = new ArrayList<com.ibm.ws.javaee.dd.ejb.Timer>();
            mapToUse.put(methodName, timerList);
        }

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "found XML timer for method " + methodName);
        }
        timerList.add(timer);

    }

    //F743-15870
    /**
     *
     * @param methodParams MethodParams representing the parameters defined in xml for the timer.
     * @param bmd BeanMetaData
     * @param methodName The name of the Method that will recieve the timer callback.
     * @param programaticallyCreatedFlow indicates if the programatically created or automatically created
     *            timer processing invoke the method.
     * @return -1 if the parms are undefined, 0 if explicitly set to no parm, 1 if explicitly set to 1 parm
     * @throws EJBConfigurationException
     */
    private int verifyXMLTimerParmList(List<String> params,
                                       BeanMetaData bmd,
                                       String methodName,
                                       boolean programaticallyCreatedFlow) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        final int UNSPECIFIED = -1;
        final int INVALID = -2;

        int parmCount;
        if (params == null) {
            //<method-parms> tag was omitted from xml
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "The <method-parms> element was omitted from xml, so the " +
                             "parm count is unspecified.");
            }
            parmCount = UNSPECIFIED; // d659928
        } else {
            int size = params.size();
            if (size > 1) {
                //multiple <method-parm> tags specified inside of the <method-params> tag
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The parm list is invalid because more than 1 parameter was specified.");
                }
                parmCount = INVALID; // d659928
            } else if (size < 1) {
                //the <method-params> tag was specified, but the <method-parm> tag inside
                //of it was omitted
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The xml explicitly specified exactly zero parameters.");
                }
                parmCount = 0; // d659928
            } else {
                //both the <method-params> and the <method-parm> tag were specified
                String classTypeOfParm = params.get(0);
                if (classTypeOfParm.equals("javax.ejb.Timer")) {
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The xml explicitly specified exactly one parameter, and it was of type javax.ejb.Timer.");
                    }
                    parmCount = 1;
                } else {
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The xml explicitly specified exactly one parameter, but it was of type **" + classTypeOfParm + "**");
                    }
                    parmCount = INVALID; // d659928
                }
            }
        }

        if (parmCount == INVALID) {
            Object[] args = new Object[] { bmd.j2eeName.getComponent(), bmd.j2eeName.getModule(), methodName };
            String exceptionText;

            if (programaticallyCreatedFlow) {
                Tr.error(tc, "TIMEOUT_METHOD_MISSING_REQUIRED_PARM_CNTR0158E", args);
                exceptionText = "The bean: " + bmd.enterpriseBeanName + " in module: " +
                                bmd._moduleMetaData.ivName + " has timeout method: " + methodName +
                                " configured, but the method parameters are not correct. " +
                                "The method must take zero parameters or a single parameter of type " +
                                "javax.ejb.Timer.";
            } else {
                Tr.error(tc, "AUTOMATIC_TIMER_XML_METHOD_PARAMS_CNTR0208E", args);
                exceptionText = "CNTR0208E: The " + bmd.j2eeName.getComponent()
                                + " enterprise bean in the " + bmd.j2eeName.getModule()
                                + " module has automatic timer metadata in the deployment"
                                + " descriptor for the " + methodName + " method, but the"
                                + " method parameter types are not correct.  The method must"
                                + " take zero parameters or a single parameter of type javax.ejb.Timer.";
            }
            throw new EJBConfigurationException(exceptionText);
        }

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "Returning parmCount of **" + parmCount + "**");
        }
        return parmCount;

    }

    /**
     * Verifies that an automatic timer callback method has been coded properly
     * based on the rules from the specification. <p>
     *
     * The following rules are validated:
     * <ul>
     * <li> must not be static
     * <li> must not be final
     * <li> must have a void return type
     * <li> parameters must either be empty or Timer
     * </ul>
     *
     * @param bmd bean metadata for the bean being processed
     * @param method the session synchronization method
     *
     * @throws EJBConfigurationException thrown if the method has not been
     *             coded properly based on the rules in the specification
     */
    private void validateTimeoutCallbackMethod(BeanMetaData bmd, MethodMap.MethodInfo methodInfo) // d666251
                    throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "validateTimeoutCallbackMethod : " + methodInfo.getMethod());

        Method method = methodInfo.getMethod();
        String specificCause = "";

        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            specificCause += " The 'static' modifier is not allowed.";
        }

        if (Modifier.isFinal(modifiers)) {
            specificCause += " The 'final' modifier is not allowed.";
        }

        if (method.getReturnType() != Void.TYPE) {
            specificCause += " The return type must be 'void'.";
        }

        if (!hasTimeoutCallbackParameters(methodInfo)) {
            specificCause += " The method must take 0 parameters or a Timer parameter.";
        }

        if (!hasEmptyThrowsClause(method)) {
            specificCause += " The method must not throw any exceptions.";
        }

        if (!"".equals(specificCause)) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "validateTimeoutCallbackMethod :" + specificCause);

            Tr.error(tc, "INVALID_TIMEOUT_CALLBACK_SIGNATURE_CNTR0209E",
                     new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName, method });
            throw new EJBConfigurationException("CNTR0327E: The " + method +
                                                " method does not have the required method signature for a" +
                                                " timeout callback method." + specificCause);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "validateTimeoutCallbackMethod : valid");
    }

    /**
     * Get the automatic timers for the specified bean.
     *
     * @param eb the bean, or null
     * @return a non-null list of timer stanzas from the deployment descriptor
     */
    // F743-506
    public static List<com.ibm.ws.javaee.dd.ejb.Timer> getAutomaticTimersFromXML(EnterpriseBean eb) {
        if (eb != null) {
            if (eb.getKindValue() == EnterpriseBean.KIND_SESSION) {
                // F00743.9717
                return ((Session) eb).getTimers();
            }

            if (eb.getKindValue() == EnterpriseBean.KIND_MESSAGE_DRIVEN) {
                // F00743.9717
                return ((MessageDriven) eb).getTimers();
            }
        }

        return Collections.emptyList();
    }

    /**
     * Processes a timer stanza from ejb-jar.xml.
     *
     * @param timer the timer stanza
     * @return the processed automatic timer metadata
     */
    // F743-506
    private static TimerMethodData.AutomaticTimer processAutomaticTimerFromXML(com.ibm.ws.javaee.dd.ejb.Timer timer) {
        TimerSchedule timerSchedule = timer.getSchedule();

        boolean persistent = !timer.isSetPersistent() || timer.isPersistent();
        ScheduleExpression schedule = new ScheduleExpression();

        String year = timerSchedule.getYear();
        if (year != null) {
            schedule.year(year);
        }

        String month = timerSchedule.getMonth();
        if (month != null) {
            schedule.month(month);
        }

        String dayOfMonth = timerSchedule.getDayOfMonth();
        if (dayOfMonth != null) {
            schedule.dayOfMonth(dayOfMonth);
        }

        String dayOfWeek = timerSchedule.getDayOfWeek();
        if (dayOfWeek != null) {
            schedule.dayOfWeek(dayOfWeek);
        }

        String hour = timerSchedule.getHour();
        if (hour != null) {
            schedule.hour(hour);
        }

        String minute = timerSchedule.getMinute();
        if (minute != null) {
            schedule.minute(minute);
        }

        String second = timerSchedule.getSecond();
        if (second != null) {
            schedule.second(second);
        }

        schedule.timezone(timer.getTimezone());

        String start = timer.getStart();
        String end = timer.getEnd();
        Serializable info = timer.getInfo();

        return new TimerMethodData.AutomaticTimer(true, persistent, schedule, start, end, info); // F743-506CodRev
    }

    /**
     * Processes a Schedule annotation.
     *
     * @param timerMethod the existing timer method metadata, or null if no
     *            timer metadata has been created for this method
     * @param method the method for this automatic timer
     * @param timerMethods the list of timer method metadata
     * @param schedule the annotation
     * @return the processed automatic timer metadata
     */
    // F743-506
    private static TimerMethodData.AutomaticTimer processScheduleAnnotation(TimerMethodData timerMethod,
                                                                            Method method,
                                                                            List<TimerMethodData> timerMethods,
                                                                            Schedule annotation) {
        boolean persistent = annotation.persistent();
        ScheduleExpression schedule = new ScheduleExpression().year(annotation.year()).month(annotation.month()).dayOfMonth(annotation.dayOfMonth()).dayOfWeek(annotation.dayOfWeek()).hour(annotation.hour()).minute(annotation.minute()).second(annotation.second());
        String info = annotation.info();
        if (info.length() == 0) {
            info = null;
        }

        // The default value for the annotation is "", which will cause parse
        // errors.  Only set the timezone if it's non-empty.
        if (annotation.timezone().length() != 0) {
            schedule.timezone(annotation.timezone());
        }

        return new TimerMethodData.AutomaticTimer(false, persistent, schedule, null, null, info);
    }

    /**
     * Returns the callable timer Methods for the bean. This includes
     * TimedObject, Timeout, and automatic timers. The resulting Method objects
     * are accessible.
     *
     * @param bmd the bean metadata
     * @return the timer methods, or null if the bean has none
     */
    // F743-506
    private Method[] getTimerMethods(BeanMetaData bmd) {
        List<TimerMethodData> timerMethodList = bmd.ivInitData.ivTimerMethods;
        Method[] timerMethods;

        if (timerMethodList == null) {
            Method timeoutMethod = getTimeoutMethod(bmd);
            timerMethods = timeoutMethod == null ? null : new Method[] { timeoutMethod };
        } else if (timerMethodList.isEmpty()) {
            timerMethods = null;
        } else {
            timerMethods = new Method[timerMethodList.size()];
            for (int i = 0; i < timerMethodList.size(); i++) {
                timerMethods[i] = timerMethodList.get(i).getMethod();
            }
        }

        if (timerMethods != null) {
            AccessibleObject.setAccessible(timerMethods, true);
        }

        return timerMethods;
    }

    /**
     * Returns the ejbTimeout or Timeout method. If the bean implements the
     * TimedObject interface, the result is the ejbTimeout method from that
     * interface. If the bean has a Timeout method, then the result is that
     * method.
     *
     * @param bmd the bean metadata
     * @return the timeout method, or null if the bean does not have one
     */
    // F743-506
    private static Method getTimeoutMethod(BeanMetaData bmd) {
        if (!bmd.isTimedObject) {
            return null;
        }

        if (bmd.ivTimeoutMethod != null) {
            return bmd.ivTimeoutMethod;
        }

        return BeanMetaData.svTimedObjectMethod;
    }

    /**
     * Perform initialization of InterceptorMetaData for a specified
     * BeanMetaData in preparation of activating bean instances of this type.
     * This includes setting BeanMetaData.ivCallbackKind to the correct value,
     * even for EJB 2.1 and earlier beans. Which means this method MUST be called
     * for all module versions to ensure ivCallbackKind is initialized.
     *
     * @param bmd is the BeanMetaData for the EJB. <b>Note: </b> The {@link #finishBMDInit(BeanMetaData, ComponentDataObject, String)} and the
     *            {@link #finishBMDInit(BeanMetaData, ComponentDataObject, InjectionEngine)} methods are required to be called prior to this method.
     *
     * @throws EJBConfigurationException is thrown if an error in interceptor configuration
     *             (either annotation or WCCM) is detected.
     */
    // d367572.3 added entire method. // d367572.4 re-wrote method.
    private void initializeInterceptorMD(BeanMetaData bmd, final Map<Method, ArrayList<EJBMethodInfoImpl>> methodInfoMap) // d430356
                    throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "initializeInterceptorMD for " + bmd.j2eeName);
        }

        InterceptorMetaData imd = null; // d439634

        // Is this a EJB 3 module or later?
        if (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) {
            // d367572.9 start
            // Get the module metadata for the EJB 3 module.
            EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;

            // Have we processed the interceptors and interceptor-binding stanzas
            // in ejb-jar.xml yet?  If not, process them now. We only one to do this
            // once per module rather than for every EJB that is in the module.
            // The assumption here is only 1 thread is starting a single module.
            // Other threads may be starting other modules, but not the one this
            // thread is starting.  In other words, all beans in this module are
            // processed by the thread that is starting the module. Therefore, we
            // do not need to synchronize on the EJBModuleMetaDataImpl object.
            if (mmd.ivReadWCCMInterceptors) {
                // No, we have not, so get WCCM EJBJar object, which represents
                // the ejb-jar.xml file if there was one in the module.
                EJBJar ejbJar = bmd.wccm.ejbjar;

                // Is there a ejb-jar.xml file for this module (there may not be one for EJB 3 modules)?
                if (ejbJar != null) {
                    // Yes, there is a DD, so get the interceptors from it.
                    Interceptors interceptors = ejbJar.getInterceptors(); // d438904
                    if (interceptors != null) {
                        // There is an <interceptors> stanza, so determine how many interceptor
                        // classes are defined in it so that we know the maximum number of entries
                        // to create in the IdentityHashMap.
                        List<Interceptor> interceptorsList = interceptors.getInterceptorList(); // d438904
                        int expectedMaxSize = (interceptorsList != null) ? interceptorsList.size() : 0; // d438904

                        // Is there atleast 1 interceptor class?
                        if (expectedMaxSize > 0) {
                            // Yes, there is alteast 1 interceptor class. Create the IdentityHashMap
                            // to hold metadata about the interceptor methods in each of these classes.
                            mmd.ivInterceptorsMap = new IdentityHashMap<Class<?>, EnumMap<InterceptorMethodKind, List<Method>>>(expectedMaxSize); // d438904

                            // process the interceptors stanza that is in ejb-jar.xml for the module so that
                            // Map of interceptors is populated with meta data needed.
                            mmd.ivInterceptorMap = InterceptorMetaDataHelper.populateInterceptorsMap(bmd.classLoader, interceptors, mmd.ivInterceptorsMap, bmd.j2eeName); // d450431 // d468919
                        }
                    }

                    // Now get assembly descriptor from the DD and process the interceptor bindings.
                    AssemblyDescriptor ad = ejbJar.getAssemblyDescriptor();
                    if (ad != null) {
                        List<InterceptorBinding> bindingList = ad.getInterceptorBinding();
                        if (bindingList != null) {
                            int listSize = bindingList.size(); // d438904
                            if (listSize > 0) {
                                int initialCapacity = (int) (listSize * 1.5); // d438904
                                mmd.ivInterceptorBindingMap = new HashMap<String, List<EJBInterceptorBinding>>(initialCapacity); // d438904
                                InterceptorMetaDataHelper.populateInterceptorBindingMap(bindingList, mmd.ivInterceptorBindingMap);
                            }
                        }
                    }
                }

                // Indicate we have read the interceptor meta data for this module so that we do
                // read it again the next time an EJB from this module is initialized.
                mmd.ivReadWCCMInterceptors = false;
            }
            // d367572.9 end

            // Use factory to create InterceptorMetaData for this EJB.
            // Note, a null reference is returned if this EJB does not require any
            // EJB 3 interceptor method to ever be invoked.
            if (!bmd.isEntityBean()) {
                // Not a CMP, or BMP, so attempt to create InterceptorMetaData for this EJB.
                imd = InterceptorMetaDataFactory.createInterceptorMetaData(this, bmd, methodInfoMap); // d430356
            }
        }

        // Assign InterceptorMetaData or null if there is no interceptor meta data for this EJB.
        bmd.ivInterceptorMetaData = imd;

        // Now set CallbackKind dependent on whether there are interceptors or
        // not and the type of EJB if there are no interceptor methods to invoke.
        if (imd != null) {
            // A InteceptorMetaData object was created, so use CallbackKind of
            // InvocationContext to indicate interceptors do need to be called.
            bmd.ivCallbackKind = CallbackKind.InvocationContext;
        } else if (bmd.isSessionBean()) {
            // No interceptors and bean is a SessionBean. Does it implement the
            // javax.ejb.SessionBean interface?
            if ((SessionBean.class).isAssignableFrom(bmd.enterpriseBeanClass)) {
                // Yes it does, so set CallbackKind to SessionBean.
                bmd.ivCallbackKind = CallbackKind.SessionBean;

                // d453778 get Method object for ejbCreate method if one is implemented
                // by the statless session bean.
                try {
                    Method m = bmd.enterpriseBeanClass.getMethod("ejbCreate", new Class[] {});
                    bmd.ivEjbCreateMethod = m;
                } catch (Throwable t) {
                    //FFDCFilter.processException( t, CLASS_NAME + ".initializeInterceptorMD", "5920", this );
                    // Ignore since method does not exist.
                }
            } else {
                // Set CallbackKind to None to indicate no interceptor or
                // javax.ejb.SessionBean callback methods ever need to be called.
                bmd.ivCallbackKind = CallbackKind.None;
            }
        } else if (bmd.isMessageDrivenBean()) {
            // No interceptors and bean is a MDB. Does it implement the
            // javax.ejb.MessageDrivenBean interface?
            if ((MessageDrivenBean.class).isAssignableFrom(bmd.enterpriseBeanClass)) {
                // Yes it does, so set CallbackKind to MessageDrivenBean.
                bmd.ivCallbackKind = CallbackKind.MessageDrivenBean;

                // d453778 get Method object for ejbCreate method if one is implemented
                // by the MDB class.
                try {
                    Method m = bmd.enterpriseBeanClass.getMethod("ejbCreate", new Class[] {});
                    bmd.ivEjbCreateMethod = m;
                } catch (Throwable t) {
                    //FFDCFilter.processException( t, CLASS_NAME + ".initializeInterceptorMD", "5949", this );
                    // Ignore since method does not exist in MDB class.
                }
            } else {
                // Set CallbackKind to None to indicate no interceptor or
                // javax.ejb.MessageDrivenBean callback methods ever need to be called.
                bmd.ivCallbackKind = CallbackKind.None;
            }
        } else {
            // No interceptors and not a SessionBean or MDB.  Must be a EntityBean. So set
            // the CallbackKind to None since EntityBean do not have EJB 3 interceptors.
            bmd.ivCallbackKind = CallbackKind.None;
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "initializeInterceptorMD set CallbackKind to: " + bmd.ivCallbackKind
                        + " for " + bmd.j2eeName,
                    new Object[] { bmd.ivInterceptorMetaData }); // F743-17630
        }

    } // initializeInterceptorMD

    /**
     * Attempt to automatically enable lightweight transactions for local
     * methods. This is an optimization to avoid the overhead of beginning and
     * ending transactions when we can determine statically that user code will
     * be unable to notice.
     */
    // F61004.1
    private void processAutomaticLightweightTransaction(BeanMetaData bmd, ByteCodeMetaData byteCodeMetaData) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (ContainerProperties.DisableAutomaticLightweightMethods) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "processAutomaticLightweightTransaction: disabled");
            return;
        }

        if (bmd.isLightweight) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "processAutomaticLightweightTransaction: already lightweight");
            return;
        }

        if (bmd.type != InternalConstants.TYPE_STATELESS_SESSION &&
            bmd.type != InternalConstants.TYPE_STATEFUL_SESSION &&
            bmd.type != InternalConstants.TYPE_SINGLETON_SESSION) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "processAutomaticLightweightTransaction: unsupported bean type");
            return;
        }

        if (bmd.localMethodInfos != null) {
            for (EJBMethodInfoImpl methodInfo : bmd.localMethodInfos) {
                Method method = methodInfo.getMethod();
                if (method == null) {
                    // Ignore special-case remove method for component interfaces.
                    continue;
                }

                TransactionAttribute txAttr = methodInfo.getTransactionAttribute();
                ActivitySessionAttribute asAttr = methodInfo.getActivitySessionAttribute();

                if (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION) {
                    if (txAttr != TransactionAttribute.TX_SUPPORTS &&
                        txAttr != TransactionAttribute.TX_NOT_SUPPORTED) {
                        // BMT SFSB always switch transactions (either resume a
                        // sticky global, or start a new local), so it would be safe
                        // to elide the transaction pre/postInvoke.  However, we
                        // don't support BMT due to implementation complexities:
                        // 1.  BMStatefulBeanO.postInvoke relies on ContainerTx,
                        //     which is shared with the outer transaction
                        // 2.  EJSContainer.transitionToStickyGlobalTran "knows" that
                        //     preInvokeActivate started a transaction.
                        //
                        // CMT SFSB are enlisted in global transactions, so it is
                        // only possible to support transaction attributes that start
                        // a new transaction (local or global), though we currently
                        // only support transaction attributes that will start a new
                        // local transactions.  If the EJB doesn't have session
                        // synchronization, we could also support Required or
                        // RequiresNew, but we would need to track whether the bean
                        // is already enlisted to handle the pre/post invoke state
                        // transitions.  For Required, we could allow the
                        // optimization if no global exists.  Neither are currently
                        // worth the effort.  Finally, we don't support Never or
                        // Mandatory since those require preInvoke validation.
                        continue;
                    }

                    // Remove methods are transaction-aware.
                    if (methodInfo.isSFSBRemoveMethod()) {
                        continue;
                    }
                } else {
                    if ((txAttr != TransactionAttribute.TX_BEAN_MANAGED ||
                         bmd.type != InternalConstants.TYPE_SINGLETON_SESSION)
                        &&
                        txAttr != TransactionAttribute.TX_REQUIRED &&
                        txAttr != TransactionAttribute.TX_REQUIRES_NEW &&
                        txAttr != TransactionAttribute.TX_SUPPORTS &&
                        txAttr != TransactionAttribute.TX_NOT_SUPPORTED) {
                        // We only support the transaction attributes that don't
                        // require preInvoke validation.  We do not support BMT SLSB
                        // because BMStatelessBeanO.postInvoke relies on ContainerTx,
                        // which is shared with the outer transaction.
                        continue;
                    }
                }

                if ((asAttr == ActivitySessionAttribute.AS_UNKNOWN ||
                     asAttr == ActivitySessionAttribute.AS_SUPPORTS)
                    &&
                    // Interceptor proxies run arbitrary user code that should run
                    // in the new transaction context.
                    methodInfo.getAroundInterceptorProxies() == null) {
                    ByteCodeMetaData.MethodMetaData byteCodeMethod = byteCodeMetaData.getByteCodeMethodMetaData(method);
                    if (byteCodeMethod != null && byteCodeMethod.ivTrivial) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "enabling lightweight tx for trivial method " + method);
                        methodInfo.isLightweightTxCapable = true;
                    }
                }
            }
        }
    }

    /**
     * Map to convert bean type int values to bean type string
     * values.
     *
     */
    static String nameTypeFromBeanTypeMap[];

    private static final void populateNameTypeFromBeanTypeMap() {
        // array element #0 is unused.  Valid array indexes
        // are #1 - #7
        nameTypeFromBeanTypeMap = new String[9]; // F743-508
        nameTypeFromBeanTypeMap[InternalConstants.TYPE_UNKNOWN] = NameUtil.UNKNOWN;
        nameTypeFromBeanTypeMap[InternalConstants.TYPE_SINGLETON_SESSION] = NameUtil.SINGLETON; // F743-508
        nameTypeFromBeanTypeMap[InternalConstants.TYPE_STATELESS_SESSION] = NameUtil.STATELESS;
        nameTypeFromBeanTypeMap[InternalConstants.TYPE_STATEFUL_SESSION] = NameUtil.STATEFUL;
        nameTypeFromBeanTypeMap[InternalConstants.TYPE_BEAN_MANAGED_ENTITY] = NameUtil.BEAN_MANAGED;
        nameTypeFromBeanTypeMap[InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY] = NameUtil.CONTAINER_MANAGED;
        nameTypeFromBeanTypeMap[InternalConstants.TYPE_MESSAGE_DRIVEN] = NameUtil.MESSAGE_DRIVEN;
        nameTypeFromBeanTypeMap[InternalConstants.TYPE_MANAGED_BEAN] = NameUtil.MANAGED_BEAN;
    }

    /**
     * Map to compress the specialized bean types down to simple
     * bean types (e.g. BMP or CMP -> Entity).
     *
     */
    static int compressBeanTypeMap[];
    static final int Unknown_Type = 0;
    static final int Session_Type = 1;
    static final int Entity_Type = 2;
    static final int Message_Type = 3;
    static final int Managed_Type = 4; // F743-34301

    private static final void populateCompressBeanTypeMap() {
        // array element #0 is unused.  Valid array indexes
        // are #1 - #7
        compressBeanTypeMap = new int[9]; // F743-508
        compressBeanTypeMap[InternalConstants.TYPE_UNKNOWN] = Unknown_Type;
        compressBeanTypeMap[InternalConstants.TYPE_SINGLETON_SESSION] = Session_Type; // F743-508
        compressBeanTypeMap[InternalConstants.TYPE_STATELESS_SESSION] = Session_Type;
        compressBeanTypeMap[InternalConstants.TYPE_STATEFUL_SESSION] = Session_Type;
        compressBeanTypeMap[InternalConstants.TYPE_BEAN_MANAGED_ENTITY] = Entity_Type;
        compressBeanTypeMap[InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY] = Entity_Type;
        compressBeanTypeMap[InternalConstants.TYPE_MESSAGE_DRIVEN] = Message_Type;
        compressBeanTypeMap[InternalConstants.TYPE_MANAGED_BEAN] = Managed_Type; // F743-34301
    }

    /**
     * Initialize the SFSB Init method HashMap for a specified EJB 3 SFSB.
     * Note, this method must only be called if the bean is a SFSB and
     * it is in a EJB 3 module.
     *
     * @param bmd is the BeanMetaData for the SFSB.
     * @param methods is the array of Method objects for all public methods of the EJB.
     *
     * @throws EJBConfigurationException if any customer configuration error is detected.
     */
    // d384182 added entire method.
    // d430356 updated for init-method in xml.
    private void initializeSFSBInitMethodMap(BeanMetaData bmd, Method[] methods) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        String ejbName = bmd.enterpriseBeanName;

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "initializeSFSBInitMethodMap: " + ejbName);
        }

        // Get the init-method from ejb-jar.xml if there is an ejb-jar.xml file.
        // Note, eb is set to null if there is no ejb-jar.xml file.
        List<InitMethod> initList = null;
        EnterpriseBean eb = bmd.wccm.enterpriseBean;
        if (eb != null) {
            // Get init-method list for this Session bean from WCCM and process.
            Session sb = (Session) eb;
            List<InitMethod> imList = sb.getInitMethod();
            initList = imList;
        }

        // Populate HashMap of all of the Init methods for SFSB.
        HashMap<String, String> map = new HashMap<String, String>();
        boolean isSessionBean = (SessionBean.class).isAssignableFrom(bmd.enterpriseBeanClass);
        if (isSessionBean) {
            // The bean is written to EJB 2.1 API or earlier, so the ejbCreate<METHOD> are the
            // only methods that can be annotated as a Init method, but is not required
            // to be annotated as a Init method. See section 4.3.10.1 Stateful Session Beans
            // in the EJB 3 core specifications.
            String ejbCreate = "ejbCreate";
            int ejbCreateLength = ejbCreate.length();

            // Process any init-method from ejb-jar.xml file and verify an init-method
            // provided only specifies a ejbCreate method name.
            if (initList != null) {
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "verify init-method is for ejbCreate of bean written to EJB 2.1 API");
                }

                // Verify an init-method provided only specifies a ejbCreate method name.
                for (InitMethod im : initList) {
                    // Get SFSB method name and verify it is ejbCreate. Don't bother with
                    // verifying whether method exists by signature specified. The code
                    // below automatically treats all ejbCreate methods as an init method.
                    // The JITDeploy code has to match up method signature of the init-method
                    // with the create method on the home.  It will provide an error message if
                    // signature does not match.
                    String method = im.getBeanMethod().getMethodName();
                    if (!ejbCreate.equals(method)) {
                        // CNTR0234E: An initialization method for a stateful session bean can be an ejbCreate&lt;METHOD&gt; method
                        // only when the bean conforms to the Enterprise JavaBeans (EJB) 2.1 or earlier specification levels. Therefore, it
                        // cannot be applied to the {0} method of the {1} enterprise bean.
                        Object[] data = new Object[] { method, bmd.enterpriseBeanName };
                        Tr.error(tc, "INVALID_INIT_ANNOTATION_CNTR0234E", data);
                        throw new EJBConfigurationException("CNTR0234E: An initialization method for a stateful session bean can be an ejbCreate<METHOD> method " +
                                                            "only when the bean conforms to the Enterprise JavaBeans (EJB) 2.1 or earlier specification levels. Therefore, it " +
                                                            "cannot be applied to the " + method + " method of the " + bmd.enterpriseBeanName + " enterprise bean.");
                    }
                }
            }

            // For each public method of the bean.
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "processing public ejbCreate methods written to EJB 2.1 API");
            }

            for (Method m : methods) {
                // Is the method a ejbCreate<METHOD>?
                String methodName = m.getName();
                if (methodName.startsWith(ejbCreate)) {
                    // It is a ejbCreate<METHOD>, so insert a map entry using
                    // create<METHOD> + JDIMethodSignature as the key and
                    // METHOD as the value.
                    String key;
                    if (methodName.length() <= ejbCreateLength) {
                        key = "create" + MethodAttribUtils.jdiMethodSignature(m);
                    } else {
                        String suffix = methodName.substring(ejbCreateLength);
                        key = "create" + suffix + MethodAttribUtils.jdiMethodSignature(m);
                    }

                    map.put(key, m.getName());
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "added key: " + key
                                     + ", for method name = " + m.getName());
                    }
                } else {
                    // Don't bother to look for annotations if all config data
                    // came from xml.
                    if (!bmd.metadataComplete) {
                        // The bean is written to EJB 3 API,  or earlier, so the ejbCreate<METHOD> are the
                        // only methods that can be annotated as a Init method, but is not required
                        // to be annotated as a Init method. See section 4.3.10.1 Stateful Session Beans
                        // in the EJB 3 core specifications.
                        // Not a ejbCreate<METHOD>.  Verify it is not annotated as a Init method.
                        Init initMethod = m.getAnnotation(Init.class);
                        if (initMethod != null) {

                            // CNTR0234E: An initialization method for a stateful session bean can be an ejbCreate&lt;METHOD&gt; method
                            // only when the bean conforms to the Enterprise JavaBeans (EJB) 2.1 or earlier specification levels. Therefore, it
                            // cannot be applied to the {0} method of the {1} enterprise bean.
                            String method = m.toGenericString();
                            Object[] data = new Object[] { method, bmd.enterpriseBeanName };
                            Tr.error(tc, "INVALID_INIT_ANNOTATION_CNTR0234E", data);
                            throw new EJBConfigurationException("CNTR0234E: An initialization method for a stateful session bean can be an ejbCreate<METHOD> method " +
                                                                "only when the bean conforms to the Enterprise JavaBeans (EJB) 2.1 or earlier specification levels. Therefore, it "
                                                                +
                                                                "cannot be applied to the " + method + " method of the " + bmd.enterpriseBeanName + " enterprise bean.");
                        }
                    } else {
                        if (isTraceOn && tc.isDebugEnabled()) {
                            Tr.debug(tc, "metadata complete, skipping Init annotations");
                        }
                    }
                }
            }
        } else {
            // The bean is written to EJB 3 API, so any method can be annotated as an Init method.
            // We only need to populate the HashMap if the EJB 3 bean has either a remote or
            // local home interface. If it has neither,  Init method will never be called,
            // so do not bother populating the map in that case.
            if (bmd.homeInterfaceClass != null || bmd.localHomeInterfaceClass != null) {
                // We have a remote or local home interface, so we need process init methods.
                // Always process any init-method from ejb-jar.xml file.
                if (initList != null) {
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "processing init-method from ejb-jar.xml for bean written to EJB 3.0 API");
                    }
                    for (InitMethod im : initList) {
                        // Attempt to find Method object for the InitMethod.
                        NamedMethod namedMethod = im.getBeanMethod();
                        Method m = DDUtil.findMethod(namedMethod, methods);

                        // Method found for InitMethod?
                        if (m == null) {
                            // CNTR0236E: The {1} enterprise bean has an init-method element, which specifies
                            //  the {0} method. This method is not a public method of this bean.
                            String method = namedMethod.getMethodName();
                            Object[] data = new Object[] { method, bmd.enterpriseBeanName };
                            Tr.error(tc, "INIT_METHOD_NOT_FOUND_CNTR0236E", data);
                            throw new EJBConfigurationException("CNTR0236E: The " + bmd.enterpriseBeanName + " enterprise bean has an init-method element, which " +
                                                                "specifies the " + method + " method. This method is not a public method of this bean.");
                        } else {
                            // Method found. Create a HashMap entry for it using home method name
                            // and JDI method signature as the key and method name as the value.
                            // Note, if Init annotation did not provide home method name, then
                            // just the JDI method signature is used as key and JITDeploy has
                            // to handle this possiblity (this could result in CNTR0235E below).
                            String homeMethodName = im.getCreateMethod().getMethodName();
                            String key = homeMethodName + MethodAttribUtils.jdiMethodSignature(m);
                            String oldValue = map.put(key, m.getName());
                            if (oldValue != null) {
                                // See section 10.1.2.1 Init Annotation for Stateful Session Beans
                                // of EJB 3 simplified view specification.

                                // CNTR0235E: The name of the adapted create&lt;METHOD&gt; method must be specified for either the {0} method
                                // or the {1} method of the {2} enterprise bean.
                                Object[] data = new Object[] { oldValue, m.getName(), bmd.enterpriseBeanName };
                                Tr.error(tc, "AMBIGUOUS_INIT_ANNOTATION_CNTR0235E", data);
                                throw new EJBConfigurationException("CNTR0235E: The name of the adapted create<METHOD> method must be specified for either " +
                                                                    "the " + oldValue + " method or the " + m.getName() + " method of the " + bmd.enterpriseBeanName
                                                                    + " enterprise bean.");
                            }
                        }
                    }
                }

                // Don't bother to look for annotations if all config data
                // came from xml.
                if (!bmd.metadataComplete) {
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "processing Init annotations for bean written to EJB 3.0 API");
                    }

                    // Not metadata-complete, so we need to process all public methods
                    // to look for Init annotation.
                    for (Method m : methods) {
                        // Is this method annotated as an Init method?
                        Init initMethod = m.getAnnotation(Init.class);
                        if (initMethod != null) {
                            // Yes it is, so create a HashMap entry for it using home method name
                            // and JDI method signature as the key and method name as the value.
                            // Note, if Init annotation did not provide home method name, then
                            // just the JDI method signature is used as key and JITDeploy has
                            // to handle this possiblity (this could result in CNTR0235E below).
                            String homeMethodName = initMethod.value();
                            String key = homeMethodName + MethodAttribUtils.jdiMethodSignature(m);
                            String oldValue = map.put(key, m.getName());
                            if (oldValue != null) {
                                // See section 10.1.2.1 Init Annotation for Stateful Session Beans
                                // of EJB 3 simplified view specification.

                                // CNTR0235E: The name of the adapted create&lt;METHOD&gt; method must be specified for either the {0} method
                                // or the {1} method of the {2} enterprise bean.
                                Object[] data = new Object[] { oldValue, m.getName(), bmd.enterpriseBeanName };
                                Tr.error(tc, "AMBIGUOUS_INIT_ANNOTATION_CNTR0235E", data);
                                throw new EJBConfigurationException("CNTR0235E: The name of the adapted create<METHOD> method must be specified for either " +
                                                                    "the " + oldValue + " method or the " + m.getName() + " method of the " + bmd.enterpriseBeanName
                                                                    + " enterprise bean.");
                            }
                        }
                    }
                } else {
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "metadata complete, skipping Init annotations for bean written to EJB 3.0 API");
                    }
                }
            }
        }

        // Put map in BeanMetaData if map is not empty. Otherwise, leave BMD
        // set to null.
        if (!map.isEmpty()) {
            bmd.ivInitMethodMap = map;
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "initializeSFSBInitMethodMap: " + ejbName);
        }

    } //initializeSFSBInitMethodMap

    /**
     * Create a Map of java reflection Method object to ArrayList of EJBMethodInfoImpl
     * There is a map entry for each method of the EJB that appears in one of the
     * EJBMethodInfo arrays created for the EJB. A list of EJBMethodInfo is possible
     * since the method might appear in more than one method interface array that exists
     * for the EJB.
     *
     * @param bmd is the BeanMetaData for the EJB.
     * @param allMethodsOfEJB is the array of Method objects for all public methods of the EJB.
     *
     * @return a Map of java reflection Method object to ArrayList of EJBMethodInfoImpl
     *
     * @throws EJBConfigurationException
     */
    // d430356 - added entire method
    private Map<Method, ArrayList<EJBMethodInfoImpl>> createEJBMethodInfoMap(final BeanMetaData bmd, final Method[] allMethodsOfEJB) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "createEJBMethodInfoMap: " + bmd.enterpriseBeanName);
        }

        final HashMap<Method, ArrayList<EJBMethodInfoImpl>> map = new HashMap<Method, ArrayList<EJBMethodInfoImpl>>();

        // Determine which public methods of the EJB are business methods
        // and populate the EJBMethodInfo map with entries for each business method.
        if (bmd.isMessageDrivenBean()) {
            // As described section 5.4.9 Message Listener Interceptor Methods for Message-Driven
            // Beans, the AroundInvoke business method interceptor methods are supported for
            // message-driven beans. These interceptor methods may be defined on the bean class
            // or on a interceptor class and apply to the handling of the invocation of the
            // bean's message listener method(s).  Therefore, only the methods of the
            // message listener interface need to be processed.
            EJBMethodInfoImpl[] methodInfos = bmd.localMethodInfos;
            for (Method method : allMethodsOfEJB) {
                EJBMethodInfoImpl info = findMethodInEJBMethodInfoArray(methodInfos, method); //d494984.1
                if (info != null) {
                    ArrayList<EJBMethodInfoImpl> list = new ArrayList<EJBMethodInfoImpl>();
                    list.add(info);
                    map.put(method, list);
                }
            }
        } else if (bmd.isSessionBean() || bmd.isManagedBean()) {
            // As described in section 4.3.9 Business Method Delegation, the session bean's
            // business interface, component interface, or web service endpoint defines the
            // business methods callable by a client.  Therefore, only process methods
            // of business, component, and webservices interfaces.
            EJBMethodInfoImpl[] localInfos = bmd.localMethodInfos;
            EJBMethodInfoImpl[] remoteInfos = bmd.methodInfos;
            EJBMethodInfoImpl[] webServiceInfos = bmd.wsEndpointMethodInfos;

            for (Method method : allMethodsOfEJB) {
                ArrayList<EJBMethodInfoImpl> list = new ArrayList<EJBMethodInfoImpl>();
                if (localInfos != null) {
                    EJBMethodInfoImpl info = findMethodInEJBMethodInfoArray(localInfos, method); //d494984.1
                    if (info != null) {
                        list.add(info);
                    }
                }
                if (remoteInfos != null) {
                    EJBMethodInfoImpl info = findMethodInEJBMethodInfoArray(remoteInfos, method); //d494984.1
                    if (info != null) {
                        list.add(info);
                    }
                }
                if (webServiceInfos != null) {
                    EJBMethodInfoImpl info = findMethodInEJBMethodInfoArray(webServiceInfos, method); //d494984.1
                    if (info != null) {
                        list.add(info);
                    }
                }
                if (!list.isEmpty()) {
                    map.put(method, list);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "createEJBMethodInfoMap: " + bmd.enterpriseBeanName);
        }
        return map;
    }

    /**
     * Find the EJBMethodInfo in a specified array of EJBMethodInfo
     * that contains a specified Method object.
     *
     * @param methodInfos is the array of EJBMethodInfo objects to search.
     *
     * @param targetMethod is the target of the search.
     *
     * @return EJBMethodInfoImpl object for the target Method or a null reference if
     *         not found in the array specified by methodInfos parameter.
     */
    // d494984.1 - added entire method
    private EJBMethodInfoImpl findMethodInEJBMethodInfoArray(EJBMethodInfoImpl[] methodInfos, final Method targetMethod) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "findMethodInEJBMethodInfoArray for method \"" + targetMethod.toString() + "\"");
        }

        for (EJBMethodInfoImpl info : methodInfos) {
            if (targetMethod.equals(info.getMethod())) {
                if (isTraceOn && tc.isEntryEnabled()) {
                    Tr.exit(tc, "findMethodInEJBMethodInfoArray found EJBMethodInfoImpl for method \"" + targetMethod.toString() + "\"");
                }
                return info;
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "findMethodInEJBMethodInfoArray did not find EJBMethodInfoImpl for method \"" + targetMethod.toString() + "\"");
        }
        return null;
    }

    /**
     * Flag the SFSB business methods that are either annotated as
     * a remove method or xml indicates it is a remove-method.
     * Note, the caller must only call this method for a EJB 3 SFSB.
     *
     * @throws EJBConfigurationException if configuration error detected.
     */
    // d430356 - added entire method
    private void flagSFSBRemoveMethods(BeanMetaData bmd, final Method[] allMethodsOfEJB, Map<Method, ArrayList<EJBMethodInfoImpl>> methodInfoMap) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        String ejbName = bmd.enterpriseBeanName;

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "flagSFSBRemoveMethods: " + ejbName);
        }

        // First process any method that is annotated as a remove method
        // if metadata complete is false. This is done first since the xml
        // can be used to override the retain if exception setting.
        if (!bmd.metadataComplete) {
            for (Method m : allMethodsOfEJB) {
                // Does this method have the Remove annotation?
                Remove ra = m.getAnnotation(Remove.class);
                if (ra != null) {
                    // find the list of EJBMEthodInfoImpl objects that need to be
                    // flagged as remove methods.
                    ArrayList<EJBMethodInfoImpl> infoList = methodInfoMap.get(m);
                    if (infoList == null) {
                        // CNTR0233E: The {1} enterprise bean specifies an @Remove annotation on the {0} method.
                        // This annotation is not valid because this method is not a business method of this bean.
                        String method = m.toGenericString();
                        Object[] data = new Object[] { method, ejbName };
                        Tr.error(tc, "INVALID_REMOVE_ANNOTATION_CNTR0233E", data);
                        throw new EJBConfigurationException("CNTR0233E:  The " + ejbName + " enterprise bean specifies an @Remove annotation on the " +
                                                            m.toGenericString()
                                                            + " method.  This annotation is not valid because this method is not a business method of this bean.");
                    } else {
                        // Get retainIfException from remove annotation
                        boolean retainIfException = ra.retainIfException();

                        // Flag each of the EJBMethodInfoImpl objects for this business method that
                        // it is a SFSB remove method.
                        for (EJBMethodInfoImpl methodInfo : infoList) {
                            methodInfo.setSFSBRemove(retainIfException);
                            if (isTraceOn && tc.isDebugEnabled()) {
                                Tr.debug(tc, "SFSB @Remove method: " + methodInfo.getInterfaceType()
                                             + " - " + methodInfo.getJDIMethodSignature()
                                             + ", retain-if-exception = " + retainIfException);
                            }
                        }
                    }
                }
            }
        }

        // Now augment or override the results of above processing with remove-method
        // defined in the ejb-jar.xml file (note, eb is set to null if there is no
        // ejb-jar.xml file).
        EnterpriseBean eb = bmd.wccm.enterpriseBean;
        if (eb != null) {
            // Get remove-method list for this Session bean from WCCM and process.
            Session sb = (Session) eb;
            List<RemoveMethod> rmList = sb.getRemoveMethod();
            for (RemoveMethod rm : rmList) {
                NamedMethod namedMethod = rm.getBeanMethod();
                String namedMethodName = namedMethod.getMethodName();
                ArrayList<EJBMethodInfoImpl> infoList = null;

                // Determine if the remove method was specified using style
                // 1, 2, or 3 and build the list of EJBMethodInfos that need
                // to be flagged as remove methods.                          d615325
                if ("*".equals(namedMethodName)) {
                    // style 1 - all methods are considered remove methods
                    infoList = new ArrayList<EJBMethodInfoImpl>(methodInfoMap.size());
                    for (ArrayList<EJBMethodInfoImpl> methodInfoList : methodInfoMap.values()) {
                        infoList.addAll(methodInfoList);
                    }
                } else if (namedMethod.getMethodParamList() == null) {
                    // style 2 - all methods with the same name are remove methods
                    infoList = new ArrayList<EJBMethodInfoImpl>();
                    for (Method method : methodInfoMap.keySet()) {
                        if (method.getName().equals(namedMethodName)) {
                            infoList.addAll(methodInfoMap.get(method));
                        }
                    }
                } else {
                    // style 3 - remove method must match both name and parameters
                    Method m = DDUtil.findMethod(namedMethod, allMethodsOfEJB);
                    if (m != null) {
                        infoList = methodInfoMap.get(m);
                    }
                }

                if (infoList == null || infoList.size() == 0) {
                    // CNTR0233E: Remove annotation can not be applied to method "{0}"
                    // since the method is not a business method of EJB name "{1}".
                    String method = namedMethod.getMethodName();
                    List<String> parms = namedMethod.getMethodParamList();
                    List<String> parmList = parms == null ? Collections.<String> emptyList() : parms;
                    method = method + parmList;
                    Object[] data = new Object[] { method, ejbName };
                    Tr.error(tc, "INVALID_REMOVE_ANNOTATION_CNTR0233E", data);
                    throw new EJBConfigurationException("remove-method annotation can not be applied to method \""
                                                        + method + "\" since the method is not a business method of EJB name \""
                                                        + ejbName + "\"");
                } else {
                    // Determine if retain-if-exception is set in ejb-jar.xml for this EJB.
                    boolean retainIfExceptionIsSet = rm.isSetRetainIfException(); //d454711

                    // Flag each of the EJBMethodInfoImpl objects for this business method that
                    // it is a SFSB remove method.
                    for (EJBMethodInfoImpl methodInfo : infoList) {
                        // Was method annotated as a remove method?
                        if (methodInfo.isSFSBRemoveMethod()) {
                            // Yep, it was annoted with Remove method. Is the ejb-jar.xml
                            // overriding the retain if exception setting?
                            if (retainIfExceptionIsSet) {
                                // Yep, ejb-jar.xml is overriding the retain if exception,
                                // so get setting from xml and set it in EJBMethodInfo object.
                                boolean retainIfException = rm.isRetainIfException();
                                methodInfo.setSFSBRemove(retainIfException);
                                if (isTraceOn && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "SFSB remove-method: " + methodInfo.getInterfaceType()
                                                 + " - " + methodInfo.getJDIMethodSignature()
                                                 + ", ejb-jar.xml overriding retain-if-exception to be " + retainIfException);
                                }
                            }
                        } else {
                            // d454711
                            // Method was not annotated as a remove method, but it is a remove
                            // method in the xml file.  So flag method using information from xml file.
                            // Need to default retain if exception if not specified in xml file.
                            boolean retainIfException = (retainIfExceptionIsSet) ? rm.isRetainIfException() : false; //d454711
                            methodInfo.setSFSBRemove(retainIfException);
                            if (isTraceOn && tc.isDebugEnabled()) {
                                Tr.debug(tc, "SFSB remove-method: " + methodInfo.getInterfaceType()
                                             + " - " + methodInfo.getJDIMethodSignature()
                                             + ", retain-if-exception = " + retainIfException);
                            }
                        }
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "flagSFSBRemoveMethods: " + ejbName);
        }
    }

    /**
     * This method checks for configuration errors in the merged metadata
     * (ie. after WCCM metadata and Annotations metadata have been read and
     * merged. For EJB2.1 and earlier beans we will only log error messages and
     * not throw the EJBConfiguration exception which will cause the application
     * not to start. For EJB3 and later applications we will stop the application
     * from starting so these newer applications are cleaned up immediately.
     *
     * @param bmd is the BeanMetaData for the EJB.
     *
     */
    public void validateMergedMetaData(BeanMetaData bmd) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.entry(tc, "validateMergedMetaData for bean: " + bmd.enterpriseBeanName + " ");
        }

        // Validation #1:
        // Beans must be configured with at least one of the valid interface types or they can never
        // be called to do anything useful.  Also, some interface types are not allowed on some bean types.
        // Note that some of these errors may actually get caught by our xml parser, or be stopped
        // because the current wccm model does not allow certain error situations to occur.   However, the code
        // still covers the complete picture of allowed / disallowed interfaces and should be left this way
        // in case we change parsers or configuration data sources in the future.

        // Load up all the possible bean and home interfaces  Note that several of the validation sections below
        // share bits of this data.
        String localHomeInterface = bmd.ivInitData.ivLocalHomeInterfaceName;
        String localComponentInterface = bmd.ivInitData.ivLocalInterfaceName;
        String remoteHomeInterface = bmd.ivInitData.ivRemoteHomeInterfaceName;
        String remoteComponentInterface = bmd.ivInitData.ivRemoteInterfaceName;
        String[] localBusinessInterfaces = bmd.ivInitData.ivLocalBusinessInterfaceNames;
        String[] remoteBusinessInterfaces = bmd.ivInitData.ivRemoteBusinessInterfaceNames;
        String messageListenerInterface = bmd.ivInitData.ivMessageListenerInterfaceName;

        if (bmd.type == InternalConstants.TYPE_SINGLETON_SESSION) {
            // F743-20281.1 - Component views are not supported for singletons.
            if (remoteHomeInterface != null || localHomeInterface != null) {
                Tr.error(tc, "INVALID_SINGLETON_COMPONENT_INTERFACE_SPECIFIED_CNTR0320E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                throw new EJBConfigurationException("The " + bmd.enterpriseBeanName + " singleton session bean in the " +
                                                    bmd._moduleMetaData.ivName + " module has an interface configured for a component view.");
            }

            if (localHomeInterface == null &&
                remoteHomeInterface == null &&
                localBusinessInterfaces == null &&
                remoteBusinessInterfaces == null &&
                bmd.ivHasWebServiceEndpoint == false && // F743-1756CodRv
                bmd.ivLocalBean == false) {

                Tr.error(tc, "REQUIRED_INTERFACE_MISSING_CNTR0131E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });

                // F743-561 - Unconditionally throw, since a singleton did not exist prior to EJB 3.1.
                throw new EJBConfigurationException("The " + bmd.enterpriseBeanName + " bean in the " +
                                                    bmd._moduleMetaData.ivName + " module does not have" +
                                                    " any configured productive interfaces.");
            }

        }

        // Statlesss and Singleton session beans must have a component, business, or webservice endpoint interface, but
        // Stateless session beans may not have a message listener interfaces.      // F743-508
        if (bmd.type == InternalConstants.TYPE_SINGLETON_SESSION ||
            bmd.type == InternalConstants.TYPE_STATELESS_SESSION) {
            // Since a SLSB at EJB 3.0 or above will always have ivHasWebServiceEndpoint==true,
            // don't need to check for lack of valid interface types.

            if (messageListenerInterface != null) {
                Tr.error(tc, "INVALID_SESSION_BEAN_INTERFACE_SPECIFIED_CNTR0132E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                if (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) {
                    throw new EJBConfigurationException("The " + bmd.enterpriseBeanName + " session bean in the " + bmd._moduleMetaData.ivName
                                                        + " module has an interface configured for a message listener.");
                }
            }
        }

        // Stateful session beans must have a component, or business interfaces, but may not have
        // webservice endpoint or message listener interfaces.
        else if (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION) {
            if (localHomeInterface == null &&
                remoteHomeInterface == null &&
                localBusinessInterfaces == null &&
                remoteBusinessInterfaces == null &&
                bmd.ivLocalBean == false) {
                Tr.error(tc, "REQUIRED_INTERFACE_MISSING_CNTR0131E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                if (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) {
                    throw new EJBConfigurationException("The " + bmd.enterpriseBeanName + " bean in the " + bmd._moduleMetaData.ivName
                                                        + " module does not have any configured productive interfaces.");
                }
            }

            if (messageListenerInterface != null ||
                bmd.ivHasWebServiceEndpoint == true) {
                Tr.error(tc, "INVALID_SESSION_BEAN_INTERFACE_SPECIFIED_CNTR0132E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                if (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) {
                    throw new EJBConfigurationException("The " + bmd.enterpriseBeanName + " session bean in the " + bmd._moduleMetaData.ivName
                                                        + " module has an interface configured for a message listener or Web service endpoint.");
                }
            }
        }

        // Entity beans must have a component interfaces, but may not have any of the other
        // interface types
        else if (bmd.type == InternalConstants.TYPE_BEAN_MANAGED_ENTITY ||
                 bmd.type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY) {
            if ((localHomeInterface == null) && (remoteHomeInterface == null)) {
                Tr.error(tc, "REQUIRED_INTERFACE_MISSING_CNTR0131E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                if (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) {
                    throw new EJBConfigurationException("The " + bmd.enterpriseBeanName + " bean in the " + bmd._moduleMetaData.ivName
                                                        + " module does not have any configured productive interfaces.");
                }
            }

            if (localBusinessInterfaces != null ||
                remoteBusinessInterfaces != null ||
                messageListenerInterface != null ||
                bmd.ivHasWebServiceEndpoint == true || // F743-1756CodRv
                bmd.ivLocalBean == true) {
                Tr.error(tc, "INVALID_ENTITY_BEAN_INTERFACE_SPECIFIED_CNTR0133E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                if (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) {
                    throw new EJBConfigurationException("The " + bmd.enterpriseBeanName + " entity bean in the " + bmd._moduleMetaData.ivName
                                                        + " module has an interface configured for a business, message listener, or Web service endpoint.");
                }
            }
        }

        // Managed beans may only have a name an no-interface view      F743-34301
        else if (bmd.type == InternalConstants.TYPE_MANAGED_BEAN) {
            // No merge metadata to validate... no xml representation
        }

        // Otherwise, this must be an MDB.  MDBs must have a message listener interface, but may
        // not have any of the other interface types, or a home.
        else {
            //d460602 - for pre-EJB3 modules we always default the message listener interface if none is provided.
            // Therefore,the following check is only needed for EJB3 or newer modules.
            if (messageListenerInterface == null &&
                bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) {
                Tr.error(tc, "NO_MESSAGE_LISTENER_INTERFACE_CNTR0126E", // d659661.1
                         new Object[] { bmd.enterpriseBeanName });
                throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                    " message-driven bean (MDB) class does not define a" +
                                                    " message listener interface.");
            }

            if (localComponentInterface != null ||
                remoteComponentInterface != null ||
                localBusinessInterfaces != null ||
                remoteBusinessInterfaces != null ||
                bmd.ivHasWebServiceEndpoint == true || // F743-1756CodRv
                localHomeInterface != null ||
                remoteHomeInterface != null ||
                bmd.ivLocalBean == true) {
                Tr.error(tc, "INVALID_MDB_INTERFACE_SPECIFIED_CNTR0134E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                if (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) {
                    throw new EJBConfigurationException("The " + bmd.enterpriseBeanName + " message driven bean in the " + bmd._moduleMetaData.ivName
                                                        + " module has an interface configured for a component, business, " +
                                                        "Web service endpoint, or home.");
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "validateMergedMetaData");
        }

    } // validateMergedMetadata

    /**
     * Get the extended JPA persistent units injected into either
     * the EJB instance or instance of interceptors created for the
     * EJB instance and set into BeanMetaData. Also, determine
     * if this SFSB has extended persistence context (application
     * or container managed).
     * In addition, if the SFSB has extendedPC, then the
     * activation policy will be set.
     *
     * @param bmd is the BeanMetaData for the EJB.
     */
    //d448783 d465813
    public void findAndProcessExtendedPC(BeanMetaData bmd) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "findAndProcessExtendedPC : " + bmd.j2eeName);

        // Get the JPAComponent object that holds the information about
        // container managed persistent units.
        EJBJPAContainer jpaComponent = bmd.container.getEJBRuntime().getEJBJPAContainer();

        if (jpaComponent == null) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "findAndProcessExtendedPC : JPA disabled");
            return;
        }

        if (bmd.ivJavaColonCompEnvMap == null) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "findAndProcessExtendedPC : java:comp unavailable");
            return;
        }

        // Get a collection of all reference/injection 'binding' objects for
        // this EJB and all associated classes. These represent every XML or
        // annotation reference bound into java:comp/env for this component,
        // which includes class, field, and method level annotations.
        Collection<InjectionBinding<?>> injectionBindings = bmd.ivJavaColonCompEnvMap.values();

        // Build a set of Persistence Unit Ids for all extended type persistence
        // contexts (@PersistenceContext).
        List<Object> jpaExPcBindingList = new ArrayList<Object>(); // Object will be of type JPAPCtxtInjectionBinding
        bmd.ivPersistenceRefNames = bmd.isStatefulSessionBean() ? new HashSet<String>() : null; // F743-30682
        bmd.ivExPcPuIds = jpaComponent.getExtendedContextPuIds(injectionBindings,
                                                               bmd.enterpriseBeanName,
                                                               bmd.ivPersistenceRefNames,
                                                               jpaExPcBindingList); // F743-30682

        if (bmd.ivExPcPuIds.length > 0) {
            // Finding extended PuIds means that this sfsb has a container-managed
            // extended persistence context so we can set the bmd flag.
            bmd.ivHasCMExtendedPersistenceContext = true;

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, bmd.j2eeName +
                             " contains ContainerManaged Extended Persistence Contexts: " +
                             Arrays.toString(bmd.ivExPcPuIds));
        }

        // Now look for @PersistencUnit(s) which indicates application-managed pc
        // (which are always extended).
        bmd.ivHasAppManagedPersistenceContext = jpaComponent.hasAppManagedPC(injectionBindings,
                                                                             bmd.enterpriseBeanName,
                                                                             bmd.ivPersistenceRefNames); // F743-30682

        if (isTraceOn && tc.isDebugEnabled()) {
            if (bmd.ivHasAppManagedPersistenceContext) {
                Tr.debug(tc, bmd.j2eeName + " contains ApplicationManaged Persistence Contexts");
            }
        }

        // Container Managed Extended Persistence Context is only allowed on StatefulSessionBeans
        if (bmd.ivHasCMExtendedPersistenceContext &&
            !bmd.isStatefulSessionBean()) {
            throw new EJBConfigurationException("Extended-scoped persistence unit declared in non-SFSB: " + bmd.j2eeName);
        }

        // Scan CMEX entries to make sure there are not two with the same PU but differing TxSyncronicity.
        // The call to scanForTxSynchronizationCollisions() returns a map identifying what type of Tx Synchronicity
        // each Extended PCtx JPAPuId is associated with.
        if (bmd.ivHasCMExtendedPersistenceContext) {
            try {
                bmd.ivJpaPuIdSTUnsyncSet = jpaComponent.scanForTxSynchronizationCollisions(jpaExPcBindingList);
            } catch (IllegalStateException ise) {
                throw new EJBConfigurationException(ise);
            }
        }

        if (bmd.ivHasCMExtendedPersistenceContext ||
            bmd.ivHasAppManagedPersistenceContext) {
            // d460432
            // SFSB failover is not supported with Extended-scoped Persistence Context   //d448832
            if (bmd.ivSFSBFailover) {
                Tr.error(tc, "SFSB_FAILOVER_NOT_ALLOWED_WITH_EXTEND_PERSIST_CNTX_CNTR0156E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                throw new EJBConfigurationException("Bean: " + bmd.enterpriseBeanName + " in module: " + bmd._moduleMetaData.ivName + " is configured " +
                                                    "for both SFSB failover and extended-scope persistence context support.  This is a configuration conflict.");
            }

            // The ActivationPolicy must be "Once" for SFSBs with extendedPC since we will not be able to
            // passivate SFSB's with extendedPC since the EntityManager may not be serializable.
            // This will override whatever was set back in setActivationLoadPolicy().

            if (bmd.activationPolicy != BeanMetaData.ACTIVATION_POLICY_ONCE) {
                String policyName = (bmd.activationPolicy == BeanMetaData.ACTIVATION_POLICY_TRANSACTION) ? "TRANSACTION" : "ACTIVITY_SESSION";
                Tr.warning(tc, "STATEFUL_PERSISTENCE_CONTEXT_ACTIVATE_ONCE_CNTR0175W",
                           new Object[] { bmd.j2eeName.getComponent(),
                                          bmd.j2eeName.getModule(),
                                          bmd.j2eeName.getApplication(),
                                          policyName }); // LIDB441.5 d479669

                bmd.sessionActivateTran = false;
                bmd.sessionActivateSession = false;
                bmd.activationPolicy = BeanMetaData.ACTIVATION_POLICY_ONCE;

            }

        } // if (bmd.ivHasExtendedPersistenceContext)

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "findAndProcessExtendedPC : " + " CMExtendedPC =  " +
                        bmd.ivHasCMExtendedPersistenceContext + ", AppManagedPC = " +
                        bmd.ivHasAppManagedPersistenceContext);
        }
    } // findAndProcessExtendedPC

    protected abstract void populateBindings(BeanMetaData bmd,
                                             Map<JNDIEnvironmentRefType, Map<String, String>> allBindings,
                                             Map<String, String> envEntryValues,
                                             ResourceRefConfigList resRefList) throws EJBConfigurationException;

    // F743-17630 F743-17630CodRv
    /**
     * Does the work to gather both general data and reference related data
     * required by the bean component.
     *
     * Populates BeanMetaData with a lot of required information, and also
     * generates the ComponentNameSpaceConfiguration instance containing the
     * reference data needed by the InjectionEngine.
     *
     * This method is run exactly once for a given bean component, but the timing
     * of that run depends on the type of module the bean component lives in.
     *
     * When the bean component lives in a pure ejb module, this method will get
     * run as part of finishBMDInit() that occurs during the startModule flow.
     *
     * When the bean component lives in a hybrid module, this method will get
     * run as part of the createMeataData flow.
     */
    public ComponentNameSpaceConfiguration finishBMDInitForReferenceContext(BeanMetaData bmd,
                                                                            String defaultCnrJndiName,
                                                                            WSEJBHandlerResolver wsHandlerResolver) // F743-17630CodRv
                    throws ContainerException, EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "finishBMDInitForReferenceContext: " + bmd.j2eeName); // d640935.1

        // -----------------------------------------------------------------------
        // Load Customer provided classes (i.e. bean interface(s),
        // home interface(s), and bean implementation.
        // ----------------------------------------------------------------------
        loadCustomerProvidedClasses(bmd);

        // -----------------------------------------------------------------------
        // Perform validation of meta data on customer provided classes.
        // -----------------------------------------------------------------------
        boolean checkAppConfigCustom = bmd.isCheckConfig(); // F743-33178
        if (isValidationLoggable(checkAppConfigCustom)) {
            //Validate that business interfaces do not contain the @Asynchronous annotation
            AppConfigChecker.validateAsyncOnInterfaces(bmd.ivBusinessLocalInterfaceClasses, tc);
            AppConfigChecker.validateAsyncOnInterfaces(bmd.ivBusinessRemoteInterfaceClasses, tc);
            AppConfigChecker.validateStatefulTimeoutOnInterfaces(bmd.ivBusinessLocalInterfaceClasses, tc); // F743-6605
            AppConfigChecker.validateStatefulTimeoutOnInterfaces(bmd.ivBusinessRemoteInterfaceClasses, tc); // F743-6605
            AppConfigChecker.validateStatefulTimeoutOnSFSB(bmd, tc); // F743-6605
        }

        // ----------------------------------------------------------------------
        // Process automatic timers
        //
        // F743-506 - Process automatic timers.  Since automatic timers
        // processing is always needed at application startup regardless of
        // deferred EJB initialize, processBean might have already done this
        // processing.
        // ----------------------------------------------------------------------
        if (bmd.ivInitData.ivTimerMethods == null) {
            processAutomaticTimerMetaData(bmd);

            // Automatic timer metadata depends on timeout metadata, so we only need
            // to process timeout metadata if automatic timer processing hasn't
            // already done so.
            if (bmd.ivInitData.ivTimerMethods == null) {
                // ----------------------------------------------------------------
                // Determine if this EJB implements TimedObject or contains a 'timeout'
                // method and initialize the BMD fields for runtime processing. This
                // must be done after loading the customer provided classes, but before
                // processing the method metadata.                               d438133.2
                // ----------------------------------------------------------------
                processTimeoutMetaData(bmd);
            }
        }

        // -----------------------------------------------------------------------
        bmd.accessIntentList = null;
        bmd.isoLevelList = null;
        List<ContainerTransaction> transactionList = null;
        List<MethodPermission> securityList = null; //366845.11.1
        ExcludeList excludeList = null; //366845.11.1
        Method[] localBeanMethods = null;
        Method[] remoteBeanMethods = null;

        // ----------------------------------------------------------------------
        // Compress the specific bean types down to three basic types to simplify the code
        // which follows (e.g. BMP Entity -> Entity,  StatefulSession -> Session)
        // ----------------------------------------------------------------------
        int beanType = compressBeanTypeMap[bmd.type];

        // -----------------------------------------------------------------------
        // Calculate bean metadata specific to each of the three basic bean types
        // -----------------------------------------------------------------------
        processMetadataByBeanType(bmd, beanType);

        // ----------------------------------------------------------------------
        // Create info object to store fully fluffed up resource-reference data.
        // ----------------------------------------------------------------------
        bmd._resourceRefList = bmd.wccm.createResRefList(); // F743-18775 F743-14912

        // ----------------------------------------------------------------------
        // Create transaction configuration objects
        // ----------------------------------------------------------------------
        bmd._localTran = bmd.wccm.createLocalTransactionSettings(); // F743-18775 F743-14912
        bmd._globalTran = bmd.wccm.createGlobalTransactionSettings(); // F743-18775 F743-14912
        setCommitDanglingWork(bmd);

        // -----------------------------------------------------------------------
        // A persister is only needed for EJB 1.x CMP, create the persister
        // only if needed.                                               f110762.2
        //
        // Moved CMP 1.1 pesister creation to this point in the initialization
        // because the connection manager factory uses ComponentMetaData (ie. bmd). d114138.3
        // Also, it depends on bmd._resourceRefList set above.
        // -----------------------------------------------------------------------
        if (bmd.cmpVersion == InternalConstants.CMP_VERSION_1_X) {
            bmd.persister = createCMP11Persister(bmd, defaultCnrJndiName);

            //F001925 start: now that we have a persister, we need to use reflections to determine
            // if 'supportsFluffOnFind' is supported.  For more details, see bmd.supportsFluffOnFind.
            try {
                Method method = bmd.persister.getClass().getDeclaredMethod("supportsFluffOnFind");
                bmd.supportsFluffOnFind = ((Boolean) method.invoke(bmd.persister)).booleanValue();
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Using 'supportsFluffOnFind' for bean with J2EEName = " + bmd.j2eeName);
            } catch (Throwable t) {
                //intentionally left blank since most CMP 1.x persisters will not be using 'supportsFluffOnFind'.
            }
            //F001925 end
        }

        // ----------------------------------------------------------------------
        // Intialize Concurrency Control policies for the bean
        //-----------------------------------------------------------------------
        setConcurrencyControl(bmd); //116394.3

        // ----------------------------------------------------------------------
        // Calculate Bean Pool Limits (ie. may be values from a system
        // property or default values), including min, max, and initial
        // pool size
        // ----------------------------------------------------------------------
        processBeanPoolLimits(bmd);

        // ----------------------------------------------------------------------
        // Handle cached timer data settings
        // ----------------------------------------------------------------------
        processCachedTimerDataSettings(bmd); // F001419

        // ----------------------------------------------------------------------
        // Intialize activation and load policies for the bean.  This
        // must be done before the lines below that depend on optionA setting.
        // ----------------------------------------------------------------------
        setActivationLoadPolicy(bmd);

        // ----------------------------------------------------------------------
        // A LockManager is only needed for Option A Entity beans
        // (or) for Statless beans with a MaxCreation limit.                PK20648
        // Depends on pool limits and activation policies being set
        // above.
        // ----------------------------------------------------------------------
        if (bmd.optionACommitOption || // d173022.11
            bmd.ivMaxCreation > 0) {
            bmd.container.initOptACleanUpLockManager();
        }

        // ----------------------------------------------------------------------
        // Check if any pin policy other than TRANSACTION has been
        // specified,  if specified print a warning
        // ----------------------------------------------------------------------
        checkPinPolicy(bmd);

        //------------------------------------------------------------------------
        // Process metadata for IBM Extensions to EJB spec:
        //
        // 1) DisableFlushBeforeFind
        // 2) DisableEJBStoreForNonDirtyBeans
        // 3) LightWeightLocal
        //------------------------------------------------------------------------
        processEJBExtensionsMetadata(bmd);

        //------------------------------------------------------------------------
        // Process the XML and annotations that specify whether the
        // caller's security identity or a run-as security identity
        // should be used for the execution of the bean's methods.    d366845.11.1
        //------------------------------------------------------------------------
        processSecurityIdentity(bmd);

        //------------------------------------------------------------------------
        // Process the XML that specify the relationship between a
        // security role-link and it's corresponding role.              d366845.11
        //------------------------------------------------------------------------
        processSecurityRoleLink(bmd);

        //-----------------------------------------------------------------------
        // Build up all the method level metadata objects.  This
        // code must come after we have finished the specific processing
        // for each bean type and retrieved the vairous method attribute
        // lists above.  This processing on this call handles Bean
        // methods, Home methods, and CMR methods.
        // ----------------------------------------------------------------------

        // ----------------------------------------------------------------------
        // Get list of all methods that have TX or Security attributes from WCCM
        // ----------------------------------------------------------------------
        EJBJar ejbjar = bmd.wccm.ejbjar; //89981
        if (ejbjar != null) {
            AssemblyDescriptor assemblyDescriptor = ejbjar.getAssemblyDescriptor();
            if (assemblyDescriptor != null) {
                transactionList = assemblyDescriptor.getContainerTransactions();
                securityList = assemblyDescriptor.getMethodPermissions(); //d366845.11.1
                excludeList = assemblyDescriptor.getExcludeList(); //d366845.11.1
            }
        }

        // ----------------------------------------------------------------------
        // ActivitySessionAttributes
        //
        // LIDB441.5 - Get the list of all methods that have AttivitySessionAttributes
        // from the ContainerExtensionFactory.  The base implementation should return
        // a list with AS_UNKNOWN, while the PME implementation should parse the real
        // values from the pme extensions xmi.
        //
        // Only container mangaged activity sessions use the AS attributes
        // Also, it is very possible that there are no activity session attributes
        // because they are an optional PME extension.
        // ----------------------------------------------------------------------
        List<ActivitySessionMethod> activitySessionList = null;
        if (!bmd.usesBeanManagedAS) {
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempting to do any needed activitySession work for bean " + bmd.j2eeName.getComponent());
            }

            try {
                activitySessionList = bmd.container.getContainerExtensionFactory().getActivitySessionAttributes(bmd);//d143954 F743-24095
            } catch (Exception ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".finishBMDInit", "913", this);
                ContainerException cex = new ContainerException("Failed to initialize BeanMetaData instance", ex);
                Tr.error(tc, "CAUGHT_EXCEPTION_THROWING_NEW_EXCEPTION_CNTR0035E",
                         new Object[] { ex, cex.toString() }); // d194031
                throw cex;
            }
        } // if ! bean managed AS

        // ----------------------------------------------------------------------
        // Build complete method lists based on the various customer provided interface(s). d369262.3
        // ----------------------------------------------------------------------
        localBeanMethods = DeploymentUtil.getMethods(bmd.localInterfaceClass,
                                                     bmd.ivBusinessLocalInterfaceClasses);

        if (bmd.type == InternalConstants.TYPE_MESSAGE_DRIVEN) {
            // If this is a no-method interface MDB, we need to collect the methods off the bean class instead of the interface.
            if (bmd.container.isNoMethodInterfaceMDBEnabled() && localBeanMethods.length == 0) {
                localBeanMethods = DeploymentUtil.getMethods(null, new Class[] { bmd.enterpriseBeanClass });
                bmd.ivIsNoMethodInterfaceMDB = true;
            }

            // For MDBs using an ActivationSpec, remove any message listener interface methods that
            // conflict with the MessageEndpoint interface methods; they cannot be overridden.
            if (bmd.ivActivationSpecJndiName != null) {
                localBeanMethods = removeMessageEndpointMethods(localBeanMethods);
            }
        }

        remoteBeanMethods = DeploymentUtil.getMethods(bmd.remoteInterfaceClass,
                                                      bmd.ivBusinessRemoteInterfaceClasses);
        Method[] webserviceMethods = DeploymentUtil.getMethods(bmd.webserviceEndpointInterfaceClass,
                                                               null);
        Method[] localHomeMethods = DeploymentUtil.getMethods(bmd.localHomeInterfaceClass,
                                                              null);
        Method[] remoteHomeMethods = DeploymentUtil.getMethods(bmd.homeInterfaceClass,
                                                               null);
        Method[] timerMethods = getTimerMethods(bmd); // F743-506

        // d630468 - If the only webservice endpoint for the bean is the JAX-RPC
        // service-endpoint declared in XML, then webserviceMethods only contains
        // the methods from that interface.  If the bean has an undeclared
        // JAX-RPC service endpoint or is declared as a JAX-WS provider, then we
        // won't know the full list of webservice methods until they are provided
        // by the webservices engine later.  Collect all public methods on the
        // bean class now and filter out the unneeded ones then.
        if (bmd.ivHasWebServiceEndpoint &&
            (bmd.webserviceEndpointInterfaceClass == null ||
             bmd.ivInitData.ivWebServiceEndpoint)) {
            webserviceMethods = DeploymentUtil.getMethods(bmd.enterpriseBeanClass, null);
        }

        // Get all the public methods of the bean class including the
        // methods inherited from the super class of bean class.
        Method[] allPublicMethods = bmd.enterpriseBeanClass.getMethods(); // d430356

        ByteCodeMetaData byteCodeMetaData = new ByteCodeMetaData(bmd.enterpriseBeanClass, allPublicMethods); // F61004.1

        // ----------------------------------------------------------------------
        // Calculate all the method metadata
        // ----------------------------------------------------------------------
        processMethodMetadata(bmd,
                              byteCodeMetaData,
                              beanType,
                              transactionList,
                              securityList,
                              excludeList,
                              activitySessionList,
                              localBeanMethods,
                              remoteBeanMethods,
                              timerMethods,
                              webserviceMethods,
                              localHomeMethods,
                              remoteHomeMethods,
                              allPublicMethods); // d494984

        //------------------------------------------------------------------------
        // If running in a clustered (wlm) environment, and there is either
        // a remote home, remote interface, or remote business
        // interface(s), then obtain the Cluster Identity
        // (name), which will be used to register the remote wrappers
        // (registerServant).  Note that this value is constant for all Remote
        // objects associated witha a specific J2EEName, except for instances
        // of Stateful Session beans.  Stateful beans may not exist on more
        // than one process, so are not WLMable in the normal sense.
        // For Stateful failover, a Cluster Identity must be obtained for
        // eache bean instance.                                          LI2401-11, d461515
        //------------------------------------------------------------------------
        if (bmd.remoteInterfaceClass != null ||
            bmd.homeInterfaceClass != null ||
            bmd.ivBusinessRemoteInterfaceClasses != null) {
            bmd.ivCluster = bmd.container.getEJBRuntime().getClusterIdentity(bmd.j2eeName);
        }

        // ----------------------------------------------------------------------
        // Create method--TO--MethodInfo[] mapping
        ///
        // Build a map where the map key is the Method object of a public method of the EJB and
        // the map value is an ArrayList of EJBMethodInfoImpl objects. We have one implementation
        // of the method in the EJB class, but that method could appear in more than one
        // interface (remote or local component or business interface, thus there are
        // multiple EJBMethodInfo arrays to be processed).
        // ----------------------------------------------------------------------
        final Map<Method, ArrayList<EJBMethodInfoImpl>> methodInfoMap = createEJBMethodInfoMap(bmd, allPublicMethods); // d430356

        //------------------------------------------------------------------------
        // If this is a SFSB, determine if there are any @Init methods for this
        // EJB and create any metadata necessary for JITDeploy to be able to
        // generate the objects that invoke those methods.
        //------------------------------------------------------------------------
        if (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION &&
            !bmd._moduleMetaData.isEJBDeployed()) {
            initializeSFSBInitMethodMap(bmd, allPublicMethods);

            // For EJB 3 modules, also flag the EJBMethodInfo object for each of
            // the @Remove method in SFSB.
            if (bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) {
                flagSFSBRemoveMethods(bmd, allPublicMethods, methodInfoMap); // d430356
            }
        }

        //------------------------------------------------------------------------
        // Create interceptor meta data.
        //
        // Now that we have EJBMethodInfoImpl and the rest of BeanMetaData fields
        // finished, we can do the InterceptorMetaData that may be needed for this EJB.
        // Must follow initialization of method metadata.
        //------------------------------------------------------------------------
        initializeInterceptorMD(bmd, methodInfoMap); //d367572.4 // d430356

        if (bmd.ivInterceptorMetaData != null && bmd.ivInterceptorMetaData.ivBeanLifecycleMethods != null) {
            initializeLifecycleInterceptorMethodMD(bmd.ivInterceptorMetaData.ivBeanLifecycleMethods,
                                                   transactionList,
                                                   activitySessionList,
                                                   bmd);
        }

        // -----------------------------------------------------------------------
        // Attempt to automatically enable lightweight transactions for local methods. F61004.1
        // -----------------------------------------------------------------------
        processAutomaticLightweightTransaction(bmd, byteCodeMetaData);

        // ----------------------------------------------------------------------
        // Add the bean class itself to the list of classes 'in play' for reference processing.
        // ----------------------------------------------------------------------
        List<Class<?>> classesInPlay = new ArrayList<Class<?>>();
        classesInPlay.add(bmd.enterpriseBeanClass);

        ResourceRefConfigList resRefList = bmd._resourceRefList; // F743-18775
        Map<JNDIEnvironmentRefType, Map<String, String>> allBindings = JNDIEnvironmentRefBindingHelper.createAllBindingsMap();
        Map<String, String> envEntryValues = new HashMap<String, String>();

        populateBindings(bmd, allBindings, envEntryValues, resRefList);

        InterceptorMetaData imd = bmd.ivInterceptorMetaData;
        List<Class<?>> interceptorClassList = null;
        if (imd != null) {
            // There is atleast 1 interceptor class for this EJB.  So add the classes
            // to the injectionClasses list.
            interceptorClassList = Arrays.asList(imd.ivInterceptorClasses);
            classesInPlay.addAll(interceptorClassList);
        }

        // JAX-WS Handlers are part of the EJB's component namespace
        // (java:comp/env), so check with WebServices if there are any
        // handler classes for this Stateless/Singleton EJB, and if so, add them
        // to the list of 'injection' classes.                          d495644
        if ((bmd.ivModuleVersion >= BeanMetaData.J2EE_EJB_VERSION_3_0) &&
            (wsHandlerResolver != null) &&
            (bmd.type == InternalConstants.TYPE_SINGLETON_SESSION || // F743-508
             bmd.type == InternalConstants.TYPE_STATELESS_SESSION)) {
            List<Class<?>> handlerClasses = wsHandlerResolver.retrieveJAXWSHandlers(bmd.j2eeName);
            if (handlerClasses != null) {
                classesInPlay.addAll(handlerClasses);
            }
        }

        //---------------------------------------------------------
        // Get the WCCM artifacts relavent to refeference processing
        //----------------------------------------------------------
        String displayName = null;
        Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> allRefs = new EnumMap<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>>(JNDIEnvironmentRefType.class);

        // get WCCM artifacts associated with bean itself
        EnterpriseBean enterpriseBean = bmd.wccm.enterpriseBean;
        if (enterpriseBean != null) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "adding references from enterprise bean: " + enterpriseBean);

            List<DisplayName> displayNames = enterpriseBean.getDisplayNames();
            displayName = displayNames.isEmpty() ? null : displayNames.get(0).getValue();
            JNDIEnvironmentRefType.addAllRefs(allRefs, enterpriseBean);
        }

        // get WCCM artifacts associated with any interceptor classes
        // If there were interceptor classes defined in ejb-jar.xml file,
        // then update the WCCM reference lists with the references from
        // each interceptor class that is bound to the enterprise bean class.
        Map<String, Interceptor> interceptorMap = bmd._moduleMetaData.ivInterceptorMap;
        if ((interceptorMap != null) &&
            (interceptorClassList != null) &&
            (interceptorClassList.size() > 0)) {
            // There is atleast one interceptor class bound to the EJB class,
            // so do for each interceptor class in list passed to this method.
            for (Class<?> interceptorClass : interceptorClassList) {
                // Get the fully qualified interceptor class name and lookup in the
                // map passed to this method the WCCM Interceptor object associated
                // with the interceptor class name.
                String name = interceptorClass.getName();
                Interceptor interceptor = interceptorMap.get(name);

                // Only process if found in the interceptor map. If will not be found
                // if the Interceptor is reference via the @Inteceptor annotation
                // rather than the <interceptor> element in ejb-jar.xml file for the module.
                if (interceptor != null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "adding references from interceptor: " + interceptor);

                    // Found in the map, so it is in ejb-jar.xml which is why we
                    // have a WCCM Interceptor object to process.  So update each
                    // of the reference lists with references for this Inteceptor class.
                    JNDIEnvironmentRefType.addAllRefs(allRefs, interceptor);
                }
            }
        }

        //-------------------------------------------
        //Ensure display name is set correctly
        //-------------------------------------------
        if (displayName == null || displayName.equals("")) {
            displayName = bmd.j2eeName.getComponent();
        } else if (!displayName.equals(bmd.j2eeName.getComponent())) {
            displayName = displayName + " (" + bmd.j2eeName.getComponent() + ")";
        }

        //-----------------------------------------------------------------------
        // Stick Method related data into BMD so we can get at during .finishBMDInit()
        // ----------------------------------------------------------------------
        bmd.methodsExposedOnLocalInterface = localBeanMethods;
        bmd.methodsExposedOnRemoteInterface = remoteBeanMethods;
        bmd.methodsExposedOnLocalHomeInterface = localHomeMethods;
        bmd.methodsExposedOnRemoteHomeInterface = remoteHomeMethods;
        bmd.allPublicMethodsOnBean = allPublicMethods;
        bmd.methodsToMethodInfos = methodInfoMap;

        //-----------------------------------------------------------------------
        // Create and populate ComponentNameSpaceConfiguration
        //-----------------------------------------------------------------------
        EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;
        String logicalAppName = mmd.getEJBApplicationMetaData().getLogicalName(); // F743-23167
        String logicalModuleName = mmd.ivLogicalName; // F743-23167
        Object loadStrategy = bmd.wccm.getModuleLoadStrategy(); // F68113.4
        UserTransaction ejbUserTransaction = bmd.usesBeanManagedTx ? UserTransactionWrapper.INSTANCE : null; // F743-17630.1, d631349
        ComponentNameSpaceConfiguration.ReferenceFlowKind owningFlow = bmd.type == InternalConstants.TYPE_MANAGED_BEAN ? ComponentNameSpaceConfiguration.ReferenceFlowKind.MANAGED_BEAN // d705480
                        : ComponentNameSpaceConfiguration.ReferenceFlowKind.EJB;

        ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(displayName, bmd.getJ2EEName()); // F48603.7
        compNSConfig.setLogicalModuleName(logicalAppName, logicalModuleName); // F743-23167
        compNSConfig.setOwningFlow(owningFlow);
        compNSConfig.setCheckApplicationConfiguration(checkAppConfigCustom); // F743-33178

        compNSConfig.setClassLoader(bmd.classLoader);
        compNSConfig.setModuleMetaData(bmd.getModuleMetaData());
        compNSConfig.setModuleLoadStrategy(loadStrategy);
        compNSConfig.setComponentMetaData(bmd); // F743-31658
        compNSConfig.setMetaDataComplete(bmd.metadataComplete);
        compNSConfig.setInjectionClasses(classesInPlay);
        compNSConfig.setEJBTransaction(ejbUserTransaction);
        compNSConfig.setUsesActivitySessions(bmd.usesBeanManagedAS);
        compNSConfig.setSFSB(bmd.isStatefulSessionBean());

        JNDIEnvironmentRefType.setAllRefs(compNSConfig, allRefs);
        JNDIEnvironmentRefBindingHelper.setAllBndAndExt(compNSConfig, allBindings, envEntryValues, resRefList);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "finishBMDInitForReferenceContext: " + compNSConfig.toDumpString());
        return compNSConfig;
    }

    // F743-17630CodRv
    /**
     * Ensure that reference data is processed, and that the resulting
     * output data structures are stuffed in BeanMetaData.
     *
     * This method gets called during finishBMDInit. If the bean component is part
     * of a pure module, then its reference data has not been processed yet,
     * and we go ahead and do that now.
     *
     * However, if the bean component is part of a hybrid module, then its reference
     * data was processed between the createMetaData and startModule flows, and so
     * we do not repeat that work.
     *
     * In either case, the InjectionEngine processing is completed, and then the
     * resulting output data structures are persisted into BeanMetaData for future use.
     */
    protected void processReferenceContext(BeanMetaData bmd) throws InjectionException, EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "ensureReferenceDataIsProcessed");

        // At this point, regardless of whether we are in a pure or hybrid flow,
        // we should have a populated ReferenceContext.
        //
        // Now we attempt to process it.  If we are in a pure flow, processing
        // will actually occur.  If we are in a hybrid flow, processing will
        // occur if it has not been done yet (via the webcontainer, or via
        // some other bean component).  If it has already been done, this
        // call will no-op...but thats a black box to us here, and we don't
        // know are care if processing actually runs now, or if it already
        // ran earlier in time.
        ReferenceContext refContext = bmd.ivReferenceContext;
        refContext.process();

        // Persist output data structures into BMD
        bmd._javaNameSpaceContext = refContext.getJavaCompContext(); // F743-17630CodRv
        bmd.setJavaNameSpace(refContext.getComponentNameSpace());
        bmd._resourceRefList = refContext.getResolvedResourceRefs();
        bmd.ivJavaColonCompEnvMap = refContext.getJavaColonCompEnvMap();
        bmd.envProps = refContext.getEJBContext10Properties(); // F743-17630CodRv

        // F743-21481
        // Stick InjectionTargets that are visible to bean class into BMD
        bmd.ivBeanInjectionTargets = getInjectionTargets(bmd, bmd.enterpriseBeanClass);

        // F743-21481
        // Stick the InjectionTargets that are visible to each interceptor
        // class into the InterceptorMetaData
        InterceptorMetaData imd = bmd.ivInterceptorMetaData;
        if (imd != null) {
            Class<?>[] interceptorClasses = imd.ivInterceptorClasses;
            if (interceptorClasses != null && interceptorClasses.length > 0) {
                InjectionTarget[][] interceptorInjectionTargets = new InjectionTarget[interceptorClasses.length][0];

                for (int i = 0; i < interceptorClasses.length; i++) {
                    // The InjectionTarget[] associated with the interceptor class is
                    // empty (ie, 0 length array) if there are no visible
                    // InjectionTargets. Its added to the interceptorInjectionTargets
                    // array anyway because this array needs to have the same length
                    // as the IMD.ivInterceptorClasses array...ie, each 'row' in this
                    // array corresponds directly to a class in that array, and if the
                    // two arrays get out-of-sync, then we have a problem.
                    Class<?> oneInterceptorClass = interceptorClasses[i];
                    InjectionTarget[] targets = getInjectionTargets(bmd, oneInterceptorClass);
                    interceptorInjectionTargets[i] = targets;
                }

                imd.ivInterceptorInjectionTargets = interceptorInjectionTargets;
            }

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Interceptor metadata after adding any InjectionTargets:\n" + imd);
        }

        if (!EJSPlatformHelper.isZOSCRA()) {
            // Verify that a MDB or stateless session bean instance or the
            // interceptor instances of MDB and SLSB are not the target of a
            // extended persistent context injection since extended persistent
            // unit is only allowed for the SFSB and it's interceptor instances.
            findAndProcessExtendedPC(bmd); //465813
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "ensureReferenceDataIsProcessed");
    }

    /**
     * Obtains the list of injection targets from the ReferencContext
     * for the specified bean class and performs EJB Specification
     * specific post processing and validation. <p>
     *
     * Sorts all injection target methods for EJBContext to the beginning
     * of the provided injection target list. Also removes the set context
     * methods declared by SessionBean and MessageDrivenBean; as the
     * container calls these separate from injection. <p>
     *
     * The EJB Specification requires that injection methods for EJBContext
     * are called prior to performing any other injection. This is only vaguely
     * implied in recent versions of the specification, but was fairly clear in
     * the EJB 3.0 core contracts document, and is therefore maintained for
     * legacy reasons. <p>
     */
    // F49213.1
    private InjectionTarget[] getInjectionTargets(BeanMetaData bmd,
                                                  Class<?> beanClass) throws InjectionException, EJBConfigurationException {
        InjectionTarget[] targets = bmd.ivReferenceContext.getInjectionTargets(beanClass);

        if (!bmd.usesBeanManagedTx &&
            bmd.type != InternalConstants.TYPE_MANAGED_BEAN) {
            for (InjectionTarget target : targets) {
                if (target.getInjectionBinding().getInjectionClassType() == UserTransaction.class) {
                    Tr.error(tcInjection, "INJECTING_INCORRECT_TX_INTO_BEAN_CWNEN0043E");
                    throw new EJBConfigurationException("Injecting a UserTransaction into a" +
                                                        " container-managed transaction bean is not allowed");
                }
            }
        }

        // No need to perform any sorting if the existing array isn't big enough
        if (targets.length <= 1) {
            return targets;
        }

        int nextField = 0;
        boolean sorted = false;
        ArrayList<InjectionTarget> sortedTargets = new ArrayList<InjectionTarget>(targets.length);

        for (InjectionTarget target : targets) {
            Member targetMember = target.getMember();

            // If this target is for a field, then add it just prior to the first
            // method; the fields will remain in the same order as produced by the
            // injection engine, though that shouldn't really matter.
            if (targetMember instanceof Field) {
                sortedTargets.add(nextField, target);
                ++nextField;
            }

            // Must be a method, so if it is an EJBContext, then add it to the
            // beginning of the list, ahead of both methods and fields; otherwise
            // add it to the end.
            else {
                Class<?> targetType = target.getInjectionBinding().getInjectionClassType();
                if (targetType != null && // d718208
                    EJBContext.class.isAssignableFrom(targetType)) {
                    if ((SessionBean.class.isAssignableFrom(beanClass) &&
                         "setSessionContext".equals(targetMember.getName()))
                        ||
                        (MessageDrivenBean.class.isAssignableFrom(beanClass) &&
                         "setMessageDrivenContext".equals(targetMember.getName()))) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Skipping Session/MessageDrivenBean set context method : " + target);
                        sorted = true;
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Sorting EJBContext set method to first in list : " + target);
                        sortedTargets.add(0, target);
                        sorted = true;
                        ++nextField;
                    }
                } else {
                    sortedTargets.add(target);
                }
            }
        }

        // Only copy the array if the order or size has changed.
        if (sorted) {
            return sortedTargets.toArray(new InjectionTarget[sortedTargets.size()]);
        }

        // No sorting was performed, return the original array.
        return targets;
    }

    /**
     * Processes both the XML and annotatons for the new EJB 3.1 Session
     * Synchronization callback methods @AfterBegin, @BeforeCompletion,
     * and @AfterCompletion. <p>
     *
     * If specified, the method objects will be obtained, set accessible
     * and stored in BeanMetaData. <p>
     *
     * Any Session Synchronization methods found will also be validated
     * against the specification, including the restriction that the
     * class may not also implement the SessionSynchronization interface. <p>
     *
     * @param bmd bean metadata for the bean being processed
     * @param bean the XML representation of the session enterprise bean
     *
     * @throws EJBConfigurationException thrown if any of the session
     *             synchronization were not defined within the rules of the
     *             specification.
     */
    // F743-25855
    private void processSessionSynchronizationMD(BeanMetaData bmd, Session bean) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processSessionSynchronizationMD : " + bmd.j2eeName);

        Class<?>[] afterCompletionParams = new Class<?>[] { Boolean.TYPE };

        // First, obtain each method object that was specified in XML.
        if (bean != null) {
            bmd.ivAfterBegin = getSessionSynchMethod(bmd,
                                                     bean.getAfterBeginMethod(),
                                                     null,
                                                     "after-begin-method");
            bmd.ivBeforeCompletion = getSessionSynchMethod(bmd,
                                                           bean.getBeforeCompletionMethod(),
                                                           null,
                                                           "before-completion-method");
            bmd.ivAfterCompletion = getSessionSynchMethod(bmd,
                                                          bean.getAfterCompletionMethod(),
                                                          afterCompletionParams,
                                                          "after-completion-method");
        }

        // Second, if any of the methods were not present in XML,
        // look for annotations of those not found in XML.
        if (!bmd.metadataComplete &&
            (bmd.ivAfterBegin == null ||
             bmd.ivBeforeCompletion == null ||
             bmd.ivAfterCompletion == null)) {
            findAnnotatedSessionSynchMethods(bmd);
        }

        // Finally, validate all methods found against the specification.
        validateSessionSynchronizationMethod(bmd, bmd.ivAfterBegin,
                                             null, "after-begin-method");
        validateSessionSynchronizationMethod(bmd, bmd.ivBeforeCompletion,
                                             null, "before-completion-method");
        validateSessionSynchronizationMethod(bmd, bmd.ivAfterCompletion,
                                             afterCompletionParams, "after-completion-method");

        if ((bmd.ivAfterBegin != null ||
             bmd.ivBeforeCompletion != null ||
             bmd.ivAfterCompletion != null)
            &&
            SessionSynchronization.class.isAssignableFrom(bmd.enterpriseBeanClass)) {
            J2EEName j2eeName = bmd.j2eeName;
            Method method = (bmd.ivAfterBegin != null) ? bmd.ivAfterBegin : ((bmd.ivBeforeCompletion != null) ? bmd.ivBeforeCompletion : bmd.ivAfterCompletion);
            Tr.error(tc, "BOTH_SESSION_SYNCH_STYLES_CNTR0323E",
                     new Object[] { j2eeName.getComponent(), j2eeName.getModule(),
                                    j2eeName.getApplication(), method });
            throw new EJBConfigurationException("CNTR0323E: The " + j2eeName.getComponent() + " bean in the " +
                                                j2eeName.getModule() + " module of the " + j2eeName.getApplication() +
                                                " application implements the javax.ejb.SessionSynchronization" +
                                                " interface and also configures a session synchronization method" +
                                                " in the ejb-jar.xml or with an annotation. The configured session" +
                                                " synchronization method is " + method);
        }

        if (isTraceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "processSessionSynchronizationMD : found = " +
                        (bmd.ivAfterBegin != null ||
                         bmd.ivBeforeCompletion != null ||
                         bmd.ivAfterCompletion != null));
        }
    }

    /**
     * Obtain the Method object (set accessible) for the specified session
     * synchronization method from XML. If the session synchronization method
     * was not specified in XML, then null will be returned. <p>
     *
     * @param bmd bean metadata for the bean being processed
     * @param namedMethod session synchronization method from XML, may be null
     * @param expectedParams expected parameters for session synchronization method
     * @param xmlType the xml element type; used for configuration errors
     *
     * @return the Method associated with the XML configuration
     * @throws EJBConfigurationException thrown if a corresponding method cannot
     *             be found on the bean class, or the parameters are not correct.
     */
    // F743-25855
    private Method getSessionSynchMethod(BeanMetaData bmd,
                                         NamedMethod namedMethod,
                                         Class<?>[] expectedParams,
                                         String xmlType) throws EJBConfigurationException {
        if (namedMethod == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, xmlType + " not specified in XML");
            return null;
        }

        String methodName = namedMethod.getMethodName().trim();
        List<String> paramTypes = namedMethod.getMethodParamList();
        if (paramTypes != null) {
            if (expectedParams == null || expectedParams.length == 0) {
                if (paramTypes.size() > 0) {
                    J2EEName j2eeName = bmd.j2eeName;
                    Tr.error(tc, "INVALID_SESSION_SYNCH_PARAM_CNTR0324E",
                             new Object[] { methodName, xmlType, j2eeName.getComponent(),
                                            j2eeName.getModule(), j2eeName.getApplication() });
                    throw new EJBConfigurationException("CNTR0324E: The " + methodName + " method configured in the ejb-jar.xml" +
                                                        " file does not have the required method signature for a " + xmlType +
                                                        " session synchronization method for the " + j2eeName.getComponent() +
                                                        " bean in the " + j2eeName.getModule() + " module of the " +
                                                        j2eeName.getApplication() + " application. The number of parameters" +
                                                        " configured is " + paramTypes.size() + " but must be 0.");
                }
            } else {
                if (paramTypes.size() != expectedParams.length) {
                    J2EEName j2eeName = bmd.j2eeName;
                    Tr.error(tc, "INVALID_SESSION_SYNCH_PARAM_CNTR0324E",
                             new Object[] { methodName, xmlType, j2eeName.getComponent(),
                                            j2eeName.getModule(), j2eeName.getApplication() });
                    int configuredSize = paramTypes.size();
                    throw new EJBConfigurationException("CNTR0324E: The " + methodName + " method configured in the ejb-jar.xml" +
                                                        " file does not have the required method signature for a " + xmlType +
                                                        " session synchronization method for the " + j2eeName.getComponent() +
                                                        " bean in the " + j2eeName.getModule() + " module of the " +
                                                        j2eeName.getApplication() + " application. The number of parameters" +
                                                        " configured is " + configuredSize + " but must be " +
                                                        expectedParams.length + ".");
                }
                for (int i = 0; i < expectedParams.length; i++) {
                    if (!paramTypes.get(i).equals(expectedParams[i].getName())) {
                        J2EEName j2eeName = bmd.j2eeName;
                        Tr.error(tc, "INVALID_SESSION_SYNCH_PARAM_CNTR0324E",
                                 new Object[] { methodName, xmlType, j2eeName.getComponent(),
                                                j2eeName.getModule(), j2eeName.getApplication() });
                        throw new EJBConfigurationException("CNTR0324E: The " + methodName + " method configured in the ejb-jar.xml" +
                                                            " file does not have the required method signature for a " + xmlType +
                                                            " session synchronization method for the " + j2eeName.getComponent() +
                                                            " bean in the " + j2eeName.getModule() + " module of the " +
                                                            j2eeName.getApplication() + " application. The configured parameter" +
                                                            " type is " + paramTypes.get(i) + " but must be " +
                                                            expectedParams[i].getName() + ".");
                    }
                }
            }
        }

        Method method = null;

        Exception firstException = null;
        Class<?> clazz = bmd.enterpriseBeanClass;
        while (clazz != Object.class && method == null) {
            try {
                method = clazz.getDeclaredMethod(methodName, expectedParams);
                method.setAccessible(true);
            } catch (NoSuchMethodException ex) {
                // FFDC not required... this may be normal
                if (firstException == null) {
                    firstException = ex;
                }
                clazz = clazz.getSuperclass();
            }
        }

        if (method == null) {
            J2EEName j2eeName = bmd.j2eeName;
            Tr.error(tc, "SESSION_SYNCH_METHOD_NOT_FOUND_CNTR0325E",
                     new Object[] { xmlType, methodName, j2eeName.getComponent(),
                                    j2eeName.getModule(), j2eeName.getApplication() });
            throw new EJBConfigurationException("CNTR0325E: The configured " + xmlType + " session synchronization method " +
                                                methodName + " is not implemented by the " + j2eeName.getComponent() +
                                                " bean in the " + j2eeName.getModule() + " module of the " +
                                                j2eeName.getApplication() + " application.", firstException);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, xmlType + " = " + method);

        return method;
    }

    /**
     * Locates all annotated session synchronization methods that have not
     * already been obtained based on the XML configuration. <p>
     *
     * Note: only a single method is permitted per session synchronization
     * annotation (overridden methods are ignored). <p>
     *
     * @param bmd bean metadata for the bean being processed
     * @throws EJBConfigurationException thrown if multiple methods have
     *             been coded with the same annotation.
     */
    // F743-25855
    private void findAnnotatedSessionSynchMethods(BeanMetaData bmd) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        boolean lookForAfterBegin = (bmd.ivAfterBegin == null);
        boolean lookForBeforeCompletion = (bmd.ivBeforeCompletion == null);
        boolean lookForAfterCompletion = (bmd.ivAfterCompletion == null);

        for (MethodMap.MethodInfo methodInfo : MethodMap.getAllDeclaredMethods(bmd.enterpriseBeanClass)) {
            Method method = methodInfo.getMethod();
            if (lookForAfterBegin &&
                method.isAnnotationPresent(AfterBegin.class)) {
                if (bmd.ivAfterBegin == null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "@AfterBegin = " + method);
                    method.setAccessible(true); // d666251
                    bmd.ivAfterBegin = method;
                } else {
                    Tr.error(tc, "MULTIPLE_SESSION_SYNCH_METHODS_CNTR0326E",
                             new Object[] { "after-begin-method",
                                            bmd.j2eeName.getComponent(),
                                            bmd.ivAfterBegin, method });
                    throw new EJBConfigurationException("CNTR0326E: Multiple after-begin-method session synchronization" +
                                                        " methods have been configured for the " + bmd.j2eeName.getComponent() +
                                                        " bean. The configured session synchronization methods are : " +
                                                        bmd.ivAfterBegin + " and " + method);
                }
            }
            if (lookForBeforeCompletion &&
                method.isAnnotationPresent(BeforeCompletion.class)) {
                if (bmd.ivBeforeCompletion == null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "@BeforeCompletion = " + method);
                    method.setAccessible(true); // d666251
                    bmd.ivBeforeCompletion = method;
                } else {
                    Tr.error(tc, "MULTIPLE_SESSION_SYNCH_METHODS_CNTR0326E",
                             new Object[] { "before-completion-method",
                                            bmd.j2eeName.getComponent(),
                                            bmd.ivBeforeCompletion, method });
                    throw new EJBConfigurationException("CNTR0326E: Multiple before-completion-method session synchronization" +
                                                        " methods have been configured for the " + bmd.j2eeName.getComponent() +
                                                        " bean. The configured session synchronization methods are : " +
                                                        bmd.ivBeforeCompletion + " and " + method);
                }
            }
            if (lookForAfterCompletion &&
                method.isAnnotationPresent(AfterCompletion.class)) {
                if (bmd.ivAfterCompletion == null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "@AfterCompletion = " + method);
                    method.setAccessible(true); // d666251
                    bmd.ivAfterCompletion = method;
                } else {
                    Tr.error(tc, "MULTIPLE_SESSION_SYNCH_METHODS_CNTR0326E",
                             new Object[] { "after-completion-method",
                                            bmd.j2eeName.getComponent(),
                                            bmd.ivAfterCompletion, method });
                    throw new EJBConfigurationException("CNTR0326E: Multiple after-completion-method session synchronization" +
                                                        " methods have been configured for the " + bmd.j2eeName.getComponent() +
                                                        " bean. The configured session synchronization methods are : " +
                                                        bmd.ivAfterCompletion + " and " + method);
                }
            }
        }
    }

    /**
     * Verifies that a session synchronization method has been coded properly
     * based on the rules from the specification. <p>
     *
     * The following rules are validated:
     * <ul>
     * <li> must not be static
     * <li> must not be final
     * <li> must have a void return type
     * <li> parameters must be null or boolean, depending on which method
     * </ul>
     *
     * @param bmd bean metadata for the bean being processed
     * @param method the session synchronization method
     * @param expectedParams the expected parameters for the method
     * @param methodType the xml type of the session synchronization method
     *
     * @throws EJBConfigurationException thrown if the method has not been
     *             coded properly based on the rules in the specification
     */
    // F743-25855
    private void validateSessionSynchronizationMethod(BeanMetaData bmd,
                                                      Method method,
                                                      Class<?>[] expectedParams,
                                                      String methodType) throws EJBConfigurationException {
        if (method == null) {
            return;
        }

        String specificCause = "";

        int modifiers = method.getModifiers();
        if (Modifier.isStatic(modifiers)) {
            specificCause += " The 'static' modifier is not allowed.";
        }

        if (Modifier.isFinal(modifiers)) {
            specificCause += " The 'final' modifier is not allowed.";
        }

        if (method.getReturnType() != Void.TYPE) {
            specificCause += " The return type must be 'void'.";
        }

        Class<?>[] paramTypes = method.getParameterTypes();
        if (expectedParams == null || expectedParams.length == 0) {
            if (paramTypes != null && paramTypes.length > 0) {
                specificCause += " The method must take 0 arguments.";
            }
        } else {
            if (paramTypes == null || paramTypes.length != expectedParams.length) {
                specificCause += " The method must take " + expectedParams.length +
                                 " arguments.";
            } else {
                for (int i = 0; i < expectedParams.length; i++) {
                    if (!paramTypes[i].equals(expectedParams[i])) {
                        specificCause += " Parameter number " + i + " of type " +
                                         paramTypes[i] + " must be of type " +
                                         expectedParams[i] + ".";
                    }
                }
            }
        }

        // The spec does not specifically prohibit a throws clause, but also does
        // not indicate one is allowed. To be consistent with what the spec does
        // say about interceptors, a throws clause is being prohibited. If we are
        // required to relax this, consider using the same method that the
        // lifecycle interceptors use: isLifecycleApplicationException.
        Class<?>[] exceptions = method.getExceptionTypes();
        if (exceptions.length > 0) {
            specificCause += " The method must not throw any exceptions.";
        }

        if (!"".equals(specificCause)) {
            Tr.error(tc, "INVALID_SESSION_SYNCH_SIGNATURE_CNTR0327E",
                     new Object[] { method, methodType });
            throw new EJBConfigurationException("CNTR0327E: The " + method + " method does not have the required" +
                                                " method signature for a " + methodType + " session synchronization" +
                                                " method." + specificCause);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "validateSessionSynchronizationMethod : valid : " + method);
    }

    /**
     * Removes the MessageEndpoint interface methods from the list of methods
     * declared on the message listener interface (or the message-driven bean
     * class when the listener interface has no methods). <p>
     *
     * The message listener interface cannot override the methods on the
     * MessageEndpoint interface; the JCA specification requires that
     * MessageEndpoint interface method calls on the generated proxy be
     * intercepted and perform the defined MessageEndpoint behavior. Some
     * message listener implementations may define methods corresponding to the
     * MessageEndpoint interface methods to simplify code in the resource
     * adapter, so this should not be considered an error, but the generated
     * MessageEndpoint proxy must ensure the method behavior are not overridden.
     * Removing these methods from the message listener interface methods will
     * prevent the methods from being overridden in the generated proxy/wrapper. <p>
     *
     * @param listenerMethods
     *            methods declared on the listener interface or message-driven
     *            bean implementation when the listener interface has no
     *            methods.
     *
     * @return the listener methods excluding methods that overlap with the
     *         MessageEndpoint interface.
     */
    private Method[] removeMessageEndpointMethods(Method[] listenerMethods) {
        ArrayList<Method> methods = new ArrayList<Method>();

        for (Method method : listenerMethods) {
            String name = method.getName();
            Class<?>[] params = method.getParameterTypes();

            if ("afterDelivery".equals(name) && params.length == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "removeMessageEndpointMethods: removing afterDelivery");
                continue;
            } else if ("beforeDelivery".equals(name) && params.length == 1 && params[0] == Method.class) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "removeMessageEndpointMethods: removing beforeDelivery");
                continue;
            } else if ("release".equals(name) && params.length == 0) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "removeMessageEndpointMethods: removing release");
                continue;
            }
            methods.add(method);
        }

        if (methods.size() == listenerMethods.length) {
            return listenerMethods;
        }

        return methods.toArray(new Method[methods.size()]);
    }

    // F743-17630
    /**
     * Removes temporary lists of data from BMD when they are no longer needed.
     */
    private void removeTemporaryMethodData(BeanMetaData bmd) {
        bmd.methodsExposedOnLocalHomeInterface = null;
        bmd.methodsExposedOnLocalInterface = null;
        bmd.methodsExposedOnRemoteHomeInterface = null;
        bmd.methodsExposedOnRemoteInterface = null;
        bmd.allPublicMethodsOnBean = null;
        bmd.methodsToMethodInfos = null;
    }

    /**
     * This class is strictly used to provide a mechanism for returning multiple arrays
     * from the initializeBeanMethods and initializeHomeMethods methods of BeanMetaData.
     * After calling either of these methods, it is required to call one of the postProcessxxxxx
     * methods to set the appropriate instance variable in BeanMetaData. This mechanism allows for
     * for greater factorization of the initializeBeanMethods and initializeHomeMethods methods.
     * Future things to consider maybe to change the mechanism the EJSContainer reaches in and
     * grabs public attributes of BeanMetaData without going through a method. Restricting this access
     * would potentially eliminate the need for this wrapper class.
     */
    private class MethodDataWrapper {
        private final String[] wrapperMethodNames;
        private final EJBMethodInfoImpl[] wrapperMethodInfos;
        private final int[] wrapperIsolationAttrs;
        private final boolean[] wrapperReadOnlyAttrs;

        MethodDataWrapper(String[] _methodNames,
                          EJBMethodInfoImpl[] _methodInfos,
                          int[] _isolationAttrs,
                          boolean[] _readOnlyAttrs) {
            wrapperMethodInfos = _methodInfos;
            wrapperMethodNames = _methodNames;
            wrapperIsolationAttrs = _isolationAttrs;
            wrapperReadOnlyAttrs = _readOnlyAttrs;
        }

        void postProcessRemoteBeanMethodData(BeanMetaData bmd) {
            bmd.methodInfos = wrapperMethodInfos;
            bmd.isolationAttrs = wrapperIsolationAttrs;
            bmd.readOnlyAttrs = wrapperReadOnlyAttrs;
            bmd.methodNames = wrapperMethodNames;

        }

        void postProcessRemoteHomeMethodData(BeanMetaData bmd) {
            bmd.homeMethodInfos = wrapperMethodInfos;
            bmd.homeIsolationAttrs = wrapperIsolationAttrs;
            bmd.homeReadOnlyAttrs = wrapperReadOnlyAttrs;
            bmd.homeMethodNames = wrapperMethodNames;
        }

        void postProcessLocalBeanMethodData(BeanMetaData bmd) {
            bmd.localMethodInfos = wrapperMethodInfos;
            bmd.localMethodNames = wrapperMethodNames;
            // Isolation & ReadOnly Attrs not required for 2.x beans.
        }

        void postProcessLocalHomeMethodData(BeanMetaData bmd) {
            bmd.localHomeMethodInfos = wrapperMethodInfos;
            bmd.localHomeMethodNames = wrapperMethodNames;
            // Isolation & ReadOnly Attrs not required for 2.x beans.
        }

        void postProcessTimedBeanMethodData(BeanMetaData bmd) {
            bmd.timedMethodInfos = wrapperMethodInfos;
            bmd.timedMethodNames = wrapperMethodNames;
            // Isolation & ReadOnly Attrs not required for 2.x beans.
        }

        void postProcessWebserviceBeanMethodData(BeanMetaData bmd) {
            bmd.wsEndpointMethodInfos = wrapperMethodInfos;
            bmd.wsEndpointMethodNames = wrapperMethodNames;
            // Isolation & ReadOnly Attrs not required for 2.x beans.
        }
    } // class MethodDataWrapper

} //EJBMDOrchestrator
