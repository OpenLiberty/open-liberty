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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.resource.ResourceFactory;

import io.openliberty.concurrent.internal.cdi.interceptor.AsyncInterceptor;
import io.openliberty.concurrent.internal.qualified.QualifiedResourceFactories;
import jakarta.enterprise.concurrent.Asynchronous;
import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessInjectionPoint;

/**
 * CDI Extension for Jakarta Concurrency 3.1+ in Jakarta EE 11+, which corresponds to CDI 4.1+
 */
public class ConcurrencyExtension implements Extension {
    private static final TraceComponent tc = Tr.register(ConcurrencyExtension.class);

    private static final Annotation[] DEFAULT_QUALIFIER_ARRAY = new Annotation[] { Default.Literal.INSTANCE };

    private static final Set<Annotation> DEFAULT_QUALIFIER_SET = Set.of(Default.Literal.INSTANCE);

    /**
     * Set of qualifier lists found on ManagedExecutorService injection points.
     */
    private final Set<Set<Annotation>> executorQualifiers = new HashSet<>();

    /**
     * Set of qualifier lists found on ManagedScheduledExecutorService injection points.
     */
    private final Set<Set<Annotation>> scheduledExecutorQualifiers = new HashSet<>();

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        // register the interceptor binding and the interceptor
        AnnotatedType<Asynchronous> bindingType = beanManager.createAnnotatedType(Asynchronous.class);
        beforeBeanDiscovery.addInterceptorBinding(bindingType);
        AnnotatedType<AsyncInterceptor> interceptorType = beanManager.createAnnotatedType(AsyncInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(interceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(interceptorType, this.getClass()));
    }

    /**
     * Invoked for each matching injection point:
     *
     * @Inject {@Qualifier1 @Qualifier2 ...} ManagedExecutorService executor;
     *
     * @param <T>   bean class that has the injection point
     * @param event event
     */
    public <T> void processExecutorInjectionPoint(@Observes ProcessInjectionPoint<T, ManagedExecutorService> event) {
        InjectionPoint injectionPoint = event.getInjectionPoint();
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        executorQualifiers.add(qualifiers);
    }

    /**
     * Invoked for each matching injection point:
     *
     * @Inject {@Qualifier1 @Qualifier2 ...} ManagedScheduledExecutorService scheduledExecutor;
     *
     * @param <T>   bean class that has the injection point
     * @param event event
     */
    public <T> void processScheduledExecutorInjectionPoint(@Observes ProcessInjectionPoint<T, ManagedScheduledExecutorService> event) {
        InjectionPoint injectionPoint = event.getInjectionPoint();
        Set<Annotation> qualifiers = injectionPoint.getQualifiers();
        scheduledExecutorQualifiers.add(qualifiers);
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        BundleContext bundleContext = FrameworkUtil.getBundle(ConcurrencyExtension.class).getBundleContext();
        ServiceReference<QualifiedResourceFactories> ref = bundleContext.getServiceReference(QualifiedResourceFactories.class);
        ConcurrencyExtensionMetadata ext = (ConcurrencyExtensionMetadata) bundleContext.getService(ref);

        CDI<Object> cdi = CDI.current();

        if (!cdi.select(ContextService.class, DEFAULT_QUALIFIER_ARRAY).isResolvable())
            event.addBean(new ContextServiceBean(ext.defaultContextServiceFactory, DEFAULT_QUALIFIER_SET));

        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (cmd == null)
            throw new IllegalStateException(); // should be unreachable

        List<Map<List<String>, ResourceFactory>> list = ext.removeAll(cmd.getName());

        if (list != null) {
            Map<List<String>, ResourceFactory> qualifiedContextServices = list.get(QualifiedResourceFactories.Type.ContextService.ordinal());
            for (Entry<List<String>, ResourceFactory> entry : qualifiedContextServices.entrySet()) {
                List<String> qualifierList = entry.getKey();
                ResourceFactory factory = entry.getValue();
                try {
                    event.addBean(new ContextServiceBean(factory, qualifierList));
                } catch (Throwable x) {
                    // TODO NLS
                    System.out.println(" E Unable to create a bean for the " +
                                       factory + " ContextServiceDefinition with the " + qualifierList + " qualifiers" +
                                       " due to the following error: ");
                    x.printStackTrace();
                }
            }
        }

        for (Set<Annotation> qualifiers : executorQualifiers) {
            if (cdi.select(ManagedExecutorService.class, qualifiers.toArray(new Annotation[qualifiers.size()])).isResolvable()) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "ManagedExecutorService already exists with qualifiers " + qualifiers);
            } else {
                // It doesn't already exist, so try to add it:
                String filter = null;
                if (DEFAULT_QUALIFIER_SET.equals(qualifiers)) {
                    filter = "(id=DefaultManagedExecutorService)";
                } else { // TODO replace this temporary approach to partially simulate spec function
                    // The filter on config.displayId prevents matching scheduled executor instances with the same qualifiers
                    StringBuilder f = new StringBuilder().append("(&(config.displayId=*managedExecutorService*)");
                    for (Annotation q : qualifiers)
                        f.append("(qualifiers=").append(q.annotationType().getName()).append(')');
                    filter = f.append(')').toString();
                }

                event.addBean(new ManagedExecutorBean(filter, qualifiers));

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Added ManagedExecutorService bean with qualifiers " + qualifiers);
            }
        }
        executorQualifiers.clear();

        for (Set<Annotation> qualifiers : scheduledExecutorQualifiers) {
            if (cdi.select(ManagedScheduledExecutorService.class, qualifiers.toArray(new Annotation[qualifiers.size()])).isResolvable()) {
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "ManagedScheduledExecutorService already exists with qualifiers " + qualifiers);
            } else {
                // It doesn't already exist, so try to add it:
                String filter = null;
                if (DEFAULT_QUALIFIER_SET.equals(qualifiers)) {
                    filter = "(id=DefaultManagedScheduledExecutorService)";
                } else { // TODO replace this temporary approach to partially simulate spec function
                    StringBuilder f = new StringBuilder().append("(&");
                    for (Annotation q : qualifiers)
                        f.append("(qualifiers=").append(q.annotationType().getName()).append(')');
                    filter = f.append(')').toString();
                }

                event.addBean(new ManagedScheduledExecutorBean(filter, qualifiers));

                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc, "Added ManagedScheduledExecutorService bean with qualifiers " + qualifiers);
            }
        }
        scheduledExecutorQualifiers.clear();
    }
}