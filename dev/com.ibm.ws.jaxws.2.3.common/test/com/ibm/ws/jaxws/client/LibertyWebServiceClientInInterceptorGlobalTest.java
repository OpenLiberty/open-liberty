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
 *      Unit Tests that check the correct behavior of the LibertyWebServiceClientInInterceptor when
 *      the webServiceClient configuration is set.
 *      
 *      < webServiceClientConfig tests when serviceName IS set and global is ALSO set > 
 *      
 */
public class LibertyWebServiceClientInInterceptorGlobalTest extends LibertyWebServiceClientInInterceptorTestBase {

    
    // Config impls that need to be instantiated before the intercepter can be run.
    private static WebServiceClientConfigImpl config1;
    
    // serviceName property map
    Map<String, Object> webServiceClientServiceNameProps = new HashMap<String, Object>();

    
    /*
     * This test verifies that with both global and a serviceName webServiceClient configurations that have both enableSchemaValidation
     * and ignoreUnexpectedElements set that when the serviceName config has both set to true, that 
     * the message will still return the values for the serviceName configuration and not the client configuration
     */
    @Test
    public void testHandleMessageWithServiceNameAndGlobal_SetToFalseGlobalSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        webServiceClientServiceNameProps.clear();
        

        // Set the global default values
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true); //Liberty change
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false); //Liberty change
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER));  
    }
    
    /*
     * This test verifies that with both global and a serviceName webServiceClient configurations that have both enableSchemaValidation
     * and ignoreUnexpectedElements set that when the serviceName config has both set to false, that 
     * the message will still return the values for the serviceName configuration and not the client configuration
     */
    @Test
    public void testHandleMessageWithServiceNameAndGlobal_SetToTrueGlobalSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        webServiceClientServiceNameProps.clear();
        
        // Set the global default values
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false); //Liberty change
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true); //Liberty change
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    
    
    // Liberty change begin
    /*
     * This test verifies that with both global and a serviceName webServiceClient configurations 
     * where the config with serviceName set to true has enableSchemaValidation set to true and the 
     * global webServiceClient configuration has ignoreUnexpectedElements set to false
     * the message will return the correct values for both settings.
     */
    @Test
    public void testHandleMessageWithServiceNameAndGlobal_EnableDefaultValidationSetToFalseGlobalIgnoreUnexpectedElementsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        webServiceClientServiceNameProps.clear();
        

        // Set the global default values
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false);
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    
    
    /*
     * This test verifies that with both global and a serviceName webServiceClient configurations 
     * where the config with serviceName has ignoreUnexpectedElements set to true and the 
     * global webServiceClient configuration has enableSchemaValidation set to false
     * the message will return the correct values for both settings.
     */
    @Test
    public void testHandleMessageWithServiceNameAndGlobal_IgnoreUnexpectedElementsSetToFalseGlobalEnableDefaultValidationSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        webServiceClientServiceNameProps.clear();
        
        // Set the global default values
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
        
    }
    // Liberty change end

    /*
     * This test verifies that with both global and a serviceName webServiceClient configurations 
     * where the config with serviceName set to true has enableSchemaValidation set to false and the 
     * global webServiceClient configuration has ignoreUnexpectedElements set to true
     * the message will return the correct values for both settings.
     */
    @Test
    public void testHandleMessageWithServiceNameAndGlobal_EnableSchemaValidationSetToFalseGlobalIgnoreUnexpectedElementsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        webServiceClientServiceNameProps.clear();
        

        // Set the global default values
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    
    
    /*
     * This test verifies that with both global and a serviceName webServiceClient configurations 
     * where the config with serviceName has ignoreUnexpectedElements set to false and the 
     * global webServiceClient configuration has enableSchemaValidation set to true
     * the message will return the correct values for both settings.
     */
    @Test
    public void testHandleMessageWithServiceNameAndGlobal_IgnoreUnexpectedElementsSetToFalseGlobalEnableSchemaValidationSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        webServiceClientServiceNameProps.clear();
        
        // Set the global default values
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        // set the serviceName properties
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientServiceNameProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
        config1  = new WebServiceClientConfigImpl(webServiceClientServiceNameProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
        
    }
        
}
