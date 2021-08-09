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
package org.apache.cxf.microprofile.client.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import javax.ws.rs.Priorities;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.microprofile.client.CxfTypeSafeClientBuilder;
import org.apache.cxf.microprofile.client.config.ConfigFacade;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

public class RestClientBean implements Bean<Object>, PassivationCapable {
    private static final Logger LOG = LogUtils.getL7dLogger(RestClientBean.class); //Liberty change
    public static final String REST_URL_FORMAT = "%s/mp-rest/url";
    public static final String REST_URI_FORMAT = "%s/mp-rest/uri";
    public static final String REST_SCOPE_FORMAT = "%s/mp-rest/scope";
    public static final String REST_PROVIDERS_FORMAT = "%s/mp-rest/providers"; //Liberty change
    public static final String REST_PROVIDERS_PRIORITY_FORMAT = "%s/mp-rest/providers/%s/priority"; //Liberty change
    private static final Default DEFAULT_LITERAL = new DefaultLiteral();
    private final Class<?> clientInterface;
    private final Class<? extends Annotation> scope;
    private final BeanManager beanManager;

    public RestClientBean(Class<?> clientInterface, BeanManager beanManager) {
        this.clientInterface = clientInterface;
        this.beanManager = beanManager;
        this.scope = this.readScope();
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
        //Liberty change start
        RestClientBuilder builder = new CxfTypeSafeClientBuilder();
        String baseUri = getBaseUri();
        builder = ((CxfTypeSafeClientBuilder)builder).baseUri(URI.create(baseUri));
        List<Class<?>> providers = getConfiguredProviders();
        Map<Class<?>,Integer> providerPriorities = getConfiguredProviderPriorities(providers);
        for (Class<?> providerClass : providers){
            builder = builder.register(providerClass, 
                                       providerPriorities.getOrDefault(providerClass, Priorities.USER));
        }
        return builder.build(clientInterface);
        //Liberty change end
    }

    @Override
    public void destroy(Object instance, CreationalContext<Object> creationalContext) {

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

    @FFDCIgnore(NoSuchElementException.class)
    private String getBaseUri() {
        String interfaceName = clientInterface.getName();
        String property = String.format(REST_URI_FORMAT, interfaceName);
        String baseURI = null;
        try {
            baseURI = ConfigFacade.getValue(property, String.class);
        } catch (NoSuchElementException ex) {
            // no-op - will revert to baseURL config value (as opposed to baseURI)
        }
        if (baseURI == null) {
            // revert to baseUrl
            property = String.format(REST_URL_FORMAT, interfaceName);
            baseURI = ConfigFacade.getValue(property, String.class);
            if (baseURI == null) {
                throw new IllegalStateException("Unable to determine base URI from configuration");
            }
        }
        return baseURI;
    }

    @FFDCIgnore(Exception.class)
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

    //Liberty change start
    @FFDCIgnore(ClassNotFoundException.class)
    private List<Class<?>> getConfiguredProviders() {
        String property = String.format(REST_PROVIDERS_FORMAT, clientInterface.getName());
        String providersList = ConfigFacade.getOptionalValue(property, String.class).orElse("");
        String[] providerClassNames = providersList.split(",");
        List<Class<?>> providers = new ArrayList<>();
        for (int i=0; i<providerClassNames.length; i++) {
            try {
                providers.add(ClassLoaderUtils.loadClass(providerClassNames[i], RestClientBean.class));
            } catch (ClassNotFoundException e) {
                LOG.log(Level.WARNING,
                        "Could not load provider, {0}, configured for Rest Client interface, {1} ",
                        new Object[]{providerClassNames[i], clientInterface.getName()});
            }
        }
        return providers;
    }

    private Map<Class<?>, Integer> getConfiguredProviderPriorities(List<Class<?>> providers) {
        Map<Class<?>, Integer> map = new HashMap<>();
        for (Class<?> providerClass : providers) {
            String property = String.format(REST_PROVIDERS_PRIORITY_FORMAT, 
                                            clientInterface.getName(),
                                            providerClass.getName());
            Integer priority = ConfigFacade.getOptionalValue(property, Integer.class).orElse(Priorities.USER);
            map.put(providerClass, priority);
        }
        return map;
    }
    //Liberty change end

    private static final class DefaultLiteral extends AnnotationLiteral<Default> implements Default {
        private static final long serialVersionUID = 1L;

    }
}
