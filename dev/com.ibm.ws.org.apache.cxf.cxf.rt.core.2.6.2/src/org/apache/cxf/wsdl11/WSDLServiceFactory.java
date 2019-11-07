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

package org.apache.cxf.wsdl11;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.factory.AbstractServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.ws.commons.schema.XmlSchemaException;

public class WSDLServiceFactory extends AbstractServiceFactoryBean {

    private static final Logger LOG = LogUtils.getL7dLogger(WSDLServiceFactory.class);

    private URL wsdlUrl;
    private QName serviceName;
    private QName endpointName;
    private Definition definition;
    private boolean allowRefs;

    public WSDLServiceFactory(Bus b, Definition d) {
        setBus(b);
        definition = d;
    }

    public WSDLServiceFactory(Bus b, Definition d, QName sn) {
        this(b, d);
        serviceName = sn;
    }

    public WSDLServiceFactory(Bus b, URL url) {
        setBus(b);
        wsdlUrl = url;

        try {
            // use wsdl manager to parse wsdl or get cached definition
            definition = getBus().getExtension(WSDLManager.class).getDefinition(wsdlUrl);
        } catch (WSDLException ex) {
            throw new ServiceConstructionException(new Message("SERVICE_CREATION_MSG", LOG), ex);
        }

    }
    public WSDLServiceFactory(Bus b, String url) {
        setBus(b);
        try {
            // use wsdl manager to parse wsdl or get cached definition
            definition = getBus().getExtension(WSDLManager.class).getDefinition(url);
        } catch (WSDLException ex) {
            throw new ServiceConstructionException(new Message("SERVICE_CREATION_MSG", LOG), ex);
        }
    }

    public WSDLServiceFactory(Bus b, URL url, QName sn) {
        this(b, url);
        serviceName = sn;
    }
    public WSDLServiceFactory(Bus b, String url, QName sn) {
        setBus(b);
        try {
            // use wsdl manager to parse wsdl or get cached definition
            definition = getBus().getExtension(WSDLManager.class).getDefinition(url);
        } catch (WSDLException ex) {
            throw new ServiceConstructionException(new Message("SERVICE_CREATION_MSG", LOG), ex);
        }

        serviceName = sn;
    }
    
    public void setAllowElementRefs(boolean b) {
        allowRefs = b;
    }

    public void setEndpointName(QName qn) {
        endpointName = qn;
    }
    
    public Definition getDefinition() {
        return definition;
    }
    
    public Service create() {

        List<ServiceInfo> services;
        if (serviceName == null) {
            try {
                WSDLServiceBuilder builder = new WSDLServiceBuilder(getBus());
                builder.setAllowElementRefs(allowRefs);
                services = builder.buildServices(definition);
            } catch (XmlSchemaException ex) {
                throw new ServiceConstructionException(new Message("SERVICE_CREATION_MSG", LOG), ex);
            }
            if (services.size() == 0) {
                throw new ServiceConstructionException(new Message("NO_SERVICE_EXC", LOG));
            } else {
                //@@TODO  - this isn't good, need to return all the services
                serviceName = services.get(0).getName();
                //get all the service info's that match that first one.
                Iterator<ServiceInfo> it = services.iterator();
                while (it.hasNext()) {
                    if (!it.next().getName().equals(serviceName)) {
                        it.remove();
                    }
                }
            }
        } else {
            javax.wsdl.Service wsdlService = definition.getService(serviceName);
            if (wsdlService == null) {
                if ((!PartialWSDLProcessor.isServiceExisted(definition, serviceName))
                    && (!PartialWSDLProcessor.isBindingExisted(definition, serviceName))
                    && (PartialWSDLProcessor.isPortTypeExisted(definition, serviceName))) {
                    try {
                        Map<QName, PortType> portTypes = CastUtils.cast(definition.getPortTypes());
                        String existPortName = null;
                        PortType portType = null;
                        for (QName existPortQName : portTypes.keySet()) {
                            existPortName = existPortQName.getLocalPart();
                            if (serviceName.getLocalPart().contains(existPortName)) {
                                portType = portTypes.get(existPortQName);
                                break;
                            }
                        }
                        WSDLFactory factory = WSDLFactory.newInstance();
                        ExtensionRegistry extReg = factory.newPopulatedExtensionRegistry();
                        Binding binding = PartialWSDLProcessor.doAppendBinding(definition, 
                                                                               existPortName, portType, extReg);
                        definition.addBinding(binding);
                        wsdlService = PartialWSDLProcessor.doAppendService(definition, 
                                                                           existPortName, extReg, binding);
                        definition.addService(wsdlService);
                    } catch (Exception e) {
                        throw new ServiceConstructionException(new Message("NO_SUCH_SERVICE_EXC", LOG, serviceName));
                    }
                } else {
                    throw new ServiceConstructionException(new Message("NO_SUCH_SERVICE_EXC", LOG, serviceName));
                }
            }
            try {
                services = new WSDLServiceBuilder(getBus()).buildServices(definition, 
                                                                          wsdlService,
                                                                          endpointName);
                if (services.size() == 0) {
                    throw new ServiceConstructionException(
                        new Message("NO_SUCH_ENDPOINT_EXC", LOG, endpointName));
                }
            } catch (XmlSchemaException ex) {
                throw new ServiceConstructionException(new Message("SERVICE_CREATION_MSG", LOG), ex);
            }
        }
        ServiceImpl service = new ServiceImpl(services);
        setService(service);
        return service;
    }

}
