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
package com.ibm.ws.cdi12.fat.tests;

import java.net.MalformedURLException;

import org.jboss.shrinkwrap.api.Archive;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.cdi12.suite.ShrinkWrapServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class ConversationFilterTest extends LoggingTest {

    private static LibertyServer server;

    @Override
    protected ShrinkWrapServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getStartedLibertyServer("conversationFilterServer");
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application appConversationFilter started");

        for (Archive archive : ShrinkWrapServer.getAppsForServer("conversationFilterServer")) {
            ShrinkHelper.exportDropinAppToServer(server, archive);
        }
    }

    @Test
    public void testAppServlet() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        WebResponse response = browser.request(createURL("/appConversationFilter/test?op=begin"));
        String cid = response.getResponseBody();
        Assert.assertTrue("No cid: " + cid, cid != null && !!!cid.isEmpty());

        response = browser.request(createURL("/appConversationFilter/test?op=status&cid=" + cid));
        Assert.assertEquals("Wrong status", Boolean.FALSE.toString(), response.getResponseBody());
    }

    private String createURL(String path) throws MalformedURLException {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

}
