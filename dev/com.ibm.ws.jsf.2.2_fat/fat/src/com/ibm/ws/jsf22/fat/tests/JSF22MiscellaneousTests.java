/*
 * Copyright (c) 2015, 2019 IBM Corporation and others.
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

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf22.fat.JSFUtils;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import junit.framework.Assert;

/**
 * Tests to execute on the jsf22MiscellaneousServer that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22MiscellaneousTests {
    @Rule
    public TestName name = new TestName();

    String contextRoot = "JSF22Miscellaneous";

    protected static final Class<?> c = JSF22MiscellaneousTests.class;

    @Server("jsf22MiscellaneousServer")
    public static LibertyServer jsf22MiscellaneousServer;

    @BeforeClass
    public static void setup() throws Exception {

        JavaArchive JSF22MiscellaneousJar = ShrinkHelper.buildJavaArchive("JSF22Miscellaneous.jar", "com.ibm.ws.jsf22.fat.miscbean.jar");

        WebArchive JSF22MiscellaneousWar = ShrinkHelper.buildDefaultApp("JSF22Miscellaneous.war", "com.ibm.ws.jsf22.fat.miscbean");

        WebArchive SerializeWar = ShrinkHelper.buildDefaultApp("JSF22MiscellaneousSerialize.war", "");

        EnterpriseArchive JSF22MiscellaneousEar = ShrinkWrap.create(EnterpriseArchive.class, "JSF22Miscellaneous.ear");

        JSF22MiscellaneousWar.addAsLibraries(JSF22MiscellaneousJar);
        SerializeWar.addAsLibraries(JSF22MiscellaneousJar);
        JSF22MiscellaneousEar.addAsModule(JSF22MiscellaneousWar);
        JSF22MiscellaneousEar.addAsModule(SerializeWar);

        ShrinkHelper.addDirectory(JSF22MiscellaneousEar, "test-applications" + "/JSF22Miscellaneous.ear" + "/resources");

        ShrinkHelper.exportDropinAppToServer(jsf22MiscellaneousServer, JSF22MiscellaneousEar);

        jsf22MiscellaneousServer.startServer(JSF22MiscellaneousTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf22MiscellaneousServer != null && jsf22MiscellaneousServer.isStarted()) {
            jsf22MiscellaneousServer.stopServer();
        }
    }

    /**
     * Check to make sure that a simple page renders properly.
     * 
     * @throws Exception
     */
    @Test
    public void testSimple() throws Exception {
        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, contextRoot, "testSimple.xhtml");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        if (page == null) {
            Assert.fail("JSF22Miscellaneous_Simple.xhtml did not render properly.");
        }

        assertTrue(page.asText().contains("Testing JSF2.2 miscellaneous - this is test outputText"));
    }

    /**
     * Programmatically tests if the new JSF 2.2 API is working properly.
     * 
     * @throws Exception
     */
    @Test
    public void testAPI() throws Exception {
        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, contextRoot, "testAPI.xhtml");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("test API page was not available.");
        }

        assertTrue(page.asText().contains("Get information about the JSF 2.2 API"));

        // Click the button to execute the method and updates the page
        HtmlElement button = (HtmlElement) page.getElementById("button:test");
        page = button.click();

        HtmlElement output = (HtmlElement) page.getElementById("testOutput");

        // Look for the correct results
        if (!output.asText().contains("isSavingStateInClient = true")) {
            Assert.fail("Invalid response from server.  <isSavingStateInClient> is set incorrectly = " + output.asText());
        }
        if (!output.asText().contains("getApplicationContextPath = /JSF22Miscellaneous")) {
            Assert.fail("Invalid response from server.  <getApplicationContextPath> is set incorrectly = " + output.asText());
        }
        if (!output.asText().contains("isSecure = false")) {
            Assert.fail("Invalid response from server.  <isSecure> is set incorrectly = " + output.asText());
        }
        if (!output.asText().contains("getSessionMaxInactiveInterval = 10000")) {
            Assert.fail("Invalid response from server.  <getSessionMaxInactiveInterval> is set incorrectly = " + output.asText());
        }
        if (!output.asText().contains("setPartialRequest = true")) {
            Assert.fail("Invalid response from server.  <setPartialRequest> is set incorrectly = " + output.asText());
        }
        if (!output.asText().contains("getProtectedViewsUnmodifiable = 2")) {
            Assert.fail("Invalid response from server.  <getProtectedViewsUnmodifiable> is set incorrectly = " + output.asText());
        }
        if (!output.asText().contains("componentSystemEventChangesWorked = true")) {
            Assert.fail("Invalid response from server.  <componentSystemEventChangesWorked> is set incorrectly = " + output.asText());
        }
    }

    /**
     * Programmatically tests if the new JSF 2.2 resetValues function is working properly.
     * This is related to https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1129
     * 
     * @throws Exception
     */
    @Test
    public void testResetValues() throws Exception {
        WebClient webClient = new WebClient();
        // Use a synchronizing ajax controller to allow proper ajax updating
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());

        URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, contextRoot, "testResetValues.xhtml");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("test ResetValues page was not available.");
        }

        assertTrue(page.asText().contains("JSF 2.2 Test ResetValues"));

        // Fill in the FirstName, but not the LastName.   Click "Save", which should 
        //   respond w/ an error.    The first name and last name are both REQUIRED.
        //   Click "Reset" and this should reset the first name.    Prior to JSF2.2, the 
        //   first name still held the text.
        HtmlTextInput firstName = (HtmlTextInput) page.getElementById("form1:firstName");
        firstName.type("John", false, false, false);

        //  Save the form which should cause an error to be reported.
        HtmlElement button = (HtmlElement) page.getElementById("form1:save");
        page = button.click();

        if (!page.asText().contains("John")) {
            Assert.fail("Invalid response from server.  First Name is NOT set to <John>  = " + page.asXml());
        }

        //  Click "reset", which should remove "John" from the form.
        button = (HtmlElement) page.getElementById("form1:reset");
        page = button.click();

        // Look for the correct results.   The "John" text should not exist in the page.
        if (page.asText().contains("John")) {
            Assert.fail("Invalid response from server.  First Name is still set to <John>  = " + page.asXml());
        }
    }

    /**
     * Programmatically tests if the new JSF 2.2 CSRF functionality is working properly.
     * This is related to https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-869
     * 
     * @throws Exception
     */
    @Test
    public void testCSRF() throws Exception {
        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, contextRoot, "testCSRF.xhtml");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("test CSRF page was not available.");
        }

        assertTrue(page.asText().contains("JSF 2.2 CSRF Test Page"));

        // Click the commandButton to execute the method
        HtmlElement commandButton = (HtmlElement) page.getElementById("form1:button_postback");
        page = commandButton.click();

        // Look for the correct results
        if (!page.asText().contains("This is a protected page.")) {
            Assert.fail("Invalid response from server.  The protected page was not retrieved = " + page.asText());
        }

        page = (HtmlPage) webClient.getPage(url);
        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("test CSRF page was not available.");
        }
        assertTrue(page.asText().contains("JSF 2.2 CSRF Test Page"));

        // Click the button to execute the method
        HtmlElement button = (HtmlElement) page.getElementById("form1:button_non_postback");
        page = button.click();

        // Look for the correct results
        if (!page.asText().contains("This is a protected page.")) {
            Assert.fail("Invalid response from server.  The protected page was not retrieved = " + page.asText());
        }
        if (!page.getUrl().toString().contains("javax.faces.Token=")) {
            Assert.fail("Invalid response from server.  This page does NOT contain the token = " + page.asText());
        }

        //  Attempt to retrieve a page that is "protected".    This SHOULD result in a "500 Internal Server Error"
        try {
            webClient = new WebClient();
            // Turn off printing for this webClient.   We know there will very likely be an error.
            webClient.getOptions().setPrintContentOnFailingStatusCode(false);
            url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, contextRoot, "protectedPage.xhtml");
            webClient.getPage(url);
            Assert.fail("Protected page was retrieved.   This should not occur.");
        } catch (Exception ex1) {
            if (!ex1.toString().contains("500 Internal Server Error")) {
                Assert.fail("exception thrown getting protected page    ex = " + ex1);
            }
        }
    }

    /**
     * Test to see that the view scope issue is fixed.
     * This test is run w/ the SERIALIZE_SERVER_STATE state set to false.
     * https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-787
     * 
     * @throws Exception
     */
    @Test
    public void testViewScopeBinding() throws Exception {
        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, contextRoot, "testViewScopeBinding.jsf");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("test ViwScopeBinding page was not available.");
        }

        // Click the button to execute the method and update the page
        HtmlElement button = (HtmlElement) page.getElementById("Click");
        page = button.click();

        // Look for the correct results
        HtmlElement output = (HtmlElement) page.getElementById("testOutput");
        if (!output.asText().contains("PostConstruct counter = 1")) {
            Assert.fail("Invalid response from server.  <PostConstruct counter> is set incorrectly = " + output.asText());
        }
    }

    /**
     * Test to see that the view scope issue is fixed.
     * This test is run w/ the SERIALIZE_SERVER_STATE state set to true.
     * https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-787
     * 
     * @throws Exception
     */
    @Test
    public void testViewScopeBindingSerialize() throws Exception {
        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, contextRoot, "testViewScopeBinding.jsf");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("test ViwScopeBinding (Serialize) page was not available.");
        }

        // Click the button to execute the method and update the page
        HtmlElement button = (HtmlElement) page.getElementById("Click");
        page = button.click();

        // Look for the correct results
        HtmlElement output = (HtmlElement) page.getElementById("testOutput");
        if (!output.asText().contains("PostConstruct counter = 1")) {
            Assert.fail("Invalid response from server.  <PostConstruct counter> is set incorrectly = " + output.asText());
        }
    }

    /**
     * Test to see that the myfaces view scope issue is fixed.
     * This test is run w/ the SERIALIZE_SERVER_STATE state set to false.
     * https://issues.apache.org/jira/browse/MYFACES-3656
     * 
     * @throws Exception
     */
    @Test
    public void testViewScopeMyFaces() throws Exception {
        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, contextRoot, "testViewScopeMyFaces.jsf");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        System.out.println(page.asText());
        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("test ViewScopeMyFaces page was not available.");
        }

        // Click the button to execute the method and update the page
        HtmlElement button = (HtmlElement) page.getElementById("button2");
        page = button.click();

        // Look for the correct results
        if (!page.asText().contains("Invalid Email: Email can Not be empty")) {
            Assert.fail("Invalid response from server.  The response should contain <Invalid Email: Email can Not be empty> = " + page.asText());
        }
    }

    /**
     * Test to see that the myfaces view scope issue is fixed.
     * This test is run w/ the SERIALIZE_SERVER_STATE state set to true.
     * https://issues.apache.org/jira/browse/MYFACES-3656
     * 
     * @throws Exception
     */
    @Test
    public void testViewScopeMyFacesSerialize() throws Exception {
        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, "JSF22MiscellaneousSerialize", "testViewScopeMyFaces.jsf");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("test ViewScopeMyFaces (Serialize) page was not available.");
        }

        // Click the button to execute the method and update the page
        HtmlElement button = (HtmlElement) page.getElementById("button2");
        page = button.click();

        // Look for the correct results
        if (!page.asText().contains("Invalid Email: Email can Not be empty")) {
            Assert.fail("Invalid response from server.  The response should contain <Invalid Email: Email can Not be empty> = " + page.asText());
        }
    }

    /**
     * Check to make sure that the javax.faces.STATE_SAVING_METHOD case.
     * 
     * @throws Exception
     */
    @Test
    public void JSF22Miscellaneous_TestSERVERSTATE_case() throws Exception {

        String msg = "Wrong value in context init parameter 'javax.faces.STATE_SAVING_METHOD'";
        // Check the trace.log
        // There should not be a match so fail if there is.
        assertTrue(msg, jsf22MiscellaneousServer.findStringsInLogs(msg).isEmpty());

    }

    /**
     * Test to see that MyFaces Application.getExpressionFactory()
     * returns the same object as JspFactory.getDefaultFactory().
     * getJspApplicationContext(servletContext).getExpressionFactory().
     * 
     * Addresses CTS test failures:
     * ./jsf/api/javax_faces/application/applicationwrapper/URLClient_applicationWrapperGetExpressionFactoryTest
     * ./jsf/api/javax_faces/application/application/URLClient_applicationGetExpressionFactoryTest
     * 
     * @throws Exception
     */
    @Test
    public void testExpressionFactoryImplConsistency() throws Exception {
        WebClient webClient = new WebClient();

        URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, contextRoot, "testExpressionFactoryImplConsistency.jsf");
        HtmlPage page = (HtmlPage) webClient.getPage(url);

        // Make sure the page initially renders correctly
        if (page == null) {
            Assert.fail("testExpressionFactoryImplConsistency page was not available.");
        }

        // Look for the correct results
        if (!page.asText().contains("ExpressionFactory-instance test passed")) {
            Assert.fail("The JSP and JSF (Application) ExpressionFactory objects were not the same " + page.asText());
        }
    }
}
