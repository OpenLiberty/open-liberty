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
package io.openliberty.webcontainer.servlet60.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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

/**
 * Test the session cookie-config setAttribute API setting via web.xml
 * Test the specific getter and getAttribute for the same specific attributes (i.e Domain, Path) to make sure
 * both return the same value.
 * Test lower case attribute name which should be case insensitive
 *
 * This test uses the jvm.options set.cookie.config.sci.setCookieConfigViaSCI=false in order to set the config from web.xml
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class Servlet60SessionCookieConfigXMLTest {

    private static final Logger LOG = Logger.getLogger(Servlet60SessionCookieConfigXMLTest.class.getName());
    private static final String TEST_APP_NAME = "SessionCookieConfigSCI"; //i.e context-root
    private static final String WAR_NAME = TEST_APP_NAME + ".war";
    private static final String JAR_NAME = TEST_APP_NAME + ".jar";
    private static final String JAR_RESOURCE = "testsci.jar.servlets";

    @Server("servlet60_sessionCookieConfigXMLTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : add " + WAR_NAME + " to the server if not already present.");

        // Create the JAR which contain SCI and servlet
        JavaArchive sessionCookieConfigJar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        sessionCookieConfigJar.addPackage(JAR_RESOURCE);
        ShrinkHelper.addDirectory(sessionCookieConfigJar, "test-applications/" + JAR_NAME + "/resources");

        // Create the WAR and add the jar
        WebArchive sessionCookieConfigWar = ShrinkWrap.create(WebArchive.class, WAR_NAME);
        sessionCookieConfigWar.addAsLibrary(sessionCookieConfigJar);
        ShrinkHelper.addDirectory(sessionCookieConfigWar, "test-applications/" + WAR_NAME + "/resources");

        ShrinkHelper.exportToServer(server, "dropins", sessionCookieConfigWar);

        LOG.info("Setup : startServer, ready for Tests");
        server.startServer(Servlet60SessionCookieConfigXMLTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Programmatically add/set all cookies attribute (using specific setters and new setAttribute)
     * Servlet (/TestSessionCookieConfigServlet) is also added programmatically
     * Servlet verifies and reports to client the result in the response header
     */
    @Test
    public void test_sessionCookieConfigureByWebXML() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestSessionCookieConfigServlet";
        String expectedGeneralResponse = "Hello World from TestSessionCookieConfigServlet";
        LOG.info("\n Sending Request [" + url + "]");
        HttpGet getMethod = new HttpGet(url);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: [" + responseText + "]");

                //fail-fast check
                assertTrue("The response did not contain the following String: " + expectedGeneralResponse, responseText.contains(expectedGeneralResponse));

                String headerValue = response.getHeader("TestResult").getValue();

                LOG.info("\n TestResult : " + headerValue);

                assertTrue("The response does not contain Result [PASS]. TestResult header [" + headerValue + "]", headerValue.contains("Result [PASS]"));

            }
        }
    }
}
