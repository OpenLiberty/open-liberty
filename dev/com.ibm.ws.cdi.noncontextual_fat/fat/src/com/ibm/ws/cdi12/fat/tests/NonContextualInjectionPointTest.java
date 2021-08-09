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

import static org.junit.Assert.assertNotNull;

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


import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

public class NonContextualInjectionPointTest extends LoggingTest {

    private static LibertyServer server;

    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getStartedLibertyServer("nonContextualServer");
        WebArchive appNonContextual = ShrinkWrap.create(WebArchive.class, "appNonContextual.war")
                        .addClass("test.non.contextual.Foo")
                        .addClass("test.non.contextual.TestServlet")
                        .addClass("test.non.contextual.Baz")
                        .addClass("test.non.contextual.NonContextualBean")
                        .addClass("test.non.contextual.Bar")
                        .add(new FileAsset(new File("test-applications/appNonContextual.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        server.setMarkToEndOfLog(server.getDefaultLogFile());
        ShrinkHelper.exportDropinAppToServer(server, appNonContextual);
        assertNotNull("appNonContextual started or updated message", server.waitForStringInLogUsingMark("CWWKZ000[13]I.*appNonContextual"));
    }

    @Test
    public void testAppServlet() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();

        WebResponse response = browser.request(createURL("/appNonContextual/test"));
        Assert.assertEquals("Wrong status", "PASSED", response.getResponseBody());
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
