/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.client.rest.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.junit.Test;

import com.ibm.websphere.jmx.connector.rest.ConnectorSettings;

/**
 *
 */
public class ConnectorTest {

    private static JMXServiceURL TEST_URL = null;
    static {
        try {
            TEST_URL = new JMXServiceURL("rest", "myHost", 1234);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.client.rest.internal.Connector#connect(java.util.Map)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void wlmEndpoints_invalidListObject() throws Exception {
        Connector connector = new Connector(TEST_URL, null);

        List<Integer> list = new ArrayList<Integer>();
        list.add(123);
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(JMXConnector.CREDENTIALS, ConnectorSettings.CERTIFICATE_AUTHENTICATION);
        env.put(ConnectorSettings.WLM_ENDPOINTS, list);
        connector.connect(env);
    }

    /**
     * Test for sending a null initial JMXServiceURL
     */
    @Test(expected = IllegalArgumentException.class)
    public void wlmEndpoints_invalidinitialURL() throws Exception {
        Connector connector = new Connector(null, null);
        connector.connect(null);
    }

    /**
     * Test for no available endpoints exception
     */
    @Test(expected = IOException.class)
    public void wlmEndpoints_noAvailableEndpoints() throws Exception {
        Connector connector = new Connector(TEST_URL, null);
        List<String> list = new ArrayList<String>();
        list.add("otherEndpoint:8010");
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(JMXConnector.CREDENTIALS, ConnectorSettings.CERTIFICATE_AUTHENTICATION);
        env.put(ConnectorSettings.WLM_ENDPOINTS, list);
        connector.connect(env);
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.client.rest.internal.Connector#connect(java.util.Map)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void wlmEndpoints_invalidListElement() throws Exception {
        Connector connector = new Connector(TEST_URL, null);

        List<String> list = new ArrayList<String>();
        list.add(null);
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(JMXConnector.CREDENTIALS, ConnectorSettings.CERTIFICATE_AUTHENTICATION);
        env.put(ConnectorSettings.WLM_ENDPOINTS, list);
        connector.connect(env);
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.client.rest.internal.Connector#connect(java.util.Map)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void wlmEndpoints_noEndpoint() throws Exception {
        Connector connector = new Connector(TEST_URL, null);

        List<String> list = new ArrayList<String>();
        list.add("abc");
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(JMXConnector.CREDENTIALS, ConnectorSettings.CERTIFICATE_AUTHENTICATION);
        env.put(ConnectorSettings.WLM_ENDPOINTS, list);
        connector.connect(env);
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.client.rest.internal.Connector#connect(java.util.Map)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void wlmEndpoints_invalidEndpointBadPort() throws Exception {
        Connector connector = new Connector(TEST_URL, null);

        List<String> list = new ArrayList<String>();
        list.add("abc:xyz");
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(JMXConnector.CREDENTIALS, ConnectorSettings.CERTIFICATE_AUTHENTICATION);
        env.put(ConnectorSettings.WLM_ENDPOINTS, list);
        connector.connect(env);
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.client.rest.internal.Connector#connect(java.util.Map)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void wlmEndpoints_invalidEndpointManyColon() throws Exception {
        Connector connector = new Connector(TEST_URL, null);

        List<String> list = new ArrayList<String>();
        list.add("abc:xyz:123");
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(JMXConnector.CREDENTIALS, ConnectorSettings.CERTIFICATE_AUTHENTICATION);
        env.put(ConnectorSettings.WLM_ENDPOINTS, list);
        connector.connect(env);
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.client.rest.internal.Connector#connect(java.util.Map)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void wlmEndpoints_multipleEndpointsOneInvalid() throws Exception {
        Connector connector = new Connector(TEST_URL, null);

        List<String> list = new ArrayList<String>();
        list.add("host:123");
        list.add("abc:xyz:123");
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(JMXConnector.CREDENTIALS, ConnectorSettings.CERTIFICATE_AUTHENTICATION);
        env.put(ConnectorSettings.WLM_ENDPOINTS, list);
        connector.connect(env);
    }

    /**
     * Test method for {@link com.ibm.ws.jmx.connector.client.rest.internal.Connector#close()}.
     */
    @Test
    public void closeWithoutConnect() throws Exception {
        Connector connector = new Connector(TEST_URL, null);
        connector.close();
    }

    @Test
    public void testEndpointSplitting() throws Exception {
        endpointSplitting("myHost", "123"); //named
        endpointSplitting("9.23.231.832", "22"); //IPv4
        endpointSplitting("[2002:92a:8f7a:305:7c1e:469f:79c0:900]", "5343"); //IPv6
    }

    private void endpointSplitting(String host, String port) {
        String[] splitEndpoint = RESTMBeanServerConnection.splitEndpoint(host + ":" + port);
        assertEquals(splitEndpoint[0], host);
        assertEquals(splitEndpoint[1], port);
    }
}
