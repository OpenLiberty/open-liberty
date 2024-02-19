/*******************************************************************************
 * Copyright (c) 2021,2024 IBM Corporation and others.
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.concurrent.internal.cdi.interceptor.AsyncInterceptor;
import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactories;
import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactory;
import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
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
    private static final Annotation[] DEFAULT_QUALIFIER_ARRAY = new Annotation[] { Default.Literal.INSTANCE };

    private static final Set<Annotation> DEFAULT_QUALIFIER_SET = Set.of(Default.Literal.INSTANCE);

    /**
     * List of the qualifier sets for each ManagedThreadFactory bean with qualifiers that is
     * created during afterBeanDiscovery. Instances of these beans are obtained during
     * afterDeploymentValidation to force context capture to occur.
     */
    private List<Set<Annotation>> qualifierSetsPerMTF;

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

        BundleContext bundleContext = FrameworkUtil.getBundle(ConcurrencyExtension.class).getBundleContext();
        ServiceReference<QualifiedResourceFactories> ref = bundleContext.getServiceReference(QualifiedResourceFactories.class);
        ConcurrencyExtensionMetadata ext = (ConcurrencyExtensionMetadata) bundleContext.getService(ref);

        // Add beans for Concurrency default resources if not already present:

        CDI<Object> cdi = CDI.current();

        if (!cdi.select(ContextService.class, DEFAULT_QUALIFIER_ARRAY).isResolvable())
            event.addBean(new ContextServiceBean(ext.defaultContextServiceFactory, DEFAULT_QUALIFIER_SET));

        if (!cdi.select(ManagedExecutorService.class, DEFAULT_QUALIFIER_ARRAY).isResolvable())
            event.addBean(new ManagedExecutorBean(ext.defaultManagedExecutorFactory, DEFAULT_QUALIFIER_SET));

        if (!cdi.select(ManagedScheduledExecutorService.class, DEFAULT_QUALIFIER_ARRAY).isResolvable())
            event.addBean(new ManagedScheduledExecutorBean(ext.defaultManagedScheduledExecutorFactory, DEFAULT_QUALIFIER_SET));

        if (!cdi.select(ManagedThreadFactory.class, DEFAULT_QUALIFIER_ARRAY).isResolvable()) {
            event.addBean(new ManagedThreadFactoryBean(ext.defaultManagedThreadFactoryFactory, DEFAULT_QUALIFIER_SET));
            qualifierSetsPerMTF = new ArrayList<>();
            qualifierSetsPerMTF.add(Collections.emptySet());
        }

        // Look for beans from the module and the application.
        // TODO EJBs and component level?

        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd == null)
            throw new IllegalStateException(); // should be unreachable

        J2EEName jeeName = cmd.getJ2EEName();

        List<Map<List<String>, QualifiedResourceFactory>> listFromModule = ext.removeAll(cmd.getJ2EEName().toString());

        if (listFromModule != null)
            addBeans(event, listFromModule);

        List<Map<List<String>, QualifiedResourceFactory>> listFromApp = ext.removeAll(jeeName.getApplication());
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
                // TODO NLS
                System.out.println(" E Unable to create a bean for the " +
                                   factory + " ContextServiceDefinition with the " + factory.getQualifiers() + " qualifiers" +
                                   " due to the following error: ");
                x.printStackTrace();
            }
        }

        Map<List<String>, QualifiedResourceFactory> qualifiedManagedExecutors = //
                        list.get(QualifiedResourceFactory.Type.ManagedExecutorService.ordinal());

        for (QualifiedResourceFactory factory : qualifiedManagedExecutors.values()) {
            try {
                event.addBean(new ManagedExecutorBean(factory));
            } catch (Throwable x) {
                // TODO NLS
                System.out.println(" E Unable to create a bean for the " +
                                   factory + " ManagedExecutorDefinition with the " + factory.getQualifiers() + " qualifiers" +
                                   " due to the following error: ");
                x.printStackTrace();
            }
        }

        Map<List<String>, QualifiedResourceFactory> qualifiedManagedScheduledExecutors = //
                        list.get(QualifiedResourceFactory.Type.ManagedScheduledExecutorService.ordinal());

        for (QualifiedResourceFactory factory : qualifiedManagedScheduledExecutors.values()) {
            try {
                event.addBean(new ManagedScheduledExecutorBean(factory));
            } catch (Throwable x) {
                // TODO NLS
                System.out.println(" E Unable to create a bean for the " +
                                   factory + " ManagedScheduledExecutorDefinition with the " + factory.getQualifiers() + " qualifiers" +
                                   " due to the following error: ");
                x.printStackTrace();
            }
        }

        Map<List<String>, QualifiedResourceFactory> qualifiedManagedThreadFactories = //
                        list.get(QualifiedResourceFactory.Type.ManagedThreadFactory.ordinal());

        for (QualifiedResourceFactory factory : qualifiedManagedThreadFactories.values()) {
            try {
                event.addBean(new ManagedThreadFactoryBean(factory));
            } catch (Throwable x) {
                // TODO NLS
                System.out.println(" E Unable to create a bean for the " +
                                   factory + " ManagedThreadFactoryDefinition with the " + factory.getQualifiers() + " qualifiers" +
                                   " due to the following error: ");
                x.printStackTrace();
            }
        }
    }

    /**
     * Force context to be initialized for each ManagedThreadFactory that we registered a bean for.
     *
     * @param event
     * @param beanManager
     */
    public void afterDeploymentValidation(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
        // TODO remove this once we handle the default instances similar to the qualified instances
        if (qualifierSetsPerMTF != null) {
            CDI<Object> cdi = CDI.current();

            for (Set<Annotation> qualifierSet : qualifierSetsPerMTF) {
                Instance<ManagedThreadFactory> instance = cdi.select(ManagedThreadFactory.class, qualifierSet.toArray(new Annotation[qualifierSet.size()]));
                ManagedThreadFactory mtf = instance.get();
                // Force instantiation of the bean in order to cause context to be captured
                mtf.toString();
            }
            qualifierSetsPerMTF = null;
        }
    }
}