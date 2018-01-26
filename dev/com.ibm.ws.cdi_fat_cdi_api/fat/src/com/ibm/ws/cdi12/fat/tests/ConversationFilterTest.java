/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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

import java.io.File;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import com.ibm.websphere.simplicity.ShrinkHelper;

public class ConversationFilterTest extends LoggingTest {

    private static LibertyServer server;

    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getStartedLibertyServer("conversationFilterServer");
        WebArchive appConversationFilter = ShrinkWrap.create(WebArchive.class, "appConversationFilter.war")
                        .addClass("test.conversation.filter.ConversationActiveState")
                        .addClass("test.conversation.filter.ConversationBean")
                        .addClass("test.conversation.filter.TestServlet")
                        .addClass("test.conversation.filter.FirstFilter")
                        .add(new FileAsset(new File("test-applications/appConversationFilter.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        ShrinkHelper.exportDropinAppToServer(server, appConversationFilter);
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
