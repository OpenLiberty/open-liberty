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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import javax.net.ssl.HostnameVerifier;
import javax.ws.rs.Priorities;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.microprofile.client.CxfTypeSafeClientBuilder;
import org.apache.cxf.microprofile.client.config.ConfigFacade;
import org.eclipse.microprofile.rest.client.ext.QueryParamStyle;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

public class RestClientBean implements Bean<Object>, PassivationCapable {
    public static final String REST_URL_FORMAT = "%s/mp-rest/url";
    public static final String REST_URI_FORMAT = "%s/mp-rest/uri";
    public static final String REST_SCOPE_FORMAT = "%s/mp-rest/scope";
    public static final String REST_PROVIDERS_FORMAT = "%s/mp-rest/providers";
    public static final String REST_CONN_TIMEOUT_FORMAT = "%s/mp-rest/connectTimeout";
    public static final String REST_READ_TIMEOUT_FORMAT = "%s/mp-rest/readTimeout";
    public static final String REST_PROVIDERS_PRIORITY_FORMAT = "/mp-rest/providers/%s/priority";
    public static final String REST_TRUST_STORE_FORMAT = "%s/mp-rest/trustStore";
    public static final String REST_TRUST_STORE_PASSWORD_FORMAT = "%s/mp-rest/trustStorePassword";
    public static final String REST_TRUST_STORE_TYPE_FORMAT = "%s/mp-rest/trustStoreType";
    public static final String REST_HOSTNAME_VERIFIER_FORMAT = "%s/mp-rest/hostnameVerifier";
    public static final String REST_KEY_STORE_FORMAT = "%s/mp-rest/keyStore";
    public static final String REST_KEY_STORE_PASSWORD_FORMAT = "%s/mp-rest/keyStorePassword";
    public static final String REST_KEY_STORE_TYPE_FORMAT = "%s/mp-rest/keyStoreType";
    public static final String REST_FOLLOW_REDIRECTS_FORMAT = "%s/mp-rest/followRedirects";
    public static final String REST_PROXY_ADDRESS_FORMAT = "%s/mp-rest/proxyAddress";
    public static final String QUERY_PARAM_STYLE_FORMAT = "%s/mp-rest/queryParamStyle";
    private static final Logger LOG = LogUtils.getL7dLogger(RestClientBean.class);
    private static final Default DEFAULT_LITERAL = new DefaultLiteral();
    private final Class<?> clientInterface;
    private final Class<? extends Annotation> scope;
    private final BeanManager beanManager;
    private final Map<Object, CxfTypeSafeClientBuilder> builders = new WeakHashMap<>(); //Liberty change weak vs identity

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
        CxfTypeSafeClientBuilder builder = new CxfTypeSafeClientBuilder();
        String baseUri = getBaseUri();
        builder = (CxfTypeSafeClientBuilder) builder.baseUri(URI.create(baseUri));
        List<Class<?>> providers = getConfiguredProviders();
        Map<Class<?>, Integer> providerPriorities = getConfiguredProviderPriorities(providers);
        for (Class<?> providerClass : providers) {
            builder = (CxfTypeSafeClientBuilder) builder.register(providerClass, 
                                       providerPriorities.getOrDefault(providerClass, Priorities.USER));
        }
        setTimeouts(builder);
        setSSLConfig(builder);
        setFollowRedirects(builder);
        setProxyAddress(builder);
        setQueryParamStyle(builder);
        Object clientInstance = builder.build(clientInterface);
        builders.put(clientInstance, builder);
        return clientInstance;
    }

    @Override
    public void destroy(Object instance, CreationalContext<Object> creationalContext) {
        CxfTypeSafeClientBuilder builder = builders.remove(instance);
        if (builder != null) {
            builder.close();
        }
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

    private String getBaseUri() {
        String interfaceName = clientInterface.getName();
        String baseURI = ConfigFacade
            .getOptionalValue(REST_URI_FORMAT, clientInterface, String.class)
            .orElseGet(() -> ConfigFacade.getOptionalValue(REST_URL_FORMAT, clientInterface,
                                                           String.class).orElse(null));

        if (baseURI == null) {
            // last, if baseUrl/Uri is not specified via MP Config, check the @RegisterRestClient annotation
            RegisterRestClient anno = clientInterface.getAnnotation(RegisterRestClient.class);
            if (anno != null) {
                String annoUri = anno.baseUri();
                if (annoUri != null && !"".equals(anno.baseUri())) {
                    baseURI = annoUri;
                }
            }
        }

        if (baseURI == null) {
            throw new IllegalStateException("Unable to determine base URI from configuration for "
                                            + interfaceName);
        }
        return baseURI;
    }

    private Class<? extends Annotation> readScope() {
        // first check to see if the value is set
        String configuredScope = ConfigFacade.getOptionalValue(REST_SCOPE_FORMAT, clientInterface, String.class)
                                             .orElse(null);
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

    List<Class<?>> getConfiguredProviders() {
        String providersList = ConfigFacade.getOptionalValue(REST_PROVIDERS_FORMAT, clientInterface, String.class)
                                           .orElse(null);
        List<Class<?>> providers = new ArrayList<>();
        if (providersList != null) {
            String[] providerClassNames = providersList.split(",");
            for (int i = 0; i < providerClassNames.length; i++) {
                try {
                    providers.add(ClassLoaderUtils.loadClass(providerClassNames[i], RestClientBean.class));
                } catch (ClassNotFoundException e) {
                    LOG.log(Level.WARNING,
                            "Could not load provider, {0}, configured for Rest Client interface, {1} ",
                            new Object[]{providerClassNames[i], clientInterface.getName()});
                }
            }
        }
        return providers;
    }

    Map<Class<?>, Integer> getConfiguredProviderPriorities(List<Class<?>> providers) {
        Map<Class<?>, Integer> map = new HashMap<>();
        for (Class<?> providerClass : providers) {
            String propertyFormat = "%s" + String.format(REST_PROVIDERS_PRIORITY_FORMAT, 
                                            providerClass.getName());
            Integer priority = ConfigFacade.getOptionalValue(propertyFormat, clientInterface, Integer.class)
                                           .orElse(getPriorityFromClass(providerClass, Priorities.USER));
            map.put(providerClass, priority);
        }
        return map;
    }

    private static int getPriorityFromClass(Class<?> providerClass, int defaultValue) {
        Priority p = providerClass.getAnnotation(Priority.class);
        return p != null ? p.value() : defaultValue;
    }

    private static final class DefaultLiteral extends AnnotationLiteral<Default> implements Default {
        private static final long serialVersionUID = 1L;

    }

    private void setTimeouts(CxfTypeSafeClientBuilder builder) {
        ConfigFacade.getOptionalLong(REST_CONN_TIMEOUT_FORMAT, clientInterface).ifPresent(
            timeoutValue -> {
                builder.connectTimeout(timeoutValue, TimeUnit.MILLISECONDS);
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("readTimeout set by MP Config: " + timeoutValue);
                }
            });

        ConfigFacade.getOptionalLong(REST_READ_TIMEOUT_FORMAT, clientInterface).ifPresent(
            timeoutValue -> {
                builder.readTimeout(timeoutValue, TimeUnit.MILLISECONDS);
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("readTimeout set by MP Config: " + timeoutValue);
                }
            });
    }

    private void setFollowRedirects(CxfTypeSafeClientBuilder builder) {
        ConfigFacade.getOptionalValue(REST_FOLLOW_REDIRECTS_FORMAT, clientInterface, String.class).ifPresent(
            follows -> {
                builder.followRedirects(PropertyUtils.isTrue(follows));
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("followRedirect set by MP Config: " + follows);
                }
            });
    }

    private void setProxyAddress(CxfTypeSafeClientBuilder builder) {
        ConfigFacade.getOptionalValue(REST_PROXY_ADDRESS_FORMAT, clientInterface, String.class).ifPresent(
            address -> {
                String[] split = address.split(":");
                if (split.length != 2) {
                    throw new IllegalStateException(String.format("Invalid proxy server address configured for %s: %s",
                                                                  clientInterface.getName(),
                                                                  address));
                }
                try {
                    String hostname = split[0];
                    int port = Integer.parseInt(split[1]);
                    builder.proxyAddress(hostname, port);
                } catch (Throwable t) {
                    throw new IllegalStateException(String.format("Invalid proxy server address configured for %s: %s",
                                                                  clientInterface.getName(),
                                                                  address), t);
                }

                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("proxyAddress set by MP Config: " + address);
                }
            });
    }

    private void setQueryParamStyle(CxfTypeSafeClientBuilder builder) {
        ConfigFacade.getOptionalValue(QUERY_PARAM_STYLE_FORMAT, clientInterface, String.class).ifPresent(
            styleString -> {
                try {
                    builder.queryParamStyle(QueryParamStyle.valueOf(styleString));
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.finest("queryParamStyle set by MP Config: " + styleString);
                    }
                } catch (Throwable t) {
                    throw new IllegalStateException(String.format("Invalid queryParamStyle value specified for %s: %s",
                                                                  clientInterface.getName(),
                                                                  styleString));
                }
            });
    
    }

    private void setSSLConfig(CxfTypeSafeClientBuilder builder) {
        ConfigFacade.getOptionalValue(REST_HOSTNAME_VERIFIER_FORMAT, clientInterface, String.class).ifPresent(
            className -> {
                try {
                    @SuppressWarnings("unchecked")
                    Class<HostnameVerifier> clazz = (Class<HostnameVerifier>) 
                        ClassLoaderUtils.loadClassFromContextLoader(className);
                    Constructor<HostnameVerifier> ctor = ReflectionUtil.getConstructor(clazz);
                    if (ctor != null) {
                        builder.hostnameVerifier(ctor.newInstance());
                        return;
                    }
                } catch (Throwable t) {
                    // ignore - will log below
                }
                LOG.log(Level.WARNING, "INVALID_HOSTNAME_VERIFIER_CONFIGURED",
                        new Object[] {className, clientInterface.getName()});
        });

        
        ConfigFacade.getOptionalValue(REST_TRUST_STORE_FORMAT, clientInterface, String.class).ifPresent(
            trustStoreLoc -> {
                initTrustStore(trustStoreLoc, builder);
            }
        );

        ConfigFacade.getOptionalValue(REST_KEY_STORE_FORMAT, clientInterface, String.class).ifPresent(
            keyStoreLoc -> {
                initKeyStore(keyStoreLoc, builder);
            }
        );
        
    }

    private void initTrustStore(String trustStoreLoc, CxfTypeSafeClientBuilder builder) {
        String password = ConfigFacade.getOptionalValue(REST_TRUST_STORE_PASSWORD_FORMAT, clientInterface, String.class)
                                      .orElse(null);
        String storeType = ConfigFacade.getOptionalValue(REST_TRUST_STORE_TYPE_FORMAT, clientInterface, String.class)
                                       .orElse("JKS");

        try {
            KeyStore trustStore = KeyStore.getInstance(storeType);
            try (InputStream input = getInputStream(trustStoreLoc)) {
                trustStore.load(input, password == null ? null : password.toCharArray());
            } catch (Throwable t) {
                throw new IllegalArgumentException("Failed to initialize trust store from URL, " + trustStoreLoc, t);
            }

            builder.trustStore(trustStore);
        } catch (KeyStoreException e) {
            throw new IllegalArgumentException("Failed to initialize trust store from " + trustStoreLoc, e);
        }
    }

    private void initKeyStore(String keyStoreLoc, CxfTypeSafeClientBuilder builder) {
        String password = ConfigFacade.getOptionalValue(REST_KEY_STORE_PASSWORD_FORMAT, clientInterface, String.class)
                                      .orElse(null);
        String storeType = ConfigFacade.getOptionalValue(REST_KEY_STORE_TYPE_FORMAT, clientInterface, String.class)
                                       .orElse("JKS");

        try {
            KeyStore keyStore = KeyStore.getInstance(storeType);
            try (InputStream input = getInputStream(keyStoreLoc)) {
                keyStore.load(input, password == null ? null : password.toCharArray());
            } catch (Throwable t) {
                throw new IllegalArgumentException("Failed to initialize key store from URL, " + keyStoreLoc, t);
            }

            builder.keyStore(keyStore, password);
        } catch (KeyStoreException e) {
            throw new IllegalArgumentException("Failed to initialize key store from " + keyStoreLoc, e);
        }
    }

    InputStream getInputStream(String location) {
        if (location.startsWith("classpath:")) {
            location = location.substring(10); // 10 == "classpath:".length()
            //TODO: replace getResources lookup with getResourceAsStream once 
            // https://github.com/arquillian/arquillian-container-weld/issues/59 is resolved
            // in = ClassLoaderUtils.getResourceAsStream(location, clientInterface);
            List<URL> urls = ClassLoaderUtils.getResources(location, clientInterface);
            if (urls != null && !urls.isEmpty()) {
                try {
                    return urls.get(0).openStream();
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            LOG.warning("could not find classpath:" + location); //TODO: new message
            throw new IllegalStateException("could not find configured key/trust store: " + location);
        }
        try {
            return new URL(location).openStream();
        } catch (Exception e) {
            // try using a file URI
            try {
                return new URL("file:" + location).openStream();
            } catch (Exception e2) {
                //ignore, rethrow original exception
            }
            throw new IllegalStateException("could not find configured key/trust store URL: " + location); 
        }
    }
}
