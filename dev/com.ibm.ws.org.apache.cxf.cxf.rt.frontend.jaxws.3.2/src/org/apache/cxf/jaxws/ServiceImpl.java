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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.ws.Dispatch;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.HandlerResolver;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.spi.ServiceDelegate;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapAddress;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapBinding;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.i18n.UncheckedException;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.ServiceContractResolverRegistry;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.binding.soap.JaxWsSoapBindingConfiguration;
import org.apache.cxf.jaxws.handler.HandlerResolverImpl;
import org.apache.cxf.jaxws.handler.PortInfoImpl;
import org.apache.cxf.jaxws.spi.ProviderImpl;
import org.apache.cxf.jaxws.support.BindingID;
import org.apache.cxf.jaxws.support.DummyImpl;
import org.apache.cxf.jaxws.support.JaxWsClientEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.AbstractServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl.http.AddressType;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.wsdl11.WSDLServiceFactory;

public class ServiceImpl extends ServiceDelegate {

    private static final Logger LOG = LogUtils.getL7dLogger(ServiceImpl.class);
    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();

    private Bus bus;
    private String wsdlURL;

    private HandlerResolver handlerResolver;
    private Executor executor;
    private QName serviceName;
    private Class<?> clazz;

    private Map<QName, PortInfoImpl> portInfos = new HashMap<>();
    private WebServiceFeature[] serviceFeatures;

    public ServiceImpl(Bus b, URL url, QName name, Class<?> cls, WebServiceFeature ... f) {
        clazz = cls;
        this.serviceName = name;

        //If the class is a CXFService, then it will call initialize directly later
        //when the bus is determined
        if (cls == null || !CXFService.class.isAssignableFrom(cls)) {
            initialize(b, url, f);
        }
    }

    final void initialize(Bus b, URL url, WebServiceFeature ... f) {
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change #26529
        if (b == null) {
            b = BusFactory.getThreadDefaultBus(true);
        }
        serviceFeatures = f;
        bus = b;
        handlerResolver = new HandlerResolverImpl(bus, serviceName, clazz);

        if (null == url && null != bus) {
            ServiceContractResolverRegistry registry =
                bus.getExtension(ServiceContractResolverRegistry.class);
            if (null != registry) {
                URI uri = registry.getContractLocation(serviceName);
                if (null != uri) {
                    try {
                        url = uri.toURL();
                    } catch (MalformedURLException e) {
                        if(LOG.isLoggable(Level.FINE))  { // Liberty Change begin #26529
                            LOG.fine("resolve qname failed: " + serviceName);
                        }  // Liberty Change end #26529
                        throw new WebServiceException(e);
                    }
                }
            }
        }

        wsdlURL = url == null ? null : url.toString();

        if (url != null) {
            try {
                initializePorts();
            } catch (ServiceConstructionException e) {
                throw new WebServiceException(e);
            }
        }
    }

