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
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.MessageUtils;
import org.junit.Test;
import com.ibm.ws.jaxws.internal.WebServiceConfigConstants;

/**
 *      Unit Tests that check the correct behavior of the LibertyWebServiceClientInInterceptor when
 *      the webServiceClient configuration is set.
 *      
 *      < webServiceClientConfig tests when serviceName not set >
 *      
 */
public class LibertyWebServiceClientInInterceptorDefaultTest extends LibertyWebServiceClientInInterceptorTestBase {
        
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and enableSchemaValidation only is set to false. 
     */
    @Test
    public void testHandleMessageWithDefault_EnableSchemaValidationSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();

        // Set only global enableSchemaValidation
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
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
    public void testHandleMessageWithDefault_IgnoreUnexpectedElementsSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();

        // Set only global ignoreUnexpectedElements
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    
    // Liberty change begin
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and enableDefaultValidation only is set to false. 
     */
    @Test
    public void testHandleMessageWithDefault_EnableDefaultValidationSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();

        // Set only global enableDefaultValidation
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
    }// Liberty change end
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and enableSchemaValidation only is set to true. 
     */
    @Test
    public void testHandleMessageWithDefault_EnableSchemaValidationSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();

        // Set only global enableSchemaValidation
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has correct value
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and ignoreUnexpectedElements only is set to true. 
     */
    @Test
    public void testHandleMessageWithDefault_IgnoreUnexpectedElementsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // Set only global ignoreUnexpectedElements
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    
    // Liberty change begin
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and enableDefaultValidation only is set to true. 
     */
    @Test       
    public void testHandleMessageWithDefault_EnableDefaultValidationSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();

        // Set only global enableDefaultValidation
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER));
    } // Liberty change end

    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to false. 
     */
    @Test
    public void testHandleMessageWithDefault_EnableSchemaValidationAndIgnoreUnexpectedElementsSetToFalse() { // Liberty change

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
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
    
    // Liberty change begin
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to false. 
     */
    @Test
    public void testHandleMessageWithDefault_EnableSchemaValidationAndEnableDefaultValidationSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
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
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to false. 
     */
    @Test
    public void testHandleMessageWithDefault_EnableDefaultValidationAndIgnoreUnexpectedElementsSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
    }
    

    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to true. 
     */
    @Test
    public void testHandleMessageWithDefault_EnableSchemaValidationAndIgnoreUnexpectedElementsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to true. 
     */
    @Test
    public void testHandleMessageWithDefault_EnableDefaultValidationAndIgnoreUnexpectedElementsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to true
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
    }
    
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to true. 
     */
    @Test
    public void testHandleMessageWithDefault_EnableSchemaValidationAndEnableDefaultValidationSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        // Both configs are independent, one set JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER the other SCHEMA_VALIDATION
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
    }
    
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to true and false
     */
    @Test
    public void testHandleMessageWithDefault_EnableDefaultValidationSetToTrueAndIgnoreUnexpectedElementsSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, one false one true
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to false and true 
     */
    @Test
    public void testHandleMessageWithDefault_EnableDefaultValidationSetToFalseAndIgnoreUnexpectedElementsSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }
    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to false. 
     */
    @Test
    public void testHandleMessageWithDefault_AllSetToFalse() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to false
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, false);
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values.
        // NOTE:
        //  Because the  WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP boolean has to be flipped, we assertFalse when setting WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP to true.
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to false", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertFalse("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to false", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER)); 
    }

    
    /*
     * Tests that we get the expected values from the webServiceClient config when default is used
     * and both config properties are set to true. 
     */
    @Test
    public void testHandleMessageWithDefault_AllSetToTrue() {

        // Clear props first to ensure correct values are set. 
        webServiceClientProps.clear();
        
        // this properties map should map to all serviceNames, setting both to true
        webServiceClientProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, WebServiceConfigConstants.DEFAULT_PROP);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        webServiceClientProps.put(WebServiceConfigConstants.ENABLE_DEFAULT_VALIDATION_PROP, true); // Liberty change
        
        config  = new WebServiceClientConfigImpl(webServiceClientProps);
       
        // invoke LibertyWebServiceClientInInterceptor.handleMessage(message)
        interceptor.handleMessage(message);

        // Assert message has the set values
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER + " property to true", MessageUtils.getContextualBoolean(message, JAXBDataBinding.SET_VALIDATION_EVENT_HANDLER));
        assertTrue("The LibertyWebServiceClientInInterceptor should have set to the " + SCHEMA_VALIDATION + " property to true", MessageUtils.getContextualBoolean(message, SCHEMA_VALIDATION));
        assertNotNull(message.get(JAXBDataBinding.READER_VALIDATION_EVENT_HANDLER));
    }
    // Liberty change end
    
}
