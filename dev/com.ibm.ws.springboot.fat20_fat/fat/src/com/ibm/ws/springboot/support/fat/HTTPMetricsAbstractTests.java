/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Before;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.utils.HttpUtils;
import junit.framework.Assert;

public abstract class HTTPMetricsAbstractTests extends AbstractSpringTests {

    public static final String NO_CONTEXTROOT_WAR = "com.ibm.ws.springboot.fat20.http.app-0.0.1-SNAPSHOT.war";
    public static final String WITH_CONTEXTROOT_WAR = "com.ibm.ws.springboot.fat20.http.contextroot.app-0.0.1-SNAPSHOT.war";

    public static final String CONTEXT_ROOT = "/contextual";

    @Override
    public Set<String> getFeatures() {
        HashSet<String> result = new HashSet<>(Arrays.asList("springBoot-2.0", "servlet-4.0", "monitor-1.0", "jsp-2.3"));
        return result;
    }

    @Before
    public void setDefaultPort() {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
    }

    @Override
    public boolean useDefaultVirtualHost() {
        return true;
    }

    @Override
    public void modifyServerConfiguration(ServerConfiguration config) {
        config.getMPMetricsElement().setAuthentication(false);
    }

    protected boolean checkMBeanRegistered(String objectName) {
        return checkMBeanRegistered(objectName, false);
    }

    protected boolean checkMBeanRegistered(String objectName, boolean isContextRoot) {
        boolean result = false;
        HttpURLConnection mbBeanConn;
        try {
            mbBeanConn = HttpUtils.getHttpConnection(server, ((isContextRoot == true) ? CONTEXT_ROOT : "") + "/mbean/query?objectName=" + objectName);
            Assert.assertTrue(String.format("Expected %d, but got %d", 200, mbBeanConn.getResponseCode()), mbBeanConn.getResponseCode() == 200);
            String response = getResponseContent(mbBeanConn);
            response = response.trim();

            if (response.equalsIgnoreCase("true")) {
                result = true;
            } else {
                result = false;
                Log.info(getClass(), "checkMBeanRegistered",
                         String.format("Checking for Mbean registration did not find %s. Here is the list of registered HTTP Mbeans: \n %s",
                                       objectName, response));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    @AfterClass
    public static void stopTestServer() throws Exception {
        //Options request method results in the below warning (i.e., testNormalPathOptions()).
        stopServer(true, "SRVE8094W");
    }

    protected String getResponseContent(HttpURLConnection con) {
        try {
            String sep = System.getProperty("line.separator");

            String line = null;
            StringBuilder lines = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while ((line = br.readLine()) != null && line.length() > 0) {
                lines.append(line).append(sep);
            }

            return lines.toString();
        } catch (IOException e) {
            Log.info(getClass(), "getResponseContent", "Encountered IO exception " + e);
            return null;
        } catch (Exception e) {
            Log.info(getClass(), "getResponseContent", "Encountered an exception " + e);
            return null;
        } finally {
            if (con != null)
                con.disconnect();
        }

    }
}
