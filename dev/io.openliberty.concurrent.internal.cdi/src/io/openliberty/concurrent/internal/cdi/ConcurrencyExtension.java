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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.osgi.framework.InvalidSyntaxException;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIService;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.kernel.service.util.ServiceCaller;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

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
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

/**
 * CDI Extension for
 * Jakarta Concurrency 3.1/CDI 4.1 in Jakarta EE 11 and
 * Jakarta Concurrency 3.2/CDI 5.0 in Jakarta EE 12
 */
public class ConcurrencyExtension implements Extension {
    private static final TraceComponent tc = Tr.register(ConcurrencyExtension.class);

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
    final List<AnnotatedType<?>> beanClassesWithScheduleMethods = new ArrayList<>();

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
     * Arranges for methods annotated Schedule to be scheduled upon application
     * start.
     *
     * @param event the event
     */
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {

        ComponentMetaData cmd = ComponentMetaDataAccessorImpl //
                        .getComponentMetaDataAccessor().getComponentMetaData();
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

            // For bean classes with a method annotated @Schedule, group by the
            // module that provides the bean class
            // Group bean classes by module
            if (!beanClassesWithScheduleMethods.isEmpty()) {
                Map<J2EEName, List<AnnotatedType<?>>> perModule = new HashMap<>();
                CDIService cdiSvc = extSvc.cdiServiceRef.getServiceWithException();
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
                    perModule.computeIfAbsent(jeeNameOptional.get(),
                                              ConcurrencyExtension::newList) //
                                    .add(beanType);
                }
                beanClassesWithScheduleMethods.clear();

                // Arrange for methods with Schedule annotations to be scheduled when
                // the application starts
                if (!perModule.isEmpty())
                    extSvc.scheduleOnAppStart(jeeName.getApplication(), perModule);
            }
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
     * Force context to be initialized for the default ManagedThreadFactory instance
     * if we were able to produce one.
     *
     * @param event the event
     */
    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event) //
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