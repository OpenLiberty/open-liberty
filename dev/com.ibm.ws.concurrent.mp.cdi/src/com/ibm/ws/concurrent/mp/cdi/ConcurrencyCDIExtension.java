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

import java.lang.reflect.Member;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
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

    private final Map<String, ManagedExecutorConfig> beanMap = new HashMap<>();
    private final Set<Throwable> deploymentErrors = new LinkedHashSet<>();
    private final Set<String> appDefinedProducers = new HashSet<>();

    final MPConfigAccessor mpConfigAccessor = AccessController.doPrivileged((PrivilegedAction<MPConfigAccessor>) () -> {
        BundleContext bundleContext = FrameworkUtil.getBundle(ManagedExecutorBean.class).getBundleContext();
        ServiceReference<MPConfigAccessor> mpConfigAccessorRef = bundleContext.getServiceReference(MPConfigAccessor.class);
        return mpConfigAccessorRef == null ? null : bundleContext.getService(mpConfigAccessorRef);
    });

    Object mpConfig;

    @Trivial
    public void processProducer(@Observes ProcessProducer<?, ManagedExecutor> event, BeanManager bm) {
        // Save off app-defined @NamedInstance producers so we know *not* to create a bean for them
        NamedInstance producerName = event.getAnnotatedMember().getAnnotation(NamedInstance.class);
        if (producerName != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Found app-defined producer for: name=" + producerName.value());
            appDefinedProducers.add(producerName.value());
        }
    }

    @Trivial
    public void processInjectionPoint(@Observes ProcessInjectionPoint<?, ManagedExecutor> event, BeanManager bm) {
        Annotated injectionPoint = event.getInjectionPoint().getAnnotated();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "processInjectionPoint " + injectionPoint);

        ManagedExecutorConfig config = injectionPoint.getAnnotation(ManagedExecutorConfig.class);
        boolean configAnnoPresent = config != null;
        if (config == null)
            config = ManagedExecutorConfig.Literal.DEFAULT_INSTANCE;

        // Instance name is either @NamedInstance.value() or generated from fully-qualified field name or method parameter index
        NamedInstance nameAnno = injectionPoint.getAnnotation(NamedInstance.class);
        Member member = event.getInjectionPoint().getMember();
        String name;
        if (nameAnno == null) {
            StringBuilder n = new StringBuilder(member.getDeclaringClass().getTypeName()).append('.').append(member.getName());
            if (injectionPoint instanceof AnnotatedParameter)
                n.append('.').append(((AnnotatedParameter<?>) injectionPoint).getPosition() + 1); // switch from 0-based to 1-based
            name = n.toString();
        } else
            name = nameAnno.value();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "InjectionPoint " + name + " has config " + (configAnnoPresent ? config : "DEFAULT_INSTNACE"));
        // Automatically insert @NamedInstance("<generated name>") qualifier for @Default injection points
        if (nameAnno == null && event.getInjectionPoint().getQualifiers().contains(Default.Literal.INSTANCE))
            event.configureInjectionPoint().addQualifiers(NamedInstance.Literal.of(name));

        // The container MUST register a bean if @MEC is present
        // The container MUST register a bean for every @Default injection point
        if (configAnnoPresent || event.getInjectionPoint().getQualifiers().contains(Default.Literal.INSTANCE)) {
            ManagedExecutorConfig previousConfig = beanMap.putIfAbsent(name, config);

            // If 2 or more InjectionPoints define @NamedInstance("X") @ManagedExecutorConfig it is an error
            if (previousConfig != null) {
                String msg = "ERROR: Found existing bean with name=" + name; // TODO NLS message
                Tr.error(tc, msg);
                deploymentErrors.add(new Throwable(msg));
            }
        }
    }

    public void registerBeans(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        mpConfig = mpConfigAccessor == null ? null : mpConfigAccessor.getConfig();

        // Always register a bean for @Default programmatic CDI lookups
        event.addBean(new ManagedExecutorBean(this));

        // Don't register beans for app-defined @NamedInstance producers
        for (String appDefinedProducer : appDefinedProducers) {
            beanMap.remove(appDefinedProducer);
        }

        // Register 1 bean per un-named config, and 1 bean per unique NamedInstance
        for (Entry<String, ManagedExecutorConfig> e : beanMap.entrySet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Add bean for name=" + e.getKey());
            event.addBean(new ManagedExecutorBean(e.getKey(), e.getValue(), this));
        }
    }

    public void registerErrors(@Observes AfterDeploymentValidation event) {
        deploymentErrors.forEach(event::addDeploymentProblem);
        deploymentErrors.clear();
    }
}
