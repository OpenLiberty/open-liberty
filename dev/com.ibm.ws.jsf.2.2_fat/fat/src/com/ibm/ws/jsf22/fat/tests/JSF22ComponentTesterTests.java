/*
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat.tests;

import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Tests to execute on the jsfTestServer2 that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22ComponentTesterTests {
    @Rule
    public TestName name = new TestName();

    private static BrowserVersion browser = BrowserVersion.CHROME;

    String contextRoot = "JSF22ComponentTester";

    protected static final Class<?> c = JSF22ComponentTesterTests.class;

    @Server("jsfTestServer2")
    public static LibertyServer jsfTestServer2;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsfTestServer2, "JSF22ComponentTester.war", "com.ibm.ws.jsf22.fat.componenttester.*");

        jsfTestServer2.startServer(JSF22ComponentTesterTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer2 != null && jsfTestServer2.isStarted()) {
            jsfTestServer2.stopServer();
        }
    }

    /**
     * Check to make sure that a transient view renders with the correct viewstate value
     *
     * @throws Exception
     */
    @Test
    public void JSF22ComponentTester_TestMultiComponents() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("JSF22ComponentTester_TestMultiComponents.xhtml did not render properly.");
            }

            assertTrue(page.asText().contains("Collapsible panel test page"));
            assertTrue(page.asText().contains("This information is collapsible"));

            // Click the commandButton to execute the methods and update the page
            HtmlElement button = (HtmlElement) page.getElementById("collapsiblePanelForm:panel:toggle");
            page = button.click();

            assertTrue(!page.asText().contains("This information is collapsible"));
        }
    }

    /**
     * In order to reproduce the issue in MyFaces-3948 we drive a request to a page more than once.
     *
     * On the second request if the problem is not fixed an IndexOutOfBoundsException is thrown.
     *
     * If the problem is fixed then the page renders normally and no exception is thrown.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ComponentTester_Test_MyFaces_3948() throws Exception {
        try (WebClient webClient = new WebClient()) {

            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            // Drive the initial request.
            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "index.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Need to have a bit of time between requests to reproduce the issue
            Thread.sleep(1000);

            // Now drive the second request to the same page.
            page = (HtmlPage) webClient.getPage(url);

            // Ensure that we don't find an IndexOutOfBoundsException on the page
            assertTrue("A java.lang.IndexOutOfBoundsException was found on the page and it should not have been.",
                       !page.asText().contains("java.lang.IndexOutOfBoundsException: Index: 0, Size: 0"));
        }
    }

    /**
     * Check to make sure DocType is rendered properly when a JSF page is built from a template. This test
     * straight DOCTYPE elements.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ComponentTester_TestDocType() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "testDoctype.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("JSF22ComponentTester_TestDocType.xhtml did not render properly.");
            }

            assertTrue(page.getWebResponse().getContentAsString().contains("<!DOCTYPE html>"));
        }
    }

    /**
     * Check to make sure the <h:doctype> is rendered properly when a JSF page is built from a template.
     *
     * @throws Exception
     */
    @Test
    public void JSF22ComponentTester_TestDocTag() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "testDoctag.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("JSF22ComponentTester_TestDocTag.xhtml did not render properly.");
            }

            assertTrue(page.getWebResponse().getContentAsString().contains("<!DOCTYPE html>"));
        }
    }

    /**
     * This test is used to verify the fix provided by MYFACES-3960:
     * https://issues.apache.org/jira/browse/MYFACES-3960
     *
     * It is used to insure the proper ordering of an action vs. an ajax listener.
     * The proper order for this type of markup is ajax listener first then action:
     *
     * <h:commandButton value="GetResult" action="#{actionListenerBean.test('test action called')}">
     * <f:ajax listener="#{actionListenerBean.ajaxListener}"/>
     * </h:commandButton>
     */
    @Test
    public void JSF22ComponentTester_TestCommandButtonOrder() throws Exception {
        try (WebClient webClient = getWebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "testActionListenerOrder.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            System.out.print("JSF22ComponentTester_TestCommandButtonOrder: TOP");

            if (page == null) {
                Assert.fail("JSF22ComponentTester_TestDocTag.xhtml did not render properly.");
            }

            assertTrue(page.getWebResponse().getContentAsString().contains("Action-Listener order page"));

            // Click the commandButton to execute the methods and update the page
            HtmlElement button = (HtmlElement) page.getElementById("form:testButton");
            page = button.click();

            HtmlElement output = (HtmlElement) page.getElementById("testOutput");

            if (!output.asText().contains("test action called")) {
                Assert.fail("JSF22ComponentTester_TestCommandButtonOrder: test output is not correct: "
                            + output.asText());
            }
        }
    }

    /**
     * This test is used to verify the fix provided by MYFACES-3960:
     * https://issues.apache.org/jira/browse/MYFACES-3960
     *
     * It is used to insure the proper ordering of an action vs. an ajax listener.
     * The proper order for this type of markup is ajax listener first then action:
     *
     * <h:commandLink value="GetResult" action="#{actionListenerBean.test('test action called')}">
     * <f:ajax listener="#{actionListenerBean.ajaxListener}"/>
     * </h:commandLink>
     */
    @Test
    public void JSF22ComponentTester_TestCommandLinkOrder() throws Exception {
        try (WebClient webClient = getWebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "testActionListenerOrder.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("JSF22ComponentTester_TestDocTag.xhtml did not render properly.");
            }

            assertTrue(page.getWebResponse().getContentAsString().contains("Action-Listener order page"));

            // Click the link to execute the methods and update the page
            HtmlAnchor anchor = page.getAnchorByName("form:testLink");
            page = anchor.click();

            HtmlElement output = (HtmlElement) page.getElementById("testOutput");

            if (!output.asText().contains("test action called")) {
                Assert.fail("JSF22ComponentTester_TestCommandLinkOrder: test output is not correct: "
                            + output.asText());
            }
        }
    }

    /**
     * Instantiate a WebClient instance that plays nicely with modern web features
     */
    private WebClient getWebClient() {
        WebClient webClient = new WebClient(browser);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        return webClient;
    }
}
