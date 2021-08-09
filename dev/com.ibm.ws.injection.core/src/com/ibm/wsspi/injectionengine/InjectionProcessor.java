/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.IncidentStream;
import com.ibm.ws.injectionengine.ffdc.Formattable;
import com.ibm.wsspi.injectionengine.factory.OverrideReferenceFactory;

/**
 * This base class provides generic processing to handle annotations defined in the target class
 */
public abstract class InjectionProcessor<A extends Annotation, AS extends Annotation> implements Formattable
{
    private static final TraceComponent tc = Tr.register(InjectionProcessor.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    //Single Annotation
    private final Class<A> ivAnnotationClass; //LIDB3294-35.1

    //Plural Annotation - see the additional "s" in the name
    final Class<AS> ivAnnotationsClass; //LIDB3294-35.1

    private InjectionProcessorContext ivContext; // F743-33811.1

    protected ComponentNameSpaceConfiguration ivNameSpaceConfig;

    /**
     * The processor that is overriding this one, or null if no override.
     */
    // RTC114863
    InjectionProcessor<? extends Annotation, ? extends Annotation> ivOverrideProcessor;

    /**
     * Map of data type to ObjectFactory. These are ObjectFactories that have
     * been registered to extend the data types supported by the base processor
     * implementation. When an annotation (or XML) is applied to the specified
     * data type, then the corresponding ObjectFactory should be used.
     **/
    // F623-841
    private Map<Class<?>, ObjectFactoryInfo> ivObjectFactoryMap;

    /**
     * This field is lazily initialized from the contents of ivObjectFactoryMap
     * for processors that need to find object factories by class name rather
     * than by class object, such as when the class loader is not specified.
     *
     * @see #getObjectFactory
     */
    private Map<String, ObjectFactoryInfo> ivObjectFactoryByNameMap; // F743-32443

    /**
     * Map of data type to ObjectFactory. These are ObjectFactories that have
     * been registered to extend the data types supported by the base processor
     * implementation and do NOT support being overridden with a binding. When
     * an annotation (or XML) is applied to the specified data type, then the
     * corresponding ObjectFactory should be used, regardless of whether a
     * binding is present or not.
     **/
    // F623-841.1
    private Map<Class<?>, ObjectFactoryInfo> ivNoOverrideObjectFactoryMap;

    /**
     * This field is lazily initialized from the contents of
     * ivNoOverrideObjectFactoryMap for processors that need to find object
     * factories by class name rather than by class object, such as when the
     * class loader is not specified.
     *
     * @see #getNoOverrideObjectFactory
     */
    private Map<String, ObjectFactoryInfo> ivNoOverrideObjectFactoryByNameMap; // F743-32443

    /**
     * ArrayList of OverrideReferenceFactory instances that have been
     * registered for this Injection Processor. Before performing normal
     * 'resolve' processing, all OverrideReferenceFactory instances should
     * be given an opportunity to override the reference, until one elects
     * to provide an override.
     **/
    // F1339-9050
    protected ArrayList<OverrideReferenceFactory<A>> ivOverrideReferenceFactories;

    /**
     * Map of JNDI names to injection bindings.
     */
    protected final Map<String, InjectionBinding<A>> ivAllAnnotationsCollection = new BindingMap(); // d367834.14

    /**
     * A list of InjectionBinding that were created because the annotation for
     * this processor does not support JNDI names.
     */
    // F50309.5 PM88594
    private Map<Member, InjectionBinding<A>> ivSimpleInjectionBindings;

    /**
     * List of resources that could not be matched up to a binding from the binding file.
     * this list is meant to up updated by all subclasses of InjectionProcessor.
     **/
    protected ArrayList<String> ivMissingBindings = new ArrayList<String>(); //d435329

    /**
     * Initialize a processor with annotation classes. An instance of each of
     * the registered subclasses will be created for every component that
     * requires processing. <p>
     *
     * After creating an instance, the injection engine will call {@link #initProcessor()} to complete processor initialization. <p>
     *
     * @param annotationClass the singular annotation
     * @param annotationsClass the plural annotation
     */
    public InjectionProcessor(Class<A> annotationClass, Class<AS> annotationsClass)
    {
        ivAnnotationClass = annotationClass;
        ivAnnotationsClass = annotationsClass;
    }

    /**
     * Internal subclass of HashMap that overrides the 'get' method to support
     * the fact that reference names may optionally include 'java:comp/env/".
     *
     * The 'short' name will always be used as the key into the map,
     * and overriding get allows a lookup with either the full or short name
     * to work properly. This supports configurations where the same reference
     * is declared in multiple places, but some with the short, and some with
     * the full reference name.
     */
    // d62985.2
    class BindingMap extends LinkedHashMap<String, InjectionBinding<A>>
    {
        private static final long serialVersionUID = 1483058422051408034L;

        @Override
        public InjectionBinding<A> put(String key, InjectionBinding<A> value)
        {
            value.ivProcessorClass = InjectionProcessor.this.getClass();
            return super.put(key, value);
        }

        @Override
        public InjectionBinding<A> get(Object key)
        {
            String jndiName = (String) key;
            return super.get(InjectionScope.normalize(jndiName)); // d726563
        }
    }

    void initProcessor(ComponentNameSpaceConfiguration compNSConfig, InjectionProcessorContext context)
                    throws InjectionException
    {
        ivNameSpaceConfig = compNSConfig;
        ivContext = context;

        ivObjectFactoryMap = ivContext.ivObjectFactoryMap.get(ivAnnotationClass);
        ivNoOverrideObjectFactoryMap = ivContext.ivNoOverrideObjectFactoryMap.get(ivAnnotationClass); // F623-841.1

        @SuppressWarnings("unchecked")
        OverrideReferenceFactory<A>[] factories =
                        OverrideReferenceFactory[].class.cast(ivContext.ivOverrideReferenceFactoryMap.get(ivAnnotationClass));

        if (factories != null)
        {
            J2EEName j2eeName = ivNameSpaceConfig.getJ2EEName();
            if (j2eeName != null) // d696076
            {
                // For this processor instance, add only those factories
                // that report an interest for the current app/module
                String appName = j2eeName.getApplication();
                String modName = j2eeName.getModule();
                for (OverrideReferenceFactory<A> factory : factories)
                {
                    if (factory.hasModuleOverride(appName, modName))
                    {
                        if (ivOverrideReferenceFactories == null)
                        {
                            ivOverrideReferenceFactories = new ArrayList<OverrideReferenceFactory<A>>();
                        }
                        ivOverrideReferenceFactories.add(factory);
                    }
                }
            }
        }

        // F743-33811.2 - Prepopulate this processor instance with the saved
        // injection bindings from previous embeddable components.
        if (!ivContext.ivSaveNonCompInjectionBindings)
        {
            addSavedInjectionBindings(ivContext.ivSavedGlobalInjectionBindings);
            addSavedInjectionBindings(ivContext.ivSavedAppInjectionBindings);
            addSavedInjectionBindings(ivContext.ivSavedModuleInjectionBindings);
        }

        initProcessor();
    }

    /**
     * Merge and add saved non-java:comp injection bindings.
     */
    private void addSavedInjectionBindings(Map<Class<?>, Map<String, InjectionBinding<?>>> savedInjectionBindings)
                    throws InjectionException
    {
        if (savedInjectionBindings != null)
        {
            Map<String, InjectionBinding<A>> injectionBindingsMap = getSavedInjectionBindings(savedInjectionBindings, false);
            if (injectionBindingsMap != null)
            {
                ivAllAnnotationsCollection.putAll(injectionBindingsMap);
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Map<String, InjectionBinding<A>> getSavedInjectionBindings(Map<Class<?>, Map<String, InjectionBinding<?>>> savedInjectionBindings,
                                                                       boolean create)
    {
        Map result = savedInjectionBindings.get(getClass());
        if (result == null && create)
        {
            result = new HashMap();
            savedInjectionBindings.put(getClass(), result);
        }

        return result;
    }

    /**
     * This method is called after instantiation but before any other methods
     * are called on this processor.
     *
     * @throws InjectionException
     */
    protected void initProcessor()
                    throws InjectionException
    {
        // Intentionally blank.... for use by subclasses.
    }

    /**
     * Gets an object factory for the specified class or class name. This
     * method must be used to support configurations without a class loader.
     *
     * @param klass the class or <tt>null</tt> if unavailable
     * @param className the class name or <tt>null</tt> if klass was specified
     * @return the ObjectFactory
     */
    protected final ObjectFactoryInfo getObjectFactoryInfo(Class<?> klass, String className) // F743-32443
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectFactory: " + klass + ", " + className);

        if (ivObjectFactoryMap == null)
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectFactory: no factories");
            return null;
        }

        if (klass != null && klass != Object.class) // d700708
        {
            ObjectFactoryInfo result = ivObjectFactoryMap.get(klass);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectFactory: " + result);
            return result;
        }

        if (ivObjectFactoryByNameMap == null)
        {
            ivObjectFactoryByNameMap = new HashMap<String, ObjectFactoryInfo>();
            for (Map.Entry<Class<?>, ObjectFactoryInfo> entry : ivObjectFactoryMap.entrySet())
            {
                ivObjectFactoryByNameMap.put(entry.getKey().getName(), entry.getValue());
            }
        }

        ObjectFactoryInfo result = ivObjectFactoryByNameMap.get(className);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectFactory (by-name): " + result);
        return result;
    }

    /**
     * Gets an object factory that cannot be overridden by bindings for the
     * specified class or class name. This method must be used to support
     * configurations without a class loader.
     *
     * @param klass the class or <tt>null</tt> if unavailable
     * @param className the class name or <tt>null</tt> if klass was specified
     * @return the ObjectFactory
     */
    protected final ObjectFactoryInfo getNoOverrideObjectFactory(Class<?> klass, String className) // F743-32443
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getNoOverrideObjectFactory: " + klass + ", " + className);

        if (ivNoOverrideObjectFactoryMap == null)
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getNoOverrideObjectFactory: no factories");
            return null;
        }

        if (klass != null && klass != Object.class) // d700708
        {
            ObjectFactoryInfo result = ivNoOverrideObjectFactoryMap.get(klass);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getNoOverrideObjectFactory: " + result);
            return result;
        }

        if (ivNoOverrideObjectFactoryByNameMap == null)
        {
            ivNoOverrideObjectFactoryByNameMap = new HashMap<String, ObjectFactoryInfo>();
            for (Map.Entry<Class<?>, ObjectFactoryInfo> entry : ivNoOverrideObjectFactoryMap.entrySet())
            {
                ivNoOverrideObjectFactoryByNameMap.put(entry.getKey().getName(), entry.getValue());
            }
        }

        ObjectFactoryInfo result = ivNoOverrideObjectFactoryByNameMap.get(className);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectFactory (by-name): " + result);
        return result;
    }

