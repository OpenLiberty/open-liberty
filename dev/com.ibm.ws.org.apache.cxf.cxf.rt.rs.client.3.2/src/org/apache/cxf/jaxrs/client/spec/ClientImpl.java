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
package org.apache.cxf.jaxrs.client.spec;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.AbstractClient;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.ClientProviderFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.model.FilterProviderInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.https.SSLUtils;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxrs21.clientconfig.JAXRSClientConfigHolder;
import com.ibm.ws.jaxrs21.clientconfig.JAXRSClientConstants;

import static org.apache.cxf.jaxrs.client.ClientProperties.DEFAULT_THREAD_SAFETY_CLIENT_STATUS;
import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_AUTOREDIRECT_PROP;
import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_CONNECTION_TIMEOUT_PROP;
import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_MAINTAIN_SESSION_PROP;
import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_PROXY_SERVER_PORT_PROP;
import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_PROXY_SERVER_PROP;
import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_RECEIVE_TIMEOUT_PROP;
import static org.apache.cxf.jaxrs.client.ClientProperties.HTTP_RESPONSE_AUTOCLOSE_PROP;
import static org.apache.cxf.jaxrs.client.ClientProperties.THREAD_SAFE_CLIENT_PROP;
import static org.apache.cxf.jaxrs.client.ClientProperties.THREAD_SAFE_CLIENT_STATE_CLEANUP_PERIOD;
import static org.apache.cxf.jaxrs.client.ClientProperties.THREAD_SAFE_CLIENT_STATE_CLEANUP_PROP;

/*
 * This class overrides the same "pure" Apache class found in the libs of com.ibm.ws.org.apache.cxf.jaxrs
 */
public class ClientImpl implements Client {


