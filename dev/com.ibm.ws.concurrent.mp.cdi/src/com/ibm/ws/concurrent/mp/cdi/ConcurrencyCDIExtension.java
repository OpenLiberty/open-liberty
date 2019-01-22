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

    // This instance is used as a marker for when no configured is specified for a String[]. The reference is compared; its content does not matter.
    private static final String[] UNSPECIFIED_ARRAY = new String[] {};

    // insertion order of the following two data structures must match
    private final ArrayList<String> injectionPointNames = new ArrayList<>();
    private final Map<String, Object> instances = new LinkedHashMap<>();

    private final Set<Throwable> deploymentErrors = new LinkedHashSet<>();
    private final Set<String> appDefinedProducers = new HashSet<>();

    private final MPConfigAccessor mpConfigAccessor = AccessController.doPrivileged((PrivilegedAction<MPConfigAccessor>) () -> {
        BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference<MPConfigAccessor> mpConfigAccessorRef = bundleContext.getServiceReference(MPConfigAccessor.class);
        return mpConfigAccessorRef == null ? null : bundleContext.getService(mpConfigAccessorRef);
    });

    private Object mpConfig;

    /**
     * Construct an instance of ManagedExecutor to be injected into the specified injection point.
     *
     * @param injectionPointName identifier for the injection point.
     * @param config configuration attributes for the new instance.
     * @return new instance of ManagedExecutor.
     */
    private ManagedExecutor createManagedExecutor(String injectionPointName, ManagedExecutorConfig config) {
        ManagedExecutor.Builder b = ManagedExecutor.builder();
        mpConfig = mpConfigAccessor == null ? null : mpConfigAccessor.getConfig();
        if (mpConfig == null) {
            if (config != null) {
                b.cleared(config.cleared());
                b.maxAsync(config.maxAsync());
                b.maxQueued(config.maxQueued());
                b.propagated(config.propagated());
            }
        } else {
            int start = injectionPointName.length() + 1;
            int len = start + 10;
            StringBuilder propName = new StringBuilder(len).append(injectionPointName).append('.');

            // In order to efficiently reuse StringBuilder, properties are added in the order of the length of their names,

            propName.append("cleared");
            String[] c = mpConfigAccessor.get(mpConfig, propName.toString(), config == null ? UNSPECIFIED_ARRAY : config.cleared());
            if (c != UNSPECIFIED_ARRAY)
                b.cleared(c);

            propName.replace(start, len, "maxAsync");
            Integer a = mpConfigAccessor.get(mpConfig, propName.toString(), config == null ? null : config.maxAsync());
            if (a != null)
                b.maxAsync(a);

            propName.replace(start, len, "maxQueued");
            Integer q = mpConfigAccessor.get(mpConfig, propName.toString(), config == null ? null : config.maxQueued());
            if (q != null)
                b.maxQueued(q);

            propName.replace(start, len, "propagated");
            String[] p = mpConfigAccessor.get(mpConfig, propName.toString(), config == null ? UNSPECIFIED_ARRAY : config.propagated());
            if (p != UNSPECIFIED_ARRAY)
                b.propagated(p);
        }

        return b.build();
    }

    /**
     * Destroy all instances that were previously created.
     */
    private void destroy() {
        appDefinedProducers.clear();
        injectionPointNames.clear();
        for (Object instance : instances.values()) {
            if (instance instanceof ManagedExecutor)
                ((ManagedExecutor) instance).shutdownNow();
        }
        instances.clear();
    }

    /**
     * Construct an instance of ThreadContext to be injected into the specified injection point.
     *
     * @param injectionPointName identifier for the injection point.
     * @param config configuration attributes for the new instance.
     * @return new instance of ThreadContext.
     */
    private ThreadContext createThreadContext(String injectionPointName, ThreadContextConfig config) {
        ThreadContext.Builder b = ThreadContext.builder();
        mpConfig = mpConfigAccessor == null ? null : mpConfigAccessor.getConfig();
        if (mpConfig == null) {
            if (config != null) {
                b.cleared(config.cleared());
                b.unchanged(config.unchanged());
                b.propagated(config.propagated());
            }
        } else {
            int start = injectionPointName.length() + 1;
            int len = start + 10;
            StringBuilder propName = new StringBuilder(len).append(injectionPointName).append('.');

            // In order to efficiently reuse StringBuilder, properties are added in the order of the length of their names,

            propName.append("cleared");
            String[] c = mpConfigAccessor.get(mpConfig, propName.toString(), config == null ? UNSPECIFIED_ARRAY : config.cleared());
            if (c != UNSPECIFIED_ARRAY)
                b.cleared(c);

            propName.replace(start, len, "unchanged");
            String[] u = mpConfigAccessor.get(mpConfig, propName.toString(), config == null ? UNSPECIFIED_ARRAY : config.unchanged());
            if (u != UNSPECIFIED_ARRAY)
                b.unchanged(u);

            propName.replace(start, len, "propagated");
            String[] p = mpConfigAccessor.get(mpConfig, propName.toString(), config == null ? UNSPECIFIED_ARRAY : config.propagated());
            if (p != UNSPECIFIED_ARRAY)
                b.propagated(p);
        }

        return b.build();
    }

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
            Object instance = null;
            try {
                if (config instanceof ManagedExecutorConfig)
                    instance = createManagedExecutor(injectionPointName, (ManagedExecutorConfig) config);
                else // config instanceof ThreadContextConfig
                    instance = createThreadContext(injectionPointName, (ThreadContextConfig) config);
            } catch (IllegalArgumentException x) {
                event.addDefinitionError(x);
                destroy();
            } catch (IllegalStateException x) {
                String message = x.getMessage();
                if (message != null && message.startsWith("CWWKC") && !message.startsWith("CWWKC1150E"))
                    event.addDefinitionError(x);
                else
                    deploymentErrors.add(x);
                destroy();
            }

            if (instance != null) {
                Object previous = instances.putIfAbsent(instanceName, instance);
                if (previous == null) {
                    injectionPointNames.add(injectionPointName);
                } else { // It is an error if 2 or more InjectionPoints define @NamedInstance("X") @ManagedExecutorConfig/ThreadContextConfig
                    if (instance instanceof ManagedExecutor)
                        ((ManagedExecutor) instance).shutdownNow();
                    String msg = "ERROR: Found existing bean with name=" + instanceName; // TODO NLS message
                    Tr.error(tc, msg);
                    deploymentErrors.add(new Throwable(msg)); // TODO proper exception class
                    destroy();
                }
            }
        }
    }

    public void registerBeans(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        // Don't register beans for app-defined @NamedInstance producers
        for (String appDefinedProducer : appDefinedProducers) {
            instances.remove(appDefinedProducer);
        }

        // Register 1 bean per un-named config, and 1 bean per unique NamedInstance
        int i = 0;
        for (Entry<String, ?> e : instances.entrySet()) {
            String injectionPointName = injectionPointNames.get(i++);
            Object instance = e.getValue();
            if (instance instanceof ManagedExecutor) {
                event.addBean(new ManagedExecutorBean(injectionPointName, e.getKey(), (ManagedExecutor) instance));
            } else // instance instanceof ThreadContext
                event.addBean(new ThreadContextBean(injectionPointName, e.getKey(), (ThreadContext) instance));
        }

        appDefinedProducers.clear();
        injectionPointNames.clear();
        instances.clear();
    }

    public void registerErrors(@Observes AfterDeploymentValidation event) {
        deploymentErrors.forEach(event::addDeploymentProblem);
        deploymentErrors.clear();
    }
}
