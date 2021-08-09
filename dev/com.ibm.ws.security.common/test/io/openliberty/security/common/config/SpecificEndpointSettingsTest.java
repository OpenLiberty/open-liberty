/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.common.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.common.http.SupportedHttpMethodHandler.HttpMethod;
import test.common.SharedOutputManager;

public class SpecificEndpointSettingsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.common.*=all:com.ibm.ws.security.common.*=all");

    private final String endpointName = "someEndpoint";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_constructorAndGetters() {
        SpecificEndpointSettings settings = new SpecificEndpointSettings(endpointName);
        assertEquals("Endpoint type did not match expected value.", endpointName, settings.getEndpointName());
        // By default, there should be no supported HTTP methods
        Set<HttpMethod> supportedMethods = settings.getSupportedHttpMethods();
        assertNotNull("Set of supported methods should not have been null.", supportedMethods);
        assertTrue("Should not have recorded any supported HTTP methods, but did. Supported methods were: " + supportedMethods, supportedMethods.isEmpty());
    }

    @Test
    public void test_setSupportedHttpMethods_nullInput() {
        SpecificEndpointSettings settings = new SpecificEndpointSettings(endpointName);
        settings.setSupportedHttpMethods((String[]) null);

        Set<HttpMethod> supportedMethods = settings.getSupportedHttpMethods();
        assertNotNull("Set of supported methods should not have been null.", supportedMethods);
        assertTrue("Should not have recorded any supported HTTP methods, but did. Supported methods were: " + supportedMethods, supportedMethods.isEmpty());
    }

    @Test
    public void test_setSupportedHttpMethods_emptyInput() {
        SpecificEndpointSettings settings = new SpecificEndpointSettings(endpointName);
        settings.setSupportedHttpMethods(new String[0]);

        Set<HttpMethod> supportedMethods = settings.getSupportedHttpMethods();
        assertTrue("Should not have recorded any supported HTTP methods, but did. Supported methods were: " + supportedMethods, supportedMethods.isEmpty());
    }

    @Test
    public void test_setSupportedHttpMethods_simpleInput() {
        SpecificEndpointSettings settings = new SpecificEndpointSettings(endpointName);
        String supportedMethod = "Get";
        HttpMethod expectedMethod = HttpMethod.GET;
        settings.setSupportedHttpMethods(supportedMethod);

        Set<HttpMethod> supportedMethods = settings.getSupportedHttpMethods();
        assertEquals("Set of supported HTTP methods did not expected number of inputs. Supported methods were: " + supportedMethods, 1, supportedMethods.size());
        assertTrue("Supported HTTP methods for endpoint [" + endpointName + "] is missing expected value [" + expectedMethod + "]. Recorded methods were: " + supportedMethods, supportedMethods.contains(expectedMethod));
    }

    @Test
    public void test_setSupportedHttpMethods_multipleInputs() {
        SpecificEndpointSettings settings = new SpecificEndpointSettings(endpointName);
        String supportedMethod = "POST";
        HttpMethod expectedMethod = HttpMethod.POST;
        settings.setSupportedHttpMethods(supportedMethod);

        Set<HttpMethod> supportedMethods = settings.getSupportedHttpMethods();
        assertEquals("Set of supported HTTP methods did not expected number of inputs. Supported methods were: " + supportedMethods, 1, supportedMethods.size());
        assertTrue("Supported HTTP methods for endpoint [" + endpointName + "] is missing expected value [" + expectedMethod + "]. Recorded methods were: " + supportedMethods, supportedMethods.contains(expectedMethod));

        supportedMethod = "unknown";
        settings.setSupportedHttpMethods(supportedMethod);

        supportedMethods = settings.getSupportedHttpMethods();
        assertTrue("Should not have recorded any supported HTTP methods, but did. Supported methods were: " + supportedMethods, supportedMethods.isEmpty());

        String[] inputSupportedMethods = new String[] { "one", "get", "two", "post", "three" };
        settings.setSupportedHttpMethods(inputSupportedMethods);

        supportedMethods = settings.getSupportedHttpMethods();
        assertEquals("Set of supported HTTP methods did not expected number of inputs. Supported methods were: " + supportedMethods, 2, supportedMethods.size());
        assertTrue("Supported HTTP methods for endpoint [" + endpointName + "] is missing expected value [" + HttpMethod.GET + "]. Recorded methods were: " + supportedMethods, supportedMethods.contains(HttpMethod.GET));
        assertTrue("Supported HTTP methods for endpoint [" + endpointName + "] is missing expected value [" + HttpMethod.POST + "]. Recorded methods were: " + supportedMethods, supportedMethods.contains(HttpMethod.POST));
    }

}
