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
package com.ibm.ws.microprofile.rest.client.cdi;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.enterprise.inject.spi.WithAnnotations;

import org.apache.cxf.microprofile.client.cdi.RestClientBean;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = 
    { "api.classes=org.eclipse.microprofile.rest.client.inject.RegisterRestClient;" +
                  "org.eclipse.microprofile.rest.client.inject.RestClient",
      "bean.defining.annotations=org.eclipse.microprofile.rest.client.inject.RegisterRestClient",
      "service.vendor=IBM"})
public class LibertyRestClientExtension implements WebSphereCDIExtension, Extension {
    private final static TraceComponent tc = Tr.register(LibertyRestClientExtension.class);

    private final Map<ClassLoader, Set<Class<?>>> restClientClasses = new WeakHashMap<>();
    private final Set<Throwable> errors = new LinkedHashSet<>();
    private final Set<String> requestScopedInterfaces = new LinkedHashSet<>();
    private final Set<InjectionPoint> requestScopedClientInjectionPoints = new LinkedHashSet<>();
    private final Set<RestClientBean> allClientBeans = new LinkedHashSet<>();

    private static ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
            return Thread.currentThread().getContextClassLoader();
        });
    }

    public void findClients(@Observes @WithAnnotations({ RegisterRestClient.class }) ProcessAnnotatedType<?> pat) {
        Class<?> restClient = pat.getAnnotatedType().getJavaClass();
        if (restClient.isInterface()) {
            ClassLoader tccl = getContextClassLoader();
            synchronized (restClientClasses) {
                Set<Class<?>> classes = restClientClasses.get(tccl);
                if (classes == null) {
                    classes = new LinkedHashSet<>();
                    restClientClasses.put(tccl, classes);
                }
                classes.add(restClient);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found RestClient: " + restClient.getName() + " from classloader: " + tccl);
            }
            pat.veto();
        } else {
            errors.add(new IllegalArgumentException("The class " + restClient
                                                    + " is not an interface"));
        }
    }

    public void registerClientBeans(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        ClassLoader tccl = getContextClassLoader();
        Set<Class<?>> classes = null;
        synchronized (restClientClasses) {
            classes = restClientClasses.get(tccl);
        }
        if (classes != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Registering RestClients from classloader: " + tccl, classes);
            }
            classes.stream().map(c -> new RestClientBean(c, beanManager)).forEach(bean -> {
                afterBeanDiscovery.addBean(bean);
                allClientBeans.add(bean);
            });
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Attempting to register RestClient for unknown app classloader: " + tccl);
        }
    }

    public void registerErrors(@Observes AfterDeploymentValidation afterDeploymentValidation, BeanManager beanManager) {
        errors.forEach(afterDeploymentValidation::addDeploymentProblem);
        try {
            for (InjectionPoint point : requestScopedClientInjectionPoints) {
                for (Bean<?> clientIntfBean : allClientBeans) {
                    if (clientIntfBean.getBeanClass().equals(point.getType()) &&
                        Dependent.class.isAssignableFrom(clientIntfBean.getScope())) {
                        Class<?> clientClass = clientIntfBean.getBeanClass();
                        requestScopedInterfaces.add(clientClass.getName() + "(" + point.getBean().getBeanClass().getName() + ")");
                    }
                }
            }
            if (!requestScopedInterfaces.isEmpty() || !requestScopedClientInjectionPoints.isEmpty()) {
                Tr.info(tc, "rest.client.interface.using.request.scope", 
                        requestScopedInterfaces.stream().collect(Collectors.joining(" ")));
            }
        } finally {
            allClientBeans.clear();
            requestScopedClientInjectionPoints.clear();
            requestScopedInterfaces.clear();
        }
    }

    @SuppressWarnings("unchecked")
    public void logRequestScopedClientInterfaces(@Observes ProcessBean<?> processBean) {
        Bean<?> bean = processBean.getBean();
        ClassLoader tccl = getContextClassLoader();
        Set<Class<?>> classes;
        synchronized(restClientClasses) {
            classes = restClientClasses.getOrDefault(tccl, Collections.EMPTY_SET);
        }
        Class<?> beanClass = bean.getBeanClass();
        if (classes.contains(beanClass)) {
            Class<?> beanScope = bean.getScope();
            if (RequestScoped.class.isAssignableFrom(beanScope)) {
                requestScopedInterfaces.add(beanClass.getName());
            }
        }
    }
    
    public void logDependentScopedInterfacesInjectedIntoRequestScopedBeans(@Observes ProcessInjectionPoint<?, ?> pip) {
        InjectionPoint point = pip.getInjectionPoint();
        Bean<?> bean = point.getBean();
        if (bean != null && RequestScoped.class.isAssignableFrom(bean.getScope())) {
            requestScopedClientInjectionPoints.add(point);
        }
    }
}
