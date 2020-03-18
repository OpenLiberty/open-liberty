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
package org.apache.cxf.microprofile.client;

import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;

import org.apache.cxf.jaxrs.client.spec.TLSConfiguration;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;

import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_CONNECTION_TIMEOUT_PROP;
import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_RECEIVE_TIMEOUT_PROP;

public class CxfTypeSafeClientBuilder implements RestClientBuilder, Configurable<RestClientBuilder> {
    private static final Map<ClassLoader, Collection<RestClientListener>> REST_CLIENT_LISTENERS = 
        new WeakHashMap<>();

    private String baseUri;
    private ExecutorService executorService;
    private final MicroProfileClientConfigurableImpl<RestClientBuilder> configImpl =
            new MicroProfileClientConfigurableImpl<>(this);
    private TLSConfiguration secConfig = new TLSConfiguration();

    private static Collection<RestClientListener> listeners() {
        ClassLoader threadContextClassLoader;
        if (System.getSecurityManager() == null) {
            threadContextClassLoader = Thread.currentThread().getContextClassLoader();
        } else {
            threadContextClassLoader = AccessController.doPrivileged(
                (PrivilegedAction<ClassLoader>)() -> Thread.currentThread().getContextClassLoader());
        }
        synchronized (REST_CLIENT_LISTENERS) {
            return REST_CLIENT_LISTENERS.computeIfAbsent(threadContextClassLoader, key -> 
                StreamSupport.stream(ServiceLoader.load(RestClientListener.class).spliterator(), false)
                             .collect(Collectors.toList()));
        }
    }

    @Override
    public RestClientBuilder baseUrl(URL url) {
        this.baseUri = Objects.requireNonNull(url).toExternalForm();
        return this;
    }

    @Override
    public RestClientBuilder baseUri(URI uri) {
        this.baseUri = Objects.requireNonNull(uri).toString();
        return this;
    }

    @Override
    public RestClientBuilder executorService(ExecutorService executor) {
        if (null == executor) {
            throw new IllegalArgumentException("executor must not be null");
        }
        this.executorService = executor;
        return this;
    }

    @Override
    public RestClientBuilder connectTimeout(long timeout, TimeUnit unit) {
        if (null == unit) {
            throw new IllegalArgumentException("time unit must not be null");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be non-negative");
        }
        return property(HTTP_CONNECTION_TIMEOUT_PROP, unit.toMillis(timeout));
    }

    @Override
    public RestClientBuilder readTimeout(long timeout, TimeUnit unit) {
        if (null == unit) {
            throw new IllegalArgumentException("time unit must not be null");
        }
        if (timeout < 0) {
            throw new IllegalArgumentException("timeout must be non-negative");
        }
        return property(HTTP_RECEIVE_TIMEOUT_PROP, unit.toMillis(timeout));
    }

    @Override
    public <T> T build(Class<T> aClass) {
        if (baseUri == null) {
            throw new IllegalStateException("baseUrl not set");
        }
        Validator.checkValid(aClass);
        RegisterProvider[] providers = aClass.getAnnotationsByType(RegisterProvider.class);
        Configuration config = configImpl.getConfiguration();
        if (providers != null) {
            for (RegisterProvider provider : providers) {
                if (!config.isRegistered(provider.value())) {
                    if (provider.priority() == -1) {
                        register(provider.value());
                    } else {
                        register(provider.value(), provider.priority());
                    }
                }
            }
        }

        listeners().forEach(l -> l.onNewClient(aClass, this));

        MicroProfileClientFactoryBean bean = new MicroProfileClientFactoryBean(configImpl,
            baseUri, aClass, executorService, secConfig);
        return bean.create(aClass);
    }

    @Override
    public Configuration getConfiguration() {
        return configImpl.getConfiguration();
    }

    @Override
    public RestClientBuilder property(String key, Object value) {
        configImpl.property(key, value);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> componentClass) {
        configImpl.register(componentClass);
        return this;
    }

    @Override
    public RestClientBuilder register(Object component) {
        configImpl.register(component);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> componentClass, int priority) {
        configImpl.register(componentClass, priority);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> componentClass, Class<?>... contracts) {
        configImpl.register(componentClass, contracts);
        return this;
    }

    @Override
    public RestClientBuilder register(Class<?> componentClass, Map<Class<?>, Integer> contracts) {
        configImpl.register(componentClass, contracts);
        return this;
    }

    @Override
    public RestClientBuilder register(Object component, int priority) {
        configImpl.register(component, priority);
        return this;
    }

    @Override
    public RestClientBuilder register(Object component, Class<?>... contracts) {
        configImpl.register(component, contracts);
        return this;
    }

    @Override
    public RestClientBuilder register(Object component, Map<Class<?>, Integer> contracts) {
        configImpl.register(component, contracts);
        return this;
    }

    @Override
    public RestClientBuilder sslContext(SSLContext sslContext) {
        property("com.ibm.ws.jaxrs.client.security.skipSSLConfigInterceptor", "true"); // Liberty change
        secConfig.getTlsClientParams().setKeyManagers(null);
        secConfig.getTlsClientParams().setTrustManagers(null);
        secConfig.setSslContext(sslContext);
        return this;
    }

    @Override
    public RestClientBuilder keyStore(KeyStore store, String password) {
        property("com.ibm.ws.jaxrs.client.security.skipSSLConfigInterceptor", "true"); // Liberty change
        secConfig.setSslContext(null);
        try {
            KeyManagerFactory kmf =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(store, password.toCharArray());
            secConfig.getTlsClientParams().setKeyManagers(kmf.getKeyManagers());
        } catch (Exception ex) {
            throw new ProcessingException(ex);
        }
        return this;
    }

    @Override
    public RestClientBuilder trustStore(KeyStore store) {
        property("com.ibm.ws.jaxrs.client.security.skipSSLConfigInterceptor", "true"); // Liberty change
        secConfig.setSslContext(null);
        try {
            TrustManagerFactory tmf =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(store);
            secConfig.getTlsClientParams().setTrustManagers(tmf.getTrustManagers());
        } catch (Exception ex) {
            throw new ProcessingException(ex);
        }

        return this;
    }

    @Override
    public RestClientBuilder hostnameVerifier(HostnameVerifier verifier) {
        property("com.ibm.ws.jaxrs.client.security.skipSSLConfigInterceptor", "true"); // Liberty change
        secConfig.getTlsClientParams().setHostnameVerifier(verifier);
        return this;
    }
}

