/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE10_OR_LATER_FEATURES;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/*
 * Tests for configuration through META-INF/services/javax.faces.application.ApplicationConfigurationPopulator  (APC)
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22AppConfigPopTests {

    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22AppConfigPop";

    protected static final Class<?> c = JSF22AppConfigPopTests.class;

    @Server("jsfTestServer2")
    public static LibertyServer jsfTestServer2;

    @BeforeClass
    public static void setup() throws Exception {

        JavaArchive jar = ShrinkHelper.buildJavaArchive("JSF22AppConfigPop.jar", "com.ibm.ws.jsf22.fat.appconfigpop.jar");

        WebArchive war = ShrinkHelper.buildDefaultApp("JSF22AppConfigPop.war", "com.ibm.ws.jsf22.fat.appconfigpop");

        war.addAsLibraries(jar);

        ShrinkHelper.exportDropinAppToServer(jsfTestServer2, war);

        jsfTestServer2.startServer(c.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer2 != null && jsfTestServer2.isStarted()) {
            jsfTestServer2.stopServer();
        }
    }

    protected void verifyResponse(String contextRoot, String resource, String expectedResponse, LibertyServer server) throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(server, contextRoot, resource);
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail(resource + " did not render properly.");
            }

            if (!page.asText().contains(expectedResponse)) {
                Assert.fail("The page did not contain the following expected response: " + expectedResponse);
            }
        }

    }

    protected void verifyResponse(String contextRoot, String resource, LibertyServer server, String... expectedResponseStrings) throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(server, contextRoot, resource);
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail(resource + " did not render properly.");
            }

            for (String expectedResponse : expectedResponseStrings) {
                if (!page.asText().contains(expectedResponse)) {
                    Assert.fail("The page did not contain the following expected response: " + expectedResponse);
                }
            }
        }
    }

    protected void verifyStatusCode(String contextRoot, String resource, int statusCode, LibertyServer server) throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(server, contextRoot, resource);
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail(resource + " did not render properly.");
            }
        }
    }

    /**
     * Simple managed bean test. First tests basic managed bean creation in APC, second one verifies modifying an configuration in APC does not overwrite value specified in
     * faces-config.xml
     *
     * @throws Exception
     */
    @Test
    // Faces ManagedBeans are not supported in Faces 4.0.
    @SkipForRepeat(EE10_OR_LATER_FEATURES)
    public void testAppPopConfiguredSimpleBean() throws Exception {

        this.verifyResponse(contextRoot, "AddedBean.jsf", jsfTestServer2, "SuccessfulAddedBeanTest");

        this.verifyResponse(contextRoot, "ExistingBean.jsf", jsfTestServer2, "SuccessfulExistingBeanTest");

    }

    /**
     * Tests basic managed property in managed bean defined through APC.
     *
     * @throws Exception
     */
    @Test
    // Faces ManagedBeans/managed-property are not supported in Faces 4.0.
    @SkipForRepeat(EE10_OR_LATER_FEATURES)
    public void testAppPopConfiguredMPBean() throws Exception {

        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "MPBean.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the page initially renders correctly
            if (page == null) {
                Assert.fail("JSF22AppConfigPop/MPBean.jsf did not render properly.");
            }

            if (!page.asText().contains("SuccessfulAddedBeanTest")) {
                Assert.fail("MPBean did not get successful message from AddedBean");
            }

            if (!page.asText().contains("SuccessfulValTest")) {
                Assert.fail("MPBean did not get successful message from val");
            }

            if (!page.asText().contains("Successfultrue")) {
                Assert.fail("MPBean did not get successful message resource injected boolean val.");
            }

            if (!page.asText().contains("MPSuccessfulAddedBeanTest")) {
                Assert.fail("MPBean did not get successful message from resource injected addedbean val");
            }
        }
    }

    /**
     * Test navigation rule defined through APC.
     *
     * @throws Exception
     */
    @Test
    public void testACPNavigationRule() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "nav.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Make sure the page initially renders correctly
            if (page == null) {
                Assert.fail("JSF22AppConfigPop/nav.jsf did not render properly.");
            }
            assertTrue(page.asText().contains("This page verifies Application Configuration Populator changes take effect through a navigation rule."));

            // Click the commandButton to execute the methods and update the page
            HtmlElement button = (HtmlElement) page.getElementById("BasicNavTest:navButton");
            page = button.click();

            if (!page.asText().contains("SUCCESS")) {
                Assert.fail("ACP rule did not take effect and we did not navigate to results.jsf");
            }
        }
    }

    /**
     * Test for APC defined phase listener output in logs. This APC is configured through jar file meta-inf/services.
     *
     * @throws Exception
     */
    @Test
    public void testAppPopConfiguredPhaseListener() throws Exception {
        if (JakartaEEAction.isEE10OrLaterActive()) {
            // Drive a request to a Facelet to verify the PhaseListener.
            this.verifyResponse(contextRoot, "simpleView.jsf", jsfTestServer2, "Hello from simpleView.xhtml!");
        } else {
            this.verifyResponse(contextRoot, "AddedBean.jsf", jsfTestServer2, "SuccessfulAddedBeanTest");
        }

        String msg = "JSF22:ACP beforePhase called.";
        assertTrue(jsfTestServer2.findStringsInLogs(msg).size() > 0);
    }

    /**
     * Test for APC defined system event listener output in logs. This APC is configured through jar file meta-inf/services.
     *
     * @throws Exception
     */
    @Test
    public void testAppPopConfiguredSystemEventListener() throws Exception {
        if (JakartaEEAction.isEE10OrLaterActive()) {
            // Drive a request to a Facelet to verify the SystemEventListener.
            this.verifyResponse(contextRoot, "simpleView.jsf", jsfTestServer2, "Hello from simpleView.xhtml!");
        } else {
            this.verifyResponse(contextRoot, "AddedBean.jsf", jsfTestServer2, "SuccessfulAddedBeanTest");
        }

        String msg = "JSF22:  AOP System event listener called.";
        assertTrue(jsfTestServer2.findStringsInLogs(msg).size() > 0);
    }

}
