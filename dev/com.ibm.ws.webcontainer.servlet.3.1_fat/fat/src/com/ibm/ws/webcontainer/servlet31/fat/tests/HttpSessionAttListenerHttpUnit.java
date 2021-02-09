/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import junit.framework.Assert;

/**
 *
 */
@RunWith(FATRunner.class)
public class HttpSessionAttListenerHttpUnit extends LoggingTest {

    private static final Logger LOG = Logger.getLogger(HttpSessionAttListenerHttpUnit.class.getName());

    private static final String TEST_HTTP_SESSSION_ATTRIBUTE_LISTENER_APP_NAME = "TestHttpSessionAttrListener";

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("servlet31_wcServer");

    @BeforeClass
    public static void setupClass() throws Exception {
        // Build the war app and add the dependencies
        WebArchive TestHttpSessionAttrListenerApp = ShrinkHelper.buildDefaultApp(TEST_HTTP_SESSSION_ATTRIBUTE_LISTENER_APP_NAME + ".war",
                                                                                 "com.ibm.ws.webcontainer.servlet_31_fat.testhttpsessionattrlistener.war.sessionListener");
        // Verify if the apps are in the server before trying to deploy them
        if (SHARED_SERVER.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SHARED_SERVER.getLibertyServer().getInstalledAppNames(TEST_HTTP_SESSSION_ATTRIBUTE_LISTENER_APP_NAME);
            LOG.info("addAppToServer : " + TEST_HTTP_SESSSION_ATTRIBUTE_LISTENER_APP_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
              ShrinkHelper.exportDropinAppToServer(SHARED_SERVER.getLibertyServer(), TestHttpSessionAttrListenerApp);
          }
        SHARED_SERVER.startIfNotStarted();
        SHARED_SERVER.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + TEST_HTTP_SESSSION_ATTRIBUTE_LISTENER_APP_NAME);
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        // test cleanup
        if (SHARED_SERVER.getLibertyServer() != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            SHARED_SERVER.getLibertyServer().stopServer("SRVE8015E:.*");
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
            PostMethod post = new PostMethod(SHARED_SERVER.getServerUrl(true, LISTENER_SERVLET_URL));

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

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }
}
