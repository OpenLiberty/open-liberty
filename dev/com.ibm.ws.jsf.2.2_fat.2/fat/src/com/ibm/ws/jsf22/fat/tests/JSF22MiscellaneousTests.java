/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf22.fat.tests;

import static componenttest.annotation.SkipForRepeat.EE10_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE10_OR_LATER_FEATURES;
import static componenttest.annotation.SkipForRepeat.EE8_FEATURES;
import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf22.fat.FATSuite;
import com.ibm.ws.jsf22.fat.JSFUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.faces.fat.selenium.util.internal.CustomDriver;
import io.openliberty.faces.fat.selenium.util.internal.ExtendedWebDriver;
import io.openliberty.faces.fat.selenium.util.internal.WebPage;
import junit.framework.Assert;

/**
 * Tests to execute on the jsf22MiscellaneousServer that use HtmlUnit.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF22MiscellaneousTests {
    @Rule
    public TestName name = new TestName();

    private static final String APP_NAME_MISCELLANEOUS = "JSF22Miscellaneous";
    private static final String APP_NAME_MISCELLANEOUS_SERIALIZE = "JSF22MiscellaneousSerialize";
    private static final String APP_NAME_VIEW_SCOPE_LEAK = "ViewScopeLeak";
    private static final String APP_NAME_FUNCTION_MAPPER = "FunctionMapper";
    private static final String APP_NAME_OLGH22397 = "OLGH22397";
    private static final String APP_NAME_FACES_CONFIX_MISSING_XMLNS = "FacesConfigMissingXmlns";
    private static final String APP_NAME_MYFACES_4512 = "MYFACES-4512";
    private static final String APP_NAME_XML_VALIDATION_GOOD = "XMLValidationGood";
    private static final String APP_NAME_XML_VALIDATION_BAD = "XMLValidationBad";

    protected static final Class<?> c = JSF22MiscellaneousTests.class;

    @Server("jsf22MiscellaneousServer")
    public static LibertyServer jsf22MiscellaneousServer;

    @ClassRule
    public static BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(FATSuite.getChromeImage()).withCapabilities(new ChromeOptions())
                    .withAccessToHost(true)
                    .withSharedMemorySize(2147483648L); // avoids "message":"Duplicate mount point: /dev/shm"

    @BeforeClass
    public static void setup() throws Exception {
        boolean isEE10 = JakartaEEAction.isEE10OrLaterActive();

        JavaArchive JSF22MiscellaneousJar = ShrinkHelper.buildJavaArchive(APP_NAME_MISCELLANEOUS + ".jar", "com.ibm.ws.jsf22.fat.miscbean.jar",
                                                                          isEE10 ? "com.ibm.ws.jsf22.fat.miscbean.jar.faces40" : "com.ibm.ws.jsf22.fat.miscbean.jar.jsf22");

        WebArchive JSF22MiscellaneousWar = ShrinkHelper.buildDefaultApp(APP_NAME_MISCELLANEOUS + ".war", "com.ibm.ws.jsf22.fat.miscbean",
                                                                        isEE10 ? "com.ibm.ws.jsf22.fat.miscbean.faces40" : "com.ibm.ws.jsf22.fat.miscbean.jsf22");

        WebArchive SerializeWar = ShrinkHelper.buildDefaultApp(APP_NAME_MISCELLANEOUS_SERIALIZE + ".war", "");

        WebArchive xmlnsWar = ShrinkHelper.buildDefaultApp(APP_NAME_FACES_CONFIX_MISSING_XMLNS + ".war", "");

        EnterpriseArchive JSF22MiscellaneousEar = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME_MISCELLANEOUS + ".ear");

        JSF22MiscellaneousWar.addAsLibraries(JSF22MiscellaneousJar);
        SerializeWar.addAsLibraries(JSF22MiscellaneousJar);
        JSF22MiscellaneousEar.addAsModule(JSF22MiscellaneousWar);
        JSF22MiscellaneousEar.addAsModule(SerializeWar);

        ShrinkHelper.addDirectory(JSF22MiscellaneousEar, "test-applications" + "/" + APP_NAME_MISCELLANEOUS + ".ear" + "/resources");

        ShrinkHelper.exportDropinAppToServer(jsf22MiscellaneousServer, JSF22MiscellaneousEar);

        ShrinkHelper.exportDropinAppToServer(jsf22MiscellaneousServer, xmlnsWar);

        ShrinkHelper.defaultDropinApp(jsf22MiscellaneousServer, APP_NAME_FUNCTION_MAPPER + ".war", "com.ibm.ws.jsf23.fat.functionmapper");

        ShrinkHelper.defaultDropinApp(jsf22MiscellaneousServer, APP_NAME_VIEW_SCOPE_LEAK + ".war", "com.ibm.ws.jsf22.fat.viewscopedleak");

        ShrinkHelper.defaultDropinApp(jsf22MiscellaneousServer, APP_NAME_OLGH22397 + ".war");

        ShrinkHelper.defaultDropinApp(jsf22MiscellaneousServer, APP_NAME_MYFACES_4512 + ".war", "com.ibm.ws.jsf22.fat.myfaces4512.viewhandler");

        jsf22MiscellaneousServer.startServer(c.getSimpleName() + ".log");

        Testcontainers.exposeHostPorts(jsf22MiscellaneousServer.getHttpDefaultPort(), jsf22MiscellaneousServer.getHttpDefaultSecurePort());
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
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_MISCELLANEOUS, "testSimple.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("JSF22Miscellaneous_Simple.xhtml did not render properly.");
            }

            assertTrue(page.asText().contains("Testing JSF2.2 miscellaneous - this is test outputText"));
        }
    }

    /**
     * Programmatically tests if the new JSF 2.2 API is working properly.
     *
     * @throws Exception
     */
    @Test
    public void testAPI() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_MISCELLANEOUS, "testAPI.xhtml");
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
            if (!output.asText().contains("getApplicationContextPath = /" + APP_NAME_MISCELLANEOUS)) {
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
    }

    /**
     * Programmatically tests if the new JSF 2.2 resetValues function is working properly.
     * This is related to https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-1129
     *
     * @throws Exception
     */
    @Test
    public void testResetValues() throws Exception {

        ExtendedWebDriver driver = new CustomDriver(new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions().setAcceptInsecureCerts(true)));

        String url = JSFUtils.createSeleniumURLString(jsf22MiscellaneousServer, APP_NAME_MISCELLANEOUS, "testResetValues.xhtml");
        WebPage page = new WebPage(driver);
        Log.info(c, name.getMethodName(), "Navigating to: /" + APP_NAME_MISCELLANEOUS + "/testResetValues.jsf");
        page.get(url);
        page.waitForPageToLoad();
        assertTrue(page.isInPageText("JSF 2.2 Test ResetValues"));

        WebElement firstNameInupt = page.findElement(By.id("form1:firstName"));
        firstNameInupt.sendKeys("John");

        page.findElement(By.id("form1:save")).click();
        page.waitReqJs();
        assertTrue(page.findElement(By.id("form1:firstName")).getAttribute("value").contains("John"));

        WebElement reset = page.findElement(By.id("form1:reset"));
        Log.info(c, name.getMethodName(), "Clicking Reset...");
        reset.click();
        page.waitReqJs();

        String input = page.findElement(By.id("form1:firstName")).getAttribute("value");
        Log.info(c, name.getMethodName(), "form1:firstName value (expecting empty string) = " + input);

        // Look for the correct results. The "John" text should not exist in the page.
        assertFalse(input.contains("John"));
    }

    /**
     * Programmatically tests if the new JSF 2.2 CSRF functionality is working properly.
     * This is related to https://java.net/jira/browse/JAVASERVERFACES_SPEC_PUBLIC-869
     *
     * @throws Exception
     */
    @Test
    public void testCSRF() throws Exception {
        try (WebClient webClient = new WebClient(); WebClient webClient2 = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_MISCELLANEOUS, "testCSRF.xhtml");
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
            if (!page.getUrl().toString().contains((JakartaEEAction.isEE9OrLaterActive() ? "jakarta." : "javax.") + "faces.Token=")) {
                Assert.fail("Invalid response from server.  This page does NOT contain the token = " + page.asText());
            }

            //  Attempt to retrieve a page that is "protected".    This SHOULD result in a "500 Internal Server Error"
            try {
                // Turn off printing for this webClient.   We know there will very likely be an error.
                webClient2.getOptions().setPrintContentOnFailingStatusCode(false);
                url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_MISCELLANEOUS, "protectedPage.xhtml");
                webClient2.getPage(url);
                Assert.fail("Protected page was retrieved.   This should not occur.");
            } catch (Exception ex1) {
                if (!ex1.toString().contains("500 Internal Server Error")) {
                    Assert.fail("exception thrown getting protected page    ex = " + ex1);
                }
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
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_MISCELLANEOUS, "testViewScopeBinding.jsf");
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
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_MISCELLANEOUS, "testViewScopeBinding.jsf");
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
    }

    /**
     * Test to see that the myfaces view scope issue is fixed.
     * This test is run w/ the SERIALIZE_SERVER_STATE state set to false.
     * https://issues.apache.org/jira/browse/MYFACES-3656
     *
     * @throws Exception
     */
    @Test
    // Skip for EE10+ because this test is specific to Faces Managed Beans which
    // are no longer supported in Faces 4.0.
    @SkipForRepeat(EE10_OR_LATER_FEATURES)
    public void testViewScopeMyFaces() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_MISCELLANEOUS, "testViewScopeMyFaces.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

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
    }

    /**
     * Test to see that the myfaces view scope issue is fixed.
     * This test is run w/ the SERIALIZE_SERVER_STATE state set to true.
     * https://issues.apache.org/jira/browse/MYFACES-3656
     *
     * @throws Exception
     */
    @Test
    // Skip for EE10+ because this test is specific to Faces Managed Beans which
    // are no longer supported in Faces 4.0.
    @SkipForRepeat(EE10_OR_LATER_FEATURES)
    public void testViewScopeMyFacesSerialize() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_MISCELLANEOUS_SERIALIZE, "testViewScopeMyFaces.jsf");
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
     * For Faces 4.0, Pages is no longer supported and the specification was updated to say:
     * "The implementation must return the ExpressionFactory from the Expression Language container by calling jakarta.el.ELManager.getExpressionFactory()"
     *
     * Addresses CTS test failures:
     * ./jsf/api/javax_faces/application/applicationwrapper/URLClient_applicationWrapperGetExpressionFactoryTest
     * ./jsf/api/javax_faces/application/application/URLClient_applicationGetExpressionFactoryTest
     *
     * @throws Exception
     */
    @Test
    public void testExpressionFactoryImplConsistency() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_MISCELLANEOUS, "testExpressionFactoryImplConsistency.jsf");
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

    /**
     *
     * Ensure FunctionMapper is set on the ELContext
     * - MyFaces 4333
     *
     * @throws Exception
     */
    @Test
    public void testFunctionMapper() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_FUNCTION_MAPPER, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            page = page.getElementById("form1:button1").click();

            assertTrue("Function Mapper is null!", page.asText().contains("FunctionMapper Exists (Expecting true): true"));
        }
    }

    /**
     * Check to make sure that an app starts correctly when it has a faces-config.xml which is missing a "xmlns" declaration.
     *
     * See https://github.com/OpenLiberty/open-liberty/issues/18155
     *
     * @throws Exception
     */
    @Test
    public void testFacesConfigMissingXmlns() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_FACES_CONFIX_MISSING_XMLNS, "index.xhtml");
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            if (page == null) {
                Assert.fail("index.xhtml did not render properly.");
            } else if (!page.asText().contains("Test Success")) {
                Assert.fail("/FacesConfigMissingXmlns/index.xhtml did not contain \"Test Success\":\n" + page.asXml());
            }
        }
    }

    /**
     *
     * Check that a ClassNotFoundException is not thrown when a custom tag uses Application.createValueBinding()
     * See https://github.com/OpenLiberty/open-liberty/issues/18437
     *
     * @throws Exception
     */
    @Test
    // Skip for EE10+ as the jakarta.faces.application.Application.createValueBinding method
    // was removed. In addition the fix for this isn't necessary for Faces 4.0 because the
    // org.apache.myfaces.el.convert packages doesn't exist any longer.
    @SkipForRepeat(EE10_OR_LATER_FEATURES)
    public void testCustomValueBindingTag() throws Exception {
        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_MISCELLANEOUS, "testCustomValueBindingTag.jsf");
            HtmlPage page = (HtmlPage) webClient.getPage(url);
            page = page.getElementById("form:submit").click();

            if (page == null) {
                Assert.fail("testCustomValueBindingTag.xhtml did not render properly.");
            } else {
                Log.info(c, "page text:", page.asText());
                assertFalse("A ClassNotFoundException was thrown", page.asText().contains("java.lang.ClassNotFoundException"));
                assertTrue("Unexpected output", page.asText().contains("18437"));
            }
        }
    }

    /*
     * https://github.com/OpenLiberty/open-liberty/issues/20950
     * https://issues.apache.org/jira/browse/MYFACES-4433
     *
     * Verifies there is no leak when ViewScope Beans are used.
     * There was previously a memory leak with the ViewScopeBeanHolder's storageMap. See issues above.
     */
    @Test
    public void testMyFaces4433() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_VIEW_SCOPE_LEAK, "index.xhtml");

            String size1 = "1";
            String size2 = "2";

            // Goes to index.xhtml to create a new view. Goes to new page (invalidate) to end the view.
            // invalidate.xhtml will list size of WELD_S#0 via SessionSizeHelper. Then it loops again.
            // The size should be the same for both runs.
            // WELD_S#0 is the attribute where the ViewScope related storage is saved via CDI.
            for (int i = 0; i < 2; i++) {
                Log.info(c, name.getMethodName(), "MYFACES-4433: Making a request to " + url);
                HtmlPage page = (HtmlPage) webClient.getPage(url);

                Log.info(c, name.getMethodName(), "Clicking invalidate.....");
                HtmlSubmitInput submitButton = (HtmlSubmitInput) page.getElementById("form1:invalidate");
                page = submitButton.click();

                assertTrue("MYFACES-4433: the app output was null!", page != null);

                String sessionSize = page.getElementById("form2:sessionSize").getTextContent();
                if (i == 0) {
                    size1 = sessionSize;
                } else {
                    size2 = sessionSize;
                }

                Log.info(c, name.getMethodName(), "Session Size -> " + sessionSize);
            }

            assertTrue("WELD_S# attribute size differed! Leak Detected!", size1.equals(size2));

        }
    }

    /*
     * https://github.com/OpenLiberty/open-liberty/issues/22397
     * https://issues.apache.org/jira/projects/MYFACES/issues/MYFACES-4450
     *
     * Verify tabindex (and other attributes) are rendered
     *
     */
    @Test
    public void testMyFaces4450() throws Exception {
        try (WebClient webClient = new WebClient()) {

            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_OLGH22397, "index.xhtml");

            Log.info(c, name.getMethodName(), "MYFACES-4433: Making a request to " + url);
            HtmlPage page = (HtmlPage) webClient.getPage(url);

            Log.info(c, name.getMethodName(), page.asXml());

            String[] expected = { "onclick=\"onclick\"",
                                  "ondblclick=\"ondblclick\"",
                                  "onmousedown=\"onmousedown\"",
                                  "onmouseup=\"onmouseup\"",
                                  "onmouseover=\"onmouseover\"",
                                  "onmousemove=\"onmousemove\"",
                                  "onmouseout=\"onmouseout\"",
                                  "onkeypress=\"onkeypress\"",
                                  "onkeydown=\"onkeydown\"",
                                  "onkeyup=\"onkeyup\"",
                                  "onfocus=\"onfocus\"",
                                  "onblur=\"onblur\"",
                                  "accesskey=\"accesskey\"",
                                  "tabindex=\"tabindex\"",
                                  "style=\"color:red\"",
                                  "class=\"styleClass\"",
                                  "dir=\"dir\"",
                                  "lang=\"lang\"",
                                  "title=\"title\"",
                                  "role=\"role\"" };

            for (String attribute : expected) {
                assertTrue("Failed to render expected attribute: \n" + attribute, page.asXml().contains(attribute));
            }

        }
    }

    @Test
    public void testMyFaces4512() throws Exception {
        try (WebClient webClient = new WebClient()) {
            URL url = JSFUtils.createHttpUrl(jsf22MiscellaneousServer, APP_NAME_MYFACES_4512, "index.xhtml");

            Log.info(c, name.getMethodName(), "MYFACES-4512: Making a request to " + url);
            Page page = webClient.getPage(url);

            int statusCode = page.getWebResponse().getStatusCode();
            Log.info(c, name.getMethodName(), "Page xml: " + ((HtmlPage) page).asXml());

            assertTrue("The status code was not 200 but was: " + statusCode, statusCode == 200);

            assertTrue("The MyFaces4512ViewHandler was not invoked!", !jsf22MiscellaneousServer.waitForStringInLog("MyFaces4512ViewHandler was invoked!").isEmpty());
        }
    }

    /*
     * All the XML should be valid by default.
     */
    @SkipForRepeat({ NO_MODIFICATION, EE8_FEATURES, EE10_FEATURES })
    @Test
    public void testXMLValidationGoodCase() throws Exception {

        jsf22MiscellaneousServer.setMarkToEndOfLog();
        jsf22MiscellaneousServer.setTraceMarkToEndOfDefaultTrace();

        ShrinkHelper.defaultDropinApp(jsf22MiscellaneousServer, APP_NAME_XML_VALIDATION_GOOD + ".war");

        assertTrue("XML Validation Error found erroneously.",
                   jsf22MiscellaneousServer.findStringsInLogsAndTraceUsingMark(".*cvc-complex-type.2.4.a: Invalid content was found starting with element.*").isEmpty());

        assertTrue("XML Validation Error found erroneously.", jsf22MiscellaneousServer.findStringsInLogsAndTraceUsingMark(".*org.xml.sax.SAXParseException.*").isEmpty());

        jsf22MiscellaneousServer.resetLogMarks();

        jsf22MiscellaneousServer.removeAndStopDropinsApplications(APP_NAME_XML_VALIDATION_GOOD + ".war");
    }

    /*
     * The bad case involves the faces-config.xml including an unexpected element. Validation should fail with following exception:
     * [ERROR ] cvc-complex-type.2.4.a: Invalid content was found starting with element '{"http://xmlns.jcp.org/xml/ns/javaee":wrong-element}'.
     */
    @SkipForRepeat({ NO_MODIFICATION, EE8_FEATURES })
    @Test
    public void testXMLValidationBadCase() throws Exception {

        jsf22MiscellaneousServer.setMarkToEndOfLog();
        jsf22MiscellaneousServer.setTraceMarkToEndOfDefaultTrace();

        ShrinkHelper.defaultDropinApp(jsf22MiscellaneousServer, APP_NAME_XML_VALIDATION_BAD + ".war");

        assertTrue("XML Validation Error not found!.",
                   jsf22MiscellaneousServer.findStringsInLogsAndTraceUsingMark(".*cvc-complex-type.2.4.a: Invalid content was found starting with element.*").size() >= 1);

        jsf22MiscellaneousServer.resetLogMarks();

        jsf22MiscellaneousServer.removeAndStopDropinsApplications(APP_NAME_XML_VALIDATION_BAD + ".war");

    }

}
