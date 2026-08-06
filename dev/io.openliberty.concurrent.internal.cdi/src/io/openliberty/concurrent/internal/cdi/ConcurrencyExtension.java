/*******************************************************************************
 * Copyright (c) 2021,2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.internal.cdi;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.concurrent.WSManagedExecutorService;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.resource.ResourceFactory;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;

import io.openliberty.concurrent.internal.cdi.interceptor.AsyncInterceptor;
import io.openliberty.concurrent.internal.cdi.interceptor.LockInterceptor;
import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactories;
import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactory;
import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;
import jakarta.enterprise.concurrent.Schedule;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;
import jakarta.inject.Qualifier;

/**
 * CDI Extension for
 * Jakarta Concurrency 3.1/CDI 4.1 in Jakarta EE 11 and
 * Jakarta Concurrency 3.2/CDI 5.0 in Jakarta EE 12
 */
public class ConcurrencyExtension implements Extension {
    private static final TraceComponent tc = Tr.register(ConcurrencyExtension.class);

    private static final String DEFAULT_MSES_FILTER = //
                    "(&(id=DefaultManagedScheduledExecutorService)(component.name=com.ibm.ws.concurrent.internal.ManagedScheduledExecutorServiceImpl))";

    private static final Annotation[] DEFAULT_QUALIFIER_ARRAY = //
                    new Annotation[] { Default.Literal.INSTANCE };

    private static final Set<Annotation> DEFAULT_QUALIFIER_SET = //
                    Set.of(Default.Literal.INSTANCE);

    /**
     * Indicates if we were able to create a default ManagedThreadFactory bean.
     * If so, an instance of the bean is obtained during afterDeploymentValidation
     * to force context capture to occur.
     */
    private boolean producedDefaultMTF;

    /**
     * Collects the bean classes that have methods directly annotated Schedule.
     */
    private final List<AnnotatedType<?>> beanClassesWithScheduleMethods = //
                    new ArrayList<>();

