/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.NamingException;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.PersistenceContextRef;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfigurationProvider;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessorContextImpl;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.InternalInjectionEngineAccessor;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;
import com.ibm.wsspi.injectionengine.ReferenceContext;

public class ReferenceContextImpl implements ReferenceContext {
    private static final String CLASS_NAME = ReferenceContextImpl.class.getName();
    private static final TraceComponent tc = Tr.register(ReferenceContextImpl.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    private final AbstractInjectionEngine ivInjectionEngine; // F743-33811.1, F743-31682.1

    /**
     * The providers for component namespace configuration. Each component that
     * needs processing will have its own provider. All configuration will be
     * combined into a single configuration.
     */
    private final List<ComponentNameSpaceConfigurationProvider> ivProviders = new ArrayList<ComponentNameSpaceConfigurationProvider>();

    /**
     * The root "java:" context.
     */
    private Context ivComponentJavaContext = null;

    /**
     * The javaNameSpace that backs {@link #ivComponentJavaContext}.
     */
    private Object ivComponentNameSpace = null;

    /**
     * A map of <code>{"name": binding}</code>, where "name" is relative to
     * "java:comp/env".
     */
    private HashMap<String, InjectionBinding<?>> ivJavaColonCompEnvMap;

    /**
     * A map of <code>{"name": value}</code> for each String env-entry, where
     * "name" is relative to "java:comp/env".
     */
    private Properties ivEjbContext10 = null;

    /**
     * The list of resource reference config.
     */
    private ResourceRefConfigList ivResourceRefConfigList = null;

    // F743-17630CodRv
    /**
     * True if {@link #process} has already been called.
     */
    private boolean isAlreadyProcessed = false;

    /**
     * Non-null if {@link #isAlreadyProcessed}, and the previous attempt failed.
     */
    private InjectionException ivProcessFailure;

    // F743-21418
    /**
     * Maps a <code>Class</code> to the list of <code>InjectionTargets</code>
     * visible to that Class.
     *
     * The is not usable until this <code>ReferenceContextImpl</code> instance
     * has had its <code>process</code> method invoked, and a list of resolved
     * <code>InjectionBindings</code> has been collected.
     *
     * The Map gets update by the <code>getInjectionTargets</code> method.
     *
     * The Map is used for performance reasons so that we only need to calculate
     * the <code>InjectionTargets</code> for a given Class once,
     * and after that we simply retrieve them from the Map.
     */
    private final Map<Class<?>, InjectionTarget[]> ivInjectionTargetMap = new ConcurrentHashMap<Class<?>, InjectionTarget[]>(); // PM70141 // PI63972
    private static final int svInjTarMapCacheSize = 256;

    /**
     * Map of class to injection targets declared for that class.
     */
    // d719917
    private Map<Class<?>, List<InjectionTarget>> ivDeclaredInjectionTargets;

    /**
     * Set of all classes that have been processed.
     */
    private final Set<Class<?>> ivProcessedInjectionClasses = new HashSet<Class<?>>();
    private static final int svInjClassMapCacheSize = 512;

    /**
     * Set of all injection bindings that have been processed.
     */
    private Collection<InjectionBinding<?>> ivProcessedInjectionBindings;

    /**
     * True if {@link #processDynamic} has been called.
     */
    private boolean ivAnyProcessDynamic;

    /**
     * true if this application has been configured for extra
     * configuration checking. Default is false.
     */
    // F743-33178
    private boolean ivCheckAppConfig = false;

    /**
     * This constructor should not be called directly. Callers should always
     * go through the InjectionEngine service to obtain an instance.
     */
    public ReferenceContextImpl(AbstractInjectionEngine injectionEngine) {
        ivInjectionEngine = injectionEngine; // F743-33811.1
    }

    @Override
    public String toString() {
        return super.toString() + ivProviders;
    }

    /**
     * Gets the java:comp Context built by the InjectionEngine.
     */
    @Override
    public Context getJavaCompContext() {
        return ivComponentJavaContext;
    }

    /**
     * Gets the javaNameSpace built by the InjectionEngine.
     */
    @Override
    public Object getComponentNameSpace() {
        return ivComponentNameSpace;
    }

    @Override
    public HashMap<String, InjectionBinding<?>> getJavaColonCompEnvMap() {
        return ivJavaColonCompEnvMap;
    }

    /**
     * Gets the EJBContext 1.0 style data structure built by the InjectionEngine.
     */
    @Override
    public Properties getEJBContext10Properties() {
        return ivEjbContext10;
    }

    // F743-17630CodRv
    /**
     * Gets the ResRefList data structure added to by the InjectionEngine.
     *
     * This contains a list of <code>ResRefImpl</code> instances, each of which
     * represent the resolved data for a resource reference.
     */
    @Override
    public ResourceRefConfigList getResolvedResourceRefs() {
        return ivResourceRefConfigList;
    }

    /**
     * Adds a <code>ComponentNameSpaceConfiguration</code> object representing the
     * reference data for a single component.
     */
    @Override
    public void add(final ComponentNameSpaceConfiguration compNSConfig) {
        add(new ComponentNameSpaceConfigurationProvider() {
            @Override
            public String toString() {
                return super.toString() + '[' + compNSConfig.getJ2EEName() + ']';
            }

            @Override
            public ComponentNameSpaceConfiguration getComponentNameSpaceConfiguration() {
                return compNSConfig;
            }
        });
    }

    @Override
    public synchronized void add(ComponentNameSpaceConfigurationProvider compNSConfigProvider) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "add: " + this + ", " + compNSConfigProvider);

