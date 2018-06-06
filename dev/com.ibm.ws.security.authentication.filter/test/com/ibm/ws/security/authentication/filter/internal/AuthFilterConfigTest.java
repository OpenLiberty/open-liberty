/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.filter.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class AuthFilterConfigTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    final static String ID = "myAuthFilter";

    final static String MY_REQUEST_URL = "myRequestUrl";
    final static String MY_URL_PATTERN = "myUrlPattern";

    final static String MY_WEB_APP = "myWebApp";
    final static String MY_APP_NAME = "myAppName";

    final static String MY_HOST_NAME = "myHostName";

    final static String MY_IP = "myIp";

    final static String MY_AGENT_NAME = "myAgentName";

    @Test
    public void testConstructor_nullProps() throws Exception {
        final String methodName = "testConstructor_nullProps";
        try {
            AuthFilterConfig auhFilterConfig = new AuthFilterConfig(null);
            assertNotNull("spn GSSCredential should not be emptied", auhFilterConfig);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testProperties_null() throws Exception {
        final String methodName = "testProperties_null";
        try {
            AuthFilterConfig auhFilterConfig = new AuthFilterConfig(null);
            assertFalse("should have no authFilter config", auhFilterConfig.hasAnyFilterConfig());

            assertTrue("Expected message was not logged",
                       outputMgr.checkForMessages("CWWKS4357I:"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRequestUrl() throws Exception {
        final String methodName = "testRequestUrl";
        final Map<String, Object> props = new Hashtable<String, Object>();
        props.put(AuthFilterConfig.KEY_ID, ID);
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("requestUrl.0.urlPattern", MY_URL_PATTERN);
        p.put("requestUrl.0.matchType", AuthFilterConfig.MATCH_TYPE_CONTAINS);
        props.putAll(p);

        try {
            AuthFilterConfig auhFilterConfig = new AuthFilterConfig(props);
            assertEquals("authFilter ID should be " + ID, ID, auhFilterConfig.getId());
            assertTrue("should have filter configure ", auhFilterConfig.hasFilterConfig());
            assertEquals("matchType should be " + AuthFilterConfig.MATCH_TYPE_CONTAINS, AuthFilterConfig.MATCH_TYPE_CONTAINS,
                         auhFilterConfig.getRequestUrls().get(0).getProperty(AuthFilterConfig.KEY_MATCH_TYPE));
            assertEquals("urlPattern should be " + MY_URL_PATTERN, MY_URL_PATTERN, auhFilterConfig.getRequestUrls().get(0).getProperty(AuthFilterConfig.KEY_URL_PATTERN));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testWebApp() throws Exception {
        final String methodName = "testWebApp";
        final Map<String, Object> props = new Hashtable<String, Object>();
        props.put(AuthFilterConfig.KEY_ID, ID);
        final Map<String, Object> p = new Hashtable<String, Object>();
        p.put("webApp.0.name", MY_APP_NAME);
        p.put("webApp.0.matchType", AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN);
        props.putAll(p);

        try {
            AuthFilterConfig auhFilterConfig = new AuthFilterConfig(props);
            assertEquals("matchType should be " + AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN, AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN,
                         auhFilterConfig.getWebApps().get(0).getProperty(AuthFilterConfig.KEY_MATCH_TYPE));
            assertEquals("webApp name should be " + MY_APP_NAME, MY_APP_NAME, auhFilterConfig.getWebApps().get(0).getProperty(AuthFilterConfig.KEY_NAME));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testHost() throws Exception {
        final String methodName = "testHost";
        final Map<String, Object> props = new Hashtable<String, Object>();
        props.put(AuthFilterConfig.KEY_ID, ID);
        final Map<String, Object> p = new Hashtable<String, Object>();
        p.put("host.0.name", MY_HOST_NAME);
        p.put("host.0.matchType", AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN);
        props.putAll(p);

        try {
            AuthFilterConfig auhFilterConfig = new AuthFilterConfig(props);
            assertEquals("matchType should be " + AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN, AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN,
                         auhFilterConfig.getHosts().get(0).getProperty(AuthFilterConfig.KEY_MATCH_TYPE));
            assertEquals("host name should be " + MY_HOST_NAME, MY_HOST_NAME, auhFilterConfig.getHosts().get(0).getProperty(AuthFilterConfig.KEY_NAME));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testRemoteAddress() throws Exception {
        final String methodName = "testRemoteAddress";
        final Map<String, Object> props = new Hashtable<String, Object>();
        props.put(AuthFilterConfig.KEY_ID, ID);
        final Map<String, Object> p = new Hashtable<String, Object>();
        p.put("remoteAddress.0.ip", MY_IP);
        p.put("remoteAddress.0.matchType", AuthFilterConfig.MATCH_TYPE_GREATER_THAN);
        props.putAll(p);

        try {
            AuthFilterConfig auhFilterConfig = new AuthFilterConfig(props);
            assertEquals("matchType should be " + AuthFilterConfig.MATCH_TYPE_GREATER_THAN, AuthFilterConfig.MATCH_TYPE_GREATER_THAN,
                         auhFilterConfig.getRemoteAddresses().get(0).getProperty(AuthFilterConfig.KEY_MATCH_TYPE));
            assertEquals("host name should be " + MY_IP, MY_IP, auhFilterConfig.getRemoteAddresses().get(0).getProperty(AuthFilterConfig.KEY_IP));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testUserAgent() throws Exception {
        final String methodName = "testUserAgent";
        final Map<String, Object> props = new Hashtable<String, Object>();
        props.put(AuthFilterConfig.KEY_ID, ID);
        final Map<String, Object> p = new Hashtable<String, Object>();
        p.put("userAgent.0.agent", MY_AGENT_NAME);
        p.put("userAgent.0.matchType", AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN);
        props.putAll(p);

        try {
            AuthFilterConfig auhFilterConfig = new AuthFilterConfig(props);
            assertEquals("matchType should be " + AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN, AuthFilterConfig.MATCH_TYPE_NOT_CONTAIN,
                         auhFilterConfig.getUserAgents().get(0).getProperty(AuthFilterConfig.KEY_MATCH_TYPE));
            assertEquals("agent name should be " + MY_AGENT_NAME, MY_AGENT_NAME, auhFilterConfig.getUserAgents().get(0).getProperty(AuthFilterConfig.KEY_AGENT));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
