/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionConfigConstants;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorContextImpl;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;
import com.ibm.wsspi.injectionengine.MethodMap;

/**
 * Manages the InjectionProcessor instances for processing of injection metadata
 * for a single ComponentNameSpaceConfiguration.
 */
public class InjectionProcessorManager
{
    private static final String CLASS_NAME = InjectionProcessorManager.class.getName();
    private static final TraceComponent tc = Tr.register(InjectionProcessorManager.class,
                                                         InjectionConfigConstants.traceString,
                                                         InjectionConfigConstants.messageFile);

    private final AbstractInjectionEngine ivInjectionEngine;
    private final ComponentNameSpaceConfiguration ivNameSpaceConfig;
    private final InjectionProcessorContextImpl ivContext;
    private final List<InjectionProcessorProvider<?, ?>> ivProviders;

    /**
     * Lazily initialized array of processors. The contents of this array
     * parallel {@link #ivProviders}.
     */
    private InjectionProcessor<?, ?>[] ivProcessors;

    /**
     * For each processor, the index of the processor that overrides it, or -1 if
     * no processor overrides it.
     */
    private int[] ivOverrideIndices;

    public InjectionProcessorManager(AbstractInjectionEngine injectionEngine,
                                     ComponentNameSpaceConfiguration compNSConfig,
                                     InjectionProcessorContextImpl context,
                                     List<InjectionProcessorProvider<?, ?>> providers)
    {
        ivInjectionEngine = injectionEngine;
        ivNameSpaceConfig = compNSConfig;
        ivContext = context;
        ivProviders = providers;

        for (int processorIndex = 0; processorIndex < providers.size(); processorIndex++)
        {
            InjectionProcessorProvider<?, ?> provider = providers.get(processorIndex);
            Class<? extends Annotation> overrideAnnClass = provider.getOverrideAnnotationClass();
            if (overrideAnnClass != null)
            {
                for (int overriddenProcessorIndex = 0; overriddenProcessorIndex < providers.size(); overriddenProcessorIndex++)
                {
                    if (overrideAnnClass == ivProviders.get(overriddenProcessorIndex).getAnnotationClass())
                    {
                        if (ivOverrideIndices == null)
                        {
                            ivOverrideIndices = new int[providers.size()];
                            Arrays.fill(ivOverrideIndices, -1);
                        }

                        ivOverrideIndices[overriddenProcessorIndex] = processorIndex;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Get or create a processor.
     *
     * @param processorIndex the index in {@link #ivProcessors}
     * @param provider the corresponding provider
     * @return the processor
     * @throws InjectionException if instance creation or initialization fails
     */
    private <A extends Annotation, AS extends Annotation> InjectionProcessor<A, AS> getProcessor(int processorIndex,
                                                                                                 InjectionProcessorProvider<A, AS> provider)
                    throws InjectionException
    {
        if (ivProcessors == null)
        {
            ivProcessors = new InjectionProcessor<?, ?>[ivProviders.size()];
        }

        if (ivProcessors[processorIndex] == null)
        {
            ivProcessors[processorIndex] = ivProviders.get(processorIndex).createInjectionProcessor();
            ivContext.initProcessor(ivProcessors[processorIndex], ivNameSpaceConfig);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        InjectionProcessor<A, AS> processor = (InjectionProcessor) ivProcessors[processorIndex];
        return processor;
    }

    /**
     * Process XML for all processors as needed.
     */
    public void processXML()
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processXML");

        for (int processorIndex = 0; processorIndex < ivProviders.size(); processorIndex++)
        {
            InjectionProcessorProvider<?, ?> provider = ivProviders.get(processorIndex);
            if (isProcessXMLNeeded(provider))
            {
                InjectionProcessor<?, ?> processor = getProcessor(processorIndex, provider);

                try
                {
                    processor.processXML();
                } catch (InjectionException iex)
                {
                    FFDCFilter.processException(iex, CLASS_NAME + ".processXML",
                                                "300", this, new Object[] { processor });
                    // Include the cause message in the logged message, and don't
                    // unnecessarily wrap the exception.                          F53641
                    Tr.error(tc, "FAILED_TO_PROCESS_XML_FROM_DD_CWNEN0009E", iex.getMessage());
                    throw iex;
                } catch (Throwable t)
                {
                    FFDCFilter.processException(t, CLASS_NAME + ".processXML",
                                                "768", this, new Object[] { processor });
                    // Include the cause stack in the logged message.             F53641
                    Tr.error(tc, "FAILED_TO_PROCESS_XML_FROM_DD_CWNEN0009E", t);
                    throw new InjectionConfigurationException("Failed to process xml from Deployment Descriptor.", t);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processXML");
    }

    /**
     * Returns true if the specified processor must be created to process XML.
     */
    private boolean isProcessXMLNeeded(InjectionProcessorProvider<?, ?> provider)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "isProcessXMLNeeded: " + provider);

        List<Class<? extends JNDIEnvironmentRef>> refClasses = provider.getJNDIEnvironmentRefClasses();
        if (refClasses == null)
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "isProcessXMLNeeded: true (unknown)");
            return true;
        }

        for (Class<? extends JNDIEnvironmentRef> refClass : refClasses)
        {
            List<? extends JNDIEnvironmentRef> refs = ivNameSpaceConfig.getJNDIEnvironmentRefs(refClass);
            if (refs != null && !refs.isEmpty())
            {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "isProcessXMLNeeded: true (" + refClass.getName() + ")");
                return true;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "isProcessXMLNeeded: false");
        return false;
    }

    /**
     * Process all class, field, and method annotations for the class hierarchy.
     *
     * @param classHierarchy the subclass of the hierarchy to process
     */
    public void processAnnotations(Class<?> classHierarchy)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processAnnotations: " + classHierarchy);

        for (Class<?> klass = classHierarchy; klass != null && klass != Object.class; klass = klass.getSuperclass())
        {
            Field[] fields = getAllDeclaredFields(klass, classHierarchy);

            for (int processorIndex = 0; processorIndex < ivProviders.size(); processorIndex++)
            {
                InjectionProcessorProvider<?, ?> provider = ivProviders.get(processorIndex);

                processClassAnnotations(processorIndex, provider, klass);

                if (fields != null)
                {
                    processMemberAnnotations(processorIndex, provider, fields, fields);
                }
            }
        }

        Method[] methods = getAllDeclaredMethods(classHierarchy);
        for (int processorIndex = 0; processorIndex < ivProviders.size(); processorIndex++)
        {
            processMemberAnnotations(processorIndex, ivProviders.get(processorIndex), methods, methods);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processAnnotations");
    }

    /**
     * Creates and adds a new injection binding, or merges the data in an
     * annotation with an existing injection binding.
     */
    private <A extends Annotation, AS extends Annotation> void addOrMergeInjectionBinding(int processorIndex,
                                                                                          InjectionProcessor<A, AS> processor,
                                                                                          Class<?> klass,
                                                                                          Member member,
                                                                                          A ann)
                    throws InjectionException
    {
        // Lazily create override processors as needed.
        if (ivOverrideIndices != null)
        {
            int overrideIndex = ivOverrideIndices[processorIndex];
            if (overrideIndex != -1)
            {
                // Create and set the override processor only once.
                ivOverrideIndices[processorIndex] = -1;

                InjectionProcessor<?, ?> overrideProcessor = getProcessor(overrideIndex, ivProviders.get(overrideIndex));
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "adding override processor " + overrideProcessor + " to " + processor);
                InjectionProcessorContextImpl.setOverrideProcessor(processor, overrideProcessor);
            }
        }

        InjectionProcessorContextImpl.addOrMergeInjectionBinding(processor, klass, member, ann);
    }

    /**
     * Convert an annotation to a string, but mask members named password.
     *
     * @param ann the annotation
     * @return the string representation of the annotation
     */
    static String toStringSecure(Annotation ann) {
        Class<?> annType = ann.annotationType();

        StringBuilder sb = new StringBuilder();
        sb.append('@').append(annType.getName()).append('(');

        boolean any = false;
        for (Method m : annType.getMethods()) {
            Object defaultValue = m.getDefaultValue();
            if (defaultValue != null) {
                String name = m.getName();

                Object value;
                try {
                    value = m.invoke(ann);
                    if (name.equals("password") && !defaultValue.equals(value)) {
                        value = "********";
                    } else if (value instanceof Object[]) {
                        value = Arrays.toString((Object[]) value);
                    } else {
                        value = String.valueOf(value);
                    }
                } catch (Throwable t) {
                    value = "<" + t + ">";
                }

                if (any) {
                    sb.append(", ");
                } else {
                    any = true;
                }

                sb.append(name).append('=').append(value);
            }
        }

        return sb.append(')').toString();
    }

    private static <A extends Annotation> A getAnnotation(Class<?> klass, Class<A> annClass) {
        try {
            return klass.getAnnotation(annClass);
        } catch (Error error) {
            // Attempt to workaround JDK bug that results in GenericSignatureFormatError or AnnotationFormatError
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Retry after error obtaining " + annClass + " annotation from " + klass + " class : " + error);
            return klass.getAnnotation(annClass);
        }
    }
    
    /**
     * Process class annotations with the specified processor.
     *
     * @param klass the class hierarchy
     * @param processorIndex the index in {@link #ivProcessors}
     * @param provider the corresponding provider
     */
    private <A extends Annotation, AS extends Annotation> void processClassAnnotations(int processorIndex,
                                                                                       InjectionProcessorProvider<A, AS> provider,
                                                                                       Class<?> klass)
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        Class<A> annClass = provider.getAnnotationClass();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "looking for annotation : " + annClass);
        if (annClass != null)
        {
            A ann = getAnnotation(klass, annClass);
            if (ann != null)
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "found class annotation " + toStringSecure(ann));

                InjectionProcessor<A, AS> processor = getProcessor(processorIndex, provider);
                addOrMergeInjectionBinding(processorIndex, processor, klass, null, ann);
            }

            Class<AS> pluralAnnClass = provider.getAnnotationsClass();
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "looking for annotation(s) : " + pluralAnnClass);
            if (pluralAnnClass != null)
            {
                AS pluralAnn = klass.getAnnotation(pluralAnnClass);
                if (pluralAnn != null)
                {
                    InjectionProcessor<A, AS> processor = getProcessor(processorIndex, provider);

                    A[] singleAnns = processor.getAnnotations(pluralAnn);
                    if (singleAnns != null)
                    {
                        for (A singleAnn : singleAnns)
                        {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "found plural class annotation " + toStringSecure(singleAnn));
                            addOrMergeInjectionBinding(processorIndex, processor, klass, null, singleAnn);
                        }
                    }
                }
            }
        }
    }

    /**
     * Return the declared fields for the specified class in the class hierarchy.
     *
     * @param klass the specified class to get declared fields
     * @param classHierarchy the entire class hierarchy
     * @return the declared fields, or null if an exception occurs
     */
    private Field[] getAllDeclaredFields(Class<?> klass, Class<?> classHierarchy)
    {
        try
        {
            return klass.getDeclaredFields();
        } catch (Throwable ex)
        {
            // The most common 'problem' here is a NoClassDefFoundError because
            // a dependency class (super/field type, etc) could not be found
            // when the class is fully initialized.

            // Since interrogating a class for annotations is new in Java EE 1.5,
            // it is possible this application may have worked in prior versions
            // of WebSphere, if the application never actually used the class.
            // So, rather than just fail the app start, a Warning will be logged
            // indicating the class will not be processed for annotations, and
            // the application will be allowed to start.                 d477931

            FFDCFilter.processException(ex, CLASS_NAME + ".getAllDeclaredFields",
                                        "249", new Object[] { classHierarchy, klass });

            if (classHierarchy != klass)
            {
                Tr.warning(tc, "SUPER_FIELD_ANNOTATIONS_IGNORED_CWNEN0048W",
                           klass.getName(), classHierarchy.getName(), ex.toString()); // d479669 RTC119889
                if (ivInjectionEngine.isValidationFailable(ivNameSpaceConfig.isCheckApplicationConfiguration())) // F743-14449
                {
                    throw new RuntimeException("Resource annotations on the fields of the " + klass.getName() +
                                               " class could not be processed. The " + klass.getName() +
                                               " class is being processed for annotations because it is" +
                                               " referenced by the " + classHierarchy.getName() + " application class." +
                                               " The annotations could not be obtained because of the exception : " +
                                               ex, ex);
                }
            }
            else
            {
                Tr.warning(tc, "FIELD_ANNOTATIONS_IGNORED_CWNEN0047W",
                           klass.getName(), ex.toString()); // d479669 d641396 RTC119889
                if (ivInjectionEngine.isValidationFailable(ivNameSpaceConfig.isCheckApplicationConfiguration())) // F743-14449
                {
                    throw new RuntimeException("Resource annotations on the fields of the " + klass.getName() +
                                               " class could not be processed. The annotations could not be obtained" +
                                               " because of the exception : " + ex, ex);
                }
            }

            return null;
        }
    }

    /**
     * Return the non-overridden declared methods for the specified class.
     */
    private static Method[] getAllDeclaredMethods(Class<?> classHierarchy)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        Collection<MethodMap.MethodInfo> methodInfos = MethodMap.getAllDeclaredMethods(classHierarchy);
        Method[] methods = new Method[methodInfos.size()];
        int index = 0;

        for (MethodMap.MethodInfo methodInfo : methodInfos)
        {
            Method method = methodInfo.getMethod();
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "adding method " + method);
            methods[index++] = method;
        }

        return methods;
    }

    /**
     * Process annotations on fields and methods.
     *
     * @param members the Field[] or Method[] array
     * @param annotatedMembers the same Field[] or Method[] array
     * @param processorIndex the index in {@link #ivProcessors}
     * @param provider the corresponding provider
     */
    private <A extends Annotation> void processMemberAnnotations(int processorIndex,
                                                                 InjectionProcessorProvider<A, ?> provider,
                                                                 Member[] members,
                                                                 AnnotatedElement[] annotatedMembers)
                    throws InjectionException
    {
        Class<A> annClass = provider.getAnnotationClass();
        if (annClass != null)
        {
            for (int i = 0; i < members.length; i++)
            {
                A ann = annotatedMembers[i].getAnnotation(annClass);
                if (ann != null)
                {
                    Member member = members[i];
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "found member annotation " + toStringSecure(ann) + " on " + member);

                    InjectionProcessor<A, ?> processor = getProcessor(processorIndex, provider);
                    addOrMergeInjectionBinding(processorIndex, processor, member.getDeclaringClass(), member, ann);
                }
            }
        }
    }

    /**
     * Process injection bindings for all created processors.
     */
    public void processBindings()
                    throws InjectionException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "processBindings");

        if (ivProcessors != null) // RTC115266
        {
            for (InjectionProcessor<?, ?> processor : ivProcessors)
            {
                if (processor != null)
                {
                    try
                    {
                        InjectionProcessorContextImpl.processBindings(processor);
                    } catch (InjectionException iex)
                    {
                        FFDCFilter.processException(iex, CLASS_NAME + ".processBindings",
                                                    "480", this, new Object[] { processor });
                        // Include the cause message in the logged message, and don't
                        // unnecessarily wrap the exception.                          F53641
                        Tr.error(tc, "FAILED_TO_PROCESS_BINDINGS_CWNEN0011E", iex.getMessage());
                        throw iex;
                    } catch (Throwable t)
                    {
                        FFDCFilter.processException(t, CLASS_NAME + ".processBindings",
                                                    "815", this, new Object[] { processor });
                        // Include the cause stack in the logged message.             F53641
                        Tr.error(tc, "FAILED_TO_PROCESS_BINDINGS_CWNEN0011E", t);
                        throw new InjectionException("Failed to process bindings for metadata", t);
                    }
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "processBindings");
    }
}
