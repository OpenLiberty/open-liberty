/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.ProcessProducer;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;
import org.eclipse.microprofile.concurrent.NamedInstance;
import org.eclipse.microprofile.concurrent.ThreadContext;
import org.eclipse.microprofile.concurrent.ThreadContextConfig;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "api.classes=" +
                        "org.eclipse.microprofile.concurrent.ManagedExecutor;" +
                        "org.eclipse.microprofile.concurrent.ManagedExecutorConfig;" +
                        "org.eclipse.microprofile.concurrent.ThreadContext;" +
                        "org.eclipse.microprofile.concurrent.ThreadContextConfig",
                        "service.vendor=IBM"
           })
public class ConcurrencyCDIExtension implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(ConcurrencyCDIExtension.class);

    // insertion order of the following two data structures must match
    private final ArrayList<String> injectionPointNames = new ArrayList<>();
    private final Map<String, Annotation> instanceNameToConfig = new LinkedHashMap<>();

    private final Set<Throwable> deploymentErrors = new LinkedHashSet<>();
    private final Set<String> appDefinedProducers = new HashSet<>();

    final MPConfigAccessor mpConfigAccessor = AccessController.doPrivileged((PrivilegedAction<MPConfigAccessor>) () -> {
        BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<MPConfigAccessor> mpConfigAccessorRef = bundleContext.getServiceReference(MPConfigAccessor.class);
        return mpConfigAccessorRef == null ? null : bundleContext.getService(mpConfigAccessorRef);
    });

    Object mpConfig;

    @Trivial
    public void processManagedExecutorInjectionPoint(@Observes ProcessInjectionPoint<?, ManagedExecutor> event, BeanManager bm) {
        processInjectionPoint(ManagedExecutorConfig.class, event);
    }

    @Trivial
    public void processManagedExecutorProducer(@Observes ProcessProducer<?, ManagedExecutor> event, BeanManager bm) {
        // Save off app-defined @NamedInstance producers so we know *not* to create a bean for them
        NamedInstance producerName = event.getAnnotatedMember().getAnnotation(NamedInstance.class);
        if (producerName != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Found app-defined producer for: name=" + producerName.value());
            appDefinedProducers.add(producerName.value());
        }
    }

    @Trivial
    public void processThreadContextInjectionPoint(@Observes ProcessInjectionPoint<?, ThreadContext> event, BeanManager bm) {
        processInjectionPoint(ThreadContextConfig.class, event);
    }

    @Trivial
    public void processThreadContextProducer(@Observes ProcessProducer<?, ThreadContext> event, BeanManager bm) {
        // Save off app-defined @NamedInstance producers so we know *not* to create a bean for them
        NamedInstance producerName = event.getAnnotatedMember().getAnnotation(NamedInstance.class);
        if (producerName != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Found app-defined producer for: name=" + producerName.value());
            appDefinedProducers.add(producerName.value());
        }
    }

    @Trivial
    public void processInjectionPoint(Class<? extends Annotation> configAnnoClass, ProcessInjectionPoint<?, ?> event) {
        Annotated injectionPoint = event.getInjectionPoint().getAnnotated();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "processInjectionPoint " + injectionPoint);

        Annotation config = injectionPoint.getAnnotation(configAnnoClass);
        boolean configAnnoPresent = config != null;
        if (config == null)
            config = ManagedExecutorConfig.class.equals(configAnnoClass) //
                            ? ManagedExecutorConfig.Literal.DEFAULT_INSTANCE //
                            : ThreadContextConfig.Literal.DEFAULT_INSTANCE;

        // Instance name is either @NamedInstance.value() or generated from fully-qualified field name or method parameter index
        NamedInstance nameAnno = injectionPoint.getAnnotation(NamedInstance.class);
        Member member = event.getInjectionPoint().getMember();
        StringBuilder n = new StringBuilder(member.getDeclaringClass().getTypeName()).append('.').append(member.getName());
        if (injectionPoint instanceof AnnotatedParameter)
            n.append('.').append(((AnnotatedParameter<?>) injectionPoint).getPosition() + 1); // switch from 0-based to 1-based
        String injectionPointName = n.toString();
        String instanceName = nameAnno == null ? injectionPointName : nameAnno.value();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "InjectionPoint " + injectionPointName + " has config " + (configAnnoPresent ? config : "DEFAULT_INSTANCE"),
                     nameAnno == null ? null : instanceName);
        // Automatically insert @NamedInstance("<generated name>") qualifier for @Default injection points
        if (nameAnno == null && event.getInjectionPoint().getQualifiers().contains(Default.Literal.INSTANCE))
            event.configureInjectionPoint().addQualifiers(NamedInstance.Literal.of(instanceName));

        // The container MUST register a bean if @ManagedExecutorConfig/ThreadContextConfig is present
        // The container MUST register a bean for every @Default injection point
        if (configAnnoPresent || event.getInjectionPoint().getQualifiers().contains(Default.Literal.INSTANCE)) {
            Annotation previousConfig = instanceNameToConfig.putIfAbsent(instanceName, config);
            if (previousConfig == null) {
                injectionPointNames.add(injectionPointName);
            } else { // It is an error if 2 or more InjectionPoints define @NamedInstance("X") @ManagedExecutorConfig/ThreadContextConfig
                String msg = "ERROR: Found existing bean with name=" + instanceName; // TODO NLS message
                Tr.error(tc, msg);
                deploymentErrors.add(new Throwable(msg)); // TODO proper exception class
            }
        }
    }

    public void registerBeans(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        mpConfig = mpConfigAccessor == null ? null : mpConfigAccessor.getConfig();

        // Always register a bean for @Default programmatic CDI lookups
        event.addBean(new ManagedExecutorBean(this));

        // Don't register beans for app-defined @NamedInstance producers
        for (String appDefinedProducer : appDefinedProducers) {
            instanceNameToConfig.remove(appDefinedProducer);
        }

        // Register 1 bean per un-named config, and 1 bean per unique NamedInstance
        int i = 0;
        for (Entry<String, Annotation> e : instanceNameToConfig.entrySet()) {
            String injectionPointName = injectionPointNames.get(i++);
            Annotation configAnno = e.getValue();
            if (configAnno instanceof ManagedExecutorConfig)
                event.addBean(new ManagedExecutorBean(injectionPointName, e.getKey(), (ManagedExecutorConfig) configAnno, this));
            else // configAnno instanceof ThreadContextConfig
                event.addBean(new ThreadContextBean(injectionPointName, e.getKey(), (ThreadContextConfig) configAnno, this));
        }
    }

    public void registerErrors(@Observes AfterDeploymentValidation event) {
        deploymentErrors.forEach(event::addDeploymentProblem);
        deploymentErrors.clear();
    }
}