    private void initializePorts() {
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change #26529
        try {
            Definition def = bus.getExtension(WSDLManager.class).getDefinition(wsdlURL);
            javax.wsdl.Service serv = def.getService(serviceName);
            if (serv == null) {
                throw new WebServiceException("Could not find service named " + serviceName
                                              + " in wsdl " + wsdlURL);
            }

            Map<String, Port> wsdlports = CastUtils.cast(serv.getPorts());
            if(isFinestEnabled) { // Liberty Change: prevention of NPE begin
                if(wsdlports != null) {
                    LOG.finest("WSDL ports size: " + wsdlports.size());
                } else {
                    LOG.finest("wsdlPorts is null, falling back to add ports via the EndpointInfo");
                }
            } // Liberty Change: prevention of NPE end
            for (Port port : wsdlports.values()) {
                QName name = new QName(serviceName.getNamespaceURI(), port.getName());

                String address = null;
                String bindingID = null;
                List<? extends ExtensibilityElement> extensions
                    = CastUtils.cast(port.getBinding().getExtensibilityElements());
                
                if (!extensions.isEmpty()) {
                    ExtensibilityElement e = extensions.get(0);
                    if (e instanceof SoapBinding) {
                        bindingID = SOAPBinding.SOAP11HTTP_BINDING;
                    } else if (e instanceof SOAP12Binding) {
                        bindingID = SOAPBinding.SOAP12HTTP_BINDING;
                    } else if (e instanceof javax.wsdl.extensions.soap.SOAPBinding) {
                        bindingID = SOAPBinding.SOAP11HTTP_BINDING;
                    }
                }
                if(isFinestEnabled)  {  // Liberty Change begin #26529
                    LOG.finest("Binding id: " + bindingID);
                }  // Liberty Change end #26529
                extensions = CastUtils.cast(port.getExtensibilityElements());
                if (!extensions.isEmpty()) {
                    ExtensibilityElement e = extensions.get(0);
                    if (e instanceof SoapAddress) {
                        address = ((SoapAddress)e).getLocationURI();
                    } else if (e instanceof AddressType) {
                        address = ((AddressType)e).getLocation();
                    } else if (e instanceof SOAP12Address) {
                        address = ((SOAP12Address)e).getLocationURI();
                    } else if (e instanceof SOAPAddress) {
                        address = ((SOAPAddress)e).getLocationURI();
                    } else if (e instanceof HTTPAddress) {
                        address = ((HTTPAddress)e).getLocationURI();
                    }
                }
                addPort(name, bindingID, address);
            }
        } catch (WebServiceException e) {
            throw e;
        } catch (Throwable e) {
            if (e instanceof UncheckedException && LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, e.getLocalizedMessage(), e);
            }
            WSDLServiceFactory sf = new WSDLServiceFactory(bus, wsdlURL, serviceName);
            Service service = sf.create();
            for (ServiceInfo si : service.getServiceInfos()) {
                for (EndpointInfo ei : si.getEndpoints()) {
                    String bindingID = BindingID.getJaxwsBindingID(ei.getTransportId());
                    addPort(ei.getName(), bindingID, ei.getAddress());
                }
            }
        }
    }

    public final void addPort(QName portName, String bindingId, String address) {
        PortInfoImpl portInfo = new PortInfoImpl(bindingId, portName, serviceName);
        portInfo.setAddress(address);
        portInfos.put(portName, portInfo);
    }

    private List<WebServiceFeature> getAllFeatures(WebServiceFeature[] features) {
        List<WebServiceFeature> f = new ArrayList<>();
        if (features != null) {
            Collections.addAll(f, features);
        }
        if (serviceFeatures != null) {
            Collections.addAll(f, serviceFeatures);
        }
        return f;
    }

    private JaxWsClientEndpointImpl getJaxwsEndpoint(QName portName, AbstractServiceFactoryBean sf,
                                      WebServiceFeature...features) {
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change #26529
        Service service = sf.getService();
        EndpointInfo ei;
        if (portName == null) {
            ei = service.getServiceInfos().get(0).getEndpoints().iterator().next();
            if (isFinestEnabled) {  // Liberty Change begin #26529
                LOG.finest("EndpointInfo obtained from ServiceInfo because portname is null: " + ei);
            }  // Liberty Change end #26529
        } else {
            ei = service.getEndpointInfo(portName);
            if (isFinestEnabled) {  // Liberty Change begin #26529
                LOG.finest("EndpointInfo obtained from Service using portname: " + ei);
            }  // Liberty Change end #26529
            if (ei == null) {
                PortInfoImpl portInfo = getPortInfo(portName);
                if (isFinestEnabled) {  // Liberty Change begin #26529
                    LOG.finest("PortInfoImpl that is obtained from ServiceInpl.portInfos with key portname: " + portInfo);
                }  // Liberty Change end #26529
                if (null != portInfo) {
                    try {
                        ei = createEndpointInfo(sf, portName, portInfo);
                    } catch (BusException e) {
                        throw new WebServiceException(e);
                    }
                    if (isFinestEnabled) { // Liberty Change begin #26529
                        LOG.finest("Endpoint was null, it's created using endpoint info: " + ei); 
                    }  // Liberty Change end #26529
                }
            }
        }

        if (ei == null) {
            Message msg = new Message("INVALID_PORT", BUNDLE, portName);
            throw new WebServiceException(msg.toString());
        }

        //When the dispatch is created from EPR, the EPR's address will be set in portInfo
        PortInfoImpl portInfo = getPortInfo(portName);
        if (portInfo != null
            && portInfo.getAddress() != null
            && !portInfo.getAddress().equals(ei.getAddress())) {
            ei.setAddress(portInfo.getAddress());
            if (isFinestEnabled) {  // Liberty Change begin #26529
                LOG.finest("Setting EndpointInfo, " + ei + ", with Address: " + portInfo.getAddress());
            } // Liberty Change end #26529
        }

        try {
            return new JaxWsClientEndpointImpl(bus, service, ei, this,
                                               getAllFeatures(features));
        } catch (EndpointException e) {
            throw new WebServiceException(e);
        }
    }

    private AbstractServiceFactoryBean createDispatchService(DataBinding db) {
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change #26529
        AbstractServiceFactoryBean serviceFactory;

        final Service dispatchService;

        if (null != wsdlURL) {
            WSDLServiceFactory sf = new WSDLServiceFactory(bus, wsdlURL, serviceName);
            dispatchService = sf.create();
            dispatchService.setDataBinding(db);
            serviceFactory = sf;
            if (isFinestEnabled) {  // Liberty Change begin #26529
                LOG.finest("ServiceFactory is created using WSDL URL with WSDLServiceFactory: " + sf);
            }  // Liberty Change end #26529
        } else {
            ReflectionServiceFactoryBean sf = new JaxWsServiceFactoryBean();
            sf.setBus(bus);
            sf.setServiceName(serviceName);
            // maybe we can find another way to create service which have no SEI
            sf.setServiceClass(DummyImpl.class);
            sf.setDataBinding(db);
            dispatchService = sf.create();
            serviceFactory = sf;
            if (isFinestEnabled) {  // Liberty Change begin #26529
                LOG.finest("ServiceFactory is created trough ReflectionServiceFactoryBean: " + sf);
            }  // Liberty Change end #26529
        }
        configureObject(dispatchService);
        for (ServiceInfo si : dispatchService.getServiceInfos()) {
            si.setProperty("soap.force.doclit.bare", Boolean.TRUE);
            if (null == wsdlURL) {
                for (EndpointInfo ei : si.getEndpoints()) {
                    ei.setProperty("soap.no.validate.parts", Boolean.TRUE);
                }
            }

            for (BindingInfo bind : si.getBindings()) {
                for (BindingOperationInfo bop : bind.getOperations()) {
                    //force to bare, no unwrapping
                    if (bop.isUnwrappedCapable()) {
                        bop.getOperationInfo().setUnwrappedOperation(null);
                        bop.setUnwrappedOperation(null);
                    }
                }
            }
        }
        return serviceFactory;
    }

    public Executor getExecutor() {
        return executor;
    }

    public HandlerResolver getHandlerResolver() {
        return handlerResolver;
    }

    public <T> T getPort(Class<T> serviceEndpointInterface) {
        return getPort(serviceEndpointInterface, new WebServiceFeature[]{});
    }

    public <T> T getPort(Class<T> serviceEndpointInterface, WebServiceFeature... features) {
        try {
            return createPort(null, null, serviceEndpointInterface, features);
        } catch (ServiceConstructionException e) {
            throw new WebServiceException(e);
        }
    }

    public <T> T getPort(QName portName, Class<T> serviceEndpointInterface) {
        return getPort(portName, serviceEndpointInterface, new WebServiceFeature[]{});
    }

    public <T> T getPort(QName portName, Class<T> serviceEndpointInterface, WebServiceFeature... features) {
        if (portName == null) {
            throw new WebServiceException(BUNDLE.getString("PORT_NAME_NULL_EXC"));
        }

        try {
            return createPort(portName, null, serviceEndpointInterface, features);
        } catch (ServiceConstructionException e) {
            throw new WebServiceException(e);
        }
    }

    public <T> T getPort(EndpointReferenceType endpointReference,
                            Class<T> type) {
        return getPort(endpointReference, type, new WebServiceFeature[]{});
    }

    public <T> T getPort(EndpointReferenceType endpointReference, Class<T> type,
                         WebServiceFeature... features) {
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change #26529
        endpointReference = EndpointReferenceUtils.resolve(endpointReference, bus);
        QName serviceQName = EndpointReferenceUtils.getServiceName(endpointReference, bus);
        String portName = EndpointReferenceUtils.getPortName(endpointReference);
        if (isFinestEnabled) {  // Liberty Change begin #26529
            LOG.finest("Service QName: " + serviceQName);
            LOG.finest("Port Name: " + portName);
        }  // Liberty Change end #26529
        
        QName portQName = null;
        if (portName != null && serviceQName != null) {
            String ns = serviceQName.getNamespaceURI();
            if (StringUtils.isEmpty(ns)) {
                //hack to workaround a xalan bug
                for (QName qn : portInfos.keySet()) {
                    if (portName.equals(qn.getLocalPart())) {
                        ns = qn.getNamespaceURI();
                    }
                }
            }
            if (StringUtils.isEmpty(ns) && serviceName != null) {
                ns = serviceName.getNamespaceURI();
            }
            portQName = new QName(ns, portName);
        }

        return createPort(portQName, endpointReference, type, features);
    }

    public Iterator<QName> getPorts() {
        return portInfos.keySet().iterator();
    }

    public QName getServiceName() {
        return serviceName;
    }

    public URL getWSDLDocumentLocation() {
        try {
            return new URL(wsdlURL);
        } catch (MalformedURLException e) {
            throw new WebServiceException(e);
        }
    }

    public void setExecutor(Executor e) {
        this.executor = e;
    }

    public void setHandlerResolver(HandlerResolver hr) {
        handlerResolver = hr;
    }

    public Bus getBus() {
        return bus;
    }

    protected <T> T createPort(QName portName, EndpointReferenceType epr, Class<T> serviceEndpointInterface) {
        return createPort(portName, epr, serviceEndpointInterface, new WebServiceFeature[]{});
    }

    protected <T> T createPort(QName portName, EndpointReferenceType epr, Class<T> serviceEndpointInterface,
                               WebServiceFeature... features) {
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change #26529
        if (LOG.isLoggable(Level.FINE)) {   // Liberty Change begin #26529
            LOG.fine("creating port for portName" + portName);
            LOG.fine("endpoint reference:" + epr);
            LOG.fine("endpoint interface:" + serviceEndpointInterface);
        }  // Liberty Change end #26529

        final JaxWsProxyFactoryBean proxyFac = new JaxWsProxyFactoryBean(); // Liberty Change
        JaxWsClientFactoryBean clientFac = (JaxWsClientFactoryBean) proxyFac.getClientFactoryBean();
        JaxWsServiceFactoryBean serviceFactory = (JaxWsServiceFactoryBean) proxyFac.getServiceFactory();
        List<WebServiceFeature> f = getAllFeatures(features);
        proxyFac.initFeatures();
        if (f != null) {
            serviceFactory.setWsFeatures(f);
        }


        proxyFac.setBus(bus);
        proxyFac.setServiceClass(serviceEndpointInterface);
        proxyFac.setServiceName(serviceName);
        if (epr != null
            && epr.getAddress() != null
            && epr.getAddress().getValue() != null) {
            clientFac.setAddress(epr.getAddress().getValue());
            if (isFinestEnabled) {   // Liberty Change begin #26529
                LOG.finest("Address is set to JaxWsClientFactoryBean with the address that is obtained from EndpointReferenceType: " + clientFac.getAddress());
            }  // Liberty Change end #26529
        }

        if (wsdlURL != null) {
            proxyFac.setWsdlURL(wsdlURL);
            if (isFinestEnabled) {   // Liberty Change begin #26529
                LOG.finest("WSDL URL(wsdlURL) is set to JaxWsProxyFactoryBean with the value that is obtained from ServivceImpl: " + wsdlURL);
            }  // Liberty Change end #26529
        }

        configureObject(proxyFac);
        configureObject(clientFac);

        if (portName == null) {
            QName portTypeName = getPortTypeName(serviceEndpointInterface);

            Service service = serviceFactory.getService();
            
            if (service == null) {
                serviceFactory.setServiceClass(serviceEndpointInterface);
                serviceFactory.setBus(getBus());
                service = serviceFactory.create();
                if (isFinestEnabled) {   // Liberty Change begin #26529
                    LOG.finest("Service object was null. It's created from JaxWsServiceFactoryBean: " + service);
                }
            }

            EndpointInfo ei = ServiceModelUtil.findBestEndpointInfo(portTypeName, service.getServiceInfos());
            if (ei != null) {
                portName = ei.getName();
            } else {
                portName = serviceFactory.getEndpointName();
            }
        }

        serviceFactory.setEndpointName(portName);

        if (epr != null) {
            clientFac.setEndpointReference(epr);
        }
        PortInfoImpl portInfo = portInfos.get(portName);
        if (portInfo != null) {
            clientFac.setBindingId(portInfo.getBindingID());
            clientFac.setAddress(portInfo.getAddress());
            if (isFinestEnabled) {   // Liberty Change begin #26529
                LOG.finest("Binding id is set into JaxWsClientFactoryBean(clientFac): " + clientFac.getBindingId());
                LOG.finest("Address is  set into JaxWsClientFactoryBean(clientFac): " + clientFac.getAddress());
            }
        }
        //configureObject(portName.toString() + ".jaxws-client.proxyFactory", proxyFac);
        if (clazz != ServiceImpl.class) {
            // handlerchain should be on the generated Service object
            proxyFac.setLoadHandlers(false);
        }
        // Libery Change Start 
        Object obj =  AccessController.doPrivileged(new PrivilegedAction<Object>() { 
            @Override
            public Object run() {
                        return proxyFac.create();
  
            }
        });
        // Liberty Change End
         proxyFac.create();

        // Configure the Service
        Service service = serviceFactory.getService();
        configureObject(service);

        // Configure the JaxWsEndpoitnImpl
        Client client = ClientProxy.getClient(obj);
        client.getEndpoint().setExecutor(executor);
        client.setExecutor(executor);
        JaxWsEndpointImpl jaxwsEndpoint = (JaxWsEndpointImpl) client.getEndpoint();
        configureObject(jaxwsEndpoint);
        @SuppressWarnings("rawtypes")
        List<Handler> hc = jaxwsEndpoint.getJaxwsBinding().getHandlerChain();

        hc.addAll(handlerResolver.getHandlerChain(portInfos.get(portName)));
        jaxwsEndpoint.getJaxwsBinding().setHandlerChain(hc);
        if(LOG.isLoggable(Level.FINE))  {   // Liberty Change begin #26529
            LOG.log(Level.FINE, "created proxy", obj);
        }
        if (portInfo == null) {
            addPort(portName, clientFac.getBindingId(), clientFac.getAddress());
            if (isFinestEnabled) {   // Liberty Change begin #26529
                LOG.finest("PortInfoImpl was null. A port is added with name: " + portName + ", binding id: " + clientFac.getBindingId() + ", address: " + clientFac.getAddress());
            }
        }
        return serviceEndpointInterface.cast(obj);
    }

    private EndpointInfo createEndpointInfo(AbstractServiceFactoryBean serviceFactory,
                                            QName portName,
                                            PortInfoImpl portInfo) throws BusException {
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change #26529
        String address = portInfo.getAddress();
        String bindingID = BindingID.getBindingID(portInfo.getBindingID());

        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        try {
            //the bindingId might be the transportId, just attempt to
            //load it to force the factory to load
            dfm.getDestinationFactory(bindingID);
        } catch (BusException ex) {
            //ignore
        }
        DestinationFactory df = dfm.getDestinationFactoryForUri(address);
        if (isFinestEnabled) {   // Liberty Change begin #26529
            LOG.finest("DestinationFactory that is obtained from DestinationFactoryManager: " + df != null ? df.getClass().getName() : "" + ", with address: " + address);
        }
        final String transportId;
        if (df != null && df.getTransportIds() != null && !df.getTransportIds().isEmpty()) {
            transportId = df.getTransportIds().get(0);
            if (isFinestEnabled) {   // Liberty Change begin #26529
                LOG.finest("transportId that is obtained from DestinationFactoryManager: " + transportId);
            }  // Liberty Change end #26529
        } else {
            transportId = bindingID;
            if (isFinestEnabled) {   // Liberty Change begin #26529
                LOG.finest("bindingID that is assigned to transportId : " + transportId);
            }  // Liberty Change end #26529
        }

        Object config = null;
        if (serviceFactory instanceof JaxWsServiceFactoryBean) {
            config = new JaxWsSoapBindingConfiguration((JaxWsServiceFactoryBean)serviceFactory);
        }
        BindingInfo bindingInfo = bus.getExtension(BindingFactoryManager.class).getBindingFactory(bindingID)
                .createBindingInfo(serviceFactory.getService(), bindingID, config);
        if (isFinestEnabled) {   // Liberty Change begin #26529
            LOG.finest("BindingInfo that is created from BindingFactory: " + bindingInfo + ", with service: " + serviceFactory.getService() + ", BindingID: " + bindingID + " and config: " + config);
        }


        Service service = serviceFactory.getService();
        service.getServiceInfos().get(0).addBinding(bindingInfo);

        EndpointInfo ei = new EndpointInfo(service.getServiceInfos().get(0), transportId);
        ei.setName(portName);
        ei.setAddress(address);
        ei.setBinding(bindingInfo);
        if (isFinestEnabled) {   // Liberty Change begin #26529
            LOG.finest("Newly instantiated EndpointInfo : " + ei + ", with service info: " + service.getServiceInfos().get(0) + " and with transport id: " + transportId);
        }  // Liberty Change end #26529
        
        service.getServiceInfos().get(0).addEndpoint(ei);
        return ei;
    }

    private void configureObject(Object instance) {
        configureObject(null, instance);
    }

    private void configureObject(String name, Object instance) {
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(name, instance);
        }
    }

    private PortInfoImpl getPortInfo(QName portName) {
        if (portName == null) {
            return null;
        }
        return portInfos.get(portName);
    }

    private QName getPortTypeName(Class<?> serviceEndpointInterface) {
        Class<?> seiClass = serviceEndpointInterface;
        if (!serviceEndpointInterface.isAnnotationPresent(WebService.class)) {
            Message msg = new Message("SEI_NO_WEBSERVICE_ANNOTATION", BUNDLE, serviceEndpointInterface
                .getCanonicalName());
            throw new WebServiceException(msg.toString());
        }

        if (!serviceEndpointInterface.isInterface()) {
            WebService webService = serviceEndpointInterface.getAnnotation(WebService.class);
            String epi = webService.endpointInterface();
            if (epi.length() > 0) {
                try {
                    seiClass = Thread.currentThread().getContextClassLoader().loadClass(epi);
                } catch (ClassNotFoundException e) {
                    Message msg = new Message("COULD_NOT_LOAD_CLASS", BUNDLE, epi);
                    throw new WebServiceException(msg.toString());
                }
                if (!seiClass.isAnnotationPresent(javax.jws.WebService.class)) {
                    Message msg = new Message("SEI_NO_WEBSERVICE_ANNOTATION", BUNDLE,
                                              seiClass.getCanonicalName());
                    throw new WebServiceException(msg.toString());
                }
            } else {    // Liberty Change begin #26529
                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.finest("Fully qualified name of WebService Endpoint Interface(epi) is empty.");
                }  // Liberty Change end #26529
            }
        }

        WebService webService = seiClass.getAnnotation(WebService.class);
        String name = webService.name();
        if (name.length() == 0) {
            name = seiClass.getSimpleName();
        }

        String tns = webService.targetNamespace();
        if (tns.isEmpty()) {
            tns = PackageUtils.getNamespace(PackageUtils.getPackageName(seiClass));
            if (LOG.isLoggable(Level.FINEST)) {   // Liberty Change begin #26529
                LOG.finest("Target namespace is not defined in WebService annotation. It will be obtained from SEI class package name.");
            }  // Liberty Change end #26529
        }

        return new QName(tns, name);
    }

    @Override
    public <T> Dispatch<T> createDispatch(QName portName, Class<T> type, Mode mode) {
        return createDispatch(portName, type, mode, new WebServiceFeature[]{});
    }

    @Override
    public <T> Dispatch<T> createDispatch(QName portName,
                                          Class<T> type,
                                          Mode mode,
                                          WebServiceFeature... features) {
        return createDispatch(portName, type, null, mode, features);
    }
    public <T> Dispatch<T> createDispatch(QName portName,
                                          Class<T> type,
                                          JAXBContext context,
                                          Mode mode,
                                          WebServiceFeature... features) {
        
        boolean isFinestEnabled = LOG.isLoggable(Level.FINEST);   // Liberty Change #26529
        //using this instead of JaxWsClientFactoryBean so that handlers are configured
        JaxWsProxyFactoryBean clientFac = new JaxWsProxyFactoryBean();

        //Initialize Features.
        configureObject(portName.toString() + ".jaxws-client.proxyFactory", clientFac);

        final AbstractServiceFactoryBean sf;
        try {
            DataBinding db;
            if (context != null) {
                db = new JAXBDataBinding(context);
            } else {
                db = new SourceDataBinding(type);
            }
            sf = createDispatchService(db);
        } catch (ServiceConstructionException e) {
            throw new WebServiceException(e);
        }
        JaxWsEndpointImpl endpoint = getJaxwsEndpoint(portName, sf, features);
        // if the client factory has properties specified, then set those into the endpoint
        if (clientFac.getProperties() != null) {
            endpoint.putAll(clientFac.getProperties());
        }
        // add all the client factory features onto the endpoint feature list
        endpoint.getFeatures().addAll(clientFac.getFeatures());
        // if the client factory has a bus specified (other than the thread default),
        // then use that for the client.  Otherwise use the bus from this service.
        Bus clientBus = getBus();
        if (clientFac.getBus() != BusFactory.getThreadDefaultBus(false)
            && clientFac.getBus() != null) {
            clientBus = clientFac.getBus();
        }
        @SuppressWarnings("rawtypes")
        List<Handler> hc = clientFac.getHandlers();
        //CXF-3956
        hc.addAll(handlerResolver.getHandlerChain(portInfos.get(portName)));
        endpoint.getJaxwsBinding().setHandlerChain(hc);

        // create the client object, then initialize the endpoint features against it
        Client client = new ClientImpl(clientBus, endpoint, clientFac.getConduitSelector());
        for (Feature af : endpoint.getFeatures()) {
            af.initialize(client, clientBus);
            if (isFinestEnabled) {   // Liberty Change begin #26529
                LOG.finest("Initialized feature that is obtained from JaxWsEndpointImpl(endpoint): " + af);
            }  // Liberty Change end #26529
        }
        //CXF-2822
        initIntercepors(client, clientFac);
        if (executor != null) {
            client.getEndpoint().setExecutor(executor);
        }
        // if the client factory has an address specified, use that, if not
        // then try to get it from the wsdl
        if (!StringUtils.isEmpty(clientFac.getAddress())) {
            client.getEndpoint().getEndpointInfo().setAddress(clientFac.getAddress());
        } else {
            //Set the the EPR's address in EndpointInfo
            PortInfoImpl portInfo = portInfos.get(portName);
            if (portInfo != null && !StringUtils.isEmpty(portInfo.getAddress())) {
                client.getEndpoint().getEndpointInfo().setAddress(portInfo.getAddress());
            }
        }

        Dispatch<T> disp = new DispatchImpl<>(client, mode, context, type);
        configureObject(disp);
        return disp;
    }

    @Override
    public <T> Dispatch<T> createDispatch(EndpointReference endpointReference,
                                          Class<T> type,
                                          Mode mode,
                                          WebServiceFeature... features) {
        EndpointReferenceType ref = ProviderImpl.convertToInternal(endpointReference);
        QName portName = EndpointReferenceUtils.getPortQName(ref, bus);
        updatePortInfoAddress(portName, EndpointReferenceUtils.getAddress(ref));
        return createDispatch(portName,
                              type, mode, features);
    }

    @Override
    public Dispatch<Object> createDispatch(QName portName, JAXBContext context, Mode mode) {
        return createDispatch(portName, context, mode, new WebServiceFeature[]{});
    }

    @Override
    public Dispatch<Object> createDispatch(QName portName,
                                           JAXBContext context,
                                           Mode mode,
                                           WebServiceFeature... features) {
        return createDispatch(portName, Object.class, context, mode, features);
    }

    @Override
    public Dispatch<Object> createDispatch(EndpointReference endpointReference,
                                           JAXBContext context,
                                           Mode mode,
                                           WebServiceFeature... features) {
        EndpointReferenceType ref = ProviderImpl.convertToInternal(endpointReference);
        QName portName = EndpointReferenceUtils.getPortQName(ref, bus);
        updatePortInfoAddress(portName, EndpointReferenceUtils.getAddress(ref));
        return createDispatch(portName, context, mode, features);
    }

    @Override
    public <T> T getPort(EndpointReference endpointReference, Class<T> serviceEndpointInterface,
                         WebServiceFeature... features) {
        return getPort(ProviderImpl.convertToInternal(endpointReference), serviceEndpointInterface,
                       features);

    }

    private void initIntercepors(Client client, AbstractBasicInterceptorProvider clientFact) {
        client.getInInterceptors().addAll(clientFact.getInInterceptors());
        client.getOutInterceptors().addAll(clientFact.getOutInterceptors());
        client.getInFaultInterceptors().addAll(clientFact.getInFaultInterceptors());
        client.getOutFaultInterceptors().addAll(clientFact.getOutFaultInterceptors());
    }

    private void updatePortInfoAddress(QName portName, String eprAddress) {
        PortInfoImpl portInfo = portInfos.get(portName);
        if (!StringUtils.isEmpty(eprAddress) && portInfo != null) {
            portInfo.setAddress(eprAddress);
        }
    }
}


