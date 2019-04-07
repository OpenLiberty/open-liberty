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
package com.ibm.ws.jsf.container.fat.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf.container.fat.utils.JSFApplication;
import com.ibm.ws.jsf.container.fat.utils.JSFImplementation;
import com.ibm.ws.jsf.container.fat.utils.JSFUtils;
import com.ibm.ws.jsf.container.fat.utils.UseImplementation;
import com.ibm.ws.jsf.container.fat.utils.WebArchiveInfo;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * General JSF 2.3 test cases the also require CDI.
 */
@RunWith(FATRunner.class)
@UseImplementation(JSFImplementation.MYFACES)
public class JSF23CDIGeneralTests extends FATServletClient {

    protected static final Class<?> c = JSF23CDIGeneralTests.class;

    @Rule
    public TestName name = new TestName();

    @Rule
    public JSFApplication jsfApplication = new JSFApplication(jsf23CDIServer);

    @Server("jsf.container.2.3_fat.cdi")
    public static LibertyServer jsf23CDIServer;

    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and use the class name so we can find logs easily.
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
    @Test
    @Mode(TestMode.FULL)
    @WebArchiveInfo(name = "PostRenderViewEvent", pkgs = { "com.ibm.ws.jsf23.fat.prve.events" })
    public void testPostRenderViewEvent() throws Exception {
        String contextRoot = "PostRenderViewEvent";
        WebClient webClient = new WebClient();

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
    @WebArchiveInfo(name = "PostRenderViewEvent", pkgs = { "com.ibm.ws.jsf23.fat.prve.events" })
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
    @WebArchiveInfo(name = "CDIManagedProperty", pkgs = { "com.ibm.ws.jsf23.fat.cdi.managedproperty" })
    public void testCDIManagedProperty() throws Exception {
        String contextRoot = "CDIManagedProperty";
        WebClient webClient = new WebClient();
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

    /**
     * Test to ensure that the EL implicit objects can be injected through CDI.
     *
     * @throws Exception
     */
    @Test
    @WebArchiveInfo(name = "ELImplicitObjectsViaCDI", pkgs = { "com.ibm.ws.jsf23.fat.elcdi.beans" })
    public void testInjectableELImplicitObjects() throws Exception {
        WebClient webClient = new WebClient();

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
        assertTrue(resultPage.asText().contains("Flow map object is null: Exception: WELD-001303: No active contexts "
                                                + "for scope type javax.faces.flow.FlowScoped")); // Expected exception
        assertTrue(resultPage.asText().contains("Message from HeaderMap: This is a test"));
        assertTrue(resultPage.asText().contains("Cookie object from CookieMap: javax.servlet.http.Cookie"));
        assertTrue(resultPage.asText().contains("WELD_CONTEXT_ID_KEY from InitParameterMap: ELImplicitObjectsViaCDI"));
        assertTrue(resultPage.asText().contains("Message from RequestParameterMap: Hello World"));
        assertTrue(resultPage.asText().contains("Message from RequestParameterValuesMap: [Hello World]"));
        assertTrue(resultPage.asText().contains("Message from HeaderValuesMap: [This is a test]"));
        assertTrue(resultPage.asText().contains("Resource handler JSF_SCRIPT_LIBRARY_NAME constant: javax.faces"));
    }

    /**
     * Test to ensure that the EL Resolution of implicit objects works as expected
     * when CDI is being used.
     *
     * @throws Exception
     */
    @Test
    @WebArchiveInfo(name = "ELImplicitObjectsViaCDI", pkgs = { "com.ibm.ws.jsf23.fat.elcdi.beans" })
    public void testELResolutionImplicitObjects() throws Exception {
        WebClient webClient = new WebClient();

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

    /**
     * Test the EL Resolution of implicit object #{flowScope}
     *
     * @throws Exception
     */
    @Test
    @WebArchiveInfo(name = "ELImplicitObjectsViaCDI", pkgs = { "com.ibm.ws.jsf23.fat.elcdi.beans" })
    public void testELResolutionOfFlowScope() throws Exception {
        WebClient webClient = new WebClient();

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

    /**
     * Test that a CDI Managed Bean can be injected in a FacesConverter.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @WebArchiveInfo(name = "ConverterValidatorBehaviorInjectionTarget", pkgs = { "com.ibm.ws.jsf23.fat.converter_validator.beans" })
    public void testFacesConverterBeanInjection() throws Exception {
        WebClient webClient = new WebClient();

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

    /**
     * Test that a CDI Managed Bean can be injected in a FacesValidator.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @WebArchiveInfo(name = "ConverterValidatorBehaviorInjectionTarget", pkgs = { "com.ibm.ws.jsf23.fat.converter_validator.beans" })
    public void testFacesValidatorBeanInjection() throws Exception {
        WebClient webClient = new WebClient();

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

    /**
     * Test that a CDI Managed Bean can be injected in a FacesBehavior.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @WebArchiveInfo(name = "ConverterValidatorBehaviorInjectionTarget", pkgs = { "com.ibm.ws.jsf23.fat.converter_validator.beans" })
    public void testFacesBehaviorBeanInjection() throws Exception {
        WebClient webClient = new WebClient();
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

    /**
     * Test that a FacesConverter, a FacesValidator and a FacesBehavior can be injected in a Managed Bean
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    @WebArchiveInfo(name = "ConverterValidatorBehaviorInjectionTarget", pkgs = { "com.ibm.ws.jsf23.fat.converter_validator.beans" })
    public void testConverterValidatorBehaviorObjectInjection() throws Exception {
        WebClient webClient = new WebClient();

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
        assertTrue(page.asText().contains("com.ibm.ws.jsf23.fat.converter_validator.beans.TestConverter"));

        assertTrue(page.asText().contains("com.ibm.ws.jsf23.fat.converter_validator.beans.TestValidator"));
        assertTrue(page.asText().contains("com.ibm.ws.jsf23.fat.converter_validator.beans.TestBehavior"));
    }

    @Test
    @Mode(TestMode.FULL)
    @WebArchiveInfo(name = "ViewHandlerTest", pkgs = { "jsf.container.viewhandlertest" })
    public void testViewHandler() throws Exception {
        String contextRoot = "ViewHandlerTest";

        // Wait for the application to be started.
        jsf23CDIServer.waitForStringInLog("CWWKZ0001I: Application " + contextRoot + " started");

        // Construct the URL for the test
        URL url = JSFUtils.createHttpUrl(jsf23CDIServer, contextRoot, "index.xhtml");

        WebClient webClient = new WebClient();
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
