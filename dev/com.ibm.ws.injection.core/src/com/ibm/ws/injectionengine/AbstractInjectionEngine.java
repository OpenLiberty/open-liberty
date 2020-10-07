/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.naming.Context;
import javax.naming.spi.ObjectFactory;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.injectionengine.factory.MBLinkReferenceFactoryImpl;
import com.ibm.ws.injectionengine.ffdc.Formattable;
import com.ibm.ws.injectionengine.ffdc.InjectionDiagnosticModule;
import com.ibm.ws.injectionengine.processor.DataSourceDefinitionProcessorProvider;
import com.ibm.ws.injectionengine.processor.ResourceProcessorProvider;
import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.ws.util.dopriv.GetClassLoaderPrivileged;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionMetaData;
import com.ibm.wsspi.injectionengine.InjectionMetaDataListener;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorContextImpl;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.InternalInjectionEngineAccessor;
import com.ibm.wsspi.injectionengine.MethodMap;
import com.ibm.wsspi.injectionengine.ObjectFactoryInfo;
import com.ibm.wsspi.injectionengine.OverrideInjectionProcessor;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.injectionengine.factory.EJBLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.IndirectJndiLookupReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.MBLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.OverrideReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResAutoLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResRefReferenceFactory;

/**
 * The implementation of the InjectionEngine interface that is shared by all
 * runtime environments.
 */
