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
 *      Unit Tests that check the correct behavior of the LibertyWebServiceClientInInterceptor when
 *      the webServiceClient configuration is set.
 */
public class LibertyWebServiceClientInInterceptorTest {
    
    // Property that LibertyWebServiceClientInInterceptor uses to set schema validation on the message
    final String SCHEMA_VALIDATION = "schema-validation-enabled";
    
    // serviceName set in "config"
    final String SERVICE_NAME = "TestService";
    
    // Uncomment this Logger if debugging is needed
    // Logger LOG = Logger.getLogger("LibertyWebServiceClientInInterceptorTest");
    
    static {
        // Since the webServiceClient configuration is in beta, need to set the system property to pass tests
        System.getProperties().setProperty("com.ibm.ws.beta.edition", "true");
    }
    
    // the Message instance shared across tests
    private static Message message;
    
    // Config impls that need to be instantiated before the intercepter can be run.
    private static WebServiceClientConfigImpl config;
    private static WebServiceClientConfigImpl config1;
    
    // Default property map
    Map<String, Object> webServiceClientDefaultProps = new HashMap<String, Object>();
    
    // serviceName property map
    Map<String, Object> webServiceClientServiceNameProps = new HashMap<String, Object>();
    
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
    
    
    /********************* webServiceClientConfig tests when serviceName not set *********************/
    
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and enableSchemaValidation only is set to false. 
     */
    @Test
    public void testHandleMessageWithDefaultEnableSchemaValidationSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientDefaultProps.clear();

        // Set only global enableSchemaValidation
        webServiceClientDefaultProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientDefaultProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and ignoreUnexpectedElemnts only is set to false. 
     */
    @Test
    public void testHandleMessageWithDefaultIgnoreUnexpectedElemntsSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientDefaultProps.clear();

        // Set only global ignoreUnexpectedElements
        webServiceClientDefaultProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientDefaultProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and enableSchemaValidation only is set to true. 
     */
    @Test
    public void testHandleMessageWithDefaultEnableSchemaValidationSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientDefaultProps.clear();

        // Set only global enableSchemaValidation
        webServiceClientDefaultProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientDefaultProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and ignoreUnexpectedElemnts only is set to true. 
     */
    @Test
    public void testHandleMessageWithDefaultIgnoreUnexpectedElemntsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientDefaultProps.clear();
        
        // Set only global ignoreUnexpectedElements
        webServiceClientDefaultProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientDefaultProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        
    }

    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to false. 
     */
    @Test
    public void testHandleMessageWithDefaultBothSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientDefaultProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientDefaultProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientDefaultProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to false. 
     */
    @Test
    public void testHandleMessageWithDefaultSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientDefaultProps.clear();
        
        // this properties map should map to all serviceNames, setting both to true
        webServiceClientDefaultProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientDefaultProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        // NOTE:
        //  Because the  JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
    }

    
    
    
    /********************* webServiceClientConfig tests when serviceName IS set *********************/
    

    
   
    /*
     * Tests that we get the expected values from the webServiceClient config when "TestService" is used
     * and enableSchemaValidation only is set to false. 
     */
    @Test
    public void testHandleMessageWithServiceNameEnableSchemaValidationSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientServiceNameProps.clear();
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and enableSchemaValidation only is set to true. 
     */
    @Test
    public void testHandleMessageWithServiceNameEnableSchemaValidationSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientServiceNameProps.clear();
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
    }
    
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and ignoreUnexpectedElemnts only is set to false. 
     */
    @Test
    public void testHandleMessageWithServiceNameIgnoreUnexpectedElemntsSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientServiceNameProps.clear();
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        
    }
    

    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and ignoreUnexpectedElemnts only is set to true. 
     */
    @Test
    public void testHandleMessageWithServiceNameIgnoreUnexpectedElemntsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientServiceNameProps.clear();
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        
    }

    
    @Test
    public void testHandleMessageWithServiceNameBothSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientServiceNameProps.clear();
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        
    }
    
    @Test
    public void testHandleMessageWithServiceNameBothSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientServiceNameProps.clear();
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        // NOTE:
        //  Because the  JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        
    }
    

    
    
    
    /********************* webServiceClientConfig tests when serviceName IS set and global is ALSO set *********************/
    

    
    /*
     * This test verifies that with both global and a serviceName webServiceClient configurations that have both enableSchemaValidation
     * and ignoreUnexpectedElements set that when the serviceName config has both set to true, that 
     * the message will still return the values for the serviceName configuration and not the client configuration
     */
    @Test
    public void testHandleMessageWithServiceNameSetToFalseGlobalSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientDefaultProps.clear();
        webServiceClientServiceNameProps.clear();
        

        // Set the global default values
        webServiceClientDefaultProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        
        config  = new WebServiceClientConfigImpl(webServiceClientDefaultProps);
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        
    }
    
    /*
     * This test verifies that with both global and a serviceName webServiceClient configurations that have both enableSchemaValidation
     * and ignoreUnexpectedElements set that when the serviceName config has both set to false, that 
     * the message will still return the values for the serviceName configuration and not the client configuration
     */
    @Test
    public void testHandleMessageWithServiceNameSetToTrueGlobalSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientDefaultProps.clear();
        webServiceClientServiceNameProps.clear();
        
        // Set the global default values
        webServiceClientDefaultProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientDefaultProps);
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        // NOTE:
        //  Because the  JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        
    }
    
    /*
     * This test verifies that with both global and a serviceName webServiceClient configurations 
     * where the config with serviceName set to true has enableSchemaValidation set to true and the 
     * global webServiceClient configuration has ignoreUnexpectedElements set to false
     * the message will return the correct values for both settings.
     */
    @Test
    public void testHandleMessageWithServiceNameEnableSchemaValidationSetToFalseGlobalIgnoreUnexpectedElemntsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientDefaultProps.clear();
        webServiceClientServiceNameProps.clear();
        

        // Set the global default values
        webServiceClientDefaultProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        
        config  = new WebServiceClientConfigImpl(webServiceClientDefaultProps);
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        
    }
    
    
    /*
     * This test verifies that with both global and a serviceName webServiceClient configurations 
     * where the config with serviceName has ignoreUnexpectedElements set to true and the 
     * global webServiceClient configuration has enableSchemaValidation set to false
     * the message will return the correct values for both settings.
     */
    @Test
    public void testHandleMessageWithServiceNameIgnoreUnexpectedElemntsSetToFalseGlobalEnableSchemaValidationSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientDefaultProps.clear();
        webServiceClientServiceNameProps.clear();
        
        // Set the global default values
        webServiceClientDefaultProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientDefaultProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientDefaultProps);
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        // NOTE:
        //  Because the  JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        
    }
}
