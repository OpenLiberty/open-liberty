/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.io.smallrye.graphql.component;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;


@Component(service = WebSphereCDIExtension.class,
		configurationPolicy = ConfigurationPolicy.IGNORE,
immediate = true,
property = { "api.classes=org.eclipse.microprofile.graphql.GraphQLApi",
             "bean.defining.annotations=org.eclipse.microprofile.graphql.GraphQLApi",
             "application.bdas.visible=true",
             "service.vendor=IBM" })
public class GraphQLExtension implements Extension, WebSphereCDIExtension {
    private static final Logger LOG = Logger.getLogger(GraphQLExtension.class.getName());
    
    static Map<ClassLoader, BeanManager> beanManagers = new WeakHashMap<>();

    static Map<ClassLoader, Set<Bean<?>>> graphQLApiBeans = new WeakHashMap<>();

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        //register the tracing interceptor binding and the interceptor itself
        AnnotatedType<GraphQLApi> bindingType = beanManager.createAnnotatedType(GraphQLApi.class);
        beforeBeanDiscovery.addInterceptorBinding(bindingType);
        AnnotatedType<TracingInterceptor> interceptorType = beanManager.createAnnotatedType(TracingInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(interceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(interceptorType, this.getClass()));
    }

    public <X> void detectGraphQLComponent(@Observes ProcessBean<X> event, BeanManager beanManager) {
        Annotated annotated = event.getAnnotated();
        if (annotated.isAnnotationPresent(GraphQLApi.class)) {
            Bean<?> bean = event.getBean();
            Class<?> beanClass = bean.getBeanClass();
            ClassLoader loader = getContextClassLoader();
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.finest("class, " + beanClass + " loaded by TCCL " + loader + " has beanManager, " + beanManager);
            }
            beanManagers.put(loader, beanManager);
            graphQLApiBeans.computeIfAbsent(loader, k -> new HashSet<>()).add(bean);
        }
    }

    public void registerGraphQLBeans(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        ClassLoader tccl = getContextClassLoader();
        Set<Bean<?>> beans = null;
        synchronized (graphQLApiBeans) {
            beans = graphQLApiBeans.get(tccl);
        }
        if (beans != null) {
            for (Bean<?> bean : beans) {
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("adding Bean, " + bean + " to afterBeanDiscovery");
                }
                afterBeanDiscovery.addBean(bean);
            }
        } else if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("Attempting to register GraphQLApi beans for unknown app classloader: " + tccl);
        }
    }

    static BeanManager getBeanManager() {
        BeanManager beanManager = beanManagers.get(getContextClassLoader());
        return beanManager;
    }

    private final static boolean SECURITY_MANAGER_ENABLED = System.getSecurityManager() != null;
    static ClassLoader getContextClassLoader() {
        if (SECURITY_MANAGER_ENABLED) {
            return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
                return Thread.currentThread().getContextClassLoader();
            });
        }
        return Thread.currentThread().getContextClassLoader();
    }
}
