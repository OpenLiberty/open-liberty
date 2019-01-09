/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessInjectionPoint;

import org.eclipse.microprofile.concurrent.ManagedExecutor;
import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;
import org.eclipse.microprofile.concurrent.NamedInstance;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
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

    public void processInjectionPoint(@Observes ProcessInjectionPoint<?, ManagedExecutor> event, BeanManager bm) {
        Annotated injectionPoint = event.getInjectionPoint().getAnnotated();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "processInjectionPoint " + injectionPoint);

        // Skip if no @ManagedExecutorConfig or module is not CDI enabled
        if (!injectionPoint.isAnnotationPresent(ManagedExecutorConfig.class))
            return;

        ManagedExecutorConfig config = injectionPoint.getAnnotation(ManagedExecutorConfig.class);

        // Instance name is either @NamedInstance.value() or generated from fully-qualified class+field name
        NamedInstance nameAnno = injectionPoint.getAnnotation(NamedInstance.class);
        Member member = event.getInjectionPoint().getMember();
        String name = nameAnno == null ? member.getDeclaringClass().getTypeName() + "." + member.getName() : nameAnno.value();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "InjectionPoint " + name + " has config " + config);
        if (nameAnno == null)
            event.configureInjectionPoint().addQualifiers(createNamedInstance(name));
        ManagedExecutorConfig prevoiusConfig = beanMap.putIfAbsent(name, config);

        // If 2 or more InjectionPoints define @NamedInstance("X") @ManagedExecutorConfig it is an error
        if (prevoiusConfig != null) {
            String msg = "ERROR: Found existing bean with name=" + name; // TODO NLS message
            Tr.error(tc, msg);
            deploymentErrors.add(new Throwable(msg));
        }
    }

    public void registerBeans(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        // TODO: Right now this is coded to give 1 ManagedExecutor instance for all @Default injection points
        // wait to see how discussion on this PR ends up: https://github.com/eclipse/microprofile-concurrency/pull/54
        // Always register bean for default instance(s)
        event.addBean(new ManagedExecutorBean());

        // Register 1 bean per un-named config, and 1 bean per unique NamedInstance
        for (Entry<String, ManagedExecutorConfig> e : beanMap.entrySet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Add bean for name=" + e.getKey());
            event.addBean(new ManagedExecutorBean(e.getKey(), e.getValue()));
        }
    }

    public void registerErrors(@Observes AfterDeploymentValidation event) {
        deploymentErrors.forEach(event::addDeploymentProblem);
        deploymentErrors.clear();
    }

    @Deprecated // TODO this can be replaced with a spec-provided Literal soon
    public static NamedInstance createNamedInstance(final String name) {
        return new NamedInstance() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return NamedInstance.class;
            }

            @Override
            public String value() {
                return name;
            }
        };
    }

    @Deprecated // TODO this can be replaced with a spec-provided Literal soon
    public static final ManagedExecutorConfig MEC_LITERAL = new ManagedExecutorConfig() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return ManagedExecutorConfig.class;
        }

        @Override
        public String[] propagated() {
            return new String[] { "Remaining" };
        }

        @Override
        public int maxQueued() {
            return -1;
        }

        @Override
        public int maxAsync() {
            return -1;
        }

        @Override
        public String[] cleared() {
            return new String[] { "Transaction" };
        }
    };

}