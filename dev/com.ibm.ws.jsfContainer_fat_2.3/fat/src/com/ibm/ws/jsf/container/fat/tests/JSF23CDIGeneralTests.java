/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jsf.container.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.CollectingAlertHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf.container.fat.FATSuite;
import com.ibm.ws.jsf.container.fat.utils.JSFUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * General JSF 2.3 test cases the also require CDI.
 */
@RunWith(FATRunner.class)
public class JSF23CDIGeneralTests extends FATServletClient {

    private static final Class<?> c = JSF23CDIGeneralTests.class;
    private static final String POST_RENDER_VIEW_EVENT_APP_NAME = "PostRenderViewEvent";
    private static final String CDI_MANAGED_PROPERTY_APP_NAME = "CDIManagedProperty";
    private static final String EL_IMPLICIT_OBJECTS_CDI_APP_NAME = "ELImplicitObjectsViaCDI";
    private static final String CONVERTER_VALIDATOR_BEHAVIOR_INJECTION_TARGET_APP_NAME = "ConverterValidatorBehaviorInjectionTarget";
    private static final String VIEW_HANDLER_APP_NAME = "ViewHandlerTest";
    private static final String CONVERSATION_SCOPED_APP_NAME = "ConversationScopedTest";

    @Rule
    public TestName name = new TestName();

    @Server("jsf.container.2.3_fat.cdi")
    public static LibertyServer jsf23CDIServer;

    private static boolean isEE9;
    private static boolean isEE10;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive postRenderViewEventApp = ShrinkWrap.create(WebArchive.class, POST_RENDER_VIEW_EVENT_APP_NAME + ".war").addPackages(false, "com.ibm.ws.jsf23.fat.prve.events");
        postRenderViewEventApp = FATSuite.addMyFaces(postRenderViewEventApp);
        postRenderViewEventApp = (WebArchive) ShrinkHelper.addDirectory(postRenderViewEventApp, "publish/files/permissions");
        postRenderViewEventApp = (WebArchive) ShrinkHelper.addDirectory(postRenderViewEventApp, "test-applications/" + POST_RENDER_VIEW_EVENT_APP_NAME + "/resources");
        ShrinkHelper.exportDropinAppToServer(jsf23CDIServer, postRenderViewEventApp);

        WebArchive cdiManagedPropertyApp = ShrinkWrap.create(WebArchive.class, CDI_MANAGED_PROPERTY_APP_NAME + ".war")
                        .addPackages(false, "com.ibm.ws.jsf23.fat.cdi.managedproperty");
        cdiManagedPropertyApp = FATSuite.addMyFaces(cdiManagedPropertyApp);
        cdiManagedPropertyApp = (WebArchive) ShrinkHelper.addDirectory(cdiManagedPropertyApp, "publish/files/permissions");
        cdiManagedPropertyApp = (WebArchive) ShrinkHelper.addDirectory(cdiManagedPropertyApp, "test-applications/" + CDI_MANAGED_PROPERTY_APP_NAME + "/resources");
        ShrinkHelper.exportDropinAppToServer(jsf23CDIServer, cdiManagedPropertyApp);

        WebArchive elImplicitObjectsCDIAPP = ShrinkWrap.create(WebArchive.class, EL_IMPLICIT_OBJECTS_CDI_APP_NAME + ".war")
                        .addPackages(false, "com.ibm.ws.jsf23.fat.elcdi.beans");
        elImplicitObjectsCDIAPP = FATSuite.addMyFaces(elImplicitObjectsCDIAPP);
        elImplicitObjectsCDIAPP = (WebArchive) ShrinkHelper.addDirectory(elImplicitObjectsCDIAPP, "publish/files/permissions");
        elImplicitObjectsCDIAPP = (WebArchive) ShrinkHelper.addDirectory(elImplicitObjectsCDIAPP, "test-applications/" + EL_IMPLICIT_OBJECTS_CDI_APP_NAME + "/resources");
        ShrinkHelper.exportDropinAppToServer(jsf23CDIServer, elImplicitObjectsCDIAPP);

        WebArchive converterValidatorBehaviorInjectionTargetApp = ShrinkWrap.create(WebArchive.class, CONVERTER_VALIDATOR_BEHAVIOR_INJECTION_TARGET_APP_NAME + ".war")
                        .addPackages(false, "com.ibm.ws.jsf23.fat.converter_validator.beans");
        converterValidatorBehaviorInjectionTargetApp = FATSuite.addMyFaces(converterValidatorBehaviorInjectionTargetApp);
        converterValidatorBehaviorInjectionTargetApp = (WebArchive) ShrinkHelper.addDirectory(converterValidatorBehaviorInjectionTargetApp, "publish/files/permissions");
        converterValidatorBehaviorInjectionTargetApp = (WebArchive) ShrinkHelper
                        .addDirectory(converterValidatorBehaviorInjectionTargetApp, "test-applications/" + CONVERTER_VALIDATOR_BEHAVIOR_INJECTION_TARGET_APP_NAME + "/resources");
        ShrinkHelper.exportDropinAppToServer(jsf23CDIServer, converterValidatorBehaviorInjectionTargetApp);

