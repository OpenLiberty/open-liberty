/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.security.fat;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * This class tests the scenario that WSDL location is set to HTTPS url, and
 * need to initialize SSL configuration before trying to access WSDL doc.
 */
@RunWith(FATRunner.class)
@Mode(Mode.TestMode.LITE)
public class WsdlLocationHttpsTest {

    @Server("WsdlLocationHttpsServer")
    public static LibertyServer server;

    private final static int REQUEST_TIMEOUT = 10;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void beforeAllTests() throws Exception {

        server.addIgnoredErrors(Arrays.asList("CWPKI0063W"));
        ShrinkHelper.defaultDropinApp(server, "WsdlLocationHttpsClient", "com.ibm.was.wssample.sei.echo",
                                      "com.ibm.was.wssample.servlet");

        ShrinkHelper.defaultDropinApp(server, "WsdlLocationHttpsServer", "com.ibm.was.wssample.sei.echo");

        server.startServer();
        Assert.assertNotNull("The application WsdlLocationHttpsServer did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*WsdlLocationHttpsServer"));
        Assert.assertNotNull("The application WsdlLocationHttpsClient did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*WsdlLocationHttpsClient"));
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void test_access_https_wsdl_location() throws Exception {
        StringBuilder sBuilder = new StringBuilder("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/WsdlLocationHttpsClient/EchoServlet").append("?securePort=").append(server.getHttpDefaultSecurePort());
        String urlStr = sBuilder.toString();
        Log.info(this.getClass(), testName.getMethodName(), "Calling Application with URL=" + urlStr);

        HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr), HttpURLConnection.HTTP_OK,
                                                            REQUEST_TIMEOUT);
        BufferedReader br = HttpUtils.getConnectionStream(con);
        String line = br.readLine();

        String responseString = "JAX-WS==>>HttpsWsdlLocation";
        assertTrue("The excepted response must contain " + responseString + ", but the real response is " + line,
                   line.contains(responseString));
    }

}
