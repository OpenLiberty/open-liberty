/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.csi;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.ApplicationException;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.ContainerProperties;
import com.ibm.ejs.util.dopriv.SystemGetPropertyPrivileged;
import com.ibm.websphere.cpmi.PMModuleCookie;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.JCDIHelper;
import com.ibm.ws.ejbcontainer.runtime.EJBApplicationEventListener;
import com.ibm.ws.javaee.dd.ejb.Interceptor;
import com.ibm.ws.metadata.ejb.AutomaticTimerBean;
import com.ibm.ws.metadata.ejb.EJBInterceptorBinding;
import com.ibm.ws.metadata.ejb.InterceptorMethodKind;
import com.ibm.ws.metadata.ejb.ModuleInitData;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataImpl;
import com.ibm.wsspi.ejbcontainer.JITDeploy;

/**
 * <code>EJBModuleMetaDataImpl<code> is the EJB Container's internal implementation
 * format of one module's (ie. EJB jar's) runtime configuration metadata. A single
 * application may have multiple EJB jars, and therefore require multiple module
 * metadata implementation objects.
 */
public class EJBModuleMetaDataImpl extends MetaDataImpl
                implements com.ibm.websphere.csi.EJBModuleMetaData
{
    private static final TraceComponent tc = Tr.register(EJBModuleMetaDataImpl.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * The data used to initialize the module. This field will be null as soon
     * as module start begins.
     */
    public ModuleInitData ivInitData; // F743-36113

    /**
     * Name of this module.
     *
     * This is the physical name of the module, not the logical name.
     *
     * This variable is required by applicationProfile.
     */
    public String ivName;

    // F743-25385
    /**
     * This is the logical name of the module.
     *
     * The logical name of the module may be explicitly specified by the user
     * via the <module-name> element in the ejb-jar.xml or web.xml deployment
     * descriptor file.
     *
     * If a logical module name is not explicitly specified by the user, then
     * this value will be the name of the physical module, minus the extension
     * (eg, for a physical name of A.jar, the logical name would just be A).
     *
     * This is needed because the name of the physical module file is likely
     * different than the logical name of the module, and in this case we need
     * to know both the logical and physical names in order to support the
     * various ejb-link syntaxes.
     */
    public String ivLogicalName;

    /**
     * Name of Application that contains this module
     */
    public String ivAppName;

    /**
     * Java EE Name of this module (ie. AppName + ModuleName + null ComponentName).
     * This variable is required by applicationProfile
     */
    public J2EEName ivJ2EEName;

    /**
     * Version info for this module.
     */
    public int ivModuleVersion;

    /**
     * The container's metadata for the application containing this module.
     */
    private final EJBApplicationMetaData ivEJBApplicationMetaData;

    /**
     * True if SFSB failover is enabled for this module.
     */
    public boolean ivSfsbFailover;

    /**
     * Failover instance id for SFSB failover, or null if ivSfsbFailover is false.
     */
    public String ivFailoverInstanceId;

    /**
     * The cookie returned from PersistenceManager.modulePreInstall. The
     * module cookie is used by the Persistence Manager to track installation
     * and uninstallation of EJB modules in order to maintain correct state in
     * its cache.
     */
    public PMModuleCookie ivPMModuleCookie; // F743-21131

    /**
     * Flag to indicate if configuration metadata for this module only exists
     * in XML (ie. no annotations)
     *
     */
    public boolean ivMetadataComplete = false;

    /**
     * Set to true to indicate that we need to read from WCCM the
     * interceptor meta data for this module. Reset to false once
     * the interceptor meta data is read. This ensures we only process
     * the interceptor meta data from ejb-jar.xml once per module.
     */
    public boolean ivReadWCCMInterceptors = true; // d367572.9

    /**
     * ApplicationExceptionMap. An entry is created in this map for each
     * WCCM object found for this module that represents an
     * application-exception deployment descriptor. The fully qualified
     * class name of the exception class specified by application-exception
     * deployment descriptor is the key for the map. Note, this map only
     * contains the application-exception DD's and it does not contain
     * exception classes that are annotated by using the @AnnotationException
     * annotation. This is done to avoid having to load every single class
     * to determine if it is annotated as an exception class. Instead, that
     * check is done by {@link #getApplicationException}.
     */
    public Map<String, ApplicationException> ivApplicationExceptionMap; // d395666, F743-15982

    /**
     * A map of the <interceptors></interceptors> meta data the is read from
     * the ejb-jar.xml file for this module. For example:
     * <pre>
     * <interceptors id="interceptors_1">
     * <interceptor id="interceptor_1">
     * <interceptor-class>suite.r70.base.ejb3.intx.CLInterceptor1</interceptor-class>
     * <around-invoke>
     * <method-name>superAroundInvoke</method-name>
     * </around-invoke>
     * </interceptor>
     * <interceptor id="interceptor_2">
     * <interceptor-class>suite.r70.base.ejb3.intx.CLInterceptor2</interceptor-class>
     * <post-construct>
     * <method-name>aroundInvoke</method-name>
     * </post-construct>
     * </interceptor>
     * </interceptors>
     * </pre>
     * The map key is the loaded Class object of the interceptor class
     * as identified by the <interceptor-class> stanza. The map value is a
     * EnumMap< InterceptorMethodKind, List<Method>> > where InterceptorMethodKind
     * is the enum that indicates the kind of interceptor method (around-invoke,
     * post-construct, post-activate, pre-passivate, or pre-destroy) that is the
     * List of Method objects. The Method object is the java reflection Method object
     * for the interceptor method. In above example, there would be an around invoke
     * Method object for the "superAroundInvoke" method for the Map entry for
     * interceptor class CLInterceptor1 and a post-construct Method object for the
     * "postConstruct" method in the Map entry for interceptor class CLInterceptor2.
     * A list is needed since each interceptor class is allowed to have multiple
     * interceptor methods, one per class in the class inheritance tree.
     * <p>
     * Each List<Method> created in the EnumMap is a LIFO list so that the
     * first Method out of the list is a method of the most generic superclass of
     * an interceptor class and the last out is a method of the interceptor class
     * itself. This is done so that the methods are invoked in the order required
     * by the EJB specification.
     */
    public IdentityHashMap<Class<?>, EnumMap<InterceptorMethodKind, List<Method>>> ivInterceptorsMap; // d367572.9

    /**
     * A map of the <interceptor-binding></interceptor-binding> meta data the is read from
     * the ejb-jar.xml file for this module. For example:
     *
     * <pre>
     * <assembly-descriptor id="AssemblyDescriptor_1">
     * <interceptor-binding>
     * <ejb-name>AdvStatefulLocalBean</ejb-name>
     * <interceptor-class>suite.r70.base.ejb3.intx.CLInterceptor1</interceptor-class>
     * <interceptor-class>suite.r70.base.ejb3.intx.CLInterceptor2</interceptor-class>
     * </interceptor-binding>
     * </assembly-descriptor>
     * </pre>
     *
     * The map key is the EJBName of the EJB the binding is for or * if the
     * binding applies to all EJBs of this module (e.g. to provide default interceptors
     * for the module). The map value is a List of EJBInterceptorBinding object created
     * to hold the metadata obtained from WCCM objects. For default interceptor binding,
     * there should only be 1 entry in the List since you can only have 1 default
     * interceptor binding per module.
     */
    public Map<String, List<EJBInterceptorBinding>> ivInterceptorBindingMap; // d367572.9

    /**
     * Current backend ID.
     */
    public String ivCurrentBackendId = null;

    /**
     * Default connection factory for CMP entity beans.
     */
    public String ivDefaultCmpConnectionFactory = null; // F743-36290.1

    /**
     * Reference to default (ie. module level) datasource binding name.
     */
    public String ivDefaultDataSource = null; // F743-36290.1
    public String ivDefaultDataSourceUser = null; // F743-36290.1
    public String ivDefaultDataSourcePassword = null; // F743-36290.1

    /**
     * A map where the key is the fully qualified class name of an
     * interceptor class and the value is the WCCM Interceptor class.
     */
    public Map<String, Interceptor> ivInterceptorMap; // d468919

    /**
     * Set to true if and only if a MBean is registered for this module.
     * We must register a MBean for the module before we can register a
     * MBean for any EJB in the module. Therefore, if this flag is false,
     * then none of the EJBs in the module have a MBean registered for it.
     */
    public boolean ivMBeanRegistered = false; //d458588

    public final Map<String, String> ivMessageDestinationBindingMap = new HashMap<String, String>(); //d465081

    /**
     * Set to true when the fireMetaDataCreated method is called for
     * this EJBModuleMetaData object so that we know whether to call the
     * fireMetaDataDestroyed when the EJBModuleMetaData is destroyed.
     */
    public boolean ivMetaDataDestroyRequired = false; // d505055

    /**
     * Set to true when the Module will use the SetRollbackOnly behavior
     * that was implemented for EJB 3.0 modules.
     * Beginning with EJB 3.0 (CTS), it is required that
     * bean methods return normally regardless of how the tx was
     * marked for rollback, so don't throw rollback exception. Instead
     * return the application exception or return normally if no application
     * exception was caught. This value will be set to "true" in the
     * following circumstances:
     *
     * . The EJB module is 3.0 or greater and the
     * com.ibm.websphere.ejbcontainer.limitSetRollbackOnlyBehaviorToInstanceFor
     * property does not contain this application name.
     *
     * . The EJB module is 2.x and the
     * com.ibm.websphere.ejbcontainer.extendSetRollbackOnlyBehaviorBeyondInstanceFor
     * property contains this application name.
     */
    //d461917.1
    public boolean ivUseExtendedSetRollbackOnlyBehavior = false; //d461917.1

    /**
     * List of metadata for beans that contain automatic timers. This list is
     * null until the first bean with automatic timers is processed. This list
     * is always null after this module has started.
     */
    public List<AutomaticTimerBean> ivAutomaticTimerBeans; // d604213

    /**
     * As beans are added to the list of ivAutomaticTimerBeans, set a flag if a non-persistent
     * or persistent timer exists to call their respective createAutomaticTimers method
     */
    public boolean ivHasNonPersistentAutomaticTimers = false;
    public boolean ivHasPersistentAutomaticTimers = false;

    // F743-17630
    /**
     * Map of bean name to BeanMetaData.
     *
     * For hybrid modules, contains a BeanMetaData instance for each
     * bean component in the module. The BeanMetaData instance is
     * removed from the map after that bean component has been
     * fully initialized. At some point, when all the bean components
     * have been initialized, the map will be empty.
     *
     * For pure modules, this map is never populated and is always empty.
     */
    public Map<String, BeanMetaData> ivBeanMetaDatas = new LinkedHashMap<String, BeanMetaData>();

    /**
     * The number of beans with {@link BeanMetaData#fullyInitialized} = true.
     */
    private int ivNumFullyInitializedBeans;

    /**
     * List of application event listeners for this module.
     */
    public List<EJBApplicationEventListener> ivApplicationEventListeners; // F743-26072

    /**
     * True if this module is stopping or has stopped.
     */
    public boolean ivStopping; // F743-15941

    /**
     * True if this metadata has been created for EJBs in a WAR.
     */
    public boolean ivEJBInWAR; // F743-21131

    /**
     * True if this metadata has been created for managed beans in an
     * application client module.
     */
    public boolean ivManagedBeansInClient; // d702400

    /**
     * True if this metadata has been created for managed beans only.
     */
    public boolean ivManagedBeansOnly; // F743-36113

    /**
     * Contains the JCDIHelper if the module is CDI enabled (i.e. it contains
     * a beans.xml file).
     */
    // F743-15628 d649636
    public JCDIHelper ivJCDIHelper;

    /**
     * The base application name of a versioned module.
     */
    public String ivVersionedAppBaseName; //F54184.2

    /**
     * The base module name of a versioned module.
     */
    public String ivVersionedModuleBaseName; //F54184

    /**
     * At least one bean in the module allows caching of timer data.
     */
    public boolean ivAllowsCachedTimerData;

    /**
     * The EJBModuleMetaDataImpl constructor requires a reference to the WAS Runtime's
     * DeployedModule, and a slot count because this object ties the EJB Container to
     * the WAS Runtimes metadata framework. Also, total bean count is required so
     * so that we know how many EJBs may eventually be initialized for this module
     * (i.e. with deferred initialization this happens over time, all beans are not
     * necessarily initialized when the application first starts).
     */
    public EJBModuleMetaDataImpl(int slotCnt, EJBApplicationMetaData ejbAMD)
    {
        super(slotCnt);
        ivEJBApplicationMetaData = ejbAMD;
    }

    /**
     * Gets the application exception and rollback status of the specified
     * exception.
     *
     * @param t the exception
     * @return null if the exception is not an application exception;
     *         otherwise, Boolean.TRUE if the exception should cause rollback
     *         or Boolean.FALSE if it should not
     */
    public Boolean getApplicationExceptionRollback(Throwable t) // F743-14982
    {
        Class<?> klass = t.getClass();
        ApplicationException ae = getApplicationException(klass);
        Boolean rollback = null;

        if (ae != null)
        {
            rollback = ae.rollback();
        }
        // Prior to EJB 3.1, the specification did not explicitly state that
        // application exception status was inherited, so for efficiency, we
        // assumed it did not.  The EJB 3.1 specification clarified this but
        // used a different default.
        else if (!ContainerProperties.EE5Compatibility) // F743-14982CdRv
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                Tr.debug(tc, "searching for inherited application exception for " + klass.getName());
            }

            for (Class<?> superClass = klass.getSuperclass(); superClass != Throwable.class; superClass = superClass.getSuperclass())
            {
                ae = getApplicationException(superClass);
                if (ae != null)
                {
                    // The exception class itself has already been checked.
                    // If a super-class indicates that application exception
                    // status is not inherited, then the exception is not
                    // an application exception, so leave rollback null.
                    if (ae.inherited())
                    {
                        rollback = ae.rollback();
                    }
                    break;
                }
            }
        }

        return rollback;
    }

    /**
     * Gets the application exception status of the specified exception class
     * from either the deployment descriptor or from the annotation.
     *
     * @param klass is the Throwable class
     *
     * @return the settings, or null if no application-exception was provided
     *         for the specified class.
     */
    private ApplicationException getApplicationException(Class<?> klass) // F743-14982
    {
        ApplicationException result = null;
        if (ivApplicationExceptionMap != null)
        {
            result = ivApplicationExceptionMap.get(klass.getName());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && result != null)
            {
                Tr.debug(tc, "found application-exception for " + klass.getName()
                             + ", rollback=" + result.rollback() + ", inherited=" + result.inherited());
            }
        }
        if (result == null)
        {
            result = klass.getAnnotation(ApplicationException.class);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && result != null)
            {
                Tr.debug(tc, "found ApplicationException for " + klass.getName()
                             + ", rollback=" + result.rollback() + ", inherited=" + result.inherited());
            }
        }
        return result;
    }

    /**
     * Return the Name for this Module
     *
     * @see com.ibm.ws.runtime.metadata.MetaData
     */
    @Override
    public String getName() {
        return ivName;
    }

    /**
     * Return module version of this EJB module
     */
    @Override
    public int getEJBModuleVersion() {
        return ivModuleVersion;
    }

    /**
     * Return the Java EE name of this module
     *
     * @see com.ibm.ws.runtime.metadata.ModuleMetaData
     */
    @Override
    public J2EEName getJ2EEName() {
        return ivJ2EEName;
    }

    /**
     * Return an Array of fully initialized ComponentMetaDatas for this modules.
     */
    @Override
    public ComponentMetaData[] getComponentMetaDatas()
    {
        ComponentMetaData[] initializedComponentMetaDatas = null;

        synchronized (ivEJBApplicationMetaData)
        {
            initializedComponentMetaDatas = new ComponentMetaData[ivNumFullyInitializedBeans];

            int i = 0;
            for (BeanMetaData bmd : ivBeanMetaDatas.values())
            {
                if (bmd.fullyInitialized)
                {
                    initializedComponentMetaDatas[i++] = bmd;
                }
            }
        }

        return initializedComponentMetaDatas;
    }

    /**
     * Return the WAS Runtime ApplicationMetaData object
     *
     * @see com.ibm.ws.runtime.metadata.ModuleMetaData
     */
    @Override
    public ApplicationMetaData getApplicationMetaData() {
        return ivEJBApplicationMetaData.getApplicationMetaData(); // F743-12528
    }

    /**
     * Returns the container's metadata for the application containing this
     * module.
     */
    public EJBApplicationMetaData getEJBApplicationMetaData() {
        return ivEJBApplicationMetaData;
    }

    /**
     * Release unneeded resources
     *
     * @see com.ibm.ws.runtime.metadata.MetaData
     */
    @Override
    public void release() {
        // intentional empty method.
    }

    /**
     * Return a String with EJB Container's module level runtime config data
     */
    public String toDumpString()
    {
        // Get the New Line separator from the Java system property.
        String newLine = AccessController.doPrivileged(new SystemGetPropertyPrivileged("line.separator", "\n"));
        StringBuilder sb = new StringBuilder(toString());
        String indent = "   ";
        sb.append(newLine + newLine + "--- Dump EJBModuleMetaDataImpl fields ---");
        if (ivName != null) {
            sb.append(newLine + indent + "Module name = " + ivName);
        } else {
            sb.append(newLine + indent + "Module name = null");
        }
        // F743-25385
        if (ivLogicalName != null) {
            sb.append(newLine + indent + "Module logical name = " + ivLogicalName);
        } else {
            sb.append(newLine + indent + "Module logical name = null");
        }
        sb.append(newLine + indent + "Module version = " + ivModuleVersion);
        if (getApplicationMetaData() != null) {
            sb.append(newLine + indent + "Application metadata = " + getApplicationMetaData());
        } else {
            sb.append(newLine + indent + "Application metadata = null");
        }
        sb.append(newLine + indent + "Beans = " + ivBeanMetaDatas.keySet());
        if (ivApplicationExceptionMap != null) {
            sb.append(newLine + indent + "Application Exception map contents:");
            for (Map.Entry<String, ApplicationException> entry : ivApplicationExceptionMap.entrySet()) { // F743-14982
                ApplicationException value = entry.getValue();
                sb.append(newLine + indent + indent + "Exception: " + entry.getKey()
                          + ", rollback = " + value.rollback()
                          + ", inherited = " + value.inherited());
            }
        } else {
            sb.append(newLine + indent + "Application Exception map = null");
        }
        sb.append(newLine + indent + "SFSB failover = " + ivSfsbFailover);
        if (ivFailoverInstanceId != null) {
            sb.append(newLine + indent + "Failover instance ID = " + ivFailoverInstanceId);
        } else {
            sb.append(newLine + indent + "Failover instance ID = null");
        }
        sb.append(newLine + indent + "Fully initialized bean count = " + ivNumFullyInitializedBeans);
        sb.append(newLine + indent + "JCDIHelper = " + ivJCDIHelper);
        sb.append(newLine + indent + "UseExtendedSetRollbackOnlyBehavior = " + ivUseExtendedSetRollbackOnlyBehavior);
        sb.append(newLine + indent + "VersionedBaseName = " + ivVersionedAppBaseName + "#" + ivVersionedModuleBaseName); // F54184
        toString(sb, newLine, indent);
        sb.append(newLine + "--- End EJBModuleMetaDataImpl fields ---");
        sb.append(newLine);

        return sb.toString();

    } //toString

    protected void toString(StringBuilder sb, String nl, String indent)
    {
        // Empty.
    }

    /**
     * Determines if this follow should have been processed by the ejbdeploy
     * tool as part of application deployment.
     *
     * @return true if the module was processed by ejbdeploy
     */
    public boolean isEJBDeployed() // F743-21131
    {
        return ivModuleVersion < BeanMetaData.J2EE_EJB_VERSION_3_0 && !ivEJBInWAR; // F743-21131CodRv
    }

    /**
     * Determines the RMIC compatibility level for generated tie classes for all
     * remote interfaces for all beans in this module. This value is only used
     * if {@link #isEJBDeployed} returns false.
     *
     * @return the RMIC compatibility level
     */
    public int getRMICCompatible()
    {
        return JITDeploy.RMICCompatible;
    }

    /**
     * Get the EJBInterceptorBinding object created for a specified EJBName.
     *
     * @param ejbName of the EJB or "*" if the default interceptor binding is desired.
     *
     * @return desired EJBInterceptorBinding.
     */
    final public List<EJBInterceptorBinding> getEJBInterceptorBindings(String ejbName) // d367572.9
    {
        return ivInterceptorBindingMap.get(ejbName);
    }

    /**
     * Remove the EJBInterceptorBinding object created for a specified EJBName.
     *
     * @param ejbName of the EJB or "*" if the default interceptor binding is desired.
     *
     * @return desired EJBInterceptorBinding.
     */
    final public List<EJBInterceptorBinding> removeEJBInterceptorBindings(String ejbName) // d367572.9
    {
        return ivInterceptorBindingMap.remove(ejbName);
    }

    /**
     * Adds a new application even listener to be notified when an application
     * has fully started or will begin stopping.
     *
     * @param listener the listener
     */
    public void addApplicationEventListener(EJBApplicationEventListener listener) // F743-26072
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "addApplicationEventListener: " + listener);

        if (ivApplicationEventListeners == null)
        {
            ivApplicationEventListeners = new ArrayList<EJBApplicationEventListener>();
        }
        ivApplicationEventListeners.add(listener);
    }

    /**
     * Adds a list of timer method metadata for a bean belonging to this
     * application. This method will only be called for beans that contain
     * automatic timers.
     *
     * @param timerBean the list of timer method metadata
     */
    public void addAutomaticTimerBean(AutomaticTimerBean timerBean) // d604213
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "addAutomaticTimerBean: " + timerBean.getBeanMetaData().j2eeName);

        if (ivAutomaticTimerBeans == null)
        {
            ivAutomaticTimerBeans = new ArrayList<AutomaticTimerBean>();
        }

        ivHasNonPersistentAutomaticTimers |= timerBean.getNumNonPersistentTimers() > 0;
        ivHasPersistentAutomaticTimers |= timerBean.getNumPersistentTimers() > 0;
        ivAutomaticTimerBeans.add(timerBean);
    }

    /**
     * This method is called when EJB initialization has completed
     * and it wants to free resources no longer needed once all EJBs
     * of the module are initialized. This method can only be called
     * from the application startup thread or if the thread is holding
     * the application lock as part of deferred EJB initialization.
     *
     * @param bmd is the BeanMetaData object for the EJB that initialization
     *            just completed.
     *
     * @returns boolean - indicating true if all ejbs have been initialized.
     */
    // d468919 -added entire method.
    // d462512 - log orphan warning message.
    public void freeResourcesAfterAllBeansInitialized(BeanMetaData bmd)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "freeResourcesAfterAllBeansInitialized: " + bmd.j2eeName + ", " +
                         (ivNumFullyInitializedBeans + 1) + "/" + ivBeanMetaDatas.size());

        ivNumFullyInitializedBeans++;
        boolean freeResources = ivNumFullyInitializedBeans == ivBeanMetaDatas.size();

        // Free the resources if all beans have been initialized.
        if (freeResources)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                Tr.debug(tc, "all beans are initialized in module name = " + ivName
                             + ", freeing resources no longer needed");
            }

            // Since all beans are initialized, free any map no longer needed.
            ivInterceptorMap = null;
            ivInterceptorBindingMap = null; // d611747
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "freeResourcesAfterAllBeansInitialized");
    }

    /**
     * Set the base application and module names for EJBs defined in this
     * module if it uses a forward-compatible version strategy.
     */
    // F54184 F54184.2
    @Override
    public void setVersionedModuleBaseName(String appBaseName, String modBaseName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "ModuleName = " + ivName + ", VersionedBaseName = " +
                         appBaseName + "#" + modBaseName);

        if (ivInitData == null) {
            throw new IllegalStateException("ModuleMetaData has finished creation.");
        }
        if (appBaseName == null) {
            throw new IllegalArgumentException("appBaseName is null");
        }
        if (modBaseName == null) {
            throw new IllegalArgumentException("modBaseName is null");
        }
        ivEJBApplicationMetaData.validateVersionedModuleBaseName(appBaseName, modBaseName);

        ivVersionedAppBaseName = appBaseName;
        ivVersionedModuleBaseName = modBaseName;
    }

    /**
     * Returns true if this is a versioned module. <p>
     *
     * A module is considered versioned if a base version name has been set with {@link #setVersionedModuleBaseName}, even if the current version of the
     * module uses the same name as the base version name. <p>
     */
    // F54184
    public boolean isVersionedModule()
    {
        return ivVersionedModuleBaseName != null;
    }
} //EJBModuleMetaDataImpl

