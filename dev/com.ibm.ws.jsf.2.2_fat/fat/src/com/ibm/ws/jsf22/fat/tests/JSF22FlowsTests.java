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

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * A collection of tests for the JSF 2.2 Faces Flows feature
 * All of these tests make use of the declarative facilities provided by JSF.
 *
 * Flows features tested include:
 * faces-config.xml configuration
 * *-flow.xml configuration
 * switches
 * parameters, nested flows
 * JAR packaging
 * explicit navigation cases
 *
 * @author Bill Lucy
 */

@RunWith(FATRunner.class)
public class JSF22FlowsTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22FacesFlows";

    protected static final Class<?> c = JSF22FlowsTests.class;

    @Server("jsfFacesFlowsServer")
    public static LibertyServer jsfFacesFlowsServer;

    @BeforeClass
    public static void setup() throws Exception {
        JavaArchive JSF22FacesFlowsJar = ShrinkHelper.buildJavaArchive("JSF22FacesFlows.jar", "");

        WebArchive JSF22FacesFlowsWar = ShrinkHelper.buildDefaultApp("JSF22FacesFlows.war", "com.ibm.ws.jsf22.fat.flows.beans");

        JSF22FacesFlowsWar.addAsLibraries(JSF22FacesFlowsJar);

        ShrinkHelper.exportDropinAppToServer(jsfFacesFlowsServer, JSF22FacesFlowsWar);

        jsfFacesFlowsServer.startServer(JSF22FlowsTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsfFacesFlowsServer != null && jsfFacesFlowsServer.isStarted()) {
            jsfFacesFlowsServer.stopServer();
        }
    }

    /**
     * Verify the behavior of a simple flow which is defined via a *-flow.xml configuration
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestSimple() throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfFacesFlowsServer, contextRoot, "");
        testSimpleCase("simple", url);
    }

    /**
     * Verify the behavior of a simple flow which is defined via a faces-config.xml configuration
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestSimpleFacesConfig() throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfFacesFlowsServer, contextRoot, "");
        testSimpleCase("simpleFacesConfig", url);
    }

    /**
     * Verify the behavior of a simple flow which is defined via a faces-config.xml configuration and
     * packaged in a JAR
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestSimpleJar() throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfFacesFlowsServer, contextRoot, "");
        testSimpleCase("simple-jar", url);
    }

    /**
     * Check that arbitrary flow nodes can't be accessed
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestFailedFlowEntry() throws Exception {
        // Navigate to the failed flow entry page
        try (WebClient webClient = getWebClient()) {

            URL url = JSFUtils.createHttpUrl(jsfFacesFlowsServer, contextRoot, "JSF22Flows_noAccess.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            assertNotInFlow(page);

            // Try navigating directly to the second page in an application-local flow
            page = findAndClickButton(page, "button1");
            assertNotInFlow(page);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("No navigation case match"));
        }
    }

    /**
     * Verify the behavior of a simple flow which is defined via *-flow.xml, and which employs navigation rules
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestDeclarativeNavigation() throws Exception {
        // Navigate to the
        try (WebClient webClient = getWebClient()) {
            URL url = JSFUtils.createHttpUrl(jsfFacesFlowsServer, contextRoot, "");
            HtmlPage page = getIndex(webClient, url);

            String flowID = "simpleNavigationDeclarative";

            /*
             * Enter flow, submit a flowScope variable, navigate back and update it, then exit flow
             */
            page = findAndClickButton(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Declarative Navigation Flow Example Page 1"));
            assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: no flowscope value"));

            // Assign flowscope value
            HtmlInput inputField = (HtmlInput) page.getElementById("inputValue");
            inputField.setValueAttribute("test string");

            // Navigate to next page and check flowScope value
            page = findAndClickButton(page, "button1");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Declarative Navigation Flow Example Page 2"));
            assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: test string"));

            page = findAndClickButton(page, "button1");

            // Update flowScope value
            inputField = (HtmlInput) page.getElementById("inputValue");
            inputField.setValueAttribute("another test string");

            // Navigate to next page and verify updated flowScope value
            page = findAndClickButton(page, "button1");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Declarative Navigation Flow Example Page 2"));
            assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: another test string"));

            // Exit flow, verify exit
            page = findAndClickButton(page, "button2");
            assertNotInFlow(page);
        }
    }

    /**
     * Verify the behavior of a simple flow which utilizes a switch to test navigation outcomes
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestDeclarativeSwitch() throws Exception {
        URL url = JSFUtils.createHttpUrl(jsfFacesFlowsServer, contextRoot, "");
        testFlowSwitch("declarativeSwitch", url);
    }

    /**
     * Tests that JSF flow switches are working correctly. Utilizes a switch to test a navigation outcome.
     * Accepts a string for the flow ID.
     */
    protected static void testFlowSwitch(String flowID, URL url) throws Exception {
        // Navigate to the index
        try (WebClient webClient = getWebClient()) {
            HtmlPage page = getIndex(webClient, url);
            //String flowID = "declarativeSwitch";

            /*
             * Enter flow, submit a flowScope variable that doesn't, then does, satisfy the switch, then exit
             */
            page = findAndClickButton(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Declarative Switch Page 1"));
            assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: no flowscope value"));

            // Assign flowscope value
            HtmlInput inputField = (HtmlInput) page.getElementById("inputValue");
            inputField.setValueAttribute("incorrect value");

            // Click submit: we should stay on the same page - the switch only allows us to
            // navigate to the next page when the flowScope value is "next"
            page = findAndClickButton(page, "button1");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Declarative Switch Page 1"));
            assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: incorrect value"));

            // Assign flowscope value
            inputField = (HtmlInput) page.getElementById("inputValue");
            inputField.setValueAttribute("next");

            // Click submit: since the flowScope value is "next" we should navigate to page 2
            page = findAndClickButton(page, "button1");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Declarative Switch Page 2"));
            assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: next"));

            // Exit flow, verify exit
            page = findAndClickButton(page, "button2");
            assertNotInFlow(page);
        }
    }

    /**
     * Verify the behavior of a pair of flows, in order to evaluate nested flow functionality.
     * Additionally tests that declarative parameters are working.
     *
     * We also confirm that the initializer on the InitializerBean is being called w/ the proper
     * explicitly-defined inbound parameters. This test is related to this issue --->
     * https://issues.apache.org/jira/browse/MYFACES-3969 (Also see Defect 169488)
     *
     * @throws Exception
     */
    @Test
    public void JSF22Flows_TestDeclarativeNestedFlows() throws Exception {
        // Navigate to the index
        URL url = JSFUtils.createHttpUrl(jsfFacesFlowsServer, contextRoot, "");
        testNestedFlows("declarativeNested1", "declarativeNested2", "declarativeNested", url);
    }

    /**
     * Helper method to verify that nested flows are working correctly.
     */
    protected static void testNestedFlows(String flowID1, String flowID2, String ButtonID, URL url) throws Exception {
        // Navigate to the index
        try (WebClient webClient = getWebClient()) {
            HtmlPage page = getIndex(webClient, url);

            /*
             * Navigate into flow 1, verify a parameter is passed to flow 2, return to flow 1 and update
             * the parameter, verify that this updated value is passed to flow 2, then exit
             */
            page = findAndClickButton(page, ButtonID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Nested Flow Example Page 1"));
            assertInFlow(page, flowID1);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: no flowscope value"));

            // Assign flowscope value and navigate to page 2 in the first flow
            HtmlInput inputField = (HtmlInput) page.getElementById("inputValue");
            inputField.setValueAttribute("test string");

            page = findAndClickButton(page, "button1");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Nested Flow Example Page 2"));
            assertInFlow(page, flowID1);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: test string"));

            // Navigate into flow 2, check passed parameter value
            page = findAndClickButton(page, "button2");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Nested Flow Example Page 1 (flow 2)"));
            assertInFlow(page, flowID2);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: test string"));

            // Navigate back into flow 1 page 1
            page = findAndClickButton(page, "button2");
            page = findAndClickButton(page, "button1");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Nested Flow Example Page 1"));
            assertInFlow(page, flowID1);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: test string"));

            // Assign a new flowscope value, make sure it propagates to flow 2
            inputField = (HtmlInput) page.getElementById("inputValue");
            inputField.setValueAttribute("another test string");

            page = findAndClickButton(page, "button1");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Nested Flow Example Page 2"));
            assertInFlow(page, flowID1);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: another test string"));

            // Navigate into flow 2, check passed parameter value
            page = findAndClickButton(page, "button2");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Nested Flow Example Page 1 (flow 2)"));
            assertInFlow(page, flowID2);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: another test string"));

            // Navigate into flow 2 page 2, check passed parameter value
            page = findAndClickButton(page, "button1");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Nested Flow Example Page 2 (flow 2)"));
            assertInFlow(page, flowID2);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: another test string"));

            // Navigate back to page 1
            page = findAndClickButton(page, "button2");

            // Exit flows
            page = findAndClickButton(page, "button3");
            assertNotInFlow(page);
        }
    }

    /**
     * Verify the behavior of a simple flow; the flow ID must be passed in, allowing re-use
     *
     * @throws Exception
     */
    protected static void testSimpleCase(String flowID, URL url) throws Exception {
        try (WebClient webClient = getWebClient()) {

            HtmlPage page = getIndex(webClient, url);

            /*
             * 1: Enter flow, then verify exit
             */
            page = findAndClickButton(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("flow page 1"));
            assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Flow Id: " + flowID));
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: no flowscope value"));

            // Exit flow, verify exit
            page = findAndClickButton(page, "button2");
            assertNotInFlow(page);

            /*
             * 2: Enter flow, assign flowScope a value, check for that value in the second flow page,
             * return to the first flow page, update the flowScope value, return to page 2 and
             * check for the updated value, then exit the flow and return to the index
             */
            page = findAndClickButton(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("flow page 1"));
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains(flowID));
            assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: no flowscope value"));

            // Assign flowscope value
            HtmlInput inputField = (HtmlInput) page.getElementById("inputValue");
            inputField.setValueAttribute("test string");

            // Navigate to next page and check flowScope value
            page = findAndClickButton(page, "button1");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("flow page 2"));
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains(flowID));
            assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: test string"));
            page = findAndClickButton(page, "button1");

            // Update flowScope value
            inputField = (HtmlInput) page.getElementById("inputValue");
            inputField.setValueAttribute("another test string");

            // Navigate to next page and verify updated flowScope value
            page = findAndClickButton(page, "button1");
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("flow page 2"));
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains(flowID));
            assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: another test string"));

            // Exit flow, verify exit
            HtmlElement nextButton = (HtmlElement) page.getElementById("button2");
            page = nextButton.click();
            assertNotInFlow(page);

            nextButton = (HtmlElement) page.getElementById("button1");
            page = nextButton.click();
            assertNotInFlow(page);

            /*
             * 3: Verify that the flowScope value is not still set upon flow re-entry
             */
            page = findAndClickButton(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("flow page 1"));
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains(flowID));
            assertInFlow(page, flowID);
            assertTrue("The page doesn't contain the right text: " + page.asText(),
                       page.asText().contains("Current flowscope value: no flowscope value"));

            // Exit flow, verify exit
            page = findAndClickButton(page, "button2");
            assertNotInFlow(page);
        }
    }

    /**
     * Set up a new WebClient object with a set options/properties
     */
    protected static WebClient getWebClient() {
        WebClient wc = new WebClient(BrowserVersion.CHROME);
        // set any options here

        return wc;
    }

    /**
     * Helper method to check if we're in a flow.
     * Dependent on the format of the text output on the test pages
     */
    protected static void assertInFlow(HtmlPage page, String flowID) {
        assertTrue("Currently in a flow when we shouldn't be: " + page.asText(),
                   page.asText().contains("In flow ? true"));
        assertTrue("A flow ID is set when it shouldn't be: " + page.asText(),
                   page.asText().contains("Flow Id: " + flowID));
    }

    /**
     * Helper method to check if we're in a flow.
     * Dependent on the format of the text output on the test pages
     */
    protected static void assertNotInFlow(HtmlPage page) {
        assertTrue("Currently in a flow when we shouldn't be: " + page.asText(),
                   page.asText().contains("In flow ? false"));
        assertTrue("A flow ID is set when it shouldn't be: " + page.asText(),
                   page.asText().contains("Flow Id: no flow ID"));
    }

    /**
     * Return the test application index; make sure it's set up properly (not in a flow, etc.)
     */
    protected static HtmlPage getIndex(WebClient wc, URL url) throws Exception {
        HtmlPage page = (HtmlPage) wc.getPage(url);

        if (page == null) {
            Assert.fail("JSF22Flows_index.xhtml did not render properly.");
        }
        assertNotInFlow(page);
        return page;
    }

    /**
     * Helper method to find a button on the page, click it, and return the resulting page
     */
    protected static HtmlPage findAndClickButton(HtmlPage page, String button) throws Exception {
        return ((HtmlElement) page.getElementById(button)).click();
    }
}
