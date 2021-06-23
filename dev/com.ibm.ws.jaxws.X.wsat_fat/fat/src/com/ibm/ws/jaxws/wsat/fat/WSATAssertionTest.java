/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.jaxws.wsat.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;


import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
@RunWith(FATRunner.class)
public class WSATAssertionTest {
    @Server("clientout_server")
    public static LibertyServer server;

	
    private static final String contextRoot = "/WSATAssertionTest";

    private static String BASE_URL;
    private final static int REQUEST_TIMEOUT = 60;
    

    public static String serviceApp = "WSATAssertionTest";

    @BeforeClass
    public static void beforeTests() throws Exception {
        WebArchive wsatAssertionTest = ShrinkWrap.create(WebArchive.class, serviceApp + ".war")
                .addPackages(false, "com.ibm.ws.wsat.fat.client.assertion")
                .addPackages(false, "com.ibm.ws.wsat.fat.server");

        ShrinkHelper.addDirectory(wsatAssertionTest, "test-applications/" + serviceApp + "/resources");

        ShrinkHelper.exportDropinAppToServer(server, wsatAssertionTest);

        BASE_URL = "http://" + server.getHostname() + ":"
                + server.getHttpDefaultPort();
        if (server != null && !server.isStarted()) {
            server.startServer();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer(); // ensure server has stopped
        }
    }

    @Test
    public void testWSATAssertion() throws Exception {
        try {
            String urlStr = BASE_URL + contextRoot +
                            "/AssertionClientServlet?&baseurl=" + BASE_URL;
            HttpURLConnection con = HttpUtils.getHttpConnection(new URL(urlStr),
                                                                HttpURLConnection.HTTP_OK, REQUEST_TIMEOUT);
            BufferedReader br = HttpUtils.getConnectionStream(con);
            String result = br.readLine();
            assertNotNull(result);
            System.out.println("testWSATAssertion Result : " + result);
            assertTrue("Cannot get expected exception from server",
                       result.contains("WS-AT Feature is not installed"));

        } catch (Exception e) {
            fail("Exception happens: " + e.toString());
        }
    }
}
