/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * Test Class used to test default properties in Webcontainer
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCServerPropertyTest {

    private static final Logger LOG = Logger.getLogger(WCServerPropertyTest.class.getName());

    @Server("servlet40_jspCdi")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ShrinkHelper.defaultDropinApp(server, "DeferServletRequestListenerDestroyOnErrorTest.war", "deferlistener.war.bean");

        server.startServer(WCServerPropertyTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE0315E", "SRVE0777E"); // related to the RuntimeException in testDeferServletRequestListenerDestroyOnError
        }
    }

    /**
     * Tests the default deferServletRequestListenerDestroyOnError the running servlet version.
     * For servlet-5.0 and later, it's true. For all others, it's false.
     *
     * The error page uses the testBean, but when WeldInitialListener.requestDestroyed is called, it deactivates CDI beans.
     * An error therefore during error page processing.
     * ServletRequestListener.requestDestroyed should be defered and called after the error page handling.
     *
     * In both cases, RuntimeException is expected (thrown in test.jsp).
     *
     * - Servlet 4.0 and lower: 500, Empty page, and ContextNotActiveException
     * - Servlet 5.0 and higher: 500, "Expected Error Page!"
     *
     * The property was originally added in PI26908.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" }) // Original Exception
    @AllowedFFDC({ "org.jboss.weld.contexts.ContextNotActiveException" }) // Occurs when deferServletRequestListenerDestroyOnError = false
    public void testDeferServletRequestListenerDestroyOnError() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + "DeferServletRequestListenerDestroyOnErrorTest" + "/test.jsp";
        String expectedResponse = "Expected Error Page! Bean.prop = test";

        HttpGet getMethod = new HttpGet(url);

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {

                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                if (JakartaEE9Action.isActive()) {
                    assertTrue("The response did not contain the following String: " + expectedResponse, responseText.contains(expectedResponse));
                } else {
                    assertTrue("The response should have been empty! " + expectedResponse, responseText.length() == 0);
                }
                // True for both runs
                assertTrue("The response should have been 500!", response.getCode() == 500);

                

            }
        }
    }

}