        WebArchive viewHandlerApp = ShrinkWrap.create(WebArchive.class, VIEW_HANDLER_APP_NAME + ".war")
                        .addPackages(false, "jsf.container.viewhandlertest");
        viewHandlerApp = FATSuite.addMyFaces(viewHandlerApp);
        viewHandlerApp = (WebArchive) ShrinkHelper.addDirectory(viewHandlerApp, "publish/files/permissions");
        viewHandlerApp = (WebArchive) ShrinkHelper
                        .addDirectory(viewHandlerApp, "test-applications/" + VIEW_HANDLER_APP_NAME + "/resources");
        ShrinkHelper.exportDropinAppToServer(jsf23CDIServer, viewHandlerApp);

        WebArchive conversationScopedApp = ShrinkWrap.create(WebArchive.class, CONVERSATION_SCOPED_APP_NAME + ".war")
                        .addPackages(false, "com.ibm.ws.jsf.conversationscoped.bean");
        conversationScopedApp = FATSuite.addMyFaces(conversationScopedApp);
        conversationScopedApp = (WebArchive) ShrinkHelper.addDirectory(conversationScopedApp, "publish/files/permissions");
        conversationScopedApp = (WebArchive) ShrinkHelper
                        .addDirectory(conversationScopedApp, "test-applications/" + CONVERSATION_SCOPED_APP_NAME + "/resources");
        ShrinkHelper.exportDropinAppToServer(jsf23CDIServer, conversationScopedApp);

        // Start the server and use the class name so we can find logs easily.
        jsf23CDIServer.startServer(JSF23CDIGeneralTests.class.getSimpleName() + ".log");

