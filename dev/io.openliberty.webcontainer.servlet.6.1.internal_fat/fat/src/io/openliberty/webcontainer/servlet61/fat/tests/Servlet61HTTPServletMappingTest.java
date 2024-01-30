/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
 * Test the functionality of the HttpServletRequest.getMapping()
 *
 * Multiple requests will be driven to the same servlet using different url
 * patterns to ensure that the correct ServletMapping values are returned.
 *
 * The following examples are from the updated Java doc Servlet 6.1
 *
 * <servlet>
 *     <servlet-name>MyServlet</servlet-name>
 *     <servlet-class>MyServlet</servlet-class>
 * </servlet>
 *
 * <servlet-mapping>
 *     <servlet-name>MyServlet</servlet-name>
 *     <url-pattern>/MyServlet</url-pattern>
 *     <url-pattern>""</url-pattern>
 *     <url-pattern>*.extension</url-pattern>
 *     <url-pattern>/path/*</url-pattern>
 * </servlet-mapping>
 *
 * URI Path (in quotes)         matchValue      pattern         mappingMatch
 *   ""                         ""              ""              CONTEXT_ROOT
 *   "/index.html"              ""              /               DEFAULT
 *   "MyServlet/index.html"     ""              /               DEFAULT
 *   "/MyServlet"               MyServlet       /MyServlet      EXACT
 *   "MyServlet/foo"            ""              /               DEFAULT
 *   "/foo.extension"           foo             *.extension     EXTENSION
 *   "/bar/foo.extension"       bar/foo         *.extension     EXTENSION
 *   "/path/foo"                foo             /path/*         PATH
 *   "/path/foo/bar"            foo/bar         /path/*         PATH
 *
 *
 * There are 4 new examples added in Servlet 6.1.  This test will cover those 4.
 * The remaining should have covered/tested in the FAT 4.0 WCGetMappingTest
 *
 * URI Path (in quotes)         matchValue      pattern         mappingMatch
 *   "MyServlet/index.html"     ""              /               DEFAULT
 *   "MyServlet/foo"            ""              /               DEFAULT
 *   "/bar/foo.extension"       bar/foo         *.extension     EXTENSION
 *   "/path/foo/bar"            foo/bar         /path/*         PATH
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class Servlet61HTTPServletMappingTest {
    private static final Logger LOG = Logger.getLogger(Servlet61HTTPServletMappingTest.class.getName());
    private static final String TEST_APP_NAME = "HTTPServletMappingTest";

    @Server("servlet61_HttpServletMappingTest")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        LOG.info("Setup : servlet61_HttpServletMappingTest.");
        ShrinkHelper.defaultDropinApp(server, TEST_APP_NAME + ".war", "getmapping.servlets");
        server.startServer(Servlet61HTTPServletMappingTest.class.getSimpleName() + ".log");
        LOG.info("Setup : startServer, ready for Tests.");
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        LOG.info("testCleanUp : stop server");

        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Request URI is /HTTPServletMappingTest//MyServlet/index.html  (Note that there is an empty space between context root and MyServlet
     */
    @Test
    public void test_mappingMatch_DEFAULT() throws Exception {
        String URI = "//MyServlet/index.html";
        String expectedString = "mappingMatch [DEFAULT] matchValue [] pattern [/] servletName [MyServlet]";

        LOG.info("====== <test_mappingMatch_DEFAULT> ======");
        runTest(URI, expectedString);
    }

    @Test
    public void test_mappingMatch_DEFAULT_1() throws Exception {
        String URI = "//MyServlet/foo";
        String expectedString = "mappingMatch [DEFAULT] matchValue [] pattern [/] servletName [MyServlet]";

        LOG.info("====== <test_mappingMatch_DEFAULT_1> ======");
        runTest(URI, expectedString);
    }

    @Test
    public void test_mappingMatch_EXTENSION() throws Exception {
        String URI = "/bar/foo.extension";
        String expectedString = "mappingMatch [EXTENSION] matchValue [bar/foo] pattern [*.extension] servletName [MyServlet]";

        LOG.info("====== <test_mappingMatch_EXTENSION> ======");
        runTest(URI, expectedString);

    }

    @Test
    public void test_mappingMatch_PATH() throws Exception {
        String URI = "/path/foo/bar";
        String expectedString = "mappingMatch [PATH] matchValue [foo/bar] pattern [/path/*] servletName [MyServlet]";

        LOG.info("====== <test_mappingMatch_PATH> ======");
        runTest(URI, expectedString);
    }

    private void runTest(String URI, String stringToSearch) throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_NAME + URI;
        HttpGet getMethod = new HttpGet(url);

        LOG.info("Sending request ["+ url + "]");

        try (final CloseableHttpClient client = HttpClientBuilder.create().build()) {
            try (final CloseableHttpResponse response = client.execute(getMethod)) {
                String responseText = EntityUtils.toString(response.getEntity());
                LOG.info("\n" + "Response Text: \n[" + responseText + "]");

                if (stringToSearch != null) {
                    assertTrue("Expected string not found [" + stringToSearch + "]", responseText.contains(stringToSearch));
                }
            }
        }
    }
}