        // F743-17630CodRv
        if (isAlreadyProcessed) {
            throw new IllegalStateException("Unable to add a new ComponentNameSpaceConfiguration info object " +
                                            "because this ReferenceContext instance has already been processed.");
        }

        ivProviders.add(compNSConfigProvider);
    }

    // F743-17630CodRv
    /**
     * Processes the reference data currently stored in this <code>ReferenceContext</code>.
     *
     * If there are multiple <code>ComponentNameSpaceConfiguration</code> instances
     * currently stored, then they are combined into a single one, and then sent
     * into the InjectionEngine.
     *
     * This method causes the InjectionEngine to populate the output data structures.
     * After this method completes, the output data structures exist and are usable.
     */
    // F743-17630CodRv
    @Override
    public synchronized void process() throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "process: " + this);

        // -------------------------------------------------------------------
        // Determine if it's valid to process the instance or not.
        // -------------------------------------------------------------------
        if (isAlreadyProcessed) {
            if (ivProcessFailure != null) {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "rethrowing " + ivProcessFailure);
                throw new InjectionException(ivProcessFailure.getMessage(), ivProcessFailure);
            }

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "No-opping the .process() method because this ReferenceContext " +
                            "instance has already been processed.");
            return;
        }

        if (ivProviders.isEmpty()) {
            throw new IllegalStateException("Unable to perform reference processing.  " +
                                            "The list of input components was empty.");
        }

        boolean complete = false;

        try {
            processImpl();
            complete = true;
        } catch (InjectionException ex) {
            ivProcessFailure = ex;
            complete = true;

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "process: " + ex);
            throw ex;
        } finally {
            if (complete) {
                // Ensure that we only attempt processing once.  This will avoid redundant
                // work, and will also ensure ComponentNameSpaceConfigurationProvider is
                // only called once.
                isAlreadyProcessed = true; // F743-17630CodRv

                // Remove input objects from memory as they are no longer needed.
                // Keep output data structures around, since they need to be retrieved
                // by the various containers at unknown point in the future.
                ivProviders.clear();
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "process");
    }

    private void processImpl() throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        int numProviders = ivProviders.size();
        List<ComponentNameSpaceConfiguration> compNSConfigs = new ArrayList<ComponentNameSpaceConfiguration>(numProviders);
        ComponentNameSpaceConfiguration primaryCompNSConfig = null;

        for (int i = 0; i < numProviders; i++) {
            ComponentNameSpaceConfigurationProvider provider = ivProviders.get(i);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "config " + i + " from " + provider);

            ComponentNameSpaceConfiguration compNSConfig = provider.getComponentNameSpaceConfiguration();
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "config " + i + " = " + compNSConfig.toDumpString());

            compNSConfigs.add(compNSConfig);

            if (compNSConfig.getOwningFlow() == ComponentNameSpaceConfiguration.ReferenceFlowKind.WEB) {
                // For hybrid flow (numProviders > 1), we use some data from the
                // web module specifically.
                primaryCompNSConfig = compNSConfig;
            }

            ivCheckAppConfig |= compNSConfig.isCheckApplicationConfiguration(); // F743-33178
        }

        if (primaryCompNSConfig == null) {
            primaryCompNSConfig = compNSConfigs.get(0);
        }

        // ------------------------------------------------
        // Create the needed output data structures
        //
        //    These output data structures are created "empty" and
        //    stored in instance variables and then passed into the
        //    InjectionEngine, where they are "populated".
        //
        //    At a future point in time, containers come back to this
        //    object instance and obtain the results of the reference
        //    processing by extracting these stored output data structures.
        // ------------------------------------------------

        J2EEName j2eeName = primaryCompNSConfig.getJ2EEName();
        String logicalModuleName = null;
        String logicalAppName = null;

        if (primaryCompNSConfig.getOwningFlow() != ComponentNameSpaceConfiguration.ReferenceFlowKind.MANAGED_BEAN) {
            // F743-26137 - Create the javaNameSpace to be capable of accessing
            // java:global, java:app, and java:module.
            logicalModuleName = primaryCompNSConfig.getLogicalModuleName();
            logicalAppName = primaryCompNSConfig.getLogicalApplicationName();

            String componentName = numProviders > 1 ? null : j2eeName.getComponent();

            try {
                // Create the server type specific Java name space          F46994.3
                ivComponentNameSpace = ivInjectionEngine.createJavaNameSpace(logicalAppName != null ? logicalAppName : logicalModuleName, // d741153
                                                                             j2eeName.getModule(),
                                                                             logicalModuleName,
                                                                             componentName);
                ivComponentJavaContext = ivInjectionEngine.createComponentNameSpaceContext(ivComponentNameSpace);
            } catch (NamingException nex) {
                FFDCFilter.processException(nex, CLASS_NAME + ".process", "517", this);
                InjectionException iex = new InjectionException("Failed to create the JNDI component name space for the " + primaryCompNSConfig.getDisplayName() +
                                                                " component in the " + primaryCompNSConfig.getModuleName() +
                                                                " module of the " + primaryCompNSConfig.getApplicationName() +
                                                                " application : " + nex.getMessage(), nex);
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "process: " + iex);
                throw iex;
            }

            ivJavaColonCompEnvMap = new LinkedHashMap<String, InjectionBinding<?>>();
            ivEjbContext10 = new Properties();
        }

        // -------------------------------------------------------------------
        // Get the cumulative ComponentNamespaceConfiguration info object
        // -------------------------------------------------------------------
        ComponentNameSpaceConfiguration mainCompNSConfig;
        List<Class<?>> annotatedClasses;

        if (numProviders == 1) {
            // ----------------------------------------------------------------
            // Update the single specified ComponentNamespaceConfiguration info
            // object with data specific to the pure flow.
            // ----------------------------------------------------------------

            // Use the specified ComponentNameSpaceConfiguration input object in a pure flow
            mainCompNSConfig = primaryCompNSConfig;

            // Stick output data structures into the input object so they can get
            // populated by the InjectionEngine.
            mainCompNSConfig.setJavaColonContext(ivComponentJavaContext);
            mainCompNSConfig.setJavaColonCompEnvMap(ivJavaColonCompEnvMap);
            mainCompNSConfig.setEnvironmentProperties(ivEjbContext10);

            // The user will ultimately retrieve their list of resolved resource
            // refs from the 'ivResolvedResourceRef' instance variable on this
            // object.  However, in the pure flow, the ResRefListImpl instance
            // in the passed in ComponentNameSpaceConfiguration object already has the
            // currently known resolved resource refs, and it will also get updated
            // by the InjectionEngine with any new resource ref data.  Thus, we need
            // to make sure that the 'ivResolvedResourceRef' instance variable
            // has a pointer to the same spot in memory as this existing list, so that
            // it can see the current resource-ref data, as well as any updates the
            // InjectionEngine makes to it.
            ivResourceRefConfigList = mainCompNSConfig.getResourceRefConfigList(); // F743-18775

            // F743-33811.1 - InjectionEngine will default this to the list of
            // injection classes if isMetadataComplete returns false.
            annotatedClasses = null;
        } else {
            // ----------------------------------------------------------------
            // Create the cumulative input data structures and hardcoded values.
            //
            // The metadata complete flag is only used in the InjectionEngine
            // itself to determine that we should create the InjectionTarget
            // list in the alternate 'expanded' format.  For a hybrid flow,
            // we should never need this alternate format.  The metadata flag
            // is no longer used to determine if we should (or should not)
            // scan classes for annotations.  Thus, we are ok hardcoding
            // to false for the hybrid flow.
            //
            // The client container does not use the hybrid flow, so the
            // isClientContainer switch will always be false.
            //
            // Both the pure web and pure ejb flows currently hardcode
            // autoInject to false, so the same is done for the hybrid flow.
            //
            // containerManagedTrans is hardcoded to false so we can resolve
            // a UserTransaction binding. We are required to resolve the
            // UserTransaction binding when the web component requests it,
            // regardless of whether the various ejb components are using
            // container managed transactions or not.
            //
            // componentBindingInfoAccessor is hardcoded to null because its only
            // used to get CMP connection factories, and CMPs are not support
            // in the hybrid flow.
            //
            // F743-17630.1
            // isBindJTAUserTran is hardcoded to true.  We want to bind the
            // ExtendedJTAUserTran in all cases, except when we are running in
            // the embeddable container.  Since we are in the hybrid flow,
            // (which is not supported by the embeddable container) we know that
            // we are not in the embeddable container, and thus we do need to
            // bind the ExtendedJTAUserTran.
            //
            // F743-17630.1
            // ejbUserTran is intentionally left null in the hybrid flow.
            // The ejbUserTran instance holds the ejb-specific UserTransactionWrapper,
            // which is only bound into the namespace in the pure ejb flow.
            // Since we are in the hybrid flow, we are not going to bind the
            // UserTransactionWrapper directly into the namespace, and so we
            // don't set the variable.
            //
            // metadataComplete does not need to be set because we pass an explicit
            // list of annotation classes to processInjectionMetaData.
            // ----------------------------------------------------------------
            Set<Class<?>> totalInjectionClasses = new HashSet<Class<?>>();
            Set<Class<?>> totalAnnotatedClasses = new HashSet<Class<?>>();
            Map<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>> totalRefs = new EnumMap<JNDIEnvironmentRefType, List<? extends JNDIEnvironmentRef>>(JNDIEnvironmentRefType.class);
            ClassLoader classLoader = null;
            boolean usesActivitySessions = false;
            boolean isSFSB = false;

            //-----------------------------------------------------------------
            // Combine the data from all input components
            //
            // Since we are in a hybrid flow, there are multiple
            // input components, one representing the web data,
            // and additional components for each of the ejb components.
            // ----------------------------------------------------------------
            for (int componentIndex = 0; componentIndex < numProviders; componentIndex++) {
                ComponentNameSpaceConfiguration compNSConfig = compNSConfigs.get(componentIndex);
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "merging component " + componentIndex + ": " + compNSConfig.getJ2EEName());
                }

                // The list of classes in play should never be null at the current time
                // because the only users of this new reference processing framework right
                // now are the pure ejb and hybrid flows.
                //
                // Once pure web or client container comes on board, then we'll either need
                // to update this logic to account for a null list of injection classes, or
                // we'll need to get those groups to agree to not send in a null class list.
                List<Class<?>> classesInPlay = compNSConfig.getInjectionClasses();
                if (classesInPlay != null) {
                    totalInjectionClasses.addAll(classesInPlay);

                    if (!compNSConfig.isMetaDataComplete()) {
                        totalAnnotatedClasses.addAll(classesInPlay);
                    }
                }

                JNDIEnvironmentRefType.addAllRefs(totalRefs, compNSConfig);

                ClassLoader currentClassLoader = compNSConfig.getClassLoader();
                if (classLoader == null) {
                    classLoader = currentClassLoader;
                } else {
                    if (classLoader != currentClassLoader) {
                        throw new IllegalStateException("Unable to perform reference processing. " +
                                                        "The input components are not using the same classloader.");
                    }
                }

                if (!usesActivitySessions && compNSConfig.usesActivitySessions()) {
                    // If any component in the entire list uses activity sessions,
                    // set flag to true so ActivitySession gets bound into namespace.
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Setting the usesActivitySessions flag to true for hybrid module.");
                    }
                    usesActivitySessions = true;
                }

                if (!isSFSB && compNSConfig.isSFSB()) {
                    // If any bean component in the hybrid module is a stateful session
                    // bean, then we set the flag to true.  This will result in a
                    // serializable JPA wrapper getting created for the beans, which
                    // allows the JPA persistence context to survive stateful session
                    // bean passivation/reactivation.
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Setting SFSB flag to true because at least one bean " +
                                     "module is a SFSB.");
                    }
                    isSFSB = true;
                }

            }

            annotatedClasses = new ArrayList<Class<?>>(totalAnnotatedClasses);

            // ----------------------------------------------------------------
            // Create the main input object using the cumulative just gathered.
            // ----------------------------------------------------------------
            mainCompNSConfig = new ComponentNameSpaceConfiguration(primaryCompNSConfig.getModuleName(), primaryCompNSConfig.getJ2EEName()); // F48603.7
            mainCompNSConfig.setLogicalModuleName(logicalAppName, logicalModuleName); // F743-29417
            mainCompNSConfig.setOwningFlow(ComponentNameSpaceConfiguration.ReferenceFlowKind.HYBRID);
            mainCompNSConfig.setCheckApplicationConfiguration(ivCheckAppConfig); // F743-33178

            mainCompNSConfig.setJavaColonContext(ivComponentJavaContext);
            mainCompNSConfig.setJavaColonCompEnvMap(ivJavaColonCompEnvMap);
            mainCompNSConfig.setClassLoader(classLoader);
            mainCompNSConfig.setModuleMetaData(primaryCompNSConfig.getModuleMetaData());
            mainCompNSConfig.setModuleLoadStrategy(primaryCompNSConfig.getModuleLoadStrategy());
            mainCompNSConfig.setInjectionClasses(new ArrayList<Class<?>>(totalInjectionClasses));
            mainCompNSConfig.setSFSB(isSFSB);
            mainCompNSConfig.setUsesActivitySessions(usesActivitySessions);

            mainCompNSConfig.setEnvironmentProperties(ivEjbContext10);
            JNDIEnvironmentRefType.setAllRefs(mainCompNSConfig, totalRefs);

            mergeResRefsAndBindings(mainCompNSConfig, compNSConfigs, null); // F743-33811.2
            ivResourceRefConfigList = mainCompNSConfig.getResourceRefConfigList();

            createPersistenceMaps(mainCompNSConfig, compNSConfigs); // F743-30682
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "annotatedClasses=" + annotatedClasses);

        InjectionProcessorContextImpl context = ivInjectionEngine.createInjectionProcessorContext(); // F743-33811.1
        mainCompNSConfig.setInjectionProcessorContext(context);

        List<Class<?>> injectionClasses = mainCompNSConfig.getInjectionClasses();
        if (injectionClasses != null) {
            ivProcessedInjectionClasses.addAll(injectionClasses);
        }

        //-----------------------------------------------
        // Do the actual reference processing
        //-----------------------------------------------
        ivInjectionEngine.processInjectionMetaData(mainCompNSConfig, annotatedClasses); // F743-33811.1

        // F743-21481
        // Each of the InjectionProcessor instances provided a set of resolved
        // InjectionBindings.  Obtain the map of declared injection targets
        // for all classes and subclass that we know of so that we can later
        // calculate InjectionTarget for a specified class hierarchy.
        List<InjectionBinding<?>> injectionBindings = context.ivProcessedInjectionBindings; // F743-33811.1
        ivProcessedInjectionBindings = injectionBindings;
        ivDeclaredInjectionTargets = ivInjectionEngine.getDeclaredInjectionTargets(injectionBindings); // d719917

        // Now that injection targets are obtained, complete the context.   F87539
        context.metadataProcessingComplete();

        // d643203
        // -------------------------------------------------------------------
        // Invoke any injection engine listeners
        //
        //       The only known listener at this time is webservices.
        //
        //       Any users still on the legacy (ie, non-referenceContext) path
        //       through the injection engine get the listeners invoked inside
        //       the injection engine itself.
        //
        //       The listeners are invoked now (instead of during the injection
        //       engine processing itself) so that we can pass in the fully
        //       initialized ReferenceContext instance.  Specifically, we need
        //       the list of InjectionBinding instances discovered by the
        //       reference processing to be in the ReferenceContext, since that
        //       information is what the webservice listener actually cares about.
        // -------------------------------------------------------------------
        ivInjectionEngine.notifyInjectionMetaDataListeners(this, mainCompNSConfig);

        if (isTraceOn && tc.isDebugEnabled()) {
            Tr.debug(tc, "Output component context:", ivComponentJavaContext);
            if (ivJavaColonCompEnvMap != null) {
                Tr.debug(tc, "Output java:comp/env map: ", dumpJavaColonCompEnvMap()); // F743-17630CodRv
            }
            Tr.debug(tc, "Output EJB context 1.0: ", ivEjbContext10);
            Tr.debug(tc, "Output ResourceRefs: ", ivResourceRefConfigList);
            Tr.debug(tc, "Output InjectionBindings: ", injectionBindings); // F743-21481
        }
    }

    /**
     * Create the class-to-components and persistence-refs-to-components maps
     * and set them on the specified ComponentNameSpaceConfiguration.
     */
    // F743-30682
    private void createPersistenceMaps(ComponentNameSpaceConfiguration mainCompNSConfig,
                                       List<ComponentNameSpaceConfiguration> compNSConfigs) {
        Map<Class<?>, Collection<String>> classesToComponents = new HashMap<Class<?>, Collection<String>>();
        Map<String, Collection<String>> persistenceRefsToComponents = new HashMap<String, Collection<String>>();

        for (ComponentNameSpaceConfiguration compNSConfig : compNSConfigs) {
            ComponentMetaData cmd = compNSConfig.getComponentMetaData();
            if (cmd != null) {
                String name = cmd.getJ2EEName().getComponent();

                if (!compNSConfig.isMetaDataComplete()) {
                    List<Class<?>> classesToScan = compNSConfig.getInjectionClasses();
                    if (classesToScan != null) {
                        for (Class<?> klass : classesToScan) {
                            for (Class<?> superClass = klass; superClass != null && superClass != Object.class; superClass = superClass.getSuperclass()) {
                                addComponentToPersistenceMap(classesToComponents, superClass, name);
                            }
                        }
                    }
                }

                List<? extends PersistenceContextRef> pcRefs = compNSConfig.getPersistenceContextRefs();
                if (pcRefs != null) {
                    for (PersistenceContextRef ref : pcRefs) {
                        addComponentToPersistenceMap(persistenceRefsToComponents, ref.getName(), name);
                    }
                }

                List<? extends PersistenceUnitRef> puRefs = compNSConfig.getPersistenceUnitRefs();
                if (puRefs != null) {
                    for (PersistenceUnitRef ref : puRefs) {
                        addComponentToPersistenceMap(persistenceRefsToComponents, ref.getName(), name);
                    }
                }
            }
        }

        mainCompNSConfig.setPersistenceMaps(classesToComponents, persistenceRefsToComponents);
    }

    private <T> void addComponentToPersistenceMap(Map<T, Collection<String>> map, T key, String name) {
        Collection<String> components = map.get(key);
        if (components == null) {
            components = new LinkedHashSet<String>();
            map.put(key, components);
        }

        components.add(name);
    }

    // F743-17630CodRv
    /**
     * Provides nice looking trace output for the EJBContext.lookup data structure.
     */
    private String dumpJavaColonCompEnvMap() {
        StringBuffer buffer = new StringBuffer("");
        buffer.append("EJBContext.lookup data structure contents:\n");
        buffer.append("   Contains **" + ivJavaColonCompEnvMap.size() + "** bindings.\n");
        if (!ivJavaColonCompEnvMap.isEmpty()) {
            Set<Map.Entry<String, InjectionBinding<?>>> entries = ivJavaColonCompEnvMap.entrySet();
            Iterator<Map.Entry<String, InjectionBinding<?>>> entryIterator = entries.iterator();
            int count = 0;
            while (entryIterator.hasNext()) {
                Map.Entry<String, InjectionBinding<?>> oneEntry = entryIterator.next();
                buffer.append("     Entry " + count + "\n");
                buffer.append("            Key: **" + oneEntry.getKey() + "**\n");
                buffer.append("            Value: **" + oneEntry.getValue() + "**\n");
                buffer.append("\n");
                count++;
            }
        }
        return buffer.toString();
    }

    /**
     * Merge bindings and resource references.
     *
     * @param mainCompNSConfig the output component
     * @param compNSConfigs    the input components
     * @param scope            the desired scope, or null for all scopes
     */
    static void mergeResRefsAndBindings(ComponentNameSpaceConfiguration mainCompNSConfig,
                                        List<ComponentNameSpaceConfiguration> compNSConfigs,
                                        InjectionScope scope) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "mergeResRefsAndBindings");

        Map<String, ResourceRefConfig[]> resRefMap = new LinkedHashMap<String, ResourceRefConfig[]>();

        Map<JNDIEnvironmentRefType, Map<String, ComponentNameSpaceConfiguration>> allBindingComps = new EnumMap<JNDIEnvironmentRefType, Map<String, ComponentNameSpaceConfiguration>>(JNDIEnvironmentRefType.class);
        for (JNDIEnvironmentRefType refType : JNDIEnvironmentRefType.VALUES) {
            if (refType.getBindingElementName() != null) {
                mainCompNSConfig.setJNDIEnvironmentRefBindings(refType.getType(), new HashMap<String, String>());
                allBindingComps.put(refType, new HashMap<String, ComponentNameSpaceConfiguration>());
            }
        }

        Map<String, String> envEntryValues = new HashMap<String, String>();
        Map<String, ComponentNameSpaceConfiguration> envEntryValueComps = new HashMap<String, ComponentNameSpaceConfiguration>();
        mainCompNSConfig.setEnvEntryValues(envEntryValues);

        boolean refMergeSuccess = true;
        for (int componentIndex = 0; componentIndex < compNSConfigs.size(); componentIndex++) {
            ComponentNameSpaceConfiguration compNSConfig = compNSConfigs.get(componentIndex);
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "merging component " + componentIndex + ": " + compNSConfig.getJ2EEName());

            // d643480 - Build map of resource reference arrays for merging.
            ResourceRefConfigList resRefs = compNSConfig.getResourceRefConfigList();
            if (resRefs != null) {
                for (int i = 0, size = resRefs.size(); i < size; i++) {
                    ResourceRefConfig resRef = resRefs.getResourceRefConfig(i);
                    String resRefName = resRef.getName();

                    if (scope == null ||
                        scope == InjectionScope.match(resRefName)) {
                        ResourceRefConfig[] resRefArray = resRefMap.get(resRefName);
                        if (resRefArray == null) {
                            resRefArray = new ResourceRefConfig[compNSConfigs.size()];
                            resRefMap.put(resRefName, resRefArray);
                        }

                        // Like in previous releases, we won't error if an individual
                        // component has conflicting ResRefs.  ResRefList.findByName
                        // has always returned the first ResRef in the list, so
                        // ignore subsequent ResRefs for this component.
                        if (resRefArray[componentIndex] == null) {
                            resRefArray[componentIndex] = resRef;
                        }
                    }
                }
            }

            for (JNDIEnvironmentRefType refType : JNDIEnvironmentRefType.VALUES) {
                if (refType.getBindingElementName() != null) {
                    refMergeSuccess &= mergeBindings(compNSConfig, scope,
                                                     refType.getBindingElementName(),
                                                     refType.getBindingAttributeName(),
                                                     compNSConfig.getJNDIEnvironmentRefBindings(refType.getType()),
                                                     mainCompNSConfig.getJNDIEnvironmentRefBindings(refType.getType()),
                                                     allBindingComps.get(refType));
                }
            }

            refMergeSuccess &= mergeBindings(compNSConfig, scope,
                                             "env-entry", "value",
                                             compNSConfig.getEnvEntryValues(),
                                             envEntryValues, envEntryValueComps);
        }

        refMergeSuccess &= mergeResRefs(mainCompNSConfig, compNSConfigs, resRefMap); // d643480

        if (!refMergeSuccess) {
            throw new InjectionConfigurationException("There were conflicting references.  " +
                                                      "See CWNEN0061E messages in log for details.");
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "mergeResRefsAndBindings");
    }

    /**
     * Merges bindings and places the result in <tt>allBindings</tt>.
     *
     * @param compNSConfig  the component of the bindings being merged
     * @param scope         the desired scope, or null for all scopes
     * @param whatType      the binding element type name
     * @param whatAttribute the binding element attribute name
     * @param bindings      the new component bindings
     * @param allBindings   the output mapping of reference name to binding
     * @param allComps      the output mapping of reference name to the name of the
     *                          component that provided the reference binding
     * @return <tt>true</tt> if the merge was successful, or <tt>false</tt> if
     *         conflicts were reported
     */
    private static boolean mergeBindings(ComponentNameSpaceConfiguration compNSConfig,
                                         InjectionScope scope, // F743-33811.2
                                         String whatType,
                                         String whatAttribute,
                                         Map<String, String> bindings,
                                         Map<String, String> allBindings,
                                         Map<String, ComponentNameSpaceConfiguration> allComps) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "mergeBindings: " + whatType);

        boolean success = true;
        if (bindings != null) {
            for (Map.Entry<String, String> entry : bindings.entrySet()) {
                String refName = entry.getKey();
                if (scope == null || scope == InjectionScope.match(refName)) {
                    String binding = entry.getValue();
                    String oldBinding = allBindings.get(refName);

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "merging " + refName + ": " + binding + " and " + oldBinding);

                    if (oldBinding == null) {
                        allBindings.put(refName, binding);
                        allComps.put(refName, compNSConfig);
                    } else if (!binding.equals(oldBinding)) {
                        ComponentNameSpaceConfiguration oldCompNSConfig = allComps.get(refName);
                        Tr.error(tc, "CONFLICTING_REFERENCES_CWNEN0062E",
                                 oldCompNSConfig.getDisplayName(),
                                 compNSConfig.getDisplayName(),
                                 compNSConfig.getModuleName(),
                                 compNSConfig.getApplicationName(),
                                 whatAttribute,
                                 refName,
                                 oldBinding,
                                 binding);
                        success = false;
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "mergeBindings: " + success);
        return success;
    }

    /**
     * Merge resource references. Each array in <tt>totalResRefs</tt> contains
     * resource references from the input components categorized by resource
     * reference name. Each array has a length equal to the number of input
     * components, and the entry in the array represents the resource reference
     * with a given name contributed by that component. If the component does
     * not have a reference by that name, the entry will be <tt>null</tt>.
     *
     * @param mainCompNSConfig the output component configuration
     * @param compNSConfigs    the input component configurations
     * @param totalResRefs     mapping of resource reference name to array of
     *                             resources indexed by component index
     * @return <tt>true</tt> if the merge was successful, or <tt>false</tt> if
     *         conflicts were reported
     */
    private static boolean mergeResRefs(ComponentNameSpaceConfiguration mainCompNSConfig,
                                        List<ComponentNameSpaceConfiguration> compNSConfigs,
                                        Map<String, ResourceRefConfig[]> totalResRefs) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "mergeResRefs");

        ResourceRefConfigList resRefList = InternalInjectionEngineAccessor.getInstance().createResourceRefConfigList();
        mainCompNSConfig.setResourceRefConfigList(resRefList);

        List<ResourceRefConfig.MergeConflict> conflicts = new ArrayList<ResourceRefConfig.MergeConflict>();
        for (Map.Entry<String, ResourceRefConfig[]> entry : totalResRefs.entrySet()) {
            ResourceRefConfig mergedResRef = resRefList.findOrAddByName(entry.getKey());
            mergedResRef.mergeBindingsAndExtensions(entry.getValue(), conflicts);
        }

        boolean success = conflicts.isEmpty();
        if (!success) {
            for (ResourceRefConfig.MergeConflict conflict : conflicts) {
                Tr.error(tc, "CONFLICTING_REFERENCES_CWNEN0062E",
                         compNSConfigs.get(conflict.getIndex1()).getDisplayName(),
                         compNSConfigs.get(conflict.getIndex2()).getDisplayName(),
                         mainCompNSConfig.getModuleName(),
                         mainCompNSConfig.getApplicationName(),
                         conflict.getAttributeName(),
                         conflict.getResourceRefConfig().getName(),
                         conflict.getValue1(),
                         conflict.getValue2());
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "mergeResRefs: " + success);
        return success;
    }

    @Override
    public synchronized void processDynamic(final ComponentNameSpaceConfiguration compNSConfig) throws InjectionException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws InjectionException {
                    processDynamicPrivileged(compNSConfig);
                    return null;
                }
            });
        } catch (PrivilegedActionException paex) {
            Throwable cause = paex.getCause();
            if (cause instanceof InjectionException) {
                throw (InjectionException) cause;
            }
            throw new Error(cause);
        }
    }

    /**
     * Performs the function of {@link #processDynamic}.
     *
     * This method requires permissions to use {@link Class#getDeclaredFields()}
     * and calls to this method must be synchronized.
     */
    private void processDynamicPrivileged(ComponentNameSpaceConfiguration compNSConfig) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processDynamic");

        List<Class<?>> injectionClasses = compNSConfig.getInjectionClasses();
        if (isProcessDynamicNeeded(injectionClasses)) {
            Map<String, InjectionBinding<?>> completedInjectionBindings = new HashMap<String, InjectionBinding<?>>();
            for (InjectionBinding<?> binding : ivProcessedInjectionBindings) {
                completedInjectionBindings.put(binding.getJndiName(), binding);
            }

            // Process injection metadata for the new injection classes.
            InjectionProcessorContextImpl context = ivInjectionEngine.createInjectionProcessorContext();
            context.ivCompletedInjectionBindings = completedInjectionBindings;
            compNSConfig.setInjectionProcessorContext(context);
            ivInjectionEngine.processInjectionMetaData(compNSConfig, injectionClasses);

            // Add new processed injection bindings.  If this is the first time
            // we've done dynamic processing, copy the ArrayList to a Set to
            // avoid duplicates.
            if (!ivAnyProcessDynamic) {
                ivAnyProcessDynamic = true;
                ivProcessedInjectionBindings = new LinkedHashSet<InjectionBinding<?>>(ivProcessedInjectionBindings);
            }
            ivProcessedInjectionBindings.addAll(context.ivProcessedInjectionBindings);

            // Add new declared injection targets.
            Map<Class<?>, List<InjectionTarget>> declaredTargets = ivInjectionEngine.getDeclaredInjectionTargets(context.ivProcessedInjectionBindings);
            ivDeclaredInjectionTargets.putAll(declaredTargets);

            //clear the cached list of injection targets for the dynamic classes
            for (Class<?> injectionClass : injectionClasses) {
                ivInjectionTargetMap.remove(injectionClass);
            }

            // Now that injection targets are obtained, complete the context.
            context.metadataProcessingComplete();

            // Now that metadata has processed successfully, add all the classes
            // to the list of processed classes.
            if (ivProcessedInjectionClasses.size() <= svInjClassMapCacheSize) {
                ivProcessedInjectionClasses.addAll(injectionClasses);
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "processDynamic: added to processed classes; size = " + ivProcessedInjectionClasses.size());
            } else {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "processDynamic: not added to processed classes; size = " + ivProcessedInjectionClasses.size());
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processDynamic");
    }

    /**
     * Returns true if dynamic processing is needed for any of the classes.
     */
    @Override
    public boolean isProcessDynamicNeeded(List<Class<?>> injectionClasses) {
        for (Class<?> klass : injectionClasses) {
            if (!ivProcessedInjectionClasses.contains(klass)) {
                return true;
            }
        }

        return false;
    }

    // F743-21481
    /**
     * Gets the <code>InjectionTarget</code> instances visible to the specified Class.
     *
     * If there are no <code>InjectionTarget</code> instances visible to the Class,
     * then an empty (non-null) list is returned.
     */
    @Override
    public InjectionTarget[] getInjectionTargets(final Class<?> classToInjectInto) throws InjectionException {
        // This method attempts to cache the InjectTargets that are associated
        // with the specified class, so that if this method is invoked again and
        // the same class is specified, we can just grab the list from the Map
        // and not re-calculate it.
        //
        // EJB container should not need this caching, because it caches the list
        // of InjectionTargets in BeanMetaData and InterceptorMetaData.
        //
        // However, WebContainer will use this caching.  Under most circumstances,
        // they only inject into a class once...but I WebContainer team lead says
        // its possible to have multiple servlets share the same class, and in that
        // scenario they would be injecting into the same class multiple times,
        // and take advantage of the caching.

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getInjectionTargets: " + classToInjectInto + ", " + this);

        InjectionTarget[] injectionTargetsForClass = ivInjectionTargetMap.get(classToInjectInto);
        if (injectionTargetsForClass == null) {
            synchronized (ivInjectionTargetMap) {
                injectionTargetsForClass = ivInjectionTargetMap.get(classToInjectInto);
                if (injectionTargetsForClass == null) {
                    // If targets were not found in cache from app start, then rebuilding them
                    // needs to run privileged, as this may run in the context of the application
                    try {
                        injectionTargetsForClass = AccessController.doPrivileged(new PrivilegedExceptionAction<InjectionTarget[]>() {
                            @Override
                            public InjectionTarget[] run() throws Exception {
                                return ivInjectionEngine.getInjectionTargets(ivDeclaredInjectionTargets, classToInjectInto, ivCheckAppConfig);
                            }
                        });
                    } catch (PrivilegedActionException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof InjectionException) {
                            throw (InjectionException) cause;
                        }
                        if (cause instanceof RuntimeException) {
                            throw (RuntimeException) cause;
                        }
                        throw new InjectionException(cause);
                    }
                    if (ivInjectionTargetMap.size() <= svInjTarMapCacheSize || injectionTargetsForClass.length > 0) {
                        ivInjectionTargetMap.put(classToInjectInto, injectionTargetsForClass);
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "getInjectionTargets: added to cache; size = " + ivInjectionTargetMap.size());
                    } else {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "getInjectionTargets: not added to cache; size = " + ivInjectionTargetMap.size());
                    }
                } else {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "getInjectionTargets: found in cache; size = " + ivInjectionTargetMap.size());
                }
            }
        } else {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "getInjectionTargets: found in cache; size = " + ivInjectionTargetMap.size());
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getInjectionTargets", Arrays.asList(injectionTargetsForClass));
        return injectionTargetsForClass;
    }

    @Override
    public synchronized Set<Class<?>> getProcessedInjectionClasses() {
        // Synchronized to ensure processDynamic() is not concurrently updating the
        // set of processed injection classes.
        return new HashSet<Class<?>>(ivProcessedInjectionClasses);
    }
}
