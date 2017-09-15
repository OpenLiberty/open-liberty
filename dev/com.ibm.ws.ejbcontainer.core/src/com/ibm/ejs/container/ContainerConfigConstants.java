/*******************************************************************************
 * Copyright (c) 2002, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import com.ibm.ejs.container.util.DeploymentUtil;
import com.ibm.wsspi.ejbcontainer.JITDeploy;

/**
 * Holds the configuration and property constants for the WebSphere Container.
 **/
public class ContainerConfigConstants {
    /**
     * Property used to specify the min. and max. pool sizes for the EJB Container.<p>
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.poolSize<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B> <p>
     * "Min. pool size"
     * "Max. pool size"<p>
     * <B>Description:</B> <p>
     * This property is used when a customer does not wish to use the default min. and max. pool sizes.<p>
     *
     * @see BeanMetaData
     **/

    public static final String poolSizeSpecProp = "com.ibm.websphere.ejbcontainer.poolSize";

    /**
     * Property used to allow the customer to define the default behavior for all FindByPrimaryKey methods to be READ-ONLY.<p>
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.FbpkAlwaysReadOnly<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "true"<p>
     * "false" (default)<p>
     * <B>Description:</B>
     * If the user specifies this property as "true" then the default behavior for all FindByPrimaryKey methods
     * will be READ-ONLY. If "false" is specified then the default behavior will be UPDATE.<p>
     *
     * @see BeanMetaData
     **/

    public static final String fbpkReadOnlyProp = "com.ibm.websphere.ejbcontainer.FbpkAlwaysReadOnly";

    /**
     * This property allows the user to force the container to use Option A caching in a workload managed environment.<p>
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.wlmAllowOptionAReadOnly<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Description:</B> <p>
     * In WebSphere 4.0X, Option A caching strategy is illegal when utilizing workload management (WLM). The default
     * action the container takes in this situation is to automatically switch to Option C caching strategy. This
     * property allows the user to force the container to accept Option A caching with WLM. By setting this property
     * to true the user is taking the responsibility to ensure that they will only access the datastore in a READ-ONLY
     * fashion.
     *
     * @see BeanMetaData
     **/

    public static final String wlmAllowOptionAReadOnlyProp = "com.ibm.websphere.ejbcontainer.wlmAllowOptionAReadOnly";

    /**
     * This property allows the user to indicate whether to use the classes found in
     * ejbportable.jar or to use the legacy classes (the ones that existed prior to release 5.
     * <p>
     * <B>Value:</B> com.ibm.websphere.container.portable
     * <p>
     * <B>Usage:</B> Container Property
     * <p>
     * <B>Description:</B>
     * <p>
     * In WebSphere 5.0X, in order to obtain Java EE 1.3 compliance, changes were made in the
     * classes that implement the EJB Handle, HomeHandle, and EJBMetaData interfaces. These
     * classes are required to be usable in another vendor's container implementation. Thus,
     * some of the changes broke interoperability with prior releases of websphere. Similar
     * changes where made to other classes in the area of classes used as return values in
     * EJB architected interfaces. For example, in the area of java.util.Collection and
     * java.util.Enumeration that are returned by finder methods. These classes that must
     * be used in a non-websphere container were all placed in a ejbportable.jar file that
     * can be made available to SUN reference implementation of a client or web container.
     * By doing so, interoperability testing that CTS does between SUN RI container and a
     * websphere container is obtained. This jar can also be made available to other vendor's
     * container that are Java EE 1.3 compliant.
     * <p>
     * To regain interoperability with websphere 3.5.6 and 4.0x, the ejbportable.jar classes
     * are made available to the prior releases in the form of a PTF. Since not every server
     * in the network can be forced to upgrade to the same level at the same time, it became
     * necessary to add this property. By setting this property to false, it will for the
     * 5.0 server to use the old implementation of these classes. Since we are certified
     * as Java EE 1.3 compliant, by default this property is set to true so that the new portable
     * classes found in ejbportable.jar is used (which allows CTS to pass).
     * <p>
     * Until all websphere client are upgraded to have the ejbportable.jar, a 5.0 server must
     * have this property set to false. Once all clients are updated, the property can be
     * changed to true or simply defaulted to true. Until that point is reached, a non
     * websphere client can not share a 5.0 server with a websphere client.
     * <p>
     *
     * The com.ibm.websphere.container.portable.finder flag allows 4.0 collections/enumerations
     * to be returned and 5.0 Handles and Metadata.
     * <p>
     **/
    public static final String portableProp = "com.ibm.websphere.container.portable"; //p125891
    public static final String portableFinderProp = "com.ibm.websphere.container.portable.finder"; //p125891

    /**
     * This property allows the user to allow the mutation of the PrimaryKeyObject. <p>
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.allowPrimaryKeyMutation<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "true"<p>
     * "false" (default)<p>
     * <B>Description:</B>
     * <p>
     * When the client is running with the system property "noLocalCopies" set to true, if the
     * PrimaryKey object is mutated there is a high potential for data integrity problem. So whenever the
     * customer want to mutate the PrimaryKey object they will need to set this property to "true"
     *
     * @see EJSHome
     **/
    //d138865

    public static final String allowPrimaryKeyMutation = "com.ibm.websphere.ejbcontainer.allowPrimaryKeyMutation";

    /**
     * This property allows the user to specify that their code will not do any mutation of the PrimaryKeyObject. <p>
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.noPrimaryKeyMutation<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "true"<p>
     * "false" (default)<p>
     * <B>Description:</B>
     * <p>
     * When the client is using local interfaces, WebSphere assumes that the PrimaryKey object
     * may be mutated and therefore a "deep copy" of the object must be made. However, if the customer
     * can guarantee that their code does not mutate the PrimaryKey object, then they can
     * set this property to "true" and receive slightly better performance.
     *
     * @see EJSHome
     **/
    //d138865.1

