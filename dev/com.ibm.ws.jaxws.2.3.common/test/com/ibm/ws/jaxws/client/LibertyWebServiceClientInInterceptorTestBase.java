/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;



import org.apache.cxf.Bus;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapBindingFactory;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.bus.extension.ExtensionManagerBus;
import org.apache.cxf.bus.managers.BindingFactoryManagerImpl;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.jaxb.JAXBDataBinding;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.jaxws.internal.WebServiceConfigConstants;

import test.common.SharedOutputManager;

/**
 *      Unit Tests that check the correct behavior of the LibertyWebServiceClientInInterceptor
 *      when the webServiceClient configuration is set.This is the super class providing setup and
 *      main fields to other tests
 */
public class LibertyWebServiceClientInInterceptorTestBase {
    
    // Property that LibertyWebServiceClientInInterceptor uses to set schema validation on the message
    final String SCHEMA_VALIDATION = "schema-validation-enabled";
    
    // serviceName set in "config"
    final String SERVICE_NAME = "TestService";
    
    // Uncomment this Logger if debugging is needed
    // Logger LOG = Logger.getLogger("LibertyWebServiceClientInInterceptorTestBase");
    
    static {
        // Since the webServiceClient configuration is in beta, need to set the system property to pass tests
        System.getProperties().setProperty("com.ibm.ws.beta.edition", "true");
    }
    
    // the Message instance shared across tests
    protected static Message message;
    
    // Config impls that need to be instantiated before the intercepter can be run.
    protected static WebServiceClientConfigImpl config;
    
    // Default property map
    protected Map<String, Object> webServiceClientProps = new HashMap<String, Object>();
    
    // Map of Maps
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    
    // Intercepter instance used for testing
    LibertyWebServiceClientInInterceptor interceptor = new LibertyWebServiceClientInInterceptor();
    
    // Since the Intercepter checks for service name, we need to set it on the exchange.
    QName testServiceQName= new QName("http://unittest.openliberty.io", SERVICE_NAME);
    
    @Rule
    public TestRule rule = outputMgr;

    @Rule
    public TestName name = new TestName();

    @Before 
    public void setup() throws EndpointException {
        
        // Instantiate all the necessary CXF objects needed to invoke a our intercepter's handleMessage(..) method
        // Starting with the SoapBindingFactory
        SoapBindingFactory soapBindingFactory = new SoapBindingFactory();
        
        // Build the CXF service with the ServiceInfo model based on our QName
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setName(testServiceQName);
        Service service = new ServiceImpl(serviceInfo);
        
        // Add a CXF bus
        Bus  bus = new ExtensionManagerBus();
        
        // Build the BindingFactoryManager and associate it with our bus
        BindingFactoryManager bindingFactory = new BindingFactoryManagerImpl(bus);
        bindingFactory.registerBindingFactory(SERVICE_NAME, soapBindingFactory);
        
        // Build the CXF EndpointInfo Model associated with the message.
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://unittest.openliberty.io");
        ei.setName(testServiceQName);
        

        // Build the CXF InterfaceInfo Model associated with the message.
        InterfaceInfo ii = serviceInfo.createInterface(new QName("http://unittest.openliberty.io", "TestServiceInterface"));
        serviceInfo.setInterface(ii);
        ii.addOperation(new QName("http://unittest.openliberty.io", "testOperation"));
        

        // Build the CXF BindingInfo Model associated with the message.
        SoapBindingInfo bindingInfo = new SoapBindingInfo(serviceInfo, "http://schemas.xmlsoap.org/soap/", Soap11.getInstance());
        bindingInfo.setTransportURI("http://schemas.xmlsoap.org/soap/");
        BindingOperationInfo boi =  bindingInfo.buildOperation(new QName("http://unittest.openliberty.io", "testOperation"), null, null);
        ei.setBinding(bindingInfo);
        
        // Create a new endpoint based on our Bus, Service, Info Model
        Endpoint testEndpoint = new EndpointImpl(bus, service, ei);
        
        // Create a new Exchange and associate everything with it
        ExchangeImpl exchange = new ExchangeImpl();
        
        exchange.put(Endpoint.class, testEndpoint);
        exchange.put(Service.class, testEndpoint.getService());
        exchange.put(BindingInfo.class, bindingInfo);
        exchange.put(BindingOperationInfo.class, boi);
        exchange.put(Bus.class, bus);
        exchange.put(ServiceInfo.class, serviceInfo);
        exchange.put(InterfaceInfo.class, ii);
        
        // Create a basic message to pass to handleMessage(Message message) and set our exchange
        StringWriter sw = new StringWriter();
        sw.append("<today/>");
        message = new MessageImpl();
        message.put(BindingInfo.class, bindingInfo);
        message.put(BindingOperationInfo.class, boi);
        message.setExchange(exchange);
        message.put(Message.CONTENT_TYPE, "application/xml");
        message.setContent(Writer.class, sw);

    }
    
    @After
    public void tearDown() {
        
    }
    
}
