/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ibm.ws.microprofile.rest.client.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.microprofile.client.CxfTypeSafeClientBuilder;
import org.apache.cxf.microprofile.client.config.ConfigFacade;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class LibertyRestClientBean implements Bean<Object>, PassivationCapable {
    private final static TraceComponent tc = Tr.register(LibertyRestClientExtension.class);
    
    public static final String REST_URL_FORMAT = "%s/mp-rest/url";
    public static final String REST_SCOPE_FORMAT = "%s/mp-rest/scope";
    private static final Default DEFAULT_LITERAL = new DefaultLiteral();
    private final Class<?> clientInterface;
    private final Class<? extends Annotation> scope;
    private final BeanManager beanManager;

    // Liberty change - making ctor public so can be called from LibertyRestClientExtension
    public LibertyRestClientBean(Class<?> clientInterface, BeanManager beanManager) {
        this.clientInterface = clientInterface;
        this.beanManager = beanManager;
        this.scope = this.readScope();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "<init> - clientInterface=" + clientInterface + " beanManager=" + beanManager + " scope=" + scope);
        }
    }

    @Override
    public String getId() {
        return clientInterface.getName();
    }

    @Override
    public Class<?> getBeanClass() {
        return clientInterface;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public Object create(CreationalContext<Object> creationalContext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "create - creationalContext=" + creationalContext);
        }
        CxfTypeSafeClientBuilder builder = new CxfTypeSafeClientBuilder();
        String baseUrl = getBaseUrl();
        Object client = null;
        try {
            client = builder.baseUrl(new URL(baseUrl)).build(clientInterface);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The value of URL was invalid " + baseUrl);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "create - creationalContext=" + creationalContext + " client=" + client + " baseUrl=" + baseUrl);
        }
        return client;
    }

    @Override
    public void destroy(Object instance, CreationalContext<Object> creationalContext) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "destroy - instance=" + instance + " creationalContext=" + creationalContext);
        }
        creationalContext.release();
    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(clientInterface);
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return new HashSet<>(Arrays.asList(DEFAULT_LITERAL, RestClient.RestClientLiteral.LITERAL));
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    @Override
    public String getName() {
        return clientInterface.getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    private String getBaseUrl() {
        String property = String.format(REST_URL_FORMAT, clientInterface.getName());
        String baseURL = ConfigFacade.getValue(property, String.class);
        if (baseURL == null) {
            throw new IllegalStateException("Unable to determine base URL from configuration");
        }
        return baseURL;
    }

    private Class<? extends Annotation> readScope() {
        // first check to see if the value is set
        String property = String.format(REST_SCOPE_FORMAT, clientInterface.getName());
        String configuredScope = ConfigFacade.getOptionalValue(property, String.class).orElse(null);
        if (configuredScope != null) {
            try {
                return ClassLoaderUtils.loadClass(configuredScope, getClass(), Annotation.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("The scope " + configuredScope + " is invalid", e);
            }
        }

        List<Annotation> possibleScopes = new ArrayList<>();
        Annotation[] annotations = clientInterface.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            if (beanManager.isScope(annotation.annotationType())) {
                possibleScopes.add(annotation);
            }
        }
        if (possibleScopes.isEmpty()) {
            return Dependent.class;
        } else if (possibleScopes.size() == 1) {
            return possibleScopes.get(0).annotationType();
        } else {
            throw new IllegalArgumentException("The client interface " + clientInterface
                                               + " has multiple scopes defined " + possibleScopes);
        }
    }

    private static final class DefaultLiteral extends AnnotationLiteral<Default> implements Default {
        private static final long serialVersionUID = 1L;

    }
}