    public static final String noPrimaryKeyMutation = "com.ibm.websphere.ejbcontainer.noPrimaryKeyMutation";

    // d112604.1 begin

    /**
     * This JVM property allows the user to have the EJB Container dynamically extend the SQL and semantics for customer finders to support FOR UPDATE requirements. (d112604.1) <p>
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.customfinder.honorAccessIntent<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "all"<p>
     * J2EENAME[:J2EENAME]...(default)<p>
     * <B>Description:</B>
     * <p>
     * When the application server is running with the JVM system property:
     *
     * "com.ibm.websphere.ejbcontainer.customfinder.honorAccessIntent"
     *
     * set to all, any home interface CMP 1.1 custom
     * finder (prefix named "find") that has "update" as an access intent will change behavior based upon its access intent.
     * If the backend datastore requires a special semantic, it will be applied. The particular SQL used varies by isolation
     * level the user has chosen in the application as well the database being used.
     *
     * The default is to disregard the access intent set in the deployment description extension file for
     * CMP 1.1 custom finders.
     *
     * For those customers who wish to ensure data integrity, they should have their finders
     * denoted with the "Update" intent in r/w situations. The following option allows customers to specify certain beans
     * in which this important, and leave others run in the default manner. An example value:
     *
     * com.ibm.websphere.com.Inc:com.ibm.websphere.com.Inc1
     *
     * In this case, two beans, Inc and Inc1 have been configured to have their custom finder
     * SQL dynamically enhanced depending on the update attribute, the isolation level and
     * finally the backend the beans interact with.
     *
     * Another example:
     *
     * all
     *
     * Enable for all beans in this container.
     *
     * If this attribute is not set, the container defaults to not supporting the Custom Finders
     * Access Intent as in previous version of the release. Defaulting this to on can cause
     * for applications to act incorrectly since the SQL passed to the connection for processing
     * will be mal-formed.
     *
     * Note, method level access intent is available below, and will enable honoring custom finder
     * methods per method. If "com.ibm.websphere.ejbcontainer.customfinder.honorAccessIntent"
     * is set to all, it will overwrite the per method level enabling property. If this
     * attribute enables an entire Bean, all methods will honor the access intent. If this
     * value is not set, then the only way to honor the access intent for customer finders
     * is using the per method support below.
     *
     **/
    //d138865

    public static final String allowCustomFinderSQLForUpdate = "com.ibm.websphere.ejbcontainer.customfinder.honorAccessIntent";

    /**
     * This property allows the user disable at the bean level the EJB Container finder methods FOR UPDATE support. This is a compatiablity option for customers that have deployed
     * on WAS 390 (d112604.1) <p>
     * <B>Value:</B>
     * com.ibm.websphere.persistence.bean.managed.custom.finder.access.intent<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "true"<p>
     * <B>Description:</B>
     * <p>
     *
     * This is a compatibility preference honored at the bean level. If enabled, and the server
     * has custom finder FOR UPDATE semantic enabled, this will disable that support for this bean.
     *
     * Value of "true" disables this support, any other value is equated to false.
     *
     * The per method level attribute com.ibm.websphere.ejbcontainer.customfinder.honorAccessIntent.methodLevelallow below,
     * overrides this at the method level. The
     *
     * This attribute is not supported at the server level, since the default for the Application Server
     * is off already, and customers must set an attribute to turn on, which is opposite of the 390 4.0x implementation.
     * This value does override the com.ibm.websphere.ejbcontainer.customfinder.honorAccessIntent setting
     * for a specific bean, this was preserved to ensure initial bean implementation honored.
     *
     **/

    public static final String allowWAS390CustomFinderAtBeanLevelStr = "com.ibm.websphere.persistence.bean.managed.custom.finder.access.intent";

    /**
     * This bean (module) property allows the user to have the EJB Container dynamically extend the SQL and semantics for 1 or more customer finder methods to support FOR UPDATE
     * requirements. (d112604.1) <p>
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.customfinder.honorAccessIntent.methodLevel<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "method1(parm1,parm2,..parmn):method2(parm1,parm2,..parmn):methodn(...)"<p>
     * <B>Description:</B>
     * <p>
     * When the application server is running with this resource environment property set to a list of 1 or more custom finder methods, any
     * home interface CMP 1.1 custom finder (prefix named "find") that has a matching method name and parameter signature
     * with a Read/Write access intent will result in the FOR UPDATE semantic being applied as required dynamically.
     * This occurs only if the backend datastore supports this function. The particular SQL used varies by isolation level the user has
     * chosen in the application as well the database being used.
     *
     * The default is to disregard the access intent set in the deployment description extension file for
     * CMP 1.1 custom finders when not enumerated here or enabled globally at the server or bean level.
     *
     * This attribute overrides the 390 per bean disabling preference.
     *
     **/
    //d138865

    public static final String envCustomFinderMethods = "com.ibm.websphere.ejbcontainer.customfinder.honorAccessIntent.methodLevel";

    // d112604.1 end

    /**
     * This property allows the user to allow not defer the initialization of any EJBs. <p>
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.initializeEJBsAtStartup<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "true"<p>
     * "false" (default)<p>
     * <B>Description:</B>
     * <p>
     * The default behavior following the inclusion of LIDB859-4 is to defer the initialization
     * of all EJBs. This property will allow the user to globally disable this function,
     * forcing all EJBs to be initialized at module start time.
     **/
    //LIDB859-4

    public static final String initializeEJBsAtStartup = "com.ibm.websphere.ejbcontainer.initializeEJBsAtStartup";

