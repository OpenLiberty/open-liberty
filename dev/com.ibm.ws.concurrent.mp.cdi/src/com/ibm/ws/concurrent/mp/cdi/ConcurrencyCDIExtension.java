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
import java.security.AccessController;
import java.security.PrivilegedAction;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.CDIService;
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

    private final Map<String, Map<String, ManagedExecutorConfig>> beanMap = new HashMap<>(4);
    private final Set<Throwable> deploymentErrors = new LinkedHashSet<>();

    // Cannot use proper DS @Reference here because CDI Extensions are created/used as unmanaged objects
    private CDIService cdiSvc;

    public void processInjectionPoint(@Observes ProcessInjectionPoint<?, ManagedExecutor> event, BeanManager bm) {
        Annotated injectionPoint = event.getInjectionPoint().getAnnotated();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "processInjectionPoint " + injectionPoint);

        // Skip if no @ManagedExecutorConfig or module is not CDI enabled
        if (!injectionPoint.isAnnotationPresent(ManagedExecutorConfig.class))
            return;
        CDIService cdiSvc = getCDIService();
        if (!cdiSvc.isCurrentModuleCDIEnabled()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Current module is not CDI enabled. Skipping further processing.");
            return;
        }

        // Store configs in 3 tier map:  appName -> instanceName -> config
        String appName = cdiSvc.getCurrentApplicationContextID();
        Map<String, ManagedExecutorConfig> appBeanMap = beanMap.computeIfAbsent(appName, (e) -> new HashMap<>(8));
        ManagedExecutorConfig config = injectionPoint.getAnnotation(ManagedExecutorConfig.class);

        // Instance name is either @NamedInstance.value() or generated from fully-qualified class+field name
        NamedInstance nameAnno = injectionPoint.getAnnotation(NamedInstance.class);
        Member member = event.getInjectionPoint().getMember();
        String name = nameAnno == null ? member.getDeclaringClass().getTypeName() + "." + member.getName() : nameAnno.value();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "InjectionPoint " + name + " in app " + appName + " has config " + config);
        if (nameAnno == null)
            event.configureInjectionPoint().addQualifiers(createNamedInstance(name));
        ManagedExecutorConfig prevoiusConfig = appBeanMap.putIfAbsent(name, config);

        // If 2 or more InjectionPoints define @NamedInstance("X") @ManagedExecutorConfig it is an error
        if (prevoiusConfig != null) {
            String msg = "ERROR: Found existing bean with name=" + name; // TODO NLS message
            Tr.error(tc, msg);
            deploymentErrors.add(new Throwable(msg));
        }
    }

    private CDIService getCDIService() {
        if (cdiSvc != null)
            return cdiSvc;

        Bundle bundle = FrameworkUtil.getBundle(CDIService.class);
        return cdiSvc = AccessController.doPrivileged((PrivilegedAction<CDIService>) () -> {
            BundleContext bCtx = bundle.getBundleContext();
            ServiceReference<CDIService> svcRef = bCtx.getServiceReference(CDIService.class);
            return svcRef == null ? null : bCtx.getService(svcRef);
        });
    }

    public void registerBeans(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        CDIService cdiSvc = getCDIService();
        if (!cdiSvc.isCurrentModuleCDIEnabled())
            return;

        String appName = cdiSvc.getCurrentApplicationContextID();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Register beans for app " + appName);

        // TODO: Right now this is coded to give 1 ManagedExecutor instance for all @Default injection points
        // wait to see how discussion on this PR ends up: https://github.com/eclipse/microprofile-concurrency/pull/54
        // Always register bean for default instance(s)
        event.addBean(new ManagedExecutorBean());

        // Register 1 bean per un-named config, and 1 bean per unique NamedInstance
        Map<String, ManagedExecutorConfig> appMap = beanMap.get(appName);
        if (appMap == null)
            return;
        for (Entry<String, ManagedExecutorConfig> e : appMap.entrySet()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Add bean for name=" + e.getKey());
            event.addBean(new ManagedExecutorBean(e.getKey(), e.getValue()));
        }
    }

    public void registerErrors(@Observes AfterDeploymentValidation event) {
        deploymentErrors.forEach(event::addDeploymentProblem);
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