/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.webcontainer.servlet61.fat.tests;

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
import componenttest.topology.impl.LibertyServer;

/**
 * Test ServletContext, ServletRequest, ServletResponse: setCharacterEncoding(Charset)
 */
@RunWith(FATRunner.class)
public class Servlet61CharsetEncodingTest {

    private static final Logger LOG = Logger.getLogger(Servlet61CharsetEncodingTest.class.getName());
    private static final String TEST_APP_NAME = "CharSetEncodingTest";
    private static final String WAR_NAME = TEST_APP_NAME + ".war";
    private static final String JAR_NAME = TEST_APP_NAME + ".jar";
    private static final String JAR_RESOURCE = "sci.servlets";

    @Server("servlet61_CharsetEncodingTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_CharsetEncodingTest.");

        // Create the JAR which contain SCI and servlet
        JavaArchive charsetEncodingJar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        ShrinkHelper.addDirectory(charsetEncodingJar, "test-applications/" + JAR_NAME + "/resources");
        charsetEncodingJar.addPackage(JAR_RESOURCE);

        // Create the WAR and add the jar
        WebArchive charsetEncodingWar = ShrinkWrap.create(WebArchive.class, WAR_NAME);
        charsetEncodingWar.addAsLibrary(charsetEncodingJar);
        charsetEncodingWar.addPackage("charset.servlets");

        ShrinkHelper.exportDropinAppToServer(server, charsetEncodingWar);

        server.startServer(Servlet61CharsetEncodingTest.class.getSimpleName() + ".log");
        LOG.info("Setup : startServer, ready for Tests");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test ServletRequest and ServletResponse setCharacterEncoding(Charset)
     */
    @Test
    public void test_RequestResponseCharsetEncoding() throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + "/TestCharSetEncoding";
        HttpGet getMethod = new HttpGet(url);
        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text:");
                LOG.info("\n" + responseText);

                assertTrue("The response has a FAIL result ", !responseText.contains("FAIL"));
            }
        }
    }
}