    /**
     * This property allows the user to specify which Stateful Session passivation
     * policy should be used on zOS. <p>
     *
     * The default passivation policy is ON_CAHCE_FULL, which indicates Stateful
     * Session beans should only be passivated when the EJB Cache becomes full. <p>
     *
     * <B>Value:</B> com.ibm.websphere.csi.passivationpolicy <p>
     *
     * <B>Usage:</B> Optional Container Property <p>
     *
     * <B>Property values:</B>
     * <ul>
     * <li> AT_COMMIT
     * <li> ON_DEMAND
     * <li> ON_CACHE_FULL (default)
     * </ul>
     **/
    // 391302
    public static final String passivationPolicy = "com.ibm.websphere.csi.passivationpolicy";

    public static final String syncToOSThreadSetting = "com.ibm.websphere.security.SyncToOSThread"; // LI2775-107.2

    /**
     * This env-var setting allows the user to disable flush-before-find for
     * the applicable EJB.<p>
     * <B>Value:</B>
     * com/ibm/websphere/ejbcontainer/disableFlushBeforeFind<p>
     * <B>Usage:</B>
     * EJB env-var setting<p>
     * <B>Usage:</B>
     * Optional EJB env-var setting<p>
     * <B>Property values:</B>
     * java.lang.Boolean true<p>
     * java.lang.Boolean false (default)<p>
     * <B>Description:</B>
     * <p>
     * The default behavior prescribed by the EJB 2.0 spec is to synchronize all
     * beans involved in the current transaction with persistent storage, prior
     * to executing a custom finder method. This setting overrides that behavior
     * and disables the synchronization (flush) operation for the EJB on which
     * the env-var setting is defined.
     *
     * @see BeanMetaData
     **/
    // d203853
    public static final String disableFlushBeforeFind = "com/ibm/websphere/ejbcontainer/disableFlushBeforeFind";

    /**
     * This env-var setting allows the user to disable ejbStore for the
     * applicable EJB.<p>
     * <B>Value:</B>
     * com/ibm/websphere/ejbcontainer/disableEJBStoreForNonDirtyBeans<p>
     * <B>Usage:</B>
     * EJB env-var setting<p>
     * <B>Usage:</B>
     * Optional EJB env-var setting<p>
     * <B>Property values:</B>
     * java.lang.Boolean true<p>
     * java.lang.Boolean false (default)<p>
     * <B>Description:</B>
     * <p>
     * The default behavior prescribed by the EJB 2.x spec is to always invoke
     * the ejbStore() method on all beans enlisted in a transaction, when the
     * transaction commits. This setting overrides that behavior and will
     * cause the EJB container to skip the ejbStore call if the
     * PersistenceManager indicates that the bean has not been dirtied during
     * the current transaction.
     *
     * @see BeanMetaData
     **/
    // d237506
    public static final String disableEJBStoreForNonDirtyBeans = "com/ibm/websphere/ejbcontainer/disableEJBStoreForNonDirtyBeans";

    //PQ89520
    /**
     * This property allows the user to force the container to insert record
     * during ejbCreate.<p>
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.allowEarlyInsert<p>
     * <B>Usage:</B>
     * Optional container property<p>
     * <B>Property values:</B>
     * true | false (default) <p>
     * <B>Description:</B> <p>
     * For CMP1.1 beans, the default behavior is to create/insert the
     * database record until after completion of ejbPostCreate.
     * If the database insert is required at ejbCreate, set this system
     * property to true. <p>
     **/
    public static final String allowEarlyInsert = "com.ibm.websphere.ejbcontainer.allowEarlyInsert";

    /**
     * This env-var setting allows the user to disable the EJB pool.<p>
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.noEJBPool<p>
     * <B>Usage:</B>
     * EJB env-var setting<p>
     * <B>Usage:</B>
     * Optional EJB env-var setting<p>
     * <B>Property values:</B>
     * java.lang.Boolean true<p>
     * java.lang.Boolean false (default)<p>
     * <B>Description:</B>
     * <p>
     * The default behavior is to always have at least 1 entry in the EJB pool.
     * However there are certain circumstances when it would be useful to
     * operate without an EJB pool (during development only). For example,
     * operating without an EJB pool will force a new bean instances to be
     * created whenever one is needed. This can help a developer ensure that
     * they are initializing all of their ejb fields appropriately and not
     * relying on "stale" data contained in pooled instances. This can also be
     * useful in debugging of "data corruption" issues, helping to ensure that
     * the data corruption isn't occurring because of fields in "pooled"
     * instances that are not being appropriately initialized by the
     * application.
     *
     * @see EJSHome
     **/
    // d262413
    public static final String noEJBPool = "com.ibm.websphere.ejbcontainer.noEJBPool";

