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
package com.ibm.ws.jsf23.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * General JSF 2.3 test cases the also require CDI.
 */
@RunWith(FATRunner.class)
public class JSF23CDIGeneralTests {

    protected static final Class<?> c = JSF23CDIGeneralTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23CDIServer")
    public static LibertyServer jsf23CDIServer;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "PostRenderViewEvent.war", "com.ibm.ws.jsf23.fat.postrenderview.events");
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "CDIManagedProperty.war", "com.ibm.ws.jsf23.fat.cdi.managedproperty");
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "ELImplicitObjectsViaCDI.war", "com.ibm.ws.jsf23.fat.elimplicit.cdi.beans");
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "JSF23Spec1300.war", "com.ibm.ws.jsf23.fat.spec1300");
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "ResourceRendering.war", "com.ibm.ws.jsf23.fat.resourcerendering");
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "JSF23Spec217.war", "com.ibm.ws.jsf23.fat.spec217");
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "JSF23Spec790.war", "com.ibm.ws.jsf23.fat.spec790");
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "ConvertDateTime.war", "com.ibm.ws.jsf23.fat.convertdatetime.beans");
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "ConverterValidatorBehaviorInjectionTarget.war", "com.ibm.ws.jsf23.fat.converter.validator.behavior.injection.beans");
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "JSF23CDITests.war",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans.factory",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.client.window");
        ShrinkHelper.defaultDropinApp(jsf23CDIServer, "CDIIntegrationTest.war",
                                      "com.ibm.ws.jsf23.fat.cdi.integration.application",
                                      "com.ibm.ws.jsf23.fat.cdi.integration.beans",
                                      "com.ibm.ws.jsf23.fat.cdi.integration.viewhandler");

        // Create the JSF23Spec1433 JAR
        JavaArchive spec1433Jar = ShrinkWrap.create(JavaArchive.class, "JSF23Spec1433.jar");
        spec1433Jar.addPackage("com.ibm.ws.jsf23.fat.spec1433");

        // Create the JSF23Spec1433True WAR and add the JSF23Spec1433 JAR to it
        WebArchive spec1433TrueWar = ShrinkWrap.create(WebArchive.class, "JSF23Spec1433True.war");
        spec1433TrueWar.addAsLibrary(spec1433Jar);
        ShrinkHelper.addDirectory(spec1433TrueWar, "test-applications/" + "JSF23Spec1433True.war" + "/resources");
        ShrinkHelper.exportToServer(jsf23CDIServer, "dropins", spec1433TrueWar);

        // Create the JSF231433SpecFalse WAR and add the JSF23Spec1433 JAR to it
        WebArchive spec1433FalseWar = ShrinkWrap.create(WebArchive.class, "JSF23Spec1433False.war");
        spec1433FalseWar.addAsLibrary(spec1433Jar);
        ShrinkHelper.addDirectory(spec1433FalseWar, "test-applications/" + "JSF23Spec1433False.war" + "/resources");
        ShrinkHelper.exportToServer(jsf23CDIServer, "dropins", spec1433FalseWar);

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server
        jsf23CDIServer.startServer(JSF23CDIGeneralTests.class.getSimpleName() + ".log");
    }

    @Before
    public void startServer() throws Exception {
        if (jsf23CDIServer != null && !jsf23CDIServer.isStarted()) {
            jsf23CDIServer.startServer(JSF23CDIGeneralTests.class.getSimpleName() + ".log");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23CDIServer != null && jsf23CDIServer.isStarted()) {
            jsf23CDIServer.stopServer();
        }
    }

    /**
     * This test case drives a request to an application that has a PhaseListener that will
     * log a message before and after the RENDER_RESPONSE phase of the JSF lifecycle.
     *
     * During the RENDER_RESSPONE phase the PreRenderView and PostRenderView events should
     * be fired in order. We check the trace log to ensure that the logged messages are output
     * in the correct order.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testPostRenderViewEvent() throws Exception {
        String contextRoot = "PostRenderViewEvent";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure the messages are output in the correct order to verify the events are fired
            // in the correct order.
            String beforeRenderResponse = "Before Render Response Phase";
            String afterRenderResponse = "After Render Response Phase";
            String preRenderView = "Processing PreRenderViewEvent";
            String postRenderView = "Processing PostRenderViewEvent";

            assertNotNull("The following String was not found in the trace log: " + beforeRenderResponse,
                          jsf23CDIServer.waitForStringInTraceUsingLastOffset(beforeRenderResponse));

            assertNotNull("The following String was not found in the trace log: " + preRenderView,
                          jsf23CDIServer.waitForStringInTraceUsingLastOffset(preRenderView));

            assertNotNull("The following String was not found in the trace log: " + postRenderView,
                          jsf23CDIServer.waitForStringInTraceUsingLastOffset(postRenderView));

            assertNotNull("The following String was not found in the trace log: " + afterRenderResponse,
                          jsf23CDIServer.waitForStringInTraceUsingLastOffset(afterRenderResponse));
        }

    }

    /**
     * This test is run on a server that has an application deployed that contains a
     * faces-config.xml with a version element of 2.3. The PostRenderViewEvent.war
     * has a faces-config.xml in it with the 2.3 version.
     *
     * This test will ensure the application with the faces-config.xml we are testing
     * has been started.
     *
     * The test will ensure that the following exception is not found in the trace.log:
     *
     * CWWKC2262E: The server is unable to process the 2.3 version and the
     * http://xmlns.jcp.org/xml/ns/javaee namespace in the /WEB-INF/faces-config.xml
     * deployment descriptor on line 5
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testFacesConfigVersion23() throws Exception {
        String appStarted = jsf23CDIServer.waitForStringInLog("CWWKZ0001I.*" + "Application PostRenderViewEvent", jsf23CDIServer.getConsoleLogFile());

        assertTrue("The PostRenderViewEvent application did not start.", appStarted != null);
        assertTrue("The CWWKC2262E exception was found in the trace.log when it should not have been.",
                   jsf23CDIServer.findStringsInTrace("CWWKC2262E").isEmpty());
    }

    /**
     * This is a test for the CDI @ManagedProperty. The test drives a request to a page
     * and ensures that the proper initial values are found.
     *
     * Then the values are updated and the test checks to ensure we get the proper
     * updated values.
     *
     * The beans that are used are testing that @ManagedPropery works for multiple different
     * types including arrays, parameterized List, and primitive
     *
     * See the CDIManagedProperty.war for more details.
     *
     * @throws Exception
     */
    @Test
    public void testCDIManagedProperty() throws Exception {
        String contextRoot = "CDIManagedProperty";
        try (WebClient webClient = new WebClient()) {
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());

            String initalValue = "numberManagedProperty = 0 textManagedProperty = zero "
                                 + "listManagedProperty = zero stringArrayManagedProperty = "
                                 + "zero bean = com.ibm.ws.jsf23.fat.cdi.managedproperty.TestBean";

            String finalValue = "numberManagedProperty = 1 textManagedProperty = 2 "
                                + "listManagedProperty = 3 stringArrayManagedProperty = 4 bean = "
                                + "com.ibm.ws.jsf23.fat.cdi.managedproperty.TestBean";

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlTextInput input1 = (HtmlTextInput) page.getElementById("in1");
            HtmlTextInput input2 = (HtmlTextInput) page.getElementById("in2");
            HtmlTextInput input3 = (HtmlTextInput) page.getElementById("in3");
            HtmlTextInput input4 = (HtmlTextInput) page.getElementById("in4");

            String output = page.getElementById("out1").asText();

            // Assert the initial values of out1.
            assertTrue("The initial values were not correct. One or more of the @ManagedProperty injections failed.",
                       output.substring(0, output.indexOf("@")).equals(initalValue));

            // Now fill in the new values into the input fields
            input1.setValueAttribute("1");
            input2.setValueAttribute("2");
            input3.setValueAttribute("3");
            input4.setValueAttribute("4");

            // Now click the submit button
            page = page.getElementById("button1").click();

            Log.info(c, name.getMethodName(), page.asText());

            output = page.getElementById("out1").asText();

            // Assert the updated values of out1
            assertTrue("The updated values were not correct. One or more of the @ManagedProperty injections failed.",
                       output.substring(0, output.indexOf("@")).equals(finalValue));
        }
    }

    /**
     * Test to ensure that the EL implicit objects can be injected through CDI.
     *
     * Additionally, test that this application can be restarted without a LinkageError:
     * see https://github.com/OpenLiberty/open-liberty/issues/10816
     *
     * @throws Exception
     */
    @Test
    public void testInjectableELImplicitObjects() throws Exception {
        try (WebClient webClient = new WebClient()) {
            checkInjectableELImplicitObjects(webClient);
            // restart the app and test again
            jsf23CDIServer.restartDropinsApplication("ELImplicitObjectsViaCDI.war");
            checkInjectableELImplicitObjects(webClient);
        }
    }

    private void checkInjectableELImplicitObjects(WebClient webClient) throws Exception {

            // Add a message to the header map
            webClient.addRequestHeader("headerMessage", "This is a test");

            // Construct the URL for the test
            String contextRoot = "ELImplicitObjectsViaCDI";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "index.xhtml");

            HtmlPage testInjectableImplicitObjectsPage = (HtmlPage) webClient.getPage(url);

            // Verify that the page contains the expected messages.
            assertTrue(testInjectableImplicitObjectsPage.asText().contains("JSF 2.3 EL implicit objects using CDI"));

            // Get the form that we are dealing with
            HtmlForm form = testInjectableImplicitObjectsPage.getFormByName("form1");

            // Get the button to click
            HtmlSubmitInput submitButton = form.getInputByName("form1:submitButton");

            // Now click the button and get the resulting page.
            HtmlPage resultPage = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultPage.asText());
            Log.info(c, name.getMethodName(), resultPage.asXml());

            // Verify that the page contains the expected messages.
            assertTrue(resultPage.asText().contains("FacesContext project stage: Production"));
            assertTrue(resultPage.asText().contains("ServletContext context path: /ELImplicitObjectsViaCDI"));
            assertTrue(resultPage.asText().contains("ExternalContext app context path: /ELImplicitObjectsViaCDI"));
            assertTrue(resultPage.asText().contains("UIViewRoot viewId: /index.xhtml"));
            assertTrue(resultPage.asText().contains("Flash isRedirect: false"));
            assertTrue(resultPage.asText().contains("HttpSession isNew: false"));
            assertTrue(resultPage.asText().contains("Application name from ApplicationMap: ELImplicitObjectsViaCDI"));
            assertTrue(resultPage.asText().contains("Char set from SessionMap: UTF-8"));
            assertTrue(resultPage.asText().contains("ViewMap isEmpty: true"));
            assertTrue(resultPage.asText().contains("URI from RequestMap: /ELImplicitObjectsViaCDI/index.xhtml"));
            assertTrue(resultPage.asText().contains("Message from HeaderMap: This is a test"));
            assertTrue(resultPage.asText().contains("WELD_CONTEXT_ID_KEY from InitParameterMap: ELImplicitObjectsViaCDI"));
            assertTrue(resultPage.asText().contains("Message from RequestParameterMap: Hello World"));
            assertTrue(resultPage.asText().contains("Message from RequestParameterValuesMap: [Hello World]"));
            assertTrue(resultPage.asText().contains("Message from HeaderValuesMap: [This is a test]"));

            if(JakartaEE9Action.isActive()){
              assertTrue(resultPage.asText().contains("Resource handler JSF_SCRIPT_LIBRARY_NAME constant: jakarta.faces"));
              assertTrue(resultPage.asText()
                              .contains("Flow map object is null: Exception: WELD-001303: No active contexts "
                                        + "for scope type jakarta.faces.flow.FlowScoped")); // Expected exception
              assertTrue(resultPage.asText().contains("Cookie object from CookieMap: jakarta.servlet.http.Cookie"));

            } else {
              assertTrue(resultPage.asText().contains("Resource handler JSF_SCRIPT_LIBRARY_NAME constant: javax.faces"));
              assertTrue(resultPage.asText()
                              .contains("Flow map object is null: Exception: WELD-001303: No active contexts "
                                        + "for scope type javax.faces.flow.FlowScoped")); // Expected exception
              assertTrue(resultPage.asText().contains("Cookie object from CookieMap: javax.servlet.http.Cookie"));
            }

    }

    /**
     * Test to ensure that the EL Resolution of implicit objects works as expected
     * when CDI is being used.
     *
     * @throws Exception
     */
    @Test
    public void testELResolutionImplicitObjects() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Add a message to the header map
            webClient.addRequestHeader("headerMessage", "This is a test");

            // Construct the URL for the test
            String contextRoot = "ELImplicitObjectsViaCDI";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "implicit_objects.xhtml?message=Hello World");

            HtmlPage testELResolutionImplicitObjectsPage = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testELResolutionImplicitObjectsPage.asText());
            Log.info(c, name.getMethodName(), testELResolutionImplicitObjectsPage.asXml());

            // Verify that the page contains the expected messages.
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("JSF 2.3 EL resolution of implicit objects using CDI"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("Bean: com.ibm.ws.jsf23.fat.elimplicit.cdi.beans.ELImplicitObjectBean"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("Application project stage: Production"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("ApplicationScope application name: ELImplicitObjectsViaCDI "));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("Component getStyle: font-weight:bold"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("CompositeComponent label: Hello World"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("FacesContext project stage: Production"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("Flash isRedirect: false"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("Header: This is a test"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("HeaderValues: This is a test"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("InitParam: ELImplicitObjectsViaCDI"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("Param: Hello World"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("ParamValues: Hello World"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("Session isNew: true"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("View viewId: /implicit_objects.xhtml "));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("ViewScope isEmpty: true"));
        }
    }

    /**
     * Test the EL Resolution of implicit object #{flowScope}
     *
     * @throws Exception
     */
    @Test
    public void testELResolutionOfFlowScope() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "ELImplicitObjectsViaCDI";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "flow_index.xhtml");

            HtmlPage testELResolutionOfFlowScopePage = (HtmlPage) webClient.getPage(url);

            // Verify that the page contains the expected messages.
            assertTrue(testELResolutionOfFlowScopePage.asText().contains("This flow tests a basic configuration with a @FlowScoped bean. The flow is defined via simple-flow.xml"));

            // Get the form that we are dealing with
            HtmlForm form = testELResolutionOfFlowScopePage.getFormByName("form1");

            // Get the submit button to click
            HtmlSubmitInput submitButton = form.getInputByName("form1:simpleBean");

            // Now click the button and get the resulting page.
            HtmlPage resultPage = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultPage.asText());
            Log.info(c, name.getMethodName(), resultPage.asXml());

            // Verify that the page contains the expected messages.
            assertTrue(resultPage.asText().contains("FlowScope isEmpty: true"));
            assertTrue(resultPage.asText().contains("Flow map isEmpty: true"));
        }
    }

    /**
     * Test injection of EL implicit objects in a bean with no @FacesConfig annotation.
     *
     * The app should not start, throwing an exception due to CDI not being present in the chain.
     *
     * For more information please check Section 5.6.3 from the JSF 2.3 specification.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    @ExpectedFFDC({ "org.jboss.weld.exceptions.DeploymentException", "com.ibm.ws.container.service.state.StateChangeException" })
    public void testELImplicitObjectsInjectionWithNoFacesConfigAnnotation() throws Exception {
        String appName = "ELImplicitObjectsViaCDIErrorApp.war";

        // Set the mark to the end of the logs and install the application.
        // Use the ELImplicitObjectsViaCDIErrorAppServer.xml server configuration file.
        jsf23CDIServer.setMarkToEndOfLog();
        jsf23CDIServer.saveServerConfiguration();
        ShrinkHelper.defaultApp(jsf23CDIServer, appName, "com.ibm.ws.jsf23.fat.elimplicit.cdi.error.beans");
        jsf23CDIServer.setServerConfigurationFile("ELImplicitObjectsViaCDIErrorAppServer.xml");

        // Make sure the application doesn't start
        String expectedCWWKZ0002E = "CWWKZ0002E: An exception occurred while starting the application ELImplicitObjectsViaCDIErrorApp";
        jsf23CDIServer.addIgnoredErrors(Arrays.asList(expectedCWWKZ0002E));
        assertNotNull("The app started and did not throw an error", jsf23CDIServer.waitForStringInLog(expectedCWWKZ0002E));

        // Search for the expected exception
        String message = "The exception message was: com.ibm.ws.container.service.state.StateChangeException: "
                         + "org.jboss.weld.exceptions.DeploymentException: WELD-001408: Unsatisfied dependencies for type FacesContext with qualifiers @Default";
        assertNotNull("The following String was not found in the logs: " + message,
                      jsf23CDIServer.waitForStringInLog(message));

        // Move the mark to the end of the log so we can ensure we wait for the correct server
        // configuration message to be output before uninstalling the application
        jsf23CDIServer.setMarkToEndOfLog();

        // Stop the server but don't archive the logs.
        jsf23CDIServer.stopServer(false);

        // Restore the original server configuration and uninstall the application
        jsf23CDIServer.restoreServerConfiguration();

        // Ensure that the server configuration has completed before uninstalling the application
        jsf23CDIServer.waitForConfigUpdateInLogUsingMark(null);

        // Now uninstall the application and archive the logs.
        jsf23CDIServer.removeInstalledAppForValidation(appName.substring(0, appName.length() - 4));
        jsf23CDIServer.postStopServerArchive();
    }

    /**
     * Test that a CDI Managed Bean can be injected in a FacesConverter.
     *
     * @throws Exception
     */
    @Test
    public void testFacesConverterBeanInjection() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "ConverterValidatorBehaviorInjectionTarget";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Verify that the page contains the expected messages.
            assertTrue(page.asText().contains("JSF 2.3 support for injection into JSF Managed Objects"));

            // Get the form that we are dealing with
            HtmlForm form = page.getFormByName("form1");

            // Get the input text and submit button
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("form1:textId");
            HtmlSubmitInput submitButton = form.getInputByName("form1:submitButton");

            // Fill the input text
            inputText.setValueAttribute("Hello World");

            // Now click the button and get the resulting page.
            HtmlPage resultPage = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultPage.asText());
            Log.info(c, name.getMethodName(), resultPage.asXml());

            // Verify that the page contains the expected messages.
            assertTrue(resultPage.asText().contains("Hello Earth"));
        }
    }

    /**
     * Test that a CDI Managed Bean can be injected in a FacesValidator.
     *
     * @throws Exception
     */
    @Test
    public void testFacesValidatorBeanInjection() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "ConverterValidatorBehaviorInjectionTarget";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Verify that the page contains the expected messages.
            assertTrue(page.asText().contains("JSF 2.3 support for injection into JSF Managed Objects"));

            // Get the form that we are dealing with
            HtmlForm form = page.getFormByName("form1");

            // Get the input text and submit button
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("form1:textId");
            HtmlSubmitInput submitButton = form.getInputByName("form1:submitButton");

            // Fill the input text
            inputText.setValueAttribute("1234");

            // Now click the button and get the resulting page.
            HtmlPage resultPage = submitButton.click();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultPage.asText());
            Log.info(c, name.getMethodName(), resultPage.asXml());

            // Verify that the page contains the expected messages.
            assertTrue(resultPage.asText().contains("Text validation failed. Text does not contain 'World' or 'Earth'."));
        }
    }

    /**
     * Test that a CDI Managed Bean can be injected in a FacesBehavior.
     *
     * @throws Exception
     */
    @Test
    public void testFacesBehaviorBeanInjection() throws Exception {
        try (WebClient webClient = new WebClient()) {
            CollectingAlertHandler alertHandler = new CollectingAlertHandler();
            webClient.setAlertHandler(alertHandler);

            // Construct the URL for the test
            String contextRoot = "ConverterValidatorBehaviorInjectionTarget";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Verify that the page contains the expected messages.
            assertTrue(page.asText().contains("JSF 2.3 support for injection into JSF Managed Objects"));

            // Get the form that we are dealing with
            HtmlForm form = page.getFormByName("form1");

            // Get the submit button
            HtmlSubmitInput submitButton = form.getInputByName("form1:submitButton");

            // Now click the button and get the resulting page.
            HtmlPage resultPage = submitButton.click();

            // Get the alert message
            List<String> alertmsgs = new ArrayList<String>();
            alertmsgs = alertHandler.getCollectedAlerts();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultPage.asText());
            Log.info(c, name.getMethodName(), resultPage.asXml());

            // Verify that the alert contains the expected message
            assertTrue(alertmsgs.contains("Hello World"));
        }
    }

    /**
     * Test that a FacesConverter, a FacesValidator and a FacesBehavior can be injected in a Managed Bean
     *
     * @throws Exception
     */
    @Test
    public void testConverterValidatorBehaviorObjectInjection() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "ConverterValidatorBehaviorInjectionTarget";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "JSFArtifactsInjection.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Verify that the page contains the expected response.
            // TestConverter, TestValidator and TestBehavior objects should have been injected
            assertTrue(page.asText().contains("JSF 2.3 support injection of JSF Managed Objects: FacesConverter, FacesValidator, FacesBehavior"));
            assertTrue(page.asText().contains("com.ibm.ws.jsf23.fat.converter.validator.behavior.injection.beans.TestConverter"));

            assertTrue(page.asText().contains("com.ibm.ws.jsf23.fat.converter.validator.behavior.injection.beans.TestValidator"));
            assertTrue(page.asText().contains("com.ibm.ws.jsf23.fat.converter.validator.behavior.injection.beans.TestBehavior"));
        }
    }

    /**
     * Test the Java Time Support by using f:convertDateTime element.
     *
     * Test the old types "date", "time" and "both" along with the new Java 8 types "localDate",
     * "localTime", "localDateTime", "offsetTime". "offsetDateTime" and "zonedDateTime".
     *
     * Verify that the response contains the expected messages with the correct date/time pattern.
     *
     * @throws Exception
     */
    @Test
    public void testJavaTimeSupport() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "ConvertDateTime";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            String pageText = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            // Verify that the page contains the expected messages.
            assertTrue(pageText.contains("JSF 2.3 Java Time Support - f:convertDateTime"));

            // Get the form that we are dealing with
            HtmlForm form = page.getFormByName("form1");

            // Get the input text
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("form1:input1");

            // Verify that the response contains the expected messages with the correct date/time
            assertTrue("Unexpected value in the input field",
                       inputText.getValueAttribute().equals("2017-06-01 10:30:45"));
            assertTrue(pageText.contains("Type date, dateStyle short: 6/1/17"));
            assertTrue(pageText.contains("Type date, dateStyle medium: Jun 1, 2017"));
            assertTrue(pageText.contains("Type date, dateStyle long: June 1, 2017"));
            assertTrue(pageText.contains("Type date, dateStyle full: Thursday, June 1, 2017"));
            assertTrue(pageText.contains("Type date, pattern MM-dd-yyyy: 06-01-2017"));
            assertTrue(pageText.contains("Type time, timeStyle short: 10:30 AM"));
            assertTrue(pageText.contains("Type time, timeStyle medium: 10:30:45 AM"));
            assertTrue(pageText.contains("Type time, timeStyle long: 10:30:45 AM GMT"));
            assertTrue(pageText.contains("Type time, timeStyle full: 10:30:45 AM GMT") ||
                       pageText.contains("Type time, timeStyle full: 10:30:45 AM Greenwich Mean Time"));
            assertTrue(pageText.contains("Type both, dateStyle full, timeStyle medium: Thursday, June 1, 2017 10:30:45 AM") ||
                       pageText.contains("Type both, dateStyle full, timeStyle medium: Thursday, June 1, 2017, 10:30:45 AM"));
            assertTrue(pageText.contains("Type localDate, dateStyle short: 6/1/17"));
            assertTrue(pageText.contains("Type localDate, dateStyle medium: Jun 1, 2017"));
            assertTrue(pageText.contains("Type localDate, dateStyle long: June 1, 2017"));
            assertTrue(pageText.contains("Type localDate, dateStyle full: Thursday, June 1, 2017"));
            assertTrue(pageText.contains("Type localDate, pattern MM-dd-yyyy: 06-01-2017"));
            assertTrue(pageText.contains("Type localTime, timeStyle short: 10:35 AM"));
            assertTrue(pageText.contains("Type localTime, timeStyle medium: 10:35:45 AM"));
            assertTrue(pageText.contains("Type localTime, pattern HH:mm:ss: 10:35:45"));
            assertTrue(pageText.contains("Type localDateTime, dateStyle short, timeStyle short: 6/1/17 10:30 AM") ||
                       pageText.contains("Type localDateTime, dateStyle short, timeStyle short: 6/1/17, 10:30 AM"));
            assertTrue(pageText.contains("Type localDateTime, dateStyle medium, timeStyle medium: Jun 1, 2017 10:30:45 AM") ||
                       pageText.contains("Type localDateTime, dateStyle medium, timeStyle medium: Jun 1, 2017, 10:30:45 AM"));
            assertTrue(pageText.contains("Type localDateTime, pattern MM-dd-yyyy HH:mm:ss: 06-01-2017 10:30:45"));
            assertTrue(pageText.contains("Type offsetTime: 10:30:45.5-07:00"));
            assertTrue(pageText.contains("Type offsetTime, pattern HH:mm:ss:SSS ZZZZ: 10:30:45:500 GMT-07:00"));
            assertTrue(pageText.contains("Type offsetDateTime: 2017-06-01T10:30:45.5-07:00"));
            assertTrue(pageText.contains("Type offsetDateTime, pattern MM-dd-yyyy HH:mm:ss:SSS ZZZZ: 06-01-2017 10:30:45:500 GMT-07:00"));
            assertTrue(pageText.contains("Type zonedDateTime: 2017-06-01T10:30:45.5-07:00[America/Los_Angeles] "));
            assertTrue(pageText.contains("Type zonedDateTime, pattern MM-dd-yyyy HH:mm:ss ZZZZ z: 06-01-2017 10:30:45:500 GMT-07:00 PDT"));
        }
    }

    /**
     * Test for the following JSF 2.3 Specification Issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/1300
     *
     * First we drive a request to the application and ensure a page that uses
     * a ViewScoped bean loads. This request should ensure that the PostConstructViewMapEvent
     * is published.
     *
     * The application uses a custom ViewRoot component and has a listener for the
     * PostConstructViewMapEvent.
     *
     * The PostConstructViewMapEvent JavaDoc states that the source class for the event
     * will be javax.faces.component.UIViewRoot.
     *
     * Since the application is using a custom ViewRoot and the specification does not
     * allow Application.publishEvent to go up the type hierarchy a sourceBaseType must
     * be used to ensure the listener is invoked even when the UIViewRoot is extended.
     *
     * The source class expected would be UIViewRoot but the actual source class is
     * CustomViewRoot. Using the sourceBaseType solves the issue.
     *
     * MyFaces solution: https://issues.apache.org/jira/browse/MYFACES-4158
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testSpecIssue1300_MyFaces_4158() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec1300";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure the page loaded correctly
            String pageText = "JSF 2.3 Spec Issue 1300";
            assertTrue("The page did not contain the following text: " + pageText, page.asText().contains(pageText));

            // No look at the logs to ensure that the PostConstructViewMapEventListener was invoked
            String listenerString = "PostConstructViewMapEventListener processEvent invoked for PostConstructViewMapEvent!!";
            String listenerInvoked = jsf23CDIServer.waitForStringInLog(listenerString);

            // Ensure the string was found, otherwise the test should fail.
            assertNotNull("The following string was not found in the logs: " + listenerString, listenerInvoked);
        }
    }

    /**
     * A test for the following JSF 2.3 Specification issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/217
     *
     * There are two parts to this test.
     *
     * First we test the h:dataTable rowClass attribute to ensure that
     * the class attribute is rendered correctly. The even rows should
     * have a class attribute with a value of "testStyleRowEven" and
     * the odd rows with a value of "testStyleRowOdd".
     *
     * Second we test the h:column styleClass attribute to ensure that
     * the class attribute is rendered correctly. Each column should have
     * a class attribute with a value of "testStyleCol".
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testSpecIssue217() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec217";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // First make sure that the h:dataTable rowClass attribute works correctly
            HtmlTable table = (HtmlTable) page.getElementById("table");
            String evenClass = "testStyleRowEven";
            String oddClass = "testStyleRowOdd";

            // Need to look at the second and third row as the first row (0) is the header.
            HtmlTableRow row1 = table.getRow(1);
            HtmlTableRow row2 = table.getRow(2);
            Log.info(c, name.getMethodName(), row1.toString());
            Log.info(c, name.getMethodName(), row2.toString());
            String row1Class = row1.getAttribute("class");
            String row2Class = row2.getAttribute("class");

            assertTrue("The class attribute for row 1 should have been: " + evenClass + " but was: " + row1Class, row1Class.equals(evenClass));
            assertTrue("The class attribute for row 2 should have been: " + oddClass + " but was: " + row2Class, row2Class.equals(oddClass));

            // Second make sure that the h:column styleClass attribute works correctly
            String columnClassExpected = "testStyleCol";
            String columnClass;
            List<HtmlTableRow> rows = table.getRows();
            HtmlTableCell cell;

            // Skip row 0 since that is the header
            for (int i = 1; i < rows.size(); i++) {
                cell = rows.get(i).getCell(0);
                columnClass = cell.getAttribute("class");
                Log.info(c, name.getMethodName(), cell.asXml());
                Log.info(c, name.getMethodName(), columnClass);
                assertTrue("The cell had an incorrect column style class. It was expected to be: " + columnClassExpected +
                           " but was: " + columnClass, columnClass.equals(columnClassExpected));
            }
        }
    }

    /**
     * Test ClientWindowFactory spec issue: https://github.com/javaee/javaserverfaces-spec/issues/1241
     * We should be able to use <client-window-factory></client-window-factory> in the faces-config.xml
     *
     * Test that field and method injection is possible on ClientWindowFactory JSF artifact.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testSpecIssue1241AndClientWindowFactoryInjection() throws Exception {

        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23CDITests";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "FactoryInfo.jsf");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Format:  Class | Method | Method injected class: Field injected class : <option> constructor injected class
            String expectedText = "CustomClientWindowFactory|getClientWindow|FactoryDepBean:FactoryAppBean:PostConstructCalled";
            assertTrue("Did not find " + expectedText + " in response", page.asText().contains(expectedText));

            // Stop the server but don't archive the logs (this should shutdown the app and trigger preDestroy)
            jsf23CDIServer.stopServer(false);

            // Verify that PreDestroy is being called
            assertTrue("PreDestroy was not called",
                       jsf23CDIServer.findStringsInLogs("CustomClientWindowFactory preDestroy called").size() == 1);

            // Archive the logs
            jsf23CDIServer.postStopServerArchive();
        }
    }

    /**
     * A test for the following JSF 2.3 Specification issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/790
     *
     * Test that cross form submit can be done using AJAX.
     *
     * First we drive a request to the application.
     *
     * First part:
     * Input a value to form 'a' and submit that value. Verify that the input value
     * of the second form 'b' was re-rendered.
     *
     * Second part:
     * Input a value to form 'b' and submit that value. Verify that the input value
     * of the first form 'a' was re-rendered.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testSpecIssue790Test1() throws Exception {
        try (WebClient webClient = new WebClient()) {
            // Use a synchronizing ajax controller to allow proper ajax updating
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());

            // Construct the URL for the test
            String contextRoot = "JSF23Spec790";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "test1.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Get the input text of the first form and set a value
            HtmlTextInput textInput1 = (HtmlTextInput) page.getElementById("a:input1");
            textInput1.setValueAttribute("test");

            // Get the button to click from the first form
            HtmlSubmitInput submitButton = (HtmlSubmitInput) page.getElementById("a:submitButton1");
            page = submitButton.click();

            String resultingPage = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), page.asXml());

            // Verify that the input text from the second form contains the same value as the input text of the first form.
            assertTrue("Input text of the second form does not contain '" + textInput1.getValueAttribute() + "'",
                       resultingPage.contains("Enter a new value for input2: test"));

            // Get the input text for the second form and set a value
            HtmlTextInput textInput2 = (HtmlTextInput) page.getElementById("b:input2");
            textInput2.setValueAttribute("testagain");

            // Get the commandLink to click from the second form
            HtmlAnchor commandLink1 = (HtmlAnchor) page.getElementById("b:commandLink1");
            page = commandLink1.click();

            resultingPage = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), page.asXml());

            // Verify that the input text from the first form contains the same value as the input text of the second form.
            assertTrue("Input text of the first form does not contain '" + textInput2.getValueAttribute() + "'",
                       resultingPage.contains("Enter a new value for input1: testagain"));
        }
    }

    /**
     * A test for the following JSF 2.3 Specification issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/790
     *
     * Test that cross form submit can be done using AJAX.
     *
     * First we drive a request to the application.
     *
     * Input a value to form 'a' and submit that value. Verify that the output text value
     * of the second form 'b' was re-rendered.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testSpecIssue790Test2() throws Exception {
        try (WebClient webClient = new WebClient()) {
            // Use a synchronizing ajax controller to allow proper ajax updating
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());

            // Construct the URL for the test
            String contextRoot = "JSF23Spec790";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "test2.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Get the input text of the first form and set a value
            HtmlTextInput textInput1 = (HtmlTextInput) page.getElementById("a:input1");
            textInput1.setValueAttribute("test");

            // Get the button to click from the first form
            HtmlSubmitInput submitButton = (HtmlSubmitInput) page.getElementById("a:submitButton1");
            page = submitButton.click();

            String resultingPage = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), resultingPage);
            Log.info(c, name.getMethodName(), page.asXml());

            // Verify that the input text from the second form contains the same value as the input text of the first form.
            assertTrue("Input text of the second form is not '" + textInput1.getValueAttribute() + "'",
                       resultingPage.contains("This is the value from outputText of form 'b': test"));
        }
    }

    /**
     * This test case uses an application that has the following context-parameter enabled:
     * javax.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE
     *
     * With the context-parameter enabled, and input fields with required set to true
     * validation should occur even when getSubmittedValue returns null. The only way
     * I've been able to get a getSubmittedValue to be null is to hack the response a bit.
     *
     * Setting the input1 name to "" and then submitting the page does the trick.
     *
     * When no values are submitted in either field we should see both requiredMessage values
     * displayed.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSpecIssue1433_PerformValidationRequiredTrueEnabled_EmptySubmit() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec1433True";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlInput input1 = (HtmlInput) page.getElementById("input1");
            input1.setAttribute("name", "");

            page = page.getElementById("button1").click();

            String pageText = page.asText();
            String expectedText1 = "Value one is required please enter value one!";
            String expectedText2 = "Value two is required please enter value two!";

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The page did not contain: " + expectedText1, pageText.contains(expectedText1));
            assertTrue("The page did not contain: " + expectedText2, pageText.contains(expectedText2));
        }
    }

    /**
     * This test case uses an application that has the following context-parameter enabled:
     * javax.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE
     *
     * With the context-parameter enabled, and input fields with required set to true
     * validation should occur even when getSubmittedValue returns null. The only way
     * I've been able to get a getSubmittedValue to be null is to hack the response a bit.
     *
     * Setting the input1 name to "" and then submitting the page does the trick.
     *
     * When values are submitted in both fields we should see the requiredMessage for
     * the first input only since getSubmittedValue for it was null since we changed
     * the name attribute before we submitted.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSpecIssue1433_PerformValidationRequiredTrueEnabled_ValueSubmit() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec1433True";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlInput input1 = (HtmlInput) page.getElementById("input1");
            input1.setAttribute("name", "");
            input1.setValueAttribute("test");

            HtmlInput input2 = (HtmlInput) page.getElementById("input2");
            input2.setValueAttribute("test");

            page = page.getElementById("button1").click();

            String pageText = page.asText();
            String expectedText = "Value one is required please enter value one!";
            String unexpectedText = "Value two is required please enter value two!";

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The page did not contain: " + expectedText, pageText.contains(expectedText));
            assertTrue("The page contains unexpected text: " + unexpectedText, !pageText.contains(unexpectedText));
        }
    }

    /**
     * This test case uses an application that has the following context-parameter disabled:
     * javax.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE
     *
     * With the context-parameter disabled, and input fields with required set to true
     * validation should not occur even when getSubmittedValue returns null. The only way
     * I've been able to get a getSubmittedValue to be null is to hack the response a bit.
     *
     * Setting the input1 name to "" and then submitting the page does the trick.
     *
     * When no values are submitted in either field we should see the second input
     * requiredMessage since no validation was done on the first input which had the
     * name attribute set to "".
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSpecIssue1433_PerformValidationRequiredTrueDisabled_EmptySubmit() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec1433False";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlInput input1 = (HtmlInput) page.getElementById("input1");
            input1.setAttribute("name", "");

            page = page.getElementById("button1").click();

            String pageText = page.asText();
            String expectedText = "Value two is required please enter value two!";
            String unexpectedText = "Value one is required please enter value one!";

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The page did not contain: " + expectedText, pageText.contains(expectedText));
            assertTrue("The page contains unexpected text: " + unexpectedText, !pageText.contains(unexpectedText));
        }
    }

    /**
     * This test case uses an application that has the following context-parameter disabled:
     * javax.faces.ALWAYS_PERFORM_VALIDATION_WHEN_REQUIRED_IS_TRUE
     *
     * With the context-parameter disabled, and input fields with required set to true
     * validation should not occur even when getSubmittedValue returns null. The only way
     * I've been able to get a getSubmittedValue to be null is to hack the response a bit.
     *
     * Setting the input1 name to "" and then submitting the page does the trick.
     *
     * When values are submitted in both fields we should not see a requiredMessage for
     * either input as a value was entered for each field and validation won't be
     * performed on the hacked input since the parameter is disabled.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSpecIssue1433_PerformValidationRequiredTrueDisabled_ValueSubmit() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "JSF23Spec1433False";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlInput input1 = (HtmlInput) page.getElementById("input1");
            input1.setValueAttribute("test");

            HtmlInput input2 = (HtmlInput) page.getElementById("input2");
            input2.setValueAttribute("test");

            page = page.getElementById("button1").click();

            String pageText = page.asText();
            String unexpectedText1 = "Value one is required please enter value one!";
            String unexpectedText2 = "Value two is required please enter value two!";

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure that we don't see any validation messages in this case
            assertTrue("Page contains unexpected text: " + unexpectedText1, !pageText.contains(unexpectedText1));
            assertTrue("Page contains unexpected text: " + unexpectedText2, !pageText.contains(unexpectedText2));
        }
    }

    /**
     * Test the new JSF 2.3 API methods described in the following two spec issues:
     * https://github.com/javaee/javaserverfaces-spec/issues/1404
     * https://github.com/javaee/javaserverfaces-spec/issues/1423
     *
     * ResourceHandler.markResourceRendered()
     * ResourceHandler.isResourceRendered()
     * UIViewRoot.getComponentResources()
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testSpecIssue1404And1423_JSF23NewAPIMethods() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "ResourceRendering";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlSubmitInput submitButton = (HtmlSubmitInput) page.getElementById("form1:submitButton");
            page = submitButton.click();

            String pageText = page.asText();
            String expectedText1 = "Message from CustomComponent: isResourceRendered library=css name=test-style.css --> false";
            String expectedText2 = "Message from CustomComponent: isResourceRendered library=css name=test-style.css --> true";
            String expectedText3 = "Message from CustomComponent: getComponentResources List size --> 2";
            String expectedText4 = "Message from MyBean: isResourceRendered library=css name=test-style.css --> true";
            String expectedText5 = "Message from MyBean: isResourceRendered library=css name=another.css --> false";
            String expectedText6 = "Message from MyBean: isResourceRendered library=another name=test-style.css --> false";

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            // Ensure that we don't see any validation messages in this case
            assertTrue("Page does not contain expected text: " + expectedText1, pageText.contains(expectedText1));
            assertTrue("Page does not contain expected text: " + expectedText2, pageText.contains(expectedText2));
            assertTrue("Page does not contain expected text: " + expectedText3, pageText.contains(expectedText3));
            assertTrue("Page does not contain expected text: " + expectedText4, pageText.contains(expectedText4));
            assertTrue("Page does not contain expected text: " + expectedText5, pageText.contains(expectedText5));
            assertTrue("Page does not contain expected text: " + expectedText6, pageText.contains(expectedText6));
        }
    }

    /**
     * Test the CDI-JSF integration.
     *
     * In this test we want make sure that a custom ViewHandler
     * and a custom Application can be used in an app.
     *
     * Also, make sure that the IBMViewHandler is used
     * when CDI is enabled.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testCDIIntegration() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            String contextRoot = "CDIIntegrationTest";
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            String responseText = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), responseText);
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("Page does not contain expected response.", responseText.contains("CDI Integration Test"));

            assertTrue("The Custom ApplicationFactory was not invoked.",
                       !jsf23CDIServer.findStringsInTrace("CustomApplicationFactory was invoked!").isEmpty());

            assertTrue("The Custom Application was not invoked.",
                       !jsf23CDIServer.findStringsInTrace("CustomApplication was invoked!").isEmpty());

            assertTrue("The Custom ViewHandler was not invoked.",
                       !jsf23CDIServer.findStringsInTrace("CustomViewHandler was invoked!").isEmpty());

            assertTrue("The IBMViewHandler was not used.",
                       !jsf23CDIServer.findStringsInTrace("set ViewHandler =.*IBMViewHandler").isEmpty());
        }
    }
}
