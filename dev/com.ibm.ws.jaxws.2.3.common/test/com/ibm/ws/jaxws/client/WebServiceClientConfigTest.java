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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.jaxws.internal.WebServiceConfigConstants;

import test.common.SharedOutputManager;

/**
 * Basic unit tests for the webServiceClient configuration
 */
public class WebServiceClientConfigTest {
    
    static {
        // Since the webServiceClient configuration is in beta, need to set the system property to pass tests
        System.getProperties().setProperty("com.ibm.ws.beta.edition", "true");
    }

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule rule = outputMgr;

    @Rule
    public TestName name = new TestName();

    @After
    public void tearDown() {
    }

    /**
     * verify a basic default webServiceClient configuration
     */
    @Test
    public void testSimpleClientConfig() {
        Map<String, Object> basicProps = new HashMap<String, Object>();

        // this properties map should map to all serviceNames 
        basicProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, "default");
        basicProps.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        basicProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        WebServiceClientConfigImpl config = new WebServiceClientConfigImpl(basicProps);
        
        List<String> serviceNamesToTest = new ArrayList<String>();
        serviceNamesToTest.add("SimpleService");
        serviceNamesToTest.add("SimpleService2");
        serviceNamesToTest.add("SimpleService3");

        for (String serviceName : serviceNamesToTest) {
            Assert.assertTrue((boolean) WebServicesClientConfigHolder.getIgnoreUnexpectedElements(serviceName));
            Assert.assertTrue((boolean) WebServicesClientConfigHolder.getEnableSchemaValidation(serviceName));

        }
        config.deactivate();
    }

    /**
     * verify updating a single webServiceClient configuration
     */
    @Test
    public void testConfigUpdate() {

        Map<String, Object> basicProps1 = new HashMap<String, Object>();
        Map<String, Object> basicProps2 = new HashMap<String, Object>();
        Map<String, Object> basicProps3 = new HashMap<String, Object>();
        String serviceName = "default";
        String serviceName1 = "SimpleService";
        String serviceName2 = "SimpleService1";

        basicProps2.put(WebServiceConfigConstants.SERVICE_NAME_PROP, serviceName);
        basicProps2.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        WebServiceClientConfigImpl config = new WebServiceClientConfigImpl(basicProps2);

        Assert.assertTrue((boolean) WebServicesClientConfigHolder.getEnableSchemaValidation(serviceName));

        basicProps1.put(WebServiceConfigConstants.SERVICE_NAME_PROP, serviceName1);
        basicProps1.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        config.modified(basicProps1);


        Assert.assertTrue((boolean) WebServicesClientConfigHolder.getIgnoreUnexpectedElements(serviceName1));

        basicProps3.put(WebServiceConfigConstants.SERVICE_NAME_PROP, serviceName2);
        basicProps3.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        basicProps3.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        config.modified(basicProps3);

        Assert.assertFalse((boolean) WebServicesClientConfigHolder.getEnableSchemaValidation(serviceName2));
        Assert.assertFalse((boolean) WebServicesClientConfigHolder.getIgnoreUnexpectedElements(serviceName2));

        config.deactivate();

        Assert.assertNull(WebServicesClientConfigHolder.getNameProps(serviceName2));
    }

    /**
     * verify with multiple webServiceClient configuration
     */
    @Test
    public void testMultipleElements() {


        Map<String, Object> basicProps = new HashMap<String, Object>();
        Map<String, Object> basicProps1 = new HashMap<String, Object>();
        Map<String, Object> basicProps2 = new HashMap<String, Object>();

        String serviceName = "default";
        String serviceName1 = "SimpleService";
        String serviceName2 = "SimpleService1";

        basicProps.put(WebServiceConfigConstants.SERVICE_NAME_PROP, serviceName);
        basicProps.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        WebServiceClientConfigImpl config = new WebServiceClientConfigImpl(basicProps);
        
        Assert.assertTrue((boolean) WebServicesClientConfigHolder.getIgnoreUnexpectedElements(serviceName));

        basicProps1.put(WebServiceConfigConstants.SERVICE_NAME_PROP, serviceName1);
        basicProps1.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, true);
        basicProps1.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, true);
        
        WebServiceClientConfigImpl config1 = new WebServiceClientConfigImpl(basicProps1);
        basicProps2.put(WebServiceConfigConstants.SERVICE_NAME_PROP, serviceName2);
        
        basicProps2.put(WebServiceConfigConstants.ENABLE_SCHEMA_VALIDATION_PROP, false);
        basicProps2.put(WebServiceConfigConstants.IGNORE_UNEXPECTED_ELEMENTS_PROP, false);
        
        WebServiceClientConfigImpl config2 = new WebServiceClientConfigImpl(basicProps2);

        Assert.assertTrue((boolean) WebServicesClientConfigHolder.getIgnoreUnexpectedElements(serviceName));
        Assert.assertTrue((boolean) WebServicesClientConfigHolder.getIgnoreUnexpectedElements(serviceName1));
        Assert.assertFalse((boolean) WebServicesClientConfigHolder.getEnableSchemaValidation(serviceName2));

        config.deactivate();
        config1.deactivate();
        config2.deactivate();
    }

}
