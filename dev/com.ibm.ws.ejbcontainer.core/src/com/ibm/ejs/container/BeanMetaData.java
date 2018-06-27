/*******************************************************************************
 * Copyright (c) 1998, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.ejb.TimedObject;
import javax.naming.Context;

import com.ibm.ejs.container.activator.Activator;
import com.ibm.ejs.container.interceptors.InterceptorMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.tx.jta.embeddable.GlobalTransactionSettings;
import com.ibm.tx.jta.embeddable.LocalTransactionSettings;
import com.ibm.websphere.cpi.Persister;
import com.ibm.websphere.csi.ActivitySessionAttribute;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.TransactionAttribute;
import com.ibm.websphere.ejbcontainer.LightweightLocal;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.CallbackKind;
import com.ibm.ws.ejbcontainer.EJBComponentMetaData;
import com.ibm.ws.ejbcontainer.EJBMethodInterface;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.EJBType;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.diagnostics.IntrospectionWriter;
import com.ibm.ws.ejbcontainer.diagnostics.TrDumpWriter;
import com.ibm.ws.ejbcontainer.failover.SfFailoverClient;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.ws.metadata.ejb.BeanInitData;
import com.ibm.ws.metadata.ejb.WCCMMetaData;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.ReferenceContext;

/**
 * An instance of <code>BeanMetaData</code> contains all the metadata
 * the container associates with a particular type of enterprise bean. <p>
 *
 * The data contained in a <code>BeanMetaData</code> instance is
 * extracted from a deployment descriptor instance and combined with any
 * additional metadata from other sources, such as container defaults
 * or systems management data. <p>
 *
 * A <code>BeanMetaData</code> instance is not specialized for the
 * type of bean it is associated with, i.e. there is not an entity bean
 * specific version nor a session bean specific version. It therefore
 * contains the union of all possible metadata for each type of bean.
 * Not all metadata will be valid for every <code>BeanMetaData</code>
 * instance. The clients of a <code>BeanMetaData</code> instance must
 * ensure that they only attempt to access the metadata appropriate for
 * that instance. For example, a client must not attempt to retrieve the
 * session timeout value for an entity bean. <p>
 *
 * A metadata instance is immutable. <p>
 */

