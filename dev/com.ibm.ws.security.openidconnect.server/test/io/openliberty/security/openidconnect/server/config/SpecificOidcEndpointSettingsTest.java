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
package io.openliberty.security.openidconnect.server.config;

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
public class SpecificOidcEndpointSettingsTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("io.openliberty.security.openidconnect.*=all:com.ibm.ws.security.openidconnect.*=all");

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
        EndpointType endpointType = EndpointType.userinfo;
        SpecificOidcEndpointSettings settings = new SpecificOidcEndpointSettings(endpointType);
        assertEquals("Endpoint type did not match expected value.", endpointType, settings.getEndpointType());
        assertEquals("Endpoint name did not match expected value.", "userinfo", settings.getEndpointName());
        // By default, there should be no supported HTTP methods
        Set<HttpMethod> supportedMethods = settings.getSupportedHttpMethods();
        assertNotNull("Set of supported methods should not have been null.", supportedMethods);
        assertTrue("Should not have recorded any supported HTTP methods, but did. Supported methods were: " + supportedMethods, supportedMethods.isEmpty());
    }

}
