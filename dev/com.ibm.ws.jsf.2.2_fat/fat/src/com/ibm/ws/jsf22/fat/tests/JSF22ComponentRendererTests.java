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

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
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
public class JSF22ComponentRendererTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22ComponentRenderer";

    protected static final Class<?> c = JSF22ComponentRendererTests.class;

    @Server("jsfTestServer2")
    public static LibertyServer jsfTestServer2;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive War = ShrinkHelper.defaultDropinApp(jsfTestServer2, "JSF22ComponentRenderer.war", "com.ibm.ws.jsf22.fat.componentrenderer.*");

        jsfTestServer2.startServer(JSF22ComponentRendererTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfTestServer2 != null && jsfTestServer2.isStarted()) {
            jsfTestServer2.stopServer();
        }
    }

    /**
     * jsf479 -- http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-479
     * UIData supports the Collection Interface rather than List.
     *
     * @throws Exception
     */
    @Test
    public void testJsf479() throws Exception {
        String methodName = "testJsf479";

        String testUrl = "JSF22ComponentRenderer/jsf479.xhtml";

        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "jsf479.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: " + testUrl);

            if (page == null) {
                Assert.fail(testUrl + " did not render properly.");
            }

            // If the page contains the string "Two Thing 9" then the collection was completely rendered.
            assertTrue(page.asText().contains("Two Thing 9"));
        }
    }

    /**
     * jsf 1134 -- http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1134
     *
     * Add the "role" pass through attribute.
     *
     * The resulting table should have a 'role' attribute on it.
     *
     * @throws Exception
     */
    @Test
    public void testJsf1134() throws Exception {
        String methodName = "testJsf1134";

        String testUrl = "/JSF22ComponentRenderer/jsf1134.xhtml";

        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "jsf1134.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: " + testUrl);

            if (page == null) {
                Assert.fail(testUrl + " did not render properly.");
            }

            DomElement element = page.getElementById("panel");
            Log.info(c, name.getMethodName(), "Test whether the role attribute was properly added to the <table> attribute");
            //Test that the placeholder pass through element was properly added as an attribute to <input>
            String roleAttr = element.getAttribute("role");
            Log.info(c, name.getMethodName(), "role attribute: " + roleAttr);
            Assert.assertEquals("jsf1134", roleAttr);
        }
    }

    /**
     * jsf 1019 -- http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1019
     * Modify spec for ResponseWriter.writeURIAttribute() to explicitly require adherence to the W3C URI spec
     *
     * NOTE: The jsf1019.xhtml file has a 'verbose' and a 'succinct' id. I think they both should replace all spaces with
     * %20 rather than a + sign, but the 'verbose' form replaces spaces to the right of the ? (parameters) with a + rather than
     * a %20.(to the left is replaces w/ %20) This is deemed OK for now because that is how the reference implementation does it.
     *
     * We are not testing the 'verbose' version here.
     *
     * @throws Exception
     */
    @Test
    public void testJsf1019() throws Exception {
        String methodName = "testJsf1019";

        String testUrl = "/JSF22ComponentRenderer/jsf1019.xhtml";

        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "jsf1019.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: " + testUrl);

            if (page == null) {
                Assert.fail(testUrl + " did not render properly.");
            }

            DomElement element = page.getElementById("succinct");
            Log.info(c, name.getMethodName(), "Test whether the spaces in the link were replaced w/ %20 ");
            // Test that the spaces were replaces w/ %20
            String hrefAttr = element.getAttribute("href");
            Log.info(c, name.getMethodName(), "href attribute: " + hrefAttr);
            Assert.assertEquals("link%20file%20with%20spaces?user%20name=This%20is%20a%20test", hrefAttr);
        }
    }

    /**
     * JSF 703 http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-703
     *
     * Make "value" optional for @FacesComponent.
     *
     * Creates a component using the annotation @FacesComponent without any arguments. Uses the component if it works.
     *
     *
     * @throws Exception
     */
    @Test
    public void testJsf703() throws Exception {
        String methodName = "testJsf703";

        String testUrl = "/JSF22ComponentRenderer/jsf703.xhtml";

        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "jsf703.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: " + testUrl);

            if (page == null) {
                Assert.fail(testUrl + " did not render properly.");
            }

            // If the page contains the string "JSF703 COMPONENT TEST" then the component was rendered correctly.
            assertTrue(page.asText().contains("JSF703 COMPONENT TEST"));
        }
    }

    /**
     * jsf 943 -- http://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-943
     *
     * This is just to make sure the javax.faces.view.ViewDeclarationLanguageWrapper class exists.
     *
     * @throws Exception
     */
    @Test
    public void testJsf943() throws Exception {
        String methodName = "testJsf943";

        String testUrl = "/JSF22ComponentRenderer/jsf943.xhtml";

        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "jsf943.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: " + testUrl);

            if (page == null) {
                Assert.fail(testUrl + " did not render properly.");
            }

            // If the page contains the string "Found Class true" then the class was found.
            assertTrue(page.asText().contains("Found Class true"));
        }
    }

    /**
     * jsf 997 -- https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-997
     *
     * This is a regression test to insure that the changes to support 997 did not break @ListenerFor.
     *
     * @throws Exception
     */
    @Test
    public void testJsf997() throws Exception {
        String testUrl = "/JSF22ComponentRenderer/jsf997.xhtml";

        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "jsf997.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: " + testUrl);

            assertTrue(page.asText().contains("PostAddToViewEvent"));

            HtmlSubmitInput button = (HtmlSubmitInput) page.getElementById("button");

            page = button.click();
            assertTrue(page.asText().contains("preValidateEvent"));
            assertTrue(page.asText().contains("postValidateEvent"));
        }
    }

    /**
     * jsf 599 -- https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-599
     *
     * This test the use of ViewDeclarationLanguage.createComponent() to create a child
     * of a parent composite component.
     *
     * @throws Exception
     */
    @Test
    public void testJsf599() throws Exception {
        try (WebClient webClient = new WebClient()) {

            String testUrl = "/JSF22ComponentRenderer/jsf599.xhtml";

            URL url = JSFUtils.createHttpUrl(jsfTestServer2, contextRoot, "jsf599.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), "Navigating to: " + testUrl);

            assertTrue(page.asText().contains("I'm a person!"));
            assertTrue(page.asText().contains("I'm a place!"));
        }
    }
}