public class BeanMetaData extends com.ibm.ws.runtime.metadata.MetaDataImpl implements EJBComponentMetaData {
    private static final TraceComponent tc = Tr.register(BeanMetaData.class,
                                                         "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /** Class for the javax.ejb.TimedObject interface **/
    // LI2281.07
    public static final Class<TimedObject> svTimedObjectClass = TimedObject.class; // d174057
    /** Method for javax.ejb.TimedObject.ejbTimeout **/
    public static final Method svTimedObjectMethod = svTimedObjectClass.getMethods()[0]; // F743-506

    /**
     * Configuration constants. The following constants are the container's
     * internal definition of the values allowed for our configuration
     * data. These values are independent of any specific configuration
     * repository such as WCCM, property files, etc. It is the responsibility
     * of configuration loaders to map from a specific repository's
     * configuration data format / values to the internal data format / values
     * required by the container.
     */

    // Access Intent values (ie.  called AccessIntentKind in WCCM)
    public static final int ACCESS_INTENT_READ = 0;
    public static final int ACCESS_INTENT_UPDATE = 1;

    // Activation Policy values (ie.  called ActivationPolicyKind
    // in WCCM)
    public static final int ACTIVATION_POLICY_ONCE = 0;
    public static final int ACTIVATION_POLICY_ACTIVITY_SESSION = 1;
    public static final int ACTIVATION_POLICY_TRANSACTION = 2;

    // Concurrency Control values (ie.  called ConcurrencyControlKind
    // in WCCM)
    public static final int CONCURRENCY_CONTROL_PESSIMISTIC = 0;
    public static final int CONCURRENCY_CONTROL_OPTIMISTIC = 1;

    // Java EE and EJB Version values

    public static final int J2EE_EJB_VERSION_UNKNOWN = 0;
    // Consistent with org.eclipse.jst.j2ee.internal.J2EEVersionConstants.EJB_1_0_ID
    public static final int J2EE_EJB_VERSION_1_0 = 10;
    public static final int J2EE_EJB_VERSION_1_1 = EJBJar.VERSION_1_1;
    public static final int J2EE_EJB_VERSION_2_0 = EJBJar.VERSION_2_0;
    public static final int J2EE_EJB_VERSION_2_1 = EJBJar.VERSION_2_1;
    public static final int J2EE_EJB_VERSION_3_0 = EJBJar.VERSION_3_0;
    public static final int J2EE_EJB_VERSION_3_1 = EJBJar.VERSION_3_1;
    public static final int J2EE_EJB_VERSION_3_2 = EJBJar.VERSION_3_2;

    // Consistent with org.eclipse.jst.j2ee.internal.J2EEVersionConstants.JAVAEE_5_ID
    private static final int JAVAEE_VERSION_5 = 50;

    // Load Policy values (ie.  called LoadPolicyKind in WCCM)
    public static final int LOAD_POLICY_ACTIVATION = 0;
    public static final int LOAD_POLICY_TRANSACTION = 1;
    public static final int LOAD_POLICY_INTERVAL = 2;
    public static final int LOAD_POLICY_DAILY = 3;
    public static final int LOAD_POLICY_WEEKLY = 4;

    // Local Transacion Unresolved Action values (ie.  called
    // LocalTranUnresolvedActionKind in WCCM
    public static final int LOCAL_TX_UNRESOLVED_ACTION_ROLLBACK = 0;
    public static final int LOCAL_TX_UNRESOLVED_ACTION_COMMIT = 1;

    //Method Type values (ie. called MethodElementKind in WCCM)
    public static final int METHOD_TYPE_UNSPECIFIED = 0;
    public static final int METHOD_TYPE_REMOTE = 1;
    public static final int METHOD_TYPE_REMOTE_HOME = 2;
    public static final int METHOD_TYPE_LOCAL = 3;
    public static final int METHOD_TYPE_LOCAL_HOME = 4;
    public static final int METHOD_TYPE_SERVICE_ENDPOINT = 5;

    // Persistence Management Type values (ie.  represented by
    //interfaces in WCCM)
    public static final int PERSISTENCE_BEAN_MANAGED = 0;
    public static final int PERSISTENCE_CONTAINER_MANAGED = 1;

    //Pin Policy values (ie.  called PinPolicyKind in WCCM)
    public static final int PIN_POLICY_ACTIVATION_PERIOD = 0;
    public static final int PIN_POLICY_ACTIVITY_SESSION = 1;
    public static final int PIN_POLICY_TRANSACTION = 2;
    public static final int PIN_POLICY_BUSINESS_METHOD = 3;

    // Transaction Attribute Type values (ie. called
    // TransactionAttributeType in WCCM
    public static final int TX_ATTRIBUTE_TYPE_NOT_SUPPORTED = 0;
    public static final int TX_ATTRIBUTE_TYPE_SUPPORTS = 1;
    public static final int TX_ATTRIBUTE_TYPE_REQUIRED = 2;
    public static final int TX_ATTRIBUTE_TYPE_REQUIRES_NEW = 3;
    public static final int TX_ATTRIBUTE_TYPE_MANDATORY = 4;
    public static final int TX_ATTRIBUTE_TYPE_NEVER = 5;
    public static final int TX_ATTRIBUTE_TYPE_BEAN_MANAGED = 6;

    // Transaction Management values (ie.  called TransactionType
    // in WCCM
    public static final int TX_BEAN_MANAGED = 0;
    public static final int TX_CONTAINER_MANAGED = 1;

    // Transaction Isolation Level values(ie.  called TxIsolationLevel
    // in WCCM
    public static final int TX_ISOLATION_LEVEL_REPEATABLE_READ = 0;
    public static final int TX_ISOLATION_LEVEL_READ_COMMITTED = 1;
    public static final int TX_ISOLATION_LEVEL_READ_UNCOMMITTED = 2;
    public static final int TX_ISOLATION_LEVEL_SERIALIZABLE = 3;

    /**
     * The data used to initialize this object. This field will be null after
     * the bean has been fully initialized.
     */
    public BeanInitData ivInitData; // F743-36113

    /**
     * Information required at the level of all Component meta data
     */
    public Context _javaNameSpaceContext;
    public EJBModuleMetaDataImpl _moduleMetaData;

    public ResourceRefConfigList _resourceRefList;
    public LocalTransactionSettings _localTran;
    public GlobalTransactionSettings _globalTran; // LIDB1673-13.1

    /**
     * The Java EE Name (app-module-component) for the bean.
     */
    public J2EEName j2eeName;

    /**
     * The 'unversioned' (or 'base') Java EE Name for the bean. Will be null if
     * a base module version name has not been specified for the module.
     */
    // F54184
    public J2EEName ivUnversionedJ2eeName;

    /**
     * Type of bean that is associated with this <code>BeanMetaData</code>
     * instance. <p>
     */
    public int type; // 126512

    /**
     * List of EJBs that this bean depends on. This field will always be null
     * if the bean is not a singleton or if singleton dependencies have not yet
     * been resolved. This field will be null or empty if this singleton has
     * no dependencies depending on whether or not early dependency resolution
     * has been attempted.
     */
    // F743-4950
    public List<J2EEName> ivDependsOn;

    // PQ63130
    public static final String[] entityRemoteNoTxAttrMethods = { "getEJBHome", "getHandle", "getPrimaryKey", "isIdentical" };
    public static final String[] entityRemoteNoTxAttrMethodSignatures = { "", "", "", "javax.ejb.EJBObject" };

    public static final String[] entityLocalNoTxAttrMethods = { "getEJBHome", "getPrimaryKey", "isIdentical" };
    public static final String[] entityLocalNoTxAttrMethodSignatures = { "", "", "javax.ejb.EJBObject" };

    public static final String[] entityRemoteHomeNoTxAttrMethods = { "getEJBMetaData", "getHomeHandle" };
    public static final String[] entityRemoteHomeNoTxAttrMethodSignatures = { "", "" };

    public static final String[] entityLocalHomeNoTxAttrMethods = { "" };
    public static final String[] entityLocalHomeNoTxAttrMethodSignatures = { "" };

    public static final String[] sessionLocalHomeNoTxAttrMethods = { "*" };
    public static final String[] sessionLocalHomeNoTxAttrMethodSignatures = { "" };

    public static final String[] sessionRemoteHomeNoTxAttrMethods = { "*" };
    public static final String[] sessionRemoteHomeNoTxAttrMethodSignatures = { "" };
    //PQ63130

    /**
     * EJB module version this component is in.
     */
    public int ivModuleVersion;

    /**
     * The class instances associated with this metadata instance.
     * Not all of these classes will have valid values for BeanMetaData
     * instances. <p>
     */
    public String enterpriseBeanClassName;
    public Class<?> enterpriseBeanClass; // d367572.3
    public Class<?> enterpriseBeanAbstractClass; // d367572.3

    public Class<?> homeBeanClass;

    /**
     * The factory for creating managed bean or enterprise bean instances, or null
     * if {@link #ivEnterpriseBeanClassConstructor} should be used to create instances.
     */
    public ManagedObjectFactory<?> ivEnterpriseBeanFactory;

    // Lazily initialize ivEnterpriseBeanClassConstructor in
    // BeanMetaData.getEnterpriseBeanClassConstructor() if there is a
    // factory in order to avoid problems with CDI.  In theory, CDI
    // ManagedObjectFactory.getConstructor() should be fine to call at
    // this point, but they currently drive the deferred initialization
    // code path in order to determine @Remove methods, and
    // getConstructor() returns nonsense until they have that answer.
    public Constructor<?> ivEnterpriseBeanClassConstructor;

    /**
     * True if the managed object factory and managed object instance will manage
     * all injection and interceptors.
     */
    public boolean managedObjectManagesInjectionAndInterceptors;

    // This flag indicates that the fields of this BMD instance
    // are completely filled in.   It becomes true only on the very
    // last line of the EJBMDOrchestrator's finishBMDInit method.
    // Until that point, use of this instance may be fragile, or yield
    // unpredictable results.
    public boolean fullyInitialized = false;

    public Class<?> pKeyClass;

    public boolean m_syncToOSThreadValue; // LI2775-107.2 WS18354.02

    /**
     * The set of persistence reference names on which this bean has declared a
     * dependency.
     */
    public Set<String> ivPersistenceRefNames; // F743-30682

    /**
     * Extended Scope JPAPuIds that are SynchronizationType.UNSYNCHRONIZED.
     */
    public Set<?> ivJpaPuIdSTUnsyncSet;

    public Object[] ivExPcPuIds; // d416151.3

    /**
     * Returns true iff the EJB has a remote home interface.
     */
    public boolean hasRemoteHome() // d121117
    {
        return homeInterfaceClass != null;
    }

    /**
     * Returns true iff the EJB has a local home interface.
     */
    public boolean hasLocalHome() // d121863
    {
        return localHomeInterfaceClass != null;
    }

    /**
     * Remote home interface and generated classes
     */
    public String homeInterfaceClassName;
    public Class<?> homeInterfaceClass;
    public Class<?> remoteInterfaceClass;
    public Class<?> remoteImplClass;
    public Class<?> remoteTieClass; // d413752
    public Class<?> homeRemoteImplClass;
    public Class<?> homeRemoteTieClass; // d413752

    /**
     * Local home interface and generated classes
     */
    public String localHomeInterfaceClassName; // f111627
    public Class<?> localHomeInterfaceClass; // f111627
    public Class<?> localInterfaceClass; // f111627
    public Class<?> localImplClass; // f111627
    public Constructor<?> localImplProxyConstructor; // F58064
    public Class<?> homeLocalImplClass; // f111627
    public Constructor<?> homeLocalImplProxyConstructor; // F58064

    /**
     * Bean has a WebService Endpoint defined, either as an interface in
     * XML or via the WebService or WebServiceProvider annotation.
     **/
    // F743-1756CodRv
    public boolean ivHasWebServiceEndpoint;

    /**
     * WebService Endpoint interface class
     */
    public Class<?> webserviceEndpointInterfaceClass; // fLIDB2281.24

    /**
     * Name to be used for the Web Service Endpoint generated proxy, if
     * a Web Service Endpoint is accessed. <p>
     *
     * Web Services now process all metadata for the endpoint, so
     * EJB Container may not be aware of the existance of the endpoint until
     * it is accessed, but the endpoint proxy class name is pre-determined
     * when all of the other deployed class names are calculated.
     **/
    // LI3294-35 d497921
    public String ivWebServiceEndpointProxyName;

    /**
     * Web Service Endpoint JIT deployed proxy class. <p>
     *
     * This class is not generated until the first time the Web Service Endpoint
     * is accessed, and then only if there are methods with around invoke
     * interceptors. <p>
     *
     * The code that generates the class and sets this field will synchronize on
     * this BMD instance to insure multiple threads do not attempt to generate
     * the class concurrently.
     **/
    // d497921
    public Class<?> ivWebServiceEndpointProxyClass;

    /**
     * Indicates that the WebServices component has called to create the
     * Web Service Endpoint for this bean and the methods are now known. <p>
     *
     * The WebServices component now process all metadata for the endpoint, so
     * EJB Container may not be aware of the existance of the endpoint until
     * it is accessed. The Web Service Endpoint methods are known if a Web
     * Service Endpoint Interface has been specified in ejb-jar.xml, or after
     * the first time the WebServices component calls
     * createWebServiceEndpointManager(). <p>
     **/
    // d497921
    public boolean ivWebServiceEndpointCreated;

    // --------------------------------------------------------------------------
    // EJB 3.0 Business Local and Remote interface and generated classes
    //
    // Note: The business local classes will contain the No-Interface view
    //       as the first entry, if present.                            F743-1756
    // --------------------------------------------------------------------------
    public String[] ivBusinessLocalInterfaceClassNames; // F743-26072
    public Class<?> ivBusinessLocalInterfaceClasses[]; // d366807
    public String[] ivBusinessRemoteInterfaceClassNames; // F743-26072
    public Class<?> ivBusinessRemoteInterfaceClasses[]; // d366807
    public Class<?> ivBusinessLocalImplClasses[]; // d366807.1
    public Constructor<?> ivBusinessLocalImplProxyConstructors[]; // F58064
    public Class<?> ivBusinessRemoteImplClasses[]; // d366807.1
    public Class<?> ivBusinessRemoteTieClasses[]; // d413752

    /**
     * Generated wrapper class that implements all business local interfaces.
     * This class is not generated until the first time it is used.
     *
     * The code that generates the class and sets this field will synchronize on
     * this BMD instance to insure multiple threads do not attempt to generate
     * the class concurrently.
     */
    // F743-34304
    public Class<?> ivAggregateLocalImplClass;

    /**
     * When true, the first business local interface is the No-Interface view,
     * which is the EJB impl class.
     **/
    // F743-1756
    public boolean ivLocalBean = false;

    /**
     * When there is a No-Interface view (LocalBean), this will be set to the
     * reflection Field on the generated wrapper class that holds a reference
     * to the wrapper metadata (an EJSWrapperBase subclass).
     **/
    // F743-1756
    public Field ivLocalBeanWrapperField;

    /**
     * When there is a no-method interface MDB, this will be set to the
     * reflection Field on the generated proxy class that holds a reference
     * to the MessageEndpointBase.
     **/
    public Field ivMessageEndpointBaseField;

    /**
     * For ManagedBeans with PreDestroy or AroundInvoke, this will be set to the
     * reflection Field on the generated wrapper class that holds a reference
     * to the corresponding BeanO.
     **/
    // F743-34301.1
    public Field ivManagedBeanBeanOField;

    /**
     * True if indirect local proxies should be used to access this EJB.
     */
    // F58064
    public boolean ivIndirectLocalProxies;

    /**
     * List of persistent CMP fields that need to be reset to Java defaults during ejbCreate,
     * if the bean instance was obtained from the instance pool as opposed to being newly created.
     * This variable will be null of not CMP or if no fields need to be reset (e.g. if generated
     * deployment code is handling instead).
     */
    public java.lang.reflect.Field cmpResetFields[];
    public String connFactoryName;

    /*
     * javaNameSpace associatated with the Bean
     */
    //public Context javaNameSpaceValue;// DELETED LIDB1181.2.1

    /**
     * The environment properties associated with the enterprise bean.
     */
    public Properties envProps;

    /**
     * The simple binding name is equvalent to the pre-EJB3 home binding name, but starting
     * with EJB3 this binding name also can be applied to business interfaces.
     */
    public String simpleJndiBindingName = null;

    /**
     * The localHomeJndiBindingName, and remoteHomeJndiBinding are new stanzas supported only in the new EJB3
     * xml bindings file. These two stanzas allow customers to configure unique binding names for local and
     * remote homes. These names may NOT be used in combination with simpleJndiBindingName above. The customer
     * must pick one of the two methods to specify the bindgins.
     */
    public String localHomeJndiBindingName = null;
    public String remoteHomeJndiBindingName = null;

    /**
     * The remote home jndi name is the unique binding name for the Remote Home.
     * It may be the simpleJndiBindingName, or it may be the long default jndi name or
     * it may be from the binding file. //d512996
     */
    public String ivRemoteHomeJndiName = null;

    /**
     * The local home jndi name is the unique binding name for the local Home.
     * It may be the simpleJndiBindingName, or it may be the long default jndi name or
     * it may be from the binding file. //d513978
     */
    public String ivLocalHomeJndiName = null;

    /**
     * The businessInterfaceJndiBindingsMap is a list of the binding names for all the business interfaces that
     * this bean supports. The Map is keyed by business interface name Strings and the value is the specific
     * JNDI binding name String for the given interface name. The values from this map comes from the <interface>
     * stanza(s) in the new EJB3 xml bindings file. If no specific binding is profided for an interface, a
     * container created default binding name will be used instead.
     */
    public Map<String, String> businessInterfaceJndiBindingNames = null; // d456907

    /**
     * The ejb-name attribute for the bean.
     */
    public String enterpriseBeanName;

    /**
     * Is the associated enterprise bean reentrant?
     */
    public boolean reentrant;

    /**
     * True if the stateful session bean itself is configured to be passivation
     * capable. This field being true is necessary for an EJB to be passivation
     * capable, but other configuration might implicitly prevent it.
     *
     * @see #isPassivationCapable
     */
    public boolean passivationCapable;

    /**
     * The session timeout value for session beans, in milliseconds.
     */
    public long sessionTimeout; // F743-6605.1

    /**
     * Set to true if and only if SFSB failover is enabled for this bean.
     */
    public boolean ivSFSBFailover = false; //LIDB2018-1 //d236973

    /**
     * The DataXfer object to use for SFSB failover.
     */
    public SfFailoverClient ivSfFailoverClient = null; //LIDB2018-1

    /**
     * True if the session bean instance needs to passivated on transaction
     * boundaries.
     */
    public boolean sessionActivateTran = false;

    /**
     * True if the session bean instance needs to passivated on session
     * boundaries.
     */
    public boolean sessionActivateSession = false; // LIDB441.5

    /**
     * True if option A commit option is to be used for this entity bean.
     * NOTE : option A commit option also implies a single instance of
     * the bean will be used to serialize all access to the database
     */
    public boolean optionACommitOption = false;

    /**
     * True if option B commit option is to be used
     */
    public boolean optionBCommitOption = false;

    /**
     * True if option C commit option is to be used
     */
    public boolean optionCCommitOption = false;

    /**
     * True if ReadOnly / Interval commit option is to be used.
     **/
    // LI3408
    public boolean ivReadOnlyCommitOption = false;

    /**
     * True if entity sessional tran activation policy to be used
     */
    public boolean entitySessionalTranOption = false; // LIDB441.5

    /**
     * True if optimistic Concurrency Control is to be used
     */
    public boolean optimisticConcurrencyControl = false;

    // The following values are Enumeration constants for the EJB Cache
    // Reload type (ivCacheReloadType), and indicate how to use the
    // EJB Cache Reload Interval value (ivCacheReloadInterval) to determine
    // when a cached Entity needs to be reloaded.                          LI3408

    /** Cached Entity beans are never reloaded. **/
    public static final int CACHE_RELOAD_NONE = 0;

    /** Milliseconds between reloads of cached Entity beans. **/
    public static final int CACHE_RELOAD_INTERVAL = 1;

    /** Milliseconds after midnight to reload cached Entity beans. **/
    public static final int CACHE_RELOAD_DAILY = 2;

    /** Milliseconds after midnight Sunday to reload cached Entity beans. **/
    public static final int CACHE_RELOAD_WEEKLY = 3;

    /**
     * Defines the meaning of the EJB Cache Reload value using one of the
     * Cache Reload enumeration constants. The deployment descriptor may
     * define that an Entity bean in the EJB Cache be reloaded periodically,
     * and this field indicates how to interpret the specified 'interval'. <p>
     *
     * Valid values include:
     * <ul>
     * <li>CACHE_RELOAD_NONE - Entity is never reloaded (default).
     * <li>CACHE_RELOAD_INTERVAL - Entity reloaded every x milliseconds.
     * <li>CACHE_RELOAD_DAILY - Entity reloaded at set time of day.
     * <li>CACHE_RELOAD_WEEKLY - Entity reloaded at set time of week.
     * </ul> <p>
     *
     * Note that his option applies only to Entity ActivationStrategies with
     * an ActivationPolicy of 'ONCE', where the beans remain in the EJB Cache
     * in the 'Active' state (Opt A and ReadOnly), and always implies that the
     * Entity bean is read only.
     **/
    // LI3408
    public int ivCacheReloadType = CACHE_RELOAD_NONE;

    /**
     * A re-load interval in milliseconds to be used for Entity beans
     * that have been configured with an ActivationPolicy of 'ONCE'
     * (Option A and ReadOnly ActivationStrategies). <p>
     *
     * The meaning of the reload value differs depending on the
     * LoadPolicy specified. The meaning is defined by the
     * reload type field, and includes the following values/meanings:
     * <ul>
     * <li>CACHE_RELOAD_NONE - (default) LoadPolicy that provides an
     * interval was not specified and the bean
     * is NOT reloaded at a specified interval,
     * and the value of this field is undefined.
     * <li>CACHE_RELOAD_INTERVAL - LoadPolicy indicates the bean should
     * be reloaded after a specified number of
     * minutes, and this field contains that
     * number of minutes converted to milliseconds.
     * <li>CACHE_RELOAD_DAILY - LoadPolicy indicates the bean should be
     * reloaded at a specific time every day,
     * and this field contains the number of
     * milliseconds from midnight to that time.
     * <li>CACHE_RELOAD_WEEKLY - LoadPolicy indicates the bean should be
     * reloaded at a specific day and time every
     * week, and this feild contains the number
     * of milliseconds from midnight on Sunday
     * to that day and time.
     * </ul> <p>
     *
     * Note that 0 is a valid interval value for all Cache Reload types,
     * but has special meaning for CACHE_RELOAD_INTERVAL, where it indicates
     * that the bean should never be reloaded. This meaning is similar to
     * CACHE_RELOAD_NONE, except that CACHE_RELOAD_NONE will result in
     * Option A caching, whereas CACHE_RELOAD_INTERVAL may result in
     * ReadOnly caching (and multi-threaded / concurrent bean access). <p>
     **/
    // LI3408
    public long ivCacheReloadInterval = -1;

    /**
     * 121048
     * The activation policy, values include
     * ActivationPolicyKind.ONCE - activate once
     * ActivationPolicyKind.ACTIVITY_SESSION - activate at activity session start
     * ActivationPolicyKind.TRANSACTION - activate at transaction start
     */
    public int activationPolicy; // 121048

    public int getActivationPolicy() {
        return activationPolicy;
    } // 121048

    /**
     * Names of bean methods in method id order (sorted).
     */
    public String methodNames[];
    public String localMethodNames[]; // f111627
    public String timedMethodNames[]; // LI2281.07
    public String wsEndpointMethodNames[]; // LI2281.24
    public String lifecycleInterceptorMethodNames[]; // F743-1751

    /**
     * The method level descriptors for the enterprise
     * bean methods in method id order.
     */
    public EJBMethodInfoImpl methodInfos[];
    public EJBMethodInfoImpl localMethodInfos[]; // f111627

    /**
     * The timer methods. If the bean implements TimedObject, the first entry
     * will always be ejbTimeout. Otherwise, if the bean has a Timeout method,
     * the first entry will be that method. All remaining entries will
     * correspond to automatic timers (if there is no ejbTimeout/Timeout method,
     * then the entire array will be automatic timer methods). If the bean
     * has no timer methods, this field will be null.
     *
     * <p>Any method in the array might have multiple automatic timers
     * (Schedule, Schedules, or multiple timer stanzas in XML). This includes
     * the ejbTimeout/Timeout method, if it exists.
     */
    public EJBMethodInfoImpl timedMethodInfos[]; // LI2281.07

    public EJBMethodInfoImpl wsEndpointMethodInfos[]; // LI2281.24
    public EJBMethodInfoImpl lifecycleInterceptorMethodInfos[]; // F743-1751

    /**
     * The method level isolation attributes for the enterprise
     * bean methods in method id order.
     */
    public int isolationAttrs[];

    /**
     * Per method indication of whether method accesses bean read-only
     * or for update of bean methods in method id order.
     */
    public boolean readOnlyAttrs[];

    /**
     * Names of home methods in method id order (sorted).
     */
    public String homeMethodNames[];
    public String localHomeMethodNames[]; // f111627

    /**
     * The method level descriptors for the enterprise
     * bean home methods in method id order.
     */
    public EJBMethodInfoImpl homeMethodInfos[];
    public EJBMethodInfoImpl localHomeMethodInfos[]; // f111627

    /**
     * Isolation level attributes for home methods in method id order.
     */
    public int homeIsolationAttrs[];

    /**
     * Per method indication of whether method accesses bean read-only
     * or for update of home methods in method id order.
     */
    public boolean homeReadOnlyAttrs[];

    /**
     * Local transaction unresolved action behavior
     */
    public boolean commitDanglingWork = false;

    /**
     * True iff this bean has inheritance (i.e. child beans).
     **/
    // d154342.9
    protected boolean ivHasInheritance = false;

    /**
     * True iff this bean has a detectable applicatin managed persistence context.
     * This boolean value will be set to true for StatefulSessionBeans
     * and only under the following circumstances:
     * . The SFSB contains the <code>@PersistenceUnit(s)</code>
     * annotation or <persistence-unit-ref> xml stanza. The existence of
     * a PersistenceUnit indicates application-managed extended persistence context.
     *
     * ***Note**** It is also possible for an application to use JSE methods to
     * directly get the EntityManagerFactory. The is also application-managed extended
     * persistence context, but it is not detectable by the EJBContainer.
     **/
    // d465813
    public boolean ivHasAppManagedPersistenceContext = false;

    /**
     * True iff this bean has a detectable extended persistence context.
     * This boolean value will be set to true for StatefulSessionBeans
     * and only under the following circumstances:
     * . The SFSB contains the <code>@PersistenceContext(s)</code>
     * annotation or <persistence-context-ref> xml stanza specifying "Extended".
     * This indicates container-managed extended persistence context
     *
     **/
    // d465813
    public boolean ivHasCMExtendedPersistenceContext = false;

    /**
     * True iff this bean uses bean managed transactions.
     */
    public boolean usesBeanManagedTx;

    /**
     * True iff this bean uses bean managed activity sessions
     */
    public boolean usesBeanManagedAS = false; // LIDB441.5

    /**
     * Container Managed Persistence Version. Must be set to one of the constants
     * defined in EJBComponentMetaData.
     */
    public int cmpVersion; // 129321

    /**
     * The persister associated with this type of bean. May be null,
     * only used by beans using container-managed persistence.
     */
    public Persister persister; // 114138.3 can't be final - now init'ed after component meta data created

    /**
     * The class loader to use to load EJB classes and interfaces and to define
     * generated classes.
     *
     * @see #ivContextClassLoader
     */
    public ClassLoader classLoader;

    /**
     * The class loader to set as the thread context class loader when calling
     * any user code.
     */
    // F85059
    public ClassLoader ivContextClassLoader;

    /**
     * Minimum and maximum number of elements to keep in this
     * bean's instance pool.
     */
    public int minPoolSize, maxPoolSize;

    /**
     * Can a Timer bean read stale data?
     */
    public int allowCachedTimerDataForMethods = 0; //F001419

    /**
     * Initial number of bean instances to pre-load in this
     * bean's instance pool.
     * Currently only supported for Stateless Session beans.
     **/
    // PK20648
    public int ivInitialPoolSize = 0;

    /**
     * Maximum number of bean instances allowed to be created.
     * The total number created must remain <= instances in use +
     * instances in free pool.
     * Currently only supported for Stateless Session beans.
     **/
    // PK20648
    public int ivMaxCreation = 0;

    /**
     * Time to wait for the next available bean instance when the maximum number of bean
     * instances has been configured and the maximum number of bean instances is currently
     * in use. See {@link #ivMaxCreation}.
     */
    public long ivMaxCreationTimeout = 300000; // 5 minutes

    /**
     * True indicates all Entity beans that have been modified in the
     * transaction will be flushed out to the datastore prior to executing
     * a finder method on this home. <p>
     *
     * The default is true. (Spec-mandated for EJB 2.0 and later modules) <p>
     *
     **/
    // d122418-7
    public boolean isPreFindFlushEnabled = true;

    /**
     * True indicates that ejbStore will be invoked on this EntityBean at the end of the
     * transaction even if the bean has not been dirtied during the tran. (This is the
     * EJB spec-mandated behavior.) <p>
     *
     * The default is true. <p>
     */
    public boolean isCleanEJBStoreEnabled = true; // d237506

    /**
     * Used to record whether console message as already been issued for this condition,
     * so message will only be issued once if all beans are overridden, rather than repeatedly
     * for each bean type.
     */
    public static boolean fbpkReadOnlyOverrideAllBeans = false;

    // Enable Custom Finders for all beans  d112604.1 begin

    public static boolean allowCustomFinderSQLForUpdateALLBeans = false; // use to avoid string lookups for per bean cases
    public boolean allowCustomFinderSQLForUpdateThisBean = false;
    public boolean allowCustomFinderSQLForUpdateMethodLevel = false;
    public boolean allowWAS390CustomFinderAtBeanLevel = false; // 390 compatablity check

    public static final int CF_METHOD_NAME_OFFSET = 0; // stores the method name of custom finder
    public static final int CF_METHOD_SIG_OFFSET = 1; // stores the method signature of custom finder

    // d112604.1 end

    /**
     * True if the bean implements the javax.ejb.TimedObject interface, or has
     * a configured 'timeout' method (<timeout-method> or @Timeout).
     **/
    // LI2281.07
    public boolean isTimedObject = false;

    /**
     * Timeout Method reflect object, when NOT implementing javax.ejb.TimedObject.
     **/
    // d438133.2
    public Method ivTimeoutMethod = null;

    /**
     * True if the bean implements the
     * com.ibm.websphere.ejbcontainer.LightweightLocal marker interface, or
     * has the corresponding env-var defined as a boolean true.
     **/
    // LI3795-56
    public boolean isLightweight = false;

    /**
     * JNDIname of ActivationSpec object for RA when a MDB
     * is bound to a JCA 1.5 resource adapter rather than a
     * JMS provider.
     */
    public String ivActivationSpecJndiName = null; //LI2110.41

    /**
     * ActivationSpec Authenication Alias to use for RA when a MDB
     * is bound to a JCA 1.5 resource adapter rather than a
     * JMS provider.
     */
    public String ivActivationSpecAuthAlias = null; //LI2110-46

    /**
     * JNDIname of the MessageDestinationRef to be passed
     * to RALifeCycleManager when MessageEndpoint is activated.
     */
    public String ivMessageDestinationJndiName = null; //LI2110-46

    public String ivMessageListenerPortName; // F743-36290.1

    /**
     * The name of the interface class that a MDB implements. Note,
     * this is commonly refered to "message listener interface" for
     * MDB 2.0 beans and is specified by "messaging-type" element in
     * MDB 2.1 deployment descriptor.
     */
    public String ivMessagingTypeClassName = null; //LI2110.41

    /**
     * Application version ID.
     */
    // LI2110.41
    public int ivApplicationVersionId = JAVAEE_VERSION_5;

    /**
     * List of WCCM ActivationConfigProperty objects to use when
     * activating the RA for a message endpoint. Make users do
     * a cast to access WCCM data.
     */
    public Properties ivActivationConfig; //LI2110.41

    /**
     * Is this EJB initialized early at application start time, or
     * later when a running application actually needs it?
     */
    public boolean ivDeferEJBInitialization = true;

    /**
     * Cluster Identity to be used for registerServant/export of
     * Remote Home (wrappers) and Remote instance (wrappers), except
     * instances of Stateful Session beans. <p>
     *
     * Will be null if there are no remote interfaces or when running
     * in a single server environment. <p>
     **/
    // LI2401-11
    public Object ivCluster = null;

    // F743-21481
    /**
     * The list of <code>InjectionTarget</code> instances that are visible
     * to the ejb class itself.
     *
     * This list is null until after the reference processing has occurred.
     */
    public InjectionTarget[] ivBeanInjectionTargets;

    /**
     * Map of java:comp/env jndi name to InjectionBinding, which is filled
     * in by the InjectionEngine as 'references' are processed. <p>
     *
     * This map does NOT need to by thread safe, as it will not be
     * modified after the call to populate it. <p>
     **/
    // d473811
    public Map<String, InjectionBinding<?>> ivJavaColonCompEnvMap;

    /**
     * Following is a collection WCCM Objects. We
     * stuff them into a separate data object so that
     * they can be easily released after the bean is
     * started.
     */
    public WCCMMetaData wccm;

    /**
     * EJB3 Session beans and Entity beans may have a component ID field
     * specified in the bindings file. If so, it provides a replacement
     * string to overrid the appName / module / beanName part of the default
     * binding name. If not, this field will remain null;
     */
    public String ivComponent_Id = null;

    public int removeTxAttr;
    public EJSContainer container;

    public HomeRecord homeRecord; //LIDB859-4

    public List<?> accessIntentList;
    public List<?> isoLevelList;

    /*** new for EJB3 *********************************************************/

    /**
     * CallbackKind to know how to invoke callback methods for this bean.
     */
    public CallbackKind ivCallbackKind = CallbackKind.None; // d367572.3

    /**
     * Interceptor meta data needed for invoking life cycle callback
     * event interceptors or null.
     */
    public InterceptorMetaData ivInterceptorMetaData = null; // d367572.3

    /**
     * A map of Init methods (e.g. @Init) that is used for EJB 3 SFSB.
     * The key of the map is one of the following:
     * <ul>
     * <li>
     * When bean does not implement javax.ejb.SessionBean, the key is
     * Init.value() + MethodAttribUtils.jdiMethodSignature( Method )
     * <li>
     * When bean does implement javax.ejb.SessionBean, the key is
     * "create" + <METHOD> + MethodAttribUtils.jdiMethodSignature( Method )
     * where <METHOD> is the suffix of the ejbCreate<METHOD> method in
     * the bean as required by EJB 2.1 API or earlier.
     * </ul>
     */
    public HashMap<String, String> ivInitMethodMap = null; // d384182

    /**
     * A map of security role links to their corresponding roles.
     * The key of the map is the following:
     * <ul>
     * <li>
     * The key will be the role-link string.
     * </ul>
     */
    public HashMap<String, String> ivRoleLinkMap = null; // d366845.11.1

    /**
     * Flag indicating that all metadata to configure this bean was
     * provided only via XML, not annotations. Start out assuming we
     * might have annotations to process.
     */
    public boolean metadataComplete = false;

    /**
     * boolean indicating that either "use-caller-identity" has been
     * specified in XML or that the security principle for this
     * Enterprise Bean has been allowed to default.
     */
    public boolean ivUseCallerIdentity = false;

    /**
     * String indicating the run-as identity for the execution of all
     * methods for this EJB. //366845.11.1
     */
    public String ivRunAs = null;

    /**
     * The java reflection object of a ejbCreate method if one needs to be
     * invoked for a Stateless session bean when CallbackKind is SessionBean
     * or for a MDB when CallbackKind is MessageDrivenBean.
     */
    public Method ivEjbCreateMethod; // d453778

    /**
     * Set to true when the fireMetaDataCreated method is called for
     * this BeanMetaData so that we know whether to call the
     * fireMetaDataDestroyed when the BeanMetaData is removed from the
     * array of ComponentMetaData kept in EJBModuleMetaDataImpl object.
     */
    public boolean ivMetaDataDestroyRequired = false; // d505055

    /*
     * When a user uses WebSphere's notion of CMP 1.x inheritance and a 'parent' bean is
     * retrieved from the DB, any 'children' associated with the 'parent' are also retrieved
     * from the DB. However, the 'children' are not hydrated with the result set. As such,
     * when the user makes calls on a child, another SQL is generated to get the child. This
     * results in a performance degradation of course. Most of the limitations for not hydrating
     * the child/children are due to deficiencies in deployed code and feature F001925 was opened
     * to address this issue.
     * This variable is set to false by default (i.e. support existing customers) and set to true
     * when the user generates the persister with feature F001925 enabled.
     */
    public boolean supportsFluffOnFind = false; //  F001925

    /*** new for EJB3.1 *********************************************************/

    /**
     * Set to true when a Singleton session bean uses bean managed
     * concurrency. The default is false (ie. Singletons use container
     * managed concurrency by default).
     */
    public boolean ivSingletonUsesBeanManagedConcurrency = false;

    /**
     * Set to true when this bean has one or more asynchronous methods.
     * Default is false.
     */
    public boolean ivHasAsynchMethod = false;

    /**
     * Stateful AfterBegin session synchronization method when bean does
     * not implement the SessionSynchronization interface.
     */
    // F743-25855
    public Method ivAfterBegin = null;

    /**
     * Stateful BeforeCompletion session synchronization method when bean does
     * not implement the SessionSynchronization interface.
     */
    // F743-25855
    public Method ivBeforeCompletion = null;

    /**
     * Stateful AfterCompletion session synchronization method when bean does
     * not implement the SessionSynchronization interface.
     */
    // F743-25855
    public Method ivAfterCompletion = null;

    // F743-17630
    /**
     * Temporary list of Methods exposed on local interfaces.
     * Null after the bean is fully initialized.
     */
    public Method[] methodsExposedOnLocalInterface = null;

    /**
     * Temporary list of Methods exposed on remote interfaces.
     * Null after the bean is fully initialized.
     */
    public Method[] methodsExposedOnRemoteInterface = null;

    /**
     * Temporary list of Methods exposed on local home interfaces.
     * Null after the bean is fully initialized.
     */
    public Method[] methodsExposedOnLocalHomeInterface = null;

    /**
     * Temporary list of Methods exposed on remote home interfaces.
     * Null after the bean is fully initialized.
     */
    public Method[] methodsExposedOnRemoteHomeInterface = null;

    /**
     * Temporary list of all public Methods exposed on local interfaces.
     * Null after the bean is fully initialized.
     */
    public Method[] allPublicMethodsOnBean = null;

    /**
     * Temporary list of Method--TO---EJBMethodInfoImpl mappings.
     * Null after the bean is fully initialized.
     */
    public Map<Method, ArrayList<EJBMethodInfoImpl>> methodsToMethodInfos = null;

    // F743-21481
    public ReferenceContext ivReferenceContext;

    /**
     * A map of class name to a map of declared fields by name. The bean class,
     * all of its super classes, all interceptor classes, and all interceptor
     * super classes are in this object. The map of fields does not include
     * static or transient fields. All Field objects are accessible. This
     * field will be null until it is populated by StatefulPassivator.
     */
    public volatile Map<String, Map<String, Field>> ivPassivatorFields; // d648122

    /**
     * When true, the MDB interface has no methods, which results in
     * all non-static public methods of the bean being exposed.
     * New as of MDB 3.2.
     **/
    public boolean ivIsNoMethodInterfaceMDB;

    /**
     * Construct a new <code>BeanMetaData</code> instance.
     * Do as little as possible in ctor, but run superclass ctor to
     * setup the BMD instance as ComponentMetaData on thread slot.
     */
    public BeanMetaData(int slotSize) {
        super(slotSize);
    }

    public boolean isEntityBean() // d121556
    {
        return type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY ||
               type == InternalConstants.TYPE_BEAN_MANAGED_ENTITY;
    } // d121556

    public boolean isSessionBean() // d121556
    {
        return type == InternalConstants.TYPE_SINGLETON_SESSION ||
               type == InternalConstants.TYPE_STATELESS_SESSION ||
               type == InternalConstants.TYPE_STATEFUL_SESSION;
    }

    public boolean isSingletonSessionBean() // F743-508
    {
        return type == InternalConstants.TYPE_SINGLETON_SESSION;
    }

    public boolean isStatelessSessionBean() // d121556
    {
        return type == InternalConstants.TYPE_STATELESS_SESSION;
    }

    public boolean isStatefulSessionBean() // d121556
    {
        return type == InternalConstants.TYPE_STATEFUL_SESSION;
    }

    public boolean isMessageDrivenBean() // d121556
    {
        return type == InternalConstants.TYPE_MESSAGE_DRIVEN;
    }

    public boolean isManagedBean() // F743-34301
    {
        return type == InternalConstants.TYPE_MANAGED_BEAN;
    }

    public Class<?> getLocalHomeInterface() // d121556
    { // d121556
        return localHomeInterfaceClass; // d121556
    }

    public Class<?> getRemoteHomeImpl() // LIDB859-4
    { // LIDB859-4
        return homeRemoteImplClass; // LIDB859-4
    }

    public int getEJBTransactionPolicy() // d126512
    {
        return usesBeanManagedTx ? InternalConstants.TX_POLICY_BEAN_MANAGED : InternalConstants.TX_POLICY_CONTAINER_MANAGED;
    }

    /**
     * Gets the index of the local business interface.
     *
     * @param interfaceName the interface to find the index of
     * @return the index of the interface passed in
     */
    public int getLocalBusinessInterfaceIndex(String interfaceName) {
        if (ivBusinessLocalInterfaceClasses != null) {
            for (int i = 0; i < ivBusinessLocalInterfaceClasses.length; i++) {
                String bInterfaceName = ivBusinessLocalInterfaceClasses[i].getName();
                if (bInterfaceName.equals(interfaceName)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "getLocalBusinessInterfaceIndex : " +
                                     bInterfaceName + " at index " + i);
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Gets the index of the local business interface. This will also
     * try to match the interface class passed in to a super type of a
     * known interface of an EJB. <p>
     *
     * Note: this should only be used if necessary, as this is an expensive
     * check. The getLocalBusinessInterfaceIndex method should be used
     * if this specialized check is not needed.
     *
     * @param target the class that needs to be matched to an interface
     * @return the index of the found matching interface
     */
    public int getAssignableLocalBusinessInterfaceIndex(Class<?> target) {
        if (ivBusinessLocalInterfaceClasses != null) {
            for (int i = 0; i < ivBusinessLocalInterfaceClasses.length; i++) {
                Class<?> bInterface = ivBusinessLocalInterfaceClasses[i];
                if (target.isAssignableFrom(bInterface)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "getAssignableLocalBusinessInterfaceIndex : " +
                                     bInterface.getName() + " at index " + i);
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Gets the index of the local busines interface. This method will throw an
     * IllegalStateException if a local interface could not be matched. If it is
     * known that a match must occur on a local business interface, then this
     * method should be used.
     *
     * @param interfaceName the interface to find a match on
     * @return the index of the business interface found
     *
     * @throws IllegalStateException if the business interface cannot be found
     */
    public int getRequiredLocalBusinessInterfaceIndex(String interfaceName) throws IllegalStateException {
        int interfaceIndex = getLocalBusinessInterfaceIndex(interfaceName);

        if (interfaceIndex == -1) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getRequiredLocalBusinessInterfaceIndex : IllegalStateException : " +
                             "Requested business interface not found : " +
                             interfaceName);

            throw new IllegalStateException("Requested business interface not found : " + interfaceName);
        }

        return interfaceIndex;
    }

    /**
     * Returns the Local Business interface index for this bean, which implements
     * the specified target interface class. <p>
     *
     * If the target interface is one of the Local Business interfaces, its index
     * will be returned. Otherwise, the first Local Business interface found
     * which implements the specified target interface will be returned. <p>
     *
     * If no Local Business interface is found which implements the specified
     * target interface, then an EJBConfigurationException will be thrown, as
     * the customer has likely configured an EJB injection improperly. <p>
     *
     * This method is intended for use when processing ejb-link resolution. <p>
     *
     * @param interfaceName the name of the interface that needs to be matched
     * @return the index of the found matching Local Business interface
     * @throws EJBConfigurationException if none of the Local Business
     *             interfaces implement the specified target interface
     * @throws ClassNotFoundException if the interface name passed in could
     *             not be loaded
     **/
    // d449434
    public int getSupportingLocalBusinessInterfaceIndex(String interfaceName) throws ClassNotFoundException, EJBConfigurationException {
        int interfaceIndex = getLocalBusinessInterfaceIndex(interfaceName);

        if (interfaceIndex == -1) {
            Class<?> target = classLoader.loadClass(interfaceName);
            interfaceIndex = getAssignableLocalBusinessInterfaceIndex(target);

            if (interfaceIndex == -1) {
                Tr.error(tc, "ATTEMPT_TO_REFERENCE_MISSING_INTERFACE_CNTR0154E",
                         new Object[] { enterpriseBeanName,
                                        _moduleMetaData.ivName,
                                        interfaceName });
                EJBConfigurationException ejbex = new EJBConfigurationException("Another component is attempting to reference local interface: " + interfaceName +
                                                                                " which is not implemented by bean: " + j2eeName);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "getSupportingLocalBusinessInterfaceIndex : " + ejbex);

                throw ejbex;
            }
        }

        return interfaceIndex;
    }

    /**
     * Gets the index of the remote business interface.
     *
     * @param interfaceName the interface to find the index of
     * @return the index of the interface passed in
     */
    public int getRemoteBusinessInterfaceIndex(String interfaceName) {
        if (ivBusinessRemoteInterfaceClasses != null) {
            for (int i = 0; i < ivBusinessRemoteInterfaceClasses.length; i++) {
                String bInterface = ivBusinessRemoteInterfaceClasses[i].getName();
                if (bInterface.equals(interfaceName)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "getRemoteBusinessInterfaceIndex : " +
                                     bInterface + " at index " + i);
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Gets the index of the remote business interface. This will also
     * try to match the interface class passed in to a super type of a
     * known interface of an EJB. <p>
     *
     * Note: this should only be used if necessary, as this is an expensive
     * check. The getRemoteBusinessInterfaceIndex method should be used
     * if this specialized check is not needed.
     *
     * @param target the class that needs to be matched to an interface
     * @return the index of the found matching interface
     */
    public int getAssignableRemoteBusinessInterfaceIndex(Class<?> target) {
        if (ivBusinessRemoteInterfaceClasses != null) {
            for (int i = 0; i < ivBusinessRemoteInterfaceClasses.length; i++) {
                Class<?> bInterface = ivBusinessRemoteInterfaceClasses[i];
                if (target.isAssignableFrom(bInterface)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "getAssignableRemoteBusinessInterfaceIndex : " +
                                     bInterface.getName() + " at index " + i);
                    return i;
                }
            }
        }

        return -1;
    }

    /**
     * Gets the index of the remote busines interface. This method will throw an
     * IllegalStateException if a remte interface could not be matched. If it is
     * known that a match must occur on a remote business interface, then this
     * method should be used.
     *
     * @param interfaceName the interface to find a match on
     * @return the index of the business interface found
     *
     * @throws IllegalStateException if the requested business interface cannot be found.
     */
    public int getRequiredRemoteBusinessInterfaceIndex(String interfaceName) throws IllegalStateException {
        int interfaceIndex = getRemoteBusinessInterfaceIndex(interfaceName);

        if (interfaceIndex == -1) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getRequiredRemoteBusinessInterfaceIndex : IllegalStateException : " +
                             "Requested business interface not found : " +
                             interfaceName);

            throw new IllegalStateException("Requested business interface not found : " + interfaceName);
        }

        return interfaceIndex;
    }

    /**
     * Returns the Remote Business interface index for this bean, which implements
     * the specified target interface class. <p>
     *
     * If the target interface is one of the Local Business interfaces, its index
     * will be returned. Otherwise, the first Remote Business interface found
     * which implements the specified target interface will be returned. <p>
     *
     * If no Remote Business interface is found which implements the specified
     * target interface, then an EJBConfigurationException will be thrown, as
     * the customer has likely configured an EJB injection improperly. <p>
     *
     * This method is intended for use when processing ejb-link resolution. <p>
     *
     * @param interfaceName the name of the interface that needs to be matched
     * @return the index of the found matching Local Business interface
     * @throws EJBConfigurationException if none of the Local Business
     *             interfaces implement the specified target interface
     * @throws ClassNotFoundException if the interface name passed in could
     *             not be loaded
     **/
    // d449434
    public int getSupportingRemoteBusinessInterfaceIndex(String interfaceName) throws ClassNotFoundException, EJBConfigurationException {
        int interfaceIndex = getRemoteBusinessInterfaceIndex(interfaceName);

        if (interfaceIndex == -1) {
            Class<?> target = classLoader.loadClass(interfaceName);
            interfaceIndex = getAssignableRemoteBusinessInterfaceIndex(target);

            if (interfaceIndex == -1) {
                Tr.error(tc, "ATTEMPT_TO_REFERENCE_MISSING_INTERFACE_CNTR0154E",
                         new Object[] { enterpriseBeanName,
                                        _moduleMetaData.ivName,
                                        interfaceName });
                EJBConfigurationException ejbex = new EJBConfigurationException("Another component is attempting to reference local interface: " + interfaceName +
                                                                                " which is not implemented by bean: " + j2eeName);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "getSupportingRemoteBusinessInterfaceIndex : " + ejbex);

                throw ejbex;
            }
        }

        return interfaceIndex;
    }

    /**
     * Format a representation of this <code>BeanMetaData</code>
     * instance to the trace stream. <p>
     */
    public void dump() {
        /*
         * Defect 88284
         * Performance hit of creating data structure, which occupies memory
         * even though if the trace is not turned ON. So suppress creation
         * by checking trace and debug flags.
         */
        if (!TraceComponent.isAnyTracingEnabled() || !(tc.isDumpEnabled() ||
                                                       tc.isDebugEnabled())) {
            return;
        }

        introspect(new TrDumpWriter(tc));
    }

    /**
     * Writes the significant state data of this class, in a readable format,
     * to the specified output writer. <p>
     *
     * @param writer output resource for the introspection data
     */
    // F86406
    public void introspect(IntrospectionWriter writer) {
        // If BMD is fully initialized dump out everything, otherwise just dump
        // out the subset we have filled in (ie. before deferred initialization
        // takes place

        if (fullyInitialized) {
            //F743-1752CodRev start
            String singletonConcurrency = "not applicable";
            if (isSingletonSessionBean()) {
                if (ivSingletonUsesBeanManagedConcurrency) {
                    singletonConcurrency = "bean managed";
                } else {
                    singletonConcurrency = "container managed";
                }
            } //F743-1752CodRev end

            String allData[] = new String[] {
                                              this.toIntrospectString(),
                                              "Application version         = " + ivApplicationVersionId,
                                              "CMP version                 = " + cmpVersion,
                                              "SupportsFluffOnFind         = " + supportsFluffOnFind, //F001925
                                              "SimpleBindingName           = " + simpleJndiBindingName,
                                              "LocalHomeBindingName        = " + localHomeJndiBindingName,
                                              "RemoteHomeBindingName       = " + remoteHomeJndiBindingName,
                                              "BusinessIntfBindingNames    = " + businessInterfaceJndiBindingNames,
                                              "HomeInterfaceClassName      = " + homeInterfaceClassName,
                                              "HomeInterfaceClass          = " + homeInterfaceClass,
                                              "RemoteInterfaceClass        = " + remoteInterfaceClass,
                                              "RemoteImplClass             = " + remoteImplClass,
                                              "HomeRemoteImplClass         = " + homeRemoteImplClass,
                                              "LocalHomeInterfaceClass     = " + localHomeInterfaceClass, // f111627
                                              "LocalInterfaceClass         = " + localInterfaceClass, // f111627
                                              "LocalImplClass              = " + localImplClass, // f111627
                                              "HomeLocalImplClass          = " + homeLocalImplClass, // f111627
                                              "HasWebServiceEndpoint       = " + ivHasWebServiceEndpoint,
                                              "WebService Endpoint Class   = " + webserviceEndpointInterfaceClass, // LI2281.24
                                              "WebService Endpoint Proxy   = " + ivWebServiceEndpointProxyClass, // d497921
                                              "BusinessRemoteInterfaceClass= " + Arrays.toString(ivBusinessRemoteInterfaceClasses),
                                              "BusinessRemoteImplClass     = " + Arrays.toString(ivBusinessRemoteImplClasses),
                                              "No-Interface View           = " + ivLocalBean,
                                              "BusinessLocalInterfaceClass = " + Arrays.toString(ivBusinessLocalInterfaceClasses),
                                              "BusinessLocalImplClass      = " + Arrays.toString(ivBusinessLocalImplClasses),
                                              "EnterpriseBeanClass         = " + enterpriseBeanClass, // LI2281.07
                                              "EnterpriseBeanAbstractClass = " + enterpriseBeanAbstractClass, // d174057.3
                                              "pKeyClass                   = " + pKeyClass,
                                              "J2EE Name                   = " + j2eeName,
                                              "Unversioned J2EE Name       = " + ivUnversionedJ2eeName, // F54184
                                              "Cluster Identity            = " + ivCluster, // LI2401-11
                                              "ActivationSpecJndiName      = " + ivActivationSpecJndiName, // d174057.3
                                              "MessageDestinationJndiName  = " + ivMessageDestinationJndiName, //LI2110-46
                                              "Connection factory name     = " + connFactoryName,
                                              "Pool Size (min,max)         = (" + minPoolSize + "," + maxPoolSize + ")",
                                              "Initial Pool Size           = " + ivInitialPoolSize,
                                              "Max Beans Created           = " + ivMaxCreation,
                                              "Max Beans Created Timeout   = " + ivMaxCreationTimeout,
                                              "TimedObject                 = " + isTimedObject, // LI2281.07
                                              "Reentrant                   = " + reentrant,
                                              "Passivation Capable         = " + passivationCapable,
                                              "Session Timeout             = " + sessionTimeout, // d140886.1
                                              "PreFindFlush                = " + isPreFindFlushEnabled,
                                              "Commit Option               = " + getCommitOptionString(),
                                              "Cache Reload Interval       = " + getCacheReloadIntervalString(),
                                              "LightweightLocal            = " + isLightweight,
                                              "AllowCachedTimerDataFor     = " + allowCachedTimerDataForMethods, //F001419
                                              "DeferredInit                = " + ivDeferEJBInitialization,
                                              "Security RunAs              = " + ivRunAs,
                                              "Interceptor CallbackKind    = " + ivCallbackKind,
                                              "InjectionTargets            = " + Arrays.toString(ivBeanInjectionTargets), // F743-21481
                                              "Component Id                = " + ivComponent_Id,
                                              "Metadata Complete           = " + metadataComplete,
                                              "Has Ext CM Persist. Cntx.   = " + ivHasCMExtendedPersistenceContext,
                                              "Has App Man Persit. Cntx.   = " + ivHasAppManagedPersistenceContext,
                                              "Persistence Ref Names       = " + ivPersistenceRefNames, // F743-30682
                                              "ExPC Ids                    = " + Arrays.toString(ivExPcPuIds), // F743-30682
                                              "WebService Endpoint Created = " + ivWebServiceEndpointCreated, // d497921
                                              "Component NameSpace :  nsid = " + getJavaNameSpaceID(), // d508455
                                              "Has aysnchronous method(s)  = " + ivHasAsynchMethod,
                                              "Singleton Concurrency Type  = " + singletonConcurrency, //F743-1752CodRev
                                              "Synch AfterBegin            = " + ivAfterBegin, // F743-25855
                                              "Synch BeforeCompletion      = " + ivBeforeCompletion, // F743-25855
                                              "Synch AfterCompletion       = " + ivAfterCompletion, // F743-25855
                                              "Application Classloader     = " + classLoader,
                                              "Context class loader        = " + (classLoader == ivContextClassLoader ? "(same)" : ivContextClassLoader)
            };

            writer.begin("BeanMetaData Dump");
            writer.dump(allData);

            //--------------------------
            // Now dump per-method info
            //--------------------------
            // PK31372 start
            if (methodInfos != null) {
                writer.begin("Remote Methods : " + methodInfos.length);
                for (int i = 0; i < methodInfos.length; i++) {
                    methodInfos[i].introspect(writer, "Remote", i, true);
                }
                writer.end();
            }

            if (homeMethodInfos != null) {
                writer.begin("Home Methods : " + homeMethodInfos.length);
                for (int i = 0; i < homeMethodInfos.length; i++) {
                    homeMethodInfos[i].introspect(writer, "Remote Home", i, true);
                }
                writer.end();
            }

            if (localMethodInfos != null) {
                writer.begin("Local Methods : " + localMethodInfos.length);
                for (int i = 0; i < localMethodInfos.length; i++) {
                    localMethodInfos[i].introspect(writer, "Local", i, true);
                }
                writer.end();
            }

            if (localHomeMethodInfos != null) {
                writer.begin("Local Home Methods : " + localHomeMethodInfos.length);
                for (int i = 0; i < localHomeMethodInfos.length; i++) {
                    localHomeMethodInfos[i].introspect(writer, "Local Home", i, true);
                }
                writer.end();
            }

            if (wsEndpointMethodInfos != null &&
                (webserviceEndpointInterfaceClass != null ||
                 ivWebServiceEndpointCreated)) // d497921
            {
                writer.begin("WebService Endpoint Methods : " + wsEndpointMethodInfos.length);
                for (int i = 0; i < wsEndpointMethodInfos.length; i++) {
                    wsEndpointMethodInfos[i].introspect(writer, "Service Endpoint", i, true);
                }
                writer.end();
            }

            if (timedMethodInfos != null) { // LI2281.07
                writer.begin("Timer Methods : " + timedMethodInfos.length);
                for (int i = 0; i < timedMethodInfos.length; i++) {
                    timedMethodInfos[i].introspect(writer, "Timer", i, true);
                }
                writer.end();
            }

            if (lifecycleInterceptorMethodInfos != null) // F743-1751
            {
                writer.begin("Lifecycle Interceptor Methods : " + lifecycleInterceptorMethodInfos.length);
                for (int i = 0; i < lifecycleInterceptorMethodInfos.length; i++) {
                    lifecycleInterceptorMethodInfos[i].introspect(writer, "Lifecycle", i, true);
                }
                writer.end();
            }
            // PK31372 end of change.

            /*
             * It would be nice to dump the java:comp name space at this time but
             * there are a couple issue that need to be resolved.
             * 1. The dumps should be into the RAS PrintStream instead of DumpNameSpace's default of System.out
             * 2. The target of the EJB refs may not be bound yet, so NameNotFound errors will be generated. The
             * solution is to move this dump after all EJBs in the module have been loaded.
             * Tr.dump(tc, "Dumping java:comp name space for the EJB: ");
             * DumpNameSpace dumper = new DumpNameSpace();
             * dumper.generateDump(getJavaNameSpaceContext());
             */

            // If we are in a deferred init scenario, dump the metadata
            // we have so far.  The finished metadata object will be dumped
            // once deferred init processing happens later

        } else {
            String partialData[] = new String[] {
                                                  this.toIntrospectString(),
                                                  "Application version         = " + ivApplicationVersionId,
                                                  "CMP version                 = " + cmpVersion,
                                                  "SimpleBindingName           = " + simpleJndiBindingName,
                                                  "LocalHomeBindingName        = " + localHomeJndiBindingName,
                                                  "RemoteHomeBindingName       = " + remoteHomeJndiBindingName,
                                                  "BusinessIntfBindingNames    = " + businessInterfaceJndiBindingNames,
                                                  "HomeInterfaceClassName      = " + homeInterfaceClassName,
                                                  "HasWebServiceEndpoint       = " + ivHasWebServiceEndpoint,
                                                  "No-Interface View           = " + ivLocalBean,
                                                  "J2EE Name                   = " + j2eeName,
                                                  "Unversioned J2EE Name       = " + ivUnversionedJ2eeName, // F54184
                                                  "Connection factory name     = " + connFactoryName,
                                                  "DeferredInit                = " + ivDeferEJBInitialization,
                                                  "Metadata Complete           = " + metadataComplete,
                                                  "Component Id                = " + ivComponent_Id,
                                                  "Application Classloader     = " + classLoader
            };

            writer.begin("Partial BeanMetaData Dump");
            writer.dump(partialData);
        } // if/else fully initialized

        writer.end();
    } // dump

    /**
     * Returns the name of the configured bean caching/commit option
     * for trace purposes. <p>
     *
     * For example, an Entity bean configured with an Activation policy of
     * "Once" and a Load policy of "Activation" will return something
     * like "Entity Option A". <p>
     **/
    // LI3408
    private String getCommitOptionString() {
        int strategy;

        switch (type) {
            case InternalConstants.TYPE_SINGLETON_SESSION:
            case InternalConstants.TYPE_STATELESS_SESSION:
            case InternalConstants.TYPE_MESSAGE_DRIVEN:
                strategy = Activator.UNCACHED_ACTIVATION_STRATEGY;
                break;

            case InternalConstants.TYPE_STATEFUL_SESSION:
                if (sessionActivateTran) {
                    strategy = Activator.STATEFUL_ACTIVATE_TRAN_ACTIVATION_STRATEGY;
                } else if (sessionActivateSession) {
                    strategy = Activator.STATEFUL_ACTIVATE_SESSION_ACTIVATION_STRATEGY;
                } else {
                    strategy = Activator.STATEFUL_ACTIVATE_ONCE_ACTIVATION_STRATEGY;
                }
                break;

            case InternalConstants.TYPE_BEAN_MANAGED_ENTITY:
            case InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY:
                if (optionACommitOption) {
                    strategy = Activator.OPTA_ENTITY_ACTIVATION_STRATEGY;
                } else if (optionBCommitOption) {
                    strategy = Activator.OPTB_ENTITY_ACTIVATION_STRATEGY;
                } else if (entitySessionalTranOption) {
                    strategy = Activator.ENTITY_SESSIONAL_TRAN_ACTIVATION_STRATEGY;
                } else if (ivReadOnlyCommitOption) {
                    strategy = Activator.READONLY_ENTITY_ACTIVATION_STRATEGY;
                } else {
                    strategy = Activator.OPTC_ENTITY_ACTIVATION_STRATEGY;
                }
                break;

            default:
                strategy = -1;
        }

        // Ask the Activator for the appropriate activation strategy string.
        return Activator.getActivationStrategyString(strategy);
    }

    /**
     * Returns the name of the configured bean caching reload option
     * and the reload interval value for trace purposes. <p>
     *
     * For example, an Entity bean configured with an Activation policy of
     * "Once" and a Load policy of "Interval" will return something
     * like "INTERVAL (1200000)" for a 20 minute interval. <p>
     **/
    // LI3408
    private String getCacheReloadIntervalString() {
        String reload;

        switch (ivCacheReloadType) {
            case CACHE_RELOAD_NONE:
                reload = "N/A";
                break;

            case CACHE_RELOAD_INTERVAL:
                reload = "INTERVAL (" + ivCacheReloadInterval + ")";
                break;

            case CACHE_RELOAD_DAILY:
                reload = "DAILY (" + ivCacheReloadInterval + ")";
                break;

            case CACHE_RELOAD_WEEKLY:
                reload = "WEEKLY (" + ivCacheReloadInterval + ")";
                break;

            default:
                reload = "UNKNOWN (" + ivCacheReloadInterval + ")";
        }

        return reload;
    }

    /**
     * Converts the integer-encoded representation of absolute reload time
     * in the bean's deployment descriptor, to the number of milliseconds
     * past midnight. The absolute reload time is encoded in so-called
     * "military time", for example 2215 represents 22:15 or 10:15 PM.
     * If the "weekly" reload is being used, the reload time is prefaced
     * by an additional digit indicating the day of week, 1=Sunday and
     * 7=Saturday. For example, 20530 represents 5:30 in the morning on Monday.
     * In this case the number returned is the number of milliseconds past
     * midnight on Sunday.
     **/
    // LI3408
    public static long convertDDAbsoluteReloadToMillis(int ddInterval,
                                                       int reloadType) {
        long millis = 0;

        if (ddInterval >= 0) {
            switch (reloadType) {
                case CACHE_RELOAD_WEEKLY:
                    if (ddInterval >= 10000) {
                        millis = millis + ((ddInterval / 10000) - 1) * 24 * 60 * 60 * 1000;
                        ddInterval = ddInterval % 10000;
                    }
                case CACHE_RELOAD_DAILY:
                    if (ddInterval <= 2359) {
                        millis = millis + (((ddInterval / 100) * 60) +
                                           (ddInterval % 100))
                                          * 60 * 1000;
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        return millis;
    }

    /**
     * Get SfFailoverClient for this SFSB. A null reference is returned
     * if not a SFSB or SFSB failover is not enabled for this SFSB.
     *
     * @return The <code>SfFailoverClient</code> instance associated with this
     *         <code>SFSB</code> or null.
     */
    public final SfFailoverClient getSfFailoverClient() //LIDB2018-1
    {
        return ivSfFailoverClient;
    }

    /**
     *
     * @return EJSHome
     */
    //d345616
    public EJSHome getHome() {
        return this.homeRecord.getHome();
    }

    /**
     *
     * @return J2EEName
     * @see com.ibm.ws.runtime.metadata.ComponentMetaData
     */
    @Override
    public J2EEName getJ2EEName() {
        return j2eeName;
    }

    /**
     *
     * @return ModuleMetaDataImpl
     * @see com.ibm.ws.runtime.metadata.ComponentMetaData
     */
    @Override
    public ModuleMetaData getModuleMetaData() {
        return _moduleMetaData;
    }

    /**
     *
     * @return String
     * @see com.ibm.ws.runtime.metadata.MetaData
     */
    @Override
    public String getName() {
        return j2eeName.getComponent();
    }

    /**
     *
     * @see com.ibm.ws.runtime.metadata.MetaData
     */
    @Override
    public void release() {
        // null out any wccm stuff no longer needed
    }

    /**
     *
     * @return Context
     * @see com.ibm.ws.runtime.metadata.ComponentMetaData
     */
    public Context getJavaNameSpaceContext() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            // F743-506 - jns can be null if BeanMetaData is used by scheduler
            // during automatic timer creation.
            Tr.debug(tc, "getJavaNameSpaceContext: nsID=" + getJavaNameSpaceID() +
                         "  :  J2EEName = " + j2eeName); // d508455

        return _javaNameSpaceContext;
    }

    public void setJavaNameSpace(Object jns) {
        // Nothing.
    }

    protected int getJavaNameSpaceID() {
        return -1;
    }

    @Override
    public String getBeanClassName() {
        return enterpriseBeanClassName;
    }

    /**
     * Get the unique jndi name of the home. For 2.x EJBs this will
     * always be the home name from the binding file. For 3.x EJBs it
     * may come from the binding file, or it may be the "simple binding name"
     * or it may be the "long" generated default binding, or it may be
     * "null" if no home exists.
     * We will return the Remote home value if it exists. If no remote
     * value is set then we will use the local value (which may also be
     * null).
     *
     * @return String - represents the unique jndi name of the home.
     */
    //d512996 d513978
    public String getJndiName() {
        String returnValue = simpleJndiBindingName;
        if ((returnValue == null) || (returnValue.equals(""))) {
            returnValue = ivRemoteHomeJndiName;
            if ((returnValue == null) || (returnValue.equals(""))) {
                returnValue = ivLocalHomeJndiName;
            }
        }
        return returnValue;
    }

    public int getEJBComponentType() {
        return type;
    }

    @Override
    public EJBType getEJBType() {
        return EJBType.forValue(type);
    }

    public EJBMethodInfoImpl createEJBMethodInfoImpl(int slotSize) {
        return new EJBMethodInfoImpl(slotSize);
    }

    /**
     * Returns an array of method information objects for all methods of the
     * specified interface type. The valid interface types are defined by {@link EJBMethodInterface}.
     *
     * Null will be returned if the EJB does not implement the specified
     * interface. And, a zero length array will be returned if the EJB
     * implements the specified interface, but the interface contains no
     * methods. <p>
     *
     * @param methodInterface interface type of methods to return
     * @return list of method information objects, or null
     **/
    // d162441
    @Override
    public List<EJBMethodMetaData> getEJBMethodMetaData(EJBMethodInterface methodInterface) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getEJBMethodMetaData: " + methodInterface);

        EJBMethodMetaData[] methodMetaDatas = null;

        if (methodInterface == EJBMethodInterface.REMOTE) {
            methodMetaDatas = methodInfos;
        } else if (methodInterface == EJBMethodInterface.HOME) {
            methodMetaDatas = homeMethodInfos;
        } else if (methodInterface == EJBMethodInterface.LOCAL) {
            if (type == InternalConstants.TYPE_MESSAGE_DRIVEN) {
                methodMetaDatas = null;
            } else {
                methodMetaDatas = localMethodInfos;
            }
        } else if (methodInterface == EJBMethodInterface.LOCAL_HOME) {
            methodMetaDatas = localHomeMethodInfos;
        } else if (methodInterface == EJBMethodInterface.SERVICE_ENDPOINT) {
            methodMetaDatas = wsEndpointMethodInfos; // LI2281.24
        } else if (methodInterface == EJBMethodInterface.MESSAGE_ENDPOINT) {
            if (type == InternalConstants.TYPE_MESSAGE_DRIVEN) {
                methodMetaDatas = localMethodInfos;
            } else {
                methodMetaDatas = null;
            }
        } else if (methodInterface == EJBMethodInterface.TIMER) {
            methodMetaDatas = timedMethodInfos; // LI2281.07
        } else if (methodInterface == EJBMethodInterface.LIFECYCLE_INTERCEPTOR) // F743-1751
        {
            methodMetaDatas = lifecycleInterceptorMethodInfos;
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "getEJBMethodMetaData: Invalid MethodInterface");
            throw new IllegalArgumentException("Invalid MethodInterface");
        }

        List<EJBMethodMetaData> result = methodMetaDatas == null ? null : Arrays.asList(methodMetaDatas);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getEJBMethodMetaData: " + result);

        return result;
    }

    @Override
    public boolean isReentrant() {
        return reentrant;
    }

    /**
     * Gets and returns CMP version.
     *
     * @return one of the following constants is returned:
     *         <p>
     *         <ul>
     *         <li>{@link InternalConstants#CMP_VERSION_1_X} <li>{@link InternalConstants#CMP_VERSION_2_X} <li>{@link InternalConstants#CMP_VERSION_UNKNOWN} </ul>
     */
    public int getCMPVersion() //129321
    {
        return cmpVersion;
    }

    /**
     * Gets and returns EJB module version as defined by one of the constants
     * org.eclipse.jst.j2ee.internal.J2EEVersionConstants interface.
     *
     * @return one of the following constants:
     *         <p>
     *         <ul>
     *         <li> J2EEVersionConstants.EJB_1_0_ID for a EJB 1.0 module.
     *         <li> J2EEVersionConstants.EJB_1_1_ID for a EJB 1.1 module.
     *         <li> J2EEVersionConstants.EJB_2_0_ID for a EJB 2.0 module.
     *         <li> J2EEVersionConstants.EJB_2_1_ID for a EJB 2.1 module.
     *         <li> J2EEVersionConstants.EJB_3_0_ID for a EJB 3.0 module.
     *         <li> J2EEVersionConstants.EJB_3_1_ID for a EJB 3.1 module.
     */
    //LI2110.41 - return new J2EEVersionConstants value.
    public int getEJBModuleVersion() //129321
    {
        return ivModuleVersion;
    }

    /**
     * Gets and returns application version as defined by one of the constants
     * org.eclipse.jst.j2ee.internal.J2EEVersionConstants interface.
     *
     * @return one of the following constants:
     *         <p>
     *         <ul>
     *         <li> J2EEVersionConstants.J2EE_1_2_ID for a J2EE 1.2 application.
     *         <li> J2EEVersionConstants.J2EE_1_3_ID for a J2EE 1.3 application.
     *         <li> J2EEVersionConstants.J2EE_1_4_ID for a J2EE 1.4 application.
     *         <li> J2EEVersionConstants.JAVAEE_5_ID for a JavaEE 5 application.
     *         <li> J2EEVersionConstants.JAVAEE_6_ID for a JavaEE 6 application.
     *         </ul>
     */
    //LI2110.41 - new method.
    public int getApplicationVersionId() {
        return ivApplicationVersionId;
    }

    /**
     * Method added for defect 133681
     *
     * This checks for some currently known invalid combinations (that we must guard against )
     * and throws exception.
     * Test for invalid combination
     * For EJB1.1 modules:
     * 1.a ActivateAt != ActivitySession.
     * Activity sessions are not supported in EJB1.1 module - hence the check
     * 1.b LTC Boundary != ActivitySession
     * Activity sessions are not supported in EJB1.1 module - hence the check.
     * 1.c LTC Resolver != Container at Boundary
     * Resolver can only be application - as was in case prior to 5.0 release.
     * Being enforced here. (But not for Entity Beans)
     *
     * For EJB 2.0 modules
     * 2.a For ejb2.0 entity beans and SFS's, if CMAS (ContainerManagedActivitySession),
     * then only supported ActivateAt policy is Activity Session & ReadOnly
     * (The bean must remain in cache, until AS completes).
     * 2.b if ActivateAt = AS, then loadAt must be Transaction
     *
     * 2.c if LTC resolution control is ContainerAtBoundary, then the Bean must be CMT or CMAS
     * Basically, a beanManagedTx(bmt) bean cannot have resolution control containerAtBoundary(cab).
     * i.e. bmt+app, cmt+cab and cmt+app is good. bmt+cab must throw exception.
     *
     * 2.d CMP entity beans must use resolution control of ContainerAtBoundary. The application does not control
     * database connections.
     *
     * 2.e SLSB's & MDB's do not have activation policy. Therefore their LTC may never be ActivitySession
     * because they cannot be pinned to an Activity session
     *
     * For EJBs implementing the javax.ejb.TimedObject interface: // LI2281.07
     * - must be an EJB 2.x or greater bean
     * - must not be a Stateful Session bean.
     * - ejbTimeout must be TX_REQUIRES_NEW, TX_REQUIRED, TX_NOT_SUPPORTED, or TX_BEAN_MANAGED
     *
     * LightweightLocal support has the following restrictions: // LI3795-56
     * - must be an Entity bean
     * - must be an EJB 2.0 bean (and CMP 2.x or later)
     * - must contain a local interface
     *
     * Pre-Load Pool / Max Creation Hard Limit: // PK20648
     * - must be a 2.0 Stateless Session bean.
     *
     * Option A caching not allowed with optimistic concurrency control
     *
     * Test configuration errors for beans with asynchronous methods (ie. new in EJB 3.1).
     *
     */
    public void validate() throws EJBConfigurationException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "validate " + j2eeName);

        boolean asAttributesDefined = false;

        if (ivModuleVersion <= J2EE_EJB_VERSION_1_1) {
            // it is an EJB Module version 1.1
            // 1.a
            if (ACTIVATION_POLICY_ACTIVITY_SESSION == activationPolicy) {
                Tr.error(tc, "INVALID_ACTIVITY_SESSION_POLICY_CNTR0069E", enterpriseBeanName);
                throw new EJBConfigurationException("CNTR0069E: Bean \"" + enterpriseBeanName +
                                                    "\" in an EJB 1.1 module attempted to use an invalid \"Activate at\" policy of \"Activity Session\".");

            }

            // 1.b
            if (LocalTransactionSettings.BOUNDARY_ACTIVITY_SESSION == _localTran.getBoundary()) {
                Tr.error(tc, "INVALID_ACTIVITY_SESSION_POLICY_CNTR0070E", enterpriseBeanName);
                throw new EJBConfigurationException("CNTR0070E: Bean \"" + enterpriseBeanName +
                                                    "\" in an EJB 1.1 module attempted to use an invalid Local Transactions Boundary of \"Activity Session\".");
            }
            // 1.c
            if (LocalTransactionSettings.RESOLVER_CONTAINER_AT_BOUNDARY == _localTran.getResolver() &&
                type != InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY) { //d138543
                Tr.error(tc, "INVALID_LOCAL_TRANSACTION_RESOLVER_CNTR0071E", enterpriseBeanName);
                throw new EJBConfigurationException("CNTR0071E: Bean \"" + enterpriseBeanName +
                                                    "\" in an EJB 1.1 module attempted to use an invalid Local Transactions Resolution control of \"ContainerAtBoundary\".");
            }

        }
        if (ivModuleVersion >= J2EE_EJB_VERSION_2_0) //LI2110.41
        {
            // EJB Module version 2.x
            // 2.a if ejb2.0 module + CMAS, then only supported ActivateAt policy
            //   is Activity Session or ReadOnly
            if (isEntityBean() || isStatefulSessionBean()) {
                // This is an Entity Bean or a Stateful Session bean
                if (!usesBeanManagedTx) {
                    String temp_method_name = null;

                    // for these CMAS/TX, check if method have AS Attributes defined
                    // Need to check all methods on all interfaces....        d174057
                    EJBMethodInterface[] mtypes = EJBMethodInterface.values();
                    for (int j = 0; j < mtypes.length && !asAttributesDefined; ++j) {
                        List<EJBMethodMetaData> emi = getEJBMethodMetaData(mtypes[j]);
                        int length = 0;
                        if (emi != null) // PK31372
                        {
                            length = emi.size() - 1;
                            for (int i = 0; i < length && !asAttributesDefined; ++i) {
                                ActivitySessionAttribute asa = ((EJBMethodInfoImpl) emi.get(i)).getActivitySessionAttribute();
                                switch (asa.getValue()) {
                                    case ActivitySessionAttribute.BEAN_MANAGED:
                                    case ActivitySessionAttribute.REQUIRED:
                                    case ActivitySessionAttribute.SUPPORTS:
                                    case ActivitySessionAttribute.REQUIRES_NEW:
                                    case ActivitySessionAttribute.MANDATORY:
                                        temp_method_name = emi.get(i).getMethodName();
                                        asAttributesDefined = true;
                                        // FIXME: Fallthrough makes this all dead code.
                                    default:
                                        asAttributesDefined = false;
                                }
                            } // end for (i) methodInfos
                        } // end if
                    } // end for (j) methodInterfaces

                    if (asAttributesDefined &&
                        ACTIVATION_POLICY_ACTIVITY_SESSION != activationPolicy &&
                        !ivReadOnlyCommitOption) // LI3408
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Exception thrown for method " + temp_method_name);
                        Tr.error(tc, "INVALID_ACTIVITY_SESSION_POLICY_CNTR0072E",
                                 enterpriseBeanClassName);
                        throw new EJBConfigurationException("Invalid activateAt policy with Container Managed " +
                                                            "Activity Session for = " + enterpriseBeanClassName);
                    }
                }
            }
            /*
             * //2.b if ActivateAt = AS, then loadAt must be Transaction
             * Already taken care of in method setActivationLoadPolicy. Throws CNTR0044W exception
             */
            //2.c
            if (usesBeanManagedTx) { //flags usesBeanManagedTx and usesBeanManagedAS are same. They are set if bean is BMT
                                     //It is a BMT
                                     //LTC must NOT be ContainerAtBoundary
                if (_localTran.getResolver() == LocalTransactionSettings.RESOLVER_CONTAINER_AT_BOUNDARY) {
                    Tr.error(tc, "INVALID_LOCAL_TRANSACTION_RESOLVER_CNTR0073E", j2eeName);
                    throw new EJBConfigurationException("Unsupported Local Transaction resolution control for BeanManaged .");
                }
            }
        } //end ejb 2.0 module if block
          // Check for invalid local transaction configuration.
          // First, check for invalid resolver configuration
          // and then check for invalid boundary.
          // 130016 and 132312 - start
        if (type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY) { //2.d
            if (_localTran.getResolver() == LocalTransactionSettings.RESOLVER_APPLICATION) {
                Tr.error(tc, "INVALID_CONFIGURATION_CMP_RESOLVER_APPLICATION_CNTR0065E", j2eeName);
                throw new EJBConfigurationException("Unsupported Local Transaction resolution control for CMP Beans.");
            }
        } else if (/* type == STATELESS_SESSION || */type == InternalConstants.TYPE_MESSAGE_DRIVEN) { //2.e//d180809
            if (_localTran.getBoundary() == LocalTransactionSettings.BOUNDARY_ACTIVITY_SESSION) {
                Tr.error(tc, "LOCAL_TRAN_BOUNDARY_ACTIVITY_INVALID_CNTR0066E", j2eeName);
                throw new EJBConfigurationException("Local Transaction Boundary of activity session is invalid for Message Driven Beans.");
            }
        }
        //check for invalid resolver configuration 130016 and 132312- stop

        TransactionAttribute txAttr;

        // -----------------------------------------------------------------------
        // Perform EJB Specification validation for MessageDriven beans. LI2281.07
        // -----------------------------------------------------------------------
        if (type == InternalConstants.TYPE_MESSAGE_DRIVEN) {
            // MDBs may only implement some Tx Atributes...
            for (int i = 0; i < localMethodInfos.length; i++) // LIDB2110.41
            {
                txAttr = localMethodInfos[i].getTransactionAttribute();
                if (!(txAttr == TransactionAttribute.TX_REQUIRED ||
                      txAttr == TransactionAttribute.TX_NOT_SUPPORTED || txAttr == TransactionAttribute.TX_BEAN_MANAGED)) {
                    String method = localMethodInfos[i].getMethodName();
                    Tr.error(tc, "INVALID_TX_ATTR_CNTR0089E", // d170741
                             new Object[] { txAttr.toString(), method, j2eeName });
                    throw new EJBConfigurationException("Transaction attribute " +
                                                        txAttr.toString() + " is not allowed " +
                                                        "for method " + method + " on EJB " +
                                                        j2eeName);
                }
            }
        }

        // -----------------------------------------------------------------------
        // Perform EJB Specification validation for beans that implement the
        // javax.ejb.TimedObject interface.                              LI2281.07
        // -----------------------------------------------------------------------
        if (timedMethodInfos != null) // F743-506
        {
            // Per the EJB Spec, if this bean implements the javax.ejb.TimedObject
            // interface, verify that it is a EJB 2.0 or later bean, and is not
            // a Stateful Session bean.
            if (ivModuleVersion < J2EE_EJB_VERSION_2_0 ||
                cmpVersion == InternalConstants.CMP_VERSION_1_X ||
                type == InternalConstants.TYPE_STATEFUL_SESSION ||
                type == InternalConstants.TYPE_MANAGED_BEAN) {
                Tr.error(tc, "INVALID_TIMEDOBJECT_IMPL_CNTR0088E", j2eeName); // d170741
                throw new EJBConfigurationException("EJB 1.x, 2.0, and Stateful Session " +
                                                    "beans may not implement the " +
                                                    "javax.ejb.TimedObject interface : " +
                                                    j2eeName);
            }

            // F743-506 - Validate all timer methods
            for (int i = 0; i < timedMethodInfos.length; i++) {
                // Per the Spec. TimedObject.ejbTimeout may only implement some Tx Atributes...
                // Note:  In the 2.X specifications TX_REQUIRED was not specified as a valid
                // transaction attribute for TimedObjects.   This did not make much sense and
                // was changed in the 3.0 specification to allow it.   We will no longer
                // consider this to be an error situation (even for 2.X beans).  //d366845.8
                txAttr = timedMethodInfos[i].getTransactionAttribute();
                if (!(txAttr == TransactionAttribute.TX_REQUIRED ||
                      txAttr == TransactionAttribute.TX_REQUIRES_NEW ||
                      txAttr == TransactionAttribute.TX_NOT_SUPPORTED || txAttr == TransactionAttribute.TX_BEAN_MANAGED)) {
                    String method = timedMethodInfos[i].getMethodName();
                    Tr.error(tc, "INVALID_TX_ATTR_CNTR0089E", // d170741
                             new Object[] { txAttr.toString(), method, j2eeName });
                    throw new EJBConfigurationException("Transaction attribute " +
                                                        txAttr.toString() + " is not allowed " +
                                                        "for method " + method + " on EJB " +
                                                        j2eeName);
                }
            }
        }

        if (lifecycleInterceptorMethodInfos != null) // F743-1751
        {
            for (EJBMethodInfoImpl methodInfo : lifecycleInterceptorMethodInfos) {
                txAttr = methodInfo.getTransactionAttribute();
                // REQUIRED must have already been translated to REQUIRES_NEW.
                if (txAttr != TransactionAttribute.TX_REQUIRES_NEW &&
                    txAttr != TransactionAttribute.TX_NOT_SUPPORTED &&
                    txAttr != TransactionAttribute.TX_BEAN_MANAGED) {
                    String method = methodInfo.getMethodName();
                    Tr.error(tc, "INVALID_TX_ATTR_CNTR0089E",
                             new Object[] { txAttr.toString(), method, j2eeName });
                    throw new EJBConfigurationException("Transaction attribute " +
                                                        txAttr.toString() + " is not allowed " +
                                                        "for method " + method + " on EJB " +
                                                        j2eeName);
                }
            }
        }

        // ReadOnly / Interval caching option beans must be CMP 2.x or later,
        // since that is the only way to insure the state of the bean is not
        // changed/updated.                                                 LI3408
        if (ivCacheReloadType != CACHE_RELOAD_NONE) {
            if (cmpVersion < InternalConstants.CMP_VERSION_2_X) {
                Tr.error(tc, "INVALID_CACHE_RELOAD_POLICY_CNTR0094E",
                         enterpriseBeanClassName);

                StringBuffer exText = new StringBuffer();
                exText.append("Bean ").append(j2eeName).append(" with LoadPolicy ");
                exText.append(getCacheReloadIntervalString());
                exText.append(" must be CMP Version 2.x or later.");

                throw new EJBConfigurationException(exText.toString());
            }
        }

        // -----------------------------------------------------------------------
        // Perform validation for beans that implement the LightweightLocal
        // interface.                                                    LI3795-56
        // -----------------------------------------------------------------------
        if (isLightweight) {

            // -------------------------------------------------------------------------
            // A special check for temporary WPS hack, allowing SLSBs to be Lightweight.
            // As a performance fix for WPS, SLSBs may be Lightweight.  This is only
            // enabled by an undocumented EJB environment variable.  It cannot be
            // enabled by a WCCM config switch, nor by inheriting the Lightweight
            // interface.  The latter is flagged as an error here.  The former can
            // not happen since we do not support this in the WCCM model for SLSBs.
            //                                                                   PK21246
            // -------------------------------------------------------------------------
            if (type == InternalConstants.TYPE_STATELESS_SESSION &&
                (LightweightLocal.class).isAssignableFrom(enterpriseBeanAbstractClass)) {
                Tr.error(tc, "INVALID_LIGHTWEIGHT_IMPL_CNTR0119E",
                         new Object[] { j2eeName, "1" });
                throw new EJBConfigurationException("Only Entity EJBs may implement " +
                                                    "the LightweightLocal interface : " +
                                                    j2eeName);
            }

            // LightweightLocal support has the following restrictions:
            //  - must be an Entity bean
            //  - must be an EJB 2.0 bean (and CMP 2.x or later)
            //  - must contain a local interface
            //  - might be a WPS hacked SLSB (ie. see above check)   PK21246
            else if (type != InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY &&
                     type != InternalConstants.TYPE_BEAN_MANAGED_ENTITY &&
                     type != InternalConstants.TYPE_STATELESS_SESSION) {
                Tr.error(tc, "INVALID_LIGHTWEIGHT_IMPL_CNTR0119E",
                         new Object[] { j2eeName, "1" });
                throw new EJBConfigurationException("Only Entity EJBs may implement " +
                                                    "the LightweightLocal interface : " +
                                                    j2eeName);
            }

            if (ivModuleVersion < J2EE_EJB_VERSION_2_0 ||
                cmpVersion == InternalConstants.CMP_VERSION_1_X) {
                Tr.error(tc, "INVALID_LIGHTWEIGHT_IMPL_CNTR0119E",
                         new Object[] { j2eeName, "2" });
                throw new EJBConfigurationException("EJB 1.x and CMP 1.x beans may not " +
                                                    "implement the LightweightLocal " +
                                                    "interface : " + j2eeName);
            }

            if (localHomeInterfaceClassName == null) {
                Tr.error(tc, "INVALID_LIGHTWEIGHT_IMPL_CNTR0119E",
                         new Object[] { j2eeName, "3" });
                throw new EJBConfigurationException("EJBs that implement the " +
                                                    "LightweightLocal interface must " +
                                                    "define a local interface : " + j2eeName);
            }
        }

        // -----------------------------------------------------------------------
        // Pre-Load Pool / Max Creation Hard Limit                      // PK20648
        // - must be a 2.0 Stateless Session bean.
        // -----------------------------------------------------------------------
        if (ivInitialPoolSize > 0 &&
            (type != InternalConstants.TYPE_STATELESS_SESSION ||
             ivModuleVersion <= J2EE_EJB_VERSION_1_1)) {
            Tr.warning(tc, "INVALID_MIN_POOLSIZE_CNTR0057W", new Object[] { j2eeName.toString(), "H" + Integer.toString(minPoolSize) });
            throw new EJBConfigurationException("A hard (H) minimum pool size may only be " +
                                                "specified for EJB 2.x Stateless Session " +
                                                "EJBs : " + j2eeName);
        }
        if (ivMaxCreation > 0 &&
            (type != InternalConstants.TYPE_STATELESS_SESSION ||
             ivModuleVersion <= J2EE_EJB_VERSION_1_1)) {
            Tr.warning(tc, "INVALID_MAX_POOLSIZE_CNTR0058W", new Object[] { j2eeName.toString(), "H" + Integer.toString(maxPoolSize) });
            throw new EJBConfigurationException("A hard (H) maximum pool size may only be " +
                                                "specified for EJB 2.x Stateless Session " +
                                                "EJBs : " + j2eeName);
        }

        // -----------------------------------------------------------------------
        // Option A caching and Optimistic Concurrency Control not allowed
        //                                                                116394.3
        // -----------------------------------------------------------------------
        if (optionACommitOption && optimisticConcurrencyControl) {
            Tr.warning(tc, "COMMIT_OPTION_A_AND_OPTIMISTIC_CONCURRENCY_CONTROL_NOT_SUPPORTED_CNTR0049E");
            throw new EJBConfigurationException("Using Commit Option A with Optimistic Concurrency is not supported.  EJB = " + enterpriseBeanClassName);
        }

        // -----------------------------------------------------------------------------------
        // Test configuration errors for beans with asynchronous methods (ie. new in EJB 3.1).                                                                 116394.3
        // -----------------------------------------------------------------------------------

        if (ivHasAsynchMethod) {

            // Asynch methods are only allowed on session beans
            if (!isSessionBean()) {
                Tr.error(tc, "INVALID_BEAN_TYPE_FOR_ASYNCH_METHOD_CNTR0185E",
                         new Object[] { j2eeName.getComponent(), j2eeName.getModule(), j2eeName.getApplication() });

                throw new EJBConfigurationException("The " + j2eeName.getComponent() + " bean in the " + j2eeName.getModule() + " module of the " +
                                                    j2eeName.getApplication() + " application has one or more asynchronous methods configured, but is not a session bean.  " +
                                                    "Asynchronous methods can only be configured on session beans.");
            }

        } // Validation of asynchronous methods

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "validate");
    }

    private String toIntrospectString() {
        // Get the New Line separator from the Java system property.
        String separator = ContainerProperties.LineSeparator; // 391302
        StringBuilder sb = new StringBuilder(toString());
        String sep = "                                 ";
        sb.append(separator + sep + "*** START ComponentMetaData fields ***");
        if (ivUnversionedJ2eeName != null)
            sb.append(separator + sep + "Unversioned    = " + ivUnversionedJ2eeName);

        if (type == InternalConstants.TYPE_SINGLETON_SESSION) // F743-508
            sb.append(separator + sep + "EJB Type       = SINGLETON_SESSION");
        else if (type == InternalConstants.TYPE_STATELESS_SESSION)
            sb.append(separator + sep + "EJB Type       = STATELESS_SESSION");
        else if (type == InternalConstants.TYPE_STATEFUL_SESSION)
            sb.append(separator + sep + "EJB Type       = STATEFUL_SESSION");
        else if (type == InternalConstants.TYPE_BEAN_MANAGED_ENTITY)
            sb.append(separator + sep + "EJB Type       = BEAN_MANAGED_ENTITY");
        else if (type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY)
            sb.append(separator + sep + "EJB Type       = CONTAINER_MANAGED_ENTITY");
        else if (type == InternalConstants.TYPE_MESSAGE_DRIVEN)
            sb.append(separator + sep + "EJB Type       = MESSAGE_DRIVEN"); // 126512
        else if (type == InternalConstants.TYPE_MANAGED_BEAN)
            sb.append(separator + sep + "EJB Type       = MANAGED_BEAN"); // F743-34310
        else
            sb.append(separator + sep + "EJB Type       = UNKNOWN");

        if (usesBeanManagedTx)
            sb.append(separator + sep + "TX Type        = BEAN_MANAGED_TX");
        else
            sb.append(separator + sep + "TX Type        = CONTAINER_MANAGED_TX");

        sb.append(separator + sep + "Module Version = " + ivModuleVersion);

        if (type == InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY) {
            if (cmpVersion == InternalConstants.CMP_VERSION_1_X)
                sb.append(separator + sep + "CMP Version    = 1.x");
            else if (cmpVersion == InternalConstants.CMP_VERSION_2_X)
                sb.append(separator + sep + "CMP Version    = 2.x");
            else
                sb.append(separator + sep + "CMP Version    = UNKNOWN");
        }

        if (null != _resourceRefList)
            sb.append(_resourceRefList);
        if (null != _localTran)
            sb.append(_localTran);

        sb.append(separator + sep + "*** END ComponentMetaData fields  ***");
        return sb.toString();
    }

    // d112604.1

    public String[][] getMethodLevelCustomFinderMethodSignatures(String cfprocessstring) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getMethodLevelCustomFinderMethodSignatures:" + cfprocessstring);

        StringTokenizer st = new StringTokenizer(cfprocessstring, ":");

        // array form [item#][CFMethodName]=methodname, e.g. findByCustomerName
        // array form [item#][CFMethodParms]]=parameter list, e.g. int,java.lang.String

        String cfMethodSignatureArray[][] = new String[st.countTokens()][2];
        int methodCounter = 0;

        while (st.hasMoreTokens()) {
            String methodSignature = st.nextToken();

            StringTokenizer methodSignaturest = new StringTokenizer(methodSignature, "()");

            // format exepected, first attribute method name (can't be nul)
            //                   second are the signatures parms, in order

            try {
                cfMethodSignatureArray[methodCounter][CF_METHOD_NAME_OFFSET] = methodSignaturest.nextToken();

                if (methodSignature.equals("()")) {
                    cfMethodSignatureArray[methodCounter][CF_METHOD_SIG_OFFSET] = null; // no signature given, but followed expected format
                } else {
                    cfMethodSignatureArray[methodCounter][CF_METHOD_SIG_OFFSET] = methodSignaturest.nextToken();
                }
            } catch (java.util.NoSuchElementException ex) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()
                    && methodSignature != null)
                    Tr.debug(tc, "Processing offset [" + methodCounter + "] " + methodSignature + " failed, incorrect format");
                // badSignatureGiven=true;
            }

            methodCounter++;
        } // while

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getMethodLevelCustomFinderMethodSignatures: " + cfMethodSignatureArray);

        return cfMethodSignatureArray;
    }

    public boolean cfMethodSignatureEqual(String homeMethodName,
                                          String homeMethodSignature,
                                          String[][] cfMethodSignatures) {
        for (int lcv = 0; lcv < cfMethodSignatures.length; lcv++) {
            // check if method name is the same...
            if ((cfMethodSignatures[lcv][CF_METHOD_NAME_OFFSET] != null) && homeMethodName.equals(cfMethodSignatures[lcv][CF_METHOD_NAME_OFFSET])) {
                // method name matches, now have to verify signature as well...
                if ((homeMethodSignature == null) && (cfMethodSignatures[lcv][CF_METHOD_SIG_OFFSET] == null)) {
                    return true; // two method with same name, no parms, thus equal
                }
                if ((homeMethodSignature == null) || (cfMethodSignatures[lcv][CF_METHOD_SIG_OFFSET] == null)) {
                    return false; // two method with same name, one's is not null (passed first check) and one is (passed this check) thus not equal
                }
                // know both not null, and now need to string check
                if (homeMethodSignature.equals(cfMethodSignatures[lcv][CF_METHOD_SIG_OFFSET])) {
                    return true;
                } // if method signature parameter lists match
            } // if method names match
        } // for run through all methods asked to enable via customer in cf property setting
        return false;
    }

    // d112604.1

    // LI2775-107.2 Begins WS18354.02A
    public boolean isApplicationSyncToOSThreadEnabled() {
        return m_syncToOSThreadValue;
    }

    // LI2775-107.2 Ends

    /**
     * Returns <tt>true</tt> if more rigid checking for EJB application
     * configuration problems should be performed and should result in
     * additional warnings and/or errors. <p>.
     *
     * This method returns an application custom property.
     */
    // F743-33178
    public boolean isCheckConfig() {
        return _moduleMetaData.getEJBApplicationMetaData().isCheckConfig();
    }

    /**
     * Returns true if this stateful session EJB can be passivated.
     */
    public boolean isPassivationCapable() {
        return passivationCapable &&
        // Passivation of SFSB with extended persistence context is problematic. d465813
               !ivHasAppManagedPersistenceContext &&
               !ivHasCMExtendedPersistenceContext;
    }

    /**
     * Returns enterprise bean constructor
     * ivEnterpriseBeanClassConstructor for default no-arg constructor
     * ivEnterpriseBeanFactory.getConstructor() for non default
     *
     * @throws ManagedObjectException
     */
    public Constructor<?> getEnterpriseBeanClassConstructor() {
        Constructor<?> constructor = ivEnterpriseBeanClassConstructor;
        if (constructor == null) {
            constructor = ivEnterpriseBeanFactory.getConstructor();
        }
        return constructor;
    }

} // BeanMetaData
