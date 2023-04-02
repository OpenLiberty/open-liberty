/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.openidconnect.backchannellogout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class BackchannelLogoutServiceTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private BackchannelLogoutService service;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        service = new BackchannelLogoutService();
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
    public void test_normalizeUsername_simple() {
        String input = "jdoe";
        String result = service.normalizeUserName(input);
        assertEquals(input, result);
    }

    @Test
    public void test_normalizeUsername_accessId_simple() {
        String expectedUserName = "jdoe";
        String input = "user:BasicRealm/" + expectedUserName;
        String result = service.normalizeUserName(input);
        assertEquals(expectedUserName, result);
    }

    @Test
    public void test_normalizeUsername_accessId_complex() {
        String expectedUserName = "jdoe";
        String input = "user:http://1.2.3.4/" + expectedUserName;
        String result = service.normalizeUserName(input);
        assertEquals(expectedUserName, result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_requestUriEmpty() {
        String requestUri = "";
        String providerId = "OP";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertFalse("An empty request URI should not have been matched.", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_requestUriCompletelyDifferent() {
        String requestUri = "/some/other/path/to/logout";
        String providerId = "OP";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertFalse("Request URI [" + requestUri + "] should not have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_contextPathSubstringShouldNotMatch() {
        String providerId = "OP";
        String requestUri = "/someother" + providerId + "/logout";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertFalse("Request URI [" + requestUri + "] should not have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_endpointSubstringShouldNotMatch() {
        String providerId = "OP";
        String requestUri = "/" + providerId + "/logoutEndpoint";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertFalse("Request URI [" + requestUri + "] should not have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_simpleMatch_logout() {
        String providerId = "OP";
        String requestUri = "/" + providerId + "/logout";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertTrue("Request URI [" + requestUri + "] should have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_simpleMatch_endSession() {
        String providerId = "OP";
        String requestUri = "/" + providerId + "/end_session";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertTrue("Request URI [" + requestUri + "] should have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_matchWithContextRoot_logout() {
        String providerId = "OP";
        String requestUri = "/lengthy/context/root/" + providerId + "/logout";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertTrue("Request URI [" + requestUri + "] should have been matched to the provider [" + providerId + "].", result);
    }

    @Test
    public void test_isEndpointThatMatchesConfig_matchWithContextRoot_endSession() {
        String providerId = "OP";
        String requestUri = "/lengthy/context/root/" + providerId + "/end_session";
        boolean result = service.isEndpointThatMatchesConfig(requestUri, providerId);
        assertTrue("Request URI [" + requestUri + "] should have been matched to the provider [" + providerId + "].", result);
    }

}