    /**
     * Register interceptors before bean discovery.
     *
     * @param beforeBeanDiscovery
     * @param beanManager
     */
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event,
                                    BeanManager beanManager) {
        // register the Asynchronous interceptor binding and interceptor
        AnnotatedType<Asynchronous> asyncBindingType = //
                        beanManager.createAnnotatedType(Asynchronous.class);
        event.addInterceptorBinding(asyncBindingType);

        AnnotatedType<AsyncInterceptor> asyncInterceptorType = //
                        beanManager.createAnnotatedType(AsyncInterceptor.class);
        String asyncInterceptorTypeString = CDIServiceUtils //
                        .getAnnotatedTypeIdentifier(asyncInterceptorType,
                                                    getClass());
        event.addAnnotatedType(asyncInterceptorType,
                               asyncInterceptorTypeString);

        // register the Lock interceptor binding and interceptor (if available)
        if (LockInterceptor.ANNO_CLASS != null) {
            // register the Asynchronous interceptor binding and interceptor
            AnnotatedType<? extends Annotation> lockBindingType = beanManager //
                            .createAnnotatedType(LockInterceptor.ANNO_CLASS);
            event.addInterceptorBinding(lockBindingType);

            AnnotatedType<LockInterceptor> lockInterceptorType = beanManager //
                            .createAnnotatedType(LockInterceptor.class);
            String identifier = CDIServiceUtils //
                            .getAnnotatedTypeIdentifier(lockInterceptorType,
                                                        getClass());
            AnnotatedTypeConfigurator<LockInterceptor> lockInterceptorConfigurator = //
                            event.addAnnotatedType(LockInterceptor.class,
                                                   identifier);

            // We are not able to code @Lock directly on LockInterceptor because
            // the project must compile against Java 17 (@Lock requires Java 21).
            // To work around that, we add it dynamically here:
            lockInterceptorConfigurator.add(LockInterceptor.LOCK_ANNO);
        }
    }

    /**
     * Collect a list of bean classes that have methods directly annotated Schedule.
     *
     * @param <T>   bean class
     * @param event the event
     */
    public <T> void addAnnotatedType//
    (
     @Observes @WithAnnotations(Schedule.class) ProcessAnnotatedType<T> event) {
        beanClassesWithScheduleMethods.add(event.getAnnotatedType());
    }

    /**
     * Register beans for default instances and qualified instances of concurrency
     * resources after bean discovery.
     *
     * @param event the event
     */
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {

        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd == null)
            throw new IllegalStateException(); // should be unreachable

        J2EEName jeeName = cmd.getJ2EEName();
        CDI<Object> cdi = CDI.current();

        boolean successState = ServiceCaller.callOnce(ConcurrencyExtension.class, QualifiedResourceFactories.class, service -> {
            ConcurrencyExtensionMetadata extSvc = (ConcurrencyExtensionMetadata) service;

            // Add beans for Concurrency default resources if not already present:
            if (!cdi.select(ContextService.class, DEFAULT_QUALIFIER_ARRAY).isResolvable())
                event.addBean(new ContextServiceBean(extSvc.defaultContextServiceFactory, DEFAULT_QUALIFIER_SET));

            if (!cdi.select(ManagedExecutorService.class, DEFAULT_QUALIFIER_ARRAY).isResolvable())
                event.addBean(new ManagedExecutorBean(extSvc.defaultManagedExecutorFactory, DEFAULT_QUALIFIER_SET));

            if (!cdi.select(ManagedScheduledExecutorService.class, DEFAULT_QUALIFIER_ARRAY).isResolvable())
                event.addBean(new ManagedScheduledExecutorBean(extSvc.defaultManagedScheduledExecutorFactory, DEFAULT_QUALIFIER_SET));

            if (!cdi.select(ManagedThreadFactory.class, DEFAULT_QUALIFIER_ARRAY).isResolvable()) {
                try {
                    event.addBean(new ManagedThreadFactoryBean(cmd, extSvc, DEFAULT_QUALIFIER_SET));
                    producedDefaultMTF = true;
                } catch (IllegalStateException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "A default managed thread factory was not created for this component", cmd);
                    }
                }
            }

            // Look for beans from the module and the application.
            // TODO EJBs and component level?

            List<Map<List<String>, QualifiedResourceFactory>> listFromModule = extSvc.removeAll(cmd.getJ2EEName().toString());
            if (listFromModule != null)
                addBeans(event, listFromModule, extSvc);

            List<Map<List<String>, QualifiedResourceFactory>> listFromApp = extSvc.removeAll(jeeName.getApplication());
            if (listFromApp != null)
                addBeans(event, listFromApp, extSvc);
        });

        if (!successState) {
            // Could not register all managed service beans because the
            // ConcurrencyExtensionMetadata service was unavailable.
            throw new IllegalStateException();
        }
    }

    /**
     * Add beans for Concurrency resources that have one or more qualifier annotations:
     *
     * @param event event for AfterBeanDiscovery.
     * @param list  list of qualifiers to resource factory for each type of resource and for each JEE name.
     *                  JEEName -> [qualifiers -> ResourceFactory for ContextService,
     *                  . . . . . . qualifiers -> ResourceFactory for ManagedExecutorService,
     *                  . . . . . . qualifiers -> ResourceFactory for ManagedScheduledExecutorService,
     *                  . . . . . . qualifiers -> ResourceFactory for ManagedThreadFactory ]
     */
    private void addBeans(AfterBeanDiscovery event, List<Map<List<String>, QualifiedResourceFactory>> list, ConcurrencyExtensionMetadata extSvc) {
        Map<List<String>, QualifiedResourceFactory> qualifiedContextServices = //
                        list.get(QualifiedResourceFactory.Type.ContextService.ordinal());

        for (QualifiedResourceFactory factory : qualifiedContextServices.values()) {
            try {
                event.addBean(new ContextServiceBean(factory));
            } catch (Throwable x) {
                Tr.error(tc, "CWWKC1411.qualified.res.err",
                         ContextService.class.getSimpleName(),
                         ContextServiceDefinition.class.getSimpleName(),
                         "context-service",
                         factory.getName(),
                         factory.getQualifiers(),
                         toArtifactName(factory.getDeclaringMetadata()),
                         toStackTrace(x));
            }
        }

        Map<List<String>, QualifiedResourceFactory> qualifiedManagedExecutors = //
                        list.get(QualifiedResourceFactory.Type.ManagedExecutorService.ordinal());

        for (QualifiedResourceFactory factory : qualifiedManagedExecutors.values()) {
            try {
                event.addBean(new ManagedExecutorBean(factory));
            } catch (Throwable x) {
                Tr.error(tc, "CWWKC1411.qualified.res.err",
                         ManagedExecutorService.class.getSimpleName(),
                         ManagedExecutorDefinition.class.getSimpleName(),
                         "managed-executor",
                         factory.getName(),
                         factory.getQualifiers(),
                         toArtifactName(factory.getDeclaringMetadata()),
                         toStackTrace(x));
            }
        }

        Map<List<String>, QualifiedResourceFactory> qualifiedManagedScheduledExecutors = //
                        list.get(QualifiedResourceFactory.Type.ManagedScheduledExecutorService.ordinal());

        for (QualifiedResourceFactory factory : qualifiedManagedScheduledExecutors.values()) {
            try {
                event.addBean(new ManagedScheduledExecutorBean(factory));
            } catch (Throwable x) {
                Tr.error(tc, "CWWKC1411.qualified.res.err",
                         ManagedScheduledExecutorService.class.getSimpleName(),
                         ManagedScheduledExecutorDefinition.class.getSimpleName(),
                         "managed-scheduled-executor",
                         factory.getName(),
                         factory.getQualifiers(),
                         toArtifactName(factory.getDeclaringMetadata()),
                         toStackTrace(x));
            }
        }

        Map<List<String>, QualifiedResourceFactory> qualifiedManagedThreadFactories = //
                        list.get(QualifiedResourceFactory.Type.ManagedThreadFactory.ordinal());

        for (QualifiedResourceFactory factory : qualifiedManagedThreadFactories.values()) {
            try {
                event.addBean(new ManagedThreadFactoryBean(factory, extSvc));
            } catch (Throwable x) {
                Tr.error(tc, "CWWKC1411.qualified.res.err",
                         ManagedThreadFactory.class.getSimpleName(),
                         ManagedThreadFactoryDefinition.class.getSimpleName(),
                         "managed-thread-factory",
                         factory.getName(),
                         factory.getQualifiers(),
                         toArtifactName(factory.getDeclaringMetadata()),
                         toStackTrace(x));
            }
        }
    }

    /**
     * Schedule bean methods that are annotated @Schedule.
     *
     * Force context to be initialized for the default ManagedThreadFactory instance
     * if we were able to produce one.
     *
     * @param event
     * @param beanManager
     */
    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event,
                                          BeanManager beanManager) //
                    throws InvalidSyntaxException {

        // Default ManagedThreadFactory bean
        if (producedDefaultMTF) {
            CDI<Object> cdi = CDI.current();
            Instance<ManagedThreadFactory> instance = //
                            cdi.select(ManagedThreadFactory.class, new Annotation[0]);
            ManagedThreadFactory mtf = instance.get();

            // Force instantiation of the bean in order to cause context to be captured
            mtf.toString();
        }

        // Schedule annotations on bean methods
        if (!beanClassesWithScheduleMethods.isEmpty()) {
            // DefaultManagedScheduledExecutor is used by all Schedule methods
            BundleContext bc = FrameworkUtil//
                            .getBundle(WSManagedExecutorService.class) //
                            .getBundleContext();
            Collection<ServiceReference<ResourceFactory>> refs = bc == null ? //
                            List.of() : //
                            bc.getServiceReferences(ResourceFactory.class,
                                                    DEFAULT_MSES_FILTER);
            WSManagedExecutorService execSvc = refs.isEmpty() ? //
                            null : //
                            (WSManagedExecutorService) bc //
                                            .getService(refs.iterator().next());
            // TODO consider if we should unget the above on applicaton stop
            // or if it would be safe to do so at the end of this method.

            // CDIService can identify which module a bean comes from
            ServiceReference<CDIService> cdiSvcRef = bc //
                            .getServiceReference(CDIService.class);
            CDIService cdiSvc = cdiSvcRef == null ? null : bc.getService(cdiSvcRef);

            if (execSvc == null || cdiSvc == null)
                throw new IllegalStateException(); // TODO NLS message if shutting down?

            // Group bean classes by module
            Map<J2EEName, List<AnnotatedType<?>>> beanClassesPerModule = new HashMap<>();
            for (AnnotatedType<?> beanType : beanClassesWithScheduleMethods) {
                Optional<J2EEName> jeeNameOptional = cdiSvc //
                                .getModuleNameForClass(beanType.getJavaClass());
                if (jeeNameOptional.isEmpty()) {
                    // TODO NLS error message
                    throw new UnsupportedOperationException //
                    (beanType.getJavaClass().getName() +
                     " has one or more methods annotated Schedule" +
                     " but the bean class is not associated with a" +
                     " Jakarta EE application module.");
                }
                beanClassesPerModule //
                                .computeIfAbsent(jeeNameOptional.get(),
                                                 ConcurrencyExtension::newList) //
                                .add(beanType);
            }
            bc.ungetService(cdiSvcRef);

            for (Entry<J2EEName, List<AnnotatedType<?>>> group : //
            /*   */ beanClassesPerModule.entrySet()) {
                // Capture thread context for the respective module
                ThreadContextDescriptor threadContext;
                J2EEName moduleJeeName = group.getKey();
                // TODO put the correct metadata onto the thread based on the module
                try {
                    threadContext = execSvc.captureThreadContext(null);
                } finally {
                    // TODO restore previous metadata
                }

                // Schedule each method that is annotated @Schedule
                for (AnnotatedType<?> beanType : group.getValue())
                    for (AnnotatedMethod<?> method : beanType.getMethods()) {
                        Schedule schedule = method.getAnnotation(Schedule.class);
                        if (schedule != null) {
                            ArrayList<Annotation> beanAnnoList = new ArrayList<>();
                            for (Annotation beanAnno : beanType.getAnnotations())
                                if (beanAnno.annotationType() //
                                                .isAnnotationPresent(Qualifier.class))
                                    beanAnnoList.add(beanAnno);
                            Annotation[] beanAnnos = beanAnnoList //
                                            .toArray(new Annotation[beanAnnoList.size()]);
                            new ScheduledMethod<>( //
                                            method.getJavaMember(), //
                                            schedule, //
                                            threadContext, //
                                            execSvc, //
                                            beanType.getJavaClass(), //
                                            beanAnnos);
                        }
                    }
            }
        }
    }

    /**
     * Constructs a new modifiable list.
     *
     * @param <T>    type of list elements
     * @param ignore ignored value
     * @return a new modifiable list.
     */
    @Trivial
    private static final <T> ArrayList<T> newList(Object ignore) {
        return new ArrayList<T>();
    }

    /**
     * Utility method to obtain the JEE name for a MetaData instance if possible,
     * and otherwise the name.
     *
     * @param metadata ComponentMetaData, ModuleMetaData, or ApplicationMetaData.
     * @return the artifact name, preferably as a JEE name string.
     */
    @Trivial
    private static String toArtifactName(MetaData metadata) {
        if (metadata instanceof ComponentMetaData)
            return ((ComponentMetaData) metadata).getJ2EEName().toString();
        else if (metadata instanceof ModuleMetaData)
            return ((ModuleMetaData) metadata).getJ2EEName().toString();
        else if (metadata instanceof ApplicationMetaData)
            return ((ApplicationMetaData) metadata).getJ2EEName().toString();
        else
            return metadata.getName();
    }

    /**
     * Utility method that converts an exception to a stack trace string.
     *
     * @param x exception.
     * @return stack trace string.
     */
    @Trivial
    private static String toStackTrace(Throwable x) {
        StringWriter s = new StringWriter();
        x.printStackTrace(new PrintWriter(s));
        return s.toString();
    }
}