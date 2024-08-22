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
import org.apache.cxf.message.MessageUtils;
import org.junit.Test;
import org.apache.cxf.jaxb.JAXBDataBinding;
import com.ibm.ws.jaxws.internal.WebServiceConfigConstants;


/**
 *      Unit Tests that check the correct behavior of the LibertyWebServiceClientInInterceptor when
 *      the webServiceClient configuration is set.
 *      
 *      < webServiceClientConfig tests when serviceName IS set >
 *      
 */
public class LibertyWebServiceClientInInterceptorServiceTest extends LibertyWebServiceClientInInterceptorTestBase {

    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and enableSchemaValidation only is set to false. 
     */
    @Test
    public void testHandleMessageWithServiceName_EnableSchemaValidationSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and enableSchemaValidation only is set to true. 
     */
    @Test
    public void testHandleMessageWithServiceName_EnableSchemaValidationSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);


        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
    }
    
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and ignoreUnexpectedElements only is set to false. 
     */
    @Test
    public void testHandleMessageWithServiceName_IgnoreUnexpectedElementsSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    

    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and ignoreUnexpectedElements only is set to true. 
     */
    @Test
    public void testHandleMessageWithServiceName_IgnoreUnexpectedElementsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }

    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and enableDefaultValidation only is set to false. 
     */
    @Test
    public void testHandleMessageWithServiceName_EnableDefaultValidationSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();

        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
    }
    
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and enableDefaultValidation only is set to true. 
     */
    @Test       
    public void testHandleMessageWithServiceName_EnableDefaultValidationSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();

        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
    }
    
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and both config properties are set to false. 
     */
    @Test
    public void testHandleMessageWithServiceName_EnableSchemaValidationAndIgnoreUnexpectedElementsSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and both config properties are set to false. 
     */
    @Test
    public void testHandleMessageWithServiceName_EnableSchemaValidationAndEnableDefaultValidationSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        // Both configs are independent, one set JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER the other SCHEMA_VALIDATION
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER));
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and both config properties are set to false. 
     */
    @Test
    public void testHandleMessageWithServiceName_EnableDefaultValidationAndIgnoreUnexpectedElementsSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to false
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
    } 
    
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and both config properties are set to true. 
     */
    @Test
    public void testHandleMessageWithServiceName_EnableSchemaValidationAndIgnoreUnexpectedElementsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
   
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and both config properties are set to true. 
     */
    @Test
    public void testHandleMessageWithServiceName_EnableDefaultValidationAndIgnoreUnexpectedElementsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and both config properties are set to true. 
     */
    @Test
    public void testHandleMessageWithServiceName_EnableSchemaValidationAndEnableDefaultValidationSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
   
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and both config properties are set to false. 
     */
    @Test
    public void testHandleMessageWithServiceName_EnableDefaultValidationSetToTrueAndIgnoreUnexpectedElementsSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP boolean has to be flipped, we assertTrue when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to false.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and both config properties are set to true. 
     */
    @Test
    public void testHandleMessageWithServiceName_EnableDefaultValidationSetToFalseAndIgnoreUnexpectedElementsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP boolean has to be flipped, we assertTrue when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and all config properties are set to false. 
     */
    @Test
    public void testHandleMessageWithServiceName_AllSetToFalse() { 

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
 
    /*
     * Tests that we get the expected values from the webServiceClient config when service name is used
     * and all config properties are set to true. 
     */
    @Test
    public void testHandleMessageWithServiceName_AllSetToTrue() { 

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // set the serviceName properties
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, SERVICE_NAME);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    
    
}
