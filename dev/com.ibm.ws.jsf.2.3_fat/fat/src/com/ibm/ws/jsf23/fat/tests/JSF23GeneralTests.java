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

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jsf23.fat.JSFUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * General JSF 2.3 test cases.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF23GeneralTests {

    protected static final Class<?> c = JSF23GeneralTests.class;

    @Rule
    public TestName name = new TestName();

    @Server("jsf23Server")
    public static LibertyServer jsf23Server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(jsf23Server, "FacesServletExactMapping.war");
        ShrinkHelper.defaultDropinApp(jsf23Server, "JSF23GeneralTests.war", "com.ibm.ws.jsf23.fat.generaltests.listeners");

        // Create the PhaseListenerExceptionJar that is used in PerViewPhaseListenerDoNotQueueException.war,
        // PerViewPhaseListenerQueueException.war, and GlobalPhaseListenerQueueException.war
        JavaArchive phaseListenerExceptionJar = ShrinkWrap.create(JavaArchive.class, "PhaseListenerException.jar");
        phaseListenerExceptionJar.addPackage("com.ibm.ws.jsf23.fat.phaselistener.exception");

        // Create the PerViewPhaseListenerDoNotQueueException.war application
        WebArchive perViewPhaseListenerDoNotQueueExceptionWar = ShrinkWrap.create(WebArchive.class, "PerViewPhaseListenerDoNotQueueException.war");
        perViewPhaseListenerDoNotQueueExceptionWar.addAsLibrary(phaseListenerExceptionJar);
        ShrinkHelper.addDirectory(perViewPhaseListenerDoNotQueueExceptionWar, "test-applications/" + "PerViewPhaseListenerDoNotQueueException.war" + "/resources");
        ShrinkHelper.exportToServer(jsf23Server, "dropins", perViewPhaseListenerDoNotQueueExceptionWar);

        // Create the PerViewPhaseListenerQueueException.war application
        WebArchive perViewPhaseListenerQueueExceptionWar = ShrinkWrap.create(WebArchive.class, "PerViewPhaseListenerQueueException.war");
        perViewPhaseListenerQueueExceptionWar.addAsLibrary(phaseListenerExceptionJar);
        ShrinkHelper.addDirectory(perViewPhaseListenerQueueExceptionWar, "test-applications/" + "PerViewPhaseListenerQueueException.war" + "/resources");
        ShrinkHelper.exportToServer(jsf23Server, "dropins", perViewPhaseListenerQueueExceptionWar);

        // Create the GlobalPhaseListenerQueueException.war application
        WebArchive globalPhaseListenerQueueExceptionWar = ShrinkWrap.create(WebArchive.class, "GlobalPhaseListenerQueueException.war");
        globalPhaseListenerQueueExceptionWar.addAsLibrary(phaseListenerExceptionJar);
        ShrinkHelper.addDirectory(globalPhaseListenerQueueExceptionWar, "test-applications/" + "GlobalPhaseListenerQueueException.war" + "/resources");
        ShrinkHelper.exportToServer(jsf23Server, "dropins", globalPhaseListenerQueueExceptionWar);

        ShrinkHelper.defaultDropinApp(jsf23Server, "JSF23Spec1430.war", "com.ibm.ws.jsf23.fat.spec1430");
        ShrinkHelper.defaultDropinApp(jsf23Server, "JSF23Spec1346.war", "com.ibm.ws.jsf23.fat.spec1346");
        ShrinkHelper.defaultDropinApp(jsf23Server, "JSF23DisableFacesServletToXhtml.war");
        ShrinkHelper.defaultDropinApp(jsf23Server, "JSF23ViewActionFlowEntry.war");
        ShrinkHelper.defaultDropinApp(jsf23Server, "JSF23Spec1113.war");

        // Start the server and use the class name so we can find logs easily.
        // Many tests use the same server.
        jsf23Server.startServer(JSF23GeneralTests.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (jsf23Server != null && jsf23Server.isStarted()) {
            jsf23Server.stopServer();
        }
    }

    /**
     * This test case ensures that the FacesServlet can be mapped to an extenionless mapping.
     * For example we want to ensure that we can drive a request and invoke the FacesServlet
     * when the url used does not have suffix or prefix mapping and is an exact match.
     *
     * For this test case we configure the following mappings for the FacesServlet in the web.xml:
     *
     * <url-pattern>exactMapping</url-pattern>
     * <url-pattern>test/exactMapping</url-pattern>
     *
     * The test case drive a request to each of the above mappings and ensures that the correct
     * exactMapping.xhtml facelet is invoked.
     *
     * @throws Exception
     */
    @Test
    public void testFacesServletExactMapping() throws Exception {
        String contextRoot = "FacesServletExactMapping";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "exactMapping");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The correct page was not invoked.", page.getElementById("form1:out1").getTextContent().equals("exactMapping.xhtml invoked"));

            url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "test/exactMapping");

            page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The correct page was not invoked.", page.getElementById("form1:out1").getTextContent().equals("test/exactMapping.xhtml invoked"));
        }
    }

    /**
     * This test will ensure that the spec defined API constants are correct.
     *
     * @throws Exception
     */
    @Test
    public void testAPIConstants() throws Exception {
        String contextRoot = "JSF23GeneralTests";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "JSF23APIConstants.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Check the value of each API constant
            String output = page.getElementById("out1").getTextContent();
            assertTrue("The value of javax.faces.application.ResourceHandler.JSF_SCRIPT_RESOURCE_NAME was incorrect: " + output,
                       output.equals("jsf.js"));

            output = page.getElementById("out2").getTextContent();

            String expected = "javax.faces";

            if(JakartaEE9Action.isActive()){
              expected = "jakarta.faces";
            }

            assertTrue("The value of javax.faces.application.ResourceHandler.JSF_SCRIPT_LIBRARY_NAME was incorrect: " + output,
                       output.equals(expected));

            output = page.getElementById("out3").getTextContent();
            assertTrue("The value of javax.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_SOURCE_PARAM_NAME was incorrect: " + output,
                       output.equals(expected + ".source"));

            output = page.getElementById("out4").getTextContent();
            assertTrue("The value of javax.faces.component.behavior.ClientBehaviorContext.BEHAVIOR_EVENT_PARAM_NAME was incorrect: " + output,
                       output.equals(expected +".behavior.event"));

            output = page.getElementById("out5").getTextContent();
            assertTrue("The value of javax.faces.context.PartialViewContext.PARTIAL_EVENT_PARAM_NAME was incorrect: " + output,
                       output.equals(expected +".partial.event"));
        }
    }

    /**
     * New to JSF 2.3 is the SystemEvent.getFacesContext() method. To test this we have a
     * SystemEventListener that listens for a SystemEvent and uses the new getFacesContext method
     * in the processEvent method to log a message.
     *
     * public void processEvent(SystemEvent arg0) {
     * // Use the SystemEvent.getFacesContext() method new to JSF 2.3.
     * arg0.getFacesContext().getExternalContext().log("PostConstructApplicationEventListener processEvent invoked!!");
     * }
     *
     * @throws Exception
     */
    @Test
    public void testSystemEventGetFacesContextMethod() throws Exception {
        List<String> result = jsf23Server.findStringsInLogs("PostConstructApplicationEventListener processEvent invoked!!");
        assertTrue("The SystemEvent.getFacesContext() method did not work.", result.size() == 1);
    }

    /**
     * New to JSF 2.3 is the FacesEvent.getFacesContext() method. To test this we have an
     * ActionListener that is invoked and uses the new getFacesContext method
     * in the processAction method to log a message.
     *
     * public void processAction(ActionEvent arg0) throws AbortProcessingException {
     * // Use the FacesEvent.getFacesContext() method new to JSF 2.3.
     * arg0.getFacesContext().getExternalContext().log("TestActionListener processAction invoked!!");
     * }
     *
     * @throws Exception
     */
    @Test
    public void testFacesEventGetFacesContextMethod() throws Exception {
        String contextRoot = "JSF23GeneralTests";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "JSF23FacesEvent.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Now click the submit button
            page.getElementById("button1").click();

            List<String> result = jsf23Server.findStringsInLogs("TestActionListener processAction invoked!!");
            assertTrue("The FacesEvent.getFacesContext() method did not work.", result.size() == 1);
        }
    }

    /**
     * This test ensures that if a per view PhaseListener is registered and an exception is
     * thrown from the PhaseListener the exception can be handled by an ExceptionHandler.
     *
     * The following context-parameter must be enabled for this to work:
     *
     * javax.faces.VIEWROOT_PHASE_LISTENER_QUEUES_EXCEPTIONS
     *
     * @throws Exception
     */
    @Test
    public void testPerViewPhaseListenerQueueExceptionEnabled() throws Exception {
        String contextRoot = "PerViewPhaseListenerQueueException";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The error page was not displayed.", page.asText().equals("ERROR PAGE"));
        }
    }

    /**
     * This test ensures that if a per view PhaseListener is registered and an exception is
     * thrown from the PhaseListener the exception is not queued and is swallowed.
     *
     * The following context-parameter must not be enabled:
     *
     * javax.faces.VIEWROOT_PHASE_LISTENER_QUEUES_EXCEPTIONS
     *
     * @throws Exception
     */
    @Test
    public void testPerViewPhaseListnerQueueExceptionDefault() throws Exception {
        String contextRoot = "PerViewPhaseListenerDoNotQueueException";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // The page should load successfully even though an exception was thrown in the
            // PhaseListener.
            assertTrue("The exception was not handled correctly.", page.asText().contains("Hello World"));
        }
    }

    /**
     * This test ensures that if a Global PhaseListener is registered and an exception is thrown
     * from the PhaseListener the exception can be handled by an ExceptionHandler.
     *
     * @throws Exception
     */
    @Test
    public void testGlobalPhaseListenerException() throws Exception {
        String contextRoot = "GlobalPhaseListenerQueueException";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The error page was not displayed.", page.asText().equals("ERROR PAGE"));
        }
    }

    /**
     * This test ensures that the <h:button/> disabled attributed works.
     *
     * Drive a request to the page and then verify that the html has the following:
     *
     * disabled="disabled"
     *
     */
    @Test
    public void testButtonDisabledAttriute() throws Exception {
        String contextRoot = "JSF23GeneralTests";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "JSF23ButtonDisabledAttribute.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            String pageXml = page.asXml();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), pageXml);

            assertTrue("The disabled attributed did not work as expected, the html did not contain: disabled=\"disabled\": " + pageXml,
                       pageXml.contains("disabled=\"disabled\""));
        }
    }

    /**
     * A test for the following JSF 2.3 Specification issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/1430
     *
     * There are two parts to this test.
     *
     * First we test that the @ResourceDependency annotation used in a repeatable way
     * works as expected. The page that the request is driven to should have two stylesheets.
     *
     * Second we test that the @ListenerFor annotation used in a repeatable way
     * works as expected. The page after a reload should resolve two values from the
     * RequestMap.
     *
     * @throws Exception
     */
    @Test
    public void testRepeated_ListenerFor_ResourceDependency_SpecIssue1430() throws Exception {
        String contextRoot = "JSF23Spec1430";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), page.asXml());

            // Now click the submit button
            page = page.getElementById("button").click();

            // Log the page for debugging if necessary in the future
            String pageXml = page.asXml();
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), pageXml);

            // If both of the @ResourceDependencies were not resolved then these two checks would fail.
            assertTrue("The page did not contain the test-style.css stylesheet.", pageXml.contains("test-style.css"));
            assertTrue("The page did not contain the test-style2.css stylesheet.", pageXml.contains("test-style2.css"));

            // If both of the @ListenerFor were not resolved then these two checks would fail.
            assertTrue("The page did not contain preValidateEvent.", pageXml.contains("preValidateEvent"));
            assertTrue("The page did not contain postValidateEvent.", pageXml.contains("postValidateEvent"));
        }
    }

    /**
     * A simple test for the following JSF 2.3 Specification Issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/1258
     *
     * We drive a request to a page that contains an <h:outputText> component
     * within a <script> tag as well as outside and see if the value passed
     * in the query parameter is escaped or not.
     *
     * It should be escaped outside of the <script> tag but not escaped within
     * the <script> tag. This has been the behavior for awhile but
     * it is now documented within the JSF 2.3 Specification.
     *
     * @throws Exception
     */
    @Test
    public void testSpecIssue1258() throws Exception {
        String contextRoot = "JSF23GeneralTests";
        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setThrowExceptionOnScriptError(false);

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "JSF23SpecIssue1258.xhtml");

            WebRequest requestSettings = new WebRequest(url, HttpMethod.POST);
            requestSettings.setRequestParameters(new ArrayList());
            requestSettings.getRequestParameters().add(new NameValuePair("test", "<test>"));

            HtmlPage page = (HtmlPage) webClient.getPage(requestSettings);

            String pageXml = page.asXml();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), pageXml);

            assertTrue("The value within the script should be: Within Script: <test>", pageXml.contains("Within Script: <test>"));
            assertTrue("The value outside the script should be: Outside Script: &lt;test&gt;", pageXml.contains("Outside Script: &lt;test&gt;"));
        }
    }

    /**
     * A simple test for the following JSF 2.3 Specification Issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/1346
     *
     * This test ensures that we can use a Custom FaceletCacheFactory and
     * FaceletCache. The Spec issue is more to ensure that reflection does not
     * need to be used in the JSF implementation but we can at least ensure that
     * a custom implementation can be used.
     *
     * @throws Exception
     */
    @Test
    public void testSpecIssue1346() throws Exception {
        String contextRoot = "JSF23Spec1346";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            String pageText = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            assertTrue("The CustomFaceletCacheFactory was not invoked.", pageText.contains("CustomFaceletCacheFactory: getFaceletCache Invoked!"));
            assertTrue("The CustomFaceletCache was not invoked.", pageText.contains("CustomFaceletCache: getFacelet Invoked!"));
        }
    }

    /**
     * This test case uses an application that does not define a FacesServlet in the web.xml.
     * The application also enabled the following context parameter:
     *
     * javax.faces.DISABLE_FACESSERVLET_TO_XHTML
     *
     * A request driven to index.xhtml should not go through the FacesServlet and the expression
     * should not be evaluated. The *.jsf mapping should have been added by default so a request
     * to index.jsf should go through the FacesServlet and the expression should be evaluated.
     *
     * @throws Exception
     */
    @Test
    public void testDisableFacesServletToXhtml() throws Exception {
        String contextRoot = "JSF23DisableFacesServletToXhtml";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            String pageText = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            // If the .xhtml mapping was not added, which is the expected behavior in this instance
            // since we have disabled the addition via the context parameter then the expression should
            // not be evaluated.
            assertTrue("The .xhtml mapping was added and it should not have been.", !pageText.contains("4"));

            url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "index.jsf");

            page = (HtmlPage) webClient.getPage(url);

            pageText = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            // The .jsf mapping should have been added, which is expected. The expression should be
            // evaluated in this case. If the expression was not evaluated then the mapping was not
            // properly added.
            assertTrue("The .jsf mapping should have been added but was not.", pageText.contains("4"));
        }
    }

    /**
     * This test case ensures a JSF flow can be entered via a viewAction call.
     * JSF 2.3 Spec Issue: https://github.com/javaee/javaserverfaces-spec/issues/1403
     *
     * @throws Exception
     */
    @Test
    public void testViewActionFlowEntry() throws Exception {
        String contextRoot = "JSF23ViewActionFlowEntry";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "JSF23ViewActionFlow_index.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            String pageText = page.asText();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            // The viewAction should have been called and the page redirected to the first page in the flow
            assertTrue("The viewAction did not redirect to the flow", (pageText.contains("Flow Id: simple") && pageText.contains("In flow ? true")));

            String queryString = page.getElementById("queryStringText").getTextContent();
            //log the query string
            Log.info(c, name.getMethodName(), "Query String: " + queryString);

            //Spec Issue 1403 ( https://github.com/javaee/javaserverfaces-spec/issues/1403 ) states that when entering a
            //flow from a viewAction, the query string must contain "jffi" and "jftfdi".
            assertTrue("The expected parameters (jffi and jftfdi) were not in the query string.", (queryString.contains("jffi") && queryString.contains("jftfdi")));

            // Get the form that we are dealing with
            HtmlForm form = page.getFormByName("form");
            // Get the submit button and input text
            HtmlSubmitInput submitButton = form.getInputByName("button1");
            HtmlTextInput inputText = (HtmlTextInput) form.getInputByName("inputValue");

            // Fill the input text
            inputText.setValueAttribute("test");

            //click the button
            page = submitButton.click();

            pageText = page.asText();
            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), pageText);
            Log.info(c, name.getMethodName(), page.asXml());

            //make sure we still have the value
            assertTrue("The value entered was not saved between flow pages", pageText.contains("Current flowscope value: test"));
            //make sure we are still in flow
            assertTrue("Exited flow unexpectedly", pageText.contains("'Simple' flow page 2 In flow ? true"));
        }
    }

    /**
     * A simple test for the following JSF 2.3 Specification Issue:
     *
     * https://github.com/javaee/javaserverfaces-spec/issues/1113
     *
     * This test ensures that onselect attribute is not rendered when
     * selectable components are used.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.security.PrivilegedActionException")
    public void testSpecIssue1113() throws Exception {
        String contextRoot = "JSF23Spec1113";
        try (WebClient webClient = new WebClient()) {

            // Construct the URL for the test, in this case: faces/selectManyListboxSelectItems.xhtml
            URL url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "faces/selectManyListboxSelectItems.xhtml");

            HtmlPage page = (HtmlPage) webClient.getPage(url);

            String pageXml = page.asXml();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), pageXml);

            // Test that h:selectManyListbox does not render onselect attribute in a facelet with a select element
            assertTrue("The onselect attribute was rendered in a facelet.", !pageXml.contains("onselect=\"jsFunction\""));

            // Construct the URL for the test, in this case: faces/selectManyListboxSelectItems.jsp
            url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "faces/selectManyListboxSelectItems.jsp");

            try {
                Log.info(c, name.getMethodName(), "Invoking JSP page");
                page = (HtmlPage) webClient.getPage(url);
            } catch (FailingHttpStatusCodeException e) {
                String response = e.getResponse().getContentAsString();
                int statusCode = e.getStatusCode();

                Log.info(c, name.getMethodName(), "Caught FailingHttpStatusCodeException");
                Log.info(c, name.getMethodName(), "FailingHttpStatusCodeException response: " + response);
                Log.info(c, name.getMethodName(), "FailingHttpStatusCodeException statusCode: " + statusCode);

                /*
                 * com.ibm.ws.jsp.translator.JspTranslationException:
                 * JSPG0123E: Unable to locate tag attribute info for tag attribute onselect.<br>
                 */
                assertTrue("The JSP was rendered successfully and it should not have been.", response.contains("JSPG0123E") && statusCode == 500);
            }

            // Construct the URL for the test, in this case: faces/selectManyListboxSelectItems.jsp
            url = JSFUtils.createHttpUrl(jsf23Server, contextRoot, "faces/selectManyCheckboxSelectItems.xhtml");

            page = (HtmlPage) webClient.getPage(url);

            pageXml = page.asXml();

            // Log the page for debugging if necessary in the future.
            Log.info(c, name.getMethodName(), page.asText());
            Log.info(c, name.getMethodName(), pageXml);

            // Test that h:selectManyCheckbox does render onselect attribute in an input element
            assertTrue("The onselect attribute was not rendered in an input element.", pageXml.contains("onselect=\"jsFunction\""));
        }
    }
}
