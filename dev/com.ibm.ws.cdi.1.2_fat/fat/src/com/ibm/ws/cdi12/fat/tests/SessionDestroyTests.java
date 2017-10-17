/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.fat.tests;

import java.net.MalformedURLException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
public class SessionDestroyTests extends LoggingTest {

    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12BasicServer");

    private static LibertyServer server;
    {
        server = SHARED_SERVER.getLibertyServer();
    }

    private final String expectedResults = "session created: true session destroyed: true session created: true ";

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testInvalidation() throws Exception {

        WebBrowser wb = this.createWebBrowserForTestCase();
        wb.request(createURL("/InvalidateServlet"));
        WebResponse webResponse = wb.request(createURL("/ResultsServlet"));

        Assert.assertTrue("expected " + expectedResults + " but saw " + webResponse.getResponseBody(),
                          webResponse.getResponseBody().contains(expectedResults));

    }

    @Test
    public void testTimeout() throws Exception {

        WebBrowser wb = this.createWebBrowserForTestCase();
        wb.request(createURL("/TimeoutServlet"));
        Thread.sleep(3000);
        wb.request(createURL("/ResultsServlet")); //poke it a second time to ensure that the timeout is processed.
        WebResponse webResponse = wb.request(createURL("/ResultsServlet"));

        Assert.assertTrue("expected " + expectedResults + " but saw " + webResponse.getResponseBody(),
                          webResponse.getResponseBody().contains(expectedResults));

    }

    private String createURL(String path) throws MalformedURLException {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/WebListener" + path;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