    /**
     * This property allows the user to include a 'root' exception when their Tx
     * is rolledback.<p>
     *
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.includeRootExceptionOnRollback<p>
     *
     * <B>Usage:</B>
     * Optional JVM System Property<p>
     *
     * <B>Property values:</B>
     * true|false|<integer><p>
     *
     * <B>Description:</B><p>
     *
     * For a remote EJB request which causes a Transaction (Tx) to be created, a
     * TransactionRolledbackException (TRE) will be returned to the client if the bean
     * method causes the Tx to be rolledback. When a TRE is returned, the ORB will
     * strip off any nested exception(s) included within it, thus the user is not
     * able to take action based on the root cause. This is not always a desirable
     * outcome for users. Setting this property to "true" will allow a generic RemoteException
     * to be returned from the EJB Container, rather than a TRE, when the method causes
     * the Tx to be rolledback. This will allow nested exceptions to be returned to the
     * client as the ORB will not strip off nested exception contained in a RemoteException.
     * This property is not enabled by default because the client may not have on their
     * classpath all (nested) exceptions returned by the server.<p>
     *
     * This property has been updated via APAR PK87857 to include more scenarios in which
     * to include nested exceptions on a Tx rollback. However, for 'backwards' compatibility
     * we must not allow 'includeRootExceptionOnRollback=true' to include the new code added
     * via APAR PK87857. As such, this property will be allowed to include more values (options).
     * Specifically, this value can now take an integer. The integer values are defined
     * to the following options:<p>
     *
     * 0 - Disables this property, same as setting 'false' (default).<p>
     * 1 - Enables the prior (pre-PK87857) version of this property/code, same as setting 'true'.
     * 2 - Enables the code of PK87857, that is, includes more scenarios in which a RemoteException
     * is returned on a rollback. This value will also have the effect of allowing Heuristic
     * Exceptions to be returned rather than a TransactionRolledbackLocalException (for a
     * local client) or TRE (for a remote client).
     * <p>This value also includes the same code/function enabled via value 1.<p>
     * 4 - Allows a RemoteException to be returned to the Tx beginner when a down stream component
     * causes the Tx to be rolledback. This is a spec violation, that is, the 'Exception Handling'
     * chapter of the EJB Spec states that when a method executes in the context of the caller's
     * tx, and that method throws an unhandled exception, the container must throw a
     * TRE. As explained above, the ORB will strip off any exceptions nested in the TRE. This
     * property allows a RemoteException to be returned to the caller, however, this is a spec
     * violation. That is, take this example: EJB 1, method m1, begins a Tx and calls
     * EJB 2, method m2, where m2 causes an unhandled exception. In this case, the spec mandates that
     * m1 would receive a TRE. With this value, a RemoteException will be returned instead which
     * will include any nested exception(s).
     * <p>This value does nothing on its own, that is, it must be applied with value 1 or 2 to be
     * useful.<p>
     *
     * To enabled a combination of these options, one must sum the integer for the options of
     * interest. For example, to enable option 1 and 4 (1+4=5), one would set the property as follows:<p>
     * 'includeRootExceptionOnRollback=5'<p>
     * As another example, to enable all options (2+4=6, or 1+2+4=7, but recall that value 2 includes 1), one
     * would set the property as follows:<p>
     * 'includeRootExceptionOnRollback=6'<p>
     * <p>
     *
     * @see RemoteExceptionMappingStrategy
     **/
    public static final String includeNestedExceptions = "com.ibm.websphere.ejbcontainer.includeRootExceptionOnRollback";// d253963 PK87857

    /**
     * This env-var setting allows the user to indicate that a Stateless
     * Session bean should run in a mode that allows the EJB container to
     * streamline the pre- and post-method work.
     *
     * This is a temporary performance hack for WPS only. This env-var
     * will not be documented for use by customers.
     * PK21246
     **/
    public static final String lightweightLocal = "com/ibm/websphere/ejbcontainer/lightweightWPSSLSB";

    /**
     * This system property allows for expansion of a CMP Connection Factory JNDI name. <p>
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.expandCMPCFJNDIName<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "true"<p>
     * "false" (default)<p>
     * <B>Description:</B>
     * <p>
     * This system property allows the user to have the value for their CMP Connection Factory JNDI name (which is set in
     * the XML bindings file) expanded. That is, if the user uses a WebSphere variable as part of the CMP CF JNDI name,
     * it will only be expanded by setting this flag, otherwise the value is used literally.
     **/
    //d425164

    public static final String expandCMPCFJNDIName = "com.ibm.websphere.ejbcontainer.expandCMPCFJNDIName";

    /**
     * This property allows the user to explicitly disable bindings of simple default (a name consisting only
     * of the fully qualifed interface name....com.ibm.MyCart) jndiNames.
     * . <p>
     *
     * <B>Value:</B> com.ibm.websphere.ejbcontainer.disableShortDefaultBindings <p>
     *
     * <B>Usage:</B> Optional Container Property <p>
     *
     * <B>Property values:</B>
     * appName1:appName2:appName3...or * for all applications <p>
     **/
    public static final String disableShortDefaultBindings = "com.ibm.websphere.ejbcontainer.disableShortDefaultBindings"; // d444470

    /**
     * Property that allows the user to specify application names
     * that they would like to have the 2.x EJBs have
     * the same SetRollbackOnly behavior as 3.x EJBs.
     * Beginning with EJB 3.0 (CTS) it is required that the
     * bean method return normally regardless of how the tx was
     * marked for rollback, so don't throw rollback exception, instead
     * return the application exception or return normally if no application
     * exception was caught. <p>
     *
     * <B>Value:</B> com.ibm.websphere.ejbcontainer.extendSetRollbackOnlyBehaviorBeyondInstanceFor <p>
     *
     * <B>Usage:</B> Optional Container Property <p>
     *
     * <B>Property values:</B>
     * appName1:appName2:appName3.... or '*' which will apply this property to every app.<p> //PK76079
     **/
    //d461917.1
    public static final String extendSetRollbackOnlyBehaviorBeyondInstanceFor = "com.ibm.websphere.ejbcontainer.extendSetRollbackOnlyBehaviorBeyondInstanceFor";

    /**
     * Property that allows the user to specify application names
     * that they would like to have the 3.x EJBs have
     * the same SetRollbackOnly behavior as 2.x EJBs.
     * Beginning with EJB 3.0 (CTS) it is required that the
     * bean method return normally regardless of how the tx was
     * marked for rollback, so don't throw rollback exception, instead
     * return the application exception or return normally if no application
     * exception was caught. <p>
     *
     * <B>Value:</B> com.ibm.websphere.ejbcontainer.limitSetRollbackOnlyBehaviorToInstanceFor <p>
     *
     * <B>Usage:</B> Optional Container Property <p>
     *
     * <B>Property values:</B>
     * appName1:appName2:appName3...<p>
     **/
    //d461917.1
    public static final String limitSetRollbackOnlyBehaviorToInstanceFor = "com.ibm.websphere.ejbcontainer.limitSetRollbackOnlyBehaviorToInstanceFor";

