/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wc.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class WCSendRedirectRelativeURLTrue {

    private static final Logger LOG = Logger.getLogger(WCAddJspFileTest.class.getName());

    @Server("servlet40_sendRedirectURL_True")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add TestAddJspFile to the server if not already present.");

        ShrinkHelper.defaultDropinApp(server, "TestAddJspFile.war", "testaddjspfile.listeners");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(WCSendRedirectRelativeURLTrue.class.getSimpleName() + ".log");
        LOG.info("Setup : complete, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test with WC property: redirecttorelativeurl = true.
     * Request to a JSP which generates sendRedirect(RelativeURL). //NOTE that this target RelativeURL does not actually exist.
     * Use the HttpURLConnection() with conn.setInstanceFollowRedirects(false) to stop following the redirect.
     * The test only interests in the 302 with Location header being set to the RelativeURL (i.e there is no http://host:port/ portion)
     * Most (if not all) modern browser agents should be able to redirect with the relative URL
     */
    @Test
    @Mode(TestMode.FULL)
    public void testResponseSendRedirectToRelativeURL_True() throws Exception {
        String expectedLocationHeader = "/TestAddJspFile/targetNoneExistRedirect.jsp";

        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [testResponseSendRedirectToRelativeURL_True]: Test with WCCustomProperty set to true");
        LOG.info("\n /************************************************************************************/");

        try {
            String URLString = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/TestAddJspFile/sendRedirect.jsp";
            URL url = new URL(URLString);
            HttpURLConnection con = null;
            int responseCode = 0;
            String locationHeader;

            con = (HttpURLConnection) url.openConnection();
            con.setInstanceFollowRedirects(false); // Stop following the REDIRECT; true will follow with redirect
            con.setRequestMethod("GET");
            con.connect();
            responseCode = con.getResponseCode();
            locationHeader = con.getHeaderField("Location");

            LOG.info("\n Actual Response code : [" + responseCode + "] , Expected : 302");
            LOG.info("\n Actual Location header: [" + locationHeader + "] , Expected : [" + expectedLocationHeader + "]");

            con.disconnect();

            assertEquals(URLString + "Expecting Response Code 302 ", 302, responseCode);
            assertEquals(URLString + "Expecting Location header: [/TestAddJspFile/targetNoneExistRedirect.jsp]", expectedLocationHeader, locationHeader);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n testResponseSendRedirectToRelativeURL_True]: Test with WCCustomProperty set to true. Finish");
        LOG.info("\n /************************************************************************************/");

    }
}