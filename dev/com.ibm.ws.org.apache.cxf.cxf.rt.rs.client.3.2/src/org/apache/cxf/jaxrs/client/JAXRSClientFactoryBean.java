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
package org.apache.cxf.jaxrs.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.ProxyHelper;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.ClientLifeCycleManager;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.UpfrontConduitSelector;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.AbstractJAXRSFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.JAXRSServiceImpl;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.FactoryBeanListener;

public class JAXRSClientFactoryBean extends AbstractJAXRSFactoryBean {

    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSClientFactoryBean.class);

    private String username;
    private String password;
    private boolean inheritHeaders;
    private MultivaluedMap<String, String> headers;
    private ClientState initialState;
    private boolean threadSafe;
    private long timeToKeepState;
    private Class<?> serviceClass;
    private ClassLoader proxyLoader;

    public JAXRSClientFactoryBean() {
        this(new JAXRSServiceFactoryBean());
    }

    public JAXRSClientFactoryBean(JAXRSServiceFactoryBean serviceFactory) {
        super(serviceFactory);
        serviceFactory.setEnableStaticResolution(true);

    }

    /**
     * Sets the custom class loader to be used for creating proxies.
     * By default the class loader of the given serviceClass will be used.
     *
     * @param loader
     */
    public void setClassLoader(ClassLoader loader) {
        proxyLoader = loader;
    }

    /**
     * Indicates if a single proxy or WebClient instance can be reused
     * by multiple threads.
     *
     * @param threadSafe if true then multiple threads can invoke on
     *        the same proxy or WebClient instance.
     */
    public void setThreadSafe(boolean threadSafe) {
        this.threadSafe = threadSafe;
    }

    /**
     * Sets the time a thread-local client state will be kept.
     * This property is ignored for thread-unsafe clients
     * @param time secondsToKeepState
     */
    public void setSecondsToKeepState(long time) {
        this.timeToKeepState = time;
    }

    /**
     * Gets the user name
     * @return the name
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     * Setting the username and password is a simple way to
     * create a Basic Authentication token.
     *
     * @param username the user name
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets the password
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     * Setting the username and password is a simple way to
     * create a Basic Authentication token.
     *
     * @param password the password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Indicates if the headers set by a current proxy will be inherited
     * when a subresource proxy is created
     * vice versa.
     *
     * @param ih if set to true then the current headers will be inherited
     */
    public void setInheritHeaders(boolean ih) {
        inheritHeaders = ih;
    }

    /**
     * Sets the resource class
     * @param cls the resource class
     */
    public void setResourceClass(Class<?> cls) {
        setServiceClass(cls);
    }

    /**
     * Sets the service class, may be called from a Spring handler
     * @param cls the service class
     */
    public void setServiceClass(Class<?> cls) {
        this.serviceClass = cls;
        serviceFactory.setResourceClass(cls);
    }

    /**
     * Returns the service class
     */
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    /**
     * Sets the headers new proxy or WebClient instances will be
     * initialized with.
     *
     * @param map the headers
     */
    public void setHeaders(Map<String, String> map) {
        headers = new MetadataMap<String, String>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String[] values = entry.getValue().split(",");
            for (String v : values) {
                if (v.length() != 0) {
                    headers.add(entry.getKey(), v);
                }
            }
        }
    }

    /**
     * Gets the initial headers
     * @return the headers
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Creates a WebClient instance
     * @return WebClient instance
     */
    public WebClient createWebClient() {
        String serviceAddress = getAddress();
        int queryIndex = serviceAddress != null ? serviceAddress.lastIndexOf('?') : -1;
        if (queryIndex != -1) {
            serviceAddress = serviceAddress.substring(0, queryIndex);
        }
        Service service = new JAXRSServiceImpl(serviceAddress, getServiceName());
        getServiceFactory().setService(service);

        try {
            Endpoint ep = createEndpoint();
            this.getServiceFactory().sendEvent(FactoryBeanListener.Event.PRE_CLIENT_CREATE, ep);
            ClientState actualState = getActualState();
            WebClient client = actualState == null ? new WebClient(getAddress())
                : new WebClient(actualState);
            initClient(client, ep, actualState == null);

            notifyLifecycleManager(client);
            this.getServiceFactory().sendEvent(FactoryBeanListener.Event.CLIENT_CREATED, client, ep);

            return client;
        } catch (Exception ex) {
            LOG.severe(ex.getClass().getName() + " : " + ex.getLocalizedMessage());
            throw new RuntimeException(ex);
        }
    }

    private void notifyLifecycleManager(Object client) {
        ClientLifeCycleManager mgr = bus.getExtension(ClientLifeCycleManager.class);
        if (null != mgr) {
            mgr.clientCreated(new FrontendClientAdapter(WebClient.getConfig(client)));
        }
    }

    private ClientState getActualState() {
        if (threadSafe) {
            initialState = new ThreadLocalClientState(getAddress(), timeToKeepState);
        }
        if (initialState != null) {
            return headers != null
                ? initialState.newState(URI.create(getAddress()), headers, null) : initialState;
        }
        return null;
    }

    /**
     * Creates a proxy
     * @param cls the proxy class
     * @param varValues optional list of values which will be used to substitute
     *        template variables specified in the class-level JAX-RS Path annotations
     * @return the proxy
     */
    public <T> T create(Class<T> cls, Object... varValues) {
        return cls.cast(createWithValues(varValues));
    }

    /**
     * Create a Client instance. Proxies and WebClients are Clients.
     * @return the client
     */
    public Client create() {
        if (serviceClass == WebClient.class) {
            return createWebClient();
        }
        return createWithValues();
    }

    /**
     * Create a Client instance. Proxies and WebClients are Clients.
     * @param varValues optional list of values which will be used to substitute
     *        template variables specified in the class-level JAX-RS Path annotations
     *
     * @return the client
     */
    public Client createWithValues(Object... varValues) {
        serviceFactory.setBus(getBus());
        checkResources(false);
        ClassResourceInfo cri = null;
        try {
            Endpoint ep = createEndpoint();
            if (getServiceClass() != null) {
                for (ClassResourceInfo info : serviceFactory.getClassResourceInfo()) {
                    if (info.getServiceClass().isAssignableFrom(getServiceClass())
                        || getServiceClass().isAssignableFrom(info.getServiceClass())) {
                        cri = info;
                        break;
                    }
                }
                if (cri == null) {
                    // can not happen in the reality
                    throw new RuntimeException("Service class " + getServiceClass().getName()
                                               + " is not recognized");
                }
            } else {
                cri = serviceFactory.getClassResourceInfo().get(0);
            }

            boolean isRoot = cri.getURITemplate() != null;
            ClientProxyImpl proxyImpl = null;
            ClientState actualState = getActualState();
            proxyImpl = createClientProxy(cri, isRoot, actualState, varValues);
            initClient(proxyImpl, ep, actualState == null);

            ClassLoader theLoader = proxyLoader == null ? cri.getServiceClass().getClassLoader() : proxyLoader;
            Class<?>[] ifaces = new Class[]{Client.class, InvocationHandlerAware.class, cri.getServiceClass()};
            Client actualClient = (Client)ProxyHelper.getProxy(theLoader, ifaces, proxyImpl);
            proxyImpl.setProxyClient(actualClient);
            notifyLifecycleManager(actualClient);
            this.getServiceFactory().sendEvent(FactoryBeanListener.Event.CLIENT_CREATED, actualClient, ep);
            return actualClient;
        } catch (IllegalArgumentException ex) {
            String message = ex.getLocalizedMessage();
            if (cri != null) {
                String expected = cri.getServiceClass().getSimpleName();
                if ((expected + " is not an interface").equals(message)) {
                    message += "; make sure CGLIB is on the classpath";
                }
            }
            LOG.severe(ex.getClass().getName() + " : " + message);
            throw ex;
        } catch (Exception ex) {
            LOG.severe(ex.getClass().getName() + " : " + ex.getLocalizedMessage());
            throw new RuntimeException(ex);
        }

    }

    protected ClientProxyImpl createClientProxy(ClassResourceInfo cri, boolean isRoot,
                                                ClientState actualState, Object[] varValues) {
        if (actualState == null) {
            return new ClientProxyImpl(URI.create(getAddress()), proxyLoader, cri, isRoot,
                                    inheritHeaders, varValues);
        } else {
            return new ClientProxyImpl(actualState, proxyLoader, cri, isRoot,
                                    inheritHeaders, varValues);
        }
    }

    protected ConduitSelector getConduitSelector(Endpoint ep) {
        ConduitSelector cs = getConduitSelector();
        if (cs == null) {
            cs = new UpfrontConduitSelector();
        }
        cs.setEndpoint(ep);
        return cs;
    }

    protected void initClient(AbstractClient client, Endpoint ep, boolean addHeaders) {

        if (username != null) {
            AuthorizationPolicy authPolicy = new AuthorizationPolicy();
            authPolicy.setUserName(username);
            authPolicy.setPassword(password);
            ep.getEndpointInfo().addExtensor(authPolicy);
        }

        client.getConfiguration().setConduitSelector(getConduitSelector(ep));
        client.getConfiguration().setBus(getBus());
        client.getConfiguration().getOutInterceptors().addAll(getOutInterceptors());
        client.getConfiguration().getOutInterceptors().addAll(ep.getOutInterceptors());
        client.getConfiguration().getInInterceptors().addAll(getInInterceptors());
        client.getConfiguration().getInInterceptors().addAll(ep.getInInterceptors());
        client.getConfiguration().getInFaultInterceptors().addAll(getInFaultInterceptors());

        applyFeatures(client);

        if (headers != null && addHeaders) {
            client.headers(headers);
        }
        ClientProviderFactory factory = ClientProviderFactory.createInstance(getBus());
        setupFactory(factory, ep);

        final Map<String, Object> theProperties = super.getProperties();
        final boolean encodeClientParameters = PropertyUtils.isTrue(theProperties, "url.encode.client.parameters");
        if (encodeClientParameters) {
            final String encodeClientParametersList = theProperties == null ? null
                : (String)getProperties().get("url.encode.client.parameters.list");
            factory.registerUserProvider(new ParamConverterProvider() {

                @SuppressWarnings("unchecked")
                @Override
                public <T> ParamConverter<T> getConverter(Class<T> cls, Type t, Annotation[] anns) {
                    if (cls == String.class
                        && AnnotationUtils.getAnnotation(anns, HeaderParam.class) == null
                        && AnnotationUtils.getAnnotation(anns, CookieParam.class) == null) {
                        return (ParamConverter<T>)new UrlEncodingParamConverter(encodeClientParametersList);
                    }
                    return null;
                }

            });
        }
    }

    protected void applyFeatures(AbstractClient client) {
        if (getFeatures() != null) {
            for (Feature feature : getFeatures()) {
                feature.initialize(client.getConfiguration(), getBus());
            }
        }
    }

    /**
     * Sets the initial client state, can be a thread-safe state.
     * @param initialState the state
     */
    public void setInitialState(ClientState initialState) {
        this.initialState = initialState;
    }


}