    /**
     * Internal property that allows the user to disable EJB timers.
     * <B>Value:</B>
     * com.ibm.ws.ejbcontainer.disableTimers<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "true"<p>
     * "false" (default)<p>
     * <B>Description:</B>
     * <p>
     * If this property is set to true, then beans with timeout methods or
     * automatic timers will fail with an error during initialization.
     */
    public static final String disableTimers = "com.ibm.ws.ejbcontainer.disableTimers"; // F743-13022

    /**
     * Internal property that allows the user to disable EJB persistent timers.
     * <B>Value:</B>
     * com.ibm.ws.ejbcontainer.disablePersistentTimers<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "true"<p>
     * "false" (default)<p>
     * <B>Description:</B>
     * <p>
     * If this property is set to true, then the EJB Timer Service Scheduler will not be created which
     * disables persistent timer creation, retrieval, and timeout
     */
    public static final String disablePersistentTimers = "com.ibm.websphere.ejbcontainer.disablePersistentTimers";

    /**
     * Internal property that allows the user to disable asynchronous methods.
     * <B>Value:</B>
     * com.ibm.ws.ejbcontainer.disableAsyncMethods<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "true"<p>
     * "false" (default)<p>
     * <B>Description:</B>
     * <p>
     * If this property is set to true, then beans with asynchronous methods will
     * fail with an error during initialization.
     */
    public static final String disableAsyncMethods = "com.ibm.ws.ejbcontainer.disableAsyncMethods"; // F743-13022

    /**
     * Internal property that allows the user to disable MDBs.
     * <B>Value:</B>
     * com.ibm.ws.ejbcontainer.disableMDBs<p>
     * <B>Usage:</B>
     * Container Property<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "true"<p>
     * "false" (default)<p>
     * <B>Description:</B>
     * <p>
     * If this property is set to true, then MDBs will fail with an error during
     * application start.
     */
    public static final String disableMDBs = "com.ibm.ws.ejbcontainer.disableMDBs"; // F743-13023

    /**
     * Property that allows the user to configure the container to validate
     * and report any known problems in the EJB module.
     */
    public static final String checkAppConfigProp = "com.ibm.websphere.ejbcontainer.checkEJBApplicationConfiguration"; //F743-13921

    /**
     * Property that allows the user to specify EJB name(s) of EJB Timers which they would like to
     * allow potentially stale Timer data, that is, to use cached Timer data. If you look at the
     * javax.ejb.Timer interface, the following methods can be configured (using this property
     * and the integer value to the left of the method) to return cached data:
     * <UL><LI>
     * 1 - getHandle()
     * </LI><LI>
     * 2 - getInfo()
     * </LI><LI>
     * 4 - getNextTimeout()
     * </LI><LI>
     * 8 - getTimeRemaining()
     * </LI></UL>
     * 16 - getSchedule()
     * </LI></UL>
     * 32 - isPersistent()
     * </LI></UL>
     * 64 - isCalendarTimer()
     *
     * The user can apply this property to any individual methods listed above, using the assigned
     * integer value, or they can apply it to a combination of these methods. For example, to apply
     * this property to the 'getHandle' method and 'getTimeRemaining', the user would sum the integer
     * value for the two methods: 9 (1+8). Or to apply the property to the first four methods, the
     * user would use the value 15 (1+2+4+8).<p>
     *
     * <B>Name:</B>com.ibm.websphere.ejbcontainer.allowCachedTimerDataFor<p>
     *
     * <B>Usage:</B> Optional Container Property <p>
     *
     * <B>Property values:</B>
     * com.ibm.websphere.ejbcontainer.allowCachedTimerDataFor=*[=<integer>]|j2eename[=<integer>][:j2eename[=<integer>]]...<p>
     * When '*' is used, this property will be applied to all installed applications.<p>
     *
     * Lets look at some examples. For the examples assume we have two apps, each with a Timer, with
     * the following J2EENames: App1#EJBJar1.jar#EJBTimer1 and App2#EJBJar2.jar#EJBTimer2 <p>
     *
     * Example 1: to apply this property to the 'getInfo' method on EJBTimer1 and to 'getNextTimeout'
     * and 'getTimeRemaining on EJBTimer2, we would use the following:<p>
     * com.ibm.websphere.ejbcontainer.allowCachedTimerDataFor=App1#EJBJar1.jar#EJBTimer1=2:App2#EJBJar2.jar#EJBTimer2=12<p>
     *
     * Example 2: to apply this property to all methods on all Timers on all apps, we would use the following:<p>
     * com.ibm.websphere.ejbcontainer.allowCachedTimerDataFor=*=127, or simply<p>
     * com.ibm.websphere.ejbcontainer.allowCachedTimerDataFor=*<p>
     *
     * Example 3: to apply this property to 'getInfo' on all Timer on all apps, we would use the following:<p>
     * com.ibm.websphere.ejbcontainer.allowCachedTimerDataFor=*=2<p>
     *
     * Example 4: to apply this property to 'getHandle' and 'getNextTimeout' on EJBTimer2, we would use
     * the following:<p>
     * com.ibm.websphere.ejbcontainer.allowCachedTimerDataFor=App2#EJBJar2.jar#EJBTimer2=5<p>
     *
     **/
    //F001419
    public static final String allowCachedTimerDataFor = "com.ibm.websphere.ejbcontainer.allowCachedTimerDataFor"; //F001419

