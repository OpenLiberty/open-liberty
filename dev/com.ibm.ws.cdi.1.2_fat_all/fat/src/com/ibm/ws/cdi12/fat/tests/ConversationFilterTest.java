/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import java.net.MalformedURLException;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class ConversationFilterTest extends LoggingTest {

    private static LibertyServer server;

    @Override
    protected ShutDownSharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getStartedLibertyServer("conversationFilterServer");
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application appConversationFilter started");
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
