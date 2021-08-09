/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat.tests;

import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf.container.fat.FATSuite;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
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
public class JSF22FlowsTests extends FATServletClient {

    private static final String MOJARRA_APP = "JSF22FacesFlows";
    private static final String MYFACES_APP = "JSF22FacesFlows_MyFaces";

    public static LibertyServer server = LibertyServerFactory.getLibertyServer("jsf.container.2.2_fat");

    @BeforeClass
    public static void setup() throws Exception {
        server.removeAllInstalledAppsForValidation();

        JavaArchive facesFlowJar = ShrinkWrap.create(JavaArchive.class, "JSF22FacesFlows.jar");
        facesFlowJar = (JavaArchive) ShrinkHelper.addDirectory(facesFlowJar, "test-applications/JSF22FacesFlows/resources/jar");

        WebArchive mojarraApp = ShrinkWrap.create(WebArchive.class, MOJARRA_APP + ".war")
                        .addAsLibrary(facesFlowJar)
                        .addPackage("jsf.flow.beans");
        mojarraApp = FATSuite.addMojarra(mojarraApp);
        mojarraApp = (WebArchive) ShrinkHelper.addDirectory(mojarraApp, "test-applications/JSF22FacesFlows/resources/war");
        ShrinkHelper.exportToServer(server, "dropins", mojarraApp);
        server.addInstalledAppForValidation(MOJARRA_APP);

        WebArchive myfacesApp = ShrinkWrap.create(WebArchive.class, MYFACES_APP + ".war")
                        .addAsLibrary(facesFlowJar)
                        .addPackage("jsf.flow.beans");
        myfacesApp = FATSuite.addMyFaces(myfacesApp);
        myfacesApp = (WebArchive) ShrinkHelper.addDirectory(myfacesApp, "test-applications/JSF22FacesFlows/resources/war");
        ShrinkHelper.exportToServer(server, "dropins", myfacesApp);
        server.addInstalledAppForValidation(MYFACES_APP);

        server.startServer(JSF22FlowsTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Ensure that each app is running with the appropriate provider so that
     * we don't accidentally run both apps with a single provider.
     */
    @Test
    public void verifyAppProviders() throws Exception {
        server.resetLogMarks();
        server.waitForStringInLogUsingMark("Initializing Mojarra .* for context '/" + MOJARRA_APP + "'");
        // Since MyFaces doesn't output any initialization messages that contain the app name,
        // all we can do is check to make sure the MyFaces app didn't initialize with Mojarra
        Assert.assertEquals(0, server.findStringsInLogs("Initializing Mojarra .* for context '/" + MYFACES_APP + "'").size());
    }

    /**
     * Verify the behavior of a simple flow which is defined via a *-flow.xml configuration
     */
    @Test
    public void JSF22Flows_TestSimple_Mojarra() throws Exception {
        testSimpleCase("simple", MOJARRA_APP);
    }

    @Test
    public void JSF22Flows_TestSimple_MyFaces() throws Exception {
        testSimpleCase("simple", MYFACES_APP);
    }

    /**
     * Verify the behavior of a simple flow which is defined via a faces-config.xml configuration
     */
    @Test
    public void JSF22Flows_TestSimpleFacesConfig_Mojarra() throws Exception {
        testSimpleCase("simpleFacesConfig", MOJARRA_APP);
    }

    @Test
    public void JSF22Flows_TestSimpleFacesConfig_MyFaces() throws Exception {
        testSimpleCase("simpleFacesConfig", MYFACES_APP);
    }

    /**
     * Verify the behavior of a simple flow which is defined via a faces-config.xml configuration and
     * packaged in a JAR
     */
    @Test
    @ExpectedFFDC("java.util.NoSuchElementException")
    public void JSF22Flows_TestSimpleJar_Mojarra() throws Exception {
        testSimpleCase("simple-jar", MOJARRA_APP);
    }

    @Test
    public void JSF22Flows_TestSimpleJar_MyFaces() throws Exception {
        testSimpleCase("simple-jar", MYFACES_APP);
    }

    /**
     * Check that arbitrary flow nodes can't be accessed
     */
    @Test
    @ExpectedFFDC("java.util.NoSuchElementException")
    public void JSF22Flows_TestFailedFlowEntry_Mojarra() throws Exception {
        // Navigate to the failed flow entry page
        WebClient webClient = getWebClient();
        HtmlPage page = webClient.getPage(getServerURL() + '/' + MOJARRA_APP + "/JSF22Flows_noAccess.xhtml");
        assertNotInFlow(page);

        // Try navigating directly to the second page in an application-local flow
        page = findAndClickButton(page, "button1");
        assertNotInFlow(page);

        // MyFaces has slightly different err msg than Mojarra
        assertTrue("The page doesn't contain the right text: " + page.asText(),
                   page.asText().contains("Unable to find matching navigation case"));
    }

    @Test
    public void JSF22Flows_TestFailedFlowEntry_MyFaces() throws Exception {
        // Navigate to the failed flow entry page
        WebClient webClient = getWebClient();
        HtmlPage page = webClient.getPage(getServerURL() + '/' + MYFACES_APP + "/JSF22Flows_noAccess.xhtml");
        assertNotInFlow(page);

        // Try navigating directly to the second page in an application-local flow
        page = findAndClickButton(page, "button1");
        assertNotInFlow(page);

        // MyFaces has slightly different err msg than Mojarra
        assertTrue("The page doesn't contain the right text: " + page.asText(),
                   page.asText().contains("No navigation case match"));
    }

    /**
     * Verify the behavior of a simple flow which is defined via *-flow.xml, and which employs navigation rules
     */
    @Test
    public void JSF22Flows_TestDeclarativeNavigation_Mojarra() throws Exception {
        JSF22Flows_TestDeclarativeNavigation(MOJARRA_APP);
    }

    @Test
    public void JSF22Flows_TestDeclarativeNavigation_MyFaces() throws Exception {
        JSF22Flows_TestDeclarativeNavigation(MYFACES_APP);
    }

    public void JSF22Flows_TestDeclarativeNavigation(String app) throws Exception {
        // Navigate to the
        WebClient webClient = getWebClient();
        HtmlPage page = getIndex(webClient, app);

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

    /**
     * Verify the behavior of a simple flow which utilizes a switch to test navigation outcomes
     */
    @Test
    public void JSF22Flows_TestDeclarativeSwitch_Mojarra() throws Exception {
        testFlowSwitch("declarativeSwitch", MOJARRA_APP);
    }

    @Test
    public void JSF22Flows_TestDeclarativeSwitch_MyFaces() throws Exception {
        testFlowSwitch("declarativeSwitch", MYFACES_APP);
    }

    /**
     * Tests that JSF flow switches are working correctly. Utilizes a switch to test a navigation outcome.
     * Accepts a string for the flow ID.
     */
    protected static void testFlowSwitch(String flowID, String contextRoot) throws Exception {
        // Navigate to the index
        WebClient webClient = getWebClient();
        HtmlPage page = getIndex(webClient, contextRoot);
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

    /**
     * Verify the behavior of a pair of flows, in order to evaluate nested flow functionality.
     * Additionally tests that declarative parameters are working.
     *
     * We also confirm that the initializer on the InitializerBean is being called w/ the proper
     * explicitly-defined inbound parameters. This test is related to this issue --->
     * https://issues.apache.org/jira/browse/MYFACES-3969 (Also see Defect 169488)
     */
    @Test
    public void JSF22Flows_TestDeclarativeNestedFlows_Mojarra() throws Exception {
        // Navigate to the index
        testNestedFlows("declarativeNested1", "declarativeNested2", "declarativeNested", MOJARRA_APP);
    }

    @Test
    public void JSF22Flows_TestDeclarativeNestedFlows_MyFaces() throws Exception {
        // Navigate to the index
        testNestedFlows("declarativeNested1", "declarativeNested2", "declarativeNested", MYFACES_APP);
    }

    /**
     * Helper method to verify that nested flows are working correctly.
     */
    protected static void testNestedFlows(String flowID1, String flowID2, String ButtonID, String contextRoot) throws Exception {
        // Navigate to the index
        WebClient webClient = getWebClient();
        HtmlPage page = getIndex(webClient, contextRoot);

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

    /**
     * Verify the behavior of a simple flow; the flow ID must be passed in, allowing re-use
     */
    protected static void testSimpleCase(String flowID, String appName) throws Exception {
        WebClient webClient = getWebClient();
        HtmlPage page = getIndex(webClient, appName);

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

    /**
     * Set up a new WebClient object with a set options/properties
     */
    public static WebClient getWebClient() {
        WebClient wc = new WebClient(BrowserVersion.CHROME);
        // set any options here

        return wc;
    }

    /**
     * Helper method to check if we're in a flow.
     * Dependent on the format of the text output on the test pages
     */
    public static void assertInFlow(HtmlPage page, String flowID) {
        assertTrue("Currently in a flow when we shouldn't be: " + page.asText(),
                   page.asText().contains("In flow ? true"));
        assertTrue("A flow ID is set when it shouldn't be: " + page.asText(),
                   page.asText().contains("Flow Id: " + flowID));
    }

    /**
     * Helper method to check if we're in a flow.
     * Dependent on the format of the text output on the test pages
     */
    public static void assertNotInFlow(HtmlPage page) {
        assertTrue("Currently in a flow when we shouldn't be: " + page.asText(),
                   page.asText().contains("In flow ? false"));
        assertTrue("A flow ID is set when it shouldn't be: " + page.asText(),
                   page.asText().contains("Flow Id: no flow ID"));
    }

    /**
     * Helper method to find a button on the page, click it, and return the resulting page
     */
    public static HtmlPage findAndClickButton(HtmlPage page, String button) throws Exception {
        return ((HtmlElement) page.getElementById(button)).click();
    }

    /**
     * Return the test application index; make sure it's set up properly (not in a flow, etc.)
     */
    public static HtmlPage getIndex(WebClient wc, String appName) throws Exception {
        HtmlPage page = (HtmlPage) wc.getPage(getServerURL() + "/" + appName);
        if (page == null) {
            Assert.fail("JSF22Flows_index.xhtml did not render properly.");
        }
        assertNotInFlow(page);
        return page;
    }

    private static String getServerURL() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    }
}