    public static final int ALLOW_CACHED_TIMER_GET_HANDLE = 1;
    public static final int ALLOW_CACHED_TIMER_GET_INFO = 2;
    public static final int ALLOW_CACHED_TIMER_GET_NEXT_TIMEOUT = 4;
    public static final int ALLOW_CACHED_TIMER_GET_TIME_REMAINING = 8;
    public static final int ALLOW_CACHED_TIMER_GET_SCHEDULE = 16;
    public static final int ALLOW_CACHED_TIMER_IS_PERSISTENT = 32;
    public static final int ALLOW_CACHED_TIMER_IS_CALENDAR_TIMER = 64;

    /**
     * The operations that require TimerTaskHandler.
     */
    public static final int ALLOW_CACHED_TIMER_OPS_USING_TASK_HANDLER = ALLOW_CACHED_TIMER_GET_INFO |
                                                                        ALLOW_CACHED_TIMER_GET_SCHEDULE |
                                                                        ALLOW_CACHED_TIMER_IS_CALENDAR_TIMER;

    /**
     * Internal property that allows the user to disable remote interfaces.
     * <B>Value:</B>
     * com.ibm.ws.ejbcontainer.disableRemote<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * "true"<p>
     * "false" (default)<p>
     * <B>Description:</B>
     * <p>
     * If this property is set to true, then beans with remote interfaces will
     * fail with an error during application start.
     */
    public static final String disableRemote = "com.ibm.ws.ejbcontainer.disableRemote"; // F743-13024

    /**
     * Property that allows the user to specify (server wide) the stateful session bean timeout value,
     * The value is specified in minutes.
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.defaultStatefulSessionTimeout<p>
     * <B>Usage:</B>
     * Optional Container Property<p>
     * <B>Property values:</B>
     * <integer><p>
     * 0 - Disables this property, use default value (10 minutes).<p>
     * all other valid integer value - Enables this property, setting the timeout value to specified minutes.
     * <B>Description:</B>
     * <p>
     * If this property is set the stateful session bean timeout is used server wide instead of the
     * default value of 10 minutes. At individual stateful session bean level the timeout can be
     * overridden using @StatefulTimeout annotation on the bean class or using the stateful-timeout
     * deployment descriptor element.
     */
    public static final String defaultStatefulSessionTimeout = "com.ibm.websphere.ejbcontainer.defaultStatefulSessionTimeout"; // F743-14726  d627044

    /**
     * Property that allows the user to specify that incoming EJB requests should
     * be blocked until an application is fully started as specified by the {@link #blockWorkUntilAppStartedWaitTime} property. This behavior is
     * required by the EJB specification for applications with startup
     * singletons, but for compatibility, this behavior is not the default for
     * other applications.
     * <p><b>Property values:</b>
     * true or false (default false)
     */
    public static final String blockWorkUntilAppStarted = "com.ibm.websphere.ejbcontainer.blockWorkUntilAppStarted"; // F743-15941

    /**
     * Property that allows the user to specify how long external requests should
     * be blocked while an application is starting. If the application does not
     * start in the specified duration, then requests will be rejected with
     * ApplicationNotStartedException. The value is specified in seconds. If
     * the value is 0, then external requests will be immediately rejected until
     * the application is fully started.
     * <p><b>Property values:</b>
     * any non-negative integer value (default 120)
     */
    public static final String blockWorkUntilAppStartedWaitTime = "com.ibm.websphere.ejbcontainer.blockWorkUntilAppStartedWaitTime"; // F743-15941

    /**
     * Property that allows the user to specify that the container should behave
     * as described by the EE5 specifications rather than later specifications.
     * Setting this property to true indicates:
     * 1. Application exceptions are not inherited
     * 2. SFSB concurrent access results in an exception rather than blocking
     * <p><b>Property values:</b>
     * true or false (default false)
     */
    public static final String ee5Compatibility = "com.ibm.websphere.ejbcontainer.EE5Compatibility"; // F743-14982CdRv

    /**
     * Property that allows the user to specify to use fair locking policy for singleton beans.
     * Setting this property to true causes the threads contend for entry using an
     * approximately arrival-order policy.
     * <p><b>Property values:</b>
     * true or false (default false)
     */
    public static final String useFairSingletonLockingPolicy = "com.ibm.websphere.ejbcontainer.useFairSingletonLockingPolicy"; // F743-9002

    /**
     * Property that allows the user to specify that unchecked exceptions
     * declared on the throws clause should not be treated as application
     * exceptions, per the EJB specifications. Historically, the EJB container
     * has incorrectly treated unchecked exceptions on the throws clause as
     * application exceptions.
     * <p><b>Property values:</b> true or false (default true)
     */
    public static final String declaredUncheckedAreSystemExceptions = DeploymentUtil.declaredUncheckedAreSystemExceptions; // d660332

    /**
     * Deployed application or system property that allows the user to specify
     * ether or not EJBs in this application should be bound to the server root.
     * <p><b>Property values:</b> true or false (default true)
     */
    public static final String bindToServerRoot = "com.ibm.websphere.ejbcontainer.bindToServerRoot"; // F743-33812

    /**
     * Deployed application or system property that allows the user to specify
     * whether or not EJBs in this application should be bound to "java:"
     * namespaces as required by the EJB 3.1 specification.
     * <p><b>Property values:</b> true or false (default true)
     */
    public static final String bindToJavaGlobal = "com.ibm.websphere.ejbcontainer.bindToJavaGlobal"; // F743-33812

