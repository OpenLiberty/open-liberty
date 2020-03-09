/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.net.MalformedURLException;
import java.io.File;

import com.ibm.websphere.simplicity.ShrinkHelper;

import com.ibm.ws.fat.util.BuildShrinkWrap;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebResponse;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.ServerRules;
import componenttest.rules.TestRules;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests for <code>@WithAnnotations</code> used in portable extensions for observing type discovery of beans with certain annotations.
 */
@Mode(TestMode.FULL)
public class ProducesTest extends LoggingTest {
    private static final LibertyServer server = LibertyServerFactory.getLibertyServer("cdi12BasicServer");
    private static boolean hasSetUp = false;

    @BeforeClass
    public static void setUp() throws Exception {
 
        if (hasSetUp) {
            return;
        }


        WebArchive war = ShrinkWrap.create(WebArchive.class, "DepProducerApp.war")
                        .addPackages(true, "com.ibm.ws.cdi.test.dependentscopedproducer")
                        .add(new FileAsset(new File("test-applications/DepProducerApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/DepProducerApp.war/resources/WEB-INF/ibm-web-ext.xml")), "/WEB-INF/ibm-web-ext.xml");

        ShrinkHelper.exportDropinAppToServer(server, war);
        hasSetUp = true;
        Assert.assertNotNull(
            "Message was not detected in the log",
            server.waitForStringInLogUsingMark("CWWKZ0001I"));

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return null;
    }

    @ClassRule
    public static final TestRule startAndStopServerRule = ServerRules.startAndStopAutomatically(server);

    @Test
    public void testDependentScopedProducerHandlesNullCorrectly() throws Exception {
  
        WebBrowser browser = createWebBrowserForTestCase();

        WebResponse response = browser.request(createURL("/DepProducerApp/"));
        String body = response.getResponseBody();
        Assert.assertTrue("Test failed: " + body, body.contains("Test Passed"));

        response = browser.request(createURL("/DepProducerApp/failAppScopedMethod"));
        body = response.getResponseBody();
        Assert.assertTrue("A null was illegally injected or the producer didn't fire: " + body, body.contains("Test Passed"));

        response = browser.request(createURL("/DepProducerApp/failAppSteryotypedMethod"));
        body = response.getResponseBody();
        Assert.assertTrue("A null was illegally injected or the producer didn't fire: " + body, body.contains("Test Passed"));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    private String createURL(String path) throws MalformedURLException {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path;
    }

}
