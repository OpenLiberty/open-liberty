/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import java.io.File;
import java.net.MalformedURLException;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.ShrinkWrapSharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES }) // Skipped temporarily to test PassivationBeanTests for sessionDatabase-1.0 feature
public class SessionDestroyTests extends LoggingTest {

    @ClassRule
    public static ShrinkWrapSharedServer SHARED_SERVER = new ShrinkWrapSharedServer("cdi12SessionInvalidationServer");

    private static LibertyServer server;
    {
        server = SHARED_SERVER.getLibertyServer();
    }

    private final String expectedResults = "session created: true session destroyed: true";

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShrinkWrapSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BuildShrinkWrap
    public static Archive buildShrinkWrap() {
        return ShrinkWrap.create(WebArchive.class,
                                 "WebListener.war").addPackage("com.ibm.ws.cdi.test.session.destroy").add(new FileAsset(new File("test-applications/WebListener.war/resources/META-INF/permissions.xml")),
                                                                                                          "/META-INF/permissions.xml").add(new FileAsset(new File("test-applications/WebListener.war/resources/WEB-INF/beans.xml")),
                                                                                                                                           "/WEB-INF/beans.xml");
    }

    @Test
    public void testInvalidation() throws Exception {

        WebBrowser wb = this.createWebBrowserForTestCase();
        wb.request(createURL("/InvalidateServlet"));
        WebResponse webResponse = wb.request(createURL("/ResultsServlet"));

        Assert.assertTrue("expected " + "Invalidate Session - " + expectedResults + " but saw " + webResponse.getResponseBody(),
                          webResponse.getResponseBody().contains("Invalidate Session - " + expectedResults));

    }

    @Test
    public void testTimeout() throws Exception {

        WebBrowser wb = this.createWebBrowserForTestCase();
        wb.request(createURL("/TimeoutServlet"));
        Thread.sleep(3000);
        wb.request(createURL("/ResultsServlet")); //poke it a second time to ensure that the timeout is processed.
        WebResponse webResponse = wb.request(createURL("/ResultsServlet"));

        Assert.assertTrue("expected " + "Timeout Session - " + expectedResults + " but saw " + webResponse.getResponseBody(),
                          webResponse.getResponseBody().contains("Timeout Session - " + expectedResults));

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
