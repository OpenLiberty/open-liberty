/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
@SkipForRepeat(NO_MODIFICATION)
@RunWith(FATRunner.class)
public class HibernateSearchTest extends LoggingTest {

    private static LibertyServer server;

    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive hibernateSearchTest = ShrinkWrap.create(WebArchive.class, "hibernateSearchTest.war")
                        .addPackages(true, "cdi.hibernate.test")
                        .add(new FileAsset(new File("test-applications/hibernateSearchTest.war/resources/WEB-INF/classes/META-INF/persistence.xml")), "/WEB-INF/classes/META-INF/persistence.xml")
                        .add(new FileAsset(new File("test-applications/hibernateSearchTest.war/resources/WEB-INF/classes/META-INF/jpaorm.xml")), "/WEB-INF/classes/META-INF/jpaorm.xml");

        server = LibertyServerFactory.getLibertyServer("cdi20HibernateSearchServer");
        ShrinkHelper.exportAppToServer(server, hibernateSearchTest);
        server.startServer();
        server.waitForStringInLogUsingMark("CWWKZ0001I.*Application hibernateSearchTest.war started");
    }

    @Test
    public void testHibernateSearch() throws Exception {
        WebBrowser browser = createWebBrowserForTestCase();
        String url = createURL("/hibernateSearchTest/SimpleTestServlet");
        WebResponse response = browser.request(url);
        String body = response.getResponseBody();
        Assert.assertTrue("Could not find \"field bridge called: true\" in: " + body, body.contains("field bridge called: true"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    private String createURL(String path) {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path;
    }

}
