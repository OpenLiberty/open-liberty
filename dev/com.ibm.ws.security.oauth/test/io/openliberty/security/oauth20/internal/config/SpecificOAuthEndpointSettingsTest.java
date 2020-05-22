/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oauth20.internal.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;
import com.ibm.ws.security.test.common.CommonTestClass;

import io.openliberty.security.common.http.SupportedHttpMethodHandler.HttpMethod;
import test.common.SharedOutputManager;

@SuppressWarnings("restriction")
public class SpecificOAuthEndpointSettingsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.oauth.*=all:com.ibm.ws.security.oauth.*=all");

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
        EndpointType endpointType = EndpointType.authorize;
        SpecificOAuthEndpointSettings settings = new SpecificOAuthEndpointSettings(endpointType);
        assertEquals("Endpoint type did not match expected value.", endpointType, settings.getEndpointType());
        // By default, there should be no supported HTTP methods
        Set<HttpMethod> supportedMethods = settings.getSupportedHttpMethods();
        assertNotNull("Set of supported methods should not have been null.", supportedMethods);
        assertTrue("Should not have recorded any supported HTTP methods, but did. Supported methods were: " + supportedMethods, supportedMethods.isEmpty());
    }

    @Test
    public void test_setSupportedHttpMethods_nullInput() {
        SpecificOAuthEndpointSettings settings = new SpecificOAuthEndpointSettings(EndpointType.authorize);
        settings.setSupportedHttpMethods((String[]) null);

        Set<HttpMethod> supportedMethods = settings.getSupportedHttpMethods();
        assertNotNull("Set of supported methods should not have been null.", supportedMethods);
        assertTrue("Should not have recorded any supported HTTP methods, but did. Supported methods were: " + supportedMethods, supportedMethods.isEmpty());
    }

    @Test
    public void test_setSupportedHttpMethods_emptyInput() {
        SpecificOAuthEndpointSettings settings = new SpecificOAuthEndpointSettings(EndpointType.clientMetatype);
        settings.setSupportedHttpMethods(new String[0]);

        Set<HttpMethod> supportedMethods = settings.getSupportedHttpMethods();
        assertTrue("Should not have recorded any supported HTTP methods, but did. Supported methods were: " + supportedMethods, supportedMethods.isEmpty());
    }

    @Test
    public void test_setSupportedHttpMethods_simpleInput() {
        SpecificOAuthEndpointSettings settings = new SpecificOAuthEndpointSettings(EndpointType.introspect);
        String supportedMethod = "Get";
        HttpMethod expectedMethod = HttpMethod.GET;
        settings.setSupportedHttpMethods(supportedMethod);

        Set<HttpMethod> supportedMethods = settings.getSupportedHttpMethods();
        assertEquals("Set of supported HTTP methods did not expected number of inputs. Supported methods were: " + supportedMethods, 1, supportedMethods.size());
        assertTrue("Supported HTTP methods for endpoint [" + settings.getEndpointType() + "] is missing expected value [" + expectedMethod + "]. Recorded methods were: " + supportedMethods, supportedMethods.contains(expectedMethod));
    }

    @Test
    public void test_setSupportedHttpMethods_multipleInputs() {
        SpecificOAuthEndpointSettings settings = new SpecificOAuthEndpointSettings(EndpointType.registration);
        String supportedMethod = "POST";
        HttpMethod expectedMethod = HttpMethod.POST;
        settings.setSupportedHttpMethods(supportedMethod);

        Set<HttpMethod> supportedMethods = settings.getSupportedHttpMethods();
        assertEquals("Set of supported HTTP methods did not expected number of inputs. Supported methods were: " + supportedMethods, 1, supportedMethods.size());
        assertTrue("Supported HTTP methods for endpoint [" + settings.getEndpointType() + "] is missing expected value [" + expectedMethod + "]. Recorded methods were: " + supportedMethods, supportedMethods.contains(expectedMethod));

        supportedMethod = "unknown";
        settings.setSupportedHttpMethods(supportedMethod);

        supportedMethods = settings.getSupportedHttpMethods();
        assertTrue("Should not have recorded any supported HTTP methods, but did. Supported methods were: " + supportedMethods, supportedMethods.isEmpty());

        String[] inputSupportedMethods = new String[] { "one", "two", "three" };
        settings.setSupportedHttpMethods(inputSupportedMethods);

        supportedMethods = settings.getSupportedHttpMethods();
        assertTrue("Should not have recorded any supported HTTP methods, but did. Supported methods were: " + supportedMethods, supportedMethods.isEmpty());
    }

}
