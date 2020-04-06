/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import componenttest.annotation.MinimumJavaLevel;

/**
 * Tests to execute on the wcServer that use HttpUnit.
 */
@MinimumJavaLevel(javaLevel = 7)
public class VHServerHttpUnit extends LoggingTest {
    private static final Logger LOG = Logger.getLogger(VHServerHttpUnit.class.getName());
    protected static final Map<String, String> testUrlMap = new HashMap<String, String>();

    @ClassRule
    public static SharedServer ALTVHOST_SERVER = new SharedServer("servlet31_vhServer");

    //public static SharedServer ALTVHOST_SERVER = SHARED_SERVER;

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
    }

    @Test
    public void testDefaultGetVirtulServerName() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | DefaultGetVirtulServerNameTest : virtual host is default");
        LOG.info("\n /************************************************************************************/");

        String serverVName = "default_host";
        LOG.info("Expecting virtualServerName : " + serverVName);
        WebConversation wc = new WebConversation();
        String contextRoot = "/TestServlet31";
        wc.setExceptionsThrownOnErrorStatus(false);

        WebRequest request = new GetMethodWebRequest(ALTVHOST_SERVER.getServerUrl(true, contextRoot + "/GetVirtualServerNameServlet?serverName=" + serverVName));
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().startsWith("SUCCESS"));

    }

    @Test
    public void testConfiguredGetVirtualServerName1() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | GetConfiguredServerNameTest : virtual host is alternateHost1");
        LOG.info("\n /************************************************************************************/");

        String serverVName = "alternateHost1";

        LOG.info("Expecting virtualServerName : " + serverVName);
        WebConversation wc = new WebConversation();

        String hostName = ALTVHOST_SERVER.getLibertyServer().getHostname();
        String hostPort = "18080";
        String contextRoot = "/TestServlet31AltVHost1";

        wc.setExceptionsThrownOnErrorStatus(false);

        String url = "http://" + hostName + ":" + hostPort + contextRoot + "/GetAltVirtualServerNameServlet1?serverName=" + serverVName;

        LOG.info("Request Url : " + url);
        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().startsWith("SUCCESS"));

    }

    @Test
    public void testConfiguredGetVirtualServerName2() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | GetConfiguredServerNameTest : virtual host is alternateHost2");
        LOG.info("\n /************************************************************************************/");

        String serverVName = "alternateHost2";

        LOG.info("Expecting virtualServerName : " + serverVName);
        WebConversation wc = new WebConversation();

        String hostName = ALTVHOST_SERVER.getLibertyServer().getHostname();
        String hostPort = "18082";
        String contextRoot = "/TestServlet31AltVHost2";

        wc.setExceptionsThrownOnErrorStatus(false);

        String url = "http://" + hostName + ":" + hostPort + contextRoot + "/GetAltVirtualServerNameServlet2?serverName=" + serverVName;

        LOG.info("Request Url : " + url);
        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().startsWith("SUCCESS"));

    }

    @Test
    public void testConfiguredGetVirtualServerName3() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | GetConfiguredServerNameTest : virtual host is alternateHost2");
        LOG.info("\n /************************************************************************************/");

        String serverVName = "alternateHost2";

        LOG.info("Expecting virtualServerName : " + serverVName);
        WebConversation wc = new WebConversation();

        String hostName = ALTVHOST_SERVER.getLibertyServer().getHostname();
        String hostPort = "18443";
        String contextRoot = "/TestServlet31AltVHost2";

        wc.setExceptionsThrownOnErrorStatus(false);

        String url = "http://" + hostName + ":" + hostPort + contextRoot + "/GetAltVirtualServerNameServlet2?serverName=" + serverVName;

        LOG.info("Request Url : " + url);
        WebRequest request = new GetMethodWebRequest(url);
        WebResponse response = wc.getResponse(request);
        LOG.info("Servlet response : " + response.getText());
        assertTrue(response.getResponseCode() == 200);
        assertTrue(response.getText().startsWith("SUCCESS"));

    }

}