        isEE9 = JakartaEE9Action.isActive();
        isEE10 = JakartaEE10Action.isActive();
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
    @Test
    @Mode(TestMode.FULL)
    public void testPostRenderViewEvent() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, POST_RENDER_VIEW_EVENT_APP_NAME, "");

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
    @Test
    @Mode(TestMode.FULL)
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
        try (WebClient webClient = new WebClient()) {

            String initalValue = "numberManagedProperty = 0 textManagedProperty = zero "
                                 + "listManagedProperty = zero stringArrayManagedProperty = "
                                 + "zero bean = com.ibm.ws.jsf23.fat.cdi.managedproperty.TestBean";

            String finalValue = "numberManagedProperty = 1 textManagedProperty = 2 "
                                + "listManagedProperty = 3 stringArrayManagedProperty = 4 bean = "
                                + "com.ibm.ws.jsf23.fat.cdi.managedproperty.TestBean";

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, CDI_MANAGED_PROPERTY_APP_NAME, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            HtmlInput input1 = (HtmlInput) page.getElementById("in1");
            HtmlInput input2 = (HtmlInput) page.getElementById("in2");
            HtmlInput input3 = (HtmlInput) page.getElementById("in3");
            HtmlInput input4 = (HtmlInput) page.getElementById("in4");

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
            page = ((HtmlElement) page.getElementById("button1")).click();

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
     * @throws Exception
     */
    @Test
    public void testInjectableELImplicitObjects() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Add a message to the header map
            webClient.addRequestHeader("headerMessage", "This is a test");

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, EL_IMPLICIT_OBJECTS_CDI_APP_NAME, "index.xhtml");

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
            assertTrue(resultPage.asText()
                            .contains("Flow map object is null: Exception: WELD-001303: No active contexts "
                                      + "for scope type " + (isEE10 || isEE9 ? "jakarta." : "javax.") + "faces.flow.FlowScoped")); // Expected exception
            assertTrue(resultPage.asText().contains("Message from HeaderMap: This is a test"));
            assertTrue(resultPage.asText().contains("Cookie object from CookieMap: " + (isEE10 || isEE9 ? "jakarta." : "javax.") + "servlet.http.Cookie"));
            assertTrue(resultPage.asText().contains("WELD_CONTEXT_ID_KEY from InitParameterMap: ELImplicitObjectsViaCDI"));
            assertTrue(resultPage.asText().contains("Message from RequestParameterMap: Hello World"));
            assertTrue(resultPage.asText().contains("Message from RequestParameterValuesMap: [Hello World]"));
            assertTrue(resultPage.asText().contains("Message from HeaderValuesMap: [This is a test]"));
            assertTrue(resultPage.asText().contains("Resource handler JSF_SCRIPT_LIBRARY_NAME constant: " + (isEE10 || isEE9 ? "jakarta." : "javax.") + "faces"));
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
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, EL_IMPLICIT_OBJECTS_CDI_APP_NAME, "implicit_objects.xhtml?message=Hello World");

            HtmlPage testELResolutionImplicitObjectsPage = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), testELResolutionImplicitObjectsPage.asText());
            Log.info(c, name.getMethodName(), testELResolutionImplicitObjectsPage.asXml());

            // Verify that the page contains the expected messages.
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("JSF 2.3 EL resolution of implicit objects using CDI"));
            assertTrue(testELResolutionImplicitObjectsPage.asText().contains("Bean: com.ibm.ws.jsf23.fat.elcdi.beans.ELImplicitObjectBean"));
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
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, EL_IMPLICIT_OBJECTS_CDI_APP_NAME, "flow_index.xhtml");

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
     * Test that a CDI Managed Bean can be injected in a FacesConverter.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testFacesConverterBeanInjection() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, CONVERTER_VALIDATOR_BEHAVIOR_INJECTION_TARGET_APP_NAME, "index.xhtml");

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
    @Mode(TestMode.FULL)
    public void testFacesValidatorBeanInjection() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, CONVERTER_VALIDATOR_BEHAVIOR_INJECTION_TARGET_APP_NAME, "index.xhtml");

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
    @Mode(TestMode.FULL)
    public void testFacesBehaviorBeanInjection() throws Exception {
        try (WebClient webClient = new WebClient()) {
            CollectingAlertHandler alertHandler = new CollectingAlertHandler();
            webClient.setAlertHandler(alertHandler);

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, CONVERTER_VALIDATOR_BEHAVIOR_INJECTION_TARGET_APP_NAME, "index.xhtml");

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
    @Mode(TestMode.FULL)
    public void testConverterValidatorBehaviorObjectInjection() throws Exception {
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23CDIServer, CONVERTER_VALIDATOR_BEHAVIOR_INJECTION_TARGET_APP_NAME, "JSFArtifactsInjection.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Verify that the page contains the expected response.
            // TestConverter, TestValidator and TestBehavior objects should have been injected
            assertTrue(page.asText().contains("JSF 2.3 support injection of JSF Managed Objects: FacesConverter, FacesValidator, FacesBehavior"));
            assertTrue(page.asText().contains("com.ibm.ws.jsf23.fat.converter_validator.beans.TestConverter"));

            assertTrue(page.asText().contains("com.ibm.ws.jsf23.fat.converter_validator.beans.TestValidator"));
            assertTrue(page.asText().contains("com.ibm.ws.jsf23.fat.converter_validator.beans.TestBehavior"));
        }
    }

    @Test
    @Mode(TestMode.FULL)
    public void testViewHandler() throws Exception {

        // Construct the URL for the test
        URL url = JSFUtils.createHttpUrl(jsf23CDIServer, VIEW_HANDLER_APP_NAME, "index.xhtml");

        try (WebClient webClient = new WebClient()) {
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
                       !jsf23CDIServer.findStringsInTrace("setViewHandler Setting IBM View Handler").isEmpty());
        }
    }

    @Test
    @Mode(TestMode.FULL)
    public void testConversationScoped() throws Exception {

        // Construct the URL for the test
        URL url = JSFUtils.createHttpUrl(jsf23CDIServer, CONVERSATION_SCOPED_APP_NAME, "index.xhtml");

        try (WebClient webClient = new WebClient()) {
            HtmlPage indexPage = (HtmlPage) webClient.getPage(url);

            //Get index page & click increment button
            HtmlForm form = indexPage.getFormByName("form1");
            HtmlSubmitInput submitButton = form.getInputByName("form1:incrementButton");
            indexPage = submitButton.click();

            String responseText = indexPage.asText();
            assertTrue("Page does not contain expected response.", responseText.contains("Value is 1"));

            //Click Continue to go to page2.xhtml
            form = indexPage.getFormByName("form1");
            submitButton = form.getInputByName("form1:continueButton");
            HtmlPage page2 = submitButton.click();

            //Ensure value is still 1 for conversation scoped bean
            responseText = page2.asText();
            assertTrue("Page does not contain expected response.", responseText.contains("Value is 1"));

            //End conversation and redirect to index.xhtml
            form = page2.getFormByName("form2");
            submitButton = form.getInputByName("form2:endButton");
            indexPage = submitButton.click();

            //Ensure bean counter resets to 0 when conversation scope ends.
            responseText = indexPage.asText();
            assertTrue("Page does not contain expected response.", responseText.contains("Value is 0"));
        }
    }
}