    /**
     * EJB Container system property that allows the user to exclude the 'root'
     * exception when a transaction is rolled back during commit processing. <p>
     *
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.excludeRootExceptionOnRollback<p>
     *
     * <B>Usage:</B>
     * Optional JVM System Property<p>
     *
     * <B>Property values:</B>
     * true|false<p>
     *
     * <B>Description:</B><p>
     *
     *
     * In WebSphere 8.0, the container was enhanced to provide the cause
     * exception of a transaction failure during commit, by setting it on
     * the returned transaction rollback exception. This property allows the
     * container to be configured to provide the old behavior, and could be
     * useful for applications that are not expecting to find a cause, or
     * applications that log the exception and are now seeing significantly
     * more log information. <p>
     */
    // d672063
    public static final String excludeNestedExceptions = "com.ibm.websphere.ejbcontainer.excludeRootExceptionOnRollback";

    /**
     * Internal EJB Container system property that allows the user to turn on
     * validation of the merged XML produced by WAS.metadatamgr. <p>
     *
     * When enabled, the output of the internal EJB Container merge operation is
     * compared to the output of the merge produced by WAS.metadatamgr and any
     * mismatches are reported. <p>
     *
     * <b>Property values:</b>
     * <ul>
     * <li> log - when a mismatch is found, an exception is logged, allowing
     * the bean to start and function normally.
     * <li> fail - when a mismatch is found, an exception is thrown, resulting
     * in a failure to start the bean.
     * </ul>
     */
    // d680497
    public static final String validateMergedXML = "com.ibm.websphere.ejbcontainer.validateMergedXML";

    /**
     * The maximum number of Future objects that will be kept by the container
     * for finished asynchronous method invocations that are waiting for the
     * caller to acquire results. After the limit is reached, each subsequent
     * completed asynchronous method invocation will evict the oldest Future
     * object. If the value is less than or equal to 0, no limit will be
     * enforced. <p>
     *
     * <b>Property values:</b>
     * any integer value (default 1000)
     */
    public static final String maxUnclaimedAsyncResults = "com.ibm.websphere.ejbcontainer.maxUnclaimedAsyncResults"; // d690014.1

    /**
     * The maximum time, in seconds, for the Timer.cancel method to retry the
     * cancel operation when the time is concurrently running.
     *
     * <b>Property values:</b>
     * any integer value (default 60)
     */
    // d703086
    public static final String timerCancelTimeout = "com.ibm.websphere.ejbcontainer.timerCancelTimeout";

    /**
     * Property that allows the user to specify (server wide) the default session concurrency
     * access timeout value. The value is specified in milliseconds. <p>
     *
     * <B>Value:</B>
     * com.ibm.websphere.ejbcontainer.defaultSessionAccessTimeout<p>
     *
     * <B>Usage:</B>
     * Optional Container Property<p>
     *
     * <B>Property values:</B> <p>
     * <ul>
     * <li> 0 - Disables session concurrency; no wait time.
     * <li> all other positive long values - sets the timeout value to specified milliseconds.
     * </ul>
     *
     * <B>Description:</B> <p>
     * If this property is set, the specified session bean concurrency access timeout is used
     * server wide instead of the default value of -1 (wait forever). This applies to both
     * stateful and singleton session beans. At the individual session bean level, the timeout
     * can be overridden using the @AccessTimeout annotation on the bean class or method or
     * using the access-timeout deployment descriptor element. <p>
     */
    // d704504
    public static final String defaultSessionAccessTimeout = "com.ibm.websphere.ejbcontainer.defaultSessionAccessTimeout";

    /**
     * Property that allows the user to specify the level of rmic compatibility
     * for JIT-generated stubs and ties. <p>
     *
     * <b>Property values:</b>
     * <ul>
     * <li> "none" (default) - disables all compatibility options
     * <li> "values" - detect interfaces that should be written using
     * write_value rather than unconditionally using writeAbstractObject;
     * requires SerializableStub and SerializedStub in ejbportable.jar
     * <li> "exceptions" - mangle exception names (PM94096)
     * <li> "" or "all" - full compatibility; this option enables all
     * currently known compatibility options, which means its meaning might
     * change with time, so it is not forwards compatible
     * </ul>
     */
    public static final String rmicCompatible = JITDeploy.rmicCompatible; // PM46698

    /**
     * Property that allows the user to revert the way EJB stubs are generated
     * for EJB 3.x API beans to exhibit earlier behavior where a RemoteException
     * may be thrown even though RemoteException is not on the throws clause. <p>
     *
     * <b>Property values:</b>
     * <ul>
     * <li> "false" (default) - RemoteException will not be thrown if
     * the method signature does not contain 'throws RemoteException'.
     * <li> "true" - RemoteException may be thrown by the remote method
     * even though RemoteException is not on the throws clause.
     * </ul>
     */
    public static final String throwRemoteFromEjb3Stub = JITDeploy.throwRemoteFromEjb3Stub;

    /**
     * Property that allows the user to disable retries for remote Future.get.
     * When disabled, client calls to Future.get will not abort due to
     * NO_RESPONSE simply because the timeout is too large. Additionally,
     * when disabling, server threads will actively wait for responses rather
     * than being returned to the thread pool.
     *
     * <b>Property values</b>
     * true or false (default)
     */
    public static final String disableAsyncResultRetries = ClientAsyncResult.disableAsyncResultRetries; // F16043

    /**
     * Property that allows the user to specify the maximum number of seconds the
     * server should wait for an async result to become available before
     * responding to the client request indicating a retry. By default, the
     * server will attempt to respond before the client read times out, but if
     * the client ORB read timeout has been configured lower than the default,
     * the retry response will fail silently on the server and the client will
     * silently retry.
     *
     * <b>Property values:</b>
     * any integer value (default 150)
     *
     * @see #asyncResultNoResponseBackoff
     */
    public static final String maxAsyncResultWaitTime = "com.ibm.websphere.ejbcontainer.maxAsyncResultWaitTime"; // F16043