    private Configurable<Client> configImpl;
    private TLSConfiguration secConfig;
    private boolean closed;
    private Set<WebClient> baseClients =
        Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<WebClient, Boolean>()));

    public ClientImpl(Configuration config,
                      TLSConfiguration secConfig) {
        configImpl = new ClientConfigurableImpl<>(this, config);
        this.secConfig = secConfig;
    }

    @Override
    public void close() {
        if (!closed) {
            synchronized (baseClients) {
                for (WebClient wc : baseClients) {
                    wc.close();
                }
            }
            baseClients = null;
            closed = true;
        }

    }

    @Override
    public Builder invocation(Link link) {
        checkNull(link);
        checkClosed();
        Builder builder = target(link.getUriBuilder()).request();
        String type = link.getType();
        if (type != null) {
            builder.accept(type);
        }
        return builder;
    }

    @Override
    public WebTarget target(UriBuilder builder) {
        checkNull(builder);
        checkClosed();

        return new WebTargetImpl(builder, getConfiguration());
    }


    @Override
    public WebTarget target(String address) {
        checkNull(address);
        if (address.isEmpty()) {
            address = "/";
        }

        // Liberty change start
        WebTarget target;
        int braceIndex = address.indexOf('{');
        if (braceIndex < 0) {
            UriBuilder builder = UriBuilder.fromUri(address);
            target = target(builder);
        } else {
            String strippedAddress = address.substring(0, braceIndex);
            String template = address.substring(braceIndex);
            target = target(UriBuilder.fromUri(strippedAddress));
            target = target.path(template);
        }
        return target;
        // Liberty change end
    }

    @Override
    public WebTarget target(Link link) {
        checkNull(link);
        return target(link.getUriBuilder());
    }

    @Override
    public WebTarget target(URI uri) {
        checkNull(uri);
        return target(UriBuilder.fromUri(uri));
    }

    private void checkNull(Object... target) {
        for (Object o : target) {
            if (o == null) {
                throw new NullPointerException("Value is null");
            }
        }
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        checkClosed();
        return secConfig.getTlsClientParams().getHostnameVerifier();
    }

    @Override
    public SSLContext getSslContext() {
        checkClosed();
        if (secConfig.getSslContext() != null) {
            return secConfig.getSslContext();
        } else if (secConfig.getTlsClientParams().getTrustManagers() != null) {
            try {
                return SSLUtils.getSSLContext(secConfig.getTlsClientParams());
            } catch (Exception ex) {
                throw new ProcessingException(ex);
            }
        } else {
            return null;
        }
    }

    private void checkClosed() {
        if (closed) {
            throw new IllegalStateException("client is closed");
        }
    }

    @Override
    public Configuration getConfiguration() {
        checkClosed();
        return configImpl.getConfiguration();
    }

    @Override
    public Client property(String name, @Sensitive Object value) {
        checkClosed();
        // Liberty change start need to convert proxy password to ProtectedString
        if (JAXRSClientConstants.PROXY_PASSWORD.equals(name) && value != null &&
            !(value instanceof ProtectedString)) {
            return configImpl.property(name, new ProtectedString(value.toString().toCharArray()));
        } // Liberty change end
        return configImpl.property(name, value);
    }

    @Override
    public Client register(Class<?> cls) {
        checkClosed();
        return configImpl.register(cls);
    }

    @Override
    public Client register(Object object) {
        checkClosed();
        return configImpl.register(object);
    }

    @Override
    public Client register(Class<?> cls, int index) {
        checkClosed();
        return configImpl.register(cls, index);
    }

    @Override
    public Client register(Class<?> cls, Class<?>... contracts) {
        checkClosed();
        return configImpl.register(cls, contracts);
    }

    @Override
    public Client register(Class<?> cls, Map<Class<?>, Integer> map) {
        checkClosed();
        return configImpl.register(cls, map);
    }

    @Override
    public Client register(Object object, int index) {
        checkClosed();
        return configImpl.register(object, index);
    }

    @Override
    public Client register(Object object, Class<?>... contracts) {
        checkClosed();
        return configImpl.register(object, contracts);
    }

    @Override
    public Client register(Object object, Map<Class<?>, Integer> map) {
        checkClosed();
        return configImpl.register(object, map);
    }

    public class WebTargetImpl implements WebTarget {
        private Configurable<WebTarget> configImpl;
        private UriBuilder uriBuilder;
        private WebClient targetClient;


        public WebTargetImpl(UriBuilder uriBuilder,
                             Configuration config) {
            this(uriBuilder, config, null);
        }

        @FFDCIgnore(IllegalArgumentException.class)
        public WebTargetImpl(UriBuilder uriBuilder,
                             Configuration config,
                             WebClient targetClient) {
            this.configImpl = new ClientConfigurableImpl<>(this, config);
            this.uriBuilder = uriBuilder.clone();
            this.targetClient = targetClient;

            //rtc211775, apply config properties that were supplied in server.xml.
            try {
                Map<String, String> propertiesFromServerXml = JAXRSClientConfigHolder.getURIProps(uriBuilder);
                applyProperties(propertiesFromServerXml, configImpl);

            } catch (IllegalArgumentException iaex) {
                iaex = null;
                // incomplete url encountered from uriBuilder.build, we can't act on it
            }

        }

        /**
         * if applicable config properties were present in server.xml, apply them now.
         *
         * @param propertiesFromServerXml
         * @param config
         */
        private void applyProperties(Map<String, String> props, Configurable<WebTarget> config) {
            if (props == null || props.size() == 0)
                return;

            Iterator<String> it = props.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                config.property(key, props.get(key));
            }
        }

        public WebClient getWebClient() {
            return this.targetClient;
        }

        @Override
        public Builder request() {
            checkClosed();
            Map<String, Object> configProps = getConfiguration().getProperties();

            initTargetClientIfNeeded(configProps);

            ClientProviderFactory pf =
                ClientProviderFactory.getInstance(WebClient.getConfig(targetClient).getEndpoint());
            List<Object> providers = new LinkedList<>();
            List<org.apache.cxf.feature.Feature> cxfFeatures =
                new LinkedList<>();
            Configuration cfg = configImpl.getConfiguration();
            for (Object p : cfg.getInstances()) {
                if (p instanceof org.apache.cxf.feature.Feature) {
                    cxfFeatures.add((org.apache.cxf.feature.Feature)p);
                } else if (!(p instanceof Feature)) {
                    Map<Class<?>, Integer> contracts = cfg.getContracts(p.getClass());
                    if (contracts == null || contracts.isEmpty()) {
                        providers.add(p);
                    } else {
                        final Class<?> providerCls = ClassHelper.getRealClass(pf.getBus(), p);
                        providers.add(new FilterProviderInfo<Object>(p.getClass(),
                            providerCls, p, pf.getBus(), contracts));
                    }
                }
            }

            pf.setUserProviders(providers);
            ClientConfiguration clientCfg = WebClient.getConfig(targetClient);

            clientCfg.getRequestContext().putAll(configProps);
            clientCfg.getRequestContext().put(Client.class.getName(), ClientImpl.this);
            clientCfg.getRequestContext().put(Configuration.class.getName(),
                                                                      getConfiguration());

            // Response auto-close
            Boolean responseAutoClose = getBooleanValue(configProps.get(HTTP_RESPONSE_AUTOCLOSE_PROP));
            if (responseAutoClose != null) {
                clientCfg.getResponseContext().put("response.stream.auto.close", responseAutoClose);
            }
            // TLS
            TLSClientParameters tlsParams = secConfig.getTlsClientParams();
            if (tlsParams.getSSLSocketFactory() != null
                || tlsParams.getTrustManagers() != null
                || tlsParams.getHostnameVerifier() != null) {
                clientCfg.getHttpConduit().setTlsClientParameters(tlsParams);
            }
            // Executor for the asynchronous calls
            Object executorServiceProp = configProps.get(AbstractClient.EXECUTOR_SERVICE_PROPERTY);
            if (executorServiceProp != null) {
                clientCfg.getResponseContext().put(AbstractClient.EXECUTOR_SERVICE_PROPERTY, executorServiceProp);
            }
            setConnectionProperties(configProps, clientCfg);
            // CXF Features
            for (org.apache.cxf.feature.Feature cxfFeature : cxfFeatures) {
                cxfFeature.initialize(clientCfg, clientCfg.getBus());
            }
            // Start building the invocation
            return new InvocationBuilderImpl(WebClient.fromClient(targetClient),
                                             getConfiguration());
        }

        private void setConnectionProperties(Map<String, Object> configProps, ClientConfiguration clientCfg) {
            Long connTimeOutValue = getLongValue(configProps.get(HTTP_CONNECTION_TIMEOUT_PROP));
            if (connTimeOutValue != null) {
                clientCfg.getHttpConduit().getClient().setConnectionTimeout(connTimeOutValue);
            }
            Long recTimeOutValue = getLongValue(configProps.get(HTTP_RECEIVE_TIMEOUT_PROP));
            if (recTimeOutValue != null) {
                clientCfg.getHttpConduit().getClient().setReceiveTimeout(recTimeOutValue);
            }
            Object proxyServerValue = configProps.get(HTTP_PROXY_SERVER_PROP);
            if (proxyServerValue != null) {
                clientCfg.getHttpConduit().getClient().setProxyServer((String)proxyServerValue);
            }
            Integer proxyServerPortValue = getIntValue(configProps.get(HTTP_PROXY_SERVER_PORT_PROP));
            if (proxyServerPortValue != null) {
                clientCfg.getHttpConduit().getClient().setProxyServerPort(proxyServerPortValue);
            }
            Boolean autoRedirectValue = getBooleanValue(configProps.get(HTTP_AUTOREDIRECT_PROP));
            if (autoRedirectValue != null) {
                clientCfg.getHttpConduit().getClient().setAutoRedirect(autoRedirectValue);
            }
            Boolean mantainSessionValue = getBooleanValue(configProps.get(HTTP_MAINTAIN_SESSION_PROP));
            if (mantainSessionValue != null) {
                clientCfg.getRequestContext().put(Message.MAINTAIN_SESSION, mantainSessionValue);
            }
        }

        private void initTargetClientIfNeeded(Map<String, Object> configProps) {
            URI uri = uriBuilder.build();
            if (targetClient == null) {
                JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
                bean.setAddress(uri.toString());
                Boolean threadSafe = getBooleanValue(configProps.get(THREAD_SAFE_CLIENT_PROP));
                if (threadSafe == null) {
                    threadSafe = DEFAULT_THREAD_SAFETY_CLIENT_STATUS;
                }
                bean.setThreadSafe(threadSafe);
                if (threadSafe) {
                    Integer cleanupPeriod = getIntValue(configProps.get(THREAD_SAFE_CLIENT_STATE_CLEANUP_PROP));
                    if (cleanupPeriod == null) {
                        cleanupPeriod = THREAD_SAFE_CLIENT_STATE_CLEANUP_PERIOD;
                    }
                    if (cleanupPeriod != null) {
                        bean.setSecondsToKeepState(cleanupPeriod);
                    }
                }
                targetClient = bean.createWebClient();
                ClientImpl.this.baseClients.add(targetClient);
            } else if (!targetClient.getCurrentURI().equals(uri)) {
                targetClient.to(uri.toString(), false);
            }
        }

        @Override
        public Builder request(String... accept) {
            return request().accept(accept);
        }

        @Override
        public Builder request(MediaType... accept) {
            return request().accept(accept);
        }

        @Override
        public URI getUri() {
            checkClosed();
            return uriBuilder.build();
        }

        @Override
        public UriBuilder getUriBuilder() {
            checkClosed();
            return uriBuilder.clone();
        }

        @Override
        public WebTarget path(String path) {
            checkClosed();
            checkNull(path);
            return newWebTarget(getUriBuilder().path(path));
        }

        @Override
        public WebTarget queryParam(String name, Object... values) {
            checkClosed();
            checkNullValues(name, values);
            UriBuilder thebuilder = getUriBuilder();
            if (values == null || values.length == 1 && values[0] == null) {
                thebuilder.replaceQueryParam(name, (Object[])null);
            } else {
                thebuilder.queryParam(name, values);
            }
            return newWebTarget(thebuilder);
        }

        @Override
        public WebTarget matrixParam(String name, Object... values) {
            checkClosed();
            checkNullValues(name, values);

            UriBuilder thebuilder = getUriBuilder();
            if (values == null || values.length == 1 && values[0] == null) {
                thebuilder.replaceMatrixParam(name, (Object[])null);
            } else {
                thebuilder.matrixParam(name, values);
            }
            return newWebTarget(thebuilder);
        }

        @Override
        public WebTarget resolveTemplate(String name, Object value) {
            return resolveTemplate(name, value, true);
        }

        @Override
        public WebTarget resolveTemplate(String name, Object value, boolean encodeSlash) {
            checkClosed();
            checkNull(name, value);
            return newWebTarget(getUriBuilder().resolveTemplate(name, value, encodeSlash));
        }

        @Override
        public WebTarget resolveTemplateFromEncoded(String name, Object value) {
            checkNull(name, value);
            return newWebTarget(getUriBuilder().resolveTemplateFromEncoded(name, value));
        }

        @Override
        public WebTarget resolveTemplates(Map<String, Object> templatesMap) {
            return resolveTemplates(templatesMap, true);
        }

        @Override
        public WebTarget resolveTemplates(Map<String, Object> templatesMap, boolean encodeSlash) {
            checkClosed();
            checkNullMap(templatesMap);

            if (templatesMap.isEmpty()) {
                return this;
            }
            return newWebTarget(getUriBuilder().resolveTemplates(templatesMap, encodeSlash));
        }

        @Override
        public WebTarget resolveTemplatesFromEncoded(Map<String, Object> templatesMap) {
            checkClosed();
            checkNullMap(templatesMap);
            if (templatesMap.isEmpty()) {
                return this;
            }
            return newWebTarget(getUriBuilder().resolveTemplatesFromEncoded(templatesMap));
        }

        private WebTarget newWebTarget(UriBuilder newBuilder) {
            WebClient newClient;
            if (targetClient != null) {
                newClient = WebClient.fromClient(targetClient);
            } else {
                newClient = null;
            }
            return new WebTargetImpl(newBuilder, getConfiguration(), newClient);
        }

        @Override
        public Configuration getConfiguration() {
            checkClosed();
            return configImpl.getConfiguration();
        }

        @Override
        public WebTarget property(String name, @Sensitive Object value) {
            checkClosed();
            // need to convert proxy password to ProtectedString - Liberty change start
            if (JAXRSClientConstants.PROXY_PASSWORD.equals(name) && value != null &&
                !(value instanceof ProtectedString)) {
                return configImpl.property(name, new ProtectedString(value.toString().toCharArray()));
            } // Liberty change end
            return configImpl.property(name, value);
        }

        @Override
        public WebTarget register(Class<?> cls) {
            checkClosed();
            return configImpl.register(cls);
        }

        @Override
        public WebTarget register(Object object) {
            checkClosed();
            return configImpl.register(object);
        }

        @Override
        public WebTarget register(Class<?> cls, int index) {
            checkClosed();
            return configImpl.register(cls, index);
        }

        @Override
        public WebTarget register(Class<?> cls, Class<?>... contracts) {
            checkClosed();
            return configImpl.register(cls, contracts);
        }

        @Override
        public WebTarget register(Class<?> cls, Map<Class<?>, Integer> map) {
            checkClosed();
            return configImpl.register(cls, map);
        }

        @Override
        public WebTarget register(Object object, int index) {
            checkClosed();
            return configImpl.register(object, index);
        }

        @Override
        public WebTarget register(Object object, Class<?>... contracts) {
            checkClosed();
            return configImpl.register(object, contracts);
        }

        @Override
        public WebTarget register(Object object, Map<Class<?>, Integer> map) {
            checkClosed();
            return configImpl.register(object, map);
        }

        private void checkNullValues(Object name, Object... values) {
            checkNull(name);
            if (values != null && values.length > 1) {
                checkNull(values);
            }
        }

        private void checkNullMap(Map<String, Object> templatesMap) {
            checkNull(templatesMap);
            checkNull(templatesMap.keySet().toArray());
            checkNull(templatesMap.values().toArray());
        }
    }
    private static Long getLongValue(Object o) {
        return o instanceof Long ? (Long)o
            : o instanceof String ? Long.valueOf(o.toString())
            : o instanceof Integer ? ((Integer)o).longValue() : null;
    }
    private static Integer getIntValue(Object o) {
        return o instanceof Integer ? (Integer)o : o instanceof String ? Integer.valueOf(o.toString()) : null;
    }
    private static Boolean getBooleanValue(Object o) {
        return o instanceof Boolean ? (Boolean)o : o instanceof String ? Boolean.valueOf(o.toString()) : null;
    }
}