    /**
     * Updates an InjectionBinding created for an annotation.
     */
    private void updateInjectionBinding(String jndiName, InjectionBinding<A> injectionBinding) // d675172
    {
        // If the annotation didn't have a name or the binding implementation
        // constructor did not set it, then the binding needs to be updated
        // with the default name or name (if not set by constructor).   // d682415
        if (!jndiName.equals(injectionBinding.getJndiName())) // d675172.1
        {
            injectionBinding.setJndiName(jndiName);
        }
    }

    /**
     * Gets an injection binding by name. This will either return a previously
     * created injection binding from this injection processing, a completed
     * injection binding from a previous injection processing, or null if the
     * injection binding cannot be found.
     *
     * @param jndiName the JNDI name
     * @return the possibly completed injection binding.
     */
    // d730349
    private InjectionBinding<A> getInjectionBindingForAnnotation(String jndiName)
    {
        InjectionBinding<A> injectionBinding = ivAllAnnotationsCollection.get(jndiName);
        if (injectionBinding == null && ivContext.ivCompletedInjectionBindings != null)
        {
            InjectionBinding<?> completedInjectionBinding = ivContext.ivCompletedInjectionBindings.get(jndiName);
            if (completedInjectionBinding != null && completedInjectionBinding.ivProcessorClass == getClass())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "reusing completed " + completedInjectionBinding.toSimpleString());

                @SuppressWarnings("unchecked")
                InjectionBinding<A> uncheckedInjectionBinding = (InjectionBinding<A>) completedInjectionBinding;
                injectionBinding = uncheckedInjectionBinding;
                injectionBinding.metadataProcessingInitialize(ivNameSpaceConfig); // d730349.1
                ivAllAnnotationsCollection.put(jndiName, injectionBinding);
            }
        }

        return injectionBinding;
    }

    /**
     * Creates and adds a new override injection binding, or merges the data in
     * an annotation with an existing override injection binding.
     *
     * @param <P> the primary annotation class; from the perspective of this
     *            method, the A type variable is the overridden annotation type
     * @param instanceClass the class that contained the annotation
     * @param member the member in class that contained the annotation, or
     *            <tt>null</tt> if the annotation was found at the class level
     * @param annotation the override annotation data
     * @return true if the override injection binding was created or merged, or
     *         false if the annotation was not overridden
     * @throws InjectionException
     */
    private <P extends Annotation> InjectionBinding<?> addOrMergeOverrideInjectionBinding(Class<?> instanceClass,
                                                                                          Member member,
                                                                                          A annotation,
                                                                                          String jndiName) // d675843.2
    throws InjectionException
    {
        @SuppressWarnings("unchecked")
        InjectionProcessor<P, ?> processor = (InjectionProcessor<P, ?>) ivOverrideProcessor;

        if (processor != null)
        {
            // This logic is the same as addOrMergeInjectionBinding, except it
            // uses OverrideInjectionProcessor methods to create and merge
            // injection bindings rather than InjectionProcessor and
            // InjectionBinding methods.

            InjectionBinding<P> injectionBinding = processor.getInjectionBindingForAnnotation(jndiName);
            @SuppressWarnings("unchecked")
            OverrideInjectionProcessor<P, A> overrideProcessor = (OverrideInjectionProcessor<P, A>) processor;

            if (injectionBinding == null)
            {
                injectionBinding = overrideProcessor.createOverrideInjectionBinding(instanceClass, member, annotation, jndiName);

                if (injectionBinding != null)
                {
                    processor.updateInjectionBinding(jndiName, injectionBinding);
                    processor.addInjectionBinding(injectionBinding);
                    return injectionBinding;
                }
            }
            else
            {
                overrideProcessor.mergeOverrideInjectionBinding(instanceClass, member, annotation, injectionBinding);
                return injectionBinding;
            }
        }

        return null;
    }

    /**
     * Creates and adds a new injection binding, or merges the data in an
     * annotation with an existing injection binding.
     *
     * @param instanceClass the class that contained the annotation
     * @param member the member in class that contained the annotation, or
     *            <tt>null</tt> if the annotation was found at the class level
     * @param annotation the annotation data
     */
    void addOrMergeInjectionBinding(Class<?> instanceClass, Member member, A annotation)
                    throws InjectionException
    {
        String jndiName = getJndiName(annotation);

        String propertyName = null;
        if (jndiName == null || jndiName.length() == 0)
        {
            if (member == null)
            {
                validateMissingJndiName(instanceClass, annotation);
                return;
            }

            propertyName = getJavaBeansPropertyName(member);
            if (propertyName == null && !isNonJavaBeansPropertyMethodAllowed())
            {
                Tr.error(tc, "NOT_A_SETTER_METHOD_ON_METHOD_ANNOTATION_CWNEN0008E", member.getName());
                if (isValidationFailable()) // F50309.6
                {
                    throw new InjectionException("The " + member.getDeclaringClass() + '.' + member.getName() +
                                                 " method is annotated @" + annotation.annotationType().getSimpleName() +
                                                 ", but it is not a setter method.");
                }
                return;
            }
        }

        InjectionBinding<?> resultInjectionBinding;
        if (jndiName == null) // F50309.5
        {
            if (ivSimpleInjectionBindings == null)
            {
                ivSimpleInjectionBindings = new LinkedHashMap<Member, InjectionBinding<A>>();
            }

            // If the binding already exists for this member; no need to merge or add
            // the target since same member processed from a different subclass. PM88594
            if (ivSimpleInjectionBindings.containsKey(member))
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "binding found, merge not needed : " + member);
                return;
            }

            InjectionBinding<A> injectionBinding = createInjectionBinding(annotation, instanceClass, member, null);
            resultInjectionBinding = injectionBinding;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "adding " + injectionBinding);
            ivSimpleInjectionBindings.put(member, injectionBinding);
        }
        else
        {
            if (propertyName != null)
            {
                jndiName = instanceClass.getCanonicalName() + '/' + propertyName;
            }

            InjectionBinding<A> injectionBinding = getInjectionBindingForAnnotation(jndiName);
            if (injectionBinding == null)
            {
                resultInjectionBinding = addOrMergeOverrideInjectionBinding(instanceClass, member, annotation, jndiName); // d675843.2

                if (resultInjectionBinding == null)
                {
                    injectionBinding = createInjectionBinding(annotation, instanceClass, member, jndiName);
                    resultInjectionBinding = injectionBinding;

                    updateInjectionBinding(jndiName, injectionBinding);
                    addInjectionBinding(injectionBinding);
                }
            }
            else
            {
                resultInjectionBinding = injectionBinding;
                injectionBinding.merge(annotation, instanceClass, member); // d675172.1

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "merged " + injectionBinding);
            }
        }

        if (member == null)
        {
            resultInjectionBinding.addInjectionClass(instanceClass); // F743-30682
        }
        else
        {
            resultInjectionBinding.addInjectionTarget(member);
        }
    }

    /**
     * Perform validation on a class-level annotation for which {@link #getJndiName} returned the empty string. By default, this method
     * performs no validation for backwards compatibility.
     *
     * @param instanceClass the class containing the annotation
     * @param annotation the annotation
     * @throws InjectionException
     */
    protected void validateMissingJndiName(Class<?> instanceClass, A annotation)
                    throws InjectionException
    {
        if (isValidationLoggable()) // F50309.6
        {
            Tr.error(tc, "MISSING_CLASS_LEVEL_ANNOTATION_NAME_CWNEN0073E",
                     '@' + annotation.annotationType().getSimpleName(),
                     instanceClass.getName(),
                     ivNameSpaceConfig.getModuleName(),
                     ivNameSpaceConfig.getApplicationName());
            if (isValidationFailable())
            {
                throw new InjectionException("The @" + annotation.annotationType().getSimpleName() +
                                             " class-level annotation on the " + instanceClass.getName() +
                                             " class in the " + ivNameSpaceConfig.getModuleName() +
                                             " module of the " + ivNameSpaceConfig.getApplicationName() +
                                             " application does not specify a JNDI name.");
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "ignoring class-level annotation without name");
        }
    }

    /**
     * Determine if injection should be allowed for methods that do not conform
     * to JavaBeans naming conventions. By default, this method returns false.
     *
     * @param method the method containing the annotation
     * @param annotation the annotation
     * @return true if non-JavaBeans property methods should be allowed
     */
    // F50309.5
    protected boolean isNonJavaBeansPropertyMethodAllowed()
    {
        return false;
    }

    /**
     * Resolve all bindings belonging to this processor.
     *
     * @throws InjectionException
     */
    // re-wrote method d457733
    void resolveInjectionBindings()
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolveInjectionBindings: " + this);

        Collection<InjectionBinding<A>> allInjectionBindings = ivAllAnnotationsCollection.values();
        for (InjectionBinding<A> injectionBinding : allInjectionBindings)
        {
            if (injectionBinding.ivResolveAttempted) // F87539
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "skipping already resolved " + injectionBinding.toSimpleString());
                continue;
            }

            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "resolving " + injectionBinding.toSimpleString());

            injectionBinding.resolveJndiName(); // F743-29417
            resolve(injectionBinding);
            injectionBinding.ivResolveAttempted = true; // F87539

            InjectionScope scope = injectionBinding.getInjectionScope();
            if (scope != InjectionScope.COMP &&
                ivContext.ivSaveNonCompInjectionBindings) // F743-33811.2
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "saving " + scope + " injection binding " + injectionBinding.toSimpleString());

                Map<Class<?>, Map<String, InjectionBinding<?>>> savedInjectionBindings;
                if (scope == InjectionScope.MODULE)
                {
                    savedInjectionBindings = ivContext.ivSavedModuleInjectionBindings;
                }
                else if (scope == InjectionScope.APP)
                {
                    savedInjectionBindings = ivContext.ivSavedAppInjectionBindings;
                }
                else
                {
                    savedInjectionBindings = ivContext.ivSavedGlobalInjectionBindings;
                }

                Map<String, InjectionBinding<A>> injectionBindingsMap = getSavedInjectionBindings(savedInjectionBindings, true);
                String jndiName = injectionBinding.getJndiName();
                InjectionBinding<A> savedBinding = injectionBindingsMap.get(jndiName);
                if (savedBinding == null)
                {
                    injectionBindingsMap.put(jndiName, injectionBinding);
                }
                else
                {
                    savedBinding.mergeSaved(injectionBinding);
                }
            }
        }

        // F743-21481
        // Add all of the InjectionBindings that this processor instance knows
        // about (these InjectionBinding instances are now resolved) so they can
        // later be used to determine the injection targets for a class.
        ivContext.ivProcessedInjectionBindings.addAll(allInjectionBindings); // F743-33811.1

        if (ivSimpleInjectionBindings != null) // F50309.5 PM88594
        {
            Collection<InjectionBinding<A>> injectionBindings = ivSimpleInjectionBindings.values();
            for (InjectionBinding<A> injectionBinding : injectionBindings)
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "resolving " + injectionBinding.toSimpleString());

                resolve(injectionBinding);
            }

            ivContext.ivProcessedInjectionBindings.addAll(injectionBindings);
        }

        if (!ivMissingBindings.isEmpty()) {
            Iterator<String> missingBindingIter = ivMissingBindings.iterator();
            String resRefName;
            String displayName = ivNameSpaceConfig.getDisplayName();
            while (missingBindingIter.hasNext()) {
                resRefName = missingBindingIter.next();
                Tr.error(tc, "UNABLE_TO_RESOLVE_THE_RESOURCE_REFERENCE_CWNEN0044E", resRefName, displayName);
            }
            throw new InjectionException("CWNEN0044E: A resource reference binding could not be found for the following resource references " + ivMissingBindings.toString()
                                         + ", defined for the " + displayName + " component.");
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolveInjectionBindings");
    }

    /**
     * Bind all the jndi annotation entries found.
     */
    void performJavaNameSpaceBinding() throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "performJavaNameSpaceBinding: " + this);

        for (InjectionBinding<A> injectionBinding : ivAllAnnotationsCollection.values())
        {
            // F743-31682 - Only bind to java:global/:app/:module if needed.
            if (injectionBinding.getInjectionScope() == InjectionScope.COMP ||
                ivContext.ivBindNonCompInjectionBindings)
            {
                if (injectionBinding.getBindingObject() == null)
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "skipping empty " + injectionBinding);
                }
                else
                {
                    injectionBinding.bindInjectedObject();
                }
            }
            else
            {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.debug(tc, "skipping non-java:comp " + injectionBinding.toSimpleString());
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "performJavaNameSpaceBinding");
    }

    /**
     * Add the InjectionBinding to the annotationCollection. The collection will be used
     * later when binding and resolving injection targets.
     *
     * @param injectionBinding
     */
    public final void addInjectionBinding(InjectionBinding<A> injectionBinding)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "addInjectionBinding: " + injectionBinding);

        // jndi name not found in collection, simple add
        ivAllAnnotationsCollection.put(injectionBinding.getJndiName(), injectionBinding);
    }

    /**
     * Gets the JavaBeans property name of a member. For a field, this is the
     * field name. For a method, this is the decapitalized name of the method
     * after removing the "set" prefix. If the method is not a JavaBeans
     * setter, then null is returned.
     *
     * @param fieldOrMethod the field or method
     * @return the JavaBeans property name
     */
    // F50309.5
    protected final String getJavaBeansPropertyName(Member fieldOrMethod)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getJavaBeansPropertyName : " + fieldOrMethod);

        String propertyName;
        if (fieldOrMethod instanceof Field)
        {
            Field field = (Field) fieldOrMethod;
            propertyName = field.getName();
        }
        else
        {
            Method method = (Method) fieldOrMethod;

            String name = method.getName();
            if (name.startsWith("set") &&
                name.length() > 3 &&
                Character.isUpperCase(name.charAt(3)))
            {
                propertyName = Character.toLowerCase(name.charAt(3)) +
                               (name.length() > 4 ? name.substring(4) : "");
            }
            else
            {
                propertyName = null;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getJavaBeansPropertyName : " + propertyName);
        return propertyName;
    }

    /**
     * Process XML metadata obtained from {@link #ivNameSpaceConfig}, such as {@link ComponentNameSpaceConfiguration#getResourceRefs}. For each
     * reference, {@link #ivAllAnnotationsCollection} should be accessed. If an
     * InjectionBinding with the name exists in the Map, the metadata should be
     * merged. Otherwise, a new InjectionBinding should be created, and the {@link #addInjectionBinding} and {@link InjectionBinding#addInjectionTarget(Class, String, String)}
     * methods should be called.
     *
     * @throws InjectionException
     */
    public abstract void processXML()
                    throws InjectionException; // d367834.11

    /**
     * Returns an annotation-specific InjectionBinding associated with the
     * specified input annotation and JNDI name.
     *
     * @param annotation the annotation to create a binding for.
     * @param instanceClass the class containing the annotation.
     * @param member the Field or Method associated with the annotation;
     *            null if a class level annotation.
     * @param jndiName {@link #getJndiName} or {@link #getDefaultJndiName}
     * @throws InjectionException
     */
    public abstract InjectionBinding<A> createInjectionBinding(A annotation,
                                                               Class<?> instanceClass,
                                                               Member member,
                                                               String jndiName)
                    throws InjectionException;

    /**
     * Resolves the injection binding. For bindings that will be bound into
     * naming, this method should call {@link InjectionBinding#setObjects(Object, Object)}, {@link InjectionBinding#setObjects(Object, javax.naming.Reference)}, or
     * {@link InjectionBinding#setReferenceObject}. For processors that handle
     * injection only, override {@link InjectionBinding#isResolved} to return
     * true, and override {@link InjectionBinding#getInjectionObjectInstance} to
     * return instances to inject.
     *
     * @param injectionBinding the injection binding
     * @throws InjectionException
     */
    public abstract void resolve
                    (InjectionBinding<A> injectionBinding)
                                    throws InjectionException;

    /**
     * Returns the 'name' attribute of the annotation. <p>
     *
     * The name attribute, if present, is the Jndi Name where the
     * injection object is bound into naming. <p>
     *
     * Although most injection annotations have a 'name' attribute,
     * the attribute is not present in the base annotation class,
     * so each subclass processor must extract the value. <p>
     *
     * @param annotation the annotation to extract the name from.
     * @return the JNDI name, "" if {@link #getDefaultJndiName} should be
     *         used instead, or null if the annotation does not support binding
     **/
    public abstract String getJndiName(A annotation);

    /**
     * Returns the array of individual annotations for a plural annotation.
     * This method will only be called if a non-null plural annotation class
     * was passed to the constructor.
     *
     * @param pluralAnnotation the plural annotation
     * @return the array of value annotations
     */
    public abstract A[] getAnnotations(AS pluralAnnotation); // F743-21590

    /**
     * Checks whether validation messages should be logged or not. <p>
     */
    // F50309.6
    protected final boolean isValidationLoggable()
    {
        return InjectionEngineAccessor.getInternalInstance().isValidationLoggable
                        (ivNameSpaceConfig.isCheckApplicationConfiguration());
    }

    /**
     * Checks whether more significant validation messages should
     * result in a failure or not. <p>
     */
    // F743-33178
    protected final boolean isValidationFailable()
    {
        return InjectionEngineAccessor.getInternalInstance().isValidationFailable
                        (ivNameSpaceConfig.isCheckApplicationConfiguration()); // F50309.6
    }

    /**
     * Emit the customized human readable text to represent this object
     * in an FFDC incident report.
     *
     * @param is the incident stream, the data will be written here
     */
    // F49213
    public final void formatTo(IncidentStream is)
    {
        // -----------------------------------------------------------------------
        // Indicate the start of the dump, and include the identity
        // of InjectionProcessor, so this can easily be matched to a trace.
        // -----------------------------------------------------------------------
        is.writeLine("", ">--- Start InjectionProcessor Dump ---> " + Util.identity(this));
        is.writeLine("", "Annotation  = " + ivAnnotationClass);
        is.writeLine("", "Annotations = " + ivAnnotationsClass);

        is.writeLine("", "");
        is.writeLine("", ivContext != null ? ivContext.toString() : "ivContext = null");

        is.writeLine("", "");
        is.writeLine("", ivNameSpaceConfig != null ? ivNameSpaceConfig.toString() : "ivNameSpaceConfig = null");

        is.writeLine("", "");
        is.writeLine("", "Override Processor : " + ivOverrideProcessor);

        is.writeLine("", "");
        is.writeLine("", "Object Factory Map : ");
        for (Class<?> key : ivObjectFactoryMap.keySet())
        {
            is.writeLine("", "   " + key.getName() + " : " + Util.identity(ivObjectFactoryMap.get(key)));
        }

        is.writeLine("", "");
        is.writeLine("", "No-Override Object Factory Map : ");
        for (Class<?> key : ivNoOverrideObjectFactoryMap.keySet())
        {
            is.writeLine("", "   " + key.getName() + " : " + Util.identity(ivNoOverrideObjectFactoryMap.get(key)));
        }

        is.writeLine("", "");
        is.writeLine("", "Override Reference Factories : ");
        for (OverrideReferenceFactory<A> factory : ivOverrideReferenceFactories)
        {
            is.writeLine("", "   " + Util.identity(factory));
        }

        is.writeLine("", "");
        is.writeLine("", "All Bindings : ");
        for (String key : ivAllAnnotationsCollection.keySet())
        {
            is.writeLine("", "   " + key + " : " + Util.identity(ivAllAnnotationsCollection.get(key)));
        }

        is.writeLine("", "<--- InjectionProcessor Dump Complete---< ");
    }
}
