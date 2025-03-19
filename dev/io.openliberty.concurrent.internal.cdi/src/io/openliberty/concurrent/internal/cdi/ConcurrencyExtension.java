/*******************************************************************************
 * Copyright (c) 2021,2025 IBM Corporation and others.
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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.concurrent.internal.cdi.interceptor.AsyncInterceptor;
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

/**
 * CDI Extension for Jakarta Concurrency 3.1+ in Jakarta EE 11+, which corresponds to CDI 4.1+
 */
public class ConcurrencyExtension implements Extension {
    private static final TraceComponent tc = Tr.register(ConcurrencyExtension.class);

    private static final Annotation[] DEFAULT_QUALIFIER_ARRAY = new Annotation[] { Default.Literal.INSTANCE };

    private static final Set<Annotation> DEFAULT_QUALIFIER_SET = Set.of(Default.Literal.INSTANCE);

    /**
     * OSGi service for the Concurrency extension;
     */
    private ConcurrencyExtensionMetadata extSvc;

    /**
     * Indicates if we were able to create a default ManagedThreadFactory bean.
     * If so, an instance of the bean is obtained during afterDeploymentValidation
     * to force context capture to occur.
     */
    private boolean producedDefaultMTF;

    /**
     * Register interceptors before bean discovery.
     *
     * @param beforeBeanDiscovery
     * @param beanManager
     */
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        // register the interceptor binding and the interceptor
        AnnotatedType<Asynchronous> bindingType = beanManager.createAnnotatedType(Asynchronous.class);
        beforeBeanDiscovery.addInterceptorBinding(bindingType);
        AnnotatedType<AsyncInterceptor> interceptorType = beanManager.createAnnotatedType(AsyncInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(interceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(interceptorType, this.getClass()));
    }

    /**
     * Register beans for default instances and qualified instances of concurrency resources after bean discovery.
     *
     * @param event
     * @param beanManager
     */
    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanManager) {

        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd == null)
            throw new IllegalStateException(); // should be unreachable

        J2EEName jeeName = cmd.getJ2EEName();

        BundleContext bundleContext = FrameworkUtil.getBundle(ConcurrencyExtension.class).getBundleContext();
        ServiceReference<QualifiedResourceFactories> ref = bundleContext.getServiceReference(QualifiedResourceFactories.class);
        extSvc = (ConcurrencyExtensionMetadata) bundleContext.getService(ref);

        // Add beans for Concurrency default resources if not already present:

        CDI<Object> cdi = CDI.current();

        if (!cdi.select(ContextService.class, DEFAULT_QUALIFIER_ARRAY).isResolvable())
            event.addBean(new ContextServiceBean(extSvc.defaultContextServiceFactory, DEFAULT_QUALIFIER_SET));

        if (!cdi.select(ManagedExecutorService.class, DEFAULT_QUALIFIER_ARRAY).isResolvable())
            event.addBean(new ManagedExecutorBean(extSvc.defaultManagedExecutorFactory, DEFAULT_QUALIFIER_SET));

        if (!cdi.select(ManagedScheduledExecutorService.class, DEFAULT_QUALIFIER_ARRAY).isResolvable())
            event.addBean(new ManagedScheduledExecutorBean(extSvc.defaultManagedScheduledExecutorFactory, DEFAULT_QUALIFIER_SET));

        if (!cdi.select(ManagedThreadFactory.class, DEFAULT_QUALIFIER_ARRAY).isResolvable()) {
            event.addBean(new ManagedThreadFactoryBean(cmd, extSvc, DEFAULT_QUALIFIER_SET));
            producedDefaultMTF = true;
        }

        // Look for beans from the module and the application.
        // TODO EJBs and component level?

        List<Map<List<String>, QualifiedResourceFactory>> listFromModule = extSvc.removeAll(cmd.getJ2EEName().toString());

        if (listFromModule != null)
            addBeans(event, listFromModule);

        List<Map<List<String>, QualifiedResourceFactory>> listFromApp = extSvc.removeAll(jeeName.getApplication());
        if (listFromApp != null)
            addBeans(event, listFromApp);
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
    private void addBeans(AfterBeanDiscovery event, List<Map<List<String>, QualifiedResourceFactory>> list) {
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
     * @param event
     * @param beanManager
     */
    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event,
                                          BeanManager beanManager) {
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