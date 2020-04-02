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
package com.ibm.ws.io.smallrye.graphql.metrics.component;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessBean;

import com.ibm.ws.cdi.CDIServiceUtils;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;


@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
immediate = true,
property = { "api.classes=org.eclipse.microprofile.graphql.GraphQLApi",
             "bean.defining.annotations=org.eclipse.microprofile.graphql.GraphQLApi",
             "service.vendor=IBM" })
public class GraphQLMetricsExtension implements Extension, WebSphereCDIExtension {
    private static final Logger LOG = Logger.getLogger(GraphQLMetricsExtension.class.getName());
    
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        //register the interceptor binding and in the interceptor itself
        AnnotatedType<GraphQLApi> bindingType = beanManager.createAnnotatedType(GraphQLApi.class);
        beforeBeanDiscovery.addInterceptorBinding(bindingType);

        AnnotatedType<GraphQLMetricsInterceptor> metricsInterceptorType = beanManager.createAnnotatedType(GraphQLMetricsInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(metricsInterceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(metricsInterceptorType, this.getClass()));
        
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.finest("GraphQLMetricsInterceptor bean registered");
        }
    }
}