    /**
     * Property that allows the user to specify the number of seconds to "back
     * off" after receiving a NO_RESPONSE from the server from a Future.get after
     * a remote asynchronous call. Typically, the server will respond to the
     * client request indicating a retry, but if the client ORB read timeout has
     * been configured lower than the default, the client read will timeout
     * before the server can send its retry response. In that case, the client
     * will ignore the NO_RESPONSE and retry the request with a wait time equal
     * to the actual elapsed time minus the backoff time.
     *
     * <b>Property values:</b>
     * any integer value (default 30)
     *
     * @see #maxAsyncResultWaitTime
     */
    public static final String asyncResultNoResponseBackoff = ClientAsyncResult.asyncResultNoResponseBackoff; // F16043

    /**
     * Property that allows the user to specify that duplicate binding elements
     * should be ignored rather than merged together. When true, this is
     * compatible with pre-EJB3.
     *
     * <p><b>Property values:</b> true or false (default false)
     */
    public final static String ignoreDuplicateEJBBindings = "com.ibm.websphere.ejbcontainer.ignoreDuplicateEJBBindings"; // PM51230

    /**
     * Property that allows the user to specify that indirect proxies should be
     * used for local client views. A direct proxy (wrapper) is bound to the
     * lifecycle of the application containing the local EJB. An indirect local
     * proxy works like a remote stub and allows access to the local EJB even if
     * the application is restarted.
     *
     * <p><b>Property values:</b> true or false (default false)
     */
    public static final String indirectLocalProxies = "com.ibm.websphere.ejbcontainer.indirectLocalProxies"; // F58064

    /**
     * Property that allows the user to specify that the container should not
     * automatically enable lightweight mode for trivial method implementations.
     * This might be necessary if the user is performing bytecode manipulation on
     * bean methods (javaagent, JVMTI, etc.), and the inserted bytecodes rely on
     * method contexts.
     *
     * <p><b>Property values:</b> true or false (default false)
     */
    public static final String disableAutomaticLightweightMethods = "com.ibm.websphere.ejbcontainer.disableAutomaticLightweightMethods"; // F61004.1

    /**
     * Property that allows the user to specify the container should behave as
     * described by the EE6 specifications rather than later specifications.
     * Setting this property enables all of:
     * 1. com.ibm.websphere.ejbcontainer.emptyAnnotationIgnoresExplicitInterfaces=true
     * <p><b>Property values:</b>
     * true or false (default false)
     */
    public static final String ee6Compatibility = "com.ibm.websphere.ejbcontainer.EE6Compatibility";

    /**
     * Property that allows the user to specify whether or not the container
     * should ignore explicitly configured local/remote business interfaces and
     * no-interface view when determining whether or not an empty Local/Remote
     * annotation should cause a single interface on the implements clause to be
     * considered a business interface. This was the default behavior prior to
     * our implementation of EJB 3.2, which clarified the behavior of empty
     * Local/Remote annotations.
     *
     * <p><b>Property values:</b> true or false (default false)
     */
    public static final String emptyAnnotationIgnoresExplicitInterfaces = "com.ibm.websphere.ejbcontainer.emptyAnnotationIgnoresExplicitInterfaces";

    /**
     * Property that allows the user to specify that subclasses of RemoteException
     * specified on the throws clause should be treated as application exceptions,
     * contrary to the EJB specifications.
     * <p><b>Property values:</b> true or false (default false)
     */
    public static final String declaredRemoteAreApplicationExceptions = DeploymentUtil.declaredRemoteAreApplicationExceptions;

    /**
     * Property that allows the user to specify that scheduler QOS_ATLEASTONCE
     * setting should be used for timer callback methods with a Required
     * transaction attribute, which is the internal default if no transaction
     * attribute is specified. The EJB specification requires QOS_ONLYONCE
     * semantics in this scenario, and this was the behavior in WAS 6.1.
     * <p><b>Property values:</b> true or false (default false)
     */
    public static final String timerQOSAtLeastOnceForRequired = "com.ibm.websphere.ejbcontainer.timerQOSAtLeastOnceForRequired";

    /**
     * Property that allows the user to specify the deadlock timeout for persistent
     * timers associated with singleton beans. Since singleton beans require a lock
     * to run any method, including the timeout callback, a deadlock may occur between
     * the timer database and the singleton method lock if a method that is running
     * on the singleton bean calls TimerService.getTimres() while a timeout callback
     * is also trying to run.
     * <p><b>Property values:</b>
     * <ul>
     * <li> -1 : deadlock detection disabled
     * <li>a positive integer value (default is 10000) : time in milliseconds for
     * the timer to wait on the singleton bean lock before checking for deadlock.
     * </ul>
     */
    public static final String persistentTimerSingletonDeadlockTimeout = "com.ibm.websphere.ejbcontainer.persistentTimerSingletonDeadlockTimeout";

    /**
     * Property that allows the user to specify that the max EJB cache should be
     * strictly enforced rather than only increasing the frequency of sweeps and
     * or reducing the number of sweeps an element can remain. This property is
     * primarily intended to be used by tests.
     * <p><b>Property values:</b> true or false (default false)
     */
    public static final String strictMaxCacheSize = "com.ibm.websphere.ejbcontainer.strictMaxCacheSize";

    /**
     * Property that allows the user to prevent a bean class instance from being
     * created at EJB start. This was traditionally done for diagnostic purposes
     * to ensure that bean class instances can be created when for an EJB request,
     * but it also causes bean class static initializer and constructor logic to
     * run, which can cause application logic to run while sensitive EJB container
     * locks are held. It cannot be disabled by default because some customers
     * might be relying on the static initializer or constructor to run.
     *
     * <p><b>Property values:</b> true or false (default true)
     */
    public static final String createInstanceAtStartup = "com.ibm.websphere.ejbcontainer.createInstanceAtStart"; // PI23717
}
