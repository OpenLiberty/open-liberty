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
package org.apache.cxf.jaxws;

import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.ws.Binding;
import javax.xml.ws.EndpointContext;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.WebServicePermission;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.http.HTTPBinding;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingConfiguration;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http_jaxws_spi.JAXWSHttpSpiTransportFactory;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;

public class EndpointImpl extends javax.xml.ws.Endpoint
    implements InterceptorProvider, Configurable, AutoCloseable {
    /**
     * This property controls whether the 'publishEndpoint' permission is checked
     * using only the AccessController (i.e. when SecurityManager is not installed).
     * By default this check is not done as the system property is not set.
     */
    public static final String CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY =
        "org.apache.cxf.jaxws.checkPublishEndpointPermission";

    public static final String CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY_WITH_SECURITY_MANAGER =
        "org.apache.cxf.jaxws.checkPublishEndpointPermissionWithSecurityManager";

    private static final WebServicePermission PUBLISH_PERMISSION =
        new WebServicePermission("publishEndpoint");
    private static final Logger LOG = LogUtils.getL7dLogger(EndpointImpl.class);

    private Bus bus;
    private Object implementor;
    private Server server;
    private JaxWsServerFactoryBean serverFactory;
    private JaxWsServiceFactoryBean serviceFactory;
    private Service service;
    private Map<String, Object> properties;
    private List<Source> metadata;
    private Invoker invoker;
    private Executor executor;
    private String bindingUri;
    private String wsdlLocation;
    private String address;
    private String publishedEndpointUrl;
    private QName endpointName;
    private QName serviceName;
    private Class<?> implementorClass;

    private List<String> schemaLocations;
    private List<Feature> features;
    private List<Interceptor<? extends Message>> in
        = new ModCountCopyOnWriteArrayList<>();
    private List<Interceptor<? extends Message>> out
        = new ModCountCopyOnWriteArrayList<>();
    private List<Interceptor<? extends Message>> outFault
        = new ModCountCopyOnWriteArrayList<>();
    private List<Interceptor<? extends Message>> inFault
        = new ModCountCopyOnWriteArrayList<>();
    @SuppressWarnings("rawtypes")
    private List<Handler> handlers = new ModCountCopyOnWriteArrayList<>();
    private EndpointContext endpointContext;

    /**
     * Flag indicating internal state of this instance.  If true,
     * the instance can have {@link #publish(String, Object)} called
     * and/or settings changed.
     */
    private boolean publishable = true;

    public EndpointImpl(Object implementor) {
        this(BusFactory.getThreadDefaultBus(), implementor);
    }

    public EndpointImpl(Bus b, Object implementor,
                        JaxWsServerFactoryBean sf) {
        this.bus = b;
        this.serverFactory = sf;
        this.implementor = implementor;
    }

    /**
     *
     * @param b
     * @param i The implementor object.
     * @param bindingUri The URI of the Binding being used. Optional.
     * @param wsdl The URL of the WSDL for the service, if different than the URL specified on the
     * WebService annotation. Optional.
     */
    public EndpointImpl(Bus b, Object i, String bindingUri, String wsdl) {
        this(b, i, bindingUri, wsdl, null);
    }
    public EndpointImpl(Bus b, Object i, String bindingUri, String wsdl, WebServiceFeature[] f) {
        bus = b;
        implementor = i;
        this.bindingUri = bindingUri;
        wsdlLocation = wsdl == null ? null : new String(wsdl);
        serverFactory = new JaxWsServerFactoryBean();
        if (f != null) {
            ((JaxWsServiceFactoryBean)serverFactory.getServiceFactory()).setWsFeatures(Arrays.asList(f));
        }
    }

    public EndpointImpl(Bus b, Object i, String bindingUri) {
        this(b, i, bindingUri, (String)null);
    }
    public EndpointImpl(Bus b, Object i, String bindingUri, WebServiceFeature[] features) {
        this(b, i, bindingUri, (String)null, features);
    }

    public EndpointImpl(Bus bus, Object implementor) {
        this(bus, implementor, (String) null);
    }

    public void setBus(Bus b) {
        bus = b;
    }
    public Bus getBus() {
        return bus;
    }

    public Binding getBinding() {
        return ((JaxWsEndpointImpl) getEndpoint()).getJaxwsBinding();
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Executor getExecutor() {
        return executor;
    }

    public Service getService() {
        return service;
    }

    public JaxWsServiceFactoryBean getServiceFactory() {
        return serviceFactory;
    }


    @Override
    public Object getImplementor() {
        return implementor;
    }

    /**
     * Gets the class of the implementor.
     * @return the class of the implementor object
     */
    public Class<?> getImplementorClass() {
        return implementorClass != null ? implementorClass : ClassHelper.getRealClass(bus, implementor);
    }

    public List<Source> getMetadata() {
        return metadata;
    }

    @Override
    public Map<String, Object> getProperties() {
        if (server != null) {
            return server.getEndpoint();
        }
        if (properties == null) {
            properties = new HashMap<>();
        }
        return properties;
    }

    @Override
    public boolean isPublished() {
        return server != null;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation performs no action except to check the publish permission.
     */
    @Override
    public void publish(Object arg0) {
        // Since this does not do anything now, just check the permission
        checkPublishPermission();
    }

    @Override
    public void publish(String addr) {
        doPublish(addr);
    }

    public void setServiceFactory(JaxWsServiceFactoryBean sf) {
        serviceFactory = sf;
    }

    public void setMetadata(List<Source> metadata) {
        checkPublishable();
        this.metadata = metadata;
    }

    @Override
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;

        if (server != null) {
            server.getEndpoint().putAll(properties);
        }
    }

    @Override
    public void stop() {
        if (null != server) {
            server.destroy();
            server = null;
        }
    }

    public String getBeanName() {
        return endpointName.toString() + ".jaxws-endpoint";
    }

    public JaxWsServerFactoryBean getServerFactory() {
        return serverFactory;
    }

    protected void setServerFactory(JaxWsServerFactoryBean bean) {
        this.serverFactory = bean;
    }

    protected void checkProperties() {
        if (properties != null) {
            if (properties.containsKey("javax.xml.ws.wsdl.description")) {
                wsdlLocation = properties.get("javax.xml.ws.wsdl.description").toString();
            }
            if (properties.containsKey(javax.xml.ws.Endpoint.WSDL_PORT)) {
                endpointName = (QName)properties.get(javax.xml.ws.Endpoint.WSDL_PORT);
            }
            if (properties.containsKey(javax.xml.ws.Endpoint.WSDL_SERVICE)) {
                serviceName = (QName)properties.get(javax.xml.ws.Endpoint.WSDL_SERVICE);
            }
        }
    }

    /**
     * Performs the publication action by setting up a {@link Server}
     * instance based on this endpoint's configuration.
     *
     * @param addr the optional endpoint address.
     *
     * @throws IllegalStateException if the endpoint cannot be published/republished
     * @throws SecurityException if permission checking is enabled and policy forbids publishing
     * @throws WebServiceException if there is an error publishing the endpoint
     *
     * @see #checkPublishPermission()
     * @see #checkPublishable()
     * @see #getServer(String)
     */
    protected void doPublish(String addr) {
        checkPublishPermission();
        checkPublishable();

        ServerImpl serv = null;

        ClassLoaderHolder loader = null;
        try {
            if (bus != null) {
                ClassLoader newLoader = bus.getExtension(ClassLoader.class);
                if (newLoader != null) {
                    loader = ClassLoaderUtils.setThreadContextClassloader(newLoader);
                }
            }
            serv = getServer(addr);
            if (addr != null) {
                EndpointInfo endpointInfo = serv.getEndpoint().getEndpointInfo();
                if (endpointInfo.getAddress() == null || !endpointInfo.getAddress().contains(addr)) {
                    endpointInfo.setAddress(addr);
                // Liberty change: 2 lines below are removed
                // }
                // if (publishedEndpointUrl != null) { // Liberty change: end
                    endpointInfo.setProperty(WSDLGetUtils.PUBLISHED_ENDPOINT_URL, publishedEndpointUrl);
                }
                if (publishedEndpointUrl != null && wsdlLocation != null) {
                    //early update the publishedEndpointUrl so that endpoints in the same app sharing the same wsdl
                    //do not require all of them to be queried for wsdl before the wsdl is finally fully updated
                    Definition def = endpointInfo.getService()
                        .getProperty(WSDLServiceBuilder.WSDL_DEFINITION, Definition.class);
                    if (def == null) {
                        def = bus.getExtension(WSDLManager.class).getDefinition(wsdlLocation);
                    }
                    new WSDLGetUtils().updateWSDLPublishedEndpointAddress(def, endpointInfo);
                }

                if (null != properties) {
                    for (Entry<String, Object> entry : properties.entrySet()) {
                        endpointInfo.setProperty(entry.getKey(), entry.getValue());
                    }
                }

                this.address = endpointInfo.getAddress();
            }
            serv.start();
            publishable = false;
        } catch (Exception ex) {
            try {
                stop();
            } catch (Exception e) {
                // Nothing we can do.
            }

            throw new WebServiceException(ex);
        } finally {
            if (loader != null) {
                loader.reset();
            }
        }
    }

    public ServerImpl getServer() {
        return getServer(null);
    }

    public synchronized ServerImpl getServer(String addr) {
        if (server == null) {
            checkProperties();

            ClassLoaderHolder loader = null;
            try {
                if (bus != null) {
                    ClassLoader newLoader = bus.getExtension(ClassLoader.class);
                    if (newLoader != null) {
                        loader = ClassLoaderUtils.setThreadContextClassloader(newLoader);
                    }
                }

                // Initialize the endpointName so we can do configureObject
                QName origEpn = endpointName;
                if (endpointName == null) {
                    JaxWsImplementorInfo implInfo = new JaxWsImplementorInfo(getImplementorClass());
                    endpointName = implInfo.getEndpointName();
                }

                if (serviceFactory != null) {
                    serverFactory.setServiceFactory(serviceFactory);
                }

                /*if (serviceName != null) {
                    serverFactory.getServiceFactory().setServiceName(serviceName);
                }*/

                configureObject(this);
                endpointName = origEpn;

                // Set up the server factory
                serverFactory.setAddress(addr);
                serverFactory.setStart(false);
                serverFactory.setEndpointName(endpointName);
                serverFactory.setServiceBean(implementor);
                serverFactory.setBus(bus);
                serverFactory.setFeatures(getFeatures());
                serverFactory.setInvoker(invoker);
                serverFactory.setSchemaLocations(schemaLocations);
                if (serverFactory.getProperties() != null) {
                    serverFactory.getProperties().putAll(properties);
                } else {
                    serverFactory.setProperties(properties);
                }

                // Be careful not to override any serverfactory settings as a user might
                // have supplied their own.
                if (getWsdlLocation() != null) {
                    serverFactory.setWsdlURL(getWsdlLocation());
                }

                if (bindingUri != null) {
                    serverFactory.setBindingId(bindingUri);
                }

                if (serviceName != null) {
                    serverFactory.getServiceFactory().setServiceName(serviceName);
                }

                if (implementorClass != null) {
                    serverFactory.setServiceClass(implementorClass);
                }

                if (executor != null) {
                    serverFactory.getServiceFactory().setExecutor(executor);
                }
                if (!handlers.isEmpty()) {
                    serverFactory.addHandlers(handlers);
                }

                configureObject(serverFactory);

                server = serverFactory.create();

                org.apache.cxf.endpoint.Endpoint endpoint = getEndpoint();
                if (in != null) {
                    endpoint.getInInterceptors().addAll(in);
                }
                if (out != null) {
                    endpoint.getOutInterceptors().addAll(out);
                }
                if (inFault != null) {
                    endpoint.getInFaultInterceptors().addAll(inFault);
                }
                if (outFault != null) {
                    endpoint.getOutFaultInterceptors().addAll(outFault);
                }

                if (properties != null) {
                    endpoint.putAll(properties);
                }

                configureObject(endpoint.getService());
                configureObject(endpoint);
                this.service = endpoint.getService();

                if (getWsdlLocation() == null) {
                    //hold onto the wsdl location so cache won't clear till we go away
                    setWsdlLocation(serverFactory.getWsdlURL());
                }

                if (serviceName == null) {
                    setServiceName(serverFactory.getServiceFactory().getServiceQName());
                }
                if (endpointName == null) {
                    endpointName = endpoint.getEndpointInfo().getName();
                }
            } finally {
                if (loader != null) {
                    loader.reset();
                }
            }
        }
        return (ServerImpl) server;
    }

    org.apache.cxf.endpoint.Endpoint getEndpoint() {
        return getServer(null).getEndpoint();
    }

    private void configureObject(Object instance) {
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(instance);
        }
    }

    protected void checkPublishPermission() {
        SecurityManager sm = System.getSecurityManager();
        boolean checkPublishEndpointPermissionWithSecurityManager
            = Boolean.parseBoolean(
                      SystemPropertyAction.getProperty(
                                         CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY_WITH_SECURITY_MANAGER,
                                         "true"));
        if (checkPublishEndpointPermissionWithSecurityManager && sm != null) {
            sm.checkPermission(PUBLISH_PERMISSION);
        } else if (Boolean.getBoolean(CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY)) {
            AccessController.checkPermission(PUBLISH_PERMISSION);
        }
    }

    /**
     * Checks the value of {@link #publishable} and throws
     * an {@link IllegalStateException} if the value is {@code false}.
     *
     * @throws IllegalStateException if {@link #publishable} is false
     */
    protected void checkPublishable() {
        if (!this.publishable) {
            throw new IllegalStateException("Cannot invoke method "
                    + "after endpoint has been published.");
        }
    }

    public void publish() {
        publish(getAddress());
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
    * The published endpoint url is used for excplicitely specifying the url of the
    * endpoint that would show up the generated wsdl definition, when the service is
    * brought on line.
    * @return
    */
    public String getPublishedEndpointUrl() {
        return publishedEndpointUrl;
    }

    public void setPublishedEndpointUrl(String publishedEndpointUrl) {
        this.publishedEndpointUrl = publishedEndpointUrl;
    }

    public QName getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(QName endpointName) {
        this.endpointName = endpointName;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    public String getWsdlLocation() {
        return wsdlLocation;
    }

    public void setWsdlLocation(String wsdlLocation) {
        if (wsdlLocation != null) {
            this.wsdlLocation = new String(wsdlLocation);
        } else {
            this.wsdlLocation = null;
        }
    }

    public void setBindingUri(String binding) {
        this.bindingUri = binding;
    }

    public String getBindingUri() {
        return this.bindingUri;
    }

    public void setDataBinding(DataBinding dataBinding) {
        serverFactory.setDataBinding(dataBinding);
    }

    public DataBinding getDataBinding() {
        return serverFactory.getDataBinding();
    }

    public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
        if (server == null) {
            return outFault;
        }
        return new DoubleAddInterceptorList(outFault, server.getEndpoint().getOutFaultInterceptors());
    }

    public List<Interceptor<? extends Message>> getInFaultInterceptors() {
        if (server == null) {
            return inFault;
        }
        return new DoubleAddInterceptorList(inFault, server.getEndpoint().getInFaultInterceptors());
    }

    public List<Interceptor<? extends Message>> getInInterceptors() {
        if (server == null) {
            return in;
        }
        return new DoubleAddInterceptorList(in, server.getEndpoint().getInInterceptors());
    }

    public List<Interceptor<? extends Message>> getOutInterceptors() {
        if (server == null) {
            return out;
        }
        return new DoubleAddInterceptorList(out, server.getEndpoint().getOutInterceptors());
    }

    class DoubleAddInterceptorList implements List<Interceptor<? extends Message>> {
        List<Interceptor<? extends Message>> orig;
        List<Interceptor<? extends Message>> other;
        DoubleAddInterceptorList(List<Interceptor<? extends Message>> a1,
                                 List<Interceptor<? extends Message>> a2) {
            orig = a1;
            other = a2;
        }
        public boolean add(Interceptor<? extends Message> e) {
            other.add(e);
            return orig.add(e);
        }
        public void add(int index, Interceptor<? extends Message> element) {
            other.add(element);
            orig.add(index, element);
        }
        public boolean addAll(Collection<? extends Interceptor<? extends Message>> c) {
            other.addAll(c);
            return orig.addAll(c);
        }
        public boolean addAll(int index, Collection<? extends Interceptor<? extends Message>> c) {
            other.addAll(c);
            return orig.addAll(index, c);
        }
        public void clear() {
            orig.clear();
        }
        public boolean contains(Object o) {
            return orig.contains(o);
        }
        public boolean containsAll(Collection<?> c) {
            return orig.containsAll(c);
        }
        public Interceptor<? extends Message> get(int index) {
            return orig.get(index);
        }
        public int indexOf(Object o) {
            return orig.indexOf(o);
        }
        public boolean isEmpty() {
            return orig.isEmpty();
        }
        public Iterator<Interceptor<? extends Message>> iterator() {
            return orig.iterator();
        }
        public int lastIndexOf(Object o) {
            return orig.lastIndexOf(o);
        }
        public ListIterator<Interceptor<? extends Message>> listIterator() {
            return orig.listIterator();
        }
        public ListIterator<Interceptor<? extends Message>> listIterator(int index) {
            return orig.listIterator(index);
        }
        public boolean remove(Object o) {
            other.remove(o);
            return orig.remove(o);
        }
        public Interceptor<? extends Message> remove(int index) {
            Interceptor<? extends Message> o = orig.remove(index);
            if (o == null) {  // Liberty change: added if clause
              other.remove(o);
            }
            return o; // Liberty change: end
        }
        public boolean removeAll(Collection<?> c) {
            other.removeAll(c);
            return orig.removeAll(c);
        }
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }
        public Interceptor<? extends Message> set(int index, Interceptor<? extends Message> element) {
            Interceptor<? extends Message> o = orig.set(index, element);
            if (o != null) {
                int idx = other.indexOf(o);
                other.set(idx, element);
            }
            return o;
        }
        public int size() {
            return orig.size();
        }
        public List<Interceptor<? extends Message>> subList(int fromIndex, int toIndex) {
            return orig.subList(fromIndex, toIndex);
        }
        public Object[] toArray() {
            return orig.toArray();
        }
        public <T> T[] toArray(T[] a) {
            return orig.toArray(a);
        }
    }

    public void setInInterceptors(List<Interceptor<? extends Message>> interceptors) {
        in = interceptors;
    }

    public void setInFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
        inFault = interceptors;
    }

    public void setOutInterceptors(List<Interceptor<? extends Message>> interceptors) {
        out = interceptors;
    }

    public void setOutFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
        outFault = interceptors;
    }
    public void setHandlers(@SuppressWarnings("rawtypes") List<Handler> h) {
        handlers.clear();
        handlers.addAll(h);
    }
    @SuppressWarnings("rawtypes")
    public List<Handler> getHandlers() {
        return handlers;
    }

    public List<Feature> getFeatures() {
        if (features == null) {
            features = new ArrayList<>();
        }
        return features;
    }

    public void setFeatures(List<? extends Feature> features) {
        this.features = CastUtils.cast(features);
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }

    public void setImplementorClass(Class<?> implementorClass) {
        this.implementorClass = implementorClass;
    }

    public void setTransportId(String transportId) {
        serverFactory.setTransportId(transportId);
    }

    public String getTransportId() {
        return serverFactory.getTransportId();
    }

    public void setBindingConfig(BindingConfiguration config) {
        serverFactory.setBindingConfig(config);
    }

    public BindingConfiguration getBindingConfig() {
        return serverFactory.getBindingConfig();
    }

    public List<String> getSchemaLocations() {
        return schemaLocations;
    }

    public void setSchemaLocations(List<String> schemaLocations) {
        this.schemaLocations = schemaLocations;
    }

    public EndpointReference getEndpointReference(Element... referenceParameters) {
        if (!isPublished()) {
            throw new WebServiceException(new org.apache.cxf.common.i18n.Message("ENDPOINT_NOT_PUBLISHED",
                                                                                 LOG).toString());
        }

        if (getBinding() instanceof HTTPBinding) {
            throw new UnsupportedOperationException(new org.apache.cxf.common.i18n.Message(
                                                        "GET_ENDPOINTREFERENCE_UNSUPPORTED_BINDING",
                                                        LOG).toString());
        }

        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        builder.address(address);
        builder.serviceName(serviceName);
        builder.endpointName(endpointName);
        if (referenceParameters != null) {
            for (Element referenceParameter : referenceParameters) {
                builder.referenceParameter(referenceParameter);
            }
        }
        builder.wsdlDocumentLocation(wsdlLocation);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(EndpointReferenceBuilder.class.getClassLoader());
            return builder.build();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public <T extends EndpointReference> T getEndpointReference(Class<T> clazz,
                                                                Element... referenceParameters) {
        if (W3CEndpointReference.class.isAssignableFrom(clazz)) {
            return clazz.cast(getEndpointReference(referenceParameters));
        }
        throw new WebServiceException(new org.apache.cxf.common.i18n.Message(
            "ENDPOINTREFERENCE_TYPE_NOT_SUPPORTED", LOG, clazz
            .getName()).toString());
    }

    public void setEndpointContext(EndpointContext ctxt) {
        endpointContext = ctxt;
    }
    public EndpointContext getEndpointContext() {
        return endpointContext;
    }
    public void publish(javax.xml.ws.spi.http.HttpContext context) {
        ServerFactoryBean sf = getServerFactory();
        if (sf.getDestinationFactory() == null) {
            sf.setDestinationFactory(new JAXWSHttpSpiTransportFactory(context));
        }
        publish(context.getPath());
    }

    @Override
    public void close() throws Exception {
        stop();
    }

}
