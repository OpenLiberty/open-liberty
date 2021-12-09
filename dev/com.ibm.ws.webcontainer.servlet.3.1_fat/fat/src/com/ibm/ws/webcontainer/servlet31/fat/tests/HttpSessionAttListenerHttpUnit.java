/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
import junit.framework.Assert;

/**
 *
 */
@RunWith(FATRunner.class)
public class HttpSessionAttListenerHttpUnit {

    private static final Logger LOG = Logger.getLogger(HttpSessionAttListenerHttpUnit.class.getName());

    private static final String TEST_HTTP_SESSSION_ATTRIBUTE_LISTENER_APP_NAME = "TestHttpSessionAttrListener";

    @Server("servlet31_wcServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the war app and add the dependencies
        WebArchive TestHttpSessionAttrListenerApp = ShrinkHelper.buildDefaultApp(TEST_HTTP_SESSSION_ATTRIBUTE_LISTENER_APP_NAME + ".war",
                                                                                 "com.ibm.ws.webcontainer.servlet_31_fat.testhttpsessionattrlistener.war.sessionListener");

        // Export the application.
        ShrinkHelper.exportDropinAppToServer(server, TestHttpSessionAttrListenerApp);

        // Start the server and use the class name so we can find logs easily.
        server.startServer(HttpSessionAttListenerHttpUnit.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (server != null && server.isStarted()) {
            server.stopServer("SRVE8015E:.*");
        }
    }

    private static final String LISTENER_SERVLET_URL = "/TestHttpSessionAttrListener/TestAddListener";

    /*
     * This test will first create HttpSessionAttributeListener.
     * Add attribute to it.
     * Call invalidate , which will call removeattribute by server session.
     *
     * Test will make sure that it passes when Add and Remove Attribute triggers HttpSessionAttributeListener.
     */
    @Test
    @Mode(TestMode.LITE)
    public void test_RemoveAttribute_onInvalidate() throws Exception {
        LOG.info("\n /************************************************************************************/");
        LOG.info("\n [WebContainer | HttpSessionAttListenerHttpUnit]: test_RemoveAttribute_onInvalidate Start");

        try {
            HttpClient HttpClient = new HttpClient();
            PostMethod post = new PostMethod("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + LISTENER_SERVLET_URL);

            int responseCode = HttpClient.executeMethod(post);
            LOG.info("Status Code = " + responseCode);

            String responseBody = post.getResponseBodyAsString();
            LOG.info("responseBody = " + responseBody);

            post.releaseConnection();

            Assert.assertTrue(responseBody.contains("TEST_PASSED"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception from request: " + e.getMessage());
        }

        LOG.info("\n [WebContainer | HttpSessionAttListenerHttpUnit]: test_RemoveAttribute_onInvalidate Finish");
        LOG.info("\n /************************************************************************************/");
    }
}