public abstract class AbstractInjectionEngine implements InternalInjectionEngine, Formattable {
    private static final TraceComponent tc = Tr.register(AbstractInjectionEngine.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    private static final ThreadContextAccessor svThreadContextAccessor = ThreadContextAccessor.getThreadContextAccessor(); // F54050

    private static final InjectionTarget[] EMPTY_INJECTION_TARGETS = new InjectionTarget[0];

    /**
     * Default Naming Reference Factory to be used when a resource-ref for a
     * managed bean or ManagedBean annotation will use auto-link.
     **/
    // d698540.1
    private static final MBLinkReferenceFactory DEFAULT_MBLinkRefFactory = new MBLinkReferenceFactoryImpl();

    protected MBLinkReferenceFactory ivMBLinkRefFactory = DEFAULT_MBLinkRefFactory; // d703474

    private final Map<Class<?>, InjectionProcessorProvider<?, ?>> ivProcessorProviders = new ConcurrentHashMap<Class<?>, InjectionProcessorProvider<?, ?>>();

    /**
     * Map of annotation to map of data type to ObjectFactory. These are
     * ObjectFactories that have been registered to extend the data types
     * supported by the base processor implementation. When an annotation
     * (or XML) is applied to the specified data type, then the corresponding
     * ObjectFactory should be used.
     *
     * <p>Concurrency for this map is handled by using synchronized-read and
     * copy-on-write as needed using {@link #ivObjectFactoryMapCopyOnWrite}.
     **/
    // F623-841
    private Map<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>> ivObjectFactoryMap = new HashMap<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>>();

    /**
     * True indicates {@link #ivObjectFactoryMap} must be copied before modification.
     */
    // PM79779
    private boolean ivObjectFactoryMapCopyOnWrite;

    /**
     * Map of annotation to map of data type to ObjectFactory. These are
     * ObjectFactories that have been registered to extend the data types
     * supported by the base processor implementation and do NOT support being
     * overridden with a binding. When an annotation (or XML) is applied to
     * the specified data type, then the corresponding ObjectFactory should be
     * used, regardless of whether a binding is present or not.
     *
     * <p>Concurrency for this map is handled by using synchronized-read and
     * copy-on-write as needed using {@link #ivNoOverrideObjectFactoryMapCopyOnWrite}.
     **/
    // F623-841.1
    private Map<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>> ivNoOverrideObjectFactoryMap = new HashMap<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>>();

    /**
     * True indicates {@link #ivNoOverrideObjectFactoryMap} must be copied before modification.
     */
    // PM79779
    private boolean ivNoOverrideObjectFactoryMapCopyOnWrite;

    /**
     * Map of annotation to Array of OverrideReferenceFactory instances that
     * have been registered for that annotation. Before performing normal
     * 'resolve' processing, all OverrideReferenceFactory instances should be
     * given an opportunity to override the reference, until one elects to
     * provide an override.
     *
     * <p>Concurrency for this map is handled by using synchronized-read and
     * copy-on-write as needed using {@link #ivOverrideReferenceFactoryMapCopyOnWrite}.
     **/
    // F1339-9050
    private HashMap<Class<? extends Annotation>, OverrideReferenceFactory<?>[]> ivOverrideReferenceFactoryMap = new HashMap<Class<? extends Annotation>, OverrideReferenceFactory<?>[]>();

    /**
     * True indicates {@link #ivOverrideReferenceFactoryMap} must be copied before modification.
     */
    // PM79779
    private boolean ivOverrideReferenceFactoryMapCopyOnWrite;

    //A boolean to control registration ability
    private boolean ivIsInitialized = false;

    //List of all InjectionMetaDataListener instances registered with the injection engine
    private final List<InjectionMetaDataListener> metaDataListeners = new CopyOnWriteArrayList<InjectionMetaDataListener>();

    /**
     * The set of class loaders that have been checked for overridden annotation
     * classes.
     */
    private final Set<ClassLoader> ivCheckedAnnotationClassLoaders = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<ClassLoader, Boolean>())); // d676633

    /**
     * A mapping from class loaders to the set of annotation classes that they
     * define for which we have already issued a warning.
     *
     * <p>Concurrency for this map is handled by synchronizing on this object.
     */
    private Map<ClassLoader, Set<String>> ivWarnedClassLoaderAnnotations; // d676633

    /**
     * Returns true if this injection engine is running in the embeddable EJB
     * container.
     */
    @Override
    public abstract boolean isEmbeddable();

    /**
     * Default Naming Reference Factory to be used when a resource reference has
     * a binding.
     **/
    // d442047
    protected abstract IndirectJndiLookupReferenceFactory getDefaultIndirectJndiLookupReferenceFactory();

    /**
     * Default Naming Reference Factory to be used when an EJB reference has a
     * binding.
     */
    protected abstract IndirectJndiLookupReferenceFactory getDefaultResIndirectJndiLookupReferenceFactory();

    /**
     * Default Naming Reference Factory to be used when a resource-ref or
     * Resource annotation has a binding override.
     **/
    // d442047
    protected abstract ResRefReferenceFactory getDefaultResRefReferenceFactory();

    /**
     * Default Resource Reference Factory to be used when an reference handled
     * by the resource processor will use auto-link.
     */
    protected abstract ResAutoLinkReferenceFactory getDefaultResAutoLinkReferenceFactory();

    /**
     * Default Naming Reference Factory to be used when an ejb-ref or EJB
     * annotation has an ejb-link/beanName or will use auto-link.
     **/
    // d442047
    protected abstract EJBLinkReferenceFactory getDefaultEJBLinkReferenceFactory();

    /**
     * Gets injection data for the global, application, module, or component
     * scope.
     *
     * @param md <tt>null</tt> for the global scope, {@link ApplicationMetaData}, {@link ModuleMetaData}, or {@link ComponentMetaData}
     */
    public abstract InjectionScopeData getInjectionScopeData(MetaData md);

    /**
     * Returns the {@link ResourceFactoryBuilder} for a resource factory type.
     *
     * @param type the resource type
     */
    public abstract ResourceFactoryBuilder getResourceFactoryBuilder(String type) throws InjectionException;

    public void initialize() {
        InternalInjectionEngineAccessor.setInjectionEngine(this); // F46994.2

        InjectionDiagnosticModule dm = InjectionDiagnosticModule.instance();
        dm.initialize(this);
        dm.registerWithFFDCService();

        // set the flag to true
        ivIsInitialized = true;

        try {
            registerInjectionProcessorProvider(new ResourceProcessorProvider()); // RTC115266
            registerInjectionProcessorProvider(new DataSourceDefinitionProcessorProvider()); // F743-21590, RTC115266
        } catch (InjectionException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Set the initialized flag to false so that others may not NOT register in their start method.
     */
    public void stop() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "stop");
        ivIsInitialized = false;
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "stop");
    }

    /**
     * Registers the specified processor with the injection engine.
     *
     * If a processor was already registered with the specified annotation, that class will be returned
     * otherwise a null will be returned.
     *
     * @param processor  The processor class to be registered
     * @param annotation The annotation class the processor is associated.
     * @throws InjectionException if the provider is already registered
     */
    @Override
    public <A extends Annotation, AS extends Annotation> void registerInjectionProcessor(Class<? extends InjectionProcessor<A, AS>> processor,
                                                                                         Class<A> annotation) throws InjectionException {
        if (OverrideInjectionProcessor.class.isAssignableFrom(processor)) {
            throw new IllegalArgumentException("OverrideInjectionProcessor must be registered with an InjectionProcessorProvider");
        }

        registerInjectionProcessorProvider(new InjectionProcessorProviderImpl<A, AS>(annotation, processor));
    }

    @Override
    public synchronized void registerInjectionProcessorProvider(InjectionProcessorProvider<?, ?> provider) throws InjectionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.debug(tc, "registerInjectionProcessor: " +
                         provider.getAnnotationClass() + " = " + provider);

        if (!ivIsInitialized) {
            Tr.error(tc, "INJECTION_ENGINE_SERVICE_NOT_INITIALIZED_CWNEN0006E");
            throw new InjectionException("injection engine is not initialized");
        }

        Class<?> annotationClass = provider.getAnnotationClass();

        // Since OSGi doesn't force an order for register/unregister when a service restarts,
        // allowing either order, so the last register call will override any prior register.

        InjectionProcessorProvider<?, ?> previousProvider = ivProcessorProviders.put(annotationClass, provider);

        if (previousProvider != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.debug(tc, "registerInjectionProcessor: provider already registered for " + annotationClass.getName() + ", previous = " + previousProvider);
        }
    }

    @Override
    public synchronized void unregisterInjectionProcessorProvider(InjectionProcessorProvider<?, ?> provider) throws InjectionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.debug(tc, "unregisterInjectionProcessorProvider: " +
                         provider.getAnnotationClass() + " = " + provider);

        if (!ivIsInitialized) {
            Tr.error(tc, "INJECTION_ENGINE_SERVICE_NOT_INITIALIZED_CWNEN0006E");
            throw new InjectionException("injection engine is not initialized");
        }

        Class<?> annotationClass = provider.getAnnotationClass();

        // Since OSGi doesn't force an order for register/unregister when a service restarts,
        // allowing either order, so the last register call will override any prior register.

        InjectionProcessorProvider<?, ?> currentProvider = ivProcessorProviders.get(annotationClass);

        if (currentProvider == provider) {
            ivProcessorProviders.remove(annotationClass);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.debug(tc, "unregisterInjectionProcessorProvider: provider not registered for " + annotationClass.getName() + ", current = " + currentProvider);
        }
    }

    InjectionProcessorContextImpl createInjectionProcessorContext() {
        InjectionProcessorContextImpl context = new InjectionProcessorContextImpl();

        // Copy the map instance variables into the processor context, and
        // indicate they must be copied if a call is made to modify them, since
        // the current copies are in use.                                  PM79779
        synchronized (this) {
            context.ivObjectFactoryMap = ivObjectFactoryMap;
            ivObjectFactoryMapCopyOnWrite = true;

            context.ivNoOverrideObjectFactoryMap = ivNoOverrideObjectFactoryMap;
            ivNoOverrideObjectFactoryMapCopyOnWrite = true;

            context.ivOverrideReferenceFactoryMap = ivOverrideReferenceFactoryMap;
            ivOverrideReferenceFactoryMapCopyOnWrite = true;
        }

        return context;
    }

    ComponentNameSpaceConfiguration createNonCompNameSpaceConfig(InjectionScope scope,
                                                                 J2EEName j2eeName,
                                                                 Context javaColonContext,
                                                                 InjectionProcessorContextImpl context) {
        String displayName = j2eeName != null ? j2eeName.getComponent() : null; // d696076
        if (displayName == null) {
            displayName = scope.qualifiedName();
        }

        ComponentNameSpaceConfiguration compNSConfig = new ComponentNameSpaceConfiguration(displayName, j2eeName);

        if (compNSConfig.getModuleName() == null) {
            compNSConfig.setModuleDisplayName(scope.qualifiedName()); // d696076
        }
        if (compNSConfig.getApplicationName() == null) {
            compNSConfig.setApplicationDisplayName(scope.qualifiedName()); // d696076
        }

        compNSConfig.setJavaColonContext(javaColonContext);
        compNSConfig.setInjectionClasses(Collections.<Class<?>> emptyList()); // d678836
        compNSConfig.setInjectionProcessorContext(context);

        return compNSConfig;
    }

    /**
     * Populates the empty cookie map with cookies to be injections.
     *
     * @param injectionTargetMap An empty map to be populated with the injection targets from
     *                               the merged xml and annotations.
     * @param compNSConfig       The component configuration information provided by the container.
     * @throws InjectionException if an error occurs processing the injection metadata
     */
    @Override
    public void processInjectionMetaData(HashMap<Class<?>, InjectionTarget[]> injectionTargetMap,
                                         ComponentNameSpaceConfiguration compNSConfig) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processInjectionMetaData", "(targets)");

        InjectionProcessorContextImpl context = createInjectionProcessorContext();
        // F743-31682 - Always bind in the client container code flow.
        context.ivBindNonCompInjectionBindings = compNSConfig.isClientContainer() && compNSConfig.getClassLoader() != null;

        compNSConfig.setInjectionProcessorContext(context);
        processInjectionMetaData(compNSConfig, null);

        List<Class<?>> injectionClasses = compNSConfig.getInjectionClasses();
        if (injectionClasses != null && !injectionClasses.isEmpty()) {
            Map<Class<?>, List<InjectionTarget>> declaredTargets = getDeclaredInjectionTargets(context.ivProcessedInjectionBindings);

            boolean checkAppConfig = compNSConfig.isCheckApplicationConfiguration();
            for (Class<?> injectionClass : injectionClasses) {
                InjectionTarget[] injectionTargets = getInjectionTargets(declaredTargets, injectionClass, checkAppConfig);
                injectionTargetMap.put(injectionClass, injectionTargets);
            }
        }

        context.metadataProcessingComplete(); // F87539
        notifyInjectionMetaDataListeners(null, compNSConfig);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processInjectionMetaData", injectionTargetMap);
    }

    /**
     * Processes injection metadata using the specified configuration with
     * InjectionProcessorContext already set.
     *
     * @param compNSConfig     the component configuration with
     *                             InjectionProcessorContext already set
     * @param annotatedClasses the list of classes that should be processed for
     *                             annotations, or <tt>null</tt> if the list should be determined from {@link ComponentNameSpaceConfiguration#getInjectionClasses}
     */
    protected void processInjectionMetaData(ComponentNameSpaceConfiguration compNSConfig,
                                            List<Class<?>> annotatedClasses) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processInjectionMetaData: " + compNSConfig.toDumpString());

        // Provide the default Naming ObjectFactory implementations if the
        // component has not provided an override.                         d442047
        if (compNSConfig.getIndirectJndiLookupReferenceFactory() == null)
            compNSConfig.setIndirectJndiLookupReferenceFactory(getDefaultIndirectJndiLookupReferenceFactory());
        if (compNSConfig.getResIndirectJndiLookupReferenceFactory() == null)
            compNSConfig.setResIndirectJndiLookupReferenceFactory(getDefaultResIndirectJndiLookupReferenceFactory());
        if (compNSConfig.getResRefReferenceFactory() == null)
            compNSConfig.setResRefReferenceFactory(getDefaultResRefReferenceFactory());
        if (compNSConfig.getResAutoLinkReferenceFactory() == null)
            compNSConfig.setResAutoLinkReferenceFactory(getDefaultResAutoLinkReferenceFactory()); // F48603.9
        if (compNSConfig.getEJBLinkReferenceFactory() == null)
            compNSConfig.setEJBLinkReferenceFactory(getDefaultEJBLinkReferenceFactory());
        if (compNSConfig.getMBLinkReferenceFactory() == null)
            // d703474 - FIXME: we should use a consistent MBLinkReferenceFactory
            // and a per-process MBFactory, but for now, force the injection
            // MBLinkReferenceFactory for client.
            compNSConfig.setMBLinkReferenceFactory(compNSConfig.isClientContainer() ? DEFAULT_MBLinkRefFactory : ivMBLinkRefFactory);

        InjectionProcessorContextImpl context = InjectionProcessorContextImpl.get(compNSConfig);
        context.ivJavaNameSpaceContext = compNSConfig.getJavaColonContext(); // d682474

        List<InjectionProcessorProvider<?, ?>> providers = new ArrayList<InjectionProcessorProvider<?, ?>>(ivProcessorProviders.values());
        InjectionProcessorManager processorManager = new InjectionProcessorManager(this, compNSConfig, context, providers);

        // Extract all the persistence related specification from the deployment
        // description xml.
        processorManager.processXML();

        // Populate the injectionTargetMap with the  the annotation information
        // using the registered processors and instance classes
        if (annotatedClasses == null) {
            if (!compNSConfig.isMetaDataComplete()) {
                annotatedClasses = compNSConfig.getInjectionClasses();
            }
            if (annotatedClasses == null) {
                annotatedClasses = Collections.emptyList();
            }
        }

        if (!annotatedClasses.isEmpty()) {
            ClassLoader loader = compNSConfig.getClassLoader();
            if (loader != null) {
                checkAnnotationClasses(loader); // d676633
            }

            for (Class<?> annotatedClass : annotatedClasses) {
                processorManager.processAnnotations(annotatedClass);
            }
        }

        processorManager.processBindings(); //d500868

        // For federated client modules, collect all the client injection targets
        // and make them available to client processes.
        if (compNSConfig.getOwningFlow() == ComponentNameSpaceConfiguration.ReferenceFlowKind.CLIENT &&
            compNSConfig.getClassLoader() == null) {
            processClientInjections(compNSConfig, context); // F743-33811.1
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processInjectionMetaData");
    }

    /**
     * Check the specified class loader to check if it has overridden any
     * annotation classes processed by the injection engine.
     */
    private void checkAnnotationClasses(ClassLoader loader) {
        boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.entry(tc, "checkAnnotationClasses: " + loader);

        // All EJBs in an application share the same class loader.  Only check
        // a given class loader once.
        if (ivCheckedAnnotationClassLoaders.add(loader)) {
            for (Class<?> processorClass : ivProcessorProviders.keySet()) {
                String className = processorClass.getName();

                Class<?> loaderClass;
                try {
                    loaderClass = loader.loadClass(className);
                } catch (ClassNotFoundException ex) {
                    // At least it hasn't been overridden, so ignore per WAB.  F53641
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "ignoring " + className + " : " + ex);
                    continue;
                }

                if (loaderClass != processorClass) {
                    Set<String> warnedAnnotations = null;
                    ClassLoader loaderClassLoader = loaderClass.getClassLoader();

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "loaded " + loaderClass + " from application class loader " + loaderClassLoader);

                    synchronized (this) {
                        if (ivWarnedClassLoaderAnnotations == null) {
                            ivWarnedClassLoaderAnnotations = new WeakHashMap<ClassLoader, Set<String>>();
                        }

                        warnedAnnotations = ivWarnedClassLoaderAnnotations.get(loaderClassLoader);
                        if (warnedAnnotations == null) {
                            warnedAnnotations = Collections.synchronizedSet(new HashSet<String>());
                            ivWarnedClassLoaderAnnotations.put(loaderClassLoader, warnedAnnotations);
                        }
                    }

                    // Only warn once about a particular class loader loading a
                    // particular annotation class.  We do not want a PARENT_LAST
                    // application to generate warnings for every child WAR class
                    // loader.  Similarly for application server class loaders.
                    if (warnedAnnotations == null || warnedAnnotations.add(className)) {
                        CodeSource codeSource = loaderClass.getProtectionDomain().getCodeSource();
                        String codeSourceLocation = codeSource == null ? null : String.valueOf(codeSource.getLocation());

                        Tr.warning(tc, "INCOMPATIBLE_ANNOTATION_CLASS_CWNEN0070W",
                                   className, codeSourceLocation);
                    }
                }
            }
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.exit(tc, "checkAnnotationClasses");
    }

    protected abstract void processClientInjections(ComponentNameSpaceConfiguration compNSConfig,
                                                    InjectionProcessorContextImpl context) throws InjectionException;

    /**
     * Creates a map of class to injection targets for fields and methods
     * declared in that class only (not including injection targets for fields
     * and methods in superclasses).
     *
     * @param resolvedInjectionBindings the resolved injection bindings
     * @see #getInjectionTargets
     */
    // d719917
    Map<Class<?>, List<InjectionTarget>> getDeclaredInjectionTargets(List<InjectionBinding<?>> resolvedInjectionBindings) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getDeclaredInjectionTargets");

        Map<Class<?>, List<InjectionTarget>> declaredTargets = new HashMap<Class<?>, List<InjectionTarget>>();

        // First, collect declared injection targets on a per-class basis.
        for (InjectionBinding<?> injectionBinding : resolvedInjectionBindings) {
            if (!injectionBinding.isResolved()) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "skipping unresolved " + injectionBinding);
                continue;
            }

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "adding targets for " +
                             Util.identity(injectionBinding) + '[' + injectionBinding.getDisplayName() + ']');

            List<InjectionTarget> injectionTargets = InjectionProcessorContextImpl.getInjectionTargets(injectionBinding);
            if (injectionTargets != null) {
                for (InjectionTarget target : injectionTargets) {
                    Member member = target.getMember();
                    Class<?> memberClass = member.getDeclaringClass();

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "adding " + member);

                    List<InjectionTarget> classTargets = declaredTargets.get(memberClass);
                    if (classTargets == null) {
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "creating list for " + memberClass + "/" + AccessController.doPrivileged(new GetClassLoaderPrivileged(memberClass)));

                        classTargets = new ArrayList<InjectionTarget>();
                        declaredTargets.put(memberClass, classTargets);
                    }

                    classTargets.add(target);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getDeclaredInjectionTargets");
        return declaredTargets;
    }

    /**
     * Determines the set of injection targets applicable to a class hierarchy
     * from a map of declared injection targets.
     *
     * @param declaredTargets the result of {@link #getDeclaredInjectionTargets}
     * @param instanceClass   the class to which injection targets should apply
     * @param checkAppConfig  true if application configuration should be checked
     * @return the applicable injection targets
     * @throws InjectionException
     */
    // F50309.4
    InjectionTarget[] getInjectionTargets(Map<Class<?>, List<InjectionTarget>> declaredTargets,
                                          Class<?> instanceClass,
                                          boolean checkAppConfig) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getInjectionTargets: " + instanceClass + "/" + AccessController.doPrivileged(new GetClassLoaderPrivileged(instanceClass)));

        // d622258.1 - A field or method should only be injected once.  If
        // multiple annotations (or injection-target) are applied to the same
        // member, then we will instantiate multiple objects and call setters
        // multiple times.  The last injection will "win", but the order is
        // undefined.  To warn the user, we maintain a map of members to targets.
        Map<Member, InjectionTarget> injectionTargets = null;

        // Lazily initialized set of non-private, non-overridden methods in the
        // class hierarchy.                                                d719917
        Set<Method> nonPrivateMethods = null;

        // Create a list of classes in the hierarchy from Object to superclass
        // to instanceClass.  This ensures that we inject into superclass methods
        // before subclass methods.                                        d719917
        List<Class<?>> classHierarchy = new ArrayList<Class<?>>();
        for (Class<?> superclass = instanceClass; superclass != null && superclass != Object.class; // d721081
                        superclass = superclass.getSuperclass()) {
            classHierarchy.add(superclass);
        }
        Collections.reverse(classHierarchy);

        // Fields must be injected before methods, so we make two passes over
        // the injection targets: first fields, then methods.
        for (int memberRound = 0; memberRound < 2; memberRound++) {
            boolean wantFields = memberRound == 0;

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, wantFields ? "collecting fields" : "collecting methods");

            for (Class<?> superclass : classHierarchy) {
                List<InjectionTarget> classTargets = declaredTargets.get(superclass);
                if (classTargets == null) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "no members for " + superclass + "/" + AccessController.doPrivileged(new GetClassLoaderPrivileged(superclass)));
                    continue;
                }

                for (InjectionTarget injectionTarget : classTargets) {
                    Member member = injectionTarget.getMember();

                    boolean isField = member instanceof Field;
                    if (wantFields != isField) {
                        continue;
                    }

                    // Only consider fields and non-overridden methods.  Private
                    // methods and methods on the class itself are trivially not
                    // overridden.                                            d719917
                    if (!isField &&
                        !Modifier.isPrivate(member.getModifiers()) &&
                        member.getDeclaringClass() != instanceClass) {
                        if (nonPrivateMethods == null) {
                            // Compute the set of "overriding" non-private methods in
                            // the hierarchy.
                            nonPrivateMethods = new HashSet<Method>();
                            for (MethodMap.MethodInfo methodInfo : MethodMap.getAllNonPrivateMethods(instanceClass)) {
                                nonPrivateMethods.add(methodInfo.getMethod());
                            }
                        }

                        if (!nonPrivateMethods.contains(member)) {
                            // The method was overridden.
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "skipping overridden " + member + " for " +
                                             Util.identity(injectionTarget.getInjectionBinding()) +
                                             '[' + injectionTarget.getInjectionBinding().getDisplayName() + ']');
                            continue;
                        }
                    }

                    if (injectionTargets == null) {
                        injectionTargets = new LinkedHashMap<Member, InjectionTarget>();
                    }

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "adding " + member + " for " +
                                     Util.identity(injectionTarget.getInjectionBinding()) +
                                     '[' + injectionTarget.getInjectionBinding().getDisplayName() + ']');

                    InjectionTarget previousTarget = injectionTargets.put(member, injectionTarget);

                    if (previousTarget != null) {
                        String memberName = member.getDeclaringClass().getName() + "." + member.getName();
                        Tr.warning(tc, "DUPLICATE_INJECTION_TARGETS_SPECIFIED_CWNEN0040W", memberName); //d447011
                        if (isValidationFailable(checkAppConfig)) {
                            memberName += isField ? " field" : " method";
                            String curRefName = injectionTarget.getInjectionBinding().getDisplayName();
                            String preRefName = previousTarget.getInjectionBinding().getDisplayName();
                            throw new InjectionConfigurationException("The " + memberName + " was configured to be injected multiple times." +
                                                                      " The same injection target is associated with both the " + curRefName +
                                                                      " and " + preRefName + " references. The object injected is undefined.");
                        }
                    }
                }
            }
        }

        InjectionTarget[] result;
        if (injectionTargets == null) {
            result = EMPTY_INJECTION_TARGETS;
        } else {
            result = new InjectionTarget[injectionTargets.size()];
            injectionTargets.values().toArray(result);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getInjectionTargets: " + result.length);
        return result;
    }

    /**
     * This method handles the actual injection of injectedObject to the target beanObject using
     * either the METHOD or FIELD specified in the injectionTarget.
     */
    @Override
    public void inject(Object objectToInject,
                       InjectionTarget injectionTarget) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "inject", objectToInject, injectionTarget);

        injectionTarget.inject(objectToInject, null);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "inject");
    }

    /*
     * Implementation of InjectionEngine interface method.
     */// F49213.1
    @Override
    public void inject(Object objectToInject,
                       InjectionTarget target,
                       InjectionTargetContext targetContext) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "inject " + "(" + Util.identity(objectToInject) +
                         ", " + target + ", " + targetContext + ")");

        target.inject(objectToInject, targetContext);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "inject");
    }

    // d643203
    /**
     * Invokes all registered {@link InjectionMetaDataListener}s.
     */
    @Override
    public void notifyInjectionMetaDataListeners(ReferenceContext referenceContext,
                                                 ComponentNameSpaceConfiguration compNSpaceConfiguration) throws InjectionException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "notifyInjectionMetaDataListeners");

        InjectionMetaData injectionMetaData = new InjectionMetaDataImpl(this, compNSpaceConfiguration, referenceContext); // F48603

        for (InjectionMetaDataListener metaDataListener : metaDataListeners) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "invoking InjectionMetaDataListener: " + metaDataListener);
            metaDataListener.injectionMetaDataCreated(injectionMetaData);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "notifyInjectionMetaDataListeners");
    }

    /**
     * This method will register an instance of an InjectionMetaDataListener with the current
     * engine instance.
     */
    @Override
    public void registerInjectionMetaDataListener(InjectionMetaDataListener metaDataListener) {
        if (metaDataListener == null) {
            throw new IllegalArgumentException("A null InjectionMetaDataListener cannot be registered " +
                                               "with the injection engine.");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "registerInjectionMetaDataListener", metaDataListener.getClass().getName());
        metaDataListeners.add(metaDataListener);
    } //492391.2 end

    /**
     * This method will unregister an instance of an InjectionMetaDataListener with the current
     * engine instance.
     */
    @Override
    public void unregisterInjectionMetaDataListener(InjectionMetaDataListener metaDataListener) {
        if (metaDataListener == null) {
            throw new IllegalArgumentException("A null InjectionMetaDataListener cannot be unregistered " +
                                               "from the injection engine.");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "unregisterInjectionMetaDataListener", metaDataListener.getClass().getName());
        metaDataListeners.remove(metaDataListener);
    } // D96791

    /** {@inheritDoc} */
    @Override
    public void registerObjectFactory(Class<? extends Annotation> annotation,
                                      Class<?> type,
                                      Class<? extends ObjectFactory> objectFactory,
                                      boolean allowOverride) throws InjectionException {
        registerObjectFactory(annotation, type, objectFactory, allowOverride, null, true);
    }

    /** {@inheritDoc} */
    @Override
    public void registerObjectFactory(Class<? extends Annotation> annotation,
                                      Class<?> type,
                                      Class<? extends ObjectFactory> objectFactory,
                                      boolean allowOverride,
                                      Set<String> allowedAttributes,
                                      boolean refAddrNeeded) throws InjectionException {
        registerObjectFactoryInfo(new ObjectFactoryInfoImpl(annotation, type, objectFactory, allowOverride, allowedAttributes, refAddrNeeded));
    }

    @Override
    public void registerObjectFactoryInfo(ObjectFactoryInfo info) throws InjectionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "registerObjectFactoryInfo: " +
                         info.getAnnotationClass() + " : " + info.getType() +
                         " (override=" + info.isOverrideAllowed() + ") = " + info);

        updateObjectFactoryInfo(info, true);
    }

    @Override
    public void unregisterObjectFactoryInfo(ObjectFactoryInfo info) throws InjectionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "unregisterObjectFactoryInfo: " +
                         info.getAnnotationClass() + " : " + info.getType() +
                         " (override=" + info.isOverrideAllowed() + ") = " + info);

        updateObjectFactoryInfo(info, false);
    }

    private void updateObjectFactoryInfo(ObjectFactoryInfo info, boolean register) throws InjectionException {
        if (!ivIsInitialized) {
            Tr.error(tc, "INJECTION_ENGINE_SERVICE_NOT_INITIALIZED_CWNEN0006E");
            throw new InjectionException("injection engine is not initialized");
        }

        Class<? extends Annotation> annotation = info.getAnnotationClass();
        Class<?> type = info.getType();
        Class<?> objectFactory = info.getObjectFactoryClass();

        if (annotation == null ||
            type == null ||
            objectFactory == null) {
            throw new IllegalArgumentException("Null arguments are not allowed: " +
                                               annotation + ", " + type + ", " + objectFactory);
        }

        // There are two maps of ObjectFactories.... one that does not support
        // binding overrides and one that does.  Select the correct map based
        // on the 'allowOverride' parameter.                            F623-841.1
        boolean allowOverride = info.isOverrideAllowed();

        // Now add the new ObjectFactory to the map, per annotation and data type.
        synchronized (this) {
            Map<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>> objectFactoryMap = allowOverride ? ivObjectFactoryMap : ivNoOverrideObjectFactoryMap;

            boolean copyOnWrite = allowOverride ? ivObjectFactoryMapCopyOnWrite : ivNoOverrideObjectFactoryMapCopyOnWrite;
            if (copyOnWrite) {
                Map<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>> newMap = new HashMap<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>>();
                for (Map.Entry<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>> entry : objectFactoryMap.entrySet()) {
                    newMap.put(entry.getKey(), new HashMap<Class<?>, ObjectFactoryInfo>(entry.getValue()));
                }

                objectFactoryMap = newMap;
            }

            Map<Class<?>, ObjectFactoryInfo> factories = objectFactoryMap.get(annotation);

            if (factories == null) {
                if (!register) {
                    throw new InjectionException("Object factory " + objectFactory.getName() +
                                                 " not registered for the " + annotation.getName() +
                                                 " annotation and the " + type.getName() + " type.");
                }

                if (!ivProcessorProviders.containsKey(annotation)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "registerObjectFactory: An injection processor does not exist for the specified annotation: " +
                                     annotation.getName() + ". Object factory " + objectFactory.getName() + " not registered for the " + type.getName() + " type.");
                    throw new InjectionException("An injection processor does not exist for the specified annotation: " +
                                                 annotation.getName() + ". Object factory " + objectFactory.getName() + " not registered for the " + type.getName() + " type.");
                }

                factories = new HashMap<Class<?>, ObjectFactoryInfo>();
                objectFactoryMap.put(annotation, factories);
            }

            if (register) {
                factories.put(type, info); // d675976
            } else {
                if (factories.get(type) != info) {
                    throw new InjectionException("Object factory " + objectFactory.getName() +
                                                 " not registered for the " + annotation.getName() +
                                                 " annotation and the " + type.getName() + " type.");
                }

                factories.remove(type);
            }

            if (copyOnWrite) {
                // Replace exiting map after all updates have been made.
                if (allowOverride) {
                    ivObjectFactoryMap = objectFactoryMap;
                    ivObjectFactoryMapCopyOnWrite = false; // PM79779
                } else {
                    ivNoOverrideObjectFactoryMap = objectFactoryMap;
                    ivNoOverrideObjectFactoryMapCopyOnWrite = false; // PM79779
                }
            }
        }
    }

    /**
     * Provides a mechanism to override the algorithm used to identify
     * the target of a reference defined for the application component's
     * environment (java:comp/env). <p>
     *
     * For those processors that support override reference factories,
     * the registered reference factory will be invoked to create a naming
     * Reference that will be bound into the java:comp/env name space and
     * used to obtain an instance of the object that represents the target
     * of the specified component reference. <p>
     *
     * If the registered override reference factory does not wish to override
     * the component reference, it should return null; and normal processing
     * will be performed as if the override reference factory did not exist. <p>
     *
     * @param annotation the type of annotation processor the factory is to
     *                       be registered with.
     * @param factory    thread safe instance of an override reference factory.
     *
     * @throws InjectionException       if an injection processor has not been
     *                                      registered for the specified annotation.
     * @throws IllegalArgumentException if any of the parameters are null.
     **/
    // F1339-9050
    @Override
    public <A extends Annotation> void registerOverrideReferenceFactory(Class<A> annotation,
                                                                        OverrideReferenceFactory<A> factory) throws InjectionException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "registerOverrideReferenceFactory", annotation, factory);

        if (annotation == null ||
            factory == null) {
            throw new IllegalArgumentException("Null arguments are not allowed: " +
                                               annotation + ", " + factory);
        }

        // Now add the new factory to the map, per annotation.
        synchronized (this) {
            HashMap<Class<? extends Annotation>, OverrideReferenceFactory<?>[]> map = ivOverrideReferenceFactoryMap;

            if (ivOverrideReferenceFactoryMapCopyOnWrite) {
                HashMap<Class<? extends Annotation>, OverrideReferenceFactory<?>[]> newMap = new HashMap<Class<? extends Annotation>, OverrideReferenceFactory<?>[]>();
                for (Map.Entry<Class<? extends Annotation>, OverrideReferenceFactory<?>[]> entry : map.entrySet()) {
                    OverrideReferenceFactory<?>[] value = entry.getValue();
                    OverrideReferenceFactory<?>[] newValue = new OverrideReferenceFactory[value.length];
                    System.arraycopy(value, 0, newValue, 0, value.length);
                    newMap.put(entry.getKey(), newValue);
                }

                map = newMap;
            }

            OverrideReferenceFactory<?>[] factories = map.get(annotation);

            if (factories == null) {
                if (!ivProcessorProviders.containsKey(annotation)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "registerOverrideReferenceFactory: An injection processor does not exist for the specified annotation: " +
                                     annotation.getName() + ". Factory " + factory.getClass().getName() + " not registered.");
                    throw new InjectionException("An injection processor does not exist for the specified annotation: " +
                                                 annotation.getName() + ". Factory " + factory.getClass().getName() + " not registered.");
                }

                factories = new OverrideReferenceFactory[1];
                factories[0] = factory;

                map.put(annotation, factories);
            } else {
                OverrideReferenceFactory<?>[] newFactories = new OverrideReferenceFactory[factories.length + 1];
                System.arraycopy(factories, 0, newFactories, 0, factories.length);
                newFactories[factories.length] = factory;

                map.put(annotation, newFactories);
            }

            if (ivOverrideReferenceFactoryMapCopyOnWrite) {
                // Replace exiting map after all updates have been made.
                ivOverrideReferenceFactoryMap = map;
                ivOverrideReferenceFactoryMapCopyOnWrite = false;
            }
        }
    }

    /**
     * Gets the list of override factories for the specified class.
     *
     * @param klass the class
     * @return the list of override factories
     */
    @Override
    @SuppressWarnings("unchecked")
    public synchronized <A extends Annotation> OverrideReferenceFactory<A>[] getOverrideReferenceFactories(Class<A> klass) {
        ivOverrideReferenceFactoryMapCopyOnWrite = true; // PM79779
        return (OverrideReferenceFactory<A>[]) ivOverrideReferenceFactoryMap.get(klass);
    }

    @Override
    public ObjectFactory getObjectFactory(String objectFactoryClassName,
                                          Class<? extends ObjectFactory> objectFactoryClass) throws InjectionException {
        try {
            if (objectFactoryClass == null) {
                ClassLoader classLoader = svThreadContextAccessor.getContextClassLoaderForUnprivileged(Thread.currentThread());
                objectFactoryClass = classLoader.loadClass(objectFactoryClassName).asSubclass(ObjectFactory.class);
            }

            return objectFactoryClass.newInstance();
        } catch (Throwable ex) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getInjectionObjectInstance", ex);
            Tr.error(tc, "OBJECT_FACTORY_CLASS_FAILED_TO_LOAD_CWNEN0024E", objectFactoryClassName);
            throw new InjectionException(ex.toString(), ex);
        }
    }

    /**
     * Provides a mechanism to override the default managed bean reference
     * factory for controlling managed bean auto-link behavior. <p>
     *
     * Returns the current default managed bean reference factory. <p>
     *
     * @param mbLinkRefFactory new managed bean reference factory class.
     *
     * @return the current default managed bean reference factory.
     **/
    // d698540.1
    @Override
    public MBLinkReferenceFactory registerManagedBeanReferenceFactory(MBLinkReferenceFactory mbLinkRefFactory) {
        MBLinkReferenceFactory rtnFactory = DEFAULT_MBLinkRefFactory;
        ivMBLinkRefFactory = mbLinkRefFactory; // d703474
        return rtnFactory;
    }

    // F743-17630
    /**
     * Creates new <code>ReferenceContext</code> instance.
     *
     * Does not attempt to re-use a shared <code>ReferenceContext</code> from
     * the <code>DeployedModule</code>, nor does it attach the newly created
     * <code>ReferenceContext</code> to the <code>DeployedModule</code>.
     */
    @Override
    public ReferenceContext createReferenceContext() {
        return new ReferenceContextImpl(this); // F743-33811.1
    }

    /**
     * Emit the customized human readable text to represent this object
     * in an FFDC incident report.
     *
     * @param is the incident stream, the data will be written here
     */
    // F49213
    @Override
    public void formatTo(IncidentStream is) {
        is.writeLine("", "");

        // -----------------------------------------------------------------------
        // Indicate the start of the dump, and include the toString()
        // of EJSContainer, so this can easily be matched to a trace.
        // -----------------------------------------------------------------------
        is.writeLine("", "*** Start InjectionEngine Dump    ---> " + Util.identity(this));

        is.writeLine("", "");
        is.writeLine("", "   Default Factories : ");
        is.writeLine("", "      Indirect    = " + Util.identity(getDefaultIndirectJndiLookupReferenceFactory()));
        is.writeLine("", "      ResIndirect = " + Util.identity(getDefaultResIndirectJndiLookupReferenceFactory()));
        is.writeLine("", "      ResRef      = " + Util.identity(getDefaultResRefReferenceFactory()));
        is.writeLine("", "      ResAuto     = " + Util.identity(getDefaultResAutoLinkReferenceFactory()));
        is.writeLine("", "      EJBLink     = " + Util.identity(getDefaultEJBLinkReferenceFactory()));
        is.writeLine("", "      MBLink      = " + Util.identity(DEFAULT_MBLinkRefFactory));

        is.writeLine("", "");
        is.writeLine("", "   Actual Factories : ");
        is.writeLine("", "      " + Util.identity(ivMBLinkRefFactory));

        is.writeLine("", "");
        is.writeLine("", "   Registered Processors : ");
        for (Map.Entry<Class<?>, ?> entry : ivProcessorProviders.entrySet()) {
            is.writeLine("", "      " + entry.getKey().getName() +
                             " : " + entry.getValue());
        }

        // Synchronize access to the factory maps, to prevent them from changing
        // while their state is being logged, but the CopyOnWrite flags don't
        // need to be set since the maps, nor any of their state will be
        // referenced beyond this synchronized block.                      PM79779
        synchronized (this) {
            is.writeLine("", "");
            is.writeLine("", "   Registered Object Factories : ");
            for (Map.Entry<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>> entry : ivObjectFactoryMap.entrySet()) {
                is.writeLine("", "      " + entry.getKey().getName());
                Map<Class<?>, ObjectFactoryInfo> factories = entry.getValue();
                for (Map.Entry<Class<?>, ObjectFactoryInfo> factoryEntry : factories.entrySet()) {
                    is.writeLine("", "         " + factoryEntry.getKey().getName() + " : " + factoryEntry.getValue());
                }
            }

            is.writeLine("", "");
            is.writeLine("", "   Registered No-Override Object Factories : ");
            for (Map.Entry<Class<? extends Annotation>, Map<Class<?>, ObjectFactoryInfo>> entry : ivNoOverrideObjectFactoryMap.entrySet()) {
                is.writeLine("", "      " + entry.getKey().getName());
                Map<Class<?>, ObjectFactoryInfo> factories = entry.getValue();
                for (Map.Entry<Class<?>, ObjectFactoryInfo> factoryEntry : factories.entrySet()) {
                    is.writeLine("", "         " + factoryEntry.getKey().getName() + " : " + factoryEntry.getValue());
                }
            }

            is.writeLine("", "");
            is.writeLine("", "   Registered Override Reference Factories : ");
            for (Map.Entry<Class<? extends Annotation>, OverrideReferenceFactory<?>[]> entry : ivOverrideReferenceFactoryMap.entrySet()) {
                is.writeLine("", "      " + entry.getKey().getName());
                for (OverrideReferenceFactory<?> factory : entry.getValue()) {
                    is.writeLine("", "         " + Util.identity(factory));
                }
            }
        }

        is.writeLine("", "");
        is.writeLine("", "   Registered MetaDataListeners : ");
        for (InjectionMetaDataListener listener : metaDataListeners) {
            is.writeLine("", "      " + Util.identity(listener));
        }
        is.writeLine("", "");
        is.writeLine("", "   isEmbeddable    : " + this.isEmbeddable());

        is.writeLine("", "");
        is.writeLine("", "*** InjectionEngine Dump Complete ***");
        is.writeLine("", "");
    }
}
