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
package com.ibm.ws.microprofile.graphql.authorization.component;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
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
public class GraphQLAuthorizationExtension implements Extension, WebSphereCDIExtension {
    TraceComponent tc = Tr.register(GraphQLAuthorizationExtension.class);

    @Trivial
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        //register the authorization interceptor binding and the interceptor itself
        AnnotatedType<GraphQLApi> bindingType = beanManager.createAnnotatedType(GraphQLApi.class);
        beforeBeanDiscovery.addInterceptorBinding(bindingType);
        AnnotatedType<AuthorizationInterceptor> interceptorType = beanManager.createAnnotatedType(AuthorizationInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(interceptorType, CDIServiceUtils.getAnnotatedTypeIdentifier(interceptorType, this.getClass()));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "beforeBeanDiscovery - registered AuthorizingInterceptor");
        }
    }
}
